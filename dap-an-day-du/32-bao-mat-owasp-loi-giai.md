# Lời giải đầy đủ — Module 24: Bảo mật OWASP

> Nguồn đề: `32 bao mat owasp/32-bao-mat-owasp.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Sửa lỗi SQL Injection bằng Named Parameter

### Đề
```java
@Query(value = "SELECT * FROM products WHERE category = '" + "#{#category}" + "'", nativeQuery = true)
List<Product> findByCategory(@Param("category") String category);
```

### Phân tích lỗi

Đây là **SQL Injection kinh điển** — chuỗi query được **NỐI CHUỖI TRỰC TIẾP** (string concatenation) với giá trị đầu vào của người dùng — nếu `category` chứa nội dung độc hại như `"' OR '1'='1"`, câu SQL cuối cùng trở thành `SELECT * FROM products WHERE category = '' OR '1'='1'` — điều kiện `'1'='1'` LUÔN ĐÚNG, trả về **TOÀN BỘ bảng `products`** thay vì lọc theo category — nguy hiểm hơn, kẻ tấn công có thể inject `'; DROP TABLE products; --` để **XÓA TOÀN BỘ BẢNG**.

### Lời giải

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Named Parameter (:category) - Spring/Hibernate tự động dùng PreparedStatement
    // với placeholder THẬT SỰ, KHÔNG NỐI CHUỖI trực tiếp giá trị vào câu SQL
    @Query(value = "SELECT * FROM products WHERE category = :category", nativeQuery = true)
    List<Product> findByCategory(@Param("category") String category);
}
```

### Giải thích

- **Nguyên nhân gốc rễ của lỗi:** cú pháp `"#{#category}"` trong code gốc là **SpEL (Spring Expression Language)**, được Spring **ĐÁNH GIÁ (evaluate) VÀ NỐI CHUỖI TRỰC TIẾP** giá trị vào câu SQL string **TRƯỚC KHI** gửi xuống DB — điều này biến DB THÀNH KHÔNG THỂ PHÂN BIỆT được đâu là "cấu trúc câu lệnh SQL" và đâu là "dữ liệu đầu vào của người dùng" — đây chính là bản chất của MỌI lỗ hổng SQL Injection.
- **Named Parameter (`:category`) là cách SỬA ĐÚNG, KHÔNG PHẢI chỉ "che giấu" lỗi:** Hibernate dịch `:category` thành **PreparedStatement với placeholder `?`** ở tầng JDBC — DB nhận được câu lệnh SQL đã **BIÊN DỊCH SẴN CẤU TRÚC** (`SELECT * FROM products WHERE category = ?`), sau đó giá trị của `category` được gửi **RIÊNG BIỆT, dưới dạng DỮ LIỆU THUẦN TÚY** — DB **KHÔNG BAO GIỜ** diễn giải nội dung của `category` như 1 phần CẤU TRÚC câu lệnh SQL, dù giá trị đó có chứa ký tự đặc biệt (`'`, `;`, `--`...) NHƯ THẾ NÀO — về mặt kỹ thuật, đây là sự khác biệt GIỮA "string concatenation" và "parameterized query", KHÔNG PHẢI chỉ đơn thuần "escape ký tự đặc biệt" (cách tiếp cận CŨ, DỄ SAI SÓT và KHÔNG TRIỆT ĐỂ).
- **Nguyên tắc TỔNG QUÁT áp dụng cho MỌI loại query (không chỉ `nativeQuery`):** JPQL (`@Query` không có `nativeQuery = true`), `Specification`/Criteria API (Module 16, Bài 1) đều **TỰ ĐỘNG DÙNG PreparedStatement** phía dưới — hoàn toàn AN TOÀN theo mặc định — lỗi SQL Injection CHỈ xảy ra khi lập trình viên **CHỦ ĐỘNG NỐI CHUỖI THỦ CÔNG** (dù bằng `+` hay SpEL) thay vì dùng cơ chế parameter binding chuẩn của framework.

