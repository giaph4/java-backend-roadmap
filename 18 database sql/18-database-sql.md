# Module 10 — Database & SQL

> **Mức độ ưu tiên: Cao** — Backend về bản chất là "xử lý dữ liệu". SQL yếu thì mọi framework phía trên (Hibernate, Spring Data JPA — Module 11, 14) đều xây trên nền cát — code ORM "chạy được" nhưng sinh SQL chậm mà không biết vì sao, không đọc được `EXPLAIN`, không tối ưu được khi dữ liệu lớn.

> **Phạm vi bài này:** SQL chuẩn (SELECT/JOIN/GROUP BY/Window Function/Subquery), Transaction & ACID, Isolation Level & khóa ở tầng database, Index (cơ chế & loại), thiết kế schema (chuẩn hóa/khóa), `EXPLAIN`. **Chỉ nhắc tên, không đi sâu:** JPA/Hibernate, `@Transactional`, N+1 query (Module 11); NoSQL (module kế); logging/monitoring query chậm (Module 21); System Design/sharding (module cuối). Ví dụ dùng cú pháp PostgreSQL, ghi chú khi MySQL khác biệt đáng kể.

---

## Mục lục

1. [SELECT cơ bản, thứ tự thực thi & NULL](#1-select-cơ-bản-thứ-tự-thực-thi--null)
2. [JOIN — kết hợp dữ liệu từ nhiều bảng](#2-join--kết-hợp-dữ-liệu-từ-nhiều-bảng)
3. [GROUP BY & Aggregate Function](#3-group-by--aggregate-function)
4. [Window Function — tính toán trên "cửa sổ" dữ liệu](#4-window-function--tính-toán-trên-cửa-sổ-dữ-liệu)
5. [Subquery & Set Operations](#5-subquery--set-operations)
6. [Transaction & ACID](#6-transaction--acid)
7. [Isolation Level, khóa & MVCC](#7-isolation-level-khóa--mvcc)
8. [Index — cơ chế & loại](#8-index--cơ-chế--loại)
9. [Database Design — Chuẩn hóa dữ liệu (Normalization)](#9-database-design--chuẩn-hóa-dữ-liệu-normalization)
10. [Khóa chính, khóa ngoại & ràng buộc toàn vẹn](#10-khóa-chính-khóa-ngoại--ràng-buộc-toàn-vẹn)
11. [`EXPLAIN` — phân tích query chậm](#11-explain--phân-tích-query-chậm)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. SELECT cơ bản, thứ tự thực thi & NULL

Giả sử bảng `employees(id, name, department, salary, hire_date)`:

```sql
SELECT name, salary FROM employees;                              -- chỉ lấy cột cần — KHÔNG SELECT * trong production
SELECT * FROM employees WHERE department = 'IT';
SELECT * FROM employees WHERE salary > 15000000 AND department = 'IT';
SELECT * FROM employees ORDER BY department ASC, salary DESC;    -- sắp nhiều cấp (giống thenComparing, Module 02.4)
SELECT * FROM employees LIMIT 10 OFFSET 20;                       -- nền tảng PHÂN TRANG
SELECT DISTINCT department FROM employees;                        -- loại trùng (giống Set, Module 03.1)
```

### Thứ tự THỰC THI thực sự (khác thứ tự viết)

```sql
SELECT   department, AVG(salary)     -- (5)
FROM     employees                    -- (1)
WHERE    hire_date > '2020-01-01'      -- (2) lọc từng dòng, TRƯỚC group
GROUP BY department                   -- (3)
HAVING   AVG(salary) > 15000000        -- (4) lọc SAU group — WHERE không làm được
ORDER BY AVG(salary) DESC              -- (6)
LIMIT    5;                            -- (7)
```

`FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT`. Đây là lý do không dùng được alias đặt trong `SELECT` ở `WHERE` (chạy trước), nhưng dùng được ở `ORDER BY` (chạy sau).

### `CASE WHEN` — điều kiện trong biểu thức

```sql
SELECT name,
       CASE WHEN salary >= 20000000 THEN 'Cao'
            WHEN salary >= 10000000 THEN 'Trung bình'
            ELSE 'Thấp' END AS salary_tier
FROM employees;
```

### NULL — logic ba giá trị (TRUE / FALSE / UNKNOWN)

```sql
WHERE salary = NULL       -- ⚠️ LUÔN cho kết quả UNKNOWN (không phải true/false) — KHÔNG BAO GIỜ trả dòng nào
WHERE salary IS NULL      -- ✅ cách đúng
WHERE salary IS NOT NULL

SELECT COALESCE(phone, 'chưa cập nhật') FROM customers;   -- giá trị THAY THẾ đầu tiên khác NULL
SELECT NULLIF(a, b);                                        -- NULL nếu a = b, ngược lại trả a
```

> `AND`/`OR` với `UNKNOWN` tuân quy tắc riêng: `UNKNOWN AND FALSE = FALSE`, `UNKNOWN OR TRUE = TRUE`, còn lại thường ra `UNKNOWN` — và `WHERE`/`HAVING` chỉ giữ dòng có kết quả **TRUE** (bỏ cả FALSE lẫn UNKNOWN). Đây là gốc rễ của bẫy `NOT IN` + NULL ở mục 5.

---

## 2. JOIN — kết hợp dữ liệu từ nhiều bảng

Thêm bảng `departments(id, name)`, `employees.department_id` tham chiếu `departments.id`.

### INNER JOIN — chỉ dòng khớp cả hai bảng

```sql
SELECT e.name, d.name AS department_name
FROM employees e
INNER JOIN departments d ON e.department_id = d.id;
-- department_id = NULL hoặc trỏ tới department KHÔNG tồn tại → bị loại hoàn toàn
```

### LEFT JOIN — toàn bộ bảng trái

```sql
SELECT e.name, d.name AS department_name
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id;
-- TẤT CẢ nhân viên, kể cả chưa có phòng ban — cột department_name là NULL cho dòng không khớp
```

### RIGHT JOIN

```sql
SELECT e.name, d.name AS department_name
FROM employees e
RIGHT JOIN departments d ON e.department_id = d.id;
-- TẤT CẢ department, kể cả department rỗng
```

> `RIGHT JOIN` hiếm dùng trực tiếp — `A RIGHT JOIN B` luôn viết lại được thành `B LEFT JOIN A`; nhiều team quy ước chỉ dùng `LEFT JOIN` cho nhất quán.

### `CROSS JOIN` — tích Đề-các, mọi tổ hợp

```sql
SELECT s.size, c.color FROM sizes s CROSS JOIN colors c;   -- không có ON — mỗi size ghép với mỗi color
```

Hiếm dùng trực tiếp trong nghiệp vụ; thường xuất hiện **vô tình** khi quên điều kiện `ON` (bug kinh điển: JOIN không điều kiện → số dòng nhân lên theo tích số dòng hai bảng).

### `SELF JOIN` — một bảng, hai vai trò

```sql
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;   -- cùng bảng employees, join với chính nó qua alias khác nhau
```

### `FULL OUTER JOIN`

```sql
-- PostgreSQL/SQL Server hỗ trợ trực tiếp:
SELECT * FROM employees e FULL OUTER JOIN departments d ON e.department_id = d.id;

-- MySQL KHÔNG có FULL OUTER JOIN — mô phỏng bằng UNION của LEFT + RIGHT:
SELECT * FROM employees e LEFT  JOIN departments d ON e.department_id = d.id
UNION
SELECT * FROM employees e RIGHT JOIN departments d ON e.department_id = d.id;
```

### Sơ đồ Venn

```
INNER:   A ∩ B         LEFT: TOÀN BỘ A       RIGHT: TOÀN BỘ B       FULL OUTER: TOÀN BỘ A+B
```

### Nhiều JOIN cùng lúc

```sql
SELECT e.name, d.name AS department, p.name AS project
FROM employees e
INNER JOIN departments d ON e.department_id = d.id
INNER JOIN project_assignments pa ON e.id = pa.employee_id
INNER JOIN projects p ON pa.project_id = p.id;
```

> **Liên hệ Module 11 (JPA/Hibernate):** nền tảng để hiểu **N+1 Query Problem** — không hiểu JOIN sẽ không hiểu vì sao Hibernate sinh hàng nghìn `SELECT` nhỏ lẻ thay vì một câu JOIN khi cấu hình `fetch` sai.

---

## 3. GROUP BY & Aggregate Function

```sql
SELECT department, COUNT(*) AS total_employees
FROM employees GROUP BY department;      -- tương đương Collectors.groupingBy(..., counting()), Module 03.3

SELECT department, AVG(salary), MAX(salary), MIN(salary)
FROM employees GROUP BY department;

SELECT department, SUM(salary) AS total_payroll
FROM employees GROUP BY department
HAVING SUM(salary) > 100000000;           -- lọc SAU group — WHERE không dùng được aggregate
```

| Function | Ý nghĩa |
|---|---|
| `COUNT(*)` | Đếm số dòng |
| `COUNT(column)` | Đếm dòng có giá trị **khác NULL** ở cột đó |
| `SUM()` / `AVG()` | Tổng / trung bình |
| `MAX()` / `MIN()` | Lớn nhất / nhỏ nhất |

> **Bẫy:** `WHERE COUNT(*) > 5` ❌ sai — `WHERE` chạy trước `GROUP BY`. Phải dùng `HAVING`.

### Quy tắc "mọi cột không aggregate phải nằm trong GROUP BY"

Chuẩn SQL yêu cầu: cột nào trong `SELECT` **không** nằm trong hàm aggregate thì **phải** xuất hiện trong `GROUP BY` — nếu không, giá trị trả về không xác định (DB không biết chọn dòng nào đại diện cho nhóm). PostgreSQL luôn chặn vi phạm này; MySQL trước đây cho phép và tự chọn một giá trị "ngẫu nhiên" (dễ ra bug âm thầm), từ MySQL 5.7+ chế độ `ONLY_FULL_GROUP_BY` mặc định bật, chặn giống PostgreSQL.

### `ROLLUP` — thêm dòng tổng (mở rộng, ít dùng hằng ngày)

```sql
SELECT department, SUM(salary) FROM employees GROUP BY ROLLUP(department);
-- thêm 1 dòng department = NULL chứa TỔNG toàn bộ, ngoài các dòng theo từng department
```

---

## 4. Window Function — tính toán trên "cửa sổ" dữ liệu

Khác `GROUP BY` (**gộp** nhiều dòng thành một), window function **giữ nguyên số dòng**, chỉ thêm cột tính theo một "cửa sổ" (window) dữ liệu liên quan.

```sql
SELECT name, department, salary,
       RANK()       OVER (PARTITION BY department ORDER BY salary DESC) AS rank_in_dept,
       DENSE_RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS dense_rank_in_dept,
       ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS row_num,
       SUM(salary)  OVER (PARTITION BY department)                     AS dept_total,
       AVG(salary)  OVER ()                                            AS company_avg
FROM employees;
```

| Hàm | Khi hai dòng bằng nhau (đồng hạng) |
|---|---|
| `ROW_NUMBER()` | Vẫn cấp số **khác nhau tuyệt đối** (1,2,3,4...) — tùy ý ai trước ai sau nếu không `ORDER BY` đủ tiêu chí phân biệt |
| `RANK()` | Cấp **cùng hạng**, rồi **nhảy số** (1,1,3,4) |
| `DENSE_RANK()` | Cấp cùng hạng, **không nhảy số** (1,1,2,3) |

### `LAG`/`LEAD` — so với dòng trước/sau; running total

```sql
SELECT order_date, total_amount,
       LAG(total_amount)  OVER (ORDER BY order_date)                         AS prev_amount,
       LEAD(total_amount) OVER (ORDER BY order_date)                         AS next_amount,
       SUM(total_amount)  OVER (ORDER BY order_date ROWS UNBOUNDED PRECEDING) AS running_total
FROM orders;
```

### Ứng dụng kinh điển: Top-N mỗi nhóm

```sql
SELECT * FROM (
    SELECT *, RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rnk
    FROM employees
) ranked WHERE rnk = 1;    -- nhân viên lương cao nhất TỪNG phòng ban
```

> Window function được đánh giá **sau** `WHERE`/`GROUP BY`/`HAVING` nhưng **trước** `ORDER BY`/`LIMIT` cuối cùng — vì vậy không dùng trực tiếp trong `WHERE` (`WHERE rnk = 1` ❌ sai cùng lý do với aggregate ở `WHERE`) — phải bọc trong subquery/CTE như trên.

---

## 5. Subquery & Set Operations

### Subquery không tương quan (non-correlated) — chạy độc lập, một lần

```sql
SELECT name, salary FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees);
```

### Subquery tương quan (correlated) — tham chiếu cột ngoài, chạy lại cho mỗi dòng

```sql
SELECT e1.* FROM employees e1
WHERE e1.salary = (SELECT MAX(e2.salary) FROM employees e2 WHERE e2.department = e1.department);
-- subquery bên trong dùng e1.department (đến từ query NGOÀI) → phải chạy lại cho TỪNG dòng của e1
```

### Subquery trong `FROM` (derived table) & CTE

```sql
SELECT dept_summary.department, dept_summary.avg_salary
FROM (SELECT department, AVG(salary) AS avg_salary FROM employees GROUP BY department) AS dept_summary
WHERE dept_summary.avg_salary > 15000000;

WITH department_summary AS (
    SELECT department, AVG(salary) AS avg_salary FROM employees GROUP BY department
)
SELECT * FROM department_summary WHERE avg_salary > 15000000;
```

> Nhiều tầng subquery lồng nhau → ưu tiên **CTE (`WITH`)**: đọc từ trên xuống như các bước tuần tự, thay vì "đọc từ trong ra ngoài".

### `EXISTS` vs `IN` — và bẫy `NOT IN` với NULL

```sql
SELECT name FROM customers c WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
SELECT name FROM customers c WHERE c.id IN (SELECT customer_id FROM orders);
```

Tương đương nhau khi cột không có `NULL`. `EXISTS` thường được optimizer xử lý tốt hơn cho subquery lớn (dừng ngay khi thấy **một** dòng khớp — short-circuit).

```sql
-- ⚠️ BẪY KINH ĐIỂN:
SELECT name FROM customers
WHERE id NOT IN (SELECT customer_id FROM orders);
-- Nếu orders.customer_id có DÙ CHỈ MỘT dòng NULL → NOT IN trả về RỖNG cho MỌI khách hàng!
-- Lý do: x <> NULL luôn là UNKNOWN (mục 1) — "NOT IN (..., NULL)" không bao giờ chắc chắn TRUE cho bất kỳ x nào.

-- SỬA — dùng NOT EXISTS (an toàn tuyệt đối với NULL):
SELECT name FROM customers c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
```

### Set Operations — gộp kết quả nhiều câu SELECT

```sql
SELECT email FROM customers
UNION                                  -- gộp, TỰ ĐỘNG loại trùng (tốn công sort/dedupe)
SELECT email FROM newsletter_subscribers;

SELECT email FROM customers
UNION ALL                              -- gộp, GIỮ trùng — nhanh hơn, dùng khi chắc chắn không trùng hoặc trùng không sao
SELECT email FROM newsletter_subscribers;

SELECT customer_id FROM orders_2025
INTERSECT SELECT customer_id FROM orders_2026;   -- chỉ giữ phần GIAO

SELECT customer_id FROM all_customers
EXCEPT SELECT customer_id FROM churned_customers; -- (MINUS ở Oracle) — có ở vế 1, KHÔNG có ở vế 2
```

Yêu cầu: các `SELECT` phải **cùng số cột**, kiểu dữ liệu tương thích theo vị trí.

---

## 6. Transaction & ACID

```sql
BEGIN TRANSACTION;
UPDATE accounts SET balance = balance - 1000000 WHERE id = 'A';
UPDATE accounts SET balance = balance + 1000000 WHERE id = 'B';
COMMIT;
-- Lỗi giữa chừng: ROLLBACK; — A được trả lại như chưa từng bị trừ
```

> **Autocommit:** không có `BEGIN` tường minh → hầu hết DB chạy chế độ **autocommit** — mỗi câu lệnh riêng lẻ tự là một transaction ngầm, COMMIT ngay khi chạy xong. Đây là lý do 2 câu `UPDATE` rời nhau (không bọc transaction) có thể để "tiền biến mất" nếu crash giữa hai câu — câu đầu đã COMMIT thật sự.

### ACID

| Chữ | Tên | Ý nghĩa |
|---|---|---|
| **A** | Atomicity | Toàn bộ thao tác thành công hết hoặc thất bại hết |
| **C** | Consistency | DB luôn chuyển từ trạng thái hợp lệ sang trạng thái hợp lệ khác — không vi phạm ràng buộc (FK, `NOT NULL`, `CHECK`) |
| **I** | Isolation | Nhiều transaction đồng thời không ảnh hưởng sai lẫn nhau — mục 7 |
| **D** | Durability | Sau `COMMIT`, dữ liệu tồn tại vĩnh viễn dù crash ngay sau đó |

### Cơ chế đứng sau Durability — Write-Ahead Log (WAL)

Trước khi sửa dữ liệu thật trên đĩa, DB ghi thay đổi vào một **log tuần tự** (ghi log nhanh hơn ghi ngẫu nhiên vào file dữ liệu); nếu crash giữa chừng, khởi động lại DB **phát lại (replay)** log để khôi phục đúng trạng thái đã `COMMIT`. `fsync` đảm bảo log thực sự nằm trên đĩa vật lý (không chỉ cache OS) trước khi báo `COMMIT` thành công cho client.

### `SAVEPOINT` — rollback một phần trong transaction

```sql
BEGIN;
UPDATE accounts SET balance = balance - 1000 WHERE id = 'A';
SAVEPOINT before_bonus;
UPDATE accounts SET balance = balance + 100 WHERE id = 'A';    -- giả sử logic này lỗi
ROLLBACK TO SAVEPOINT before_bonus;   -- chỉ hủy phần SAU savepoint, giữ phần trừ tiền trước đó
COMMIT;
```

> **Liên hệ Module 12/14 (Spring):** `@Transactional` là cách Spring **tự động** quản lý `BEGIN`/`COMMIT`/`ROLLBACK` — method có `@Transactional` ném **unchecked exception** thì Spring tự `ROLLBACK`, đảm bảo Atomicity mà không cần viết SQL thủ công như trên.

---

## 7. Isolation Level, khóa & MVCC

### Ba (bốn) hiện tượng khi nhiều transaction chạy đồng thời

| Hiện tượng | Mô tả |
|---|---|
| **Dirty Read** | A đọc dữ liệu **chưa commit** của B — nếu B `ROLLBACK`, A đã đọc dữ liệu "không bao giờ tồn tại thật" |
| **Non-Repeatable Read** | Trong cùng transaction, A đọc 1 dòng 2 lần ra **giá trị khác nhau** — vì B đã `UPDATE` + commit ở giữa |
| **Phantom Read** | A chạy cùng 1 query 2 lần, ra **số dòng khác nhau** — vì B đã `INSERT`/`DELETE` dòng khớp điều kiện ở giữa |
| **Lost Update** | A và B cùng **đọc** 1 giá trị, cùng tính toán dựa trên nó, cùng **ghi đè** — một trong hai lần cập nhật "biến mất" |

### 4 Isolation Level chuẩn SQL

```
READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ → SERIALIZABLE
   (nhanh, rủi ro cao)                              (chậm, an toàn nhất)
```

| Level | Dirty Read | Non-Repeatable | Phantom |
|---|---|---|---|
| `READ UNCOMMITTED` | ❌ | ❌ | ❌ |
| `READ COMMITTED` (mặc định PostgreSQL, Oracle) | ✅ | ❌ | ❌ |
| `REPEATABLE READ` (mặc định MySQL/InnoDB) | ✅ | ✅ | ❌ theo chuẩn lý thuyết (InnoDB thực tế ngăn được nhờ cơ chế riêng — xem MVCC dưới) |
| `SERIALIZABLE` | ✅ | ✅ | ✅ |

> Càng chặt càng an toàn nhưng càng chậm (nhiều transaction chờ/khóa nhau hơn — cùng bản chất đánh đổi với `synchronized`, Module 05.1). Đa số backend dùng mặc định của DB là đủ; `SERIALIZABLE` cho nghiệp vụ cực nhạy cảm (tồn kho Flash-Sale).

### MVCC — cơ chế thực tế của PostgreSQL/InnoDB

Thực tế PostgreSQL/InnoDB không chỉ dựa vào khóa (locking) như bảng lý thuyết trên gợi ý, mà dùng **Multi-Version Concurrency Control (MVCC)**: mỗi transaction thấy một **ảnh chụp (snapshot)** nhất quán của dữ liệu tại thời điểm bắt đầu; `UPDATE` tạo **phiên bản mới** của dòng thay vì ghi đè ngay, phiên bản cũ vẫn còn cho transaction khác đang đọc dở. Nhờ vậy `REPEATABLE READ` đạt phần lớn hiệu quả **mà không cần khóa đọc** — giảm tranh chấp (contention) rất nhiều so với mô hình khóa thuần túy.

### Khóa tường minh — bi quan (pessimistic)

```sql
SELECT * FROM tickets WHERE id = 1 FOR UPDATE;   -- khóa ĐỘC QUYỀN (exclusive) — transaction khác PHẢI CHỜ
SELECT * FROM tickets WHERE id = 1 FOR SHARE;     -- khóa CHIA SẺ — nhiều transaction cùng đọc, KHÔNG ai ghi được tới khi nhả
```

`FOR UPDATE` là **khóa bi quan**: giả định xung đột chắc chắn xảy ra, khóa trước. Row-level lock (khóa đúng dòng, không cả bảng) là mặc định của các engine hiện đại. Hai transaction cùng chờ khóa của nhau theo vòng tròn → **deadlock ở tầng DB** — cùng bản chất Circular Wait (Module 05.1); DB tự phát hiện và **chủ động abort** một trong hai transaction (thường ném lỗi để tầng ứng dụng bắt và thử lại).

### Khóa lạc quan (optimistic locking)

Không khóa gì trước — thêm cột `version`; khi `UPDATE` kiểm tra `WHERE id = ? AND version = <bản đã đọc>`; **0 dòng bị ảnh hưởng** nghĩa là ai đó đã sửa trước → tầng ứng dụng báo lỗi/thử lại.

```sql
UPDATE tickets SET stock = stock - 1, version = version + 1
WHERE id = 1 AND version = 5;    -- 0 dòng ảnh hưởng ⇒ có transaction khác đã cập nhật version trước
```

Hiệu quả hơn khóa bi quan khi xung đột **hiếm** (ít khóa, ít chờ). Đây chính là ý nghĩa của `@Version` trong JPA (Module 11/14).

---

## 8. Index — cơ chế & loại

**Index** là cấu trúc dữ liệu phụ (thường **B-Tree**, cùng ý tưởng `TreeMap`, Module 03.1) giúp tìm dữ liệu nhanh mà không quét toàn bảng.

```sql
SELECT * FROM employees WHERE email = 'pho@example.com';    -- không index → Seq Scan, O(n)
CREATE INDEX idx_employees_email ON employees(email);
SELECT * FROM employees WHERE email = 'pho@example.com';    -- có index → Index Scan, ~O(log n)
```

Ví dụ trực quan: mục lục cuối sách — không có phải lật từng trang; có thì tra tên, nhảy thẳng tới trang.

### Đánh đổi

| Lợi | Chi phí |
|---|---|
| Tăng tốc **đọc** đáng kể | Làm **chậm ghi** — mỗi `INSERT`/`UPDATE`/`DELETE` phải cập nhật **lại mọi index** liên quan tới cột bị đổi |
| | Tốn thêm bộ nhớ/đĩa lưu index |

> Tạo index cho cột hay xuất hiện trong `WHERE`/`JOIN ON`/`ORDER BY` — đặc biệt **khóa ngoại** (không tự động có index ở PostgreSQL/SQL Server, khác khóa chính luôn có). Tránh index bừa bãi ở bảng ghi nhiều mà ít truy vấn theo cột đó.

### Composite index & Leftmost Prefix

```sql
CREATE INDEX idx_employees_dept_salary ON employees(department, salary);
-- WHERE department='IT' AND salary>15000000   → tận dụng TỐI ĐA
-- WHERE salary>15000000 (không có department) → KHÔNG tận dụng hiệu quả (thiếu cột ĐẦU TIÊN)
```

**Leftmost Prefix**: composite index chỉ hữu ích khi truy vấn dùng được **tiền tố liên tục từ cột đầu**. Nguyên tắc đặt thứ tự cột: **cột so sánh bằng (`=`) đặt trước**, cột so sánh khoảng (`>`, `<`, `BETWEEN`) đặt **sau**.

### Clustered vs Secondary index — vì sao PK ngắn quan trọng

InnoDB (MySQL) lưu bảng **vật lý theo thứ tự khóa chính** — gọi là **clustered index** (PK chính là cách dữ liệu sắp trên đĩa). Mọi index khác (**secondary index**) chỉ lưu `(giá trị cột, con trỏ về PK)` — tra secondary index xong phải "nhảy" thêm bước về clustered index để lấy đủ dữ liệu (**bookmark lookup**). Vì mọi secondary index đều "cõng" một bản sao của PK, **PK ngắn gọn** (`BIGINT` thay vì UUID/text dài) giữ mọi index khác nhỏ gọn theo.

### Covering index — tránh hẳn bước "nhảy về bảng chính"

```sql
CREATE INDEX idx_covering ON employees(department, salary) INCLUDE (name);
SELECT name FROM employees WHERE department = 'IT' ORDER BY salary DESC;
-- MỌI cột cần (department, salary, name) đều nằm sẵn trong index → KHÔNG cần đọc bảng chính
-- EXPLAIN hiện "Index Only Scan" thay vì "Index Scan"
```

### Cardinality — index không phải lúc nào cũng giúp

**Cardinality** = số giá trị khác nhau trong cột / tổng số dòng. Index trên cột cardinality **thấp** (ví dụ `is_active` chỉ `true`/`false`) hiếm khi hữu ích — lọc theo cột đó không loại được nhiều dòng, optimizer thường vẫn chọn Seq Scan.

### Loại index khác (biết tên, chi tiết ngoài phạm vi)

| Loại | Dùng khi |
|---|---|
| B-Tree (mặc định) | Hầu hết trường hợp — `=`, `<`, `>`, `BETWEEN`, sắp xếp |
| Hash | Chỉ `=` tuyệt đối, không hỗ trợ range |
| GIN/GiST (PostgreSQL) | JSON, mảng, full-text search |
| Bitmap (Oracle, data warehouse) | Cột cardinality thấp trong OLAP |

> `UNIQUE` constraint và `CREATE UNIQUE INDEX` thực chất là **một** — `UNIQUE` tự tạo một unique index bên dưới để enforce.

---

## 9. Database Design — Chuẩn hóa dữ liệu (Normalization)

Tổ chức bảng để **giảm dư thừa** và **tránh bất thường dữ liệu (anomaly)** khi thêm/sửa/xóa.

### Vấn đề khi không chuẩn hóa

```
orders (TỆ)
┌────┬─────────────┬──────────────┬────────────────┐
│ id │ product_name│ customer_name│ customer_email   │
├────┼─────────────┼──────────────┼────────────────┤
│ 1  │ Laptop      │ Pho          │ pho@example.com  │
│ 2  │ Mouse       │ Pho          │ pho@example.com  │ ← lặp lại
└────┴─────────────┴──────────────┴────────────────┘
```

"Pho" đổi email → phải `UPDATE` nhiều dòng cùng lúc, dễ sót → **Update Anomaly**.

### 1NF — mỗi ô một giá trị nguyên tử

```
❌ phone_numbers = "0901234567, 0987654321"    (nhiều giá trị trong 1 ô)
✅ customer_phones(customer_id, phone_number)   (mỗi dòng 1 số)
```

### 2NF — không "Partial Dependency" (khi khóa chính ghép nhiều cột)

```
❌ order_items(order_id, product_id, product_name, quantity)
   -- product_name chỉ phụ thuộc product_id, KHÔNG cần order_id
✅ order_items(order_id, product_id, quantity)
   products(product_id, product_name)
```

### 3NF — không "Transitive Dependency"

```
❌ employees(id, department_id, department_name)
   -- id → department_id → department_name (phụ thuộc BẮC CẦU, không trực tiếp từ id)
✅ employees(id, department_id)
   departments(id, name)
```

### BCNF (Boyce-Codd Normal Form) — bản chặt hơn 3NF

Xử lý trường hợp hiếm gặp còn sót khi bảng có **nhiều khóa ứng viên (candidate key)** chồng chéo lẫn nhau. Hầu hết schema backend thiết kế đúng 3NF trong thực tế đã tự động đạt BCNF; phân biệt sâu hơn nằm ngoài phạm vi bài này.

### Áp dụng thực tế

```sql
customers(id, name, email)
products(id, name, price)
orders(id, customer_id, order_date)
order_items(order_id, product_id, quantity)
```

Đổi email "Pho" — chỉ **một** `UPDATE` trong `customers`, mọi `orders` tự "thấy" thông tin mới qua `customer_id`.

### Denormalization — phá vỡ chuẩn hóa có chủ đích

Chuẩn hóa nhiều bảng hơn → nhiều JOIN hơn. Traffic đọc rất lớn → đôi khi chấp nhận **lưu dư thừa có chủ đích** (ví dụ lưu sẵn `product_name` trong `order_items` tại thời điểm đặt hàng) để tránh JOIN, tăng tốc đọc — kỹ thuật tối ưu ở hệ thống quy mô lớn (System Design).

---

## 10. Khóa chính, khóa ngoại & ràng buộc toàn vẹn

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    salary DECIMAL(12,2) CHECK (salary >= 0),
    department_id BIGINT,
    FOREIGN KEY (department_id) REFERENCES departments(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE          -- id departments đổi (hiếm với surrogate key) → tự cập nhật theo
);
```

| `ON DELETE` | Ý nghĩa |
|---|---|
| `CASCADE` | Xóa luôn dòng liên quan (xóa `order` → tự xóa `order_items`) |
| `SET NULL` | Đặt FK thành `NULL` |
| `RESTRICT` (mặc định) | Ngăn xóa nếu còn dòng tham chiếu — ném lỗi |

> `CASCADE` cần cẩn thận — dễ xóa nhầm hàng loạt. Nhiều team ưu tiên **Soft Delete** (`is_deleted = true`) thay vì xóa thật (Module 11).

### Natural key vs Surrogate key

| | Natural key | Surrogate key |
|---|---|---|
| Ví dụ | email, mã số thuế, ISBN | `BIGINT` tự tăng, `UUID` |
| Ý nghĩa nghiệp vụ | Có | Không |
| Rủi ro | Giá trị nghiệp vụ **có thể đổi** (email đổi) → PK "phải đổi theo" → cascade tốn kém khắp FK | Không bao giờ cần đổi |
| Khuyến nghị | Chỉ khi thực sự bất biến | **Mặc định** cho khóa chính |

### `BIGINT` tự tăng vs `UUID` làm khóa

| | `BIGINT AUTO_INCREMENT`/`IDENTITY` | `UUID` |
|---|---|---|
| Kích thước | 8 byte | 16 byte — index lớn hơn |
| Lộ thông tin (đoán được số lượng/thứ tự) | Có (rủi ro nghiệp vụ, VD số đơn hàng) | Không |
| Sinh phân tán (nhiều service không đụng nhau) | Khó, cần phối hợp | Dễ, gần như không đụng độ |
| Chèn vào B-Tree | Luôn ở cuối — không phân mảnh | Ngẫu nhiên → chèn giữa cây → phân mảnh (UUIDv7 theo thời gian khắc phục) |

### Khóa chính ghép (composite PK)

```sql
CREATE TABLE post_tags (
    post_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (tag_id)  REFERENCES tags(id)
);
```

---

## 11. `EXPLAIN` — phân tích query chậm

```sql
EXPLAIN SELECT * FROM employees WHERE email = 'pho@example.com';
```
```
Seq Scan on employees  (cost=0.00..1834.00 rows=1 width=120)
  Filter: (email = 'pho@example.com'::text)
```

`Seq Scan` = quét tuần tự (không dùng index). Sau khi tạo index:

```
Index Scan using idx_employees_email on employees  (cost=0.29..8.31 rows=1 width=120)
  Index Cond: (email = 'pho@example.com'::text)
```

`cost` giảm mạnh (1834 → 8.31) — bằng chứng index có hiệu quả.

### Đọc `cost` đúng cách

```
cost=0.00..1834.00 rows=5000000 width=64
      ^start   ^total  ^ước tính số dòng  ^ước tính byte/dòng
```

`cost` là đơn vị **tương đối** (không phải mili-giây thật), dựa trên thống kê bảng — chỉ để **so sánh giữa các kế hoạch (plan) khả thi mà optimizer đang cân nhắc**, không phải để so tuyệt đối giữa hai query khác nhau. Optimizer duyệt nhiều plan, ước lượng cost, chọn plan **rẻ nhất tìm được** — không đảm bảo là tối ưu tuyệt đối.

### Ba thuật toán JOIN thường gặp trong plan

| Thuật toán | Cơ chế | Hiệu quả khi |
|---|---|---|
| **Nested Loop** | Với mỗi dòng bảng ngoài, quét/tra index bảng trong | Một bên rất nhỏ, hoặc có index tốt cho điều kiện join |
| **Hash Join** | Dựng bảng băm từ bảng nhỏ hơn (trong RAM), quét bảng lớn đối chiếu | Cả hai bảng lớn, không có index phù hợp |
| **Merge Join** | Cả hai đã **sắp xếp** theo cột join, "trộn" song song | Dữ liệu đã sort sẵn nhờ index |

### `EXPLAIN ANALYZE` — chạy thật, đo thời gian thật

```sql
EXPLAIN ANALYZE SELECT * FROM employees WHERE email = 'pho@example.com';
-- Ngoài cost ước tính: thời gian THỰC TẾ, số dòng THỰC TẾ — chính xác hơn EXPLAIN thường
```

### Quy trình chẩn đoán chuẩn

1. Xác định query chậm (log, monitoring — Module 21).
2. `EXPLAIN ANALYZE`.
3. Tìm dấu hiệu bất thường: `Seq Scan` trên bảng lớn, số dòng **ước tính** lệch xa số dòng **thực tế** (thống kê bảng đã cũ, cần `ANALYZE`/`VACUUM ANALYZE`), `Nested Loop` không hiệu quả trên JOIN nhiều dòng.
4. Thêm index phù hợp hoặc viết lại query.
5. Chạy lại `EXPLAIN ANALYZE` để **xác nhận** cải thiện thật, không đoán mò.

> Thực tế backend không chờ người dùng báo "chậm" mới `EXPLAIN` — bật **slow query log** (MySQL) / `pg_stat_statements` (PostgreSQL) để tự động ghi lại query vượt ngưỡng, giám sát định kỳ (Module 21).

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Thứ tự thực thi | `FROM→WHERE→GROUP BY→HAVING→SELECT→ORDER BY→LIMIT` — khác thứ tự viết |
| NULL | Ba giá trị logic; `= NULL` luôn sai, dùng `IS NULL`; `NOT IN` + NULL → rỗng toàn bộ, dùng `NOT EXISTS` |
| JOIN | INNER giao nhau; LEFT/RIGHT toàn bộ 1 bên; CROSS = tích Đề-các (thường do quên `ON`); SELF = 1 bảng 2 vai trò |
| HAVING vs WHERE | WHERE lọc trước group (không aggregate được); HAVING lọc sau |
| Window Function | Giữ nguyên số dòng, thêm cột tính theo cửa sổ; `RANK`/`DENSE_RANK`/`ROW_NUMBER` khác nhau khi đồng hạng |
| Correlated subquery | Tham chiếu cột ngoài → chạy lại mỗi dòng. `EXISTS`/`NOT EXISTS` an toàn hơn `IN`/`NOT IN` với NULL |
| ACID | Atomicity/Consistency/Isolation/Durability; Durability nhờ WAL; autocommit = mỗi câu lệnh = 1 transaction ngầm |
| `SAVEPOINT` | Rollback một phần trong transaction |
| Isolation | Dirty/Non-repeatable/Phantom/Lost Update; càng chặt càng chậm; MVCC = snapshot + versioning, ít lock hơn lý thuyết |
| Khóa | `FOR UPDATE` (exclusive, bi quan) vs `FOR SHARE`; optimistic locking dùng cột `version` (→ `@Version` JPA) |
| Index | Tăng đọc, chậm ghi; Leftmost Prefix (`=` trước, range sau); clustered (PK) vs secondary (bookmark lookup); covering index tránh lookup; cardinality thấp → index vô ích |
| Chuẩn hóa | 1NF/2NF/3NF/BCNF giảm dư thừa; Denormalization đánh đổi có chủ đích cho tốc độ đọc |
| Khóa chính | Surrogate key (mặc định) an toàn hơn natural key; `BIGINT` tự tăng nhỏ gọn hơn UUID; composite PK cho bảng trung gian |
| `EXPLAIN` | `cost` tương đối, chỉ so giữa các plan; Nested Loop/Hash Join/Merge Join; `EXPLAIN ANALYZE` đo thật |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Sửa lỗi:
```sql
SELECT department, COUNT(*) FROM employees WHERE COUNT(*) > 5 GROUP BY department;
```

**Câu 2.** 10 nhân viên, 2 người `department_id = NULL`. `INNER JOIN` và `LEFT JOIN` với `departments` trả về bao nhiêu dòng mỗi loại (còn lại đều khớp hợp lệ)?

**Câu 3.** Không transaction, server crash ngay sau dòng đầu:
```java
statement.execute("UPDATE accounts SET balance = balance - 500 WHERE id = 'A'");
// [CRASH]
statement.execute("UPDATE accounts SET balance = balance + 500 WHERE id = 'B'");
```
Điều gì xảy ra? Liên hệ khái niệm autocommit.

**Câu 4.** `products(id, category_id, category_name, price)` vi phạm chuẩn hóa nào? Sửa thế nào?

**Câu 5.** `EXPLAIN` cho thấy `Seq Scan` trên bảng 5 triệu dòng với `WHERE status = 'ACTIVE'`. Đây có luôn là dấu hiệu cần index không? (Gợi ý: 4.9 triệu/5 triệu dòng đều `ACTIVE`.)

**Câu 6.** Đoạn sau sai ở đâu, sửa thế nào?
```sql
SELECT name FROM customers WHERE id NOT IN (SELECT customer_id FROM orders);
-- orders.customer_id có 1 dòng NULL (đơn hàng khách vãng lai, chưa gán customer)
```

**Câu 7.** Hai transaction cùng chạy `SELECT stock FROM tickets WHERE id=1` (không `FOR UPDATE`) rồi cùng `UPDATE tickets SET stock = stock - 1`. Đặt tên hiện tượng này. Nêu hai cách sửa khác triết lý nhau.

**Câu 8.** Vì sao PostgreSQL/MySQL không tự tạo index cho cột khóa ngoại (trừ InnoDB ở một số trường hợp), trong khi khóa chính luôn tự có index? Hệ quả nếu quên tạo index cho FK hay dùng trong JOIN?

---

### Phần B — Bài tập viết SQL

**Bài 1 — JOIN & GROUP BY tổng hợp.**
`customers(id, name)`, `orders(id, customer_id, order_date, total_amount)`. Viết: (a) tên khách hàng + tổng chi tiêu, giảm dần; (b) chỉ khách chi tiêu > 5.000.000; (c) khách **chưa từng đặt hàng** vẫn xuất hiện với tổng = 0 (gợi ý `LEFT JOIN` + `COALESCE`).

**Bài 2 — Window Function cho Top-N mỗi nhóm.**
`employees(id, name, department, salary)`. Viết 2 cách tìm lương cao nhất **từng phòng ban**: (a) correlated subquery, (b) `RANK() OVER (PARTITION BY ...)`. Viết thêm câu dùng `LAG()` để so lương mỗi nhân viên với người xếp **ngay trên** (theo `salary DESC`) trong cùng phòng ban.

**Bài 3 — Thiết kế schema chuẩn hóa cho Blog.**
`CREATE TABLE` đầy đủ: `users`, `posts` (1 tác giả/bài), `tags`, quan hệ nhiều-nhiều `posts`↔`tags` qua bảng trung gian với khóa chính ghép. Tuân thủ 3NF.

**Bài 4 — Transaction đặt vé với khóa bi quan.**
Viết SQL: `SELECT ... FOR UPDATE` kiểm tra còn vé, giảm số vé, insert `bookings`. Comment giải thích vì sao cần `FOR UPDATE` (liên hệ Race Condition Module 05.1, tầng DB).

**Bài 5 — Chẩn đoán & tối ưu query chậm.**
```sql
SELECT * FROM orders WHERE customer_id = 12345 AND order_date > '2026-01-01' ORDER BY order_date DESC;
```
(bảng `orders` 10 triệu dòng). Đề xuất index (đơn/composite, thứ tự cột theo Leftmost Prefix), viết `CREATE INDEX`, mô tả cách xác nhận bằng `EXPLAIN ANALYZE`.

**Bài 6 — Khóa lạc quan cho oversold.**
Viết lại Bài 4 bằng **optimistic locking** (cột `version`), không dùng `FOR UPDATE`. So sánh (bằng comment) khi nào cách này tốt hơn/kém hơn pessimistic locking.

---

### Phần C — Nâng cao

**Câu 1.** Giải thích chính xác vì sao `NOT IN` với subquery chứa `NULL` cho kết quả rỗng toàn bộ, dùng logic ba giá trị (TRUE/FALSE/UNKNOWN). Viết một test case (dữ liệu mẫu 3 dòng) chứng minh, và chứng minh `NOT EXISTS` cho kết quả đúng trên cùng dữ liệu.

**Câu 2.** So sánh MVCC (PostgreSQL/InnoDB) với mô hình khóa hai giai đoạn thuần túy (2PL — two-phase locking) về: throughput khi đọc nhiều/ghi ít, khả năng reader chặn writer (và ngược lại), chi phí "dọn rác" phiên bản cũ (`VACUUM` ở PostgreSQL).

**Câu 3.** Composite index `(department, salary)` — viết ba câu `WHERE` khác nhau: một tận dụng tối đa, một tận dụng một phần (chỉ dùng được `department`), một không tận dụng được gì. Giải thích từng trường hợp qua "Leftmost Prefix". Covering index khác gì so với composite index thường ở kết quả `EXPLAIN`?

**Câu 4.** Vì sao UUID ngẫu nhiên làm khóa chính có thể khiến hiệu năng `INSERT` giảm dần theo thời gian trên bảng lớn, trong khi `BIGINT` tự tăng thì không? Giải thích bằng cấu trúc B-Tree/clustered index. UUIDv7 giải quyết vấn đề này bằng cách nào?

**Câu 5.** Cho ví dụ cụ thể (số liệu database) minh họa Lost Update mà `REPEATABLE READ` (theo MVCC) **không** tự động ngăn được nếu ứng dụng không chủ động `SELECT ... FOR UPDATE` hoặc dùng optimistic locking. Vì sao MVCC "mỗi transaction thấy snapshot riêng" không đồng nghĩa với "an toàn tuyệt đối với ghi đồng thời"?

**Câu 6.** `EXPLAIN` cho `cost` ước tính dựa trên thống kê bảng (histogram, cardinality) mà optimizer lưu trữ. Điều gì xảy ra nếu thống kê **đã cũ** (bảng vừa tăng đột biến số dòng nhưng chưa `ANALYZE` lại)? Optimizer có thể chọn sai plan như thế nào, và cách khắc phục?

**Câu 7.** So sánh optimistic locking (`version` column) và pessimistic locking (`FOR UPDATE`) trên ba trục: hiệu năng khi xung đột hiếm, hiệu năng khi xung đột thường xuyên, trải nghiệm lập trình (ai xử lý retry, lỗi hiển thị cho người dùng thế nào). Đề xuất khi nào dùng cái nào cho hệ thống Flash-Sale (nhiều người tranh mua cùng 1 sản phẩm số lượng giới hạn).

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. `COUNT(*)` không dùng được ở `WHERE`. Sửa: `SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5;`
2. `INNER JOIN` → **8 dòng** (2 người NULL bị loại). `LEFT JOIN` → **10 dòng** (tất cả, cột department NULL cho 2 người đó).
3. Do autocommit, câu `UPDATE` đầu **đã COMMIT thật** ngay khi chạy xong (mỗi câu lệnh riêng là 1 transaction ngầm) — A mất 500 vĩnh viễn, B chưa kịp cộng do crash trước câu 2 → 500 biến mất khỏi hệ thống. Sửa: bọc `BEGIN...COMMIT` quanh cả hai câu.
4. Vi phạm 3NF (Transitive Dependency) — `category_name` phụ thuộc `category_id`, không trực tiếp từ `id`. Sửa: tách `categories(id, name)`, `products` giữ `category_id` làm FK.
5. Không luôn đúng — nếu 4.9/5 triệu dòng đều `ACTIVE` (điều kiện gần như không lọc được gì), dùng index có thể **chậm hơn** Seq Scan (thêm bước nhảy về bảng chính cho gần hết dữ liệu). Optimizer thường tự nhận ra và chọn Seq Scan dù có index — luôn cần `EXPLAIN ANALYZE` để kiểm chứng, không suy luận suông.
6. `x <> NULL` luôn `UNKNOWN` → `NOT IN (..., NULL)` không bao giờ chắc chắn `TRUE` cho bất kỳ `x` nào → mọi khách hàng bị loại khỏi kết quả. Sửa: `SELECT name FROM customers c WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);`
7. **Lost Update.** Hai cách sửa: (a) pessimistic — `SELECT ... FOR UPDATE` trước khi tính toán/ghi; (b) optimistic — thêm cột `version`, `UPDATE ... WHERE id=? AND version=?`, kiểm tra số dòng ảnh hưởng.
8. Khóa chính gắn liền với **cấu trúc lưu trữ vật lý** (clustered index ở InnoDB) nên bắt buộc có index để đảm bảo tính duy nhất hiệu quả; khóa ngoại chỉ là một **ràng buộc logic** (kiểm tra tồn tại), DB không bắt buộc phải index nó để enforce ràng buộc — chỉ cần quét tra cứu khi insert/delete. Quên index FK: JOIN theo cột đó và các thao tác `ON DELETE`/`ON UPDATE` phải Seq Scan bảng con để tìm dòng liên quan — chậm nghiêm trọng trên bảng lớn.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1(c):**
```sql
SELECT c.name, COALESCE(SUM(o.total_amount), 0) AS total_spent
FROM customers c LEFT JOIN orders o ON c.id = o.customer_id
GROUP BY c.id, c.name
HAVING COALESCE(SUM(o.total_amount), 0) > 5000000
ORDER BY total_spent DESC;
```
- **Bài 2:** (a) `SELECT e1.* FROM employees e1 WHERE e1.salary = (SELECT MAX(e2.salary) FROM employees e2 WHERE e2.department = e1.department);` (b) `SELECT * FROM (SELECT *, RANK() OVER (PARTITION BY department ORDER BY salary DESC) rnk FROM employees) x WHERE rnk = 1;` `LAG`: `SELECT name, department, salary, LAG(salary) OVER (PARTITION BY department ORDER BY salary DESC) AS salary_above FROM employees;`
- **Bài 3:** `post_tags(post_id, tag_id, PRIMARY KEY(post_id, tag_id), FOREIGN KEY(post_id) REFERENCES posts(id), FOREIGN KEY(tag_id) REFERENCES tags(id))`.
- **Bài 4:** `SELECT available FROM tickets WHERE id=1 FOR UPDATE;` (khóa dòng) → kiểm tra `available > 0` trong code → `UPDATE tickets SET available = available - 1 WHERE id = 1;` → `INSERT INTO bookings(...)` → `COMMIT`. Comment: không `FOR UPDATE` → 2 transaction cùng đọc "còn 1 vé" trước khi ai kịp `COMMIT` → cả 2 đặt thành công → oversold.
- **Bài 5:** `CREATE INDEX idx_orders_customer_date ON orders(customer_id, order_date DESC);` — `customer_id` (equality) trước, `order_date` (range + cùng chiều `ORDER BY DESC`) sau. Xác nhận: `EXPLAIN ANALYZE` trước/sau, so `Seq Scan`→`Index Scan` và thời gian thực tế giảm.
- **Bài 6:** `UPDATE tickets SET available = available - 1, version = version + 1 WHERE id = 1 AND version = :readVersion;` kiểm tra số dòng ảnh hưởng ở tầng ứng dụng, 0 dòng → retry hoặc báo lỗi. Tốt hơn khi xung đột **hiếm** (không tốn chi phí khóa mỗi lần đọc); kém hơn khi xung đột **thường xuyên** (nhiều lần retry lãng phí, trải nghiệm người dùng tệ hơn khi phải báo lỗi "thử lại").

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. `NOT IN (v1, v2, NULL)` khai triển tương đương `x<>v1 AND x<>v2 AND x<>NULL`; vế cuối luôn `UNKNOWN`; `TRUE AND TRUE AND UNKNOWN = UNKNOWN` → `WHERE` loại bỏ mọi dòng (chỉ giữ `TRUE`). Dữ liệu mẫu: `customers(id)`=(1),(2); `orders(customer_id)`=(1),(NULL). `id NOT IN (SELECT customer_id FROM orders)` → với `id=2`: `2<>1 AND 2<>NULL` = `TRUE AND UNKNOWN` = `UNKNOWN` → bị loại; với `id=1` cũng bị loại tương tự → kết quả **rỗng** dù khách `2` rõ ràng không có đơn nào. `NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id)`: với `id=2`, không dòng nào `orders.customer_id = 2` (kể cả dòng NULL, vì `NULL = 2` là `UNKNOWN` chứ không `TRUE`) → `NOT EXISTS` = `TRUE` → giữ đúng khách `2`.
2. MVCC: reader không chặn writer và writer không chặn reader (đọc snapshot cũ trong khi ghi tạo phiên bản mới) → throughput đọc-nhiều/ghi-ít rất cao, gần như không có lock contention giữa SELECT và UPDATE khác dòng. 2PL thuần túy: reader (kể cả `SELECT` giữ shared lock tới hết transaction ở mức nghiêm ngặt) có thể chặn writer và ngược lại → throughput thấp hơn dưới tải đọc cao. Chi phí MVCC: các phiên bản cũ của dòng phải được **dọn dẹp** (`VACUUM` ở PostgreSQL) sau khi không còn transaction nào cần tới — nếu `VACUUM` không chạy đều đặn (hoặc bị transaction dài giữ lại), bảng phình to (bloat), hiệu năng giảm dần.
3. `WHERE department='IT' AND salary>15000000` → tận dụng tối đa (đủ cả 2 cột, đúng thứ tự). `WHERE department='IT'` → tận dụng một phần (chỉ dùng được tiền tố `department`, `salary` không lọc được qua index). `WHERE salary>15000000` (không có `department`) → không tận dụng được (thiếu cột đầu tiên của composite index — vi phạm Leftmost Prefix). Covering index: `EXPLAIN` hiện `Index Only Scan` (mọi cột cần đều lấy được trực tiếp từ index, không đọc bảng chính) thay vì `Index Scan` (phải nhảy về bảng chính lấy thêm cột không có trong index).
4. B-Tree cần **giữ thứ tự** để hỗ trợ tìm kiếm/duyệt hiệu quả. `BIGINT` tự tăng luôn chèn vào **cuối cùng** của cây (giá trị luôn lớn hơn giá trị trước) — trang (page) cuối liên tục được lấp đầy tuần tự, ít phải tách trang (page split), dữ liệu vẫn liền mạch trên đĩa. UUID ngẫu nhiên (v4) rơi vào **vị trí bất kỳ** trong cây với xác suất đều — liên tục gây page split ở giữa cây, phân mảnh (fragmentation) tăng dần, cache hiệu quả kém hơn (working set lớn hơn số trang thực cần). UUIDv7 nhúng **timestamp** vào các bit đầu → giá trị sinh sau luôn lớn hơn giá trị sinh trước (gần giống tính chất tăng dần của `BIGINT`) trong khi vẫn giữ được tính phân tán/không đoán được của UUID ở phần còn lại — khôi phục lại đặc tính "chèn cuối cây" mà vẫn sinh được độc lập giữa nhiều service.
5. Ví dụ: `tickets.stock = 1`. T1: `SELECT stock FROM tickets WHERE id=1;` (đọc snapshot, thấy `1`). T2 (bắt đầu sau, snapshot riêng): cũng `SELECT stock` thấy `1`. T1: tính `stock-1=0`, `UPDATE tickets SET stock=0 WHERE id=1; COMMIT;`. T2 vẫn đang dùng giá trị `1` đã đọc trước đó (snapshot của T2 không tự cập nhật theo T1), tính `stock-1=0`, `UPDATE tickets SET stock=0 WHERE id=1;` — ở `REPEATABLE READ` thuần theo MVCC, `UPDATE` của T2 dù dựa trên dữ liệu cũ vẫn có thể được cho phép (tùy engine: một số sẽ báo "could not serialize" và bắt T2 phải abort, một số cho qua) → nếu cho qua, **kết quả cuối là `stock=0` dù cả hai lần "mua" đều tưởng đã trừ từ `1`** — mất một lần trừ. MVCC chỉ đảm bảo **T2 không đọc dữ liệu bị T1 sửa giữa chừng transaction của nó** (isolation khỏi việc đọc), nhưng **không tự động phối hợp** hai transaction cùng ghi dựa trên cùng giá trị cũ — đó là lý do vẫn cần `FOR UPDATE` (ép T2 chờ T1 xong rồi đọc giá trị mới) hoặc optimistic locking (`version`, phát hiện xung đột tại thời điểm ghi) khi có `read → tính toán → write` trên cùng dữ liệu.
6. Thống kê cũ → optimizer ước tính sai số dòng (`rows=` trong `EXPLAIN` lệch xa thực tế), có thể chọn Nested Loop cho một JOIN thực ra rất lớn (tưởng một bên nhỏ), hoặc chọn Seq Scan/Index Scan sai chỗ vì tưởng bảng nhỏ hơn nhiều so với thực tế sau khi dữ liệu tăng đột biến. Khắc phục: chạy `ANALYZE tablename;` (PostgreSQL) hoặc tương đương để cập nhật thống kê; nhiều DB có auto-analyze theo ngưỡng thay đổi nhưng độ trễ có thể đáng kể sau một đợt insert lớn — nên chủ động chạy lại sau batch import/migration.
7. Xung đột hiếm: optimistic thắng — gần như không overhead (không khóa), chỉ trả giá khi thực sự đụng độ (hiếm). Xung đột thường xuyên: pessimistic thắng — optimistic sẽ retry liên tục, lãng phí công tính toán lặp lại và trải nghiệm người dùng tệ (nhiều lần báo lỗi "vui lòng thử lại"). Trải nghiệm lập trình: pessimistic đơn giản hơn ở tầng gọi (chỉ cần chờ, không cần vòng lặp retry); optimistic cần tầng ứng dụng chủ động bắt lỗi version-conflict và quyết định retry/báo người dùng. Flash-Sale (rất nhiều người tranh mua cùng lúc, xung đột **chắc chắn xảy ra dày đặc**): nghiêng về **pessimistic** (`FOR UPDATE`) hoặc các cơ chế chuyên biệt hơn (hàng đợi/giảm tải trước khi chạm DB, distributed lock) — optimistic dưới tải cực cao sẽ gây "storm" retry làm tình hình tệ hơn.

</details>

---

*File tiếp theo trong lộ trình: **Module 10 (tiếp) — RDBMS phổ biến & NoSQL** (MySQL/PostgreSQL thực hành, MongoDB, Redis — khi nào chọn NoSQL thay vì SQL).*
