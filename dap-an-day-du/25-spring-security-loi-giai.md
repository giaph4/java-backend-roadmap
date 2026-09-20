# Lời giải đầy đủ — Module 17: Spring Security

> Nguồn đề: `25-spring-security/25-spring-security.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `SecurityFilterChain` cho REST API dùng JWT

### Đề
Public `/api/v1/auth/**`, chỉ ADMIN `/api/v1/admin/**`, GET công khai `/api/v1/products/**`, còn lại yêu cầu đăng nhập. Đúng thứ tự rule.

### Lời giải

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // bật @PreAuthorize (cần cho Bài 4)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)   // REST API dùng JWT (stateless) - không cần CSRF token
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // THỨ TỰ QUAN TRỌNG - rule CỤ THỂ HƠN phải khai báo TRƯỚC rule TỔNG QUÁT hơn
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .anyRequest().authenticated()   // LUÔN đặt CUỐI CÙNG - rule "bắt hết" các trường hợp còn lại
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Giải thích

- **Thứ tự rule trong `authorizeHttpRequests` được đánh giá THEO ĐÚNG THỨ TỰ KHAI BÁO, dừng lại ở rule ĐẦU TIÊN khớp** — đây là lý do BẮT BUỘC đặt `anyRequest().authenticated()` **CUỐI CÙNG**: nếu đặt nó lên đầu, MỌI request (kể cả `/api/v1/auth/**`) sẽ khớp ngay rule đầu tiên này và yêu cầu đăng nhập, khiến các rule cụ thể hơn phía sau **KHÔNG BAO GIỜ được xét tới** — lỗi cấu hình cực kỳ phổ biến với người mới học Spring Security.
- **`requestMatchers(HttpMethod.GET, "/api/v1/products/**")`** chỉ áp dụng cho **method GET** — `POST/PUT/DELETE` tới cùng path này KHÔNG khớp rule này, sẽ rơi xuống `anyRequest().authenticated()` (yêu cầu đăng nhập) — đúng ngữ nghĩa nghiệp vụ: ai cũng XEM được sản phẩm công khai, nhưng phải đăng nhập mới SỬA/XÓA được.
- **`sessionCreationPolicy(STATELESS)`**: báo cho Spring Security KHÔNG tạo/dùng `HttpSession` — vì xác thực dựa hoàn toàn vào JWT gửi kèm mỗi request (không lưu trạng thái đăng nhập ở server) — đúng triết lý REST API stateless.
- **`csrf().disable()`** hợp lý CHỈ khi API stateless dùng JWT (không dùng session/cookie để xác thực) — CSRF attack khai thác việc trình duyệt TỰ ĐỘNG gửi kèm cookie of phiên đăng nhập; với JWT gửi qua header `Authorization: Bearer ...` (không tự động gửi kèm bởi trình duyệt), rủi ro CSRF không còn áp dụng theo cách tương tự.

---

## Bài 2 — `AuthController` với `/register` và `/login`

### Đề
`POST /register` (hash password bằng BCrypt), `POST /login` (verify password, trả JWT nếu đúng).

### Lời giải

```java
public record RegisterRequest(@NotBlank String username, @NotBlank @Size(min = 8) String password) {}
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
public record AuthResponse(String accessToken, String refreshToken) {}

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username đã tồn tại: " + request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        // TUYỆT ĐỐI KHÔNG lưu password thô (plain text) - luôn hash TRƯỚC KHI lưu xuống DB
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        // authenticate() TỰ ĐỘNG gọi UserDetailsService.loadUserByUsername() + so khớp password
        // qua PasswordEncoder đã cấu hình - ném BadCredentialsException nếu sai
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }
}
```

### Giải thích

- **`passwordEncoder.encode(...)` BẮT BUỘC trước khi lưu password** — BCrypt là thuật toán hash **CHUYÊN DÙNG CHO PASSWORD** (khác hẳn `MD5`/`SHA-256` dùng cho checksum dữ liệu thông thường) — BCrypt CÓ CHỦ ĐÍCH **CHẬM** (tunable "cost factor") và tự động sinh **salt ngẫu nhiên** cho mỗi lần hash, chống lại tấn công Rainbow Table và Brute-force hiệu quả hơn nhiều so với hash nhanh 1 chiều thông thường.
- **`authenticationManager.authenticate(...)` là cách CHUẨN, KHÔNG tự viết code so sánh password thủ công** (VD `passwordEncoder.matches(rawPassword, user.getPassword())` viết tay) — ủy quyền toàn bộ luồng xác thực (tìm user, so khớp password, kiểm tra tài khoản có bị khóa/hết hạn không...) cho cơ chế chuẩn của Spring Security, tận dụng đầy đủ các `AuthenticationProvider` đã cấu hình, tránh viết lại logic dễ sai sót bảo mật.
- **KHÔNG BAO GIỜ trả về password (dù đã hash) trong response** — `AuthResponse` chỉ chứa token, không có bất kỳ field nào liên quan tới password của user.

---

## Bài 3 — `JwtService`: generate, validate, refresh token

### Đề
`generateToken()`, `isTokenValid()`, `generateRefreshToken()` (access = 15 phút, refresh = 7 ngày).

### Lời giải

```java
@Component
public class JwtService {

    @Value("${app.jwt.secret-key}")
    private String secretKey;

    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;              // 15 phút (ms)
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;   // 7 ngày (ms)

    public String generateToken(User user) {
        return buildToken(user, ACCESS_TOKEN_EXPIRATION, Map.of("role", user.getRole()));
    }

    public String generateRefreshToken(User user) {
        // Refresh token KHÔNG cần nhúng claim "role" - chỉ dùng để đổi lấy access token mới,
        // không dùng để xác thực trực tiếp cho các API nghiệp vụ
        return buildToken(user, REFRESH_TOKEN_EXPIRATION, Map.of());
    }

    private String buildToken(User user, long expirationMs, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            String username = extractUsername(token);
            return username.equals(expectedUsername) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            // token bị sửa đổi (sai chữ ký), sai định dạng, hoặc bất kỳ lỗi parse nào khác -> coi là KHÔNG hợp lệ
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload();
        return resolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
```

### Giải thích

- **Vì sao thời gian sống KHÁC NHAU giữa Access Token (15 phút) và Refresh Token (7 ngày):** đây là sự đánh đổi có chủ đích giữa **BẢO MẬT** và **TRẢI NGHIỆM NGƯỜI DÙNG** — Access Token sống NGẮN để giới hạn "cửa sổ rủi ro" nếu bị đánh cắp (kẻ tấn công chỉ lợi dụng được tối đa 15 phút); Refresh Token sống DÀI để người dùng KHÔNG PHẢI đăng nhập lại liên tục mỗi 15 phút — Refresh Token được lưu trữ CẨN THẬN HƠN ở client (thường httpOnly cookie, không phải `localStorage` dễ bị XSS đánh cắp) và chỉ dùng cho 1 mục đích DUY NHẤT: gọi tới 1 endpoint riêng (`/auth/refresh`) để đổi lấy Access Token mới, KHÔNG dùng trực tiếp cho các API nghiệp vụ khác.
- **`isTokenValid` bắt `JwtException`** (lớp cha của nhiều exception cụ thể: `SignatureException` — chữ ký sai do bị giả mạo/sửa đổi, `ExpiredJwtException`, `MalformedJwtException`...) — bắt gộp ở đây và trả `false` đơn giản hóa API cho tầng gọi, dù trong hệ thống lớn có thể cần phân biệt riêng "hết hạn" (để biết có nên dùng refresh token) và "chữ ký sai" (dấu hiệu tấn công, có thể cần log cảnh báo bảo mật riêng).
- **`extraClaims` (role) chỉ nhúng vào Access Token, KHÔNG nhúng vào Refresh Token:** giảm thiểu dữ liệu nhạy cảm mang theo trong Refresh Token (payload JWT KHÔNG được mã hóa, chỉ được KÝ — ai cũng đọc được nội dung nếu decode base64, chỉ không sửa được vì có chữ ký) — nguyên tắc "least privilege": Refresh Token chỉ cần đủ thông tin để xác định user (`subject`), không cần role vì nó không dùng để authorize trực tiếp.

---

## Bài 4 — `@PreAuthorize` cho phân quyền theo dữ liệu

### Đề
ADMIN xem tất cả order; USER chỉ xem order của chính họ (so sánh `userId` với `authentication.principal.id`).

### Lời giải

```java
public interface UserPrincipal {
    Long getId();   // giả định UserDetails implementation của hệ thống có thêm method này
}

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // hasRole('ADMIN') HOẶC (user thường VÀ userId khớp CHÍNH XÁC với id của người đang đăng nhập)
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.userId == authentication.principal.id")
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy order: " + orderId));
    }
}
```

### Giải thích

- **`#userId` trong biểu thức SpEL tham chiếu TRỰC TIẾP tới tham số method** (nhờ `@EnableMethodSecurity` bật parameter name discovery) — cho phép so sánh giá trị TRUYỀN VÀO method (`userId` mà client yêu cầu xem) với `authentication.principal.id` (id của user ĐANG ĐĂNG NHẬP, lấy từ chính JWT đã xác thực) — nếu USER thường cố tình truyền `userId` của NGƯỜI KHÁC, biểu thức trả `false`, Spring Security tự động ném `AccessDeniedException` **TRƯỚC KHI** method thực thi.
- **`@PreAuthorize` vs `@PostAuthorize`:** `@PreAuthorize` kiểm tra **TRƯỚC** khi method chạy (dựa trên tham số đầu vào — phù hợp `getOrdersByUserId` vì `userId` có sẵn ngay từ tham số); `@PostAuthorize` kiểm tra **SAU** khi method đã chạy XONG, dựa trên **KẾT QUẢ TRẢ VỀ** (`returnObject`) — cần thiết cho `getOrderById` vì chỉ SAU KHI đã load được `Order` từ DB mới biết được `order.getUserId()` để so sánh (tham số đầu vào chỉ có `orderId`, không có sẵn `userId` để kiểm tra trước).
- **Nguyên tắc bảo mật quan trọng đằng sau bài này:** đây chính là kiểm soát truy cập **THEO DỮ LIỆU (data-level authorization / object-level authorization)** — khác với kiểm soát chỉ theo ROLE thuần túy (Bài 1, `hasRole('ADMIN')`) — lỗi bảo mật kinh điển **IDOR (Insecure Direct Object Reference)** xảy ra khi hệ thống CHỈ kiểm tra "đã đăng nhập chưa" mà QUÊN kiểm tra "dữ liệu này có THUỘC VỀ người đang đăng nhập không" — sẽ được đào sâu thêm ở Module 24 (Bảo mật OWASP).

---

## Bài 5 — Sửa lỗi bảo mật CORS: `allowedOrigins("*")` + `allowCredentials(true)`

### Đề
```java
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOrigins(List.of("*"));
configuration.setAllowCredentials(true);
```
Chỉ ra lỗi bảo mật và sửa lại.

### Phân tích lỗi

**Đây là 1 tổ hợp cấu hình BỊ TRÌNH DUYỆT TỪ CHỐI** (và về bản chất, nếu "lách" được thì cực kỳ nguy hiểm) — `allowedOrigins("*")` (chấp nhận request từ **BẤT KỲ domain nào**) kết hợp với `allowCredentials(true)` (cho phép gửi kèm cookie/thông tin xác thực) là **KHÔNG ĐƯỢC PHÉP theo chuẩn CORS spec** — trình duyệt hiện đại sẽ **CHẶN** tổ hợp này (báo lỗi console: "The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is 'include'"). Nhưng NẾU giả sử có cách nào đó "lách" được kiểm tra này (VD 1 số thư viện/proxy cấu hình sai phản hồi wildcard thực sự), hậu quả sẽ là: **BẤT KỲ website độc hại nào** cũng có thể gửi request kèm cookie/credential của người dùng đã đăng nhập tới API của bạn — mở đường cho tấn công **CSRF ở quy mô toàn cầu**, đánh cắp dữ liệu người dùng từ domain bất kỳ.

### Lời giải — sửa lại

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // CHỈ liệt kê CHÍNH XÁC các domain THỰC SỰ ĐƯỢC TIN CẬY (frontend chính thức của hệ thống)
    configuration.setAllowedOrigins(List.of(
            "https://app.example.com",
            "https://admin.example.com"
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);   // giờ đây AN TOÀN vì origin đã được GIỚI HẠN CỤ THỂ

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
}
```

### Giải thích

- **Nguyên tắc sửa: KHÔNG BAO GIỜ dùng wildcard `"*"` cho `allowedOrigins` khi CÓ `allowCredentials(true)`** — phải liệt kê **DANH SÁCH TRẮNG (whitelist) TƯỜNG MINH** các domain thực sự tin cậy — đây là biểu hiện cụ thể của nguyên tắc bảo mật tổng quát **"Deny by Default, Allow by Exception"** (mặc định từ chối, chỉ cho phép những gì đã xác nhận rõ ràng).
- **Nếu THỰC SỰ không cần gửi credential (cookie/session) kèm request** (VD API hoàn toàn public, xác thực qua header `Authorization: Bearer` riêng — không dựa vào cookie), có thể giữ `allowedOrigins("*")` NHƯNG PHẢI đổi `allowCredentials(false)` — đây là tổ hợp HỢP LỆ và trình duyệt chấp nhận, phù hợp cho API công khai không cần biết "ai" đang gọi qua cookie.
- **`allowedHeaders`/`allowedMethods` cũng nên khai báo TƯỜNG MINH** (không dùng `"*"` tùy tiện) theo đúng nguyên tắc tối thiểu hóa quyền hạn (principle of least privilege) — chỉ cho phép ĐÚNG những method/header mà frontend thực sự cần dùng.

---

## Bài 6 — `AuthenticationEntryPoint` và `AccessDeniedHandler` trả JSON chuẩn

### Đề
Trả JSON theo format `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`). Đăng ký cả 2 vào `SecurityFilterChain`.

### Lời giải

```java
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {}

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Kích hoạt khi request KHÔNG có / có JWT KHÔNG HỢP LỆ và cố truy cập endpoint cần xác thực -> 401
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Yêu cầu đăng nhập để truy cập tài nguyên này",
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Kích hoạt khi ĐÃ đăng nhập THÀNH CÔNG nhưng KHÔNG ĐỦ QUYỀN (VD USER cố vào /admin/**) -> 403
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Bạn không có quyền truy cập tài nguyên này",
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationEntryPoint authenticationEntryPoint,
                           JwtAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(authenticationEntryPoint)   // xử lý lỗi 401
                    .accessDeniedHandler(accessDeniedHandler)              // xử lý lỗi 403
            );
            // ...các cấu hình khác như Bài 1...

        return http.build();
    }
}
```

### Giải thích

- **Vì sao KHÔNG THỂ dùng `@RestControllerAdvice` (`GlobalExceptionHandler` đã viết ở phần API Design) để xử lý 2 lỗi này:** `AuthenticationException`/`AccessDeniedException` xảy ra ở **TẦNG FILTER CHAIN của Spring Security**, TRƯỚC KHI request đi tới `DispatcherServlet` và các `@RestController` — tại thời điểm đó, cơ chế `@ExceptionHandler` của Spring MVC **CHƯA CÓ CƠ HỘI can thiệp** (nó chỉ bắt exception ném ra TỪ BÊN TRONG Controller) — vì vậy Spring Security cung cấp 2 interface RIÊNG (`AuthenticationEntryPoint`, `AccessDeniedHandler`) chuyên xử lý lỗi ở TẦNG SECURITY FILTER, độc lập với cơ chế exception handling của MVC.
- **Phân biệt RÕ 401 vs 403 — lỗi rất hay bị nhầm lẫn:**
  - **401 Unauthorized** (`AuthenticationEntryPoint`): chưa xác định được DANH TÍNH — request KHÔNG có JWT, hoặc JWT sai/hết hạn — "tôi không biết bạn là ai".
  - **403 Forbidden** (`AccessDeniedHandler`): ĐÃ xác định được danh tính (JWT hợp lệ, biết chính xác bạn là ai), nhưng bạn **KHÔNG ĐỦ QUYỀN** cho hành động này — "tôi biết bạn là ai, nhưng bạn không được phép làm việc này".
- **Nhất quán format `ErrorResponse` xuyên suốt toàn bộ ứng dụng** (giống hệt cấu trúc đã dùng ở `GlobalExceptionHandler` cho lỗi nghiệp vụ) — đảm bảo client (frontend) xử lý MỌI loại lỗi (nghiệp vụ lẫn bảo mật) theo CÙNG 1 CẤU TRÚC dữ liệu thống nhất, không cần viết logic parse riêng cho từng loại lỗi khác nhau.

---

*Đây là lời giải cho toàn bộ Phần B của Module 25. Tiếp theo: Module 18 — Testing.*
