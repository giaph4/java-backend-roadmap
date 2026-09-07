# Module 05.1 — Thread cơ bản

> **Mức độ ưu tiên: Cao** — Backend xử lý hàng nghìn request đồng thời (concurrent), mỗi request thường chạy trên 1 thread riêng. Không hiểu race condition/deadlock sẽ dẫn đến bug **cực kỳ khó tái hiện** (chỉ xảy ra ngẫu nhiên, lúc có lúc không, khó debug bằng breakpoint thông thường) — đây cũng là loại câu hỏi "khó" kinh điển để phân biệt ứng viên Junior và Middle/Senior trong phỏng vấn.

---

## Mục lục

1. [Process vs Thread](#1-process-vs-thread)
2. [Tạo Thread — `Thread` vs `Runnable`](#2-tạo-thread--thread-vs-runnable)
3. [Thread Lifecycle](#3-thread-lifecycle)
4. [Race Condition](#4-race-condition)
5. [`synchronized` — giải quyết Race Condition](#5-synchronized--giải-quyết-race-condition)
6. [`volatile` — đảm bảo tính hiển thị (Visibility)](#6-volatile--đảm-bảo-tính-hiển-thị-visibility)
7. [`synchronized` vs `volatile` — khi nào dùng cái nào](#7-synchronized-vs-volatile--khi-nào-dùng-cái-nào)
8. [Deadlock](#8-deadlock)
9. [Các phương thức điều khiển Thread quan trọng](#9-các-phương-thức-điều-khiển-thread-quan-trọng)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Process vs Thread

| Tiêu chí | Process (Tiến trình) | Thread (Luồng) |
|---|---|---|
| Định nghĩa | 1 chương trình đang chạy, có vùng nhớ **độc lập riêng** | 1 đơn vị thực thi **bên trong** 1 process, chia sẻ vùng nhớ với các thread khác cùng process |
| Bộ nhớ | Mỗi Process có Heap, không gian địa chỉ riêng | Các Thread trong CÙNG Process **chia sẻ chung Heap**, nhưng mỗi Thread có **Stack riêng** |
| Giao tiếp | Phức tạp hơn (IPC — Inter-Process Communication) | Đơn giản hơn (chia sẻ trực tiếp biến trong Heap) — nhưng chính vì vậy dễ gây race condition |
| Chi phí tạo mới | Tốn kém (JVM phải cấp phát vùng nhớ độc lập hoàn toàn) | Nhẹ hơn nhiều |
| Ví dụ | Mỗi ứng dụng Java bạn chạy (`java -jar app.jar`) là 1 Process | Trong 1 ứng dụng Spring Boot, mỗi request HTTP thường được xử lý trên 1 Thread riêng (lấy từ Thread Pool) |

```
Process (JVM instance)
 ├── Heap (dùng chung cho MỌI thread trong process này)
 ├── Method Area / Metaspace (dùng chung)
 ├── Thread 1 → Stack riêng, Program Counter riêng
 ├── Thread 2 → Stack riêng, Program Counter riêng
 └── Thread 3 → Stack riêng, Program Counter riêng
```

> **Đây chính là lý do Race Condition tồn tại:** vì các Thread **chia sẻ chung Heap**, nếu nhiều Thread cùng đọc/ghi 1 biến trên Heap **cùng lúc mà không kiểm soát**, dữ liệu có thể bị sai lệch — chi tiết ở mục 4.

---

## 2. Tạo Thread — `Thread` vs `Runnable`

### Cách 1: kế thừa (extends) `Thread`

```java
public class MyThread extends Thread {
    @Override
    public void run() { // override method run() — chứa logic sẽ chạy trên thread mới
        System.out.println("Đang chạy trên thread: " + Thread.currentThread().getName());
    }
}
```
```java
MyThread t = new MyThread();
t.start(); // khởi động thread MỚI — JVM gọi run() trên 1 luồng thực thi riêng
```

### Cách 2: implements `Runnable` (khuyến nghị hơn — áp dụng nguyên lý đã học ở Module 02.2)

```java
public class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Đang chạy trên thread: " + Thread.currentThread().getName());
    }
}
```
```java
Thread t = new Thread(new MyTask());
t.start();

// Hoặc dùng Lambda (Module 03.3) — vì Runnable là 1 Functional Interface
Thread t2 = new Thread(() -> System.out.println("Chạy bằng Lambda"));
t2.start();
```

### Vì sao nên ưu tiên `Runnable` hơn `extends Thread`?

1. **Java không hỗ trợ đa kế thừa class** (Module 02.1) — nếu class đã `extends Thread`, nó **không thể `extends` class nào khác nữa**. Dùng `Runnable` giữ được sự linh hoạt để `extends` class khác nếu cần.
2. **Tách biệt rõ ràng "task cần chạy" (Runnable) và "cơ chế chạy nó" (Thread)** — đúng theo tinh thần Single Responsibility Principle (Module 02.3).
3. `Runnable` dễ dàng tái sử dụng với **Thread Pool** (`ExecutorService` — sẽ học ở Module 05.2), là cách quản lý thread **chuẩn trong backend thực tế**, thay vì tự tạo `Thread` thủ công.

> ⚠️ **Bẫy hay gặp:** gọi `run()` **trực tiếp** (thay vì `start()`) sẽ **KHÔNG** tạo thread mới — nó chỉ chạy như 1 method bình thường **trên chính thread hiện tại**:
> ```java
> MyThread t = new MyThread();
> t.run();   // ❌ SAI — chạy tuần tự trên thread hiện tại, KHÔNG tạo thread mới
> t.start(); // ✅ ĐÚNG — tạo thread mới thực sự, run() được gọi TRÊN thread đó
> ```

---

## 3. Thread Lifecycle

```
        new Thread()
             │
             ▼
        ┌─────────┐
        │   NEW    │  ← vừa tạo, chưa gọi start()
        └────┬─────┘
             │ start()
             ▼
        ┌─────────┐
   ┌───►│ RUNNABLE │◄──────────────┐  ← sẵn sàng chạy hoặc đang chạy (JVM/OS quyết định thời điểm thực thi cụ thể)
   │    └────┬─────┘                │
   │         │                      │ notify()/notifyAll()
   │         │ synchronized bị      │ hoặc hết thời gian sleep/wait
   │         │ block bởi thread khác│
   │         ▼                      │
   │    ┌─────────┐            ┌────┴─────┐
   │    │ BLOCKED  │            │  WAITING /│
   │    └─────────┘            │TIMED_WAITING│
   │                            └──────────┘
   │                                 ▲
   │         run() kết thúc          │ wait()/sleep()/join()
   └─────────────┬───────────────────┘
                 ▼
           ┌───────────┐
           │TERMINATED  │  ← run() đã chạy xong, thread kết thúc vĩnh viễn
           └───────────┘
```

| Trạng thái | Ý nghĩa |
|---|---|
| `NEW` | Object `Thread` đã tạo, nhưng `start()` chưa được gọi |
| `RUNNABLE` | Đang chạy, hoặc đang chờ CPU cấp phát thời gian xử lý (JVM/OS lập lịch — scheduling) |
| `BLOCKED` | Đang chờ để giành được **lock** (`synchronized`) đang bị thread khác giữ |
| `WAITING` / `TIMED_WAITING` | Đang chủ động chờ (`wait()`, `join()`, hoặc `sleep()` — có thời hạn) |
| `TERMINATED` | `run()` đã thực thi xong, thread kết thúc — **không thể `start()` lại lần 2** |

> ⚠️ **Lưu ý:** gọi `start()` **2 lần** trên cùng 1 object `Thread` sẽ ném `IllegalThreadStateException` — mỗi object `Thread` chỉ chạy được **đúng 1 lần** trong vòng đời của nó.

---

## 4. Race Condition

**Race Condition** xảy ra khi **nhiều thread cùng truy cập và thay đổi 1 dữ liệu dùng chung**, và **kết quả cuối cùng phụ thuộc vào thứ tự thực thi ngẫu nhiên** của các thread — dẫn đến kết quả **không nhất quán, khó dự đoán**.

### Ví dụ kinh điển: bộ đếm dùng chung (Shared Counter)

```java
public class Counter {
    private int count = 0;

    public void increment() {
        count++; // trông có vẻ là 1 thao tác ĐƠN GIẢN, nhưng THỰC RA gồm 3 bước riêng biệt!
    }

    public int getCount() { return count; }
}
```

**Vấn đề cốt lõi:** `count++` **KHÔNG PHẢI** là 1 thao tác nguyên tử (atomic) — nó thực chất là **3 bước** ở tầng bytecode/CPU:
```
1. ĐỌC giá trị hiện tại của count từ bộ nhớ (READ)
2. TĂNG giá trị đó lên 1 (INCREMENT)
3. GHI giá trị mới trở lại bộ nhớ (WRITE)
```

### Minh họa cụ thể Race Condition xảy ra như thế nào

```
Thời điểm    Thread A                  Thread B                  Giá trị count trong bộ nhớ
────────────────────────────────────────────────────────────────────────────────────
t1           ĐỌC count = 0                                       0
t2                                     ĐỌC count = 0              0   ← B đọc TRƯỚC KHI A kịp ghi!
t3           TĂNG lên 1 (biến local)                              0
t4                                     TĂNG lên 1 (biến local)     0
t5           GHI count = 1                                        1
t6                                     GHI count = 1               1   ← B GHI ĐÈ lên kết quả của A!

KẾT QUẢ SAI: count = 1, dù CẢ 2 thread đều đã gọi increment() — LẼ RA phải là 2!
```

```java
Counter counter = new Counter();
List<Thread> threads = new ArrayList<>();

for (int i = 0; i < 1000; i++) {
    Thread t = new Thread(counter::increment);
    threads.add(t);
    t.start();
}
for (Thread t : threads) {
    t.join(); // chờ tất cả thread hoàn thành (xem mục 9)
}

System.out.println(counter.getCount());
// KẾT QUẢ MONG ĐỢI: 1000
// KẾT QUẢ THỰC TẾ (không đồng bộ hóa): thường là 1 con số NHỎ HƠN 1000, và THAY ĐỔI mỗi lần chạy!
```

> **Đây chính là bug nổi tiếng khó chịu nhất trong lập trình đa luồng:** code compile hoàn toàn bình thường, chạy thử vài lần có khi vẫn ra đúng 1000 (do may mắn về thời điểm), nhưng chạy dưới tải cao (nhiều thread, nhiều request đồng thời trong production) thì bắt đầu ra sai — cực kỳ khó tái hiện lại để debug.

---

## 5. `synchronized` — giải quyết Race Condition

`synchronized` đảm bảo **tại một thời điểm, chỉ có DUY NHẤT 1 thread** được thực thi đoạn code đó (gọi là **critical section** — vùng code "nhạy cảm" cần bảo vệ) trên **cùng 1 object khóa (lock/monitor)**.

### `synchronized` method

```java
public class Counter {
    private int count = 0;

    public synchronized void increment() { // toàn bộ method là critical section, khóa trên "this"
        count++;
    }
}
```

### `synchronized` block — khóa phạm vi nhỏ hơn, kiểm soát chính xác hơn

```java
public class Counter {
    private int count = 0;
    private final Object lock = new Object(); // object riêng chỉ dùng để làm khóa

    public void increment() {
        synchronized (lock) { // chỉ khóa ĐÚNG đoạn code cần thiết, không khóa cả method
            count++;
        }
    }
}
```

> **Vì sao nên dùng `synchronized` block thay vì cả method khi có thể?** Khóa càng ít code càng tốt (giảm thời gian các thread khác phải chờ đợi) — nếu method có nhiều logic không liên quan đến dữ liệu dùng chung, khóa cả method sẽ làm giảm hiệu năng không cần thiết (các thread phải xếp hàng chờ nhau dù phần lớn code không thực sự xung đột).

### Cơ chế hoạt động (Monitor Lock)

Mỗi object trong Java có 1 **monitor lock** ẩn gắn liền với nó. Khi 1 thread vào được khối `synchronized`, nó **"giữ" (acquire)** lock của object đó — mọi thread khác muốn vào **cùng khối `synchronized` trên CÙNG object** sẽ bị **BLOCKED** (nhắc lại Thread Lifecycle ở mục 3) cho đến khi thread đang giữ lock **thoát ra** (**release** lock, tự động khi hết khối `synchronized` hoặc có exception xảy ra).

```java
Counter counter = new Counter();
// ... chạy lại đúng đoạn code 1000 thread ở mục 4 ...
System.out.println(counter.getCount()); // LUÔN LUÔN đúng = 1000
```

---

## 6. `volatile` — đảm bảo tính hiển thị (Visibility)

Đây là khái niệm **dễ nhầm lẫn nhất** với `synchronized` — `volatile` giải quyết vấn đề **KHÁC**, không phải Race Condition.

### Vấn đề: CPU Cache khiến các Thread "nhìn thấy" giá trị khác nhau

Mỗi CPU core thường có **cache riêng** — khi 1 Thread ghi giá trị mới vào biến, giá trị đó **có thể chỉ nằm trong cache của core đó**, chưa kịp "đồng bộ" trở lại bộ nhớ chính (Main Memory) — Thread khác chạy trên core khác **có thể vẫn đọc giá trị CŨ** từ cache của riêng nó.

```java
public class FlagExample {
    private boolean running = true; // KHÔNG có volatile

    public void stop() {
        running = false; // Thread A ghi giá trị mới
    }

    public void doWork() {
        while (running) { // Thread B đọc — CÓ THỂ vẫn thấy "true" mãi mãi, dù Thread A đã set false từ lâu!
            // xử lý công việc
        }
        System.out.println("Đã dừng");
    }
}
```

**Vấn đề:** nếu `Thread B` đang chạy `doWork()` trong vòng lặp `while`, và `Thread A` gọi `stop()` từ 1 thread khác, `Thread B` **có thể không bao giờ thấy** giá trị `running = false` đã thay đổi — vì trình biên dịch JIT có thể tối ưu hóa bằng cách **cache** giá trị `running` vào thanh ghi (register) của CPU thay vì đọc lại từ bộ nhớ chính mỗi vòng lặp, dẫn đến vòng lặp chạy **vô hạn**.

### `volatile` — đảm bảo MỌI thread luôn đọc/ghi trực tiếp từ Main Memory

```java
public class FlagExample {
    private volatile boolean running = true; // BẮT BUỘC đọc/ghi trực tiếp Main Memory, không qua cache riêng của thread

    public void stop() {
        running = false;
    }

    public void doWork() {
        while (running) { // LUÔN thấy giá trị MỚI NHẤT ngay khi Thread A thay đổi
            // xử lý công việc
        }
        System.out.println("Đã dừng");
    }
}
```

### ⚠️ `volatile` KHÔNG giải quyết được Race Condition

```java
private volatile int count = 0;

public void increment() {
    count++; // volatile KHÔNG BẢO VỆ được thao tác này! vẫn là 3 bước (đọc-tăng-ghi), vẫn race condition như thường
}
```
`volatile` chỉ đảm bảo **tính hiển thị (visibility)** — mọi thread đọc được giá trị mới nhất — nhưng **KHÔNG đảm bảo tính nguyên tử (atomicity)** cho các thao tác phức hợp như `count++` (đọc-sửa-ghi). Muốn an toàn cho `count++`, vẫn cần `synchronized` (hoặc `AtomicInteger` — sẽ nhắc ở Module 05.2).

---

## 7. `synchronized` vs `volatile` — khi nào dùng cái nào

| Tiêu chí | `synchronized` | `volatile` |
|---|---|---|
| Giải quyết vấn đề | Race Condition (đảm bảo Atomicity — tính nguyên tử) | Visibility (đảm bảo mọi thread thấy giá trị mới nhất) |
| Bảo vệ thao tác phức hợp (đọc-sửa-ghi) | ✅ Có | ❌ Không |
| Hiệu năng | Chậm hơn (có overhead của việc acquire/release lock, có thể gây BLOCKED) | Nhanh hơn nhiều (không có lock, chỉ đảm bảo đọc/ghi Main Memory trực tiếp) |
| Dùng phù hợp cho | Bộ đếm dùng chung, tài khoản ngân hàng, bất kỳ thao tác đọc-sửa-ghi nào trên dữ liệu chung | Cờ hiệu (flag) đơn giản dùng để dừng vòng lặp/thread, biến trạng thái chỉ ĐỌC hoặc GHI đơn lẻ (không phải đọc-sửa-ghi) |

> **Quy tắc ghi nhớ nhanh:** nếu thao tác chỉ là **gán 1 giá trị đơn giản** (ví dụ `flag = true`) mà nhiều thread cần "thấy" ngay — dùng `volatile`. Nếu thao tác là **đọc rồi tính toán rồi ghi lại** (ví dụ `count++`, `balance -= amount`) — bắt buộc phải dùng `synchronized` (hoặc các công cụ concurrency nâng cao hơn ở Module 05.2).

---

## 8. Deadlock

**Deadlock** xảy ra khi **2 (hoặc nhiều) thread chờ nhau vô thời hạn** — mỗi thread đang giữ 1 lock mà thread kia cần, và ngược lại — không ai có thể tiếp tục chạy.

### Ví dụ kinh điển: Deadlock giữa 2 lock

```java
public class DeadlockExample {
    private final Object lockA = new Object();
    private final Object lockB = new Object();

    public void method1() {
        synchronized (lockA) {
            System.out.println("Thread 1: đã giữ lockA, đang chờ lockB...");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            synchronized (lockB) { // Thread 1 cần lockB để tiếp tục
                System.out.println("Thread 1: đã giữ cả 2 lock");
            }
        }
    }

    public void method2() {
        synchronized (lockB) {
            System.out.println("Thread 2: đã giữ lockB, đang chờ lockA...");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            synchronized (lockA) { // Thread 2 cần lockA để tiếp tục
                System.out.println("Thread 2: đã giữ cả 2 lock");
            }
        }
    }
}
```

```java
DeadlockExample example = new DeadlockExample();
new Thread(example::method1).start(); // giữ lockA, chờ lockB
new Thread(example::method2).start(); // giữ lockB, chờ lockA
// KẾT QUẢ: cả 2 thread bị BLOCKED VĨNH VIỄN — chương trình "treo", không bao giờ kết thúc!
```

### Sơ đồ minh họa "vòng chờ" (Circular Wait) — điều kiện cốt lõi gây Deadlock

```
Thread 1 ──giữ──► lockA          Thread 2 ──giữ──► lockB
   │                                  │
   └──────chờ lockB (bị Thread 2 giữ)─┘
                    ▲
   ┌────chờ lockA (bị Thread 1 giữ)───┘
   │
Thread 2
```
Đây gọi là **Circular Wait** (chờ vòng tròn) — điều kiện **bắt buộc** để Deadlock xảy ra. Cả 2 thread đều **giữ 1 phần tài nguyên** và **chờ phần còn lại** đang bị đối phương giữ — không ai chịu nhường, không ai có thể tiến lên.

### Cách phòng tránh Deadlock

> **Nguyên tắc quan trọng nhất: luôn giành lock theo MỘT THỨ TỰ CỐ ĐỊNH, nhất quán trong toàn bộ ứng dụng.**

```java
// Sửa lại: CẢ HAI method đều giành lockA TRƯỚC, lockB SAU — thứ tự nhất quán, không thể xảy ra Circular Wait
public void method1() {
    synchronized (lockA) {
        synchronized (lockB) {
            // ...
        }
    }
}

public void method2() {
    synchronized (lockA) { // ĐÃ ĐỔI — cùng thứ tự với method1, không còn giành lockB trước
        synchronized (lockB) {
            // ...
        }
    }
}
```

Các nguyên tắc phòng tránh Deadlock khác:
1. **Giữ lock trong thời gian ngắn nhất có thể** — giảm khả năng xung đột.
2. **Tránh giữ nhiều lock cùng lúc** nếu có thể tái cấu trúc logic để không cần thiết.
3. Dùng `tryLock()` với timeout (thuộc `java.util.concurrent.locks.Lock`, công cụ nâng cao hơn `synchronized`, sẽ gặp ở Module 05.2) — cho phép thread "bỏ cuộc" sau 1 khoảng thời gian thay vì chờ vô hạn.

---

## 9. Các phương thức điều khiển Thread quan trọng

```java
Thread.sleep(1000);      // static method — tạm dừng THREAD HIỆN TẠI trong 1000ms, không giữ lock (nếu có) trong lúc sleep
thread.join();            // thread GỌI join() sẽ CHỜ cho đến khi "thread" (object đang gọi join trên nó) chạy xong (TERMINATED)
thread.interrupt();       // gửi tín hiệu "ngắt" đến thread — thread cần TỰ kiểm tra Thread.interrupted() hoặc bắt InterruptedException để phản hồi
Thread.currentThread();   // lấy reference đến thread ĐANG chạy đoạn code này
thread.setName("Worker-1"); // đặt tên cho thread — hữu ích khi debug/log trong hệ thống nhiều luồng
thread.setDaemon(true);    // đánh dấu là "daemon thread" — JVM sẽ tự thoát dù daemon thread chưa chạy xong (khác với thread thường — JVM CHỜ mọi thread thường kết thúc mới thoát)
```

### `join()` — ví dụ minh họa quan trọng (đã dùng ở mục 4)

```java
List<Thread> threads = new ArrayList<>();
for (int i = 0; i < 5; i++) {
    Thread t = new Thread(() -> System.out.println("Đang xử lý..."));
    threads.add(t);
    t.start();
}

// Nếu KHÔNG có join(), dòng dưới có thể chạy TRƯỚC KHI các thread ở trên hoàn thành xong!
for (Thread t : threads) {
    t.join(); // main thread CHỜ từng thread trong danh sách hoàn thành
}
System.out.println("Tất cả đã xử lý xong"); // đảm bảo chắc chắn chạy SAU CÙNG
```

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| Process vs Thread | Thread chia sẻ chung Heap trong 1 Process — nguồn gốc của mọi vấn đề concurrency |
| `Runnable` vs `extends Thread` | Nên ưu tiên `Runnable` — tránh mất khả năng đa kế thừa, dễ dùng với Thread Pool |
| `start()` vs `run()` | `start()` tạo thread mới thực sự; gọi `run()` trực tiếp chỉ chạy tuần tự như method thường |
| Thread Lifecycle | NEW → RUNNABLE → (BLOCKED/WAITING) → TERMINATED — không `start()` lại được thread đã TERMINATED |
| Race Condition | Nhiều thread cùng đọc-sửa-ghi 1 dữ liệu chung, kết quả phụ thuộc thứ tự thực thi ngẫu nhiên |
| `synchronized` | Đảm bảo Atomicity — chỉ 1 thread vào critical section tại 1 thời điểm |
| `volatile` | Đảm bảo Visibility — mọi thread đọc giá trị mới nhất từ Main Memory, KHÔNG đảm bảo Atomicity |
| Deadlock | 2+ thread chờ lock lẫn nhau vô thời hạn (Circular Wait) — phòng tránh bằng cách giành lock theo thứ tự cố định |
| `join()` | Thread gọi hàm này sẽ CHỜ thread khác hoàn thành trước khi tiếp tục |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau có tạo ra thread mới không? Giải thích.
```java
Thread t = new Thread(() -> System.out.println("Hello"));
t.run();
```

**Câu 2.** Đoạn code sau chạy 1000 thread cùng tăng biến `count`, kết quả in ra có luôn là `1000` không? Giải thích.
```java
private static int count = 0;
public static void increment() { count++; }
```

**Câu 3.** Sửa đoạn code ở Câu 2 bằng `synchronized`, giải thích tại sao cách sửa của bạn đảm bảo kết quả luôn đúng.

**Câu 4.** Đoạn code sau có đảm bảo Thread B thoát khỏi vòng lặp khi Thread A gọi `stopWork()` không? Nếu không, sửa lại.
```java
public class Worker {
    private boolean stop = false;
    public void doWork() {
        while (!stop) { /* làm việc */ }
    }
    public void stopWork() { stop = true; }
}
```

**Câu 5.** 2 method sau có nguy cơ Deadlock không? Nếu có, chỉ ra nguyên nhân và đề xuất cách sửa.
```java
void transfer(Account from, Account to, double amount) {
    synchronized (from) {
        synchronized (to) {
            from.withdraw(amount);
            to.deposit(amount);
        }
    }
}
// Giả sử có 2 lời gọi ĐỒNG THỜI:
// Thread 1: transfer(accountA, accountB, 100);
// Thread 2: transfer(accountB, accountA, 50);
```

---

### Phần B — Bài tập viết code

**Bài 1 — Tái hiện Race Condition rồi sửa bằng synchronized.**
Viết class `BankAccount` với field `balance`. Viết method `deposit(double amount)` **KHÔNG** đồng bộ hóa. Tạo 100 thread, mỗi thread gọi `deposit(10)` — dùng `join()` chờ tất cả hoàn thành, in `balance` cuối cùng (kỳ vọng 1000, nhưng thực tế thường sai). Sau đó sửa `deposit()` bằng `synchronized`, chạy lại 5 lần liên tiếp để chứng minh kết quả **luôn đúng 1000** mỗi lần.

**Bài 2 — So sánh synchronized method vs synchronized block.**
Viết class `Logger` có 2 field: `logCount` (dùng chung, cần đồng bộ) và `configName` (không đổi, không cần đồng bộ). Viết method `log(String message)` chỉ khóa (`synchronized` block) đúng phần code liên quan đến `logCount++`, KHÔNG khóa phần in `configName`. Giải thích bằng comment trong code tại sao cách làm này hiệu quả hơn khóa cả method.

**Bài 3 — Minh họa volatile bằng ví dụ dừng thread an toàn.**
Viết class `Worker implements Runnable` có field `volatile boolean running = true`, vòng lặp `while(running)` in ra số đếm tăng dần mỗi 100ms (dùng `Thread.sleep(100)`). Viết `main` khởi động Worker trên 1 thread riêng, sau 2 giây gọi `stop()` (set `running = false`) từ main thread, dùng `join()` chờ Worker thread kết thúc hẳn, in ra "Worker đã dừng thành công". Thử **bỏ** từ khóa `volatile` để quan sát (có thể không tái hiện được ngay trên máy cá nhân do JIT tối ưu khác nhau — nhưng vẫn nên thử và ghi nhận, đồng thời giải thích bằng lý thuyết dù không quan sát được trực tiếp).

**Bài 4 — Tái hiện Deadlock rồi sửa bằng cách sắp xếp thứ tự lock.**
Cài đặt lại chính xác ví dụ Deadlock ở mục 8 (2 method giành lock theo thứ tự khác nhau). Chạy chương trình, quan sát nó "treo" (không bao giờ kết thúc — cần tự dừng chương trình thủ công sau khi quan sát). Sau đó sửa lại theo nguyên tắc "thứ tự lock cố định", chạy lại để chứng minh chương trình luôn kết thúc bình thường.

**Bài 5 — Bài toán tổng hợp: Hệ thống đặt vé đơn giản (mô phỏng race condition thực tế, chuẩn bị tư duy cho capstone Flash-Sale).**
Viết class `TicketBooth` với field `int availableTickets = 100`. Viết method `boolean bookTicket()`: nếu còn vé (`availableTickets > 0`), giảm `availableTickets` đi 1 và trả về `true`; nếu hết vé, trả về `false`. Tạo 200 thread (nhiều hơn số vé thực có) cùng gọi `bookTicket()`, đếm tổng số lần trả về `true` bằng 1 biến đếm dùng chung (cũng cần đồng bộ hóa đúng cách!). Kiểm tra: nếu **không** đồng bộ hóa `bookTicket()`, số vé bán ra có thể **vượt quá 100** (oversold — bán vượt quá số lượng thực có, đúng là lỗi nghiêm trọng trong hệ thống bán vé thực tế) — sau đó sửa lại bằng `synchronized` để đảm bảo **không bao giờ** bán vượt quá 100 vé dù có bao nhiêu thread cùng tranh giành.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Không** — gọi `t.run()` trực tiếp chỉ thực thi `run()` như 1 method bình thường, chạy tuần tự trên thread hiện tại (thường là main thread), không hề tạo thread mới. Phải gọi `t.start()` để JVM thực sự tạo 1 thread thực thi riêng.
2. **Không đảm bảo** — vì `count++` không phải thao tác nguyên tử (atomic), gồm 3 bước đọc-tăng-ghi riêng biệt. Với 1000 thread chạy đồng thời không đồng bộ hóa, kết quả thường **nhỏ hơn 1000** và thay đổi ngẫu nhiên giữa các lần chạy — đây chính là Race Condition.
3. Thêm `synchronized` vào method `increment()` (hoặc dùng `synchronized` block bọc quanh `count++`) — đảm bảo tại một thời điểm chỉ 1 thread được thực hiện trọn vẹn cả 3 bước đọc-tăng-ghi, không có thread nào khác "chen vào giữa" làm sai lệch giá trị.
4. **Không đảm bảo** — vì `stop` không có `volatile`, Thread B (đang chạy `doWork()`) có thể **không bao giờ thấy** giá trị mới `stop = true` do JIT compiler tối ưu hóa việc đọc biến từ cache CPU thay vì Main Memory, dẫn đến vòng lặp chạy vô hạn. Sửa: thêm `volatile` vào khai báo `private volatile boolean stop = false;`.
5. **Có nguy cơ Deadlock rất cao** — Thread 1 giành lock `accountA` trước rồi mới giành `accountB`; Thread 2 (do gọi `transfer(accountB, accountA, ...)`) giành lock `accountB` trước rồi mới giành `accountA` — thứ tự **ngược nhau**, tạo điều kiện Circular Wait kinh điển. Cách sửa: luôn giành lock theo thứ tự **cố định** không phụ thuộc tham số truyền vào, ví dụ sắp xếp theo `accountId` hoặc `hashCode()` của account trước khi giành lock (ví dụ: luôn khóa account có `id` nhỏ hơn trước).

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Đây là bài tập kinh điển nhất để **tự mắt thấy** Race Condition trước khi tin vào lý thuyết — kết quả không đồng bộ hóa thường dao động quanh 900-990 thay vì đúng 1000 (con số cụ thể phụ thuộc vào tốc độ CPU và số nhân của máy chạy thử).
- **Bài 3:** Nếu bỏ `volatile`, về mặt lý thuyết Worker thread có thể chạy vô hạn không bao giờ thấy `running = false` — dù trên thực tế nhiều máy hiện đại/JVM version mới có thể **vẫn** vô tình hoạt động đúng do các tối ưu hóa khác nhau của JIT compiler ở từng thời điểm/phiên bản. Đây chính là điều làm race condition/visibility bug NGUY HIỂM: **"có vẻ chạy đúng khi test" không đồng nghĩa "chắc chắn đúng"** — luôn phải áp dụng đúng lý thuyết (`volatile`/`synchronized`) thay vì dựa vào may rủi quan sát được.
- **Bài 4:** Sau khi tái hiện Deadlock, cách sửa chuẩn nhất: định nghĩa 1 thứ tự nhất quán (ví dụ so sánh `System.identityHashCode(lockA)` và `System.identityHashCode(lockB)`, luôn giành lock có hashCode nhỏ hơn trước) — áp dụng đúng nguyên tắc đã học ở mục 8.
- **Bài 5:** Đây là bài tập **quan trọng nhất** của module, vì mô phỏng trực tiếp bài toán cốt lõi của capstone "High-Concurrency Event Ticketing & Flash-Sale Engine" — nếu không đồng bộ hóa đúng cách, hệ thống thực tế sẽ **bán vượt quá số lượng vé/sản phẩm có thật (oversold)**, gây hậu quả nghiêm trọng về nghiệp vụ và uy tín. Ghi nhớ: đây mới là giải pháp **Java thuần túy cơ bản** (`synchronized`) — trong dự án capstone thực tế với backend phân tán (nhiều instance service chạy song song), sẽ cần thêm các công cụ ở tầng cao hơn (distributed lock qua Redis, `SELECT ... FOR UPDATE` ở database, hoặc lạc quan khóa qua `@Version` của JPA) — những chủ đề sẽ gặp lại ở Module 18 (Caching & Messaging) và khi triển khai capstone.

</details>

---

*File tiếp theo trong lộ trình: **Module 05.2 — Concurrency Utilities** (ExecutorService, ThreadPoolExecutor, Future/CompletableFuture, CountDownLatch, Semaphore).*
