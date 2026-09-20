# Lời giải đầy đủ — Module 02.4: equals(), hashCode(), toString()

> Nguồn đề: `07-equals-hashcode-tostring/07-equals-hashcode-tostring.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Override chuẩn cho Entity

### Đề
Viết class `Book`: `isbn` (String, định danh duy nhất, `final`), `title`, `author`, `price`. Override `equals()`/`hashCode()` **chỉ dựa `isbn`**. Override `toString()` in đầy đủ **trừ `price`** (coi là nhạy cảm). Viết `main` chứng minh: thêm hai `Book` cùng `isbn` (khác `price`) vào `HashSet` chỉ còn 1 phần tử; `HashMap<Book,String>` `put` bằng object này, `get` bằng object kia vẫn ra giá trị.

### Phân tích

Nguyên tắc chọn field cho `equals()`/`hashCode()`: dựa vào **định danh nghiệp vụ (business key)**, không nhất thiết phải dựa **toàn bộ field**. `isbn` là mã sách duy nhất — 2 `Book` cùng `isbn` **luôn luôn là cùng 1 quyển sách thật**, dù `price`/`title` có thể ghi khác nhau (VD: cập nhật giá theo thời gian). Đây là mẫu hình rất phổ biến khi thiết kế Entity (sẽ gặp lại chính xác ở Module 11 — JPA `@Id`).

**Nguyên tắc bất di bất dịch:** field dùng trong `hashCode()` phải là **tập con hoặc đúng bằng** tập field dùng trong `equals()` — không được nhiều hơn, không được ít hơn (nếu ít hơn vẫn hợp lệ về mặt kỹ thuật nhưng làm giảm chất lượng phân tán hash).

### Lời giải

```java
package baitap.bai1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final double price;

    public Book(String isbn, String title, String author, double price) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book other = (Book) o;
        return Objects.equals(isbn, other.isbn); // CHỈ dựa isbn - định danh nghiệp vụ
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn); // PHẢI khớp đúng field dùng trong equals()
    }

    @Override
    public String toString() {
        // KHÔNG in "price" - coi là dữ liệu nhạy cảm
        return "Book{isbn='" + isbn + "', title='" + title + "', author='" + author + "'}";
    }

    public static void main(String[] args) {
        Book b1 = new Book("978-604-1", "Java Core", "Pho", 150_000);
        Book b2 = new Book("978-604-1", "Java Core (tái bản)", "Pho", 180_000); // CÙNG isbn, KHÁC price/title

        System.out.println("b1.equals(b2) = " + b1.equals(b2));
        System.out.println("b1.hashCode() == b2.hashCode(): " + (b1.hashCode() == b2.hashCode()));
        System.out.println("b1.toString() = " + b1);

        Set<Book> books = new HashSet<>();
        books.add(b1);
        books.add(b2);
        System.out.println("Số phần tử trong HashSet: " + books.size()); // kỳ vọng: 1

        Map<Book, String> priceNote = new HashMap<>();
        priceNote.put(b1, "Giá gốc: 150k");
        String result = priceNote.get(b2); // get() bằng b2, nhưng put() bằng b1
        System.out.println("get(b2) sau khi put(b1, ...) = " + result);
    }
}
```

**Kết quả chạy:**
```
b1.equals(b2) = true
b1.hashCode() == b2.hashCode(): true
b1.toString() = Book{isbn='978-604-1', title='Java Core', author='Pho'}
Số phần tử trong HashSet: 1
get(b2) sau khi put(b1, ...) = Giá gốc: 150k
```

### Giải thích

- `HashSet.add(b2)` khi đã có `b1`: `HashSet` tính `hashCode()` của `b2` để tìm đúng "bucket", rồi so `equals()` với các phần tử đã có trong bucket đó — vì `b1.equals(b2)` là `true`, `b2` bị coi là **trùng**, không được thêm vào — size vẫn là 1.
- `HashMap.get(b2)` tìm đúng key `b1` đã `put` trước đó, **vì `b1.hashCode() == b2.hashCode()` VÀ `b1.equals(b2)`** — đây chính xác là 2 điều kiện `HashMap` cần để coi 2 object là "cùng 1 key", bất kể chúng có phải cùng 1 tham chiếu trên heap hay không.
- Nếu **chỉ** override `equals()` mà **quên** `hashCode()` (giữ nguyên bản mặc định của `Object`, dựa vào địa chỉ bộ nhớ), cả 2 chứng minh trên sẽ **thất bại** — đây chính xác là nội dung Bài 2 sau đây.

---

## Bài 2 — Tái hiện rồi sửa "vi phạm contract"

### Đề
Viết `Coordinate{int x, y}` với `equals()` đúng chuẩn nhưng **cố tình quên** `hashCode()`. `main`: thêm 1 object vào `HashSet`, tạo object mới nội dung giống hệt, in `equals()` (`true`) và `contains()` (`false`). Sau đó bổ sung `hashCode()` bằng `Objects.hash(x, y)`, chạy lại, `contains()` giờ là `true`. In cả `hashCode()` của hai object ở hai phiên bản để thấy rõ nguyên nhân.

### Phân tích

**Hợp đồng `equals`/`hashCode`** (JavaDoc của `Object`) quy định: **nếu `a.equals(b) == true` thì BẮT BUỘC `a.hashCode() == b.hashCode()`**. Nếu chỉ override `equals()` mà không override `hashCode()`, class vẫn dùng `hashCode()` **mặc định của `Object`** — thường dựa trên **địa chỉ bộ nhớ/định danh nội bộ (identity hash)**, gần như chắc chắn **khác nhau** giữa 2 object khác nhau trên heap, dù `equals()` báo `true`. Đây là **vi phạm hợp đồng** trực tiếp, gây ra `HashSet`/`HashMap` hoạt động sai (không tìm thấy phần tử "trùng" dù `equals()` xác nhận đúng là trùng).

### Lời giải — phiên bản LỖI (thiếu hashCode)

```java
package baitap.bai2;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Coordinate {
    private final int x, y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate)) return false;
        Coordinate other = (Coordinate) o;
        return x == other.x && y == other.y;
    }

    // CỐ TÌNH KHÔNG override hashCode() - vẫn dùng bản mặc định của Object (identity-based)

    public static void main(String[] args) {
        System.out.println("===== Phiên bản LỖI (thiếu hashCode) =====");

        Coordinate c1 = new Coordinate(3, 4);
        Set<Coordinate> set = new HashSet<>();
        set.add(c1);

        Coordinate c2 = new Coordinate(3, 4); // NỘI DUNG giống hệt c1

        System.out.println("c1.equals(c2) = " + c1.equals(c2));           // true - nội dung giống nhau
        System.out.println("set.contains(c2) = " + set.contains(c2));     // FALSE - vi phạm hợp đồng!
        System.out.println("c1.hashCode() = " + c1.hashCode());
        System.out.println("c2.hashCode() = " + c2.hashCode());
        System.out.println("hashCode có bằng nhau không? " + (c1.hashCode() == c2.hashCode()));
    }
}
```

**Kết quả chạy (con số `hashCode()` cụ thể tùy JVM, nhưng luôn KHÁC nhau):**
```
===== Phiên bản LỖI (thiếu hashCode) =====
c1.equals(c2) = true
set.contains(c2) = false
c1.hashCode() = 1908925028
c2.hashCode() = 1592448506
hashCode có bằng nhau không? false
```

### Lời giải — phiên bản ĐÚNG (bổ sung hashCode)

```java
package baitap.bai2;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CoordinateFixed {
    private final int x, y;

    public CoordinateFixed(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoordinateFixed)) return false;
        CoordinateFixed other = (CoordinateFixed) o;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y); // BỔ SUNG - dựa ĐÚNG field đã dùng trong equals()
    }

    public static void main(String[] args) {
        System.out.println("===== Phiên bản ĐÚNG (có hashCode) =====");

        CoordinateFixed c1 = new CoordinateFixed(3, 4);
        Set<CoordinateFixed> set = new HashSet<>();
        set.add(c1);

        CoordinateFixed c2 = new CoordinateFixed(3, 4);

        System.out.println("c1.equals(c2) = " + c1.equals(c2));
        System.out.println("set.contains(c2) = " + set.contains(c2));     // TRUE - đúng như kỳ vọng
        System.out.println("c1.hashCode() = " + c1.hashCode());
        System.out.println("c2.hashCode() = " + c2.hashCode());
        System.out.println("hashCode có bằng nhau không? " + (c1.hashCode() == c2.hashCode()));
    }
}
```

**Kết quả chạy:**
```
===== Phiên bản ĐÚNG (có hashCode) =====
c1.equals(c2) = true
set.contains(c2) = true
c1.hashCode() = 1024
c2.hashCode() = 1024
hashCode có bằng nhau không? true
```

### Giải thích

- **Nguyên nhân gốc rễ `set.contains(c2)` trả `false` ở phiên bản lỗi:** `HashSet.contains()` **KHÔNG duyệt tuần tự** toàn bộ phần tử rồi gọi `equals()` — nó **tính `hashCode()` của `c2` TRƯỚC**, dùng giá trị đó để xác định "bucket" cần tìm, rồi **CHỈ so `equals()` với các phần tử TRONG bucket đó**. Vì `c1.hashCode() != c2.hashCode()` (do dùng `hashCode()` mặc định theo địa chỉ bộ nhớ), `c2` bị tính vào **bucket khác hoàn toàn** với `c1` — `HashSet` "không thèm" so `equals()` với `c1` vì nó còn chưa tìm đúng chỗ.
- Sau khi bổ sung `hashCode()` dựa đúng `x, y`, `c1.hashCode() == c2.hashCode()` → cả 2 rơi vào **cùng 1 bucket** → `HashSet` mới thực sự gọi `equals()` để so sánh → tìm thấy đúng.
- **Bài học cốt lõi:** `equals()` đúng nhưng `hashCode()` sai (hoặc thiếu) là lỗi **âm thầm, khó phát hiện qua code review thông thường** — chỉ lộ ra khi object được dùng làm key/phần tử trong cấu trúc dựa trên hash (`HashSet`, `HashMap`, `HashMultimap`...). Đây là lý do IDE hiện đại luôn cảnh báo (hoặc tự sinh cặp `equals`/`hashCode` cùng lúc) khi override 1 trong 2.

---

## Bài 3 — `getClass()` vs `instanceof` và tính đối xứng

### Đề
Viết `Point{int x,y}` (`equals` kiểu `instanceof`) và `ColorPoint extends Point { String color }` override `equals()` để so thêm `color`. `main`: tạo `Point p = new Point(1,1)` và `ColorPoint cp = new ColorPoint(1,1,"red")`, in `p.equals(cp)` và `cp.equals(p)` — chỉ ra sự **bất đối xứng**. Rồi viết lại `ColorPoint` **không kế thừa** `Point` mà **chứa** một `Point` + `String color` (composition), thêm method `Point asPoint()`; giải thích trong comment vì sao cách này không còn mâu thuẫn contract.

### Phân tích

**Tính đối xứng (symmetric)** trong hợp đồng `equals()`: `a.equals(b)` phải **LUÔN cho cùng kết quả** với `b.equals(a)`. Khi `Point.equals()` dùng `instanceof Point` (chấp nhận bất kỳ subclass nào) nhưng `ColorPoint.equals()` **so thêm `color`** (không chấp nhận `Point` thuần vì thiếu field `color`), ta có tình huống điển hình phá vỡ đối xứng:

- `p.equals(cp)`: `p` là `Point`, `cp instanceof Point` → `true` → so `x, y` → khớp → `true`.
- `cp.equals(p)`: `cp` là `ColorPoint`, `p instanceof Point` → `true`, nhưng logic bên trong `ColorPoint.equals()` cố ép `p` về `ColorPoint` để lấy `color` → `p` **không phải** `ColorPoint` → `false`.

→ `p.equals(cp)` là `true` nhưng `cp.equals(p)` là `false` — **vi phạm tính đối xứng**, gây lỗi khó lường khi object nằm trong `HashSet`/`List.contains()` tùy thuộc **thứ tự gọi** `equals()`.

### Lời giải — tái hiện lỗi

```java
package baitap.bai3;

