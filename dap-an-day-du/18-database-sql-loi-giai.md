# Lời giải đầy đủ — Module 10: Database & SQL

> Nguồn đề: `18-database-sql/18-database-sql.md` (Phần B — Bài tập viết SQL). Chỉ làm Phần B.

---

## Bài 1 — JOIN & GROUP BY tổng hợp

### Đề
`customers(id, name)`, `orders(id, customer_id, order_date, total_amount)`. (a) tên khách hàng + tổng chi tiêu, giảm dần; (b) chỉ khách chi tiêu > 5.000.000; (c) khách chưa từng đặt hàng vẫn xuất hiện với tổng = 0.

### Phân tích

Điểm mấu chốt: **(a)/(b) chỉ cần khách CÓ đơn hàng** → `INNER JOIN` là đủ (khách không có đơn tự động bị loại). Nhưng **(c) yêu cầu NGƯỢC LẠI** — khách KHÔNG có đơn hàng vẫn phải xuất hiện → bắt buộc **`LEFT JOIN`** (giữ toàn bộ dòng bên trái là `customers`, dù không khớp bên `orders`) kết hợp **`COALESCE`** để biến `NULL` (do không có dòng khớp) thành `0`.

### Lời giải

```sql
-- (a) Tổng chi tiêu mỗi khách, giảm dần (chỉ khách CÓ đơn hàng)
SELECT c.name, SUM(o.total_amount) AS total_spent
FROM customers c
JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name
ORDER BY total_spent DESC;

-- (b) Chỉ khách chi tiêu > 5.000.000 - lọc SAU khi GROUP BY nên dùng HAVING, không dùng WHERE
SELECT c.name, SUM(o.total_amount) AS total_spent
FROM customers c
JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name
HAVING SUM(o.total_amount) > 5000000
ORDER BY total_spent DESC;

-- (c) TẤT CẢ khách hàng, kể cả chưa từng đặt hàng (tổng = 0)
SELECT c.name, COALESCE(SUM(o.total_amount), 0) AS total_spent
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name
ORDER BY total_spent DESC;
```

### Giải thích

- **`WHERE` vs `HAVING`:** `WHERE` lọc **DÒNG THÔ** trước khi `GROUP BY` gộp nhóm; `HAVING` lọc **SAU KHI** đã tính `SUM`/`COUNT`/... — vì điều kiện ở câu (b) dựa trên kết quả tổng hợp (`SUM(total_amount) > 5000000`), **bắt buộc** dùng `HAVING`. Viết nhầm thành `WHERE SUM(...) > 5000000` sẽ báo lỗi cú pháp (hàm tổng hợp không được dùng trong `WHERE`).
- **Vì sao `LEFT JOIN` ở câu (c) tạo ra `NULL` cho `SUM`:** khi khách không có đơn hàng khớp, `LEFT JOIN` vẫn giữ dòng `customers` nhưng toàn bộ cột từ `orders` (kể cả `total_amount`) là `NULL`. `SUM(NULL, NULL, ...)` trên 1 nhóm toàn `NULL` trả về `NULL` (không phải `0`) — vì vậy **bắt buộc** bọc `COALESCE(SUM(...), 0)` để hiển thị đúng yêu cầu nghiệp vụ "tổng = 0".
- **`GROUP BY c.id, c.name`** (không chỉ `c.name`): nhóm theo khóa chính `id` để tránh gộp nhầm 2 khách **trùng tên** thành 1 nhóm — nguyên tắc chung: luôn `GROUP BY` theo khóa duy nhất, các cột khác (như `name`) chỉ nên "phụ thuộc hàm" vào khóa đó (functional dependency), một số RDBMS (MySQL ở chế độ không strict) cho phép bỏ qua nhưng PostgreSQL/chuẩn SQL sẽ báo lỗi nếu thiếu.

---

## Bài 2 — Window Function cho Top-N mỗi nhóm

### Đề
`employees(id, name, department, salary)`. (a) correlated subquery, (b) `RANK() OVER (PARTITION BY ...)` tìm lương cao nhất mỗi phòng ban. Thêm câu dùng `LAG()` so lương với người xếp ngay trên.

### Lời giải

