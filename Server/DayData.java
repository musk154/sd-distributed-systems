package Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Common.Event;


public class DayData {
        final int dayId;
        final List<Event> events;
        boolean completed;
        // Cache lazy de agregações por produto 
        final Map<String, ProductAggregation> aggregationCache;
        final ReentrantReadWriteLock cacheLock;

        DayData(int dayId) {
            this.dayId = dayId;
            this.events = new ArrayList<>();
            this.completed = false;
            this.aggregationCache = new HashMap<>();
            this.cacheLock = new ReentrantReadWriteLock();
        }
        
        ProductAggregation getAggregation(String product) {
            // Primeiro tenta ler do cache
            cacheLock.readLock().lock();
            try {
                ProductAggregation agg = aggregationCache.get(product);
                if (agg != null) {
                    return agg;  // Cache hit - devolve logo
                }
            } finally {
                cacheLock.readLock().unlock();
            }
            
            // Cache miss - precisa de escrever
            cacheLock.writeLock().lock();
            try {
                ProductAggregation agg = aggregationCache.get(product);
                if (agg == null) {
                    agg = calculateAggregation(product);
                    aggregationCache.put(product, agg);
                }
                return agg;
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        
        private ProductAggregation calculateAggregation(String product) {
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
        
        void clearCache() {
            cacheLock.writeLock().lock();
            try {
                aggregationCache.clear();
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
    }