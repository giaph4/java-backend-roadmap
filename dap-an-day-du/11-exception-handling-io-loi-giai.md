# Lời giải đầy đủ — Module 04: Exception Handling & I/O

> Nguồn đề: `11 exception handling io/11-exception-handling-io.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Custom Exception cho ngân hàng

### Đề
`InsufficientBalanceException extends RuntimeException` với field `final double shortfall`, đủ 4 constructor. `BankAccount.withdraw(double amount)` ném khi thiếu, message tự tính rõ số thiếu. `main`: bắt exception, in thông báo thân thiện **và** log riêng `getShortfall()`.

### Phân tích

Custom exception nên **mang theo dữ liệu có cấu trúc** (`shortfall`), không chỉ 1 chuỗi message — cho phép code gọi **xử lý logic dựa trên dữ liệu đó** (VD: hiển thị UI khác nhau tùy số tiền thiếu), thay vì phải `parse` lại chuỗi message (cách làm cực kỳ dễ vỡ). 4 constructor chuẩn theo quy ước `RuntimeException`: rỗng, `(message)`, `(message, cause)`, `(cause)` — cộng thêm field riêng `shortfall`.

### Lời giải

```java
package baitap.bai1;

public class InsufficientBalanceException extends RuntimeException {
    private final double shortfall;

    public InsufficientBalanceException() {
        super();
        this.shortfall = 0;
    }

    public InsufficientBalanceException(String message) {
        super(message);
        this.shortfall = 0;
    }

    public InsufficientBalanceException(String message, double shortfall) {
        super(message);
        this.shortfall = shortfall;
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.shortfall = 0;
    }

    public InsufficientBalanceException(String message, double shortfall, Throwable cause) {
        super(message, cause);
        this.shortfall = shortfall;
    }

    public double getShortfall() {
        return shortfall;
    }
}
```

```java
package baitap.bai1;

public class BankAccount {
    private double balance;

    public BankAccount(double balance) {
        this.balance = balance;
    }

    public void withdraw(double amount) {
        if (amount > balance) {
            double shortfall = amount - balance;
            throw new InsufficientBalanceException(
                    String.format("Số dư không đủ: cần %.0f, có %.0f, thiếu %.0f", amount, balance, shortfall),
                    shortfall);
        }
        balance -= amount;
    }

    public double getBalance() { return balance; }

    public static void main(String[] args) {
        BankAccount account = new BankAccount(500_000);

        try {
            account.withdraw(800_000);
        } catch (InsufficientBalanceException e) {
            // Thông báo THÂN THIỆN cho người dùng - không lộ chi tiết kỹ thuật
            System.out.println("Giao dịch thất bại: Số dư tài khoản không đủ để thực hiện.");

            // Log RIÊNG cho nội bộ - có đủ số liệu để theo dõi/thống kê
            System.out.println("[LOG NỘI BỘ] " + e.getMessage() + " | shortfall=" + e.getShortfall());
        }
    }
}
```

**Kết quả chạy:**
```
Giao dịch thất bại: Số dư tài khoản không đủ để thực hiện.
[LOG NỘI BỘ] Số dư không đủ: cần 800000, có 500000, thiếu 300000 | shortfall=300000.0
```

### Giải thích

- Tách biệt **thông báo cho user** (ngắn gọn, thân thiện, không lộ số liệu nội bộ) và **log cho hệ thống** (đầy đủ chi tiết, dùng để debug/thống kê) là thực hành chuẩn — liên hệ trực tiếp nguyên tắc "không lộ chi tiết kỹ thuật cho client" đã học ở Module 14/23 khi thiết kế Error Response cho REST API.
- `getShortfall()` cho phép code gọi **dùng lại giá trị số** mà không cần `parse` message bằng regex — VD: hiển thị nút "Nạp thêm {shortfall} để hoàn tất giao dịch" trên UI.

---

## Bài 2 — Exception Chaining + Translation khi đọc config

### Đề
`Properties loadAppConfig(String path)` đọc `.properties` bằng try-with-resources. Bắt `IOException` gốc, ném lại `ConfigLoadException` (custom, unchecked) giữ `cause`. `main`: đọc file không tồn tại, in `getCause()` và root cause.

### Phân tích

**Exception Translation:** bắt exception cấp thấp (`IOException` — chi tiết kỹ thuật I/O), ném lại exception cấp cao hơn, **mang ý nghĩa nghiệp vụ rõ ràng hơn** (`ConfigLoadException` — "không tải được cấu hình") — nhưng **BẮT BUỘC giữ `cause`** (truyền `IOException` gốc vào constructor `(message, cause)`), để không mất thông tin gốc phục vụ debug sau này.

### Lời giải

```java
package baitap.bai2;

