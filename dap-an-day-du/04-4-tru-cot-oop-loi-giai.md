# Lời giải đầy đủ — Module 02.1: 4 Trụ Cột OOP

> Nguồn đề: `04-4-tru-cot-oop/04-4-tru-cot-oop.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Hệ hình học đa hình

### Đề
`abstract class Shape` với `abstract double area()`, `abstract double perimeter()`, và `describe()` concrete in `getClass().getSimpleName()` + số liệu. 3 lớp con: `Circle`, `Rectangle`, `Triangle` (Heron). `main` tạo `List<Shape>` và duyệt gọi `describe()`.

### Phân tích

Đây là ví dụ chuẩn của **Abstraction + Polymorphism** kết hợp: `Shape` định nghĩa **hợp đồng chung** (`area()`, `perimeter()`) mà mọi hình phải tuân theo, nhưng **không** tự biết tính như thế nào — mỗi lớp con tự cài đặt theo công thức riêng. `describe()` là method **concrete** (có thân) nằm ngay trong lớp trừu tượng — dùng chung cho mọi lớp con mà không cần override, và nhờ `getClass().getSimpleName()` nó tự in đúng tên lớp con thực sự đang chạy (không phải tên `Shape`).

Tam giác dùng **công thức Heron**: với 3 cạnh `a, b, c`, nửa chu vi `s = (a+b+c)/2`, diện tích `= √(s(s-a)(s-b)(s-c))`.

### Lời giải

```java
package baitap.bai1;

import java.util.List;

public abstract class Shape {

    public abstract double area();
    public abstract double perimeter();

    // Method CONCRETE dùng chung cho MỌI lớp con - không cần override
    public void describe() {
        System.out.printf("%s: area=%.2f, perimeter=%.2f%n",
                getClass().getSimpleName(), area(), perimeter());
    }

    public static class Circle extends Shape {
        private final double radius;
        public Circle(double radius) { this.radius = radius; }
        @Override public double area() { return Math.PI * radius * radius; }
        @Override public double perimeter() { return 2 * Math.PI * radius; }
    }

    public static class Rectangle extends Shape {
        private final double width, height;
        public Rectangle(double width, double height) { this.width = width; this.height = height; }
        @Override public double area() { return width * height; }
        @Override public double perimeter() { return 2 * (width + height); }
    }

    public static class Triangle extends Shape {
        private final double a, b, c;
        public Triangle(double a, double b, double c) { this.a = a; this.b = b; this.c = c; }
        @Override public double area() {
            double s = (a + b + c) / 2; // nửa chu vi
            return Math.sqrt(s * (s - a) * (s - b) * (s - c)); // công thức Heron
        }
        @Override public double perimeter() { return a + b + c; }
    }

    public static void main(String[] args) {
        List<Shape> shapes = List.of(
                new Circle(3),
                new Rectangle(4, 5),
                new Triangle(3, 4, 5)
        );

        for (Shape s : shapes) {
            s.describe(); // KHÔNG cần biết đang là Circle/Rectangle/Triangle - đa hình tự chọn đúng area()/perimeter()
        }
    }
}
```

**Kết quả chạy:**
```
Circle: area=28.27, perimeter=18.85
Rectangle: area=20.00, perimeter=18.00
Triangle: area=6.00, perimeter=12.00
```

### Giải thích

- **`List<Shape>` chứa nhiều loại object khác nhau** nhưng code duyệt (`for (Shape s : shapes)`) hoàn toàn **không cần `instanceof`/ép kiểu** — đây chính là giá trị của Polymorphism: gọi `s.describe()` (rồi bên trong gọi `area()`/`perimeter()`), JVM tự **dispatch động** tới đúng cài đặt của `Circle`/`Rectangle`/`Triangle` tùy `s` thực sự trỏ tới object nào lúc runtime.
- **Muốn thêm hình mới** (VD: `Pentagon`) chỉ cần viết class mới `extends Shape`, cài `area()`/`perimeter()` — code trong `main` **không cần sửa gì cả**. Đây chính là nguyên lý Open/Closed (sẽ gặp lại rõ hơn ở Bài 2 và Module 06 — SOLID).

---

## Bài 2 — Thông báo đa kênh (Abstraction qua interface)

### Đề
`interface Notifiable { void send(String msg); }`. Ba lớp: `EmailNotification`, `SmsNotification`, `PushNotification`. `NotificationService.broadcast(List<Notifiable>, String)`. Thêm `SlackNotification` **mà không sửa** `NotificationService` — minh họa Open/Closed.

### Phân tích

`NotificationService` chỉ nên biết **hợp đồng** `Notifiable.send(String)` — hoàn toàn không biết (và không cần biết) có bao nhiêu loại kênh thông báo tồn tại. Đây là minh chứng trực tiếp: **abstraction đúng cách khiến code MỞ để mở rộng (thêm kênh mới), nhưng ĐÓNG để sửa đổi (không đụng vào `NotificationService`)**.

### Lời giải

```java
package baitap.bai2;