```sql
-- (a) Correlated subquery: với MỖI dòng employees, kiểm tra có phải lương CAO NHẤT
--     trong CHÍNH phòng ban của dòng đó không
SELECT e.name, e.department, e.salary
FROM employees e
WHERE e.salary = (
    SELECT MAX(e2.salary)
    FROM employees e2
    WHERE e2.department = e.department   -- "correlated": subquery THAM CHIẾU tới dòng ngoài (e.department)
);

-- (b) Window function RANK() OVER PARTITION BY - hiệu quả hơn, dễ đọc hơn
SELECT name, department, salary
FROM (
    SELECT name, department, salary,
           RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rnk
    FROM employees
) ranked
WHERE rnk = 1;

-- Bonus: LAG() so lương với người xếp NGAY TRÊN (theo salary DESC) cùng phòng ban
SELECT name, department, salary,
       LAG(salary) OVER (PARTITION BY department ORDER BY salary DESC) AS salary_nguoi_tren,
       salary - LAG(salary) OVER (PARTITION BY department ORDER BY salary DESC) AS chenh_lech
FROM employees
ORDER BY department, salary DESC;
```

### Giải thích

| Tiêu chí | Correlated Subquery | Window Function |
|---|---|---|
| Độ phức tạp thời gian | O(n²) về khái niệm — với MỖI dòng ngoài, chạy lại subquery quét toàn bộ nhóm tương ứng | O(n log n) — DB chỉ cần 1 lần `PARTITION` + `SORT`, không quét lặp lại |
| Xử lý "đồng hạng" (2 người cùng lương cao nhất) | Trả về CẢ HAI dòng tự nhiên (vì `=` so khớp cả hai) | `RANK()` cũng trả cả hai (cùng rank 1) — nhất quán |
| Độ dễ đọc/mở rộng | Khó mở rộng thành "Top 3 mỗi phòng ban" (phải sửa logic subquery) | Chỉ cần đổi điều kiện `WHERE rnk <= 3` — mở rộng cực dễ |
| Khuyến nghị thực tế | Dùng khi DB cũ không hỗ trợ Window Function (hiếm gặp ngày nay) | **Ưu tiên mặc định** với PostgreSQL/MySQL 8+/SQL Server hiện đại |

- **`RANK()` vs `DENSE_RANK()` vs `ROW_NUMBER()`:** `RANK()` để lại "khoảng trống" sau đồng hạng (1,1,3); `DENSE_RANK()` không để trống (1,1,2); `ROW_NUMBER()` luôn duy nhất dù trùng giá trị (1,2,3) — bài này dùng `RANK()` vì đúng ngữ nghĩa "cùng lương cao nhất thì đều là hạng 1".
- **`LAG()`** đọc giá trị của **DÒNG PHÍA TRƯỚC** trong cùng partition theo thứ tự `ORDER BY` — cực hữu ích để so sánh "chênh lệch với người liền kề" mà không cần tự-join (self-join) phức tạp; `LEAD()` là hàm đối xứng, đọc dòng PHÍA SAU.

---

## Bài 3 — Thiết kế schema chuẩn hóa cho Blog

### Đề
`CREATE TABLE` đầy đủ: `users`, `posts` (1 tác giả/bài), `tags`, quan hệ N-N `posts`↔`tags` qua bảng trung gian khóa chính ghép. Tuân thủ 3NF.

### Lời giải

```sql
CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    email    VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE posts (
    id         BIGSERIAL PRIMARY KEY,
    author_id  BIGINT NOT NULL REFERENCES users(id),   -- 1 tác giả/bài -> khóa ngoại đơn, không cần bảng trung gian
    title      VARCHAR(255) NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Bảng trung gian (junction table) cho quan hệ NHIỀU-NHIỀU: 1 post có nhiều tag, 1 tag gắn nhiều post
CREATE TABLE post_tags (
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)   -- khóa chính GHÉP - đảm bảo 1 cặp (post, tag) chỉ tồn tại 1 lần
);

-- Index hỗ trợ tra cứu ngược "tag này có những post nào" hiệu quả (khóa ghép chỉ tối ưu tra theo post_id trước)
CREATE INDEX idx_post_tags_tag_id ON post_tags(tag_id);
```

### Giải thích