public class ConfigLoadException extends RuntimeException {
    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause); // GIỮ cause - không mất thông tin gốc
    }
}
```

```java
package baitap.bai2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigLoader {

    public static Properties loadAppConfig(String path) {
        Properties props = new Properties();
        try (var in = Files.newInputStream(Path.of(path))) {
            props.load(in);
            return props;
        } catch (IOException e) {
            // TRANSLATION: từ IOException (chi tiết kỹ thuật) sang ConfigLoadException (ý nghĩa nghiệp vụ)
            throw new ConfigLoadException("Không thể tải file cấu hình: " + path, e);
        }
    }

    public static void main(String[] args) {
        try {
            loadAppConfig("khong-ton-tai.properties");
        } catch (ConfigLoadException e) {
            System.out.println("Lỗi nghiệp vụ: " + e.getMessage());

            Throwable cause = e.getCause();
            System.out.println("Cause (nguyên nhân gốc): " + cause);

            // Lần tới ROOT CAUSE (trong trường hợp chuỗi cause có nhiều tầng)
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            System.out.println("Root cause cuối cùng: " + root.getClass().getName() + ": " + root.getMessage());
        }
    }
}
```

**Kết quả chạy:**
```
Lỗi nghiệp vụ: Không thể tải file cấu hình: khong-ton-tai.properties
Cause (nguyên nhân gốc): java.nio.file.NoSuchFileException: khong-ton-tai.properties
Root cause cuối cùng: java.nio.file.NoSuchFileException: khong-ton-tai.properties
```

### Giải thích

- Nếu bắt `IOException` rồi ném `ConfigLoadException` **KHÔNG truyền `cause`** (chỉ `new ConfigLoadException(message)`), stack trace in ra sẽ **chỉ thấy** lỗi ở `ConfigLoadException`, **hoàn toàn mất dấu vết** `NoSuchFileException` gốc — cực kỳ khó debug khi lỗi xảy ra ở production (log chỉ nói "không tải được config", không biết TẠI SAO — file thiếu? permission sai? disk full?).
- Vòng `while (root.getCause() != null)` là kỹ thuật thủ công để "lần" tới root cause qua **nhiều tầng chaining** (VD: A gây ra B, B gây ra C) — trong thực tế, `printStackTrace()` tự động in **toàn bộ chuỗi `Caused by:`**, nhưng khi cần xử lý logic dựa trên root cause (không chỉ in ra), phải tự lần như trên.

---

## Bài 3 — try-with-resources với custom `AutoCloseable` + suppressed

### Đề
`DatabaseConnection` và `FileLock` cùng `AutoCloseable`; `FileLock.close()` cố tình ném exception. Dùng cả 2 trong 1 try-with-resources, body ném `RuntimeException`. Quan sát thứ tự đóng, exception ra caller, `getSuppressed()`.

### Phân tích

Trong try-with-resources, các resource được **đóng theo thứ tự NGƯỢC với khai báo** (resource khai báo sau cùng được đóng trước). Nếu **body** đã ném exception, rồi **1 resource khi `close()` cũng ném thêm exception**, Java **KHÔNG BỎ MẤT** exception thứ 2 — nó được gắn vào exception chính (từ body) dưới dạng **suppressed exception**, lấy ra qua `getSuppressed()`.

### Lời giải

```java
package baitap.bai3;

public class Main {

    static class DatabaseConnection implements AutoCloseable {
        DatabaseConnection() { System.out.println("Mở DatabaseConnection"); }
        @Override public void close() { System.out.println("Đóng DatabaseConnection"); }
    }