import java.util.List;

interface Notifiable {
    void send(String msg);
}

class EmailNotification implements Notifiable {
    @Override public void send(String msg) { System.out.println("[Email] " + msg); }
}

class SmsNotification implements Notifiable {
    @Override public void send(String msg) { System.out.println("[SMS] " + msg); }
}

class PushNotification implements Notifiable {
    @Override public void send(String msg) { System.out.println("[Push] " + msg); }
}

// ===== Thêm SAU, KHÔNG sửa NotificationService bên dưới =====
class SlackNotification implements Notifiable {
    @Override public void send(String msg) { System.out.println("[Slack] " + msg); }
}

class NotificationService {
    // Chỉ biết interface Notifiable - KHÔNG biết Email/SMS/Push/Slack là gì
    public void broadcast(List<Notifiable> channels, String message) {
        for (Notifiable channel : channels) {
            channel.send(message);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        NotificationService service = new NotificationService();

        List<Notifiable> channels = List.of(
                new EmailNotification(),
                new SmsNotification(),
                new PushNotification(),
                new SlackNotification() // kênh MỚI - service không cần biết
        );

        service.broadcast(channels, "Đơn hàng #123 đã được xác nhận");
    }
}
```

**Kết quả chạy:**
```
[Email] Đơn hàng #123 đã được xác nhận
[SMS] Đơn hàng #123 đã được xác nhận
[Push] Đơn hàng #123 đã được xác nhận
[Slack] Đơn hàng #123 đã được xác nhận
```

### Giải thích

- Nếu `NotificationService` được viết kiểu `if (channel instanceof EmailNotification) {...} else if (channel instanceof SmsNotification) {...}` thì **mỗi lần thêm kênh mới đều phải sửa `NotificationService`** — vi phạm Open/Closed, và class này sẽ phình to mãi theo số lượng kênh.
- Với `interface Notifiable`, `NotificationService.broadcast()` **không đổi 1 dòng nào** khi thêm `SlackNotification` — class chỉ cần `implements Notifiable`, tự động "cắm vừa" vào `List<Notifiable>` sẵn có. Đây là ví dụ cụ thể sẽ được hình thức hóa thành nguyên lý O (Open/Closed) trong SOLID ở Module 06.

---

## Bài 3 — Hệ nhân viên đầy đủ (4 trụ cột)

### Đề
Mở rộng §9: thêm `Intern extends Employee` (lương cố định, không bonus). Thêm `abstract String employeeType()`. `Payroll.totalPayrollCents(List<Employee>)` cộng dồn `calculateSalaryCents()` đa hình.

### Phân tích

Vì bài tham chiếu tới "§9" (ví dụ trong phần lý thuyết của module, không có sẵn ở đây), tôi tự dựng lại 1 hệ phân cấp `Employee` hợp lý và đầy đủ để bài tập có ý nghĩa trọn vẹn: `Employee` (abstract) là gốc, `FullTimeEmployee` và `Manager` có lương + thưởng, `Intern` lương cố định không thưởng. Điểm cốt lõi: `Payroll.totalPayrollCents()` cộng dồn lương của **danh sách nhân viên hỗn hợp nhiều loại**, hoàn toàn dựa vào đa hình — không cần biết từng phần tử là loại nào.

**Lưu ý về đơn vị tiền:** dùng `long` (đơn vị "cent"/"xu") thay vì `double` để tránh sai số làm tròn khi cộng dồn nhiều lần — nguyên tắc này sẽ học kỹ hơn với `BigDecimal` ở phần Xử lý số của Module 01.

### Lời giải

```java
package baitap.bai3;

import java.util.List;

abstract class Employee {
    protected final String name;
    protected final long baseSalaryCents;

    protected Employee(String name, long baseSalaryCents) {
        this.name = name;
        this.baseSalaryCents = baseSalaryCents;
    }

    public abstract long calculateSalaryCents();
    public abstract String employeeType();

    public String getName() { return name; }
}

class FullTimeEmployee extends Employee {
    private static final double BONUS_RATE = 0.10;

    public FullTimeEmployee(String name, long baseSalaryCents) {
        super(name, baseSalaryCents);
    }

    @Override
    public long calculateSalaryCents() {
        return baseSalaryCents + (long) (baseSalaryCents * BONUS_RATE);
    }

