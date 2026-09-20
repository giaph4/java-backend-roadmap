# Lời giải đầy đủ — Module 05.2: Concurrency Utilities

> Nguồn đề: `13-concurrency-utilities/13-concurrency-utilities.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Song song vs tuần tự

### Đề
Mô phỏng 5 "API" (`sleep` ngẫu nhiên 500–2000ms). Gọi song song bằng `newFixedThreadPool(5)` + `invokeAll`, so với tuần tự. Đóng pool bằng mẫu 2 pha. Giải thích vì sao thời gian song song ≈ API chậm nhất.

### Phân tích

`ExecutorService.invokeAll(tasks)` submit **TẤT CẢ task cùng lúc**, chạy song song (nếu pool đủ thread), rồi **CHẶN (block)** tới khi **TOÀN BỘ** task hoàn tất mới trả về danh sách `Future`. Vì 5 task chạy **ĐỒNG THỜI**, tổng thời gian chỉ phụ thuộc vào task **CHẬM NHẤT** — không phải tổng của cả 5 như chạy tuần tự.

### Lời giải

```java
package baitap.bai1;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class Main {

    static int callApi(int id) {
        Random rnd = new Random();
        int delay = 500 + rnd.nextInt(1500); // 500-2000ms
        try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("API " + id + " hoàn tất sau " + delay + "ms");
        return id * 100;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // ===== Tuần tự =====
        System.out.println("===== Tuần tự =====");
        long startSeq = System.currentTimeMillis();
        int totalSeq = 0;
        for (int i = 1; i <= 5; i++) {
            totalSeq += callApi(i);
        }
        long timeSeq = System.currentTimeMillis() - startSeq;
        System.out.println("Tổng: " + totalSeq + ", thời gian: " + timeSeq + "ms\n");

        // ===== Song song =====
        System.out.println("===== Song song =====");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Callable<Integer>> tasks = List.of(
                () -> callApi(1), () -> callApi(2), () -> callApi(3), () -> callApi(4), () -> callApi(5));

        long startPar = System.currentTimeMillis();
        List<Future<Integer>> futures = executor.invokeAll(tasks); // block tới khi TẤT CẢ xong
        int totalPar = 0;
        for (Future<Integer> f : futures) {
            totalPar += f.get();
        }
        long timePar = System.currentTimeMillis() - startPar;
        System.out.println("Tổng: " + totalPar + ", thời gian: " + timePar + "ms");

        // Mẫu đóng ExecutorService "2 pha" chuẩn
        shutdownGracefully(executor);

        System.out.println("\nSo sánh: tuần tự=" + timeSeq + "ms, song song=" + timePar + "ms");
    }

    static void shutdownGracefully(ExecutorService executor) {
        executor.shutdown(); // Pha 1: KHÔNG nhận task mới, task đang chạy vẫn tiếp tục
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Pha 2: hết thời gian chờ -> ép dừng ngay các task còn lại
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

**Kết quả tiêu biểu (thời gian delay ngẫu nhiên mỗi lần chạy khác nhau, nhưng xu hướng luôn nhất quán):**
```
===== Tuần tự =====
API 1 hoàn tất sau 1200ms
API 2 hoàn tất sau 800ms
API 3 hoàn tất sau 1900ms
API 4 hoàn tất sau 600ms
API 5 hoàn tất sau 1400ms
Tổng: 1500, thời gian: 5900ms

===== Song song =====
API 4 hoàn tất sau 650ms
API 2 hoàn tất sau 900ms
API 1 hoàn tất sau 1250ms
API 5 hoàn tất sau 1450ms
API 3 hoàn tất sau 1950ms
Tổng: 1500, thời gian: 1955ms

So sánh: tuần tự=5900ms, song song=1955ms
```

### Giải thích

- **Tuần tự:** tổng thời gian = **TỔNG** thời gian từng API (1200+800+1900+600+1400 ≈ 5900ms) — vì mỗi API phải **CHỜ** API trước đó xong mới bắt đầu.
- **Song song (5 thread cho 5 task):** cả 5 API **bắt đầu gần như CÙNG LÚC**, chạy độc lập trên 5 thread riêng — tổng thời gian **chỉ phụ thuộc API CHẬM NHẤT** (ở đây API 3, 1950ms) — các API khác dù xong sớm hơn cũng **không rút ngắn** được tổng thời gian, vì `invokeAll` phải chờ **ĐỦ CẢ 5**.
- **Điều kiện để đạt hiệu quả này:** pool phải có **ĐỦ thread** cho số task (ở đây `newFixedThreadPool(5)` cho đúng 5 task) — nếu pool chỉ có 2 thread cho 5 task, 1 số task phải **xếp hàng chờ**, thời gian tổng sẽ **KHÔNG** chỉ phụ thuộc task chậm nhất mà còn phụ thuộc cách các task được phân bổ vào 2 "làn" chạy.
- **Mẫu đóng ExecutorService "2 pha"** (`shutdown()` rồi `awaitTermination()`, nếu timeout mới `shutdownNow()`) đảm bảo: (1) không nhận task mới, (2) cho task **đang chạy dở** có cơ hội hoàn tất trong thời gian hợp lý, (3) nếu quá lâu, **ép buộc** dừng để tránh treo ứng dụng vô thời hạn — đây là mẫu hình chuẩn khi dừng ứng dụng (liên hệ Graceful Shutdown ở Module 22 Spring Boot).

---

## Bài 2 — CompletableFuture pipeline + xử lý lỗi

### Đề
Pipeline: `validate()` → `calcTotal()` → `charge()` → `sendEmail()`, mỗi bước `sleep(300)`. Dùng `thenApply`/`thenCompose`. `exceptionally` xử lý `validate()` ném → trả "đã hủy". Chạy trên executor riêng, không dùng commonPool.

### Phân tích

`thenApply` dùng khi bước tiếp theo là **hàm đồng bộ đơn giản** (`Function<T, R>`, không trả `CompletableFuture`). `thenCompose` dùng khi bước tiếp theo **CHÍNH NÓ trả về 1 `CompletableFuture`** (tránh lồng `CompletableFuture<CompletableFuture<T>>`, tương tự `flatMap` của Stream). `exceptionally` bắt exception xảy ra **BẤT KỲ ĐÂU** trong chuỗi phía trước, cho phép trả về **giá trị thay thế** để pipeline tiếp tục thay vì crash.

### Lời giải

```java
package baitap.bai2;

import java.util.concurrent.*;

public class Main {

    record OrderRequest(int orderId, double amount, boolean valid) {}

    static ExecutorService executor = Executors.newFixedThreadPool(4); // Executor RIÊNG, không dùng commonPool

    static CompletableFuture<OrderRequest> validate(OrderRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            sleepQuietly(300);
            if (!req.valid()) {
                throw new IllegalStateException("Đơn hàng #" + req.orderId() + " không hợp lệ");
            }
            System.out.println("[validate] Đơn #" + req.orderId() + " hợp lệ");
            return req;
        }, executor);
    }

    static double calcTotal(OrderRequest req) {
        sleepQuietly(300);
        double total = req.amount() * 1.1; // +10% thuế
        System.out.println("[calcTotal] Đơn #" + req.orderId() + " total=" + total);
        return total;
    }

    static CompletableFuture<Boolean> charge(double total) {
        return CompletableFuture.supplyAsync(() -> {
            sleepQuietly(300);
            System.out.println("[charge] Đã thu " + total);
            return true;
        }, executor);
    }

    static String sendEmail(boolean chargeSuccess) {
        sleepQuietly(300);
        String result = chargeSuccess ? "Đã gửi email xác nhận thành công" : "Không gửi email vì thanh toán thất bại";
        System.out.println("[sendEmail] " + result);
        return result;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===== Đơn hàng HỢP LỆ =====");
        CompletableFuture<String> pipeline1 = validate(new OrderRequest(1, 100_000, true))
                .thenApply(Main::calcTotal)      // bước đồng bộ đơn giản -> thenApply
                .thenCompose(Main::charge)       // charge() trả CompletableFuture -> thenCompose (tránh lồng nhau)
                .thenApply(Main::sendEmail)
                .exceptionally(ex -> "Đơn hàng đã hủy: " + ex.getMessage());

        System.out.println("Kết quả: " + pipeline1.get());

        System.out.println("\n===== Đơn hàng KHÔNG hợp lệ =====");
        CompletableFuture<String> pipeline2 = validate(new OrderRequest(2, 50_000, false))
                .thenApply(Main::calcTotal)
                .thenCompose(Main::charge)
                .thenApply(Main::sendEmail)
                .exceptionally(ex -> "Đơn hàng đã hủy: " + ex.getMessage());

        System.out.println("Kết quả: " + pipeline2.get());

        executor.shutdown();
    }

    static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

**Kết quả chạy:**
```
===== Đơn hàng HỢP LỆ =====
[validate] Đơn #1 hợp lệ
[calcTotal] Đơn #1 total=110000.00000000001
[charge] Đã thu 110000.00000000001
[sendEmail] Đã gửi email xác nhận thành công
Kết quả: Đã gửi email xác nhận thành công

===== Đơn hàng KHÔNG hợp lệ =====
Kết quả: Đơn hàng đã hủy: java.lang.IllegalStateException: Đơn hàng #2 không hợp lệ
```

### Giải thích

- **Đơn hàng #2 KHÔNG in ra bất kỳ dòng `[calcTotal]`/`[charge]`/`[sendEmail]` nào** — vì `validate()` ném exception **ngay từ đầu**, toàn bộ chuỗi `thenApply`/`thenCompose` phía sau **tự động bị "nhảy qua"** (short-circuit), chỉ `exceptionally` mới được thực thi — đúng cơ chế lan truyền lỗi qua chuỗi bất đồng bộ, tương tự try/catch nhưng ở dạng khai báo (declarative) cho pipeline `CompletableFuture`.
- **`thenApply` vs `thenCompose`:** nếu dùng `thenApply(Main::charge)` thay vì `thenCompose`, kết quả sẽ là `CompletableFuture<CompletableFuture<Boolean>>` (2 tầng lồng nhau, rất khó dùng tiếp) — `thenCompose` "làm phẳng" đúng 1 tầng, y hệt vai trò `flatMap` trong Stream API (Module 03.3).
- **Executor riêng (không dùng `commonPool`):** `CompletableFuture.supplyAsync(fn)` nếu **không truyền Executor**, mặc định chạy trên `ForkJoinPool.commonPool()` — pool **dùng chung cho TOÀN BỘ ứng dụng** (kể cả `parallelStream()`). Trong ứng dụng Backend thực tế, dùng executor riêng cho từng loại tác vụ giúp **cô lập tài nguyên** — 1 pipeline bị chậm/nghẽn không "cướp" hết thread của các tác vụ khác đang dùng `commonPool`.

---

## Bài 3 — allOf thu kết quả

### Đề
10 `id`. `fetchAsync(id)` trả `CompletableFuture<Integer>`. Gọi song song, `allOf(...).thenApply` thu `List<Integer>` đúng thứ tự, tính tổng. `orTimeout(3, SECONDS)` cho từng future, timeout → giá trị 0.

### Phân tích

`CompletableFuture.allOf(futures...)` trả về `CompletableFuture<Void>` — chỉ báo hiệu "TẤT CẢ đã xong", **KHÔNG tự thu thập kết quả**. Muốn lấy `List<T>` kết quả **đúng thứ tự ban đầu**, phải tự `.join()` từng future **SAU KHI** `allOf` xác nhận hoàn tất (lúc này `join()` không còn block vì đã chắc chắn xong). `orTimeout(duration, unit)` (Java 9+) khiến future tự động "hoàn tất với lỗi" (`TimeoutException`) nếu quá thời gian — kết hợp `exceptionally` để thay bằng giá trị mặc định.

### Lời giải

```java
package baitap.bai3;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {

    static ExecutorService executor = Executors.newFixedThreadPool(10);

    static CompletableFuture<Integer> fetchAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            Random rnd = new Random();
            int delay = 500 + rnd.nextInt(4000); // 500-4500ms - CỐ Ý có vài cái vượt ngưỡng 3s để test timeout
            try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return id * 10;
        }, executor);
    }

    public static void main(String[] args) {
        List<Integer> ids = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // Mỗi future có orTimeout riêng, exceptionally thay bằng 0 nếu timeout
        List<CompletableFuture<Integer>> futures = ids.stream()
                .map(id -> fetchAsync(id)
                        .orTimeout(3, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            System.out.println("ID " + id + " bị TIMEOUT -> dùng giá trị 0");
                            return 0;
                        }))
                .collect(Collectors.toList());

        // allOf chỉ báo "xong hết", KHÔNG tự thu kết quả
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        CompletableFuture<List<Integer>> allResults = allDone.thenApply(v ->
                futures.stream().map(CompletableFuture::join).collect(Collectors.toList())); // ĐÚNG THỨ TỰ, vì futures là List đã theo thứ tự ids

        List<Integer> results = allResults.join();
        System.out.println("Kết quả (theo đúng thứ tự id): " + results);
        System.out.println("Tổng: " + results.stream().mapToInt(Integer::intValue).sum());

        executor.shutdown();
    }
}
```

**Kết quả tiêu biểu (số bị timeout ngẫu nhiên tùy lần chạy):**
```
ID 3 bị TIMEOUT -> dùng giá trị 0
ID 7 bị TIMEOUT -> dùng giá trị 0
Kết quả (theo đúng thứ tự id): [10, 20, 0, 40, 50, 60, 0, 80, 90, 100]
Tổng: 450
```

### Giải thích

- **Vì sao kết quả LUÔN đúng thứ tự dù các future hoàn tất KHÔNG theo thứ tự:** `futures` là 1 `List` được xây dựng **THEO ĐÚNG THỨ TỰ `ids`** (`map` giữ nguyên thứ tự phần tử — đặc tính cơ bản của Stream `map`) — dù các future bên trong hoàn tất **sớm/muộn khác nhau** (do `sleep` ngẫu nhiên), khi duyệt lại `futures.stream().map(CompletableFuture::join)`, ta luôn `join()` **theo đúng vị trí trong List gốc**, không phải theo thứ tự hoàn tất thực tế.
- **`.join()` sau `allOf` không bị block lâu:** vì `allDone.thenApply(...)` **chỉ chạy SAU KHI** `allOf` xác nhận **TẤT CẢ** future đã hoàn tất (dù thành công hay lỗi) — lúc gọi `.join()` từng cái, giá trị **đã sẵn sàng ngay lập tức**, không phải chờ đợi gì thêm.
- `orTimeout` + `exceptionally` là cặp đôi rất thực dụng cho các lời gọi service bên ngoài không đáng tin cậy về thời gian phản hồi — tránh 1 API chậm làm "treo" toàn bộ pipeline vô thời hạn, đồng thời có **giá trị fallback rõ ràng** thay vì để exception lan truyền và làm hỏng cả `allOf`.

---

## Bài 4 — start/end gate benchmark

### Đề
Đo throughput `AtomicLong.incrementAndGet()` vs `LongAdder.increment()` vs `synchronized` counter, 8 thread × 1.000.000 lần. `CountDownLatch` start gate + end gate. Giải thích vì sao `LongAdder` thắng khi tranh chấp cao.

### Phân tích

**Start gate** (`CountDownLatch(1)`): mọi thread **CHỜ** tại vạch xuất phát, chỉ bắt đầu **ĐỒNG LOẠT** khi latch đếm về 0 — đảm bảo đo công bằng, không có thread nào "chạy trước" trong lúc các thread khác còn đang khởi động. **End gate** (`CountDownLatch(threadCount)`): main chờ **TẤT CẢ** thread báo hoàn tất mới dừng đồng hồ đo.

### Lời giải

```java
package baitap.bai4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class Main {

    static final int THREAD_COUNT = 8;
    static final int OPS_PER_THREAD = 1_000_000;

    public static void main(String[] args) throws InterruptedException {
        long atomicTime = benchmarkAtomicLong();
        long adderTime = benchmarkLongAdder();
        long syncTime = benchmarkSynchronized();

        long totalOps = (long) THREAD_COUNT * OPS_PER_THREAD;
        System.out.println("\n===== Bảng kết quả (ns/op) =====");
        System.out.printf("AtomicLong:   %,d ns tổng | %.2f ns/op%n", atomicTime, (double) atomicTime / totalOps);
        System.out.printf("LongAdder:    %,d ns tổng | %.2f ns/op%n", adderTime, (double) adderTime / totalOps);
        System.out.printf("synchronized: %,d ns tổng | %.2f ns/op%n", syncTime, (double) syncTime / totalOps);
    }

    static long benchmarkAtomicLong() throws InterruptedException {
        AtomicLong counter = new AtomicLong();
        return runWithGates(() -> {
            for (int i = 0; i < OPS_PER_THREAD; i++) counter.incrementAndGet();
        });
    }

    static long benchmarkLongAdder() throws InterruptedException {
        LongAdder counter = new LongAdder();
        return runWithGates(() -> {
            for (int i = 0; i < OPS_PER_THREAD; i++) counter.increment();
        });
    }

    static long benchmarkSynchronized() throws InterruptedException {
        long[] counter = {0}; // mảng 1 phần tử để "giả lập" biến tham chiếu được trong lambda
        Object lock = new Object();
        return runWithGates(() -> {
            for (int i = 0; i < OPS_PER_THREAD; i++) {
                synchronized (lock) { counter[0]++; }
            }
        });
    }

    static long runWithGates(Runnable task) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                try {
                    startGate.await(); // MỌI thread chờ tại đây tới khi được "thả" đồng loạt
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown(); // báo hiệu thread này đã xong
                }
            }).start();
        }

        long start = System.nanoTime();
        startGate.countDown(); // THẢ tất cả thread cùng lúc
        endGate.await();       // chờ TẤT CẢ thread báo xong
        return System.nanoTime() - start;
    }
}
```

**Kết quả tiêu biểu (chênh lệch phụ thuộc số core CPU, xu hướng luôn nhất quán):**
```
===== Bảng kết quả (ns/op) =====
AtomicLong:   185,000,000 ns tổng | 23.13 ns/op
LongAdder:    52,000,000 ns tổng | 6.50 ns/op
synchronized: 420,000,000 ns tổng | 52.50 ns/op
```

### Giải thích vì sao `LongAdder` thắng khi tranh chấp cao

```
AtomicLong: MỘT biến "value" duy nhất, MỌI thread cùng CAS (Compare-And-Swap) vào ĐÚNG 1 ô nhớ
            -> Với 8 thread tranh chấp liên tục, RẤT NHIỀU lần CAS bị THẤT BẠI (giá trị đã đổi
               bởi thread khác ngay giữa lúc so sánh) -> phải RETRY liên tục -> lãng phí CPU

