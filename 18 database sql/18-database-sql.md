# Module 10 — Database & SQL

> **Mức độ ưu tiên: Cao** — Backend về bản chất là "xử lý dữ liệu". SQL yếu thì mọi framework phía trên (Hibernate, Spring Data JPA — Module 11, 14) đều xây trên nền cát — bạn sẽ viết ra những đoạn code ORM "chạy được" nhưng sinh ra câu SQL cực kỳ chậm mà không biết tại sao, không đọc hiểu được `EXPLAIN`, và không thể tối ưu khi hệ thống có dữ liệu lớn.

---

## Mục lục

1. [SELECT cơ bản & mệnh đề đi kèm](#1-select-cơ-bản--mệnh-đề-đi-kèm)
2. [JOIN — kết hợp dữ liệu từ nhiều bảng](#2-join--kết-hợp-dữ-liệu-từ-nhiều-bảng)
3. [GROUP BY & Aggregate Function](#3-group-by--aggregate-function)
4. [Subquery](#4-subquery)
5. [Transaction & ACID](#5-transaction--acid)
6. [Isolation Level — mức độ cô lập giao dịch](#6-isolation-level--mức-độ-cô-lập-giao-dịch)
7. [Index — tăng tốc truy vấn](#7-index--tăng-tốc-truy-vấn)
8. [Database Design — Chuẩn hóa dữ liệu (Normalization)](#8-database-design--chuẩn-hóa-dữ-liệu-normalization)
9. [Khóa chính, khóa ngoại & ràng buộc toàn vẹn](#9-khóa-chính-khóa-ngoại--ràng-buộc-toàn-vẹn)
10. [`EXPLAIN` — phân tích query chậm](#10-explain--phân-tích-query-chậm)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. SELECT cơ bản & mệnh đề đi kèm

Giả sử có bảng `employees(id, name, department, salary, hire_date)`:

```sql
SELECT name, salary FROM employees;                          -- chỉ lấy 2 cột cần thiết, KHÔNG dùng SELECT * trong code production
SELECT * FROM employees WHERE department = 'IT';               -- lọc theo điều kiện
SELECT * FROM employees WHERE salary > 15000000 AND department = 'IT'; -- kết hợp nhiều điều kiện
SELECT * FROM employees ORDER BY salary DESC;                  -- sắp xếp giảm dần theo lương
SELECT * FROM employees ORDER BY department ASC, salary DESC;   -- sắp xếp NHIỀU cấp (giống Comparator.thenComparing, Module 02.4)
SELECT * FROM employees LIMIT 10;                                -- chỉ lấy 10 dòng đầu
SELECT * FROM employees LIMIT 10 OFFSET 20;                     -- BỎ QUA 20 dòng đầu, lấy 10 dòng tiếp theo — nền tảng của PHÂN TRANG (Module 14)
SELECT DISTINCT department FROM employees;                       -- loại bỏ trùng lặp (giống Set, Module 03.1)
```

### Thứ tự THỰC THI thực sự của 1 câu SELECT (khác thứ tự VIẾT — rất hay bị hỏi)

```sql
SELECT   department, AVG(salary)     -- (5) chọn cột hiển thị
FROM     employees                    -- (1) xác định NGUỒN dữ liệu
WHERE    hire_date > '2020-01-01'      -- (2) lọc TỪNG DÒNG trước khi group
GROUP BY department                   -- (3) gom nhóm
HAVING   AVG(salary) > 15000000        -- (4) lọc SAU KHI đã group (WHERE không làm được việc này)
ORDER BY AVG(salary) DESC              -- (6) sắp xếp kết quả cuối cùng
LIMIT    5;                            -- (7) giới hạn số dòng trả về
```

> **Thứ tự thực thi thực sự:** `FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT` — **khác hoàn toàn** thứ tự các từ khóa được **viết ra** trên màn hình. Đây là kiến thức nền tảng để hiểu **tại sao** không thể dùng alias đặt trong `SELECT` ở mệnh đề `WHERE` (vì `WHERE` thực thi TRƯỚC `SELECT`), nhưng **có thể** dùng ở `ORDER BY` (thực thi SAU `SELECT`).

---

## 2. JOIN — kết hợp dữ liệu từ nhiều bảng

Giả sử có thêm bảng `departments(id, name)`, và `employees` có cột `department_id` tham chiếu đến `departments.id`.

### INNER JOIN — chỉ lấy dòng khớp ở CẢ HAI bảng

```sql
SELECT e.name, d.name AS department_name
FROM employees e
INNER JOIN departments d ON e.department_id = d.id;
-- CHỈ trả về nhân viên CÓ department_id khớp với 1 department THỰC SỰ tồn tại
-- Nhân viên có department_id = NULL, hoặc trỏ đến department KHÔNG tồn tại → BỊ LOẠI hoàn toàn khỏi kết quả
```

### LEFT JOIN — lấy TOÀN BỘ bảng bên trái, dù có khớp hay không

```sql
SELECT e.name, d.name AS department_name
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id;
-- TRẢ VỀ TẤT CẢ nhân viên (bảng bên TRÁI — "LEFT"), kể cả nhân viên CHƯA có phòng ban (department_id = NULL)
-- Với những dòng KHÔNG khớp, cột "department_name" sẽ là NULL
```

### RIGHT JOIN — ngược lại, lấy TOÀN BỘ bảng bên phải

```sql
SELECT e.name, d.name AS department_name
FROM employees e
RIGHT JOIN departments d ON e.department_id = d.id;
-- TRẢ VỀ TẤT CẢ department, kể cả department CHƯA có nhân viên nào ("phòng ban rỗng")
```

> **Lưu ý thực tế:** `RIGHT JOIN` **hiếm khi** được dùng trực tiếp trong thực tế — vì `A RIGHT JOIN B` luôn có thể viết lại tương đương thành `B LEFT JOIN A` (chỉ cần đổi thứ tự bảng), và đa số lập trình viên/team quy ước **chỉ dùng `LEFT JOIN`** cho nhất quán, dễ đọc hơn khi luôn "cố định tư duy" 1 chiều.

### Sơ đồ Venn minh họa trực quan

```
INNER JOIN:        LEFT JOIN:              RIGHT JOIN:            FULL OUTER JOIN (ít dùng, không phải mọi DB đều hỗ trợ):
   A ∩ B              TOÀN BỘ A              TOÀN BỘ B                TOÀN BỘ A + TOÀN BỘ B
  ┌───┬───┐          ┌───┬───┐              ┌───┬───┐               ┌───┬───┐
  │   │███│          │███│███│              │   │███│               │███│███│
  │   │███│          │███│███│              │███│███│               │███│███│
  └───┴───┘          └───┴───┘              └───┴───┘               └───┴───┘
```

### Nhiều JOIN cùng lúc

```sql
SELECT e.name, d.name AS department, p.name AS project
FROM employees e
INNER JOIN departments d ON e.department_id = d.id
INNER JOIN project_assignments pa ON e.id = pa.employee_id
INNER JOIN projects p ON pa.project_id = p.id;
```

> **Liên hệ trực tiếp Module 11 (JPA/Hibernate):** đây chính là kiến thức nền tảng để hiểu **N+1 Query Problem** — 1 trong những vấn đề hiệu năng phổ biến nhất khi dùng ORM. Nếu không hiểu rõ JOIN, sẽ không hiểu được tại sao Hibernate lại sinh ra **hàng nghìn câu SELECT nhỏ lẻ** thay vì **1 câu JOIN duy nhất** khi cấu hình `fetch` sai.

---

## 3. GROUP BY & Aggregate Function

```sql
SELECT department, COUNT(*) AS total_employees
FROM employees
GROUP BY department;
-- ĐẾM số nhân viên theo TỪNG phòng ban — tương đương Collectors.groupingBy(..., Collectors.counting()) đã học ở Module 03.3

SELECT department, AVG(salary) AS avg_salary, MAX(salary) AS max_salary, MIN(salary) AS min_salary
FROM employees
GROUP BY department;

SELECT department, SUM(salary) AS total_payroll
FROM employees
GROUP BY department
HAVING SUM(salary) > 100000000; -- lọc SAU KHI group — WHERE không làm được vì WHERE chạy TRƯỚC khi group
```

### Aggregate Function phổ biến

| Function | Ý nghĩa |
|---|---|
| `COUNT(*)` | Đếm số dòng |
| `COUNT(column)` | Đếm số dòng có giá trị KHÁC NULL ở cột đó |
| `SUM()` | Tổng |
| `AVG()` | Trung bình |
| `MAX()` / `MIN()` | Giá trị lớn nhất/nhỏ nhất |

> **Bẫy hay gặp:** `WHERE` không thể dùng aggregate function (`WHERE COUNT(*) > 5` ❌ SAI) — vì `WHERE` chạy **trước** khi dữ liệu được nhóm lại, lúc đó chưa có kết quả `COUNT()` để so sánh. Phải dùng `HAVING` (chạy **sau** `GROUP BY`).

---

## 4. Subquery

**Subquery (truy vấn con)** — 1 câu `SELECT` nằm **bên trong** 1 câu `SELECT` khác.

### Subquery trong `WHERE`

```sql
SELECT name, salary
FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees); -- tìm nhân viên có lương CAO HƠN mức trung bình toàn công ty
```

### Subquery trong `FROM` (Derived Table)

```sql
SELECT dept_summary.department, dept_summary.avg_salary
FROM (
    SELECT department, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
) AS dept_summary
WHERE dept_summary.avg_salary > 15000000;
```

### `IN` với Subquery

```sql
SELECT name FROM employees
WHERE department_id IN (SELECT id FROM departments WHERE name = 'IT'); -- tương đương với JOIN, nhưng đôi khi dễ đọc hơn cho trường hợp đơn giản
```

### Common Table Expression (CTE) — `WITH` — cách viết hiện đại, dễ đọc hơn Subquery lồng nhau

```sql
WITH department_summary AS ( -- đặt TÊN cho subquery, giống như "biến tạm" trong SQL
    SELECT department, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
)
SELECT * FROM department_summary WHERE avg_salary > 15000000;
```

> **Khuyến nghị thực tế:** với query phức tạp có **nhiều tầng subquery lồng nhau**, nên ưu tiên dùng **CTE (`WITH`)** thay vì subquery lồng sâu — dễ đọc, dễ debug hơn nhiều (đọc từ trên xuống như các bước tuần tự, thay vì phải "đọc từ trong ra ngoài" của subquery lồng nhau).

---

## 5. Transaction & ACID

**Transaction (giao dịch)** là 1 nhóm các thao tác SQL được thực thi như **1 đơn vị KHÔNG THỂ CHIA TÁCH** — hoặc **TẤT CẢ** đều thành công, hoặc **TẤT CẢ** đều bị hủy bỏ (không có trạng thái "làm được một nửa").

### Ví dụ kinh điển: chuyển khoản ngân hàng

```sql
BEGIN TRANSACTION;

UPDATE accounts SET balance = balance - 1000000 WHERE id = 'A'; -- trừ tiền tài khoản A
UPDATE accounts SET balance = balance + 1000000 WHERE id = 'B'; -- cộng tiền tài khoản B

COMMIT; -- XÁC NHẬN — cả 2 thay đổi trên được LƯU VĨNH VIỄN cùng lúc
-- Nếu có LỖI xảy ra giữa chừng (ví dụ mất kết nối sau dòng UPDATE đầu tiên):
-- ROLLBACK; -- HỦY BỎ TOÀN BỘ, tài khoản A được TRẢ LẠI như CHƯA từng bị trừ tiền
```

> **Nếu KHÔNG dùng Transaction:** nếu hệ thống "sập" đúng lúc giữa 2 câu `UPDATE`, tài khoản A đã bị trừ tiền nhưng B **chưa kịp** được cộng — **tiền "biến mất"** khỏi hệ thống, đây là lỗi nghiêm trọng bậc nhất trong hệ thống tài chính, và Transaction chính là cơ chế **bắt buộc** để ngăn chặn tình huống này.

### ACID — 4 tính chất cốt lõi của Transaction

| Chữ cái | Tên | Ý nghĩa |
|---|---|---|
| **A** | **Atomicity** (Tính nguyên tử) | Tất cả thao tác trong transaction hoặc **THÀNH CÔNG HẾT**, hoặc **THẤT BẠI HẾT** — không có trạng thái "làm nửa chừng" |
| **C** | **Consistency** (Tính nhất quán) | Database luôn chuyển từ 1 trạng thái **HỢP LỆ** sang 1 trạng thái **HỢP LỆ** khác — không vi phạm bất kỳ ràng buộc nào (khóa ngoại, `NOT NULL`, `CHECK`...) |
| **I** | **Isolation** (Tính cô lập) | Nhiều transaction chạy **ĐỒNG THỜI** không ảnh hưởng lẫn nhau theo cách gây ra kết quả sai — chi tiết ở mục 6 |
| **D** | **Durability** (Tính bền vững) | Sau khi `COMMIT` thành công, dữ liệu được lưu **VĨNH VIỄN**, dù hệ thống có mất điện/crash ngay sau đó |

> **Liên hệ trực tiếp Module 12 (Spring Framework Core) và Module 14 (Spring Data JPA):** annotation `@Transactional` chính là cách Spring **tự động quản lý** `BEGIN TRANSACTION`/`COMMIT`/`ROLLBACK` — đây là lý do ở Module 04 (Exception Handling) đã nhắc: khi 1 method có `@Transactional` ném ra 1 **Unchecked Exception**, Spring **tự động ROLLBACK** toàn bộ transaction đó, đảm bảo tính Atomicity **mà không cần lập trình viên tự viết** `BEGIN`/`COMMIT`/`ROLLBACK` thủ công như ví dụ SQL thuần ở trên.

---

## 6. Isolation Level — mức độ cô lập giao dịch

Khi **nhiều transaction chạy đồng thời** (liên hệ trực tiếp Module 05 — Multithreading, nhưng ở tầng database), có thể xảy ra các vấn đề sau nếu không kiểm soát đúng:

| Vấn đề | Mô tả |
|---|---|
| **Dirty Read** | Transaction A đọc được dữ liệu **CHƯA COMMIT** của Transaction B — nếu B sau đó `ROLLBACK`, A đã "đọc nhầm" dữ liệu không bao giờ tồn tại thật sự |
| **Non-Repeatable Read** | Trong CÙNG 1 transaction, A đọc 1 dòng 2 lần, nhưng **giá trị khác nhau** giữa 2 lần đọc — vì B đã `UPDATE` và `COMMIT` dòng đó ở giữa |
| **Phantom Read** | Trong CÙNG 1 transaction, A chạy CÙNG 1 câu query 2 lần, nhưng **SỐ LƯỢNG dòng trả về khác nhau** — vì B đã `INSERT`/`DELETE` thêm dòng mới khớp điều kiện đó ở giữa |

### 4 Isolation Level chuẩn SQL (từ LỎNG nhất đến CHẶT nhất)

```
READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ → SERIALIZABLE
(nhanh nhất,                                          (chậm nhất,
 rủi ro cao nhất)                                      an toàn nhất)
```

| Isolation Level | Ngăn được Dirty Read? | Ngăn được Non-Repeatable Read? | Ngăn được Phantom Read? |
|---|---|---|---|
| `READ UNCOMMITTED` | ❌ | ❌ | ❌ |
| `READ COMMITTED` (mặc định PostgreSQL, Oracle) | ✅ | ❌ | ❌ |
| `REPEATABLE READ` (mặc định MySQL/InnoDB) | ✅ | ✅ | ❌ (MySQL InnoDB thực tế còn ngăn được cả Phantom Read nhờ cơ chế riêng, nhưng theo chuẩn SQL lý thuyết thì không đảm bảo) |
| `SERIALIZABLE` | ✅ | ✅ | ✅ |

> **Đánh đổi cốt lõi:** Isolation Level càng **CHẶT** (càng gần `SERIALIZABLE`) thì càng **AN TOÀN** trước các vấn đề đồng thời, nhưng cũng càng **CHẬM** (nhiều transaction phải chờ đợi/khóa lẫn nhau nhiều hơn — liên hệ trực tiếp khái niệm `synchronized` ở Module 05.1, cùng bản chất đánh đổi giữa an toàn và hiệu năng). Đa số ứng dụng backend thực tế dùng mức mặc định của database (`READ COMMITTED` hoặc `REPEATABLE READ`) là đủ, chỉ nâng lên `SERIALIZABLE` cho các nghiệp vụ **cực kỳ nhạy cảm** (ví dụ: xử lý tồn kho trong hệ thống Flash-Sale — sẽ gặp lại ở capstone).

---

## 7. Index — tăng tốc truy vấn

**Index** là 1 cấu trúc dữ liệu **phụ** (thường là **B-Tree**, tương tự ý tưởng `TreeMap` đã học ở Module 03.1) giúp database **tìm dữ liệu nhanh hơn** mà **không cần quét toàn bộ bảng (Full Table Scan)**.

### Không có Index — Full Table Scan

```sql
SELECT * FROM employees WHERE email = 'pho@example.com';
-- KHÔNG có index trên "email" → database phải QUÉT TỪNG DÒNG MỘT trong TOÀN BỘ bảng để tìm — O(n), CỰC KỲ CHẬM với bảng triệu dòng
```

### Có Index

```sql
CREATE INDEX idx_employees_email ON employees(email);

SELECT * FROM employees WHERE email = 'pho@example.com';
-- CÓ index → database dùng cấu trúc B-Tree để "nhảy thẳng" đến đúng vị trí — GẦN O(log n), NHANH HƠN RẤT NHIỀU
```

### So sánh trực quan với Index của 1 cuốn sách

Giống hệt **mục lục (index)** ở cuối 1 cuốn sách giáo trình — không có mục lục, muốn tìm 1 khái niệm phải **lật từng trang** đọc hết; có mục lục, chỉ cần tra tên khái niệm, biết ngay số trang, **nhảy thẳng** đến đó.

### Đánh đổi của Index — KHÔNG PHẢI cứ tạo Index là luôn tốt

| Lợi ích | Chi phí |
|---|---|
| Tăng tốc **đọc** (`SELECT`) đáng kể | Làm **CHẬM** thao tác **ghi** (`INSERT`/`UPDATE`/`DELETE`) — mỗi lần ghi dữ liệu, database phải **cập nhật LẠI** cấu trúc Index tương ứng |
| | Tốn thêm **bộ nhớ/dung lượng đĩa** để lưu trữ cấu trúc Index |

> **Nguyên tắc thực tế khi quyết định tạo Index:** nên tạo Index cho các cột **thường xuyên xuất hiện** trong mệnh đề `WHERE`, `JOIN ON`, `ORDER BY` — đặc biệt là **khóa ngoại** (thường KHÔNG tự động có Index, khác với khóa chính luôn tự động có Index) và các cột dùng để tra cứu thường xuyên (`email`, `username`...). **KHÔNG** nên tạo Index bừa bãi cho mọi cột — đặc biệt tránh với bảng có **tần suất ghi cao** (write-heavy) mà ít khi truy vấn theo cột đó.

### Composite Index — Index trên NHIỀU cột

```sql
CREATE INDEX idx_employees_dept_salary ON employees(department, salary);
-- Index này TỐI ƯU cho query lọc theo department TRƯỚC, rồi salary
-- Query: WHERE department = 'IT' AND salary > 15000000  → TẬN DỤNG được index TỐI ĐA
-- Query: WHERE salary > 15000000 (không có department) → KHÔNG tận dụng được index này hiệu quả (nguyên tắc "Leftmost Prefix" — cột ĐẦU TIÊN trong composite index phải xuất hiện trong điều kiện thì mới tận dụng tốt được)
```

---

## 8. Database Design — Chuẩn hóa dữ liệu (Normalization)

**Chuẩn hóa (Normalization)** là quá trình tổ chức bảng dữ liệu để **giảm dư thừa (redundancy)** và **tránh bất thường dữ liệu (data anomaly)** khi thêm/sửa/xóa.

### Vấn đề khi KHÔNG chuẩn hóa — bảng "phẳng" chứa mọi thứ

```
orders (thiết kế TỆ — chưa chuẩn hóa)
┌────┬─────────────┬──────────────────┬────────────────────┐
│ id │ product_name│ customer_name     │ customer_email      │
├────┼─────────────┼──────────────────┼────────────────────┤
│ 1  │ Laptop      │ Pho               │ pho@example.com     │
│ 2  │ Mouse       │ Pho               │ pho@example.com     │ ← LẶP LẠI thông tin customer!
│ 3  │ Keyboard    │ Pho               │ pho@example.com     │ ← LẶP LẠI LẦN NỮA!
└────┴─────────────┴──────────────────┴────────────────────┘
```

**Vấn đề:** nếu "Pho" đổi email, phải **UPDATE nhiều dòng CÙNG LÚC** (dễ sót, dễ gây dữ liệu KHÔNG NHẤT QUÁN nếu chỉ update sót 1-2 dòng) — đây gọi là **"Update Anomaly"**.

### 1NF (First Normal Form) — mỗi ô chỉ chứa 1 giá trị nguyên tử (atomic)

```
❌ VI PHẠM 1NF:
┌────┬──────────────────────┐
│ id │ phone_numbers         │
├────┼──────────────────────┤
│ 1  │ "0901234567, 0987654321" │ ← NHIỀU giá trị trong 1 ô!
└────┴──────────────────────┘

✅ TUÂN THỦ 1NF — tách thành bảng riêng:
customer_phones(customer_id, phone_number) -- mỗi dòng CHỈ 1 số điện thoại
```

### 2NF (Second Normal Form) — không có "Partial Dependency" (áp dụng khi có khóa chính GHÉP nhiều cột)

Nếu khóa chính là **cặp (order_id, product_id)**, mọi cột khác **PHẢI phụ thuộc vào CẢ HAI cột** của khóa chính, không phải chỉ 1 trong 2.

```
❌ VI PHẠM 2NF:
order_items(order_id, product_id, product_name, quantity)
-- "product_name" chỉ phụ thuộc vào product_id (KHÔNG cần order_id để xác định) → VI PHẠM

✅ TUÂN THỦ 2NF — tách "product_name" ra bảng products riêng:
order_items(order_id, product_id, quantity)
products(product_id, product_name)
```

### 3NF (Third Normal Form) — không có "Transitive Dependency" (phụ thuộc bắc cầu)

```
❌ VI PHẠM 3NF:
employees(id, department_id, department_name)
-- "department_name" phụ thuộc vào "department_id", KHÔNG phụ thuộc TRỰC TIẾP vào "id" (khóa chính)
-- → PHỤ THUỘC BẮC CẦU: id → department_id → department_name

✅ TUÂN THỦ 3NF — tách department ra bảng riêng (đúng như ví dụ employees/departments đã dùng xuyên suốt module này):
employees(id, department_id) -- CHỈ giữ lại khóa ngoại, KHÔNG lặp lại department_name
departments(id, name)
```

### Áp dụng 3NF thực tế cho bài toán ban đầu

```sql
customers(id, name, email)
products(id, name, price)
orders(id, customer_id, order_date)  -- khóa ngoại đến customers
order_items(order_id, product_id, quantity) -- khóa ngoại đến CẢ orders VÀ products
```
> Giờ đây, đổi email của "Pho" chỉ cần **UPDATE 1 DÒNG DUY NHẤT** trong bảng `customers` — mọi `orders` liên quan tự động "thấy" thông tin mới thông qua `customer_id`, không có dữ liệu nào bị dư thừa/không nhất quán.

### Khi nào NÊN "phá vỡ" chuẩn hóa (Denormalization)?

Chuẩn hóa quá mức đồng nghĩa với **nhiều bảng hơn → nhiều JOIN hơn** khi truy vấn — với hệ thống có **traffic đọc RẤT LỚN**, đôi khi chấp nhận **lưu dư thừa CÓ CHỦ ĐÍCH** (ví dụ lưu sẵn `product_name` trong `order_items` tại thời điểm đặt hàng, dù về lý thuyết "vi phạm" 2NF) để **tránh JOIN**, tăng tốc đọc — đây gọi là **Denormalization**, 1 kỹ thuật tối ưu hiệu năng có chủ đích, thường gặp ở hệ thống quy mô lớn (sẽ nhắc lại khi học Module 22 — System Design & Scalability).

---

## 9. Khóa chính, khóa ngoại & ràng buộc toàn vẹn

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY,                     -- Khóa chính (Primary Key) — định danh DUY NHẤT mỗi dòng, KHÔNG được NULL
    name VARCHAR(100) NOT NULL UNIQUE            -- NOT NULL: bắt buộc có giá trị; UNIQUE: không được trùng
);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    salary DECIMAL(12,2) CHECK (salary >= 0),   -- CHECK: ràng buộc điều kiện tùy chỉnh — không cho lương âm
    department_id BIGINT,
    FOREIGN KEY (department_id) REFERENCES departments(id) -- Khóa ngoại — đảm bảo department_id LUÔN trỏ đến 1 department CÓ THẬT
        ON DELETE SET NULL                       -- nếu department bị XÓA → department_id của nhân viên tự động thành NULL (thay vì lỗi hoặc xóa luôn nhân viên)
);
```

### Các hành vi `ON DELETE` phổ biến khi khóa ngoại bị xóa

| Hành vi | Ý nghĩa |
|---|---|
| `CASCADE` | XÓA LUÔN các dòng liên quan (ví dụ: xóa `order` thì tự động xóa toàn bộ `order_items` của nó) |
| `SET NULL` | Đặt khóa ngoại thành `NULL` (như ví dụ trên) |
| `RESTRICT` (mặc định) | **NGĂN CHẶN** việc xóa nếu vẫn còn dòng khác đang tham chiếu đến nó — ném lỗi |

> **Lưu ý thiết kế quan trọng:** `CASCADE` cần dùng **CẨN THẬN** — dễ gây **xóa nhầm dữ liệu hàng loạt** ngoài ý muốn nếu không hiểu rõ toàn bộ chuỗi quan hệ liên quan. Trong thực tế backend, nhiều team ưu tiên **"Soft Delete"** (chỉ đánh dấu `is_deleted = true` thay vì xóa thật) để tránh rủi ro mất dữ liệu vĩnh viễn — sẽ gặp lại khái niệm này khi thiết kế Entity ở Module 11.

---

## 10. `EXPLAIN` — phân tích query chậm

`EXPLAIN` cho biết database sẽ **THỰC THI** 1 câu query như thế nào — đây là công cụ **quan trọng bậc nhất** để chẩn đoán và tối ưu query chậm trong thực tế.

```sql
EXPLAIN SELECT * FROM employees WHERE email = 'pho@example.com';
```

Kết quả (ví dụ đơn giản hóa, cú pháp/format cụ thể khác nhau tùy PostgreSQL/MySQL):

```
Seq Scan on employees  (cost=0.00..1834.00 rows=1 width=120)
  Filter: (email = 'pho@example.com'::text)
```

`Seq Scan` (Sequential Scan — quét tuần tự, tương đương Full Table Scan) nghĩa là **KHÔNG dùng Index**, phải quét **toàn bộ bảng** — nếu bảng có hàng triệu dòng, đây chính là dấu hiệu **CẦN thêm Index** cho cột `email`.

```sql
CREATE INDEX idx_employees_email ON employees(email);
EXPLAIN SELECT * FROM employees WHERE email = 'pho@example.com';
```

Kết quả sau khi có Index:

```
Index Scan using idx_employees_email on employees  (cost=0.29..8.31 rows=1 width=120)
  Index Cond: (email = 'pho@example.com'::text)
```

`Index Scan` — database đã dùng Index để "nhảy thẳng" đến dữ liệu, `cost` giảm **đáng kể** (từ 1834 xuống chỉ còn 8.31 trong ví dụ minh họa này) — đây chính là bằng chứng cụ thể chứng minh Index có hiệu quả.

### `EXPLAIN ANALYZE` — chạy THỰC SỰ câu query và đo thời gian THẬT

```sql
EXPLAIN ANALYZE SELECT * FROM employees WHERE email = 'pho@example.com';
-- Ngoài "cost" ƯỚC TÍNH, còn cho biết THỜI GIAN THỰC TẾ đã chạy, số dòng THỰC TẾ trả về — chính xác hơn EXPLAIN thường (chỉ ước tính, chưa chạy thật)
```

> **Quy trình chẩn đoán query chậm chuẩn thực tế:** (1) xác định câu query đang chậm (qua log, qua công cụ monitoring — Module 21), (2) chạy `EXPLAIN ANALYZE` để xem chi tiết cách database thực thi, (3) tìm các dấu hiệu bất thường (`Seq Scan` trên bảng lớn, số dòng ước tính SAI LỆCH quá xa số dòng thực tế, `Nested Loop` không hiệu quả trên JOIN nhiều dòng...), (4) thêm Index phù hợp hoặc viết lại query, (5) chạy lại `EXPLAIN ANALYZE` để **XÁC NHẬN** đã cải thiện thực sự (không chỉ đoán mò).

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| Thứ tự thực thi SQL | `FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT` — khác thứ tự viết |
| INNER vs LEFT JOIN | INNER chỉ lấy dòng khớp cả 2 bên; LEFT lấy TOÀN BỘ bảng trái, NULL cho phần không khớp |
| HAVING vs WHERE | WHERE lọc TRƯỚC group (không dùng được aggregate function); HAVING lọc SAU group |
| ACID | Atomicity, Consistency, Isolation, Durability — 4 tính chất cốt lõi của Transaction |
| Isolation Level | Càng chặt (SERIALIZABLE) càng an toàn nhưng càng chậm — đánh đổi kinh điển |
| Index | Tăng tốc ĐỌC, làm chậm GHI — chỉ nên tạo cho cột hay dùng trong WHERE/JOIN/ORDER BY |
| 1NF/2NF/3NF | Chuẩn hóa để giảm dư thừa dữ liệu, tránh Update/Insert/Delete Anomaly |
| Denormalization | Phá vỡ chuẩn hóa có chủ đích để tối ưu tốc độ đọc ở hệ thống quy mô lớn |
| Khóa ngoại `ON DELETE` | CASCADE (xóa theo), SET NULL, RESTRICT (mặc định, ngăn xóa) — dùng CASCADE cẩn thận |
| `EXPLAIN` | Công cụ quan trọng nhất để chẩn đoán query chậm — tìm `Seq Scan` trên bảng lớn |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Câu SQL sau có lỗi gì? Sửa lại đúng.
```sql
SELECT department, COUNT(*) FROM employees WHERE COUNT(*) > 5 GROUP BY department;
```

**Câu 2.** Phân biệt `INNER JOIN` và `LEFT JOIN` bằng ví dụ cụ thể: nếu có 10 nhân viên, trong đó 2 người chưa được gán `department_id` (giá trị NULL), câu `INNER JOIN` và `LEFT JOIN` với bảng `departments` sẽ trả về bao nhiêu dòng tương ứng (giả sử mọi department_id còn lại đều khớp hợp lệ)?

**Câu 3.** Trong đoạn code Java dùng JDBC sau, nếu KHÔNG có transaction, điều gì có thể xảy ra nếu server crash NGAY SAU dòng đầu tiên?
```java
statement.execute("UPDATE accounts SET balance = balance - 500 WHERE id = 'A'");
// [server CRASH ngay tại đây]
statement.execute("UPDATE accounts SET balance = balance + 500 WHERE id = 'B'");
```

**Câu 4.** Bảng `products(id, category_id, category_name, price)` đang vi phạm chuẩn hóa nào? Đề xuất cách sửa.

**Câu 5.** `EXPLAIN` cho thấy 1 query đang dùng `Seq Scan` trên bảng có 5 triệu dòng, với điều kiện `WHERE status = 'ACTIVE'`. Đây có phải luôn luôn dấu hiệu cần thêm Index không? (Gợi ý: xem xét trường hợp 4.9 triệu/5 triệu dòng đều có `status = 'ACTIVE'`.)

---

### Phần B — Bài tập viết SQL

**Bài 1 — JOIN & GROUP BY tổng hợp.**
Cho schema: `customers(id, name)`, `orders(id, customer_id, order_date, total_amount)`. Viết câu SQL: (a) liệt kê TÊN khách hàng và TỔNG số tiền đã chi tiêu, sắp xếp giảm dần theo tổng chi tiêu; (b) chỉ hiển thị khách hàng có tổng chi tiêu **trên 5.000.000**; (c) đảm bảo khách hàng **CHƯA từng đặt hàng nào** vẫn xuất hiện trong kết quả với tổng chi tiêu = 0 (gợi ý: cần LEFT JOIN kết hợp `COALESCE`).

**Bài 2 — Subquery & CTE.**
Cho schema `employees(id, name, department, salary)`. Viết câu SQL tìm ra nhân viên có **lương cao nhất trong TỪNG phòng ban** (không phải cao nhất toàn công ty) — thử viết bằng 2 cách: (a) dùng Subquery tương quan (correlated subquery), (b) dùng Window Function `RANK() OVER (PARTITION BY department ORDER BY salary DESC)` (kiến thức mở rộng — tìm hiểu thêm nếu chưa quen, đây là kỹ thuật rất mạnh và phổ biến trong SQL hiện đại).

**Bài 3 — Thiết kế schema chuẩn hóa cho hệ thống Blog.**
Thiết kế schema (viết `CREATE TABLE` đầy đủ với khóa chính, khóa ngoại) cho 1 hệ thống blog đơn giản gồm: `users` (tác giả), `posts` (bài viết, mỗi bài có 1 tác giả), `tags` (thẻ phân loại), và quan hệ **nhiều-nhiều** giữa `posts` và `tags` (1 bài viết có thể có nhiều tag, 1 tag có thể gắn với nhiều bài viết — cần thêm 1 bảng trung gian). Đảm bảo tuân thủ 3NF.

**Bài 4 — Transaction giả lập hệ thống đặt vé (liên hệ trực tiếp Module 05).**
Viết đoạn SQL mô phỏng Transaction cho việc đặt vé: kiểm tra còn vé (`SELECT ... FOR UPDATE` — khóa dòng đang đọc để tránh 2 giao dịch cùng đọc thấy "còn vé" rồi cùng đặt, liên hệ trực tiếp Race Condition đã học ở Module 05.1 nhưng ở TẦNG DATABASE thay vì tầng ứng dụng), giảm số vé còn lại, thêm bản ghi vào bảng `bookings`. Giải thích ngắn gọn (bằng comment) vì sao `SELECT ... FOR UPDATE` cần thiết ở đây, liên hệ lại khái niệm Isolation Level đã học ở mục 6.

**Bài 5 — Bài toán tổng hợp: chẩn đoán và tối ưu query chậm.**
Cho câu query sau đang chạy chậm trên bảng `orders` có 10 triệu dòng:
```sql
SELECT * FROM orders
WHERE customer_id = 12345 AND order_date > '2026-01-01'
ORDER BY order_date DESC;
```
Đề xuất (a) loại Index phù hợp nhất nên tạo (đơn cột hay composite? thứ tự cột nào?), (b) giải thích lý do dựa trên nguyên tắc "Leftmost Prefix" đã học ở mục 7, (c) viết câu lệnh `CREATE INDEX` cụ thể, (d) mô tả cách bạn sẽ XÁC NHẬN việc thêm Index thực sự cải thiện hiệu năng (liên hệ mục 10 — `EXPLAIN ANALYZE`).

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. Lỗi: dùng `COUNT(*)` trong `WHERE` — aggregate function KHÔNG dùng được ở `WHERE` (chạy trước GROUP BY, chưa có kết quả để so sánh). Sửa: chuyển sang `HAVING`:
```sql
SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5;
```
2. `INNER JOIN` trả về **8 dòng** (chỉ 8 nhân viên CÓ department_id hợp lệ, 2 người NULL bị loại hoàn toàn). `LEFT JOIN` trả về **10 dòng** (TẤT CẢ nhân viên, kể cả 2 người có department_id NULL — với các dòng này, cột thông tin department sẽ là NULL).
3. Tài khoản A đã bị trừ 500 (và COMMIT ngay lập tức vì KHÔNG có transaction bao bọc — mỗi câu SQL riêng lẻ tự động là 1 "transaction ngầm" theo mặc định của hầu hết database engine khi không có `BEGIN`/`COMMIT` tường minh), nhưng B **CHƯA kịp** được cộng 500 do server crash trước khi thực thi dòng thứ 2 — 500 "biến mất" khỏi hệ thống, vi phạm Atomicity nghiêm trọng. Đây chính là lý do BẮT BUỘC phải bọc `BEGIN TRANSACTION ... COMMIT` quanh CẢ HAI câu UPDATE.
4. Vi phạm **3NF** (Transitive Dependency) — `category_name` phụ thuộc vào `category_id`, không phụ thuộc trực tiếp vào khóa chính `id` của `products`. Sửa: tách `categories(id, name)` riêng, `products` chỉ giữ `category_id` làm khóa ngoại.
5. **Không luôn luôn đúng** — nếu 4.9 triệu/5 triệu dòng đều có `status = 'ACTIVE'` (tức là điều kiện lọc gần như KHÔNG chọn lọc được gì, chiếm gần 100% bảng), việc dùng Index thực ra có thể **CHẬM HƠN** Seq Scan (vì phải đọc Index rồi lại nhảy về bảng chính để lấy dữ liệu đầy đủ cho gần như toàn bộ 5 triệu dòng — tốn thêm bước trung gian không cần thiết). Query Optimizer của database **thường tự động** nhận ra tình huống này và **chủ động chọn** Seq Scan dù có Index — đây là lý do luôn cần `EXPLAIN ANALYZE` để **kiểm chứng thực tế**, không nên chỉ dựa vào "quy tắc chung chung" mà kết luận vội vàng.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Phần (c) gợi ý lời giải:
```sql
SELECT c.name, COALESCE(SUM(o.total_amount), 0) AS total_spent
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
GROUP BY c.id, c.name
HAVING COALESCE(SUM(o.total_amount), 0) > 5000000
ORDER BY total_spent DESC;
```
(`COALESCE` trả về giá trị THAY THẾ nếu biểu thức đầu là NULL — cần thiết vì khách hàng chưa có đơn hàng nào sẽ có `SUM(o.total_amount)` là NULL, không phải 0.)
- **Bài 2:** Cách (a) — Correlated Subquery:
```sql
SELECT e1.* FROM employees e1
WHERE e1.salary = (SELECT MAX(e2.salary) FROM employees e2 WHERE e2.department = e1.department);
```
Cách (b) — Window Function (thường được ưa chuộng hơn trong SQL hiện đại vì rõ ràng và thường hiệu quả hơn):
```sql
SELECT * FROM (
    SELECT *, RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rnk
    FROM employees
) ranked WHERE rnk = 1;
```
- **Bài 3:** Bảng trung gian cho quan hệ nhiều-nhiều thường đặt tên `post_tags(post_id, tag_id)` với khóa chính GHÉP (composite primary key) là cả 2 cột, và 2 khóa ngoại tương ứng trỏ về `posts(id)` và `tags(id)`.
- **Bài 4:** `SELECT ... FOR UPDATE` cần thiết vì nếu KHÔNG khóa dòng đang đọc, 2 transaction chạy đồng thời (2 người cùng cố đặt vé cuối cùng) đều có thể **CÙNG ĐỌC** thấy "còn 1 vé" TRƯỚC KHI bất kỳ ai kịp `UPDATE`/`COMMIT` — dẫn đến CẢ HAI đều đặt thành công, gây **oversold** (bán vượt số lượng thực có) — chính xác là vấn đề Race Condition đã học ở Module 05.1, nhưng lần này xảy ra ở **TẦNG DATABASE** thay vì tầng ứng dụng Java thuần túy. `FOR UPDATE` buộc transaction thứ 2 phải **CHỜ** transaction thứ 1 hoàn tất (COMMIT hoặc ROLLBACK) trước khi được phép đọc dòng đó, đảm bảo tính nhất quán.
- **Bài 5:** Nên tạo **Composite Index** trên `(customer_id, order_date)` — theo đúng thứ tự này, KHÔNG phải ngược lại — vì nguyên tắc "Leftmost Prefix": điều kiện `WHERE customer_id = 12345` là so sánh **bằng (equality)**, nên đặt LÀM CỘT ĐẦU TIÊN của composite index; điều kiện `order_date > '2026-01-01'` là so sánh **khoảng (range)**, đặt sau cùng — đây là quy tắc chuẩn khi thiết kế composite index: **cột dùng so sánh bằng (`=`) luôn đặt TRƯỚC** cột dùng so sánh khoảng (`>`, `<`, `BETWEEN`). Việc `ORDER BY order_date DESC` cũng được hưởng lợi thêm từ index này vì dữ liệu trong mỗi nhóm `customer_id` đã được sắp xếp sẵn theo `order_date` nhờ cấu trúc B-Tree của Index.

</details>

---

*File tiếp theo trong lộ trình: **Module 10 (tiếp) — RDBMS phổ biến & NoSQL** (MySQL/PostgreSQL thực hành, MongoDB, Redis — khi nào chọn NoSQL thay vì SQL).*
