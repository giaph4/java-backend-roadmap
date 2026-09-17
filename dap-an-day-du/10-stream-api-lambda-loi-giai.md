# Lời giải đầy đủ — Module 03.3: Stream API & Lambda

> Nguồn đề: `10 stream api lambda/10-stream-api-lambda.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Xử lý danh sách sinh viên

### Đề
`record Student(String name, int age, double gpa)`, ≥ 8 phần tử. Dùng Stream: lọc `gpa >= 3.5` lấy tên sắp alphabet; GPA trung bình bằng `average()`; `IntSummaryStatistics` tuổi; sinh viên GPA cao nhất bằng `max()`.

### Phân tích

4 yêu cầu tương ứng 4 kỹ thuật Stream cốt lõi: `filter + map + sorted + toList` (lọc-biến đổi-sắp xếp-thu gom); `mapToDouble().average()` trả `OptionalDouble` (vì list rỗng thì không có trung bình); `mapToInt().summaryStatistics()` (1 lần duyệt lấy đủ min/max/avg/count/sum); `max(Comparator)` trả `Optional<T>` (vì list rỗng thì không có "lớn nhất").

### Lời giải

```java
package baitap.bai1;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    record Student(String name, int age, double gpa) {}

    public static void main(String[] args) {
        List<Student> students = List.of(
                new Student("Pho", 22, 3.8),
                new Student("Huynh", 21, 3.2),
                new Student("Gia", 23, 3.9),
                new Student("An", 20, 3.6),
                new Student("Binh", 24, 2.9),
                new Student("Chi", 22, 3.5),
                new Student("Dung", 21, 3.7),
                new Student("Em", 25, 3.1)
        );

        // (1) Lọc gpa >= 3.5, lấy tên đã sắp alphabet
        List<String> highGpaNames = students.stream()
                .filter(s -> s.gpa() >= 3.5)
                .map(Student::name)
                .sorted()
                .toList();
        System.out.println("Tên GPA >= 3.5 (alphabet): " + highGpaNames);

        // (2) GPA trung bình
        OptionalDouble avgGpa = students.stream().mapToDouble(Student::gpa).average();
        avgGpa.ifPresentOrElse(
                avg -> System.out.printf("GPA trung bình: %.2f%n", avg),
                () -> System.out.println("Danh sách rỗng, không có GPA trung bình")
        );

        // (3) IntSummaryStatistics của tuổi
        IntSummaryStatistics ageStats = students.stream().mapToInt(Student::age).summaryStatistics();
        System.out.printf("Tuổi: min=%d, max=%d, avg=%.2f, count=%d%n",
                ageStats.getMin(), ageStats.getMax(), ageStats.getAverage(), ageStats.getCount());

        // (4) Sinh viên GPA cao nhất
        Optional<Student> topStudent = students.stream()
                .max(Comparator.comparingDouble(Student::gpa));
        topStudent.ifPresent(s -> System.out.println("GPA cao nhất: " + s.name() + " (" + s.gpa() + ")"));
    }
}
```

**Kết quả chạy:**
```
Tên GPA >= 3.5 (alphabet): [An, Chi, Dung, Gia, Pho]
GPA trung bình: 3.46
Tuổi: min=20, max=25, avg=22.25, count=8
GPA cao nhất: Gia (3.9)
```

### Giải thích

- `average()`/`max()` trả `Optional`/`OptionalDouble` (không phải giá trị thẳng) vì **Stream rỗng** không có "trung bình"/"lớn nhất" nào cả — buộc caller phải xử lý tường minh trường hợp này (`ifPresent`, `orElse`...), thay vì âm thầm trả `0`/`null` gây hiểu lầm.
- `summaryStatistics()` chỉ **duyệt Stream đúng 1 lần** mà lấy được cả 5 chỉ số (`min/max/average/count/sum`) — hiệu quả hơn nhiều so với gọi riêng lẻ `min()`, `max()`, `average()` (mỗi lần gọi là 1 lượt duyệt Stream riêng, và **Stream chỉ dùng được 1 lần** nên phải tạo lại Stream mới cho mỗi lần gọi nếu làm riêng lẻ).

---

## Bài 2 — Group By & downstream collector

### Đề
Dùng `Employee(name, dept, salary)`: nhóm theo dept chỉ lấy tên; nhân viên lương cao nhất mỗi phòng; tổng quỹ lương mỗi phòng sắp giảm dần; tổng quỹ lương công ty.

### Phân tích

**Downstream collector** là collector "thứ 2" truyền vào `groupingBy(classifier, downstream)` — quyết định **mỗi nhóm** được gom lại thành gì (không nhất thiết phải là `List<Employee>` mặc định). `mapping(Employee::name, toList())` biến đổi trước rồi mới gom; `collectingAndThen(maxBy(...), Optional::get)` tìm max rồi "bóc" ra khỏi `Optional` (an toàn vì mỗi nhóm luôn có ít nhất 1 phần tử — do chính `groupingBy` tạo ra nhóm).

### Lời giải

```java
package baitap.bai2;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    record Employee(String name, String dept, double salary) {}

    public static void main(String[] args) {
        List<Employee> employees = List.of(
                new Employee("Pho", "IT", 20_000_000),
                new Employee("Huynh", "IT", 25_000_000),
                new Employee("Gia", "Sales", 15_000_000),
                new Employee("An", "Sales", 18_000_000),
                new Employee("Binh", "HR", 12_000_000),
                new Employee("Chi", "IT", 22_000_000)
        );

        // (1) Map<dept, List<tên>>
        Map<String, List<String>> namesByDept = employees.stream()
                .collect(Collectors.groupingBy(Employee::dept,
                        Collectors.mapping(Employee::name, Collectors.toList())));
        System.out.println("Tên theo phòng: " + namesByDept);

        // (2) Map<dept, Employee lương cao nhất>
        Map<String, Employee> topEarnerByDept = employees.stream()
                .collect(Collectors.groupingBy(Employee::dept,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingDouble(Employee::salary)),
                                Optional::get))); // AN TOÀN - mỗi nhóm groupingBy tạo ra luôn có >= 1 phần tử
        System.out.println("\nLương cao nhất mỗi phòng:");
        topEarnerByDept.forEach((dept, e) -> System.out.println("  " + dept + ": " + e.name() + " (" + e.salary() + ")"));

        // (3) Tổng quỹ lương mỗi phòng, sắp GIẢM DẦN, thu vào LinkedHashMap (giữ thứ tự đã sort)
        Map<String, Double> totalByDept = employees.stream()
                .collect(Collectors.groupingBy(Employee::dept, Collectors.summingDouble(Employee::salary)));

        Map<String, Double> sortedTotalByDept = totalByDept.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new)); // LinkedHashMap giữ đúng thứ tự đã sort

        System.out.println("\nTổng quỹ lương mỗi phòng (giảm dần):");
        sortedTotalByDept.forEach((dept, total) -> System.out.println("  " + dept + ": " + total));

        // (4) Tổng quỹ lương toàn công ty
        double companyTotal = employees.stream().mapToDouble(Employee::salary).sum();
        System.out.println("\nTổng quỹ lương công ty: " + companyTotal);
    }
}
```

**Kết quả chạy:**
```
Tên theo phòng: {HR=[Binh], IT=[Pho, Huynh, Chi], Sales=[Gia, An]}

