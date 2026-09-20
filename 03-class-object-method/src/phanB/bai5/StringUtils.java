package phanB.bai5;

public final class StringUtils {

    private StringUtils() {
        throw new AssertionError("Không được khởi tạo utility class");
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String reverse(String s) {
        if (s == null) return null;
        return new StringBuilder(s).reverse().toString();
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String repeat(String s, int times) {
        if (s == null) return null;
        return s.repeat(Math.max(0, times));
    }
}
