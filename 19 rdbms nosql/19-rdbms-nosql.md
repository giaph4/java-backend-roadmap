# Module 10 (tiếp) — RDBMS phổ biến & NoSQL

> **Mức độ ưu tiên: Trung bình (RDBMS thực hành) → Bổ sung (NoSQL)** — Biết cú pháp SQL chuẩn (Module 10) là chưa đủ, cần thành thạo **ít nhất 1 RDBMS thực tế** để làm việc được ngay khi đi thực tập/đi làm. NoSQL không phải "thay thế" SQL mà là **công cụ bổ sung** cho đúng bài toán — hiểu **khi nào** dùng loại nào quan trọng hơn việc thuộc lòng cú pháp MongoDB.
>
> **Phạm vi bài này:** so sánh MySQL/PostgreSQL ở mức thực hành, cơ chế lưu trữ bên trong (InnoDB, kiểu dữ liệu PostgreSQL nâng cao), tổng quan NoSQL (MongoDB, Redis) và tư duy chọn công nghệ. Bài **không** đi sâu ORM/JPA — đó là Module 11 (@Entity, N+1, Lazy/Eager) — và **không** đi sâu kiến trúc Microservices/System Design — đó là các module 18–22 phía sau; bài chỉ **gieo mầm tư duy** để chuẩn bị cho các module đó.

---

## Mục lục