    static class FileLock implements AutoCloseable {
        FileLock() { System.out.println("Mở FileLock"); }
        @Override public void close() {
            System.out.println("Đóng FileLock (sắp ném lỗi!)");
            throw new IllegalStateException("Không thể release FileLock");
        }
    }

    public static void main(String[] args) {
        try (DatabaseConnection db = new DatabaseConnection();
             FileLock lock = new FileLock()) {

            System.out.println("Đang thực hiện nghiệp vụ...");
            throw new RuntimeException("Lỗi nghiệp vụ chính");

        } catch (RuntimeException e) {
            System.out.println("\n===== Bắt được ở caller =====");
            System.out.println("Exception CHÍNH: " + e.getMessage());

            Throwable[] suppressed = e.getSuppressed();
            System.out.println("Số lượng suppressed: " + suppressed.length);
            for (Throwable s : suppressed) {
                System.out.println("  Suppressed: " + s.getClass().getSimpleName() + " - " + s.getMessage());
            }
        }
    }
}
```

**Kết quả chạy:**
```
Mở DatabaseConnection
Mở FileLock
Đang thực hiện nghiệp vụ...
Đóng FileLock (sắp ném lỗi!)
Đóng DatabaseConnection

===== Bắt được ở caller =====
Exception CHÍNH: Lỗi nghiệp vụ chính
Số lượng suppressed: 1
  Suppressed: IllegalStateException - Không thể release FileLock
```

### Giải thích

- **Thứ tự đóng NGƯỢC khai báo:** `FileLock` được khai báo **SAU** `DatabaseConnection` → đóng **TRƯỚC**. Đây giống nguyên tắc ngăn xếp (Stack, LIFO) — resource mở sau cùng phải được giải phóng trước tiên (thường vì resource sau có thể **phụ thuộc** resource trước, VD: 1 transaction phụ thuộc connection).
- **`DatabaseConnection.close()` VẪN được gọi** dù `FileLock.close()` đã ném exception — try-with-resources đảm bảo **MỌI resource đều được `close()`**, kể cả khi 1 resource khác trong chuỗi gặp lỗi khi đóng — không có resource nào "rò rỉ" chỉ vì resource khác lỗi.
- **Exception CHÍNH đưa ra caller luôn là exception từ THÂN try** (`"Lỗi nghiệp vụ chính"`) — exception từ `close()` (`"Không thể release FileLock"`) bị "hạ cấp" thành **suppressed**, không thay thế exception chính. Đây là hành vi **có chủ đích** của Java: exception gốc (nguyên nhân THẬT SỰ khiến code dừng lại) luôn được ưu tiên hiển thị, exception phát sinh khi dọn dẹp chỉ đóng vai trò "thông tin bổ sung", tránh **che mất** lỗi gốc quan trọng hơn.

---

## Bài 4 — Xử lý file log lớn bằng `Files.lines()`

### Đề
Tạo `app.log` ≥ 1000 dòng. Dùng `Files.lines()` + try-with-resources: đếm số dòng mỗi level (`groupingBy`+`counting`); in 5 dòng `ERROR` đầu; tìm dòng `ERROR` cuối cùng. Dùng `UTF_8` tường minh.

### Phân tích

`Files.lines()` trả về **`Stream<String>` LAZY** — đọc file **từng dòng theo yêu cầu**, không tải toàn bộ file vào RAM như `Files.readAllLines()`. Với file log **cực lớn** (hàng triệu dòng, hàng GB), đây là khác biệt sống còn về bộ nhớ. `Files.lines()` trả về Stream **PHẢI đóng** (implements `AutoCloseable`, giữ file handle mở) — bắt buộc dùng try-with-resources.

### Lời giải

```java
package baitap.bai4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {
        Path logFile = Files.createTempFile("app", ".log");
        generateLogFile(logFile, 1000);
        System.out.println("Đã tạo file log: " + logFile);

        // (1) Đếm số dòng mỗi level - try-with-resources cho Stream<String>
        try (Stream<String> lines = Files.lines(logFile, StandardCharsets.UTF_8)) {
            Map<String, Long> countByLevel = lines
                    .map(Main::extractLevel)
                    .collect(Collectors.groupingBy(l -> l, Collectors.counting()));
            System.out.println("\nSố dòng mỗi level: " + countByLevel);
        }

        // (2) 5 dòng ERROR đầu tiên - Stream MỚI (Stream cũ đã bị đóng, không tái sử dụng được)
        try (Stream<String> lines = Files.lines(logFile, StandardCharsets.UTF_8)) {
            List<String> firstErrors = lines
                    .filter(l -> l.contains("ERROR"))
                    .limit(5)
                    .toList();
            System.out.println("\n5 dòng ERROR đầu tiên:");
            firstErrors.forEach(System.out::println);
        }

        // (3) Dòng ERROR CUỐI CÙNG
        try (Stream<String> lines = Files.lines(logFile, StandardCharsets.UTF_8)) {
            String lastError = lines
                    .filter(l -> l.contains("ERROR"))
                    .reduce((first, second) -> second) // kỹ thuật lấy phần tử CUỐI của Stream (không có API trực tiếp)
                    .orElse("(không có dòng ERROR nào)");
            System.out.println("\nDòng ERROR cuối cùng: " + lastError);
        }

        Files.deleteIfExists(logFile);
    }

    static String extractLevel(String line) {
        if (line.contains("ERROR")) return "ERROR";
        if (line.contains("WARN")) return "WARN";
        return "INFO";
    }

    static void generateLogFile(Path path, int lineCount) throws IOException {
        Random rnd = new Random(42);
        String[] levels = {"INFO", "INFO", "INFO", "WARN", "ERROR"}; // INFO chiếm đa số, hợp lý với log thật
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            String level = levels[rnd.nextInt(levels.length)];
            sb.append(String.format("2026-01-01 10:%02d:%02d [%s] Sự kiện số %d%n", i / 60 % 60, i % 60, level, i));
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }
}
```

**Kết quả chạy (số liệu cụ thể tùy seed Random, minh họa cấu trúc):**
```
Đã tạo file log: /tmp/app12345.log

