# Lời giải đầy đủ — Module 03.2: Generics

> Nguồn đề: `09-generics/09-generics.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `Pair<K, V>` hoàn chỉnh

### Đề
`Pair<K, V>` với field `final`, constructor, getter, `equals()`/`hashCode()`/`toString()` chuẩn (dựa cả `key` và `value` — Module 02.4). Thêm generic method `static <A, B> Pair<B, A> swapped(Pair<A, B> p)`. `main` chứng minh `equals()` đúng và `swapped(swapped(p)).equals(p)`.

### Phân tích

Đây là bài kết hợp trực tiếp kiến thức Module 02.4 (`equals`/`hashCode` chuẩn — dùng `Objects.equals`/`Objects.hash` để an toàn với `null`) và Generics: `swapped()` là 1 **generic static method** với 2 type parameter riêng (`<A, B>`, độc lập với type parameter của class `Pair<K, V>`) — nhận `Pair<A, B>`, trả `Pair<B, A>` (đảo vị trí 2 type parameter).

### Lời giải

```java
package baitap.bai1;

import java.util.Objects;

public class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> other = (Pair<?, ?>) o;
        return Objects.equals(key, other.key) && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }

    // Generic method ĐỘC LẬP với <K, V> của class - dùng <A, B> riêng
    public static <A, B> Pair<B, A> swapped(Pair<A, B> p) {
        return new Pair<>(p.getValue(), p.getKey());
    }

    public static void main(String[] args) {
        Pair<String, Integer> p = new Pair<>("age", 25);
        System.out.println("p = " + p);

        Pair<Integer, String> sp = swapped(p);
        System.out.println("swapped(p) = " + sp);

        Pair<String, Integer> back = swapped(sp);
        System.out.println("swapped(swapped(p)) = " + back);
        System.out.println("swapped(swapped(p)).equals(p) = " + back.equals(p));

        System.out.println("\nKiểm chứng equals():");
        Pair<String, Integer> p2 = new Pair<>("age", 25);
        System.out.println("p.equals(p2) (cùng nội dung, khác object) = " + p.equals(p2));
        System.out.println("p.hashCode() == p2.hashCode() = " + (p.hashCode() == p2.hashCode()));
    }
}
```

**Kết quả chạy:**
```
p = (age, 25)
swapped(p) = (25, age)
swapped(swapped(p)) = (age, 25)
swapped(swapped(p)).equals(p) = true

Kiểm chứng equals():
p.equals(p2) (cùng nội dung, khác object) = true
p.hashCode() == p2.hashCode() = true
```

### Giải thích

- `swapped(swapped(p))` cho lại đúng nội dung ban đầu — kiểm chứng bằng `equals()` (dựa nội dung, không phải `==`) đúng chuẩn Module 02.4.
- Trong `equals()`, ép kiểu về `Pair<?, ?>` (wildcard, không phải `Pair<K, V>`) — vì tại thời điểm `equals(Object o)`, không thể biết chắc `o` mang type parameter gì; dùng `?` để hợp lệ với **mọi** `Pair` bất kể type argument, đúng nguyên tắc `equals(Object)` phải nhận **mọi** class.

---

## Bài 2 — Bounded generic method

### Đề
`<T extends Comparable<? super T>> T minOf(List<T> list)` (dùng recursive bound như `Collections.max`). Gọi với `List<Integer>`, `List<String>`, và một cặp `class Employee`/`class Manager extends Employee` mà chỉ `Employee implements Comparable<Employee>` — chứng minh `minOf(List<Manager>)` vẫn biên dịch.

### Phân tích

`<T extends Comparable<? super T>>` là **recursive bound** (bound tự tham chiếu tới chính `T`) — ý nghĩa: "`T` phải so sánh được với **chính nó HOẶC với 1 kiểu cha nào đó của nó**". Cách viết này **linh hoạt hơn** `<T extends Comparable<T>>` thông thường: cho phép `T` (VD `Manager`) kế thừa `compareTo` từ 1 **class cha** (`Employee implements Comparable<Employee>`) mà không cần chính `Manager` phải tự `implements Comparable<Manager>`.

### Lời giải

```java
package baitap.bai2;

import java.util.List;

public class Main {

