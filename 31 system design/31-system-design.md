# Module 22 — System Design cơ bản cho Backend

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Đây là module **tổng hợp toàn bộ lộ trình** — Load Balancing, Scaling, Replication/Sharding, CDN, Rate Limiting đều là những khái niệm bạn đã chạm tới rải rác (Module 10, 18, 19) nhưng giờ được ghép lại thành **tư duy thiết kế hệ thống end-to-end**. System Design Interview là vòng phỏng vấn **quyết định** cho vị trí Mid/Senior Backend — không đánh giá "bạn code giỏi không" mà đánh giá "bạn có hiểu đánh đổi (trade-off) khi xây hệ thống chịu được hàng triệu user không". Đây cũng là kỹ năng trực tiếp áp dụng khi bạn thiết kế kiến trúc cho đồ án/dự án thực tế.

---

## Mục lục

1. [Scaling: Vertical vs Horizontal](#1-scaling-vertical-vs-horizontal)
2. [Load Balancing](#2-load-balancing)
3. [Database Replication](#3-database-replication)
4. [Database Sharding](#4-database-sharding)
5. [CDN (Content Delivery Network)](#5-cdn)
6. [Rate Limiting](#6-rate-limiting)
7. [Quy trình tiếp cận 1 câu hỏi System Design](#7-quy-trình-tiếp-cận-1-câu-hỏi-system-design)
8. [Case Study 1: Thiết kế URL Shortener (bit.ly)](#8-case-study-1-url-shortener)
9. [Case Study 2: Thiết kế News Feed (Facebook/Twitter)](#9-case-study-2-news-feed)
10. [⚠️ Các bẫy hay gặp](#10-các-bẫy-hay-gặp)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. Scaling: Vertical vs Horizontal

### Vertical Scaling (Scale Up) — nâng cấp 1 máy chủ mạnh hơn

```
Trước: 1 server (4 CPU, 8GB RAM)
Sau:   1 server (32 CPU, 128GB RAM)  <- CÙNG 1 máy, chỉ nâng cấp phần cứng
```

| Ưu điểm | Nhược điểm |
|---|---|
| Đơn giản — không cần sửa code, không cần Load Balancer | **Có GIỚI HẠN VẬT LÝ** — không thể nâng cấp vô hạn |
| Không có vấn đề đồng bộ dữ liệu (chỉ 1 máy, 1 DB) | **Single Point of Failure** — máy chết là toàn hệ thống chết |
| | Chi phí tăng theo cấp SỐ MŨ khi lên cấu hình cao (máy càng mạnh càng đắt phi tuyến) |

### Horizontal Scaling (Scale Out) — thêm NHIỀU máy chủ chạy song song

```
Trước: 1 server (4 CPU, 8GB RAM)
Sau:   4 server, mỗi cái (4 CPU, 8GB RAM)  <- NHIỀU máy giống nhau, chạy song song
       + cần Load Balancer để phân phối traffic
```

| Ưu điểm | Nhược điểm |
|---|---|
| **Không giới hạn** — thêm máy khi cần (đã có nền tảng ở Module 19/20: Kubernetes tự scale) | **Phức tạp hơn** — cần Load Balancer, Service Discovery |
| **Chịu lỗi tốt hơn** — 1 máy chết, các máy khác vẫn phục vụ | Vấn đề **đồng bộ trạng thái** giữa các instance (Session, Cache — đã học Module 16, 18) |
| Chi phí tăng TUYẾN TÍNH (thêm máy giá tương đương) | Database thường khó horizontal scale hơn Application server (mục 3, 4) |

> **Nguyên tắc thực chiến:** Ứng dụng hiện đại luôn thiết kế để **Stateless** (đã học ở Module 14, 16 — JWT thay vì Session lưu server) chính là để **horizontal scale dễ dàng** — bất kỳ instance nào cũng xử lý được request bất kỳ, không cần "dính" vào 1 server cụ thể.

---

## 2. Load Balancing

**Load Balancer (LB)** đứng giữa Client và các Server instance, phân phối traffic đều để không server nào bị quá tải trong khi server khác rảnh rỗi.

```
                    ┌──────────────┐
Client ────────────►│Load Balancer  │
                    └──────┬───────┘
              ┌────────────┼────────────┐
              ▼             ▼             ▼
        ┌─────────┐  ┌─────────┐  ┌─────────┐
        │Server 1  │  │Server 2  │  │Server 3  │
        └─────────┘  └─────────┘  └─────────┘
```

### Các thuật toán phân phối phổ biến

| Thuật toán | Cơ chế |
|---|---|
| **Round Robin** | Phân phối lần lượt theo vòng tròn (Server 1 → 2 → 3 → 1 → ...) — đơn giản, phổ biến nhất |
| **Least Connections** | Route request tới server đang có **ÍT connection đang xử lý nhất** — công bằng hơn khi các request có độ nặng khác nhau |
| **IP Hash** | Hash địa chỉ IP client → LUÔN route về CÙNG 1 server — hữu ích khi cần "Sticky Session" (dù bản thân Sticky Session không phải best practice, đã bàn ở Module 16) |
| **Weighted Round Robin** | Server mạnh hơn nhận tỷ trọng traffic cao hơn (VD: Server A gấp đôi CPU Server B → nhận gấp đôi request) |

### Layer 4 vs Layer 7 Load Balancer

| | Layer 4 (Transport Layer) | Layer 7 (Application Layer) |
|---|---|---|
| Dựa trên | IP + Port (TCP/UDP) | Nội dung HTTP (path, header, cookie) |
| Tốc độ | Nhanh hơn (ít xử lý) | Chậm hơn 1 chút (phải đọc nội dung HTTP) |
| Khả năng route thông minh | Hạn chế | **Cao** — route theo path (`/api/orders` → Order Service, `/api/users` → User Service — liên hệ API Gateway ở Module 19) |
| Ví dụ | AWS Network Load Balancer (NLB) | Nginx, AWS Application Load Balancer (ALB), Spring Cloud Gateway |

### Health Check — Load Balancer cần biết Server nào còn "sống"

```
Load Balancer định kỳ gọi GET /actuator/health (liên hệ Module 13) tới từng server
     │
     ├─► Server phản hồi 200 OK -> tiếp tục nhận traffic
     └─► Server không phản hồi/lỗi -> LOẠI KHỎI danh sách, KHÔNG route traffic tới nữa
         cho tới khi health check lại thành công
```

---

## 3. Database Replication

**Vấn đề:** Application Server dễ horizontal scale (stateless, thêm instance là xong), nhưng **Database thường là điểm nghẽn (bottleneck)** — nhiều Application instance cùng gọi vào **1 Database duy nhất**.

### Master-Slave (Primary-Replica) Replication

```
                  ┌──────────────┐
       Write ────►│ Master (Primary) │  <- CHỈ Master nhận GHI (INSERT/UPDATE/DELETE)
                  └──────┬───────┘
                          │ Replicate (sao chép dữ liệu, thường bất đồng bộ)
              ┌───────────┼───────────┐
              ▼            ▼            ▼
        ┌─────────┐ ┌─────────┐ ┌─────────┐
Read ──►│ Replica 1 │ │ Replica 2 │ │ Replica 3 │  <- Đọc (SELECT) phân tán ra nhiều Replica
        └─────────┘ └─────────┘ └─────────┘
```

**Nguyên lý:** Tách biệt luồng **Ghi** (chỉ 1 Master) và **Đọc** (nhiều Replica) — phù hợp với đặc tính **read-heavy** phổ biến của đa số ứng dụng (đọc nhiều hơn ghi rất nhiều lần, VD: đọc feed, đọc sản phẩm).

```java
// Cấu hình Spring Boot với 2 DataSource (Master ghi, Replica đọc)
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource masterDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://master-db:3306/mydb")
                .build();
    }

    @Bean
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://replica-db:3306/mydb")
                .build();
    }
}

@Service
public class OrderService {

    @Transactional // Mặc định dùng Master (ghi)
    public void createOrder(Order order) { orderRepository.save(order); }

    @Transactional(readOnly = true) // Có thể route sang Replica (đọc) qua AbstractRoutingDataSource
    public List<Order> getOrders() { return orderRepository.findAll(); }
}
```

### Vấn đề: Replication Lag (Độ trễ đồng bộ)

⚠️ **Bẫy cực kỳ quan trọng:** Việc sao chép dữ liệu từ Master sang Replica thường là **BẤT ĐỒNG BỘ (asynchronous)** — có 1 khoảng trễ (thường mili-giây tới vài giây). Nếu Client **ghi xong LẬP TỨC đọc lại** (VD: vừa tạo Order, load lại trang chi tiết Order NGAY) mà đọc từ Replica **CHƯA KỊP đồng bộ** → **KHÔNG thấy dữ liệu vừa ghi** (dù đã ghi thành công vào Master).

```
Timeline:
T1: User submit "Tạo Order" -> Ghi vào MASTER thành công
T2: Client redirect NGAY tới trang "Chi tiết Order" -> Đọc từ REPLICA
T3: Replica CHƯA kịp đồng bộ dữ liệu Order vừa tạo -> Trả về "Không tìm thấy Order"!
     (dù Order ĐÃ tồn tại trong Master từ T1)
```

**Giải pháp:** Với các thao tác **"ghi rồi đọc ngay"** (Read-after-Write), nên đọc TRỰC TIẾP từ Master (chấp nhận không tận dụng Replica cho trường hợp cụ thể này), hoặc dùng cơ chế "Read Your Own Writes" (route riêng user vừa ghi về Master trong 1 khoảng thời gian ngắn).

---

## 4. Database Sharding

**Vấn đề tiếp theo:** Replication giải quyết vấn đề **ĐỌC** (scale ra nhiều Replica), nhưng **KHÔNG giải quyết** vấn đề **GHI** — vẫn chỉ có **1 Master** xử lý toàn bộ ghi. Khi dữ liệu/tải ghi quá lớn (hàng tỷ dòng, hàng chục nghìn write/giây), **1 Master không đủ sức** dù phần cứng có mạnh tới đâu (giới hạn Vertical Scaling).

### Sharding — chia nhỏ dữ liệu ra NHIỀU Database độc lập

```
┌───────────┐   ┌───────────┐   ┌───────────┐
│  Shard 1    │   │  Shard 2    │   │  Shard 3    │
│ user_id      │   │ user_id      │   │ user_id      │
│ 1-1000000    │   │ 1000001-     │   │ 2000001-     │
│              │   │ 2000000      │   │ 3000000      │
└───────────┘   └───────────┘   └───────────┘
     Mỗi Shard là 1 Database RIÊNG BIỆT, chứa 1 PHẦN dữ liệu
```

### Các chiến lược Sharding phổ biến

**1. Range-based Sharding** — chia theo khoảng giá trị (VD: user_id 1-1M vào Shard 1, 1M-2M vào Shard 2):
```
Ưu điểm: Đơn giản, dễ query theo range (VD: "user_id BETWEEN X AND Y")
Nhược điểm: Dễ bị "Hotspot" - nếu user MỚI luôn có ID lớn, Shard mới nhất bị quá tải,
            trong khi Shard cũ (user_id nhỏ) ít hoạt động
```

**2. Hash-based Sharding** — dùng hàm hash (VD: `hash(user_id) % số_lượng_shard`):
```
Ưu điểm: Phân phối ĐỀU dữ liệu across các shard, tránh Hotspot
Nhược điểm: Khó query range (VD: "lấy tất cả user có ID từ 100-200" phải hỏi TẤT CẢ shard),
            khó THÊM/BỚT shard sau này (đổi số lượng shard làm hash thay đổi -> phải re-shard toàn bộ)
```

**3. Geographic/Directory-based Sharding** — chia theo khu vực địa lý hoặc dùng 1 bảng lookup riêng:
```
Ưu điểm: Dữ liệu gần user (giảm latency), tuân thủ luật lưu trữ dữ liệu theo quốc gia
Nhược điểm: Cần bảng lookup/routing riêng, phức tạp hơn khi user "di chuyển" giữa các vùng
```

### Vấn đề lớn nhất của Sharding: Cross-shard Query & Cross-shard Transaction

```sql
-- Query đơn giản khi KHÔNG sharding:
SELECT * FROM orders WHERE user_id = 12345;  -- 1 query, 1 DB

-- Khi ĐÃ sharding theo user_id, muốn tìm "TOP 10 SẢN PHẨM BÁN CHẠY NHẤT TOÀN HỆ THỐNG"
-- (dữ liệu này TRẢI RỘNG trên MỌI shard, không thể "SELECT ... GROUP BY" đơn giản như trước)
-- -> phải query TỪNG shard riêng biệt, rồi TỔNG HỢP kết quả ở tầng ứng dụng (Scatter-Gather)
```

⚠️ **Đây chính là lý do Sharding thường đi kèm với các giải pháp khác** (Read Replica cho từng Shard, hoặc tổng hợp dữ liệu riêng vào 1 Data Warehouse/Read Model dùng cho báo cáo/thống kê tổng thể — liên hệ CQRS đã giới thiệu ở Module 19).

> **Liên hệ Module 19 (Database per Service):** Sharding và "Database per Service" trong Microservices thực chất giải quyết **CÙNG 1 vấn đề cốt lõi** (chia nhỏ dữ liệu để scale) — chỉ khác là Sharding chia theo **giá trị dữ liệu** (user_id), còn Database per Service chia theo **ranh giới nghiệp vụ** (domain boundary).

---

## 5. CDN

**CDN (Content Delivery Network)** là mạng lưới server đặt **RẢI RÁC theo địa lý** (gần user hơn), lưu cache các nội dung **tĩnh** (static: hình ảnh, CSS, JS, video) — giảm độ trễ và tải cho server gốc (Origin Server).

```
Không có CDN:
User ở Việt Nam -> Request TỚI TẬN server gốc ở Mỹ (độ trễ CAO do khoảng cách địa lý)

Có CDN:
User ở Việt Nam -> Request TỚI CDN node GẦN NHẤT (VD: Singapore) -> trả về NGAY từ cache
                   (chỉ khi CDN chưa có cache, mới forward về server gốc 1 lần, rồi cache lại)
```

```
┌──────────────────────────────────────────────┐
│                  CDN Network                       │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐           │
│  │Node Asia  │  │Node Europe │  │Node US    │           │
│  └────┬────┘  └────┬────┘  └────┬────┘           │
└───────┼────────────┼────────────┼───────────────┘
        │             │             │ (chỉ khi cache miss)
        └─────────────┴─────────────┘
                       ▼
              ┌───────────────┐
              │ Origin Server   │
              └───────────────┘
```

**Lợi ích:**
- Giảm **latency** đáng kể cho user ở xa server gốc
- Giảm tải cho Origin Server (CDN đã "chặn" phần lớn request tới nội dung tĩnh)
- Chống DDoS tốt hơn (CDN có hạ tầng lớn, hấp thụ được traffic bất thường)

> **Liên hệ trực tiếp Caching (Module 18):** CDN về bản chất là **Cache-Aside pattern** đã học, chỉ khác là áp dụng ở **quy mô địa lý toàn cầu**, cho nội dung TĨNH, thay vì cache dữ liệu động trong 1 Redis server như đã học.

---

## 6. Rate Limiting

**Vấn đề:** Không giới hạn số request 1 client có thể gửi → dễ bị lạm dụng (1 client gửi hàng triệu request/giây) — vô tình (bug ở client) hoặc cố ý (DDoS, brute-force tấn công login).

### Các thuật toán Rate Limiting phổ biến

**1. Token Bucket** (phổ biến nhất, cân bằng tốt giữa đơn giản và linh hoạt):

```
Bucket chứa TỐI ĐA N token, được "refill" (nạp thêm) theo tốc độ cố định (VD: 10 token/giây)
Mỗi request TIÊU THỤ 1 token
Bucket HẾT token -> request bị TỪ CHỐI (429 Too Many Requests - đã học Module 14)

Ưu điểm: Cho phép "burst" (đợt tăng đột biến ngắn hạn) miễn là bucket còn token,
         phù hợp traffic thực tế không đều đặn tuyệt đối
```

**2. Sliding Window Log:**

```
Lưu TIMESTAMP của MỖI request trong "cửa sổ thời gian" (VD: 1 phút gần nhất)
Đếm số request trong cửa sổ đó, nếu vượt ngưỡng -> từ chối

Ưu điểm: Chính xác tuyệt đối
Nhược điểm: Tốn bộ nhớ (phải lưu timestamp của MỌI request)
```

**3. Fixed Window Counter (đơn giản nhất nhưng có nhược điểm):**

```
Đếm số request trong MỖI khung thời gian CỐ ĐỊNH (VD: 0:00-0:59, 1:00-1:59...)
Hết khung -> reset counter về 0

⚠️ Nhược điểm: Vấn đề ở RANH GIỚI khung giờ - user có thể gửi tối đa request
   vào CUỐI khung giờ trước + TỐI ĐA request vào ĐẦU khung giờ sau
   -> trong 1 khoảng thời gian ngắn (bắc cầu qua ranh giới), request có thể
      gấp ĐÔI giới hạn cho phép mà không bị chặn
```

### Triển khai Rate Limiting với Redis (liên hệ Module 18)

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int MAX_REQUESTS = 100; // Tối đa 100 request
    private static final int WINDOW_SECONDS = 60; // Trong 60 giây

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {
        String clientId = request.getRemoteAddr(); // Hoặc lấy từ API key/JWT subject
        String key = "rate_limit:" + clientId;

        Long currentCount = redisTemplate.opsForValue().increment(key); // Redis INCR - atomic, an toàn với concurrency
        if (currentCount == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS)); // Set TTL cho lần đầu
        }

        if (currentCount > MAX_REQUESTS) {
            response.setStatus(429); // Too Many Requests - đã học ở Module 14
            response.getWriter().write("{\"message\": \"Vượt quá giới hạn request, vui lòng thử lại sau\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

> **Nơi triển khai Rate Limiting trong thực tế:** Thường đặt tại **API Gateway** (đã học Module 19) — tập trung tại 1 điểm, áp dụng cho toàn hệ thống, không cần lặp lại logic ở từng service riêng lẻ (giống nguyên tắc tập trung Authentication tại Gateway).

---

## 7. Quy trình tiếp cận 1 câu hỏi System Design

Đây là **khung tư duy** (không phải công thức cứng nhắc) để tiếp cận bất kỳ câu hỏi System Design nào (phỏng vấn hoặc thực tế):

```
Bước 1: LÀM RÕ YÊU CẦU (Clarify Requirements)
   - Functional: Hệ thống cần làm gì? (VD: rút gọn URL, redirect khi truy cập)
   - Non-functional: Bao nhiêu user? Bao nhiêu request/giây? Đọc nhiều hay ghi nhiều?
     Cần Strong Consistency hay Eventual Consistency chấp nhận được?

Bước 2: ƯỚC LƯỢNG QUY MÔ (Capacity Estimation)
   - Số lượng user, request/giây (QPS - Queries Per Second)
   - Lượng dữ liệu lưu trữ (bao nhiêu GB/TB dữ liệu sau 1 năm?)
   - Tỷ lệ Đọc:Ghi (Read:Write ratio)

Bước 3: THIẾT KẾ API/DATA MODEL (High-level Design)
   - API endpoint chính (liên hệ Module 14 - REST API Design)
   - Database schema cơ bản (liên hệ Module 10, 11)

Bước 4: THIẾT KẾ KIẾN TRÚC TỔNG THỂ (Architecture Diagram)
   - Vẽ sơ đồ: Client -> LB -> App Server -> Cache -> Database
   - Xác định nơi cần Cache, CDN, Message Queue...

Bước 5: ĐI SÂU VÀO CÁC ĐIỂM QUAN TRỌNG (Deep Dive)
   - Bottleneck nằm ở đâu? Cần Sharding/Replication không?
   - Xử lý Edge Case (dữ liệu trùng lặp, race condition...)

Bước 6: THẢO LUẬN ĐÁNH ĐỔI (Trade-offs)
   - Không có giải pháp "hoàn hảo" - LUÔN có đánh đổi
   - Thể hiện rõ VÌ SAO chọn giải pháp này thay vì giải pháp khác
```

> **Điều quan trọng nhất của System Design Interview:** Không có "đáp án đúng duy nhất" — người phỏng vấn đánh giá **quá trình tư duy, khả năng đặt câu hỏi làm rõ yêu cầu, và hiểu đánh đổi** — chứ không phải việc thuộc lòng 1 kiến trúc cụ thể.

---

## 8. Case Study 1: URL Shortener

**Yêu cầu:** Xây dựng dịch vụ giống bit.ly — nhận URL dài, trả về URL ngắn; khi truy cập URL ngắn, redirect tới URL gốc.

### Bước 1-2: Làm rõ yêu cầu & Ước lượng quy mô

```
Functional:
- POST /shorten {longUrl} -> trả về {shortUrl}
- GET /{shortCode} -> redirect (302) tới longUrl gốc

Non-functional:
- Giả sử: 100 triệu URL mới/tháng, tỷ lệ Đọc:Ghi = 100:1 (đọc/redirect NHIỀU HƠN rất nhiều so với tạo mới)
- Cần độ trễ redirect THẤP (user không nên chờ lâu khi click link)
- Không cần Strong Consistency tuyệt đối (URL tạo xong sau vài giây mới truy cập được cũng chấp nhận được)
```

### Bước 3: Thiết kế Data Model & thuật toán sinh Short Code

```sql
CREATE TABLE urls (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL
);
CREATE INDEX idx_short_code ON urls(short_code); -- Liên hệ Module 10 - Index cực kỳ quan trọng cho lookup
```

**Thuật toán sinh Short Code — Base62 Encoding (chuyển ID số thành chuỗi ngắn):**

```java
private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

public String encode(long id) {
    StringBuilder sb = new StringBuilder();
    while (id > 0) {
        sb.append(BASE62.charAt((int) (id % 62)));
        id /= 62;
    }
    return sb.reverse().toString();
    // VD: id = 125 -> mã hóa thành chuỗi ngắn dùng 62 ký tự (0-9, a-z, A-Z)
    // 62^6 = ~56 tỷ tổ hợp với chỉ 6 ký tự - đủ dùng trong thời gian dài
}
```

> **Đây chính là ứng dụng thực tế của Auto-increment ID (Module 11 — GenerationType.IDENTITY/SEQUENCE)** — tận dụng ID tăng dần sẵn có, encode thành chuỗi ngắn thay vì tự sinh chuỗi ngẫu nhiên (tránh trùng lặp phức tạp).

### Bước 4-5: Kiến trúc tổng thể & Deep Dive

```
Client -> Load Balancer -> App Server (nhiều instance, Stateless)
                              │
                              ├─► Cache (Redis) - CHECK TRƯỚC khi query DB
                              │    (liên hệ Module 18 - Cache-Aside Pattern,
                              │     vì tỷ lệ Đọc:Ghi = 100:1, cache cực kỳ hiệu quả ở đây)
                              │
                              └─► Database (URL mapping)
                                   - Có thể Sharding theo short_code nếu quy mô cực lớn
                                   - Read Replica cho luồng redirect (đọc nhiều)
```

```java
@GetMapping("/{shortCode}")
public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
    String longUrl = urlCacheService.getLongUrl(shortCode); // Cache-Aside: check Redis trước
    if (longUrl == null) {
        throw new UrlNotFoundException(shortCode);
    }
    return ResponseEntity.status(HttpStatus.FOUND) // 302 - Redirect tạm thời
            .location(URI.create(longUrl))
            .build();
}
```

⚠️ **Điểm cần thảo luận (Trade-off) trong phỏng vấn:** Dùng **302 Found** (redirect tạm thời, browser KHÔNG cache) hay **301 Moved Permanently** (browser TỰ cache, lần sau KHÔNG gọi lại server)? → 301 giảm tải server hơn nhiều, nhưng **mất khả năng đếm số lượt click** (vì browser không gọi lại server nữa) — nếu nghiệp vụ cần thống kê click, PHẢI dùng 302 dù tốn tài nguyên hơn.

---

## 9. Case Study 2: News Feed

**Yêu cầu:** Xây dựng News Feed giống Facebook/Twitter — user đăng bài (post), follow người khác, xem feed tổng hợp bài viết từ những người mình follow, sắp xếp theo thời gian.

### Bước 1-2: Làm rõ yêu cầu & Ước lượng quy mô

```
Functional:
- POST /posts -> đăng bài mới
- GET /feed -> lấy feed (bài viết từ người mình follow)
- POST /follow/{userId} -> follow người khác

Non-functional:
- Tỷ lệ Đọc:Ghi RẤT CAO (user xem feed liên tục, nhưng chỉ thỉnh thoảng đăng bài)
- Độ trễ xem feed phải THẤP (user mong đợi feed load nhanh)
- 1 số user có HÀNG TRIỆU follower (celebrity) - vấn đề đặc biệt cần xử lý riêng
```

### Bước 3-4: Thiết kế cốt lõi — Fan-out Pattern

Đây là **quyết định kiến trúc quan trọng nhất** của bài toán News Feed:

**Fan-out on Write (Push Model) — TÍNH TOÁN TRƯỚC feed khi có bài đăng mới:**

```
User A đăng bài mới
     │
     ▼
NGAY LẬP TỨC, hệ thống "đẩy" (push) bài viết này vào feed CỦA TẤT CẢ follower của A
(lưu sẵn vào Redis/DB riêng cho từng follower - "materialized feed")
     │
     ▼
Khi follower B mở app xem feed -> CHỈ CẦN đọc feed ĐÃ TÍNH SẴN (cực nhanh, không cần query phức tạp)
```

```java
@Service
public class PostService {

    @Async // Xử lý fan-out bất đồng bộ (liên hệ Module 18) - không chặn response của việc đăng bài
    public void fanOutToFollowers(Post post) {
        List<Long> followerIds = followRepository.findFollowerIds(post.getUserId());
        for (Long followerId : followerIds) {
            // Đẩy post vào feed cache của TỪNG follower (VD: Redis Sorted Set, score = timestamp)
            redisTemplate.opsForZSet().add("feed:" + followerId, post.getId().toString(), post.getCreatedAt().toEpochSecond());
        }
    }
}
```

**Ưu điểm:** Đọc feed CỰC NHANH (đã tính sẵn).
**Nhược điểm — "Celebrity Problem":** Nếu 1 user có **10 triệu follower** đăng bài, hệ thống phải ghi vào **10 triệu feed riêng biệt CÙNG LÚC** → tốn tài nguyên khủng khiếp, có thể làm chậm/sập hệ thống ngay tại thời điểm đăng bài.

**Fan-out on Read (Pull Model) — TÍNH TOÁN feed NGAY KHI user mở app xem:**

```
User B mở app xem feed
     │
     ▼
NGAY LÚC ĐÓ, hệ thống query "lấy bài viết MỚI NHẤT từ TẤT CẢ người B đang follow"
(query real-time, KHÔNG có dữ liệu tính sẵn)
```

**Ưu điểm:** Không có vấn đề "Celebrity Problem" khi ĐĂNG bài (không cần ghi hàng triệu nơi).
**Nhược điểm:** Xem feed CHẬM HƠN (phải query + gộp dữ liệu real-time mỗi lần mở app) — tệ hơn khi user follow NHIỀU người (phải gộp dữ liệu từ nhiều nguồn).

### Giải pháp thực tế — Hybrid Approach (Kết hợp cả 2, đây chính là câu trả lời "khôn ngoan" trong phỏng vấn)

```
- User THƯỜNG (follower ít) -> dùng Fan-out on WRITE (push) -> feed đọc nhanh
- User CELEBRITY (follower RẤT nhiều, VD > 1 triệu) -> dùng Fan-out on READ (pull)
  -> tránh phải ghi hàng triệu feed cùng lúc khi họ đăng bài

Khi user B xem feed:
  - Lấy feed ĐÃ TÍNH SẴN (từ những người thường B follow) - nhanh
  - + Query REAL-TIME riêng cho các Celebrity B đang follow - ít người nên query nhanh
  - Gộp 2 nguồn lại, sắp xếp theo thời gian, trả về
```

> **Đây chính là bài học cốt lõi của System Design Interview:** Không có 1 giải pháp "đúng tuyệt đối" cho mọi trường hợp — Fan-out on Write và Fan-out on Read đều có **đánh đổi riêng**, và giải pháp thực tế thường là **kết hợp (Hybrid)** dựa trên đặc điểm cụ thể của dữ liệu (VD: phân loại user thường/celebrity).

---

## 10. ⚠️ Các bẫy hay gặp

1. **Đi thẳng vào thiết kế chi tiết mà KHÔNG làm rõ yêu cầu trước** (bỏ qua Bước 1) — trong phỏng vấn, đây là lỗi bị đánh giá thấp nhất, vì thể hiện thiếu tư duy hệ thống.

2. **Coi Vertical Scaling là giải pháp lâu dài** — luôn có giới hạn vật lý, không phải hướng đi bền vững cho hệ thống lớn.

3. **Không nhận ra Replication Lag** khi thiết kế luồng "ghi rồi đọc ngay" — dẫn tới bug "dữ liệu vừa tạo không thấy" khó tái hiện (chỉ xảy ra khi tải cao, độ trễ replication tăng).

4. **Chọn Sharding key sai** (VD: sharding theo `created_at` thay vì `user_id`) — gây Hotspot nghiêm trọng (mọi ghi mới đều dồn vào 1 shard duy nhất, các shard khác "chết" theo thời gian).

5. **Dùng Fixed Window Counter cho Rate Limiting** mà không nhận ra vấn đề "ranh giới khung giờ" — cho phép traffic gấp đôi giới hạn trong khoảng bắc cầu.

6. **Không cân nhắc "Celebrity Problem"** khi thiết kế hệ thống Social Network — dùng THUẦN Fan-out on Write sẽ sập hệ thống khi user nổi tiếng đăng bài.

7. **Thiết kế hệ thống đòi hỏi Strong Consistency tuyệt đối cho MỌI thứ** — không phải mọi dữ liệu đều cần vậy (VD: số lượt like có thể chấp nhận hiển thị trễ vài giây — Eventual Consistency), gây tốn kém tài nguyên không cần thiết.

8. **Bỏ qua CDN cho nội dung tĩnh** — để mọi request (kể cả ảnh, CSS, JS) đều đánh thẳng vào Origin Server, lãng phí tài nguyên và tăng độ trễ không cần thiết cho user ở xa.

9. **Không ước lượng quy mô (Capacity Estimation)** trước khi thiết kế — dẫn tới thiết kế "quá tay" (over-engineering cho quy mô nhỏ) hoặc "thiếu tay" (không đủ sức chịu tải thực tế).

10. **Chỉ đưa ra 1 giải pháp mà không thảo luận đánh đổi (Trade-off)** — trong phỏng vấn, thể hiện khả năng phân tích NHIỀU phương án và đánh đổi giữa chúng quan trọng hơn nhiều so với việc đưa ra "câu trả lời đúng" duy nhất.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Vertical vs Horizontal Scaling | Nâng cấp 1 máy (giới hạn) vs thêm nhiều máy (không giới hạn, cần LB) |
| Load Balancer | Phân phối traffic — Round Robin/Least Connections/IP Hash; Layer 4 (nhanh) vs Layer 7 (thông minh) |
| Master-Slave Replication | 1 Master ghi, nhiều Replica đọc — cẩn thận Replication Lag |
| Sharding | Chia dữ liệu ra nhiều DB độc lập — Range/Hash-based; khó Cross-shard Query |
| CDN | Cache nội dung tĩnh gần user về mặt địa lý — giảm latency, giảm tải Origin Server |
| Rate Limiting | Token Bucket (phổ biến) / Sliding Window / Fixed Window (có bug ranh giới) |
| Quy trình System Design | Làm rõ yêu cầu → Ước lượng quy mô → Data Model → Kiến trúc tổng thể → Deep Dive → Trade-off |
| URL Shortener | Base62 encode ID, Cache-Aside cho redirect, cân nhắc 301 vs 302 |
| News Feed Fan-out | Push (ghi trước, đọc nhanh) vs Pull (đọc real-time) — Hybrid cho Celebrity Problem |
| Nguyên tắc cốt lõi | Không có giải pháp hoàn hảo — LUÔN có đánh đổi (trade-off) |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Vertical Scaling có thể mở rộng vô hạn nếu có đủ ngân sách.
2. Master-Slave Replication giúp giải quyết cả vấn đề Đọc VÀ Ghi khi tải tăng cao.
3. Fixed Window Counter cho Rate Limiting có thể cho phép traffic vượt quá giới hạn cho phép tại ranh giới giữa 2 khung thời gian.
4. CDN chủ yếu dùng để cache nội dung ĐỘNG (dữ liệu thay đổi liên tục theo user), không phải nội dung tĩnh.
5. Fan-out on Write (Push Model) phù hợp hơn cho user có SỐ LƯỢNG FOLLOWER RẤT LỚN (celebrity).
6. Trong System Design Interview, việc làm rõ yêu cầu (Functional/Non-functional) trước khi thiết kế chi tiết là bước quan trọng, không nên bỏ qua.
7. Sharding theo Hash-based giúp phân phối dữ liệu đều hơn Range-based, nhưng khó thực hiện query theo khoảng giá trị (range query).
8. Replication Lag có thể gây ra tình huống user vừa tạo dữ liệu xong nhưng đọc lại ngay không thấy dữ liệu đó.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Viết thuật toán Base62 Encode/Decode đầy đủ (cả 2 chiều: từ ID số sang chuỗi ngắn, và ngược lại từ chuỗi ngắn về ID số) cho hệ thống URL Shortener.

**Bài 2:** Viết `RateLimitingFilter` áp dụng thuật toán **Token Bucket** (khác với Fixed Window Counter đã có ví dụ trong bài — dùng Redis để lưu trạng thái token còn lại và thời điểm refill gần nhất).

**Bài 3:** Thiết kế Database Schema (SQL) cho hệ thống News Feed đơn giản gồm: `users`, `posts`, `follows` — kèm Index cần thiết để tối ưu truy vấn "lấy feed của 1 user" (Fan-out on Read).

**Bài 4:** Viết pseudo-code (Java) cho luồng Hybrid Fan-out: khi user đăng bài, kiểm tra nếu số follower > ngưỡng (VD: 100,000) thì KHÔNG fan-out ngay (để dành cho Fan-out on Read), ngược lại thì fan-out bình thường vào feed cache của từng follower.

**Bài 5:** Cho yêu cầu: "Thiết kế hệ thống đếm lượt xem video (View Count) cho 1 nền tảng video lớn, mỗi video có thể có hàng triệu lượt xem/ngày". Áp dụng quy trình 6 bước ở mục 7 để phác thảo (ngắn gọn) giải pháp — đặc biệt chú ý: có cần Strong Consistency cho số View Count hiển thị không, hay có thể chấp nhận Eventual Consistency để tối ưu hiệu năng?

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Vertical Scaling LUÔN có giới hạn vật lý (không có CPU/RAM vô hạn), và chi phí tăng theo cấp số mũ ở mức cấu hình cao, không phải giải pháp scale vô hạn.
2. **Sai.** Master-Slave Replication chỉ giải quyết vấn đề ĐỌC (scale ra nhiều Replica) — vấn đề GHI vẫn bị giới hạn bởi 1 Master duy nhất, cần Sharding để giải quyết vấn đề Ghi.
3. **Đúng.** Đây chính là nhược điểm cố hữu của Fixed Window Counter — user có thể gửi tối đa request vào cuối khung này + tối đa request vào đầu khung sau, tổng cộng gấp đôi giới hạn trong khoảng ngắn.
4. **Sai.** CDN chủ yếu dùng cho nội dung TĨNH (ảnh, CSS, JS, video) — ít thay đổi, phù hợp cache lâu dài; nội dung động thường không phù hợp với CDN truyền thống.
5. **Sai.** Ngược lại — Fan-out on WRITE gây ra "Celebrity Problem" nghiêm trọng cho user có follower cực lớn; nên dùng Fan-out on READ (hoặc Hybrid) cho trường hợp này.
6. **Đúng.** Đây là bước đầu tiên và quan trọng nhất trong quy trình tiếp cận System Design Interview.
7. **Đúng.** Đây chính là đánh đổi cốt lõi giữa 2 chiến lược Sharding.
8. **Đúng.** Đây là hệ quả trực tiếp của Replication Lag (đồng bộ bất đồng bộ giữa Master và Replica) khi đọc dữ liệu ngay sau khi ghi.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
public class Base62Codec {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    public static String encode(long id) {
        if (id == 0) return String.valueOf(BASE62.charAt(0));

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(BASE62.charAt((int) (id % BASE)));
            id /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String shortCode) {
        long id = 0;
        for (char c : shortCode.toCharArray()) {
            id = id * BASE + BASE62.indexOf(c);
        }
        return id;
    }
}

// Test: Base62Codec.encode(125) -> "cb" (ví dụ minh họa)
//       Base62Codec.decode("cb") -> 125 (giải mã ngược lại đúng ID gốc)
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
@Component
public class TokenBucketRateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int BUCKET_CAPACITY = 100;      // Tối đa 100 token trong bucket
    private static final double REFILL_RATE_PER_SECOND = 10.0; // Nạp 10 token/giây

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {
        String clientId = request.getRemoteAddr();
        String tokensKey = "bucket:tokens:" + clientId;
        String lastRefillKey = "bucket:lastRefill:" + clientId;

        long now = System.currentTimeMillis();
        String lastRefillStr = redisTemplate.opsForValue().get(lastRefillKey);
        String tokensStr = redisTemplate.opsForValue().get(tokensKey);

        double currentTokens = tokensStr != null ? Double.parseDouble(tokensStr) : BUCKET_CAPACITY;
        long lastRefill = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;

        // Tính số token được nạp thêm dựa trên thời gian đã trôi qua kể từ lần refill trước
        double elapsedSeconds = (now - lastRefill) / 1000.0;
        double refillTokens = elapsedSeconds * REFILL_RATE_PER_SECOND;
        currentTokens = Math.min(BUCKET_CAPACITY, currentTokens + refillTokens);

        if (currentTokens < 1) {
            response.setStatus(429);
            response.getWriter().write("{\"message\": \"Vượt quá giới hạn request\"}");
            return;
        }

        // Tiêu thụ 1 token cho request này
        currentTokens -= 1;
        redisTemplate.opsForValue().set(tokensKey, String.valueOf(currentTokens), Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now), Duration.ofMinutes(5));

        filterChain.doFilter(request, response);
    }
}
```

*(Lưu ý: Trong thực tế production, nên dùng Lua script chạy atomic trong Redis để tránh race condition khi nhiều request đồng thời đọc/ghi cùng key — đây là bản đơn giản hóa để minh họa nguyên lý Token Bucket.)*

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Index QUAN TRỌNG NHẤT cho Fan-out on Read:
-- Query "lấy bài viết mới nhất của user X" -> cần Index trên (user_id, created_at)
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);

CREATE TABLE follows (
    follower_id BIGINT NOT NULL,   -- Người đi follow
    followee_id BIGINT NOT NULL,   -- Người được follow
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, followee_id),
    FOREIGN KEY (follower_id) REFERENCES users(id),
    FOREIGN KEY (followee_id) REFERENCES users(id)
);
-- Index để query nhanh "user X đang follow những ai" (Fan-out on Read cần query này TRƯỚC)
CREATE INDEX idx_follows_follower ON follows(follower_id);

-- Query Fan-out on Read điển hình (lấy feed của user X):
-- SELECT p.* FROM posts p
-- JOIN follows f ON p.user_id = f.followee_id
-- WHERE f.follower_id = :userX
-- ORDER BY p.created_at DESC
-- LIMIT 20;
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@Service
public class PostService {

    private static final int CELEBRITY_THRESHOLD = 100_000;

    private final FollowRepository followRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public void createPost(Post post) {
        postRepository.save(post);

        long followerCount = followRepository.countFollowers(post.getUserId());

        if (followerCount > CELEBRITY_THRESHOLD) {
            // User CELEBRITY - KHÔNG fan-out ngay, để dành cho Fan-out on Read
            // (khi follower mở feed, hệ thống sẽ TỰ query real-time riêng cho các celebrity họ follow)
            log.info("User {} là celebrity ({} follower) - bỏ qua fan-out on write",
                    post.getUserId(), followerCount);
            return;
        }

        // User THƯỜNG - fan-out on WRITE bình thường
        fanOutToFollowers(post);
    }

    @Async
    public void fanOutToFollowers(Post post) {
        List<Long> followerIds = followRepository.findFollowerIds(post.getUserId());
        for (Long followerId : followerIds) {
            redisTemplate.opsForZSet().add(
                    "feed:" + followerId,
                    post.getId().toString(),
                    post.getCreatedAt().toEpochSecond(ZoneOffset.UTC));
        }
    }

    public List<Post> getFeed(Long userId) {
        // Bước 1: Lấy feed ĐÃ TÍNH SẴN (từ user thường) - nhanh
        Set<String> cachedPostIds = redisTemplate.opsForZSet()
                .reverseRange("feed:" + userId, 0, 19);

        // Bước 2: Query REAL-TIME riêng cho các Celebrity user này đang follow
        List<Long> celebrityIdsFollowed = followRepository.findCelebrityFolloweeIds(userId, CELEBRITY_THRESHOLD);
        List<Post> celebrityPosts = postRepository.findLatestPostsByUserIds(celebrityIdsFollowed, 20);

        // Bước 3: Gộp 2 nguồn, sắp xếp theo thời gian, trả về top N
        return mergeAndSortByTime(cachedPostIds, celebrityPosts, 20);
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

**Áp dụng quy trình 6 bước cho "Đếm lượt xem Video":**

**Bước 1 - Làm rõ yêu cầu:**
- Functional: Mỗi lần user xem video → tăng View Count; hiển thị View Count trên trang video
- Non-functional: Hàng triệu view/ngày cho 1 video hot; KHÔNG cần chính xác tuyệt đối real-time (khác với số dư tài khoản ngân hàng)

**Bước 2 - Ước lượng quy mô:**
- Giả sử: 1 video hot có thể nhận 10,000 view/giây tại thời điểm cao điểm
- Nếu MỖI view đều `UPDATE videos SET view_count = view_count + 1` trực tiếp vào DB → 10,000 UPDATE/giây vào CÙNG 1 dòng → **Row-level Lock Contention** cực nghiêm trọng (liên hệ Module 15 - Pessimistic/Optimistic Locking) — DB sẽ nghẽn ngay lập tức

**Bước 3-4 - Thiết kế:**
```
Client xem video -> gửi event "view" (KHÔNG update DB trực tiếp)
     │
     ▼
Message Queue (Kafka - liên hệ Module 18, phù hợp vì throughput cực cao)
     │
     ▼
Consumer gộp (aggregate) số lượng view theo BATCH (VD: mỗi 10 giây,
gộp tất cả view event của CÙNG 1 video thành 1 con số duy nhất)
     │
     ▼
Định kỳ UPDATE view_count += (số lượng đã gộp) vào DB
(1 UPDATE cho hàng nghìn view, thay vì hàng nghìn UPDATE riêng lẻ)
```

**Bước 5 - Deep Dive & trả lời câu hỏi Consistency:**

Câu trả lời: **KHÔNG cần Strong Consistency** cho View Count hiển thị — đây là ứng dụng điển hình chấp nhận **Eventual Consistency**:
- User sẽ KHÔNG để ý (và không quan trọng) nếu View Count hiển thị "10,234" thay vì con số chính xác tuyệt đối "10,241" tại đúng khoảnh khắc đó
- Đổi lại, việc **BATCH/gộp** số lượng view (thay vì update DB theo từng view riêng lẻ) giúp giảm tải DB **hàng nghìn lần**, tránh Lock Contention hoàn toàn

Có thể cache View Count trong Redis (Counter — liên hệ Module 18) để đọc cực nhanh, đồng bộ định kỳ xuống DB làm nguồn "sự thật" lâu dài (persist).

**Bước 6 - Trade-off:**
Đánh đổi: chấp nhận View Count hiển thị có độ trễ nhỏ (vài giây tới vài chục giây) để đổi lấy khả năng chịu tải cực cao — đây là lựa chọn ĐÚNG ĐẮN cho bài toán này, vì tính chính xác tuyệt đối theo thời gian thực **không mang lại giá trị nghiệp vụ tương xứng** với chi phí kỹ thuật phải trả (khác hẳn với bài toán như số dư tài khoản ngân hàng — nơi Strong Consistency là BẮT BUỘC).

</details>

---

*File tiếp theo trong lộ trình: **Module 23 — Bảo mật ứng dụng nâng cao (OWASP Top 10)** (SQL Injection, XSS, CSRF nâng cao, Broken Access Control, Security Misconfiguration, Insecure Deserialization, và checklist bảo mật cho Backend Developer).*
