# Module 01.3 — Class, Object, Method

> **Mức độ ưu tiên: Cao** — Đây là điểm chuyển từ "viết script chạy tuần tự" sang lập trình hướng đối tượng thực sự. Hiểu sai `static` vs instance, overload resolution, hay thứ tự khởi tạo sẽ tạo ra bug rất khó debug khi code lớn dần — và toàn bộ Dependency Injection của Spring dựa trên khái niệm *instance member* + *constructor injection*.

> **Phạm vi bài học:** class & object (bao gồm reference, vòng đời, `record`, nested class ở mức tổng quan), constructor (chaining, `private`, thứ tự khởi tạo field), method (signature, overloading & overload resolution, varargs), `this` (kể cả qualified `this` và `this` trong lambda/anonymous), `static` vs instance, access modifier (kể cả nuance của `protected` và class), static/instance initializer block. Các chủ đề: kế thừa & `super`, `@Override`, đa hình runtime (Module 01.4), `equals`/`hashCode`/`toString` chi tiết (Module 01.7), interface (Module 01.5), Design Pattern (Module 01.16), đồng bộ hóa static state (Module 01.12) **không** thuộc bài này — chỉ nhắc khi liên quan trực tiếp.

---

## Mục lục

1. [Class & Object](#1-class--object)
2. [Constructor](#2-constructor)
3. [Method & Overloading](#3-method--overloading)
4. [`this` keyword](#4-this-keyword)
5. [`static` vs Instance Member](#5-static-vs-instance-member)
6. [Access Modifier](#6-access-modifier)
7. [Khối khởi tạo — static block & instance block](#7-khối-khởi-tạo--static-block--instance-block)
8. [Thứ tự khởi tạo đầy đủ (kể cả khi có kế thừa)](#8-thứ-tự-khởi-tạo-đầy-đủ-kể-cả-khi-có-kế-thừa)
9. [Tổng kết — Bảng ghi nhớ nhanh](#9-tổng-kết--bảng-ghi-nhớ-nhanh)
10. [Bài tập luyện tập](#10-bài-tập-luyện-tập)

---

## 1. Class & Object

**Class** là **khuôn mẫu (blueprint)** định nghĩa *trạng thái* (field) và *hành vi* (method). **Object** là **thực thể cụ thể (instance)** được tạo từ class, nằm trong **heap** khi chương trình chạy.

```java
public class Student {
    String name;          // instance field — mỗi object 1 bản
    int age;
    double gpa;

    void printInfo() {    // instance method
        System.out.println(name + " - " + age + " tuổi - GPA: " + gpa);
    }
}
```

```java
Student s1 = new Student();   // cấp phát object trên heap; s1 là REFERENCE trỏ tới nó
s1.name = "Pho";
s1.age = 22;

Student s2 = new Student();   // object khác, độc lập hoàn toàn
Student s3 = s1;              // s3 và s1 cùng trỏ MỘT object — sửa qua s3 thấy được qua s1
```

### Reference — không phải object

Biến kiểu class chỉ chứa **địa chỉ tham chiếu**, không chứa object. Hệ quả (xem thêm Module 01.1 §10 *pass-by-value*):

```java
Student a = new Student();
Student b = a;
b.age = 99;
System.out.println(a.age);   // 99 — cùng một object

Student x = new Student();
Student y = new Student();
System.out.println(x == y);        // false — hai object khác nhau (so sánh địa chỉ)
System.out.println(x.equals(y));   // false — Object.equals mặc định cũng so sánh địa chỉ (đến khi override, Module 01.7)

Student z = null;
z.printInfo();               // NullPointerException — reference rỗng, không trỏ object nào
```

### Điều gì xảy ra khi `new Student()` chạy

1. **Cấp phát** vùng nhớ trên heap đủ chứa mọi instance field.
2. **Zero-init** toàn bộ field về giá trị mặc định (`0`/`0.0`/`false`/`'\u0000'`/`null`) — luôn xảy ra, khác biến local.
3. Chạy **field initializer + instance initializer block** theo thứ tự văn bản.
4. Chạy **thân constructor**.
5. Trả về **reference** tới object đã sẵn sàng.

### Vòng đời object & Garbage Collector

Object sống tới khi **không còn reference nào** trỏ tới nó → trở thành "rác" và bị **Garbage Collector** thu hồi ở thời điểm không xác định.

- Không có `delete`/`free` trong Java. Gán `ref = null` chỉ *gỡ* một tham chiếu.
- `finalize()` đã **deprecated** (Java 9) và bị loại bỏ — không bao giờ dựa vào nó để dọn tài nguyên. Dùng `try-with-resources` / `AutoCloseable` (Module 01.11).

### `record` — class dữ liệu cô đọng (Java 16+)

Khi class chỉ để **mang dữ liệu bất biến**, `record` sinh sẵn constructor, getter (`name()` chứ không phải `getName()`), `equals`/`hashCode`/`toString`:

```java
public record Point(int x, int y) { }

Point p = new Point(3, 4);
p.x();                         // 3
new Point(3, 4).equals(p);     // true — so sánh theo giá trị field
```

- Field của record là `private final`; record **không thể kế thừa** class khác và **không thể bị kế thừa** (ngầm `final`).
- **Compact constructor** để validate:
  ```java
  public record Point(int x, int y) {
      public Point {                         // không có danh sách tham số, không gán this.x
          if (x < 0 || y < 0) throw new IllegalArgumentException("toạ độ âm");
      }
  }
  ```

### Các loại class lồng nhau (tổng quan)

| Loại | Khai báo | Đặc điểm chính |
|---|---|---|
| **static nested class** | `static class Inner` bên trong class ngoài | Không giữ tham chiếu tới instance class ngoài; như một top-level class được "đặt nhờ" namespace |
| **inner class** (non-static) | `class Inner` bên trong class ngoài | Mỗi instance gắn với **một instance class ngoài**; truy cập được field của class ngoài; tạo bằng `outer.new Inner()` |
| **local class** | khai báo bên trong một method | Chỉ dùng trong method đó; bắt được biến local *effectively final* |
| **anonymous class** | `new Runnable() { ... }` | Không có tên; định nghĩa + tạo instance cùng lúc |

> Mỗi file `.java` chỉ được có **tối đa một `public` top-level class**, và tên file phải trùng tên class đó.

---

## 2. Constructor

Constructor được gọi **tự động** khi `new` một object, để đưa object về trạng thái hợp lệ ban đầu.

### Constructor KHÔNG phải method

| | Constructor | Method |
|---|---|---|
| Tên | Trùng tên class | Tuỳ ý |
| Kiểu trả về | **Không có** (kể cả `void`) | Bắt buộc (hoặc `void`) |
| Kế thừa? | **Không** | Có |
| Bị override? | **Không** | Có |
| Gọi thế nào | Chỉ qua `new` (hoặc `this(...)`/`super(...)`) | Gọi trực tiếp bằng tên |

> ⚠️ Nếu bạn *vô tình* thêm kiểu trả về, nó không còn là constructor mà thành một **method thường trùng tên class** — compile được nhưng `new` sẽ không gọi nó:
> ```java
> public class Student {
>     public void Student() { ... }   // ĐÂY LÀ METHOD, không phải constructor
> }
> ```

### Default constructor

Nếu class **không viết constructor nào**, compiler sinh một constructor **rỗng, không tham số**, với **access modifier giống class** (`public class` → `public` default constructor).

```java
public class Student {
    String name;
    // compiler ngầm sinh: public Student() { super(); }
}
```

> ⚠️ Ngay khi bạn viết **bất kỳ** constructor nào (dù có tham số), compiler **ngừng** sinh default constructor. Cần `new Student()` thì phải tự viết.

### Constructor Overloading & `this(...)` chaining

```java
public class Student {
    String name;
    int age;
    double gpa;

    public Student() {
        this("Chưa đặt tên", 0);        // gọi constructor 2 tham số
    }

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public Student(String name, int age, double gpa) {
        this(name, age);                // tái dùng logic
        this.gpa = gpa;
    }
}
```

Quy tắc `this(...)`:

- **Phải là câu lệnh đầu tiên** trong constructor (Java 22+ nới lỏng: cho phép vài câu lệnh không tham chiếu `this` phía trước — nhưng cứ coi như "phải đầu tiên" cho an toàn).
- Không được vừa `this(...)` vừa `super(...)` trong cùng constructor (cả hai đều đòi vị trí đầu tiên).
- Không được gọi vòng: `A()` gọi `this(x)` mà `A(x)` lại gọi `this()` → lỗi compile "recursive constructor invocation".

### `private` constructor — chặn khởi tạo từ ngoài

```java
public final class MathUtils {
    private MathUtils() {                        // không ai new được
        throw new AssertionError("Không khởi tạo");
    }
    public static int gcd(int a, int b) { ... }
}
```

Dùng cho **utility class** (toàn method `static`) và **Singleton** (Module 01.16).

### Constructor có thể ném exception

```java
public Connection(String url) throws IOException {
    if (url == null) throw new IllegalArgumentException("url null");   // unchecked
    this.socket = openSocket(url);                                     // có thể ném IOException (checked)
}
```

Nếu constructor ném exception, object **coi như chưa được tạo** — reference không bao giờ được gán.

### Copy constructor — tạo bản sao

```java
public Student(Student other) {
    this.name = other.name;
    this.age = other.age;
    this.gpa = other.gpa;
}
```

Java không có copy constructor tự động (khác C++). Cân nhắc *deep copy* nếu field là kiểu tham chiếu có thể thay đổi.

> ⚠️ **Không gọi method có thể bị override trong constructor.** Khi constructor lớp cha chạy, phần lớp con **chưa khởi tạo**; nếu constructor cha gọi một method mà lớp con override, method đó chạy trên object nửa vời (field lớp con còn `null`/`0`). Chi tiết ở Module 01.4 — ở đây chỉ cần nhớ: **constructor chỉ nên gán field và gọi `private`/`final`/`static` method**.

### Telescoping constructor — anti-pattern

Chuỗi constructor "n tham số, n+1 tham số, n+2..." trở nên khó đọc khi nhiều field tuỳ chọn. Giải pháp là **Builder pattern** (Module 01.16). Ở bài này chỉ cần nhận diện vấn đề.

---

## 3. Method & Overloading

### Cấu trúc

```java
[modifier...] [static] [final] returnType name(paramList) [throws E1, E2] {
    ...
    return value;                 // bắt buộc nếu returnType khác void
}
```

### Chữ ký method (method signature)

Signature = **tên method + danh sách KIỂU tham số (theo thứ tự)**.

**KHÔNG** thuộc signature: kiểu trả về, tên tham số, `throws`, modifier, `final` trên tham số.

```java
int    add(int a, int b) { ... }
double add(int a, int b) { ... }   // ❌ trùng signature add(int,int) — kiểu trả về không tính
```

### Method Overloading

Nhiều method **cùng tên, khác signature** (khác số lượng / kiểu / thứ tự kiểu tham số) trong cùng class (hoặc kế thừa).

```java
class Calculator {
    int    add(int a, int b)          { return a + b; }
    double add(double a, double b)    { return a + b; }   // khác kiểu
    int    add(int a, int b, int c)   { return a + b + c; }// khác số lượng
    int    add(int a, double b)       { return (int)(a + b); }
    int    add(double a, int b)       { return (int)(a + b); }// khác thứ tự kiểu → hợp lệ
}
```

Việc chọn method nào diễn ra tại **compile-time**, dựa trên **kiểu tĩnh** của đối số — gọi là *static binding / compile-time polymorphism* (khác *overriding* là runtime, Module 01.4).

### Overload Resolution — compiler chọn method thế nào (3 pha)

Compiler thử lần lượt, dừng ở pha đầu tiên tìm được method phù hợp:

1. **Pha 1:** chỉ *widening* primitive & *subtype* — **không** autoboxing, **không** varargs.
2. **Pha 2:** cho phép *autoboxing/unboxing*.
3. **Pha 3:** cho phép *varargs*.

Trong mỗi pha, nếu nhiều method khớp, compiler chọn cái **cụ thể nhất (most specific)**; không quyết được → lỗi *"reference to X is ambiguous"*.

```java
void f(int x)      { print("int"); }
void f(long x)     { print("long"); }
void f(Integer x)  { print("Integer"); }
void f(Object x)   { print("Object"); }
void f(int... x)   { print("varargs"); }

f(10);
// Pha 1: f(int) khớp trực tiếp → in "int"
// (f(long) cũng khớp pha 1 nhưng f(int) cụ thể hơn)
```

```java
void g(Integer x) { ... }
void g(long x)    { ... }
g(5);   // in ra? → "long": pha 1 (widening int→long) THẮNG pha 2 (boxing int→Integer)
```

```java
void h(String s) { ... }
void h(Integer i) { ... }
h(null);   // ❌ ambiguous — null khớp cả hai, không cái nào cụ thể hơn.
           // Sửa: h((String) null)
```

> ⚠️ **Bẫy khi thêm overload mới:** một lời gọi đang chạy đúng có thể **âm thầm đổi method được chọn** khi ai đó thêm overload "cụ thể hơn". Với API công khai, cân nhắc đặt tên khác thay vì overload.

### Varargs — số tham số biến đổi

```java
int sum(int... numbers) {          // numbers là int[] bên trong method
    int total = 0;
    for (int n : numbers) total += n;
    return total;
}

sum();              // 0  — mảng rỗng
sum(1, 2, 3);       // 6
int[] arr = {4, 5};
sum(arr);           // 9  — truyền thẳng mảng cũng được
```

Quy tắc & bẫy:

- Varargs **phải là tham số cuối cùng**: `f(String fmt, Object... args)`.
- Một method varargs `f(int...)` và một non-varargs `f(int, int)` cùng tồn tại → lời gọi `f(1, 2)` ưu tiên **non-varargs** (pha 3 xét sau).
- `f((Object[]) null)` truyền `null` làm mảng → `NullPointerException` khi duyệt; `f((Object) null)` truyền mảng 1 phần tử `null`.
- `printf`/`String.format` dùng varargs — truyền sai số lượng/kiểu tham số ⇒ `MissingFormatArgumentException` lúc runtime, compiler không bắt.
- Truyền `T[]` vào `T...` generic có thể sinh cảnh báo *heap pollution* → đánh dấu `@SafeVarargs` nếu chắc chắn an toàn (chi tiết ở Module 01.9).

### Trả về `this` để nối chuỗi (fluent API)

```java
class Query {
    Query select(String c) { ...; return this; }
    Query where(String c)  { ...; return this; }
}
new Query().select("*").where("id = 1");
```

---

## 4. `this` keyword

`this` = tham chiếu tới **chính object đang thực thi** instance method/constructor.

### (a) Phân biệt field với tham số/biến local cùng tên

```java
public Student(String name) {
    this.name = name;    // this.name: field; name: tham số
    // "name = name;" chỉ gán tham số cho chính nó — field KHÔNG đổi (bug im lặng)
}
```

### (b) Gọi constructor khác — `this(...)`

Xem §2.

### (c) Truyền chính object ra ngoài

```java
void register(EventBus bus) {
    bus.subscribe(this);         // đăng ký chính object này làm listener
}
```

### (d) Gọi tường minh method của chính mình

`this.doWork()` ≡ `doWork()` — chỉ cần khi muốn nhấn mạnh, hoặc bị biến local che tên (hiếm).

### `this` **không** tồn tại trong ngữ cảnh static

```java
static void greet() {
    System.out.println(this.name);   // ❌ lỗi compile — static context không có object "hiện tại"
}
```

### Qualified `this` — `OuterClass.this` (trong inner class)

```java
public class Outer {
    private int value = 10;
    class Inner {
        private int value = 20;
        void print() {
            System.out.println(this.value);        // 20 — this của Inner
            System.out.println(Outer.this.value);   // 10 — this của Outer bao ngoài
        }
    }
}
```

### `this` trong lambda vs anonymous class — khác nhau

```java
public class Widget {
    private String id = "W1";

    void anon() {
        Runnable r = new Runnable() {
            public void run() {
                System.out.println(this.getClass());   // this = object Runnable ẩn danh
                System.out.println(Widget.this.id);     // muốn Widget phải qualified
            }
        };
    }

    void lambda() {
        Runnable r = () -> {
            System.out.println(this.id);   // this = Widget — lambda KHÔNG tạo scope 'this' riêng
        };
    }
}
```

> Đây là một khác biệt ngữ nghĩa quan trọng: **lambda không có `this` của riêng nó**, nó "mượn" `this` của method bao quanh; anonymous class thì `this` trỏ tới chính instance ẩn danh.

---

## 5. `static` vs Instance Member

### Instance member — gắn với từng object

```java
class Student {
    String name;            // mỗi object 1 bản
    void printName() { System.out.println(name); }
}
```

### Static member — một bản duy nhất cho cả class

```java
class Student {
    static int totalStudents = 0;      // 1 bản, chia sẻ toàn class
    static final double PASS_GPA = 2.0;// hằng số dùng chung
    String name;

    Student(String name) {
        this.name = name;
        totalStudents++;
    }
    static void printTotal() {          // gọi qua tên class, không cần object
        System.out.println("Tổng: " + totalStudents);
    }
}

new Student("Pho"); new Student("An");
Student.printTotal();                   // Tổng: 2
```

### Sơ đồ vùng nhớ

```
        +---------------------------------+
        |   Class metadata (Metaspace)    |
        |   Student.totalStudents = 2      |  <--- DUNG CHUNG
        |   Student.PASS_GPA     = 2.0     |
        +---------------------------------+
                       ^
          +------------+------------+
          |                         |
   +--------------+          +--------------+
   |  Heap        |          |  Heap        |
   |  name="Pho"  |          |  name="An"   |   <--- RIENG tung object
   +--------------+          +--------------+
```

### Các quy tắc bắt buộc nhớ

> ⚠️ **static method không truy cập trực tiếp instance member** (không có object "hiện tại"):
> ```java
> static void greet() { System.out.println(name); }   // ❌ "non-static field cannot be referenced from a static context"
> ```
> Ngược lại, **instance method dùng được cả static lẫn instance member**.

> ⚠️ **Truy cập static qua một reference instance là hợp lệ nhưng gây hiểu lầm** — và được phân giải theo **kiểu tĩnh** của reference, không phải object thực:
> ```java
> Student s = new Student("X");
> s.totalStudents;          // biên dịch OK nhưng nên viết Student.totalStudents
> Student s2 = null;
> int t = s2.totalStudents; // KHÔNG NPE! — chỉ dùng KIỂU của s2 để tìm field static
> ```

> ⚠️ **static method không bị override, chỉ bị "che" (hiding).** Lời gọi static method phân giải theo **kiểu tĩnh**, không theo object. Chi tiết ở Module 01.4.

### Vì sao `main` là `static`

```java
public static void main(String[] args) { ... }
```

JVM gọi `main` **trước khi có object nào** → phải gọi được mà không cần `new`.

### Class được nạp & khởi tạo khi nào (class initialization)

Static field initializer + static block chỉ chạy khi class **được khởi tạo lần đầu**, kích hoạt bởi: `new`, truy cập static field **không phải hằng compile-time**, gọi static method, khởi tạo class con, dùng reflection... Truy cập một `static final` **hằng compile-time** thì **không** kích hoạt nạp class (giá trị đã được inline — Module 01.1 §1).

### static import

```java
import static java.lang.Math.max;
import static java.lang.Math.PI;
...
double r = max(a, b) * PI;      // không cần Math.
```

Dùng tiết chế — lạm dụng làm mất dấu vết "hàm này ở đâu ra".

### Khi nào static, khi nào instance

| Dùng `static` | Dùng instance |
|---|---|
| Dữ liệu/hành vi dùng chung cả class (bộ đếm, hằng, factory) | Trạng thái riêng của từng thực thể |
| Hàm tiện ích thuần, không phụ thuộc trạng thái (`Math.sqrt`, `Integer.parseInt`) | Hành vi thao tác trên field của chính object |
| `static final` constant | Field mô tả đặc điểm một đối tượng cụ thể |

> ⚠️ **static field khả biến (mutable) = trạng thái toàn cục.** Nhiều thread cùng đọc/ghi → cần đồng bộ hóa (Module 01.12). Trong ứng dụng Spring, hầu như luôn ưu tiên *instance field của bean* thay vì `static`.

---

## 6. Access Modifier

Kiểm soát **phạm vi truy cập** của class, field, method, constructor.

| Modifier | Cùng class | Cùng package | Subclass khác package | Mọi nơi khác |
|---|---|---|---|---|
| `private` | ✅ | ❌ | ❌ | ❌ |
| *(default / package-private)* | ✅ | ✅ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ✅ (có điều kiện — xem dưới) | ❌ |
| `public` | ✅ | ✅ | ✅ | ✅ |

```java
public class Student {
    private String password;   // chỉ trong Student
    String name;               // cùng package
    protected double gpa;       // cùng package + subclass
    public String studentId;    // mọi nơi
}
```

### Áp dụng cho **class**

- **Top-level class**: chỉ `public` hoặc *default* (package-private). Không được `private`/`protected`.
- **Nested class**: cả 4 mức.
- `public` class phải nằm trong file cùng tên.

### Nuance của `protected` (hay bị hỏi)

Subclass ở **package khác** chỉ truy cập được member `protected` **thông qua tham chiếu kiểu chính nó (hoặc con nó)** — không qua tham chiếu kiểu lớp cha:

```java
package a;
public class Base { protected int x; }

package b;
public class Sub extends Base {
    void m(Sub s, Base b) {
        this.x = 1;   // ✅
        s.x = 1;      // ✅ — qua kiểu Sub
        b.x = 1;      // ❌ — qua kiểu Base ở package khác
    }
}
```

### Nguyên tắc "least privilege" & Encapsulation

Mặc định chọn mức **hẹp nhất** có thể; chỉ mở rộng khi thực sự cần. Quy ước Java: **field `private`**, thao tác qua constructor / getter / setter có validate:

```java
public class Student {
    private String name;
    private int age;

    public Student(String name, int age) {   // validate ngay lúc tạo
        setName(name);
        setAge(age);
    }
    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Tên trống");
        this.name = name;
    }
    public int getAge() { return age; }
    public void setAge(int age) {
        if (age < 0 || age > 150)
            throw new IllegalArgumentException("Tuổi không hợp lệ: " + age);
        this.age = age;
    }
}
```

- **Immutability**: nếu không cần thay đổi sau khi tạo → field `private final`, **không có setter**, chỉ getter. An toàn cho đa luồng và dễ suy luận.
- `record` (§1) tự động cho bạn field `private final` + accessor.

> ⚠️ **Getter trả về tham chiếu tới object khả biến bên trong = rò rỉ đóng gói:**
> ```java
> public List<String> getTags() { return tags; }   // caller có thể tags.clear() !
> public List<String> getTags() { return List.copyOf(tags); }   // ✅ trả bản sao chỉ-đọc
> ```

### Module system (Java 9+) — nhắc ngắn

`module-info.java` với `exports <package>` thêm một tầng kiểm soát *trên cả* `public`: một class `public` trong package **không được `exports`** vẫn không nhìn thấy từ module khác. Chi tiết ở Module 01.14.

---

## 7. Khối khởi tạo — static block & instance block

### Static block — chạy **đúng một lần**, khi class được khởi tạo, trước `main`

```java
public class Config {
    static final Map<String, String> DEFAULTS;
    static {
        DEFAULTS = new HashMap<>();
        DEFAULTS.put("timeout", "30");
        DEFAULTS.put("retries", "3");
    }
}
```

Dùng khi khởi tạo static field cần nhiều dòng logic (đọc file, dựng bảng tra cứu...).

> ⚠️ **Exception trong static initializer → `ExceptionInInitializerError`.** Class bị đánh dấu "erroneous"; mọi lần truy cập **sau đó** ném `NoClassDefFoundError`. Đây là loại lỗi khởi động rất khó lần vì stack trace gốc chỉ xuất hiện **một lần**.

### Instance block — chạy **mỗi lần tạo object**, sau field initializer, trước thân constructor

```java
public class Student {
    String name;
    { name = "Chưa đặt tên"; }          // instance initializer
    public Student() { }
}
```

Ít dùng trong code thường (nên đưa logic vào constructor). Hữu ích khi:

- Nhiều constructor cần chung một đoạn khởi tạo mà không tiện đặt trong `this(...)`.
- **Anonymous class** cần "constructor" (không đặt tên constructor được) → dùng instance block.

### Field initializer + block chạy theo **thứ tự văn bản**

```java
public class Order {
    int a = 1;
    { System.out.println("block1: a=" + a + ", b=" + b); } // b chưa khởi tạo ở đây? -> lỗi "illegal forward reference" nếu đọc; ghi thì khác
    int b = 2;
}
```

Trong một block/initializer, chỉ được **đọc** field đã khai báo phía trên; đọc field khai báo phía dưới là *illegal forward reference*.

### Blank final field

`final` field không gán khi khai báo (**blank final**) phải được gán **đúng một lần** — trong initializer **hoặc** trong **mọi** constructor:

```java
public class Circle {
    private final double radius;
    public Circle(double r) {
        if (r <= 0) throw new IllegalArgumentException();
        this.radius = r;                 // gán trong constructor — OK
    }
    // nếu có constructor thứ 2 mà KHÔNG gán radius → lỗi compile
}
```

---

## 8. Thứ tự khởi tạo đầy đủ (kể cả khi có kế thừa)

Câu hỏi phỏng vấn kinh điển. Với `new Sub()` mà `Sub extends Base`:

```
1. Nạp & khởi tạo class (chỉ lần đầu, theo thứ tự cha → con):
     1a. static field init + static block của Base   (thứ tự văn bản)
     1b. static field init + static block của Sub

2. Tạo object (mỗi lần new):
     2a. super(...)  -> chạy phần khởi tạo của Base:
           - instance field init + instance block của Base (thứ tự văn bản)
           - thân constructor Base
     2b. instance field init + instance block của Sub (thứ tự văn bản)
     2c. thân constructor Sub
```

```java
class Base {
    static { System.out.println("A: static Base"); }
    { System.out.println("C: instance Base"); }
    Base() { System.out.println("D: ctor Base"); }
}
class Sub extends Base {
    static { System.out.println("B: static Sub"); }
    { System.out.println("E: instance Sub"); }
    Sub() { System.out.println("F: ctor Sub"); }
}

new Sub();
new Sub();
// A B C D E F   (lần 1: static cha, static con, rồi instance cha, ctor cha, instance con, ctor con)
// C D E F       (lần 2: static block KHÔNG chạy lại)
```

> Mọi constructor mà không viết `this(...)` hoặc `super(...)` ở dòng đầu thì compiler **tự chèn `super();`** (gọi constructor không tham số của lớp cha). Nếu lớp cha **không có** constructor không tham số → lỗi compile, phải gọi `super(...)` tường minh (chi tiết Module 01.4).

---

## 9. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Reference vs object | Biến class chứa địa chỉ; `b = a` → cùng object; `x == y` so sánh địa chỉ |
| `new` | cấp phát → zero-init → field/instance-block (thứ tự văn bản) → constructor → trả reference |
| Vòng đời | GC thu hồi khi không còn reference; không có `delete`; `finalize()` bỏ |
| `record` | class dữ liệu bất biến; field `private final`; sinh sẵn accessor/`equals`/`hashCode`; compact constructor để validate; ngầm `final` |
| Constructor | Không phải method: không kiểu trả về, không kế thừa, không override; thêm return type → thành method thường |
| Default constructor | Chỉ sinh khi class không có constructor nào; access modifier theo class |
| `this(...)` | Câu lệnh đầu tiên; không cùng lúc với `super(...)`; cấm gọi vòng |
| `private` constructor | Utility class / Singleton |
| Constructor & override | Đừng gọi method bị override trong constructor — object còn nửa vời |
| Method signature | tên + kiểu tham số; **không** gồm kiểu trả về / tên tham số / `throws` |
| Overload resolution | Pha 1 widening → Pha 2 boxing → Pha 3 varargs; chọn "most specific"; `f(null)` giữa 2 reference → ambiguous |
| Overload vs override | Overload = compile-time (static binding); override = runtime (Module 01.4) |
| Varargs | `T...` là `T[]`; phải là tham số cuối; non-varargs thắng varargs; `printf` sai đối số → lỗi runtime |
| `this` | object hiện tại; không có trong static; `Outer.this` trong inner class |
| `this` lambda vs anonymous | lambda mượn `this` của method bao ngoài; anonymous class có `this` riêng |
| static method | Không đọc instance member trực tiếp; không bị override (chỉ "hiding"); phân giải theo kiểu tĩnh |
| static qua instance ref | Hợp lệ nhưng xấu; dùng kiểu tĩnh, `null.staticField` **không** NPE |
| Class init | static block/initializer chạy 1 lần khi class được init; hằng compile-time không kích hoạt init |
| Access modifier | `private` < package-private < `protected` < `public`; top-level class chỉ `public`/default |
| `protected` khác package | Subclass chỉ truy cập qua tham chiếu kiểu chính nó, không qua kiểu lớp cha |
| Encapsulation | field `private` (+ `final` nếu bất biến); getter trả bản sao nếu nội bộ khả biến |
| static initializer lỗi | `ExceptionInInitializerError` → sau đó `NoClassDefFoundError` |
| Thứ tự khởi tạo có kế thừa | static cha → static con (1 lần) → [ super: field/block cha → ctor cha ] → field/block con → ctor con |

---

## 10. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Có lỗi compile không? Dòng nào, vì sao?
```java
public class Counter {
    int count;
    static void increment() { count++; }
}
```

**Câu 2.** `new Product()` có hợp lệ không?
```java
public class Product {
    String name;
    public Product(String name, double price) { this.name = name; }
}
```

**Câu 3.** In ra thứ tự gì?
```java
public class Test {
    static { System.out.println("A"); }
    { System.out.println("B"); }
    public Test() { System.out.println("C"); }
    public static void main(String[] args) { new Test(); new Test(); }
}
```

**Câu 4.** Hai method sau có gây lỗi "duplicate method"?
```java
void process(int a, double b) { }
void process(double a, int b) { }
```

**Câu 5.** Sửa lỗi và giải thích vì sao field không được cập nhật:
```java
public class Account {
    private double balance;
    public Account(double balance) { balance = balance; }
}
```

**Câu 6.** In ra gì? Giải thích theo overload resolution.
```java
static void f(long x)    { System.out.println("long"); }
static void f(Integer x) { System.out.println("Integer"); }
static void f(Object x)  { System.out.println("Object"); }
public static void main(String[] a) { f(5); }
```

**Câu 7.** Dòng nào lỗi compile? Dòng nào chạy nhưng đáng ngờ?
```java
class A { static int k = 10; int v = 1; }
...
A a = null;
int x = a.k;      // (1)
int y = a.v;      // (2)
int z = A.k;      // (3)
```

**Câu 8.** `main` in ra gì?
```java
public class P {
    private String tag = "outer";
    void run() {
        Runnable r1 = new Runnable() { public void run() { System.out.println(tag); } };
        Runnable r2 = () -> System.out.println(this.tag);
        r1.run(); r2.run();
    }
    public static void main(String[] a) { new P().run(); }
}
```

**Câu 9.** Đoạn nào compile lỗi?
```java
public class Circle {
    private final double r;
    public Circle(double r) { this.r = r; }
    public Circle() { }               // (?)
}
```

**Câu 10.** In ra gì và vì sao chỉ một dòng "static" xuất hiện?
```java
class Base { static { System.out.print("Sb "); } { System.out.print("Ib "); } Base() { System.out.print("Cb "); } }
class Sub extends Base { static { System.out.print("Ss "); } { System.out.print("Is "); } Sub() { System.out.print("Cs "); } }
public class M { public static void main(String[] a) { new Sub(); System.out.println(); new Sub(); } }
```

**Câu 11.** `h(null)` gọi được không? Nếu không, cách sửa?
```java
void h(String s)  { }
void h(Integer i) { }
```

**Câu 12.** `Config.NAME` có làm chạy static block của `Config` không?
```java
class Config {
    static final String NAME = "app";
    static { System.out.println("init Config"); }
}
// ... System.out.println(Config.NAME);
```

---

### Phần B — Bài tập viết code

**Bài 1 — `BankAccount` hoàn chỉnh.**
Field `private`: `accountNumber` (String, **`final`**), `balance` (double), `ownerName` (String). Hai constructor: đủ 3 tham số; và chỉ `ownerName` (balance = 0, `accountNumber` tự sinh `"ACC" + (++totalAccounts)` bằng static counter). `deposit`/`withdraw` validate số âm và rút quá số dư (`IllegalArgumentException`). Getter cho cả 3, **không** setter cho `accountNumber`. Static `totalAccounts` + static `printTotal()`. Dùng `this(...)` chaining.

**Bài 2 — `Rectangle` với `this(...)`.**
`Rectangle()` → vuông cạnh 1; `Rectangle(double side)` → vuông; `Rectangle(double w, double h)` → chữ nhật. Constructor ngắn gọi constructor dài qua `this(...)` (không lặp code gán field). Thêm `area()`, `perimeter()`, và `Rectangle(Rectangle other)` (copy constructor).

**Bài 3 — static vs instance qua bài toán lương.**
`Employee`: instance `name`, `baseSalary`; static `companyBonusRate`. `double calculateFinalSalary()` dùng cả hai. `static void updateBonusRate(double r)`. Trong `main`: tạo 3 nhân viên, in lương, gọi `updateBonusRate`, in lương lại — chứng minh **cả 3** đều đổi vì cùng đọc một static field.

**Bài 4 — Encapsulation + validate qua setter.**
`Student` field `private`: `name`, `age`, `email`. Setter validate: `name` không rỗng/không toàn khoảng trắng; `age` 16..100; `email` chứa `@` và có `.` sau `@`. Constructor gọi lại setter để dùng chung validate. `main` thử giá trị hợp lệ và không hợp lệ, in message exception.

**Bài 5 — Utility class đúng chuẩn.**
`StringUtils` chỉ có static method: `isBlank(String)`, `reverse(String)`, `capitalize(String)`, `repeat(String, int)`. Constructor `private` ném `AssertionError`. Class `final`. Viết `main` gọi thử vài method.

**Bài 6 — Overload resolution (dự đoán rồi kiểm chứng).**
Viết class có các overload `describe(int)`, `describe(long)`, `describe(Integer)`, `describe(Object)`, `describe(int...)`. Gọi lần lượt với: `describe(1)`, `describe(1L)`, `describe(Integer.valueOf(1))`, `describe("x")`, `describe(1, 2, 3)`, `describe()`. **Dự đoán** output từng dòng theo 3 pha resolution, rồi chạy để đối chiếu.

**Bài 7 — `record` + compact constructor.**
`record Money(long amountCents, String currency)`. Compact constructor: `currency` không null, đúng 3 ký tự in hoa; `amountCents` không âm. Thêm method `Money plus(Money other)` (chỉ cộng nếu cùng `currency`, nếu không ném `IllegalArgumentException`) và `String format()` trả `"12.34 USD"`. Chứng minh `new Money(1234,"USD").equals(new Money(1234,"USD"))` là `true`.

**Bài 8 — Thứ tự khởi tạo có kế thừa (dự đoán trước).**
```java
class Animal {
    static { System.out.println("static Animal"); }
    { System.out.println("instance Animal"); }
    Animal() { System.out.println("ctor Animal"); }
}
class Dog extends Animal {
    static { System.out.println("static Dog"); }
    { System.out.println("instance Dog"); }
    Dog() { System.out.println("ctor Dog"); }
}
public class Zoo {
    public static void main(String[] args) {
        new Dog();
        System.out.println("----");
        new Dog();
    }
}
```
Ghi output dự đoán, chạy thật, giải thích từng dòng.

---

### Phần C — Bài tập nâng cao (tư duy compiler / JVM)

**Bài 9.** Giải thích tại sao đoạn sau in `"Base"` chứ không phải `"Sub"`, dù đối tượng thực là `Sub`. Đây là *hiding* hay *overriding*?
```java
class Base { static String who() { return "Base"; } }
class Sub extends Base { static String who() { return "Sub"; } }
...
Base b = new Sub();
System.out.println(b.who());
```

**Bài 10.** Constructor lớp cha gọi một method bị lớp con override. Viết ví dụ tối thiểu cho thấy method chạy trên object mà field lớp con **vẫn là `null`/`0`**. Giải thích theo thứ tự khởi tạo ở §8, và nêu quy tắc phòng tránh.

**Bài 11.** Một class có static block ném `RuntimeException`. Viết `main` bắt (`catch (Throwable t)`) lần truy cập đầu tiên và lần thứ hai. Hai lần ném ra **hai loại lỗi khác nhau** — cho biết tên và giải thích vì sao lần thứ hai không còn stack trace gốc.

**Bài 12.** Cho `void m(List<String> a)` và `void m(ArrayList<String> a)`. Với `m(new ArrayList<>())` compiler chọn cái nào? Với `List<String> x = new ArrayList<>(); m(x);` chọn cái nào? Giải thích bằng khái niệm "kiểu tĩnh của đối số" và "most specific".

**Bài 13.** Vì sao `new Outer.Inner()` sai với inner class không `static`, mà phải `outer.new Inner()`? Chuyển `Inner` thành `static` thì cú pháp nào đúng? Liên hệ tới việc inner class giữ tham chiếu ngầm tới instance `Outer`.

**Bài 14.** Đoạn sau: `getTags()` trả thẳng field `List`. Viết một `main` "phá" đóng gói qua getter đó, rồi sửa `getTags()` để an toàn. Nêu 2 cách sửa và đánh đổi của mỗi cách.

**Bài 15.** Java 22 nới lỏng "câu lệnh đầu tiên phải là `this()/super()`". Trước Java 22, viết lại đoạn sau cho hợp lệ (gợi ý: dùng `private static` helper để tính tham số trước khi gọi `this(...)`):
```java
public Temperature(String raw) {
    double c = Double.parseDouble(raw.replace("C", "").trim());
    if (c < -273.15) throw new IllegalArgumentException();
    this(c);   // ❌ trước Java 22: this() phải đứng đầu
}
```

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Phần A</summary>

1. **Lỗi compile** tại `count++` — `count` là instance field, static method `increment()` không có object hiện tại. Sửa: bỏ `static`, hoặc `static int count;`.
2. **Không hợp lệ** — đã có constructor `(String,double)` nên compiler không sinh default constructor; `new Product()` → "constructor Product() is undefined".
3. `A B C B C` — static block 1 lần khi nạp class (trước `main` body chạy tới `new`); mỗi `new` chạy instance block rồi constructor.
4. **Không lỗi** — khác *thứ tự kiểu* tham số (`int,double` vs `double,int`) → hai signature khác nhau → overloading hợp lệ.
5. `balance = balance;` gán tham số cho chính nó; field `this.balance` vẫn `0.0`. Sửa: `this.balance = balance;`.
6. In `long`. Pha 1 cho phép widening `int → long` → `f(long)` khớp ngay, thắng `f(Integer)` (cần boxing, pha 2) và `f(Object)`.
7. Không dòng nào **lỗi compile**. `(1)` và `(3)` giống nhau (truy cập `A.k`), `(2)` cũng compile (truy cập static `v`? — không, `v` là instance) → thực ra `(2) a.v` compile OK về cú pháp nhưng **NPE lúc chạy** vì `a == null` và `v` là instance field. `(1) a.k` **không** NPE (chỉ dùng kiểu `A`). Điểm học: truy cập static qua ref chỉ nhìn *kiểu*, truy cập instance qua ref cần *object thật*.
8. In `outer` rồi `outer`. Anonymous class: `tag` không bị che nên nhìn thấy `P.this.tag` = `"outer"`. Lambda: `this` là `P` → `this.tag` = `"outer"`.
9. **Lỗi compile** ở constructor `Circle()` — blank final `r` không được gán trên mọi constructor. Sửa: `this(1.0);` hoặc gán `this.r = ...`.
10. `Sb Ss Ib Cb Is Cs` rồi (dòng 2) `Ib Cb Is Cs`. Static của cả cha lẫn con chỉ chạy lần đầu; mỗi `new` chạy: (super) instance-block cha + ctor cha, rồi instance-block con + ctor con.
11. **Không** — `h(null)` ambiguous giữa `String` và `Integer` (không cái nào cụ thể hơn). Sửa: `h((String) null)` hoặc `h((Integer) null)`.
12. **Không** — `NAME` là `static final` gán bằng hằng compile-time nên được inline; truy cập nó không kích hoạt khởi tạo class → "init Config" **không** in.

</details>

<details>
<summary>Phần B — ý chính</summary>

- **Bài 1:** `accountNumber` là `final`, gán trong constructor đầy đủ; constructor `(ownerName)` gọi `this("ACC" + (++totalAccounts), 0, ownerName)`. `withdraw`: `if (amount <= 0) throw ...; if (amount > balance) throw ...; balance -= amount;`.
- **Bài 2:** `Rectangle()` → `this(1)`; `Rectangle(double s)` → `this(s, s)`; `Rectangle(double w,double h)` gán field. Copy: `this(other.width, other.height)`.
- **Bài 3:** `companyBonusRate` static → 1 bản; sau `updateBonusRate(0.15)`, mọi `Employee` gọi `calculateFinalSalary()` đều dùng 0.15 (không object nào giữ bản sao riêng).
- **Bài 4:** Constructor: `this.setName(name); this.setAge(age); this.setEmail(email);`. `email`: `int at = email.indexOf('@'); if (at < 1 || email.indexOf('.', at) < 0) throw ...`.
- **Bài 5:** `private StringUtils() { throw new AssertionError(); }`, mọi method `public static`, class `public final`.
- **Bài 6:** `describe(1)` → `int`; `describe(1L)` → `long`; `describe(Integer.valueOf(1))` → `Integer`; `describe("x")` → `Object`; `describe(1,2,3)` → `int...`; `describe()` → `int...` (mảng rỗng). Lưu ý `describe(1)` **không** chọn varargs vì pha 1 đã khớp.
- **Bài 7:** compact ctor: `if (currency == null || !currency.matches("[A-Z]{3}")) throw ...; if (amountCents < 0) throw ...;`. `plus`: kiểm tra cùng currency. `equals` do record sinh sẵn → `true`.
- **Bài 8:** `static Animal`, `static Dog`, `instance Animal`, `ctor Animal`, `instance Dog`, `ctor Dog`, `----`, rồi `instance Animal`, `ctor Animal`, `instance Dog`, `ctor Dog`.

</details>

<details>
<summary>Phần C</summary>

- **Bài 9:** static method **không** đa hình — phân giải theo **kiểu tĩnh** `Base b` → `Base.who()`. Đây là **hiding**, không phải overriding. (IDE thường cảnh báo "static method called via instance reference".)
- **Bài 10:**
  ```java
  class Base { Base() { init(); } void init() { } }
  class Sub extends Base {
      String name = "set-in-field-init";
      @Override void init() { System.out.println("name = " + name); }  // in "name = null"
  }
  new Sub();
  ```
  Khi `super()` (ctor Base) gọi `init()`, phần khởi tạo field của `Sub` (§8 bước 2b) **chưa chạy** → `name` vẫn `null`. Phòng tránh: constructor chỉ gọi `private`/`final`/`static` method.
- **Bài 11:** Lần đầu: `ExceptionInInitializerError` (bọc RuntimeException gốc). Lần hai: `NoClassDefFoundError` ("Could not initialize class ...") — JVM đã đánh dấu class "erroneous" và không thử khởi tạo lại, nên không có stack trace của lỗi gốc.
- **Bài 12:** `m(new ArrayList<>())` → `m(ArrayList<String>)` (most specific, `ArrayList` là con của `List`). `m(x)` với `x` khai báo `List<String>` → `m(List<String>)` (chọn theo **kiểu tĩnh** của biến `x`, không theo object thật).
- **Bài 13:** inner class không `static` cần một instance `Outer` để tồn tại (nó giữ tham chiếu ngầm `Outer.this`) → cú pháp `outerRef.new Inner()`. Khi `Inner` là `static`, nó không cần `Outer` instance → `new Outer.Inner()`.
- **Bài 14:** "Phá": `obj.getTags().clear();`. Sửa (1) `return List.copyOf(tags);` — bất biến, nhưng tạo bản sao mỗi lần gọi. Sửa (2) `return Collections.unmodifiableList(tags);` — không copy, nhưng là *view*, caller thấy thay đổi nội bộ sau này và vẫn share cấu trúc.
- **Bài 15:**
  ```java
  public Temperature(String raw) { this(parse(raw)); }
  private static double parse(String raw) {
      double c = Double.parseDouble(raw.replace("C", "").trim());
      if (c < -273.15) throw new IllegalArgumentException();
      return c;
  }
  ```

</details>

---

*File tiếp theo trong lộ trình: **Module 01.4 — 4 trụ cột OOP** (Encapsulation, Inheritance, Polymorphism, Abstraction).*
