# Lời giải đầy đủ — Module 08: Design Patterns

> Nguồn đề: `16 design patterns/16-design-patterns.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Singleton: chọn cách và chứng minh

### Đề
`AppLogger` singleton (holder idiom hoặc `enum`). `main` 100 thread cùng `getInstance()`, gom vào `Set` theo `identityHashCode`, khẳng định `size() == 1`. Bình luận vì sao trong Spring không viết class này.

### Phân tích

**Chọn `enum` singleton** — đây là cách được Joshua Bloch (Effective Java) khuyến nghị **AN TOÀN NHẤT**: JVM đảm bảo **CHỈ 1** instance của mỗi hằng số `enum` được tạo, kể cả khi có nhiều thread cùng truy cập lần đầu (class loading trong JVM vốn đã thread-safe), và **miễn nhiễm** với tấn công qua Reflection/Serialization (2 cách có thể "phá" singleton kiểu class thường).

### Lời giải

```java
package baitap.bai1;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum AppLogger {
    INSTANCE; // JVM đảm bảo CHỈ 1 instance, thread-safe TỰ NHIÊN nhờ cơ chế class loading

    public void log(String message) {
        System.out.println("[" + LocalDateTime.now() + "] " + message);
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 100;
        Set<Integer> identityHashes = Collections.synchronizedSet(new HashSet<>());
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                AppLogger logger = AppLogger.INSTANCE; // gọi qua "field" của enum - tương đương getInstance()
                identityHashes.add(System.identityHashCode(logger));
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        System.out.println("Số identityHashCode DUY NHẤT quan sát được: " + identityHashes.size());
        System.out.println(identityHashes.size() == 1 ? "ĐÚNG - chỉ 1 instance duy nhất" : "SAI!");

        AppLogger.INSTANCE.log("Ứng dụng khởi động");
    }
}
```

**Kết quả chạy:**
```
Số identityHashCode DUY NHẤT quan sát được: 1
ĐÚNG - chỉ 1 instance duy nhất
[2026-01-01T10:30:00.123] Ứng dụng khởi động
```

### Giải thích

- **So với Holder Idiom** (`private static class Holder { static final AppLogger INSTANCE = new AppLogger(); }`) — cả 2 đều thread-safe và lazy (chỉ khởi tạo khi thực sự dùng lần đầu), nhưng `enum` **thêm 2 lớp bảo vệ mà Holder Idiom KHÔNG có**: (1) chống **Reflection Attack** — `Constructor.setAccessible(true)` để gọi lại constructor `private` KHÔNG hoạt động được với `enum` (JVM chặn thẳng ở tầng ngôn ngữ); (2) chống **Serialization Attack** — deserialize 1 class Singleton thường (nếu implement `Serializable`) mặc định tạo ra **instance MỚI**, phá vỡ tính chất "chỉ 1 instance" trừ khi tự viết `readResolve()`; `enum` tự động xử lý đúng, không cần thêm code gì.

### Vì sao trong dự án Spring KHÔNG viết class này

Spring IoC Container (Module 12) **đã tự quản lý vòng đời Singleton cho MỌI Bean theo mặc định** (`@Component`/`@Service` mặc định scope `singleton`) — Spring tự đảm bảo chỉ tạo **1 instance duy nhất** cho mỗi Bean trong `ApplicationContext`, và cung cấp instance đó qua **Dependency Injection** thay vì gọi `getInstance()` thủ công. Tự viết Singleton pattern (dù bằng `enum` hay Holder Idiom) trong code Spring là **dư thừa và có hại**: nó tạo ra **1 singleton "cứng" độc lập với Spring Container**, làm **mất khả năng thay thế bằng mock khi test** (Module 17 — không thể "tiêm" mock vào chỗ đang gọi `AppLogger.INSTANCE` trực tiếp), và làm **rối loạn vòng đời Bean** (Spring không biết gì về instance được tạo bên ngoài nó).

---

## Bài 2 — Builder bất biến + required + validate

### Đề
`HttpRequestConfig`: `url` bắt buộc, các field khác có mặc định. Builder: `url` qua `HttpRequestConfig.to(url)`, chaining cho các field khác. `build()` validate. Chứng minh bất biến sau `build()`.

### Phân tích

**Builder Pattern** giải quyết vấn đề "quá nhiều tham số tùy chọn" mà constructor không xử lý gọn được — `url` là tham số **bắt buộc DUY NHẤT**, nên đặt ngay ở **static factory method** (`to(url)`) thay vì `builder()` rỗng, buộc caller **phải cung cấp `url` NGAY TỪ ĐẦU** (compiler-enforced required field — kỹ thuật hay bị bỏ qua nhưng rất hiệu quả).

### Lời giải

```java
package baitap.bai2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpRequestConfig {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final int timeoutMs;
    private final int retries;

    private HttpRequestConfig(Builder b) {
        this.url = b.url;
        this.method = b.method;
        this.headers = Collections.unmodifiableMap(new HashMap<>(b.headers)); // BẤT BIẾN - copy + wrap
        this.timeoutMs = b.timeoutMs;
        this.retries = b.retries;
    }

    public String getUrl() { return url; }
    public String getMethod() { return method; }
    public Map<String, String> getHeaders() { return headers; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getRetries() { return retries; }

    // Static factory - "url" BẮT BUỘC ngay từ điểm khởi đầu, compiler ép phải truyền
    public static Builder to(String url) {
        return new Builder(url);
    }

    public static class Builder {
        private final String url;
        private String method = "GET";
        private Map<String, String> headers = new HashMap<>();
        private int timeoutMs = 5000;
        private int retries = 0;

        private Builder(String url) {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("url không được để trống");
            }
            this.url = url;
        }

        public Builder method(String method) { this.method = method; return this; }
        public Builder header(String key, String value) { this.headers.put(key, value); return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }
        public Builder retries(int retries) { this.retries = retries; return this; }

        public HttpRequestConfig build() {
            if (timeoutMs <= 0) {
                throw new IllegalStateException("timeoutMs phải > 0, nhận: " + timeoutMs);
            }
            if (retries < 0) {
                throw new IllegalStateException("retries không được âm, nhận: " + retries);
            }
            return new HttpRequestConfig(this);
        }
    }

    public static void main(String[] args) {
        HttpRequestConfig config = HttpRequestConfig.to("https://api.example.com/orders")
                .method("POST")
                .header("Authorization", "Bearer token123")
                .header("Content-Type", "application/json")
                .timeoutMs(3000)
                .retries(2)
                .build();

        System.out.println("url: " + config.getUrl());
        System.out.println("method: " + config.getMethod());
        System.out.println("headers: " + config.getHeaders());
        System.out.println("timeoutMs: " + config.getTimeoutMs());
        System.out.println("retries: " + config.getRetries());

        // Chứng minh BẤT BIẾN - thử sửa headers sau khi build()
        try {
            config.getHeaders().put("X-Hacked", "true");
        } catch (UnsupportedOperationException e) {
            System.out.println("\nThử sửa headers -> UnsupportedOperationException (đúng như kỳ vọng - bất biến)");
        }

        // Chứng minh validate
        try {
            HttpRequestConfig.to("https://x.com").timeoutMs(-1).build();
        } catch (IllegalStateException e) {
            System.out.println("timeoutMs=-1 -> " + e.getMessage());
        }

        try {
            HttpRequestConfig.to(null);
        } catch (IllegalArgumentException e) {
            System.out.println("url=null -> " + e.getMessage());
        }
    }
}
```

**Kết quả chạy:**
```
url: https://api.example.com/orders
method: POST
headers: {Content-Type=application/json, Authorization=Bearer token123}
timeoutMs: 3000
retries: 2

