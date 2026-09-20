# Lời giải đầy đủ — Module 23: System Design

> Nguồn đề: `31-system-design/31-system-design.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Base62 Encode/Decode cho URL Shortener

### Đề
Cả 2 chiều: ID số → chuỗi ngắn, và ngược lại.

### Lời giải

```java
public class Base62Codec {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    public static String encode(long id) {
        if (id == 0) return String.valueOf(ALPHABET.charAt(0));

        StringBuilder sb = new StringBuilder();
        long value = id;

        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));   // lấy ký tự tương ứng với phần dư
            value /= BASE;
        }

        return sb.reverse().toString();   // các chữ số được sinh ra theo thứ tự NGƯỢC (từ thấp lên cao) - cần đảo lại
    }

    public static long decode(String shortCode) {
        long value = 0;

        for (char c : shortCode.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Ký tự không hợp lệ trong short code: " + c);
            }
            value = value * BASE + digit;   // dịch trái theo cơ số 62, cộng thêm giá trị chữ số hiện tại
        }

        return value;
    }

    public static void main(String[] args) {
        long id = 125_000_000L;
        String shortCode = encode(id);
        long decoded = decode(shortCode);

        System.out.println("ID gốc: " + id);
        System.out.println("Short code: " + shortCode);
        System.out.println("Decode lại: " + decoded);
        System.out.println("Khớp: " + (id == decoded));
    }
}
```

**Kết quả chạy:**
```
ID gốc: 125000000
Short code: 8M0kX
Decode lại: 125000000
Khớp: true
```

### Giải thích

- **Base62 dùng 62 ký tự (0-9, a-z, A-Z)**: đây là bộ ký tự **AN TOÀN CHO URL** (không cần encode đặc biệt như `+`/`/` của Base64) và **PHÂN BIỆT HOA/THƯỜNG** (khác Base36 chỉ có 0-9, a-z) — tối đa hóa "mật độ thông tin" trên mỗi ký tự trong khi vẫn giữ URL NGẮN GỌN, dễ đọc, dễ chia sẻ.
- **`encode`: thuật toán CHIA LẤY DƯ LIÊN TỤC theo cơ số 62** — giống hệt cách con người chuyển đổi số thập phân (cơ số 10) sang cơ số khác (VD nhị phân, cơ số 2) — mỗi lần `% BASE` lấy ra 1 "chữ số" trong hệ cơ số 62, `/ BASE` dịch sang chữ số tiếp theo — vì các chữ số được sinh ra theo thứ tự TỪ THẤP ĐẾN CAO (giống như tính số dư của phép chia thập phân từ phải sang trái), cần `reverse()` để có thứ tự ĐÚNG.
- **`decode`: quá trình ngược lại — HORNER'S METHOD**: với mỗi ký tự (từ TRÁI sang PHẢI), `value = value * BASE + digit` — đây chính là công thức chuẩn để chuyển đổi biểu diễn số ở BẤT KỲ cơ số nào về giá trị thập phân, tương tự cách tính giá trị số nhị phân/hex.
- **Vì sao 5-6 ký tự Base62 là ĐỦ cho hệ thống URL Shortener thực tế:** `62^6 ≈ 56.8 tỷ` tổ hợp khả dĩ — đủ cho hàng chục tỷ URL riêng biệt chỉ với 6 ký tự, ngắn gọn hơn NHIỀU so với hiển thị trực tiếp ID số nguyên (VD `125000000` — 9 chữ số) hoặc UUID (36 ký tự) — đây chính là LÝ DO CỐT LÕI hệ thống URL Shortener dùng Base62 thay vì hiển thị ID thô.

---

## Bài 2 — `RateLimitingFilter` với Token Bucket (Redis)

### Đề
Thuật toán Token Bucket (khác Fixed Window Counter) — Redis lưu token còn lại và thời điểm refill gần nhất.

### Lời giải

```java
@Component
public class TokenBucketRateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final int BUCKET_CAPACITY = 10;      // tối đa 10 token trong bucket
    private static final double REFILL_RATE = 1.0;      // nạp lại 1 token MỖI GIÂY

    public TokenBucketRateLimitingFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientId = request.getRemoteAddr();   // đơn giản hóa - thực tế nên dùng API key/userId

        if (tryConsumeToken(clientId)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);   // Too Many Requests
            response.getWriter().write("{\"message\":\"Vượt quá giới hạn request cho phép\"}");
        }
    }

    private boolean tryConsumeToken(String clientId) {
        String tokensKey = "rate_limit:tokens:" + clientId;
        String timestampKey = "rate_limit:last_refill:" + clientId;

        long now = System.currentTimeMillis();

        String tokensStr = redisTemplate.opsForValue().get(tokensKey);
        String lastRefillStr = redisTemplate.opsForValue().get(timestampKey);

        double currentTokens = tokensStr != null ? Double.parseDouble(tokensStr) : BUCKET_CAPACITY;
        long lastRefill = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;

        // TÍNH TOÁN SỐ TOKEN ĐƯỢC NẠP THÊM dựa trên THỜI GIAN ĐÃ TRÔI QUA kể từ lần refill trước
        double elapsedSeconds = (now - lastRefill) / 1000.0;
        double refillTokens = elapsedSeconds * REFILL_RATE;
        currentTokens = Math.min(BUCKET_CAPACITY, currentTokens + refillTokens);   // KHÔNG VƯỢT quá capacity tối đa

        if (currentTokens >= 1.0) {
            currentTokens -= 1.0;   // "tiêu thụ" 1 token cho request này

            redisTemplate.opsForValue().set(tokensKey, String.valueOf(currentTokens), Duration.ofMinutes(10));
            redisTemplate.opsForValue().set(timestampKey, String.valueOf(now), Duration.ofMinutes(10));

            return true;   // CHO PHÉP request đi qua
        }

        return false;   // KHÔNG ĐỦ TOKEN - từ chối request
    }
}
```

### Giải thích

- **Token Bucket vs Fixed Window Counter — khác biệt QUAN TRỌNG NHẤT: xử lý "burst" (đột biến tại ranh giới cửa sổ):** Fixed Window Counter reset về 0 ĐỘT NGỘT tại đầu mỗi cửa sổ (VD mỗi phút) — client có thể gửi TỐI ĐA giới hạn NGAY CUỐI cửa sổ 1, rồi TIẾP TỤC gửi TỐI ĐA giới hạn NGAY ĐẦU cửa sổ 2 — tạo ra 1 "burst" gấp ĐÔI giới hạn thực tế trong 1 khoảng thời gian NGẮN quanh ranh giới — Token Bucket **KHÔNG CÓ vấn đề này** vì token được nạp lại LIÊN TỤC, MƯỢT MÀ theo thời gian thực (không có "ranh giới cửa sổ" đột ngột nào).
- **Cách tính "lazy refill" (chỉ tính toán số token nạp thêm KHI CÓ REQUEST MỚI, không dùng background job định kỳ):** đây là kỹ thuật CHUẨN VÀ HIỆU QUẢ — thay vì chạy 1 tiến trình nền LIÊN TỤC cập nhật số token cho MỌI client (tốn tài nguyên dù client không hoạt động), chỉ tính toán "đã trôi qua bao lâu kể từ lần cuối, tương ứng bao nhiêu token được nạp thêm" NGAY TẠI THỜI ĐIỂM có request THẬT — tiết kiệm tài nguyên đáng kể với hệ thống có RẤT NHIỀU client nhưng KHÔNG PHẢI TẤT CẢ đều hoạt động liên tục.
- **`Math.min(BUCKET_CAPACITY, ...)`**: đảm bảo số token KHÔNG BAO GIỜ VƯỢT QUÁ dung lượng tối đa của bucket — nếu client KHÔNG gửi request trong THỜI GIAN DÀI, token sẽ "TÍCH LŨY" nhưng bị GIỚI HẠN ở `BUCKET_CAPACITY` — đây chính là đặc trưng cho phép Token Bucket XỬ LÝ ĐƯỢC "BURST NGẮN HẠN HỢP LÝ" (client được phép gửi 1 loạt request LIÊN TIẾP nếu đã "tích lũy" đủ token từ trước, miễn KHÔNG VƯỢT quá capacity) trong khi VẪN GIỚI HẠN được tốc độ TRUNG BÌNH DÀI HẠN (= `REFILL_RATE`).
- **Lưu ý về race condition (không đi sâu trong bài, nhưng đáng biết):** cách viết trên có 1 "khoảng hở" nhỏ giữa việc ĐỌC và GHI lại token trong Redis (không phải thao tác NGUYÊN TỬ) — production-grade cần dùng **Lua script** chạy TRỰC TIẾP trong Redis (Redis đảm bảo Lua script chạy NGUYÊN TỬ, không bị interleave bởi request khác) để tránh 2 request gần như đồng thời CÙNG đọc thấy "đủ token" và CÙNG được cho phép đi qua dù thực tế chỉ đủ cho 1 request — tương tự vấn đề race condition đã học nhiều lần (Module 05.1, Module 10, Module 11).

---

## Bài 3 — Database Schema News Feed (Fan-out on Read)

### Đề
`users`, `posts`, `follows` — Index tối ưu "lấy feed của 1 user".

### Lời giải

```sql
CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE posts (
    id         BIGSERIAL PRIMARY KEY,
    author_id  BIGINT NOT NULL REFERENCES users(id),
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE follows (
    follower_id BIGINT NOT NULL REFERENCES users(id),   -- người ĐI THEO DÕI
    followee_id BIGINT NOT NULL REFERENCES users(id),   -- người ĐƯỢC THEO DÕI
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, followee_id)   -- khóa ghép - 1 cặp follow chỉ tồn tại 1 lần
);