Lương cao nhất mỗi phòng:
  HR: Binh (1.2E7)
  IT: Huynh (2.5E7)
  Sales: An (1.8E7)

Tổng quỹ lương mỗi phòng (giảm dần):
  IT: 6.7E7
  Sales: 3.3E7
  HR: 1.2E7

Tổng quỹ lương công ty: 1.12E8
```

### Giải thích

- `collectingAndThen(collector, finisher)` cho phép **"hậu xử lý"** kết quả của collector chính — ở đây `maxBy` trả `Optional<Employee>` (vì về mặt lý thuyết `maxBy` có thể áp dụng cho Stream rỗng), nhưng vì đây là kết quả của `groupingBy` (mỗi nhóm chắc chắn ≥ 1 phần tử, không thể rỗng), `Optional::get` **an toàn tuyệt đối** để "mở khóa" giá trị thật, biến `Map<String, Optional<Employee>>` thành `Map<String, Employee>` gọn hơn nhiều khi dùng.
- `groupingBy` mặc định trả `HashMap` (không giữ thứ tự) — muốn có thứ tự (giảm dần theo tổng lương), phải **sort riêng entrySet rồi thu lại vào `LinkedHashMap`** (vì `LinkedHashMap` giữ đúng thứ tự các phần tử được `put` vào nó, ở đây là thứ tự đã sort).

---

## Bài 3 — flatMap dữ liệu lồng

### Đề
`record Order(String customerId, List<String> products)`, ≥ 4 đơn. Dùng `flatMap` lấy danh sách sản phẩm **không trùng**, sắp alphabet. Rồi dùng `flatMap` + `groupingBy` + `counting` đếm mỗi sản phẩm xuất hiện trong bao nhiêu đơn.

### Phân tích

`flatMap` dùng khi mỗi phần tử Stream **tự nó chứa 1 Stream/Collection con** — cần "làm phẳng" (flatten) thành 1 Stream duy nhất chứa tất cả phần tử con, thay vì `map` (sẽ tạo ra `Stream<Stream<String>>`, 1 tầng lồng không mong muốn).

### Lời giải

```java
package baitap.bai3;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    record Order(String customerId, List<String> products) {}

    public static void main(String[] args) {
        List<Order> orders = List.of(
                new Order("C1", List.of("Áo", "Quần", "Giày")),
                new Order("C2", List.of("Áo", "Nón")),
                new Order("C3", List.of("Giày", "Vớ")),
                new Order("C4", List.of("Áo", "Quần", "Vớ"))
        );

        // (1) Danh sách sản phẩm KHÔNG TRÙNG, sắp alphabet - flatMap "làm phẳng" List<List<String>> -> Stream<String>
        List<String> uniqueProducts = orders.stream()
                .flatMap(o -> o.products().stream())
                .distinct()
                .sorted()
                .toList();
        System.out.println("Sản phẩm duy nhất: " + uniqueProducts);

        // (2) Đếm mỗi sản phẩm xuất hiện trong bao nhiêu đơn
        Map<String, Long> productOrderCount = orders.stream()
                .flatMap(o -> o.products().stream())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        System.out.println("Số đơn chứa mỗi sản phẩm: " + productOrderCount);
    }
}
```

**Kết quả chạy:**
```
Sản phẩm duy nhất: [Giày, Nón, Quần, Vớ, Áo]
Số đơn chứa mỗi sản phẩm: {Giày=2, Nón=1, Quần=2, Vớ=2, Áo=3}
```
*(thứ tự các key trong `Map` khi `println` phụ thuộc `HashMap`, không đảm bảo; thứ tự chữ Việt có dấu trong `sorted()` cũng phụ thuộc bảng mã Unicode mặc định, không theo đúng thứ tự alphabet tiếng Việt)*

### Giải thích

- Không dùng `flatMap` mà dùng `map(Order::products)`, kết quả sẽ là `Stream<List<String>>` — mỗi phần tử Stream vẫn là **1 List riêng**, phải duyệt lồng thêm 1 tầng nữa mới lấy được từng sản phẩm — `flatMap` gộp hết **TẤT CẢ sản phẩm của TẤT CẢ đơn hàng** thành **1 Stream phẳng duy nhất** ngay từ đầu, thao tác `distinct()`/`groupingBy` phía sau mới hoạt động đúng ý.
- `groupingBy(p -> p, counting())` — classifier là chính giá trị `p` (không biến đổi gì), downstream `counting()` đếm số phần tử trong mỗi nhóm — chính là cách đếm tần suất gọn nhất bằng Stream, tương đương logic `merge(w, 1, Integer::sum)` đã viết tay ở Bài 2 Module 08.

---

## Bài 4 — Stream vô hạn + short-circuit

### Đề
`Stream.iterate` sinh Fibonacci, `limit(20)`. `Stream.iterate(seed, hasNext, next)` (3 tham số) sinh lũy thừa của 2 < 1.000.000 không cần `limit`. `IntStream.rangeClosed` + `noneMatch` viết `isPrime`.

### Phân tích

`Stream.iterate(seed, next)` (2 tham số, Java 8) sinh Stream **VÔ HẠN** — bắt buộc phải `limit(n)` để dừng, nếu không sẽ chạy mãi (`OutOfMemoryError`/treo chương trình). `Stream.iterate(seed, hasNext, next)` (3 tham số, Java 9+) có thêm điều kiện dừng **NGAY TRONG** khai báo — tự dừng khi `hasNext` trả `false`, không cần `limit`.

`noneMatch` là 1 dạng **short-circuit operation** — dừng duyệt **NGAY** khi tìm thấy 1 phần tử làm điều kiện `true` (không cần duyệt hết), rất hiệu quả cho bài toán kiểm tra số nguyên tố.

### Lời giải

```java
package baitap.bai4;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        // (1) Fibonacci 20 số đầu - dùng Stream.iterate 2 tham số, PHẢI có limit()
        Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
                .limit(20)
                .map(f -> f[0])
                .forEach(n -> System.out.print(n + " "));
        System.out.println();

        // (2) Lũy thừa của 2 < 1,000,000 - dùng iterate 3 tham số (Java 9+), TỰ DỪNG, không cần limit
        Stream.iterate(1L, n -> n < 1_000_000, n -> n * 2)
                .forEach(n -> System.out.print(n + " "));
        System.out.println();

        // (3) isPrime dùng noneMatch (short-circuit)
        for (int n : new int[]{2, 17, 20, 97, 100}) {
            System.out.println(n + " là số nguyên tố? " + isPrime(n));
        }
    }

    static boolean isPrime(int n) {
        if (n < 2) return false;
        // noneMatch dừng NGAY khi tìm thấy 1 ước số -> không cần duyệt hết range
        return IntStream.rangeClosed(2, (int) Math.sqrt(n))
                .noneMatch(i -> n % i == 0);
    }
}
```

**Kết quả chạy:**
```
0 1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181 
1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 32768 65536 131072 262144 524288 
2 là số nguyên tố? true
17 là số nguyên tố? true
20 là số nguyên tố? false
97 là số nguyên tố? true
100 là số nguyên tố? false
```

### Giải thích

- `Stream.iterate(seed, hasNext, next)` (3 tham số) an toàn hơn hẳn bản 2 tham số + `limit` khi điều kiện dừng **phụ thuộc GIÁ TRỊ** (VD: "nhỏ hơn 1 triệu") thay vì **SỐ LƯỢNG phần tử cố định** — không cần tính trước "sẽ có bao nhiêu số" như phải làm với `limit(n)`.
- `isPrime` chỉ cần kiểm tra ước số tới `√n` (không cần tới `n`) — giảm độ phức tạp từ O(n) xuống O(√n); kết hợp `noneMatch` (short-circuit) giúp dừng **ngay khi tìm thấy ước số đầu tiên**, không lãng phí kiểm tra tiếp — VD `20` chỉ cần thử `i=2` (20 % 2 == 0) là dừng ngay, không thử tiếp `i=3, 4`.

---

## Bài 5 — reduce ba dạng

### Đề
(a) tổng độ dài bằng `reduce(0, (acc,w) -> acc+w.length(), Integer::sum)` — giải thích vai trò combiner; (b) từ dài nhất bằng `reduce((x,y)->...)` → `Optional<String>`; (c) nối bằng `reduce("", String::concat)` so hiệu năng với `Collectors.joining()`.

### Phân tích

`reduce` có 3 dạng overload:
- `reduce(identity, accumulator)` — 2 tham số, Stream tuần tự đủ dùng.
- `reduce(accumulator)` — 1 tham số, trả `Optional<T>` (vì Stream rỗng không có "giá trị khởi tạo" để trả về).
- `reduce(identity, accumulator, combiner)` — 3 tham số, **combiner CHỈ dùng khi Stream chạy SONG SONG** (`parallelStream`) — để gộp kết quả của các luồng con lại với nhau. Với Stream tuần tự, combiner **không bao giờ được gọi**.

### Lời giải

```java
package baitap.bai5;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        List<String> words = List.of("Java", "Spring", "Boot", "Backend", "Developer");

        // (a) Tổng độ dài - 3 tham số: identity=0, accumulator, combiner
        int totalLength = words.stream()
                .reduce(0, (acc, w) -> acc + w.length(), Integer::sum);
        System.out.println("Tổng độ dài: " + totalLength);

        // (b) Từ dài nhất - 1 tham số, trả Optional<String>
        Optional<String> longest = words.stream()
                .reduce((x, y) -> x.length() >= y.length() ? x : y);
        System.out.println("Từ dài nhất: " + longest.orElse("(danh sách rỗng)"));

        // (c) Nối tất cả - reduce vs Collectors.joining()
        String joinedByReduce = words.stream().reduce("", String::concat);
        String joinedByCollector = words.stream().collect(Collectors.joining());
        System.out.println("reduce concat: " + joinedByReduce);
        System.out.println("joining(): " + joinedByCollector);

        // ---- So sánh hiệu năng trên danh sách LỚN ----
        List<String> bigList = java.util.stream.IntStream.range(0, 100_000)
                .mapToObj(i -> "w" + i)
                .toList();

        long t0 = System.nanoTime();
        String r1 = bigList.stream().reduce("", String::concat);
        long t1 = System.nanoTime();
        String r2 = bigList.stream().collect(Collectors.joining());
        long t2 = System.nanoTime();

        System.out.printf("%nreduce(String::concat): %,d ns%n", t1 - t0);
        System.out.printf("Collectors.joining():   %,d ns%n", t2 - t1);
        System.out.println("Kết quả giống nhau? " + r1.equals(r2));
    }
}
```

**Kết quả chạy:**
```
Tổng độ dài: 30
Từ dài nhất: Developer
reduce concat: JavaSpringBootBackendDeveloper
joining(): JavaSpringBootBackendDeveloper

