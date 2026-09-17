package ru.nsu.zenin.keygen.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import ru.nsu.zenin.keygen.api.KeypairAndCert;

// TODO: Implement AutoCloseable
class CAServer {
  private static int SO_TIMEOUT = 5 * 1000; // 5 seconds

  private final ServerSocket serverSock;
  private final KeyService keyserivce;

  public CAServer(ServerSocket sock, KeyService keyserivce) {
    this.serverSock = sock;
    this.keyserivce = keyserivce;
  }

  public void listen() throws IOException {
    while (true) {
      Socket client = serverSock.accept();
      client.setSoTimeout(SO_TIMEOUT);
      System.err.println("Connected " + client.getInetAddress());
      Thread.ofVirtual().start(() -> serveRequest(client));
    }
  }

  private void serveRequest(Socket socket) {
    try {
      InputStream is = socket.getInputStream();
      String name = receiveSubjectName(is);

      System.err.println("Received name from " + socket.getInetAddress() + ": " + name);

      KeypairAndCert out = keyserivce.getKeyForSubject(name);
      DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
      out.serialize(dos);

      System.err.println("Sent key and cert to " + socket.getInetAddress());
    } catch (InterruptedException ignore) {
    } catch (Exception e) {
      System.err.println("Failed to serve request from " + socket.getInetAddress() + ": " + e);
    } finally {
      try {
        socket.close();
      } catch (IOException ignore) {
      }
    }
  }

  private String receiveSubjectName(InputStream is) throws IOException {
    DataInputStream dis = new DataInputStream(is);
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
