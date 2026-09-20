# Lời giải đầy đủ — Module 18: Testing

> Nguồn đề: `26-testing/26-testing.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Unit Test `AuthService.login()` với Mockito

### Đề
3 trường hợp: đăng nhập thành công, sai password (`BadCredentialsException`), user không tồn tại (`UserNotFoundException`).

### Lời giải

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_thanhCong_traVeUser() {
        // Arrange
        User user = new User("alice@example.com", "hashed-password");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);

        // Act
        User result = authService.login("alice@example.com", "plain-password");

        // Assert
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void login_saiPassword_throwBadCredentialsException() {
        User user = new User("alice@example.com", "hashed-password");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice@example.com", "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userKhongTonTai_throwUserNotFoundException() {
        when(userRepository.findByEmail("khong-ton-tai@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("khong-ton-tai@example.com", "any-password"))
                .isInstanceOf(UserNotFoundException.class);

        // Verify: KHÔNG BAO GIỜ được gọi passwordEncoder.matches() nếu user không tồn tại
        // (tránh NullPointerException, và đúng thứ tự logic: kiểm tra tồn tại TRƯỚC khi so password)
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
```

### Giải thích

- **`@Mock` cho `UserRepository`/`PasswordEncoder`, `@InjectMocks` cho `AuthService`:** đây là Unit Test **THUẦN TÚY, CÔ LẬP HOÀN TOÀN** — không có Spring Context nào được khởi động (khác hẳn `@SpringBootTest`), không chạm DB thật — chạy cực nhanh (mili-giây), phù hợp kiểm thử LOGIC nghiệp vụ độc lập với hạ tầng.
- **`when(...).thenReturn(...)`** định nghĩa hành vi GIẢ LẬP của mock — khi `authService.login()` gọi `userRepository.findByEmail(...)` bên trong, nó nhận về ĐÚNG giá trị đã "lập trình sẵn", không thực sự chạm database.
- **`verify(passwordEncoder, never()).matches(any(), any())`** ở trường hợp thứ 3 là điểm kiểm tra QUAN TRỌNG không chỉ dừng ở "exception có ném ra không" mà còn xác nhận **THỨ TỰ LOGIC ĐÚNG** — nếu code service lỡ gọi `passwordEncoder.matches()` TRƯỚC khi kiểm tra user tồn tại, sẽ gây `NullPointerException` (vì `user.getPassword()` trên `null`) thay vì `UserNotFoundException` đúng nghĩa — verify này giúp phát hiện SỚM lỗi thiết kế logic, không chỉ dựa vào exception cuối cùng ném ra.
- **`assertThatThrownBy` (AssertJ)** rõ ràng, dễ đọc hơn `assertThrows` (JUnit thuần) khi cần assert thêm chi tiết (message, cause...) — phong cách fluent API đọc gần giống câu văn tự nhiên.

---

## Bài 2 — `@ParameterizedTest` + `@CsvSource` cho `validatePassword()`

### Đề
`validatePassword(String password)` trả `true`/`false` dựa trên độ dài tối thiểu 8 ký tự. ≥4-5 bộ dữ liệu test.

### Lời giải

```java
class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @ParameterizedTest(name = "password=\"{0}\" -> expected={1}")
    @CsvSource({
        "12345678, true",       // đúng 8 ký tự - biên (boundary) hợp lệ
        "1234567,  false",      // 7 ký tự - THIẾU 1 ký tự so với biên - test case QUAN TRỌNG NHẤT
        "abcdefgh, true",       // 8 ký tự chữ - hợp lệ
        "'',       false",      // rỗng hoàn toàn - trường hợp cực đoan (edge case)
        "abcdefghijklmnop, true" // dài hơn nhiều so với tối thiểu - vẫn hợp lệ
    })
    void validatePassword_kiemTraDoDaiToiThieu(String password, boolean expected) {
        assertThat(validator.validatePassword(password)).isEqualTo(expected);
    }
}
```

### Giải thích

