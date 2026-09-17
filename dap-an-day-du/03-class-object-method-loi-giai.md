# Lời giải đầy đủ — Module 01.3: Class, Object, Method

> Nguồn đề: `03 class object method/03-class-object-method.md` (Phần B — Bài tập viết code). Chỉ làm Phần B (không làm Phần A trắc nghiệm và Phần C nâng cao lý thuyết).

---

## Bài 1 — `BankAccount` hoàn chỉnh

### Đề
Field `private`: `accountNumber` (String, **`final`**), `balance` (double), `ownerName` (String). Hai constructor: đủ 3 tham số; và chỉ `ownerName` (balance = 0, `accountNumber` tự sinh `"ACC" + (++totalAccounts)` bằng static counter). `deposit`/`withdraw` validate số âm và rút quá số dư (`IllegalArgumentException`). Getter cho cả 3, **không** setter cho `accountNumber`. Static `totalAccounts` + static `printTotal()`. Dùng `this(...)` chaining.

### Phân tích

Đây là bài tổng hợp 3 khái niệm cốt lõi của module: **`final` field**, **static counter dùng chung giữa mọi instance**, và **constructor chaining bằng `this(...)`**.

- `accountNumber` là `final` → phải được gán **đúng 1 lần**, hoặc trong khai báo, hoặc trong **mọi** constructor (không được để trống ở bất kỳ nhánh nào). Vì giá trị này phụ thuộc vào constructor nào được gọi (constructor đủ 3 tham số nhận `accountNumber` từ tham số; constructor rút gọn tự sinh), cách gọn nhất là **constructor rút gọn gọi constructor đủ tham số qua `this(...)`**, truyền `accountNumber` tự sinh vào.
- `totalAccounts` là `static` → chỉ có **1 bản duy nhất** dùng chung cho mọi `BankAccount`, tăng lên mỗi lần có accoutn mới được tạo. Đặt `++totalAccounts` ngay tại nơi sinh `accountNumber` để đảm bảo số thứ tự khớp với số tài khoản đã tạo.
- Không có setter cho `accountNumber` — đây là cách thể hiện "immutable field" ở tầng API công khai, đúng tinh thần Encapsulation: chỉ class tự quyết định giá trị này, không cho bên ngoài áp đặt.

### Lời giải

```java
package baitap.bai1;

public class BankAccount {

    private final String accountNumber;
    private double balance;
    private String ownerName;

    private static int totalAccounts = 0;

    // Constructor đủ 3 tham số — nơi DUY NHẤT thực sự gán field
    public BankAccount(String accountNumber, double balance, String ownerName) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.ownerName = ownerName;
        totalAccounts++;
    }

    // Constructor rút gọn — tự sinh accountNumber, balance = 0, rồi CHAIN sang constructor trên
    public BankAccount(String ownerName) {
        this("ACC" + (totalAccounts + 1), 0, ownerName);
        // Lưu ý: totalAccounts++ đã nằm trong constructor kia (được this(...) gọi),
        // nên KHÔNG cộng lại ở đây để tránh đếm 2 lần.
    }

    public void deposit(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Số tiền nạp không được âm: " + amount);
        }
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Số tiền rút không được âm: " + amount);
        }
        if (amount > balance) {
            throw new IllegalArgumentException(
                "Số dư không đủ: có " + balance + ", cần rút " + amount);
        }
        balance -= amount;
    }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public String getOwnerName() { return ownerName; }
    // KHÔNG có setAccountNumber() - field final + không setter = bất biến với bên ngoài

    public static void printTotal() {
        System.out.println("Tổng số tài khoản đã tạo: " + totalAccounts);
    }

    public static void main(String[] args) {
        BankAccount a1 = new BankAccount("ACC-VIP-001", 500_000, "Pho");
        BankAccount a2 = new BankAccount("Huynh"); // tự sinh accountNumber

        System.out.println(a1.getAccountNumber() + " - " + a1.getOwnerName() + " - " + a1.getBalance());
        System.out.println(a2.getAccountNumber() + " - " + a2.getOwnerName() + " - " + a2.getBalance());

        a2.deposit(200_000);
        a2.withdraw(50_000);
        System.out.println("Số dư a2 sau giao dịch: " + a2.getBalance());

        try {
            a2.withdraw(1_000_000);
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được: " + e.getMessage());
        }

        BankAccount.printTotal();
    }
}
```

