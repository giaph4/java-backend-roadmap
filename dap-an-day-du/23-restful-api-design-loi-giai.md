# Lời giải đầy đủ — Module 15: RESTful API Design

> Nguồn đề: `23 restful api design/23-restful-api-design.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Thiết kế URI + HTTP method cho hệ thống Thư viện

### Đề
`Book`, `Author`, `Member`, `Loan` theo chuẩn REST — bao gồm action không-CRUD "gia hạn mượn sách".

### Lời giải

```
# ===== Book (tài nguyên gốc) =====
GET    /api/v1/books                  Lấy danh sách sách (hỗ trợ pagination/filter)
GET    /api/v1/books/{id}             Lấy chi tiết 1 sách
POST   /api/v1/books                  Tạo sách mới
PUT    /api/v1/books/{id}             Cập nhật TOÀN BỘ thông tin sách
PATCH  /api/v1/books/{id}             Cập nhật MỘT PHẦN thông tin sách
DELETE /api/v1/books/{id}             Xóa sách

# ===== Author =====
GET    /api/v1/authors                Lấy danh sách tác giả
GET    /api/v1/authors/{id}           Lấy chi tiết 1 tác giả
POST   /api/v1/authors                Tạo tác giả mới

# ===== Quan hệ lồng nhau (nested resource) - sách của 1 tác giả cụ thể =====
GET    /api/v1/authors/{id}/books     Lấy danh sách sách của tác giả này

# ===== Member =====
GET    /api/v1/members                Lấy danh sách thành viên
GET    /api/v1/members/{id}           Lấy chi tiết 1 thành viên
POST   /api/v1/members                Đăng ký thành viên mới

# ===== Loan (mượn sách) =====
GET    /api/v1/loans                  Lấy danh sách phiếu mượn (hỗ trợ filter theo memberId/status)
GET    /api/v1/loans/{id}             Lấy chi tiết 1 phiếu mượn
POST   /api/v1/loans                  Tạo phiếu mượn mới (mượn sách)
                                       Body: { "bookId": ..., "memberId": ... }

# ===== Action KHÔNG-CRUD: "gia hạn mượn sách" =====
POST   /api/v1/loans/{id}/renew       Gia hạn phiếu mượn (action, không phải CRUD thuần)

# ===== Action KHÔNG-CRUD khác cùng kiểu (tham khảo, không bắt buộc trong đề): "trả sách" =====
POST   /api/v1/loans/{id}/return      Đánh dấu đã trả sách
```

### Giải thích

- **`POST /loans/{id}/renew` (không phải `PUT /loans/{id}` với body chứa "action: renew"):** đây là nguyên tắc quan trọng khi thiết kế REST cho **hành động nghiệp vụ không thuần CRUD** — mô hình hóa hành động đó như 1 **sub-resource dạng động từ** gắn vào tài nguyên gốc qua `POST`. Dùng `PUT`/`PATCH` với 1 field "action" trong body (`{"action": "renew"}`) là **anti-pattern phổ biến** — vi phạm nguyên tắc REST vì `PUT` có ngữ nghĩa "thay thế toàn bộ trạng thái tài nguyên", không phù hợp để biểu diễn 1 HÀNH VI nghiệp vụ cụ thể (gia hạn có logic riêng: kiểm tra số lần gia hạn tối đa, tính lại hạn trả...).
- **`GET /authors/{id}/books` (nested resource):** biểu diễn đúng quan hệ ngữ nghĩa "sách THUỘC VỀ tác giả này" — khác với `GET /books?authorId=...` (dùng query param filter, cũng là cách hợp lệ) — nested resource phù hợp khi mối quan hệ CHA-CON RÕ RÀNG và bản thân sub-resource không có ý nghĩa tồn tại độc lập ngoài ngữ cảnh cha; ở đây `Book` vẫn có endpoint độc lập `GET /books/{id}` vì Book CÓ ý nghĩa truy cập trực tiếp, nên cả 2 cách đều hợp lý tùy ngữ cảnh sử dụng.
- **`POST /loans`** (không phải `POST /books/{id}/loans` hay `POST /members/{id}/loans`): vì `Loan` là 1 **tài nguyên có ý nghĩa độc lập** (liên kết cả `bookId` VÀ `memberId`), không thuộc về riêng 1 trong 2 — thông tin liên kết được truyền qua **body**, không qua URI.

---

## Bài 2 — `GlobalExceptionHandler` xử lý 3 loại exception

### Đề
`ResourceNotFoundException` (404), `DuplicateResourceException` (409), validation lỗi từ `@Valid` (400) — theo format Error Response chuẩn.

### Lời giải

```java
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        List<String> details   // dùng cho validation - liệt kê từng lỗi field cụ thể
) {}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)   // exception Spring tự ném khi @Valid thất bại
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Dữ liệu đầu vào không hợp lệ",
                LocalDateTime.now(),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### Giải thích