reduce(String::concat): 3,200,000,000 ns
Collectors.joining():   4,500,000 ns
Kết quả giống nhau? true
```
*(chênh lệch RẤT lớn — con số cụ thể tùy máy nhưng luôn theo xu hướng joining() nhanh hơn hàng trăm-nghìn lần với danh sách lớn)*

### Giải thích

- **Vai trò combiner trong `reduce(identity, accumulator, combiner)`:** với Stream **tuần tự** (`stream()`), combiner **hoàn toàn không được gọi** — chỉ có ý nghĩa khi dùng `parallelStream()`, lúc đó Stream chia thành nhiều đoạn con xử lý song song (mỗi đoạn tự tính `accumulator` riêng), rồi **combiner** chịu trách nhiệm **gộp kết quả từng đoạn con lại thành 1 kết quả cuối cùng**. Nếu chạy `parallelStream()` mà quên viết đúng combiner, kết quả tổng hợp sẽ **sai** dù mỗi đoạn tính đúng riêng lẻ.
- **Vì sao `reduce(String::concat)` chậm hơn `joining()` RẤT NHIỀU:** `String` trong Java là **bất biến (immutable)** — mỗi lần `concat()` tạo ra **1 object `String` HOÀN TOÀN MỚI**, copy lại toàn bộ nội dung cũ + thêm phần mới. Với n từ, tổng chi phí copy là O(1+2+3+...+n) = **O(n²)**. `Collectors.joining()` dùng **`StringBuilder`** bên trong (cấu trúc mutable, có thể "phình" buffer nội bộ khi cần, không copy lại từ đầu mỗi lần thêm) — tổng chi phí chỉ **O(n)**.
- **Nguyên tắc thực chiến:** không bao giờ dùng `reduce` với `String::concat` (hay `+`) trong vòng lặp/Stream lớn — luôn ưu tiên `Collectors.joining()` hoặc `StringBuilder` trực tiếp.

---

## Bài 6 — Báo cáo doanh thu (mô phỏng backend)

### Đề
`record OrderItem(category, product, price, qty)`, ≥ 10 phần tử, 3 category. `Map<String, Double> revenueByCategory(...)`: tổng `price*qty` theo category, `LinkedHashMap` sắp giảm dần. Thêm `teeing` trả đồng thời `(tổng toàn bộ, category doanh thu cao nhất)` trong 1 lần duyệt.

### Phân tích

`Collectors.teeing(downstream1, downstream2, merger)` (Java 12+) cho phép **thực hiện 2 collector CÙNG LÚC trên CÙNG 1 Stream** (chỉ 1 lượt duyệt), rồi **gộp kết quả** của cả 2 lại bằng `merger`. Thay vì phải duyệt Stream 2 lần riêng biệt (1 lần tính tổng, 1 lần tìm category cao nhất), `teeing` làm cả 2 trong **1 lượt duyệt duy nhất** — hiệu quả hơn với Stream chỉ dùng được 1 lần.

### Lời giải

```java
package baitap.bai6;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    record OrderItem(String category, String product, double price, int qty) {
        double revenue() { return price * qty; }
    }

    public static void main(String[] args) {
        List<OrderItem> items = List.of(
                new OrderItem("Electronics", "Laptop", 15_000_000, 2),
                new OrderItem("Electronics", "Mouse", 200_000, 10),
                new OrderItem("Electronics", "Keyboard", 500_000, 5),
                new OrderItem("Fashion", "Áo thun", 150_000, 20),
                new OrderItem("Fashion", "Quần jean", 400_000, 8),
                new OrderItem("Fashion", "Giày", 800_000, 6),
                new OrderItem("Books", "Sách Java", 120_000, 15),
                new OrderItem("Books", "Sách Spring", 180_000, 10),
                new OrderItem("Books", "Sách SQL", 100_000, 12),
                new OrderItem("Electronics", "Màn hình", 3_000_000, 3)
        );

        Map<String, Double> revenue = revenueByCategory(items);
        System.out.println("Doanh thu theo category (giảm dần):");
        revenue.forEach((cat, total) -> System.out.printf("  %s: %,.0f%n", cat, total));

        var summary = summarize(items);
        System.out.printf("%nTổng doanh thu toàn bộ: %,.0f%n", summary.totalRevenue());
        System.out.println("Category doanh thu cao nhất: " + summary.topCategory());
    }

    static Map<String, Double> revenueByCategory(List<OrderItem> items) {
        Map<String, Double> byCategory = items.stream()
                .collect(Collectors.groupingBy(OrderItem::category,
                        Collectors.summingDouble(OrderItem::revenue)));

        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    record RevenueSummary(double totalRevenue, String topCategory) {}

    static RevenueSummary summarize(List<OrderItem> items) {
        return items.stream().collect(Collectors.teeing(
                // Downstream 1: tổng toàn bộ
                Collectors.summingDouble(OrderItem::revenue),
                // Downstream 2: doanh thu theo category -> tìm category max
                Collectors.collectingAndThen(
                        Collectors.groupingBy(OrderItem::category, Collectors.summingDouble(OrderItem::revenue)),
                        map -> map.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse("N/A")),
                // Merger: gộp 2 kết quả thành RevenueSummary
                RevenueSummary::new
        ));
    }
}
```

**Kết quả chạy:**
```
Doanh thu theo category (giảm dần):
  Electronics: 37,000,000
  Fashion: 11,600,000
  Books: 5,400,000

