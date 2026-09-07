# Module 01.1 — Cú pháp & Kiểu dữ liệu trong Java

> **Mức độ ưu tiên: Cao** — Đây là nền tảng tuyệt đối. Mọi lỗi runtime khó hiểu sau này (NullPointerException, sai kết quả tính toán tiền tệ, so sánh String bằng `==` cho kết quả sai, tràn số `int` âm thầm, `0.1 + 0.2 != 0.3`...) đều bắt nguồn từ việc chưa nắm chắc phần này.

> **Phạm vi bài học:** cú pháp khai báo, 8 kiểu nguyên thủy + wrapper, quy tắc ép kiểu & thăng hạng kiểu (type promotion), toán tử & thứ tự ưu tiên, số thực IEEE 754, mảng, `String`/`StringBuilder`/`StringBuffer`, String Pool, mô hình bộ nhớ Stack/Heap và ngữ nghĩa *pass-by-value*. Các chủ đề `if/switch/loop` (Module 01.2), OOP (01.3+), Collections (01.8), `BigDecimal` chi tiết (Module riêng) **không** thuộc bài này.

---

## Mục lục

1. [Biến (Variables) & `var`](#1-biến-variables--var)
2. [Literal — cách viết giá trị trong mã nguồn](#2-literal--cách-viết-giá-trị-trong-mã-nguồn)
3. [Primitive Types vs Wrapper Class](#3-primitive-types-vs-wrapper-class)
4. [Ép kiểu (Casting) & Thăng hạng kiểu (Type Promotion)](#4-ép-kiểu-casting--thăng-hạng-kiểu-type-promotion)
5. [Toán tử (Operators) & thứ tự ưu tiên](#5-toán-tử-operators--thứ-tự-ưu-tiên)
6. [Số thực IEEE 754 — NaN, Infinity, sai số](#6-số-thực-ieee-754--nan-infinity-sai-số)
7. [Mảng (Arrays)](#7-mảng-arrays)
8. [String, StringBuilder, StringBuffer](#8-string-stringbuilder-stringbuffer)
9. [String Pool & Immutability](#9-string-pool--immutability)
10. [Mô hình bộ nhớ & Pass-by-value](#10-mô-hình-bộ-nhớ--pass-by-value)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. Biến (Variables) & `var`

Một biến trong Java là một vùng nhớ được đặt tên, dùng để lưu trữ dữ liệu, và **luôn phải có kiểu dữ liệu (type)** xác định tại thời điểm biên dịch — Java là ngôn ngữ *statically typed, strongly typed*.

```java
int age = 22;
double gpa = 3.65;
boolean isGraduated = false;
```

### Quy tắc đặt tên biến (identifier)

- Ký tự đầu là chữ cái Unicode, `_` hoặc `$` (không bắt đầu bằng chữ số). Về mặt kỹ thuật cho phép cả chữ tiếng Việt/emoji, nhưng **không bao giờ dùng** trong code thực tế.
- `$` theo quy ước chỉ dành cho code do máy sinh (generated code) — đừng tự đặt tên biến có `$`.
- Theo convention `camelCase`: `studentName`, `totalScore`.
- Không trùng **keyword** (`class`, `int`, `return`, `static`...) và **reserved literal** (`true`, `false`, `null`).
- Không trùng **contextual keyword** ở vị trí nhạy cảm: `var`, `yield`, `record`, `sealed`, `permits`, `module` — vẫn dùng làm tên biến được nhưng nên tránh.
- Tên nên mô tả rõ ý nghĩa — `daysUntilDeadline` tốt hơn `d`.

### Phạm vi biến (Variable Scope) & vòng đời

| Loại | Khai báo ở đâu | Giá trị mặc định | Vòng đời & nơi lưu |
|---|---|---|---|
| **Local variable** | Trong method / constructor / block `{}` | **Không có** — bắt buộc gán trước khi đọc | Sống trong block; slot nằm trên **stack frame** của lời gọi method |
| **Parameter** | Trong ngoặc `()` của method | Nhận giá trị từ lời gọi | Như local variable |
| **Instance variable (field)** | Trong class, ngoài method, **không** `static` | Có (`0` / `0.0` / `false` / `'\u0000'` / `null`) | Gắn với từng object, nằm trong **heap** cùng object |
| **Static variable (class variable)** | Trong class, có `static` | Có (như trên) | Dùng chung cho cả class, nằm trong vùng metadata của class, tồn tại suốt vòng đời class được nạp |

```java
public class Student {
    static int totalStudents = 0;   // static variable — 1 bản cho cả class
    String name;                    // instance variable — mỗi object 1 bản, mặc định null
    int credits;                    // instance variable — mặc định 0

    void enroll() {
        int localCounter = 1;       // local variable — chỉ sống trong method này
    }
}
```

> **Definite Assignment (gán xác định):** Trình biên dịch phân tích *mọi luồng thực thi có thể* để chắc chắn local variable đã được gán trước khi đọc. Nếu tồn tại **một** nhánh nào đó biến chưa được gán, compiler báo lỗi `variable might not have been initialized`.
> ```java
> int x;
> if (someCondition) x = 10;
> System.out.println(x); // ❌ lỗi compile — nhánh else không gán x
> ```
> Đây là điểm khác biệt cốt lõi so với instance/static variable (luôn có giá trị mặc định nên không bao giờ dính lỗi này).

### Che biến (Variable Shadowing)

Biến local (hoặc parameter) cùng tên sẽ **che** field cùng tên trong phạm vi của nó. Đây là lý do phải dùng `this.` trong constructor/setter:

```java
public class Account {
    private double balance;

    public Account(double balance) {
        // 'balance' ở đây là parameter, che field 'balance'
        this.balance = balance; // this.balance = field; balance = parameter
    }
}
```

### `final` — biến chỉ gán một lần

```java
final double PI = 3.14159;       // hằng số — gán khi khai báo
final int limit;                 // "blank final" — cho phép gán trễ...
if (mode == 1) limit = 100; else limit = 200; // ...nhưng đúng 1 lần trên mọi nhánh
```

- `final` với **kiểu tham chiếu** chỉ khóa *reference*, **không** khóa nội dung object: `final List<Integer> xs = new ArrayList<>(); xs.add(1); // OK`.
- Convention: hằng số (`static final`) dùng `UPPER_SNAKE_CASE`.
- **Compile-time constant:** một `static final` (hoặc `final` local) kiểu primitive/`String` được gán bằng *biểu thức hằng* sẽ được compiler **nội tuyến (inline)** thẳng giá trị vào nơi sử dụng — bytecode không còn tham chiếu tới biến nữa.
  ```java
  static final int MAX = 100;        // compile-time constant
  int y = MAX * 2;                    // bytecode chứa thẳng 200
  ```
  Hệ quả thực tế: nếu thư viện A phơi bày `public static final int VERSION = 1;` và bạn biên dịch code dùng hằng đó, việc thư viện A đổi thành `2` sẽ **không có hiệu lực** cho tới khi bạn *biên dịch lại* code của mình.

### `var` — suy luận kiểu cho biến local (Java 10+)

`var` **không phải** kiểu động (không giống JavaScript). Compiler suy ra kiểu tĩnh từ vế phải, sau đó biến bị khóa cứng vào kiểu đó.

```java
var name = "Pho";                 // suy ra String
var scores = new ArrayList<Integer>(); // suy ra ArrayList<Integer>
var total = 0L;                   // suy ra long (nhờ hậu tố L)

name = 123;                       // ❌ lỗi compile — name đã là String
```

**Chỉ dùng được cho:** local variable *có khởi tạo*, chỉ số vòng `for`, biến tài nguyên trong try-with-resources.

**Không dùng được cho:** field, parameter, kiểu trả về, biến khởi tạo `null`, khởi tạo mảng kiểu tắt (`var a = {1,2,3};` ❌).

| Nên dùng `var` khi | Nên tránh `var` khi |
|---|---|
| Kiểu đã hiển nhiên từ vế phải: `var user = new User();` | Vế phải là lời gọi method tên mơ hồ: `var x = compute();` — mất khả năng đọc hiểu |
| Kiểu generic dài dòng: `var map = new HashMap<String, List<Order>>();` | Muốn khai báo bằng *interface* nhưng khởi tạo bằng *lớp cụ thể* (`var` sẽ chọn lớp cụ thể) |

> ⚠️ **Bẫy với literal số:** `var i = 3;` là `int`, `var d = 3.0;` là `double`. `var x = 'a' + 1;` là `int` (giá trị 98), không phải `char`.

---

## 2. Literal — cách viết giá trị trong mã nguồn

### Literal số nguyên

```java
int dec  = 1_000_000;     // thập phân, '_' chỉ để dễ đọc (Java 7+)
int hex  = 0xFF;          // 255  — tiền tố 0x / 0X
int oct  = 0777;          // 511  — tiền tố 0 (BÁT PHÂN! nguồn bug kinh điển)
int bin  = 0b1010_0001;   // 161  — tiền tố 0b / 0B (Java 7+)
long big = 9_000_000_000L; // hậu tố L bắt buộc khi vượt phạm vi int (~2.1 tỷ)
```

- Mặc định literal số nguyên có kiểu `int`. Không có hậu tố cho `byte`/`short` — dùng literal `int` rồi để phép gán tự thu hẹp (nếu vừa phạm vi).
- Dấu `_` không được đặt ở đầu/cuối literal, không cạnh dấu chấm thập phân, không cạnh tiền tố/hậu tố: `0x_1` ❌, `1_.0` ❌, `100_L` ❌.
- **Bẫy bát phân:** `int code = 013;` ra `11`, không phải `13`. Cẩn thận khi copy mã bưu chính / mã zip có số 0 ở đầu.

### Literal số thực

```java
double d1 = 3.14;
double d2 = 1.5e3;        // 1500.0 — ký hiệu khoa học
float  f1 = 19.99f;       // hậu tố f/F bắt buộc (mặc định literal thực là double)
double hexFloat = 0x1.8p1; // 3.0 — literal thực hệ hex (hiếm dùng)
double under = 3_000.123_456;
```

### Literal `char` & chuỗi escape

`char` là số nguyên 16 bit không dấu (0–65535), lưu **một mã đơn vị UTF-16 (UTF-16 code unit)**.

```java
char a    = 'A';        // 65
char newl = '\n';       // xuống dòng
char tab  = '\t';
char quote= '\'';       // \'  \"  \\
char uni  = 'é';  // 'é' — Unicode escape: '\u' + 4 chữ số hex
char zero = '\0';       // ký tự NUL, giá trị 0 — cũng là giá trị mặc định của char
char num  = 65;         // hợp lệ: gán literal int vừa phạm vi cho char → 'A'
```

> ⚠️ **Unicode escape (`\uXXXX`) được xử lý ở giai đoạn quét mã nguồn, TRƯỚC cả khi phân tích cú pháp (lexing).** Hệ quả: nếu `\u000A` (mã của newline) xuất hiện ngay trong một comment `//`, nó bị biến thành ký tự xuống dòng thật, kết thúc comment sớm và có thể làm lộ/hỏng mã phía sau. Tương tự, viết `"` giữa chuỗi sẽ đóng chuỗi ngoài ý muốn. Đây là lý do chỉ nên dùng `\uXXXX` cho ký tự thật sự không gõ được, không dùng cho ASCII thường.

> ⚠️ **Ký tự ngoài BMP** (emoji, một số chữ Hán hiếm) cần **2 char** (surrogate pair) để biểu diễn. `"😀".length()` trả về `2`. Xử lý đúng cần dùng *code point* (`String.codePointCount`, `codePoints()` stream).

### Literal `boolean` và `null`

Chỉ có `true`, `false`, `null`. `null` không có kiểu — nó gán được cho *mọi* biến tham chiếu; `0` **không** phải `false`, `null` **không** phải `0`.

---

## 3. Primitive Types vs Wrapper Class

Java có **8 kiểu nguyên thủy (primitive type)** — không phải object, lưu trực tiếp giá trị (bit pattern), nhanh và tiết kiệm bộ nhớ.

| Primitive | Kích thước | Phạm vi giá trị | Giá trị mặc định | Wrapper Class |
|---|---|---|---|---|
| `byte` | 8 bit | −128 → 127 | 0 | `Byte` |
| `short` | 16 bit | −32,768 → 32,767 | 0 | `Short` |
| `int` | 32 bit | −2,147,483,648 → 2,147,483,647 | 0 | `Integer` |
| `long` | 64 bit | ≈ −9.22×10¹⁸ → 9.22×10¹⁸ (hậu tố `L`) | 0L | `Long` |
| `float` | 32 bit | ~6–7 chữ số thập phân có nghĩa (hậu tố `f`) | 0.0f | `Float` |
| `double` | 64 bit | ~15–16 chữ số thập phân có nghĩa (mặc định) | 0.0d | `Double` |
| `char` | 16 bit | 0 → 65,535 (1 code unit UTF-16) | `'\u0000'` | `Character` |
| `boolean` | JVM-defined (thường 1 byte trong mảng, 1 word trên stack) | `true` / `false` | `false` | `Boolean` |

- **Tất cả số nguyên trong Java đều có dấu**, ngoại trừ `char`. Không có `unsigned int`. Từ Java 8 có các method *xử lý như không dấu*: `Integer.parseUnsignedInt`, `Integer.toUnsignedLong`, `Integer.divideUnsigned`, `Integer.compareUnsigned`, `Long.remainderUnsigned`...
- Biểu diễn số âm: **bù 2 (two's complement)**. Hệ quả: `Math.abs(Integer.MIN_VALUE)` trả về... `Integer.MIN_VALUE` (số âm!) vì `+2147483648` không tồn tại trong `int`.
- `long`/`double` trên máy 32-bit về lý thuyết không đảm bảo đọc/ghi *nguyên tử* nếu không `volatile` (chi tiết ở Module concurrency) — nhưng đây là kiến thức bên lề ở bài này.

```java
long population = 8_000_000_000L;
float price = 19.99f;
char grade = 'A';
double eps = 1e-9;
```

### Hằng số hữu ích trong wrapper

```java
Integer.MAX_VALUE      // 2147483647
Integer.MIN_VALUE      // -2147483648
Long.MAX_VALUE
Double.MAX_VALUE       // ~1.7976931348623157E308
Double.MIN_VALUE       // ~4.9E-324 — số DƯƠNG nhỏ nhất, KHÔNG phải số âm nhất
Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN
Integer.SIZE           // 32 (số bit)   — Integer.BYTES = 4
Character.MIN_VALUE     // '\u0000'
```

### Wrapper Class — là gì và khi nào bắt buộc dùng

Wrapper class **bọc (wrap)** một giá trị primitive thành **Object bất biến (immutable)**. Bắt buộc dùng khi:

- Lưu trong **Collection / Generics** (`List<Integer>`, `Map<String, Double>`) — generics không hỗ trợ primitive.
- Cần `null` để biểu diễn "chưa có dữ liệu" (cột nullable trong DB, field optional trong JSON).
- Dùng API tiện ích: `Integer.parseInt`, `Integer.MAX_VALUE`, `Integer.compare`, `Integer.toBinaryString`, `Integer.bitCount`...

```java
List<Integer> scores = new ArrayList<>();   // không thể viết List<int>
scores.add(90);
Integer notGraded = null;                    // hợp lệ — "chưa chấm"
```

### `valueOf` vs `new` — và vì sao constructor wrapper bị "khai tử"

```java
Integer a = Integer.valueOf(100);  // ✅ có thể trả object từ cache
Integer b = new Integer(100);      // ⚠️ deprecated từ Java 9, xóa dần — LUÔN tạo object mới
```

Từ **Java 9**, các constructor `new Integer(...)`, `new Double(...)`, `new Boolean(...)`... bị `@Deprecated`. Lý do: chúng luôn cấp phát object mới (lãng phí, phá vỡ cache), trong khi `valueOf` mới là cách đúng. Autoboxing bên dưới cũng gọi `valueOf`.

### Autoboxing & Unboxing

Từ Java 5, compiler tự chèn lời gọi chuyển đổi:

```java
Integer boxed = 10;      // autoboxing  → biên dịch thành Integer.valueOf(10)
int unboxed = boxed;     // unboxing    → biên dịch thành boxed.intValue()
```

> ⚠️ **NPE khi unboxing `null`:**
> ```java
> Integer count = null;
> int result = count + 1; // NullPointerException — count.intValue() gọi ngầm trên null
> ```
> Rất phổ biến khi lấy dữ liệu từ DB (cột nullable ánh xạ `Integer`) rồi tính toán trực tiếp. Cũng xảy ra với ternary: `Integer x = flag ? null : 1;` an toàn, nhưng `int x = map.getOrDefault(k, 0)` an toàn còn `int x = flag ? 1 : map.get(k)` có thể NPE do binary numeric promotion (xem mục 4).

> ⚠️ **Hiệu năng — autoboxing trong vòng lặp:**
> ```java
> Long sum = 0L;
> for (long i = 0; i < 1_000_000; i++) sum += i; // mỗi vòng: unbox → cộng → BOX LẠI object mới
> ```
> Tạo cả triệu object `Long` rác. Dùng `long sum` (primitive) thay vì `Long`.

### Integer Cache — và các cache khác

```java
Integer a = 100, b = 100;
System.out.println(a == b); // true  — 100 nằm trong Integer Cache [-128, 127]

Integer x = 200, y = 200;
System.out.println(x == y); // false — ngoài cache → 2 object khác nhau
```

`Integer.valueOf` cache sẵn các object cho khoảng **−128 đến 127**. Cận trên có thể nâng bằng cờ JVM `-XX:AutoBoxCacheMax=<n>` (cận dưới cố định −128).

| Wrapper | Khoảng được cache bởi `valueOf` |
|---|---|
| `Boolean` | cả `TRUE` và `FALSE` (luôn cache) |
| `Byte` | toàn bộ (−128..127) |
| `Short`, `Integer`, `Long` | −128 → 127 |
| `Character` | 0 → 127 |
| `Float`, `Double` | **không cache gì cả** |

> **Quy tắc an toàn tuyệt đối:** so sánh *giá trị* của wrapper luôn dùng `.equals()` hoặc `Objects.equals(a, b)` (an toàn với `null`) hoặc `a.compareTo(b) == 0`. Chỉ dùng `==` khi *cố ý* so sánh reference.

### Parsing & format cơ bản

```java
int n     = Integer.parseInt("42");        // ném NumberFormatException nếu sai định dạng
int hexN  = Integer.parseInt("FF", 16);    // 255
Integer boxed = Integer.valueOf("42");     // trả Integer (có thể từ cache)
double d  = Double.parseDouble("3.14");
long l    = Long.parseLong("9000000000");

String bin = Integer.toBinaryString(10);   // "1010"
String hx  = Integer.toHexString(255);     // "ff"
int bits   = Integer.bitCount(255);        // 8
```

---

## 4. Ép kiểu (Casting) & Thăng hạng kiểu (Type Promotion)

### 4.1 Widening (mở rộng) — tự động, an toàn

Chuyển từ kiểu "nhỏ" sang kiểu "lớn hơn", compiler tự làm:

```java
int i = 100;
long l = i;      // int → long
double d = l;    // long → double
```

Chuỗi mở rộng: `byte → short → int → long → float → double`, và `char → int → long → float → double`.

> ⚠️ Widening vẫn có thể **mất độ chính xác** (không mất *độ lớn*): `int → float` và `long → float`/`long → double` — vì mantissa của float/double không đủ bit để giữ mọi số nguyên lớn.
> ```java
> int big = 123_456_789;
> float f = big;          // f = 1.23456792E8 — đã lệch!
> System.out.println((int) f); // 123456792, không phải 123456789
> ```

### 4.2 Narrowing (thu hẹp) — phải ép tường minh, có thể sai lệch

```java
double price = 19.99;
int truncated = (int) price;     // 19 — CẮT phần thập phân (truncate về 0), KHÔNG làm tròn

int n = 300;
byte b = (byte) n;               // 44 — lấy 8 bit thấp: 300 & 0xFF = 44

long big = 4_294_967_296L;        // 2^32
int overflow = (int) big;        // 0 — lấy 32 bit thấp

double huge = 1e20;
int clamp = (int) huge;          // 2147483647 — ép double→int quá lớn thì KẸP về MAX_VALUE
double nan = Double.NaN;
int fromNan = (int) nan;         // 0 — NaN ép sang số nguyên luôn ra 0
```

> **Ghi nhớ 2 cơ chế khác nhau:**
> - Số nguyên → số nguyên nhỏ hơn: **bỏ bit cao** (wraparound).
> - Số thực → số nguyên: cắt phần lẻ; nếu tràn thì **kẹp về MIN/MAX**; `NaN` → `0`.
> Muốn làm tròn đúng: `Math.round(price)` (trả `long` cho `double`, `int` cho `float`), hoặc `Math.floor`/`Math.ceil`.

### 4.3 Binary Numeric Promotion — quy tắc "nâng cấp" khi tính toán

Khi một toán tử số học/so sánh nhận **hai toán hạng khác kiểu**, Java nâng cả hai lên kiểu "cao nhất" theo thứ tự:

> nếu có `double` → cả hai thành `double`; ngược lại nếu có `float` → `float`; ngược lại nếu có `long` → `long`; **ngược lại cả hai thành `int`**.

Hệ quả cực quan trọng: **`byte`, `short`, `char` luôn được nâng lên `int` trước khi tính** (unary numeric promotion).

```java
byte x = 10, y = 20;
byte z = x + y;          // ❌ lỗi compile — (x + y) là int, không tự thu hẹp về byte
byte z2 = (byte)(x + y); // ✅ phải ép tường minh

char c = 'A';
int code = c + 0;        // 65 — c nâng lên int
System.out.println('A' + 'B');   // 131 (int), KHÔNG phải "AB"
System.out.println("" + 'A' + 'B'); // "AB" — có String thì '+' là nối chuỗi

short s = 5;
s = s + 1;               // ❌ int không thu hẹp
s += 1;                  // ✅ compound assignment TỰ chèn ép kiểu ngầm: s = (short)(s + 1)
```

> ⚠️ **Bẫy compound assignment ẩn ép kiểu:** `byte b = 10; b *= 2.5;` **biên dịch được** và cho `25` — vì `b *= 2.5` tương đương `b = (byte)(b * 2.5)`. Dễ mất dữ liệu âm thầm.

### 4.4 Kiểu của biểu thức ba ngôi (ternary) — bẫy tinh vi

Kiểu kết quả của `cond ? a : b` được xác định lúc biên dịch bằng luật riêng, có áp dụng binary numeric promotion:

```java
Object o = true ? Integer.valueOf(1) : Double.valueOf(2.0);
System.out.println(o);   // 1.0  (!) — cả hai nhánh bị nâng về Double rồi mới box

Integer i = null;
Object r = false ? 0 : i; // NullPointerException — nhánh '0' là int nên toàn biểu thức
                          // là int → i bị unbox (null.intValue())
```

### 4.5 Tràn số nguyên (Integer Overflow) — âm thầm, không có ngoại lệ

Số học `int`/`long` **không bao giờ ném lỗi khi tràn** — nó cuộn vòng theo bù 2:

```java
int max = Integer.MAX_VALUE;      // 2147483647
System.out.println(max + 1);      // -2147483648  (!!)

int a = 100_000, b = 100_000;
long product = a * b;             // -727379968 (!) — a*b tính ở int RỒI mới gán long
long ok = (long) a * b;           // 10000000000 — ép 1 toán hạng sang long TRƯỚC
```

Phòng tránh: dùng `long`/`BigInteger`, hoặc các method ném lỗi khi tràn (Java 8+):

```java
Math.addExact(a, b);       // ArithmeticException nếu tràn
Math.multiplyExact(a, b);
Math.subtractExact(a, b);
Math.negateExact(x);
Math.toIntExact(someLong);  // ArithmeticException nếu long không vừa int
Math.incrementExact(x);
```

### 4.6 Chia & lấy dư với số nguyên

```java
System.out.println(7 / 2);     // 3   — chia nguyên, cắt về 0
System.out.println(-7 / 2);    // -3  — cắt về 0 (KHÔNG phải -4)
System.out.println(-7 % 2);    // -1  — dấu của '%' theo SỐ BỊ CHIA (dividend)
System.out.println(7 % -2);    // 1
System.out.println(1 / 0);     // ArithmeticException: / by zero  (chỉ số nguyên)
System.out.println(1.0 / 0);   // Infinity  (số thực KHÔNG ném lỗi)

Math.floorDiv(-7, 2);          // -4  — làm tròn xuống (toán học)
Math.floorMod(-7, 2);          // 1   — dư luôn cùng dấu số chia; hữu ích khi tính index vòng
```

### 4.7 Reference casting (xem sâu ở Module OOP)

```java
Object obj = "Hello";
String s = (String) obj;       // downcast — có thể ném ClassCastException nếu kiểu thực sai
Object n = 42;
String bad = (String) n;       // ClassCastException lúc runtime
```

---

## 5. Toán tử (Operators) & thứ tự ưu tiên

| Nhóm | Toán tử | Ghi chú nhanh |
|---|---|---|
| Truy cập / gọi | `.` `()` `[]` `::` | ưu tiên cao nhất |
| Hậu tố | `expr++` `expr--` | trả giá trị *cũ* |
| Tiền tố / một ngôi | `++expr` `--expr` `+` `-` `!` `~` | `~` đảo bit; `!` phủ định boolean |
| Nhân/chia | `* / %` | |
| Cộng/trừ | `+ -` | `+` cũng là nối chuỗi khi có `String` |
| Dịch bit | `<< >> >>>` | |
| Quan hệ | `< <= > >= instanceof` | |
| Bằng | `== !=` | với object là so sánh *reference* |
| Bitwise AND | `&` | không short-circuit |
| Bitwise XOR | `^` | |
| Bitwise OR | `\|` | |
| Logic AND | `&&` | **short-circuit** |
| Logic OR | `\|\|` | **short-circuit** |
| Ba ngôi | `? :` | kết hợp phải sang trái |
| Gán | `= += -= *= /= %= &= ^= \|= <<= >>= >>>=` | kết hợp phải sang trái |

> Java **không có** toán tử `**` (lũy thừa) — dùng `Math.pow(a, b)` (trả `double`). Không nạp chồng toán tử (no operator overloading).

### 5.1 Thứ tự *tính* toán hạng — luôn từ trái sang phải

Java **luôn tính toán hạng trái trước toán hạng phải**, kể cả với `/`, `-`... (khác C/C++ vốn không định nghĩa). Kết hợp với `++` gây ra biểu thức khó đọc:

```java
int i = 1;
int r = i++ + i++ + i++;   // (1) + (2) + (3) = 6 ; sau đó i = 4
int[] arr = new int[3];
int k = 0;
arr[k++] = k;              // arr[0] = 1
```

`i++` trả giá trị *trước khi tăng*; `++i` tăng rồi mới trả. Đừng viết code phụ thuộc vào thứ tự này trong thực tế.

### 5.2 `&&` / `||` (short-circuit) vs `&` / `|` (eager)

```java
if (obj != null && obj.isValid()) { ... }  // an toàn — vế phải bị bỏ qua nếu obj == null
if (obj != null &  obj.isValid()) { ... }  // ❌ NPE — '&' luôn tính cả hai vế

boolean changed = updateA() | updateB();   // cố ý: muốn CẢ HAI method chạy dù A trả true
```

`&`, `|`, `^` khi hai vế đều `boolean` là toán tử logic *không short-circuit*. `^` với boolean = "khác nhau" (XOR).

### 5.3 Toán tử bit (bitwise) — trên số nguyên

```java
5 & 3     // 1     (0101 & 0011 = 0001)
5 | 3     // 7      (0111)
5 ^ 3     // 6      (0110)  — cũng dùng để hoán đổi không cần biến tạm
~5        // -6     (đảo mọi bit; ~x == -x - 1)

1 << 4    // 16     dịch trái n bit = nhân 2^n (có thể tràn dấu)
-8 >> 1   // -4     dịch phải SỐ HỌC: nhồi bit dấu vào bên trái
-8 >>> 1  // 2147483644  dịch phải LOGIC: nhồi 0 (không dấu)
```

> ⚠️ **Số lần dịch bị lấy modulo:** với `int`, `x << 33` giống `x << 1` (33 % 32 = 1). Với `long` thì modulo 64.

Ứng dụng thực tế: cờ bитмаск (`flags & MASK`), `n & 1` kiểm tra lẻ/chẵn (nhanh và đúng cả với số âm, khác `n % 2`), `n >>> 1` chia đôi an toàn khi tính `mid = (low + high) >>> 1` để tránh tràn khi cộng.

### 5.4 `==` với primitive vs với object

- Primitive: so sánh **giá trị bit**. Lưu ý `Double.NaN == Double.NaN` là `false` (mục 6).
- Object: so sánh **địa chỉ tham chiếu** (hai biến có trỏ cùng một object không). Muốn so sánh nội dung: `.equals()`.

### 5.5 Nối chuỗi bằng `+` — cách compiler xử lý

- Từ **Java 9**, `"a" + b + c` được biên dịch thành một lời gọi `invokedynamic` tới `StringConcatFactory` (không còn tự sinh `StringBuilder` như Java 8). Nghĩa là nối chuỗi **rời rạc, một lần** thì hoàn toàn tối ưu — **không cần** `StringBuilder` cho những trường hợp đó.
- Vấn đề hiệu năng chỉ xảy ra khi nối **lặp lại trong vòng lặp** (mục 8).
- `"x" + 1 + 2` → `"x12"`, nhưng `1 + 2 + "x"` → `"3x"` (trái sang phải, `1+2` là số trước).

---

## 6. Số thực IEEE 754 — NaN, Infinity, sai số

`float`/`double` tuân theo chuẩn **IEEE 754** (binary32 / binary64): mỗi giá trị = dấu × mantissa × 2^mũ. Vì cơ số 2, **không** biểu diễn chính xác các phân số thập phân "tròn" như `0.1`, `0.2`, `0.3`.

```java
System.out.println(0.1 + 0.2);            // 0.30000000000000004
System.out.println(0.1 + 0.2 == 0.3);     // false
System.out.println(0.3 - 0.1 - 0.2);      // -2.7755575615628914E-17
```

### Các giá trị đặc biệt

```java
1.0 / 0.0        // Infinity
-1.0 / 0.0       // -Infinity
0.0 / 0.0        // NaN  (Not-a-Number)
Math.sqrt(-1)    // NaN
Double.NaN == Double.NaN     // false  — NaN KHÔNG bằng chính nó
Double.isNaN(x)              // cách đúng để kiểm tra NaN
Double.compare(0.0, -0.0)    // 1  — compare phân biệt +0.0 và -0.0...
0.0 == -0.0                  // ...nhưng '==' coi chúng bằng nhau
```

> ⚠️ Vì `NaN != NaN`, một mảng chứa `NaN` sẽ khiến `Arrays.sort` cho thứ tự lạ, và `list.contains(Double.NaN)` có thể trả kết quả bất ngờ nếu không đi qua `Double.equals` (mà `Double.equals` thì coi `NaN` bằng `NaN` — ngược với `==`!).

### So sánh số thực đúng cách

```java
double a = 0.1 + 0.2, b = 0.3;
double EPS = 1e-9;
boolean equal = Math.abs(a - b) < EPS;                 // so sánh tương đối/tuyệt đối
int cmp = Double.compare(a, b);                        // xử lý NaN, ±0.0 nhất quán
```

### Vì sao KHÔNG dùng `float`/`double` cho tiền tệ

Cộng dồn hàng nghìn giao dịch, sai số tích lũy đủ để lệch vài xu — không chấp nhận được trong tài chính/kế toán. Giải pháp: **`BigDecimal`** (khởi tạo từ `String`, không từ `double`!) hoặc lưu số nguyên đơn vị nhỏ nhất (vd: lưu *xu* thay vì *đồng*). Chi tiết ở module riêng; ở bài này chỉ cần **nhận diện được vấn đề**.

### `float` hay `double`?

Mặc định luôn dùng `double`. Chỉ dùng `float` khi có ràng buộc bộ nhớ rất lớn (mảng hàng chục triệu phần tử) hoặc tương tác API đồ họa/khoa học yêu cầu. `strictfp` (buộc tính toán IEEE 754 nghiêm ngặt trên mọi nền tảng) đã trở thành hành vi mặc định từ Java 17.

---

## 7. Mảng (Arrays)

Mảng trong Java là **object** nằm trên **heap**, kích thước **cố định** sau khi tạo, các phần tử **cùng kiểu**, đánh index từ `0` đến `length - 1`.

```java
int[] numbers = new int[5];              // 5 phần tử, mặc định 0
int[] scores  = {90, 85, 77, 92, 60};    // khởi tạo tắt (chỉ khi khai báo)
int[] more    = new int[]{1, 2, 3};      // khởi tạo tắt tường minh (dùng được ở mọi nơi)

String[] names = new String[3];          // mặc định null (3 phần tử null)
boolean[] flags = new boolean[2];        // mặc định false

scores[0] = 100;
System.out.println(scores.length);       // 5 — .length là FIELD, không có ()
```

Cú pháp `int[] a` (khuyến nghị) và `int a[]` (kế thừa từ C, tránh dùng) là tương đương. `int[] a, b;` → **cả hai** là mảng; `int a[], b;` → `a` là mảng, `b` là `int`.

### Giá trị mặc định của phần tử

Khi tạo bằng `new`, mọi phần tử được khởi tạo giá trị mặc định của kiểu: `0` / `0.0` / `false` / `'\u0000'` / `null`. (Khác local variable — mảng *luôn* được zero-initialized.)

### Mảng nhiều chiều = "mảng của mảng"

Java không có mảng chữ nhật thực sự; `int[][]` là mảng mà mỗi phần tử là một `int[]` (có thể độ dài khác nhau — *jagged array*).

```java
int[][] matrix = {
    {1, 2, 3},
    {4, 5, 6}
};
System.out.println(matrix[1][2]);   // 6
System.out.println(matrix.length);  // 2 (số hàng)
System.out.println(matrix[0].length); // 3 (số cột hàng 0)

int[][] jagged = new int[3][];      // chỉ cấp chiều ngoài
jagged[0] = new int[]{1};
jagged[1] = new int[]{1, 2, 3};     // độ dài khác nhau — hợp lệ
// jagged[2] còn null → truy cập jagged[2][0] gây NPE

int[][] grid = new int[2][3];       // cấp cả hai chiều: 2 hàng, mỗi hàng 3 số 0
```

### Duyệt mảng

```java
for (int i = 0; i < scores.length; i++) System.out.println(scores[i]);

for (int score : scores) System.out.println(score);   // for-each: gọn, không có index,
                                                      // không sửa được phần tử gốc qua biến 'score'
```

### Lớp tiện ích `java.util.Arrays`

```java
import java.util.Arrays;

Arrays.sort(scores);                         // sắp xếp tăng dần (dual-pivot quicksort cho primitive)
Arrays.sort(objs, Comparator.reverseOrder());// object: merge sort ổn định, nhận Comparator
int idx = Arrays.binarySearch(scores, 85);   // mảng PHẢI đã sort; không thấy → trả (-(insertionPoint)-1)
Arrays.fill(numbers, 7);                      // gán tất cả = 7
int[] copy  = Arrays.copyOf(scores, 10);     // dài 10, phần dư = 0
int[] slice = Arrays.copyOfRange(scores, 1, 4); // [1, 4)
boolean eq  = Arrays.equals(scores, copy);    // so sánh nội dung 1 chiều
boolean deq = Arrays.deepEquals(m1, m2);      // so sánh nội dung mảng nhiều chiều
String s    = Arrays.toString(scores);        // "[60, 77, 85, 90, 92]"
String ds   = Arrays.deepToString(matrix);    // "[[1, 2, 3], [4, 5, 6]]"
int[] sorted= Arrays.stream(scores).sorted().toArray(); // bắc cầu sang Stream API
```

> ⚠️ **`Arrays.asList` với mảng primitive:** `Arrays.asList(new int[]{1,2,3})` trả về `List<int[]>` có **1 phần tử** (chính mảng đó), không phải `List<Integer>` 3 phần tử. Với `Integer[]` thì đúng như mong đợi. Ngoài ra list trả về có **kích thước cố định** — `add`/`remove` ném `UnsupportedOperationException`.

### Sao chép mảng — `clone()` là *shallow copy*

```java
int[] a = {1, 2, 3};
int[] b = a.clone();          // copy độc lập (primitive) — sửa b không ảnh hưởng a

String[][] m = {{"x"}, {"y"}};
String[][] mc = m.clone();    // SHALLOW — mc[0] và m[0] vẫn trỏ CÙNG mảng con
mc[0][0] = "z";               // m[0][0] cũng thành "z" (!)

System.arraycopy(a, 0, dest, 0, a.length); // API cấp thấp, nhanh, dùng khi copy 1 phần
```

### Array covariance & `ArrayStoreException`

Mảng trong Java là **hiệp biến (covariant)**: `String[]` *là* `Object[]`. Điều này cho phép lỗi lọt qua compiler nhưng nổ lúc runtime:

```java
Object[] arr = new String[3];   // hợp lệ (covariance)
arr[0] = "ok";
arr[1] = 42;                     // ArrayStoreException lúc runtime — kiểu thực là String[]
```

(Đây là một lý do Generics được thiết kế *bất biến* — invariant — sẽ học ở Module Generics.)

### Mảng vs `ArrayList`

| | Mảng | `ArrayList` |
|---|---|---|
| Kích thước | Cố định | Tự resize (thường nhân đôi capacity khi đầy) |
| Kiểu phần tử | Primitive **hoặc** object | Chỉ object (phải box primitive) |
| Truy cập phần tử | `a[i]` | `list.get(i)` |
| Độ dài | `a.length` | `list.size()` |
| API | `Arrays.*` | phong phú (add/remove/contains...) |

Hiểu rõ hạn chế "kích thước cố định" của mảng chính là lý do Collections Framework (Module 01.8) tồn tại.

> ⚠️ **`ArrayIndexOutOfBoundsException`** khi index < 0 hoặc ≥ `length`. **`NegativeArraySizeException`** khi `new int[-1]`. **`NullPointerException`** khi truy cập `.length` / phần tử của biến mảng đang `null`.

---

## 8. String, StringBuilder, StringBuffer

### 8.1 `String` — bất biến (immutable)

```java
String greeting = "Hello";
greeting = greeting + " World"; // KHÔNG sửa chuỗi cũ — tạo String MỚI rồi gán lại reference
```

Mọi method "biến đổi" (`concat`, `substring`, `replace`, `toUpperCase`, `trim`, `strip`...) đều **trả về object String mới**; object gốc không đổi. Nội bộ (từ Java 9) `String` lưu một `byte[]` + một cờ `coder` (LATIN1 hoặc UTF16) — gọi là **Compact Strings**, tiết kiệm nửa bộ nhớ cho chuỗi thuần ASCII.

### 8.2 Method String hay dùng

```java
String s = "  Java Backend Developer  ";

s.length();                    // 27  (kể cả khoảng trắng)
s.isEmpty();                   // length == 0
s.isBlank();                   // chỉ chứa whitespace? (Java 11+)
s.trim();                      // bỏ ký tự <= U+0020 ở hai đầu
s.strip();                     // như trim nhưng theo Character.isWhitespace, Unicode-aware (Java 11+)
s.stripLeading(); s.stripTrailing();
s.toUpperCase(); s.toLowerCase();  // NÊN truyền Locale: s.toUpperCase(Locale.ROOT)
s.substring(2, 6);             // "Java"  — [2, 6)
s.indexOf("Backend");          // vị trí đầu tiên, -1 nếu không có
s.lastIndexOf('a');
s.charAt(2);                   // 'J'
s.replace("Java", "Kotlin");   // thay MỌI lần xuất hiện (không regex)
s.replaceAll("\\s+", "_");     // regex
s.split("\\s+");               // tách theo regex → String[]
s.contains("Backend");         // true/false
s.startsWith("  Java"); s.endsWith("  ");
s.equals("Java");              // so sánh nội dung (chuẩn)
s.equalsIgnoreCase("java");
s.compareTo("abc");            // thứ tự từ điển theo giá trị char (< 0 / 0 / > 0)
s.chars();                     // IntStream các code unit
s.repeat(3);                   // Java 11+
"a,b,c".split(",");            // ["a", "b", "c"]
String.join(", ", "a", "b");   // "a, b"
String.format("%s là %d tuổi", "Pho", 22);
"%s là %d".formatted("Pho", 22);  // Java 15+
"line1\nline2".lines();        // Stream<String> (Java 11+)
String.valueOf(42);            // "42" — cách an toàn khi giá trị có thể null
Integer.toString(42);
```

> ⚠️ **`substring`/`indexOf` làm việc theo *char* (UTF-16 code unit), không theo *ký tự người dùng thấy*.** Với emoji hoặc ký tự ngoài BMP, cắt nhầm giữa surrogate pair cho ra ký tự hỏng.

> ⚠️ **`toUpperCase()`/`toLowerCase()` không truyền `Locale` phụ thuộc locale máy chủ.** Kinh điển: locale Thổ Nhĩ Kỳ biến `"i".toUpperCase()` thành `"İ"` (I có chấm) → so sánh/định danh sai. Với xử lý dữ liệu kỹ thuật luôn dùng `Locale.ROOT`.

### 8.3 Text Blocks — chuỗi nhiều dòng (Java 15+)

```java
String json = """
    {
        "name": "Pho",
        "role": "backend"
    }
    """;
```

- Mở đầu bằng `"""` rồi **xuống dòng ngay** (không ký tự nào sau `"""` mở).
- *Incidental whitespace* (khoảng trắng thụt lề chung) bị loại bỏ tự động, tính theo dòng thụt ít nhất và vị trí `"""` đóng.
- `\` cuối dòng: nối dòng (bỏ ký tự newline). `\s`: giữ một khoảng trắng (chống bị trim).

### 8.4 `StringBuilder` — mutable, KHÔNG thread-safe

```java
StringBuilder sb = new StringBuilder();      // hoặc new StringBuilder(256) đặt capacity trước
sb.append("Java").append(' ').append(2024);  // sửa TRỰC TIẾP buffer char[]/byte[] nội bộ
sb.insert(0, ">> ");
sb.replace(0, 2, "**");
sb.deleteCharAt(0);
sb.reverse();
sb.setLength(0);                             // "xóa sạch" để tái dùng buffer
int len = sb.length();
String result = sb.toString();               // chỉ lúc này mới tạo String
```

Bên trong là một mảng tự **resize** (thường `newCap = oldCap * 2 + 2`). Đặt capacity ban đầu hợp lý nếu ước lượng được kích thước → tránh nhiều lần copy khi lớn dần.

### 8.5 `StringBuffer` — giống `StringBuilder` nhưng thread-safe

Mọi method `synchronized`. Trên thực tế **hiếm khi hữu ích**: một `StringBuilder` gần như luôn là biến local (không chia sẻ giữa thread), còn khi thực sự cần chia sẻ thì đồng bộ ở tầng cao hơn hợp lý hơn là khóa từng `append`. Có mặt chủ yếu vì lý do lịch sử (ra đời Java 1.0, trước `StringBuilder` của Java 5).

### 8.6 Bảng so sánh then chốt

| Tiêu chí | `String` | `StringBuilder` | `StringBuffer` |
|---|---|---|---|
| Mutable? | Không | Có | Có |
| Thread-safe? | Có (do immutable) | **Không** | Có (`synchronized`) |
| Ra đời | 1.0 | 5 | 1.0 |
| Hiệu năng nối lặp | Kém (O(n²)) | **Tốt nhất (O(n))** | Tốt nhưng chậm hơn do khóa |
| Khi nào dùng | Chuỗi cố định / nối rời rạc vài lần | Nối trong vòng lặp, đơn luồng (đa số) | Gần như không — ưu tiên `StringBuilder` |

> ⚠️ **Bẫy kinh điển — nối String trong vòng lặp:**
> ```java
> String result = "";
> for (int i = 0; i < 10_000; i++) result += i; // mỗi vòng tạo 1 String mới + copy toàn bộ
> // → O(n²) thời gian & rác bộ nhớ khổng lồ
> ```
> Sửa:
> ```java
> StringBuilder sb = new StringBuilder();
> for (int i = 0; i < 10_000; i++) sb.append(i);
> String result = sb.toString();               // O(n)
> ```
> **Lưu ý ngược lại:** nối **rời rạc, không lặp** như `"Hi " + name + ", age " + age` thì cứ dùng `+` — Java 9+ đã tối ưu bằng `invokedynamic`, tự tay `StringBuilder` không nhanh hơn mà còn khó đọc.

### 8.7 `hashCode()` của String

Công thức cố định trong spec: `s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]`. Vì `String` bất biến nên giá trị này được **cache** sau lần tính đầu (`"".hashCode() == 0`). Đó là lý do `String` làm key `HashMap` cực hiệu quả.

---

## 9. String Pool & Immutability

### 9.1 Vì sao String bất biến

1. **Bảo mật:** dùng lưu username, mật khẩu, URL, đường dẫn, tham số kết nối DB — nếu mutable, một đoạn code có thể sửa giá trị *sau khi đã validate*.
2. **Thread-safe miễn phí:** nhiều thread đọc chung một `String` không cần đồng bộ.
3. **Cho phép String Pool:** chia sẻ literal an toàn vì không ai sửa được.
4. **Cache `hashCode()`:** tính một lần, dùng mãi → key `HashMap`/`HashSet` nhanh.
5. **An toàn khi làm khóa/hằng:** giá trị không đổi trong suốt vòng đời.

### 9.2 String Pool (String Constant Pool)

Vùng nhớ đặc biệt trong **heap** (từ Java 7; trước đó ở PermGen), JVM lưu các **String literal** để tái sử dụng.

```java
String a = "hello";           // Pool chưa có "hello" → tạo, đưa vào Pool
String b = "hello";           // Pool đã có → TRẢ REFERENCE CŨ
System.out.println(a == b);        // true  — cùng object trong Pool
System.out.println(a.equals(b));   // true

String c = new String("hello");    // ÉP tạo object mới NGOÀI Pool (trên heap thường)
System.out.println(a == c);        // false — 2 object khác nhau
System.out.println(a.equals(c));   // true  — equals() so sánh NỘI DUNG
```

**Nối chuỗi lúc biên dịch cũng vào Pool** (constant folding):

```java
String x = "hel" + "lo";           // compiler gộp thành "hello" LÚC BIÊN DỊCH
System.out.println(a == x);         // true

String part = "hel";
String y = part + "lo";            // 'part' là biến (không phải hằng) → tính lúc runtime → object mới
System.out.println(a == y);         // false
final String pf = "hel";
String z = pf + "lo";             // pf là compile-time constant → gộp lúc biên dịch
System.out.println(a == z);         // true
```

**Sơ đồ vùng nhớ:**

```
String a = "hello";           String c = new String("hello");
        │                                │
        ▼                                ▼
   ┌─────────────┐                 ┌─────────────┐
   │ String Pool │                 │  Heap thường │
   │  "hello" ◄──┼── a, b, x, z    │ new String  │◄── c
   └─────────────┘                 │  ("hello")  │
                                   └──────┬──────┘
                                          │ .intern()
                                          ▼ trả reference trong Pool (== a)
```

### 9.3 `intern()`

```java
String c = new String("hello");
String d = c.intern();             // nếu Pool đã có nội dung bằng → trả reference đó; nếu chưa → thêm vào
System.out.println(a == d);         // true
```

> Trong thực tế **hiếm khi tự gọi `intern()`** — dễ nghĩ là tối ưu bộ nhớ nhưng thường phản tác dụng (chi phí tra bảng hash toàn cục, giữ chuỗi sống lâu). Chỉ cân nhắc khi nạp lượng cực lớn chuỗi trùng lặp và đã đo đạc.

### 9.4 Quy tắc vàng khi so sánh String

> ⚠️ **KHÔNG BAO GIỜ dùng `==` để so sánh nội dung String.** Luôn `.equals()` / `.equalsIgnoreCase()`.

```java
String input = scanner.nextLine();       // "yes" — đến từ new String() nội bộ Scanner, KHÔNG ở Pool
if (input == "yes") { ... }              // ❌ luôn false
if (input.equals("yes")) { ... }         // ✅
if ("yes".equals(input)) { ... }         // ✅✅ "Yoda condition" — an toàn cả khi input == null
if (Objects.equals(input, "yes")) { ... }// ✅ null-safe cả hai phía
```

---

## 10. Mô hình bộ nhớ & Pass-by-value

Hiểu Stack/Heap giúp lý giải hầu hết hành vi ở các mục trên.

| | **Stack** | **Heap** |
|---|---|---|
| Chứa gì | Stack frame mỗi lời gọi method: biến local (primitive) & **reference** (địa chỉ) tới object | Bản thân **object** & mảng; field của object; String Pool |
| Vòng đời | Tạo/hủy theo lời gọi/kết thúc method | Sống tới khi không còn reference → Garbage Collector thu hồi |
| Tốc độ | Rất nhanh (chỉ dịch con trỏ stack) | Chậm hơn (cấp phát + GC) |
| Phạm vi | Riêng từng thread | Chia sẻ giữa các thread |

```java
int x = 10;                    // giá trị 10 nằm ngay trên stack
int[] arr = {1, 2, 3};         // 'arr' (reference) trên stack; mảng {1,2,3} trên heap
String s = "hi";               // 's' trên stack; object "hi" trong String Pool (heap)
```

### Java **luôn** truyền tham số theo giá trị (pass-by-value)

Không có pass-by-reference trong Java. Cái được sao chép khi gọi method là:
- **primitive:** sao chép *giá trị*.
- **object:** sao chép *giá trị của reference* (địa chỉ) — không sao chép object.

```java
static void addOne(int n)        { n = n + 1; }              // không ảnh hưởng biến gốc
static void fill(int[] a)        { a[0] = 99; }              // SỬA được nội dung object qua reference sao chép
static void reassign(int[] a)    { a = new int[]{7, 8, 9}; } // KHÔNG ảnh hưởng — chỉ đổi bản sao reference
static void grow(String t)       { t = t + "!"; }            // KHÔNG ảnh hưởng — String bất biến + reference là bản sao

int v = 5;           addOne(v);      // v vẫn 5
int[] xs = {1,2,3};  fill(xs);       // xs[0] == 99   (sửa qua reference)
                     reassign(xs);   // xs vẫn {99,2,3}
String name = "Pho"; grow(name);     // name vẫn "Pho"
```

> **Câu chốt để nhớ:** method có thể *thay đổi trạng thái bên trong* object mà tham số trỏ tới, nhưng **không thể** khiến biến của người gọi trỏ sang object khác.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Local variable | Không có giá trị mặc định; compiler kiểm tra *definite assignment* trên mọi nhánh |
| `var` | Suy luận kiểu **tĩnh** lúc biên dịch, chỉ cho local có khởi tạo; không phải kiểu động |
| `final` | Gán một lần; với object chỉ khóa reference; compile-time constant bị inline vào bytecode |
| Literal | `0x` hex, `0b` binary, `0` **octal** (bẫy!), `_` phân tách; `L`/`f` hậu tố; `\uXXXX` xử lý ở giai đoạn quét |
| 8 primitive | Tất cả số nguyên **có dấu** trừ `char`; số âm dùng bù 2 |
| Wrapper | Bất biết; `valueOf` (có cache) thay cho `new` (deprecated Java 9+); NPE khi unbox `null` |
| Integer Cache | `−128..127` cho Byte/Short/Integer/Long, `0..127` Character, luôn cache Boolean; Float/Double không cache |
| Widening | Tự động; `int/long → float`, `long → double` vẫn **mất độ chính xác** |
| Narrowing | Tường minh; số nguyên → bỏ bit cao; số thực → cắt lẻ, tràn thì kẹp MIN/MAX, `NaN → 0` |
| Numeric promotion | `byte/short/char` luôn nâng `int` trước khi tính; `byte b; b += 1` ẩn ép kiểu |
| Ternary | Kiểu kết quả tính lúc biên dịch, có numeric promotion → có thể unbox gây NPE |
| Overflow | `int/long` cuộn vòng **im lặng**; dùng `Math.addExact/multiplyExact` hoặc `long`/`BigInteger` |
| Chia nguyên | Cắt về 0; `%` mang dấu số bị chia; `1/0` ném lỗi, `1.0/0` = `Infinity` |
| `&&`/`||` vs `&`/`|` | Chỉ `&&`/`||` short-circuit; `&`/`|` luôn tính cả hai vế |
| `>>` vs `>>>` | `>>` giữ dấu; `>>>` nhồi 0; số lần dịch lấy modulo 32 (int) / 64 (long) |
| IEEE 754 | `0.1+0.2 != 0.3`; `NaN != NaN` (dùng `Double.isNaN`); không dùng `double` cho tiền tệ |
| Mảng | Object trên heap, kích thước cố định, phần tử zero-init; `.length` là field; covariant → `ArrayStoreException` |
| Mảng nhiều chiều | Là "mảng của mảng" (jagged được phép); `clone()` là shallow copy |
| `String` | Bất biến; nội bộ `byte[]` + coder (Compact Strings Java 9+) |
| Nối `+` | Rời rạc thì tối ưu sẵn (invokedynamic, Java 9+); chỉ dùng `StringBuilder` khi nối **trong vòng lặp** |
| `StringBuffer` | Thread-safe nhưng gần như luôn nên thay bằng `StringBuilder` |
| String Pool | Literal & hằng biên dịch được chia sẻ; `new String()` nằm ngoài Pool; `intern()` hiếm khi cần |
| So sánh String | `.equals()` / `Objects.equals()` / `"const".equals(x)`; không bao giờ `==` cho nội dung |
| Pass-by-value | Java luôn sao chép; sửa được *nội dung* object qua reference, không đổi được *biến* của người gọi |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** In ra gì? Giải thích.
```java
Integer a = 127, b = 127, c = 128, d = 128;
System.out.println(a == b);
System.out.println(c == d);
System.out.println(c.equals(d));
```

**Câu 2.** Đoạn code compile được không? Nếu không, sửa thế nào?
```java
void method(boolean cond) {
    int x;
    if (cond) x = 10;
    System.out.println(x);
}
```

**Câu 3.** Kết quả từng dòng?
```java
System.out.println(10 / 3);
System.out.println(10 / 3.0);
System.out.println(-10 % 3);
System.out.println((double)(10 / 3));
System.out.println(1.0 / 0);
System.out.println(0.0 / 0.0);
```

**Câu 4.** Lỗi gì (nếu có) và cách khắc phục?
```java
int[] scores = new int[3];
scores[3] = 100;
```

**Câu 5.** Kết quả `==` và `equals()` cho từng cặp, giải thích:
```java
String s1 = "backend";
String s2 = "backend";
String s3 = new String("backend");
String s4 = s3.intern();
String s5 = "back" + "end";
String pre = "back";
String s6 = pre + "end";
```

**Câu 6.** In ra gì? Vì sao?
```java
int a = 100_000;
int b = 100_000;
long c = a * b;
long d = (long) a * b;
System.out.println(c);
System.out.println(d);
```

**Câu 7.** Kết quả và lý do:
```java
byte b = 10;
b += 5;
System.out.println(b);
byte c = 10;
// c = c + 5;   // dòng này có compile không?
System.out.println((byte) 130);
```

**Câu 8.** In ra gì?
```java
int i = 1;
int r = i++ + i++ + ++i;
System.out.println(r);
System.out.println(i);
```

**Câu 9.** Đúng/sai và giải thích:
```java
double x = 0.1 + 0.2;
System.out.println(x == 0.3);
System.out.println(Double.compare(x, 0.3) == 0);
System.out.println(Math.abs(x - 0.3) < 1e-9);
System.out.println(Double.NaN == Double.NaN);
```

**Câu 10.** Điều gì xảy ra lúc compile / runtime?
```java
Object[] arr = new String[2];
arr[0] = "ok";
arr[1] = Integer.valueOf(1);
```

**Câu 11.** Sau đoạn code, giá trị `v`, `xs[0]`, `name` là gì?
```java
static void addOne(int n)     { n++; }
static void fill(int[] a)     { a[0] = 99; }
static void grow(String t)    { t = t + "!"; }
// ...
int v = 5;            addOne(v);
int[] xs = {1, 2, 3}; fill(xs);
String name = "Pho";  grow(name);
```

**Câu 12.** `"😀".length()` trả về mấy? Còn `"😀".codePointCount(0, "😀".length())`?

---

### Phần B — Bài tập viết code

**Bài 1 — Tính tiền điện bậc thang.**
Nhận `double kWh`, tính tiền: 50 kWh đầu giá 1.678 đ/kWh, từ kWh thứ 51 giá 2.014 đ/kWh. Chạy thử với dữ liệu cộng dồn nhiều hộ và **chỉ ra bằng thực nghiệm** sai số khi dùng `double` (so với kết quả kỳ vọng). Ghi chú: giải pháp triệt để là `BigDecimal` / lưu theo *đồng nguyên* — học ở module sau.

**Bài 2 — Đảo chuỗi không dùng `reverse()`.**
`String reverseString(String input)` — tự viết bằng vòng lặp + `toCharArray()` (hoán đổi hai đầu) hoặc `StringBuilder.append`. **Xử lý đúng** trường hợp chuỗi chứa emoji (gợi ý: đảo theo *code point*, không theo `char`, nếu không muốn làm hỏng surrogate pair).

**Bài 3 — Kiểm tra Palindrome.**
`boolean isPalindrome(String s)` — bỏ qua khoảng trắng, dấu câu, không phân biệt hoa/thường; dùng `Locale.ROOT` khi hạ chữ thường. Ví dụ `"A man, a plan, a canal: Panama"` → `true`. Viết bằng hai con trỏ (two-pointer), không tạo chuỗi đảo ngược.

**Bài 4 — So hiệu năng nối chuỗi.**
Viết 3 method nối các số `0..99_999` thành một chuỗi: `withPlus()` (dùng `+`), `withBuilder()` (dùng `StringBuilder`), `withBuilderPresized()` (`new StringBuilder(600_000)`). Đo bằng `System.nanoTime()`, chạy warm-up vài lần trước khi đo. Giải thích chênh lệch dựa trên độ phức tạp O(n²) vs O(n) và chi phí resize buffer.

**Bài 5 — Ma trận điểm (jagged-aware).**
`int[][] scores` cho 5 sinh viên, mỗi người **có thể có số môn khác nhau** (jagged array). Tính điểm trung bình mỗi sinh viên, tìm người có trung bình cao nhất, và xử lý an toàn hàng `null` hoặc rỗng (không chia cho 0).

**Bài 6 — Chuẩn hóa username.**
`String normalizeUsername(String raw)` theo thứ tự: `strip()` → hạ chữ thường (`Locale.ROOT`) → thay chuỗi khoảng trắng ở giữa bằng một `_` (`replaceAll("\\s+", "_")`). Trả `""` nếu `raw` là `null` hoặc rỗng sau khi strip. Ví dụ `"  Pho   Huynh  "` → `"pho_huynh"`.

**Bài 7 — Cộng an toàn không tràn.**
`int safeSum(int[] nums)` trả tổng, nhưng **ném `ArithmeticException`** nếu tổng vượt phạm vi `int` (dùng `Math.addExact`). Viết thêm `long sum(int[] nums)` cộng dồn vào `long` để so sánh hành vi.

**Bài 8 — Parse cấu hình.**
`int parsePort(String s)` nhận chuỗi như `"8080"`, `"0x1F90"`, `"0b1111110010000"` và trả về số cổng. Ném `IllegalArgumentException` với thông báo rõ ràng nếu định dạng sai hoặc ngoài khoảng `1..65535`. (Gợi ý: `Integer.parseInt(s, radix)` sau khi tách tiền tố.)

**Bài 9 — Bitmask quyền.**
Cho các cờ `READ = 1<<0`, `WRITE = 1<<1`, `EXECUTE = 1<<2`. Viết `int grant(int perms, int flag)`, `int revoke(int perms, int flag)`, `boolean has(int perms, int flag)`, và `String describe(int perms)` trả về ví dụ `"rw-"`.

---

### Phần C — Bài tập nâng cao (tư duy JVM / spec)

**Bài 10.** Giải thích bằng lời tại sao đoạn sau có thể gây `OutOfMemoryError` với số vòng lặp cực lớn, còn bản `StringBuilder` thì không. Liên hệ: số object String trung gian, heap vs String Pool, áp lực lên Garbage Collector.
```java
String log = "";
for (int i = 0; i < 5_000_000; i++) log += "Line " + i + "\n";
```

**Bài 11.** Cho biết `a == b` in ra gì trong từng trường hợp và vì sao:
```java
String a = "hello";
String b1 = "hel" + "lo";
final String p = "hel";  String b2 = p + "lo";
String q = "hel";        String b3 = q + "lo";
String b4 = ("hel" + "lo").intern();
String b5 = new String("hello");
```

**Bài 12.** `Math.abs(Integer.MIN_VALUE)` trả về gì? Giải thích bằng biểu diễn bù 2. Viết một hàm `absExact(int)` xử lý đúng bằng cách ném ngoại lệ ở trường hợp biên.

**Bài 13.** Vì sao `(int) 1e20 == Integer.MAX_VALUE` nhưng `(long) 1e20` lại là một số âm? Trình bày quy tắc ép `double → int` và `double → long` khác nhau ở đâu (gợi ý: `1e20` vẫn vừa `double` nhưng vượt cả `long`).

**Bài 14.** Đoạn sau in ra gì và tại sao nó **không** phải `"12"`?
```java
System.out.println('1' + '2');
System.out.println("" + '1' + '2');
System.out.println('1' + 2);
```

**Bài 15.** Giải thích `strictfp` (và vì sao từ Java 17 nó thành mặc định) — trả lời ngắn gọn: nó ràng buộc điều gì và giải quyết vấn đề gì về tính *tái lập* (reproducibility) của phép toán số thực giữa các nền tảng CPU.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Phần A</summary>

1. `a == b` → `true` (127 trong Integer Cache); `c == d` → `false` (128 ngoài cache); `c.equals(d)` → `true` (so sánh giá trị).
2. **Không compile** — `x` có thể chưa gán khi `cond == false` (`variable x might not have been initialized`). Sửa: khởi tạo `int x = 0;` hoặc thêm nhánh `else x = ...;`.
3. `3` · `3.3333333333333335` · `-1` (dấu theo số bị chia) · `3.0` (`10/3` tính ở int trước) · `Infinity` · `NaN`.
4. `ArrayIndexOutOfBoundsException` (index hợp lệ 0–2). Sửa: `new int[4]` hoặc dùng index ≤ 2.
5. `s1==s2` true (Pool). `s1==s3` false (`new` → ngoài Pool). `s1==s4` true (`intern` trả reference Pool). `s1==s5` true (`"back"+"end"` gộp lúc biên dịch). `s1==s6` **false** (`pre` là biến, nối lúc runtime → object mới). Mọi `equals()` đều true.
6. `c` → `-727379968` (a*b tràn ở `int` rồi mới gán `long`); `d` → `10000000000` (ép một toán hạng sang `long` trước khi nhân).
7. `b` → `15` (`b += 5` ⇔ `b = (byte)(b + 5)`). `c = c + 5;` **không compile** (`int` không tự thu hẹp về `byte`). `(byte) 130` → `-126` (130 − 256).
8. `i++` cho 1, `i++` cho 2, `++i` cho 4 → `r = 7`; sau đó `i = 4`.
9. `x == 0.3` → `false` (sai số IEEE 754). `Double.compare(x, 0.3) == 0` → `false` (khác bit). `Math.abs(x - 0.3) < 1e-9` → `true`. `Double.NaN == Double.NaN` → `false`.
10. Compile **được** (mảng covariant: `String[]` là `Object[]`). Runtime: dòng `arr[1] = Integer...` ném `ArrayStoreException` vì kiểu thực là `String[]`.
11. `v == 5` (primitive sao chép). `xs[0] == 99` (sửa nội dung qua reference). `name` vẫn `"Pho"` (String bất biến, `t` chỉ là bản sao reference).
12. `.length()` → `2` (emoji ngoài BMP = surrogate pair, 2 char). `.codePointCount(...)` → `1`.

</details>

<details>
<summary>Phần B — ý chính</summary>

- **Bài 1:** Công thức: `kWh <= 50 ? kWh*1678 : 50*1678 + (kWh-50)*2014`. Sai số `double` lộ ra khi cộng dồn nhiều kết quả có phần lẻ nhị phân không biểu diễn được — in ra `System.out.printf("%.10f")` sẽ thấy đuôi `...0001`/`...9999`.
- **Bài 2:** Cách an toàn nhất: `new StringBuilder(input).reverse()` bị cấm → dùng `input.codePoints()` gom vào list rồi ghép ngược bằng `appendCodePoint`. Nếu chỉ đảo `char` thì emoji vỡ.
- **Bài 3:** Hai con trỏ `l=0, r=len-1`; bỏ qua ký tự không phải chữ/số bằng `Character.isLetterOrDigit`; so sánh `Character.toLowerCase`.
- **Bài 4:** `withPlus` chậm gấp hàng chục–trăm lần: mỗi `+=` cấp phát String mới + copy toàn bộ nội dung cũ → O(n²). `withBuilder` O(n) nhưng vẫn có vài lần copy khi buffer nhân đôi. `withBuilderPresized` nhanh nhất — không lần resize nào. Nhớ warm-up để JIT biên dịch trước khi đo.
- **Bài 5:** Bỏ qua `row == null || row.length == 0`; `avg = sum / (double) row.length`.
- **Bài 6:** `if (raw == null) return ""; String t = raw.strip(); return t.isEmpty() ? "" : t.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");`
- **Bài 7:** `int acc = 0; for (int n : nums) acc = Math.addExact(acc, n);` — ném `ArithmeticException` khi tràn; bản `long` cộng bình thường tới khi vượt `long`.
- **Bài 8:** Tách tiền tố `0x`/`0b` → `Integer.parseInt(body, 16|2)`; bắt `NumberFormatException`; kiểm tra `1..65535`.
- **Bài 9:** `grant = perms | flag`; `revoke = perms & ~flag`; `has = (perms & flag) != 0`.

</details>

<details>
<summary>Phần C</summary>

- **Bài 10:** Mỗi `log += ...` tạo một String hoàn toàn mới trên heap (không vào Pool vì nối động); String cũ (ngày càng dài) lập tức thành rác. Với 5 triệu vòng, tốc độ sinh rác vượt tốc độ GC dọn, cộng với việc mỗi lần phải copy chuỗi cũ ngày càng lớn → heap đầy → `OutOfMemoryError` (hoặc GC overhead limit exceeded). `StringBuilder` dùng một `char[]/byte[]` tự resize (nhân đôi), không tạo object mỗi vòng → O(n) thời gian, bộ nhớ tỉ lệ tuyến tính.
- **Bài 11:** `b1` true (gộp biên dịch). `b2` true (`p` là compile-time constant). `b3` **false** (`q` là biến → nối runtime). `b4` true (`intern` trả reference Pool, mà nội dung `"hello"` literal đã có sẵn). `b5` false (`new`).
- **Bài 12:** Trả về `Integer.MIN_VALUE` (âm). `MIN_VALUE = -2^31`; `-(-2^31) = 2^31` không tồn tại trong `int` → tràn cuộn về chính `-2^31`. `absExact`: `if (x == Integer.MIN_VALUE) throw new ArithmeticException("overflow"); return Math.abs(x);` (hoặc `Math.absExact` từ Java 15).
- **Bài 13:** `double → int`: nếu giá trị > `Integer.MAX_VALUE` thì kết quả **kẹp** về `Integer.MAX_VALUE`. `double → long`: cũng có luật kẹp về `Long.MAX_VALUE`... nhưng `1e20 > Long.MAX_VALUE (~9.22e18)` nên kẹp về `Long.MAX_VALUE = 9223372036854775807` (dương, không âm). *Lưu ý:* nếu đề quan sát thấy số âm là do nhầm với ép qua `int` trung gian hoặc phép nhân tràn — trình bày rõ luật kẹp của JLS §5.1.3: kết quả là `MIN`/`MAX` của kiểu đích, `NaN → 0`. (Điểm mấu chốt cần nêu: hai phép ép có *cùng* luật kẹp nhưng *ngưỡng* khác nhau vì phạm vi `int` và `long` khác nhau.)
- **Bài 14:** `'1' + '2'` = `49 + 50` = `99` (int — hai char nâng lên int). `"" + '1' + '2'` = `"12"` (có String → nối). `'1' + 2` = `51` (int). Không có String thì `+` giữa ký tự/số luôn là số học.
- **Bài 15:** `strictfp` buộc mọi phép toán trung gian `float`/`double` dùng đúng độ rộng IEEE 754 (32/64 bit), cấm CPU dùng thanh ghi mở rộng 80-bit (x87) cho kết quả trung gian → cùng một biểu thức cho **kết quả bit-for-bit giống nhau trên mọi nền tảng**. Từ Java 17 (JEP 306) hành vi này thành mặc định vì phần cứng hiện đại (SSE2) đã làm số thực nghiêm ngặt không tốn chi phí, nên từ khóa `strictfp` trở nên thừa.

</details>

---

*File tiếp theo trong lộ trình: **Module 01.2 — Cấu trúc điều khiển** (if/else, switch expression, vòng lặp, đệ quy).*