**Kết quả chạy:**
```
ACC-VIP-001 - Pho - 500000.0
ACC2 - Huynh - 0.0
Số dư a2 sau giao dịch: 150000.0
Lỗi bắt được: Số dư không đủ: có 150000.0, cần rút 1000000.0
Tổng số tài khoản đã tạo: 2
```

### Giải thích

- **Vì sao `accountNumber` sinh ra là `"ACC2"` chứ không phải `"ACC1"`:** `a1` được tạo bằng constructor đủ 3 tham số (truyền thẳng `"ACC-VIP-001"`), nhưng constructor này **vẫn** chạy `totalAccounts++` → sau khi tạo `a1`, `totalAccounts = 1`. Khi tạo `a2` bằng constructor rút gọn, biểu thức `totalAccounts + 1` được tính **trước khi** `this(...)` chạy xong (Java đánh giá tham số truyền cho `this(...)` trước), lúc đó `totalAccounts` vẫn đang là `1` → sinh ra `"ACC2"`. Đây là điểm dễ gây bug nếu không để ý thứ tự: **không được** cộng `totalAccounts` ở cả 2 constructor, vì `this(...)` đã kích hoạt constructor kia chạy đủ 3 tham số rồi.
- **Vì sao `this(...)` phải là dòng ĐẦU TIÊN:** Java bắt buộc lời gọi `this(...)` (hoặc `super(...)`) nếu có, phải là câu lệnh đầu tiên trong constructor — đảm bảo field được khởi tạo đúng 1 lần theo đúng trình tự, tránh field bị đọc trước khi gán.
- **`IllegalArgumentException`** là unchecked exception (đã học ở Module 04 sau này) — phù hợp cho lỗi do **client gọi sai tham số** (âm, vượt số dư), không cần khai báo `throws`.

---

## Bài 2 — `Rectangle` với `this(...)`

### Đề
`Rectangle()` → vuông cạnh 1; `Rectangle(double side)` → vuông; `Rectangle(double w, double h)` → chữ nhật. Constructor ngắn gọi constructor dài qua `this(...)` (không lặp code gán field). Thêm `area()`, `perimeter()`, và `Rectangle(Rectangle other)` (copy constructor).

### Phân tích

Đây là bài luyện **constructor chaining theo chuỗi 3 cấp**: constructor không tham số gọi constructor 1 tham số, constructor 1 tham số gọi constructor 2 tham số — chỉ có **constructor 2 tham số** (`w, h`) là nơi thực sự gán field `width`/`height`. Cách tổ chức này tránh lặp code, và nếu sau này cần thêm validate (VD: cạnh phải dương), chỉ cần sửa **đúng 1 chỗ**.

`Rectangle(Rectangle other)` là **copy constructor** — tạo object mới có cùng giá trị field với object `other`, không tham chiếu chung (khác với chỉ gán `Rectangle r2 = r1;` — đó chỉ là copy tham chiếu, sửa `r2` sẽ ảnh hưởng `r1`).

### Lời giải

```java
package baitap.bai2;

public class Rectangle {

    private double width;
    private double height;

    // Cấp 3 (gốc) - nơi DUY NHẤT thực sự gán field
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    // Cấp 2 - vuông cạnh "side" -> chain sang cấp 3
    public Rectangle(double side) {
        this(side, side);
    }

    // Cấp 1 - vuông cạnh 1 -> chain sang cấp 2
    public Rectangle() {
        this(1);
    }

    // Copy constructor - tạo object MỚI, độc lập với "other"
    public Rectangle(Rectangle other) {
        this(other.width, other.height);
    }

    public double area() {
        return width * height;
    }

    public double perimeter() {
        return 2 * (width + height);
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public static void main(String[] args) {
        Rectangle r1 = new Rectangle();          // vuông cạnh 1
        Rectangle r2 = new Rectangle(4);          // vuông cạnh 4
        Rectangle r3 = new Rectangle(3, 5);       // chữ nhật 3x5
        Rectangle r4 = new Rectangle(r3);         // copy của r3

        System.out.printf("r1: %.1fx%.1f area=%.1f perimeter=%.1f%n", r1.getWidth(), r1.getHeight(), r1.area(), r1.perimeter());
        System.out.printf("r2: %.1fx%.1f area=%.1f perimeter=%.1f%n", r2.getWidth(), r2.getHeight(), r2.area(), r2.perimeter());
        System.out.printf("r3: %.1fx%.1f area=%.1f perimeter=%.1f%n", r3.getWidth(), r3.getHeight(), r3.area(), r3.perimeter());
        System.out.printf("r4: %.1fx%.1f area=%.1f perimeter=%.1f%n", r4.getWidth(), r4.getHeight(), r4.area(), r4.perimeter());

        System.out.println("r3 va r4 la cung 1 object? " + (r3 == r4)); // false - object khac nhau
    }
}
```

