# Module 05.1 — Thread cơ bản

> **Mức độ ưu tiên: Cao** — Backend xử lý hàng nghìn request đồng thời, mỗi request thường một thread. Không hiểu race condition / visibility / deadlock sẽ tạo ra bug **chỉ xuất hiện dưới tải cao, không tái hiện được bằng breakpoint** — loại câu hỏi kinh điển để phân biệt Junior với Middle/Senior.

> **Phạm vi bài này:** mô hình bộ nhớ luồng, tạo & vòng đời `Thread`, race condition, `synchronized`, `volatile`, `wait`/`notify`, deadlock, và các method điều khiển thread ở mức JDK lõi. **Chỉ nhắc tên, không đi sâu:** `ExecutorService`/thread pool, `Future`/`CompletableFuture`, `java.util.concurrent.locks.Lock`, `Atomic*`, `CountDownLatch`/`Semaphore` (tất cả ở Module 05.2), virtual thread (Java 21 — nêu vừa đủ). `InterruptedException` đã học ở Module 04, ở đây chỉ nhắc phần liên quan.

---

## Mục lục

1. [Process vs Thread](#1-process-vs-thread)
2. [Tạo Thread — `Thread` vs `Runnable`](#2-tạo-thread--thread-vs-runnable)
3. [Thread Lifecycle](#3-thread-lifecycle)
4. [Race Condition — ba mối nguy: atomicity, visibility, reordering](#4-race-condition--ba-mối-nguy-atomicity-visibility-reordering)
5. [Java Memory Model & quan hệ happens-before](#5-java-memory-model--quan-hệ-happens-before)
6. [`synchronized`](#6-synchronized)
7. [`volatile`](#7-volatile)
8. [`synchronized` vs `volatile` — khi nào dùng cái nào](#8-synchronized-vs-volatile--khi-nào-dùng-cái-nào)
9. [`wait` / `notify` / `notifyAll` — guarded block](#9-wait--notify--notifyall--guarded-block)
10. [Deadlock & các lỗi liveness](#10-deadlock--các-lỗi-liveness)
11. [Các method điều khiển Thread](#11-các-method-điều-khiển-thread)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. Process vs Thread

| Tiêu chí | Process (Tiến trình) | Thread (Luồng) |
|---|---|---|
| Định nghĩa | Một chương trình đang chạy, có không gian địa chỉ **riêng** | Một đơn vị thực thi **bên trong** một process, chia sẻ bộ nhớ với thread khác cùng process |
| Bộ nhớ | Heap / không gian địa chỉ riêng | Cùng process → **chung Heap, Metaspace, biến `static`**; mỗi thread có **Stack + Program Counter riêng** |
| Giao tiếp | IPC (socket, pipe, shared memory) — phức tạp | Chia sẻ biến trên Heap trực tiếp — đơn giản, nhưng đây chính là nguồn của race condition |
| Chi phí tạo/chuyển đổi | Nặng | Nhẹ hơn nhiều, nhưng **context switch** vẫn tốn (lưu/khôi phục thanh ghi, xả cache, cập nhật scheduler) |
| Ví dụ | Mỗi `java -jar app.jar` là một process | Trong một Spring Boot app, mỗi request HTTP chạy trên một thread lấy từ pool |

```
Process (một JVM)
 ├── Heap            ← MỌI thread dùng chung (object, mảng)
 ├── Metaspace       ← dùng chung (metadata class, biến static)
 ├── Thread 1 → Stack riêng (biến local, khung gọi hàm), PC riêng
 ├── Thread 2 → Stack riêng, PC riêng
 └── Thread 3 → Stack riêng, PC riêng
```

> **Vì sao race condition tồn tại:** thread chia sẻ Heap. Nhiều thread cùng đọc/ghi một object trên Heap **không kiểm soát** → dữ liệu sai lệch (mục 4). Biến **local trên Stack** thì an toàn tự nhiên — mỗi thread một bản.

> **Thread trong Java = thread hệ điều hành (platform thread).** Mỗi thread tốn ~1 MB stack (`-Xss`) → tạo hàng vạn thread là không khả thi. **Virtual thread (Java 21)** — `Thread.ofVirtual().start(task)` — là luồng cực nhẹ do JVM tự lập lịch trên số ít "carrier thread", cho phép hàng triệu luồng; cách viết code giữ nguyên (vẫn viết tuần tự, blocking). Chi tiết ngoài phạm vi bài.

---

## 2. Tạo Thread — `Thread` vs `Runnable`

### Cách 1: `extends Thread`

```java
public class MyThread extends Thread {
    @Override public void run() {
        System.out.println("chạy trên: " + Thread.currentThread().getName());
    }
}
new MyThread().start();   // start() → JVM tạo luồng OS mới, gọi run() trên luồng đó
```

### Cách 2: `implements Runnable` (khuyến nghị)

```java
public class MyTask implements Runnable {
    @Override public void run() { /* ... */ }
}
new Thread(new MyTask()).start();
new Thread(() -> System.out.println("lambda")).start();   // Runnable là functional interface (Module 03.3)
```

### Vì sao ưu tiên `Runnable`

1. Java không đa kế thừa class (Module 02.1) — `extends Thread` khóa mất khả năng `extends` class khác.
2. Tách "việc cần chạy" (`Runnable`) khỏi "cơ chế chạy" (`Thread`) — đúng tinh thần SRP (Module 01.6).
3. `Runnable`/`Callable` dùng trực tiếp với **thread pool** (`ExecutorService` — Module 05.2), là cách quản lý thread chuẩn trong backend; hầu như không ai `new Thread()` thủ công trong code production.

> `Runnable.run()` **không trả giá trị** và **không `throws` checked exception**. Khi cần trả kết quả / ném checked exception → `Callable<V>` (`V call() throws Exception`) — nhưng chạy `Callable` cần `ExecutorService` hoặc `FutureTask` (Module 05.2).

> ⚠️ **Gọi `run()` trực tiếp KHÔNG tạo thread mới** — nó chạy như method thường trên thread hiện tại:
> ```java
> t.run();    // ❌ tuần tự trên thread hiện tại
> t.start();  // ✅ luồng mới thực sự
> ```

### Exception không bắt được trong thread

```java
Thread t = new Thread(() -> { throw new RuntimeException("nổ trong thread"); });
t.setUncaughtExceptionHandler((th, ex) -> log.error("Thread {} chết:", th.getName(), ex));
t.start();
```

- Exception **không lan về** thread đã gọi `start()`. Thread ném exception sẽ **chết**, các thread khác không bị ảnh hưởng.
- Không set handler → JVM in stack trace ra `stderr` rồi thread kết thúc. Đặt mặc định toàn cục: `Thread.setDefaultUncaughtExceptionHandler(...)`.

### Daemon thread

```java
Thread bg = new Thread(this::pollMetrics);
bg.setDaemon(true);   // PHẢI gọi TRƯỚC start()
bg.start();
```

JVM thoát ngay khi mọi thread **không phải daemon** kết thúc — daemon thread bị "cắt" giữa chừng, `finally` có thể không chạy. Dùng cho tác vụ nền phụ trợ (metrics, cleanup), **không** cho việc cần hoàn tất (ghi file, flush DB).

---

## 3. Thread Lifecycle

`Thread.State` — enum sáu trạng thái:

```
   new Thread()
        │
        ▼
     ┌─────┐   start()   ┌──────────┐
     │ NEW │ ──────────► │ RUNNABLE │ ◄──── notify()/notifyAll(), hết sleep/join, giành được lock
     └─────┘             └──┬───┬───┘
                            │   │ synchronized bị thread khác giữ
              wait()/join() │   ▼
              /sleep(t)     │  ┌─────────┐
                            │  │ BLOCKED │  (chờ MONITOR lock)
                            ▼  └─────────┘
              ┌──────────────────────────┐
              │ WAITING / TIMED_WAITING  │  (chờ có điều kiện / có thời hạn)
              └──────────────────────────┘
                            │ run() kết thúc (hoặc ném exception)
                            ▼
                     ┌────────────┐
                     │ TERMINATED │  ← không start() lại được
                     └────────────┘
```

| Trạng thái | Ý nghĩa |
|---|---|
| `NEW` | Đã tạo object `Thread`, chưa `start()` |
| `RUNNABLE` | **Gộp ba tình huống**: đang chạy trên CPU · chờ CPU lập lịch · **đang chờ I/O của hệ điều hành** (JVM không phân biệt được thread đang kẹt trong syscall đọc file/socket) |
| `BLOCKED` | Chờ giành **monitor lock** (`synchronized`) đang bị thread khác giữ |
| `WAITING` | Chờ vô hạn: `Object.wait()`, `Thread.join()`, `LockSupport.park()` |
| `TIMED_WAITING` | Như trên nhưng có thời hạn: `sleep(t)`, `wait(t)`, `join(t)` |
| `TERMINATED` | `run()` đã xong (hoặc ném exception thoát ra) |

```java
t.getState();   // Thread.State
t.isAlive();    // true nếu đã start() và chưa TERMINATED
```

> ⚠️ `start()` **hai lần** trên cùng object → `IllegalThreadStateException`. Mỗi object `Thread` chạy được đúng một lần.

---

## 4. Race Condition — ba mối nguy: atomicity, visibility, reordering

**Race condition** = kết quả phụ thuộc **thứ tự xen kẽ ngẫu nhiên** giữa các thread khi cùng truy cập dữ liệu chung. Có **ba** mối nguy độc lập:

### 4.1. Mất tính nguyên tử (atomicity) — lost update

```java
public void increment() { count++; }   // KHÔNG nguyên tử — thực chất 3 bước
```

`count++` ở tầng bytecode = **đọc → tăng → ghi**:

```
Thời điểm   Thread A            Thread B            count trong Heap
t1          đọc count = 0                           0
t2                              đọc count = 0        0   ← B đọc trước khi A ghi
t3          tăng (local) → 1                        0
t4                              tăng (local) → 1     0
t5          ghi count = 1                           1
t6                              ghi count = 1        1   ← B ghi đè kết quả của A
KẾT QUẢ: 1 — lẽ ra phải là 2
```

```java
for (int i = 0; i < 1000; i++) new Thread(counter::increment).start();
// ... join tất cả ...
counter.getCount();   // thường < 1000, đổi mỗi lần chạy
```

### 4.2. Mất tính hiển thị (visibility)

Thread B có thể **không bao giờ thấy** giá trị Thread A vừa ghi — do JIT giữ biến trong thanh ghi, hoặc giá trị nằm trong cache của core khác chưa đồng bộ về bộ nhớ chính (mục 7).

### 4.3. Sắp xếp lại lệnh (reordering)

Trình biên dịch / CPU được phép **đổi thứ tự** các lệnh không phụ thuộc nhau (để tối ưu). Trong một thread thì vô hại; nhìn từ thread khác thì thấy các ghi "xảy ra sai thứ tự" — nguồn của nhiều bug tinh vi (ví dụ double-checked locking hỏng nếu thiếu `volatile`).

### 4.4. Word tearing với `long` / `double`

```java
long balance;             // gán 64-bit KHÔNG đảm bảo nguyên tử khi thiếu volatile (JLS cho phép tách 2 nửa 32-bit)
volatile long balance2;   // gán nguyên tử
```

Thread khác có thể đọc được "nửa cao mới + nửa thấp cũ". `int`/`boolean`/reference thì gán luôn nguyên tử.

### 4.5. Check-then-act

```java
if (instance == null) instance = new Heavy();   // hai thread cùng qua "if" → tạo 2 instance
if (tickets > 0) tickets--;                      // hai thread cùng qua "if" khi tickets == 1 → tickets = -1 (oversold)
```

> **Bug đa luồng khó chịu nhất:** code compile sạch, test vài lần vẫn đúng (may mắn về thời điểm), nhưng dưới tải production thì sai — và không tái hiện được bằng debugger vì breakpoint làm đổi timing.

---

## 5. Java Memory Model & quan hệ happens-before

JMM định nghĩa: một ghi ở thread A **được đảm bảo nhìn thấy** ở thread B **khi và chỉ khi** có quan hệ **happens-before (hb)** từ ghi đó tới đọc của B. Không có hb → không có bảo đảm gì (dù thực tế đôi khi vẫn "chạy đúng" do may).

### Các quy tắc happens-before quan trọng

| Nguồn hb | Nội dung |
|---|---|
| **Program order** | Trong một thread, lệnh viết trước hb lệnh viết sau |
| **Monitor lock** | `unlock` một monitor hb mọi `lock` **sau đó** trên **cùng** monitor |
| **volatile** | Ghi một biến `volatile` hb mọi lần đọc **sau đó** trên cùng biến |
| **Thread start** | `t.start()` hb mọi lệnh trong thân `t` |
| **Thread join** | Mọi lệnh trong thân `t` hb `t.join()` trả về |
| **Bắc cầu** | A hb B và B hb C ⇒ A hb C |
| **final field** | Giá trị `final` field gán trong constructor được nhìn thấy đúng ở mọi thread — **nếu** `this` không "thoát" (escape) khỏi constructor |

### "Piggyback" — ghi volatile công bố mọi ghi trước nó

```java
int data;                 // KHÔNG volatile
volatile boolean ready;    // volatile

// Thread A:
data = 42;
ready = true;              // ghi volatile → tạo hàng rào, "đẩy" cả data=42 về bộ nhớ chính

// Thread B:
if (ready) {               // đọc volatile
    assert data == 42;     // ĐẢM BẢO đúng — nhờ hb: (data=42 hb ready=true) hb (đọc ready) hb (đọc data)
}
```

> Đây là nền tảng của mọi cơ chế đồng bộ: `synchronized`, `volatile`, `Thread.join()`, `Atomic*`, `BlockingQueue`... tất cả đều "hoạt động" vì chúng thiết lập quan hệ happens-before.

---

## 6. `synchronized`

Đảm bảo tại một thời điểm **chỉ một thread** thực thi vùng code được bảo vệ (**critical section**) trên **cùng một object khóa** — và thiết lập happens-before (nên `synchronized` sửa **cả** atomicity **lẫn** visibility).

### `synchronized` method vs block

```java
public synchronized void increment() { count++; }   // khóa trên "this", cả method

private final Object lock = new Object();
public void increment() {
    doExpensiveUnrelatedWork();      // KHÔNG khóa phần này
    synchronized (lock) { count++; } // chỉ khóa đúng phần đụng dữ liệu chung
}
```

> Khóa **ít code nhất có thể** — thread khác chờ ít hơn. Nhưng đừng chia nhỏ tới mức một chuỗi thao tác cần-nguyên-vẹn bị tách làm hai khối `synchronized` (lại race).

### Bốn điều dễ sai về object khóa

```java
public static synchronized void m() { }   // khóa trên ClassName.class (KHÁC với "this")

// ĐỪNG khóa trên:
synchronized ("key") { }                   // ✗ String literal bị intern → chia sẻ toàn JVM
synchronized (Integer.valueOf(1)) { }       // ✗ wrapper cache (-128..127) → chia sẻ ngoài ý muốn
synchronized (this.boxedField) { }          // ✗ field bị gán lại → mỗi lúc khóa một object khác
// NÊN: private final Object lock = new Object();  (không lộ ra ngoài, không đổi)
```

### Reentrant — lock đếm số lần

```java
public synchronized void a() { b(); }   // thread đang giữ lock this...
public synchronized void b() { }         // ...vào b() được luôn (cùng lock, cùng thread). Nhả hết khi thoát a()
```

### Bảo đảm & hạn chế

- **Bảo đảm:** loại trừ lẫn nhau + visibility (hàng rào bộ nhớ khi vào/ra) + reentrant.
- **Không** có: timeout, khả năng bị `interrupt` khi đang chờ lock, tính công bằng (fairness), nhiều condition. Cần những thứ đó → `ReentrantLock`/`ReadWriteLock` (Module 05.2).
- **Không gọi "alien method"** (method của class khác/override được) khi đang giữ lock — dễ kéo theo deadlock/liveness nếu method đó lại giành lock khác.

```java
Counter c = new Counter();
for (int i = 0; i < 1000; i++) new Thread(c::increment).start();
// ... join ...
c.getCount();   // LUÔN = 1000
```

---

## 7. `volatile`

Giải quyết **visibility** và **reordering** — **không** giải quyết atomicity.

### Vấn đề: cờ dừng không thấy

```java
public class Worker {
    private boolean running = true;                 // KHÔNG volatile
    public void stop()    { running = false; }       // Thread A
    public void doWork()  { while (running) { } }     // Thread B — CÓ THỂ lặp vô hạn
}
```

JIT được phép "nâng" `running` ra khỏi vòng lặp (đọc một lần vào thanh ghi) → Thread B không bao giờ thấy `false`.

### `volatile` — mọi đọc/ghi đi thẳng bộ nhớ chính + tạo happens-before

```java
private volatile boolean running = true;   // sửa xong: Thread B thấy false ngay
```

### Dùng `volatile` đúng chỗ

| Hợp lệ | Không hợp lệ |
|---|---|
| Cờ dừng / cờ trạng thái ghi một nơi, đọc nhiều nơi | `count++`, `balance -= x` (đọc-sửa-ghi) |
| Công bố **an toàn** một object bất biến vừa dựng xong (safe publication) | Bất biến liên quan **nhiều** biến (`lo <= hi`) |
| "Quan sát độc lập": nhiệt độ hiện tại, timestamp cập nhật gần nhất | Đếm, tích lũy, so-sánh-rồi-đặt |
| Cờ trong double-checked locking (bắt buộc `volatile`) | |
| Làm `long`/`double` gán nguyên tử | |

```java
private volatile int count = 0;
public void increment() { count++; }   // ❌ VẪN race — volatile không làm 3 bước thành 1
```

→ Cho `count++`: dùng `synchronized` hoặc `AtomicInteger` (Module 05.2).

### `final` field — công bố an toàn không cần `volatile`/`synchronized`

Object **bất biến** (mọi field `final`, `this` không thoát khỏi constructor) được nhìn thấy đúng ở mọi thread ngay cả khi chia sẻ qua data race. Đây là lý do `String`, `Integer`, `record` an toàn khi chia sẻ.

---

## 8. `synchronized` vs `volatile` — khi nào dùng cái nào

| Tiêu chí | `synchronized` | `volatile` |
|---|---|---|
| Atomicity (đọc-sửa-ghi) | ✅ | ❌ |
| Visibility | ✅ | ✅ |
| Chặn reordering | ✅ (quanh critical section) | ✅ (quanh biến volatile) |
| Thread có thể bị BLOCKED | Có (chờ lock) | Không (không có lock) |
| Reentrant | Có | — |
| Chi phí | Cao hơn (acquire/release, có thể park thread) | Thấp (chỉ hàng rào bộ nhớ) |
| Dùng cho | Bộ đếm, số dư, mọi chuỗi thao tác cần nguyên vẹn | Cờ hiệu, công bố object bất biến, biến quan sát đơn lẻ |

> **Quy tắc nhanh:** chỉ **gán một giá trị** mà nhiều thread cần thấy ngay → `volatile`. **Đọc rồi tính rồi ghi lại**, hoặc bất biến trên nhiều biến → `synchronized` (hoặc công cụ Module 05.2).

---

## 9. `wait` / `notify` / `notifyAll` — guarded block

Khi một thread phải **chờ một điều kiện** do thread khác tạo ra (hàng đợi có phần tử, kết nối sẵn sàng...), *bận-đợi* (`while(!cond){}`) đốt CPU. `wait()`/`notify()` cho thread ngủ và được đánh thức đúng lúc.

### Mẫu chuẩn — hộp thư một phần tử (producer/consumer)

```java
public class MessageBox {
    private final Object lock = new Object();
    private String message;
    private boolean hasMessage = false;

    public void put(String m) throws InterruptedException {
        synchronized (lock) {
            while (hasMessage) lock.wait();      // WHILE — không phải if
            message = m;
            hasMessage = true;
            lock.notifyAll();                     // đánh thức bên đang chờ take()
        }
    }

    public String take() throws InterruptedException {
        synchronized (lock) {
            while (!hasMessage) lock.wait();
            hasMessage = false;
            lock.notifyAll();
            return message;
        }
    }
}
```

### Năm quy tắc bắt buộc

1. **Phải đang giữ monitor của object** khi gọi `obj.wait()`/`obj.notify()` — nếu không: `IllegalMonitorStateException`.
2. `wait()` **nhả lock** trong lúc chờ, **giành lại** khi được đánh thức (khác `sleep()` — giữ nguyên lock).
3. **Luôn `wait()` trong vòng `while`** kiểm tra lại điều kiện — vì (a) *spurious wakeup* (JVM được phép đánh thức không lý do), (b) giữa lúc `notify` và lúc thread này giành lại lock, một thread thứ ba có thể đã làm điều kiện sai trở lại.
4. **Ưu tiên `notifyAll()` hơn `notify()`** — `notify()` đánh thức **một** thread ngẫu nhiên; nếu trúng thread không xử lý được điều kiện thì nó ngủ lại, thread cần thức thì không được gọi → "kẹt tín hiệu".
5. Điều kiện chờ phải là **biến chia sẻ** được bảo vệ bởi **cùng** monitor dùng cho `wait`/`notify`.

> Thực tế backend hiếm khi viết `wait`/`notify` tay — dùng `BlockingQueue`, `CountDownLatch`, `CompletableFuture` (Module 05.2). Nhưng hiểu cơ chế này là nền tảng để hiểu chúng.

---

## 10. Deadlock & các lỗi liveness

**Deadlock:** hai (hoặc nhiều) thread chờ nhau vô hạn — mỗi thread giữ một lock mà thread kia cần.

```java
public void method1() {
    synchronized (lockA) {
        synchronized (lockB) { /* ... */ }   // Thread 1: A rồi B
    }
}
public void method2() {
    synchronized (lockB) {
        synchronized (lockA) { /* ... */ }   // Thread 2: B rồi A  ← thứ tự NGƯỢC
    }
}
```

```
Thread 1 ──giữ──► lockA ──cần──► lockB ──bị giữ bởi──► Thread 2 ──cần──► lockA ──vòng lại Thread 1
```

### Bốn điều kiện Coffman — phá **bất kỳ một** là hết deadlock

| Điều kiện | Nội dung | Cách phá trong thực tế |
|---|---|---|
| **Mutual exclusion** | Tài nguyên không chia sẻ được | Dùng cấu trúc bất biến / lock-free (`Atomic*`) khi có thể |
| **Hold and wait** | Giữ lock này, chờ lock khác | Giành **tất cả** lock cần cùng lúc, hoặc không giành gì |
| **No preemption** | Không cưỡng đoạt được lock | `tryLock(timeout)` — bỏ cuộc và nhả hết nếu không lấy đủ (Module 05.2) |
| **Circular wait** | Vòng chờ khép kín | **Giành lock theo một thứ tự toàn cục cố định** ← phổ biến nhất |

### Lock ordering — sửa ví dụ chuyển tiền

```java
void transfer(Account from, Account to, long amount) {
    Account first  = from.id() < to.id() ? from : to;    // luôn khóa id nhỏ trước
    Account second = from.id() < to.id() ? to : from;
    synchronized (first) {
        synchronized (second) {
            from.withdraw(amount);
            to.deposit(amount);
        }
    }
    // id bằng nhau (rất hiếm) → thêm một "tie-breaker lock" tĩnh dùng chung, giành nó ngoài cùng
}
```

### Các nguyên tắc phòng khác

1. Giữ lock **ngắn nhất có thể**; không làm I/O / gọi alien method khi đang giữ lock.
2. Tránh giữ **nhiều** lock cùng lúc nếu tái cấu trúc được.
3. `Lock.tryLock(200, MILLISECONDS)` — có đường thoát thay vì chờ vô hạn (Module 05.2).

### Lỗi liveness khác (không phải deadlock nhưng cũng "không tiến lên")

| Lỗi | Mô tả |
|---|---|
| **Livelock** | Các thread liên tục phản ứng lẫn nhau, đổi trạng thái nhưng không ai hoàn thành (hai người né nhau ngoài hành lang mãi) |
| **Starvation** | Một thread không bao giờ được cấp CPU/lock vì các thread khác (ưu tiên cao hơn, hoặc lock không công bằng) luôn chiếm trước |
| **Contention** | Quá nhiều thread tranh một lock → phần lớn thời gian nằm ở BLOCKED, thông lượng sụp |

### Phát hiện deadlock

- **Thread dump:** `jstack <pid>`, `jcmd <pid> Thread.print`, hoặc gửi `SIGQUIT` (`kill -3 <pid>`) → JVM in `Found one Java-level deadlock:` kèm thread nào chờ lock nào.
- Trong code: `ManagementFactory.getThreadMXBean().findDeadlockedThreads()`.

---

## 11. Các method điều khiển Thread

```java
Thread.sleep(1000);            // static — ngủ thread HIỆN TẠI 1000ms; KHÔNG nhả lock đang giữ
t.join();                       // thread gọi join() CHỜ t chạy xong (TERMINATED); join(ms) có thời hạn
t.interrupt();                  // đặt CỜ interrupt của t — yêu cầu hủy hợp tác, không ép dừng
Thread.currentThread();         // reference tới thread đang chạy đoạn này
t.setName("worker-1");          // đặt tên — hiện trong log/thread dump
t.setDaemon(true);              // trước start() — JVM không chờ daemon khi thoát
t.setPriority(Thread.MAX_PRIORITY);  // gợi ý 1..10 cho scheduler — phụ thuộc OS, thường bỏ qua, đừng dựa vào
```

### `interrupt` — cơ chế hủy hợp tác

```java
Thread.currentThread().isInterrupted();   // ĐỌC cờ, KHÔNG xóa
Thread.interrupted();                      // đọc cờ của thread hiện tại RỒI XÓA (static)
```

- Method blocking (`sleep`, `wait`, `join`, `BlockingQueue.take`...) khi thread bị interrupt → **ném `InterruptedException` và xóa cờ**. Muốn giữ tín hiệu, `catch` xong gọi lại `Thread.currentThread().interrupt()` (Module 04).
- Vòng lặp dài không blocking → tự kiểm tra `while (!Thread.currentThread().isInterrupted()) { ... }`.
- `interrupt()` **không** làm gì nếu thread không hợp tác kiểm tra — nó chỉ là *tín hiệu*.

### `sleep` vs `wait`

| | `Thread.sleep(t)` | `obj.wait()` |
|---|---|---|
| Nhả lock đang giữ | **Không** | **Có** (nhả monitor của `obj`) |
| Gọi ở đâu | Bất kỳ đâu | Chỉ khi đang giữ monitor của `obj` |
| Thức dậy do | Hết thời gian / interrupt | `notify`/`notifyAll` / interrupt / spurious / hết `wait(t)` |

### Đã bị loại bỏ — không bao giờ dùng

`Thread.stop()` (nhả **mọi** lock đột ngột → object hỏng bất biến), `suspend()`/`resume()` (dừng thread khi đang giữ lock → deadlock). Đều `@Deprecated` từ lâu.

> `ThreadLocal<T>` — biến "một bản cho mỗi thread" (`SimpleDateFormat` per-thread, ngữ cảnh request). ⚠️ Với **thread pool** (thread sống lâu, tái dùng), phải `remove()` sau mỗi tác vụ, nếu không → rò rỉ bộ nhớ và dữ liệu request này lẫn sang request khác. Chi tiết ở Module 05.2.

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Process vs Thread | Thread chung Heap/Metaspace/static, riêng Stack/PC. Nguồn gốc mọi vấn đề concurrency. |
| `Runnable` vs `extends Thread` | Ưu tiên `Runnable` — giữ khả năng kế thừa, hợp thread pool. `Callable` khi cần trả giá trị. |
| `start()` vs `run()` | `start()` tạo luồng OS mới; `run()` trực tiếp = method thường. `start()` 2 lần → `IllegalThreadStateException`. |
| Exception trong thread | Không lan về thread cha; thread đó chết. `setUncaughtExceptionHandler`. |
| Daemon | `setDaemon(true)` trước `start()`; JVM không chờ; `finally` có thể không chạy. |
| Lifecycle | 6 trạng thái. `RUNNABLE` gộp cả "chờ I/O OS". `TERMINATED` không quay lại. |
| Race condition | 3 mối nguy tách biệt: **atomicity** (lost update), **visibility**, **reordering**. `long`/`double` gán không nguyên tử nếu thiếu `volatile`. |
| Happens-before | Không có hb → không bảo đảm nhìn thấy. Nguồn hb: program order, monitor unlock→lock, volatile write→read, `start()`, `join()`, `final` field. |
| `synchronized` | Loại trừ lẫn nhau **+ visibility + reentrant**. Khóa `private final Object`, không khóa `this`/String/wrapper/field đổi. Static → khóa `.class`. Không timeout/interrupt/fairness. |
| `volatile` | Visibility + chặn reordering, **không** atomicity. Cho cờ / công bố object bất biến / biến quan sát đơn lẻ. `count++` vẫn race. |
| `final` field | Công bố an toàn không cần đồng bộ (nếu `this` không escape trong constructor). |
| `wait`/`notify` | Gọi khi giữ monitor; `wait()` **nhả lock**; luôn `wait()` trong `while`; ưu tiên `notifyAll()`. |
| Deadlock | 4 điều kiện Coffman — phá 1 là hết. Thực tế: **lock ordering** theo id/hash cố định; `tryLock` timeout. Phát hiện bằng thread dump (`jstack`). |
| Liveness khác | Livelock, starvation, contention. |
| `interrupt` | Tín hiệu hủy **hợp tác**. Blocking method ném `InterruptedException` + xóa cờ. `Thread.interrupted()` xóa, `isInterrupted()` không. |
| `sleep` vs `wait` | `sleep` giữ lock; `wait` nhả lock. |
| Cấm | `Thread.stop()`/`suspend()`/`resume()`. `ThreadLocal` phải `remove()` khi dùng với pool. |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Có tạo thread mới không? In ra trên thread nào?
```java
Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName()));
t.run();
```

**Câu 2.** 1000 thread cùng gọi `increment()`, kết quả có luôn là `1000`? Nêu **cả ba** mối nguy có thể xảy ra.
```java
private static int count = 0;
public static void increment() { count++; }
```

**Câu 3.** Thêm `volatile` vào `count` ở Câu 2 có làm kết quả luôn đúng `1000` không? Vì sao? Hai cách sửa đúng là gì?

**Câu 4.** Thread B có chắc chắn thoát vòng lặp khi Thread A gọi `stopWork()`? Nếu không, sửa và giải thích bằng happens-before.
```java
class Worker {
    private boolean stop = false;
    void doWork()   { while (!stop) { } }
    void stopWork() { stop = true; }
}
```

**Câu 5.** Hai lời gọi `transfer` đồng thời dưới đây có nguy cơ gì? Chỉ ra điều kiện Coffman bị vi phạm và cách sửa.
```java
void transfer(Account from, Account to, long amt) {
    synchronized (from) { synchronized (to) { from.debit(amt); to.credit(amt); } }
}
// Thread 1: transfer(A, B, 100);   Thread 2: transfer(B, A, 50);
```

**Câu 6.** Đoạn `wait()` sau sai ở hai chỗ — chỉ ra và sửa.
```java
if (queue.isEmpty()) {
    queue.wait();
}
return queue.remove();
```

**Câu 7.** `synchronized ("LOCK")` và `synchronized (Integer.valueOf(1))` sai ở đâu? Nên khóa trên gì?

**Câu 8.** `sleep(1000)` và `wait(1000)` khác nhau thế nào về việc giữ lock? Gọi `wait()` ngoài khối `synchronized` thì sao?

---

### Phần B — Bài tập viết code

**Bài 1 — Tái hiện rồi sửa race condition.**
`BankAccount` với `long balanceCents`. `deposit(long)` **không** đồng bộ. 100 thread mỗi thread `deposit(1000)`, `join` hết, in `balance` (kỳ vọng 100_000, thực tế thường thiếu). Sửa bằng `synchronized`, chạy 5 lần liên tiếp chứng minh luôn đúng. In thêm `getCount()` sai lệch trung bình qua 5 lần.

**Bài 2 — synchronized method vs block.**
`RequestLogger` có `long total` (chung, cần đồng bộ) và `String node` (bất biến). `log(String msg)` chỉ `synchronized` đúng `total++`, phần format/in dùng `node` để ngoài. Comment giải thích vì sao khóa hẹp hơn = ít contention hơn. Đo throughput 8 thread × 100_000 lần với hai cách (khóa cả method vs khóa hẹp).

**Bài 3 — volatile & dừng thread an toàn.**
`Worker implements Runnable` có `volatile boolean running = true`, vòng `while(running)` tăng bộ đếm và `sleep(50)`. `main`: chạy Worker trên thread riêng, sau 1s gọi `stop()`, `join()`, in "đã dừng, đếm được N". Thử bỏ `volatile` — ghi nhận kết quả và giải thích bằng lý thuyết vì sao **không thể dựa vào việc "máy mình vẫn chạy đúng"**.

**Bài 4 — Deadlock: tái hiện & sửa bằng lock ordering.**
Cài đúng ví dụ deadlock (2 method giành lock ngược thứ tự). Chạy, xác nhận treo (dùng `jstack <pid>` hoặc `jcmd` để chụp thread dump, dán đoạn `Found one Java-level deadlock`). Sửa bằng lock ordering theo `System.identityHashCode`, chạy lại chứng minh luôn kết thúc.

**Bài 5 — wait/notify: hàng đợi chặn 1 phần tử.**
Tự viết `SynchronousBox<T>` với `put(T)` / `take()` dùng `wait`/`notifyAll` (không dùng `java.util.concurrent`). 1 producer đẩy 20 số, 2 consumer lấy ra; in thứ tự lấy được. Đảm bảo: `wait()` trong `while`, không mất phần tử, không deadlock khi cả 2 consumer cùng chờ.

**Bài 6 — Bài toán tổng hợp: phòng vé (chuẩn bị capstone Flash-Sale).**
`TicketBooth` với `int available = 100`. `boolean book()`: còn vé thì giảm 1 trả `true`, hết thì `false`. 200 thread cùng `book()`, đếm số `true` bằng biến chung (cũng phải đồng bộ đúng!). Chứng minh: **không** đồng bộ `book()` → số vé bán ra có thể > 100 (oversold, do check-then-act ở mục 4.5). Sửa bằng `synchronized`, chứng minh **không bao giờ** vượt 100. Ghi chú: đây là lời giải Java thuần một-instance; backend nhiều instance cần distributed lock / `SELECT ... FOR UPDATE` / optimistic lock `@Version` (Module 14, 18).

---

### Phần C — Nâng cao

**Câu 1.** Phân biệt rạch ròi **atomicity**, **visibility**, **ordering**. Với `count++` thiếu đồng bộ, mối nguy nào xảy ra? Với `while(!stopFlag){}` thiếu `volatile`, mối nguy nào? `volatile` chữa được (những) mối nguy nào, `synchronized` chữa được (những) mối nguy nào?

**Câu 2.** Phát biểu quan hệ **happens-before** cho: (a) `synchronized`, (b) `volatile`, (c) `Thread.start()`, (d) `Thread.join()`. Dùng chúng giải thích vì sao đoạn "piggyback" (`data=42; ready=true;` ở thread A — `if(ready) use(data);` ở thread B) đảm bảo B thấy `data == 42` **dù `data` không `volatile`**.

**Câu 3.** `synchronized` cung cấp **cả** visibility lẫn atomicity, còn `volatile` chỉ visibility. Vậy tại sao vẫn cần `volatile`? Nêu ba trường hợp `volatile` là lựa chọn đúng mà `synchronized` là thừa/sai, kèm lý do hiệu năng và ngữ nghĩa.

**Câu 4.** `Thread.State.RUNNABLE` gộp ba tình huống khác nhau. Liệt kê. Vì sao một thread đang `socket.read()` chờ dữ liệu mạng lại hiện `RUNNABLE` chứ không phải `WAITING`? Điều này gây hiểu nhầm gì khi đọc thread dump, và nên nhìn thêm dấu hiệu nào?

**Câu 5.** Bốn điều kiện Coffman. Với hệ thống chuyển tiền giữa các tài khoản, phân tích cách phá từng điều kiện và đánh giá tính khả thi/chi phí của mỗi cách. Vì sao "lock ordering" (phá circular wait) thường là lựa chọn thực tế nhất? Khi hai tài khoản có `id` bằng nhau thì lock ordering hỏng thế nào và cách vá?

**Câu 6.** Vì sao `wait()` **bắt buộc** trong vòng `while` chứ không phải `if`? Nêu **hai** lý do độc lập (một về JVM, một về logic đa consumer). Vì sao `notifyAll()` an toàn hơn `notify()` — cho một kịch bản `notify()` gây kẹt vĩnh viễn với hai loại thread chờ trên cùng một lock.

**Câu 7.** `interrupt()` là "hủy hợp tác". Giải thích: (a) điều gì xảy ra với cờ interrupt khi `Thread.sleep()` bị interrupt, (b) khác nhau giữa `Thread.interrupted()` và `isInterrupted()`, (c) vì sao "nuốt" `InterruptedException` (chỉ `catch` rồi bỏ qua) là bug, (d) vì sao `Thread.stop()` bị loại bỏ dù "dừng ngay" nghe tiện hơn.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Không** tạo thread mới. `t.run()` gọi thẳng `run()` như method thường → in ra tên thread **hiện tại** (thường `main`). Cần `t.start()` để JVM tạo luồng OS và in ra `Thread-0`.
2. **Không** đảm bảo `1000`. Ba mối nguy: (a) **atomicity** — `count++` là đọc-tăng-ghi, hai thread lost update; (b) **visibility** — thread này không thấy giá trị mới nhất thread kia ghi; (c) **reordering** — ít rõ với một biến nhưng vẫn thuộc phạm trù JMM. Kết quả thường < 1000, đổi mỗi lần chạy.
3. **Không.** `volatile` cho visibility nhưng `count++` vẫn là ba bước không nguyên tử → vẫn lost update. Sửa: (i) `synchronized` quanh `count++`; (ii) `AtomicInteger.incrementAndGet()` (Module 05.2).
4. **Không chắc chắn.** Thiếu `volatile` nên không có quan hệ happens-before giữa `stop = true` (Thread A) và lần đọc `stop` trong vòng lặp (Thread B) → JIT được phép nâng `stop` ra khỏi vòng, B lặp vô hạn. Sửa: `private volatile boolean stop = false;` — ghi `volatile` hb đọc `volatile` sau đó → B thấy `true`.
5. Nguy cơ **deadlock**: Thread 1 khóa `A` rồi chờ `B`; Thread 2 khóa `B` rồi chờ `A` → **circular wait** (điều kiện Coffman thứ 4). Sửa: giành lock theo thứ tự cố định không phụ thuộc tham số — `Account first = a.id() < b.id() ? a : b; ...` rồi `synchronized(first){ synchronized(second){...} }`.
6. Hai lỗi: (a) dùng `if` thay vì `while` — spurious wakeup / consumer khác đã lấy mất phần tử giữa lúc `notify` và lúc giành lại lock → `queue.remove()` nổ trên hàng đợi rỗng; (b) toàn bộ phải nằm **trong `synchronized (queue)`** — gọi `wait()`/`remove()` ngoài monitor → `IllegalMonitorStateException` / race. Sửa: `synchronized (queue) { while (queue.isEmpty()) queue.wait(); return queue.remove(); }`.
7. `"LOCK"` là String literal → bị intern → **mọi** đoạn code trong JVM dùng cùng literal khóa lên **cùng** object → contention/deadlock ngoài ý muốn. `Integer.valueOf(1)` nằm trong cache wrapper (-128..127) → tương tự bị chia sẻ. Nên khóa trên `private final Object lock = new Object();` — riêng tư, không đổi, không ai ngoài class giữ tham chiếu.
8. `sleep(1000)` **không nhả** lock đang giữ — thread khác vẫn bị chặn khỏi critical section suốt 1s. `wait(1000)` **nhả** monitor của object trong lúc chờ, giành lại khi thức. Gọi `wait()` ngoài `synchronized` của đúng object → `IllegalMonitorStateException` ngay lập tức.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `deposit` không đồng bộ: `balance` cuối thường 95_000–99_000/100_000. `synchronized void deposit(long c){ balanceCents += c; }` → luôn 100_000. `getCount()` (số lần chênh) trung bình vài trăm–vài nghìn tùy CPU.
- **Bài 2:** Khóa hẹp: `synchronized (counterLock) { total++; }`; phần `String line = "[" + node + "] " + msg; out.println(line);` để ngoài. Khóa hẹp giảm thời gian giữ lock ⇒ ít BLOCKED ⇒ throughput cao hơn (chênh rõ khi phần "ngoài" nặng).
- **Bài 3:** Có `volatile`: dừng trong ~1 vòng lặp sau `stop()`. Bỏ `volatile`: **có thể** vẫn dừng trên máy/JDK của bạn (do JIT/kiến trúc cụ thể) — đó chính là cái bẫy: "quan sát thấy đúng" ≠ "được JMM đảm bảo đúng". Luôn dùng `volatile`/`synchronized` theo lý thuyết.
- **Bài 4:** Thread dump sẽ có `Found one Java-level deadlock:` + `"Thread-0": waiting to lock <0x...> (a java.lang.Object), which is held by "Thread-1"` và ngược lại. Sau lock ordering theo `identityHashCode` (kèm tie-breaker khi hash bằng nhau): chương trình luôn in đủ và kết thúc.
- **Bài 5:** `put`: `synchronized(lock){ while(full) lock.wait(); item=v; full=true; lock.notifyAll(); }`. `take`: `synchronized(lock){ while(!full) lock.wait(); full=false; lock.notifyAll(); return item; }`. Hai consumer cùng chờ → `notifyAll` đánh thức cả hai, một lấy được, một thấy `!full` lại `wait` — không mất phần tử, không kẹt.
- **Bài 6:** Không đồng bộ: hai thread cùng qua `if (available > 0)` khi `available == 1` → cả hai `available--` → `available == -1`, số `true` đếm được > 100 (oversold). `synchronized boolean book(){ if(available > 0){ available--; return true; } return false; }` + biến đếm `true` cũng `synchronized`/`AtomicInteger` → tổng `true` luôn = 100.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. **Atomicity** — một chuỗi thao tác hoặc xảy ra trọn vẹn hoặc không, không bị thread khác chen giữa. **Visibility** — ghi của thread này được thread khác nhìn thấy. **Ordering** — thứ tự các lệnh quan sát từ thread khác đúng như mã nguồn. `count++` thiếu đồng bộ → chủ yếu hỏng **atomicity** (và visibility). `while(!stopFlag){}` thiếu `volatile` → hỏng **visibility** (và cho phép hoisting = ordering). `volatile` chữa visibility + ordering, **không** atomicity. `synchronized` chữa **cả ba** (trong phạm vi critical section).
2. (a) `unlock` monitor M hb mọi `lock` M sau đó. (b) ghi biến `volatile` v hb mọi đọc v sau đó. (c) `t.start()` hb mọi hành động trong `t`. (d) mọi hành động trong `t` hb `t.join()` trả về. Piggyback: trong thread A, `data=42` hb `ready=true` (program order); `ready=true` hb `đọc ready==true` ở B (quy tắc volatile); `đọc ready` hb `use(data)` ở B (program order). Bắc cầu ⇒ `data=42` hb `use(data)` ⇒ B **phải** thấy `42`, dù `data` không volatile.
3. `synchronized` có chi phí acquire/release, có thể park/unpark thread (chuyển ngữ cảnh), tạo điểm contention, và có thể deadlock. `volatile` chỉ là hàng rào bộ nhớ, không block. Ba trường hợp `volatile` đúng: (i) **cờ dừng** `running` — chỉ cần thấy giá trị mới, không có bất biến phức hợp; `synchronized` ở đây thêm blocking vô ích. (ii) **công bố object bất biến** vừa tạo (`config = new ImmutableConfig(...)`) — reader hoặc thấy `null` hoặc thấy config hoàn chỉnh. (iii) **biến quan sát độc lập** như `lastHeartbeatMillis` — mỗi ghi độc lập, đọc không cần phối hợp với biến khác.
4. `RUNNABLE` = đang chạy trên CPU **hoặc** sẵn sàng chờ scheduler cấp CPU **hoặc** đang chờ trong một syscall blocking của OS (đọc file/socket). JVM để `socket.read()` là `RUNNABLE` vì trạng thái `WAITING`/`BLOCKED` của Java chỉ dành cho cơ chế đồng bộ **của JVM** (`wait`, `synchronized`), còn chờ I/O của kernel thì JVM không mô hình hóa. Hiểu nhầm khi đọc thread dump: thấy nhiều thread `RUNNABLE` tưởng CPU quá tải, thực ra chúng đang nằm chờ mạng. Nhìn thêm: dòng stack trên cùng (`socketRead0`, `epollWait`...) và mức sử dụng CPU thực tế.
5. **Mutual exclusion**: thay lock bằng thao tác lock-free trên số dư (`AtomicLong` + CAS) — khả thi cho debit/credit đơn giản, khó khi cần cập nhật hai tài khoản như một đơn vị. **Hold-and-wait**: `tryLock` cả hai, thất bại thì nhả hết và thử lại — khả thi, nhưng có livelock nếu không backoff ngẫu nhiên. **No preemption**: `tryLock(timeout)` — đơn giản, chi phí là phải xử lý đường thất bại. **Circular wait**: lock ordering — rẻ nhất, chỉ cần một khóa sắp xếp ổn định (`id`), không đổi cấu trúc code nhiều → thực tế nhất. `id` bằng nhau (self-transfer, hoặc trùng hash nếu dùng `identityHashCode`): hai nhánh `first/second` trỏ cùng object hoặc thứ tự không xác định → thêm một `static final Object TIE_BREAKER` giành ngoài cùng khi khóa sắp xếp bằng nhau.
6. (i) **Spurious wakeup**: JLS cho phép `wait()` trả về mà không có `notify` nào — `if` sẽ chạy tiếp với điều kiện chưa thỏa. (ii) **Đa consumer**: thread A `notifyAll`, hai consumer B, C thức; B giành lock trước, lấy phần tử, nhả lock; C giành lock — nếu dùng `if`, C tưởng vẫn có phần tử và `remove()` trên hàng rỗng. `while` bắt C kiểm tra lại → thấy rỗng → `wait()` tiếp. `notify()` kẹt vĩnh viễn: lock có cả thread "chờ đầy để put" và thread "chờ rỗng để take"; sau một `take()`, `notify()` có thể đánh thức nhầm một thread `take()` khác (điều kiện của nó vẫn sai → ngủ lại), trong khi thread `put()` đang chờ chỗ trống không được báo → kẹt. `notifyAll()` đánh thức tất cả, đúng thread sẽ tiến lên.
7. (a) `sleep()` bị interrupt → ném `InterruptedException` **và xóa cờ interrupt** (cờ trở về `false`). (b) `Thread.interrupted()` là static, đọc cờ của thread hiện tại **rồi xóa**; `isInterrupted()` là instance, đọc cờ **không xóa**. (c) Nuốt `InterruptedException` làm mất tín hiệu hủy — vòng lặp/tác vụ bao ngoài không biết cần dừng, tiếp tục chạy; tối thiểu phải `Thread.currentThread().interrupt()` để khôi phục cờ, hoặc thoát tác vụ. (d) `Thread.stop()` ném `ThreadDeath` tại **điểm bất kỳ**, kể cả giữa một cập nhật hai biến → nhả **mọi** lock đang giữ trong khi object đang ở trạng thái nửa vời, không có bất biến → phần còn lại của chương trình thấy dữ liệu hỏng mà không có dấu hiệu. Không có cách nào an toàn → bỏ.

</details>

---

*File tiếp theo trong lộ trình: **Module 05.2 — Concurrency Utilities** (ExecutorService, ThreadPoolExecutor, Future/CompletableFuture, CountDownLatch, Semaphore, Atomic, Lock).*