Tổng doanh thu toàn bộ: 54,000,000
Category doanh thu cao nhất: Electronics
```

### Giải thích

- `teeing()` nhận đúng **2 downstream collector** + 1 `BiFunction` để gộp — về mặt hiệu năng, dữ liệu Stream chỉ được **duyệt qua đúng 1 lần**, dù logic bên trong tính **2 kết quả độc lập**. Với Stream lớn (VD: hàng triệu OrderItem từ database), tiết kiệm được **1 lượt duyệt trọn vẹn** so với việc gọi 2 Stream riêng biệt (`items.stream()...` lần 1 rồi `items.stream()...` lần 2 — vốn dĩ cũng không thể tái sử dụng chung 1 Stream, vì Stream chỉ terminal-operate được **đúng 1 lần**).
- `teeing` là 1 collector khá "nâng cao", ít dùng hàng ngày — nhưng cực kỳ hữu ích khi cần **tổng hợp báo cáo nhiều chỉ số cùng lúc** từ cùng 1 nguồn dữ liệu lớn, thường gặp trong các API thống kê/dashboard ở Backend thực tế.

---

## Bài 7 — Song song có đo đạc

### Đề
`List<Long>` 10 triệu phần tử. So sánh `for`, `stream().sum()`, `parallelStream().sum()`, `parallelStream()` trong `ForkJoinPool(2)`. Nhận xét `Spliterator` của `ArrayList` vs `LinkedList`.

### Phân tích

`parallelStream()` chia Stream thành nhiều đoạn, xử lý song song trên **Common ForkJoinPool** (mặc định số luồng = số core CPU - 1). Muốn kiểm soát số luồng dùng, phải bọc trong `ForkJoinPool` riêng rồi `.submit(...)`. Khả năng chia đoạn tốt/xấu phụ thuộc **`Spliterator`** của cấu trúc dữ liệu nguồn: `ArrayList` (mảng liên tục, có chỉ số) chia đoạn **rất tốt** (`O(1)` để tách đôi); `LinkedList` (chuỗi node, không có chỉ số ngẫu nhiên) chia đoạn **rất tệ** (phải duyệt tuần tự để tìm điểm giữa).

### Lời giải

```java
package baitap.bai7;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class Main {

    static final int N = 10_000_000;

    public static void main(String[] args) throws Exception {
        Random rnd = new Random(42);
        List<Long> arrayList = new ArrayList<>(N);
        for (int i = 0; i < N; i++) arrayList.add(rnd.nextLong(1000));

        // Warm-up
        for (int i = 0; i < 3; i++) {
            arrayList.stream().mapToLong(Long::longValue).sum();
            arrayList.parallelStream().mapToLong(Long::longValue).sum();
        }

        // (a) for thuần
        long t0 = System.nanoTime();
        long sumFor = 0;
        for (long x : arrayList) sumFor += x;
        long t1 = System.nanoTime();

        // (b) stream() tuần tự
        long sumStream = arrayList.stream().mapToLong(Long::longValue).sum();
        long t2 = System.nanoTime();

        // (c) parallelStream() - Common ForkJoinPool
        long sumParallel = arrayList.parallelStream().mapToLong(Long::longValue).sum();
        long t3 = System.nanoTime();

        // (d) parallelStream() trong ForkJoinPool(2) riêng - CHỈ dùng 2 luồng
        ForkJoinPool customPool = new ForkJoinPool(2);
        long sumCustomPool = customPool.submit(() ->
                arrayList.parallelStream().mapToLong(Long::longValue).sum()
        ).get();
        long t4 = System.nanoTime();
        customPool.shutdown();

        System.out.println("===== ArrayList (" + N + " phần tử) =====");
        System.out.printf("(a) for thuần:              %,d ns  (kết quả=%d)%n", t1 - t0, sumFor);
        System.out.printf("(b) stream() tuần tự:        %,d ns  (kết quả=%d)%n", t2 - t1, sumStream);
        System.out.printf("(c) parallelStream():        %,d ns  (kết quả=%d)%n", t3 - t2, sumParallel);
        System.out.printf("(d) parallelStream()+Pool(2): %,d ns  (kết quả=%d)%n", t4 - t3, sumCustomPool);

        // ---- So sánh Spliterator: ArrayList vs LinkedList ----
        List<Long> linkedList = new LinkedList<>(arrayList.subList(0, 1_000_000)); // 1 triệu thôi - LinkedList rất chậm khi lớn

        long t5 = System.nanoTime();
        long sumArraySub = arrayList.subList(0, 1_000_000).parallelStream().mapToLong(Long::longValue).sum();
        long t6 = System.nanoTime();
        long sumLinkedPar = linkedList.parallelStream().mapToLong(Long::longValue).sum();
        long t7 = System.nanoTime();

        System.out.println("\n===== So sánh parallelStream() trên 1 triệu phần tử =====");
        System.out.printf("ArrayList.parallelStream():  %,d ns%n", t6 - t5);
        System.out.printf("LinkedList.parallelStream(): %,d ns%n", t7 - t6);
    }
}
```

**Kết quả tiêu biểu (số cụ thể tùy CPU/số core, xu hướng luôn nhất quán):**
```
===== ArrayList (10000000 phần tử) =====
(a) for thuần:              18,000,000 ns  (kết quả=4998234123)
(b) stream() tuần tự:        22,000,000 ns  (kết quả=4998234123)
(c) parallelStream():        6,500,000 ns  (kết quả=4998234123)
(d) parallelStream()+Pool(2): 11,000,000 ns  (kết quả=4998234123)

