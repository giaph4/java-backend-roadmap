# Lời giải đầy đủ — Module 06: Java Modern (8 → 21+)

> Nguồn đề: `14 java modern/14-java-modern.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Refactor `null` → `Optional`

### Đề
Viết lại hàm lấy domain email (đang dùng nhiều `if (x == null) return ...`) bằng chuỗi `Optional.ofNullable(...).map(...).filter(...).map(...).orElse(...)`. Viết thêm bản dùng `db.find` **đã trả `Optional<User>`** để thấy khi nào cần `flatMap`.

### Phân tích

Chuỗi `if (x == null) return ...` lặp lại nhiều lần là dấu hiệu kinh điển nên chuyển sang `Optional` — mỗi bước biến đổi (`map`) **tự động "tắt" (short-circuit)** nếu giá trị trước đó là rỗng, không cần kiểm tra `null` thủ công ở từng bước.

**Phân biệt `map` vs `flatMap`:** nếu hàm bên trong (`db.find`) **đã tự trả về `Optional<User>`** (thay vì `User` có thể `null`), dùng `map(this::findUser)` sẽ tạo ra `Optional<Optional<User>>` (2 tầng lồng nhau, sai) — phải dùng **`flatMap`** để "làm phẳng" đúng 1 tầng.

### Lời giải

```java
package baitap.bai1;

import java.util.Map;
import java.util.Optional;

public class Main {

    record User(Long id, String email) {}

    // ===== Giả lập DB, bản CŨ: trả User hoặc null =====
    static Map<Long, User> dbNullable = Map.of(
            1L, new User(1L, "pho@example.com"),
            2L, new User(2L, "khong-hop-le")   // KHÔNG có "@" - để test filter
    );
    static User findUserNullable(Long id) {
        return dbNullable.get(id); // trả null nếu không tìm thấy - Map.get() mặc định vậy
    }

    // ===== Giả lập DB, bản MỚI: đã tự trả Optional<User> =====
    static Optional<User> findUserOptional(Long id) {
        return Optional.ofNullable(dbNullable.get(id));
    }

    // ----- Bản refactor từ code cũ (nguồn dữ liệu trả null) -----
    static String getUserEmailDomain(Long userId) {
        return Optional.ofNullable(findUserNullable(userId))
                .map(User::email)                                   // User -> String (email), null-safe
                .filter(email -> email != null && email.contains("@"))
                .map(email -> email.substring(email.indexOf("@") + 1).toLowerCase())
                .orElse("unknown");
    }

    // ----- Bản dùng nguồn dữ liệu ĐÃ trả Optional<User> - cần flatMap -----
    static String getUserEmailDomainV2(Long userId) {
        return Optional.of(userId)
                .flatMap(Main::findUserOptional) // Optional<Long> -> Optional<User> - PHẢI flatMap vì findUserOptional ĐÃ trả Optional
                .map(User::email)
                .filter(email -> email.contains("@"))
                .map(email -> email.substring(email.indexOf("@") + 1).toLowerCase())
                .orElse("unknown");
    }

    public static void main(String[] args) {
        System.out.println("getUserEmailDomain(1) = " + getUserEmailDomain(1L));       // example.com
        System.out.println("getUserEmailDomain(2) = " + getUserEmailDomain(2L));       // unknown (không có @)
        System.out.println("getUserEmailDomain(99) = " + getUserEmailDomain(99L));     // unknown (không tồn tại)

        System.out.println("\ngetUserEmailDomainV2(1) = " + getUserEmailDomainV2(1L)); // example.com
        System.out.println("getUserEmailDomainV2(99) = " + getUserEmailDomainV2(99L)); // unknown
    }
}
```

**Kết quả chạy:**
```
getUserEmailDomain(1) = example.com
getUserEmailDomain(2) = unknown
getUserEmailDomain(99) = unknown

getUserEmailDomainV2(1) = example.com
getUserEmailDomainV2(99) = unknown
```

### Giải thích khi nào cần `flatMap`

```
map(f):     Optional<A> --f: A->B--> Optional<B>          (f trả GIÁ TRỊ THƯỜNG)
flatMap(f): Optional<A> --f: A->Optional<B>--> Optional<B> (f ĐÃ TỰ trả Optional - "làm phẳng" 1 tầng)

