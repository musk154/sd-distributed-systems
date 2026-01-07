package Server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import Common.*;

public class ClientWorker implements Runnable {
    private final Socket socket;
    private final Auth auth;
    private final ThreadPool pool;
    private Connection conn;
    private User user = null;

    public ClientWorker (Socket socket, Auth auth, ThreadPool pool){
        this.socket=socket;
        this.auth=auth;
        this.pool=pool;
    }

    private class Task implements Runnable {
        private final int tag;
        private final byte[] data;

        public Task(int tag, byte[] data){
            this.tag=tag;
            this.data=data;
        }

        @Override
        public void run(){
            try{
                processRequest(tag,data);
            } catch (IOException e) {
                System.err.println("Erro a processar pedido: " + e.getMessage());

            }

        }

    }

    @Override
    public void run() {
        try{
            conn = new Connection(socket);
            System.out.println("Cliente conectado: " + socket.getRemoteSocketAddress());

            // Thread lê pedidos do seu cliente e manda à thread pool
            while (true) {
                Connection.Frame frame = conn.receive();
                int tag = frame.tag;
                byte[] data = frame.data;


                // Cria a tarefa e submete ao ThreadPool
                Task task = new Task(tag,data);
                pool.submit(task);
                
            }
        } catch (IOException e){
            System.err.println("Erro: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}  // ← try-catch
        }
    }

    private void processRequest(int routingTag, byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        int operationType = dis.readInt();   // lê primeiro byte da data que é a tag da tarefa
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        switch (operationType) {
            case Protocolo.TAG_LOGIN: {
                String username = dis.readUTF();
                String password = dis.readUTF();
                User u = auth.login(username, password);
                if (u != null) {
                    user = u;
                    dos.writeByte(Protocolo.STATUS_OK);
                    dos.writeBoolean(u.admin);
                    System.out.println("[AUTH] Login OK: " + username);
                } else {
                    dos.writeByte(Protocolo.STATUS_INVALID_CREDENTIALS);
                    System.out.println("[AUTH] Login FALHOU: " + username);
                }
                break;
            }
            case Protocolo.TAG_REGISTER: {
                String username = dis.readUTF();
                String password = dis.readUTF();
                if (auth.register(username, password)) {
                    dos.writeByte(Protocolo.STATUS_OK);
                    System.out.println("[AUTH] Registo OK: " + username);
                } else {
                    dos.writeByte(Protocolo.STATUS_USER_EXISTS);
                    System.out.println("[AUTH] Registo FALHOU: " + username);
                }
                break;
            }
            default:
                dos.writeByte(Protocolo.STATUS_ERROR);
        }

        // Envia resposta com a mesma tag (TaggedConnection já tem lock)
        conn.send(routingTag, baos.toByteArray());
    }
    
}