    static <T extends Comparable<? super T>> T minOf(List<T> list) {
        T min = list.get(0);
        for (T item : list) {
            if (item.compareTo(min) < 0) {
                min = item;
            }
        }
        return min;
    }

    static class Employee implements Comparable<Employee> {
        String name;
        double salary;
        Employee(String name, double salary) { this.name = name; this.salary = salary; }

        @Override
        public int compareTo(Employee other) {
            return Double.compare(this.salary, other.salary);
        }

        @Override
        public String toString() { return name + "(" + salary + ")"; }
    }

    // Manager KHÔNG tự implements Comparable<Manager> - chỉ kế thừa Comparable<Employee> từ cha
    static class Manager extends Employee {
        Manager(String name, double salary) { super(name, salary); }
    }

    public static void main(String[] args) {
        System.out.println("minOf(Integer): " + minOf(List.of(5, 2, 8, 1, 9)));
        System.out.println("minOf(String): " + minOf(List.of("banana", "apple", "cherry")));

        List<Manager> managers = List.of(
                new Manager("A", 3000),
                new Manager("B", 1500),
                new Manager("C", 2200)
        );
        // Vẫn BIÊN DỊCH ĐƯỢC - vì Manager thỏa "T extends Comparable<? super T>":
        // T = Manager, "? super Manager" chấp nhận Comparable<Employee> (Employee LÀ cha của Manager)
        System.out.println("minOf(Manager): " + minOf(managers));
    }
}
```

**Kết quả chạy:**
```
minOf(Integer): 1
minOf(String): apple
minOf(Manager): B(1500.0)
```

### Giải thích

- Nếu khai báo `<T extends Comparable<T>>` (không có `? super`), `minOf(managers)` sẽ **KHÔNG compile** — vì compiler đòi hỏi `Manager` phải `implements Comparable<Manager>` **chính xác**, trong khi thực tế `Manager` chỉ có `Comparable<Employee>` (thừa kế từ cha).
- Với `Comparable<? super T>`, khi `T = Manager`, điều kiện trở thành "`Manager` phải implements `Comparable<X>` với `X` là `Manager` hoặc **bất kỳ lớp cha nào của `Manager`**" — `Employee` là cha của `Manager`, và `Employee implements Comparable<Employee>` → điều kiện **thỏa mãn**.
- Đây chính xác là cách JDK thật khai báo `Collections.max()`/`Collections.min()` — bound `<T extends Comparable<? super T>>` **linh hoạt hơn nhiều** so với `<T extends Comparable<T>>`, và là mẫu hình nên nhớ khi tự viết method generic tổng quát có so sánh.

---

## Bài 3 — Wildcard theo PECS

### Đề
`static double sum(List<? extends Number> src)` và `static void fillZeros(List<? super Integer> dst, int n)`. `main` gọi cả hai với `List<Integer>`, `List<Double>`, `List<Number>`, `List<Object>` ở những chỗ hợp lệ; ghi chú (comment) những lời gọi **không** hợp lệ và vì sao.

### Phân tích

**PECS (Producer Extends, Consumer Super):** nếu tham số chỉ bị **ĐỌC** (producer — đưa dữ liệu RA), dùng `? extends T`. Nếu tham số chỉ bị **GHI** (consumer — nhận dữ liệu VÀO), dùng `? super T`. `sum()` chỉ **đọc** từng phần tử để cộng dồn → Producer → `? extends Number`. `fillZeros()` chỉ **ghi** giá trị `0` vào list → Consumer → `? super Integer`.

### Lời giải

```java
package baitap.bai3;

import java.util.ArrayList;
import java.util.List;

public class Main {

    // Producer - CHỈ ĐỌC từng phần tử -> "? extends Number"
    static double sum(List<? extends Number> src) {
        double total = 0;
        for (Number n : src) {
            total += n.doubleValue();
        }
        return total;
    }

    // Consumer - CHỈ GHI giá trị vào list -> "? super Integer"
    static void fillZeros(List<? super Integer> dst, int n) {
        for (int i = 0; i < n; i++) {
            dst.add(0);
        }
    }

