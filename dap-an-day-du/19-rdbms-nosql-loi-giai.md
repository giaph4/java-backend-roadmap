# Lời giải đầy đủ — Module 11: RDBMS & NoSQL

> Nguồn đề: `19 rdbms nosql/19-rdbms-nosql.md` (Phần B — Bài tập thực hành). Chỉ làm Phần B.

---

## Bài 1 — Thiết kế schema PostgreSQL tận dụng JSONB

### Đề
Bảng `products` cho e-commerce đa dạng loại sản phẩm — cột cố định cho thông tin CHUNG, `JSONB` cho thuộc tính RIÊNG. 2-3 `INSERT` mẫu, 1 `SELECT` truy vấn theo thuộc tính trong JSONB.

### Phân tích

Đây là mô hình **lai (hybrid)** — tận dụng ưu điểm của CẢ 2 thế giới: cột quan hệ chuẩn (có kiểu dữ liệu, index B-Tree, ràng buộc toàn vẹn) cho dữ liệu **CHUNG, ổn định về cấu trúc**; `JSONB` (linh hoạt schema-less) cho dữ liệu **THAY ĐỔI theo từng loại sản phẩm** — tránh phải tạo hàng chục cột `NULL` (VD: điện thoại có `ram_gb`, sách có `so_trang`, quần áo có `size` — nếu dùng cột cứng cho tất cả, mỗi dòng sẽ có rất nhiều cột NULL không liên quan).

### Lời giải

```sql
CREATE TABLE products (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    price      NUMERIC(12,2) NOT NULL,
    category   VARCHAR(50) NOT NULL,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb   -- thuộc tính RIÊNG từng loại sản phẩm
);

-- Index GIN cho phép truy vấn HIỆU QUẢ bên trong JSONB (không phải quét tuần tự toàn bộ JSON)
CREATE INDEX idx_products_attributes ON products USING GIN (attributes);

-- INSERT mẫu cho các loại sản phẩm khác nhau
INSERT INTO products (name, price, category, attributes) VALUES
('iPhone 15 Pro', 25000000, 'dien-thoai', '{"ram_gb": 8, "storage_gb": 256, "color": "titan"}'),
('Sách "Clean Code"', 250000, 'sach', '{"so_trang": 464, "tac_gia": "Robert C. Martin", "nxb": "Prentice Hall"}'),
('Áo thun basic', 150000, 'quan-ao', '{"size": ["S", "M", "L", "XL"], "chat_lieu": "cotton"}');

-- SELECT: tìm điện thoại có RAM >= 8GB
SELECT name, price, attributes->>'ram_gb' AS ram
FROM products
WHERE category = 'dien-thoai'
  AND (attributes->>'ram_gb')::int >= 8;
```

### Giải thích

- **`attributes->>'ram_gb'`** dùng toán tử `->>` (trả về giá trị dạng **TEXT**), khác với `->` (trả về dạng **jsonb**) — vì cần so sánh số (`>= 8`), phải ép kiểu `::int` sau khi lấy ra dạng text.
- **`WHERE category = 'dien-thoai' AND ...`** đặt điều kiện lọc `category` (cột thường, có index B-Tree riêng nếu cần) TRƯỚC — giúp thu hẹp tập dữ liệu cần "đào sâu" vào JSONB, tránh phải parse JSON trên toàn bộ 1 triệu dòng không liên quan.
- **Index `GIN`** (Generalized Inverted Index) là loại index chuyên dụng cho JSONB/mảng — hỗ trợ hiệu quả các toán tử `@>`, `?`, `?&`... (kiểm tra "chứa key/value"); với so sánh SỐ như `>=` trong ví dụ trên, GIN index hỗ trợ giới hạn hơn — trong thực tế, nếu 1 thuộc tính (VD `ram_gb`) được truy vấn RẤT THƯỜNG XUYÊN và cần so sánh khoảng giá trị, nên cân nhắc tạo **expression index** riêng: `CREATE INDEX ON products (((attributes->>'ram_gb')::int));`.
- **Đánh đổi cố hữu của thiết kế này:** JSONB **KHÔNG có ràng buộc kiểu dữ liệu/NOT NULL ở tầng DB** cho từng thuộc tính con — ứng dụng (tầng Java/Spring) phải tự đảm bảo tính hợp lệ trước khi ghi, DB không giúp "bắt lỗi" như với cột quan hệ chuẩn.

