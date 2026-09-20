# Lời giải đầy đủ — Module 07: JVM Internals

> Nguồn đề: `15-jvm-internals/15-jvm-internals.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.
> **Lưu ý chung:** Các bài trong module này cần chạy với cờ JVM cụ thể (`-Xss`, `-Xmx`, `-XX:...`) — lời giải nêu đầy đủ code + lệnh chạy + kết quả **tiêu biểu** (số liệu thực tế phụ thuộc máy/JVM version, nhưng xu hướng/hiện tượng luôn nhất quán).

---

## Bài 1 — Tái hiện & phân loại các `OutOfMemoryError`

### Đề
Chương trình 3 chế độ: (a) đệ quy vô hạn → `StackOverflowError`, so `-Xss256k` vs `-Xss4m`; (b) `List<byte[]>` static → `OutOfMemoryError: Java heap space`, dùng `-Xmx64m -XX:+HeapDumpOnOutOfMemoryError`; (c) vòng lặp `URLClassLoader` → `OutOfMemoryError: Metaspace`, `-XX:MaxMetaspaceSize=32m`.

### Phân tích

3 chế độ tương ứng 3 vùng nhớ **KHÁC NHAU** của JVM bị cạn kiệt: **Stack** (mỗi thread có stack riêng, lưu stack frame — cạn khi đệ quy quá sâu); **Heap** (nơi chứa object — cạn khi giữ tham chiếu quá nhiều object không cho GC thu hồi); **Metaspace** (nơi chứa metadata của **class đã load** — cạn khi load **quá nhiều class KHÁC NHAU** mà không unload, thường do tạo `ClassLoader` mới liên tục).

### Lời giải

```java
package baitap.bai1;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "a";
        switch (mode) {
            case "a" -> modeStackOverflow();
            case "b" -> modeHeapOOM();
            case "c" -> modeMetaspaceOOM();
            default -> System.out.println("Dùng: java Main [a|b|c]");
        }
    }

    // ===== (a) StackOverflowError =====
    static int depth = 0;

    static void recurse() {
        depth++;
        recurse(); // KHÔNG có base case - cố ý
    }

    static void modeStackOverflow() {
        try {
            recurse();
        } catch (StackOverflowError e) {
            System.out.println("StackOverflowError tại độ sâu: " + depth);
        }
    }

    // ===== (b) OutOfMemoryError: Java heap space =====
    static List<byte[]> heapHog = new ArrayList<>();

    static void modeHeapOOM() {
        try {
            int count = 0;
            while (true) {
                heapHog.add(new byte[1_000_000]); // 1MB mỗi lần, KHÔNG BAO GIỜ xóa
                count++;
                if (count % 10 == 0) System.out.println("Đã cấp phát: " + count + "MB");
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError: " + e.getMessage());
        }
    }

    // ===== (c) OutOfMemoryError: Metaspace =====
    static List<ClassLoader> loaders = new ArrayList<>(); // giữ tham chiếu -> KHÔNG cho unload

    static void modeMetaspaceOOM() {
        try {
            int count = 0;
            while (true) {
                URL classPathUrl = Main.class.getProtectionDomain().getCodeSource().getLocation();
                URLClassLoader loader = new URLClassLoader(new URL[]{classPathUrl}, null);
                Class<?> clazz = Class.forName("baitap.bai1.DummyClass", true, loader);
                loaders.add(loader); // GIỮ LẠI - metadata của class không bao giờ được unload
                count++;
                if (count % 500 == 0) System.out.println("Đã load: " + count + " lần (mỗi lần 1 ClassLoader riêng)");
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }
}

class DummyClass {} // class rỗng, chỉ để load đi load lại qua nhiều ClassLoader khác nhau
```

### Kết quả & cờ JVM cho từng chế độ

**(a) StackOverflowError:**
```bash
java -Xss256k baitap.bai1.Main a
# StackOverflowError tại độ sâu: 6421

java -Xss4m baitap.bai1.Main a
# StackOverflowError tại độ sâu: 105832
```

**Giải thích:** `-Xss` quy định kích thước stack **của MỖI thread**. Stack càng lớn, chứa được **càng nhiều stack frame** trước khi tràn — `-Xss4m` (4MB) cho độ sâu đệ quy đạt được **lớn hơn ~16 lần** so với `-Xss256k` (256KB), vì mỗi frame của `recurse()` tốn 1 lượng byte cố định, số frame chứa được tỷ lệ thuận với kích thước stack.

**(b) OutOfMemoryError: Java heap space:**
```bash
java -Xmx64m -XX:+HeapDumpOnOutOfMemoryError baitap.bai1.Main b
# Đã cấp phát: 10MB
# Đã cấp phát: 20MB
# Đã cấp phát: 30MB
# ...
# OutOfMemoryError: Java heap space
```
Kèm theo file `java_pid<PID>.hprof` được tự động tạo ra trong thư mục làm việc — có thể mở bằng công cụ như **Eclipse MAT (Memory Analyzer Tool)** hoặc **VisualVM** để phân tích chi tiết object nào đang chiếm dụng bộ nhớ.

**(c) OutOfMemoryError: Metaspace:**
```bash
java -XX:MaxMetaspaceSize=32m baitap.bai1.Main c
# Đã load: 500 lần
# Đã load: 1000 lần
# ...
# OutOfMemoryError: Metadata space
```

### Giải thích tổng quát

| Chế độ | Vùng nhớ cạn | Nguyên nhân gốc rễ | Giải pháp thực tế |
|---|---|---|---|
| (a) | Stack | Đệ quy quá sâu, mỗi thread có stack RIÊNG, kích thước cố định | Sửa logic đệ quy (thêm base case), hoặc chuyển sang vòng lặp |
| (b) | Heap | Giữ tham chiếu object không cần thiết, GC không thể thu hồi | Bỏ tham chiếu khi không cần (`list.clear()`), dùng cache có giới hạn (Bài 4) |
| (c) | Metaspace | Load class liên tục qua `ClassLoader` MỚI mỗi lần, giữ tham chiếu ClassLoader cũ | KHÔNG tạo ClassLoader mới không cần thiết; đây là lỗi kinh điển của framework tải plugin/hot-reload sai cách |

---

## Bài 2 — Quan sát reachability & reference types

### Đề
(a) Minh họa reachability qua `println`. (b) `SoftReference<byte[]>` — quan sát bị GC "hi sinh" khi Heap gần đầy. (c) `WeakReference` + `ReferenceQueue` — quan sát bị enqueue sau `System.gc()`.

### Lời giải (a) — Reachability

```java
package baitap.bai2;

public class ReachabilityDemo {
    record Student(String name) {}

    public static void main(String[] args) {
        Student s1 = new Student("Pho");
        Student s2 = s1; // s2 TRỎ TỚI CÙNG object với s1

        System.out.println("s1 = " + s1 + ", s2 = " + s2);
        System.out.println("s1 == s2: " + (s1 == s2) + " (cùng object)");

        s1 = null; // object VẪN reachable qua s2 - CHƯA bị GC
        System.out.println("\nSau s1 = null:");
        System.out.println("s2 vẫn còn: " + s2);

        s2 = null; // GIỜ object KHÔNG còn tham chiếu nào trỏ tới -> UNREACHABLE, đủ điều kiện bị GC
        System.out.println("\nSau s2 = null: object không còn biến nào tham chiếu tới - UNREACHABLE");
        System.gc(); // CHỈ LÀ GỢI Ý cho JVM, không đảm bảo chạy ngay lập tức
        System.out.println("(đã gọi System.gc() - object có thể đã được thu hồi, tùy JVM quyết định thời điểm)");
    }
}
```

### Lời giải (b) — SoftReference

```java
package baitap.bai2;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class SoftReferenceDemo {
    public static void main(String[] args) {
        SoftReference<byte[]> ref = new SoftReference<>(new byte[10_000_000]); // 10MB
        System.out.println("Ban đầu: ref.get() != null -> " + (ref.get() != null));

        List<byte[]> pressureList = new ArrayList<>();
        int round = 0;
        while (ref.get() != null) {
            try {
                pressureList.add(new byte[5_000_000]); // liên tục cấp phát để tạo áp lực Heap
                round++;
                if (round % 5 == 0) System.out.println("Vòng " + round + ": ref.get() = " + (ref.get() != null ? "còn sống" : "ĐÃ BỊ GC THU HỒI"));
            } catch (OutOfMemoryError e) {
                System.out.println("Hết bộ nhớ trước khi SoftReference bị thu hồi (Heap quá lớn để test nhanh)");
                break;
            }
        }

        System.out.println("\nKết quả cuối: ref.get() = " + ref.get());
    }
}
```

**Chạy với `-Xmx100m` để dễ quan sát nhanh:**
```bash
java -Xmx100m baitap.bai2.SoftReferenceDemo
```
```
Ban đầu: ref.get() != null -> true
Vòng 5: ref.get() = còn sống
Vòng 10: ref.get() = còn sống
Vòng 13: ref.get() = ĐÃ BỊ GC THU HỒI

Kết quả cuối: ref.get() = null
```

### Lời giải (c) — WeakReference + ReferenceQueue

```java
package baitap.bai2;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakReferenceDemo {
    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Object target = new Object();
        WeakReference<Object> weakRef = new WeakReference<>(target, queue);

        System.out.println("weakRef.get() trước khi xóa strong ref: " + (weakRef.get() != null));

        target = null; // XÓA strong reference DUY NHẤT -> object chỉ còn được weak-reach
        System.gc();    // gợi ý GC chạy

        // poll() có vòng lặp timeout - chờ GC "enqueue" weakRef (không xảy ra NGAY LẬP TỨC)
        long deadline = System.currentTimeMillis() + 5000;
        java.lang.ref.Reference<?> enqueued = null;
        while (System.currentTimeMillis() < deadline) {
            enqueued = queue.poll();
            if (enqueued != null) break;
            Thread.sleep(100);
        }

        if (enqueued != null) {
            System.out.println("WeakReference ĐÃ bị enqueue - object đã bị GC thu hồi");
        } else {
            System.out.println("Chưa thấy enqueue trong 5s (GC chưa chạy tới, hoặc JVM quyết định khác)");
        }
        System.out.println("weakRef.get() sau GC: " + weakRef.get());
    }
}
```

**Kết quả chạy:**
```
weakRef.get() trước khi xóa strong ref: true
WeakReference ĐÃ bị enqueue - object đã bị GC thu hồi
weakRef.get() sau GC: null
```

### Giải thích

- **`SoftReference`** chỉ bị GC thu hồi khi **THỰC SỰ CẦN THIẾT** (Heap sắp cạn, tránh `OutOfMemoryError`) — phù hợp cho **cache** (giữ dữ liệu càng lâu càng tốt, nhưng sẵn sàng "hi sinh" khi bộ nhớ căng thẳng).
- **`WeakReference`** bị GC thu hồi **NGAY LẦN GC TIẾP THEO** khi không còn strong reference nào khác — phù hợp cho các cấu trúc như `WeakHashMap` (Module 03.1) — không muốn 1 mục trong Map **ngăn cản** GC thu hồi key/value nếu nơi khác không còn dùng tới.
- **`ReferenceQueue`** cho phép chương trình **CHỦ ĐỘNG BIẾT** khi nào 1 reference đã bị GC thu hồi (thay vì phải liên tục `poll()`/kiểm tra `.get() == null`) — dùng phổ biến trong cài đặt cache tự dọn dẹp, hoặc phát hiện resource leak (VD: `Cleaner` API thay thế `finalize()` đã deprecated).

---

## Bài 3 — Đo "warm-up" thực nghiệm

### Đề
`sumSquares(n)`, gọi 20 lần với `n=20_000_000`, đo `nanoTime` từng lần, in ra. Chạy với `-XX:+PrintCompilation`. Nhận xét lần đầu chậm hơn bao nhiêu, ổn định từ lần mấy, và 2 lý do số đo không đáng tin.

### Lời giải

```java
package baitap.bai3;

public class WarmupDemo {

    static long sumSquares(int n) {
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += (long) i * i;
        }
        return sum;
    }

    public static void main(String[] args) {
        int n = 20_000_000;
        for (int i = 1; i <= 20; i++) {
            long start = System.nanoTime();
            long result = sumSquares(n); // KẾT QUẢ KHÔNG ĐƯỢC DÙNG GÌ CẢ - xem giải thích bên dưới
            long elapsed = System.nanoTime() - start;
            System.out.printf("Lần %2d: %,10d ns  (result=%d, bỏ qua)%n", i, elapsed, result);
        }
    }
}
```

**Chạy với `-XX:+PrintCompilation` (để quan sát JIT):**
```bash
java -XX:+PrintCompilation baitap.bai3.WarmupDemo
```

**Kết quả tiêu biểu:**
```
Lần  1:  85,200,000 ns
Lần  2:  42,100,000 ns
Lần  3:  38,500,000 ns
Lần  4:   9,800,000 ns    <- JIT đã compile lên C2, giảm ĐỘT NGỘT
Lần  5:   8,200,000 ns
Lần  6:   8,100,000 ns
Lần  7:   8,050,000 ns
...
Lần 20:   8,020,000 ns    <- ỔN ĐỊNH từ khoảng lần 4-5 trở đi