---

## Bài 2 — `GET /invoices/{id}` với kiểm tra Ownership

### Đề
Chỉ cho phép user xem invoice của CHÍNH HỌ, hoặc ADMIN xem tất cả.

### Lời giải

```java
@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    // @PostAuthorize kiểm tra SAU KHI đã load được Invoice (cần returnObject để biết chủ sở hữu THẬT SỰ)
    @PostAuthorize("hasRole('ADMIN') or returnObject.customerId == authentication.principal.id")
    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));
    }
}

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(InvoiceResponse.from(invoice));
        // NẾU không đủ quyền, @PostAuthorize đã ném AccessDeniedException TRƯỚC KHI tới được dòng này
    }
}
```

### Giải thích

- **Đây chính là ứng dụng LẶP LẠI CHÍNH XÁC pattern đã học ở Module 17 (Spring Security, Bài 4) — `@PostAuthorize` với `returnObject`**: phải dùng `@PostAuthorize` (KHÔNG PHẢI `@PreAuthorize`) vì tham số ĐẦU VÀO của method (`invoiceId`) KHÔNG ĐỦ để biết "invoice này thuộc về ai" — chỉ SAU KHI đã `findById` xong (load được `Invoice.customerId` THẬT), Spring Security mới có đủ thông tin để so sánh với `authentication.principal.id`.
- **Đây chính là bài toán "IDOR" (Insecure Direct Object Reference)** đã được nhắc tới ở Module 17 — lỗ hổng XẢY RA nếu hệ thống CHỈ kiểm tra "đã đăng nhập chưa" (authentication) mà **QUÊN kiểm tra "dữ liệu này CÓ THUỘC VỀ người đang đăng nhập không"** (authorization theo dữ liệu/object-level) — nếu THIẾU bước kiểm tra Ownership này, BẤT KỲ user nào ĐÃ ĐĂNG NHẬP đều có thể xem hóa đơn của NGƯỜI KHÁC chỉ bằng cách ĐỔI `id` trên URL (`/invoices/1`, `/invoices/2`, `/invoices/3`...) — đây là 1 trong những lỗ hổng **PHỔ BIẾN VÀ NGHIÊM TRỌNG NHẤT** trong OWASP Top 10 (Broken Access Control).
- **Vì sao TRẢ VỀ `404 Not Found` (thay vì `403 Forbidden`) thường được khuyến nghị TỐT HƠN cho trường hợp này** (dù bài này dùng `@PostAuthorize` mặc định trả `403`): trả `403` **TIẾT LỘ** cho kẻ tấn công biết "hóa đơn với `id` này CÓ TỒN TẠI, chỉ là bạn không có quyền xem" — trong khi trả `404` (như thể hóa đơn KHÔNG TỒN TẠI) **KHÔNG TIẾT LỘ** thông tin gì về sự tồn tại của tài nguyên đó — với dữ liệu CỰC KỲ NHẠY CẢM, có thể cân nhắc custom `AccessDeniedHandler` (Module 17, Bài 6) để trả `404` thay vì `403` mặc định, dù đánh đổi là trải nghiệm debug khó hơn 1 chút cho DEV.

---

## Bài 3 — DTO Request ngăn Mass Assignment cho `PATCH /users/{id}/profile`

### Đề
Chỉ cho phép cập nhật `fullName` và `avatarUrl`. Không có cách nào để tự thay đổi `role`/`isActive`.

### Lời giải

