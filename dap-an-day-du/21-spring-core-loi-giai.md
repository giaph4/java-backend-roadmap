# Lời giải đầy đủ — Module 13: Spring Core (IoC/DI, AOP)

> Nguồn đề: `21 spring core/21-spring-core.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `@Qualifier` chọn implementation cụ thể

### Đề
Interface `NotificationService`, 2 implementation `EmailNotificationService`/`SmsNotificationService`. `OrderService` chỉ dùng `EmailNotificationService` bằng `@Qualifier`.

### Phân tích

Khi Spring container thấy **NHIỀU HƠN 1 bean cùng implement 1 interface**, việc `@Autowired` thẳng theo kiểu interface sẽ gây lỗi `NoUniqueBeanDefinitionException` — cần chỉ rõ bean nào bằng `@Qualifier` (khớp theo TÊN bean, không phải kiểu).

### Lời giải

```java
public interface NotificationService {
    void send(String to, String message);
}

@Service("emailNotificationService")   // tên bean mặc định nếu không đặt tên: "emailNotificationService" (chữ cái đầu viết thường)
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String to, String message) {
        System.out.println("Gửi Email tới " + to + ": " + message);
    }
}

@Service("smsNotificationService")
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String to, String message) {
        System.out.println("Gửi SMS tới " + to + ": " + message);
    }
}

@Service
public class OrderService {

    private final NotificationService notificationService;

    // @Qualifier chỉ rõ CHÍNH XÁC bean nào - khớp theo TÊN bean, giải quyết NoUniqueBeanDefinitionException
    public OrderService(@Qualifier("emailNotificationService") NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void placeOrder(String customerEmail) {
        // ...logic đặt hàng...
        notificationService.send(customerEmail, "Đơn hàng của bạn đã được xác nhận.");
    }
}
```

### Giải thích

- **`@Qualifier` hoạt động qua CONSTRUCTOR INJECTION** (khuyến nghị) — đặt annotation trực tiếp trên tham số constructor; nếu dùng field injection (`@Autowired` trên field), `@Qualifier` cũng đặt cùng vị trí trên field.
- **Giải pháp thay thế khác (ít rõ ràng hơn):** đổi tên tham số/field trùng khớp với tên bean (`private NotificationService emailNotificationService;`) — Spring có cơ chế fallback "match theo tên field/tham số" khi có nhiều candidate — nhưng cách này **ẩn ý (implicit)**, dễ gãy khi refactor đổi tên biến; `@Qualifier` tường minh, an toàn hơn khi bảo trì lâu dài.
- **`@Primary`** là lựa chọn khác: đánh dấu 1 trong 2 implementation là mặc định khi không có `@Qualifier` — phù hợp khi 1 implementation rõ ràng "chính", còn implementation kia chỉ dùng ở vài nơi đặc biệt (dùng `@Qualifier` riêng ở những nơi đó).

---

## Bài 2 — `@Aspect` đo thời gian thực thi, cảnh báo > 100ms

### Đề
Áp dụng cho toàn bộ package `com.example.repository`, in cảnh báo nếu method chạy quá 100ms.

### Lời giải

```java
@Aspect
@Component
public class RepositoryTimingAspect {

    private static final Logger log = LoggerFactory.getLogger(RepositoryTimingAspect.class);
    private static final long THRESHOLD_MS = 100;

