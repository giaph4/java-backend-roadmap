## Bài 7 — Fibonacci: 3 phiên bản

### Đề
Viết `fibNaive`, `fibMemo`, `fibLoop`. Đo thời gian `fib(45)` bằng `System.nanoTime()` (có warm-up). Giải thích chênh lệch O(2ⁿ) vs O(n).

### Phân tích

Định nghĩa: `fib(0)=0`, `fib(1)=1`, `fib(n)=fib(n-1)+fib(n-2)`.

| Phiên bản | Cách làm | Thời gian | Bộ nhớ |
|---|---|---|---|
| `fibNaive` | đệ quy thẳng theo định nghĩa | **O(2ⁿ)** | O(n) stack |
| `fibMemo` | đệ quy + cache kết quả đã tính | O(n) | O(n) cache + O(n) stack |
| `fibLoop` | lặp bottom-up, chỉ giữ 2 giá trị gần nhất | O(n) | **O(1)** |

**Vì sao `fibNaive` là O(2ⁿ):** `fib(45)` gọi `fib(44)` và `fib(43)`; `fib(44)` lại gọi `fib(43)` và `fib(42)`... Cùng một giá trị bị tính lại **rất nhiều lần**. Cây lời gọi phình theo cấp số nhân — `fib(45)` tốn khoảng 1,1 tỷ lời gọi.

**Vì sao warm-up:** JVM chạy bytecode qua trình thông dịch lúc đầu, chỉ sau vài nghìn lần lặp mới biên dịch JIT sang mã máy tối ưu. Đo ngay lần chạy đầu → dính thời gian "khởi động", số liệu sai lệch. Chạy "nóng máy" vài vòng trước khi đo.

### Lời giải

```java
package baitap.bai7;

import java.util.HashMap;
import java.util.Map;

public class Main {

    // (a) Đệ quy thô — O(2^n)
    public static long fibNaive(int n) {
        if (n <= 1) return n;                       // base: fib(0)=0, fib(1)=1
        return fibNaive(n - 1) + fibNaive(n - 2);   // tính lại vô số lần
    }

    // (b) Memoization (top-down) — O(n)
    public static long fibMemo(int n, Map<Integer, Long> cache) {
        if (n <= 1) return n;
        Long hit = cache.get(n);
        if (hit != null) return hit;               // đã tính rồi -> lấy ngay
        long v = fibMemo(n - 1, cache) + fibMemo(n - 2, cache);
        cache.put(n, v);                           // lưu lại: mỗi n tính đúng 1 lần
        return v;
    }

    // (c) Bottom-up (khử đệ quy) — O(n) thời gian, O(1) bộ nhớ
    public static long fibLoop(int n) {
        if (n <= 1) return n;
        long a = 0, b = 1;                         // a = fib(i-2), b = fib(i-1)
        for (int i = 2; i <= n; i++) {
            long t = a + b;                        // fib(i)
            a = b;
            b = t;
        }
        return b;
    }

    public static void main(String[] args) {
        int n = 45;

        // ---- Warm-up: cho JIT biên dịch trước khi đo ----
        for (int i = 0; i < 5; i++) {
            fibNaive(30);
            fibMemo(90, new HashMap<>());
            fibLoop(90);
        }

        long t0 = System.nanoTime();
        long r1 = fibNaive(n);
        long t1 = System.nanoTime();
        long r2 = fibMemo(n, new HashMap<>());
        long t2 = System.nanoTime();
        long r3 = fibLoop(n);
        long t3 = System.nanoTime();

        System.out.printf("fibNaive(%d) = %d  |  %,d ns%n", n, r1, t1 - t0);
        System.out.printf("fibMemo (%d) = %d  |  %,d ns%n", n, r2, t2 - t1);
        System.out.printf("fibLoop (%d) = %d  |  %,d ns%n", n, r3, t3 - t2);
    }
}
```

**Kết quả tiêu biểu (số cụ thể tùy máy):**
```
fibNaive(45) = 1134903170  |  3.200.000.000 ns   (~3,2 giây)
fibMemo (45) = 1134903170  |  8.000 ns           (~8 micro giây)
fibLoop (45) = 1134903170  |  700 ns
```

### Giải thích chênh lệch

- `fibNaive`: số lời gọi ≈ `fib(n)` ≈ `1,618ⁿ`. Từ `n=40` trở đi mỗi tăng 1 đơn vị làm thời gian **gần gấp rưỡi**. `fib(50)` đã mất cả phút.
- `fibMemo`: mỗi giá trị `fib(2)..fib(n)` tính **đúng một lần** rồi nằm trong cache → `n-1` phép cộng. Nhanh hơn hàng trăm nghìn lần. Cái giá: `HashMap` + độ sâu đệ quy `n` (với `n` cỡ vài chục nghìn có thể tràn stack → khi đó phải dùng `fibLoop`).
- `fibLoop`: cùng `n` phép cộng như memo nhưng **không** đệ quy, **không** cache, chỉ 3 biến `long`. Nhanh nhất, an toàn nhất → lựa chọn mặc định trong sản phẩm thật.

