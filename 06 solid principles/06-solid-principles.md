# Module 02.3 — SOLID Principles

> **Mức độ ưu tiên: Cao** — SOLID là bộ nguyên lý thiết kế OOP quan trọng nhất trong lập trình backend hiện đại. Đây **không phải** kiến thức "học cho biết" — nó là **nền tảng tư duy trực tiếp** giải thích tại sao Spring Framework thiết kế Dependency Injection, `@Autowired`, interface-based programming như vậy. Câu hỏi "Giải thích SOLID với ví dụ thực tế" gần như chắc chắn xuất hiện ở phỏng vấn Middle/Senior.

---

## Mục lục

1. [Tổng quan SOLID](#1-tổng-quan-solid)
2. [S — Single Responsibility Principle](#2-s--single-responsibility-principle)
3. [O — Open/Closed Principle](#3-o--openclosed-principle)
4. [L — Liskov Substitution Principle](#4-l--liskov-substitution-principle)
5. [I — Interface Segregation Principle](#5-i--interface-segregation-principle)
6. [D — Dependency Inversion Principle](#6-d--dependency-inversion-principle)
7. [SOLID và Spring Framework — mối liên hệ trực tiếp](#7-solid-và-spring-framework--mối-liên-hệ-trực-tiếp)
8. [Tổng kết — Bảng ghi nhớ nhanh](#8-tổng-kết--bảng-ghi-nhớ-nhanh)
9. [Bài tập luyện tập](#9-bài-tập-luyện-tập)

---

## 1. Tổng quan SOLID

SOLID là 5 nguyên lý thiết kế hướng đối tượng do Robert C. Martin (Uncle Bob) tổng hợp, giúp code **dễ mở rộng, dễ bảo trì, dễ test**, giảm thiểu việc sửa 1 chỗ làm hỏng nhiều chỗ khác (hiệu ứng domino).

| Chữ cái | Tên đầy đủ | Câu hỏi cốt lõi |
|---|---|---|
| **S** | Single Responsibility Principle | Class này có đang làm **quá nhiều việc** không? |
| **O** | Open/Closed Principle | Khi cần thêm tính năng, tôi phải **sửa code cũ** hay chỉ cần **thêm code mới**? |
| **L** | Liskov Substitution Principle | Subclass thay thế được cho class cha mà **không phá vỡ** logic chương trình không? |
| **I** | Interface Segregation Principle | Interface có đang **ép** class implement những method nó **không cần dùng** không? |
| **D** | Dependency Inversion Principle | Class có đang phụ thuộc vào **chi tiết cụ thể** thay vì **abstraction** không? |

> **Lưu ý quan trọng khi trả lời phỏng vấn:** SOLID không phải luật cứng nhắc phải áp dụng 100% mọi lúc — mục tiêu là **giảm coupling (sự phụ thuộc chặt)** và **tăng cohesion (tính gắn kết nội tại)**. Áp dụng SOLID quá mức cho một ứng dụng nhỏ, đơn giản có thể gây **over-engineering** (thiết kế phức tạp không cần thiết) — nên biết cân bằng.

---

## 2. S — Single Responsibility Principle

> **Một class chỉ nên có DUY NHẤT MỘT lý do để thay đổi.**

Nghĩa là: một class chỉ nên chịu trách nhiệm cho **một nhiệm vụ (responsibility)** cụ thể — không gộp nhiều mối quan tâm (concern) không liên quan vào cùng 1 class.

### ❌ Vi phạm SRP

```java
public class Invoice {
    private double amount;

    public double calculateTotal() { // trách nhiệm 1: TÍNH TOÁN nghiệp vụ
        return amount * 1.1; // giả sử cộng thuế 10%
    }

    public void saveToDatabase() { // trách nhiệm 2: LƯU TRỮ dữ liệu
        System.out.println("Đang lưu hóa đơn vào database...");
    }

    public void printInvoice() { // trách nhiệm 3: XUẤT/IN ấn
        System.out.println("In hóa đơn: " + calculateTotal());
    }

    public void sendEmailNotification() { // trách nhiệm 4: GỬI THÔNG BÁO
        System.out.println("Gửi email hóa đơn cho khách hàng...");
    }
}
```

**Vấn đề:** `Invoice` đang gánh **4 lý do khác nhau để thay đổi** — nếu đổi công thức tính thuế, đổi loại database, đổi định dạng in ấn, hay đổi nhà cung cấp email, đều phải sửa **cùng 1 class** này. Điều này làm class trở nên **cồng kềnh, khó test riêng lẻ, dễ xung đột khi nhiều người cùng sửa**.

### ✅ Áp dụng SRP — tách trách nhiệm ra từng class riêng

```java
public class Invoice { // CHỈ chịu trách nhiệm chứa dữ liệu + tính toán nghiệp vụ liên quan trực tiếp
    private double amount;
    public double calculateTotal() { return amount * 1.1; }
}

public class InvoiceRepository { // CHỈ chịu trách nhiệm lưu trữ
    public void save(Invoice invoice) { System.out.println("Đang lưu vào database..."); }
}

public class InvoicePrinter { // CHỈ chịu trách nhiệm in ấn
    public void print(Invoice invoice) { System.out.println("In hóa đơn: " + invoice.calculateTotal()); }
}

public class InvoiceNotifier { // CHỈ chịu trách nhiệm gửi thông báo
    public void sendEmail(Invoice invoice) { System.out.println("Gửi email..."); }
}
```

> **Liên hệ trực tiếp với kiến trúc backend thực tế:** đây chính là lý do vì sao một ứng dụng Spring Boot chuẩn luôn tách thành nhiều tầng: `Controller` (nhận request), `Service` (xử lý nghiệp vụ), `Repository` (truy xuất dữ liệu) — mỗi tầng là một "trách nhiệm" riêng biệt, không gộp chung.

---

## 3. O — Open/Closed Principle

> **Class nên MỞ để mở rộng (open for extension), nhưng ĐÓNG để sửa đổi (closed for modification).**

Nghĩa là: khi cần thêm tính năng mới, nên **thêm code mới** (ví dụ tạo class mới implement 1 interface có sẵn), thay vì **sửa lại code đã hoạt động ổn định** — giảm rủi ro gây lỗi ở những phần code cũ đã được test kỹ.

### ❌ Vi phạm OCP

```java
public class DiscountCalculator {
    public double calculate(String customerType, double price) {
        if (customerType.equals("REGULAR")) {
            return price;
        } else if (customerType.equals("VIP")) {
            return price * 0.9;
        } else if (customerType.equals("PREMIUM")) {
            return price * 0.8;
        }
        // Mỗi khi có loại khách hàng MỚI, phải MỞ LẠI class này và thêm "else if"
        return price;
    }
}
```

**Vấn đề:** mỗi lần công ty ra mắt hạng khách hàng mới (`GOLD`, `PLATINUM`...), buộc phải **sửa trực tiếp** vào method đã hoạt động ổn định — rủi ro gây lỗi dây chuyền cho các loại khách hàng cũ đang chạy tốt.

### ✅ Áp dụng OCP — dùng Polymorphism + Abstraction thay vì if/else

```java
public interface DiscountStrategy {
    double apply(double price);
}

public class RegularDiscount implements DiscountStrategy {
    public double apply(double price) { return price; }
}
public class VipDiscount implements DiscountStrategy {
    public double apply(double price) { return price * 0.9; }
}
public class PremiumDiscount implements DiscountStrategy {
    public double apply(double price) { return price * 0.8; }
}

public class DiscountCalculator {
    public double calculate(DiscountStrategy strategy, double price) {
        return strategy.apply(price); // KHÔNG cần biết cụ thể là loại nào — đa hình xử lý hết
    }
}
```

```java
// Khi cần thêm hạng khách hàng MỚI — chỉ THÊM class mới, KHÔNG đụng đến code cũ
public class GoldDiscount implements DiscountStrategy {
    public double apply(double price) { return price * 0.85; }
}
```

> Đây chính là **Strategy Pattern** (sẽ học kỹ ở Module 08 — Design Patterns) — ứng dụng trực tiếp của OCP. `DiscountCalculator` giờ đây **"đóng" để sửa đổi** (không bao giờ cần sửa lại) nhưng **"mở" để mở rộng** (thêm class chiến lược mới bất cứ lúc nào).

---

## 4. L — Liskov Substitution Principle

> **Object của subclass phải có thể thay thế object của superclass mà KHÔNG làm thay đổi tính đúng đắn (correctness) của chương trình.**

Nói cách khác: nếu `B extends A`, thì ở bất kỳ đâu code đang dùng `A`, thay bằng `B` **không được gây lỗi hoặc hành vi bất ngờ**.

### ❌ Vi phạm LSP — ví dụ kinh điển "Hình vuông không phải là Hình chữ nhật (trong lập trình)"

```java
public class Rectangle {
    protected double width, height;
    public void setWidth(double w) { this.width = w; }
    public void setHeight(double h) { this.height = h; }
    public double getArea() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(double w) {
        this.width = w;
        this.height = w; // Square BẮT BUỘC width = height — phá vỡ kỳ vọng của Rectangle!
    }
    @Override
    public void setHeight(double h) {
        this.width = h;
        this.height = h;
    }
}
```

```java
public void testRectangle(Rectangle r) {
    r.setWidth(5);
    r.setHeight(10);
    assert r.getArea() == 50; // kỳ vọng hợp lý với Rectangle
}

testRectangle(new Rectangle()); // ✅ Pass — area = 50
testRectangle(new Square());    // ❌ FAIL! — vì setHeight(10) cũng ghi đè width thành 10, area = 100, không phải 50
```

**Vấn đề:** dù về mặt hình học "hình vuông là 1 trường hợp đặc biệt của hình chữ nhật" nghe hợp lý, nhưng trong lập trình OOP, `Square` **không thể thay thế `Rectangle`** mà không phá vỡ hành vi mong đợi — đây là ví dụ kinh điển cho thấy quan hệ kế thừa **không nên chỉ dựa vào trực giác hình học/thực tế**, mà phải dựa vào **hành vi (behavior) có tương thích hay không**.

### ✅ Sửa lại — tách quan hệ kế thừa hợp lý hơn

```java
public interface Shape {
    double getArea();
}
public class Rectangle implements Shape {
    private double width, height;
    public Rectangle(double w, double h) { this.width = w; this.height = h; }
    public double getArea() { return width * height; }
}
public class Square implements Shape { // KHÔNG kế thừa Rectangle nữa — độc lập, tự định nghĩa hành vi riêng
    private double side;
    public Square(double side) { this.side = side; }
    public double getArea() { return side * side; }
}
```

### Dấu hiệu nhận biết vi phạm LSP trong code thực tế

- Subclass **ném exception** ở method mà lớp cha không ném (ví dụ `UnsupportedOperationException`).
- Subclass **override method nhưng làm ngược lại ý nghĩa gốc** của lớp cha.
- Cần dùng `instanceof` để kiểm tra kiểu cụ thể trước khi gọi method — dấu hiệu rõ ràng cho thấy các subclass **không thực sự thay thế được cho nhau**.

---

## 5. I — Interface Segregation Principle

> **Không nên ép một class phải implement những method nó KHÔNG DÙNG ĐẾN. Nên chia nhỏ interface lớn thành nhiều interface nhỏ, chuyên biệt hơn.**

### ❌ Vi phạm ISP — interface "phình to" (fat interface)

```java
public interface Worker {
    void work();
    void eat();
    void sleep();
}

public class HumanWorker implements Worker {
    public void work() { System.out.println("Đang làm việc"); }
    public void eat() { System.out.println("Đang ăn trưa"); }
    public void sleep() { System.out.println("Đang nghỉ ngơi"); }
}

public class RobotWorker implements Worker {
    public void work() { System.out.println("Robot đang làm việc"); }
    public void eat() { throw new UnsupportedOperationException("Robot không ăn!"); } // ép buộc implement method vô nghĩa
    public void sleep() { throw new UnsupportedOperationException("Robot không ngủ!"); }
}
```

**Vấn đề:** `RobotWorker` bị **ép buộc** implement `eat()` và `sleep()` dù hoàn toàn không liên quan đến bản chất của nó — dẫn đến code "giả vờ implement" bằng cách ném exception, đây là dấu hiệu thiết kế interface sai.

### ✅ Áp dụng ISP — chia nhỏ interface theo từng khả năng riêng biệt

```java
public interface Workable {
    void work();
}
public interface Eatable {
    void eat();
}
public interface Sleepable {
    void sleep();
}

public class HumanWorker implements Workable, Eatable, Sleepable {
    public void work() { System.out.println("Đang làm việc"); }
    public void eat() { System.out.println("Đang ăn trưa"); }
    public void sleep() { System.out.println("Đang nghỉ ngơi"); }
}

public class RobotWorker implements Workable { // CHỈ implement những gì thực sự liên quan
    public void work() { System.out.println("Robot đang làm việc"); }
}
```

> **Liên hệ thực tế Spring:** đây là lý do các interface trong Spring Data JPA được thiết kế phân tầng nhỏ (`CrudRepository` → `PagingAndSortingRepository` → `JpaRepository`) thay vì gộp hết mọi method vào 1 interface khổng lồ — cho phép chọn đúng mức độ khả năng cần dùng.

---

## 6. D — Dependency Inversion Principle

> **Module cấp cao (high-level) không nên phụ thuộc vào module cấp thấp (low-level). Cả hai nên phụ thuộc vào ABSTRACTION. Abstraction không nên phụ thuộc vào chi tiết cài đặt — chi tiết cài đặt nên phụ thuộc vào abstraction.**

Đây là nguyên lý **quan trọng nhất** trong 5 nguyên lý SOLID đối với lập trình backend — là nền tảng lý thuyết trực tiếp của **Dependency Injection** trong Spring.

### ❌ Vi phạm DIP — phụ thuộc trực tiếp vào implementation cụ thể

```java
public class MySQLDatabase { // module cấp THẤP — chi tiết cài đặt cụ thể
    public void save(String data) {
        System.out.println("Lưu vào MySQL: " + data);
    }
}

public class UserService { // module cấp CAO — logic nghiệp vụ
    private MySQLDatabase database = new MySQLDatabase(); // ❌ PHỤ THUỘC TRỰC TIẾP vào class cụ thể

    public void registerUser(String userData) {
        database.save(userData);
    }
}
```

**Vấn đề:** `UserService` (logic nghiệp vụ quan trọng, cấp cao) bị **khóa cứng (hard-coded)** vào `MySQLDatabase` (chi tiết kỹ thuật, cấp thấp). Nếu muốn đổi sang PostgreSQL, hoặc MongoDB, hoặc cần viết Unit Test với database giả lập (mock) — **bắt buộc phải sửa `UserService`**, vi phạm cả OCP lẫn DIP.

### ✅ Áp dụng DIP — cả hai phụ thuộc vào Abstraction (interface)

```java
public interface Database { // ABSTRACTION — cả 2 tầng đều phụ thuộc vào đây
    void save(String data);
}

public class MySQLDatabase implements Database {
    public void save(String data) { System.out.println("Lưu vào MySQL: " + data); }
}

public class MongoDatabase implements Database { // dễ dàng thêm implementation mới
    public void save(String data) { System.out.println("Lưu vào MongoDB: " + data); }
}

public class UserService {
    private final Database database; // ✅ phụ thuộc vào INTERFACE, không phụ thuộc class cụ thể

    public UserService(Database database) { // inject qua constructor — chính là "Dependency Injection"
        this.database = database;
    }

    public void registerUser(String userData) {
        database.save(userData);
    }
}
```

```java
// Người gọi (caller) quyết định implementation cụ thể nào được inject vào
UserService service1 = new UserService(new MySQLDatabase());
UserService service2 = new UserService(new MongoDatabase());

// Khi viết Unit Test — dễ dàng "tiêm" vào 1 Database giả lập (mock) mà không đụng database thật
UserService testService = new UserService(new FakeDatabaseForTesting());
```

### Sơ đồ minh họa "đảo ngược" luồng phụ thuộc

```
TRƯỚC (vi phạm DIP):
  UserService  ────depends on────►  MySQLDatabase (class cụ thể)

SAU (tuân thủ DIP):
  UserService  ────depends on────►  Database (interface)
                                          ▲
                                          │ implements
                                     MySQLDatabase
```

Chú ý mũi tên phụ thuộc của `UserService` giờ trỏ vào **interface trừu tượng**, còn `MySQLDatabase` (chi tiết cụ thể) mới là bên phải "chạy theo" implement interface đó — đây chính là ý nghĩa của từ **"Inversion" (đảo ngược)**: thay vì module cấp cao phụ thuộc chi tiết cấp thấp (thông thường), cả hai giờ cùng phụ thuộc vào 1 abstraction chung ở giữa.

> **Đây CHÍNH XÁC là cách Spring Framework hoạt động:** bạn khai báo field/constructor kiểu `interface` (`UserRepository`, `PaymentService`...), Spring container **tự động "tiêm" (inject)** implementation cụ thể vào lúc chạy — bạn không bao giờ viết `new MySQLDatabase()` trực tiếp trong code nghiệp vụ nữa. Toàn bộ cơ chế `@Autowired`, `@Component`, `ApplicationContext` ở Module 12 (Spring Framework Core) đều xây dựng trên chính nguyên lý DIP này.

---

## 7. SOLID và Spring Framework — mối liên hệ trực tiếp

| Nguyên lý | Biểu hiện cụ thể trong Spring |
|---|---|
| **S**RP | Kiến trúc phân tầng `Controller` / `Service` / `Repository` — mỗi tầng một trách nhiệm |
| **O**CP | Thêm 1 `@Bean` hoặc `@Component` implementation mới mà không sửa code Service đang dùng interface đó |
| **L**SP | Mọi implementation của 1 interface (`@Service`) phải dùng thay thế lẫn nhau an toàn — nền tảng cho Spring chọn đúng Bean lúc runtime |
| **I**SP | Interface Spring Data JPA chia nhỏ theo tầng (`CrudRepository`, `PagingAndSortingRepository`, `JpaRepository`) |
| **D**IP | **Trái tim của toàn bộ Spring** — IoC Container + `@Autowired` chính là cơ chế tự động hóa việc "tiêm" implementation cụ thể vào nơi cần dùng abstraction |

> Nắm chắc SOLID — đặc biệt là **DIP** — trước khi học Module 12 sẽ giúp hiểu Spring **"tại sao lại thiết kế vậy"** thay vì chỉ học thuộc cú pháp `@Autowired` một cách máy móc.

---

## 8. Tổng kết — Bảng ghi nhớ nhanh

| Nguyên lý | Ghi nhớ 1 câu | Dấu hiệu vi phạm cần chú ý |
|---|---|---|
| SRP | 1 class = 1 lý do để thay đổi | Class có tên chung chung như `Manager`, `Util`, `Helper` ôm quá nhiều logic không liên quan |
| OCP | Thêm tính năng bằng cách thêm code mới, không sửa code cũ | Chuỗi `if/else if` hoặc `switch` liên tục phải sửa khi có case mới |
| LSP | Subclass thay thế class cha không được gây lỗi | Subclass ném `UnsupportedOperationException`, hoặc code cần `instanceof` để check kiểu cụ thể |
| ISP | Không ép implement method thừa không dùng | Interface có nhiều method mà nhiều class implement phải để trống hoặc ném exception |
| DIP | Phụ thuộc vào interface, không phụ thuộc class cụ thể | Dùng `new ConcreteClass()` trực tiếp trong logic nghiệp vụ thay vì inject qua interface |

---

## 9. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Class sau vi phạm nguyên lý SOLID nào? Giải thích và đề xuất cách sửa ngắn gọn.
```java
public class ReportGenerator {
    public String generateReportData() { return "data"; }
    public void exportToPDF(String data) { System.out.println("Xuất PDF: " + data); }
    public void exportToExcel(String data) { System.out.println("Xuất Excel: " + data); }
    public void sendReportByEmail(String data) { System.out.println("Gửi email: " + data); }
}
```

**Câu 2.** Đoạn code sau vi phạm nguyên lý nào? Vì sao nó sẽ ngày càng khó bảo trì khi hệ thống phát triển?
```java
public class ShippingCostCalculator {
    public double calculate(String shippingType, double weight) {
        if (shippingType.equals("STANDARD")) return weight * 1000;
        else if (shippingType.equals("EXPRESS")) return weight * 2000;
        else if (shippingType.equals("SAME_DAY")) return weight * 5000;
        return 0;
    }
}
```

**Câu 3.** Cho `interface Bird` sau, việc `Penguin implements Bird` có tiềm ẩn vi phạm SOLID không? Nguyên lý nào?
```java
public interface Bird {
    void fly();
    void eat();
}
public class Penguin implements Bird {
    public void fly() { throw new UnsupportedOperationException("Chim cánh cụt không biết bay!"); }
    public void eat() { System.out.println("Đang ăn cá"); }
}
```

**Câu 4.** So sánh 2 đoạn code sau — đoạn nào tuân thủ DIP tốt hơn? Giải thích.
```java
// Đoạn A
public class OrderService {
    private EmailSender sender = new EmailSender();
}

// Đoạn B
public class OrderService {
    private final NotificationSender sender;
    public OrderService(NotificationSender sender) { this.sender = sender; }
}
```

**Câu 5.** Nêu 1 ví dụ thực tế (không cần code, chỉ mô tả bằng lời) về việc áp dụng SOLID **quá mức** có thể gây "over-engineering" cho một ứng dụng nhỏ, đơn giản.

---

### Phần B — Bài tập viết code

**Bài 1 — Refactor vi phạm SRP.**
Refactor class `ReportGenerator` ở Câu 1 Phần A thành nhiều class tuân thủ SRP: `ReportDataService`, `PdfExporter`, `ExcelExporter`, `ReportEmailNotifier`. Viết thêm `interface ReportExporter` với method `export(String data)`, cho `PdfExporter` và `ExcelExporter` cùng implement (chuẩn bị áp dụng OCP ở bài sau).

**Bài 2 — Áp dụng OCP cho hệ thống tính phí ship.**
Refactor `ShippingCostCalculator` ở Câu 2 Phần A bằng Strategy Pattern: tạo `interface ShippingStrategy` với method `calculate(double weight)`, cài đặt `StandardShipping`, `ExpressShipping`, `SameDayShipping`. Viết thêm 1 loại vận chuyển mới `InternationalShipping` để **chứng minh** không cần sửa bất kỳ class cũ nào.

**Bài 3 — Phát hiện & sửa vi phạm LSP.**
Cho class `Bird` (abstract) có method `fly()`, và subclass `Sparrow` (chim sẻ — bay được), `Ostrich` (đà điểu — KHÔNG bay được, hiện đang override `fly()` bằng cách ném exception). Redesign lại hệ thống bằng cách tách interface `Flyable` riêng — chỉ những loài chim biết bay mới implement — để `Ostrich` không còn vi phạm LSP.

**Bài 4 — Áp dụng ISP cho hệ thống thiết bị văn phòng.**
Cho `interface MultiFunctionDevice` với các method: `print()`, `scan()`, `fax()`. Có 2 loại thiết bị: `AllInOnePrinter` (làm được cả 3 việc) và `SimplePrinter` (chỉ in được, không scan/fax được). Chia nhỏ interface theo ISP để `SimplePrinter` không bị ép implement những method không dùng đến.

**Bài 5 — Áp dụng DIP hoàn chỉnh (bài tổng hợp quan trọng nhất).**
Xây dựng hệ thống gửi thông báo đơn hàng gồm:
- `interface NotificationSender` với method `send(String message)`.
- 2 implementation: `EmailNotificationSender`, `SmsNotificationSender`.
- `class OrderService` nhận `NotificationSender` qua **constructor injection** (không dùng `new` trực tiếp bên trong).
- Viết `main` tạo 2 instance `OrderService` khác nhau — một dùng Email, một dùng SMS — chứng minh cùng 1 class `OrderService` hoạt động với **bất kỳ implementation nào** miễn tuân thủ interface.
- Viết thêm 1 class `FakeNotificationSender` (chỉ in ra console, giả lập việc gửi) để minh họa lợi ích của DIP khi viết Unit Test — không cần gửi email/SMS thật khi test.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. Vi phạm **SRP** — class ôm 4 trách nhiệm: tạo dữ liệu báo cáo, xuất PDF, xuất Excel, gửi email. Sửa: tách thành các class/interface riêng theo từng trách nhiệm (chính là Bài 1 Phần B).
2. Vi phạm **OCP** — mỗi lần công ty thêm loại vận chuyển mới (ví dụ `INTERNATIONAL`), bắt buộc phải sửa lại method `calculate()` đã hoạt động ổn định, tăng rủi ro gây lỗi cho các loại vận chuyển cũ đang chạy tốt; chuỗi `if/else if` cũng sẽ ngày càng dài và khó đọc.
3. **Có** — vi phạm **LSP**. `Penguin` là 1 `Bird` nhưng không thể thay thế cho `Bird` nói chung trong ngữ cảnh cần gọi `fly()` — code gọi `bird.fly()` cho mọi `Bird` sẽ bất ngờ gặp exception khi object thực tế là `Penguin`. Đây cũng gián tiếp cho thấy interface `Bird` đang vi phạm ISP (ép mọi loài chim phải có khả năng bay).
4. **Đoạn B tuân thủ DIP tốt hơn** — `OrderService` phụ thuộc vào abstraction (`NotificationSender` interface) qua constructor injection, không tự tạo (`new`) implementation cụ thể bên trong. Đoạn A vi phạm DIP vì hard-code phụ thuộc trực tiếp vào `EmailSender` — muốn đổi sang SMS phải sửa code `OrderService`.
5. Ví dụ: một ứng dụng "To-do list" cá nhân đơn giản, chỉ có 1 người dùng, không có kế hoạch mở rộng — nếu tách ra 5-6 interface riêng biệt cho từng thao tác CRUD, thêm Strategy Pattern cho việc sắp xếp task, thêm Factory Pattern để tạo task... sẽ khiến codebase phức tạp không cần thiết so với quy mô thực tế của bài toán, làm chậm tốc độ phát triển ban đầu mà không mang lại lợi ích tương xứng.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Đây là bước đệm trực tiếp chuẩn bị cho Bài 2 — sau khi tách theo SRP, việc thêm OCP (qua interface `ReportExporter`) trở nên tự nhiên vì các class đã đủ nhỏ và tập trung.
- **Bài 3:** Bài này thể hiện rõ **LSP và ISP luôn đi kèm nhau** trong thực tế — sửa vi phạm LSP (Ostrich không nên "là" 1 thứ biết bay) thường dẫn tới việc phải tách nhỏ interface (ISP) để phản ánh đúng khả năng thực tế của từng loại.
- **Bài 5:** Đây là bài tập quan trọng nhất của cả module — chính là "phiên bản viết tay" của những gì Spring Framework làm tự động phía sau `@Autowired`. Hiểu rõ bài này nghĩa là đã sẵn sàng để học Module 12 (Spring Framework Core — IoC Container & Dependency Injection) một cách có nền tảng, thay vì học thuộc annotation một cách máy móc.

</details>

---

*File tiếp theo trong lộ trình: **Module 02.4 — equals(), hashCode(), toString()** (contract giữa equals/hashCode, Comparable vs Comparator).*