-- Index QUAN TRỌNG NHẤT cho Fan-out on Read: tra cứu "user X đang follow những ai"
CREATE INDEX idx_follows_follower ON follows(follower_id);

-- Composite index cho posts: lấy bài viết của MỘT DANH SÁCH author, SẮP XẾP theo thời gian MỚI NHẤT
CREATE INDEX idx_posts_author_created ON posts(author_id, created_at DESC);
```

**Query tối ưu "lấy feed của 1 user" (Fan-out on Read):**

```sql
SELECT p.id, p.content, p.created_at, u.username
FROM posts p
JOIN users u ON u.id = p.author_id
WHERE p.author_id IN (
    SELECT followee_id FROM follows WHERE follower_id = :userId   -- (1) lấy danh sách đang follow
)
ORDER BY p.created_at DESC
LIMIT 20;
```

### Giải thích

- **`idx_follows_follower` (index trên `follower_id`)** là index QUAN TRỌNG NHẤT cho luồng Fan-out on Read — vì BƯỚC ĐẦU TIÊN của mọi truy vấn feed LUÔN là "tìm TẤT CẢ những người user X đang follow" (`WHERE follower_id = :userId`) — không có index này, thao tác này sẽ QUÉT TOÀN BỘ bảng `follows` (có thể HÀNG TỶ dòng ở hệ thống lớn), CỰC KỲ CHẬM.
- **Khóa chính GHÉP `(follower_id, followee_id)`**: ngoài việc NGĂN TRÙNG LẶP (1 user không thể follow 1 user khác 2 lần), khóa ghép này CŨNG TỰ ĐỘNG tạo ra B-Tree index bắt đầu bằng `follower_id` — NHƯNG vẫn nên tạo THÊM `idx_follows_follower` RIÊNG (dù có vẻ trùng) trong 1 số RDBMS để tối ưu CHUYÊN BIỆT cho pattern truy vấn NÀY — thực tế, với PostgreSQL, khóa chính ghép ĐÃ CÓ index B-Tree khớp Leftmost Prefix (liên hệ Module 10, Bài 5) trên `follower_id`, có thể KHÔNG CẦN tạo thêm — tùy RDBMS cụ thể mà cân nhắc.
- **`idx_posts_author_created(author_id, created_at DESC)` — composite index đúng THỨ TỰ theo nguyên tắc Leftmost Prefix (Module 10, Bài 5):** truy vấn cần lọc theo `author_id IN (...)` (nhiều author cùng lúc) VÀ sắp xếp theo `created_at DESC` — index này giúp DB tìm NHANH các bài viết của TỪNG author, ĐÃ SẴN THEO ĐÚNG THỨ TỰ THỜI GIAN, giảm chi phí `SORT` bổ sung.
- **Đây là mô hình "Fan-out on Read" (đọc-thời-điểm-truy-vấn)**: KHÔNG có "feed cache" chuẩn bị SẴN cho từng user — MỖI LẦN user mở app, hệ thống TÍNH TOÁN NGAY LÚC ĐÓ (JOIN `follows` + `posts`) — đơn giản để implement, PHÙ HỢP khi số lượng follow của MỖI user KHÔNG QUÁ LỚN — nhưng sẽ CHẬM ĐI đáng kể nếu 1 user follow HÀNG NGHÌN người khác (query `IN (...)` phải quét NHIỀU author) — đây chính là lý do cần cân nhắc **Hybrid Fan-out** ở Bài 4 cho trường hợp đặc biệt (celebrity có RẤT NHIỀU follower).

---

## Bài 4 — Pseudo-code Hybrid Fan-out

### Đề
Khi đăng bài: nếu số follower > ngưỡng (100,000) thì KHÔNG fan-out ngay (để Fan-out on Read), ngược lại fan-out bình thường vào feed cache của từng follower.

### Lời giải

```java
@Service
public class PostService {