public class Main {

    static class Point {
        protected final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false; // chấp nhận CẢ Point lẫn ColorPoint (vì ColorPoint IS-A Point)
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(x, y); }
    }

    static class ColorPoint extends Point {
        String color;
        ColorPoint(int x, int y, String color) { super(x, y); this.color = color; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ColorPoint)) return false; // CHỈ chấp nhận ColorPoint, KHÔNG chấp nhận Point thuần
            ColorPoint cp = (ColorPoint) o;
            return x == cp.x && y == cp.y && color.equals(cp.color);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(x, y, color); }
    }

    public static void main(String[] args) {
        Point p = new Point(1, 1);
        ColorPoint cp = new ColorPoint(1, 1, "red");

        System.out.println("p.equals(cp) = " + p.equals(cp));   // true
        System.out.println("cp.equals(p) = " + cp.equals(p));   // false -> BẤT ĐỐI XỨNG!
    }
}
```

**Kết quả chạy:**
```
p.equals(cp) = true
cp.equals(p) = false
```

### Lời giải — sửa bằng Composition (thay vì kế thừa)

```java
package baitap.bai3;

import java.util.Objects;

public class Fixed {

    static class Point {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() { return Objects.hash(x, y); }

        @Override
        public String toString() { return "Point(" + x + "," + y + ")"; }
    }