---

## Bài 2 — Thiết kế Document MongoDB cho hệ thống Blog

### Đề
So sánh với thiết kế SQL chuẩn hóa (Module 10, Bài 3): thiết kế lại theo MongoDB — document mẫu cho `posts`, nhúng (embed) tác giả và tag. Phân tích ưu/nhược.

### Lời giải

```javascript
// Document mẫu trong collection "posts"
{
  "_id": ObjectId("65f1a2b3c4d5e6f7a8b9c0d1"),
  "title": "Học Spring Boot từ A-Z",
  "content": "...",
  "created_at": ISODate("2026-01-15T10:00:00Z"),

  // NHÚNG (embed) trực tiếp thông tin tác giả - không cần JOIN/bảng users riêng
  "author": {
    "id": "u123",
    "username": "giapho",
    "email": "gia@example.com"
  },

  // NHÚNG mảng tag trực tiếp - không cần bảng trung gian post_tags
  "tags": ["java", "spring-boot", "backend"]
}
```

### Giải thích — Ưu điểm / Nhược điểm

**Ưu điểm (tốc độ đọc):**
- Chỉ cần **1 lần truy vấn duy nhất** (`db.posts.findOne({_id: ...})`) để lấy TOÀN BỘ thông tin hiển thị 1 bài viết (tiêu đề, nội dung, tên tác giả, danh sách tag) — không cần `JOIN` qua 3-4 bảng như thiết kế SQL chuẩn hóa (`posts` ⋈ `users` ⋈ `post_tags` ⋈ `tags`).
- Phù hợp với **pattern đọc chiếm ưu thế** (read-heavy) điển hình của hệ thống blog — 1 bài viết được ĐỌC hàng nghìn lần nhưng chỉ được TẠO/SỬA 1 lần.

**Nhược điểm (dữ liệu lặp lại — denormalization):**
- Nếu tác giả **đổi `username`**, thông tin `author.username` đã nhúng vào **TOÀN BỘ document `posts` cũ** của tác giả đó **KHÔNG TỰ ĐỘNG CẬP NHẬT** — dẫn đến **BẤT NHẤT DỮ LIỆU (data inconsistency)**: các bài viết cũ hiển thị tên cũ, trong khi thiết kế SQL chuẩn hóa chỉ cần `UPDATE users SET username = ... WHERE id = ...` **1 LẦN DUY NHẤT**, mọi `JOIN` sau đó tự động phản ánh tên mới.
- Cần chiến lược khắc phục thực tế: hoặc chấp nhận độ trễ đồng bộ (chạy batch job cập nhật lại các document cũ khi tác giả đổi tên — tốn kém nếu tác giả có hàng nghìn bài viết), hoặc **chỉ nhúng `author.id`** (tham chiếu, không nhúng toàn bộ thông tin) rồi query riêng — nhưng làm vậy sẽ MẤT đi lợi thế "1 lần truy vấn" ban đầu, quay lại gần giống mô hình JOIN.
- **Nguyên tắc chung khi thiết kế MongoDB:** nhúng (embed) khi dữ liệu **ít thay đổi VÀ luôn được đọc CÙNG NHAU**; dùng tham chiếu (reference, giống khóa ngoại) khi dữ liệu **thay đổi thường xuyên** hoặc **được nhiều entity khác dùng chung** (VD nếu 1 user có hàng nghìn bài viết, nhúng full profile vào từng bài là lãng phí không gian lưu trữ đáng kể).

---

## Bài 3 — Thiết kế chiến lược Cache bằng Redis

### Đề
`GET /api/products/{id}` gọi rất nhiều, dữ liệu ít đổi. Mô tả: (a) key kiểm tra; (b) cache hit; (c) cache miss + TTL; (d) invalidation khi `PUT`; (e) `allkeys-lru` khi RAM gần đầy.

### Lời giải