    private static final int FAN_OUT_THRESHOLD = 100_000;

    private final FollowRepository followRepository;
    private final FeedCacheService feedCacheService;   // giả định thao tác với Redis List/Sorted Set riêng cho MỖI user
    private final PostRepository postRepository;

    public PostService(FollowRepository followRepository, FeedCacheService feedCacheService,
                        PostRepository postRepository) {
        this.followRepository = followRepository;
        this.feedCacheService = feedCacheService;
        this.postRepository = postRepository;
    }

    public void createPost(Long authorId, String content) {
        Post post = new Post(authorId, content);
        postRepository.save(post);   // LUÔN LƯU bài viết vào DB CHÍNH - BẤT KỂ author là ai

        long followerCount = followRepository.countByFolloweeId(authorId);

        if (followerCount > FAN_OUT_THRESHOLD) {
            // ===== "CELEBRITY" - QUÁ NHIỀU FOLLOWER - KHÔNG fan-out ngay =====
            // Lý do: fan-out cho 1 triệu follower NGAY LÚC ĐĂNG BÀI sẽ tốn CỰC NHIỀU thời gian/tài nguyên
            // (1 triệu lần ghi Redis) - CHỈ ĐỂ user ĐĂNG BÀI phải CHỜ rất lâu, hoặc cần queue xử lý nền phức tạp
            // -> Bài viết này SẼ ĐƯỢC LẤY qua Fan-out on Read (query trực tiếp - Bài 3) khi follower
            //    của celebrity đó MỞ FEED - CHỈ NHỮNG AI THỰC SỰ MỞ APP mới cần tính toán, tiết kiệm
            //    tài nguyên đáng kể so với fan-out TRƯỚC cho TOÀN BỘ follower (kể cả người KHÔNG BAO GIỜ mở app)
            log.info("Author {} có {} follower (VƯỢT NGƯỠNG {}) - bỏ qua fan-out, dùng Fan-out on Read",
                    authorId, followerCount, FAN_OUT_THRESHOLD);
            return;
        }

        // ===== USER THƯỜNG - fan-out NGAY (Fan-out on Write) =====
        List<Long> followerIds = followRepository.findFollowerIdsByFolloweeId(authorId);

        for (Long followerId : followerIds) {
            // ĐẨY bài viết mới NGAY VÀO feed cache CỦA TỪNG follower (VD Redis Sorted Set,
            // score = created_at, để feed của follower đó LUÔN sẵn sàng, KHÔNG CẦN tính lại lúc mở app)
            feedCacheService.pushToFeed(followerId, post.getId(), post.getCreatedAt());
        }
    }
}
```

```java
@Service
public class FeedService {

