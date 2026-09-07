# Module 15 — Spring Data & Persistence nâng cao

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Module 11 đã cho bạn nền tảng JPA/Hibernate; module này đưa bạn tới mức **thực chiến production**. Query động phức tạp (Specification/Querydsl), audit tự động (ai tạo/sửa record lúc nào), xử lý **race condition** khi nhiều request cùng sửa 1 dòng dữ liệu (Locking), và quản lý schema database qua version control (Flyway/Liquibase) — đây đều là những kỹ năng phân biệt rõ ràng giữa "biết dùng Spring Data JPA" và "dùng Spring Data JPA đúng cách trong hệ thống thật có nhiều người dùng đồng thời".

---

## Mục lục

1. [Spring Data JPA Repository — các tầng trừu tượng](#1-spring-data-jpa-repository)
2. [Derived Query Methods nâng cao](#2-derived-query-methods-nâng-cao)
3. [Specification API — query động linh hoạt](#3-specification-api)
4. [Querydsl — thay thế Specification hiện đại hơn](#4-querydsl)
5. [Auditing — tự động ghi lại ai/khi nào tạo-sửa record](#5-auditing)
6. [Optimistic Locking vs Pessimistic Locking](#6-optimistic-locking-vs-pessimistic-locking)
7. [Database Migration: Flyway & Liquibase](#7-database-migration)
8. [Projection — tối ưu query chỉ lấy field cần thiết](#8-projection)
9. [⚠️ Các bẫy hay gặp](#9-các-bẫy-hay-gặp)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Spring Data JPA Repository

### Hệ thống phân cấp Interface

```
Repository<T, ID>                    (marker interface, không có method nào)
        ▲
        │
CrudRepository<T, ID>                (save, findById, findAll, delete, count...)
        ▲
        │
PagingAndSortingRepository<T, ID>    (+ findAll(Pageable), findAll(Sort))
        ▲
        │
JpaRepository<T, ID>                 (+ flush, saveAndFlush, deleteAllInBatch,
                                         getReferenceById...)
```

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Kế thừa sẵn: save(), findById(), findAll(), deleteById(), count()...
    // KHÔNG cần viết implementation - Spring Data JPA tự sinh code lúc runtime
    // bằng Dynamic Proxy (giống cơ chế AOP Proxy đã học ở Module 12)
}
```

⚠️ **`getReferenceById()` vs `findById()` — khác biệt quan trọng:**

```java
// findById() -> Query DB NGAY LẬP TỨC, trả về Optional<T>
Optional<User> user = userRepository.findById(1L); // SELECT * FROM users WHERE id = 1

// getReferenceById() -> KHÔNG query DB ngay, trả về Proxy object (Lazy)
User userRef = userRepository.getReferenceById(1L); // Chưa có SELECT nào chạy!
// Chỉ khi truy cập field (VD: userRef.getFullName()) mới thực sự query DB
// -> Hữu ích khi chỉ cần set foreign key mà KHÔNG cần đọc dữ liệu user thật:

Order order = new Order();
order.setUser(userRepository.getReferenceById(1L)); // Không cần load User đầy đủ, chỉ cần set FK
orderRepository.save(order); // Tiết kiệm 1 query SELECT không cần thiết
```

### saveAndFlush() vs save()

```java
userRepository.save(user);       // Chỉ đưa vào Persistence Context, CHƯA chắc đã chạy SQL ngay
                                  // (Hibernate có thể trì hoãn tới cuối transaction - gọi là "flush")

userRepository.saveAndFlush(user); // Ép Hibernate chạy SQL NGAY LẬP TỨC
                                    // Hữu ích khi cần lấy giá trị auto-generated (VD: id)
                                    // để dùng ngay trong cùng transaction cho logic tiếp theo
```

---

## 2. Derived Query Methods nâng cao

Đã học cơ bản ở Module 11 — đây là các pattern nâng cao hơn.

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Kết hợp nhiều điều kiện với And/Or
    List<Order> findByStatusAndTotalAmountGreaterThan(OrderStatus status, BigDecimal amount);

    // Between - khoảng giá trị
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // In - danh sách giá trị
    List<Order> findByStatusIn(List<OrderStatus> statuses);

    // Not / IsNull / IsNotNull
    List<Order> findByCancelledAtIsNull();       // Đơn hàng chưa bị hủy
    List<Order> findByUserIsNotNull();

    // OrderBy trong tên method (thay vì dùng Sort/Pageable riêng)
    List<Order> findTop5ByStatusOrderByCreatedAtDesc(OrderStatus status);

    // Distinct
    List<Order> findDistinctByUserId(Long userId);

    // Đếm / kiểm tra tồn tại / xóa (không cần load Entity trước)
    long countByStatus(OrderStatus status);
    boolean existsByUserIdAndStatus(Long userId, OrderStatus status);
    void deleteByStatus(OrderStatus status); // ⚠️ Xem bẫy ở mục 9

    // Query lồng qua quan hệ (Nested Property)
    List<Order> findByUser_Email(String email); // JOIN qua field "user" -> field "email"
    // Tương đương JPQL: SELECT o FROM Order o WHERE o.user.email = :email
}
```

⚠️ **Giới hạn của Derived Query Method:** Tên method sẽ **rất dài và khó đọc** khi có quá nhiều điều kiện (VD: `findByStatusAndUserIdAndCreatedAtBetweenAndTotalAmountGreaterThanOrderByCreatedAtDesc`). Đây là dấu hiệu nên chuyển sang `@Query` (JPQL) hoặc `Specification`/`Querydsl`.

---

## 3. Specification API

**Specification** là cách xây dựng query **động** (dynamic query) bằng code Java, dựa trên **Criteria API** (đã giới thiệu sơ lược ở Module 11) — cực kỳ hữu ích khi API có **nhiều tham số filter tùy chọn** (search form với 10+ field, người dùng có thể điền bất kỳ tổ hợp nào).

### Setup

```java
public interface OrderRepository extends JpaRepository<Order, Long>,
                                          JpaSpecificationExecutor<Order> { // Bắt buộc thêm interface này
}
```

### Viết Specification

```java
public class OrderSpecifications {

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
            // Trả về null nghĩa là "bỏ qua điều kiện này" -> Spring tự loại nó khỏi WHERE clause
    }

    public static Specification<Order> hasMinAmount(BigDecimal minAmount) {
        return (root, query, cb) ->
            minAmount == null ? null : cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount);
    }

    public static Specification<Order> createdBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from.atStartOfDay(), to.atTime(23, 59, 59));
            }
            return from != null
                ? cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay())
                : cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(23, 59, 59));
        };
    }

    // Specification qua quan hệ (JOIN)
    public static Specification<Order> belongsToUserEmail(String email) {
        return (root, query, cb) -> {
            if (email == null) return null;
            Join<Order, User> userJoin = root.join("user"); // JOIN order.user
            return cb.equal(userJoin.get("email"), email);
        };
    }
}
```

### Sử dụng trong Service — kết hợp nhiều điều kiện linh hoạt

```java
@Service
public class OrderSearchService {

    private final OrderRepository orderRepository;

    public Page<Order> search(OrderSearchCriteria criteria, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecifications.hasStatus(criteria.status()))
                .and(OrderSpecifications.hasMinAmount(criteria.minAmount()))
                .and(OrderSpecifications.createdBetween(criteria.fromDate(), criteria.toDate()))
                .and(OrderSpecifications.belongsToUserEmail(criteria.userEmail()));

        // Chỉ các điều kiện có giá trị (không null) mới được đưa vào WHERE clause
        // -> 1 method duy nhất xử lý được MỌI tổ hợp filter mà client gửi lên
        return orderRepository.findAll(spec, pageable);
    }
}
```

> **So sánh với Derived Query Method:** Nếu dùng Derived Query Method, bạn phải viết **hàng chục method** cho từng tổ hợp filter khác nhau (`findByStatus`, `findByStatusAndMinAmount`, `findByStatusAndMinAmountAndUserEmail`...). Specification giải quyết vấn đề này bằng **compose động** — chỉ cần định nghĩa từng điều kiện 1 lần, sau đó ghép tùy ý.

---

## 4. Querydsl

**Querydsl** là thư viện bên thứ 3 (không thuộc chuẩn JPA), được nhiều dự án production **ưa chuộng hơn Specification** vì cú pháp gần giống SQL/Java thuần, dễ đọc, **type-safe hoàn toàn** (lỗi sai tên field bị bắt lúc compile, không phải runtime).

### Setup (cần plugin generate Q-classes)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
</dependency>
<!-- + annotation processor để tự sinh QOrder, QUser... từ Entity -->
```

Sau khi build, Querydsl tự sinh ra các class `Q{TênEntity}` (VD: `QOrder`, `QUser`) chứa metadata field dạng type-safe.

### So sánh Specification vs Querydsl

```java
// Specification - dùng String cho tên field ("status", "totalAmount")
// -> Gõ sai chính tả sẽ KHÔNG báo lỗi lúc compile, chỉ lỗi khi CHẠY THẬT
Specification<Order> spec = (root, query, cb) -> cb.equal(root.get("staus"), status); // typo "staus" - compile OK, runtime lỗi!

// Querydsl - dùng Q-class, IDE autocomplete, sai tên field -> LỖI COMPILE NGAY
QOrder order = QOrder.order;
BooleanExpression predicate = order.status.eq(status); // order.staus -> IDE báo đỏ ngay, không compile được
```

```java
public interface OrderRepository extends JpaRepository<Order, Long>,
                                          QuerydslPredicateExecutor<Order> {
}

@Service
public class OrderSearchService {
    public Page<Order> search(OrderSearchCriteria criteria, Pageable pageable) {
        QOrder order = QOrder.order;
        BooleanBuilder builder = new BooleanBuilder();

        if (criteria.status() != null) {
            builder.and(order.status.eq(criteria.status()));
        }
        if (criteria.minAmount() != null) {
            builder.and(order.totalAmount.goe(criteria.minAmount())); // goe = greater or equal
        }
        if (criteria.userEmail() != null) {
            builder.and(order.user.email.eq(criteria.userEmail())); // Truy cập quan hệ TRỰC TIẾP, không cần join() thủ công
        }

        return orderRepository.findAll(builder, pageable);
    }
}
```

| | Specification | Querydsl |
|---|---|---|
| Thuộc chuẩn nào | JPA Criteria API (chuẩn JPA) | Thư viện bên thứ 3 (không thuộc JPA) |
| Type-safe | ❌ Dùng String cho tên field | ✅ Q-class autocomplete, lỗi compile |
| Độ dễ đọc | Trung bình (verbose) | Cao hơn, gần cú pháp SQL tự nhiên |
| Cần build step riêng | Không | **Có** (annotation processor sinh Q-class) |
| Phổ biến trong thực tế | Phổ biến | **Rất phổ biến trong dự án lớn**, dù cần setup thêm |

> **Khuyến nghị thực chiến:** Với dự án nhỏ/vừa, Specification đủ dùng. Với dự án lớn có nhiều query động phức tạp, **Querydsl thường được ưu tiên** nhờ type-safety và khả năng đọc code tốt hơn — đáng để đầu tư thời gian setup ban đầu.

---

## 5. Auditing

**Auditing** tự động ghi lại **ai** đã tạo/sửa record và **khi nào** — không cần set thủ công ở từng Service.

### Bật Auditing

```java
@SpringBootApplication
@EnableJpaAuditing // Bật tính năng Auditing toàn ứng dụng
public class MyApp { }
```

### Base Entity dùng chung cho Auditing

```java
@MappedSuperclass // Không tạo bảng riêng, các field sẽ được "kế thừa" vào Entity con
@EntityListeners(AuditingEntityListener.class) // Lắng nghe sự kiện persist/update để tự động điền field
public abstract class BaseAuditableEntity {

    @CreatedDate
    @Column(updatable = false) // Không cho phép update sau khi đã tạo
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    // getters (không cần setters - Hibernate tự set qua Auditing)
}

@Entity
public class Order extends BaseAuditableEntity {
    @Id @GeneratedValue private Long id;
    private BigDecimal totalAmount;
    // Tự động có sẵn createdAt, updatedAt, createdBy, updatedBy - không cần khai báo lại!
}
```

### Cung cấp "ai đang đăng nhập" cho @CreatedBy/@LastModifiedBy

```java
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM"); // Giá trị mặc định khi không có user đăng nhập (VD: batch job)
        }
        return Optional.of(authentication.getName()); // Lấy username từ Spring Security context
    }
}

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware") // Trỏ tới Bean vừa tạo
public class MyApp { }
```

> **Liên hệ thực tế:** Auditing cực kỳ hữu ích cho **compliance/audit trail** trong hệ thống tài chính, y tế — trả lời câu hỏi "ai đã sửa record này lúc nào" mà không cần code thủ công ở mọi Service method.

---

## 6. Optimistic Locking vs Pessimistic Locking

Đây là kiến thức **cực kỳ quan trọng** để xử lý **Race Condition** — khi nhiều request cùng đọc/sửa 1 dòng dữ liệu đồng thời (đã giới thiệu khái niệm ở Module 05 Thread và Module 10 Isolation Level, giờ áp dụng cụ thể vào JPA).

### Vấn đề: Lost Update

```
Thời điểm  Request A                          Request B
   T1      Đọc Product (stock=10)
   T2                                          Đọc Product (stock=10)
   T3      Trừ 3 -> set stock=7, save()
   T4                                          Trừ 5 -> set stock=5, save()
                                                (dựa trên giá trị ĐỌC LÚC T2, không biết A đã sửa)

Kết quả: stock cuối cùng = 5 (của B), NHƯNG đáng lẽ phải là 10 - 3 - 5 = 2!
-> Thay đổi của Request A bị "mất" (Lost Update) - lỗi rất nguy hiểm với tồn kho/số dư tài khoản!
```

### 6.1. Optimistic Locking — "lạc quan", kiểm tra khi save

**Nguyên lý:** Thêm 1 cột `@Version` — mỗi lần update, Hibernate tự động kiểm tra version chưa đổi mới cho update, đồng thời tăng version lên 1.

```java
@Entity
public class Product {
    @Id @GeneratedValue private Long id;
    private String name;
    private int stock;

    @Version // Cột version - Hibernate tự động quản lý, KHÔNG set thủ công
    private Long version;
}
```

```sql
-- Hibernate tự sinh câu UPDATE kèm điều kiện version:
UPDATE products SET stock = 7, version = 2 WHERE id = 1 AND version = 1;
-- Nếu record đã bị request khác update trước đó (version đã đổi thành 2),
-- câu UPDATE này ảnh hưởng 0 dòng -> Hibernate throw OptimisticLockException
```

```java
@Service
public class ProductService {

    @Transactional
    public void reduceStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.setStock(product.getStock() - quantity);
        // Khi transaction commit, Hibernate tự check version
        // Nếu version không khớp (đã bị sửa bởi transaction khác) -> OptimisticLockException
    }
}

// Xử lý exception và RETRY ở tầng gọi
@Retryable(retryFor = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
public void reduceStockWithRetry(Long productId, int quantity) {
    productService.reduceStock(productId, quantity);
}
```

**Khi nào dùng Optimistic Locking:**
- Xung đột **hiếm xảy ra** (ít khả năng 2 request cùng sửa 1 record cùng lúc)
- Ưu tiên **hiệu năng cao** — không khóa database, cho phép nhiều transaction đọc/ghi song song
- Ví dụ: Cập nhật thông tin cá nhân user, cập nhật bài viết blog

### 6.2. Pessimistic Locking — "bi quan", khóa ngay khi đọc

**Nguyên lý:** Khóa record ngay khi đọc (dùng `SELECT ... FOR UPDATE` của SQL), các transaction khác phải **chờ** tới khi transaction hiện tại commit/rollback mới được đọc/sửa.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // Tương đương SELECT ... FOR UPDATE
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}

@Service
public class ProductService {

    @Transactional
    public void reduceStock(Long productId, int quantity) {
        // Request khác gọi đồng thời SẼ BỊ CHẶN (block) tại dòng này
        // cho tới khi transaction này commit/rollback
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow();

        if (product.getStock() < quantity) {
            throw new InsufficientStockException();
        }
        product.setStock(product.getStock() - quantity);
        // Không có race condition nào xảy ra được - đảm bảo tuyệt đối tính nhất quán
    }
}
```

**Khi nào dùng Pessimistic Locking:**
- Xung đột xảy ra **thường xuyên** (VD: flash sale, nhiều người cùng mua 1 sản phẩm số lượng giới hạn cùng lúc)
- Nghiệp vụ **bắt buộc đúng tuyệt đối**, chấp nhận đánh đổi hiệu năng (throughput thấp hơn do phải chờ khóa)
- Ví dụ: Trừ tồn kho khi flash sale, chuyển khoản ngân hàng

### So sánh trực quan

| | Optimistic Locking | Pessimistic Locking |
|---|---|---|
| Cơ chế | Kiểm tra version khi save, KHÔNG khóa DB | Khóa record ngay khi đọc (`FOR UPDATE`) |
| Hiệu năng | Cao (không blocking) | Thấp hơn (request khác phải chờ) |
| Xử lý xung đột | Throw exception, cần retry ở tầng gọi | Tự động tuần tự hóa, không cần retry |
| Rủi ro | Có thể fail nhiều lần nếu xung đột thường xuyên (nhiều lần retry) | Rủi ro **Deadlock** nếu khóa nhiều resource theo thứ tự khác nhau |
| Phù hợp | Xung đột hiếm, ưu tiên throughput | Xung đột thường xuyên, ưu tiên tính đúng đắn tuyệt đối |

> **Liên hệ:** Chủ đề "trừ tồn kho không bị oversell khi flash sale" — 1 bài toán kinh điển trong phỏng vấn — chính là ứng dụng trực tiếp của Pessimistic Locking (hoặc các giải pháp nâng cao hơn như Redis distributed lock, sẽ gặp ở Module Microservices/System Design).

---

## 7. Database Migration

**Vấn đề:** Khi team nhiều người cùng làm việc, database schema thay đổi liên tục (thêm cột, tạo bảng mới...) — nếu chỉ dựa vào `ddl-auto: update` của Hibernate (đã cảnh báo ở Module 13 là nguy hiểm ở production), sẽ **không có lịch sử thay đổi schema**, không thể rollback, không đồng bộ được giữa các môi trường (dev/staging/prod).

**Giải pháp:** Quản lý schema như quản lý code — mỗi thay đổi là 1 file **migration script** có version, được version control (Git) cùng source code.

### Flyway

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_orders_table.sql
├── V3__add_phone_column_to_users.sql
└── V4__create_index_on_orders_status.sql
```

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

```sql
-- V3__add_phone_column_to_users.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate # QUAN TRỌNG: để Flyway quản lý schema, Hibernate chỉ validate, KHÔNG tự sửa
```

**Cách hoạt động:** Khi ứng dụng khởi động, Flyway tự động kiểm tra bảng `flyway_schema_history` (bảng nội bộ Flyway tự tạo để track version đã chạy), so sánh với các file migration có sẵn, và **chỉ chạy các file MỚI chưa từng chạy** — đảm bảo mỗi migration chỉ chạy đúng 1 lần, theo đúng thứ tự version.

⚠️ **Quy tắc bất di bất dịch với Flyway:** **KHÔNG BAO GIỜ sửa lại 1 file migration đã chạy** (đã có trong `flyway_schema_history` ở bất kỳ môi trường nào) — vì Flyway tính checksum (hash) của mỗi file, phát hiện file bị sửa sau khi đã chạy sẽ báo lỗi (`checksum mismatch`). Muốn sửa, phải tạo **file migration MỚI** (VD: `V5__fix_phone_column.sql`).

### Liquibase (thay thế Flyway — dùng XML/YAML/JSON thay vì SQL thuần)

```xml
<!-- changelog.xml -->
<changeSet id="1" author="pho">
    <createTable tableName="users">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="full_name" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

| | Flyway | Liquibase |
|---|---|---|
| Cú pháp | SQL thuần (dễ học, đúng SQL chuẩn dialect) | XML/YAML/JSON/SQL (trừu tượng hóa, database-agnostic hơn) |
| Độ phức tạp | Đơn giản, dễ tiếp cận | Phức tạp hơn nhưng linh hoạt hơn (hỗ trợ rollback tự động tốt hơn) |
| Phổ biến | **Rất phổ biến**, đặc biệt dự án Spring Boot | Phổ biến trong doanh nghiệp lớn, đa database |
| Khuyến nghị người mới | ⭐⭐⭐ Nên bắt đầu với Flyway (đơn giản, SQL quen thuộc) | ⭐⭐ Cân nhắc khi cần tính năng nâng cao |

---

## 8. Projection

Đã giới thiệu DTO Projection ở Module 11 để giải quyết N+1 — đây là các cách projection khác trong Spring Data JPA.

### Interface-based Projection (đơn giản nhất)

```java
public interface OrderSummary { // Interface, KHÔNG phải class
    Long getId();
    BigDecimal getTotalAmount();
    String getUserFullName(); // Spring Data JPA tự map qua "user.fullName"
}

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<OrderSummary> findByStatus(OrderStatus status); // Spring tự tạo Proxy implement interface này
}
```

### Class-based Projection (DTO record — type-safe hơn, khuyến nghị)

```java
public record OrderSummaryDto(Long id, BigDecimal totalAmount, String userFullName) {}

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT new com.example.dto.OrderSummaryDto(o.id, o.totalAmount, o.user.fullName) " +
           "FROM Order o WHERE o.status = :status")
    List<OrderSummaryDto> findSummaryByStatus(@Param("status") OrderStatus status);
}
```

### Dynamic Projection (chọn kiểu trả về linh hoạt cùng 1 method)

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    <T> List<T> findByStatus(OrderStatus status, Class<T> type);
}

// Gọi linh hoạt:
List<OrderSummary> summaries = orderRepository.findByStatus(OrderStatus.PENDING, OrderSummary.class);
List<Order> fullOrders = orderRepository.findByStatus(OrderStatus.PENDING, Order.class);
```

---

## 9. ⚠️ Các bẫy hay gặp

1. **`deleteByX()` không có `@Transactional`** — Derived Query delete method cần `@Transactional` bao quanh (thường ở tầng Service), nếu không sẽ throw `InvalidDataAccessApiUsageException`.

2. **Dùng `String` cho tên field trong Specification, gõ sai chính tả** → lỗi chỉ phát hiện lúc runtime, không compile-time (đây là lý do Querydsl được ưu tiên trong dự án lớn).

3. **Sửa lại file Flyway migration đã chạy ở production** → gây `checksum mismatch`, ứng dụng không khởi động được ở các môi trường khác.

4. **Dùng Optimistic Locking cho nghiệp vụ xung đột thường xuyên** (VD: flash sale) mà không có cơ chế retry → user gặp lỗi liên tục, trải nghiệm tệ.

5. **Dùng Pessimistic Locking tràn lan** cho mọi nghiệp vụ → giảm throughput nghiêm trọng, có nguy cơ Deadlock nếu khóa nhiều resource không theo thứ tự nhất quán.

6. **Quên set `ddl-auto: validate`** khi đã dùng Flyway/Liquibase → Hibernate và migration tool "giẫm chân nhau", gây xung đột schema khó debug.

7. **`@Version` field bị set thủ công trong code** (VD: `product.setVersion(1L)`) → phá vỡ cơ chế Optimistic Locking, Hibernate không kiểm soát đúng được version thật.

8. **Interface-based Projection với quan hệ sâu** (`getUser().getAddress().getCity()`) có thể vẫn gây N+1 nếu không cẩn thận — nên ưu tiên Class-based Projection với JPQL tường minh.

9. **Không đặt Index cho cột thường dùng để `WHERE`/`ORDER BY`** trong Specification/Querydsl query động → query chậm dần khi dữ liệu lớn (liên hệ Module 10 — Index).

10. **Auditing không set `AuditorAware` đúng cách** → field `createdBy`/`updatedBy` luôn là `null` hoặc giá trị sai, audit trail vô nghĩa.

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| `getReferenceById()` | Trả Proxy, không query ngay — dùng khi chỉ cần set FK |
| Specification | Query động dùng Criteria API — String field name, không type-safe |
| Querydsl | Query động type-safe hơn (Q-class), cần build step riêng |
| Auditing | `@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`/`@LastModifiedBy` + `@EnableJpaAuditing` |
| Optimistic Locking | `@Version`, kiểm tra khi save, throw exception nếu conflict — phù hợp xung đột hiếm |
| Pessimistic Locking | `SELECT ... FOR UPDATE`, khóa ngay khi đọc — phù hợp xung đột thường xuyên (flash sale) |
| Flyway/Liquibase | Quản lý schema như code, version control — KHÔNG BAO GIỜ sửa migration đã chạy |
| ddl-auto khi dùng migration tool | Luôn `validate`, để Flyway/Liquibase quản lý schema thật |
| Projection | Interface-based (đơn giản) hoặc Class-based DTO (type-safe hơn, khuyến nghị) |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. `getReferenceById()` luôn query database ngay lập tức giống `findById()`.
2. Specification trả về `null` cho 1 điều kiện có nghĩa là điều kiện đó sẽ bị bỏ qua khỏi WHERE clause.
3. Querydsl phát hiện lỗi gõ sai tên field ngay lúc compile, trong khi Specification chỉ phát hiện lúc runtime.
4. Optimistic Locking phù hợp hơn Pessimistic Locking cho nghiệp vụ flash sale có xung đột xảy ra liên tục.
5. Khi đã dùng Flyway quản lý schema, nên set `ddl-auto: update` để Hibernate hỗ trợ thêm.
6. Sửa lại nội dung 1 file migration Flyway đã từng chạy ở production là an toàn miễn là tên file không đổi.
7. `@Version` field nên được lập trình viên set giá trị thủ công để kiểm soát chính xác.
8. Pessimistic Locking có nguy cơ gây Deadlock nếu nhiều transaction khóa nhiều resource theo thứ tự khác nhau.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Viết 1 `Specification` kết hợp 3 điều kiện tùy chọn cho `Product`: theo `category`, khoảng giá `minPrice`-`maxPrice`, và tên chứa từ khóa (`LIKE`). Viết Service method gọi kết hợp cả 3.

**Bài 2:** Thiết kế `BaseAuditableEntity` với đầy đủ `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`, và 1 Entity `Product` kế thừa nó. Viết `AuditorAware` giả lập trả về username cố định `"admin"` (không cần Spring Security thật).

**Bài 3:** Viết đầy đủ luồng xử lý "trừ tồn kho" bằng **Pessimistic Locking** cho tình huống Flash Sale — bao gồm Repository method với `@Lock`, Service method với `@Transactional`, throw exception khi hết hàng.

**Bài 4:** Viết đầy đủ luồng xử lý tương tự Bài 3 nhưng bằng **Optimistic Locking** — bao gồm Entity có `@Version`, Service method, và 1 method wrapper có retry logic (dùng vòng lặp `for` đơn giản, không cần `@Retryable`) khi gặp `OptimisticLockException`.

**Bài 5:** Viết 2 file Flyway migration: `V1__create_products_table.sql` (tạo bảng với các cột cơ bản) và `V2__add_category_index.sql` (thêm index cho cột `category`). Giải thích vì sao không được gộp chung vào 1 file V1 duy nhất nếu V1 đã từng chạy ở production.

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** `getReferenceById()` trả về Proxy, KHÔNG query ngay — chỉ query khi thực sự truy cập field của object.
2. **Đúng.** Đây chính là cơ chế Specification dùng để loại bỏ điều kiện không cần thiết khỏi query động.
3. **Đúng.** Đây là ưu điểm cốt lõi của Querydsl — type-safe nhờ Q-class được generate, IDE báo lỗi ngay khi gõ sai.
4. **Sai.** Ngược lại — Pessimistic Locking phù hợp hơn cho xung đột thường xuyên (flash sale), vì Optimistic Locking sẽ khiến nhiều request bị fail liên tục cần retry, trải nghiệm tệ.
5. **Sai.** Khi đã dùng migration tool, phải set `ddl-auto: validate` để tránh 2 cơ chế "giẫm chân nhau" gây xung đột schema.
6. **Sai.** Tuyệt đối không được sửa — Flyway tính checksum dựa trên NỘI DUNG file, sửa nội dung sẽ gây lỗi checksum mismatch dù tên file giữ nguyên.
7. **Sai.** `@Version` phải để Hibernate tự động quản lý hoàn toàn — set thủ công sẽ phá vỡ cơ chế Optimistic Locking.
8. **Đúng.** Đây là rủi ro cố hữu của Pessimistic Locking — cần thiết kế thứ tự khóa nhất quán để tránh Deadlock.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
public class ProductSpecifications {

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) ->
            category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("price"), minPrice, maxPrice);
            }
            return minPrice != null
                ? cb.greaterThanOrEqualTo(root.get("price"), minPrice)
                : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) ->
            keyword == null ? null : cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }
}

@Service
public class ProductSearchService {
    private final ProductRepository productRepository;

    public Page<Product> search(String category, BigDecimal minPrice, BigDecimal maxPrice,
                                  String keyword, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecifications.hasCategory(category))
                .and(ProductSpecifications.priceBetween(minPrice, maxPrice))
                .and(ProductSpecifications.nameContains(keyword));
        return productRepository.findAll(spec, pageable);
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditableEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    // getters
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
}

@Entity
public class Product extends BaseAuditableEntity {
    @Id @GeneratedValue private Long id;
    private String name;
    private BigDecimal price;
    // Tự động có createdAt, updatedAt, createdBy, updatedBy
}

@Component
public class FixedAuditorAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        // Giả lập - thực tế sẽ lấy từ SecurityContextHolder
        return Optional.of("admin");
    }
}

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "fixedAuditorAware")
public class MyApp { }
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
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
    public void purchaseFlashSaleItem(Long productId, int quantity) {
        // Request khác gọi cùng productId sẽ BỊ CHẶN tại đây cho tới khi transaction này kết thúc
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStock() < quantity) {
            throw new InsufficientStockException(
                "Sản phẩm đã hết hàng: còn " + product.getStock() + ", cần " + quantity);
        }

        product.setStock(product.getStock() - quantity);
        // Dirty Checking tự động UPDATE khi commit -
        // đảm bảo tuyệt đối không bị oversell dù có 1000 request cùng lúc
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@Entity
public class Product {
    @Id @GeneratedValue private Long id;
    private String name;
    private int stock;

    @Version
    private Long version; // Hibernate tự quản lý, không set thủ công
}

@Service
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional
    public void reduceStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStock() < quantity) {
            throw new InsufficientStockException(productId);
        }
        product.setStock(product.getStock() - quantity);
        // Khi commit, nếu version đã bị transaction khác đổi -> OptimisticLockException
    }
}

@Service
public class ProductServiceWithRetry {

    private final ProductService productService;
    private static final int MAX_RETRY = 3;

    public void reduceStockWithRetry(Long productId, int quantity) {
        int attempt = 0;
        while (true) {
            try {
                productService.reduceStock(productId, quantity);
                return; // Thành công -> thoát vòng lặp
            } catch (OptimisticLockException e) {
                attempt++;
                if (attempt >= MAX_RETRY) {
                    throw new RuntimeException("Không thể xử lý sau " + MAX_RETRY + " lần thử, vui lòng thử lại sau", e);
                }
                // Có thể thêm delay nhỏ giữa các lần retry để giảm tỉ lệ xung đột tiếp theo
                try {
                    Thread.sleep(50L * attempt); // Backoff tăng dần đơn giản
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```sql
-- V1__create_products_table.sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

```sql
-- V2__add_category_index.sql
CREATE INDEX idx_products_category ON products(category);
```

**Giải thích vì sao không được gộp chung vào V1 nếu V1 đã chạy ở production:**

Flyway lưu lại **checksum (hash)** của từng file migration trong bảng `flyway_schema_history` ngay khi file đó chạy thành công lần đầu ở bất kỳ môi trường nào. Nếu sau đó bạn sửa nội dung file `V1__create_products_table.sql` (thêm câu `CREATE INDEX` vào) — dù chỉ thêm mà không xóa gì — checksum của file sẽ thay đổi. Lần deploy tiếp theo, Flyway so sánh checksum hiện tại của file với checksum đã lưu, phát hiện **không khớp** → ném lỗi `FlywayValidateException: Migration checksum mismatch`, ứng dụng **không khởi động được** ở bất kỳ môi trường nào đã từng chạy V1 trước đó.

**Nguyên tắc:** Mỗi thay đổi schema, dù nhỏ, luôn phải là 1 **file migration MỚI** với version tăng dần (V2, V3...) — không bao giờ sửa lại file cũ đã chạy, giống như nguyên tắc "không được sửa lại 1 commit đã push lên remote" trong Git.

</details>

---

*File tiếp theo trong lộ trình: **Module 16 — Spring Security** (Authentication vs Authorization, Security Filter Chain, JWT, OAuth2/OpenID Connect, Password Encoding, CORS, CSRF, Method-level Security).*