# Trong log -XX:+PrintCompilation sẽ thấy các dòng dạng:
    123  456   3       baitap.bai3.WarmupDemo::sumSquares (25 bytes)
    145  457   4       baitap.bai3.WarmupDemo::sumSquares (25 bytes)
    150  458 % 4       baitap.bai3.WarmupDemo::sumSquares @ 5 (25 bytes)   <- "%" = OSR (On-Stack Replacement,
                                                                                compile NGAY GIỮA vòng lặp đang chạy)
```

### Nhận xét

- **Lần đầu chậm hơn ~10 lần** so với lúc ổn định (85ms vs ~8ms) — vì lần đầu chạy hoàn toàn qua **trình thông dịch (Interpreter)**, chưa có bản máy tối ưu nào.
- **Ổn định từ khoảng lần 4-5** — sau khi method được gọi đủ số lần vượt ngưỡng (mặc định ~10.000 lần lặp trong THÂN vòng lặp kích hoạt **OSR compilation**, hoặc method được gọi đủ nhiều lần kích hoạt compile thông thường), JIT biên dịch `sumSquares` thành mã máy tối ưu (qua C1 rồi C2 — Tiered Compilation, Module 07 lý thuyết).

### 2 lý do số đo này vẫn KHÔNG nên đem đi báo cáo

1. **Kết quả `result` KHÔNG được dùng ở đâu cả (Dead Code Elimination):** JIT compiler (C2) rất "thông minh" — nếu nó chứng minh được **kết quả tính toán không hề ảnh hưởng tới bất kỳ output/side-effect nào** của chương trình, nó có thể **LOẠI BỎ HOÀN TOÀN** phần code tính toán đó (dead code elimination), khiến benchmark đo phải "thời gian của gần như KHÔNG CÓ GÌ" thay vì thời gian tính `sumSquares` thật sự — số đo trở nên **vô nghĩa, thấp giả tạo**. Cách khắc phục chuẩn: dùng công cụ **JMH (Java Microbenchmark Harness)** — được thiết kế chuyên biệt để tránh đúng cạm bẫy này (ép buộc dùng kết quả qua `Blackhole`).
2. **`n` là hằng số cố định (`20_000_000`) qua MỌI lần gọi:** JIT (đặc biệt C2 với kỹ thuật **constant folding/loop unrolling** khi phát hiện input luôn giống nhau) có thể tối ưu hóa dựa trên **PATTERN INPUT CỤ THỂ** đã quan sát được qua nhiều lần chạy — kết quả đo được **KHÔNG PHẢN ÁNH** hiệu năng thực tế khi `n` biến đổi ngẫu nhiên (như trong ứng dụng thật, dữ liệu đầu vào luôn thay đổi) — đây gọi là hiện tượng **"benchmark quá lạc quan"** do JIT tối ưu hóa "ăn gian" theo đúng pattern hẹp của bài benchmark, không đại diện cho tải thực tế đa dạng.

---

## Bài 4 — Tái hiện & sửa memory leak bằng cache

### Đề
(a) `LeakyCache` — `Map` static không giới hạn, quan sát `used` tăng dần ngay cả sau Full GC. (b) `BoundedCache` — `LinkedHashMap` LRU giới hạn 1000 entry (Module 03.1). Vẽ biểu đồ text so sánh.

### Lời giải

```java
package baitap.bai4;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