Thử sửa headers -> UnsupportedOperationException (đúng như kỳ vọng - bất biến)
timeoutMs=-1 -> timeoutMs phải > 0, nhận: -1
url=null -> url không được để trống
```

### Giải thích

- **`Collections.unmodifiableMap(new HashMap<>(b.headers))`** — 2 lớp bảo vệ: `new HashMap<>(b.headers)` tạo **bản sao độc lập** (defensive copy, liên hệ bài học Module 02.1 Bài 4 — nếu chỉ `Collections.unmodifiableMap(b.headers)` mà KHÔNG copy, `Builder` vẫn giữ tham chiếu tới `Map` gốc, có thể **tiếp tục sửa nó SAU KHI `build()`**, làm "config bất biến" thực chất vẫn bị thay đổi ngầm qua backdoor này); `unmodifiableMap` chặn caller sửa qua `getHeaders()` (ném `UnsupportedOperationException` khi cố `put`/`remove`).
- `HttpRequestConfig.to(url)` khiến việc **quên truyền `url`** trở thành **LỖI COMPILE** (không có cách nào gọi `new Builder()` rỗng rồi quên `url`) — mạnh hơn nhiều so với validate `url == null` chỉ phát hiện lúc **runtime** ở `build()`.

---

## Bài 3 — Strategy bằng `Map<String, Supplier>` thay `switch`

### Đề
`NotificationFactory.create(String type)` đang `switch` cho EMAIL/SMS/PUSH. Refactor sang `Map<String, Supplier<NotificationSender>>` đăng ký sẵn; thêm `SLACK` chỉ 1 dòng đăng ký, không sửa `create`. Test type không tồn tại.

### Phân tích

Đây là biến thể trực tiếp của bài học **OCP qua registry** đã làm ở Module 06 Bài 2 (`DiscountCalculator`), áp dụng cho **Strategy Pattern**: mỗi `NotificationSender` là 1 "chiến lược" gửi thông báo khác nhau; thay vì `switch` liệt kê hết các case (vi phạm OCP), dùng `Map<String, Supplier<...>>` để **tra cứu động**.

### Lời giải

```java
package baitap.bai3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

