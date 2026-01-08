package Server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Common.*;

public class ClientWorker implements Runnable {
    private final Socket socket;
    private final Auth auth;
    private final ThreadPool pool;
    private final TimeSeriesManager tsManager;
    private Connection conn;
    private User user = null;  
    private final ReadWriteLock userLock = new ReentrantReadWriteLock();  // lock do user para prevenir de 2 threads diferentes tentem mexer no user ao mesmo tempo 

    private void setUser(User u) {
        userLock.writeLock().lock();
        try {
            user = u;
        } finally {
            userLock.writeLock().unlock();
        }
    }

    private User getUser() {
        userLock.readLock().lock();
        try {
            return user;
        } finally {
            userLock.readLock().unlock();
        }
    }

    public ClientWorker (Socket socket, Auth auth, ThreadPool pool, TimeSeriesManager tsManager){
        this.socket=socket;
        this.auth=auth;
        this.pool=pool;
        this.tsManager=tsManager;
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

        if (operationType != Tags.TAG_LOGIN && operationType != Tags.TAG_REGISTER) {  // extra proteção para só analisar pedidos autenticados
            if(getUser() == null){
                dos.writeByte(Tags.STATUS_NOT_AUTHENTICATED);
                conn.send(routingTag, baos.toByteArray());
                return;
            }

        }

        switch (operationType) {
            case Tags.TAG_LOGIN:
                handleLogin(dis, dos);
                break;
                
            case Tags.TAG_REGISTER:
                handleRegister(dis, dos);
                break;
            
            case Tags.TAG_ADD_EVENT:
                handleAddEvent(dis, dos);
                break;
                
            case Tags.TAG_NEW_DAY:
                handleNewDay(dis, dos);
                break;
            case Tags.TAG_GET_CURRENT_DAY:
                handleCurrentDay(dis, dos);
                break;
            
            // Agregações
            case Tags.TAG_AGG_QUANTITY:
                handleAggQuantity(dis, dos);
                break;
            case Tags.TAG_AGG_VOLUME:
                handleAggVolume(dis, dos);
                break;
            case Tags.TAG_AGG_AVG_PRICE:
                handleAggAvgPrice(dis, dos);
                break;
            case Tags.TAG_AGG_MAX_PRICE:
                handleAggMaxPrice(dis, dos);
                break;
                
            default:
                dos.writeByte(Tags.STATUS_ERROR);
        }

        // Envia resposta com a mesma tag (TaggedConnection já tem lock)
        conn.send(routingTag, baos.toByteArray());
    }



    private void handleLogin(DataInputStream dis, DataOutputStream dos) throws IOException {
        String username = dis.readUTF();
        String password = dis.readUTF();
        
        User u = auth.login(username, password);
        if (u != null) {
            setUser(u);
            dos.writeByte(Tags.STATUS_OK);
            dos.writeBoolean(u.admin);
            System.out.println("[AUTH] Login OK: " + username);
        } else {
            dos.writeByte(Tags.STATUS_INVALID_CREDENTIALS);
            System.out.println("[AUTH] Login FALHOU: " + username);
        }
    }

    private void handleRegister(DataInputStream dis, DataOutputStream dos) throws IOException {
        String username = dis.readUTF();
        String password = dis.readUTF();
        
        if (auth.register(username, password)) {
            dos.writeByte(Tags.STATUS_OK);
            System.out.println("[AUTH] Registo OK: " + username);
        } else {
            dos.writeByte(Tags.STATUS_USER_EXISTS);
            System.out.println("[AUTH] Registo FALHOU: " + username);
        }
    }
    
    
    /**
     * Adiciona um evento ao dia corrente.
     * Formato do pedido: [TAG_ADD_EVENT][product:UTF][quantity:int][price:double]
     * Formato da resposta: [STATUS_OK]
     */
    private void handleAddEvent(DataInputStream dis, DataOutputStream dos) throws IOException {
        String product = dis.readUTF();
        int quantity = dis.readInt();
        double price = dis.readDouble();
        
        // Validar parâmetros
        if (product == null || product.isEmpty() || quantity <= 0 || price < 0) {
            dos.writeByte(Tags.STATUS_INVALID_PARAMS);
            return;
        }
        
        // Adicionar evento ao dia corrente
        tsManager.addEvent(product, quantity, price);
        
        dos.writeByte(Tags.STATUS_OK);
        System.out.println("Adicionado: " + product + " " + quantity + " vezes a " + price + "€");
    }

    /**
     * Avança para um novo dia (apenas admin).
     * Formato do pedido: [TAG_NEW_DAY]
     * Formato da resposta: [STATUS_OK][newDayId:int]
     */
    private void handleNewDay(DataInputStream dis, DataOutputStream dos) throws IOException {
        // Verificar se é admin
        User currentUser = getUser();
        if (currentUser == null || !currentUser.admin) {
            dos.writeByte(Tags.STATUS_NOT_ADMIN);
            System.out.println("User ão é admin");
            return;
        }
        
        // Avançar dia
        tsManager.newDay();
        int newDayId = tsManager.getCurrentDayId();
        
        dos.writeByte(Tags.STATUS_OK);
        dos.writeInt(newDayId);
        System.out.println("Novo dia: " + newDayId);
    }

