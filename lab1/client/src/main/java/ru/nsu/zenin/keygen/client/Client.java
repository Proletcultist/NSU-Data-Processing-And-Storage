package ru.nsu.zenin.keygen.client;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.NoSuchAlgorithmException;
import java.io.EOFException;
import java.util.Optional;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import org.bouncycastle.asn1.x500.X500Name;
import ru.nsu.zenin.keygen.api.KeypairAndCert;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Client {
    private final InetSocketAddress addr;
    private final boolean fail;
    private final long delay;

    public Optional<KeypairAndCert> getKeypairAndCert(X500Name name) throws IOException, InterruptedException, InvalidKeySpecException {
        try (Socket sock = new Socket()) {
            sock.connect(addr);

            DataInputStream dis = new DataInputStream(sock.getInputStream());
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());

            for (char c : name.toString().toCharArray()) {
                dos.writeChar(c);
            }
            dos.writeChar('\0');

            if (fail) {
                return Optional.empty();
            } else if (delay > 0) {
                Thread.sleep(delay);
            }

            return Optional.of(KeypairAndCert.deserialize(dis, "RSA"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unexpected exception", e);
        } catch (EOFException e) {
            // Rethrow because generated EOFException doesn't contain any message
            throw new EOFException("Server unexpectedly closed the connection");
        }

    }
}
