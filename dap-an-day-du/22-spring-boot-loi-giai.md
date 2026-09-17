# Lời giải đầy đủ — Module 14: Spring Boot nâng cao

> Nguồn đề: `22 spring boot/22-spring-boot.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `application.yml` chung + `dev` + `prod`

### Đề
DataSource MySQL. Dev: `ddl-auto: update` + log SQL bật. Prod: `ddl-auto: validate` + credential từ biến môi trường.

### Lời giải

```yaml
# application.yml (CHUNG - áp dụng cho MỌI profile)
spring:
  application:
    name: library-service
  datasource:
    url: jdbc:mysql://localhost:3306/librarydb
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none   # mặc định AN TOÀN NHẤT - từng profile con override lại cụ thể
    open-in-view: false

---
# application-dev.yml
spring:
  config:
    activate:
      on-profile: dev
  jpa:
    hibernate:
      ddl-auto: update      # tự động cập nhật schema theo Entity - tiện cho dev, KHÔNG BAO GIỜ dùng ở prod
    show-sql: true
    properties:
      hibernate:
        format_sql: true    # in SQL đẹp, dễ đọc khi debug

logging:
  level:
    org.hibernate.SQL: debug

---
# application-prod.yml
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    username: ${DB_USERNAME}      # ĐỌC TỪ BIẾN MÔI TRƯỜNG - KHÔNG hardcode credential thật vào file
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate    # CHỈ kiểm tra schema khớp Entity, KHÔNG tự động sửa đổi DB production
    show-sql: false          # tắt log SQL ở prod - tránh lộ dữ liệu nhạy cảm vào log, giảm I/O
```

### Giải thích

- **`ddl-auto: update` (dev) vs `validate` (prod) — khác biệt CHÍ MẠNG:** `update` cho phép Hibernate **TỰ ĐỘNG ALTER schema** của DB theo Entity hiện tại — tiện lợi khi phát triển (không cần viết migration script thủ công mỗi lần đổi Entity), nhưng **CỰC KỲ NGUY HIỂM ở production** — Hibernate có thể suy luận SAI ý đồ thay đổi (VD đổi tên cột thành `ALTER... DROP COLUMN` + `ADD COLUMN` thay vì `RENAME COLUMN`, gây **MẤT DỮ LIỆU** thật). `validate` chỉ **SO SÁNH và BÁO LỖI** nếu schema DB không khớp Entity, không bao giờ tự ý sửa đổi DB — thay đổi schema production phải qua migration tool có kiểm soát (Flyway/Liquibase).
- **`${DB_USERNAME}`/`${DB_PASSWORD}` (Property Placeholder):** Spring Boot tự động thay thế cú pháp `${...}` bằng giá trị từ **biến môi trường HỆ ĐIỀU HÀNH** (hoặc system property `-D`) tại thời điểm khởi động — đảm bảo **KHÔNG BAO GIỜ commit credential thật vào Git** (liên hệ trực tiếp `.env`/`.gitignore` đã làm ở Module 09 — Build Tools).
- **File tách riêng theo `on-profile`** (thay vì 3 file `.yml` riêng biệt `application.yml`/`application-dev.yml`/`application-prod.yml`) sử dụng cú pháp **multi-document YAML** (phân tách bằng `---`) — cách này gộp được trong 1 file nếu muốn, nhưng đề bài yêu cầu 3 file riêng thì tách theo tên file chuẩn Spring Boot tự nhận diện (`application-{profile}.yml`) cũng hoàn toàn tương đương, chỉ khác cách tổ chức file.

---

## Bài 2 — `@ConfigurationProperties` cho `app.jwt` + validate + record

### Đề
`app.jwt`: `secretKey`, `expirationMinutes`, validate `expirationMinutes > 0` bằng `@Min`. Viết lại bằng `record`.

### Lời giải — phong cách class truyền thống

```java
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated   // BẮT BUỘC để kích hoạt validate Bean Validation (@Min, @NotBlank...) trên các field bên dưới
public class JwtProperties {

    @NotBlank
    private String secretKey;

    @Min(value = 1, message = "expirationMinutes phải lớn hơn 0")
    private int expirationMinutes;