interface NotificationSender {
    void send(String message);
}

class EmailSender implements NotificationSender {
    @Override public void send(String message) { System.out.println("[Email] " + message); }
}
class SmsSender implements NotificationSender {
    @Override public void send(String message) { System.out.println("[SMS] " + message); }
}
class PushSender implements NotificationSender {
    @Override public void send(String message) { System.out.println("[Push] " + message); }
}
// Thêm SAU, KHÔNG sửa NotificationFactory
class SlackSender implements NotificationSender {
    @Override public void send(String message) { System.out.println("[Slack] " + message); }
}

class NotificationFactory {
    private static final Map<String, Supplier<NotificationSender>> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put("EMAIL", EmailSender::new);
        REGISTRY.put("SMS", SmsSender::new);
        REGISTRY.put("PUSH", PushSender::new);
        REGISTRY.put("SLACK", SlackSender::new); // THÊM loại mới CHỈ 1 DÒNG - "create" bên dưới KHÔNG đổi
    }

    public static NotificationSender create(String type) {
        Supplier<NotificationSender> supplier = REGISTRY.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException("Loại thông báo không tồn tại: " + type);
        }
        return supplier.get();
    }
}

public class Main {
    public static void main(String[] args) {
        NotificationFactory.create("EMAIL").send("Đơn hàng đã xác nhận");
        NotificationFactory.create("SLACK").send("Deploy production thành công");

        try {
            NotificationFactory.create("TELEGRAM");
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi (đúng như kỳ vọng): " + e.getMessage());
        }
    }
}
```

**Kết quả chạy:**
```
[Email] Đơn hàng đã xác nhận
[Slack] Deploy production thành công
Lỗi (đúng như kỳ vọng): Loại thông báo không tồn tại: TELEGRAM
```

### Giải thích

- `Supplier<NotificationSender>` (không phải `NotificationSender` trực tiếp) đảm bảo **mỗi lần `create()` tạo INSTANCE MỚI** (`EmailSender::new` chỉ được **gọi thực sự** khi `.get()` được invoke) — nếu lưu trực tiếp `new EmailSender()` vào Map, TẤT CẢ lời gọi `create("EMAIL")` sẽ trả về **CÙNG 1 instance dùng chung**, có thể gây vấn đề nếu `NotificationSender` sau này có state riêng theo từng lần gửi.
- Đây chính xác là kỹ thuật Spring dùng ngầm khi quét `@Component` — mỗi implementation tự "đăng ký" vào container qua annotation, không có nơi nào phải viết `switch`/`if-else` liệt kê thủ công từng loại.

---

## Bài 4 — Decorator: đo lường + retry cho một `DataSource`

### Đề
`interface PriceFeed`, `RemotePriceFeed` (chậm, thỉnh thoảng ném). 2 decorator: `TimingPriceFeed`, `RetryingPriceFeed`. Lắp `Timing(Retrying(Remote))`, giải thích đổi thứ tự cho ý nghĩa khác.

### Phân tích

**Decorator Pattern** "bọc" 1 object bằng 1 (hoặc nhiều) lớp khác **cùng implement chung interface**, mỗi lớp bọc thêm **1 hành vi** mà không sửa object gốc. Thứ tự bọc **CÓ Ý NGHĨA QUAN TRỌNG**: `Timing(Retrying(Remote))` đo **TỔNG thời gian bao gồm CẢ các lần retry**; `Retrying(Timing(Remote))` sẽ đo riêng **từng lần gọi** (và retry sẽ lặp lại cả phần đo thời gian).

### Lời giải

```java
package baitap.bai4;

