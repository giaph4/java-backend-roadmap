package phanB.bai1;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Shape> shapes = List.of(
                new Circle(3),
                new Rectangle(4, 5),
                new Triangle(3, 4, 5)
        );

        for (Shape shape : shapes) {
            shape.describe();
        }
    }
}
