# Module 05.2 — Concurrency Utilities

> **Mức độ ưu tiên: Cao trong thực tế** — Backend **không bao giờ** `new Thread()` thủ công (Module 05.1) — luôn dùng `ExecutorService`. Đây là kiến thức áp dụng trực tiếp cho `@Async` của Spring, gọi API song song, và là nền tảng bắt buộc trước capstone Flash-Sale.

> **Phạm vi bài này:** toàn bộ `java.util.concurrent` ở mức ứng dụng — thread pool (`ExecutorService`, `ThreadPoolExecutor`), `BlockingQueue`, `Future`/`CompletableFuture`, `CountDownLatch`/`CyclicBarrier`/`Semaphore`, `Atomic*`, `Lock`/`ReadWriteLock`/`Condition`, `ConcurrentHashMap`. **Chỉ nhắc tên, không đi sâu:** cơ chế `synchronized`/`volatile`/`wait`/happens-before (Module 05.1 — chỉ nhắc lại vừa đủ), `@Async`/`ThreadPoolTaskExecutor` của Spring (Module 13), reactive `Mono`/`Flux` (Module 19), Fork/Join framework nội bộ. Virtual thread (Java 21) nêu vừa đủ để biết xu hướng.

---

## Mục lục

1. [Vì sao không tự tạo Thread — và cách chọn kích thước pool](#1-vì-sao-không-tự-tạo-thread--và-cách-chọn-kích-thước-pool)
2. [ExecutorService — Thread Pool cơ bản](#2-executorservice--thread-pool-cơ-bản)
3. [Các loại Thread Pool có sẵn](#3-các-loại-thread-pool-có-sẵn)
4. [ThreadPoolExecutor — hàng đợi quyết định hành vi](#4-threadpoolexecutor--hàng-đợi-quyết-định-hành-vi)
5. [BlockingQueue — producer/consumer](#5-blockingqueue--producerconsumer)
6. [Future — lấy kết quả bất đồng bộ](#6-future--lấy-kết-quả-bất-đồng-bộ)
7. [CompletableFuture — bất đồng bộ hiện đại](#7-completablefuture--bất-đồng-bộ-hiện-đại)
8. [CountDownLatch, CyclicBarrier, Phaser](#8-countdownlatch-cyclicbarrier-phaser)
9. [Semaphore](#9-semaphore)
10. [Atomic — CAS, ABA, LongAdder](#10-atomic--cas-aba-longadder)
11. [Lock, ReadWriteLock, Condition](#11-lock-readwritelock-condition)
12. [ConcurrentHashMap](#12-concurrenthashmap)
13. [Tổng kết — Bảng ghi nhớ nhanh](#13-tổng-kết--bảng-ghi-nhớ-nhanh)
14. [Bài tập luyện tập](#14-bài-tập-luyện-tập)

---

## 1. Vì sao không tự tạo Thread — và cách chọn kích thước pool

Nhắc Module 05.1: mỗi platform thread tốn ~1 MB stack + đăng ký với OS scheduler. `new Thread()` cho **mỗi request** (hàng nghìn/giây) dẫn tới:

- **Cạn tài nguyên** — hàng nghìn thread = hàng GB RAM chỉ để quản lý thread.
- **Context switch quá nhiều** — CPU dành thời gian chuyển ngữ cảnh thay vì làm việc thật.
- **Không có trần** — traffic tăng đột biến → `OutOfMemoryError: unable to create new native thread`.
- **Không có ranh giới xử lý lỗi, không đặt tên thread, không backpressure.**

**Thread pool** tách "gửi việc" (submit) khỏi "chạy việc" (execute): một số thread cố định, tái dùng, có hàng đợi và chính sách quá tải.

### Chọn số thread cho pool

| Loại tác vụ | Công thức gần đúng |
|---|---|
| **CPU-bound** (tính toán, mã hóa, xử lý ảnh) | `số nhân + 1` — thêm thread chỉ làm tăng context switch |
| **I/O-bound** (gọi DB, HTTP, đọc file) | `số nhân × (1 + thời gian chờ / thời gian tính)` — thread nằm chờ I/O không dùng CPU |

Ví dụ 8 nhân, tác vụ gọi API mất 90 ms chờ + 10 ms xử lý → `8 × (1 + 90/10) = 80` thread. Luôn **đo** (throughput, latency p99) rồi chỉnh, đừng đoán.

---

## 2. ExecutorService — Thread Pool cơ bản

```java
import java.util.concurrent.*;

ExecutorService pool = Executors.newFixedThreadPool(4);

pool.execute(() -> log.info("fire-and-forget"));                 // Runnable, void
Future<Integer> f = pool.submit(() -> 1 + 1);                     // Callable, có Future

pool.shutdown();                                                   // không nhận task mới, chờ task đang chạy
```

### `submit()` vs `execute()` — và bẫy nuốt exception

| Method | Trả về | Exception khi task ném |
|---|---|---|
| `execute(Runnable)` | `void` | Đi tới `UncaughtExceptionHandler` của thread → in `stderr` |
| `submit(Runnable/Callable)` | `Future<T>` | **Nuốt vào `Future`** — chỉ lộ ra khi gọi `future.get()` (bọc trong `ExecutionException`) |

```java
Future<?> f = pool.submit(() -> { throw new RuntimeException("nổ"); });
// KHÔNG in gì — exception nằm im trong f
try {
    f.get();
} catch (ExecutionException e) {
    Throwable real = e.getCause();   // RuntimeException("nổ")
}
```

> ⚠️ **Bug rất phổ biến:** `submit(...)` một task rồi **không bao giờ gọi `get()`** → exception biến mất hoàn toàn, không log, không dấu vết. Nếu chỉ fire-and-forget, dùng `execute()`, hoặc bọc `try/catch` trong chính task.

### `Runnable` vs `Callable`

```java
Runnable r         = () -> log.info("no return, no checked exception");
Callable<Integer> c = () -> { return riskyIo(); };   // trả giá trị + được throws checked
Future<Integer> fc  = pool.submit(c);
```

### `invokeAll` / `invokeAny`

```java
List<Callable<Integer>> tasks = List.of(() -> a(), () -> b(), () -> c());
List<Future<Integer>> all = pool.invokeAll(tasks);           // CHỜ tất cả xong, trả list Future
Integer first = pool.invokeAny(tasks);                        // trả kết quả của task ĐẦU TIÊN xong, hủy phần còn lại
```

### Đóng pool đúng cách — mẫu hai pha (Javadoc)

```java
void shutdownGracefully(ExecutorService pool) {
    pool.shutdown();                                          // pha 1: ngừng nhận task mới
    try {
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
            pool.shutdownNow();                               // pha 2: interrupt task đang chạy
            if (!pool.awaitTermination(30, TimeUnit.SECONDS))
                log.error("pool không chịu dừng");
        }
    } catch (InterruptedException e) {
        pool.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

| Method | Hành vi |
|---|---|
| `shutdown()` | Không nhận task mới; task trong hàng đợi **vẫn chạy** |
| `shutdownNow()` | `interrupt()` các task đang chạy; trả về `List<Runnable>` các task **chưa** chạy |
| `awaitTermination(t, unit)` | Chờ tối đa `t` để pool đóng hẳn; `true` nếu kịp |

> ⚠️ Quên `shutdown()` → thread pool (mặc định **không phải daemon**) giữ JVM sống mãi, chương trình không tự thoát.

### Java 19+ — `ExecutorService` là `AutoCloseable`

```java
try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
    pool.submit(task1);
    pool.submit(task2);
}   // close() = shutdown() + awaitTermination (chờ) — hết khối là mọi task đã xong
```

### Đặt tên thread (để đọc log / thread dump)

```java
ThreadFactory named = r -> { Thread t = new Thread(r, "order-worker"); return t; };
ExecutorService pool = Executors.newFixedThreadPool(4, named);
```

---

## 3. Các loại Thread Pool có sẵn

```java
Executors.newFixedThreadPool(4);
// 4 thread cố định. Task thừa → LinkedBlockingQueue KHÔNG GIỚI HẠN.
// ⚠️ Tải tăng đột biến → hàng đợi phình vô hạn → OutOfMemoryError. Xem mục 4.

Executors.newCachedThreadPool();
// Thread tăng/giảm theo nhu cầu, KHÔNG có trần; thread rảnh 60s tự hủy.
// ⚠️ Quá nhiều task cùng lúc → tạo hàng vạn thread → cạn tài nguyên.

Executors.newSingleThreadExecutor();
// 1 thread, task chạy tuần tự đúng thứ tự submit. Không cast/chỉnh lại được (khác newFixedThreadPool(1)).

Executors.newScheduledThreadPool(2);   // chạy theo lịch — xem dưới

Executors.newWorkStealingPool();       // ForkJoinPool, mỗi thread một deque, "trộm việc" của nhau — hợp task chia nhỏ đệ quy

Executors.newVirtualThreadPerTaskExecutor();   // Java 21 — mỗi task một VIRTUAL thread
```

### `ScheduledExecutorService` — `scheduleAtFixedRate` vs `scheduleWithFixedDelay`

```java
var sched = Executors.newScheduledThreadPool(2);

sched.scheduleAtFixedRate(job, 0, 5, TimeUnit.SECONDS);
// Lần chạy thứ n bắt đầu tại initialDelay + n×period. Task chạy lâu hơn period → các lần chạy DỒN nhau (không chồng lấn, nhưng chạy liên tục không nghỉ).

sched.scheduleWithFixedDelay(job, 0, 5, TimeUnit.SECONDS);
// Lần sau bắt đầu 5s SAU KHI lần trước KẾT THÚC. Luôn có khoảng nghỉ cố định.
```

> ⚠️ **Task định kỳ ném exception mà không `catch` → lịch lặp DỪNG HẲN, im lặng.** Luôn bọc toàn bộ thân task định kỳ trong `try/catch` và log.

### Virtual thread (Java 21) — nhắc xu hướng

```java
try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
    for (var req : requests) pool.submit(() -> handleBlocking(req));   // hàng chục nghìn task I/O
}
```

Virtual thread cực nhẹ (không tốn ~1 MB stack, JVM lập lịch trên số ít carrier thread) → không cần "pool để tái dùng" nữa; tạo một cái cho mỗi task. **Không** pool virtual thread; giới hạn tài nguyên bằng `Semaphore` thay vì kích thước pool. Spring Boot 3.2+ hỗ trợ. Chi tiết ngoài phạm vi bài.

---

## 4. ThreadPoolExecutor — hàng đợi quyết định hành vi

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    2,                                  // corePoolSize — thread tối thiểu luôn giữ
    4,                                  // maximumPoolSize — thread tối đa
    60L, TimeUnit.SECONDS,              // keepAliveTime — thread "thừa" (core..max) rảnh bao lâu thì hủy
    new ArrayBlockingQueue<>(100),      // hàng đợi task chờ
    new ThreadPoolExecutor.CallerRunsPolicy());   // làm gì khi pool + queue đều đầy
```

### Luồng quyết định khi có task mới

```
Số thread < corePoolSize?  ──Có──► tạo thread mới, chạy ngay
        │Không
Hàng đợi còn chỗ?          ──Có──► xếp vào hàng đợi
        │Không
Số thread < maximumPoolSize? ─Có──► tạo thread mới (vượt core), chạy ngay
        │Không
        ▼
   REJECTION POLICY
```

### ⚠️ Loại hàng đợi quyết định `maximumPoolSize` có tác dụng hay không

| Hàng đợi | Hệ quả |
|---|---|
| `LinkedBlockingQueue` **không bound** | Task luôn xếp được vào queue → bước "tạo thread vượt core" **không bao giờ tới** → `maximumPoolSize` **bị bỏ qua**, queue phình đến OOM. Đây là cấu hình của `newFixedThreadPool`. |
| `SynchronousQueue` (sức chứa 0) | Không giữ task nào — mỗi task cần một thread rảnh **ngay**, không thì tạo thread mới tới `max`; hết `max` → reject. Cấu hình của `newCachedThreadPool` (với `max = Integer.MAX_VALUE`). |
| `ArrayBlockingQueue(n)` / `LinkedBlockingQueue(n)` **có bound** | Queue đầy mới tạo thread tới `max`; `max` đầy mới reject. **Đây là cấu hình đúng cho backend** — có trần bộ nhớ rõ ràng. |

### Rejection Policy

| Policy | Hành vi |
|---|---|
| `AbortPolicy` (mặc định) | Ném `RejectedExecutionException` |
| `CallerRunsPolicy` | Chạy task **trên thread đang submit** → tự làm chậm tốc độ nhận task (backpressure tự nhiên) |
| `DiscardPolicy` | Bỏ im lặng task mới — ⚠️ mất việc không dấu vết |
| `DiscardOldestPolicy` | Bỏ task **cũ nhất** trong queue, xếp task mới vào |

### Giám sát runtime

```java
pool.getActiveCount();          // thread đang chạy task
pool.getQueue().size();          // task đang chờ
pool.getPoolSize();              // tổng thread hiện có
pool.getLargestPoolSize();       // đỉnh cao nhất từng đạt
pool.getCompletedTaskCount();    // task đã xong
```

### Bẫy: thread starvation deadlock

```java
ExecutorService pool = Executors.newFixedThreadPool(2);
Future<String> a = pool.submit(() -> {
    Future<String> b = pool.submit(() -> "con");   // task con
    return b.get();                                  // CHỜ task con
});
// 2 task cha chiếm cả 2 thread, cùng chờ task con — task con không có thread để chạy → treo vĩnh viễn
```

→ Task **không** được submit task khác vào **cùng pool** rồi block chờ nó. Dùng pool riêng, hoặc `CompletableFuture` chaining.

> **Liên hệ Spring:** `@Async` dùng `ThreadPoolTaskExecutor` — wrapper của `ThreadPoolExecutor` này. Hiểu core/max/queue/rejection ở đây là cấu hình đúng cho Spring Boot (Module 13).

---

## 5. BlockingQueue — producer/consumer

Hàng đợi an toàn đa luồng, **tự chặn** khi rỗng/đầy — nền tảng của thread pool và mọi mô hình producer/consumer, thay cho `wait`/`notify` viết tay (Module 05.1).

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(1000);

// Producer
queue.put(task);          // CHẶN nếu queue đầy
queue.offer(task, 200, TimeUnit.MILLISECONDS);   // chờ tối đa rồi trả false

// Consumer
Task t = queue.take();    // CHẶN nếu queue rỗng
Task t2 = queue.poll(1, TimeUnit.SECONDS);        // chờ tối đa rồi trả null
```

| Loại | Đặc điểm |
|---|---|
| `ArrayBlockingQueue(n)` | Mảng vòng, **bound cố định**, tùy chọn fair. Hợp làm buffer có trần. |
| `LinkedBlockingQueue` | Linked list, bound tùy chọn (mặc định gần vô hạn). Throughput cao, hai khóa riêng cho đầu/cuối. |
| `SynchronousQueue` | Sức chứa 0 — mỗi `put` chờ một `take` khớp (hand-off trực tiếp). |
| `PriorityBlockingQueue` | Không bound, lấy ra theo `Comparator`/`Comparable`. |
| `DelayQueue` | Phần tử chỉ lấy được khi đã "đến hạn" (`Delayed`). Hợp scheduler, retry có trễ. |
| `LinkedTransferQueue` | `transfer()` — chặn tới khi có consumer thực sự nhận. |

> Producer/consumer với `BlockingQueue` **không cần** `synchronized`/`wait`/`notify` — mọi đồng bộ nằm trong queue.

---

## 6. Future — lấy kết quả bất đồng bộ

```java
Future<Integer> f = pool.submit(() -> { Thread.sleep(2000); return 42; });

f.isDone();                              // không block
Integer r = f.get();                     // ⚠️ BLOCKING tới khi xong
Integer r2 = f.get(1, TimeUnit.SECONDS); // ném TimeoutException nếu quá hạn
f.cancel(true);                          // true = interrupt nếu đang chạy; false = chỉ hủy nếu chưa bắt đầu
f.isCancelled();
```

### `get()` ném ba loại exception

| Exception | Khi nào |
|---|---|
| `ExecutionException` | Task ném exception → gói vào đây; lấy gốc bằng `getCause()` |
| `InterruptedException` | Thread đang chờ `get()` bị interrupt |
| `CancellationException` | Task đã bị `cancel()` |
| `TimeoutException` | Chỉ với `get(timeout, unit)` |

### Hạn chế — lý do có `CompletableFuture`

`Future.get()` **blocking**; không đăng ký được callback; không nối (compose) nhiều `Future`; không có xử lý lỗi khai báo.

---

## 7. CompletableFuture — bất đồng bộ hiện đại

### Tạo & callback không block

```java
CompletableFuture<Integer> cf = CompletableFuture.supplyAsync(() -> slowCompute());   // chạy trên ForkJoinPool.commonPool()
cf.thenAccept(r -> log.info("kết quả {}", r));     // callback tự chạy khi xong, KHÔNG block thread hiện tại
```

> ⚠️ `supplyAsync(task)` không truyền executor → chạy trên **`ForkJoinPool.commonPool()`** dùng chung JVM (cùng rủi ro như `parallelStream` — Module 03.3). Với tác vụ **I/O**, luôn truyền pool riêng:
> ```java
> CompletableFuture.supplyAsync(() -> callHttp(), ioPool)
>                  .thenApplyAsync(this::parse, cpuPool);
> ```

### `thenApply` vs `thenCompose` vs `thenCombine`

| Method | Đầu vào | Kết quả | Tương tự Stream |
|---|---|---|---|
| `thenApply(Function<T,R>)` | `T` | `CF<R>` | `map` |
| `thenCompose(Function<T,CF<R>>)` | `T` | `CF<R>` (không lồng) | `flatMap` |
| `thenAccept(Consumer<T>)` | `T` | `CF<Void>` | tiêu thụ |
| `thenRun(Runnable)` | — | `CF<Void>` | chỉ cần biết "đã xong" |
| `thenCombine(CF<U>, BiFunction)` | `T` + `U` | `CF<R>` | zip 2 nguồn |

```java
findUser(id)
    .thenApply(User::name)               // CF<String>
    .thenCompose(name -> loadProfile(name))   // loadProfile trả CF<Profile> → KHÔNG lồng
    .thenCombine(loadSettings(id), (profile, settings) -> render(profile, settings));
```

`thenApply` vs `thenApplyAsync`: bản thường chạy callback **trên thread vừa hoàn thành bước trước** (hoặc thread gọi, nếu đã xong); bản `Async` đẩy callback sang pool → dùng khi callback nặng hoặc muốn tách pool.

### Kết hợp nhiều future

```java
CompletableFuture<Integer> a = supplyAsync(this::fetchA, pool);
CompletableFuture<Integer> b = supplyAsync(this::fetchB, pool);

a.thenCombine(b, Integer::sum);                    // chờ CẢ HAI, gộp

CompletableFuture.allOf(a, b).join();               // chờ tất cả (trả Void)
CompletableFuture.anyOf(a, b).join();               // chỉ cần MỘT xong (ví dụ 2 server dự phòng)

// allOf trả Void → thu kết quả bằng join sau khi allOf hoàn thành:
List<CompletableFuture<Integer>> fs = ids.stream().map(id -> supplyAsync(() -> fetch(id), pool)).toList();
CompletableFuture<List<Integer>> results = CompletableFuture
    .allOf(fs.toArray(CompletableFuture[]::new))
    .thenApply(v -> fs.stream().map(CompletableFuture::join).toList());
```

### Xử lý lỗi — `exceptionally` / `handle` / `whenComplete`

```java
cf.exceptionally(ex -> -1)                          // chỉ chạy khi LỖI, trả giá trị thay thế
  .handle((result, ex) -> ex != null ? "lỗi: " + ex : "ok: " + result)   // chạy MỌI trường hợp, biến đổi
  .whenComplete((result, ex) -> log.info("xong (ex={})", ex));           // chạy MỌI trường hợp, KHÔNG biến đổi, ném lại lỗi
```

> Bên trong chuỗi `CompletableFuture`, exception được bọc trong **`CompletionException`** — trong `exceptionally`/`handle` nhớ `ex.getCause()` để lấy gốc.

### Timeout (Java 9+) & hoàn thành thủ công

```java
cf.orTimeout(2, TimeUnit.SECONDS);                       // quá hạn → hoàn thành với TimeoutException
cf.completeOnTimeout(fallbackValue, 2, TimeUnit.SECONDS); // quá hạn → hoàn thành với giá trị dự phòng

// Bọc API callback cũ thành CompletableFuture:
CompletableFuture<String> bridge = new CompletableFuture<>();
legacyClient.onSuccess(bridge::complete);
legacyClient.onError(bridge::completeExceptionally);
```

### `join()` vs `get()`

`join()` ném `CompletionException` (unchecked) → dùng được trong lambda/Stream. `get()` ném checked `ExecutionException`/`InterruptedException`.

> **Liên hệ:** `CompletableFuture` là nền tảng tư duy cho reactive (`Mono`/`Flux` — Module 19): "nối các bước bất đồng bộ, không block".

---

## 8. CountDownLatch, CyclicBarrier, Phaser

### `CountDownLatch` — một thread chờ N thao tác xong (dùng MỘT lần)

```java
CountDownLatch done = new CountDownLatch(3);
for (int i = 0; i < 3; i++) {
    pool.submit(() -> { try { work(); } finally { done.countDown(); } });
}
done.await();                                   // block tới khi đếm về 0
done.await(10, TimeUnit.SECONDS);               // hoặc chờ có thời hạn
System.out.println("cả 3 đã xong");
```

Sau khi về 0 **không reset được**.

### Mẫu "start gate / end gate" — đo hiệu năng công bằng

```java
CountDownLatch startGate = new CountDownLatch(1);
CountDownLatch endGate   = new CountDownLatch(N);
for (int i = 0; i < N; i++) pool.submit(() -> {
    startGate.await();                          // mọi thread chờ ở vạch xuất phát
    try { task(); } finally { endGate.countDown(); }
});
long t0 = System.nanoTime();
startGate.countDown();                          // thả TẤT CẢ cùng lúc
endGate.await();
long elapsedNs = System.nanoTime() - t0;        // đo đúng từ lúc bắt đầu đồng loạt
```

### So sánh với `CyclicBarrier` và `Phaser`

| Công cụ | Ai chờ ai | Tái dùng | Điểm riêng |
|---|---|---|---|
| `CountDownLatch(n)` | 1+ thread chờ **n lần `countDown()`** (từ thread bất kỳ) | **Không** | Đơn giản nhất; "cổng" một chiều |
| `CyclicBarrier(n)` | **n thread chờ lẫn nhau** cùng tới điểm hẹn | **Có** (tự reset) | Chạy được `barrierAction` khi đủ n; hợp thuật toán theo vòng (simulation) |
| `Phaser` | Số bên (parties) **thay đổi động** qua từng pha | Có | `register()`/`arriveAndAwaitAdvance()`; linh hoạt nhất, phức tạp nhất |

---

## 9. Semaphore

Giới hạn **số thread tối đa** vào một tài nguyên cùng lúc — "N chỗ trong bãi đỗ".

```java
Semaphore sem = new Semaphore(5, /* fair */ true);   // 5 permit, cấp theo thứ tự chờ

for (int i = 0; i < 100; i++) pool.submit(() -> {
    if (!sem.tryAcquire(200, TimeUnit.MILLISECONDS))
        throw new RejectedExecutionException("hệ thống bận");
    try {
        callThirdPartyApi();                          // tối đa 5 lời gọi song song
    } finally {
        sem.release();                               // BẮT BUỘC trong finally
    }
});
```

### Ba điểm khác biệt so với `synchronized` / `Lock`

| | `Semaphore` |
|---|---|
| Số thread vào cùng lúc | **N** (cấu hình), không phải luôn 1 |
| Gắn với chủ sở hữu (owner) | **Không** — thread A `acquire`, thread B `release` là hợp lệ |
| Reentrant | **Không** — cùng thread `acquire()` hai lần chiếm **hai** permit |

`acquire(n)` / `release(n)` lấy/trả nhiều permit. `release()` nhiều hơn `acquire()` → **tăng** tổng permit (bug nếu vô ý; hoặc chủ đích để "mở rộng hạn ngạch").

### Ứng dụng backend

Rate-limit phía client (tối đa 5 request song song tới API đối tác theo SLA), giới hạn số luồng ghi file/kết nối tới hệ thống ngoài, "bơm" giới hạn cho một hàng chờ virtual thread.

---

## 10. Atomic — CAS, ABA, LongAdder

`java.util.concurrent.atomic` đảm bảo **atomicity trên một biến** mà **không cần lock**, dựa trên lệnh CPU **CAS (Compare-And-Swap)**: "nếu giá trị hiện tại đúng bằng `expected` thì đổi thành `new`, trả về có đổi được không".

```java
AtomicInteger c = new AtomicInteger(0);
c.incrementAndGet();          // ++c, atomic
c.getAndIncrement();          // c++ (trả giá trị TRƯỚC khi tăng)
c.addAndGet(5);
c.compareAndSet(5, 10);       // nếu đang là 5 → thành 10, trả true/false
c.updateAndGet(n -> n * 2);   // Java 8 — áp một hàm, atomic (dùng CAS loop nội bộ)
c.accumulateAndGet(3, Integer::sum);
```

### CAS loop — nền tảng của lock-free

Cho check-then-act "còn vé thì giảm" (mục ví dụ oversold ở Module 05.1) — `get()` rồi `decrementAndGet()` **vẫn race**. Gộp atomic:

```java
boolean bookTicket() {
    while (true) {
        int cur = tickets.get();
        if (cur <= 0) return false;                     // hết vé
        if (tickets.compareAndSet(cur, cur - 1)) return true;   // đúng một thread "thắng"
        // CAS thất bại (thread khác vừa đổi) → lặp lại với giá trị mới nhất
    }
}
// hoặc gọn: int left = tickets.updateAndGet(n -> n > 0 ? n - 1 : n);
```

### ABA problem

Thread 1 đọc giá trị `A`. Thread 2 đổi `A → B → A`. Thread 1 `compareAndSet(A, C)` **thành công** dù thực tế giá trị đã "đi một vòng" — với cấu trúc dữ liệu lock-free (stack/queue trỏ node), điều này gây hỏng. Khắc phục: **`AtomicStampedReference`** — CAS kiểm tra **cả giá trị lẫn "tem" (version) tăng dần**.

### `LongAdder` / `LongAccumulator` — đếm dưới tranh chấp cao

```java
LongAdder hits = new LongAdder();
hits.increment();          // ghi vào "ô" (cell) riêng theo thread → giảm tranh chấp CAS
long total = hits.sum();   // gộp các cell — KHÔNG phải ảnh chụp nguyên tử tại một thời điểm
```

`AtomicLong` dưới nhiều thread cùng `incrementAndGet()` → CAS thất bại và retry liên tục (contention). `LongAdder` tách thành nhiều cell → nhanh hơn nhiều cho **đếm/thống kê**; nhược điểm: `sum()` không nhất quán tức thời. Dùng `AtomicLong` khi cần đọc giá trị chính xác thường xuyên.

### `AtomicReference` — hoán đổi object bất biến lock-free

```java
AtomicReference<Config> config = new AtomicReference<>(initial);
config.updateAndGet(old -> old.withTimeout(5000));   // thay bằng bản mới, atomic
```

---

## 11. Lock, ReadWriteLock, Condition

`ReentrantLock` làm mọi việc `synchronized` làm, **cộng thêm** những thứ `synchronized` không có (Module 05.1 đã trỏ tới đây).

```java
private final ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    // critical section
} finally {
    lock.unlock();          // ⚠️ BẮT BUỘC trong finally — quên = lock kẹt vĩnh viễn
}
```

### Những gì `synchronized` không có

```java
if (lock.tryLock()) { ... }                              // thử, không chờ
if (lock.tryLock(1, TimeUnit.SECONDS)) { ... }           // chờ có thời hạn → phá "no preemption" của deadlock
lock.lockInterruptibly();                                 // đang chờ lock vẫn interrupt được
new ReentrantLock(true);                                  // fair — cấp theo thứ tự chờ (chậm hơn, chống starvation)
```

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| Nhả lock | Tự động (hết khối / exception) | **Thủ công** `unlock()` trong `finally` |
| `tryLock` / timeout / interruptible | Không | Có |
| Fairness | Không | Tùy chọn |
| Số điều kiện chờ (`Condition`) | 1 (`wait`/`notify`) | Nhiều |
| Cú pháp | Gọn, khó rò rỉ | Dài, dễ quên `unlock` |

→ **Mặc định dùng `synchronized`**; chuyển sang `ReentrantLock` khi cần `tryLock`/timeout/interruptible/fairness/nhiều `Condition`.

### `Condition` — nhiều "phòng chờ" trên một lock

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition notFull  = lock.newCondition();
private final Condition notEmpty = lock.newCondition();

void put(T x) throws InterruptedException {
    lock.lock();
    try {
        while (count == capacity) notFull.await();       // chờ ĐÚNG điều kiện "chưa đầy"
        items[tail] = x; tail = (tail + 1) % capacity; count++;
        notEmpty.signal();                                // đánh thức ĐÚNG bên chờ "có phần tử"
    } finally { lock.unlock(); }
}
```

So với `wait`/`notifyAll` một phòng (Module 05.1): `signal()` đánh thức đúng nhóm cần thiết, không "đánh thức nhầm" bên kia.

### `ReadWriteLock` — nhiều reader song song, một writer độc quyền

```java
private final ReadWriteLock rw = new ReentrantReadWriteLock();

T read()  { rw.readLock().lock();  try { return data; } finally { rw.readLock().unlock(); } }
void write(T v) { rw.writeLock().lock(); try { data = v; } finally { rw.writeLock().unlock(); } }
```

Dùng khi **đọc >> ghi** (cache, bảng cấu hình). `StampedLock` (Java 8) — nhanh hơn nữa với "optimistic read" nhưng không reentrant, dễ dùng sai.

---

## 12. ConcurrentHashMap

`HashMap` không an toàn đa luồng (Module 03.1 — có thể mất update, JDK 7 còn vòng lặp vô hạn khi resize). `Collections.synchronizedMap` khóa toàn bảng mỗi thao tác → nghẽn. `ConcurrentHashMap` khóa mịn / lock-free cho đọc.

```java
ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();

counts.merge(key, 1L, Long::sum);                          // đếm — ATOMIC (khác HashMap.merge)
counts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(x);   // khởi tạo nhánh — atomic
counts.compute(key, (k, v) -> v == null ? 1L : v + 1L);
long total = counts.reduceValues(1000, Long::sum);         // duyệt song song
```

- **Không cho `null`** key hoặc value.
- `get`/đọc **không khóa**; iteration **weakly-consistent** — không ném `ConcurrentModificationException`, nhưng có thể không thấy thay đổi xảy ra sau khi iterator tạo.
- `size()` là **ước lượng** (không khóa toàn bảng) — đừng dùng cho logic chính xác.
- Hàm trong `compute`/`merge`/`computeIfAbsent` **giữ khóa một bin** trong lúc chạy → phải **nhanh**, **không gọi lại `map`** đó (nguy cơ deadlock/treo).
- `computeIfAbsent` + `merge` là cách "đọc-sửa-ghi trên một key" an toàn — thay cho `if (containsKey) ... else ...`.

Các cấu trúc concurrent khác cùng họ: `CopyOnWriteArrayList` (ghi hiếm, đọc nhiều, iterator không CME), `ConcurrentLinkedQueue` (không bound, lock-free), `ConcurrentSkipListMap` (sắp xếp, thay `TreeMap`).

---

## 13. Tổng kết — Bảng ghi nhớ nhanh

| Công cụ | Dùng khi |
|---|---|
| `ExecutorService` | Luôn thay cho `new Thread()`. `submit` → exception nằm trong `Future` (phải `get()`); `execute` → `stderr`. |
| Đóng pool | `shutdown()` → `awaitTermination()` → `shutdownNow()` (hai pha). Java 19+: try-with-resources. Quên `shutdown` → JVM không thoát. |
| `newFixedThreadPool` | Queue **không bound** → OOM khi tải cao. Chỉ dùng khi tải chắc chắn ổn định. |
| `newCachedThreadPool` | Không trần thread → cạn tài nguyên khi bùng task. |
| `ThreadPoolExecutor` | **Loại queue quyết định `max` có tác dụng không**: unbounded `LinkedBlockingQueue` → `max` bị bỏ qua; bounded queue → cấu hình đúng cho backend. |
| Rejection Policy | `AbortPolicy` (ném), `CallerRunsPolicy` (backpressure), `Discard*` (⚠️ mất việc). |
| `scheduleAtFixedRate` vs `WithFixedDelay` | Theo mốc thời gian cố định vs nghỉ cố định sau mỗi lần. Task ném exception → **lịch dừng im lặng** → luôn bọc try/catch. |
| Starvation deadlock | Task không được submit vào **cùng pool** rồi block chờ. |
| `BlockingQueue` | Producer/consumer không cần `wait`/`notify`. `put`/`take` chặn; `offer`/`poll` có timeout. |
| `Future` | `get()` blocking, ném `ExecutionException` (unwrap `getCause()`). Không callback, không compose. |
| `CompletableFuture` | `supplyAsync` không executor → **commonPool** (nguy hiểm cho I/O). `thenApply`=map, `thenCompose`=flatMap, `thenCombine`=zip. Lỗi bọc `CompletionException`. `allOf` + `join` để thu kết quả. `orTimeout`. |
| `CountDownLatch` | 1 thread chờ N thao tác (một lần). Mẫu start/end gate cho benchmark. |
| `CyclicBarrier` / `Phaser` | N thread chờ nhau, tái dùng (barrier) / số bên động (phaser). |
| `Semaphore` | Giới hạn N thread đồng thời. Không owner-bound, không reentrant. `tryAcquire(timeout)`. |
| `Atomic*` | CAS, không lock. `updateAndGet`/CAS loop cho check-then-act. **ABA** → `AtomicStampedReference`. |
| `LongAdder` | Đếm/thống kê dưới tranh chấp cao — nhanh hơn `AtomicLong`; `sum()` không nhất quán tức thời. |
| `ReentrantLock` | Khi cần `tryLock`/timeout/interruptible/fairness/nhiều `Condition`. Phải `unlock()` trong `finally`. Mặc định vẫn ưu tiên `synchronized`. |
| `ReadWriteLock` | Đọc >> ghi. |
| `ConcurrentHashMap` | `merge`/`compute*` atomic; không `null`; `size()` ước lượng; hàm trong `compute` phải nhanh, không gọi lại map. |

---

## 14. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn này có vấn đề gì? Điều gì xảy ra với JVM sau khi chạy xong?
```java
ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> System.out.println("done"));
```

**Câu 2.** Đoạn này in gì? Vì sao? Sửa thế nào để không "mất" lỗi.
```java
ExecutorService pool = Executors.newFixedThreadPool(2);
pool.submit(() -> { throw new IllegalStateException("nổ"); });
pool.shutdown();
```

**Câu 3.** `new ThreadPoolExecutor(2, 10, 60, SECONDS, new LinkedBlockingQueue<>())` — dưới tải 5000 task/giây kéo dài, pool có bao giờ chạy tới 10 thread không? Chuyện gì xảy ra với bộ nhớ? Sửa thế nào?

**Câu 4.** Đoạn `AtomicInteger` sau có an toàn đa luồng không? Giải thích điều xảy ra giữa (1) và (2). Sửa lại.
```java
private final AtomicInteger stock = new AtomicInteger(100);
public boolean buy(int qty) {
    if (stock.get() >= qty) {          // (1)
        stock.addAndGet(-qty);          // (2)
        return true;
    }
    return false;
}
```

**Câu 5.** `future.get()` và `completableFuture.thenApply(...)` khác nhau cốt lõi ở điểm nào về hành vi của **thread hiện tại**?

**Câu 6.** `scheduleAtFixedRate(job, 0, 1, SECONDS)` với `job` thỉnh thoảng ném `RuntimeException` (không catch). Sau vài giờ, job "ngừng chạy" mà không có log lỗi. Vì sao? Sửa thế nào?

**Câu 7.** Hai task cùng submit vào `newFixedThreadPool(2)`; mỗi task lại `pool.submit(...)` một task con rồi `.get()` chờ. Chuyện gì xảy ra? Tên hiện tượng?

**Câu 8.** `ConcurrentHashMap`: vì sao `map.computeIfAbsent(k, ...).add(x)` an toàn còn `if (!map.containsKey(k)) map.put(k, new ArrayList<>()); map.get(k).add(x);` thì không?

---

### Phần B — Bài tập viết code

**Bài 1 — Song song vs tuần tự.**
Mô phỏng gọi 5 "API" (mỗi cái `sleep` ngẫu nhiên 500–2000 ms rồi trả `int`). Gọi song song bằng `newFixedThreadPool(5)` + `invokeAll`, cộng tổng; đo thời gian và so với gọi tuần tự. Đóng pool bằng mẫu hai pha. Giải thích vì sao thời gian song song ≈ API chậm nhất.

**Bài 2 — CompletableFuture pipeline + xử lý lỗi.**
Pipeline đơn hàng bất đồng bộ: `validate()` → `calcTotal()` → `charge()` → `sendEmail()`, mỗi bước `sleep(300)` và truyền giá trị cho bước sau. Dùng `thenApply`/`thenCompose` (một bước trả `CompletableFuture`). Thêm `exceptionally` xử lý `validate()` ném (đơn không hợp lệ) → trả kết quả "đã hủy". Chạy trên executor **riêng**, không dùng commonPool.

**Bài 3 — allOf thu kết quả.**
Cho danh sách 10 `id`. `fetchAsync(id)` trả `CompletableFuture<Integer>` (`sleep` ngẫu nhiên). Gọi cả 10 song song, dùng `allOf(...).thenApply(v -> ...join...)` để thu về `List<Integer>` đúng thứ tự, tính tổng. Thêm `orTimeout(3, SECONDS)` cho từng future và xử lý phần tử timeout bằng giá trị 0.

**Bài 4 — start/end gate benchmark.**
Đo throughput của `AtomicLong.incrementAndGet()` vs `LongAdder.increment()` vs `synchronized` counter, mỗi cái 8 thread × 1_000_000 lần. Dùng `CountDownLatch` start gate (thả đồng loạt) + end gate (chờ xong) để đo công bằng. In bảng ns/op, giải thích vì sao `LongAdder` thắng khi tranh chấp cao.

**Bài 5 — Semaphore rate-limit.**
20 thread cùng "gọi API đối tác" (`sleep(1000)`), nhưng `Semaphore(3)` đảm bảo tối đa 3 lời gọi song song. Dùng thêm một `AtomicInteger` đếm số cuộc gọi đang active, in ngay sau `acquire()` và trước `release()` — chứng minh không bao giờ vượt 3. Thêm `tryAcquire(200, MILLISECONDS)`: thread không xin được permit trong 200 ms thì bỏ cuộc và log "bận".

**Bài 6 — BlockingQueue producer/consumer.**
1 producer đẩy 100 "job" vào `ArrayBlockingQueue(10)`; 4 consumer `take()` và xử lý (`sleep` ngẫu nhiên). Dùng "poison pill" (job đặc biệt) để báo consumer dừng. `CountDownLatch` để main biết cả 4 consumer đã kết thúc. Không dùng `wait`/`notify` tay.

**Bài 7 — Bài toán tổng hợp: phòng vé bằng ExecutorService + CAS loop (chuẩn bị capstone).**
`TicketBooth` với `AtomicInteger available = new AtomicInteger(100)`. `boolean book()` dùng **CAS loop** (`compareAndSet`) để "kiểm tra còn vé + giảm 1" thành một thao tác nguyên tử. Submit 500 task `book()` vào `newFixedThreadPool(50)`; đếm số `true` bằng `AtomicInteger`/`LongAdder`; `CountDownLatch(500)` để main chờ; in tổng vé bán — **luôn đúng 100, không oversold**. Ghi chú vì sao `get()` rồi `decrementAndGet()` riêng lẻ vẫn sai, và backend nhiều instance cần thêm distributed lock / `SELECT … FOR UPDATE` / `@Version` (Module 14, 18).

---

### Phần C — Nâng cao

**Câu 1.** Phân biệt hành vi khi task ném exception với `execute(Runnable)` vs `submit(Callable)`. Vì sao "submit rồi không bao giờ `get()`" là một class bug âm thầm nguy hiểm? Nêu ba cách đảm bảo mọi lỗi task đều được ghi log.

**Câu 2.** Với `ThreadPoolExecutor`, chứng minh bằng lập luận: `LinkedBlockingQueue` **không bound** khiến `maximumPoolSize` trở nên vô nghĩa. `SynchronousQueue` khác thế nào? Viết một cấu hình `ThreadPoolExecutor` "đúng cho backend" cho tác vụ I/O 8 nhân, gọi API ~100 ms, và giải thích từng tham số.

**Câu 3.** `CompletableFuture.supplyAsync(task)` (không executor) chạy trên `ForkJoinPool.commonPool()`. Nêu ba hệ quả trong một service Spring Boot nhiều request đồng thời khi `task` là I/O (gọi DB/HTTP). Vì sao `thenApply` vs `thenApplyAsync` cho kết quả "chạy trên thread nào" khác nhau, và khi nào phải dùng bản `Async`?

**Câu 4.** CAS loop cho "còn vé thì giảm": viết bản `compareAndSet` tường minh và bản `updateAndGet`, chỉ ra chúng tương đương. Giải thích **ABA problem** bằng ví dụ một lock-free stack (push/pop trỏ `head`), và vì sao `AtomicStampedReference` khắc phục. Bài toán vé có bị ABA không — vì sao?

**Câu 5.** `LongAdder` vs `AtomicLong` dưới tranh chấp cao: giải thích cơ chế "striping" (nhiều cell). Vì sao `LongAdder.sum()` **không** phải ảnh chụp nguyên tử, và điều đó chấp nhận được cho metric nhưng **không** cho "số dư tài khoản"? Khi nào `AtomicLong` vẫn là lựa chọn đúng?

**Câu 6.** `ReentrantLock` với hai `Condition` (`notFull`/`notEmpty`) so với một monitor `wait`/`notifyAll` (Module 05.1): giải thích vì sao `signal()` đúng `Condition` tránh được "đánh thức nhầm" mà `notify()` một phòng gặp phải. Cái giá phải trả của `ReentrantLock` so với `synchronized` là gì, và vì sao `unlock()` **bắt buộc** trong `finally`?

**Câu 7.** `ConcurrentHashMap.compute(key, remappingFn)` giữ khóa một bin trong lúc chạy `remappingFn`. Nêu ba điều `remappingFn` **không được làm** và hậu quả từng cái. Vì sao `size()` chỉ là ước lượng, và `map.forEach` không ném `ConcurrentModificationException` (khác `HashMap`) nhưng cũng không đảm bảo thấy gì?

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. Quên `pool.shutdown()`. Thread trong pool **không phải daemon** → JVM không tự thoát dù task đã xong; chương trình treo tới khi bị tắt thủ công.
2. **Không in gì.** `submit` bọc exception vào `Future`; không ai gọi `get()` nên `IllegalStateException` biến mất, không log. Sửa: dùng `execute(...)` (đi tới `stderr`/`UncaughtExceptionHandler`), hoặc bọc `try/catch` trong task, hoặc giữ `Future` và gọi `get()`, hoặc `afterExecute` hook.
3. **Không bao giờ tới 10 thread.** `LinkedBlockingQueue` không bound → mọi task xếp được vào queue → bước "tạo thread vượt core" không kích hoạt → pool đứng ở 2 thread, queue phình vô hạn → `OutOfMemoryError`. Sửa: dùng `new ArrayBlockingQueue<>(N)` (bound) + `CallerRunsPolicy` → queue đầy mới tạo thread tới 10, max đầy mới đẩy ngược lại caller (backpressure).
4. **Không an toàn.** `get()` và `addAndGet()` mỗi cái atomic riêng, nhưng **cặp** không nguyên tử. Giữa (1) và (2), thread khác có thể mua hết stock → (1) đã "lỗi thời" khi (2) chạy → bán âm. Sửa bằng CAS loop: `while(true){ int c=stock.get(); if(c<qty) return false; if(stock.compareAndSet(c, c-qty)) return true; }`.
5. `future.get()` **blocking** — thread hiện tại dừng chờ. `thenApply(...)` đăng ký callback — thread hiện tại **chạy tiếp ngay**, callback tự chạy sau (trên thread hoàn thành bước trước hoặc pool) khi có kết quả.
6. Task định kỳ ném exception không catch → `ScheduledExecutorService` coi task đó "thất bại" và **hủy lịch lặp**, không ném ra đâu cả (nằm trong `Future` không ai đọc). Sửa: bọc **toàn bộ** thân task trong `try/catch(Throwable)` + log, để exception không bao giờ thoát ra ngoài `run()`.
7. **Thread starvation deadlock:** 2 task cha chiếm cả 2 thread của pool, cùng block ở `.get()` chờ task con; task con nằm trong queue không có thread nào rảnh để chạy → treo vĩnh viễn. Task không được submit-rồi-block vào cùng pool.
8. `computeIfAbsent` thực hiện "kiểm tra vắng + đặt" **atomic trên một key** (giữ khóa bin). Bản `containsKey`/`put`/`get` là ba thao tác rời — hai thread cùng qua `!containsKey` → cả hai `put` list mới → một list (kèm phần tử vừa `add`) bị ghi đè mất.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `List<Future<Integer>> fs = pool.invokeAll(tasks); int sum = 0; for (var f : fs) sum += f.get();`. Song song ≈ max(thời gian 5 API) vì cả 5 chạy đồng thời trên 5 thread; tuần tự = tổng 5 thời gian.
- **Bài 2:** `validate(order).thenCompose(o -> calcTotal(o)).thenApply(this::charge).thenCompose(this::sendEmail).exceptionally(ex -> "đã hủy: " + ex.getCause().getMessage())` — tất cả với executor thứ hai truyền vào mỗi `*Async`.
- **Bài 3:** `var fs = ids.stream().map(id -> fetchAsync(id).completeOnTimeout(0, 3, SECONDS)).toList(); CompletableFuture.allOf(fs.toArray(CompletableFuture[]::new)).thenApply(v -> fs.stream().map(CompletableFuture::join).toList()).join()`.
- **Bài 4:** `AtomicLong` ~15–40 ns/op dưới 8 thread; `LongAdder` ~3–8 ns/op; `synchronized` ~30–80 ns/op (tùy CPU). `LongAdder` thắng vì mỗi thread cập nhật cell riêng → gần như không CAS-retry; `AtomicLong` mọi thread đập vào một ô → CAS thất bại và lặp liên tục.
- **Bài 5:** `AtomicInteger active`; sau `acquire()`: `int now = active.incrementAndGet(); assert now <= 3;` ; trước `release()`: `active.decrementAndGet()`. `if (!sem.tryAcquire(200, MILLISECONDS)) { log("bận"); return; }`.
- **Bài 6:** Producer đẩy 100 job rồi đẩy 4 poison pill; consumer `while(true){ Job j = q.take(); if (j == POISON) break; process(j); }` rồi `latch.countDown()`. Main: `latch.await()`.
- **Bài 7:** `boolean book(){ while(true){ int c=available.get(); if(c<=0) return false; if(available.compareAndSet(c,c-1)) return true; } }`. `get()`+`decrementAndGet()` rời nhau: hai thread cùng đọc `c==1`, cả hai giảm → `-1` (oversold). Tổng `true` = 100 chính xác vì mỗi lần giảm thành công là một CAS "thắng" duy nhất.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. `execute`: exception lan tới `Thread.UncaughtExceptionHandler` (mặc định in stack trace ra `stderr`, thread worker chết rồi pool tạo thread thay thế). `submit`: exception bị "bắt" và lưu trong `Future`; chỉ ném ra (bọc `ExecutionException`) khi gọi `get()`. "Submit rồi không `get()`" nguy hiểm vì lỗi **hoàn toàn im lặng** — không log, không metric, không stack trace; bug (dữ liệu không được xử lý) chỉ lộ ra rất muộn qua hậu quả. Ba cách: (i) `try/catch(Throwable)` + log bao trọn thân mỗi task; (ii) override `ThreadPoolExecutor.afterExecute(r, t)` để log `t` (và unwrap `Future` nếu `r instanceof Future`); (iii) dùng `CompletableFuture` với `.whenComplete((v, ex) -> if (ex != null) log...)` bắt buộc ở cuối chuỗi.
2. `LinkedBlockingQueue` không bound: `offer()` **luôn thành công** → `ThreadPoolExecutor` không bao giờ tới nhánh "số thread < max thì tạo thêm" → pool đứng ở `corePoolSize`, `maximumPoolSize`/`keepAliveTime` thành số trang trí; queue phình → OOM. `SynchronousQueue`: sức chứa 0, `offer()` chỉ thành công nếu có thread đang `poll` chờ → task mới luôn buộc tạo thread tới `max`, hết `max` → reject ngay (không tích lũy). Cấu hình I/O 8 nhân, ~100 ms/call: `core = 16`, `max = 64` (≈ `8 × (1 + 90/10)`, làm tròn), `keepAlive = 60s`, queue = `ArrayBlockingQueue(200)` (trần bộ nhớ ~200 task chờ), `CallerRunsPolicy` (khi quá tải, đẩy việc về caller → tự giảm nhịp nhận request). Đo p99 rồi chỉnh.
3. (i) `commonPool` dùng chung toàn JVM, kích thước ≈ nhân − 1 → vài chục request I/O song song là cạn, mọi `parallelStream`/`CompletableFuture` khác trong tiến trình đứng hình. (ii) Thread `commonPool` bị **block** trong `read()` mạng — fork/join thiết kế cho tác vụ CPU ngắn, không co giãn theo tải I/O. (iii) Không cách ly lỗi/tài nguyên giữa các request. `thenApply` chạy callback **trên thread vừa hoàn thành stage trước** (hoặc thread gọi nếu future đã xong) — nếu stage trước xong trên thread I/O đang cần giải phóng, callback nặng sẽ giữ thread đó; `thenApplyAsync(fn, pool)` đẩy callback sang `pool` chỉ định. Dùng `Async` khi callback tốn CPU/thời gian hoặc cần tách khỏi pool hoàn thành.
4. `compareAndSet`: `while(true){ int c=t.get(); if(c<=0) return false; if(t.compareAndSet(c,c-1)) return true; }`. `updateAndGet`: `int left = t.updateAndGet(n -> n>0 ? n-1 : n); return left < /*snapshot?*/ ...` — thực ra `updateAndGet` nội bộ **chính là** CAS loop trên, nên tương đương; chỉ cần biết "có giảm được không" thì bản `compareAndSet` tường minh rõ hơn. ABA (lock-free stack): T1 đọc `head = A` (định `pop`, `next` của A là B). T2 `pop` A, `pop` B, `push` A lại → `head = A` nhưng giờ `A.next` khác. T1 `compareAndSet(head, A, B)` **thành công** dù B đã không còn trong stack → hỏng. `AtomicStampedReference` gắn tem version: CAS so cả `(ref, stamp)` → T2 đã tăng stamp nên T1 CAS trượt, phải đọc lại. Bài toán vé **không** bị ABA vì giá trị là số đếm giảm đơn điệu (`int`), không "quay lại giá trị cũ theo cách có ý nghĩa khác" — và kể cả trùng giá trị thì ngữ nghĩa vẫn đúng (còn n vé là còn n vé).
5. `LongAdder` giữ một mảng `Cell`; mỗi thread hash vào một cell riêng, `increment()` chỉ CAS cell của mình → gần như không đụng độ. `AtomicLong` mọi thread CAS **một** ô → dưới N thread, tỉ lệ CAS-fail-retry tăng theo N. `sum()` cộng dồn các cell **không khóa** → trong lúc cộng, cell đã cộng có thể lại thay đổi → kết quả là "giá trị tại một thời điểm mờ", đủ tốt cho đếm request/hit-rate nhưng sai cho "số dư" (cần đọc-ghi nhất quán, dùng lock hoặc `AtomicLong`). `AtomicLong` đúng khi: cần `get()` chính xác thường xuyên, hoặc cần `compareAndSet`/`updateAndGet` (CAS loop) — `LongAdder` không có.
6. Một monitor + `notify()`: chỉ đánh thức **một** thread chờ ngẫu nhiên trong tập chờ chung; nếu tập đó lẫn cả "thread chờ chưa-đầy" và "thread chờ chưa-rỗng", `notify()` sau một `take()` có thể trúng một thread `take()` khác (điều kiện của nó vẫn sai → ngủ lại) trong khi thread `put()` cần được báo thì không → kẹt; phải `notifyAll()` (đánh thức tất cả, tốn hơn). Hai `Condition` tách tập chờ: `notEmpty.signal()` chỉ đụng đúng nhóm chờ "có phần tử". Cái giá của `ReentrantLock`: dài dòng hơn, và **quên `unlock()` = lock kẹt vĩnh viễn** (không có cơ chế tự nhả như `synchronized` khi thoát khối/exception) → bắt buộc `try { ... } finally { lock.unlock(); }`.
7. `remappingFn` **không được**: (i) gọi lại `map.compute`/`put`/`computeIfAbsent` trên **cùng** map — có thể deadlock/`IllegalStateException` (đang giữ khóa bin đó). (ii) chạy lâu / block I/O — giữ khóa bin làm nghẽn mọi thao tác khác trên các key cùng bin. (iii) có side-effect không idempotent — `compute` có thể (hiếm) chạy lại hàm. `size()` ước lượng vì `ConcurrentHashMap` không giữ một biến đếm có khóa toàn cục (sẽ thành điểm nghẽn) — nó cộng các bộ đếm phân tán (`CounterCell`) không khóa. `forEach`/iterator **weakly-consistent**: duyệt trên trạng thái tại/sau lúc tạo iterator, phản ánh **một số** thay đổi đồng thời nhưng không đảm bảo thấy hết; đổi lại không bao giờ ném `ConcurrentModificationException`.

</details>

---

*File tiếp theo trong lộ trình: **Module 06 — Java Modern (8 → 21+)** (Optional, record, sealed class, pattern matching, Text Block, Virtual Threads).*
