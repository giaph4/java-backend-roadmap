# Lời giải đầy đủ — Module 19: Caching & Messaging

> Nguồn đề: `27-caching-messaging/27-caching-messaging.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `@Cacheable`/`@CachePut`/`@CacheEvict` cho `ProductService`

### Đề
`getProduct(id)`, `updateProduct(product)`, `deleteProduct(id)`.

### Lời giải

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        // Cache MISS: method THỰC SỰ chạy, kết quả sau đó được LƯU vào cache tự động
        // Cache HIT: method body KHÔNG BAO GIỜ chạy, trả thẳng giá trị từ cache
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
    }

    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        // LUÔN LUÔN chạy method body (KHÁC @Cacheable) - cập nhật DB THẬT,
        // sau đó GHI ĐÈ kết quả mới vào cache - đảm bảo cache KHÔNG BAO GIỜ chứa dữ liệu cũ
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        // Xóa entry TƯƠNG ỨNG khỏi cache - lần GET tiếp theo sẽ cache miss, tự nhiên báo "không tìm thấy"
        productRepository.deleteById(id);
    }
}
```

```java
@Configuration
@EnableCaching   // BẮT BUỘC để kích hoạt toàn bộ cơ chế @Cacheable/@CachePut/@CacheEvict
public class CacheConfig {
}
```

### Giải thích

- **`@Cacheable` vs `@CachePut` — khác biệt CỐT LÕI:** `@Cacheable` **KIỂM TRA cache TRƯỚC**, chỉ chạy method khi cache miss (dùng cho ĐỌC); `@CachePut` **LUÔN chạy method**, chỉ dùng cache để LƯU LẠI kết quả sau đó (dùng cho GHI, đảm bảo DB và cache LUÔN đồng bộ ngay lập tức) — nhầm lẫn 2 annotation này là lỗi phổ biến: nếu dùng `@Cacheable` cho `updateProduct`, method sẽ **KHÔNG CHẠY** nếu đã có cache (dữ liệu KHÔNG BAO GIỜ được cập nhật thật).
- **`key = "#id"` / `key = "#product.id"`**: cú pháp SpEL (Spring Expression Language) chỉ định rõ khóa cache dựa trên tham số method — quan trọng để đảm bảo `getProduct(1)` và `updateProduct(product có id=1)` dùng **CÙNG 1 KHÓA** trong cache (`products::1`), nếu không key mặc định (dựa trên toàn bộ tham số) có thể không khớp nhau giữa các method.
- **Đây chính là chiến lược "Cache-Aside" đã mô tả bằng pseudo-code ở Module 11 (RDBMS & NoSQL, Bài 3)**, giờ triển khai bằng annotation khai báo (declarative) của Spring thay vì viết tay logic `redis.GET`/`redis.SETEX`/`redis.DEL` — Spring tự động sinh AOP proxy xử lý toàn bộ logic đó phía sau annotation.

---

## Bài 2 — Chống Cache Penetration

### Đề
Cache cả trường hợp sản phẩm KHÔNG tồn tại, TTL ngắn hơn (30 giây so với 10 phút).

### Phân tích

**Cache Penetration** xảy ra khi có RẤT NHIỀU request truy vấn 1 `id` **KHÔNG TỒN TẠI** (VD do tấn công cố ý, hoặc bug ở client) — vì kết quả "không tồn tại" thường **KHÔNG được cache** (`@Cacheable` mặc định không cache giá trị `null`/exception), MỌI request như vậy đều **XUYÊN THẲNG qua cache, đập thẳng vào DB** — với khối lượng lớn, có thể làm QUÁ TẢI DB dù dữ liệu THẬT SỰ không hề tồn tại.

