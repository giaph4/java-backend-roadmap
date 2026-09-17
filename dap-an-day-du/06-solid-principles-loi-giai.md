# Lời giải đầy đủ — Module 01.6: SOLID Principles trong Java

> Nguồn đề: `06 solid principles/06-solid-principles.md` (Phần B — Bài tập viết code/refactor). Chỉ làm Phần B.

---

## Bài 1 — Tách trách nhiệm (SRP)

### Đề
Cho `OrderService` gồm: tính tổng tiền (có thuế theo quốc gia), lưu vào DB, xuất hóa đơn PDF, gửi email xác nhận, ghi log audit. Refactor thành: một domain object hoặc calculator cho phần tính tiền, các interface `OrderRepository` / `InvoiceRenderer` / `OrderNotifier` / `AuditLog`, và một `PlaceOrderUseCase` điều phối. Viết `PlaceOrderUseCase` với constructor injection.

### Phân tích

**SRP (Single Responsibility Principle):** 1 class chỉ nên có **1 lý do để thay đổi**. `OrderService` gốc gánh **5 trách nhiệm khác nhau** (tính tiền, lưu DB, render PDF, gửi email, ghi log) — nếu đổi công thức thuế, đổi định dạng PDF, hay đổi nhà cung cấp email, **cùng 1 class** đều phải sửa, dù các thay đổi đó **hoàn toàn không liên quan nhau**.

**Cách tách:** mỗi trách nhiệm → 1 interface riêng (định nghĩa "cái gì cần làm", không phải "làm như thế nào"). `PlaceOrderUseCase` chỉ **điều phối** (orchestrate) — gọi đúng thứ tự các interface đó — không tự tay làm bất kỳ việc chi tiết nào. Đây là bước đệm trực tiếp cho tư duy Dependency Injection sẽ gặp đầy đủ ở Spring (Module 12).

### Lời giải

```java
package baitap.bai1;

import java.math.BigDecimal;
import java.util.List;

// ===== Domain object: chỉ lo tính tiền, không biết gì về DB/PDF/email =====
class TaxCalculator {
    public BigDecimal calculateTotal(BigDecimal subtotal, String countryCode) {
        BigDecimal taxRate = switch (countryCode) {
            case "VN" -> new BigDecimal("0.10");
            case "US" -> new BigDecimal("0.07");
            case "JP" -> new BigDecimal("0.08");
            default -> BigDecimal.ZERO;
        };
        return subtotal.add(subtotal.multiply(taxRate));
    }
}

class Order {
    final String orderId;
    final String customerEmail;
    final BigDecimal total;
    final String countryCode;

    Order(String orderId, String customerEmail, BigDecimal total, String countryCode) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.total = total;
        this.countryCode = countryCode;
    }
}

// ===== Mỗi trách nhiệm -> 1 interface riêng =====
interface OrderRepository {
    void save(Order order);
}

interface InvoiceRenderer {
    byte[] renderPdf(Order order);
}

interface OrderNotifier {
    void sendConfirmation(Order order);
}

interface AuditLog {
    void record(String action, String orderId);
}

// ===== PlaceOrderUseCase: CHỈ điều phối, không tự làm chi tiết =====
class PlaceOrderUseCase {
    private final TaxCalculator taxCalculator;
    private final OrderRepository repository;
    private final InvoiceRenderer invoiceRenderer;
    private final OrderNotifier notifier;
    private final AuditLog auditLog;

    // Constructor injection - toàn bộ dependency truyền qua constructor, KHÔNG "new" trực tiếp bên trong
    public PlaceOrderUseCase(TaxCalculator taxCalculator, OrderRepository repository,
                              InvoiceRenderer invoiceRenderer, OrderNotifier notifier, AuditLog auditLog) {
        this.taxCalculator = taxCalculator;
        this.repository = repository;
        this.invoiceRenderer = invoiceRenderer;
        this.notifier = notifier;
        this.auditLog = auditLog;
    }

    public Order placeOrder(String orderId, String customerEmail, BigDecimal subtotal, String countryCode) {
        BigDecimal total = taxCalculator.calculateTotal(subtotal, countryCode);
        Order order = new Order(orderId, customerEmail, total, countryCode);

        repository.save(order);
        invoiceRenderer.renderPdf(order);
        notifier.sendConfirmation(order);
        auditLog.record("ORDER_PLACED", orderId);

        return order;
    }
}
```