- **Bộ dữ liệu bao phủ đúng nguyên tắc "Boundary Value Analysis" (Phân tích giá trị biên):** thay vì test ngẫu nhiên nhiều độ dài password, tập trung vào **NGAY TẠI RANH GIỚI** của điều kiện (`length >= 8`) — test case `7 ký tự` (ngay dưới biên, PHẢI `false`) và `8 ký tự` (ngay tại biên, PHẢI `true`) là 2 case **QUAN TRỌNG NHẤT**, dễ phát hiện lỗi "off-by-one" (VD code viết nhầm `length > 8` thay vì `length >= 8`) — nếu chỉ test với password 20 ký tự và password rỗng, lỗi off-by-one này sẽ **KHÔNG BAO GIỜ bị phát hiện**.
- **`@CsvSource` với chuỗi rỗng cần cú pháp đặc biệt `''`** (2 dấu nháy đơn liền nhau) — vì CSV mặc định coi giá trị trống là... trống, không phân biệt được với `null`; `''` báo cho JUnit đây là CHUỖI RỖNG THỰC SỰ (không phải thiếu giá trị).
- **`name = "..."`** tùy chỉnh tên hiển thị của từng test case trong báo cáo test (IDE/CI) — giúp đọc kết quả test dễ dàng hơn nhiều so với tên mặc định `[1] 12345678, true` khó phân biệt khi có nhiều case.
- **1 method test DUY NHẤT chạy 5 LẦN** (mỗi bộ dữ liệu 1 lần, độc lập) — nếu 1 case FAIL, các case khác VẪN CHẠY BÌNH THƯỜNG và báo cáo riêng biệt — hiệu quả hơn hẳn viết 5 method `@Test` riêng lẻ trùng lặp logic assert.

---

## Bài 3 — `MockMvc` test cho `POST /api/v1/products`

### Đề
(a) tạo thành công trả `201` kèm header `Location`; (b) thiếu field `name` bắt buộc trả `400`.

### Lời giải

```java
@WebMvcTest(ProductController.class)   // CHỈ load tầng web (Controller), MOCK toàn bộ tầng Service bên dưới
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean   // thay thế bean thật bằng mock trong Spring Context - Controller vẫn @Autowired bình thường
    private ProductService productService;

    @Test
    void createProduct_thanhCong_tra201KemLocationHeader() throws Exception {
        CreateProductRequest request = new CreateProductRequest("iPhone 15", new BigDecimal("25000000"), "dien-thoai");
        ProductResponse response = new ProductResponse(1L, "iPhone 15", new BigDecimal("25000000"), "dien-thoai");

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/products/1"))
                .andExpect(jsonPath("$.name").value("iPhone 15"));
    }

    @Test
    void createProduct_thieuFieldName_tra400() throws Exception {
        // request KHÔNG có "name" - vi phạm @NotBlank trên field name của CreateProductRequest
        String invalidJson = """
                {
                    "price": 25000000,
                    "category": "dien-thoai"
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
```

### Giải thích

- **`@WebMvcTest(ProductController.class)`** chỉ khởi động **DispatcherServlet + Controller + các thành phần liên quan tới tầng web** (validation, exception handler, filter...) — **KHÔNG khởi động** toàn bộ Spring Context (không có DataSource, không có Repository thật) — nhanh hơn NHIỀU so với `@SpringBootTest` đầy đủ, phù hợp test RIÊNG tầng Controller.
- **`@MockBean`** thay thế `ProductService` thật bằng mock TRONG Spring Context — Controller vẫn nhận được bean qua DI bình thường, nhưng hành vi được kiểm soát hoàn toàn qua `when(...)` — cô lập test khỏi logic nghiệp vụ thật (đã test riêng ở tầng Service — Bài 1).
- **`header().string("Location", "/api/v1/products/1")`** kiểm tra ĐÚNG chuẩn REST cho response `201 Created` — header `Location` PHẢI trỏ tới URI của tài nguyên VỪA TẠO — đây là quy ước HTTP chuẩn thường bị bỏ sót khi implement.
- **Test case (b) KHÔNG cần `@MockBean` trả về gì** vì request bị chặn NGAY Ở TẦNG VALIDATION (`@Valid` trên `CreateProductRequest`), **TRƯỚC KHI** code trong `ProductController`/`ProductService` được gọi tới — đây chính là minh chứng thực tế cho luồng xử lý `GlobalExceptionHandler` (`MethodArgumentNotValidException`) đã viết ở phần API Design.

