# Lời giải đầy đủ — Module 16: Spring Data & Advanced Persistence

> Nguồn đề: `24 spring data persistence/24-spring-data-persistence.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `Specification` kết hợp 3 điều kiện tùy chọn

### Đề
`category`, khoảng giá `minPrice`-`maxPrice`, tên chứa từ khóa (`LIKE`). Service method gọi kết hợp cả 3.

### Lời giải

```java
public class ProductSpecifications {

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) ->
                category == null ? null : cb.equal(root.get("category"), category);
        // Trả về null khi điều kiện KHÔNG áp dụng - Specification.and() tự động BỎ QUA điều kiện null
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) return cb.between(root.get("price"), minPrice, maxPrice);
            if (minPrice != null) return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) ->
                keyword == null ? null : cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
        // cb.lower() ở CẢ HAI vế - tìm kiếm KHÔNG PHÂN BIỆT HOA/THƯỜNG
    }
}

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, String keyword) {
        Specification<Product> spec = Specification
                .where(ProductSpecifications.hasCategory(category))
                .and(ProductSpecifications.priceBetween(minPrice, maxPrice))
                .and(ProductSpecifications.nameContains(keyword));

        return productRepository.findAll(spec);
    }
}

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}
```

### Giải thích

- **Tách mỗi điều kiện thành 1 static method riêng trong `ProductSpecifications`:** tuân thủ Single Responsibility — mỗi `Specification` chỉ biểu diễn ĐÚNG 1 tiêu chí lọc, dễ test độc lập, dễ TÁI SỬ DỤNG ở các query khác (VD dùng lại `hasCategory()` cho 1 endpoint khác không liên quan tới giá).
- **Trả về `null` khi điều kiện không áp dụng** là kỹ thuật CHUẨN của Specification API — Spring Data JPA's `Specification.and()` tự động bỏ qua predicate `null` khi build câu `WHERE` cuối cùng — tránh phải viết `if/else` lồng nhau phức tạp ở tầng gọi (Service) để quyết định "có nên thêm điều kiện này không".
- **`cb.lower()` cho tìm kiếm không phân biệt hoa/thường:** nếu chỉ dùng `cb.like(root.get("name"), "%" + keyword + "%")`, tìm "iphone" sẽ KHÔNG khớp với dữ liệu "iPhone 15" (phân biệt hoa thường theo mặc định của hầu hết collation SQL) — bọc `cb.lower()` cả 2 vế đảm bảo so khớp nhất quán.
- **`JpaSpecificationExecutor<Product>`** là interface BẮT BUỘC phải extend thêm (ngoài `JpaRepository`) để có được method `findAll(Specification)` — nếu quên, Repository sẽ KHÔNG có method này, biên dịch lỗi ngay.

---

## Bài 2 — `BaseAuditableEntity` với Spring Data JPA Auditing

### Đề
`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`. `Product` kế thừa. `AuditorAware` giả lập trả về `"admin"`.

### Lời giải

```java
@MappedSuperclass   // KHÔNG PHẢI @Entity - chỉ là class cha cung cấp field dùng chung, không tự tạo bảng riêng
@EntityListeners(AuditingEntityListener.class)   // kích hoạt cơ chế auto-fill của Spring Data JPA Auditing
public abstract class BaseAuditableEntity {

    @CreatedDate
    @Column(updatable = false)   // KHÔNG cho phép UPDATE lại giá trị này sau khi đã tạo - bảo vệ tính toàn vẹn audit
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

    // getter (KHÔNG cần setter public - các field này CHỈ nên được Spring Data JPA tự động gán)
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public String getCreatedBy() { return createdBy; }
    public String getLastModifiedBy() { return lastModifiedBy; }
}

@Entity
public class Product extends BaseAuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;

    // getter/setter...
}