> Lưu ý kiểu: dùng `long`. `fib(46)` = 1.836.311.903 vẫn lọt `int`, nhưng `fib(47)` tràn `int`. `long` an toàn tới `fib(92)`.

---

## Bài 8 — Đệ quy: duyệt cây thư mục

### Đề
```java
class Node { String name; boolean isFile; List<Node> children; }
```
Viết `void printTree(Node node, int depth)` in cây với thụt lề `"  ".repeat(depth)`. Viết thêm bản **khử đệ quy** bằng `ArrayDeque`, kiểm chứng hai bản cho cùng kết quả.

### Phân tích

Cây thư mục là cấu trúc **đệ quy tự nhiên**: một thư mục chứa các Node con, mỗi Node con lại có thể là thư mục chứa Node con nữa — không biết trước độ sâu. Đây đúng là trường hợp "ưu tiên đệ quy" trong bảng §9.

- **Bản đệ quy:** in node hiện tại (thụt lề theo `depth`), rồi gọi `printTree(child, depth + 1)` cho từng con. Base case *ngầm*: node là file (hoặc không có con) → không có lời gọi đệ quy nào nữa.
- **Bản khử đệ quy:** mô phỏng call stack bằng `ArrayDeque` dùng như stack (LIFO). Đẩy `root` vào; lặp: `pop` một node, in nó, rồi đẩy các con vào. **Mẹo:** đẩy con theo thứ tự **ngược** để khi `pop` ra lại đúng thứ tự trái→phải.

Muốn hai bản in giống hệt nhau thì cả hai phải là **pre-order, con trái trước** (in cha trước con, duyệt con theo thứ tự danh sách).

### Lời giải

```java
package baitap.bai8;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Main {

    static class Node {
        String name;
        boolean isFile;
        List<Node> children = new ArrayList<>();

        Node(String name, boolean isFile) {
            this.name = name;
            this.isFile = isFile;
        }
        Node add(Node child) { children.add(child); return this; }
    }

    // ---- Bản đệ quy ----
    static void printTree(Node node, int depth, StringBuilder out) {
        out.append("  ".repeat(depth)).append(node.name).append('\n');
        for (Node child : node.children) {          // file -> children rỗng -> dừng đệ quy
            printTree(child, depth + 1, out);
        }
    }

    // ---- Bản khử đệ quy bằng ArrayDeque (dùng như stack) ----
    static void printTreeIterative(Node root, StringBuilder out) {
        Deque<Node> stackNode  = new ArrayDeque<>();
        Deque<Integer> stackDep = new ArrayDeque<>();   // stack song song lưu depth
        stackNode.push(root);
        stackDep.push(0);

        while (!stackNode.isEmpty()) {
            Node cur = stackNode.pop();
            int depth = stackDep.pop();
            out.append("  ".repeat(depth)).append(cur.name).append('\n');

            // đẩy NGƯỢC để pop ra đúng thứ tự trái -> phải
            for (int i = cur.children.size() - 1; i >= 0; i--) {
                stackNode.push(cur.children.get(i));
                stackDep.push(depth + 1);
            }
        }
    }

    public static void main(String[] args) {
        Node root = new Node("project", false)
            .add(new Node("src", false)
                .add(new Node("Main.java", true))
                .add(new Node("util", false)
                    .add(new Node("Helper.java", true))))
            .add(new Node("README.md", true))
            .add(new Node("target", false)
                .add(new Node("app.jar", true)));

        StringBuilder a = new StringBuilder();
        StringBuilder b = new StringBuilder();
        printTree(root, 0, a);
        printTreeIterative(root, b);

        System.out.print(a);
        System.out.println("---- hai ban giong nhau? " + a.toString().equals(b.toString()));
    }
}
```

**Kết quả chạy:**
```
project
  src
    Main.java
    util
      Helper.java
  README.md
  target
    app.jar
---- hai ban giong nhau? true
```

### Giải thích chi tiết

**Bản đệ quy:**
- Mỗi lời gọi in một node rồi lặp qua các con — độ sâu call stack = độ sâu cây.
- Không cần `if (node.isFile) return;` tường minh: file có `children` rỗng nên vòng `for` không chạy → tự dừng. (Vẫn nên có nếu muốn diễn đạt base case rõ ràng.)
- Truyền `StringBuilder out` để gom kết quả — nhờ đó so sánh được hai bản thay vì in thẳng ra màn hình.

