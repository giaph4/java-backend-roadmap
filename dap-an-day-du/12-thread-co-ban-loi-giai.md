# Lời giải đầy đủ — Module 05.1: Thread cơ bản

> Nguồn đề: `12-thread-co-ban/12-thread-co-ban.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Tái hiện rồi sửa race condition

### Đề
`BankAccount` với `long balanceCents`. `deposit(long)` **không** đồng bộ. 100 thread mỗi thread `deposit(1000)`, `join` hết, in `balance` (kỳ vọng 100_000, thực tế thường thiếu). Sửa bằng `synchronized`, chạy 5 lần chứng minh luôn đúng.

### Phân tích

`balance += amount` **KHÔNG PHẢI** 1 thao tác nguyên tử (atomic) — nó thực chất là **3 bước riêng biệt** ở tầng bytecode/CPU: (1) đọc giá trị `balance` hiện tại, (2) cộng thêm `amount`, (3) ghi lại kết quả. Nếu 2 thread cùng thực hiện 3 bước này **xen kẽ nhau** (interleaving), 1 thread có thể ghi đè lên kết quả thread kia dựa trên giá trị **cũ đã lỗi thời** — đây chính là **race condition** kinh điển.

### Lời giải

```java
package baitap.bai1;

import java.util.ArrayList;
import java.util.List;

public class Main {

    static class BankAccount {
        private long balanceCents = 0;

        // KHÔNG đồng bộ - race condition
        public void depositUnsafe(long amount) {
            balanceCents += amount; // đọc -> cộng -> ghi: 3 bước KHÔNG nguyên tử
        }

        // ĐỒNG BỘ - an toàn
        public synchronized void depositSafe(long amount) {
            balanceCents += amount;
        }

        public long getBalance() { return balanceCents; }
        public void reset() { balanceCents = 0; }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 100;
        long depositPerThread = 1000;
        long expected = threadCount * depositPerThread;

        System.out.println("===== KHÔNG đồng bộ (depositUnsafe) =====");
        BankAccount unsafeAccount = new BankAccount();
        runConcurrentDeposits(unsafeAccount::depositUnsafe, threadCount, depositPerThread);
        System.out.println("Kỳ vọng: " + expected + ", Thực tế: " + unsafeAccount.getBalance()
                + " (thiếu " + (expected - unsafeAccount.getBalance()) + ")");

        System.out.println("\n===== CÓ đồng bộ (depositSafe) - chạy 5 lần =====");
        long totalDiscrepancy = 0;
        for (int run = 1; run <= 5; run++) {
            BankAccount safeAccount = new BankAccount();
            runConcurrentDeposits(safeAccount::depositSafe, threadCount, depositPerThread);
            long diff = expected - safeAccount.getBalance();
            totalDiscrepancy += Math.abs(diff);
            System.out.println("Lần " + run + ": balance = " + safeAccount.getBalance()
                    + (diff == 0 ? " (ĐÚNG)" : " (SAI - lệch " + diff + ")"));
        }
        System.out.println("Sai lệch trung bình qua 5 lần: " + (totalDiscrepancy / 5.0));
    }

    interface DepositAction { void deposit(long amount); }

    static void runConcurrentDeposits(DepositAction action, int threadCount, long amount) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> action.deposit(amount));
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join(); // CHỜ TẤT CẢ thread hoàn tất trước khi đọc kết quả cuối cùng
        }
    }
}
```

**Kết quả tiêu biểu (con số cụ thể của bản KHÔNG đồng bộ thay đổi ngẫu nhiên mỗi lần chạy):**
```
===== KHÔNG đồng bộ (depositUnsafe) =====
Kỳ vọng: 100000, Thực tế: 97000 (thiếu 3000)