- **`@RestControllerAdvice`** gom TOÀN BỘ logic xử lý exception vào **1 nơi duy nhất, áp dụng cho MỌI `@RestController`** trong ứng dụng — tránh phải viết `try-catch` lặp lại ở từng Controller, tuân thủ nguyên tắc **DRY (Don't Repeat Yourself)** và **Separation of Concerns** (Controller chỉ lo điều phối request/response, không lo xử lý lỗi chi tiết).
- **`MethodArgumentNotValidException`** là exception **CHUẨN CỦA SPRING**, tự động được ném ra khi 1 `@RequestBody` gắn `@Valid` thất bại validate (VD `@NotBlank`, `@Email`...) — cần bắt ĐÚNG loại exception này (không phải tự định nghĩa) để tận dụng cơ chế validate tích hợp sẵn của Spring.
- **`details: List<String>`** cho phép trả về **CHI TIẾT TỪNG FIELD LỖI** (VD `"email: phải đúng định dạng email"`, `"age: phải lớn hơn 0"`) thay vì chỉ 1 message chung chung — giúp client (frontend) hiển thị lỗi CHÍNH XÁC ngay dưới từng ô input tương ứng, trải nghiệm người dùng tốt hơn nhiều.
- **HTTP status code phản ánh ĐÚNG BẢN CHẤT lỗi:** `404` (tài nguyên không tồn tại) khác hẳn `409` (xung đột trạng thái — VD trùng email) khác hẳn `400` (dữ liệu đầu vào sai định dạng) — client có thể dựa vào status code để xử lý logic khác nhau (VD 409 gợi ý "thử email khác", 400 gợi ý "sửa lại form") mà không cần parse message.

---

## Bài 3 — `GET /products` với Pagination + Filtering + Sorting

### Đề
Pagination (`Pageable`), Filter theo `category`, khoảng giá `minPrice`/`maxPrice`, Sort theo `price`/`name`.

### Lời giải

```java
public record ProductResponse(Long id, String name, BigDecimal price, String category) {}

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        Page<ProductResponse> products = productService.findProducts(category, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(products);
    }
}

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> findProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(null);

        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        return productRepository.findAll(spec, pageable)
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getCategory()));
    }
}

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}
```

```
# Ví dụ gọi API:
GET /api/v1/products?category=dien-thoai&minPrice=5000000&maxPrice=20000000&sort=price,desc&page=0&size=10
```

### Giải thích

- **`Pageable` tự động bind từ query param chuẩn của Spring Data:** `page` (số trang, bắt đầu từ 0), `size` (số phần tử/trang), `sort` (định dạng `field,direction`, VD `price,desc`) — Spring tự động parse các query param này thành object `Pageable` mà **KHÔNG CẦN khai báo từng tham số riêng lẻ** trong method signature.
- **`Specification` (JPA Criteria API) cho phép xây dựng query ĐỘNG** — chỉ thêm điều kiện `WHERE` khi tham số filter tương ứng KHÔNG NULL — tránh phải viết nhiều method riêng cho từng tổ hợp filter (VD `findByCategory`, `findByCategoryAndPriceBetween`...), linh hoạt hơn nhiều so với tên method suy luận của Spring Data JPA khi có nhiều điều kiện filter TÙY CHỌN kết hợp.
- **`Page<ProductResponse>` (không phải `List<ProductResponse>`)** trong response — `Page` tự động bao gồm metadata quan trọng: `totalElements`, `totalPages`, `number` (trang hiện tại), `size` — client (frontend) cần các thông tin này để render UI phân trang (nút "trang tiếp theo", hiển thị "1-20 trong tổng 543 kết quả"...).
- **`@PageableDefault(size = 20, sort = "name")`:** đảm bảo có giá trị MẶC ĐỊNH hợp lý khi client KHÔNG truyền `page`/`size`/`sort` — tránh trả về TOÀN BỘ dữ liệu (không giới hạn) nếu client quên truyền `size`, đây là 1 lỗ hổng hiệu năng/bảo mật phổ biến (DoS vô tình) nếu không đặt default.

---

## Bài 4 — `POST /payments` với Idempotency Key

### Đề
Idempotency Key — luồng xử lý khi client gọi lại cùng key (Map trong bộ nhớ).

### Lời giải

```java
public record PaymentRequest(BigDecimal amount, String description) {}
public record PaymentResponse(String paymentId, BigDecimal amount, String status) {}

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    // Map minh họa - THỰC TẾ nên dùng Redis (có TTL tự động, chia sẻ được giữa nhiều instance backend)
    private final Map<String, PaymentResponse> idempotencyCache = new ConcurrentHashMap<>();

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid PaymentRequest request) {

        // BƯỚC 1: Kiểm tra key ĐÃ XỬ LÝ TRƯỚC ĐÓ chưa
        PaymentResponse cached = idempotencyCache.get(idempotencyKey);
        if (cached != null) {
            // Trả lại CHÍNH XÁC kết quả LẦN ĐẦU - KHÔNG xử lý thanh toán lần 2
            // (client có thể gọi lại do timeout mạng, retry tự động... nhưng thanh toán CHỈ xảy ra 1 lần)
            return ResponseEntity.ok(cached);
        }

        // BƯỚC 2: Chưa từng xử lý - thực hiện thanh toán THẬT
        PaymentResponse result = paymentService.processPayment(request);

        // BƯỚC 3: Lưu kết quả vào cache TRƯỚC KHI trả response, gắn với đúng idempotency key này
        idempotencyCache.put(idempotencyKey, result);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
```

```
# Client gọi (LẦN 1):
POST /api/v1/payments
Idempotency-Key: 7f9c2a4e-...
Body: {"amount": 500000, "description": "Thanh toan don hang #123"}
-> 201 Created, paymentId = "PAY-001"

# Client gọi LẠI (do timeout không nhận được response lần 1, tự động retry) - CÙNG Idempotency-Key:
POST /api/v1/payments
Idempotency-Key: 7f9c2a4e-...    (GIỐNG HỆT lần 1)
Body: {"amount": 500000, "description": "Thanh toan don hang #123"}
-> 200 OK, paymentId = "PAY-001"  (CÙNG kết quả lần 1 - KHÔNG tạo giao dịch thanh toán MỚI/TRÙNG)
```

### Giải thích

- **Vấn đề Idempotency Key giải quyết:** `POST` (tạo mới) về bản chất **KHÔNG idempotent** (gọi 2 lần tạo ra 2 tài nguyên khác nhau) — nhưng với **thanh toán**, việc client (do mất kết nối, timeout, hoặc lỗi retry tự động ở tầng HTTP client/mobile app) gọi lại request THANH TOÁN GIỐNG HỆT **2 LẦN** có thể dẫn tới **TRỪ TIỀN 2 LẦN** cho cùng 1 giao dịch — hậu quả nghiêm trọng về tài chính. Idempotency Key biến `POST /payments` thành **idempotent NHÂN TẠO**: cùng 1 key → cùng 1 kết quả, dù gọi bao nhiêu lần.
- **Client (không phải server) sinh ra Idempotency Key** (thường là UUID) TRƯỚC KHI gửi request LẦN ĐẦU — và **PHẢI DÙNG LẠI ĐÚNG KEY ĐÓ** cho mọi lần retry của CÙNG 1 ý định thanh toán — nếu client sinh key MỚI cho mỗi lần gọi (kể cả retry), cơ chế này hoàn toàn mất tác dụng.
- **Trong thực tế production, `ConcurrentHashMap` KHÔNG đủ** — cần Redis (hoặc DB) với **TTL** (tự động hết hạn sau vài giờ/ngày, tránh cache phình vô hạn) và **CHIA SẺ ĐƯỢC giữa nhiều instance backend** (nếu hệ thống scale ngang nhiều server, `Map` cục bộ trong 1 JVM không thấy được request đã xử lý ở JVM khác) — liên hệ trực tiếp vai trò Redis đã học ở Module 11 (RDBMS & NoSQL, Bài 5 — Distributed Lock).
- **Cân nhắc race condition:** nếu 2 request CÙNG idempotency key đến **GẦN NHƯ ĐỒNG THỜI** (trước khi bước 3 kịp lưu cache), cả 2 đều có thể "lọt" qua bước kiểm tra ở bước 1 và cùng xử lý thanh toán — cài đặt production-grade cần thêm bước "đặt lock/đánh dấu ĐANG XỬ LÝ" cho key đó NGAY LẬP TỨC (trước khi gọi `processPayment`) để chặn request thứ 2 đến gần như đồng thời, tương tự vấn đề đã học ở Module 10 (Database SQL, Bài 4 — race condition tầng DB).

---

## Bài 5 — Sửa API thiết kế sai `POST /api/getUserOrders`

### Đề
`POST /api/getUserOrders` (nhận `userId` trong body, trả mã lỗi trong body dù status 200). Chỉ ra ≥4 lỗi vi phạm REST, viết lại đúng chuẩn.

### Phân tích — 4 lỗi vi phạm

1. **Dùng `POST` cho hành động ĐỌC dữ liệu (get orders):** vi phạm ngữ nghĩa HTTP method — `GET` mới đúng cho thao tác chỉ đọc, không thay đổi trạng thái server; dùng `POST` khiến request KHÔNG được cache được bởi trình duyệt/proxy, và gây hiểu nhầm về mục đích API.
2. **Tên endpoint theo kiểu RPC (`getUserOrders`, dạng động từ + tên hàm) thay vì DANH TỪ SỐ NHIỀU đại diện tài nguyên:** REST chuẩn dùng URI biểu diễn **TÀI NGUYÊN** (`/orders`), không biểu diễn **HÀNH ĐỘNG** (`getUserOrders`) — vi phạm nguyên tắc "URI xác định resource, HTTP method xác định hành động".
3. **`userId` đặt trong body thay vì URI/query param:** với thao tác chỉ ĐỌC dữ liệu theo 1 định danh, `userId` nên nằm trong **URI path** (`/users/{userId}/orders`) hoặc **query param** (`/orders?userId=...`) — đặt trong body của 1 request được kỳ vọng là `GET` (không có body chuẩn) gây khó khăn cho việc cache, logging, và không tuân thủ REST convention.
4. **Trả mã lỗi trong body dù HTTP status vẫn 200 OK:** vi phạm nguyên tắc "HTTP status code phải phản ánh ĐÚNG kết quả thực tế" — client/middleware (API Gateway, monitoring tool, HTTP client library) dựa vào status code để quyết định retry/alert/log lỗi; nếu LUÔN trả `200` bất kể thành công hay lỗi, các cơ chế tự động này KHÔNG THỂ phát hiện lỗi mà không phải parse sâu vào body — vi phạm nghiêm trọng tính "self-descriptive" của REST.

### Lời giải — thiết kế đúng chuẩn

```
GET /api/v1/users/{userId}/orders

Response THÀNH CÔNG:
200 OK
[
  { "id": 1, "total": 500000, "status": "COMPLETED" },
  { "id": 2, "total": 750000, "status": "PENDING" }
]

Response LỖI (user không tồn tại):
404 Not Found
{
  "status": 404,
  "error": "Not Found",
  "message": "Không tìm thấy user với id: 999",
  "timestamp": "2026-09-11T10:00:00"
}
```

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserOrderController {

    private final OrderService orderService;

    public UserOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Long userId) {
        List<OrderResponse> orders = orderService.findOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
        // Nếu userId không tồn tại -> orderService ném ResourceNotFoundException
        // -> GlobalExceptionHandler (Bài 2) tự động trả 404 ĐÚNG CHUẨN
    }
}
```

---

## Bài 6 — ETag/Conditional Request cho `GET`/`PUT /articles/{id}`

### Đề
`GET` trả kèm `ETag`; `PUT` yêu cầu `If-Match`, trả `412 Precondition Failed` nếu không khớp (dùng field `version`).

### Lời giải

```java
public record ArticleResponse(Long id, String title, String content, long version) {}
public record UpdateArticleRequest(String title, String content) {}

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticle(@PathVariable Long id) {
        ArticleResponse article = articleService.findById(id);

        // ETag mô phỏng bằng field version - bọc trong dấu " (chuẩn HTTP ETag header)
        String etag = "\"" + article.version() + "\"";

        return ResponseEntity.ok()
                .eTag(etag)
                .body(article);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody UpdateArticleRequest request) {

        ArticleResponse current = articleService.findById(id);
        String currentEtag = "\"" + current.version() + "\"";

        // So khớp ETag client gửi lên (If-Match) với ETag HIỆN TẠI của server
        if (!currentEtag.equals(ifMatch)) {
            // Dữ liệu đã bị người khác sửa TRƯỚC ĐÓ (version không khớp) -> từ chối update
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        ArticleResponse updated = articleService.update(id, request);
        return ResponseEntity.ok()
                .eTag("\"" + updated.version() + "\"")
                .body(updated);
    }
}
```

```
# Luồng ví dụ:
# 1. Client GET để lấy bài viết hiện tại
GET /api/v1/articles/10
-> 200 OK, ETag: "5", body: {"id":10, "title":"...", "version":5}

