package variables;

// Store the price of a product (double) and the quantity (int). Print the total cost.
public class Exercise2 {
    public static void main(String[] args) {
        double price = 4.59;
        int quantity = 3;

        System.out.println("Price: R$ " + price);
        System.out.println("Quantity: " + quantity);
        System.out.println("===============");
        System.out.println("Total: R$ " + price * quantity);
    }
}