### Lời giải

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL_TON_TAI = Duration.ofMinutes(10);
    private static final Duration TTL_KHONG_TON_TAI = Duration.ofSeconds(30);   // NGẮN HƠN nhiều
    private static final String NULL_MARKER = "__NULL__";   // giá trị đánh dấu "đã kiểm tra, KHÔNG tồn tại"

    public ProductService(ProductRepository productRepository, RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    public Optional<Product> getProduct(Long id) {
        String key = "product:" + id;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            // Cache HIT - có thể là sản phẩm THẬT, hoặc marker "đã biết là không tồn tại"
            if (NULL_MARKER.equals(cached)) {
                return Optional.empty();   // KHÔNG chạm DB - đã "nhớ" là sản phẩm này không có
            }
            return Optional.of((Product) cached);
        }

        // Cache MISS - lần ĐẦU TIÊN hỏi về id này (hoặc đã hết hạn cache trước đó)
        Optional<Product> product = productRepository.findById(id);

        if (product.isPresent()) {
            redisTemplate.opsForValue().set(key, product.get(), TTL_TON_TAI);
        } else {
            // Cache LUÔN cả trường hợp "KHÔNG TỒN TẠI" - với TTL NGẮN HƠN NHIỀU
            redisTemplate.opsForValue().set(key, NULL_MARKER, TTL_KHONG_TON_TAI);
        }

        return product;
    }
}
```

### Giải thích

- **TTL ngắn hơn cho "không tồn tại" (30s vs 10 phút) là đánh đổi có chủ đích:** nếu 1 sản phẩm được TẠO MỚI ngay sau khi đã bị cache "không tồn tại", TTL ngắn đảm bảo cache tự "quên" nhanh chóng (tối đa 30 giây) và phản ánh đúng trạng thái MỚI — nếu dùng TTL dài giống hệt trường hợp tồn tại (10 phút), sản phẩm MỚI TẠO sẽ "biến mất" khỏi API trong tận 10 phút dù đã có trong DB thật, gây trải nghiệm rất tệ.
- **Điểm khác biệt so với chiến lược Cache-Aside cơ bản (Bài 1):** Bài 1 chỉ cache kết quả THÀNH CÔNG; Bài 2 mở rộng cache CẢ THẤT BẠI (not-found) — đây LÀ giải pháp CHUẨN cho Cache Penetration được nhắc tới trong hầu hết tài liệu về thiết kế hệ thống caching quy mô lớn.
- **Giải pháp thay thế khác (không yêu cầu trong bài, nhưng nên biết):** **Bloom Filter** — cấu trúc dữ liệu xác suất, kiểm tra CỰC NHANH "id này CHẮC CHẮN không tồn tại" hay "CÓ THỂ tồn tại" TRƯỚC KHI chạm Redis/DB — phù hợp hơn khi số lượng ID không tồn tại là RẤT LỚN và đa dạng (cache marker riêng cho từng ID tốn nhiều bộ nhớ Redis hơn 1 Bloom Filter duy nhất).

---

## Bài 3 — RabbitMQ Producer/Consumer với Idempotent Consumer

### Đề
Producer publish `OrderCreatedEvent`. Consumer xử lý gửi email — Idempotent qua kiểm tra `messageId`.

### Lời giải — Producer

```java
public record OrderCreatedEvent(String messageId, Long orderId, String customerEmail) {}

@Service
public class OrderService {

    private final RabbitTemplate rabbitTemplate;

    public OrderService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void placeOrder(Order order) {
        // ...logic tạo order...

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),   // messageId DUY NHẤT cho mỗi sự kiện
                order.getId(),
                order.getCustomerEmail()
        );

        rabbitTemplate.convertAndSend("order.exchange", "order.created", event);
    }
}
```

### Lời giải — Consumer (Idempotent)

```java
@Component
public class OrderEmailConsumer {

    private final ProcessedMessageRepository processedMessageRepository;
    private final EmailService emailService;

    public OrderEmailConsumer(ProcessedMessageRepository processedMessageRepository, EmailService emailService) {
        this.processedMessageRepository = processedMessageRepository;
        this.emailService = emailService;
    }