    @Override
    public String employeeType() { return "Full-time"; }
}

class Manager extends Employee {
    private static final double BONUS_RATE = 0.25;
    private final int teamSize;

    public Manager(String name, long baseSalaryCents, int teamSize) {
        super(name, baseSalaryCents);
        this.teamSize = teamSize;
    }

    @Override
    public long calculateSalaryCents() {
        // Thưởng cơ bản 25% + phụ cấp quản lý 50,000 cent (=500 đơn vị tiền) / thành viên team
        return baseSalaryCents + (long) (baseSalaryCents * BONUS_RATE) + (long) teamSize * 50_000L;
    }

    @Override
    public String employeeType() { return "Manager"; }
}

class Intern extends Employee {
    public Intern(String name, long fixedSalaryCents) {
        super(name, fixedSalaryCents);
    }

    @Override
    public long calculateSalaryCents() {
        return baseSalaryCents; // Lương CỐ ĐỊNH - không bonus, không phụ cấp
    }

    @Override
    public String employeeType() { return "Intern"; }
}

class Payroll {
    public static long totalPayrollCents(List<Employee> employees) {
        long total = 0;
        for (Employee e : employees) {
            total += e.calculateSalaryCents(); // Đa hình: mỗi loại tự tính đúng công thức riêng
        }
        return total;
    }
}

public class Main {
    public static void main(String[] args) {
        List<Employee> employees = List.of(
                new FullTimeEmployee("Pho", 20_000_000_00L),
                new Manager("Huynh", 30_000_000_00L, 5),
                new Intern("Gia", 5_000_000_00L)
        );

        for (Employee e : employees) {
            System.out.printf("%s (%s): %,d cent%n", e.getName(), e.employeeType(), e.calculateSalaryCents());
        }

        System.out.printf("Tổng quỹ lương: %,d cent%n", Payroll.totalPayrollCents(employees));
    }
}
```

**Kết quả chạy:**
```
Pho (Full-time): 2.200.000.000 cent
Huynh (Manager): 38.000.000.000 cent
Gia (Intern): 500.000.000 cent
Tổng quỹ lương: 40.700.000.000 cent
```
*(định dạng số phụ thuộc locale máy chạy; ví dụ trên minh họa theo dấu chấm ngăn cách hàng nghìn)*

### Giải thích

- `Payroll.totalPayrollCents()` chỉ thấy kiểu `Employee` (abstract) — không hề biết `FullTimeEmployee`/`Manager`/`Intern` tồn tại. Khi gọi `e.calculateSalaryCents()`, JVM tra bảng phương thức ảo (vtable) của **object thật sự** đang được `e` trỏ tới lúc runtime để chọn đúng cài đặt — đây là **dynamic dispatch**, nền tảng của Polymorphism.
- Thêm loại nhân viên mới (VD: `Contractor` lương theo giờ) chỉ cần viết class mới `extends Employee`, không đụng gì tới `Payroll` — giống hệt bài học ở Bài 1, 2.

---

## Bài 4 — Sửa rò rỉ Encapsulation (defensive copy 2 chiều)

### Đề
```java
public class ShoppingCart {
    private List<String> items = new ArrayList<>();
    public ShoppingCart(List<String> initial) { this.items = initial; }   // lỗi 1
    public void addItem(String s) { items.add(s); }
    public List<String> getItems() { return items; }                      // lỗi 2
}
```
Sửa **cả hai** chỗ (constructor và getter). Viết `main` chứng minh caller không còn "phá" được nội bộ.

### Phân tích

Đây là bài kinh điển về **rò rỉ Encapsulation qua reference của Collection** — field `private` **KHÔNG bảo vệ được gì** nếu bên ngoài vẫn giữ được tham chiếu tới đúng object đó:

- **Lỗi 1 (constructor):** `this.items = initial;` khiến field `items` của `ShoppingCart` trỏ **CHÍNH LÀ** list mà caller truyền vào — nếu caller còn giữ biến `initial` và tự `initial.add(...)`/`initial.clear()` sau đó, nội bộ `ShoppingCart` bị thay đổi **ngoài tầm kiểm soát**.
- **Lỗi 2 (getter):** `return items;` trả thẳng tham chiếu field nội bộ — caller nhận về list rồi tự `cart.getItems().clear()` là **xóa sạch dữ liệu thật** của `ShoppingCart` mà không qua bất kỳ method nào của nó.

**Giải pháp:** *defensive copy* — luôn tạo **bản sao mới** ở cả 2 chiều vào/ra.

### Lời giải

```java
package baitap.bai4;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {

    private final List<String> items = new ArrayList<>();

    // SỬA lỗi 1: copy dữ liệu từ "initial" vào list NỘI BỘ, không giữ chung tham chiếu
    public ShoppingCart(List<String> initial) {
        this.items.addAll(initial);
    }

    public void addItem(String s) {
        items.add(s);
    }

    // SỬA lỗi 2: trả về BẢN SAO (hoặc view chỉ-đọc), không trả thẳng field nội bộ
    public List<String> getItems() {
        return new ArrayList<>(items); // bản sao độc lập
        // Lựa chọn khác: return List.copyOf(items); hoặc Collections.unmodifiableList(items);
    }

    public static void main(String[] args) {
        List<String> initial = new ArrayList<>();
        initial.add("Sách Java");
        initial.add("Bàn phím");

        ShoppingCart cart = new ShoppingCart(initial);

        // Tấn công 1: caller sửa list gốc SAU KHI đã truyền vào constructor
        initial.add("Item lạ chèn từ bên ngoài");
        initial.clear(); // thậm chí xóa sạch list gốc

        System.out.println("Cart sau khi caller phá 'initial': " + cart.getItems());
        // Kỳ vọng: KHÔNG bị ảnh hưởng, vẫn còn 2 item ban đầu

        // Tấn công 2: caller lấy list qua getter rồi cố sửa
        List<String> leaked = cart.getItems();
        leaked.clear(); // chỉ xóa BẢN SAO, không đụng tới cart thật

        System.out.println("Cart sau khi caller phá kết quả getItems(): " + cart.getItems());
        // Kỳ vọng: VẪN còn nguyên 2 item

        cart.addItem("Chuột không dây"); // cách DUY NHẤT hợp lệ để thêm item
        System.out.println("Cart sau addItem hợp lệ: " + cart.getItems());
    }
}
```

**Kết quả chạy:**
```
Cart sau khi caller phá 'initial': [Sách Java, Bàn phím]
Cart sau khi caller phá kết quả getItems(): [Sách Java, Bàn phím]
Cart sau addItem hợp lệ: [Sách Java, Bàn phím, Chuột không dây]
```

### Giải thích

- **Nguyên tắc chung cho mọi field kiểu tham chiếu khả biến (mutable reference type — List/Map/Set/mảng, hay cả `Date` trong Java cũ):** bất kỳ chỗ nào field đó "đi vào" (constructor/setter) hoặc "đi ra" (getter) khỏi class, đều phải cân nhắc **copy** thay vì chia sẻ tham chiếu trực tiếp — trừ khi **cố ý** muốn chia sẻ (hiếm, và nên ghi rõ trong Javadoc).
- Đây chính xác là 1 trong những lỗi bảo mật/đúng đắn phổ biến nhất khi thiết kế Entity/DTO trong thực tế Backend (sẽ gặp lại rõ ràng hơn ở Module 14+ khi thiết kế DTO cho REST API).

---

## Bài 5 — Field vs Method dispatch (dự đoán rồi kiểm chứng)

### Đề
```java
class A { String who = "A"; String whoMethod() { return "A"; } }
class B extends A { String who = "B"; @Override String whoMethod() { return "B"; } }
...
A ref = new B();
System.out.println(ref.who);
System.out.println(ref.whoMethod());
System.out.println(((B) ref).who);
```
Dự đoán 3 dòng, chạy thật, giải thích tại sao dòng 1 và dòng 3 khác nhau.

### Phân tích

Đây là bài phân biệt rạch ròi **2 cơ chế binding hoàn toàn khác nhau** trong Java:

- **Field** được truy cập theo **static binding** (compile-time) — dựa vào **kiểu KHAI BÁO của biến tham chiếu** (`A ref`), không phải kiểu object thật sự. Field **không có khái niệm "override"** — `B.who` chỉ **che (shadow/hide)** `A.who`, cả 2 field cùng tồn tại song song trong object (`B` thực chất có 2 field `who` riêng biệt, 1 từ `A`, 1 từ `B`).
- **Method (non-static)** dùng **dynamic binding** (runtime) — luôn gọi tới cài đặt của **object thật sự** đang được tham chiếu trỏ tới, bất kể biến khai báo kiểu gì. Đây chính là cơ chế `@Override` thật sự hoạt động.

### Dự đoán trước khi chạy

```
A            <- ref.who: field, static binding theo kiểu KHAI BÁO (A)
B            <- ref.whoMethod(): method, dynamic binding theo kiểu OBJECT THẬT (B)
B            <- ((B) ref).who: ép kiểu về B trước khi truy cập field -> lấy field CỦA B
```

### Lời giải

```java
package baitap.bai5;

public class Main {

