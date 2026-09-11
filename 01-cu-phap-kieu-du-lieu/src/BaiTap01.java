import java.util.Arrays;
import java.util.Locale;

/**
 * Lời giải TẤT CẢ bài tập Module 01.1 — Cú pháp & Kiểu dữ liệu.
 *
 * Chạy:  javac -encoding UTF-8 BaiTap01.java && java BaiTap01
 *
 *   Phần A  — Trắc nghiệm nhận định (Câu 1..12)
 *   Phần B  — Bài tập viết code      (Bài 1..9)
 *   Phần C  — Nâng cao (tư duy JVM)  (Bài 10..15)
 */
public class BaiTap01 {

    public static void main(String[] args) {
        phanA();
        phanB();
        phanC();
    }

    // ==========================================================================
    // PHẦN A — TRẮC NGHIỆM NHẬN ĐỊNH
    // ==========================================================================
    static void phanA() {
        System.out.println("======== PHẦN A ========");

        // ---- Câu 1 ----
        // Integer trong [-128,127] được Integer.valueOf cache -> cùng object.
        // 128 ngoài cache -> 2 object khác nhau -> '==' false; equals() so sánh giá trị.
        Integer a = 127, b = 127, c = 128, d = 128;
        System.out.println("C1: a==b        = " + (a == b));       // true
        System.out.println("C1: c==d        = " + (c == d));       // false
        System.out.println("C1: c.equals(d) = " + c.equals(d));    // true

        // ---- Câu 2 ----
        // KHÔNG compile: 'x' có thể chưa gán khi cond == false
        //   -> "variable x might not have been initialized".
        // Sửa: khởi tạo 'int x = 0;' hoặc thêm nhánh else.
        int x = 0;
        boolean cond = false;
        if (cond) x = 10;
        System.out.println("C2 (đã sửa): x = " + x); // 0

        // ---- Câu 3 ----
        System.out.println("C3: 10/3           = " + (10 / 3));           // 3
        System.out.println("C3: 10/3.0         = " + (10 / 3.0));         // 3.3333333333333335
        System.out.println("C3: -10 % 3        = " + (-10 % 3));          // -1 (dấu theo số bị chia)
        System.out.println("C3: (double)(10/3) = " + (double) (10 / 3));  // 3.0 (10/3 tính ở int trước)
        System.out.println("C3: 1.0/0          = " + (1.0 / 0));          // Infinity
        System.out.println("C3: 0.0/0.0        = " + (0.0 / 0.0));        // NaN

        // ---- Câu 4 ----
        // 'scores[3] = 100;' -> ArrayIndexOutOfBoundsException (index hợp lệ 0..2).
        // Sửa: new int[4].
        int[] scores = new int[4];
        scores[3] = 100;
        System.out.println("C4 (đã sửa): scores = " + Arrays.toString(scores));

        // ---- Câu 5 ----
        String s1 = "backend";
        String s2 = "backend";
        String s3 = new String("backend");
        String s4 = s3.intern();
        String s5 = "back" + "end";          // gộp lúc BIÊN DỊCH
        String pre = "back";
        String s6 = pre + "end";             // 'pre' là biến -> nối RUNTIME -> object mới
        System.out.println("C5: s1==s2 = " + (s1 == s2)); // true
        System.out.println("C5: s1==s3 = " + (s1 == s3)); // false
        System.out.println("C5: s1==s4 = " + (s1 == s4)); // true
        System.out.println("C5: s1==s5 = " + (s1 == s5)); // true
        System.out.println("C5: s1==s6 = " + (s1 == s6)); // false
        System.out.println("C5: tất cả equals() = "
                + (s1.equals(s2) && s1.equals(s3) && s1.equals(s4) && s1.equals(s5) && s1.equals(s6))); // true

        // ---- Câu 6 ----
        int a6 = 100_000, b6 = 100_000;
        long c6 = a6 * b6;          // tính ở int -> TRÀN -> rồi mới gán long
        long d6 = (long) a6 * b6;   // ép 1 toán hạng sang long TRƯỚC
        System.out.println("C6: c = " + c6); // 1410065408  (10_000_000_000 & 0xFFFFFFFF -> vẫn sai/tràn)
        System.out.println("C6: d = " + d6); // 10000000000 (đúng)
        // LƯU Ý: đáp án "-727379968" trong file .md bị sai; giá trị đúng là 1410065408.

        // ---- Câu 7 ----
        byte b7 = 10;
        b7 += 5;                    // b7 = (byte)(b7 + 5)
        System.out.println("C7: b = " + b7);                 // 15
        // byte c7 = 10; c7 = c7 + 5;  // KHÔNG compile: (c7 + 5) là int
        System.out.println("C7: (byte)130 = " + (byte) 130); // -126 (130 - 256)

        // ---- Câu 8 ----
        int i8 = 1;
        int r8 = i8++ + i8++ + ++i8;   // 1 + 2 + 4 = 7
        System.out.println("C8: r = " + r8); // 7
        System.out.println("C8: i = " + i8); // 4

        // ---- Câu 9 ----
        double x9 = 0.1 + 0.2;
        System.out.println("C9: x == 0.3                = " + (x9 == 0.3));                  // false
        System.out.println("C9: Double.compare(x,0.3)==0 = " + (Double.compare(x9, 0.3) == 0)); // false
        System.out.println("C9: abs(x-0.3) < 1e-9       = " + (Math.abs(x9 - 0.3) < 1e-9));  // true
        System.out.println("C9: NaN == NaN              = " + (Double.NaN == Double.NaN));   // false

        // ---- Câu 10 ----
        // Compile ĐƯỢC (covariant: String[] là Object[]). Runtime -> ArrayStoreException.
        Object[] arr = new String[2];
        arr[0] = "ok";
        try {
            arr[1] = Integer.valueOf(1);
        } catch (ArrayStoreException e) {
            System.out.println("C10: ArrayStoreException lúc runtime -> " + e.getMessage());
        }

        // ---- Câu 11 ----
        int v = 5;            addOne(v);
        int[] xs = {1, 2, 3}; fill(xs);
        String name = "Pho";  grow(name);
        System.out.println("C11: v = " + v);         // 5   (primitive sao chép)
        System.out.println("C11: xs[0] = " + xs[0]); // 99  (sửa nội dung qua reference)
        System.out.println("C11: name = " + name);   // Pho (String bất biến)

        // ---- Câu 12 ----
        String emoji = "😀"; // 😀 (ngoài BMP)
        System.out.println("C12: length()       = " + emoji.length());                        // 2
        System.out.println("C12: codePointCount = " + emoji.codePointCount(0, emoji.length())); // 1

        System.out.println();
    }