```
(a) Khi có request GET /api/products/{id}:
    key = "product:" + id     // ví dụ: "product:42"
    value = redis.GET(key)

(b) NẾU value != null (CACHE HIT):
    → Deserialize value (JSON string -> Object)
    → Trả về NGAY cho client, KHÔNG chạm tới PostgreSQL

(c) NẾU value == null (CACHE MISS):
    → product = database.query("SELECT * FROM products WHERE id = ?", id)
    → NẾU product tồn tại:
        redis.SETEX(key, TTL_SECONDS, serialize(product))   // set kèm thời gian hết hạn
    → Trả về product cho client

    TTL đề xuất: 10-30 phút (600-1800 giây)
    Lý do: dữ liệu sản phẩm "ít khi thay đổi" nhưng KHÔNG PHẢI KHÔNG BAO GIỜ đổi
    (giá có thể được admin cập nhật) - TTL vừa đủ dài để giảm tải DB đáng kể,
    vừa đủ ngắn để dữ liệu cũ (nếu invalidation ở bước (d) lỡ thất bại) tự "tự phục hồi"
    sau tối đa TTL giây, không bị "cache SAI vĩnh viễn"

(d) Khi PUT /api/products/{id} (cập nhật sản phẩm) - CACHE INVALIDATION:
    → database.update(...)   // cập nhật DB TRƯỚC (nguồn sự thật - source of truth)
    → redis.DEL("product:" + id)   // XÓA key cache cũ NGAY sau khi update DB thành công
    → Lần GET tiếp theo sẽ là cache miss -> tự động đọc lại DB -> ghi lại cache với dữ liệu MỚI

    (Chiến lược "Cache-Aside" / "Lazy Loading" kết hợp "Write-through Invalidation")

(e) Nếu dùng allkeys-lru và RAM gần đầy:
    → Redis TỰ ĐỘNG loại bỏ (evict) các key ÍT ĐƯỢC TRUY CẬP GẦN ĐÂY NHẤT
      (Least Recently Used) để giải phóng RAM cho key MỚI
    → Các sản phẩm ít được xem (VD sản phẩm cũ, ế) sẽ bị đẩy khỏi cache TRƯỚC,
      lần GET tiếp theo cho các sản phẩm đó sẽ là cache miss (tự nhiên, chấp nhận được)
    → Sản phẩm HOT (đang được xem nhiều) vẫn được giữ lại trong RAM
```

### Giải thích

- **Vì sao `DEL` (xóa) chứ không `SET` lại giá trị mới ngay trong bước (d):** xóa đơn giản và AN TOÀN hơn — tránh race condition nếu 2 request `PUT` gần như đồng thời ghi đè cache bằng dữ liệu SAI thứ tự (request cũ ghi SAU request mới, để lại cache SAI). Xóa key và để lần `GET` tiếp theo tự "lazy load" lại từ DB (nguồn sự thật) là chiến lược đơn giản, ít lỗi hơn.
- **Nguyên tắc "DB trước, cache sau" ở bước (d)**: nếu làm ngược lại (xóa cache trước, update DB sau), có 1 khoảng hở nhỏ mà 1 request `GET` khác chen vào GIỮA 2 bước đó sẽ đọc DB (chưa update) rồi VÔ TÌNH ghi lại cache với dữ liệu CŨ — thứ tự "update DB trước, xóa cache sau" giảm thiểu (dù không loại bỏ hoàn toàn) rủi ro này.
- **`allkeys-lru`** là 1 trong nhiều **eviction policy** của Redis — phù hợp cho use-case cache (không phải nơi lưu trữ dữ liệu chính), vì việc "mất" 1 cache entry không gây mất dữ liệu (luôn đọc lại được từ DB) — khác với policy `noeviction` (từ chối ghi mới khi đầy RAM, phù hợp khi Redis được dùng như DB chính, không phải cache).

---

## Bài 4 — Redis Sorted Set cho bảng xếp hạng

### Đề
Lệnh Redis xây leaderboard: cập nhật điểm, lấy TOP 10, lấy rank của 1 người chơi (`ZREVRANK`).

### Lời giải

