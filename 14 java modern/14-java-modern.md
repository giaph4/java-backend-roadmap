# Module 06 — Java Modern (8 → 21+)

> **Mức độ ưu tiên: Cao (Java 8 essentials) → Trung bình (9-17) → Bổ sung (21 LTS)** — Code Java hiện đại trông rất khác Java "cổ điển" mà nhiều giáo trình vẫn dạy. Hầu hết công ty tuyển dụng hiện nay yêu cầu thành thạo Java 8 (bắt buộc), và ngày càng nhiều dự án mới chạy thẳng trên Java 17/21 LTS — không cập nhật kiến thức này sẽ khiến code bạn viết trông "cũ kỹ" và bỏ lỡ nhiều công cụ giúp code an toàn, ngắn gọn hơn đáng kể.

---

## Mục lục

1. [`Optional<T>` — chấm dứt NullPointerException](#1-optionalt--chấm-dứt-nullpointerexception)
2. [`record` — data class hiện đại (Java 16+)](#2-record--data-class-hiện-đại-java-16)
3. [`var` — Local Variable Type Inference (Java 10+)](#3-var--local-variable-type-inference-java-10)
4. [`sealed class` — kế thừa có kiểm soát (Java 17+)](#4-sealed-class--kế-thừa-có-kiểm-soát-java-17)
5. [Pattern Matching cho `instanceof` (Java 16+)](#5-pattern-matching-cho-instanceof-java-16)
6. [Pattern Matching cho `switch` (Java 21+)](#6-pattern-matching-cho-switch-java-21)
7. [Record Pattern — destructuring (Java 21+)](#7-record-pattern--destructuring-java-21)
8. [Text Block (Java 15+)](#8-text-block-java-15)
9. [Virtual Threads (Java 21 LTS)](#9-virtual-threads-java-21-lts)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. `Optional<T>` — chấm dứt NullPointerException

`NullPointerException` (NPE) là exception **phổ biến nhất** trong lịch sử Java — người phát minh ra khái niệm `null`, Tony Hoare, từng gọi đó là **"sai lầm trị giá hàng tỷ đô la"**. `Optional<T>` (Java 8+) là 1 "hộp chứa" **có thể có hoặc không có giá trị**, buộc lập trình viên phải **xử lý tường minh** trường hợp không có giá trị, thay vì âm thầm để `null` lan truyền rồi crash bất ngờ ở đâu đó xa nguồn gốc.

### Vấn đề với `null` truyền thống

```java
public User findUserById(Long id) {
    // ... tìm trong database
    return null; // không tìm thấy — trả về null
}

User user = findUserById(999L);
System.out.println(user.getName()); // 💥 NullPointerException nếu quên kiểm tra null!
```

### Dùng `Optional<T>` — buộc caller phải xử lý trường hợp rỗng

```java
public Optional<User> findUserById(Long id) {
    User user = ...; // tìm trong database
    return Optional.ofNullable(user); // tự động bọc thành Optional.empty() nếu user là null
}
```

```java
Optional<User> result = findUserById(999L);

// Cách 1 — kiểm tra tường minh
if (result.isPresent()) {
    System.out.println(result.get().getName());
} else {
    System.out.println("Không tìm thấy user");
}

// Cách 2 — isEmpty() (Java 11+), đọc tự nhiên hơn cho trường hợp phủ định
if (result.isEmpty()) {
    System.out.println("Không tìm thấy user");
}

// Cách 3 — ifPresent() với Lambda, phong cách functional (Module 03.3)
result.ifPresent(user -> System.out.println(user.getName()));

// Cách 4 — giá trị mặc định khi rỗng
User user = result.orElse(new User("Khách vãng lai"));
User user2 = result.orElseGet(() -> createDefaultUser()); // orElseGet — chỉ TẠO giá trị mặc định khi THỰC SỰ cần (lazy), hiệu quả hơn orElse() nếu việc tạo giá trị mặc định tốn kém

// Cách 5 — chủ động ném exception rõ ràng nếu không có giá trị
User user3 = result.orElseThrow(() -> new ResourceNotFoundException("User", id)); // liên hệ Module 04 — Custom Exception
```

### Method tạo `Optional`

```java
Optional<String> present = Optional.of("Hello");      // ném NullPointerException NGAY nếu tham số là null — dùng khi CHẮC CHẮN không null
Optional<String> empty = Optional.empty();              // Optional rỗng, tường minh
Optional<String> nullable = Optional.ofNullable(value); // an toàn — tự chuyển null thành Optional.empty()
```

### Chaining với `map()`/`filter()` — phong cách functional giống Stream

```java
Optional<String> email = findUserById(1L)
    .map(User::getEmail)                    // biến đổi bên trong Optional, giống Stream.map()
    .filter(e -> e.contains("@"))            // giữ lại nếu thỏa điều kiện, ngược lại thành Optional.empty()
    .map(String::toLowerCase);

System.out.println(email.orElse("Không có email hợp lệ"));
```

### ⚠️ Những điều KHÔNG nên làm với `Optional`

```java
// ❌ KHÔNG dùng Optional.get() mà không kiểm tra trước — vẫn ném NoSuchElementException, chỉ "dời" vấn đề chứ không giải quyết
Optional<User> result = findUserById(999L);
User user = result.get(); // 💥 vẫn crash nếu rỗng!

// ❌ KHÔNG dùng Optional làm kiểu tham số method hay field của class/Entity
public class User {
    private Optional<String> nickname; // ❌ Anti-pattern — Optional không được thiết kế để làm field, gây phức tạp khi serialize/persist
}

// ❌ KHÔNG dùng Optional cho kiểu trả về của method mà rồi lại check null thủ công như cũ
public Optional<User> findUser(Long id) {
    Optional<User> result = ...;
    if (result == null) { ... } // ❌ vô nghĩa — Optional CHÍNH LÀ để thay thế việc check null, không phải để check null của chính nó
}
```

> **Quy tắc vàng:** `Optional<T>` chỉ nên dùng làm **kiểu trả về của method** (đặc biệt là method có thể "không tìm thấy" điều gì đó, như `findById`) — **không** dùng làm kiểu tham số, không dùng làm field trong Entity/DTO, không dùng cho Collection (dùng `List` rỗng thay vì `Optional<List<T>>`).

---

## 2. `record` — data class hiện đại (Java 16+)

Đã giới thiệu sơ ở Module 02.4 — đây là phần trình bày đầy đủ hơn.

```java
public record Point(int x, int y) {
    // Java TỰ ĐỘNG sinh: constructor, getter (x(), y() — KHÔNG phải getX()/getY()), equals(), hashCode(), toString()
}
```

```java
Point p1 = new Point(3, 4);
System.out.println(p1.x());      // 3 — accessor, không phải getX()
System.out.println(p1);          // Point[x=3, y=4]
System.out.println(p1.equals(new Point(3, 4))); // true — so sánh dựa trên TẤT CẢ field
```

### Compact Constructor — thêm validate mà không cần viết lại toàn bộ constructor

```java
public record Point(int x, int y) {
    public Point { // KHÔNG có dấu ngoặc () — đây là "compact constructor", cú pháp đặc biệt riêng của record
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Tọa độ không được âm");
        }
        // KHÔNG cần viết this.x = x; this.y = y; — Java tự động làm sau khi compact constructor chạy xong
    }
}
```

### Record có thể có thêm method, static field, implements interface

```java
public record Point(int x, int y) implements Comparable<Point> {
    public double distanceFromOrigin() { // method thường, tính toán dựa trên field có sẵn
        return Math.sqrt(x * x + y * y);
    }

    public static Point origin() { // static factory method
        return new Point(0, 0);
    }

    @Override
    public int compareTo(Point other) {
        return Double.compare(this.distanceFromOrigin(), other.distanceFromOrigin());
    }
}
```

> **Giới hạn của record cần nhớ:** record **luôn là `final`** (không thể `extends` được) và **không thể có thêm instance field** ngoài những field khai báo trong phần tham số — vì bản chất record được thiết kế cho **immutable data carrier** (đối tượng chỉ mang dữ liệu, không thay đổi được sau khi tạo), không phải để thay thế hoàn toàn cho class thông thường có state phức tạp, hành vi thay đổi theo thời gian.

---

## 3. `var` — Local Variable Type Inference (Java 10+)

`var` cho phép compiler **tự suy luận kiểu dữ liệu** của biến local dựa trên giá trị khởi tạo — giảm sự lặp lại không cần thiết, **nhưng vẫn là statically typed** (khác hoàn toàn với `var`/`let` trong JavaScript — kiểu vẫn cố định ngay từ compile-time, chỉ là không cần **gõ tường minh**).

```java
var name = "Pho";                     // compiler suy luận: String
var age = 22;                          // compiler suy luận: int
var students = new ArrayList<Student>(); // compiler suy luận: ArrayList<Student>

// Sau khi khai báo, KHÔNG THỂ gán kiểu khác — var KHÔNG PHẢI kiểu động
// name = 123; // ❌ Lỗi compile — name vẫn là String, chỉ là không cần ghi "String" khi khai báo
```

### Giới hạn của `var` — chỉ dùng được cho local variable

```java
var x = 10;         // ✅ hợp lệ — local variable trong method
// var field;        // ❌ KHÔNG hợp lệ — field của class KHÔNG được dùng var
// public var method() { } // ❌ KHÔNG hợp lệ — kiểu trả về của method KHÔNG được dùng var
// var y;             // ❌ KHÔNG hợp lệ — PHẢI khởi tạo giá trị NGAY khi khai báo, compiler cần giá trị để suy luận kiểu
```

### Khi nào NÊN và KHÔNG NÊN dùng `var`

| Nên dùng `var` | Không nên dùng `var` |
|---|---|
| Kiểu dữ liệu đã RÕ RÀNG từ vế phải (`var list = new ArrayList<String>();`) | Kiểu không rõ ràng từ ngữ cảnh (`var result = calculate();` — người đọc không biết `result` là gì nếu không xem định nghĩa `calculate()`) |
| Kiểu Generic dài dòng (`var map = new HashMap<String, List<Order>>();`) | Làm giảm khả năng đọc code trong các trường hợp phức tạp |

> **Lưu ý quan trọng cho phong cách viết code chuyên nghiệp:** `var` là công cụ **tiện lợi**, không phải "luôn luôn tốt hơn" — lạm dụng `var` ở những chỗ kiểu dữ liệu không rõ ràng làm **giảm khả năng đọc code**, đi ngược lại chính mục đích ban đầu (giảm sự rườm rà, KHÔNG phải giảm sự rõ ràng).

---

## 4. `sealed class` — kế thừa có kiểm soát (Java 17+)

`sealed` cho phép 1 class/interface **giới hạn tường minh danh sách các subclass được phép kế thừa nó** — không cho bất kỳ class nào khác tự do `extends`/`implements` như thông thường.

```java
public sealed interface Shape permits Circle, Rectangle, Triangle {
    double area();
}

public final class Circle implements Shape { // "final" — không cho kế thừa tiếp nữa
    private double radius;
    public Circle(double radius) { this.radius = radius; }
    public double area() { return Math.PI * radius * radius; }
}

public final class Rectangle implements Shape {
    private double width, height;
    public Rectangle(double width, double height) { this.width = width; this.height = height; }
    public double area() { return width * height; }
}

public non-sealed class Triangle implements Shape { // "non-sealed" — MỞ LẠI, cho phép kế thừa tiếp tự do (hiếm dùng hơn)
    private double base, height;
    public Triangle(double base, double height) { this.base = base; this.height = height; }
    public double area() { return 0.5 * base * height; }
}
```

Mỗi subclass của `sealed` type **BẮT BUỘC** phải khai báo là 1 trong 3 trạng thái: `final` (không kế thừa tiếp), `sealed` (tiếp tục giới hạn kế thừa ở tầng sâu hơn), hoặc `non-sealed` (mở lại hoàn toàn tự do).

### Lợi ích lớn nhất: kết hợp với `switch expression` — compiler kiểm tra ĐẦY ĐỦ trường hợp (Exhaustiveness Checking)

```java
public double calculateArea(Shape shape) {
    return switch (shape) {
        case Circle c -> c.area();
        case Rectangle r -> r.area();
        case Triangle t -> t.area();
        // KHÔNG CẦN "default" — vì compiler BIẾT CHẮC CHẮN đây là TOÀN BỘ khả năng có thể có (nhờ "permits" đã khai báo)
    };
}
```

Nếu sau này thêm 1 loại `Shape` mới (ví dụ `Hexagon`) nhưng **quên** thêm `case` xử lý trong `switch` này, **compiler sẽ báo lỗi ngay lập tức** (`"the switch expression does not cover all possible input values"`) — giúp phát hiện thiếu sót **lúc compile-time**, thay vì để lọt đến runtime rồi mới phát hiện thiếu xử lý.

> **So sánh với `enum`:** `sealed` giống như "phiên bản mở rộng" của `enum` — `enum` giới hạn tập hợp các **giá trị (instance)** cố định, còn `sealed` giới hạn tập hợp các **kiểu (type/class)** cố định, mỗi kiểu có thể mang theo state và hành vi phức tạp riêng.

---

## 5. Pattern Matching cho `instanceof` (Java 16+)

Trước Java 16, kiểm tra kiểu bằng `instanceof` luôn cần thêm 1 bước ép kiểu (cast) thủ công sau đó:

```java
// Cách CŨ
if (obj instanceof String) {
    String s = (String) obj; // phải ép kiểu THỦ CÔNG, dư thừa vì đã kiểm tra ở dòng trên rồi
    System.out.println(s.length());
}
```

```java
// Cách MỚI (Java 16+) — Pattern Matching cho instanceof
if (obj instanceof String s) { // "s" là BIẾN MỚI, tự động có kiểu String NGAY BÊN TRONG khối if
    System.out.println(s.length()); // dùng "s" trực tiếp, KHÔNG cần ép kiểu thủ công
}
```

### Kết hợp với điều kiện khác

```java
if (obj instanceof String s && s.length() > 5) { // "s" có thể dùng NGAY trong điều kiện && phía sau
    System.out.println("Chuỗi dài: " + s);
}
```

---

## 6. Pattern Matching cho `switch` (Java 21+)

Mở rộng ý tưởng ở mục 5 vào `switch expression` (đã giới thiệu sơ ở Module 01.2) — cho phép `switch` theo **kiểu dữ liệu (type)**, không chỉ theo giá trị cụ thể.

```java
public String describe(Object obj) {
    return switch (obj) {
        case Integer i when i > 0 -> "Số nguyên dương: " + i; // "when" — thêm điều kiện lọc (guard condition)
        case Integer i -> "Số nguyên không dương: " + i;
        case String s when s.isBlank() -> "Chuỗi rỗng";
        case String s -> "Chuỗi: " + s;
        case null -> "Giá trị null";                            // Java 21+ CHO PHÉP case null trực tiếp trong switch!
        default -> "Kiểu khác: " + obj.getClass().getSimpleName();
    };
}
```

> **Lưu ý quan trọng:** trước Java 21, `switch` trên biến `null` luôn ném `NullPointerException` ngay lập tức — phải kiểm tra `null` riêng **trước** khi vào `switch`. Java 21 cho phép xử lý `case null` **trực tiếp trong switch**, giúp code gọn hơn đáng kể khi cần xử lý cả trường hợp null lẫn nhiều kiểu dữ liệu khác nhau trong cùng 1 khối logic.

---

## 7. Record Pattern — destructuring (Java 21+)

Kết hợp `record` (mục 2) với Pattern Matching (mục 5, 6) — cho phép **"tách" (destructure)** trực tiếp các field bên trong 1 record ngay tại điểm kiểm tra kiểu, không cần gọi từng accessor riêng lẻ.

```java
public record Point(int x, int y) {}

// Cách CŨ (chỉ Pattern Matching thông thường)
if (obj instanceof Point p) {
    System.out.println(p.x() + ", " + p.y());
}

// Cách MỚI (Java 21+) — Record Pattern, "tách" luôn x và y ra thành biến riêng
if (obj instanceof Point(int x, int y)) {
    System.out.println(x + ", " + y); // dùng TRỰC TIẾP x, y — không cần gọi p.x(), p.y()
}
```

### Kết hợp với `switch` — cực kỳ mạnh mẽ cho dữ liệu lồng nhau

```java
public record Point(int x, int y) {}
public record Line(Point start, Point end) {}

public String describe(Object obj) {
    return switch (obj) {
        case Line(Point(var x1, var y1), Point(var x2, var y2)) -> // "tách" LỒNG NHAU 2 tầng ngay trong switch!
            "Đường thẳng từ (" + x1 + "," + y1 + ") đến (" + x2 + "," + y2 + ")";
        case Point(var x, var y) -> "Điểm tại (" + x + "," + y + ")";
        default -> "Không xác định";
    };
}
```

> Đây là tính năng khá mới (Java 21), **chưa phổ biến rộng rãi** trong codebase doanh nghiệp hiện tại (nhiều công ty vẫn chạy Java 11/17), nhưng nên biết vì xu hướng các dự án mới đang dần chuyển sang tận dụng — đặc biệt hữu ích khi làm việc với DTO dạng `record` lồng nhau trong Spring Boot REST API (Module 16).

---

## 8. Text Block (Java 15+)

Giải quyết vấn đề viết chuỗi nhiều dòng (đặc biệt JSON, SQL, HTML) vốn rất khó đọc với cách nối chuỗi truyền thống.

```java
// Cách CŨ — rất khó đọc, dễ lỗi khi quên \n hoặc dấu ""
String json = "{\n" +
              "  \"name\": \"Pho\",\n" +
              "  \"age\": 22\n" +
              "}";

// Cách MỚI — Text Block, dùng """ (3 dấu ngoặc kép)
String json2 = """
    {
      "name": "Pho",
      "age": 22
    }
    """;
```

### Quy tắc thụt lề (indentation) quan trọng

```java
String text = """
    Dòng 1
    Dòng 2
    """;
// Java tự động XÓA phần thụt lề CHUNG NHỎ NHẤT của mọi dòng (dựa trên vị trí của dòng """ đóng)
// → Kết quả thực tế: "Dòng 1\nDòng 2\n" — KHÔNG có khoảng trắng thừa ở đầu mỗi dòng
```

### Ứng dụng thực tế: viết SQL/JSON dễ đọc hơn nhiều

```java
String sql = """
    SELECT u.id, u.name, u.email
    FROM users u
    WHERE u.status = 'ACTIVE'
    ORDER BY u.created_at DESC
    """;
```

> **Liên hệ thực tế:** Text Block cực kỳ hữu ích khi viết native query trong JPA (Module 11), test data JSON cho unit test (Module 17), hoặc bất kỳ đâu cần nhúng chuỗi nhiều dòng — giúp code dễ đọc hơn hẳn so với chuỗi nối `+` truyền thống.

---

## 9. Virtual Threads (Java 21 LTS)

Đây là **thay đổi mang tính cách mạng nhất** của Java trong nhiều năm gần đây (Project Loom), liên hệ trực tiếp với Module 05 (Multithreading) đã học.

### Vấn đề với Platform Thread (Thread truyền thống)

Mỗi `Thread` truyền thống ("Platform Thread") ánh xạ **1-1 với 1 thread hệ điều hành (OS thread)** — tốn kém tài nguyên (như đã nhắc ở Module 05.2), khiến số lượng thread tối đa thực tế trong 1 ứng dụng thường chỉ ở mức **vài nghìn**, dù ứng dụng có thể cần xử lý **hàng trăm nghìn kết nối đồng thời** (điển hình: I/O-bound workload — chờ database, chờ API bên ngoài).

### Virtual Thread — thread "ảo", cực kỳ nhẹ

```java
// Tạo Virtual Thread — cú pháp gần như GIỐNG HỆT Platform Thread
Thread vThread = Thread.ofVirtual().start(() -> {
    System.out.println("Chạy trên Virtual Thread: " + Thread.currentThread());
});

// Hoặc dùng Executor chuyên biệt (khuyến nghị hơn — nhất quán với ExecutorService đã học ở Module 05.2)
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) { // có thể tạo HÀNG TRĂM NGHÌN task mà KHÔNG cạn tài nguyên!
        executor.submit(() -> {
            Thread.sleep(1000); // I/O-bound task — Virtual Thread cực kỳ hiệu quả cho trường hợp này
            return null;
        });
    }
} // executor tự động shutdown khi thoát khối try-with-resources (ExecutorService implements AutoCloseable từ Java 19+)
```

### Cơ chế: nhiều Virtual Thread chia sẻ ít Platform Thread (M:N Scheduling)

```
Hàng trăm nghìn Virtual Thread
        │  │  │  │  │  │
        ▼  ▼  ▼  ▼  ▼  ▼
   ┌──────────────────────┐
   │  JVM Scheduler tự     │  ← khi Virtual Thread bị BLOCK (chờ I/O), JVM tự động "gỡ" nó khỏi
   │  động "mount/unmount"  │    Platform Thread bên dưới, nhường chỗ cho Virtual Thread khác chạy tiếp
   └──────────┬───────────┘
              ▼
   Chỉ VÀI Platform Thread thực sự (ánh xạ đến OS thread)
```

> **Điểm mấu chốt:** khi 1 Virtual Thread gọi thao tác **blocking** (`Thread.sleep()`, đọc/ghi I/O, chờ database phản hồi...), JVM **tự động "unmount"** nó khỏi Platform Thread bên dưới, giải phóng Platform Thread đó để chạy Virtual Thread **khác** — chỉ khi có kết quả, Virtual Thread mới được "mount" trở lại vào 1 Platform Thread rảnh để tiếp tục. Cơ chế này khiến hàng trăm nghìn Virtual Thread "tưởng như" chạy song song thực sự, dù bên dưới chỉ dùng số lượng rất ít Platform Thread.

### Khi nào nên dùng Virtual Thread?

| Phù hợp | Không phù hợp / không cần thiết |
|---|---|
| **I/O-bound** workload: gọi database, gọi API, đọc/ghi file — nhiều task cùng CHỜ | **CPU-bound** workload: tính toán nặng, không có thao tác chờ đợi — Virtual Thread không mang lại lợi ích (vẫn bị giới hạn bởi số nhân CPU thực tế) |
| Cần xử lý **số lượng cực lớn** request/kết nối đồng thời (điển hình: web server hiện đại) | Code cũ dùng `synchronized` **block** dài (không phải toàn method) kết hợp Virtual Thread có thể gây "pinning" (Virtual Thread bị "ghim" chặt vào Platform Thread, mất đi lợi ích chính) — cần cẩn trọng khi áp dụng vào codebase cũ |

> **Liên hệ thực tế:** Spring Boot 3.2+ (chạy trên Java 21+) đã hỗ trợ **bật Virtual Thread cho Tomcat** chỉ bằng 1 dòng cấu hình (`spring.threads.virtual.enabled=true`) — giúp mỗi request HTTP chạy trên 1 Virtual Thread riêng thay vì tranh nhau 1 Platform Thread Pool giới hạn, cải thiện đáng kể khả năng chịu tải (throughput) cho ứng dụng I/O-bound (đa số ứng dụng backend CRUD/API điển hình chính là I/O-bound — chờ database là chủ yếu).

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Tính năng | Java version | Điểm mấu chốt cần nhớ |
|---|---|---|
| `Optional<T>` | 8 | Buộc xử lý tường minh trường hợp rỗng; chỉ dùng làm kiểu trả về, không dùng làm field/tham số |
| `record` | 16 | Tự sinh equals/hashCode/toString/constructor/accessor; immutable, không extends được |
| `var` | 10 | Chỉ dùng cho local variable, kiểu vẫn cố định (static typing), chỉ nên dùng khi kiểu rõ ràng |
| `sealed class/interface` | 17 | Giới hạn tường minh danh sách subclass qua `permits`; hỗ trợ Exhaustiveness Checking với switch |
| Pattern Matching `instanceof` | 16 | Tự động cast, gán biến mới ngay trong điều kiện kiểm tra |
| Pattern Matching `switch` | 21 | Switch theo kiểu dữ liệu, hỗ trợ `when` (guard condition) và `case null` |
| Record Pattern | 21 | "Tách" (destructure) field của record ngay tại điểm kiểm tra kiểu, hỗ trợ lồng nhau |
| Text Block | 15 | `"""..."""`, tự động xử lý thụt lề — lý tưởng cho JSON/SQL/HTML nhiều dòng |
| Virtual Thread | 21 (LTS) | Thread "ảo" siêu nhẹ, lý tưởng cho I/O-bound workload; tự "unmount" khỏi Platform Thread khi blocking |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau có vấn đề gì về mặt thiết kế (dù compile được)?
```java
public Optional<User> findUserById(Long id) {
    Optional<User> result = repository.findById(id);
    User user = result.get(); // ?
    return Optional.of(user);
}
```

**Câu 2.** `record Money(String currency, double amount)` có cho phép viết `money.amount = 100;` để sửa giá trị sau khi tạo không? Giải thích.

**Câu 3.** Đoạn code sau có compile được không? Giải thích theo `sealed`.
```java
public sealed interface Animal permits Dog, Cat {}
public final class Dog implements Animal {}
public final class Cat implements Animal {}
public class Bird implements Animal {} // ?
```

**Câu 4.** So sánh 2 đoạn switch sau — đoạn nào an toàn hơn khi hệ thống mở rộng thêm loại `Shape` mới trong tương lai? Giải thích.
```java
// Đoạn A — Shape là class thường
switch (shapeType) {
    case "CIRCLE" -> ...;
    case "RECTANGLE" -> ...;
    default -> throw new IllegalStateException("Không xác định");
}

// Đoạn B — Shape là sealed interface
switch (shape) {
    case Circle c -> ...;
    case Rectangle r -> ...;
}
```

**Câu 5.** Trong Virtual Thread, điều gì xảy ra khi 1 Virtual Thread gọi `Thread.sleep(5000)`? So sánh với Platform Thread truyền thống.

---

### Phần B — Bài tập viết code

**Bài 1 — Refactor code dùng null sang Optional.**
Cho method sau dùng `null` truyền thống, hãy refactor sang dùng `Optional<T>` đúng chuẩn (bao gồm cả cách gọi nó):
```java
public String getUserEmail(Long userId) {
    User user = database.find(userId);
    if (user == null) return "N/A";
    if (user.getEmail() == null) return "N/A";
    return user.getEmail().toLowerCase();
}
```

**Bài 2 — Thiết kế hệ thống Result bằng sealed interface.**
Thiết kế `sealed interface ApiResult<T> permits Success, Failure` mô phỏng kết quả gọi API (thành công hoặc thất bại), với `record Success<T>(T data) implements ApiResult<T>` và `record Failure<T>(String errorMessage, int errorCode) implements ApiResult<T>`. Viết method `String formatResult(ApiResult<String> result)` dùng Pattern Matching switch (Record Pattern nếu có thể) để xử lý cả 2 trường hợp, trả về chuỗi mô tả kết quả phù hợp.

**Bài 3 — Record Pattern với dữ liệu lồng nhau.**
Cho `record Address(String city, String country)` và `record Customer(String name, Address address)`. Viết method dùng Record Pattern (Java 21+) trong `switch` để trích xuất trực tiếp `city` khi `country` là `"Vietnam"`, in ra `"Khách hàng ở [city], Việt Nam"`; nếu ở quốc gia khác, in `"Khách hàng quốc tế"`.

**Bài 4 — Text Block cho SQL và JSON.**
Viết 1 method trả về `String` chứa 1 câu truy vấn SQL nhiều dòng (join 2-3 bảng giả định, có `WHERE`, `ORDER BY`) bằng Text Block. Viết thêm 1 method trả về `String` chứa mẫu JSON response API giả định (ít nhất 4 field, có 1 field là mảng lồng nhau) cũng bằng Text Block. So sánh (bằng comment) với cách viết nối chuỗi `+` truyền thống để thấy rõ lợi ích về khả năng đọc.

**Bài 5 — Bài toán tổng hợp: mô phỏng xử lý 10,000 "request" bằng Virtual Thread.**
Viết chương trình mô phỏng xử lý 10,000 "request" đồng thời, mỗi request giả lập gọi database (`Thread.sleep(100)` rồi trả về kết quả) — dùng `Executors.newVirtualThreadPerTaskExecutor()`. Đo tổng thời gian thực hiện, so sánh (bằng comment giải thích, không nhất thiết phải chạy thực tế nếu máy yếu) với việc thử làm tương tự bằng `Executors.newFixedThreadPool(200)` (Platform Thread) — nêu nhận xét về sự khác biệt tài nguyên tiêu tốn giữa 2 cách tiếp cận.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. Vi phạm chính nguyên tắc của `Optional` — gọi `.get()` mà không kiểm tra `isPresent()`/dùng `orElseThrow()` trước, nghĩa là vẫn có thể ném `NoSuchElementException` nếu `result` rỗng — đây là anti-pattern "dùng Optional nhưng vẫn code như thể đang dùng null trực tiếp", không tận dụng được lợi ích thực sự của kiểu dữ liệu này.
2. **Không** — `record` là immutable, mọi field được sinh tự động là `private final`, chỉ có accessor (`money.amount()`) để **đọc**, không có setter để **ghi**. Muốn "thay đổi", phải tạo 1 record MỚI (ví dụ `new Money(money.currency(), 100)`).
3. **Không compile được** — `Bird` cố gắng `implements Animal` (1 sealed interface) nhưng **không có tên trong danh sách `permits Dog, Cat`** — compiler báo lỗi vì `sealed` giới hạn NGHIÊM NGẶT chỉ những class được liệt kê tường minh mới được phép implement.
4. **Đoạn B an toàn hơn nhiều** — vì `Shape` là `sealed`, nếu sau này thêm `Triangle` mới vào danh sách `permits`, compiler **sẽ tự động báo lỗi** ở MỌI chỗ dùng `switch` trên `Shape` mà quên xử lý `Triangle` (Exhaustiveness Checking). Đoạn A dùng `String` thông thường — không có cơ chế nào cảnh báo lúc compile-time, lỗi thiếu case chỉ phát hiện được lúc RUNTIME (khi rơi vào nhánh `default` và ném exception), quá muộn so với việc bắt lỗi ngay khi biên dịch.
5. Khi Virtual Thread gọi `Thread.sleep(5000)`, JVM **tự động "unmount"** nó khỏi Platform Thread đang chạy nó, giải phóng Platform Thread đó ngay lập tức để phục vụ Virtual Thread khác — sau 5 giây, Virtual Thread được "mount" trở lại 1 Platform Thread rảnh (không nhất thiết là Platform Thread ban đầu) để tiếp tục chạy. Với Platform Thread truyền thống, gọi `sleep(5000)` khiến chính OS thread đó bị **BLOCK hoàn toàn** trong 5 giây, không thể phục vụ việc gì khác — đây chính là lý do Virtual Thread hiệu quả vượt trội cho khối lượng lớn task I/O-bound.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Lời giải tham khảo:
```java
public String getUserEmail(Long userId) {
    return Optional.ofNullable(database.find(userId))
        .map(User::getEmail)
        .map(String::toLowerCase)
        .orElse("N/A");
}
```
- **Bài 2:** Ví dụ dùng Record Pattern trong switch:
```java
String formatResult(ApiResult<String> result) {
    return switch (result) {
        case Success<String>(String data) -> "Thành công: " + data;
        case Failure<String>(String msg, int code) -> "Lỗi [" + code + "]: " + msg;
    };
}
```
- **Bài 3:** Ví dụ lời giải:
```java
String describeCustomer(Customer customer) {
    return switch (customer) {
        case Customer(var name, Address(var city, var country)) when country.equals("Vietnam") ->
            "Khách hàng ở " + city + ", Việt Nam";
        default -> "Khách hàng quốc tế";
    };
}
```
- **Bài 5:** Nhận xét mong đợi: với Virtual Thread, 10,000 "request" xử lý gần như đồng thời, tổng thời gian gần bằng thời gian của **1** request đơn lẻ (~100ms), vì hầu như không giới hạn số lượng Virtual Thread thực sự tồn tại cùng lúc. Với `newFixedThreadPool(200)`, chỉ có 200 Platform Thread — 10,000 request phải "xếp hàng" theo từng lô 200, tổng thời gian sẽ dài hơn đáng kể (khoảng 10,000/200 × 100ms = 5000ms, gấp 50 lần so với Virtual Thread) — minh họa rất rõ lợi ích thực tế của Virtual Thread cho workload I/O-bound có độ đồng thời (concurrency) cực cao, điển hình của các backend API hiện đại.

</details>

---

*File tiếp theo trong lộ trình: **Module 07 — JVM Internals** (Heap vs Stack, Garbage Collection, ClassLoader, JIT Compiler).*
