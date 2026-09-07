# Module 19 — Microservices

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Đây là module tổng hợp toàn bộ kiến thức từ Module 12-18 (Spring Core, Spring Boot, REST API, Persistence, Security, Testing, Caching/Messaging) và áp dụng vào 1 kiến trúc hệ thống **phân tán (distributed)**. Microservices không phải "công nghệ mới cần học" — nó là **cách tổ chức hệ thống** giải quyết vấn đề khi Monolith trở nên quá lớn để phát triển/scale. Hiểu đúng khi nào NÊN và KHÔNG NÊN dùng Microservices, cùng các vấn đề cố hữu của hệ phân tán (Network không đáng tin cậy, Distributed Transaction, Service Discovery) là kiến thức phân biệt rõ ràng Junior và Senior Backend Developer.

---

## Mục lục

1. [Monolith vs Microservices](#1-monolith-vs-microservices)
2. [Khi nào NÊN và KHÔNG NÊN dùng Microservices](#2-khi-nào-nên-và-không-nên-dùng-microservices)
3. [Service Discovery](#3-service-discovery)
4. [API Gateway](#4-api-gateway)
5. [Inter-service Communication](#5-inter-service-communication)
6. [Circuit Breaker & Resilience4j](#6-circuit-breaker--resilience4j)
7. [Distributed Transaction & Saga Pattern](#7-distributed-transaction--saga-pattern)
8. [Database per Service & Data Consistency](#8-database-per-service--data-consistency)
9. [⚠️ Các bẫy hay gặp](#9-các-bẫy-hay-gặp)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Monolith vs Microservices

### Monolith (Kiến trúc nguyên khối)

```
┌─────────────────────────────────────────┐
│           1 Ứng dụng duy nhất              │
│  ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐  │
│  │ User   │ │ Order  │ │Payment │ │Inventory│ │
│  │ Module │ │ Module │ │ Module │ │ Module │  │
│  └───────┘ └───────┘ └───────┘ └───────┘  │
│                                              │
│           1 Database duy nhất               │
└─────────────────────────────────────────┘
       Deploy: 1 file .jar/.war DUY NHẤT
```

Đây chính là những gì bạn đã xây dựng xuyên suốt Module 12-18 — 1 ứng dụng Spring Boot với nhiều package (`user/`, `order/`, `payment/`...) nhưng **build và deploy như 1 khối thống nhất**.

### Microservices (Kiến trúc vi dịch vụ)

```
┌───────────┐   ┌───────────┐   ┌───────────┐   ┌───────────┐
│User Service│   │OrderService│   │PaymentService│  │InventoryService│
│  (Spring   │   │  (Spring   │   │  (Spring   │   │  (Spring   │
│   Boot)    │   │   Boot)    │   │   Boot)    │   │   Boot)    │
├───────────┤   ├───────────┤   ├───────────┤   ├───────────┤
│  User DB   │   │  Order DB  │   │ Payment DB │   │Inventory DB│
└───────────┘   └───────────┘   └───────────┘   └───────────┘
     Mỗi service: Deploy ĐỘC LẬP, Database RIÊNG, có thể viết bằng ngôn ngữ khác nhau
     Giao tiếp qua mạng: REST API / Message Queue (đã học ở Module 14, 18)
```

### So sánh chi tiết

| | Monolith | Microservices |
|---|---|---|
| Deploy | 1 lần deploy cho toàn bộ ứng dụng | Deploy ĐỘC LẬP từng service |
| Database | 1 DB chung cho toàn bộ | **Mỗi service 1 DB riêng** (Database per Service) |
| Scale | Scale TOÀN BỘ ứng dụng (dù chỉ 1 phần cần tải cao) | Scale ĐỘC LẬP từng service theo nhu cầu thực tế |
| Giao tiếp giữa module | Gọi hàm trực tiếp (in-process, cực nhanh, luôn tin cậy) | Gọi qua mạng (network call — CHẬM HƠN, CÓ THỂ LỖI) |
| Công nghệ | Thường 1 ngôn ngữ/framework thống nhất | Có thể mỗi service dùng ngôn ngữ/công nghệ khác nhau |
| Độ phức tạp ban đầu | Thấp — dễ bắt đầu, dễ debug (mọi thứ trong 1 process) | **Cao** — cần hạ tầng thêm (Service Discovery, API Gateway, distributed tracing...) |
| Team ownership | Khó chia team độc lập (code chung 1 repo, dễ xung đột) | Mỗi team sở hữu 1 (vài) service riêng, ít xung đột hơn |
| Testing | Đơn giản hơn (test trong 1 process) | Phức tạp hơn (cần test cả tương tác giữa các service — Contract Testing, E2E) |
| Transaction | ACID transaction đơn giản (1 DB) | **Distributed Transaction phức tạp** (nhiều DB riêng biệt — mục 7) |

> **Nguyên tắc thực chiến quan trọng nhất của module này:** Microservices **KHÔNG PHẢI** "kiến trúc tốt hơn" một cách tuyệt đối — nó là sự **đánh đổi (trade-off)**: đổi lấy khả năng scale/deploy độc lập bằng cái giá của độ phức tạp vận hành cao hơn rất nhiều. Rất nhiều công ty (kể cả lớn) vẫn chạy Monolith thành công, và không ít công ty áp dụng Microservices quá sớm đã phải "gộp lại" ("Monolith First" là 1 trường phái kiến trúc được nhiều Senior Engineer ủng hộ).

---

## 2. Khi nào NÊN và KHÔNG NÊN dùng Microservices

### Dấu hiệu NÊN cân nhắc Microservices

- Đội ngũ phát triển đã **lớn** (nhiều team, > 20-30 developer), Monolith gây xung đột code liên tục, khó coordinate deploy
- Các phần của hệ thống có **nhu cầu scale rất khác nhau** (VD: service xử lý thanh toán cần scale mạnh dịp sale, nhưng service quản lý nội dung tĩnh thì không)
- Cần **công nghệ khác nhau** cho từng phần (VD: 1 service cần Python cho Machine Learning, phần còn lại dùng Java)
- Hệ thống đã đủ **trưởng thành, ranh giới nghiệp vụ (domain boundary) đã rõ ràng** — biết chắc "Order" và "Payment" là 2 domain tách biệt, ít khi cần sửa chung

### Dấu hiệu KHÔNG NÊN dùng Microservices (ít nhất là chưa vội)

- Team **nhỏ** (dưới 10 người) — chi phí vận hành hạ tầng phân tán (Service Discovery, monitoring, distributed tracing...) áp đảo lợi ích
- Dự án **mới bắt đầu**, domain boundary **CHƯA RÕ RÀNG** — chia service quá sớm khi chưa hiểu hết nghiệp vụ dễ dẫn tới chia SAI ranh giới, sau này phải refactor lại toàn bộ (tốn kém hơn nhiều so với refactor trong Monolith)
- Chưa có kinh nghiệm/hạ tầng DevOps đủ mạnh để vận hành hệ thống phân tán (Kubernetes, CI/CD cho nhiều service, centralized logging...)

> **"Monolith First" strategy:** Nhiều kiến trúc sư có kinh nghiệm khuyến nghị **bắt đầu bằng Monolith** (nhưng thiết kế **module hóa tốt** bên trong — package theo domain rõ ràng như đã đề cập ở Module 13), rồi **tách dần thành Microservices** khi thực sự cần thiết (khi đã hiểu rõ domain boundary qua thực tế vận hành) — thay vì thiết kế Microservices ngay từ đầu dựa trên phỏng đoán domain boundary có thể sai.

---

## 3. Service Discovery

**Vấn đề:** Trong Microservices, mỗi service có thể chạy trên **nhiều instance** (để scale), với địa chỉ IP/port **thay đổi liên tục** (do container bị restart, auto-scaling thêm/bớt instance...). Làm sao Service A biết được **địa chỉ hiện tại** của Service B để gọi?

### Không có Service Discovery (hardcode địa chỉ — không khả thi trong thực tế)

```yaml
# ❌ Cách này KHÔNG hoạt động khi instance của order-service thay đổi địa chỉ
order-service:
  url: http://192.168.1.15:8080  # Địa chỉ CỐ ĐỊNH -> sẽ SAI khi instance restart ở địa chỉ khác
```

### Với Service Discovery

```
┌─────────────────────────────────────────┐
│           Service Registry (Eureka)        │
│  "order-service" -> [10.0.0.1:8080,        │
│                       10.0.0.2:8080,        │
│                       10.0.0.3:8080]        │  <- Danh sách instance CẬP NHẬT liên tục
└─────────────────────────────────────────┘
        ▲ đăng ký (register)      │ tra cứu (discover)
        │                          ▼
┌──────────────┐          ┌──────────────┐
│ Order Service  │          │ Payment Service │
│ (Instance 1-3) │          │ (gọi Order qua   │
│                │          │  tên "order-service"│
└──────────────┘          │  thay vì IP cụ thể) │
                            └──────────────┘
```

**Cơ chế hoạt động:**
1. Mỗi service instance khi khởi động **tự đăng ký (register)** địa chỉ của mình vào Service Registry
2. Định kỳ gửi **Heartbeat** (tín hiệu "tôi vẫn còn sống") — Registry tự động loại bỏ instance không phản hồi (coi như đã chết)
3. Service khác muốn gọi, chỉ cần hỏi Registry theo **tên service** (VD: "order-service"), Registry trả về danh sách địa chỉ instance đang hoạt động

### Spring Cloud Netflix Eureka (phổ biến trong hệ sinh thái Spring)

```xml
<!-- Eureka Server (Service Registry) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableEurekaServer // Ứng dụng này TRỞ THÀNH Service Registry
public class EurekaServerApplication { }
```

```xml
<!-- Mỗi Microservice (Client đăng ký vào Eureka) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```yaml
# application.yml của order-service
spring:
  application:
    name: order-service # Tên này sẽ được dùng để tra cứu qua Service Discovery
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

> **Xu hướng hiện đại:** Với hệ thống chạy trên **Kubernetes**, Service Discovery thường được Kubernetes **tự đảm nhiệm** (qua Kubernetes Service + DNS nội bộ) — không cần thêm Eureka riêng. Eureka vẫn phổ biến trong môi trường không dùng Kubernetes hoặc hệ thống cũ hơn.

---

## 4. API Gateway

**Vấn đề:** Nếu Client (Frontend/Mobile App) phải tự gọi **trực tiếp** tới từng Microservice riêng lẻ:

```
❌ Client phải biết địa chỉ VÀ gọi riêng từng service:
Client -> GET http://order-service:8081/orders/5
Client -> GET http://user-service:8082/users/10
Client -> GET http://payment-service:8083/payments/order/5
```

**Vấn đề:** Client phải biết địa chỉ của TẤT CẢ service, xử lý Authentication/CORS ở TỪNG service riêng biệt, khó thay đổi cấu trúc service phía sau mà không ảnh hưởng Client.

### Với API Gateway — 1 điểm vào DUY NHẤT

```
                    ┌──────────────┐
Client ────────────►│ API Gateway   │
                    └──────┬───────┘
                           │ Route theo path
              ┌────────────┼────────────┐
              ▼             ▼             ▼
      ┌───────────┐ ┌───────────┐ ┌───────────┐
      │Order Service│ │User Service │ │Payment Service│
      └───────────┘ └───────────┘ └───────────┘
```

```
Client -> GET https://api.myapp.com/orders/5      -> Gateway route tới order-service
Client -> GET https://api.myapp.com/users/10       -> Gateway route tới user-service
Client -> GET https://api.myapp.com/payments/5     -> Gateway route tới payment-service
```

### Các trách nhiệm tập trung tại API Gateway

| Trách nhiệm | Giải thích |
|---|---|
| **Routing** | Định tuyến request tới đúng service dựa trên path/header |
| **Authentication** | Xác thực JWT **1 LẦN** tại Gateway, không cần lặp lại logic ở từng service |
| **Rate Limiting** | Giới hạn số request/giây từ 1 client — chống lạm dụng/DDoS |
| **Load Balancing** | Phân phối request đều tới nhiều instance của cùng 1 service |
| **Logging/Monitoring tập trung** | Ghi log mọi request đi qua tại 1 điểm duy nhất, dễ giám sát tổng thể |
| **Response Aggregation** | Gộp kết quả từ nhiều service thành 1 response duy nhất cho Client (giảm số lần round-trip) |

### Spring Cloud Gateway (thay thế cho Zuul đã cũ)

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service-route
          uri: lb://order-service # "lb://" -> Load Balanced, tự động dùng Service Discovery để tìm instance
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - StripPrefix=0

        - id: user-service-route
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**
```

```java
// Hoặc cấu hình bằng code (Java Config) - linh hoạt hơn YAML cho logic phức tạp
@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("order-service", r -> r.path("/api/v1/orders/**")
                        .filters(f -> f.rewritePath("/api/v1/orders/(?<segment>.*)", "/orders/${segment}"))
                        .uri("lb://order-service"))
                .build();
    }
}
```

---

## 5. Inter-service Communication

### 5.1. Synchronous — REST/gRPC (Service A gọi Service B, CHỜ phản hồi ngay)

```java
@Service
public class OrderService {

    private final RestClient userServiceClient; // Hoặc dùng OpenFeign, WebClient

    public Order createOrder(OrderRequest request) {
        // Gọi ĐỒNG BỘ sang User Service để lấy thông tin user (CHỜ phản hồi trước khi tiếp tục)
        UserDto user = userServiceClient.get()
                .uri("/users/{id}", request.userId())
                .retrieve()
                .body(UserDto.class);

        if (user == null) {
            throw new UserNotFoundException(request.userId());
        }
        // ... tiếp tục logic tạo order
    }
}
```

**OpenFeign — cách viết REST Client "khai báo" (declarative), phổ biến trong hệ sinh thái Spring Cloud:**

```java
@FeignClient(name = "user-service") // Tự động tích hợp với Service Discovery (dùng tên thay vì URL cụ thể)
public interface UserServiceClient {
    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable Long id);
}

@Service
public class OrderService {
    private final UserServiceClient userServiceClient; // Chỉ cần gọi như 1 method Java bình thường

    public Order createOrder(OrderRequest request) {
        UserDto user = userServiceClient.getUser(request.userId()); // Ẩn toàn bộ chi tiết HTTP call bên dưới
        // ...
    }
}
```

### 5.2. Asynchronous — Message Queue (đã học ở Module 18)

```java
@Service
public class OrderService {
    private final RabbitTemplate rabbitTemplate;

    public Order createOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        // KHÔNG chờ phản hồi - publish event, để service khác TỰ xử lý độc lập
        rabbitTemplate.convertAndSend("order.exchange", "order.created", new OrderCreatedEvent(order.getId()));
        return order;
    }
}
```

### So sánh Synchronous vs Asynchronous trong giao tiếp Microservices

| | Synchronous (REST/gRPC) | Asynchronous (Message Queue) |
|---|---|---|
| Độ trễ | Client chờ phản hồi ngay | Không chờ — xử lý sau |
| Coupling | **Chặt hơn** — Service A "biết" và phụ thuộc trực tiếp Service B còn sống | **Lỏng hơn** — Producer không cần biết Consumer có tồn tại/đang chạy không |
| Khi Service B down | Service A **fail ngay lập tức** (trừ khi có Circuit Breaker — mục 6) | Message vẫn nằm trong Queue, xử lý được khi Service B hồi phục |
| Phù hợp khi nào | Cần dữ liệu NGAY để tiếp tục xử lý (VD: cần biết `user` có tồn tại trước khi tạo order) | Tác vụ có thể trễ (VD: gửi email, đồng bộ dữ liệu sang service khác) |

> **Cạm bẫy kiến trúc phổ biến: "Distributed Monolith"** — khi các Microservices gọi Synchronous chằng chịt lẫn nhau (Service A gọi B, B gọi C, C gọi lại A...) tạo thành **chuỗi phụ thuộc chặt chẽ giống hệt Monolith**, nhưng lại phải chịu thêm độ trễ mạng và rủi ro lỗi của hệ phân tán — tệ hơn cả 2 kiến trúc. Nguyên tắc thiết kế tốt: **ưu tiên Asynchronous** cho giao tiếp không cần phản hồi ngay lập tức.

---

## 6. Circuit Breaker & Resilience4j

### Vấn đề: Cascading Failure (Lỗi dây chuyền)

```
Order Service gọi Payment Service (đang bị chậm/down)
     │
     ▼
Order Service CHỜ timeout (VD: 30 giây) cho MỖI request
     │
     ▼
Hàng nghìn request cùng lúc đều "kẹt" chờ Payment Service
     │
     ▼
Order Service CẠN KIỆT Thread Pool (tất cả thread đều đang chờ)
     │
     ▼
Order Service KHÔNG THỂ xử lý request MỚI nào nữa (dù không liên quan Payment)
     │
     ▼
Toàn bộ hệ thống "sập dây chuyền" dù chỉ 1 service con bị lỗi!
```

### Circuit Breaker Pattern — giải pháp "cầu dao điện"

Lấy cảm hứng từ cầu dao điện thật — khi phát hiện dòng điện bất thường, cầu dao **TỰ NGẮT** để bảo vệ toàn bộ hệ thống, thay vì để nó tiếp tục gây cháy nổ lan rộng.

```
State: CLOSED (bình thường)
   │  Request đi qua bình thường, Circuit Breaker đếm số lần lỗi
   │
   ▼ (tỷ lệ lỗi vượt ngưỡng, VD: > 50% request lỗi trong 10 giây)
State: OPEN (Circuit "mở" - NGẮT hoàn toàn)
   │  MỌI request bị CHẶN NGAY LẬP TỨC (fail-fast), KHÔNG gọi Payment Service nữa
   │  -> Trả lỗi/fallback NGAY, không tốn thời gian chờ timeout
   │
   ▼ (sau 1 khoảng thời gian chờ, VD: 30 giây)
State: HALF_OPEN (Thử nghiệm)
   │  Cho phép 1 SỐ ÍT request đi qua để "thử" xem Payment Service đã hồi phục chưa
   │
   ├─► Nếu request thử thành công -> chuyển về CLOSED (hoạt động bình thường trở lại)
   └─► Nếu vẫn lỗi -> quay lại OPEN (tiếp tục chờ)
```

### Resilience4j — thư viện Circuit Breaker phổ biến cho Spring Boot (thay thế Hystrix đã ngừng phát triển)

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10           # Xét 10 request gần nhất
        failure-rate-threshold: 50        # Nếu >= 50% lỗi -> chuyển sang OPEN
        wait-duration-in-open-state: 30s  # Chờ 30s trước khi thử lại (chuyển sang HALF_OPEN)
        permitted-number-of-calls-in-half-open-state: 3 # Cho phép 3 request thử nghiệm
```

```java
@Service
public class OrderService {

    private final PaymentServiceClient paymentServiceClient;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResult processPayment(Order order) {
        return paymentServiceClient.charge(order.getTotalAmount());
        // Nếu Circuit Breaker đang OPEN -> method này KHÔNG được gọi,
        // Resilience4j tự động gọi THẲNG fallbackMethod bên dưới
    }

    // Fallback method - PHẢI có cùng tham số + thêm 1 tham số Throwable ở cuối
    private PaymentResult paymentFallback(Order order, Throwable throwable) {
        log.warn("Payment Service đang gặp sự cố, dùng phương án dự phòng cho order {}", order.getId());
        return PaymentResult.pending("Đơn hàng đang chờ xử lý thanh toán, sẽ thông báo sau");
        // Trả về kết quả "graceful degradation" thay vì để lỗi lan ra toàn hệ thống
    }
}
```

### Các pattern liên quan khác trong Resilience4j

```java
@Retry(name = "paymentService", fallbackMethod = "paymentFallback")
// Tự động RETRY lại N lần nếu lỗi (kết hợp Circuit Breaker để tránh retry vô tận khi service đã "chết hẳn")
@RateLimiter(name = "paymentService")
// Giới hạn số lượng request gọi sang Payment Service trong 1 khoảng thời gian
@Bulkhead(name = "paymentService")
// Giới hạn số lượng THREAD ĐỒNG THỜI được phép gọi Payment Service
// -> cô lập tài nguyên, tránh 1 service "ngốn" hết thread pool ảnh hưởng service khác (giống khoang tàu thủy)
@TimeLimiter(name = "paymentService")
// Giới hạn THỜI GIAN chờ tối đa cho 1 request - tự động fail nếu quá lâu
public PaymentResult processPayment(Order order) { ... }
```

> **Liên hệ tên gọi "Bulkhead":** Lấy cảm hứng từ khoang chống thấm nước trên tàu thủy — nếu 1 khoang bị thủng, các khoang khác vẫn hoạt động bình thường nhờ được "cô lập" — áp dụng vào phần mềm: 1 dependency chậm không được phép "ngốn" hết tài nguyên (thread) dùng chung cho toàn bộ ứng dụng.

---

## 7. Distributed Transaction & Saga Pattern

### Vấn đề: Không còn ACID Transaction đơn giản như Module 11/15

Trong Monolith, đặt hàng có thể là **1 transaction duy nhất** (nhờ `@Transactional`, tất cả cùng commit hoặc cùng rollback):

```java
@Transactional
public void placeOrder(OrderRequest request) {
    orderRepository.save(order);           // Cùng 1 DB
    inventoryRepository.reduceStock(...);   // Cùng 1 DB
    paymentRepository.charge(...);          // Cùng 1 DB
    // Nếu bất kỳ bước nào lỗi -> TOÀN BỘ rollback tự động, đơn giản
}
```

Nhưng trong Microservices, **mỗi service có DB RIÊNG** — `@Transactional` của Spring **KHÔNG THỂ** bao trọn nhiều database khác nhau qua network:

```
Order Service (Order DB) -> gọi -> Inventory Service (Inventory DB) -> gọi -> Payment Service (Payment DB)

Nếu Payment Service THẤT BẠI ở bước cuối, làm sao ROLLBACK lại
những gì Order Service và Inventory Service ĐÃ COMMIT rồi (ở 2 DB khác nhau)?
```

### Saga Pattern — giải pháp cho Distributed Transaction

**Saga** chia 1 giao dịch lớn thành **chuỗi các giao dịch cục bộ (local transaction)** nhỏ hơn — mỗi bước tự commit vào DB riêng của nó, và nếu 1 bước thất bại, thực hiện **Compensating Transaction** (giao dịch bù trừ) để "hoàn tác" các bước ĐÃ thành công trước đó theo chiều ngược lại.

```
Bước 1: Order Service    -> tạo Order (status=PENDING)         [Local Transaction 1 - COMMIT]
Bước 2: Inventory Service -> trừ tồn kho                          [Local Transaction 2 - COMMIT]
Bước 3: Payment Service   -> thu tiền                             [Local Transaction 3 - THẤT BẠI!]

-> Compensating Transaction (thực hiện NGƯỢC LẠI):
Bước 3': (không cần - Payment chưa commit gì)
Bước 2': Inventory Service -> HOÀN LẠI tồn kho đã trừ            [Compensating Transaction]
Bước 1': Order Service     -> đổi Order status = CANCELLED       [Compensating Transaction]
```

### 2 cách triển khai Saga

**1. Choreography (Biên đạo — mỗi service TỰ LẮNG NGHE event và tự quyết định hành động, không có "nhạc trưởng"):**

```
Order Service: tạo Order -> publish "OrderCreated"
     │
     ▼ (Inventory Service lắng nghe "OrderCreated")
Inventory Service: trừ tồn kho -> publish "StockReserved" (hoặc "StockReservationFailed")
     │
     ▼ (Payment Service lắng nghe "StockReserved")
Payment Service: thu tiền -> publish "PaymentCompleted" (hoặc "PaymentFailed")
     │
     ▼ (Order Service lắng nghe "PaymentFailed" -> TỰ thực hiện Compensating Transaction)
Order Service: nhận "PaymentFailed" -> đổi Order status = CANCELLED
     │
     ▼ (Inventory Service CŨNG lắng nghe "PaymentFailed" -> tự hoàn tồn kho)
Inventory Service: nhận "PaymentFailed" -> hoàn lại tồn kho đã trừ
```

```java
@Component
public class InventoryEventListener {
    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        boolean success = inventoryService.reserveStock(event.orderId(), event.items());
        if (success) {
            eventPublisher.publish("stock.reserved", new StockReservedEvent(event.orderId()));
        } else {
            eventPublisher.publish("stock.reservation.failed", new StockReservationFailedEvent(event.orderId()));
        }
    }

    @RabbitListener(queues = "payment.failed.queue") // Lắng nghe để BÙ TRỪ khi bước sau thất bại
    public void handlePaymentFailed(PaymentFailedEvent event) {
        inventoryService.releaseStock(event.orderId()); // Compensating Transaction
    }
}
```

**2. Orchestration (Điều phối — có 1 "nhạc trưởng" trung tâm điều khiển toàn bộ luồng):**

```java
@Component
public class OrderSagaOrchestrator {

    public void executeOrderSaga(OrderRequest request) {
        Long orderId = orderService.createOrder(request);
        try {
            inventoryServiceClient.reserveStock(orderId, request.items());
            paymentServiceClient.charge(orderId, request.totalAmount());
            orderService.markAsCompleted(orderId);
        } catch (InventoryException e) {
            orderService.markAsCancelled(orderId); // Compensating - chưa cần hoàn kho vì bước này fail
            throw e;
        } catch (PaymentException e) {
            inventoryServiceClient.releaseStock(orderId, request.items()); // Compensating - hoàn kho
            orderService.markAsCancelled(orderId);
            throw e;
        }
    }
}
```

| | Choreography | Orchestration |
|---|---|---|
| Cơ chế | Mỗi service tự lắng nghe event, tự quyết định | 1 "nhạc trưởng" trung tâm điều khiển toàn bộ luồng |
| Coupling | Lỏng hơn (service không biết luồng tổng thể) | Chặt hơn với Orchestrator, nhưng RÕ RÀNG luồng nghiệp vụ |
| Độ phức tạp khi luồng dài | Khó theo dõi luồng tổng thể (logic "rải rác" ở nhiều service) | Dễ theo dõi (toàn bộ logic Saga ở 1 nơi) |
| Khi nào dùng | Luồng đơn giản, ít bước | Luồng nghiệp vụ phức tạp, nhiều bước, cần dễ quan sát/debug |

> **Đánh đổi quan trọng của Saga:** Không còn tính "Atomicity" tức thời như ACID Transaction — có 1 khoảng thời gian ngắn hệ thống ở trạng thái **"không nhất quán tạm thời"** (VD: Order đã tạo, nhưng Payment chưa xong) — đây gọi là **Eventual Consistency** (Tính nhất quán cuối cùng), khác với **Strong Consistency** của ACID truyền thống.

---

## 8. Database per Service & Data Consistency

### Vì sao mỗi Service cần DB riêng?

- Đảm bảo **loose coupling** thực sự — Service A không thể "lách" gọi thẳng vào DB của Service B, buộc phải giao tiếp qua API/Event đã định nghĩa (tránh phá vỡ encapsulation ở cấp độ hệ thống)
- Cho phép mỗi service chọn loại DB phù hợp nhất với nhu cầu riêng (VD: Order Service dùng PostgreSQL, Product Search Service dùng Elasticsearch, Session Service dùng Redis — **Polyglot Persistence** đã học ở Module 10)
- Scale DB độc lập theo tải riêng của từng service

### Vấn đề: Query dữ liệu xuyên nhiều Service (JOIN không còn khả thi)

```sql
-- ❌ KHÔNG THỂ làm điều này trong Microservices - Order và User ở 2 DB KHÁC NHAU!
SELECT o.*, u.full_name
FROM orders o
JOIN users u ON o.user_id = u.id;
```

**Giải pháp phổ biến — API Composition (gọi nhiều service rồi gộp kết quả ở tầng gọi):**

```java
public OrderDetailResponse getOrderDetail(Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    UserDto user = userServiceClient.getUser(order.getUserId()); // Gọi sang User Service
    return new OrderDetailResponse(order, user); // Gộp kết quả ở tầng ứng dụng, KHÔNG phải SQL JOIN
}
```

**Giải pháp nâng cao hơn — CQRS (Command Query Responsibility Segregation):** Xây dựng 1 **Read Model** riêng (thường denormalized, tổng hợp sẵn dữ liệu từ nhiều service qua Event, lưu vào 1 DB đọc riêng tối ưu cho truy vấn) — tách biệt hoàn toàn luồng ghi (Command) và luồng đọc (Query). Đây là kỹ thuật nâng cao, thường chỉ cần thiết khi hệ thống đã rất lớn — nên biết khái niệm, không cần thành thạo ngay ở giai đoạn học này.

---

## 9. ⚠️ Các bẫy hay gặp

1. **Áp dụng Microservices quá sớm** khi team nhỏ, domain chưa rõ ràng — tăng độ phức tạp vận hành mà chưa có lợi ích tương xứng.

2. **Chia sai ranh giới Service (Bounded Context sai)** — VD: tách "User" và "Authentication" thành 2 service riêng dù luôn thay đổi cùng nhau → phải gọi qua lại liên tục, tăng độ trễ không cần thiết mà không có lợi ích thực sự.

3. **"Distributed Monolith"** — các service gọi Synchronous chằng chịt lẫn nhau, tạo phụ thuộc chặt như Monolith nhưng chịu thêm độ trễ mạng.

4. **Dùng chung 1 Database cho nhiều Service** ("Shared Database" anti-pattern) — phá vỡ hoàn toàn tính độc lập, biến Microservices thành "Monolith phân tán tệ hơn" (vẫn coupling qua DB, lại thêm độ phức tạp mạng).

5. **Không có Circuit Breaker khi gọi Service khác** — 1 service down có thể kéo sập dây chuyền toàn hệ thống (Cascading Failure).

6. **Không xử lý Eventual Consistency đúng cách** — hiển thị dữ liệu cho user ngay khi Saga CHƯA hoàn tất, gây hiểu lầm (VD: hiển thị "Đặt hàng thành công" trong khi Payment vẫn đang xử lý ở background).

7. **Bỏ qua Compensating Transaction khi thiết kế Saga** — chỉ nghĩ tới "happy path", không xử lý trường hợp 1 bước giữa chừng thất bại.

8. **Không có Distributed Tracing** (VD: Zipkin, Jaeger — công cụ theo dõi 1 request đi qua bao nhiêu service) — khi có lỗi, không biết lỗi xảy ra ở service nào trong chuỗi gọi phức tạp.

9. **Đặt tên Service quá chi tiết (quá "micro")** — chia nhỏ tới mức mỗi service chỉ có 1-2 API endpoint, gây bùng nổ số lượng service cần vận hành, tăng chi phí hạ tầng và độ phức tạp giao tiếp không cần thiết.

10. **Không có API Gateway, để Client gọi trực tiếp từng service** — khó quản lý Authentication/Rate Limiting tập trung, khó thay đổi cấu trúc backend mà không ảnh hưởng Client.

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Monolith vs Microservices | Đánh đổi giữa đơn giản (Monolith) và khả năng scale/deploy độc lập (Microservices) |
| Monolith First | Nên bắt đầu Monolith module hóa tốt, tách Microservices khi thực sự cần |
| Service Discovery | Đăng ký + tra cứu địa chỉ instance động (Eureka, hoặc Kubernetes Service) |
| API Gateway | 1 điểm vào duy nhất — routing, auth tập trung, rate limiting |
| Distributed Monolith | Anti-pattern — gọi Synchronous chằng chịt như Monolith nhưng chịu độ trễ mạng |
| Circuit Breaker | CLOSED → OPEN (fail-fast) → HALF_OPEN (thử lại) — ngăn Cascading Failure |
| Resilience4j | CircuitBreaker, Retry, RateLimiter, Bulkhead, TimeLimiter |
| Saga Pattern | Chuỗi Local Transaction + Compensating Transaction khi 1 bước thất bại |
| Choreography vs Orchestration | Event-driven phân tán vs có "nhạc trưởng" trung tâm |
| Eventual Consistency | Hệ quả tất yếu của Distributed Transaction — không còn Strong Consistency như ACID |
| Database per Service | Mỗi service 1 DB riêng — JOIN thay bằng API Composition/CQRS |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Microservices luôn là lựa chọn kiến trúc tốt hơn Monolith cho mọi dự án, bất kể quy mô team.
2. Circuit Breaker ở trạng thái OPEN sẽ chặn TẤT CẢ request tới service đích ngay lập tức mà không cần chờ timeout.
3. Trong Saga Pattern, Compensating Transaction là giao dịch dùng để "hoàn tác" các bước đã thành công trước đó khi có 1 bước thất bại.
4. "Distributed Monolith" là thuật ngữ mô tả kiến trúc Microservices được thiết kế đúng chuẩn, với giao tiếp Asynchronous hợp lý.
5. Dùng chung 1 Database cho nhiều Microservices là cách tiếp cận được khuyến nghị để đơn giản hóa việc JOIN dữ liệu.
6. Saga Pattern đảm bảo tính "Strong Consistency" giống hệt ACID Transaction truyền thống.
7. API Gateway giúp tập trung xử lý Authentication, tránh phải lặp lại logic này ở từng Microservice riêng lẻ.
8. Orchestration Saga có 1 thành phần trung tâm điều khiển toàn bộ luồng nghiệp vụ, trong khi Choreography Saga để mỗi service tự lắng nghe event và quyết định.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Thiết kế sơ đồ (dạng text/ASCII) hệ thống "Đặt vé xem phim" theo Microservices gồm: `Movie Service`, `Booking Service`, `Payment Service`, `Notification Service` — chỉ rõ Database riêng của mỗi service và luồng giao tiếp Synchronous/Asynchronous phù hợp.

**Bài 2:** Viết `@FeignClient` interface cho `InventoryServiceClient` gọi tới endpoint `POST /inventory/reserve` (kiểm tra và trừ tồn kho), dùng trong `OrderService`.

**Bài 3:** Viết 1 method `processPayment()` áp dụng `@CircuitBreaker` kèm `fallbackMethod` — khi Payment Service down, trả về kết quả "đơn hàng đang chờ xử lý" thay vì để lỗi lan ra ngoài.

**Bài 4:** Thiết kế Saga theo mô hình **Orchestration** cho luồng "Đặt vé xem phim" (Bài 1): Booking Service giữ chỗ ghế → Payment Service thu tiền → nếu thanh toán thất bại, thực hiện Compensating Transaction hủy giữ chỗ ghế. Viết pseudo-code minh họa.

**Bài 5:** Giải thích (bằng ví dụ cụ thể) tình huống hệ thống rơi vào **"Distributed Monolith"** anti-pattern, và đề xuất cách tái cấu trúc lại giao tiếp giữa các service để tránh vấn đề này.

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Đây là đánh đổi (trade-off) — với team nhỏ, domain chưa rõ ràng, Monolith thường là lựa chọn tốt hơn nhờ đơn giản, nhanh phát triển.
2. **Đúng.** Đây chính là ý nghĩa "fail-fast" của trạng thái OPEN — chặn ngay, không tốn thời gian chờ timeout của request thật.
3. **Đúng.** Đây là định nghĩa cốt lõi của Compensating Transaction trong Saga Pattern.
4. **Sai.** Ngược lại — "Distributed Monolith" là ANTI-PATTERN, xảy ra khi các service gọi Synchronous chằng chịt lẫn nhau, tạo phụ thuộc chặt như Monolith nhưng chịu thêm độ trễ/rủi ro mạng.
5. **Sai.** Đây là anti-pattern "Shared Database" — phá vỡ tính độc lập của Microservices, không được khuyến nghị.
6. **Sai.** Saga chỉ đảm bảo "Eventual Consistency" (nhất quán cuối cùng, có độ trễ), KHÔNG phải Strong Consistency tức thời như ACID.
7. **Đúng.** Đây là 1 trong những lợi ích chính của API Gateway — xử lý cross-cutting concern tập trung.
8. **Đúng.** Đây chính là khác biệt cốt lõi giữa 2 mô hình triển khai Saga.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```
┌─────────────┐     Sync (REST)    ┌─────────────┐
│Movie Service │◄───────────────────│Booking Service│
│  (Movie DB)  │  Lấy thông tin phim, │ (Booking DB) │
└─────────────┘  suất chiếu, ghế trống └──────┬──────┘
                                              │ Sync (REST) - cần biết NGAY
                                              │ kết quả thanh toán để xác nhận vé
                                              ▼
                                      ┌─────────────┐
                                      │Payment Service│
                                      │ (Payment DB) │
                                      └──────┬──────┘
                                              │ Async (publish event "PaymentCompleted"/
                                              │        "PaymentFailed")
                                              ▼
                                      ┌─────────────┐
                                      │Notification   │
                                      │  Service      │
                                      │(Notification DB)│
                                      └─────────────┘

Giải thích lựa chọn:
- Booking -> Movie: Synchronous (cần biết ngay ghế còn trống hay không để tiếp tục giữ chỗ)
- Booking -> Payment: Synchronous (cần biết ngay kết quả thanh toán để xác nhận/hủy vé)
- Payment -> Notification: Asynchronous (gửi thông báo không cần chờ ngay lập tức,
  Booking Service không cần phụ thuộc vào việc gửi thông báo có thành công hay không)
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
@FeignClient(name = "inventory-service") // Tự động tích hợp Service Discovery
public interface InventoryServiceClient {

    @PostMapping("/inventory/reserve")
    ReservationResult reserveStock(@RequestBody ReserveStockRequest request);
}

public record ReserveStockRequest(Long productId, int quantity) {}
public record ReservationResult(boolean success, String message) {}

@Service
public class OrderService {

    private final InventoryServiceClient inventoryServiceClient;
    private final OrderRepository orderRepository;

    public Order createOrder(OrderRequest request) {
        ReservationResult result = inventoryServiceClient.reserveStock(
                new ReserveStockRequest(request.productId(), request.quantity()));

        if (!result.success()) {
            throw new InsufficientStockException(result.message());
        }

        Order order = new Order(request.userId(), request.productId(), request.quantity());
        return orderRepository.save(order);
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
@Service
public class OrderPaymentService {

    private final PaymentServiceClient paymentServiceClient;
    private final OrderRepository orderRepository;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService") // Thử lại vài lần TRƯỚC KHI Circuit Breaker "mở" hẳn
    public PaymentResult processPayment(Order order) {
        return paymentServiceClient.charge(order.getId(), order.getTotalAmount());
    }

    // Fallback - PHẢI cùng chữ ký tham số + thêm Throwable ở cuối
    private PaymentResult paymentFallback(Order order, Throwable throwable) {
        log.warn("Payment Service không phản hồi cho order {}: {}", order.getId(), throwable.getMessage());

        order.setStatus(OrderStatus.PAYMENT_PENDING); // Đánh dấu trạng thái chờ xử lý
        orderRepository.save(order);

        // Có thể publish event để retry xử lý payment sau qua Message Queue (đã học Module 18)
        return PaymentResult.pending("Hệ thống thanh toán đang bận, đơn hàng của bạn sẽ được xử lý sớm nhất");
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@Component
public class BookingSagaOrchestrator {

    private final BookingServiceClient bookingServiceClient; // giả định là chính Booking Service
    private final PaymentServiceClient paymentServiceClient;

    public BookingResult executeBookingSaga(BookingRequest request) {
        // Bước 1: Giữ chỗ ghế (Local Transaction 1)
        Long bookingId = bookingServiceClient.reserveSeat(request.showtimeId(), request.seatIds());

        try {
            // Bước 2: Thu tiền (Local Transaction 2)
            PaymentResult paymentResult = paymentServiceClient.charge(bookingId, request.totalAmount());

            if (!paymentResult.success()) {
                // Compensating Transaction: hủy giữ chỗ ghế vì thanh toán thất bại
                bookingServiceClient.releaseSeat(bookingId);
                return BookingResult.failed("Thanh toán thất bại, vé đã được hủy giữ chỗ");
            }

            // Cả 2 bước thành công -> xác nhận vé
            bookingServiceClient.confirmBooking(bookingId);
            return BookingResult.success(bookingId);

        } catch (PaymentServiceException e) {
            // Bất kỳ lỗi nào từ Payment Service (timeout, service down...) đều PHẢI compensate
            bookingServiceClient.releaseSeat(bookingId);
            return BookingResult.failed("Không thể xử lý thanh toán, vé đã được hủy giữ chỗ: " + e.getMessage());
        }
    }
}
```

**Giải thích:** Orchestrator (có thể đặt trong chính Booking Service hoặc 1 service riêng) đóng vai trò "nhạc trưởng" — biết rõ toàn bộ luồng, và chịu trách nhiệm gọi Compensating Transaction (`releaseSeat`) khi bước Payment thất bại, đảm bảo không có ghế nào bị "giữ chỗ vĩnh viễn" mà không thanh toán thành công.

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

**Ví dụ tình huống "Distributed Monolith":**

```
Client gọi Order Service để tạo đơn hàng:

Order Service (Synchronous) -> gọi User Service (kiểm tra user tồn tại)
     │
     ▼ (Synchronous)
User Service -> gọi Loyalty Service (kiểm tra điểm thưởng của user)
     │
     ▼ (Synchronous)
Loyalty Service -> gọi lại Order Service (lấy lịch sử đơn hàng để tính điểm thưởng chính xác)
     │
     ▼ (Synchronous)
Order Service -> gọi Inventory Service (trừ tồn kho)
     │
     ▼ (Synchronous)
Inventory Service -> gọi Notification Service (thông báo tồn kho thấp) -> CHỜ phản hồi
```

**Vấn đề:** Đây là chuỗi gọi Synchronous **5 tầng lồng nhau**, thậm chí có **vòng lặp phụ thuộc** (Order → User → Loyalty → LẠI gọi về Order) — về bản chất, đây chính là hành vi giống hệt gọi hàm trực tiếp trong Monolith, nhưng **mỗi lần gọi giờ đây là 1 network call** (chậm hơn nhiều lần, có thể lỗi do mạng). Nếu Notification Service (ở cuối chuỗi) bị chậm, toàn bộ chuỗi phía trên đều bị "kẹt" chờ — dù bản chất "thông báo tồn kho thấp" hoàn toàn KHÔNG cần thiết phải chờ mới hoàn tất được việc "tạo đơn hàng".

**Đề xuất tái cấu trúc:**

```
Order Service tạo đơn hàng (Local Transaction) -> publish "OrderCreated" event
     │
     ├─► (Async) User Service lắng nghe -> cập nhật lịch sử mua hàng của user
     ├─► (Async) Inventory Service lắng nghe -> trừ tồn kho -> publish "StockReduced"
     │        └─► (Async) Notification Service lắng nghe "StockReduced" nếu tồn kho thấp -> gửi cảnh báo
     └─► (Async) Loyalty Service lắng nghe -> tự tính điểm thưởng dựa trên dữ liệu ĐÃ CÓ SẴN
              (Loyalty Service nên tự lưu 1 bản sao rút gọn lịch sử đơn hàng cần thiết
               qua việc lắng nghe event từ trước, KHÔNG cần gọi ngược lại Order Service)
```

**Nguyên tắc áp dụng:** Chỉ giữ Synchronous cho những bước **THỰC SỰ cần kết quả ngay để quyết định bước tiếp theo** (VD: kiểm tra tồn kho đủ hay không TRƯỚC KHI xác nhận đơn hàng). Các tác vụ "phụ" không ảnh hưởng luồng chính (thông báo, tính điểm thưởng, cập nhật thống kê...) nên chuyển sang Asynchronous qua Event — giúp giảm coupling, tránh Cascading Failure, và loại bỏ hoàn toàn vòng lặp phụ thuộc giữa các service.

</details>

---

*File tiếp theo trong lộ trình: **Module 20 — DevOps cơ bản cho Backend Developer** (Docker: Image/Container/Dockerfile/docker-compose, CI/CD cơ bản với GitHub Actions/GitLab CI, giới thiệu Kubernetes, Linux command line cần thiết).*