@Component
public class SimpleAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // Giả lập - THỰC TẾ sẽ đọc từ SecurityContextHolder.getContext().getAuthentication().getName()
        // khi đã tích hợp Spring Security (Module 17)
        return Optional.of("admin");
    }
}

@Configuration
@EnableJpaAuditing(auditorAwareRef = "simpleAuditorAware")   // BẮT BUỘC để kích hoạt toàn bộ cơ chế Auditing
public class JpaAuditingConfig {
}
```

### Giải thích

- **`@MappedSuperclass` (không phải `@Entity`)**: đánh dấu class cha chỉ đóng vai trò **cung cấp field/mapping dùng chung** cho các Entity con kế thừa — Hibernate **KHÔNG tạo bảng riêng** cho `BaseAuditableEntity`, các field của nó được "trộn" (inline) trực tiếp vào bảng của Entity con (`products` sẽ có đủ cột `created_date`, `created_by`...).
- **`@EntityListeners(AuditingEntityListener.class)`** là "cầu nối" kích hoạt cơ chế Auditing — Hibernate gọi listener này TỰ ĐỘNG tại các sự kiện lifecycle (`@PrePersist`, `@PreUpdate`), và listener tự động điền giá trị vào các field gắn `@CreatedDate`/`@CreatedBy`/... — không cần code thủ công `product.setCreatedDate(LocalDateTime.now())` ở bất kỳ đâu trong tầng Service.
- **`@EnableJpaAuditing`** BẮT BUỘC khai báo ở 1 `@Configuration` class — nếu quên, toàn bộ annotation `@CreatedDate`/`@CreatedBy`... sẽ **BỊ BỎ QUA HOÀN TOÀN, các field này mãi mãi `null`** dù đã khai báo đúng — đây là lỗi rất phổ biến khi mới thiết lập Auditing.
- **`@Column(updatable = false)` trên `createdDate`/`createdBy`**: bảo vệ tính TOÀN VẸN của dữ liệu audit — dù có ai đó (do lỗi code) cố tình set lại giá trị này rồi save, Hibernate sẽ **BỎ QUA** không đưa cột này vào câu `UPDATE` sinh ra — đảm bảo "ngày tạo"/"người tạo" LUÔN LUÔN đúng với lần đầu tiên, không thể bị ghi đè về sau.

---

## Bài 3 — Trừ tồn kho bằng Pessimistic Locking cho Flash Sale

### Đề
Repository với `@Lock`, Service với `@Transactional`, throw exception khi hết hàng.

### Lời giải

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // sinh SQL kèm "FOR UPDATE" - khóa dòng NGAY LÚC ĐỌC
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}

@Service
public class FlashSaleService {

    private final ProductRepository productRepository;

    public FlashSaleService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void reduceStock(Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));
        // Tại đây, dòng Product ĐÃ BỊ KHÓA - mọi transaction KHÁC cố đọc-để-sửa cùng dòng này PHẢI CHỜ

        if (product.getStock() < quantity) {
            throw new OutOfStockException("Sản phẩm " + productId + " đã hết hàng. Còn: " + product.getStock());
        }

        product.setStock(product.getStock() - quantity);
        // Dirty checking tự động UPDATE khi transaction commit - lock TỰ ĐỘNG giải phóng ngay sau COMMIT
    }
}

class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) { super(message); }
}
```

### Giải thích