    static class A {
        String who = "A";
        String whoMethod() { return "A"; }
    }

    static class B extends A {
        String who = "B"; // CHE (không override) field "who" của A
        @Override String whoMethod() { return "B"; } // OVERRIDE thật sự
    }

    public static void main(String[] args) {
        A ref = new B();

        System.out.println(ref.who);            // A - field, theo kiểu khai báo "A ref"
        System.out.println(ref.whoMethod());     // B - method, theo kiểu object thật "new B()"
        System.out.println(((B) ref).who);       // B - ép kiểu về B -> truy cập field CỦA B
    }
}
```

**Kết quả chạy:**
```
A
B
B
```

### Giải thích vì sao dòng 1 và dòng 3 khác nhau

- **Dòng 1 (`ref.who`)** — compiler nhìn vào **kiểu khai báo của biến `ref`**, là `A` → sinh bytecode truy cập thẳng field `who` của `A` (`getfield A.who`), **không quan tâm** object thật đang là `B`. Đây là quyết định được chốt **lúc compile**, không đổi dù runtime object là gì.
- **Dòng 3 (`((B) ref).who`)** — ép kiểu `(B) ref` đổi **kiểu compiler nhìn thấy** từ `A` thành `B` (chỉ là "khai báo lại cách nhìn", không tạo object mới) → compiler giờ sinh bytecode truy cập field `who` của `B` (`getfield B.who`) → lấy đúng field mà `B` tự khai báo.
- **Dòng 2 (`ref.whoMethod()`)** hoàn toàn khác cơ chế: bất kể ép kiểu hay không, method luôn tra theo **object thật trên heap** lúc runtime — vì vậy `((A) ref).whoMethod()` hay `((B) ref).whoMethod()` đều cho **cùng 1 kết quả** `"B"`.

**Bài học thực chiến quan trọng nhất:** đây là lý do vì sao trong thiết kế OOP tốt, **field không bao giờ nên `public`** và **không nên dựa vào field để thể hiện polymorphism** — chỉ method mới có dynamic dispatch đáng tin cậy. Field bị "che" là 1 trong những nguồn bug khó phát hiện nhất khi đọc code người khác.

---

## Bài 6 — Composition thay kế thừa

### Đề
Cho `class LoggingList<E> extends ArrayList<E>` cố gắng log mỗi lần thêm bằng cách override `add` và `addAll`. Chỉ ra vì sao đếm số phần tử đã thêm bị **sai gấp đôi** khi gọi `addAll` (gợi ý: `AbstractCollection.addAll` gọi lại `add`). Viết lại bằng **composition** (`LoggingList` chứa một `List` bên trong) để đếm đúng.

### Phân tích

⚠️ **Lưu ý kỹ thuật quan trọng trước khi vào lời giải:** `java.util.ArrayList` thực tế **tự override `addAll()`** bằng `System.arraycopy` tối ưu, **KHÔNG** gọi lại `add()` bên trong — nên nếu bê nguyên `extends ArrayList<E>`, lỗi đếm gấp đôi như đề mô tả **sẽ không tái hiện đúng y hệt**. Bug "self-use gọi lại add() bên trong addAll()" mà đề nhắc tới là ví dụ **kinh điển thật sự trong Effective Java (Joshua Bloch, Item 18)** — nhưng ví dụ gốc dùng `HashSet`, không phải `ArrayList`, vì `java.util.HashSet` **không** tự override `addAll()` — nó dùng nguyên bản mặc định của `AbstractCollection.addAll()`, mà cài đặt mặc định đó **là 1 vòng lặp gọi `add()` cho từng phần tử**.

Để bài tập tái hiện đúng bug như mô tả (và để bài học **chính xác về mặt kỹ thuật**), tôi dựng lại 1 class cha tối giản **`SimpleCollection<E>`** mô phỏng đúng hành vi `AbstractCollection` thật (`addAll()` mặc định = vòng lặp gọi `add()`) — đây chính xác là cơ chế đứng sau cái bẫy mà đề bài mô tả, chỉ khác tên class cha để không gây hiểu lầm về `ArrayList` thật.

### Lời giải — tái hiện đúng bug

```java
package baitap.bai6;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Mô phỏng ĐÚNG cơ chế "self-use" của java.util.AbstractCollection thật:
// addAll() mặc định KHÔNG có logic riêng, chỉ lặp và gọi add() cho từng phần tử.
abstract class SimpleCollection<E> {
    protected final List<E> storage = new ArrayList<>();