    static void addOne(int n) { n++; }
    static void fill(int[] arr) { arr[0] = 99; }
    static void grow(String t) { t = t + "!"; }

    // ==========================================================================
    // PHẦN B — BÀI TẬP VIẾT CODE
    // ==========================================================================
    static void phanB() {
        System.out.println("======== PHẦN B ========");

        // ---- Bài 1: tiền điện bậc thang + minh hoạ sai số double ----
        System.out.println("-- Bài 1: tiền điện bậc thang --");
        double[] hoTieuThu = {50, 75.5, 123.4, 10, 250.25, 0.1, 0.2};
        double tongDouble = 0;
        for (double kWh : hoTieuThu) tongDouble += tienDien(kWh);
        System.out.printf(Locale.ROOT, "Tổng (double) = %.10f%n", tongDouble);
        System.out.printf(Locale.ROOT, "tienDien(0.1)+tienDien(0.2) = %.15f  (kỳ vọng %.15f)%n",
                tienDien(0.1) + tienDien(0.2), 0.3 * 1678.0);
        System.out.println("=> giải pháp triệt để: BigDecimal / lưu theo đồng nguyên (module sau).");

        // ---- Bài 2: đảo chuỗi (code point aware) ----
        System.out.println("-- Bài 2: đảo chuỗi --");
        System.out.println(reverseString("Java Backend"));       // dnekcaB avaJ
        System.out.println(reverseString("ab😀cd"));   // dc😀ba (emoji không vỡ)

        // ---- Bài 3: palindrome ----
        System.out.println("-- Bài 3: palindrome --");
        System.out.println(isPalindrome("A man, a plan, a canal: Panama")); // true
        System.out.println(isPalindrome("Backend"));                        // false

        // ---- Bài 4: so hiệu năng nối chuỗi ----
        System.out.println("-- Bài 4: benchmark nối chuỗi (0..99_999) --");
        for (int w = 0; w < 3; w++) { withPlus(); withBuilder(); withBuilderPresized(); } // warm-up
        long t0 = System.nanoTime(); withPlus();            long tPlus = System.nanoTime() - t0;
        t0 = System.nanoTime(); withBuilder();              long tSb   = System.nanoTime() - t0;
        t0 = System.nanoTime(); withBuilderPresized();      long tSbP  = System.nanoTime() - t0;
        System.out.printf(Locale.ROOT, "withPlus            = %8.2f ms  (O(n^2): mỗi += copy toàn bộ chuỗi cũ)%n", tPlus / 1e6);
        System.out.printf(Locale.ROOT, "withBuilder         = %8.2f ms  (O(n): vài lần resize buffer nhân đôi)%n", tSb / 1e6);
        System.out.printf(Locale.ROOT, "withBuilderPresized = %8.2f ms  (O(n): không lần resize nào)%n", tSbP / 1e6);

        // ---- Bài 5: ma trận điểm jagged ----
        System.out.println("-- Bài 5: ma trận điểm (jagged) --");
        int[][] diem = {
                {8, 9, 10},
                {5, 6},
                {},           // rỗng
                null,         // null
                {7, 7, 8, 10}
        };
        double best = Double.NEGATIVE_INFINITY;
        int bestIdx = -1;
        for (int sv = 0; sv < diem.length; sv++) {
            double tb = trungBinh(diem[sv]);
            System.out.printf(Locale.ROOT, "SV %d: TB = %s%n", sv,
                    Double.isNaN(tb) ? "(không có điểm)" : String.format(Locale.ROOT, "%.2f", tb));
            if (!Double.isNaN(tb) && tb > best) { best = tb; bestIdx = sv; }
        }
        System.out.printf(Locale.ROOT, "Cao nhất: SV %d (%.2f)%n", bestIdx, best);

        // ---- Bài 6: chuẩn hóa username ----
        System.out.println("-- Bài 6: normalizeUsername --");
        System.out.println("[" + normalizeUsername("  Pho   Huynh  ") + "]"); // [pho_huynh]
        System.out.println("[" + normalizeUsername("   ") + "]");             // []
        System.out.println("[" + normalizeUsername(null) + "]");              // []

        // ---- Bài 7: cộng an toàn không tràn ----
        System.out.println("-- Bài 7: safeSum / sum --");
        int[] ok = {1, 2, 3, 4, 5};
        int[] tran = {Integer.MAX_VALUE, 1};
        System.out.println("safeSum(ok) = " + safeSum(ok));   // 15
        System.out.println("sum(tran)   = " + sum(tran));     // 2147483648 (long)
        try {
            safeSum(tran);
        } catch (ArithmeticException e) {
            System.out.println("safeSum(tran) -> ArithmeticException: " + e.getMessage());
        }

        // ---- Bài 8: parse cấu hình cổng ----
        System.out.println("-- Bài 8: parsePort --");
        System.out.println(parsePort("8080"));               // 8080
        System.out.println(parsePort("0x1F90"));             // 8080
        System.out.println(parsePort("0b1111110010000"));    // 8080
        try { parsePort("99999"); }
        catch (IllegalArgumentException e) { System.out.println("parsePort(\"99999\") -> " + e.getMessage()); }
        try { parsePort("abc"); }
        catch (IllegalArgumentException e) { System.out.println("parsePort(\"abc\") -> " + e.getMessage()); }

        // ---- Bài 9: bitmask quyền ----
        System.out.println("-- Bài 9: bitmask quyền --");
        int perms = 0;
        perms = grant(perms, READ);
        perms = grant(perms, WRITE);
        System.out.println("sau grant R,W : " + describe(perms) + "  has(EXECUTE)=" + has(perms, EXECUTE)); // rw-
        perms = grant(perms, EXECUTE);
        System.out.println("sau grant X   : " + describe(perms)); // rwx
        perms = revoke(perms, WRITE);
        System.out.println("sau revoke W  : " + describe(perms)); // r-x

        System.out.println();
    }

