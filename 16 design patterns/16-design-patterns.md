# Module 08 — Design Patterns

> **Mức độ ưu tiên: Cao** — Spring Framework được xây dựng gần như hoàn toàn từ các pattern trong module này. Không học phần này thì việc đọc source code Spring (hoặc thậm chí chỉ đọc Javadoc) sẽ mãi mãi chỉ là "học thuộc annotation" thay vì thực sự hiểu **tại sao** framework thiết kế như vậy. Đây cũng là chủ đề rất hay bị hỏi ở phỏng vấn Middle/Senior dưới dạng "thiết kế 1 hệ thống X, bạn sẽ dùng pattern nào?".

---

## Mục lục

1. [Tổng quan 3 nhóm Design Pattern](#1-tổng-quan-3-nhóm-design-pattern)
2. [Singleton Pattern](#2-singleton-pattern)
3. [Factory Pattern](#3-factory-pattern)
4. [Builder Pattern](#4-builder-pattern)
5. [Proxy Pattern](#5-proxy-pattern)
6. [Adapter Pattern](#6-adapter-pattern)
7. [Dependency Injection & Inversion of Control](#7-dependency-injection--inversion-of-control)
8. [Strategy Pattern](#8-strategy-pattern)
9. [Observer Pattern](#9-observer-pattern)
10. [Template Method Pattern](#10-template-method-pattern)
11. [Chain of Responsibility Pattern](#11-chain-of-responsibility-pattern)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. Tổng quan 3 nhóm Design Pattern

| Nhóm | Giải quyết vấn đề gì | Pattern trong module này |
|---|---|---|
| **Creational** (Khởi tạo) | **Cách tạo object** linh hoạt, kiểm soát, tránh phụ thuộc trực tiếp vào `new ConcreteClass()` | Singleton, Factory, Builder |
| **Structural** (Cấu trúc) | Cách **kết hợp/tổ chức** class và object thành cấu trúc lớn hơn, linh hoạt hơn | Proxy, Adapter |
| **Behavioral** (Hành vi) | Cách các object **giao tiếp, phân chia trách nhiệm** với nhau | Strategy, Observer, Template Method, Chain of Responsibility |

---

## 2. Singleton Pattern

**Mục đích:** đảm bảo 1 class chỉ có **DUY NHẤT 1 instance** trong toàn bộ vòng đời ứng dụng, cung cấp 1 điểm truy cập toàn cục (global access point) đến instance đó.

Đã làm quen sơ ở Module 01.3 (Bài 5) — đây là phiên bản đầy đủ hơn.

### Cách viết CƠ BẢN (chưa an toàn với đa luồng)

```java
public class AppConfig {
    private static AppConfig instance;
    private AppConfig() { } // constructor PRIVATE — chặn tạo object từ bên ngoài bằng "new"

    public static AppConfig getInstance() {
        if (instance == null) { // ⚠️ KHÔNG thread-safe — nhiều thread có thể cùng vượt qua check này TRƯỚC KHI instance được gán
            instance = new AppConfig();
        }
        return instance;
    }
}
```

### Cách viết THREAD-SAFE — liên hệ trực tiếp Module 05.1 (synchronized)

```java
public class AppConfig {
    private static volatile AppConfig instance; // "volatile" — đảm bảo VISIBILITY giữa các thread (Module 05.1)
    private AppConfig() { }

    public static AppConfig getInstance() {
        if (instance == null) {                    // check 1 (không lock) — tối ưu hiệu năng, tránh lock không cần thiết ở đa số lần gọi
            synchronized (AppConfig.class) {        // chỉ LOCK khi CẦN THIẾT
                if (instance == null) {              // check 2 (BÊN TRONG lock) — đảm bảo KHÔNG bị 2 thread cùng tạo 2 instance
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }
}
```
Kỹ thuật này gọi là **"Double-Checked Locking"** — kiểm tra `null` **2 lần** để vừa đảm bảo thread-safe, vừa **không phải lock mỗi lần gọi** `getInstance()` (chỉ lock ở lần khởi tạo đầu tiên, các lần sau instance đã có sẵn, check đầu tiên trả về `false` ngay, không cần vào `synchronized`).

### Cách viết ĐƠN GIẢN & AN TOÀN NHẤT — Eager Initialization

```java
public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig(); // khởi tạo NGAY khi class được nạp (Module 07 — ClassLoader)
    private AppConfig() { }

    public static AppConfig getInstance() {
        return INSTANCE; // JVM đảm bảo class chỉ được nạp/khởi tạo field static ĐÚNG 1 LẦN — tự động thread-safe, không cần "synchronized"
    }
}
```
> **Đánh đổi:** Eager Initialization đơn giản và an toàn tuyệt đối, nhưng object được tạo **ngay khi class được nạp**, dù có thể chưa bao giờ thực sự cần dùng đến (tốn tài nguyên nếu việc khởi tạo phức tạp/tốn kém). Double-Checked Locking phức tạp hơn nhưng chỉ tạo instance **khi thực sự cần** (lazy).

### Liên hệ trực tiếp Spring Framework — Singleton Scope

```java
@Component // hoặc @Service, @Repository...
public class UserService {
    // Mặc định, Spring Bean là SINGLETON trong phạm vi ApplicationContext (Module 12)
}
```
> **Đây chính là ứng dụng thực tế lớn nhất của Singleton Pattern trong công việc backend hàng ngày:** mỗi `@Component`/`@Service`/`@Repository` trong Spring, theo mặc định, **chỉ có 1 instance duy nhất** được tạo và quản lý bởi Spring Container (`ApplicationContext`) — bạn **không bao giờ** tự viết code Singleton thủ công như trên trong dự án Spring Boot thực tế, vì Spring đã làm điều đó **tự động** cho bạn thông qua cơ chế Bean Scope (sẽ học kỹ ở Module 12).

---

## 3. Factory Pattern

**Mục đích:** tách biệt **logic tạo object** ra khỏi code sử dụng object đó — người dùng chỉ cần gọi Factory, không cần biết chi tiết **class cụ thể nào** đang được tạo ra.

### Simple Factory (chưa phải Pattern "chính thức" theo GoF, nhưng rất hay dùng thực tế)

```java
public interface NotificationSender {
    void send(String message);
}
public class EmailSender implements NotificationSender {
    public void send(String message) { System.out.println("Gửi Email: " + message); }
}
public class SmsSender implements NotificationSender {
    public void send(String message) { System.out.println("Gửi SMS: " + message); }
}

public class NotificationFactory {
    public static NotificationSender create(String type) { // TẬP TRUNG logic quyết định tạo class NÀO
        return switch (type) { // liên hệ switch expression, Module 01.2
            case "EMAIL" -> new EmailSender();
            case "SMS" -> new SmsSender();
            default -> throw new IllegalArgumentException("Loại không hỗ trợ: " + type);
        };
    }
}
```

```java
NotificationSender sender = NotificationFactory.create("EMAIL"); // caller KHÔNG cần biết class cụ thể là EmailSender
sender.send("Xin chào!");
```

> **Liên hệ trực tiếp SOLID (Module 02.3):** đây chính là ứng dụng thực tế của **Dependency Inversion Principle** — code gọi (`sender.send(...)`) chỉ phụ thuộc vào `interface NotificationSender`, không phụ thuộc trực tiếp vào `EmailSender`/`SmsSender` cụ thể — logic quyết định "dùng class nào" được **tập trung** ở 1 nơi duy nhất (Factory), dễ mở rộng thêm loại mới mà không sửa code nơi khác đang sử dụng (Open/Closed Principle).

### Factory Method Pattern (phiên bản "chuẩn" GoF hơn — dùng kế thừa)

```java
public abstract class NotificationCreator {
    public abstract NotificationSender createSender(); // subclass QUYẾT ĐỊNH tạo loại nào

    public void notify(String message) { // logic CHUNG, không đổi giữa các subclass
        NotificationSender sender = createSender();
        sender.send(message);
    }
}

public class EmailNotificationCreator extends NotificationCreator {
    @Override
    public NotificationSender createSender() { return new EmailSender(); }
}
```

### Liên hệ trực tiếp Spring Framework

```java
@Bean // Method được đánh dấu @Bean CHÍNH LÀ 1 "Factory Method" — Spring gọi method này để TẠO object, quản lý vòng đời của nó
public NotificationSender notificationSender() {
    return new EmailSender();
}
```
> `BeanFactory`/`ApplicationContext` (sẽ học ở Module 12) — chính tên gọi đã tiết lộ rõ ràng: **toàn bộ Spring Container về bản chất là 1 "Factory" khổng lồ**, chịu trách nhiệm tạo và quản lý mọi Bean trong ứng dụng, thay vì để lập trình viên tự `new` thủ công khắp nơi.

---

## 4. Builder Pattern

**Mục đích:** xây dựng object **phức tạp, nhiều tham số tùy chọn** một cách **dễ đọc, rõ ràng**, tránh tình trạng constructor có **quá nhiều tham số** (gọi là "Telescoping Constructor Anti-pattern").

### Vấn đề: constructor quá nhiều tham số

```java
// ❌ Anti-pattern — KHÓ ĐỌC, dễ TRUYỀN NHẦM THỨ TỰ tham số (đặc biệt khi nhiều tham số CÙNG KIỂU)
public class Pizza {
    public Pizza(String size, boolean cheese, boolean pepperoni, boolean mushroom, boolean onion, boolean extraSauce) { ... }
}
Pizza p = new Pizza("Large", true, false, true, false, true); // đọc vào KHÔNG BIẾT true/false nào ứng với topping nào!
```

### Áp dụng Builder Pattern

```java
public class Pizza {
    private final String size;         // final — immutable sau khi build xong (liên hệ record, Module 06)
    private final boolean cheese;
    private final boolean pepperoni;
    private final boolean mushroom;

    private Pizza(Builder builder) { // constructor PRIVATE — chỉ Builder mới gọi được
        this.size = builder.size;
        this.cheese = builder.cheese;
        this.pepperoni = builder.pepperoni;
        this.mushroom = builder.mushroom;
    }

    public static class Builder { // static nested class — tách biệt logic "xây dựng" khỏi object "kết quả"
        private String size = "Medium"; // giá trị MẶC ĐỊNH — chỉ cần override khi thực sự cần khác
        private boolean cheese = false;
        private boolean pepperoni = false;
        private boolean mushroom = false;

        public Builder size(String size) { this.size = size; return this; } // trả về "this" — cho phép GỌI NỐI TIẾP (method chaining)
        public Builder cheese(boolean cheese) { this.cheese = cheese; return this; }
        public Builder pepperoni(boolean pepperoni) { this.pepperoni = pepperoni; return this; }
        public Builder mushroom(boolean mushroom) { this.mushroom = mushroom; return this; }

        public Pizza build() { // method CUỐI CÙNG — tạo ra object THỰC SỰ
            return new Pizza(this);
        }
    }
}
```

```java
Pizza pizza = new Pizza.Builder()
    .size("Large")
    .cheese(true)
    .mushroom(true)
    .build(); // CHỈ CẦN set những gì THỰC SỰ cần, thứ tự KHÔNG quan trọng, code TỰ GIẢI THÍCH Ý NGHĨA
```

> **Liên hệ thực tế cực kỳ phổ biến:** `StringBuilder` (Module 01.1), `UriComponentsBuilder` trong Spring Web, các thư viện HTTP Client hiện đại (`HttpRequest.Builder` trong `java.net.http`), hay khi tự viết DTO/Entity phức tạp trong dự án Spring Boot — Builder Pattern xuất hiện **khắp nơi** trong Java ecosystem vì giải quyết rất tốt bài toán "object có nhiều tham số tùy chọn".

---

## 5. Proxy Pattern

**Mục đích:** cung cấp 1 object **"đại diện" (Proxy)** đứng trước object thật (**"Real Subject"**), kiểm soát việc truy cập vào nó — có thể thêm logic **trước/sau** khi gọi method thật, mà **KHÔNG cần sửa code của object thật**.

```java
public interface Database { // interface chung — cả Proxy VÀ RealSubject đều implement
    void query(String sql);
}

public class RealDatabase implements Database { // "Real Subject" — logic nghiệp vụ THẬT
    public void query(String sql) {
        System.out.println("Đang thực thi query: " + sql);
    }
}

public class DatabaseProxy implements Database { // "Proxy" — CÙNG interface, nhưng "bọc" thêm logic
    private RealDatabase realDatabase;

    public void query(String sql) {
        System.out.println("[LOG] Chuẩn bị query: " + sql); // logic THÊM TRƯỚC khi gọi thật
        long start = System.currentTimeMillis();

        if (realDatabase == null) { // LAZY INITIALIZATION — chỉ tạo object THẬT khi thực sự cần dùng lần đầu
            realDatabase = new RealDatabase();
        }
        realDatabase.query(sql); // ỦY QUYỀN (delegate) cho object THẬT xử lý

        System.out.println("[LOG] Query hoàn thành sau " + (System.currentTimeMillis() - start) + "ms"); // logic THÊM SAU
    }
}
```

```java
Database db = new DatabaseProxy(); // code gọi KHÔNG BIẾT (và KHÔNG CẦN BIẾT) đây là Proxy hay RealDatabase thật
db.query("SELECT * FROM users"); // tự động có thêm logging + đo thời gian, mà RealDatabase KHÔNG hề bị sửa đổi code
```

### Liên hệ trực tiếp Spring Framework — **CỰC KỲ quan trọng**

**Spring AOP (Aspect-Oriented Programming, Module 12)** hoạt động **hoàn toàn dựa trên Proxy Pattern**. Khi dùng:

```java
@Service
public class OrderService {
    @Transactional // Module 14 — Spring Data JPA
    public void placeOrder(Order order) {
        // logic nghiệp vụ
    }
}
```

Spring **KHÔNG** gọi trực tiếp `placeOrder()` trên object `OrderService` thật — Spring **tự động tạo ra 1 Proxy** đứng trước `OrderService`, Proxy này thực hiện: **mở transaction (trước)** → gọi method thật → **commit/rollback transaction (sau)**, hoàn toàn tương tự cấu trúc `DatabaseProxy` ở trên. Đây chính xác là cơ chế đứng sau `@Transactional`, `@Cacheable`, `@Async`, và mọi annotation "kỳ diệu" khác của Spring — **tất cả đều là Proxy Pattern được tự động hóa**.

---

## 6. Adapter Pattern

**Mục đích:** "chuyển đổi" giao diện (interface) của 1 class **KHÔNG tương thích** thành giao diện mà code hiện tại **mong đợi** — giống như "adapter cắm điện" chuyển đổi giữa các chuẩn ổ cắm khác nhau ở đời thực.

```java
// Interface HỆ THỐNG MỚI mong đợi
public interface PaymentProcessor {
    void processPayment(double amountInUSD);
}

// Thư viện BÊN THỨ 3 (KHÔNG THỂ sửa code, có interface KHÁC)
public class LegacyPaymentGateway {
    public void makeTransaction(String currencyCode, long amountInCents) {
        System.out.println("Giao dịch " + amountInCents + " cents (" + currencyCode + ")");
    }
}

// ADAPTER — "dịch" giữa 2 interface không tương thích
public class PaymentAdapter implements PaymentProcessor {
    private LegacyPaymentGateway legacyGateway;

    public PaymentAdapter(LegacyPaymentGateway legacyGateway) {
        this.legacyGateway = legacyGateway;
    }

    @Override
    public void processPayment(double amountInUSD) { // interface MỚI mà hệ thống hiện tại dùng
        long amountInCents = Math.round(amountInUSD * 100); // "DỊCH" định dạng dữ liệu
        legacyGateway.makeTransaction("USD", amountInCents); // gọi API CŨ với định dạng phù hợp
    }
}
```

```java
PaymentProcessor processor = new PaymentAdapter(new LegacyPaymentGateway());
processor.processPayment(99.99); // code MỚI dùng interface QUEN THUỘC, không cần biết chi tiết thư viện cũ bên dưới
```

> **Tình huống thực tế hay gặp nhất:** khi tích hợp 1 thư viện/SDK bên thứ 3 (payment gateway, SMS provider, thư viện logging cũ...) có interface **không khớp** với kiến trúc hệ thống hiện tại — thay vì sửa code khắp nơi để "ép" dùng theo interface cũ, viết 1 lớp Adapter **duy nhất** để "dịch" qua lại, giữ cho phần còn lại của hệ thống sạch sẽ, nhất quán.

---

## 7. Dependency Injection & Inversion of Control

Đây **không hẳn là 1 pattern GoF truyền thống** riêng biệt, mà là **nguyên lý kiến trúc** — nhưng quan trọng đến mức phải nhắc lại ở đây, vì nó là **nền tảng của mọi thứ Spring làm**.

Đã học chi tiết cơ chế và ví dụ đầy đủ ở **Module 02.3 (Dependency Inversion Principle)** — tại đây chỉ nhắc lại điểm mấu chốt trong ngữ cảnh Design Pattern:

```java
// KHÔNG có DI — class TỰ TẠO dependency của chính nó (High coupling)
public class OrderService {
    private EmailSender sender = new EmailSender(); // ❌ TỰ new — khó thay thế, khó test
}

// CÓ DI — dependency được "TIÊM" TỪ BÊN NGOÀI vào (Inversion of Control)
public class OrderService {
    private final NotificationSender sender;
    public OrderService(NotificationSender sender) { // ✅ nhận từ NGOÀI — Spring Container sẽ tự động làm việc này
        this.sender = sender;
    }
}
```

**"Inversion of Control" (IoC)** nghĩa là: thay vì `OrderService` **tự kiểm soát** việc tạo dependency của nó (cách truyền thống), quyền kiểm soát này được **"đảo ngược"** — giao cho 1 thực thể **BÊN NGOÀI** (Spring Container, hoặc bất kỳ "IoC Container" nào) quyết định **tạo gì, khi nào, và tiêm vào đâu**. Đây chính là chủ đề trọng tâm của **Module 12 — Spring Framework Core**.

---

## 8. Strategy Pattern

Đã làm quen sơ ở Module 02.3 (áp dụng OCP cho hệ thống giảm giá) — đây là phần trình bày độc lập, đầy đủ hơn.

**Mục đích:** định nghĩa **1 họ các thuật toán (algorithm)** có thể **hoán đổi cho nhau (interchangeable)** tại runtime, thay vì "cứng" 1 thuật toán duy nhất bằng `if/else`.

```java
public interface SortStrategy {
    void sort(int[] array);
}
public class BubbleSort implements SortStrategy {
    public void sort(int[] array) { System.out.println("Sắp xếp bằng Bubble Sort"); /* logic */ }
}
public class QuickSort implements SortStrategy {
    public void sort(int[] array) { System.out.println("Sắp xếp bằng Quick Sort"); /* logic */ }
}

public class Sorter {
    private SortStrategy strategy; // THAM CHIẾU đến interface, KHÔNG biết cụ thể thuật toán nào

    public Sorter(SortStrategy strategy) { this.strategy = strategy; } // strategy được "TIÊM" vào — chính là DI!

    public void executeSort(int[] array) {
        strategy.sort(array); // ỦY QUYỀN việc sắp xếp cho strategy CỤ THỂ được chọn
    }
}
```

```java
Sorter sorter = new Sorter(new QuickSort()); // chọn strategy LÚC RUNTIME, có thể đổi bất kỳ lúc nào
sorter.executeSort(new int[]{5, 2, 8, 1});

sorter = new Sorter(new BubbleSort()); // đổi strategy KHÁC, KHÔNG cần sửa class Sorter
```

> **Liên hệ Spring:** khi có **nhiều implementation** của cùng 1 interface (`@Service` khác nhau cùng implement `PaymentService`), và Spring tự động **chọn đúng implementation** để "tiêm" vào dựa trên `@Qualifier` hoặc `@Primary` — đây chính là Strategy Pattern được Spring quản lý tự động.

---

## 9. Observer Pattern

**Mục đích:** khi 1 object (**Subject**) thay đổi trạng thái, **TỰ ĐỘNG thông báo** cho tất cả các object **quan sát nó (Observer)** đã đăng ký theo dõi — mà Subject **không cần biết chi tiết** từng Observer sẽ làm gì với thông báo đó.

```java
public interface OrderObserver { // "Observer" — bên MUỐN được thông báo
    void onOrderPlaced(Order order);
}

public class EmailNotifier implements OrderObserver {
    public void onOrderPlaced(Order order) { System.out.println("Gửi email xác nhận đơn hàng #" + order.getId()); }
}
public class InventoryUpdater implements OrderObserver {
    public void onOrderPlaced(Order order) { System.out.println("Cập nhật tồn kho cho đơn hàng #" + order.getId()); }
}

public class OrderService { // "Subject" — bên PHÁT SINH sự kiện
    private List<OrderObserver> observers = new ArrayList<>();

    public void addObserver(OrderObserver observer) { observers.add(observer); } // ĐĂNG KÝ theo dõi

    public void placeOrder(Order order) {
        // ... logic tạo đơn hàng ...
        notifyObservers(order); // THÔNG BÁO cho TẤT CẢ observer đã đăng ký
    }

    private void notifyObservers(Order order) {
        for (OrderObserver observer : observers) {
            observer.onOrderPlaced(order); // MỖI observer TỰ QUYẾT ĐỊNH xử lý gì — OrderService KHÔNG CẦN BIẾT
        }
    }
}
```

```java
OrderService service = new OrderService();
service.addObserver(new EmailNotifier());
service.addObserver(new InventoryUpdater());
service.placeOrder(new Order(1L)); // TỰ ĐỘNG kích hoạt CẢ HAI observer, KHÔNG cần gọi từng cái thủ công
```

> **Liên hệ Spring — `ApplicationEventPublisher`:** Spring cung cấp sẵn cơ chế Event/Listener (`@EventListener`) xây dựng chính xác trên Observer Pattern — cho phép các phần khác nhau của ứng dụng **phản ứng** với sự kiện nghiệp vụ (đơn hàng mới, user đăng ký...) mà **không bị coupling chặt** với nhau, đúng tinh thần Single Responsibility + Open/Closed Principle đã học ở Module 02.3.

---

## 10. Template Method Pattern

**Mục đích:** định nghĩa **khung xương (skeleton)** của 1 thuật toán trong 1 method ở lớp cha, **để trống một số bước cụ thể** cho subclass tự override — nhưng **thứ tự và cấu trúc tổng thể** luôn được lớp cha kiểm soát, không đổi.

```java
public abstract class DataProcessor { // lớp cha — chứa "khung xương" thuật toán
    public final void process() { // "final" — KHÔNG cho subclass override THỨ TỰ các bước
        readData();
        processData(); // bước này ĐỂ TRỐNG cho subclass tự định nghĩa
        writeData();
    }

    private void readData() { System.out.println("Đọc dữ liệu từ nguồn"); } // bước CHUNG, không đổi
    protected abstract void processData(); // bước RIÊNG — mỗi subclass xử lý khác nhau
    private void writeData() { System.out.println("Ghi kết quả ra đích"); } // bước CHUNG, không đổi
}

public class CsvDataProcessor extends DataProcessor {
    @Override
    protected void processData() { System.out.println("Xử lý dữ liệu theo định dạng CSV"); }
}
public class JsonDataProcessor extends DataProcessor {
    @Override
    protected void processData() { System.out.println("Xử lý dữ liệu theo định dạng JSON"); }
}
```

```java
DataProcessor processor = new CsvDataProcessor();
processor.process(); // "Đọc dữ liệu..." → "Xử lý theo CSV" → "Ghi kết quả..." — THỨ TỰ LUÔN CỐ ĐỊNH, chỉ bước giữa thay đổi
```

> **Phân biệt với Strategy Pattern:** Template Method dùng **kế thừa** (`extends`, override 1 phần method) — thứ tự các bước **cố định** ở lớp cha. Strategy Pattern dùng **composition** (tiêm interface vào qua constructor/field) — **toàn bộ thuật toán** có thể thay thế hoàn toàn, linh hoạt hơn nhưng không "khóa" được cấu trúc chung như Template Method.

---

## 11. Chain of Responsibility Pattern

**Mục đích:** cho phép **nhiều object xử lý** cùng 1 request theo dạng **"chuỗi"** — mỗi object trong chuỗi tự quyết định: **xử lý request** rồi dừng lại, hoặc **chuyển tiếp** cho object kế tiếp trong chuỗi.

```java
public abstract class Middleware { // mỗi "mắt xích" trong chuỗi
    protected Middleware next; // tham chiếu đến MẮT XÍCH KẾ TIẾP

    public void setNext(Middleware next) { this.next = next; }

    public abstract boolean handle(String request);

    protected boolean callNext(String request) { // gọi tiếp mắt xích SAU, nếu có
        if (next == null) return true;
        return next.handle(request);
    }
}

public class AuthenticationMiddleware extends Middleware {
    @Override
    public boolean handle(String request) {
        System.out.println("Kiểm tra xác thực...");
        if (!request.contains("token")) {
            System.out.println("❌ Từ chối — thiếu token xác thực");
            return false; // DỪNG chuỗi TẠI ĐÂY, không gọi tiếp
        }
        return callNext(request); // XÁC THỰC OK — chuyển tiếp cho mắt xích kế tiếp
    }
}

public class RateLimitMiddleware extends Middleware {
    @Override
    public boolean handle(String request) {
        System.out.println("Kiểm tra rate limit...");
        // giả sử luôn pass để đơn giản hóa ví dụ
        return callNext(request);
    }
}
```

```java
Middleware auth = new AuthenticationMiddleware();
Middleware rateLimit = new RateLimitMiddleware();
auth.setNext(rateLimit); // XÂY CHUỖI: auth → rateLimit

boolean result = auth.handle("GET /api/orders?token=abc123"); // đi qua LẦN LƯỢT từng mắt xích
```

> **Liên hệ Spring — cực kỳ trực tiếp:** đây chính xác là cơ chế đứng sau **Servlet Filter Chain** và **Spring Security Filter Chain** (sẽ học kỹ ở Module 15 — Spring Security)! Mỗi request HTTP đi qua **1 chuỗi Filter** (xác thực, CORS, logging, rate limiting...) — mỗi Filter tự quyết định xử lý request rồi dừng lại (ví dụ từ chối vì thiếu quyền truy cập) hoặc gọi `chain.doFilter()` để chuyển tiếp cho Filter kế tiếp — cấu trúc **giống hệt** ví dụ trên.

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Pattern | Nhóm | Vấn đề giải quyết | Ứng dụng trực tiếp trong Spring |
|---|---|---|---|
| **Singleton** | Creational | Đảm bảo chỉ có 1 instance | Bean scope mặc định của Spring Container |
| **Factory** | Creational | Tách logic tạo object khỏi code sử dụng | `@Bean` method, `BeanFactory`/`ApplicationContext` |
| **Builder** | Creational | Object nhiều tham số tùy chọn, dễ đọc | `StringBuilder`, `UriComponentsBuilder` |
| **Proxy** | Structural | Thêm logic trước/sau mà không sửa object thật | Spring AOP — nền tảng của `@Transactional`, `@Cacheable`, `@Async` |
| **Adapter** | Structural | "Dịch" giữa 2 interface không tương thích | Tích hợp thư viện/SDK bên thứ 3 |
| **Strategy** | Behavioral | Hoán đổi thuật toán linh hoạt tại runtime | Nhiều implementation của cùng 1 interface, `@Qualifier` |
| **Observer** | Behavioral | Thông báo tự động khi trạng thái thay đổi | `ApplicationEventPublisher`, `@EventListener` |
| **Template Method** | Behavioral | Khung thuật toán cố định, chi tiết linh hoạt | `JdbcTemplate`, các lớp `*Template` trong Spring |
| **Chain of Responsibility** | Behavioral | Xử lý request qua chuỗi nhiều bước | Servlet Filter Chain, Spring Security Filter Chain |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code Singleton sau có an toàn với đa luồng không? Giải thích.
```java
public class Config {
    private static Config instance;
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }
}
```

**Câu 2.** Phân biệt Factory Pattern và Builder Pattern — khi nào chọn cái nào? Cho 1 ví dụ tình huống cụ thể cho mỗi pattern.

**Câu 3.** Vì sao nói `@Transactional` của Spring "về bản chất là Proxy Pattern"? Giải thích cơ chế ở mức khái niệm.

**Câu 4.** So sánh Strategy Pattern và Template Method Pattern — điểm khác biệt cốt lõi về cách "linh hoạt hóa" hành vi là gì?

**Câu 5.** Chain of Responsibility Pattern và Servlet Filter Chain có điểm chung gì về mặt cấu trúc? Nêu ít nhất 2 điểm tương đồng.

---

### Phần B — Bài tập viết code

**Bài 1 — Singleton hoàn chỉnh cho hệ thống Logger.**
Viết class `AppLogger` theo Singleton Pattern (dùng kỹ thuật Double-Checked Locking hoặc Eager Initialization — tự chọn và giải thích lý do chọn), có method `log(String message)` in ra kèm timestamp. Viết `main` chứng minh gọi `AppLogger.getInstance()` nhiều lần từ nhiều thread khác nhau vẫn luôn trả về **cùng 1 object** (dùng `==` so sánh).

**Bài 2 — Factory Method cho hệ thống xử lý file.**
Viết `interface FileParser` với method `parse(String filePath)`. Cài đặt `CsvFileParser`, `JsonFileParser`, `XmlFileParser`. Viết `class FileParserFactory` với static method `create(String fileExtension)` trả về đúng parser dựa trên đuôi file (`.csv`, `.json`, `.xml`), ném `IllegalArgumentException` nếu không hỗ trợ định dạng đó (liên hệ Module 04 — Custom Exception).

**Bài 3 — Builder Pattern cho Entity phức tạp.**
Viết class `HttpRequestConfig` với nhiều tham số tùy chọn: `url` (bắt buộc), `method` (mặc định "GET"), `headers` (Map, mặc định rỗng), `timeout` (mặc định 5000ms), `retryCount` (mặc định 0). Dùng Builder Pattern với method chaining để tạo object, đảm bảo `url` là bắt buộc (ném exception trong `build()` nếu `url` chưa được set).

**Bài 4 — Proxy Pattern mô phỏng Caching.**
Viết `interface DataService` với method `String fetchData(String key)`. Viết `RealDataService` giả lập việc lấy dữ liệu tốn thời gian (`Thread.sleep(1000)` rồi trả về `"Data for " + key"`). Viết `CachingProxy implements DataService` — nếu `key` đã có trong cache nội bộ (`Map<String, String>`), trả về NGAY từ cache (không gọi `RealDataService`, không tốn 1000ms); nếu chưa có, gọi `RealDataService`, lưu kết quả vào cache trước khi trả về. Đo thời gian để chứng minh lần gọi thứ 2 với cùng `key` nhanh hơn NHIỀU so với lần đầu.

**Bài 5 — Bài toán tổng hợp: Xây dựng hệ thống xử lý đơn hàng kết hợp NHIỀU Pattern.**
Xây dựng hệ thống xử lý đơn hàng kết hợp:
- **Chain of Responsibility:** chuỗi validate gồm `StockCheckMiddleware` (kiểm tra tồn kho) → `PaymentValidationMiddleware` (kiểm tra thông tin thanh toán) → `FraudCheckMiddleware` (kiểm tra gian lận giả lập đơn giản) — mỗi middleware có thể "chặn" đơn hàng và dừng chuỗi.
- **Observer:** sau khi đơn hàng đi qua HẾT chuỗi validate mà không bị chặn, thông báo cho `EmailObserver` và `InventoryObserver` (giống mục 9).
- **Strategy:** áp dụng chiến lược tính phí ship khác nhau (`StandardShipping`, `ExpressShipping` — liên hệ lại Module 02.3 Bài 2) trước khi hoàn tất đơn hàng.

Viết `main` mô phỏng 2 đơn hàng: 1 đơn **hợp lệ** (đi qua hết chuỗi, kích hoạt observer, tính phí ship) và 1 đơn **bị chặn** ở `PaymentValidationMiddleware` (chứng minh chuỗi dừng đúng lúc, các observer KHÔNG bị gọi).

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Không an toàn** — nếu 2 thread cùng gọi `getInstance()` gần như đồng thời khi `instance` còn `null`, cả 2 đều có thể vượt qua điều kiện `if (instance == null)` **TRƯỚC KHI** bất kỳ thread nào kịp gán giá trị — dẫn đến việc tạo ra **2 instance khác nhau**, vi phạm chính mục đích của Singleton Pattern. Đây là Race Condition điển hình (liên hệ Module 05.1) — cần Double-Checked Locking hoặc Eager Initialization để sửa.
2. **Factory Pattern**: tập trung vào việc **QUYẾT ĐỊNH tạo class NÀO** trong số nhiều lựa chọn có sẵn — ví dụ chọn `EmailSender` hay `SmsSender` dựa trên tham số đầu vào. **Builder Pattern**: tập trung vào việc **XÂY DỰNG DẦN DẦN** 1 object DUY NHẤT có nhiều tham số tùy chọn phức tạp — ví dụ tạo 1 object `HttpRequestConfig` với nhiều field optional. Hai pattern giải quyết 2 vấn đề khác nhau, có thể **kết hợp cùng lúc** trong 1 hệ thống thực tế (ví dụ Factory trả về đúng LOẠI Builder cần dùng).
3. Vì khi Spring thấy annotation `@Transactional` trên 1 method, nó **không** để code gọi trực tiếp vào object thật — Spring **tự động tạo ra 1 Proxy object** bọc quanh object thật đó. Khi code gọi `orderService.placeOrder(...)`, thực chất đang gọi vào **Proxy**, Proxy này thực hiện: mở transaction (logic THÊM TRƯỚC) → gọi method thật của `OrderService` → commit hoặc rollback transaction tùy có exception hay không (logic THÊM SAU) — đúng cấu trúc "bọc thêm logic trước/sau mà không sửa code thật" của Proxy Pattern.
4. Strategy Pattern linh hoạt hóa bằng **composition** — tiêm 1 implementation HOÀN TOÀN KHÁC của cùng interface vào (thay thế TOÀN BỘ thuật toán). Template Method linh hoạt hóa bằng **kế thừa** — chỉ 1 vài BƯỚC CON trong 1 khung thuật toán CỐ ĐỊNH được phép tùy biến, thứ tự tổng thể và các bước còn lại luôn giữ nguyên do lớp cha kiểm soát.
5. Điểm chung: (a) **cấu trúc chuỗi** — mỗi phần tử tự quyết định xử lý rồi dừng lại hoặc chuyển tiếp cho phần tử kế tiếp; (b) **tính "trong suốt" (transparent)** với request gốc — request đi qua từng mắt xích mà không biết trước có bao nhiêu bước, mỗi bước độc lập, có thể thêm/bớt/sắp xếp lại thứ tự các Filter mà không ảnh hưởng logic của các Filter khác — đúng tinh thần Open/Closed Principle (Module 02.3).

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Nên ưu tiên **Eager Initialization** cho trường hợp Logger (đơn giản, không tốn kém tài nguyên khi khởi tạo, và Logger thường CHẮC CHẮN sẽ được dùng trong ứng dụng) — đơn giản hơn Double-Checked Locking mà vẫn đảm bảo an toàn tuyệt đối.
- **Bài 3:** Ví dụ cách viết `build()` với validate:
```java
public HttpRequestConfig build() {
    if (url == null || url.isBlank()) {
        throw new IllegalStateException("URL là bắt buộc, không được để trống");
    }
    return new HttpRequestConfig(this);
}
```
- **Bài 4:** Kết quả mong đợi: lần gọi đầu (`fetchData("user1")`) mất ~1000ms (do phải gọi `RealDataService`); lần gọi lại VỚI CÙNG key mất gần như 0ms (chỉ đọc từ `Map` cache nội bộ) — minh họa trực quan giá trị thực tế của Proxy Pattern trong việc thêm tính năng caching **mà không cần sửa 1 dòng code nào** của `RealDataService` gốc.
- **Bài 5:** Đây là bài tập **tổng hợp quan trọng nhất** của module — chứng minh rằng các Design Pattern **hiếm khi đứng riêng lẻ** trong hệ thống thực tế, mà thường **kết hợp với nhau** để giải quyết bài toán nghiệp vụ phức tạp. Đây cũng chính là mô hình thu nhỏ của luồng xử lý 1 request thực tế trong Spring Boot: đi qua Filter Chain (Chain of Responsibility) → Service xử lý nghiệp vụ, publish event (Observer) → áp dụng business rule khác nhau tùy tình huống (Strategy) — hoàn thành tốt bài này nghĩa là đã có tư duy kiến trúc vững vàng để bắt đầu Module 12 (Spring Framework Core).

</details>

---

*File tiếp theo trong lộ trình: **Module 09 — Build Tools & Quản lý dự án** (Maven, Gradle, Git & quy trình làm việc nhóm).*
