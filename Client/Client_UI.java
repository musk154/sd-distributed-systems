package Client;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import Common.Event;

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
                } else if (op.equals("2")) {
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
            System.out.println("1 - Ver dia atual");
            System.out.println("2 - Consultar agregações");
            System.out.println("3 - Filtrar eventos por produtos");
            if (client.isAdmin()) {
                System.out.println("4 - Avançar dia (ADMIN)");
            }
            System.out.println("9 - Sair");
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
                        menuAgregacoes();
                        break;

                    case "3":
                        menuFiltrarEventos();
                        break;

                    case "4":
                        if (client.isAdmin()) {
                            menuNewDay();
                        } else {
                            System.out.println("Opção inválida.");
                        }
                        break;


                    case "9":
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

    private void menuAgregacoes() throws IOException {
        System.out.println("\n=== Agregações (dias anteriores) ===");
        System.out.println("0 - Quantidade vendida");
        System.out.println("1 - Volume de vendas (valor total)");
        System.out.println("2 - Preço médio");
        System.out.println("3 - Preço máximo");
        System.out.println("4 - Todas as agregações de um produto");
        System.out.println("9 - Voltar");
        System.out.print("> ");

        String op = sc.nextLine();

        if (op.equals("9")) return;

        // Para todas as opções, pedir produto e dias
        System.out.print("Produto: ");
        String product = sc.nextLine();
        if (product.isEmpty()) {
            System.out.println("Produto não pode ser vazio.");
            return;
        }

        System.out.print("Número de dias anteriores: ");
        int days;
        try {
            days = Integer.parseInt(sc.nextLine());
            if (days < 1) {
                System.out.println("Dias deve ser >= 1.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Número inválido.");
            return;
        }

        switch (op) {
            case "0":
                handleQuantidade(product, days);
                break;
            case "1":
                handleVolume(product, days);
                break;
            case "2":
                handlePrecoMedio(product, days);
                break;
            case "3":
                handlePrecoMaximo(product, days);
                break;
            case "4":
                handleTodasAgregacoes(product, days);
                break;
            default:
                System.out.println("Opção inválida.");
        }
    }

    private void handleQuantidade(String product, int days) throws IOException {
        Integer qty = client.getQuantitySold(product, days);
        if (qty != null) {
            System.out.printf("Quantidade de '%s' vendida nos últimos %d dias: %d unidades%n", 
                              product, days, qty);
        } else {
            System.out.println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handleVolume(String product, int days) throws IOException {
        Double vol = client.getSalesVolume(product, days);
        if (vol != null) {
            System.out.printf("Volume de vendas de '%s' nos últimos %d dias: %.2f€%n", 
                              product, days, vol);
        } else {
            System.out.println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handlePrecoMedio(String product, int days) throws IOException {
        Double avg = client.getAveragePrice(product, days);
        if (avg != null) {
            System.out.printf("Preço médio de '%s' nos últimos %d dias: %.2f€%n", 
                              product, days, avg);
        } else {
            System.out.println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handlePrecoMaximo(String product, int days) throws IOException {
        Double max = client.getMaxPrice(product, days);
        if (max != null) {
            System.out.printf("Preço máximo de '%s' nos últimos %d dias: %.2f€%n", 
                              product, days, max);
        } else {
            System.out.println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handleTodasAgregacoes(String product, int days) throws IOException {
        System.out.println("\n--- Agregações para '" + product + "' (últimos " + days + " dias) ---");
        
        Integer qty = client.getQuantitySold(product, days);
        Double vol = client.getSalesVolume(product, days);
        Double avg = client.getAveragePrice(product, days);
        Double max = client.getMaxPrice(product, days);

        if (qty == null && vol == null && avg == null && max == null) {
            System.out.println("Erro: dados insuficientes ou produto inexistente.");
            return;
        }

        if (qty != null) System.out.printf("  Quantidade vendida: %d unidades%n", qty);
        if (vol != null) System.out.printf("  Volume de vendas:   %.2f€%n", vol);
        if (avg != null) System.out.printf("  Preço médio:        %.2f€%n", avg);
        if (max != null) System.out.printf("  Preço máximo:       %.2f€%n", max);
    }

    private void menuFiltrarEventos() throws IOException {
        System.out.print("Que dia anterior queres ver Eventos? ");
        int dayOffset;
        try {
            dayOffset = Integer.parseInt(sc.nextLine()); 
            if (dayOffset < 1) {
                System.out.println("Dia deve ser >= 1.");
                return;  
            }
        } catch (NumberFormatException e) {
            System.out.println("Número inválido."); 
            return;
        }
    
        System.out.println("Produtos a filtrar (escreve 0 para terminar):");
        Set<String> products = new HashSet<>();

        while (true) {
            System.out.print("  Produto: ");
            String produto = sc.nextLine();
            
            if (produto.equals("0")) {
                break;  // Sai do ciclo
            }
            
            if (!produto.isEmpty()) {
                products.add(produto);
                System.out.println("    ✓ Adicionado: " + produto);
            }
        }

        if (products.isEmpty()) {
            System.out.println("Deve indicar pelo menos um produto.");
            return;
        }

        List<Event> events = client.filterEvents(dayOffset, products);
        if (events == null) {
            System.out.println("Erro: dados insuficientes ou parâmetros inválidos.");
            return;
        }
        
        System.out.println("\n--- Eventos filtrados (dia -" + dayOffset + ") ---");
        if (events.isEmpty()) {
            System.out.println("Nenhum evento encontrado para os produtos indicados.");
        } else {
            for (Event e : events) {
                System.out.printf("  %s: %d unidades a %.2f€%n", 
                    e.getProduct(), e.getQuantity(), e.getPrice());
            }
            System.out.println("Total: " + events.size() + " eventos");
        }
    }
}