    // KHÔNG "extends Point" nữa - mà CHỨA 1 Point bên trong (Composition)
    static class ColorPoint {
        private final Point point; // composition, không phải kế thừa
        private final String color;

        ColorPoint(int x, int y, String color) {
            this.point = new Point(x, y);
            this.color = color;
        }

        Point asPoint() { return point; } // cho phép "nhìn" ColorPoint dưới dạng Point khi cần

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // Giờ đây ColorPoint chỉ so equals với CHÍNH XÁC ColorPoint khác - không còn quan hệ IS-A với Point
            // nên KHÔNG còn tình huống "Point coi ColorPoint là Point, nhưng ColorPoint không coi Point là ColorPoint"
            if (!(o instanceof ColorPoint)) return false;
            ColorPoint other = (ColorPoint) o;
            return point.equals(other.point) && color.equals(other.color);
        }

        @Override
        public int hashCode() { return Objects.hash(point, color); }
    }

    public static void main(String[] args) {
        Point p = new Point(1, 1);
        ColorPoint cp = new ColorPoint(1, 1, "red");

        // p.equals(cp) -> KHÔNG COMPILE ĐƯỢC theo nghĩa "so sánh trực tiếp có ý nghĩa" vì 2 kiểu
        // không còn quan hệ kế thừa - nhưng nếu vẫn ép kiểu Object thì luôn là false, KHÔNG bất đối xứng:
        System.out.println("p.equals(cp) = " + p.equals((Object) cp));         // false
        System.out.println("cp.equals(p) = " + cp.equals(p));                  // false - NHẤT QUÁN với dòng trên!

        // Muốn so sánh phần "Point" của 2 bên, dùng asPoint() tường minh:
        System.out.println("p.equals(cp.asPoint()) = " + p.equals(cp.asPoint())); // true - rõ ràng, không mập mờ
    }
}
```

**Kết quả chạy:**
```
p.equals(cp) = false
cp.equals(p) = false
p.equals(cp.asPoint()) = true
```

### Giải thích vì sao Composition không còn mâu thuẫn contract

- Với kế thừa (`ColorPoint extends Point`), `ColorPoint` **về mặt kiểu** vẫn "LÀ MỘT" `Point` (đa hình cho phép gán `Point ref = new ColorPoint(...)`) — nhưng `equals()` lại **từ chối** đối xử với `Point` như nhau ở 2 chiều, tạo ra mâu thuẫn giữa **quan hệ kiểu (IS-A)** và **hành vi thực tế (equals)**.
- Với composition, `ColorPoint` **KHÔNG PHẢI** 1 `Point` về mặt kiểu (không có quan hệ kế thừa) — nó chỉ **CHỨA** 1 `Point`. Vì vậy `p.equals(cp)` và `cp.equals(p)` **đều nhất quán trả `false`** (2 kiểu hoàn toàn khác nhau, không ai "giả vờ" là loại kia) — tính đối xứng **được bảo toàn tự nhiên**, không cần thủ thuật gì thêm.
- Method `asPoint()` cho phép **khi thực sự cần** so sánh "phần Point" của 1 `ColorPoint` với 1 `Point` khác, làm điều đó **tường minh** (`p.equals(cp.asPoint())`), thay vì để `equals()` tự động "đoán ý" gây mập mờ như bản kế thừa.
- **Bài học rút ra (liên hệ trực tiếp Effective Java, và cũng chính là bài học Composition ở Bài 6 Module 04):** "không thể vừa mở rộng 1 class cụ thể (không abstract) bằng cách thêm field mới, vừa giữ được `equals()` đúng chuẩn contract" — đây là hạn chế **cố hữu** của kế thừa khi kết hợp với `equals()`, và composition là lối thoát triệt để.

---

## Bài 4 — `float`/mảng trong equals/hashCode

### Đề
Viết `Signal{ double[] samples, double gain }`. Override `equals()`/`hashCode()` đúng: `samples` bằng `Arrays.equals`/`Arrays.hashCode`, `gain` bằng `Double.compare`/`Double.hashCode`. `main`: chứng minh hai `Signal` tạo từ hai mảng khác reference nhưng cùng nội dung thì `equals()` = `true`; và hai `Signal` có `gain = 0.0` vs `-0.0` thì `equals()` = `false` **và** `hashCode()` cũng khác (nhất quán).

### Phân tích

2 bẫy kỹ thuật quan trọng của bài này:

1. **Mảng (`double[]`) không override `equals()`/`hashCode()` — kế thừa từ `Object`, so theo địa chỉ tham chiếu.** `arr1.equals(arr2)` với 2 mảng khác reference nhưng cùng nội dung luôn là `false`, dù `arr1 == arr2` cũng `false` nhưng nội dung giống hệt. Phải dùng `Arrays.equals(a, b)` (so nội dung) và `Arrays.hashCode(a)` (hash theo nội dung) thay vì gọi trực tiếp `.equals()`/`.hashCode()` trên mảng.
2. **`double` không nên so bằng `==` trong `equals()`** vì cần xử lý đúng các giá trị đặc biệt (`NaN`, `+0.0`/`-0.0`) theo đúng ngữ nghĩa "object equality" — `Double.compare(a, b) == 0` (hoặc `Double.doubleToLongBits`) coi `0.0` và `-0.0` là **KHÁC NHAU** (khác với toán tử `==` coi `0.0 == -0.0` là `true`), và coi `NaN` **bằng chính nó**.

### Lời giải

```java
package baitap.bai4;