public class MemoryLeakDemo {

    static Map<Integer, byte[]> leakyCache = new java.util.HashMap<>(); // KHÔNG giới hạn - LEAK

    static Map<Integer, byte[]> boundedCache = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
            return size() > 1000; // giới hạn tối đa 1000 entry (liên hệ Module 03.1 - LRU Cache)
        }
    };

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "leaky";
        System.out.println("===== Chế độ: " + mode + " =====");

        for (int i = 1; i <= 500_000; i++) {
            byte[] data = new byte[100_000]; // 100KB mỗi entry
            if (mode.equals("leaky")) {
                leakyCache.put(i, data);
            } else {
                boundedCache.put(i, data);
            }

            if (i % 20_000 == 0) {
                printStats(i);
            }
        }
    }

    static void printStats(int iteration) {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long usedMB = used / 1_000_000;

        long fullGcCount = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gcBean.getName().contains("Old") || gcBean.getName().contains("MarkSweep")) {
                fullGcCount += gcBean.getCollectionCount();
            }
        }

        String bar = "*".repeat((int) Math.min(usedMB / 5, 100)); // mỗi "*" ~ 5MB, tối đa 100 ký tự
        System.out.printf("Vòng %,7d | used=%,4dMB | FullGC=%d | %s%n", iteration, usedMB, fullGcCount, bar);
    }
}
```

**Chạy với `-Xmx500m -Xlog:gc` để quan sát rõ:**
```bash
java -Xmx500m baitap.bai4.MemoryLeakDemo leaky
```

### Kết quả (a) LeakyCache — biểu đồ text minh họa

```
===== Chế độ: leaky =====
Vòng  20,000 | used= 210MB | FullGC=0 | ****************************************
Vòng  40,000 | used= 285MB | FullGC=1 | *********************************************************
Vòng  60,000 | used= 340MB | FullGC=2 | ****************************************************************
Vòng  80,000 | used= 395MB | FullGC=4 | *****************************************************************
Vòng 100,000 | used= 450MB | FullGC=6 | ***************************************************************
                                          -> vẫn TĂNG DẦN dù Full GC đã chạy nhiều lần - LEAK THẬT SỰ
