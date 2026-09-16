package ru.nsu.zenin.keygen.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import ru.nsu.zenin.keygen.api.KeypairAndCert;

// TODO: Implement AutoCloseable
class CAServer {
    private final ServerSocket serverSock;
    private final KeyService keyserivce;

    public CAServer(ServerSocket sock, KeyService keyserivce) {
        this.serverSock = sock;
        this.keyserivce = keyserivce;
    }
    
    public void listen() throws IOException {
        while (true) {
            // TODO: Set timeout for socket
            Socket client = serverSock.accept();
            Thread.ofVirtual().start(() -> serveRequest(client));
        }
    }

    private void serveRequest(Socket socket) {
        try {
            InputStream is = socket.getInputStream();
            String name = receiveSubjectName(is);

            KeypairAndCert out = keyserivce.getKeyForSubject(name);

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            out.serialize(dos);
        }
        catch (InterruptedException ignore) {}
        catch (Exception e) {
            System.err.println("Failed to serve request from " + socket.getInetAddress() + ": " + e);
        }
        finally {
            try {
                socket.close();
            } catch (IOException ignore) {}
        }
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