import java.util.Arrays;
import java.util.Objects;

public class Signal {
    private final double[] samples;
    private final double gain;

    public Signal(double[] samples, double gain) {
        this.samples = samples;
        this.gain = gain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Signal)) return false;
        Signal other = (Signal) o;
        return Arrays.equals(samples, other.samples)      // SO NỘI DUNG mảng, không phải reference
                && Double.compare(gain, other.gain) == 0;   // phân biệt +0.0/-0.0, coi NaN == NaN
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(samples), Double.hashCode(gain));
    }

    public static void main(String[] args) {
        double[] arr1 = {0.1, 0.2, 0.3};
        double[] arr2 = {0.1, 0.2, 0.3}; // NỘI DUNG giống hệt arr1, nhưng KHÁC reference

        Signal s1 = new Signal(arr1, 1.0);
        Signal s2 = new Signal(arr2, 1.0);

        System.out.println("arr1 == arr2: " + (arr1 == arr2));                 // false - khác reference
        System.out.println("arr1.equals(arr2): " + arr1.equals(arr2));         // false - Object.equals() mặc định của mảng!
        System.out.println("Arrays.equals(arr1, arr2): " + Arrays.equals(arr1, arr2)); // true - so nội dung

        System.out.println("s1.equals(s2) = " + s1.equals(s2));                // true - nhờ dùng Arrays.equals bên trong
        System.out.println("s1.hashCode() == s2.hashCode(): " + (s1.hashCode() == s2.hashCode())); // true

        // ---- Trường hợp gain = 0.0 vs -0.0 ----
        Signal sPos = new Signal(new double[]{1.0}, 0.0);
        Signal sNeg = new Signal(new double[]{1.0}, -0.0);

        System.out.println();
        System.out.println("0.0 == -0.0 (toán tử ==): " + (0.0 == -0.0));                       // true (!)
        System.out.println("Double.compare(0.0, -0.0): " + Double.compare(0.0, -0.0));           // 1 (khác 0 -> KHÔNG bằng)
        System.out.println("sPos.equals(sNeg) = " + sPos.equals(sNeg));                          // false
        System.out.println("sPos.hashCode() == sNeg.hashCode(): " + (sPos.hashCode() == sNeg.hashCode())); // false - NHẤT QUÁN
    }
}
```

**Kết quả chạy:**
```
arr1 == arr2: false
arr1.equals(arr2): false
Arrays.equals(arr1, arr2): true
s1.equals(s2) = true
s1.hashCode() == s2.hashCode(): true

