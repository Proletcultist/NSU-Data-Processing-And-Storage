package ru.nsu.zenin.keygen.client;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.PrivateKey;
import java.util.List;
import java.util.ArrayList;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import java.security.SecureRandom;
import java.security.KeyPairGenerator;
import org.bouncycastle.asn1.x500.X500Name;
import java.time.Period;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.nsu.zenin.keygen.server.KeypairAndCertGenerator;
import ru.nsu.zenin.keygen.server.KeyService;
import ru.nsu.zenin.keygen.server.CAServer;
import ru.nsu.zenin.keygen.api.KeypairAndCert;

class ClientTest {
    private static final int SERVER_PORT = 1234;
    private static final String TEST_RSA_KEY_FILENAME = "private.der";
    private static final X500Name name = new X500Name("CN=Test Certificate, O=My Organization, C=RU");
    private static final X500Name clientsName = new X500Name("CN=Test Certificate, O=NOTMy Organization, C=RU");
    private static final Period certLifetime = Period.ofYears(999);

    private static ServerSocket serverSocket;

    @BeforeAll
    static void startServer() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), SERVER_PORT);

        URL resource = ClientTest.class.getClassLoader().getResource(TEST_RSA_KEY_FILENAME);
        byte[] keyEncoded = Files.readAllBytes(Paths.get(resource.toURI()));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(keyEncoded);
        PrivateKey CAPrivateKey = factory.generatePrivate(privKeySpec);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(CAPrivateKey);

        KeyPairGenerator keypairGenerator = KeyPairGenerator.getInstance("RSA");
        keypairGenerator.initialize(8192, SecureRandom.getInstance("SHA1PRNG"));

        KeypairAndCertGenerator generator = new KeypairAndCertGenerator(name, signer, keypairGenerator, certLifetime);
        ExecutorService computationsExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        serverSocket = new ServerSocket();
        serverSocket.bind(addr);
        KeyService keyservice = new KeyService(generator, computationsExecutor);

        new Thread(() -> {
            try (CAServer serv = new CAServer(serverSocket, keyservice)) {
                serv.listen();
            } catch (Exception ignore) {}
        }).start();
    }

    @AfterAll
    static void shutdownServer() throws Exception {
        serverSocket.close();
    }

    @Test
    void test() throws Exception {
        int threadsAmount = Runtime.getRuntime().availableProcessors() + 1;
        Thread[] threads = new Thread[threadsAmount];
        KeypairAndCert[] keypairs = new KeypairAndCert[threadsAmount];

        for (int i = 0; i < threadsAmount; i++) {
            Client client = new Client(serverSocket.getLocalSocketAddress(), false, 0);
            int tmp = i;
            threads[i] = new Thread(() -> {
                try {
                    keypairs[tmp] = client.getKeypairAndCert(clientsName).get();
                } catch (Exception ignore) {
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threadsAmount; i++) {
            threads[i].join();
        }

        Assertions.assertNotNull(keypairs[0]);
        for (int i = 0; i < threadsAmount - 1; i++) {
            Assertions.assertNotNull(keypairs[i + 1]);
            Assertions.assertEquals(keypairs[i], keypairs[i + 1]);
        }
    }
}