```bash
# Mỗi khi người chơi ghi điểm - ZADD (nếu member đã tồn tại, CẬP NHẬT điểm; nếu chưa, THÊM MỚI)
ZADD leaderboard 1500 "player:alice"
ZADD leaderboard 2300 "player:bob"
ZADD leaderboard 1800 "player:carol"

# Nếu muốn CỘNG DỒN điểm (thay vì ghi đè) - dùng ZINCRBY
ZINCRBY leaderboard 100 "player:alice"   # alice +100 điểm

# Lấy TOP 10 người chơi điểm CAO NHẤT (ZREVRANGE - đảo ngược vì mặc định ZSet sắp xếp TĂNG DẦN)
ZREVRANGE leaderboard 0 9 WITHSCORES

# Lấy THỨ HẠNG cụ thể của 1 người chơi (rank bắt đầu từ 0, ZREVRANK vì cần thứ hạng theo điểm CAO -> THẤP)
ZREVRANK leaderboard "player:alice"
# Nếu muốn hạng bắt đầu từ 1 (thân thiện hơn với người dùng): cộng thêm 1 ở tầng ứng dụng
```

### Giải thích

- **Sorted Set (ZSet) là cấu trúc dữ liệu LÝ TƯỞNG cho leaderboard** vì Redis **DUY TRÌ THỨ TỰ SẮP XẾP TỰ ĐỘNG** bên trong (dựa trên cấu trúc Skip List nội bộ) — các thao tác `ZADD`, `ZRANK`/`ZREVRANK`, `ZRANGE`/`ZREVRANGE` đều có độ phức tạp **O(log N)**, RẤT NHANH ngay cả với hàng triệu người chơi — so với việc tự cài đặt bảng xếp hạng bằng SQL (`ORDER BY score DESC LIMIT 10` mỗi lần cần tính lại, tốn hơn nhiều nếu bảng lớn và không có index phù hợp).
- **`ZREVRANK` vs `ZRANK`:** `ZRANK` trả về thứ hạng theo điểm **TĂNG DẦN** (hạng 0 = điểm THẤP NHẤT) — ngược với ý nghĩa thông thường của "bảng xếp hạng" (hạng 1 = điểm CAO NHẤT); `ZREVRANK` mới đúng ngữ nghĩa cần — luôn cẩn thận đọc kỹ tài liệu khi chọn giữa 2 lệnh gần giống nhau này.
- **`ZINCRBY` vs `ZADD` lại giá trị mới:** `ZINCRBY` an toàn hơn cho tình huống "cộng dồn điểm" (VD mỗi câu trả lời đúng +10 điểm) — tránh phải tự đọc điểm cũ rồi tính tổng rồi `ZADD` lại (2 bước, có race condition tiềm ẩn giữa đọc và ghi); `ZINCRBY` là 1 lệnh **NGUYÊN TỬ (atomic)** duy nhất ở tầng Redis.

---

## Bài 5 — Bài toán tổng hợp: Polyglot Persistence cho hệ thống Flash-Sale

### Đề
(a) dữ liệu nào (vé, đơn hàng, thanh toán, user) nên ở PostgreSQL/MySQL và tại sao; (b) vai trò Redis giải quyết oversold ở tầng DB/cache cho nhiều instance backend (Distributed Lock); (c) MongoDB có cần không, cho trường hợp nào?

### Lời giải

**(a) Dữ liệu nên nằm ở PostgreSQL/MySQL: vé, đơn hàng, thanh toán, user**

Tất cả 4 loại dữ liệu này đều có đặc điểm chung: **cần tính ACID nghiêm ngặt, đặc biệt là tính NHẤT QUÁN (Consistency) và CÔ LẬP (Isolation)** — sai lệch dù nhỏ (VD bán 2 vé cho cùng 1 chỗ ngồi, hoặc thanh toán trừ tiền 2 lần) là **KHÔNG THỂ CHẤP NHẬN** về mặt nghiệp vụ lẫn pháp lý (giao dịch tài chính). RDBMS với `TRANSACTION` (`BEGIN`/`COMMIT`/`ROLLBACK`), ràng buộc khóa ngoại (đơn hàng phải trỏ tới user có thật, vé phải trỏ tới sự kiện có thật), và cơ chế khóa (`FOR UPDATE` — Module 10 Bài 4) là công cụ ĐÚNG ĐẮN NHẤT cho các yêu cầu này.

