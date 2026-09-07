# Module 01.5 — Interface vs Abstract Class

> **Mức độ ưu tiên: Cao** — Câu hỏi "Khi nào dùng interface, khi nào dùng abstract class?" gần như chắc chắn có trong phỏng vấn Java. Nhưng câu trả lời "xịn" cần thêm: quy tắc **class thắng interface**, **re-abstraction**, vì sao `default` method **không** override được `equals`/`hashCode`, và vì sao *constant interface* là anti-pattern. Đây cũng là kiến thức bắt buộc để đọc source Spring (`JpaRepository`, `UserDetailsService`, `PasswordEncoder`... đều là interface).

> **Phạm vi bài học:** interface & abstract class (thành phần, modifier ngầm định, `sealed`), `default` / `static` / `private` method trong interface, đa kế thừa hành vi & type, quy tắc phân giải xung đột (3 rules), re-abstraction, marker interface, constant interface anti-pattern, cây quyết định "dùng cái nào". Các chủ đề: lambda & functional interface chi tiết (Module 01.10), SOLID (Module 01.6), `equals`/`hashCode` (Module 01.7), Generics trên interface (Module 01.9) **không** thuộc bài này — chỉ nhắc khi liên quan. Kiến thức nền: kế thừa, override, đa hình (Module 01.4).

---

## Mục lục