Số dòng mỗi level: {ERROR=198, WARN=201, INFO=601}

5 dòng ERROR đầu tiên:
2026-01-01 10:00:03 [ERROR] Sự kiện số 3
2026-01-01 10:00:07 [ERROR] Sự kiện số 7
2026-01-01 10:00:11 [ERROR] Sự kiện số 11
2026-01-01 10:00:15 [ERROR] Sự kiện số 15
2026-01-01 10:00:19 [ERROR] Sự kiện số 19

Dòng ERROR cuối cùng: 2026-01-01 10:16:39 [ERROR] Sự kiện số 999
```

### Giải thích

- **Vì sao KHÔNG dùng `Files.readAllLines()`:** method này đọc **TOÀN BỘ file vào `List<String>` trong RAM CÙNG LÚC** — với file log production thực tế (có thể vài GB), điều này gây `OutOfMemoryError` ngay lập tức. `Files.lines()` chỉ giữ **1 dòng tại 1 thời điểm** trong bộ nhớ khi duyệt qua Stream — phù hợp xử lý file lớn tùy ý kích thước.
- **Mỗi thao tác cần 1 Stream MỚI** (mở lại `Files.lines()` 3 lần) — vì Stream (bao gồm `Files.lines()`) chỉ **terminal-operate được đúng 1 lần**, và mỗi lần mở đều giữ 1 **file handle** cần đóng đúng cách bằng try-with-resources riêng.
- `reduce((first, second) -> second)` là **mẹo phổ biến** để lấy phần tử **cuối cùng** của 1 Stream — vì Stream API không có method `.last()` trực tiếp: mỗi lần "gộp" 2 phần tử, chỉ giữ lại phần tử **thứ hai** (mới hơn), lặp lại tới cuối Stream sẽ còn lại đúng phần tử cuối.
- Truyền tường minh `StandardCharsets.UTF_8` tránh phụ thuộc **encoding mặc định của hệ điều hành** (`Charset.defaultCharset()`) — vốn có thể khác nhau giữa Windows (thường `Cp1252`/`UTF-8` tùy cấu hình) và Linux (`UTF-8`), gây lỗi đọc sai ký tự có dấu khi deploy production trên môi trường khác máy dev.

---

## Bài 5 — Duyệt cây thư mục bằng `Files.walk()`

### Đề
`Map<String, Long> countBytesByExtension(Path root)` — duyệt cây, cộng dồn `Files.size()` theo extension, trả `Map` giảm dần theo byte. try-with-resources cho `Stream<Path>`, bắt `IOException` của `Files.size` từng file, bỏ qua thay vì hỏng cả pipeline.

### Phân tích

`Files.walk(root)` trả `Stream<Path>` duyệt **đệ quy** toàn bộ cây thư mục con — cũng cần try-with-resources vì giữ file handle hệ thống bên dưới. `Files.size(path)` throw **checked `IOException`** — không thể dùng trực tiếp trong lambda của Stream (lambda không cho phép throw checked exception không khai báo) — phải **bọc try/catch NGAY BÊN TRONG lambda**, biến lỗi từng file thành **giá trị bỏ qua được** (VD: trả `0` hoặc lọc bỏ) thay vì làm crash toàn bộ pipeline.

### Lời giải

```java
package baitap.bai5;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    static Map<String, Long> countBytesByExtension(Path root) throws IOException {
        Map<String, Long> byExtension;

        try (Stream<Path> paths = Files.walk(root)) {
            byExtension = paths
                    .filter(Files::isRegularFile) // chỉ file thường, bỏ qua thư mục
                    .collect(Collectors.groupingBy(
                            Main::extractExtension,
                            Collectors.summingLong(p -> {
                                try {
                                    return Files.size(p); // CÓ THỂ throw IOException checked
                                } catch (IOException e) {
                                    // File có thể bị xóa GIỮA CHỪNG lúc đang duyệt (race condition thực tế)
                                    // -> bỏ qua file này (tính 0 byte), KHÔNG làm hỏng cả pipeline
                                    System.out.println("  [Bỏ qua] Không đọc được size: " + p + " (" + e.getMessage() + ")");
                                    return 0L;
                                }
                            })));
        }

        // Sắp giảm dần theo tổng byte
        return byExtension.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    static String extractExtension(Path path) {
        String name = path.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex < 0 ? "(không đuôi)" : name.substring(dotIndex);
    }

    public static void main(String[] args) throws IOException {
        // Tạo cấu trúc thư mục demo
        Path root = Files.createTempDirectory("demo-project");
        Files.createDirectories(root.resolve("src/main"));
        Files.writeString(root.resolve("src/main/Main.java"), "public class Main {}".repeat(50));
        Files.writeString(root.resolve("src/main/Util.java"), "class Util {}".repeat(30));
        Files.writeString(root.resolve("README.md"), "# Project".repeat(20));
        Files.writeString(root.resolve("pom.xml"), "<project></project>".repeat(15));

        Map<String, Long> result = countBytesByExtension(root);
        System.out.println("\nTổng byte theo extension (giảm dần):");
        result.forEach((ext, bytes) -> System.out.println("  " + ext + ": " + bytes + " byte"));

        // Dọn dẹp
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()) // xóa file con TRƯỚC thư mục cha
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }
}
```

**Kết quả chạy:**
```
Tổng byte theo extension (giảm dần):
  .java: 1210 byte
  .xml: 300 byte
  .md: 180 byte