    private void handleCurrentDay(DataInputStream dis, DataOutputStream dos) throws IOException {
        int currentDay = tsManager.getCurrentDayId();
        dos.writeByte(Tags.STATUS_OK);
        dos.writeInt(currentDay);
    }

    // ==================== HANDLERS DE AGREGAÇÃO ====================
    
    /**
     * Quantidade de vendas: devolve a quantidade de unidades de um produto p 
     * vendidas nos últimos d dias anteriores.
     * Formato do pedido: [TAG_AGG_QUANTITY][product:UTF][days:int]
     * Formato da resposta: [STATUS_OK][quantity:int] ou [STATUS_NOT_ENOUGH_DATA]
     */
    private void handleAggQuantity(DataInputStream dis, DataOutputStream dos) throws IOException {
        String product = dis.readUTF();
        int days = dis.readInt();
        
        // Validar parâmetros
        if (product == null || product.isEmpty() || days < 1 || days > tsManager.getMaxDays()) {
            dos.writeByte(Tags.STATUS_INVALID_PARAMS);
            return;
        }
        
        // Verificar se há dados suficientes
        if (!tsManager.hasEnoughHistory(days)) {
            dos.writeByte(Tags.STATUS_NOT_ENOUGH_DATA);
            return;
        }
        
        int quantity = tsManager.getQuantitySold(product, days);
        dos.writeByte(Tags.STATUS_OK);
        dos.writeInt(quantity);
        System.out.println("[AGG] Quantidade " + product + " (" + days + " dias): " + quantity);
    }
    
    /**
     * Volume de vendas: devolve o valor total das vendas de um produto p 
     * nos últimos d dias anteriores.
     * Formato do pedido: [TAG_AGG_VOLUME][product:UTF][days:int]
     * Formato da resposta: [STATUS_OK][volume:double] ou [STATUS_NOT_ENOUGH_DATA]
     */
    private void handleAggVolume(DataInputStream dis, DataOutputStream dos) throws IOException {
        String product = dis.readUTF();
        int days = dis.readInt();
        
        if (product == null || product.isEmpty() || days < 1 || days > tsManager.getMaxDays()) {
            dos.writeByte(Tags.STATUS_INVALID_PARAMS);
            return;
        }
        
        if (!tsManager.hasEnoughHistory(days)) {
            dos.writeByte(Tags.STATUS_NOT_ENOUGH_DATA);
            return;
        }
        
        double volume = tsManager.getSalesVolume(product, days);
        dos.writeByte(Tags.STATUS_OK);
        dos.writeDouble(volume);
        System.out.println("[AGG] Volume " + product + " (" + days + " dias): " + volume + "€");
    }
    
    /**
     * Preço médio de venda: devolve o preço médio de venda de um produto p 
     * nos últimos d dias anteriores.
     * Formato do pedido: [TAG_AGG_AVG_PRICE][product:UTF][days:int]
     * Formato da resposta: [STATUS_OK][avgPrice:double] ou [STATUS_NOT_ENOUGH_DATA]
     */
    private void handleAggAvgPrice(DataInputStream dis, DataOutputStream dos) throws IOException {
        String product = dis.readUTF();
        int days = dis.readInt();
        
        if (product == null || product.isEmpty() || days < 1 || days > tsManager.getMaxDays()) {
            dos.writeByte(Tags.STATUS_INVALID_PARAMS);
            return;
        }
        
        if (!tsManager.hasEnoughHistory(days)) {
            dos.writeByte(Tags.STATUS_NOT_ENOUGH_DATA);
            return;
        }
        
        double avgPrice = tsManager.getAveragePrice(product, days);
        if (avgPrice < 0) {
            // Produto não existe ou sem vendas
            dos.writeByte(Tags.STATUS_NOT_ENOUGH_DATA);
            return;
        }
        
        dos.writeByte(Tags.STATUS_OK);
        dos.writeDouble(avgPrice);
        System.out.println("[AGG] Preço médio " + product + " (" + days + " dias): " + avgPrice + "€");
    }
    
    /**
     * Preço máximo de venda: devolve o preço máximo de venda de um produto p 
     * nos últimos d dias anteriores.
     * Formato do pedido: [TAG_AGG_MAX_PRICE][product:UTF][days:int]
     * Formato da resposta: [STATUS_OK][maxPrice:double] ou [STATUS_NOT_ENOUGH_DATA]
     */
    private void handleAggMaxPrice(DataInputStream dis, DataOutputStream dos) throws IOException {
        String product = dis.readUTF();
        int days = dis.readInt();
        
        if (product == null || product.isEmpty() || days < 1 || days > tsManager.getMaxDays()) {
            dos.writeByte(Tags.STATUS_INVALID_PARAMS);
            return;
        }
        
        if (!tsManager.hasEnoughHistory(days)) {
            dos.writeByte(Tags.STATUS_NOT_ENOUGH_DATA);
            return;
        }
        
        double maxPrice = tsManager.getMaxPrice(product, days);
        if (maxPrice < 0) {
            dos.writeByte(Tags.STATUS_NOT_ENOUGH_DATA);
            return;
        }
        
        dos.writeByte(Tags.STATUS_OK);
        dos.writeDouble(maxPrice);
        System.out.println("[AGG] Preço máximo " + product + " (" + days + " dias): " + maxPrice + "€");
    }
}