    // getter/setter bắt buộc - @ConfigurationProperties cần setter để bind giá trị từ YAML
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public int getExpirationMinutes() { return expirationMinutes; }
    public void setExpirationMinutes(int expirationMinutes) { this.expirationMinutes = expirationMinutes; }
}
```

### Lời giải — phong cách `record` (immutable, khuyến nghị hiện đại)

```java
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
        @NotBlank String secretKey,
        @Min(value = 1, message = "expirationMinutes phải lớn hơn 0") int expirationMinutes
) {}
```

```java
// Kích hoạt: cần khai báo ở class @Configuration (record KHÔNG tự động là bean nếu không có @Component)
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {
}
```

```yaml
# application.yml
app:
  jwt:
    secret-key: "day-la-secret-key-cuc-ky-bi-mat"
    expiration-minutes: 60
```

### Giải thích

- **Vì sao `record` TỐT HƠN class truyền thống cho Configuration Properties:** liên hệ trực tiếp lợi ích của `record` đã học ở Module 06 (Java Modern) — Configuration Properties về bản chất là **dữ liệu bất biến sau khi đọc từ file cấu hình** (không có lý do gì để code nghiệp vụ SỬA lại `secretKey` lúc runtime) — `record` (constructor injection tự động qua **Constructor Binding**, không cần setter) loại bỏ hoàn toàn khả năng vô tình mutate cấu hình, và code NGẮN GỌN hơn đáng kể (không cần viết getter/setter tay).
- **`@Validated` là annotation BẮT BUỘC** (dễ quên) — nếu thiếu, các annotation `@Min`/`@NotBlank` sẽ **BỊ BỎ QUA HOÀN TOÀN**, không có bất kỳ cảnh báo nào lúc biên dịch hay khởi động — lỗi cấu hình sai (VD `expiration-minutes: -5`) chỉ phát hiện được khi logic nghiệp vụ dùng tới giá trị đó và gây lỗi khó hiểu ở runtime xa nơi khai báo.
- **Constructor Binding tự động của Spring Boot với `record`:** kể từ Spring Boot 2.6+, Spring **TỰ ĐỘNG NHẬN DIỆN** `record` là ứng viên cho Constructor Binding mà không cần thêm `@ConstructorBinding` (annotation này ĐÃ DEPRECATED từ Spring Boot 3.0 khi dùng trên `record` — chỉ còn cần thiết nếu dùng trên class thường có nhiều constructor, để chỉ rõ constructor nào dùng để bind).
- **`kebab-case` trong YAML (`secret-key`) tự động map với `camelCase` trong Java (`secretKey`)** — đây là quy ước "relaxed binding" mặc định của Spring Boot, không cần cấu hình gì thêm.

---

## Bài 3 — `@Profile` chọn implementation `NotificationSender`

### Đề
2 `@Bean` cùng interface `NotificationSender`, `@Profile("dev")`/`@Profile("prod")` — dev in console, prod gửi HTTP thật.

### Lời giải

```java
public interface NotificationSender {
    void send(String message);
}

@Configuration
public class NotificationConfig {

    @Bean
    @Profile("dev")   // Bean này CHỈ được tạo khi active profile là "dev"
    public NotificationSender consoleNotificationSender() {
        return message -> System.out.println("[DEV] Gửi thông báo (console): " + message);
    }

    @Bean
    @Profile("prod")   // Bean này CHỈ được tạo khi active profile là "prod"
    public NotificationSender httpNotificationSender(RestTemplate restTemplate) {
        return message -> {
            // Gửi thật qua HTTP tới 1 dịch vụ notification bên ngoài
            restTemplate.postForEntity("https://notification-service.internal/send", message, Void.class);
        };
    }
}
```

```yaml
# application.yml
spring:
  profiles:
    active: dev   # hoặc set qua biến môi trường SPRING_PROFILES_ACTIVE=prod lúc deploy, KHÔNG hardcode ở prod