import java.math.BigDecimal;
import java.util.Random;

interface PriceFeed {
    BigDecimal get(String symbol);
}

class RemotePriceFeed implements PriceFeed {
    private final Random random = new Random();

    @Override
    public BigDecimal get(String symbol) {
        try { Thread.sleep(100 + random.nextInt(200)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (random.nextInt(10) < 3) { // 30% khả năng lỗi - giả lập service không ổn định
            throw new RuntimeException("Lỗi kết nối tới price service");
        }
        return BigDecimal.valueOf(100 + random.nextInt(900));
    }
}

class TimingPriceFeed implements PriceFeed {
    private final PriceFeed delegate;
    public TimingPriceFeed(PriceFeed delegate) { this.delegate = delegate; }

    @Override
    public BigDecimal get(String symbol) {
        long start = System.currentTimeMillis();
        try {
            return delegate.get(symbol);
        } finally {
            System.out.println("[Timing] get(" + symbol + ") tốn " + (System.currentTimeMillis() - start) + "ms");
        }
    }
}

class RetryingPriceFeed implements PriceFeed {
    private final PriceFeed delegate;
    private final int maxAttempts;
    public RetryingPriceFeed(PriceFeed delegate) { this(delegate, 3); }
    public RetryingPriceFeed(PriceFeed delegate, int maxAttempts) { this.delegate = delegate; this.maxAttempts = maxAttempts; }

    @Override
    public BigDecimal get(String symbol) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return delegate.get(symbol);
            } catch (RuntimeException e) {
                lastError = e;
                System.out.println("[Retry] Lần " + attempt + " thất bại: " + e.getMessage());
            }
        }
        throw lastError;
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("===== Timing(Retrying(Remote)) - đo TỔNG thời gian gồm cả retry =====");
        PriceFeed feed1 = new TimingPriceFeed(new RetryingPriceFeed(new RemotePriceFeed()));
        try {
            System.out.println("Giá AAPL: " + feed1.get("AAPL"));
        } catch (RuntimeException e) {
            System.out.println("Thất bại sau hết số lần retry: " + e.getMessage());
        }

        System.out.println("\n===== Retrying(Timing(Remote)) - đo RIÊNG từng lần gọi =====");
        PriceFeed feed2 = new RetryingPriceFeed(new TimingPriceFeed(new RemotePriceFeed()));
        try {
            System.out.println("Giá GOOG: " + feed2.get("GOOG"));
        } catch (RuntimeException e) {
            System.out.println("Thất bại sau hết số lần retry: " + e.getMessage());
        }
    }
}
```

**Kết quả tiêu biểu:**
```
===== Timing(Retrying(Remote)) - đo TỔNG thời gian gồm cả retry =====
[Retry] Lần 1 thất bại: Lỗi kết nối tới price service
[Timing] get(AAPL) tốn 385ms
Giá AAPL: 542