    // Pointcut: TOÀN BỘ method public trong package com.example.repository (và sub-package nhờ ..)
    @Around("execution(public * com.example.repository..*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();   // thực thi method GỐC - bắt buộc gọi, nếu không method gốc sẽ KHÔNG chạy

        long elapsed = System.currentTimeMillis() - start;

        if (elapsed > THRESHOLD_MS) {
            log.warn("[CHẬM] {} mất {}ms (ngưỡng: {}ms)",
                    joinPoint.getSignature().toShortString(), elapsed, THRESHOLD_MS);
        }

        return result;
    }
}
```

### Giải thích

- **`@Around` (không phải `@Before`/`@After`)** là loại advice DUY NHẤT cho phép đo thời gian THỰC THI method — vì cần bắt thời điểm TRƯỚC (`start`) và SAU (`elapsed`) lệnh gọi method gốc, bọc quanh (around) lệnh gọi đó; `@Before`/`@After` chỉ chèn code ở 1 trong 2 thời điểm, không "bao quanh" được.
- **`joinPoint.proceed()` BẮT BUỘC phải gọi** — đây là lệnh THỰC SỰ ủy quyền cho method gốc (`repository.findById(...)` chẳng hạn) chạy; quên gọi `proceed()` sẽ khiến method gốc **KHÔNG BAO GIỜ CHẠY**, một lỗi rất dễ mắc và khó phát hiện khi mới học AOP.
- **`execution(public * com.example.repository..*.*(..))`** — cú pháp AspectJ pointcut: `public *` (mọi kiểu trả về, chỉ method public), `com.example.repository..` (package này VÀ mọi sub-package, do dấu `..`), `*` (mọi tên class), `.*(..)` (mọi method, mọi tham số).
- **Lưu ý về giới hạn của AOP dựa trên Proxy (mặc định của Spring):** chỉ hoạt động khi method được gọi **TỪ BÊN NGOÀI object** (qua Spring bean proxy) — nếu 1 method trong `com.example.repository` gọi 1 method KHÁC cùng class bằng `this.otherMethod()` (self-invocation), Aspect **SẼ KHÔNG kích hoạt** cho lệnh gọi nội bộ đó, vì proxy bị bỏ qua hoàn toàn trong trường hợp này.

---

## Bài 3 — Loại bỏ Circular Dependency

### Đề
`InventoryService` và `NotificationService` phụ thuộc vòng lặp lẫn nhau — refactor loại bỏ (tách class thứ 3 nếu cần).

### Phân tích

Circular Dependency thường là **dấu hiệu thiết kế sai** (vi phạm SRP — 1 trong 2 class đang ôm quá nhiều trách nhiệm, hoặc logic phối hợp giữa 2 service bị đặt sai chỗ), không chỉ là "lỗi kỹ thuật cần fix bằng `@Lazy`".

### Lời giải — trước (lỗi vòng lặp)

```java
@Service
public class InventoryService {
    private final NotificationService notificationService;   // phụ thuộc NotificationService

    public InventoryService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void reduceStock(Long productId, int qty) {
        // ...trừ tồn kho...
        if (/* tồn kho thấp */ true) {
            notificationService.notifyLowStock(productId);   // gọi ngược sang NotificationService
        }
    }
}

@Service
public class NotificationService {
    private final InventoryService inventoryService;   // phụ thuộc NGƯỢC LẠI InventoryService -> VÒNG LẶP!

    public NotificationService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void notifyLowStock(Long productId) {
        int currentStock = inventoryService.getStock(productId);   // gọi ngược lại
        System.out.println("Cảnh báo tồn kho thấp: " + productId + ", còn " + currentStock);
    }
}
```

### Lời giải — sau (refactor: tách class thứ 3 điều phối)

```java
// InventoryService KHÔNG còn phụ thuộc NotificationService nữa - chỉ lo đúng 1 việc: quản lý tồn kho
@Service
public class InventoryService {

    public boolean reduceStock(Long productId, int qty) {
        // ...trừ tồn kho...
        boolean isLowStock = /* kiểm tra tồn kho sau khi trừ */ true;
        return isLowStock;   // TRẢ VỀ kết quả, không tự gọi sang service khác
    }

    public int getStock(Long productId) { /* ... */ return 0; }
}

// NotificationService cũng KHÔNG phụ thuộc InventoryService - chỉ lo đúng 1 việc: gửi thông báo
@Service
public class NotificationService {