---

## Bài 4 — `@DataJpaTest` cho `ProductRepository`

### Đề
`findByCategoryAndPriceLessThan`, dùng `TestEntityManager` chuẩn bị dữ liệu.

### Lời giải

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryAndPriceLessThan(String category, BigDecimal price);
}

@DataJpaTest   // CHỈ load tầng JPA (Repository, Entity, DataSource TEST - mặc định dùng H2 in-memory)
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;   // công cụ chuyên dụng để chuẩn bị dữ liệu test, thay cho Repository

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByCategoryAndPriceLessThan_traVeDungSanPham() {
        // Arrange - dùng TestEntityManager để chèn dữ liệu TRỰC TIẾP, không qua logic Repository (tránh phụ thuộc vòng)
        Product cheap = new Product(null, "Áo thun", new BigDecimal("150000"), "quan-ao");
        Product expensive = new Product(null, "Áo khoác da", new BigDecimal("2000000"), "quan-ao");
        Product otherCategory = new Product(null, "Sách Clean Code", new BigDecimal("100000"), "sach");

        entityManager.persist(cheap);
        entityManager.persist(expensive);
        entityManager.persist(otherCategory);
        entityManager.flush();   // đẩy xuống DB NGAY - đảm bảo dữ liệu THỰC SỰ có mặt trước khi query

        // Act
        List<Product> result = productRepository.findByCategoryAndPriceLessThan("quan-ao", new BigDecimal("500000"));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Áo thun");
    }
}
```

### Giải thích

- **`@DataJpaTest`** tự động cấu hình DataSource TRỎ VỀ **H2 in-memory** (mặc định), chỉ khởi động các bean liên quan tới JPA (Repository, `EntityManager`) — không load Controller/Service, nhanh hơn nhiều so với `@SpringBootTest` toàn bộ ứng dụng.
- **`TestEntityManager` (không phải `productRepository.save()`)** là công cụ CHUYÊN DÙNG để chuẩn bị dữ liệu test — lý do: nếu dùng CHÍNH `productRepository.save()` để chuẩn bị dữ liệu RỒI LẠI dùng `productRepository.findBy...()` để test, thì bài test **PHỤ THUỘC VÀO CHÍNH TÍNH ĐÚNG ĐẮN của `save()`** — nếu `save()` có lỗi, test có thể pass/fail SAI LÝ DO, khó xác định lỗi nằm ở đâu. `TestEntityManager` tách biệt rõ "chuẩn bị dữ liệu" (Arrange) khỏi "hành vi đang test" (Act).
- **`entityManager.flush()` bắt buộc** — Hibernate mặc định GOM các thao tác ghi lại trong Persistence Context, chỉ thực sự chạy SQL khi cần thiết (VD lúc transaction commit, hoặc trước khi chạy 1 query mới) — `flush()` ÉP đẩy dữ liệu xuống DB NGAY, đảm bảo câu `findByCategoryAndPriceLessThan` sau đó THẬT SỰ query trên dữ liệu đã có, không bị race condition giữa "đã persist trong bộ nhớ" và "đã có trong DB".
- **`@DataJpaTest` tự động rollback sau MỖI test method** (bọc trong `@Transactional` ngầm định) — đảm bảo các test ĐỘC LẬP HOÀN TOÀN với nhau, không cần tự viết code dọn dẹp dữ liệu (cleanup) sau mỗi test.

---

## Bài 5 — Tình huống H2 PASS nhưng Testcontainers (PostgreSQL thật) FAIL

### Đề
Giải thích tình huống cụ thể — liên hệ JSONB hoặc hàm SQL đặc thù (Module 11 — RDBMS & NoSQL).

### Lời giải — minh họa bằng code

```java
@Entity
public class Product {
    @Id @GeneratedValue
    private Long id;

