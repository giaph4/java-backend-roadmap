# Module 02.4 — equals(), hashCode(), toString()

> **Mức độ ưu tiên: Trung bình–Cao** — Không "hot" bằng SOLID hay đa hình trong phỏng vấn lý thuyết, nhưng lại là nguyên nhân của **rất nhiều bug runtime khó hiểu nhất** trong thực tế: object "biến mất" khỏi `HashSet`, `HashMap.get()` trả về `null` với đúng key, `TreeSet` và `HashSet` cho kết quả khác nhau trên cùng dữ liệu, `list.sort()` ném `IllegalArgumentException`... Bắt buộc nắm chắc trước khi làm việc với Collections nâng cao (Module 03.1) và Entity trong JPA/Hibernate (Module 11).

> **Phạm vi bài này:** ba method `Object.toString()` / `Object.equals()` / `Object.hashCode()`, *contract* ràng buộc giữa chúng, `record` ở khía cạnh nó tự sinh ba method này, và hai interface sắp xếp `Comparable` / `Comparator` (vì `compareTo` cũng có contract và có quan hệ "nên nhất quán với `equals`"). **Không** đi vào: chi tiết cài đặt `HashMap` (Module 03.1), annotation JPA (Module 11), Stream/Collector (Module 03.3). Những chỗ chạm tới các chủ đề đó chỉ nêu đủ để hiểu *vì sao* ba method này quan trọng.

---

## Mục lục

