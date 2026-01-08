package Server;

/**
 * Agregações calculadas para um produto num dia específico
 * Classe serve como cache para evitar recalcular agregações várias vezess
 * 
 * Imutável após criação - thread-safe
 */
public class ProductAggregation {
    private final String product;
    private final int totalQuantity;      // Quantidade total vendida
    private final double totalVolume;     // Volume total de vendas em euros
    private final double maxPrice;        // Preço máximo de venda
    private final int eventCount;         // Número de eventos

    public ProductAggregation(String product, int totalQuantity, double totalVolume, 
                              double maxPrice, int eventCount) {
        this.product = product;
        this.totalQuantity = totalQuantity;
        this.totalVolume = totalVolume;
        this.maxPrice = maxPrice;
        this.eventCount = eventCount;
    }

    public String getProduct() {
        return product;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public double getTotalVolume() {
        return totalVolume;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public int getEventCount() {
        return eventCount;
    }

    /**
     * Calcula o preço médio de venda. 0 se não houver eventos
     */
    public double getAveragePrice() {
        if (eventCount == 0) {
            return 0.0;
        }
        return totalVolume / totalQuantity;  
    }

    /**
     * Combina duas agregações do mesmo produto (para agregação multi-dia)
     */
    public ProductAggregation combine(ProductAggregation other) {
        if (other == null) {
            return this;
        }
        return new ProductAggregation(
            this.product,
            this.totalQuantity + other.totalQuantity,
            this.totalVolume + other.totalVolume,
            Math.max(this.maxPrice, other.maxPrice),
            this.eventCount + other.eventCount
        );
    }

    @Override
    public String toString() {
        return String.format("ProductAggregation{product='%s', qty=%d, vol=%.2f€, maxPrice=%.2f€, avgPrice=%.2f€}",
                product, totalQuantity, totalVolume, maxPrice, getAveragePrice());
    }
}