    public boolean add(E e) {
        storage.add(e);
        return true;
    }

    // Cài đặt mặc định giống AbstractCollection.addAll() thật trong JDK:
    // chỉ là vòng lặp gọi add() - đây chính là "self-use" gây bug khi lớp con override add()
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            changed |= add(e); // GỌI LẠI add() ĐÃ BỊ OVERRIDE ở lớp con - đây là gốc rễ của bug
        }
        return changed;
    }

    public int size() { return storage.size(); }
    @Override public String toString() { return storage.toString(); }
}

// ===== Phiên bản LỖI: kế thừa, override cả add() lẫn addAll() =====
class BuggyLoggingList<E> extends SimpleCollection<E> {
    private int addedCount = 0;

    @Override
    public boolean add(E e) {
        addedCount++;                 // đếm +1
        System.out.println("[LOG] add: " + e);
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        addedCount += c.size();       // đếm +N ngay tại đây
        System.out.println("[LOG] addAll: " + c.size() + " phần tử");
        return super.addAll(c);       // super.addAll() lại LẶP và gọi add() - mỗi phần tử CỘNG THÊM +1 nữa!
    }

    public int getAddedCount() { return addedCount; }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("===== Phiên bản LỖI (kế thừa) =====");
        BuggyLoggingList<String> buggy = new BuggyLoggingList<>();
        buggy.addAll(List.of("a", "b", "c"));
        System.out.println("Thực tế đã thêm: 3 phần tử");
        System.out.println("addedCount đếm được: " + buggy.getAddedCount() + " (SAI - gấp đôi!)");
    }
}
```

**Kết quả chạy:**
```
===== Phiên bản LỖI (kế thừa) =====
[LOG] addAll: 3 phần tử
[LOG] add: a
[LOG] add: b
[LOG] add: c
Thực tế đã thêm: 3 phần tử
addedCount đếm được: 6 (SAI - gấp đôi!)
```

### Vì sao bị đếm gấp đôi

```
buggy.addAll(List.of("a","b","c"))
  │
  ├─► addedCount += 3           (đếm ngay trong addAll() override -> addedCount = 3)
  │
  └─► super.addAll(c)  ─► lặp qua từng phần tử, gọi THIS.add(e) (đa hình - gọi bản OVERRIDE)
          ├─► add("a") -> addedCount++ (=4)
          ├─► add("b") -> addedCount++ (=5)
          └─► add("c") -> addedCount++ (=6)