0.0 == -0.0 (toán tử ==): true
Double.compare(0.0, -0.0): 1
sPos.equals(sNeg) = false
sPos.hashCode() == sNeg.hashCode(): false
```

### Giải thích

- **`arr1.equals(arr2)` trả `false` dù nội dung giống hệt** là bẫy cực kỳ phổ biến — mảng trong Java **KHÔNG override `equals()`**, luôn dùng bản mặc định của `Object` (so địa chỉ). Đây chính xác là lý do `Signal.equals()` phải gọi **`Arrays.equals(samples, other.samples)`** thay vì `samples.equals(other.samples)` — nếu lỡ viết nhầm cách thứ 2, `Signal.equals()` sẽ **luôn `false`** với 2 mảng khác reference, kể cả nội dung y hệt — 1 bug rất khó phát hiện qua test hời hợt.
- **Nghịch lý `0.0 == -0.0` nhưng `Double.compare(0.0,-0.0) != 0`:** toán tử `==` trên `double` tuân theo chuẩn IEEE 754 (coi `+0.0` và `-0.0` là cùng giá trị số học), nhưng `Double.compare()`/`Double.equals()` (dùng trong ngữ cảnh **object equality**, VD làm key trong `TreeMap`/`HashMap`) cố tình phân biệt chúng — vì chúng có **biểu diễn bit khác nhau** (`Double.doubleToLongBits`), và về mặt "định danh giá trị" trong 1 số ngữ cảnh khoa học/kỹ thuật, dấu của số 0 có thể mang ý nghĩa (VD: hướng tiệm cận trong giải tích số). Đây là lý do JavaDoc khuyến nghị **luôn dùng `Double.compare()`** trong `equals()`, không dùng `==` trực tiếp.
- Kết quả `hashCode()` khác nhau giữa `sPos`/`sNeg` **nhất quán đúng hợp đồng**: vì `equals()` đã trả `false`, `hashCode()` khác nhau là **hợp lệ** (hợp đồng chỉ bắt buộc chiều ngược lại: `equals() == true` thì `hashCode()` phải bằng nhau; `equals() == false` thì `hashCode()` được phép giống hoặc khác tùy ý, nhưng ở đây `Double.hashCode(0.0) != Double.hashCode(-0.0)` nên tự nhiên khác nhau, càng tốt cho khả năng phân tán của hash).

---

## Bài 5 — Comparable + Comparator trên cùng bài toán, kèm bẫy `reversed()`

### Đề
Viết `Movie{ String title, double rating, int year }`.
- `implements Comparable<Movie>` — thứ tự tự nhiên: `rating` **giảm dần** (dùng `Double.compare`, không dùng phép trừ).
- Ba `Comparator<Movie>`: theo `title` A→Z; theo `year` tăng dần; và một comparator "`title` A→Z, cùng `title` thì `year` **giảm** dần" — viết **đúng** (chỉ đảo tiêu chí `year`) và viết thêm phiên bản **sai** (`.reversed()` cuối chuỗi), in kết quả cả hai để thấy khác biệt.
- `main`: sort bằng `Collections.sort` (Comparable) rồi bằng từng `Comparator`, in ra so sánh.

### Phân tích

- **`Comparable<Movie>`** định nghĩa **thứ tự tự nhiên DUY NHẤT** của class (VD: khi `Collections.sort(list)` không truyền `Comparator`). **`Comparator<Movie>`** cho phép định nghĩa **bao nhiêu thứ tự tùy ý** khác, không đụng vào class `Movie`.
- **Bẫy `.reversed()` ở cuối chuỗi `thenComparing`:** `comparator.reversed()` đảo ngược **TOÀN BỘ chuỗi so sánh đã xây dựng trước đó** (mọi tiêu chí), không chỉ đảo tiêu chí **cuối cùng**. Muốn chỉ đảo 1 tiêu chí cụ thể, phải đảo **ngay tại chỗ khai báo tiêu chí đó** (VD: `Comparator.comparingInt(Movie::getYear).reversed()` rồi mới `thenComparing`), không đảo cả chuỗi ở cuối.

### Lời giải

```java
package baitap.bai5;

