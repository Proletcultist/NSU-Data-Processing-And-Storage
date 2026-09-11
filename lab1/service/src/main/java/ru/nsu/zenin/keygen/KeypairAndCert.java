package ru.nsu.zenin.keygen;

import org.bouncycastle.cert.X509CertificateHolder;
import java.security.KeyPair;

record KeypairAndCert(KeyPair keypair, X509CertificateHolder cert) {}