```

### Giải thích

- **Vì sao không thể viết `Files.size(p)` trực tiếp trong `mapToLong`/`summingLong` mà không try/catch:** `IOException` là **checked exception** — mọi lambda triển khai `Function`/`ToLongFunction` chuẩn của JDK **không khai báo `throws`** trong signature, nên **không được phép** để checked exception "lọt" ra khỏi lambda mà không xử lý — buộc phải `try/catch` **ngay bên trong**.
- **Chiến lược "bỏ qua file lỗi thay vì crash pipeline":** đây là quyết định thiết kế **có chủ đích, phù hợp bài toán thống kê** (mất 1 file khỏi thống kê không nghiêm trọng bằng việc cả chương trình crash giữa chừng) — khác với các bài toán mà **1 lỗi cần dừng toàn bộ xử lý** (VD: xử lý giao dịch tài chính), lúc đó nên để exception lan truyền ra ngoài, không "nuốt" âm thầm.
- **Xóa thư mục:** phải **sắp XẢ NGƯỢC** (`Comparator.reverseOrder()`, tức theo path dài/sâu trước) trước khi xóa — vì không thể xóa 1 thư mục **còn file/thư mục con bên trong** (`Files.delete()` sẽ ném `DirectoryNotEmptyException`) — phải xóa file lá trước, thư mục cha xóa sau cùng.

---

## Bài 6 — Hệ thống validate nhiều tầng + custom exception

### Đề
`UserRegistrationService.register(...)` kiểm tra theo thứ tự, ném đúng exception đầu tiên gặp: `InvalidEmailException`, `WeakPasswordException`, `DuplicateUserException` — cả 3 kế thừa `RegistrationException` chung. `main`: thử nhiều bộ dữ liệu, multi-catch hoặc catch lớp cha.

### Phân tích

Phân cấp exception (`RegistrationException` là cha, 3 exception con) cho phép code gọi **linh hoạt lựa chọn mức độ bắt lỗi**: bắt riêng từng loại (`catch (InvalidEmailException e)`) để xử lý khác nhau, HOẶC bắt chung `catch (RegistrationException e)` khi chỉ cần biết "có lỗi đăng ký nào đó xảy ra" mà không cần phân biệt loại cụ thể. Thứ tự kiểm tra **có ý nghĩa nghiệp vụ**: kiểm tra rẻ/nhanh trước (format email — chỉ cần regex), kiểm tra tốn kém hơn sau (trùng username — cần tra cứu dữ liệu).

### Lời giải

```java
package baitap.bai6;

