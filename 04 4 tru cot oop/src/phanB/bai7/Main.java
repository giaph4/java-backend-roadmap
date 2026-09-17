package phanB.bai7;

public class Main {

    sealed interface Shape permits Circle, Square, Rectangle {}

    record Circle(double radius) implements Shape {}
    record Square(double side) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}

    // switch pattern matching KHÔNG có "default" - compiler tự kiểm tra bao phủ hết mọi permits
    // Nếu thêm 1 record mới vào "permits" mà quên xử lý ở đây -> LỖI COMPILE "the switch expression
    // does not cover all possible input values" ngay lập tức, thay vì lỗi ẩn lúc runtime.
    static double area(Shape s) {
        return switch (s) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Square sq -> sq.side() * sq.side();
            case Rectangle r -> r.width() * r.height();
        };
    }

    public static void main(String[] args) {
        Shape[] shapes = { new Circle(2), new Square(3), new Rectangle(4, 5) };
        for (Shape s : shapes) {
            System.out.printf("%s -> area = %.2f%n", s, area(s));
        }

        System.out.println();
        System.out.println("Nếu thêm 'record Triangle(...) implements Shape' vào 'permits Circle, Square, Rectangle'");
        System.out.println("mà KHÔNG thêm case Triangle vào switch ở area(), compiler sẽ báo lỗi ngay khi biên dịch:");
        System.out.println("\"the switch expression does not cover all possible input values\" -");
        System.out.println("khác với interface thường (không sealed) + switch có 'default: throw ...', nơi lỗi");
        System.out.println("chỉ lộ ra lúc RUNTIME khi gặp đúng đối tượng Triangle đầu tiên.");
    }
}
