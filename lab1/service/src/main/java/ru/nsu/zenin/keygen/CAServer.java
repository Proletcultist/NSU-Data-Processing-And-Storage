package ru.nsu.zenin.keygen;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.OutputStream;

class CAServer {
    private final ConcurrentMap<String, CompletableFuture<KeypairAndCert>> keypairs;
    private final ServerSocket serverSock;
    private final KeypairAndCertGenerator generator;
    private final ExecutorService computationsExecutor;

    public CAServer(ServerSocket sock, KeypairAndCertGenerator generator, ExecutorService computationsExecutor) {
        this.keypairs = new ConcurrentHashMap<String, CompletableFuture<KeypairAndCert>>();
        this.serverSock = sock;
        this.generator = generator;
        this.computationsExecutor = computationsExecutor;
    }
    
    public void listen() throws IOException {
        while (true) {
            Socket client = serverSock.accept();
            Thread.ofVirtual().start(() -> serveRequest(client));
        }
    }

    private void serveRequest(Socket socket) {
        try {
            try {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                String name = receiveSubjectName(is);
            }
            finally {
                socket.close();
            }
        } catch (IOException ignore) {}
    }

    private String receiveSubjectName(InputStream is) throws IOException {
        try (DataInputStream dis = new DataInputStream(is)) {
            StringBuilder sb = new StringBuilder();

            // TODO: Limit max characters amount to secure from DoS
            while (true) {
                char c = dis.readChar();
                if (c == '\0') {
                    break;
                }

                sb.append(c);
            }

            return sb.toString();
        }
    }
}
