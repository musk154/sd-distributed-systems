package Server;

import java.io.DataOutputStream;
import java.io.IOException;

public class Event {
    private final String product;
    private final int quantity;
    private final double price;

    public Event (String product, int quantity, double price){
        this.product=product;
        this.quantity=quantity;
        this.price=price;
    }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(product);
        out.writeInt(quantity);
        out.writeDouble(price);
    }
    public Event readFrom(java.io.DataInputStream dis) throws IOException {
        String product = dis.readUTF();
        int quantity = dis.readInt();
        double price = dis.readDouble();
        return new Event(product, quantity, price);
    }
}
