package ru.nsu.zenin.keygen.client;

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
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import ru.nsu.zenin.keygen.api.KeypairAndCert;
import ru.nsu.zenin.util.InetSocketAddressParser;

public class Client {
    private static Path publicKeyFile = Paths.get("public.key");
    private static Path privateKeyFile = Paths.get("private.key");
    private static Path certFile = Paths.get("certificate.crt");

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

    public static void main(String[] args)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        Option delay =
                Option.builder()
                        .argName("secs")
                        .option("d")
                        .longOpt("delay")
                        .hasArg(true)
                        .desc("delay between sending request and receiving response")
                        .build();
        Option fail =
                Option.builder()
                        .option("f")
                        .longOpt("fail")
                        .hasArg(false)
                        .desc("disconnect after sending request instead of waiting for response")
                        .build();
        Option help =
                Option.builder()
                        .option("h")
                        .longOpt("help")
                        .hasArg(false)
                        .desc("display help message")
                        .build();
        Options options = new Options();
        options.addOption(delay);
        options.addOption(fail);
        options.addOption(help);

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(help)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("keygen-client [options] <endpoint> <subject name>", options);
                return;
            }

            List<String> cmdArgs = cmd.getArgList();

            if (cmdArgs.size() == 0) {
                System.err.println("Error: missing endpoint and subject name");
                System.exit(-1);
            } else if (cmdArgs.size() == 1) {
                System.err.println("Error: missing subject name");
                System.exit(-1);
            } else if (cmdArgs.size() > 2) {
                System.err.println("Error: too much arguments provided");
                System.exit(-1);
            }

            String delayRaw = cmd.getOptionValue(delay);
            long delayArg = delayRaw == null ? 0 : Long.parseLong(delayRaw);
            InetSocketAddress addr = InetSocketAddressParser.parse(cmdArgs.get(0));
            String subjectName = cmdArgs.get(1);

            appMain(addr, subjectName, delayArg, cmd.hasOption(fail));
        } catch (ParseException | IllegalArgumentException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(-1);
        } catch (InterruptedException e) {
            System.exit(-1);
        }
    }

    private static void appMain(
            InetSocketAddress addr, String subjectName, long delay, boolean fail)
            throws IOException,
                    InterruptedException,
                    InvalidKeySpecException,
                    NoSuchAlgorithmException {
        try (Socket sock = new Socket()) {
            sock.connect(addr);

            DataInputStream dis = new DataInputStream(sock.getInputStream());
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());

            for (char c : subjectName.toCharArray()) {
                dos.writeChar(c);
            }
            dos.writeChar('\0');

            if (fail) {
                return;
            } else if (delay > 0) {
                Thread.sleep(delay);
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
            } catch (EOFException e) {
                // Rethrow because generated EOFException doesn't contain any message
                throw new EOFException("Server unexpectedly closed the connection");
            }
        }
    }
}