    // JSONB - kiểu dữ liệu ĐẶC THÙ CỦA POSTGRESQL (đã dùng ở Module 11, Bài 1)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes;
}

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Dùng toán tử JSONB ĐẶC THÙ CỦA POSTGRESQL: ->> để truy vấn bên trong cột JSON
    @Query(value = "SELECT * FROM products WHERE attributes ->> 'ram_gb' = :ram", nativeQuery = true)
    List<Product> findByRamGb(@Param("ram") String ram);
}
```

```java
// Test dùng @DataJpaTest (mặc định H2) - SẼ FAIL NGAY LẬP TỨC, KHÔNG PHẢI "false positive PASS"
// vì H2 KHÔNG HIỂU cú pháp toán tử "->>"  - báo lỗi cú pháp SQL ngay khi chạy
// (Đây là ví dụ "native query" - lộ rõ ngay. Tình huống "PASS giả" tinh vi hơn nằm ở CHỖ KHÁC - xem bên dưới)

// ===== Tình huống THỰC SỰ nguy hiểm: PASS ở H2, FAIL ở PostgreSQL thật =====

public interface ProductRepository2 extends JpaRepository<Product, Long> {

    // Query TƯỞNG NHƯ "chuẩn SQL thông thường", nhưng cách 2 DB xử lý PHÂN BIỆT HOA/THƯỜNG KHÁC NHAU
    @Query(value = "SELECT * FROM products WHERE name = :name", nativeQuery = true)
    List<Product> findByExactName(@Param("name") String name);
}

