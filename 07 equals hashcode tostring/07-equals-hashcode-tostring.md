# Module 02.4 — equals(), hashCode(), toString()

> **Mức độ ưu tiên: Trung bình** — Không "hot" bằng SOLID hay đa hình trong phỏng vấn lý thuyết, nhưng lại là nguyên nhân của **rất nhiều bug runtime khó hiểu nhất** trong thực tế (object bị "biến mất" khỏi `HashSet`, `HashMap` trả về sai kết quả...). Bắt buộc phải nắm chắc trước khi làm việc với Collections nâng cao và Entity trong JPA/Hibernate (Module 11).

---

## Mục lục

1. [`toString()` — biểu diễn object dưới dạng chuỗi](#1-tostring--biểu-diễn-object-dưới-dạng-chuỗi)
2. [`equals()` — so sánh nội dung, không phải reference](#2-equals--so-sánh-nội-dung-không-phải-reference)
3. [`hashCode()` — nền tảng của HashMap/HashSet](#3-hashcode--nền-tảng-của-hashmaphashset)
4. [Contract bắt buộc giữa equals() và hashCode()](#4-contract-bắt-buộc-giữa-equals-và-hashcode)
5. [Hậu quả khi vi phạm Contract — minh họa bằng HashMap thực tế](#5-hậu-quả-khi-vi-phạm-contract--minh-họa-bằng-hashmap-thực-tế)
6. [`record` (Java 16+) — tự động sinh equals/hashCode/toString](#6-record-java-16--tự-động-sinh-equalshashcodetostring)
7. [Comparable vs Comparator](#7-comparable-vs-comparator)
8. [Tổng kết — Bảng ghi nhớ nhanh](#8-tổng-kết--bảng-ghi-nhớ-nhanh)
9. [Bài tập luyện tập](#9-bài-tập-luyện-tập)

---

## 1. `toString()` — biểu diễn object dưới dạng chuỗi

Mọi class trong Java **ngầm kế thừa** `toString()` từ `java.lang.Object` (nhắc lại từ Module 02.1). Mặc định, nó trả về chuỗi dạng `TênClass@hashCodeDạngHex` — gần như **vô dụng** để debug:

```java
public class Student {
    String name;
    int age;
}

Student s = new Student();
s.name = "Pho";
s.age = 22;
System.out.println(s); // Student@1b6d3586 — không cho biết gì về nội dung thực sự!
```

### Override `toString()` để in ra thông tin có ý nghĩa

```java
public class Student {
    private String name;
    private int age;

    @Override
    public String toString() {
        return "Student{name='" + name + "', age=" + age + "}";
    }
}
```

```java
System.out.println(s); // Student{name='Pho', age=22}
```

> **Khi nào `toString()` được gọi ngầm định?** Khi dùng `System.out.println(object)`, khi nối chuỗi (`"Info: " + object`), khi log (`logger.info("User: {}", user)`), hoặc khi debug trong IDE. **Luôn nên override `toString()`** cho mọi class có ý nghĩa nghiệp vụ — giúp log và debug dễ dàng hơn rất nhiều trong dự án backend thực tế.

> ⚠️ **Lưu ý bảo mật:** không đưa các field nhạy cảm (password, token, số thẻ ngân hàng...) vào `toString()` — vì log hệ thống rất dễ vô tình in ra thông tin nhạy cảm này.

---

## 2. `equals()` — so sánh nội dung, không phải reference

Mặc định, `equals()` kế thừa từ `Object` chỉ so sánh **reference** (giống hệt `==`):

```java
public class Student {
    String name;
}

Student s1 = new Student(); s1.name = "Pho";
Student s2 = new Student(); s2.name = "Pho";

System.out.println(s1 == s2);       // false — 2 object khác nhau trên Heap
System.out.println(s1.equals(s2));  // false! — vì chưa override, mặc định equals() = so sánh reference giống ==
```

Đây là lý do **bắt buộc phải override `equals()`** nếu muốn so sánh 2 object dựa trên **nội dung** thay vì địa chỉ bộ nhớ.

### Override `equals()` đúng chuẩn

```java
public class Student {
    private String studentId;
    private String name;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;                          // (1) cùng reference → chắc chắn bằng nhau, tối ưu tốc độ
        if (obj == null || getClass() != obj.getClass()) return false; // (2) null hoặc khác kiểu class → chắc chắn không bằng
        Student other = (Student) obj;                          // (3) ép kiểu an toàn (đã kiểm tra getClass() ở trên)
        return Objects.equals(studentId, other.studentId);       // (4) so sánh field then chốt (dùng Objects.equals để an toàn với null)
    }
}
```

### Vì sao dùng `Objects.equals(a, b)` thay vì `a.equals(b)` trực tiếp?

```java
Objects.equals(studentId, other.studentId)
// tương đương với:
(studentId == other.studentId) || (studentId != null && studentId.equals(other.studentId))
```
`Objects.equals()` (trong `java.util.Objects`) **tự động xử lý trường hợp `null`** — tránh `NullPointerException` nếu `studentId` là `null`. Đây là cách viết an toàn và ngắn gọn hơn nhiều so với tự viết kiểm tra `null` thủ công.

### `equals()` nên dựa vào field nào?

Chỉ nên dựa vào field **định danh nghiệp vụ (business identity)** — ví dụ `studentId` (mã số sinh viên duy nhất) — **không nhất thiết** phải so sánh **tất cả** field. So sánh 2 sinh viên trùng mã số sinh viên coi như "cùng 1 người", dù các field khác (điểm GPA, ngày cập nhật...) có thể khác nhau tại các thời điểm khác nhau.

---

## 3. `hashCode()` — nền tảng của HashMap/HashSet

`hashCode()` trả về một số nguyên (`int`) đại diện cho object, dùng để **xác định nhanh "cái giỏ" (bucket)** mà object đó nên nằm trong các cấu trúc dữ liệu dựa trên hash: `HashMap`, `HashSet`, `Hashtable`.

### Cơ chế hoạt động của HashMap (tóm tắt để hiểu vì sao hashCode() quan trọng)

```
1. Khi put(key, value): JVM gọi key.hashCode() → xác định bucket (ô nhớ) để lưu
2. Khi get(key): JVM gọi key.hashCode() → tìm ĐÚNG bucket đó → 
   rồi dùng key.equals() để so sánh, tìm ĐÚNG entry trong bucket (vì 1 bucket có thể chứa nhiều entry do trùng hashCode — gọi là "hash collision")
```

```java
public class Student {
    private String studentId;

    @Override
    public int hashCode() {
        return Objects.hash(studentId); // dùng Objects.hash() — tự tính hashCode dựa trên các field truyền vào, xử lý null an toàn
    }
}
```

### `Objects.hash(...)` hoạt động như thế nào?

```java
Objects.hash(field1, field2, field3);
// Tương đương về ý tưởng với:
int result = 1;
result = 31 * result + (field1 == null ? 0 : field1.hashCode());
result = 31 * result + (field2 == null ? 0 : field2.hashCode());
// ...
```
Số `31` được chọn vì là số nguyên tố lẻ, giúp phân tán giá trị hash đều hơn, giảm tỷ lệ trùng lặp (hash collision) trong thực tế — đây là công thức chuẩn IDE (IntelliJ, Eclipse) hay sinh tự động khi bấm "Generate equals() and hashCode()".

---

## 4. Contract bắt buộc giữa equals() và hashCode()

Đây là phần **quan trọng nhất** của cả module — một quy tắc **bắt buộc tuân thủ**, được định nghĩa chính thức trong Javadoc của `Object.hashCode()`:

> ### Quy tắc vàng (General Contract):
> **Nếu `a.equals(b)` trả về `true`, thì BẮT BUỘC `a.hashCode() == b.hashCode()` cũng phải đúng.**
>
> (Chiều ngược lại **KHÔNG** bắt buộc: 2 object có `hashCode()` giống nhau **không nhất thiết** phải `equals()` — đây gọi là "hash collision", là điều bình thường và HashMap có cơ chế xử lý riêng cho trường hợp này bằng `equals()`.)

### Tại sao contract này tồn tại?

Vì cơ chế `HashMap`/`HashSet` dựa vào giả định: **2 object "bằng nhau" (theo equals) phải luôn nằm trong cùng 1 bucket** (tức có cùng hashCode) để có thể tìm thấy được. Nếu vi phạm — override `equals()` mà **quên** override `hashCode()` tương ứng — sẽ dẫn đến lỗi cực kỳ khó phát hiện (xem ví dụ minh họa ở mục 5).

### Quy tắc thực hành: LUÔN override CẢ HAI cùng lúc

> ⚠️ **Không bao giờ chỉ override `equals()` mà bỏ qua `hashCode()`, hoặc ngược lại.** Hầu hết IDE hiện đại (IntelliJ IDEA, Eclipse, VS Code với Java extension) có tính năng **"Generate equals() and hashCode()"** sinh code chuẩn tự động cùng lúc — nên tận dụng thay vì tự viết tay để tránh sai sót.

---

## 5. Hậu quả khi vi phạm Contract — minh họa bằng HashMap thực tế

```java
public class Student {
    private String studentId;
    private String name;

    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) { // ✅ CÓ override equals()
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Student other = (Student) obj;
        return Objects.equals(studentId, other.studentId);
    }

    // ❌ QUÊN override hashCode() — vẫn dùng bản mặc định từ Object (dựa trên địa chỉ bộ nhớ)
}
```

```java
Set<Student> students = new HashSet<>();
Student s1 = new Student("SV001", "Pho");
students.add(s1);

Student s2 = new Student("SV001", "Pho"); // CÙNG studentId — equals() sẽ trả về true

System.out.println(s1.equals(s2));           // true — theo logic equals() đã viết
System.out.println(students.contains(s2));   // FALSE! — Bug khó hiểu!
```

**Vì sao `contains(s2)` trả về `false` dù `equals()` là `true`?**

Vì `s1` và `s2` có `hashCode()` **khác nhau** (do dùng bản mặc định dựa trên địa chỉ bộ nhớ, mà `s1` và `s2` là 2 object khác nhau trên Heap). `HashSet` khi gọi `contains(s2)` trước tiên tính `s2.hashCode()` để tìm bucket — nhưng `s1` lại nằm ở **bucket khác** (vì `s1.hashCode() != s2.hashCode()`) — nên `HashSet` **thậm chí không đi tìm đến bucket chứa `s1`** để so sánh `equals()`, dẫn đến kết luận sai là "không tìm thấy".

> **Đây chính xác là loại bug âm thầm, khó debug nhất trong Java** — code compile bình thường, `equals()` hoạt động đúng khi test riêng lẻ, nhưng logic dùng `HashMap`/`HashSet` lại âm thầm sai theo cách khó phát hiện. Luôn nhớ: **override cả 2 cùng lúc, không bao giờ chỉ 1 trong 2.**

---

## 6. `record` (Java 16+) — tự động sinh equals/hashCode/toString

Java 16 giới thiệu `record` — một cách khai báo class dữ liệu (data class) cực kỳ gọn, **tự động sinh sẵn** `equals()`, `hashCode()`, `toString()` đúng chuẩn dựa trên toàn bộ các field khai báo, không cần viết tay:

```java
public record StudentDTO(String studentId, String name, int age) {
    // KHÔNG cần viết equals(), hashCode(), toString() — Java tự sinh đầy đủ, đúng chuẩn!
}
```

```java
StudentDTO s1 = new StudentDTO("SV001", "Pho", 22);
StudentDTO s2 = new StudentDTO("SV001", "Pho", 22);

System.out.println(s1.equals(s2)); // true — record tự động so sánh TẤT CẢ field
System.out.println(s1);            // StudentDTO[studentId=SV001, name=Pho, age=22] — toString() tự sinh, đọc rõ ràng
System.out.println(s1.studentId()); // Pho — record tự sinh "accessor" (không phải getStudentId(), mà là studentId())
```

> **Lưu ý:** `record` tự động dùng **TẤT CẢ field** để tính `equals()`/`hashCode()` — không tùy chỉnh chọn field nào như khi tự viết tay. `record` cực kỳ phù hợp cho **DTO (Data Transfer Object)** trong backend — sẽ dùng rất nhiều khi làm việc với Spring Boot REST API (Module 16) để truyền dữ liệu giữa các tầng mà không cần logic nghiệp vụ phức tạp.

---

## 7. Comparable vs Comparator

Cả hai đều dùng để **xác định thứ tự sắp xếp (sorting order)** cho object — nhưng phục vụ mục đích khác nhau.

### `Comparable<T>` — định nghĩa thứ tự "tự nhiên" (natural ordering) NGAY TRONG class

```java
public class Student implements Comparable<Student> {
    private String name;
    private double gpa;

    @Override
    public int compareTo(Student other) {
        return Double.compare(this.gpa, other.gpa); // sắp xếp tăng dần theo GPA
        // trả về ÂM nếu this < other, DƯƠNG nếu this > other, 0 nếu bằng nhau
    }
}
```

```java
List<Student> students = new ArrayList<>(List.of(
    new Student("Pho", 3.6), new Student("An", 3.9), new Student("Binh", 3.2)
));
Collections.sort(students); // dùng ĐÚNG compareTo() đã định nghĩa — tự động sắp theo GPA tăng dần
```

### `Comparator<T>` — định nghĩa thứ tự "bên ngoài", linh hoạt hơn, không sửa class gốc

```java
Comparator<Student> byName = (s1, s2) -> s1.getName().compareTo(s2.getName());

students.sort(byName); // sắp theo TÊN — không cần sửa gì trong class Student

// Cách viết hiện đại, ngắn gọn hơn dùng method reference (Module 03 sẽ học sâu)
students.sort(Comparator.comparing(Student::getName));

// Sắp theo GPA giảm dần, nếu bằng thì sắp theo tên tăng dần (kết hợp nhiều tiêu chí)
students.sort(
    Comparator.comparingDouble(Student::getGpa).reversed()
              .thenComparing(Student::getName)
);
```

### So sánh Comparable vs Comparator

| Tiêu chí | `Comparable` | `Comparator` |
|---|---|---|
| Vị trí định nghĩa | Ngay trong class (`implements Comparable<T>`) | Class riêng biệt hoặc lambda expression bên ngoài |
| Method | `compareTo(T other)` | `compare(T o1, T o2)` |
| Số lượng cách sắp xếp | Chỉ 1 — thứ tự "tự nhiên" duy nhất cho class | Không giới hạn — có thể định nghĩa nhiều Comparator khác nhau cho cùng 1 class |
| Khi nào dùng | Khi class có 1 thứ tự mặc định rõ ràng, hợp lý (ví dụ `String` sắp theo alphabet, `Integer` sắp theo giá trị số) | Khi cần **nhiều tiêu chí sắp xếp khác nhau** tùy ngữ cảnh sử dụng, hoặc không thể sửa class gốc (class từ thư viện ngoài) |
| Gọi sắp xếp | `Collections.sort(list)` hoặc `list.sort(null)` | `list.sort(comparator)` hoặc `Collections.sort(list, comparator)` |

> **Lưu ý thực tế:** trong dự án backend, `Comparator` được dùng **phổ biến hơn** `Comparable` vì linh hoạt hơn nhiều — đặc biệt khi sắp xếp dữ liệu theo yêu cầu động từ client (ví dụ API cho phép `?sortBy=name` hoặc `?sortBy=gpa`), không thể "cứng" 1 thứ tự duy nhất trong class Entity.

---

## 8. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| `toString()` mặc định | `ClassName@hashCodeHex` — vô nghĩa để debug, nên luôn override |
| `equals()` mặc định | So sánh reference (giống `==`) — phải override để so sánh nội dung |
| `hashCode()` mặc định | Dựa trên địa chỉ bộ nhớ — không liên quan nội dung nghiệp vụ |
| **Contract bắt buộc** | `equals() == true` → **BẮT BUỘC** `hashCode()` phải bằng nhau — luôn override cả 2 cùng lúc |
| Vi phạm contract | Object "biến mất" khỏi `HashSet`/`HashMap` dù `equals()` đúng — bug rất khó phát hiện |
| `Objects.equals()` / `Objects.hash()` | Cách viết an toàn, tự xử lý `null`, nên dùng thay vì tự viết tay |
| `record` (Java 16+) | Tự động sinh equals/hashCode/toString đúng chuẩn dựa trên mọi field — lý tưởng cho DTO |
| `Comparable` | 1 thứ tự "tự nhiên" duy nhất, định nghĩa trong chính class |
| `Comparator` | Nhiều thứ tự linh hoạt, định nghĩa ngoài class — phổ biến hơn trong thực tế backend |

---

## 9. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau in ra gì?
```java
public class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
}
Point p1 = new Point(1, 2);
Point p2 = new Point(1, 2);
System.out.println(p1.equals(p2));
System.out.println(p1 == p2);
```

**Câu 2.** Class sau vi phạm contract equals/hashCode ở đâu? Hậu quả cụ thể là gì khi dùng trong `HashSet`?
```java
public class Product {
    private String code;
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Product)) return false;
        return code.equals(((Product) obj).code);
    }
    // không override hashCode()
}
```

**Câu 3.** Đoạn `compareTo()` sau có lỗi tiềm ẩn gì? (Gợi ý: liên quan đến kiểu dữ liệu số nguyên và overflow)
```java
public class Score implements Comparable<Score> {
    int value;
    @Override
    public int compareTo(Score other) {
        return this.value - other.value; // cách viết PHỔ BIẾN nhưng có rủi ro
    }
}
```

**Câu 4.** `record` sau có cần viết thêm `equals()`/`hashCode()` không? Giải thích.
```java
public record Money(String currency, double amount) { }
```

**Câu 5.** Trong đoạn code sau, `list.sort(...)` sắp xếp theo tiêu chí gì, theo thứ tự ưu tiên nào?
```java
list.sort(Comparator.comparing(Employee::getDepartment)
                     .thenComparing(Employee::getSalary, Comparator.reverseOrder()));
```

---

### Phần B — Bài tập viết code

**Bài 1 — Override equals/hashCode/toString chuẩn cho Entity.**
Viết class `Book` với field: `isbn` (String — mã định danh duy nhất), `title`, `author`, `price`. Override `equals()`/`hashCode()` **chỉ dựa vào `isbn`** (vì đây là định danh nghiệp vụ duy nhất, 2 sách trùng ISBN coi là cùng 1 cuốn dù giá có thể khác theo thời điểm). Override `toString()` in đầy đủ thông tin trừ `price` (giả định là thông tin nhạy cảm cần ẩn khi log). Viết đoạn `main` chứng minh việc thêm 2 `Book` có cùng `isbn` vào `HashSet` chỉ giữ lại 1 phần tử.

**Bài 2 — Tái hiện và sửa lỗi "vi phạm contract".**
Viết class `Coordinate` với `equals()` override đúng chuẩn nhưng **cố tình quên** override `hashCode()`. Viết đoạn `main` chứng minh bug: thêm 1 object vào `HashSet`, sau đó tạo object mới có nội dung giống hệt, gọi `contains()` và quan sát kết quả `false` bất ngờ. Sau đó sửa lại bằng cách thêm `hashCode()` đúng chuẩn (dùng `Objects.hash()`), chạy lại để chứng minh `contains()` giờ trả về `true`.

**Bài 3 — So sánh Comparable vs Comparator trên cùng 1 bài toán.**
Viết class `Movie` với field `title`, `rating` (điểm đánh giá), `releaseYear`.
- Cho `Movie implements Comparable<Movie>`, định nghĩa thứ tự tự nhiên theo `rating` giảm dần (phim điểm cao xếp trước).
- Viết thêm 2 `Comparator<Movie>` riêng biệt: một sắp theo `title` alphabet, một sắp theo `releaseYear` tăng dần.
- Viết `main` tạo `List<Movie>`, thử sắp xếp lần lượt bằng `Collections.sort()` (dùng Comparable) và `list.sort(comparator)` (dùng từng Comparator), in kết quả để so sánh trực quan sự khác biệt.

**Bài 4 — DTO bằng record, so sánh với cách viết class truyền thống.**
Viết `record ProductDTO(String sku, String name, double price)`. Viết `main` tạo 2 instance có dữ liệu giống hệt nhau, chứng minh `equals()` trả về `true` và in `toString()` mặc định. Sau đó, viết lại **cùng chức năng** bằng class thông thường (`class ProductDTOClassic`) với `equals()`/`hashCode()`/`toString()`/constructor/getter viết tay đầy đủ — so sánh số dòng code giữa 2 cách viết để cảm nhận rõ lợi ích của `record` cho các DTO đơn giản.

**Bài 5 — Bài toán tổng hợp: sắp xếp danh sách nhân viên theo nhiều tiêu chí.**
Viết class `Employee` với field: `name`, `department`, `salary`. Viết `main` tạo danh sách ít nhất 6 nhân viên thuộc 2-3 phòng ban khác nhau, mức lương khác nhau. Yêu cầu dùng `Comparator` kết hợp `thenComparing()` để sắp xếp: **trước tiên theo `department` (alphabet tăng dần)**, **nếu cùng phòng ban thì theo `salary` giảm dần**. In danh sách trước và sau khi sắp xếp để so sánh trực quan.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. `p1.equals(p2)` → `false` (chưa override `equals()`, dùng mặc định so sánh reference); `p1 == p2` → `false` (2 object khác nhau trên Heap). Cả 2 dòng đều `false` vì bản chất giống nhau khi chưa override.
2. Vi phạm **contract equals/hashCode**: `equals()` so sánh theo `code`, nhưng `hashCode()` vẫn dùng bản mặc định của `Object` (dựa địa chỉ bộ nhớ) — 2 `Product` có cùng `code` (equals = true) nhưng khác `hashCode()`. Hậu quả: thêm vào `HashSet` rồi gọi `contains()` với 1 object khác có cùng `code` sẽ trả về `false` sai lệch — giống hệt ví dụ minh họa ở mục 5.
3. Rủi ro **integer overflow**: nếu `this.value` rất lớn (dương) và `other.value` rất nhỏ (âm), phép trừ `this.value - other.value` có thể **tràn số (overflow)**, cho ra kết quả sai dấu hoàn toàn. Cách an toàn hơn: dùng `Integer.compare(this.value, other.value)` — xử lý đúng mọi trường hợp biên mà không lo overflow.
4. **Không cần** — `record` tự động sinh `equals()`/`hashCode()` dựa trên **toàn bộ** field khai báo trong phần tham số (`currency` và `amount`), đúng chuẩn contract, không cần viết tay.
5. Sắp xếp theo `department` trước (alphabet tăng dần, mặc định của `comparing()`), nếu 2 nhân viên **cùng department** thì mới xét tiếp `salary` — nhưng theo **giảm dần** (do dùng `Comparator.reverseOrder()` trong `thenComparing`) — đây là kỹ thuật sắp xếp đa tiêu chí (multi-level sorting) rất hay dùng trong truy vấn/API thực tế.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `Objects.hash(isbn)` cho `hashCode()`; `toString()` mẫu: `"Book{isbn='" + isbn + "', title='" + title + "', author='" + author + "'}"` (không có `price`).
- **Bài 2:** Bài tập này tái hiện chính xác tình huống ở mục 5 — mục tiêu là **tự tay quan sát** bug xảy ra trước khi sửa, giúp ghi nhớ sâu hơn nhiều so với chỉ đọc lý thuyết.
- **Bài 4:** Với `record`, chỉ cần 1 dòng khai báo; với class truyền thống viết tay đầy đủ thường tốn 25-35+ dòng code (constructor, 3 getter, `equals()`, `hashCode()`, `toString()`) — con số chênh lệch này giúp cảm nhận trực quan lý do `record` trở thành lựa chọn mặc định cho DTO trong code Java hiện đại (Java 16+).
- **Bài 5:** Đây là bài toán rất sát với thực tế backend: API trả danh sách có `?sortBy=department,salary` — logic `Comparator.comparing(...).thenComparing(...)` chính là cách triển khai chuẩn cho yêu cầu sắp xếp đa cấp này.

</details>

---

*File tiếp theo trong lộ trình: **Module 03.1 — Collections Framework** (List, Set, Map — độ phức tạp Big-O và khi nào chọn cấu trúc nào).*