```

### Giải thích

- **`@Profile` áp dụng ĐIỀU KIỆN tạo bean dựa trên "active profile"** — nếu KHÔNG có profile nào khớp được active (VD chạy mặc định, không set `dev` hay `prod`), **KHÔNG bean nào** trong 2 bean trên được tạo — nếu có nơi khác `@Autowired NotificationSender`, ứng dụng sẽ lỗi `NoSuchBeanDefinitionException` lúc khởi động — cần đảm bảo LUÔN có đúng 1 profile phù hợp được active trong mọi môi trường thực tế.
- **Nguyên tắc thiết kế quan trọng:** interface `NotificationSender` (dùng ở tầng nghiệp vụ, VD `OrderService`) **KHÔNG BAO GIỜ biết** đang chạy implementation nào — đây chính là ứng dụng thực tế của **Dependency Inversion Principle**, cho phép đổi hành vi TOÀN BỘ ứng dụng (console log ở dev, gửi HTTP thật ở prod) chỉ bằng cách đổi 1 giá trị cấu hình (`spring.profiles.active`), không cần sửa 1 dòng code nghiệp vụ nào.
- **Best practice production:** KHÔNG nên hardcode `spring.profiles.active: prod` ngay trong `application.yml` đóng gói sẵn trong JAR — nên set qua biến môi trường (`SPRING_PROFILES_ACTIVE=prod`) hoặc tham số dòng lệnh lúc chạy (`--spring.profiles.active=prod`) để CÙNG 1 JAR build ra dùng được cho MỌI môi trường, chỉ khác cách khởi chạy.

---

## Bài 4 — Auto-configuration với `@ConditionalOnClass`/`@ConditionalOnMissingBean`

### Đề
Giải thích cách Auto-configuration của `spring-boot-starter-data-jpa` xác định có nên tạo Bean `DataSource` hay không. Pseudo-code minh họa.

### Lời giải — pseudo-code minh họa cơ chế

```java
// Đây là mô phỏng ĐƠN GIẢN HÓA cơ chế THẬT bên trong DataSourceAutoConfiguration của Spring Boot
@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
// -> Chỉ "kích hoạt" class cấu hình này NẾU classpath THỰC SỰ CÓ mặt class DataSource
//    (tức là có driver JDBC nào đó, VD mysql-connector-j, đã được khai báo dependency trong pom.xml)
//    Nếu dự án KHÔNG dùng database gì cả (không có driver trong classpath) -> toàn bộ config này
//    bị Spring Boot TỰ ĐỘNG BỎ QUA HOÀN TOÀN, không tốn tài nguyên tạo bean thừa
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    // -> CHỈ tạo bean DataSource NÀY nếu person dùng thư viện KHÔNG ĐÃ TỰ ĐỊNH NGHĨA bean DataSource
    //    nào khác trong code của họ (VD 1 @Bean DataSource tùy chỉnh trong @Configuration riêng)
    //    Đây chính là cơ chế "convention over configuration": Spring Boot CUNG CẤP MẶC ĐỊNH HỢP LÝ,
    //    nhưng LUÔN NHƯỜNG ƯU TIÊN cho cấu hình THỦ CÔNG của người dùng nếu có
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
        // Đọc cấu hình từ spring.datasource.* trong application.yml để build DataSource mặc định
    }
}
```

### Giải thích

- **`@ConditionalOnClass`:** kiểm tra classpath (những JAR đã được đưa vào dependency qua Maven/Gradle) có chứa class chỉ định hay không — đây chính là cơ chế cho phép Spring Boot **TỰ ĐỘNG "phát hiện"** người dùng có ý định dùng JPA/DataSource hay không, THÔNG QUA VIỆC HỌ CÓ THÊM DEPENDENCY (`spring-boot-starter-data-jpa` kéo theo driver JDBC) HAY KHÔNG — không cần người dùng phải VIẾT tường minh bất kỳ dòng cấu hình `@Bean DataSource` nào, chỉ cần thêm đúng dependency trong `pom.xml`.
- **`@ConditionalOnMissingBean`:** đảm bảo Auto-configuration của Spring Boot **KHÔNG BAO GIỜ ghi đè** lên cấu hình do CHÍNH người dùng viết ra — nếu người dùng đã tự viết `@Bean public DataSource myCustomDataSource() {...}` (VD cấu hình connection pool đặc biệt, hoặc multiple DataSource cho multi-tenant), Spring Boot **TỰ ĐỘNG NHƯỜNG**, không tạo bean `DataSource` mặc định của mình nữa (tránh xung đột `NoUniqueBeanDefinitionException` do có 2 bean cùng kiểu).
- **Đây chính là triết lý cốt lõi "convention over configuration" (mặc định hợp lý, nhưng luôn override được) làm nên sự "kỳ diệu" của Spring Boot** — lý giải chính xác câu hỏi kinh điển của người mới học: "tại sao chỉ cần thêm `spring-boot-starter-data-jpa` vào `pom.xml` mà không cần viết code cấu hình gì, ứng dụng đã tự động có DataSource hoạt động?" — câu trả lời: **toàn bộ là các `@Configuration` class ẩn bên trong JAR `spring-boot-autoconfigure`, được kích hoạt CÓ ĐIỀU KIỆN dựa trên `@ConditionalOnClass`/`@ConditionalOnMissingBean`/`@ConditionalOnProperty`... quét tự động khi ứng dụng khởi động**.

---

## Bài 5 — Cấu trúc thư mục project cho ứng dụng Thư viện

### Đề
Layer-based structure cho `Book`, `Author`, `Member`, `Loan`. Liệt kê package + 1 file ví dụ mỗi layer.

### Lời giải

```
com.example.library
├── LibraryApplication.java                     (main class, @SpringBootApplication)
│
├── entity/
│   ├── Book.java
│   ├── Author.java
│   ├── Member.java
│   └── Loan.java
│
├── repository/
│   ├── BookRepository.java
│   ├── AuthorRepository.java
│   ├── MemberRepository.java
│   └── LoanRepository.java
│
├── service/
│   ├── BookService.java
│   ├── LoanService.java
│   └── impl/
│       ├── BookServiceImpl.java
│       └── LoanServiceImpl.java
│
├── controller/
│   ├── BookController.java
│   ├── MemberController.java
│   └── LoanController.java
│
├── dto/
│   ├── request/
│   │   └── CreateLoanRequest.java
│   └── response/
│       ├── BookResponse.java
│       └── LoanResponse.java
│
├── exception/
│   ├── BookNotFoundException.java
│   ├── LoanLimitExceededException.java
│   └── GlobalExceptionHandler.java
│
└── config/
    ├── SecurityConfig.java
    └── JpaConfig.java
