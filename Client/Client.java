package Client;

import Common.Connection;
import Common.Tags;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements AutoCloseable {
    private final Demultiplexer demux;
    private final AtomicInteger tagCounter = new AtomicInteger(0);
    private boolean authenticated = false;
    private boolean admin = false;

    public Client(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        this.demux = new Demultiplexer(socket);
        demux.start();
    }

    private int nextTag() {
        return tagCounter.incrementAndGet();
    }

    public boolean login(String username, String password) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_LOGIN);
        dos.writeUTF(username);
        dos.writeUTF(password);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            if (dis.readByte() == Tags.STATUS_OK) {
                authenticated = true;
                admin = dis.readBoolean();
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public boolean registar(String username, String password) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_REGISTER);
        dos.writeUTF(username);
        dos.writeUTF(password);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            return resp[0] == Tags.STATUS_OK;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public boolean isAuthenticated() { return authenticated; }
    public boolean isAdmin() { return admin; }

    public void close() throws IOException {
        demux.close();
    }
}