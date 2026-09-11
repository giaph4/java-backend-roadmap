# Module 01.6 — SOLID Principles trong Java

> **Mức độ ưu tiên: Cao** — SOLID giúp thiết kế phần mềm dễ thay đổi, dễ kiểm thử và ít ảnh hưởng dây chuyền. Giá trị của SOLID nằm ở khả năng nhận ra đúng nguồn thay đổi và đặt ranh giới phụ thuộc hợp lý, không nằm ở số lượng interface hay class.

> **Phạm vi bài học:** tư duy thiết kế và refactor theo SRP, OCP, LSP, ISP, DIP; mối quan hệ với coupling, cohesion, composition và Dependency Injection. Cú pháp interface/abstract class đã học ở Bài 05; Design Patterns học sâu ở Bài 16; IoC Container và Spring DI học ở Bài 21.

---

## Mục lục

1. [SOLID giải quyết vấn đề gì?](#1-solid-giải-quyết-vấn-đề-gì)
2. [S — Single Responsibility Principle](#2-s--single-responsibility-principle)
3. [O — Open/Closed Principle](#3-o--openclosed-principle)
4. [L — Liskov Substitution Principle](#4-l--liskov-substitution-principle)
5. [I — Interface Segregation Principle](#5-i--interface-segregation-principle)
6. [D — Dependency Inversion Principle](#6-d--dependency-inversion-principle)
7. [DIP, Dependency Injection và IoC](#7-dip-dependency-injection-và-ioc)
8. [Năm nguyên lý phối hợp trong một thiết kế](#8-năm-nguyên-lý-phối-hợp-trong-một-thiết-kế)
9. [Code smell và quy trình refactor](#9-code-smell-và-quy-trình-refactor)
10. [SOLID trong Spring](#10-solid-trong-spring)
11. [Giới hạn và trade-off](#11-giới-hạn-và-trade-off)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. SOLID giải quyết vấn đề gì?

SOLID là tên ghép từ năm nguyên lý thiết kế hướng đối tượng được Robert C. Martin phổ biến:

| Chữ | Nguyên lý | Câu hỏi kiểm tra nhanh |
|---|---|---|
| **S** | Single Responsibility Principle | Thành phần này có bao nhiêu **lý do độc lập để thay đổi**? |
| **O** | Open/Closed Principle | Biến thể mới có thể được thêm mà không sửa logic ổn định không? |
| **L** | Liskov Substitution Principle | Mọi implementation có giữ đúng hợp đồng của abstraction không? |
| **I** | Interface Segregation Principle | Client có bị phụ thuộc vào method nó không dùng không? |
| **D** | Dependency Inversion Principle | Chính sách nghiệp vụ có phụ thuộc trực tiếp vào chi tiết kỹ thuật không? |

SOLID hướng đến hai thuộc tính:

- **High cohesion:** các phần trong một module cùng phục vụ một mục đích rõ ràng.
- **Low coupling:** thay đổi một module ít buộc module khác thay đổi theo.

SOLID không phải thước đo kiểu “càng nhiều class/interface càng tốt”. Một thiết kế tốt phải phục vụ **những thay đổi thực tế**. Trừu tượng hóa sai chỗ chỉ chuyển sự phức tạp từ một file sang nhiều file.

### SOLID áp dụng ở nhiều cấp độ

“Class” thường xuất hiện trong định nghĩa để dễ học, nhưng nguyên lý có thể áp dụng cho method, package, module hoặc service:

- Một method vừa validate, vừa lưu dữ liệu, vừa gửi email có vấn đề SRP.
- Một package nghiệp vụ import trực tiếp SDK nhà cung cấp có vấn đề DIP.
- Một API buộc mọi client nhận khả năng không cần thiết có nét tương tự ISP.

### Nguyên lý, pattern và kỹ thuật

| Khái niệm | Vai trò | Ví dụ |
|---|---|---|
| **Nguyên lý** | Tiêu chí đánh giá thiết kế | SRP, OCP, LSP, ISP, DIP |
| **Pattern** | Khuôn giải pháp cho một ngữ cảnh | Strategy, Adapter, Decorator |
| **Kỹ thuật** | Cơ chế ngôn ngữ/framework | interface, constructor injection, lambda |

Strategy có thể giúp đạt OCP; constructor injection có thể giúp đạt DIP. Dùng pattern hoặc kỹ thuật chưa tự động bảo đảm nguyên lý.

---

## 2. S — Single Responsibility Principle

> **Một module chỉ nên có một lý do để thay đổi.** Chính xác hơn: module nên chịu trách nhiệm trước **một actor hoặc một nhóm yêu cầu thay đổi gắn kết**.

### “Một trách nhiệm” không có nghĩa là “một method”

Một `BankAccount` có thể có `deposit`, `withdraw`, `freeze` và `balance`. Các method cùng bảo vệ quy tắc tài khoản nên vẫn có cohesion cao. Tách mỗi method thành một class sẽ phá mô hình miền.

SRP bị vi phạm khi các thay đổi độc lập bị trộn:

```java
public final class InvoiceService {
    public long calculateTotal(Invoice invoice) {
        return invoice.lines().stream()
                .mapToLong(line -> line.unitPrice() * line.quantity())
                .sum();
    }
    public void save(Invoice invoice) { /* JDBC / SQL */ }
    public byte[] renderPdf(Invoice invoice) { return new byte[0]; }
    public void emailCustomer(Invoice invoice) { /* SMTP */ }
}
```

Luật tính tiền, schema database, mẫu PDF và nhà cung cấp email là bốn trục thay đổi.

### Tách theo nguồn thay đổi

```java
public final class InvoiceCalculator {
    public long totalOf(Invoice invoice) {
        return invoice.lines().stream()
                .mapToLong(line -> line.unitPrice() * line.quantity())
                .sum();
    }
}

public interface InvoiceRepository {
    void save(Invoice invoice);
}

public interface InvoiceRenderer {
    byte[] render(Invoice invoice);
}

public interface InvoiceNotifier {
    void notifyCreated(Invoice invoice);
}
```

```java
public final class CreateInvoiceUseCase {
    private final InvoiceRepository repository;
    private final InvoiceNotifier notifier;

    public CreateInvoiceUseCase(
            InvoiceRepository repository,
            InvoiceNotifier notifier) {
        this.repository = repository;
        this.notifier = notifier;
    }

    public void execute(Invoice invoice) {
        repository.save(invoice);
        notifier.notifyCreated(invoice);
    }
}
```

`CreateInvoiceUseCase` có nhiều dependency nhưng vẫn có một trách nhiệm: điều phối ca sử dụng “tạo hóa đơn”. Số dòng hoặc số dependency không tự quyết định SRP.

### Cohesion và domain object

```java
public final class BankAccount {
    private long balance;
    private boolean frozen;

    public void withdraw(long amount) {
        if (frozen) throw new IllegalStateException("Account is frozen");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > balance) throw new IllegalStateException("Insufficient balance");
        balance -= amount;
    }
}
```

Validation và cập nhật `balance` cùng bảo vệ một invariant nên ở gần nhau. Đây là encapsulation tốt; đẩy từng phép kiểm tra sang class riêng sẽ làm giảm cohesion.

### Dấu hiệu SRP có vấn đề

- Tên mơ hồ như `Manager`, `Processor`, `Helper`, `CommonUtil`.
- Class import đồng thời HTTP, JDBC, PDF, email và business model.
- Một thay đổi nhỏ kéo theo sửa nhiều vùng không liên quan trong cùng file.
- Test cần mock rất nhiều dependency cho một hành vi đơn giản.
- Nhiều nhóm phụ trách sửa cùng class vì các lý do khác nhau.

> Hãy hỏi “Ai hoặc yêu cầu nào khiến code này thay đổi?” thay vì chỉ đếm method.

---

## 3. O — Open/Closed Principle

> **Một thực thể phần mềm nên mở cho mở rộng nhưng đóng cho sửa đổi.**

“Đóng” không có nghĩa là không bao giờ sửa code. Ý nghĩa thực tế: tại một **điểm biến thiên đã biết**, thêm biến thể mới không buộc sửa thuật toán ổn định đang sử dụng nó.

### Vi phạm OCP: dispatch bằng điều kiện tăng dần

```java
public final class ShippingCalculator {
    public long calculate(String type, long weightGram) {
        return switch (type) {
            case "STANDARD" -> weightGram * 2;
            case "EXPRESS"  -> weightGram * 5;
            case "SAME_DAY" -> weightGram * 10;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
```

Nếu loại vận chuyển là điểm thường xuyên mở rộng, mỗi loại mới buộc sửa class và kiểm thử lại tất cả nhánh.

### Áp dụng OCP bằng Strategy

```java
public interface ShippingPolicy {
    long calculate(long weightGram);
}

public final class StandardShipping implements ShippingPolicy {
    @Override
    public long calculate(long weightGram) {
        return weightGram * 2;
    }
}

public final class ExpressShipping implements ShippingPolicy {
    @Override
    public long calculate(long weightGram) {
        return weightGram * 5;
    }
}

public final class ShippingCalculator {
    public long calculate(ShippingPolicy policy, long weightGram) {
        return policy.calculate(weightGram);
    }
}
```

Thêm `InternationalShipping` bằng class mới; calculator không đổi.

### OCP không đồng nghĩa với “cấm switch”

`switch` vẫn phù hợp khi:

- Tập trường hợp nhỏ, ổn định và thuộc cùng một module.
- `enum` là tập đóng theo chủ đích.
- Logic chỉ ánh xạ dữ liệu đơn giản.
- Pattern matching kiểm tra đầy đủ một `sealed hierarchy`.

```java
sealed interface Result permits Success, Failure {}
record Success(String value) implements Result {}
record Failure(String message) implements Result {}

String describe(Result result) {
    return switch (result) {
        case Success(var value) -> value;
        case Failure(var message) -> "Error: " + message;
    };
}
```

Hierarchy ở đây được thiết kế đóng. Exhaustive `switch` giúp compiler báo nơi cần cập nhật khi thêm subtype.

### Các cơ chế mở rộng khác

- Polymorphism qua interface hoặc abstract class.
- Hàm/lambda cho hành vi nhỏ (`Predicate`, `Function`).
- Composition và Decorator.
- Registry `Map<Key, Handler>` thay chuỗi điều kiện.
- Cấu hình/dữ liệu khi khác biệt chỉ là giá trị.

```java
Map<String, ShippingPolicy> policies = Map.of(
        "STANDARD", new StandardShipping(),
        "EXPRESS", new ExpressShipping()
);

ShippingPolicy policy = Optional.ofNullable(policies.get(type))
        .orElseThrow(() -> new IllegalArgumentException("Unknown type: " + type));
```

### Tránh dự đoán mọi tương lai

Không cần tạo interface khi chỉ có một implementation ổn định và chưa có ranh giới cần thay thế. OCP hiệu quả khi abstraction bảo vệ biến thiên có bằng chứng: nhà cung cấp thanh toán, quy tắc chiết khấu, kênh Email/SMS/Push.

> Trừu tượng hóa quanh điểm thường thay đổi, không quanh mọi dòng code có thể thay đổi.

---

## 4. L — Liskov Substitution Principle

> Nếu `S` là subtype của `T`, object kiểu `S` phải thay thế được object kiểu `T` mà vẫn giữ tính đúng đắn của chương trình.

LSP nói về **hợp đồng hành vi**, không chỉ về việc code compile. `implements` hoặc `extends` chỉ tạo quan hệ kiểu; implementation còn phải giữ kỳ vọng của client.

### Hợp đồng hành vi

Subtype đúng cần tôn trọng:

1. **Precondition không mạnh hơn:** không đòi đầu vào khắt khe hơn kiểu cha.
2. **Postcondition không yếu hơn:** kết quả không kém bảo đảm hơn kiểu cha.
3. **Invariant được giữ nguyên:** trạng thái hợp lệ của abstraction luôn còn đúng.
4. **Ngoại lệ phù hợp:** không bất ngờ từ chối thao tác hợp lệ.
5. **Ngữ nghĩa và tác dụng phụ phù hợp:** tên và kết quả quan sát được giữ cùng ý nghĩa.

### Subtype làm mạnh precondition

```java
public interface FileStore {
    // Contract: lưu byte[] có kích thước từ 0 đến 10 MB
    void save(String name, byte[] content);
}

public final class TinyFileStore implements FileStore {
    @Override
    public void save(String name, byte[] content) {
        if (content.length > 1_000) { // chỉ nhận tối đa 1 KB
            throw new IllegalArgumentException("Too large");
        }
    }
}
```

Client có quyền truyền file 2 KB theo contract của `FileStore`, nhưng subtype từ chối. Precondition bị làm mạnh nên LSP bị vi phạm.

### Ví dụ Rectangle–Square

```java
public class Rectangle {
    protected int width;
    protected int height;

    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width;
    }

    @Override
    public void setHeight(int height) {
        this.width = height;
        this.height = height;
    }
}
```

```java
void resize(Rectangle rectangle) {
    rectangle.setWidth(5);
    rectangle.setHeight(10);
    if (rectangle.area() != 50) {
        throw new AssertionError("Broken Rectangle contract");
    }
}
```

`new Square()` làm hỏng kỳ vọng rằng width và height của `Rectangle` thay đổi độc lập. Quan hệ hình học không đủ để chứng minh quan hệ subtype hành vi.

```java
public interface Shape {
    int area();
}

public record Rectangle(int width, int height) implements Shape {
    @Override public int area() { return width * height; }
}

public record Square(int side) implements Shape {
    @Override public int area() { return side * side; }
}
```

### `UnsupportedOperationException` là tín hiệu mạnh

```java
class ReadOnlyDocument extends Document {
    @Override
    public void save() {
        throw new UnsupportedOperationException();
    }
}
```

Nếu `Document` hứa `save()` dùng được, subtype từ chối là vi phạm LSP. Có thể tách `ReadableDocument` và `WritableDocument`, hoặc dùng composition.

Exception này không luôn chứng minh vi phạm: nếu contract gốc tuyên bố rõ thao tác là optional, implementation vẫn có thể đúng contract. Khi đó contract có thể khó dùng và vi phạm tinh thần ISP.

### Java kiểm tra được gì?

Java hỗ trợ một phần LSP ở compile time:

- Override được trả về kiểu hẹp hơn (*covariant return type*).
- Không được ném checked exception rộng hơn method cha.
- Không được giảm mức truy cập.

Compiler không kiểm tra được ý nghĩa nghiệp vụ, nullability, tính idempotent hoặc tác dụng phụ. Những điều này cần tài liệu contract và contract test.

```java
interface Cache {
    // put cùng key/value nhiều lần không đổi kết quả quan sát được
    void put(String key, String value);
    Optional<String> get(String key);
}
```

Mọi implementation có thể chạy chung một bộ test để chứng minh contract.

### Dấu hiệu vi phạm LSP

- Subtype ném exception cho đầu vào hợp lệ theo abstraction.
- Override bỏ qua công việc client trông đợi.
- Client dùng `instanceof` để né một subtype cụ thể.
- Subtype trả `null` trong khi contract hứa giá trị khác null.
- Subtype đổi nghĩa của method.
- Kế thừa chỉ để tái sử dụng code dù quan hệ hành vi không đúng.

---

## 5. I — Interface Segregation Principle

> **Client không nên bị buộc phụ thuộc vào method mà nó không dùng.** Interface lớn nên được chia thành các interface nhỏ, theo vai trò, để mỗi client chỉ thấy phần liên quan.

ISP nhìn abstraction từ **phía client**. SRP hỏi "nhà cung cấp có bao nhiêu lý do để thay đổi?"; ISP hỏi "client có bị kéo theo thay đổi mà nó không quan tâm không?".

### Fat interface: một hợp đồng phục vụ nhiều vai trò

```java
public interface Machine {
    void print(Document d);
    void scan(Document d);
    void fax(Document d);
    void staple(Document d);
}

public final class SimplePrinter implements Machine {
    @Override public void print(Document d) { /* ... */ }
    @Override public void scan(Document d)  { throw new UnsupportedOperationException(); }
    @Override public void fax(Document d)   { throw new UnsupportedOperationException(); }
    @Override public void staple(Document d){ throw new UnsupportedOperationException(); }
}
```

Ba method rỗng hoặc ném exception là dấu hiệu interface không khớp với người cài đặt. Client chỉ cần in vẫn phải biên dịch lại khi `Machine` thêm `collate()`.

### Tách theo vai trò (role interface)

```java
public interface Printer { void print(Document d); }
public interface Scanner { void scan(Document d); }
public interface Fax     { void fax(Document d); }

public final class SimplePrinter implements Printer {
    @Override public void print(Document d) { /* ... */ }
}

public final class OfficeMachine implements Printer, Scanner, Fax {
    @Override public void print(Document d) { /* ... */ }
    @Override public void scan(Document d)  { /* ... */ }
    @Override public void fax(Document d)   { /* ... */ }
}
```

`OfficeMachine` vẫn gom đủ khả năng khi cần, nhưng client khai báo phụ thuộc hẹp:

```java
public final class NightlyReportJob {
    private final Printer printer;   // không thấy scan/fax → không bị ảnh hưởng khi chúng đổi
    public NightlyReportJob(Printer printer) { this.printer = printer; }
}
```

### ISP ở cấp tham số: phụ thuộc kiểu hẹp nhất đủ dùng

```java
// Kém: buộc caller đưa đúng ArrayList
long sum(ArrayList<Integer> numbers) { /* ... */ }

// Tốt: chỉ cần duyệt
long sum(Iterable<Integer> numbers) { /* ... */ }
```

Nhận `Iterable` thay vì `List`, `List` thay vì `ArrayList`, `Reader` thay vì `FileReader`. Kiểu tham số càng hẹp, hàm càng dễ tái dùng và càng ít ràng buộc client.

### `java.util.List` là ví dụ fat interface trong chính JDK

`List` khai báo `add`, `remove`, `set`, `clear`... nhưng `List.of(...)` và `Arrays.asList(...)` ném `UnsupportedOperationException` cho các thao tác thay đổi. Đây là *optional operation* — một sự thỏa hiệp lịch sử. Bài học: khi thiết kế interface mới, tránh gộp "đọc" và "ghi" nếu có client chỉ đọc.

```java
public interface ReadOnlyCatalog {
    Optional<Product> findById(String id);
    List<Product> search(Query query);
}

public interface MutableCatalog extends ReadOnlyCatalog {
    void add(Product product);
    void remove(String id);
}
```

### Default method giảm nhẹ nhưng không thay ISP

Thêm `default` method rỗng vào interface lớn giúp client cũ biên dịch tiếp, nhưng client vẫn *nhìn thấy* method không liên quan trong autocomplete và tài liệu. Tách interface vẫn là giải pháp gốc.

### Dấu hiệu ISP có vấn đề

- Implementation có method rỗng hoặc ném `UnsupportedOperationException` theo nhóm.
- Thêm method vào interface làm nhiều class không liên quan phải sửa.
- Mock trong test phải stub nhiều method mà kịch bản không chạm tới.
- Tên interface chung chung (`Service`, `Manager`, `Repository` không có ngữ cảnh) và có hơn 8–10 method thuộc các nhóm khác nhau.

---

## 6. D — Dependency Inversion Principle

> **Module cấp cao không nên phụ thuộc module cấp thấp; cả hai phụ thuộc vào abstraction.** Và **abstraction không phụ thuộc chi tiết; chi tiết phụ thuộc abstraction.**

"Cấp cao" là chính sách nghiệp vụ (tính giá, duyệt đơn, quy trình). "Cấp thấp" là chi tiết kỹ thuật (JDBC, HTTP client, SDK nhà cung cấp, hệ thống file).

### Phụ thuộc trực tiếp vào chi tiết

```java
public final class CheckoutService {
    private final StripeClient stripe = new StripeClient();   // cấp cao trói vào SDK cụ thể

    public void checkout(Cart cart) {
        long amount = cart.totalCents();
        stripe.charge(amount, cart.customerToken());
    }
}
```

`CheckoutService` không thể kiểm thử nếu không gọi Stripe, không thể đổi cổng thanh toán nếu không sửa logic nghiệp vụ, và package nghiệp vụ giờ `import com.stripe.*`.

### Đảo phụ thuộc: abstraction thuộc phía chính sách

```java
// Nằm trong package nghiệp vụ, do phía "cấp cao" định nghĩa và sở hữu
public interface PaymentGateway {
    PaymentResult charge(long amountCents, String customerRef);
}

public final class CheckoutService {
    private final PaymentGateway gateway;

    public CheckoutService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public void checkout(Cart cart) {
        PaymentResult result = gateway.charge(cart.totalCents(), cart.customerRef());
        if (!result.approved()) {
            throw new PaymentDeclinedException(result.reason());
        }
    }
}
```

```java
// Nằm trong package hạ tầng, phụ thuộc VÀO abstraction của nghiệp vụ
public final class StripePaymentGateway implements PaymentGateway {
    private final StripeClient stripe;

    public StripePaymentGateway(StripeClient stripe) {
        this.stripe = stripe;
    }

    @Override
    public PaymentResult charge(long amountCents, String customerRef) {
        var response = stripe.charge(amountCents, customerRef);
        return new PaymentResult(response.isPaid(), response.failureMessage());
    }
}
```

### Điểm dễ hiểu sai: "dùng interface" chưa phải DIP

Nếu interface `PaymentGateway` được đặt trong package hạ tầng và mô phỏng sát API Stripe (`charge(StripeChargeRequest)`), thì nghiệp vụ vẫn phụ thuộc chi tiết, chỉ thêm một lớp gián tiếp. DIP đạt được khi:

1. Abstraction diễn đạt bằng **ngôn ngữ của nghiệp vụ**, không phải của công nghệ.
2. Abstraction **được sở hữu bởi phía dùng nó** (package nghiệp vụ), không phải phía cài đặt.
3. Chiều phụ thuộc mã nguồn: hạ tầng → nghiệp vụ, ngược với chiều lời gọi runtime.

### Hình dạng phụ thuộc

```
          Không có DIP                         Có DIP

  CheckoutService                      CheckoutService ──► PaymentGateway
        │                                                        ▲
        ▼                                                        │
  StripePaymentGateway                 StripePaymentGateway ─────┘
        │                                     │
        ▼                                     ▼
    StripeClient                          StripeClient

  Nghiệp vụ phụ thuộc hạ tầng           Hạ tầng phụ thuộc abstraction nghiệp vụ
```

Đây là ý tưởng cốt lõi của kiến trúc Ports and Adapters (Hexagonal): "port" là abstraction do miền định nghĩa, "adapter" là implementation ở rìa hệ thống.

### Dấu hiệu DIP có vấn đề

- Package/module nghiệp vụ `import` SDK, driver JDBC, annotation ORM, class HTTP client.
- `new` một class hạ tầng bên trong logic nghiệp vụ.
- Không thể viết unit test cho quy tắc nghiệp vụ nếu không có database hoặc mạng.
- Đổi thư viện kỹ thuật buộc phải sửa file chứa quy tắc nghiệp vụ.

---

## 7. DIP, Dependency Injection và IoC

Ba khái niệm hay bị dùng lẫn:

| Khái niệm | Loại | Nội dung |
|---|---|---|
| **Dependency Inversion Principle** | Nguyên lý thiết kế | Chính sách và chi tiết cùng phụ thuộc abstraction; abstraction thuộc phía chính sách |
| **Dependency Injection** | Kỹ thuật | Dependency được **truyền vào** từ ngoài (constructor/setter/tham số) thay vì đối tượng tự tạo |
| **Inversion of Control** | Nguyên tắc kiến trúc | Luồng điều khiển do framework/hạ tầng nắm; code ứng dụng được gọi lại (callback, lifecycle, container) |

DI là một cách hiện thực IoC cho việc lắp ráp đối tượng. Có thể đạt DIP mà không cần container: chỉ cần constructor nhận abstraction và một nơi lắp ráp (composition root).

### Constructor injection là mặc định nên chọn

```java
public final class CreateInvoiceUseCase {
    private final InvoiceRepository repository;
    private final InvoiceNotifier notifier;

    public CreateInvoiceUseCase(InvoiceRepository repository, InvoiceNotifier notifier) {
        this.repository = Objects.requireNonNull(repository);
        this.notifier = Objects.requireNonNull(notifier);
    }
}
```

Ưu điểm so với setter/field injection:

- Field `final` — đối tượng bất biến sau khi tạo, an toàn khi chia sẻ.
- Dependency bắt buộc được nêu tường minh trong chữ ký; thiếu là lỗi biên dịch/khởi tạo, không phải `NullPointerException` lúc chạy.
- Test dựng đối tượng bằng `new` với test double, không cần framework.
- Danh sách tham số quá dài trở thành tín hiệu SRP.

### Composition root: lắp ráp thủ công

```java
public final class Main {
    public static void main(String[] args) {
        var dataSource = new HikariDataSource(config());
        InvoiceRepository repository = new JdbcInvoiceRepository(dataSource);
        InvoiceNotifier notifier = new EmailInvoiceNotifier(new SmtpMailer(config()));

        var useCase = new CreateInvoiceUseCase(repository, notifier);
        new HttpServer(useCase).start();
    }
}
```

Chỉ một nơi biết các class cụ thể. Phần còn lại của hệ thống làm việc với abstraction.

### Service Locator: đối lập nên tránh

```java
public final class CreateInvoiceUseCase {
    public void execute(Invoice invoice) {
        var repository = ServiceLocator.get(InvoiceRepository.class); // phụ thuộc bị giấu
        repository.save(invoice);
    }
}
```

Dependency không còn xuất hiện trong chữ ký, khó thấy khi đọc code và khó kiểm soát trong test. Constructor injection giữ phụ thuộc **hiện diện và tường minh**.

---

## 8. Năm nguyên lý phối hợp trong một thiết kế

Một lát cắt "đăng ký người dùng rồi gửi thông báo chào mừng":

```java
// Chính sách nghiệp vụ (cấp cao). Sở hữu các abstraction mà nó cần.
public interface UserRepository {                 // DIP: port do miền định nghĩa
    boolean existsByEmail(String email);
    void save(User user);
}

public interface WelcomeChannel {                 // ISP: chỉ một khả năng, hẹp
    void sendWelcome(User user);
}

public final class RegisterUserUseCase {          // SRP: điều phối đúng một ca sử dụng
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final List<WelcomeChannel> channels;  // OCP: thêm kênh không sửa class này

    public RegisterUserUseCase(UserRepository users, PasswordHasher hasher, List<WelcomeChannel> channels) {
        this.users = users;
        this.hasher = hasher;
        this.channels = List.copyOf(channels);
    }

    public User execute(RegisterCommand command) {
        if (users.existsByEmail(command.email())) {
            throw new EmailAlreadyUsedException(command.email());
        }
        var user = User.create(command.email(), hasher.hash(command.rawPassword()));
        users.save(user);
        channels.forEach(channel -> channel.sendWelcome(user)); // LSP: mọi kênh giữ đúng hợp đồng
        return user;
    }
}
```

```java
// Chi tiết kỹ thuật (cấp thấp). Phụ thuộc vào abstraction phía trên.
public final class JpaUserRepository implements UserRepository { /* ... */ }
public final class EmailWelcomeChannel implements WelcomeChannel { /* ... */ }
public final class SmsWelcomeChannel   implements WelcomeChannel { /* ... */ }
```

- **SRP:** use case chỉ điều phối; hashing, lưu trữ, gửi tin ở nơi khác.
- **OCP:** thêm `PushWelcomeChannel` là thêm class và đăng ký vào danh sách; `execute` không đổi.
- **LSP:** mọi `WelcomeChannel` phải thực sự gửi và không ném exception cho user hợp lệ.
- **ISP:** use case phụ thuộc `WelcomeChannel` và `UserRepository` hẹp, không phụ thuộc một `NotificationManager` đồ sộ.
- **DIP:** `RegisterUserUseCase` không biết JPA hay SMTP tồn tại.

Không nguyên lý nào tự đứng một mình: ISP tạo ra abstraction hẹp để DIP đảo phụ thuộc; OCP dựa trên LSP để biến thể mới an toàn; SRP quyết định ranh giới để cả bốn cái còn lại có chỗ đặt.

---

## 9. Code smell và quy trình refactor

Robert C. Martin mô tả các triệu chứng của thiết kế kém — thường xuất hiện cùng lúc:

| Smell | Biểu hiện | Nguyên lý liên quan |
|---|---|---|
| **Rigidity** | Một thay đổi nhỏ lan ra nhiều module | SRP, DIP |
| **Fragility** | Sửa chỗ này, vỡ chỗ không liên quan | SRP, LSP |
| **Immobility** | Không tách được phần hữu ích để tái dùng vì dính quá nhiều thứ | SRP, ISP, DIP |
| **Viscosity** | Cách "làm đúng" khó hơn cách "chắp vá" | Toàn bộ |
| **Needless complexity** | Abstraction cho biến thiên chưa từng xảy ra | OCP (lạm dụng) |
| **Needless repetition** | Cùng một logic sao chép nhiều nơi | SRP |
| **Opacity** | Đọc code không hiểu ý định | SRP |

### Quy trình refactor an toàn

1. **Phủ test đặc tả (characterization test):** ghi lại hành vi hiện tại trước khi đổi cấu trúc.
2. **Xác định trục thay đổi thật:** dựa trên lịch sử commit và yêu cầu sắp tới, không dựa cảm giác.
3. **Tạo seam:** đưa một interface vào đúng ranh giới cần thay thế; ban đầu chỉ có một implementation.
4. **Extract:** tách trách nhiệm phụ ra class/method riêng; giữ bước nhỏ, chạy test sau mỗi bước.
5. **Đảo phụ thuộc:** cho phía cấp cao nhận abstraction qua constructor; đẩy việc `new` ra composition root.
6. **Xóa nhánh điều kiện dispatch:** thay `switch`/`if` phân loại bằng polymorphism hoặc registry khi tập biến thể mở.
7. **Kiểm chứng:** test xanh, và thử nghiệm thêm một biến thể mới để xác nhận điểm mở rộng hoạt động.

> Refactor để phục vụ thay đổi đang tới. Nếu chưa có thay đổi nào ép buộc, việc chia nhỏ có thể chờ.

---

## 10. SOLID trong Spring

Spring là một IoC container: nó tạo, cấu hình và lắp ráp bean, rồi gọi lại code ứng dụng.

| Nguyên lý | Cách Spring hỗ trợ | Lưu ý |
|---|---|---|
| **SRP** | Mỗi `@Service`/`@Component` một trách nhiệm; tách `@Repository`, `@RestController`, mapper | Controller "mỏng", không chứa quy tắc nghiệp vụ |
| **OCP** | Nhiều bean cùng interface; inject `List<T>` hoặc `Map<String, T>`; `@ConditionalOnMissingBean`, `@Profile` | Thêm implementation là thêm class có `@Component`, không sửa nơi dùng |
| **LSP** | Program to interface (`UserService`), mọi impl chạy chung contract test | Proxy AOP: `@Transactional` trên method `private`/`final` hoặc gọi nội bộ (self-invocation) không có hiệu lực |
| **ISP** | Interface repository hẹp theo aggregate; Spring Data sinh implementation | Tránh một `FacadeService` gom mọi thứ |
| **DIP** | Constructor injection (khuyến nghị chính thức); `@Bean` đặt trong module nghiệp vụ, adapter ở module hạ tầng | Field injection (`@Autowired` trên field) làm test khó và giấu phụ thuộc |

```java
@Service
public class RegisterUserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final List<WelcomeChannel> channels;   // Spring tự inject mọi bean WelcomeChannel

    public RegisterUserService(UserRepository users, PasswordEncoder encoder, List<WelcomeChannel> channels) {
        this.users = users;
        this.encoder = encoder;
        this.channels = channels;
    }
}
```

> Từ Spring 4.3, class có **một constructor** thì không cần `@Autowired`. Constructor injection cho phép field `final` và test bằng `new` không cần khởi động context.

### Cạm bẫy proxy và LSP

```java
@Service
public class ReportService {
    @Transactional
    public void generate() { buildRows(); }      // gọi nội bộ

    @Transactional
    public void buildRows() { /* ... */ }         // KHÔNG chạy trong transaction mới khi bị gọi từ generate()
}
```

Proxy chỉ áp dụng khi lời gọi đi qua tham chiếu được inject. Đây là nơi hành vi "thay thế được" bị phá ngầm nếu không nắm cơ chế.

---

## 11. Giới hạn và trade-off

SOLID là công cụ đánh giá, không phải luật bắt buộc áp cho mọi dòng code.

### Trừu tượng hóa sớm tốn kém hơn lặp code

Một abstraction sai buộc mọi implementation uốn theo hình dạng không phù hợp, và việc gỡ nó ra khó hơn xóa vài dòng trùng lặp. Nguyên tắc thực dụng: **chấp nhận lặp cho tới khi hình dạng của biến thiên rõ ràng** (thường là lần thứ ba), rồi mới rút abstraction.

### Chi phí của lớp gián tiếp

Mỗi interface thêm một bước khi đọc code: từ nơi gọi phải tìm implementation thật. Với hệ thống nhỏ hoặc script một lần, một class 200 dòng mạch lạc dễ bảo trì hơn tám file mỗi file 30 dòng.

### Khi nào có thể hoãn SOLID

- Prototype, thử nghiệm, code dùng một lần.
- Miền nghiệp vụ chưa ổn định, chưa biết trục thay đổi.
- Chỉ có một implementation và chưa có ranh giới cần test thay thế.

### Khi nào nên đầu tư sớm

- Ranh giới với hệ thống ngoài (thanh toán, email, lưu trữ) — gần như chắc chắn cần thay thế trong test.
- Điểm đã có lịch sử thay đổi nhiều lần.
- Code được nhiều nhóm cùng sửa vì các lý do khác nhau.

> Câu hỏi quyết định luôn là "thay đổi nào sắp tới và nó tốn bao nhiêu nếu không chuẩn bị", không phải "thiết kế đã đủ nhiều tầng chưa".

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Nguyên lý | Một câu | Vi phạm điển hình | Công cụ thường dùng |
|---|---|---|---|
| **SRP** | Một module, một nguồn thay đổi / một actor | Class trộn nghiệp vụ + JDBC + PDF + email | Tách class, use case, domain object |
| **OCP** | Mở để mở rộng, đóng để sửa | `switch` theo loại phình dần cho điểm hay mở rộng | Strategy, polymorphism, registry `Map`, `sealed` + exhaustive switch |
| **LSP** | Subtype thay thế được mà giữ đúng hợp đồng hành vi | Subtype làm mạnh precondition, ném exception cho input hợp lệ, `instanceof` để né subtype | Contract test, tách interface theo khả năng, composition thay kế thừa |
| **ISP** | Client không phụ thuộc method nó không dùng | Fat interface, method rỗng / `UnsupportedOperationException` theo nhóm | Role interface, tham số nhận kiểu hẹp nhất |
| **DIP** | Chính sách và chi tiết cùng phụ thuộc abstraction do chính sách sở hữu | Nghiệp vụ `import` SDK, `new` class hạ tầng trong logic | Port do miền định nghĩa, constructor injection, composition root |

| Phân biệt | Ý |
|---|---|
| DIP vs DI vs IoC | DIP = nguyên lý; DI = truyền phụ thuộc từ ngoài; IoC = framework nắm luồng điều khiển |
| SRP vs ISP | SRP nhìn từ phía nhà cung cấp (lý do thay đổi); ISP nhìn từ phía client (phụ thuộc thừa) |
| "Dùng interface" vs DIP | Có interface nhưng đặt ở phía hạ tầng và mô phỏng API kỹ thuật thì vẫn chưa đảo phụ thuộc |
| Nguyên lý vs pattern | Strategy/Adapter/Decorator là khuôn giải pháp; SOLID là tiêu chí đánh giá |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Class sau vi phạm nguyên lý nào? Có bao nhiêu trục thay đổi?
```java
public final class UserController {
    public void register(HttpRequest req) {
        var email = req.param("email");
        if (!email.contains("@")) throw new IllegalArgumentException("bad email");
        var sql = "INSERT INTO users(email) VALUES ('" + email + "')";
        jdbc.execute(sql);
        smtp.send(email, "Welcome", renderTemplate("welcome.html", email));
    }
}
```

**Câu 2.** Đoạn này "dùng interface" nhưng có đạt DIP không? Giải thích.
```java
package com.app.billing;
import com.stripe.model.Charge;

public interface StripeGateway {
    Charge createCharge(long amount, String stripeToken);
}
```

**Câu 3.** `Square extends Rectangle` với `setWidth`/`setHeight` đồng bộ hai cạnh vi phạm LSP ở điểm nào cụ thể? Nêu một client code bị hỏng.

**Câu 4.** Interface sau có vấn đề ISP không? Nếu có, tách thế nào?
```java
public interface Repository<T> {
    T findById(String id);
    List<T> findAll();
    void save(T entity);
    void delete(String id);
    void bulkImport(InputStream csv);
    byte[] exportCsv();
    void reindexSearch();
}
```

**Câu 5.** Việc thay `switch` bằng Strategy trong `ShippingCalculator` (mục 3) có luôn tốt hơn không? Nêu hai điều kiện khiến `switch` vẫn là lựa chọn đúng.

**Câu 6.** `RegisterUserService` nhận 6 dependency qua constructor. Đây có phải vi phạm SRP không? Dựa vào đâu để kết luận?

**Câu 7.** Vì sao constructor injection được ưu tiên hơn field injection (`@Autowired` trên field) khi xét theo LSP và khả năng kiểm thử?

**Câu 8.** `@Transactional` trên method `buildRows()` không có hiệu lực khi `generate()` gọi `this.buildRows()`. Điều này liên quan nguyên lý nào và cơ chế Spring nào?

---

### Phần B — Bài tập viết code / refactor

**Bài 1 — Tách trách nhiệm (SRP).**
Cho `OrderService` gồm: tính tổng tiền (có thuế theo quốc gia), lưu vào DB, xuất hóa đơn PDF, gửi email xác nhận, ghi log audit. Refactor thành: một domain object hoặc calculator cho phần tính tiền, các interface `OrderRepository` / `InvoiceRenderer` / `OrderNotifier` / `AuditLog`, và một `PlaceOrderUseCase` điều phối. Viết `PlaceOrderUseCase` với constructor injection.

**Bài 2 — Đóng cho sửa đổi (OCP).**
Cho `DiscountCalculator` dùng chuỗi `if` theo `customerType` (`REGULAR`, `SILVER`, `GOLD`, `STAFF`). Chuyển sang `interface DiscountPolicy` + các implementation + một registry `Map<CustomerType, DiscountPolicy>`. Chứng minh thêm loại `PARTNER` không phải sửa `DiscountCalculator`.

**Bài 3 — Sửa vi phạm LSP.**
Cho hệ thống `Bird` với method `fly()`; `Penguin extends Bird` ném `UnsupportedOperationException` trong `fly()`. Thiết kế lại bằng cách tách khả năng (`Bird`, `FlyingBird`) hoặc composition, sao cho không còn method ném exception cho trạng thái hợp lệ. Viết một hàm `void migrate(List<FlyingBird> flock)` chạy đúng với thiết kế mới.

**Bài 4 — Tách interface (ISP).**
Cho `interface PaymentProcessor` gồm `charge`, `refund`, `subscribe`, `cancelSubscription`, `generateInvoice`, `handleWebhook`. Tách thành các role interface hợp lý. Viết một class `OneTimeCheckout` chỉ phụ thuộc phần nó cần.

**Bài 5 — Đảo phụ thuộc (DIP) và contract test.**
Định nghĩa `interface KeyValueStore { Optional<String> get(String key); void put(String key, String value); }` trong package nghiệp vụ. Viết hai implementation: `InMemoryKeyValueStore` và `FileKeyValueStore`. Viết một bộ test JUnit **dùng chung** (abstract test class hoặc parameterized) kiểm tra contract: `put` rồi `get` trả đúng giá trị; `get` key chưa có trả `Optional.empty()`; `put` cùng key ghi đè.

**Bài 6 — Composition root.**
Viết `Main` lắp ráp toàn bộ hệ thống ở Bài 1 mà không dùng framework: tạo các class hạ tầng cụ thể, inject vào `PlaceOrderUseCase`, và chỉ ra rằng phần còn lại của code không tham chiếu class cụ thể nào.

---

### Phần C — Bài tập nâng cao (thiết kế / phân tích)

**Câu 7.** Cho một codebase có `PaymentService` là interface với **một** implementation duy nhất `PaymentServiceImpl`, không có test nào mock nó, và cổng thanh toán chưa từng đổi trong 3 năm. Interface này có đang tạo giá trị theo OCP/DIP không? Lập luận cho cả hai phía "giữ" và "bỏ".

**Câu 8.** Phân biệt hai tình huống cùng dùng `throw new UnsupportedOperationException()`: (a) `List.of(...)` cho `add()`; (b) `ReadOnlyDocument.save()`. Trường hợp nào vi phạm LSP, trường hợp nào là "đúng contract nhưng contract kém"? Đề xuất cách thiết kế lại cho (b).

**Câu 9.** Một service inject `List<Validator>` và chạy tuần tự. Yêu cầu mới: một số validator phải chạy trước số khác. Thảo luận các cách giữ OCP: `@Order`/`Comparable`, chia thành các pha (`List<PreValidator>`, `List<PostValidator>`), hoặc một `ValidationPipeline` cấu hình được. Đánh đổi của mỗi cách.

**Câu 10.** "SRP nói một class một trách nhiệm" thường bị hiểu thành "một class một method" hoặc "một class càng nhỏ càng tốt". Dùng ví dụ `BankAccount` (mục 2) để phản biện, và phát biểu lại SRP theo hướng "actor / nguồn thay đổi".

**Câu 11.** Trong kiến trúc Hexagonal, port `NotificationPort` do miền định nghĩa. Nếu sau này có 5 kênh (email, SMS, push, Slack, webhook) và mỗi lần gửi cần thử lần lượt tới khi thành công, logic "thử lần lượt + retry" nên nằm ở đâu: trong use case, trong một adapter tổng hợp, hay một decorator? Lập luận theo SRP và OCP.

**Câu 12.** Cho biết vì sao việc lạm dụng interface (mỗi class một interface `XxxImpl`) có thể **làm hại** khả năng đọc và bảo trì, dù nghe có vẻ "đúng SOLID". Nêu tiêu chí quyết định khi nào một class xứng đáng có interface.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Phần A</summary>

1. Vi phạm **SRP** (và kèm DIP). Bốn trục thay đổi: định dạng/nguồn HTTP, luật validate email, schema/DB, template + nhà cung cấp email. Ngoài ra nối chuỗi SQL trực tiếp là lỗ hổng SQL injection. Refactor: controller chỉ nhận request và gọi use case; validate ở domain; lưu qua `UserRepository`; gửi qua `WelcomeChannel`.
2. **Không đạt DIP.** Interface nằm trong package `com.app.billing` nhưng (a) trả về `com.stripe.model.Charge` — kiểu của nhà cung cấp rò vào nghiệp vụ; (b) tên và tham số (`stripeToken`) mô phỏng API Stripe; (c) thực chất phía nghiệp vụ vẫn phụ thuộc khái niệm của Stripe, chỉ thêm một lớp bọc. Đúng DIP: `PaymentGateway.charge(long amountCents, String customerRef)` trả `PaymentResult` của miền.
3. Client kỳ vọng: với `Rectangle`, `setWidth` không đổi `height`. `Square` làm mạnh mối liên hệ (đổi một cạnh đổi cả hai) → hàm `resize` đặt `width=5`, `height=10` rồi assert `area()==50` sẽ nhận `100` với `Square`. Precondition/invariant của `Rectangle` bị phá. Sửa: `Shape` bất biến với `area()`, `Rectangle` và `Square` là record riêng.
4. **Có.** Trộn CRUD cơ bản với nhập/xuất hàng loạt và reindex — client chỉ đọc theo id vẫn phụ thuộc `bulkImport`, `reindexSearch`. Tách: `ReadRepository<T>` (`findById`, `findAll`), `WriteRepository<T>` (`save`, `delete`), `BulkPort` (`bulkImport`, `exportCsv`), `SearchIndexPort` (`reindexSearch`).
5. Không luôn tốt hơn. `switch` vẫn đúng khi: tập trường hợp **nhỏ và ổn định** thuộc cùng một module; hoặc là `sealed hierarchy` với `switch` exhaustive để compiler ép cập nhật khi thêm subtype; hoặc chỉ là ánh xạ dữ liệu đơn giản.
6. Không kết luận chỉ bằng số lượng. Nếu 6 dependency cùng phục vụ **một** ca sử dụng (điều phối "đăng ký user") thì cohesion vẫn cao. Nó thành vấn đề khi các dependency thuộc các nhóm thay đổi khác nhau và method dùng các tập con rời rạc — khi đó tách use case.
7. Constructor injection cho field `final` (đối tượng bất biến, thay thế an toàn hơn theo tinh thần LSP), nêu phụ thuộc bắt buộc tường minh (thiếu là lỗi khởi tạo chứ không phải NPE runtime), và test dựng bằng `new` với test double không cần container. Field injection giấu phụ thuộc và buộc dùng reflection/framework để test.
8. Liên quan **LSP** (hành vi "chạy trong transaction" bị phá ngầm) và cơ chế **proxy AOP** của Spring: `@Transactional` chỉ có hiệu lực khi lời gọi đi qua proxy được inject; `this.buildRows()` gọi thẳng object thật, bỏ qua proxy. Sửa: tách `buildRows` sang bean khác, hoặc tự inject, hoặc dùng `TransactionTemplate`.

</details>

<details>
<summary>Phần B — ý chính</summary>

- **Bài 1:** `PlaceOrderUseCase` giữ `OrderRepository`, `InvoiceRenderer`, `OrderNotifier`, `AuditLog` là field `final` gán trong constructor. `execute(cmd)`: `var total = calculator.totalOf(order, country); repo.save(order); var pdf = renderer.render(order); notifier.confirm(order); audit.record("ORDER_PLACED", order.id());`. Phần tính thuế nằm trong `OrderCalculator` hoặc trong `Order` (domain).
- **Bài 2:** `interface DiscountPolicy { long apply(long amountCents); }`; `Map<CustomerType, DiscountPolicy> policies`; `calculate` tra map, mặc định `DiscountPolicy.none()` nếu không thấy. Thêm `PARTNER` = thêm một entry, `DiscountCalculator` không đổi.
- **Bài 3:** `interface Bird { void eat(); }`, `interface FlyingBird extends Bird { void fly(); }`. `Sparrow implements FlyingBird`, `Penguin implements Bird`. `void migrate(List<FlyingBird> flock) { flock.forEach(FlyingBird::fly); }` — `Penguin` không lọt vào danh sách nên không có exception.
- **Bài 4:** `Charger` (`charge`), `Refunder` (`refund`), `SubscriptionManager` (`subscribe`, `cancelSubscription`), `InvoiceIssuer` (`generateInvoice`), `WebhookHandler` (`handleWebhook`). `OneTimeCheckout` chỉ nhận `Charger` (và có thể `Refunder`).
- **Bài 5:** `abstract class KeyValueStoreContractTest { abstract KeyValueStore newStore(); @Test void putThenGet(){...} @Test void missingKeyEmpty(){...} @Test void putOverwrites(){...} }`; hai lớp con override `newStore()`. Cả hai implementation phải xanh cùng bộ test — đó là bằng chứng LSP.
- **Bài 6:** `Main` tạo `DataSource`, `new JdbcOrderRepository(ds)`, `new PdfInvoiceRenderer()`, `new EmailOrderNotifier(mailer)`, `new Slf4jAuditLog()`, rồi `new PlaceOrderUseCase(...)`. Grep toàn bộ package nghiệp vụ không thấy `import` hạ tầng nào.

</details>

<details>
<summary>Phần C</summary>

- **Câu 7:** Phía "bỏ": chưa có biến thiên thực tế, chưa có test dùng mock → interface hiện chỉ là chi phí gián tiếp (needless complexity). Phía "giữ": nếu interface nằm ở ranh giới hạ tầng (thanh toán) thì gần như chắc chắn sẽ cần test double khi viết test cho tầng nghiệp vụ; giữ để sẵn seam rẻ hơn thêm lại sau. Kết luận hợp lý: giữ nếu là ranh giới I/O ngoài; cân nhắc bỏ nếu chỉ là interface nội bộ không ranh giới.
- **Câu 8:** (a) `List.of` — `List` khai báo `add` là *optional operation* trong contract của chính JDK, nên về mặt chữ nghĩa không "vi phạm" nhưng là **contract kém** (ISP): gộp đọc + ghi khiến client chỉ đọc vẫn thấy `add`. (b) `ReadOnlyDocument.save()` — nếu `Document.save()` được hứa dùng được thì đây là **vi phạm LSP**. Thiết kế lại: `interface Document { ... }` chỉ đọc; `interface WritableDocument extends Document { void save(); }`; code cần lưu nhận `WritableDocument`.
- **Câu 9:** `@Order`/`Comparable`: đơn giản, nhưng thứ tự nằm rải rác ở từng class, khó nhìn tổng thể. Chia pha: rõ ràng về giai đoạn, nhưng cứng khi cần chèn pha mới. `ValidationPipeline` cấu hình: linh hoạt nhất, thứ tự tập trung một chỗ, nhưng thêm một khái niệm và một điểm cấu hình. Chọn theo số lượng validator và tần suất thay đổi thứ tự.
- **Câu 10:** `BankAccount` có nhiều method (`deposit`, `withdraw`, `freeze`) nhưng tất cả bảo vệ **một** invariant tài khoản → một trách nhiệm, cohesion cao; tách mỗi method thành class sẽ phá mô hình miền và giảm cohesion. Phát biểu lại: "một module chịu trách nhiệm trước **một actor / một nhóm yêu cầu thay đổi gắn kết**" — tiêu chí là *nguồn thay đổi*, không phải *số lượng method*.
- **Câu 11:** Logic "thử lần lượt + retry" là một chính sách gửi tin, không phải quy tắc nghiệp vụ đăng ký/đặt hàng → **không** đặt trong use case (giữ SRP cho use case). Đặt trong một **adapter tổng hợp** `CompositeNotificationAdapter implements NotificationPort` hoặc một **decorator** `RetryingNotification` bọc từng kênh. Thêm kênh thứ 6 chỉ là thêm vào danh sách bên trong adapter → giữ OCP cho use case và cho các adapter đơn lẻ.
- **Câu 12:** Mỗi interface một-impl thêm một bước gián tiếp khi đọc (phải nhảy tới `Impl`), làm phình số file, và tạo cảm giác linh hoạt không có thật. Tiêu chí một class xứng đáng có interface: (a) là ranh giới với hệ thống ngoài cần test double; (b) đã hoặc sắp có nhiều implementation thật; (c) là điểm mở rộng công khai cho module/plugin khác; (d) cần phá vòng phụ thuộc biên dịch giữa các module. Không có tiêu chí nào đúng thì để class cụ thể.

</details>

---

*File tiếp theo trong lộ trình: **Module 01.7 — equals, hashCode, toString** (hợp đồng của `Object`, vì sao ghi đè theo cặp, và ảnh hưởng tới `HashMap`/`HashSet`).*