**Cài đặt cụ thể (để `main` demo được, đặt cùng file cho gọn):**

```java
class InMemoryOrderRepository implements OrderRepository {
    private final List<Order> storage = new java.util.ArrayList<>();
    @Override public void save(Order order) {
        storage.add(order);
        System.out.println("[Repository] Đã lưu order " + order.orderId);
    }
}

class SimplePdfInvoiceRenderer implements InvoiceRenderer {
    @Override public byte[] renderPdf(Order order) {
        System.out.println("[InvoiceRenderer] Đã render PDF cho order " + order.orderId + ", tổng: " + order.total);
        return new byte[0]; // giả lập nội dung PDF
    }
}

class EmailOrderNotifier implements OrderNotifier {
    @Override public void sendConfirmation(Order order) {
        System.out.println("[Notifier] Đã gửi email xác nhận tới " + order.customerEmail);
    }
}

class ConsoleAuditLog implements AuditLog {
    @Override public void record(String action, String orderId) {
        System.out.println("[AuditLog] " + action + " - orderId=" + orderId);
    }
}

public class Main {
    public static void main(String[] args) {
        PlaceOrderUseCase useCase = new PlaceOrderUseCase(
                new TaxCalculator(),
                new InMemoryOrderRepository(),
                new SimplePdfInvoiceRenderer(),
                new EmailOrderNotifier(),
                new ConsoleAuditLog()
        );

        useCase.placeOrder("ORD-001", "pho@example.com", new BigDecimal("100"), "VN");
    }
}
```

**Kết quả chạy:**
```
[Repository] Đã lưu order ORD-001
[InvoiceRenderer] Đã render PDF cho order ORD-001, tổng: 110.00
[Notifier] Đã gửi email xác nhận tới pho@example.com
[AuditLog] ORDER_PLACED - orderId=ORD-001
```

### Giải thích

- Mỗi class giờ có **đúng 1 lý do để thay đổi**: đổi công thức thuế → chỉ sửa `TaxCalculator`; đổi nhà cung cấp email (SMTP → SendGrid) → chỉ sửa `EmailOrderNotifier` (hoặc viết implementation mới, xem Bài 2); đổi công nghệ lưu trữ (in-memory → database thật) → chỉ viết `OrderRepository` implementation mới, `PlaceOrderUseCase` **không đổi 1 dòng**.
- `PlaceOrderUseCase` không `new` bất kỳ implementation cụ thể nào bên trong nó — toàn bộ nhận qua constructor. Đây chính là nền tảng để Spring (Module 12) sau này tự động "tiêm" (inject) các Bean vào, thay vì tự tay `new` như ở `Main` (composition root — xem lại đầy đủ ở Bài 6).

---

## Bài 2 — Đóng cho sửa đổi (OCP)

### Đề
Cho `DiscountCalculator` dùng chuỗi `if` theo `customerType` (`REGULAR`, `SILVER`, `GOLD`, `STAFF`). Chuyển sang `interface DiscountPolicy` + các implementation + một registry `Map<CustomerType, DiscountPolicy>`. Chứng minh thêm loại `PARTNER` không phải sửa `DiscountCalculator`.

### Phân tích

**OCP (Open/Closed Principle):** class nên **MỞ để mở rộng** (thêm hành vi mới), nhưng **ĐÓNG để sửa đổi** (không đụng vào code đã hoạt động ổn định). Chuỗi `if/else` theo loại khách hàng là dấu hiệu kinh điển vi phạm OCP — mỗi lần công ty thêm hạng khách hàng mới, phải **sửa trực tiếp** `DiscountCalculator`, rủi ro phá vỡ logic cũ đang chạy tốt.

**Giải pháp:** đưa từng loại giảm giá thành 1 class implement `DiscountPolicy`, đăng ký vào `Map<CustomerType, DiscountPolicy>` (registry). `DiscountCalculator` chỉ **tra map**, không biết và không cần biết có bao nhiêu loại chính sách tồn tại.

### Trước khi refactor (vi phạm OCP)

