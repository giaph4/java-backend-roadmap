# Module 03.3 — Stream API & Lambda

> **Mức độ ưu tiên: Cao trong thực tế** — Dù xếp "trung bình" trong lộ trình lý thuyết, đây là phong cách viết code **dùng nhiều nhất** trong mọi codebase Spring Boot hiện đại. Không thành thạo Stream/lambda thì đọc code đồng nghiệp hoặc tài liệu Spring sẽ chật vật, và dễ viết pipeline sai một cách khó phát hiện (dựa vào `peek`, tái dùng Stream, `parallelStream()` có side-effect...).

> **Phạm vi bài này:** lambda, functional interface, method reference, Stream API tuần tự & song song, `Collectors`, lazy evaluation. **Chỉ nhắc tên, không đi sâu:** `equals`/`hashCode` (Module 02.4 — `distinct` phụ thuộc vào nó), Collections (Module 03.1), Generics (Module 03.2 — signature các functional interface), Concurrency/`ForkJoinPool` (Module 09 — `parallelStream` chạy trên đó), Optional đầy đủ (Module 04). Các chỗ chạm chủ đề khác chỉ nêu đủ để bài trọn vẹn.

---

## Mục lục

1. [Functional Interface — nền tảng của Lambda](#1-functional-interface--nền-tảng-của-lambda)
2. [Lambda Expression](#2-lambda-expression)
3. [Họ functional interface trong `java.util.function`](#3-họ-functional-interface-trong-javautilfunction)
4. [Method Reference (`::`)](#4-method-reference-)
5. [Tạo Stream — nhiều nguồn & stream nguyên thủy](#5-tạo-stream--nhiều-nguồn--stream-nguyên-thủy)
6. [Thao tác trung gian (Intermediate Operations)](#6-thao-tác-trung-gian-intermediate-operations)
7. [Thao tác kết thúc (Terminal Operations)](#7-thao-tác-kết-thúc-terminal-operations)
8. [`Collectors` — thu thập kết quả](#8-collectors--thu-thập-kết-quả)
9. [Lazy Evaluation & ngữ nghĩa Stream](#9-lazy-evaluation--ngữ-nghĩa-stream)
10. [`parallelStream()` — song song & cạm bẫy](#10-parallelstream--song-song--cạm-bẫy)
11. [Khi nào NÊN và KHÔNG NÊN dùng Stream](#11-khi-nào-nên-và-không-nên-dùng-stream)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. Functional Interface — nền tảng của Lambda

**Functional interface** = interface có **đúng một method trừu tượng** (Single Abstract Method — SAM). Được phép có thêm `default`/`static`/`private` method (Module 02.2) mà vẫn là functional interface.

```java
@FunctionalInterface   // KHÔNG bắt buộc, nhưng nên có — compiler báo lỗi nếu vô tình thêm method trừu tượng thứ 2
public interface Calculator {
    int calculate(int a, int b);
}
```

### Ngoại lệ: method của `Object` không tính vào SAM

Một functional interface **được** khai báo lại các method `public` của `Object` (`equals`, `hashCode`, `toString`) mà không phá số đếm SAM — vì mọi implementation đã có sẵn chúng từ `Object`. `java.util.Comparator` là ví dụ: nó có `compare(T,T)` **và** `equals(Object)` nhưng vẫn là functional interface.

```java
@FunctionalInterface
public interface MyComparator<T> {
    int compare(T a, T b);        // SAM duy nhất
    boolean equals(Object o);      // không tính — kế thừa từ Object
}
```

Vì chỉ có **một** method cần cài đặt, Java cho phép thay thế bằng **lambda** hoặc **method reference** thay vì viết class/anonymous class dài dòng.

---

## 2. Lambda Expression

### Anonymous class (cũ) vs Lambda (Java 8+)

```java
// CŨ — anonymous class
Calculator add = new Calculator() {
    @Override public int calculate(int a, int b) { return a + b; }
};
// MỚI — lambda
Calculator add2 = (a, b) -> a + b;
```

### Cú pháp

```java
Calculator add   = (a, b) -> a + b;                      // biểu thức đơn — tự return
Calculator add2  = (int a, int b) -> { return a + b; };   // khối lệnh — cần {} và return
Runnable   task  = () -> System.out.println("Chạy");      // không tham số
Runnable   task2 = () -> { log("A"); log("B"); };          // nhiều lệnh
Function<Integer,Integer> sq = n -> n * n;                 // 1 tham số — bỏ được ngoặc
```

### Lambda KHÔNG có kiểu cố hữu — "target typing"

Cùng một lambda có thể mang nhiều kiểu tùy ngữ cảnh nơi nó xuất hiện:

```java
Calculator        c  = (a, b) -> a + b;   // Calculator
IntBinaryOperator op = (a, b) -> a + b;   // IntBinaryOperator — cùng thân, khác kiểu đích
// var x = (a, b) -> a + b;               // ❌ không suy luận được: không có "kiểu đích"
```

### Lambda vs anonymous class — `this` và bản chất

```java
class Widget {
    private String id = "W1";
    Runnable asLambda()    { return () -> System.out.println(id + " / " + this.getClass()); }
    Runnable asAnonymous() { return new Runnable() {
        public void run() { System.out.println(/* id vẫn OK */ " / " + this.getClass()); }  // this = Runnable ẩn danh
    }; }
}
```

| | Lambda | Anonymous class |
|---|---|---|
| `this` | **Instance bao ngoài** (`Widget`) | **Chính object ẩn danh** |
| File `.class` riêng | Không — dịch bằng `invokedynamic` + `LambdaMetafactory` lúc chạy | Có — `Widget$1.class` |
| Che biến bao ngoài (shadowing) | Không được | Được |
| Có state/field riêng | Không | Có |
| Không "capture" gì | Tái dùng **một** instance (non-capturing → singleton) | Tạo instance mới mỗi lần |

Liên hệ Module 01.3 (mục `this` trong lambda vs anonymous).

### Variable capture — phải "effectively final"

```java
int discount = 10;
Calculator apply = (price, q) -> price - discount;   // capture discount

int counter = 0;
Runnable r = () -> System.out.println(counter);
// counter = 5;   // ❌ "Variable used in lambda should be final or effectively final"
```

- Biến local bị capture **không được gán lại** (dù không cần từ khóa `final`).
- **Field** của object thì được sửa thoải mái (capture qua `this`, không phải biến local): `() -> this.count++` hợp lệ.
- "Lách" bằng mảng 1 phần tử `int[] box = {0}; ... box[0]++;` hoặc `AtomicInteger` — chạy được nhưng là **code smell**: thường nên dùng `reduce`/`Collectors`/`IntStream.sum()` thay vì tích lũy vào biến ngoài (nhất là khi chạy song song → race condition).

### Checked exception trong lambda

`Function`, `Consumer`... **không khai báo `throws`** → lambda ném checked exception sẽ không compile:

```java
// Function<String,byte[]> read = p -> Files.readAllBytes(Path.of(p));   // ❌ IOException không khớp
Function<String,byte[]> read = p -> {
    try { return Files.readAllBytes(Path.of(p)); }
    catch (IOException e) { throw new UncheckedIOException(e); }          // bọc thành unchecked
};
```

Giải pháp khác: tự định nghĩa `@FunctionalInterface ThrowingFunction<T,R,E extends Exception>` rồi có helper `unchecked(...)`.

---

## 3. Họ functional interface trong `java.util.function`

### Bốn interface cốt lõi

| Interface | Method | Ý nghĩa | Ví dụ |
|---|---|---|---|
| `Function<T,R>` | `R apply(T)` | Biến đổi `T` → `R` | `Function<String,Integer> len = String::length;` |
| `Predicate<T>` | `boolean test(T)` | Kiểm tra điều kiện | `Predicate<Integer> even = n -> n % 2 == 0;` |
| `Supplier<T>` | `T get()` | Cung cấp giá trị (lười) | `Supplier<UUID> id = UUID::randomUUID;` |
| `Consumer<T>` | `void accept(T)` | Tiêu thụ, không trả về | `Consumer<String> log = System.out::println;` |

### Biến thể arity & "cùng kiểu"

```java
BiFunction<Integer,Integer,Integer> add = Integer::sum;   // 2 tham số
BiPredicate<String,Integer> lenIs = (s, n) -> s.length() == n;
BiConsumer<String,Integer> put = map::put;
UnaryOperator<String>  up  = String::toUpperCase;          // Function<T,T>
BinaryOperator<Integer> sum = Integer::sum;                // BiFunction<T,T,T>
```

> Không có `TriFunction` — quá 2 tham số phải tự khai báo functional interface riêng.

### Biến thể nguyên thủy — TRÁNH boxing

`Function<Integer,Integer>` box/unbox mỗi lần gọi. Với số, dùng bản chuyên biệt:

| Nhóm | Ví dụ |
|---|---|
| `IntPredicate`, `LongPredicate`, `DoublePredicate` | `IntPredicate pos = n -> n > 0;` |
| `IntFunction<R>`, `ToIntFunction<T>`, `IntToLongFunction` | `ToIntFunction<String> l = String::length;` |
| `IntUnaryOperator`, `IntBinaryOperator` | `IntBinaryOperator max = Math::max;` |
| `IntSupplier`, `BooleanSupplier` | `IntSupplier dice = () -> 1 + rnd.nextInt(6);` |
| `IntConsumer`, `ObjIntConsumer<T>` | `IntConsumer p = System.out::println;` |

### Tổ hợp — `compose`, `andThen`, `and/or/negate`, `identity`

```java
Function<Integer,Integer> f = x -> x + 1;
Function<Integer,Integer> g = x -> x * 2;
f.andThen(g).apply(3);   // g(f(3)) = (3+1)*2 = 8
f.compose(g).apply(3);   // f(g(3)) = (3*2)+1 = 7
Function.<String>identity().apply("x");   // "x" — hay dùng trong Collectors.toMap

Predicate<Integer> pos  = n -> n > 0;
Predicate<Integer> even = n -> n % 2 == 0;
pos.and(even).test(4);   // true
pos.or(even).test(-4);   // true
pos.negate().test(-1);   // true
Predicate.not(String::isBlank);   // Java 11 — phủ định method reference gọn hơn negate()

Consumer<String> a = s -> System.out.print("[");
Consumer<String> b = System.out::println;
a.andThen(b).accept("x");   // in "[" rồi "x\n"
```

---

## 4. Method Reference (`::`)

Khi lambda **chỉ gọi lại một method có sẵn**, viết gọn bằng method reference.

| Loại | Lambda | Method reference | Ghi chú |
|---|---|---|---|
| Static | `n -> Integer.parseInt(n)` | `Integer::parseInt` | |
| Instance của **object cụ thể** (bound) | `s -> out.println(s)` | `out::println` | receiver cố định, bắt tại thời điểm tạo |
| Instance của **kiểu bất kỳ** (unbound) | `(String s) -> s.toUpperCase()` | `String::toUpperCase` | **tham số đầu tiên trở thành receiver** |
| Constructor | `() -> new ArrayList<>()` | `ArrayList::new` | |
| Constructor mảng | `n -> new String[n]` | `String[]::new` | kiểu là `IntFunction<String[]>` |
| `super` method | `() -> super.toString()` | `super::toString` | trong instance method |

```java
List<String> names = new ArrayList<>(List.of("pho", "an", "binh"));

names.forEach(System.out::println);                         // bound
names.replaceAll(String::toUpperCase);                       // unbound — receiver là phần tử
names.sort(String::compareToIgnoreCase);                     // unbound 2 tham số: a.compareToIgnoreCase(b)
String[] arr = names.stream().toArray(String::new);          // ✗ sai — cần String[]::new
String[] ok  = names.stream().toArray(String[]::new);        // ✓

Supplier<List<String>> factory = ArrayList::new;
BiFunction<String,String,Boolean> eq = String::equals;      // unbound: (a,b) -> a.equals(b)
```

> **Nguyên tắc chọn:** lambda dạng `x -> x.m()` hoặc `x -> C.m(x)` — **không thêm logic** — thì đổi sang method reference. Còn `x -> x.m() + 1` hay `x -> C.m(x, other)` thì giữ lambda.

---

## 5. Tạo Stream — nhiều nguồn & stream nguyên thủy

```java
Collection<T>.stream()                       // phổ biến nhất
Arrays.stream(array)                          // từ mảng (có bản cho int[]/long[]/double[])
Stream.of("a", "b", "c")                      // từ vài giá trị rời
Stream.empty()                                // rỗng
Stream.ofNullable(maybeNull)                  // Java 9 — 0 hoặc 1 phần tử
Stream.iterate(1, n -> n * 2)                 // VÔ HẠN: 1,2,4,8,... → cần limit()
Stream.iterate(1, n -> n <= 100, n -> n * 2) // Java 9 — có điều kiện dừng → HỮU HẠN
Stream.generate(Math::random).limit(5)       // VÔ HẠN theo Supplier → cần limit()
"hello".chars()                              // IntStream các mã ký tự
Pattern.compile(",").splitAsStream("a,b,c")  // tách chuỗi lười
Files.lines(path)                            // từng dòng file (nên dùng trong try-with-resources)
```

### `IntStream` / `LongStream` / `DoubleStream` — không boxing

```java
int total = IntStream.rangeClosed(1, 100).sum();               // range: [0,n)  rangeClosed: [1,n]
OptionalDouble avg = words.stream().mapToInt(String::length).average();
IntSummaryStatistics st = words.stream().mapToInt(String::length).summaryStatistics();
// st.getCount() getSum() getMin() getMax() getAverage()

List<Integer> boxed = IntStream.range(0, 5).boxed().toList();   // IntStream → Stream<Integer>
Stream<String> labels = IntStream.range(0, 3).mapToObj(i -> "row" + i);
```

Chuyển đổi hai chiều: `stream.mapToInt(...)` / `intStream.boxed()` / `intStream.mapToObj(...)`.

---

## 6. Thao tác trung gian (Intermediate Operations)

Trả về **Stream mới**, cho nối chuỗi, **lazy** (mục 9). Chia hai nhóm:

| Nhóm | Đặc điểm | Ví dụ |
|---|---|---|
| **Stateless** — xử lý từng phần tử độc lập | Rẻ, song song hóa tốt | `filter`, `map`, `mapToInt`, `flatMap`, `peek` |
| **Stateful** — cần nhìn phần tử khác / đệm toàn bộ | Đắt hơn, có thể chặn stream vô hạn | `sorted` (đệm hết), `distinct` (nhớ đã thấy), `limit`, `skip`, `takeWhile`, `dropWhile` |

```java
List<Integer> ns = List.of(5, 3, 8, 1, 9, 2, 8);
ns.stream().filter(n -> n > 3);
ns.stream().map(n -> n * 2);
ns.stream().sorted(Comparator.reverseOrder());
ns.stream().distinct();                 // dựa equals()/hashCode() — Module 02.4
ns.stream().limit(3);
ns.stream().skip(2);
ns.stream().takeWhile(n -> n < 8);      // Java 9 — lấy tới khi gặp phần tử SAI điều kiện: [5,3]
ns.stream().dropWhile(n -> n < 8);      // Java 9 — bỏ tiền tố thỏa điều kiện: [8,1,9,2,8]
```

> ⚠️ **`peek` không đáng tin cho logic.** Đặc tả cho phép runtime **bỏ qua** `peek` nếu kết quả không cần duyệt phần tử — ví dụ `Stream.of(1,2,3).peek(System.out::println).count()` từ Java 9 có thể **không in gì** (vì `count()` suy ra được số lượng mà không chạy pipeline). Chỉ dùng `peek` để debug tạm, không để tạo side-effect thật.

### `flatMap` — làm phẳng cấu trúc lồng

```java
List<List<String>> nested = List.of(List.of("Java","Spring"), List.of("Go"));
List<String> flat = nested.stream().flatMap(List::stream).toList();   // [Java, Spring, Go]

// Thường gặp: 1 object chứa nhiều con
record Order(String id, List<String> products) {}
List<String> allProducts = orders.stream()
    .flatMap(o -> o.products().stream())
    .distinct().sorted().toList();
```

- `map`: 1 phần tử vào → **1** phần tử ra. `flatMap`: 1 phần tử vào → **một Stream con** → nối tất cả lại thành một Stream phẳng.
- `mapMulti` (Java 16) — thay thế `flatMap` khi mỗi phần tử sinh ít phần tử con, tránh tạo Stream trung gian: `stream.mapMulti((o, sink) -> o.products().forEach(sink))`.

---

## 7. Thao tác kết thúc (Terminal Operations)

**Kích hoạt** pipeline, trả về kết quả **không phải Stream**. Sau đó Stream **hết hạn** (mục 9).

```java
List<Integer> ns = List.of(5, 3, 8, 1, 9);
ns.stream().forEach(System.out::println);          // không đảm bảo thứ tự khi song song
ns.stream().forEachOrdered(System.out::println);   // giữ encounter order kể cả song song
long c = ns.stream().filter(n -> n > 3).count();
Optional<Integer> mx = ns.stream().max(Integer::compareTo);
boolean any  = ns.stream().anyMatch(n -> n > 8);   // short-circuit: dừng ngay khi thấy true
boolean all  = ns.stream().allMatch(n -> n > 0);
boolean none = ns.stream().noneMatch(n -> n < 0);
Integer[] arr = ns.stream().toArray(Integer[]::new);
```

### `findFirst` vs `findAny`

```java
Optional<Integer> a = ns.stream().filter(n -> n > 3).findFirst();  // phần tử ĐẦU theo encounter order
Optional<Integer> b = ns.parallelStream().filter(n -> n > 3).findAny();  // BẤT KỲ phần tử thỏa — nhanh hơn khi song song
```

### `reduce` — ba dạng

```java
// 1) reduce(BinaryOperator) → Optional (rỗng nếu stream rỗng)
Optional<Integer> s1 = ns.stream().reduce(Integer::sum);

// 2) reduce(identity, accumulator) → giá trị (identity khi stream rỗng)
int s2 = ns.stream().reduce(0, Integer::sum);

// 3) reduce(identity, accumulator, combiner) → khi kiểu tích lũy KHÁC kiểu phần tử, hoặc chạy song song
int totalLen = words.stream()
    .reduce(0, (acc, w) -> acc + w.length(), Integer::sum);
//         ^acc:int      ^w:String           ^gộp 2 kết quả từ 2 luồng song song
```

Yêu cầu để `reduce` (nhất là song song) đúng:
- `identity`: `combiner.apply(identity, x) == x` (0 cho cộng, 1 cho nhân, `""` cho nối).
- `accumulator` & `combiner`: **kết hợp được (associative)**, không phụ thuộc thứ tự, không side-effect.

> `count()` (Java 9+) có thể trả kết quả **mà không chạy** các thao tác trung gian nếu số lượng suy ra được từ nguồn (`SIZED`) và pipeline không có `filter`/`flatMap` — lý do khác để không nhét logic vào `peek`.

---

## 8. `Collectors` — thu thập kết quả

```java
import static java.util.stream.Collectors.*;

List<String> names = List.of("Pho", "An", "Binh", "Cuong");

names.stream().collect(toList());          // List (khả biến, kiểu không đảm bảo)
names.stream().toList();                    // Java 16 — BẤT BIẾN, ngắn hơn; ưu tiên khi chỉ đọc
names.stream().collect(toSet());
names.stream().collect(toUnmodifiableList());   // Java 10 — bất biến, tường minh
names.stream().collect(joining(", ", "[", "]"));   // "[Pho, An, Binh, Cuong]"
```

### `toMap` — bốn dạng, chú ý va chạm key

```java
// 2 tham số — NÉM IllegalStateException nếu 2 phần tử cho cùng key
Map<Integer,String> m1 = names.stream().collect(toMap(String::length, s -> s));   // "Pho" & "Binh"? → nổ

// 3 tham số — merge function xử lý va chạm
Map<Integer,String> m2 = names.stream()
    .collect(toMap(String::length, s -> s, (a, b) -> a + "|" + b));

// 4 tham số — chọn loại Map
Map<Integer,String> m3 = names.stream()
    .collect(toMap(String::length, s -> s, (a, b) -> a, TreeMap::new));

// Đảo List thành Map<id, object>:
Map<Long,User> byId = users.stream().collect(toMap(User::id, Function.identity()));
```

### `groupingBy` — ba dạng + downstream collector

```java
record Employee(String name, String dept, double salary) {}

Map<String,List<Employee>> g1 = emp.stream().collect(groupingBy(Employee::dept));
Map<String,Long>           g2 = emp.stream().collect(groupingBy(Employee::dept, counting()));
Map<String,Double>         g3 = emp.stream().collect(groupingBy(Employee::dept, summingDouble(Employee::salary)));
Map<String,Double>         g4 = emp.stream().collect(groupingBy(Employee::dept, averagingDouble(Employee::salary)));
Map<String,List<String>>   g5 = emp.stream().collect(groupingBy(Employee::dept, mapping(Employee::name, toList())));
Map<String,Set<String>>    g6 = emp.stream().collect(groupingBy(Employee::dept, mapping(Employee::name, toSet())));
Map<String,Optional<Employee>> g7 = emp.stream()
    .collect(groupingBy(Employee::dept, maxBy(comparingDouble(Employee::salary))));

// Bỏ Optional bằng collectingAndThen:
Map<String,Employee> topPerDept = emp.stream().collect(groupingBy(Employee::dept,
    collectingAndThen(maxBy(comparingDouble(Employee::salary)), Optional::get)));

// Chọn loại Map + lồng nhiều tầng:
Map<String, Map<Boolean, List<Employee>>> g8 = emp.stream().collect(
    groupingBy(Employee::dept, TreeMap::new,
        partitioningBy(e -> e.salary() >= 15_000_000)));

// Java 9: filtering / flatMapping làm downstream (khác đặt filter TRƯỚC groupingBy: vẫn giữ key nhóm rỗng)
Map<String,List<Employee>> g9 = emp.stream()
    .collect(groupingBy(Employee::dept, filtering(e -> e.salary() > 10_000_000, toList())));
```

### `partitioningBy` — luôn có ĐỦ hai key `true`/`false`

```java
Map<Boolean,List<Employee>> p = emp.stream().collect(partitioningBy(e -> e.salary() > 15_000_000));
// p.get(true) và p.get(false) LUÔN tồn tại (có thể là list rỗng) — khác groupingBy trên boolean
```

### `teeing` (Java 12) — hai collector song song rồi gộp

```java
record MinMax(int min, int max) {}
MinMax r = Stream.of(3, 1, 4, 1, 5).collect(teeing(
    minBy(Integer::compareTo),
    maxBy(Integer::compareTo),
    (mn, mx) -> new MinMax(mn.orElseThrow(), mx.orElseThrow())));
```

### Sắp xếp `Map` theo value → `LinkedHashMap`

```java
Map<String,Double> sorted = revenue.entrySet().stream()
    .sorted(Map.Entry.<String,Double>comparingByValue().reversed())
    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                   (a, b) -> a, LinkedHashMap::new));   // giữ đúng thứ tự đã sort
```

> `groupingBy` ↔ `GROUP BY` trong SQL (Module 12). Nếu quen SQL thì downstream collector chính là `COUNT`/`SUM`/`AVG`/`HAVING`.

---

## 9. Lazy Evaluation & ngữ nghĩa Stream

### Trung gian chỉ "ghi nhận", terminal mới chạy

```java
Stream<String> s = names.stream().filter(n -> {
    System.out.println("filter: " + n);   // chưa in gì ở đây
    return n.length() > 2;
});
System.out.println("Đã dựng pipeline");
long c = s.count();                        // ĐẾN ĐÂY "filter:" mới in
```

Mỗi phần tử đi **hết** pipeline rồi mới tới phần tử sau (per-element, không phải theo tầng). Nhờ vậy `findFirst`/`anyMatch`/`limit` **short-circuit** được — dừng sớm, xử lý được cả nguồn vô hạn.

### Bốn quy tắc bắt buộc nhớ

| Quy tắc | Hệ quả nếu vi phạm |
|---|---|
| **Stream dùng một lần** | Gọi terminal lần 2 → `IllegalStateException: stream has already been operated upon or closed`. Muốn xử lý lại → `list.stream()` mới. |
| **Không sửa nguồn khi pipeline đang chạy** (interference) | `list.stream().forEach(x -> list.add(x))` → `ConcurrentModificationException` hoặc kết quả sai. |
| **Behavioral parameter phải stateless & không side-effect** | Lambda trong `map`/`filter` đọc–ghi biến chia sẻ → sai khi song song, khó hiểu khi tuần tự. |
| **`peek` có thể bị bỏ qua** | Đừng đặt logic vào `peek`. |

### Encounter order

Nguồn có thứ tự (`List`, mảng, `sorted`) → Stream có *encounter order*; `HashSet`, `Stream.generate` → không. `forEach` song song **không** giữ order (dùng `forEachOrdered` nếu cần). `unordered()` chủ động bỏ ràng buộc order để `distinct`/`limit` chạy nhanh hơn khi song song.

---

## 10. `parallelStream()` — song song & cạm bẫy

```java
long n = bigList.parallelStream().filter(x -> x > 0).count();
int sum = list.stream().parallel().mapToInt(Integer::intValue).sum();   // .parallel() bật trên stream đã có
```

### Cơ chế & giới hạn

- Chạy trên **`ForkJoinPool.commonPool()`** dùng chung toàn JVM, kích thước ≈ *số nhân − 1*. Một tác vụ song song nặng làm chậm mọi `parallelStream` khác trong tiến trình.
- Muốn pool riêng: `new ForkJoinPool(4).submit(() -> list.parallelStream()....).get();`
- Hiệu quả chia việc phụ thuộc **`Spliterator`**: `ArrayList`/mảng/`IntStream.range` chia đôi rẻ → tốt; `LinkedList`, `Files.lines`, `Stream.iterate` chia kém → thường **chậm hơn** tuần tự.

### Khi nào KHÔNG dùng

| Trường hợp | Lý do |
|---|---|
| Dữ liệu nhỏ (< ~10⁴ phần tử, thao tác rẻ) | Overhead fork/join/merge > lợi ích |
| Tác vụ I/O (gọi DB, HTTP, đọc file) | Chặn thread pool dùng chung; nên dùng thread pool riêng / async |
| Lambda có side-effect / state chia sẻ | Race condition — `Collectors.groupingBy` (không `Concurrent`) và `reduce` với `identity` không hợp lệ đều cho kết quả sai |
| Cần giữ thứ tự nghiêm ngặt trong `forEach` | Phải `forEachOrdered` → mất phần lớn lợi ích song song |

> **Quy tắc:** mặc định dùng `stream()` tuần tự. Chỉ đổi sang `parallelStream()` khi đã **benchmark** trên dữ liệu thật và thấy lợi ích rõ ràng.

---

## 11. Khi nào NÊN và KHÔNG NÊN dùng Stream

| Nên dùng Stream | Nên dùng vòng lặp `for` |
|---|---|
| Nhiều bước biến đổi nối tiếp (`filter → map → collect`) | Một bước đơn giản — Stream chỉ làm rối |
| Group / partition / thống kê | Cần `break`/`continue`/`return` giữa chừng phức tạp |
| Ưu tiên diễn đạt ý định (declarative) | Cần chỉ số phần tử (`i`), hoặc duyệt nhiều collection song song theo index |
| Có thể tận dụng song song | Logic nhiều side-effect (ghi biến ngoài, I/O tuần tự) |
| | Vòng lặp cực nóng, cực nhạy hiệu năng (Stream có overhead nhỏ + boxing nếu không dùng `IntStream`) |

Nhược điểm cần cân nhắc: stack trace của lambda khó đọc hơn; debug từng bước phải đặt breakpoint trong lambda; checked exception phải bọc.

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Functional interface | Đúng 1 method trừu tượng (method của `Object` không tính). `@FunctionalInterface` để compiler canh. |
| Lambda | Không có kiểu cố hữu — target typing. `this` = instance bao ngoài (khác anonymous class). Không sinh `.class` riêng. |
| Capture | Biến local phải effectively final; field thì sửa được; "lách" bằng holder là code smell. |
| Checked exception | Không khớp `Function`/`Consumer` → bọc `UncheckedIOException` hoặc functional interface tự định nghĩa. |
| 4 core + biến thể | `Function/Predicate/Supplier/Consumer`; `Bi*`, `UnaryOperator`, `BinaryOperator`; bản `Int/Long/Double*` tránh boxing. |
| Tổ hợp | `andThen`/`compose`/`identity`; `and`/`or`/`negate`/`Predicate.not`. |
| Method reference | 4 loại: static, bound, **unbound** (tham số đầu = receiver), constructor (`Type::new`, `Type[]::new`). |
| Tạo Stream | `stream()`, `Stream.of`, `Arrays.stream`, `iterate`/`generate` (+`limit`), `IntStream.range`, `chars()`, `Files.lines`. |
| Stream nguyên thủy | `IntStream`/`LongStream`/`DoubleStream` — `sum`/`average`/`summaryStatistics`, `boxed`/`mapToObj`. |
| Trung gian | Lazy, trả Stream mới. Stateless (`filter`/`map`/`flatMap`) vs stateful (`sorted`/`distinct`/`limit`/`takeWhile`). |
| `peek` | Chỉ để debug — runtime được phép bỏ qua. |
| `map` vs `flatMap` | 1→1 vs 1→Stream-con rồi làm phẳng. `mapMulti` (Java 16) thay thế nhẹ hơn. |
| Kết thúc | Kích hoạt pipeline, Stream hết hạn. `findFirst` (order) vs `findAny` (song song). Short-circuit: `anyMatch`/`findFirst`/`limit`. |
| `reduce` | 3 dạng; song song cần identity hợp lệ + accumulator/combiner associative, không side-effect. |
| `Collectors` | `toList`(Java 16 bất biến) / `toUnmodifiable*`; `toMap` cần merge function khi trùng key; `groupingBy` + downstream (`counting`/`summingX`/`mapping`/`maxBy`/`collectingAndThen`); `partitioningBy` luôn đủ 2 key; `teeing`. |
| Lazy & ngữ nghĩa | Dùng 1 lần; không sửa nguồn khi chạy; behavioral param stateless; encounter order. |
| `parallelStream` | Chạy trên common `ForkJoinPool`; tốt với `ArrayList`/`range`, tệ với `LinkedList`/I/O; benchmark trước khi dùng. |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Có lỗi compile không? Vì sao? Viết lại đúng "kiểu functional".
```java
int total = 0;
List.of(1, 2, 3).forEach(n -> total += n);
```

**Câu 2.** In ra gì, theo thứ tự nào?
```java
List.of(1, 2, 3).stream()
    .peek(n -> System.out.println("peek " + n))
    .filter(n -> n % 2 == 0)
    .forEach(n -> System.out.println("each " + n));
```

**Câu 3.** Lỗi gì lúc chạy? Ở dòng nào? Vì sao?
```java
Stream<Integer> s = List.of(1, 2, 3).stream();
long a = s.count();
long b = s.count();
```

**Câu 4.** Hai đoạn khác kết quả thế nào, vì sao?
```java
var a = Stream.of(List.of(1, 2), List.of(3, 4)).map(x -> x).toList();
var b = Stream.of(List.of(1, 2), List.of(3, 4)).flatMap(List::stream).toList();
```

**Câu 5.** Đoạn này có thể ném exception gì lúc chạy? Cách sửa để không nổ?
```java
Map<Integer, String> m = Stream.of("Pho", "An", "Binh", "Cuong")
    .collect(Collectors.toMap(String::length, s -> s));
```

**Câu 6.** `findFirst()` và `findAny()` khác nhau ở điểm nào? Trên `stream()` tuần tự chúng có luôn cho cùng kết quả không? Trên `parallelStream()` thì sao?

**Câu 7.** Vì sao đoạn này **không đáng tin**? Từ Java mấy thì nó có thể "không in gì"?
```java
long c = Stream.of("a", "b", "c").peek(System.out::println).count();
```

**Câu 8.** Chuyển sang Stream (dùng `filter`, `map`, `toList`):
```java
List<String> r = new ArrayList<>();
for (String n : List.of("pho", "an", "binh", "cuong"))
    if (n.length() > 2) r.add(n.toUpperCase());
```

---

### Phần B — Bài tập viết code

**Bài 1 — Xử lý danh sách sinh viên.**
`record Student(String name, int age, double gpa)`, ≥ 8 phần tử. Dùng Stream:
- Lọc `gpa >= 3.5`, lấy danh sách **tên** đã sắp alphabet.
- GPA trung bình toàn danh sách bằng `mapToDouble(Student::gpa).average()` → xử lý `OptionalDouble`.
- `IntSummaryStatistics` của tuổi (`mapToInt`) — in min/max/avg/count.
- Sinh viên GPA cao nhất bằng `max(Comparator.comparingDouble(Student::gpa))` → `Optional<Student>`.

**Bài 2 — Group By & downstream collector.**
Dùng `Employee(name, dept, salary)`:
- `Map<String, List<String>>` — theo `dept`, mỗi nhóm chỉ **tên** (`groupingBy` + `mapping` + `toList`).
- `Map<String, Employee>` — nhân viên lương cao nhất **mỗi phòng** (`groupingBy` + `collectingAndThen(maxBy(...), Optional::get)`).
- `Map<String, Double>` — tổng quỹ lương mỗi phòng, **sắp giảm dần** theo tổng, thu vào `LinkedHashMap`.
- Tổng quỹ lương công ty bằng `mapToDouble(...).sum()`.

**Bài 3 — flatMap dữ liệu lồng.**
`record Order(String customerId, List<String> products)`, ≥ 4 đơn. Dùng `flatMap` lấy danh sách sản phẩm **không trùng**, sắp alphabet. Rồi dùng `flatMap` + `Collectors.groupingBy` + `counting` để đếm mỗi sản phẩm xuất hiện trong bao nhiêu đơn.

**Bài 4 — Stream vô hạn + short-circuit.**
Dùng `Stream.iterate` sinh dãy Fibonacci, `limit(20)` in 20 số đầu. Dùng `Stream.iterate(seed, hasNext, next)` (3 tham số) sinh các lũy thừa của 2 **nhỏ hơn 1_000_000** mà không cần `limit`. Dùng `IntStream.rangeClosed(2, n)` + `noneMatch` viết `isPrime(int n)`.

**Bài 5 — reduce ba dạng.**
Cho `List<String> words`. Tính: (a) tổng độ dài bằng `reduce(0, (acc,w) -> acc + w.length(), Integer::sum)` — giải thích vai trò combiner; (b) từ dài nhất bằng `reduce((x,y) -> x.length() >= y.length() ? x : y)` → `Optional<String>`; (c) nối tất cả bằng `reduce("", String::concat)` rồi so sánh hiệu năng với `Collectors.joining()` trên danh sách lớn, giải thích vì sao `joining` nhanh hơn.

**Bài 6 — Báo cáo doanh thu (mô phỏng backend).**
`record OrderItem(String category, String product, double price, int qty)`, ≥ 10 phần tử, 3 category. Viết `Map<String, Double> revenueByCategory(List<OrderItem>)`: tổng `price * qty` theo category, trả `LinkedHashMap` sắp **giảm dần** theo doanh thu. Viết thêm `teeing` để trả về đồng thời `(tổng toàn bộ, category doanh thu cao nhất)` trong một lần duyệt.

**Bài 7 — Song song có đo đạc.**
`List<Long>` 10 triệu phần tử ngẫu nhiên. So sánh `System.nanoTime()` cho tổng bằng: (a) `for`, (b) `stream().mapToLong(...).sum()`, (c) `parallelStream().mapToLong(...).sum()`, (d) `parallelStream()` bọc trong `new ForkJoinPool(2).submit(...)`. In bảng, nhận xét về `Spliterator` của `ArrayList` vs nếu đổi sang `LinkedList`.

---

### Phần C — Nâng cao

**Câu 1.** Giải thích "target typing": vì sao `Object o = (a, b) -> a + b;` không compile còn `Runnable r = () -> {};` thì được. Lambda có kiểu tại thời điểm nào — parse, compile, hay runtime?

**Câu 2.** `this` trong lambda vs trong anonymous class khác nhau ra sao, và điều đó ảnh hưởng gì tới việc dùng lambda làm listener cần `removeListener(this)`? Vì sao lambda không sinh file `.class` riêng (nhắc `invokedynamic`/`LambdaMetafactory`)?

**Câu 3.** Phân biệt **capturing** và **non-capturing** lambda về mặt cấp phát object. Vì sao `list.forEach(x -> total[0] += x)` với `int[] total` chạy được nhưng sai khi `list.parallelStream().forEach(...)`? Cách đúng để "tích lũy" là gì?

**Câu 4.** Với `reduce(identity, accumulator, combiner)` chạy song song: phát biểu chính xác ba điều kiện (identity, associativity, không can thiệp) và cho một ví dụ `identity` **sai** (gợi ý: dùng `1` cho phép cộng, hoặc list rỗng chia sẻ) khiến kết quả song song khác kết quả tuần tự.

**Câu 5.** So sánh ba cách "đếm theo nhóm rồi bỏ nhóm rỗng": (a) `filter` trước `groupingBy`, (b) `groupingBy` + downstream `filtering` (Java 9), (c) `groupingBy` xong rồi `entrySet().removeIf`. Kết quả khác nhau thế nào về **các key xuất hiện** trong Map?

**Câu 6.** `Collectors.toMap` không có merge function ném `IllegalStateException` khi trùng key; `Collectors.groupingBy` thì không. Giải thích vì sao thiết kế khác nhau. Khi nào `toMap(..., HashMap::new)` vẫn ném dù đã truyền supplier?

**Câu 7.** `parallelStream()` dùng `ForkJoinPool.commonPool()`. Nêu ba hệ quả vận hành trong một service Spring Boot xử lý nhiều request đồng thời, và giải thích vì sao tác vụ I/O (gọi REST/DB) trong `parallelStream` đặc biệt nguy hiểm. Cách cô lập bằng pool riêng có nhược điểm gì?

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Lỗi compile** — `total` bị gán lại trong lambda (`+=`) nên không effectively final. Đúng kiểu functional: `int total = List.of(1,2,3).stream().mapToInt(Integer::intValue).sum();` hoặc `.reduce(0, Integer::sum)`.
2. Mỗi phần tử đi hết pipeline rồi mới tới phần tử sau:
   ```
   peek 1
   peek 2
   each 2
   peek 3
   ```
   `peek` chạy cho **mọi** phần tử (đứng trước `filter`); `1` và `3` bị `filter` loại nên không có `each` tương ứng.
3. `IllegalStateException: stream has already been operated upon or closed` tại dòng `b` — Stream chỉ dùng một lần, `a` đã tiêu thụ `s`.
4. `a` là `List<List<Integer>>` `[[1,2],[3,4]]` — `map(x -> x)` không đổi cấu trúc. `b` là `List<Integer>` `[1,2,3,4]` — `flatMap` trải mỗi list con thành phần tử rời trong một Stream phẳng.
5. `IllegalStateException: Duplicate key` — "Pho"(3) và ... thực ra "An"(2), "Pho"(3), "Binh"(4), "Cuong"(5) không trùng độ dài, nhưng nếu thêm "Ba"(2) sẽ trùng key 2. Sửa: thêm merge function `toMap(String::length, s -> s, (x, y) -> x + "|" + y)`.
6. `findFirst` trả phần tử **đầu tiên theo encounter order**; `findAny` trả **bất kỳ** phần tử thỏa (cho runtime tự do chọn). Tuần tự: thường cùng kết quả (đều là phần tử đầu), nhưng không có cam kết cho `findAny`. Song song: `findFirst` phải điều phối để lấy đúng phần tử đầu (chậm hơn), `findAny` trả về cái nào tìm thấy trước — nhanh hơn, không xác định.
7. `peek` chỉ để quan sát; đặc tả cho phép runtime bỏ qua nếu không cần duyệt phần tử. Từ **Java 9**, `count()` trên nguồn `SIZED` không có `filter`/`flatMap` trả thẳng số lượng → `peek` không chạy → không in gì.
8. `List<String> r = Stream.of("pho","an","binh","cuong").filter(n -> n.length() > 2).map(String::toUpperCase).toList();`

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `OptionalDouble avg = students.stream().mapToDouble(Student::gpa).average();` → `avg.orElse(0.0)`. Top: `students.stream().max(Comparator.comparingDouble(Student::gpa))`.
- **Bài 2:** Lương cao nhất mỗi phòng: `groupingBy(Employee::dept, collectingAndThen(maxBy(comparingDouble(Employee::salary)), Optional::get))`. Sắp `Map` theo tổng giảm dần: tính `groupingBy(dept, summingDouble(salary))` trước, rồi `entrySet().stream().sorted(Map.Entry.<String,Double>comparingByValue().reversed()).collect(toMap(k,v,(a,b)->a,LinkedHashMap::new))`.
- **Bài 3:** `orders.stream().flatMap(o -> o.products().stream()).distinct().sorted().toList()`. Đếm số đơn chứa mỗi sản phẩm: `orders.stream().flatMap(o -> o.products().stream().distinct()).collect(groupingBy(p -> p, counting()))`.
- **Bài 4:** Fibonacci: `Stream.iterate(new long[]{0,1}, a -> new long[]{a[1], a[0]+a[1]}).limit(20).map(a -> a[0])`. Lũy thừa 2: `Stream.iterate(1, n -> n < 1_000_000, n -> n * 2)`. `isPrime`: `n > 1 && IntStream.rangeClosed(2, (int)Math.sqrt(n)).noneMatch(d -> n % d == 0)`.
- **Bài 5:** (a) combiner `Integer::sum` gộp kết quả từ các luồng con khi chạy song song (kiểu tích lũy `int` khác kiểu phần tử `String`). (b) `words.stream().reduce((x,y) -> x.length() >= y.length() ? x : y)`. (c) `reduce("", String::concat)` tạo chuỗi trung gian mới mỗi bước → O(n²); `Collectors.joining()` dùng `StringBuilder` nội bộ → O(n).
- **Bài 6:** `items.stream().collect(groupingBy(OrderItem::category, summingDouble(i -> i.price() * i.qty())))` rồi sort vào `LinkedHashMap`. `teeing`: `collect(teeing(summingDouble(i -> i.price()*i.qty()), maxBy(comparingDouble(i -> i.price()*i.qty())), (tong, top) -> ...))` — hoặc teeing hai collector `summingDouble` và `groupingBy` rồi hậu xử lý.
- **Bài 7:** Thường (c) nhanh hơn (b) trên máy nhiều nhân với `ArrayList` (Spliterator chia đôi mảng rẻ). (d) giới hạn 2 thread nên chậm hơn (c) dùng cả common pool, nhưng cô lập được. Đổi sang `LinkedList` → (c) có thể **chậm hơn** (b) vì Spliterator phải duyệt tuần tự để chia.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. Lambda **không có kiểu tự thân**; kiểu của nó do "kiểu đích" (target type) tại vị trí gán/tham số quyết định, và điều này xảy ra lúc **compile**. `Object` không phải functional interface nên không có SAM để khớp → không compile. `Runnable` có SAM `run()` khớp `() -> {}`. `var` cũng thất bại vì `var` cần suy ra kiểu từ vế phải, mà lambda lại cần kiểu đích từ vế trái → vòng luẩn quẩn.
2. Lambda: `this` = instance của class bao ngoài. Anonymous class: `this` = chính object ẩn danh. Hệ quả: `button.addListener(e -> ...)` rồi muốn `button.removeListener(this)` — `this` là object bao ngoài, không phải listener; phải giữ tham chiếu lambda vào biến. Lambda không sinh `.class` riêng vì compiler phát ra một lệnh `invokedynamic`; lần chạy đầu, `LambdaMetafactory` dựng (spin) một lớp ẩn hiện thực functional interface và cache lại — tránh "class explosion" khi có hàng nghìn lambda.
3. Non-capturing lambda (không tham chiếu biến ngoài/`this`) được biên dịch thành **một instance singleton** tái dùng. Capturing lambda tạo **instance mới mỗi lần evaluate** (mang theo biến bắt được). `total[0] += x` chạy được vì `total` (biến local kiểu mảng) là effectively final — chỉ *nội dung* mảng đổi; nhưng song song thì nhiều thread cùng ghi `total[0]` → race, mất cập nhật. Đúng: `mapToInt(...).sum()` / `reduce` / `Collectors`.
4. (i) *identity*: `combiner.apply(identity, u).equals(u)` với mọi `u` — `0` cho cộng, `1` cho nhân, `""` cho nối. (ii) *associativity*: `(a op b) op c == a op (b op c)` — trừ và chia vi phạm. (iii) *không can thiệp / không side-effect*: accumulator & combiner không đọc–ghi state chia sẻ, không sửa nguồn. Ví dụ sai: `reduce(1, Integer::sum, Integer::sum)` — mỗi lần chia luồng song song cộng thêm một `1` thừa → tổng song song > tổng tuần tự. Hoặc `identity` là một `ArrayList` dùng chung → các luồng cùng add vào một list.
5. (a) `filter` trước `groupingBy`: phần tử bị loại **trước khi phân nhóm** → key nào không còn phần tử nào thì **không xuất hiện** trong Map. (b) `filtering` downstream (Java 9): phân nhóm trước, lọc trong từng nhóm → key vẫn xuất hiện với **list rỗng**. (c) `groupingBy` đầy đủ rồi `removeIf`: giống (a) về key cuối cùng nhưng đã tốn công dựng nhóm. Khác biệt cốt lõi: **(b) giữ key nhóm rỗng, (a)/(c) không**.
6. `toMap` mô hình hóa quan hệ **1 key → 1 value**; hai phần tử cùng key là mâu thuẫn dữ liệu → ném để lập trình viên biết. `groupingBy` bản chất là **1 key → nhiều value** (gom list) nên trùng key là bình thường. Supplier `HashMap::new` chỉ chọn *loại Map*, không đổi luật va chạm — vẫn ném `IllegalStateException` nếu không có merge function và có hai phần tử cùng key.
7. (i) Common pool dùng chung toàn JVM → một request chạy `parallelStream` nặng làm chậm `parallelStream` của các request khác. (ii) Kích thước pool ≈ số nhân − 1, không co giãn theo tải → nghẽn. (iii) Không có cách ly lỗi/tài nguyên giữa các request. Tác vụ I/O nguy hiểm vì thread common pool bị **chặn** chờ mạng/DB — vài request đủ để cạn pool, mọi `parallelStream` khác đứng hình (fork/join giả định tác vụ CPU-bound, ngắn). Pool riêng (`new ForkJoinPool(n)`) cô lập được nhưng: tốn tài nguyên tạo/quản lý, dễ tạo quá nhiều pool, và vẫn không phải mô hình đúng cho I/O (nên dùng async / thread pool chuyên cho I/O — Module 09).

</details>

---

*File tiếp theo trong lộ trình: **Module 04 — Exception Handling & I/O** (try-with-resources, custom exception, exception chaining, File I/O cơ bản).*
