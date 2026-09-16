package ru.nsu.zenin.keygen.server;

import org.bouncycastle.asn1.x500.X500Name;
import java.security.PrivateKey;
import org.bouncycastle.operator.ContentSigner;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.ExecutorService;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import java.io.File;
import java.io.IOException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import java.io.FileReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import ru.nsu.zenin.util.InetSocketAddressParser;

public class App {

    public static void main(String[] args) throws Exception {
        Option jobs = Option.builder()
            .argName("n")
            .option("j")
            .longOpt("jobs")
            .hasArg(true)
            .desc("key generating threads amount")
            .build();
        Option config = Option.builder()
            .argName("file")
            .option("c")
            .longOpt("config")
            .hasArg(true)
            .desc("path to config file")
            .build();
        Option help = Option.builder()
            .option("h")
            .longOpt("help")
            .hasArg(false)
            .desc("display help message")
            .build();

        Options options = new Options();
        options.addOption(jobs);
        options.addOption(config);
        options.addOption(help);

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(help)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("keygen-server --config <file> [options]", options);
                return;
            }
            if (!cmd.hasOption(config)) {
                System.err.println("Error: no config file provided");
                System.exit(-1);
            }

            String workerThreadsRaw = cmd.getOptionValue(jobs);
            int workerThreads = workerThreadsRaw == null ? Runtime.getRuntime().availableProcessors() : Integer.parseInt(workerThreadsRaw);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule());
            CAServerConfig caconfig = mapper.readValue(new File(cmd.getOptionValue(config)), CAServerConfig.class);

            appMain(workerThreads, caconfig);
        }
        catch (IOException | ParseException | InvalidKeySpecException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static void appMain(int workerThreads, CAServerConfig config) throws Exception {
        InetSocketAddress addr = InetSocketAddressParser.parse(config.endpoint());
        PrivateKey CAPrivateKey = readPrivateKey(config.privateKeyFile());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(CAPrivateKey);
        KeyPairGenerator keypairGenerator = KeyPairGenerator.getInstance("RSA");
        keypairGenerator.initialize(8192, new SecureRandom());
        X500Name caname = new X500Name(config.name());

        KeypairAndCertGenerator generator = new KeypairAndCertGenerator(caname, signer, keypairGenerator, config.certLifetime());
        ExecutorService computationsExecutor = Executors.newFixedThreadPool(workerThreads);

        ServerSocket sock = new ServerSocket();
        sock.bind(addr);
        KeyService keyservice = new KeyService(generator, computationsExecutor);

        CAServer serv = new CAServer(sock, keyservice);
        serv.listen();
    }

    static PrivateKey readPrivateKey(String filename) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (PemReader keyReader = new PemReader(new FileReader(new File(filename)))) {
            PemObject pemObject = keyReader.readPemObject();
            if (pemObject == null) {
                throw new IllegalArgumentException("File " + filename + " doesn't contain private key in PEM format");
            }

            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            return factory.generatePrivate(privKeySpec);
        }
    }
}
