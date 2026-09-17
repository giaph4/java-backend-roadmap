package phanB.bai8;

public class Main {

    // Nhận "8080", "0x1F90", "0b1111110010000" -> tách tiền tố rồi Integer.parseInt(s, radix)
    static int parsePort(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Chuỗi cổng rỗng/null");
        }

        String body = s.trim();
        int radix = 10;
        if (body.startsWith("0x") || body.startsWith("0X")) {
            radix = 16;
            body = body.substring(2);
        } else if (body.startsWith("0b") || body.startsWith("0B")) {
            radix = 2;
            body = body.substring(2);
        }

        int port;
        try {
            port = Integer.parseInt(body, radix);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Định dạng cổng không hợp lệ: \"" + s + "\"");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Cổng ngoài khoảng 1..65535: " + port);
        }
        return port;
    }

    public static void main(String[] args) {
        System.out.println(parsePort("8080"));               // 8080
        System.out.println(parsePort("0x1F90"));             // 8080
        System.out.println(parsePort("0b1111110010000"));    // 8080

        try {
            parsePort("99999");
        } catch (IllegalArgumentException e) {
            System.out.println("parsePort(\"99999\") -> " + e.getMessage());
        }

        try {
            parsePort("abc");
        } catch (IllegalArgumentException e) {
            System.out.println("parsePort(\"abc\") -> " + e.getMessage());
        }
    }
}