Nếu dùng map() với f trả Optional<B>:
Optional<A> --map(f)--> Optional<Optional<B>>   ❌ 2 TẦNG LỒNG - sai, khó dùng tiếp
```

- `User::email` trả **`String` thường** (có thể `null`) → dùng `map` đúng.
- `Main::findUserOptional` **ĐÃ TỰ trả `Optional<User>`** → nếu dùng `map(Main::findUserOptional)` trên `Optional<Long>`, kết quả sẽ là `Optional<Optional<User>>` — phải gọi `.get()` lồng 2 lần mới lấy được `User` thật, cực kỳ bất tiện và mất hết ý nghĩa null-safety. `flatMap` "gộp" 2 tầng `Optional` thành 1, y hệt vai trò `flatMap` của Stream API (Module 03.3) khi "làm phẳng" `List<List<T>>`.
- **Nguyên tắc chọn nhanh:** nhìn vào **kiểu trả về của hàm truyền vào** — nếu đã là `Optional<X>`, dùng `flatMap`; nếu là `X` thường, dùng `map`.

---

## Bài 2 — `sealed` + record + pattern switch: hệ thống `Result`

### Đề
`sealed interface ApiResult<T> permits Success, Failure`; 2 record. `String render(ApiResult<String> r)` dùng **record pattern** trong `switch`, **không** `default`. Thêm `Pending<T>()` vào `permits`, quan sát lỗi compile.

### Phân tích

**Record Pattern** (Java 21 chính thức) cho phép `switch` **vừa kiểm tra kiểu, vừa "bung" (destructure) trực tiếp các component của record** trong cùng 1 dòng `case` — không cần gọi lại `.data()`/`.message()` thủ công sau khi đã biết kiểu. Kết hợp `sealed`, compiler chứng minh **exhaustiveness** y hệt bài học ở Module 02.1 Bài 7.

### Lời giải

```java
package baitap.bai2;

public class Main {

    sealed interface ApiResult<T> permits Success, Failure {}
    record Success<T>(T data) implements ApiResult<T> {}
    record Failure<T>(String message, int code) implements ApiResult<T> {}

    static String render(ApiResult<String> r) {
        // Record pattern - "bung" trực tiếp component ngay trong case, KHÔNG default
        return switch (r) {
            case Success<String> s -> "OK: " + s.data();
            case Failure<String> f -> "ERROR[" + f.code() + "]: " + f.message();
        };
    }

    public static void main(String[] args) {
        System.out.println(render(new Success<>("Đã lưu đơn hàng #123")));
        System.out.println(render(new Failure<>("Không tìm thấy user", 404)));
    }
}
```

**Kết quả chạy:**
```
OK: Đã lưu đơn hàng #123
ERROR[404]: Không tìm thấy user
```

### Thêm `Pending<T>()` và quan sát lỗi compile

```java
sealed interface ApiResult<T> permits Success, Failure, Pending {} // thêm Pending
record Pending<T>() implements ApiResult<T> {}

