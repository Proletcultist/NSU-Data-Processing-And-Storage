package ru.nsu.zenin.keygen.server;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import ru.nsu.zenin.keygen.api.KeypairAndCert;

class KeyService {
    private final ConcurrentMap<String, CompletableFuture<KeypairAndCert>> keypairs;
    private final KeypairAndCertGenerator generator;
    private final ExecutorService computationsExecutor;

    KeyService(KeypairAndCertGenerator generator, ExecutorService computationsExecutor) {
        this.keypairs = new ConcurrentHashMap<String, CompletableFuture<KeypairAndCert>>();
        this.generator = generator;
        this.computationsExecutor = computationsExecutor;
    }

    /*
    KeypairAndCert getKeyForSubject(String subjectName) {
    }
    */
}