```java
package baitap.bai2;

class DiscountCalculatorBad {
    double calculate(String customerType, double amount) {
        if (customerType.equals("REGULAR")) return amount;
        else if (customerType.equals("SILVER")) return amount * 0.95;
        else if (customerType.equals("GOLD")) return amount * 0.90;
        else if (customerType.equals("STAFF")) return amount * 0.50;
        // Thêm "PARTNER" -> BẮT BUỘC sửa method này, thêm 1 "else if" nữa
        throw new IllegalArgumentException("Không xác định: " + customerType);
    }
}
```

### Lời giải

```java
package baitap.bai2;

import java.util.EnumMap;
import java.util.Map;

enum CustomerType { REGULAR, SILVER, GOLD, STAFF, PARTNER }

interface DiscountPolicy {
    double apply(double amount);
}

class RegularDiscount implements DiscountPolicy {
    @Override public double apply(double amount) { return amount; }
}

class SilverDiscount implements DiscountPolicy {
    @Override public double apply(double amount) { return amount * 0.95; }
}

class GoldDiscount implements DiscountPolicy {
    @Override public double apply(double amount) { return amount * 0.90; }
}

class StaffDiscount implements DiscountPolicy {
    @Override public double apply(double amount) { return amount * 0.50; }
}

// ===== Thêm SAU, KHÔNG sửa DiscountCalculator bên dưới =====
class PartnerDiscount implements DiscountPolicy {
    @Override public double apply(double amount) { return amount * 0.85; }
}

class DiscountCalculator {
    private final Map<CustomerType, DiscountPolicy> registry = new EnumMap<>(CustomerType.class);

    public DiscountCalculator register(CustomerType type, DiscountPolicy policy) {
        registry.put(type, policy);
        return this; // cho phép chain nhiều register() liên tiếp
    }

    public double calculate(CustomerType type, double amount) {
        DiscountPolicy policy = registry.get(type);
        if (policy == null) {
            throw new IllegalStateException("Chưa đăng ký chính sách cho: " + type);
        }
        return policy.apply(amount); // KHÔNG if/else, KHÔNG biết có bao nhiêu loại chính sách
    }
}

public class Main {
    public static void main(String[] args) {
        DiscountCalculator calculator = new DiscountCalculator()
                .register(CustomerType.REGULAR, new RegularDiscount())
                .register(CustomerType.SILVER, new SilverDiscount())
                .register(CustomerType.GOLD, new GoldDiscount())
                .register(CustomerType.STAFF, new StaffDiscount())
                .register(CustomerType.PARTNER, new PartnerDiscount()); // loại MỚI - đăng ký thêm, không sửa class

        for (CustomerType type : CustomerType.values()) {
            System.out.printf("%s: %.2f%n", type, calculator.calculate(type, 1_000_000));
        }
    }
}
```

**Kết quả chạy:**
```
REGULAR: 1000000.00
SILVER: 950000.00
GOLD: 900000.00
STAFF: 500000.00
PARTNER: 850000.00
```

### Chứng minh thêm PARTNER không sửa DiscountCalculator

So sánh 2 phiên bản: `DiscountCalculatorBad.calculate()` phải **thêm 1 dòng `else if` mới** mỗi khi có loại khách hàng mới — sửa trực tiếp vào **thân method đã hoạt động**. `DiscountCalculator` (bản mới) — thêm `PartnerDiscount` là **1 class hoàn toàn mới**, và chỉ cần thêm **1 dòng `.register(...)` ở nơi khởi tạo** (composition root, xem Bài 6) — bản thân `class DiscountCalculator` **không có ký tự nào bị sửa**.

### Giải thích

- Registry (`Map<CustomerType, DiscountPolicy>`) là kỹ thuật rất phổ biến để đạt OCP triệt để — thay vì code "biết" tất cả các case, nó chỉ **tra bảng**. Đây cũng chính là nguyên lý đứng sau `@Component`/tự động quét Bean của Spring (Module 12): thêm 1 class mới đánh dấu đúng annotation, Spring tự "đăng ký" nó vào container mà không ai phải sửa code điều phối trung tâm.
- `EnumMap` được chọn thay `HashMap` vì key là `enum` — `EnumMap` tối ưu hơn (dùng mảng nội bộ theo thứ tự `ordinal()`), đúng khuyến nghị hiệu năng khi key là enum.

---

## Bài 3 — Sửa vi phạm LSP