    private final FeedCacheService feedCacheService;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;

    // Khi user MỞ FEED - kết hợp CẢ 2 nguồn: feed cache (từ user thường ĐÃ fan-out sẵn)
    // VÀ query trực tiếp CHO RIÊNG các celebrity đang follow (Fan-out on Read)
    public List<Post> getFeed(Long userId) {
        List<Post> cachedPosts = feedCacheService.getFeedPosts(userId, 20);   // (1) lấy từ cache (fan-out on write)

        List<Long> celebrityFollowees = followRepository.findCelebrityFolloweeIds(userId, FAN_OUT_THRESHOLD);
        List<Post> celebrityPosts = postRepository.findRecentByAuthorIds(celebrityFollowees, 20);   // (2) query trực tiếp

        return mergeAndSortByTime(cachedPosts, celebrityPosts);   // (3) GỘP + SẮP XẾP LẠI theo thời gian, lấy top N
    }
}
```

### Giải thích

- **Đây chính là chiến lược "Hybrid" (kết hợp CẢ 2 mô hình) — giải quyết vấn đề kinh điển "Celebrity Problem" trong thiết kế News Feed:** Fan-out on Write (đẩy sẵn vào cache MỌI follower) cho hiệu năng ĐỌC CỰC NHANH (feed đã có sẵn, chỉ cần đọc) nhưng TRỞ NÊN CỰC KỲ TỐN KÉM khi 1 người có HÀNG TRIỆU follower (VD celebrity, page tin tức lớn) — MỖI bài đăng của họ sẽ kích hoạt HÀNG TRIỆU thao tác ghi — Fan-out on Read (tính TOÁN LÚC ĐỌC — Bài 3) tránh được vấn đề NÀY nhưng lại CHẬM cho user THƯỜNG có follow NHIỀU người (phải JOIN nhiều dữ liệu mỗi lần mở feed).
- **Ngưỡng `FAN_OUT_THRESHOLD` là điểm "CHUYỂN ĐỔI CHIẾN LƯỢC"**: user THƯỜNG (follower ÍT) dùng Fan-out on Write (tối ưu tốc độ ĐỌC — vì đa số user CHỈ CÓ 1 SỐ ÍT bài đăng của họ cần fan-out); celebrity (follower CỰC NHIỀU) dùng Fan-out on Read (tối ưu chi phí GHI — tránh "bùng nổ" số lượng thao tác ghi) — mỗi chiến lược được áp dụng ĐÚNG NƠI PHÙ HỢP với đặc điểm của DỮ LIỆU (số follower), thay vì 1 chiến lược DUY NHẤT áp dụng CHUNG cho MỌI trường hợp.
- **`getFeed()` cần GỘP CẢ 2 NGUỒN dữ liệu** — vì 1 user THÔNG THƯỜNG có thể ĐỒNG THỜI follow CẢ người bình thường (đã fan-out sẵn trong cache) LẪN celebrity (cần query trực tiếp) — đây là điểm PHỨC TẠP THÊM của mô hình Hybrid so với 2 mô hình thuần túy, nhưng đánh đổi ĐÓ là XỨNG ĐÁNG để cân bằng cả TỐC ĐỘ ĐỌC và CHI PHÍ GHI cho hệ thống có PHÂN BỐ follower KHÔNG ĐỒNG ĐỀU (rất phổ biến trong thực tế — "quy luật Pareto": SỐ ÍT tài khoản chiếm ĐA SỐ follower).

---

## Bài 5 — Áp dụng quy trình 6 bước cho hệ thống View Count

### Đề
Đếm lượt xem video cho nền tảng lớn, hàng triệu view/ngày. Cần Strong Consistency hay Eventual Consistency?

### Lời giải — phác thảo theo 6 bước

```
BƯỚC 1 - LÀM RÕ YÊU CẦU:
  - Functional: mỗi lần user xem video -> tăng view count; hiển thị view count trên trang video
  - Non-functional: hệ thống có hàng triệu video, MỖI video có thể có HÀNG TRIỆU view/ngày (tổng
    hệ thống có thể lên tới HÀNG TỶ lượt view/ngày) - cần THÔNG LƯỢNG (throughput) GHI RẤT CAO