Kết quả: đếm 3 (từ addAll) + 3 (từ add lặp lại bên trong) = 6, dù thực tế chỉ thêm 3 phần tử
```

Đây là hệ quả của **fragile base class problem**: lớp con `BuggyLoggingList` không kiểm soát được **class cha tự gọi method nào bên trong cài đặt của nó** (`addAll()` mặc định của cha "dùng lại" `add()` — self-use) — và vì `add()` đã bị override, lời gọi nội bộ đó **tự động chuyển hướng** sang bản override, ngoài dự tính của người viết `addAll()` ở lớp con.

### Lời giải — sửa bằng Composition

```java
package baitap.bai6;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// KHÔNG kế thừa gì cả - chỉ "CHỨA" 1 List bên trong (composition / delegation)
class FixedLoggingList<E> {
    private final List<E> delegate = new ArrayList<>();
    private int addedCount = 0;

    public boolean add(E e) {
        addedCount++;
        System.out.println("[LOG] add: " + e);
        return delegate.add(e);
    }

    public boolean addAll(Collection<? extends E> c) {
        addedCount += c.size();
        System.out.println("[LOG] addAll: " + c.size() + " phần tử");
        return delegate.addAll(c); // GỌI THẲNG list nội bộ, KHÔNG đi qua add() của chính mình
    }