### Đề
Cho hệ thống `Bird` với method `fly()`; `Penguin extends Bird` ném `UnsupportedOperationException` trong `fly()`. Thiết kế lại bằng cách tách khả năng (`Bird`, `FlyingBird`) hoặc composition, sao cho không còn method ném exception cho trạng thái hợp lệ. Viết một hàm `void migrate(List<FlyingBird> flock)` chạy đúng với thiết kế mới.

### Phân tích

**LSP (Liskov Substitution Principle):** mọi nơi dùng được `Bird` (kiểu cha), phải dùng được **bất kỳ subclass nào** của nó **mà không gây lỗi/hành vi bất ngờ**. `Penguin extends Bird` rồi `fly()` ném `UnsupportedOperationException` là vi phạm LSP kinh điển: bất kỳ code nào viết `for (Bird b : birds) b.fly();` sẽ **crash bất ngờ** khi gặp `Penguin`, dù về mặt kiểu, `Penguin` "hợp lệ" là 1 `Bird`.

**Giải pháp:** đừng đặt `fly()` ở `Bird` (gốc, chung cho MỌI loài chim) — chỉ đặt ở `FlyingBird` (interface/class riêng cho **các loài biết bay**). `Penguin` chỉ `extends Bird`, **không** implement `FlyingBird` — không còn method `fly()` nào để "ép" nó phải cài sai bản chất.

### Trước khi refactor (vi phạm LSP)

```java
package baitap.bai3;

abstract class BirdBad {
    abstract void fly();
}

class SparrowBad extends BirdBad {
    @Override void fly() { System.out.println("Sẻ bay vút lên"); }
}

class PenguinBad extends BirdBad {
    @Override void fly() {
        throw new UnsupportedOperationException("Chim cánh cụt không biết bay!"); // VI PHẠM LSP
    }
}
```

### Lời giải

```java
package baitap.bai3;

import java.util.List;

abstract class Bird {
    protected final String name;
    protected Bird(String name) { this.name = name; }
    public String getName() { return name; }

    public void eat() { // hành vi CHUNG mọi loài chim đều làm được - đặt ở đây hợp lý
        System.out.println(name + " đang ăn");
    }
}

// Chỉ các loài BIẾT BAY mới implements interface này
interface FlyingBird {
    void fly();
}

class Sparrow extends Bird implements FlyingBird {
    public Sparrow(String name) { super(name); }
    @Override public void fly() { System.out.println(name + " (Sẻ) bay vút lên"); }
}

class Eagle extends Bird implements FlyingBird {
    public Eagle(String name) { super(name); }
    @Override public void fly() { System.out.println(name + " (Đại bàng) bay lượn trên cao"); }
}

// Penguin CHỈ extends Bird - KHÔNG implements FlyingBird -> không còn fly() để cài sai
class Penguin extends Bird {
    public Penguin(String name) { super(name); }
    public void swim() { System.out.println(name + " (Cánh cụt) đang bơi"); }
}

public class Main {

    // Hàm này CHỈ nhận FlyingBird - về mặt KIỂU, không thể truyền nhầm Penguin vào đây
    static void migrate(List<FlyingBird> flock) {
        System.out.println("=== Bắt đầu di cư ===");
        for (FlyingBird bird : flock) {
            bird.fly(); // TUYỆT ĐỐI AN TOÀN - mọi phần tử trong List<FlyingBird> chắc chắn bay được
        }
    }

    public static void main(String[] args) {
        List<FlyingBird> flyers = List.of(new Sparrow("Sẻ nâu"), new Eagle("Đại bàng vàng"));
        migrate(flyers);

        // migrate(List.of(new Penguin("Pingu"))); // LỖI COMPILE - Penguin không phải FlyingBird
        //                                            (không phải lỗi RUNTIME như bản cũ!)

        Bird penguin = new Penguin("Pingu");
        penguin.eat(); // vẫn dùng được hành vi CHUNG (Bird) bình thường
        ((Penguin) penguin).swim(); // hành vi RIÊNG của Penguin
    }
}
```

**Kết quả chạy:**
```
=== Bắt đầu di cư ===
Sẻ nâu (Sẻ) bay vút lên
Đại bàng vàng (Đại bàng) bay lượn trên cao
Pingu đang ăn
Pingu (Cánh cụt) đang bơi
```

### Giải thích