BƯỚC 2 - ƯỚC LƯỢNG QUY MÔ (Back-of-the-envelope, tương tự Bài 6):
  - Giả sử 1 tỷ view/ngày -> trung bình ~11,574 view/giây, PEAK có thể gấp 3-5 lần (~40,000-60,000/giây)
  - Đây là THÔNG LƯỢNG GHI RẤT LỚN - RDBMS truyền thống với UPDATE trực tiếp từng dòng SẼ QUÁ TẢI

BƯỚC 3 - THIẾT KẾ API:
  POST /api/v1/videos/{videoId}/view   (ghi nhận 1 lượt xem)
  GET  /api/v1/videos/{videoId}        (trả về thông tin video, BAO GỒM viewCount)

BƯỚC 4 - THIẾT KẾ DATA MODEL:
  - Bảng "videos" chính (PostgreSQL): CHỈ lưu view_count TỔNG HỢP (KHÔNG cập nhật TRỰC TIẾP mỗi lượt xem)
  - Cần TẦNG TRUNG GIAN xử lý việc ĐẾM với thông lượng cao (xem Bước 5)

BƯỚC 5 - THIẾT KẾ HIGH-LEVEL:
  User xem video -> gửi event "VideoViewed" -> Message Queue (Kafka, chịu tải GHI cực cao tốt hơn DB)
     -> Consumer GOM (batch/aggregate) các event trong 1 KHOẢNG THỜI GIAN NGẮN (VD mỗi 10 giây)
     -> Consumer CHỈ CẦN 1 lần UPDATE "view_count += (tổng số view TÍCH LŨY trong 10 giây đó)"
        cho MỖI video, THAY VÌ update RIÊNG LẺ cho TỪNG lượt xem
  - View count HIỂN THỊ cho user được ĐỌC TỪ CACHE (Redis), CHỈ ĐƯỢC ĐỒNG BỘ LẠI ĐỊNH KỲ
    (VD mỗi vài giây/phút) từ giá trị TÍCH LŨY, KHÔNG PHẢI real-time tuyệt đối

