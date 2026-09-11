# Module 08 — Design Patterns

> **Mức độ ưu tiên: Cao** — Spring Framework được xây dựng gần như hoàn toàn từ các pattern trong module này. Không nắm phần này thì đọc source Spring (hoặc chỉ đọc Javadoc) mãi chỉ là "học thuộc annotation" thay vì hiểu **tại sao** framework thiết kế như vậy. Đây cũng là chủ đề rất hay bị hỏi ở phỏng vấn Middle/Senior dưới dạng "thiết kế hệ thống X, bạn dùng pattern nào và vì sao **không** dùng pattern Y?".

> **Phạm vi bài này:** 23 pattern GoF cổ điển và cách chúng xuất hiện thật trong JDK và Spring backend — chia theo ba nhóm Creational / Structural / Behavioral, kèm các anti-pattern đi kèm (God object, premature abstraction, Singleton như global state, service locator, anemic domain model). Mỗi pattern: ý định một câu, cấu trúc, ví dụ Java tối thiểu, dấu hiệu nên/không nên dùng, một ví dụ JDK hoặc Spring, và cái bẫy thường gặp. **Chỉ nhắc tên, không đi sâu:** SOLID, coupling/cohesion, composition over inheritance, code smell, premature abstraction, DI/IoC, service locator, self-invocation proxy → Module 01.6; `equals`/`hashCode`/`clone`/`Comparator` → Module 02.4; fail-fast iterator → Module 03.1; Stream/lambda/`sealed`/`record`/pattern matching → Module 03.3 và Module 06; `java.io` decorator stream, try-with-resources → Module 04; `synchronized`/`volatile`/double-checked locking → Module 05.1/05.2; `@Transactional`, Bean scope, AOP nội bộ → Module 21 (Spring Core) và Module 14 (Spring Data JPA). Không lan sang kiến trúc microservice, event sourcing, CQRS.

---

## Mục lục

