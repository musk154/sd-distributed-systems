package Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Common.Event;

public class TimeSeriesManager {
    
    private final int maxDays;  // número máximo de dias histórico a guardar
    private final List<DayData> historicalDays;  // Dias completos
    private DayData currentDay;  // Dia atual (onde chegam eventos novos)
    private int currentDayId;    // ID do dia atual
    private final ReentrantReadWriteLock lock; 


    private final List<SimultaneousNotifier> simultaneousNotifiers;
    private final Set<String> productsSoldToday;

    private final List<ConsecutiveNotifier> consecutiveNotifiers;
    private String lastProduct;
    private int consecutiveCount;

    

    private static class SimultaneousNotifier {
        final String p1;
        final String p2;
        final Condition condition;
        boolean satisfied;
        boolean dayEnded;

        SimultaneousNotifier(String p1, String p2, Condition condition) {
            this.p1 = p1;
            this.p2 = p2;
            this.condition = condition;
            this.satisfied = false;
            this.dayEnded = false;
        }
        
        boolean checkCondition(Set<String> soldProducts) {
            return soldProducts.contains(p1) && soldProducts.contains(p2);
        }
    }
    
    private static class ConsecutiveNotifier {
        final int n;
        final Condition condition;
        String satisfiedProduct;
        boolean dayEnded;

        ConsecutiveNotifier(int n, Condition condition) {
            this.n = n;
            this.condition = condition;
            this.satisfiedProduct = null;
            this.dayEnded = false;
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
        
        this.simultaneousNotifiers = new ArrayList<>();
        this.productsSoldToday = new HashSet<>();
        this.consecutiveNotifiers = new ArrayList<>();
        this.lastProduct = null;
        this.consecutiveCount = 0;
    }

    public void addEvent(String product, int quantity, double price) {
        lock.writeLock().lock();
        try {
            Event event = new Event(product, quantity, price);
            currentDay.events.add(event);
            
            // Atualizar produtos vendidos hoje (para simultâneas)
            productsSoldToday.add(product);
            
            // Atualizar contagem consecutiva
            if (product.equals(lastProduct)) {
                consecutiveCount++;
            } else {
                lastProduct = product;
                consecutiveCount = 1;
            }
            // Verificar e acordar notifiers de vendas simultâneas
            for (SimultaneousNotifier waiter : simultaneousNotifiers) {
                if (!waiter.satisfied && waiter.checkCondition(productsSoldToday)) {
                    waiter.satisfied = true;
                    waiter.condition.signal();
                }
            }

            // Verificar e acordar notifiers de vendas consecutivas
            for (ConsecutiveNotifier waiter : consecutiveNotifiers) {
                if (waiter.satisfiedProduct == null && consecutiveCount >= waiter.n) {
                    waiter.satisfiedProduct = product;
                    waiter.condition.signal();
                }
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    
    public void newDay() {
        lock.writeLock().lock();
        try {

            for (SimultaneousNotifier notifier : simultaneousNotifiers) {
                if (!notifier.satisfied) {
                    notifier.dayEnded = true;
                    notifier.condition.signal();
                }
            }
            
            for (ConsecutiveNotifier notifier : consecutiveNotifiers) {
                if (notifier.satisfiedProduct == null) {
                    notifier.dayEnded = true;
                    notifier.condition.signal();
                }
            }

            currentDay.completed = true;
            
            // Adicionar ao início do histórico (mais recente primeiro)
            historicalDays.add(0, currentDay);
            
            // Remover dias antigos se exceder o limite e limpar cache 
            while (historicalDays.size() > maxDays) {
                DayData oldDay = historicalDays.remove(historicalDays.size() - 1);
                oldDay.clearCache();  // Descartar cache que já não é relevante
            }
            
            // Criar um novo dia
            currentDayId++;
            currentDay = new DayData(currentDayId);

            // Reset notificações diárias
            productsSoldToday.clear();
            lastProduct = null;
            consecutiveCount = 0;
            
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
        return maxDays;  
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

    public List<Event> filterEventsByProducts(int dayOffset, Set<String> products) {
        lock.readLock().lock();
        try {
            int index = dayOffset - 1;
            if (index < 0 || index >= historicalDays.size()) {
                return null;
            }
            
            List<Event> result = new ArrayList<>();
            for (Event event : historicalDays.get(index).events) {
                if (products.contains(event.getProduct())) {
                    result.add(event);
                }
            }  
            return result;
        } finally {
            lock.readLock().unlock();
        }

    }

    public boolean waitSimultaneous(String p1, String p2) {
        lock.writeLock().lock();
        try {
            // Verificar se já estão ambos vendidos
            if (productsSoldToday.contains(p1) && productsSoldToday.contains(p2)) {
                return true;
            }
            
            // Criar waiter e registar
            Condition myCondition = lock.writeLock().newCondition();
            SimultaneousNotifier waiter = new SimultaneousNotifier(p1, p2, myCondition);
            simultaneousNotifiers.add(waiter);
            
            try {
                // Esperar até ser acordado
                while (!waiter.satisfied && !waiter.dayEnded) {
                    myCondition.await();
                }
                
                return waiter.satisfied;  // devolve este booleano que é alterado na função add event se for necesário notificação
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } finally {
                simultaneousNotifiers.remove(waiter);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }


    public String waitConsecutive(int n) {
        lock.writeLock().lock();
        try {
            // Verificar se já há n consecutivas
            if (consecutiveCount >= n && lastProduct != null) {
                return lastProduct;
            }
            
            Condition myCondition = lock.writeLock().newCondition();
            ConsecutiveNotifier waiter = new ConsecutiveNotifier(n, myCondition);
            consecutiveNotifiers.add(waiter);

            try {
                while (waiter.satisfiedProduct == null && !waiter.dayEnded) {
                    myCondition.await();
                }

                return waiter.satisfiedProduct;  // mesmo que na função acima

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                consecutiveNotifiers.remove(waiter);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
}