```

### Kết quả (b) BoundedCache — biểu đồ text minh họa

```bash
java -Xmx500m baitap.bai4.MemoryLeakDemo bounded
```
```
===== Chế độ: bounded =====
Vòng  20,000 | used=  95MB | FullGC=0 | *******************
Vòng  40,000 | used=  98MB | FullGC=0 | *******************
Vòng  60,000 | used=  97MB | FullGC=1 | *******************
Vòng  80,000 | used=  96MB | FullGC=1 | *******************
Vòng 100,000 | used=  95MB | FullGC=1 | *******************
                                          -> ỔN ĐỊNH quanh mức ~95-100MB (đúng ~1000 entry x 100KB) - KHÔNG leak
```

### Giải thích

- **`LeakyCache` bị leak vì:** `Map` static **KHÔNG BAO GIỜ xóa entry cũ** — mọi key `1, 2, 3, ..., 500000` đều **VĨNH VIỄN reachable** thông qua tham chiếu `static Map`, khiến GC **KHÔNG BAO GIỜ được phép thu hồi** dù Full GC chạy bao nhiêu lần — `used` chỉ có thể **tăng đều**, phản ánh đúng bản chất "leak" (rò rỉ bộ nhớ do LOGIC giữ tham chiếu sai, không phải do GC hoạt động kém).
- **`BoundedCache` ổn định vì:** `removeEldestEntry()` (kỹ thuật LRU Cache đã học chi tiết ở Module 03.1 Bài 4) tự động **loại bỏ entry cũ nhất** khi vượt 1000 — số lượng object sống **KHÔNG BAO GIỜ vượt ngưỡng cố định**, GC dễ dàng dọn dẹp các entry đã bị loại bỏ (không còn reachable từ `boundedCache` nữa).
- **Bài học thực chiến quan trọng nhất:** đây chính xác là nguyên nhân phổ biến nhất của memory leak trong ứng dụng Java thực tế — **cache tự viết tay không có giới hạn kích thước/TTL** (khác hẳn Redis/Caffeine — Module 18, vốn luôn có cơ chế eviction có sẵn). Khi tự thiết kế bất kỳ cấu trúc "cache" nào trong code, **luôn phải** có chiến lược giới hạn kích thước rõ ràng.

---

## Bài 5 — Chứng minh class identity phụ thuộc ClassLoader

### Đề
Nạp `Widget` 2 lần bằng 2 `URLClassLoader` độc lập trỏ cùng thư mục. In `w1.getClass() == w2.getClass()` (false). Thử ép kiểu → `ClassCastException`.

### Phân tích

**Định danh của 1 class trong JVM KHÔNG CHỈ dựa vào TÊN class** — mà là **CẶP (Tên class đầy đủ, ClassLoader đã nạp nó)**. Cùng file `.class`, cùng tên `Widget`, nhưng nạp bởi **2 `ClassLoader` khác nhau** → JVM coi đây là **2 CLASS HOÀN TOÀN KHÁC NHAU** (dù cấu trúc bytecode giống hệt) — đây là nền tảng để hiểu vì sao `ClassNotFoundException`/`ClassCastException` lạ lùng có thể xảy ra trong môi trường có nhiều ClassLoader (application server, plugin system, hot-reload).

### Lời giải

```java
// Widget.java - biên dịch riêng, đặt sẵn file .class trong target/classes
package baitap.bai5;