1. [Nhắc lại nhanh & các modifier ngầm định](#1-nhắc-lại-nhanh--các-modifier-ngầm-định)
2. [Default Method trong Interface (Java 8+)](#2-default-method-trong-interface-java-8)
3. [Static Method trong Interface (Java 8+)](#3-static-method-trong-interface-java-8)
4. [Private Method trong Interface (Java 9+)](#4-private-method-trong-interface-java-9)
5. [Đa kế thừa qua Interface — 3 loại "kim cương"](#5-đa-kế-thừa-qua-interface--3-loại-kim-cương)
6. [Quy tắc phân giải method: class thắng interface](#6-quy-tắc-phân-giải-method-class-thắng-interface)
7. [Re-abstraction & các mẫu dùng interface](#7-re-abstraction--các-mẫu-dùng-interface)
8. [Constant Interface — anti-pattern](#8-constant-interface--anti-pattern)
9. [So sánh toàn diện Interface vs Abstract Class](#9-so-sánh-toàn-diện-interface-vs-abstract-class)
10. [Khi nào dùng cái nào — cây quyết định](#10-khi-nào-dùng-cái-nào--cây-quyết-định)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. Nhắc lại nhanh & các modifier ngầm định

```java
public abstract class Animal {
    protected String name;                       // field mutable, mọi access modifier
    protected Animal(String name) { this.name = name; }   // CÓ constructor
    public abstract void makeSound();            // buộc subclass cài
    public void sleep() { System.out.println(name + " đang ngủ"); }  // concrete
}

public interface Flyable {
    void fly();                                  // ngầm: public abstract
}
```

### Mọi thứ trong `interface` đều có modifier ngầm định

| Thành phần trong interface | Modifier ngầm định (viết thừa cũng được, nhưng đừng) |
|---|---|
| Method không thân | `public abstract` |
| Method `default` / `static` | `public` |
| Field | `public static final` (**hằng số**, phải khởi tạo ngay) |
| Nested class / interface / enum | `public static` |

> ⚠️ Trong interface **không có** `protected`, `package-private`, hay `private` cho *abstract* method. `private` method chỉ xuất hiện từ Java 9 và **phải có thân** (§4). Không có field instance, không có constructor, không có instance initializer.

### `sealed interface` (Java 17+)

```java
public sealed interface Shape permits Circle, Square, Rectangle { }
```

Giới hạn **danh sách đóng** các kiểu được `implements`/`extends` → `switch` pattern matching kiểm tra được tính bao phủ (Module 01.2 §5). Lớp con phải là `final`, `sealed`, hoặc `non-sealed`.

### Marker interface — interface rỗng

`Serializable`, `Cloneable`, `RandomAccess` không có method nào — chúng chỉ **"đánh dấu"** một khả năng để code khác kiểm tra bằng `instanceof`. Ngày nay annotation thường thay thế vai trò này, nhưng marker interface vẫn hữu ích vì nó tạo ra một *kiểu* (dùng được trong khai báo tham số).

---

## 2. Default Method trong Interface (Java 8+)

Trước Java 8, interface không thể có method có thân → thêm một method mới vào interface sẽ làm **mọi class đang `implements`** lỗi compile. `default` method giải quyết đúng bài toán *tiến hóa interface* này (ví dụ `Collection.stream()`, `List.replaceAll()`, `Iterable.forEach()` được thêm vào Java 8 mà không phá vỡ code cũ).

```java
public interface Vehicle {
    void accelerate();                       // abstract

    default void honk() {                    // có thân
        System.out.println("Beep beep!");
    }
}

public class Car implements Vehicle {
    @Override public void accelerate() { System.out.println("Tăng tốc"); }
    // KHÔNG cần cài honk() — thừa hưởng bản default
}
```

Class implement **có thể override** default method để đổi hành vi, và trong bản override được gọi lại bản gốc bằng `Vehicle.super.honk()`.

### `default` method có thể gọi abstract method khác (Template Method trong interface)

```java
public interface Validator<T> {
    boolean isValid(T value);                        // abstract

    default T requireValid(T value) {                // default dùng abstract
        if (!isValid(value)) throw new IllegalArgumentException("không hợp lệ: " + value);
        return value;
    }
}
```

### ⚠️ `default` method KHÔNG được override method của `Object`

```java
public interface Bad {
    default String toString() { return "x"; }   // ❌ compile lỗi
    default boolean equals(Object o) { ... }     // ❌
    default int hashCode() { ... }               // ❌
}
```

Lý do: `Object` là lớp cha của mọi class, và theo quy tắc **class thắng interface** (§6) bản của `Object` sẽ luôn thắng → một `default` như vậy vô nghĩa; Java cấm luôn ở compile-time.

### ⚠️ `default` method không có state để dựa vào

Interface không có field instance. Một `default` method muốn "đọc dữ liệu" phải gọi qua **abstract getter** mà class implement cung cấp:

```java
public interface Named {
    String name();                                  // abstract — class implement cấp dữ liệu
    default String greeting() { return "Xin chào " + name(); }
}
```

---

## 3. Static Method trong Interface (Java 8+)

Thuộc về **chính interface**, gọi qua tên interface — **không** kế thừa bởi class implement, **không** gọi qua instance.

```java
public interface Discount {
    double apply(double price);

    static Discount percentage(double pct) {         // static factory method
        return price -> price - price * pct / 100;   // trả về một lambda (Module 01.10)
    }
    static Discount none() { return price -> price; }
}

Discount d = Discount.percentage(10);
d.apply(200);                                        // 180.0
// new SomeImpl().percentage(...)   // ❌ — không gọi static của interface qua instance
// SomeImpl.percentage(...)          // ❌ — cũng không qua tên class implement; phải Discount.percentage(...)
```

Ứng dụng phổ biến trong JDK: `Comparator.comparing(...)`, `List.of(...)`, `Stream.of(...)`, `Path.of(...)`, `Map.entry(...)` — gom "hàm tạo/tiện ích" liên quan chặt vào chính interface, tránh phải có một class `XxxUtils` riêng.

---

## 4. Private Method trong Interface (Java 9+)

Dùng để **tái sử dụng code nội bộ** giữa các `default`/`static` method mà không phơi ra hợp đồng public.

```java
public interface ReportGenerator {
    default String pdf(String data)   { return "PDF: "   + normalize(data); }
    default String excel(String data) { return "Excel: " + normalize(data); }

    private String normalize(String data) {          // private instance — chỉ default method dùng
        return "[" + data.strip().toUpperCase(java.util.Locale.ROOT) + "]";
    }
    private static String header() {                 // private static — cả static lẫn default dùng được
        return "=== REPORT ===";
    }
}
```

| Loại | Ai gọi được |
|---|---|
| `private` (instance) | các `default` method trong cùng interface |
| `private static` | cả `default` method lẫn `static` method trong cùng interface |

`private` method trong interface **bắt buộc có thân**, **không** được class implement nhìn thấy.

---

## 5. Đa kế thừa qua Interface — 3 loại "kim cương"

Một class `implements` nhiều interface; một interface `extends` nhiều interface.

```java
public interface Swimmable { void swim(); }
public interface Flyable    { void fly(); }

public class Duck implements Swimmable, Flyable {
    @Override public void swim() { System.out.println("Vịt bơi"); }
    @Override public void fly()  { System.out.println("Vịt bay"); }
}

public interface Amphibious extends Swimmable, Flyable {   // interface extends NHIỀU interface
    void walk();
}
```

Đa kế thừa gồm **3 khía cạnh** — chỉ một khía cạnh gây rắc rối:

| Khía cạnh kế thừa | Interface có gây xung đột? | Vì sao |
|---|---|---|
| **Kiểu (type)** — object vừa là `Swimmable` vừa là `Flyable` | Không | Chỉ là "gắn nhãn" nhiều kiểu, không mâu thuẫn |
| **State (trạng thái)** | Không | Interface không có field instance → không có "kim cương dữ liệu" như C++ |
| **Implementation (`default` method)** | **Có thể** | Hai `default` cùng chữ ký từ hai interface → compiler bắt lỗi (§6) |

---

## 6. Quy tắc phân giải method: class thắng interface

Khi một method (cùng chữ ký) đến từ **nhiều nguồn** (lớp cha + nhiều interface), Java áp dụng 3 quy tắc theo thứ tự:

**Quy tắc 1 — Class/superclass thắng interface.**

```java
class Base {
    public void hello() { System.out.println("Base.hello"); }
}
interface Greeter {
    default void hello() { System.out.println("Greeter.hello (default)"); }
}
class Sub extends Base implements Greeter { }

new Sub().hello();   // "Base.hello" — bản của class thắng, KHÔNG cần override, KHÔNG báo xung đột
```

> Hệ quả tinh tế: thêm một `default` method vào interface có thể **bị "che" hoàn toàn** nếu lớp cha (kể cả `Object`) đã có method cùng chữ ký. Đây chính là lý do `default` không override được `toString`/`equals`/`hashCode`.

**Quy tắc 2 — Interface con (cụ thể hơn) thắng interface cha.**

```java
interface A            { default void m() { System.out.println("A"); } }
interface B extends A  { default void m() { System.out.println("B"); } }   // B cụ thể hơn A
class C implements A, B { }

new C().m();   // "B" — không xung đột, bản của interface "gần" hơn thắng
```

**Quy tắc 3 — Còn lại: buộc override tường minh.**

```java
interface X { default void m() { System.out.println("X"); } }
interface Y { default void m() { System.out.println("Y"); } }   // X, Y không liên quan nhau

class Z implements X, Y {
    // ❌ nếu không override: "class Z inherits unrelated defaults for m() from types X and Y"
    @Override public void m() {
        X.super.m();          // gọi tường minh bản của MỘT interface cụ thể
        Y.super.m();
        System.out.println("Z");
    }
}
```

### Xung đột giữa `abstract` và `default` cùng chữ ký

```java
interface P { void m(); }                                   // abstract
interface Q { default void m() { System.out.println("Q"); } }
class R implements P, Q { }   // ❌ lỗi — sự hiện diện của abstract m() ở P làm bản default của Q
                              //    KHÔNG còn tự động áp dụng; R buộc phải cài đặt m()
```

> Nói cách khác: một `default` method chỉ "cứu" bạn khỏi phải cài đặt **khi không có** một khai báo abstract nào cùng chữ ký ở nhánh kế thừa khác.

---

## 7. Re-abstraction & các mẫu dùng interface

### Re-abstraction — biến `default` thành `abstract` trở lại

Interface con có thể **thu hồi** bản cài đặt mặc định của cha, buộc mọi class implement phải tự viết:

```java
interface Logger {
    default void log(String msg) { System.out.println(msg); }
}
interface AuditLogger extends Logger {
    @Override void log(String msg);      // KHÔNG có thân → "abstract lại" — buộc implement
}
```

### Mẫu 1 — Skeletal implementation (`AbstractXxx`)

JDK dùng cặp *interface + abstract class khung*: `Collection` + `AbstractCollection`, `List` + `AbstractList`, `Map` + `AbstractMap`. Interface định nghĩa hợp đồng; abstract class cài sẵn phần lặp lại dựa trên vài method "nguyên thủy". Class người dùng `extends AbstractList` chỉ cần cài `get(int)` và `size()`.

### Mẫu 2 — Static factory trong interface

```java
public interface Shape {
    double area();
    static Shape circle(double r)       { return () -> Math.PI * r * r; }
    static Shape square(double s)       { return () -> s * s; }
}
```

### Mẫu 3 — Functional interface (xem Module 01.10)

Interface có **đúng một** abstract method → dùng được với lambda. Đánh dấu `@FunctionalInterface` để compiler canh giữ:

```java
@FunctionalInterface
public interface Transformer<I, O> {
    O transform(I input);
    default <R> Transformer<I, R> andThen(Transformer<O, R> next) {
        return in -> next.transform(this.transform(in));
    }
}
```

### Mẫu 4 — Marker interface

`implements Serializable` — không thêm method, chỉ để `ObjectOutputStream` kiểm tra `obj instanceof Serializable`.

---

## 8. Constant Interface — anti-pattern

```java
// ❌ ĐỪNG LÀM THẾ NÀY
public interface AppConstants {
    String API_URL = "https://api.example.com";
    int TIMEOUT = 30;
}
public class Client implements AppConstants {   // "implements" chỉ để khỏi gõ tiền tố
    void call() { connect(API_URL, TIMEOUT); }
}
```

Vấn đề:

- `implements AppConstants` làm chi tiết cài đặt (việc *lấy hằng từ đâu*) **rò vào API công khai** của `Client` — người dùng `Client` thấy nó "là một" `AppConstants`, vô nghĩa.
- Không bỏ được: mọi lớp con của `Client` cũng thừa hưởng đống hằng đó.

**Cách đúng:** một `final` class với `private` constructor + `static` field, dùng qua `import static`:

```java
public final class AppConstants {
    private AppConstants() {}
    public static final String API_URL = "https://api.example.com";
    public static final int TIMEOUT = 30;
}
// nơi dùng:
import static com.example.AppConstants.*;
```

Hoặc tốt hơn nữa: `enum` (nếu là tập giá trị hữu hạn) hoặc lớp cấu hình (`@ConfigurationProperties` trong Spring).

---

## 9. So sánh toàn diện Interface vs Abstract Class

| Tiêu chí | `abstract class` | `interface` |
|---|---|---|
| Từ khóa | `extends` — **một** lớp cha | `implements` — **nhiều**; interface `extends` nhiều interface |
| Constructor | Có (chạy khi subclass gọi `super()`) | Không |
| Field instance (state) | Có, mọi access modifier, mutable | Không — chỉ `public static final` hằng |
| Method abstract | `abstract`, mọi access (trừ `private`) | ngầm `public abstract` |
| Method có thân | concrete method | `default`, `static` (Java 8), `private`/`private static` (Java 9) |
| `protected` / package-private member | Có | Không |
| Đa kế thừa | Không | Có (hành vi qua `default`, kiểu qua nhiều interface) |
| Tiến hóa (thêm method sau này) | Thêm abstract method → phá lớp con; thêm concrete method → an toàn | Thêm abstract → phá class implement; thêm `default`/`static` → an toàn |
| `sealed` | Có | Có (Java 17) |
| Dùng với lambda | Không | Có, nếu là functional interface (1 abstract method) |
| Quan hệ ngữ nghĩa | "is-a" chặt + chia sẻ **state & code** giữa họ hàng gần | "can-do" / capability — các class **không cần liên quan** về bản chất |
| Ví dụ JDK | `AbstractList`, `AbstractMap`, `Number`, `InputStream` | `List`, `Comparable`, `Runnable`, `AutoCloseable`, `Serializable` |

---

## 10. Khi nào dùng cái nào — cây quyết định

**Bắt đầu bằng `interface`** (mặc định nên là lựa chọn đầu tiên), chuyển sang / kết hợp thêm `abstract class` khi:

| Chọn `interface` khi... | Chọn `abstract class` khi... |
|---|---|
| Định nghĩa một **khả năng** mà nhiều class không liên quan đều có thể có (`Comparable`, `AutoCloseable`) | Các lớp con **họ hàng gần**, chia sẻ **field/state** và một phần lớn logic |
| Cần một class đóng **nhiều vai trò** (đa kế thừa hành vi) | Cần **constructor** để khởi tạo state chung, hoặc field `protected`/`private` nội bộ |
| Muốn dùng được với **lambda** | Cần **kiểm soát access modifier** chi tiết cho method nội bộ |
| Lập trình **hướng hợp đồng** (Spring DI: inject theo interface, không theo class) | Muốn cung cấp *skeletal implementation* để giảm công cho người kế thừa |

### Mẫu phổ biến nhất trong thực tế: kết hợp cả hai

```java
public interface Payable {                         // hợp đồng
    long calculateSalaryCents();
}

public abstract class Employee implements Payable { // chia sẻ state chung
    private final String name;
    protected Employee(String name) { this.name = name; }
    public String getName() { return name; }
    // calculateSalaryCents() vẫn abstract — lớp con cụ thể cài
}

public class Developer extends Employee {
    public Developer(String name) { super(name); }
    @Override public long calculateSalaryCents() { /* ... */ return 0; }
}
```

> Trong Spring Boot: interface `UserService` (hợp đồng, để inject & mock), có thể có `AbstractUserService` (logic chung), và `UserServiceImpl` (chi tiết). Client luôn phụ thuộc `UserService`, không phụ thuộc `UserServiceImpl`.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Chủ đề | Điểm mấu chốt |
|---|---|
| Modifier ngầm định | method → `public abstract`; field → `public static final`; nested → `public static`; không có `protected`/instance field/constructor |
| `default` method | Có thân; giải bài toán *tiến hóa interface*; class implement thừa hưởng, override được, gọi lại bằng `X.super.m()` |
| `default` vs `Object` | **Không** được `default` cho `equals`/`hashCode`/`toString` (class luôn thắng interface) |
| `default` & state | Không có field → đọc dữ liệu qua abstract getter |
| `static` method interface | Gọi qua **tên interface**; không kế thừa; không qua instance / tên class implement |
| `private` method interface (Java 9) | Chỉ nội bộ; `private` cho `default` dùng, `private static` cho cả `default` + `static`; bắt buộc có thân |
| Đa kế thừa: 3 khía cạnh | type (OK), state (OK vì không có field), implementation (`default` trùng chữ ký → xung đột) |
| Quy tắc phân giải | (1) class thắng interface → (2) sub-interface thắng super-interface → (3) buộc override `X.super.m()` |
| `abstract` + `default` trùng chữ ký | `default` **không** cứu được — class buộc phải cài đặt |
| Re-abstraction | Interface con khai lại method không thân → thu hồi `default`, buộc implement |
| Skeletal implementation | Cặp `interface` + `AbstractXxx` (như JDK Collections) |
| Constant interface | Anti-pattern — dùng `final` class + `static` field + `import static` (hoặc `enum`) |
| Marker interface | Interface rỗng tạo một *kiểu* để kiểm tra `instanceof` (`Serializable`) |
| Chọn interface (mặc định) | capability, đa vai trò, lambda, hướng hợp đồng (Spring DI) |
| Chọn abstract class | họ hàng gần + chia sẻ state/constructor + cần access modifier nội bộ |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Compile được không? `PI` mang những modifier ngầm định nào?
```java
public interface Shape {
    double PI = 3.14;
    double area();
}
public class Circle implements Shape {
    private double radius;
    public Circle(double r) { this.radius = r; }
    @Override public double area() { return PI * radius * radius; }
}
```

**Câu 2.** Lỗi gì? Sửa thế nào?
```java
public interface Greetable { default void greet() { System.out.println("Hello"); } }
public interface Farewell  { default void greet() { System.out.println("Goodbye"); } }
public class Person implements Greetable, Farewell { }
```

**Câu 3.** Hợp lệ không? Giải thích chính xác điều gì xảy ra.
```java
public abstract class Base { public Base(String name) { } }
public class Impl extends Base { public Impl() { } }
```

**Câu 4.** `s.log("test")` gọi được không?
```java
public interface Utils { static void log(String m) { System.out.println(m); } }
public class Service implements Utils { }
// Service s = new Service(); s.log("test");
```

**Câu 5.** In ra gì? Giải thích bằng "quy tắc phân giải".
```java
class Base { public void hello() { System.out.println("Base"); } }
interface Greeter { default void hello() { System.out.println("Greeter"); } }
class Sub extends Base implements Greeter { }
// new Sub().hello();
```

**Câu 6.** Compile được không?
```java
public interface Printable {
    default String toString() { return "printable"; }
}
```

**Câu 7.** `new R().m()` — điều gì xảy ra?
```java
interface P { void m(); }
interface Q { default void m() { System.out.println("Q"); } }
class R implements P, Q { }
```

**Câu 8.** In ra gì?
```java
interface A           { default void m() { System.out.println("A"); } }
interface B extends A  { default void m() { System.out.println("B"); } }
class C implements A, B { }
// new C().m();
```

**Câu 9.** Đoạn nào lỗi compile?
```java
public interface Config {
    int PORT = 8080;          // (1)
    int timeout;              // (2)
    private int retries = 3;  // (3)
    void reload();            // (4)
}
```

**Câu 10.** `AuditLogger` buộc class implement làm gì?
```java
interface Logger { default void log(String s) { System.out.println(s); } }
interface AuditLogger extends Logger { @Override void log(String s); }
class FileAuditLogger implements AuditLogger { }
```

**Câu 11.** Vì sao "constant interface" (`class X implements AppConstants`) bị coi là anti-pattern? Nêu cách thay thế.

**Câu 12.** Một interface có thể `sealed` không? Nếu có, lớp `implements` nó phải thỏa điều kiện gì?

---

### Phần B — Bài tập viết code

**Bài 1 — MediaPlayer (default method).**
`interface MediaPlayer`: abstract `play(String file)`; default `pause()` → `"Đã tạm dừng"`; default `stop()` → `"Đã dừng"`. `AudioPlayer`, `VideoPlayer` chỉ cài `play()`. `PremiumVideoPlayer` override `stop()` để "lưu vị trí đang xem" rồi gọi `MediaPlayer.super.stop()`.

**Bài 2 — Static factory qua interface.**
`interface Discount { double apply(double price); }` + static `percentage(double pct)`, `fixedAmount(double amt)` (trả lambda), `none()`. Thêm default `andThen(Discount next)` để **ghép** hai giảm giá. `main`: `Discount.percentage(10).andThen(Discount.fixedAmount(5)).apply(200)`.

**Bài 3 — Giải quyết xung đột default method.**
`Walker` và `Swimmer` đều có default `move()`. `Amphibian implements Walker, Swimmer` override `move()` gọi **cả hai** bản gốc (`Walker.super.move()`, `Swimmer.super.move()`) rồi in thêm dòng riêng.

**Bài 4 — Interface + Abstract class (hệ hình học).**
`interface Drawable { void draw(); }`, `interface Resizable { void resize(double f); }`. `abstract class Shape implements Drawable, Resizable` chứa `width`, `height`, cài sẵn `resize()` (nhân `width`/`height` với `f`), để `draw()` abstract. `Square`, `Circle` chỉ cài `draw()`.

**Bài 5 — Refactor abstract class → interface.**
Cho `abstract class SoundMaker` mà `Dog` và `Robot` (không liên quan bản chất) đang buộc phải kế thừa. Đổi `SoundMaker` thành `interface`; giải thích 2–3 câu vì sao hợp lý hơn (is-a vs can-do).

**Bài 6 — Skeletal implementation.**
`interface Stack<E>` với `push`, `pop`, `peek`, `size`, `isEmpty`. Viết `abstract class AbstractStack<E> implements Stack<E>` cài sẵn `isEmpty()` (dựa `size()`) và `peek()` throw nếu rỗng. Viết `ArrayStack<E> extends AbstractStack<E>` chỉ cài `push`/`pop`/`size`.

**Bài 7 — Re-abstraction.**
`interface JsonSerializable` có default `toJson()` trả `"{}"`. `interface StrictJsonSerializable extends JsonSerializable` re-abstract `toJson()`. Chứng minh: `class A implements JsonSerializable {}` compile được, còn `class B implements StrictJsonSerializable {}` **không** compile cho tới khi cài `toJson()`.

**Bài 8 — "Class thắng interface" (dự đoán rồi kiểm chứng).**
```java
class LegacyPrinter { public void print() { System.out.println("Legacy"); } }
interface ModernPrinter { default void print() { System.out.println("Modern"); } }
class Report extends LegacyPrinter implements ModernPrinter { }
// new Report().print();  -> dự đoán? Muốn gọi bản "Modern" thì viết thế nào trong Report?
```

---

### Phần C — Bài tập nâng cao (thiết kế / spec)

**Câu 9.** Vì sao Java 8 chọn `default` method thay vì cho phép abstract method mới "có giá trị mặc định coi như đã implement"? Trình bày dưới góc độ *binary compatibility* của `List.stream()` khi thêm vào interface `Collection` — điều gì xảy ra với hàng nghìn class `implements Collection` bên ngoài JDK nếu KHÔNG có `default`?

**Câu 10.** Cho `interface A { default int val() { return 1; } }`, `interface B extends A { default int val() { return 2; } }`, `interface C extends A { }`, `class D implements B, C { }`. `new D().val()` trả gì? Giải thích bằng quy tắc "sub-interface cụ thể hơn thắng" và vì sao đây **không** phải xung đột cần override.

**Câu 11.** Giải thích vì sao `default` method không thể là `synchronized` một cách hữu ích, và vì sao không có `default` cho `equals`. Liên hệ tới việc interface không có identity/state.

**Câu 12.** Một team muốn thêm method `void audit()` vào interface `Repository` đã có 200 class implement trong codebase. Có 3 lựa chọn: (a) abstract method mới; (b) `default` method rỗng `{}`; (c) tách `interface AuditableRepository extends Repository`. Phân tích đánh đổi từng cách (số class phải sửa, nguy cơ "quên implement", rõ ràng về ý định).

**Câu 13.** `abstract class` có thể `implements` một interface mà **không** cài đặt method abstract của interface đó. Vì sao hợp lệ? Ai chịu trách nhiệm cài đặt cuối cùng?

**Câu 14.** So sánh 2 cách tạo bất biến "chỉ 1 abstract method": (a) `abstract class` 1 abstract method; (b) `interface` 1 abstract method (`@FunctionalInterface`). Cái nào dùng được với lambda? Cái nào giữ được state/constructor? Khi nào bắt buộc phải chọn (a)?

**Câu 15.** `sealed interface Shape permits Circle, Square` + `switch` pattern matching không `default`. Thêm `Triangle` vào `permits`. Điều gì xảy ra lúc **compile** với đoạn `switch` cũ, và vì sao đây là ưu điểm so với dùng interface thường + `default:` throw?

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Phần A</summary>

1. **Compile được.** `PI` ngầm là `public static final` → hằng số dùng chung; `Circle` truy cập trực tiếp `PI` (hoặc `Shape.PI`).
2. `"class Person inherits unrelated defaults for greet() from types Greetable and Farewell"`. Sửa: `Person` override `greet()`, bên trong có thể `Greetable.super.greet()` / `Farewell.super.greet()` / logic mới.
3. **Lỗi compile** — `Impl()` không gọi `super(...)`, compiler tự chèn `super()`, nhưng `Base` không có constructor không tham số → "constructor Base() is undefined". Sửa: `Impl()` gọi `super("...")` tường minh.
4. **Không** — static method của interface không kế thừa, không gọi qua instance; kể cả `Service.log(...)` cũng lỗi. Phải `Utils.log("test")`.
5. `"Base"` — quy tắc 1: class/superclass thắng interface. Không xung đột, không cần override.
6. **Không** — `default` không được cung cấp cho method của `Object` (`toString`). Compile lỗi.
7. **Lỗi compile** — `abstract m()` ở `P` khiến `default m()` của `Q` không tự áp dụng; `R` buộc phải cài `m()`.
8. `"B"` — quy tắc 2: `B extends A`, `B` cụ thể hơn → bản của `B` thắng, không cần override.
9. `(2)` lỗi — field interface là `public static final`, phải khởi tạo ngay (`int timeout;` thiếu giá trị). `(3)` lỗi — `private` field trong interface không hợp lệ (chỉ `private` *method* từ Java 9, và phải có thân). `(1)`, `(4)` hợp lệ.
10. Buộc `FileAuditLogger` phải **tự cài đặt** `log(String)` — vì `AuditLogger` đã re-abstract nó; `class FileAuditLogger implements AuditLogger { }` rỗng sẽ **lỗi compile**.
11. `implements AppConstants` làm "nguồn gốc hằng số" rò vào API công khai của class (nó "là một" AppConstants — vô nghĩa) và mọi lớp con thừa hưởng. Thay bằng `final` class + `private` constructor + `public static final` field + `import static`, hoặc `enum`.
12. **Có** (Java 17). Lớp `implements`/`extends` nó phải nằm trong `permits` và phải là `final`, `sealed`, hoặc `non-sealed`.

</details>

<details>
<summary>Phần B — ý chính</summary>

- **Bài 1:** `PremiumVideoPlayer.stop()`: `System.out.println("Lưu vị trí " + pos); MediaPlayer.super.stop();`.
- **Bài 2:** `static Discount percentage(double p){ return price -> price*(1 - p/100); }`; `default Discount andThen(Discount n){ return price -> n.apply(this.apply(price)); }`.
- **Bài 3:** `@Override public void move(){ Walker.super.move(); Swimmer.super.move(); System.out.println("Cả hai!"); }`.
- **Bài 4:** `resize(double f){ width *= f; height *= f; }` trong `Shape`; `Square`/`Circle` chỉ `@Override draw()`.
- **Bài 5:** `Dog` và `Robot` không "is-a SoundMaker" theo phân loại tự nhiên — chúng chỉ *cùng có khả năng* phát âm thanh. Quan hệ đúng là "can-do" → `interface`. Bonus: `Robot` giờ còn `implements` được các interface khác (`Chargeable`, `Programmable`) mà không vướng đơn kế thừa.
- **Bài 6:** `AbstractStack`: `public boolean isEmpty(){ return size()==0; }`, `public E peek(){ if(isEmpty()) throw new NoSuchElementException(); ... }` — nhưng `peek` cần truy cập dữ liệu → thường vẫn để abstract hoặc cài trong `ArrayStack`. Chấp nhận cả hai lời giải nếu giải thích được.
- **Bài 7:** `class A implements JsonSerializable {}` OK (thừa hưởng `toJson()` default). `class B implements StrictJsonSerializable {}` lỗi "does not override abstract method toJson()".
- **Bài 8:** `new Report().print()` in `"Legacy"` (class thắng interface). Muốn gọi "Modern": trong `Report` override `print()` và gọi `ModernPrinter.super.print();`.

</details>

<details>
<summary>Phần C</summary>

- **Câu 9:** Nếu thêm abstract `stream()` vào `Collection`, mọi class `implements Collection` (ArrayList của bạn, thư viện bên thứ ba...) **lập tức lỗi compile** vì thiếu override → không thể nâng JDK mà không sửa/biên dịch lại toàn bộ. `default stream()` cung cấp sẵn cài đặt → code cũ **chạy và biên dịch nguyên vẹn**, chỉ những class muốn tối ưu mới override. Đây là *source & binary compatibility*.
- **Câu 10:** `2`. `B extends A` (override `val` → 2). `C extends A` (không override → vẫn "là" `A.val`). `D implements B, C`: bản của `B` cụ thể hơn (là hậu duệ) so với bản đến qua `C` (chính là `A.val`) → quy tắc 2 chọn `B` → `2`. Không xung đột vì hai bản **có quan hệ** (một là override của cái kia).
- **Câu 11:** `default` method không có `this`-state riêng, không có monitor/lock có ý nghĩa gắn với interface; `synchronized` trên nó chỉ khóa `this` của object implement — không phải "tính năng của interface". `equals` cần đối xứng/bắc cầu dựa trên *state* cụ thể của class → không thể có bản mặc định đúng cho mọi class; ngoài ra `Object.equals` luôn thắng (quy tắc 1).
- **Câu 12:** (a) abstract: buộc mọi nơi cài `audit()` — rõ ý định nhất nhưng phải sửa 200 class ngay. (b) `default {}` rỗng: 0 class phải sửa nhưng dễ "quên" cài thật, hành vi im lặng không audit — nguy hiểm. (c) interface con: chỉ class *cần* audit mới `implements AuditableRepository` — rõ ràng, không đụng 200 class, nhưng chỗ gọi phải biết phân biệt kiểu. Thường (c) tốt nhất cho tính năng "tùy chọn".
- **Câu 13:** `abstract class` bản thân không tạo object được → chưa cần "hoàn thiện hợp đồng". Trách nhiệm cài đặt method abstract còn lại dồn cho **class con cụ thể (concrete) đầu tiên**. Nếu class con vẫn không cài đủ → nó cũng phải `abstract`.
- **Câu 14:** Lambda chỉ dùng với (b) functional interface. State/constructor chỉ (a) `abstract class` có. Bắt buộc chọn (a) khi cần: field chung, constructor validate, method `protected`/`private` chia sẻ, hoặc muốn kiểm soát chặt việc kế thừa (một cây phân cấp cụ thể).
- **Câu 15:** `switch` cũ (không `default`, đã liệt kê `Circle`, `Square`) sẽ **lỗi compile** ngay: "not exhaustive, Triangle not covered". Với interface thường + `default: throw`, việc thiếu nhánh `Triangle` chỉ nổ **lúc runtime** khi gặp `Triangle` đầu tiên. `sealed` chuyển lỗi từ runtime về compile-time.

</details>

---

*File tiếp theo trong lộ trình: **Module 01.6 — SOLID Principles** (5 nguyên lý thiết kế nền tảng, và vì sao Spring thiết kế IoC/DI như vậy).*
