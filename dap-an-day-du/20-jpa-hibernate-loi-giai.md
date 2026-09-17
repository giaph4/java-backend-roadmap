# Lời giải đầy đủ — Module 12: JPA & Hibernate

> Nguồn đề: `20 jpa hibernate/20-jpa-hibernate.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `Category` & `Product` với `@OneToMany`/`@ManyToOne`

### Đề
1 Category có nhiều Product. Owning side/inverse side đúng chuẩn, helper method `addProduct()`/`removeProduct()` đồng bộ 2 chiều.

### Phân tích

Trong quan hệ 1-N, **`@ManyToOne` LUÔN là owning side** (phía sở hữu khóa ngoại thực tế) — vì bảng `products` mới là nơi chứa cột `category_id` (khóa ngoại) trong CSDL quan hệ; `@OneToMany` phía `Category` chỉ là **inverse side** (phía "ánh xạ ngược", cần `mappedBy` trỏ về field owning bên kia). Nhầm lẫn owning/inverse là lỗi JPA phổ biến nhất, dẫn tới hiện tượng "set quan hệ nhưng không lưu xuống DB".

### Lời giải

```java
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // INVERSE SIDE - mappedBy trỏ tới field "category" bên Product (owning side)
    // KHÔNG sở hữu khóa ngoại - chỉ phản ánh quan hệ đã được định nghĩa bên kia
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    // Helper method: ĐỒNG BỘ CẢ 2 CHIỀU trong 1 lần gọi - tránh lỗi "quên set 1 bên"
    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);   // set NGƯỢC LẠI - bắt buộc vì đây là owning side quyết định lưu DB
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setCategory(null);
    }

    // getter/setter...
}

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;

    // OWNING SIDE - chứa @JoinColumn, tương ứng cột khóa ngoại category_id trong bảng products
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // getter/setter...
}
```

### Giải thích

- **Vì sao helper method BẮT BUỘC, không chỉ là "tiện lợi":** nếu chỉ gọi `category.getProducts().add(product)` mà QUÊN gọi `product.setCategory(category)`, Hibernate sẽ **KHÔNG lưu quan hệ xuống DB** — vì Hibernate chỉ đọc trạng thái từ **owning side** (`Product.category`) khi sinh câu `INSERT`/`UPDATE`; phía `inverse side` (`Category.products`) chỉ dùng để Hibernate BIẾT CÁCH TRUY VẤN ngược lại, không ảnh hưởng gì tới việc ghi dữ liệu.
- **`fetch = FetchType.LAZY` trên `@ManyToOne`:** mặc định của JPA cho `@ManyToOne` là **EAGER** (khác với `@OneToMany` mặc định LAZY) — đây là 1 trong những "bẫy" phổ biến nhất khiến hiệu năng kém do load thừa dữ liệu không cần thiết; luôn khai báo tường minh `LAZY` cho `@ManyToOne`/`@OneToOne` trừ khi có lý do rõ ràng cần EAGER.
- **`orphanRemoval = true`:** khi 1 `Product` bị `remove()` khỏi list `products` của `Category` (qua `removeProduct()`), Hibernate tự động `DELETE` luôn `Product` đó khỏi DB (coi là "mồ côi" — không còn cha nào sở hữu) — khác với chỉ `cascade = ALL` (chỉ cascade khi thao tác trực tiếp trên `Category`, không tự xóa khi phần tử bị loại khỏi collection).

---

## Bài 2 — Giải quyết N+1 Query Problem bằng 3 cách

### Đề
```java
List<Category> categories = categoryRepository.findAll();
for (Category c : categories) {
    System.out.println(c.getProducts().size());
}
```
Sửa bằng JOIN FETCH, EntityGraph, DTO Projection.

### Phân tích

Đoạn code gốc: `findAll()` chạy **1 câu `SELECT` lấy toàn bộ Category** (1 query), sau đó **MỖI LẦN** gọi `c.getProducts()` trong vòng lặp lại kích hoạt **1 câu `SELECT` RIÊNG** để lazy-load danh sách Product của category đó (N query) → tổng cộng **1 + N query** thay vì lý tưởng chỉ cần 1-2 query.

### Lời giải

**Cách 1 — `JOIN FETCH` trong JPQL:**

```java
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.products")
    List<Category> findAllWithProducts();
}
```
*Hibernate sinh ra ĐÚNG 1 câu `SELECT ... JOIN ...` duy nhất, lấy cả Category lẫn Product liên quan trong 1 lần round-trip DB. `DISTINCT` cần thiết vì `JOIN` sinh ra nhiều dòng trùng lặp Category (1 dòng/Product) ở tầng SQL, Hibernate cần loại trùng ở tầng object.*

**Cách 2 — `@EntityGraph`:**

```java
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"products"})
    List<Category> findAll();   // override lại findAll() mặc định, áp dụng entity graph
}
```
*`@EntityGraph` khai báo "khi load Category, LOAD LUÔN products" mà không cần viết JPQL thủ công — Spring Data JPA tự sinh câu query tương đương JOIN FETCH phía dưới. Ưu điểm: gọn hơn, tái dùng được method `findAll()` sẵn có của `JpaRepository`.*

**Cách 3 — DTO Projection:**

```java
public record CategorySummaryDto(Long id, String name, long productCount) {}

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT new com.example.dto.CategorySummaryDto(c.id, c.name, COUNT(p)) " +
           "FROM Category c LEFT JOIN c.products p GROUP BY c.id, c.name")
    List<CategorySummaryDto> findAllSummary();
}
```
*Không load Entity đầy đủ (không cần toàn bộ object `Product`) — chỉ cần ĐẾM số lượng, DTO Projection để DB tự `COUNT()` và trả về đúng con số cần, tránh load dữ liệu thừa hoàn toàn.*

### Giải thích — so sánh 3 cách

| Cách | Số query | Dữ liệu trả về | Khi nào dùng |
|---|---|---|---|
| `JOIN FETCH` | 1 | Đầy đủ Entity Category + Product (object thật, có thể tiếp tục thao tác/cascade) | Cần thao tác tiếp trên Entity (sửa, cascade delete...) |
| `@EntityGraph` | 1 | Giống JOIN FETCH nhưng khai báo linh hoạt hơn (có thể bật/tắt theo từng method riêng, không sửa JPQL) | Muốn tránh viết JPQL tay, hoặc cần nhiều biến thể fetch khác nhau cho cùng 1 Entity |
| DTO Projection | 1 | Chỉ đúng dữ liệu cần hiển thị (không phải Entity, không thể `save()` lại) | Chỉ cần HIỂN THỊ dữ liệu (read-only), đặc biệt khi chỉ cần vài field/con số tổng hợp — hiệu quả nhất về băng thông + bộ nhớ |
| Đoạn code gốc | 1 + N | Đầy đủ nhưng TỐN N query thừa | KHÔNG nên dùng khi biết trước sẽ truy cập quan hệ LAZY trong vòng lặp |

- **Khuyến nghị thực tế:** DTO Projection thường là lựa chọn TỐT NHẤT cho các API chỉ đọc (read-only), vì tránh hoàn toàn overhead quản lý Entity (dirty checking, persistence context) mà JPQL/EntityGraph vẫn phải trả (dù đã gộp query, Hibernate vẫn tạo Entity đầy đủ, theo dõi thay đổi — tốn bộ nhớ/CPU hơn DTO thuần).

---

## Bài 3 — `@Transactional` mô phỏng nghiệp vụ đặt hàng

### Đề
`placeOrder`: trừ tồn kho, tạo Order, nếu tồn kho không đủ thì throw exception, đảm bảo rollback toàn bộ. `RuntimeException` hay cần `rollbackFor`?

### Lời giải

```java
@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional   // KHÔNG cần rollbackFor vì dùng RuntimeException (xem giải thích)
    public Order placeOrder(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm: " + productId));

        if (product.getStock() < quantity) {
            // RuntimeException (unchecked) - Spring TỰ ĐỘNG rollback, KHÔNG cần khai báo thêm gì
            throw new InsufficientStockException(
                "Không đủ tồn kho cho sản phẩm " + productId +
                ". Còn: " + product.getStock() + ", cần: " + quantity);
        }

        product.setStock(product.getStock() - quantity);   // Hibernate tự UPDATE nhờ dirty checking, không cần gọi save()

        Order order = new Order();
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.CREATED);

        return orderRepository.save(order);
    }
}

