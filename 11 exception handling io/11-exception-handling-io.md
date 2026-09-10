# Module 04 — Exception Handling & I/O

> **Mức độ ưu tiên: Cao** — Backend chuyên nghiệp = xử lý lỗi tử tế: không để một exception không bắt được làm sập service, không "nuốt" lỗi rồi tiếp tục như không có gì, luôn giữ được nguyên nhân gốc trong log. Đây cũng là nền tảng để hiểu `@ControllerAdvice`/`@ExceptionHandler` (Module 13) và connection pool (Module 12).

> **Phạm vi bài này:** cơ chế exception của Java (hierarchy, checked/unchecked, `try`/`catch`/`finally`, try-with-resources, custom exception, chaining, best practice), File I/O với `java.nio.file` + `java.io` luồng, và Serialization cơ bản. **Chỉ nhắc tên, không đi sâu:** `@ControllerAdvice`/HTTP error mapping (Module 13), Bean Validation (Module 13), JDBC/connection pool (Module 12), Concurrency (Module 09 — trừ phần `InterruptedException` nêu vừa đủ), logging framework (Module 21). Các chỗ chạm chủ đề khác chỉ nêu đủ để bài trọn vẹn.

---

## Mục lục

1. [Exception Hierarchy — cây `Throwable`](#1-exception-hierarchy--cây-throwable)
2. [Checked vs Unchecked Exception](#2-checked-vs-unchecked-exception)
3. [`try` / `catch` / `finally`](#3-try--catch--finally)
4. [try-with-resources & suppressed exception](#4-try-with-resources--suppressed-exception)
5. [Custom Exception](#5-custom-exception)
6. [Exception Chaining & Exception Translation](#6-exception-chaining--exception-translation)
7. [Best Practice — nên và không nên](#7-best-practice--nên-và-không-nên)
8. [File I/O với `java.nio.file`](#8-file-io-với-javaniofile)
9. [`java.io` — luồng byte vs luồng ký tự](#9-javaio--luồng-byte-vs-luồng-ký-tự)
10. [Serialization](#10-serialization)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. Exception Hierarchy — cây `Throwable`

```
                          Throwable  ── implements Serializable
                         /          \
                    Error            Exception
        (lỗi tầng JVM, KHÔNG        /          \
         nên bắt xử lý)   RuntimeException      (các Exception khác)
         OutOfMemoryError   (UNCHECKED)          (CHECKED)
         StackOverflowError      │                    │
         NoClassDefFoundError    │                    │
         ExceptionInInitializerError               IOException
                          NullPointerException     SQLException
                          IllegalArgumentException InterruptedException
                          IllegalStateException    ClassNotFoundException
                          ClassCastException       ParseException
                          ArithmeticException
                          NumberFormatException
                          ConcurrentModificationException
                          UnsupportedOperationException
```

| Nhánh | Đặc điểm |
|---|---|
| `Error` | Lỗi nghiêm trọng ở tầng JVM (hết bộ nhớ, tràn stack, lỗi nạp class). **Không** `catch` để xử lý tiếp — chương trình thường không thể phục hồi. |
| `RuntimeException` + subclass | **Unchecked** — compiler **không** bắt buộc `catch`/`throws`. Thường là **bug lập trình**. |
| `Exception` trừ nhánh `RuntimeException` | **Checked** — compiler **bắt buộc** `catch` hoặc `throws`. Thường là **điều kiện bên ngoài** (file, mạng, DB). |

### `Throwable` — các method hay dùng khi `catch`

```java
catch (Exception e) {
    e.getMessage();          // mô tả lỗi (có thể null)
    e.getCause();            // Throwable gây ra e, hoặc null — "nguyên nhân gốc" (mục 6)
    e.getStackTrace();       // StackTraceElement[] — nơi exception phát sinh
    e.getSuppressed();       // Throwable[] — exception bị "nén" trong try-with-resources (mục 4)
    log.error("Lỗi xử lý đơn {}", orderId, e);   // SLF4J: truyền e ở tham số CUỐI, KHÔNG cần {} cho nó
}
```

> `printStackTrace()` chỉ dùng khi thử nhanh. Code production luôn dùng logger (`SLF4J`/`Logback` — Module 21) để log có timestamp, level, và đi đúng đích (file/aggregator).

### `Error` đáng biết (đã gặp ở các module trước)

`ExceptionInInitializerError` → khi static initializer ném exception; kéo theo `NoClassDefFoundError` ở lần dùng class sau đó (Module 01.3). `StackOverflowError` → đệ quy không điểm dừng / không có tối ưu đuôi (Module 01.2).

---

## 2. Checked vs Unchecked Exception

### Checked — compiler kiểm tra lúc biên dịch

```java
public void readFile(String path) throws IOException {   // BẮT BUỘC throws, hoặc catch bên trong
    var reader = new FileReader(path);   // FileNotFoundException (con của IOException) là checked
}
```

Không `throws`/`try-catch` → lỗi compile: `unreported exception IOException; must be caught or declared to be thrown`.

### Unchecked (`RuntimeException`) — compiler không ép

```java
public int divide(int a, int b) {
    return a / b;   // ArithmeticException nếu b == 0 — không cần khai báo
}
```

### Bảng so sánh & triết lý

| Tiêu chí | Checked | Unchecked |
|---|---|---|
| Compiler ép xử lý | Có (`catch`/`throws`) | Không |
| Ý nghĩa | Lỗi **dự đoán được**, caller **có thể phục hồi** (thử lại, dùng giá trị mặc định, báo người dùng) | **Bug lập trình** — nên sửa code, không "chữa cháy" runtime |
| Ví dụ | `IOException`, `SQLException`, `InterruptedException` | `NullPointerException`, `IllegalArgumentException`, `IllegalStateException` |
| Xu hướng framework | Spring bọc `SQLException` → `DataAccessException` (unchecked); tránh `try/catch` rải khắp nơi | — |

> **NPE "biết nói" (Helpful NullPointerException, Java 14+):** thông báo chỉ rõ *biến nào* null — `Cannot invoke "String.length()" because "user.name" is null` — thay vì chỉ số dòng. Bật mặc định từ Java 15.

### `InterruptedException` — trường hợp checked đặc biệt

Khi bị `catch`, **không được nuốt** — phải **khôi phục cờ interrupt** để tầng trên biết luồng đã được yêu cầu dừng:

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // đặt lại cờ — KHÔNG bỏ qua
    throw new RuntimeException("Bị gián đoạn khi chờ", e);
}
```

> Tranh luận thực tế: nhiều người thấy checked exception gây "mệt mỏi" (`throws Exception` lan truyền, xung đột với lambda/Stream — Module 03.3). Nguyên tắc dung hòa: dùng checked khi caller **thật sự** có phương án xử lý; ngoài ra dùng unchecked và **dịch (translate)** lỗi tầng dưới thành lỗi hợp với tầng của mình (mục 6).

---

## 3. `try` / `catch` / `finally`

```java
try {
    int[] a = {1, 2, 3};
    System.out.println(a[5]);                 // ArrayIndexOutOfBoundsException
} catch (ArrayIndexOutOfBoundsException e) {  // cụ thể trước
    log.warn("Truy cập mảng sai chỉ số", e);
} catch (RuntimeException e) {                 // tổng quát hơn sau
    log.error("Lỗi runtime khác", e);
} finally {
    // LUÔN chạy — dù có exception hay không, dù try/catch có return
}
```

### Thứ tự `catch` — cụ thể → tổng quát

```java
} catch (FileNotFoundException e) {   // con
} catch (IOException e) {              // cha — phải đứng SAU con
} catch (Exception e) {                // tổng quát nhất — CUỐI
}
// Đảo ngược (IOException trước FileNotFoundException) → lỗi compile "exception has already been caught"
```

### Multi-catch (Java 7+)

```java
} catch (NumberFormatException | DateTimeParseException e) {   // xử lý giống nhau, không lặp code
    throw new InvalidInputException("Dữ liệu không đúng định dạng", e);
}
```

Biến `e` trong multi-catch **ngầm `final`** — không gán lại được.

### Precise rethrow (Java 7+) — `catch` rộng, `throw` lại vẫn giữ kiểu hẹp

```java
public void doWork() throws IOException, SQLException {
    try {
        risky();                       // ném IOException hoặc SQLException
    } catch (Exception e) {
        log.warn("thử lại lần cuối...", e);
        throw e;                        // compiler BIẾT e chỉ có thể là IOException | SQLException
    }
}
```

### `finally` — luôn chạy, và các bẫy

```java
public int test() {
    try { return 1; }
    finally { System.out.println("chạy TRƯỚC khi method thực sự trả 1"); }
}
```

`finally` chạy trong **mọi** trường hợp trừ khi `System.exit()` được gọi hoặc luồng/JVM bị hủy.

> ⚠️ **Ba anti-pattern trong `finally`** — đều khiến giá trị/exception của `try` biến mất **âm thầm**:
> ```java
> try { return 1; }        finally { return 2; }              // luôn trả 2
> try { return 1; }        finally { break; }                  // (trong loop) nuốt return
> try { throw new IOException("gốc"); }
> finally { throw new IllegalStateException("trong finally"); } // caller chỉ thấy IllegalStateException
> ```
> Trường hợp thứ ba đặc biệt tệ: khác với try-with-resources (exception `close()` được **đính kèm** làm *suppressed* — mục 4), exception trong `finally` viết tay **thay thế hoàn toàn** exception gốc. Không bao giờ `return`/`break`/`continue`/`throw` trong `finally`.

> `finally` chạy *sau* khi biểu thức `return` đã được tính. Nếu `return obj;` rồi `finally` **sửa field của `obj`** — thay đổi đó **có** hiệu lực (cùng tham chiếu). Nhưng `finally` **gán lại biến local** thì không ảnh hưởng giá trị đã trả.

---

## 4. try-with-resources & suppressed exception

Tự động đóng tài nguyên (file, connection, stream, lock...) — không cần `finally` thủ công.

### Cũ (trước Java 7) vs mới

```java
// CŨ — dài, dễ quên, close() lại phải try/catch lồng nhau
FileReader r = null;
try { r = new FileReader("data.txt"); /* ... */ }
finally { if (r != null) try { r.close(); } catch (IOException ignored) {} }

// MỚI — Java 7+
try (FileReader r = new FileReader("data.txt")) {
    // dùng r
}   // r.close() tự gọi khi thoát khối — kể cả khi có exception hoặc return sớm
```

### Điều kiện: implement `AutoCloseable`

```java
public class Report implements AutoCloseable {
    public void render() { /* ... */ }
    @Override public void close() { System.out.println("dọn dẹp"); }   // được gọi tự động
}
```

| | `AutoCloseable` (Java 7, `java.lang`) | `Closeable` (Java 5, `java.io`) — kế thừa `AutoCloseable` |
|---|---|---|
| `close()` khai báo `throws` | `Exception` | `IOException` |
| Gọi `close()` nhiều lần | Không yêu cầu an toàn | **Yêu cầu** idempotent (gọi lại vô hại) |

### Nhiều resource — đóng theo thứ tự NGƯỢC khai báo (LIFO)

```java
try (var in  = Files.newBufferedReader(Path.of("in.txt"));
     var out = Files.newBufferedWriter(Path.of("out.txt"))) {
    // ...
}   // out.close() TRƯỚC, rồi in.close()
```

Nếu constructor của resource thứ 2 ném exception, resource thứ 1 (đã mở) **vẫn được đóng**.

### Java 9 — resource là biến effectively-final có sẵn

```java
BufferedReader r = Files.newBufferedReader(path);
try (r) { /* ... */ }   // không cần khai báo lại trong ngoặc
```

### Suppressed exception — vì sao try-with-resources hơn `finally` viết tay

```java
class Res implements AutoCloseable {
    public void close() { throw new IllegalStateException("close hỏng"); }
}

try (Res r = new Res()) {
    throw new RuntimeException("body hỏng");
}
// Ném: RuntimeException("body hỏng")
//   ... stack trace ...
//   Suppressed: java.lang.IllegalStateException: close hỏng   ← ĐÍNH KÈM, không mất
```

Exception từ body **được ưu tiên ném ra**; exception từ `close()` được gắn vào qua `addSuppressed()` và hiện trong stack trace dưới nhãn `Suppressed:`. Lấy lại bằng `e.getSuppressed()`.

> **Ứng dụng thực tế:** `Connection`, `Statement`, `ResultSet`, `InputStream`, `Files.lines()`, `Lock` (qua wrapper) đều `AutoCloseable`. **Luôn** dùng try-with-resources để tránh **connection leak** / **file handle leak** — lỗi khiến pool cạn dần rồi service "treo" sau vài giờ chạy.

---

## 5. Custom Exception

Định nghĩa exception riêng cho từng loại lỗi nghiệp vụ → code xử lý rõ ràng, có ngữ nghĩa.

### Bộ 4 constructor chuẩn (soi theo `Exception`)

```java
public class OrderProcessingException extends RuntimeException {   // RuntimeException → unchecked
    private static final long serialVersionUID = 1L;               // Throwable là Serializable → nên khai báo

    public OrderProcessingException() { }
    public OrderProcessingException(String message) { super(message); }
    public OrderProcessingException(String message, Throwable cause) { super(message, cause); }
    public OrderProcessingException(Throwable cause) { super(cause); }
}
```

Có đủ 4 dạng để mọi kiểu wrap/chaining (mục 6) đều dùng được.

### Mang theo dữ liệu có cấu trúc (immutable)

```java
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super("%s không tìm thấy với id: %s".formatted(resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }
    public String getResourceName() { return resourceName; }
    public Object getResourceId()   { return resourceId; }
}
throw new ResourceNotFoundException("User", 42L);   // message: "User không tìm thấy với id: 42"
```

### Đừng tạo custom khi đã có exception chuẩn phù hợp

| Tình huống | Dùng sẵn |
|---|---|
| Tham số method sai giá trị | `IllegalArgumentException` |
| Object đang ở trạng thái không cho phép thao tác này | `IllegalStateException` |
| Thao tác không được hỗ trợ (collection bất biến...) | `UnsupportedOperationException` |
| Không còn phần tử để lấy | `NoSuchElementException` |
| Tham số bắt buộc bị `null` | `NullPointerException` qua `Objects.requireNonNull(x, "x")` |

### Checked hay unchecked?

- **Unchecked** (kế thừa `RuntimeException`) — mặc định cho code backend hiện đại: lỗi validate, "không tìm thấy", vi phạm quy tắc nghiệp vụ. Xử lý tập trung bằng `@ControllerAdvice` (Module 13).
- **Checked** (kế thừa `Exception`) — chỉ khi caller **thật sự** phải có phương án xử lý ngay tại chỗ và bạn muốn compiler ép điều đó.

### Exception "tín hiệu" tần suất cao — tắt stack trace (nâng cao, hiếm)

Chi phí lớn nhất của việc *ném* exception là `fillInStackTrace()` (chụp toàn bộ ngăn xếp). Nếu một exception được dùng như tín hiệu điều khiển nội bộ, ném hàng nghìn lần/giây:

```java
protected ParseSignal(String m) { super(m, null, /*enableSuppression*/ false, /*writableStackTrace*/ false); }
```

`writableStackTrace = false` → không chụp stack → ném gần như miễn phí. Chỉ làm khi đã đo và chứng minh cần.

---

## 6. Exception Chaining & Exception Translation

Khi một exception xảy ra **do** một exception khác, phải "gắn" nguyên nhân gốc vào thay vì làm mất nó.

### Sai — nuốt nguyên nhân gốc

```java
try { readConfigFile(); }
catch (IOException e) {
    throw new RuntimeException("Không thể load cấu hình");   // ❌ mất IOException — không biết vì sao
}
```

### Đúng — truyền `cause` (tham số thứ 2)

```java
try { readConfigFile(); }
catch (IOException e) {
    throw new ConfigLoadException("Không thể load cấu hình", e);   // ✅ giữ toàn bộ chuỗi nguyên nhân
}
```

```java
catch (ConfigLoadException e) {
    Throwable root = e;
    while (root.getCause() != null) root = root.getCause();   // lần tới nguyên nhân gốc cùng
    // stack trace in đầy đủ:
    // ConfigLoadException: Không thể load cấu hình
    //   ...
    // Caused by: java.nio.file.NoSuchFileException: /etc/app/config.yml
}
```

### Exception Translation — mỗi tầng ném exception hợp với tầng của nó

> Tầng cao **không nên** để lộ exception của tầng thấp. Repository bắt `SQLException` → ném `DataAccessException`; service bắt cái đó → ném `OrderProcessingException`. Mỗi lần dịch, **giữ `cause`** để không mất dấu vết.

Điều này giúp:
- Caller không phải `import java.sql.SQLException` chỉ để gọi một service nghiệp vụ.
- Đổi công nghệ tầng dưới (JDBC → JPA) không làm vỡ chữ ký API tầng trên.

### Đừng "log rồi ném lại"

```java
catch (IOException e) {
    log.error("lỗi đọc file", e);   // ❌ nếu đã ném lại...
    throw new ConfigLoadException("...", e);
}
```

→ tầng trên bắt và log lần nữa → **cùng một lỗi xuất hiện 2–3 lần** trong log, gây nhiễu. Quy tắc: **hoặc log, hoặc ném — không cả hai**. Nơi cuối cùng thực sự xử lý mới log.

---

## 7. Best Practice — nên và không nên

### ❌ Nuốt exception (swallowing)

```java
try { riskyOperation(); }
catch (Exception e) { }   // lỗi biến mất, chương trình chạy tiếp như không có gì
```

Anti-pattern **nguy hiểm nhất**: bug lộ ra rất xa nguồn gốc (dữ liệu sai, mất tiền, crash chỗ khác). Nếu *cố tình* bỏ qua, phải ghi rõ lý do: `catch (XxxException ignored) { /* giải thích vì sao an toàn */ }`.

### ❌ Bắt `Exception`/`Throwable` quá rộng

```java
catch (Exception e) { log.error("có lỗi"); }   // che cả bug lập trình lẫn lỗi nghiệp vụ hợp lệ
```

Chỉ `catch` đúng loại mình **biết cách xử lý**. Không bao giờ `catch (Throwable)` hay `catch (Error)`.

### ❌ Dùng exception cho luồng điều khiển bình thường

```java
try {
    for (int i = 0; ; i++) process(data[i]);       // dựa vào ArrayIndexOutOfBounds để dừng
} catch (ArrayIndexOutOfBoundsException e) { }
```

Chi phí `fillInStackTrace()` mỗi lần ném là đáng kể. Exception chỉ cho tình huống **thực sự bất thường**.

### ❌ `catch (NullPointerException)` để "xử lý null"

Sửa nguyên nhân null (guard clause, `Optional`, `Objects.requireNonNull` sớm), đừng bắt NPE.

### ✅ Tổng hợp

1. **Fail fast:** kiểm tra tham số đầu method bằng `Objects.requireNonNull` / guard clause + `IllegalArgumentException`. Lỗi lộ ngay tại nguồn.
2. **Throw early, catch late:** ném ngay khi phát hiện; chỉ bắt ở tầng thực sự quyết định được phải làm gì (thường là biên: controller, message listener, `main`).
3. **Log hoặc ném, không cả hai.**
4. **Luôn Exception Chaining** khi wrap — không mất `cause`.
5. **Custom exception tên rõ nghĩa nghiệp vụ** (`InsufficientBalanceException`, không `MyException`).
6. **Luôn try-with-resources** cho mọi thứ `AutoCloseable`.
7. **`@throws` trong Javadoc** cho cả checked lẫn unchecked exception quan trọng mà caller cần biết.
8. **`assert`** dùng cho *bất biến nội bộ* mà bạn tin chắc đúng (tắt mặc định, bật bằng `-ea` khi test) — **không** thay cho việc kiểm tra input từ ngoài.

---

## 8. File I/O với `java.nio.file`

NIO.2 (Java 7+) — gọn và rõ hơn `java.io.File` cũ.

### `Path` — chỉ là đường dẫn, chưa chạm đĩa

```java
Path base = Path.of("/var/app");                 // Java 11+ (thay Paths.get(...))
Path cfg  = base.resolve("config/app.yml");       // /var/app/config/app.yml
base.relativize(cfg);                             // config/app.yml
Path.of("/a/b/../c").normalize();                 // /a/c
cfg.getParent();  cfg.getFileName();  cfg.toAbsolutePath();
Path p = file.toPath();  File f = path.toFile();  // interop với java.io.File
```

### Đọc / ghi ngắn gọn — LUÔN chỉ định charset

```java
String s = Files.readString(path, StandardCharsets.UTF_8);
List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
byte[] bytes = Files.readAllBytes(path);

Files.writeString(path, "nội dung", StandardCharsets.UTF_8);
Files.write(path, List.of("d1", "d2"), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
```

> Từ **Java 18**, charset mặc định của `Files`/`String` là **UTF-8** không phụ thuộc OS. Trên JDK cũ hơn, mặc định là *platform default* (Windows hay ra `windows-1252`) → file đọc trên máy dev đúng nhưng lỗi ký tự trên server. **Luôn truyền `StandardCharsets.UTF_8` tường minh** để code chạy đúng trên mọi JDK/OS.

### `StandardOpenOption`

| Option | Ý nghĩa |
|---|---|
| `CREATE` | Tạo nếu chưa có; ghi đè/nối tùy option khác |
| `CREATE_NEW` | Tạo mới; **lỗi** `FileAlreadyExistsException` nếu đã tồn tại (chống ghi đè) |
| `APPEND` | Ghi nối vào cuối |
| `TRUNCATE_EXISTING` | Xóa nội dung cũ trước khi ghi (mặc định của `write` khi có `WRITE`) |
| `WRITE` / `READ` | Mở để ghi / đọc |

### Thao tác file/thư mục

```java
Files.exists(path);   Files.isRegularFile(path);   Files.isDirectory(path);
Files.size(path);      Files.getLastModifiedTime(path);   Files.probeContentType(path);
Files.createDirectories(Path.of("logs/2026"));            // tạo cả thư mục cha
Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE);     // đổi tên nguyên tử (nếu FS hỗ trợ)
Files.delete(path);          // NoSuchFileException nếu không tồn tại
Files.deleteIfExists(path);  // không ném nếu không tồn tại
```

### Đọc file lớn & duyệt cây thư mục — Stream, BẮT BUỘC try-with-resources

```java
try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
    long errors = lines.filter(l -> l.contains("ERROR")).count();
}   // Stream giữ 1 file handle → phải đóng

try (Stream<Path> tree = Files.walk(root)) {
    tree.filter(Files::isRegularFile)
        .filter(p -> p.toString().endsWith(".log"))
        .forEach(System.out::println);
}
```

> `Files.readAllLines()` nạp **toàn bộ** file vào RAM → file log vài GB gây `OutOfMemoryError`. `Files.lines()` đọc **lazy từng dòng**. Các API trả `Stream` (`lines`, `walk`, `list`, `find`, `newDirectoryStream`) đều giữ tài nguyên hệ điều hành → phải trong try-with-resources.

---

## 9. `java.io` — luồng byte vs luồng ký tự

Vẫn cần khi API cấp thấp (nén, mã hóa, socket, thư viện cũ) trả về `InputStream`/`OutputStream`.

| | Luồng **byte** | Luồng **ký tự** |
|---|---|---|
| Lớp gốc | `InputStream` / `OutputStream` | `Reader` / `Writer` |
| Đơn vị | byte thô (ảnh, PDF, zip, dữ liệu nhị phân) | ký tự (văn bản) — cần **charset** để chuyển byte ↔ char |
| Cầu nối | | `InputStreamReader` / `OutputStreamWriter` (byte→char kèm charset) |

### Pattern Decorator — bọc từng lớp thêm một tính năng

```java
try (BufferedReader in = new BufferedReader(
        new InputStreamReader(
            new FileInputStream("data.txt"), StandardCharsets.UTF_8))) {
    in.lines().forEach(System.out::println);
}
```

- `FileInputStream` — đọc byte thô từ file.
- `InputStreamReader` — giải mã byte → ký tự theo UTF-8.
- `BufferedReader` — đệm để giảm số lần gọi hệ điều hành, thêm `readLine()`/`lines()`.

`java.nio.file` đã bọc sẵn: `Files.newBufferedReader(path, charset)` = ba lớp trên gộp lại.

> `System.out` / `System.err` là `PrintStream` (luồng byte); `System.in` là `InputStream`.

---

## 10. Serialization

Chuyển object trong RAM ↔ chuỗi byte để lưu/truyền.

```java
public class User implements Serializable {          // marker interface — không có method
    private static final long serialVersionUID = 1L; // ID phiên bản class — khai báo tường minh

    private String username;
    private transient String password;               // transient → KHÔNG serialize (bảo mật / dữ liệu tạm)
    private static String appName = "MyApp";          // static → KHÔNG serialize (thuộc class, không thuộc object)
}
```

```java
try (var out = new ObjectOutputStream(Files.newOutputStream(Path.of("user.dat")))) {
    out.writeObject(new User("pho", "secret"));
}
try (var in = new ObjectInputStream(Files.newInputStream(Path.of("user.dat")))) {
    User u = (User) in.readObject();   // readObject() trả Object → ép kiểu; ném ClassNotFoundException nếu class thiếu
}
```

### Ba điều bắt buộc nhớ

| Vấn đề | Chi tiết |
|---|---|
| **`serialVersionUID`** | Nếu không khai báo, compiler tự sinh theo cấu trúc class; thêm/bớt field → UID đổi → đọc file cũ ném `InvalidClassException`. Khai báo tường minh `= 1L` để **bạn** kiểm soát khi nào coi là "không tương thích". |
| **Bỏ qua constructor** | `readObject()` dựng object **không gọi constructor** → mọi kiểm tra bất biến trong constructor bị bỏ qua. Cần validate lại trong `readObject` tự viết. |
| **Rủi ro bảo mật** | `readObject()` trên **dữ liệu không tin cậy** = một trong những lớp lỗ hổng RCE nghiêm trọng nhất của Java (gadget chain). Java 9+ có `ObjectInputFilter` để chỉ cho phép danh sách class an toàn: `ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter("com.myapp.*;java.base/*;!*"));` |

### `transient` + `writeObject`/`readObject` tùy biến

```java
private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeObject(encrypt(password));   // serialize password ở dạng đã mã hóa
}
private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.password = decrypt((String) in.readObject());
}
```

`record` được serialize **an toàn hơn**: qua canonical constructor → chạy validation, không bị bypass.

> **Thực tế backend:** Java Serialization hầu như **không dùng để trao đổi dữ liệu** nữa — REST API dùng **JSON** (Jackson — Module 16). `Serializable`/`transient`/`serialVersionUID` vẫn gặp ở session của Spring Security, cache phân tán (Hazelcast/Redis với Java serializer), và RMI cũ.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| `Throwable` | Cha của `Error` (JVM, không bắt) và `Exception`. `getCause`/`getSuppressed`/`getStackTrace`. |
| Checked | Compiler ép `catch`/`throws`. Điều kiện ngoài, caller phục hồi được. `IOException`, `SQLException`. |
| Unchecked | `RuntimeException`. Không ép. Bug lập trình. `NPE`, `IllegalArgument/StateException`. |
| `InterruptedException` | Khi catch: `Thread.currentThread().interrupt()` khôi phục cờ — không nuốt. |
| Thứ tự `catch` | Con trước cha; multi-catch `A \| B` (biến ngầm `final`); precise rethrow giữ kiểu hẹp. |
| `finally` | Luôn chạy (trừ `System.exit`). **Không** `return`/`break`/`continue`/`throw` trong `finally` — nuốt kết quả/exception của `try`. |
| try-with-resources | Tự `close()` theo thứ tự **ngược khai báo**. Cần `AutoCloseable`. Exception `close()` thành **suppressed**, không che exception body. |
| `AutoCloseable` vs `Closeable` | `Closeable.close()` throws `IOException` + phải idempotent; `AutoCloseable` throws `Exception`. |
| Custom Exception | 4 constructor chuẩn + `serialVersionUID`; mang dữ liệu immutable; tái dùng exception chuẩn khi hợp; unchecked là mặc định backend. |
| Exception Chaining | Luôn truyền `cause` (`new X(msg, e)`). Lần tới root cause qua `getCause()`. |
| Exception Translation | Mỗi tầng ném exception hợp tầng mình, giữ `cause`. Tầng cao không lộ `SQLException`. |
| Anti-pattern | Nuốt exception; `catch (Exception/Throwable)`; exception cho flow control; log-và-ném-lại (log 2 lần). |
| Fail fast | `Objects.requireNonNull` + guard clause đầu method → lỗi lộ tại nguồn. |
| `java.nio.file` | `Path.of`; `Files.readString/writeString/lines/walk`; **luôn truyền `StandardCharsets.UTF_8`**; `StandardOpenOption`; API trả `Stream` phải try-with-resources. |
| `readAllLines` vs `lines` | `lines()` lazy (file lớn); `readAllLines()` nạp hết RAM. |
| `java.io` | byte (`InputStream`/`OutputStream`) vs ký tự (`Reader`/`Writer`, cần charset). Decorator: `Buffered(InputStreamReader(FileInputStream))`. |
| Serialization | `Serializable` marker; `transient`/`static` không serialize; `serialVersionUID` lệch → `InvalidClassException`; `readObject` bỏ qua constructor; **RCE nếu dữ liệu không tin cậy** → `ObjectInputFilter`. Thực tế: dùng JSON. |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** In ra gì, theo thứ tự nào?
```java
static int test() {
    try { System.out.println("A"); return 1; }
    finally { System.out.println("B"); }
}
// main: System.out.println("Kết quả: " + test());
```

**Câu 2.** Có compile không? Vì sao? Nêu hai cách sửa.
```java
public void readData() { var reader = new FileReader("data.txt"); }
```

**Câu 3.** Đoạn này trả về gì? Vấn đề thiết kế là gì?
```java
static int f() {
    try { return 1; }
    finally { return 2; }
}
```

**Câu 4.** `e.getCause()` in ra exception nào? Còn `e.getSuppressed()` khác `getCause()` ở điểm nào về mặt khái niệm?
```java
try {
    try { int x = 5 / 0; }
    catch (ArithmeticException ex) { throw new IllegalStateException("Lỗi tính toán", ex); }
} catch (IllegalStateException e) { System.out.println(e.getCause()); }
```

**Câu 5.** Với hai resource, thứ tự các dòng in là gì khi body ném exception giữa chừng? Exception nào ra tới caller, exception nào thành suppressed?
```java
class R implements AutoCloseable {
    final String n; R(String n){ this.n=n; System.out.println("mở "+n); }
    public void close(){ System.out.println("đóng "+n); throw new RuntimeException("close "+n+" hỏng"); }
}
try (R a = new R("A"); R b = new R("B")) { throw new RuntimeException("body hỏng"); }
```

**Câu 6.** Đoạn này bắt `InterruptedException` rồi chỉ `log.warn(...)`. Nêu vấn đề và cách sửa đúng.
```java
try { Thread.sleep(500); }
catch (InterruptedException e) { log.warn("bị gián đoạn", e); }
```

**Câu 7.** Vì sao đọc file bằng `Files.readAllLines(path)` (không truyền charset) chạy đúng trên máy dev Windows nhưng lỗi ký tự tiếng Việt trên server Linux JDK 11? Sửa thế nào?

**Câu 8.** `try (Stream<String> s = Files.lines(path)) { ... }` — vì sao **bắt buộc** khối try-with-resources ở đây, trong khi `list.stream()` thì không cần?

---

### Phần B — Bài tập viết code

**Bài 1 — Custom Exception cho ngân hàng.**
`InsufficientBalanceException extends RuntimeException` với field `final double shortfall` (số tiền thiếu), đủ 4 constructor. `BankAccount.withdraw(double amount)` ném exception này khi thiếu, message tự tính rõ số thiếu. `main`: bắt exception, in thông báo thân thiện cho người dùng **và** log riêng `getShortfall()` cho nội bộ.

**Bài 2 — Exception Chaining + Translation khi đọc config.**
`Properties loadAppConfig(String path)` — đọc file `.properties` bằng `Files.newInputStream` + `Properties.load` trong try-with-resources. Bắt `IOException` gốc, ném lại `ConfigLoadException` (custom, unchecked) với message rõ ràng, **giữ cause**. `main`: đọc file không tồn tại, in `getCause()` và lần tới root cause để chứng minh không mất thông tin.

**Bài 3 — try-with-resources với custom `AutoCloseable` + suppressed.**
`DatabaseConnection` và `FileLock` cùng implement `AutoCloseable` (constructor + `close()` in log; `close()` của `FileLock` **cố tình ném** exception). Dùng cả hai trong một khối try-with-resources, body ném `RuntimeException`. In ra: thứ tự đóng (ngược khai báo), exception nào ra caller, `e.getSuppressed()` chứa gì.

**Bài 4 — Xử lý file log lớn bằng `Files.lines()`.**
Tạo `app.log` ≥ 1000 dòng (bằng code, `Random` trộn `INFO`/`WARN`/`ERROR`). Dùng `Files.lines()` (không `readAllLines`) + try-with-resources để: đếm số dòng mỗi level (`Collectors.groupingBy` + `counting` — Module 03.3); in 5 dòng `ERROR` đầu tiên; tìm dòng `ERROR` **cuối cùng**. Truyền `StandardCharsets.UTF_8` tường minh.

**Bài 5 — Duyệt cây thư mục bằng `Files.walk()`.**
Viết `Map<String, Long> countBytesByExtension(Path root)` — duyệt toàn bộ cây thư mục, với mỗi file thường lấy phần mở rộng (`.java`, `.md`, `.txt`...) và cộng dồn `Files.size()`. Trả `Map` sắp giảm dần theo tổng byte. Dùng try-with-resources cho `Stream<Path>`, bắt `IOException` của `Files.size` từng file (file có thể bị xóa giữa chừng) và bỏ qua file đó thay vì làm hỏng cả pipeline.

**Bài 6 — Hệ thống validate nhiều tầng + custom exception.**
`UserRegistrationService.register(String username, String email, String password)` kiểm tra **theo thứ tự**, ném đúng exception đầu tiên gặp:
- `InvalidEmailException` — email không chứa `@` hoặc không có `.` sau `@`.
- `WeakPasswordException` — độ dài < 8.
- `DuplicateUserException` — `username` đã có trong `Set<String>` đăng ký.
Cả ba unchecked, kế thừa một lớp cha chung `RegistrationException`. `main`: thử nhiều bộ dữ liệu (hợp lệ / email sai / mật khẩu yếu / trùng), dùng multi-catch hoặc `catch (RegistrationException e)` để in thông báo phù hợp.

**Bài 7 — Serialization an toàn.**
`record Account(String owner, long balanceCents) implements Serializable` với `serialVersionUID`. Ghi một `Account` ra file rồi đọc lại. Sau đó: (a) sửa `record` thêm một component, chạy lại phần đọc file cũ → quan sát `InvalidClassException`; (b) viết lại phần đọc có `ObjectInputFilter` chỉ cho phép `java.base/*` và package của bạn, thử nạp file và giải thích lớp bảo vệ này chặn gì.

---

### Phần C — Nâng cao

**Câu 1.** Phân biệt chính xác ba trường hợp exception "biến mất": (a) `catch` rỗng, (b) `throw` trong `finally`, (c) `close()` ném exception trong try-with-resources. Trường hợp nào giữ lại được thông tin, giữ ở đâu, lấy lại bằng gì?

**Câu 2.** `finally` chạy *sau* khi biểu thức `return` được tính nhưng *trước* khi giá trị thực sự trả về. Viết một ví dụ mà `finally` **sửa được** kết quả trả về (qua tham chiếu object) và một ví dụ mà `finally` **không** sửa được (gán lại biến local). Giải thích sự khác nhau.

**Câu 3.** Chi phí runtime của việc *ném* một exception chủ yếu nằm ở đâu? Constructor `Throwable(String, Throwable, boolean, boolean)` với `writableStackTrace = false` giải quyết gì? Khi nào việc dùng nó là hợp lý, khi nào là tối ưu hóa sớm nguy hiểm?

**Câu 4.** "Exception translation" (Effective Java Item 73): vì sao một repository **không nên** để `SQLException` lọt lên service? Nêu hai hệ quả kiến trúc cụ thể nếu vi phạm, và cách `DataAccessException` của Spring giải quyết. Việc dịch exception có làm mất thông tin debug không — vì sao?

**Câu 5.** `readObject()` dựng object **không qua constructor**. Cho một class có bất biến "balance ≥ 0" được đảm bảo trong constructor. Mô tả cách kẻ tấn công tạo file serialized khiến `balance = -1000` sau khi deserialize, và hai cách phòng: validate trong `readObject` tự viết, hoặc chuyển sang `record`. Vì sao `record` an toàn hơn?

**Câu 6.** Charset mặc định: trước Java 18 `Files.readString(path)` dùng gì, từ Java 18 dùng gì? Viết một tình huống bug production cụ thể do dựa vào charset mặc định (file sinh ở tầng này, đọc ở tầng khác). Vì sao `Files.write` không truyền charset cũng nguy hiểm tương tự?

**Câu 7.** So sánh `Files.lines()`, `BufferedReader.lines()`, và `Files.readAllLines()` về: bộ nhớ, việc giữ file handle, khả năng xử lý file đang được ghi tiếp, và hành vi khi gặp dòng lỗi encoding giữa file. Trường hợp nào bắt buộc try-with-resources và vì sao?

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. `A`, `B`, `Kết quả: 1`. `finally` in `B` chạy trước khi giá trị `1` (đã tính từ `try`) được trả về; vì `finally` không có `return` riêng nên `1` được giữ.
2. **Không compile.** Constructor `FileReader` ném `FileNotFoundException` (checked, con của `IOException`), method không `throws` cũng không `try/catch`. Sửa: (a) `public void readData() throws IOException { ... }`; (b) bọc `try (var reader = new FileReader("data.txt")) { ... } catch (IOException e) { ... }`.
3. Trả về `2`. `return 2` trong `finally` **ghi đè** `return 1` của `try` — `1` mất hoàn toàn. Anti-pattern: không bao giờ `return` trong `finally`.
4. `getCause()` in `java.lang.ArithmeticException: / by zero` — đúng object gốc truyền vào constructor thứ 2. Khác biệt khái niệm: `getCause()` = **nguyên nhân** (A xảy ra *vì* B); `getSuppressed()` = các exception **phụ** xảy ra trong lúc dọn dẹp (`close()`) khi đã có một exception chính đang lan truyền — chúng "song song", không phải quan hệ nhân quả.
5. `mở A` → `mở B` → (body ném) → `đóng B` → `đóng A`. Ra caller: `RuntimeException("body hỏng")`. Suppressed: `RuntimeException("close B hỏng")` **và** `RuntimeException("close A hỏng")` (theo thứ tự đóng). `close()` được gọi cho cả hai dù chúng đều ném.
6. Vấn đề: **nuốt trạng thái interrupt** — `Thread.sleep` khi bị interrupt đã xóa cờ; nếu chỉ log, tầng trên (vòng lặp worker, thread pool) không biết luồng đã được yêu cầu dừng → luồng chạy tiếp. Sửa: thêm `Thread.currentThread().interrupt();` trong catch, và/hoặc ném lại (`throw new SomeRuntimeException(e)`) để thoát công việc hiện tại.
7. `readAllLines(path)` không charset → dùng **charset mặc định của JVM**. JDK 11 trên Windows dev thường là `windows-1252`/`UTF-8` khớp file; trên Linux server locale khác (hoặc `POSIX` → `US-ASCII`/`ISO-8859-1`) → ký tự tiếng Việt (nhiều byte UTF-8) bị giải mã sai. Sửa: `Files.readAllLines(path, StandardCharsets.UTF_8)` — luôn tường minh.
8. `Files.lines()` mở một **file handle của hệ điều hành** và giữ tới khi Stream được `close()`; không đóng → rò rỉ handle, hết hạn ngạch, file bị khóa (Windows). `list.stream()` chỉ duyệt cấu trúc dữ liệu trong RAM, không có tài nguyên OS nào cần trả. Các `Stream` từ `Files.*` (`lines`/`walk`/`list`/`find`) đều `AutoCloseable` và phải nằm trong try-with-resources.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `this.shortfall = amount - balance;` trong constructor exception; message `"Số dư không đủ, còn thiếu %,.0f₫".formatted(shortfall)`. `main`: `catch (InsufficientBalanceException e) { showUser("Giao dịch thất bại: số dư không đủ."); log.warn("shortfall={}", e.getShortfall()); }`.
- **Bài 2:** `try (var in = Files.newInputStream(Path.of(path))) { var p = new Properties(); p.load(in); return p; } catch (IOException e) { throw new ConfigLoadException("Không đọc được config: " + path, e); }`. `main`: bắt `ConfigLoadException`, `while (root.getCause() != null) root = root.getCause();` → root là `NoSuchFileException`.
- **Bài 3:** Đóng theo thứ tự ngược: `FileLock.close()` trước → ném → `DatabaseConnection.close()` vẫn chạy. Ra caller: `RuntimeException` của body. `e.getSuppressed()[0]` = exception của `FileLock.close()`.
- **Bài 4:** `try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) { Map<String,Long> byLevel = lines.map(Bai4::level).collect(groupingBy(x -> x, counting())); }`. Dòng ERROR cuối: `Files.lines(...).filter(l -> l.contains("ERROR")).reduce((a,b) -> b)` → `Optional`.
- **Bài 5:** `try (Stream<Path> s = Files.walk(root)) { return s.filter(Files::isRegularFile).collect(groupingBy(Bai5::ext, summingLong(Bai5::sizeQuiet))); }` với `sizeQuiet` bọc `Files.size` trong try/catch trả `0L` khi lỗi. Sắp giảm dần → đổ vào `LinkedHashMap` (Module 03.3).
- **Bài 6:** `sealed class RegistrationException permits ...` hoặc lớp cha thường; mỗi lớp con 1 message mặc định. `register` kiểm tra tuần tự, `throw` ngay khi sai. `main`: `catch (RegistrationException e) { System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage()); }`.
- **Bài 7:** (a) Thêm component → `serialVersionUID` khai báo vẫn `= 1L` nhưng cấu trúc lệch → khi đọc: nếu giữ nguyên UID thì `record` deserialize gọi canonical constructor với field thiếu → tùy JDK có thể `InvalidClassException` hoặc gán mặc định; nếu bỏ UID thì UID tự sinh đổi → chắc chắn `InvalidClassException`. (b) `ObjectInputFilter.Config.createFilter("java.base/*;com.myapp.**;!*")` — chặn mọi class ngoài allowlist trước khi nó được khởi tạo → vô hiệu hóa gadget chain.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. (a) `catch` rỗng — exception mất **hoàn toàn**, không lấy lại được, chỉ còn hậu quả gián tiếp. (b) `throw` trong `finally` — exception của `try` bị **thay thế**; JLS quy định giá trị/exception "đột ngột" của `finally` thắng; không có cơ chế nào giữ lại exception gốc → mất. (c) try-with-resources — exception body **được ném ra**, exception `close()` **được giữ** qua `addSuppressed()`, lấy lại bằng `caughtException.getSuppressed()`. Chỉ (c) an toàn.
2. Sửa được: `List<String> f() { List<String> r = new ArrayList<>(); try { r.add("x"); return r; } finally { r.add("y"); } }` → trả `[x, y]` (cùng object). Không sửa được: `int g() { int v = 1; try { return v; } finally { v = 99; } }` → trả `1` (giá trị `1` đã được sao chép ra "thanh ghi trả về" trước khi `finally` chạy; gán lại biến local `v` không đụng tới bản sao đó).
3. Chi phí chính: `fillInStackTrace()` — duyệt và chụp toàn bộ khung ngăn xếp hiện tại (native, tỉ lệ với độ sâu stack). `writableStackTrace = false` → bỏ hẳn bước này, ném exception gần như bằng chi phí tạo một object thường. Hợp lý khi: exception là **tín hiệu điều khiển nội bộ** ném rất thường xuyên và **không ai đọc stack trace của nó** (ví dụ thoát sớm khỏi parser đệ quy). Nguy hiểm khi: exception có thể lọt ra log/monitoring — mất stack trace khiến không debug được; hoặc khi chưa đo mà đã "tối ưu" — phần lớn code không ném đủ nhiều để đáng.
4. (i) Caller phải `import java.sql.SQLException` và biết chi tiết JDBC chỉ để gọi một API nghiệp vụ — rò rỉ chi tiết cài đặt qua tầng. (ii) Đổi tầng persistence (JDBC → JPA → MongoDB) làm đổi loại exception → vỡ mọi `catch` ở tầng trên. `DataAccessException` (unchecked, phân loại lại: `DuplicateKeyException`, `OptimisticLockingFailureException`...) cho phép tầng trên bắt theo *ngữ nghĩa* độc lập công nghệ. Không mất debug: `SQLException` gốc luôn được giữ làm `cause`, stack trace đầy đủ vẫn in dưới `Caused by:`.
5. Kẻ tấn công serialize một object hợp lệ, rồi **sửa byte** trường `balance` trong file (hoặc dùng một class giả cùng `serialVersionUID`), hoặc dựng luồng byte thủ công. `readObject()` mặc định gán thẳng field từ luồng, **không** chạy constructor → `balance = -1000`. Phòng: (a) tự viết `private void readObject(ObjectInputStream in)` gọi `in.defaultReadObject()` rồi `if (balance < 0) throw new InvalidObjectException(...)`. (b) `record` — deserialization của record **bắt buộc đi qua canonical constructor**, nên mọi kiểm tra trong compact constructor được thực thi; không có đường tắt gán field trực tiếp.
6. Trước Java 18: `Files.readString(path)` dùng **`Charset.defaultCharset()`** = phụ thuộc `file.encoding` / locale OS. Từ Java 18 (JEP 400): luôn **UTF-8**. Bug: service A (JDK 17, Linux, locale `en_US.UTF-8`) ghi file CSV có tên tiếng Việt bằng `Files.writeString` (→ UTF-8); batch job B (JDK 17, Windows, `file.encoding=windows-1252`) đọc bằng `Files.readString` → tên hỏng thành ký tự lạ, đối chiếu dữ liệu sai. `Files.write(...)` không charset cũng vậy — ghi ra bằng charset mặc định máy ghi, máy đọc khác charset → hỏng; luôn truyền `StandardCharsets.UTF_8` cả hai chiều.
7. `Files.readAllLines()` — O(kích thước file) RAM, không giữ handle sau khi trả (đọc xong đóng ngay), không dùng được cho file rất lớn. `Files.lines()` — lazy, RAM ~O(1 dòng), **giữ handle tới khi Stream đóng** → bắt buộc try-with-resources. `BufferedReader.lines()` — lazy, nhưng **bạn** sở hữu `BufferedReader` nên phải tự đóng nó (thường cũng try-with-resources); đọc được file đang ghi tiếp (đọc tới EOF hiện tại). Dòng lỗi encoding: cả ba ném `MalformedInputException`/`UncheckedIOException` khi *chạm* tới dòng đó — với `lines()`/`BufferedReader.lines()` lỗi bật ra giữa chừng pipeline (đã xử lý một phần), với `readAllLines()` lỗi bật ra trước khi trả bất cứ dòng nào.

</details>

---

*File tiếp theo trong lộ trình: **Module 05.1 — Thread cơ bản** (Thread vs Runnable, thread lifecycle, synchronized, volatile, race condition, deadlock).*