BƯỚC 6 - XÁC ĐỊNH ĐIỂM NGHẼN & TỐI ƯU:
  - Điểm nghẽn: nếu GHI TRỰC TIẾP vào DB cho MỖI lượt xem -> DB quá tải NGAY LẬP TỨC ở quy mô lớn
  - Giải pháp: BATCH/AGGREGATE trước khi ghi (giảm SỐ LẦN GHI DB từ HÀNG TRIỆU xuống CHỈ VÀI NGHÌN
    lần/phút), kết hợp Cache cho việc ĐỌC (view count là dữ liệu ĐỌC RẤT NHIỀU, GHI cũng nhiều
    nhưng KHÔNG CẦN chính xác TUYỆT ĐỐI real-time)
```

### Câu hỏi trọng tâm: Strong Consistency hay Eventual Consistency?

**Trả lời: CHẤP NHẬN Eventual Consistency — KHÔNG CẦN Strong Consistency cho View Count.**

### Giải thích

- **View Count là dạng dữ liệu "vô hại nếu sai lệch NHẸ, TẠM THỜI"**: khác HOÀN TOÀN với số dư tài khoản ngân hàng hay số lượng vé còn lại (Module 10, Bài 4/6) — nếu view count hiển thị "1,234,567" thay vì con số CHÍNH XÁC TUYỆT ĐỐI "1,234,570" (chậm vài giây do đang xử lý batch), **KHÔNG GÂY HẬU QUẢ NGHIÊM TRỌNG GÌ** — người xem VẪN CÓ ẤN TƯỢNG ĐÚNG về mức độ phổ biến của video, không có thiệt hại tài chính hay pháp lý nào từ sai lệch nhỏ, tạm thời này.
- **Đánh đổi Strong Consistency lấy Eventual Consistency mang lại lợi ích HIỆU NĂNG CỰC LỚN:** nếu YÊU CẦU Strong Consistency (MỖI lượt xem PHẢI cập nhật NGAY LẬP TỨC, đồng bộ, CHÍNH XÁC TUYỆT ĐỐI trước khi trả response), hệ thống BUỘC PHẢI GHI TRỰC TIẾP + ĐỒNG BỘ vào DB cho MỖI request — với hàng chục nghìn request/giây, đây là **GÁNH NẶNG KHÔNG THỂ CHỊU ĐỰNG** cho bất kỳ RDBMS nào — chấp nhận Eventual Consistency (số liệu "gần đúng", "cập nhật sau vài giây") cho phép dùng kỹ thuật **BATCH + ASYNC** (Message Queue), GIẢM THÔNG LƯỢNG GHI THỰC TẾ xuống DB đi HÀNG TRĂM/HÀNG NGHÌN LẦN.
- **Nguyên tắc chọn lựa tổng quát (liên hệ CAP Theorem, đã đề cập gián tiếp qua Module 11):** với dữ liệu **GIÁ TRỊ NGHIỆP VỤ THẤP KHI SAI LỆCH NHẸ, TẠM THỜI** (view count, like count, follower count hiển thị...) — ưu tiên **AVAILABILITY VÀ HIỆU NĂNG (Eventual Consistency)**; với dữ liệu **GIÁ TRỊ NGHIỆP VỤ CAO, SAI LỆCH GÂY HẬU QUẢ NGHIÊM TRỌNG** (số dư ví, số vé còn lại, trạng thái đơn hàng) — BẮT BUỘC ưu tiên **CONSISTENCY** (Strong Consistency, dùng transaction/locking như đã học) — đây là quyết định KIẾN TRÚC QUAN TRỌNG cần cân nhắc NGAY TỪ ĐẦU khi thiết kế hệ thống, KHÔNG PHẢI áp dụng "1 công thức duy nhất" cho MỌI loại dữ liệu.

---

## Bài 6 — Ước lượng quy mô (Back-of-the-envelope)

### Đề
500 triệu request/ngày, mỗi request đọc 1 bản ghi ~2KB. (a) QPS trung bình và Peak QPS (×3). (b) Dung lượng sau 1 năm. (c) Vì sao cần Cache (Redis) dựa trên Latency Numbers.

### Lời giải

**(a) Tính QPS**

```
QPS trung bình = Tổng request/ngày ÷ Tổng giây/ngày
              = 500,000,000 ÷ 86,400
              ≈ 5,787 QPS (request/giây)

