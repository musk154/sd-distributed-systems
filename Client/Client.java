package Client;

import Common.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements AutoCloseable {
    private final Demultiplexer demux;
    private int tagCounter = 0;
    private final Lock lock = new ReentrantLock();
    private boolean authenticated = false;
    private boolean admin = false;

    public Client(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        this.demux = new Demultiplexer(socket);
        demux.start();
    }

    private int nextTag() {
        lock.lock();
        try {
            return tagCounter++;
        } finally {
            lock.unlock();
        }
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

    public boolean addEvent(String product, int quantity, double price) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_ADD_EVENT);
        dos.writeUTF(product);
        dos.writeInt(quantity);
        dos.writeDouble(price);

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

    public int newDay() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_NEW_DAY);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            if (dis.readByte() == Tags.STATUS_OK) {
                return dis.readInt();  // Retorna o ID do novo dia
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return -1;
    }
    public int getCurrentDayId() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_GET_CURRENT_DAY);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            if (dis.readByte() == Tags.STATUS_OK) {
                return dis.readInt();  // Retorna o ID do dia atual
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return -1;
    }

    
    public Integer getQuantitySold(String product, int days) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_AGG_QUANTITY);
        dos.writeUTF(product);
        dos.writeInt(days);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            byte status = dis.readByte();
            if (status == Tags.STATUS_OK) {
                return dis.readInt();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    
    public Double getSalesVolume(String product, int days) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_AGG_VOLUME);
        dos.writeUTF(product);
        dos.writeInt(days);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            byte status = dis.readByte();
            if (status == Tags.STATUS_OK) {
                return dis.readDouble();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    
    public Double getAveragePrice(String product, int days) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_AGG_AVG_PRICE);
        dos.writeUTF(product);
        dos.writeInt(days);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            byte status = dis.readByte();
            if (status == Tags.STATUS_OK) {
                return dis.readDouble();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    
    public Double getMaxPrice(String product, int days) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_AGG_MAX_PRICE);
        dos.writeUTF(product);
        dos.writeInt(days);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            byte status = dis.readByte();
            if (status == Tags.STATUS_OK) {
                return dis.readDouble();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public List<Event> filterEvents(int dayOffset, Set<String> products) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_FILTER_EVENTS);
        dos.writeInt(dayOffset);
        dos.writeInt(products.size());
        for (String prod : products) {
            dos.writeUTF(prod);
        }

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            byte status = dis.readByte();
            if (status != Tags.STATUS_OK) {
                return null;
            }
            
            //  DESERIALIZAÇÃO 
            
            // Ler dicionário
            int numProducts = dis.readInt();
            String[] productDict = new String[numProducts];
            for (int i = 0; i < numProducts; i++) {
                productDict[i] = dis.readUTF();
            }
            
            // Ler eventos e reconstruir
            int numEvents = dis.readInt();
            List<Event> events = new ArrayList<>();
            for (int i = 0; i < numEvents; i++) {
                int idx = dis.readInt();
                int quantity = dis.readInt();
                double price = dis.readDouble();
                events.add(new Event(productDict[idx], quantity, price));
            }
            
            return events;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }


    public Boolean waitSimultaneous(String p1, String p2) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_WAIT_SIMULTANEOUS);
        dos.writeUTF(p1);
        dos.writeUTF(p2);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            if (dis.readByte() == Tags.STATUS_OK) {
                return dis.readBoolean();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public String waitConsecutive(int n) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(Tags.TAG_WAIT_CONSECUTIVE);
        dos.writeInt(n);

        int tag = nextTag();
        try {
            demux.send(tag, baos.toByteArray());
            byte[] resp = demux.receive(tag);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp));
            
            if (dis.readByte() == Tags.STATUS_OK) {
                if (dis.readBoolean()) {
                    return dis.readUTF();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public boolean isAuthenticated() { return authenticated; }
    public boolean isAdmin() { return admin; }

    public void close() throws IOException {
        demux.close();
    }
}