- **Điểm mấu chốt:** `List.of(new Penguin("Pingu"))` **không thể** truyền vào `migrate(List<FlyingBird> flock)` — đây là lỗi bị compiler chặn **NGAY LÚC BIÊN DỊCH**, không phải đợi tới runtime mới `throw UnsupportedOperationException` như thiết kế cũ. Đây chính là giá trị cốt lõi của việc sửa đúng LSP: biến 1 lớp lỗi tiềm ẩn ở runtime thành **không thể xảy ra** nhờ hệ thống kiểu (type system).
- Nguyên tắc chọn chỗ đặt method: method nào **KHÔNG PHẢI mọi subclass đều làm được hợp lý**, thì **không nên đặt ở class cha chung** — nên tách ra interface/class riêng chỉ dành cho nhóm con thực sự có khả năng đó (đây chính xác là bài học đã gặp ở Bài 5 Module 04 — is-a vs can-do).

---

## Bài 4 — Tách interface (ISP)

### Đề
Cho `interface PaymentProcessor` gồm `charge`, `refund`, `subscribe`, `cancelSubscription`, `generateInvoice`, `handleWebhook`. Tách thành các role interface hợp lý. Viết một class `OneTimeCheckout` chỉ phụ thuộc phần nó cần.

### Phân tích

**ISP (Interface Segregation Principle):** không nên ép 1 class phải implement những method **nó không bao giờ dùng tới**. `PaymentProcessor` gộp 6 method thuộc **3 nhóm nghiệp vụ khác nhau**: thanh toán 1 lần (`charge`, `refund`), thuê bao định kỳ (`subscribe`, `cancelSubscription`), và tác vụ phụ trợ (`generateInvoice`, `handleWebhook`). 1 class chỉ xử lý **thanh toán 1 lần** (`OneTimeCheckout`) bị buộc phải cài (hoặc ném `UnsupportedOperationException` cho) 4 method nó **không cần**.

**Giải pháp:** tách thành các **role interface** nhỏ, mỗi interface đại diện đúng **1 vai trò nghiệp vụ**, class nào cần gì thì `implements` đúng cái đó.

### Lời giải

```java
package baitap.bai4;

// ===== Tách thành các role interface nhỏ, mỗi cái đúng 1 vai trò =====
interface OneTimeChargeable {
    String charge(double amount);
    void refund(String transactionId);
}

interface Subscribable {
    String subscribe(String planId);
    void cancelSubscription(String subscriptionId);
}

interface InvoiceGenerating {
    byte[] generateInvoice(String transactionId);
}

interface WebhookHandling {
    void handleWebhook(String payload);
}

// ===== Class chỉ implements ĐÚNG những gì nó thực sự cần =====
class OneTimeCheckout implements OneTimeChargeable {
    @Override
    public String charge(double amount) {
        String txId = "TX-" + System.nanoTime();
        System.out.println("Thu tiền " + amount + " -> " + txId);
        return txId;
    }

    @Override
    public void refund(String transactionId) {
        System.out.println("Hoàn tiền giao dịch " + transactionId);
    }
    // KHÔNG có subscribe/cancelSubscription/generateInvoice/handleWebhook -
    // vì OneTimeCheckout không bao giờ cần tới chúng
}

// Ví dụ 1 class khác cần NHIỀU role hơn -> implements NHIỀU interface
class FullPaymentGateway implements OneTimeChargeable, Subscribable, InvoiceGenerating, WebhookHandling {
    @Override public String charge(double amount) { return "TX-" + amount; }
    @Override public void refund(String transactionId) { System.out.println("Refund " + transactionId); }
    @Override public String subscribe(String planId) { return "SUB-" + planId; }
    @Override public void cancelSubscription(String subscriptionId) { System.out.println("Cancel " + subscriptionId); }
    @Override public byte[] generateInvoice(String transactionId) { return new byte[0]; }
    @Override public void handleWebhook(String payload) { System.out.println("Webhook: " + payload); }
}

public class Main {
    public static void main(String[] args) {
        OneTimeCheckout checkout = new OneTimeCheckout();
        String txId = checkout.charge(500_000);
        checkout.refund(txId);

        // checkout.subscribe("PLAN-A"); // LỖI COMPILE - OneTimeCheckout không có method này
        //                                  (đúng như mong đợi - nó không cần role đó)
    }
}
```

**Kết quả chạy:**
```
Thu tiền 500000.0 -> TX-123456789
Hoàn tiền giao dịch TX-123456789
```

### Giải thích