// Giả sử dữ liệu trong DB có tên "iPhone 15"
// Test gọi: findByExactName("iphone 15")  (chữ thường, KHÔNG khớp hoa/thường)
//
// - H2 (mặc định): tùy cấu hình, MẶC ĐỊNH so sánh CASE-INSENSITIVE trong một số ngữ cảnh
//   collation mặc định -> query CÓ THỂ trả về kết quả khớp "iPhone 15" -> Test PASS (SAI LẦM)
// - PostgreSQL thật (Testcontainers): mặc định collation CASE-SENSITIVE cho so sánh "="
//   -> "iphone 15" != "iPhone 15" -> KHÔNG khớp -> query trả về RỖNG -> Test FAIL
//   -> ĐÂY MỚI LÀ HÀNH VI ĐÚNG với môi trường production THẬT (cũng chạy PostgreSQL)
```

### Giải thích

- **Nguyên nhân gốc rễ:** H2 in-memory được thiết kế để **MÔ PHỎNG GẦN ĐÚNG** hành vi SQL chuẩn, nhưng **KHÔNG PHẢI BẢN SAO CHÍNH XÁC 100%** của bất kỳ RDBMS cụ thể nào — sự khác biệt về **collation** (quy tắc so sánh chuỗi — phân biệt hay không phân biệt hoa/thường), **kiểu dữ liệu đặc thù** (JSONB, `TSTZRANGE`, `ARRAY` của PostgreSQL không tồn tại trong chuẩn SQL chung), và **hàm SQL riêng của từng vendor** (`EXCLUDE CONSTRAINT`, `->>`, `RETURNING`...) là những điểm khác biệt kinh điển.
- **Hệ quả nguy hiểm nhất KHÔNG phải là "test fail rõ ràng"** (dễ phát hiện, dễ sửa) mà là trường hợp **"PASS SAI" ở H2** — test XANH (green) tạo cảm giác AN TÂM giả tạo, nhưng khi code THẬT chạy trên PostgreSQL production, hành vi lại KHÁC — đây chính là lý do quan trọng nhất khiến **Testcontainers** (chạy PostgreSQL THẬT trong Docker container, chỉ tồn tại trong lúc chạy test) được khuyến nghị mạnh mẽ cho các dự án dùng nhiều tính năng đặc thù của 1 RDBMS cụ thể — đảm bảo môi trường TEST khớp CHÍNH XÁC với môi trường PRODUCTION, loại bỏ hoàn toàn rủi ro "works on H2, breaks on Postgres".
- **Nguyên tắc thực hành rút ra:** với dự án CHỈ dùng các tính năng SQL chuẩn CƠ BẢN (không dùng JSONB, Range Type, hàm đặc thù...), `@DataJpaTest` với H2 vẫn đủ tin cậy và NHANH HƠN đáng kể (không cần khởi động Docker container) — nhưng ngay khi dự án bắt đầu dùng các tính năng "vendor-specific" (như capstone Flash-Sale đã thiết kế ở Module 11 với `TSTZRANGE`/`EXCLUDE CONSTRAINT`/JSONB), Testcontainers trở thành **BẮT BUỘC**, không còn là tùy chọn.

---

## Bài 6 — `MockMvc` + `@WithMockUser` cho `DELETE /api/v1/admin/orders/{id}`

### Đề
3 trường hợp: ADMIN trả `204`, USER trả `403`, không đăng nhập trả `401`.

### Lời giải

```java
@WebMvcTest(AdminOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
// Import CẤU HÌNH SECURITY THẬT vào test - để @PreAuthorize THỰC SỰ được áp dụng và kiểm tra đúng đắn
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "ADMIN")   // giả lập user ĐÃ ĐĂNG NHẬP với role ADMIN - KHÔNG cần JWT thật
    void deleteOrder_voiRoleAdmin_tra204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders/1"))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(1L);
    }

    @Test
    @WithMockUser(roles = "USER")   // giả lập user đăng nhập nhưng CHỈ role USER - không đủ quyền
    void deleteOrder_voiRoleUser_tra403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders/1"))
                .andExpect(status().isForbidden());

        // Verify: Service KHÔNG BAO GIỜ được gọi - @PreAuthorize chặn NGAY TỪ TRƯỚC,
        // đảm bảo request KHÔNG "lọt" tới tầng nghiệp vụ dù chỉ 1 phần
        verify(orderService, never()).deleteOrder(anyLong());
    }

    @Test
    // KHÔNG có @WithMockUser - mô phỏng request KHÔNG kèm bất kỳ thông tin xác thực nào
    void deleteOrder_khongDangNhap_tra401() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders/1"))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).deleteOrder(anyLong());
    }
}
```

### Giải thích

- **`@WithMockUser(roles = "ADMIN")`** là annotation TIỆN LỢI của Spring Security Test — TỰ ĐỘNG tạo sẵn 1 `Authentication` object giả lập với role chỉ định, đặt vào `SecurityContext` TRƯỚC KHI request chạy — **KHÔNG CẦN** phải thực sự generate JWT thật, gọi `/login`, hay mock `JwtAuthenticationFilter` phức tạp — đơn giản hóa việc test riêng LOGIC PHÂN QUYỀN (`@PreAuthorize`) mà không phụ thuộc vào cơ chế xác thực cụ thể (JWT/session/OAuth2...).
- **`@Import(SecurityConfig.class, ...)` cần thiết** vì mặc định `@WebMvcTest` **KHÔNG tự động áp dụng** cấu hình Security tùy chỉnh đầy đủ — cần import tường minh để đảm bảo `@PreAuthorize` trên `OrderService` (hoặc filter chain rules) THỰC SỰ được kích hoạt trong ngữ cảnh test, nếu không test sẽ luôn PASS 200 (do không có bất kỳ rule bảo mật nào được áp dụng) bất kể role gì — 1 lỗi test "giả PASS" nguy hiểm.
- **`verify(orderService, never()).deleteOrder(...)`** ở 2 trường hợp lỗi (403/401) là phép kiểm tra QUAN TRỌNG, không chỉ dừng lại ở "status code đúng" — xác nhận rằng **security filter chặn request TRƯỚC KHI** nó có cơ hội chạm tới tầng nghiệp vụ (dù chỉ 1 phần nhỏ logic) — đảm bảo KHÔNG có "rò rỉ" thực thi nghiệp vụ ngoài ý muốn dù response trả lỗi đúng.
- **Không có test case `@WithMockUser` cho trường hợp thứ 3 (401)** là ĐÚNG CHỦ ĐÍCH — chính vì KHÔNG có bất kỳ thông tin xác thực nào trong request, đúng mô phỏng tình huống "chưa đăng nhập" cần kiểm tra.

---

*Đây là lời giải cho toàn bộ Phần B của Module 26. Tiếp theo: Module 19 — Caching & Messaging.*
