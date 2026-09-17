package phanB.bai10;

public class Main {

    // Cách (a) - chuỗi if/else if, trực quan dễ đọc với người mới
    private static String fizzBuzzIfElse(int n) {
        if (n % 15 == 0) return "FizzBuzz";
        if (n % 3 == 0) return "Fizz";
        if (n % 5 == 0) return "Buzz";
        return String.valueOf(n);
    }

    // Cách (b) - switch expression trên mã bitmask (1=chia hết 3, 2=chia hết 5)
    // Gọn hơn khi số điều kiện tăng lên, nhưng "code" (1/2/3) kém tự giải thích hơn nếu không có comment
    private static String fizzBuzzSwitch(int n) {
        int code = (n % 3 == 0 ? 1 : 0) + (n % 5 == 0 ? 2 : 0);
        return switch (code) {
            case 1 -> "Fizz";
            case 2 -> "Buzz";
            case 3 -> "FizzBuzz";
            default -> String.valueOf(n);
        };
    }

    public static void main(String[] args) {
        for (int n = 1; n <= 100; n++) {
            String a = fizzBuzzIfElse(n);
            String b = fizzBuzzSwitch(n);
            if (!a.equals(b)) {
                throw new AssertionError("Hai cách cho kết quả khác nhau tại n=" + n);
            }
            System.out.println(a);
        }
        System.out.println("Hai cách (if/else và switch) cho kết quả GIỐNG NHAU trên toàn bộ 1..100.");
    }
}