    public static void main(String[] args) {
        List<Integer> ints = new ArrayList<>(List.of(1, 2, 3));
        List<Double> doubles = new ArrayList<>(List.of(1.5, 2.5));
        List<Number> numbers = new ArrayList<>(List.of(1, 2.5, 3L));
        List<Object> objects = new ArrayList<>(List.of("a", "b"));

        // ---- sum() - HỢP LỆ với mọi List<? extends Number> ----
        System.out.println("sum(ints) = " + sum(ints));       // OK - Integer extends Number
        System.out.println("sum(doubles) = " + sum(doubles)); // OK - Double extends Number
        System.out.println("sum(numbers) = " + sum(numbers)); // OK - Number extends Number (chính nó)
        // sum(objects); // ❌ KHÔNG COMPILE - Object KHÔNG PHẢI Number (Object là cha, không phải con)

        // ---- fillZeros() - HỢP LỆ với List<? super Integer> ----
        fillZeros(ints, 2);
        System.out.println("ints sau fillZeros: " + ints);       // OK - Integer super Integer (chính nó)
        fillZeros(numbers, 2);
        System.out.println("numbers sau fillZeros: " + numbers); // OK - Number là CHA của Integer
        fillZeros(objects, 2);
        System.out.println("objects sau fillZeros: " + objects); // OK - Object là CHA của Integer
        // fillZeros(doubles, 2); // ❌ KHÔNG COMPILE - Double KHÔNG PHẢI cha (cũng không phải chính) của Integer
    }
}
```

**Kết quả chạy:**
```
sum(ints) = 6.0
sum(doubles) = 4.0
sum(numbers) = 6.5
ints sau fillZeros: [1, 2, 3, 0, 0]
numbers sau fillZeros: [1, 2.5, 3, 0, 0]
objects sau fillZeros: [a, b, 0, 0]
```

### Giải thích các lời gọi KHÔNG hợp lệ

- **`sum(objects)` không compile:** `List<Object>` không phải `List<? extends Number>` — `Object` **KHÔNG** kế thừa `Number` (quan hệ ngược lại: `Number extends Object`). Wildcard `? extends Number` chỉ chấp nhận `Number` hoặc **các lớp con** của nó (`Integer`, `Double`, `Long`...), không chấp nhận `Object` (lớp cha của `Number`).
- **`fillZeros(doubles, 2)` không compile:** `List<Double>` không phải `List<? super Integer>` — `Double` **không phải** `Integer`, cũng **không phải cha** của `Integer` (cả 2 đều là con trực tiếp của `Number`, ngang hàng nhau, không có quan hệ kế thừa giữa chúng). Wildcard `? super Integer` chỉ chấp nhận `Integer` hoặc **các lớp cha thực sự** của nó (`Number`, `Object`).

---

## Bài 4 — `MyStack<T>` bằng `ArrayDeque` nội bộ

### Đề
`push`/`pop`/`peek`/`isEmpty`/`size`. `pop()`/`peek()` khi rỗng → `throw new NoSuchElementException("Stack rỗng")` (không để `null` lan truyền). Thêm `static <T> MyStack<T> of(T... items)` với `@SafeVarargs` — giải thích trong comment vì sao ở đây `@SafeVarargs` an toàn.

### Phân tích

Dùng `ArrayDeque` làm cấu trúc lưu trữ bên trong (composition, không kế thừa — đúng bài học Module 02.1 Bài 6) vì `ArrayDeque` là cấu trúc **Deque hiệu năng cao nhất** trong JDK cho việc dùng như Stack (nhanh hơn cả `java.util.Stack` cũ — vốn kế thừa `Vector`, có `synchronized` thừa thãi không cần thiết trong đa số trường hợp đơn luồng).

`@SafeVarargs` chỉ nên gắn khi method **KHÔNG** ghi giá trị sai kiểu vào mảng varargs, và **KHÔNG** trả mảng đó ra ngoài — `of()` chỉ **đọc** `items` để `push` từng phần tử vào Stack, hoàn toàn thỏa điều kiện an toàn.

### Lời giải

```java
package baitap.bai4;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

public class MyStack<T> {
    private final Deque<T> data = new ArrayDeque<>();

