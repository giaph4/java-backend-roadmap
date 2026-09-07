# Module 10 (tiếp) — RDBMS phổ biến & NoSQL

> **Mức độ ưu tiên: Trung bình (RDBMS thực hành) → Bổ sung (NoSQL)** — Biết cú pháp SQL chuẩn (Module 10) là chưa đủ, cần thành thạo **ít nhất 1 RDBMS thực tế** để làm việc được ngay khi đi thực tập/đi làm. NoSQL không phải "thay thế" SQL mà là **công cụ bổ sung** cho đúng bài toán — hiểu **khi nào** dùng loại nào quan trọng hơn việc thuộc lòng cú pháp MongoDB.

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
| Kiểu dữ liệu | Cơ bản, ít kiểu nâng cao | **Phong phú hơn hẳn**: JSON/JSONB, Array, UUID nguyên sinh, Range Type... (mục 3) |
| Full-Text Search | Có, nhưng hạn chế hơn | Mạnh hơn đáng kể, tích hợp sẵn |
| Xử lý đồng thời (Concurrency) | Dùng khóa (locking) truyền thống | Dùng **MVCC (Multi-Version Concurrency Control)** — cho phép đọc/ghi đồng thời hiệu quả hơn mà ít gây khóa chờ |
| Độ phổ biến tại Việt Nam | RẤT phổ biến — đặc biệt ở startup, dự án web truyền thống, framework PHP (WordPress, Laravel mặc định) | Ngày càng phổ biến ở dự án MỚI, đặc biệt ưa chuộng trong hệ sinh thái backend hiện đại (nhiều công ty công nghệ, dự án Spring Boot) |
| License | GPL (Oracle sở hữu) | PostgreSQL License (rất "mở", gần giống MIT) |

> **Khuyến nghị thực tế cho người mới:** cả 2 đều là lựa chọn tốt và **kiến thức SQL chuẩn học được ở Module 10 áp dụng được cho cả hai** (khác biệt chủ yếu ở 1 số tính năng mở rộng, cú pháp function đặc thù). Nếu phải chọn 1 để thực hành sâu trước, **PostgreSQL** đang là xu hướng được nhiều dự án Spring Boot hiện đại và các công ty công nghệ ưa chuộng hơn nhờ tính năng phong phú và độ tuân thủ chuẩn cao — nhưng biết cả 2 (ít nhất ở mức thực hành cơ bản) là lợi thế lớn khi đi phỏng vấn, vì mỗi công ty có stack công nghệ khác nhau.

---

## 2. Storage Engine của MySQL — InnoDB

MySQL có khái niệm **Storage Engine** (cơ chế lưu trữ) — có thể chọn khác nhau cho từng bảng, dù **InnoDB** gần như luôn là lựa chọn mặc định và đúng đắn trong thực tế hiện đại.

| Storage Engine | Hỗ trợ Transaction (ACID)? | Hỗ trợ Foreign Key? | Khi nào dùng |
|---|---|---|---|
| **InnoDB** (mặc định từ MySQL 5.5+) | ✅ Có | ✅ Có | **LUÔN LUÔN nên dùng** cho ứng dụng backend thông thường — hỗ trợ đầy đủ ACID (Module 10) |
| **MyISAM** (cũ, hiếm dùng ngày nay) | ❌ Không | ❌ Không | Chỉ phù hợp bài toán đọc RẤT nhiều, ghi RẤT ít, KHÔNG cần transaction — hiếm gặp trong backend hiện đại |

> **Lưu ý quan trọng khi phỏng vấn:** nếu được hỏi "MySQL có hỗ trợ Transaction không?" — câu trả lời chính xác là **"Có, NẾU dùng storage engine InnoDB"** (là mặc định hiện nay) — không nên trả lời "Có" hay "Không" một cách tuyệt đối mà không nhắc đến ngữ cảnh storage engine, vì đây chính là điểm mà nhiều tài liệu cũ hay gây nhầm lẫn.

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
> **Liên hệ thực tế:** UUID phù hợp hơn `AUTO_INCREMENT`/`SERIAL` khi hệ thống có **nhiều service/instance cùng tạo dữ liệu độc lập** (Module 19 — Microservices) — tránh xung đột ID khi gộp dữ liệu từ nhiều nguồn, và không để lộ thông tin nghiệp vụ qua ID tuần tự (ví dụ ID tăng dần lộ ra "công ty có bao nhiêu đơn hàng" cho đối thủ cạnh tranh).

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
> Đây là **con dao 2 lưỡi**: linh hoạt cực cao (không cần `ALTER TABLE` khi thay đổi cấu trúc dữ liệu như SQL — liên hệ Module 14, khái niệm Database Migration/Flyway), nhưng **mất đi sự đảm bảo tính toàn vẹn dữ liệu ở tầng database** mà SQL cung cấp sẵn (`NOT NULL`, `CHECK`, kiểu dữ liệu cố định) — trách nhiệm đảm bảo dữ liệu "đúng chuẩn" chuyển phần lớn sang **tầng ứng dụng (code Java)**.

