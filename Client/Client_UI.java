package Client;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import Common.Event;

public class Client_UI {
    private Client client;
    private Scanner sc = new Scanner(System.in);

    private final ReentrantLock printLock = new ReentrantLock();

    private void print(String msg) {
        printLock.lock();
        try {
            System.out.print(msg);
        } finally {
            printLock.unlock();
        }
    }

    private void println(String msg) {
        printLock.lock();
        try {
            System.out.println(msg);
        } finally {
            printLock.unlock();
        }
    }

    private void println() {
        printLock.lock();
        try {
            System.out.println();
        } finally {
            printLock.unlock();
        }
    }

    private void printf(String format, Object... args) {
        printLock.lock();
        try {
            System.out.printf(format, args);
        } finally {
            printLock.unlock();
        }
    }

    public static void main(String[] args) {
        new Client_UI().run();
    }

    private void run() {
        println("=== Cliente SD ===");
        
        try {
            print("A conectar...");
            client = new Client("localhost", 12345);
            println(" OK!");
        } catch (IOException e) {
            println(" ERRO: " + e.getMessage());
            return;
        }

        menuLogin();
        menuPrincipal();

        try { client.close(); } catch (IOException ignored) {}
    }

    private void menuLogin() {
        while (!client.isAuthenticated()) {
            println("\n=== Registo / Login ===");
            println("0 - Login");
            println("1 - Registar");
            println("2 - Sair");
            print("> ");

            String op = sc.nextLine();
            try {
                if (op.equals("0")) {
                    print("Username: ");
                    String user = sc.nextLine();
                    print("Password: ");
                    String pass = sc.nextLine();
                    
                    if (client.login(user, pass)) {
                        println("Login OK!" + (client.isAdmin() ? " (ADMIN)" : ""));
                    } else {
                        println("Credenciais inválidas.");
                    }
                } else if (op.equals("1")) {
                    print("Username: ");
                    String user = sc.nextLine();
                    print("Password: ");
                    String pass = sc.nextLine();
                    
                    if (client.registar(user, pass)) {
                        println("Registo OK! Pode fazer login.");
                    } else {
                        println("Username já existe.");
                    }
                } else if (op.equals("2")) {
                    println("Adeus!");
                    System.exit(0);
                }
            } catch (IOException e) {
                println("Erro: " + e.getMessage());
            }
        }
    }

    private void menuPrincipal() {
        while (true) {
            println("\n=== Menu Principal ===");
            println("0 - Adicionar evento de venda");
            println("1 - Ver dia atual");
            println("2 - Consultar agregações");
            println("3 - Notificações");
            println("4 - Filtrar eventos por produtos");
            if (client.isAdmin()) {
                println("5 - Avançar dia (ADMIN)");
            }
            println("6 - Sair");
            print("> ");

            String op = sc.nextLine();
            try {
                switch (op) {
                    case "0":
                        menuAddEvent();
                        break;

                    case "1":
                        println("Estamos no dia: " + client.getCurrentDayId());
                        break;

                    case "2":
                        menuAgregacoes();
                        break;

                    case "3":
                        menuNotificacoes();
                        break;

                    case "4":
                        menuFiltrarEventos();
                        break;

                    case "5":
                        if (client.isAdmin()) {
                            menuNewDay();
                        } else {
                            println("Opção inválida.");
                        }
                        break;


                    case "6":
                        println("Adeus!");
                        return;
                        
                    default:
                        println("Opção inválida.");
                }
            } catch (IOException e) {
                println("Erro: " + e.getMessage());
            }
        }
    }

    private void menuAddEvent() throws IOException {
        print("Produto: ");
        String product = sc.nextLine();
        if (product.isEmpty()) {
            println("Produto não pode ser vazio.");
            return;
        }
        
        print("Quantidade: ");
        int quantity;
        try {
            quantity = Integer.parseInt(sc.nextLine());
            if (quantity <= 0) {
                println("Quantidade deve ser > 0.");
                return;
            }
        } catch (NumberFormatException e) {
            println("Quantidade inválida.");
            return;
        }
        
        print("Preço: ");
        double price;
        try {
            price = Double.parseDouble(sc.nextLine().replace(",", "."));
            if (price < 0) {
                println("Preço deve ser >= 0.");
                return;
            }
        } catch (NumberFormatException e) {
            println("Preço inválido.");
            return;
        }
        
        if (client.addEvent(product, quantity, price)) {
            println("Evento adicionado com sucesso!");
            printf("  %s: %d unidades a %.2f€ cada%n", product, quantity, price);
        } else {
            println("Erro ao adicionar evento.");
        }
    }

