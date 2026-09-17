package ru.nsu.zenin.keygen.server;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.time.temporal.TemporalAmount;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import java.time.ZonedDateTime;
import org.bouncycastle.cert.X509CertificateHolder;
import java.util.Date;
import java.math.BigInteger;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import java.security.PublicKey;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import ru.nsu.zenin.keygen.api.KeypairAndCert;

class KeypairAndCertGenerator {
    private final X500Name caname;
    private final ContentSigner signer;
    private final KeyPairGenerator keyGenerator;
    private final TemporalAmount certLifetime;

    public KeypairAndCertGenerator(X500Name caname, ContentSigner signer, KeyPairGenerator keyGenerator, TemporalAmount certLifetime) {
        this.caname = caname;
        this.signer = signer;
        this.keyGenerator = keyGenerator;
        this.certLifetime = certLifetime;
    }

    public KeypairAndCert generate(String subjectname) {
        X500Name name = new X500Name(subjectname);
        KeyPair keypair = keyGenerator.generateKeyPair();
        PublicKey pub = keypair.getPublic();

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime certEnd = now.plus(certLifetime);

        X509CertificateHolder cert = new JcaX509v3CertificateBuilder(
            caname,
            BigInteger.valueOf(now.toEpochSecond()),
            Date.from(now.toInstant()),
            Date.from(certEnd.toInstant()),
            name,
            pub
        ).build(signer);

        return new KeypairAndCert(keypair.getPublic(), keypair.getPrivate(), cert);
    }
}
