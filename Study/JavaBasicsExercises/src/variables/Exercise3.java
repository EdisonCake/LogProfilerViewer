package variables;

// Convert a temperature in Celsius to Fahrenheit.
public class Exercise3 {
    public static void main(String[] args) {
        int celsius = 22;
        double fahrenheit = 0;

        System.out.println("It is " + celsius + " Celsius degree.");

        fahrenheit = ((double) (celsius * 9/5)) + 32;

        System.out.println("Fahrenheit: " + fahrenheit);
    }
}