**Kết quả chạy:**
```
r1: 1.0x1.0 area=1.0 perimeter=4.0
r2: 4.0x4.0 area=16.0 perimeter=16.0
r3: 3.0x5.0 area=15.0 perimeter=16.0
r4: 3.0x5.0 area=15.0 perimeter=16.0
r3 va r4 la cung 1 object? false
```

### Giải thích

- **Chuỗi `this(...)` chạy theo thứ tự NGƯỢC với khai báo:** gọi `new Rectangle()` → chạy `this(1)` → chạy `this(1, 1)` → mới thực sự gán field. Field chỉ được gán đúng **1 lần**, ở tầng sâu nhất.
- **`r3 == r4` là `false`:** dù `r4` được tạo từ `r3` qua copy constructor, chúng là **2 object khác nhau trên heap**, chỉ có giá trị field giống nhau. Đây là điểm phân biệt copy constructor với việc gán tham chiếu (`Rectangle r4 = r3;`, khi đó `r3 == r4` sẽ là `true`).
- Nếu sau này muốn thêm validate "cạnh phải dương", chỉ cần sửa **1 chỗ duy nhất** — constructor `(double width, double height)` — toàn bộ các constructor còn lại tự động được bảo vệ nhờ đi qua `this(...)`.

---

## Bài 3 — static vs instance qua bài toán lương

### Đề
`Employee`: instance `name`, `baseSalary`; static `companyBonusRate`. `double calculateFinalSalary()` dùng cả hai. `static void updateBonusRate(double r)`. Trong `main`: tạo 3 nhân viên, in lương, gọi `updateBonusRate`, in lương lại — chứng minh **cả 3** đều đổi vì cùng đọc một static field.

### Phân tích

Trọng tâm bài này: **static field là DÙNG CHUNG**, không phải "mỗi object có 1 bản riêng" như instance field. `name`/`baseSalary` là instance field — mỗi `Employee` có giá trị riêng. `companyBonusRate` là static field — chỉ tồn tại **1 bản duy nhất** ở cấp class, mọi instance đọc **cùng 1 giá trị** đó.

`calculateFinalSalary()` là instance method (đọc `baseSalary` — cần biết object cụ thể nào), nhưng vẫn truy cập được `companyBonusRate` (static) bình thường — vì static field/method **luôn truy cập được từ instance method**, chiều ngược lại (instance method gọi từ static context) mới không được phép.

### Lời giải

```java
package baitap.bai3;

public class Employee {

    private String name;
    private double baseSalary;

    private static double companyBonusRate = 0.10; // 10% mặc định, DÙNG CHUNG cho mọi Employee

    public Employee(String name, double baseSalary) {
        this.name = name;
        this.baseSalary = baseSalary;
    }

    public double calculateFinalSalary() {
        // Đọc field INSTANCE (baseSalary - riêng của object này)
        // và field STATIC (companyBonusRate - dùng chung mọi object)
        return baseSalary + baseSalary * companyBonusRate;
    }

    public static void updateBonusRate(double newRate) {
        companyBonusRate = newRate; // Đổi field static -> ẢNH HƯỞNG MỌI Employee cùng lúc
    }

    public String getName() { return name; }

    public static void main(String[] args) {
        Employee e1 = new Employee("Pho", 20_000_000);
        Employee e2 = new Employee("Huynh", 25_000_000);
        Employee e3 = new Employee("Gia", 30_000_000);

        System.out.println("--- Trước khi đổi bonus rate (10%) ---");
        for (Employee e : new Employee[]{e1, e2, e3}) {
            System.out.printf("%s: %.0f%n", e.getName(), e.calculateFinalSalary());
        }

        Employee.updateBonusRate(0.20); // Chỉ gọi 1 LẦN, qua class - không qua instance nào cả

        System.out.println("--- Sau khi đổi bonus rate (20%) ---");
        for (Employee e : new Employee[]{e1, e2, e3}) {
            System.out.printf("%s: %.0f%n", e.getName(), e.calculateFinalSalary());
        }
    }
}
```