**(b) Vai trò của Redis giải quyết oversold ở tầng DB/cache — Distributed Lock**

Vấn đề mới so với Module 05.1/05.2: `synchronized`/`AtomicInteger` chỉ hoạt động đúng **TRONG CÙNG 1 JVM** — khi hệ thống có **NHIỀU INSTANCE backend chạy song song** (scale ngang, mỗi instance là 1 process/JVM riêng biệt), `synchronized` ở instance A **HOÀN TOÀN VÔ NGHĨA** với instance B — 2 instance có thể vẫn đồng thời "nghĩ" rằng còn vé.

Redis giải quyết bằng **Distributed Lock**, ví dụ dùng `SET key value NX EX 10` (chỉ set nếu key CHƯA tồn tại — `NX`, tự hết hạn sau 10 giây — `EX`, tránh deadlock vĩnh viễn nếu instance giữ lock bị crash):

```
1. Instance nhận request đặt vé cho event_id = 42
2. Thử: SET lock:event:42 <instance_id> NX EX 10
   - Nếu SET thành công (trả về OK) -> instance này ĐANG GIỮ LOCK cho event 42
     -> tiến hành kiểm tra + trừ vé trong PostgreSQL (transaction FOR UPDATE như Module 10 Bài 4)
     -> xong thì DEL lock:event:42 (giải phóng lock SỚM, không cần đợi hết 10s)
   - Nếu SET thất bại (key đã tồn tại, instance KHÁC đang giữ lock)
     -> trả lỗi "hệ thống đang xử lý, thử lại" hoặc đưa vào hàng đợi retry
```

Ngoài ra, Redis còn có thể dùng theo cách **KHÔNG CẦN LOCK** hiệu quả hơn: giữ **số vé còn lại ngay trong Redis** (`DECR available_seats:42`) — `DECR` là lệnh NGUYÊN TỬ ở tầng Redis (single-threaded event loop nội bộ, không có race condition dù hàng nghìn request đồng thời), nếu kết quả `< 0` thì `INCR` lại (hoàn tác) và báo hết vé; PostgreSQL chỉ đóng vai trò lưu trữ bền vững (persist) đơn hàng SAU KHI Redis đã "chốt" thành công — cách này **NHANH HƠN NHIỀU** so với dùng lock rồi mới chạm DB, vì tránh hẳn round-trip tới PostgreSQL cho phần lớn request (chỉ request THẬT SỰ mua thành công mới cần ghi DB).

**(c) MongoDB có cần không?**

**CÓ THỂ cần**, nhưng KHÔNG cho dữ liệu giao dịch cốt lõi (vé/đơn hàng/thanh toán — đã thuộc PostgreSQL ở mục (a)). Trường hợp phù hợp: **log sự kiện / audit trail / lịch sử hoạt động người dùng** (VD: lưu lại TOÀN BỘ log "user X xem sản phẩm Y lúc Z", "user X thử đặt vé nhưng thất bại vì hết vé lúc W") — dữ liệu này:
- Có **khối lượng RẤT LỚN**, tăng liên tục theo thời gian (write-heavy).
- **KHÔNG cần tính ACID nghiêm ngặt** (mất 1 vài dòng log không gây hậu quả nghiêm trọng).
- **Cấu trúc linh hoạt** — mỗi loại sự kiện có thể có field khác nhau (schema-less phù hợp).
- Dùng để phân tích sau này (analytics, phát hiện gian lận/bot mua vé hàng loạt) — MongoDB (hoặc giải pháp NoSQL write-heavy khác) phù hợp hơn RDBMS cho khối lượng ghi lớn, ít ràng buộc quan hệ.

---

## Bài 6 — Chống overlap bằng PostgreSQL Range Type

### Đề
Đặt phòng họp, 1 phòng không trùng khung giờ. Schema dùng `TSTZRANGE` + `EXCLUDE CONSTRAINT`. So sánh với tự kiểm tra overlap bằng code Java.

### Lời giải

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;   -- cần thiết để EXCLUDE hoạt động với cột thường (room_id) + range