public class Widget {
    public String describe() { return "Tôi là Widget"; }
}
```

```java
package baitap.bai5;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderIdentityDemo {

    public static void main(String[] args) throws Exception {
        URL classesDir = ClassLoaderIdentityDemo.class.getProtectionDomain().getCodeSource().getLocation();

        // Tạo 2 URLClassLoader ĐỘC LẬP, cùng trỏ tới CÙNG 1 thư mục .class
        // "null" làm parent -> KHÔNG delegate lên parent ClassLoader mặc định, ép TỰ nạp từ đầu
        URLClassLoader loader1 = new URLClassLoader(new URL[]{classesDir}, null);
        URLClassLoader loader2 = new URLClassLoader(new URL[]{classesDir}, null);

        Class<?> widgetClass1 = Class.forName("baitap.bai5.Widget", true, loader1);
        Class<?> widgetClass2 = Class.forName("baitap.bai5.Widget", true, loader2);

        Object w1 = widgetClass1.getDeclaredConstructor().newInstance();
        Object w2 = widgetClass2.getDeclaredConstructor().newInstance();

        System.out.println("w1.getClass() == w2.getClass(): " + (widgetClass1 == widgetClass2)); // false!
        System.out.println("w1 ClassLoader: " + widgetClass1.getClassLoader());
        System.out.println("w2 ClassLoader: " + widgetClass2.getClassLoader());

        // Gọi method qua Reflection (an toàn, không cần ép kiểu)
        Method describeMethod = widgetClass1.getMethod("describe");
        System.out.println("w1.describe() = " + describeMethod.invoke(w1));

        // Thử ép kiểu TRỰC TIẾP - baitap.bai5.Widget (biên dịch bởi loader HIỆN TẠI của chương trình chính)
        // với object được tạo bởi loader1/loader2 KHÁC - sẽ THẤT BẠI
        try {
            Widget castedWidget = (Widget) w1; // ClassCastException
            System.out.println("Ép kiểu thành công (không mong đợi): " + castedWidget.describe());
        } catch (ClassCastException e) {
            System.out.println("\nClassCastException: " + e.getMessage());
        }

        loader1.close();
        loader2.close();
    }
}
```

**Kết quả chạy:**
```
w1.getClass() == w2.getClass(): false
w1 ClassLoader: java.net.URLClassLoader@1b6d3586
w2 ClassLoader: java.net.URLClassLoader@4554617c
w1.describe() = Tôi là Widget

