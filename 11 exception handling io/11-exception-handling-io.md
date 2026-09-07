# Module 04 — Exception Handling & I/O

> **Mức độ ưu tiên: Cao** — Backend chuyên nghiệp = xử lý lỗi tử tế, không để 1 exception không bắt được làm sập cả service, ảnh hưởng đến hàng nghìn request khác đang chạy. Đây cũng là nền tảng bắt buộc để hiểu cơ chế `@ControllerAdvice`/`@ExceptionHandler` sẽ học ở Module 13 (Spring Boot) — nơi Exception Handling được nâng lên tầm kiến trúc toàn hệ thống.

---

## Mục lục

1. [Exception Hierarchy — cây phân cấp Throwable](#1-exception-hierarchy--cây-phân-cấp-throwable)
2. [Checked vs Unchecked Exception](#2-checked-vs-unchecked-exception)
3. [try / catch / finally](#3-try--catch--finally)
4. [try-with-resources](#4-try-with-resources)
5. [Custom Exception](#5-custom-exception)
6. [Exception Chaining](#6-exception-chaining)
7. [Best Practice — những điều NÊN và KHÔNG NÊN làm](#7-best-practice--những-điều-nên-và-không-nên-làm)
8. [File I/O cơ bản với `java.nio.file`](#8-file-io-cơ-bản-với-javaniofile)
9. [Serialization cơ bản](#9-serialization-cơ-bản)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Exception Hierarchy — cây phân cấp Throwable

```
                        Throwable
                       /          \
                  Error            Exception
             (lỗi nghiêm trọng,      /        \
              KHÔNG nên tự bắt   RuntimeException   (các Exception khác)
              xử lý — ví dụ      (Unchecked)          (Checked)
              OutOfMemoryError,       │                    │
              StackOverflowError)     │                    │
                              NullPointerException    IOException
                              ArrayIndexOutOfBounds    SQLException
                              ClassCastException       FileNotFoundException
                              IllegalArgumentException ParseException
                              ArithmeticException
```

| Nhánh | Đặc điểm |
|---|---|
| `Error` | Lỗi nghiêm trọng ở tầng JVM (hết bộ nhớ, stack tràn...) — **không nên** cố `catch` và xử lý tiếp, vì chương trình thường không thể phục hồi đúng nghĩa |
| `RuntimeException` (và mọi subclass) | **Unchecked Exception** — compiler **không bắt buộc** phải xử lý hoặc khai báo |
| `Exception` (trừ nhánh `RuntimeException`) | **Checked Exception** — compiler **bắt buộc** phải `catch` hoặc khai báo `throws` |

---

## 2. Checked vs Unchecked Exception

### Checked Exception — compiler bắt buộc kiểm tra tại thời điểm biên dịch

```java
public void readFile(String path) throws IOException { // BẮT BUỘC khai báo throws, hoặc catch bên trong
    FileReader reader = new FileReader(path); // FileNotFoundException là Checked — kế thừa IOException
}
```

Nếu không `throws` hoặc `try/catch`, compiler báo lỗi ngay: `"unreported exception IOException; must be caught or declared to be thrown"`.

### Unchecked Exception (RuntimeException) — compiler KHÔNG bắt buộc kiểm tra

```java
public int divide(int a, int b) {
    return a / b; // ArithmeticException (Unchecked) — compiler KHÔNG bắt buộc phải catch hay khai báo throws
}
```

### Bảng so sánh & triết lý thiết kế

| Tiêu chí | Checked Exception | Unchecked Exception |
|---|---|---|
| Compiler kiểm tra | Bắt buộc catch hoặc `throws` | Không bắt buộc |
| Ý nghĩa triết lý | Lỗi **có thể dự đoán trước và nên xử lý** — thường do điều kiện bên ngoài (file không tồn tại, kết nối mạng lỗi, database timeout) | Lỗi **do lập trình sai** (bug logic) — thường không nên "nuốt" (swallow) mà nên sửa code gốc |
| Ví dụ tiêu biểu | `IOException`, `SQLException`, `ClassNotFoundException` | `NullPointerException`, `ArrayIndexOutOfBoundsException`, `IllegalArgumentException` |
| Xu hướng thiết kế hiện đại (Spring) | Nhiều framework hiện đại (Spring, Hibernate) **chuyển sang dùng Unchecked Exception** cho cả lỗi hệ thống — tránh code bị "ép" `try/catch` ở khắp nơi, làm giảm khả năng đọc | — |

> **Lưu ý quan trọng cho tư duy backend hiện đại:** Spring Framework có xu hướng **bọc lại (wrap)** các Checked Exception từ tầng thấp (ví dụ `SQLException`) thành các **Unchecked Exception riêng** (`DataAccessException`) — triết lý là: buộc developer phải `try/catch` **ở mọi nơi gọi database** là quá phiền toái và thường dẫn đến code chỉ `catch` rồi `printStackTrace()` cho có, không thực sự xử lý gì. Đây là lý do khi học đến Spring Data JPA (Module 14), sẽ thấy rất ít code Checked Exception tường minh.

---

## 3. try / catch / finally

```java
public void processFile(String path) {
    try {
        int[] numbers = {1, 2, 3};
        System.out.println(numbers[5]); // ném ArrayIndexOutOfBoundsException
    } catch (ArrayIndexOutOfBoundsException e) {
        System.out.println("Lỗi truy cập mảng: " + e.getMessage());
    } catch (Exception e) { // catch tổng quát hơn — PHẢI đặt SAU catch cụ thể hơn
        System.out.println("Lỗi khác: " + e.getMessage());
    } finally {
        System.out.println("Khối finally LUÔN chạy — dù có exception hay không, dù có return giữa chừng hay không");
    }
}
```

### Quy tắc thứ tự `catch` — từ cụ thể đến tổng quát

```java
try {
    // ...
} catch (FileNotFoundException e) { // cụ thể nhất trước
    // ...
} catch (IOException e) {           // tổng quát hơn sau
    // ...
} catch (Exception e) {              // tổng quát nhất, đặt CUỐI CÙNG
    // ...
}
// catch (IOException e) { } catch (FileNotFoundException e) { } // ❌ Lỗi compile — "exception has already been caught" vì FileNotFoundException là subclass của IOException, đặt SAU sẽ không bao giờ được chạm tới
```

### Multi-catch (Java 7+) — bắt nhiều loại exception cùng xử lý giống nhau

```java
try {
    // ...
} catch (IllegalArgumentException | NullPointerException e) { // dùng | để gộp, tránh lặp code
    System.out.println("Lỗi tham số: " + e.getMessage());
}
```

### `finally` chạy khi nào? (kể cả trường hợp đặc biệt)

```java
public int test() {
    try {
        return 1;
    } finally {
        System.out.println("finally vẫn chạy TRƯỚC KHI method thực sự return"); // luôn in ra
    }
}
```
`finally` **luôn chạy**, kể cả khi có `return` trong `try`/`catch` — ngoại lệ **duy nhất** là khi JVM bị tắt đột ngột (`System.exit()`) hoặc thread bị kill.

> ⚠️ **Bẫy hiếm gặp nhưng hay bị hỏi:** nếu `finally` cũng có `return`, nó sẽ **ghi đè** giá trị `return` trong `try`:
> ```java
> public int badExample() {
>     try {
>         return 1;
>     } finally {
>         return 2; // ❌ Anti-pattern — GHI ĐÈ giá trị return của try, rất dễ gây bug khó hiểu
>     }
> }
> // badExample() luôn trả về 2, giá trị "return 1" ở try bị "nuốt" hoàn toàn
> ```
> **Không bao giờ nên `return` trong `finally`** — đây là code smell kinh điển.

---

## 4. try-with-resources

Dùng để tự động **đóng tài nguyên (resource)** — file, kết nối database, socket... — mà **không cần viết `finally` thủ công** để đóng.

### Cách CŨ — trước Java 7, phải tự đóng thủ công trong `finally`

```java
FileReader reader = null;
try {
    reader = new FileReader("data.txt");
    // xử lý file
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (reader != null) {
        try {
            reader.close(); // phải tự try/catch LỒNG NHAU cho close() — rất dài dòng, dễ quên
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Cách MỚI — try-with-resources (Java 7+)

```java
try (FileReader reader = new FileReader("data.txt")) { // khai báo resource NGAY TRONG dấu ngoặc của try
    // xử lý file
} catch (IOException e) {
    e.printStackTrace();
}
// reader.close() được JVM TỰ ĐỘNG gọi khi thoát khỏi khối try — KHÔNG cần viết finally thủ công
```

### Điều kiện: resource phải implement `AutoCloseable`

```java
public class MyResource implements AutoCloseable {
    public void doSomething() { System.out.println("Đang xử lý..."); }

    @Override
    public void close() { // BẮT BUỘC override method close()
        System.out.println("Tài nguyên đã được đóng tự động");
    }
}
```
```java
try (MyResource resource = new MyResource()) {
    resource.doSomething();
} // close() TỰ ĐỘNG được gọi ở đây, kể cả khi có exception xảy ra trong khối try
```

### Nhiều resource cùng lúc — đóng theo thứ tự NGƯỢC LẠI với khai báo

```java
try (FileReader reader = new FileReader("input.txt");
     FileWriter writer = new FileWriter("output.txt")) {
    // xử lý
} catch (IOException e) {
    e.printStackTrace();
}
// Thứ tự đóng: writer.close() TRƯỚC, rồi mới reader.close() — ngược với thứ tự khai báo (giống nguyên tắc Stack — LIFO)
```

> **Đây chính là kiến thức nền tảng để hiểu Connection Pool trong Spring/JDBC** (Module 10 — Database) — mọi kết nối database (`Connection`, `Statement`, `ResultSet`) đều implement `AutoCloseable`, và **luôn phải** dùng try-with-resources để tránh **rò rỉ kết nối (connection leak)** — lỗi cực kỳ phổ biến và nghiêm trọng trong backend thực tế khi quên đóng connection, khiến connection pool dần cạn kiệt và service "treo" sau một thời gian chạy.

---

## 5. Custom Exception

Tự định nghĩa Exception riêng cho từng loại lỗi nghiệp vụ — giúp code xử lý lỗi **rõ ràng, có ý nghĩa nghiệp vụ**, thay vì dùng chung `Exception` mơ hồ cho mọi trường hợp.

### Custom Unchecked Exception (phổ biến hơn trong code backend hiện đại)

```java
public class InsufficientBalanceException extends RuntimeException { // kế thừa RuntimeException → Unchecked
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
```

```java
public class BankAccount {
    private double balance;

    public void withdraw(double amount) {
        if (amount > balance) {
            throw new InsufficientBalanceException(
                "Số dư không đủ: cần " + amount + " nhưng chỉ có " + balance
            );
        }
        balance -= amount;
    }
}
```

### Custom Checked Exception (khi muốn BẮT BUỘC người gọi phải xử lý)

```java
public class InvalidOrderException extends Exception { // kế thừa Exception → Checked
    public InvalidOrderException(String message) {
        super(message);
    }
}
```

```java
public void placeOrder(Order order) throws InvalidOrderException {
    if (order.getItems().isEmpty()) {
        throw new InvalidOrderException("Đơn hàng phải có ít nhất 1 sản phẩm");
    }
}
```

### Custom Exception nên thêm field bổ sung để mang theo thông tin lỗi có cấu trúc

```java
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(resourceName + " không tìm thấy với id: " + resourceId);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public String getResourceName() { return resourceName; }
    public Object getResourceId() { return resourceId; }
}
```
```java
throw new ResourceNotFoundException("User", 42L);
// message tự động: "User không tìm thấy với id: 42"
```

> **Liên hệ trực tiếp Spring Boot (Module 13):** đây chính xác là pattern được dùng trong mọi ứng dụng Spring Boot thực tế — định nghĩa các custom exception như `ResourceNotFoundException`, `InvalidRequestException`, sau đó dùng `@ControllerAdvice` + `@ExceptionHandler` để **bắt tập trung** và tự động convert thành HTTP response chuẩn (ví dụ `404 Not Found` với body JSON mô tả lỗi) — không cần try/catch thủ công ở từng Controller.

---

## 6. Exception Chaining

Khi 1 exception xảy ra **do một exception khác** gây ra (nguyên nhân gốc — root cause), nên "gắn" exception gốc vào exception mới thay vì **nuốt mất thông tin lỗi ban đầu**.

### ❌ Sai — nuốt mất thông tin lỗi gốc

```java
public void loadConfig() {
    try {
        readConfigFile();
    } catch (IOException e) {
        throw new RuntimeException("Không thể load cấu hình"); // ❌ MẤT thông tin IOException gốc — không biết lý do thực sự
    }
}
```

### ✅ Đúng — dùng Exception Chaining, giữ nguyên "chuỗi nguyên nhân"

```java
public void loadConfig() {
    try {
        readConfigFile();
    } catch (IOException e) {
        throw new RuntimeException("Không thể load cấu hình", e); // ✅ Truyền "e" làm CAUSE — tham số thứ 2 của constructor
    }
}
```

```java
try {
    loadConfig();
} catch (RuntimeException e) {
    e.printStackTrace();
    // Stack trace in ra ĐẦY ĐỦ cả 2 lớp:
    // RuntimeException: Không thể load cấu hình
    //   at ...
    // Caused by: java.io.IOException: (chi tiết lỗi gốc)
    //   at ...

    Throwable cause = e.getCause(); // lấy lại được exception GỐC nếu cần xử lý riêng
}
```

> **Vì sao Exception Chaining cực kỳ quan trọng trong backend thực tế?** Khi hệ thống production gặp lỗi, log server thường là **manh mối duy nhất** để debug. Nếu "nuốt" mất exception gốc, đội vận hành (DevOps/SRE) chỉ thấy "Không thể load cấu hình" mà **hoàn toàn không biết lý do thực sự** (file không tồn tại? quyền truy cập bị từ chối? định dạng sai?) — Exception Chaining giữ lại **toàn bộ dấu vết (stack trace)** để debug hiệu quả.

---

## 7. Best Practice — những điều NÊN và KHÔNG NÊN làm

### ❌ Anti-pattern: "Nuốt" exception (Swallowing Exception)

```java
try {
    riskyOperation();
} catch (Exception e) {
    // KHÔNG làm gì cả — lỗi biến mất âm thầm, cực kỳ nguy hiểm!
}
```
Đây là lỗi thiết kế **nghiêm trọng nhất** trong Exception Handling — chương trình tiếp tục chạy như không có gì xảy ra, dù có lỗi thực sự, khiến bug gần như không thể phát hiện cho đến khi hậu quả (dữ liệu sai, mất tiền, crash chỗ khác) đã xảy ra ở rất xa nguồn gốc thực sự.

### ❌ Anti-pattern: bắt `Exception` (hoặc tệ hơn — `Throwable`) quá rộng

```java
try {
    // logic phức tạp
} catch (Exception e) { // quá rộng — che giấu cả những lỗi lập trình (bug) lẫn lỗi nghiệp vụ hợp lệ
    log.error("Có lỗi xảy ra");
}
```
Nên `catch` **đúng loại exception cụ thể** mà mình **thực sự biết cách xử lý** — bắt quá rộng khiến việc phân biệt "lỗi nghiệp vụ có thể dự đoán" và "bug lập trình cần sửa code" trở nên mơ hồ.

### ❌ Anti-pattern: dùng Exception để điều khiển luồng chương trình (Flow Control)

```java
// ❌ Dùng exception để thoát vòng lặp — CHẬM và khó đọc
try {
    for (int i = 0; ; i++) {
        if (data[i] == null) throw new RuntimeException("Kết thúc");
        process(data[i]);
    }
} catch (RuntimeException e) {
    // "bắt" exception để dừng vòng lặp — sai mục đích thiết kế của exception
}
```
Exception **có chi phí hiệu năng** đáng kể (JVM phải tạo stack trace đầy đủ mỗi lần ném) — không nên dùng cho logic chạy bình thường, chỉ nên dùng cho **tình huống thực sự ngoại lệ**.

### ✅ Best Practice tổng hợp

1. **Luôn log đầy đủ thông tin** khi catch exception (dùng logger, không dùng `printStackTrace()` trong code production — sẽ học `SLF4J`/`Logback` ở Module 21).
2. **Chỉ catch exception mà mình biết cách xử lý** — nếu không xử lý được, để nó "bay lên" (propagate) cho tầng cao hơn xử lý, hoặc dùng Exception Chaining để bọc lại có ý nghĩa hơn.
3. **Custom Exception nên có tên rõ nghĩa nghiệp vụ** (`InsufficientBalanceException` tốt hơn nhiều so với `MyException`).
4. **Không dùng exception cho flow control** thông thường — chỉ dùng cho tình huống thực sự bất thường.
5. **Luôn dùng try-with-resources** cho mọi resource cần đóng (file, connection, stream).

---

## 8. File I/O cơ bản với `java.nio.file`

JDK hiện đại khuyến khích dùng package `java.nio.file` (NIO.2, từ Java 7) thay vì `java.io.File` cũ — API gọn hơn, xử lý exception rõ ràng hơn.

```java
import java.nio.file.*;
import java.util.List;

Path path = Paths.get("data.txt"); // đại diện đường dẫn file/thư mục, CHƯA thao tác gì trên đĩa

// Đọc toàn bộ nội dung file
String content = Files.readString(path);              // đọc thành 1 String (Java 11+)
List<String> lines = Files.readAllLines(path);          // đọc thành List<String>, mỗi phần tử là 1 dòng

// Ghi file
Files.writeString(path, "Nội dung mới"); // ghi đè toàn bộ (Java 11+)
Files.write(path, List.of("Dòng 1", "Dòng 2"), StandardOpenOption.APPEND); // ghi nối thêm vào cuối file

// Kiểm tra & thao tác file/thư mục
boolean exists = Files.exists(path);
Files.createDirectories(Paths.get("logs/2026"));  // tạo thư mục (kể cả thư mục cha nếu chưa có)
Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
Files.delete(path); // ném NoSuchFileException nếu file không tồn tại
Files.deleteIfExists(path); // an toàn hơn — không ném exception nếu file không tồn tại
```

### Đọc file lớn hiệu quả — dùng Stream thay vì đọc hết vào bộ nhớ cùng lúc

```java
try (Stream<String> lines = Files.lines(path)) { // Stream<String> — đọc LAZY, không load toàn bộ file vào RAM
    long errorCount = lines
        .filter(line -> line.contains("ERROR"))
        .count();
    System.out.println("Số dòng lỗi: " + errorCount);
} catch (IOException e) {
    throw new RuntimeException("Không đọc được file log", e); // áp dụng Exception Chaining đã học ở mục 6
}
```
> **Lưu ý quan trọng:** `Files.readAllLines()` load **toàn bộ** file vào bộ nhớ (RAM) — với file log vài GB sẽ gây `OutOfMemoryError`. `Files.lines()` trả về Stream đọc **lazy theo từng dòng**, phù hợp hơn nhiều khi xử lý file lớn — đồng thời **bắt buộc** dùng try-with-resources vì `Files.lines()` giữ 1 file handle mở cho đến khi Stream được đóng.

---

## 9. Serialization cơ bản

**Serialization** là quá trình chuyển object trong bộ nhớ thành dạng có thể **lưu trữ hoặc truyền đi** (thường là chuỗi byte), **Deserialization** là quá trình ngược lại.

```java
import java.io.*;

public class User implements Serializable { // BẮT BUỘC implement Serializable (interface đánh dấu — marker interface, không có method nào)
    private static final long serialVersionUID = 1L; // nên khai báo tường minh — kiểm soát tính tương thích khi class thay đổi cấu trúc sau này

    private String username;
    private transient String password; // "transient" — field này SẼ KHÔNG được serialize (bảo mật, hoặc dữ liệu tạm không cần lưu)

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
```

```java
// Serialization — ghi object ra file
try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("user.dat"))) {
    out.writeObject(new User("pho", "secret123"));
} catch (IOException e) {
    throw new RuntimeException("Lỗi khi lưu user", e);
}

// Deserialization — đọc object từ file
try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("user.dat"))) {
    User user = (User) in.readObject(); // cần ép kiểu, vì readObject() trả về Object
} catch (IOException | ClassNotFoundException e) {
    throw new RuntimeException("Lỗi khi đọc user", e);
}
```

> **Lưu ý thực tế backend:** Java Serialization truyền thống (`Serializable`) **hiếm khi được dùng trực tiếp** trong backend hiện đại — thay vào đó, dữ liệu trao đổi qua REST API thường dùng **JSON** (qua thư viện Jackson, sẽ gặp ở Module 16 khi học Spring Boot REST API). Tuy nhiên, khái niệm `transient` và `serialVersionUID` vẫn hữu ích cần biết vì đôi khi gặp trong cấu hình Session của Spring Security, hoặc khi làm việc với cache phân tán.

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| Checked Exception | Compiler bắt buộc catch/throws — thường do điều kiện bên ngoài (`IOException`, `SQLException`) |
| Unchecked Exception | Không bắt buộc — thường do bug lập trình (`NullPointerException`, `IllegalArgumentException`) |
| `finally` | Luôn chạy, kể cả có `return` trong try/catch — TUYỆT ĐỐI không `return` trong `finally` |
| try-with-resources | Tự động đóng resource implement `AutoCloseable` — bắt buộc dùng cho file/connection/stream |
| Đóng nhiều resource | Theo thứ tự NGƯỢC LẠI với khai báo (giống Stack — LIFO) |
| Custom Exception | Nên đặt tên rõ nghĩa nghiệp vụ, đa số dùng Unchecked trong code backend hiện đại |
| Exception Chaining | Luôn truyền exception gốc làm `cause` (`new RuntimeException(msg, e)`) — không "nuốt" thông tin lỗi |
| Anti-pattern nguy hiểm nhất | "Nuốt" exception (catch rỗng, không log gì) |
| `Files.lines()` vs `readAllLines()` | `lines()` đọc lazy (phù hợp file lớn), `readAllLines()` load hết vào RAM |
| `transient` | Field bị loại khỏi Serialization (bảo mật hoặc dữ liệu tạm) |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau in ra gì?
```java
public static int test() {
    try {
        System.out.println("A");
        return 1;
    } finally {
        System.out.println("B");
    }
}
public static void main(String[] args) {
    System.out.println("Kết quả: " + test());
}
```

**Câu 2.** Đoạn code sau có compile được không? Giải thích.
```java
public void readData() {
    FileReader reader = new FileReader("data.txt");
}
```

**Câu 3.** Đoạn code sau có vấn đề thiết kế gì nghiêm trọng? (Liên hệ Best Practice mục 7)
```java
try {
    processPayment(order);
} catch (Exception e) {
}
System.out.println("Xử lý xong");
```

**Câu 4.** Trong đoạn code sau, `e.getCause()` trả về exception nào?
```java
try {
    try {
        int x = 5 / 0;
    } catch (ArithmeticException ex) {
        throw new RuntimeException("Lỗi tính toán", ex);
    }
} catch (RuntimeException e) {
    System.out.println(e.getCause());
}
```

**Câu 5.** Đoạn code sau có đóng đúng thứ tự resource không? Giải thích thứ tự thực tế.
```java
try (var a = new MyResource("A"); var b = new MyResource("B")) {
    // ...
}
```

---

### Phần B — Bài tập viết code

**Bài 1 — Custom Exception cho hệ thống ngân hàng.**
Viết class `InsufficientBalanceException extends RuntimeException` mang thêm field `double shortfallAmount` (số tiền còn thiếu). Viết class `BankAccount` với method `withdraw(double amount)` ném exception này khi số dư không đủ, message tự động tính rõ số tiền còn thiếu. Viết `main` bắt exception và in ra thông báo thân thiện cho người dùng cuối, đồng thời log riêng `shortfallAmount` (giả lập, dùng `System.out` là đủ) cho mục đích debug nội bộ.

**Bài 2 — Exception Chaining trong hệ thống đọc cấu hình.**
Viết method `Properties loadAppConfig(String path)` đọc file `.properties` (dùng `java.util.Properties` + `FileInputStream` trong try-with-resources). Nếu file không tồn tại hoặc lỗi định dạng, bắt `IOException` gốc và ném lại `RuntimeException` mới với message rõ ràng, **đảm bảo giữ nguyên exception gốc** làm cause. Viết `main` cố tình đọc file không tồn tại, in ra `e.getCause()` để chứng minh thông tin lỗi gốc không bị mất.

**Bài 3 — try-with-resources với custom AutoCloseable.**
Viết class `DatabaseConnection implements AutoCloseable` giả lập kết nối database (constructor in ra "Đang kết nối...", method `close()` in ra "Đã đóng kết nối"). Viết class `FileLock implements AutoCloseable` giả lập khóa file (tương tự). Viết đoạn code dùng **cả 2 resource cùng lúc** trong 1 khối try-with-resources, cố tình ném 1 `RuntimeException` ở giữa xử lý — chứng minh **cả 2 resource vẫn được đóng đúng cách** (theo thứ tự ngược khai báo) dù có lỗi xảy ra giữa chừng.

**Bài 4 — Xử lý file log lớn hiệu quả bằng Stream.**
Tạo 1 file log mẫu (`app.log`) với ít nhất 1000 dòng, một số dòng chứa từ khóa `"ERROR"`, một số chứa `"WARN"`, còn lại là `"INFO"` (có thể tạo bằng code, không cần thủ công). Viết method dùng `Files.lines()` (không dùng `readAllLines()`) để:
- Đếm số dòng theo từng loại (`ERROR`/`WARN`/`INFO`) — gợi ý: kết hợp với `Collectors.groupingBy` đã học ở Module 03.3.
- Trích xuất và in ra 5 dòng `ERROR` đầu tiên.

Đảm bảo dùng try-with-resources đúng cách vì `Files.lines()` giữ file handle mở.

**Bài 5 — Bài toán tổng hợp: Hệ thống Validate & Custom Exception nhiều tầng.**
Xây dựng hệ thống đăng ký người dùng với các custom exception:
- `InvalidEmailException` (Unchecked) — email sai định dạng (kiểm tra đơn giản: chứa `@` và có phần sau dấu `.`).
- `WeakPasswordException` (Unchecked) — password dưới 8 ký tự.
- `DuplicateUserException` (Unchecked) — username đã tồn tại (giả lập bằng `Set<String>` chứa danh sách username đã đăng ký).

Viết class `UserRegistrationService` với method `void register(String username, String email, String password)` kiểm tra validate **theo đúng thứ tự trên**, ném đúng exception tương ứng ngay khi phát hiện lỗi đầu tiên (không cần kiểm tra tiếp các điều kiện sau). Viết `main` thử đăng ký với nhiều bộ dữ liệu khác nhau (hợp lệ, email sai, password yếu, username trùng), dùng multi-catch hoặc catch riêng từng loại để in ra thông báo lỗi phù hợp cho từng trường hợp.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. In ra: `A`, `B`, `Kết quả: 1` — `finally` (in "B") luôn chạy **trước khi** giá trị `return 1` thực sự được trả về cho caller, nhưng vì `finally` ở đây **không** có `return` riêng, giá trị `1` từ `try` vẫn được giữ nguyên và trả về đúng.
2. **Không compile được** — `FileReader` constructor ném `FileNotFoundException` (Checked Exception, kế thừa `IOException`), method `readData()` không `throws` cũng không `try/catch` — vi phạm quy tắc bắt buộc của Checked Exception.
3. Đây là **anti-pattern "nuốt exception"** nghiêm trọng nhất trong Best Practice — nếu `processPayment()` thất bại (ví dụ do lỗi kết nối cổng thanh toán), lỗi bị "nuốt" hoàn toàn, chương trình vẫn in ra `"Xử lý xong"` như thể thanh toán thành công — có thể dẫn đến hậu quả nghiêm trọng về nghiệp vụ (khách hàng nghĩ đã thanh toán nhưng thực ra chưa, hoặc ngược lại).
4. `e.getCause()` trả về đúng object `ArithmeticException` gốc (với message `"/ by zero"`) — vì đã được truyền vào làm tham số thứ 2 khi tạo `RuntimeException` mới (`new RuntimeException("Lỗi tính toán", ex)`), đúng kỹ thuật Exception Chaining.
5. Có — nhưng theo thứ tự **ngược lại** với khai báo: `b.close()` ("B" đóng trước) rồi mới `a.close()` ("A" đóng sau) — nguyên tắc giống Stack (LIFO), đã trình bày ở mục 4.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** `shortfallAmount = amount - balance;` tính ngay trong constructor của exception, message tự động: `"Số dư không đủ, cần thêm " + shortfallAmount + " để hoàn tất giao dịch"`.
- **Bài 2:** Đây là bài tập tái hiện chính xác pattern chuẩn trong Spring khi load `application.properties`/`application.yml` thất bại — luôn giữ lại exception gốc để đội vận hành debug được nguyên nhân thực sự (file thiếu quyền đọc? sai đường dẫn? file bị corrupt?).
- **Bài 3:** Output mong đợi (minh họa thứ tự): `"Đang kết nối..." (DatabaseConnection) → "Đang khóa file..." (FileLock) → [exception xảy ra] → "Đã đóng khóa file" (FileLock đóng TRƯỚC) → "Đã đóng kết nối" (DatabaseConnection đóng SAU)` — chứng minh try-with-resources đảm bảo dọn dẹp tài nguyên đúng thứ tự dù có lỗi giữa chừng, điều mà code viết tay dễ quên hoặc làm sai.
- **Bài 4:** Có thể tạo file mẫu bằng vòng lặp ghi ngẫu nhiên các dòng `"INFO: ..."`, `"WARN: ..."`, `"ERROR: ..."` bằng `Random`. Phần đếm theo loại có thể viết:
```java
try (Stream<String> lines = Files.lines(path)) {
    Map<String, Long> countByLevel = lines
        .map(line -> line.contains("ERROR") ? "ERROR" : line.contains("WARN") ? "WARN" : "INFO")
        .collect(Collectors.groupingBy(level -> level, Collectors.counting()));
    System.out.println(countByLevel);
}
```
- **Bài 5:** Đây là bài tập tổng hợp quan trọng nhất — chính là mô hình thu nhỏ của **Bean Validation** (`@Valid`, `@NotBlank`, `@Email`...) sẽ gặp ở Module 13 (Spring Boot Web Layer) và cách xử lý tập trung bằng `@ControllerAdvice` ở Module 13. Hoàn thành tốt bài này nghĩa là đã hiểu rõ **tại sao** cần validate theo tầng và **tại sao** mỗi loại lỗi nghiệp vụ nên có Exception riêng thay vì dùng chung 1 `Exception` mơ hồ.

</details>

---

*File tiếp theo trong lộ trình: **Module 05.1 — Thread cơ bản** (Thread vs Runnable, thread lifecycle, synchronized, volatile, race condition, deadlock).*