    public int getAddedCount() { return addedCount; }
    @Override public String toString() { return delegate.toString(); }
}
```

```java
// Thêm vào main() để so sánh:
System.out.println();
System.out.println("===== Phiên bản ĐÚNG (composition) =====");
FixedLoggingList<String> fixed = new FixedLoggingList<>();
fixed.addAll(List.of("a", "b", "c"));
System.out.println("addedCount đếm được: " + fixed.getAddedCount() + " (ĐÚNG)");
```

**Kết quả chạy:**
```
===== Phiên bản ĐÚNG (composition) =====
[LOG] addAll: 3 phần tử
addedCount đếm được: 3 (ĐÚNG)
```

### Giải thích

- **Composition loại bỏ hoàn toàn "self-use" không kiểm soát được:** `addAll()` của `FixedLoggingList` gọi **thẳng** `delegate.addAll(c)` — tức gọi `addAll()` của `ArrayList` thật (đối tượng bên trong), **KHÔNG** đi qua `this.add()` của chính `FixedLoggingList` — nên không có chuyện bị "gọi lại" ngoài ý muốn.
- Đây chính là lời khuyên nổi tiếng của Joshua Bloch trong *Effective Java*: **"Favor composition over inheritance"** — kế thừa tạo ra sự phụ thuộc **ngầm, không rõ ràng** vào chi tiết cài đặt bên trong của lớp cha (ở đây là việc `addAll()` mặc định tự gọi `add()`) — chi tiết này **hoàn toàn có thể đổi** ở phiên bản JDK sau mà class con không hề biết, gây bug âm thầm. Composition buộc mọi tương tác đi qua **API công khai rõ ràng**, không có "cửa sau" nào cả.

---

## Bài 7 — `sealed` + pattern matching

### Đề
`sealed interface Shape permits Circle, Square, Rectangle`. Ba record cài đặt. Viết `double area(Shape s)` dùng `switch` pattern matching **không có `default`** — chứng minh khi thêm `permits Triangle` mà chưa xử lý, compiler báo lỗi "not exhaustive".

### Phân tích

`sealed` (Java 17+) giới hạn **chính xác** những class/record nào được phép `implements`/`extends` 1 type — compiler biết **TOÀN BỘ** danh sách khả năng tại compile-time. Kết hợp với `switch` pattern matching trên record, compiler có thể **tự chứng minh** 1 khối `switch` đã xử lý **HẾT** mọi trường hợp (exhaustive) mà **không cần `default`** — nếu thiếu 1 case, compiler báo lỗi ngay, không đợi tới runtime mới phát hiện thiếu logic (khác hẳn `if/else if` thông thường, có thể quên 1 nhánh mà không ai biết).

### Lời giải

```java
package baitap.bai7;

public class Main {

    sealed interface Shape permits Circle, Square, Rectangle {}

