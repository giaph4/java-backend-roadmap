# Module 12 — Spring Framework Core

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Đây là "trái tim" của toàn bộ hệ sinh thái Spring — Spring Boot, Spring MVC, Spring Data, Spring Security... tất cả đều xây dựng trên nền tảng IoC Container và Dependency Injection. Không hiểu bản chất Bean Lifecycle, `ApplicationContext`, và cách Spring AOP hoạt động thì việc dùng `@Autowired`, `@Transactional`, `@Cacheable`... chỉ là "học vẹt annotation" — sẽ rất khó debug khi gặp lỗi `NoSuchBeanDefinitionException`, circular dependency, hay khi annotation "không có tác dụng" (thường do self-invocation với AOP proxy).

---

## Mục lục

1. [IoC (Inversion of Control) là gì?](#1-ioc-inversion-of-control-là-gì)
2. [Dependency Injection — 3 cách inject](#2-dependency-injection--3-cách-inject)
3. [Spring Container: BeanFactory vs ApplicationContext](#3-spring-container-beanfactory-vs-applicationcontext)
4. [Khai báo Bean: @Component và các stereotype, @Bean, @Configuration](#4-khai-báo-bean)
5. [Bean Scope](#5-bean-scope)
6. [Bean Lifecycle chi tiết](#6-bean-lifecycle-chi-tiết)
7. [@Autowired — cơ chế wiring & giải quyết xung đột](#7-autowired--cơ-chế-wiring)
8. [Circular Dependency](#8-circular-dependency)
9. [Spring AOP — Aspect-Oriented Programming](#9-spring-aop)
10. [⚠️ Các bẫy hay gặp](#10-các-bẫy-hay-gặp)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. IoC (Inversion of Control) là gì?

**IoC (Đảo ngược quyền điều khiển)** là nguyên lý thiết kế: thay vì code tự tạo và quản lý các đối tượng phụ thuộc (dependency), quyền này được **giao cho 1 framework/container bên ngoài** quản lý.

### Không có IoC (cách viết truyền thống)

```java
public class OrderService {
    private PaymentGateway paymentGateway;

    public OrderService() {
        // OrderService TỰ quyết định dùng implementation nào -> phụ thuộc chặt (tight coupling)
        this.paymentGateway = new StripePaymentGateway();
    }
}
```

**Vấn đề:** `OrderService` bị gắn chặt (tight coupling) với `StripePaymentGateway`. Muốn đổi sang `PaypalPaymentGateway`, hoặc viết Unit Test với mock object, đều phải sửa code `OrderService`.

### Có IoC (Spring quản lý)

```java
public class OrderService {
    private final PaymentGateway paymentGateway;

    // OrderService KHÔNG tự tạo đối tượng -> nhận từ bên ngoài (Spring Container inject vào)
    public OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
}
```

**Lợi ích:**
- `OrderService` chỉ phụ thuộc vào **interface** `PaymentGateway`, không quan tâm implementation cụ thể là gì
- Dễ dàng thay đổi implementation (Stripe/Paypal/Mock) mà không sửa `OrderService`
- Dễ viết Unit Test — chỉ cần inject 1 mock `PaymentGateway`

> **Đây chính là nguyên lý D trong SOLID (Dependency Inversion Principle)** đã học ở Module 02.3 — Spring là framework hiện thực hóa nguyên lý này ở quy mô toàn ứng dụng.

**IoC là khái niệm rộng hơn DI:** IoC là *nguyên lý* (đảo ngược quyền kiểm soát), còn **Dependency Injection (DI)** là *kỹ thuật cụ thể* để hiện thực IoC (còn có các kỹ thuật khác như Service Locator, Template Method — nhưng DI phổ biến nhất).

---

## 2. Dependency Injection — 3 cách inject

### 2.1. Constructor Injection (⭐ Khuyến nghị mặc định)

```java
@Service
public class OrderService {

    private final PaymentGateway paymentGateway; // final -> immutable, bắt buộc phải inject khi tạo object
    private final OrderRepository orderRepository;

    // Từ Spring 4.3+, nếu class chỉ có DUY NHẤT 1 constructor,
    // KHÔNG cần @Autowired -> Spring tự động inject
    public OrderService(PaymentGateway paymentGateway, OrderRepository orderRepository) {
        this.paymentGateway = paymentGateway;
        this.orderRepository = orderRepository;
    }
}
```

**Ưu điểm:**
- Field có thể khai báo `final` → **immutable**, đảm bảo object luôn ở trạng thái hợp lệ sau khi khởi tạo
- Phát hiện **Circular Dependency** ngay khi khởi động ứng dụng (fail-fast) thay vì lỗi runtime khó debug
- Dễ viết Unit Test thuần Java (`new OrderService(mockGateway, mockRepo)`) mà **không cần Spring Context**
- Thể hiện rõ ràng dependency bắt buộc — nhìn constructor là biết class cần gì

### 2.2. Setter Injection

```java
@Service
public class OrderService {
    private PaymentGateway paymentGateway;

    @Autowired
    public void setPaymentGateway(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
}
```

**Khi dùng:** Cho dependency **tùy chọn** (optional) — có thể không cần set ngay lúc khởi tạo, có thể thay đổi sau này.

### 2.3. Field Injection (⚠️ Không khuyến nghị cho production)

```java
@Service
public class OrderService {
    @Autowired
    private PaymentGateway paymentGateway; // Inject trực tiếp vào field
}
```

**Vì sao phổ biến nhưng KHÔNG khuyến nghị:**
- Không thể khai báo `final` → object có thể ở trạng thái "nửa vời" (dependency = null) nếu ai đó tạo object bằng `new` thay vì qua Spring
- **Không thể viết Unit Test thuần** (JUnit thường) mà không dùng Reflection hoặc Spring Test Context — vì không có constructor để truyền mock vào
- Che giấu số lượng dependency thực sự (class có quá nhiều `@Autowired` field là dấu hiệu vi phạm Single Responsibility Principle nhưng khó nhận ra)
- Circular Dependency có thể "lọt" qua mà không báo lỗi rõ ràng (xem mục 8)

> **Quy tắc thực chiến:** Luôn dùng **Constructor Injection**. Chỉ dùng Field Injection cho code test nhanh, demo, hoặc trong `@Configuration` class không cần thiết.

### So sánh nhanh

| Tiêu chí | Constructor | Setter | Field |
|---|---|---|---|
| Immutable (`final`) | ✅ | ❌ | ❌ |
| Bắt buộc/Optional | Bắt buộc | Tùy chọn | Tùy chọn |
| Phát hiện Circular Dependency sớm | ✅ | ❌ | ❌ |
| Dễ Unit Test thuần Java | ✅ | ⚠️ Vừa phải | ❌ |
| Khuyến nghị | ⭐⭐⭐ | ⭐⭐ (cho optional) | ⭐ (tránh dùng) |

---

## 3. Spring Container: BeanFactory vs ApplicationContext

**Spring Container** là "trái tim" của Spring — chịu trách nhiệm tạo, cấu hình, và quản lý vòng đời của các **Bean** (object được Spring quản lý).

```
┌──────────────────────────────────────────┐
│              BeanFactory                   │  ← Interface gốc, cơ bản nhất
│  getBean(), lazy initialization mặc định   │
└──────────────────────────────────────────┘
                      ▲
                      │ extends
┌──────────────────────────────────────────┐
│           ApplicationContext                │  ← Interface mở rộng, dùng thực tế 99% trường hợp
│  + Event publishing (ApplicationEvent)      │
│  + Internationalization (i18n)              │
│  + AOP integration                          │
│  + Eager initialization mặc định (singleton)│
└──────────────────────────────────────────┘
```

| | **BeanFactory** | **ApplicationContext** |
|---|---|---|
| Khởi tạo Bean | **Lazy** (chỉ tạo khi `getBean()` được gọi) | **Eager** (tạo sẵn tất cả singleton bean lúc khởi động) |
| Tính năng | Cơ bản: chỉ có DI | Đầy đủ: DI + AOP + Event + i18n + Environment abstraction |
| Dùng khi nào | Hầu như không dùng trực tiếp trong thực tế | **Dùng mặc định** trong mọi ứng dụng Spring/Spring Boot |

> **Trong Spring Boot:** `ApplicationContext` cụ thể được dùng là `AnnotationConfigApplicationContext` (dựa trên annotation) hoặc `ServletWebServerApplicationContext` (cho web app). Bạn hiếm khi phải tự tạo `ApplicationContext` thủ công — `@SpringBootApplication` + `SpringApplication.run()` đã lo việc này.

```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MyApp.class, args);
        // context chính là ApplicationContext -> "trái tim" chứa toàn bộ Bean của ứng dụng
    }
}
```

---

## 4. Khai báo Bean

### 4.1. Stereotype Annotations — cách khai báo Bean phổ biến nhất

```java
@Component        // Đánh dấu tổng quát: "đây là 1 Bean, hãy để Spring quản lý"
public class GenericHelper { }

@Service           // Chuyên biệt hóa @Component -> tầng Business Logic
public class OrderService { }

@Repository        // Chuyên biệt hóa @Component -> tầng truy cập dữ liệu (DAO)
                    // Đặc biệt: tự động dịch exception của JDBC/Hibernate
                    // thành Spring's DataAccessException (unchecked, thống nhất)
public class OrderRepository { }

@Controller         // Chuyên biệt hóa @Component -> tầng xử lý HTTP request (trả về view)
public class HomeController { }

@RestController     // = @Controller + @ResponseBody -> trả JSON/XML trực tiếp (dùng cho REST API)
public class OrderApiController { }
```

**Về bản chất, cả 4 annotation `@Service`/`@Repository`/`@Controller`/`@RestController` đều là `@Component`** — chỉ khác nhau về **ý nghĩa ngữ nghĩa** (semantic) giúp code dễ đọc và một số hành vi đặc biệt (như dịch exception ở `@Repository`). Về mặt kỹ thuật DI, chúng hoạt động giống hệt nhau.

```java
// Bên trong Spring source code (đơn giản hóa):
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component  // <-- @Service chính là 1 dạng @Component "gắn nhãn" thêm
public @interface Service {
}
```

### 4.2. Component Scanning — Spring tìm Bean ở đâu?

```java
@SpringBootApplication // Đã bao gồm @ComponentScan, mặc định quét package hiện tại + package con
public class MyApp { }

// Tương đương với:
@Configuration
@ComponentScan(basePackages = "com.example.myapp")
public class MyApp { }
```

⚠️ **Bẫy hay gặp:** Nếu class chính (`@SpringBootApplication`) nằm ở package `com.example.myapp`, nhưng bạn có 1 `@Service` nằm ở package `com.other.thing` (ngoài phạm vi quét) → Spring sẽ **không tìm thấy Bean đó**, dẫn tới `NoSuchBeanDefinitionException` khi có class khác cố `@Autowired` nó.

### 4.3. @Bean + @Configuration — khai báo Bean thủ công

Dùng khi cần config đối tượng từ **thư viện bên thứ 3** (không sửa được source code để thêm `@Component`), hoặc khi cần logic khởi tạo phức tạp:

```java
@Configuration     // Đánh dấu class này chứa các định nghĩa Bean
public class AppConfig {

    @Bean          // Method này trả về 1 object -> Spring lưu vào Container như 1 Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(List.of(new LoggingInterceptor()));
        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Config thêm cho thư viện Jackson
        return mapper;
    }
}
```

| | `@Component` | `@Bean` |
|---|---|---|
| Áp dụng cho | Class do bạn viết (tự thêm annotation vào source) | Bất kỳ object nào, kể cả class từ thư viện ngoài |
| Cách khai báo | Annotation trên class | Method trong `@Configuration` class, method trả về object |
| Kiểm soát khởi tạo | Spring tự gọi constructor | Bạn viết logic khởi tạo tùy ý trong method |

---

## 5. Bean Scope

Quy định **Spring tạo bao nhiêu instance** và **vòng đời** của Bean đó.

```java
@Service
@Scope("singleton") // Mặc định, có thể bỏ qua annotation này
public class OrderService { }

@Component
@Scope("prototype")
public class ShoppingCart { }
```

| Scope | Ý nghĩa | Ví dụ dùng |
|---|---|---|
| `singleton` (**mặc định**) | Chỉ **1 instance duy nhất** cho toàn bộ ứng dụng, được tạo sẵn khi khởi động (eager) | Service, Repository — hầu hết Bean stateless |
| `prototype` | Tạo **instance mới** mỗi lần `getBean()`/inject | Object có state riêng cho từng lần dùng (VD: giỏ hàng tạm) |
| `request` | 1 instance cho mỗi HTTP request (chỉ dùng trong web app) | Dữ liệu tạm gắn với 1 request cụ thể |
| `session` | 1 instance cho mỗi HTTP session | Dữ liệu người dùng theo phiên đăng nhập |
| `application` | 1 instance cho toàn bộ `ServletContext` | Tương tự singleton nhưng ở cấp Servlet |

⚠️ **Bẫy kinh điển: Inject Prototype Bean vào Singleton Bean**

```java
@Service // Singleton - chỉ tạo 1 lần
public class OrderService {

    @Autowired
    private ShoppingCart cart; // ShoppingCart là prototype

    // ❌ VẤN ĐỀ: cart chỉ được inject 1 LẦN DUY NHẤT lúc OrderService khởi tạo
    // (vì OrderService là singleton, chỉ tạo 1 lần)
    // -> Mọi request sau đó đều dùng CHUNG 1 instance ShoppingCart
    //    dù khai báo là "prototype" (đáng lẽ phải mới mỗi lần)
}
```

**Giải pháp — dùng `ObjectFactory`/`Provider` hoặc `@Lookup`:**

```java
@Service
public class OrderService {

    @Autowired
    private ObjectFactory<ShoppingCart> cartFactory;

    public void processOrder() {
        ShoppingCart cart = cartFactory.getObject(); // Tạo instance MỚI mỗi lần gọi
        // ...
    }
}
```

---

## 6. Bean Lifecycle chi tiết

Đây là kiến thức giúp bạn hiểu rõ **khi nào** logic khởi tạo/dọn dẹp của Bean chạy — cực kỳ hữu ích khi debug các vấn đề liên quan tới khởi tạo dữ liệu, mở/đóng kết nối.

```
1. Instantiation           -> Spring gọi constructor tạo object
2. Populate Properties      -> Spring inject các dependency (@Autowired)
3. BeanNameAware            -> setBeanName() (nếu Bean implement interface này)
4. BeanFactoryAware          -> setBeanFactory()
5. ApplicationContextAware   -> setApplicationContext()
6. @PostConstruct            -> Method đánh dấu chạy NGAY SAU KHI dependency injection xong
7. InitializingBean          -> afterPropertiesSet() (nếu implement interface)
8. Custom init-method         -> Method chỉ định trong @Bean(initMethod = "...")
   ─────────── Bean đã sẵn sàng sử dụng (Ready) ───────────
9. @PreDestroy                -> Method chạy TRƯỚC KHI container đóng (graceful shutdown)
10. DisposableBean             -> destroy() (nếu implement interface)
11. Custom destroy-method       -> Method chỉ định trong @Bean(destroyMethod = "...")
```

### Ví dụ thực tế dùng @PostConstruct / @PreDestroy

```java
@Service
public class CacheWarmupService {

    private final ProductRepository productRepository;
    private Map<Long, Product> cache;

    public CacheWarmupService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        // ⚠️ KHÔNG nên gọi productRepository ở đây -
        // vì tại thời điểm constructor chạy, dependency injection CÓ THỂ chưa hoàn tất
        // với các Bean phức tạp (dù thường Spring đảm bảo constructor injection xong trước khi trả về object)
    }

    @PostConstruct // Đảm bảo chạy SAU KHI mọi dependency đã inject xong hoàn toàn
    public void warmupCache() {
        this.cache = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        System.out.println("Cache đã warm-up với " + cache.size() + " sản phẩm");
    }

    @PreDestroy // Chạy khi ứng dụng shutdown -> dọn dẹp tài nguyên (đóng connection, flush log...)
    public void cleanup() {
        cache.clear();
        System.out.println("Đã dọn dẹp cache trước khi tắt ứng dụng");
    }
}
```

### BeanPostProcessor — can thiệp vào MỌI Bean trong Container

```java
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Trước khi init Bean: " + beanName);
        return bean; // Có thể trả về 1 Proxy object khác để "bọc" bean gốc
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("Sau khi init Bean: " + beanName);
        return bean;
    }
}
```

> **Đây chính là cơ chế nền tảng mà Spring AOP, `@Transactional`, `@Async`, `@Cacheable`... dùng để "bọc" Bean gốc bằng Proxy** — sẽ giải thích rõ ở mục 9.

---

## 7. @Autowired — cơ chế wiring

### Wiring theo Type trước, sau đó theo Name

```java
public interface PaymentGateway { }

@Component
public class StripeGateway implements PaymentGateway { }

@Component
public class PaypalGateway implements PaymentGateway { }

@Service
public class OrderService {
    @Autowired
    private PaymentGateway paymentGateway;
    // ❌ NoUniqueBeanDefinitionException: Spring tìm thấy 2 Bean cùng type PaymentGateway
    //    (StripeGateway và PaypalGateway) -> không biết chọn cái nào!
}
```

### Giải quyết xung đột — 3 cách

**Cách 1 — `@Primary`:** đánh dấu 1 Bean là "ưu tiên mặc định"

```java
@Component
@Primary
public class StripeGateway implements PaymentGateway { }
```

**Cách 2 — `@Qualifier`:** chỉ định rõ tên Bean cần inject

```java
@Component("stripeGateway")
public class StripeGateway implements PaymentGateway { }

@Component("paypalGateway")
public class PaypalGateway implements PaymentGateway { }

@Service
public class OrderService {
    @Autowired
    @Qualifier("stripeGateway")
    private PaymentGateway paymentGateway;
}
```

**Cách 3 — Inject tất cả (List/Map) — dùng cho Strategy Pattern:**

```java
@Service
public class PaymentService {

    private final Map<String, PaymentGateway> gateways;

    // Spring tự động inject TẤT CẢ Bean implement PaymentGateway vào Map,
    // key = tên Bean, value = instance
    public PaymentService(Map<String, PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public void pay(String method, BigDecimal amount) {
        PaymentGateway gateway = gateways.get(method + "Gateway");
        gateway.process(amount);
    }
}
```

> **Liên hệ Design Pattern (Module 08):** Cách 3 chính là cách Spring hỗ trợ **Strategy Pattern** rất tự nhiên — không cần `if/else`/`switch` chọn implementation thủ công.

### required = false — dependency tùy chọn

```java
@Autowired(required = false)
private OptionalFeatureService optionalService; // Không lỗi nếu không tìm thấy Bean, sẽ = null

// Cách hiện đại hơn — dùng Optional:
@Autowired
private Optional<OptionalFeatureService> optionalService;
```

---

## 8. Circular Dependency

Xảy ra khi 2 (hoặc nhiều) Bean phụ thuộc lẫn nhau tạo thành vòng lặp.

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) { this.serviceB = serviceB; }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    public ServiceB(ServiceA serviceA) { this.serviceA = serviceA; } // Vòng lặp!
}
```

Với **Constructor Injection**, Spring phát hiện ngay khi khởi động:

```
❌ BeanCurrentlyInCreationException:
Error creating bean with name 'serviceA':
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

Với **Field Injection**, Spring **có thể** giải quyết được nhờ cơ chế 3-level cache (tạo object rỗng trước, inject dependency sau) — nhưng đây là dấu hiệu **thiết kế sai**, không phải giải pháp nên dựa vào:

```java
@Service
public class ServiceA {
    @Autowired
    private ServiceB serviceB; // Có thể chạy được (Field Injection cho phép "lazy" hơn)
}
```

⚠️ **Vì sao đây là "dấu hiệu thiết kế sai" chứ không phải bug cần "sửa" bằng cách đổi sang Field Injection:** Circular Dependency thường có nghĩa là 2 class đang vi phạm **Single Responsibility Principle** — nên tách logic dùng chung ra 1 class thứ 3, hoặc dùng `@Lazy` để trì hoãn khởi tạo (chỉ là giải pháp tạm, không giải quyết gốc rễ thiết kế):

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(@Lazy ServiceB serviceB) { // Trì hoãn -> chỉ tạo proxy, chưa init thật
        this.serviceB = serviceB;
    }
}
```

**Giải pháp đúng đắn — Refactor:**

```java
// Tách phần logic dùng chung ra class thứ 3
@Service
public class SharedLogicService {
    // Logic mà cả A và B đều cần
}

@Service
public class ServiceA {
    private final SharedLogicService sharedLogic;
    public ServiceA(SharedLogicService sharedLogic) { this.sharedLogic = sharedLogic; }
}

@Service
public class ServiceB {
    private final SharedLogicService sharedLogic;
    public ServiceB(SharedLogicService sharedLogic) { this.sharedLogic = sharedLogic; }
}
```

---

## 9. Spring AOP

**AOP (Aspect-Oriented Programming)** cho phép tách các **cross-cutting concerns** (logic lặp lại ở nhiều nơi, không thuộc business logic chính — logging, transaction, security, caching) ra khỏi code nghiệp vụ.

### Vấn đề khi không có AOP

```java
public void placeOrder(Order order) {
    long start = System.currentTimeMillis(); // Logging - lặp lại ở MỌI method
    log.info("Bắt đầu placeOrder");
    try {
        // ... business logic thực sự
        orderRepository.save(order);
    } finally {
        log.info("Kết thúc placeOrder, mất " + (System.currentTimeMillis() - start) + "ms");
    }
}
```

Nếu có 50 method cần logging tương tự → code logging lặp lại 50 lần, khó bảo trì.

### Với Spring AOP

```java
@Aspect
@Component
public class LoggingAspect {

    // Pointcut: định nghĩa "áp dụng aspect này cho method nào"
    @Around("execution(* com.example.service.*.*(..))") // Mọi method trong package service
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // Gọi method GỐC thực sự
        long duration = System.currentTimeMillis() - start;
        System.out.println(joinPoint.getSignature() + " mất " + duration + "ms");
        return result;
    }
}

// OrderService giờ CHỈ cần viết business logic thuần túy, không có code logging nào cả!
@Service
public class OrderService {
    public void placeOrder(Order order) {
        orderRepository.save(order); // Sạch sẽ, logic thuần
    }
}
```

### Các loại Advice (thời điểm chèn logic)

| Advice | Thời điểm chạy |
|---|---|
| `@Before` | Trước khi method gốc chạy |
| `@After` | Sau khi method gốc chạy (dù có exception hay không — giống `finally`) |
| `@AfterReturning` | Sau khi method gốc chạy **thành công** (không có exception) |
| `@AfterThrowing` | Sau khi method gốc **ném exception** |
| `@Around` | Bao trọn method gốc — mạnh nhất, có thể sửa cả input/output/không cho method chạy |

### Cơ chế Proxy đằng sau AOP — GIẢI THÍCH self-invocation

Spring AOP hoạt động bằng cách tạo **Proxy object** bọc quanh Bean gốc:

```
Client code
     │
     ▼
┌─────────────┐
│   PROXY      │  ← Đây là object THỰC SỰ được inject vào nơi khác (@Autowired)
│ (AOP logic)  │
└─────────────┘
     │ gọi qua proxy.method()
     ▼
┌─────────────┐
│ Real Bean    │  ← Object gốc chứa business logic thật
│ (OrderService)│
└─────────────┘
```

Có 2 loại Proxy Spring dùng:

| | JDK Dynamic Proxy | CGLIB Proxy |
|---|---|---|
| Điều kiện | Bean implement ít nhất 1 interface | Bean KHÔNG implement interface (hoặc ép dùng) |
| Cơ chế | Tạo class implement cùng interface | Tạo class con **kế thừa** Bean gốc |
| Giới hạn | — | Không proxy được method `final`/`private`/`static` |

⚠️ **Đây chính là lý do giải thích bẫy self-invocation đã nói ở Module 11 (`@Transactional`):**

```java
@Service
public class OrderService {
    public void placeOrder(Order order) {
        this.saveOrder(order); // "this" = Real Bean, KHÔNG PHẢI Proxy -> bỏ qua AOP hoàn toàn!
    }

    @Transactional // Cũng là 1 dạng AOP (Around Advice) do Spring cung cấp sẵn
    public void saveOrder(Order order) { ... }
}
```

Vì `@Autowired`/Spring Container chỉ inject **Proxy** vào các nơi khác — khi `OrderService` tự gọi method nội bộ qua `this`, nó đang gọi trực tiếp Real Bean, hoàn toàn bỏ qua lớp Proxy chứa logic `@Transactional`/AOP.

> **Liên hệ:** `@Transactional`, `@Cacheable`, `@Async`, `@Retryable` — tất cả annotation "thần kỳ" này của Spring **đều được triển khai bằng Spring AOP** (dùng chung cơ chế Proxy). Hiểu AOP = hiểu vì sao các annotation này có giới hạn giống nhau (self-invocation, không áp dụng được cho method `private`/`final`).

---

## 10. ⚠️ Các bẫy hay gặp

1. **Field Injection** khiến class khó test, không immutable — nên chuyển sang Constructor Injection.

2. **Inject Prototype Bean vào Singleton Bean** mà không dùng `ObjectFactory`/`@Lookup` → chỉ tạo 1 instance duy nhất dù khai báo prototype.

3. **Circular Dependency** — dấu hiệu vi phạm Single Responsibility Principle, nên refactor thay vì "vá" bằng `@Lazy`.

4. **Self-invocation với AOP** (`this.method()`) → `@Transactional`/`@Cacheable`/`@Async` bị bỏ qua hoàn toàn, không có cảnh báo lỗi rõ ràng.

5. **Bean không nằm trong phạm vi `@ComponentScan`** → `NoSuchBeanDefinitionException` lúc runtime, khó nhận ra ngay vì lỗi compile vẫn pass bình thường.

6. **Nhiều Bean cùng Type mà không dùng `@Primary`/`@Qualifier`** → `NoUniqueBeanDefinitionException`.

7. **Gọi logic phụ thuộc dependency trong Constructor** thay vì `@PostConstruct` — với constructor injection thường an toàn, nhưng dễ gây lỗi khó lường khi logic phức tạp hơn (VD: dependency đó lại phụ thuộc Bean khác chưa init xong).

8. **CGLIB Proxy không proxy được method `private`/`final`** — đặt `@Transactional`/`@Cacheable` trên method `private` sẽ bị bỏ qua hoàn toàn mà Spring không báo lỗi.

9. **Lạm dụng `@Autowired` field injection quá nhiều trong 1 class** — dấu hiệu class đang vi phạm Single Responsibility Principle (quá nhiều dependency = quá nhiều trách nhiệm), Constructor Injection giúp "lộ" vấn đề này rõ ràng hơn (constructor quá dài = cảnh báo trực quan).

10. **Nhầm lẫn `@Component` và `@Bean` khi nào dùng cái nào** — `@Bean` dùng cho object bên thứ 3 không sửa được source code; `@Component` cho class tự viết.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| IoC | Nguyên lý: giao quyền tạo/quản lý object cho Container |
| DI | Kỹ thuật hiện thực IoC — Constructor > Setter > Field (ưu tiên) |
| ApplicationContext | Superset của BeanFactory — dùng mặc định trong Spring Boot |
| @Component/@Service/@Repository/@Controller | Bản chất giống nhau, khác ý nghĩa ngữ nghĩa + hành vi đặc biệt (@Repository dịch exception) |
| @Bean | Dùng trong `@Configuration` để khai báo Bean thủ công (thư viện ngoài) |
| Singleton scope | Mặc định — 1 instance/toàn app, tạo eager |
| Prototype scope | Instance mới mỗi lần inject — cẩn thận khi inject vào Singleton |
| Bean Lifecycle | Constructor → DI → @PostConstruct → Ready → @PreDestroy |
| @Autowired xung đột | Dùng `@Primary` (ưu tiên mặc định) hoặc `@Qualifier` (chỉ định rõ) |
| Circular Dependency | Constructor Injection phát hiện ngay lúc khởi động — nên refactor, không nên "vá" |
| Spring AOP | Tách cross-cutting concerns (logging, transaction...) bằng Proxy |
| Self-invocation | `this.method()` bỏ qua Proxy → AOP annotation mất tác dụng |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Field Injection giúp class dễ viết Unit Test hơn Constructor Injection.
2. `ApplicationContext` khởi tạo Bean singleton ngay khi ứng dụng start (eager), trong khi `BeanFactory` mặc định lazy.
3. `@Service` và `@Component` khác nhau hoàn toàn về mặt kỹ thuật DI.
4. Nếu 1 class chỉ có duy nhất 1 constructor, Spring 4.3+ tự động inject mà không cần `@Autowired`.
5. Circular Dependency luôn gây lỗi ứng dụng không khởi động được, bất kể dùng cách inject nào.
6. `@PostConstruct` đảm bảo chạy sau khi mọi dependency injection đã hoàn tất.
7. Inject 1 Bean có scope `prototype` vào 1 Bean có scope `singleton` (không dùng `ObjectFactory`) sẽ tạo instance mới mỗi lần dùng.
8. `@Transactional` đặt trên method `private` sẽ hoạt động bình thường vì CGLIB có thể proxy mọi loại method.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Viết 1 interface `NotificationService` với 2 implementation `EmailNotificationService` và `SmsNotificationService`. Dùng `@Qualifier` để 1 class `OrderService` chỉ dùng `EmailNotificationService`.

**Bài 2:** Viết 1 `@Aspect` đo thời gian thực thi (giống ví dụ `LoggingAspect` ở mục 9) áp dụng cho toàn bộ package `com.example.repository`, in ra cảnh báo nếu method chạy quá 100ms.

**Bài 3:** Cho 2 class `InventoryService` và `NotificationService` phụ thuộc vòng lặp lẫn nhau (Circular Dependency). Hãy refactor lại thiết kế để loại bỏ vòng lặp này (tách class thứ 3 nếu cần).

**Bài 4:** Viết 1 Bean implement `InitializingBean` và `DisposableBean` để mô phỏng việc mở/đóng kết nối tới 1 external service khi ứng dụng khởi động/tắt. Giải thích khác biệt so với dùng `@PostConstruct`/`@PreDestroy`.

**Bài 5:** Viết 1 `PaymentService` dùng kỹ thuật inject `Map<String, PaymentGateway>` (như ví dụ mục 7, cách 3) để chọn động gateway thanh toán dựa theo tham số truyền vào runtime, không dùng `if/else`.

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Ngược lại — Constructor Injection dễ test hơn nhiều vì có thể `new` object trực tiếp với mock, không cần Reflection hay Spring Test Context.
2. **Đúng.** Đây là khác biệt cốt lõi giữa 2 interface.
3. **Sai.** Về mặt kỹ thuật DI, chúng hoạt động giống hệt nhau — chỉ khác ý nghĩa ngữ nghĩa và 1 số hành vi đặc biệt (VD: `@Repository` dịch exception).
4. **Đúng.** Đây là tính năng convenience của Spring 4.3+ — không cần `@Autowired` nếu chỉ có 1 constructor.
5. **Sai.** Với Field Injection, Spring có thể giải quyết được circular dependency nhờ cơ chế cache 3 cấp (dù không khuyến khích dựa vào điều này). Chỉ Constructor Injection mới chắc chắn fail ngay lúc khởi động.
6. **Đúng.** Đây chính là mục đích thiết kế của `@PostConstruct` — đảm bảo an toàn để dùng mọi dependency đã inject.
7. **Sai.** Chỉ tạo 1 instance duy nhất (lúc Singleton khởi tạo) — phải dùng `ObjectFactory`/`@Lookup` mới tạo mới mỗi lần.
8. **Sai.** CGLIB Proxy tạo class con kế thừa Bean gốc — không thể override được method `private` (Java không cho override private method), nên `@Transactional` trên method `private` hoàn toàn bị bỏ qua.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
public interface NotificationService {
    void send(String message);
}

@Component("emailNotificationService")
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Gửi email: " + message);
    }
}

@Component("smsNotificationService")
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Gửi SMS: " + message);
    }
}

@Service
public class OrderService {
    private final NotificationService notificationService;

    public OrderService(@Qualifier("emailNotificationService") NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void placeOrder() {
        notificationService.send("Đơn hàng đã được đặt thành công");
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
@Aspect
@Component
public class PerformanceMonitorAspect {

    private static final long THRESHOLD_MS = 100;

    @Around("execution(* com.example.repository.*.*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        if (duration > THRESHOLD_MS) {
            System.out.printf("⚠️ CẢNH BÁO: %s mất %dms (vượt ngưỡng %dms)%n",
                    joinPoint.getSignature(), duration, THRESHOLD_MS);
        }
        return result;
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
// ❌ Trước khi refactor - Circular Dependency
@Service
public class InventoryService {
    private final NotificationService notificationService;
    public InventoryService(NotificationService notificationService) { ... }

    public void checkLowStock() {
        // Cần gửi thông báo khi hết hàng
        notificationService.notifyLowStock(...);
    }
}

@Service
public class NotificationService {
    private final InventoryService inventoryService;
    public NotificationService(InventoryService inventoryService) { ... }

    public void notifyLowStock(...) {
        // Cần kiểm tra lại tồn kho trước khi gửi
        inventoryService.checkLowStock(); // Vòng lặp!
    }
}

// ✅ Sau khi refactor - Tách phần logic dùng chung
@Service
public class StockChecker { // Class thứ 3 chứa logic dùng chung
    public boolean isLowStock(Product product) {
        return product.getStock() < product.getMinThreshold();
    }
}

@Service
public class InventoryService {
    private final StockChecker stockChecker;
    private final NotificationService notificationService;

    public InventoryService(StockChecker stockChecker, NotificationService notificationService) {
        this.stockChecker = stockChecker;
        this.notificationService = notificationService;
    }

    public void checkLowStock(Product product) {
        if (stockChecker.isLowStock(product)) {
            notificationService.sendLowStockAlert(product);
        }
    }
}

@Service
public class NotificationService {
    // KHÔNG còn phụ thuộc InventoryService nữa -> vòng lặp đã bị phá vỡ
    public void sendLowStockAlert(Product product) {
        System.out.println("Cảnh báo tồn kho thấp: " + product.getName());
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@Component
public class ExternalServiceConnector implements InitializingBean, DisposableBean {

    private Connection connection;

    @Override
    public void afterPropertiesSet() throws Exception {
        // Tương đương @PostConstruct - chạy sau khi dependency injection hoàn tất
        System.out.println("Đang mở kết nối tới external service...");
        this.connection = openConnection();
    }

    @Override
    public void destroy() throws Exception {
        // Tương đương @PreDestroy - chạy khi ứng dụng shutdown
        System.out.println("Đang đóng kết nối...");
        if (connection != null) connection.close();
    }

    private Connection openConnection() {
        // Logic mở kết nối thực tế
        return new Connection();
    }
}
```

**Giải thích khác biệt:**
- `InitializingBean`/`DisposableBean` là **interface của Spring** → code bị **gắn chặt (coupling) với Spring Framework**, khó tái sử dụng nếu sau này đổi framework.
- `@PostConstruct`/`@PreDestroy` là **annotation chuẩn Java** (`jakarta.annotation`) → không phụ thuộc Spring, class vẫn "sạch", đây là lý do `@PostConstruct`/`@PreDestroy` được khuyến nghị hơn trong thực tế.
- Về thứ tự thực thi: nếu 1 Bean dùng cả 2 cách, `@PostConstruct` chạy **trước** `afterPropertiesSet()`.

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```java
public interface PaymentGateway {
    void process(BigDecimal amount);
}

@Component("stripeGateway")
public class StripeGateway implements PaymentGateway {
    @Override
    public void process(BigDecimal amount) {
        System.out.println("Thanh toán " + amount + " qua Stripe");
    }
}

@Component("paypalGateway")
public class PaypalGateway implements PaymentGateway {
    @Override
    public void process(BigDecimal amount) {
        System.out.println("Thanh toán " + amount + " qua Paypal");
    }
}

@Service
public class PaymentService {

    private final Map<String, PaymentGateway> gateways;

    // Spring tự động inject TẤT CẢ Bean implement PaymentGateway,
    // key = tên Bean đã khai báo trong @Component("...")
    public PaymentService(Map<String, PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public void pay(String method, BigDecimal amount) {
        PaymentGateway gateway = gateways.get(method + "Gateway");
        if (gateway == null) {
            throw new IllegalArgumentException("Không hỗ trợ phương thức thanh toán: " + method);
        }
        gateway.process(amount); // Không cần if/else/switch nào cả!
    }
}

// Sử dụng:
paymentService.pay("stripe", BigDecimal.valueOf(100)); // -> gọi StripeGateway
paymentService.pay("paypal", BigDecimal.valueOf(200));  // -> gọi PaypalGateway
```

**Giải thích:** Cách này tuân thủ **Open/Closed Principle** (SOLID) — muốn thêm gateway mới (VD: MoMo), chỉ cần tạo thêm 1 `@Component` implement `PaymentGateway`, **không cần sửa** `PaymentService`.

</details>

---

*File tiếp theo trong lộ trình: **Module 13 — Spring Boot** (Auto-configuration, Starter dependencies, application.properties/yml, Profiles, Spring Boot Actuator, cấu trúc project chuẩn).*