    @RabbitListener(queues = "order.email.queue")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        // BƯỚC 1: Kiểm tra messageId ĐÃ XỬ LÝ TRƯỚC ĐÓ chưa
        if (processedMessageRepository.existsById(event.messageId())) {
            // ĐÃ xử lý rồi - bỏ qua, KHÔNG gửi email lần 2
            // (RabbitMQ có thể redeliver message do consumer crash trước khi ack, network glitch...)
            return;
        }

        // BƯỚC 2: Xử lý nghiệp vụ THẬT
        emailService.sendOrderConfirmation(event.customerEmail(), event.orderId());

        // BƯỚC 3: Đánh dấu messageId ĐÃ XỬ LÝ - lưu trong CÙNG transaction với logic nghiệp vụ (nếu có DB write)
        processedMessageRepository.save(new ProcessedMessage(event.messageId(), LocalDateTime.now()));
    }
}

@Entity
public class ProcessedMessage {
    @Id
    private String messageId;
    private LocalDateTime processedAt;
    // constructor, getter...
}
```

### Giải thích

- **Vì sao cần Idempotent Consumer — bản chất "At-Least-Once Delivery" của message queue:** RabbitMQ (và hầu hết message broker) mặc định đảm bảo message được giao **ÍT NHẤT 1 LẦN** (at-least-once), KHÔNG đảm bảo **ĐÚNG 1 LẦN** (exactly-once) — nếu consumer xử lý XONG (gửi email thành công) nhưng **CRASH TRƯỚC KHI** gửi `ack` về broker, RabbitMQ sẽ **REDELIVER** (gửi lại) message đó cho consumer khác/lần sau — nếu không có cơ chế Idempotent, khách hàng sẽ nhận **EMAIL TRÙNG LẶP**.
- **`messageId` (UUID sinh bởi Producer, KHÔNG PHẢI `orderId`):** dùng `orderId` làm định danh chống trùng có thể SAI nếu nghiệp vụ hợp lệ cho phép NHIỀU sự kiện khác nhau liên quan cùng 1 order (VD "order created", "order updated" đều liên quan `orderId` đó) — `messageId` RIÊNG cho MỖI SỰ KIỆN đảm bảo tính duy nhất chính xác tuyệt đối, không phụ thuộc ngữ nghĩa nghiệp vụ.
- **Kiểm tra + lưu `messageId` trong CÙNG `@Transactional`** với logic nghiệp vụ (nếu nghiệp vụ có ghi DB) đảm bảo tính NHẤT QUÁN — nếu tách rời (lưu `messageId` TRƯỚC khi gửi email thật), và gửi email thất bại giữa chừng, `messageId` đã bị đánh dấu "đã xử lý" dù email chưa thực sự gửi — email đó sẽ **KHÔNG BAO GIỜ được gửi lại** dù message có được redeliver.
- **Lưu ý về `emailService.sendOrderConfirmation` (I/O ra bên ngoài, KHÔNG có transaction bảo vệ):** nếu gửi email THÀNH CÔNG nhưng sau đó lưu `ProcessedMessage` THẤT BẠI (transaction rollback), email vẫn ĐÃ GỬI nhưng hệ thống KHÔNG ghi nhận — message có thể được redeliver và gửi email TRÙNG LẦN NỮA — đây là giới hạn cố hữu khi kết hợp thao tác I/O bên ngoài với transaction DB (tương tự vấn đề "email đã gửi không thể rollback" đã nhắc ở Module 13, Bài 6 — Event-driven).

---

## Bài 4 — Cache Stampede và Double-Checked Locking

### Đề
Cache "danh sách sản phẩm bán chạy nhất trong ngày" — minh họa Cache Stampede và khắc phục bằng Double-Checked Locking.

### Phân tích

**Cache Stampede** (còn gọi "Thundering Herd"): khi 1 cache entry CỰC KỲ PHỔ BIẾN (mọi user đều truy cập) **HẾT HẠN (expire)** đồng thời — HÀNG NGHÌN request gần như cùng lúc đều thấy cache miss, **TẤT CẢ CÙNG LÚC** chạy lại truy vấn DB nặng (tính toán "bán chạy nhất") để build lại cache — gây **QUÁ TẢI ĐỘT NGỘT** cho DB dù đáng lẽ chỉ cần **1 LẦN** tính toán là đủ.

### Lời giải

```java
@Service
public class BestSellerService {

