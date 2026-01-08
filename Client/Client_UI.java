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
            System.out.println("2 - Sair");
            System.out.print("> ");

            String op = sc.nextLine();
            try {
                if (op.equals("0")) {
                    System.out.print("Username: ");
                    String user = sc.nextLine();
                    System.out.print("Password: ");
                    String pass = sc.nextLine();
                    
                    if (client.login(user, pass)) {
                        System.out.println("Login OK!" + (client.isAdmin() ? " (ADMIN)" : ""));
                    } else {
                        System.out.println("Credenciais inválidas.");
                    }
                } else if (op.equals("1")) {
                    System.out.print("Username: ");
                    String user = sc.nextLine();
                    System.out.print("Password: ");
                    String pass = sc.nextLine();
                    
                    if (client.registar(user, pass)) {
                        System.out.println("Registo OK! Pode fazer login.");
                    } else {
                        System.out.println("Username já existe.");
                    }
                } else if( op.equals("2")) {
                    System.out.println("Adeus!");
                    System.exit(0);
                }
            } catch (IOException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private void menuPrincipal() {
        while (true) {
            System.out.println("\n=== Menu Principal ===");
            System.out.println("0 - Adicionar evento de venda");
            System.out.println("1 - Ver em que dia estamos");

            if (client.isAdmin()) {
                System.out.println("2 - Avançar dia (ADMIN)");
            }
            System.out.println("3 - Sair");
            System.out.print("> ");

            String op = sc.nextLine();
            try {
                switch (op) {
                    case "0":
                        menuAddEvent();
                        break;

                    case "1":
                        System.out.println("Estamos no dia: " + client.getCurrentDayId());
                        break;

                    case "2":
                        if (client.isAdmin()) {
                            menuNewDay();
                        } else {
                            System.out.println("Opção inválida.");
                        }
                        break;

                    case "3":
                        System.out.println("Adeus!");
                        return;
                        
                    default:
                        System.out.println("Opção inválida.");
                }
            } catch (IOException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }


    private void menuAddEvent() throws IOException {
        System.out.print("Produto: ");
        String product = sc.nextLine();
        if (product.isEmpty()) {
            System.out.println("Produto não pode ser vazio.");
            return;
        }
        
        System.out.print("Quantidade: ");
        int quantity;
        try {
            quantity = Integer.parseInt(sc.nextLine());
            if (quantity <= 0) {
                System.out.println("Quantidade deve ser > 0.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Quantidade inválida.");
            return;
        }
        
        System.out.print("Preço: ");
        double price;
        try {
            price = Double.parseDouble(sc.nextLine().replace(",", "."));
            if (price < 0) {
                System.out.println("Preço deve ser >= 0.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Preço inválido.");
            return;
        }
        
        if (client.addEvent(product, quantity, price)) {
            System.out.println("Evento adicionado com sucesso!");
            System.out.printf("  %s: %d unidades a %.2f€ cada%n", product, quantity, price);
        } else {
            System.out.println("Erro ao adicionar evento.");
        }
    }

    private void menuNewDay() throws IOException {
        int newDayId = client.newDay();
        if (newDayId >= 0) {
            System.out.println("Novo dia iniciado! Dia atual: " + newDayId);
        } else {
            System.out.println("Erro ao avançar dia (apenas admin pode fazer isto).");
        }

    }

}