    public void push(T item) {
        data.push(item);
    }

    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack rỗng");
        }
        return data.pop();
    }

    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack rỗng");
        }
        return data.peek();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int size() {
        return data.size();
    }

    @SafeVarargs
    // AN TOÀN vì: (1) chỉ ĐỌC "items" (push từng phần tử vào Stack), KHÔNG ghi giá trị lạ vào mảng varargs;
    // (2) KHÔNG trả "items" (hay bất kỳ mảng nào dựa trên nó) ra bên ngoài -> không có nguy cơ
    // ClassCastException xảy ra ở nơi khác do heap pollution.
    public static <T> MyStack<T> of(T... items) {
        MyStack<T> stack = new MyStack<>();
        for (T item : items) {
            stack.push(item);
        }
        return stack;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public static void main(String[] args) {
        MyStack<String> stack = MyStack.of("a", "b", "c");
        System.out.println("Stack: " + stack);
        System.out.println("size() = " + stack.size());
        System.out.println("peek() = " + stack.peek());
        System.out.println("pop() = " + stack.pop());
        System.out.println("Stack sau pop: " + stack);

        MyStack<Integer> emptyStack = new MyStack<>();
        try {
            emptyStack.pop();
        } catch (NoSuchElementException e) {
            System.out.println("pop() khi rỗng -> " + e.getMessage());
        }
    }
}
```

**Kết quả chạy:**
```
Stack: [c, b, a]
size() = 3
peek() = c
pop() = c
Stack sau pop: [b, a]
pop() khi rỗng -> Stack rỗng
```

### Giải thích

- **Vì sao KHÔNG để `pop()`/`peek()` trả `null` khi rỗng:** nếu `T` là kiểu tham chiếu, trả `null` khiến caller **không phân biệt được** "Stack rỗng" với "phần tử thực sự chứa giá trị `null`" — ném exception rõ ràng buộc caller phải **chủ động kiểm tra `isEmpty()` trước** hoặc `try/catch`, tránh bug âm thầm lan truyền `NullPointerException` ở nơi khác xa gốc lỗi thật.
- `@SafeVarargs` chỉ được phép gắn cho method `static`, `final`, hoặc `private` (từ Java 9+) — vì chỉ những method này **không thể bị override**, đảm bảo hành vi "an toàn" không bị 1 lớp con phá vỡ sau này.

---

## Bài 5 — Generic array & capture helper

### Đề
(a) `static <T> void swap(List<?> list, int i, int j)` dùng **capture helper** — giải thích vì sao bản viết trực tiếp `list.set(i, list.set(j, list.get(i)))` trên `List<?>` không compile.
(b) `static <T> T[] toArray(List<T> list, IntFunction<T[]> gen)` (gọi kiểu `toArray(list, String[]::new)`) — giải thích vì sao cần `gen` thay vì `new T[list.size()]`.

### Phân tích (a) — Wildcard Capture

`List<?>` nghĩa là "list của **1 kiểu cụ thể nào đó, nhưng compiler không biết là kiểu gì**". Viết `list.set(i, list.get(j))` trực tiếp trên `List<?>` **không compile** — vì `set(int, E)` của `List<E>` yêu cầu tham số **đúng kiểu `E`**, nhưng với `?`, compiler chỉ biết "1 kiểu ẩn danh nào đó" (ký hiệu nội bộ `CAP#1`) — nó **không thể chứng minh** giá trị lấy từ `list.get(j)` (kiểu `CAP#1`) là **an toàn** để đặt lại vào `list.set(i, ...)` (cũng cần đúng `CAP#1`), dù về logic 2 lần `CAP#1` này **là cùng 1 kiểu thật sự** — compiler chỉ đơn giản **không đủ thông tin để chứng minh** điều đó ở chỗ gọi trực tiếp.

**Giải pháp — Capture Helper:** tách ra 1 method **generic riêng** (`<T>`), gọi nó từ bên trong — bên trong method helper, `T` được "chốt" thành **1 kiểu cụ thể xuyên suốt cả method**, compiler **chứng minh được** an toàn.

### Lời giải (a)

```java
package baitap.bai5;

import java.util.List;

public class Main {

    // Method CÔNG KHAI nhận List<?> - KHÔNG thể swap trực tiếp tại đây
    static void swap(List<?> list, int i, int j) {
        swapHelper(list, i, j); // ủy quyền cho capture helper
    }

    // Capture helper - PRIVATE, dùng <T> riêng để "chốt" kiểu thật của list
    private static <T> void swapHelper(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j)); // AN TOÀN - cả 2 vế đều chắc chắn cùng kiểu T
        list.set(j, temp);
    }

    public static void main(String[] args) {
        List<String> names = new java.util.ArrayList<>(List.of("A", "B", "C"));
        swap(names, 0, 2);
        System.out.println("Sau swap(0, 2): " + names);
    }
}
```