===== Retrying(Timing(Remote)) - đo RIÊNG từng lần gọi =====
[Timing] get(GOOG) tốn 145ms
[Retry] Lần 1 thất bại: Lỗi kết nối tới price service
[Timing] get(GOOG) tốn 210ms
Giá GOOG: 733
```

### Giải thích vì sao thứ tự thay đổi ý nghĩa

- **`Timing(Retrying(Remote))`:** `TimingPriceFeed` ở **NGOÀI CÙNG** — nó chỉ đo thời gian **1 LẦN**, bao trọn **TOÀN BỘ** quá trình bên trong (kể cả các lần retry) — con số đo được phản ánh **"user phải chờ tổng cộng bao lâu"** (metric hướng tới trải nghiệm người dùng cuối/latency thực tế).
- **`Retrying(Timing(Remote))`:** `TimingPriceFeed` ở **BÊN TRONG**, bị `RetryingPriceFeed` **gọi lại nhiều lần** — mỗi lần retry đều in ra **1 dòng đo thời gian RIÊNG** cho từng lần gọi cụ thể — phù hợp khi muốn **phân tích CHI TIẾT từng lần gọi remote service** (VD: debug xem lần nào chậm, có pattern gì không), không quan tâm tổng thời gian user phải chờ.
- **Nguyên tắc chọn thứ tự bọc:** decorator "đo lường tổng trải nghiệm" (Timing hướng UX) nên đặt **NGOÀI CÙNG**; decorator "xử lý lỗi/retry ở tầng thấp" nên đặt **GẦN object gốc** — đây là bài học thiết kế quan trọng khi xây dựng pipeline nhiều decorator (Interceptor Chain, Filter Chain — sẽ gặp lại ở Module 12 Spring AOP, Module 20 Spring Security Filter Chain).

---

## Bài 5 — State cho vòng đời đơn hàng

### Đề
`Order` 4 trạng thái (`NEW/PAID/SHIPPED/CANCELLED`), hành động `pay()/ship()/cancel()`. State pattern — mỗi trạng thái 1 class implement `OrderState`. Hành động sai ném `IllegalStateException`. So sánh với `switch (status)`.

### Phân tích

**State Pattern** thay thế `switch (status) { case NEW: ...; case PAID: ...; }` (lặp lại KHẮP nơi mỗi khi cần xử lý theo trạng thái) bằng cách **ủy quyền hành vi cho chính object trạng thái** — mỗi trạng thái "tự biết" nó cho phép hành động gì, chuyển sang trạng thái nào tiếp theo. `Order` chỉ giữ 1 tham chiếu `OrderState currentState`, gọi thẳng `currentState.pay(this)` mà không cần biết trạng thái cụ thể là gì.

### Lời giải

```java
package baitap.bai5;

public class Main {

    interface OrderState {
        default OrderState pay(Order order) { throw invalidTransition("pay", order); }
        default OrderState ship(Order order) { throw invalidTransition("ship", order); }
        default OrderState cancel(Order order) { throw invalidTransition("cancel", order); }
        String name();

        private static IllegalStateException invalidTransition(String action, Order order) {
            return new IllegalStateException(
                    "Không thể '" + action + "' đơn hàng #" + order.getId() + " đang ở trạng thái " + order.getState().name());
        }
    }

    static class NewState implements OrderState {
        @Override public OrderState pay(Order order) { System.out.println("Đơn #" + order.getId() + ": NEW -> PAID"); return new PaidState(); }
        @Override public OrderState cancel(Order order) { System.out.println("Đơn #" + order.getId() + ": NEW -> CANCELLED"); return new CancelledState(); }
        @Override public String name() { return "NEW"; }
    }

    static class PaidState implements OrderState {
        @Override public OrderState ship(Order order) { System.out.println("Đơn #" + order.getId() + ": PAID -> SHIPPED"); return new ShippedState(); }
        @Override public OrderState cancel(Order order) { System.out.println("Đơn #" + order.getId() + ": PAID -> CANCELLED (hoàn tiền)"); return new CancelledState(); }
        @Override public String name() { return "PAID"; }
    }

    static class ShippedState implements OrderState {
        @Override public String name() { return "SHIPPED"; } // KHÔNG override gì - mọi hành động đều throw (trạng thái cuối)
    }

    static class CancelledState implements OrderState {
        @Override public String name() { return "CANCELLED"; } // trạng thái cuối - không hành động nào hợp lệ
    }

    static class Order {
        private final long id;
        private OrderState state = new NewState();

        Order(long id) { this.id = id; }

        void pay() { state = state.pay(this); }
        void ship() { state = state.ship(this); }
        void cancel() { state = state.cancel(this); }

        long getId() { return id; }
        OrderState getState() { return state; }
    }