### Khi nào MongoDB phù hợp hơn SQL?

- Dữ liệu có cấu trúc **thay đổi thường xuyên**, khó định nghĩa schema cố định trước (catalog sản phẩm đa dạng loại, CMS nội dung linh hoạt).
- Dữ liệu **lồng nhau tự nhiên**, ít cần JOIN phức tạp qua nhiều bảng (profile người dùng đầy đủ thông tin trong 1 document).
- Cần **mở rộng theo chiều ngang (horizontal scaling)** dễ dàng cho khối lượng dữ liệu cực lớn (MongoDB hỗ trợ **sharding** — phân tán dữ liệu qua nhiều server — dễ dàng hơn RDBMS truyền thống).

### Khi nào SQL vẫn tốt hơn MongoDB?

- Dữ liệu có **quan hệ phức tạp, cần JOIN nhiều bảng thường xuyên** (hệ thống tài chính, ERP).
- Cần **Transaction ACID mạnh** xuyên suốt nhiều bảng/document (dù MongoDB từ bản 4.0+ đã hỗ trợ Multi-Document Transaction, nhưng RDBMS vẫn là lựa chọn "mặc định an toàn hơn" cho nghiệp vụ tài chính nhạy cảm).
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

### Ứng dụng phổ biến nhất của Redis trong backend

| Ứng dụng | Mô tả |
|---|---|
| **Caching** | Lưu kết quả tính toán/truy vấn database TỐN KÉM để tránh tính lại — liên hệ trực tiếp `@Cacheable` sẽ học ở Module 18 |
| **Session Storage** | Lưu session người dùng đăng nhập — đặc biệt quan trọng khi có **NHIỀU instance** backend chạy song song (Module 19 — Microservices), cần nơi lưu session **DÙNG CHUNG** thay vì lưu trong bộ nhớ của TỪNG instance riêng lẻ |
| **Rate Limiting** | Đếm số request từ 1 user/IP trong khoảng thời gian, chặn nếu vượt ngưỡng — dùng `INCR` + `EXPIRE` |
| **Distributed Lock** | Khóa phân tán — mở rộng khái niệm `synchronized` (Module 05.1) ra **NHIỀU instance backend khác nhau** cùng truy cập 1 tài nguyên chung (ví dụ: đảm bảo chỉ 1 instance xử lý 1 tác vụ định kỳ cụ thể tại 1 thời điểm) |
| **Pub/Sub Messaging** | Cơ chế publish/subscribe đơn giản — liên hệ Module 18 (Message Queue), dù Redis Pub/Sub đơn giản hơn Kafka/RabbitMQ nhiều |

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

> **Không cần đi sâu quá vào lý thuyết CAP ở giai đoạn học Backend cơ bản này** — chỉ cần hiểu **ý tưởng đánh đổi cốt lõi**: hệ thống SQL truyền thống thường ưu tiên **tính đúng đắn tuyệt đối** của dữ liệu (đặc biệt phù hợp nghiệp vụ tài chính), trong khi nhiều hệ thống NoSQL sẵn sàng "linh hoạt" hơn về tính nhất quán tức thời để đổi lấy khả năng phục vụ liên tục và mở rộng dễ dàng hơn ở quy mô cực lớn — kiến thức này sẽ được đào sâu hơn nhiều khi học **Module 19 (Microservices) và Module 22 (System Design & Scalability)**.

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