    public void notifyLowStock(Long productId, int currentStock) {
        System.out.println("Cảnh báo tồn kho thấp: " + productId + ", còn " + currentStock);
    }
}

// Class thứ 3 - ĐIỀU PHỐI (orchestrator) - phụ thuộc CẢ HAI, nhưng CẢ HAI không phụ thuộc lẫn nhau -> hết vòng lặp
@Service
public class InventoryManagementFacade {

    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public InventoryManagementFacade(InventoryService inventoryService, NotificationService notificationService) {
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    public void reduceStockAndNotifyIfNeeded(Long productId, int qty) {
        boolean isLowStock = inventoryService.reduceStock(productId, qty);
        if (isLowStock) {
            notificationService.notifyLowStock(productId, inventoryService.getStock(productId));
        }
    }
}
```

### Giải thích

- **Bản chất của fix: đảo NGƯỢC hướng phụ thuộc từ "2 chiều" thành "hình cây" (tree, không có chu trình)** — `InventoryManagementFacade` (class thứ 3, đóng vai trò **Facade/Orchestrator**) là nơi DUY NHẤT biết cả 2 service, còn bản thân `InventoryService` và `NotificationService` **không biết gì về nhau** — đây chính là áp dụng **Dependency Inversion Principle (DIP)** đã học ở Module 06: thay vì 2 module cấp thấp phụ thuộc trực tiếp lẫn nhau, đặt logic điều phối vào 1 lớp riêng ở "cấp cao hơn" phụ thuộc vào CẢ HAI qua abstraction.
- **Giải pháp "nhanh" nhưng KHÔNG khuyến khích:** dùng `@Lazy` trên 1 trong 2 constructor injection (Spring sẽ tạo proxy trì hoãn khởi tạo thật tới lần gọi đầu tiên, phá vỡ vòng lặp lúc khởi động) — cách này chỉ "che giấu triệu chứng", vấn đề thiết kế (2 service biết quá nhiều về nhau) VẪN CÒN NGUYÊN, khiến code khó hiểu và khó test hơn (phải mock cả 2 chiều khi viết Unit Test cho từng service).

---

## Bài 4 — `InitializingBean`/`DisposableBean` mô phỏng mở/đóng kết nối

### Đề
Bean implement `InitializingBean` và `DisposableBean` mô phỏng mở/đóng kết nối external service. Khác biệt so với `@PostConstruct`/`@PreDestroy`.

### Lời giải

```java
@Component
public class ExternalServiceConnection implements InitializingBean, DisposableBean {

    private Connection connection;   // giả lập, thực tế có thể là Socket, HTTP client...

    @Override
    public void afterPropertiesSet() throws Exception {
        // Được gọi SAU KHI Spring đã inject xong TOÀN BỘ dependency của bean này,
        // TRƯỚC KHI bean sẵn sàng phục vụ request
        System.out.println("Đang mở kết nối tới external service...");
        this.connection = openConnection();
        System.out.println("Kết nối đã mở thành công.");
    }

    @Override
    public void destroy() throws Exception {
        // Được gọi khi Spring ApplicationContext ĐÓNG (ứng dụng tắt, hoặc context.close())
        System.out.println("Đang đóng kết nối tới external service...");
        if (connection != null) {
            connection.close();
        }
        System.out.println("Đã đóng kết nối.");
    }

    private Connection openConnection() {
        // ...logic mở kết nối thật...
        return new Connection();
    }

    static class Connection {
        void close() { /* ... */ }
    }
}
```

### Giải thích — khác biệt so với `@PostConstruct`/`@PreDestroy`

| Tiêu chí | `InitializingBean`/`DisposableBean` | `@PostConstruct`/`@PreDestroy` |
|---|---|---|
| Nguồn gốc | Interface CỦA SPRING FRAMEWORK (Spring-specific) | Annotation chuẩn `jakarta.annotation` (trước đây `javax.annotation`) — chuẩn CHUNG của Java EE/Jakarta EE, KHÔNG ràng buộc riêng Spring |
| Coupling với framework | **CAO** — class implement trực tiếp interface của Spring, khó tái sử dụng ngoài môi trường Spring | **THẤP** — chỉ là annotation chuẩn Java, class vẫn dùng được (dù annotation vô hiệu) ngoài Spring, hoặc chuyển sang framework DI khác dễ hơn |
| Cách khai báo | Bắt buộc override đúng tên method (`afterPropertiesSet()`, `destroy()`) theo interface | Tùy ý đặt tên method, chỉ cần gắn đúng annotation |
| Khuyến nghị thực tế | Ít dùng hơn trong code nghiệp vụ hiện đại — chủ yếu gặp trong chính source code nội bộ của Spring Framework hoặc thư viện tích hợp | **Được khuyến nghị phổ biến hơn** cho code ứng dụng — Spring official docs cũng khuyến nghị ưu tiên `@PostConstruct`/`@PreDestroy` hoặc `@Bean(initMethod=..., destroyMethod=...)` |
| Thứ tự thực thi (nếu 1 bean có CẢ HAI cách) | `@PostConstruct` chạy TRƯỚC `afterPropertiesSet()`; `destroy()` (DisposableBean) chạy TRƯỚC `@PreDestroy`... (thực ra thứ tự đầy đủ còn phức tạp hơn, nhưng điểm mấu chốt: **không nên trộn lẫn cả 2 cách trên cùng 1 bean** — chọn 1 cách nhất quán) | |

- **Kết luận thực tế:** trong dự án ứng dụng thông thường (không phải viết framework/thư viện), nên ưu tiên `@PostConstruct`/`@PreDestroy` — giảm coupling trực tiếp vào API của Spring, code "sạch" hơn, dễ đọc hơn, và là annotation chuẩn được nhiều framework khác (không chỉ Spring) hỗ trợ.

---

## Bài 5 — `PaymentService` inject `Map<String, PaymentGateway>` chọn động

### Đề
Chọn động gateway thanh toán dựa theo tham số runtime, không dùng if/else.

### Lời giải

```java
public interface PaymentGateway {
    void pay(BigDecimal amount);
}

@Component("momo")   // tên bean CHÍNH LÀ key trong Map khi Spring tự động gom nhóm
public class MomoPaymentGateway implements PaymentGateway {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("Thanh toán " + amount + " qua Momo");
    }
}

@Component("vnpay")
public class VnPayPaymentGateway implements PaymentGateway {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("Thanh toán " + amount + " qua VNPay");
    }
}