- **`posts.author_id` là khóa ngoại ĐƠN, không cần bảng trung gian** vì đề bài nói rõ "1 tác giả/bài" — đây là quan hệ **1-N** (1 user viết N post), khác hẳn với quan hệ N-N (`posts`↔`tags`) cần bảng trung gian riêng.
- **Khóa chính GHÉP `(post_id, tag_id)`** trên `post_tags` vừa đóng vai trò khóa chính, vừa tự động **NGĂN TRÙNG LẶP** (không thể insert 2 lần cùng 1 cặp post-tag) — không cần thêm `UNIQUE` constraint riêng.
- **Tuân thủ 3NF:** không có cột nào phụ thuộc bắc cầu (transitive dependency) vào cột không-khóa khác — VD `tags.name` chỉ phụ thuộc vào `tags.id`, không lưu trùng lặp tên tag trong `posts` hay `post_tags`.
- **`ON DELETE CASCADE`** trên `post_tags`: khi xóa 1 `post` hoặc 1 `tag`, các dòng liên kết trong bảng trung gian tự động bị xóa theo — tránh "orphan row" (dòng mồ côi trỏ tới khóa ngoại không còn tồn tại).
- **`idx_post_tags_tag_id`:** khóa chính ghép `(post_id, tag_id)` tự tạo index B-Tree nhưng chỉ tối ưu truy vấn **bắt đầu từ `post_id`** (Leftmost Prefix — xem thêm Bài 5). Truy vấn ngược "tag X gắn với những post nào" (`WHERE tag_id = X`) cần index riêng trên `tag_id` mới nhanh.

---

## Bài 4 — Transaction đặt vé với khóa bi quan (Pessimistic Locking)

### Đề
`SELECT ... FOR UPDATE` kiểm tra còn vé, giảm vé, insert `bookings`. Giải thích vì sao cần `FOR UPDATE`.

### Lời giải

```sql
BEGIN;

-- FOR UPDATE: khóa (lock) dòng này NGAY LÚC ĐỌC, các transaction KHÁC cố đọc-để-sửa
-- cùng dòng này sẽ phải CHỜ (block) cho tới khi transaction hiện tại COMMIT/ROLLBACK
SELECT available_seats FROM events WHERE id = 42 FOR UPDATE;

-- Giả sử ứng dụng đọc được available_seats = 3, kiểm tra > 0 (ở tầng code hoặc ngay trong SQL)
-- Nếu available_seats <= 0 thì ROLLBACK và báo "hết vé", không chạy tiếp 2 lệnh dưới

UPDATE events SET available_seats = available_seats - 1 WHERE id = 42;

INSERT INTO bookings (event_id, user_id, booked_at) VALUES (42, 1001, NOW());

COMMIT;
```

### Giải thích — vì sao cần `FOR UPDATE`

- **Liên hệ trực tiếp Race Condition đã học ở Module 05.1 (Thread cơ bản), nhưng ở TẦNG DATABASE thay vì tầng bộ nhớ trong 1 process JVM:** nếu KHÔNG có `FOR UPDATE`, 2 transaction chạy đồng thời (2 request đặt vé cùng lúc khi chỉ còn 1 vé) đều `SELECT` ra `available_seats = 1` **TRƯỚC KHI** bên nào kịp `UPDATE` — cả hai đều thấy "còn vé", cả hai đều `UPDATE` giảm xuống `0`, cả hai đều `INSERT` vào `bookings` → **OVERSOLD** (bán vượt quá số vé thực có), tương tự race condition kinh điển "đọc-sửa-ghi không nguyên tử" (`count++` không an toàn giữa nhiều thread).
- **`FOR UPDATE` tương đương `synchronized` ở tầng ứng dụng, nhưng hoạt động ở tầng DB, giữa nhiều KẾT NỐI/PROCESS khác nhau** (kể cả từ nhiều server ứng dụng khác nhau, không chỉ trong 1 JVM) — nó biến chuỗi "đọc → kiểm tra → sửa" thành 1 khối **NGUYÊN TỬ (atomic)** đối với dòng dữ liệu đó: transaction thứ 2 phải **ĐỢI** transaction thứ nhất `COMMIT` xong (giải phóng lock) mới được đọc, lúc đó nó sẽ thấy `available_seats = 0` (giá trị ĐÃ CẬP NHẬT) và biết hết vé.
- **Đánh đổi:** `FOR UPDATE` giảm khả năng xử lý đồng thời (throughput) vì các transaction phải xếp hàng chờ nhau trên cùng 1 dòng — chấp nhận được với tài nguyên có giới hạn cứng (vé, chỗ ngồi) nơi **tính đúng đắn (correctness) quan trọng hơn tốc độ**. So sánh với giải pháp thay thế ở Bài 6 (optimistic locking).

---

## Bài 5 — Chẩn đoán & tối ưu query chậm

