# Module 16 — Spring Security

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Bảo mật không phải là tính năng "thêm vào sau" — 1 lỗ hổng bảo mật (JWT không verify đúng, password lưu plaintext, thiếu CORS/CSRF protection) có thể làm lộ toàn bộ dữ liệu người dùng và phá hủy uy tín hệ thống. Đây cũng là chủ đề bị hỏi sâu nhất trong phỏng vấn Backend — không chỉ "dùng annotation nào" mà là **hiểu đúng cơ chế** Filter Chain, vì sao JWT phù hợp với REST hơn Session, và các lỗ hổng bảo mật kinh điển (OWASP Top 10 liên quan) để tránh mắc phải.

---

## Mục lục

1. [Authentication vs Authorization](#1-authentication-vs-authorization)
2. [Spring Security Filter Chain — cơ chế hoạt động](#2-spring-security-filter-chain)
3. [Password Encoding](#3-password-encoding)
4. [Session-based Authentication (cách truyền thống)](#4-session-based-authentication)
5. [JWT (JSON Web Token) — chuẩn cho REST API](#5-jwt)
6. [Triển khai JWT Authentication đầy đủ trong Spring Boot](#6-triển-khai-jwt-authentication)
7. [OAuth2 & OpenID Connect](#7-oauth2--openid-connect)
8. [Method-level Security](#8-method-level-security)
9. [CORS](#9-cors)
10. [CSRF](#10-csrf)
11. [⚠️ Các bẫy hay gặp & lỗ hổng bảo mật kinh điển](#11-các-bẫy-hay-gặp)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. Authentication vs Authorization

Đây là 2 khái niệm **bị nhầm lẫn nhiều nhất** kể cả bởi dev có kinh nghiệm — cũng chính là điều đã đề cập ở Module 14 khi phân biệt `401` vs `403`.

```
Authentication (Xác thực)  -> "Bạn là ai?"       -> Trả lời SAI/thiếu -> 401 Unauthorized
Authorization (Phân quyền) -> "Bạn được làm gì?"  -> Không đủ quyền    -> 403 Forbidden
```

| | Authentication | Authorization |
|---|---|---|
| Câu hỏi | Xác minh danh tính (đăng nhập) | Kiểm tra quyền truy cập resource |
| Thực hiện trước | ✅ Luôn xảy ra trước | Xảy ra SAU khi đã xác thực |
| Cơ chế phổ biến | Username/Password, JWT, OAuth2 | Role-based (RBAC), Permission-based |
| Ví dụ | Đăng nhập bằng email/password | User role "USER" không được xóa bài viết của người khác |

---

## 2. Spring Security Filter Chain

Spring Security hoạt động dựa trên **chuỗi các Filter** (Servlet Filter — khái niệm gốc từ Java EE, không phải riêng Spring) được xử lý **tuần tự** trước khi request chạm tới Controller.

```
HTTP Request
     │
     ▼
┌──────────────────────────────┐
│  SecurityContextPersistenceFilter │  Khôi phục SecurityContext từ Session (nếu có)
├──────────────────────────────┤
│  UsernamePasswordAuthenticationFilter │  Xử lý login form (username/password)
├──────────────────────────────┤
│  JwtAuthenticationFilter (custom)   │  Filter TỰ VIẾT để verify JWT token (mục 6)
├──────────────────────────────┤
│  ExceptionTranslationFilter    │  Bắt AccessDeniedException/AuthenticationException
├──────────────────────────────┤
│  FilterSecurityInterceptor     │  Kiểm tra Authorization (role/permission) cuối cùng
└──────────────────────────────┘
     │
     ▼
DispatcherServlet -> Controller
```

**Nguyên lý cốt lõi:** Mỗi Filter có thể:
- Cho phép request đi tiếp (`filterChain.doFilter(request, response)`)
- Chặn request lại và trả response ngay (VD: 401 nếu token không hợp lệ)

### SecurityContext — nơi lưu thông tin "ai đang đăng nhập"

```java
// Sau khi xác thực thành công, thông tin user được lưu vào SecurityContext
// (lưu trong ThreadLocal - mỗi thread/request có SecurityContext riêng)
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
```

> **Liên hệ:** `AuditorAware` ở Module 15 lấy username từ chính `SecurityContextHolder` này — đây là lý do vì sao Auditing tự động biết "ai đang đăng nhập" mà không cần truyền tham số qua từng method.

### Cấu hình Filter Chain (Spring Security 6+ — dùng SecurityFilterChain Bean, KHÔNG dùng WebSecurityConfigurerAdapter đã deprecated)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF cho REST API (xem lý do ở mục 10)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Không tạo Session - dùng JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()      // Public - không cần login
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") // Chỉ ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll() // GET công khai
                .anyRequest().authenticated()                          // Còn lại bắt buộc đăng nhập
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
        return http.build();
    }
}
```

⚠️ **Thứ tự khai báo `authorizeHttpRequests` RẤT QUAN TRỌNG** — Spring Security đánh giá theo thứ tự từ trên xuống, khớp rule đầu tiên sẽ dừng lại:

```java
// ❌ SAI THỨ TỰ - rule chung đặt trước rule cụ thể
.authorizeHttpRequests(auth -> auth
    .anyRequest().authenticated()                    // Rule này khớp TẤT CẢ trước
    .requestMatchers("/api/v1/auth/**").permitAll()   // -> Không bao giờ được áp dụng!
)

// ✅ ĐÚNG - rule cụ thể/đặc biệt đặt TRƯỚC rule chung
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()   // Cụ thể trước
    .anyRequest().authenticated()                       // Chung nhất đặt CUỐI CÙNG
)
```

---

## 3. Password Encoding

**TUYỆT ĐỐI KHÔNG BAO GIỜ lưu password dạng plaintext** trong database — nếu database bị lộ (data breach), toàn bộ mật khẩu người dùng bị lộ theo.

```java
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Thuật toán hash chuyên dụng cho password
    }
}
```

### Vì sao dùng BCrypt thay vì MD5/SHA-256 thông thường?

| | MD5 / SHA-256 | BCrypt |
|---|---|---|
| Mục đích thiết kế | Hash dữ liệu **nhanh** (checksum, tính toàn vẹn file) | Hash **CHẬM CÓ CHỦ ĐÍCH** cho password |
| Tốc độ tính | Cực nhanh (hàng tỷ lần/giây trên GPU) | Chậm có kiểm soát (dùng "work factor"/"cost") |
| Chống Brute-force | ❌ Yếu — tốc độ nhanh giúp kẻ tấn công thử hàng tỷ password/giây | ✅ Mạnh — cố ý làm chậm để tấn công brute-force không khả thi |
| Salt tự động | Không — phải tự thêm salt thủ công | ✅ Tự động sinh salt ngẫu nhiên, nhúng luôn trong hash output |
| Rainbow Table Attack | Dễ bị tấn công nếu không có salt riêng | Chống được nhờ salt ngẫu nhiên mỗi lần hash |

```java
@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void register(String email, String rawPassword) {
        String hashedPassword = passwordEncoder.encode(rawPassword);
        // hashedPassword VD: $2a$10$N9qo8uLOickgx2ZMRZoMye... (60 ký tự, chứa cả salt + cost factor)
        User user = new User(email, hashedPassword);
        userRepository.save(user);
    }

    public boolean login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseThrow();
        // KHÔNG BAO GIỜ so sánh trực tiếp rawPassword == user.getPassword()
        // Phải dùng passwordEncoder.matches() -> tự động hash rawPassword rồi so sánh với hash đã lưu
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}
```

⚠️ **Đặc điểm quan trọng của BCrypt:** Cùng 1 password gốc, hash 2 lần sẽ ra **2 chuỗi hash khác nhau** (do salt ngẫu nhiên) — nhưng `matches()` vẫn trả `true` vì salt được nhúng ngay trong chuỗi hash output.

```java
String hash1 = passwordEncoder.encode("mypassword"); // $2a$10$abc123...
String hash2 = passwordEncoder.encode("mypassword"); // $2a$10$xyz789... (KHÁC hash1!)
// Nhưng cả 2 đều matches("mypassword", hash1/hash2) == true
```

---

## 4. Session-based Authentication

Cách truyền thống (thường dùng cho server-side rendered web app, ít dùng cho REST API hiện đại — nhưng cần hiểu để so sánh với JWT).

```
1. Client POST /login (username, password)
2. Server xác thực -> tạo Session, lưu trong bộ nhớ server (hoặc Redis)
3. Server trả về Session ID qua Cookie (Set-Cookie: JSESSIONID=abc123)
4. Client tự động gửi kèm Cookie ở mọi request tiếp theo
5. Server tra cứu Session ID -> biết user là ai
```

**Vấn đề khi scale ngang (nhiều server instance):**

```
❌ Vấn đề: Session lưu trong bộ nhớ Server A
   -> Load Balancer route request tiếp theo tới Server B
   -> Server B KHÔNG có Session này -> user bị "văng" ra, phải đăng nhập lại

Giải pháp truyền thống: Sticky Session (Load Balancer luôn route cùng user
   về cùng 1 server) HOẶC Session lưu tập trung ở Redis (Shared Session Store)
   -> đều thêm độ phức tạp hạ tầng
```

> **Đây chính là lý do REST API hiện đại ưu tiên JWT** — vì bản chất "Stateless" (đã học ở Module 14) giúp scale ngang dễ dàng hơn nhiều, không cần đồng bộ session giữa các server.

---

## 5. JWT (JSON Web Token)

### Cấu trúc JWT — 3 phần, phân cách bởi dấu chấm

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwaG9AZXhhbXBsZS5jb20i....SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

┌─────────────┐ . ┌──────────────┐ . ┌────────────┐
│   Header     │   │    Payload    │   │  Signature  │
└─────────────┘   └──────────────┘   └────────────┘
```

**1. Header** — thuật toán ký (base64-encoded JSON):
```json
{ "alg": "HS256", "typ": "JWT" }
```

**2. Payload** — dữ liệu (claims), base64-encoded JSON (⚠️ **KHÔNG được mã hóa, chỉ encode** — ai cũng đọc được nếu decode base64):
```json
{
  "sub": "pho@example.com",
  "role": "USER",
  "iat": 1725609600,
  "exp": 1725613200
}
```

**3. Signature** — chữ ký số, đảm bảo token không bị giả mạo/sửa đổi:
```
HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secretKey)
```

⚠️ **Điểm cực kỳ quan trọng nhiều người hiểu sai:** JWT payload **KHÔNG hề được mã hóa (encrypted)** — chỉ **encode (base64)**. Bất kỳ ai có token đều có thể decode và đọc được nội dung payload (dùng jwt.io chẳng hạn). **Signature chỉ đảm bảo tính toàn vẹn (integrity) — chống giả mạo/sửa đổi, KHÔNG đảm bảo tính bí mật (confidentiality).**

```
=> KHÔNG BAO GIỜ nhét thông tin nhạy cảm (password, số thẻ tín dụng, dữ liệu cá nhân nhạy cảm)
   vào JWT payload!
```

### Luồng hoạt động

```
1. Client POST /login (username, password)
2. Server xác thực -> tạo JWT (ký bằng secretKey), trả về cho Client
3. Client lưu JWT (thường ở localStorage hoặc httpOnly Cookie - có đánh đổi bảo mật khác nhau)
4. Client gửi kèm JWT ở header MỌI request tiếp theo:
      Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
5. Server KHÔNG cần lưu gì cả (Stateless!) - chỉ cần verify Signature bằng secretKey
   -> Nếu hợp lệ, đọc claims (username, role) từ payload -> biết user là ai, có quyền gì
```

### Access Token vs Refresh Token

| | Access Token | Refresh Token |
|---|---|---|
| Thời gian sống | Ngắn (15 phút - 1 giờ) | Dài (7-30 ngày) |
| Mục đích | Xác thực mỗi API request | Lấy Access Token mới khi hết hạn |
| Lưu ở đâu | Memory/localStorage (client) | httpOnly Cookie (an toàn hơn) hoặc DB (có thể revoke) |
| Rủi ro nếu bị đánh cắp | Giới hạn (hết hạn nhanh) | Nguy hiểm hơn (sống lâu) — cần cơ chế revoke |

```
Access Token hết hạn -> Client gọi POST /auth/refresh kèm Refresh Token
                      -> Server verify Refresh Token, cấp Access Token MỚI
                      -> Client không cần đăng nhập lại bằng password
```

---

## 6. Triển khai JWT Authentication đầy đủ

### JwtService — tạo và verify token

```java
@Service
public class JwtService {

    @Value("${app.jwt.secret-key}")
    private String secretKey; // Đọc từ application.yml, nên set qua biến môi trường ở production

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        // parseSignedClaims TỰ ĐỘNG verify Signature -
        // nếu token bị sửa đổi (dù chỉ 1 ký tự), sẽ throw SignatureException ngay tại đây
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
```

### JwtAuthenticationFilter — custom Filter chạy trước Controller

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Không có token -> cho qua, để rule authorizeHttpRequests xử lý
            return;
        }

        String token = authHeader.substring(7); // Bỏ "Bearer " (7 ký tự)
        try {
            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    // Set thông tin user vào SecurityContext -> các bước xử lý sau (Controller, @PreAuthorize)
                    // sẽ biết "ai đang gọi request này"
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            // Token không hợp lệ (hết hạn, sai signature...) -> KHÔNG set Authentication
            // -> request tiếp tục đi qua, nhưng sẽ bị chặn ở FilterSecurityInterceptor nếu endpoint yêu cầu auth
            logger.warn("JWT không hợp lệ: " + e.getMessage());
        }

        filterChain.doFilter(request, response); // LUÔN gọi để request tiếp tục đi qua chain
    }
}
```

### AuthController — endpoint login/register

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        // AuthenticationManager tự động: tìm UserDetails theo username,
        // so sánh password bằng PasswordEncoder.matches() -
        // throw BadCredentialsException nếu sai (được GlobalExceptionHandler bắt -> 401)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email()).orElseThrow();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
```

---

## 7. OAuth2 & OpenID Connect

### OAuth2 là gì?

**OAuth2** là giao thức **ủy quyền (Authorization)** — cho phép ứng dụng thứ 3 truy cập tài nguyên của user trên 1 hệ thống khác **mà không cần biết password** của user đó. Ví dụ điển hình: "Đăng nhập bằng Google/Facebook".

```
Người dùng bấm "Đăng nhập bằng Google" trên app của bạn
     │
     ▼
Redirect tới Google -> User đăng nhập trên trang CỦA GOOGLE (app của bạn KHÔNG BAO GIỜ thấy password Google)
     │
     ▼
Google redirect về app của bạn kèm "Authorization Code"
     │
     ▼
App của bạn (server-side) dùng Code này đổi lấy "Access Token" từ Google
     │
     ▼
Dùng Access Token gọi Google API để lấy thông tin user (email, tên, avatar...)
```

### OpenID Connect (OIDC) — mở rộng của OAuth2

**OAuth2 vốn chỉ giải quyết Authorization** (ủy quyền truy cập tài nguyên) — **không phải Authentication**. **OpenID Connect (OIDC)** là lớp mở rộng thêm trên OAuth2 để chuẩn hóa việc **xác thực danh tính**, bổ sung thêm **ID Token** (1 dạng JWT chứa thông tin định danh user: email, tên...).

| | OAuth2 | OpenID Connect (OIDC) |
|---|---|---|
| Mục đích chính | Authorization (ủy quyền truy cập tài nguyên) | Authentication (xác thực danh tính) |
| Token trả về | Access Token | Access Token **+ ID Token** (JWT chuẩn hóa) |
| Ví dụ | App được cấp quyền đọc Google Drive của user | "Đăng nhập bằng Google" (xác định user là ai) |

### Spring Boot hỗ trợ sẵn (Spring Security OAuth2 Client)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: profile, email
```

> **Mức độ ưu tiên học:** Hiểu khái niệm và luồng hoạt động là đủ ở giai đoạn này (thường bị hỏi phỏng vấn dưới dạng lý thuyết). Triển khai OAuth2 Server/Client đầy đủ là kiến thức nâng cao hơn, thường học sâu khi làm dự án thực tế cần tích hợp Social Login.

---

## 8. Method-level Security

Bổ sung cho Authorization ở tầng URL (`authorizeHttpRequests`) — cho phép kiểm soát quyền **ngay tại method**, linh hoạt hơn nhiều.

```java
@Configuration
@EnableMethodSecurity // Bật tính năng này
public class SecurityConfig { }
```

```java
@Service
public class OrderService {

    @PreAuthorize("hasRole('ADMIN')") // Kiểm tra TRƯỚC khi method chạy
    public void deleteAllOrders() { ... }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    // Cho phép ADMIN, HOẶC chính user đó truy cập dữ liệu của mình
    public Order getOrder(Long userId, Long orderId) { ... }

    @PostAuthorize("returnObject.user.email == authentication.name")
    // Kiểm tra SAU khi method chạy, dựa trên kết quả trả về
    // -> Hữu ích khi điều kiện phân quyền phụ thuộc vào chính dữ liệu vừa lấy được
    public Order getMyOrder(Long orderId) { ... }

    @PreFilter("filterObject.status == 'PENDING'")
    // Lọc TRƯỚC khi vào method - áp dụng cho tham số dạng Collection
    public void processOrders(List<Order> orders) { ... }

    @PostFilter("filterObject.user.id == authentication.principal.id")
    // Lọc kết quả trả về SAU khi method chạy xong
    public List<Order> getAllOrders() { ... }
}
```

> **Liên hệ:** `@PreAuthorize`/`@PostAuthorize` cũng hoạt động dựa trên **Spring AOP** (đã học ở Module 12) — cùng cơ chế Proxy với `@Transactional`, nên cũng gặp **bẫy self-invocation** tương tự nếu gọi qua `this`.

**Khi nào dùng Method-level Security thay vì URL-based:**
- Logic phân quyền phức tạp, phụ thuộc vào dữ liệu cụ thể (VD: "chỉ chủ sở hữu mới được sửa")
- Cần áp dụng cho method không phải Controller (VD: Service được gọi từ nhiều nơi, kể cả background job)

---

## 9. CORS

**CORS (Cross-Origin Resource Sharing)** là cơ chế **trình duyệt** (không phải server) thực thi để chặn JavaScript ở 1 origin (domain/port/protocol) gọi API tới **origin khác** — trừ khi server đó tường minh cho phép.

```
Frontend: http://localhost:3000
Backend:  http://localhost:8080

-> 2 origin KHÁC NHAU (khác port) -> trình duyệt sẽ CHẶN request
   trừ khi Backend trả về header cho phép CORS
```

⚠️ **Hiểu nhầm phổ biến:** CORS **KHÔNG PHẢI** là cơ chế bảo mật để chặn kẻ tấn công — nó là cơ chế **bảo vệ người dùng trình duyệt** khỏi các trang web độc hại gọi ngầm API tới domain khác bằng chính Cookie/Session của họ. Postman, curl, hay server-to-server call **hoàn toàn không bị CORS chặn** (vì CORS chỉ được trình duyệt thực thi).

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://myapp.com"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true); // Cho phép gửi kèm Cookie

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

```java
// Áp dụng trong SecurityFilterChain
http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
```

⚠️ **Bẫy bảo mật:** Không bao giờ dùng `setAllowedOrigins(List.of("*"))` (cho phép MỌI origin) kết hợp với `setAllowCredentials(true)` — thực tế trình duyệt hiện đại **sẽ từ chối** tổ hợp này (đúng theo spec CORS) vì nó tương đương "cho phép mọi trang web trên Internet gọi API kèm cookie của user" — cực kỳ nguy hiểm.

---

## 10. CSRF

**CSRF (Cross-Site Request Forgery)** là kiểu tấn công lừa user (đã đăng nhập, có session hợp lệ) **vô tình** thực hiện 1 request độc hại mà không hề hay biết.

### Kịch bản tấn công

```html
<!-- Trang web độc hại, user đã đăng nhập ngân hàng ở tab khác (còn Session/Cookie hợp lệ) -->
<img src="https://mybank.com/transfer?to=hacker&amount=1000000" />
<!-- Trình duyệt tự động gửi request này KÈM Cookie Session của mybank.com
     (vì Cookie được trình duyệt tự động đính kèm theo domain, không quan tâm trang nào gọi) -->
```

### Vì sao JWT (đặt trong Header) miễn nhiễm với CSRF, còn Session-Cookie thì không?

```
Session-based (Cookie tự động gửi kèm mọi request tới domain đó):
-> Trình duyệt TỰ ĐỘNG đính kèm Cookie dù request được khởi tạo từ trang web khác
-> Dễ bị CSRF nếu không có biện pháp bảo vệ

JWT (đặt trong Authorization Header, do JavaScript CHỦ ĐỘNG set):
-> Trang web độc hại KHÔNG THỂ tự động biết/đọc/set Authorization Header
   của app khác (bị chặn bởi Same-Origin Policy + CORS)
-> Không có cách nào "tự động" gửi kèm JWT trong request giả mạo
```

**Đây là lý do trong config ở mục 2, ta có `csrf(csrf -> csrf.disable())`:** Khi dùng JWT (Stateless, không dùng Session/Cookie để auth), CSRF protection của Spring Security là **không cần thiết** — vì bản chất tấn công CSRF chỉ khai thác được cơ chế Cookie tự động gửi kèm.

⚠️ **Lưu ý quan trọng:** Nếu ứng dụng **vẫn dùng Session/Cookie** (không phải JWT thuần), **PHẢI giữ CSRF protection bật** — tắt CSRF trong trường hợp đó là lỗ hổng bảo mật nghiêm trọng.

```java
// Nếu dùng Session-based (không phải JWT) - PHẢI giữ CSRF bật (mặc định của Spring Security)
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
// Client phải gửi kèm CSRF Token (nhận từ Cookie) trong header của mỗi request thay đổi dữ liệu
```

---

## 11. ⚠️ Các bẫy hay gặp & lỗ hổng bảo mật kinh điển

1. **Lưu password dạng plaintext hoặc hash bằng MD5/SHA-256 thường** thay vì BCrypt/Argon2 → dễ bị brute-force nếu database bị lộ.

2. **Nhét thông tin nhạy cảm vào JWT payload** (số CMND, số thẻ...) — payload chỉ encode, không encrypt, ai cũng đọc được.

3. **Không set thời gian hết hạn cho JWT (hoặc set quá dài)** → token bị đánh cắp sẽ có hiệu lực vô thời hạn/rất lâu.

4. **Tắt CSRF khi vẫn dùng Session-based Authentication** → mở lỗ hổng CSRF nghiêm trọng.

5. **Cấu hình sai thứ tự `authorizeHttpRequests`** (rule chung đặt trước rule cụ thể) → rule cụ thể không bao giờ được áp dụng.

6. **`setAllowedOrigins("*")` kèm `setAllowCredentials(true)`** → vi phạm spec CORS, có thể gây lỗi runtime hoặc lỗ hổng nếu framework không chặn đúng.

7. **Không revoke được Refresh Token khi cần** (VD: user đổi password, logout tất cả thiết bị) — nếu Refresh Token chỉ lưu ở client mà server không có cơ chế theo dõi/blacklist, không thể vô hiệu hóa được.

8. **So sánh password bằng `==` hoặc `.equals()` trực tiếp** thay vì `passwordEncoder.matches()` — không thể hoạt động đúng vì hash có salt ngẫu nhiên.

9. **Quên `@EnableMethodSecurity`** khi dùng `@PreAuthorize`/`@PostAuthorize` → annotation không có tác dụng gì, không báo lỗi rõ ràng.

10. **Self-invocation với `@PreAuthorize`** (giống bẫy `@Transactional` ở Module 12) — gọi qua `this.method()` sẽ bỏ qua kiểm tra phân quyền hoàn toàn.

11. **Không validate/sanitize input trước khi query DB** → lỗ hổng SQL Injection (dù dùng JPA/Hibernate với JPQL/Query Method đã tự động parameterize, vẫn cần cẩn thận với Native Query nối chuỗi thủ công).

12. **Log thông tin nhạy cảm** (password, token, số thẻ) ra console/log file → rò rỉ qua hệ thống logging tập trung (ELK, Splunk...).

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Authentication vs Authorization | "Bạn là ai" (401) vs "Bạn được làm gì" (403) |
| Filter Chain | Chuỗi Filter xử lý tuần tự trước Controller, thứ tự rule quan trọng |
| BCrypt | Hash chậm có chủ đích + salt tự động — không dùng MD5/SHA cho password |
| JWT | 3 phần Header.Payload.Signature — payload CHỈ encode, không encrypt |
| Access vs Refresh Token | Access ngắn hạn dùng mỗi request; Refresh dài hạn để lấy Access mới |
| Session vs JWT | Session cần đồng bộ giữa server (scale khó); JWT Stateless (scale dễ) |
| OAuth2 vs OIDC | OAuth2 = Authorization; OIDC = OAuth2 + Authentication (ID Token) |
| Method-level Security | `@PreAuthorize`/`@PostAuthorize` — dùng AOP, cũng dính bẫy self-invocation |
| CORS | Cơ chế TRÌNH DUYỆT thực thi, không chặn được Postman/curl/server-to-server |
| CSRF | Chỉ nguy hiểm với Session-Cookie auth; JWT (Header) miễn nhiễm tự nhiên |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. JWT payload được mã hóa nên an toàn để lưu thông tin nhạy cảm như số thẻ tín dụng.
2. CORS là cơ chế bảo mật ngăn chặn mọi loại tấn công từ bên ngoài, kể cả gọi API bằng Postman.
3. Khi dùng JWT (Stateless authentication), tắt CSRF protection là hợp lý và an toàn.
4. `passwordEncoder.encode("123456")` gọi 2 lần sẽ luôn cho ra cùng 1 chuỗi hash.
5. `@PreAuthorize` hoạt động dựa trên cơ chế Spring AOP, nên cũng gặp vấn đề self-invocation giống `@Transactional`.
6. OAuth2 về bản chất được thiết kế cho mục đích Authentication (xác thực danh tính).
7. Thứ tự khai báo rule trong `authorizeHttpRequests` không quan trọng, Spring Security tự tìm rule phù hợp nhất.
8. Refresh Token thường có thời gian sống dài hơn Access Token.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Viết đầy đủ `SecurityFilterChain` cho ứng dụng REST API dùng JWT: public cho `/api/v1/auth/**`, chỉ ADMIN truy cập `/api/v1/admin/**`, GET công khai cho `/api/v1/products/**`, còn lại yêu cầu đăng nhập. Đảm bảo đúng thứ tự rule.

**Bài 2:** Viết `AuthController` đầy đủ 2 endpoint: `POST /register` (hash password bằng BCrypt trước khi lưu) và `POST /login` (verify password, trả JWT nếu đúng).

**Bài 3:** Viết `JwtService` với đầy đủ 3 method: `generateToken()`, `isTokenValid()`, và thêm mới `generateRefreshToken()` với thời gian sống dài hơn Access Token (giả sử access = 15 phút, refresh = 7 ngày).

**Bài 4:** Viết 1 `OrderService` dùng `@PreAuthorize` để đảm bảo: ADMIN được xem tất cả order; USER thường chỉ được xem order của chính họ (so sánh `userId` với `authentication.principal.id`).

**Bài 5:** Cho đoạn code CORS config sau, hãy chỉ ra lỗi bảo mật và sửa lại:
```java
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOrigins(List.of("*"));
configuration.setAllowCredentials(true);
```

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** JWT payload chỉ được **encode base64**, không hề mã hóa — bất kỳ ai cũng decode đọc được. Tuyệt đối không nhét thông tin nhạy cảm vào đó.
2. **Sai.** CORS chỉ là cơ chế được **trình duyệt** thực thi để bảo vệ user — hoàn toàn không chặn được Postman, curl, hay gọi API trực tiếp giữa các server.
3. **Đúng.** Vì CSRF khai thác cơ chế Cookie tự động gửi kèm của trình duyệt — JWT đặt trong Header do JavaScript chủ động set, không bị lợi dụng theo cách tương tự, nên tắt CSRF là hợp lý khi dùng JWT thuần Stateless.
4. **Sai.** BCrypt sinh salt ngẫu nhiên mỗi lần hash → 2 lần gọi cho 2 chuỗi hash khác nhau, dù cùng input, nhưng `matches()` vẫn xác định đúng.
5. **Đúng.** `@PreAuthorize` dựa trên Spring AOP Proxy — gọi qua `this.method()` trong cùng class sẽ bỏ qua kiểm tra, giống hệt cơ chế của `@Transactional`.
6. **Sai.** OAuth2 vốn được thiết kế cho **Authorization** (ủy quyền truy cập tài nguyên) — OpenID Connect mới là lớp mở rộng thêm cho Authentication.
7. **Sai.** Thứ tự RẤT quan trọng — Spring Security đánh giá tuần tự từ trên xuống, khớp rule đầu tiên sẽ dừng lại, không tự tìm "rule phù hợp nhất".
8. **Đúng.** Access Token ngắn hạn (phút-giờ) để giảm rủi ro nếu bị lộ; Refresh Token dài hạn (ngày-tuần) để tránh user phải đăng nhập lại liên tục.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()             // Cụ thể nhất - public
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll() // GET công khai
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")       // Cụ thể - chỉ ADMIN
                .anyRequest().authenticated()                                 // Chung nhất - CUỐI CÙNG
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password())); // BCrypt hash trước khi lưu
        user.setFullName(request.fullName());
        user.setRole(Role.USER);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        // Ném BadCredentialsException nếu sai username/password
        // (Spring Security tự dùng PasswordEncoder.matches() bên trong AuthenticationManager)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user sau khi xác thực thành công"));

        String accessToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new TokenResponse(accessToken));
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
@Service
public class JwtService {

    @Value("${app.jwt.secret-key}")
    private String secretKey;

    @Value("${app.jwt.access-expiration-minutes:15}")
    private long accessExpirationMinutes;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    public String generateToken(User user) {
        return buildToken(user, accessExpirationMinutes * 60 * 1000);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationDays * 24 * 60 * 60 * 1000);
        // Refresh Token thường CHỈ chứa subject (username), KHÔNG cần claim "role"
        // vì mục đích duy nhất là lấy Access Token mới, không dùng để authorize trực tiếp
    }

    private String buildToken(User user, long expirationMillis) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // ADMIN xem được order của ai cũng được, USER chỉ xem được order của chính mình
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.user.id == authentication.principal.id")
    // Dùng @PostAuthorize vì cần kiểm tra dựa trên KẾT QUẢ trả về (order.getUser())
    // (không biết trước order thuộc về userId nào cho tới khi query xong)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
```

*(Giả định `UserDetails` implementation của bạn có method `getId()` để lấy userId từ `authentication.principal`.)*

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

**Lỗi bảo mật:** Kết hợp `setAllowedOrigins("*")` (cho phép TẤT CẢ origin) với `setAllowCredentials(true)` (cho phép gửi kèm Cookie/Credential) là tổ hợp cực kỳ nguy hiểm — về bản chất tương đương "cho phép BẤT KỲ trang web nào trên Internet gọi API kèm theo Cookie/Session của user", mở đường cho tấn công CSRF-like và đánh cắp dữ liệu.

*(Lưu ý: Spec CORS hiện đại thực tế đã cấm tổ hợp này ở tầng trình duyệt — `Access-Control-Allow-Origin: *` không được phép đi kèm `Access-Control-Allow-Credentials: true`, trình duyệt sẽ tự chặn — nhưng vẫn không nên viết code cấu hình sai như vậy vì thể hiện tư duy bảo mật kém và có thể gây lỗi khó hiểu khi test.)*

**Sửa lại — chỉ định rõ danh sách origin được tin tưởng:**

```java
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOrigins(List.of(
    "https://myapp.com",
    "https://admin.myapp.com"
    // Liệt kê CHÍNH XÁC từng domain được phép, KHÔNG dùng wildcard "*"
));
configuration.setAllowCredentials(true); // Giờ mới an toàn để bật, vì origin đã bị giới hạn rõ ràng
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

</details>

---

*File tiếp theo trong lộ trình: **Module 17 — Testing** (JUnit 5, Mockito, @SpringBootTest, Testcontainers, Test Pyramid, MockMvc cho Controller Test, Integration Test vs Unit Test).*