```

**Ví dụ 1 file mỗi layer:**

```java
// entity/Book.java
@Entity
public class Book {
    @Id @GeneratedValue private Long id;
    private String title;
    private String isbn;
    @ManyToOne private Author author;
    private boolean available;
}
```

```java
// repository/BookRepository.java
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByAuthorId(Long authorId);
}
```

```java
// service/BookService.java (interface) + service/impl/BookServiceImpl.java
public interface BookService {
    BookResponse getBookById(Long id);
}

@Service
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    public BookServiceImpl(BookRepository bookRepository) { this.bookRepository = bookRepository; }

    @Override
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
        return new BookResponse(book.getId(), book.getTitle(), book.isAvailable());
    }
}
```

```java
// controller/BookController.java
@RestController
@RequestMapping("/api/v1/books")
public class BookController {
    private final BookService bookService;
    public BookController(BookService bookService) { this.bookService = bookService; }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }
}
```

```java
// dto/response/BookResponse.java
public record BookResponse(Long id, String title, boolean available) {}
```

### Giải thích

- **Layer-based (theo tầng kỹ thuật) là cấu trúc PHỔ BIẾN, dễ tiếp cận nhất cho dự án vừa/nhỏ** — mỗi package tương ứng đúng 1 vai trò trong kiến trúc phân lớp cổ điển (Controller → Service → Repository → Entity), dễ định hướng cho người mới join dự án ("muốn sửa API thì vào `controller/`, muốn sửa logic nghiệp vụ thì vào `service/`").
- **`service/` tách interface và `impl/` (implementation):** cho phép dễ dàng viết Unit Test (mock qua interface) và tuân thủ Dependency Inversion — Controller phụ thuộc vào **abstraction** `BookService`, không phụ thuộc trực tiếp implementation cụ thể — liên hệ nguyên tắc DIP đã học ở Module 06.
- **`dto/` tách riêng khỏi `entity/`:** tránh để lộ trực tiếp cấu trúc Entity (bao gồm cả quan hệ LAZY, các field nội bộ) ra API công khai — đây là thực hành chuẩn đã nhấn mạnh nhiều lần ở Module 12 (tránh `LazyInitializationException` khi serialize trực tiếp Entity thành JSON) và sẽ học sâu hơn ở Module 15 (RESTful API Design).
- **Nhược điểm của layer-based khi dự án LỚN DẦN** (đáng lưu ý, dù không phải trọng tâm bài này): khi số lượng entity/tính năng tăng lên hàng chục, mỗi package (`service/`, `controller/`...) chứa hàng chục file KHÔNG liên quan trực tiếp tới nhau về mặt nghiệp vụ — nhiều dự án lớn chuyển sang cấu trúc **feature-based/package-by-feature** (mỗi package = 1 tính năng, chứa đủ controller/service/repository riêng của tính năng đó) để dễ điều hướng hơn — đây là lựa chọn kiến trúc cần cân nhắc theo quy mô thực tế của dự án.

---

## Bài 6 — Custom `HealthIndicator` + Property Source ưu tiên

### Đề
`HealthIndicator` kiểm tra `InventoryServiceClient.isReachable()`. Giải thích: `application.yml` có `server.port: 8080`, nhưng deploy chạy `SERVER_PORT=9090 java -jar app.jar` — ứng dụng chạy cổng nào, vì sao?

### Lời giải — Custom `HealthIndicator`

```java
@Component
public class InventoryServiceHealthIndicator implements HealthIndicator {