- **`@Lock(LockModeType.PESSIMISTIC_WRITE)`** là cách khai báo Pessimistic Locking THUẦN JPA (tương đương trực tiếp `SELECT ... FOR UPDATE` đã viết bằng SQL thuần ở Module 10, Bài 4) — Hibernate tự động thêm mệnh đề khóa vào câu `SELECT` sinh ra.
- **Toàn bộ luồng PHẢI nằm trong CÙNG 1 transaction (`@Transactional`)** — lock chỉ có tác dụng và tự động giải phóng đúng theo vòng đời transaction; nếu `findByIdForUpdate` và `product.setStock(...)` VÔ TÌNH chạy ở 2 transaction khác nhau (VD gọi qua 2 method riêng không transaction bao trùm), lock sẽ bị giải phóng NGAY sau lần đọc đầu tiên, mất hết tác dụng bảo vệ.
- **Phù hợp cho Flash Sale** (đúng ngữ cảnh đề bài) vì đặc trưng "hot row" — RẤT NHIỀU request cùng tranh giành **1 SỐ ÍT sản phẩm cụ thể** trong thời gian ngắn — Pessimistic Locking đảm bảo tuần tự hóa chính xác các thao tác trừ kho trên dòng đó, tránh oversold TUYỆT ĐỐI, đánh đổi bằng việc các request phải XẾP HÀNG chờ nhau (throughput giảm nhưng chấp nhận được vì đây là tài nguyên có giới hạn cứng).

---

## Bài 4 — Trừ tồn kho bằng Optimistic Locking + Retry

### Đề
`@Version`, Service method, wrapper có retry logic (vòng lặp `for`) khi gặp `OptimisticLockException`.

### Lời giải

```java
@Entity
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int stock;

    @Version
    private Long version;

    // getter/setter...
}

@Service
public class FlashSaleOptimisticService {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleOptimisticService.class);
    private static final int MAX_RETRY = 3;

    private final ProductRepository productRepository;

    public FlashSaleOptimisticService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void reduceStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));

        if (product.getStock() < quantity) {
            throw new OutOfStockException("Sản phẩm " + productId + " đã hết hàng. Còn: " + product.getStock());
        }

        product.setStock(product.getStock() - quantity);
        // Khi commit: UPDATE ... SET stock=?, version=version+1 WHERE id=? AND version=?
        // Nếu version không khớp (đã bị transaction khác đổi) -> ObjectOptimisticLockingFailureException
    }

    // Wrapper KHÔNG có @Transactional - GỌI RA method reduceStock() (CÓ @Transactional) qua self-injection/proxy khác
    // để mỗi lần retry là 1 TRANSACTION MỚI HOÀN TOÀN (đọc lại version MỚI NHẤT từ DB)
    public void reduceStockWithRetry(Long productId, int quantity) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                reduceStock(productId, quantity);
                return;   // thành công - thoát ngay
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt >= MAX_RETRY) {
                    throw new IllegalStateException(
                        "Không thể trừ tồn kho sau " + MAX_RETRY + " lần thử - hệ thống đang quá tải", ex);
                }
                log.warn("Optimistic lock conflict cho product {} - thử lại lần {}/{}", productId, attempt, MAX_RETRY);
                // Có thể thêm sleep ngắn/backoff trước khi retry để giảm tranh chấp tiếp diễn
            }
        }
    }
}
```

### Giải thích

- **Quan trọng: `reduceStockWithRetry()` KHÔNG được gắn `@Transactional`** — nếu gắn, TOÀN BỘ vòng lặp retry sẽ nằm trong **CÙNG 1 TRANSACTION DUY NHẤT**, và transaction đó **ĐÃ BỊ ĐÁNH DẤU rollback-only** ngay sau lần thất bại ĐẦU TIÊN (`ObjectOptimisticLockingFailureException` là 1 dạng exception khiến Spring đánh dấu transaction hiện tại phải rollback) — các lần retry tiếp theo trong CÙNG transaction đó SẼ THẤT BẠI NGAY LẬP TỨC với `UnexpectedRollbackException`, không có cơ hội thử lại thật sự.
- **Mỗi lần gọi `reduceStock()` (method CÓ `@Transactional`) từ vòng lặp `while` phải là 1 TRANSACTION HOÀN TOÀN MỚI** — điều này đọc lại `version` **MỚI NHẤT** từ DB ở đầu mỗi lần thử, cho phép lần retry sau có cơ hội THÀNH CÔNG khi tranh chấp đã qua đi. (Lưu ý kỹ thuật: nếu 2 method này nằm CÙNG 1 class, self-invocation `this.reduceStock()` sẽ **BỎ QUA proxy AOP của Spring**, `@Transactional` sẽ KHÔNG có tác dụng — trong thực tế cần tách 2 method ra 2 class/bean riêng, hoặc dùng `AopContext.currentProxy()`, để đảm bảo lệnh gọi đi qua đúng proxy).
- **So sánh với Bài 3 (Pessimistic):** Optimistic + Retry phù hợp hơn khi kỳ vọng xung đột **không quá dày đặc** — đa số lần thử đầu tiên đã thành công, chỉ tốn thêm round-trip DB cho SỐ ÍT trường hợp thực sự xung đột — ngược lại Pessimistic phù hợp khi BIẾT CHẮC tranh chấp xảy ra CỰC KỲ DÀY ĐẶC (VD chỉ còn 1 sản phẩm cuối, hàng nghìn người cùng bấm mua) — nguyên tắc lựa chọn đã tổng kết đầy đủ ở Module 12, Bài 6.