# 2. Client sửa nội dung, gửi PUT kèm ETag đã lấy được ở bước 1
PUT /api/v1/articles/10
If-Match: "5"
Body: {"title": "Tiêu đề mới", "content": "..."}
-> 200 OK (khớp version 5) - update thành công, trả ETag: "6"

# 3. NẾU giữa bước 1 và 2, có NGƯỜI KHÁC đã sửa bài viết này trước (version DB giờ là 6)
PUT /api/v1/articles/10
If-Match: "5"    (client vẫn gửi ETag CŨ, không biết đã có người sửa)
-> 412 Precondition Failed - server TỪ CHỐI ghi đè, tránh mất thay đổi của người khác (lost update)
```

### Giải thích

- **ETag/`If-Match` giải quyết vấn đề "Lost Update"** — kịch bản: 2 người CÙNG mở 1 bài viết để sửa, người A sửa xong lưu TRƯỚC, người B (đang dựa trên bản CŨ, không biết người A đã sửa) lưu SAU — nếu KHÔNG kiểm tra gì, thay đổi của người B sẽ **GHI ĐÈ HOÀN TOÀN** lên thay đổi của người A (mất dữ liệu của A mà không ai biết) — đây chính là biến thể HTTP-level của **Optimistic Locking** đã học ở Module 12 (JPA & Hibernate, Bài 6) — về bản chất kỹ thuật GIỐNG HỆT `@Version`, chỉ khác tầng áp dụng (HTTP protocol thay vì JPA/DB).
- **`412 Precondition Failed`** là status code CHUẨN dành RIÊNG cho tình huống này — khác với `409 Conflict` (thường dùng cho xung đột NGHIỆP VỤ như trùng dữ liệu) — `412` mang ý nghĩa "điều kiện tiên quyết đính kèm trong request (header `If-Match`) không được thỏa mãn", đúng ngữ nghĩa HTTP chuẩn cho Conditional Request.
- **`If-Match` là header CHUẨN của HTTP** (không phải tự chế) — được thiết kế riêng cho mục đích Conditional Request, hoạt động thống nhất với `ETag` do server trả về ở `GET` trước đó — client hiện đại (trình duyệt, HTTP client library) đều hỗ trợ sẵn cơ chế này mà không cần code thêm gì đặc biệt phía client.
- **Liên hệ trực tiếp:** đây là lý do vì sao API thiết kế tốt LUÔN trả về `ETag`/`version` trong response của `GET` — không chỉ để hiển thị, mà còn là **tiền đề bắt buộc** để client có thể thực hiện Conditional Update an toàn ở bước tiếp theo.

---

*Đây là lời giải cho toàn bộ Phần B của Module 23. Tiếp theo: Module 16 — Spring Data & Advanced Persistence.*