class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) { super(message); }
}
```

### Giải thích — `RuntimeException` hay cần `rollbackFor`?

- **Mặc định của `@Transactional`: chỉ rollback khi gặp `RuntimeException` (unchecked) hoặc `Error`** — với **checked exception** (kế thừa `Exception` nhưng KHÔNG kế thừa `RuntimeException`), Spring **KHÔNG TỰ ĐỘNG rollback** trừ khi khai báo tường minh `@Transactional(rollbackFor = MyCheckedException.class)`.
- Trong bài này, `InsufficientStockException` được thiết kế kế thừa `RuntimeException` **CÓ CHỦ ĐÍCH** — đây chính là lý do **KHÔNG CẦN** thêm `rollbackFor`: khi exception này được ném ra, Spring's transaction interceptor tự động đánh dấu transaction là `rollback-only` và thực hiện `ROLLBACK` khi thoát khỏi method — toàn bộ thay đổi (kể cả `product.setStock(...)` đã "dirty" trong persistence context nhưng CHƯA flush xuống DB) đều bị hủy bỏ, không có sản phẩm nào bị trừ tồn kho "nửa vời".
- **Nếu lỡ dùng checked exception mà quên `rollbackFor`:** đây là lỗi cực kỳ nguy hiểm trong thực tế — transaction vẫn `COMMIT` bình thường dù có exception, dẫn tới **trạng thái dữ liệu bất nhất** (VD: tồn kho đã bị trừ nhưng Order lại không được tạo do lỗi xảy ra sau đó) — nguyên tắc an toàn: **luôn ưu tiên dùng `RuntimeException` (hoặc con cháu của nó) cho các exception nghiệp vụ cần rollback**, tránh phải nhớ khai báo `rollbackFor` ở khắp nơi.

---

## Bài 4 — `LazyInitializationException` và cách sửa bằng DTO Projection

### Đề
Minh họa lỗi `LazyInitializationException` (load trong `@Transactional`, trả ra ngoài, truy cập LAZY ở method khác không transaction). Sửa bằng DTO Projection.

### Lời giải — code LỖI

```java
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category loadCategory(Long id) {
        // Bên trong transaction: session Hibernate CÒN MỞ, entity ở trạng thái "managed"
        return categoryRepository.findById(id).orElseThrow();
        // Khi method này RETURN, transaction COMMIT, session Hibernate ĐÓNG LẠI
        // -> entity trả về chuyển sang trạng thái "detached" (rời khỏi persistence context)
    }

    // Method này KHÔNG có @Transactional - gọi ở tầng Controller chẳng hạn
    public void printProducts(Long id) {
        Category category = loadCategory(id);   // entity đã "detached", session đã đóng

        // BOOM! LazyInitializationException:
        // "could not initialize proxy - no Session" -
        // vì products là LAZY, Hibernate cần session ĐANG MỞ để chạy query lazy-load,
        // nhưng session đã đóng từ lúc loadCategory() return
        System.out.println(category.getProducts().size());
    }
}
```

### Lời giải — sửa bằng DTO Projection

```java
public record CategoryDetailDto(Long id, String name, List<String> productNames) {}

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)   // load VÀ truy cập LAZY collection TRONG CÙNG 1 transaction
    public CategoryDetailDto loadCategoryDetail(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow();

        // Truy cập category.getProducts() NGAY TẠI ĐÂY - còn trong transaction, session còn mở -> AN TOÀN
        List<String> productNames = category.getProducts().stream()
                .map(Product::getName)
                .toList();

        // Trả về DTO thuần (không phải Entity) - KHÔNG còn phụ thuộc vào session/lazy-loading nữa
        return new CategoryDetailDto(category.getId(), category.getName(), productNames);
    }

    // Method gọi bên ngoài giờ đây AN TOÀN - chỉ làm việc với DTO, không đụng gì tới Hibernate session
    public void printProducts(Long id) {
        CategoryDetailDto dto = loadCategoryDetail(id);
        System.out.println(dto.productNames().size());   // KHÔNG lỗi
    }
}
```

### Giải thích

- **Nguyên nhân gốc rễ:** `LazyInitializationException` xảy ra vì **vòng đời của Entity (managed → detached) không khớp với thời điểm truy cập quan hệ LAZY** — Entity chỉ có thể lazy-load quan hệ khi CÒN nằm trong 1 Persistence Context (session) ĐANG MỞ, tương ứng với 1 transaction ĐANG CHẠY.
- **DTO Projection giải quyết TẬN GỐC** (không phải chỉ "vá lỗi") — bằng cách đảm bảo **MỌI truy cập vào quan hệ LAZY đều xảy ra BÊN TRONG transaction** (ngay trong tầng Service, method `@Transactional`), rồi chuyển đổi hoàn toàn sang object DTO thuần túy (không còn proxy Hibernate, không còn phụ thuộc session) trước khi trả ra ngoài — tầng gọi bên ngoài (Controller, hoặc method khác) không bao giờ còn cơ hội chạm vào 1 Entity "detached" có quan hệ LAZY chưa load.
- **Giải pháp thay thế khác (ít khuyến khích hơn):** đổi `FetchType.LAZY` thành `EAGER` — nhưng cách này ảnh hưởng TOÀN BỘ nơi dùng Entity đó (kể cả những nơi KHÔNG cần `products`), gây lãng phí hiệu năng lan rộng — không linh hoạt bằng DTO Projection (chỉ load ĐÚNG những gì cần cho từng use-case cụ thể).

---

## Bài 5 — `@ManyToMany` refactor thành Entity trung gian `Enrollment`

### Đề
`Student`↔`Course` qua `@ManyToMany`, rồi refactor thành 2 `@ManyToOne` qua `Enrollment` (có `enrolledDate`, `grade`). Vì sao cách 2 tốt hơn?

### Lời giải — Bản đầu: `@ManyToMany` thuần

```java
@Entity
public class Student {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany
    @JoinTable(name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

### Lời giải — Refactor: Entity trung gian `Enrollment`

```java
@Entity
public class Enrollment {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDate enrolledDate;   // KHÔNG THỂ đặt field này ở đâu trong @ManyToMany thuần!
    private String grade;
}

@Entity
public class Student {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
}

@Entity
public class Course {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
}
```

### Giải thích — vì sao cách 2 tốt hơn trong thực tế

- **Lý do quan trọng nhất — `@ManyToMany` KHÔNG THỂ lưu thêm dữ liệu về CHÍNH mối quan hệ đó:** bảng trung gian tự sinh bởi `@JoinTable` chỉ có đúng 2 cột khóa ngoại (`student_id`, `course_id`) — **không có chỗ nào** để lưu `enrolledDate` (ngày ghi danh) hay `grade` (điểm số) — đây là dữ liệu gắn liền với MỐI QUAN HỆ (1 sinh viên ghi danh 1 khóa học vào NGÀY nào, đạt ĐIỂM bao nhiêu), không thuộc về riêng `Student` hay riêng `Course`. Đây là dấu hiệu kinh điển cho thấy quan hệ N-N cần được "nâng cấp" thành Entity trung gian tường minh.
- **Kiểm soát TỐT HƠN về hiệu năng và ngữ nghĩa:** với `@ManyToMany`, khó fetch chọn lọc (VD chỉ muốn lấy các Enrollment của học kỳ hiện tại) — với Entity `Enrollment` riêng, có thể viết `EnrollmentRepository.findByStudentIdAndSemester(...)` linh hoạt như bất kỳ Entity nào khác.
- **Tránh vấn đề hiệu năng cố hữu của `@ManyToMany` khi cần XÓA 1 phần tử:** Hibernate xử lý xóa 1 phần tử khỏi `Set<Course>` bằng cách **XÓA TOÀN BỘ bảng trung gian liên quan rồi INSERT LẠI TỪ ĐẦU** (không có `DELETE` chọn lọc đúng dòng) — cực kỳ kém hiệu quả với collection lớn; với Entity `Enrollment` tường minh, `DELETE` 1 dòng Enrollment cụ thể là thao tác bình thường, hiệu quả, không ảnh hưởng gì tới các Enrollment khác.
- **Nguyên tắc chung được khuyến nghị rộng rãi trong cộng đồng JPA/Hibernate (kể cả tài liệu chính thức):** hạn chế dùng `@ManyToMany` thuần trong dự án thực tế lâu dài — hầu hết quan hệ N-N trong nghiệp vụ thực đều sớm muộn cần thêm thuộc tính riêng cho mối quan hệ, nên **thiết kế Entity trung gian TƯỜNG MINH ngay từ đầu** thường là lựa chọn bền vững hơn.

---

## Bài 6 — Optimistic Locking cho bài toán trừ tồn kho

### Đề
Thêm `@Version` vào `Product`. `reduceStock()` với Optimistic Locking. Kịch bản 2 request đồng thời — điều gì xảy ra, xử lý gì để tránh lỗi 500 khó hiểu.

### Lời giải

```java
@Entity
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int stock;

    @Version   // Hibernate tự động quản lý: tự tăng sau mỗi UPDATE, tự thêm điều kiện WHERE version = ? khi UPDATE
    private Long version;

    // getter/setter...
}

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void reduceStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm: " + productId));

        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Không đủ tồn kho");
        }

        product.setStock(product.getStock() - quantity);
        // Khi transaction commit, Hibernate sinh:
        // UPDATE products SET stock = ?, version = version + 1 WHERE id = ? AND version = ?
        // (dirty checking tự động flush, KHÔNG cần gọi save() tường minh)
    }
}

// Xử lý ở tầng Controller/GlobalExceptionHandler - tránh lỗi 500 khó hiểu cho người dùng
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        // KHÔNG trả 500 Internal Server Error (gây hiểu lầm "lỗi hệ thống")
        // Trả 409 CONFLICT - đúng ngữ nghĩa HTTP: "xung đột trạng thái tài nguyên"
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Sản phẩm vừa được cập nhật bởi người khác, vui lòng thử lại.");
    }
}
```

### Giải thích — kịch bản 2 request đồng thời

1. Request A và Request B **cùng đọc** `Product` (id=1) tại thời điểm `stock = 10`, `version = 5`.
2. Request A `COMMIT` trước: Hibernate sinh `UPDATE products SET stock = 8, version = 6 WHERE id = 1 AND version = 5` — điều kiện `version = 5` **KHỚP** (chưa ai đổi) → **UPDATE THÀNH CÔNG**, 1 dòng bị ảnh hưởng, `version` trong DB giờ là `6`.
3. Request B `COMMIT` sau: Hibernate sinh `UPDATE products SET stock = 7, version = 6 WHERE id = 1 AND version = 5` — nhưng `version` trong DB **ĐÃ LÀ 6** (do Request A vừa đổi), điều kiện `version = 5` **KHÔNG khớp** → **0 dòng bị ảnh hưởng** → Hibernate phát hiện, ném `ObjectOptimisticLockingFailureException`.
4. **Nếu KHÔNG xử lý gì thêm:** exception này lan ra tới tầng Controller, Spring Boot mặc định trả về **HTTP 500 Internal Server Error** kèm stack trace khó hiểu — trải nghiệm rất tệ với người dùng cuối (họ không hiểu "Internal Server Error" nghĩa là gì, tưởng hệ thống bị lỗi thật).
5. **Cách xử lý đúng (đã viết ở `GlobalExceptionHandler` trên):** bắt riêng `ObjectOptimisticLockingFailureException`, trả về **HTTP 409 Conflict** với thông báo RÕ RÀNG, DỄ HIỂU ("sản phẩm vừa được cập nhật, thử lại") — đúng bản chất ngữ nghĩa HTTP (409 = xung đột trạng thái tài nguyên, không phải lỗi hệ thống) và cho phép tầng client (frontend) có thể tự động **RETRY** (thử lại request, lúc này sẽ đọc được `version = 6` mới nhất và thử update lại) — mang lại trải nghiệm mượt mà hơn nhiều so với để lộ lỗi 500 thô.

---

*Đây là lời giải cho toàn bộ Phần B của Module 20. Tiếp theo: Module 13 — Spring Core & IoC/DI.*