===== CÓ đồng bộ (depositSafe) - chạy 5 lần =====
Lần 1: balance = 100000 (ĐÚNG)
Lần 2: balance = 100000 (ĐÚNG)
Lần 3: balance = 100000 (ĐÚNG)
Lần 4: balance = 100000 (ĐÚNG)
Lần 5: balance = 100000 (ĐÚNG)
Sai lệch trung bình qua 5 lần: 0.0
```

### Giải thích

- **Vì sao con số thiếu KHÔNG cố định qua các lần chạy:** race condition phụ thuộc vào **cách JVM/hệ điều hành lập lịch (schedule)** các thread — không có gì đảm bảo timing giống nhau giữa các lần chạy. Đây chính là lý do bug race condition **cực kỳ khó tái hiện/debug** — có thể chạy đúng 9 lần liên tiếp rồi lần thứ 10 mới sai, khiến việc test thông thường dễ "bỏ lọt".
- `synchronized` trên `depositSafe()` đảm bảo tại **1 thời điểm chỉ 1 thread** được thực thi thân method này (dùng **intrinsic lock** của chính object `this`) — thread khác phải **CHỜ** tới khi thread đang giữ lock xong việc mới được vào, loại bỏ hoàn toàn khả năng đọc-cộng-ghi bị xen kẽ.
- Cái giá phải trả: `synchronized` làm **giảm throughput** (các thread phải xếp hàng thay vì chạy song song hoàn toàn tự do) — đây chính là chủ đề của Bài 2 tiếp theo, về cách **thu hẹp phạm vi khóa** để giảm thiểu cái giá này.

---

## Bài 2 — synchronized method vs block

### Đề
`RequestLogger` có `long total` (chung, cần đồng bộ) và `String node` (bất biến). `log(String msg)` chỉ `synchronized` đúng `total++`, phần format/in dùng `node` để ngoài. Đo throughput 8 thread × 100_000 lần với 2 cách (khóa cả method vs khóa hẹp).

### Phân tích

**Nguyên tắc:** chỉ nên `synchronized` **phần code THỰC SỰ cần bảo vệ** (đọc/ghi shared mutable state) — phần code **KHÔNG đụng tới shared state** (ở đây: format chuỗi dùng `node` bất biến, `System.out.println`) nên nằm **NGOÀI** khối khóa, để các thread khác **không phải chờ** trong lúc thread hiện tại đang làm việc không liên quan tới `total`.

### Lời giải

```java
package baitap.bai2;

import java.util.concurrent.atomic.AtomicLong;

public class Main {

    // ===== Phiên bản 1: khóa CẢ METHOD (thô, contention cao) =====
    static class RequestLoggerWideLock {
        private long total = 0;
        private final String node = "server-01";

        public synchronized void log(String msg) {
            total++;                                        // CẦN đồng bộ
            String formatted = "[" + node + "] " + msg;      // KHÔNG cần đồng bộ - nhưng vẫn bị khóa CHUNG
            // (bỏ println thật để đo throughput công bằng, không bị I/O làm nhiễu số liệu)
        }

        public long getTotal() { return total; }
    }

    // ===== Phiên bản 2: khóa HẸP (chỉ đúng phần cần) =====
    static class RequestLoggerNarrowLock {
        private long total = 0;
        private final Object lock = new Object(); // lock riêng, tách biệt khỏi "this" (thực hành tốt)
        private final String node = "server-01";  // bất biến - AN TOÀN đọc từ nhiều thread không cần khóa

        public void log(String msg) {
            synchronized (lock) {
                total++; // CHỈ khóa đúng dòng cần bảo vệ
            }
            String formatted = "[" + node + "] " + msg; // NGOÀI khóa - thread khác không phải chờ đoạn này
        }

        public long getTotal() {
            synchronized (lock) { return total; }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 8;
        int callsPerThread = 100_000;

        RequestLoggerWideLock wideLogger = new RequestLoggerWideLock();
        long wideTime = benchmark(wideLogger::log, threadCount, callsPerThread);
        System.out.println("Khóa CẢ METHOD:  " + wideTime + " ms, total=" + wideLogger.getTotal());

        RequestLoggerNarrowLock narrowLogger = new RequestLoggerNarrowLock();
        long narrowTime = benchmark(narrowLogger::log, threadCount, callsPerThread);
        System.out.println("Khóa HẸP (block): " + narrowTime + " ms, total=" + narrowLogger.getTotal());
    }

    interface LogAction { void log(String msg); }

