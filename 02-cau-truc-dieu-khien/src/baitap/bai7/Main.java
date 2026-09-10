package baitap.bai7;

import java.util.HashMap;
import java.util.Map;

public class Main {

    // (a) Đệ quy thô — O(2^n)
    public static long fibNaive(int n) {
        if (n <= 1) return n;                       // base: fib(0)=0, fib(1)=1
        return fibNaive(n - 1) + fibNaive(n - 2);   // tính lại vô số lần
    }

    // (b) Memoization (top-down) — O(n)
    public static long fibMemo(int n, Map<Integer, Long> cache) {
        if (n <= 1) return n;
        Long hit = cache.get(n);
        if (hit != null) return hit;               // đã tính rồi -> lấy ngay
        long v = fibMemo(n - 1, cache) + fibMemo(n - 2, cache);
        cache.put(n, v);                           // lưu lại: mỗi n tính đúng 1 lần
        return v;
    }

    // (c) Bottom-up (khử đệ quy) — O(n) thời gian, O(1) bộ nhớ
    public static long fibLoop(int n) {
        if (n <= 1) return n;
        long a = 0, b = 1;                         // a = fib(i-2), b = fib(i-1)
        for (int i = 2; i <= n; i++) {
            long t = a + b;                        // fib(i)
            a = b;
            b = t;
        }
        return b;
    }

    public static void main(String[] args) {
        int n = 50;

        // ---- Warm-up: cho JIT biên dịch trước khi đo ----
        for (int i = 0; i < 5; i++) {
            fibNaive(30);
            fibMemo(90, new HashMap<>());
            fibLoop(90);
        }

        long t0 = System.nanoTime();
        long r1 = fibNaive(n);
        long t1 = System.nanoTime();
        long r2 = fibMemo(n, new HashMap<>());
        long t2 = System.nanoTime();
        long r3 = fibLoop(n);
        long t3 = System.nanoTime();

        System.out.printf("fibNaive(%d) = %d  |  %,d ns%n", n, r1, t1 - t0);
        System.out.printf("fibMemo (%d) = %d  |  %,d ns%n", n, r2, t2 - t1);
        System.out.printf("fibLoop (%d) = %d  |  %,d ns%n", n, r3, t3 - t2);
    }
}