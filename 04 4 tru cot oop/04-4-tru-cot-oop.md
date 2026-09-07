# Module 02.1 — 4 Trụ Cột OOP (Object-Oriented Programming)

> **Mức độ ưu tiên: Cao** — Phần **gần như chắc chắn bị hỏi** trong mọi buổi phỏng vấn Java. Yêu cầu không phải định nghĩa suông mà phải giải thích bằng ví dụ thực tế, phân biệt được **overriding / overloading / hiding / shadowing**, hiểu **field không đa hình còn method thì có**, và biết **khi nào chọn kế thừa, khi nào chọn composition**. Đây cũng là nền tảng để hiểu IoC/DI của Spring (Module 01.21).

> **Phạm vi bài học:** 4 trụ cột (Encapsulation, Inheritance, Polymorphism, Abstraction); `extends` / `super`; quy tắc override đầy đủ (covariant return, exception, access); dynamic dispatch; upcasting/downcasting & `instanceof`; hiding vs overriding vs shadowing; `abstract class`; kế thừa vs composition; `final`/`sealed` class ở mức nhận biết. Các chủ đề: `interface` chi tiết + `default`/`static` method (Module 01.5), SOLID (Module 01.6), `equals`/`hashCode` (Module 01.7), Generics & bridge method (Module 01.9), Design Pattern (Module 01.16) **không** thuộc bài này — chỉ nhắc khi liên quan.

---

## Mục lục