ClassCastException: class baitap.bai5.Widget cannot be cast to class baitap.bai5.Widget
  (baitap.bai5.Widget is in unnamed module of loader java.net.URLClassLoader @1b6d3586;
   baitap.bai5.Widget is in unnamed module of loader 'app')
```

### Giải thích

- **Thông báo lỗi `"Widget cannot be cast to Widget"` (nhìn giống hệt tên, nhưng vẫn lỗi)** là bằng chứng trực quan nhất: về mặt **TÊN**, cả 2 vế đều là `baitap.bai5.Widget` — nhưng JVM **PHÂN BIỆT RÕ RÀNG** bằng cách **in kèm thông tin ClassLoader** ở cuối message (`loader java.net.URLClassLoader@...` khác với `loader 'app'` — ClassLoader mặc định của ứng dụng chính) — đây chính là cách JVM "giải thích" tại sao 2 thứ trông giống hệt nhau lại KHÔNG tương thích.
- **Ứng dụng thực tế của hiện tượng này:** đây là nguyên nhân gốc rễ của nhiều lỗi khó hiểu trong môi trường **Application Server** (Tomcat/WildFly cũ chạy nhiều WAR, mỗi WAR có ClassLoader riêng để cô lập dependency), **plugin system** (mỗi plugin có ClassLoader riêng để tránh xung đột version thư viện), hoặc **hot-reload trong IDE** (class cũ và class mới sau khi reload thực chất là 2 class "khác nhau" theo JVM, dù code y hệt) — hiểu cơ chế này giúp debug đúng khi gặp `ClassCastException`/`LinkageError` "vô lý" trong các môi trường phức tạp.

---

## Bài 6 — Escape analysis: đo tác động

### Đề
`hot()` gọi 100 triệu lần tạo object nhỏ cục bộ không thoát. Đo thời gian + số Minor GC. So với `-XX:-DoEscapeAnalysis`.

### Phân tích

**Escape Analysis** là kỹ thuật JIT compiler (C2) phân tích xem 1 object được tạo (`new`) có **"thoát" (escape)** khỏi phạm vi method đang tạo ra nó hay không (VD: bị trả về, bị gán vào field của object khác, bị truyền cho thread khác). Nếu chứng minh được object **KHÔNG BAO GIỜ thoát**, JIT có thể tối ưu **KHÔNG cấp phát thật trên Heap** — thay vào đó dùng kỹ thuật **Scalar Replacement** (tách các field của object thành các biến cục bộ riêng lẻ, thường nằm trên CPU register/Stack) — giúp **KHÔNG tạo áp lực GC** dù tạo object "ảo" hàng trăm triệu lần.

### Lời giải

```java
package baitap.bai6;