    public static void main(String[] args) {
        System.out.println("===== Luồng HỢP LỆ: NEW -> PAID -> SHIPPED =====");
        Order order1 = new Order(1);
        order1.pay();
        order1.ship();
        System.out.println("Trạng thái cuối: " + order1.getState().name());

        System.out.println("\n===== Luồng SAI: NEW -> ship() trực tiếp =====");
        Order order2 = new Order(2);
        try {
            order2.ship(); // KHÔNG hợp lệ - đơn chưa thanh toán mà đòi ship
        } catch (IllegalStateException e) {
            System.out.println("Lỗi (đúng như kỳ vọng): " + e.getMessage());
        }
    }
}
```

**Kết quả chạy:**
```
===== Luồng HỢP LỆ: NEW -> PAID -> SHIPPED =====
Đơn #1: NEW -> PAID
Đơn #1: PAID -> SHIPPED
Trạng thái cuối: SHIPPED

===== Luồng SAI: NEW -> ship() trực tiếp =====
Lỗi (đúng như kỳ vọng): Không thể 'ship' đơn hàng #2 đang ở trạng thái NEW
```

### So sánh ngắn với bản `switch (status)`

```java
// ❌ Bản switch - LOGIC CHUYỂN TRẠNG THÁI nằm RẢI RÁC trong TỪNG method của Order
void ship() {
    switch (status) {
        case PAID -> { status = OrderStatus.SHIPPED; System.out.println("..."); }
        case NEW, SHIPPED, CANCELLED -> throw new IllegalStateException("Không thể ship khi đang " + status);
    }
}
// Mỗi hành động (pay/ship/cancel) đều cần switch RIÊNG, LIỆT KÊ LẠI toàn bộ trạng thái mỗi lần
// Thêm trạng thái mới (VD: REFUNDED) -> phải SỬA switch ở TẤT CẢ method pay/ship/cancel - dễ SÓT
```

- **State Pattern:** thêm trạng thái mới chỉ cần **1 class mới** implement `OrderState`, override đúng những hành động **hợp lệ** cho trạng thái đó — các hành động không hợp lệ **tự động** dùng default method ném exception, **không cần sửa** `Order`/các state khác.
- **`switch (status)`:** mỗi hành động (`pay`/`ship`/`cancel`) đều cần switch riêng liệt kê **TẤT CẢ** trạng thái — thêm 1 trạng thái mới buộc phải **rà soát và sửa MỌI switch** rải rác khắp nơi, dễ **bỏ sót** dẫn tới bug logic chuyển trạng thái sai.

---

## Bài 6 — Bài tổng hợp: pipeline xử lý request kết hợp nhiều pattern

### Đề
Chain of Responsibility (`StockHandler→PaymentHandler→FraudHandler`) + Strategy (`ShippingPolicy`) + Observer (`OrderPlaced` cho `EmailListener`/`InventoryListener`, `CopyOnWriteArrayList`, có `unregister`) + Decorator bọc `PaymentHandler` đo thời gian.

### Lời giải

```java
package baitap.bai6;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// ===== PATTERN: Chain of Responsibility =====
// Mỗi Handler xử lý 1 bước, có thể CHẶN và DỪNG chuỗi
interface OrderHandler {
    boolean handle(OrderContext ctx); // trả false = CHẶN, dừng chuỗi ngay
}

class OrderContext {
    String orderId;
    BigDecimal amount;
    boolean fraudSuspect;
    OrderContext(String orderId, BigDecimal amount, boolean fraudSuspect) {
        this.orderId = orderId; this.amount = amount; this.fraudSuspect = fraudSuspect;
    }
}

class StockHandler implements OrderHandler {
    @Override public boolean handle(OrderContext ctx) {
        System.out.println("[StockHandler] Kiểm tra tồn kho cho " + ctx.orderId + " -> OK");
        return true;
    }
}

class PaymentHandler implements OrderHandler {
    @Override public boolean handle(OrderContext ctx) {
        System.out.println("[PaymentHandler] Xử lý thanh toán " + ctx.amount + " cho " + ctx.orderId);
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (ctx.amount.compareTo(new BigDecimal("10000000")) > 0) {
            System.out.println("[PaymentHandler] BỊ CHẶN - số tiền vượt hạn mức thanh toán");
            return false; // CHẶN chuỗi
        }
        return true;
    }
}

class FraudHandler implements OrderHandler {
    @Override public boolean handle(OrderContext ctx) {
        System.out.println("[FraudHandler] Kiểm tra gian lận cho " + ctx.orderId);
        if (ctx.fraudSuspect) {
            System.out.println("[FraudHandler] BỊ CHẶN - nghi ngờ gian lận");
            return false;
        }
        return true;
    }
}