1. [`toString()` — biểu diễn object dưới dạng chuỗi](#1-tostring--biểu-diễn-object-dưới-dạng-chuỗi)
2. [`equals()` — so sánh nội dung, không phải reference](#2-equals--so-sánh-nội-dung-không-phải-reference)
3. [Quan hệ tương đương — 5 tính chất bắt buộc của `equals()`](#3-quan-hệ-tương-đương--5-tính-chất-bắt-buộc-của-equals)
4. [`getClass()` vs `instanceof` — bài toán kế thừa & tính đối xứng](#4-getclass-vs-instanceof--bài-toán-kế-thừa--tính-đối-xứng)
5. [`hashCode()` — nền tảng của HashMap/HashSet](#5-hashcode--nền-tảng-của-hashmaphashset)
6. [Contract bắt buộc giữa `equals()` và `hashCode()`](#6-contract-bắt-buộc-giữa-equals-và-hashcode)
7. [Hậu quả khi vi phạm Contract — minh họa bằng HashMap thực tế](#7-hậu-quả-khi-vi-phạm-contract--minh-họa-bằng-hashmap-thực-tế)
8. [Field số thực, mảng, và field mutable trong equals/hashCode](#8-field-số-thực-mảng-và-field-mutable-trong-equalshashcode)
9. [`record` (Java 16+) — tự động sinh & các cạm bẫy](#9-record-java-16--tự-động-sinh--các-cạm-bẫy)
10. [Comparable vs Comparator](#10-comparable-vs-comparator)
11. [Contract của `compareTo` & quy tắc "nhất quán với equals"](#11-contract-của-compareto--quy-tắc-nhất-quán-với-equals)
12. [equals/hashCode cho Entity JPA/Hibernate — nhìn trước](#12-equalshashcode-cho-entity-jpahibernate--nhìn-trước)
13. [Công cụ hỗ trợ: IDE, Lombok, kiểm thử](#13-công-cụ-hỗ-trợ-ide-lombok-kiểm-thử)
14. [Tổng kết — Bảng ghi nhớ nhanh](#14-tổng-kết--bảng-ghi-nhớ-nhanh)
15. [Bài tập luyện tập](#15-bài-tập-luyện-tập)

---

## 1. `toString()` — biểu diễn object dưới dạng chuỗi

Mọi class trong Java **ngầm kế thừa** `toString()` từ `java.lang.Object` (nhắc lại từ Module 02.1). Mặc định, nó trả về `getClass().getName() + "@" + Integer.toHexString(hashCode())` — dạng `TênClass@1b6d3586` — gần như **vô dụng** để debug:

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

> ⚠️ Chuỗi `@1b6d3586` **không phải địa chỉ bộ nhớ** — đó là `hashCode()` mặc định (identity hash) in ở hệ 16. Java không cho phép đọc địa chỉ thật của object; JVM còn có thể di chuyển object trong Heap khi GC nén bộ nhớ.

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

> **Khi nào `toString()` được gọi ngầm định?** Khi `System.out.println(object)`, khi nối chuỗi (`"Info: " + object`), trong `String.valueOf(object)`, khi log (`logger.info("User: {}", user)` — SLF4J gọi `toString()` chỉ khi level log thực sự bật, nên rẻ), khi hiển thị biến trong debugger. **Luôn nên override `toString()`** cho class có ý nghĩa nghiệp vụ.

### Bốn quy tắc thực hành cho `toString()`

| Quy tắc | Lý do |
|---|---|
| **Không đưa field nhạy cảm vào** (password, token, số thẻ, khóa bí mật, PII) | Log hệ thống rất dễ vô tình phát tán. Nếu buộc phải in, hãy mask: `card='****" + last4 + "'`. |
| **Không coi định dạng `toString()` là API** | Đừng để code khác `split`/`substring`/regex trên chuỗi `toString()` của bạn. Cần dữ liệu có cấu trúc thì viết getter hoặc method riêng. Đổi format `toString()` phải là thay đổi "vô hại". |
| **Cẩn thận tham chiếu vòng (circular reference)** | `Order` chứa `List<LineItem>`, mỗi `LineItem` giữ lại `Order` cha → `toString()` gọi đệ quy lẫn nhau → `StackOverflowError`. Với quan hệ hai chiều, chỉ in `orderId` ở phía con, đừng in cả object cha. |
| **Field kiểu mảng phải dùng `Arrays.toString()`** | `int[] a` mặc định in ra `[I@1b6d3586`. Dùng `Arrays.toString(a)`, mảng lồng nhau dùng `Arrays.deepToString(a)`. |

```java
// Null-safe khi field có thể null:
return "Student{name='" + Objects.toString(name, "<none>") + "', age=" + age + "}";
```

> Với class có nhiều field, `StringBuilder` hoặc *text block* + `String.formatted()` (Java 15+) dễ đọc hơn phép `+` dài:
> ```java
> return """
>        Student{id=%s, name='%s', age=%d}""".formatted(id, name, age);
> ```

---

## 2. `equals()` — so sánh nội dung, không phải reference

Mặc định, `equals()` kế thừa từ `Object` chỉ so sánh **reference** (đúng bằng `this == obj`):

```java
Student s1 = new Student(); s1.name = "Pho";
Student s2 = new Student(); s2.name = "Pho";

System.out.println(s1 == s2);       // false — 2 object khác nhau trên Heap
System.out.println(s1.equals(s2));  // false! — chưa override, equals() mặc định == reference
```

Đây là lý do **bắt buộc override `equals()`** nếu muốn so sánh 2 object theo **nội dung**.

### Khi nào KHÔNG cần override `equals()`?

- Mỗi instance vốn dĩ là duy nhất (`Thread`, các đối tượng "hoạt động" chứ không phải "giá trị").
- Không có nhu cầu "hai instance có bằng nhau không" (nhiều lớp tiện ích, lớp điều phối).
- Lớp cha đã override `equals()` và hành vi đó đúng cho lớp con (`AbstractList`, `AbstractSet`...).
- Lớp `private`/package-private và bạn chắc chắn `equals()` không bao giờ được gọi — có thể chặn hẳn:
  ```java
  @Override public boolean equals(Object o) { throw new AssertionError(); }
  ```

### Override `equals()` đúng chuẩn — công thức chuẩn (Effective Java, Item 10)

```java
public final class Student {
    private final String studentId;   // định danh nghiệp vụ, immutable
    private final String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                       // (1) cùng reference → tối ưu, và đảm bảo phản xạ
        if (!(o instanceof Student other)) return false;  // (2) đồng thời loại null (null instanceof X == false) + sai kiểu; pattern matching Java 16+ tự cast
        return studentId.equals(other.studentId);         // (3) so sánh field định danh; ở đây studentId non-null nên .equals() trực tiếp là đủ
    }
}
```

Bốn bước:

1. `this == o` — vừa là tối ưu tốc độ, vừa bảo đảm **tính phản xạ** ngay cả khi phần so sánh field có lỗi.
2. `o instanceof Student other` — một lệnh làm ba việc: kiểm tra `null` (mọi `null instanceof X` đều `false`), kiểm tra kiểu, và **cast luôn** vào biến `other` (pattern matching for `instanceof`, Java 16+). Trước Java 16 phải viết `if (!(o instanceof Student)) return false; Student other = (Student) o;`.
3. So sánh lần lượt các field "định danh". Với field có thể `null` → `Objects.equals(a, b)`; field non-null (đã kiểm tra trong constructor) → `a.equals(b)` trực tiếp.
4. **Sắp field theo thứ tự: khác nhau nhiều nhất trước, rẻ nhất trước.** Field nào loại được nhiều cặp không-bằng nhất, hoặc so sánh nhanh nhất, đặt lên đầu để "đoản mạch" sớm.

### `equals()` nên dựa vào field nào?

Chỉ dựa vào **định danh nghiệp vụ (business identity)** — ví dụ `studentId` — **không nhất thiết** so sánh *tất cả* field. Hai sinh viên trùng mã số coi như "cùng một người", dù GPA hay `updatedAt` khác nhau tại các thời điểm khác nhau.

> Ngoại lệ: với **value object** thuần (`Money`, `Point`, `Range`, DTO) thì "định danh" chính là *toàn bộ* giá trị → so sánh mọi field. Đây đúng là lúc `record` tỏa sáng (mục 9).

### Vì sao dùng `Objects.equals(a, b)` cho field có thể null?

```java
Objects.equals(a, b)
// tương đương:
(a == b) || (a != null && a.equals(b))
```

Tự xử lý `null` cả hai vế, tránh `NullPointerException`. Ngắn và an toàn hơn tự viết kiểm tra `null` bằng tay.

> ⚠️ **Luôn ghi `@Override`.** Chữ ký đúng là `equals(Object)`. Nếu lỡ viết `equals(Student)` thì đó là **nạp chồng (overload)** một method *mới*, không phải ghi đè — `Object.equals(Object)` vẫn là bản mặc định, và `List.contains()` / `HashMap` vẫn gọi bản mặc định. `@Override` khiến trình biên dịch báo lỗi ngay nếu chữ ký sai.

---

## 3. Quan hệ tương đương — 5 tính chất bắt buộc của `equals()`

Javadoc của `Object.equals` quy định `equals()` phải định nghĩa một **quan hệ tương đương (equivalence relation)**. Vi phạm thì các lớp Collections (vốn *giả định* các tính chất này) sẽ hành xử sai một cách khó lường.

| Tính chất | Phát biểu | Bẫy thường gặp |
|---|---|---|
| **Phản xạ (reflexive)** | `x.equals(x)` luôn `true` | Hầu như không ai làm hỏng — trừ khi so sánh field kiểu số thực bằng `==` và field đang là `NaN` (xem mục 8). |
| **Đối xứng (symmetric)** | `x.equals(y)` ⇔ `y.equals(x)` | Lớp con thêm field rồi so sánh thêm field đó với lớp cha; hoặc `equals()` "nới tay" cho một kiểu khác (ví dụ cho `CaseInsensitiveString` bằng cả `String`). |
| **Bắc cầu (transitive)** | `x.equals(y)` và `y.equals(z)` ⇒ `x.equals(z)` | Kế thừa + thêm field giá trị: `ColorPoint` bỏ qua màu khi so với `Point` → `red.equals(point)` và `point.equals(blue)` nhưng `red.equals(blue)` là `false`. |
| **Nhất quán (consistent)** | Gọi nhiều lần, dữ liệu không đổi → kết quả không đổi | `equals()` phụ thuộc yếu tố **không tất định**: địa chỉ IP phân giải từ tên miền (`URL.equals` — lỗi thiết kế nổi tiếng của JDK), thời gian hiện tại, trạng thái mutable đã bị đổi. |
| **Khác null (non-nullity)** | `x.equals(null)` luôn `false`, không ném exception | Quên bước kiểm tra — nhưng dùng `o instanceof Type` thì được miễn phí. |

```java
// Vi phạm ĐỐI XỨNG — "nới tay" cho String:
public final class CIString {
    private final String s;
    @Override public boolean equals(Object o) {
        if (o instanceof CIString ci) return s.equalsIgnoreCase(ci.s);
        if (o instanceof String str)  return s.equalsIgnoreCase(str); // ❌ mầm mống lỗi
        return false;
    }
}
CIString a = new CIString("abc");
String   b = "ABC";
a.equals(b); // true
b.equals(a); // false — String.equals không biết CIString là gì
// Hậu quả: List.of(a).contains(b) và List.of(b).contains(a) cho kết quả khác nhau.
```

> **Quy tắc:** `equals()` chỉ nên chấp nhận đối tượng **cùng kiểu** (hoặc cùng "họ" được định nghĩa rõ ràng). Đừng bao giờ để nó bằng với một kiểu mà bạn không kiểm soát.

---

## 4. `getClass()` vs `instanceof` — bài toán kế thừa & tính đối xứng

Có hai cách viết bước kiểm tra kiểu, và chúng **không tương đương**:

```java
// Cách A — instanceof: chấp nhận cả lớp con
if (!(o instanceof Student other)) return false;

// Cách B — getClass: chỉ chấp nhận đúng lớp
if (o == null || getClass() != o.getClass()) return false;
Student other = (Student) o;
```

### Vấn đề: "mở rộng lớp instantiable + thêm field giá trị" phá vỡ contract

Giả sử `Point{x,y}` và `ColorPoint extends Point { color }`:

- **Dùng `instanceof`:** để `point.equals(colorPoint)` trả `true` (Point bỏ qua màu) thì `colorPoint.equals(point)` cũng phải `true` → `ColorPoint` buộc phải bỏ qua màu khi so với `Point` thuần → **mất tính bắc cầu** giữa hai `ColorPoint` khác màu và một `Point`.
- **Dùng `getClass()`:** đối xứng và bắc cầu OK, nhưng `new ColorPoint(1,2,RED).equals(new Point(1,2))` luôn `false` → một `ColorPoint` **không bao giờ** bằng `Point` dù cùng tọa độ. Điều này vi phạm tinh thần **LSP** (Module 01.6): code nhận `Point` không dùng được `ColorPoint` như một `Point` thực thụ trong ngữ cảnh so sánh.

> **Kết luận (Effective Java):** *Không có cách nào mở rộng một lớp instantiable và thêm field giá trị mà vẫn giữ trọn vẹn contract của `equals()`.* Giải pháp là **composition thay cho inheritance** (Module 01.6): cho `ColorPoint` **chứa** một `Point` và một `Color`, cộng thêm method `asPoint()` để "view" khi cần.

### Vậy nên chọn cái nào?

| Tình huống | Nên dùng |
|---|---|
| Lớp `final` (không thể bị kế thừa) | `instanceof` — gọn, không có rủi ro kế thừa, lại xử lý `null` luôn. |
| Lớp `record` | Không phải chọn — `record` ngầm `final` và tự sinh `equals()` theo kiểu `instanceof` + so sánh mọi component. |
| Lớp cho phép kế thừa nhưng **lớp con không thêm field giá trị** (chỉ thêm hành vi) | `instanceof` — mọi lớp con "bằng nhau" theo tiêu chí của lớp cha; đây là cách `AbstractList`, `AbstractSet` làm. |
| Lớp cho phép kế thừa và lớp con **có thể** thêm field giá trị | Xem lại thiết kế: nên `final` + composition. Nếu vẫn phải mở, dùng `getClass()` và chấp nhận giới hạn LSP. |
| Entity Hibernate (bị proxy hóa) | `instanceof` — vì proxy là *lớp con động* của entity, `getClass()` sẽ khác nhau (xem mục 12). |

---

## 5. `hashCode()` — nền tảng của HashMap/HashSet

`hashCode()` trả về một `int` đại diện cho object, dùng để **xác định nhanh "cái giỏ" (bucket)** mà object nên nằm trong các cấu trúc dựa trên băm: `HashMap`, `HashSet`, `Hashtable`, `LinkedHashMap`, `ConcurrentHashMap`.

### Cơ chế (tóm tắt — chi tiết ở Module 03.1)

```
put(key, value):
  1. h = key.hashCode()          → trải bit: (h ^ (h >>> 16))  → & (n-1)  → chỉ số bucket
  2. tại bucket: duyệt các entry, dùng key.equals() để xem key đã tồn tại chưa
get(key):
  1. cùng phép tính trên → ra ĐÚNG bucket
  2. trong bucket, dùng key.equals() để chọn ĐÚNG entry
     (1 bucket chứa nhiều entry khi trùng chỉ số — gọi là "hash collision", chuyện bình thường)
```

Điểm mấu chốt: **nếu `hashCode()` sai, bước 1 đã tìm nhầm bucket → bước 2 không bao giờ chạy tới object cần tìm.**

### Công thức chuẩn (Effective Java, Item 11)

```java
@Override
public int hashCode() {
    int result = Integer.hashCode(areaCode);            // field đầu tiên
    result = 31 * result + Integer.hashCode(prefix);    // mỗi field tiếp theo: result = 31*result + hash(field)
    result = 31 * result + Integer.hashCode(lineNum);
    return result;
}
```

Quy tắc tính `hash(field)` theo kiểu:

| Kiểu field | Cách lấy hash con |
|---|---|
| `int`, `short`, `byte`, `char` | `Integer.hashCode(f)` (hoặc chính `f`) |
| `long` | `Long.hashCode(f)` — **không** ép `(int) f` (mất nửa số bit) |
| `float` | `Float.hashCode(f)` |
| `double` | `Double.hashCode(f)` |
| `boolean` | `Boolean.hashCode(f)` (`f ? 1231 : 1237`) |
| Object | `Objects.hashCode(f)` (null → 0) |
| Mảng | `Arrays.hashCode(f)`; mảng lồng nhau → `Arrays.deepHashCode(f)`; nếu **mọi** phần tử đều quan trọng đừng dùng `f.hashCode()` (đó là identity hash) |

### `Objects.hash(...)` — tiện nhưng có giá

```java
@Override
public int hashCode() {
    return Objects.hash(areaCode, prefix, lineNum); // dễ đọc, đúng chuẩn
}
```

`Objects.hash(...)` bên trong chính là vòng lặp `result = 31*result + ...` ở trên. **Nhưng** nó nhận `Object...` varargs → mỗi lần gọi phải **cấp phát một mảng** và **autobox** mọi primitive. Trên đường đi nóng (hashCode của key gọi hàng triệu lần/giây) điều này đáng kể → khi đó viết tay công thức Item 11.

> Phân biệt hai method dễ nhầm:
> - `Objects.hash(a, b, c)` — **varargs**, kết hợp *nhiều* field. `Objects.hash(x)` với một tham số ≠ `x.hashCode()` (nó là `31 + x.hashCode()`).
> - `Objects.hashCode(x)` — **một** đối tượng, null-safe, trả `x == null ? 0 : x.hashCode()`.

### Vì sao là số 31?

- Số **nguyên tố lẻ** → giảm trùng lặp khi nhân dồn.
- `31 * i` được JIT tối ưu thành `(i << 5) - i` (dịch bit + trừ) — nhanh.
- Là hằng số lịch sử: `String.hashCode()` dùng đúng đa thức này: `s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]`.

### Ba lỗi kinh điển của `hashCode()`

1. **Trả hằng số** (`return 42;`): *đúng* về mặt contract nhưng biến `HashMap` thành danh sách liên kết — mọi entry vào chung một bucket, `get()` từ O(1) tụt xuống O(n) (Java 8+ có thể "tree hóa" bucket thành cây đỏ-đen nếu key `Comparable`, cứu xuống O(log n), nhưng vẫn tệ).
2. **Dùng tập field khác `equals()`**: `hashCode()` chỉ được phép dùng **tập con** các field mà `equals()` dùng — lý tưởng là **đúng bằng** tập đó. Dùng thừa field → vi phạm contract (mục 6). Dùng thiếu → vẫn hợp lệ nhưng phân bố kém.
3. **`Math.abs(hashCode()) % n`**: `hashCode()` có thể là `Integer.MIN_VALUE`, mà `Math.abs(Integer.MIN_VALUE)` vẫn là số âm → chỉ số mảng âm → `ArrayIndexOutOfBoundsException`. Dùng `Math.floorMod(hashCode(), n)` hoặc `(hashCode() & 0x7fffffff) % n`.

### Cache `hashCode` cho object bất biến (tối ưu nâng cao)

Nếu object **immutable** và `hashCode()` nằm trên đường đi nóng, có thể tính một lần rồi nhớ lại:

```java
private int hashCode; // tự động = 0 khi khởi tạo

@Override
public int hashCode() {
    int result = hashCode;
    if (result == 0) {                       // 0 = "chưa tính"
        result = Integer.hashCode(areaCode);
        result = 31 * result + Integer.hashCode(prefix);
        result = 31 * result + Integer.hashCode(lineNum);
        hashCode = result;                   // ghi nhớ (an toàn vì object bất biến)
    }
    return result;
}
```

Đây đúng là cách `String` làm (field `private int hash;`). Hệ quả nhỏ: nếu hash "thật" tình cờ bằng 0 thì lần nào cũng tính lại — chấp nhận được vì cực hiếm.

---

## 6. Contract bắt buộc giữa `equals()` và `hashCode()`

Phần **quan trọng nhất** của module — quy tắc **bắt buộc**, định nghĩa chính thức trong Javadoc `Object.hashCode()`:

> ### Quy tắc vàng (General Contract)
> 1. `hashCode()` gọi nhiều lần trên cùng object (dữ liệu không đổi) → phải trả **cùng một giá trị** trong một lần chạy chương trình.
> 2. **Nếu `a.equals(b)` là `true` thì BẮT BUỘC `a.hashCode() == b.hashCode()`.**
> 3. Nếu `a.equals(b)` là `false` thì `hashCode()` **không bắt buộc** khác nhau — nhưng khác nhau thì hiệu năng hash tốt hơn.

Chiều ngược của (2) **không** đúng: hai object cùng `hashCode()` **không nhất thiết** `equals()` — đó là *hash collision*, chuyện bình thường, `HashMap` xử lý bằng `equals()` trong bucket.

### Tại sao contract này tồn tại?

`HashMap`/`HashSet` giả định: **hai object "bằng nhau" (theo `equals`) phải nằm cùng một bucket** (tức cùng `hashCode`) thì mới tìm thấy nhau. Override `equals()` mà **quên** `hashCode()` (hoặc ngược lại) → giả định đổ vỡ → bug ở mục 7.

### Quy tắc thực hành

> ⚠️ **Luôn override CẢ HAI cùng lúc, và cho chúng dùng CÙNG tập field.** Không bao giờ chỉ một trong hai. IDE (IntelliJ, Eclipse, VS Code + Java extension) đều có "Generate equals() and hashCode()" sinh cặp code khớp nhau — hãy dùng nó thay vì viết tay từng cái.

> Hệ quả tinh tế: **nếu `equals()` dùng field mutable thì `hashCode()` cũng mutable theo** → xem mục 8 vì sao đó là nguồn bug khi object làm key.

---

## 7. Hậu quả khi vi phạm Contract — minh họa bằng HashMap thực tế

```java
public class Student {
    private String studentId;
    private String name;

    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {            // ✅ CÓ override equals()
        if (this == o) return true;
        if (!(o instanceof Student other)) return false;
        return Objects.equals(studentId, other.studentId);
    }

    // ❌ QUÊN override hashCode() — vẫn dùng bản mặc định (identity hash, khác nhau mỗi object)
}
```

```java
Set<Student> students = new HashSet<>();
Student s1 = new Student("SV001", "Pho");
students.add(s1);

Student s2 = new Student("SV001", "Pho"); // CÙNG studentId → equals() trả về true

System.out.println(s1.equals(s2));          // true
System.out.println(students.contains(s2));  // FALSE! — bug khó hiểu
System.out.println(students.contains(s1));  // true — chỉ tìm thấy đúng object đã bỏ vào

Map<Student, String> grades = new HashMap<>();
grades.put(s1, "A");
System.out.println(grades.get(s2));         // null! — dù s1.equals(s2)
```

**Vì sao?** `s1` và `s2` có `hashCode()` **khác nhau** (identity hash). `contains(s2)` tính `s2.hashCode()` → tìm bucket X; `s1` nằm ở bucket Y ≠ X. `HashSet` **không bao giờ duyệt tới bucket Y** để thử `equals()` → kết luận sai "không có".

> **Đây chính xác là loại bug âm thầm nhất trong Java:** code compile sạch, `equals()` test riêng lẻ vẫn đúng, nhưng mọi logic đi qua `HashMap`/`HashSet`/`distinct()` của Stream/`groupingBy` đều sai lệch. Ngược lại — override `hashCode()` mà quên `equals()` — cũng hỏng y hệt: hai object cùng bucket nhưng `equals()` mặc định vẫn `false`.

---

## 8. Field số thực, mảng, và field mutable trong equals/hashCode

### 8.1. Field `float` / `double` — không so sánh bằng `==`

```java
public final class Measurement {
    private final double value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Measurement m)) return false;
        return Double.compare(value, m.value) == 0;   // ✅ KHÔNG dùng value == m.value
    }
    @Override
    public int hashCode() { return Double.hashCode(value); }
}
```

Hai lý do bắt buộc dùng `Double.compare` / `Float.compare` trong `equals()`:

| Trường hợp | `a == b` | `Double.compare(a,b) == 0` | Mong muốn cho `equals` |
|---|---|---|---|
| `a = 0.0`, `b = -0.0` | `true` | `false` | Tùy quan điểm — nhưng phải **nhất quán với `hashCode()`**: `Double.hashCode(0.0) != Double.hashCode(-0.0)`, nên nếu `equals` coi chúng bằng thì contract vỡ. `Double.compare` coi chúng khác → khớp `hashCode`. |
| `a = b = Double.NaN` | `false` | `true` | `equals` **phải phản xạ**: `x.equals(x)` với `x.value = NaN`. `==` làm hỏng phản xạ; `Double.compare` cứu. |

→ `Double.compare` là lựa chọn khiến `equals()` vừa **phản xạ** vừa **nhất quán với `hashCode()`**.

### 8.2. Field kiểu mảng — `Arrays.equals`, không phải `Objects.equals`

```java
public final class Packet {
    private final byte[] payload;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Packet p)) return false;
        return Arrays.equals(payload, p.payload);   // ✅ so sánh nội dung; Objects.equals sẽ so sánh reference
    }
    @Override
    public int hashCode() { return Arrays.hashCode(payload); } // ✅ không phải payload.hashCode()
}
```

`Objects.equals(arr1, arr2)` gọi `arr1.equals(arr2)` — mà mảng **không override `equals()`** → so sánh reference → hai mảng cùng nội dung vẫn `false`. Mảng nhiều chiều: `Arrays.deepEquals` / `Arrays.deepHashCode`.

> Với `byte[]` là **bí mật** (mã băm mật khẩu, token, chữ ký HMAC), so sánh trong bối cảnh bảo mật nên dùng `MessageDigest.isEqual(a, b)` — chạy **thời gian hằng số**, không "đoản mạch" ở byte khác đầu tiên, chống *timing attack*. `Arrays.equals` đoản mạch nên **không** dùng để so sánh bí mật.

### 8.3. Field mutable trong `equals()`/`hashCode()` — quả bom hẹn giờ

```java
public class Tag {
    private String label;                 // mutable!
    public void setLabel(String l) { this.label = l; }

    @Override public boolean equals(Object o) { /* dựa trên label */ }
    @Override public int hashCode() { return Objects.hash(label); }
}
```

```java
Set<Tag> set = new HashSet<>();
Tag t = new Tag("draft");
set.add(t);                    // rơi vào bucket theo hash("draft")
t.setLabel("published");       // hashCode của t đổi, nhưng t vẫn nằm ở bucket cũ!

set.contains(t);               // false — tìm ở bucket mới, không có
set.remove(t);                 // không xóa được
for (Tag x : set) { }          // vẫn thấy t khi duyệt → "bóng ma" trong Set
```

> **Quy tắc:** field tham gia `equals()`/`hashCode()` **nên là `final` và bất biến**. Nếu object *bắt buộc* mutable, thì **đừng** đưa nó vào `HashSet`/làm key `HashMap` dựa trên các field hay đổi — hoặc chỉ dựa `equals()`/`hashCode()` vào phần **định danh không bao giờ đổi** (ví dụ `id` gán lúc tạo).

---

## 9. `record` (Java 16+) — tự động sinh & các cạm bẫy

Java 16 chính thức hóa `record` — khai báo lớp mang dữ liệu bất biến, **tự sinh**: constructor chuẩn tắc, accessor cho từng component, và `equals()` / `hashCode()` / `toString()` đúng contract dựa trên **toàn bộ** component:

```java
public record StudentDTO(String studentId, String name, int age) {
    // KHÔNG cần viết equals()/hashCode()/toString()
}
```

```java
StudentDTO a = new StudentDTO("SV001", "Pho", 22);
StudentDTO b = new StudentDTO("SV001", "Pho", 22);

a.equals(b);          // true — so sánh TẤT CẢ component
a.hashCode() == b.hashCode(); // true
a.toString();         // StudentDTO[studentId=SV001, name=Pho, age=22]
a.studentId();        // "SV001" — accessor tên là studentId(), KHÔNG phải getStudentId()
```

### `record` sinh `equals()` như thế nào?

- Kiểu `instanceof` (nên `record` ngầm `final`, không lo bài toán kế thừa ở mục 4).
- Component `float`/`double` được so bằng ngữ nghĩa `Float.compare`/`Double.compare` → `NaN` bằng `NaN`, `0.0` khác `-0.0` (đúng như mục 8.1, khỏi tự lo).
- Component kiểu Object so bằng `Objects.equals`.

### Ba cạm bẫy của `record`

**1. Component kiểu mảng — `equals()` sinh sẵn so sánh reference:**

```java
public record Tags(String name, String[] values) { }

Tags a = new Tags("x", new String[]{"a", "b"});
Tags b = new Tags("x", new String[]{"a", "b"});
a.equals(b);   // FALSE! — record dùng Objects.equals(values, ...) → so reference mảng
```

Cách xử lý: dùng `List<String>` thay cho `String[]` (khuyến nghị — bất biến hơn), **hoặc** override thủ công trong record:

```java
public record Tags(String name, String[] values) {
    @Override public boolean equals(Object o) {
        return o instanceof Tags t && name.equals(t.name) && Arrays.equals(values, t.values);
    }
    @Override public int hashCode() { return 31 * name.hashCode() + Arrays.hashCode(values); }
    @Override public String toString() { return "Tags[name=" + name + ", values=" + Arrays.toString(values) + "]"; }
}
```

**2. Không chọn được tập field.** `record` luôn dùng **mọi** component. Nếu định danh nghiệp vụ chỉ là một field (như `studentId`) và bạn muốn `equals()` bỏ qua phần còn lại → `record` **không hợp**; dùng class thường. `record` chỉ đúng cho **value object** nơi "bằng nhau" nghĩa là "mọi field bằng nhau".

**3. Override được nhưng phải giữ contract.** Có thể override bất kỳ method sinh sẵn nào trong `record`, nhưng nếu override `equals()` thì gần như luôn phải override `hashCode()` kèm theo cho khớp — `record` không "vá" giúp phần còn lại.

> `record` immutable ⇒ an toàn làm key `HashMap`, phần tử `HashSet`, không dính bẫy mục 8.3. Rất hợp cho **DTO**, khóa phức hợp (composite key), kiểu trả về nhiều giá trị. Sẽ dùng nhiều ở Module 16 (Spring REST).

---

## 10. Comparable vs Comparator

Cả hai xác định **thứ tự sắp xếp** cho object, nhưng khác vai trò.

### `Comparable<T>` — thứ tự "tự nhiên" (natural ordering), định nghĩa NGAY TRONG class

```java
public final class Student implements Comparable<Student> {
    private final String name;
    private final double gpa;

    @Override
    public int compareTo(Student other) {
        return Double.compare(this.gpa, other.gpa); // tăng dần theo GPA
        // < 0 nếu this đứng trước; > 0 nếu this đứng sau; 0 nếu "ngang hàng"
    }
}
```

```java
List<Student> students = new ArrayList<>(List.of(
    new Student("Pho", 3.6), new Student("An", 3.9), new Student("Binh", 3.2)));
Collections.sort(students);           // dùng compareTo() — GPA tăng dần
students.sort(null);                  // tương đương — null nghĩa là "dùng natural ordering"
var min = Collections.min(students);  // cũng dựa compareTo()
TreeSet<Student> ranked = new TreeSet<>(students); // TreeSet/TreeMap CẦN Comparable (hoặc Comparator)
```

Các kiểu JDK đã `Comparable` sẵn: `String` (từ điển theo UTF-16), số bao (`Integer`, `Double`...), `BigDecimal`, `BigInteger`, `LocalDate`/`LocalDateTime`/`Instant`, `enum` (theo thứ tự khai báo — `ordinal`), `Character`, `Boolean`.

### `Comparator<T>` — thứ tự "bên ngoài", linh hoạt, không đụng class gốc

```java
Comparator<Student> byName = Comparator.comparing(Student::getName);
students.sort(byName);

// Nhiều tiêu chí: GPA giảm dần, hòa thì tên A→Z
students.sort(
    Comparator.comparingDouble(Student::getGpa).reversed()
              .thenComparing(Student::getName));

// Xử lý null an toàn: đẩy student null xuống cuối, so tiếp theo tên
students.sort(Comparator.nullsLast(Comparator.comparing(Student::getName)));
```

Nhà máy & tổ hợp hay dùng của `Comparator`:

| API | Công dụng |
|---|---|
| `comparing(keyExtractor)` | Rút một khóa `Comparable` để so. **Có boxing** nếu khóa là primitive. |
| `comparingInt` / `comparingLong` / `comparingDouble` | Như trên nhưng **không boxing** — ưu tiên khi khóa là số. |
| `thenComparing(...)` | Tiêu chí phụ khi tiêu chí trước trả 0. Nối chuỗi tùy ý. |
| `reversed()` | Đảo **toàn bộ** comparator dựng tới thời điểm gọi (xem bẫy dưới). |
| `naturalOrder()` / `reverseOrder()` | Comparator từ `Comparable` sẵn có. |
| `nullsFirst(cmp)` / `nullsLast(cmp)` | Bọc để chịu được phần tử `null`. |

> ⚠️ **Bẫy `reversed()` đảo cả chuỗi.** Muốn "tên A→Z, hòa thì tuổi lớn→nhỏ":
> ```java
> // SAI — reversed() đảo luôn cả tiêu chí tên:
> Comparator.comparing(Person::getName).thenComparing(Person::getAge).reversed();
> // ĐÚNG — chỉ đảo tiêu chí tuổi:
> Comparator.comparing(Person::getName)
>           .thenComparing(Comparator.comparing(Person::getAge).reversed());
> // hoặc:
> Comparator.comparing(Person::getName).thenComparing(Person::getAge, Comparator.reverseOrder());
> ```

### So sánh Comparable vs Comparator

| Tiêu chí | `Comparable` | `Comparator` |
|---|---|---|
| Vị trí định nghĩa | Trong chính class (`implements Comparable<T>`) | Ngoài class — lambda, biến, hoặc class riêng |
| Method | `compareTo(T other)` (1 tham số) | `compare(T a, T b)` (2 tham số) |
| Số cách sắp xếp | Đúng **1** — thứ tự "tự nhiên" duy nhất | Không giới hạn — bao nhiêu comparator cũng được |
| Sửa được class gốc? | Cần (phải `implements`) | Không cần — dùng được cho cả class thư viện ngoài |
| Gọi sắp xếp | `Collections.sort(list)`, `list.sort(null)`, `new TreeSet<>()` | `list.sort(cmp)`, `Collections.sort(list, cmp)`, `new TreeSet<>(cmp)` |
| Khi nào chọn | Class có đúng một thứ tự mặc định hiển nhiên (`String`, `Integer`, `LocalDate`) | Cần nhiều thứ tự tùy ngữ cảnh, hoặc không đụng được class gốc |

> **Thực tế backend:** `Comparator` phổ biến hơn hẳn — API thường cho client chọn `?sort=name,-createdAt`, không thể "đóng cứng" một thứ tự trong Entity. `Comparable` để dành cho các kiểu giá trị có thứ tự hiển nhiên.

### Thuật toán sắp xếp phía sau (để hiểu vì sao contract quan trọng)

- `Collections.sort` / `List.sort` / `Arrays.sort(Object[])` → **TimSort**: ổn định (*stable* — giữ nguyên thứ tự tương đối của các phần tử "ngang hàng"), ~O(n) với dữ liệu gần sắp sẵn.
- `Arrays.sort(int[]/double[]/...)` (primitive) → **dual-pivot quicksort**: **không** ổn định (nhưng primitive thì "ổn định" vô nghĩa), O(n log n) trung bình.

---

## 11. Contract của `compareTo` & quy tắc "nhất quán với equals"

`compareTo` cũng có contract bắt buộc (Javadoc `Comparable`):

1. **Phản đối xứng dấu:** `sgn(x.compareTo(y)) == -sgn(y.compareTo(x))` với mọi `x, y`. Và `x.compareTo(y)` ném exception ⇔ `y.compareTo(x)` cũng ném.
2. **Bắc cầu:** `x.compareTo(y) > 0` và `y.compareTo(z) > 0` ⇒ `x.compareTo(z) > 0`.
3. **Nhất quán trên lớp bằng nhau:** `x.compareTo(y) == 0` ⇒ `sgn(x.compareTo(z)) == sgn(y.compareTo(z))` với mọi `z`.
4. **Khuyến nghị mạnh (không bắt buộc):** `(x.compareTo(y) == 0) == x.equals(y)` — gọi là *"consistent with equals"*.

### Không dùng phép trừ để so sánh số

```java
// SAI — tràn số nguyên:
Comparator<Integer> bad = (a, b) -> a - b;   // a = 2_000_000_000, b = -2_000_000_000 → a - b tràn → âm → sai dấu
public int compareTo(Score o) { return this.value - o.value; } // cùng lỗi

// ĐÚNG:
Comparator<Integer> good = Comparator.comparingInt(Integer::intValue); // hoặc Integer::compare
public int compareTo(Score o) { return Integer.compare(this.value, o.value); }
```

Tương tự: `double` dùng `Double.compare` (còn lo cả `NaN`/`±0.0`), không dùng `d1 - d2`.

### Khi `compareTo` **không** nhất quán với `equals` — hệ quả thật

`BigDecimal` là ví dụ JDK kinh điển:

```java
BigDecimal x = new BigDecimal("1.0");
BigDecimal y = new BigDecimal("1.00");

x.equals(y);       // false — equals() so cả "scale" (số chữ số thập phân)
x.compareTo(y);    // 0     — compareTo() chỉ so giá trị số học

Set<BigDecimal> hash = new HashSet<>(); hash.add(x); hash.contains(y); // false — HashSet dùng equals()
Set<BigDecimal> tree = new TreeSet<>(); tree.add(x); tree.contains(y); // TRUE  — TreeSet dùng compareTo()

Map<BigDecimal,String> tm = new TreeMap<>(); tm.put(x, "a"); tm.put(y, "b");
tm.size(); // 1 — với TreeMap, y "trùng key" x nên ghi đè
```

> **Quy tắc:** `TreeSet`, `TreeMap`, `Collections.sort`, `SortedSet`/`SortedMap` định nghĩa "bằng nhau" **hoàn toàn bằng `compareTo`/`compare`**, phớt lờ `equals()`. Nếu comparator của bạn coi hai phần tử là "ngang hàng" thì với các cấu trúc này chúng **là một** — kể cả khi `equals()` nói khác. Hãy làm `compareTo` nhất quán với `equals` trừ khi có lý do rõ ràng (và ghi chú lại, như Javadoc `BigDecimal` đã làm).

### `IllegalArgumentException: Comparison method violates its general contract!`

TimSort **tự phát hiện** comparator mâu thuẫn (thiếu phản đối xứng hoặc thiếu bắc cầu) và ném lỗi này giữa chừng. Nguyên nhân hay gặp:

- So sánh dựa trên hiệu số bị tràn (mục trên).
- Logic kiểu "a > b thì 1, còn lại -1" — quên trả `0` khi bằng → mất phản đối xứng.
- Khóa so sánh thay đổi (mutable) trong lúc đang sort (đa luồng, hoặc lambda có side effect).

---

## 12. equals/hashCode cho Entity JPA/Hibernate — nhìn trước

> Chi tiết JPA ở **Module 11**. Ở đây chỉ chốt phần *liên quan trực tiếp tới `equals`/`hashCode`*, vì đây là nơi người mới sai nhiều nhất.

Entity JPA có ba đặc điểm phá vỡ các cách viết `equals()` "sách giáo khoa":

| Đặc điểm | Vì sao gây lỗi |
|---|---|
| `@Id` sinh tự động (`@GeneratedValue`) là **`null` trước khi persist** | Nếu `equals()`/`hashCode()` dựa trên `id`: bỏ entity (chưa lưu, `id == null`) vào `HashSet`, sau khi `save()` thì `id` được gán → `hashCode()` đổi → entity "mất tích" trong Set (đúng bẫy mục 8.3). |
| Hibernate trả về **proxy** — một lớp con động của entity | `getClass()` trên proxy ≠ `getClass()` trên entity thật → cách viết `getClass()` (mục 4) khiến entity và proxy của chính nó "không bằng nhau". |
| So sánh **tất cả field** là bất khả thi | Field `@OneToMany` lazy → gọi tới nó ngoài session ném `LazyInitializationException`; nhiều field lại mutable. |

**Cách viết được khuyến nghị (Vlad Mihalcea / Hibernate team):**

```java
@Entity
public class Customer {
    @Id @GeneratedValue
    private Long id;

    private String email;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;   // instanceof → chịu được proxy
        return id != null && id.equals(other.id);           // 2 entity CHƯA lưu (id null) → luôn KHÁC nhau
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();   // HẰNG SỐ cho mọi Customer → hashCode không bao giờ đổi dù id được gán sau
    }
}
```

Ý tưởng: `hashCode()` **hằng số** (chấp nhận mọi entity cùng loại rơi vào một bucket — tập entity trong một Set thường nhỏ nên không sao), còn `equals()` dựa trên `id` với điều kiện `id != null`. Nếu entity có **khóa nghiệp vụ tự nhiên bất biến** (mã số thuế, ISBN, email đã xác minh) thì dùng khóa đó cho **cả hai** method là sạch nhất.

---

## 13. Công cụ hỗ trợ: IDE, Lombok, kiểm thử

### IDE "Generate"

IntelliJ (`Alt+Insert` → *equals() and hashCode()*) / Eclipse sinh cặp method khớp nhau. Lưu ý các tùy chọn:
- **"Use getters"** thay vì truy cập field trực tiếp — cần khi entity bị proxy.
- **"Accept subclasses"** = dùng `instanceof` thay `getClass()`.
- Chọn **đúng tập field định danh**, đừng tick hết theo quán tính.

### Lombok

```java
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)   // chỉ field được đánh dấu mới tính
@ToString(exclude = "passwordHash")                  // loại field nhạy cảm khỏi toString
public class Account {
    @EqualsAndHashCode.Include private final String accountNumber;
    private String displayName;
    private String passwordHash;
}
```

- `@Data` = `@Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor`. **Không dùng `@Data` cho Entity JPA** — nó sinh `equals()`/`hashCode()` trên *mọi* field (dính cả ba lỗi ở mục 12) và `@Setter` phá tính bất biến.
- `@EqualsAndHashCode(callSuper = true)` khi cần gộp cả field lớp cha.
- Với `record`, Lombok gần như không cần thiết.

### Kiểm thử `equals()`/`hashCode()`

- **JUnit + AssertJ:** `assertThat(a).isEqualTo(b); assertThat(a).hasSameHashCodeAs(b);`
- **EqualsVerifier** (thư viện `nl.jqno.equalsverifier`) — kiểm tra tự động *toàn bộ* contract (phản xạ, đối xứng, bắc cầu, nhất quán, non-null, quan hệ với `hashCode`, xử lý field mảng/null):
  ```java
  EqualsVerifier.forClass(Student.class).suppress(Warning.NONFINAL_FIELDS).verify();
  ```
  Nên thêm cho mọi class có `equals()` viết tay — bắt lỗi mà mắt người hay bỏ sót.

---

## 14. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| `toString()` mặc định | `ClassName@hexHashCode` — vô nghĩa; luôn override. Không đưa field nhạy cảm; không coi format là API; mảng dùng `Arrays.toString`. |
| `equals()` mặc định | So sánh reference (`==`). Override theo công thức: `this==o` → `instanceof` (pattern) → so field định danh, field null-able dùng `Objects.equals`. Luôn `@Override`. |
| 5 tính chất | Phản xạ, đối xứng, bắc cầu, nhất quán, khác-null. Vi phạm → Collections hành xử sai khó lường. |
| `getClass()` vs `instanceof` | `final`/`record` → `instanceof`. Không có cách mở rộng lớp instantiable + thêm field giá trị mà giữ trọn contract → dùng composition. Proxy Hibernate → `instanceof`. |
| `hashCode()` mặc định | Identity hash — không liên quan nội dung. Công thức: `result = 31*result + hash(field)`. `Objects.hash(...)` tiện nhưng boxing + cấp mảng. |
| **Contract equals/hashCode** | `equals()==true` ⇒ `hashCode()` **BẮT BUỘC** bằng nhau. Override **cả hai**, dùng **cùng tập field**. |
| Vi phạm contract | Object "biến mất" khỏi `HashSet`/`HashMap`, `get()` trả `null` với đúng key — bug âm thầm nhất. |
| Field `float`/`double` | Dùng `Double.compare`/`Float.compare` trong `equals` (vì `NaN`, `±0.0`). |
| Field mảng | `Arrays.equals` + `Arrays.hashCode` (không `Objects.equals`/`.hashCode()`). |
| Field mutable | Không nên tham gia `equals`/`hashCode` nếu object dùng làm key/phần tử Set — đổi field = "bóng ma" trong Set. |
| `record` (Java 16+) | Tự sinh cả 3 method trên **mọi** component, kiểu `instanceof`, xử lý `float`/`double` đúng. Bẫy: component **mảng** so theo reference; không chọn được tập field. |
| `Comparable` | Một thứ tự "tự nhiên", `compareTo` trong class. `TreeSet`/`TreeMap` cần nó. Dùng `Integer.compare`, không dùng phép trừ. |
| `Comparator` | Nhiều thứ tự, ngoài class; `comparing`/`thenComparing`/`reversed`/`nullsLast`. Bẫy: `reversed()` đảo **cả chuỗi**. |
| `compareTo` vs `equals` | Nên "nhất quán với equals". `BigDecimal` thì không → `HashSet` và `TreeSet` cho kết quả khác nhau. `TreeSet`/`TreeMap` chỉ nhìn `compareTo`. |
| Entity JPA | `equals`: `instanceof` + `id != null && id.equals(...)`. `hashCode`: hằng số (`getClass().hashCode()`). Không `@Data`/không dùng mọi field. |
| Công cụ | IDE "Generate" (chọn đúng tập field), Lombok `@EqualsAndHashCode(onlyExplicitlyIncluded)`, kiểm thử bằng **EqualsVerifier**. |

---

## 15. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau in ra gì? Vì sao hai dòng cho cùng kết quả?
```java
public class Point { int x, y; Point(int x, int y){ this.x=x; this.y=y; } }
Point p1 = new Point(1, 2), p2 = new Point(1, 2);
System.out.println(p1.equals(p2));
System.out.println(p1 == p2);
```

**Câu 2.** Class sau vi phạm contract ở đâu? Mô tả *chính xác* điều xảy ra khi chạy `set.contains(new Product("A"))` sau khi đã `set.add(new Product("A"))`.
```java
public class Product {
    private String code;
    public Product(String code){ this.code = code; }
    @Override public boolean equals(Object o){
        if (!(o instanceof Product)) return false;
        return code.equals(((Product) o).code);
    }
    // không override hashCode()
}
```

**Câu 3.** Hai lỗi tiềm ẩn trong `compareTo` sau là gì? (gợi ý: một về kiểu số, một về contract)
```java
public class Score implements Comparable<Score> {
    int value;
    @Override public int compareTo(Score o){
        return this.value > o.value ? 1 : -1;
    }
}
```

**Câu 4.** `record` sau: `a.equals(b)` trả về gì, với `a` và `b` được tạo từ hai mảng khác nhau nhưng cùng nội dung? Sửa thế nào?
```java
public record Line(String label, int[] points) { }
Line a = new Line("x", new int[]{1,2,3});
Line b = new Line("x", new int[]{1,2,3});
System.out.println(a.equals(b));
```

**Câu 5.** Vì sao đoạn này khiến `hs.contains(y)` là `false` nhưng `ts.contains(y)` là `true`?
```java
BigDecimal x = new BigDecimal("2.5"), y = new BigDecimal("2.50");
Set<BigDecimal> hs = new HashSet<>(List.of(x));
Set<BigDecimal> ts = new TreeSet<>(List.of(x));
```

**Câu 6.** `list.sort(...)` dưới đây sắp theo tiêu chí gì, thứ tự ưu tiên nào? Có đúng ý "phòng ban A→Z, cùng phòng thì lương cao→thấp" không?
```java
list.sort(Comparator.comparing(Employee::getDepartment)
                     .thenComparing(Employee::getSalary).reversed());
```

**Câu 7.** Class dưới đây dùng làm phần tử `HashSet`. Chuyện gì xảy ra sau khi gọi `tag.rename("done")` với `tag` đã nằm trong set?
```java
class Tag {
    private String name;
    Tag(String n){ name = n; }
    void rename(String n){ name = n; }
    @Override public boolean equals(Object o){ return o instanceof Tag t && name.equals(t.name); }
    @Override public int hashCode(){ return Objects.hash(name); }
}
```

**Câu 8.** Vì sao `equals()` của một class so sánh field `double` nên dùng `Double.compare(a,b)==0` thay vì `a==b`? Nêu **hai** trường hợp cụ thể.

---

### Phần B — Bài tập viết code

**Bài 1 — Override chuẩn cho Entity.**
Viết class `Book`: `isbn` (String, định danh duy nhất, `final`), `title`, `author`, `price`. Override `equals()`/`hashCode()` **chỉ dựa `isbn`**. Override `toString()` in đầy đủ **trừ `price`** (coi là nhạy cảm). Viết `main` chứng minh: thêm hai `Book` cùng `isbn` (khác `price`) vào `HashSet` chỉ còn 1 phần tử; `HashMap<Book,String>` `put` bằng object này, `get` bằng object kia vẫn ra giá trị.

**Bài 2 — Tái hiện rồi sửa "vi phạm contract".**
Viết `Coordinate{int x, y}` với `equals()` đúng chuẩn nhưng **cố tình quên** `hashCode()`. `main`: thêm 1 object vào `HashSet`, tạo object mới nội dung giống hệt, in `equals()` (`true`) và `contains()` (`false`). Sau đó bổ sung `hashCode()` bằng `Objects.hash(x, y)`, chạy lại, `contains()` giờ là `true`. In cả `hashCode()` của hai object ở hai phiên bản để thấy rõ nguyên nhân.

**Bài 3 — `getClass()` vs `instanceof` và tính đối xứng.**
Viết `Point{int x,y}` (`equals` kiểu `instanceof`) và `ColorPoint extends Point { String color }` override `equals()` để so thêm `color`. `main`: tạo `Point p = new Point(1,1)` và `ColorPoint cp = new ColorPoint(1,1,"red")`, in `p.equals(cp)` và `cp.equals(p)` — chỉ ra sự **bất đối xứng**. Rồi viết lại `ColorPoint` **không kế thừa** `Point` mà **chứa** một `Point` + `String color` (composition), thêm method `Point asPoint()`; giải thích trong comment vì sao cách này không còn mâu thuẫn contract.

**Bài 4 — `float`/mảng trong equals/hashCode.**
Viết `Signal{ double[] samples, double gain }`. Override `equals()`/`hashCode()` đúng: `samples` bằng `Arrays.equals`/`Arrays.hashCode`, `gain` bằng `Double.compare`/`Double.hashCode`. `main`: chứng minh hai `Signal` tạo từ hai mảng khác reference nhưng cùng nội dung thì `equals()` = `true`; và hai `Signal` có `gain = 0.0` vs `-0.0` thì `equals()` = `false` **và** `hashCode()` cũng khác (nhất quán).

**Bài 5 — Comparable + Comparator trên cùng bài toán, kèm bẫy `reversed()`.**
Viết `Movie{ String title, double rating, int year }`.
- `implements Comparable<Movie>` — thứ tự tự nhiên: `rating` **giảm dần** (dùng `Double.compare`, không dùng phép trừ).
- Ba `Comparator<Movie>`: theo `title` A→Z; theo `year` tăng dần; và một comparator "`title` A→Z, cùng `title` thì `year` **giảm** dần" — viết **đúng** (chỉ đảo tiêu chí `year`) và viết thêm phiên bản **sai** (`.reversed()` cuối chuỗi), in kết quả cả hai để thấy khác biệt.
- `main`: sort bằng `Collections.sort` (Comparable) rồi bằng từng `Comparator`, in ra so sánh.

**Bài 6 — `compareTo` không nhất quán với `equals`.**
Viết `Version{ int major, int minor }` với `equals()`/`hashCode()` dựa **cả hai** field, nhưng `compareTo` **chỉ so `major`**. `main`: tạo `v1 = (1,0)`, `v2 = (1,5)`. In `v1.equals(v2)` (`false`) và `v1.compareTo(v2)` (`0`). Thêm cả hai vào `HashSet` (size = 2) và vào `TreeSet` (size = 1 — `v2` bị coi là trùng). Viết 2–3 câu giải thích rủi ro thực tế của thiết kế này và cách sửa (`thenComparingInt(minor)`).

---

### Phần C — Nâng cao

**Câu 1.** Class `Money{ long amountCents; String currency }` dùng làm **key** của `HashMap` trong một service tính giá gọi hàng trăm nghìn lần/giây. Đề xuất cách viết `hashCode()` tối ưu (viết tay công thức Item 11 + cache cho object bất biến) và giải thích vì sao `Objects.hash(...)` không lý tưởng ở đây. Object này cần điều kiện gì để cache hashCode an toàn?

**Câu 2.** Giải thích vì sao `hashCode()` trả hằng số `return 0;` vẫn **hợp lệ về contract** nhưng biến `HashMap` với 1 triệu key thành gần O(n) cho mỗi thao tác. Java 8+ có cơ chế gì giảm nhẹ, và nó yêu cầu gì ở key?

**Câu 3.** Một `Comparator` viết bằng `(a, b) -> a.getScore() - b.getScore()` (kiểu `int`) chạy tốt suốt quá trình dev nhưng thỉnh thoảng ném `IllegalArgumentException: Comparison method violates its general contract!` trên production. Giải thích cơ chế: vì sao lỗi *thỉnh thoảng*, TimSort phát hiện bằng cách nào, sửa thế nào.

**Câu 4.** Cho Entity JPA `Order{ @Id @GeneratedValue Long id; ... }`. Một dev viết `equals()`/`hashCode()` bằng IDE generate (mọi field, kiểu `getClass()`). Liệt kê **ba** tình huống runtime cụ thể mà cách này gây bug (gợi ý: trước/sau `save()`, proxy lazy, `Set` collection trong entity cha). Viết lại cho đúng.

**Câu 5.** `record Range(int lo, int hi)` — bạn muốn hai `Range` "bằng nhau" khi **giao nhau** (overlap), không phải khi trùng khít. Vì sao **không được** cài điều đó vào `equals()` của record (hay của bất kỳ class nào)? Tính chất nào của quan hệ tương đương bị phá? Nên mô hình hóa "overlap" bằng gì thay thế?

**Câu 6.** Thiết kế `equals()`/`hashCode()` cho `CaseInsensitiveKey` bọc một `String`, sao cho `new CaseInsensitiveKey("Abc")` và `new CaseInsensitiveKey("ABC")` là một key trong `HashMap`. Chỉ ra: phải chuẩn hóa (normalize) ở đâu, `hashCode()` phải tính trên dạng nào, và vì sao **không** để nó `equals()` với `String` trần.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. Cả hai in `false`. `Point` không override `equals()` → `equals()` mặc định chính là `this == obj` → cùng bản chất với dòng `==`. Hai `new` tạo hai object khác nhau trên Heap.
2. Vi phạm **contract equals/hashCode**: `equals()` so theo `code` nhưng `hashCode()` vẫn là identity hash → hai `Product` cùng `code` có `equals()==true` nhưng `hashCode()` khác. `set.add(pA)` đặt `pA` vào bucket theo `pA.hashCode()`. `set.contains(new Product("A"))` tính `hashCode()` của object mới → bucket khác → `HashSet` không duyệt tới bucket chứa `pA` → trả `false` dù `equals()` sẽ `true`. (Phần `equals()` còn dùng `instanceof` nên chấp nhận lớp con — rủi ro đối xứng nếu có lớp con thêm field.)
3. (a) **Không bao giờ trả 0** khi hai `value` bằng nhau → phá **phản đối xứng dấu**: `x.compareTo(y)` và `y.compareTo(x)` cùng ra `-1` khi `x.value == y.value` (đáng lẽ đối dấu). (b) Hệ quả: `TreeSet`/`TreeMap` không bao giờ coi hai `Score` là trùng; `sort` có thể ném `IllegalArgumentException` do contract vỡ. Sửa: `return Integer.compare(this.value, o.value);`.
4. In `false`. `record` sinh `equals()` dùng `Objects.equals(points, other.points)` → so sánh **reference** hai mảng → khác nhau. Sửa: đổi component sang `List<Integer>` (khuyến nghị), hoặc override thủ công `equals`/`hashCode`/`toString` trong record dùng `Arrays.equals`/`Arrays.hashCode`/`Arrays.toString`.
5. `BigDecimal.equals()` so cả **scale** → `"2.5"` (scale 1) ≠ `"2.50"` (scale 2) → `HashSet` (dùng `equals`) không thấy `y`. `BigDecimal.compareTo()` chỉ so **giá trị số học** → `0` → `TreeSet` (dùng `compareTo`) coi `y` trùng `x` → `contains(y)` là `true`.
6. `.reversed()` ở cuối đảo **toàn bộ** comparator đã dựng → thành "phòng ban **Z→A**, cùng phòng thì lương **thấp→cao**". **Không** đúng ý. Muốn đúng: `Comparator.comparing(Employee::getDepartment).thenComparing(Comparator.comparing(Employee::getSalary).reversed())` hoặc `...thenComparing(Employee::getSalary, Comparator.reverseOrder())`.
7. `hashCode()` của `tag` đổi theo `name` mới, nhưng `tag` vẫn nằm ở bucket ứng với `name` cũ. Kết quả: `set.contains(tag)` → `false`; `set.remove(tag)` → không xóa được; duyệt `for` vẫn thấy `tag`. `tag` thành "bóng ma" — Set hỏng bất biến. Nguyên nhân: field mutable tham gia `equals`/`hashCode`.
8. (a) `x.value = Double.NaN`: `NaN == NaN` là `false` → `x.equals(x)` thành `false` → **mất tính phản xạ**. `Double.compare(NaN, NaN) == 0`. (b) `0.0 == -0.0` là `true` nhưng `Double.hashCode(0.0) != Double.hashCode(-0.0)` → nếu `equals` coi chúng bằng thì **vi phạm contract với hashCode**; `Double.compare(0.0, -0.0) != 0` giữ nhất quán.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `hashCode()` = `Objects.hash(isbn)` (hoặc `isbn.hashCode()`); `equals()` kiểu `this==o` → `o instanceof Book b` → `isbn.equals(b.isbn)`. `toString()` bỏ `price`: `"Book{isbn='%s', title='%s', author='%s'}".formatted(isbn, title, author)`. Vì bucket được xác định bởi `isbn.hashCode()` nên `put`/`get` bằng hai object khác nhau cùng `isbn` trỏ về cùng entry.
- **Bài 2:** Tái hiện đúng tình huống mục 7. Điểm học: in `hashCode()` hai object *trước* khi thêm `hashCode()` override — chúng khác nhau (identity hash); *sau* khi thêm — bằng nhau (`Objects.hash(x,y)` cùng input).
- **Bài 3:** `p.equals(cp)` → `true` (Point chỉ so `x,y`); `cp.equals(p)` → `false` (`p` không phải `ColorPoint`, hoặc thiếu `color`) → **bất đối xứng** → `List.of(p).contains(cp)` khác `List.of(cp).contains(p)`. Bản composition: `ColorPoint` giữ `private final Point point; private final String color;`, `equals()` so cả hai; nó **không** còn là `Point` nên không có kỳ vọng "so được với Point" → hết mâu thuẫn. `asPoint()` trả `point` khi cần dùng như tọa độ.
- **Bài 4:** `equals()`: `this==o` → `instanceof Signal s` → `Double.compare(gain, s.gain)==0 && Arrays.equals(samples, s.samples)`. `hashCode()`: `31 * Double.hashCode(gain) + Arrays.hashCode(samples)`. Với `gain` `0.0` vs `-0.0`: `Double.compare` ra khác `0` → `equals` `false`; `Double.hashCode` cũng khác → `hashCode` khác → nhất quán.
- **Bài 5:** `compareTo`: `Double.compare(o.rating, this.rating)` (đảo vị trí để giảm dần). Comparator "title A→Z, year giảm": `Comparator.comparing(Movie::getTitle).thenComparing(Comparator.comparingInt(Movie::getYear).reversed())`. Bản sai `...thenComparingInt(Movie::getYear).reversed()` đảo cả `title` → `title` Z→A. In hai danh sách cạnh nhau để thấy dòng đầu khác nhau.
- **Bài 6:** `equals`/`hashCode` trên `(major, minor)`; `compareTo` = `Integer.compare(this.major, o.major)`. `HashSet` size 2 (khác `minor` → khác `equals`/`hashCode`). `TreeSet` size 1 (`compareTo` chỉ nhìn `major` → `v2` "trùng" `v1`). Rủi ro: dùng `TreeSet`/`TreeMap`/`SortedSet` sẽ **mất dữ liệu** âm thầm; `Collections.binarySearch` cho kết quả không khớp `indexOf`. Sửa: `Integer.compare(major, o.major)` rồi `thenComparingInt` trên `minor` → `compareTo` nhất quán với `equals`.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. Viết tay: `int h = Long.hashCode(amountCents); h = 31*h + currency.hashCode(); return h;` — tránh cấp phát mảng `Object[]` và autobox `long`→`Long` mà `Objects.hash(amountCents, currency)` bắt buộc phải làm mỗi lần gọi. Cache: thêm `private int hash;`, tính lần đầu khi `hash == 0` rồi gán lại. An toàn khi và chỉ khi object **bất biến** (`amountCents`, `currency` đều `final`, `currency` là `String` bất biến) — nếu field đổi được thì giá trị cache sẽ sai.
2. Hợp lệ vì contract chỉ đòi "`equals` bằng ⇒ `hashCode` bằng"; hằng số thỏa hiển nhiên. Nhưng mọi key vào **một bucket** → mỗi `get`/`put`/`remove` phải duyệt tuyến tính danh sách trong bucket đó ⇒ ~O(n). Java 8+: khi một bucket vượt ngưỡng (8 entry) và bảng đủ lớn, bucket được **chuyển thành cây đỏ-đen** → O(log n). Điều kiện: key phải `Comparable` để cây so sánh được; nếu không, cây rơi về so sánh theo identity hash và lợi ích giảm.
3. `a.getScore() - b.getScore()` tràn khi hiệu vượt phạm vi `int` (một điểm gần `Integer.MAX_VALUE`, một gần `MIN_VALUE`) → dấu bị lật → comparator **mất bắc cầu/phản đối xứng**. "Thỉnh thoảng" vì chỉ vỡ với những phân bố dữ liệu nhất định. TimSort trong lúc merge kiểm tra các bất biến của run; khi phát hiện thứ tự mâu thuẫn nó ném `IllegalArgumentException` thay vì lặp vô hạn/ra kết quả sai. Sửa: `Integer.compare(a.getScore(), b.getScore())` hoặc `Comparator.comparingInt(X::getScore)`.
4. (a) `save()` một `Order` mới: trước khi lưu `id == null`, `hashCode()` (từ `id` hoặc từ mọi field) là một giá trị; sau `save()` `id` được gán → `hashCode()` đổi → nếu `Order` đã nằm trong `HashSet` thì "mất tích". (b) `getClass()`: `orderRepository.getOne(id)` trả **proxy** `Order$HibernateProxy` → `entity.equals(proxy)` `false` do `getClass()` khác. (c) Entity cha chứa `Set<OrderLine>`; `equals()`/`hashCode()` của `OrderLine` dựa mọi field mutable → thêm vào set trước khi set `id`/khi field còn đổi → trùng lặp hoặc không tìm thấy. Sửa: `equals()` dùng `instanceof` + `id != null && id.equals(other.id)`; `hashCode()` trả `getClass().hashCode()` (hằng) hoặc hash của khóa nghiệp vụ bất biến.
5. "Overlap" **không bắc cầu**: `[1,5]` overlap `[4,8]`, `[4,8]` overlap `[7,10]`, nhưng `[1,5]` **không** overlap `[7,10]`. Cũng dễ mất **nhất quán với hashCode** (không có hàm hash nào khiến mọi cặp overlap cùng giá trị). Đưa vào `equals()` sẽ làm `HashSet`/`HashMap` hỏng. Mô hình đúng: giữ `equals()` là trùng khít `(lo, hi)`, và tách riêng `boolean overlaps(Range other)` như một method nghiệp vụ; muốn gộp các khoảng giao nhau thì viết thuật toán "merge intervals" trả `List<Range>`.
6. Chuẩn hóa **một lần trong constructor**: `this.key = raw.toLowerCase(Locale.ROOT);` (dùng `Locale.ROOT` để tránh bẫy locale kiểu Thổ Nhĩ Kỳ với chữ `i`). `equals()` so `this.key.equals(other.key)`; `hashCode()` = `key.hashCode()` — tức tính trên **dạng đã chuẩn hóa**, nhờ vậy `"Abc"` và `"ABC"` cùng bucket, cùng entry. Không cho `equals()` chấp nhận `String` trần vì sẽ **mất đối xứng** (`ciKey.equals("abc")` có thể `true` nhưng `"abc".equals(ciKey)` luôn `false`) → `HashMap` hành xử khác nhau tùy chiều gọi.

</details>

---

*File tiếp theo trong lộ trình: **Module 03.1 — Collections Framework** (List, Set, Map — độ phức tạp Big-O, cây vs băm, khi nào chọn cấu trúc nào).*
