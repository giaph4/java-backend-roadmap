# Module 13 — Spring Boot

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Spring Boot là cách gần như 100% dự án Java Backend hiện nay dùng Spring Framework trong thực tế. Hiểu **Auto-configuration** hoạt động thế nào giúp bạn debug được lỗi "vì sao Bean này tự có mà tôi không khai báo", cấu hình đúng `application.yml` theo từng môi trường (dev/staging/production) bằng **Profiles**, và dùng **Actuator** để giám sát ứng dụng — đây đều là kỹ năng bắt buộc khi đi làm, không chỉ để chạy `mvn spring-boot:run` là xong.

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
9. [Spring Boot Actuator](#9-spring-boot-actuator)
10. [Đóng gói & chạy ứng dụng (Packaging)](#10-đóng-gói--chạy-ứng-dụng)
11. [⚠️ Các bẫy hay gặp](#11-các-bẫy-hay-gặp)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

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
| `spring-boot-starter-actuator` | Health check, metrics giám sát ứng dụng (mục 9) |
| `spring-boot-starter-data-redis` | Tích hợp Redis |
| `spring-boot-starter-amqp` | Tích hợp RabbitMQ |

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

---

## 9. Spring Boot Actuator

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

⚠️ **Lưu ý bảo mật:** Actuator endpoint (đặc biệt `/actuator/env`, `/actuator/heapdump`) chứa thông tin nhạy cảm — **luôn bảo vệ bằng Spring Security** hoặc chỉ expose nội bộ (không public ra Internet), và chỉ `include` những endpoint thực sự cần thiết thay vì `include: "*"`.

> **Liên hệ thực tế:** Trong hệ thống Microservices/Kubernetes, `/actuator/health` chính là endpoint được dùng cho **Liveness Probe** và **Readiness Probe** — Kubernetes tự động restart container nếu health check fail liên tục.

---

## 10. Đóng gói & chạy ứng dụng

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

---

## 11. ⚠️ Các bẫy hay gặp

1. **Đặt class `@SpringBootApplication` sai vị trí** (không ở package gốc) → `@ComponentScan` bỏ sót Bean ở package khác.

2. **`ddl-auto: update` hoặc `create` ở production** → nguy cơ mất dữ liệu, luôn dùng `validate`/`none` + Flyway/Liquibase.

3. **Dùng Tab thay vì Space trong YAML** → lỗi parse âm thầm, khó phát hiện bằng mắt.

4. **Hardcode secret (password, API key) trực tiếp trong `application.yml`** → luôn dùng biến môi trường (`${DB_PASSWORD}`) hoặc secret manager (Vault, AWS Secrets Manager).

5. **Expose toàn bộ Actuator endpoint (`include: "*"`) mà không bảo vệ bằng Security** → rò rỉ thông tin nhạy cảm (`/actuator/env`, `/actuator/heapdump`).

6. **Quên set `spring.profiles.active`** khi deploy → chạy nhầm config `dev` ở môi trường production (VD: kết nối vào DB dev thay vì DB thật).

7. **Không hiểu Auto-configuration nên tự viết `@Bean` trùng lặp** với Bean Spring Boot đã tự tạo sẵn → gây `BeanDefinitionOverrideException` hoặc hành vi không như mong đợi.

8. **Dùng quá nhiều `@Value` rải rác** thay vì gom nhóm bằng `@ConfigurationProperties` → khó maintain khi có nhiều config liên quan.

9. **Không set `server.port` khác nhau khi chạy nhiều service cục bộ cùng lúc** (khi thực hành Microservices sau này) → xung đột cổng.

10. **Nhầm lẫn thứ tự ưu tiên khi có nhiều nguồn config** (command line > biến môi trường > `application-{profile}.yml` > `application.yml`) → không hiểu tại sao giá trị bị "ghi đè" ngoài ý muốn.

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Spring Boot | Lớp convention-over-configuration trên Spring Framework — giảm boilerplate |
| @SpringBootApplication | = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan |
| Auto-configuration | Dựa trên `@ConditionalOnClass`/`@ConditionalOnMissingBean` — tự tạo Bean nếu đủ điều kiện |
| Starter | Gộp nhóm dependency tương thích sẵn — tránh Dependency Hell |
| application.yml | Cấu trúc phân cấp, khuyến nghị hơn `.properties`; cẩn thận Tab vs Space |
| Profiles | `application-{profile}.yml` — tách config theo môi trường (dev/staging/prod) |
| ddl-auto | Production LUÔN dùng `validate`/`none`, không bao giờ `update`/`create` |
| @ConfigurationProperties | Inject cả nhóm config type-safe, tốt hơn nhiều `@Value` rải rác |
| Actuator | `/actuator/health`, `/actuator/metrics` — giám sát production, nhớ bảo mật |
| Fat JAR | Đóng gói code + dependency + embedded server thành 1 file `.jar` duy nhất |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. `@SpringBootApplication` phải luôn được đặt ở package gốc để `@ComponentScan` hoạt động đúng.
2. Auto-configuration của Spring Boot sẽ ghi đè lên Bean bạn tự khai báo nếu trùng type.
3. `ddl-auto: update` an toàn để dùng ở môi trường production vì Hibernate chỉ thêm cột mới, không xóa dữ liệu.
4. `@ConfigurationProperties` cho phép validate giá trị bằng Bean Validation annotation.
5. YAML cho phép dùng cả Tab và Space để thụt lề, miễn là nhất quán trong 1 file.
6. Actuator endpoint `/actuator/health` thường được dùng làm Liveness/Readiness Probe trong Kubernetes.
7. Starter dependency như `spring-boot-starter-web` chỉ kéo về đúng 1 thư viện duy nhất.
8. Nếu không chỉ định `spring.profiles.active`, Spring Boot sẽ báo lỗi không khởi động được.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Viết cấu trúc `application.yml` (chung) + `application-dev.yml` + `application-prod.yml` cho 1 ứng dụng có DataSource MySQL, với dev dùng `ddl-auto: update` + log SQL bật, prod dùng `ddl-auto: validate` + đọc credential từ biến môi trường.

**Bài 2:** Viết 1 class `@ConfigurationProperties` cho nhóm config `app.jwt` gồm `secretKey`, `expirationMinutes`, và validate `expirationMinutes` phải > 0 bằng `@Min`.

**Bài 3:** Viết 1 `@Configuration` class có 2 `@Bean` cùng interface `NotificationSender`, dùng `@Profile("dev")` và `@Profile("prod")` để chọn implementation khác nhau (dev in ra console, prod gửi thật qua HTTP).

**Bài 4:** Giải thích (bằng comment code, không cần chạy thật) cách Auto-configuration của `spring-boot-starter-data-jpa` xác định có nên tạo Bean `DataSource` hay không, dựa trên `@ConditionalOnClass` và `@ConditionalOnMissingBean` — viết pseudo-code minh họa.

**Bài 5:** Thiết kế cấu trúc thư mục project chuẩn (layer-based) cho 1 ứng dụng quản lý "Thư viện" (Library) với các entity: `Book`, `Author`, `Member`, `Loan`. Liệt kê đầy đủ package và ít nhất 1 file ví dụ cho mỗi layer.

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

---

*File tiếp theo trong lộ trình: **Module 14 — RESTful API Design** (HTTP methods đúng chuẩn REST, status code, versioning API, HATEOAS, Idempotency, Pagination/Filtering/Sorting, Error Response chuẩn hóa).*