```java
// DTO RIÊNG, CHỈ CHỨA ĐÚNG 2 FIELD ĐƯỢC PHÉP CẬP NHẬT - KHÔNG PHẢI toàn bộ Entity User
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @URL String avatarUrl
) {}

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/{id}/profile")
    @PreAuthorize("#id == authentication.principal.id")   // chỉ CHÍNH CHỦ mới được sửa profile của mình
    public ResponseEntity<Void> updateProfile(@PathVariable Long id, @RequestBody @Valid UpdateProfileRequest request) {
        userService.updateProfile(id, request);
        return ResponseEntity.noContent().build();
    }
}

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + userId));

        // CHỈ SET ĐÚNG 2 FIELD được phép - role/isActive HOÀN TOÀN KHÔNG XUẤT HIỆN Ở ĐÂY,
        // KHÔNG CÓ CÁCH NÀO client có thể "chèn" giá trị vào 2 field đó qua endpoint này
        user.setFullName(request.fullName());
        user.setAvatarUrl(request.avatarUrl());
        // KHÔNG gọi userRepository.save() cần thiết nếu đang trong @Transactional (dirty checking tự flush)
    }
}
```

### Giải thích

- **Đây chính là biện pháp phòng chống "Mass Assignment Vulnerability"** — lỗ hổng xảy ra khi code **BIND TRỰC TIẾP TOÀN BỘ REQUEST BODY VÀO ENTITY** (VD `User user = objectMapper.readValue(body, User.class); userRepository.save(user);`) — nếu client (kẻ tấn công) gửi thêm field `"role": "ADMIN"` hoặc `"isActive": true` vào JSON body (dù API "chính thức" không tài liệu hóa field này), và code KHÔNG kiểm soát, framework serialization CÓ THỂ TỰ ĐỘNG SET các field ĐÓ vào Entity — kẻ tấn công **TỰ NÂNG QUYỀN THÀNH ADMIN** chỉ bằng cách gửi thêm 1 field JSON không được phép.
- **DTO RIÊNG BIỆT (`UpdateProfileRequest`) là biện pháp phòng thủ TRIỆT ĐỂ NHẤT:** vì DTO này **VỀ MẶT CẤU TRÚC (structurally) KHÔNG CÓ FIELD `role`/`isActive`** — dù client CÓ GỬI THÊM các field đó trong JSON, Jackson (thư viện serialize JSON mặc định của Spring Boot) sẽ **TỰ ĐỘNG BỎ QUA** các field KHÔNG KHỚP với property nào trong DTO (hành vi mặc định) — hoàn toàn KHÔNG THỂ "lọt" vào Entity `User` bằng bất kỳ cách nào — đây là giải pháp AN TOÀN HƠN NHIỀU so với cách tiếp cận "blacklist" (cố gắng LIỆT KÊ các field CẤM, dễ bỏ sót khi thêm field mới về sau).
- **`@PreAuthorize("#id == authentication.principal.id")`** bổ sung THÊM 1 LỚP BẢO VỆ KHÁC (Ownership check, giống Bài 2) — ngăn user A gọi `PATCH /users/{id-của-B}/profile` để sửa profile của B — kết hợp CẢ 2 biện pháp (DTO riêng + Ownership check) tạo thành "PHÒNG THỦ THEO CHIỀU SÂU" (Defense in Depth) — ngay cả khi 1 lớp bảo vệ có sơ hở, lớp còn lại vẫn ngăn chặn được tấn công.

---

## Bài 4 — `ResponseCookie` lưu JWT với cờ bảo mật

### Đề
`httpOnly`, `secure`, `sameSite=Strict`.

