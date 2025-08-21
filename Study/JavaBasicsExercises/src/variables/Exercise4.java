package variables;
import static java.lang.Math.PI;

// Given the radius of a circle, calculate and print the area.
public class Exercise4 {
    public static void main(String[] args) {
        double area = 0.00;
        int radius = 5;

        area = PI * radius * radius;
        System.out.println("Area is " + area);
    }
}
