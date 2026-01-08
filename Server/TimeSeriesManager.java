package Server;

import java.util.ArrayList;
import java.util.List;
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
        
        DayData(int dayId) {
            this.dayId = dayId;
            this.events = new ArrayList<>();
            this.completed = false;
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
            
            // 3. Remover dias antigos se exceder o limite D
            while (historicalDays.size() > maxDays) {
                historicalDays.remove(historicalDays.size() - 1);
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

}