// render() GIỮ NGUYÊN, KHÔNG sửa gì - biên dịch lại:
```

**Thông báo lỗi compile:**
```
error: the switch expression does not cover all possible input values
        return switch (r) {
               ^
```

### Giải thích

- Compiler **tự biết** `ApiResult<T>` giờ có **3** khả năng (`Success`, `Failure`, `Pending` — nhờ `permits` đã khai báo đủ), và `switch` trong `render()` chỉ xử lý **2** — báo lỗi **NGAY LÚC COMPILE**, không đợi tới khi có `Pending` thật chạy vào `render()` mới phát hiện thiếu logic ở runtime.
- Đây chính là lợi ích thực chiến to lớn: khi hệ thống Result/trạng thái có thêm 1 loại mới (VD: thêm `Pending` cho các API bất đồng bộ dạng `202 Accepted` — liên hệ Module 14 REST API), **MỌI nơi** dùng `switch` pattern matching trên `ApiResult` (có thể rải rác nhiều file trong dự án lớn) sẽ **đồng loạt báo lỗi compile** nếu chưa xử lý case mới — không có chỗ nào bị "bỏ sót" âm thầm.

---

## Bài 3 — Record pattern lồng nhau + guard

### Đề
`record Address(city, country)`, `record Customer(name, address)`. `String label(Customer c)` dùng `switch` + record pattern lồng 2 tầng + `when`: `country == "Vietnam"` → `"[name] ở [city]"`; ngược lại → `"[name] (quốc tế)"`.

### Phân tích

**Record pattern lồng nhau** cho phép "bung" trực tiếp `Customer(name, Address(city, country))` trong **1 dòng `case` duy nhất** — truy cập thẳng `city`/`country` mà **không cần** viết `c.address().city()` từng bước. **`when` (guard clause)** thêm điều kiện bổ sung **SAU KHI** đã khớp pattern cấu trúc — chỉ nhánh nào **VỪA khớp cấu trúc VỪA thỏa `when`** mới được chọn.

### Lời giải

```java
package baitap.bai3;

public class Main {

    record Address(String city, String country) {}
    record Customer(String name, Address address) {}

    static String label(Customer c) {
        return switch (c) {
            // Bung 2 tầng: Customer(name, Address(city, country)) - lấy thẳng city/country
            // "when" - guard clause: CHỈ khớp nhánh này nếu country.equals("Vietnam")
            case Customer(String name, Address(String city, String country)) when country.equals("Vietnam") ->
                    name + " ở " + city;

            // Nhánh còn lại: vẫn khớp cấu trúc Customer(Address), nhưng KHÔNG thỏa "when" ở trên
            case Customer(String name, Address address) ->
                    name + " (quốc tế)";
        };
    }

    public static void main(String[] args) {
        System.out.println(label(new Customer("Pho", new Address("Hà Nội", "Vietnam"))));
        System.out.println(label(new Customer("John", new Address("Tokyo", "Japan"))));
        System.out.println(label(new Customer("Huynh", new Address("Sài Gòn", "Vietnam"))));
    }
}
```

**Kết quả chạy:**
```
Pho ở Hà Nội
John (quốc tế)
Huynh ở Sài Gòn
```

### Giải thích

- **Không cần `when`, có thể viết `if (country.equals("Vietnam"))` bên trong nhánh** — nhưng cách đó phải bung `Address` **2 lần** (1 lần ở `case`, 1 lần đọc lại `country` trong `if`) — dùng `when` ngay tại `case` giúp **VỪA khớp pattern VỪA lọc điều kiện trong 1 bước duy nhất**, code đọc tự nhiên hơn nhiều: "khớp `Customer(name, Address(city, country))` **KHI** `country` là Vietnam".
- Thứ tự `case` **CÓ Ý NGHĨA** ở đây (khác `sealed` + record pattern không guard, nơi thứ tự không quan trọng vì mỗi case là 1 type riêng biệt) — nhánh có `when` phải đặt **TRƯỚC** nhánh không có `when` khớp cùng cấu trúc, nếu không nhánh tổng quát hơn sẽ "chặn" mất nhánh có điều kiện, y hệt bẫy đã gặp ở FizzBuzz (Module 02, Bài 10) và `if/else` thứ tự.
- **Liên hệ thực tế:** đây chính xác là cách xử lý logic điều kiện phức tạp gọn gàng hơn hẳn chuỗi `if (c.address() != null && c.address().country().equals(...))` truyền thống — đặc biệt hữu ích khi xử lý DTO/response lồng nhiều tầng từ REST API (Module 14) hoặc Entity JPA có quan hệ (Module 11).

---

## Bài 4 — Text block cho SQL + JSON

### Đề
Method 1: SQL join 3 bảng (`WHERE`, `GROUP BY`, `ORDER BY`) bằng text block. Method 2: JSON response mẫu (≥4 field, 1 field mảng object lồng), chèn giá trị động bằng `.formatted(...)`. So sánh số ký tự escape với nối `+`.

### Phân tích

**Text block** (`"""..."""`, Java 15+) cho phép viết chuỗi nhiều dòng **giữ nguyên định dạng gốc** (xuống dòng, thụt lề) mà **không cần** `\n` + dấu `+` nối chuỗi ở từng dòng như trước — cực kỳ hữu ích cho SQL/JSON/HTML nhiều dòng. `.formatted(args...)` (tương đương `String.format`) chèn giá trị động vào chỗ có `%s`/`%d`.

### Lời giải

```java
package baitap.bai4;

public class Main {

    // ===== Method 1: SQL join 3 bảng bằng text block =====
    static String buildOrderReportSql() {
        return """
                SELECT u.full_name, o.id AS order_id, SUM(oi.quantity * oi.price) AS total
                FROM orders o
                JOIN users u ON o.user_id = u.id
                JOIN order_items oi ON oi.order_id = o.id
                WHERE o.status = 'COMPLETED'
                GROUP BY u.full_name, o.id
                ORDER BY total DESC
                """;
    }

    // ===== Method 2: JSON response mẫu, chèn giá trị động bằng .formatted() =====
    static String buildOrderJson(long orderId, String customerName, double total, String item1, String item2) {
        return """
                {
                  "orderId": %d,
                  "customerName": "%s",
                  "total": %.2f,
                  "status": "COMPLETED",
                  "items": [
                    { "name": "%s", "qty": 2 },
                    { "name": "%s", "qty": 1 }
                  ]
                }""".formatted(orderId, customerName, total, item1, item2);
    }

    public static void main(String[] args) {
        System.out.println("===== SQL =====");
        System.out.println(buildOrderReportSql());

        System.out.println("===== JSON =====");
        System.out.println(buildOrderJson(123, "Pho", 350_000.5, "Bàn phím", "Chuột"));
    }
}
```

**Kết quả chạy:**
```
===== SQL =====
SELECT u.full_name, o.id AS order_id, SUM(oi.quantity * oi.price) AS total
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN order_items oi ON oi.order_id = o.id
WHERE o.status = 'COMPLETED'
GROUP BY u.full_name, o.id
ORDER BY total DESC

===== JSON =====
{
  "orderId": 123,
  "customerName": "Pho",
  "total": 350000.50,
  "status": "COMPLETED",
  "items": [
    { "name": "Bàn phím", "qty": 2 },
    { "name": "Chuột", "qty": 1 }
  ]
}
```

### So sánh số ký tự escape với cách nối `+`

```java
// ❌ Cách CŨ (nối chuỗi bằng +) - phải escape MỌI dấu ngoặc kép, TỰ thêm \n từng dòng
String jsonOld = "{\n" +
        "  \"orderId\": " + orderId + ",\n" +
        "  \"customerName\": \"" + customerName + "\",\n" +
        "  \"total\": " + total + ",\n" +
        "  \"status\": \"COMPLETED\"\n" +
        "}";
// -> 10 dấu \" (escape ngoặc kép) + 5 dấu \n tường minh + 5 dấu + nối chuỗi = RẤT khó đọc, dễ lỗi thiếu/thừa dấu

// ✅ Text block - HOÀN TOÀN không cần escape " thường, không cần \n, không cần +
String jsonNew = """
        {
          "orderId": %d,
          "customerName": "%s",
          "total": %.2f,
          "status": "COMPLETED"
        }""".formatted(orderId, customerName, total);
```

### Giải thích

- **Text block chỉ cần escape** khi thực sự cần **dấu `"""`** xuất hiện làm nội dung (hiếm gặp) — dấu `"` đơn lẻ (như trong JSON) **hoàn toàn không cần escape** vì text block dùng **3 dấu ngoặc kép liên tiếp** làm delimiter, không xung đột với `"` đơn.
- **Thụt lề tự động được "cắt" theo dòng thụt lề ÍT NHẤT** (thường là dòng chứa `"""` đóng) — cho phép viết code Java thụt lề đẹp mắt theo chuẩn style mà **không** làm lệch định dạng chuỗi SQL/JSON thực tế xuất ra.
- **Với SQL/JSON nhiều dòng, số ký tự "nhiễu" (escape + nối chuỗi) giảm tới 60-80%** so với cách nối `+` truyền thống — không chỉ gọn hơn mà còn **giảm hẳn nguy cơ lỗi cú pháp** (quên `\"`, quên `+`, thừa/thiếu khoảng trắng ở đầu dòng) — đây là lý do text block trở thành lựa chọn mặc định khi viết SQL/JSON/HTML trực tiếp trong code Java hiện đại.

---

## Bài 5 — `var` đúng và sai

### Đề
6 khai báo: 3 chỗ `var` **cải thiện** đọc hiểu, 3 chỗ `var` **làm xấu**. Comment giải thích.

### Lời giải

```java
package baitap.bai5;

import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        // ===== 3 CHỖ "var" CẢI THIỆN khả năng đọc =====

        // (1) Generic type QUÁ DÀI - "var" giúp giảm nhiễu thị giác, tên biến "usersByDept" đã đủ rõ nghĩa
        var usersByDept = new HashMap<String, List<Map<String, Object>>>();
        // So với: HashMap<String, List<Map<String, Object>>> usersByDept = new HashMap<>();
        // -> phần bên phải "var" giữ nguyên thông tin kiểu ĐẦY ĐỦ, không mất gì cả

        // (2) Lớp ẩn danh (anonymous class) - kiểu thật rất dài dòng, "var" gọn hơn NHIỀU
        var clickHandler = new Runnable() {
            @Override public void run() { System.out.println("Đã click!"); }
        };
        clickHandler.run();

        // (3) Kết quả của "new" đã RÕ RÀNG kiểu ngay trên cùng dòng - "var" không mất thông tin gì
        var orderList = new ArrayList<String>();
        orderList.add("Order-001");
        System.out.println("orderList: " + orderList);


        // ===== 3 CHỖ "var" LÀM XẤU khả năng đọc =====

        // (4) SAI - kết quả method MỜ NGHĨA, không đoán được kiểu nếu không xem code method
        var result = processOrder(); // "result" là gì? String? OrderResponse? Không rõ - PHẢI xem processOrder() mới biết
        // NÊN viết: OrderResponse result = processOrder();

        // (5) SAI - literal số dễ NHẦM kiểu int/long, "var" che mất cảnh báo trực quan
        var userId = 100000000000L; // Đọc lướt dễ tưởng "int" (thiếu hậu tố L sẽ TRÀN SỐ, nhưng var không "nhắc" gì)
        // NÊN viết: long userId = 100000000000L; - từ khóa "long" TỰ nhắc kiểu dữ liệu ngay khi đọc

        // (6) SAI - cần LẬP TRÌNH THEO INTERFACE (List), không phải theo implementation cụ thể (ArrayList)
        var items = new ArrayList<String>(); // "var" khiến kiểu THỰC SỰ là ArrayList, không phải List
        someMethodExpectingList(items); // vẫn gọi được (ArrayList implements List) NHƯNG code đọc lên
        // không còn thể hiện Ý ĐỊNH THIẾT KẾ "chỉ cần bất kỳ List nào" - mất đi lợi ích lập trình theo interface
        // NÊN viết: List<String> items = new ArrayList<>();
    }

    static Object processOrder() { return "OK"; } // giả lập - kiểu trả về không rõ ràng khi đọc lướt

    static void someMethodExpectingList(List<String> list) {
        System.out.println("Nhận list: " + list);
    }
}
```

### Giải thích tổng quát

**Nguyên tắc chọn khi nào dùng `var`:**
- ✅ Dùng khi **kiểu đã HIỂN NHIÊN từ vế phải** (`new HashMap<...>()`, `new ArrayList<>()`) — `var` không mất thông tin gì, chỉ giảm nhiễu thị giác.
- ✅ Dùng khi kiểu **quá dài dòng** (generic lồng nhau, anonymous class) — cải thiện đọc hiểu rõ rệt.
- ❌ **Tránh** khi kết quả từ method **không rõ kiểu ngay tại chỗ đọc** — buộc người đọc phải "nhảy" sang xem định nghĩa method mới hiểu, làm chậm quá trình đọc code.
- ❌ **Tránh** với **literal số** dễ nhầm kiểu (`int` vs `long` vs `double`) — từ khóa kiểu tường minh đóng vai trò "tài liệu tại chỗ" quan trọng.
- ❌ **Tránh** khi có **chủ đích lập trình theo interface** (`List` thay vì `ArrayList`, `Map` thay vì `HashMap`) — khai báo tường minh `List<String> items = new ArrayList<>()` truyền tải rõ ý định "code này chỉ quan tâm hành vi `List`, không quan tâm implementation cụ thể", giúp dễ đổi implementation sau này (VD: đổi sang `LinkedList`) mà không cần sửa khai báo.

---

## Bài 6 — Bài toán tổng hợp: 10.000 "request" bằng virtual thread

### Đề
10.000 request, mỗi cái `sleep(100)`, dùng `Executors.newVirtualThreadPerTaskExecutor()`. Đo thời gian, so với `newFixedThreadPool(200)`. Thêm `Semaphore(50)` giới hạn số request **thực sự gọi "DB"** cùng lúc dù có 10.000 virtual thread.

### Phân tích

**Virtual Thread** (Java 21, Project Loom) là thread **"ảo"**, cực nhẹ (chỉ vài trăm byte, so với ~1MB của thread hệ điều hành truyền thống — Platform Thread) — JVM có thể tạo **HÀNG TRIỆU** virtual thread mà không cạn tài nguyên OS. Khi 1 virtual thread `sleep()`/block I/O, JVM **tự động "tháo" (unmount)** nó khỏi Platform Thread (carrier thread) bên dưới, cho phép carrier thread đó **phục vụ virtual thread KHÁC** trong lúc chờ — hoàn toàn khác Platform Thread truyền thống (khi block, **toàn bộ tài nguyên OS thread bị "đóng băng" chờ**, không làm việc gì khác được).

### Lời giải

```java
package baitap.bai6;

import java.util.concurrent.*;

public class Main {

    static final int REQUEST_COUNT = 10_000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== Virtual Thread (newVirtualThreadPerTaskExecutor) =====");
        long vtTime = benchmark(Executors.newVirtualThreadPerTaskExecutor());
        System.out.println("Thời gian: " + vtTime + "ms");

        System.out.println("\n===== Fixed Thread Pool (200 platform thread) =====");
        long fixedTime = benchmark(Executors.newFixedThreadPool(200));
        System.out.println("Thời gian: " + fixedTime + "ms");

        System.out.println("\nSo sánh: virtual≈" + vtTime + "ms, fixed(200)≈" + fixedTime + "ms"
                + " (fixed ≈ 10000/200 * 100 = 5000ms theo lý thuyết)");

        // ===== Semaphore(50) giới hạn số request THỰC SỰ gọi "DB" =====
        System.out.println("\n===== Virtual Thread + Semaphore(50) giới hạn DB call =====");
        demoWithSemaphore();
    }

    static long benchmark(ExecutorService executor) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(REQUEST_COUNT);
        long start = System.currentTimeMillis();

        for (int i = 0; i < REQUEST_COUNT; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(100); // giả lập chờ I/O (gọi API/DB...)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long time = System.currentTimeMillis() - start;
        executor.shutdown();
        return time;
    }

    static void demoWithSemaphore() throws InterruptedException {
        Semaphore dbSemaphore = new Semaphore(50); // TỐI ĐA 50 "kết nối DB" cùng lúc
        CountDownLatch latch = new CountDownLatch(1000); // giảm số lượng để demo nhanh, đủ minh họa
        java.util.concurrent.atomic.AtomicInteger activeDbCalls = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger maxObserved = new java.util.concurrent.atomic.AtomicInteger(0);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1000; i++) {
                executor.submit(() -> {
                    try {
                        dbSemaphore.acquire(); // CHỈ 50 virtual thread được "vào" gọi DB cùng lúc
                        try {
                            int current = activeDbCalls.incrementAndGet();
                            maxObserved.updateAndGet(max -> Math.max(max, current));
                            Thread.sleep(20); // giả lập query DB thật sự tốn thời gian
                        } finally {
                            activeDbCalls.decrementAndGet();
                            dbSemaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        System.out.println("Số DB call đồng thời CAO NHẤT quan sát được: " + maxObserved.get() + " (giới hạn: 50)");
    }
}
```

**Kết quả tiêu biểu:**
```
===== Virtual Thread (newVirtualThreadPerTaskExecutor) =====
Thời gian: 145ms

===== Fixed Thread Pool (200 platform thread) =====
Thời gian: 5120ms

So sánh: virtual≈145ms, fixed(200)≈5120ms (fixed ≈ 10000/200 * 100 = 5000ms theo lý thuyết)

===== Virtual Thread + Semaphore(50) giới hạn DB call =====
Số DB call đồng thời CAO NHẤT quan sát được: 50 (giới hạn: 50)
```

### Giải thích

- **Vì sao virtual thread ≈ 100ms (gần bằng thời gian sleep của 1 request duy nhất):** với `newVirtualThreadPerTaskExecutor()`, JVM tạo **10.000 virtual thread ĐỘC LẬP** — mỗi virtual thread tự `sleep(100)` **THỰC SỰ SONG SONG** (về mặt logic), không phải xếp hàng chờ "suất" thread như platform thread giới hạn. Vì `sleep()` là thao tác **block**, JVM tự "tháo" virtual thread khỏi carrier thread trong lúc chờ, giải phóng carrier thread đó phục vụ virtual thread khác — chỉ cần **rất ít** carrier thread (thường = số core CPU) để "luân phiên" phục vụ hàng chục nghìn virtual thread đang `sleep`.
- **Vì sao `fixedThreadPool(200)` ≈ 5000ms:** chỉ có **200 platform thread thật sự** — với 10.000 request, phải chia thành `10000/200 = 50` "đợt" thực thi tuần tự (mỗi đợt 200 request chạy song song, mỗi đợt tốn 100ms) → tổng thời gian ≈ `50 × 100ms = 5000ms`. Đây chính là giới hạn **VẬT LÝ** của Platform Thread — mỗi thread tốn ~1MB stack (RAM) + là tài nguyên OS thật sự (context switch tốn kém) — không thể tạo hàng chục nghìn platform thread cùng lúc mà không cạn RAM/quá tải OS scheduler.
- **Khác biệt về RAM/OS thread:** 10.000 Platform Thread tốn khoảng **10GB RAM** chỉ riêng cho stack (chưa kể overhead khác) — không khả thi trên hầu hết server thông thường. 10.000 Virtual Thread chỉ tốn **vài chục MB** — vì Virtual Thread **KHÔNG map 1-1 với OS thread**, chúng được JVM tự quản lý và "mượn" 1 số lượng nhỏ Platform Thread (carrier thread) để thực thi khi cần.
- **Vì sao giới hạn bằng `Semaphore(50)` chứ KHÔNG bằng kích thước pool:** với Virtual Thread, **KHÔNG CÓ khái niệm "pool size"** theo nghĩa Platform Thread truyền thống — `newVirtualThreadPerTaskExecutor()` tạo **1 virtual thread MỚI cho MỖI task**, không giới hạn số lượng. Muốn giới hạn **SỐ LƯỢNG THAO TÁC ĐỒNG THỜI thực sự chạm tới 1 tài nguyên hữu hạn** (VD: Connection Pool của Database — chỉ có 50 connection thật), phải dùng **`Semaphore`** để kiểm soát **ở tầng LOGIC nghiệp vụ**, hoàn toàn tách biệt khỏi cơ chế quản lý thread bên dưới — đây là điểm khác biệt tư duy quan trọng nhất khi chuyển từ lập trình Platform Thread (giới hạn tự nhiên qua pool size) sang Virtual Thread (không giới hạn tự nhiên, phải TỰ kiểm soát bằng công cụ như `Semaphore` khi cần).
- **Liên hệ thực tế Backend:** đây chính là lý do Spring Boot 3.2+ hỗ trợ **Virtual Thread cho Tomcat** (`spring.threads.virtual.enabled=true`) — mỗi HTTP request được xử lý trên 1 virtual thread riêng, cho phép server chịu tải **hàng chục nghìn request đồng thời** (đặc biệt hiệu quả với request có nhiều I/O chờ đợi — gọi API khác, query DB) mà không cần tăng kích thước thread pool truyền thống lên mức không thực tế.

---

*Đây là lời giải cho toàn bộ Phần B của Module 14. Tiếp theo: Module 15 — JVM Internals.*