    private static final String CACHE_KEY = "bestsellers:today";
    private final Object lock = new Object();   // lock RIÊNG cho đúng 1 cache entry này

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;

    public BestSellerService(RedisTemplate<String, Object> redisTemplate, ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
    }

    public List<Product> getBestSellersToday() {
        // LẦN KIỂM TRA THỨ NHẤT (không lock) - đa số request (cache còn hạn) trả về NGAY, KHÔNG tốn chi phí lock
        List<Product> cached = getFromCache();
        if (cached != null) {
            return cached;
        }

        // Cache miss - CHỈ 1 THREAD được vào tính toán lại, các thread khác PHẢI CHỜ ở synchronized
        synchronized (lock) {
            // LẦN KIỂM TRA THỨ HAI (bên trong lock) - "double-checked"
            // Nếu 1 thread KHÁC đã vào TRƯỚC và build xong cache RỒI, thread này sẽ THẤY NGAY, KHÔNG build lại
            cached = getFromCache();
            if (cached != null) {
                return cached;
            }

            // CHỈ THREAD ĐẦU TIÊN thực sự chạy tới đây - build lại cache 1 LẦN DUY NHẤT
            List<Product> freshData = productRepository.findTop10BestSellersToday();
            redisTemplate.opsForValue().set(CACHE_KEY, freshData, Duration.ofMinutes(5));
            return freshData;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Product> getFromCache() {
        return (List<Product>) redisTemplate.opsForValue().get(CACHE_KEY);
    }
}
```

### Giải thích

- **Vì sao gọi là "Double-Checked" (kiểm tra 2 lần):** LẦN 1 (ngoài `synchronized`) để **TRÁNH CHI PHÍ LOCK KHÔNG CẦN THIẾT** cho đa số request (cache còn hạn — trường hợp phổ biến nhất, chiếm >99% thời gian) — chỉ khi THỰC SỰ cache miss mới vào `synchronized`; LẦN 2 (bên trong `synchronized`) để xử lý đúng tình huống: giữa lúc Thread A đang CHỜ lấy được lock, có thể Thread B (đã vào TRƯỚC) đã build XONG cache rồi — nếu không kiểm tra lại, Thread A sẽ build lại LẦN NỮA, THỪA THÃI dù mục tiêu ban đầu (giảm số lần build) không đạt được hoàn toàn.
- **Kết quả:** dù có HÀNG NGHÌN request đồng thời gặp cache miss cùng lúc, chỉ **ĐÚNG 1 THREAD DUY NHẤT** thực sự chạy `productRepository.findTop10BestSellersToday()` (câu truy vấn nặng) — các thread còn lại XẾP HÀNG chờ ngắn (thời gian build cache, không phải thời gian query DB LẶP LẠI nhiều lần), sau đó nhận NGAY kết quả đã có sẵn từ lần kiểm tra thứ 2.
- **Liên hệ trực tiếp với `synchronized` đã học ở Module 05.1 (Thread cơ bản):** đây chính là ứng dụng THỰC TẾ của Double-Checked Locking Pattern — mẫu hình quen thuộc trong Java (từng gặp ở Singleton lazy-initialization) — nhưng áp dụng ở NGỮ CẢNH KHÁC (cache rebuild) thay vì khởi tạo object.
- **Giới hạn của giải pháp này:** `synchronized` chỉ hoạt động **TRONG CÙNG 1 JVM** — nếu hệ thống có NHIỀU instance backend (tương tự vấn đề đã nêu ở Module 11, Bài 5), mỗi instance vẫn có thể ĐỘC LẬP build lại cache CỦA RIÊNG NÓ khi cache Redis dùng chung hết hạn — với hệ thống scale ngang thực sự cần **Distributed Lock** (Redis `SET NX EX`, tương tự đã làm ở Module 11) thay vì `synchronized` cục bộ JVM.

---

## Bài 5 — `@Async` cho push notification, giải thích vì sao KHÔNG dùng cho trừ tiền ví

### Đề
`@Async` gửi push notification, `ThreadPoolTaskExecutor` (core=3, max=10, queue=50). Vì sao không dùng `@Async` cho trừ tiền ví.

### Lời giải

```java
@Configuration
@EnableAsync   // BẮT BUỘC để kích hoạt @Async trong toàn ứng dụng
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notif-async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class NotificationService {

    @Async("notificationExecutor")   // chỉ RÕ TÊN executor - tránh nhầm với executor mặc định/executor khác trong hệ thống
    public void sendPushNotification(Long userId, String message) {
        // Chạy trên THREAD RIÊNG (thread pool "notif-async-*"), KHÔNG chặn thread xử lý request HTTP gốc
        System.out.println("[" + Thread.currentThread().getName() + "] Gửi push notification tới user " + userId + ": " + message);
        // ...logic gọi Firebase Cloud Messaging / APNs thật...
    }
}

@Service
public class OrderService {

    private final NotificationService notificationService;

    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void placeOrder(Order order) {
        // ...logic tạo order (đồng bộ, quan trọng)...

        // Gửi thông báo là việc PHỤ - KHÔNG cần chờ kết quả, KHÔNG ảnh hưởng luồng chính nếu chậm/thất bại
        notificationService.sendPushNotification(order.getUserId(), "Đơn hàng của bạn đã được tạo!");
        // method trả về NGAY LẬP TỨC (không chờ push notification gửi xong) - placeOrder() tiếp tục chạy
    }
}
```

### Giải thích — vì sao KHÔNG nên dùng `@Async` cho trừ tiền ví

- **`@Async` về bản chất PHÁ VỠ tính ĐỒNG BỘ VÀ TÍNH GIAO DỊCH (transactional) của luồng gọi:** method `@Async` chạy trên **THREAD KHÁC**, **TÁCH RỜI HOÀN TOÀN** khỏi transaction của thread gọi ban đầu — nếu method gọi (`placeOrder`) đang trong 1 `@Transactional` và SAU ĐÓ bị `ROLLBACK` (VD lỗi ở bước khác), **THAO TÁC `@Async` ĐÃ CHẠY (hoặc ĐANG CHẠY) SẼ KHÔNG BỊ ROLLBACK THEO** — vì nó thuộc transaction RIÊNG (hoặc KHÔNG transaction nào cả) trên thread khác.
- **Áp dụng vào trừ tiền ví — kịch bản THẢM HỌA nếu dùng `@Async`:** giả sử `walletService.deductBalance(userId, amount)` được đánh dấu `@Async` — thread gọi tiếp tục chạy NGAY (không chờ kết quả trừ tiền), nếu bước SAU ĐÓ trong luồng nghiệp vụ chính thất bại và ROLLBACK toàn bộ transaction, **THAO TÁC TRỪ TIỀN (đã chạy trên thread async riêng) VẪN GIỮ NGUYÊN, KHÔNG BỊ HOÀN TÁC** — dẫn tới **MẤT TIỀN THỰC SỰ của user** dù đơn hàng KHÔNG được tạo thành công — hậu quả tài chính nghiêm trọng, không thể chấp nhận.
- **Ngoài ra, `@Async` KHÔNG cho phép caller biết kết quả THÀNH CÔNG hay THẤT BẠI một cách trực tiếp, đồng bộ** (trừ khi trả về `CompletableFuture` và caller chủ động `.get()`/`.join()` — nhưng làm vậy thì lại MẤT ĐI lợi ích "không chặn" của `@Async`) — với thao tác TÀI CHÍNH, việc biết CHẮC CHẮN NGAY LẬP TỨC kết quả (thành công/thất bại) để quyết định bước tiếp theo (VD tạo Order chỉ khi trừ tiền THÀNH CÔNG) là YÊU CẦU BẮT BUỘC — không thể chấp nhận "cứ coi như thành công, xử lý bất đồng bộ, tính sau".
- **Nguyên tắc chọn lựa tổng quát:** `@Async` PHÙ HỢP cho các tác vụ **PHỤ, KHÔNG QUYẾT ĐỊNH KẾT QUẢ NGHIỆP VỤ CHÍNH**, chấp nhận được nếu chậm trễ/thất bại (gửi email, push notification, ghi log audit, đồng bộ dữ liệu qua hệ thống thứ 3 không quan trọng) — **TUYỆT ĐỐI KHÔNG PHÙ HỢP** cho thao tác **CÓ TÍNH TOÀN VẸN GIAO DỊCH CAO** (trừ tiền, trừ tồn kho, tạo đơn hàng) — những thao tác này CẦN chạy ĐỒNG BỘ, trong CÙNG 1 transaction, để đảm bảo tính "tất cả hoặc không gì cả" (all-or-nothing) của ACID.

---

## Bài 6 — Outbox Pattern cho `placeOrder()`

### Đề
Lưu `Order` và `OutboxEvent` trong cùng transaction. `OutboxPoller` dùng `@Scheduled` publish event chưa xử lý. Giải thích Dual-Write Problem.

### Phân tích — Dual-Write Problem

Nếu `placeOrder()` viết trực tiếp: `orderRepository.save(order)` **RỒI SAU ĐÓ** `rabbitTemplate.convertAndSend(...)` — đây là **2 THAO TÁC GHI VÀO 2 HỆ THỐNG KHÁC NHAU** (DB và Message Broker), **KHÔNG THỂ đảm bảo tính NGUYÊN TỬ (atomic) giữa 2 thao tác này** — nếu DB `save()` THÀNH CÔNG nhưng ứng dụng CRASH **NGAY TRƯỚC KHI** gọi `convertAndSend()` (hoặc broker tạm thời không phản hồi), Order đã được lưu nhưng **EVENT KHÔNG BAO GIỜ ĐƯỢC GỬI** — dữ liệu bị BẤT NHẤT giữa 2 hệ thống (DB có Order, nhưng các service khác lắng nghe `OrderCreatedEvent` KHÔNG BAO GIỜ biết).

### Lời giải

```java
@Entity
public class OutboxEvent {
    @Id @GeneratedValue
    private Long id;

    private String aggregateType;   // "Order"
    private String aggregateId;     // orderId
    private String eventType;       // "OrderCreated"

    @Column(columnDefinition = "TEXT")
    private String payload;         // JSON serialize của event

    private boolean processed = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    // constructor, getter/setter...
}

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository,
                         ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional   // MẤU CHỐT: CẢ 2 thao tác ghi dưới đây nằm trong CÙNG 1 TRANSACTION DUY NHẤT, CÙNG 1 DATABASE
    public Order placeOrder(OrderRequest request) throws JsonProcessingException {
        Order order = new Order(request.customerId(), request.items());
        orderRepository.save(order);   // GHI 1: lưu Order

        OrderCreatedEvent eventPayload = new OrderCreatedEvent(order.getId(), order.getCustomerId());
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("Order");
        outboxEvent.setAggregateId(order.getId().toString());
        outboxEvent.setEventType("OrderCreated");
        outboxEvent.setPayload(objectMapper.writeValueAsString(eventPayload));
        outboxEventRepository.save(outboxEvent);   // GHI 2: lưu event VÀO CHÍNH DATABASE ĐÓ, KHÔNG PHẢI message broker

        return order;
        // Khi transaction COMMIT: CẢ order VÀ outboxEvent CÙNG ĐƯỢC LƯU, hoặc CẢ HAI CÙNG ROLLBACK - KHÔNG BAO GIỜ "nửa vời"
    }
}

@Component
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPoller(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)   // chạy định kỳ mỗi 5 giây - quét các event CHƯA XỬ LÝ
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pendingEvents) {
            try {
                rabbitTemplate.convertAndSend("order.exchange", "order." + event.getEventType(), event.getPayload());
                event.setProcessed(true);   // đánh dấu ĐÃ XỬ LÝ - lần poll SAU sẽ KHÔNG gửi lại event này
                outboxEventRepository.save(event);
            } catch (Exception ex) {
                // Publish thất bại (broker tạm thời down...) - GIỮ NGUYÊN processed=false
                // -> lần @Scheduled TIẾP THEO (5 giây sau) sẽ TỰ ĐỘNG THỬ LẠI - KHÔNG MẤT event
                log.error("Lỗi publish OutboxEvent id={}, sẽ thử lại ở lần poll sau", event.getId(), ex);
            }
        }
    }
}
```

### Giải thích

- **Cách Outbox Pattern giải quyết Dual-Write Problem:** thay vì ghi vào **2 HỆ THỐNG KHÁC NHAU** (DB + Message Broker) trong 1 luồng nghiệp vụ, chuyển thành ghi **CẢ 2 BẢNG (`orders` VÀ `outbox_events`) VÀO CÙNG 1 DATABASE, TRONG CÙNG 1 TRANSACTION** — tận dụng ĐÚNG tính chất **ATOMICITY** mà bản thân RDBMS ĐÃ ĐẢM BẢO SẴN (transaction "tất cả hoặc không gì cả") — không còn cần phải "phối hợp" atomicity giữa 2 hệ thống khác nhau (vốn RẤT KHÓ đảm bảo mà không cần tới các giao thức phức tạp như 2-Phase Commit).
- **`OutboxPoller` đóng vai trò "cầu nối" ĐỘC LẬP, chạy ĐỊNH KỲ**, đọc các event CHƯA publish (`processed = false`) từ bảng `outbox_events` và publish sang RabbitMQ THẬT — nếu publish thất bại (broker tạm ngừng hoạt động), event VẪN CÒN NGUYÊN trong bảng (`processed` vẫn `false`) — lần poll TIẾP THEO (5 giây sau) sẽ TỰ ĐỘNG THỬ LẠI, KHÔNG CẦN can thiệp thủ công, KHÔNG BAO GIỜ MẤT EVENT.
- **Đảm bảo "At-Least-Once" cho việc publish (không phải "Exactly-Once"):** nếu publish RabbitMQ THÀNH CÔNG nhưng ứng dụng CRASH **NGAY TRƯỚC KHI** kịp lưu `processed = true`, event có thể được publish LẦN NỮA ở lần poll sau — đây chính là LÝ DO **Consumer PHẢI Idempotent** (đã làm ở Bài 3) — Outbox Pattern giải quyết vấn đề "MẤT event", còn "TRÙNG event" (hiếm hơn, do crash đúng thời điểm) vẫn cần Consumer tự bảo vệ bằng `messageId`.
- **So sánh với cách viết "ngây thơ" (gọi trực tiếp `rabbitTemplate` trong `placeOrder()`):** cách "ngây thơ" đơn giản hơn về code, nhưng có "cửa sổ rủi ro" (window of failure) giữa 2 thao tác ghi khác hệ thống — Outbox Pattern PHỨC TẠP HƠN (cần thêm bảng, thêm poller định kỳ, độ trễ publish tối đa = khoảng thời gian giữa 2 lần poll) nhưng đổi lại đảm bảo **KHÔNG BAO GIỜ MẤT EVENT**, đây là đánh đổi HOÀN TOÀN XỨNG ĐÁNG cho hệ thống có yêu cầu tính đúng đắn nghiêm ngặt (như capstone Flash-Sale — liên hệ Module 11, Bài 5).

---

*Đây là lời giải cho toàn bộ Phần B của Module 27. Tiếp theo: Module 20 — Microservices.*