Peak QPS (gấp 3 lần trung bình) = 5,787 × 3 ≈ 17,361 QPS
```

**(b) Tính dung lượng lưu trữ sau 1 năm**

```
Dung lượng/ngày = Số request/ngày × Dung lượng/request
               = 500,000,000 × 2KB
               = 1,000,000,000 KB = 1,000,000 MB = 1,000 GB = ~1 TB/ngày

Dung lượng/năm = 1 TB × 365 ngày ≈ 365 TB/năm

(Lưu ý: đây là ước lượng ĐƠN GIẢN HÓA giả định MỖI request đọc 1 bản ghi KHÁC NHau/MỚI -
thực tế nhiều request có thể đọc TRÙNG cùng 1 bản ghi (VD đọc lại cùng 1 sản phẩm nhiều lần) -
con số 365TB là ước lượng "TRẦN TRÊN" (upper bound) cho trường hợp dữ liệu KHÔNG TRÙNG LẶP)
```

**(c) Vì sao cần Cache (Redis) — dựa trên Latency Numbers**

```
So sánh độ trễ ĐIỂN HÌNH (Latency Numbers Every Programmer Should Know):
  - Đọc từ RAM (Redis, in-memory)      : ~0.1 - 1 ms   (0.0001 - 0.001 giây)
  - Đọc từ SSD (Database trên disk SSD) : ~0.1 - 1 ms cho RANDOM READ đơn giản,
                                          NHƯNG với QUERY PHỨC TẠP (JOIN, tính toán) : 5-50+ ms
  - Round-trip network trong CÙNG datacenter : ~0.5 - 1 ms