---

## Bài 5 — Flyway Migration `V1`/`V2`

### Đề
`V1__create_products_table.sql`, `V2__add_category_index.sql`. Vì sao không gộp vào V1 nếu V1 đã chạy ở production.

### Lời giải

```sql
-- V1__create_products_table.sql
CREATE TABLE products (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)  NOT NULL,
    price      NUMERIC(12,2) NOT NULL,
    category   VARCHAR(50)   NOT NULL,
    stock      INT           NOT NULL DEFAULT 0,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
```

```sql
-- V2__add_category_index.sql
CREATE INDEX idx_products_category ON products(category);
```

### Giải thích — vì sao KHÔNG được gộp chung vào V1 nếu V1 đã chạy ở production

- **Cơ chế cốt lõi của Flyway: mỗi file migration chỉ được áp dụng (apply) ĐÚNG 1 LẦN DUY NHẤT, và Flyway lưu lại CHECKSUM (mã băm nội dung file) của từng migration đã chạy** trong bảng nội bộ `flyway_schema_history` — nếu **SỬA LẠI** nội dung của `V1__create_products_table.sql` **SAU KHI nó đã chạy ở production** (VD thêm `CREATE INDEX` trực tiếp vào file V1), lần deploy tiếp theo Flyway sẽ **TÍNH LẠI CHECKSUM** của file V1 hiện tại, phát hiện **KHÔNG KHỚP** với checksum đã lưu từ lần chạy trước → Flyway **TỪ CHỐI KHỞI ĐỘNG** ứng dụng, ném lỗi `FlywayValidateException` ("Migration checksum mismatch").
- **Nguyên tắc bất biến (immutability) của migration đã APPLY:** 1 khi file migration đã chạy ở BẤT KỲ môi trường nào (đặc biệt production), nó phải được coi là **BẤT BIẾN VĨNH VIỄN** — mọi thay đổi về sau (thêm cột, thêm index, sửa kiểu dữ liệu...) PHẢI đi qua 1 file migration **MỚI** với số version CAO HƠN (`V2`, `V3`...) — đây là nguyên tắc y hệt với "KHÔNG amend/force-push 1 commit Git đã được người khác pull về" (đã học ở phần Git) — lịch sử migration đã "công khai" (đã chạy ở môi trường khác) không nên bị viết lại.
- **Hậu quả nếu cố tình sửa V1 rồi force Flyway bỏ qua kiểm tra (`flyway repair` không đúng cách):** môi trường production (đã chạy V1 CŨ, KHÔNG có index) và môi trường mới deploy từ đầu (chạy V1 ĐÃ SỬA, CÓ index) sẽ có **SCHEMA THỰC TẾ KHÁC NHAU** dù cùng "V1" — phá vỡ hoàn toàn mục đích cốt lõi của migration tool: đảm bảo MỌI môi trường (dev, staging, production) có CÙNG MỘT lịch sử thay đổi schema, THEO ĐÚNG THỨ TỰ, có thể tái tạo lại (reproducible) một cách đáng tin cậy.