    static long benchmark(LogAction action, int threadCount, int callsPerThread) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    action.log("request-" + j);
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        return System.currentTimeMillis() - start;
    }
}
```

**Kết quả tiêu biểu (chênh lệch phụ thuộc số core CPU, luôn theo xu hướng khóa hẹp nhanh hơn):**
```
Khóa CẢ METHOD:  145 ms, total=800000
Khóa HẸP (block): 98 ms, total=800000
```

### Giải thích vì sao khóa hẹp hơn = ít contention hơn

- **Contention (tranh chấp lock):** khi nhiều thread cùng cố giành **1 lock**, chỉ 1 thread được vào, các thread còn lại phải **CHỜ** (block) — thời gian chờ này là "lãng phí" thuần túy, không làm việc gì. **Contention càng cao** (nhiều thread tranh nhau, mỗi thread giữ lock càng LÂU) → throughput tổng thể càng giảm.
- Ở `RequestLoggerWideLock`, **toàn bộ thân method** (kể cả phần format chuỗi — vốn không đụng `total` chút nào) nằm trong vùng khóa — kéo dài **thời gian mỗi thread giữ lock**, khiến các thread khác phải chờ lâu hơn mức cần thiết.
- Ở `RequestLoggerNarrowLock`, vùng khóa chỉ bao **đúng 1 dòng `total++`** — cực kỳ ngắn — thread giữ lock rồi **nhả ra gần như ngay lập tức**, giảm tối đa thời gian các thread khác phải chờ đợi, dù tổng số lần `synchronized` được gọi **KHÔNG đổi** — chỉ đổi **THỜI GIAN mỗi lần giữ lock**.
- **Nguyên tắc thực chiến:** luôn tìm cách **thu hẹp critical section (vùng cần đồng bộ) tới mức tối thiểu tuyệt đối** — đây là 1 trong những kỹ thuật tối ưu hiệu năng đa luồng quan trọng nhất, áp dụng trực tiếp khi thiết kế các thành phần dùng chung trong hệ thống Backend chịu tải cao.

---

## Bài 3 — volatile & dừng thread an toàn

### Đề
`Worker implements Runnable` có `volatile boolean running = true`, vòng `while(running)` tăng bộ đếm và `sleep(50)`. `main`: chạy trên thread riêng, sau 1s gọi `stop()`, `join()`, in kết quả. Thử bỏ `volatile` — giải thích vì sao không thể dựa vào "máy mình vẫn chạy đúng".

### Phân tích

**`volatile`** đảm bảo 2 điều: (1) **Visibility** — mọi thread đọc field `volatile` luôn thấy **giá trị MỚI NHẤT** đã ghi bởi thread khác (không bị "kẹt" đọc giá trị cache cục bộ trong CPU register/core cache của riêng thread đó); (2) ngăn 1 số **compiler/JIT optimization** có thể "tối ưu hóa nhầm" vòng lặp `while(running)` thành `while(true)` (nếu compiler thấy `running` "không bao giờ đổi" từ góc nhìn của riêng thread đang chạy vòng lặp, nó có thể hợp lý hóa việc **không đọc lại** biến từ bộ nhớ chính mỗi vòng lặp).

### Lời giải

```java
package baitap.bai3;

public class Main {

    static class Worker implements Runnable {
        private volatile boolean running = true;
        private int counter = 0;

