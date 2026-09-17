package phanB.bai4;

import java.util.Locale;

public class Main {

    // O(n^2): mỗi lần "+=" phải copy toàn bộ chuỗi cũ sang String mới
    static String withPlus() {
        String s = "";
        for (int i = 0; i < 100_000; i++) s += i;
        return s;
    }

    // O(n): StringBuilder tự nhân đôi buffer khi cần, chỉ tốn vài lần resize
    static String withBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100_000; i++) sb.append(i);
        return sb.toString();
    }

    // O(n): cấp sẵn dung lượng đủ lớn (0..99_999 tối đa 6 ký tự/số -> ước lượng 600_000) -> KHÔNG resize lần nào
    static String withBuilderPresized() {
        StringBuilder sb = new StringBuilder(600_000);
        for (int i = 0; i < 100_000; i++) sb.append(i);
        return sb.toString();
    }

    public static void main(String[] args) {
        for (int w = 0; w < 3; w++) { withPlus(); withBuilder(); withBuilderPresized(); } // warm-up cho JIT

        long t0 = System.nanoTime();
        withPlus();
        long tPlus = System.nanoTime() - t0;

        t0 = System.nanoTime();
        withBuilder();
        long tSb = System.nanoTime() - t0;

        t0 = System.nanoTime();
        withBuilderPresized();
        long tSbP = System.nanoTime() - t0;

        System.out.printf(Locale.ROOT, "withPlus            = %8.2f ms  (O(n^2): mỗi += copy toàn bộ chuỗi cũ)%n", tPlus / 1e6);
        System.out.printf(Locale.ROOT, "withBuilder         = %8.2f ms  (O(n): vài lần resize buffer nhân đôi)%n", tSb / 1e6);
        System.out.printf(Locale.ROOT, "withBuilderPresized = %8.2f ms  (O(n): không lần resize nào)%n", tSbP / 1e6);
    }
}
