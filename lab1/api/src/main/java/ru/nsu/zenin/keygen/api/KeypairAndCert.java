package ru.nsu.zenin.keygen.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.cert.X509CertificateHolder;
import ru.nsu.zenin.api.exception.IncompatiblePublicAndPrivateKeysException;

public class KeypairAndCert {
    private final String algorithm;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final X509CertificateHolder cert;

    public KeypairAndCert(PublicKey publicKey, PrivateKey privateKey, X509CertificateHolder cert) {
        if (!publicKey.getAlgorithm().equals(privateKey.getAlgorithm())) {
            throw new IncompatiblePublicAndPrivateKeysException(
                    "Public and private keys was generated with different algorithms");
        }

        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.cert = cert;
        this.algorithm = publicKey.getAlgorithm();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509CertificateHolder getCert() {
        return cert;
    }

    public static KeypairAndCert deserialize(DataInputStream dis, String algorithm)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

        byte[] publicKeyEncoded = new byte[dis.readInt()];
        dis.readFully(publicKeyEncoded);
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyEncoded));

        byte[] privateKeyEncoded = new byte[dis.readInt()];
        dis.readFully(privateKeyEncoded);
        PrivateKey privateKey =
                keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyEncoded));

        byte[] certEncoded = new byte[dis.readInt()];
        dis.readFully(certEncoded);
        X509CertificateHolder cert = new X509CertificateHolder(certEncoded);

        return new KeypairAndCert(publicKey, privateKey, cert);
    }

    public void serialize(DataOutputStream dos)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

        X509EncodedKeySpec publicKey =
                keyFactory.getKeySpec(this.publicKey, X509EncodedKeySpec.class);
        PKCS8EncodedKeySpec privateKey =
                keyFactory.getKeySpec(this.privateKey, PKCS8EncodedKeySpec.class);

        byte[] publicKeyEncoded = publicKey.getEncoded();
        byte[] privateKeyEncoded = privateKey.getEncoded();
        byte[] certEncoded = cert.getEncoded();

        dos.writeInt(publicKeyEncoded.length);
        dos.write(publicKeyEncoded, 0, publicKeyEncoded.length);

        dos.writeInt(privateKeyEncoded.length);
        dos.write(privateKeyEncoded, 0, privateKeyEncoded.length);

        dos.writeInt(certEncoded.length);
        dos.write(certEncoded, 0, certEncoded.length);
    }
}
