# Module 03.2 — Generics

> **Mức độ ưu tiên: Cao** — Generics là kiến thức **bắt buộc** để đọc hiểu source Spring/Hibernate (`JpaRepository<T, ID>`, `ResponseEntity<T>`, `Comparator<? super T>`...). Không nắm wildcard (`? extends`/`? super`) thì signature của method thư viện trở nên khó hiểu, và rất dễ viết code generic sai theo cách compiler cảnh báo mờ nhạt (`unchecked warning`) rồi nổ `ClassCastException` ở chỗ khác.

> **Phạm vi bài này:** cơ chế generic của Java — generic class/method, bounded type, tính bất biến (invariance), wildcard, quy tắc PECS, type erasure và các hệ quả của nó. **Chỉ nhắc tên, không đi sâu:** Stream/lambda/functional interface (Module 03.3), Spring Data JPA (Module 14), annotation processing. Các ví dụ Spring/JDK ở mục 10 chỉ để *đọc hiểu signature*, không phải để học framework.

---

## Mục lục

1. [Vấn đề trước khi có Generics — raw type & unchecked warning](#1-vấn-đề-trước-khi-có-generics--raw-type--unchecked-warning)
2. [Generic Class](#2-generic-class)
3. [Generic Method & suy luận kiểu](#3-generic-method--suy-luận-kiểu)
4. [Bounded Type — `<T extends X>`](#4-bounded-type--t-extends-x)
5. [Tính bất biến (invariance) & quan hệ với mảng](#5-tính-bất-biến-invariance--quan-hệ-với-mảng)
6. [Wildcard — `?`, `? extends`, `? super` & capture](#6-wildcard---extends-super--capture)
7. [PECS — Producer Extends, Consumer Super](#7-pecs--producer-extends-consumer-super)
8. [Type Erasure — Generics lúc runtime](#8-type-erasure--generics-lúc-runtime)
9. [Hệ quả của Erasure & cách lách](#9-hệ-quả-của-erasure--cách-lách)
10. [Generics trong Spring/JDK thực tế](#10-generics-trong-springjdk-thực-tế)
11. [Bẫy thường gặp](#11-bẫy-thường-gặp)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. Vấn đề trước khi có Generics — raw type & unchecked warning

Trước Java 5, Collection lưu `Object` — mất an toàn kiểu, lỗi chỉ lộ lúc **runtime**:

```java
List list = new ArrayList();     // "raw type" — không khai báo kiểu phần tử
list.add("Hello");
list.add(123);                   // compiler CHO PHÉP — cái gì cũng là Object

String s = (String) list.get(1); // ép kiểu thủ công → ClassCastException lúc CHẠY
```

Generics **tham số hóa kiểu (type parameter)** để compiler kiểm tra **lúc biên dịch**:

```java
List<String> list = new ArrayList<>();   // chỉ chứa String
list.add("Hello");
// list.add(123);                        // ❌ lỗi COMPILE ngay
String s = list.get(1);                  // KHÔNG cần ép kiểu
```

### Raw type vẫn tồn tại — và vì sao nên tránh

Java giữ raw type để **tương thích ngược** với code trước Java 5. Nhưng dùng raw type làm **tắt toàn bộ kiểm tra generic** cho biến đó:

```java
List<String> strings = new ArrayList<>();
List raw = strings;              // hợp lệ (chỉ là warning)
raw.add(42);                     // "unchecked call" — compiler chỉ CẢNH BÁO, không chặn
String s = strings.get(0);       // 💥 ClassCastException — Integer 42 chui vào List<String>
```

Hiện tượng "kiểu sai lọt được vào collection do bỏ qua kiểm tra generic" gọi là **heap pollution**.

- `List` (raw) ≠ `List<Object>` ≠ `List<?>`. `List<Object>` vẫn được kiểm tra kiểu đầy đủ; `List<?>` an toàn nhưng không cho ghi (mục 6); chỉ raw `List` mới tắt kiểm tra.
- Gặp *unchecked warning* → sửa cho hết, đừng bỏ qua. Khi *chắc chắn* an toàn mà không diễn đạt được cho compiler → `@SuppressWarnings("unchecked")` đặt ở **phạm vi hẹp nhất có thể** (một biến local, không phải cả method) kèm comment giải thích vì sao an toàn.

---

## 2. Generic Class

Class có **tham số kiểu** — thường một chữ hoa (`T`, `E`, `K`, `V`) — thay bằng kiểu cụ thể khi dùng:

```java
public class Box<T> {
    private T content;
    public void set(T content) { this.content = content; }
    public T get() { return content; }
}
```

```java
Box<String> sb = new Box<>();
sb.set("Hello");
String v = sb.get();                 // không ép kiểu
Box<Integer> ib = new Box<>();
// ib.set("text");                    // ❌ compile
```

### Quy ước tên type parameter (JDK convention)

| Ký hiệu | Quy ước |
|---|---|
| `T` | Type chung |
| `E` | Element (`List<E>`, `Set<E>`) |
| `K`, `V` | Key, Value (`Map<K, V>`) |
| `N` | Number |
| `R` | Result / Return (`Function<T, R>`) |
| `S`, `U`, `V` | type parameter thứ 2, 3, 4 |

### Nhiều type parameter

```java
public class Pair<K, V> {
    private final K key;
    private final V value;
    public Pair(K key, V value) { this.key = key; this.value = value; }
    public K getKey()   { return key; }
    public V getValue() { return value; }
}
Pair<String, Integer> p = new Pair<>("Pho", 95);
```

### Bốn giới hạn của type parameter trong class

```java
public class Box<T> {

    // 1. KHÔNG dùng T ở ngữ cảnh static — T gắn với INSTANCE, static thì không có instance
    // static T shared;
    // static T identity(T t) { return t; }

    // Nhưng static method được có type parameter RIÊNG của nó:
    static <U> U echo(U u) { return u; }

    // 2. KHÔNG new T() — lúc runtime không biết T là gì (mục 8). Muốn tạo → nhận Supplier<T> hoặc Class<T> (mục 9)
    // T make() { return new T(); }

    // 3. KHÔNG new T[n] — không tạo mảng generic trực tiếp (mục 9)
    // T[] arr = new T[10];

    // 4. T KHÔNG nhận primitive — Box<int> sai, phải Box<Integer> (kèm chi phí autoboxing)
}
```

> Constructor **không** viết `Box<T>()` — chỉ `Box()`. Type parameter `<T>` khai báo ở tên class là đủ; constructor dùng lại `T` đó. (Constructor *có thể* khai báo type parameter riêng, hiếm dùng.)

---

## 3. Generic Method & suy luận kiểu

Một method có thể generic **dù class chứa nó không generic** — type parameter khai báo **ngay trước kiểu trả về**:

```java
public class ArrayUtils {
    public static <T> void printAll(T[] array) {
        for (T item : array) System.out.println(item);
    }
    public static <T> T firstOrNull(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }
    // nhiều type parameter, độc lập:
    public static <K, V> Pair<V, K> swap(Pair<K, V> p) {
        return new Pair<>(p.getValue(), p.getKey());
    }
}
```

### Suy luận kiểu (type inference)

```java
Integer[] nums = {1, 2, 3};
ArrayUtils.printAll(nums);              // T suy ra Integer từ tham số

List<String> empty = Collections.emptyList();          // T suy ra String từ kiểu biến đích (target typing)
Map<String, List<Integer>> m = new HashMap<>();        // diamond <> — Java 7+
var list = new ArrayList<String>();                    // var: kiểu là ArrayList<String>

// Chỉ định tường minh (type witness) — hiếm cần, dùng khi suy luận thất bại:
List<String> e2 = Collections.<String>emptyList();
ArrayUtils.<String>printAll(new String[]{"a"});
```

### Khi nào cần generic method thay vì chỉ dùng type parameter của class?

- Method **tĩnh** (không có instance → không thấy `T` của class).
- Quan hệ kiểu **chỉ nằm trong phạm vi method đó** — ví dụ `<T> T firstOrNull(List<T>)`: `T` chỉ liên kết tham số với kiểu trả về, không cần lưu vào class.
- Cần **ràng buộc giữa nhiều tham số** cho riêng một lời gọi: `<T> void copy(List<? super T> dst, List<? extends T> src)`.

---

## 4. Bounded Type — `<T extends X>`

Giới hạn `T` là **X hoặc subtype của X** → cho phép gọi method của `X` trên biến kiểu `T` (mặc định compiler chỉ cho `T` các method của `Object`):

```java
// KHÔNG bounded — compiler không biết T có compareTo()
public static <T> T max(T a, T b) {
    // return a.compareTo(b) > 0 ? a : b;   // ❌ Object không có compareTo()
    return null;
}
// CÓ bounded — compiler BIẾT T có compareTo()
public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) > 0 ? a : b;      // ✅
}
max(5, 10);              // 10
max("apple", "banana");  // "banana"
```

> **Cú pháp:** dù `X` là **interface**, vẫn viết `extends` (không phải `implements`) trong bounded type — quy tắc riêng của Generics, hay bị nhầm.

### Bound theo class

```java
public class NumberBox<T extends Number> {          // Integer, Double, Long, BigDecimal...
    private final T value;
    public NumberBox(T value) { this.value = value; }
    public double asDouble() { return value.doubleValue(); }   // gọi được method của Number
}
// NumberBox<String> x;    // ❌ String không phải Number
```

### Multiple bounds — `&`

```java
public static <T extends Number & Comparable<T>> T clamp(T v, T lo, T hi) {
    if (v.compareTo(lo) < 0) return lo;
    if (v.compareTo(hi) > 0) return hi;
    return v;
}
```

Quy tắc: nếu có nhiều bound thì **tối đa một class**, và class đó phải **đứng đầu**; phần còn lại là interface.

### Recursive bound — `<T extends Comparable<? super T>>`

Kiểu ràng buộc "T so sánh được với chính nó (hoặc lớp cha của nó)" xuất hiện khắp JDK. Signature thật của `Collections.max`:

```java
public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)
```

`Comparable<? super T>` (thay vì `Comparable<T>`) cho phép `T` **kế thừa** khả năng so sánh từ lớp cha — ví dụ `class Manager extends Employee` mà chỉ `Employee implements Comparable<Employee>` thì `max(List<Manager>)` vẫn hợp lệ.

---

## 5. Tính bất biến (invariance) & quan hệ với mảng

Điểm khiến người mới bối rối nhất:

```java
List<Integer> ints = new ArrayList<>();
// List<Number> nums = ints;        // ❌ LỖI COMPILE — dù Integer LÀ Number
// List<Object> objs = ints;        // ❌ cũng lỗi
```

> **Generic là *bất biến* (invariant):** `List<Integer>` **không phải** là con của `List<Number>`, dù `Integer` là con của `Number`. `List<A>` và `List<B>` không có quan hệ cha–con nào trừ khi `A` và `B` giống hệt nhau.

### Vì sao phải bất biến? Đối chiếu với mảng (Module 01.1)

Mảng thì **hiệp biến (covariant)** — và đó chính là *lỗ hổng*:

```java
Integer[] ia = {1, 2, 3};
Number[]  na = ia;             // hợp lệ — mảng covariant
na[0] = 3.14;                  // biên dịch OK, nhưng 💥 ArrayStoreException lúc RUNTIME
```

Nếu generic cũng covariant thì đoạn sau sẽ lọt qua compiler và hỏng **âm thầm** (không có `ArrayStoreException` vì erasure — mục 8):

```java
List<Integer> ints = new ArrayList<>();
List<Number> nums = ints;      // GIẢ SỬ hợp lệ...
nums.add(3.14);                // ...thì Double lọt vào List<Integer>
int x = ints.get(0);          // 💥 ClassCastException ở NƠI KHÁC, khó lần
```

→ Java chọn cấm ở bước gán. Muốn "một list của kiểu nào đó là con của Number" → dùng **wildcard**.

---

## 6. Wildcard — `?`, `? extends`, `? super` & capture

Wildcard dùng khi **khai báo kiểu tham số/biến**, không dùng khi *định nghĩa* class/method generic.

### `?` — unbounded: "List của kiểu nào đó, không rõ"

```java
public static void printSize(List<?> list) {
    System.out.println(list.size());     // OK — thao tác không phụ thuộc kiểu phần tử
    for (Object o : list) System.out.println(o);   // đọc ra chỉ chắc là Object
    // list.add("x");                     // ❌ không ghi được (trừ null)
}
printSize(List.of("a", "b"));   printSize(List.of(1, 2));
```

`List<?>` khác raw `List`: `List<?>` **vẫn an toàn kiểu** — compiler chặn mọi thao tác ghi có thể phá vỡ; raw `List` thì không.

### `? extends X` — upper bound: ĐỌC được, KHÔNG ghi

```java
public static double sum(List<? extends Number> list) {
    double s = 0;
    for (Number n : list) s += n.doubleValue();    // đọc: chắc chắn ≥ Number
    // list.add(1);                                 // ❌ không biết list là List<Integer> hay List<Double>
    return s;
}
sum(List.of(1, 2, 3));        // List<Integer>
sum(List.of(1.5, 2.5));       // List<Double>
```

### `? super X` — lower bound: GHI được, đọc ra chỉ là Object

```java
public static void addInts(List<? super Integer> list) {
    list.add(1); list.add(2);          // ghi: mọi kiểu của list đều là Integer hoặc cha → nhận Integer OK
    // Integer i = list.get(0);         // ❌ list có thể là List<Object>
    Object o = list.get(0);            // chỉ chắc Object
}
addInts(new ArrayList<Integer>());
addInts(new ArrayList<Number>());
addInts(new ArrayList<Object>());
```

### Không khởi tạo được wildcard; capture

```java
// new ArrayList<? extends Number>()    // ❌ vô nghĩa — kiểu phần tử phải xác định
List<? extends Number> l = new ArrayList<Integer>();   // ✅ biến thì được
```

Khi compiler gặp `?`, nó gán một **kiểu bắt được (captured type)** tạm gọi `CAP#1`. Lỗi kiểu `capture of ?` xuất hiện khi bạn cố dùng hai `?` như thể chúng là **cùng** một kiểu. Cách lách: **capture helper** — một private generic method đặt tên được cho kiểu:

```java
public static void swap(List<?> list, int i, int j) {   // API công khai: gọn
    swapHelper(list, i, j);
}
private static <E> void swapHelper(List<E> list, int i, int j) {   // E "bắt" cái ?
    E tmp = list.get(i);
    list.set(i, list.set(j, tmp));
}
```

---

## 7. PECS — Producer Extends, Consumer Super

Quy tắc kinh điển (Joshua Bloch, *Effective Java*) để chọn `extends` hay `super`:

> **PECS = Producer Extends, Consumer Super**

| Vai trò tham số | Method làm gì với nó | Wildcard |
|---|---|---|
| **Producer** — nguồn dữ liệu | **đọc / lấy ra** (`get`, duyệt) | `? extends X` |
| **Consumer** — nơi nhận dữ liệu | **ghi / bỏ vào** (`add`, `set`) | `? super X` |
| Vừa đọc vừa ghi | cả hai | **không dùng wildcard** — dùng `T` |

### Ví dụ: `Collections.copy`

```java
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (int i = 0; i < src.size(); i++) dest.set(i, src.get(i));
}
```

- `src` bị **đọc** → Producer → `? extends T`.
- `dest` bị **ghi** → Consumer → `? super T`.

```java
List<Integer> src = List.of(1, 2, 3);
List<Number>  dst = new ArrayList<>(List.of(0, 0, 0));
Collections.copy(dst, src);      // src: extends Number ✓   dst: super Integer ✓
```

### PECS ở khắp JDK

```java
Stream<T>.forEach(Consumer<? super T> action)              // consumer → super
boolean Collection<E>.addAll(Collection<? extends E> c)    // c là producer → extends
void List<E>.sort(Comparator<? super E> c)                 // comparator "tiêu thụ" phần tử → super
static <T> T Collections.max(Collection<? extends T> coll) // coll là producer → extends
Optional<T>.ifPresent(Consumer<? super T>)                 // super
```

> **Quy tắc kèm theo:** **đừng dùng wildcard ở kiểu trả về.** `List<? extends Number> load()` ép mọi nơi gọi phải xử lý wildcard. Trả `List<Number>` hoặc `List<Integer>` cụ thể.

> **Mẹo đọc code:** thấy `? extends` → yên tâm **lấy ra dùng**; thấy `? super` → yên tâm **bỏ vào**; thấy `?` trơn → coi như `Object`, chỉ đọc.

---

## 8. Type Erasure — Generics lúc runtime

**Type erasure:** thông tin type parameter (`T`, `E`...) **chỉ tồn tại lúc compile** để compiler kiểm tra kiểu; khi ra bytecode nó bị **xóa** — `T` không bound → thay bằng `Object`; `T extends Number` → thay bằng `Number`. Compiler tự chèn **cast** ở chỗ cần.

```java
List<String> a = new ArrayList<>();
List<Integer> b = new ArrayList<>();
a.getClass() == b.getClass();     // TRUE — cả hai chỉ là "ArrayList" lúc runtime

// Nguồn:
class Box<T> { T v; T get() { return v; } }
// Sau erasure (khái niệm):
class Box { Object v; Object get() { return v; } }
```

### Bridge method

Khi generic gặp override, compiler sinh thêm **method cầu nối** để giữ tính đa hình sau erasure:

```java
class MyInt implements Comparable<MyInt> {
    public int compareTo(MyInt o) { ... }
}
// Compiler sinh thêm:
// public int compareTo(Object o) { return compareTo((MyInt) o); }   // bridge — cầu nối tới bản thật
```

Vì vậy stack trace đôi khi hiện `compareTo(Object)` hoặc dòng "synthetic".

### Câu phỏng vấn kinh điển

> "Generics trong Java có tồn tại lúc runtime không?" → **KHÔNG.** Java dùng erasure (để tương thích ngược với bytecode trước Java 5). Khác với C#/Kotlin-`reified`, nơi generic được *reified* (giữ nguyên kiểu lúc runtime).

**Cái *được* reify trong Java:** mảng, raw type, và unbounded wildcard `List<?>`. **Không reify:** `List<String>`, `T`, `Pair<K,V>`.

---

## 9. Hệ quả của Erasure & cách lách

| Không làm được | Vì sao | Cách lách |
|---|---|---|
| `new T()` | Runtime không biết T | Nhận `Supplier<T>` hoặc `Class<T>` rồi `cls.getDeclaredConstructor().newInstance()` |
| `new T[n]` | Không tạo được mảng của kiểu đã xóa | `(T[]) new Object[n]` + `@SuppressWarnings("unchecked")`, **không lộ** ra ngoài dưới kiểu `T[]`; hoặc `Array.newInstance(cls, n)` |
| `obj instanceof T` / `catch (T e)` | Không có kiểu để kiểm tra lúc runtime | `instanceof` với kiểu cụ thể; hoặc `cls.isInstance(obj)` |
| `T.class`, `List<String>.class` | Không có `Class` cho kiểu đã xóa | Chỉ `List.class`; truyền `Class<T>` làm tham số |
| `static` field kiểu `T` | `T` gắn với instance | Type parameter riêng cho method, hoặc thiết kế lại |
| Hai overload `f(List<String>)` & `f(List<Integer>)` | Sau erasure đều là `f(List)` — "same erasure" | Đổi tên method |
| `class X extends Throwable<T>` | Exception phải reify để `catch` khớp | Không cho phép — exception không generic được |

### `Class<T>` token — mẫu phổ biến nhất

```java
public static <T> T fromJson(String json, Class<T> type) {
    // ... parse rồi type.cast(result)
}
User u = fromJson(body, User.class);      // T suy ra User; type.cast an toàn

public static <T> T create(Class<T> cls) throws ReflectiveOperationException {
    return cls.getDeclaredConstructor().newInstance();
}
```

Hạn chế: `Class<T>` không mang được kiểu **tham số hóa** (`List<User>.class` không tồn tại). Cần điều đó → *super type token*: Gson `TypeToken<List<User>>`, Spring `ParameterizedTypeReference<List<User>>` (khai thác việc kiểu của **lớp cha** trong anonymous class *được* giữ trong metadata).

### Generic array & `toArray`

```java
// Mẫu chuẩn JDK — nhận sẵn mảng đúng kiểu từ caller:
<T> T[] toArray(T[] a)      // list.toArray(new String[0])

// Tự tạo trong class, KHÔNG trả ra ngoài dưới kiểu T[]:
@SuppressWarnings("unchecked")
private final T[] buf = (T[]) new Object[16];   // OK khi buf chỉ dùng nội bộ
```

### Generics + varargs → `@SafeVarargs`

```java
@SafeVarargs                                   // "tôi cam kết không gây heap pollution"
static <T> List<T> listOf(T... items) {        // varargs generic tạo mảng T[] ẩn → warning
    return new ArrayList<>(Arrays.asList(items));
}
```

Chỉ đặt `@SafeVarargs` khi method **chỉ đọc** mảng varargs, **không** ghi vào nó và **không** trả nó ra ngoài.

---

## 10. Generics trong Spring/JDK thực tế

```java
// Spring Data JPA (Module 14) — T = Entity, ID = kiểu khóa chính
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID> { }
public interface UserRepository extends JpaRepository<User, Long> { }   // T=User, ID=Long

// Spring Web (Module 16)
ResponseEntity<UserDTO> res = ResponseEntity.ok(dto);

// java.util
Optional<User> u = repo.findById(1L);
Comparator<Employee> byName = Comparator.comparing(Employee::name);   // sort nhận Comparator<? super E>

// java.util.function (Module 03.3)
Function<String, Integer> len = String::length;      // Function<T, R>

// Super type token — lấy về List<User> qua REST, giữ được kiểu tham số hóa
ParameterizedTypeReference<List<User>> ref = new ParameterizedTypeReference<>() {};
```

Nắm generic thì các signature này **tự giải nghĩa** thay vì phải học thuộc.

---

## 11. Bẫy thường gặp

### 11.1. Trộn raw type làm mất kiểm tra kiểu
`List raw = genericList; raw.add(saiKiểu);` → chỉ warning, nổ ở chỗ khác. Không dùng raw type trong code mới.

### 11.2. Tưởng `List<Object>` nhận được `List<String>`
Không — invariance (mục 5). Tham số "list đọc bất kỳ" phải là `List<?>` hoặc `List<? extends Object>`.

### 11.3. `? extends` rồi cố `add`
`List<? extends Number>` không `add(1)` được. Cần ghi → `? super`, hoặc dùng `T`.

### 11.4. Wildcard ở kiểu trả về
`Map<String, ? extends Number> getConfig()` khiến caller vướng wildcard. Trả kiểu cụ thể.

### 11.5. So sánh `Class` mà quên erasure
`list.getClass() == ArrayList.class` đúng, nhưng không có cách phân biệt `ArrayList<String>` với `ArrayList<Integer>` lúc runtime.

### 11.6. `new` mảng generic hoặc `T[]` lộ ra ngoài
`(T[]) new Object[n]` trả ra dưới kiểu `T[]` → caller nhận `String[] s = ...` → `ClassCastException` (mảng thật là `Object[]`). Giữ nội bộ, hoặc dùng `toArray(T[])`.

### 11.7. Autoboxing ẩn khi `T` là wrapper
`List<Integer>` trong vòng lặp nóng → mỗi `add(int)`/`get()` box/unbox. Cần hiệu năng cao trên số nguyên thô → mảng primitive, không generic.

### 11.8. `Collections.emptyList()` vào chỗ cần kiểu cụ thể
Đôi khi suy luận ra `List<Object>`. Dùng type witness `Collections.<String>emptyList()` hoặc gán vào biến có kiểu rõ.

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Generics giải quyết | Type safety **lúc compile**, hết ép kiểu thủ công, hết `ClassCastException` bất ngờ |
| Raw type | Còn tồn tại vì tương thích ngược; dùng = **tắt kiểm tra generic** → heap pollution. Tránh. |
| Generic class | `class Box<T>` — `T` gắn **instance**: không `static T`, không `new T()`, không `new T[]`, không primitive |
| Generic method | `<T> T m(T x)` — `T` khai báo **trước kiểu trả về**; suy luận kiểu + diamond `<>`; type witness `Foo.<String>m()` khi cần |
| Bounded `<T extends X>` | `T` là X/subtype → gọi được method của X. Interface vẫn viết `extends`. Nhiều bound: `A & B & C`, class đứng đầu |
| Recursive bound | `<T extends Comparable<? super T>>` — cho phép kế thừa khả năng so sánh từ lớp cha (xem `Collections.max`) |
| Invariance | `List<Integer>` **không** là `List<Number>`. Khác mảng (covariant → `ArrayStoreException`). Cần quan hệ cha–con → wildcard |
| `?` | "kiểu nào đó" — an toàn hơn raw; đọc ra `Object`, không ghi (trừ `null`) |
| `? extends X` | **Producer** — đọc ra ≥ X; **không** ghi |
| `? super X` | **Consumer** — ghi X vào; đọc ra chỉ `Object` |
| PECS | Producer Extends, Consumer Super. Vừa đọc vừa ghi → `T`. Đừng trả wildcard ra ngoài |
| Capture (`CAP#1`) | Compiler đặt tên tạm cho `?`; lỗi "capture of ?" → tách **capture helper** `private <E> ...` |
| Type erasure | `T` bị xóa lúc runtime (→ `Object`/bound). `List<String>` và `List<Integer>` cùng `getClass()`. Bridge method giữ đa hình |
| Reified / không | Reified: mảng, raw, `List<?>`. Không: `List<String>`, `T` |
| Lách erasure | `Class<T>` token, `Supplier<T>`, `cls.isInstance`, đổi tên method trùng erasure, `@SafeVarargs` |
| Spring/JDK | `JpaRepository<T,ID>`, `ResponseEntity<T>`, `Optional<T>`, `Comparator<? super T>`, `ParameterizedTypeReference<...>` |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn sau có lỗi compile không? Vì sao?
```java
public class Container<T> {
    private T item;
    public boolean sameTypeAs(T other) { return item.getClass() == other.getClass(); }
}
```

**Câu 2.** Method sau thiếu gì để compile? Sửa lại đúng.
```java
public static T findMax(List<T> list) {
    T max = list.get(0);
    for (T x : list) if (x.compareTo(max) > 0) max = x;
    return max;
}
```

**Câu 3.** Dòng nào compile, dòng nào không? Giải thích bằng invariance.
```java
List<Integer> a = new ArrayList<>();
List<Number>  b = a;                 // (1)
List<? extends Number> c = a;        // (2)
Object[] d = new Integer[3];         // (3)
```

**Câu 4.** Đoạn này compile được không? Giải thích theo wildcard.
```java
public static void addDog(List<? extends Animal> list) { list.add(new Dog()); }
```

**Câu 5.** Áp dụng PECS: điền `extends` hay `super`, giải thích.
```java
public static <T> void moveAll(List<? ___ T> dst, List<? ___ T> src) {
    for (T x : src) dst.add(x);
    src.clear();
}
```

**Câu 6.** Vì sao đoạn này lỗi compile? Liên hệ erasure. Viết cách sửa dùng `Class<T>`.
```java
public class Box<T> {
    public boolean holds(Object o) { return o instanceof T; }
}
```

**Câu 7.** Hai method sau cùng class — vì sao không compile?
```java
void process(List<String> xs) { }
void process(List<Integer> xs) { }
```

**Câu 8.** `List raw = new ArrayList<String>(); raw.add(42);` — compile ra sao (lỗi/warning/sạch)? Dòng `String s = ((List<String>) raw).get(0);` chuyện gì xảy ra lúc chạy? Tên hiện tượng?

---

### Phần B — Bài tập viết code

**Bài 1 — `Pair<K, V>` hoàn chỉnh.**
`Pair<K, V>` với field `final`, constructor, getter, `equals()`/`hashCode()`/`toString()` chuẩn (dựa cả `key` và `value` — Module 02.4). Thêm generic method `static <A, B> Pair<B, A> swapped(Pair<A, B> p)`. `main` chứng minh `equals()` đúng và `swapped(swapped(p)).equals(p)`.

**Bài 2 — Bounded generic method.**
`<T extends Comparable<? super T>> T minOf(List<T> list)` (dùng recursive bound như `Collections.max`). Gọi với `List<Integer>`, `List<String>`, và một cặp `class Employee`/`class Manager extends Employee` mà chỉ `Employee implements Comparable<Employee>` — chứng minh `minOf(List<Manager>)` vẫn biên dịch.

**Bài 3 — Wildcard theo PECS.**
`static double sum(List<? extends Number> src)` và `static void fillZeros(List<? super Integer> dst, int n)`. `main` gọi cả hai với `List<Integer>`, `List<Double>`, `List<Number>`, `List<Object>` ở những chỗ hợp lệ; ghi chú (comment) những lời gọi **không** hợp lệ và vì sao.

**Bài 4 — `MyStack<T>` bằng `ArrayDeque` nội bộ.**
`push`/`pop`/`peek`/`isEmpty`/`size`. `pop()`/`peek()` khi rỗng → `throw new NoSuchElementException("Stack rỗng")` (không để `null` lan truyền). Thêm `static <T> MyStack<T> of(T... items)` với `@SafeVarargs` — giải thích trong comment vì sao ở đây `@SafeVarargs` an toàn.

**Bài 5 — Generic array & capture helper.**
(a) `static <T> void swap(List<?> list, int i, int j)` dùng **capture helper** — giải thích vì sao bản viết trực tiếp `list.set(i, list.set(j, list.get(i)))` trên `List<?>` không compile.
(b) `static <T> T[] toArray(List<T> list, IntFunction<T[]> gen)` (gọi kiểu `toArray(list, String[]::new)`) — giải thích vì sao cần `gen` thay vì `new T[list.size()]`.

**Bài 6 — Generic Repository (chuẩn bị cho Spring Data JPA).**
`interface Repository<T, ID> { T save(T e); Optional<T> findById(ID id); List<T> findAll(); void deleteById(ID id); }`. Cài `class InMemoryUserRepo implements Repository<User, Long>` dùng `Map<Long, User>` + `AtomicLong` sinh id. `main` CRUD thử. So sánh signature với `JpaRepository<T, ID>` thật.

---

### Phần C — Nâng cao

**Câu 1.** Giải thích chính xác vì sao generic phải **invariant** trong khi mảng **covariant**, và điều gì khác nhau về thời điểm phát hiện lỗi (`ArrayStoreException` runtime vs lỗi compile). Vì sao "generic covariant giả định" còn *tệ hơn* mảng covariant (gợi ý: erasure → không có `ArrayStoreException` để cứu).

**Câu 2.** `Collections.max` có signature `<T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)`. Giải nghĩa **từng mảnh**: vì sao `& Object` (gợi ý: binary compatibility / kiểu trả về sau erasure), vì sao `Comparable<? super T>` chứ không `Comparable<T>`, vì sao `Collection<? extends T>`.

**Câu 3.** Type erasure sinh **bridge method**. Cho `class Node implements Comparable<Node>`, liệt kê các method `compareTo` thực sự có trong bytecode và quan hệ gọi giữa chúng. Điều này ảnh hưởng gì tới stack trace và tới việc dùng reflection tìm method?

**Câu 4.** So sánh ba cách "mang kiểu vào runtime": `Class<T>` token, super type token (`ParameterizedTypeReference`/`TypeToken`), và truyền `Supplier<T>`/factory. Mỗi cách giải quyết hạn chế nào của erasure, và giới hạn còn lại là gì?

**Câu 5.** `@SafeVarargs` — heap pollution qua varargs generic xảy ra thế nào? Viết một method varargs generic **không an toàn** (ghi vào mảng varargs hoặc trả nó ra) minh họa `ClassCastException`, rồi giải thích vì sao đánh `@SafeVarargs` lên nó là sai.

**Câu 6.** `List<?>` được coi là *reifiable* còn `List<String>` thì không. Định nghĩa "reifiable type" và liệt kê đầy đủ các nhóm reifiable trong Java. Vì sao `instanceof List<?>` hợp lệ nhưng `instanceof List<String>` thì không?

**Câu 7.** Cho API `<T> Optional<T> firstMatching(List<T> list, Predicate<? super T> p)`. Giải thích vì sao `Predicate<? super T>` (không phải `Predicate<T>` hay `Predicate<? extends T>`), và cho ví dụ lời gọi mà `Predicate<T>` sẽ **từ chối** còn `? super T` thì nhận (gợi ý: `Predicate<Object>` kiểm `Objects::nonNull`).

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Không lỗi.** `getClass()` là method của `Object`, mọi `T` (kể cả chưa bounded) đều có. Hợp lệ.
2. Thiếu khai báo type parameter **và** bound cho `compareTo`. Sửa:
   ```java
   public static <T extends Comparable<? super T>> T findMax(List<T> list) {
       T max = list.get(0);
       for (T x : list) if (x.compareTo(max) > 0) max = x;
       return max;
   }
   ```
3. (1) **Không compile** — generic invariant, `List<Integer>` không phải `List<Number>`. (2) **Compile** — `? extends Number` chấp nhận `List<Integer>`. (3) **Compile** — mảng covariant (`Integer[]` là `Object[]`), nhưng ghi phần tử sai kiểu vào `d` sẽ `ArrayStoreException` lúc chạy.
4. **Không compile.** `List<? extends Animal>` là Producer — chỉ đọc. Compiler không biết `list` thực là `List<Dog>` hay `List<Cat>`; cho `add(new Dog())` mà nó là `List<Cat>` sẽ phá type safety. Đây là lý do PECS.
5. `dst` bị **ghi** (`add`) → Consumer → `? super T`. `src` bị **đọc** (`for`) → Producer → `? extends T`. (`src.clear()` không cần kiểu phần tử nên không ảnh hưởng.)
6. `o instanceof T` — sau erasure `T` là `Object`, JVM không có kiểu để kiểm tra → Java cấm `instanceof` với type parameter. Sửa:
   ```java
   public class Box<T> {
       private final Class<T> type;
       public Box(Class<T> type) { this.type = type; }
       public boolean holds(Object o) { return type.isInstance(o); }
   }
   ```
7. Sau erasure cả hai đều là `process(List)` — "name clash: same erasure" → không phải overload hợp lệ. Đổi tên (`processStrings` / `processInts`).
8. Compile ra **warning** ("unchecked call to add(E)"), không phải lỗi. Lúc chạy: `raw.add(42)` bỏ `Integer` vào list; `((List<String>) raw).get(0)` — cast list không nổ, nhưng gán vào `String s` khiến compiler chèn cast `(String)` → **`ClassCastException`**. Hiện tượng: **heap pollution**.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `equals()` so `Objects.equals(key, o.key) && Objects.equals(value, o.value)`; `hashCode()` = `Objects.hash(key, value)`. `swapped`: `new Pair<>(p.getValue(), p.getKey())`. `swapped(swapped(p))` trả về Pair nội dung như `p` → `equals` true.
- **Bài 2:** `<T extends Comparable<? super T>> T minOf(List<T> list)`. Với `Manager extends Employee implements Comparable<Employee>`: `T = Manager`, cần `Manager extends Comparable<? super Manager>` — thỏa vì `Comparable<Employee>` và `Employee` là *super* của `Manager`. Nếu viết `Comparable<T>` thì cần `Comparable<Manager>` → **không** thỏa → chứng minh vì sao dùng `? super`.
- **Bài 3:** `sum`: hợp lệ với mọi `List<Integer/Double/Number>`; **không** với `List<Object>` (Object không phải Number). `fillZeros`: hợp lệ với `List<Integer/Number/Object>`; **không** với `List<Double>` (Double không phải cha của Integer).
- **Bài 4:** `ArrayDeque.peek()` trả `null` khi rỗng (không ném) → tự `if (isEmpty()) throw new NoSuchElementException(...)`. `@SafeVarargs` an toàn vì `of` chỉ **đọc** `items` để `push`, không ghi vào mảng varargs, không trả nó ra ngoài.
- **Bài 5:** (a) `list.set(i, list.set(j, list.get(i)))` trên `List<?>` không compile vì `list.set(index, ...)` cần đối số kiểu `CAP#1` mà `list.get(i)` trả `CAP#1` — compiler *không chứng minh được* hai `?` là cùng kiểu. Capture helper `<E>` đặt tên `E` cho cái `?` → trong helper mọi thứ là `E` nhất quán. (b) `new T[n]` bị erasure cấm; `IntFunction<T[]> gen` (ví dụ `String[]::new`) để **caller** tạo mảng đúng kiểu thật.
- **Bài 6:** `save`: nếu `id == null` thì `e.setId(seq.incrementAndGet())`, `map.put(id, e)`, return `e`. `findById`: `Optional.ofNullable(map.get(id))`. Signature khớp gần hệt `JpaRepository<T, ID>` — chỉ khác Spring cài sẵn phần persistence.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. Mảng **giữ kiểu phần tử lúc runtime** (reified) nên mỗi lần ghi JVM kiểm tra và ném `ArrayStoreException` — covariant mà *vẫn* an toàn bộ nhớ, chỉ là lỗi bị đẩy sang runtime. Generic bị **erasure**: `List<Integer>` lúc runtime chỉ là `List`, không có gì để kiểm tra khi `add`. Nếu Java cho generic covariant, `Double` lọt vào `List<Integer>` sẽ **không có** exception nào tại điểm ghi; lỗi nổ ở một `get()` xa xôi dưới dạng `ClassCastException` do cast do compiler chèn — khó lần hơn hẳn. Nên generic chọn cấm ở compile-time.
2. `& Object`: sau erasure, bound đầu tiên quyết định kiểu xóa và **kiểu trả về trong bytecode**; ghi `Object &` để `max` erase thành `Object max(Collection)` (giữ tương thích nhị phân với JDK cũ trước khi `Comparable` được generic hóa) thay vì `Comparable max(...)`. `Comparable<? super T>`: cho phép `T` kế thừa `compareTo` từ lớp cha (`Manager` dùng `Comparable<Employee>`). `Collection<? extends T>`: `coll` chỉ bị **đọc** → Producer → `extends`.
3. Bytecode có `int compareTo(Node)` (bản thật) **và** `int compareTo(Object)` (bridge, synthetic) — bridge cast đối số về `Node` rồi gọi bản thật; nó tồn tại để `Comparable.compareTo(Object)` sau erasure vẫn dispatch đúng. Ảnh hưởng: stack trace có thể hiện khung `compareTo(Object)`; reflection `getMethods()` trả **cả hai**, cần lọc `Method::isBridge`/`isSynthetic` khi tìm "method thật".
4. `Class<T>`: giải quyết `new T()`, `instanceof T`, cast — nhưng **không** mang kiểu tham số hóa (`List<User>`). Super type token: khai thác việc kiểu *generic superclass* của anonymous class được lưu trong metadata (`Class.getGenericSuperclass()`) → lấy được `List<User>` đầy đủ — nhưng phải tạo `new X<...>(){}` (có `{}`). `Supplier<T>`/factory: giải quyết *khởi tạo* mà không cần reflection/constructor công khai — nhưng caller phải cung cấp cách tạo, và vẫn không cho `instanceof`.
5. Varargs generic `f(T... a)` thực chất tạo mảng `T[]` mà lúc runtime là `Object[]`. Nếu method **ghi** một giá trị sai kiểu vào `a[0]`, hoặc **trả `a` ra ngoài** rồi nơi khác đọc dưới kiểu `T[]` cụ thể → `ClassCastException`. Ví dụ không an toàn: `static <T> T[] toArray(T... a) { return a; }` rồi `String[] s = toArray("x", 1);` — mảng thật `Object[]`, gán `String[]` nổ. Đánh `@SafeVarargs` lên nó là **nói dối compiler** — annotation chỉ *tắt cảnh báo*, không làm code an toàn.
6. **Reifiable type** = kiểu mà thông tin đầy đủ của nó *có* lúc runtime. Nhóm reifiable: kiểu primitive; non-generic class/interface; kiểu tham số hóa mà **mọi** đối số là **unbounded wildcard** (`List<?>`, `Map<?, ?>`); raw type (`List`); mảng của reifiable; kiểu `? extends`/`? super` thì **không**. `instanceof List<?>` hợp lệ vì runtime chỉ cần biết "có phải `List` không" — `?` không thêm ràng buộc; `instanceof List<String>` cần kiểm tra kiểu phần tử đã bị xóa → cấm.
7. `Predicate<? super T>` vì predicate **tiêu thụ** `T` (nhận `T`, trả `boolean`) → Consumer → `super`. `Predicate<T>` sẽ từ chối `Predicate<Object> nonNull = Objects::nonNull` khi `T = String` (vì `Predicate<Object>` không phải `Predicate<String>`); `? super String` thì nhận, vì `Object` là cha của `String`. `? extends` sai hoàn toàn ở đây — sẽ cho `Predicate<Integer>` áp lên `List<String>`.

</details>

---

*File tiếp theo trong lộ trình: **Module 03.3 — Stream API & Lambda** (map/filter/reduce/collect, method reference, functional interface).*