- Với `interface PaymentProcessor` gộp chung ban đầu, nếu `OneTimeCheckout implements PaymentProcessor`, nó **bắt buộc** phải viết đủ cả 6 method — 4 method không liên quan sẽ phải cài đặt "giả" (`throw new UnsupportedOperationException()` hoặc để trống), lại **tái phạm chính lỗi LSP** đã sửa ở Bài 3!
- Tách theo role interface nhỏ giúp mỗi class chỉ "cam kết" đúng những gì nó thực sự làm được — đọc code cũng dễ hiểu hơn nhiều: nhìn `class X implements OneTimeChargeable` là biết ngay `X` có khả năng gì, không cần đọc hết thân class.
- Đây là nguyên lý áp dụng trực tiếp khi thiết kế Repository trong Spring Data (Module 15): `JpaRepository` cung cấp rất nhiều method, nhưng nếu chỉ cần đọc, có thể `extends Repository` + tự khai báo đúng vài method cần dùng, tránh "phình" interface không cần thiết.

---

## Bài 5 — Đảo phụ thuộc (DIP) và contract test

### Đề
Định nghĩa `interface KeyValueStore { Optional<String> get(String key); void put(String key, String value); }` trong package nghiệp vụ. Viết hai implementation: `InMemoryKeyValueStore` và `FileKeyValueStore`. Viết một bộ test JUnit **dùng chung** (abstract test class hoặc parameterized) kiểm tra contract: `put` rồi `get` trả đúng giá trị; `get` key chưa có trả `Optional.empty()`; `put` cùng key ghi đè.

### Phân tích

**DIP (Dependency Inversion Principle):** module cấp cao (business logic) không nên phụ thuộc trực tiếp vào module cấp thấp (chi tiết hạ tầng cụ thể — file, database...) — cả hai nên phụ thuộc vào **abstraction chung** (`interface KeyValueStore`). Đặt `interface` trong **package nghiệp vụ** (không phải package hạ tầng) thể hiện đúng tinh thần "đảo ngược": **hạ tầng phụ thuộc vào interface nghiệp vụ định nghĩa**, không phải nghiệp vụ phụ thuộc vào chi tiết hạ tầng.

**Abstract test class** cho phép viết **1 bộ test case DÙNG CHUNG** cho mọi implementation — mỗi implementation chỉ cần cung cấp cách khởi tạo instance của chính nó, còn logic kiểm tra ("hợp đồng" — contract) chỉ viết **1 lần duy nhất**. Đây là kỹ thuật **Contract Testing** ở mức đơn giản (khái niệm đầy đủ hơn sẽ gặp lại ở Module 19 — Microservices, kiểm tra hợp đồng API giữa các service).

### Lời giải

```java
package baitap.bai5;

import java.util.Optional;

// interface đặt trong package NGHIỆP VỤ - implementation cụ thể phụ thuộc VÀO nó, không phải ngược lại
public interface KeyValueStore {
    Optional<String> get(String key);
    void put(String key, String value);
}
```

```java
package baitap.bai5;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryKeyValueStore implements KeyValueStore {
    private final Map<String, String> data = new HashMap<>();

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    @Override
    public void put(String key, String value) {
        data.put(key, value); // Map.put() TỰ ghi đè nếu key đã tồn tại
    }
}
```

```java
package baitap.bai5;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.Properties;

public class FileKeyValueStore implements KeyValueStore {
    private final Path filePath;

    public FileKeyValueStore(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public Optional<String> get(String key) {
        Properties props = loadProps();
        return Optional.ofNullable(props.getProperty(key));
    }

    @Override
    public void put(String key, String value) {
        Properties props = loadProps();
        props.setProperty(key, value); // Properties.setProperty() cũng TỰ ghi đè nếu key đã tồn tại
        saveProps(props);
    }

    private Properties loadProps() {
        Properties props = new Properties();
        if (Files.exists(filePath)) {
            try (var in = Files.newInputStream(filePath)) {
                props.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return props;
    }

    private void saveProps(Properties props) {
        try (var out = Files.newOutputStream(filePath)) {
            props.store(out, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
```

**Bộ test dùng chung (JUnit 5 — sẽ học sâu ở Module 17, ở đây dùng như minh họa thực hành):**