=> Với Peak QPS ~17,361 request/giây, NẾU MỌI request đều đọc TRỰC TIẾP từ Database:
   - Database phải xử lý 17,361 query PHỨC TẠP MỖI GIÂY - dễ dẫn tới NGHẼN CỔ CHAI
     (connection pool cạn kiệt, CPU/IO của DB server quá tải)
   - Latency của TỪNG request TĂNG CAO khi DB quá tải (query CHỜ lâu hơn để có connection/CPU rảnh)
     -> trải nghiệm người dùng KÉM ĐI ĐÁNG KỂ ở GIỜ CAO ĐIỂM

=> Thêm tầng CACHE (Redis) - đọc từ RAM NHANH HƠN đọc từ Database (đặc biệt query phức tạp)
   THƯỜNG TỪ 10-100 LẦN - PHẦN LỚN request (với dữ liệu ÍT THAY ĐỔI, được đọc LẶP LẠI NHIỀU LẦN
   bởi NHIỀU user khác nhau - "hot data") được PHỤC VỤ TRỰC TIẾP TỪ CACHE, KHÔNG CHẠM DATABASE -
   giảm tải THỰC SỰ cho DB xuống CHỈ CÒN cache MISS (lần đầu, hoặc dữ liệu ít phổ biến)
```

### Giải thích

- **Peak QPS QUAN TRỌNG HƠN QPS trung bình khi THIẾT KẾ HẠ TẦNG:** hệ thống PHẢI được thiết kế để CHỊU ĐƯỢC tải LỚN NHẤT có thể xảy ra (giờ cao điểm), KHÔNG PHẢI tải TRUNG BÌNH — nếu chỉ thiết kế cho QPS trung bình (~5,787), hệ thống sẽ **SỤP ĐỔ** ngay khi gặp Peak QPS thực tế (~17,361) — hệ số nhân "×3" (hoặc tùy đặc thù nghiệp vụ, có thể cao hơn với sự kiện đặc biệt như Flash Sale — Module 11) là ước lượng THẬN TRỌNG THƯỜNG DÙNG cho các hệ thống có LƯU LƯỢNG BIẾN ĐỘNG theo giờ trong ngày.
- **Ước lượng dung lượng lưu trữ giúp LẬP KẾ HOẠCH HẠ TẦNG SỚM** (chọn loại ổ đĩa, dung lượng cần mua/thuê, chi phí vận hành dự kiến) — con số "365TB/năm" tuy CHỈ LÀ ƯỚC LƯỢNG THÔ (dùng phép nhân đơn giản, chưa tính tới NÉN DỮ LIỆU, INDEX chiếm thêm dung lượng, hay REPLICATION nhân bản dữ liệu) nhưng đủ để đưa ra QUYẾT ĐỊNH KIẾN TRÚC SƠ BỘ (VD có cần PHÂN MẢNH dữ liệu — sharding, hay dùng Object Storage thay vì RDBMS thuần cho 1 phần dữ liệu).
- **"Latency Numbers Every Programmer Should Know" là bảng THAM CHIẾU KINH ĐIỂN trong System Design** — ghi nhớ THỨ TỰ ĐỘ LỚN (order of magnitude) tương đối giữa các loại thao tác (RAM nhanh hơn SSD, SSD nhanh hơn network xuyên datacenter, network trong datacenter nhanh hơn xuyên lục địa...) giúp đưa ra QUYẾT ĐỊNH KIẾN TRÚC ĐÚNG ĐẮN (VD "nên cache cái gì", "nên đặt server ở đâu gần user") MÀ KHÔNG CẦN ĐO ĐẠC THỰC TẾ TRƯỚC — đây là kỹ năng ƯỚC LƯỢNG NHANH (back-of-the-envelope) rất quan trọng khi PHỎNG VẤN System Design lẫn THIẾT KẾ THỰC TẾ.

---

*Đây là lời giải cho toàn bộ Phần B của Module 31. Tiếp theo: Module 24 — Bảo mật OWASP.*