**Bản khử đệ quy:**
- `ArrayDeque` với `push`/`pop` hoạt động như stack. Thứ tự xử lý mô phỏng đúng đệ quy pre-order.
- **Vì sao đẩy ngược:** stack là LIFO. Nếu đẩy `[c0, c1, c2]` theo thứ tự thuận thì `pop` ra `c2` trước → sai. Đẩy `c2, c1, c0` thì `pop` ra `c0` trước → đúng.
- Cần **stack thứ hai lưu `depth`** vì `Node` không tự biết mình sâu bao nhiêu. Trong bản đệ quy, `depth` là tham số nằm sẵn trên mỗi stack frame; khử đệ quy thì ta phải tự quản lý.
- Ưu điểm: độ sâu cây 100.000 mức vẫn chạy tốt (dữ liệu nằm trên heap, không phải call stack) → không `StackOverflowError`.

---

## Bài 9 — Chuyển đệ quy đuôi thành vòng lặp

### Đề
```java
int sum(int[] a, int i, int acc) {
    if (i == a.length) return acc;
    return sum(a, i + 1, acc + a[i]);
}
```
Giải thích vì sao `sum(bigArray, 0, 0)` có thể `StackOverflowError` **dù là đệ quy đuôi**, rồi viết lại bằng `for`.

### Giải thích vì sao vẫn tràn stack

**Đệ quy đuôi (tail recursion)** = lời gọi đệ quy là thao tác **cuối cùng**, không còn phép tính nào sau khi nó trả về. Ở đây `return sum(a, i+1, acc+a[i]);` — sau lời gọi không nhân, không cộng gì thêm → đúng là đệ quy đuôi.

Một số ngôn ngữ (Scala, Kotlin với `tailrec`) phát hiện dạng này và **tái sử dụng cùng một stack frame** thay vì tạo frame mới — biến đệ quy thành vòng lặp ngầm, O(1) stack. Đó gọi là **Tail Call Optimization (TCO)**.

**JVM/Java KHÔNG làm TCO.** Lý do chính: JVM giữ nguyên toàn bộ frame trên stack để phục vụ `StackWalker`, stack trace trong exception, và mô hình bảo mật dựa trên caller. Vì vậy:

- Mỗi lời gọi `sum(...)` vẫn cấp **một stack frame mới** (lưu `a`, `i`, `acc`, địa chỉ trả về).
- Với mảng 1 triệu phần tử → 1 triệu frame chồng lên nhau.
- Vượt dung lượng stack của thread (mặc định ~512 KB, chứa được ~10.000–20.000 frame) → **`StackOverflowError`**.

Kết luận: "đệ quy đuôi" chỉ là *tiềm năng* được tối ưu; trên Java nó **không** giúp gì — vẫn phải tự viết vòng lặp.

### Lời giải

```java
package baitap.bai9;

public class Main {

    // Bản đệ quy đuôi gốc — TRÀN STACK với mảng lớn trên Java
    static long sumRecursive(int[] a, int i, long acc) {
        if (i == a.length) return acc;
        return sumRecursive(a, i + 1, acc + a[i]);
    }

    // Bản vòng lặp — tương đương ngữ nghĩa, O(1) stack
    static long sumLoop(int[] a) {
        long acc = 0;                    // <- chính là tham số "acc", giờ là biến local
        for (int i = 0; i < a.length; i++) {   // <- "i" và điều kiện "i == a.length"
            acc = acc + a[i];            // <- bước "acc + a[i]" truyền cho lời gọi kế
        }
        return acc;                      // <- "return acc" ở base case
    }

    public static void main(String[] args) {
        int[] big = new int[2_000_000];
        for (int i = 0; i < big.length; i++) big[i] = 1;

        System.out.println(sumLoop(big));   // 2000000, chạy tức thì

        try {
            System.out.println(sumRecursive(big, 0, 0));
        } catch (StackOverflowError e) {
            System.out.println("sumRecursive -> StackOverflowError (Java khong co TCO)");
        }
    }
}
```

**Kết quả chạy:**
```
2000000
sumRecursive -> StackOverflowError (Java khong co TCO)
```

### Ánh xạ đệ quy đuôi → vòng lặp

Đệ quy đuôi luôn chuyển thành vòng lặp theo công thức máy móc:

