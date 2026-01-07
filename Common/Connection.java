package Common;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;


public class Connection {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    
    // 2 locks diferentes um para impedir que 2 threads tentem escrever no dos ao mm tempo
    // outra para impedir que 2 threads tentem ler do dis ao mm tempo 
    private final ReentrantLock readLock = new ReentrantLock();
    private final ReentrantLock writeLock = new ReentrantLock();

    public static class Frame {
        public final int tag;
        public final byte[] data;
        public Frame(int tag, byte[] data) {
            this.tag = tag;
            this.data = data;
        }
    }

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }


    //Envia: [tamanho][tag][dados]

    public void send(int tag, byte[] data) throws IOException {
        writeLock.lock();
        try {
            // FRAMING: escreve tamanho total (tag + dados)
            out.writeInt(4 + data.length);
            
            // TAG: identifica este pedido/resposta
            out.writeInt(tag);
            
            // DADOS
            out.write(data);
            out.flush();
        } finally {
            writeLock.unlock();
        }
    }

    // Recebe: [tamanho][tag][dados]
    public Frame receive() throws IOException {
        readLock.lock();
        try {
            // FRAMING: lê tamanho para saber quantos bytes ler
            int totalLen = in.readInt();
            
            // TAG: lê identificador
            int tag = in.readInt();
            
            // DADOS: lê o resto (tamanho - 4 bytes da tag)
            byte[] data = new byte[totalLen - 4];
            in.readFully(data);
            
            return new Frame(tag, data);
        } finally {
            readLock.unlock();
        }
    }

    public void close() throws IOException {
        socket.close();
    }
}