        @Override
        public void run() {
            while (running) {
                counter++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        public void stop() {
            running = false; // GHI từ thread KHÁC (main) - cần volatile để thread run() THẤY được ngay
        }

        public int getCounter() { return counter; }
    }

    public static void main(String[] args) throws InterruptedException {
        Worker worker = new Worker();
        Thread thread = new Thread(worker);
        thread.start();

        Thread.sleep(1000); // để Worker chạy 1 giây
        worker.stop();
        thread.join(); // CHỜ thread thực sự dừng hẳn trước khi đọc counter

        System.out.println("Đã dừng, đếm được " + worker.getCounter());
    }
}
```

**Kết quả chạy:**
```
Đã dừng, đếm được 20
```
*(xấp xỉ 1000ms / 50ms = 20 lần, có thể lệch ±1-2 do thời gian khởi động thread)*

### Thử bỏ `volatile` — quan sát và giải thích

```java
private boolean running = true; // BỎ "volatile"
```

**Kết quả có thể xảy ra khi bỏ `volatile` (KHÔNG ĐẢM BẢO, tùy máy/JVM):**
```
(chương trình TREO VĨNH VIỄN - không bao giờ in ra dòng "Đã dừng...")
```

**Giải thích vì sao KHÔNG thể dựa vào "máy mình vẫn chạy đúng":**

- Nếu bỏ `volatile`, chương trình **CÓ THỂ** vẫn chạy đúng trên máy dev của bạn — vì trên thực tế, nhiều JVM/CPU hiện đại **thường** đồng bộ hóa giá trị field giữa các core khá nhanh trong tình huống đơn giản này, hoặc JIT compiler **CHƯA KỊP** tối ưu vòng lặp (chỉ tối ưu sau khi chạy đủ "nóng" — liên hệ tiered compilation Module 07). Đây chính là cái bẫy nguy hiểm: **thiếu `volatile` là VI PHẠM ĐÚNG ĐẮN theo đặc tả Java Memory Model, nhưng KHÔNG PHẢI LÚC NÀO CŨNG BIỂU HIỆN THÀNH BUG QUAN SÁT ĐƯỢC** — hành vi phụ thuộc vào **kiến trúc CPU, phiên bản JVM, mức độ tối ưu hóa JIT tại thời điểm chạy, thậm chí tải hệ thống lúc đó**.
- Trên production (server khác kiến trúc CPU, JVM version khác, chạy lâu hơn để JIT tối ưu sâu hơn, dưới tải cao), đoạn code **thiếu `volatile` y hệt** hoàn toàn có thể biểu hiện: `thread.join()` **treo mãi mãi** vì thread `run()` **không bao giờ thấy** giá trị `running = false` mới (bị "kẹt" đọc giá trị cache cũ hoặc bị JIT tối ưu bỏ qua việc đọc lại từ bộ nhớ).
- **Bài học cốt lõi (liên hệ trực tiếp Module 05.1 phần lý thuyết Happens-Before):** đúng đắn của code đa luồng **phải được chứng minh bằng đặc tả (Java Memory Model)**, không phải bằng "chạy thử thấy đúng trên máy tôi". Đây là lớp bug **cực kỳ nguy hiểm** vì có thể **ẩn náu qua hàng tháng test** rồi mới bùng phát ở production dưới điều kiện tải/phần cứng khác.

---

## Bài 4 — Deadlock: tái hiện & sửa bằng lock ordering

### Đề
Cài đúng ví dụ deadlock (2 method giành lock ngược thứ tự). Chạy, xác nhận treo (`jstack`/`jcmd`). Sửa bằng lock ordering theo `System.identityHashCode`.

### Phân tích

**Deadlock** xảy ra khi 2 (hoặc nhiều) thread **giữ lock của nhau đang cần**, mỗi thread **chờ** thread kia nhả lock ra — cả 2 **chờ mãi mãi**, không ai nhường ai. Điều kiện kinh điển: Thread A giữ Lock1, đang chờ Lock2; Thread B giữ Lock2, đang chờ Lock1 — vòng chờ khép kín (circular wait).

**Giải pháp — Lock Ordering:** đảm bảo **MỌI thread LUÔN giành lock theo ĐÚNG 1 THỨ TỰ CỐ ĐỊNH** (VD: theo `System.identityHashCode()` của object, hoặc theo 1 ID cố định gán sẵn) — nếu tất cả thread đều tuân theo cùng 1 thứ tự, **không thể xảy ra vòng chờ khép kín**.

### Lời giải — Tái hiện Deadlock

```java
package baitap.bai4;

public class DeadlockDemo {
    static final Object lockA = new Object();
    static final Object lockB = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> transferAtoB(), "Thread-1-A-to-B");
        Thread t2 = new Thread(() -> transferBtoA(), "Thread-2-B-to-A");

        t1.start();
        t2.start();

        // Chương trình sẽ TREO VĨNH VIỄN tại đây (không bao giờ kết thúc)
        // Dùng lệnh (chạy trong terminal khác, trong lúc chương trình đang treo):
        //   jcmd <pid> Thread.print
        // hoặc:
        //   jstack <pid>
        // sẽ thấy đoạn:
        //
        //   Found one Java-level deadlock:
        //   =============================
        //   "Thread-2-B-to-A":
        //     waiting to lock monitor 0x... (object lockA),
        //     which is held by "Thread-1-A-to-B"
        //   "Thread-1-A-to-B":
        //     waiting to lock monitor 0x... (object lockB),
        //     which is held by "Thread-2-B-to-A"
        //
        //   Java stack information for the threads listed above:
        //   ===================================================
        //   "Thread-2-B-to-A":
        //           at baitap.bai4.DeadlockDemo.transferBtoA(DeadlockDemo.java:XX)
        //           - waiting to lock <0x...> (a java.lang.Object)
        //           - locked <0x...> (a java.lang.Object)
        //   "Thread-1-A-to-B":
        //           at baitap.bai4.DeadlockDemo.transferAtoB(DeadlockDemo.java:XX)
        //           - waiting to lock <0x...> (a java.lang.Object)
        //           - locked <0x...> (a java.lang.Object)
    }