@Component("creditCard")
public class CreditCardPaymentGateway implements PaymentGateway {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("Thanh toán " + amount + " qua thẻ tín dụng");
    }
}

@Service
public class PaymentService {

    // Spring TỰ ĐỘNG gom TOÀN BỘ bean implement PaymentGateway vào Map này,
    // key = TÊN BEAN (đã khai báo trong @Component("...")), value = instance tương ứng
    private final Map<String, PaymentGateway> gateways;

    public PaymentService(Map<String, PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public void processPayment(String gatewayName, BigDecimal amount) {
        PaymentGateway gateway = gateways.get(gatewayName);
        if (gateway == null) {
            throw new IllegalArgumentException("Không hỗ trợ cổng thanh toán: " + gatewayName);
        }
        gateway.pay(amount);   // KHÔNG có if/else nào - chọn động hoàn toàn qua Map lookup
    }
}
```

### Giải thích

- **Cơ chế Spring tự động gom bean vào `Map<String, T>`:** khi 1 constructor/field khai báo kiểu `Map<String, PaymentGateway>`, Spring **TỰ ĐỘNG** tìm TOÀN BỘ bean implement `PaymentGateway` trong context, đưa vào Map với **key = tên bean**, **value = instance bean** — không cần cấu hình gì thêm, đây là tính năng "collection injection" tích hợp sẵn của Spring.
- **Tuân thủ Open/Closed Principle (OCP) tuyệt đối:** khi cần thêm 1 gateway MỚI (VD `ZaloPayGateway`), chỉ cần tạo class mới implement `PaymentGateway`, gắn `@Component("zalopay")` — **KHÔNG cần sửa 1 dòng nào** trong `PaymentService` — khác hẳn với thiết kế `if (gatewayName.equals("momo")) {...} else if (...)` phải SỬA method `processPayment()` mỗi khi thêm gateway mới, vi phạm OCP.
- Đây chính là biến thể của **Strategy Pattern** (đã gặp ở Module 08 — Design Patterns) kết hợp cơ chế DI của Spring để tự động hóa việc "đăng ký" các strategy — không cần tự viết `Map<String, Supplier<PaymentGateway>>` thủ công như ví dụ thuần Java ở Module 08, Spring làm thay hoàn toàn.

---

## Bài 6 — Event-driven cho nghiệp vụ đặt hàng

### Đề
Refactor `placeOrder()` (gọi trực tiếp `emailService`, `inventoryService`, `loyaltyPointService`) sang publish `OrderPlacedEvent`, 3 listener riêng biệt. `InventoryListener` chỉ chạy khi transaction gốc COMMIT thành công.

### Lời giải — trước (coupling trực tiếp)

```java
@Service
public class OrderService {
    private final EmailService emailService;
    private final InventoryService inventoryService;
    private final LoyaltyPointService loyaltyPointService;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        Order order = createOrder(request);
        emailService.sendConfirmation(order);           // gọi TRỰC TIẾP - coupling chặt
        inventoryService.reduceStock(order);             // gọi TRỰC TIẾP
        loyaltyPointService.addPoints(order);             // gọi TRỰC TIẾP
        return order;
    }
    // ...
}
```

### Lời giải — sau (Event-driven, tách rời hoàn toàn)

```java
public class OrderPlacedEvent {
    private final Order order;

