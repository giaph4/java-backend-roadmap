# Module 03.2 — Generics

> **Mức độ ưu tiên: Cao** — Generics là kiến thức **bắt buộc** để đọc hiểu source code Spring/Hibernate (`JpaRepository<T, ID>`, `ResponseEntity<T>`, `List<T>`...). Không hiểu wildcard (`? extends`/`? super`) sẽ khiến việc đọc signature của các method thư viện trở nên rất khó hiểu, và dễ viết code Generic sai mà compiler không báo lỗi rõ ràng.

---

## Mục lục

1. [Vấn đề trước khi có Generics](#1-vấn-đề-trước-khi-có-generics)
2. [Generic Class](#2-generic-class)
3. [Generic Method](#3-generic-method)
4. [Bounded Type — `<T extends X>`](#4-bounded-type--t-extends-x)
5. [Wildcard — `?`, `? extends`, `? super`](#5-wildcard---extends-super)
6. [PECS — Producer Extends, Consumer Super](#6-pecs--producer-extends-consumer-super)
7. [Type Erasure — cơ chế Generics hoạt động thế nào lúc runtime](#7-type-erasure--cơ-chế-generics-hoạt-động-thế-nào-lúc-runtime)
8. [Generics trong thực tế Spring/JDK](#8-generics-trong-thực-tế-springjdk)
9. [Tổng kết — Bảng ghi nhớ nhanh](#9-tổng-kết--bảng-ghi-nhớ-nhanh)
10. [Bài tập luyện tập](#10-bài-tập-luyện-tập)

---

## 1. Vấn đề trước khi có Generics

Trước Java 5, các Collection lưu trữ kiểu `Object` chung chung — mất an toàn kiểu dữ liệu (type safety), lỗi chỉ phát hiện lúc **runtime**:

```java
// Cách viết KIỂU CŨ (trước Java 5) — không nên dùng, chỉ để hiểu vấn đề
List list = new ArrayList(); // không khai báo kiểu phần tử
list.add("Hello");
list.add(123); // Compiler CHO PHÉP — vì list lưu Object, String hay Integer đều hợp lệ

String s = (String) list.get(1); // ép kiểu thủ công — CHẠY chương trình mới phát hiện lỗi!
// Exception in thread "main" java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String
```

**Generics giải quyết vấn đề này** bằng cách cho phép **tham số hóa kiểu dữ liệu (type parameter)** — compiler kiểm tra kiểu **ngay lúc biên dịch**, bắt lỗi sớm trước khi chương trình chạy:

```java
List<String> list = new ArrayList<>(); // khai báo RÕ RÀNG: chỉ chứa String
list.add("Hello");
// list.add(123); // ❌ Lỗi COMPILE ngay lập tức — không cần chờ đến runtime mới phát hiện

String s = list.get(1); // KHÔNG cần ép kiểu thủ công — compiler đã biết chắc chắn là String
```

---

## 2. Generic Class

Định nghĩa 1 class với **tham số kiểu (type parameter)** — thường ký hiệu bằng 1 chữ cái viết hoa (`T`, `E`, `K`, `V`...), sẽ được "thay thế" bằng kiểu cụ thể khi sử dụng.

```java
public class Box<T> { // T là type parameter — placeholder cho 1 kiểu bất kỳ
    private T content;

    public void set(T content) {
        this.content = content;
    }

    public T get() {
        return content;
    }
}
```

```java
Box<String> stringBox = new Box<>();
stringBox.set("Hello");
String value = stringBox.get(); // KHÔNG cần ép kiểu

Box<Integer> intBox = new Box<>();
intBox.set(100);
// intBox.set("text"); // ❌ Lỗi compile — Box<Integer> chỉ chấp nhận Integer
```

### Quy ước đặt tên type parameter (convention chuẩn JDK)

| Ký hiệu | Ý nghĩa quy ước |
|---|---|
| `T` | Type (kiểu dữ liệu chung chung) |
| `E` | Element (dùng trong Collection: `List<E>`) |
| `K`, `V` | Key, Value (dùng trong Map: `Map<K, V>`) |
| `N` | Number |
| `R` | Return type (thường dùng trong `Function<T, R>`) |

### Generic Class với nhiều type parameter

```java
public class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
}
```

```java
Pair<String, Integer> studentScore = new Pair<>("Pho", 95);
```

---

## 3. Generic Method

Một method **độc lập** có thể là generic **dù bản thân class chứa nó không phải generic** — type parameter được khai báo **trước kiểu trả về**:

```java
public class ArrayUtils {
    // <T> ngay trước kiểu trả về "void" — khai báo đây là generic method
    public static <T> void printArray(T[] array) {
        for (T item : array) {
            System.out.println(item);
        }
    }

    // Generic method có thể có nhiều type parameter, độc lập với class
    public static <T> T findFirst(List<T> list) {
        if (list.isEmpty()) return null;
        return list.get(0);
    }
}
```

```java
Integer[] numbers = {1, 2, 3};
ArrayUtils.printArray(numbers); // T được suy luận tự động là Integer

String[] words = {"a", "b", "c"};
ArrayUtils.printArray(words);   // T được suy luận tự động là String
```

> **Type Inference (suy luận kiểu):** compiler thường **tự động suy luận** `T` là gì dựa vào tham số truyền vào, không cần chỉ định tường minh. Vẫn có thể ghi tường minh nếu cần: `ArrayUtils.<String>printArray(words);` (hiếm khi cần thiết trong thực tế).

### Ví dụ thực tế: method generic so sánh 2 giá trị

```java
public static <T extends Comparable<T>> T max(T a, T b) { // xem mục 4 để hiểu "extends" ở đây
    return a.compareTo(b) > 0 ? a : b;
}
```

---

## 4. Bounded Type — `<T extends X>`

**Bounded Type** giới hạn `T` chỉ được là **X hoặc subclass/subtype của X** — cho phép gọi các method của `X` ngay trên biến kiểu `T` (điều mà Generic thông thường không cho phép, vì compiler mặc định coi `T` chỉ có các method của `Object`).

```java
// KHÔNG bounded — compiler KHÔNG biết T có method compareTo() hay không
public static <T> T max(T a, T b) {
    // return a.compareTo(b) > 0 ? a : b; // ❌ Lỗi compile — Object không có compareTo()
    return null;
}

// CÓ bounded — T PHẢI implement Comparable<T>, nên compiler BIẾT chắc T có compareTo()
public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) > 0 ? a : b; // ✅ Hợp lệ
}
```

```java
System.out.println(max(5, 10));          // 10 — Integer implements Comparable<Integer>
System.out.println(max("apple", "banana")); // banana — String implements Comparable<String>
```

> **Lưu ý cú pháp:** dù `X` là **interface**, từ khóa vẫn luôn là `extends` (không phải `implements`) trong ngữ cảnh bounded type — đây là quy tắc cú pháp riêng của Generics, hay bị nhầm lẫn.

### Bounded Type với class trừu tượng (giới hạn theo quan hệ kế thừa)

```java
public class NumberBox<T extends Number> { // T CHỈ ĐƯỢC LÀ Number hoặc subclass: Integer, Double, Long...
    private T value;
    public NumberBox(T value) { this.value = value; }

    public double doubleValue() {
        return value.doubleValue(); // gọi được method của Number vì compiler biết chắc T là Number
    }
}
```

```java
NumberBox<Integer> box1 = new NumberBox<>(10);   // ✅ hợp lệ
NumberBox<Double> box2 = new NumberBox<>(3.14);  // ✅ hợp lệ
// NumberBox<String> box3 = new NumberBox<>("abc"); // ❌ Lỗi compile — String không phải Number
```

### Multiple Bounds — giới hạn theo nhiều điều kiện cùng lúc

```java
public static <T extends Number & Comparable<T>> T max(T a, T b) {
    // T phải VỪA là Number, VỪA implement Comparable<T> — dùng "&" để nối nhiều điều kiện
    return a.compareTo(b) > 0 ? a : b;
}
```
> Nếu có nhiều bound, **tối đa 1 class** (phải đứng đầu tiên), các bound còn lại phải là **interface**.

---

## 5. Wildcard — `?`, `? extends`, `? super`

Wildcard dùng khi **khai báo tham số của method/biến**, không dùng khi định nghĩa class/method generic (khác với `T` ở các mục trên).

### `?` — Unbounded Wildcard (không giới hạn kiểu)

```java
public static void printList(List<?> list) { // chấp nhận List của BẤT KỲ kiểu nào
    for (Object item : list) { // chỉ có thể coi phần tử là Object — không biết chính xác kiểu gì
        System.out.println(item);
    }
}
```
```java
printList(List.of("a", "b"));      // ✅ hợp lệ
printList(List.of(1, 2, 3));       // ✅ hợp lệ
```
Dùng khi method chỉ cần **đọc** dữ liệu theo cách chung chung (`toString()`, `size()`...), không quan tâm kiểu cụ thể.

### `? extends X` — Upper Bounded Wildcard (chỉ ĐỌC, không GHI)

```java
public static double sumNumbers(List<? extends Number> list) {
    // list có thể là List<Integer>, List<Double>, List<Number>...
    double sum = 0;
    for (Number n : list) { // ĐỌC an toàn — chắc chắn mọi phần tử ít nhất là Number
        sum += n.doubleValue();
    }
    // list.add(10); // ❌ Lỗi compile — không biết chính xác list là List<Integer> hay List<Double>, thêm bậy sẽ phá vỡ type safety
    return sum;
}
```
```java
sumNumbers(List.of(1, 2, 3));       // List<Integer> — hợp lệ
sumNumbers(List.of(1.5, 2.5));      // List<Double> — hợp lệ
```

### `? super X` — Lower Bounded Wildcard (chỉ GHI, đọc ra chỉ chắc chắn là Object)

```java
public static void addNumbers(List<? super Integer> list) {
    // list có thể là List<Integer>, List<Number>, List<Object>...
    list.add(1); // ✅ GHI an toàn — mọi kiểu trong "list" đều là Integer hoặc kiểu cha của nó, nên thêm Integer luôn hợp lệ
    list.add(2);
    // Integer x = list.get(0); // ❌ Lỗi compile — không chắc chắn phần tử lấy ra là Integer (có thể list thực ra là List<Object>)
    Object x = list.get(0); // ✅ chỉ chắc chắn được kiểu Object
}
```
```java
List<Integer> ints = new ArrayList<>();
addNumbers(ints); // hợp lệ

List<Number> numbers = new ArrayList<>();
addNumbers(numbers); // cũng hợp lệ — Number là kiểu cha của Integer
```

---

## 6. PECS — Producer Extends, Consumer Super

Đây là **quy tắc ghi nhớ kinh điển** (do Joshua Bloch đề xuất trong cuốn "Effective Java") giúp quyết định dùng `extends` hay `super`:

> **PECS = Producer Extends, Consumer Super**

- Nếu tham số **"sản xuất" (Producer)** dữ liệu — tức là method sẽ **ĐỌC (get)** dữ liệu ra từ nó → dùng `? extends X`.
- Nếu tham số **"tiêu thụ" (Consumer)** dữ liệu — tức là method sẽ **GHI (add/put)** dữ liệu vào nó → dùng `? super X`.
- Nếu method **vừa đọc vừa ghi** → **không nên dùng wildcard**, dùng type parameter thông thường (`T`) là hợp lý hơn.

### Ví dụ kinh điển: method `copy()` trong `java.util.Collections`

```java
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (int i = 0; i < src.size(); i++) {
        dest.set(i, src.get(i));
    }
}
```
- `src` (nguồn — bị **đọc** ra, "sản xuất" dữ liệu để copy đi) → `? extends T` (Producer Extends).
- `dest` (đích — bị **ghi** vào, "tiêu thụ" dữ liệu được copy đến) → `? super T` (Consumer Super).

```java
List<Integer> ints = List.of(1, 2, 3);
List<Number> numbers = new ArrayList<>(List.of(0, 0, 0));
Collections.copy(numbers, ints); // src là List<Integer> (extends Number), dest là List<Number> (super Integer) — hợp lệ
```

> **Mẹo ghi nhớ nhanh khi đọc code người khác:** thấy `? extends` → có thể an tâm **lấy dữ liệu ra dùng**; thấy `? super` → có thể an tâm **đưa dữ liệu vào**; thấy `?` trơn → chỉ nên coi là `Object` để đọc, không ghi được gì có ý nghĩa.

---

## 7. Type Erasure — cơ chế Generics hoạt động thế nào lúc runtime

Đây là kiến thức **nâng cao nhưng quan trọng** để hiểu vì sao Generics trong Java có một số hạn chế kỳ lạ.

**Type Erasure** nghĩa là: thông tin type parameter (`T`, `E`...) **chỉ tồn tại lúc compile-time**, để compiler kiểm tra kiểu — nhưng **bị "xóa" (erase)** khi biên dịch ra bytecode, thay bằng `Object` (hoặc bound cụ thể nếu có `extends`).

```java
List<String> stringList = new ArrayList<>();
List<Integer> intList = new ArrayList<>();

System.out.println(stringList.getClass() == intList.getClass()); // TRUE!
// Vì lúc runtime, CẢ 2 đều chỉ là "ArrayList" thuần túy — thông tin <String> và <Integer> đã bị xóa
```

### Hệ quả thực tế của Type Erasure — những điều KHÔNG làm được với Generics

```java
public class Box<T> {
    // private T[] items = new T[10];       // ❌ Không thể tạo mảng generic trực tiếp
    // if (obj instanceof T) { }              // ❌ Không thể dùng instanceof với type parameter
    // T instance = new T();                  // ❌ Không thể new T() trực tiếp (không biết T là gì lúc runtime)

    public static <T> void method(T t) { }
    // public static <T> void method(List<T> list) { } // ❌ Lỗi "erasure of method is same" nếu overload cả 2 — sau erasure cả 2 đều thành method(Object)
}
```

> Không cần nhớ chi tiết cách khắc phục từng trường hợp trên (khá hiếm gặp trong code backend nghiệp vụ thông thường) — điều quan trọng là **hiểu khái niệm Type Erasure tồn tại**, để không bất ngờ khi gặp các lỗi compile "khó hiểu" liên quan đến Generics, và để trả lời được câu hỏi phỏng vấn "Generics trong Java có thực sự tồn tại lúc runtime không?" (**Câu trả lời: KHÔNG** — đây khác với một số ngôn ngữ khác như C# có "reified generics" giữ nguyên thông tin kiểu lúc runtime).

---

## 8. Generics trong thực tế Spring/JDK

Hiểu Generics giúp đọc hiểu ngay các signature quen thuộc sẽ gặp xuyên suốt lộ trình:

```java
// Spring Data JPA (Module 14) — T là kiểu Entity, ID là kiểu khóa chính
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID> { }

// Cách dùng cụ thể:
public interface UserRepository extends JpaRepository<User, Long> { }
// → T = User, ID = Long

// Spring Web (Module 16) — bọc response REST API với generic type
public class ResponseEntity<T> { }
ResponseEntity<UserDTO> response = ResponseEntity.ok(userDTO);

// Optional (Module 06) — bọc giá trị có thể null một cách an toàn kiểu
Optional<User> user = userRepository.findById(1L);

// Functional Interface (Module 06) — hầu hết đều dùng Generics
Function<String, Integer> parseLength = String::length; // Function<T, R>
```

Nếu không nắm chắc Generics, những dòng code trên sẽ chỉ là "học thuộc cú pháp" — nắm chắc rồi thì **tự suy luận được ý nghĩa** ngay khi gặp bất kỳ class generic mới nào trong tài liệu Spring.

---

## 9. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| Generics ra đời để giải quyết | Type safety lúc compile-time, tránh `ClassCastException` lúc runtime |
| Generic Class | `class Box<T> { }` — type parameter gắn với cả class |
| Generic Method | `<T> T method(T param)` — type parameter độc lập, khai báo trước kiểu trả về |
| Bounded Type `<T extends X>` | Giới hạn T là X hoặc subtype — cho phép gọi method của X trên biến kiểu T |
| `? extends X` | Chỉ ĐỌC an toàn (Producer) — không ghi được vì không rõ kiểu con cụ thể |
| `? super X` | Chỉ GHI an toàn (Consumer) — đọc ra chỉ chắc chắn là Object |
| PECS | Producer Extends, Consumer Super — quy tắc ghi nhớ khi chọn wildcard |
| Type Erasure | Type parameter bị xóa lúc runtime, chỉ tồn tại lúc compile-time để kiểm tra kiểu |
| Ứng dụng thực tế | `JpaRepository<T, ID>`, `ResponseEntity<T>`, `Optional<T>` — nền tảng đọc hiểu code Spring |

---

## 10. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau có lỗi compile không? Giải thích.
```java
public class Container<T> {
    private T item;
    public boolean isSameType(T other) {
        return item.getClass() == other.getClass();
    }
}
```

**Câu 2.** Method sau thiếu gì để compile được? Sửa lại đúng cú pháp.
```java
public static T findMax(List<T> list) {
    T max = list.get(0);
    for (T item : list) {
        if (item.compareTo(max) > 0) max = item;
    }
    return max;
}
```

**Câu 3.** Đoạn code sau có compile được không? Giải thích theo cơ chế wildcard.
```java
public static void addAnimal(List<? extends Animal> animals) {
    animals.add(new Dog()); // ?
}
```

**Câu 4.** Áp dụng PECS, chọn `extends` hay `super` cho tham số sau, giải thích lý do:
```java
public static <T> void fillList(List<? ??? T> list, T value, int count) {
    for (int i = 0; i < count; i++) {
        list.add(value); // list bị GHI vào
    }
}
```

**Câu 5.** Vì sao đoạn code sau gây lỗi compile? Liên hệ với khái niệm Type Erasure.
```java
public class Box<T> {
    public boolean isInstance(Object obj) {
        return obj instanceof T; // ?
    }
}
```

---

### Phần B — Bài tập viết code

**Bài 1 — Generic Class `Pair<K, V>` hoàn chỉnh.**
Viết lại class `Pair<K, V>` với constructor, getter, và override `equals()`/`hashCode()`/`toString()` đúng chuẩn (liên hệ Module 02.4 — dựa trên cả `key` và `value`). Viết `main` tạo vài `Pair<String, Integer>`, chứng minh `equals()` hoạt động đúng khi so sánh 2 Pair có nội dung giống nhau.

**Bài 2 — Generic Method giới hạn (Bounded Type).**
Viết method generic `<T extends Comparable<T>> T findMin(List<T> list)` tìm phần tử nhỏ nhất trong danh sách bất kỳ (miễn kiểu phần tử có implement `Comparable`). Thử gọi với `List<Integer>`, `List<String>`, `List<Double>` để chứng minh tính tái sử dụng của generic method.

**Bài 3 — Áp dụng Wildcard đúng theo PECS.**
Viết method `static double sumAll(List<? extends Number> numbers)` tính tổng danh sách bất kỳ kiểu con nào của `Number`. Viết thêm method `static void fillWithZero(List<? super Integer> list, int count)` thêm `count` số 0 vào danh sách. Viết `main` thử gọi cả 2 method với `List<Integer>`, `List<Double>`, `List<Number>` để kiểm chứng tính linh hoạt.

**Bài 4 — Generic Stack tự cài đặt (dùng ArrayDeque nội bộ).**
Viết class `MyStack<T>` với các method `push(T item)`, `pop()`, `peek()`, `isEmpty()`, dùng `ArrayDeque<T>` làm cấu trúc lưu trữ bên trong (liên hệ Module 03.1). Đảm bảo `pop()` khi Stack rỗng ném ra `NoSuchElementException` với message rõ ràng thay vì để lỗi mặc định khó hiểu.

**Bài 5 — Bài toán tổng hợp: Generic Repository đơn giản (chuẩn bị tư duy cho Spring Data JPA).**
Viết `interface Repository<T, ID>` với các method: `T save(T entity)`, `Optional<T> findById(ID id)`, `List<T> findAll()`, `void deleteById(ID id)`. Cài đặt `class InMemoryUserRepository implements Repository<User, Long>` dùng `Map<Long, User>` làm nơi lưu trữ tạm trong bộ nhớ (giả lập database). Đây chính là mô hình thu nhỏ của `JpaRepository<T, ID>` sẽ gặp lại ở Module 14 — hoàn thành tốt bài này nghĩa là đã sẵn sàng về mặt tư duy generic cho phần đó.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Không lỗi** — vì `getClass()` là method của `Object`, mọi kiểu `T` (dù chưa bounded) đều thừa hưởng được từ `Object`. Đoạn code này hợp lệ.
2. Thiếu khai báo type parameter `<T extends Comparable<T>>` trước kiểu trả về, và thiếu ở tên method. Sửa:
```java
public static <T extends Comparable<T>> T findMax(List<T> list) {
    T max = list.get(0);
    for (T item : list) {
        if (item.compareTo(max) > 0) max = item;
    }
    return max;
}
```
3. **Không compile được** — vì `List<? extends Animal>` chỉ cho phép **đọc** (Producer), compiler không biết chính xác `animals` là `List<Dog>`, `List<Cat>`, hay `List<Animal>` — nếu cho phép `add(new Dog())` mà thực ra `animals` là `List<Cat>`, sẽ phá vỡ type safety. Đây chính là lý do quy tắc PECS tồn tại.
4. Dùng `super`: `List<? super T>` — vì `list` đóng vai trò **Consumer** (bị ghi giá trị `value` vào), áp dụng đúng "Consumer Super" trong PECS.
5. Lỗi vì **Type Erasure** — tại runtime, thông tin `T` cụ thể là gì đã bị xóa (chỉ còn là `Object` sau khi biên dịch), nên JVM **không có cách nào biết** `obj` có phải kiểu `T` hay không tại thời điểm chạy — Java cấm dùng `instanceof` trực tiếp với type parameter vì lý do này.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `equals()` nên kiểm tra cả `key` và `value` bằng `Objects.equals()`; `hashCode()` dùng `Objects.hash(key, value)` — áp dụng đúng nguyên tắc đã học ở Module 02.4, chỉ khác là bây giờ field có kiểu generic `K`/`V` thay vì kiểu cụ thể.
- **Bài 3:** Kết quả mong đợi: cả `sumAll()` lẫn `fillWithZero()` đều hoạt động linh hoạt với nhiều kiểu `List` khác nhau — đây chính là minh chứng thực tế cho lợi ích của wildcard so với việc phải viết nhiều overload riêng cho từng kiểu cụ thể (`sumAllInt`, `sumAllDouble`...).
- **Bài 4:** Vì `ArrayDeque` không có sẵn method `peek()` ném exception khi rỗng (nó trả về `null`), cần tự kiểm tra `isEmpty()` trước và chủ động `throw new NoSuchElementException("Stack đang rỗng")` để có thông báo lỗi rõ ràng, dễ debug hơn so với việc để `null` lan truyền âm thầm rồi gây `NullPointerException` ở một chỗ khác xa nguồn gốc lỗi thực sự.
- **Bài 5:** Đây là bài tập quan trọng nhất của module — khi hoàn thành, hãy so sánh lại `interface Repository<T, ID>` bạn vừa viết với `interface JpaRepository<T, ID>` thực tế của Spring Data (sẽ gặp ở Module 14) để thấy rõ chúng có cấu trúc tư duy **gần như giống hệt nhau**, chỉ khác là Spring Data đã cài đặt sẵn phần kết nối database thực sự thay vì `Map` giả lập trong bộ nhớ.

</details>

---

*File tiếp theo trong lộ trình: **Module 03.3 — Stream API & Lambda** (map/filter/reduce/collect, method reference, functional interface).*
