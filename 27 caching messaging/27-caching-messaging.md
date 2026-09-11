# Module 18 — Caching & Messaging

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Đây là 2 kỹ thuật cốt lõi để hệ thống chịu tải cao (high-throughput) và tách rời (decouple) các thành phần — nền tảng trực tiếp cho Module Microservices sắp tới. Caching sai cách (đặc biệt là **Cache Invalidation** — "one of the two hard things in Computer Science") gây ra bug hiển thị dữ liệu cũ khó phát hiện; dùng Message Queue sai (không hiểu Delivery Guarantee, không xử lý message trùng lặp) gây mất dữ liệu hoặc xử lý nghiệp vụ 2 lần (VD: gửi email 2 lần, trừ tiền 2 lần).

> **Phạm vi bài này:** Tập trung vào cơ chế Caching và Message Queue **ở tầng ứng dụng Spring Boot**. Không đi sâu vào vận hành/cấu hình hạ tầng Redis Cluster hay Kafka Broker (Partition Rebalancing, Replication Factor...) — những chủ đề đó thuộc phạm vi DevOps/System Design chuyên sâu hơn, chỉ nhắc tới mức đủ hiểu khi cần thiết kế ứng dụng phía Client.

---

## Mục lục

1. [Vì sao cần Caching?](#1-vì-sao-cần-caching)
2. [Spring Cache Abstraction](#2-spring-cache-abstraction)
3. [Redis làm Cache Server](#3-redis-làm-cache-server)
4. [Cache Pattern: Cache-Aside, Write-Through, Write-Behind](#4-cache-pattern)
5. [Cache Invalidation & TTL](#5-cache-invalidation--ttl)
6. [Vấn đề kinh điển: Cache Stampede, Cache Penetration, Cache Avalanche](#6-các-vấn-đề-kinh-điển-của-caching)
7. [Message Queue — vì sao cần?](#7-message-queue--vì-sao-cần)
8. [RabbitMQ — mô hình Message Queue truyền thống](#8-rabbitmq)
9. [Kafka — mô hình Event Streaming](#9-kafka)
10. [Delivery Guarantee & Idempotent Consumer](#10-delivery-guarantee--idempotent-consumer)
11. [Outbox Pattern — giải quyết Dual-Write Problem](#11-outbox-pattern)
12. [@Async — xử lý bất đồng bộ trong Spring](#12-async)
13. [⚠️ Các bẫy hay gặp](#13-các-bẫy-hay-gặp)
14. [Tổng kết — Bảng ghi nhớ nhanh](#14-tổng-kết--bảng-ghi-nhớ-nhanh)
15. [Bài tập luyện tập](#15-bài-tập-luyện-tập)

---

## 1. Vì sao cần Caching?

**Caching** là kỹ thuật lưu tạm kết quả của 1 thao tác **tốn kém** (query DB phức tạp, gọi API bên ngoài chậm, tính toán nặng) để **tái sử dụng** cho các lần gọi sau, tránh lặp lại thao tác tốn kém đó.

```
Không có Cache:
Client -> Server -> Query DB (50ms) -> Trả kết quả
Client -> Server -> Query DB (50ms) -> Trả kết quả  (LẶP LẠI query giống hệt lần trước!)
Client -> Server -> Query DB (50ms) -> Trả kết quả

Có Cache:
Client -> Server -> Query DB (50ms) -> Lưu vào Cache -> Trả kết quả
Client -> Server -> Đọc từ Cache (0.5ms) -> Trả kết quả  (NHANH HƠN 100 LẦN!)
Client -> Server -> Đọc từ Cache (0.5ms) -> Trả kết quả
```

**Lợi ích:**
- Giảm tải cho Database (đặc biệt hữu ích khi dữ liệu **đọc nhiều, ghi ít** — read-heavy workload)
- Giảm độ trễ (latency) response cho client
- Giảm chi phí hạ tầng (không cần scale DB lên quá lớn)

**Đánh đổi:**
- **Cache Invalidation** (khi nào xóa/cập nhật cache) là bài toán khó — dữ liệu trong cache có thể **cũ (stale)** so với DB thật nếu không quản lý đúng
- Tốn thêm bộ nhớ, thêm 1 thành phần hạ tầng cần vận hành/giám sát (Redis server)

---

## 2. Spring Cache Abstraction

Spring cung cấp sẵn 1 lớp trừu tượng (`spring-boot-starter-cache`) giúp thêm caching **chỉ bằng annotation** — không cần viết code quản lý cache thủ công, và có thể **đổi cache provider** (Redis, Caffeine, EhCache...) mà không sửa business logic.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableCaching // Bật tính năng Cache Abstraction
public class MyApp { }
```

### @Cacheable — cache kết quả trả về của method

```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    // Lần gọi đầu: chạy method thật, lưu kết quả vào cache với key = "products::5" (VD: id=5)
    // Lần gọi sau (cùng id): trả THẲNG từ cache, KHÔNG chạy lại method (không query DB!)
    public Product getProduct(Long id) {
        System.out.println("Đang query DB cho product " + id); // Chỉ in ra ở lần gọi ĐẦU TIÊN
        return productRepository.findById(id).orElseThrow();
    }
}
```

### @CachePut — LUÔN chạy method, đồng thời cập nhật cache (khác @Cacheable)

```java
@CachePut(value = "products", key = "#product.id")
// Khác @Cacheable: method LUÔN được thực thi (không "tắt" bằng cache),
// nhưng kết quả trả về sẽ ĐƯỢC CẬP NHẬT vào cache
// -> Dùng cho thao tác UPDATE, đảm bảo cache luôn khớp với DB sau khi sửa
public Product updateProduct(Product product) {
    return productRepository.save(product);
}
```

### @CacheEvict — xóa cache khi dữ liệu bị xóa/thay đổi

```java
@CacheEvict(value = "products", key = "#id")
public void deleteProduct(Long id) {
    productRepository.deleteById(id);
    // Sau khi xóa DB, cache entry tương ứng cũng bị xóa
    // -> tránh trường hợp cache vẫn còn dữ liệu của record đã xóa (dữ liệu "ma")
}

@CacheEvict(value = "products", allEntries = true) // Xóa TOÀN BỘ cache "products"
public void reloadAllProducts() { ... }
```

### @Caching — kết hợp nhiều thao tác cache trên 1 method

```java
@Caching(
    put = { @CachePut(value = "products", key = "#product.id") },
    evict = { @CacheEvict(value = "productList", allEntries = true) } // Xóa cache danh sách khi 1 sản phẩm đổi
)
public Product updateProduct(Product product) {
    return productRepository.save(product);
}
```

⚠️ **Bẫy self-invocation — LẶP LẠI đúng vấn đề đã học ở Module 12/16:** `@Cacheable`/`@CachePut`/`@CacheEvict` cũng hoạt động dựa trên **Spring AOP Proxy** — gọi qua `this.method()` trong cùng class sẽ **bỏ qua hoàn toàn** cơ chế cache.

```java
@Service
public class ProductService {
    public void refreshAndGet(Long id) {
        this.getProduct(id); // ❌ Gọi qua "this" -> BỎ QUA cache hoàn toàn, luôn query DB!
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) { ... }
}
```

---

## 3. Redis làm Cache Server

Mặc định, Spring Cache Abstraction dùng `ConcurrentHashMap` **trong bộ nhớ (in-memory)** của chính ứng dụng — vấn đề: **không chia sẻ được cache giữa nhiều instance** (khi scale ngang nhiều server), và mất hết cache khi ứng dụng restart.

**Redis** giải quyết vấn đề này — là 1 in-memory data store **độc lập**, chạy như 1 server riêng, mọi instance ứng dụng đều kết nối tới **cùng 1 Redis** → cache được chia sẻ đồng nhất.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  cache:
    type: redis
    redis:
      time-to-live: 600000 # TTL mặc định 10 phút (đơn vị: ms)
```

```
Không dùng Redis (cache local từng instance):
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ Instance A   │   │ Instance B   │   │ Instance C   │
│ Cache riêng   │   │ Cache riêng   │   │ Cache riêng   │
└─────────────┘   └─────────────┘   └─────────────┘
-> Mỗi instance có bản cache RIÊNG, KHÔNG đồng bộ -> user gọi lần lượt 3 instance
   có thể thấy dữ liệu KHÁC NHAU (instance A đã cập nhật, B/C vẫn cache cũ)

Dùng Redis (cache tập trung, chia sẻ):
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ Instance A   │   │ Instance B   │   │ Instance C   │
└──────┬──────┘   └──────┬──────┘   └──────┬──────┘
       │                  │                  │
       └──────────────────┴──────────────────┘
                           │
                    ┌─────────────┐
                    │    Redis     │  <- Cache DUY NHẤT, mọi instance đều thấy CÙNG dữ liệu
                    └─────────────┘
```

> **Liên hệ Module 10:** Redis đã được giới thiệu ở phần NoSQL — ở đây là ứng dụng cụ thể nhất và phổ biến nhất của Redis trong thực tế: **làm Cache layer** cho ứng dụng Backend.

### Redis Template — thao tác trực tiếp (không qua annotation) khi cần logic phức tạp hơn

```java
@Service
public class ProductCacheService {

    private final RedisTemplate<String, Product> redisTemplate;

    public Product getProduct(Long id) {
        String key = "product:" + id;
        Product cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached; // Cache hit
        }

        Product product = productRepository.findById(id).orElseThrow(); // Cache miss -> query DB
        redisTemplate.opsForValue().set(key, product, Duration.ofMinutes(10)); // Lưu vào cache kèm TTL
        return product;
    }
}
```

### Multi-level Cache — kết hợp Caffeine (Local) + Redis (Distributed)

Redis vẫn tốn **1 lần round-trip network** cho mỗi lần đọc (dù nhanh hơn DB rất nhiều, vẫn chậm hơn đọc trực tiếp trong bộ nhớ JVM). Với dữ liệu **cực kỳ nóng** (hot data — được đọc hàng nghìn lần/giây, VD: cấu hình hệ thống, danh mục sản phẩm), nhiều hệ thống production dùng thêm **cache local (L1)** ngay trong từng instance, đặt trước Redis (L2):

```
Request -> [L1: Caffeine - trong JVM, ~0.01ms] -> hit? trả ngay
                    │ miss
                    ▼
           [L2: Redis - qua network, ~1ms] -> hit? trả về + ghi vào L1
                    │ miss
                    ▼
           [DB - ~50ms] -> trả về + ghi vào L2 VÀ L1
```

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

```java
@Bean
public CacheManager caffeineCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("hotConfig");
    cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)              // Giới hạn số entry - Caffeine tự evict theo LRU khi đầy
            .expireAfterWrite(Duration.ofMinutes(5)));
    return cacheManager;
}
```

| | Caffeine (Local, L1) | Redis (Distributed, L2) |
|---|---|---|
| Vị trí lưu | Trong bộ nhớ JVM của TỪNG instance | Server riêng, chia sẻ giữa mọi instance |
| Tốc độ | Cực nhanh (không qua network) | Nhanh, nhưng có round-trip network |
| Nhất quán giữa instance | ❌ Mỗi instance có bản riêng, có thể lệch nhau | ✅ Tất cả instance thấy cùng dữ liệu |
| Mất khi restart | Có (nằm trong JVM) | Không (Redis là process riêng) |
| Phù hợp | Dữ liệu cực nóng, hiếm đổi, chấp nhận độ trễ đồng bộ nhỏ giữa các instance | Dữ liệu cần nhất quán giữa các instance |

⚠️ **Đánh đổi của Multi-level Cache:** L1 (Caffeine) làm tăng độ phức tạp invalidation — khi dữ liệu đổi, phải evict CẢ L1 (từng instance) LẪN L2 (Redis), thường cần thêm cơ chế broadcast (VD: Redis Pub/Sub gửi tín hiệu "evict" tới mọi instance). Chỉ nên áp dụng cho dữ liệu thực sự cực nóng, không phải mặc định cho mọi cache.

---

## 4. Cache Pattern

### 4.1. Cache-Aside (Lazy Loading) — pattern phổ biến nhất, chính là cách `@Cacheable` hoạt động

```
Đọc dữ liệu:
1. Application kiểm tra Cache trước
2. Cache HIT -> trả về ngay
3. Cache MISS -> Application tự query DB, sau đó tự ghi kết quả vào Cache

Ghi dữ liệu:
1. Application ghi trực tiếp vào DB
2. Application XÓA (evict) cache entry liên quan (không update cache trực tiếp,
   để lần đọc TIẾP THEO tự động load lại dữ liệu MỚI từ DB vào cache)
```

```java
// Đây chính là cách @Cacheable + @CacheEvict hoạt động - Cache-Aside pattern "ẩn" sau annotation
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) { return productRepository.findById(id).orElseThrow(); }

@CacheEvict(value = "products", key = "#product.id")
public void updateProduct(Product product) { productRepository.save(product); }
```

**Ưu điểm:** Đơn giản, chỉ cache dữ liệu THỰC SỰ được đọc (không lãng phí bộ nhớ cache cho dữ liệu ít dùng).
**Nhược điểm:** Lần đọc đầu tiên sau khi cache miss/evict luôn chậm hơn (phải query DB).

### Race Condition trong Cache-Aside — bẫy nâng cao ít người để ý

Ngay cả khi làm đúng "ghi DB rồi evict cache", vẫn tồn tại 1 khoảng hở **race condition** hiếm gặp nhưng có thật khi 2 request chạy gần như đồng thời — 1 request ghi (write) và 1 request đọc (read):

```
Thời điểm  Request A (WRITE, đang update giá)     Request B (READ, đang đọc giá)
   T1      Đọc giá cũ từ DB (100k)
   T2                                              Cache MISS (vừa bị evict trước đó) -> đọc DB, thấy giá CŨ (100k)
   T3      Ghi giá MỚI (150k) vào DB
   T4      Evict cache (nhưng cache ĐANG TRỐNG vì B chưa kịp ghi)
   T5                                              Ghi giá CŨ (100k) vào cache (dữ liệu B đọc được ở T2)

Kết quả: Cache giờ chứa giá SAI (100k) dù DB đã có giá ĐÚNG (150k) -
         và vì evict đã xảy ra ở T4, cache "cũ" này có thể tồn tại tới hết TTL!
```

**Giải pháp thực dụng (không giải quyết được 100% nhưng giảm mạnh xác suất xảy ra):**
- Đặt **TTL đủ ngắn** làm lưới an toàn cuối cùng (nhắc lại nguyên tắc mục 5) — dù race condition xảy ra, dữ liệu sai chỉ tồn tại tối đa bằng TTL
- **Delayed Double Delete:** evict cache 1 lần ngay sau khi ghi DB (như bình thường), rồi evict THÊM 1 lần nữa sau vài trăm mili-giây (đủ thời gian cho các request đọc "lỡ nhịp" hoàn tất) để dọn sạch mọi dữ liệu cache sai có thể vừa bị ghi vào

```java
@CacheEvict(value = "products", key = "#product.id")
@Async
public void evictAgainAfterDelay(Long productId) {
    try {
        Thread.sleep(500); // Đợi các request đọc "lỡ nhịp" hoàn tất
        cacheManager.getCache("products").evict(productId); // Evict LẦN 2
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

> **Mức độ ưu tiên:** Race condition này xác suất xảy ra thấp (cần đúng thời điểm 2 request chạy chồng lấn) — hầu hết hệ thống chấp nhận rủi ro nhỏ này và chỉ dựa vào TTL ngắn làm lưới an toàn, thay vì áp dụng Delayed Double Delete cho MỌI cache. Chỉ nên đầu tư giải pháp này cho dữ liệu cực kỳ nhạy cảm với tính chính xác (giá, số dư).

### 4.2. Write-Through — ghi đồng thời cả Cache và DB trong CÙNG 1 thao tác

```
Ghi dữ liệu:
1. Application ghi vào Cache
2. Cache TỰ ĐỘNG đồng bộ ghi xuống DB (đồng bộ, trong cùng request)
-> Cache và DB LUÔN nhất quán ngay lập tức
```

```java
// Đây chính là ý tưởng của @CachePut - LUÔN chạy method (ghi DB) VÀ cập nhật cache cùng lúc
@CachePut(value = "products", key = "#product.id")
public Product updateProduct(Product product) {
    return productRepository.save(product); // Ghi DB
    // Kết quả trả về TỰ ĐỘNG được ghi vào cache ngay (Write-Through)
}
```

**Ưu điểm:** Cache luôn "tươi" (fresh), không có khoảng thời gian dữ liệu cũ.
**Nhược điểm:** Mỗi lần ghi chậm hơn (phải ghi cả 2 nơi), tốn cache cho dữ liệu có thể không bao giờ được đọc lại.

### 4.3. Write-Behind (Write-Back) — ghi Cache trước, ghi DB SAU (bất đồng bộ)

```
Ghi dữ liệu:
1. Application ghi vào Cache NGAY LẬP TỨC, trả response cho client luôn
2. Cache (hoặc 1 background job riêng) ghi xuống DB SAU đó (bất đồng bộ, có thể trễ vài giây/phút)
```

**Ưu điểm:** Ghi cực nhanh (client không phải chờ DB) — phù hợp hệ thống ghi cực nhiều (VD: đếm lượt view bài viết, like...).
**Nhược điểm:** Rủi ro **mất dữ liệu** nếu Cache server crash TRƯỚC KHI kịp ghi xuống DB — cần cân nhắc kỹ nghiệp vụ nào chấp nhận được rủi ro này.

### So sánh 3 pattern

| | Cache-Aside | Write-Through | Write-Behind |
|---|---|---|---|
| Độ phức tạp | Đơn giản, phổ biến nhất | Trung bình | Phức tạp nhất |
| Độ tươi của Cache | Có thể "cũ" 1 khoảng ngắn (giữa lúc evict và lần đọc kế) | Luôn tươi | Có thể "cũ" tạm thời (DB chưa kịp cập nhật) |
| Tốc độ ghi | Nhanh (ghi DB, evict cache) | Chậm hơn (ghi cả 2) | Rất nhanh (chỉ ghi cache) |
| Rủi ro mất dữ liệu | Thấp | Thấp | **Cao hơn** nếu cache crash trước khi sync DB |
| Dùng khi nào | Đa số trường hợp (mặc định của `@Cacheable`) | Cần cache luôn khớp DB ngay lập tức | Ghi cực nhiều, chấp nhận rủi ro nhỏ (view counter, analytics) |

---

## 5. Cache Invalidation & TTL

**"There are only two hard things in Computer Science: cache invalidation and naming things."** — Phil Karlton. Đây là câu nói kinh điển vì Cache Invalidation **thực sự khó** trong hệ thống phức tạp.

### TTL (Time-To-Live) — cách đơn giản và phổ biến nhất

```java
@Cacheable(value = "products", key = "#id")
// TTL được cấu hình ở tầng Redis config (application.yml: spring.cache.redis.time-to-live)
// hoặc riêng từng cache name qua RedisCacheManager customization
public Product getProduct(Long id) { ... }
```

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)); // TTL mặc định

    Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
    configMap.put("products", defaultConfig.entryTtl(Duration.ofMinutes(30))); // TTL riêng cho cache "products"
    configMap.put("exchangeRates", defaultConfig.entryTtl(Duration.ofSeconds(60))); // Tỷ giá đổi nhanh -> TTL ngắn

    return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(configMap)
            .build();
}
```

**Nguyên tắc chọn TTL:** Dữ liệu càng ít thay đổi → TTL càng dài (VD: thông tin sản phẩm, danh mục — vài giờ). Dữ liệu thay đổi nhanh → TTL ngắn hoặc dùng chiến lược evict chủ động thay vì chỉ dựa vào TTL (VD: tỷ giá, tồn kho — vài giây, hoặc evict ngay khi có thay đổi).

### Evict chủ động khi có sự kiện thay đổi (chính xác hơn chỉ dùng TTL)

```java
@CacheEvict(value = "products", key = "#productId")
public void updateProductPrice(Long productId, BigDecimal newPrice) {
    productRepository.updatePrice(productId, newPrice);
    // Evict NGAY lập tức khi có thay đổi -> không cần chờ TTL hết hạn
    // -> Cache luôn chính xác dù TTL còn dài
}
```

> **Best practice thực chiến:** Kết hợp cả 2 — TTL làm "lưới an toàn" cuối cùng (đề phòng trường hợp quên evict ở đâu đó), và evict chủ động khi có sự kiện thay đổi dữ liệu để đảm bảo tính chính xác ngay lập tức.

---

## 6. Các vấn đề kinh điển của Caching

### 6.1. Cache Penetration (Cache Xuyên Thủng)

**Vấn đề:** Request liên tục hỏi dữ liệu **KHÔNG TỒN TẠI** (VD: `GET /products/999999999` — id không có thật) → Cache luôn miss (vì không có gì để cache) → mọi request đều "xuyên thủng" cache, đánh thẳng vào DB → có thể bị lợi dụng để tấn công DDoS vào Database.

**Giải pháp:** Cache **cả kết quả "không tìm thấy"** (với TTL ngắn), hoặc dùng **Bloom Filter** (cấu trúc dữ liệu xác suất, kiểm tra nhanh "chắc chắn không tồn tại" trước khi query DB).

```java
@Cacheable(value = "products", key = "#id", unless = "#result == null")
// unless = "#result == null" -> mặc định KHÔNG cache giá trị null
// -> nếu không xử lý gì thêm, mỗi request hỏi id không tồn tại đều query DB lại

// Giải pháp: cache 1 "giá trị đại diện" cho trường hợp không tìm thấy
public Product getProduct(Long id) {
    return productRepository.findById(id).orElse(NULL_PRODUCT_PLACEHOLDER); // Cache cả trường hợp "rỗng"
}
```

### 6.2. Cache Avalanche (Cache Sụp Đổ Hàng Loạt)

**Vấn đề:** Số lượng lớn cache entry **cùng hết hạn (TTL) tại 1 thời điểm** → tất cả request cùng lúc "miss" cache → dồn tải đột ngột lên DB → có thể làm sập DB (do lượng query tăng đột biến).

**Giải pháp:** Thêm **jitter (độ lệch ngẫu nhiên nhỏ)** vào TTL để các cache entry không hết hạn đồng loạt cùng lúc.

```java
// Thay vì TTL cố định 600 giây cho MỌI entry:
Duration ttl = Duration.ofSeconds(600);

// Thêm jitter ngẫu nhiên (VD: ±60 giây) để tránh hết hạn đồng loạt:
Duration ttlWithJitter = Duration.ofSeconds(600 + ThreadLocalRandom.current().nextInt(-60, 60));
```

### 6.3. Cache Stampede (Cache "Giẫm Đạp" / Thundering Herd)

**Vấn đề:** Khi 1 cache entry **PHỔ BIẾN** (nhiều request cùng cần) hết hạn, hàng nghìn request **cùng lúc** phát hiện cache miss → tất cả đều đồng thời query DB để load lại **CÙNG 1 dữ liệu** → lãng phí tài nguyên, có thể làm sập DB dù chỉ 1 dữ liệu bị miss.

**Giải pháp — Locking (chỉ 1 request được phép query DB, các request khác chờ):**

```java
@Service
public class ProductCacheService {

    private final Map<Long, Object> locks = new ConcurrentHashMap<>(); // Đơn giản hóa - thực tế dùng Redis distributed lock

    public Product getProduct(Long id) {
        Product cached = redisTemplate.opsForValue().get("product:" + id);
        if (cached != null) return cached;

        Object lock = locks.computeIfAbsent(id, k -> new Object());
        synchronized (lock) { // CHỈ 1 thread được vào đây tại 1 thời điểm cho CÙNG 1 productId
            // Kiểm tra lại cache LẦN NỮA (Double-Checked Locking pattern)
            // vì có thể thread khác đã load xong trong lúc thread này chờ lock
            cached = redisTemplate.opsForValue().get("product:" + id);
            if (cached != null) return cached;

            Product product = productRepository.findById(id).orElseThrow();
            redisTemplate.opsForValue().set("product:" + id, product, Duration.ofMinutes(10));
            return product;
        }
    }
}
```

> **Liên hệ Module 05 (Concurrency):** Đây chính là ứng dụng thực tế của **Double-Checked Locking pattern** đã có nền tảng lý thuyết từ Module Thread cơ bản — kiểm tra điều kiện trước và sau khi lấy lock để tránh khóa không cần thiết.

### Bảng phân biệt 3 vấn đề (dễ nhầm lẫn tên gọi)

| Vấn đề | Nguyên nhân | Ảnh hưởng |
|---|---|---|
| **Penetration** | Query dữ liệu KHÔNG TỒN TẠI liên tục | DB bị tấn công bởi query vô nghĩa lặp lại |
| **Avalanche** | NHIỀU cache entry cùng hết hạn 1 lúc | DB bị dồn tải đột biến diện rộng |
| **Stampede** | 1 cache entry PHỔ BIẾN hết hạn, nhiều request cùng lúc load lại | DB bị dồn tải cho 1 dữ liệu cụ thể |

---

## 7. Message Queue — vì sao cần?

**Message Queue (MQ)** cho phép 2 thành phần hệ thống giao tiếp **bất đồng bộ (asynchronous)** và **tách rời (decoupled)** — Producer gửi message vào Queue, Consumer xử lý message đó **độc lập về thời gian**, không cần Producer chờ Consumer xử lý xong.

### Vấn đề khi không có Message Queue (Synchronous, gọi trực tiếp)

```java
@PostMapping("/orders")
public OrderResponse placeOrder(@RequestBody OrderRequest request) {
    Order order = orderService.createOrder(request);
    emailService.sendConfirmationEmail(order);   // Chờ gửi email xong (có thể mất 2-3 giây)
    smsService.sendConfirmationSms(order);        // Chờ gửi SMS xong
    inventoryService.updateWarehouseSystem(order); // Chờ đồng bộ hệ thống kho (external system chậm)

    return OrderResponse.from(order); // Client phải CHỜ TẤT CẢ các bước trên xong mới nhận được response!
}
```

**Vấn đề:**
- Client phải chờ **toàn bộ chuỗi xử lý** dù chỉ cần biết "đơn hàng đã tạo thành công"
- Nếu `emailService` bị lỗi/chậm (service bên thứ 3 down) → **toàn bộ request bị treo/lỗi**, dù việc tạo đơn hàng đã thành công
- Khó scale riêng từng phần (không thể tăng số lượng worker xử lý email độc lập với phần tạo order)

### Với Message Queue

```java
@PostMapping("/orders")
public OrderResponse placeOrder(@RequestBody OrderRequest request) {
    Order order = orderService.createOrder(request);
    messageProducer.publish("order.created", new OrderCreatedEvent(order.getId())); // Gửi vào Queue, KHÔNG CHỜ

    return OrderResponse.from(order); // Trả response NGAY, không chờ email/sms/kho
}

// Consumer riêng biệt, xử lý ĐỘC LẬP, có thể chạy trên server/instance khác
@Component
public class EmailNotificationConsumer {
    @RabbitListener(queues = "order.created.email")
    public void handleOrderCreated(OrderCreatedEvent event) {
        emailService.sendConfirmationEmail(event.orderId()); // Xử lý riêng, không ảnh hưởng response ban đầu
    }
}
```

**Lợi ích:**
- **Giảm độ trễ response** cho client (chỉ chờ phần logic chính, không chờ tác vụ phụ)
- **Decoupling** — Producer và Consumer không cần biết nhau tồn tại, có thể phát triển/deploy độc lập
- **Chịu lỗi tốt hơn (resilience):** Nếu Consumer tạm thời down, message vẫn nằm trong Queue chờ, không bị mất (tùy cấu hình)
- **Scale độc lập:** Tăng số lượng Consumer instance để xử lý message nhanh hơn khi tải cao, không ảnh hưởng phần API chính

---

## 8. RabbitMQ

**RabbitMQ** là Message Broker truyền thống, hiện thực chuẩn **AMQP (Advanced Message Queuing Protocol)** — mô hình **Producer → Exchange → Queue → Consumer**.

```
Producer -> Exchange -> [Routing theo Binding] -> Queue 1 -> Consumer 1
                                                 -> Queue 2 -> Consumer 2
```

### Các loại Exchange

| Loại Exchange | Cơ chế routing |
|---|---|
| **Direct** | Route theo routing key CHÍNH XÁC (VD: key = "order.created" chỉ vào đúng queue đăng ký key đó) |
| **Fanout** | Broadcast tới **TẤT CẢ** queue đã bind, không quan tâm routing key (VD: thông báo tất cả service khi có sự kiện) |
| **Topic** | Route theo pattern (VD: `order.*` khớp cả `order.created`, `order.cancelled`) |
| **Headers** | Route dựa trên header của message thay vì routing key |

### Setup cơ bản trong Spring Boot

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```java
@Configuration
public class RabbitConfig {

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue("order.created.queue", true); // durable = true -> Queue sống sót qua RabbitMQ restart
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }

    @Bean
    public Binding binding(Queue orderCreatedQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with("order.created");
    }
}

// Producer
@Service
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend("order.exchange", "order.created", event);
    }
}

// Consumer
@Component
public class OrderCreatedConsumer {
    @RabbitListener(queues = "order.created.queue")
    public void handle(OrderCreatedEvent event) {
        System.out.println("Nhận được sự kiện order created: " + event.orderId());
        // Xử lý nghiệp vụ (gửi email, cập nhật kho...)
    }
}
```

**Đặc điểm RabbitMQ:** Message thường được **xóa khỏi Queue sau khi Consumer xác nhận đã xử lý (ACK)** — phù hợp mô hình "task queue" (mỗi message chỉ cần xử lý 1 lần, bởi 1 consumer).

### Dead Letter Queue (DLQ) — xử lý message "độc" (poison message)

**Vấn đề:** Nếu Consumer xử lý 1 message bị lỗi liên tục (VD: dữ liệu message sai định dạng, bug logic không bao giờ thành công), mặc định RabbitMQ sẽ **requeue message đó và thử lại vô hạn** — message này bị "kẹt" mãi mãi, liên tục retry, chiếm tài nguyên và có thể làm nghẽn toàn bộ Queue (các message hợp lệ phía sau phải chờ).

**Giải pháp:** Cấu hình **Dead Letter Exchange (DLX)** — sau N lần retry thất bại, message tự động được chuyển sang 1 Queue riêng ("nghĩa địa message lỗi") để xử lý thủ công/giám sát riêng, thay vì retry vô hạn trên Queue chính:

```java
@Configuration
public class RabbitConfig {

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable("order.created.queue")
                .withArgument("x-dead-letter-exchange", "order.dlx")       // Trỏ tới Dead Letter Exchange
                .withArgument("x-dead-letter-routing-key", "order.failed") // Routing key khi vào DLX
                .withArgument("x-message-ttl", 30000) // Message tồn tại tối đa 30s trên Queue chính trước khi bị coi là "chết"
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("order.dlx");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("order.created.dlq").build(); // Queue lưu message lỗi để kiểm tra thủ công
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("order.failed");
    }
}
```

```java
@Component
public class DeadLetterMonitor {
    @RabbitListener(queues = "order.created.dlq")
    public void handleFailedMessage(OrderCreatedEvent event) {
        // Message đã retry hết số lần cho phép nhưng vẫn lỗi
        // -> log lại, cảnh báo (alert) cho team vận hành, KHÔNG được để "biến mất" trong im lặng
        log.error("Message xử lý thất bại sau nhiều lần retry: orderId={}", event.orderId());
        alertService.notifyOpsTeam("DLQ nhận message lỗi: " + event.orderId());
    }
}
```

> **Nguyên tắc thực chiến:** Mọi Queue xử lý nghiệp vụ quan trọng ở production **nên có DLQ đi kèm** — không có DLQ, message lỗi hoặc bị mất âm thầm (nếu Consumer chỉ log lỗi rồi bỏ qua), hoặc kẹt Queue vô hạn (nếu Consumer cứ throw exception cho RabbitMQ tự requeue) — cả 2 đều khó phát hiện và khó debug khi xảy ra ở production.

---

## 9. Kafka

**Apache Kafka** không chỉ là Message Queue — nó là 1 **nền tảng Event Streaming (Distributed Log)**, được thiết kế cho khối lượng dữ liệu cực lớn (Big Data, real-time analytics).

### Khác biệt cốt lõi so với RabbitMQ

```
RabbitMQ: Message bị XÓA khỏi Queue sau khi Consumer xử lý xong (ACK)
Kafka:    Message được LƯU LẠI (persist) trong 1 khoảng thời gian cấu hình được
          (VD: 7 ngày) - Consumer chỉ "đọc" và di chuyển con trỏ (offset),
          KHÔNG xóa message -> Nhiều Consumer Group KHÁC NHAU có thể đọc lại
          CÙNG 1 message độc lập với nhau
```

```
Kafka Topic: "order-events"
         Partition 0: [msg1] [msg2] [msg3] [msg4] ...
         Partition 1: [msg5] [msg6] [msg7] ...

Consumer Group A (VD: Email Service) -> đọc từ offset riêng của nhóm A
Consumer Group B (VD: Analytics Service) -> đọc từ offset riêng của nhóm B (ĐỘC LẬP với A)
-> Cả 2 group đều đọc được TOÀN BỘ message, không "giành" message của nhau
```

```java
// Producer
@Service
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send("order-events", String.valueOf(event.orderId()), event);
        // Key = orderId -> đảm bảo các message của CÙNG 1 order luôn vào CÙNG 1 partition
        // -> giữ được thứ tự xử lý (ordering guarantee) trong phạm vi 1 order
    }
}

// Consumer
@Component
public class OrderEventConsumer {
    @KafkaListener(topics = "order-events", groupId = "email-service-group")
    public void handle(OrderCreatedEvent event) {
        emailService.sendConfirmationEmail(event.orderId());
    }
}
```

### So sánh RabbitMQ vs Kafka

| | RabbitMQ | Kafka |
|---|---|---|
| Mô hình | Message Queue truyền thống (Producer-Consumer) | Distributed Event Log (Event Streaming) |
| Message sau khi xử lý | Bị xóa khỏi Queue (sau ACK) | **Lưu lại** theo thời gian cấu hình, không xóa ngay |
| Nhiều Consumer đọc cùng message | Không (1 message chỉ 1 Consumer xử lý, trừ Fanout Exchange) | **Có** — nhiều Consumer Group đọc độc lập |
| Throughput | Tốt (hàng chục nghìn msg/giây) | **Rất cao** (hàng triệu msg/giây) — thiết kế cho Big Data |
| Độ phức tạp vận hành | Đơn giản hơn | Phức tạp hơn (cần hiểu Partition, Consumer Group, Offset) |
| Use case phù hợp | Task Queue (xử lý công việc nền: gửi email, tạo PDF...) | Event Sourcing, real-time analytics, log aggregation, Microservices event-driven architecture |

> **Ghi nhớ đơn giản:** RabbitMQ giống "hàng đợi công việc" (làm xong thì bỏ đi); Kafka giống "sổ nhật ký sự kiện" (ghi lại mãi mãi, ai cần đọc lại lúc nào cũng được, theo đúng thứ tự thời gian).

---

## 10. Delivery Guarantee & Idempotent Consumer

### 3 mức độ đảm bảo giao message

| Mức đảm bảo | Ý nghĩa | Rủi ro |
|---|---|---|
| **At-most-once** | Message được gửi **tối đa 1 lần** — có thể MẤT nếu lỗi xảy ra | Mất message |
| **At-least-once** | Message được gửi **ít nhất 1 lần** — có thể bị **gửi TRÙNG LẶP** nếu retry | Xử lý message trùng lặp (phổ biến nhất trong thực tế) |
| **Exactly-once** | Message được xử lý **CHÍNH XÁC 1 lần** — lý tưởng nhất nhưng khó đạt được tuyệt đối, tốn chi phí | Phức tạp, hiệu năng thấp hơn |

> **Thực tế:** Đa số hệ thống dùng **At-least-once** (đơn giản, hiệu năng tốt) và tự xử lý vấn đề trùng lặp ở tầng Consumer bằng **Idempotent Consumer Pattern** — thay vì cố gắng đạt "Exactly-once" tuyệt đối ở tầng hạ tầng (rất tốn kém, phức tạp).

### Idempotent Consumer — đảm bảo xử lý message trùng lặp KHÔNG gây tác dụng phụ (liên hệ trực tiếp Idempotency Key ở Module 14)

```java
@Component
public class OrderEventConsumer {

    private final ProcessedMessageRepository processedMessageRepository; // Bảng lưu message ID đã xử lý

    @RabbitListener(queues = "order.created.queue")
    @Transactional
    public void handle(OrderCreatedEvent event) {
        // Kiểm tra message này ĐÃ được xử lý trước đó chưa (dựa vào messageId duy nhất)
        if (processedMessageRepository.existsById(event.messageId())) {
            log.info("Message {} đã được xử lý trước đó, bỏ qua (tránh xử lý trùng lặp)", event.messageId());
            return; // Bỏ qua - KHÔNG xử lý lại nghiệp vụ (không gửi email 2 lần!)
        }

        emailService.sendConfirmationEmail(event.orderId());
        processedMessageRepository.save(new ProcessedMessage(event.messageId(), Instant.now()));
        // Lưu ý: Việc lưu "đã xử lý" và xử lý nghiệp vụ nên nằm trong CÙNG 1 transaction
        // để đảm bảo tính nhất quán (nếu 1 trong 2 fail, cả 2 đều rollback)
    }
}
```

⚠️ **Vì sao At-least-once + retry luôn có khả năng gửi trùng lặp?** Khi Consumer xử lý message xong nhưng **CHƯA KỊP gửi ACK về Broker** (VD: mạng bị ngắt đúng lúc đó), Broker sẽ nghĩ message **chưa được xử lý** và gửi lại (redeliver) — dẫn tới Consumer nhận **cùng 1 message 2 lần**. Đây không phải "lỗi" của hệ thống Message Queue — mà là **đánh đổi thiết kế tất yếu** để đảm bảo không mất message, và trách nhiệm xử lý trùng lặp thuộc về tầng Consumer (Idempotent Consumer Pattern).

---

## 11. Outbox Pattern

### Vấn đề: Dual-Write Problem

Mục 10 giải quyết vấn đề **Consumer nhận trùng message**. Nhưng còn 1 vấn đề gốc rễ hơn ở phía **Producer**: khi 1 nghiệp vụ cần **vừa ghi DB, vừa publish message**, 2 thao tác này chạm tới **2 hệ thống khác nhau** (Database và Message Broker) — không thể đặt cả 2 trong **cùng 1 transaction** theo cách thông thường:

```java
@Transactional
public void placeOrder(OrderRequest request) {
    Order order = orderRepository.save(new Order(request)); // (1) Ghi DB - nằm trong transaction
    messageProducer.publish("order.created", new OrderCreatedEvent(order.getId())); // (2) Publish message - KHÔNG nằm trong transaction DB!

    // ⚠️ Nếu (1) thành công nhưng (2) thất bại (Broker tạm thời down) -> Order đã lưu nhưng
    //    KHÔNG CÓ message nào được gửi -> email xác nhận/đồng bộ kho KHÔNG BAO GIỜ xảy ra!
    // ⚠️ Nếu (2) publish thành công nhưng transaction DB sau đó ROLLBACK (lỗi ở bước khác)
    //    -> message ĐÃ được gửi cho 1 Order KHÔNG HỀ tồn tại trong DB!
}
```

Đây gọi là **Dual-Write Problem** — không có cách nào đảm bảo **cả 2 thao tác cùng thành công hoặc cùng thất bại** khi chúng thuộc 2 hệ thống độc lập.

### Giải pháp: Outbox Pattern

**Ý tưởng cốt lõi:** Thay vì publish message trực tiếp, ghi message vào **1 bảng riêng trong CHÍNH database đó** (bảng `outbox`) — trong **CÙNG 1 transaction** với thao tác nghiệp vụ chính. Sau đó, 1 tiến trình riêng (poller) đọc bảng `outbox` và publish message thật, đảm bảo tính nguyên tử (atomicity) nhờ tận dụng transaction của chính database.

```java
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id @GeneratedValue private Long id;
    private String aggregateType;   // VD: "Order"
    private String aggregateId;     // VD: orderId
    private String eventType;       // VD: "OrderCreated"
    @Column(columnDefinition = "TEXT")
    private String payload;         // JSON của event
    private boolean processed = false;
    private Instant createdAt = Instant.now();
}

@Service
public class OrderService {

    @Transactional
    public Order placeOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));

        // Ghi vào bảng outbox - CÙNG transaction với việc lưu Order
        // -> HOẶC CẢ 2 CÙNG COMMIT, HOẶC CẢ 2 CÙNG ROLLBACK - không còn dual-write problem!
        OutboxEvent event = new OutboxEvent("Order", order.getId().toString(),
                "OrderCreated", toJson(new OrderCreatedEvent(order.getId())));
        outboxRepository.save(event);

        return order;
        // Chưa hề gọi tới Message Broker ở bước này!
    }
}
```

```java
@Component
public class OutboxPoller {

    // Chạy định kỳ, đọc các event CHƯA publish và gửi đi
    @Scheduled(fixedDelay = 1000) // Mỗi 1 giây quét 1 lần
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAt();

        for (OutboxEvent event : pending) {
            try {
                rabbitTemplate.convertAndSend("order.exchange", "order.created", event.getPayload());
                event.setProcessed(true); // Đánh dấu đã publish thành công
            } catch (Exception e) {
                log.warn("Publish thất bại, sẽ thử lại ở lần quét sau: {}", event.getId());
                // Không set processed=true -> lần quét TIẾP THEO sẽ thử lại
            }
        }
    }
}
```

### Vì sao giải quyết được vấn đề?

```
Bước 1 (đồng bộ, trong 1 transaction DB): Lưu Order + Lưu OutboxEvent
        -> CHỈ có 1 transaction duy nhất, chạm 1 database duy nhất
        -> Hoặc CẢ 2 cùng thành công, hoặc CẢ 2 cùng rollback - KHÔNG còn tình huống "nửa vời"

Bước 2 (bất đồng bộ, riêng biệt): OutboxPoller đọc bảng outbox, publish message thật
        -> Nếu publish thất bại -> record outbox vẫn còn "chưa xử lý" -> retry ở lần quét sau
        -> Đảm bảo message CUỐI CÙNG sẽ được gửi (ít nhất 1 lần - vẫn cần Idempotent Consumer ở mục 10)
```

| | Publish trực tiếp (có Dual-Write Problem) | Outbox Pattern |
|---|---|---|
| Tính nguyên tử DB + Message | ❌ Không đảm bảo | ✅ Đảm bảo (cùng 1 transaction DB) |
| Độ phức tạp | Đơn giản | Phức tạp hơn (cần thêm bảng, poller/scheduler) |
| Độ trễ publish message | Ngay lập tức | Trễ 1 khoảng nhỏ (chờ poller quét, VD: 1 giây) |
| Phù hợp | Hệ thống chấp nhận rủi ro nhỏ mất message | Nghiệp vụ quan trọng (đơn hàng, thanh toán) cần đảm bảo message không bao giờ "biến mất" |

> **Lưu ý:** Outbox Pattern vẫn đảm bảo **At-least-once** (không phải Exactly-once) — message vẫn có thể được publish trùng nếu poller crash giữa lúc publish thành công và lúc set `processed=true`. Vì vậy Outbox Pattern **luôn cần đi kèm Idempotent Consumer** (mục 10) ở phía nhận — 2 pattern này bổ trợ nhau: Outbox đảm bảo message **không bị mất** ở Producer, Idempotent Consumer đảm bảo message trùng lặp **không gây hại** ở Consumer.

---

## 12. @Async

Spring cung cấp cách đơn giản để chạy 1 method **bất đồng bộ** (trong thread riêng) mà **không cần Message Queue** — phù hợp cho tác vụ nền đơn giản, trong cùng 1 ứng dụng (không cần giao tiếp giữa nhiều service khác nhau).

```java
@SpringBootApplication
@EnableAsync // Bật tính năng Async
public class MyApp { }

@Service
public class NotificationService {

    @Async // Method này chạy trên THREAD KHÁC, không block thread đang xử lý HTTP request
    public void sendWelcomeEmail(String email) {
        // Tác vụ chậm (gọi SMTP server) chạy nền, không ảnh hưởng response time của API
        emailClient.send(email, "Chào mừng bạn!");
    }

    @Async
    public CompletableFuture<Boolean> checkFraudAsync(Order order) {
        // Trả về CompletableFuture nếu cần LẤY KẾT QUẢ của tác vụ async sau này
        boolean isFraud = fraudDetectionApi.check(order);
        return CompletableFuture.completedFuture(isFraud);
    }
}
```

```java
@RestController
public class OrderController {
    private final NotificationService notificationService;

    @PostMapping("/orders")
    public OrderResponse createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);
        notificationService.sendWelcomeEmail(order.getUserEmail()); // KHÔNG chờ - trả response ngay
        return OrderResponse.from(order);
    }
}
```

### Cấu hình Thread Pool riêng cho @Async (quan trọng — mặc định không tối ưu cho production)

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        return executor;
        // Liên hệ Module 05.2 (Concurrency Utilities) - đây chính là ThreadPoolExecutor
        // đã học, được Spring "bọc" lại tiện dùng hơn qua @Async
    }
}

@Service
public class NotificationService {
    @Async("taskExecutor") // Chỉ định RÕ RÀNG thread pool nào sẽ dùng
    public void sendWelcomeEmail(String email) { ... }
}
```

⚠️ **`@Async` cũng dựa trên Spring AOP Proxy** — gặp đúng bẫy self-invocation quen thuộc, và **không nên dùng cho tác vụ BẮT BUỘC phải thành công** (VD: xử lý thanh toán) vì lỗi xảy ra trong method `@Async` **không được ném ngược lại cho caller** — caller không hề biết tác vụ đã fail trừ khi tự xử lý qua `CompletableFuture` hoặc `AsyncUncaughtExceptionHandler`.

### @Async vs Message Queue — khi nào dùng cái nào?

| | `@Async` | Message Queue (RabbitMQ/Kafka) |
|---|---|---|
| Phạm vi | Trong CÙNG 1 ứng dụng, CÙNG 1 process | Giữa các service/ứng dụng KHÁC NHAU (kể cả khác ngôn ngữ) |
| Độ tin cậy khi lỗi | Thấp — mất tác vụ nếu ứng dụng crash giữa chừng | Cao hơn — message được persist, có thể retry sau khi Consumer crash |
| Độ phức tạp | Đơn giản, không cần hạ tầng thêm | Cần vận hành thêm hệ thống Message Broker riêng |
| Khi nào dùng | Tác vụ đơn giản, chấp nhận rủi ro nhỏ, trong 1 service (VD: gửi email không quan trọng) | Giao tiếp giữa Microservices, cần đảm bảo không mất dữ liệu, cần scale Consumer độc lập |

---

## 13. ⚠️ Các bẫy hay gặp

1. **Self-invocation với `@Cacheable`/`@CacheEvict`/`@Async`** — gọi qua `this.method()` bỏ qua hoàn toàn cơ chế (giống hệt bẫy `@Transactional` đã học).

2. **Không evict cache khi dữ liệu thay đổi** — chỉ dựa vào TTL, khiến user thấy dữ liệu cũ trong khoảng thời gian dài không cần thiết.

3. **Cache dữ liệu nhạy cảm không có TTL hợp lý hoặc không mã hóa** — VD: cache thông tin cá nhân với TTL quá dài, tăng rủi ro nếu Redis bị truy cập trái phép.

4. **Không xử lý Cache Penetration** — API bị lợi dụng hỏi liên tục ID không tồn tại, gây quá tải DB.

5. **TTL giống hệt nhau cho hàng loạt cache entry** — dẫn tới Cache Avalanche khi tất cả cùng hết hạn 1 lúc.

6. **Coi Kafka như RabbitMQ (task queue thông thường)** — dùng sai mô hình, không tận dụng được ưu thế Event Streaming/nhiều Consumer Group đọc độc lập của Kafka.

7. **Không xử lý Idempotent Consumer** khi dùng At-least-once delivery — message bị xử lý trùng lặp (gửi email 2 lần, cộng điểm thưởng 2 lần...).

8. **Dùng `@Async` cho tác vụ BẮT BUỘC phải thành công** (thanh toán, trừ tồn kho) — lỗi âm thầm biến mất, không ai biết tác vụ đã fail.

9. **Không cấu hình Thread Pool riêng cho `@Async`** — dùng Thread Pool mặc định (`SimpleAsyncTaskExecutor`, tạo Thread MỚI cho MỖI lần gọi, không giới hạn) → có thể gây **cạn kiệt tài nguyên hệ thống** khi tải cao.

10. **Producer publish message nhưng Consumer không tồn tại/không lắng nghe đúng Queue/Topic** — message "biến mất" trong im lặng, khó debug nếu không có giám sát (monitoring) phù hợp.

11. **Ghi DB và publish message trực tiếp trong 1 method mà không dùng Outbox Pattern** (Dual-Write Problem) — dữ liệu đã lưu nhưng message không bao giờ gửi (hoặc ngược lại), gây mất đồng bộ nghiêm trọng giữa các service.

12. **Không có Dead Letter Queue** — message lỗi bị RabbitMQ requeue vô hạn (nghẽn Queue) hoặc bị Consumer log rồi bỏ qua âm thầm, không ai phát hiện được nghiệp vụ đã thất bại.

---

## 14. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| `@Cacheable` | Cache kết quả, method KHÔNG chạy lại nếu cache hit |
| `@CachePut` | LUÔN chạy method, cập nhật cache (Write-Through) |
| `@CacheEvict` | Xóa cache — dùng khi dữ liệu bị xóa/thay đổi |
| Multi-level Cache | Caffeine (L1, local, cực nhanh) + Redis (L2, chia sẻ) — chỉ cho dữ liệu cực nóng |
| Cache-Aside | Đọc: check cache → miss thì query DB rồi cache lại; Ghi: ghi DB rồi evict cache |
| Cache-Aside Race Condition | Read/Write chồng lấn có thể ghi cache SAI — TTL ngắn hoặc Delayed Double Delete |
| Cache Penetration | Query dữ liệu không tồn tại liên tục — cache cả kết quả rỗng |
| Cache Avalanche | Nhiều cache cùng hết hạn 1 lúc — thêm jitter vào TTL |
| Cache Stampede | 1 cache phổ biến hết hạn, nhiều request cùng load — dùng Locking |
| RabbitMQ | Message Queue truyền thống — message bị xóa sau khi Consumer ACK |
| Dead Letter Queue | Message lỗi sau N lần retry chuyển sang Queue riêng, tránh nghẽn/mất âm thầm |
| Kafka | Event Streaming — message được lưu lại, nhiều Consumer Group đọc độc lập |
| At-least-once | Phổ biến nhất trong thực tế — luôn có khả năng trùng lặp, cần Idempotent Consumer |
| Outbox Pattern | Ghi DB + event vào CÙNG transaction, poller publish sau — giải quyết Dual-Write Problem |
| `@Async` | Chạy nền trong CÙNG ứng dụng — không đảm bảo tin cậy cao như Message Queue |

---

## 15. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. `@Cacheable` sẽ LUÔN chạy lại method dù cache đã có sẵn dữ liệu.
2. Gọi `this.getProduct(id)` (method có `@Cacheable`) từ 1 method khác trong CÙNG class sẽ vẫn kích hoạt cơ chế cache bình thường.
3. Kafka xóa message ngay sau khi 1 Consumer xử lý xong, giống hệt cơ chế của RabbitMQ.
4. Cache Stampede xảy ra khi NHIỀU cache entry khác nhau cùng hết hạn tại 1 thời điểm.
5. At-least-once delivery guarantee có nghĩa là message có thể bị xử lý trùng lặp, cần cơ chế Idempotent Consumer ở tầng Consumer.
6. `@Async` đảm bảo độ tin cậy tương đương với việc dùng Message Queue thật sự.
7. Thêm "jitter" (độ lệch ngẫu nhiên) vào TTL giúp giảm nguy cơ Cache Avalanche.
8. Redis dùng làm Cache tập trung giúp nhiều instance ứng dụng chia sẻ chung 1 bản cache nhất quán.
9. Outbox Pattern đảm bảo message được publish với đúng "Exactly-once" tuyệt đối, không cần Idempotent Consumer nữa.
10. Dead Letter Queue giúp tránh trường hợp 1 message lỗi bị RabbitMQ requeue và retry vô hạn, làm nghẽn Queue chính.

### Phần B — Bài tập viết code (6 bài)

**Bài 1:** Viết `ProductService` dùng `@Cacheable`, `@CachePut`, `@CacheEvict` đầy đủ cho 3 method: `getProduct(id)`, `updateProduct(product)`, `deleteProduct(id)`.

**Bài 2:** Viết đoạn code minh họa giải pháp chống **Cache Penetration** cho method `getProduct(id)` — cache cả trường hợp sản phẩm không tồn tại với TTL ngắn hơn (VD: 30 giây thay vì 10 phút).

**Bài 3:** Viết Producer (RabbitMQ) publish sự kiện `OrderCreatedEvent` khi tạo đơn hàng thành công, và Consumer xử lý gửi email — đảm bảo Idempotent bằng cách kiểm tra `messageId` đã xử lý chưa trước khi gửi.

**Bài 4:** Viết ví dụ minh họa Cache Stampede và cách khắc phục bằng Double-Checked Locking (dùng `synchronized`), tương tự mục 6.3 nhưng áp dụng cho 1 tình huống khác: cache "danh sách sản phẩm bán chạy nhất trong ngày" (dữ liệu này cực kỳ phổ biến, mọi user đều truy cập).

**Bài 5:** Viết `@Async` method gửi thông báo push notification khi có đơn hàng mới, kèm cấu hình `ThreadPoolTaskExecutor` riêng (core=3, max=10, queue=50). Giải thích vì sao KHÔNG nên dùng `@Async` cho việc trừ tiền trong ví điện tử của user.

**Bài 6:** Viết `OrderService.placeOrder()` áp dụng **Outbox Pattern** đầy đủ — lưu `Order` và `OutboxEvent` trong cùng transaction, cùng với 1 `OutboxPoller` đơn giản dùng `@Scheduled` để publish các event chưa xử lý. Giải thích ngắn gọn Dual-Write Problem mà cách viết này giải quyết được.

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Ngược lại — nếu cache đã có dữ liệu (cache hit), `@Cacheable` sẽ trả THẲNG từ cache, KHÔNG chạy lại method.
2. **Sai.** Đây là bẫy self-invocation kinh điển — gọi qua `this` bỏ qua hoàn toàn Spring AOP Proxy, cache annotation mất tác dụng.
3. **Sai.** Ngược lại — Kafka LƯU LẠI message theo thời gian cấu hình (không xóa ngay), khác biệt cốt lõi so với RabbitMQ.
4. **Đúng.** Đây chính là định nghĩa của Cache Avalanche — phân biệt với Cache Stampede (1 entry phổ biến hết hạn).
5. **Đúng.** Đây là bản chất cố hữu của At-least-once — không thể tránh khỏi trùng lặp hoàn toàn, phải xử lý ở tầng ứng dụng.
6. **Sai.** `@Async` chạy trong cùng ứng dụng/process, KHÔNG đảm bảo persist message như Message Queue — nếu ứng dụng crash giữa chừng, tác vụ async có thể bị mất hoàn toàn.
7. **Đúng.** Jitter giúp các cache entry hết hạn rải rác thay vì đồng loạt cùng lúc.
8. **Đúng.** Đây là lý do chính Redis được dùng làm Cache tập trung thay vì cache local (in-memory) của từng instance.
9. **Sai.** Outbox Pattern vẫn chỉ đảm bảo At-least-once — poller có thể publish trùng nếu crash giữa lúc gửi xong và lúc đánh dấu `processed=true`, nên vẫn cần Idempotent Consumer ở phía nhận.
10. **Đúng.** Đây chính là mục đích thiết kế của DLQ — sau N lần retry thất bại, message được chuyển sang Queue riêng thay vì requeue vô hạn.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
@Service
public class ProductCacheService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Object NOT_FOUND_MARKER = new Object(); // Giá trị đại diện cho "không tồn tại"

    public Optional<Product> getProduct(Long id) {
        String key = "product:" + id;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            if (cached == NOT_FOUND_MARKER || "NOT_FOUND".equals(cached)) {
                return Optional.empty(); // Đã biết chắc là KHÔNG TỒN TẠI, không cần query DB lại
            }
            return Optional.of((Product) cached);
        }

        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            redisTemplate.opsForValue().set(key, product.get(), Duration.ofMinutes(10)); // TTL bình thường
        } else {
            // Cache CẢ trường hợp không tìm thấy, nhưng với TTL NGẮN HƠN
            // (tránh cache "vĩnh viễn" 1 ID có thể được tạo THẬT trong tương lai)
            redisTemplate.opsForValue().set(key, "NOT_FOUND", Duration.ofSeconds(30));
        }
        return product;
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
// Producer
@Service
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(), // messageId duy nhất cho mỗi lần publish
                order.getId(),
                order.getUser().getEmail());
        rabbitTemplate.convertAndSend("order.exchange", "order.created", event);
    }
}

public record OrderCreatedEvent(String messageId, Long orderId, String userEmail) {}

// Entity lưu message đã xử lý (Idempotent tracking)
@Entity
public class ProcessedMessage {
    @Id
    private String messageId;
    private Instant processedAt;
    // constructor, getters
}

// Consumer
@Component
public class OrderEmailConsumer {

    private final ProcessedMessageRepository processedMessageRepository;
    private final EmailService emailService;

    @RabbitListener(queues = "order.created.email.queue")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (processedMessageRepository.existsById(event.messageId())) {
            log.info("Message {} đã xử lý trước đó, bỏ qua", event.messageId());
            return; // Idempotent - không gửi email trùng lặp
        }

        emailService.sendConfirmationEmail(event.userEmail(), event.orderId());
        processedMessageRepository.save(new ProcessedMessage(event.messageId(), Instant.now()));
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@Service
public class BestSellerCacheService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, List<Product>> redisTemplate;
    private final Object bestSellerLock = new Object(); // Lock riêng cho dữ liệu PHỔ BIẾN này

    private static final String CACHE_KEY = "products:bestseller:today";

    public List<Product> getBestSellersToday() {
        List<Product> cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            return cached; // Cache hit - trả ngay, KHÔNG vào lock
        }

        // Cache miss - vì đây là dữ liệu RẤT PHỔ BIẾN, nếu không có lock,
        // hàng nghìn request cùng lúc sẽ đồng thời query DB (Cache Stampede)
        synchronized (bestSellerLock) {
            // Double-Checked Locking: kiểm tra lại LẦN NỮA sau khi có lock
            // vì có thể request khác đã load xong trong lúc request này chờ lock
            cached = redisTemplate.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                return cached;
            }

            // CHỈ 1 THREAD DUY NHẤT thực hiện query nặng này
            List<Product> bestSellers = productRepository.findTop10BestSellersToday(); // Query phức tạp, tốn kém
            redisTemplate.opsForValue().set(CACHE_KEY, bestSellers, Duration.ofMinutes(5));
            return bestSellers;
        }
    }
}
```

**Giải thích:** Không có lock, nếu 10,000 user cùng truy cập trang chủ đúng lúc cache "bestseller" hết hạn, cả 10,000 request sẽ đồng thời chạy query `findTop10BestSellersToday()` (thường là query aggregation nặng) → có thể làm sập DB. Với lock, chỉ 1 request thực sự query DB, 9,999 request còn lại **chờ** rồi nhận kết quả đã được cache bởi request đầu tiên.

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notification-async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class PushNotificationService {

    @Async("notificationExecutor")
    public void sendNewOrderNotification(Long orderId, String userDeviceToken) {
        // Gọi FCM/APNs (Firebase Cloud Messaging/Apple Push Notification) - có thể chậm/không ổn định
        pushNotificationClient.send(userDeviceToken, "Đơn hàng #" + orderId + " đã được xác nhận!");
    }
}
```

**Giải thích vì sao KHÔNG nên dùng `@Async` cho việc trừ tiền trong ví điện tử:**

1. **Không đảm bảo tính tin cậy (reliability):** Nếu ứng dụng crash (restart, OOM, deploy mới) đúng lúc thread `@Async` đang chạy nhưng chưa hoàn tất, tác vụ trừ tiền có thể **bị mất hoàn toàn** — không có cơ chế "lưu lại và thử lại sau" như Message Queue.

2. **Exception bị "nuốt" âm thầm:** Nếu method `@Async` throw exception (VD: số dư không đủ), exception đó **KHÔNG được ném ngược về caller** (trừ khi dùng `CompletableFuture` và xử lý tường minh) — dẫn tới tình huống nguy hiểm: API trả về "thành công" cho client, nhưng thực tế giao dịch trừ tiền đã fail âm thầm phía sau.

3. **Không có Delivery Guarantee:** Message Queue (RabbitMQ/Kafka) có cơ chế persist + retry + dead-letter-queue để đảm bảo giao dịch quan trọng **chắc chắn được xử lý** (ít nhất 1 lần), trong khi `@Async` hoàn toàn không có các cơ chế bảo vệ này.

**Kết luận:** Nghiệp vụ tài chính (trừ tiền, thanh toán) **bắt buộc** phải xử lý đồng bộ trong 1 transaction rõ ràng (có thể kết hợp Pessimistic/Optimistic Locking đã học ở Module 15), hoặc nếu cần bất đồng bộ thì phải dùng Message Queue với cơ chế đảm bảo tin cậy đầy đủ (kèm Idempotent Consumer), tuyệt đối không dùng `@Async` đơn thuần.

</details>

<details>
<summary><b>Đáp án Bài 6</b></summary>

```java
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id @GeneratedValue private Long id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    @Column(columnDefinition = "TEXT")
    private String payload;
    private boolean processed = false;
    private Instant createdAt = Instant.now();
    // constructor, getters/setters
}

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional // Cả 2 lệnh save() dưới đây nằm trong CÙNG 1 transaction DB
    public Order placeOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));

        try {
            String payload = objectMapper.writeValueAsString(new OrderCreatedEvent(order.getId()));
            OutboxEvent event = new OutboxEvent("Order", order.getId().toString(), "OrderCreated", payload);
            outboxRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể serialize OrderCreatedEvent", e);
        }

        return order;
        // Nếu có lỗi xảy ra ở bất kỳ đâu trong method này -> Order VÀ OutboxEvent CÙNG rollback
        // Nếu method chạy xong -> Order VÀ OutboxEvent CÙNG được commit - không còn tình huống "nửa vời"
    }
}

@Component
public class OutboxPoller {

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pending) {
            try {
                rabbitTemplate.convertAndSend("order.exchange", "order.created", event.getPayload());
                event.setProcessed(true);
            } catch (Exception e) {
                log.warn("Publish outbox event {} thất bại, sẽ thử lại ở lần quét sau", event.getId(), e);
                // KHÔNG set processed=true -> event này vẫn còn "pending" cho lần quét kế tiếp
            }
        }
    }
}
```

**Giải thích Dual-Write Problem được giải quyết:** Nếu publish message trực tiếp trong `placeOrder()` (không qua Outbox), việc ghi `Order` (vào DB) và publish message (vào RabbitMQ) là **2 thao tác độc lập trên 2 hệ thống khác nhau** — không thể đảm bảo cả 2 cùng thành công hoặc cùng thất bại. Với Outbox Pattern, bước ghi dữ liệu nghiệp vụ (`Order`) và bước "đăng ký ý định gửi message" (`OutboxEvent`) đều nằm trong **cùng 1 transaction của CÙNG 1 database**, nên tận dụng được tính nguyên tử (atomicity) sẵn có của transaction DB. Việc publish message THẬT được tách ra thành bước riêng (`OutboxPoller`), có thể an toàn retry nhiều lần mà không ảnh hưởng tới tính đúng đắn của dữ liệu nghiệp vụ chính.

</details>

---

*File tiếp theo trong lộ trình: **Module 19 — Microservices** (Monolith vs Microservices, Service Discovery, API Gateway, Circuit Breaker/Resilience4j, Distributed Transaction/Saga Pattern, Inter-service Communication).*
