# Module 13 — Spring Boot

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Spring Boot là cách gần như 100% dự án Java Backend hiện nay dùng Spring Framework trong thực tế. Hiểu **Auto-configuration** hoạt động thế nào giúp bạn debug được lỗi "vì sao Bean này tự có mà tôi không khai báo", cấu hình đúng `application.yml` theo từng môi trường (dev/staging/production) bằng **Profiles**, và dùng **Actuator** để giám sát ứng dụng — đây đều là kỹ năng bắt buộc khi đi làm, không chỉ để chạy `mvn spring-boot:run` là xong.
>
> **Phạm vi bài này:** cơ chế Spring Boot — auto-configuration, cấu hình, Profile, Actuator, đóng gói/triển khai. Bài **không** đi sâu thiết kế REST endpoint (`@RestController`, status code, versioning — đó là Module 14) hay Testing (`@SpringBootTest`, `@WebMvcTest` — module Testing riêng) — chỉ nhắc tới như ví dụ minh họa.

---

## Mục lục

1. [Spring Boot là gì? Giải quyết vấn đề gì?](#1-spring-boot-là-gì)
2. [@SpringBootApplication — 3 annotation gộp lại](#2-springbootapplication)
3. [Auto-configuration — cơ chế "phép màu"](#3-auto-configuration)
4. [Starter Dependencies](#4-starter-dependencies)
5. [Cấu trúc project chuẩn](#5-cấu-trúc-project-chuẩn)
6. [application.properties vs application.yml](#6-applicationproperties-vs-applicationyml)
7. [Profiles — cấu hình theo môi trường](#7-profiles)
8. [Externalized Configuration & @ConfigurationProperties](#8-externalized-configuration)
9. [Property Source & thứ tự ưu tiên cấu hình](#9-property-source--thứ-tự-ưu-tiên-cấu-hình)
10. [CommandLineRunner & ApplicationRunner](#10-commandlinerunner--applicationrunner)
11. [Spring Boot Actuator](#11-spring-boot-actuator)
12. [Đóng gói & triển khai ứng dụng](#12-đóng-gói--triển-khai-ứng-dụng)
13. [Tự viết Auto-configuration — hiểu "phép màu" từ bên trong](#13-tự-viết-auto-configuration)
14. [⚠️ Các bẫy hay gặp](#14-các-bẫy-hay-gặp)
15. [Tổng kết — Bảng ghi nhớ nhanh](#15-tổng-kết--bảng-ghi-nhớ-nhanh)
16. [Bài tập luyện tập](#16-bài-tập-luyện-tập)

---

## 1. Spring Boot là gì?

**Spring Boot không phải là 1 framework khác thay thế Spring** — nó là 1 lớp **convention-over-configuration** xây dựng trên nền Spring Framework (đã học ở Module 12), với mục tiêu: **giảm tối đa cấu hình thủ công (boilerplate config)** để bạn tập trung vào business logic.

### Trước Spring Boot (Spring "cổ điển" — chỉ để tham khảo lịch sử)

```xml
<!-- web.xml -->
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
</servlet>
```
```java
// Phải tự cấu hình DataSource, TransactionManager, ViewResolver...
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        // ... hàng chục dòng config thủ công khác
        return ds;
    }
    // ... rất nhiều @Bean khác phải tự viết
}
```

### Với Spring Boot

```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

```yaml
# application.yml — chỉ cần khai báo giá trị, Spring Boot tự lo phần "nối dây"
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
```

**3 trụ cột chính của Spring Boot:**

| Trụ cột | Giải quyết vấn đề gì |
|---|---|
| **Auto-configuration** | Tự động tạo Bean cần thiết dựa trên dependency có trong classpath |
| **Starter Dependencies** | Gộp nhóm dependency liên quan thành 1 dòng khai báo duy nhất |
| **Embedded Server** | Tomcat/Jetty được nhúng sẵn — chạy `java -jar app.jar` là có server, không cần cài đặt WAR lên server ngoài |

---

## 2. @SpringBootApplication

```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

`@SpringBootApplication` thực chất là **tổ hợp của 3 annotation:**

```java
@SpringBootConfiguration  // = @Configuration -> class này có thể khai báo @Bean
@EnableAutoConfiguration  // Bật cơ chế Auto-configuration (mục 3)
@ComponentScan            // Quét @Component/@Service/@Repository/@Controller
                           // trong package hiện tại + package con (đã học ở Module 12)
public @interface SpringBootApplication { }
```

⚠️ **Hệ quả quan trọng của `@ComponentScan` mặc định:** Class chứa `@SpringBootApplication` phải đặt ở package **gốc (root package)** của dự án, để `@ComponentScan` quét được toàn bộ package con. Đặt sai vị trí (VD: trong 1 sub-package) → các Bean ở package khác sẽ **không được Spring nhận diện**.

```
com.example.myapp
├── MyApp.java                    ✅ Đúng — package gốc
├── controller/OrderController.java
├── service/OrderService.java
└── repository/OrderRepository.java

com.example.myapp.config
└── MyApp.java                    ❌ Sai — đặt lệch vào sub-package
                                   -> Bean trong service/, controller/ không được quét!
```

---

## 3. Auto-configuration

Đây là **"phép màu"** cốt lõi khiến Spring Boot khác biệt — nhưng thực chất hoạt động theo cơ chế rất logic: **kiểm tra điều kiện (Conditional) dựa trên classpath và Bean đã tồn tại**.

### Cơ chế hoạt động

```java
// Bên trong Spring Boot source code (đơn giản hóa để minh họa nguyên lý)
@Configuration
@ConditionalOnClass(DataSource.class)          // CHỈ áp dụng NẾU classpath có class DataSource
                                                  // (nghĩa là bạn đã thêm dependency JDBC/JPA)
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean                    // CHỈ tạo Bean này NẾU bạn CHƯA tự khai báo DataSource
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
```

**Nguyên lý:** Spring Boot có sẵn hàng trăm class `*AutoConfiguration` (đóng gói trong file `spring-boot-autoconfigure.jar`), mỗi class được đánh dấu `@Conditional...` để chỉ "kích hoạt" khi:

| Annotation điều kiện | Ý nghĩa |
|---|---|
| `@ConditionalOnClass` | Chỉ áp dụng nếu 1 class cụ thể **có mặt** trong classpath |
| `@ConditionalOnMissingBean` | Chỉ tạo Bean này nếu **chưa có** Bean cùng type do bạn tự khai báo |
| `@ConditionalOnProperty` | Chỉ áp dụng nếu 1 property trong `application.yml` có giá trị nhất định |
| `@ConditionalOnWebApplication` | Chỉ áp dụng nếu ứng dụng là web application |
| `@ConditionalOnMissingClass` | Ngược lại với `@ConditionalOnClass` |

### Ví dụ thực tế dễ hình dung

Khi bạn thêm dependency `spring-boot-starter-data-jpa` vào `pom.xml`:
1. Classpath giờ có class `EntityManager`, `DataSource`...
2. `@ConditionalOnClass(DataSource.class)` trong `DataSourceAutoConfiguration` được thỏa mãn
3. Spring Boot tự động tạo Bean `DataSource` dựa trên config trong `application.yml` (`spring.datasource.url`...)
4. Tương tự, `HibernateJpaAutoConfiguration` tự tạo `EntityManagerFactory`, `JpaTransactionManager`

**→ Đây là lý do vì sao bạn KHÔNG cần viết bất kỳ `@Bean` nào cho DataSource/EntityManager khi dùng Spring Boot — chỉ cần thêm dependency + khai báo property.**

### Ghi đè Auto-configuration khi cần

```java
@Configuration
public class CustomDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        // Tự định nghĩa DataSource riêng (VD: dùng HikariCP với config đặc biệt)
        HikariDataSource ds = new HikariDataSource();
        ds.setMaximumPoolSize(50);
        return ds;
    }
    // Vì có @ConditionalOnMissingBean trong Auto-configuration gốc,
    // Bean tự khai báo của bạn sẽ ĐƯỢC ƯU TIÊN, Spring Boot sẽ KHÔNG tạo Bean mặc định nữa
}
```

### Xem Auto-configuration nào đang chạy (debug)

```yaml
# application.yml
debug: true
```

Chạy ứng dụng, log sẽ in ra **CONDITIONS EVALUATION REPORT** — liệt kê chi tiết Auto-configuration nào được kích hoạt (Positive matches) và bị bỏ qua (Negative matches), rất hữu ích khi debug "vì sao Bean X không được tạo".

---

## 4. Starter Dependencies

**Starter** là 1 dependency "gộp" (aggregator) — chỉ cần khai báo 1 dòng, Maven/Gradle tự kéo về toàn bộ thư viện liên quan đã được kiểm chứng tương thích phiên bản với nhau.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

`spring-boot-starter-web` thực chất kéo về:
- `spring-webmvc` (Spring MVC)
- `spring-boot-starter-tomcat` (Embedded Tomcat server)
- `jackson-databind` (JSON serialization)
- `spring-boot-starter` (dependency lõi: logging, auto-configuration...)

### Các Starter phổ biến nhất

| Starter | Dùng cho |
|---|---|
| `spring-boot-starter-web` | Xây dựng REST API/MVC web app (bao gồm embedded Tomcat) |
| `spring-boot-starter-data-jpa` | JPA/Hibernate + connection pool (HikariCP mặc định) |
| `spring-boot-starter-security` | Spring Security (đã học/sẽ học ở Module riêng) |
| `spring-boot-starter-validation` | Bean Validation (`@NotNull`, `@Size`...) |
| `spring-boot-starter-test` | JUnit, Mockito, AssertJ, Spring Test — cho testing |
| `spring-boot-starter-actuator` | Health check, metrics giám sát ứng dụng (mục 11) |
| `spring-boot-starter-data-redis` | Tích hợp Redis |
| `spring-boot-starter-amqp` | Tích hợp RabbitMQ |
| `spring-boot-devtools` | Hot reload/restart tự động khi code thay đổi — chỉ dùng khi development (mục 12) |

> **Vì sao Starter quan trọng hơn vẻ ngoài của nó:** Vấn đề "Dependency Hell" (2 thư viện yêu cầu 2 version khác nhau của cùng 1 thư viện thứ 3, gây conflict runtime) gần như biến mất nhờ Spring Boot's **BOM (Bill of Materials)** — `spring-boot-starter-parent` định nghĩa sẵn version tương thích cho hàng trăm thư viện phổ biến, bạn thường **không cần** tự khai báo `<version>` cho từng dependency.

---

## 5. Cấu trúc project chuẩn

```
my-spring-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/myapp/
│   │   │       ├── MyApp.java                    # Entry point (@SpringBootApplication)
│   │   │       ├── controller/                    # Tầng nhận HTTP request (REST endpoints)
│   │   │       │   └── OrderController.java
│   │   │       ├── service/                       # Tầng business logic
│   │   │       │   ├── OrderService.java
│   │   │       │   └── impl/OrderServiceImpl.java  # (tùy chọn, tách interface/impl)
│   │   │       ├── repository/                    # Tầng truy cập dữ liệu (Spring Data JPA)
│   │   │       │   └── OrderRepository.java
│   │   │       ├── entity/ (hoặc domain/, model/)  # JPA Entity
│   │   │       │   └── Order.java
│   │   │       ├── dto/                            # Data Transfer Object (request/response)
│   │   │       │   ├── OrderRequest.java
│   │   │       │   └── OrderResponse.java
│   │   │       ├── exception/                      # Custom Exception + Global Exception Handler
│   │   │       │   ├── OrderNotFoundException.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       └── config/                         # @Configuration classes
│   │   │           └── SecurityConfig.java
│   │   └── resources/
│   │       ├── application.yml                     # Config chính
│   │       ├── application-dev.yml                 # Config riêng cho Profile "dev"
│   │       ├── application-prod.yml                # Config riêng cho Profile "prod"
│   │       ├── static/                              # File tĩnh (nếu có web frontend nhúng)
│   │       └── templates/                           # Template engine (Thymeleaf...) nếu dùng
│   └── test/
│       └── java/com/example/myapp/
│           ├── service/OrderServiceTest.java
│           └── controller/OrderControllerTest.java
├── pom.xml (hoặc build.gradle)
└── README.md
```

**Kiến trúc phân lớp (Layered Architecture) này ánh xạ trực tiếp với các annotation đã học ở Module 12:**

```
Controller (@RestController)
     │  nhận HTTP request, gọi Service, trả DTO response
     ▼
Service (@Service)
     │  business logic, @Transactional
     ▼
Repository (@Repository / extends JpaRepository)
     │  truy vấn database
     ▼
Entity (@Entity)
     database table
```

> **Lưu ý:** Đây là cấu trúc phổ biến nhất (package theo **layer**), phù hợp với dự án vừa và nhỏ. Dự án lớn hơn thường tổ chức theo **feature/domain** (package theo module nghiệp vụ: `order/`, `user/`, `payment/`, mỗi package con chứa đủ Controller/Service/Repository riêng) để dễ tách thành Microservices sau này (sẽ học ở Module Microservices).

---

## 6. application.properties vs application.yml

Cả 2 định dạng đều dùng để cấu hình ứng dụng, tương đương nhau về chức năng — khác nhau về cú pháp.

### application.properties

```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### application.yml (⭐ khuyến nghị — phổ biến hơn trong thực tế)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
```

| | properties | yml |
|---|---|---|
| Cú pháp | Flat key=value, phân cấp bằng dấu `.` | Phân cấp bằng thụt lề (indentation) |
| Độ dài khi config nhiều | Dài dòng, lặp lại tiền tố nhiều lần | Ngắn gọn hơn, dễ đọc theo cây phân cấp |
| Hỗ trợ List/Array | Hạn chế, phải dùng index `[0]`, `[1]` | Tự nhiên hơn với `-` |
| Lỗi thường gặp | Ít | **Nhạy cảm với khoảng trắng thụt lề** — dùng Tab thay vì Space sẽ lỗi parse |

⚠️ **Bẫy hay gặp với YAML:** YAML **không cho phép dùng Tab để thụt lề**, chỉ được dùng Space — nhiều editor tự động chèn Tab gây lỗi khó phát hiện bằng mắt thường (`application.yml` bị parse sai, dẫn tới `NoSuchBeanDefinitionException` hoặc config bị bỏ qua âm thầm).

```yaml
spring:
	datasource:    # ❌ Dòng này dùng Tab -> YAML parser lỗi hoặc bỏ qua toàn bộ block!
    url: jdbc:mysql://localhost:3306/mydb
```

### Relaxed Binding — Spring Boot linh hoạt hóa cách viết tên property

Spring Boot chấp nhận **nhiều biến thể cách viết khác nhau** cho cùng 1 property, giúp file config đọc tự nhiên hơn dù code Java dùng `camelCase`:

```yaml
app:
  mail-retry-count: 3    # kebab-case (khuyến nghị trong .yml — dễ đọc nhất)
```
```properties
app.mailRetryCount=3     # camelCase
app.MAIL_RETRY_COUNT=3   # UPPER_SNAKE_CASE (thường dùng khi set qua biến môi trường OS)
```

Cả 3 cách trên đều bind đúng vào field `mailRetryCount` trong class Java nhờ **Relaxed Binding**. Đây là lý do biến môi trường OS (chỉ hỗ trợ `UPPER_SNAKE_CASE`, không hỗ trợ dấu chấm) vẫn ánh xạ đúng được vào property dạng `spring.datasource.url` → biến môi trường tương ứng là `SPRING_DATASOURCE_URL`.

---

## 7. Profiles

**Profile** cho phép định nghĩa **các bộ cấu hình khác nhau cho từng môi trường** (development, staging, production) và chuyển đổi linh hoạt mà không cần sửa code.

### Cấu trúc file theo Profile

```
application.yml           # Config CHUNG cho mọi môi trường
application-dev.yml       # Chỉ áp dụng khi Profile "dev" active
application-prod.yml      # Chỉ áp dụng khi Profile "prod" active
```

```yaml
# application.yml (chung)
spring:
  application:
    name: my-app
  profiles:
    active: dev   # Profile mặc định nếu không chỉ định gì khác

logging:
  level:
    root: INFO
```

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb_dev
    username: root
    password: root
  jpa:
    show-sql: true      # Bật log SQL để debug khi dev
    hibernate:
      ddl-auto: update  # Tự động cập nhật schema khi dev (tiện lợi)

logging:
  level:
    org.hibernate.SQL: DEBUG
```

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-db-host:3306/mydb_prod
    username: ${DB_USERNAME}    # Đọc từ biến môi trường -> KHÔNG hardcode credential
    password: ${DB_PASSWORD}
  jpa:
    show-sql: false             # Tắt log SQL ở production (hiệu năng + bảo mật)
    hibernate:
      ddl-auto: validate        # CHỈ kiểm tra schema khớp, KHÔNG tự động sửa DB ở production!

logging:
  level:
    root: WARN
```

### Kích hoạt Profile

```bash
# Cách 1: command line argument
java -jar app.jar --spring.profiles.active=prod

# Cách 2: biến môi trường
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar

# Cách 3: trong IDE (VD IntelliJ) - Run Configuration -> Environment variables
```

### @Profile trên Bean — chỉ tạo Bean cho môi trường cụ thể

```java
@Configuration
public class MailConfig {

    @Bean
    @Profile("dev")
    public MailSender devMailSender() {
        return new FakeMailSender(); // Không gửi mail thật khi dev, chỉ log ra console
    }

    @Bean
    @Profile("prod")
    public MailSender prodMailSender() {
        return new SmtpMailSender(); // Gửi mail thật qua SMTP server
    }
}
```

> **Liên hệ Module 12:** `@Profile` thực chất là 1 dạng `@Conditional` chuyên biệt (`@ConditionalOnExpression` phía dưới) — cùng họ hàng với `@ConditionalOnClass`/`@ConditionalOnMissingBean` đã học ở mục 3, chỉ khác điều kiện kiểm tra là "Profile nào đang active" thay vì "classpath có class gì".

**Nhiều Profile cùng lúc & Profile Group:**

```bash
java -jar app.jar --spring.profiles.active=prod,cloud  # kích hoạt CẢ 2 Profile cùng lúc
```
```yaml
# application.yml — gom nhóm nhiều Profile thành 1 "nhóm" tiện gọi tắt (Spring Boot 2.4+)
spring:
  profiles:
    group:
      production: "prod,cloud,monitoring" # active "production" = tự động active cả 3 profile con
```

⚠️ **Bẫy sản xuất cực kỳ nghiêm trọng:** `ddl-auto: update` (hoặc tệ hơn là `create`/`create-drop`) **TUYỆT ĐỐI KHÔNG được dùng ở production** — Hibernate có thể tự động sửa đổi/xóa schema database thật dựa trên Entity class, dẫn tới **mất dữ liệu không thể khôi phục**. Ở production, luôn dùng `validate` (chỉ kiểm tra, không sửa) hoặc `none`, và quản lý schema migration bằng công cụ chuyên dụng như **Flyway** hoặc **Liquibase**.

| ddl-auto | Ý nghĩa | Môi trường phù hợp |
|---|---|---|
| `create` | Xóa toàn bộ schema cũ, tạo lại mới mỗi lần start | Test cục bộ, không bao giờ dùng ở đâu khác |
| `create-drop` | Giống `create`, nhưng xóa schema khi ứng dụng tắt | Unit test tự động |
| `update` | Cập nhật schema cho khớp Entity (không xóa dữ liệu cũ, nhưng KHÔNG đáng tin cậy 100%) | Development cục bộ |
| `validate` | Chỉ kiểm tra Entity có khớp schema hiện có, KHÔNG sửa gì | **Production (khuyến nghị)** |
| `none` | Không làm gì cả | Production (khi dùng Flyway/Liquibase quản lý riêng) |

---

## 8. Externalized Configuration

### @Value — inject 1 property đơn lẻ

```java
@Component
public class MailService {
    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.retry-count:3}") // Giá trị mặc định = 3 nếu property không tồn tại
    private int retryCount;
}
```

### @ConfigurationProperties — inject cả 1 nhóm property vào 1 class (⭐ khuyến nghị cho nhóm config phức tạp)

```yaml
# application.yml
app:
  mail:
    from-address: noreply@example.com
    retry-count: 3
    smtp:
      host: smtp.gmail.com
      port: 587
```

```java
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
    private String fromAddress;
    private int retryCount;
    private Smtp smtp = new Smtp();

    public static class Smtp {
        private String host;
        private int port;
        // getters/setters
    }
    // getters/setters cho fromAddress, retryCount, smtp
}
```

```java
@Service
public class MailService {
    private final MailProperties mailProperties; // Inject cả nhóm config, type-safe

    public MailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public void send() {
        System.out.println("Gửi từ: " + mailProperties.getFromAddress());
        System.out.println("SMTP host: " + mailProperties.getSmtp().getHost());
    }
}
```

**Ưu điểm `@ConfigurationProperties` so với nhiều `@Value` rải rác:**
- Type-safe, IDE tự động gợi ý (autocomplete) nhờ metadata
- Validate được bằng Bean Validation (`@NotNull`, `@Min`...) ngay trên field
- Gom nhóm logic, dễ đọc, dễ maintain khi có nhiều property liên quan

### Phong cách hiện đại — `@ConfigurationProperties` với `record` (Java 17+, immutable)

Kết hợp với kiến thức `record` đã học ở Module 09, `@ConfigurationProperties` **không bắt buộc** phải là class có getter/setter — có thể dùng `record` để config **immutable** ngay từ khi khởi động, gọn hơn nhiều:

```java
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String fromAddress,
        int retryCount,
        Smtp smtp
) {
    public record Smtp(String host, int port) {}
}
```
> Với `record`, Spring Boot dùng **constructor binding** (thay vì gọi setter) để gán giá trị từ file cấu hình — không cần thêm annotation gì khác, không cần no-arg constructor. Cách này được khuyến khích cho project mới vì tận dụng đúng tinh thần bất biến (immutability) đã học ở Module 09, tránh việc cấu hình bị vô tình sửa đổi ở đâu đó giữa lúc ứng dụng chạy.

---

## 9. Property Source & thứ tự ưu tiên cấu hình

Spring Boot có thể đọc cấu hình từ **rất nhiều nguồn khác nhau cùng lúc** — hiểu đúng **thứ tự ưu tiên** (nguồn nào "thắng" khi có xung đột giá trị) là kỹ năng thực chiến quan trọng, tránh tình huống "tôi đã set biến môi trường mà ứng dụng vẫn không đổi".

### Thứ tự ưu tiên từ CAO xuống THẤP (nguồn cao hơn ghi đè nguồn thấp hơn)

| # | Nguồn | Ví dụ |
|---|---|---|
| 1 (cao nhất) | Command line argument | `java -jar app.jar --server.port=9090` |
| 2 | `SPRING_APPLICATION_JSON` (biến môi trường/system property chứa JSON) | `SPRING_APPLICATION_JSON='{"server":{"port":9090}}'` |
| 3 | JNDI attributes | (Hiếm dùng trong Spring Boot hiện đại) |
| 4 | Java System Properties | `java -Dserver.port=9090 -jar app.jar` |
| 5 | OS Environment Variables | `export SERVER_PORT=9090` |
| 6 | `application-{profile}.yml` (Profile-specific, ngoài JAR) | `application-prod.yml` đặt cạnh file JAR |
| 7 | `application.yml` (ngoài JAR, cùng thư mục hoặc `config/`) | |
| 8 | `application-{profile}.yml` (đóng gói TRONG JAR, `src/main/resources/`) | |
| 9 (thấp nhất) | `application.yml` (đóng gói TRONG JAR) | Giá trị mặc định |

> **Nguyên tắc dễ nhớ:** cấu hình **bên ngoài** JAR luôn thắng cấu hình **đóng gói bên trong** JAR; cấu hình truyền lúc **chạy** (command line, biến môi trường) luôn thắng cấu hình **tĩnh** trong file. Đây chính là cơ chế cho phép 1 file JAR **duy nhất** build 1 lần (`mvn clean package`) chạy được ở nhiều môi trường khác nhau chỉ bằng cách đổi biến môi trường/tham số dòng lệnh khi khởi động — không cần build lại cho từng môi trường.

```bash
# File JAR có sẵn application.yml với server.port=8080 (nguồn #9)
# Nhưng khi chạy production, override bằng biến môi trường (nguồn #5) - thắng vì cao hơn
SERVER_PORT=8443 java -jar app.jar
```

### Vì sao `application-{profile}.yml` (#6/#8) ưu tiên hơn `application.yml` (#7/#9)?

Đây là lý do kỹ thuật cho phép Profile (mục 7) "ghi đè" cấu hình chung — `application-prod.yml` không phải là file độc lập tách biệt, mà được xem như 1 nguồn property có độ ưu tiên **cao hơn** `application.yml` khi Profile "prod" đang active, nên các key trùng tên sẽ lấy giá trị từ file Profile-specific.

---

## 10. CommandLineRunner & ApplicationRunner

Đôi khi cần chạy 1 đoạn code **ngay sau khi ứng dụng khởi động xong** (Spring Context đã sẵn sàng, mọi Bean đã inject đầy đủ) — ví dụ: nạp dữ liệu mẫu, kiểm tra kết nối tới hệ thống ngoài, in ra thông tin cấu hình đang dùng.

```java
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Chạy đúng 1 LẦN, NGAY SAU KHI ApplicationContext khởi tạo xong hoàn tất
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin", "admin@example.com"));
            System.out.println("Đã tạo tài khoản admin mặc định");
        }
    }
}
```

```java
@Component
public class StartupChecker implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Giống CommandLineRunner nhưng args được parse SẴN thành ApplicationArguments
        // (phân biệt được --option=value vs tham số thường), tiện hơn khi cần đọc argument phức tạp
        if (args.containsOption("skip-seed")) {
            System.out.println("Bỏ qua bước seed dữ liệu theo yêu cầu");
        }
    }
}
```

| | `CommandLineRunner` | `ApplicationRunner` |
|---|---|---|
| Tham số `run()` | `String... args` (thô) | `ApplicationArguments` (đã parse) |
| Khi dùng | Đơn giản, không cần phân biệt loại argument | Cần đọc argument dạng `--key=value` |

> **Thứ tự thực thi khi có nhiều Runner:** dùng `@Order(n)` để kiểm soát — số nhỏ hơn chạy trước, giống nguyên tắc `@Order` cho nhiều Aspect đã học ở Module 12.

⚠️ **Lưu ý:** không nên dùng `CommandLineRunner` để seed dữ liệu **thật** ở production (dễ chạy nhầm nhiều lần khi có nhiều instance backend khởi động — liên hệ khái niệm nhiều instance đã bàn ở Module 10) — với migration/seed dữ liệu bền vững, công cụ chuyên dụng như **Flyway/Liquibase** (đã nhắc ở mục 7) vẫn là lựa chọn đúng đắn hơn.

---

## 11. Spring Boot Actuator

**Actuator** cung cấp sẵn các endpoint giám sát (monitoring) và quản lý ứng dụng ở production **mà không cần tự viết code**.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus  # Chỉ bật các endpoint cần thiết
  endpoint:
    health:
      show-details: always
```

### Các endpoint quan trọng nhất

| Endpoint | Chức năng |
|---|---|
| `/actuator/health` | Kiểm tra ứng dụng còn "sống" không — dùng cho Load Balancer/Kubernetes health check |
| `/actuator/info` | Thông tin build (version, git commit...) |
| `/actuator/metrics` | Số liệu runtime: memory, CPU, HTTP request count, response time... |
| `/actuator/env` | Xem toàn bộ property đang active (⚠️ cẩn thận lộ secret nếu bật ở production không kiểm soát) |
| `/actuator/loggers` | Xem/đổi log level runtime **mà không cần restart ứng dụng** |
| `/actuator/prometheus` | Xuất metrics theo format Prometheus (tích hợp Grafana giám sát) |

```json
// GET /actuator/health
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL" } },
    "diskSpace": { "status": "UP" }
  }
}
```

### Custom HealthIndicator — mở rộng health check theo nghiệp vụ riêng

Health check mặc định (`db`, `diskSpace`) chỉ kiểm tra hạ tầng chung — thực tế thường cần kiểm tra thêm các phụ thuộc đặc thù của ứng dụng (VD: 1 external payment gateway có đang phản hồi không):

```java
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {

    private final PaymentGatewayClient client;

    public PaymentGatewayHealthIndicator(PaymentGatewayClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            boolean reachable = client.ping();
            return reachable
                    ? Health.up().withDetail("gateway", "reachable").build()
                    : Health.down().withDetail("gateway", "unreachable").build();
        } catch (Exception e) {
            return Health.down(e).build(); // Đính kèm cả exception vào response health
        }
    }
}
```
> Sau khi đăng ký, `/actuator/health` sẽ tự động có thêm mục `paymentGateway` bên cạnh `db`/`diskSpace` — nếu component này báo `DOWN`, **toàn bộ status tổng** của `/actuator/health` cũng chuyển thành `DOWN` (trừ khi cấu hình riêng), phản ánh đúng thực trạng "ứng dụng không hoạt động đầy đủ" dù bản thân server Java vẫn chạy bình thường.

⚠️ **Lưu ý bảo mật:** Actuator endpoint (đặc biệt `/actuator/env`, `/actuator/heapdump`) chứa thông tin nhạy cảm — **luôn bảo vệ bằng Spring Security** hoặc chỉ expose nội bộ (không public ra Internet), và chỉ `include` những endpoint thực sự cần thiết thay vì `include: "*"`.

> **Liên hệ thực tế:** Trong hệ thống Microservices/Kubernetes, `/actuator/health` chính là endpoint được dùng cho **Liveness Probe** và **Readiness Probe** — Kubernetes tự động restart container nếu health check fail liên tục.

---

## 12. Đóng gói & triển khai ứng dụng

### Maven

```bash
mvn clean package          # Tạo file .jar trong thư mục target/
java -jar target/my-app-1.0.0.jar
```

### Gradle

```bash
./gradlew build
java -jar build/libs/my-app-1.0.0.jar
```

### Fat JAR / Executable JAR

Spring Boot đóng gói ứng dụng thành **1 file JAR duy nhất** chứa cả code, dependency, và **embedded Tomcat server** — không cần cài đặt server ngoài, không cần deploy file WAR lên server riêng như Spring cổ điển:

```
my-app.jar
├── BOOT-INF/classes/          # Code của bạn
├── BOOT-INF/lib/               # Tất cả dependency (Spring, Hibernate, Jackson...)
└── org/springframework/boot/loader/  # Class loader đặc biệt để chạy Fat JAR
```

### Layered JAR — tối ưu build Docker image

Đóng gói toàn bộ Fat JAR vào **1 layer Docker duy nhất** là kém hiệu quả: mỗi lần code thay đổi dù chỉ 1 dòng, Docker phải build lại và đẩy lên registry **toàn bộ** JAR (bao gồm cả dependency vốn hiếm khi đổi). Spring Boot hỗ trợ tách JAR thành **nhiều layer** theo tần suất thay đổi:

```bash
java -Djarmode=layertools -jar my-app.jar extract
# Sinh ra các thư mục: dependencies/, spring-boot-loader/, snapshot-dependencies/, application/
```

```dockerfile
FROM eclipse-temurin:21-jre AS builder
WORKDIR /app
COPY target/my-app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy theo thứ tự TỪ ÍT ĐỔI NHẤT -> HAY ĐỔI NHẤT -> tận dụng Docker layer cache tối đa
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./          # Code của bạn — đổi thường xuyên nhất, đặt CUỐI
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```
> **Lợi ích:** vì Docker cache từng layer riêng biệt, khi chỉ code nghiệp vụ đổi (layer `application/`), Docker **chỉ cần build lại layer cuối cùng** — các layer `dependencies/` (hiếm khi đổi) được tái sử dụng từ cache, giúp build/deploy nhanh hơn đáng kể trong CI/CD.

### Graceful Shutdown — tắt ứng dụng "có trách nhiệm"

Mặc định, khi nhận tín hiệu dừng (SIGTERM — VD: Kubernetes rolling update), embedded server dừng **ngay lập tức**, có thể cắt ngang các request đang xử lý dở. Bật graceful shutdown để server **hoàn tất các request đang chạy** trước khi tắt hẳn:

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # Chờ tối đa 30s cho request đang chạy hoàn tất, rồi buộc tắt
```
> **Liên hệ thực tế Microservices/Kubernetes:** đây là cấu hình gần như **bắt buộc** khi triển khai trên Kubernetes — kết hợp với `preStop` hook và Readiness Probe (`/actuator/health`, mục 11) để đảm bảo không có request nào bị "rớt" (dropped) trong lúc rolling update pod.

---

## 13. Tự viết Auto-configuration

Hiểu sâu cơ chế Auto-configuration (mục 3) từ góc nhìn "người tạo ra nó" — không chỉ "người dùng nó" — giúp củng cố toàn bộ kiến thức trước đó, và là nền tảng nếu sau này cần đóng gói 1 module dùng chung thành **Starter riêng** cho nhiều dự án trong công ty.

### Bước 1 — Viết Auto-configuration class như bình thường

```java
@AutoConfiguration                          // Từ Spring Boot 2.7+, thay cho @Configuration trong ngữ cảnh này
@ConditionalOnClass(SmsClient.class)         // Chỉ kích hoạt nếu classpath có thư viện SMS
@EnableConfigurationProperties(SmsProperties.class)
public class SmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean                 // Cho phép người dùng thư viện tự override nếu muốn
    @ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SmsService smsService(SmsProperties properties) {
        return new SmsService(properties.getApiKey(), properties.getSenderId());
    }
}

@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {
    private String apiKey;
    private String senderId;
    private boolean enabled = true;
    // getters/setters
}
```

### Bước 2 — Đăng ký để Spring Boot TỰ TÌM THẤY class này

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```
```
com.example.sms.SmsAutoConfiguration
```
> Đây chính là "sổ đăng ký" mà `@EnableAutoConfiguration` (thành phần của `@SpringBootApplication`, mục 2) **đọc lúc khởi động** để biết cần thử kích hoạt những Auto-configuration class nào — mỗi thư viện/starter của Spring Boot (và của bên thứ 3) đều có 1 file tương tự. *(Trước Spring Boot 2.7, cơ chế cũ dùng file `META-INF/spring.factories` với key `org.springframework.boot.autoconfigure.EnableAutoConfiguration` — vẫn còn gặp trong nhiều thư viện cũ.)*

### Kết quả: đóng gói thành 1 module riêng, dự án khác chỉ cần thêm dependency

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>sms-spring-boot-starter</artifactId>
</dependency>
```
```yaml
app:
  sms:
    api-key: xxx
    sender-id: MyCompany
```
> Dự án dùng thư viện này **không cần viết bất kỳ `@Bean` nào** — hoàn toàn giống trải nghiệm dùng `spring-boot-starter-data-jpa` ở mục 3, vì bản chất chính là **cùng 1 cơ chế** mà Spring Boot team dùng để xây dựng mọi starter chính thức.

---

## 14. ⚠️ Các bẫy hay gặp

1. **Đặt class `@SpringBootApplication` sai vị trí** (không ở package gốc) → `@ComponentScan` bỏ sót Bean ở package khác.

2. **`ddl-auto: update` hoặc `create` ở production** → nguy cơ mất dữ liệu, luôn dùng `validate`/`none` + Flyway/Liquibase.

3. **Dùng Tab thay vì Space trong YAML** → lỗi parse âm thầm, khó phát hiện bằng mắt.

4. **Hardcode secret (password, API key) trực tiếp trong `application.yml`** → luôn dùng biến môi trường (`${DB_PASSWORD}`) hoặc secret manager (Vault, AWS Secrets Manager).

5. **Expose toàn bộ Actuator endpoint (`include: "*"`) mà không bảo vệ bằng Security** → rò rỉ thông tin nhạy cảm (`/actuator/env`, `/actuator/heapdump`).

6. **Quên set `spring.profiles.active`** khi deploy → chạy nhầm config `dev` ở môi trường production (VD: kết nối vào DB dev thay vì DB thật).

7. **Không hiểu Auto-configuration nên tự viết `@Bean` trùng lặp** với Bean Spring Boot đã tự tạo sẵn → gây `BeanDefinitionOverrideException` hoặc hành vi không như mong đợi.

8. **Dùng quá nhiều `@Value` rải rác** thay vì gom nhóm bằng `@ConfigurationProperties` → khó maintain khi có nhiều config liên quan.

9. **Không set `server.port` khác nhau khi chạy nhiều service cục bộ cùng lúc** (khi thực hành Microservices sau này) → xung đột cổng.

10. **Nhầm lẫn thứ tự ưu tiên khi có nhiều nguồn config** (command line > biến môi trường > `application-{profile}.yml` > `application.yml`, chi tiết mục 9) → không hiểu tại sao giá trị bị "ghi đè" ngoài ý muốn.

11. **Dùng `CommandLineRunner` để seed dữ liệu nghiệp vụ thật ở production** — dễ chạy trùng lặp không kiểm soát khi có nhiều instance cùng khởi động; nên dùng Flyway/Liquibase cho migration bền vững.

12. **Không bật `server.shutdown: graceful` khi triển khai trên Kubernetes/có Load Balancer** — request đang xử lý dở có thể bị cắt ngang giữa chừng khi pod bị terminate trong lúc rolling update.

13. **Đóng gói toàn bộ JAR thành 1 layer Docker duy nhất** — mỗi lần sửa 1 dòng code, toàn bộ image (kể cả hàng trăm MB dependency không đổi) phải build/push lại từ đầu, làm chậm CI/CD không cần thiết.

---

## 15. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Spring Boot | Lớp convention-over-configuration trên Spring Framework — giảm boilerplate |
| @SpringBootApplication | = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan |
| Auto-configuration | Dựa trên `@ConditionalOnClass`/`@ConditionalOnMissingBean` — tự tạo Bean nếu đủ điều kiện |
| Starter | Gộp nhóm dependency tương thích sẵn — tránh Dependency Hell |
| application.yml | Cấu trúc phân cấp, khuyến nghị hơn `.properties`; cẩn thận Tab vs Space; Relaxed Binding cho phép nhiều cách viết tên |
| Profiles | `application-{profile}.yml` — tách config theo môi trường (dev/staging/prod); Profile Group gom nhiều Profile |
| ddl-auto | Production LUÔN dùng `validate`/`none`, không bao giờ `update`/`create` |
| @ConfigurationProperties | Inject cả nhóm config type-safe; có thể dùng `record` (Java 17+) cho immutable config |
| Property Source Priority | Command line > System property > biến môi trường > file Profile-specific > file chung; ngoài JAR thắng trong JAR |
| CommandLineRunner/ApplicationRunner | Chạy code 1 lần ngay sau khi ứng dụng khởi động xong — không dùng để seed dữ liệu thật ở production |
| Actuator | `/actuator/health`, `/actuator/metrics` — giám sát production, nhớ bảo mật; `HealthIndicator` mở rộng kiểm tra riêng |
| Fat JAR / Layered JAR | Fat JAR: code+dependency+server trong 1 file; Layered JAR tách theo tần suất đổi — tối ưu cache Docker |
| Graceful Shutdown | `server.shutdown: graceful` — hoàn tất request đang chạy trước khi tắt, cần cho Kubernetes rolling update |
| Tự viết Auto-configuration | `@AutoConfiguration` + file `AutoConfiguration.imports` — cơ chế y hệt mọi starter chính thức của Spring Boot |

---

## 16. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. `@SpringBootApplication` phải luôn được đặt ở package gốc để `@ComponentScan` hoạt động đúng.
2. Auto-configuration của Spring Boot sẽ ghi đè lên Bean bạn tự khai báo nếu trùng type.
3. `ddl-auto: update` an toàn để dùng ở môi trường production vì Hibernate chỉ thêm cột mới, không xóa dữ liệu.
4. `@ConfigurationProperties` cho phép validate giá trị bằng Bean Validation annotation.
5. YAML cho phép dùng cả Tab và Space để thụt lề, miễn là nhất quán trong 1 file.
6. Actuator endpoint `/actuator/health` thường được dùng làm Liveness/Readiness Probe trong Kubernetes.
7. Starter dependency như `spring-boot-starter-web` chỉ kéo về đúng 1 thư viện duy nhất.
8. Nếu không chỉ định `spring.profiles.active`, Spring Boot sẽ báo lỗi không khởi động được.
9. Biến môi trường OS (`SERVER_PORT=9090`) có độ ưu tiên CAO HƠN giá trị trong `application.yml` đóng gói sẵn trong JAR.
10. `@ConfigurationProperties` chỉ hoạt động với class thường (có getter/setter), không dùng được với `record`.

### Phần B — Bài tập viết code (6 bài)

**Bài 1:** Viết cấu trúc `application.yml` (chung) + `application-dev.yml` + `application-prod.yml` cho 1 ứng dụng có DataSource MySQL, với dev dùng `ddl-auto: update` + log SQL bật, prod dùng `ddl-auto: validate` + đọc credential từ biến môi trường.

**Bài 2:** Viết 1 class `@ConfigurationProperties` cho nhóm config `app.jwt` gồm `secretKey`, `expirationMinutes`, và validate `expirationMinutes` phải > 0 bằng `@Min`. Viết lại bằng phong cách `record` (immutable).

**Bài 3:** Viết 1 `@Configuration` class có 2 `@Bean` cùng interface `NotificationSender`, dùng `@Profile("dev")` và `@Profile("prod")` để chọn implementation khác nhau (dev in ra console, prod gửi thật qua HTTP).

**Bài 4:** Giải thích (bằng comment code, không cần chạy thật) cách Auto-configuration của `spring-boot-starter-data-jpa` xác định có nên tạo Bean `DataSource` hay không, dựa trên `@ConditionalOnClass` và `@ConditionalOnMissingBean` — viết pseudo-code minh họa.

**Bài 5:** Thiết kế cấu trúc thư mục project chuẩn (layer-based) cho 1 ứng dụng quản lý "Thư viện" (Library) với các entity: `Book`, `Author`, `Member`, `Loan`. Liệt kê đầy đủ package và ít nhất 1 file ví dụ cho mỗi layer.

**Bài 6 — Custom HealthIndicator + Property Source.** Viết 1 `HealthIndicator` kiểm tra kết nối tới 1 dịch vụ giả định `InventoryServiceClient` (có method `boolean isReachable()`). Sau đó, giải thích: nếu ứng dụng chạy với `application.yml` đóng gói sẵn `server.port: 8080`, nhưng khi deploy production lại chạy lệnh `SERVER_PORT=9090 java -jar app.jar`, ứng dụng thực sự chạy ở cổng nào? Vì sao?

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Đúng.** `@ComponentScan` mặc định chỉ quét package chứa class đó + package con, đặt sai vị trí sẽ bỏ sót Bean.
2. **Sai.** Ngược lại — nhờ `@ConditionalOnMissingBean`, Bean bạn tự khai báo sẽ được **ưu tiên**, Auto-configuration sẽ **không** tạo Bean mặc định nữa.
3. **Sai.** `update` không đáng tin cậy 100% và có thể gây hành vi không mong muốn (VD: đổi tên cột không được nhận diện đúng, gây trùng lặp cột) — luôn dùng `validate` ở production.
4. **Đúng.** Có thể kết hợp `@ConfigurationProperties` với `@Validated` và các annotation như `@NotNull`, `@Min`, `@Max`.
5. **Sai.** YAML **không cho phép dùng Tab**, chỉ chấp nhận Space để thụt lề.
6. **Đúng.** Đây là use case rất phổ biến trong triển khai Kubernetes/container orchestration.
7. **Sai.** Starter là 1 dependency "gộp" — kéo về nhiều thư viện liên quan (Spring MVC, embedded Tomcat, Jackson...).
8. **Sai.** Nếu không chỉ định, Spring Boot chạy với Profile mặc định (`default`), không báo lỗi, chỉ là các config theo Profile riêng (`application-{profile}.yml`) sẽ không được áp dụng.
9. **Đúng.** Theo thứ tự ưu tiên ở mục 9, biến môi trường OS đứng ở vị trí cao hơn nhiều so với `application.yml` đóng gói trong JAR (nguồn thấp nhất).
10. **Sai.** Từ Spring Boot 2.x/3.x hỗ trợ đầy đủ `@ConfigurationProperties` trên `record` qua constructor binding — không cần getter/setter.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```yaml
# application.yml
spring:
  application:
    name: library-app
  profiles:
    active: dev

---
# application-dev.yml
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:mysql://localhost:3306/library_dev
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

---
# application-prod.yml
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/library_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

*(Ghi chú: có thể tách thành 3 file riêng `application.yml`/`application-dev.yml`/`application-prod.yml`, hoặc gộp bằng multi-document YAML với `---` như trên — cả 2 cách đều hợp lệ với Spring Boot 2.4+.)*

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
// Cách 1: class thường (mutable)
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated // Bắt buộc phải có để kích hoạt validate cho @ConfigurationProperties
public class JwtProperties {

    @NotBlank
    private String secretKey;

    @Min(value = 1, message = "expirationMinutes phải lớn hơn 0")
    private int expirationMinutes;

    // getters/setters
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public int getExpirationMinutes() { return expirationMinutes; }
    public void setExpirationMinutes(int expirationMinutes) { this.expirationMinutes = expirationMinutes; }
}

// Cách 2: record (immutable, khuyến nghị cho dự án mới)
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
        @NotBlank String secretKey,
        @Min(value = 1, message = "expirationMinutes phải lớn hơn 0") int expirationMinutes
) {}
```

```yaml
app:
  jwt:
    secret-key: my-super-secret-key
    expiration-minutes: 60
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
public interface NotificationSender {
    void send(String message);
}

@Configuration
public class NotificationConfig {

    @Bean
    @Profile("dev")
    public NotificationSender consoleNotificationSender() {
        return message -> System.out.println("[DEV] Thông báo: " + message);
    }

    @Bean
    @Profile("prod")
    public NotificationSender httpNotificationSender() {
        return message -> {
            // Giả lập gọi HTTP thật tới dịch vụ notification
            System.out.println("[PROD] Gửi HTTP request thông báo: " + message);
        };
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
// Pseudo-code minh họa nguyên lý (không phải source code thật của Spring Boot,
// nhưng phản ánh đúng cơ chế @ConditionalOnClass / @ConditionalOnMissingBean)

@Configuration
@ConditionalOnClass(name = "javax.sql.DataSource")
// Điều kiện 1: classpath phải CÓ class DataSource
// -> Chỉ đúng khi bạn đã thêm dependency như spring-boot-starter-data-jpa
//    (dependency này kéo theo JDBC API có chứa DataSource)
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    // Điều kiện 2: CHƯA có Bean nào type DataSource được khai báo thủ công
    // -> Nếu bạn tự viết @Bean public DataSource dataSource() {...} ở đâu đó,
    //    Auto-configuration này sẽ TỰ ĐỘNG BỎ QUA, không tạo Bean trùng lặp
    public DataSource dataSource(DataSourceProperties properties) {
        // Đọc property từ application.yml (spring.datasource.*)
        // và tạo DataSource tương ứng (mặc định dùng HikariCP)
        return properties.initializeDataSourceBuilder().build();
    }
}

// Kết luận cơ chế:
// 1. Có dependency đúng trong classpath? -> @ConditionalOnClass PASS
// 2. Chưa tự khai báo Bean cùng type?    -> @ConditionalOnMissingBean PASS
// => CẢ 2 điều kiện đúng -> Auto-configuration tạo Bean tự động
// => Chỉ CẦN 1 điều kiện sai -> Auto-configuration bị bỏ qua hoàn toàn
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```
library-app/
├── src/main/java/com/example/library/
│   ├── LibraryApplication.java              # @SpringBootApplication
│   │
│   ├── controller/
│   │   ├── BookController.java              # @RestController - REST endpoint cho Book
│   │   ├── AuthorController.java
│   │   ├── MemberController.java
│   │   └── LoanController.java              # Xử lý mượn/trả sách
│   │
│   ├── service/
│   │   ├── BookService.java                 # Interface (tùy chọn)
│   │   ├── LoanService.java                 # Business logic: kiểm tra sách còn không, hạn mượn...
│   │   └── impl/
│   │       └── LoanServiceImpl.java
│   │
│   ├── repository/
│   │   ├── BookRepository.java              # extends JpaRepository<Book, Long>
│   │   ├── AuthorRepository.java
│   │   ├── MemberRepository.java
│   │   └── LoanRepository.java
│   │
│   ├── entity/
│   │   ├── Book.java                        # @Entity, quan hệ @ManyToOne với Author
│   │   ├── Author.java                      # @OneToMany với Book
│   │   ├── Member.java
│   │   └── Loan.java                        # @ManyToOne Book, @ManyToOne Member
│   │
│   ├── dto/
│   │   ├── BookRequest.java
│   │   ├── BookResponse.java
│   │   └── LoanRequest.java
│   │
│   ├── exception/
│   │   ├── BookNotFoundException.java
│   │   ├── BookNotAvailableException.java   # Sách đã hết, không thể mượn
│   │   └── GlobalExceptionHandler.java      # @RestControllerAdvice
│   │
│   └── config/
│       └── SecurityConfig.java              # Nếu có Spring Security
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
│
└── src/test/java/com/example/library/
    ├── service/LoanServiceTest.java
    └── controller/BookControllerTest.java
```

Ví dụ minh họa 1 file mỗi layer:

```java
// entity/Book.java
@Entity
public class Book {
    @Id @GeneratedValue private Long id;
    private String title;
    private String isbn;
    private int availableCopies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;
}

// repository/BookRepository.java
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
}

// service/impl/LoanServiceImpl.java
@Service
public class LoanServiceImpl implements LoanService {
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public LoanServiceImpl(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    @Transactional
    public Loan borrowBook(Long bookId, Long memberId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        if (book.getAvailableCopies() <= 0) {
            throw new BookNotAvailableException(bookId);
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1); // Dirty Checking tự update
        Loan loan = new Loan(book, memberId, LocalDate.now());
        return loanRepository.save(loan);
    }
}

// controller/LoanController.java
@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> borrowBook(@RequestBody @Valid LoanRequest request) {
        Loan loan = loanService.borrowBook(request.bookId(), request.memberId());
        return ResponseEntity.ok(LoanResponse.from(loan));
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 6</b></summary>

```java
@Component
public class InventoryServiceHealthIndicator implements HealthIndicator {

    private final InventoryServiceClient client;

    public InventoryServiceHealthIndicator(InventoryServiceClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            if (client.isReachable()) {
                return Health.up().withDetail("inventoryService", "reachable").build();
            }
            return Health.down().withDetail("inventoryService", "unreachable").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("inventoryService", "error khi kiểm tra").build();
        }
    }
}
```

**Câu hỏi Property Source:** Ứng dụng sẽ chạy ở **cổng 9090**, KHÔNG PHẢI 8080. Theo bảng thứ tự ưu tiên ở mục 9, **OS Environment Variables** (`SERVER_PORT=9090`, nguồn #5) có độ ưu tiên **cao hơn** `application.yml` đóng gói sẵn trong JAR (nguồn #9, thấp nhất) — nên giá trị từ biến môi trường sẽ **ghi đè** giá trị tĩnh 8080 trong file cấu hình đóng gói sẵn. Đây chính xác là cơ chế cho phép 1 file JAR build 1 lần duy nhất chạy được ở nhiều môi trường khác nhau chỉ bằng cách đổi biến môi trường lúc khởi động.

</details>

---

*File tiếp theo trong lộ trình: **Module 14 — RESTful API Design** (HTTP methods đúng chuẩn REST, status code, versioning API, HATEOAS, Idempotency, Pagination/Filtering/Sorting, Error Response chuẩn hóa).*
