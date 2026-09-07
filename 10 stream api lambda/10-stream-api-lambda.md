# Module 03.3 — Stream API & Lambda

> **Mức độ ưu tiên: Trung bình → Cao trong thực tế** — Dù xếp "trung bình" trong lộ trình lý thuyết, đây là phong cách viết code **được dùng nhiều nhất** trong mọi codebase Spring Boot hiện đại. Không thành thạo Stream API sẽ khiến việc đọc code đồng nghiệp, hoặc code trong tài liệu Spring, trở nên chật vật — gần như mọi service xử lý danh sách dữ liệu ngày nay đều dùng Stream thay vì vòng lặp `for` truyền thống.

---

## Mục lục

1. [Functional Interface — nền tảng của Lambda](#1-functional-interface--nền-tảng-của-lambda)
2. [Lambda Expression](#2-lambda-expression)
3. [4 Functional Interface cốt lõi trong `java.util.function`](#3-4-functional-interface-cốt-lõi-trong-javautilfunction)
4. [Method Reference (`::`)](#4-method-reference-)
5. [Stream API — tổng quan pipeline](#5-stream-api--tổng-quan-pipeline)
6. [Các thao tác trung gian (Intermediate Operations)](#6-các-thao-tác-trung-gian-intermediate-operations)
7. [Các thao tác kết thúc (Terminal Operations)](#7-các-thao-tác-kết-thúc-terminal-operations)
8. [`Collectors` — thu thập kết quả](#8-collectors--thu-thập-kết-quả)
9. [Lazy Evaluation — Stream chỉ chạy khi có Terminal Operation](#9-lazy-evaluation--stream-chỉ-chạy-khi-có-terminal-operation)
10. [Khi nào NÊN và KHÔNG NÊN dùng Stream](#10-khi-nào-nên-và-không-nên-dùng-stream)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. Functional Interface — nền tảng của Lambda

**Functional Interface** là 1 interface chỉ có **đúng 1 method trừu tượng** (có thể có thêm default/static method — không tính, đã học ở Module 02.2).

```java
@FunctionalInterface // annotation KHÔNG bắt buộc, nhưng nên dùng — compiler sẽ báo lỗi nếu vô tình thêm method trừu tượng thứ 2
public interface Calculator {
    int calculate(int a, int b); // ĐÚNG 1 method trừu tượng
}
```

Vì chỉ có **duy nhất 1 method** cần cài đặt, Java cho phép dùng **Lambda Expression** để viết implementation cực kỳ gọn, thay vì phải tạo hẳn 1 class hoặc anonymous class dài dòng.

---

## 2. Lambda Expression

### So sánh: Anonymous Class (cách cũ) vs Lambda (cách mới, Java 8+)

```java
// Cách CŨ — Anonymous Class, dài dòng
Calculator addition = new Calculator() {
    @Override
    public int calculate(int a, int b) {
        return a + b;
    }
};

// Cách MỚI — Lambda Expression, ngắn gọn
Calculator addition2 = (a, b) -> a + b;
```

### Cú pháp Lambda

```java
(tham số) -> biểu thức_hoặc_khối_lệnh
```

```java
Calculator add = (a, b) -> a + b;                  // biểu thức đơn — tự động return
Calculator add2 = (int a, int b) -> { return a + b; }; // khối lệnh — cần {} và return tường minh

Runnable task = () -> System.out.println("Chạy");   // không tham số
Runnable task2 = () -> { System.out.println("A"); System.out.println("B"); }; // nhiều dòng lệnh

// Java thường suy luận được kiểu tham số (type inference) — có thể bỏ khai báo kiểu
Calculator add3 = (a, b) -> a + b; // không cần ghi (int a, int b)
```

### Lambda "bắt giữ" biến bên ngoài (Variable Capture)

```java
int discount = 10; // biến local

Calculator applyDiscount = (price, unused) -> price - discount; // Lambda "capture" biến discount từ scope bên ngoài
```

> ⚠️ **Ràng buộc quan trọng:** biến local bị "capture" bởi Lambda phải là **effectively final** — nghĩa là dù không bắt buộc ghi từ khóa `final`, biến đó **không được phép gán lại giá trị** sau khi khai báo, nếu không sẽ lỗi compile:
> ```java
> int counter = 0;
> Runnable r = () -> System.out.println(counter);
> counter = 5; // ❌ Lỗi compile: "Variable used in lambda expression should be final or effectively final"
> ```
> Lý do kỹ thuật: Lambda có thể chạy ở thời điểm khác, hoặc thread khác — Java cần đảm bảo giá trị biến capture không đổi để tránh tình trạng dữ liệu không nhất quán.

---

## 3. 4 Functional Interface cốt lõi trong `java.util.function`

JDK đã cung cấp sẵn rất nhiều functional interface thông dụng trong package `java.util.function`, không cần tự định nghĩa lại cho mọi trường hợp.

| Interface | Method trừu tượng | Ý nghĩa | Ví dụ |
|---|---|---|---|
| `Function<T, R>` | `R apply(T t)` | Nhận `T`, trả về `R` (biến đổi dữ liệu) | `Function<String, Integer> len = String::length;` |
| `Predicate<T>` | `boolean test(T t)` | Nhận `T`, trả về `boolean` (kiểm tra điều kiện) | `Predicate<Integer> isEven = n -> n % 2 == 0;` |
| `Supplier<T>` | `T get()` | Không nhận gì, trả về `T` (cung cấp giá trị) | `Supplier<String> greet = () -> "Hello";` |
| `Consumer<T>` | `void accept(T t)` | Nhận `T`, không trả về gì (tiêu thụ/xử lý dữ liệu) | `Consumer<String> print = System.out::println;` |

```java
Function<String, Integer> stringLength = s -> s.length();
System.out.println(stringLength.apply("Java")); // 4

Predicate<Integer> isPositive = n -> n > 0;
System.out.println(isPositive.test(-5)); // false

Supplier<Double> randomValue = () -> Math.random();
System.out.println(randomValue.get()); // giá trị ngẫu nhiên mỗi lần gọi

Consumer<String> logger = message -> System.out.println("[LOG] " + message);
logger.accept("Ứng dụng đã khởi động");
```

### Các biến thể hay gặp

```java
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b; // nhận 2 tham số
UnaryOperator<Integer> square = n -> n * n;                   // Function<T,T> — vào ra cùng kiểu
BinaryOperator<Integer> sum = (a, b) -> a + b;                // BiFunction<T,T,T> — vào ra cùng kiểu
```

### Kết hợp Predicate — `and()`, `or()`, `negate()`

```java
Predicate<Integer> isPositive = n -> n > 0;
Predicate<Integer> isEven = n -> n % 2 == 0;

Predicate<Integer> isPositiveAndEven = isPositive.and(isEven);
Predicate<Integer> isPositiveOrEven = isPositive.or(isEven);
Predicate<Integer> isNegative = isPositive.negate();

System.out.println(isPositiveAndEven.test(4));  // true
System.out.println(isPositiveAndEven.test(-4)); // false
```

---

## 4. Method Reference (`::`)

Khi Lambda **chỉ đơn giản gọi lại 1 method đã tồn tại**, có thể viết gọn hơn bằng **Method Reference**.

| Loại | Cú pháp Lambda | Method Reference tương đương |
|---|---|---|
| Static method | `n -> Integer.parseInt(n)` | `Integer::parseInt` |
| Instance method của object cụ thể | `s -> System.out.println(s)` | `System.out::println` |
| Instance method của kiểu tham số (bất kỳ object nào thuộc kiểu đó) | `s -> s.toUpperCase()` | `String::toUpperCase` |
| Constructor | `() -> new ArrayList<>()` | `ArrayList::new` |

```java
List<String> names = List.of("pho", "an", "binh");

names.forEach(name -> System.out.println(name)); // Lambda
names.forEach(System.out::println);              // Method Reference — gọn hơn, cùng ý nghĩa

List<String> upper = names.stream()
    .map(name -> name.toUpperCase())   // Lambda
    .toList();
List<String> upper2 = names.stream()
    .map(String::toUpperCase)          // Method Reference — "toUpperCase" gọi trên CHÍNH phần tử đang xử lý
    .toList();

Supplier<List<String>> listCreator = ArrayList::new; // Constructor reference
```

> **Nguyên tắc chọn:** khi Lambda có dạng `x -> x.method()` hoặc `x -> ClassName.method(x)` — tức là **không thêm logic gì khác** ngoài việc gọi lại 1 method có sẵn — nên chuyển sang Method Reference để code gọn và dễ đọc hơn.

---

## 5. Stream API — tổng quan pipeline

**Stream** là một chuỗi các phần tử hỗ trợ xử lý dữ liệu theo phong cách **functional programming** — không sửa đổi dữ liệu nguồn, mà tạo ra 1 luồng xử lý qua nhiều bước.

```
Nguồn dữ liệu (List, Set, Array...) 
      │
      ▼
  .stream()  ────►  Intermediate Operations (map, filter, sorted...) — CÓ THỂ nối tiếp nhiều bước, LAZY (chưa chạy ngay)
      │                        │
      │                        ▼
      └──────────────►  Terminal Operation (collect, forEach, reduce...) — KÍCH HOẠT toàn bộ pipeline chạy thực sự
```

```java
List<String> names = List.of("Pho", "An", "Binh", "Cuong", "Dung");

List<String> result = names.stream()               // (1) tạo Stream từ List
    .filter(name -> name.length() > 3)              // (2) Intermediate — giữ lại tên dài hơn 3 ký tự
    .map(String::toUpperCase)                        // (3) Intermediate — chuyển hoa toàn bộ
    .sorted()                                         // (4) Intermediate — sắp xếp alphabet
    .collect(Collectors.toList());                    // (5) Terminal — thu thập kết quả thành List

System.out.println(result); // [BINH, CUONG, DUNG]
```

> **So sánh trực quan với cách viết vòng lặp truyền thống:** đoạn Stream trên tương đương đúng 1 vòng `for` + `if` + `StringBuilder`/List trung gian + `Collections.sort()` — Stream giúp diễn đạt **ý định (intent)** rõ ràng hơn nhiều, đọc gần giống ngôn ngữ tự nhiên ("lọc, rồi biến đổi, rồi sắp xếp, rồi thu thập").

---

## 6. Các thao tác trung gian (Intermediate Operations)

Đặc điểm chung: trả về **1 Stream mới**, cho phép nối tiếp (`chaining`) nhiều bước, và **LAZY** (không thực thi ngay — xem mục 9).

```java
List<Integer> numbers = List.of(5, 3, 8, 1, 9, 2, 8);

numbers.stream().filter(n -> n > 3);        // filter — giữ lại phần tử thỏa điều kiện Predicate
numbers.stream().map(n -> n * 2);            // map — biến đổi mỗi phần tử theo Function
numbers.stream().sorted();                   // sorted — sắp xếp tăng dần (hoặc theo Comparator truyền vào)
numbers.stream().distinct();                 // distinct — loại bỏ trùng lặp (dựa trên equals())
numbers.stream().limit(3);                   // limit — chỉ lấy N phần tử đầu
numbers.stream().skip(2);                    // skip — bỏ qua N phần tử đầu
numbers.stream().peek(System.out::println);  // peek — "nhìn trộm" từng phần tử, dùng để DEBUG, không nên dùng để thay đổi trạng thái (side-effect)
```

### `flatMap` — làm phẳng cấu trúc lồng nhau (rất hay dùng, hay bị hỏi)

```java
List<List<String>> nestedList = List.of(
    List.of("Java", "Spring"),
    List.of("Python", "Django"),
    List.of("Go")
);

List<String> flatList = nestedList.stream()
    .flatMap(list -> list.stream()) // "làm phẳng" từ Stream<List<String>> thành Stream<String>
    .toList();

System.out.println(flatList); // [Java, Spring, Python, Django, Go]
```

> **Phân biệt `map` vs `flatMap`:** `map` biến đổi **1-thành-1** (mỗi phần tử vào cho ra đúng 1 phần tử ra); `flatMap` dùng khi mỗi phần tử vào tạo ra **1 Stream con**, rồi "làm phẳng" tất cả các Stream con đó thành **1 Stream duy nhất** — dùng phổ biến khi xử lý dữ liệu lồng nhau (danh sách của danh sách, hoặc 1 object chứa nhiều object con cần "trải phẳng" ra).

---

## 7. Các thao tác kết thúc (Terminal Operations)

Đặc điểm chung: **kích hoạt** toàn bộ pipeline chạy, trả về kết quả **không phải Stream** (giá trị đơn, Collection, hoặc `void`) — và **Stream chỉ dùng được 1 lần**, gọi Terminal Operation xong là Stream đó "hết hạn".

```java
List<Integer> numbers = List.of(5, 3, 8, 1, 9);

numbers.stream().forEach(System.out::println);      // forEach — duyệt qua từng phần tử, không trả về gì
long count = numbers.stream().filter(n -> n > 3).count(); // count — đếm số phần tử
Optional<Integer> max = numbers.stream().max(Integer::compareTo); // max/min — trả về Optional (an toàn khi Stream rỗng)
boolean anyMatch = numbers.stream().anyMatch(n -> n > 8);   // anyMatch — CÓ ít nhất 1 phần tử thỏa điều kiện?
boolean allMatch = numbers.stream().allMatch(n -> n > 0);   // allMatch — TẤT CẢ phần tử đều thỏa điều kiện?
boolean noneMatch = numbers.stream().noneMatch(n -> n < 0); // noneMatch — KHÔNG có phần tử nào thỏa điều kiện?

int sum = numbers.stream().reduce(0, Integer::sum); // reduce — "gộp" toàn bộ phần tử thành 1 giá trị duy nhất
```

### `reduce()` — đào sâu vì hay bị hỏi phỏng vấn

```java
int sum = numbers.stream().reduce(0, (a, b) -> a + b);
// 0 = giá trị khởi tạo (identity)
// (a, b) -> a + b = cách "gộp" 2 giá trị lại — a là kết quả tích lũy tạm thời, b là phần tử tiếp theo

// Diễn giải từng bước: 0+5=5, 5+3=8, 8+8=16, 16+1=17, 17+9=26
System.out.println(sum); // 26
```
`reduce()` chính là cách tổng quát hóa của `sum()`, `max()`, `min()` — hiểu được `reduce()` nghĩa là hiểu được ý tưởng nền tảng "gộp nhiều giá trị thành 1" theo phong cách functional.

---

## 8. `Collectors` — thu thập kết quả

`Collectors` là lớp tiện ích cung cấp các cách "thu thập" Stream thành cấu trúc dữ liệu cụ thể — dùng cùng với `collect()`.

```java
List<String> names = List.of("Pho", "An", "Binh", "Cuong");

// Thu thập thành List / Set
List<String> asList = names.stream().collect(Collectors.toList());
Set<String> asSet = names.stream().collect(Collectors.toSet());
// Java 16+ có cách viết gọn hơn cho toList(): names.stream().toList(); (bất biến - immutable)

// Nối chuỗi
String joined = names.stream().collect(Collectors.joining(", ")); // "Pho, An, Binh, Cuong"
String joinedWithBrackets = names.stream().collect(Collectors.joining(", ", "[", "]")); // "[Pho, An, Binh, Cuong]"

// Thu thập thành Map
Map<String, Integer> nameToLength = names.stream()
    .collect(Collectors.toMap(name -> name, name -> name.length()));
// {Pho=3, An=2, Binh=4, Cuong=5}

// GROUPING — cực kỳ hay dùng trong thực tế backend (báo cáo, thống kê)
record Employee(String name, String department, double salary) {}

List<Employee> employees = List.of(
    new Employee("Pho", "IT", 15_000_000),
    new Employee("An", "IT", 18_000_000),
    new Employee("Binh", "Sales", 12_000_000)
);

Map<String, List<Employee>> byDepartment = employees.stream()
    .collect(Collectors.groupingBy(Employee::department));
// {IT=[Pho, An], Sales=[Binh]}

Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(Employee::department, Collectors.counting()));
// {IT=2, Sales=1}

Map<String, Double> avgSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(Employee::department, Collectors.averagingDouble(Employee::salary)));
// {IT=16500000.0, Sales=12000000.0}

// PARTITIONING — chia thành ĐÚNG 2 nhóm dựa trên true/false
Map<Boolean, List<Employee>> partitioned = employees.stream()
    .collect(Collectors.partitioningBy(e -> e.salary() > 15_000_000));
// {false=[Binh], true=[Pho, An]}
```

> **Liên hệ trực tiếp thực tế:** `groupingBy` chính là cách viết Java tương đương với `GROUP BY` trong SQL (Module 10) — nếu đã quen SQL, `Collectors.groupingBy()` sẽ rất dễ tiếp thu vì cùng bản chất tư duy.

---

## 9. Lazy Evaluation — Stream chỉ chạy khi có Terminal Operation

Đây là đặc tính quan trọng, hay gây bất ngờ cho người mới:

```java
List<String> names = List.of("Pho", "An", "Binh");

Stream<String> stream = names.stream()
    .filter(name -> {
        System.out.println("Đang filter: " + name); // sẽ KHÔNG chạy ngay ở dòng này!
        return name.length() > 2;
    });

System.out.println("Đã tạo xong Stream, chưa có gì được in ra ở trên");

long count = stream.count(); // CHỈ ĐẾN ĐÂY, dòng "Đang filter..." mới thực sự được in ra
```

**Lý do:** các Intermediate Operation (`filter`, `map`...) chỉ **"ghi nhận" các bước xử lý** vào 1 pipeline, chưa thực sự chạy trên dữ liệu. Chỉ khi gặp **Terminal Operation** (`count()`, `collect()`, `forEach()`...), Stream mới thực sự **duyệt qua từng phần tử và áp dụng toàn bộ các bước đã ghi nhận, theo đúng thứ tự, cho từng phần tử một** (không phải chạy hết `filter` cho mọi phần tử rồi mới chạy `map` cho mọi phần tử — mà mỗi phần tử đi qua toàn bộ pipeline trước khi phần tử tiếp theo bắt đầu).

> **Hệ quả thực tế cần nhớ:** một Stream **chỉ dùng được đúng 1 lần**. Gọi Terminal Operation xong, nếu cố gọi lại `stream.count()` lần nữa sẽ ném `IllegalStateException: stream has already been operated upon or closed`. Muốn xử lý lại, phải tạo Stream mới từ nguồn dữ liệu (`list.stream()` lần nữa).

---

## 10. Khi nào NÊN và KHÔNG NÊN dùng Stream

| Nên dùng Stream khi | Nên dùng vòng lặp `for` truyền thống khi |
|---|---|
| Xử lý dữ liệu qua nhiều bước biến đổi (filter → map → collect) | Logic đơn giản, chỉ 1 bước, dùng Stream sẽ "làm màu" không cần thiết |
| Cần group/partition/thống kê dữ liệu | Cần `break`/`continue` phức tạp giữa chừng (Stream không hỗ trợ trực tiếp) |
| Ưu tiên tính dễ đọc, khai báo ý định rõ ràng (declarative style) | Cần hiệu năng tối đa tuyệt đối cho vòng lặp cực lớn, cực nhạy cảm hiệu năng (Stream có overhead nhỏ so với for-loop thuần) |
| Có thể tận dụng xử lý song song dễ dàng (`parallelStream()`) | Logic có nhiều side-effect (thay đổi biến bên ngoài) — không hợp phong cách functional |

> **Lưu ý về `parallelStream()`:** cho phép Stream tự động chia nhỏ và xử lý song song trên nhiều thread — nghe hấp dẫn nhưng **không nên dùng bừa bãi**. Chỉ hiệu quả với tập dữ liệu đủ lớn và logic xử lý không có side-effect; với dữ liệu nhỏ, overhead quản lý thread còn tốn hơn lợi ích, và nếu logic có side-effect (sửa biến ngoài, ghi log không đồng bộ...) rất dễ gây race condition (liên hệ Module 05 — Multithreading).

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| Functional Interface | Đúng 1 method trừu tượng — nền tảng để dùng Lambda |
| Lambda | `(params) -> expression`; biến capture phải effectively final |
| `Function/Predicate/Supplier/Consumer` | 4 functional interface cốt lõi trong `java.util.function` |
| Method Reference | `Class::method` — dùng khi Lambda chỉ đơn giản gọi lại 1 method có sẵn |
| Stream pipeline | Nguồn → Intermediate (nhiều bước, lazy) → Terminal (kích hoạt, dùng 1 lần) |
| `map` vs `flatMap` | map: 1-thành-1; flatMap: làm phẳng cấu trúc lồng nhau |
| `reduce()` | Tổng quát hóa của sum/max/min — "gộp" nhiều giá trị thành 1 |
| `Collectors.groupingBy()` | Tương đương `GROUP BY` trong SQL |
| Lazy Evaluation | Intermediate Operation chỉ ghi nhận, KHÔNG chạy cho đến khi có Terminal Operation |
| Stream dùng 1 lần | Gọi Terminal xong là "hết hạn" — cần Stream mới nếu muốn xử lý lại |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau có lỗi compile không? Giải thích.
```java
int total = 0;
List<Integer> numbers = List.of(1, 2, 3);
numbers.forEach(n -> total += n);
```

**Câu 2.** Đoạn code sau in ra gì? Giải thích thứ tự thực thi.
```java
List.of(1, 2, 3).stream()
    .peek(n -> System.out.println("Peek: " + n))
    .filter(n -> n % 2 == 0)
    .forEach(n -> System.out.println("ForEach: " + n));
```

**Câu 3.** Đoạn code sau gây lỗi gì lúc chạy? Giải thích.
```java
Stream<Integer> stream = List.of(1, 2, 3).stream();
long count1 = stream.count();
long count2 = stream.count();
```

**Câu 4.** Phân biệt kết quả của 2 đoạn sau — vì sao khác nhau?
```java
// Đoạn A
List<List<Integer>> a = List.of(List.of(1,2), List.of(3,4)).stream()
    .map(list -> list) // map giữ nguyên cấu trúc
    .toList();

// Đoạn B
List<Integer> b = List.of(List.of(1,2), List.of(3,4)).stream()
    .flatMap(list -> list.stream())
    .toList();
```

**Câu 5.** Chuyển đoạn vòng lặp sau sang Stream API (dùng `filter`, `map`, `collect`):
```java
List<String> result = new ArrayList<>();
for (String name : List.of("pho", "an", "binh", "cuong")) {
    if (name.length() > 2) {
        result.add(name.toUpperCase());
    }
}
```

---

### Phần B — Bài tập viết code

**Bài 1 — Xử lý danh sách sinh viên bằng Stream (bài tổng hợp cơ bản).**
Cho `record Student(String name, int age, double gpa)`, tạo danh sách ít nhất 8 sinh viên. Dùng Stream API để:
- Lọc ra sinh viên có `gpa >= 3.5`.
- Lấy danh sách tên (chỉ tên, không phải cả object) của những sinh viên đó, đã sắp xếp alphabet.
- Tính GPA trung bình của toàn bộ danh sách gốc (dùng `Collectors.averagingDouble` hoặc `mapToDouble().average()`).
- Tìm sinh viên có GPA cao nhất (dùng `max()` với `Comparator`, trả về `Optional<Student>`).

**Bài 2 — Group By & thống kê nâng cao.**
Dùng lại danh sách `Employee(name, department, salary)` ở mục 8. Viết chương trình:
- Nhóm nhân viên theo `department`, trong mỗi nhóm chỉ lấy **danh sách tên** (không phải cả object) — gợi ý: `Collectors.groupingBy(..., Collectors.mapping(Employee::name, Collectors.toList()))`.
- Tìm nhân viên có lương cao nhất **trong từng phòng ban** (gợi ý: `Collectors.groupingBy(..., Collectors.maxBy(Comparator.comparingDouble(Employee::salary)))`).
- Tính tổng quỹ lương toàn công ty bằng `mapToDouble().sum()`.

**Bài 3 — flatMap với dữ liệu lồng nhau thực tế.**
Cho `record Order(String customerId, List<String> productNames)` — mỗi đơn hàng có nhiều sản phẩm. Cho danh sách ít nhất 4 `Order`. Dùng `flatMap` để lấy ra **danh sách duy nhất, không trùng lặp** (dùng `distinct()`) của tất cả sản phẩm đã từng được đặt mua trong toàn bộ đơn hàng, sắp xếp alphabet.

**Bài 4 — So sánh hiệu năng Stream tuần tự vs song song.**
Tạo `List<Integer>` chứa 10 triệu phần tử ngẫu nhiên. So sánh thời gian tính tổng bằng 3 cách: (a) vòng lặp `for` truyền thống, (b) `stream().reduce()`, (c) `parallelStream().reduce()`. Đo bằng `System.nanoTime()`, in bảng so sánh, nêu nhận xét (gợi ý: kết quả có thể khác nhau tùy số nhân CPU của máy chạy — quan trọng là hiểu **xu hướng** và **lý do**, không phải con số tuyệt đối).

**Bài 5 — Bài toán tổng hợp: Báo cáo doanh thu (mô phỏng thực tế backend).**
Cho `record OrderItem(String category, String productName, double price, int quantity)`. Tạo danh sách ít nhất 10 `OrderItem` thuộc 3 category khác nhau. Dùng Stream API viết method `Map<String, Double> revenueByCategory(List<OrderItem> items)` tính **tổng doanh thu (price × quantity)** theo từng category, trả về `Map<String, Double>` đã sắp xếp theo doanh thu **giảm dần** (gợi ý: `groupingBy` để tính tổng trước, sau đó cần thêm bước sắp xếp `Map` — có thể dùng `LinkedHashMap` để giữ thứ tự sau khi sort thủ công bằng Stream trên `entrySet()`).

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Lỗi compile** — `total` không phải effectively final (bị gán lại giá trị bên trong lambda thông qua `+=`), vi phạm ràng buộc variable capture. Muốn cộng dồn, nên dùng `reduce()` hoặc `IntStream.sum()` thay vì biến ngoài.
2. In xen kẽ theo **từng phần tử một** qua toàn bộ pipeline (không phải chạy hết `peek` cho mọi phần tử rồi mới `filter`):
```
Peek: 1
Peek: 2
ForEach: 2
Peek: 3
```
(Số 1 và 3 bị `filter` loại nên không có dòng `ForEach` tương ứng — nhưng `peek` vẫn chạy cho MỌI phần tử vì nó đứng trước filter trong pipeline.)
3. Lỗi `IllegalStateException: stream has already been operated upon or closed` ở dòng `count2` — vì Stream chỉ dùng được đúng 1 lần, `count1` đã "tiêu thụ" hết `stream` rồi.
4. Đoạn A: `map(list -> list)` không thay đổi gì, kết quả vẫn là `List<List<Integer>>` (cấu trúc lồng nhau giữ nguyên). Đoạn B: `flatMap` "làm phẳng" từng `List<Integer>` con thành các phần tử `Integer` riêng lẻ trong 1 Stream duy nhất, kết quả là `List<Integer>` phẳng: `[1, 2, 3, 4]`.
5.
```java
List<String> result = Stream.of("pho", "an", "binh", "cuong")
    .filter(name -> name.length() > 2)
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Ví dụ lấy top sinh viên GPA cao:
```java
List<String> topNames = students.stream()
    .filter(s -> s.gpa() >= 3.5)
    .map(Student::name)
    .sorted()
    .toList();
```
- **Bài 3:** Ví dụ đoạn `flatMap` cốt lõi:
```java
List<String> allProducts = orders.stream()
    .flatMap(order -> order.productNames().stream())
    .distinct()
    .sorted()
    .toList();
```
- **Bài 4:** Với 10 triệu phần tử, `parallelStream()` **thường** nhanh hơn `stream()` tuần tự trên máy nhiều nhân CPU (do chia nhỏ công việc xử lý song song), nhưng chênh lệch không phải lúc nào cũng tuyến tính theo số nhân do overhead chia việc/gộp kết quả — bài học quan trọng nhất là: **luôn đo đạc thực tế (benchmark) trước khi quyết định dùng `parallelStream()`**, không nên mặc định cho rằng "song song luôn nhanh hơn".
- **Bài 5:** Gợi ý cấu trúc lời giải — bước 1 tính tổng theo category bằng `groupingBy` + `summingDouble`; bước 2 sắp xếp kết quả `Map` bằng cách đưa `entrySet()` vào Stream, `sorted()` theo value giảm dần, rồi `collect` lại vào `LinkedHashMap` (dùng `Collectors.toMap(..., ..., mergeFunction, LinkedHashMap::new)` để giữ đúng thứ tự đã sắp xếp — đây là kỹ thuật khá nâng cao, đáng để thực hành kỹ vì rất hay gặp trong các bài toán báo cáo/thống kê thực tế).

</details>

---

*File tiếp theo trong lộ trình: **Module 04 — Exception Handling & I/O** (try-with-resources, custom exception, exception chaining, File I/O cơ bản).*
