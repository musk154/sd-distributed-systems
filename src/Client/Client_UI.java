package Client;

import java.io.IOException;
import java.util.Scanner;

public class Client_UI {
    private Client client;
    private Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        new Client_UI().run();
    }

    private void run() {
        System.out.println("=== Cliente SD ===");
        
        try {
            System.out.print("A conectar...");
            client = new Client("localhost", 12345);
            System.out.println(" OK!");
        } catch (IOException e) {
            System.out.println(" ERRO: " + e.getMessage());
            return;
        }

        menuLogin();
        menuPrincipal();

        try { client.close(); } catch (IOException ignored) {}
    }

    private void menuLogin() {
        while (!client.isAuthenticated()) {
            System.out.println("\n=== Registo / Login ===");
            System.out.println("0 - Login");
            System.out.println("1 - Registar");
            System.out.print("> ");

            String op = sc.nextLine().trim();
            try {
                if (op.equals("0")) {
                    System.out.print("Username: ");
                    String user = sc.nextLine().trim();
                    System.out.print("Password: ");
                    String pass = sc.nextLine().trim();
                    
                    if (client.login(user, pass)) {
                        System.out.println("Login OK!" + (client.isAdmin() ? " (ADMIN)" : ""));
                    } else {
                        System.out.println("Credenciais inválidas.");
                    }
                } else if (op.equals("1")) {
                    System.out.print("Username: ");
                    String user = sc.nextLine().trim();
                    System.out.print("Password: ");
                    String pass = sc.nextLine().trim();
                    
                    if (client.registar(user, pass)) {
                        System.out.println("Registo OK! Pode fazer login.");
                    } else {
                        System.out.println("Username já existe.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private void menuPrincipal() {
        while (true) {
            System.out.println("\n=== Menu Principal ===");
            System.out.println("0 - Sair");
            System.out.print("> ");

            String op = sc.nextLine().trim();
            if (op.equals("0")) {
                System.out.println("Adeus!");
                return;
            }
        }
    }
}
