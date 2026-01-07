package Client;

import Common.Connection; 
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer implements AutoCloseable {
    private final Connection conn;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<Integer, Entry> map = new HashMap<>();
    private IOException exception = null;

    private class Entry {
        byte[] data = null;
        Condition cond = lock.newCondition();
    }

    public Demultiplexer(Socket socket) throws IOException {
        this.conn = new Connection(socket);  
    }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    Connection.Frame frame = conn.receive(); 
                    
                    lock.lock();
                    try {
                        Entry e = map.get(frame.tag);
                        if (e != null) {
                            e.data = frame.data;
                            e.cond.signal();  // Acorda quem espera
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (IOException e) {
                // Conexão fechou - acorda todos
                lock.lock();
                try {
                    exception = e;
                    for (Entry entry : map.values()) {
                        entry.cond.signalAll();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    }

    public void send(int tag, byte[] data) throws IOException {
        conn.send(tag, data);
    }

    public byte[] receive(int tag) throws IOException, InterruptedException {
        lock.lock();
        try {
            Entry e = new Entry();
            map.put(tag, e);
            
            while (e.data == null && exception == null) {
                e.cond.await();  // Dorme
            }
            
            map.remove(tag);
            
            if (exception != null) throw exception;
            return e.data;
        } finally {
            lock.unlock();
        }
    }

    public void close() throws IOException {
        conn.close();
    }
}