**Kết quả chạy:**
```
--- Trước khi đổi bonus rate (10%) ---
Pho: 22000000
Huynh: 27500000
Gia: 33000000
--- Sau khi đổi bonus rate (20%) ---
Pho: 24000000
Huynh: 30000000
Gia: 36000000
```

### Giải thích

- Chỉ gọi `Employee.updateBonusRate(0.20)` **đúng 1 lần**, không gọi riêng cho `e1`, `e2`, `e3` — nhưng lương của **CẢ 3** đều tăng theo tỷ lệ mới. Điều này chứng minh `companyBonusRate` không nằm "bên trong" từng object, mà nằm ở **cấp class**, và mọi object chia sẻ **cùng 1 ô nhớ** đó.
- Ngược lại, nếu đổi `e1.baseSalary` (giả sử có setter), chỉ `e1` bị ảnh hưởng — vì đó là instance field, mỗi object có bản riêng trên heap.
- Thực tế nên gọi static method qua **tên class** (`Employee.updateBonusRate(...)`) thay vì qua instance (`e1.updateBonusRate(...)`, dù Java cho phép) — gọi qua instance dễ gây hiểu lầm rằng nó chỉ ảnh hưởng riêng `e1`.

---

## Bài 4 — Encapsulation + validate qua setter

### Đề
`Student` field `private`: `name`, `age`, `email`. Setter validate: `name` không rỗng/không toàn khoảng trắng; `age` 16..100; `email` chứa `@` và có `.` sau `@`. Constructor gọi lại setter để dùng chung validate. `main` thử giá trị hợp lệ và không hợp lệ, in message exception.

### Phân tích

Điểm quan trọng nhất: **constructor gọi setter thay vì gán field trực tiếp** — nhờ vậy, logic validate chỉ viết **1 lần** trong setter, constructor tự động được bảo vệ mà không cần copy-paste lại điều kiện kiểm tra.

Validate `email` cần đảm bảo có `@` VÀ có `.` xuất hiện **sau** vị trí `@` (không chỉ đơn thuần "có dấu chấm ở đâu đó") — dùng `indexOf('@')` rồi so sánh với `indexOf('.', vị trí sau @)`.

### Lời giải

```java
package baitap.bai4;

public class Student {

    private String name;
    private int age;
    private String email;

    public Student(String name, int age, String email) {
        // Constructor gọi LẠI setter -> dùng chung logic validate, không lặp code
        setName(name);
        setAge(age);
        setEmail(email);
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên không được để trống");
        }
        this.name = name;
    }

    public void setAge(int age) {
        if (age < 16 || age > 100) {
            throw new IllegalArgumentException("Tuổi phải từ 16 đến 100, nhận được: " + age);
        }
        this.age = age;
    }

    public void setEmail(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        boolean hasDotAfterAt = atIndex >= 0 && email.indexOf('.', atIndex) > atIndex;
        if (atIndex < 0 || !hasDotAfterAt) {
            throw new IllegalArgumentException("Email không hợp lệ: " + email);
        }
        this.email = email;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }

    public static void main(String[] args) {
        // Trường hợp hợp lệ
        Student s1 = new Student("Pho", 22, "pho@example.com");
        System.out.println("Tạo thành công: " + s1.getName() + ", " + s1.getAge() + ", " + s1.getEmail());

        // Các trường hợp không hợp lệ - từng cái
        tryCreate("  ", 20, "a@b.com");        // tên trống/toàn khoảng trắng
        tryCreate("Huynh", 15, "a@b.com");     // tuổi quá nhỏ
        tryCreate("Huynh", 101, "a@b.com");    // tuổi quá lớn
        tryCreate("Huynh", 20, "khong-co-at"); // thiếu @
        tryCreate("Huynh", 20, "a@bcom");      // có @ nhưng không có . sau đó
    }

    private static void tryCreate(String name, int age, String email) {
        try {
            new Student(name, age, email);
            System.out.println("Tạo thành công (không mong đợi!): " + name);
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được: " + e.getMessage());
        }
    }
}
```

**Kết quả chạy:**
```
Tạo thành công: Pho, 22, pho@example.com
Lỗi bắt được: Tên không được để trống
Lỗi bắt được: Tuổi phải từ 16 đến 100, nhận được: 15
Lỗi bắt được: Tuổi phải từ 16 đến 100, nhận được: 101
Lỗi bắt được: Email không hợp lệ: khong-co-at
Lỗi bắt được: Email không hợp lệ: a@bcom
```