> **Đây chính là kiến trúc thực tế** của rất nhiều hệ thống e-commerce/backend hiện đại: dữ liệu nghiệp vụ **cốt lõi** (user, order, payment — cần Transaction chặt chẽ) nằm ở **PostgreSQL**; dữ liệu **tạm thời cần truy cập cực nhanh** (session, cache kết quả API) nằm ở **Redis**; dữ liệu **log/phân tích hành vi người dùng** (ít cần transaction, cấu trúc linh hoạt, khối lượng lớn) có thể nằm ở **MongoDB** hoặc hệ thống Big Data chuyên biệt hơn. Hiểu được mô hình này là bước chuẩn bị tư duy quan trọng cho **Module 18 (Caching & Messaging)** và **Module 19 (Microservices Architecture)**.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| MySQL vs PostgreSQL | Cả 2 đều tốt; PostgreSQL có nhiều kiểu dữ liệu nâng cao hơn (JSONB, Array, UUID), MySQL phổ biến truyền thống hơn |
| InnoDB | Storage Engine MẶC ĐỊNH của MySQL hiện đại — LUÔN nên dùng vì hỗ trợ đầy đủ Transaction/ACID |
| JSONB (PostgreSQL) | Kết hợp linh hoạt như NoSQL NHƯNG vẫn trong RDBMS, vẫn có Transaction đầy đủ |
| MongoDB | Document DB, schema-less, phù hợp dữ liệu lồng nhau/cấu trúc linh hoạt, không tối ưu cho JOIN phức tạp |
| Redis | In-memory Key-Value Store, cực nhanh, dùng cho cache/session/rate limiting — KHÔNG phải database chính lưu trữ lâu dài |
| Redis cấu trúc nâng cao | List/Set/Sorted Set/Hash — tư duy giống Collections Framework (Module 03.1) nhưng chia sẻ được giữa nhiều server |
| CAP Theorem | Chỉ đảm bảo 2/3 (Consistency, Availability, Partition Tolerance) trong hệ thống phân tán — SQL thường ưu tiên C, nhiều NoSQL ưu tiên A |
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

---

### Phần B — Bài tập thực hành (viết code/query, không cần chạy thật nếu chưa có môi trường)

**Bài 1 — Thiết kế schema PostgreSQL tận dụng JSONB.**
Thiết kế bảng `products` cho hệ thống e-commerce đa dạng loại sản phẩm (đã mô tả ở Câu 2 Phần A) — dùng cột cố định cho thông tin CHUNG (`id`, `name`, `price`, `category`), và cột `JSONB` cho thuộc tính RIÊNG từng loại. Viết 2-3 câu `INSERT` mẫu cho các loại sản phẩm khác nhau, và 1 câu `SELECT` truy vấn theo thuộc tính bên trong JSONB (ví dụ tìm điện thoại có RAM >= 8GB).

**Bài 2 — Thiết kế Document MongoDB cho hệ thống Blog.**
So sánh với thiết kế SQL chuẩn hóa đã làm ở Module 10 (Bài 3 Phần B — hệ thống blog với `users`, `posts`, `tags`, bảng trung gian `post_tags`) — thiết kế LẠI theo phong cách MongoDB: viết ra 1 document mẫu cho `posts` collection, **nhúng (embed)** trực tiếp thông tin tác giả và danh sách tag NGAY TRONG document (không cần bảng trung gian/JOIN). Phân tích ngắn gọn: cách thiết kế này có ưu điểm gì (tốc độ đọc) và nhược điểm gì (dữ liệu tác giả bị LẶP LẠI ở nhiều bài viết, khó đồng bộ khi tác giả đổi tên).

**Bài 3 — Thiết kế chiến lược Cache bằng Redis.**
Cho bài toán: API `GET /api/products/{id}` được gọi RẤT NHIỀU LẦN, nhưng dữ liệu sản phẩm ít khi thay đổi. Mô tả (bằng lời + pseudo-code, không cần code Java thật vì Spring Cache sẽ học ở Module 18) chiến lược cache: (a) khi có request, kiểm tra Redis trước bằng key nào; (b) nếu có (cache hit) thì làm gì; (c) nếu không có (cache miss) thì làm gì, và cần set thời gian hết hạn (TTL) bao lâu là hợp lý, giải thích lý do; (d) khi sản phẩm được CẬP NHẬT (`PUT /api/products/{id}`), cần làm gì với cache để tránh trả về dữ liệu CŨ (gợi ý: tìm hiểu khái niệm "Cache Invalidation").

**Bài 4 — Redis Sorted Set cho bảng xếp hạng.**
Mô tả bằng lệnh Redis (giống cú pháp ở mục 7) cách xây dựng 1 bảng xếp hạng (leaderboard) cho hệ thống game/quiz: mỗi khi người chơi ghi điểm, cập nhật điểm số của họ; viết lệnh lấy TOP 10 người chơi có điểm cao nhất; viết lệnh lấy THỨ HẠNG (rank) cụ thể của 1 người chơi bất kỳ trong bảng xếp hạng (gợi ý: tìm hiểu thêm lệnh `ZREVRANK`).