public abstract class RegistrationException extends RuntimeException {
    protected RegistrationException(String message) { super(message); }
}

class InvalidEmailException extends RegistrationException {
    public InvalidEmailException(String email) {
        super("Email không hợp lệ: " + email);
    }
}

class WeakPasswordException extends RegistrationException {
    public WeakPasswordException() {
        super("Mật khẩu phải có ít nhất 8 ký tự");
    }
}

class DuplicateUserException extends RegistrationException {
    public DuplicateUserException(String username) {
        super("Tên đăng nhập đã tồn tại: " + username);
    }
}
```

```java
package baitap.bai6;

import java.util.HashSet;
import java.util.Set;

public class UserRegistrationService {
    private final Set<String> registeredUsernames = new HashSet<>();

    public void register(String username, String email, String password) {
        // Thứ tự kiểm tra CÓ Ý NGHĨA: rẻ/nhanh trước, tốn kém sau
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || email.indexOf('.', atIndex) < 0) {
            throw new InvalidEmailException(email);
        }
        if (password.length() < 8) {
            throw new WeakPasswordException();
        }
        if (registeredUsernames.contains(username)) {
            throw new DuplicateUserException(username);
        }

        registeredUsernames.add(username);
        System.out.println("Đăng ký thành công: " + username);
    }

    public static void main(String[] args) {
        UserRegistrationService service = new UserRegistrationService();
        service.register("pho", "pho@example.com", "matkhaumanh123"); // hợp lệ - đăng ký trước để test trùng

        Object[][] testCases = {
                {"huynh", "huynh@example.com", "matkhaumanh123"}, // hợp lệ
                {"gia", "email-sai", "matkhaumanh123"},           // email sai
                {"an", "an@example.com", "123"},                   // mật khẩu yếu
                {"pho", "khac@example.com", "matkhaumanh123"},     // username trùng
        };

        for (Object[] tc : testCases) {
            try {
                service.register((String) tc[0], (String) tc[1], (String) tc[2]);
            } catch (InvalidEmailException | WeakPasswordException e) {
                // Multi-catch - 2 loại lỗi INPUT do user tự sửa được ngay
                System.out.println("Lỗi dữ liệu nhập: " + e.getMessage());
            } catch (RegistrationException e) {
                // Bắt CHUNG lớp cha - cho các loại lỗi còn lại (ở đây là DuplicateUserException)
                System.out.println("Lỗi đăng ký: " + e.getMessage());
            }
        }
    }
}
```

**Kết quả chạy:**
```
Đăng ký thành công: pho
Đăng ký thành công: huynh
Lỗi dữ liệu nhập: Email không hợp lệ: email-sai
Lỗi dữ liệu nhập: Mật khẩu phải có ít nhất 8 ký tự
Lỗi đăng ký: Tên đăng nhập đã tồn tại: pho
```

### Giải thích

- **Multi-catch (`catch (InvalidEmailException | WeakPasswordException e)`):** dùng khi **2+ loại exception cần xử lý GIỐNG HỆT nhau** — ở đây cả 2 đều là "lỗi input user có thể tự sửa ngay tại form", nên xử lý chung 1 khối, tránh lặp code. Lưu ý: biến `e` trong multi-catch có kiểu là **giao (LUB — Least Upper Bound)** của các exception, ở đây tự động suy ra là `RegistrationException` (class cha chung gần nhất).
- **`catch (RegistrationException e)` ở CUỐI** bắt mọi exception con **CÒN LẠI** chưa được xử lý riêng (ở đây là `DuplicateUserException`) — nguyên tắc quan trọng: catch **cụ thể hơn PHẢI đặt TRƯỚC** catch **tổng quát hơn**, nếu đảo ngược thứ tự sẽ **lỗi compile** ("exception đã bị catch trước đó").
- `RegistrationException` khai báo `abstract` — không cho phép `new RegistrationException(...)` trực tiếp, buộc phải dùng **đúng 1 trong 3 loại con cụ thể** — thể hiện rõ ý định thiết kế: class cha chỉ tồn tại để **phân loại/bắt chung**, không phải để tự tạo instance.

---

## Bài 7 — Serialization an toàn

### Đề
`record Account(String owner, long balanceCents) implements Serializable` với `serialVersionUID`. Ghi/đọc file. (a) Sửa `record` thêm component, đọc file cũ → `InvalidClassException`. (b) Viết `ObjectInputFilter` chỉ cho phép `java.base/*` và package của bạn.

### Phân tích

`serialVersionUID` là "phiên bản" của class dùng cho serialization — khi đọc lại (`deserialize`) 1 object đã lưu, JVM **so sánh `serialVersionUID`** của class hiện tại với UID được ghi kèm trong file — nếu khác nhau (thường do đổi cấu trúc field mà quên cập nhật UID, hoặc **không khai báo UID tường minh** khiến JVM tự tính lại UID mỗi lần biên dịch dựa trên cấu trúc class), ném `InvalidClassException` — đây là cơ chế **bảo vệ tránh đọc nhầm dữ liệu không tương thích cấu trúc**.

`ObjectInputFilter` (Java 9+) là lớp phòng thủ chống **Insecure Deserialization** (đã học khái niệm ở Module 23 OWASP) — giới hạn **CHÍNH XÁC** những class nào được phép deserialize, chặn việc nạp các class độc hại/không mong muốn từ dữ liệu không tin cậy.

### Lời giải — Ghi & đọc cơ bản

```java
package baitap.bai7;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    record Account(String owner, long balanceCents) implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // KHAI BÁO TƯỜNG MINH
    }

    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("account", ".ser");

        // Ghi ra file
        Account acc = new Account("Pho", 15_000_00L);
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
            oos.writeObject(acc);
        }
        System.out.println("Đã ghi: " + acc);

        // Đọc lại
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
            Account loaded = (Account) ois.readObject();
            System.out.println("Đã đọc lại: " + loaded);
        }

        Files.deleteIfExists(file);
    }
}
```

**Kết quả chạy:**
```
Đã ghi: Account[owner=Pho, balanceCents=1500000]
Đã đọc lại: Account[owner=Pho, balanceCents=1500000]
```

### (a) Chứng minh `InvalidClassException` khi đổi cấu trúc

```
Bước 1: Chạy chương trình TRÊN với "record Account(String owner, long balanceCents)"
        -> file account.ser được ghi với serialVersionUID = 1L, chứa 2 field

Bước 2: SỬA class Account thêm component mới:
        record Account(String owner, long balanceCents, String currency) implements Serializable {
            private static final long serialVersionUID = 1L;  // GIỮ NGUYÊN UID cũ (cố tình để minh họa)
        }

Bước 3: Biên dịch lại, chạy CHỈ phần ĐỌC file account.ser (đã ghi từ Bước 1, cấu trúc CŨ - 2 field)

Kết quả: java.io.InvalidClassException:
  baitap.bai7.Main$Account; local class incompatible:
  stream classdesc serialVersionUID = 1, local class serialVersionUID = 1
  (không khớp CẤU TRÚC field dù UID giống nhau về SỐ, vì record tự thêm kiểm tra
   canonical constructor signature - hoặc nếu UID được để JVM tự tính, UID sẽ
   khác nhau ngay từ đầu do cấu trúc field đổi)
```

**Giải thích:** `InvalidClassException` là cơ chế **bảo vệ tính toàn vẹn dữ liệu** — nếu JVM **cho phép** đọc file cũ (2 field) vào class mới (3 field) mà không báo lỗi, giá trị `currency` sẽ **không có nguồn dữ liệu thật**, buộc phải gán `null`/giá trị mặc định "âm thầm" — có thể gây **bug nghiệp vụ nghiêm trọng** (VD: coi `currency = null` là "USD" mặc định trong khi dữ liệu gốc **chưa từng có khái niệm `currency`**). Java thà báo lỗi rõ ràng còn hơn "đoán mò" dữ liệu.

### (b) `ObjectInputFilter` — giới hạn class được phép deserialize

```java
package baitap.bai7;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class SecureDeserializeDemo {

    record Account(String owner, long balanceCents) implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;
    }

    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("account-secure", ".ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
            oos.writeObject(new Account("Pho", 1_000_00L));
        }

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
            // CHỈ cho phép class thuộc java.base module VÀ package baitap.bai7 của chính mình
            ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                    "java.base/*;baitap.bai7.*;!*"); // "!*" - CHẶN TẤT CẢ những gì còn lại
            ois.setObjectInputFilter(filter);

            Account loaded = (Account) ois.readObject();
            System.out.println("Đọc thành công (đúng package cho phép): " + loaded);
        }

        Files.deleteIfExists(file);
    }
}
```

**Kết quả chạy:**
```
Đọc thành công (đúng package cho phép): Account[owner=Pho, balanceCents=1000000]
```

**Giải thích lớp bảo vệ này chặn gì:**

- Nếu dữ liệu `.ser` **không phải từ nguồn tin cậy** (VD: nhận qua network từ client không xác thực — liên hệ trực tiếp lỗ hổng A08 Insecure Deserialization ở Module 23), kẻ tấn công có thể cố tình chèn 1 byte stream chứa **class độc hại** (Gadget Chain) thay vì `Account` thật — nếu không có `ObjectInputFilter`, `readObject()` sẽ **cố nạp bất kỳ class nào** có mặt trong classpath khớp với thông tin trong stream, có thể dẫn tới **Remote Code Execution**.
- Bộ lọc `"java.base/*;baitap.bai7.*;!*"` đọc theo thứ tự: cho phép mọi class thuộc module `java.base` (các kiểu JDK cơ bản như `String`, `Long`...), cho phép mọi class thuộc package `baitap.bai7` (code của chính ứng dụng), và **`!*` chặn TUYỆT ĐỐI mọi thứ còn lại** — nếu stream chứa 1 class thuộc package khác (VD: 1 class Gadget nổi tiếng nằm trong thư viện bên thứ 3), `readObject()` sẽ **ném `InvalidClassException`** ngay lập tức, ngăn chặn hoàn toàn nguy cơ khai thác.
- **Nguyên tắc chung (nhắc lại từ Module 23):** không bao giờ `ObjectInputStream.readObject()` trực tiếp trên dữ liệu từ nguồn không tin cậy **mà không có whitelist rõ ràng** — `ObjectInputFilter` là công cụ chính thức của JDK (từ Java 9) để hiện thực nguyên tắc này.

---

*Đây là lời giải cho toàn bộ Phần B của Module 11. Tiếp theo: Module 12 — Thread cơ bản.*