// ===== PATTERN: Decorator - bọc PaymentHandler đo thời gian, KHÔNG sửa PaymentHandler gốc =====
class TimingHandlerDecorator implements OrderHandler {
    private final OrderHandler delegate;
    private final String label;
    TimingHandlerDecorator(OrderHandler delegate, String label) { this.delegate = delegate; this.label = label; }

    @Override
    public boolean handle(OrderContext ctx) {
        long start = System.currentTimeMillis();
        boolean result = delegate.handle(ctx);
        System.out.println("[Timing] " + label + " tốn " + (System.currentTimeMillis() - start) + "ms");
        return result;
    }
}

// ===== PATTERN: Strategy - chọn cách tính phí ship =====
interface ShippingPolicy {
    BigDecimal calculateFee(BigDecimal orderAmount);
}
class StandardShipping implements ShippingPolicy {
    @Override public BigDecimal calculateFee(BigDecimal orderAmount) { return new BigDecimal("30000"); }
}
class ExpressShipping implements ShippingPolicy {
    @Override public BigDecimal calculateFee(BigDecimal orderAmount) { return new BigDecimal("80000"); }
}

// ===== PATTERN: Observer =====
interface OrderPlacedListener {
    void onOrderPlaced(OrderContext ctx);
}
class EmailListener implements OrderPlacedListener {
    @Override public void onOrderPlaced(OrderContext ctx) { System.out.println("[EmailListener] Gửi email xác nhận đơn " + ctx.orderId); }
}
class InventoryListener implements OrderPlacedListener {
    @Override public void onOrderPlaced(OrderContext ctx) { System.out.println("[InventoryListener] Trừ tồn kho cho đơn " + ctx.orderId); }
}

class OrderEventPublisher {
    private final List<OrderPlacedListener> listeners = new CopyOnWriteArrayList<>(); // an toàn khi duyệt + sửa đồng thời

    void register(OrderPlacedListener listener) { listeners.add(listener); }
    void unregister(OrderPlacedListener listener) { listeners.remove(listener); }

    void publish(OrderContext ctx) {
        for (OrderPlacedListener listener : listeners) {
            listener.onOrderPlaced(ctx);
        }
    }
}

// ===== Pipeline tổng hợp =====
class OrderPipeline {
    private final List<OrderHandler> handlers;
    private final OrderEventPublisher publisher;

    OrderPipeline(List<OrderHandler> handlers, OrderEventPublisher publisher) {
        this.handlers = handlers;
        this.publisher = publisher;
    }

    void process(OrderContext ctx, ShippingPolicy shippingPolicy) {
        System.out.println("\n===== Xử lý đơn " + ctx.orderId + " =====");
        for (OrderHandler handler : handlers) {
            if (!handler.handle(ctx)) {
                System.out.println(">>> Đơn " + ctx.orderId + " BỊ TỪ CHỐI - dừng chuỗi, KHÔNG phát sự kiện");
                return; // DỪNG chuỗi - observer KHÔNG được gọi
            }
        }

        BigDecimal fee = shippingPolicy.calculateFee(ctx.amount); // Strategy
        System.out.println("Phí vận chuyển: " + fee);

        publisher.publish(ctx); // Observer - CHỈ chạy nếu qua hết chuỗi
    }
}

