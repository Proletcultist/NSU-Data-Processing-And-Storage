package ru.nsu.zenin.keygen.api;

import org.bouncycastle.cert.X509CertificateHolder;
import java.security.KeyPair;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import ru.nsu.zenin.api.exception.IncompatiblePublicAndPrivateKeysException;

public class KeypairAndCert {
    private final String algorithm;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final X509CertificateHolder cert;

    public KeypairAndCert(PublicKey publicKey, PrivateKey privateKey, X509CertificateHolder cert) {
        if (!publicKey.getAlgorithm().equals(privateKey.getAlgorithm())) {
            throw new IncompatiblePublicAndPrivateKeysException("Public and private keys was generated with different algorithms");
        }

        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.cert = cert;
        this.algorithm = publicKey.getAlgorithm();
    }

    /*
    public static KeypairAndCert deserialize(DataInputStream dis) {
    }
    */

    public void serialize(DataOutputStream dos) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

        X509EncodedKeySpec publicKey = keyFactory.getKeySpec(this.publicKey, X509EncodedKeySpec.class);
        PKCS8EncodedKeySpec privateKey = keyFactory.getKeySpec(this.privateKey, PKCS8EncodedKeySpec.class);

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