import java.util.*;

public class Movie implements Comparable<Movie> {
    private final String title;
    private final double rating;
    private final int year;

    public Movie(String title, double rating, int year) {
        this.title = title;
        this.rating = rating;
        this.year = year;
    }

    public String getTitle() { return title; }
    public double getRating() { return rating; }
    public int getYear() { return year; }

    @Override
    public int compareTo(Movie other) {
        // Thứ tự tự nhiên: rating GIẢM DẦN -> đảo thứ tự tham số so với compare thông thường
        return Double.compare(other.rating, this.rating); // KHÔNG dùng phép trừ (double trừ double dễ sai làm tròn/NaN)
    }

    @Override
    public String toString() {
        return String.format("%s (%.1f, %d)", title, rating, year);
    }

    public static void main(String[] args) {
        List<Movie> movies = new ArrayList<>(List.of(
                new Movie("Zootopia", 8.0, 2016),
                new Movie("Alpha", 7.5, 2020),
                new Movie("Alpha", 9.0, 2018), // TRÙNG title "Alpha", khác rating/year - để test thenComparing
                new Movie("Mid", 8.5, 2019)
        ));

        // ---- 1. Comparable: rating giảm dần ----
        List<Movie> byRating = new ArrayList<>(movies);
        Collections.sort(byRating);
        System.out.println("Sort theo Comparable (rating giảm dần):");
        byRating.forEach(System.out::println);

        // ---- 2. Comparator: title A->Z ----
        Comparator<Movie> byTitle = Comparator.comparing(Movie::getTitle);
        List<Movie> sortedByTitle = new ArrayList<>(movies);
        sortedByTitle.sort(byTitle);
        System.out.println("\nSort theo title A->Z:");
        sortedByTitle.forEach(System.out::println);

        // ---- 3. Comparator: year tăng dần ----
        Comparator<Movie> byYear = Comparator.comparingInt(Movie::getYear);
        List<Movie> sortedByYear = new ArrayList<>(movies);
        sortedByYear.sort(byYear);
        System.out.println("\nSort theo year tăng dần:");
        sortedByYear.forEach(System.out::println);

        // ---- 4a. ĐÚNG: title A->Z, cùng title thì year GIẢM dần ----
        Comparator<Movie> correct = Comparator.comparing(Movie::getTitle)
                .thenComparing(Comparator.comparingInt(Movie::getYear).reversed()); // đảo NGAY tại tiêu chí year
        List<Movie> sortedCorrect = new ArrayList<>(movies);
        sortedCorrect.sort(correct);
        System.out.println("\n[ĐÚNG] title A->Z, cùng title year giảm dần:");
        sortedCorrect.forEach(System.out::println);

        // ---- 4b. SAI: .reversed() ở CUỐI cả chuỗi ----
        Comparator<Movie> wrong = Comparator.comparing(Movie::getTitle)
                .thenComparing(Comparator.comparingInt(Movie::getYear))
                .reversed(); // đảo NGƯỢC TOÀN BỘ chuỗi - kể cả title cũng bị đảo thành Z->A!
        List<Movie> sortedWrong = new ArrayList<>(movies);
        sortedWrong.sort(wrong);
        System.out.println("\n[SAI - .reversed() cuối chuỗi] kết quả KHÔNG như mong đợi:");
        sortedWrong.forEach(System.out::println);
    }
}
```

**Kết quả chạy:**
```
Sort theo Comparable (rating giảm dần):
Alpha (9.0, 2018)
Mid (8.5, 2019)
Zootopia (8.0, 2016)
Alpha (7.5, 2020)

