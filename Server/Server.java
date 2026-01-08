package Server;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Server {
    
    public static void main(String[] args) throws IOException {
        int port = 12345;
        int numWorkers = 10;
        int maxDays = 30;  // máximo de dias histórico
        
        Auth auth = new Auth();
        ThreadPool pool = new ThreadPool(numWorkers);
        TimeSeriesManager tsManager = new TimeSeriesManager(maxDays);

        // thread para ler terminal do servidor
        startCommandThread(tsManager);

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Servidor a escutar na porta " + port);
            System.out.println("ThreadPool com " + numWorkers + " workers");
            System.out.println("TimeSeriesManager com D=" + maxDays + " dias");            
            System.out.println("Admin: admin/admin");
            System.out.println("\n=== Server===");
            System.out.println("0 - Novo dia");
            System.out.println("1 - Sair");

            while (true) {
                Socket socket = ss.accept();
                new Thread(new ClientWorker(socket, auth, pool, tsManager)).start();
            }
        }
    }
    
    private static void startCommandThread(TimeSeriesManager tsManager) {
        Thread commandThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                String command = scanner.nextLine();
                
                switch (command) {
                    case "0":
                        tsManager.newDay();
                        System.out.println("Novo dia iniciado");
                        System.out.println(tsManager);
                        break;
                        
                    case "1":
                        System.out.println("A desligar o servidor");
                        System.exit(0);
                        break;
                    default:
                        if (!command.isEmpty()) {
                            System.out.println("Comando desconhecido");
                        }
                }
                System.out.println("\n=== Server===");
                System.out.println("0 - Novo dia");
                System.out.println("1 - Sair");
            }
        });
        commandThread.setDaemon(true);  // termina quando o programa terminar
        commandThread.start();
    }
}