    private void menuNewDay() throws IOException {
        int newDayId = client.newDay();
        if (newDayId >= 0) {
            println("Novo dia iniciado! Dia atual: " + newDayId);
        } else {
            println("Erro ao avançar dia (apenas admin pode fazer isto).");
        }
    }

    private void menuAgregacoes() throws IOException {
        println("\n=== Agregações (dias anteriores) ===");
        println("0 - Quantidade vendida");
        println("1 - Volume de vendas (valor total)");
        println("2 - Preço médio");
        println("3 - Preço máximo");
        println("4 - Todas as agregações de um produto");
        println("9 - Voltar");
        print("> ");

        String op = sc.nextLine();

        if (op.equals("9")) return;

        print("Produto: ");
        String product = sc.nextLine();
        if (product.isEmpty()) {
            println("Produto não pode ser vazio.");
            return;
        }

        print("Número de dias anteriores: ");
        int days;
        try {
            days = Integer.parseInt(sc.nextLine());
            if (days < 1) {
                println("Dias deve ser >= 1.");
                return;
            }
        } catch (NumberFormatException e) {
            println("Número inválido.");
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
                println("Opção inválida.");
        }
    }

    private void handleQuantidade(String product, int days) throws IOException {
        Integer qty = client.getQuantitySold(product, days);
        if (qty != null) {
            printf("Quantidade de '%s' vendida nos últimos %d dias: %d unidades%n", 
                              product, days, qty);
        } else {
            println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handleVolume(String product, int days) throws IOException {
        Double vol = client.getSalesVolume(product, days);
        if (vol != null) {
            printf("Volume de vendas de '%s' nos últimos %d dias: %.2f€%n", 
                              product, days, vol);
        } else {
            println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handlePrecoMedio(String product, int days) throws IOException {
        Double avg = client.getAveragePrice(product, days);
        if (avg != null) {
            printf("Preço médio de '%s' nos últimos %d dias: %.2f€%n", 
                              product, days, avg);
        } else {
            println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handlePrecoMaximo(String product, int days) throws IOException {
        Double max = client.getMaxPrice(product, days);
        if (max != null) {
            printf("Preço máximo de '%s' nos últimos %d dias: %.2f€%n", 
                              product, days, max);
        } else {
            println("Erro: dados insuficientes ou produto inexistente.");
        }
    }

    private void handleTodasAgregacoes(String product, int days) throws IOException {
        println("\n--- Agregações para '" + product + "' (últimos " + days + " dias) ---");
        
        Integer qty = client.getQuantitySold(product, days);
        Double vol = client.getSalesVolume(product, days);
        Double avg = client.getAveragePrice(product, days);
        Double max = client.getMaxPrice(product, days);

        if (qty == null && vol == null && avg == null && max == null) {
            println("Erro: dados insuficientes ou produto inexistente.");
            return;
        }

        if (qty != null) printf("  Quantidade vendida: %d unidades%n", qty);
        if (vol != null) printf("  Volume de vendas:   %.2f€%n", vol);
        if (avg != null) printf("  Preço médio:        %.2f€%n", avg);
        if (max != null) printf("  Preço máximo:       %.2f€%n", max);
    }

    private void menuFiltrarEventos() throws IOException {
        print("Que dia anterior queres ver Eventos? ");
        int dayOffset;
        try {
            dayOffset = Integer.parseInt(sc.nextLine()); 
            if (dayOffset < 1) {
                println("Dia deve ser >= 1.");
                return;  
            }
        } catch (NumberFormatException e) {
            println("Número inválido."); 
            return;
        }
    
        println("Produtos a filtrar (escreve 0 para terminar):");
        Set<String> products = new HashSet<>();

        while (true) {
            print("  Produto: ");
            String produto = sc.nextLine();
            
            if (produto.equals("0")) {
                break;
            }
            
            if (!produto.isEmpty()) {
                products.add(produto);
                println("    ✓ Adicionado: " + produto);
            }
        }

        if (products.isEmpty()) {
            println("Deve indicar pelo menos um produto.");
            return;
        }

        List<Event> events = client.filterEvents(dayOffset, products);
        if (events == null) {
            println("Erro: dados insuficientes ou parâmetros inválidos.");
            return;
        }
        
        println("\n--- Eventos filtrados (dia -" + dayOffset + ") ---");
        if (events.isEmpty()) {
            println("Nenhum evento encontrado para os produtos indicados.");
        } else {
            for (Event e : events) {
                printf("  %s: %d unidades a %.2f€%n", 
                    e.getProduct(), e.getQuantity(), e.getPrice());
            }
            println("Total: " + events.size() + " eventos");
        }
    }
    
    private void menuNotificacoes() throws IOException {
        println("\n=== Notificações (dia corrente) ===");
        println("0 - Esperar vendas simultâneas de 2 produtos");
        println("1 - Esperar N vendas consecutivas");
        println("2 - Voltar");
        print("> ");

        String op = sc.nextLine();

        switch (op) {
            case "0":
                menuWaitSimultaneous();
                break;
            case "1":
                menuWaitConsecutive();
                break;
            case "2":
                return;
            default:
                println("Opção inválida.");
        }
    }
    
    private void menuWaitSimultaneous() throws IOException {
        println("\n--- Vendas Simultâneas ---");
        println("Vai bloquear até AMBOS os produtos serem vendidos hoje,");
        println("ou até o dia terminar.");
        println();
        
        print("Produto 1: ");
        String p1 = sc.nextLine();
        if (p1.isEmpty()) {
            println("Produto não pode ser vazio.");
            return;
        }
        
        print("Produto 2: ");
        String p2 = sc.nextLine();
        if (p2.isEmpty()) {
            println("Produto não pode ser vazio.");
            return;
        }
        
        println();
        println("A esperar que '" + p1 + "' e '" + p2 + "' sejam vendidos...");
        println();
        
        new Thread(() -> {
            try {
                Boolean result = client.waitSimultaneous(p1, p2);
                
                println();
                println("=== NOTIFICAÇÃO RECEBIDA ===");
                if (result == null) {
                    println("Erro na operação de vendas simultâneas.");
                } else if (result) {
                    println( p1 + "' e '" + p2 + "' foram vendidos hoje!");
                } else {
                    println("O dia terminou antes de ambos serem vendidos.");
                }
                println("============================");
                print("> "); 
            } catch (IOException e) {
                println("Erro na notificação: " + e.getMessage());
            }
        }).start();
    }
    
    private void menuWaitConsecutive() throws IOException {
        println("\n--- Vendas Consecutivas ---");
        println("Vai bloquear até haver N vendas SEGUIDAS do mesmo produto,");
        println("ou até o dia terminar.");
        println();
        
        print("Número de vendas consecutivas (N): ");
        int n;
        try {
            n = Integer.parseInt(sc.nextLine());
            if (n < 1) {
                println("N deve ser >= 1.");
                return;
            }
        } catch (NumberFormatException e) {
            println("Número inválido.");
            return;
        }
        
        println();
        println("A esperar por " + n + " vendas consecutivas do mesmo produto...");
        println();
        
        final int finalN = n;
        new Thread(() -> {
            try {
                String result = client.waitConsecutive(finalN);
                
                println();
                println("=== NOTIFICAÇÃO RECEBIDA ===");
                if (result != null) {
                    println("Houve " + finalN + " vendas consecutivas de: " + result);
                } else {
                    println("O dia terminou antes de haver " + finalN + " vendas consecutivas.");
                }
                println("============================");
                print("> "); 
            } catch (IOException e) {
                println("Erro na notificação: " + e.getMessage());
            }
        }).start();
    }
}