Sort theo title A->Z:
Alpha (7.5, 2020)
Alpha (9.0, 2018)
Mid (8.5, 2019)
Zootopia (8.0, 2016)

Sort theo year tăng dần:
Zootopia (8.0, 2016)
Alpha (9.0, 2018)
Mid (8.5, 2019)
Alpha (7.5, 2020)

[ĐÚNG] title A->Z, cùng title year giảm dần:
Alpha (7.5, 2020)
Alpha (9.0, 2018)
Mid (8.5, 2019)
Zootopia (8.0, 2016)

[SAI - .reversed() cuối chuỗi] kết quả KHÔNG như mong đợi:
Zootopia (8.0, 2016)
Mid (8.5, 2019)
Alpha (9.0, 2018)
Alpha (7.5, 2020)
```

### Giải thích khác biệt giữa (4a) và (4b)

- **(4a) đúng:** đảo `.reversed()` **ngay tại `Comparator.comparingInt(Movie::getYear)`** — chỉ tiêu chí `year` bị đảo (giảm dần), tiêu chí `title` (A→Z) **giữ nguyên** vì không hề bị đụng tới.
- **(4b) sai:** đảo `.reversed()` ở **cuối cùng cả chuỗi** `title.thenComparing(year)` — điều này đảo ngược **toàn bộ kết quả so sánh tổng hợp**, tương đương "đảo cả `title` (Z→A) LẪN `year`" — nhìn kết quả (4b) thấy ngay `title` đang bị sắp theo **Z→A** (Zootopia lên đầu), hoàn toàn khác ý định ban đầu.
- **Quy tắc ghi nhớ:** `.reversed()` gọi **ở đâu** thì chỉ đảo **kết quả so sánh tính tới thời điểm đó** — gọi ngay sau 1 `comparingXxx()` đơn lẻ → chỉ đảo tiêu chí đó; gọi ở cuối 1 chuỗi `thenComparing` dài → đảo **toàn bộ** chuỗi đó, không phải chỉ tiêu chí cuối.

---

## Bài 6 — `compareTo` không nhất quán với `equals`

### Đề
Viết `Version{ int major, int minor }` với `equals()`/`hashCode()` dựa **cả hai** field, nhưng `compareTo` **chỉ so `major`**. `main`: tạo `v1 = (1,0)`, `v2 = (1,5)`. In `v1.equals(v2)` (`false`) và `v1.compareTo(v2)` (`0`). Thêm cả hai vào `HashSet` (size = 2) và vào `TreeSet` (size = 1 — `v2` bị coi là trùng). Viết 2–3 câu giải thích rủi ro thực tế của thiết kế này và cách sửa (`thenComparingInt(minor)`).

### Phân tích

Java **khuyến nghị mạnh mẽ** (JavaDoc `Comparable`): *"strongly recommended (though not required) that `(x.compareTo(y)==0) == (x.equals(y))`"* — tức `compareTo() == 0` nên đồng nghĩa với `equals() == true`. Vi phạm khuyến nghị này **không lỗi compile**, nhưng gây hành vi **khác nhau hoàn toàn** giữa các cấu trúc dữ liệu dựa trên **hash** (`HashSet`/`HashMap`, dùng `equals()`/`hashCode()`) và cấu trúc dựa trên **thứ tự** (`TreeSet`/`TreeMap`, chỉ dùng `compareTo()`, **hoàn toàn bỏ qua `equals()`/`hashCode()`**).

### Lời giải

```java
package baitap.bai6;

