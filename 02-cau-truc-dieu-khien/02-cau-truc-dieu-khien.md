# Module 01.2 — Cấu trúc điều khiển (Control Flow) trong Java

> **Mức độ ưu tiên: Cao** — Đây là công cụ để viết ra *logic* thực sự. Nắm chắc `switch expression` + pattern matching (Java 14/21), phân tích luồng của compiler (unreachable code, definite assignment), và hiểu vì sao Java **không** tối ưu đệ quy đuôi (tail call) là những điểm tạo khác biệt so với lập trình viên chỉ quen cú pháp cũ.

> **Phạm vi bài học:** biểu thức điều kiện & kiểu `boolean`, `if/else`, ternary, `switch` (statement + expression + pattern matching), 4 loại vòng lặp, `break`/`continue`/nhãn, phân tích luồng lúc biên dịch, đệ quy & chuyển đệ quy → vòng lặp. Các chủ đề: exception/`try-finally` (Module 01.11), `Iterator`/`ConcurrentModificationException` chi tiết (Module 01.8 Collections), Stream/`forEach` (Module 01.10) **không** thuộc bài này — chỉ nhắc khi liên quan trực tiếp.

---

## Mục lục

1. [Biểu thức điều kiện & kiểu `boolean`](#1-biểu-thức-điều-kiện--kiểu-boolean)
2. [if / else / else if](#2-if--else--else-if)
3. [Toán tử ba ngôi (ternary)](#3-toán-tử-ba-ngôi-ternary)
4. [switch — statement cổ điển](#4-switch--statement-cổ-điển)
5. [switch expression & Pattern Matching (Java 14 / 21)](#5-switch-expression--pattern-matching-java-14--21)
6. [Vòng lặp (Loops)](#6-vòng-lặp-loops)
7. [break, continue & nhãn (labels)](#7-break-continue--nhãn-labels)
8. [Phân tích luồng lúc biên dịch — unreachable code & definite assignment](#8-phân-tích-luồng-lúc-biên-dịch--unreachable-code--definite-assignment)
9. [Đệ quy (Recursion)](#9-đệ-quy-recursion)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Biểu thức điều kiện & kiểu `boolean`

Mọi cấu trúc rẽ nhánh/vòng lặp trong Java (`if`, `while`, `for`, `do-while`, `? :`) đều yêu cầu điều kiện có kiểu **`boolean`** (hoặc `Boolean` sẽ bị auto-unboxing).

### Java là ngôn ngữ *strict boolean* — khác C/C++

```java
int x = 5;
if (x) { ... }          // ❌ lỗi compile: "incompatible types: int cannot be converted to boolean"
        if (x != 0) { ... }     // ✅
```

Hệ quả tích cực: lỗi gõ nhầm `=` thành `==` **bị compiler bắt** trong đa số trường hợp:

```java
int a = 1;
if (a = 2) { ... }      // ❌ lỗi compile — (a = 2) có kiểu int, không phải boolean
```

> ⚠️ **Ngoại lệ nguy hiểm:** khi hai vế là `boolean`, phép gán lọt lưới:
> ```java
> boolean done = false;
> if (done = true) { ... }   // BIÊN DỊCH ĐƯỢC — gán done = true rồi lấy kết quả true → luôn vào nhánh
> ```
> Viết `if (done)` hoặc dùng "Yoda style" `if (true == done)` (khi đó `true = done` là lỗi compile).

### `Boolean` (wrapper) trong điều kiện → nguy cơ NPE

```java
Boolean flag = someMap.get("enabled");   // có thể null
if (flag) { ... }                        // NullPointerException nếu flag == null (unboxing null)
        if (Boolean.TRUE.equals(flag)) { ... }   // ✅ null-safe
```

### Short-circuit — thứ tự đặt điều kiện có ý nghĩa

`&&` và `||` chỉ tính vế phải khi cần (xem Module 01.1 §5.2). Dùng để *bảo vệ*:

```java
if (user != null && user.isActive()) { ... }        // an toàn
        if (list.isEmpty() || list.get(0) == null) { ... }   // không IndexOutOfBounds
```

### Phủ định điều kiện — luật De Morgan

```
!(A && B)  ≡  !A || !B
!(A || B)  ≡  !A && !B
!(a > b)   ≡  a <= b          (cẩn thận với NaN: !(x > y) KHÔNG bằng x <= y khi có NaN)
```

> ⚠️ Với `double`/`float`, mọi phép so sánh chứa `NaN` đều cho `false`, nên `!(x < y)` **không** tương đương `x >= y`. Ví dụ `x = NaN, y = 1`: `x < y` → false, `!(x<y)` → true, nhưng `x >= y` → false.

---

## 2. if / else / else if

```java
int score = 75;

if (score >= 90) {
        System.out.println("Xuất sắc");
} else if (score >= 70) {
        System.out.println("Khá");
} else if (score >= 50) {
        System.out.println("Trung bình");
} else {
        System.out.println("Yếu");
}
```

`else if` không phải cú pháp riêng — nó chỉ là một `if` lồng trong `else` (Java không có `elif`/`elseif`). Chuỗi `if/else if` được đánh giá **tuần tự từ trên xuống**, dừng ở nhánh đầu tiên đúng → **thứ tự điều kiện quan trọng**: điều kiện hẹp/đặc biệt phải đặt trước điều kiện rộng.

### Luôn dùng block `{}`

```java
// Nguy hiểm — chỉ dòng đầu thuộc if
if (score >= 90)
        System.out.println("Xuất sắc");
    System.out.println("Chúc mừng!"); // LUÔN chạy, không phụ thuộc if
```

> ⚠️ **"Dangling else":**
> ```java
> if (a > 0)
>     if (b > 0)
>         System.out.println("Cả 2 dương");
> else
>     System.out.println("a không dương"); // else GHÉP với `if (b > 0)` gần nhất, KHÔNG phải if (a > 0)
> ```
> Java luôn ghép `else` với `if` gần nhất **chưa có** `else`. Luôn dùng `{}` để loại bỏ hoàn toàn mơ hồ này.

### Guard clause (early return) — giảm lồng sâu

Nhiều tầng `if` lồng nhau ("arrow code") khó đọc. Đảo điều kiện và thoát sớm:

```java
// ❌ lồng sâu
String process(Order o) {
    if (o != null) {
        if (o.isPaid()) {
            if (o.getItems().size() > 0) {
                return "OK";
            } else return "Giỏ hàng rỗng";
        } else return "Chưa thanh toán";
    } else return "Order null";
}

// ✅ guard clause — phần "đường hạnh phúc" (happy path) nằm phẳng ở cuối
String process(Order o) {
    if (o == null)                 return "Order null";
    if (!o.isPaid())               return "Chưa thanh toán";
    if (o.getItems().isEmpty())    return "Giỏ hàng rỗng";
    return "OK";
}
```

### `if` với hằng số — không có "conditional compilation"

Java không có tiền xử lý `#ifdef`. Nhưng `if` trên **compile-time constant** được compiler xử lý đặc biệt cho *reachability*:

```java
static final boolean DEBUG = false;
if (DEBUG) {
        System.out.println("trace...");   // hợp lệ — khối này coi là "unreachable có điều kiện",
}                                     // JIT/compiler có thể loại bỏ; KHÔNG bị lỗi unreachable

        while (false) { x = 1; }              // ❌ lỗi compile "unreachable statement" (luật riêng của while)
        if (false) { x = 1; }                // ✅ được phép (cố tình, để hỗ trợ "flag off")
```

---

## 3. Toán tử ba ngôi (ternary)

```java
String result = (score >= 50) ? "Đậu" : "Rớt";
int max = (a > b) ? a : b;
```

- Là **biểu thức** (có giá trị) — khác `if` là **statement**. Dùng được ở nơi cần một giá trị: khởi tạo field, tham số method, `return`.
- Kết hợp **phải sang trái**: `a ? b : c ? d : e` ≡ `a ? b : (c ? d : e)`.
- Chỉ dùng cho biểu thức đơn giản; lồng nhiều tầng làm giảm khả năng đọc nghiêm trọng.

> ⚠️ **Kiểu của biểu thức ternary tính lúc biên dịch, có áp dụng binary numeric promotion** (xem Module 01.1 §4.4):
> ```java
> Object o = true ? Integer.valueOf(1) : Double.valueOf(2); // o = 1.0 (Double!) — cả 2 nhánh nâng về double rồi box
> Integer i = null;
> Object r = false ? 0 : i;   // NullPointerException — nhánh `0` là int → cả biểu thức là int → unbox i (null)
> ```

> ⚠️ Không có `?:` "Elvis operator" (rút gọn `a != null ? a : b`) trong Java. Dùng `Objects.requireNonNullElse(a, b)` hoặc `Optional`.

---

## 4. switch — statement cổ điển

```java
int day = 3;
String dayName;

switch (day) {
        case 1:
dayName = "Thứ Hai";
        break;
        case 2:
dayName = "Thứ Ba";
        break;
        case 3:
dayName = "Thứ Tư";
        break;
default:
dayName = "Không xác định";
        break;
        }
```

### Kiểu được phép làm biểu thức `switch`

| Cho phép | Không cho phép |
|---|---|
| `byte`, `short`, `char`, `int` | `long`, `float`, `double`, `boolean` |
| `Byte`, `Short`, `Character`, `Integer` (auto-unbox) | kiểu tham chiếu bất kỳ khác |
| `String` (Java 7+) | |
| `enum` | |
| các kiểu tham chiếu bất kỳ — **chỉ trong switch pattern matching Java 21** | |

- Nhãn `case` phải là **hằng số biên dịch được** (compile-time constant) và **duy nhất**; không dùng biến, không dùng `case x > 5:` (kiểu cổ điển).
- `default` không bắt buộc và **không nhất thiết đặt cuối** (nhưng nên đặt cuối cho dễ đọc). Nếu không khớp case nào và không có `default` → cả `switch` bị bỏ qua.

### `switch` trên `String` hoạt động thế nào

Compiler sinh mã hai bước: `switch` trên `str.hashCode()` để chọn nhóm ứng viên, rồi `equals()` để xác nhận. Hệ quả: **`null` gây `NullPointerException`** ngay tại dòng `switch (str)` (gọi `str.hashCode()`), phải tự kiểm tra `null` trước.

### `switch` trên `enum`

```java
enum Status { NEW, PAID, SHIPPED }

switch (status) {          // KHÔNG viết case Status.NEW — chỉ `case NEW`
        case NEW -> ...;
        case PAID -> ...;
        case SHIPPED -> ...;
        }
```

`switch (someEnum)` cũng ném NPE nếu `someEnum == null` (kiểu cổ điển).

> ⚠️ **Fall-through khi quên `break`:**
> ```java
> int day = 2;
> switch (day) {
>     case 1: System.out.println("Thứ Hai");
>     case 2: System.out.println("Thứ Ba");   // thiếu break ở case 1
>     case 3: System.out.println("Thứ Tư");
>         break;
>     default: System.out.println("Khác");
> }
> // In: "Thứ Ba" VÀ "Thứ Tư" — code "rơi xuyên" qua các case sau tới khi gặp break/hết switch
> ```
> Đây là lỗi kinh điển và là lý do Java 14 ra đời `switch expression` (mục 5).

### Fall-through *có chủ đích* — gộp case (hợp lệ)

```java
switch (day) {
        case 6:
        case 7:
        System.out.println("Cuối tuần");
        break;
default:
        System.out.println("Ngày thường");
}
```

### Phạm vi biến trong `switch` cổ điển — một scope chung

```java
switch (x) {
        case 1:
int temp = 10;      // khai báo trong case 1
        break;
                case 2:
temp = 20;          // VẪN NHÌN THẤY temp (cùng scope switch) — nhưng có thể chưa gán → cẩn thận
        // int temp = 30;   // ❌ lỗi: biến trùng tên trong cùng scope
        break;
        }
```

Muốn scope riêng cho từng case: bọc thân case bằng `{ }`.

---

## 5. switch expression & Pattern Matching (Java 14 / 21)

`switch` trở thành **biểu thức trả về giá trị**, dùng mũi tên `->`, **không fall-through, không cần `break`**.

```java
int day = 3;

String dayName = switch (day) {
    case 1 -> "Thứ Hai";
    case 2 -> "Thứ Ba";
    case 3 -> "Thứ Tư";
    case 4 -> "Thứ Năm";
    case 5 -> "Thứ Sáu";
    case 6, 7 -> "Cuối tuần";      // nhiều nhãn cùng một nhánh
    default -> "Không xác định";
};
```

### Bốn hình thức cú pháp

| Cú pháp | Là gì | Fall-through |
|---|---|---|
| `case L -> expr;` | nhánh biểu thức | không |
| `case L -> { ...; yield v; }` | nhánh block, trả giá trị bằng `yield` | không |
| `case L: yield v;` | nhánh dạng dấu hai chấm của switch **expression** | **có** (hiếm dùng, tránh) |
| `case L -> statement;` | switch **statement** dạng mũi tên (không trả giá trị) | không |

```java
// dạng statement mũi tên — an toàn hơn switch statement cổ điển, không cần break
switch (command) {
        case "start" -> service.start();
    case "stop"  -> service.stop();
default      -> log.warn("unknown: {}", command);
}
```

### `yield` — trả giá trị từ block nhiều dòng

```java
String rank = switch (score / 10) {
    case 10, 9 -> "A";
    case 8 -> {
        log.info("Xếp loại Khá");
        yield "B";                  // yield, KHÔNG phải return
    }
    case 7 -> "C";
    default -> "D";
};
```

> `return` bên trong nhánh `switch` sẽ *thoát khỏi cả method*, không phải thoát switch — đó là lý do cần `yield`.

### Tính bao phủ (exhaustiveness) — bắt buộc

`switch` **expression** phải xử lý **mọi giá trị đầu vào có thể**, nếu không → lỗi compile:

```java
String s = switch (day) {
    case 1 -> "T2";
    case 2 -> "T3";
};                       // ❌ "the switch expression does not cover all possible input values"
```

- Với `int`/`String`: gần như luôn phải có `default`.
- Với `enum`: nếu đã liệt kê **đủ mọi hằng** thì được phép bỏ `default` (compiler tự thêm nhánh ngầm ném `MatchException` phòng khi enum thêm hằng mới sau này).

### `switch` cổ điển vs expression

| Tiêu chí | statement cổ điển | expression (Java 14+) |
|---|---|---|
| Trả giá trị trực tiếp | Không — gán biến trong từng case | Có |
| Cần `break` | Có — quên là bug | Không (dạng `->`) |
| Gộp nhãn | `case 1: case 2:` xếp chồng | `case 1, 2 ->` |
| Block nhiều dòng | tự do | dùng `yield` |
| Bao phủ đầu vào | không kiểm tra | **bắt buộc bao phủ hết** |
| Fall-through | mặc định (nguy hiểm) | không có (an toàn) |

> **Khuyến nghị:** dự án JDK 14+ ưu tiên `switch expression`/`->`. Vẫn phải đọc hiểu cú pháp cũ vì legacy code doanh nghiệp dùng rất nhiều.

### Pattern Matching cho `switch` (Java 21 — chính thức)

`switch` giờ nhận **mọi kiểu tham chiếu** và so khớp theo *mẫu*:

```java
Object obj = ...;

String desc = switch (obj) {
    case null            -> "null";                       // nhãn null TƯỜNG MINH (Java 21)
    case Integer i when i > 0 -> "Nguyên dương: " + i;    // guarded pattern — có `when`
    case Integer i       -> "Nguyên <= 0: " + i;
    case String s        -> "Chuỗi dài " + s.length();
    case int[] arr       -> "Mảng int " + arr.length + " phần tử";
    default              -> "Kiểu khác: " + obj.getClass().getSimpleName();
};
```

Điểm cần nhớ:

- **`case null`**: nếu không viết, `switch (obj)` với `obj == null` vẫn ném `NullPointerException` như cũ. Có `case null` thì bắt được `null` an toàn. Có thể gộp `case null, default ->`.
- **Thứ tự nhãn quan trọng**: nhãn cụ thể / có `when` phải đứng **trước** nhãn tổng quát hơn, nếu không → lỗi "label is dominated by a preceding case label".
- **`when`** (guard) chỉ lọc thêm; nếu guard `false`, KHÔNG rơi sang case khác mà đi tiếp việc so khớp các nhãn sau.
- Không còn khái niệm fall-through với nhãn mẫu.
- **Record patterns** (Java 21) cho phép "mổ" record ngay trong nhãn:
  ```java
  record Point(int x, int y) {}
  String r = switch (shape) {
      case Point(int x, int y) when x == y -> "Trên đường chéo";
      case Point(int x, int y)             -> "(" + x + ", " + y + ")";
      default -> "?";
  };
  ```
- Nếu không nhãn nào khớp và không có `default`/`case null` phù hợp → ném **`MatchException`** (kiểu mới của Java 21).

### Pattern matching cho `instanceof` (Java 16) — liên quan `if`

```java
if (obj instanceof String s && s.length() > 3) {
        System.out.println(s.toUpperCase());   // s tự động ép kiểu, chỉ trong phạm vi điều kiện đúng
        }
```

Biến `s` có phạm vi "theo luồng" (flow scoping): chỉ tồn tại ở nơi mà điều kiện `instanceof` chắc chắn đúng.

---

## 6. Vòng lặp (Loops)

### 6.1 `for` — khi kiểm soát được bước lặp

```java
for (int i = 0; i < 5; i++) {
        System.out.println("Lần lặp: " + i);
}
```

Cấu trúc `for (khởi tạo; điều kiện; cập nhật)` — cả 3 phần đều **có thể để trống**:

```java
for (;;) { ... }                     // vòng lặp vô hạn (tương đương while(true))
        for (int i = 0; i < n; ) { ...; i += step; }  // cập nhật ở thân

// nhiều biến + toán tử dấu phẩy (chỉ dùng được trong phần khởi tạo & cập nhật của for)
        for (int lo = 0, hi = a.length - 1; lo < hi; lo++, hi--) {
int t = a[lo]; a[lo] = a[hi]; a[hi] = t;   // đảo mảng tại chỗ
}
```

- Biến khai báo trong phần khởi tạo (`int i`) có **phạm vi giới hạn trong vòng `for`** — hết vòng là biến mất. Muốn dùng `i` sau vòng lặp thì khai báo bên ngoài.
- Điều kiện được kiểm tra **trước** mỗi lần lặp (kể cả lần đầu) → thân có thể chạy 0 lần.

> ⚠️ **Biến đếm kiểu số thực → vòng lặp sai/vô hạn:**
> ```java
> for (double d = 0.0; d != 1.0; d += 0.1) { ... }   // KHÔNG bao giờ d == 1.0 (sai số IEEE 754) → vô hạn
> ```
> Luôn dùng biến đếm số nguyên: `for (int k = 0; k < 10; k++) { double d = k * 0.1; ... }`.

> ⚠️ **Tràn số ở điều kiện:**
> ```java
> for (int i = 0; i <= Integer.MAX_VALUE; i++) { ... }   // VÔ HẠN — i++ sau MAX_VALUE cuộn về âm, luôn <= MAX_VALUE
> ```

### 6.2 `while` — kiểm tra điều kiện TRƯỚC

```java
int attempts = 0;
while (attempts < 3) {
        System.out.println("Thử lần " + attempts);
attempts++;                      // QUÊN dòng này → vòng lặp vô hạn
        }
```

> ⚠️ `while (cond);` (có dấu `;` ngay sau) là **vòng lặp với thân rỗng** — bug thầm lặng. Tương tự `for (...);`.

### 6.3 `do-while` — chạy ÍT NHẤT 1 lần, kiểm tra điều kiện SAU

```java
int choice;
do {
choice = readMenuChoice();
handle(choice);
} while (choice != 0);               // BẮT BUỘC dấu ; sau while
```

Chọn `do-while` khi thân **phải chạy trước** rồi mới có dữ liệu để kiểm tra: menu tương tác, đọc input tới khi hợp lệ, thuật toán lặp có bước khởi động.

### 6.4 Enhanced for-loop (for-each)

```java
int[] scores = {90, 85, 77};
for (int score : scores) {
        System.out.println(score);
}

        for (String name : List.of("a", "b")) { ... }   // dùng được với mọi Iterable
```

Compiler "giải đường" (desugar) for-each thành:
- với **mảng**: vòng `for` chỉ số cổ điển (`for (int i = 0; i < a.length; i++)`).
- với **`Iterable`**: dùng `Iterator` (`hasNext()` / `next()`).

Hạn chế:

- Không có chỉ số `i`.
- Biến vòng lặp (`score`, `name`) là **bản sao** — gán lại nó **không** sửa phần tử gốc trong mảng/collection.
- Không thể `remove`/`add` phần tử của collection đang duyệt → ném **`ConcurrentModificationException`** (dùng `Iterator.remove()` hoặc `removeIf`, sẽ học ở Module Collections).
- Không duyệt lùi, không nhảy bước.

### 6.5 Vòng lặp lồng nhau & hiệu năng

```java
for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
        // chạy n * m lần — độ phức tạp O(n·m)
        }
        }
```

| Kỹ thuật tối ưu | Ý nghĩa |
|---|---|
| Đưa biểu thức bất biến ra ngoài vòng (loop invariant hoisting) | `int len = list.size();` trước vòng thay vì gọi `.size()` mỗi lần |
| Thoát sớm (`break`) khi đã đủ điều kiện | tránh duyệt thừa |
| Chọn đúng thứ tự vòng khi duyệt mảng 2 chiều | duyệt theo hàng (row-major) thân thiện cache hơn duyệt theo cột |
| Tránh tạo object trong thân vòng nóng | giảm áp lực GC |

---

## 7. break, continue & nhãn (labels)

| Từ khóa | Tác dụng |
|---|---|
| `break` | Thoát **ngay** khỏi vòng lặp **hoặc** `switch` bao quanh gần nhất |
| `continue` | Bỏ phần còn lại của lần lặp hiện tại, sang lần kế tiếp (vòng `for`: **vẫn chạy phần cập nhật**) |

```java
for (int i = 1; i <= 10; i++) {
        if (i == 5) break;          // in: 1 2 3 4
        System.out.println(i);
}

        for (int i = 1; i <= 10; i++) {
        if (i % 2 == 0) continue;   // in: 1 3 5 7 9
        System.out.println(i);
}
```

> ⚠️ **`continue` trong `while` mà chưa tăng biến đếm → vòng lặp vô hạn:**
> ```java
> int i = 0;
> while (i < 5) {
>     if (i == 2) continue;   // i mãi = 2, không bao giờ thoát
>     System.out.println(i);
>     i++;
> }
> ```

> ⚠️ **`break` bên trong `switch` nằm trong vòng lặp chỉ thoát `switch`, không thoát vòng lặp:**
> ```java
> for (...) {
>     switch (x) {
>         case 1: break;      // thoát switch, vòng for VẪN chạy tiếp
>     }
> }
> ```
> Muốn thoát vòng lặp từ trong switch: dùng nhãn (bên dưới) hoặc cờ boolean / `return`.

> `break`/`continue` **không** nhảy ra khỏi method — để rời method giữa chừng dùng `return` (hoặc ném exception).

### Nhãn (labeled) break / continue — vòng lặp lồng nhiều tầng

```java
outer:
        for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
        if (matrix[i][j] == target) {
        System.out.println("Thấy tại (" + i + "," + j + ")");
            break outer;            // thoát HẲN cả 2 vòng
        }
                }
                }

search:
        for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
        if (skip(i, j)) continue search;   // sang lần lặp kế của vòng NGOÀI
    }
            }
```

- Nhãn là một *identifier* + dấu `:` đứng ngay trước vòng lặp (hoặc trước một block).
- `break <label>;` cũng dùng được với **block thường** (không phải vòng lặp):
  ```java
  done: {
      if (a) break done;   // nhảy ra khỏi block 'done'
      doSomething();
  }
  ```
- Dùng nhãn thay cho biến cờ (`boolean found = ...`) khi cần thoát nhiều tầng — gọn và rõ ý hơn. Nhưng lồng quá 2–3 tầng thường là dấu hiệu nên **tách method** và dùng `return`.

---

## 8. Phân tích luồng lúc biên dịch — unreachable code & definite assignment

Compiler Java thực hiện *reachability analysis* và *definite assignment analysis* trên mọi cấu trúc điều khiển. Đây là lý do một số đoạn code "hợp lý" vẫn bị từ chối.

### Unreachable code = lỗi biên dịch (không chỉ cảnh báo)

```java
return x;
System.out.println("never");   // ❌ "unreachable statement"

while (true) { ... }
        System.out.println("after");   // ❌ unreachable — while(true) không thể kết thúc bình thường

for (int i = 0; i < 10; i++) {
        break;
i++;                        // ❌ unreachable
        }
```

Luật đặc biệt:

- `while (false) { S }` → `S` **unreachable** → lỗi. Nhưng `if (false) { S }` thì **được phép** (cố tình cho "feature flag").
- `while (true)` / `for (;;)` không có `break` khiến mọi lệnh **sau** vòng lặp là unreachable.
- Một method có kiểu trả về non-`void` phải **đảm bảo mọi luồng đều `return`/`throw`**:
  ```java
  int sign(int x) {
      if (x > 0) return 1;
      if (x < 0) return -1;
      // ❌ "missing return statement" — thiếu nhánh x == 0
  }
  ```
  `switch` expression bao phủ đủ, hoặc `while(true)` không break, được compiler coi là "kết thúc chắc chắn".

### Definite assignment kết hợp với luồng điều khiển

Biến `final` (kể cả blank final) và biến local phải được compiler chứng minh là **đã gán chính xác trên mọi nhánh** trước khi đọc (xem Module 01.1 §1):

```java
final int grade;
switch (level) {
        case 1 -> grade = 10;
        case 2 -> grade = 8;
default -> grade = 5;        // BỎ nhánh này → lỗi "variable grade might not have been assigned"
        }
        System.out.println(grade);
```

`throw`/`return` trong một nhánh khiến nhánh đó "không cần gán" vì luồng không đi tiếp:

```java
int v;
if (cond) v = 1;
        else throw new IllegalStateException();
System.out.println(v);          // ✅ OK — nhánh else không "chảy" xuống đây
```

---

## 9. Đệ quy (Recursion)

Đệ quy = một **method tự gọi lại chính nó**, giải bài toán bằng cách quy về bài toán con **cùng cấu trúc, nhỏ hơn**.

### Cấu trúc bắt buộc

1. **Base case (điều kiện dừng):** trả kết quả trực tiếp, không gọi đệ quy. Thiếu → `StackOverflowError`.
2. **Recursive case:** gọi lại chính nó với input **tiến gần base case**.
3. (Ngầm) mỗi lời gọi phải **thực sự thu nhỏ** bài toán — nếu không, vẫn vô hạn dù "có" base case.

```java
public static long factorial(int n) {
    if (n < 0) throw new IllegalArgumentException("n âm");
    if (n <= 1) return 1;                 // base case
    return n * factorial(n - 1);          // recursive case
}
```

### Call Stack khi chạy `factorial(4)`

```
factorial(4)
  → 4 * factorial(3)
       → 3 * factorial(2)
            → 2 * factorial(1)
                 → return 1              (base case — bắt đầu "gấp về")
            → return 2 * 1  = 2
       → return 3 * 2  = 6
  → return 4 * 6  = 24
```

Mỗi lời gọi = **1 stack frame** (lưu tham số, biến local, địa chỉ trả về). Frame chỉ được giải phóng khi lời gọi con trả về → đệ quy càng sâu, stack càng phồng.

### `StackOverflowError` & kích thước stack

- Ném khi số frame vượt dung lượng stack của thread (mặc định thường ~512 KB, đủ cho khoảng **10.000–20.000** frame đơn giản — con số phụ thuộc số biến local mỗi frame).
- Chỉnh bằng cờ JVM `-Xss` (ví dụ `-Xss4m`) — nhưng đây là "vá triệu chứng"; giải pháp đúng thường là **chuyển sang vòng lặp**.
- `StackOverflowError` là `Error`, **không** nên `catch` để "xử lý tiếp".

### Java KHÔNG tối ưu đệ quy đuôi (Tail Call Optimization)

*Đệ quy đuôi* = lời gọi đệ quy là **thao tác cuối cùng** của method (không còn phép tính nào sau đó).

```java
// đệ quy đuôi (lời gọi cuối là chính nó, không nhân thêm gì)
long factTail(int n, long acc) {
    if (n <= 1) return acc;
    return factTail(n - 1, acc * n);      // Scala/Kotlin sẽ biến thành vòng lặp; JAVA THÌ KHÔNG
}
```

Kotlin (`tailrec`), Scala tối ưu dạng này thành vòng lặp (O(1) stack). **JVM/Java không** → `factTail(1_000_000, 1)` vẫn `StackOverflowError`. Vì vậy với dữ liệu lớn phải **tự chuyển sang lặp**:

```java
long factLoop(int n) {
    long acc = 1;
    for (int i = 2; i <= n; i++) acc *= i;
    return acc;
}
```

### Chuyển đệ quy → vòng lặp bằng `Deque` làm stack tường minh

Khi thuật toán *bản chất* là đệ quy (duyệt cây) nhưng lo tràn stack, mô phỏng call stack bằng `ArrayDeque`:

```java
void printTreeIterative(Node root) {
    Deque<Node> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        Node cur = stack.pop();
        System.out.println(cur.name);
        for (int i = cur.children.size() - 1; i >= 0; i--) {
            stack.push(cur.children.get(i));   // đẩy ngược để giữ thứ tự duyệt
        }
    }
}
```

### Fibonacci đệ quy thô — O(2ⁿ)

```java
public static long fib(int n) {
    if (n <= 1) return n;
    return fib(n - 1) + fib(n - 2);          // tính lại cùng giá trị vô số lần
}
```

> `fib(40)` mất vài giây (~1,6 tỷ lời gọi). Đây là bài toán mở đầu kinh điển cho **memoization / Dynamic Programming**.

**Memoization (top-down):**

```java
public static long fibMemo(int n, Map<Integer, Long> cache) {
    if (n <= 1) return n;
    Long hit = cache.get(n);
    if (hit != null) return hit;
    long v = fibMemo(n - 1, cache) + fibMemo(n - 2, cache);
    cache.put(n, v);
    return v;                                // mỗi n tính đúng 1 lần → O(n)
}
```

**Bottom-up (khử đệ quy hoàn toàn), O(n) thời gian, O(1) bộ nhớ:**

```java
public static long fibLoop(int n) {
    if (n <= 1) return n;
    long a = 0, b = 1;
    for (int i = 2; i <= n; i++) { long t = a + b; a = b; b = t; }
    return b;
}
```

### Đệ quy hỗ tương (mutual recursion)

```java
boolean isEven(int n) { return n == 0 || isOdd(n - 1); }
boolean isOdd(int n)  { return n != 0 && isEven(n - 1); }
```

Hai method gọi qua lại nhau — vẫn cần base case chung (`n == 0`) và input thu nhỏ.

### Đệ quy nhiều nhánh & quay lui (backtracking) — mẫu hình

Sinh mọi tập con, hoán vị, giải Sudoku... đều theo khung: *chọn → đệ quy → bỏ chọn (undo)*.

```java
void subsets(int[] nums, int idx, List<Integer> path, List<List<Integer>> out) {
    if (idx == nums.length) { out.add(new ArrayList<>(path)); return; }  // base case
    subsets(nums, idx + 1, path, out);                 // nhánh: KHÔNG chọn nums[idx]
    path.add(nums[idx]);
    subsets(nums, idx + 1, path, out);                 // nhánh: CÓ chọn
    path.remove(path.size() - 1);                      // undo — trả trạng thái về trước
}
```

### Khi nào đệ quy, khi nào vòng lặp

| Ưu tiên đệ quy | Ưu tiên vòng lặp |
|---|---|
| Cấu trúc cây / đồ thị / thư mục lồng nhau (không biết trước độ sâu) | Duyệt tuần tự mảng/list phẳng |
| Chia để trị: Merge Sort, Quick Sort, tìm kiếm nhị phân trên cây | Cần hiệu năng/bộ nhớ tối đa, tránh overhead stack frame |
| Định nghĩa toán học đệ quy tự nhiên (tổ hợp, Hanoi, backtracking) | Độ sâu có thể rất lớn (nguy cơ `StackOverflowError`) |
| Code phản ánh đúng bản chất bài toán, dễ đọc hơn | Đệ quy đuôi đơn giản — Java không tối ưu, viết loop luôn |

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Điều kiện | Bắt buộc `boolean`; `if (x)` với `int` là lỗi compile; `Boolean` null → NPE khi unbox |
| `=` vs `==` | Compiler bắt được, **trừ** khi cả hai vế là `boolean` (`if (done = true)`) |
| Dangling else | `else` ghép với `if` gần nhất chưa có else — luôn dùng `{}` |
| Guard clause | Đảo điều kiện + `return` sớm để làm phẳng code, tránh lồng sâu |
| Ternary | Là biểu thức; kết hợp phải→trái; numeric promotion có thể gây NPE khi unbox |
| `switch` cổ điển | Chỉ `byte/short/char/int/String/enum`; nhãn là hằng biên dịch; quên `break` → fall-through |
| `switch (str)` / `switch (enum)` | `null` → `NullPointerException` (kiểu cổ điển) |
| `switch` expression | `->`, không fall-through, không `break`; `yield` cho block; **bắt buộc bao phủ hết đầu vào** |
| `return` trong switch | Thoát cả method — dùng `yield` để chỉ trả giá trị cho switch |
| Pattern matching (21) | `case Type v`, `when` guard, `case null`, record patterns; sai thứ tự nhãn → "dominated"; không khớp → `MatchException` |
| `instanceof` pattern (16) | `if (o instanceof String s && ...)` — `s` có flow scoping |
| `for` | 3 phần đều bỏ được; toán tử `,` trong init/update; biến `i` chỉ sống trong vòng |
| Biến đếm `double` | Sai số IEEE 754 → vòng lặp sai/vô hạn; luôn đếm bằng số nguyên |
| `while` vs `do-while` | `do-while` chạy ít nhất 1 lần, kiểm tra sau, cần dấu `;` |
| for-each | Không có index; biến lặp là bản sao; sửa collection khi duyệt → `ConcurrentModificationException` |
| `break`/`continue` | `break` thoát vòng/switch gần nhất; `continue` trong `for` vẫn chạy phần cập nhật; không rời method |
| `break` trong switch-trong-loop | Chỉ thoát switch; muốn thoát loop dùng nhãn / cờ / `return` |
| Nhãn | `break/continue <label>` thoát nhiều tầng; dùng được với block thường |
| Unreachable code | **Lỗi compile**; `while(false){}` lỗi nhưng `if(false){}` hợp lệ |
| Definite assignment | Mọi nhánh phải gán trước khi đọc; `throw`/`return` làm nhánh "không chảy tiếp" |
| Đệ quy | Bắt buộc base case + input thu nhỏ; mỗi lời gọi 1 stack frame |
| Tail call | **Java KHÔNG tối ưu** — đệ quy đuôi sâu vẫn tràn stack; tự viết vòng lặp |
| Fibonacci thô | O(2ⁿ) → memoization O(n) → bottom-up O(n) time / O(1) space |
| Backtracking | Khung "chọn → đệ quy → undo" |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** In ra gì?
```java
int x = 5;
if (x > 10)
        System.out.println("A");
    System.out.println("B");
```

**Câu 2.** In ra gì?
```java
int n = 2;
switch (n) {
        case 1: System.out.println("Một");
    case 2: System.out.println("Hai");
    case 3: System.out.println("Ba"); break;
default: System.out.println("Khác");
}
```

**Câu 3.** Chuyển sang `switch expression`:
```java
String result;
switch (grade) {
        case 'A': result = "Xuất sắc"; break;
        case 'B': result = "Khá"; break;
default:  result = "Cần cố gắng";
        }
```

**Câu 4.** Chạy bao nhiêu lần lặp, in ra gì?
```java
for (int i = 0; i < 10; i++) {
        if (i == 3) continue;
        if (i == 7) break;
        System.out.println(i);
}
```

**Câu 5.** Hàm đệ quy sau thiếu gì? Chạy `countDown(5)` xảy ra chuyện gì?
```java
public static void countDown(int n) {
    System.out.println(n);
    countDown(n - 1);
}
```

**Câu 6.** Đoạn nào compile được, đoạn nào không? Vì sao?
```java
// (a)
boolean done = false;
if (done = true) System.out.println("in?");
// (b)
int k = 0;
if (k = 1) System.out.println("in?");
// (c)
Boolean flag = null;
if (flag) System.out.println("in?");
```

**Câu 7.** In ra gì và vì sao không bị lỗi "unreachable"?
```java
static final boolean DEBUG = false;
public static void main(String[] a) {
    if (DEBUG) System.out.println("trace");
    while (true) {
        System.out.println("x");
        break;
    }
    System.out.println("done");
}
```

**Câu 8.** `switch expression` sau có compile không? Nếu không, sửa tối thiểu thế nào?
```java
enum Dir { N, S, E, W }
int dx = switch (dir) {
    case E -> 1;
    case W -> -1;
    case N, S -> 0;
};
```

**Câu 9.** Kết quả từng dòng, giải thích:
```java
Object o = 5;
String s = switch (o) {
    case Integer i when i > 10 -> "lớn";
    case Integer i -> "nhỏ: " + i;
    case null -> "null";
    default -> "khác";
};
System.out.println(s);
```

**Câu 10.** Vòng lặp sau kết thúc không? Vì sao?
```java
for (int i = 1; i > 0; i++) {
        // ...
        }
```

**Câu 11.** In ra gì?
```java
for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
        if (j == 2) break;
        if (i == 2) continue;
        System.out.print(i + "" + j + " ");
    }
            }
```

**Câu 12.** Đoạn này in ra gì, và điểm bẫy ở đâu?
```java
int i = 0;
while (i < 3) {
        if (i == 1) continue;
        System.out.println(i);
i++;
        }
```

---

### Phần B — Bài tập viết code

**Bài 1 — Phân loại tam giác.**
`String classifyTriangle(double a, double b, double c)` trả về `"Không hợp lệ"` / `"Đều"` / `"Cân"` / `"Vuông"` / `"Thường"`. Dùng bất đẳng thức tam giác để kiểm tra hợp lệ; kiểm tra vuông bằng Pytago **có dung sai** (`Math.abs(...) < 1e-9`), không dùng `==` trên `double`.

**Bài 2 — Menu quán cà phê (switch expression + do-while).**
Đọc lựa chọn `int` bằng `Scanner`. Dùng **switch expression dạng `->`** trả về `record Item(String name, int price)` cho các món 1–4; lặp bằng `do-while` tới khi nhập `0`. Xử lý input không phải số (`InputMismatchException`) và lựa chọn ngoài phạm vi mà không làm sập chương trình.

**Bài 3 — Số nguyên tố tối ưu.**
`boolean isPrime(int n)` xử lý đúng `n < 2`, chỉ kiểm tra ước tới `√n` (dùng `i * i <= n` thay vì `Math.sqrt` để tránh số thực), và sau khi loại 2 thì chỉ duyệt các số lẻ. Giải thích ngắn gọn vì sao chặn tại `√n` là đủ.

**Bài 4 — Tam giác số (vòng lặp lồng).**
In với `n = 5`:
```
1
1 2
1 2 3
1 2 3 4
1 2 3 4 5
```
Chỉ dùng vòng lặp lồng và `StringBuilder` (không nối `+` trong vòng lặp — xem Module 01.1 §8).

**Bài 5 — Tìm trong ma trận bằng labeled break.**
`int[] findFirst(int[][] m, int target)` trả về `{row, col}` của ô đầu tiên bằng `target` (duyệt theo hàng), hoặc `{-1, -1}` nếu không có. Dùng **labeled break**, không dùng biến cờ `boolean`.

**Bài 6 — Đệ quy: tổng chữ số.**
`int sumOfDigits(int n)` cho `n >= 0`. Xác định rõ base case (`n < 10`) và recursive case (`n % 10 + sumOfDigits(n / 10)`). Viết thêm bản vòng lặp và so sánh.

**Bài 7 — Fibonacci: 3 phiên bản.**
Viết `fibNaive`, `fibMemo` (dùng `HashMap` hoặc mảng), `fibLoop` (bottom-up). Đo `fib(45)` bằng `System.nanoTime()` (có warm-up). Giải thích chênh lệch theo độ phức tạp O(2ⁿ) vs O(n).

**Bài 8 — Đệ quy: duyệt cây thư mục.**
```java
class Node { String name; boolean isFile; List<Node> children; }
```
Viết `void printTree(Node node, int depth)` in cây với thụt lề `"  ".repeat(depth)`. Viết thêm bản **khử đệ quy** bằng `ArrayDeque` (mục 9) và kiểm chứng hai bản cho cùng kết quả.

**Bài 9 — Chuyển đệ quy đuôi thành vòng lặp.**
Cho hàm đệ quy đuôi tính tổng mảng:
```java
int sum(int[] a, int i, int acc) {
    if (i == a.length) return acc;
    return sum(a, i + 1, acc + a[i]);
}
```
Giải thích vì sao `sum(bigArray, 0, 0)` có thể `StackOverflowError` trên Java dù là đệ quy đuôi, rồi viết lại bằng `for`.

**Bài 10 — FizzBuzz "có kiến trúc".**
In 1..100: bội 3 → `"Fizz"`, bội 5 → `"Buzz"`, bội cả hai → `"FizzBuzz"`. Làm 2 cách và so sánh khả năng đọc: (a) chuỗi `if/else if`; (b) `switch expression` trên biểu thức `(n % 3 == 0 ? 1 : 0) + (n % 5 == 0 ? 2 : 0)`.

---

### Phần C — Bài tập nâng cao (tư duy compiler / JVM)

**Bài 11.** Giải thích vì sao đoạn (a) compile lỗi còn (b) thì không, dù ngữ nghĩa "giống nhau":
```java
// (a)
int f() {
    while (true) { if (cond()) return 1; }
    return 0;   // ← lỗi hay không?
}
// (b)
int g() {
    for (;;) { if (cond()) return 1; }
    // không có return ở đây
}
```

**Bài 12.** `switch` expression trên `enum` có đủ mọi hằng thì được bỏ `default`. Điều gì xảy ra lúc **runtime** nếu sau này ai đó thêm hằng mới vào enum nhưng **không** biên dịch lại đoạn `switch` (chỉ thay file `.class` của enum)? Tên ngoại lệ là gì?

**Bài 13.** Viết một ví dụ pattern-matching `switch` (Java 21) bị lỗi compile "this case label is dominated by a preceding case label", rồi sửa bằng cách đổi thứ tự nhãn. Giải thích quy tắc "dominance".

**Bài 14.** Cho biết đoạn sau in gì và giải thích cơ chế "flow scoping" của biến `s`:
```java
static void test(Object o) {
    if (!(o instanceof String s)) {
        System.out.println("không phải String");
        return;
    }
    System.out.println("độ dài = " + s.length());   // s dùng được ở đây — tại sao?
}
```

**Bài 15.** `-Xss` là gì? Nếu một thuật toán duyệt cây đệ quy bị `StackOverflowError` với cây sâu 50.000 mức, hãy nêu **hai** hướng xử lý (một chỉnh JVM, một chỉnh thuật toán) và cho biết hướng nào nên ưu tiên trong sản phẩm thật, vì sao.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Phần A</summary>

1. In `"B"`. Thiếu `{}` → chỉ `println("A")` thuộc `if` (điều kiện sai nên không chạy); `println("B")` là câu lệnh độc lập, luôn chạy.
2. In `"Hai"` rồi `"Ba"`. `case 2` không có `break` → fall-through xuống `case 3`, gặp `break` mới dừng.
3.
```java
String result = switch (grade) {
    case 'A' -> "Xuất sắc";
    case 'B' -> "Khá";
    default  -> "Cần cố gắng";
};
```
4. In `0 1 2 4 5 6`. `i == 3` → `continue` (không in 3, nhưng `i++` vẫn chạy). `i == 7` → `break` (dừng hẳn, không in 7 trở đi). 8 lần vào thân, 7 lần tới `println`... thực tế in 6 số.
5. Thiếu **base case**. `countDown(5)` gọi mãi với `n` giảm không giới hạn (`... 0, -1, -2 ...`) → Call Stack đầy → `StackOverflowError`. Sửa: thêm `if (n < 0) return;` (hoặc `n <= 0`) ở đầu.
6. (a) **compile được** — `done = true` là biểu thức `boolean`; luôn in (bug logic điển hình). (b) **lỗi compile** — `k = 1` có kiểu `int`, không phải `boolean`. (c) **compile được** nhưng **`NullPointerException`** lúc chạy do unbox `flag == null`.
7. In `trace`? **Không** — `DEBUG` là `false`, khối `if (DEBUG)` không chạy nhưng cũng **không** bị lỗi unreachable (luật ưu ái `if` với hằng, khác `while(false)`). `while (true){ ...; break; }` in `"x"` một lần rồi thoát nhờ `break` (nên lệnh sau vòng KHÔNG unreachable). Output: `x` rồi `done`.
8. **Không compile** nếu compiler không chắc đã bao phủ hết (thực ra 4 hằng đã đủ → *compile được* và không cần `default`). Nếu muốn phòng xa enum thêm hằng, thêm `default -> 0;`. (Ý đồ câu hỏi: nhận ra enum-switch expression đủ hằng thì hợp lệ mà không cần `default`.)
9. In `nhỏ: 5`. `o` là `Integer 5`: nhãn đầu có `when i > 10` → false, bỏ qua; nhãn `case Integer i` khớp → `"nhỏ: 5"`. `case null` không cần vì `o != null`.
10. **Không kết thúc bình thường** — `i` tăng tới `Integer.MAX_VALUE`, `i++` tiếp theo **tràn** về `Integer.MIN_VALUE` (số âm) → `i > 0` thành false → **vòng lặp thoát** sau khi cuộn vòng. Vậy nó *có* kết thúc, nhưng sau ~2,1 tỷ vòng và nhờ overflow, không phải theo cách người viết mong đợi.
11. In `00 01 10 11 ` — với `i=0,1`: `j=0,1` in (`j==2` break trước khi in). Với `i=2`: `continue` nên không in gì.
12. In `0` rồi **treo vô hạn**. Khi `i == 1`, `continue` nhảy lên kiểm tra điều kiện **mà chưa chạy `i++`** → `i` mãi bằng 1.

</details>

<details>
<summary>Phần B — ý chính</summary>

- **Bài 1:** Hợp lệ: `a+b>c && a+c>b && b+c>a`. Sắp xếp 3 cạnh để lấy cạnh lớn nhất làm huyền; vuông: `Math.abs(x*x + y*y - z*z) < 1e-9`. Đều: cả 3 bằng nhau (dung sai). Cân: đúng 2 cạnh bằng nhau.
- **Bài 2:** Bọc `scanner.nextInt()` trong `try/catch (InputMismatchException e) { scanner.next(); }`. `switch` expression trả `Item`; nhánh `default` trả `null` hoặc `Item` "không hợp lệ".
- **Bài 3:** `if (n < 2) return false; if (n == 2) return true; if (n % 2 == 0) return false; for (int i = 3; (long) i * i <= n; i += 2) if (n % i == 0) return false; return true;` — nếu `n = p*q` thì một trong hai ước `<= √n`, nên không cần duyệt xa hơn.
- **Bài 4:** vòng ngoài `i = 1..n`, vòng trong `j = 1..i` append `j + " "`; `System.out.println(sb)` mỗi hàng, `sb.setLength(0)` để tái dùng.
- **Bài 5:** `outer: for (i...) for (j...) if (m[i][j]==target) { r=i; c=j; break outer; }`.
- **Bài 6:** base `n < 10 → n`; recursive `n % 10 + sumOfDigits(n / 10)`. Bản loop: `while (n > 0) { s += n % 10; n /= 10; }`.
- **Bài 7:** `fibNaive(45)` ~1,1 tỷ lời gọi (chậm rõ rệt). `fibMemo`/`fibLoop` gần như tức thời — mỗi giá trị tính 1 lần. Nhớ warm-up JIT trước khi đo.
- **Bài 8:** base `node.isFile` (hoặc `children` rỗng) → in rồi return. Recursive: `for (Node c : node.children) printTree(c, depth + 1)`. Bản khử đệ quy: `ArrayDeque` push root, pop-in-đẩy-con (đẩy ngược để giữ thứ tự).
- **Bài 9:** Java không có TCO → mỗi lời gọi `sum` vẫn tạo frame mới, mảng lớn → tràn stack. Bản `for`: `int acc = 0; for (int v : a) acc += v; return acc;`.
- **Bài 10:** (b) gọn hơn cho trường hợp có nhiều tổ hợp; ở đây `if/else if` với thứ tự "cả hai" trước vẫn rất rõ. Điểm học: đặt điều kiện hẹp nhất (`% 15 == 0`) lên đầu chuỗi `if`.

</details>

<details>
<summary>Phần C</summary>

- **Bài 11:** `while (true)` không có điều kiện thoát nào ngoài `return` bên trong → compiler coi vòng lặp "không kết thúc bình thường" ⇒ lệnh `return 0;` sau nó là **unreachable** ⇒ (a) **lỗi compile**. (b) `for (;;)` tương tự "vô tận", nên *không có* lệnh nào sau nó, method vẫn hợp lệ vì mọi luồng thoát đều qua `return 1;`. Cách sửa (a): bỏ `return 0;`.
- **Bài 12:** Nhánh ngầm do compiler chèn cho enum-switch expression bao phủ đủ sẽ ném **`MatchException`** (Java 21; trước đó là `IncompatibleClassChangeError`) khi gặp hằng enum mới chưa từng biết lúc biên dịch.
- **Bài 13:**
  ```java
  switch (obj) {
      case Object o -> "bất kỳ";      // nhãn này "thống trị" mọi nhãn sau
      case String s -> "chuỗi";       // ❌ dominated — không bao giờ tới được
  }
  ```
  Sửa: đặt `case String s` **trước** `case Object o`. Quy tắc dominance: nhãn tổng quát hơn (hoặc `default`, hoặc pattern không guard) che khuất mọi nhãn hẹp hơn đứng sau nó.
- **Bài 14:** In `"không phải String"` rồi return (nếu `o` không phải String); ngược lại in `"độ dài = ..."`. Flow scoping: biến `s` của `instanceof` pattern chỉ "sống" ở những điểm mà điều kiện chắc chắn đúng. Trong nhánh `if (!(o instanceof String s))` thân `if` là nơi điều kiện **sai**, nhưng vì nó kết thúc bằng `return`, phần code **sau** `if` chỉ đạt được khi `o instanceof String` **đúng** → `s` hợp lệ ở đó.
- **Bài 15:** `-Xss` đặt kích thước stack mỗi thread (vd `-Xss8m`). Hai hướng: (1) tăng `-Xss` — nhanh nhưng chỉ dời ngưỡng, tốn RAM theo số thread, vẫn vỡ nếu cây sâu hơn; (2) khử đệ quy bằng stack tường minh (`ArrayDeque`) hoặc thuật toán lặp — ổn định, không phụ thuộc cấu hình JVM. Sản phẩm thật ưu tiên (2).

</details>

---

*File tiếp theo trong lộ trình: **Module 01.3 — Class, Object, Method** (constructor, `this`, static vs instance, access modifier).*