    static void transferAtoB() {
        synchronized (lockA) {
            System.out.println(Thread.currentThread().getName() + " đã giữ lockA");
            sleepQuietly(100); // tạo "khoảng hở" đủ để thread kia kịp giành lockB trước
            synchronized (lockB) { // ĐANG GIỮ lockA, CHỜ lockB
                System.out.println("Chuyển tiền A -> B thành công");
            }
        }
    }

    static void transferBtoA() {
        synchronized (lockB) {
            System.out.println(Thread.currentThread().getName() + " đã giữ lockB");
            sleepQuietly(100);
            synchronized (lockA) { // ĐANG GIỮ lockB, CHỜ lockA -> DEADLOCK với thread trên!
                System.out.println("Chuyển tiền B -> A thành công");
            }
        }
    }

    static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
```

### Lời giải — Sửa bằng Lock Ordering

```java
package baitap.bai4;

public class DeadlockFixed {
    static final Object lockA = new Object();
    static final Object lockB = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> transfer(lockA, lockB, "A -> B"), "Thread-1");
        Thread t2 = new Thread(() -> transfer(lockB, lockA, "B -> A"), "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Chương trình KẾT THÚC bình thường - không deadlock!");
    }

    // Nhận 2 lock theo THAM SỐ (thứ tự "mong muốn nghiệp vụ" có thể khác nhau giữa các lời gọi),
    // nhưng BÊN TRONG luôn SẮP XẾP LẠI theo identityHashCode trước khi giành lock
    static void transfer(Object requestedFirst, Object requestedSecond, String label) {
        Object first, second;

        // Luôn giành lock theo THỨ TỰ CỐ ĐỊNH dựa trên identityHashCode - KHÔNG phụ thuộc
        // thứ tự nghiệp vụ yêu cầu (A->B hay B->A) - đây là chìa khóa phá vỡ circular wait
        if (System.identityHashCode(requestedFirst) < System.identityHashCode(requestedSecond)) {
            first = requestedFirst;
            second = requestedSecond;
        } else {
            first = requestedSecond;
            second = requestedFirst;
        }

        synchronized (first) {
            sleepQuietly(50);
            synchronized (second) {
                System.out.println(label + " thành công (thread: " + Thread.currentThread().getName() + ")");
            }
        }
    }

    static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
```

**Kết quả chạy (LUÔN kết thúc, không bao giờ treo):**
```
A -> B thành công (thread: Thread-1)
B -> A thành công (thread: Thread-2)
Chương trình KẾT THÚC bình thường - không deadlock!
```

### Giải thích

- **Vì sao bản gốc deadlock:** Thread 1 giành `lockA` trước, `sleep(100)` (tạo khoảng hở), rồi mới xin `lockB`. Trong khoảng hở đó, Thread 2 kịp giành `lockB`, rồi cũng `sleep`, rồi xin `lockA` — lúc này Thread 1 đang **giữ `lockA`, chờ `lockB`**; Thread 2 đang **giữ `lockB`, chờ `lockA`** — vòng chờ khép kín, treo vĩnh viễn.
- **Vì sao lock ordering giải quyết triệt để:** dù lời gọi nghiệp vụ là "A→B" hay "B→A", **CẢ 2 THREAD ĐỀU GIÀNH LOCK THEO ĐÚNG 1 THỨ TỰ CỐ ĐỊNH** (dựa trên `identityHashCode` — con số cố định, không đổi cho mỗi object suốt vòng đời) — VD nếu `identityHashCode(lockA) < identityHashCode(lockB)`, **MỌI** thread đều phải giành `lockA` trước `lockB`, không có ngoại lệ. Không thể có tình huống "Thread 1 giữ X chờ Y, Thread 2 giữ Y chờ X" nữa, vì **không ai được phép giữ Y trước khi có X** — loại bỏ hoàn toàn điều kiện "circular wait" cần thiết để deadlock xảy ra.
- **Liên hệ thực tế:** kỹ thuật lock ordering này áp dụng trực tiếp khi thiết kế hệ thống chuyển khoản/giao dịch có **nhiều tài khoản tham gia đồng thời** — luôn khóa theo thứ tự cố định (VD: theo `accountId` tăng dần) thay vì theo thứ tự "ai chuyển cho ai" (dễ đảo ngược qua lại gây deadlock như bài này).

---

## Bài 5 — wait/notify: hàng đợi chặn 1 phần tử

### Đề
Tự viết `SynchronousBox<T>` với `put(T)`/`take()` dùng `wait`/`notifyAll` (không dùng `java.util.concurrent`). 1 producer đẩy 20 số, 2 consumer lấy ra; đảm bảo: `wait()` trong `while`, không mất phần tử, không deadlock khi cả 2 consumer cùng chờ.

### Phân tích

`SynchronousBox` chỉ chứa **TỐI ĐA 1 phần tử tại 1 thời điểm** — `put()` phải **CHỜ** nếu box đang đầy; `take()` phải **CHỜ** nếu box đang rỗng. Quy tắc bất di bất dịch: **luôn gọi `wait()` trong vòng `while` (không phải `if`)** — vì khi thread bị đánh thức (`notifyAll`), nó **PHẢI kiểm tra lại điều kiện** trước khi tiếp tục (có thể 1 thread khác đã "nhanh chân" hơn giành mất phần tử/chỗ trống ngay sau khi được đánh thức nhưng trước khi thread này kịp chạy tiếp — hiện tượng gọi là **"spurious wakeup"** hoặc do có nhiều thread cùng chờ).

### Lời giải

```java
package baitap.bai5;