### Đề
```sql
SELECT * FROM orders WHERE customer_id = 12345 AND order_date > '2026-01-01' ORDER BY order_date DESC;
```
(10 triệu dòng). Đề xuất index, viết `CREATE INDEX`, mô tả cách xác nhận bằng `EXPLAIN ANALYZE`.

### Phân tích

Không có index phù hợp, DB buộc phải **Sequential Scan** (quét tuần tự cả 10 triệu dòng) để tìm ra các dòng khớp `customer_id = 12345` — cực chậm. Cần **composite index** đúng thứ tự cột theo nguyên tắc **Leftmost Prefix**.

### Lời giải

```sql
-- Composite index: customer_id TRƯỚC (điều kiện bằng, lọc mạnh nhất),
-- order_date SAU (vừa dùng để lọc khoảng >, vừa dùng để ORDER BY DESC luôn - "free sort")
CREATE INDEX idx_orders_customer_date ON orders(customer_id, order_date DESC);

-- Xác nhận bằng EXPLAIN ANALYZE - so sánh TRƯỚC và SAU khi tạo index
EXPLAIN ANALYZE
SELECT * FROM orders
WHERE customer_id = 12345 AND order_date > '2026-01-01'
ORDER BY order_date DESC;
```

**Kết quả mong đợi (trước index — Sequential Scan):**
```
Seq Scan on orders  (cost=0.00..250000.00 rows=50 width=120) (actual time=850.234..1200.567 rows=42 loops=1)
  Filter: (customer_id = 12345 AND order_date > '2026-01-01')
  Rows Removed by Filter: 9999958
Planning Time: 0.150 ms
Execution Time: 1201.045 ms
```

**Kết quả mong đợi (sau index — Index Scan):**
```
Index Scan using idx_orders_customer_date on orders  (cost=0.42..8.60 rows=42 width=120) (actual time=0.045..0.180 rows=42 loops=1)
  Index Cond: (customer_id = 12345 AND order_date > '2026-01-01')
Planning Time: 0.120 ms
Execution Time: 0.210 ms
```

### Giải thích

- **Thứ tự cột trong composite index QUYẾT ĐỊNH tính hữu dụng — nguyên tắc Leftmost Prefix:** index `(customer_id, order_date)` hữu dụng cho truy vấn lọc theo `customer_id` một mình, hoặc `customer_id` + `order_date`, nhưng **VÔ DỤNG** nếu chỉ lọc theo `order_date` một mình (giống tra từ điển theo vần đầu — không thể "nhảy" thẳng vào giữa mà bỏ qua cột đầu). Vì truy vấn ở đây LUÔN có `customer_id =` (điều kiện bằng, chọn lọc mạnh — thu hẹp còn rất ít dòng) nên đặt **`customer_id` làm cột ĐẦU TIÊN** là đúng.
- **`order_date DESC` trong định nghĩa index khớp với `ORDER BY order_date DESC` trong query** — B-Tree index vốn đã có thứ tự sẵn (sorted), nên khi thứ tự index KHỚP với thứ tự `ORDER BY` cần, DB **không cần thêm bước `Sort` riêng** (tránh "free sort" bị mất, tránh `Sort` tốn O(n log n) bổ sung trên tập kết quả).
- **Đọc `EXPLAIN ANALYZE` — 3 tín hiệu quan trọng cần nhìn:**
  1. **`Seq Scan` → `Index Scan`**: loại thao tác đổi từ quét toàn bảng sang dùng index — đây là tín hiệu tối ưu thành công rõ ràng nhất.
  2. **`Rows Removed by Filter: 9999958`** (trước index): số dòng bị "đọc lên rồi loại bỏ" — càng lớn càng lãng phí I/O; sau khi có index, con số này biến mất vì index đã tìm đúng ngay từ đầu.
  3. **`Execution Time`**: so sánh trực tiếp thời gian THỰC THI (không phải ước lượng `cost`) — đây là con số đáng tin cậy nhất để khẳng định index có tác dụng.
- **`EXPLAIN` (không `ANALYZE`)** chỉ hiển thị KẾ HOẠCH ƯỚC LƯỢNG (dựa trên thống kê), KHÔNG THỰC SỰ chạy query; `EXPLAIN ANALYZE` **THỰC SỰ CHẠY** query và đo thời gian thật — cần cẩn trọng khi dùng trên câu `UPDATE`/`DELETE` ở production (nên bọc trong transaction rồi `ROLLBACK` nếu chỉ muốn đo, không muốn áp dụng thay đổi thật).

---

