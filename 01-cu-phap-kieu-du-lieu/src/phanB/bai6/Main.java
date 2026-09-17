package phanB.bai6;

import java.util.Locale;

public class Main {

    // strip() -> hạ chữ thường (Locale.ROOT) -> thay chuỗi khoảng trắng giữa bằng "_"
    static String normalizeUsername(String raw) {
        if (raw == null) return "";
        String t = raw.strip();
        if (t.isEmpty()) return "";
        return t.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    public static void main(String[] args) {
        System.out.println("[" + normalizeUsername("  Pho   Huynh  ") + "]"); // [pho_huynh]
        System.out.println("[" + normalizeUsername("   ") + "]");             // []
        System.out.println("[" + normalizeUsername(null) + "]");              // []
        System.out.println("[" + normalizeUsername("ABC") + "]");             // [abc]
    }
}