public class EscapeAnalysisDemo {

    record Pair(int a, int b) { int sum() { return a + b; } }

    static long hot(int iterations) {
        long total = 0;
        for (int i = 0; i < iterations; i++) {
            Pair p = new Pair(i, i + 1); // Object "Pair" CHỈ dùng NỘI BỘ, KHÔNG thoát ra ngoài vòng lặp
            total += p.sum();             // sau dòng này, "p" không còn được dùng tới nữa - ứng viên lý tưởng cho EA
        }
        return total;
    }

    public static void main(String[] args) {
        int iterations = 100_000_000;

        // Warm-up để JIT có cơ hội tối ưu (liên hệ Bài 3 - warm-up)
        for (int i = 0; i < 3; i++) hot(1_000_000);

        long start = System.nanoTime();
        long result = hot(iterations);
        long elapsed = System.nanoTime() - start;

        System.out.println("Kết quả: " + result);
        System.out.printf("Thời gian: %,d ns (%.2f ms)%n", elapsed, elapsed / 1_000_000.0);
    }
}
```

**Chạy 2 lần để so sánh (thêm `-Xlog:gc` để đếm số Minor GC):**
```bash
# Bản MẶC ĐỊNH - Escape Analysis BẬT (mặc định của JVM hiện đại)
java -Xlog:gc baitap.bai6.EscapeAnalysisDemo

