package ru.nsu.zenin.keygen.client;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import ru.nsu.zenin.keygen.api.KeypairAndCert;
import ru.nsu.zenin.util.parser.InetSocketAddressParser;

public class Client {
    private static Path publicKeyFile = Paths.get("public.key");
    private static Path privateKeyFile = Paths.get("private.key");
    private static Path certFile = Paths.get("certificate.crt");

    private static Option delay =
            Option.builder()
                    .argName("secs")
                    .option("d")
                    .longOpt("delay")
                    .hasArg(true)
                    .desc("delay between sending request and receiving response")
                    .build();
    private static Option fail =
            Option.builder()
                    .option("f")
                    .longOpt("fail")
                    .hasArg(false)
                    .desc("disconnect after sending request instead of waiting for response")
                    .build();
    private static Option help =
            Option.builder()
                    .option("h")
                    .longOpt("help")
                    .hasArg(false)
                    .desc("display help message")
                    .build();
    private static Options options = new Options().addOption(delay).addOption(fail).addOption(help);

    private static final Validator<ClientConfig> confValidator =
            ValidatorBuilder.<ClientConfig>of()
                    ._object(ClientConfig::getName, "name", c -> c.notNull())
                    ._object(ClientConfig::getEndpoint, "endpoint", c -> c.notNull())
                    ._long(ClientConfig::getDelay, "delay", c -> c.notNull().greaterThanOrEqual(0L))
                    ._object(ClientConfig::getFail, "fail", c -> c.notNull())
                    .build();

    public static void main(String[] args)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(help)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("keygen-client [options] <endpoint> <subject name>", options);
                return;
            }

            ClientConfig conf = parseArgs(cmd);
            ConstraintViolations violations = confValidator.validate(conf);
            if (!violations.isValid()) {
                System.err.println("Error: " + violations.get(0).message());
                System.exit(-1);
            }

            appMain(conf);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (InterruptedException e) {
            System.exit(-1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static ClientConfig parseArgs(CommandLine cmd) throws ParseException {
        List<String> cmdArgs = cmd.getArgList();

        if (cmdArgs.size() == 0) {
            throw new ParseException("Error: missing endpoint and subject name");
        } else if (cmdArgs.size() == 1) {
            throw new ParseException("Error: missing subject name");
        } else if (cmdArgs.size() > 2) {
            throw new ParseException("Error: too much arguments provided");
        }

        String delayRaw = cmd.getOptionValue(delay);
        long delayArg = delayRaw == null ? 0 : Long.parseLong(delayRaw);

        InetSocketAddress addr;
        try {
            addr = InetSocketAddressParser.parse(cmdArgs.get(0));
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                    "Error while parsing endpoint from \""
                            + cmdArgs.get(0)
                            + "\": "
                            + e.getMessage());
        }

        X500Name subjectName;
        try {
            subjectName = new X500Name(cmdArgs.get(1));
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                    "Error while parsing subject name from \""
                            + cmdArgs.get(1)
                            + "\": "
                            + e.getMessage());
        }

        return new ClientConfig(addr, subjectName, delayArg, cmd.hasOption(fail));
    }

    private static Writer tryCreateFile(Path file) throws IOException {
        if (Files.exists(file)) {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print(
                        "File " + file.toString() + " will be overwritten, continue? (y/n) ");

                try {
                    String line = scanner.nextLine();
                    if (line.equals("y")) {
                        break;
                    } else if (line.equals("n")) {
                        throw new IOException(
                                file.toString() + ": Existing file cannot be overwritten");
                    }
                } catch (NoSuchElementException e) {
                    throw new IOException(
                            file.toString() + ": Existing file cannot be overwritten");
                }
            }
        }

        return new OutputStreamWriter(
                Files.newOutputStream(
                        file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    private static void appMain(ClientConfig conf)
            throws IOException, InterruptedException, InvalidKeySpecException {
        try (Socket sock = new Socket()) {
            sock.connect(conf.getEndpoint());

            DataInputStream dis = new DataInputStream(sock.getInputStream());
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());

            for (char c : conf.getName().toString().toCharArray()) {
                dos.writeChar(c);
            }
            dos.writeChar('\0');

            if (conf.getFail()) {
                return;
            } else if (conf.getDelay() > 0) {
                Thread.sleep(conf.getDelay());
            }

            try {
                KeypairAndCert keypairAndCert = KeypairAndCert.deserialize(dis, "RSA");

                try (PemWriter publicKeyWriter = new PemWriter(tryCreateFile(publicKeyFile))) {
                    try (PemWriter privateKeyWriter =
                            new PemWriter(tryCreateFile(privateKeyFile))) {
                        try (PemWriter certWriter = new PemWriter(tryCreateFile(certFile))) {
                            KeyFactory keyFactory =
                                    KeyFactory.getInstance(keypairAndCert.getAlgorithm());
                            X509EncodedKeySpec publicKey =
                                    keyFactory.getKeySpec(
                                            keypairAndCert.getPublicKey(),
                                            X509EncodedKeySpec.class);
                            PKCS8EncodedKeySpec privateKey =
                                    keyFactory.getKeySpec(
                                            keypairAndCert.getPrivateKey(),
                                            PKCS8EncodedKeySpec.class);

                            publicKeyWriter.writeObject(
                                    new PemObject("PUBLIC KEY", publicKey.getEncoded()));
                            privateKeyWriter.writeObject(
                                    new PemObject("PRIVATE KEY", privateKey.getEncoded()));
                            certWriter.writeObject(
                                    new PemObject(
                                            "CERTIFICATE", keypairAndCert.getCert().getEncoded()));
                        }
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unexpected exception", e);
            } catch (EOFException e) {
                // Rethrow because generated EOFException doesn't contain any message
                throw new EOFException("Server unexpectedly closed the connection");
            }
        }
    }
}
