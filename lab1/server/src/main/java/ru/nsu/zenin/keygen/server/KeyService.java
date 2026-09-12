package ru.nsu.zenin.keygen.server;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
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

    KeypairAndCert getKeyForSubject(String subjectName) throws InterruptedException {
        CompletableFuture<KeypairAndCert> newFut = new CompletableFuture<KeypairAndCert>();

        CompletableFuture<KeypairAndCert> fut = keypairs.putIfAbsent(subjectName, newFut);

        // No keypair and cert for this subject
        if (fut == null) {
            KeypairAndCert ret = generator.generate(subjectName);
            newFut.complete(ret);

            return ret;
        }
        // Keypair and cert for this subject was already added
        else {
            // fut.get() cannor throw ExecutorService, this future always completes successfully
            try {
                return fut.get();
            } catch (ExecutionException unexpectable) { throw new RuntimeException("Unexpected exception happend", unexpectable); }
        }
    }
}