### Giải thích

- **Vì sao "constructor gọi setter" tốt hơn "constructor tự validate riêng"?** Nếu copy-paste điều kiện validate vào cả constructor lẫn setter, sau này sửa quy tắc (VD: tuổi tối thiểu đổi từ 16 lên 18) rất dễ **quên sửa 1 trong 2 chỗ**, gây bug logic không nhất quán. Gọi setter từ constructor đảm bảo **chỉ có 1 nguồn sự thật (single source of truth)** cho logic validate.
- **Lưu ý khi dùng setter trong constructor với field không được validate hết:** nếu 1 setter throw exception, các field **sau đó chưa kịp gán** — nhưng vì Java sẽ ném exception ngay, object coi như "chưa tạo xong", không có object nào lọt ra ngoài ở trạng thái dở dang.
- **`isBlank()`** (Java 11+) kiểm tra cả rỗng lẫn "toàn khoảng trắng" trong 1 lần gọi, gọn hơn `name.trim().isEmpty()`.

---

## Bài 5 — Utility class đúng chuẩn

### Đề
`StringUtils` chỉ có static method: `isBlank(String)`, `reverse(String)`, `capitalize(String)`, `repeat(String, int)`. Constructor `private` ném `AssertionError`. Class `final`. Viết `main` gọi thử vài method.

### Phân tích

**Utility class** (như `java.util.Collections`, `java.util.Arrays`) là class chỉ chứa static method, không bao giờ cần tạo instance. 2 kỹ thuật chuẩn để "khóa" việc lỡ tạo instance:

1. `final class` — không cho class khác kế thừa (kế thừa 1 utility class không có ý nghĩa gì).
2. `private` constructor ném `AssertionError` — nếu ai đó (kể cả code **trong chính class**, qua reflection) cố `new StringUtils()`, sẽ bị chặn ngay ở constructor. `private` đã đủ ngăn code **bên ngoài class** gọi `new StringUtils()` (lỗi compile), nhưng vẫn nên ném `AssertionError` để phòng trường hợp gọi từ chính bên trong class hoặc qua Reflection API (`Constructor.newInstance()` với `setAccessible(true)` có thể bỏ qua `private`).

### Lời giải

```java
package baitap.bai5;

public final class StringUtils {

    private StringUtils() {
        throw new AssertionError("Không được khởi tạo StringUtils - class chỉ chứa static method");
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String reverse(String s) {
        if (s == null) return null;
        return new StringBuilder(s).reverse().toString();
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String repeat(String s, int times) {
        if (s == null) return null;
        if (times < 0) throw new IllegalArgumentException("times không được âm: " + times);
        return s.repeat(times); // String.repeat() có sẵn từ Java 11 - tận dụng thay vì tự viết vòng lặp
    }

    public static void main(String[] args) {
        System.out.println("isBlank(\"\") = " + isBlank(""));
        System.out.println("isBlank(\"  \") = " + isBlank("  "));
        System.out.println("isBlank(\"a\") = " + isBlank("a"));
        System.out.println("reverse(\"hello\") = " + reverse("hello"));
        System.out.println("capitalize(\"pho\") = " + capitalize("pho"));
        System.out.println("repeat(\"ab\", 3) = " + repeat("ab", 3));

        try {
            java.lang.reflect.Constructor<StringUtils> ctor = StringUtils.class.getDeclaredConstructor();
            ctor.setAccessible(true);   // vượt qua "private" bằng Reflection
            ctor.newInstance();          // vẫn bị chặn nhờ AssertionError trong constructor
        } catch (Exception e) {
            System.out.println("Không thể khởi tạo qua Reflection: " + e.getCause());
        }
    }
}
```

**Kết quả chạy:**
```
isBlank("") = true
isBlank("  ") = true
isBlank("a") = false
reverse("hello") = olleh
capitalize("pho") = Pho
repeat("ab", 3) = ababab
Không thể khởi tạo qua Reflection: java.lang.AssertionError: Không được khởi tạo StringUtils - class chỉ chứa static method
```

### Giải thích