## Bài 6 — Khóa lạc quan (Optimistic Locking) cho oversold

### Đề
Viết lại Bài 4 bằng optimistic locking (cột `version`), không dùng `FOR UPDATE`. So sánh khi nào tốt hơn/kém hơn pessimistic.

### Lời giải

```sql
-- Chuẩn bị: bảng events có thêm cột version
-- ALTER TABLE events ADD COLUMN version INT NOT NULL DEFAULT 0;

BEGIN;

-- BƯỚC 1: Đọc BÌNH THƯỜNG, không khóa gì cả (KHÔNG dùng FOR UPDATE)
SELECT available_seats, version FROM events WHERE id = 42;
-- Giả sử ứng dụng đọc được: available_seats = 3, version = 7

-- BƯỚC 2: Kiểm tra ở tầng ứng dụng: available_seats > 0? Nếu không, dừng lại, báo hết vé.

-- BƯỚC 3: UPDATE có điều kiện đúng version ĐÃ ĐỌC - đây chính là "optimistic lock"
UPDATE events
SET available_seats = available_seats - 1,
    version = version + 1
WHERE id = 42 AND version = 7;   -- CHỈ update nếu version CHƯA bị ai khác thay đổi

-- BƯỚC 4: Kiểm tra SỐ DÒNG BỊ ẢNH HƯỞNG (rows affected) của UPDATE trên
-- Nếu = 0 -> có transaction KHÁC đã update trước (version đã đổi) -> ROLLBACK, RETRY lại từ BƯỚC 1
-- Nếu = 1 -> thành công, tiếp tục insert booking

INSERT INTO bookings (event_id, user_id, booked_at) VALUES (42, 1001, NOW());

COMMIT;
```

### Giải thích — so sánh Optimistic vs Pessimistic Locking

| Tiêu chí | Pessimistic (`FOR UPDATE`, Bài 4) | Optimistic (cột `version`, Bài 6) |
|---|---|---|
| Cơ chế | Khóa dòng NGAY LÚC ĐỌC, transaction khác phải CHỜ | Không khóa gì khi đọc; chỉ kiểm tra `version` khớp lúc `UPDATE` |
| Khi tranh chấp XẢY RA THƯỜNG XUYÊN (hot row, VD vé hot, số lượng ít, nhiều người tranh nhau cùng lúc) | **Tốt hơn** — tránh lãng phí công sức retry liên tục, transaction chờ rồi xử lý tuần tự | Kém hơn — nhiều lần `UPDATE` thất bại (`rows affected = 0`) phải retry lặp lại nhiều lần, tốn round-trip |
| Khi tranh chấp HIẾM XẢY RA (nhiều sản phẩm, ít người mua trùng thời điểm) | Kém hơn — tốn chi phí khóa/chờ dù hầu như không bao giờ thực sự xung đột | **Tốt hơn** — hầu hết các lần `UPDATE` thành công ngay lần đầu, không phải trả giá cho lock overhead |
| Rủi ro Deadlock | CÓ THỂ xảy ra nếu nhiều transaction khóa nhiều dòng theo thứ tự khác nhau (liên hệ Module 05.1 — lock ordering) | KHÔNG có deadlock (không hề giữ lock nào giữa các bước) |
| Trải nghiệm người dùng khi thất bại | Người dùng ĐỢI (block) tới khi transaction trước xong | Người dùng bị từ chối NGAY (rows affected = 0) và cần thử lại — UX có thể kém hơn nếu retry logic không tốt |

- **Nguyên tắc chọn lựa tổng quát:** *"Pessimistic khi kỳ vọng CÓ xung đột (contention cao); Optimistic khi kỳ vọng ÍT xung đột"* — đúng với chính tên gọi: "bi quan" (giả định luôn có người khác đang tranh giành, nên khóa trước cho chắc) và "lạc quan" (giả định hiếm khi có người khác đụng vào, cứ làm rồi kiểm tra lại sau).
- **Optimistic Locking chính là kỹ thuật nền tảng phía sau `@Version` của JPA/Hibernate** (sẽ gặp lại ở Module 12 — JPA & Hibernate) — Hibernate tự động thêm điều kiện `WHERE ... AND version = ?` vào câu `UPDATE` sinh ra, và ném `OptimisticLockException` khi số dòng ảnh hưởng = 0.

---

*Đây là lời giải cho toàn bộ Phần B của Module 18. Tiếp theo: Module 19 — RDBMS & NoSQL.*