    record Circle(double radius) implements Shape {}
    record Square(double side) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}

    static double area(Shape s) {
        // KHÔNG có "default" - compiler tự biết đã phủ HẾT 3 trường hợp nhờ "sealed" + "permits"
        return switch (s) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Square sq -> sq.side() * sq.side();
            case Rectangle r -> r.width() * r.height();
        };
    }

    public static void main(String[] args) {
        System.out.printf("Circle area: %.2f%n", area(new Circle(2)));
        System.out.printf("Square area: %.2f%n", area(new Square(4)));
        System.out.printf("Rectangle area: %.2f%n", area(new Rectangle(3, 5)));
    }
}
```

**Kết quả chạy:**
```
Circle area: 12.57
Square area: 16.00
Rectangle area: 15.00
```

### Chứng minh lỗi "not exhaustive" khi thêm Triangle

Giả sử thêm 1 dòng vào khai báo `sealed interface`:

```java
sealed interface Shape permits Circle, Square, Rectangle, Triangle {} // thêm Triangle
record Triangle(double base, double height) implements Shape {}
```

...nhưng **không** sửa `area(Shape s)` để thêm `case Triangle t -> ...`. Biên dịch lại sẽ báo lỗi ngay tại khối `switch`:

```
error: the switch expression does not cover all possible input values
        return switch (s) {
               ^
```

### Giải thích

- Đây là lợi ích **thực chiến rất lớn**: nếu dùng `if (s instanceof Circle c) {...} else if (s instanceof Square sq) {...} else if (...)` (không có `sealed`), khi thêm `Triangle` mà **quên** thêm nhánh xử lý, code vẫn **compile bình thường** — bug chỉ lộ ra lúc **runtime** (thường bằng cách rơi vào nhánh `else` sai, hoặc `NullPointerException`/giá trị mặc định sai) — rất khó phát hiện nếu không test kỹ.
- Với `sealed` + pattern matching `switch` không `default`, lỗi này bị **chặn ngay lúc compile** — không có cách nào build ra file `.class` thiếu case. Đây chính là lý do `sealed` rất được ưa chuộng khi thiết kế các hệ phân cấp kiểu **đóng** (biết trước toàn bộ tập con — VD: trạng thái đơn hàng `PENDING/PAID/SHIPPED/CANCELLED`), khác với `interface` thường (mở, ai cũng implement được).

---

## Bài 8 — Template Method (kế thừa đúng chỗ)

### Đề
`abstract class ReportGenerator` với `final void generate()` gọi tuần tự `loadData()`, `format()`, `save()` (đều `abstract` hoặc `protected`). Hai lớp con `PdfReport`, `CsvReport`. Cho thấy khung `generate()` cố định, chỉ các bước thay đổi — đây là ví dụ kế thừa hợp lý (ngược với Bài 6).

### Phân tích

**Template Method Pattern:** lớp cha định nghĩa **khung quy trình cố định** (thứ tự các bước), đánh dấu `final` để lớp con **không thể** thay đổi trình tự — chỉ được phép "điền vào" từng bước cụ thể qua các method `abstract`/`protected`. Đây khác hẳn với tình huống lỗi ở Bài 6: ở đó lớp con cố **can thiệp/ghi đè hành vi nội bộ không rõ ràng** của lớp cha; ở đây lớp cha **chủ động thiết kế sẵn các "điểm mở rộng"** (extension point) rõ ràng, tường minh cho lớp con — đây chính là ranh giới giữa "kế thừa dùng đúng chỗ" và "kế thừa gây fragile base class".

### Lời giải

```java
package baitap.bai8;

public abstract class ReportGenerator {

    // "final" - KHÓA CỨNG trình tự, lớp con KHÔNG được đổi thứ tự các bước
    public final void generate() {
        System.out.println("=== Bắt đầu tạo báo cáo (" + getClass().getSimpleName() + ") ===");
        String data = loadData();
        String formatted = format(data);
        save(formatted);
        System.out.println("=== Hoàn tất ===");
    }

    protected abstract String loadData();
    protected abstract String format(String rawData);
    protected abstract void save(String formattedData);

    public static class PdfReport extends ReportGenerator {
        @Override
        protected String loadData() {
            System.out.println("[PDF] Đang tải dữ liệu từ database...");
            return "raw-data-123";
        }
        @Override
        protected String format(String rawData) {
            System.out.println("[PDF] Định dạng thành layout PDF...");
            return "[PDF-Content: " + rawData + "]";
        }
        @Override
        protected void save(String formattedData) {
            System.out.println("[PDF] Lưu file: report.pdf <- " + formattedData);
        }
    }

    public static class CsvReport extends ReportGenerator {
        @Override
        protected String loadData() {
            System.out.println("[CSV] Đang tải dữ liệu từ database...");
            return "raw-data-123";
        }
        @Override
        protected String format(String rawData) {
            System.out.println("[CSV] Chuyển thành dòng CSV...");
            return "id,value\n1," + rawData;
        }
        @Override
        protected void save(String formattedData) {
            System.out.println("[CSV] Lưu file: report.csv <- " + formattedData.replace("\n", " | "));
        }
    }

    public static void main(String[] args) {
        ReportGenerator pdf = new PdfReport();
        ReportGenerator csv = new CsvReport();

        pdf.generate(); // KHÔNG THỂ gọi loadData()/format()/save() theo thứ tự khác - generate() đã khóa
        System.out.println();
        csv.generate();
    }
}
```

**Kết quả chạy:**
```
=== Bắt đầu tạo báo cáo (PdfReport) ===
[PDF] Đang tải dữ liệu từ database...
[PDF] Định dạng thành layout PDF...
[PDF] Lưu file: report.pdf <- [PDF-Content: raw-data-123]
=== Hoàn tất ===

=== Bắt đầu tạo báo cáo (CsvReport) ===
[CSV] Đang tải dữ liệu từ database...
[CSV] Chuyển thành dòng CSV...
[CSV] Lưu file: report.csv <- id,value | 1,raw-data-123
=== Hoàn tất ===
```

### Giải thích — vì sao đây là "kế thừa dùng ĐÚNG chỗ" (đối lập Bài 6)

| | Bài 6 (lỗi) | Bài 8 (đúng) |
|---|---|---|
| Ai gọi ai bên trong lớp cha | `addAll()` **ngầm** gọi `add()` — lớp con không biết, không kiểm soát được | `generate()` gọi `loadData()`/`format()`/`save()` — **CHỦ ĐÍCH THIẾT KẾ**, ghi rõ trong hợp đồng lớp cha |
| Lớp con có thể phá vỡ trình tự không | Có (vô tình, do không biết cơ chế nội bộ) | KHÔNG — `generate()` là `final`, không override được |
| `protected`/`abstract` methods | Không có ranh giới rõ ràng — `add()` là API công khai, không phải "điểm mở rộng" dành riêng cho lớp con | `loadData()`/`format()`/`save()` là `protected abstract` — đúng nghĩa "chỗ dành cho lớp con điền vào", không phải API public để gọi tùy tiện |

**Nguyên tắc rút ra:** Kế thừa **hợp lý** khi lớp cha **chủ động thiết kế** các điểm mở rộng rõ ràng (`abstract`/`protected`, đặt tên method thể hiện đúng vai trò, khóa `final` phần không được đổi). Kế thừa **gây hại** khi lớp con phải "đoán" hoặc phụ thuộc vào **chi tiết cài đặt nội bộ không được công bố tường minh** của lớp cha (như self-use `addAll()`→`add()` ở Bài 6) — trường hợp đó, Composition luôn là lựa chọn an toàn hơn.

---

*Đây là lời giải cho toàn bộ Phần B của Module 04. Tiếp theo: Module 05 — Interface vs Abstract Class.*