- **Chỉ `private` constructor không đủ nếu muốn "khóa cứng":** `private` chặn được code viết tay ở class khác (`new StringUtils()` ngoài class → lỗi compile), nhưng Reflection API có thể `setAccessible(true)` để **vượt qua** kiểm tra `private` lúc runtime. Ném `AssertionError` ngay trong thân constructor là lớp phòng thủ **thứ hai**, hoạt động bất kể constructor được gọi bằng cách nào.
- **`final class`** ngăn 1 class con kế thừa `StringUtils` rồi vô tình (hoặc cố ý) expose 1 constructor `public` mới ở class con đó — nếu `StringUtils` không `final`, quy tắc "không tạo được instance" có thể bị lách qua đường kế thừa.
- Đây chính xác là mẫu hình `java.lang.Math`, `java.util.Collections` áp dụng trong JDK thật.

---

## Bài 6 — Overload resolution (dự đoán rồi kiểm chứng)

### Đề
Viết class có các overload `describe(int)`, `describe(long)`, `describe(Integer)`, `describe(Object)`, `describe(int...)`. Gọi lần lượt với: `describe(1)`, `describe(1L)`, `describe(Integer.valueOf(1))`, `describe("x")`, `describe(1, 2, 3)`, `describe()`. **Dự đoán** output từng dòng theo 3 pha resolution, rồi chạy để đối chiếu.

### Phân tích

Java chọn overload theo đúng **3 pha tuần tự**, dừng ngay khi pha nào tìm được method áp dụng được:

```
Pha 1 (Strict invocation)     — chỉ cho phép widening primitive/reference, KHÔNG boxing, KHÔNG varargs
Pha 2 (Loose invocation)      — cho phép thêm autoboxing/unboxing, KHÔNG varargs
Pha 3 (Variable arity)        — cho phép cả varargs (int...)
```

Compiler thử hết các overload ở Pha 1 trước; nếu có đúng 1 (hoặc 1 "cụ thể nhất") ứng viên khớp → chọn luôn, KHÔNG xét tới Pha 2/3.

**Dự đoán từng dòng:**

| Lời gọi | Overload khớp | Pha | Lý do |
|---|---|---|---|
| `describe(1)` | `describe(int)` | 1 | `1` là literal `int`, khớp thẳng không cần convert gì |
| `describe(1L)` | `describe(long)` | 1 | `1L` là literal `long`, khớp thẳng `describe(long)` |
| `describe(Integer.valueOf(1))` | `describe(Integer)` | 1 | Đối số đã là kiểu tham chiếu `Integer` — khớp thẳng, không cần unboxing |
| `describe("x")` | `describe(Object)` | 1 | `String` không khớp `int`/`long`/`Integer`; nhưng `String` **là-một** `Object` (widening reference) — khớp `describe(Object)` ngay ở Pha 1 |
| `describe(1, 2, 3)` | `describe(int...)` | 3 | Không method cố định nào nhận 3 tham số `int` riêng lẻ → phải đợi tới Pha 3 (varargs) |
| `describe()` | `describe(int...)` | 3 | Không method nào nhận 0 tham số → varargs chấp nhận mảng rỗng, chỉ có ở Pha 3 |

### Lời giải

```java
package baitap.bai6;

public class Main {

    static void describe(int x) {
        System.out.println("describe(int): " + x);
    }

    static void describe(long x) {
        System.out.println("describe(long): " + x);
    }

    static void describe(Integer x) {
        System.out.println("describe(Integer): " + x);
    }

    static void describe(Object x) {
        System.out.println("describe(Object): " + x);
    }

    static void describe(int... x) {
        System.out.println("describe(int...): độ dài = " + x.length);
    }

    public static void main(String[] args) {
        describe(1);                       // int literal
        describe(1L);                      // long literal
        describe(Integer.valueOf(1));      // đã là Integer
        describe("x");                     // String -> Object
        describe(1, 2, 3);                 // 3 đối số -> chỉ varargs khớp
        describe();                        // 0 đối số -> chỉ varargs khớp
    }
}
```

**Kết quả chạy:**
```
describe(int): 1
describe(long): 1
describe(Integer): 1
describe(Object): x
describe(int...): độ dài = 3
describe(int...): độ dài = 0
```

### Giải thích