1. [MySQL vs PostgreSQL — so sánh thực hành](#1-mysql-vs-postgresql--so-sánh-thực-hành)
2. [Storage Engine của MySQL — InnoDB](#2-storage-engine-của-mysql--innodb)
3. [Kiểu dữ liệu nâng cao của PostgreSQL](#3-kiểu-dữ-liệu-nâng-cao-của-postgresql)
4. [NoSQL là gì — tổng quan các loại](#4-nosql-là-gì--tổng-quan-các-loại)
5. [MongoDB — Document Database](#5-mongodb--document-database)
6. [Redis — Key-Value Store](#6-redis--key-value-store)
7. [Redis — cấu trúc dữ liệu nâng cao](#7-redis--cấu-trúc-dữ-liệu-nâng-cao)
8. [SQL vs NoSQL — CAP Theorem ở mức khái niệm](#8-sql-vs-nosql--cap-theorem-ở-mức-khái-niệm)
9. [Khi nào chọn SQL, khi nào chọn NoSQL — cây quyết định thực tế](#9-khi-nào-chọn-sql-khi-nào-chọn-nosql--cây-quyết-định-thực-tế)
10. [Mô hình Polyglot Persistence — dùng NHIỀU loại database cùng lúc](#10-mô-hình-polyglot-persistence--dùng-nhiều-loại-database-cùng-lúc)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. MySQL vs PostgreSQL — so sánh thực hành

Cả 2 đều là **RDBMS mã nguồn mở phổ biến nhất thế giới**, đều tuân thủ chuẩn SQL đã học ở Module 10 — nhưng có những khác biệt quan trọng cần biết khi chọn dùng thực tế.

| Tiêu chí | MySQL | PostgreSQL |
|---|---|---|
| Triết lý thiết kế | Ưu tiên **tốc độ, đơn giản**, dễ dùng cho web application truyền thống | Ưu tiên **tuân thủ chuẩn SQL nghiêm ngặt**, tính năng phong phú, "đúng đắn" về mặt học thuật |
| Kiểu dữ liệu | Cơ bản, ít kiểu nâng cao | **Phong phú hơn hẳn**: JSON/JSONB, Array, UUID nguyên sinh, ENUM, Range Type... (mục 3) |
| Full-Text Search | Có, nhưng hạn chế hơn | Mạnh hơn đáng kể, tích hợp sẵn (`tsvector`/`tsquery`) |
| Xử lý đồng thời (Concurrency) | Dùng khóa (locking) truyền thống, InnoDB cũng có MVCC nhưng cài đặt đơn giản hơn | Dùng **MVCC (Multi-Version Concurrency Control)** đầy đủ và tinh vi hơn — cho phép đọc/ghi đồng thời hiệu quả mà ít gây khóa chờ (chi tiết cơ chế MVCC nói chung: Module 10, mục 7) |
| Mở rộng (Extensibility) | Hạn chế | **Extension system** mạnh — `PostGIS` (dữ liệu địa lý), `pg_trgm` (fuzzy search), `TimescaleDB` (time-series) cài như plugin |
| Replication | Master-Slave (binlog-based) đơn giản, dễ cấu hình | Streaming Replication + Logical Replication linh hoạt hơn, hỗ trợ replica đọc gần như đồng bộ |
| Độ phổ biến tại Việt Nam | RẤT phổ biến — đặc biệt ở startup, dự án web truyền thống, framework PHP (WordPress, Laravel mặc định) | Ngày càng phổ biến ở dự án MỚI, đặc biệt ưa chuộng trong hệ sinh thái backend hiện đại (nhiều công ty công nghệ, dự án Spring Boot) |
| License | GPL (Oracle sở hữu) | PostgreSQL License (rất "mở", gần giống MIT) |

> **Khuyến nghị thực tế cho người mới:** cả 2 đều là lựa chọn tốt và **kiến thức SQL chuẩn học được ở Module 10 áp dụng được cho cả hai** (khác biệt chủ yếu ở 1 số tính năng mở rộng, cú pháp function đặc thù). Nếu phải chọn 1 để thực hành sâu trước, **PostgreSQL** đang là xu hướng được nhiều dự án Spring Boot hiện đại và các công ty công nghệ ưa chuộng hơn nhờ tính năng phong phú và độ tuân thủ chuẩn cao — nhưng biết cả 2 (ít nhất ở mức thực hành cơ bản) là lợi thế lớn khi đi phỏng vấn, vì mỗi công ty có stack công nghệ khác nhau.

### Một số khác biệt cú pháp hay gặp khi chuyển đổi qua lại

| Việc cần làm | MySQL | PostgreSQL |
|---|---|---|
| Tự tăng ID | `AUTO_INCREMENT` | `SERIAL` / `GENERATED ALWAYS AS IDENTITY` (chuẩn SQL hơn) |
| Giới hạn kết quả | `LIMIT 10 OFFSET 20` | `LIMIT 10 OFFSET 20` (giống nhau) hoặc `OFFSET 20 FETCH NEXT 10 ROWS ONLY` (chuẩn ANSI) |
| Nối chuỗi | `CONCAT(a, b)` | `a \|\| b` hoặc `CONCAT(a, b)` (PostgreSQL hỗ trợ cả 2) |
| Phân biệt hoa/thường khi so sánh chuỗi | Mặc định **không phân biệt** (tùy collation) | Mặc định **CÓ phân biệt** hoa/thường (`'A' <> 'a'`) |
| Kiểu boolean | Không có kiểu riêng, dùng `TINYINT(1)` | Có kiểu `BOOLEAN` thật sự (`TRUE`/`FALSE`) |

> **Vì sao cần biết bảng này:** trong thực tế đi làm, việc đọc migration script hoặc code JPA/Hibernate viết cho MySQL rồi phải chạy trên PostgreSQL (hoặc ngược lại) không hiếm — Hibernate tự sinh SQL theo `dialect` cấu hình nên phần lớn khác biệt được che giấu, nhưng khi viết **native query** (`@Query(nativeQuery = true)`, sẽ gặp ở Module 11) thì các khác biệt này lộ ra ngay.

---

## 2. Storage Engine của MySQL — InnoDB

MySQL có khái niệm **Storage Engine** (cơ chế lưu trữ) — có thể chọn khác nhau cho từng bảng, dù **InnoDB** gần như luôn là lựa chọn mặc định và đúng đắn trong thực tế hiện đại.

| Storage Engine | Hỗ trợ Transaction (ACID)? | Hỗ trợ Foreign Key? | Khi nào dùng |
|---|---|---|---|
| **InnoDB** (mặc định từ MySQL 5.5+) | ✅ Có | ✅ Có | **LUÔN LUÔN nên dùng** cho ứng dụng backend thông thường — hỗ trợ đầy đủ ACID (Module 10) |
| **MyISAM** (cũ, hiếm dùng ngày nay) | ❌ Không | ❌ Không | Chỉ phù hợp bài toán đọc RẤT nhiều, ghi RẤT ít, KHÔNG cần transaction — hiếm gặp trong backend hiện đại |

> **Lưu ý quan trọng khi phỏng vấn:** nếu được hỏi "MySQL có hỗ trợ Transaction không?" — câu trả lời chính xác là **"Có, NẾU dùng storage engine InnoDB"** (là mặc định hiện nay) — không nên trả lời "Có" hay "Không" một cách tuyệt đối mà không nhắc đến ngữ cảnh storage engine, vì đây chính là điểm mà nhiều tài liệu cũ hay gây nhầm lẫn.

### Bên trong InnoDB hoạt động thế nào — 3 cơ chế cốt lõi

Hiểu 3 cơ chế này giúp trả lời được câu "vì sao commit lại nhanh dù dữ liệu đã ghi xuống đĩa an toàn" — câu hỏi phỏng vấn khá phổ biến khi đã học xong Durability ở Module 10.

**a) Buffer Pool — vùng nhớ đệm trong RAM.** InnoDB không đọc/ghi trực tiếp xuống đĩa cho mỗi câu lệnh — dữ liệu (data page, index page) được cache trong 1 vùng RAM gọi là **Buffer Pool**. Đọc dữ liệu đã có trong Buffer Pool nhanh hơn đọc đĩa hàng trăm lần; đây là lý do `innodb_buffer_pool_size` là tham số tuning quan trọng bậc nhất khi vận hành MySQL production.

**b) Redo Log — hiện thực hóa WAL (Write-Ahead Log).** Đây chính là cơ chế WAL đã nhắc ở Module 10 mục 6 (Durability): khi transaction COMMIT, InnoDB **không** ghi ngay các trang dữ liệu đã sửa xuống đĩa (chậm, vì phải ghi ngẫu nhiên nhiều vị trí) — mà chỉ ghi 1 bản ghi nhỏ, tuần tự (nhanh hơn nhiều) vào **Redo Log**, rồi trả lời "commit thành công" ngay. Các trang dữ liệu thật trong Buffer Pool được đồng bộ xuống đĩa **sau đó**, theo tiến trình nền (gọi là **checkpoint**). Nếu server crash giữa chừng, khi khởi động lại InnoDB **replay lại Redo Log** để khôi phục đúng trạng thái đã commit — đây là cách Durability được đảm bảo mà vẫn nhanh.

**c) Undo Log — hỗ trợ MVCC và ROLLBACK.** Khi 1 row bị UPDATE, InnoDB giữ lại phiên bản CŨ trong **Undo Log** — phục vụ 2 việc: (1) `ROLLBACK` — quay lại giá trị cũ nếu transaction hủy; (2) **MVCC** (Module 10, mục 7) — một transaction khác đang đọc ở isolation level REPEATABLE READ có thể vẫn nhìn thấy phiên bản CŨ của row (snapshot tại thời điểm bắt đầu), lấy từ Undo Log, dù transaction hiện tại đã sửa xong.

```
Transaction COMMIT
      │
      ▼
Ghi Redo Log (tuần tự, nhanh) ──► trả lời "OK" cho client NGAY
      │
      ▼ (nền, không chặn client)
Đồng bộ trang dữ liệu từ Buffer Pool xuống đĩa thật (checkpoint)
```

> **Liên hệ:** đây chính là ví dụ cụ thể nhất cho khái niệm WAL đã học ở Module 10 — nếu đã hiểu WAL ở mức khái niệm, đọc lại 3 cơ chế trên chỉ là gắn tên riêng (Redo Log, Undo Log, Buffer Pool) vào đúng vai trò đã biết.

### Khóa ở tầng row (Row-Level Locking)

InnoDB khóa ở mức **từng dòng (row)** bị ảnh hưởng, chứ không khóa nguyên bảng như MyISAM — đây là lý do InnoDB cho phép nhiều transaction ghi đồng thời vào các row khác nhau của cùng 1 bảng mà không chặn nhau, khác hẳn `synchronized` ở tầng ứng dụng (Module 05.1) vốn khóa nguyên đối tượng. Cơ chế khóa dòng cụ thể (`SELECT ... FOR UPDATE`, gap lock chống Phantom Read) đã được trình bày ở Module 10, mục 7.

---

## 3. Kiểu dữ liệu nâng cao của PostgreSQL

### JSONB — lưu trữ dữ liệu bán cấu trúc (semi-structured) NGAY TRONG cột SQL

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200),
    attributes JSONB -- lưu dữ liệu ĐỘNG, không cố định cấu trúc, ví dụ: {"color": "red", "size": "L", "material": "cotton"}
);

INSERT INTO products (id, name, attributes)
VALUES (1, 'Áo thun', '{"color": "red", "size": "L"}');

-- Truy vấn TRỰC TIẾP vào field bên trong JSON — PostgreSQL hỗ trợ TRUY VẤN & INDEX ngay trên JSONB!
SELECT * FROM products WHERE attributes->>'color' = 'red';
CREATE INDEX idx_products_attributes ON products USING GIN (attributes); -- Index CHUYÊN BIỆT cho JSONB, tăng tốc query
```

> **Ứng dụng thực tế:** cực kỳ hữu ích khi có dữ liệu **cấu trúc thay đổi tùy loại record** (ví dụ: mỗi loại sản phẩm có thuộc tính riêng — áo có "size/color", laptop có "RAM/CPU") mà không muốn tạo hàng chục cột `NULL` rải rác hoặc phải tạo bảng phụ phức tạp — JSONB cho phép **linh hoạt như NoSQL** nhưng **vẫn nằm trong RDBMS**, vẫn hưởng đầy đủ Transaction/ACID.

**`JSON` vs `JSONB` — khác nhau ở đâu?** `JSON` lưu **nguyên văn chuỗi text** đúng như nhập vào (giữ khoảng trắng, thứ tự key, key trùng lặp) — mỗi lần truy vấn phải parse lại. `JSONB` ("Binary JSON") lưu ở dạng **đã phân giải nhị phân**, mất thứ tự key gốc nhưng **truy vấn và index nhanh hơn nhiều** — vì vậy **JSONB gần như luôn là lựa chọn đúng** cho backend, `JSON` chỉ cần khi phải giữ nguyên văn bản gốc (ví dụ log request/response).

### Array — lưu mảng trực tiếp trong 1 cột

```sql
CREATE TABLE posts (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200),
    tags TEXT[] -- mảng chuỗi, ví dụ: {'java', 'spring', 'backend'}
);
SELECT * FROM posts WHERE 'java' = ANY(tags); -- tìm bài viết CÓ chứa tag "java"
```

### UUID — khóa chính không đoán được, phù hợp hệ thống phân tán

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- tự sinh UUID, không cần AUTO_INCREMENT tuần tự
    ...
);
```
> **Liên hệ thực tế:** UUID phù hợp hơn `AUTO_INCREMENT`/`SERIAL` khi hệ thống có **nhiều service/instance cùng tạo dữ liệu độc lập** — tránh xung đột ID khi gộp dữ liệu từ nhiều nguồn, và không để lộ thông tin nghiệp vụ qua ID tuần tự (ví dụ ID tăng dần lộ ra "công ty có bao nhiêu đơn hàng" cho đối thủ cạnh tranh). Đánh đổi (kích thước, phân mảnh B-Tree index) đã phân tích chi tiết ở Module 10, mục 10.

### ENUM — ràng buộc giá trị hợp lệ ngay ở tầng kiểu dữ liệu

```sql
CREATE TYPE order_status AS ENUM ('PENDING', 'PAID', 'SHIPPED', 'CANCELLED');

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    status order_status NOT NULL DEFAULT 'PENDING' -- CHỈ chấp nhận 4 giá trị đã định nghĩa, chèn giá trị khác báo lỗi ngay
);
```
> So với dùng `VARCHAR` + `CHECK (status IN (...))`, kiểu `ENUM` gọn hơn và PostgreSQL lưu trữ hiệu quả hơn (bên trong là số nguyên). Nhược điểm: thêm 1 giá trị mới vào ENUM sau này cần lệnh `ALTER TYPE ... ADD VALUE` — kém linh hoạt hơn `CHECK` một chút. Trong code Java/JPA, kiểu này ánh xạ tự nhiên với `enum` (sẽ gặp lại ở Module 11 với `@Enumerated`).

### Range Type — lưu 1 khoảng giá trị trong 1 cột

```sql
CREATE TABLE room_bookings (
    room_id BIGINT,
    during TSTZRANGE -- khoảng thời gian: [check-in, check-out)
);

INSERT INTO room_bookings VALUES (1, '[2026-01-10 14:00, 2026-01-12 12:00)');

-- Toán tử && kiểm tra 2 khoảng có GIAO NHAU không — cực mạnh cho bài toán đặt phòng/lịch
SELECT * FROM room_bookings WHERE room_id = 1 AND during && '[2026-01-11 00:00, 2026-01-11 23:59)';

-- Ràng buộc CHỐNG OVERLAP ngay ở tầng database — không cần tự kiểm tra bằng code!
ALTER TABLE room_bookings ADD CONSTRAINT no_overlap
    EXCLUDE USING GIST (room_id WITH =, during WITH &&);
```
> **Ứng dụng thực tế:** bài toán "đặt phòng/đặt lịch không được trùng khung giờ" thường bị lập trình viên tự viết logic kiểm tra overlap ở tầng Java (dễ có race condition khi nhiều request đồng thời — liên hệ khái niệm check-then-act ở Module 05.1) — PostgreSQL Range Type + `EXCLUDE CONSTRAINT` giải quyết bài toán này **ngay ở tầng database**, đảm bảo đúng đắn tuyệt đối kể cả dưới tải đồng thời cao.

---

## 4. NoSQL là gì — tổng quan các loại

**NoSQL** ("Not Only SQL") là nhóm database **không theo mô hình quan hệ (relational)** truyền thống — được sinh ra để giải quyết các bài toán mà RDBMS truyền thống **không tối ưu**: dữ liệu cực lớn (Big Data), cấu trúc linh hoạt/thay đổi liên tục, cần mở rộng theo chiều ngang (horizontal scaling) dễ dàng.

| Loại NoSQL | Mô hình dữ liệu | Ví dụ tiêu biểu | Phù hợp bài toán |
|---|---|---|---|
| **Document** | Lưu dữ liệu dạng "document" giống JSON | **MongoDB** | Dữ liệu có cấu trúc linh hoạt, lồng nhau tự nhiên (profile người dùng, catalog sản phẩm đa dạng) |
| **Key-Value** | Cặp key-value đơn giản, cực nhanh | **Redis**, Memcached | Cache, session, dữ liệu tra cứu cực nhanh theo key |
| **Column-Family** | Lưu theo cột thay vì hàng, tối ưu ghi/đọc số lượng cực lớn | Cassandra, HBase | Big Data, time-series data, hệ thống ghi log khổng lồ |
| **Graph** | Lưu dữ liệu dạng node-edge (đồ thị), tối ưu truy vấn quan hệ phức tạp | Neo4j | Mạng xã hội (bạn bè của bạn bè), hệ thống gợi ý (recommendation) |

> Trong phạm vi lộ trình Java Backend cơ bản đến trung cấp, **MongoDB** và **Redis** là 2 loại NoSQL phổ biến nhất, đáng học sâu nhất — sẽ được trình bày chi tiết ở mục 5, 6, 7.

---

## 5. MongoDB — Document Database

MongoDB lưu dữ liệu dưới dạng **document** (tương tự object JSON), nhóm các document tương tự nhau vào 1 **collection** (tương đương khái niệm "bảng" trong SQL, nhưng linh hoạt hơn nhiều).

### So sánh thuật ngữ SQL vs MongoDB

| SQL (RDBMS) | MongoDB |
|---|---|
| Database | Database |
| Table (bảng) | Collection |
| Row (dòng) | Document |
| Column (cột) | Field |
| Primary Key | `_id` (tự động sinh nếu không chỉ định) |

### Ví dụ Document

```javascript
// 1 document trong collection "users" — LƯU Ý: KHÔNG có schema CỐ ĐỊNH như bảng SQL!
{
  "_id": ObjectId("..."),
  "name": "Pho",
  "email": "pho@example.com",
  "address": {                    // NESTED OBJECT — không cần tách bảng riêng như SQL (không cần chuẩn hóa 3NF!)
    "city": "Da Nang",
    "country": "Vietnam"
  },
  "hobbies": ["reading", "coding"] // MẢNG trực tiếp trong document
}
```

### Thao tác CRUD cơ bản (cú pháp MongoDB Query Language)

```javascript
db.users.insertOne({ name: "An", email: "an@example.com" }); // Create

db.users.find({ "address.city": "Da Nang" });                  // Read — truy vấn TRỰC TIẾP vào field lồng nhau
db.users.findOne({ email: "pho@example.com" });

db.users.updateOne(                                             // Update
  { email: "pho@example.com" },
  { $set: { "address.city": "Ho Chi Minh" } }
);

db.users.deleteOne({ email: "an@example.com" });                // Delete
```

### Schema-less — điểm khác biệt CỐT LÕI so với SQL

```javascript
// 2 document TRONG CÙNG 1 collection "users" — CẤU TRÚC KHÁC NHAU HOÀN TOÀN, MongoDB vẫn CHO PHÉP!
{ "_id": 1, "name": "Pho", "email": "pho@example.com" }
{ "_id": 2, "name": "An", "email": "an@example.com", "phone": "0901234567", "vip": true } // có THÊM field, KHÔNG BÁO LỖI
```
> Đây là **con dao 2 lưỡi**: linh hoạt cực cao (không cần `ALTER TABLE` khi thay đổi cấu trúc dữ liệu như SQL), nhưng **mất đi sự đảm bảo tính toàn vẹn dữ liệu ở tầng database** mà SQL cung cấp sẵn (`NOT NULL`, `CHECK`, kiểu dữ liệu cố định) — trách nhiệm đảm bảo dữ liệu "đúng chuẩn" chuyển phần lớn sang **tầng ứng dụng (code Java)**. MongoDB có **JSON Schema Validation** tùy chọn để giảm bớt rủi ro này, nhưng ít được dùng nghiêm ngặt như ràng buộc SQL.

### Index trong MongoDB — cùng tư duy với SQL

Không có index, MongoDB phải **quét toàn bộ collection** (`COLLSCAN`) cho mỗi query — giống hệt Seq Scan đã học ở Module 10, mục 11.

```javascript
db.users.createIndex({ email: 1 });                    // Single-field index, 1 = tăng dần
db.users.createIndex({ "address.city": 1, name: 1 });   // Compound index — thứ tự trường QUAN TRỌNG,
                                                          // giống nguyên tắc Leftmost Prefix đã học ở Module 10
db.users.find({ email: "pho@example.com" }).explain();   // Tương đương EXPLAIN của SQL — xem query có dùng index không (IXSCAN hay COLLSCAN)
```
> **Liên hệ:** nguyên tắc chọn cột để đánh index (cardinality cao, cột hay dùng trong điều kiện lọc/sắp xếp) và cách đọc `explain()` **giống hệt tư duy** đã học kỹ với B-Tree Index ở Module 10, mục 8 và 11 — chỉ khác cú pháp khai báo.

### Aggregation Pipeline — tương đương GROUP BY/JOIN nâng cao của MongoDB

```javascript
// Tương đương: SELECT city, COUNT(*), AVG(age) FROM users WHERE age >= 18 GROUP BY city ORDER BY COUNT(*) DESC
db.users.aggregate([
  { $match: { age: { $gte: 18 } } },                       // giống WHERE
  { $group: { _id: "$address.city", total: { $sum: 1 }, avgAge: { $avg: "$age" } } }, // giống GROUP BY + aggregate function
  { $sort: { total: -1 } }                                 // giống ORDER BY ... DESC
]);
```
> Dữ liệu chảy qua từng **stage** (`$match` → `$group` → `$sort`) giống một pipeline xử lý tuần tự — tư duy tương tự Stream API (Module 03.2: `filter` → `collect` → `sorted`), chỉ khác là chạy trên server database thay vì trong JVM.

### Multi-Document Transaction & Replica Set/Sharding — kiến trúc phân tán

Từ bản 4.0+, MongoDB hỗ trợ **Multi-Document Transaction** (ACID xuyên nhiều document/collection) — nhưng để dùng được transaction, MongoDB **bắt buộc** phải chạy ở chế độ **Replica Set** (tối thiểu 1 primary + các secondary đồng bộ dữ liệu), không chạy được trên 1 node đơn lẻ (standalone). Khi dữ liệu quá lớn cho 1 Replica Set, MongoDB hỗ trợ **Sharding** — chia dữ liệu theo `shard key` ra nhiều cụm server khác nhau để mở rộng theo chiều ngang.

```
                    ┌─────────────┐
                    │   mongos     │  (router — nhận query, biết dữ liệu nằm ở shard nào)
                    └──────┬──────┘
           ┌────────────────┼────────────────┐
           ▼                ▼                ▼
     ┌───────────┐   ┌───────────┐   ┌───────────┐
     │  Shard 1   │   │  Shard 2   │   │  Shard 3   │   (mỗi shard là 1 Replica Set riêng)
     │(Replica Set)│   │(Replica Set)│   │(Replica Set)│
     └───────────┘   └───────────┘   └───────────┘
```
> Đây chính là ví dụ cụ thể cho khái niệm **horizontal scaling** — thay vì nâng cấp 1 server ngày càng mạnh hơn (vertical scaling, có giới hạn vật lý), hệ thống thêm NHIỀU server nhỏ hơn chạy song song. Tư duy này sẽ gặp lại xuyên suốt các module về kiến trúc phân tán phía sau lộ trình.

### Khi nào MongoDB phù hợp hơn SQL?

- Dữ liệu có cấu trúc **thay đổi thường xuyên**, khó định nghĩa schema cố định trước (catalog sản phẩm đa dạng loại, CMS nội dung linh hoạt).
- Dữ liệu **lồng nhau tự nhiên**, ít cần JOIN phức tạp qua nhiều bảng (profile người dùng đầy đủ thông tin trong 1 document).
- Cần **mở rộng theo chiều ngang (horizontal scaling)** dễ dàng cho khối lượng dữ liệu cực lớn (MongoDB hỗ trợ **sharding** như trên — phân tán dữ liệu qua nhiều server — dễ dàng hơn RDBMS truyền thống).

### Khi nào SQL vẫn tốt hơn MongoDB?

- Dữ liệu có **quan hệ phức tạp, cần JOIN nhiều bảng thường xuyên** (hệ thống tài chính, ERP).
- Cần **Transaction ACID mạnh** xuyên suốt nhiều bảng/document (dù MongoDB từ bản 4.0+ đã hỗ trợ Multi-Document Transaction, nhưng RDBMS vẫn là lựa chọn "mặc định an toàn hơn" cho nghiệp vụ tài chính nhạy cảm, và không đòi hỏi bắt buộc chạy Replica Set như MongoDB).
- Cần **tính toàn vẹn dữ liệu chặt chẽ** ở tầng database (ràng buộc khóa ngoại, kiểu dữ liệu cố định).

---

## 6. Redis — Key-Value Store

**Redis** (REmote DIctionary Server) là database **lưu trữ trong bộ nhớ (in-memory)** — cực kỳ nhanh (vì đọc/ghi trực tiếp RAM, không phải đĩa cứng như đa số RDBMS/MongoDB), thường dùng làm **cache**, không phải database chính lưu trữ dữ liệu dài hạn.

### Thao tác cơ bản

```
SET user:1:name "Pho"        # lưu 1 key-value đơn giản
GET user:1:name               # trả về "Pho"
DEL user:1:name                # xóa key

SET session:abc123 "user_id=1" EX 3600  # "EX 3600" — TỰ ĐỘNG hết hạn (expire) sau 3600 giây — cực kỳ hữu ích cho session/cache
TTL session:abc123             # xem còn bao nhiêu giây nữa key này hết hạn

EXISTS user:1:name             # kiểm tra key có tồn tại không (trả về 1/0)
INCR page:views                # TĂNG giá trị số nguyên lên 1 — ATOMIC, an toàn đa luồng/đa client (liên hệ AtomicInteger, Module 05.2!)
```

### Vì sao Redis nhanh đến vậy?

1. **Lưu trữ hoàn toàn trong RAM** (không phải đĩa cứng) — RAM nhanh hơn đĩa cứng/SSD hàng nghìn lần về độ trễ truy cập.
2. **Đơn luồng (single-threaded)** cho việc xử lý lệnh (dù có thể có thread phụ cho I/O ở phiên bản mới) — tránh hoàn toàn overhead của việc quản lý lock/đồng bộ hóa đa luồng (liên hệ Module 05.1 — mọi lệnh Redis tự động là "atomic" vì chỉ 1 luồng xử lý lệnh tại 1 thời điểm, không có Race Condition ở tầng Redis).
3. Cấu trúc dữ liệu được **tối ưu hóa đặc biệt** cho từng loại thao tác (mục 7).

### Redis vẫn "bền" được không nếu chỉ lưu trong RAM? — Persistence

RAM mất dữ liệu khi mất điện/restart — nhưng Redis vẫn có 2 cơ chế **ghi bền xuống đĩa** để không mất trắng dữ liệu khi cần:

| Cơ chế | Cách hoạt động | Đánh đổi |
|---|---|---|
| **RDB** (snapshot) | Định kỳ chụp **toàn bộ** dữ liệu tại 1 thời điểm, ghi ra 1 file nhị phân | File nhỏ gọn, phục hồi nhanh, nhưng **mất dữ liệu ghi giữa 2 lần snapshot** nếu crash |
| **AOF** (Append Only File) | Ghi lại **mọi lệnh ghi** (SET, INCR...) tuần tự vào file log, giống ý tưởng WAL/Redo Log của InnoDB (mục 2) | Mất ít dữ liệu hơn RDB (tùy tần suất `fsync`), nhưng file lớn hơn, phục hồi chậm hơn |

> Trong thực tế, nhiều hệ thống bật **cả 2** (RDB cho backup nhanh gọn định kỳ + AOF cho độ an toàn cao) hoặc chấp nhận **mất một phần dữ liệu cache khi crash** — vì Redis thường chỉ giữ dữ liệu **có thể tính lại được** từ nguồn chính (PostgreSQL/MongoDB), không phải nguồn sự thật duy nhất (single source of truth).

### Eviction Policy — khi RAM đầy thì sao?

RAM có giới hạn (`maxmemory`) — khi đầy, Redis phải **loại bỏ (evict)** bớt key theo 1 trong các chính sách cấu hình được, phổ biến nhất:

| Policy | Ý nghĩa |
|---|---|
| `noeviction` | Từ chối ghi thêm, báo lỗi (mặc định) — nguy hiểm cho hệ thống dùng Redis làm cache chính |
| `allkeys-lru` | Loại bỏ key **ít được dùng gần đây nhất** (Least Recently Used) trong TOÀN BỘ key — phổ biến nhất cho cache thuần túy |
| `volatile-lru` | Chỉ loại bỏ trong nhóm key **có đặt TTL** — an toàn hơn nếu Redis vừa dùng cho cache vừa lưu dữ liệu không muốn mất |
| `allkeys-random` / `volatile-ttl` | Loại ngẫu nhiên / loại key sắp hết hạn sớm nhất |

> **Liên hệ:** `allkeys-lru` cùng ý tưởng thuật toán LRU đã gặp khi học cấu trúc dữ liệu — Redis hiện thực hóa đúng thuật toán đó ở tầng hệ thống cache thực tế.

### Ứng dụng phổ biến nhất của Redis trong backend

| Ứng dụng | Mô tả |
|---|---|
| **Caching** | Lưu kết quả tính toán/truy vấn database TỐN KÉM để tránh tính lại — liên hệ trực tiếp `@Cacheable` sẽ học ở các module sau về Caching |
| **Session Storage** | Lưu session người dùng đăng nhập — đặc biệt quan trọng khi có **NHIỀU instance** backend chạy song song, cần nơi lưu session **DÙNG CHUNG** thay vì lưu trong bộ nhớ của TỪNG instance riêng lẻ |
| **Rate Limiting** | Đếm số request từ 1 user/IP trong khoảng thời gian, chặn nếu vượt ngưỡng — dùng `INCR` + `EXPIRE` |
| **Distributed Lock** | Khóa phân tán — mở rộng khái niệm `synchronized` (Module 05.1) ra **NHIỀU instance backend khác nhau** cùng truy cập 1 tài nguyên chung (ví dụ: đảm bảo chỉ 1 instance xử lý 1 tác vụ định kỳ cụ thể tại 1 thời điểm) |
| **Pub/Sub Messaging** | Cơ chế publish/subscribe đơn giản, dù đơn giản hơn nhiều so với các hệ thống Message Queue chuyên dụng (Kafka/RabbitMQ) |

### Redis Cluster/Replication — mở rộng khi 1 server không đủ

Giống MongoDB, Redis production thường không chạy đơn lẻ: **Replication** (1 master ghi, nhiều replica đọc — tăng khả năng đọc và chịu lỗi) và **Redis Cluster** (chia key ra nhiều node theo hash slot — mở rộng dung lượng/throughput theo chiều ngang) là 2 cơ chế phổ biến khi hệ thống lớn dần, tư duy hoàn toàn tương đồng với Replica Set/Sharding của MongoDB đã nói ở mục 5.

---

## 7. Redis — cấu trúc dữ liệu nâng cao

Redis không chỉ lưu String đơn giản — hỗ trợ nhiều cấu trúc dữ liệu, mỗi loại tối ưu cho 1 nhóm bài toán cụ thể (liên hệ trực tiếp Module 03.1 — Collections Framework, cùng tư duy "chọn đúng cấu trúc dữ liệu cho đúng bài toán"):

```
LPUSH queue:tasks "task1"     # List — thêm vào ĐẦU danh sách, tương tự LinkedList (Module 03.1)
RPOP queue:tasks               # lấy ra từ CUỐI — kết hợp LPUSH+RPOP tạo thành QUEUE (FIFO)

SADD tags:post123 "java" "spring"  # Set — tương tự HashSet, tự động loại trùng lặp
SISMEMBER tags:post123 "java"       # kiểm tra "java" có trong Set không — O(1)

ZADD leaderboard 100 "player1"     # Sorted Set — tương tự TreeSet, TỰ ĐỘNG sắp xếp theo "score" (100)
ZADD leaderboard 250 "player2"
ZRANGE leaderboard 0 -1 WITHSCORES  # lấy TOÀN BỘ, đã SẮP XẾP theo score — cực kỳ phù hợp cho BẢNG XẾP HẠNG (leaderboard) game/ứng dụng

HSET user:1 name "Pho" age "22"    # Hash — tương tự HashMap, lưu nhiều field trong 1 key
HGET user:1 name                     # lấy 1 field cụ thể
HGETALL user:1                       # lấy TOÀN BỘ field
```

### Hai cấu trúc chuyên biệt đáng biết thêm

```
PFADD visitors:2026-01-01 "user1" "user2" "user3"  # HyperLogLog — ước lượng SỐ LƯỢNG PHẦN TỬ DUY NHẤT
PFCOUNT visitors:2026-01-01                          # cực tiết kiệm bộ nhớ (~12KB dù có HÀNG TỶ phần tử), sai số nhỏ (~0.81%)
                                                       # phù hợp bài toán "đếm unique visitor mỗi ngày" mà không cần chính xác tuyệt đối

SETBIT user:1:login_days 15 1   # Bitmap — mỗi bit đại diện 1 trạng thái (ví dụ: ngày thứ 15 trong tháng có đăng nhập không)
BITCOUNT user:1:login_days        # đếm số bit = 1 — cực tiết kiệm bộ nhớ cho dữ liệu dạng cờ hiệu (flag) theo thời gian
```
> **HyperLogLog** đánh đổi độ chính xác tuyệt đối lấy hiệu quả bộ nhớ — đúng tinh thần "trade-off" đã gặp xuyên suốt các module trước (ví dụ CAP Theorem ở mục 8), chỉ khác là đánh đổi ở phạm vi 1 cấu trúc dữ liệu cụ thể thay vì kiến trúc toàn hệ thống.

> **Liên hệ trực tiếp:** nếu đã nắm chắc Module 03.1 (Collections Framework), việc học các cấu trúc dữ liệu của Redis sẽ **cực kỳ nhanh** — vì bản chất tư duy chọn cấu trúc phù hợp bài toán **giống hệt nhau**, chỉ khác là Redis chạy trên **bộ nhớ chia sẻ giữa nhiều tiến trình/server**, thay vì chỉ trong 1 JVM process như Collection thông thường.

---

## 8. SQL vs NoSQL — CAP Theorem ở mức khái niệm

**CAP Theorem** (định lý CAP) phát biểu: 1 hệ thống dữ liệu **phân tán (distributed)** chỉ có thể đảm bảo **TỐI ĐA 2 trong 3** tính chất sau **cùng lúc**, không thể có cả 3:

| Chữ cái | Ý nghĩa |
|---|---|
| **C** — Consistency | Mọi node trong hệ thống LUÔN thấy **CÙNG 1 dữ liệu MỚI NHẤT** tại cùng 1 thời điểm |
| **A** — Availability | Hệ thống LUÔN phản hồi được request (dù có thể dữ liệu chưa hoàn toàn "mới nhất") |
| **P** — Partition Tolerance | Hệ thống VẪN hoạt động được dù mạng giữa các node bị "đứt gãy" (network partition) |

> Trong thực tế, **Partition Tolerance gần như BẮT BUỘC** phải có (mạng luôn có khả năng gặp sự cố trong hệ thống phân tán thực tế) — nên lựa chọn thực sự thường là đánh đổi giữa **Consistency** và **Availability**:

| Xu hướng | Ưu tiên | Ví dụ |
|---|---|---|
| **CP** (Consistency + Partition Tolerance) | Dữ liệu LUÔN chính xác, chấp nhận **từ chối phục vụ** nếu không đảm bảo được tính nhất quán | Đa số RDBMS truyền thống (MySQL, PostgreSQL) khi cấu hình chuẩn |
| **AP** (Availability + Partition Tolerance) | LUÔN phản hồi được, chấp nhận dữ liệu có thể **"cũ" trong khoảnh khắc ngắn** (Eventual Consistency — cuối cùng rồi cũng nhất quán) | Nhiều hệ thống NoSQL (Cassandra, DynamoDB theo cấu hình mặc định) |

### Mở rộng: PACELC — CAP chưa nói hết câu chuyện

CAP Theorem chỉ mô tả điều gì xảy ra **khi có sự cố mạng (partition)** — nhưng phần lớn thời gian hệ thống chạy **bình thường, không có sự cố**. **PACELC** (đọc "pace-elk") bổ sung vế còn thiếu:

> **P**artition xảy ra → đánh đổi giữa **A**vailability và **C**onsistency (chính là CAP); **E**lse (bình thường, không sự cố) → đánh đổi giữa **L**atency (độ trễ phản hồi) và **C**onsistency.

Ví dụ: ngay cả khi mạng ổn định hoàn toàn, một hệ thống muốn đảm bảo Consistency tuyệt đối (chờ ghi xong ở TẤT CẢ các replica trước khi trả lời) sẽ có **độ trễ cao hơn** hệ thống chấp nhận trả lời ngay sau khi ghi ở 1 node rồi đồng bộ replica sau (Eventual Consistency, độ trễ thấp hơn). Đây là lý do nhiều hệ thống NoSQL vẫn "chậm nhất quán" ngay cả khi mạng hoàn toàn khỏe mạnh — không chỉ khi có sự cố.

> **Không cần đi sâu quá vào lý thuyết CAP/PACELC ở giai đoạn học Backend cơ bản này** — chỉ cần hiểu **ý tưởng đánh đổi cốt lõi**: hệ thống SQL truyền thống thường ưu tiên **tính đúng đắn tuyệt đối** của dữ liệu (đặc biệt phù hợp nghiệp vụ tài chính), trong khi nhiều hệ thống NoSQL sẵn sàng "linh hoạt" hơn về tính nhất quán tức thời để đổi lấy khả năng phục vụ liên tục, độ trễ thấp và mở rộng dễ dàng hơn ở quy mô cực lớn.

---

## 9. Khi nào chọn SQL, khi nào chọn NoSQL — cây quyết định thực tế

```
Dữ liệu có QUAN HỆ RÕ RÀNG, cần JOIN nhiều bảng, cần TRANSACTION chặt chẽ?
(ví dụ: hệ thống ngân hàng, ERP, e-commerce với đơn hàng-sản phẩm-khách hàng liên kết chặt)
    │
   Có ──────► DÙNG SQL (MySQL/PostgreSQL) — vẫn là lựa chọn MẶC ĐỊNH AN TOÀN cho đa số backend nghiệp vụ
    │
   Không, dữ liệu LINH HOẠT, cấu trúc thay đổi liên tục, ít cần JOIN?
    │
   Có ──────► CÂN NHẮC MongoDB (Document DB)

Cần đọc/ghi CỰC NHANH, dữ liệu TẠM THỜI (cache, session, rate limiting)?
    │
   Có ──────► DÙNG Redis (Key-Value Store), thường dùng SONG SONG với SQL/MongoDB, không thay thế hoàn toàn
```

> **Lời khuyên thực tế quan trọng nhất cho người mới:** **KHÔNG có công thức "NoSQL luôn nhanh hơn/hiện đại hơn SQL"** — đây là hiểu lầm phổ biến. Với **90%+ dự án backend vừa và nhỏ** (bao gồm đa số capstone/dự án thực tập), **PostgreSQL/MySQL đã là lựa chọn hoàn toàn đủ tốt và an toàn**. NoSQL chỉ thực sự cần thiết khi có **bài toán CỤ THỂ** mà SQL không giải quyết tốt (cấu trúc dữ liệu cực kỳ linh hoạt, quy mô cực lớn cần horizontal scaling, cần cache cực nhanh) — không nên chọn NoSQL chỉ vì "nghe có vẻ hiện đại hơn".

---

## 10. Mô hình Polyglot Persistence — dùng NHIỀU loại database cùng lúc

Trong thực tế, hệ thống backend **quy mô lớn hiếm khi chỉ dùng 1 loại database duy nhất** — mô hình phổ biến là **"Polyglot Persistence"**: chọn đúng loại database cho đúng loại dữ liệu, trong cùng 1 hệ thống.

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
└─────┬─────────────────┬──────────────────┬───────────────┘
      │                 │                  │
      ▼                 ▼                  ▼
┌───────────┐   ┌──────────────┐   ┌────────────────┐
│PostgreSQL  │   │  Redis        │   │  MongoDB         │
│(dữ liệu    │   │  (cache,      │   │  (log hoạt động, │
│ nghiệp vụ  │   │   session,    │   │   dữ liệu phân   │
│ chính:     │   │   rate limit) │   │   tích linh hoạt) │
│ users,     │   │               │   │                   │
│ orders...) │   │               │   │                   │
└───────────┘   └──────────────┘   └────────────────┘
```

> **Đây chính là kiến trúc thực tế** của rất nhiều hệ thống e-commerce/backend hiện đại: dữ liệu nghiệp vụ **cốt lõi** (user, order, payment — cần Transaction chặt chẽ) nằm ở **PostgreSQL**; dữ liệu **tạm thời cần truy cập cực nhanh** (session, cache kết quả API) nằm ở **Redis**; dữ liệu **log/phân tích hành vi người dùng** (ít cần transaction, cấu trúc linh hoạt, khối lượng lớn) có thể nằm ở **MongoDB** hoặc hệ thống Big Data chuyên biệt hơn. Hiểu được mô hình này là bước chuẩn bị tư duy quan trọng cho các module sau về Caching, Messaging và kiến trúc Microservices.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| MySQL vs PostgreSQL | Cả 2 đều tốt; PostgreSQL có nhiều kiểu dữ liệu nâng cao hơn (JSONB, Array, UUID, ENUM, Range Type), MySQL phổ biến truyền thống hơn |
| InnoDB | Storage Engine MẶC ĐỊNH của MySQL hiện đại — LUÔN nên dùng vì hỗ trợ đầy đủ Transaction/ACID |
| Buffer Pool / Redo Log / Undo Log | Cache RAM + WAL để commit nhanh mà vẫn Durable + hỗ trợ MVCC/Rollback — cơ chế cụ thể hiện thực hóa WAL đã học ở Module 10 |
| JSONB (PostgreSQL) | Kết hợp linh hoạt như NoSQL NHƯNG vẫn trong RDBMS, vẫn có Transaction đầy đủ; `JSONB` nhanh hơn `JSON` vì lưu dạng nhị phân đã phân giải |
| Range Type + EXCLUDE | Chống overlap lịch/đặt phòng ngay ở tầng database, không cần tự viết logic dễ race condition ở tầng ứng dụng |
| MongoDB | Document DB, schema-less, phù hợp dữ liệu lồng nhau/cấu trúc linh hoạt; có Index + Aggregation Pipeline + Multi-Document Transaction (cần Replica Set); mở rộng bằng Sharding |
| Redis | In-memory Key-Value Store, cực nhanh, dùng cho cache/session/rate limiting — KHÔNG phải database chính lưu trữ lâu dài; bền hóa qua RDB/AOF; loại bớt dữ liệu qua eviction policy (LRU) khi đầy RAM |
| Redis cấu trúc nâng cao | List/Set/Sorted Set/Hash — tư duy giống Collections Framework (Module 03.1); HyperLogLog/Bitmap cho bài toán đếm/cờ hiệu tiết kiệm bộ nhớ |
| CAP Theorem | Chỉ đảm bảo 2/3 (Consistency, Availability, Partition Tolerance) trong hệ thống phân tán — SQL thường ưu tiên C, nhiều NoSQL ưu tiên A |
| PACELC | Mở rộng CAP: kể cả KHÔNG có sự cố mạng, vẫn phải đánh đổi giữa Latency và Consistency |
| Khi nào chọn SQL | Mặc định AN TOÀN cho đa số bài toán — quan hệ rõ ràng, cần Transaction chặt |
| Khi nào chọn NoSQL | Bài toán CỤ THỂ: cấu trúc linh hoạt cực cao (MongoDB), cần tốc độ cực nhanh cho dữ liệu tạm (Redis) |
| Polyglot Persistence | Hệ thống lớn thường dùng KẾT HỢP nhiều loại database, mỗi loại cho đúng mục đích |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Nhận định "MySQL không hỗ trợ Transaction" đúng hay sai? Giải thích chính xác.

**Câu 2.** Cho bài toán: hệ thống quản lý catalog sản phẩm thương mại điện tử, mỗi LOẠI sản phẩm (điện thoại, quần áo, sách) có bộ thuộc tính HOÀN TOÀN KHÁC NHAU (điện thoại có RAM/màn hình, quần áo có size/màu). Nên dùng RDBMS truyền thống với nhiều cột NULL, hay JSONB (PostgreSQL), hay MongoDB? Phân tích ưu nhược từng lựa chọn.

**Câu 3.** Vì sao Redis được coi là "cực kỳ nhanh" so với PostgreSQL/MongoDB? Nêu ít nhất 2 lý do kỹ thuật.

**Câu 4.** Trong mô hình CAP Theorem, giải thích tại sao Partition Tolerance thường được coi là "bắt buộc phải có" trong hệ thống phân tán thực tế, khiến lựa chọn thực sự chỉ còn giữa Consistency và Availability.

**Câu 5.** 1 bạn sinh viên nói: "Em sẽ dùng MongoDB cho toàn bộ đồ án capstone vì nó hiện đại hơn SQL". Nhận định này có vấn đề gì? Đưa ra góc nhìn phản biện hợp lý.

**Câu 6.** Redis lưu dữ liệu trong RAM — vậy vì sao vẫn có thể coi Redis "không mất trắng dữ liệu hoàn toàn" khi server restart? Giải thích 2 cơ chế liên quan.

**Câu 7.** Giải thích cơ chế Redo Log của InnoDB giúp transaction commit nhanh như thế nào, dù dữ liệu vẫn phải đảm bảo Durability (ghi bền xuống đĩa).

---

### Phần B — Bài tập thực hành (viết code/query, không cần chạy thật nếu chưa có môi trường)

**Bài 1 — Thiết kế schema PostgreSQL tận dụng JSONB.**
Thiết kế bảng `products` cho hệ thống e-commerce đa dạng loại sản phẩm (đã mô tả ở Câu 2 Phần A) — dùng cột cố định cho thông tin CHUNG (`id`, `name`, `price`, `category`), và cột `JSONB` cho thuộc tính RIÊNG từng loại. Viết 2-3 câu `INSERT` mẫu cho các loại sản phẩm khác nhau, và 1 câu `SELECT` truy vấn theo thuộc tính bên trong JSONB (ví dụ tìm điện thoại có RAM >= 8GB).

**Bài 2 — Thiết kế Document MongoDB cho hệ thống Blog.**
So sánh với thiết kế SQL chuẩn hóa đã làm ở Module 10 (Bài 3 Phần B — hệ thống blog với `users`, `posts`, `tags`, bảng trung gian `post_tags`) — thiết kế LẠI theo phong cách MongoDB: viết ra 1 document mẫu cho `posts` collection, **nhúng (embed)** trực tiếp thông tin tác giả và danh sách tag NGAY TRONG document (không cần bảng trung gian/JOIN). Phân tích ngắn gọn: cách thiết kế này có ưu điểm gì (tốc độ đọc) và nhược điểm gì (dữ liệu tác giả bị LẶP LẠI ở nhiều bài viết, khó đồng bộ khi tác giả đổi tên).

**Bài 3 — Thiết kế chiến lược Cache bằng Redis.**
Cho bài toán: API `GET /api/products/{id}` được gọi RẤT NHIỀU LẦN, nhưng dữ liệu sản phẩm ít khi thay đổi. Mô tả (bằng lời + pseudo-code) chiến lược cache: (a) khi có request, kiểm tra Redis trước bằng key nào; (b) nếu có (cache hit) thì làm gì; (c) nếu không có (cache miss) thì làm gì, và cần set thời gian hết hạn (TTL) bao lâu là hợp lý, giải thích lý do; (d) khi sản phẩm được CẬP NHẬT (`PUT /api/products/{id}`), cần làm gì với cache để tránh trả về dữ liệu CŨ (gợi ý: tìm hiểu khái niệm "Cache Invalidation"); (e) nếu Redis dùng chính sách `allkeys-lru` và RAM gần đầy, điều gì có thể xảy ra với các key cache ít được truy cập?

**Bài 4 — Redis Sorted Set cho bảng xếp hạng.**
Mô tả bằng lệnh Redis (giống cú pháp ở mục 7) cách xây dựng 1 bảng xếp hạng (leaderboard) cho hệ thống game/quiz: mỗi khi người chơi ghi điểm, cập nhật điểm số của họ; viết lệnh lấy TOP 10 người chơi có điểm cao nhất; viết lệnh lấy THỨ HẠNG (rank) cụ thể của 1 người chơi bất kỳ trong bảng xếp hạng (gợi ý: tìm hiểu thêm lệnh `ZREVRANK`).

**Bài 5 — Bài toán tổng hợp: thiết kế Polyglot Persistence cho hệ thống Flash-Sale (liên hệ trực tiếp capstone).**
Dựa trên hiểu biết về capstone "High-Concurrency Event Ticketing & Flash-Sale Engine" của bạn, đề xuất kiến trúc Polyglot Persistence phù hợp: (a) dữ liệu nào (vé, đơn hàng, thanh toán, user) nên nằm ở PostgreSQL/MySQL và tại sao; (b) Redis nên đóng vai trò gì trong việc giải quyết bài toán **oversold** (bán vượt số lượng vé — đã mô phỏng ở Module 05.1 và 05.2 bằng `synchronized`/`AtomicInteger` ở tầng ứng dụng, giờ hãy suy nghĩ thêm về giải pháp ở TẦNG DATABASE/CACHE cho hệ thống có NHIỀU instance backend chạy song song — liên hệ khái niệm Distributed Lock đã nhắc ở mục 6); (c) có cần MongoDB trong hệ thống này không, cho trường hợp dữ liệu nào (nếu có)?

**Bài 6 — Chống overlap bằng PostgreSQL Range Type.**
Cho bài toán đặt phòng họp công ty (1 phòng không được đặt trùng khung giờ). Viết schema dùng `TSTZRANGE` và `EXCLUDE CONSTRAINT` như ví dụ ở mục 3 để đảm bảo KHÔNG THỂ chèn 2 booking trùng giờ cho cùng 1 phòng — kể cả khi 2 request đến gần như đồng thời. So sánh ngắn gọn với cách làm "tự kiểm tra overlap bằng code Java trước khi INSERT" — cách nào an toàn hơn dưới tải đồng thời cao, tại sao?

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Sai, nếu nói tuyệt đối** — MySQL **CÓ** hỗ trợ Transaction đầy đủ khi dùng Storage Engine **InnoDB** (mặc định từ MySQL 5.5+). Chỉ khi dùng storage engine cũ như MyISAM thì mới KHÔNG hỗ trợ. Vì InnoDB là mặc định hiện nay, thực tế hầu hết ứng dụng MySQL hiện đại ĐỀU có Transaction đầy đủ.
2. Không có đáp án "đúng tuyệt đối" — đây là bài toán đánh đổi thực tế: RDBMS với nhiều cột NULL (Anti-pattern "Sparse Table") gây lãng phí không gian và khó mở rộng khi có loại sản phẩm mới; **JSONB trong PostgreSQL** là lựa chọn CÂN BẰNG tốt — vừa linh hoạt cho thuộc tính riêng, vừa giữ được Transaction/ACID và các ràng buộc cho dữ liệu CHUNG (`price`, `category`); **MongoDB thuần túy** phù hợp nếu TOÀN BỘ hệ thống đã có xu hướng dữ liệu linh hoạt, không chỉ riêng bảng sản phẩm, và không cần JOIN phức tạp với các bảng nghiệp vụ khác (orders, payments) — với hệ thống e-commerce có nhiều nghiệp vụ LIÊN QUAN CHẶT (đơn hàng, thanh toán, tồn kho), JSONB trong PostgreSQL thường là lựa chọn thực tế hợp lý hơn MongoDB thuần túy.
3. (a) Redis lưu **HOÀN TOÀN trong RAM**, không phải đĩa cứng/SSD như đa số RDBMS/MongoDB (dù có thể ghi backup xuống đĩa định kỳ) — RAM nhanh hơn đĩa cứng ở độ trễ truy cập hàng nghìn lần; (b) Redis xử lý lệnh theo cơ chế **đơn luồng**, loại bỏ hoàn toàn overhead của việc quản lý khóa/đồng bộ hóa phức tạp giữa nhiều luồng như các database hỗ trợ đa luồng đầy đủ.
4. Vì trong hệ thống phân tán thực tế (nhiều server, nhiều trung tâm dữ liệu), sự cố mạng (network partition — mất kết nối tạm thời giữa các node) là điều **CHẮC CHẮN SẼ XẢY RA** ở 1 thời điểm nào đó (do lỗi phần cứng, lỗi cấu hình, tắc nghẽn mạng...) — 1 hệ thống thiết kế mà "giả định mạng luôn ổn định 100%" là thiết kế **KHÔNG THỰC TẾ**. Vì vậy, câu hỏi thực sự không phải "có nên chấp nhận Partition Tolerance không" (bắt buộc phải chấp nhận) mà là "khi network partition XẢY RA, hệ thống nên ưu tiên trả lời đúng ngay cả khi phải chờ đợi (Consistency) hay trả lời ngay lập tức dù có thể chưa hoàn toàn cập nhật mới nhất (Availability)".
5. Vấn đề: "hiện đại hơn" **không phải tiêu chí kỹ thuật hợp lý** để chọn công nghệ — cần dựa trên **BÀI TOÁN CỤ THỂ**. Nếu đồ án capstone có dữ liệu quan hệ rõ ràng (user-order-product, hoặc user-course-enrollment tùy đề tài), cần Transaction chặt chẽ, việc ép dùng MongoDB có thể khiến việc triển khai các nghiệp vụ liên quan nhiều bảng trở nên **PHỨC TẠP HƠN** (phải tự viết logic đảm bảo tính nhất quán ở tầng ứng dụng, thay vì để RDBMS lo qua khóa ngoại + Transaction có sẵn) — đây chính xác là tình huống "dùng công nghệ vì nghe hay, không phải vì phù hợp bài toán", nên cân nhắc lại dựa trên đặc điểm THỰC TẾ của dữ liệu trong đồ án, không phải theo cảm tính "công nghệ mới thì tốt hơn".
6. Redis có 2 cơ chế bền hóa xuống đĩa: **RDB** — định kỳ chụp snapshot toàn bộ dữ liệu ra 1 file nhị phân (phục hồi nhanh, nhưng mất phần dữ liệu ghi giữa 2 lần snapshot); **AOF** — ghi lại tuần tự MỌI lệnh ghi vào file log giống cơ chế Redo Log/WAL (mất ít dữ liệu hơn nhưng phục hồi chậm hơn). Nhiều hệ thống bật cả 2, hoặc chấp nhận mất một phần dữ liệu cache vì Redis thường chỉ lưu dữ liệu có thể TÍNH LẠI ĐƯỢC từ nguồn chính (PostgreSQL/MongoDB), không phải nguồn sự thật duy nhất.
7. Khi transaction COMMIT, InnoDB không ghi ngay các trang dữ liệu đã sửa (nằm rải rác trong Buffer Pool) xuống đĩa — việc này chậm vì phải ghi ngẫu nhiên nhiều vị trí trên đĩa. Thay vào đó, InnoDB chỉ ghi 1 bản ghi nhỏ, TUẦN TỰ (nhanh hơn ghi ngẫu nhiên rất nhiều) vào Redo Log, rồi trả lời "commit thành công" ngay lập tức. Các trang dữ liệu thật được đồng bộ xuống đĩa SAU ĐÓ, ở tiến trình nền (checkpoint), không chặn client. Nếu server crash trước khi checkpoint kịp chạy, khi khởi động lại InnoDB sẽ REPLAY lại Redo Log để khôi phục đúng trạng thái đã commit — vừa đảm bảo Durability, vừa không phải trả giá về tốc độ cho mỗi lần commit.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Ví dụ query: `SELECT * FROM products WHERE category = 'phone' AND (attributes->>'ram_gb')::int >= 8;` (cần ép kiểu `::int` vì giá trị trong JSONB mặc định là kiểu text/JSON, không phải số nguyên trực tiếp).
- **Bài 2:** Đây là minh họa trực quan cho khái niệm **Embedding vs Referencing** trong thiết kế MongoDB — Embedding (nhúng trực tiếp) tối ưu tốc độ ĐỌC (không cần JOIN/lookup), nhưng đánh đổi bằng khả năng dữ liệu bị **trùng lặp và khó đồng bộ** khi thông tin gốc (tên tác giả) thay đổi — đây chính là sự đánh đổi ngược lại hoàn toàn với triết lý chuẩn hóa 3NF đã học kỹ ở Module 10, thể hiện rõ MongoDB và SQL có TRIẾT LÝ THIẾT KẾ khác nhau căn bản, không chỉ khác cú pháp.
- **Bài 3:** TTL hợp lý cho dữ liệu sản phẩm ít thay đổi có thể là vài phút đến vài giờ (tùy tần suất cập nhật thực tế của nghiệp vụ) — không nên quá ngắn (mất tác dụng cache) hay quá dài (dữ liệu cũ tồn tại lâu nếu quên invalidate). Phần (d) — Cache Invalidation: khi `PUT /api/products/{id}` thành công, cần **XÓA (DEL)** key cache tương ứng trong Redis NGAY LẬP TỨC (thay vì chờ TTL tự hết hạn) — đảm bảo lần đọc TIẾP THEO sẽ là "cache miss", buộc phải đọc lại dữ liệu MỚI từ database chính rồi mới cache lại. Phần (e): với `allkeys-lru`, các key sản phẩm ít được truy cập gần đây sẽ bị **loại bỏ (evict)** khi RAM gần đầy để nhường chỗ cho key mới/hay dùng hơn — lần truy vấn tiếp theo tới sản phẩm đó sẽ là cache miss, phải đọc lại từ database; đây là hành vi BÌNH THƯỜNG và chấp nhận được của cache (khác với mất dữ liệu thật — vì dữ liệu gốc vẫn còn nguyên trong PostgreSQL/MongoDB).
- **Bài 4:** Ví dụ lệnh: `ZADD game:leaderboard 1500 "player_pho"`; lấy TOP 10: `ZREVRANGE game:leaderboard 0 9 WITHSCORES` (REVRANGE vì mặc định ZRANGE sắp XĂNG DẦN, cần REV để lấy điểm CAO nhất trước); lấy rank cụ thể: `ZREVRANK game:leaderboard "player_pho"`.
- **Bài 5:** Đây là bài tập **tổng hợp và mang tính định hướng kiến trúc quan trọng nhất** của module — (a) toàn bộ dữ liệu nghiệp vụ CỐT LÕI (vé, đơn hàng, thanh toán, user) nên nằm ở PostgreSQL/MySQL vì cần Transaction chặt chẽ, tránh oversold/mất tiền — đúng như đã phân tích ở Module 10; (b) Redis có thể đóng vai trò **Distributed Lock** hoặc dùng lệnh `DECR`/cơ chế atomic của Redis để kiểm tra-và-giảm số lượng vé còn lại ở TẦNG CACHE TRƯỚC KHI chạm đến database — giúp giảm tải cực lớn cho database chính trong thời điểm traffic đỉnh điểm của flash-sale (hàng chục nghìn request cùng lúc tranh mua vài trăm vé), đồng thời đảm bảo tính đúng đắn NGAY CẢ KHI có NHIỀU instance backend chạy song song (điều mà `AtomicInteger` ở Module 05.2 KHÔNG giải quyết được, vì `AtomicInteger` chỉ an toàn trong PHẠM VI 1 JVM process/instance duy nhất); (c) MongoDB có thể phù hợp cho dữ liệu **log hành vi người dùng** (ai đã xem trang vé nào, thời điểm nào, để phân tích sau sự kiện) — dữ liệu này KHÔNG cần Transaction chặt, khối lượng có thể rất lớn, và không ảnh hưởng trực tiếp đến tính đúng đắn của nghiệp vụ bán vé cốt lõi. Đây chính là bức tranh kiến trúc tổng thể sẽ được hiện thực hóa dần qua các module phía sau trong lộ trình (ORM/JPA, Caching & Messaging, Microservices).
- **Bài 6:** Schema mẫu: `CREATE TABLE room_bookings (id BIGINT PRIMARY KEY, room_id BIGINT, during TSTZRANGE, EXCLUDE USING GIST (room_id WITH =, during WITH &&));`. Cách này **an toàn hơn hẳn** so với tự kiểm tra bằng code Java, vì: ràng buộc được **database tự động thực thi NGUYÊN TỬ (atomic)** ngay tại thời điểm INSERT/UPDATE — không có khoảng hở thời gian giữa "kiểm tra" và "ghi" (check-then-act, Module 05.1) để 2 request đồng thời cùng "lọt qua" bước kiểm tra rồi cùng ghi đè lên nhau; trong khi nếu tự kiểm tra bằng code (`SELECT` xem có overlap không, rồi mới `INSERT` nếu không có), 2 request đến gần như đồng thời hoàn toàn có thể CÙNG đọc thấy "chưa có overlap" trước khi request nào kịp ghi — dẫn đến cả 2 đều chèn thành công, tạo ra 2 booking trùng giờ — đúng bản chất Race Condition đã học ở Module 05.1, giờ được giải quyết triệt để ở TẦNG DATABASE thay vì phải tự đồng bộ hóa ở tầng ứng dụng.

</details>

---

*File tiếp theo trong lộ trình: **Module 11 — ORM: JPA & Hibernate** (@Entity, mapping quan hệ, N+1 Query Problem, Lazy/Eager loading, @Transactional).*