### Lời giải

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequest request) {
        // ...xác thực user (như Module 17, Bài 2)...
        String accessToken = jwtService.generateToken(/* user */ null);

        ResponseCookie jwtCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)     // JavaScript phía client KHÔNG THỂ đọc cookie này (document.cookie) - chống XSS đánh cắp token
                .secure(true)       // cookie CHỈ được gửi qua kết nối HTTPS, KHÔNG BAO GIỜ gửi qua HTTP thuần
                .sameSite("Strict") // cookie CHỈ được gửi khi request xuất phát TỪ CHÍNH domain đó - chống CSRF
                .path("/")
                .maxAge(Duration.ofMinutes(15))   // khớp thời gian sống Access Token (Module 17, Bài 3)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .build();
    }
}
```

### Giải thích

- **`httpOnly(true)`** — chống **XSS (Cross-Site Scripting) đánh cắp token**: nếu kẻ tấn công tìm được cách CHÈN JavaScript độc hại vào trang web (VD qua 1 ô input không được sanitize đúng cách), đoạn script đó **KHÔNG THỂ ĐỌC** cookie có cờ `httpOnly` bằng `document.cookie` — token AN TOÀN dù trang có lỗ hổng XSS — đây là lý do lưu JWT trong **httpOnly cookie** được khuyến nghị AN TOÀN HƠN so với lưu trong `localStorage` (nơi JavaScript BẤT KỲ trên trang, kể cả script độc hại do XSS, ĐỀU ĐỌC ĐƯỢC).
- **`secure(true)`** — đảm bảo cookie CHỈ được trình duyệt gửi kèm request khi kết nối là **HTTPS** — nếu thiếu cờ này, cookie có thể bị gửi qua HTTP THUẦN (không mã hóa), cho phép kẻ tấn công trên CÙNG MẠNG (VD WiFi công cộng) **NGHE LÉN (sniff)** và đánh cắp token trực tiếp từ traffic mạng.
- **`sameSite("Strict")`** — chống **CSRF (Cross-Site Request Forgery)**: trình duyệt CHỈ đính kèm cookie này khi request được gửi TỪ TRANG WEB CÙNG DOMAIN (KHÔNG gửi kèm nếu request xuất phát từ 1 trang web KHÁC, dù là click link hay submit form từ trang độc hại) — đây chính là biện pháp XỬ LÝ TẬN GỐC vấn đề CSRF đã bàn ở Module 17, Bài 1 (lý do có thể `csrf().disable()` khi dùng JWT qua header `Authorization`) — nếu chuyển sang lưu JWT bằng COOKIE (thay vì header), `sameSite=Strict` trở thành LỚP PHÒNG THỦ CHỐNG CSRF QUAN TRỌNG, thay thế cho CSRF token truyền thống.
- **Đánh đổi của `SameSite=Strict` (so với `Lax`):** `Strict` CHẶN HOÀN TOÀN cookie khi user click 1 LINK TỪ TRANG NGOÀI dẫn tới ứng dụng (VD từ email, từ Google search result) — lần TRUY CẬP ĐẦU TIÊN đó sẽ KHÔNG có cookie đính kèm (coi như CHƯA đăng nhập dù thực ra ĐÃ đăng nhập trước đó) — nếu ứng dụng CẦN hỗ trợ tốt luồng "click link từ ngoài vào", `SameSite=Lax` là lựa chọn CÂN BẰNG hơn (vẫn chặn CSRF cho các request THAY ĐỔI dữ liệu như POST/PUT/DELETE, nhưng cho phép cookie đính kèm với GET điều hướng từ ngoài) — `Strict` phù hợp nhất cho ứng dụng KHÔNG CẦN luồng truy cập từ link ngoài (VD ứng dụng nội bộ, banking).

---

## Bài 5 — HASH vs ENCRYPT: dùng sai kỹ thuật cho số thẻ tín dụng/password

### Đề
Sự khác biệt HASH và ENCRYPT — vì sao dùng sai (hash số thẻ, hoặc encrypt password) đều là lỗi nghiêm trọng.

### Lời giải — minh họa code

```java
// ===== HASH - MỘT CHIỀU, KHÔNG THỂ ĐẢO NGƯỢC - PHÙ HỢP CHO PASSWORD =====
public class PasswordHashDemo {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "MyS3cretPass!";
        String hashed = encoder.encode(rawPassword);

        System.out.println("Password gốc: " + rawPassword);
        System.out.println("Password đã hash: " + hashed);
        // Ví dụ output: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