**Bài 5 — Bài toán tổng hợp: thiết kế Polyglot Persistence cho hệ thống Flash-Sale (liên hệ trực tiếp capstone).**
Dựa trên hiểu biết về capstone "High-Concurrency Event Ticketing & Flash-Sale Engine" của bạn, đề xuất kiến trúc Polyglot Persistence phù hợp: (a) dữ liệu nào (vé, đơn hàng, thanh toán, user) nên nằm ở PostgreSQL/MySQL và tại sao; (b) Redis nên đóng vai trò gì trong việc giải quyết bài toán **oversold** (bán vượt số lượng vé — đã mô phỏng ở Module 05.1 và 05.2 bằng `synchronized`/`AtomicInteger` ở tầng ứng dụng, giờ hãy suy nghĩ thêm về giải pháp ở TẦNG DATABASE/CACHE cho hệ thống có NHIỀU instance backend chạy song song — liên hệ khái niệm Distributed Lock đã nhắc ở mục 6); (c) có cần MongoDB trong hệ thống này không, cho trường hợp dữ liệu nào (nếu có)?

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Sai, nếu nói tuyệt đối** — MySQL **CÓ** hỗ trợ Transaction đầy đủ khi dùng Storage Engine **InnoDB** (mặc định từ MySQL 5.5+). Chỉ khi dùng storage engine cũ như MyISAM thì mới KHÔNG hỗ trợ. Vì InnoDB là mặc định hiện nay, thực tế hầu hết ứng dụng MySQL hiện đại ĐỀU có Transaction đầy đủ.
2. Không có đáp án "đúng tuyệt đối" — đây là bài toán đánh đổi thực tế: RDBMS với nhiều cột NULL (Anti-pattern "Sparse Table") gây lãng phí không gian và khó mở rộng khi có loại sản phẩm mới; **JSONB trong PostgreSQL** là lựa chọn CÂN BẰNG tốt — vừa linh hoạt cho thuộc tính riêng, vừa giữ được Transaction/ACID và các ràng buộc cho dữ liệu CHUNG (`price`, `category`); **MongoDB thuần túy** phù hợp nếu TOÀN BỘ hệ thống đã có xu hướng dữ liệu linh hoạt, không chỉ riêng bảng sản phẩm, và không cần JOIN phức tạp với các bảng nghiệp vụ khác (orders, payments) — với hệ thống e-commerce có nhiều nghiệp vụ LIÊN QUAN CHẶT (đơn hàng, thanh toán, tồn kho), JSONB trong PostgreSQL thường là lựa chọn thực tế hợp lý hơn MongoDB thuần túy.
3. (a) Redis lưu **HOÀN TOÀN trong RAM**, không phải đĩa cứng/SSD như đa số RDBMS/MongoDB (dù có thể ghi backup xuống đĩa định kỳ) — RAM nhanh hơn đĩa cứng ở độ trễ truy cập hàng nghìn lần; (b) Redis xử lý lệnh theo cơ chế **đơn luồng**, loại bỏ hoàn toàn overhead của việc quản lý khóa/đồng bộ hóa phức tạp giữa nhiều luồng như các database hỗ trợ đa luồng đầy đủ.
4. Vì trong hệ thống phân tán thực tế (nhiều server, nhiều trung tâm dữ liệu), sự cố mạng (network partition — mất kết nối tạm thời giữa các node) là điều **CHẮC CHẮN SẼ XẢY RA** ở 1 thời điểm nào đó (do lỗi phần cứng, lỗi cấu hình, tắc nghẽn mạng...) — 1 hệ thống thiết kế mà "giả định mạng luôn ổn định 100%" là thiết kế **KHÔNG THỰC TẾ**. Vì vậy, câu hỏi thực sự không phải "có nên chấp nhận Partition Tolerance không" (bắt buộc phải chấp nhận) mà là "khi network partition XẢY RA, hệ thống nên ưu tiên trả lời đúng ngay cả khi phải chờ đợi (Consistency) hay trả lời ngay lập tức dù có thể chưa hoàn toàn cập nhật mới nhất (Availability)".
5. Vấn đề: "hiện đại hơn" **không phải tiêu chí kỹ thuật hợp lý** để chọn công nghệ — cần dựa trên **BÀI TOÁN CỤ THỂ**. Nếu đồ án capstone có dữ liệu quan hệ rõ ràng (user-order-product, hoặc user-course-enrollment tùy đề tài), cần Transaction chặt chẽ, việc ép dùng MongoDB có thể khiến việc triển khai các nghiệp vụ liên quan nhiều bảng trở nên **PHỨC TẠP HƠN** (phải tự viết logic đảm bảo tính nhất quán ở tầng ứng dụng, thay vì để RDBMS lo qua khóa ngoại + Transaction có sẵn) — đây chính xác là tình huống "dùng công nghệ vì nghe hay, không phải vì phù hợp bài toán", nên cân nhắc lại dựa trên đặc điểm THỰC TẾ của dữ liệu trong đồ án, không phải theo cảm tính "công nghệ mới thì tốt hơn".

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Ví dụ query: `SELECT * FROM products WHERE category = 'phone' AND (attributes->>'ram_gb')::int >= 8;` (cần ép kiểu `::int` vì giá trị trong JSONB mặc định là kiểu text/JSON, không phải số nguyên trực tiếp).
- **Bài 2:** Đây là minh họa trực quan cho khái niệm **Embedding vs Referencing** trong thiết kế MongoDB — Embedding (nhúng trực tiếp) tối ưu tốc độ ĐỌC (không cần JOIN/lookup), nhưng đánh đổi bằng khả năng dữ liệu bị **trùng lặp và khó đồng bộ** khi thông tin gốc (tên tác giả) thay đổi — đây chính là sự đánh đổi ngược lại hoàn toàn với triết lý chuẩn hóa 3NF đã học kỹ ở Module 10, thể hiện rõ MongoDB và SQL có TRIẾT LÝ THIẾT KẾ khác nhau căn bản, không chỉ khác cú pháp.
- **Bài 3:** TTL hợp lý cho dữ liệu sản phẩm ít thay đổi có thể là vài phút đến vài giờ (tùy tần suất cập nhật thực tế của nghiệp vụ) — không nên quá ngắn (mất tác dụng cache) hay quá dài (dữ liệu cũ tồn tại lâu nếu quên invalidate). Phần (d) — Cache Invalidation: khi `PUT /api/products/{id}` thành công, cần **XÓA (DEL)** key cache tương ứng trong Redis NGAY LẬP TỨC (thay vì chờ TTL tự hết hạn) — đảm bảo lần đọc TIẾP THEO sẽ là "cache miss", buộc phải đọc lại dữ liệu MỚI từ database chính rồi mới cache lại.
- **Bài 4:** Ví dụ lệnh: `ZADD game:leaderboard 1500 "player_pho"`; lấy TOP 10: `ZREVRANGE game:leaderboard 0 9 WITHSCORES` (REVRANGE vì mặc định ZRANGE sắp XĂNG DẦN, cần REV để lấy điểm CAO nhất trước); lấy rank cụ thể: `ZREVRANK game:leaderboard "player_pho"`.
- **Bài 5:** Đây là bài tập **tổng hợp và mang tính định hướng kiến trúc quan trọng nhất** của module — (a) toàn bộ dữ liệu nghiệp vụ CỐT LÕI (vé, đơn hàng, thanh toán, user) nên nằm ở PostgreSQL/MySQL vì cần Transaction chặt chẽ, tránh oversold/mất tiền — đúng như đã phân tích ở Module 10; (b) Redis có thể đóng vai trò **Distributed Lock** hoặc dùng lệnh `DECR`/cơ chế atomic của Redis để kiểm tra-và-giảm số lượng vé còn lại ở TẦNG CACHE TRƯỚC KHI chạm đến database — giúp giảm tải cực lớn cho database chính trong thời điểm traffic đỉnh điểm của flash-sale (hàng chục nghìn request cùng lúc tranh mua vài trăm vé), đồng thời đảm bảo tính đúng đắn NGAY CẢ KHI có NHIỀU instance backend chạy song song (điều mà `AtomicInteger` ở Module 05.2 KHÔNG giải quyết được, vì `AtomicInteger` chỉ an toàn trong PHẠM VI 1 JVM process/instance duy nhất); (c) MongoDB có thể phù hợp cho dữ liệu **log hành vi người dùng** (ai đã xem trang vé nào, thời điểm nào, để phân tích sau sự kiện) — dữ liệu này KHÔNG cần Transaction chặt, khối lượng có thể rất lớn, và không ảnh hưởng trực tiếp đến tính đúng đắn của nghiệp vụ bán vé cốt lõi. Đây chính là bức tranh kiến trúc tổng thể sẽ được hiện thực hóa dần qua các Module 14 (Spring Data JPA), 18 (Caching & Messaging), và 19 (Microservices) phía sau trong lộ trình.

</details>

---

*File tiếp theo trong lộ trình: **Module 11 — ORM: JPA & Hibernate** (@Entity, mapping quan hệ, N+1 Query Problem, Lazy/Eager loading, @Transactional).*
