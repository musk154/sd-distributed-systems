package Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TimeSeriesManager {
    
    private final int maxDays;  // D - número máximo de dias históricos a guardar
    private final List<DayData> historicalDays;  // Dias já completos
    private DayData currentDay;  // Dia atual (onde chegam eventos)
    private int currentDayId;    // ID do dia atual
    private final ReentrantReadWriteLock lock;  // Para thread-safety
    
    private static class DayData {
        final int dayId;
        final List<Event> events;
        boolean completed;
        // Cache lazy de agregações por produto (só calculado quando pedido)
        final Map<String, ProductAggregation> aggregationCache;
        
        DayData(int dayId) {
            this.dayId = dayId;
            this.events = new ArrayList<>();
            this.completed = false;
            this.aggregationCache = new ConcurrentHashMap<>();
        }
        
        /**
         * Obtém a agregação para um produto (lazy - calcula na primeira vez).
         * Thread-safe usando ConcurrentHashMap.computeIfAbsent.
         */
        ProductAggregation getAggregation(String product) {
            return aggregationCache.computeIfAbsent(product, this::computeAggregation);
        }
        
        /**
         * Calcula a agregação para um produto percorrendo todos os eventos do dia.
         */
        private ProductAggregation computeAggregation(String product) {
            int totalQty = 0;
            double totalVol = 0.0;
            double maxPrice = Double.MIN_VALUE;
            int count = 0;
            
            for (Event event : events) {
                if (event.getProduct().equals(product)) {
                    totalQty += event.getQuantity();
                    totalVol += event.getQuantity() * event.getPrice();
                    maxPrice = Math.max(maxPrice, event.getPrice());
                    count++;
                }
            }
            
            // Se não há eventos para este produto, retorna agregação vazia
            if (count == 0) {
                return new ProductAggregation(product, 0, 0.0, 0.0, 0);
            }
            
            return new ProductAggregation(product, totalQty, totalVol, maxPrice, count);
        }
        
        /**
         * Limpa o cache de agregações (usado quando o dia é descartado).
         */
        void clearCache() {
            aggregationCache.clear();
        }
    }

    public TimeSeriesManager(int maxDays) {
        if (maxDays < 1) {
            throw new IllegalArgumentException("maxDays deve ser >= 1");
        }
        this.maxDays = maxDays;
        this.historicalDays = new ArrayList<>();
        this.currentDayId = 0;
        this.currentDay = new DayData(currentDayId);
        this.lock = new ReentrantReadWriteLock();
    }

    public void addEvent(String product, int quantity, double price) {
        lock.writeLock().lock();
        try {
            Event event = new Event(product, quantity, price);
            currentDay.events.add(event);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void addEvent(Event event) {
        lock.writeLock().lock();
        try {
            currentDay.events.add(event);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void newDay() {
        lock.writeLock().lock();
        try {
            // 1. Marcar dia atual como completo
            currentDay.completed = true;
            
            // 2. Adicionar ao início do histórico (mais recente primeiro)
            historicalDays.add(0, currentDay);
            
            // 3. Remover dias antigos se exceder o limite D (limpar cache antes)
            while (historicalDays.size() > maxDays) {
                DayData oldDay = historicalDays.remove(historicalDays.size() - 1);
                oldDay.clearCache();  // Descartar cache que já não é relevante
            }
            
            // 4. Criar novo dia corrente
            currentDayId++;
            currentDay = new DayData(currentDayId);
            
            System.out.println("[TIME] Novo dia: " + currentDayId + 
                             " (histórico: " + historicalDays.size() + "/" + maxDays + " dias)");
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public List<Event> getCurrentDayEvents() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(currentDay.events);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getCurrentDayEventCount() {
        lock.readLock().lock();
        try {
            return currentDay.events.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public int getCurrentDayId() {
        lock.readLock().lock();
        try {
            return currentDayId;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public List<List<Event>> getHistoricalEvents(int days) {
        lock.readLock().lock();
        try {
            List<List<Event>> result = new ArrayList<>();
            int count = Math.min(days, historicalDays.size());
            
            for (int i = 0; i < count; i++) {
                // Retorna cópia para evitar modificações externas
                result.add(new ArrayList<>(historicalDays.get(i).events));
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public List<Event> getEventsFromDay(int dayOffset) {
        lock.readLock().lock();
        try {
            int index = dayOffset - 1;
            if (index < 0 || index >= historicalDays.size()) {
                return null;
            }
            return new ArrayList<>(historicalDays.get(index).events);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public List<Event> getEventsByProduct(String product, int days) {
        lock.readLock().lock();
        try {
            List<Event> result = new ArrayList<>();
            int count = Math.min(days, historicalDays.size());
            
            for (int i = 0; i < count; i++) {
                for (Event event : historicalDays.get(i).events) {
                    if (event.getProduct().equals(product)) {
                        result.add(event);
                    }
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public int getMaxDays() {
        return maxDays;  // Imutável, não precisa de lock
    }
    
    public int getHistoricalDayCount() {
        lock.readLock().lock();
        try {
            return historicalDays.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasEnoughHistory(int days) {
        lock.readLock().lock();
        try {
            return historicalDays.size() >= days;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== MÉTODOS DE AGREGAÇÃO (LAZY, COM CACHE) ====================
    
    /**
     * Obtém a agregação combinada para um produto nos últimos d dias.
     * Usa cache por dia - só calcula uma vez por produto/dia.
     * 
     * @param product Nome do produto
     * @param days Número de dias anteriores (1 ≤ d ≤ D)
     * @return Agregação combinada ou null se não houver dados suficientes
     */
    private ProductAggregation getAggregation(String product, int days) {
        lock.readLock().lock();
        try {
            int count = Math.min(days, historicalDays.size());
            if (count == 0) {
                return null;
            }
            
            ProductAggregation result = null;
            
            // Combina agregações de cada dia (usa cache do DayData)
            for (int i = 0; i < count; i++) {
                DayData day = historicalDays.get(i);
                ProductAggregation dayAgg = day.getAggregation(product);
                
                if (result == null) {
                    result = dayAgg;
                } else {
                    result = result.combine(dayAgg);
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Quantidade de vendas: devolve a quantidade de unidades de um produto p 
     * vendidas nos últimos d dias anteriores.
     * 
     * @param product Nome do produto
     * @param days Número de dias (1 ≤ d ≤ D)
     * @return Quantidade vendida, ou -1 se não houver dados suficientes
     */
    public int getQuantitySold(String product, int days) {
        if (days < 1 || days > maxDays) {
            throw new IllegalArgumentException("days deve estar entre 1 e " + maxDays);
        }
        
        ProductAggregation agg = getAggregation(product, days);
        if (agg == null) {
            return -1;  // Não há dados
        }
        return agg.getTotalQuantity();
    }
    
    /**
     * Volume de vendas: devolve o valor total das vendas de um produto p 
     * nos últimos d dias anteriores.
     * 
     * @param product Nome do produto
     * @param days Número de dias (1 ≤ d ≤ D)
     * @return Volume de vendas em €, ou -1 se não houver dados suficientes
     */
    public double getSalesVolume(String product, int days) {
        if (days < 1 || days > maxDays) {
            throw new IllegalArgumentException("days deve estar entre 1 e " + maxDays);
        }
        
        ProductAggregation agg = getAggregation(product, days);
        if (agg == null) {
            return -1.0;
        }
        return agg.getTotalVolume();
    }
    
    /**
     * Preço médio de venda: devolve o preço médio de venda de um produto p 
     * nos últimos d dias anteriores (ponderado pela quantidade).
     * 
     * @param product Nome do produto
     * @param days Número de dias (1 ≤ d ≤ D)
     * @return Preço médio, ou -1 se não houver dados suficientes
     */
    public double getAveragePrice(String product, int days) {
        if (days < 1 || days > maxDays) {
            throw new IllegalArgumentException("days deve estar entre 1 e " + maxDays);
        }
        
        ProductAggregation agg = getAggregation(product, days);
        if (agg == null || agg.getEventCount() == 0) {
            return -1.0;
        }
        return agg.getAveragePrice();
    }
    
    /**
     * Preço máximo de venda: devolve o preço máximo de venda de um produto p 
     * nos últimos d dias anteriores.
     * 
     * @param product Nome do produto
     * @param days Número de dias (1 ≤ d ≤ D)
     * @return Preço máximo, ou -1 se não houver dados suficientes
     */
    public double getMaxPrice(String product, int days) {
        if (days < 1 || days > maxDays) {
            throw new IllegalArgumentException("days deve estar entre 1 e " + maxDays);
        }
        
        ProductAggregation agg = getAggregation(product, days);
        if (agg == null || agg.getEventCount() == 0) {
            return -1.0;
        }
        return agg.getMaxPrice();
    }

}