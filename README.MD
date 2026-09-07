# ☕ Từ Java Core đến Production-Ready Backend

**Java Backend Engineer — Lộ trình học đầy đủ**

> Sơ đồ tổng hợp kiến thức cần nắm để làm việc thực tế với Java Backend, sắp xếp tuần tự từ nền tảng ngôn ngữ đến hệ sinh thái backend hiện đại (microservices, DevOps, system design). Mỗi mục có nhãn mức độ ưu tiên để biết nên đầu tư thời gian vào đâu trước.

🟠 **Cao** — bắt buộc, phỏng vấn hay hỏi   |   🟢 **TB** — nên biết, hay dùng   |   ⚪ **BS** — biết thêm là lợi thế

---

## 📑 Mục lục

| # | Module |
|---|---|
| 01 | [Java Core & nền tảng ngôn ngữ](#m1) |
| 02 | [OOP & Design Principles](#m2) |
| 03 | [Collections & Generics](#m3) |
| 04 | [Exception Handling & I/O](#m4) |
| 05 | [Multithreading & Concurrency](#m5) |
| 06 | [Java Modern (8 → 21+)](#m6) |
| 07 | [JVM Internals](#m7) |
| 08 | [Design Patterns](#m8) |
| 09 | [Build Tools](#m9) |
| 10 | [Database & SQL](#m10) |
| 11 | [ORM — JPA/Hibernate](#m11) |
| 12 | [Spring Framework Core](#m12) |
| 13 | [Spring Boot](#m13) |
| 14 | [Spring Data & Persistence](#m14) |
| 15 | [Spring Security](#m15) |
| 16 | [RESTful API Design](#m16) |
| 17 | [Testing](#m17) |
| 18 | [Caching & Messaging](#m18) |
| 19 | [Microservices Architecture](#m19) |
| 20 | [DevOps & Containerization](#m20) |
| 21 | [Observability](#m21) |
| 22 | [System Design & Scalability](#m22) |
| 23 | [Bảo mật ứng dụng](#m23) |
| 24 | [Soft Skills & Quy trình làm việc](#m24) |

---

[]()
## `01` Java Core & nền tảng ngôn ngữ

*Nền móng bắt buộc — mọi framework backend (Spring, Hibernate...) đều xây trên các khái niệm này.*

🟠 **Cao** — **Cú pháp & kiểu dữ liệu**
Biến, `primitive types` vs `wrapper class`, ép kiểu (casting), toán tử, mảng, chuỗi (`String`, `StringBuilder`, `StringBuffer` và sự khác biệt hiệu năng), **String Pool & immutability**.

🟠 **Cao** — **Cấu trúc điều khiển**
if/else, switch (kể cả `switch expression` mới), vòng lặp, break/continue, đệ quy cơ bản.

🟠 **Cao** — **Class, Object, Method**
Constructor, overloading, `this` keyword, phạm vi truy cập (`public/private/protected/default`), `static` vs instance member, khối khởi tạo (`static block`, `instance block`).

🟢 **TB** — **Xử lý số & định dạng**
`BigDecimal` cho tiền tệ (tránh lỗi làm tròn float), `Math` class, `DecimalFormat`, làm việc với ngày giờ hiện đại (`LocalDate`, `LocalDateTime`, `Duration`).

---

[]()
## `02` OOP & Design Principles

*Tư duy hướng đối tượng là thứ phân biệt code "chạy được" và code bảo trì được — cực kỳ hay bị hỏi khi phỏng vấn.*

🟠 **Cao** — **4 trụ cột OOP**
**Encapsulation, Inheritance, Polymorphism, Abstraction** — phải giải thích được bằng ví dụ thực tế, không chỉ định nghĩa suông.

🟠 **Cao** — **Interface vs Abstract Class**
Khi nào dùng cái nào, `default method` & `static method` trong interface (Java 8+), đa kế thừa qua interface.

🟠 **Cao** — **SOLID Principles**
`S`ingle Responsibility, `O`pen/Closed, `L`iskov Substitution, `I`nterface Segregation, `D`ependency Inversion — nền tảng để hiểu vì sao Spring thiết kế như vậy (DI, IoC).

🟢 **TB** — **equals(), hashCode(), toString()**
Contract giữa equals/hashCode (bắt buộc khi dùng object làm key trong `HashMap`), `Comparable` vs `Comparator`.

---

[]()
## `03` Collections & Generics

*Dùng sai cấu trúc dữ liệu là nguyên nhân phổ biến nhất gây bug hiệu năng ở backend.*

🟠 **Cao** — **Collections Framework**
`List` (ArrayList vs LinkedList), `Set` (HashSet, LinkedHashSet, TreeSet), `Map` (HashMap, TreeMap, LinkedHashMap) — **độ phức tạp thời gian (Big-O)** của từng thao tác, khi nào chọn cấu trúc nào.

🟠 **Cao** — **Generics**
Generic class/method, bounded types (`t`), wildcard (`? extends`, `? super`) — cần để đọc hiểu source code Spring/Hibernate.

🟢 **TB** — **Stream API & Lambda**
map/filter/reduce/collect, method reference (`::`), xử lý dữ liệu functional-style thay vòng lặp truyền thống — được dùng rất nhiều trong code Spring hiện đại.

⚪ **BS** — **Concurrent Collections**
`ConcurrentHashMap`, `CopyOnWriteArrayList` — dùng khi có truy cập đa luồng.

---

[]()
## `04` Exception Handling & I/O

*Backend chuyên nghiệp = xử lý lỗi tử tế, không để service sập vì 1 exception không bắt được.*

🟠 **Cao** — **Checked vs Unchecked Exception**
try/catch/finally, try-with-resources, custom exception class, **exception chaining** — nền tảng để hiểu `@ControllerAdvice` sau này ở Spring.

🟢 **TB** — **File I/O & Serialization**
`java.nio.file`, đọc/ghi file, `Serializable` interface, làm việc với JSON qua thư viện (Jackson/Gson) ở mức thấp.

---

[]()
## `05` Multithreading & Concurrency

*Backend xử lý hàng nghìn request đồng thời — bắt buộc hiểu để tránh race condition, deadlock trong production.*

🟠 **Cao** — **Thread cơ bản**
`Thread` vs `Runnable`, thread lifecycle, `synchronized`, `volatile`, race condition, deadlock — **hay là câu hỏi loại "khó" trong phỏng vấn**.

🟢 **TB** — **Concurrency Utilities**
`ExecutorService`, `ThreadPoolExecutor`, `Future`/`CompletableFuture`, `CountDownLatch`, `Semaphore`.

⚪ **BS** — **Reactive & Virtual Threads**
`Project Loom` (Virtual Threads — Java 21), lập trình reactive với `Reactor`/`WebFlux` — xu hướng mới, không bắt buộc ngay nhưng nên biết khái niệm.

---

[]()
## `06` Java Modern (8 → 21+)

*Code Java hiện đại trông rất khác Java "cổ điển" — công ty tuyển dụng thường yêu cầu biết các tính năng này.*

🟠 **Cao** — **Java 8 essentials**
Lambda expression, `Optional` (tránh NullPointerException), Stream API, functional interface (`Function`, `Predicate`, `Supplier`, `Consumer`).

🟢 **TB** — **Java 9–17 features**
`var` (Java 10), `record` (Java 16 — rất hay dùng làm DTO), `sealed class`, `Text Block`, pattern matching cho `instanceof` và `switch`.

⚪ **BS** — **Java 21 LTS**
Virtual Threads chính thức, Sequenced Collections, pattern matching nâng cao — nên nắm để đọc hiểu code mới nhất.

---

[]()
## `07` JVM Internals

*Hiểu JVM giúp debug production issue (memory leak, performance) — điểm khác biệt giữa junior và senior.*

🟢 **TB** — **Memory Model**
`Heap` vs `Stack`, `Metaspace`, cách JVM cấp phát object, `Garbage Collection` (thuật toán G1, ZGC ở mức khái niệm).

⚪ **BS** — **Class Loading & Bytecode**
`ClassLoader`, quá trình biên dịch `.java → .class → bytecode`, JIT compiler — kiến thức nền để hiểu công cụ profiling (JProfiler, VisualVM, JFR).

---

[]()
## `08` Design Patterns

*Spring Framework được xây dựng gần như hoàn toàn từ các pattern này — không học phần này thì đọc code Spring sẽ rất mơ hồ.*

🟠 **Cao** — **Creational & Structural**
`Singleton`, `Factory`, `Builder`, `Proxy` (Spring AOP dùng Proxy pattern), `Adapter`.

🟠 **Cao** — **Dependency Injection & IoC**
Khái niệm **Inversion of Control**, tại sao DI giải quyết được vấn đề coupling — đây là "linh hồn" của toàn bộ Spring.

🟢 **TB** — **Behavioral Patterns**
`Strategy`, `Observer`, `Template Method`, `Chain of Responsibility` (dùng trong Filter/Interceptor chain).

---

[]()
## `09` Build Tools & Quản lý dự án

*Không có dự án backend thực tế nào build tay — công cụ build là bắt buộc từ ngày đầu đi làm.*

🟠 **Cao** — **Maven**
`pom.xml`, dependency management, plugin, vòng đời build (compile/test/package), `profile`.

🟢 **TB** — **Gradle**
`build.gradle` (Groovy/Kotlin DSL) — nhiều công ty hiện đại chuyển sang Gradle vì build nhanh hơn.

🟠 **Cao** — **Git & quy trình làm việc nhóm**
branching model (`Gitflow`, trunk-based), pull request, merge conflict, `.gitignore`, commit convention.

---

[]()
## `10` Database & SQL

*Backend về bản chất là "xử lý dữ liệu" — SQL yếu thì mọi framework phía trên đều xây trên cát.*

🟠 **Cao** — **SQL nền tảng**
SELECT/JOIN (INNER/LEFT/RIGHT), GROUP BY, subquery, `transaction` (ACID), `index` — cách index tăng tốc query.

🟠 **Cao** — **Database Design**
Chuẩn hóa dữ liệu (1NF–3NF), thiết kế ERD, khóa chính/khóa ngoại, ràng buộc toàn vẹn dữ liệu.

🟢 **TB** — **RDBMS phổ biến**
`MySQL`/`PostgreSQL` — ít nhất thành thạo một loại, biết `EXPLAIN` để phân tích query chậm.

⚪ **BS** — **NoSQL**
`MongoDB` (document), `Redis` (key-value, cũng dùng cho cache) — biết khi nào NoSQL phù hợp hơn SQL.

---

[]()
## `11` ORM — JPA / Hibernate

*Cầu nối giữa OOP và quan hệ dữ liệu — hầu hết mọi service Spring Boot đều dùng lớp này để chạm database.*

🟠 **Cao** — **JPA Fundamentals**
`@Entity`, `@Id`, mapping quan hệ (`@OneToMany`, `@ManyToOne`, `@ManyToMany`), `EntityManager`, Persistence Context.

🟠 **Cao** — **Fetch Strategy & Performance**
**Lazy vs Eager loading**, vấn đề `N+1 query` (rất hay gặp và hay bị hỏi phỏng vấn), `JOIN FETCH`, DTO projection.

🟢 **TB** — **Query & Transaction**
`JPQL`, `Criteria API`, `@Transactional` — propagation & isolation level, first/second-level cache của Hibernate.

---

[]()
## `12` Spring Framework Core

*Đây là điểm chuyển giao then chốt: từ "biết Java" sang "làm được Backend thật".*

🟠 **Cao** — **IoC Container & Bean**
`ApplicationContext`, Bean lifecycle, `@Component`/`@Service`/`@Repository`, Bean scope (singleton/prototype).

🟠 **Cao** — **Dependency Injection**
Constructor injection (khuyến nghị) vs field injection, `@Autowired`, `@Qualifier`, giải quyết circular dependency.

🟢 **TB** — **AOP (Aspect-Oriented Programming)**
`@Aspect`, pointcut/advice — dùng để tách logging, transaction, security ra khỏi business logic.

---

[]()
## `13` Spring Boot

*Framework thực chiến — gần như 100% job posting "Java Backend" ở Việt Nam yêu cầu Spring Boot.*

🟠 **Cao** — **Auto-configuration & Starters**
`@SpringBootApplication`, cách Boot tự cấu hình, `application.yml/properties`, profile theo môi trường (dev/staging/prod).

🟠 **Cao** — **Web Layer**
`@RestController`, `@RequestMapping`/@GetMapping..., binding request (`@RequestBody`, `@PathVariable`, `@RequestParam`), validation (`@Valid`, Bean Validation).

🟢 **TB** — **Xử lý lỗi tập trung**
`@ControllerAdvice` + `@ExceptionHandler` — chuẩn hóa response lỗi toàn hệ thống.

⚪ **BS** — **Actuator**
Health check, metrics endpoint có sẵn — nền tảng cho phần Observability sau này.

---

[]()
## `14` Spring Data & Persistence

*Lớp truy xuất dữ liệu chuẩn hóa — giảm code lặp lại tới mức tối đa.*

🟠 **Cao** — **Spring Data JPA**
`JpaRepository`, derived query method (`findByEmail`...), `@Query` tự viết, phân trang & sắp xếp (`Pageable`, `Sort`).

🟢 **TB** — **Database Migration**
`Flyway` hoặc `Liquibase` — quản lý version schema database, bắt buộc trong team làm việc thật (không dùng `ddl-auto: update` ở production).

---

[]()
## `15` Spring Security

*Backend không bảo mật = backend không dùng được — bất kỳ hệ thống thật nào cũng cần phần này.*

🟠 **Cao** — **Authentication & Authorization**
Filter chain của Spring Security, `UserDetailsService`, phân quyền theo `Role`/`Authority`, `@PreAuthorize`.

🟠 **Cao** — **JWT & Stateless Auth**
Cơ chế `JSON Web Token` (access token/refresh token), lưu ý bảo mật khi lưu token phía client.

🟢 **TB** — **OAuth2 & SSO**
`OAuth2` flow, đăng nhập qua bên thứ ba (Google/Facebook login), `Keycloak` ở mức khái niệm.

⚪ **BS** — **Mã hóa & hashing**
`BCrypt` cho mật khẩu, khái niệm HTTPS/TLS, CORS configuration.

---

[]()
## `16` RESTful API Design

*Thiết kế API tốt là kỹ năng phân biệt dev "viết code chạy" và dev "thiết kế hệ thống dùng lâu dài".*

🟠 **Cao** — **Nguyên tắc REST**
HTTP method đúng ngữ nghĩa (GET/POST/PUT/PATCH/DELETE), HTTP status code chuẩn, thiết kế URI resource-oriented, versioning API.

🟢 **TB** — **Tài liệu hóa API**
`OpenAPI/Swagger` — tự sinh doc từ code, rất quan trọng khi làm việc với frontend team.

⚪ **BS** — **GraphQL**
Khái niệm cơ bản, khi nào chọn GraphQL thay vì REST.

---

[]()
## `17` Testing

*Không test = không dám deploy. Kỹ năng bắt buộc trong mọi team chuyên nghiệp.*

🟠 **Cao** — **Unit Testing**
`JUnit 5`, `Mockito` (mock dependency), test coverage, viết test cho service layer.

🟢 **TB** — **Integration Testing**
`@SpringBootTest`, `TestContainers` (test với database thật trong container), `MockMvc` cho test controller.

⚪ **BS** — **API Testing thủ công**
`Postman`/`Insomnia` — kiểm thử API bằng tay khi phát triển.

---

[]()
## `18` Caching & Messaging

*Khi hệ thống có traffic cao, không thể mỗi request đều chạm database — đây là lúc cần các công cụ này.*

🟢 **TB** — **Caching**
`Redis` làm cache layer, `@Cacheable` trong Spring, chiến lược cache invalidation.

🟢 **TB** — **Message Queue**
`Kafka` hoặc `RabbitMQ` — xử lý bất đồng bộ (async processing), giao tiếp giữa các service, event-driven architecture.

---

[]()
## `19` Microservices Architecture

*Bước tiếp theo sau khi thành thạo monolith Spring Boot — kiến trúc phổ biến ở các công ty vừa và lớn.*

🟢 **TB** — **Nguyên lý & thành phần**
Service decomposition, `API Gateway`, `Service Discovery` (Eureka/Consul), giao tiếp đồng bộ (REST/gRPC) vs bất đồng bộ (message queue).

🟢 **TB** — **Spring Cloud**
`Spring Cloud Gateway`, `Config Server` (quản lý cấu hình tập trung), `Circuit Breaker` (Resilience4j) — chống lỗi lan truyền giữa các service.

⚪ **BS** — **Distributed Transaction**
Saga pattern, eventual consistency — khái niệm nâng cao khi dữ liệu trải trên nhiều service.

---

[]()
## `20` DevOps & Containerization

*Backend hiện đại không tách rời khỏi việc build, đóng gói và triển khai — nhà tuyển dụng đánh giá cao ứng viên biết phần này.*

🟠 **Cao** — **Docker**
Viết `Dockerfile` cho ứng dụng Spring Boot, `docker-compose` để chạy app + database + redis cùng lúc.

🟢 **TB** — **CI/CD**
`GitHub Actions`/`Jenkins` — tự động build, test, deploy khi push code.

⚪ **BS** — **Kubernetes**
Khái niệm Pod/Deployment/Service — không cần thành thạo ngay, nhưng nên hiểu tổng quan vì nhiều công ty lớn dùng để orchestrate microservices.

---

[]()
## `21` Observability

*Biết hệ thống đang "khỏe" hay không, khi có sự cố phải tìm ra nguyên nhân nhanh nhất có thể.*

⚪ **BS** — **Logging & Monitoring**
`SLF4J`/`Logback`, tập trung log với `ELK Stack` (Elasticsearch/Logstash/Kibana), giám sát metrics với `Prometheus` + `Grafana`.

⚪ **BS** — **Distributed Tracing**
`Zipkin`/`Jaeger` — theo dõi một request đi qua bao nhiêu service trong hệ thống microservices.

---

[]()
## `22` System Design & Scalability

*Cấp độ tư duy kiến trúc — quyết định hệ thống có "sống sót" khi traffic tăng gấp 100 lần hay không.*

🟢 **TB** — **Khái niệm nền tảng**
`Load Balancing`, `Horizontal` vs `Vertical Scaling`, `CAP theorem`, database sharding & replication.

⚪ **BS** — **Thiết kế hệ thống lớn**
Rate limiting, idempotency trong API, thiết kế hệ thống chịu tải cao (ví dụ: hệ thống đặt vé, flash-sale) — cấp độ phỏng vấn senior/system design interview.

---

[]()
## `23` Bảo mật ứng dụng (AppSec)

*Kiến thức bảo mật ứng dụng ngoài phạm vi Spring Security — cần để không viết ra lỗ hổng cho hacker khai thác.*

🟢 **TB** — **OWASP Top 10**
`SQL Injection`, `XSS`, `CSRF` — hiểu cơ chế tấn công và cách phòng chống ở tầng code.

⚪ **BS** — **Input Validation & Secrets Management**
Validate/sanitize input triệt để, không hardcode secret/API key trong code, dùng biến môi trường hoặc vault.

---

[]()
## `24` Soft Skills & Quy trình làm việc

*Kiến thức kỹ thuật giỏi vẫn cần đi kèm khả năng làm việc trong một team thật để trở thành backend developer hoàn chỉnh.*

🟠 **Cao** — **Đọc hiểu & viết tài liệu kỹ thuật**
Đọc code người khác, viết README rõ ràng, giải thích quyết định kỹ thuật (technical decision) cho người không rành sâu.

🟢 **TB** — **Agile/Scrum**
Sprint, standup, code review — quy trình làm việc phổ biến nhất ở các công ty phần mềm hiện nay.

> **💡 Gợi ý lộ trình thực hành:** Học xong Module 01–08 (Java Core + OOP + Design Pattern) trước khi động vào Spring — nếu nhảy thẳng vào Spring Boot mà chưa vững OOP/DI, code sẽ "chạy được" nhưng không hiểu vì sao. Module 09–17 là bộ kỹ năng tối thiểu để nhận việc Junior/Fresher. Module 18–24 là phần nâng cấp lên Middle/Senior, học dần qua dự án thực tế, không cần thuộc lòng trước khi đi làm.

---

// end of roadmap — Java Core → Production Backend