public class SynchronousBox<T> {
    private T item;
    private boolean hasItem = false;

    public synchronized void put(T value) throws InterruptedException {
        while (hasItem) { // dùng WHILE, không phải IF - kiểm tra lại sau khi được đánh thức
            wait(); // nhả lock, chờ tới khi được notifyAll()
        }
        item = value;
        hasItem = true;
        notifyAll(); // đánh thức TẤT CẢ thread đang chờ (cả producer chờ chỗ trống lẫn consumer chờ dữ liệu)
    }

    public synchronized T take() throws InterruptedException {
        while (!hasItem) { // dùng WHILE - có thể bị "cướp" bởi consumer khác trước khi tới lượt mình
            wait();
        }
        T value = item;
        item = null;
        hasItem = false;
        notifyAll(); // đánh thức producer đang chờ chỗ trống (hoặc consumer khác đang chờ, dù lúc này box đã rỗng)
        return value;
    }

    public static void main(String[] args) throws InterruptedException {
        SynchronousBox<Integer> box = new SynchronousBox<>();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    box.put(i);
                    System.out.println("[Producer] đã put " + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        Runnable consumerTask = () -> {
            try {
                while (true) {
                    Integer value = box.take();
                    System.out.println("  [" + Thread.currentThread().getName() + "] đã take " + value);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread consumer1 = new Thread(consumerTask, "Consumer-1");
        Thread consumer2 = new Thread(consumerTask, "Consumer-2");
        consumer1.setDaemon(true); // daemon - tự kết thúc khi main thoát, không cần "dừng" tường minh
        consumer2.setDaemon(true);

        producer.start();
        consumer1.start();
        consumer2.start();

        producer.join(); // chờ producer đẩy hết 20 số
        Thread.sleep(200); // cho consumer kịp lấy nốt phần tử cuối
        System.out.println("Hoàn tất - producer đã đẩy đủ 20 số");
    }
}
```

**Kết quả chạy (thứ tự Consumer-1/Consumer-2 xen kẽ ngẫu nhiên, nhưng LUÔN đủ 20 số, không thiếu/trùng):**
```
[Producer] đã put 1
  [Consumer-1] đã take 1
[Producer] đã put 2
  [Consumer-2] đã take 2
[Producer] đã put 3
  [Consumer-1] đã take 3
...
[Producer] đã put 20
  [Consumer-2] đã take 20
Hoàn tất - producer đã đẩy đủ 20 số
```

### Giải thích

- **Vì sao PHẢI dùng `notifyAll()` thay vì `notify()`:** có **2 LOẠI** thread khác nhau cùng chờ trên `SynchronousBox` (producer chờ ở `put()`, consumer chờ ở `take()`) — `notify()` chỉ đánh thức **1 thread NGẪU NHIÊN** trong số TẤT CẢ thread đang `wait()`, có thể **đánh thức nhầm loại** (VD: `take()` gọi xong `notifyAll()` nhưng nếu chỉ `notify()`, có thể đánh thức nhầm 1 consumer khác đang chờ — trong khi consumer đó vẫn thấy `hasItem = false` nên lại `wait()` tiếp, còn producer đang cần được đánh thức thì **không được gọi tới**, có nguy cơ "bỏ lỡ" tín hiệu). `notifyAll()` đánh thức **TẤT CẢ**, mỗi thread tự kiểm tra lại điều kiện của mình trong `while` — an toàn tuyệt đối dù có nhiều loại thread chờ khác nhau.
- **Vì sao `wait()` trong `while` chứ không phải `if`:** giả sử `hasItem = false`, cả `Consumer-1` và `Consumer-2` đều đang `wait()`. Producer `put()` xong, `notifyAll()` — CẢ 2 consumer đều được đánh thức, nhưng chỉ **1 trong 2** giành được lock trước (JVM không đảm bảo ai trước ai sau) và lấy được `item`. Consumer còn lại, khi tới lượt chạy tiếp, **PHẢI kiểm tra lại `while (!hasItem)`** — lúc này `hasItem` đã lại là `false` (item vừa bị consumer kia lấy mất) → tiếp tục `wait()` đúng đắn. Nếu dùng `if` thay vì `while`, consumer thứ 2 sẽ **cứ thế đi tiếp** dù `hasItem` đã `false`, đọc phải `item = null` sai hoặc dữ liệu cũ — bug nghiêm trọng.
- Đây chính là **cơ chế nguyên thủy** mà `java.util.concurrent.SynchronousQueue`/`BlockingQueue` (Module 05.2) đã đóng gói sẵn — viết tay bằng `wait`/`notifyAll` giúp hiểu **bản chất bên dưới** trước khi dùng bản có sẵn tiện lợi hơn nhiều của JDK.

---

## Bài 6 — Bài toán tổng hợp: phòng vé (chuẩn bị capstone Flash-Sale)

### Đề
`TicketBooth` với `int available = 100`. `boolean book()`: còn vé giảm 1 trả `true`, hết trả `false`. 200 thread cùng `book()`, đếm số `true`. Chứng minh KHÔNG đồng bộ → oversold (> 100). Sửa bằng `synchronized`, chứng minh không bao giờ vượt 100.

### Phân tích

Đây là ví dụ **"check-then-act"** kinh điển — `book()` gồm 2 bước: **check** (`available > 0`) rồi **act** (`available--`) — nếu không đồng bộ, 2 thread có thể **CÙNG check thấy `available > 0`** (VD: cả 2 cùng thấy `available = 1`), rồi **CẢ 2 CÙNG trừ đi**, dẫn tới `available` có thể xuống **ÂM**, và **CẢ 2 THREAD đều nhận `true`** dù thực chất chỉ còn đúng 1 vé — đây chính là hiện tượng **oversold** (bán vượt quá số lượng thực có), 1 bug nghiệp vụ tài chính nghiêm trọng trong thực tế.

### Lời giải

```java
package baitap.bai6;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    static class TicketBoothUnsafe {
        private int available = 100;

        public boolean book() {
            if (available > 0) {      // CHECK
                // ⚠️ KHOẢNG HỞ - thread khác có thể chen vào NGAY GIỮA 2 dòng này
                available--;           // ACT
                return true;
            }
            return false;
        }

        public int getAvailable() { return available; }
    }

    static class TicketBoothSafe {
        private int available = 100;

        public synchronized boolean book() {
            if (available > 0) {  // check-và-act giờ là 1 KHỐI NGUYÊN TỬ - không thread nào chen được vào giữa
                available--;
                return true;
            }
            return false;
        }

        public synchronized int getAvailable() { return available; }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== KHÔNG đồng bộ =====");
        TicketBoothUnsafe unsafeBooth = new TicketBoothUnsafe();
        AtomicInteger unsafeSuccessCount = new AtomicInteger(0); // dùng AtomicInteger để ĐẾM đúng,
                                                                    // tránh chính việc ĐẾM lại gây race condition khác!
        runConcurrentBookings(unsafeBooth::book, unsafeSuccessCount, 200);
        System.out.println("Số vé bán được (đếm true): " + unsafeSuccessCount.get());
        System.out.println("available còn lại (biến nội bộ): " + unsafeBooth.getAvailable());
        System.out.println(unsafeSuccessCount.get() > 100
                ? "-> OVERSOLD! Bán vượt quá 100 vé thực có!"
                : "-> Lần này không oversold (vẫn có RỦI RO xảy ra ở lần chạy khác - race condition không cố định)");

        System.out.println("\n===== CÓ đồng bộ (chạy 5 lần) =====");
        for (int run = 1; run <= 5; run++) {
            TicketBoothSafe safeBooth = new TicketBoothSafe();
            AtomicInteger safeSuccessCount = new AtomicInteger(0);
            runConcurrentBookings(safeBooth::book, safeSuccessCount, 200);
            System.out.println("Lần " + run + ": bán được " + safeSuccessCount.get()
                    + ", available còn lại = " + safeBooth.getAvailable()
                    + (safeSuccessCount.get() <= 100 ? " (ĐÚNG - không vượt 100)" : " (SAI!)"));
        }
    }

    interface BookAction { boolean book(); }

    static void runConcurrentBookings(BookAction action, AtomicInteger successCount, int threadCount) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                if (action.book()) {
                    successCount.incrementAndGet(); // AtomicInteger.incrementAndGet() - thao tác NGUYÊN TỬ có sẵn
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
    }
}
```

**Kết quả tiêu biểu:**
```
===== KHÔNG đồng bộ =====
Số vé bán được (đếm true): 103
available còn lại (biến nội bộ): -3
-> OVERSOLD! Bán vượt quá 100 vé thực có!

===== CÓ đồng bộ (chạy 5 lần) =====
Lần 1: bán được 100, available còn lại = 0 (ĐÚNG - không vượt 100)
Lần 2: bán được 100, available còn lại = 0 (ĐÚNG - không vượt 100)
Lần 3: bán được 100, available còn lại = 0 (ĐÚNG - không vượt 100)
Lần 4: bán được 100, available còn lại = 0 (ĐÚNG - không vượt 100)
Lần 5: bán được 100, available còn lại = 0 (ĐÚNG - không vượt 100)
```

### Giải thích

- **`available = -3` (âm)** trong bản không đồng bộ là bằng chứng rõ ràng nhất của **check-then-act race condition**: nhiều thread cùng thấy `available > 0` tại "khoảnh khắc" gần như đồng thời (dù thực tế giá trị đã gần hết), tất cả đều tiến hành `available--`, khiến biến giảm **xuống dưới 0** dù logic `if (available > 0)` lẽ ra phải ngăn chặn điều này.
- `synchronized boolean book()` biến **TOÀN BỘ khối "check rồi act"** thành **1 đơn vị nguyên tử không thể chia cắt** — thread khác **không thể** chen vào giữa lúc 1 thread đang kiểm tra và trừ vé — đảm bảo tuyệt đối `available` không bao giờ âm, tổng số `true` không bao giờ vượt 100.
- **Dùng `AtomicInteger` để đếm `successCount`** (thay vì `int successCount++` thường) là chi tiết **DỄ BỊ BỎ SÓT** nhưng quan trọng: nếu đếm bằng `int` thường không đồng bộ, **CHÍNH VIỆC ĐẾM** cũng có race condition riêng (y hệt Bài 1) — dẫn tới đếm sai ngay cả khi `book()` đã đúng hoàn toàn, khiến kết quả đo đạc **không đáng tin cậy**.

### Ghi chú: giới hạn của lời giải này

Đây là lời giải **Java thuần, chỉ đúng trong phạm vi 1 JVM process (1 instance ứng dụng)** — `synchronized` chỉ đồng bộ hóa các thread **TRONG CÙNG 1 process**. Trong thực tế Backend production, ứng dụng thường chạy **NHIỀU instance song song** (horizontal scaling — liên hệ Module 22 System Design) để chịu tải cao — lúc đó, `TicketBoothSafe` trên **Instance A** và **Instance B** là **2 object HOÀN TOÀN RIÊNG BIỆT trong bộ nhớ**, `synchronized` của Instance A **không hề biết** Instance B đang làm gì — oversold **HOÀN TOÀN CÓ THỂ XẢY RA LẠI** ở cấp độ liên-instance, dù mỗi instance riêng lẻ vẫn đúng.

**Giải pháp cho môi trường nhiều instance** (sẽ học chi tiết ở Module 14/18):
- **Pessimistic Locking ở database** (`SELECT ... FOR UPDATE`) — biến database thành "trọng tài" chung cho mọi instance.
- **Optimistic Locking** (`@Version` trong JPA) — kiểm tra version khi update, retry nếu conflict.
- **Distributed Lock** (VD: Redis `SETNX`/Redlock) — tạo 1 "khóa chung" mà mọi instance đều phải tuân theo, vượt ra khỏi phạm vi 1 JVM.

---

*Đây là lời giải cho toàn bộ Phần B của Module 12. Đây là module CUỐI CÙNG trong nhóm "Java Core/OOP thuần" (03-12). Tiếp theo: Module 13 — Concurrency Utilities.*
