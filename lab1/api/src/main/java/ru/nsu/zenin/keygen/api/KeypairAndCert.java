package ru.nsu.zenin.keygen.api;

import org.bouncycastle.cert.X509CertificateHolder;
import java.security.KeyPair;

public record KeypairAndCert(KeyPair keypair, X509CertificateHolder cert) {}