```java
package baitap.bai5;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

// Abstract test class - chứa TOÀN BỘ logic kiểm tra "hợp đồng" (contract)
// Mỗi implementation chỉ cần cung cấp createStore() của riêng nó
abstract class KeyValueStoreContractTest {

    protected abstract KeyValueStore createStore();

    @Test
    void put_thenGet_returnsCorrectValue() {
        KeyValueStore store = createStore();
        store.put("name", "Pho");
        assertEquals(Optional.of("Pho"), store.get("name"));
    }

    @Test
    void get_missingKey_returnsEmpty() {
        KeyValueStore store = createStore();
        assertEquals(Optional.empty(), store.get("khong-ton-tai"));
    }

    @Test
    void put_sameKeyTwice_overwritesValue() {
        KeyValueStore store = createStore();
        store.put("name", "Pho");
        store.put("name", "Huynh"); // ghi đè
        assertEquals(Optional.of("Huynh"), store.get("name"));
    }
}

class InMemoryKeyValueStoreTest extends KeyValueStoreContractTest {
    @Override
    protected KeyValueStore createStore() {
        return new InMemoryKeyValueStore(); // CHỈ khai báo cách tạo - logic test kế thừa từ lớp cha
    }
}

class FileKeyValueStoreTest extends KeyValueStoreContractTest {
    @Override
    protected KeyValueStore createStore() {
        try {
            return new FileKeyValueStore(java.nio.file.Files.createTempFile("kvstore", ".properties"));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Giải thích

- **Cả 2 implementation đều PASS cùng 3 test case** mà không cần viết lại logic kiểm tra — chỉ khác nhau ở `createStore()`. Nếu sau này viết thêm `RedisKeyValueStore`, chỉ cần thêm 1 class test mới `extends KeyValueStoreContractTest`, override `createStore()` — **KHÔNG viết lại** 3 test case đã có.
- Đây chính là giá trị thực chiến của DIP + contract test: đảm bảo **mọi implementation đều tuân thủ đúng hợp đồng** đã định nghĩa ở interface — nếu 1 implementation mới **vô tình** không ghi đè giá trị cũ khi `put` trùng key (bug), test `put_sameKeyTwice_overwritesValue` sẽ **bắt được ngay lập tức**, dù bug nằm ở bất kỳ implementation nào.
- **Business logic** (ví dụ `PlaceOrderUseCase` ở Bài 1 nếu cần lưu cache) chỉ nên phụ thuộc `KeyValueStore` (interface) — hoàn toàn không quan tâm đang chạy `InMemoryKeyValueStore` hay `FileKeyValueStore` hay (sau này) `RedisKeyValueStore` thật — đổi implementation **không ảnh hưởng** 1 dòng code nghiệp vụ nào.

---

## Bài 6 — Composition root

### Đề
Viết `Main` lắp ráp toàn bộ hệ thống ở Bài 1 mà không dùng framework: tạo các class hạ tầng cụ thể, inject vào `PlaceOrderUseCase`, và chỉ ra rằng phần còn lại của code không tham chiếu class cụ thể nào.

### Phân tích

**Composition Root** là **DUY NHẤT MỘT nơi trong toàn bộ ứng dụng** biết và `new` tất cả các implementation cụ thể, rồi "lắp ráp" (wire) chúng lại với nhau qua constructor injection — thường đặt ở điểm khởi động ứng dụng (`main()`). Mọi class nghiệp vụ khác (`PlaceOrderUseCase`, `TaxCalculator`...) **chỉ biết interface**, không bao giờ tự `new` implementation cụ thể của dependency mình cần.

Đây chính xác là điều **Spring IoC Container** (Module 12) sẽ làm **tự động** thay bạn — hiểu rõ composition root thủ công giúp hiểu **Spring đang làm hộ việc gì** khi dùng `@Autowired`/constructor injection, thay vì coi đó là "phép màu".

### Lời giải

```java
package baitap.bai6;

import java.math.BigDecimal;

// ===== Toàn bộ interface + PlaceOrderUseCase giống hệt Bài 1 (rút gọn lại để minh họa Composition Root) =====
interface OrderRepository { void save(String orderId); }
interface InvoiceRenderer { void renderPdf(String orderId); }
interface OrderNotifier { void sendConfirmation(String orderId); }
interface AuditLog { void record(String action, String orderId); }

class PlaceOrderUseCase {
    private final OrderRepository repository;
    private final InvoiceRenderer invoiceRenderer;
    private final OrderNotifier notifier;
    private final AuditLog auditLog;