public class Main {
    public static void main(String[] args) {
        OrderEventPublisher publisher = new OrderEventPublisher();
        EmailListener emailListener = new EmailListener();
        InventoryListener inventoryListener = new InventoryListener();
        publisher.register(emailListener);
        publisher.register(inventoryListener);

        List<OrderHandler> handlers = List.of(
                new StockHandler(),
                new TimingHandlerDecorator(new PaymentHandler(), "PaymentHandler"), // Decorator bọc PaymentHandler
                new FraudHandler()
        );

        OrderPipeline pipeline = new OrderPipeline(handlers, publisher);

        // (1) Đơn HỢP LỆ - đi hết chuỗi, kích hoạt observer, phí Express
        OrderContext validOrder = new OrderContext("ORD-001", new BigDecimal("500000"), false);
        pipeline.process(validOrder, new ExpressShipping());

        // (2) Đơn BỊ CHẶN ở PaymentHandler (vượt hạn mức) - observer KHÔNG chạy
        OrderContext blockedOrder = new OrderContext("ORD-002", new BigDecimal("20000000"), false);
        pipeline.process(blockedOrder, new StandardShipping());

        // Minh họa unregister
        publisher.unregister(inventoryListener);
        System.out.println("\n(Đã unregister InventoryListener)");
        OrderContext order3 = new OrderContext("ORD-003", new BigDecimal("200000"), false);
        pipeline.process(order3, new StandardShipping());
    }
}
```

**Kết quả chạy:**
```
===== Xử lý đơn ORD-001 =====
[StockHandler] Kiểm tra tồn kho cho ORD-001 -> OK
[PaymentHandler] Xử lý thanh toán 500000 cho ORD-001
[Timing] PaymentHandler tốn 152ms
[FraudHandler] Kiểm tra gian lận cho ORD-001
Phí vận chuyển: 80000
[EmailListener] Gửi email xác nhận đơn ORD-001
[InventoryListener] Trừ tồn kho cho đơn ORD-001

===== Xử lý đơn ORD-002 =====
[StockHandler] Kiểm tra tồn kho cho ORD-002 -> OK
[PaymentHandler] Xử lý thanh toán 20000000 cho ORD-002
[PaymentHandler] BỊ CHẶN - số tiền vượt hạn mức thanh toán
[Timing] PaymentHandler tốn 151ms
>>> Đơn ORD-002 BỊ TỪ CHỐI - dừng chuỗi, KHÔNG phát sự kiện

(Đã unregister InventoryListener)

===== Xử lý đơn ORD-003 =====
[StockHandler] Kiểm tra tồn kho cho ORD-003 -> OK
[PaymentHandler] Xử lý thanh toán 200000 cho ORD-003
[Timing] PaymentHandler tốn 151ms
[FraudHandler] Kiểm tra gian lận cho ORD-003
Phí vận chuyển: 30000
[EmailListener] Gửi email xác nhận đơn ORD-003
```
*(chú ý: ORD-003 không còn dòng `[InventoryListener]` vì đã unregister)*

### Ghi chú pattern & thay đổi mỗi pattern hấp thụ

| Pattern | Vị trí | Hấp thụ thay đổi gì |
|---|---|---|
| **Chain of Responsibility** | `List<OrderHandler>` + vòng lặp trong `OrderPipeline.process()` | Thêm/bớt/đổi thứ tự bước kiểm tra (VD: thêm `DiscountHandler`) — chỉ sửa `List<OrderHandler>` khi khởi tạo, không sửa `OrderPipeline` |
| **Strategy** | `ShippingPolicy` truyền vào `process()` | Thêm chính sách ship mới (VD: `SameDayShipping`) — chỉ thêm class mới, không sửa `OrderPipeline`/`calculateFee` caller |
| **Observer** | `OrderEventPublisher` + `CopyOnWriteArrayList<OrderPlacedListener>` | Thêm/bớt listener theo dõi sự kiện đơn hàng (VD: thêm `SmsListener`) mà không sửa `OrderPipeline` — hoàn toàn tách biệt "publisher" khỏi "ai đang lắng nghe" |
| **Decorator** | `TimingHandlerDecorator` bọc `PaymentHandler` | Thêm hành vi đo lường (hoặc retry, cache...) cho 1 handler cụ thể mà **KHÔNG sửa 1 dòng nào** trong `PaymentHandler` gốc — có thể tháo ra/gắn vào tùy ý |

**Vì sao `CopyOnWriteArrayList`:** an toàn khi có thể **vừa duyệt (publish) vừa sửa (register/unregister)** danh sách listener từ nhiều thread khác nhau — mỗi lần sửa tạo 1 **bản sao mảng mới**, các thread đang duyệt bản cũ **không bị ảnh hưởng** (liên hệ `ConcurrentModificationException` đã học ở Module 03.1 — `CopyOnWriteArrayList` tránh hoàn toàn lỗi này, phù hợp khi đọc/duyệt nhiều hơn ghi, đúng đặc điểm của danh sách listener — thường ít thay đổi, được duyệt liên tục).

---

*Đây là lời giải cho toàn bộ Phần B của Module 16. Tiếp theo: Module 17 — Build Tools & Quản lý dự án.*
