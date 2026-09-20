package phanB.bai2;

public class Main {

    public static void main(String[] args) {
        Rectangle r1 = new Rectangle();          // vuông cạnh 1
        Rectangle r2 = new Rectangle(4);          // vuông cạnh 4
        Rectangle r3 = new Rectangle(3, 5);       // chữ nhật 3x5
        Rectangle r4 = new Rectangle(r3);         // copy của r3

        System.out.println("r1: " + r1.getWidth() + "x" + r1.getHeight() + " area=" + r1.area() + " perimeter=" + r1.perimeter());
        System.out.println("r2: " + r2.getWidth() + "x" + r2.getHeight() + " area=" + r2.area() + " perimeter=" + r2.perimeter());
        System.out.println("r3: " + r3.getWidth() + "x" + r3.getHeight() + " area=" + r3.area() + " perimeter=" + r3.perimeter());
        System.out.println("r4: " + r4.getWidth() + "x" + r4.getHeight() + " area=" + r4.area() + " perimeter=" + r4.perimeter());

        System.out.println("r3 va r4 la cung 1 object? " + (r3 == r4)); // false - object khac nhau
    }
}
