package Server;

import java.io.IOException;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = 12345;
        int numWorkers = 10;
        
        Auth auth = new Auth();
        ThreadPool pool = new ThreadPool(numWorkers);

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Servidor a escutar na porta " + port);
            System.out.println("ThreadPool com " + numWorkers + " workers");
            System.out.println("Admin: admin/admin");

            while (true) {
                Socket socket = ss.accept();
                // Thread leitora para cada cliente
                new Thread(new ClientWorker(socket, auth, pool)).start();
            }
        }
    }
}