1. [Tổng quan 4 trụ cột](#1-tổng-quan-4-trụ-cột)
2. [Encapsulation — Tính đóng gói](#2-encapsulation--tính-đóng-gói)
3. [Inheritance — Tính kế thừa](#3-inheritance--tính-kế-thừa)
4. [Upcasting, Downcasting & `instanceof`](#4-upcasting-downcasting--instanceof)
5. [Polymorphism — Tính đa hình](#5-polymorphism--tính-đa-hình)
6. [Hiding vs Overriding vs Shadowing — phân biệt 3 khái niệm](#6-hiding-vs-overriding-vs-shadowing--phân-biệt-3-khái-niệm)
7. [Abstraction — Tính trừu tượng](#7-abstraction--tính-trừu-tượng)
8. [Kế thừa vs Composition](#8-kế-thừa-vs-composition)
9. [4 trụ cột phối hợp trong 1 ví dụ hoàn chỉnh](#9-4-trụ-cột-phối-hợp-trong-1-ví-dụ-hoàn-chỉnh)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Tổng quan 4 trụ cột

| Trụ cột | Câu hỏi cốt lõi | Từ khóa Java |
|---|---|---|
| **Encapsulation** | Làm sao *bảo vệ* trạng thái nội bộ và giữ object luôn ở trạng thái hợp lệ (invariant)? | `private`, getter/setter, `final` field |
| **Inheritance** | Làm sao *tái sử dụng & chuyên biệt hóa* hành vi giữa các class có quan hệ "is-a"? | `extends`, `super` |
| **Polymorphism** | Làm sao *cùng một lời gọi* cho hành vi khác nhau tùy object thực tế? | `@Override`, dynamic dispatch |
| **Abstraction** | Làm sao *ẩn chi tiết cài đặt*, chỉ phơi ra "cái gì cần làm"? | `abstract`, `interface` |

> **Câu hỏi phỏng vấn kinh điển — "4 trụ cột liên hệ thế nào?":** Abstraction định nghĩa *hợp đồng* (cái gì); Inheritance cho phép nhiều class chia sẻ hợp đồng đó; Polymorphism cho phép mỗi class *thực hiện* hợp đồng theo cách riêng và chọn đúng bản lúc runtime; Encapsulation bảo vệ *cách* mỗi class làm nội bộ. Cả 4 phục vụ một mục tiêu: code **dễ mở rộng, dễ thay thế, dễ bảo trì**.

> **is-a vs has-a:** Inheritance mô hình hóa "là một loại của" (`Car` *is-a* `Vehicle`). Composition mô hình hóa "có một" (`Car` *has-a* `Engine`). Chọn sai — dùng kế thừa cho quan hệ has-a — là nguồn nợ kỹ thuật lớn (xem §8).

---

## 2. Encapsulation — Tính đóng gói

**Định nghĩa:** gói dữ liệu (field) + hành vi thao tác trên dữ liệu đó vào cùng một class, **ẩn chi tiết nội bộ**, và chỉ cho tương tác qua một *giao diện được kiểm soát*. Mục tiêu quan trọng nhất: object **luôn ở trạng thái hợp lệ** (bảo toàn *invariant*).

```java
public class BankAccount {
    private long balanceCents;                 // ẩn — dùng số nguyên xu để tránh sai số double

    public void deposit(long cents) {
        if (cents <= 0) throw new IllegalArgumentException("Số tiền nạp phải dương");
        balanceCents += cents;
    }
    public void withdraw(long cents) {
        if (cents <= 0)             throw new IllegalArgumentException("Số tiền rút phải dương");
        if (cents > balanceCents)   throw new IllegalStateException("Số dư không đủ");
        balanceCents -= cents;
    }
    public long getBalanceCents() { return balanceCents; }   // chỉ đọc
}
```

### Encapsulation KHÔNG chỉ là "thêm getter/setter"

Một class chỉ có field `private` + getter/setter phơi bày 1-1 mọi field ra ngoài (gọi là *anemic model* — mô hình thiếu máu) thì về bản chất **không** đóng gói gì — bất kỳ ai vẫn đặt được trạng thái bất kỳ.

- **"Tell, Don't Ask":** thay vì lấy dữ liệu ra ngoài rồi tự xử lý (`if (acc.getBalance() >= x) acc.setBalance(acc.getBalance() - x)`), hãy *bảo* object làm (`acc.withdraw(x)`) — logic nghiệp vụ nằm cùng chỗ với dữ liệu.
- Cân nhắc **không viết setter** cho field không được phép đổi sau khi tạo → dùng `final` + gán trong constructor (immutability).

### Defensive copy — chặn rò rỉ tham chiếu nội bộ

```java
public class Team {
    private final List<String> members;

    public Team(List<String> members) {
        this.members = new ArrayList<>(members);   // COPY ĐẦU VÀO — caller sửa list gốc không ảnh hưởng
    }
    public List<String> getMembers() {
        return List.copyOf(members);               // COPY/READ-ONLY ĐẦU RA — caller không sửa được nội bộ
    }
}
```

> ⚠️ Nếu `getMembers()` trả thẳng `members`, caller gọi `team.getMembers().clear()` là xóa sạch trạng thái nội bộ mà không đi qua bất kỳ kiểm soát nào — lỗi đóng gói phổ biến nhất trong thực tế.

### Các tầng đóng gói khác `private`

- **package-private** (default): ẩn khỏi mọi package khác — hữu ích để một nhóm class hợp tác chặt mà không lộ ra API công khai.
- **module** (JPMS, Java 9): package không `exports` thì `public` cũng vô hình với module khác (Module 01.14).

> **Liên hệ Spring/JPA:** Entity có field `private` + accessor là để Hibernate truy cập *có kiểm soát*, và để tách business logic khỏi thao tác field trực tiếp.

---

## 3. Inheritance — Tính kế thừa

**Định nghĩa:** một class (**subclass**) thừa hưởng field & method (không `private`) từ class khác (**superclass**), biểu diễn quan hệ **"is-a"**.

```java
public class Vehicle {
    protected String brand;
    protected int speed;

    public Vehicle(String brand) { this.brand = brand; }

    public void move() { System.out.println(brand + " đang di chuyển"); }
}

public class Car extends Vehicle {
    private int doors;

    public Car(String brand, int doors) {
        super(brand);                 // gọi constructor lớp cha
        this.doors = doors;
    }
    public void openTrunk() { System.out.println(brand + " mở cốp"); }
}
```

### `super` — 3 cách dùng

| Cú pháp | Ý nghĩa |
|---|---|
| `super(...)` | Gọi constructor lớp cha — phải là câu lệnh đầu tiên của constructor con |
| `super.method()` | Gọi phiên bản method **của lớp cha**, kể cả khi con đã override |
| `super.field` | Truy cập field lớp cha khi con có field trùng tên (xem §6 — shadowing) |

### Constructor & chuỗi khởi tạo (nối tiếp Module 01.3 §8)

- **Constructor không được kế thừa.** Lớp con phải tự khai báo constructor (hoặc nhận default constructor nếu không viết cái nào).
- Constructor con nếu không gọi `this(...)`/`super(...)` tường minh ở dòng đầu → compiler **tự chèn `super();`**.
- Nếu lớp cha **không có** constructor không tham số → constructor con **bắt buộc** gọi `super(<đối số>)` tường minh, nếu không lỗi compile.

```java
class Base { Base(int x) { } }
class Sub extends Base {
    Sub() { }            // ❌ lỗi: "there is no default constructor available in Base"
    Sub(int x) { super(x); }   // ✅
}
```

### Đơn kế thừa với class

```java
class Car extends Vehicle { }              // OK
// class Car extends Vehicle, Machine { }  // ❌ Java không cho đa kế thừa class
```

Java né **Diamond Problem** (hai lớp cha cùng method, con không biết chọn bản nào) bằng cách chỉ cho `extends` một class, và cho `implements` nhiều interface (Module 01.5).

### `Object` — gốc của mọi class

Class không `extends` gì thì ngầm `extends java.lang.Object` → mọi object có sẵn `toString()`, `equals()`, `hashCode()`, `getClass()` (chi tiết Module 01.7).

### Kế thừa làm suy yếu đóng gói của lớp cha

Subclass phụ thuộc vào *chi tiết cài đặt* của superclass (thứ tự gọi method nội bộ, field `protected`...). Sửa lớp cha có thể âm thầm làm hỏng lớp con — gọi là **fragile base class problem**. Đây là một lý do lớn để "ưu tiên composition" (§8).

### Chặn kế thừa: `final` class & `sealed` class

```java
public final class Money { }     // không class nào extends được (như String, Integer...)

public sealed class Shape permits Circle, Square { }   // Java 17 — CHỈ Circle, Square được kế thừa
final class Circle extends Shape { }
non-sealed class Square extends Shape { }              // "mở lại" cho kế thừa tự do
```

`sealed` giúp compiler biết **danh sách đóng** các lớp con → `switch` pattern matching có thể kiểm tra bao phủ đầy đủ (Module 01.2 §5).

---

## 4. Upcasting, Downcasting & `instanceof`

### Upcasting — con → cha, **ngầm định**, luôn an toàn

```java
Car c = new Car("Toyota", 4);
Vehicle v = c;                 // upcast tự động — mọi Car đều là Vehicle
v.move();                      // OK — move() có trong Vehicle
// v.openTrunk();              // ❌ compile lỗi — kiểu tĩnh Vehicle không thấy openTrunk()
```

Upcasting **không đổi object**, chỉ *thu hẹp góc nhìn* (kiểu tĩnh). Object trên heap vẫn là `Car` đầy đủ.

### Downcasting — cha → con, **tường minh**, có thể ném `ClassCastException`

```java
Vehicle v = new Car("Toyota", 4);
Car c = (Car) v;              // OK — object thực đúng là Car
c.openTrunk();

Vehicle v2 = new Motorbike();
Car bad = (Car) v2;          // ClassCastException lúc runtime — object thực là Motorbike
```

**Luôn kiểm tra trước khi downcast:**

```java
if (v instanceof Car) {
    Car c = (Car) v;
    c.openTrunk();
}

// Java 16+ — pattern matching cho instanceof: kiểm tra + ép kiểu + gán trong 1 bước
if (v instanceof Car c) {
    c.openTrunk();            // c chỉ tồn tại ở nhánh điều kiện đúng (flow scoping)
}
```

- `null instanceof X` luôn là `false` (không NPE) → `instanceof` an toàn với `null`.
- `getClass() == Car.class` khác `instanceof Car`: cái đầu **chỉ** khớp đúng lớp `Car`, cái sau khớp cả lớp con của `Car`.

---

## 5. Polymorphism — Tính đa hình

Hai dạng:

### (a) Compile-time (Overloading) — Module 01.3

Cùng tên, khác signature. Method được chọn tại **compile-time** theo kiểu tĩnh của đối số (*static binding*).

### (b) Runtime (Overriding) — trọng tâm

Subclass **định nghĩa lại** method của superclass với **cùng tên + cùng danh sách kiểu tham số**.

```java
class Vehicle { public void makeSound() { System.out.println("..."); } }
class Car     extends Vehicle { @Override public void makeSound() { System.out.println("Bíp bíp"); } }
class Bike    extends Vehicle { @Override public void makeSound() { System.out.println("Brừm"); } }

Vehicle v1 = new Car();
Vehicle v2 = new Bike();
v1.makeSound();   // "Bíp bíp" — chọn theo OBJECT THỰC, không theo kiểu khai báo
v2.makeSound();   // "Brừm"

for (Vehicle v : List.of(new Car(), new Bike(), new Car()))
    v.makeSound();   // mỗi phần tử gọi đúng bản của chính nó
```

### Cơ chế: Dynamic Method Dispatch (late binding)

Mỗi class có một **bảng method (method table / vtable)**; object mang tham chiếu tới bảng của lớp thực sự của nó. Lời gọi `v.makeSound()` được biên dịch thành "gọi slot makeSound trong bảng của object" → **runtime** mới biết chạy bản nào. Đây là nền tảng của Strategy Pattern và cách Spring tiêm nhiều implementation của một interface.

### Quy tắc Override đầy đủ

| Khía cạnh | Ràng buộc |
|---|---|
| Tên + danh sách kiểu tham số | Phải **giống hệt** (khác đi → thành overload, không phải override) |
| Kiểu trả về | Giống, **hoặc** là kiểu con (*covariant return type*): cha trả `Vehicle`, con trả `Car` — hợp lệ |
| Access modifier | **Không được thu hẹp**: `protected` → `protected`/`public` OK; → `private` ❌ |
| Checked exception | Con **không được** khai báo ném checked exception *mới* hoặc *rộng hơn* cha; được ném hẹp hơn / bỏ bớt / ném unchecked tùy ý |
| `final` / `static` / `private` method | **Không override được** (xem §6) |
| Method constructor gọi | Xem cảnh báo dưới |
| `@Override` | Không bắt buộc nhưng **luôn nên dùng** — compiler xác minh bạn thật sự đang override |

```java
class Repo        { Object find() { ... } }
class UserRepo extends Repo {
    @Override User find() { ... }        // ✅ covariant return: User là con của Object
}
```

### `@Override` cứu bạn khỏi bug thầm lặng

```java
class Car extends Vehicle {
    @Override public void makeSond() { }   // gõ thiếu 'u' → KHÔNG có ở cha
    // Không có @Override: compile OK, tưởng đã override, runtime vẫn chạy Vehicle.makeSound() → bug khó tìm
    // Có @Override: compiler báo lỗi ngay
}
```

### ⚠️ Gọi method bị override từ trong constructor lớp cha

```java
class Base {
    Base() { init(); }                 // constructor cha gọi method
    void init() { }
}
class Sub extends Base {
    private String name = "ready";
    @Override void init() { System.out.println(name.length()); }  // NullPointerException!
}
new Sub();
```

Khi `Base()` chạy, phần khởi tạo field của `Sub` **chưa xảy ra** (Module 01.3 §8) → `name` vẫn `null`. **Quy tắc:** constructor chỉ nên gọi `private`/`final`/`static` method.

### Field KHÔNG đa hình — chỉ method mới đa hình

```java
class Base { int x = 1; int getX() { return x; } }
class Sub extends Base { int x = 2; @Override int getX() { return x; } }

Base b = new Sub();
System.out.println(b.x);        // 1  — FIELD chọn theo KIỂU TĨNH (Base)
System.out.println(b.getX());   // 2  — METHOD chọn theo OBJECT THỰC (Sub)
```

Đây là bẫy hay bị hỏi nhất. Ghi nhớ: **field access và static method → kiểu tĩnh; instance method → object thực.**

---

## 6. Hiding vs Overriding vs Shadowing — phân biệt 3 khái niệm

Ba từ nghe giống nhau nhưng ngữ nghĩa hoàn toàn khác:

| Khái niệm | Áp dụng cho | Chọn bản nào lúc chạy? | Có `@Override`? |
|---|---|---|---|
| **Overriding** | instance method (không `final`/`static`/`private`) | Theo **object thực** (đa hình) | Có |
| **Hiding** | `static` method, **hoặc** field, **hoặc** nested type trùng tên | Theo **kiểu tĩnh** (không đa hình) | Không (dùng với static method sẽ báo lỗi/cảnh báo) |
| **Shadowing** | biến (local/tham số) che field cùng tên trong phạm vi hẹp hơn | Theo **phạm vi khai báo** | Không |

```java
class A {
    static void s() { System.out.println("A.s"); }   // sẽ bị HIDING
    void m()        { System.out.println("A.m"); }    // sẽ bị OVERRIDING
    int f = 1;                                        // sẽ bị HIDING (field)
}
class B extends A {
    static void s() { System.out.println("B.s"); }
    @Override void m() { System.out.println("B.m"); }
    int f = 2;
}

A a = new B();
a.s();              // "A.s"  — static: hiding, theo kiểu tĩnh A
a.m();              // "B.m"  — instance: overriding, theo object thực B
System.out.println(a.f);   // 1 — field: hiding, theo kiểu tĩnh A
```

```java
// SHADOWING — trong 1 method:
class C {
    int value = 10;
    void set(int value) {          // tham số 'value' che field 'value'
        value = value;             // gán tham số cho chính nó — field KHÔNG đổi (bug)
        this.value = value;        // đúng: this.value là field
    }
}
```

> **Chỉ Overriding mới là "đa hình".** Hiding và Shadowing đều được phân giải tĩnh lúc biên dịch.

---

## 7. Abstraction — Tính trừu tượng

**Định nghĩa:** ẩn chi tiết cài đặt phức tạp, chỉ phơi ra **"làm được gì" (what)**, không phơi ra **"làm thế nào" (how)**.

> **Abstraction (nguyên lý)** ≠ **`abstract class` (công cụ)**. Ta đạt được abstraction bằng nhiều công cụ: `interface`, `abstract class`, hoặc thậm chí chỉ bằng một class thường với API `public` gọn gàng.

### `abstract class`

```java
public abstract class Shape {
    protected final String color;
    protected Shape(String color) { this.color = color; }   // CÓ constructor — gọi qua super()

    public abstract double area();          // KHÔNG thân — subclass phải cài đặt
    public abstract double perimeter();

    public void describe() {                // method concrete — subclass thừa hưởng
        System.out.printf("%s: S=%.2f, P=%.2f%n", getClass().getSimpleName(), area(), perimeter());
    }
}
```

Đặc điểm:

- **Không `new` trực tiếp được** (`new Shape(...)` ❌), nhưng **có constructor** để lớp con gọi `super(...)`.
- Có thể chứa **field, state, constructor, method concrete, method abstract, static method** — linh hoạt hơn interface (trước Java 8).
- `abstract` method **không được** đồng thời `private`, `static`, hay `final` (vì cả ba đều ngăn override).
- Lớp con **không override hết** abstract method → chính lớp con đó cũng phải khai báo `abstract`.
- Một class có thể `abstract` **dù không có** abstract method nào (chỉ để cấm khởi tạo trực tiếp).

```java
public class Circle extends Shape {
    private final double r;
    public Circle(String color, double r) { super(color); this.r = r; }
    @Override public double area()      { return Math.PI * r * r; }
    @Override public double perimeter() { return 2 * Math.PI * r; }
}

List<Shape> shapes = List.of(new Circle("Đỏ", 5), new Rectangle("Xanh", 4, 6));
for (Shape s : shapes) s.describe();     // gọi thống nhất — không cần biết công thức bên trong
```

> Abstraction thường **đi cùng Polymorphism**: định nghĩa hợp đồng (`area()` phải tồn tại) là abstraction; mỗi lớp con thực thi riêng và JVM chọn đúng bản lúc runtime là polymorphism.

### `interface` — nhắc ngắn (chi tiết Module 01.5)

```java
public interface Payable {
    void processPayment(long cents);        // ngầm public abstract
}
public class CreditCard implements Payable {
    @Override public void processPayment(long cents) { /* ... */ }
}
```

| | `abstract class` | `interface` |
|---|---|---|
| Kế thừa | `extends` **một** | `implements` **nhiều** |
| State (field instance) | Có | Không (chỉ `public static final` hằng) |
| Constructor | Có | Không |
| Method có thân | concrete method | `default` / `static` / `private` (Java 8/9+) |
| Ý nghĩa quan hệ | "is-a" chặt, chia sẻ code chung | "có khả năng..." (capability) |

Bảng đầy đủ + "khi nào dùng cái nào" ở **Module 01.5**.

### Vì sao Abstraction quan trọng ở backend

Lập trình dựa trên **interface** (`PaymentService`) thay vì lớp cụ thể (`CreditCardPaymentServiceImpl`) → đổi implementation (thẻ → ví điện tử) mà **không sửa** nơi sử dụng. Đây là **Dependency Inversion Principle** (chữ D của SOLID — Module 01.6) và cốt lõi của Dependency Injection.

> ⚠️ **Leaky abstraction:** khi chi tiết cài đặt "rò" ra API (ví dụ interface `Repository` ném `SQLException`, hoặc trả về kiểu `ResultSet`). Abstraction tốt che được cả kiểu lỗi và kiểu dữ liệu của tầng dưới.

---

## 8. Kế thừa vs Composition

**Composition:** thay vì `Class B extends A`, cho `B` **chứa** một `A` làm field và ủy thác (delegate) công việc cho nó.

```java
// KẾ THỪA — Stack "là một" ArrayList? (SAI is-a: Stack không nên có add(index), remove(index)...)
class Stack<E> extends ArrayList<E> { }        // rò rỉ mọi method của ArrayList ra ngoài

// COMPOSITION — Stack "có một" list bên trong, chỉ phơi ra push/pop/peek
class Stack<E> {
    private final Deque<E> items = new ArrayDeque<>();
    public void push(E e) { items.push(e); }
    public E pop()        { return items.pop(); }
    public E peek()       { return items.peek(); }
    public boolean isEmpty() { return items.isEmpty(); }
}
```

| Tiêu chí | Kế thừa (`extends`) | Composition (chứa + delegate) |
|---|---|---|
| Quan hệ | "is-a" | "has-a" / "uses-a" |
| Ràng buộc | Cứng, cố định lúc biên dịch, chỉ 1 lớp cha | Linh hoạt, đổi được lúc runtime, ghép nhiều thành phần |
| Đóng gói | Con thấy `protected` của cha → dễ vỡ khi cha đổi (fragile base class) | Chỉ dùng API `public` của thành phần → an toàn hơn |
| Phơi bày API | Con **thừa hưởng toàn bộ** API `public` của cha (kể cả cái không muốn) | Con phơi ra **đúng** cái nó muốn |
| Kiểm thử | Khó mock lớp cha | Dễ — tiêm thành phần giả (mock) |

> **Nguyên tắc (Effective Java, Item 18): "Favor composition over inheritance."** Chỉ dùng kế thừa khi thực sự "is-a" **và** bạn kiểm soát (hoặc tin tưởng) lớp cha được thiết kế để kế thừa (có tài liệu về self-use, hoặc `final`/`sealed` rõ ràng). Nghi ngờ → composition.

> Kế thừa vẫn đúng chỗ: khung sườn thuật toán chung (Template Method), phân cấp kiểu `sealed` cho pattern matching, và khi lớp cha + con nằm cùng một module do bạn kiểm soát.

---

## 9. 4 trụ cột phối hợp trong 1 ví dụ hoàn chỉnh

```java
// ABSTRACTION — hợp đồng chung
public abstract class Employee {
    // ENCAPSULATION — field private + final, validate trong constructor
    private final String name;
    private final long baseSalaryCents;

    protected Employee(String name, long baseSalaryCents) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name trống");
        if (baseSalaryCents < 0)            throw new IllegalArgumentException("lương âm");
        this.name = name;
        this.baseSalaryCents = baseSalaryCents;
    }

    public String getName()          { return name; }
    protected long getBaseSalary()   { return baseSalaryCents; }   // chỉ lớp con cần

    public abstract long calculateSalaryCents();   // ABSTRACTION
    public abstract String employeeType();

    public final void printPayslip() {             // final — khung in cố định, không cho con đổi
        System.out.printf("[%s] %s: %d xu%n", employeeType(), name, calculateSalaryCents());
    }
}

// INHERITANCE — Developer is-a Employee
public class Developer extends Employee {
    private final long bonusPerProjectCents;
    private final int  projectsDone;

    public Developer(String name, long base, long bonus, int projectsDone) {
        super(name, base);
        this.bonusPerProjectCents = bonus;
        this.projectsDone = projectsDone;
    }
    @Override public long calculateSalaryCents() {          // POLYMORPHISM
        return getBaseSalary() + bonusPerProjectCents * projectsDone;
    }
    @Override public String employeeType() { return "DEV"; }
}

public class Manager extends Employee {
    private final long teamBonusCents;
    public Manager(String name, long base, long teamBonus) {
        super(name, base);
        this.teamBonusCents = teamBonus;
    }
    @Override public long calculateSalaryCents() {          // POLYMORPHISM — công thức khác hẳn
        return getBaseSalary() + teamBonusCents;
    }
    @Override public String employeeType() { return "MGR"; }
}
```

```java
public class Payroll {
    public static long totalPayrollCents(List<Employee> staff) {
        long total = 0;
        for (Employee e : staff) {
            e.printPayslip();                  // mỗi object gọi đúng calculateSalaryCents() của nó
            total += e.calculateSalaryCents();
        }
        return total;
    }
    public static void main(String[] args) {
        var staff = List.of(
            new Developer("Pho", 15_000_00, 2_000_00, 3),
            new Manager("An", 25_000_00, 5_000_00)
        );
        System.out.println("Tổng quỹ lương: " + totalPayrollCents(staff));
    }
}
```

Đoạn code này thể hiện **cả 4 trụ cột phối hợp** — cách tư duy OOP "chuẩn" mà phỏng vấn muốn nghe.

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Chủ đề | Điểm mấu chốt |
|---|---|
| Encapsulation | Giữ *invariant*; không phải cứ có getter/setter là đóng gói; "Tell, Don't Ask"; defensive copy đầu vào & đầu ra |
| Immutability | `private final` + không setter → an toàn đa luồng, dễ suy luận; `record` cho sẵn |
| Inheritance | Thừa hưởng member không `private`; constructor **không** kế thừa; đơn kế thừa class |
| `super()` | Compiler tự chèn `super()`; cha không có no-arg ctor → con phải gọi `super(...)` tường minh |
| `final` / `sealed` class | `final` cấm kế thừa; `sealed ... permits` giới hạn danh sách lớp con (Java 17) |
| Fragile base class | Kế thừa khiến con phụ thuộc chi tiết nội bộ của cha → cân nhắc composition |
| Upcasting | Con → cha, ngầm, an toàn, chỉ thu hẹp góc nhìn (kiểu tĩnh) |
| Downcasting | Cha → con, tường minh, có thể `ClassCastException`; kiểm tra bằng `instanceof` (hoặc pattern `instanceof X x`) |
| `instanceof` vs `getClass()==` | `instanceof` khớp cả lớp con; `getClass()==` khớp đúng một lớp; `null instanceof X` = false |
| Overloading | Compile-time, theo kiểu tĩnh đối số (static binding) |
| Overriding | Runtime, theo object thực (dynamic dispatch); cùng tên+kiểu tham số |
| Covariant return | Con được trả kiểu con của kiểu trả về ở cha |
| Override & exception | Con không được ném checked exception mới/rộng hơn cha |
| Override & access | Không được thu hẹp; chỉ giữ nguyên hoặc mở rộng |
| `@Override` | Luôn dùng — bắt lỗi "tưởng override mà không phải" lúc compile |
| Constructor gọi method override | method chạy khi field lớp con còn chưa khởi tạo → chỉ gọi `private`/`final`/`static` |
| Field không đa hình | Field & static method → **kiểu tĩnh**; instance method → **object thực** |
| Hiding | static method / field / nested type trùng tên — phân giải **tĩnh**, không đa hình |
| Shadowing | biến local/tham số che field — phân giải theo phạm vi |
| `abstract class` | Không `new` được nhưng có constructor, field, state, method concrete + abstract |
| abstract method | Không được `private`/`static`/`final`; con không cài hết → con cũng `abstract` |
| Abstraction vs abstract class | Abstraction là nguyên lý; `interface`/`abstract class`/API gọn đều là công cụ đạt nó |
| Leaky abstraction | Chi tiết tầng dưới (kiểu lỗi, kiểu dữ liệu) rò ra API |
| Composition > Inheritance | Ưu tiên "has-a" + delegate; chỉ kế thừa khi is-a thật và cha được thiết kế để kế thừa |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** In ra gì? Giải thích cơ chế.
```java
class Animal { void sound() { System.out.println("Animal"); } }
class Dog extends Animal { @Override void sound() { System.out.println("Woof"); } }
...
Animal a = new Dog();
a.sound();
```

**Câu 2.** Có lỗi compile không?
```java
abstract class Shape { abstract double area(); }
...
Shape s = new Shape();
```

**Câu 3.** Override sau hợp lệ không?
```java
class Parent { protected void display() { } }
class Child extends Parent { private void display() { } }
```

**Câu 4.** Vì sao đoạn này *chưa* đóng gói đúng, dù có `private`?
```java
public class Person {
    private List<String> hobbies = new ArrayList<>();
    public List<String> getHobbies() { return hobbies; }
}
```

**Câu 5.** Vì sao Java cấm đa kế thừa class nhưng cho `implements` nhiều interface?

**Câu 6.** In ra gì?
```java
class Base { int x = 1; int get() { return x; } }
class Sub extends Base { int x = 2; @Override int get() { return x; } }
...
Base b = new Sub();
System.out.println(b.x + " " + b.get());
```

**Câu 7.** In ra gì? Đây là hiding hay overriding?
```java
class A { static String tag() { return "A"; } }
class B extends A { static String tag() { return "B"; } }
...
A a = new B();
System.out.println(a.tag());
```

**Câu 8.** Đoạn nào lỗi compile, đoạn nào chạy được?
```java
class Repo { Object load() { return null; } }
class UserRepo extends Repo {
    @Override String load() { return "u"; }   // (1)
}
class OrderRepo extends Repo {
    @Override Object load() throws java.io.IOException { return null; }  // (2)
}
```

**Câu 9.** Chạy `new Sub()` in ra gì?
```java
class Base { Base() { print(); } void print() { System.out.println("Base.print"); } }
class Sub extends Base {
    String s = "hello";
    @Override void print() { System.out.println("len=" + (s == null ? "null" : s.length())); }
}
```

**Câu 10.** `d` gán được không? `o` thì sao?
```java
Object o = "text";
Integer d = (Integer) o;
```

**Câu 11.** `Stack<String> st = new Stack<>(); st.add(0, "x");` — nếu `Stack extends ArrayList`, dòng thứ hai có hợp lệ không? Điều đó nói lên vấn đề gì của thiết kế kế thừa này?

**Câu 12.** `sealed` có tác dụng gì với `switch` pattern matching? Vì sao compiler quan tâm "danh sách lớp con đóng"?

---

### Phần B — Bài tập viết code

**Bài 1 — Hệ hình học đa hình.**
`abstract class Shape` với `abstract double area()`, `abstract double perimeter()`, và `describe()` concrete in `getClass().getSimpleName()` + số liệu. 3 lớp con: `Circle`, `Rectangle`, `Triangle` (Heron). `main` tạo `List<Shape>` và duyệt gọi `describe()`.

**Bài 2 — Thông báo đa kênh (Abstraction qua interface).**
`interface Notifiable { void send(String msg); }`. Ba lớp: `EmailNotification`, `SmsNotification`, `PushNotification`. `NotificationService.broadcast(List<Notifiable>, String)`. Thêm `SlackNotification` **mà không sửa** `NotificationService` — minh họa Open/Closed.

**Bài 3 — Hệ nhân viên đầy đủ (4 trụ cột).**
Mở rộng §9: thêm `Intern extends Employee` (lương cố định, không bonus). Thêm `abstract String employeeType()`. `Payroll.totalPayrollCents(List<Employee>)` cộng dồn `calculateSalaryCents()` đa hình.

**Bài 4 — Sửa rò rỉ Encapsulation (defensive copy 2 chiều).**
```java
public class ShoppingCart {
    private List<String> items = new ArrayList<>();
    public ShoppingCart(List<String> initial) { this.items = initial; }   // lỗi 1
    public void addItem(String s) { items.add(s); }
    public List<String> getItems() { return items; }                      // lỗi 2
}
```
Sửa **cả hai** chỗ (constructor và getter). Viết `main` chứng minh caller không còn "phá" được nội bộ.

**Bài 5 — Field vs Method dispatch (dự đoán rồi kiểm chứng).**
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

**Bài 6 — Composition thay kế thừa.**
Cho `class LoggingList<E> extends ArrayList<E>` cố gắng log mỗi lần thêm bằng cách override `add` và `addAll`. Chỉ ra vì sao đếm số phần tử đã thêm bị **sai gấp đôi** khi gọi `addAll` (gợi ý: `AbstractCollection.addAll` gọi lại `add`). Viết lại bằng **composition** (`LoggingList` chứa một `List` bên trong) để đếm đúng.

**Bài 7 — `sealed` + pattern matching.**
`sealed interface Shape permits Circle, Square, Rectangle`. Ba record cài đặt. Viết `double area(Shape s)` dùng `switch` pattern matching **không có `default`** — chứng minh khi thêm `permits Triangle` mà chưa xử lý, compiler báo lỗi "not exhaustive".

**Bài 8 — Template Method (kế thừa đúng chỗ).**
`abstract class ReportGenerator` với `final void generate()` gọi tuần tự `loadData()`, `format()`, `save()` (đều `abstract` hoặc `protected`). Hai lớp con `PdfReport`, `CsvReport`. Cho thấy khung `generate()` cố định, chỉ các bước thay đổi — đây là ví dụ kế thừa hợp lý (ngược với Bài 6).

---

### Phần C — Bài tập nâng cao (tư duy JVM / thiết kế)

**Bài 9.** Giải thích vì sao `a.tag()` ở Câu A7 in `"A"` mà `a.whoMethod()` (nếu có) in `"B"`. Trình bày sự khác nhau giữa *virtual dispatch* (dựa method table của object) và *static resolution* (dựa kiểu tĩnh). Vì sao IDE cảnh báo "static method accessed via instance reference"?

**Bài 10.** Viết ví dụ tối thiểu cho *fragile base class problem*: một `InstrumentedHashSet extends HashSet` đếm số phần tử đã thêm bị sai vì `HashSet.addAll` gọi `add`. Sau đó nêu 2 hướng khắc phục và đánh đổi (composition/forwarding vs không override `addAll`).

**Bài 11.** `class Sub extends Base` — `Base` có `void m() throws Exception`. `Sub` override `void m()` (không `throws`). Có hợp lệ không? Ngược lại `Base.m()` không `throws` mà `Sub.m() throws IOException` thì sao? Giải thích theo quy tắc "checked exception khi override".

**Bài 12.** Covariant return type được compiler hiện thực bằng *bridge method*. Với:
```java
class Repo { Object load() {...} }
class UserRepo extends Repo { @Override User load() {...} }
```
`javap -c UserRepo` cho thấy **hai** method `load`. Giải thích method cầu (`Object load()` gọi `User load()`) tồn tại để làm gì (gợi ý: lời gọi qua tham chiếu kiểu `Repo`).

**Bài 13.** Vì sao gọi được `super.method()` nhưng **không** gọi được `super.super.method()`? Điều này liên quan gì tới việc lớp con chỉ được phép "biết" một tầng cha trực tiếp?

**Bài 14.** `abstract class` có thể có `private` method và `static` method, nhưng `abstract` method thì không được `private`/`static`/`final`. Giải thích từng trường hợp vì sao mâu thuẫn về ngữ nghĩa.

**Bài 15.** Thiết kế: bạn cần `AuditableRepository` thêm ghi log cho **mọi** repository hiện có (`UserRepository`, `OrderRepository`, ...). Chọn kế thừa hay composition (decorator)? Trình bày lý do dựa trên: số lớp phải tạo, khả năng kết hợp nhiều "add-on" (audit + cache + retry), và ràng buộc đơn kế thừa.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Phần A</summary>

1. `"Woof"` — kiểu khai báo `Animal` nhưng object thực là `Dog`; dynamic dispatch chọn `Dog.sound()`.
2. **Lỗi compile** — không `new` được `abstract class`. Chỉ tạo từ lớp con cụ thể đã cài đủ abstract method.
3. **Không hợp lệ** — override không được thu hẹp access (`protected` → `private`).
4. `getHobbies()` trả thẳng reference nội bộ → `person.getHobbies().clear()`/`.add(...)` sửa trực tiếp state không qua kiểm soát. Sửa: `return List.copyOf(hobbies);` hoặc `new ArrayList<>(hobbies)`.
5. Đa kế thừa class gây Diamond Problem (hai cha cùng chữ ký, cài đặt khác → mơ hồ). Interface (trước Java 8) chỉ có hợp đồng, không có cài đặt nên không xung đột; từ Java 8 có `default` method thì khi xung đột Java **bắt buộc** class tự override giải quyết tường minh.
6. `1 2` — `b.x` theo kiểu tĩnh `Base` (=1); `b.get()` theo object thực `Sub` (=2).
7. `"A"` — `tag()` là `static` → **hiding**, phân giải theo kiểu tĩnh `A`. Không phải overriding.
8. `(1)` **hợp lệ** — covariant return (`String` là con của `Object`). `(2)` **lỗi compile** — override thêm checked exception (`IOException`) mà cha không khai báo.
9. `len=null` — khi `Base()` gọi `print()`, field `s` của `Sub` chưa được gán (vẫn `null`) → in `len=null` (không NPE vì đã kiểm tra). Nếu bỏ kiểm tra `s == null` sẽ là `NullPointerException`.
10. **Compile được** nhưng **`ClassCastException`** lúc runtime — object thực là `String`, không phải `Integer`. `o` gán bình thường (mọi thứ là `Object`).
11. **Hợp lệ về cú pháp** (`Stack` thừa hưởng `add(int, E)` từ `ArrayList`) — và đó chính là vấn đề: `Stack` lộ ra thao tác chèn giữa, xóa theo index... phá vỡ khái niệm "chỉ LIFO". Kế thừa ở đây sai vì `Stack` **không** thật sự "is-a" `ArrayList` về mặt hợp đồng.
12. `sealed` cho compiler **danh sách đóng** các lớp con → `switch` trên kiểu đó biết đã liệt kê đủ mọi khả năng chưa → cho phép bỏ `default` mà vẫn đảm bảo exhaustive; nếu thêm lớp con mới chưa xử lý, compile lỗi ngay (an toàn hơn phát hiện lúc runtime).

</details>

<details>
<summary>Phần B — ý chính</summary>

- **Bài 1:** Heron: `p=(a+b+c)/2`, `area=sqrt(p*(p-a)*(p-b)*(p-c))`. Giá trị bài học: `describe()` viết **một lần**, chạy đúng cho mọi lớp con.
- **Bài 2:** `broadcast` chỉ phụ thuộc `Notifiable`; thêm `SlackNotification implements Notifiable` là đủ — minh họa Open/Closed.
- **Bài 3:** `Intern.calculateSalaryCents()` trả thẳng `getBaseSalary()`. `totalPayrollCents` cộng dồn qua vòng lặp đa hình.
- **Bài 4:** Constructor: `this.items = new ArrayList<>(initial);`. Getter: `return List.copyOf(items);`. `main`: `cart.getItems().add("x")` ném `UnsupportedOperationException` (list bất biến) và không đổi `items` thật.
- **Bài 5:** `B`, `B`, `B`? — không: dòng 1 `ref.who` = `"A"` (field theo kiểu tĩnh); dòng 2 `ref.whoMethod()` = `"B"` (method theo object thực); dòng 3 `((B)ref).who` = `"B"` (giờ kiểu tĩnh là `B`).
- **Bài 6:** `addAll` của `AbstractCollection` lặp và gọi `add` từng phần tử → nếu `add` đã tăng bộ đếm và `addAll` cũng tăng thêm `c.size()` thì đếm gấp đôi. Composition: `LoggingList` giữ `private List<E> delegate`, chỉ tăng đúng chỗ.
- **Bài 7:** `switch (s) { case Circle c -> ...; case Square sq -> ...; case Rectangle r -> ...; }` — không `default`. Thêm `Triangle` vào `permits` mà không thêm nhánh → "the switch statement does not cover all possible input values".
- **Bài 8:** `generate()` là `final`, gọi `loadData()`/`format()`/`save()`. `PdfReport`/`CsvReport` chỉ override 3 bước. Khung cố định, bước thay đổi — kế thừa hợp lý.

</details>

<details>
<summary>Phần C</summary>

- **Bài 9:** `tag()` static → compiler thay bằng `A.tag()` theo **kiểu tĩnh** `a` (không đọc object). `whoMethod()` instance → biên dịch thành "invoke method ở slot X trong method table của object" → runtime chọn `B`. IDE cảnh báo vì viết `a.tag()` gợi ý sai rằng nó phụ thuộc object.
- **Bài 10:**
  ```java
  class InstrumentedHashSet<E> extends HashSet<E> {
      int added = 0;
      @Override public boolean add(E e) { added++; return super.add(e); }
      @Override public boolean addAll(Collection<? extends E> c) { added += c.size(); return super.addAll(c); }
  }
  new InstrumentedHashSet<>().addAll(List.of("a","b","c"));  // added == 6, không phải 3
  ```
  `HashSet.addAll` (kế thừa từ `AbstractCollection`) gọi `add` từng phần tử → cộng 2 lần. Khắc phục: (1) **không** override `addAll` (dựa vào `add` — nhưng phụ thuộc chi tiết nội bộ, dễ vỡ khi JDK đổi); (2) **forwarding/composition** — bọc một `Set` bên trong, tự cài `add`/`addAll` không gọi lẫn nhau.
- **Bài 11:** `Base void m() throws Exception` → `Sub void m()` (không throws): **hợp lệ** (bỏ bớt/thu hẹp checked exception luôn được). Ngược lại `Sub void m() throws IOException` khi cha không `throws`: **lỗi compile** — không thêm checked exception mới.
- **Bài 12:** Sau erasure, lời gọi qua `Repo r; r.load()` được biên dịch để gọi `Object load()`. `UserRepo` chỉ có `User load()` thì `r.load()` không tìm thấy slot khớp chữ ký `Object load()` → compiler sinh **bridge method** `Object load()` trong `UserRepo`, thân của nó chỉ `return this.load()` (bản `User`). Nhờ đó đa hình vẫn hoạt động khi gọi qua kiểu cha.
- **Bài 13:** `super` chỉ trỏ tới cài đặt của **lớp cha trực tiếp**; cho phép `super.super` sẽ để lớp con "nhảy cóc" bỏ qua override của cha trực tiếp → phá vỡ tính đóng gói và bất biến mà lớp cha thiết lập. Mỗi lớp chỉ chịu trách nhiệm về hợp đồng với cha trực tiếp của nó.
- **Bài 14:** `abstract` = "chưa có thân, buộc lớp con cài". `private` = không thấy ở lớp con → không cài được → mâu thuẫn. `static` = thuộc class, không có dispatch theo object → "override" vô nghĩa. `final` = cấm override → mâu thuẫn trực tiếp với "buộc phải override". Ngược lại `private`/`static` method **có thân** trong abstract class thì hoàn toàn bình thường.
- **Bài 15:** Chọn **composition + decorator**. Kế thừa: mỗi tổ hợp (audit, audit+cache, audit+cache+retry...) cần một lớp con riêng → bùng nổ số lớp; lại vướng đơn kế thừa. Decorator: mỗi add-on là một lớp `implements Repository` bọc một `Repository` khác → xếp chồng tự do lúc runtime (`new AuditRepo(new CacheRepo(new JpaRepo()))`), số lớp tuyến tính, không đụng ràng buộc kế thừa.

</details>

---

*File tiếp theo trong lộ trình: **Module 01.5 — Interface vs Abstract Class** (khi nào dùng cái nào, `default`/`static`/`private` method trong interface, đa kế thừa hành vi qua interface).*
