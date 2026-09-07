# Module 05.2 — Concurrency Utilities

> **Mức độ ưu tiên: Trung bình → Cao trong thực tế** — Trong backend thực tế, **KHÔNG BAO GIỜ** tự tạo `new Thread()` thủ công như Module 05.1 — luôn dùng `ExecutorService` (Thread Pool). Đây là kiến thức trực tiếp áp dụng khi làm việc với xử lý bất đồng bộ trong Spring (`@Async`), gọi API song song, và là nền tảng bắt buộc trước khi tiếp cận capstone Flash-Sale (Module cuối lộ trình).

---

## Mục lục

1. [Vì sao không nên tự tạo Thread thủ công](#1-vì-sao-không-nên-tự-tạo-thread-thủ-công)
2. [ExecutorService — Thread Pool cơ bản](#2-executorservice--thread-pool-cơ-bản)
3. [Các loại Thread Pool có sẵn](#3-các-loại-thread-pool-có-sẵn)
4. [ThreadPoolExecutor — tùy chỉnh sâu](#4-threadpoolexecutor--tùy-chỉnh-sâu)
5. [Future — lấy kết quả bất đồng bộ](#5-future--lấy-kết-quả-bất-đồng-bộ)
6. [CompletableFuture — bất đồng bộ hiện đại (Java 8+)](#6-completablefuture--bất-đồng-bộ-hiện-đại-java-8)
7. [CountDownLatch](#7-countdownlatch)
8. [Semaphore](#8-semaphore)
9. [AtomicInteger & các lớp Atomic — thay thế nhẹ hơn cho synchronized](#9-atomicinteger--các-lớp-atomic--thay-thế-nhẹ-hơn-cho-synchronized)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Vì sao không nên tự tạo Thread thủ công

Nhắc lại Module 05.1: tạo `Thread` là **tốn kém** (JVM phải cấp phát Stack riêng, đăng ký với OS scheduler...). Nếu 1 backend service tạo `new Thread()` cho **mỗi request** đến (có thể hàng nghìn request/giây), hệ thống sẽ nhanh chóng:

- **Cạn kiệt tài nguyên** (mỗi thread tốn ~1MB Stack mặc định — hàng nghìn thread = hàng GB RAM chỉ để quản lý thread).
- **Chi phí context-switching cao** — CPU tốn thời gian chuyển đổi qua lại giữa quá nhiều thread thay vì xử lý công việc thực sự.
- **Không kiểm soát được** số lượng thread tối đa — dễ dẫn đến `OutOfMemoryError` khi traffic tăng đột biến.

**Giải pháp chuẩn công nghiệp: Thread Pool** — tạo sẵn 1 số lượng thread **cố định (hoặc có giới hạn)**, tái sử dụng chúng để xử lý nhiều task liên tiếp, thay vì tạo mới rồi hủy liên tục.

---

## 2. ExecutorService — Thread Pool cơ bản

```java
import java.util.concurrent.*;

ExecutorService executor = Executors.newFixedThreadPool(4); // Thread Pool với 4 thread cố định

executor.submit(() -> {
    System.out.println("Task chạy trên: " + Thread.currentThread().getName());
});

executor.submit(() -> System.out.println("Task khác"));

executor.shutdown(); // BẮT BUỘC gọi — báo hiệu không nhận task mới, chờ task hiện tại hoàn thành rồi mới đóng pool
```

### `submit()` vs `execute()`

| Method | Trả về | Khi nào dùng |
|---|---|---|
| `execute(Runnable)` | `void` — không lấy được kết quả hay theo dõi task | Task đơn giản, không cần biết kết quả (fire-and-forget) |
| `submit(Runnable/Callable)` | `Future<T>` — có thể theo dõi, lấy kết quả, hủy task | Đa số trường hợp thực tế — linh hoạt hơn `execute()` |

### `Runnable` vs `Callable` — khi nào cần lấy kết quả trả về

```java
Runnable task1 = () -> System.out.println("Không trả về gì");   // run() không có return value

Callable<Integer> task2 = () -> { // call() CÓ return value, và được phép ném checked exception
    return 1 + 1;
};

Future<Integer> future = executor.submit(task2);
```

### `shutdown()` vs `shutdownNow()` vs `awaitTermination()`

```java
executor.shutdown();         // "graceful shutdown" — không nhận task mới, CHỜ task đang chạy hoàn thành
executor.shutdownNow();      // "forceful shutdown" — cố gắng NGẮT NGAY task đang chạy (dùng interrupt), trả về danh sách task chưa chạy
boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS); // CHỜ tối đa 30s để pool đóng hẳn, trả về true nếu đóng kịp thời hạn
```

> ⚠️ **Lỗi rất hay gặp ở người mới:** **quên gọi `shutdown()`** — Thread Pool sẽ **giữ JVM sống mãi**, chương trình không bao giờ tự kết thúc (vì các thread trong pool mặc định không phải daemon thread) dù logic nghiệp vụ đã xong hết.

---

## 3. Các loại Thread Pool có sẵn

```java
ExecutorService fixedPool = Executors.newFixedThreadPool(4);
// Số thread CỐ ĐỊNH (4). Task thừa xếp vào HÀNG ĐỢI KHÔNG GIỚI HẠN chờ đến lượt.
// Phù hợp: khối lượng công việc ổn định, biết trước mức tải trung bình.

ExecutorService cachedPool = Executors.newCachedThreadPool();
// Số thread TỰ ĐỘNG tăng/giảm theo nhu cầu (không giới hạn tối đa!), thread rảnh 60s sẽ tự bị hủy.
// Phù hợp: nhiều task NGẮN, số lượng dao động thất thường.
// ⚠️ RỦI RO: nếu có QUÁ NHIỀU task cùng lúc, có thể tạo VÔ SỐ thread → cạn tài nguyên hệ thống.

ExecutorService singleThread = Executors.newSingleThreadExecutor();
// CHỈ 1 thread duy nhất — các task chạy TUẦN TỰ, đảm bảo thứ tự.
// Phù hợp: cần xử lý tuần tự nghiêm ngặt (ví dụ ghi log theo đúng thứ tự xảy ra).

ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2);
// Hỗ trợ chạy task theo LỊCH (delay, định kỳ lặp lại) — tương tự cron job nhưng ở tầng ứng dụng.
scheduledPool.scheduleAtFixedRate(
    () -> System.out.println("Chạy mỗi 5 giây"),
    0,    // delay ban đầu
    5,    // khoảng cách giữa các lần chạy
    TimeUnit.SECONDS
);
```

> **Khuyến nghị thực tế (Java 19+):** JDK hiện đại khuyến khích cân nhắc `Executors.newVirtualThreadPerTaskExecutor()` (Virtual Threads — Project Loom, đã nhắc sơ ở Module 05 lý thuyết ban đầu, chính thức ổn định từ Java 21) cho khối lượng lớn task I/O-bound (gọi API, truy vấn database) — nhẹ hơn thread truyền thống hàng trăm lần. Đây là kiến thức **bổ sung**, không bắt buộc phải thành thạo ngay, nhưng nên biết xu hướng vì các dự án Spring Boot mới (từ Spring 6 / Boot 3.2+) đã hỗ trợ tận dụng Virtual Thread.

---

## 4. ThreadPoolExecutor — tùy chỉnh sâu

Khi cần kiểm soát chi tiết hơn `Executors.newFixedThreadPool()` cho phép (kích thước hàng đợi, chính sách xử lý khi quá tải...), dùng trực tiếp `ThreadPoolExecutor`:

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                              // corePoolSize — số thread TỐI THIỂU luôn duy trì
    4,                              // maximumPoolSize — số thread TỐI ĐA được tạo thêm khi hàng đợi đầy
    60L, TimeUnit.SECONDS,          // keepAliveTime — thời gian thread "thừa" (giữa core và max) rảnh trước khi bị hủy
    new LinkedBlockingQueue<>(100), // hàng đợi chứa task khi tất cả thread đang bận (giới hạn 100 task chờ)
    new ThreadPoolExecutor.CallerRunsPolicy() // Rejection Policy — xử lý khi hàng đợi VÀ pool đều đầy
);
```

### Cơ chế hoạt động (rất hay hỏi phỏng vấn Senior)

```
Task mới đến
    │
    ▼
Số thread hiện tại < corePoolSize?
    │
   Có ──► Tạo thread MỚI xử lý ngay
    │
   Không
    ▼
Hàng đợi (Queue) còn chỗ?
    │
   Có ──► Đưa vào hàng đợi, CHỜ thread rảnh
    │
   Không
    ▼
Số thread hiện tại < maximumPoolSize?
    │
   Có ──► Tạo thêm thread MỚI (vượt core, tối đa đến max) để xử lý ngay
    │
   Không (Pool VÀ Queue đều đầy)
    ▼
Áp dụng REJECTION POLICY
```

### Rejection Policy — khi hệ thống quá tải, làm gì với task mới?

| Policy | Hành vi |
|---|---|
| `AbortPolicy` (mặc định) | Ném `RejectedExecutionException` — task bị từ chối thẳng |
| `CallerRunsPolicy` | Task được chạy **ngay trên thread đang submit nó** (thường là thread gọi, không phải thread trong pool) — có tác dụng "làm chậm" tốc độ nhận task mới, giảm tải tự nhiên |
| `DiscardPolicy` | Âm thầm **bỏ qua** task mới, không báo lỗi gì (⚠️ nguy hiểm — dễ mất dữ liệu/task quan trọng mà không biết) |
| `DiscardOldestPolicy` | Bỏ task **cũ nhất** đang chờ trong hàng đợi để nhường chỗ cho task mới |

> **Liên hệ thực tế:** cấu hình Thread Pool cho `@Async` trong Spring Boot (Module 13) thực chất chính là cấu hình `ThreadPoolTaskExecutor` — về bản chất là 1 wrapper của `ThreadPoolExecutor` này. Hiểu rõ cơ chế core/max/queue/rejection policy ở đây sẽ giúp cấu hình đúng cho ứng dụng Spring Boot thực tế, tránh out-of-memory khi traffic tăng đột biến.

---

## 5. Future — lấy kết quả bất đồng bộ

`Future<T>` đại diện cho **kết quả của 1 tác vụ bất đồng bộ**, có thể chưa hoàn thành ngay khi nhận được object `Future`.

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

Future<Integer> future = executor.submit(() -> {
    Thread.sleep(2000); // giả lập tác vụ tốn thời gian (gọi API, tính toán phức tạp...)
    return 42;
});

System.out.println("Task đã được submit, tiếp tục làm việc khác trong lúc chờ...");

Integer result = future.get(); // ⚠️ BLOCKING — dừng lại CHỜ cho đến khi task hoàn thành mới lấy được kết quả
System.out.println("Kết quả: " + result);

// Có thể giới hạn thời gian chờ
try {
    Integer result2 = future.get(1, TimeUnit.SECONDS); // ném TimeoutException nếu quá 1s mà chưa xong
} catch (TimeoutException e) {
    System.out.println("Task chạy quá lâu!");
}

future.isDone();     // kiểm tra task đã xong chưa, KHÔNG block
future.cancel(true);  // cố gắng hủy task (true = cho phép interrupt nếu đang chạy)
```

### Hạn chế lớn nhất của `Future` — lý do `CompletableFuture` ra đời

`Future.get()` là **blocking** — không có cách nào "đăng ký callback" để tự động xử lý khi task xong, cũng **không thể kết hợp (compose/chain)** nhiều Future với nhau một cách gọn gàng (ví dụ "chạy task B ngay khi task A xong, dùng kết quả của A").

---

## 6. CompletableFuture — bất đồng bộ hiện đại (Java 8+)

`CompletableFuture<T>` giải quyết toàn bộ hạn chế của `Future` — hỗ trợ **callback không cần block**, **kết hợp (chaining) nhiều bước bất đồng bộ**.

### Tạo và xử lý callback không cần block

```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
    // chạy bất đồng bộ trên Thread Pool mặc định (ForkJoinPool.commonPool())
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    return 42;
});

future.thenAccept(result -> System.out.println("Kết quả: " + result)); // callback — chạy TỰ ĐỘNG khi future hoàn thành, KHÔNG block thread hiện tại
System.out.println("Dòng này có thể chạy TRƯỚC dòng in kết quả ở trên!");
```

### Chaining — nối tiếp nhiều bước xử lý bất đồng bộ

```java
CompletableFuture<String> pipeline = CompletableFuture
    .supplyAsync(() -> fetchUserFromDatabase(1L))     // (1) lấy user
    .thenApply(user -> user.getEmail())                 // (2) BIẾN ĐỔI kết quả — như map() trong Stream
    .thenApply(email -> email.toLowerCase())             // (3) tiếp tục biến đổi
    .thenApply(email -> "Đã gửi email đến: " + email);   // (4) biến đổi cuối cùng

String result = pipeline.join(); // giống get() nhưng không ném checked exception (dễ dùng trong lambda hơn)
```

### `thenApply` vs `thenAccept` vs `thenRun` — phân biệt rõ

| Method | Nhận đầu vào? | Trả về giá trị? | Tương tự |
|---|---|---|---|
| `thenApply(Function)` | Có (kết quả bước trước) | Có (kết quả mới) | `map()` trong Stream |
| `thenAccept(Consumer)` | Có | Không (`void`) | Tiêu thụ kết quả, không biến đổi tiếp |
| `thenRun(Runnable)` | Không | Không | Chỉ cần biết bước trước đã xong, không quan tâm kết quả là gì |

### Kết hợp nhiều CompletableFuture chạy song song

```java
CompletableFuture<Integer> futureA = CompletableFuture.supplyAsync(() -> fetchDataFromServiceA());
CompletableFuture<Integer> futureB = CompletableFuture.supplyAsync(() -> fetchDataFromServiceB());

// thenCombine — CHỜ CẢ HAI hoàn thành, rồi kết hợp 2 kết quả lại
CompletableFuture<Integer> combined = futureA.thenCombine(futureB, (resultA, resultB) -> resultA + resultB);

System.out.println(combined.join());
```

```java
// allOf — chờ TẤT CẢ future trong danh sách hoàn thành (không quan tâm giá trị trả về cụ thể)
CompletableFuture<Void> all = CompletableFuture.allOf(futureA, futureB);
all.join(); // chờ cả 2 xong

// anyOf — chỉ cần MỘT trong các future hoàn thành trước là đủ (ví dụ: gọi 2 server dự phòng, lấy kết quả server nào phản hồi trước)
CompletableFuture<Object> any = CompletableFuture.anyOf(futureA, futureB);
```

### Xử lý lỗi trong CompletableFuture — `exceptionally` & `handle`

```java
CompletableFuture<Integer> future = CompletableFuture
    .supplyAsync(() -> {
        if (Math.random() > 0.5) throw new RuntimeException("Lỗi ngẫu nhiên");
        return 42;
    })
    .exceptionally(ex -> { // giống "catch" — chỉ chạy khi có exception, cung cấp giá trị THAY THẾ
        System.out.println("Đã xảy ra lỗi: " + ex.getMessage());
        return -1; // giá trị fallback
    });

// handle() — chạy trong MỌI trường hợp (thành công LẪN lỗi), giống try/catch/finally gộp lại
future.handle((result, ex) -> {
    if (ex != null) {
        return "Lỗi: " + ex.getMessage();
    }
    return "Thành công: " + result;
});
```

> **Liên hệ thực tế:** `CompletableFuture` chính là nền tảng để hiểu **Reactive Programming** (`Mono`/`Flux` trong Spring WebFlux, sẽ nhắc ở Module 05 lý thuyết nâng cao và Module 19 — Microservices) — dù WebFlux có API khác, tư duy "chaining các bước xử lý bất đồng bộ không blocking" là hoàn toàn tương tự.

---

## 7. CountDownLatch

`CountDownLatch` cho phép **1 (hoặc nhiều) thread chờ cho đến khi 1 tập hợp các thao tác ở các thread khác hoàn thành** — hoạt động như "bộ đếm ngược dùng 1 lần".

```java
CountDownLatch latch = new CountDownLatch(3); // khởi tạo với đếm = 3

for (int i = 0; i < 3; i++) {
    int taskId = i;
    new Thread(() -> {
        System.out.println("Task " + taskId + " đang xử lý...");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        System.out.println("Task " + taskId + " hoàn thành");
        latch.countDown(); // giảm đếm đi 1 mỗi khi 1 task xong
    }).start();
}

latch.await(); // main thread BLOCK ở đây cho đến khi đếm về 0 (cả 3 task đều đã countDown())
System.out.println("Tất cả 3 task đã hoàn thành, tiếp tục xử lý...");
```

### Khác biệt quan trọng với `join()`

`join()` chỉ chờ được **1 thread cụ thể** (hoặc lặp qua danh sách để chờ từng cái); `CountDownLatch` linh hoạt hơn — có thể dùng để đồng bộ **nhiều nhóm thread khác nhau**, hoặc dùng theo mô hình "1 thread chờ N thread khác **báo hiệu** đã sẵn sàng/hoàn thành" mà không cần giữ tham chiếu trực tiếp đến từng `Thread` object.

> ⚠️ **Lưu ý:** `CountDownLatch` chỉ dùng được **1 lần** — sau khi đếm về 0, không thể "reset" lại để dùng tiếp (khác với `CyclicBarrier`, 1 công cụ tương tự nhưng có thể tái sử dụng — kiến thức mở rộng, không bắt buộc trong lộ trình này).

---

## 8. Semaphore

`Semaphore` giới hạn **số lượng thread tối đa được truy cập đồng thời** vào 1 tài nguyên có giới hạn — giống như "số chỗ đậu xe có hạn trong bãi".

```java
Semaphore semaphore = new Semaphore(3); // chỉ cho phép TỐI ĐA 3 thread cùng truy cập tại 1 thời điểm

for (int i = 0; i < 10; i++) {
    int taskId = i;
    new Thread(() -> {
        try {
            semaphore.acquire(); // "xin" 1 "giấy phép" (permit) — nếu hết chỗ (đã có 3 thread đang giữ), BLOCK chờ
            System.out.println("Task " + taskId + " đang sử dụng tài nguyên...");
            Thread.sleep(2000); // giả lập công việc tốn thời gian
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release(); // TRẢ LẠI "giấy phép" — BẮT BUỘC đặt trong finally để đảm bảo luôn được giải phóng dù có exception
        }
    }).start();
}
```

### Ứng dụng thực tế trong backend

`Semaphore` cực kỳ hữu ích để **giới hạn số kết nối đồng thời** đến 1 tài nguyên bên ngoài có giới hạn — ví dụ: giới hạn tối đa 5 request gọi đồng thời đến 1 API bên thứ ba (tránh bị họ chặn do gọi quá nhiều cùng lúc — rate limiting phía client), hoặc giới hạn số luồng ghi file cùng lúc để tránh quá tải disk I/O.

### So sánh `synchronized` vs `Semaphore`

| Tiêu chí | `synchronized` | `Semaphore` |
|---|---|---|
| Số thread được vào cùng lúc | Luôn là **1** | Có thể cấu hình **N** (bất kỳ số nào ≥ 1) |
| Linh hoạt | Cứng, gắn với 1 object lock cụ thể | Linh hoạt hơn — permit có thể "acquire" ở 1 chỗ, "release" ở chỗ khác |

---

## 9. AtomicInteger & các lớp Atomic — thay thế nhẹ hơn cho synchronized

Với các thao tác đơn giản trên **1 biến số nguyên/số thực/reference** (không phải logic phức tạp nhiều bước), package `java.util.concurrent.atomic` cung cấp các lớp đảm bảo **atomicity** mà **KHÔNG cần dùng lock (`synchronized`)** — dựa trên cơ chế phần cứng CPU gọi là **CAS (Compare-And-Swap)**, thường nhanh hơn `synchronized` trong tình huống tranh chấp (contention) không quá gay gắt.

```java
import java.util.concurrent.atomic.AtomicInteger;

AtomicInteger counter = new AtomicInteger(0);

counter.incrementAndGet();     // tương đương ++count, nhưng ATOMIC — an toàn tuyệt đối với đa luồng, KHÔNG cần synchronized
counter.getAndIncrement();     // tương đương count++ (trả về giá trị TRƯỚC KHI tăng)
counter.addAndGet(5);          // cộng thêm 5, trả về kết quả sau khi cộng
counter.compareAndSet(5, 10);  // nếu giá trị hiện tại ĐÚNG BẰNG 5, đổi thành 10 (trả về true/false báo có đổi thành công hay không)

System.out.println(counter.get());
```

### Áp dụng lại bài toán Race Condition ở Module 05.1 — cách giải quyết gọn hơn `synchronized`

```java
public class Counter {
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet(); // KHÔNG cần "synchronized" — bản thân AtomicInteger đã đảm bảo an toàn đa luồng
    }

    public int getCount() {
        return count.get();
    }
}
```

> **Khi nào chọn `Atomic*` thay vì `synchronized`?** Khi thao tác **chỉ đơn giản** trên 1 biến số/reference (tăng, giảm, so sánh-và-đổi) — `Atomic*` thường **nhanh hơn** vì không có overhead của việc "giữ/nhả lock" ở tầng OS. Khi logic **phức tạp hơn**, liên quan đến **nhiều biến/nhiều bước** cần đồng bộ cùng lúc (ví dụ ví dụ `BankAccount.transfer()` ở Module 05.1 — vừa trừ tài khoản này vừa cộng tài khoản kia) — vẫn cần `synchronized` (hoặc `Lock` nâng cao hơn) để đảm bảo tính nhất quán của **toàn bộ nhóm thao tác**.

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Công cụ | Dùng khi nào |
|---|---|
| `ExecutorService` | Luôn dùng thay vì tự tạo `new Thread()` thủ công trong code backend thực tế |
| `newFixedThreadPool` | Tải ổn định, biết trước quy mô |
| `newCachedThreadPool` | Nhiều task ngắn, tải dao động — cẩn thận rủi ro tạo vô số thread |
| `ThreadPoolExecutor` tùy chỉnh | Cần kiểm soát chi tiết core/max/queue/rejection policy |
| `Future` | Lấy kết quả bất đồng bộ — nhưng `get()` là **blocking** |
| `CompletableFuture` | Callback không-block, chaining nhiều bước, kết hợp nhiều task song song |
| `CountDownLatch` | 1 thread chờ N thread khác hoàn thành (dùng 1 lần) |
| `Semaphore` | Giới hạn số thread tối đa truy cập đồng thời vào tài nguyên có hạn |
| `Atomic*` (AtomicInteger...) | Thao tác đơn giản trên 1 biến — nhanh hơn `synchronized`, dựa trên CAS |
| `synchronized` | Vẫn cần thiết khi logic phức tạp, nhiều biến/nhiều bước phải nhất quán cùng lúc |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau có vấn đề gì? (Gợi ý: điều gì xảy ra với JVM sau khi chạy xong đoạn này)
```java
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> System.out.println("Task done"));
// không có dòng nào khác
```

**Câu 2.** So sánh `future.get()` và `completableFuture.thenAccept(...)` — điểm khác biệt cốt lõi là gì về mặt hành vi của thread hiện tại?

**Câu 3.** `newCachedThreadPool()` tiềm ẩn rủi ro gì mà `newFixedThreadPool(n)` không có? Giải thích bằng tình huống thực tế cụ thể.

**Câu 4.** Đoạn code sau có đảm bảo an toàn đa luồng không? Giải thích.
```java
private AtomicInteger balance = new AtomicInteger(1000);
public void transfer(int amount) {
    if (balance.get() >= amount) { // (1)
        balance.addAndGet(-amount); // (2)
    }
}
```
*(Gợi ý: xem xét kỹ điều gì có thể xảy ra GIỮA bước (1) và (2) nếu nhiều thread gọi `transfer()` cùng lúc.)*

**Câu 5.** Khi nào nên dùng `Semaphore` thay vì `synchronized`? Cho 1 ví dụ tình huống thực tế cụ thể.

---

### Phần B — Bài tập viết code

**Bài 1 — Xử lý song song với ExecutorService.**
Viết chương trình mô phỏng gọi 5 "API bên ngoài" (mỗi API giả lập bằng `Thread.sleep()` ngẫu nhiên 500-2000ms rồi trả về 1 số nguyên). Dùng `ExecutorService` với `newFixedThreadPool(5)` để gọi **song song cả 5**, dùng `Future` thu thập kết quả, tính tổng. So sánh thời gian chạy với cách gọi **tuần tự** (không dùng Thread Pool) để thấy rõ lợi ích của xử lý song song.

**Bài 2 — CompletableFuture chaining thực tế.**
Mô phỏng pipeline xử lý đơn hàng bất đồng bộ gồm 3 bước nối tiếp: `validateOrder()` → `calculateTotal()` → `sendConfirmationEmail()`, mỗi bước giả lập bằng `Thread.sleep(500)` và trả về giá trị cho bước sau dùng. Viết bằng `CompletableFuture` với `thenApply`/`thenAccept`, thêm `exceptionally()` để xử lý trường hợp `validateOrder()` ném exception (giả lập đơn hàng không hợp lệ).

**Bài 3 — CountDownLatch mô phỏng "chờ khởi động xong mới bắt đầu".**
Viết chương trình mô phỏng 3 "service" con (Database, Cache, MessageQueue) cần khởi động song song, mỗi service tốn thời gian ngẫu nhiên (1-3 giây) để sẵn sàng. Dùng `CountDownLatch(3)` để main thread **chờ CẢ 3 service khởi động xong** trước khi in ra `"Hệ thống đã sẵn sàng nhận request!"`.

**Bài 4 — Semaphore giới hạn kết nối đồng thời.**
Viết chương trình mô phỏng 20 thread cùng cố gắng "gọi API bên thứ ba" (giả lập bằng in log + `Thread.sleep(1000)`), nhưng dùng `Semaphore(3)` để đảm bảo **tối đa 3 lời gọi được thực hiện đồng thời** tại bất kỳ thời điểm nào (các thread còn lại phải chờ đến lượt). In log kèm timestamp để quan sát rõ tại một thời điểm không bao giờ có quá 3 "cuộc gọi" diễn ra cùng lúc.

**Bài 5 — Bài toán tổng hợp: Sửa lại hệ thống đặt vé (Module 05.1 Bài 5) bằng ExecutorService + AtomicInteger.**
Lấy lại bài toán `TicketBooth` ở Module 05.1 (Bài 5), nhưng lần này:
- Thay vì tự tạo 200 `Thread` thủ công, dùng `ExecutorService` với `newFixedThreadPool(20)` để submit 200 task đặt vé.
- Thay `synchronized` bằng `AtomicInteger` cho biến `availableTickets` — nhưng **cẩn thận**: phép kiểm tra "còn vé không" và "giảm số vé" phải là **1 thao tác nguyên tử duy nhất** (gợi ý: tìm hiểu method `updateAndGet()` hoặc dùng vòng lặp `compareAndSet()` thủ công, vì `get()` rồi `decrementAndGet()` riêng lẻ vẫn có thể xảy ra race condition tương tự Câu 4 Phần A).
- Dùng `CountDownLatch` để main thread biết khi nào toàn bộ 200 yêu cầu đặt vé đã được xử lý xong, rồi in ra tổng số vé đã bán thành công (phải luôn đúng, không vượt quá số vé có sẵn).

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Quên gọi `executor.shutdown()`** — các thread trong pool mặc định không phải daemon thread, nên JVM sẽ **không bao giờ tự thoát** dù task đã chạy xong từ lâu, chương trình bị "treo" vô thời hạn (phải tự tắt thủ công, ví dụ Ctrl+C khi chạy từ terminal).
2. `future.get()` là **blocking** — thread gọi nó phải **dừng lại chờ** cho đến khi có kết quả mới tiếp tục được. `completableFuture.thenAccept(...)` đăng ký 1 **callback** — thread hiện tại **tiếp tục chạy ngay**, callback sẽ tự động được gọi (trên 1 thread khác, thường từ Thread Pool) khi kết quả đã sẵn sàng, không cần chờ đợi ở đây.
3. `newCachedThreadPool()` **không có giới hạn số thread tối đa** — nếu có 1 lượng lớn task đến đột ngột (ví dụ traffic tăng đột biến, hoặc do bug khiến task được submit liên tục trong vòng lặp), pool có thể tạo ra **hàng nghìn/hàng chục nghìn thread**, dẫn đến cạn kiệt bộ nhớ (`OutOfMemoryError`) hoặc hệ thống bị "đơ" do quá tải context-switching — trong khi `newFixedThreadPool(n)` luôn giới hạn cứng số thread tối đa là `n`, các task thừa chỉ xếp hàng đợi chứ không tạo thêm thread vô hạn.
4. **KHÔNG đảm bảo an toàn** — dù `balance` là `AtomicInteger` (từng thao tác riêng lẻ như `get()` hay `addAndGet()` là atomic), nhưng **sự kết hợp của 2 thao tác riêng biệt** (`get()` rồi sau đó mới `addAndGet()`) **không phải** là 1 khối atomic duy nhất. Giữa bước (1) và (2), 1 thread khác hoàn toàn có thể "chen vào" và thay đổi `balance`, dẫn đến kết quả kiểm tra ở bước (1) đã "lỗi thời" khi thực hiện bước (2) — đây là bug tinh vi rất dễ bị bỏ sót ngay cả khi đã "dùng Atomic cho có". Cách sửa đúng: dùng `updateAndGet()` hoặc vòng lặp `compareAndSet()` để gộp CẢ 2 bước kiểm tra và cập nhật thành 1 thao tác nguyên tử thực sự.
5. Dùng `Semaphore` khi cần giới hạn **số lượng luồng truy cập đồng thời lớn hơn 1** vào 1 tài nguyên có hạn — ví dụ: hệ thống backend gọi đến 1 API thanh toán bên thứ ba chỉ cho phép tối đa 5 kết nối đồng thời (theo hợp đồng SLA với nhà cung cấp) — dùng `Semaphore(5)` đảm bảo dù có 100 request nội bộ cùng lúc cần gọi API đó, chỉ tối đa 5 request thực sự được gửi đi tại 1 thời điểm, phần còn lại tự động xếp hàng chờ.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Kết quả mong đợi: gọi song song bằng Thread Pool sẽ mất khoảng thời gian **gần bằng thời gian của API chậm nhất** (vì 5 API chạy đồng thời), trong khi gọi tuần tự sẽ mất **tổng thời gian của cả 5 API cộng lại** — chênh lệch có thể lên tới vài lần, minh họa rất trực quan lợi ích thực tế của xử lý bất đồng bộ/song song trong backend (ví dụ khi 1 API cần tổng hợp dữ liệu từ nhiều microservice khác nhau).
- **Bài 2:** Đây là mô phỏng chính xác pattern sẽ gặp lại khi học `@Async` trong Spring Boot (Module 13) và khi thiết kế microservice giao tiếp bất đồng bộ (Module 19) — chuỗi `thenApply` nối tiếp giúp code đọc tuần tự, dễ hiểu, dù bản chất đang chạy bất đồng bộ phía dưới.
- **Bài 4:** Có thể quan sát rõ bằng cách in `System.currentTimeMillis()` hoặc đếm số "cuộc gọi" đang hoạt động tại từng thời điểm (dùng thêm 1 `AtomicInteger` đếm số lượng đang active, in ra ngay sau `acquire()` và ngay trước `release()`) — con số này **không bao giờ vượt quá 3** nếu implement đúng.
- **Bài 5:** Đây là bài tập **quan trọng nhất và khó nhất** của cả module — chính là bản nháp gần hoàn chỉnh nhất (trong phạm vi kiến thức Java thuần, chưa động đến database/Redis) cho bài toán cốt lõi của capstone Flash-Sale. Gợi ý cách viết đúng cho phần kiểm tra-và-giảm vé nguyên tử:
```java
public boolean bookTicket() {
    int updated = availableTickets.updateAndGet(current -> current > 0 ? current - 1 : current);
    // Nếu updated < giá trị TRƯỚC updateAndGet(), nghĩa là đã giảm thành công — nhưng cách CHẮC CHẮN hơn:
    // Cách rõ ràng hơn, dùng vòng lặp CAS thủ công:
    while (true) {
        int current = availableTickets.get();
        if (current <= 0) return false; // hết vé
        if (availableTickets.compareAndSet(current, current - 1)) {
            return true; // đặt vé thành công, ĐÚNG 1 thread "thắng" trong tình huống tranh chấp
        }
        // nếu compareAndSet thất bại (thread khác đã thay đổi giá trị trước), vòng lặp THỬ LẠI với giá trị mới nhất
    }
}
```
Đây chính là kỹ thuật **CAS Loop (Compare-And-Swap Loop)** — nền tảng của hầu hết các cấu trúc dữ liệu "lock-free" hiệu năng cao trong Java, và là kiến thức nâng cao rất đáng tự hào nếu nắm vững trước khi bắt đầu capstone.

</details>

---

*File tiếp theo trong lộ trình: **Module 06 — Java Modern (8 → 21+)** (Optional, record, sealed class, pattern matching, Text Block, Virtual Threads).*