**Kết quả chạy:**
```
Sau swap(0, 2): [C, B, A]
```

### Phân tích (b) — Generic Array Creation

Java **CẤM tạo mảng generic trực tiếp** (`new T[n]`) — vì **type erasure**: tại runtime, `T` đã bị "xóa" (erase) thành `Object` (hoặc bound của nó), nhưng **mảng** trong Java lại **giữ thông tin kiểu tại runtime** (reified) để kiểm tra khi ghi phần tử (`ArrayStoreException`). Nếu cho phép `new T[n]`, JVM **không biết kiểu thật sự** để gắn vào mảng lúc runtime — vi phạm bản chất "mảng biết kiểu của mình" của Java.

**Giải pháp:** để **caller** (nơi biết chính xác `T` là gì lúc gọi) cung cấp cách tạo mảng đúng kiểu, qua tham số `IntFunction<T[]> gen` — thường truyền bằng method reference dạng `String[]::new`.

### Lời giải (b)

```java
package baitap.bai5;

import java.util.List;
import java.util.function.IntFunction;

public class ArrayHelper {

    static <T> T[] toArray(List<T> list, IntFunction<T[]> gen) {
        T[] array = gen.apply(list.size()); // gen biết ĐÚNG kiểu T thật sự (do caller cung cấp)
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static void main(String[] args) {
        List<String> names = List.of("Pho", "Huynh", "Gia");
        String[] array = toArray(names, String[]::new); // caller BIẾT rõ đang cần String[]

        System.out.println("Kiểu mảng thật: " + array.getClass().getSimpleName()); // String[]
        for (String s : array) System.out.println("  " + s);
    }
}
```

**Kết quả chạy:**
```
Kiểu mảng thật: String[]
  Pho
  Huynh
  Gia
```

### Giải thích

- **Nếu viết `new T[list.size()]` bên trong `toArray()`:** lỗi compile ngay `"Cannot create a generic array of T"` — vì bên trong method, sau erasure, `T` chỉ còn là `Object`, `new T[n]` thực chất sẽ tạo ra `Object[]` — nhưng khai báo kiểu trả về là `T[]`, gây **không nhất quán ngầm** giữa kiểu compile-time và runtime, chính là nguồn gốc lỗi `ArrayStoreException`/`ClassCastException` tiềm ẩn mà Java cố tình **chặn ngay từ lúc compile**.
- `gen.apply(list.size())` gọi `String[]::new` — thực chất tương đương `new String[list.size()]` — nhưng vì lời gọi này nằm **NGAY tại nơi caller biết chính xác `T = String`**, không phải bên trong method generic erasure, nên hoàn toàn hợp lệ và **mảng tạo ra có kiểu runtime chính xác là `String[]`**, không phải `Object[]` "trá hình".
- Đây chính xác là cách `List.toArray(IntFunction<T[]> generator)` của JDK thật (Java 11+) hoạt động — VD `list.toArray(String[]::new)`.

---

## Bài 6 — Generic Repository (chuẩn bị cho Spring Data JPA)

### Đề
`interface Repository<T, ID> { T save(T e); Optional<T> findById(ID id); List<T> findAll(); void deleteById(ID id); }`. Cài `class InMemoryUserRepo implements Repository<User, Long>` dùng `Map<Long, User>` + `AtomicLong` sinh id. `main` CRUD thử. So sánh signature với `JpaRepository<T, ID>` thật.

### Phân tích

Đây là bài "gieo mầm" trực tiếp cho Spring Data JPA (Module 15) — `interface Repository<T, ID>` với **2 type parameter** (`T` = kiểu Entity, `ID` = kiểu khóa chính) **CHÍNH XÁC** là ý tưởng gốc của `JpaRepository<T, ID>` thật trong Spring. Viết tay bằng `Map` + `AtomicLong` giúp hiểu **bản chất** CRUD generic trước khi dùng bản có sẵn của framework.

### Lời giải