    // ---- Bài 1 ----
    static double tienDien(double kWh) {
        final double GIA_BAC_1 = 1678.0;
        final double GIA_BAC_2 = 2014.0;
        if (kWh <= 50) return kWh * GIA_BAC_1;
        return 50 * GIA_BAC_1 + (kWh - 50) * GIA_BAC_2;
    }

    // ---- Bài 2 ----
    static String reverseString(String input) {
        if (input == null || input.isEmpty()) return input;
        int[] cps = input.codePoints().toArray();      // tách theo code point -> emoji an toàn
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = cps.length - 1; i >= 0; i--) sb.appendCodePoint(cps[i]);
        return sb.toString();
    }

    // ---- Bài 3 ----
    static boolean isPalindrome(String s) {
        if (s == null) return false;
        int l = 0, r = s.length() - 1;
        while (l < r) {
            char cl = s.charAt(l), cr = s.charAt(r);
            if (!Character.isLetterOrDigit(cl)) { l++; continue; }
            if (!Character.isLetterOrDigit(cr)) { r--; continue; }
            if (Character.toLowerCase(cl) != Character.toLowerCase(cr)) return false;
            l++; r--;
        }
        return true;
    }

    // ---- Bài 4 ----
    static String withPlus() {
        String s = "";
        for (int i = 0; i < 100_000; i++) s += i;       // O(n^2)
        return s;
    }
    static String withBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100_000; i++) sb.append(i); // O(n) + vài lần resize
        return sb.toString();
    }
    static String withBuilderPresized() {
        StringBuilder sb = new StringBuilder(600_000);
        for (int i = 0; i < 100_000; i++) sb.append(i); // O(n), không resize
        return sb.toString();
    }

    // ---- Bài 5 ----
    static double trungBinh(int[] row) {
        if (row == null || row.length == 0) return Double.NaN; // an toàn, không chia 0
        long sum = 0;
        for (int val : row) sum += val;
        return sum / (double) row.length;
    }

    // ---- Bài 6 ----
    static String normalizeUsername(String raw) {
        if (raw == null) return "";
        String t = raw.strip();
        if (t.isEmpty()) return "";
        return t.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    // ---- Bài 7 ----
    static int safeSum(int[] nums) {
        int acc = 0;
        for (int n : nums) acc = Math.addExact(acc, n); // ném ArithmeticException khi tràn
        return acc;
    }
    static long sum(int[] nums) {
        long acc = 0;
        for (int n : nums) acc += n;
        return acc;
    }

    // ---- Bài 8 ----
    static int parsePort(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("Chuỗi cổng rỗng/null");
        String body = s.trim();
        int radix = 10;
        if (body.startsWith("0x") || body.startsWith("0X")) { radix = 16; body = body.substring(2); }
        else if (body.startsWith("0b") || body.startsWith("0B")) { radix = 2; body = body.substring(2); }
        int port;
        try {
            port = Integer.parseInt(body, radix);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Định dạng cổng không hợp lệ: \"" + s + "\"");
        }
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Cổng ngoài khoảng 1..65535: " + port);
        return port;
    }

    // ---- Bài 9 ----
    static final int READ    = 1 << 0;
    static final int WRITE   = 1 << 1;
    static final int EXECUTE = 1 << 2;

    static int grant(int perms, int flag)  { return perms | flag; }
    static int revoke(int perms, int flag) { return perms & ~flag; }
    static boolean has(int perms, int flag) { return (perms & flag) != 0; }
    static String describe(int perms) {
        return (has(perms, READ) ? "r" : "-")
             + (has(perms, WRITE) ? "w" : "-")
             + (has(perms, EXECUTE) ? "x" : "-");
    }

    // ==========================================================================
    // PHẦN C — NÂNG CAO (TƯ DUY JVM / SPEC)
    // ==========================================================================
    static void phanC() {
        System.out.println("======== PHẦN C ========");

        // ---- Bài 10 (giải thích) ----
        System.out.println("Bài 10: 'log += ...' trong 5_000_000 vòng: mỗi lần tạo một String MỚI trên heap");
        System.out.println("  (nối động -> KHÔNG vào String Pool), phải copy toàn bộ chuỗi cũ ngày càng dài");
        System.out.println("  -> O(n^2) thời gian + sinh rác khổng lồ. String cũ lập tức thành rác; tốc độ tạo");
        System.out.println("  rác vượt tốc độ GC -> OutOfMemoryError / GC overhead limit exceeded.");
        System.out.println("  StringBuilder dùng MỘT byte[]/char[] tự nhân đôi capacity, không tạo object mỗi");
        System.out.println("  vòng -> O(n) thời gian, bộ nhớ tuyến tính.");

        // ---- Bài 11 ----
        String a = "hello";
        String b1 = "hel" + "lo";                       // gộp lúc biên dịch
        final String p = "hel"; String b2 = p + "lo";   // p là compile-time constant -> gộp
        String q = "hel";       String b3 = q + "lo";   // q là biến -> nối runtime -> object mới
        String b4 = ("hel" + "lo").intern();            // intern -> reference literal "hello" trong Pool
        String b5 = new String("hello");               // new -> ngoài Pool
        System.out.println("Bài 11: a==b1 = " + (a == b1)); // true
        System.out.println("Bài 11: a==b2 = " + (a == b2)); // true
        System.out.println("Bài 11: a==b3 = " + (a == b3)); // false
        System.out.println("Bài 11: a==b4 = " + (a == b4)); // true
        System.out.println("Bài 11: a==b5 = " + (a == b5)); // false

        // ---- Bài 12 ----
        System.out.println("Bài 12: Math.abs(Integer.MIN_VALUE) = " + Math.abs(Integer.MIN_VALUE)); // -2147483648
        System.out.println("  MIN_VALUE = -2^31; -(-2^31) = 2^31 không tồn tại trong int (max = 2^31-1)");
        System.out.println("  -> tràn bù 2, cuộn về chính -2^31 (số âm).");
        try {
            absExact(Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            System.out.println("  absExact(MIN_VALUE) -> ArithmeticException: " + e.getMessage());
        }
        System.out.println("  absExact(-42) = " + absExact(-42)); // 42

        // ---- Bài 13 ----
        System.out.println("Bài 13: (int) 1e20  = " + (int) 1e20);   // 2147483647 = Integer.MAX_VALUE
        System.out.println("        (long) 1e20 = " + (long) 1e20);  // 9223372036854775807 = Long.MAX_VALUE
        System.out.println("  JLS §5.1.3: double->số nguyên: NaN->0; nếu quá lớn -> KẸP về MIN/MAX kiểu đích.");
        System.out.println("  Cùng luật kẹp, khác NGƯỠNG: 1e20 > Integer.MAX (~2.1e9) và > Long.MAX (~9.2e18)");
        System.out.println("  -> cả hai kẹp về MAX tương ứng (đều DƯƠNG). Thấy số âm là do ép qua int trung");
        System.out.println("  gian hoặc phép nhân tràn, không phải bản thân luật ép double->long.");

        // ---- Bài 14 ----
        System.out.println("Bài 14: '1' + '2'       = " + ('1' + '2'));       // 99 (49+50, char nâng lên int)
        System.out.println("        \"\" + '1' + '2'  = " + ("" + '1' + '2')); // 12 (có String -> nối chuỗi)
        System.out.println("        '1' + 2         = " + ('1' + 2));         // 51 (số học)
        System.out.println("  Không có String thì '+' giữa char/số LUÔN là phép cộng số học (char -> int).");

        // ---- Bài 15 (giải thích) ----
        System.out.println("Bài 15: strictfp buộc mọi phép toán trung gian float/double dùng đúng độ rộng");
        System.out.println("  IEEE 754 (32/64 bit), cấm CPU giữ kết quả trung gian ở thanh ghi 80-bit (x87)");
        System.out.println("  -> cùng biểu thức cho kết quả BIT-FOR-BIT giống nhau trên mọi nền tảng (tính");
        System.out.println("  tái lập). Từ Java 17 (JEP 306) thành mặc định vì phần cứng hiện đại (SSE2) làm");
        System.out.println("  số thực nghiêm ngặt không tốn thêm chi phí -> từ khóa strictfp trở nên thừa.");
    }

    // ---- Bài 12 ----
    static int absExact(int x) {
        if (x == Integer.MIN_VALUE)
            throw new ArithmeticException("overflow: abs(Integer.MIN_VALUE)");
        return Math.abs(x);
    }
}
