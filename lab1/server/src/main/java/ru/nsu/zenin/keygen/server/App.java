package ru.nsu.zenin.keygen.server;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import ru.nsu.zenin.keygen.server.exception.NoPemObjectException;
import ru.nsu.zenin.util.InetSocketAddressParser;

public class App {
    private static Option jobs =
            Option.builder()
                    .argName("workerThreads")
                    .option("j")
                    .longOpt("jobs")
                    .hasArg(true)
                    .desc("key generating threads amount")
                    .build();
    private static Option config =
            Option.builder()
                    .argName("file")
                    .option("c")
                    .longOpt("config")
                    .hasArg(true)
                    .desc("path to config file")
                    .build();
    private static Option help =
            Option.builder()
                    .option("h")
                    .longOpt("help")
                    .hasArg(false)
                    .desc("display help message")
                    .build();
    private static Options options =
            new Options().addOption(jobs).addOption(config).addOption(help);

    private static final Validator<CAServerConfig> confValidator =
            ValidatorBuilder.<CAServerConfig>of()
                    ._object(CAServerConfig::getName, "name", c -> c.notNull())
                    ._object(CAServerConfig::getEndpoint, "endpoint", c -> c.notNull())
                    ._object(CAServerConfig::getCertLifetime, "certLifetime", c -> c.notNull())
                    ._object(CAServerConfig::getPrivateKeyFile, "privateKeyFile", c -> c.notNull())
                    ._integer(
                            CAServerConfig::getWorkerThreads,
                            "workerThreads",
                            c -> c.notNull().greaterThan(0))
                    .build();

    public static void main(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            // If there is --help option - display help and exit
            if (cmd.hasOption(help)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("keygen-server --config <file> [options]", options);
                return;
            }

            CAServerConfig conf = parseArgs(cmd);
            ConstraintViolations violations = confValidator.validate(conf);
            if (!violations.isValid()) {
                System.err.println("Error: " + violations.get(0).message());
                System.exit(-1);
            }

            appMain(conf);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static CAServerConfig parseArgs(CommandLine cmd) throws ParseException, IOException {
        if (!cmd.hasOption(config)) {
            throw new ParseException("No config file provided");
        }

        ObjectMapper mapper =
                new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
        CAServerConfig conf =
                mapper.readValue(new File(cmd.getOptionValue(config)), CAServerConfig.class);

        if (cmd.hasOption(jobs)) {
            String workerThreadsRaw = cmd.getOptionValue(jobs);
            int workerThreads = Integer.parseInt(workerThreadsRaw);
            conf.setWorkerThreads(workerThreads);
        } else if (conf.getWorkerThreads() == null) {
            conf.setWorkerThreads(Runtime.getRuntime().availableProcessors());
        }

        return conf;
    }

    private static void appMain(CAServerConfig config)
            throws IOException, InvalidKeySpecException, NoPemObjectException {
        Security.addProvider(new BouncyCastleProvider());

        InetSocketAddress addr = InetSocketAddressParser.parse(config.getEndpoint());
        PrivateKey CAPrivateKey = readPrivateKey(config.getPrivateKeyFile());

        ContentSigner signer;
        try {
            signer = new JcaContentSignerBuilder("SHA256withRSA").build(CAPrivateKey);
        } catch (OperatorCreationException e) {
            throw new RuntimeException("Unexpected exception", e);
        }

        KeyPairGenerator keypairGenerator;
        try {
            keypairGenerator = KeyPairGenerator.getInstance("RSA");
            keypairGenerator.initialize(8192, SecureRandom.getInstance("SHA1PRNG"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
        // TODO: Handle illegalargumentexception
        X500Name caname = new X500Name(config.getName());

        KeypairAndCertGenerator generator =
                new KeypairAndCertGenerator(
                        caname, signer, keypairGenerator, config.getCertLifetime());
        ExecutorService computationsExecutor =
                Executors.newFixedThreadPool(config.getWorkerThreads());

        ServerSocket sock = new ServerSocket();
        sock.bind(addr);
        KeyService keyservice = new KeyService(generator, computationsExecutor);

        try (CAServer serv = new CAServer(sock, keyservice)) {
            serv.listen();
        }
    }

    private static PrivateKey readPrivateKey(String filename)
            throws IOException, InvalidKeySpecException, NoPemObjectException {
        KeyFactory factory;
        try {
            factory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unexpected exception", e);
        }

        try (PemReader keyReader = new PemReader(new FileReader(new File(filename)))) {
            PemObject pemObject = keyReader.readPemObject();
            if (pemObject == null) {
                throw new NoPemObjectException(
                        "File " + filename + " doesn't contain private key in PEM format");
            }

            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            return factory.generatePrivate(privKeySpec);
        }
    }
}