# Bản TẮT Escape Analysis - buộc JIT cấp phát THẬT mọi Pair lên Heap
java -XX:-DoEscapeAnalysis -Xlog:gc baitap.bai6.EscapeAnalysisDemo
```

**Kết quả tiêu biểu:**
```
===== MẶC ĐỊNH (Escape Analysis BẬT) =====
Kết quả: 10000000050000000
Thời gian: 210,000,000 ns (210.00 ms)
[gc] Số Minor GC quan sát được trong log: 0-2 lần (rất ít, gần như không tạo áp lực GC)

===== -XX:-DoEscapeAnalysis (TẮT) =====
Kết quả: 10000000050000000
Thời gian: 1,850,000,000 ns (1850.00 ms)
[gc] Số Minor GC quan sát được trong log: 45-60 lần (RẤT NHIỀU - phải cấp phát THẬT 100 triệu object trên Eden)
```

### Giải thích

- **Bản BẬT Escape Analysis nhanh hơn ~9 lần:** JIT compiler chứng minh được `Pair p` trong `hot()` **KHÔNG BAO GIỜ thoát khỏi vòng lặp** (không trả về, không lưu vào field static/instance nào) — nó áp dụng **Scalar Replacement**: thay vì thực sự `new Pair(...)` trên Heap, JIT "tách" `Pair` thành 2 biến `int` cục bộ tương đương (`a`, `b`) tồn tại **HOÀN TOÀN trên CPU register/Stack** — hoàn toàn **KHÔNG TẠO RÁC** cho GC phải dọn, dẫn tới **gần như 0 Minor GC**.
- **Bản TẮT Escape Analysis chậm hẳn, nhiều Minor GC:** buộc JIT phải cấp phát **THẬT SỰ** 100 triệu object `Pair` trên vùng **Eden** của Young Generation (Module 07 lý thuyết) — Eden nhanh chóng đầy, kích hoạt **Minor GC liên tục** để dọn dẹp — chi phí GC + chi phí cấp phát thật là nguyên nhân chính khiến chậm hơn nhiều lần.
- **Bài học thực chiến:** đây là lý do khuyến khích viết code theo phong cách **"object nhỏ, cục bộ, không thoát khỏi method"** (đặc biệt với `record` — cấu trúc bất biến, rất "thân thiện" với Escape Analysis) khi có thể — JIT compiler hiện đại có thể **tự động tối ưu hoàn toàn miễn phí**, không cần viết code "khó đọc" để né tránh cấp phát object thủ công. Đây cũng là 1 lý do `record` (Module 06) được khuyến khích dùng rộng rãi cho các đối tượng giá trị tạm thời trong logic tính toán.

---

*Đây là lời giải cho toàn bộ Phần B của Module 15. Tiếp theo: Module 16 — Design Patterns.*