```java
package baitap.bai6;

import java.util.List;
import java.util.Optional;

interface Repository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
}

class User {
    private Long id;
    private String name;

    public User(String name) { this.name = name; } // id chưa có - sẽ được Repository gán

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }

    @Override
    public String toString() { return "User{id=" + id + ", name='" + name + "'}"; }
}

class InMemoryUserRepo implements Repository<User, Long> {
    private final java.util.Map<Long, User> storage = new java.util.HashMap<>();
    private final java.util.concurrent.atomic.AtomicLong idGenerator = new java.util.concurrent.atomic.AtomicLong(0);

    @Override
    public User save(User entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.incrementAndGet()); // TẠO MỚI - sinh id tự động
        }
        storage.put(entity.getId(), entity); // TẠO MỚI hoặc CẬP NHẬT nếu id đã tồn tại
        return entity;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }
}

public class Main {
    public static void main(String[] args) {
        InMemoryUserRepo repo = new InMemoryUserRepo();

        User u1 = repo.save(new User("Pho"));
        User u2 = repo.save(new User("Huynh"));
        System.out.println("Đã lưu: " + u1 + ", " + u2);

        System.out.println("findById(1): " + repo.findById(1L));
        System.out.println("findById(999): " + repo.findById(999L)); // Optional.empty()

        System.out.println("findAll(): " + repo.findAll());

        repo.deleteById(1L);
        System.out.println("Sau deleteById(1): " + repo.findAll());
    }
}
```

**Kết quả chạy:**
```
Đã lưu: User{id=1, name='Pho'}, User{id=2, name='Huynh'}
findById(1): Optional[User{id=1, name='Pho'}]
findById(999): Optional.empty
findAll(): [User{id=1, name='Pho'}, User{id=2, name='Huynh'}]
Sau deleteById(1): [User{id=2, name='Huynh'}]
```

### So sánh với `JpaRepository<T, ID>` thật (Spring Data)

```java
// JpaRepository thật (Spring Data JPA - Module 15) - CÙNG Ý TƯỞNG, chỉ khác cách cài đặt:
public interface UserRepository extends JpaRepository<User, Long> {
    // save(), findById(), findAll(), deleteById() ĐÃ CÓ SẴN - Spring tự sinh implementation
    // (dùng Dynamic Proxy - liên hệ Module 12 - thay vì bạn tự viết InMemoryUserRepo như trên)
}
```

| | `Repository<T, ID>` tự viết | `JpaRepository<T, ID>` thật |
|---|---|---|
| `save(T)` | Tự tay `put` vào `Map` | Spring tự sinh SQL `INSERT`/`UPDATE` |
| `findById(ID)` | Tự tay `get` từ `Map` | Spring tự sinh SQL `SELECT ... WHERE id = ?` |
| Sinh ID | Tự viết `AtomicLong` | Database tự sinh (`AUTO_INCREMENT`/`SEQUENCE`) |
| Cách implement | Bạn tự viết `class InMemoryUserRepo implements ...` | **KHÔNG viết implementation** — chỉ khai báo `interface`, Spring tự tạo Proxy lúc runtime |

### Giải thích

- Ý tưởng **2 type parameter `<T, ID>`** (Entity + kiểu khóa chính) là **hoàn toàn giống hệt** giữa bài tập tự viết và Spring Data JPA thật — đây không phải trùng hợp, mà vì đây là mẫu hình CRUD **tổng quát nhất có thể** cho bất kỳ Entity nào có 1 khóa chính.
- Khác biệt lớn nhất: `InMemoryUserRepo` là code **bạn tự viết bằng tay**; `JpaRepository` chỉ cần **khai báo `interface` rỗng**, Spring Data JPA dùng cơ chế **Dynamic Proxy** (sẽ hiểu rõ cơ chế này ở Module 12 — Spring Core) để **tự động sinh implementation** lúc chạy — bạn không bao giờ viết `class ... implements UserRepository` cho các method CRUD cơ bản.
- Hiểu bài tập này trước khi học Spring giúp không còn thấy `JpaRepository` là "phép màu" — nó chỉ là **phiên bản tự động hóa** của chính điều bạn vừa viết tay ở đây.

---

*Đây là lời giải cho toàn bộ Phần B của Module 09. Tiếp theo: Module 10 — Stream API & Lambda.*