---

## Bài 6 — `@NamedEntityGraph` + `@EntityGraph` tránh N+1

### Đề
`Order.withUser`, Repository method lấy order theo status không N+1. Vì sao tốt hơn gọi `order.getUser().getFullName()` trực tiếp sau `findAll()`.

### Lời giải

```java
@Entity
@NamedEntityGraph(
        name = "Order.withUser",
        attributeNodes = @NamedAttributeNode("user")
        // Khai báo: khi entity graph này được áp dụng, LOAD LUÔN quan hệ "user" cùng lúc (không lazy nữa)
)
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)   // vẫn khai báo LAZY mặc định - EntityGraph sẽ "ghi đè" CÓ CHỦ ĐÍCH khi cần
    @JoinColumn(name = "user_id")
    private User user;

    // getter/setter...
}

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(value = "Order.withUser", type = EntityGraph.EntityGraphType.LOAD)
    List<Order> findByStatus(String status);
}
```

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<String> getUserNamesByOrderStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(order -> order.getUser().getFullName())   // AN TOÀN - user ĐÃ ĐƯỢC LOAD SẴN, không lazy-load lại
                .toList();
    }
}
```

### Giải thích — vì sao tốt hơn gọi trực tiếp sau `findAll()`

- **Nếu KHÔNG dùng `@EntityGraph`, gọi `order.getUser().getFullName()` trong vòng lặp sau `findByStatus()` thông thường** sẽ gây ra **CHÍNH XÁC N+1 Query Problem** đã học ở Module 12, Bài 2: 1 query lấy N order (1 query), sau đó **MỖI order** kích hoạt 1 query RIÊNG để lazy-load `user` tương ứng (N query) → tổng `1 + N` query — với 100 order cùng status, đó là **101 query** thay vì lý tưởng chỉ 1.
- **`@EntityGraph(value = "Order.withUser", type = LOAD)`** khiến Hibernate sinh **ĐÚNG 1 CÂU `SELECT ... LEFT JOIN ...`** duy nhất, lấy cả `Order` VÀ `User` liên quan trong 1 lần round-trip DB — về hiệu quả HOÀN TOÀN TƯƠNG ĐƯƠNG với `JOIN FETCH` viết tay bằng JPQL (Module 12, Bài 2, cách 1), chỉ khác cách khai báo (declarative qua annotation thay vì viết JPQL thủ công).
- **`type = EntityGraph.EntityGraphType.LOAD` (so với `FETCH`):** `LOAD` nghĩa là "các attribute liệt kê trong graph sẽ EAGER, các attribute KHÁC giữ nguyên fetch type đã khai báo trên Entity (LAZY/EAGER như bình thường)"; `FETCH` nghĩa là "CHỈ các attribute trong graph là EAGER, MỌI attribute khác (dù đã khai báo EAGER trên Entity) đều bị ép về LAZY" — với bài này, `LOAD` là lựa chọn AN TOÀN HỢP LÝ hơn (không ảnh hưởng bất ngờ tới các quan hệ khác không liên quan tới `user`).
- **Ưu điểm khai báo qua `@NamedEntityGraph` + `@EntityGraph` so với viết `JOIN FETCH` thủ công bằng `@Query`:** tái sử dụng được graph "Order.withUser" ở NHIỀU method Repository khác nhau (VD `findByStatus`, `findByUserId`...) mà không cần viết lại JPQL mỗi lần, tách biệt rõ "graph load nào cần" (khai báo ngay tại Entity) khỏi "query logic" (ở Repository).

---

*Đây là lời giải cho toàn bộ Phần B của Module 24. Tiếp theo: Module 17 — Spring Security.*