1. [Tổng quan — ngôn ngữ chung của thiết kế](#1-tổng-quan--ngôn-ngữ-chung-của-thiết-kế)
2. [Nhóm Creational — khởi tạo đối tượng](#2-nhóm-creational--khởi-tạo-đối-tượng)
   - 2.1 [Singleton](#21-singleton)
   - 2.2 [Factory Method](#22-factory-method)
   - 2.3 [Abstract Factory](#23-abstract-factory)
   - 2.4 [Builder](#24-builder)
   - 2.5 [Prototype](#25-prototype)
3. [Nhóm Structural — lắp ghép cấu trúc](#3-nhóm-structural--lắp-ghép-cấu-trúc)
   - 3.1 [Adapter](#31-adapter)
   - 3.2 [Decorator](#32-decorator)
   - 3.3 [Facade](#33-facade)
   - 3.4 [Proxy](#34-proxy)
   - 3.5 [Composite](#35-composite)
   - 3.6 [Bridge](#36-bridge)
   - 3.7 [Flyweight](#37-flyweight)
4. [Nhóm Behavioral — phân chia hành vi](#4-nhóm-behavioral--phân-chia-hành-vi)
   - 4.1 [Strategy](#41-strategy)
   - 4.2 [Observer](#42-observer)
   - 4.3 [Template Method](#43-template-method)
   - 4.4 [Command](#44-command)
   - 4.5 [Iterator](#45-iterator)
   - 4.6 [State](#46-state)
   - 4.7 [Chain of Responsibility](#47-chain-of-responsibility)
   - 4.8 [Mediator](#48-mediator)
   - 4.9 [Memento](#49-memento)
   - 4.10 [Visitor](#410-visitor)
   - 4.11 [Null Object](#411-null-object)
   - 4.12 [Interpreter (ngắn)](#412-interpreter-ngắn)
5. [Dependency Injection & IoC — nền của Spring](#5-dependency-injection--ioc--nền-của-spring)
6. [Pattern và tính năng ngôn ngữ hiện đại](#6-pattern-và-tính-năng-ngôn-ngữ-hiện-đại)
7. [Design Patterns trong Spring](#7-design-patterns-trong-spring)
8. [Anti-pattern và over-engineering](#8-anti-pattern-và-over-engineering)
9. [Chọn pattern thế nào](#9-chọn-pattern-thế-nào)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Tổng quan — ngôn ngữ chung của thiết kế

Design pattern là **tên gọi chung cho một giải pháp lặp lại** trong một ngữ cảnh lặp lại. Giá trị lớn nhất không phải là "code mẫu để chép", mà là **từ vựng**: nói "chỗ này nên là một Strategy" truyền đạt nhanh hơn nhiều so với mô tả mười dòng.

23 pattern trong sách *Design Patterns* (Gang of Four, 1994) chia làm ba nhóm theo **loại vấn đề**:

| Nhóm | Câu hỏi trọng tâm | Pattern |
|---|---|---|
| **Creational** (khởi tạo) | Tạo object thế nào để không trói code vào `new ConcreteClass()` cụ thể? | Singleton, Factory Method, Abstract Factory, Builder, Prototype |
| **Structural** (cấu trúc) | Ghép class/object thành cấu trúc lớn hơn mà vẫn linh hoạt? | Adapter, Decorator, Facade, Proxy, Composite, Bridge, Flyweight |
| **Behavioral** (hành vi) | Phân chia trách nhiệm và luồng giao tiếp giữa các object? | Strategy, Observer, Template Method, Command, Iterator, State, Chain of Responsibility, Mediator, Memento, Visitor, ngoài ra Null Object, Interpreter |

### Ba nguyên tắc nền mà mọi pattern đều phục vụ

1. **Lập trình theo interface, không theo implementation** — client phụ thuộc abstraction (Module 01.6 §6, DIP).
2. **Ưu tiên composition hơn inheritance** — Strategy/Decorator/Bridge tồn tại chính vì kế thừa cứng nhắc.
3. **Đóng gói phần hay thay đổi** — tách "cái biến thiên" ra sau một abstraction, phần còn lại đứng yên (Module 01.6 §3, OCP).

### Cách đọc một pattern

Với mỗi pattern, hỏi bốn câu: **(a)** nó hấp thụ *loại thay đổi* nào? **(b)** cái giá phải trả (số class, lớp gián tiếp) là gì? **(c)** ngôn ngữ hiện đại đã có sẵn cơ chế thay thế chưa (lambda, `sealed`, `enum`, `record`)? **(d)** JDK/Spring đã có bản dựng sẵn chưa, để mình dùng lại thay vì tự viết?

> Pattern là công cụ đánh giá và giao tiếp, không phải chỉ tiêu KPI. Một hệ thống "dùng nhiều pattern" không tự nó tốt hơn; nhồi pattern vào chỗ không có biến thiên chỉ tạo ra *needless complexity* (Module 01.6 §9, §11).

---

## 2. Nhóm Creational — khởi tạo đối tượng

Mục tiêu chung: **tách quyết định "tạo object nào, tạo thế nào" ra khỏi code sử dụng object**. Trong dự án Spring, phần lớn việc này do IoC container làm hộ — nhưng vẫn cần hiểu bản chất để đọc source và để thiết kế API thư viện.

### 2.1 Singleton

> **Ý định:** đảm bảo một class chỉ có **đúng một instance** trong toàn vòng đời ứng dụng và cung cấp một điểm truy cập toàn cục tới nó.

#### Cơ bản — chưa an toàn đa luồng

```java
public class AppConfig {
    private static AppConfig instance;
    private AppConfig() { }                       // constructor private — chặn "new" từ ngoài

    public static AppConfig getInstance() {
        if (instance == null) {                   // ⚠️ race condition — 2 thread cùng vượt check này (Module 05.1)
            instance = new AppConfig();
        }
        return instance;
    }
}
```

#### Double-Checked Locking — cần `volatile` (Module 05.1/05.2)

```java
public class AppConfig {
    private static volatile AppConfig instance;   // volatile: chặn reorder + đảm bảo visibility
    private AppConfig() { }

    public static AppConfig getInstance() {
        if (instance == null) {                   // check 1: không lock, nhanh ở mọi lần gọi sau
            synchronized (AppConfig.class) {
                if (instance == null) {           // check 2: trong lock, chống tạo 2 lần
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }
}
```

> Thiếu `volatile`, một thread khác có thể thấy `instance != null` nhưng object **chưa khởi tạo xong** (đọc ra trạng thái nửa vời). Chi tiết mô hình bộ nhớ ở Module 05.2.

#### Holder idiom — lazy, không cần lock

```java
public class AppConfig {
    private AppConfig() { }
    private static class Holder {                 // class con chỉ nạp khi lần đầu bị tham chiếu
        private static final AppConfig INSTANCE = new AppConfig();
    }
    public static AppConfig getInstance() { return Holder.INSTANCE; }
}
```

JVM đảm bảo class được nạp/khởi tạo static field **đúng một lần** — vừa lazy vừa thread-safe, không `synchronized`.

#### Enum singleton — cách gọn nhất, chống được reflection và serialization (Effective Java, Item 3)

```java
public enum AppConfig {
    INSTANCE;
    private final Properties props = load();
    public String get(String key) { return props.getProperty(key); }
}
```

Constructor `enum` không gọi được bằng reflection; deserialize luôn trả về hằng có sẵn nên không tạo bản sao. Nhược điểm: không lazy, không `extends` được class khác.

#### Trong Spring: bean scope thay cho Singleton thủ công

```java
@Service                                         // mặc định scope = singleton trong ApplicationContext
public class UserService { }
```

Mỗi `@Component`/`@Service`/`@Repository` mặc định **một instance duy nhất** do container quản lý — bạn **không tự viết** Singleton trong dự án Spring. Khác biệt quan trọng: đây là "một instance mỗi container", không phải "một instance mỗi ClassLoader" như `enum` singleton; và container quản lý được vòng đời, DI, proxy AOP quanh nó.

- **Dấu hiệu nên dùng:** đúng một tài nguyên dùng chung, vô trạng thái hoặc trạng thái bất biến (registry, cache config, connection pool factory).
- **Khi KHÔNG nên dùng:** có state thay đổi được → biến thành **global mutable state**, khó test (không reset được giữa các test), giấu dependency (code gọi `X.getInstance()` bên trong thay vì nhận qua constructor — nghịch DIP, Module 01.6 §7). Trong Spring, để container lo.
- **Bẫy:** Singleton giữ state → test này ảnh hưởng test kia; `getInstance()` rải khắp code = service locator trá hình; nhiều ClassLoader (app server cũ) phá vỡ "duy nhất".

### 2.2 Factory Method

> **Ý định:** định nghĩa một method để tạo object, nhưng **để lớp con quyết định** class cụ thể nào được tạo.

```java
abstract class Dialog {
    public void render() {                        // logic CHUNG, cố định
        Button ok = createButton();               // bước tạo — hoãn cho lớp con
        ok.onClick(this::close);
        ok.paint();
    }
    protected abstract Button createButton();     // factory method
}

class WindowsDialog extends Dialog {
    protected Button createButton() { return new WindowsButton(); }
}
```

Khác **Simple Factory** (một method `switch` trả về theo tham số — tiện, hay dùng, nhưng không phải pattern GoF): Factory Method dùng **kế thừa + đa hình**, không có `switch`.

- **JDK/Spring:** `Collection.iterator()` (mỗi collection tự quyết định loại `Iterator`), `Calendar.getInstance()`, `ThreadFactory`. Trong Spring, method `@Bean` chính là một factory method do container gọi; `FactoryBean<T>` là điểm mở rộng khi việc tạo bean phức tạp.
- **Dấu hiệu nên dùng:** một khung xử lý cố định, chỉ khác nhau ở "sản phẩm" được tạo; muốn lớp con mở rộng loại sản phẩm mà không sửa khung.
- **Khi KHÔNG nên dùng:** chỉ có một loại sản phẩm ổn định → `new` thẳng; hoặc chỉ cần chọn theo tham số runtime → Simple Factory / `Map<Key, Supplier<T>>` gọn hơn.
- **Bẫy:** đẻ ra một cây lớp song song (mỗi `Product` một `Creator`); nhầm với Abstract Factory (dưới đây).

### 2.3 Abstract Factory

> **Ý định:** tạo **một họ các object liên quan** mà không nêu class cụ thể, đảm bảo các object trong họ **dùng chung được với nhau**.

```java
interface GuiFactory {
    Button createButton();
    Checkbox createCheckbox();
}
class MacFactory implements GuiFactory {
    public Button createButton() { return new MacButton(); }
    public Checkbox createCheckbox() { return new MacCheckbox(); }
}
// Client chỉ giữ GuiFactory → không bao giờ lỡ trộn MacButton với WindowsCheckbox
```

- **JDK:** `DocumentBuilderFactory`, `TransformerFactory`, `SAXParserFactory` — mỗi cái tạo một họ object phân tích XML tương thích nhau. `java.sql.Connection` cũng gần: một `Connection` sinh ra `Statement`, `PreparedStatement`, `Blob`... cùng vendor.
- **Dấu hiệu nên dùng:** sản phẩm đi theo bộ, phải khớp nền tảng/nhà cung cấp (UI theo OS, bộ widget theo theme, driver theo DB).
- **Khi KHÔNG nên dùng:** chỉ có một họ; hoặc các sản phẩm không thực sự ràng buộc lẫn nhau. Trong Spring, container + `@Profile`/`@Conditional` thường thay thế được: chọn nguyên bộ bean theo môi trường.
- **Bẫy:** bùng nổ số class; thêm một loại sản phẩm mới (thêm method vào interface factory) buộc sửa **mọi** factory — nghịch OCP theo trục "thêm sản phẩm".

### 2.4 Builder

> **Ý định:** dựng một object phức tạp, nhiều tham số tùy chọn, **theo từng bước rõ ràng**, tránh constructor dài loằng ngoằng (Effective Java, Item 2).

#### Vấn đề: telescoping constructor

```java
// ❌ đọc vào không biết true/false nào ứng với topping nào, dễ truyền nhầm thứ tự
new Pizza("L", true, false, true, false, true);
```

#### Builder + bất biến

```java
public final class Pizza {
    private final String size;                    // final → immutable sau build (liên hệ record, Module 06)
    private final boolean cheese, pepperoni, mushroom;

    private Pizza(Builder b) {
        this.size = b.size; this.cheese = b.cheese;
        this.pepperoni = b.pepperoni; this.mushroom = b.mushroom;
    }

    public static Builder builder(String size) {  // tham số BẮT BUỘC ép qua đây
        return new Builder(size);
    }

    public static final class Builder {
        private final String size;                // required
        private boolean cheese, pepperoni, mushroom;   // optional, có default
        private Builder(String size) { this.size = Objects.requireNonNull(size); }

        public Builder cheese(boolean v)    { this.cheese = v; return this; }   // chaining
        public Builder pepperoni(boolean v) { this.pepperoni = v; return this; }
        public Builder mushroom(boolean v)  { this.mushroom = v; return this; }

        public Pizza build() {
            if (mushroom && "S".equals(size))
                throw new IllegalStateException("Pizza S không đủ chỗ cho nấm");
            return new Pizza(this);               // validate cross-field ở đây
        }
    }
}
```

```java
Pizza p = Pizza.builder("L").cheese(true).mushroom(true).build();
```

- **JDK/Spring:** `StringBuilder` (không hẳn Builder GoF vì không có `build()` trả kiểu khác), `Stream.Builder`, `HttpRequest.newBuilder()` (`java.net.http`), `Locale.Builder`, `Calendar.Builder`; Spring: `UriComponentsBuilder`, `BeanDefinitionBuilder`, `MockMvcRequestBuilders`.
- **Lombok `@Builder`:** sinh builder tự động; kết hợp `@Value` cho immutable. Tiện nhưng ẩn code — cân nhắc `@Builder(toBuilder = true)` để "copy có sửa".
- **Dấu hiệu nên dùng:** ≥ 4–5 tham số, nhiều cái optional, nhiều tham số cùng kiểu; muốn object bất biến sau khi tạo.
- **Khi KHÔNG nên dùng:** 2–3 field bắt buộc → constructor hoặc `record` là đủ, builder chỉ thêm nghi thức thừa.
- **Bẫy:** quên validate trong `build()`; builder cho phép object nửa vời thoát ra ngoài; builder **mutable** bị chia sẻ giữa nhiều luồng; "required" mà để ở method chaining thì compiler không ép được — đưa vào constructor của builder hoặc của `builder(...)`.

### 2.5 Prototype

> **Ý định:** tạo object mới bằng cách **sao chép một mẫu có sẵn**, thay vì khởi tạo lại từ đầu (hữu ích khi khởi tạo tốn kém hoặc cấu hình phức tạp).

```java
// GoF gốc dùng Cloneable — nhưng clone() của Java nhiều cạm bẫy (Module 02.4)
public class Board implements Cloneable {
    private int[][] cells;
    @Override public Board clone() {
        try {
            Board copy = (Board) super.clone();   // shallow copy: copy.cells VẪN trỏ chung mảng!
            copy.cells = Arrays.stream(cells).map(int[]::clone).toArray(int[][]::new); // phải deep-copy tay
            return copy;
        } catch (CloneNotSupportedException e) { throw new AssertionError(e); }
    }
}
```

`clone()` **không gọi constructor**, không khởi tạo `final` field như mong đợi, `Cloneable` là marker interface rỗng, và mặc định là **shallow copy**. Cách được khuyến nghị: **copy constructor** hoặc **copy factory**.

```java
public record Point(int x, int y) { }
public final class Polygon {
    private final List<Point> points;
    public Polygon(Polygon other) {              // copy constructor — rõ ràng, gọi được final, không exception lạ
        this.points = List.copyOf(other.points); // Point bất biến nên không cần copy sâu hơn
    }
}
```

- **JDK/Spring:** `ArrayList(Collection)`, `HashMap(Map)` là copy constructor; `Object.clone()` dùng trong `ArrayList.clone()`, mảng (`arr.clone()`). Spring: `@Scope("prototype")` **tên trùng nhưng nghĩa khác** — nó nghĩa là "container tạo instance MỚI mỗi lần lấy bean", không phải sao chép từ mẫu.
- **Dấu hiệu nên dùng:** cần nhiều bản gần giống nhau từ một cấu hình gốc; khởi tạo thật sự đắt (đọc file, query, tính toán) và bản sao rẻ hơn.
- **Khi KHÔNG nên dùng:** object đơn giản → cứ `new`; object bất biến → chia sẻ luôn, không cần sao chép.
- **Bẫy:** shallow copy chia sẻ ngầm state con → sửa bản sao làm hỏng bản gốc; `clone()` và kế thừa xung khắc; quên copy field mới thêm sau này.

---

## 3. Nhóm Structural — lắp ghép cấu trúc

Mục tiêu chung: ghép các object lại thành cấu trúc lớn hơn mà **không hàn cứng** chúng vào nhau. Điểm mấu chốt để phân biệt nhóm này: tất cả đều xoay quanh "một object giữ tham chiếu tới object khác và ủy quyền", chỉ khác nhau ở **mục đích** của việc ủy quyền đó.

### 3.1 Adapter

> **Ý định:** chuyển interface của một class thành interface mà client mong đợi, để hai bên **không tương thích** làm việc được với nhau.

```java
interface PaymentProcessor { void pay(long amountCents); }      // hệ thống MỚI mong đợi

class LegacyGateway {                                            // thư viện CŨ, không sửa được
    void makeTransaction(String currency, double dollars) { /* ... */ }
}

class LegacyGatewayAdapter implements PaymentProcessor {         // OBJECT adapter — dùng composition
    private final LegacyGateway legacy;
    LegacyGatewayAdapter(LegacyGateway legacy) { this.legacy = legacy; }
    public void pay(long amountCents) {
        legacy.makeTransaction("USD", amountCents / 100.0);      // chỉ DỊCH dữ liệu, không thêm nghiệp vụ
    }
}
```

- **Class adapter vs object adapter:** class adapter `extends` bên bị adapt (Java chỉ đơn kế thừa nên hiếm dùng, và trói vào một class cụ thể); **object adapter** giữ tham chiếu (linh hoạt hơn, adapt được cả cây con) — hầu như luôn chọn cái này.
- **JDK:** `Arrays.asList(T...)` (mảng → `List`), `InputStreamReader` (byte stream → char stream), `Collections.enumeration`/`list` (`Iterator` ↔ `Enumeration`). Spring MVC: `HandlerAdapter` cho phép dùng nhiều kiểu controller khác nhau qua một interface.
- **Dấu hiệu nên dùng:** tích hợp SDK/thư viện bên thứ ba có interface lệch với kiến trúc hiện tại; muốn cô lập sự lệch đó vào **một** chỗ.
- **Khi KHÔNG nên dùng:** bạn sở hữu cả hai phía và sửa được — sửa thẳng interface; khi cần thiết kế trước cho nhiều impl độc lập → đó là Bridge, không phải Adapter.
- **Bẫy:** adapter phình ra chứa cả logic nghiệp vụ (vi phạm SRP); adapter rò kiểu của thư viện cũ ra ngoài (`Charge` của Stripe lọt vào nghiệp vụ — Module 01.6 §6).

### 3.2 Decorator

> **Ý định:** gắn thêm trách nhiệm cho một object **động, theo lớp**, giữ nguyên interface — thay cho việc đẻ ra tổ hợp lớp con.

```java
interface DataSource { String read(); }

class FileDataSource implements DataSource {
    public String read() { return "raw"; }
}

abstract class DataSourceDecorator implements DataSource {
    protected final DataSource wrappee;                 // GIỮ tham chiếu cùng interface
    protected DataSourceDecorator(DataSource d) { this.wrappee = d; }
}

class EncryptionDecorator extends DataSourceDecorator {
    EncryptionDecorator(DataSource d) { super(d); }
    public String read() { return decrypt(wrappee.read()); }   // thêm hành vi TRƯỚC/SAU rồi ủy quyền
}
class CompressionDecorator extends DataSourceDecorator {
    CompressionDecorator(DataSource d) { super(d); }
    public String read() { return unzip(wrappee.read()); }
}
```

```java
DataSource src = new EncryptionDecorator(new CompressionDecorator(new FileDataSource()));
// đọc: file → giải nén → giải mã. Xếp lớp tùy ý, không cần lớp "EncryptedCompressedFile"
```

- **JDK:** `java.io` là ví dụ kinh điển — `new BufferedInputStream(new GZIPInputStream(new FileInputStream(f)))` (Module 04). `Collections.unmodifiableList`/`synchronizedList` bọc thêm hành vi mà giữ interface `List`.
- **Spring:** `HttpServletRequestWrapper`/`ResponseWrapper` trong filter; `DelegatingFilterProxy`; `TransactionAwareDataSourceProxy`.
- **Decorator vs kế thừa:** 3 hành vi tùy chọn kết hợp tự do → kế thừa cần 2³ lớp, decorator cần 3 lớp + xếp chồng runtime.
- **Dấu hiệu nên dùng:** nhiều "trang sức" độc lập, bật/tắt và kết hợp tùy ý (buffer, nén, mã hóa, đo lường, retry).
- **Khi KHÔNG nên dùng:** chỉ một biến thể cố định → cứ một lớp; cần chặn/đổi luồng gọi (không chỉ "thêm") → nghiêng về Proxy.
- **Bẫy:** quá nhiều lớp mỏng khó debug (stack trace sâu); thứ tự bọc quan trọng mà không được nói rõ; `equals()`/định danh bị phá qua nhiều lớp bọc; decorator quên ủy quyền một method mới thêm vào interface.

### 3.3 Facade

> **Ý định:** cung cấp **một interface đơn giản, thống nhất** cho một hệ thống con phức tạp; client thường chỉ cần phần "80%".

```java
// Không facade: client tự lo Connection, Statement, ResultSet, try/catch/finally, close...
public final class ReportFacade {
    private final DataSource ds;
    public ReportFacade(DataSource ds) { this.ds = ds; }
    public List<Row> monthlyRevenue(int year) {
        // giấu toàn bộ JDBC lằng nhằng sau MỘT method nghiệp vụ
    }
}
```

- **Spring:** `JdbcTemplate` là facade kinh điển trên JDBC thô — nuốt hết `Connection`/`Statement`/`ResultSet`/đóng tài nguyên/dịch `SQLException` thành `DataAccessException`; bạn chỉ đưa SQL + `RowMapper`. Tương tự `RestClient`/`RestTemplate`, `JmsTemplate`, `RedisTemplate`.
- **Facade vs Adapter:** Adapter đổi *một* interface cho *khớp*; Facade *gộp nhiều* interface thành *một cái đơn giản hơn*, không nhằm khớp chuẩn nào.
- **Dấu hiệu nên dùng:** một subsystem nhiều bước, nhiều class, khách hàng đa số chỉ dùng vài kịch bản; muốn một điểm vào để giảm coupling từ ngoài vào trong.
- **Khi KHÔNG nên dùng:** subsystem vốn đã đơn giản; hoặc client thật sự cần toàn bộ khả năng chi tiết → đừng chặn họ.
- **Bẫy:** facade phình thành **God object** ôm mọi thứ (Module 01.6 §9, và §8 dưới đây); facade rò kiểu nội bộ của subsystem ra API công khai, làm mất tác dụng che chắn.

### 3.4 Proxy

> **Ý định:** cung cấp một object **đại diện** đứng trước object thật để **kiểm soát truy cập** — thêm logic trước/sau, trì hoãn tạo, kiểm tra quyền, gọi từ xa — mà không sửa object thật.

```java
interface ImageService { byte[] load(String id); }

class RemoteImageService implements ImageService {                 // Real Subject
    public byte[] load(String id) { /* gọi mạng, chậm */ return new byte[0]; }
}

class CachingImageProxy implements ImageService {                  // Proxy — cùng interface
    private final ImageService target;
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    CachingImageProxy(ImageService target) { this.target = target; }
    public byte[] load(String id) {
        return cache.computeIfAbsent(id, target::load);            // kiểm soát: chặn gọi thật nếu đã có
    }
}
```

#### Dynamic proxy trong JDK

```java
ImageService proxy = (ImageService) Proxy.newProxyInstance(
    ImageService.class.getClassLoader(),
    new Class<?>[]{ ImageService.class },
    (p, method, args) -> {                                        // InvocationHandler
        long t = System.nanoTime();
        try { return method.invoke(realTarget, args); }
        finally { log.info("{} mất {}ns", method.getName(), System.nanoTime() - t); }
    });
```

`java.lang.reflect.Proxy` chỉ proxy được qua **interface**. Khi bean không có interface, Spring dùng **CGLIB** tạo lớp con lúc runtime — do đó **không proxy được method `final`/`private`/`static`** và class `final`.

#### Spring AOP proxy — `@Transactional`, `@Cacheable`, `@Async`

```java
@Service
public class OrderService {
    @Transactional
    public void placeOrder(Order o) { validate(o); save(o); }
}
```

Spring **không** gọi thẳng `OrderService` thật — nó tạo một proxy: **mở transaction → gọi method thật → commit/rollback**. Đây là cơ chế chung sau mọi annotation "kỳ diệu".

> **Bẫy self-invocation (Module 01.6 §10, sẽ gặp lại ở Module 14):** trong `placeOrder`, gọi `this.otherTxMethod()` **không** đi qua proxy → `@Transactional`/`@Cacheable` trên `otherTxMethod` **vô hiệu**. Sửa: tách sang bean khác, tự inject chính mình, hoặc dùng `TransactionTemplate`/`AopContext.currentProxy()`.

- **Các biến thể:** virtual proxy (lazy-init, `@Lazy`), protection proxy (`@PreAuthorize`), remote proxy (RMI stub, Feign client), logging/metrics proxy.
- **Dấu hiệu nên dùng:** thêm mối quan tâm cắt ngang (transaction, cache, security, log, retry) đồng đều lên nhiều method; trì hoãn tạo object nặng.
- **Khi KHÔNG nên dùng:** chỉ "thêm hành vi trang trí" không kiểm soát luồng → Decorator; logic riêng cho từng method → viết thẳng.
- **Bẫy:** self-invocation; method `final`/`private` không được advise; `this` bên trong khác với proxy; proxy làm sai lệch `getClass()`/`instanceof`.

### 3.5 Composite

> **Ý định:** tổ chức object thành **cây phân cấp** và cho client đối xử **lá và cành như nhau** qua một interface chung.

```java
interface FsNode { long size(); }

record FileNode(String name, long bytes) implements FsNode {
    public long size() { return bytes; }
}
final class DirNode implements FsNode {
    private final List<FsNode> children = new ArrayList<>();
    public void add(FsNode n) { children.add(n); }
    public long size() {                                   // đệ quy — client không cần biết là file hay thư mục
        return children.stream().mapToLong(FsNode::size).sum();
    }
}
```

- **Ứng dụng:** cây thư mục, DOM/UI component, sơ đồ tổ chức, menu, biểu thức số học, quyền phân cấp.
- **JDK/Spring:** `java.awt.Container`, `javax.swing.JComponent`; Spring: `CompositeCacheManager`, `WebMvcConfigurerComposite`, `CompositePropertySource` — gom nhiều thể hiện cùng interface thành một.
- **Dấu hiệu nên dùng:** dữ liệu tự nhiên là cây, và thao tác lên cây phần lớn là "duyệt + tổng hợp".
- **Khi KHÔNG nên dùng:** cấu trúc phẳng; hoặc lá và cành thực sự khác nhau tới mức ép chung interface làm hại rõ ràng.
- **Bẫy:** lá bị ép cài `add()`/`remove()` vô nghĩa (căng thẳng với ISP, Module 01.6 §5) — hoặc để `add()` ở interface con `Composite`, chấp nhận client phải ép kiểu; chu trình trong "cây" gây đệ quy vô hạn.

### 3.6 Bridge

> **Ý định:** **tách một abstraction khỏi implementation của nó** để hai bên tiến hóa độc lập, thay vì trói vào nhau bằng kế thừa.

```java
// Trục 1 — abstraction: loại thông báo
abstract class Notification {
    protected final MessageChannel channel;               // cầu nối sang trục 2
    protected Notification(MessageChannel channel) { this.channel = channel; }
    abstract void send(String user, String text);
}
class UrgentNotification extends Notification {
    UrgentNotification(MessageChannel c) { super(c); }
    void send(String u, String t) { channel.deliver(u, "[URGENT] " + t); }
}
// Trục 2 — implementation: kênh gửi
interface MessageChannel { void deliver(String user, String payload); }
class EmailChannel implements MessageChannel { public void deliver(String u, String p) { } }
class SmsChannel   implements MessageChannel { public void deliver(String u, String p) { } }
```

Không có Bridge: `UrgentEmail`, `UrgentSms`, `NormalEmail`, `NormalSms`... = tích Descartes. Có Bridge: `2 + 2` lớp, ghép runtime.

- **JDK:** `java.sql.Driver`/`DriverManager` — API JDBC (abstraction) tách khỏi driver từng vendor (implementation). SLF4J (abstraction logging) bắc cầu sang Logback/Log4j2. `java.util.logging.Handler`.
- **Bridge vs Adapter:** Bridge **thiết kế từ đầu** để hai trục biến thiên độc lập; Adapter **chữa cháy** khi hai interface đã lệch sẵn.
- **Bridge vs Strategy:** cấu trúc giống (composition), khác ý định — Bridge tách *cả một hệ phân cấp abstraction* khỏi *cả một hệ phân cấp implementation*; Strategy chỉ hoán một thuật toán.
- **Dấu hiệu nên dùng:** hai chiều thay đổi vuông góc nhau, mỗi chiều đều có nhiều biến thể (loại × nền tảng, hình × cách vẽ).
- **Khi KHÔNG nên dùng:** chỉ một chiều biến thiên; hoặc mỗi chiều chỉ một biến thể (chưa cần).
- **Bẫy:** dựng Bridge khi mới có một implementation — trừu tượng hóa sớm (Module 01.6 §11).

### 3.7 Flyweight

> **Ý định:** chia sẻ **phần state bất biến, dùng chung** (intrinsic) giữa rất nhiều object nhỏ để tiết kiệm bộ nhớ; phần khác nhau (extrinsic) truyền vào lúc dùng.

```java
Integer a = Integer.valueOf(100);   // lấy từ cache -128..127
Integer b = Integer.valueOf(100);
a == b;                             // true — CÙNG object (flyweight, Module 01.1)

Integer c = Integer.valueOf(1000);
Integer d = Integer.valueOf(1000);
c == d;                            // false — ngoài dải cache, tạo mới
```

- **JDK:** cache của `Integer`/`Long`/`Short`/`Byte`/`Character.valueOf`, `Boolean.TRUE/FALSE`, String pool (literal được intern), `enum` (mỗi hằng đúng một instance).
- **Dấu hiệu nên dùng:** hàng trăm nghìn/triệu object gần giống nhau, phần lõi bất biến chiếm phần lớn bộ nhớ (glyph trong trình soạn thảo, tile trong game, `Locale`, mã tiền tệ).
- **Khi KHÔNG nên dùng:** số lượng object vừa phải; state chủ yếu là biến thiên; chưa đo thấy vấn đề bộ nhớ → đừng tối ưu sớm.
- **Bẫy:** flyweight phải **bất biến** — nếu mutable, sửa một chỗ hỏng mọi nơi dùng chung; dựa vào `==` cho `Integer` chỉ đúng trong dải cache (nguồn bug kinh điển — luôn `.equals()`); giữ tham chiếu extrinsic trong flyweight vô tình.

---

## 4. Nhóm Behavioral — phân chia hành vi

Mục tiêu chung: phân bổ trách nhiệm và tổ chức **luồng giao tiếp** giữa các object. Nhiều pattern nhóm này ngày nay được **lambda** hoặc **`sealed` + pattern matching** làm gọn đi rất nhiều (§6).

### 4.1 Strategy

> **Ý định:** đóng gói **một họ thuật toán** vào các class/hàm riêng, cho phép **hoán đổi lúc runtime** mà không đụng code gọi.

```java
interface ShippingPolicy { long fee(long grams); }
class Standard implements ShippingPolicy { public long fee(long g) { return g * 2; } }
class Express  implements ShippingPolicy { public long fee(long g) { return g * 5; } }

class Checkout {
    private final ShippingPolicy policy;                 // tiêm vào — chính là DI
    Checkout(ShippingPolicy policy) { this.policy = policy; }
    long total(long grams, long goods) { return goods + policy.fee(grams); }
}
```

- **Strategy = lambda:** `ShippingPolicy` là functional interface → `new Checkout(g -> g * 2)`. `Comparator` chính là Strategy (`list.sort(comparing(User::age))`); `Runnable`, `Predicate`, `Function` đều vậy.
- **Strategy vs `enum` có hành vi:** khi tập chiến lược **đóng và nhỏ**, `enum` với method abstract gọn hơn và exhaustively-checkable:
  ```java
  enum Op { ADD { long apply(long a, long b) { return a + b; } },
            MUL { long apply(long a, long b) { return a * b; } };
      abstract long apply(long a, long b); }
  ```
- **Spring:** inject `List<ShippingPolicy>` hoặc `Map<String, ShippingPolicy>` — container gom mọi bean cùng interface, chọn theo key/`@Qualifier`/`@Primary` (Module 01.6 §10). `RowMapper`, `ResponseErrorHandler` là Strategy.
- **Dấu hiệu nên dùng:** một chỗ trong luồng có nhiều cách tính, chọn theo cấu hình/loại khách/A-B test; muốn thêm cách mới không sửa code cũ (OCP).
- **Khi KHÔNG nên dùng:** chỉ một thuật toán ổn định; hoặc khác biệt chỉ là một tham số → truyền tham số.
- **Bẫy:** interface Strategy chỉ có một impl suốt đời (trừu tượng thừa); phải nhồi quá nhiều context vào chữ ký method vì strategy không giữ được state chung.

### 4.2 Observer

> **Ý định:** khi một object (**Subject**) đổi trạng thái, **tự động thông báo** mọi object đăng ký theo dõi (**Observer**) mà không biết cụ thể chúng là ai.

```java
interface OrderListener { void onPlaced(Order o); }

class OrderService {
    private final List<OrderListener> listeners = new CopyOnWriteArrayList<>();  // tránh race khi đăng ký/hủy
    void register(OrderListener l)   { listeners.add(l); }
    void unregister(OrderListener l) { listeners.remove(l); }                    // ⚠️ nhớ hủy!
    void place(Order o) {
        // ... lưu đơn ...
        listeners.forEach(l -> l.onPlaced(o));
    }
}
```

- **JDK:** `java.util.Observable`/`Observer` **đã deprecated từ Java 9** (thiết kế kém: phải `extends`, thứ tự không xác định, không type-safe). Thay bằng `PropertyChangeListener`/`PropertyChangeSupport`, hoặc `Flow.Publisher`/`Subscriber` (reactive).
- **Spring:** `ApplicationEventPublisher.publishEvent(...)` + `@EventListener` (đồng bộ mặc định); `@Async @EventListener` (bất đồng bộ); `@TransactionalEventListener` (chạy sau commit).
- **Dấu hiệu nên dùng:** một sự kiện nghiệp vụ kéo theo nhiều phản ứng độc lập (gửi mail, cập nhật kho, ghi audit), và danh sách phản ứng còn thay đổi; muốn tách publisher khỏi subscriber (SRP + OCP).
- **Khi KHÔNG nên dùng:** chỉ một phản ứng cố định → gọi thẳng; cần đảm bảo thứ tự/giao dịch chặt giữa các bước → orchestration tường minh, không phải event.
- **Bẫy:** **lapsed listener** — observer đăng ký mà không hủy → subject giữ tham chiếu → rò bộ nhớ; một listener ném exception làm hỏng cả vòng lặp thông báo (và cả nghiệp vụ chính nếu đồng bộ); phụ thuộc ngầm vào thứ tự gọi listener.

### 4.3 Template Method

> **Ý định:** định nghĩa **bộ khung của một thuật toán** ở lớp cha, để lớp con điền vào **một số bước** mà không đổi được cấu trúc tổng thể.

```java
abstract class ImportJob {
    public final void run() {                      // final — khóa THỨ TỰ các bước
        var raw = extract();
        var clean = transform(raw);
        load(clean);
        afterLoad();                               // hook — mặc định rỗng, lớp con override nếu cần
    }
    protected abstract List<String> extract();
    protected abstract List<Row> transform(List<String> raw);
    protected abstract void load(List<Row> rows);
    protected void afterLoad() { }                 // hook method
}
```

- **JDK:** `AbstractList`/`AbstractMap`/`AbstractSet` (bạn chỉ cài vài method nguyên thủy, phần còn lại có sẵn), `InputStream.read(byte[])` gọi `read()`, `HttpServlet.service()` phân phối sang `doGet`/`doPost`.
- **Spring:** mọi lớp `*Template` một phần là Template Method; `AbstractApplicationContext.refresh()` là template dài với nhiều hook (`postProcessBeanFactory`...).
- **Template Method vs Strategy:** Template Method = **kế thừa**, cố định khung ở lớp cha; Strategy = **composition**, hoán cả thuật toán. `JdbcTemplate` thực ra **lật** Template Method thành callback: khung ở `JdbcTemplate`, bước biến thiên truyền vào qua `RowMapper`/`PreparedStatementSetter` (Strategy) — tránh ép người dùng kế thừa.
- **Dấu hiệu nên dùng:** nhiều biến thể chia sẻ **đúng một trình tự**, chỉ khác vài bước; muốn ép trình tự đó (không cho lớp con phá).
- **Khi KHÔNG nên dùng:** các biến thể khác nhau cả về trình tự; hoặc bạn muốn tránh kế thừa → chọn callback/Strategy.
- **Bẫy:** *fragile base class* — sửa lớp cha làm vỡ lớp con ở xa; quá nhiều bước abstract khiến mỗi lớp con phải cài rất nhiều; cây kế thừa sâu.

### 4.4 Command

> **Ý định:** đóng gói **một yêu cầu thành object** — để xếp hàng, ghi log, hoàn tác (undo), truyền đi, thực thi trễ.

```java
interface Command { void execute(); default void undo() { throw new UnsupportedOperationException(); } }

class AddTextCommand implements Command {
    private final Document doc; private final String text;
    AddTextCommand(Document doc, String text) { this.doc = doc; this.text = text; }
    public void execute() { doc.append(text); }
    public void undo()    { doc.deleteLast(text.length()); }
}

class History {
    private final Deque<Command> done = new ArrayDeque<>();
    void run(Command c) { c.execute(); done.push(c); }
    void undo()         { if (!done.isEmpty()) done.pop().undo(); }
}
```

- **JDK:** `Runnable`/`Callable` **chính là Command** — `executor.submit(runnable)` xếp một Command vào hàng đợi để thread pool chạy sau (Module 05.2). `javax.swing.Action`.
- **Spring:** handler của message queue, `ApplicationRunner`/`CommandLineRunner`, các "use case object" trong kiến trúc hexagonal.
- **Dấu hiệu nên dùng:** cần undo/redo, transaction log, hàng đợi tác vụ, lập lịch, macro (gộp nhiều lệnh), tách "ai yêu cầu" khỏi "ai thực thi".
- **Khi KHÔNG nên dùng:** chỉ gọi một method đồng bộ ngay — bọc thành Command là nghi thức thừa; với callback đơn giản, lambda `Runnable` là đủ, không cần class.
- **Bẫy:** quản lý state cho `undo()` phức tạp hơn tưởng (phải lưu đủ thông tin khôi phục); Command ôm luôn nghiệp vụ nặng thay vì ủy quyền cho receiver.

### 4.5 Iterator

> **Ý định:** duyệt tuần tự các phần tử của một tập hợp mà **không lộ cấu trúc bên trong** nó.

```java
for (User u : users) { ... }                       // đường ngắn — cần Iterable
Iterator<User> it = users.iterator();
while (it.hasNext()) { User u = it.next(); if (bad(u)) it.remove(); }   // remove an toàn khi đang duyệt
```

- **JDK:** toàn bộ `Collection` là `Iterable`; `Iterator`, `ListIterator`, `Spliterator` (nền của Stream song song), `Scanner`, `Files.newDirectoryStream`.
- **Fail-fast (Module 03.1):** sửa collection trong lúc duyệt bằng cách khác `it.remove()` → `ConcurrentModificationException`. `CopyOnWriteArrayList`/`ConcurrentHashMap` cho iterator *weakly consistent* (không ném).
- **Internal vs external iteration:** `Iterator` là external (client điều khiển vòng lặp); `Stream`/`forEach` là internal (thư viện điều khiển — dễ song song hóa, Module 03.3).
- **Dấu hiệu nên dùng:** bạn viết một kiểu tập hợp mới và muốn nó dùng được với `for-each`; cần nhiều cách duyệt (xuôi/ngược/lọc) trên cùng cấu trúc.
- **Khi KHÔNG nên dùng:** đã có `Collection`/`Stream` chuẩn — đừng tự viết iterator; duyệt một `List` thường → cứ `for-each` hoặc Stream.
- **Bẫy:** quên cài `remove()` (mặc định ném `UnsupportedOperationException`); iterator vô hạn không có điều kiện dừng; giữ iterator sống lâu qua nhiều thay đổi của collection.

### 4.6 State

> **Ý định:** cho object **đổi hành vi khi trạng thái nội bộ đổi**, như thể nó đổi class — thay cho một `switch` khổng lồ trên cột `status`.

```java
interface OrderState {
    OrderState pay(Order o);
    OrderState ship(Order o);
}
class New implements OrderState {
    public OrderState pay(Order o)  { o.chargeCard(); return new Paid(); }
    public OrderState ship(Order o) { throw new IllegalStateException("Chưa thanh toán"); }
}
class Paid implements OrderState {
    public OrderState pay(Order o)  { throw new IllegalStateException("Đã thanh toán"); }
    public OrderState ship(Order o) { o.createShipment(); return new Shipped(); }
}
// Order giữ 'OrderState state' và ủy quyền: void pay() { this.state = state.pay(this); }
```

- **State vs Strategy:** cấu trúc song sinh (đều composition + ủy quyền). Khác: State **tự chuyển** giữa các trạng thái và các trạng thái biết về nhau; Strategy do client chọn, các strategy độc lập.
- **Spring:** Spring Statemachine (chỉ nhắc tên) cho FSM phức tạp; nhiều hệ thống chỉ cần `enum` + bảng chuyển tiếp.
- **Dấu hiệu nên dùng:** cùng một hành động cho kết quả khác nhau tùy trạng thái, có ≥ 3–4 trạng thái với luật chuyển tiếp rõ; các `switch (status)` trùng lặp nằm rải rác.
- **Khi KHÔNG nên dùng:** 2 trạng thái, luật đơn giản → `boolean`/`enum` + một `switch` là đủ và dễ đọc hơn.
- **Bẫy:** bùng nổ số class trạng thái; luật chuyển tiếp phân tán khắp các class State khó nhìn tổng thể (cân nhắc một bảng transition tập trung); nhầm khi trạng thái chỉ là dữ liệu, không kéo theo đổi hành vi.

### 4.7 Chain of Responsibility

> **Ý định:** cho **nhiều handler nối thành chuỗi** cùng có cơ hội xử lý một request; mỗi handler tự quyết định xử lý rồi dừng, hoặc chuyển tiếp.

```java
abstract class Handler {
    private Handler next;
    Handler linkTo(Handler n) { this.next = n; return n; }
    protected boolean passToNext(Request r) { return next == null || next.handle(r); }
    abstract boolean handle(Request r);
}
class AuthHandler extends Handler {
    boolean handle(Request r) {
        if (!r.hasToken()) return false;          // chặn — dừng chuỗi
        return passToNext(r);
    }
}
class RateLimitHandler extends Handler {
    boolean handle(Request r) { return withinLimit(r) && passToNext(r); }
}
```

- **Spring/JDK:** Servlet `Filter` + `FilterChain.doFilter()`, **Spring Security filter chain**, `HandlerInterceptor` (`preHandle` trả `false` để chặn), `OncePerRequestFilter`, Netty `ChannelPipeline`. `java.util.logging` cũng chuyển log lên chuỗi handler cha.
- **Dấu hiệu nên dùng:** một request qua nhiều bước tiền xử lý độc lập, số lượng/thứ tự bước cấu hình được (auth, CORS, log, rate limit, nén); muốn thêm/bớt bước không sửa bước khác (OCP).
- **Khi KHÔNG nên dùng:** luôn chạy đúng một chuỗi cố định, ngắn → gọi tuần tự tường minh dễ đọc hơn.
- **Bẫy:** request "rơi khỏi" cuối chuỗi mà không ai xử lý (không có handler mặc định); chuỗi quá dài/mờ, khó biết ai đã chặn; thứ tự nhạy cảm mà không được tài liệu hóa.

### 4.8 Mediator

> **Ý định:** gom **giao tiếp nhiều-nhiều** giữa một nhóm object vào **một object trung gian**; các thành viên chỉ nói chuyện với mediator, không nói trực tiếp với nhau.

```java
interface ChatRoom { void send(String from, String msg); }

class ChatRoomImpl implements ChatRoom {
    private final Map<String, User> users = new HashMap<>();
    void join(User u) { users.put(u.name(), u); u.setRoom(this); }
    public void send(String from, String msg) {
        users.values().stream().filter(u -> !u.name().equals(from))
             .forEach(u -> u.receive(from, msg));      // mediator định tuyến, User không biết nhau
    }
}
```

- **Mediator vs Observer:** Observer là kênh phát một chiều, publisher không quan tâm ai nghe; Mediator điều phối **hai chiều, có logic định tuyến/điều kiện**, thường biết đủ mọi thành viên.
- **JDK/Spring:** `ExecutorService` điều phối task ↔ worker; `DispatcherServlet` điều phối request ↔ controller/view; `ApplicationEventMulticaster` bên trong Spring events.
- **Dấu hiệu nên dùng:** N thành phần tham chiếu chằng chịt lẫn nhau (đồ thị giao tiếp gần đầy đủ), thêm một thành phần phải sửa nhiều chỗ; UI form với nhiều widget ràng buộc.
- **Khi KHÔNG nên dùng:** chỉ vài thành phần, quan hệ thưa và rõ — thêm mediator chỉ thêm một tầng.
- **Bẫy:** mediator hút hết logic điều phối thành **God object** (Module 01.6 §9); mọi thay đổi giao tiếp lại dồn về một file duy nhất.

### 4.9 Memento

> **Ý định:** chụp và khôi phục **trạng thái nội bộ** của một object mà **không phá vỡ đóng gói** (không lộ field ra ngoài).

```java
final class Editor {
    private String content = "";
    void type(String s) { content += s; }
    Memento save()               { return new Memento(content); }      // originator tạo memento
    void restore(Memento m)      { this.content = m.content(); }
    record Memento(String content) { }                                 // state đông cứng, chỉ Editor đọc được nghĩa
}
// Caretaker chỉ giữ Deque<Memento>, không hiểu bên trong
```

- **Ứng dụng:** undo/redo, checkpoint/rollback, snapshot cấu hình, "hoàn tác" trong wizard nhiều bước.
- **Serialization như cơ chế memento:** đóng băng cả đồ thị object thành byte rồi khôi phục (nhưng nặng, vấn đề versioning, bảo mật — Module 04).
- **Memento vs Prototype:** Prototype sao chép để tạo *object mới dùng song song*; Memento lưu để *khôi phục chính object cũ* về sau.
- **Dấu hiệu nên dùng:** cần quay lui trạng thái, và bạn muốn giữ field `private` (không phơi getter/setter chỉ để lưu).
- **Khi KHÔNG nên dùng:** object đã bất biến → chỉ cần giữ lại tham chiếu bản cũ; state nhỏ và công khai → copy thẳng.
- **Bẫy:** mỗi snapshot copy sâu state lớn → tốn RAM (cân nhắc lưu *diff*/command thay vì full state); memento shallow copy vẫn dính tham chiếu chung.

### 4.10 Visitor

> **Ý định:** thêm **thao tác mới** lên một cây object có **tập kiểu cố định**, mà không sửa các class kiểu đó — dùng **double dispatch**.

```java
interface Node { <R> R accept(Visitor<R> v); }
record Num(double val) implements Node { public <R> R accept(Visitor<R> v) { return v.visitNum(this); } }
record Add(Node l, Node r) implements Node { public <R> R accept(Visitor<R> v) { return v.visitAdd(this); } }

interface Visitor<R> { R visitNum(Num n); R visitAdd(Add a); }

class EvalVisitor implements Visitor<Double> {
    public Double visitNum(Num n) { return n.val(); }
    public Double visitAdd(Add a) { return a.l().accept(this) + a.r().accept(this); }
}
```

#### Java hiện đại: `sealed` + pattern-matching `switch` thay Visitor (Module 06)

```java
sealed interface Node permits Num, Add { }
record Num(double val) implements Node { }
record Add(Node l, Node r) implements Node { }

double eval(Node n) {
    return switch (n) {                              // exhaustiveness do compiler kiểm tra
        case Num(double v)      -> v;
        case Add(Node l, Node r) -> eval(l) + eval(r);
    };
}
```

- **Expression problem:** Visitor giúp **thêm thao tác dễ** (thêm một class Visitor) nhưng **thêm kiểu khó** (sửa mọi Visitor). `switch` trên `sealed` thì ngược: thêm kiểu → compiler chỉ ra mọi `switch` cần sửa; thêm thao tác → thêm một method. Chọn theo trục nào hay thay đổi hơn.
- **JDK:** `java.nio.file.FileVisitor`/`SimpleFileVisitor` (`Files.walkFileTree`), `javax.lang.model.element.ElementVisitor` (annotation processor), `DoubleVisitor` trong một số lib.
- **Dấu hiệu nên dùng:** cây kiểu **ổn định** (AST, DOM), nhiều thao tác khác nhau (in, tối ưu, kiểm tra kiểu, sinh mã); hoặc hierarchy do bên thứ ba sở hữu (không `sealed` được).
- **Khi KHÔNG nên dùng:** bạn sở hữu hierarchy và nó `sealed` được, Java 17+ → dùng pattern switch, ít boilerplate hơn hẳn; tập kiểu hay thay đổi.
- **Bẫy:** thêm một node type phải sửa mọi Visitor; `accept`/`visitXxx` bùng nổ; khó đọc với người chưa quen double dispatch.

### 4.11 Null Object

> **Ý định:** thay `null` bằng một object **"không làm gì" hợp lệ** cài cùng interface, để client khỏi rải `if (x != null)`.

```java
interface AuditLog { void record(String event); }
enum NoAuditLog implements AuditLog { INSTANCE; public void record(String e) { /* no-op */ } }

class OrderService {
    private final AuditLog audit;
    OrderService(AuditLog audit) { this.audit = (audit != null) ? audit : NoAuditLog.INSTANCE; }
    void place() { /* ... */ audit.record("PLACED"); }   // không cần null-check
}
```

- **JDK/Spring:** `Collections.emptyList()`/`emptyMap()` (null object cho collection), `Optional.empty()` (họ hàng gần), SLF4J `NOPLogger`, `OutputStream.nullOutputStream()` (Java 11), `DataBinder` no-op validators.
- **Null Object vs Optional:** Optional **buộc** caller xử lý sự vắng mặt (kiểu trả về); Null Object **giấu** sự vắng mặt sau hành vi trung tính (thường là collaborator/dependency). Đừng bọc "không tìm thấy dữ liệu" bằng Null Object — dùng `Optional` (Module 06 §1).
- **Dấu hiệu nên dùng:** một collaborator tùy chọn (logger, metrics, listener), "không có" nghĩa là "bỏ qua nhẹ nhàng".
- **Khi KHÔNG nên dùng:** sự vắng mặt là lỗi cần biết → để nó nổ hoặc trả `Optional`; hành vi "rỗng" không thật sự trung tính (nuốt mất tiền, mất dữ liệu).
- **Bẫy:** null object nuốt lỗi im lặng khiến bug khó lần; lỡ dùng cho kết quả truy vấn làm caller tưởng "có, rỗng" trong khi đúng ra là "không có".

### 4.12 Interpreter (ngắn)

> **Ý định:** định nghĩa **văn phạm** cho một ngôn ngữ nhỏ và một bộ máy **diễn giải** các câu theo văn phạm đó (thường là cây gồm các biểu thức, mỗi loại có `interpret(context)`).

Hiếm khi tự viết tay ngoài bài tập — với văn phạm thật, dùng ANTLR/JavaCC hoặc thư viện có sẵn. Ví dụ thực tế trong nền tảng: `java.util.regex.Pattern` (diễn giải regex), `java.time.format.DateTimeFormatter`, **Spring Expression Language (SpEL)**, `MessageFormat`.

- **Khi KHÔNG nên dùng:** văn phạm phức tạp/hay đổi → Interpreter thủ công phình rất nhanh; hiệu năng kém (đi cây mỗi lần). Chọn parser generator hoặc DSL bằng chính Java (builder/lambda).
- **Bẫy:** nhầm mọi cấu trúc cây với Interpreter; không tách rõ *parse* (chuỗi → cây) và *interpret* (cây → kết quả).

---

## 5. Dependency Injection & IoC — nền của Spring

DI/IoC **không phải pattern GoF**, mà là nguyên tắc kiến trúc — nhưng là "keo dán" khiến Factory, Strategy, Observer, Proxy trong Spring hoạt động tự động. Đã học kỹ ở **Module 01.6 §6–§7**; ở đây chỉ chốt liên hệ.

```java
// KHÔNG DI — tự tạo dependency, coupling cao, khó test
class OrderService { private final EmailSender sender = new EmailSender(); }

// CÓ DI — nhận abstraction từ ngoài (constructor injection — mặc định nên chọn)
class OrderService {
    private final NotificationSender sender;
    OrderService(NotificationSender sender) { this.sender = sender; }
}
```

- **IoC:** quyền quyết định "tạo gì, khi nào, tiêm vào đâu" chuyển từ object sang một container bên ngoài.
- **DI là một cách hiện thực IoC** cho việc lắp ráp object; có thể đạt DIP **không cần container** — chỉ cần constructor nhận abstraction + một *composition root* lắp ráp (Module 01.6 §7).
- **Service Locator là phản đề nên tránh** (`Locator.get(X.class)` bên trong logic) — giấu dependency, khó test (Module 01.6 §7).
- Chi tiết Bean scope, `@Autowired`, `@Configuration`, container lifecycle → **Module 21**.

---

## 6. Pattern và tính năng ngôn ngữ hiện đại

Nhiều pattern GoF ra đời khi Java chưa có lambda, `enum` giàu hành vi, `sealed`, `record`. Ngày nay phần "khung" của chúng thường thu về vài dòng — hiểu pattern vẫn cần, nhưng **đừng viết bản 5-class khi ngôn ngữ đã cho bản 1-dòng**.

| Pattern cổ điển | Bản hiện đại | Ghi chú |
|---|---|---|
| Strategy / Command / callback Template | **Lambda + functional interface** | `Comparator`, `Runnable`, `Predicate`, `Function` — không cần class riêng cho mỗi biến thể |
| Visitor trên hierarchy đóng | **`sealed` + `record` + pattern-matching `switch`** (Module 06) | Compiler kiểm tra đủ nhánh; thêm kiểu → mọi `switch` báo lỗi |
| State / máy trạng thái nhỏ | **`enum` có method** + bảng transition | Đủ khi tập trạng thái đóng, ít |
| Singleton | **`enum INSTANCE`** (Effective Java Item 3) hoặc **Spring bean** | Chống reflection/serialization sẵn |
| Flyweight | **`enum`**, cache `valueOf`, String pool, `record` bất biến | JDK làm sẵn cho nguyên thủy boxing |
| Null Object | **`Optional`** cho kiểu trả về; Null Object cho collaborator | Hai vai trò khác nhau (§4.11) |
| Factory Method cho object đơn giản | **Method reference làm `Supplier<T>`** | `Map<Key, Supplier<T>>` thay cây Creator |
| Builder | **`record`** (khi ít field bắt buộc), Lombok `@Builder` | `record` = target bất biến lý tưởng của builder |
| Iterator | **Stream / `Iterable` + for-each** | Internal iteration, song song hóa dễ |
| Prototype (`clone`) | **Copy constructor / copy factory / `record` + `withX`** | Tránh mọi cạm bẫy `clone()` (Module 02.4) |

> Quy tắc: pattern mô tả **ý định**; cú pháp hiện đại là **cách rẻ hơn** để đạt ý định đó. Nói "chỗ này là Strategy" vẫn đúng dù nó chỉ là một lambda.

---

## 7. Design Patterns trong Spring

Gần như mọi tính năng cốt lõi của Spring là một pattern GoF được đóng gói sẵn:

| Cơ chế Spring | Pattern(s) | Bản chất |
|---|---|---|
| `ApplicationContext` / `BeanFactory` | **Factory Method + Abstract Factory** | Container là "nhà máy" tạo & lắp ráp mọi bean |
| Bean scope `singleton` (mặc định) | **Singleton** | Một instance mỗi container (không phải mỗi ClassLoader) |
| Bean scope `prototype` | **Prototype** (tên) / thực ra là "instance mới mỗi lần lấy" | Cẩn thận nghĩa khác GoF |
| `@Transactional`, `@Async`, `@PreAuthorize` | **Proxy** | JDK dynamic proxy (có interface) hoặc CGLIB (không interface) |
| `@Cacheable` | **Proxy + Decorator** | Bọc thêm tra cache trước khi gọi thật |
| `JdbcTemplate`, `RestClient`, `JmsTemplate` | **Facade + Template Method** (+ **Strategy** qua `RowMapper`) | Nuốt tài nguyên & boilerplate, chừa lại bước biến thiên |
| `ApplicationEventPublisher` + `@EventListener` | **Observer** | Publisher không biết subscriber |
| Servlet `Filter`, Spring Security filter chain, `HandlerInterceptor` | **Chain of Responsibility** | Mỗi mắt xích xử lý rồi dừng, hoặc chuyển tiếp |
| Inject `List<T>` / `Map<String,T>` cùng interface, `@Qualifier`, `@Primary` | **Strategy** | Chọn implementation lúc runtime |
| `RowMapper`, `ResultSetExtractor`, `Comparator`, `ResponseErrorHandler` | **Strategy** (thường là lambda) | Thuật toán truyền vào |
| `DispatcherServlet` | **Mediator + Front Controller** | Điều phối request ↔ handler ↔ view |
| `BeanDefinitionBuilder`, `UriComponentsBuilder`, `MockMvcRequestBuilders` | **Builder** | Dựng object cấu hình phức tạp |
| `FactoryBean<T>` | **Factory Method** (điểm mở rộng) | Khi tạo bean quá phức tạp cho `@Bean` thường |
| `HandlerAdapter` | **Adapter** | Nhiều kiểu controller qua một interface |
| `CompositeCacheManager`, `WebMvcConfigurerComposite` | **Composite** | Gộp nhiều thể hiện cùng interface |

> Đọc source Spring dễ hơn nhiều khi nhận ra: `XxxTemplate` → Facade/Template; `XxxProxy`/annotation cắt ngang → Proxy; `XxxFactory`/`XxxBuilder` → tên đã nói; `Composite*`/`Delegating*` → Composite/Decorator.

---

## 8. Anti-pattern và over-engineering

Pattern dùng sai chỗ tệ hơn không dùng. Các "mùi" hay đi kèm việc lạm dụng pattern (nhiều cái đã bàn ở Module 01.6 §9, §11):

| Anti-pattern | Biểu hiện | Thuốc chữa |
|---|---|---|
| **God object / God facade** | Một class/facade ôm mọi trách nhiệm, nghìn dòng, tên `Manager`/`Processor`/`Helper` | Tách theo *nguồn thay đổi* (SRP, Module 01.6 §2) |
| **Premature abstraction** | Interface + factory + strategy cho biến thiên **chưa từng xảy ra**; mỗi class một `XxxImpl` | Chấp nhận lặp tới lần thứ ba rồi mới trừu tượng (Module 01.6 §11) |
| **Pattern for pattern's sake** | Nhồi Visitor/Bridge/Command vào bài toán ba dòng để "cho pro" | Hỏi: pattern này hấp thụ *thay đổi nào*? Không có → bỏ |
| **Singleton như global mutable state** | `X.getInstance().setThing(...)` rải khắp nơi | Bean có scope + DI; state đẩy ra ngoài, giữ Singleton bất biến |
| **Anemic domain model** | Entity chỉ có getter/setter; toàn bộ luật nằm trong `*Service` | Đưa invariant về domain object (như `BankAccount`, Module 01.6 §2) |
| **Service Locator** | Component tự `Locator.get(Dep.class)` thay vì nhận qua constructor | Constructor injection — dependency hiện diện, tường minh (Module 01.6 §7) |
| **Poltergeist / lasagna** | Quá nhiều lớp gián tiếp chỉ để chuyển tiếp lời gọi; đọc code phải nhảy 6 file | Gỡ tầng không mang giá trị; một class 200 dòng mạch lạc > 8 file 25 dòng |
| **Yo-yo problem** | Cây kế thừa Template Method quá sâu, đọc phải lên xuống liên tục | Ưu tiên composition/callback |

> Câu hỏi quyết định luôn là *"thay đổi nào sắp tới và nó tốn bao nhiêu nếu không chuẩn bị"*, không phải *"đã đủ nhiều pattern chưa"* (Module 01.6 §11).

---

## 9. Chọn pattern thế nào

Đóng khung theo **loại thay đổi bạn muốn hấp thụ**:

| Bạn dự kiến sẽ thêm... | Nghiêng về | Vì sao |
|---|---|---|
| Một **thuật toán/chính sách** mới cho một bước | **Strategy** (hoặc lambda / `enum` hành vi) | Hoán bước đó, code gọi không đổi |
| Một **bước mới trong một trình tự cố định** | **Template Method** + hook, hoặc **callback** | Khung giữ nguyên, chỉ điền bước |
| Một **kiểu mới trong tập ĐÓNG** (bạn sở hữu) | **`sealed` + pattern `switch`** (Module 06) | Compiler chỉ ra mọi chỗ cần cập nhật |
| Một **kiểu mới trong tập MỞ** (bên thứ ba, plugin) | **Đa hình / interface**, hoặc **Visitor** nếu tập thao tác cũng lớn | Không sửa được các kiểu có sẵn |
| Một **lớp hành vi bọc thêm** (buffer, nén, retry, log) có thể kết hợp | **Decorator** | Xếp chồng runtime, tránh bùng nổ lớp con |
| Một **mối quan tâm cắt ngang** (transaction, cache, security) đồng đều nhiều method | **Proxy / AOP** | Một chỗ, không rải khắp nghiệp vụ |
| Một **bước tiền xử lý** vào pipeline request | **Chain of Responsibility** | Thêm/bớt mắt xích không đụng mắt xích khác |
| Một **phản ứng mới** cho một sự kiện nghiệp vụ | **Observer / ApplicationEvent** | Publisher không cần biết subscriber |
| Một **trạng thái mới** với luật chuyển tiếp | **State** (nếu ≥ 3–4 trạng thái) | Gom hành vi theo trạng thái, bỏ `switch` rải rác |
| Một **cách tạo object** phức tạp / nhiều tham số | **Builder** (nhiều optional) / **Factory** (chọn loại) | Tách khởi tạo khỏi sử dụng |
| Một **nguồn dữ liệu ngoài** interface lệch chuẩn | **Adapter** | Cô lập sự lệch vào một chỗ |
| Hai **trục biến thiên vuông góc** cùng nhiều biến thể | **Bridge** | `m + n` lớp thay vì `m × n` |

Nếu **không** thấy trục thay đổi rõ ràng: viết code trực tiếp nhất có thể, để lại "seam" ở ranh giới I/O ngoài (thanh toán, mail, lưu trữ) vì gần như chắc chắn cần test double ở đó (Module 01.6 §11).

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Pattern | Ý định một câu | Dấu hiệu nên dùng | Ví dụ JDK / Spring | Bẫy thường gặp |
|---|---|---|---|---|
| **Singleton** | Đúng một instance, điểm truy cập toàn cục | Tài nguyên dùng chung, vô trạng thái | `enum` singleton; Spring bean scope | Global mutable state, khó test, service locator trá hình |
| **Factory Method** | Lớp con quyết định class được tạo | Khung cố định, sản phẩm biến thiên | `Collection.iterator()`, `@Bean`, `FactoryBean` | Cây Creator song song; nhầm với Abstract Factory |
| **Abstract Factory** | Tạo cả một họ object khớp nhau | Sản phẩm đi theo bộ/nền tảng | `DocumentBuilderFactory`; `@Profile` chọn bộ bean | Thêm sản phẩm → sửa mọi factory; bùng nổ class |
| **Builder** | Dựng object nhiều tham số theo bước | ≥ 4–5 field, nhiều optional, bất biến | `HttpRequest.newBuilder()`, `UriComponentsBuilder`, Lombok `@Builder` | Quên validate `build()`; builder mutable chia sẻ; required ở chaining |
| **Prototype** | Tạo mới bằng sao chép mẫu | Khởi tạo đắt, cần nhiều bản gần giống | `ArrayList(Collection)`; `@Scope("prototype")` (nghĩa khác) | Shallow copy dính state chung; `clone()` bỏ qua constructor |
| **Adapter** | Dịch interface không tương thích | Tích hợp SDK bên thứ ba lệch chuẩn | `Arrays.asList`, `InputStreamReader`, `HandlerAdapter` | Adapter chứa nghiệp vụ; rò kiểu thư viện cũ ra ngoài |
| **Decorator** | Bọc thêm hành vi động, giữ interface | Nhiều "trang sức" kết hợp tự do | `java.io` streams, `Collections.unmodifiableList` | Nhiều lớp mỏng khó debug; thứ tự bọc; định danh bị phá |
| **Facade** | Một interface đơn giản cho subsystem phức tạp | Nhiều bước/class, client chỉ cần vài kịch bản | `JdbcTemplate`, `RestClient` | Phình thành God object; rò kiểu nội bộ |
| **Proxy** | Đại diện kiểm soát truy cập object thật | Mối quan tâm cắt ngang; lazy; bảo vệ; từ xa | Dynamic proxy/CGLIB; `@Transactional`, `@Cacheable` | Self-invocation; method `final`/`private`; `this` ≠ proxy |
| **Composite** | Đối xử lá và cành như nhau trong cây | Dữ liệu là cây, thao tác = duyệt + tổng hợp | `java.awt.Container`, `CompositeCacheManager` | Lá bị ép cài `add`/`remove` (ISP); chu trình |
| **Bridge** | Tách abstraction khỏi implementation | Hai trục biến thiên vuông góc, nhiều biến thể | JDBC `Driver`/`DriverManager`, SLF4J ↔ Logback | Dựng khi mới một impl (trừu tượng sớm); nhầm với Adapter |
| **Flyweight** | Chia sẻ state bất biến giữa vô số object | Hàng trăm nghìn object, lõi bất biến lớn | `Integer.valueOf` cache, String pool, `enum` | Flyweight mutable; dựa `==` ngoài dải cache; tối ưu sớm |
| **Strategy** | Hoán đổi thuật toán lúc runtime | Nhiều cách tính, chọn theo cấu hình/loại | `Comparator`, inject `List<T>`, `RowMapper` | Chỉ một impl mãi mãi; nhồi quá nhiều context vào chữ ký |
| **Observer** | Tự động báo cho mọi bên đăng ký | Một sự kiện → nhiều phản ứng độc lập | `ApplicationEventPublisher` + `@EventListener` | Lapsed listener rò bộ nhớ; listener ném exception; thứ tự |
| **Template Method** | Khung thuật toán ở cha, bước ở con | Nhiều biến thể chung một trình tự | `AbstractList`, `HttpServlet.service()`, `*Template` | Fragile base class; quá nhiều bước abstract; kế thừa sâu |
| **Command** | Đóng gói yêu cầu thành object | Undo/redo, hàng đợi, log, lập lịch | `Runnable`/`Callable` + `ExecutorService` | State cho `undo()` phức tạp; bọc thừa cho lời gọi đơn giản |
| **Iterator** | Duyệt tuần tự không lộ cấu trúc | Viết kiểu tập hợp mới; nhiều cách duyệt | `Iterable`/`Iterator`, `Spliterator`, `Scanner` | Fail-fast `CME`; quên cài `remove()`; iterator vô hạn |
| **State** | Đổi hành vi khi trạng thái đổi | ≥ 3–4 trạng thái, luật chuyển tiếp rõ | Order lifecycle; Spring Statemachine | Bùng nổ class; luật chuyển tiếp phân tán; state chỉ là dữ liệu |
| **Chain of Responsibility** | Chuỗi handler cùng có cơ hội xử lý | Pipeline tiền xử lý cấu hình được | Servlet `Filter`, Spring Security chain, `HandlerInterceptor` | Request rơi khỏi chuỗi; thứ tự nhạy cảm; chuỗi mờ |
| **Mediator** | Gom giao tiếp nhiều-nhiều vào trung gian | Đồ thị giao tiếp gần đầy đủ giữa N thành phần | `DispatcherServlet`, `ExecutorService` | Mediator thành God object |
| **Memento** | Chụp/khôi phục state không phá đóng gói | Cần undo, giữ field `private` | Serialization; undo stack | Snapshot state lớn tốn RAM; shallow copy dính tham chiếu |
| **Visitor** | Thêm thao tác lên cây kiểu cố định | Cây kiểu ổn định, nhiều thao tác; hierarchy bên thứ ba | `FileVisitor`, `ElementVisitor` | Thêm kiểu → sửa mọi Visitor; boilerplate; `sealed`+`switch` thường thay được |
| **Null Object** | Object "không làm gì" hợp lệ thay `null` | Collaborator tùy chọn, "không có" = bỏ qua nhẹ | `Collections.emptyList()`, SLF4J `NOPLogger` | Nuốt lỗi im lặng; dùng nhầm cho kết quả truy vấn (nên `Optional`) |
| **Interpreter** | Văn phạm + bộ diễn giải câu | Ngôn ngữ nhỏ, ổn định | `regex.Pattern`, `DateTimeFormatter`, SpEL | Văn phạm phức tạp phình nhanh; nên dùng parser generator |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn nào là Strategy, đoạn nào là State? Giải thích điểm khác biệt cốt lõi.
```java
// (a)
list.sort((x, y) -> x.priority() - y.priority());
// (b)
class Task { private Status s = Status.OPEN;
    void start() { s = s.start(this); }   // Status.OPEN.start() trả Status.IN_PROGRESS
}
```

**Câu 2.** Class sau tự nhận là "áp dụng Singleton Pattern cho pro". Vấn đề gì? Trong Spring nên làm thế nào?
```java
public class SessionRegistry {
    private static final SessionRegistry I = new SessionRegistry();
    public static SessionRegistry get() { return I; }
    private final Map<String, User> active = new HashMap<>();
    public void add(String id, User u) { active.put(id, u); }
}
```

**Câu 3.** Đoạn này dùng pattern gì, và tại sao `@Transactional` trên `recalcAll()` **không có tác dụng**?
```java
@Service
class ReportService {
    @Transactional public void generate() { recalcAll(); }
    @Transactional public void recalcAll() { /* ... */ }
}
```

**Câu 4.** `new BufferedReader(new InputStreamReader(new FileInputStream(f)))` — chỉ ra chỗ nào là Decorator, chỗ nào là Adapter. Vì sao?

**Câu 5.** Đoạn sau "mùi" gì (anti-pattern nào)? Sửa hướng nào?
```java
interface UserService { User find(Long id); }
class UserServiceImpl implements UserService {           // interface một-impl, không test nào mock
    public User find(Long id) { return repo.findById(id).orElseThrow(); }
}
```

**Câu 6.** Cho `sealed interface Shape permits Circle, Square` và một `AreaVisitor`. Có nên giữ Visitor không, hay chuyển sang `switch`? Nêu tiêu chí quyết định (trục "thêm kiểu" vs "thêm thao tác").

**Câu 7.** Đoạn nào minh họa Flyweight? Vì sao `c1 == c2` cho `false`?
```java
Integer a1 = 127, a2 = 127;      // a1 == a2 ?
Integer c1 = 1000, c2 = 1000;    // c1 == c2 ?
```

**Câu 8.** `OrderService` giữ `List<OrderListener>` bằng `ArrayList`, có `register()` nhưng không có `unregister()`. Nêu hai vấn đề (một về đa luồng, một về bộ nhớ) và cách khắc phục.

---

### Phần B — Bài tập viết code

**Bài 1 — Singleton: chọn cách và chứng minh.**
Viết `AppLogger` singleton (chọn giữa holder idiom và `enum` — giải thích lý do), method `log(String)` in kèm timestamp. Viết `main` tạo 100 thread cùng gọi `AppLogger.getInstance()`, gom kết quả vào một `Set` theo `System.identityHashCode`, khẳng định `set.size() == 1`. Thêm một câu bình luận: vì sao trong dự án Spring bạn sẽ không viết class này.

**Bài 2 — Builder bất biến + required + validate.**
Viết `record`-hoặc-class `HttpRequestConfig`: `url` (bắt buộc), `method` (mặc định `GET`), `headers` (`Map`, mặc định rỗng, bất biến), `timeoutMs` (mặc định 5000), `retries` (mặc định 0). Dùng Builder: `url` truyền qua `HttpRequestConfig.to(url)`, các field khác qua chaining. `build()` ném `IllegalStateException` nếu `timeoutMs <= 0` hoặc `retries < 0`. Chứng minh object bất biến sau `build()` (thử sửa `headers` phải ném).

**Bài 3 — Strategy bằng `Map<String, Supplier>` thay `switch`.**
Cho `NotificationFactory.create(String type)` đang dùng `switch` cho `EMAIL`/`SMS`/`PUSH`. Refactor sang `Map<String, Supplier<NotificationSender>>` đăng ký sẵn; thêm loại `SLACK` chỉ bằng một dòng đăng ký, không sửa method `create`. Viết test cho trường hợp `type` không tồn tại.

**Bài 4 — Decorator: đo lường + retry cho một `DataSource`.**
Cho `interface PriceFeed { BigDecimal get(String symbol); }` và `RemotePriceFeed` (giả lập chậm, thỉnh thoảng ném). Viết hai decorator: `TimingPriceFeed` (log thời gian) và `RetryingPriceFeed` (thử tối đa 3 lần khi ném). Lắp `new TimingPriceFeed(new RetryingPriceFeed(new RemotePriceFeed()))` và giải thích vì sao đổi thứ tự hai lớp bọc cho ý nghĩa đo lường khác nhau.

**Bài 5 — State cho vòng đời đơn hàng.**
Mô hình `Order` với 4 trạng thái (`NEW`, `PAID`, `SHIPPED`, `CANCELLED`) và các hành động `pay()`, `ship()`, `cancel()`. Cài bằng State pattern (mỗi trạng thái một class implement `OrderState`). Hành động không hợp lệ ném `IllegalStateException` với thông báo rõ. Viết `main` chạy một luồng hợp lệ (`NEW → PAID → SHIPPED`) và một luồng sai (`NEW → ship()` phải ném). So sánh ngắn với bản `switch (status)`.

**Bài 6 — Bài tổng hợp: pipeline xử lý request kết hợp nhiều pattern.**
Dựng luồng xử lý một "đơn hàng" kết hợp:
- **Chain of Responsibility:** `StockHandler → PaymentHandler → FraudHandler`, mỗi handler có thể chặn và dừng chuỗi.
- **Strategy:** sau khi qua hết chuỗi, áp một `ShippingPolicy` (`Standard`/`Express`) để tính phí.
- **Observer:** phát sự kiện `OrderPlaced` cho `EmailListener` và `InventoryListener` (dùng `CopyOnWriteArrayList`, có `unregister`).
- **Proxy/Decorator (chọn một):** bọc handler `PaymentHandler` bằng một lớp đo thời gian, không sửa code `PaymentHandler`.

Viết `main` mô phỏng: (1) đơn hợp lệ đi hết chuỗi, kích hoạt observer, tính phí Express; (2) đơn bị chặn ở `PaymentHandler` — chứng minh observer **không** chạy. Ghi chú mỗi chỗ đang là pattern nào và nó hấp thụ thay đổi gì.

---

### Phần C — Nâng cao

**Câu 1.** So sánh ba cách viết Singleton lazy: double-checked locking (`volatile`), holder idiom, `enum`. Với mỗi cách nêu: nó lazy tới mức nào, có chống được reflection/serialization không, và một tình huống cụ thể khiến bạn chọn nó thay vì hai cách kia. Vì sao thiếu `volatile` trong DCL là bug (liên hệ Module 05.2)?

**Câu 2.** "Strategy và State có cấu trúc UML gần như giống hệt nhau." Chỉ ra điểm khác nhau về **ý định** và **luồng điều khiển** (ai chọn implementation, các implementation có biết về nhau không, có tự chuyển tiếp không). Cho một ví dụ mà bắt đầu là Strategy rồi tiến hóa thành State.

**Câu 3.** Với một `sealed interface Json permits JNull, JBool, JNum, JStr, JArr, JObj` mà bạn sở hữu: lập luận vì sao `switch` pattern-matching (Module 06) tốt hơn Visitor cho việc viết `render`, `validate`, `deepEquals`. Sau đó nêu một tình huống mà Visitor **vẫn** thắng (gợi ý: hierarchy do thư viện bên thứ ba định nghĩa, hoặc tập thao tác lớn hơn nhiều tập kiểu). Đây là "expression problem" — phát biểu nó.

**Câu 4.** `@Cacheable` trong Spring thường được mô tả là "Proxy" nhưng cũng có nét "Decorator". Phân tích: phần nào là kiểm soát truy cập (Proxy), phần nào là bọc thêm hành vi (Decorator)? Vì sao self-invocation phá cả hai? Cách khắc phục và đánh đổi của từng cách (tách bean, tự inject, `AopContext`).

**Câu 5.** Bạn có `interface PaymentGateway` với **một** implementation `StripePaymentGateway` suốt 3 năm, không test nào mock. Interface này đang tạo giá trị (OCP/DIP) hay chỉ là lớp gián tiếp thừa? Lập luận cả hai phía; nêu tiêu chí "một class xứng đáng có interface" (liên hệ Module 01.6 §11).

**Câu 6.** Lambda "thay thế" Strategy/Command trong nhiều trường hợp. Nêu **ba** trường hợp mà bạn vẫn nên tạo một class/interface có tên thay vì dùng lambda trần (gợi ý: cần state + nhiều method, cần đặt tên cho ý nghĩa nghiệp vụ, cần Spring quản lý như bean để inject/`@Qualifier`). Với Command, `undo()` khiến lambda không đủ ở đâu?

**Câu 7.** Cho một hệ thống đang có `NotificationManager` khổng lồ (gửi email, SMS, push, Slack, webhook; retry; ghi log; đo lường; chọn kênh theo cấu hình người dùng). Phân rã nó bằng pattern: kênh gửi → ?, "thử lần lượt tới khi thành công" → ?, retry/log/metrics → ?, chọn kênh theo user → ?. Chỉ rõ pattern cho từng phần và ranh giới trách nhiệm, sao cho thêm kênh thứ sáu chỉ là thêm một class + một dòng cấu hình.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **(a) là Strategy** — một thuật toán so sánh (lambda `Comparator`) được truyền vào `sort`; client chọn, các comparator độc lập, không tự chuyển. **(b) là State** — `Task` ủy quyền `start()` cho object trạng thái hiện tại, và trạng thái **tự quyết định trạng thái kế tiếp** (`OPEN.start()` trả `IN_PROGRESS`). Khác biệt cốt lõi: Strategy hoán một thuật toán do client chọn; State là tập trạng thái biết về nhau và tự chuyển tiếp theo luật nội bộ.
2. `SessionRegistry` là **Singleton giữ state thay đổi được** (`active` là `HashMap` mutable) → global mutable state: không reset được giữa các test, `HashMap` không thread-safe (race khi nhiều request cùng `add`), và mọi nơi gọi `SessionRegistry.get()` là dependency bị giấu (nghịch DIP). Trong Spring: khai báo `@Component`/`@Bean` (scope singleton do container quản lý), dùng `ConcurrentHashMap`, và **inject** `SessionRegistry` qua constructor thay vì gọi static — khi đó test thay được bằng bản giả.
3. Đây là **Proxy** (Spring AOP). `generate()` gọi `recalcAll()` bằng `this.recalcAll()` — lời gọi đi thẳng vào object thật, **không qua proxy** transaction → advice `@Transactional` của `recalcAll` bị bỏ qua (self-invocation). `recalcAll()` vẫn chạy trong transaction của `generate()` (do propagation mặc định `REQUIRED` mở ở `generate`), nhưng nếu `generate()` **không** có `@Transactional` thì `recalcAll()` chạy **không** transaction. Sửa: tách `recalcAll` sang bean khác, tự inject, hoặc `TransactionTemplate`.
4. `FileInputStream` là nguồn thật. `InputStreamReader` là **Adapter** — chuyển `InputStream` (byte) sang `Reader` (char), hai interface khác nhau. `BufferedReader` là **Decorator** — vẫn là `Reader`, chỉ bọc thêm hành vi đệm (`readLine()`), cùng interface với cái nó bọc. Tiêu chí: đổi interface = Adapter; giữ interface + thêm hành vi = Decorator.
5. **Premature abstraction / interface một-impl** (Module 01.6 §11). `UserService` chỉ có `UserServiceImpl`, không ai mock, cổng không đổi → interface hiện chỉ là một bước nhảy thừa khi đọc code. Hướng xử lý: nếu đây **không** phải ranh giới I/O ngoài và chưa có biến thiên thật, bỏ interface, để `UserService` là class cụ thể; thêm lại interface khi có implementation thứ hai thật hoặc khi cần test double. Giữ interface **nếu** nó là ranh giới cần mock trong test tầng trên.
6. Với `Shape` là `sealed` và **do bạn sở hữu**, nên chuyển sang `switch` pattern-matching: ít boilerplate hơn, và thêm `Triangle` vào `permits` sẽ làm **mọi** `switch` không phủ hết báo lỗi compile — an toàn khi refactor. Giữ Visitor khi: tập **thao tác** thay đổi nhiều hơn tập **kiểu** (mỗi thao tác gói gọn một class Visitor), hoặc hierarchy do bên thứ ba định nghĩa (không `sealed`/không sửa được để thêm `accept`). Tiêu chí = trục nào biến thiên nhiều hơn: thêm kiểu → `switch`/sealed; thêm thao tác → Visitor.
7. `Integer.valueOf` cache các giá trị `-128..127` (**Flyweight**) → `a1` và `a2` cùng trỏ một object, `a1 == a2` là `true`. `1000` ngoài dải cache → mỗi autoboxing tạo `Integer` mới → `c1 == c2` là `false`. Bài học: so sánh `Integer` luôn dùng `.equals()` hoặc unbox về `int` (Module 01.1).
8. (a) **Đa luồng:** `ArrayList` không thread-safe — `register()` từ thread này trong khi `place()` đang `for` duyệt listeners ở thread khác → `ConcurrentModificationException` hoặc mất phần tử. Sửa: `CopyOnWriteArrayList` (đọc nhiều, ghi hiếm) hoặc đồng bộ hóa. (b) **Bộ nhớ (lapsed listener):** không có `unregister()` → listener đăng ký một lần sống mãi cùng `OrderService`, kể cả khi bên đăng ký đã "chết" → rò bộ nhớ, và listener cũ vẫn bị gọi. Sửa: thêm `unregister()`, hoặc dùng `WeakReference`, hoặc `ApplicationEvent` của Spring (container quản lý vòng đời).

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Cả hai đều đúng; chọn **`enum AppLogger { INSTANCE; ... }`** nếu muốn ngắn nhất + chống reflection/serialization, hoặc **holder idiom** nếu cần `AppLogger` `extends`/`implements` phức tạp hoặc muốn lazy tường minh. `main`: `Set<Integer> ids = ConcurrentHashMap.newKeySet(); IntStream.range(0,100).parallel().forEach(i -> ids.add(System.identityHashCode(AppLogger.getInstance()))); assert ids.size() == 1;`. Bình luận: trong Spring, `@Component class AppLogger` + constructor injection cho scope singleton do container quản lý, test thay được, không cần static.
- **Bài 2:** `public static Builder to(String url) { return new Builder(Objects.requireNonNull(url)); }`. `Builder` giữ `url` `final`; các setter trả `this`. `build()`: `if (timeoutMs <= 0) throw new IllegalStateException("timeoutMs > 0"); if (retries < 0) throw ...; return new HttpRequestConfig(url, method, Map.copyOf(headers), timeoutMs, retries);`. `Map.copyOf` → `headers` bất biến; `config.headers().put(...)` ném `UnsupportedOperationException`.
- **Bài 3:** `private static final Map<String, Supplier<NotificationSender>> REGISTRY = new HashMap<>(); static { REGISTRY.put("EMAIL", EmailSender::new); REGISTRY.put("SMS", SmsSender::new); REGISTRY.put("PUSH", PushSender::new); }`. `create`: `return Optional.ofNullable(REGISTRY.get(type)).map(Supplier::get).orElseThrow(() -> new IllegalArgumentException("Không hỗ trợ: " + type));`. Thêm `SLACK`: `REGISTRY.put("SLACK", SlackSender::new);` — `create` không đổi. Test: `assertThrows(IllegalArgumentException.class, () -> create("FAX"));`.
- **Bài 4:** `TimingPriceFeed.get()` đo `System.nanoTime()` quanh `wrappee.get()`; `RetryingPriceFeed.get()` vòng `for (int i=1;i<=3;i++) try { return wrappee.get(); } catch (RuntimeException e) { last = e; }` rồi `throw last`. `new TimingPriceFeed(new RetryingPriceFeed(remote))` đo **tổng thời gian gồm cả các lần retry**; `new RetryingPriceFeed(new TimingPriceFeed(remote))` đo **từng lần gọi remote riêng lẻ** và retry ở ngoài. Ý nghĩa quan sát khác nhau → chọn thứ tự theo cái bạn muốn đo.
- **Bài 5:** `interface OrderState { OrderState pay(); OrderState ship(); OrderState cancel(); }`; `NewState.pay()` → `new PaidState()`, `NewState.ship()` → ném `IllegalStateException("Chưa thanh toán, không thể giao")`. `Order` giữ `OrderState state` và ủy quyền. So sánh: bản `switch (status)` gom mọi luật vào một method dễ đọc khi ít trạng thái, nhưng khi thêm trạng thái/hành động thì mọi `switch` rải rác phải sửa; State pattern gom hành vi theo trạng thái, thêm trạng thái = thêm một class.
- **Bài 6:** Chuỗi: `stock.linkTo(payment).linkTo(fraud)`; mỗi `handle` trả `boolean`, `false` = chặn. Sau chuỗi: `if (chain.handle(req)) { long fee = shippingPolicy.fee(req.grams()); publisher.publish(new OrderPlaced(req, fee)); }`. Observer: `List<OrderListener> = new CopyOnWriteArrayList<>()` với `register`/`unregister`. Bọc `PaymentHandler`: `class TimedHandler extends Handler { private final Handler delegate; boolean handle(Request r){ long t=nanoTime(); try { return delegate.handle(r);} finally { log(...); } } }` — không sửa `PaymentHandler`. `main` (2): đơn bị `PaymentHandler` trả `false` → `chain.handle` trả `false` → nhánh `publish` không chạy → observer không được gọi. Ghi chú: chain = Chain of Responsibility (hấp thụ "thêm bước tiền xử lý"); ShippingPolicy = Strategy ("thêm cách tính phí"); publisher/listener = Observer ("thêm phản ứng"); TimedHandler = Decorator/Proxy ("thêm đo lường không sửa code").

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. **DCL + `volatile`:** lazy hoàn toàn (tạo ở lần gọi đầu), **không** chống reflection/serialization tự thân, code rườm rà; chọn khi cần lazy và class phải `extends` cái khác hoặc cần logic khởi tạo có tham số. Thiếu `volatile`: việc gán `instance = new AppConfig()` không nguyên tử — JIT/CPU có thể publish tham chiếu **trước khi** constructor chạy xong; thread khác qua check `instance == null` đầu tiên thấy non-null và trả về object **nửa khởi tạo** (Module 05.2, happens-before). **Holder idiom:** lazy (Holder chỉ nạp khi `getInstance` gọi lần đầu), thread-safe nhờ đảm bảo khởi tạo class của JVM, không lock, không `volatile`; không chống reflection; chọn khi muốn lazy + đơn giản + không cần kế thừa. **`enum`:** **không** lazy (nạp cùng class), chống được cả reflection (constructor `enum` không gọi được) lẫn serialization (trả hằng có sẵn); chọn khi muốn an toàn tối đa và không cần lazy/kế thừa.
2. **Ý định:** Strategy = "có nhiều cách làm một việc, cho hoán đổi"; State = "hành vi của object phụ thuộc trạng thái, và trạng thái thay đổi trong vòng đời". **Luồng điều khiển:** Strategy do **client** set (`new Sorter(new QuickSort())`), các strategy **không biết nhau**, **không tự chuyển**; State do **chính object/các state** chuyển (`state = state.next()`), các state **biết** state kế tiếp. **Tiến hóa:** bắt đầu `PricingStrategy` chọn theo loại khách; rồi yêu cầu "khách mới sau 3 đơn thành khách VIP, sau khi hủy nhiều thành khách hạn chế" — logic chuyển tiếp xuất hiện → nâng thành `CustomerState` tự chuyển.
3. `switch` thắng vì: (i) **exhaustiveness** — thêm `JDate` vào `permits` làm compiler báo lỗi tại `render`/`validate`/`deepEquals`, không sót; (ii) mỗi thao tác nằm **một chỗ, đọc thẳng**, không phân tán qua 6 method `visitXxx` trong 3 class Visitor; (iii) ít boilerplate (không cần `accept`/`Visitor<R>`). Visitor **vẫn thắng** khi: hierarchy do thư viện bên thứ ba định nghĩa (không thêm được `accept` cũng chẳng `sealed` được — nhưng thực ra khi đó cả hai đều khó; Visitor chỉ khả thi nếu lib đã cung cấp `accept`), hoặc **tập thao tác rất lớn và hay thêm** trong khi tập kiểu đóng cứng — mỗi thao tác mới chỉ là một class Visitor, không đụng gì khác. **Expression problem:** không có cách nào (trong ngôn ngữ OOP cổ điển) vừa thêm kiểu mới vừa thêm thao tác mới mà **không sửa code cũ và vẫn an toàn kiểu**; OOP/đa hình cho "thêm kiểu dễ", Visitor/`switch` cho "thêm thao tác dễ", mỗi bên hy sinh chiều còn lại.
4. **Proxy:** Spring tạo object đại diện đứng trước bean; lời gọi bị **chặn** trước khi tới method thật để tra cache — nếu hit thì **không gọi method thật** (kiểm soát truy cập). **Decorator:** hành vi "tra cache / ghi cache" được **bọc quanh** kết quả method mà không sửa method — nếu miss thì gọi thật rồi **thêm bước** `cache.put`. Self-invocation phá cả hai vì `this.method()` bỏ qua object đại diện → không có gì chặn/bọc. Khắc phục: (a) **tách bean** — sạch nhất, nhưng đẻ thêm class; (b) **tự inject** (`@Autowired ReportService self;` rồi `self.recalcAll()`) — gọn nhưng hơi lạ khi đọc, coi chừng vòng phụ thuộc; (c) **`AopContext.currentProxy()`** — không cần field nhưng phải bật `exposeProxy = true` và trói code vào Spring AOP.
5. Phía "bỏ": chưa có biến thiên thật, không ai mock → interface chỉ là `Impl` + một bước nhảy khi đọc (needless complexity, Module 01.6 §11). Phía "giữ": `PaymentGateway` là **ranh giới I/O ngoài** — gần như chắc chắn cần test double khi viết test cho tầng nghiệp vụ gọi nó; giữ sẵn seam rẻ hơn thêm lại về sau, và nó giúp package nghiệp vụ không `import` SDK Stripe. Kết luận hợp lý: **giữ** vì là ranh giới hệ thống ngoài; nếu chỉ là interface nội bộ giữa hai class cùng module thì cân nhắc bỏ. Tiêu chí "xứng đáng có interface": (a) ranh giới với hệ thống ngoài cần test double; (b) đã/sắp có nhiều implementation thật; (c) điểm mở rộng công khai cho module/plugin khác; (d) cần cắt vòng phụ thuộc biên dịch giữa module.
6. Vẫn nên tạo class/interface có tên khi: (i) chiến lược cần **state** riêng và/hoặc **nhiều method** (lambda chỉ một method, không field); (ii) cần **đặt tên nghiệp vụ** cho ý nghĩa (`WeekendSurgePricing` đọc rõ hơn một lambda 5 dòng vô danh nhúng giữa code); (iii) cần Spring **quản lý như bean** để inject `List<T>`/`Map<String,T>`, gắn `@Qualifier`, `@Order`, hoặc để nó tự có dependency được inject. Với Command: `undo()` là method thứ hai và thường cần lưu **state để khôi phục** — lambda `Runnable` chỉ gói được `execute()`, không mang theo dữ liệu hoàn tác → cần class.
7. **Kênh gửi** (`EmailChannel`, `SmsChannel`...) → **Strategy** (mỗi kênh một implement của `NotificationChannel`), Spring inject `List`/`Map`. **"Thử lần lượt tới khi thành công"** → một **Composite** `CompositeNotificationChannel implements NotificationChannel` giữ danh sách kênh con và lặp cho tới khi một cái thành công (hoặc **Chain of Responsibility** nếu mỗi kênh tự quyết "tôi xử lý hay chuyển tiếp"). **Retry/log/metrics** → **Decorator** bọc từng kênh (`RetryingChannel`, `TimingChannel`, `LoggingChannel`), xếp chồng tùy cấu hình. **Chọn kênh theo user** → một **Strategy/Factory** `ChannelSelector` đọc cấu hình người dùng trả về kênh (hoặc thứ tự kênh) phù hợp. `NotificationManager` co lại thành một facade mỏng điều phối `ChannelSelector` + `CompositeNotificationChannel`. Thêm kênh thứ sáu = viết `WhatsAppChannel implements NotificationChannel` + đăng ký một dòng (bean `@Component` là Spring tự gom) — không sửa retry, không sửa selector, không sửa composite.

</details>

---

*File tiếp theo trong lộ trình: **Module 09 — Build Tools & Quản lý dự án** (Maven, Gradle, Git & quy trình làm việc nhóm).*