CREATE TABLE room_bookings (
    id        BIGSERIAL PRIMARY KEY,
    room_id   BIGINT NOT NULL REFERENCES rooms(id),
    booked_by BIGINT NOT NULL REFERENCES users(id),
    time_range TSTZRANGE NOT NULL,

    -- EXCLUDE CONSTRAINT: DATABASE tự động NGĂN CHẶN insert 2 dòng
    -- có CÙNG room_id VÀ time_range GIAO NHAU (overlap, toán tử &&)
    EXCLUDE USING GIST (room_id WITH =, time_range WITH &&)
);

-- INSERT hợp lệ
INSERT INTO room_bookings (room_id, booked_by, time_range)
VALUES (1, 100, '[2026-03-01 09:00+07, 2026-03-01 10:00+07)');

-- INSERT này sẽ BỊ TỪ CHỐI bởi DB (lỗi exclusion constraint violation) vì TRÙNG GIỜ với dòng trên, cùng room_id
INSERT INTO room_bookings (room_id, booked_by, time_range)
VALUES (1, 200, '[2026-03-01 09:30+07, 2026-03-01 10:30+07)');
```

### Giải thích — so sánh với tự kiểm tra bằng code Java

| | `EXCLUDE CONSTRAINT` (DB tự đảm bảo) | Tự kiểm tra overlap bằng Java trước `INSERT` |
|---|---|---|
| Cơ chế | DB kiểm tra NGUYÊN TỬ ngay tại thời điểm `INSERT`, ở tầng thấp nhất, không thể "lách" qua | Code Java: `SELECT` kiểm tra không có overlap → nếu ổn thì `INSERT` (2 bước RIÊNG BIỆT) |
| An toàn dưới tải đồng thời cao | **AN TOÀN TUYỆT ĐỐI** — không có "khoảng hở" giữa kiểm tra và ghi | **KHÔNG AN TOÀN** — y hệt race condition ở Module 10 Bài 4: 2 request đến gần như đồng thời đều `SELECT` thấy "chưa có overlap" (vì dòng kia CHƯA kịp insert), cả 2 đều `INSERT` thành công → **XUNG ĐỘT LỊCH (double-booking)** |
| Cách khắc phục nếu chỉ dùng Java | Phải tự thêm `SELECT ... FOR UPDATE` khóa các dòng liên quan trước khi kiểm tra — phức tạp, dễ sai (phải khóa ĐÚNG phạm vi, không thiếu không thừa) | — |
| Độ phức tạp cài đặt | Đơn giản, khai báo 1 lần ở schema, áp dụng cho MỌI đường ghi dữ liệu (kể cả từ nhiều service khác nhau, hoặc thao tác trực tiếp bằng SQL) | Phải LẶP LẠI logic kiểm tra ở MỌI nơi có thể ghi dữ liệu — dễ bỏ sót (VD 1 script migrate dữ liệu quên kiểm tra) |

- **Kết luận: `EXCLUDE CONSTRAINT` an toàn hơn HẲN** dưới tải đồng thời cao — đây là ví dụ tiêu biểu cho nguyên tắc **"đẩy ràng buộc toàn vẹn dữ liệu xuống tầng DB bất cứ khi nào có thể"** thay vì chỉ dựa vào logic tầng ứng dụng — tầng DB đảm bảo tính đúng đắn **BẤT KỂ** có bao nhiêu service/thread/instance nào đang ghi đồng thời, trong khi logic Java chỉ đúng nếu **TẤT CẢ** các đường ghi đều tuân thủ đúng quy trình kiểm tra (dễ có ngoại lệ bị bỏ sót qua thời gian khi hệ thống phát triển thêm nhiều tính năng mới).
- Toán tử `&&` (overlap) trên `TSTZRANGE` cùng chỉ mục **GiST** (Generalized Search Tree — loại index hỗ trợ kiểu dữ liệu "hình học"/khoảng giá trị) cho phép kiểm tra giao nhau HIỆU QUẢ (không cần quét tuần tự so sánh từng cặp khoảng thời gian).

---

*Đây là lời giải cho toàn bộ Phần B của Module 19. Tiếp theo: Module 20 — JPA & Hibernate.*
