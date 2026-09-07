# Module 11 — ORM: JPA & Hibernate

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Đây là cầu nối trực tiếp giữa Java Core/OOP và Spring Boot thực chiến. Gần như 100% backend Java hiện đại dùng Spring Data JPA (built trên Hibernate) để thao tác database. Không hiểu rõ cơ chế ORM — đặc biệt là **N+1 Query Problem** và **Lazy/Eager loading** — là nguyên nhân số 1 khiến ứng dụng Spring Boot chạy chậm bất thường trong production, và cũng là câu hỏi phỏng vấn "phân loại" ứng viên Junior vs Mid/Senior.

---

## Mục lục

1. [ORM là gì? Vì sao cần ORM?](#1-orm-là-gì-vì-sao-cần-orm)
2. [JPA vs Hibernate — Specification vs Implementation](#2-jpa-vs-hibernate)
3. [Entity cơ bản: @Entity, @Id, @Table, @Column](#3-entity-cơ-bản)
4. [Entity Lifecycle & Persistence Context](#4-entity-lifecycle--persistence-context)
5. [Mapping quan hệ: @OneToMany, @ManyToOne, @ManyToMany, @OneToOne](#5-mapping-quan-hệ)
6. [Lazy vs Eager Loading](#6-lazy-vs-eager-loading)
7. [N+1 Query Problem](#7-n1-query-problem)
8. [JPQL, Criteria API, Native Query](#8-jpql-criteria-api-native-query)
9. [@Transactional — Transaction Management](#9-transactional)
10. [Cascade Types & Orphan Removal](#10-cascade-types--orphan-removal)
11. [⚠️ Các bẫy hay gặp](#11-các-bẫy-hay-gặp)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. ORM là gì? Vì sao cần ORM?

**ORM (Object-Relational Mapping)** là kỹ thuật ánh xạ giữa mô hình hướng đối tượng (Java objects) và mô hình quan hệ (bảng SQL), giúp lập trình viên thao tác database bằng object thay vì viết SQL thủ công.

### Vấn đề khi không có ORM (JDBC thuần)

```java
// JDBC thuần — nhiều boilerplate, dễ lỗi, khó bảo trì
public User findUserById(Long id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setLong(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                // ... map thủ công từng field, dễ sai sót
                return user;
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
    return null;
}
```

### Với ORM (JPA/Hibernate)

```java
// Chỉ cần định nghĩa Entity 1 lần, sau đó thao tác như object bình thường
User user = entityManager.find(User.class, id);
// Hibernate tự sinh SQL, tự map ResultSet -> Object
```

**Lợi ích của ORM:**
- Giảm boilerplate code đáng kể
- Database-independent ở mức code (đổi MySQL sang PostgreSQL không cần sửa nhiều)
- Tự động quản lý mapping quan hệ (foreign key ↔ object reference)
- Tích hợp caching, dirty checking, lazy loading sẵn có
- Type-safe hơn so với raw SQL string

**Đánh đổi (trade-off):**
- Có "learning curve" riêng — hiểu sai cơ chế dễ tạo ra SQL tệ hơn viết tay
- Có overhead hiệu năng nếu dùng không đúng cách (N+1 Problem là ví dụ điển hình)
- Với query phức tạp (report, aggregation nặng), đôi khi native SQL vẫn tốt hơn

---

## 2. JPA vs Hibernate

Đây là điểm gây nhầm lẫn nhiều nhất với người mới.

| | **JPA** | **Hibernate** |
|---|---|---|
| Bản chất | **Specification** (đặc tả) — tập hợp interface/annotation chuẩn (`javax.persistence` / `jakarta.persistence`) | **Implementation** (triển khai cụ thể) của JPA |
| Vai trò | Định nghĩa "phải có những gì" (API) | Định nghĩa "làm như thế nào" (code thực thi) |
| Ví dụ tương tự | Giống `interface List` trong Java | Giống `ArrayList` — implementation cụ thể |
| Có thể chạy độc lập? | Không — chỉ là tập interface | Có — là 1 JPA provider hoàn chỉnh |

**Các JPA provider khác ngoài Hibernate:** EclipseLink, OpenJPA — nhưng Hibernate chiếm thị phần áp đảo và là mặc định của Spring Boot (`spring-boot-starter-data-jpa`).

```
┌─────────────────────────────────────┐
│         JPA (Specification)          │  ← javax.persistence.*
│  @Entity, @Id, EntityManager, JPQL   │
└─────────────────────────────────────┘
                  ▲
                  │ implements
┌─────────────────────────────────────┐
│      Hibernate (Implementation)       │  ← org.hibernate.*
│  SessionFactory, Session, HQL         │
└─────────────────────────────────────┘
```

> **Liên hệ Spring Boot:** Khi bạn dùng `spring-boot-starter-data-jpa`, Spring Data JPA là lớp abstraction **thêm nữa** phía trên JPA, giúp bạn không cần viết implementation cho Repository (chỉ cần khai báo interface kế thừa `JpaRepository`).

---

## 3. Entity cơ bản

### @Entity, @Table, @Id, @Column

```java
import jakarta.persistence.*;

@Entity                          // Đánh dấu class này ánh xạ với 1 bảng trong DB
@Table(name = "users")           // Chỉ định tên bảng (nếu không có, mặc định = tên class)
public class User {

    @Id                                              // Khóa chính (Primary Key)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Chiến lược sinh ID tự động
    @Column(name = "id")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // JPA BẮT BUỘC phải có constructor không tham số (no-arg constructor)
    // vì Hibernate dùng Reflection để khởi tạo object
    protected User() {}

    public User(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }

    // getters/setters...
}
```

### Các chiến lược sinh ID (`GenerationType`)

| Strategy | Cơ chế | Khi dùng |
|---|---|---|
| `IDENTITY` | Dùng auto-increment của DB (MySQL `AUTO_INCREMENT`) | MySQL — đơn giản, phổ biến nhất |
| `SEQUENCE` | Dùng SEQUENCE object của DB | PostgreSQL, Oracle — hiệu năng tốt hơn khi insert hàng loạt (batch insert) |
| `TABLE` | Dùng 1 bảng riêng để lưu counter | Hiếm dùng — chậm, chỉ khi DB không hỗ trợ 2 cách trên |
| `AUTO` | Để Hibernate tự chọn dựa theo DB | Không khuyến khích cho production (khó đoán hành vi) |

⚠️ **Lưu ý quan trọng:** `GenerationType.IDENTITY` **không cho phép Hibernate batch insert hiệu quả** (phải insert từng dòng để lấy ID trả về), trong khi `SEQUENCE` cho phép Hibernate gom nhiều insert lại thành 1 batch → tốt hơn cho hiệu năng khi insert số lượng lớn.

### Các kiểu Column annotation thường dùng

```java
@Column(name = "price", precision = 10, scale = 2) // DECIMAL(10,2) cho tiền tệ
private BigDecimal price;

@Enumerated(EnumType.STRING)  // Lưu enum dạng String thay vì ordinal (index)
private OrderStatus status;
// ⚠️ LUÔN dùng EnumType.STRING, không dùng ORDINAL (mặc định)
// Vì nếu thêm/xóa/đổi thứ tự enum, ORDINAL sẽ làm sai lệch dữ liệu cũ đã lưu

@Temporal(TemporalType.TIMESTAMP) // Chỉ cần khi dùng java.util.Date (không cần với LocalDateTime)
private Date createdDate;

@Transient  // Field này KHÔNG được map vào DB, chỉ tồn tại trong bộ nhớ
private int calculatedAge;

@Lob  // Large Object — dùng cho TEXT/BLOB (văn bản dài, hình ảnh dạng byte[])
private String description;
```

---

## 4. Entity Lifecycle & Persistence Context

Đây là khái niệm **cốt lõi** để hiểu Hibernate hoạt động khác gì so với thao tác object bình thường.

### 4 trạng thái của Entity

```
   new User()
       │
       ▼
  ┌─────────┐   persist()    ┌──────────┐
  │TRANSIENT│ ─────────────► │ MANAGED  │
  │(tạm thời)│                │(đang quản lý)│
  └─────────┘                └──────────┘
                                   │  │
                    remove()       │  │  detach() / close session
                        ┌──────────┘  └──────────┐
                        ▼                        ▼
                  ┌──────────┐            ┌──────────┐
                  │ REMOVED  │            │ DETACHED │
                  │(đã xóa)   │            │(tách rời) │
                  └──────────┘            └──────────┘
                                                │
                                        merge() │
                                                ▼
                                          quay lại MANAGED
```

| Trạng thái | Mô tả | Đặc điểm |
|---|---|---|
| **Transient** | Object vừa `new`, chưa liên kết với Persistence Context | Không được Hibernate theo dõi, thay đổi field không ảnh hưởng DB |
| **Managed (Persistent)** | Đã được `persist()`/`find()` — đang nằm trong Persistence Context | **Dirty Checking**: mọi thay đổi field tự động sync xuống DB khi transaction commit |
| **Detached** | Từng Managed nhưng session đã đóng, hoặc gọi `detach()` | Thay đổi field **không** tự động lưu, cần `merge()` lại |
| **Removed** | Đã gọi `remove()`, sẽ bị xóa khỏi DB khi flush/commit | Vẫn là object Java bình thường tới khi transaction kết thúc |

### Dirty Checking — "phép màu" của Hibernate

```java
@Transactional
public void updateUserName(Long id, String newName) {
    User user = entityManager.find(User.class, id); // user giờ ở trạng thái MANAGED
    user.setFullName(newName); // Chỉ cần set field, KHÔNG cần gọi save()/update()!
    // Khi transaction commit, Hibernate tự động so sánh snapshot ban đầu
    // với trạng thái hiện tại -> phát hiện field đã đổi -> tự sinh UPDATE SQL
}
```

⚠️ **Bẫy hay gặp:** Nhiều bạn mới học viết `userRepository.save(user)` ngay sau khi set field trong 1 method `@Transactional` — thực ra **không cần thiết** vì Dirty Checking đã tự làm điều đó. Gọi `save()` thêm không sai nhưng thừa (dù với Spring Data JPA, `save()` trên entity đã managed thực chất cũng không làm gì thêm).

### Persistence Context = "bộ nhớ đệm cấp 1" (First-Level Cache)

Mỗi `EntityManager` (hay Hibernate `Session`) sở hữu 1 Persistence Context riêng — hoạt động như 1 Map lưu Entity theo khóa chính, đảm bảo trong cùng 1 transaction, gọi `find()` với cùng ID nhiều lần chỉ query DB **1 lần duy nhất**:

```java
User u1 = entityManager.find(User.class, 1L); // Query DB
User u2 = entityManager.find(User.class, 1L); // KHÔNG query DB, lấy từ cache
System.out.println(u1 == u2); // true — cùng 1 object reference!
```

---

## 5. Mapping quan hệ

Đây là phần **quan trọng nhất** và cũng dễ gây bug nhất của JPA. 4 loại quan hệ chính, ánh xạ với thiết kế database quan hệ mà bạn đã học ở Module 10.

### 5.1. @ManyToOne — "Nhiều thuộc về Một" (phổ biến nhất)

Ví dụ: Nhiều `Order` thuộc về 1 `User`.

```java
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)   // LUÔN chỉ định LAZY tường minh (xem mục 6)
    @JoinColumn(name = "user_id")        // Tên cột foreign key trong bảng orders
    private User user;
}
```

SQL tương ứng:
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    total_amount DECIMAL(10,2),
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 5.2. @OneToMany — "Một có Nhiều" (mặt còn lại của ManyToOne)

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
    // mappedBy = "user" nghĩa là: "phía sở hữu foreign key là field 'user' bên class Order"
    // -> Đây là bên KHÔNG sở hữu quan hệ (inverse side / non-owning side)
}
```

⚠️ **Khái niệm cực kỳ quan trọng: Owning Side vs Inverse Side**

| | **Owning Side** (bên sở hữu) | **Inverse Side** (bên bị động) |
|---|---|---|
| Ai giữ `@JoinColumn`? | Bên này | Bên kia (dùng `mappedBy`) |
| Ai quyết định giá trị FK khi lưu DB? | **Chỉ bên Owning Side** | Không ảnh hưởng gì tới DB |
| Ví dụ | `Order.user` (có `@JoinColumn`) | `User.orders` (có `mappedBy`) |

```java
// SAI LẦM PHỔ BIẾN: chỉ set ở phía inverse side, quên set phía owning side
User user = userRepository.findById(1L).get();
Order order = new Order();
user.getOrders().add(order); // Chỉ set phía User.orders (inverse) -> KHÔNG lưu được FK!
orderRepository.save(order); // order.user vẫn là null -> lưu user_id = NULL trong DB!

// ĐÚNG: phải set phía owning side (Order.user)
order.setUser(user);          // set phía Owning Side — bắt buộc
user.getOrders().add(order);  // set phía Inverse Side — để đồng bộ object trong bộ nhớ (không bắt buộc với DB nhưng nên làm để tránh bug logic)
orderRepository.save(order);
```

**Best practice:** Viết 1 helper method để luôn đồng bộ cả 2 chiều:

```java
public class User {
    private List<Order> orders = new ArrayList<>();

    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this); // Đảm bảo luôn set đúng owning side
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
}
```

### 5.3. @ManyToMany

Ví dụ: `Student` học nhiều `Course`, mỗi `Course` có nhiều `Student`.

```java
@Entity
public class Student {
    @Id @GeneratedValue private Long id;

    @ManyToMany
    @JoinTable(
        name = "student_course",              // Tên bảng trung gian (junction table)
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id @GeneratedValue private Long id;

    @ManyToMany(mappedBy = "courses")  // Inverse side
    private Set<Student> students = new HashSet<>();
}
```

⚠️ **Bẫy hay gặp:** Dùng `List` thay vì `Set` cho `@ManyToMany`/`@OneToMany` có thể gây ra vấn đề hiệu năng nghiêm trọng (Hibernate xóa hết rồi insert lại toàn bộ collection khi có thay đổi, gọi là "anti-pattern MultipleBagFetchException" khi fetch nhiều List cùng lúc). **Khuyến nghị dùng `Set`** trừ khi thực sự cần thứ tự và trùng lặp.

> **Thực tế production:** `@ManyToMany` trực tiếp thường được khuyến cáo **tránh dùng** khi bảng trung gian cần thêm dữ liệu riêng (VD: ngày enroll, điểm số). Giải pháp tốt hơn là tách thành 1 Entity riêng (`Enrollment`) với 2 quan hệ `@ManyToOne`:

```java
@Entity
public class Enrollment {
    @Id @GeneratedValue private Long id;

    @ManyToOne private Student student;
    @ManyToOne private Course course;

    private LocalDate enrolledDate; // Dữ liệu riêng của quan hệ
    private Double grade;
}
```

### 5.4. @OneToOne

```java
@Entity
public class User {
    @Id @GeneratedValue private Long id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile profile;
}

@Entity
public class UserProfile {
    @Id @GeneratedValue private Long id;

    @OneToOne
    @JoinColumn(name = "user_id") // Owning side
    private User user;
}
```

### Bảng tổng hợp 4 loại quan hệ

| Quan hệ | Ví dụ thực tế | Bên giữ FK (Owning Side) |
|---|---|---|
| `@OneToOne` | User ↔ UserProfile | Bên có `@JoinColumn` |
| `@ManyToOne` | Order → User | Luôn là bên "Many" |
| `@OneToMany` | User → Orders | Bên "Many" (dùng `mappedBy`) |
| `@ManyToMany` | Student ↔ Course | Bên có `@JoinTable` |

---

## 6. Lazy vs Eager Loading

Đây là khái niệm quyết định hiệu năng ứng dụng — **phải hiểu sâu**.

### Khái niệm

- **EAGER**: Khi load Entity chính, JPA **load luôn** các entity liên quan ngay lập tức (JOIN hoặc query phụ ngay).
- **LAZY**: Khi load Entity chính, các entity liên quan **chưa được load** — chỉ load khi thực sự truy cập (gọi getter) — dùng kỹ thuật **Proxy Object**.

### Fetch Type mặc định của JPA (quan trọng — hay bị hỏi phỏng vấn)

| Annotation | Mặc định |
|---|---|
| `@ManyToOne` | **EAGER** |
| `@OneToOne` | **EAGER** |
| `@OneToMany` | LAZY |
| `@ManyToMany` | LAZY |

⚠️ **Bẫy cực kỳ hay gặp:** `@ManyToOne` và `@OneToOne` mặc định là **EAGER** — trái ngược với trực giác của nhiều người mới học (nghĩ rằng tất cả đều LAZY). Đây là lý do vì sao **best practice là luôn khai báo `fetch = FetchType.LAZY` tường minh cho MỌI quan hệ**, kể cả khi nó không cần thiết ngay bây giờ:

```java
@ManyToOne(fetch = FetchType.LAZY)  // Luôn ghi rõ, đừng dựa vào default
@JoinColumn(name = "user_id")
private User user;
```

### Cơ chế Lazy Loading — Proxy Object

```java
@Transactional
public void demo() {
    Order order = orderRepository.findById(1L).get();
    // Tại đây, order.user là 1 PROXY object (Hibernate tạo class con của User, chưa có dữ liệu thật)

    System.out.println(order.getTotalAmount()); // OK — field của Order đã load

    System.out.println(order.getUser().getFullName());
    // NGAY tại đây, Hibernate mới thực sự chạy SELECT * FROM users WHERE id = ?
    // (gọi là "initialize the proxy")
}
```

### LazyInitializationException — lỗi runtime phổ biến nhất khi mới học JPA

```java
public Order getOrder(Long id) {
    Order order = orderRepository.findById(id).get();
    return order; // Transaction/Session đã đóng khi method kết thúc
}

// Ở Controller hoặc lớp khác, ngoài transaction:
Order order = orderService.getOrder(1L);
order.getUser().getFullName();
// ❌ org.hibernate.LazyInitializationException:
//    could not initialize proxy - no Session
```

**Nguyên nhân:** Proxy object chỉ có thể "initialize" (load dữ liệu thật) khi Persistence Context/Session vẫn còn mở. Ngoài phạm vi `@Transactional`, session đã đóng → Proxy "chết", không load được nữa.

**Các giải pháp:**

```java
// Giải pháp 1: JOIN FETCH trong JPQL — load sẵn quan hệ cần dùng trong 1 query
@Query("SELECT o FROM Order o JOIN FETCH o.user WHERE o.id = :id")
Optional<Order> findByIdWithUser(@Param("id") Long id);

// Giải pháp 2: Entity Graph — khai báo tường minh field nào cần fetch eager cho query này
@EntityGraph(attributePaths = {"user"})
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findByIdWithGraph(@Param("id") Long id);

// Giải pháp 3: DTO Projection — chỉ lấy đúng field cần, tránh toàn bộ vấn đề Lazy/Proxy
public interface OrderSummary {
    Long getId();
    BigDecimal getTotalAmount();
    String getUserFullName(); // Spring Data JPA tự map qua JOIN
}

// Giải pháp 4 (không khuyến khích): mở rộng session tới hết view -
// "Open Session In View" (OSIV) — mặc định BẬT trong Spring Boot,
// nhưng gây khó kiểm soát query, nên nhiều dự án production tắt đi
// (spring.jpa.open-in-view=false) và xử lý bằng JOIN FETCH/DTO thay thế
```

---

## 7. N+1 Query Problem

Đây là vấn đề hiệu năng **kinh điển nhất** khi làm việc với ORM — gần như chắc chắn sẽ gặp trong phỏng vấn và trong công việc thực tế.

### Minh họa vấn đề

```java
List<Order> orders = orderRepository.findAll(); // 1 query: SELECT * FROM orders

for (Order order : orders) {
    System.out.println(order.getUser().getFullName());
    // Với mỗi order, Hibernate chạy thêm 1 query riêng:
    // SELECT * FROM users WHERE id = ?
}
```

Nếu có **100 orders** → tổng cộng: **1 (lấy orders) + 100 (lấy user cho từng order) = 101 query** → đây là "N+1" (1 query gốc + N query phụ).

```
Query 1: SELECT * FROM orders;                      -- lấy 100 orders
Query 2: SELECT * FROM users WHERE id = 1;           -- lấy user của order 1
Query 3: SELECT * FROM users WHERE id = 2;           -- lấy user của order 2
...
Query 101: SELECT * FROM users WHERE id = 100;       -- lấy user của order 100
```

Trong khi đáng lẽ chỉ cần **1 query JOIN duy nhất**:

```sql
SELECT o.*, u.* FROM orders o JOIN users u ON o.user_id = u.id;
```

### Vì sao nguy hiểm?

- Với dữ liệu nhỏ (test local), không thấy vấn đề gì
- Với dữ liệu production (hàng nghìn record), số lượng round-trip tới DB tăng tuyến tính → **API timeout, DB quá tải**
- Đây là loại bug **"âm thầm"** — code chạy đúng logic, không lỗi cú pháp, chỉ chậm dần khi dữ liệu lớn lên

### Cách phát hiện N+1

```yaml
# application.yml — bật log để nhìn thấy SQL Hibernate sinh ra
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
```

Hoặc dùng thư viện chuyên dụng: **datasource-proxy**, hoặc plugin **p6spy** để đếm số query thực tế trong 1 request — nếu số query tỉ lệ thuận với số record trả về → chính là N+1.

### Cách khắc phục

**Cách 1 — JOIN FETCH (JPQL):**
```java
@Query("SELECT o FROM Order o JOIN FETCH o.user")
List<Order> findAllWithUser(); // Chỉ 1 query duy nhất, dùng SQL JOIN
```

**Cách 2 — @EntityGraph:**
```java
@EntityGraph(attributePaths = {"user"})
@Query("SELECT o FROM Order o")
List<Order> findAllWithUserGraph();
```

**Cách 3 — Batch Fetching (giảm N query xuống N/batchSize):**
```java
@ManyToOne(fetch = FetchType.LAZY)
@BatchSize(size = 20) // Gom tối đa 20 ID lại thành 1 query IN (...)
@JoinColumn(name = "user_id")
private User user;
```
```sql
-- Thay vì 100 query riêng lẻ, Hibernate gom thành:
SELECT * FROM users WHERE id IN (1,2,3,...,20);
SELECT * FROM users WHERE id IN (21,22,...,40);
-- ...
```

**Cách 4 — DTO Projection (tốt nhất cho read-only, API trả dữ liệu):**
```java
public record OrderDTO(Long id, BigDecimal totalAmount, String userFullName) {}

@Query("SELECT new com.example.dto.OrderDTO(o.id, o.totalAmount, o.user.fullName) " +
       "FROM Order o JOIN o.user")
List<OrderDTO> findAllOrderDTOs(); // 1 query, chỉ lấy đúng field cần, không tạo Proxy
```

> **Nguyên tắc thực chiến:** Với API trả danh sách dữ liệu (read-heavy), ưu tiên DTO Projection. Với nghiệp vụ cần thao tác Entity đầy đủ (update, business logic phức tạp), dùng JOIN FETCH hoặc EntityGraph theo từng use case cụ thể — **không** để mặc định LAZY tự động N+1.

---

## 8. JPQL, Criteria API, Native Query

### JPQL (Java Persistence Query Language)

Query hướng đối tượng — thao tác trên **tên Entity/field**, không phải tên bảng/cột SQL:

```java
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmailJPQL(@Param("email") String email);

@Query("SELECT o FROM Order o WHERE o.totalAmount > :amount ORDER BY o.totalAmount DESC")
List<Order> findExpensiveOrders(@Param("amount") BigDecimal amount);
```

### Spring Data JPA — Derived Query Methods (không cần viết JPQL)

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA tự sinh query dựa theo TÊN METHOD
    Optional<User> findByEmail(String email);
    List<User> findByFullNameContaining(String keyword);
    List<User> findByCreatedAtAfter(LocalDateTime date);
    boolean existsByEmail(String email);
    long countByStatus(UserStatus status);
    List<User> findTop10ByOrderByCreatedAtDesc();
}
```

### Native Query — khi cần SQL đặc thù của từng DBMS

```java
@Query(value = "SELECT * FROM users WHERE MATCH(full_name) AGAINST(:keyword)",
       nativeQuery = true) // Dùng full-text search riêng của MySQL
List<User> searchByFullText(@Param("keyword") String keyword);
```

### Criteria API — xây query động bằng code (type-safe, tránh SQL injection)

```java
public List<User> searchUsers(String name, UserStatus status) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> query = cb.createQuery(User.class);
    Root<User> root = query.from(User.class);

    List<Predicate> predicates = new ArrayList<>();
    if (name != null) {
        predicates.add(cb.like(root.get("fullName"), "%" + name + "%"));
    }
    if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
    }
    query.where(predicates.toArray(new Predicate[0]));

    return entityManager.createQuery(query).getResultList();
}
```

| Cách | Khi dùng |
|---|---|
| Derived Query Method | Query đơn giản, ít điều kiện |
| JPQL (`@Query`) | Query phức tạp vừa phải, cố định |
| Criteria API | Query **động** (nhiều điều kiện filter tùy chọn — search form) |
| Native Query | Cần tính năng đặc thù của DBMS, tối ưu hiệu năng cực hạn |
| **Querydsl** (thư viện ngoài, không thuộc JPA chuẩn) | Thay thế Criteria API, code dễ đọc hơn nhiều — phổ biến trong dự án lớn |

---

## 9. @Transactional

### Vì sao cần Transaction?

Đảm bảo tính **ACID** (đã học ở Module 10) khi thực hiện nhiều thao tác DB liên quan — hoặc tất cả cùng thành công, hoặc tất cả cùng rollback.

```java
@Service
public class TransferService {

    @Transactional
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        if (from.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException(); // Toàn bộ transaction rollback -> DB không bị mất tiền
        }
        // Không cần gọi save() -> Dirty Checking tự update khi commit
    }
}
```

### Cơ chế hoạt động — Spring AOP Proxy

`@Transactional` hoạt động nhờ **Spring AOP** tạo 1 Proxy bọc quanh bean — trước khi method chạy, proxy mở transaction; sau khi method kết thúc, proxy commit (hoặc rollback nếu có exception).

⚠️ **Bẫy kinh điển #1 — Self-invocation (gọi method nội bộ trong cùng class):**

```java
@Service
public class OrderService {

    public void placeOrder(Order order) {
        saveOrder(order); // Gọi trực tiếp qua "this" -> KHÔNG đi qua Proxy -> @Transactional bị BỎ QUA!
    }

    @Transactional
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }
}
```

**Giải thích:** Vì Proxy bọc bên ngoài object thật, chỉ khi gọi method **từ bên ngoài class** (qua Spring container inject) mới đi qua proxy. Gọi `this.saveOrder()` là gọi trực tiếp method thật, bỏ qua toàn bộ cơ chế transaction.

**Giải pháp:** Tách method ra 1 Service/Bean khác, hoặc dùng `AopContext.currentProxy()` (không khuyến khích, phá vỡ tính rõ ràng của code).

⚠️ **Bẫy kinh điển #2 — Rollback chỉ áp dụng cho Unchecked Exception mặc định:**

```java
@Transactional
public void process() throws IOException {
    // ...
    throw new IOException("Lỗi file"); // Checked Exception -> Spring KHÔNG tự rollback!
}
```

Mặc định, `@Transactional` chỉ rollback khi gặp `RuntimeException` (Unchecked) hoặc `Error`. Với Checked Exception, phải khai báo tường minh:

```java
@Transactional(rollbackFor = Exception.class) // Rollback cho MỌI exception, kể cả Checked
public void process() throws IOException {
    throw new IOException("Lỗi file");
}
```

### Propagation — hành vi khi transaction lồng nhau

| Propagation | Ý nghĩa |
|---|---|
| `REQUIRED` (mặc định) | Dùng transaction hiện có, nếu chưa có thì tạo mới |
| `REQUIRES_NEW` | Luôn tạo transaction mới, tạm dừng transaction hiện tại (VD: ghi log dù transaction chính rollback) |
| `NESTED` | Tạo savepoint trong transaction hiện tại — rollback về savepoint mà không ảnh hưởng transaction cha |
| `SUPPORTS` | Dùng transaction nếu có, không có cũng chạy bình thường (không transaction) |
| `MANDATORY` | Bắt buộc phải có transaction đang chạy, không có thì throw exception |
| `NEVER` | Bắt buộc KHÔNG có transaction, có thì throw exception |

```java
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
    try {
        auditLogService.logOrderCreation(order); // Cần log được lưu dù order sau đó rollback
    } catch (Exception e) {
        // xử lý
    }
}

@Service
public class AuditLogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCreation(Order order) {
        // Transaction riêng biệt -> dù placeOrder() rollback, log này vẫn được lưu
    }
}
```

### Isolation Level trong @Transactional

```java
@Transactional(isolation = Isolation.READ_COMMITTED) // Đã học chi tiết ở Module 10
public void someMethod() { ... }
```

---

## 10. Cascade Types & Orphan Removal

### Cascade — "lan truyền" thao tác từ Entity cha xuống Entity con

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders = new ArrayList<>();
```

| CascadeType | Ý nghĩa |
|---|---|
| `PERSIST` | Khi lưu cha, tự động lưu luôn con |
| `MERGE` | Khi merge cha, tự động merge luôn con |
| `REMOVE` | Khi xóa cha, tự động xóa luôn con |
| `REFRESH` | Khi refresh cha, tự động refresh luôn con |
| `DETACH` | Khi detach cha, tự động detach luôn con |
| `ALL` | Áp dụng tất cả 5 loại trên |

```java
User user = new User("Pho", "pho@example.com");
Order order1 = new Order(...);
user.addOrder(order1); // Chỉ set quan hệ trong bộ nhớ

userRepository.save(user); // Với cascade = PERSIST/ALL, order1 CŨNG được lưu tự động
// Không cần gọi orderRepository.save(order1) riêng!
```

### orphanRemoval — xóa "con mồ côi"

```java
user.getOrders().remove(order1); // Bỏ order1 ra khỏi list trong bộ nhớ

// Với orphanRemoval = true: khi transaction commit, order1 sẽ bị XÓA khỏi DB
//   (vì nó không còn thuộc về user nào -> "mồ côi")
// Với orphanRemoval = false (mặc định): order1 chỉ mất liên kết (user_id = NULL),
//   KHÔNG bị xóa khỏi DB
```

⚠️ **Bẫy hay gặp:** Dùng `CascadeType.ALL` + `orphanRemoval = true` cho quan hệ `@ManyToOne` phía "Many" (VD: nhiều `Order` cùng share 1 `User`) sẽ cực kỳ nguy hiểm — xóa 1 User có thể vô tình cascade xóa toàn bộ Orders liên quan **kể cả khi đó không phải ý định**. Cascade chỉ nên dùng khi quan hệ mang tính "sở hữu chặt" (composition thực sự — con không có ý nghĩa tồn tại độc lập khỏi cha, VD: `User` - `UserProfile`).

---

## 11. ⚠️ Các bẫy hay gặp

1. **Quên `fetch = FetchType.LAZY` tường minh** cho `@ManyToOne`/`@OneToOne` → mặc định EAGER → load dư thừa dữ liệu không cần thiết, đôi khi gây vòng lặp vô hạn khi serialize sang JSON (`User` có `List<Order>`, mỗi `Order` lại có `User`...).

2. **N+1 Query Problem** — không kiểm tra SQL log khi code, tới lúc data lớn mới phát hiện chậm.

3. **LazyInitializationException** — truy cập quan hệ LAZY ngoài phạm vi transaction/session.

4. **Quên set cả 2 chiều quan hệ** (owning side + inverse side) → dữ liệu không nhất quán trong bộ nhớ dù DB vẫn đúng.

5. **`equals()`/`hashCode()` dựa trên `id` có thể sai** khi Entity chưa được persist (`id = null`) — 2 entity mới tạo (chưa có id) sẽ bị coi là "bằng nhau" nếu dùng `id` để so sánh mà không check null trước. Giải pháp: dùng **Business Key** (natural key, VD: email) để so sánh thay vì chỉ dựa vào `id`.

6. **Vòng lặp vô hạn khi serialize JSON** với quan hệ 2 chiều (`User.orders` ↔ `Order.user`) → dùng `@JsonIgnore`, `@JsonManagedReference`/`@JsonBackReference`, hoặc (khuyến nghị) dùng **DTO riêng cho response API**, không trả trực tiếp Entity.

7. **Dùng `CascadeType.REMOVE` bừa bãi** trên quan hệ không phải composition thật sự → xóa nhầm dữ liệu liên quan không mong muốn.

8. **`@Transactional` trên method `private`** — Spring AOP Proxy (mặc định dùng CGLIB/JDK dynamic proxy) không thể bọc được method `private`, `@Transactional` sẽ bị bỏ qua hoàn toàn, không có cảnh báo lỗi rõ ràng.

9. **Không đặt `@Transactional(readOnly = true)` cho các method chỉ đọc dữ liệu** → bỏ lỡ tối ưu hiệu năng (Hibernate bỏ qua Dirty Checking, một số DB driver tối ưu riêng cho read-only transaction).

10. **Trả Entity trực tiếp ra Controller/API** thay vì DTO → rò rỉ cấu trúc DB ra ngoài, dễ gặp LazyInitializationException khi serialize, khó version API sau này.

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| JPA vs Hibernate | JPA = spec (interface), Hibernate = implementation phổ biến nhất |
| Entity Lifecycle | Transient → Managed → (Detached / Removed) |
| Dirty Checking | Managed entity: chỉ cần set field, không cần gọi save() |
| Owning Side | Bên có `@JoinColumn` — quyết định giá trị FK thực tế trong DB |
| Fetch mặc định | `@ManyToOne`/`@OneToOne` = EAGER; `@OneToMany`/`@ManyToMany` = LAZY |
| Best practice Fetch | LUÔN khai báo `FetchType.LAZY` tường minh cho mọi quan hệ |
| N+1 Problem | 1 query cha + N query con lặp lại → khắc phục bằng JOIN FETCH/EntityGraph/DTO |
| @Transactional | Dựa trên AOP Proxy → self-invocation không hoạt động; chỉ rollback RuntimeException mặc định |
| Cascade | Lan truyền thao tác cha → con; chỉ dùng cho quan hệ composition thật sự |
| orphanRemoval | Xóa con khỏi collection cha → xóa luôn khỏi DB |
| API response | Luôn trả DTO, không trả Entity trực tiếp |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. `@OneToMany` mặc định là FetchType.EAGER.
2. Trong quan hệ `@ManyToOne` - `@OneToMany`, bên giữ `@JoinColumn` luôn là bên "Many".
3. Gọi `entityManager.find()` 2 lần với cùng ID trong cùng 1 transaction sẽ luôn query DB 2 lần.
4. `@Transactional` mặc định rollback khi gặp Checked Exception.
5. Dirty Checking chỉ hoạt động với Entity đang ở trạng thái Managed.
6. `orphanRemoval = true` sẽ xóa entity con khỏi DB ngay khi bị remove khỏi List trong bộ nhớ, kể cả khi chưa transaction commit.
7. N+1 Query Problem chỉ xảy ra với quan hệ `@OneToMany`, không xảy ra với `@ManyToOne`.
8. Self-invocation (gọi method `@Transactional` từ method khác cùng class qua `this`) vẫn kích hoạt transaction bình thường.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Thiết kế 2 Entity `Category` và `Product` với quan hệ `@OneToMany`/`@ManyToOne` (1 Category có nhiều Product). Viết đầy đủ: entity, owning side/inverse side đúng chuẩn, helper method `addProduct()`/`removeProduct()` để đồng bộ 2 chiều.

**Bài 2:** Cho đoạn code sau đang gặp N+1 Query Problem, hãy sửa lại bằng **3 cách khác nhau** (JOIN FETCH, EntityGraph, DTO Projection):
```java
List<Category> categories = categoryRepository.findAll();
for (Category c : categories) {
    System.out.println(c.getProducts().size());
}
```

**Bài 3:** Viết 1 method `@Transactional` mô phỏng nghiệp vụ đặt hàng (`placeOrder`): trừ tồn kho sản phẩm, tạo Order mới, nếu tồn kho không đủ thì throw exception và đảm bảo toàn bộ rollback. Giải thích rõ vì sao chọn `RuntimeException` hay cần thêm `rollbackFor`.

**Bài 4:** Viết đoạn code minh họa lỗi `LazyInitializationException` (entity load trong 1 `@Transactional` method, trả ra ngoài, rồi truy cập quan hệ LAZY ở method khác không có transaction) — sau đó sửa lại bằng DTO Projection.

**Bài 5:** Thiết kế quan hệ `@ManyToMany` giữa `Student` và `Course`, sau đó refactor thành 2 quan hệ `@ManyToOne` thông qua Entity trung gian `Enrollment` có thêm field `enrolledDate` và `grade`. Giải thích vì sao cách 2 tốt hơn trong thực tế.

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** `@OneToMany` mặc định là LAZY. Chỉ `@ManyToOne` và `@OneToOne` mặc định EAGER.
2. **Đúng.** Bên "Many" luôn giữ foreign key trong thiết kế database quan hệ, nên bên "Many" luôn là Owning Side với `@JoinColumn`.
3. **Sai.** Nhờ Persistence Context (First-Level Cache), lần gọi thứ 2 sẽ lấy từ cache trong bộ nhớ, không query DB lại.
4. **Sai.** Mặc định `@Transactional` chỉ rollback với `RuntimeException`/`Error` (Unchecked). Checked Exception cần khai báo `rollbackFor` tường minh.
5. **Đúng.** Dirty Checking chỉ áp dụng cho entity đang được Persistence Context theo dõi (Managed). Entity Detached/Transient không có cơ chế này.
6. **Sai.** Thao tác xóa (DELETE SQL) chỉ thực sự chạy khi transaction **flush/commit**, không xảy ra ngay lập tức khi gọi `remove()` trên List trong bộ nhớ.
7. **Sai.** N+1 xảy ra với cả `@ManyToOne` (ví dụ load N Order rồi load User cho từng Order) lẫn `@OneToMany`.
8. **Sai.** Self-invocation bỏ qua Proxy của Spring AOP, nên `@Transactional` không có tác dụng khi gọi qua `this`.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
@Entity
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    // Helper method đảm bảo đồng bộ 2 chiều
    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setCategory(null);
    }
}

@Entity
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY) // Owning side - luôn LAZY tường minh
    @JoinColumn(name = "category_id")
    private Category category;
}
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
// Cách 1: JOIN FETCH
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT DISTINCT c FROM Category c JOIN FETCH c.products")
    List<Category> findAllWithProducts();
}

// Cách 2: EntityGraph
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @EntityGraph(attributePaths = {"products"})
    @Query("SELECT c FROM Category c")
    List<Category> findAllWithProductsGraph();
}

// Cách 3: DTO Projection (không load Product entities, chỉ đếm)
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT new com.example.dto.CategorySummary(c.id, c.name, COUNT(p)) " +
           "FROM Category c LEFT JOIN c.products p GROUP BY c.id, c.name")
    List<CategorySummary> findAllWithProductCount();
}
public record CategorySummary(Long id, String name, Long productCount) {}
```

**Giải thích:** JOIN FETCH dùng `DISTINCT` để tránh trùng lặp Category khi JOIN với nhiều Product (do JOIN SQL nhân bản dòng). DTO Projection là cách tốt nhất khi chỉ cần con số thống kê, tránh load toàn bộ Product entity không cần thiết.

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional(rollbackFor = Exception.class) // An toàn cho cả Checked Exception
    public Order placeOrder(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm"));

        if (product.getStock() < quantity) {
            // InsufficientStockException nên là RuntimeException
            // -> đủ để trigger rollback mặc định, nhưng rollbackFor = Exception.class
            //    vẫn nên giữ để an toàn nếu sau này có Checked Exception khác trong method
            throw new InsufficientStockException(
                "Tồn kho không đủ: còn " + product.getStock() + ", cần " + quantity);
        }

        product.setStock(product.getStock() - quantity); // Dirty Checking tự update

        Order order = new Order(product, quantity, product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        return orderRepository.save(order);
        // Nếu exception xảy ra bất cứ đâu trong method này,
        // TOÀN BỘ thay đổi (trừ tồn kho + tạo order) đều rollback -> đảm bảo tính nhất quán
    }
}
```

**Giải thích:** Dùng `RuntimeException` (Unchecked) là đủ để Spring tự rollback mặc định, không bắt buộc cần `rollbackFor`. Tuy nhiên thêm `rollbackFor = Exception.class` là thói quen tốt (defensive coding) để tránh trường hợp sau này có ai thêm Checked Exception vào method mà quên cập nhật rollback rule.

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
// ❌ Code lỗi
@Service
public class OrderService {
    @Transactional
    public Order getOrder(Long id) {
        return orderRepository.findById(id).orElseThrow();
        // Transaction đóng ngay khi method kết thúc (return)
    }
}

@RestController
public class OrderController {
    @GetMapping("/orders/{id}")
    public String getOrderUserName(@PathVariable Long id) {
        Order order = orderService.getOrder(id); // Transaction đã đóng tại đây
        return order.getUser().getFullName();
        // ❌ LazyInitializationException: order.user là Proxy chưa initialize,
        //    session đã đóng -> không thể load
    }
}

// ✅ Sửa bằng DTO Projection
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT new com.example.dto.OrderUserDTO(o.id, u.fullName) " +
           "FROM Order o JOIN o.user u WHERE o.id = :id")
    Optional<OrderUserDTO> findOrderUserDTO(@Param("id") Long id);
}
public record OrderUserDTO(Long orderId, String userFullName) {}

@RestController
public class OrderController {
    @GetMapping("/orders/{id}/username")
    public String getOrderUserName(@PathVariable Long id) {
        OrderUserDTO dto = orderRepository.findOrderUserDTO(id).orElseThrow();
        return dto.userFullName();
        // ✅ Không có Proxy, không có vấn đề Lazy Loading -
        //    dữ liệu đã lấy đầy đủ ngay trong 1 query
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```java
// Cách 1: @ManyToMany trực tiếp (hạn chế)
@Entity
public class Student {
    @Id @GeneratedValue private Long id;
    private String name;

    @ManyToMany
    @JoinTable(name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> courses = new HashSet<>();
    // ❌ Không thể lưu enrolledDate, grade ở đâu cả -
    //    bảng trung gian student_course chỉ có 2 cột FK
}

// Cách 2: Entity trung gian Enrollment (khuyến nghị)
@Entity
public class Enrollment {
    @Id @GeneratedValue private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDate enrolledDate; // ✅ Dữ liệu riêng của quan hệ
    private Double grade;           // ✅ Lưu được điểm số cho từng lượt enroll
}

@Entity
public class Student {
    @Id @GeneratedValue private Long id;
    private String name;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
}

@Entity
public class Course {
    @Id @GeneratedValue private Long id;
    private String name;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
}
```

**Giải thích vì sao Cách 2 tốt hơn:**
- `@ManyToMany` trực tiếp chỉ phù hợp khi bảng trung gian **không cần lưu thêm dữ liệu gì**. Thực tế nghiệp vụ hầu như luôn cần (ngày đăng ký, điểm số, trạng thái...).
- Tách thành Entity riêng giúp dễ dàng thêm field mới sau này (VD: `status: ENROLLED/DROPPED/COMPLETED`) mà không phải đổi kiến trúc.
- Truy vấn linh hoạt hơn: có thể query trực tiếp trên `Enrollment` (VD: tìm tất cả sinh viên có điểm > 8 trong 1 khóa học) mà không cần join phức tạp qua bảng trung gian ẩn.
- Tránh được các vấn đề hiệu năng/hành vi khó đoán của `@ManyToMany` (Hibernate quản lý bảng trung gian tự động đôi khi xóa/insert lại toàn bộ khi có thay đổi nhỏ).

</details>

---

*File tiếp theo trong lộ trình: **Module 12 — Spring Framework Core** (IoC Container, Dependency Injection, Bean Lifecycle, ApplicationContext, @Component/@Autowired, Spring AOP).*