    private final InventoryServiceClient inventoryServiceClient;

    public InventoryServiceHealthIndicator(InventoryServiceClient inventoryServiceClient) {
        this.inventoryServiceClient = inventoryServiceClient;
    }

    @Override
    public Health health() {
        boolean reachable = inventoryServiceClient.isReachable();

        if (reachable) {
            return Health.up()
                    .withDetail("service", "InventoryService")
                    .withDetail("status", "Kết nối bình thường")
                    .build();
        }

        return Health.down()
                .withDetail("service", "InventoryService")
                .withDetail("status", "Không thể kết nối")
                .build();
    }
}
```

```yaml
# application.yml - bật endpoint /actuator/health hiển thị chi tiết
management:
  endpoint:
    health:
      show-details: always
```

*Kết quả `GET /actuator/health` sẽ tự động bao gồm mục `"inventoryService": {"status": "UP", "details": {...}}` — Spring Boot Actuator tự động phát hiện MỌI bean implement `HealthIndicator` trong context và gộp vào kết quả health check tổng, không cần đăng ký thủ công ở đâu khác.*

### Giải thích — ứng dụng chạy cổng nào?

**Ứng dụng sẽ chạy ở cổng `9090`, KHÔNG PHẢI `8080`.**

- **Lý do:** Spring Boot đọc cấu hình theo **thứ tự ưu tiên (Property Source Order)** đã được định nghĩa rõ ràng — trong đó **biến môi trường (Environment Variable) có độ ưu tiên CAO HƠN** file `application.yml` đóng gói sẵn trong JAR. Thứ tự ưu tiên (từ CAO tới THẤP, rút gọn các nguồn phổ biến nhất):
  1. Command-line arguments (`--server.port=9090`)
  2. **Biến môi trường hệ điều hành** (`SERVER_PORT=9090`) ← trường hợp trong đề bài
  3. `application-{profile}.yml` (file cấu hình theo profile)
  4. `application.yml` (file cấu hình mặc định, đóng gói sẵn trong JAR) ← chứa `server.port: 8080`
- **Cơ chế "Relaxed Binding" cho phép `SERVER_PORT` (biến môi trường, quy ước UPPER_SNAKE_CASE của hệ điều hành) TỰ ĐỘNG khớp với property `server.port`** (quy ước kebab-case/camelCase trong file cấu hình) — Spring Boot tự động chuẩn hóa cả 2 dạng về cùng 1 "canonical form" để so khớp, không cần khai báo ánh xạ thủ công.
- **Ý nghĩa thực tế — đây chính là LÝ DO CỐT LÕI vì sao cơ chế này tồn tại:** cho phép **CÙNG 1 file JAR build MỘT LẦN DUY NHẤT** (`mvn package`) được deploy vào NHIỀU môi trường khác nhau (local, staging, production) với cấu hình khác nhau (cổng, DB URL, credential...) mà **KHÔNG CẦN build lại JAR riêng cho từng môi trường** — chỉ cần set biến môi trường phù hợp lúc khởi chạy. Đây cũng chính là nguyên tắc **"Config" trong 12-Factor App** — tách biệt hoàn toàn cấu hình khỏi code, giúp cùng 1 artifact di chuyển an toàn qua các môi trường.

---

*Đây là lời giải cho toàn bộ Phần B của Module 22. Tiếp theo: Module 15 — RESTful API Design.*