- **Điểm hay nhầm nhất:** `describe(Integer.valueOf(1))` gọi `describe(Integer)`, **không phải** `describe(int)` — dù `Integer` có thể tự unbox thành `int`. Vì `describe(Integer)` đã khớp **thẳng, không cần convert gì** ở Pha 1 (tham số truyền vào đã đúng kiểu `Integer`), compiler **không cần** xét tới việc unbox — luôn ưu tiên overload khớp "rẻ nhất" (ít phép chuyển đổi nhất) trong cùng 1 pha.
- **`describe("x")` chọn `Object` chứ không lỗi compile:** vì `String` không phải là `int`/`long`/`Integer` bằng bất kỳ phép convert nào hợp lệ ở Pha 1/2, nhưng `String extends Object` — mọi reference type đều "là-một" `Object` — nên `describe(Object)` là lựa chọn hợp lệ duy nhất.
- **Varargs luôn được xét SAU CÙNG:** đây là lý do `describe(1, 2, 3)` và `describe()` đều rơi vào `describe(int...)` — không có overload cố định nào (fixed-arity) khớp về **số lượng tham số**, buộc compiler phải mở khóa Pha 3.

---

## Bài 7 — `record` + compact constructor

### Đề
`record Money(long amountCents, String currency)`. Compact constructor: `currency` không null, đúng 3 ký tự in hoa; `amountCents` không âm. Thêm method `Money plus(Money other)` (chỉ cộng nếu cùng `currency`, nếu không ném `IllegalArgumentException`) và `String format()` trả `"12.34 USD"`. Chứng minh `new Money(1234,"USD").equals(new Money(1234,"USD"))` là `true`.

### Phân tích

`record` (Java 16+) tự động sinh constructor chuẩn, `equals()`/`hashCode()`/`toString()` dựa trên **toàn bộ component**, và accessor method (`amountCents()`, `currency()` — không có tiền tố `get`). **Compact constructor** (`public Money { ... }`, không có danh sách tham số lặp lại) là nơi chèn logic validate **trước khi** Java tự gán field — cú pháp gọn hơn hẳn so với constructor tường minh của class thường.

`format()` cần chuyển `amountCents` (số nguyên, đơn vị cent) sang dạng thập phân 2 chữ số (`amountCents / 100.0`), rồi ghép với `currency`.

### Lời giải

```java
package baitap.bai7;

public record Money(long amountCents, String currency) {

    // Compact constructor - validate TRƯỚC khi Java tự gán field
    public Money {
        if (currency == null) {
            throw new IllegalArgumentException("currency không được null");
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency phải đúng 3 ký tự in hoa, nhận: " + currency);
        }
        if (amountCents < 0) {
            throw new IllegalArgumentException("amountCents không được âm: " + amountCents);
        }
        // KHÔNG cần viết "this.amountCents = amountCents;" - Java tự làm sau compact constructor
    }

    public Money plus(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Không thể cộng 2 loại tiền khác nhau: " + this.currency + " và " + other.currency);
        }
        return new Money(this.amountCents + other.amountCents, this.currency);
    }

    public String format() {
        return String.format("%.2f %s", amountCents / 100.0, currency);
    }

    public static void main(String[] args) {
        Money a = new Money(1234, "USD"); // 12.34 USD
        Money b = new Money(766, "USD");  // 7.66 USD

        System.out.println("a.format() = " + a.format());
        System.out.println("a.plus(b).format() = " + a.plus(b).format());

        System.out.println("equals: " + new Money(1234, "USD").equals(new Money(1234, "USD")));

        try {
            a.plus(new Money(100, "VND"));
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được: " + e.getMessage());
        }

        try {
            new Money(-1, "USD");
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được: " + e.getMessage());
        }

        try {
            new Money(100, "usd"); // chữ thường - vi phạm [A-Z]{3}
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được: " + e.getMessage());
        }
    }
}
```

**Kết quả chạy:**
```
a.format() = 12.34 USD
a.plus(b).format() = 20.00 USD
equals: true
Lỗi bắt được: Không thể cộng 2 loại tiền khác nhau: USD và VND
Lỗi bắt được: amountCents không được âm: -1
Lỗi bắt được: currency phải đúng 3 ký tự in hoa, nhận: usd
```

### Giải thích