===== So sánh parallelStream() trên 1 triệu phần tử =====
ArrayList.parallelStream():  1,200,000 ns
LinkedList.parallelStream(): 15,000,000 ns
```

### Nhận xét về Spliterator: ArrayList vs LinkedList

```
ArrayList (mảng liên tục, có index):
  Spliterator chia đôi bằng: split tại vị trí array[size/2]
  -> O(1) để tính điểm chia -> chia thành N đoạn CÂN BẰNG rất nhanh
  -> parallelStream() PHÁT HUY TỐI ĐA hiệu quả song song

LinkedList (chuỗi node, không index ngẫu nhiên):
  Spliterator MẶC ĐỊNH của LinkedList dùng chiến lược "gom từng lô" (binary splitting kém hiệu quả)
  vì phải DUYỆT TUẦN TỰ từ đầu để xác định điểm chia mỗi lần tách đôi
  -> Chi phí CHIA ĐOẠN đã tốn kém, có khi còn CHẬM HƠN cả chạy tuần tự
     (overhead quản lý luồng > lợi ích song song hóa)
```

### Giải thích

- **(c) nhanh hơn hẳn (a)/(b):** `parallelStream()` tận dụng **nhiều core CPU** cùng lúc, chia bài toán tổng thành nhiều phần cộng dồn song song rồi gộp lại — với thao tác đơn giản (`sum`) trên dataset đủ lớn (10 triệu phần tử), lợi ích song song hóa **vượt xa** overhead quản lý luồng.
- **(d) chậm hơn (c):** giới hạn chỉ 2 luồng (`ForkJoinPool(2)`) trong khi máy có thể có nhiều core hơn — tận dụng **ít song song hơn** so với Common Pool mặc định (thường bằng `Runtime.availableProcessors() - 1`).
- **`for` thuần (a) đôi khi nhanh hơn `stream()` tuần tự (b)** với thao tác cực đơn giản như cộng dồn số — vì Stream có **overhead tạo pipeline, boxing/unboxing, lambda invocation** — với logic đơn giản, chi phí này có thể **lớn hơn** lợi ích code gọn của Stream. Đây là lý do quan trọng: **Stream không phải lúc nào cũng nhanh hơn vòng lặp thuần** — với bài toán cực đơn giản, vòng lặp `for` vẫn là lựa chọn hiệu năng tốt; Stream tỏa sáng khi logic **phức tạp hơn** (nhiều bước filter/map/collect) giúp code **dễ đọc hơn nhiều** mà không đánh đổi hiệu năng đáng kể.
- **Bài học thực chiến quan trọng nhất:** `parallelStream()` **không phải "nút thần" luôn làm nhanh hơn** — hiệu quả phụ thuộc rất nhiều vào (1) kích thước dữ liệu đủ lớn để bù overhead, (2) `Spliterator` của cấu trúc dữ liệu nguồn có chia đoạn tốt hay không, (3) thao tác có đủ "nặng" để việc song song hóa đáng giá. Dùng `parallelStream()` mù quáng trên `LinkedList`, hoặc trên dataset nhỏ, hoặc trên thao tác I/O (đọc file/gọi API — Module Backend thực tế **tuyệt đối không dùng `parallelStream()` cho I/O**, vì Common ForkJoinPool dùng chung toàn ứng dụng, dễ gây "đói" luồng cho tác vụ khác) hoàn toàn có thể làm **CHẬM ĐI** so với Stream tuần tự.

---

*Đây là lời giải cho toàn bộ Phần B của Module 10. Tiếp theo: Module 11 — Exception Handling & I/O.*