    // PlaceOrderUseCase CHỈ biết 4 interface này - KHÔNG hề biết
    // "InMemoryOrderRepository", "PdfBoxInvoiceRenderer"... là gì cả
    PlaceOrderUseCase(OrderRepository repository, InvoiceRenderer invoiceRenderer,
                       OrderNotifier notifier, AuditLog auditLog) {
        this.repository = repository;
        this.invoiceRenderer = invoiceRenderer;
        this.notifier = notifier;
        this.auditLog = auditLog;
    }

    void placeOrder(String orderId) {
        repository.save(orderId);
        invoiceRenderer.renderPdf(orderId);
        notifier.sendConfirmation(orderId);
        auditLog.record("ORDER_PLACED", orderId);
    }
}

// ===== Implementation cụ thể - CHỈ được "new" ở đúng 1 nơi: Composition Root bên dưới =====
class InMemoryOrderRepository implements OrderRepository {
    @Override public void save(String orderId) { System.out.println("[DB] Lưu " + orderId); }
}
class PdfBoxInvoiceRenderer implements InvoiceRenderer {
    @Override public void renderPdf(String orderId) { System.out.println("[PDF] Render hóa đơn " + orderId); }
}
class SmtpOrderNotifier implements OrderNotifier {
    @Override public void sendConfirmation(String orderId) { System.out.println("[Email] Gửi xác nhận " + orderId); }
}
class Slf4jAuditLog implements AuditLog {
    @Override public void record(String action, String orderId) { System.out.println("[Log] " + action + " " + orderId); }
}

// ===== COMPOSITION ROOT - nơi DUY NHẤT "new" toàn bộ implementation cụ thể =====
public class Main {
    public static void main(String[] args) {
        // Chỉ tại ĐÚNG 1 CHỖ NÀY trong toàn bộ ứng dụng mới xuất hiện tên các class cụ thể
        OrderRepository repository = new InMemoryOrderRepository();
        InvoiceRenderer invoiceRenderer = new PdfBoxInvoiceRenderer();
        OrderNotifier notifier = new SmtpOrderNotifier();
        AuditLog auditLog = new Slf4jAuditLog();

        PlaceOrderUseCase useCase = new PlaceOrderUseCase(repository, invoiceRenderer, notifier, auditLog);

        useCase.placeOrder("ORD-999");
    }
}
```

**Kết quả chạy:**
```
[DB] Lưu ORD-999
[PDF] Render hóa đơn ORD-999
[Email] Gửi xác nhận ORD-999
[Log] ORDER_PLACED ORD-999
```

### Chứng minh phần còn lại không tham chiếu class cụ thể nào

Kiểm tra thủ công: mở file chứa `PlaceOrderUseCase` — **tìm kiếm** các tên `InMemoryOrderRepository`, `PdfBoxInvoiceRenderer`, `SmtpOrderNotifier`, `Slf4jAuditLog` trong đó → **không có kết quả nào**. `PlaceOrderUseCase` chỉ import/dùng 4 `interface`. Toàn bộ 4 tên class cụ thể **CHỈ xuất hiện đúng 1 lần**, trong `main()` của `Main` — đây chính là bằng chứng "Composition Root".

### Giải thích

- **Muốn đổi `InMemoryOrderRepository` sang 1 implementation thật kết nối MySQL** (VD `MySqlOrderRepository`), chỉ cần sửa **1 dòng duy nhất** trong `main()` — toàn bộ `PlaceOrderUseCase` và các interface **không đổi 1 ký tự nào**. Đây là minh chứng cụ thể, có thể kiểm tra được, cho lợi ích thực sự của DIP.
- **Liên hệ trực tiếp Spring (Module 12):** khi dùng `@Component`/`@Autowired`, Spring IoC Container **CHÍNH LÀ** composition root — nó tự động quét, `new` các Bean, và "tiêm" vào constructor của các class khác — thay vì bạn tự viết `main()` như bài này. Hiểu rõ bài tập này trước khi học Spring giúp không còn thấy Dependency Injection là "phép màu bí ẩn", mà là **tự động hóa** đúng việc bạn vừa tự tay làm ở đây.

---

*Đây là lời giải cho toàn bộ Phần B của Module 06. Tiếp theo: Module 07 — equals(), hashCode(), toString().*