        // KHÔNG CÓ BẤT KỲ HÀM "decode(hashed)" NÀO TỒN TẠI - về mặt TOÁN HỌC KHÔNG THỂ
        // đảo ngược lại "MyS3cretPass!" từ chuỗi hash - CHỈ CÓ THỂ "matches()" (so khớp)
        boolean matches = encoder.matches("MyS3cretPass!", hashed);
        System.out.println("Khớp: " + matches);   // true - nhưng KHÔNG BAO GIỜ lấy lại được password GỐC
    }
}
```

```java
// ===== ENCRYPT - HAI CHIỀU, CÓ THỂ ĐẢO NGƯỢC (VỚI ĐÚNG KEY) - PHÙ HỢP CHO SỐ THẺ TÍN DỤNG =====
public class CreditCardEncryptDemo {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    public static String encrypt(String cardNumber, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // CÓ hàm decrypt() - vì hệ thống THANH TOÁN CẦN LẤY LẠI SỐ THẺ GỐC để gửi tới cổng thanh toán xử lý
    public static String decrypt(String encryptedBase64, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
```

### Giải thích — tại sao dùng SAI kỹ thuật là lỗi nghiêm trọng

**Trường hợp 1 — HASH số thẻ tín dụng (SAI):**
- Hệ thống thanh toán **CẦN LẤY LẠI ĐƯỢC SỐ THẺ GỐC** để gửi tới cổng thanh toán (VISA/Mastercard gateway) xử lý giao dịch THẬT — nếu hash số thẻ (giống password), **KHÔNG CÓ CÁCH NÀO lấy lại được số thẻ gốc** — hệ thống về cơ bản **KHÔNG THỂ THỰC HIỆN GIAO DỊCH** được nữa (trừ khi lưu số thẻ RIÊNG Ở NƠI KHÁC, dẫn tới lưu trùng lặp KHÔNG BẢO VỆ, VÔ NGHĨA).

**Trường hợp 2 — ENCRYPT password (SAI):**
- Nếu **MÃ HÓA** (encrypt) password thay vì HASH, password được lưu ở dạng **CÓ THỂ GIẢI MÃ ĐƯỢC** (nếu ai đó có ĐÚNG KEY giải mã) — nếu **KEY GIẢI MÃ BỊ LỘ** (VD do lỗi cấu hình, tấn công vào hệ thống quản lý key) — kẻ tấn công có thể **GIẢI MÃ TOÀN BỘ PASSWORD CỦA MỌI USER TRONG DATABASE CÙNG MỘT LÚC** — thiệt hại LAN RỘNG VÀ NGHIÊM TRỌNG HƠN NHIỀU so với hash: mật khẩu hash CHUẨN (BCrypt) dù DB bị lộ, kẻ tấn công **VẪN KHÔNG THỂ** lấy lại password gốc (chỉ có thể thử brute-force từng password RIÊNG LẺ, cực kỳ TỐN THỜI GIAN nhờ đặc tính "chậm có chủ đích" của BCrypt).

### Bảng so sánh tổng kết

| Tiêu chí | HASH (BCrypt/Argon2) | ENCRYPT (AES) |
|---|---|---|
| Chiều | MỘT CHIỀU (không thể đảo ngược) | HAI CHIỀU (đảo ngược được VỚI ĐÚNG key) |
| Mục đích | XÁC MINH (so khớp), KHÔNG CẦN lấy lại giá trị gốc | LẤY LẠI ĐƯỢC giá trị gốc khi CẦN THIẾT cho nghiệp vụ |
| Dùng cho | Password, PIN (chỉ cần SO KHỚP, không cần "biết lại" giá trị gốc) | Số thẻ tín dụng, dữ liệu cá nhân nhạy cảm (CẦN đọc lại được để dùng cho nghiệp vụ) |
| Rủi ro nếu bị lộ DB | Kẻ tấn công CHỈ CÓ THỂ brute-force TỪNG giá trị (chậm, tốn kém) | Nếu key CŨNG bị lộ, TOÀN BỘ dữ liệu bị giải mã NGAY LẬP TỨC |
| Yêu cầu quản lý key | KHÔNG cần quản lý key giải mã (không tồn tại) | BẮT BUỘC quản lý key CỰC KỲ CẨN THẬN (Key Management System riêng, tách biệt khỏi DB) |

- **Nguyên tắc chọn lựa: câu hỏi cốt lõi là "HỆ THỐNG CÓ BAO GIỜ CẦN NHÌN LẠI GIÁ TRỊ GỐC KHÔNG?"** — Nếu KHÔNG (chỉ cần SO KHỚP, như password) → **HASH**; Nếu CÓ (cần dùng lại giá trị THẬT cho nghiệp vụ, như số thẻ gửi tới cổng thanh toán) → **ENCRYPT** (và quản lý key CỰC KỲ nghiêm ngặt, tách biệt — thường dùng dịch vụ chuyên dụng như AWS KMS/HashiCorp Vault, không tự lưu key CHUNG với dữ liệu đã mã hóa).

---

## Bài 6 — `SsrfProtectionValidator` cho webhook URL

### Đề
(a) chỉ cho phép `https`, (b) chặn IP loopback/site-local/link-local, (c) giải thích vì sao validate 1 lần chưa đủ (DNS Rebinding).

### Lời giải

```java
@Component
public class SsrfProtectionValidator {

    public void validateWebhookUrl(String urlString) {
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL không hợp lệ: " + urlString);
        }

        // (a) CHỈ cho phép scheme https - chặn http (không mã hóa), file://, ftp://, gopher://...
        // (gopher:// từng bị lợi dụng trong nhiều vụ SSRF nổi tiếng để "giả lập" giao thức khác)
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Chỉ chấp nhận URL với scheme https");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("URL không hợp lệ - thiếu host");
        }

        // (b) Resolve hostname THÀNH ĐỊA CHỈ IP THỰC TẾ - và kiểm tra IP đó
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Không thể phân giải hostname: " + host);
        }

        if (address.isLoopbackAddress()) {       // 127.0.0.1, ::1 - CHÍNH server đang chạy ứng dụng
            throw new IllegalArgumentException("Không được phép trỏ tới địa chỉ loopback");
        }
        if (address.isSiteLocalAddress()) {      // 10.x.x.x, 172.16-31.x.x, 192.168.x.x - MẠNG NỘI BỘ
            throw new IllegalArgumentException("Không được phép trỏ tới địa chỉ mạng nội bộ (site-local)");
        }
        if (address.isLinkLocalAddress()) {      // 169.254.x.x - THƯỜNG dùng cho CLOUD METADATA ENDPOINT
            throw new IllegalArgumentException("Không được phép trỏ tới địa chỉ link-local");
        }
        if (address.isAnyLocalAddress()) {       // 0.0.0.0
            throw new IllegalArgumentException("Không được phép trỏ tới địa chỉ 0.0.0.0");
        }
    }
}
```

```java
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final SsrfProtectionValidator ssrfProtectionValidator;
    private final WebhookRepository webhookRepository;

    public WebhookController(SsrfProtectionValidator ssrfProtectionValidator, WebhookRepository webhookRepository) {
        this.ssrfProtectionValidator = ssrfProtectionValidator;
        this.webhookRepository = webhookRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerWebhook(@RequestBody @Valid RegisterWebhookRequest request) {
        ssrfProtectionValidator.validateWebhookUrl(request.callbackUrl());
        webhookRepository.save(new Webhook(request.callbackUrl()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
```

### Giải thích (c) — vì sao validate 1 lần lúc đăng ký CHƯA ĐỦ AN TOÀN TUYỆT ĐỐI (DNS Rebinding)

```
Kịch bản tấn công DNS Rebinding:

BƯỚC 1 (LÚC ĐĂNG KÝ): Kẻ tấn công đăng ký webhook URL: https://evil-attacker.com/callback
  - Tại thời điểm NÀY, DNS của "evil-attacker.com" trỏ tới 1 địa chỉ IP CÔNG KHAI HỢP LỆ
    (VD 203.0.113.5) - validate PASS BÌNH THƯỜNG (không phải loopback/site-local/link-local)

BƯỚC 2 (SAU KHI ĐĂNG KÝ THÀNH CÔNG): Kẻ tấn công ĐỔI CẤU HÌNH DNS của "evil-attacker.com"
  (họ SỞ HỮU domain này, HOÀN TOÀN CÓ QUYỀN đổi DNS record BẤT KỲ LÚC NÀO) để TRỎ SANG
  1 địa chỉ IP NỘI BỘ, VD: 169.254.169.254 (địa chỉ metadata endpoint KINH ĐIỂN của AWS/GCP/Azure -
  chứa THÔNG TIN NHẠY CẢM như IAM credential, access token của server)

BƯỚC 3: Khi hệ thống THẬT SỰ GỌI webhook callback (lúc CÓ SỰ KIỆN xảy ra, CÓ THỂ SAU VALIDATE
  RẤT LÂU - vài giờ, vài ngày), hệ thống lại RESOLVE DNS "evil-attacker.com" MỘT LẦN NỮA
  (DNS được resolve LẠI mỗi lần gọi HTTP, KHÔNG "nhớ" kết quả validate TRƯỚC ĐÓ) - LẦN NÀY
  DNS trả về ĐỊA CHỈ NỘI BỘ MÀ KẺ TẤN CÔNG ĐÃ ĐỔI - request "webhook" THỰC SỰ được gửi tới
  169.254.169.254 (metadata endpoint) THAY VÌ server của kẻ tấn công

=> KẺ TẤN CÔNG LỢI DỤNG chính SERVER CỦA HỆ THỐNG để "GỌI HỘ" request tới metadata endpoint NỘI BỘ,
   VÀ NẾU response được "echo" LẠI (VD hệ thống log lại response của webhook, hoặc trả về cho client),
   kẻ tấn công CÓ THỂ ĐÁNH CẮP được credential/token nhạy cảm của chính hạ tầng cloud đang chạy hệ thống
```

**Biện pháp khắc phục bổ sung (KHÔNG chỉ validate 1 lần):**
- **Validate LẠI NGAY TRƯỚC MỖI LẦN THỰC SỰ GỌI request** (không chỉ lúc đăng ký) — resolve DNS và kiểm tra IP MỖI LẦN TRƯỚC KHI gửi HTTP request THẬT tới webhook URL.
- **"Pin" (khóa cứng) địa chỉ IP đã validate**, dùng CHÍNH XÁC IP ĐÓ khi gửi request (không để hệ thống HTTP client tự resolve DNS lại theo cách thông thường) — đảm bảo IP dùng để GỬI THẬT chính là IP ĐÃ ĐƯỢC KIỂM TRA, không có "khoảng hở" cho phép DNS bị đổi giữa lúc validate và lúc gửi.
- Dùng **mạng cách ly (network segmentation)** — server thực hiện gọi webhook đặt trong 1 network KHÔNG CÓ QUYỀN TRUY CẬP tới các endpoint nội bộ nhạy cảm (metadata endpoint, service nội bộ khác) NGAY CẢ KHI có lỗ hổng SSRF ở tầng ứng dụng — đây là lớp phòng thủ CUỐI CÙNG (defense in depth) khi các biện pháp ở tầng ứng dụng có thể có sơ hở.

---

*Đây là lời giải cho toàn bộ Phần B của Module 32. Tiếp theo: Module 25 — Soft Skills & Career.*