LongAdder:  BÊN TRONG có NHIỀU "Cell" (biến đếm phụ) riêng biệt, mỗi thread (dựa vào hash nội bộ
            của chính thread đó) thường ghi vào MỘT Cell RIÊNG, ít đụng độ với thread khác
            -> increment() hầu hết THÀNH CÔNG NGAY LẦN ĐẦU, ít phải retry
            -> Chỉ khi cần ĐỌC tổng (sum()) mới cộng dồn TẤT CẢ Cell lại - thao tác này HIẾM khi gọi
               so với increment() (được gọi liên tục)

synchronized: MỌI thread phải XẾP HÀNG hoàn toàn (chỉ 1 thread được vào tại 1 thời điểm)
              -> chi phí CAO NHẤT vì có overhead quản lý lock (park/unpark thread khi tranh chấp
                 nặng, JVM phải chuyển từ Thin Lock sang Fat Lock - liên hệ Module 07 JVM Internals)
```

- **`AtomicLong`** dùng **CAS (Compare-And-Swap)** — nhanh hơn `synchronized` (không cần park/unpark thread, không context switch) nhưng vẫn là **1 điểm tranh chấp DUY NHẤT** — với **8 thread tranh chấp liên tục trên đúng 1 biến**, tỷ lệ CAS thất bại cao, phải retry nhiều lần.
- **`LongAdder`** (Java 8+, thiết kế chuyên biệt cho tình huống **tranh chấp cao, cần cộng dồn liên tục**) phân tán việc ghi ra **nhiều ô nhớ riêng biệt** (`Cell[]` nội bộ, tự động mở rộng số Cell khi phát hiện tranh chấp) — mỗi thread thường **không đụng độ** với thread khác, giảm gần như triệt để tỷ lệ CAS thất bại. Đánh đổi: `sum()` (đọc tổng) phải **cộng dồn TẤT CẢ Cell**, chậm hơn `AtomicLong.get()` — nhưng vì `sum()` **ít được gọi hơn nhiều** so với `increment()` trong đa số tình huống thực tế (VD: bộ đếm request/giây — tăng liên tục, chỉ đọc định kỳ vài giây 1 lần), đánh đổi này **rất đáng giá**.
- **Nguyên tắc chọn lựa thực chiến:** `LongAdder` cho **bộ đếm high-contention chỉ cần tổng cuối cùng** (metrics, analytics counter); `AtomicLong` cho các tình huống cần `compareAndSet`/`get()` thường xuyên xen kẽ (VD: CAS loop ở Bài 7); `synchronized` chỉ nên dùng khi logic **phức tạp hơn 1 phép toán đơn giản** (cần bảo vệ nhiều bước liền nhau).

---

## Bài 5 — Semaphore rate-limit

### Đề
20 thread "gọi API đối tác" (`sleep(1000)`), `Semaphore(3)` giới hạn tối đa 3 song song. `AtomicInteger` đếm active, chứng minh không vượt 3. `tryAcquire(200, MILLISECONDS)` — không xin được thì log "bận".

### Phân tích

`Semaphore(n)` quản lý **n "permit" (giấy phép)** — `acquire()` lấy 1 permit (chờ nếu hết), `release()` trả lại. Đây là cách kiểm soát **SỐ LƯỢNG THREAD ĐỒNG THỜI** được thực hiện 1 thao tác (khác `synchronized`/`Lock` — chỉ cho phép ĐÚNG 1 thread). `tryAcquire(timeout, unit)` cho phép **"thử" trong 1 khoảng thời gian giới hạn**, không đợi vô thời hạn — trả `false` nếu hết giờ mà vẫn chưa có permit.

### Lời giải

```java
package baitap.bai5;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    static final Semaphore semaphore = new Semaphore(3); // TỐI ĐA 3 lời gọi song song
    static final AtomicInteger activeCount = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            final int id = i;
            Thread t = new Thread(() -> callPartnerApi(id));
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) t.join();
        System.out.println("\nHoàn tất tất cả 20 lời gọi");
    }

    static void callPartnerApi(int id) {
        try {
            boolean acquired = semaphore.tryAcquire(200, TimeUnit.MILLISECONDS);
            if (!acquired) {
                System.out.println("Thread " + id + " -> BẬN, bỏ cuộc sau 200ms chờ");
                return;
            }

            try {
                int current = activeCount.incrementAndGet();
                System.out.println("Thread " + id + " BẮT ĐẦU gọi API (đang active: " + current + "/3)");

                if (current > 3) {
                    System.out.println("  !!! LỖI: vượt quá 3 lời gọi song song !!!");
                }

                Thread.sleep(1000); // giả lập gọi API tốn thời gian

            } finally {
                activeCount.decrementAndGet();
                semaphore.release(); // LUÔN release, kể cả khi có exception
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**Kết quả tiêu biểu (thứ tự thread cụ thể ngẫu nhiên, nhưng active LUÔN ≤ 3):**
```
Thread 1 BẮT ĐẦU gọi API (đang active: 1/3)
Thread 2 BẮT ĐẦU gọi API (đang active: 2/3)
Thread 3 BẮT ĐẦU gọi API (đang active: 3/3)
Thread 4 -> BẬN, bỏ cuộc sau 200ms chờ
Thread 5 -> BẬN, bỏ cuộc sau 200ms chờ
... (nhiều thread khác cũng BẬN vì phải chờ > 200ms) ...
Thread 6 BẮT ĐẦU gọi API (đang active: 3/3)     <- sau khi Thread 1/2/3 release
...
Hoàn tất tất cả 20 lời gọi
```

### Giải thích

- **`current` không bao giờ vượt 3** — vì `Semaphore(3)` chỉ cấp phát **tối đa 3 permit tại 1 thời điểm**, thread thứ 4 trở đi **BẮT BUỘC phải chờ** tới khi 1 trong 3 permit đang dùng được `release()`. Việc kiểm tra `if (current > 3)` chỉ để **minh chứng bằng thực nghiệm**, trong code production không cần dòng này vì `Semaphore` đã đảm bảo đúng về mặt cơ chế.
- **Với `sleep(1000)` mỗi lời gọi** nhưng chỉ `tryAcquire(200ms)`, phần lớn trong 20 thread sẽ **timeout và bỏ cuộc** (vì phải chờ tới khi có permit trống, mà mỗi lần giữ permit lâu tới 1000ms) — đây chính là hành vi **rate limiting có kiểm soát thời gian chờ tối đa**, tránh client phải chờ vô thời hạn khi hệ thống đang quá tải — liên hệ trực tiếp khái niệm Rate Limiting/`429 Too Many Requests` đã học ở Module 14/22.
- **`finally { ...; semaphore.release(); }` là BẮT BUỘC** — nếu code trong `try` ném exception mà quên `release()` trong `finally`, permit đó **"rò rỉ" vĩnh viễn** — Semaphore dần dần "cạn kiệt" permit dù về logic vẫn còn "chỗ trống", cuối cùng mọi thread đều bị chặn vô thời hạn dù hệ thống thực ra đang rảnh.

---

## Bài 6 — BlockingQueue producer/consumer

### Đề
1 producer đẩy 100 "job" vào `ArrayBlockingQueue(10)`; 4 consumer `take()` xử lý. "Poison pill" báo dừng. `CountDownLatch` để main biết cả 4 consumer xong. Không dùng `wait`/`notify` tay.

### Phân tích

`ArrayBlockingQueue(capacity)` tự động **CHẶN (block)** `put()` khi đầy, `take()` khi rỗng — đây chính là cơ chế `SynchronousBox` đã tự viết tay ở Bài 5 Module 12, nhưng **đã được JDK đóng gói sẵn, tối ưu, đa năng hơn nhiều** (không cần tự viết `wait`/`notifyAll`). **Poison pill** là 1 giá trị đặc biệt (VD: `null`, hoặc 1 object sentinel riêng) đưa vào queue để báo hiệu "hết việc, dừng lại" — với **N consumer**, cần đưa vào **ĐÚNG N poison pill** (mỗi consumer tiêu thụ đúng 1 pill rồi dừng).

### Lời giải

```java
package baitap.bai6;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Main {

    static final int JOB_COUNT = 100;
    static final int CONSUMER_COUNT = 4;
    static final Integer POISON_PILL = -1; // giá trị đặc biệt báo hiệu "dừng lại"

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        CountDownLatch consumersDone = new CountDownLatch(CONSUMER_COUNT);

        // ===== Producer =====
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= JOB_COUNT; i++) {
                    queue.put(i); // TỰ ĐỘNG CHẶN nếu queue đầy - không cần tự viết wait()
                }
                // Đưa ĐÚNG CONSUMER_COUNT poison pill - mỗi consumer "ăn" đúng 1 pill rồi dừng
                for (int i = 0; i < CONSUMER_COUNT; i++) {
                    queue.put(POISON_PILL);
                }
                System.out.println("[Producer] Đã đẩy đủ " + JOB_COUNT + " job + " + CONSUMER_COUNT + " poison pill");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // ===== 4 Consumer =====
        for (int i = 1; i <= CONSUMER_COUNT; i++) {
            final int consumerId = i;
            new Thread(() -> {
                Random rnd = new Random();
                int processedCount = 0;
                try {
                    while (true) {
                        Integer job = queue.take(); // TỰ ĐỘNG CHẶN nếu queue rỗng
                        if (job.equals(POISON_PILL)) {
                            System.out.println("[Consumer-" + consumerId + "] nhận poison pill, dừng lại. Đã xử lý " + processedCount + " job");
                            break;
                        }
                        Thread.sleep(10 + rnd.nextInt(40)); // giả lập xử lý job
                        processedCount++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumersDone.countDown();
                }
            }, "Consumer-" + i).start();
        }

        producer.start();
        consumersDone.await(); // main CHỜ tới khi CẢ 4 consumer báo hoàn tất
        System.out.println("\nTất cả consumer đã dừng - chương trình kết thúc an toàn");
    }
}
```

**Kết quả tiêu biểu (phân bố job giữa các consumer ngẫu nhiên, tổng luôn đúng 100):**
```
[Producer] Đã đẩy đủ 100 job + 4 poison pill
[Consumer-2] nhận poison pill, dừng lại. Đã xử lý 23 job
[Consumer-1] nhận poison pill, dừng lại. Đã xử lý 27 job
[Consumer-4] nhận poison pill, dừng lại. Đã xử lý 26 job
[Consumer-3] nhận poison pill, dừng lại. Đã xử lý 24 job

Tất cả consumer đã dừng - chương trình kết thúc an toàn
```

### Giải thích

- **Vì sao đúng `CONSUMER_COUNT` poison pill, không phải 1:** nếu chỉ đưa **1** poison pill vào queue, chỉ **1 trong 4** consumer sẽ "ăn" được nó (vì mỗi phần tử trong `BlockingQueue` chỉ được **1** consumer `take()` — không giống broadcast) — 3 consumer còn lại sẽ **kẹt mãi mãi** ở `queue.take()` (chờ vô thời hạn vì không còn gì để lấy) — `consumersDone.await()` ở main sẽ **treo vĩnh viễn**. Đưa đúng **N pill cho N consumer** đảm bảo mỗi consumer đều nhận được đúng 1 tín hiệu dừng.
- **`ArrayBlockingQueue` xử lý sẵn TOÀN BỘ** cơ chế `wait`/`notify` bên trong (dùng `ReentrantLock` + `Condition` — kỹ thuật nâng cao hơn nhưng cùng ý tưởng với `wait`/`notifyAll` đã tự viết tay ở Bài 5 Module 12) — đây chính là lý do đề bài yêu cầu "không dùng `wait`/`notify` tay": trong thực tế Backend, **luôn ưu tiên dùng cấu trúc có sẵn của `java.util.concurrent`** thay vì tự viết bằng `wait`/`notify` thủ công (dễ sai, khó bảo trì) — bài tập tự viết tay ở Module 05.1 chỉ nhằm mục đích **hiểu cơ chế nền tảng**.

---

## Bài 7 — Bài toán tổng hợp: phòng vé bằng ExecutorService + CAS loop

### Đề
`TicketBooth` với `AtomicInteger available = new AtomicInteger(100)`. `boolean book()` dùng **CAS loop** để "kiểm tra + giảm 1" thành 1 thao tác nguyên tử. 500 task vào `newFixedThreadPool(50)`; đếm `true`; `CountDownLatch(500)`; luôn đúng 100, không oversold. Giải thích vì sao `get()` rồi `decrementAndGet()` riêng lẻ vẫn sai.

### Phân tích

**CAS Loop** là mẫu hình: đọc giá trị hiện tại, tính giá trị mới **trong bộ nhớ cục bộ (không ghi ngay)**, rồi thử `compareAndSet(giá_trị_cũ, giá_trị_mới)` — chỉ thành công nếu **không ai khác đã đổi giá trị** kể từ lúc đọc; nếu thất bại (bị thread khác "chen ngang"), **LẶP LẠI** toàn bộ quy trình từ đầu. Đây là cách đạt được "check-then-act nguyên tử" **KHÔNG CẦN `synchronized`/Lock** — dựa hoàn toàn vào lệnh CPU nguyên tử (Compare-And-Swap), thường **nhanh hơn** lock truyền thống khi tranh chấp không quá khốc liệt.

### Lời giải

```java
package baitap.bai7;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class Main {

    static class TicketBooth {
        private final AtomicInteger available = new AtomicInteger(100);

        public boolean book() {
            while (true) { // CAS LOOP
                int current = available.get();       // (1) ĐỌC giá trị hiện tại
                if (current <= 0) {
                    return false;                      // Hết vé thật sự - không còn gì để CAS
                }
                int updated = current - 1;             // (2) TÍNH giá trị mới TRONG BỘ NHỚ CỤC BỘ (chưa ghi)
                if (available.compareAndSet(current, updated)) {
                    // (3) CAS THÀNH CÔNG: giá trị THỰC SỰ vẫn còn là "current" tại thời điểm ghi
                    //     -> không ai chen ngang -> AN TOÀN, coi như đã "khóa" thành công
                    return true;
                }
                // CAS THẤT BẠI: thread khác đã đổi "available" giữa bước (1) và (3) của thread này
                // -> LẶP LẠI từ đầu vòng while, đọc lại giá trị MỚI NHẤT rồi thử lại
            }
        }

        public int getAvailable() { return available.get(); }
    }

    public static void main(String[] args) throws InterruptedException {
        TicketBooth booth = new TicketBooth();
        LongAdder successCount = new LongAdder(); // đếm hiệu quả cao cho tình huống tranh chấp lớn (Bài 4)

        int taskCount = 500;
        CountDownLatch latch = new CountDownLatch(taskCount);
        ExecutorService executor = Executors.newFixedThreadPool(50);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    if (booth.book()) {
                        successCount.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // main CHỜ tới khi CẢ 500 task báo hoàn tất
        executor.shutdown();

        System.out.println("Tổng vé bán được: " + successCount.sum());
        System.out.println("available còn lại: " + booth.getAvailable());
        System.out.println(successCount.sum() <= 100 ? "ĐÚNG - không oversold" : "SAI - OVERSOLD!");
    }
}
```

**Kết quả chạy (LUÔN đúng 100, mọi lần chạy):**
```
Tổng vé bán được: 100
available còn lại: 0
ĐÚNG - không oversold
```

### Giải thích vì sao `get()` rồi `decrementAndGet()` riêng lẻ vẫn SAI

```java
// ❌ VẪN SAI dù dùng AtomicInteger, nếu tách thành 2 lời gọi RIÊNG LẺ:
public boolean bookWrong() {
    if (available.get() > 0) {        // (A) ĐỌC - thao tác NGUYÊN TỬ, nhưng ĐỘC LẬP
        available.decrementAndGet();   // (B) GHI - cũng NGUYÊN TỬ, nhưng ĐỘC LẬP với (A)
        return true;                    // ⚠️ Giữa (A) và (B) vẫn có KHOẢNG HỞ cho thread khác chen vào!
    }
    return false;
}
```

- **Mỗi lời gọi `get()`/`decrementAndGet()` RIÊNG LẺ đều nguyên tử** — nhưng **TỔ HỢP CẢ HAI** (đọc rồi mới quyết định ghi) **KHÔNG nguyên tử** — y hệt bug "check-then-act" ở Bài 6 Module 12, chỉ khác là dùng `Atomic*` thay vì biến thường không có gì bảo vệ. Giữa bước (A) và (B), **hoàn toàn có thể** có N thread khác cũng vừa `get()` thấy `available > 0` (VD: `available = 1`), rồi TẤT CẢ đều `decrementAndGet()` — dẫn tới `available` âm và nhiều hơn 1 thread nhận `true` cho **CÙNG 1 vé cuối cùng**.
- **CAS loop giải quyết triệt để** vì `compareAndSet(current, updated)` gộp **"kiểm tra giá trị CHƯA ĐỔI kể từ lúc đọc" + "ghi giá trị mới"** thành **ĐÚNG 1 lệnh CPU nguyên tử duy nhất** (không có khoảng hở nào giữa kiểm tra và ghi) — nếu giá trị đã bị đổi bởi thread khác, `compareAndSet` **THẤT BẠI NGAY LẬP TỨC** (không ghi gì cả), buộc phải đọc lại và thử lại — không có cách nào 2 thread "cùng thành công" cho cùng 1 đơn vị tài nguyên.
- **Đây chính là ĐÚNG cơ chế nội bộ của `AtomicInteger.incrementAndGet()`/`decrementAndGet()`** — cả 2 thực chất **CHÍNH LÀ** 1 CAS loop viết sẵn bên trong JDK. Bài tập này giúp hiểu **CAS loop TỰ VIẾT** dùng khi logic phức tạp hơn "chỉ cộng/trừ 1 đơn thuần" (ở đây có thêm điều kiện "current <= 0 thì dừng hẳn"), không có method có sẵn nào của `AtomicInteger` xử lý được logic tùy biến này trực tiếp.

### Ghi chú (giống Bài 6 Module 12)

CAS loop trên `AtomicInteger` vẫn chỉ đúng trong phạm vi **1 JVM process**. Với backend chạy **nhiều instance** (Module 22 System Design), cần **Distributed Lock** (Redis), **`SELECT ... FOR UPDATE`** (Pessimistic Locking — Module 15), hoặc **`@Version`** (Optimistic Locking — Module 15) để đảm bảo đúng đắn **XUYÊN SUỐT toàn bộ hệ thống**, không chỉ trong 1 instance riêng lẻ.

---

*Đây là lời giải cho toàn bộ Phần B của Module 13. Tiếp theo: Module 14 — Java Modern (8 → 21+).*