    public OrderPlacedEvent(Order order) { this.order = order; }
    public Order getOrder() { return order; }
}

@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order placeOrder(OrderRequest request) {
        Order order = createOrder(request);
        eventPublisher.publishEvent(new OrderPlacedEvent(order));   // CHỈ 1 dòng - publish 1 event duy nhất
        return order;
        // OrderService KHÔNG CẦN BIẾT có bao nhiêu listener, hay listener nào tồn tại
    }
}

@Component
public class EmailListener {
    @EventListener   // chạy ĐỒNG BỘ, NGAY TRONG cùng transaction/thread với publishEvent() (mặc định)
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("Gửi email xác nhận đơn hàng: " + event.getOrder().getId());
    }
}

@Component
public class LoyaltyPointListener {
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("Cộng điểm thưởng cho đơn hàng: " + event.getOrder().getId());
    }
}

@Component
public class InventoryListener {

    // Chỉ chạy SAU KHI transaction GỐC (nơi publishEvent được gọi) COMMIT THÀNH CÔNG
    // Nếu transaction gốc ROLLBACK -> listener này KHÔNG BAO GIỜ chạy
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("Trừ tồn kho cho đơn hàng: " + event.getOrder().getId());
    }
}
```

### Giải thích — annotation cần dùng

- **`@EventListener`** (dùng cho `EmailListener`, `LoyaltyPointListener`): chạy **ĐỒNG BỘ** ngay khi `publishEvent()` được gọi, **KHÔNG QUAN TÂM** transaction gốc sau đó COMMIT hay ROLLBACK — nếu transaction gốc rollback SAU KHI email đã gửi, email đó **VẪN ĐÃ GỬI** (không thể "thu hồi" 1 email đã gửi) — đây là hành vi CHẤP NHẬN ĐƯỢC cho email/loyalty point trong bài này theo yêu cầu đề bài (chỉ `InventoryListener` cần đặc biệt).
- **`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`** (dùng cho `InventoryListener`): là annotation CHUYÊN BIỆT của Spring, tích hợp CHẶT với vòng đời transaction — listener chỉ thực sự chạy **SAU KHI** transaction bao quanh nơi publish event **COMMIT THÀNH CÔNG**; nếu transaction đó **ROLLBACK** (VD lỗi xảy ra ở bước nào đó sau khi publish event nhưng trước khi `placeOrder()` return), `InventoryListener.onOrderPlaced()` **SẼ KHÔNG BAO GIỜ ĐƯỢC GỌI** — đúng yêu cầu nghiệp vụ: **KHÔNG trừ tồn kho cho 1 đơn hàng cuối cùng không được tạo thành công**.
- **Vì sao `InventoryListener` CẦN đặc biệt còn 2 listener kia thì không (theo yêu cầu đề bài):** trừ tồn kho là thao tác **THAY ĐỔI TRẠNG THÁI NGHIỆP VỤ QUAN TRỌNG, CÓ THỂ GÂY OVERSOLD** nếu chạy nhầm cho đơn hàng thất bại — trong khi gửi email/cộng điểm dù "lỡ chạy" cho đơn hàng thất bại cũng ít nghiêm trọng hơn (dù về mặt lý tưởng, thực tế nên cân nhắc dùng `AFTER_COMMIT` cho CẢ 3 để nhất quán tuyệt đối, nhưng bài tập yêu cầu tập trung minh họa đúng 1 trường hợp).
- **Lợi ích tổng thể của thiết kế Event-driven:** `OrderService` giờ đây **hoàn toàn KHÔNG BIẾT** có bao nhiêu listener đang lắng nghe `OrderPlacedEvent` — tuân thủ triệt để **Open/Closed Principle**: thêm 1 nghiệp vụ MỚI cần xử lý khi đặt hàng (VD gửi SMS, ghi log audit) chỉ cần thêm 1 `@Component` listener MỚI, **không cần sửa** `OrderService.placeOrder()` — đây chính là biến thể của **Observer Pattern** (Module 08) được Spring hỗ trợ tích hợp sẵn qua `ApplicationEventPublisher`.

---

*Đây là lời giải cho toàn bộ Phần B của Module 21. Tiếp theo: Module 14 — Spring Boot nâng cao.*
