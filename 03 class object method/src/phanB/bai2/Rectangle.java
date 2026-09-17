package phanB.bai2;

public class Rectangle {

    private double width;
    private double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public Rectangle(double side) {
        this(side, side);
    }

    public Rectangle() {
        this(1);
    }

    public Rectangle(Rectangle other) {
        this(other.width, other.height);
    }

    public double area() {
        return width * height;
    }

    public double perimeter() {
        return 2 * (width + height);
    }


    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

}
