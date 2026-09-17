package phanB.bai1;

public abstract class Shape {

    public abstract double area();

    public abstract double perimeter();

    public void describe() {
        System.out.printf("%s this shape has an area of %.2f and a perimeter of %.2f.%n", getClass().getSimpleName(), area(), perimeter());
    }
}