| Thành phần đệ quy | Thành phần vòng lặp |
|---|---|
| Tham số tích luỹ `acc` | Biến local `acc`, khởi tạo bằng giá trị ban đầu (`0`) |
| Tham số con trỏ `i` | Biến đếm `for` |
| Điều kiện base case `i == a.length` | Điều kiện dừng vòng `for` (đảo lại: `i < a.length`) |
| Biểu thức trong lời gọi đệ quy `acc + a[i]` | Câu lệnh cập nhật `acc` trong thân vòng |
| `return acc` ở base case | `return acc` sau vòng lặp |

Dùng `long` cho `acc` để tránh tràn số khi cộng nhiều phần tử `int`.

---

## Bài 10 — FizzBuzz "có kiến trúc"

### Đề
In 1..100: bội 3 → `Fizz`, bội 5 → `Buzz`, bội cả hai → `FizzBuzz`, còn lại → in số. Làm 2 cách, so sánh khả năng đọc:
- (a) chuỗi `if / else if`
- (b) `switch expression` trên `(n % 3 == 0 ? 1 : 0) + (n % 5 == 0 ? 2 : 0)`

### Phân tích cách (b)

Biểu thức mã hoá trạng thái chia hết thành một số 0–3:

| `n % 3 == 0` | `n % 5 == 0` | `(…?1:0)` | `(…?2:0)` | Tổng | Nghĩa |
|---|---|---|---|---|---|
| sai | sai | 0 | 0 | **0** | in số |
| đúng | sai | 1 | 0 | **1** | Fizz |
| sai | đúng | 0 | 2 | **2** | Buzz |
| đúng | đúng | 1 | 2 | **3** | FizzBuzz |

Đây là kỹ thuật **bitmask nhỏ**: mỗi điều kiện chiếm một "bit trọng số" (1 và 2) nên tổng không bao giờ trùng nghĩa → `switch` 4 nhánh phủ kín.

### Lời giải

```java
package baitap.bai10;

public class Main {

    // (a) Chuỗi if / else if — điều kiện HẸP NHẤT đặt trước
    static String fizzBuzzIf(int n) {
        if (n % 15 == 0) return "FizzBuzz";   // bội cả 3 và 5  -> phải xét TRƯỚC
        if (n % 3 == 0)  return "Fizz";
        if (n % 5 == 0)  return "Buzz";
        return Integer.toString(n);
    }

    // (b) switch expression trên mã trạng thái 0..3
    static String fizzBuzzSwitch(int n) {
        int code = (n % 3 == 0 ? 1 : 0) + (n % 5 == 0 ? 2 : 0);
        return switch (code) {
            case 1 -> "Fizz";
            case 2 -> "Buzz";
            case 3 -> "FizzBuzz";
            default -> Integer.toString(n);   // case 0
        };
    }

    public static void main(String[] args) {
        for (int n = 1; n <= 100; n++) {
            String r1 = fizzBuzzIf(n);
            String r2 = fizzBuzzSwitch(n);
            if (!r1.equals(r2)) throw new AssertionError("Lech tai n=" + n);
            System.out.println(r1);
        }
    }
}
```

**Kết quả (trích):**
```
1
2
Fizz
4
Buzz
Fizz
7
8
Fizz
Buzz
11
Fizz
13
14
FizzBuzz
...
```

### So sánh khả năng đọc

**Cách (a) — `if / else if`:**
- Ưu: đọc là hiểu ngay, không cần "giải mã". Ai cũng theo được.
- Nhược: **thứ tự sống còn** — phải đặt `n % 15 == 0` (hoặc `n%3==0 && n%5==0`) lên **đầu**. Nếu để `n % 3 == 0` trước, số 15 sẽ khớp `Fizz` và không bao giờ ra `FizzBuzz`. Đây là bẫy kinh điển của chuỗi `if/else if`: điều kiện rộng che điều kiện hẹp.

**Cách (b) — `switch` trên mã trạng thái:**
- Ưu: mỗi tổ hợp có đúng một `case`, **không phụ thuộc thứ tự** các nhánh (trừ `default`). `switch` bắt buộc bao phủ hết → khó sót. Mở rộng tốt: thêm "bội 7 → Bazz" chỉ cần thêm trọng số `4` và vài `case`.
- Nhược: người đọc phải dừng lại hiểu `(…?1:0) + (…?2:0)` mã hoá cái gì — một tầng gián tiếp. Với chỉ 2 điều kiện thì (a) vẫn dễ đọc hơn.

**Kết luận:** Với 2 điều kiện, `if/else if` (đặt case "cả hai" trước) **dễ đọc hơn** và đủ tốt. Cách `switch`-bitmask **thắng khi số điều kiện tăng** (3–4 điều kiện trở lên): số tổ hợp `if` bùng nổ và dễ sai thứ tự, còn bảng trọng số thì cộng dồn gọn gàng và `switch` đảm bảo phủ kín.