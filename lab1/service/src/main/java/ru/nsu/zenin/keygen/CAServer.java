package ru.nsu.zenin.keygen;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;

class CAServer {
    private final ConcurrentMap<String, CompletableFuture<KeypairAndCert>> keypairs;
    private final ServerSocket sock;
    private final KeypairAndCertGenerator generator;
    private final ExecutorService computationsExecutor;

    public CAServer(ServerSocket sock, KeypairAndCertGenerator generator, ExecutorService computationsExecutor) {
        this.keypairs = new ConcurrentHashMap<String, CompletableFuture<KeypairAndCert>>();
        this.sock = sock;
        this.generator = generator;
        this.computationsExecutor = computationsExecutor;
    }
    
    public void listen() {
    }
}