- **Vì sao `equals` trả `true` dù 2 object khác nhau trên heap:** `record` tự sinh `equals()` so sánh **giá trị TỪNG component** (`amountCents` và `currency`), không phải so sánh địa chỉ như `==`. Đây chính là lợi ích lớn nhất của `record` so với class thường — không cần tự viết `equals()`/`hashCode()` (đã học kỹ hơn ở Module 02.4).
- **Compact constructor không có `return` và không lặp lại tham số:** cú pháp `public Money { ... }` khác hẳn constructor tường minh `public Money(long amountCents, String currency) { ... }` — Java hiểu ngầm compact constructor nhận đúng các component đã khai báo ở header `record Money(long amountCents, String currency)`.
- **`plus()` trả về `Money` MỚI** (record là **immutable** — component không thể sửa sau khi tạo), không sửa `this` hay `other` — đúng tinh thần bất biến của `record`.

---

## Bài 8 — Thứ tự khởi tạo có kế thừa (dự đoán trước)

### Đề
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

### Phân tích

Đây là bài kinh điển kiểm tra hiểu biết về **thứ tự khởi tạo class + object khi có kế thừa**, theo đúng quy tắc JLS:

1. **Static block** chỉ chạy **ĐÚNG 1 LẦN**, khi class được JVM **load & initialize lần đầu tiên** (không phải mỗi lần `new`). Superclass luôn được initialize **trước** subclass (`Animal` trước `Dog`).
2. **Instance initializer block + constructor** chạy **MỖI LẦN** có `new` — cũng theo thứ tự superclass trước (`Animal` instance block → `Animal` constructor → `Dog` instance block → `Dog` constructor), vì constructor luôn ngầm gọi `super()` là dòng đầu tiên.

Lần `new Dog()` **thứ hai**: `Animal`/`Dog` đã được khởi tạo (initialize) từ lần đầu → static block **KHÔNG chạy lại**, chỉ instance block + constructor chạy lại.

### Dự đoán trước khi chạy

```
static Animal
static Dog
instance Animal
ctor Animal
instance Dog
ctor Dog
----
instance Animal
ctor Animal
instance Dog
ctor Dog
```

### Lời giải (chạy thật để đối chiếu)

```java
package baitap.bai8;

public class Main {

    static class Animal {
        static { System.out.println("static Animal"); }
        { System.out.println("instance Animal"); }
        Animal() { System.out.println("ctor Animal"); }
    }

    static class Dog extends Animal {
        static { System.out.println("static Dog"); }
        { System.out.println("instance Dog"); }
        Dog() { System.out.println("ctor Dog"); }
    }

    public static void main(String[] args) {
        new Dog();
        System.out.println("----");
        new Dog();
    }
}
```

**Kết quả chạy (khớp đúng dự đoán):**
```
static Animal
static Dog
instance Animal
ctor Animal
instance Dog
ctor Dog
----
instance Animal
ctor Animal
instance Dog
ctor Dog
```

### Giải thích từng dòng

| Dòng | Vì sao |
|---|---|
| `static Animal` | Lần đầu JVM cần dùng `Dog`, nó phải load+initialize `Animal` (superclass) trước → static block của `Animal` chạy |
| `static Dog` | Sau khi `Animal` initialize xong, JVM initialize tiếp `Dog` → static block của `Dog` chạy |
| `instance Animal` | Bắt đầu `new Dog()` thật sự: constructor `Dog()` ngầm gọi `super()` đầu tiên → vào `Animal()`, trước khi chạy thân `Animal()`, Java chạy **instance initializer block** của `Animal` |
| `ctor Animal` | Sau instance block, chạy tới thân constructor `Animal()` |
| `instance Dog` | Quay lại `Dog()`: trước thân constructor, chạy **instance initializer block** của `Dog` |
| `ctor Dog` | Cuối cùng chạy thân constructor `Dog()` — object `Dog` hoàn tất |
| `instance Animal` → `ctor Dog` (lần 2) | `new Dog()` lần 2: `Animal`/`Dog` **đã initialize xong từ lần trước** → static block **không lặp lại**; nhưng đây là **object mới**, nên toàn bộ chuỗi instance block + constructor **chạy lại từ đầu** |

**Bài học cốt lõi:** "khởi tạo class" (static, 1 lần/class) và "khởi tạo object" (instance, mỗi lần `new`) là **2 khái niệm hoàn toàn tách biệt** — nhầm lẫn 2 khái niệm này là lỗi tư duy rất phổ biến khi mới học OOP, và cũng là nền tảng để hiểu sâu hơn các annotation như `@PostConstruct` của Spring (Module 12) sau này.

---

*Đây là lời giải cho toàn bộ Phần B của Module 03. Tiếp theo: Module 04 — 4 trụ cột OOP.*