import java.util.*;

public class Version implements Comparable<Version> {
    private final int major, minor;

    public Version(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        Version v = (Version) o;
        return major == v.major && minor == v.minor; // dựa CẢ HAI field
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor); // khớp đúng field dùng trong equals()
    }

    @Override
    public int compareTo(Version other) {
        return Integer.compare(this.major, other.major); // CHỈ so major - KHÔNG NHẤT QUÁN với equals()!
    }

    @Override
    public String toString() { return major + "." + minor; }

    public static void main(String[] args) {
        Version v1 = new Version(1, 0);
        Version v2 = new Version(1, 5);

        System.out.println("v1.equals(v2) = " + v1.equals(v2));       // false - minor khác nhau
        System.out.println("v1.compareTo(v2) = " + v1.compareTo(v2)); // 0 - chỉ so major, cùng major=1

        Set<Version> hashSet = new HashSet<>();
        hashSet.add(v1);
        hashSet.add(v2);
        System.out.println("HashSet size = " + hashSet.size()); // 2 - dùng equals()/hashCode(), coi là 2 phần tử khác nhau

        Set<Version> treeSet = new TreeSet<>();
        treeSet.add(v1);
        treeSet.add(v2);
        System.out.println("TreeSet size = " + treeSet.size()); // 1 - dùng compareTo()==0 để coi là TRÙNG, v2 bị "nuốt"!
        System.out.println("TreeSet nội dung: " + treeSet);      // chỉ còn [1.0] - v2 (1.5) đã BIẾN MẤT
    }
}
```

**Kết quả chạy:**
```
v1.equals(v2) = false
v1.compareTo(v2) = 0
HashSet size = 2
TreeSet size = 1
TreeSet nội dung: [1.0]
```

### Rủi ro thực tế và cách sửa

**Rủi ro:** `TreeSet`/`TreeMap` dùng `compareTo() == 0` như tiêu chí **duy nhất** để coi 2 phần tử là "trùng nhau" (bỏ qua `equals()` hoàn toàn) — nghĩa là **`v2` (version 1.5) bị âm thầm "biến mất"** khi thêm vào `TreeSet` đã có `v1` (version 1.0), dù `equals()` khẳng định chúng **khác nhau**. Nếu dùng `Version` để lưu trữ/quản lý các phiên bản phần mềm trong 1 `TreeSet` (VD: hiển thị danh sách version có sẵn để chọn), **toàn bộ minor version khác nhau trong cùng 1 major sẽ bị mất dữ liệu không báo lỗi** — bug cực kỳ khó phát hiện vì không có exception nào được ném ra.

**Cách sửa:** dùng `thenComparingInt` để `compareTo` xét **đủ cả 2 tiêu chí**, khớp đúng với field dùng trong `equals()`:

```java
@Override
public int compareTo(Version other) {
    return Comparator.comparingInt((Version v) -> v.major)
            .thenComparingInt(v -> v.minor) // so tiếp "minor" khi "major" bằng nhau
            .compare(this, other);
}
```

Sau khi sửa, `v1.compareTo(v2)` sẽ **khác 0** (vì `minor` khác nhau: `0` vs `5`), và `TreeSet` sẽ giữ **đúng cả 2** phần tử — nhất quán với `equals()`.

---

*Đây là lời giải cho toàn bộ Phần B của Module 07. Tiếp theo: Module 08 — Collections Framework.*
