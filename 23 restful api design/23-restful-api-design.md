# Module 14 — RESTful API Design

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** API là "giao diện" duy nhất mà Frontend, Mobile App, hoặc service khác nhìn thấy từ Backend của bạn. Thiết kế API sai chuẩn (dùng sai HTTP method, trả sai status code, không version hóa, error response không nhất quán...) sẽ gây khó khăn cho toàn bộ team tích hợp, khó mở rộng về sau, và là điểm bị soi kỹ nhất trong code review lẫn phỏng vấn — vì đây là kỹ năng thể hiện rõ nhất khả năng "thiết kế hệ thống" chứ không chỉ "viết code chạy được".

> **Phạm vi bài này:** Chỉ tập trung vào **thiết kế API** (HTTP semantics, URI, status code, versioning, error format, caching qua HTTP, tài liệu hóa). Các chủ đề liên quan nhưng thuộc phạm vi module khác: Authentication/Authorization chi tiết (JWT, OAuth2 → Module Spring Security), Validation nâng cao/Custom Validator (→ Module Validation), Spring Data JPA Specification/Querydsl chi tiết (→ Module Spring Data nâng cao), Rate Limiting implementation cụ thể (→ Module Resilience/Gateway).

---

## Mục lục

1. [REST là gì? 6 nguyên tắc + Richardson Maturity Model](#1-rest-là-gì)
2. [HTTP Methods đúng chuẩn](#2-http-methods-đúng-chuẩn)
3. [Thiết kế URI (Resource Naming)](#3-thiết-kế-uri)
4. [HTTP Status Code — dùng đúng ngữ cảnh](#4-http-status-code)
5. [Idempotency — tính bất biến khi gọi lại](#5-idempotency)
6. [Pagination, Filtering, Sorting](#6-pagination-filtering-sorting)
7. [API Versioning & Content Negotiation](#7-api-versioning--content-negotiation)
8. [Error Response chuẩn hóa](#8-error-response-chuẩn-hóa)
9. [HATEOAS](#9-hateoas)
10. [Conditional Requests & ETag — caching và concurrency qua HTTP](#10-conditional-requests--etag)
11. [Request/Response Body Design & DTO](#11-requestresponse-body-design)
12. [Bulk Operations & Long-running Operations](#12-bulk-operations--long-running-operations)
13. [Tài liệu hóa API — OpenAPI/Swagger](#13-tài-liệu-hóa-api--openapiswagger)
14. [⚠️ Các bẫy hay gặp](#14-các-bẫy-hay-gặp)
15. [Tổng kết — Bảng ghi nhớ nhanh](#15-tổng-kết--bảng-ghi-nhớ-nhanh)
16. [Bài tập luyện tập](#16-bài-tập-luyện-tập)

---

## 1. REST là gì?

**REST (Representational State Transfer)** là 1 kiến trúc (architectural style) do Roy Fielding đề xuất năm 2000, định nghĩa các ràng buộc (constraints) để xây dựng API dễ mở rộng, nhất quán, dễ hiểu.

### 6 ràng buộc của REST

| Ràng buộc | Ý nghĩa |
|---|---|
| **Client-Server** | Tách biệt hoàn toàn Frontend và Backend, giao tiếp chỉ qua API |
| **Stateless** | Mỗi request phải chứa đủ thông tin để server xử lý — server **không lưu session state** giữa các request (khác với session-based authentication truyền thống) |
| **Cacheable** | Response phải khai báo rõ có thể cache được hay không (`Cache-Control` header) |
| **Uniform Interface** | Giao diện thống nhất — dùng HTTP method chuẩn, URI định danh resource rõ ràng (chi tiết ở mục 2,3) |
| **Layered System** | Client không cần biết đang gọi trực tiếp server hay qua Load Balancer/Proxy/Gateway |
| **Code on Demand** (tùy chọn) | Server có thể trả về code thực thi được (hiếm dùng trong thực tế) |

> **Lưu ý quan trọng:** "REST API" và "RESTful API" trong thực tế công nghiệp thường **không tuân thủ 100%** cả 6 ràng buộc trên (đặc biệt HATEOAS — mục 9 thường bị bỏ qua). Phần lớn API gọi là "REST" chỉ đang áp dụng **Uniform Interface** + **Stateless** — đây gọi là **"RESTful-ish"** hoặc **"Pragmatic REST"**, và điều đó hoàn toàn bình thường trong công nghiệp.

### Richardson Maturity Model — thước đo "REST đến đâu"

Leonard Richardson đề xuất 1 mô hình 4 cấp độ (0-3) để đánh giá 1 API "REST hóa" đến mức nào — rất hữu ích để tự đánh giá API của chính mình, và thường được hỏi trong phỏng vấn thiết kế hệ thống:

| Level | Đặc điểm | Ví dụ |
|---|---|---|
| **Level 0 — The Swamp of POX** | 1 endpoint duy nhất, dùng `POST` cho mọi thao tác, body chứa "action" cần thực hiện | `POST /api` với body `{"action": "getUser", "id": 5}` |
| **Level 1 — Resources** | Có nhiều URI riêng biệt cho từng resource, nhưng vẫn dùng chung 1 HTTP method (thường là `POST`) | `POST /users/5`, `POST /orders/12` |
| **Level 2 — HTTP Verbs** | Dùng đúng HTTP method (`GET`/`POST`/`PUT`/`DELETE`) + đúng Status Code cho từng resource | `GET /users/5` → `200`, `DELETE /users/5` → `204` — **đây là mức mà tuyệt đại đa số API "REST" trong thực tế đạt tới, và cũng là mức được khuyến nghị hướng tới** |
| **Level 3 — HATEOAS** | Response trả kèm hypermedia link hành động khả dụng tiếp theo (mục 9) | `_links: { "cancel": {...} }` — hiếm gặp trong thực tế |

> **Kết luận thực dụng:** Mục tiêu hợp lý cho hầu hết dự án là đạt **Level 2** một cách nhất quán (đúng method, đúng status code, đúng URI) — đây chính là nội dung xuyên suốt các mục 2-8 của bài này. Level 3 (HATEOAS) nên biết nhưng không bắt buộc áp dụng.

### Stateless — hệ quả quan trọng với Authentication

```
❌ Session-based (không Stateless thuần):
   Server lưu session trong bộ nhớ -> Client gửi Session ID (cookie) mỗi request
   -> Nếu có nhiều server (scale ngang), phải đồng bộ session giữa các server (sticky session/Redis)

✅ Token-based (JWT) - phù hợp với Stateless:
   Server KHÔNG lưu gì cả -> Client tự gửi kèm Token (chứa đủ thông tin xác thực) mỗi request
   -> Dễ dàng scale ngang, không cần đồng bộ state giữa các server
```

> Sẽ học chi tiết JWT/Session ở Module Spring Security — ở đây chỉ cần hiểu **vì sao REST ưu tiên Token-based auth hơn Session-based**.

---

## 2. HTTP Methods đúng chuẩn

Đây là phần **cơ bản nhưng bị dùng sai nhiều nhất** trong thực tế — kể cả bởi dev có kinh nghiệm.

| Method | Ý nghĩa | Idempotent? | Có Body? | Ví dụ |
|---|---|---|---|---|
| `GET` | Lấy dữ liệu (read-only, không thay đổi state) | ✅ | Không | `GET /users/5` |
| `POST` | Tạo mới resource (hoặc thao tác không chuẩn CRUD) | ❌ | Có | `POST /users` |
| `PUT` | **Thay thế toàn bộ** resource (full update) | ✅ | Có | `PUT /users/5` |
| `PATCH` | **Cập nhật một phần** resource (partial update) | ❌ (thường không) | Có | `PATCH /users/5` |
| `DELETE` | Xóa resource | ✅ | Thường không | `DELETE /users/5` |

### Sai lầm phổ biến: dùng POST cho MỌI thao tác

```
❌ Thiết kế sai (dùng POST cho tất cả):
POST /getUser?id=5
POST /createUser
POST /updateUser
POST /deleteUser

✅ Thiết kế đúng chuẩn REST (dùng đúng HTTP method):
GET    /users/5
POST   /users
PUT    /users/5
DELETE /users/5
```

### PUT vs PATCH — phân biệt rõ ràng

```java
// PUT - THAY THẾ TOÀN BỘ resource
// Client phải gửi ĐẦY ĐỦ mọi field, kể cả field không đổi
@PutMapping("/users/{id}")
public UserResponse updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
    // request PHẢI có đủ: fullName, email, phone, address...
    // Nếu client chỉ gửi { "fullName": "Pho" } -> các field khác sẽ bị SET VỀ NULL!
}

// PATCH - CẬP NHẬT MỘT PHẦN, chỉ field nào gửi lên mới bị đổi
@PatchMapping("/users/{id}")
public UserResponse patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
    // Client chỉ cần gửi { "fullName": "Pho" } -> CHỈ fullName bị đổi, các field khác giữ nguyên
}
```

⚠️ **Bẫy hay gặp:** Rất nhiều API thực tế "gọi là PUT" nhưng lại code theo logic PATCH (chỉ update field được gửi lên) — điều này **vi phạm ngữ nghĩa chuẩn HTTP**, gây hiểu lầm cho client. Nếu chỉ muốn hỗ trợ update từng phần, hãy dùng đúng `PATCH`.

### JSON Patch (RFC 6902) — chuẩn hóa PATCH thay vì Map tùy ý

Dùng `Map<String, Object>` cho PATCH đơn giản nhưng thiếu chuẩn (không biểu diễn được "xóa 1 field", "chèn vào giữa mảng"...). Với API cần PATCH phức tạp, có chuẩn **JSON Patch**:

```http
PATCH /users/5
Content-Type: application/json-patch+json

[
  { "op": "replace", "path": "/fullName", "value": "Huỳnh Gia Phò" },
  { "op": "remove", "path": "/nickname" },
  { "op": "add", "path": "/tags/-", "value": "vip" }
]
```

> **Thực tế:** JSON Patch ít dùng ở API nội bộ đơn giản (Map tùy ý là đủ), nhưng phổ biến ở API public cần khả năng cập nhật từng phần phức tạp, có thư viện hỗ trợ (`com.github.java-json-tools:json-patch`). Nên biết để chọn đúng công cụ khi cần, không bắt buộc áp dụng mọi nơi.

---

## 3. Thiết kế URI

### Nguyên tắc: URI là DANH TỪ (Resource), không phải ĐỘNG TỪ (Action)

```
❌ Sai — URI chứa động từ (đang mô tả HÀNH ĐỘNG, không phải RESOURCE):
GET  /getAllUsers
POST /createNewOrder
POST /users/5/deleteAccount

✅ Đúng — URI là danh từ số nhiều (Resource), HTTP method mới là hành động:
GET    /users              -> Lấy danh sách user
POST   /orders              -> Tạo order mới
DELETE /users/5             -> Xóa user id=5
```

### Quan hệ phân cấp (Nested Resource)

```
GET  /users/5/orders           -> Lấy tất cả order CỦA user 5
GET  /users/5/orders/12        -> Lấy order 12 (thuộc user 5)
POST /users/5/orders           -> Tạo order mới cho user 5
```

⚠️ **Không nên lồng quá 2-3 cấp** — khó đọc, khó bảo trì:

```
❌ Quá sâu:
GET /users/5/orders/12/items/3/reviews/7

✅ Tốt hơn — dùng resource độc lập kèm query hoặc chỉ giữ 1-2 cấp cần thiết:
GET /reviews/7
GET /orders/12/items
```

### Action không phải CRUD chuẩn — dùng động từ như 1 "sub-resource"

Một số hành động không map thẳng vào CRUD (VD: "duyệt đơn hàng", "reset password") — cách xử lý phổ biến:

```
POST /orders/12/approve       -> Xem "approve" như 1 sub-resource hành động
POST /users/5/reset-password
POST /orders/12/cancel
```

### Quy tắc đặt tên khác

| Quy tắc | Ví dụ |
|---|---|
| Dùng danh từ số nhiều (plural) | `/users` không phải `/user` |
| Dùng chữ thường, nối bằng dấu gạch ngang (`kebab-case`) | `/order-items` không phải `/orderItems` hay `/order_items` |
| Không dùng đuôi file | `/users.json` ❌ — dùng HTTP header `Accept` để xác định format |
| Query string cho filter/sort/pagination, không cho định danh resource | `/users?status=active` ✅, không phải `/users/active` (trừ khi "active" thực sự là 1 resource riêng) |

---

## 4. HTTP Status Code

Trả sai status code là lỗi khiến client (Frontend) khó xử lý logic đúng — đây là phần **bắt buộc phải nắm chắc**.

### Nhóm 2xx — Thành công

| Code | Ý nghĩa | Dùng khi nào |
|---|---|---|
| `200 OK` | Thành công chung | `GET`, `PUT`, `PATCH` thành công |
| `201 Created` | Đã tạo resource mới | `POST` tạo thành công — **nên kèm header `Location`** trỏ tới resource mới |
| `202 Accepted` | Đã nhận yêu cầu, đang xử lý bất đồng bộ | Xử lý nền (async processing), chưa có kết quả ngay (chi tiết ở mục 12) |
| `204 No Content` | Thành công nhưng không có body trả về | `DELETE` thành công, hoặc `PUT`/`PATCH` không cần trả lại resource |

### Nhóm 4xx — Lỗi do Client

| Code | Ý nghĩa | Dùng khi nào |
|---|---|---|
| `400 Bad Request` | Request sai format/thiếu field bắt buộc | Validation lỗi (VD: `email` không đúng định dạng) |
| `401 Unauthorized` | Chưa xác thực (chưa đăng nhập / token không hợp lệ) | Thiếu/sai Token |
| `403 Forbidden` | Đã xác thực nhưng KHÔNG có quyền | User đã login nhưng không đủ role để thực hiện |
| `404 Not Found` | Resource không tồn tại | `GET /users/999` (id không tồn tại) |
| `405 Method Not Allowed` | HTTP method không được hỗ trợ cho URI này | `DELETE /users` (không cho xóa toàn bộ list) |
| `409 Conflict` | Xung đột trạng thái | Tạo user với email đã tồn tại, hoặc optimistic locking conflict |
| `412 Precondition Failed` | Điều kiện HTTP header (`If-Match`...) không thỏa | Cập nhật resource đã bị thay đổi bởi request khác (mục 10) |
| `422 Unprocessable Entity` | Cú pháp đúng nhưng ngữ nghĩa sai (business rule vi phạm) | Ngày kết thúc < ngày bắt đầu |
| `429 Too Many Requests` | Vượt rate limit | Chống spam/DDoS |

### Nhóm 5xx — Lỗi do Server

| Code | Ý nghĩa | Dùng khi nào |
|---|---|---|
| `500 Internal Server Error` | Lỗi không xác định phía server | Exception không được catch, bug code |
| `502 Bad Gateway` | Gateway/Proxy nhận response lỗi từ upstream server | Thường do infrastructure, không phải code của bạn |
| `503 Service Unavailable` | Server tạm thời không sẵn sàng | Đang bảo trì, quá tải |

⚠️ **Phân biệt 401 vs 403 — bẫy hay gặp nhất trong nhóm này:**

```
401 Unauthorized -> "Tôi không biết bạn là ai" (chưa đăng nhập, token sai/hết hạn)
403 Forbidden     -> "Tôi biết bạn là ai, nhưng bạn không được phép làm việc này"
                     (đã đăng nhập, nhưng role không đủ quyền)
```

```java
// Ví dụ: User thường (role USER) cố truy cập API chỉ dành cho ADMIN
@GetMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public List<Report> getReports() { ... }

// Nếu KHÔNG có token / token hết hạn -> 401 Unauthorized
// Nếu CÓ token hợp lệ nhưng role = USER (không phải ADMIN) -> 403 Forbidden
```

### 400 vs 422 — phân biệt tinh tế

```
400 Bad Request        -> Request SAI CÚ PHÁP/FORMAT (thiếu field, sai kiểu dữ liệu JSON)
422 Unprocessable Entity -> Request ĐÚNG cú pháp nhưng VI PHẠM business rule
```

```json
// 400 Bad Request - thiếu field bắt buộc "email"
POST /users
{ "fullName": "Pho" }

// 422 Unprocessable Entity - đủ field, đúng format, nhưng vi phạm nghiệp vụ
POST /orders
{ "startDate": "2026-09-10", "endDate": "2026-09-01" }  // endDate < startDate
```

> **Lưu ý thực tế:** Nhiều team/công ty đơn giản hóa, chỉ dùng `400` cho cả 2 trường hợp trên (không phân biệt 400/422) — đây là lựa chọn thiết kế chấp nhận được miễn là **nhất quán trong toàn bộ API**, và tài liệu hóa rõ ràng.

### Rate Limiting — header đi kèm 429

Khi trả `429 Too Many Requests`, nên kèm các header chuẩn để client biết khi nào được gọi lại — tránh client "đoán mò" retry liên tục càng làm nặng thêm hệ thống:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 30
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1757572800
```

| Header | Ý nghĩa |
|---|---|
| `Retry-After` | Số giây client nên đợi trước khi gọi lại (chuẩn HTTP, được hầu hết HTTP client hiểu) |
| `X-RateLimit-Limit` | Giới hạn tổng số request cho phép trong 1 cửa sổ thời gian |
| `X-RateLimit-Remaining` | Số request còn lại trong cửa sổ hiện tại |
| `X-RateLimit-Reset` | Thời điểm (Unix timestamp) cửa sổ giới hạn được reset |

> Việc **triển khai** rate limiting (thuật toán Token Bucket/Sliding Window, dùng Redis/Bucket4j, hay cấu hình ở API Gateway) thuộc phạm vi module Resilience — ở đây chỉ cần nhớ **hợp đồng response** (status code + header) mà API cần tuân thủ khi bị giới hạn.

---

## 5. Idempotency

**Idempotent** (tính bất biến khi gọi lại): gọi cùng 1 request **nhiều lần liên tiếp** sẽ cho **cùng 1 kết quả** như gọi 1 lần — không gây tác dụng phụ tích lũy.

```
GET     -> Idempotent (đọc dữ liệu không thay đổi gì)
PUT     -> Idempotent (thay thế toàn bộ = N lần giống 1 lần)
DELETE  -> Idempotent (xóa N lần cũng chỉ có tác dụng như xóa 1 lần -
            lần 2 trở đi trả 404 nhưng KHÔNG gây thêm tác dụng phụ)
POST    -> KHÔNG Idempotent (gọi 2 lần POST /orders -> tạo ra 2 order khác nhau!)
PATCH   -> Thường KHÔNG Idempotent (tùy logic, VD: PATCH tăng số lượng +1 mỗi lần gọi)
```

### Vì sao Idempotency quan trọng trong thực tế?

**Tình huống thực tế:** Client gửi request `POST /orders` để đặt hàng, nhưng do mạng chậm, không nhận được response kịp thời → Client (hoặc code retry tự động) **gửi lại y hệt request đó**. Nếu server không có cơ chế bảo vệ → **tạo ra 2 đơn hàng trùng lặp**, khách hàng bị trừ tiền 2 lần!

### Giải pháp — Idempotency Key

```http
POST /orders
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{ "productId": 10, "quantity": 2 }
```

```java
@PostMapping("/orders")
public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody OrderRequest request) {

    // Kiểm tra key đã xử lý trước đó chưa (lưu trong Redis/DB với TTL)
    Optional<Order> existingOrder = idempotencyService.findByKey(idempotencyKey);
    if (existingOrder.isPresent()) {
        // Trả về KẾT QUẢ CŨ, không tạo order mới
        return ResponseEntity.ok(OrderResponse.from(existingOrder.get()));
    }

    Order newOrder = orderService.createOrder(request);
    idempotencyService.save(idempotencyKey, newOrder); // Lưu lại để lần gọi sau (nếu trùng key) trả kết quả cũ
    return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(newOrder));
}
```

> **Liên hệ thực tế:** Đây chính là cơ chế các cổng thanh toán (Stripe, VNPay, Momo...) **bắt buộc** áp dụng — vì thao tác "thanh toán" tuyệt đối không được xử lý trùng lặp do lỗi mạng/retry.

---

## 6. Pagination, Filtering, Sorting

### Pagination — không bao giờ trả về TOÀN BỘ dữ liệu 1 lần

```
❌ Nguy hiểm: GET /users -> trả về 1 triệu record cùng lúc -> crash server/client

✅ Đúng: GET /users?page=0&size=20
```

**2 kiểu Pagination phổ biến:**

**Offset-based (đơn giản, phổ biến nhất — Spring Data JPA hỗ trợ sẵn qua `Pageable`):**

```java
@GetMapping("/users")
public Page<UserResponse> getUsers(Pageable pageable) {
    // Pageable tự động parse từ query param: ?page=0&size=20&sort=fullName,asc
    return userRepository.findAll(pageable).map(UserResponse::from);
}
```

```json
// Response format chuẩn của Spring Data JPA Page<T>
{
  "content": [ /* danh sách user */ ],
  "totalElements": 1000,
  "totalPages": 50,
  "number": 0,       // trang hiện tại
  "size": 20,
  "first": true,
  "last": false
}
```

⚠️ **Nhược điểm Offset-based:** Với bảng dữ liệu lớn, `OFFSET 500000 LIMIT 20` vẫn phải quét qua 500,000 dòng trước khi lấy 20 dòng cần thiết → **chậm dần khi trang càng sâu**.

**Cursor-based (hiệu năng tốt hơn cho dataset lớn — dùng ở các hệ thống lớn như Facebook, Twitter):**

```
GET /users?limit=20&cursor=eyJpZCI6MTIzfQ==
```
```json
{
  "data": [ /* 20 user */ ],
  "nextCursor": "eyJpZCI6MTQzfQ==" // Con trỏ trỏ tới vị trí tiếp theo, thường encode base64 của id cuối cùng
}
```

| | Offset-based | Cursor-based |
|---|---|---|
| Cú pháp | `?page=2&size=20` | `?cursor=xyz&limit=20` |
| Nhảy tới trang bất kỳ | ✅ Dễ dàng | ❌ Chỉ đi tuần tự (next/prev) |
| Hiệu năng với dataset lớn | Giảm dần khi trang sâu | Ổn định (dùng index, không cần OFFSET) |
| Vấn đề khi dữ liệu thay đổi liên tục | Có thể bị trùng/thiếu record khi có insert/delete giữa các lần gọi | Ổn định hơn nhiều |
| Dùng khi nào | Admin dashboard, dataset vừa phải | Feed mạng xã hội, dataset lớn, real-time |

### Filtering

```
GET /orders?status=PENDING&minAmount=100000&createdAfter=2026-01-01
```

```java
@GetMapping("/orders")
public Page<OrderResponse> getOrders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) LocalDate createdAfter,
        Pageable pageable) {
    // Dùng Specification (JPA Criteria API) hoặc Querydsl để build query động
    return orderService.search(status, minAmount, createdAfter, pageable);
}
```

### Sorting

```
GET /users?sort=fullName,asc
GET /users?sort=createdAt,desc&sort=fullName,asc  // Sort nhiều field
```

---

## 7. API Versioning & Content Negotiation

Khi API thay đổi breaking change (đổi cấu trúc response, xóa field...), cần versioning để **không phá vỡ client cũ đang dùng phiên bản trước**.

### 4 cách versioning phổ biến

**1. URI Versioning (⭐ phổ biến nhất, dễ hiểu nhất):**
```
GET /api/v1/users
GET /api/v2/users
```

**2. Query Parameter Versioning:**
```
GET /api/users?version=1
```

**3. Header Versioning (Custom Header):**
```
GET /api/users
X-API-Version: 1
```

**4. Media Type Versioning (Content Negotiation — thuần REST nhất nhưng phức tạp nhất):**
```
GET /api/users
Accept: application/vnd.myapp.v1+json
```

| Cách | Ưu điểm | Nhược điểm |
|---|---|---|
| URI Versioning | Rõ ràng, dễ debug, dễ cache theo URL, dễ route ở Gateway | "Vi phạm" thuần REST (URI đáng lẽ chỉ định danh resource, không nên chứa version) |
| Header Versioning | URI sạch, giữ đúng "tinh thần REST" hơn | Khó test qua trình duyệt trực tiếp, dễ bị quên khi gọi |
| Media Type | Đúng chuẩn REST/HTTP nhất | Phức tạp, ít công cụ hỗ trợ tốt, khó dùng trong thực tế |

> **Thực tế công nghiệp:** **URI Versioning** (`/api/v1/...`) được dùng phổ biến áp đảo vì tính thực dụng, dù về mặt lý thuyết REST "thuần" không khuyến khích. Đây là ví dụ điển hình của **pragmatism thắng thế over purism** trong thiết kế API thực tế.

### Content Negotiation — 1 URI, nhiều định dạng response

Content Negotiation là cơ chế HTTP cho phép **cùng 1 URI trả về nhiều định dạng khác nhau** tùy theo header `Accept` mà client gửi lên — đây chính là nền tảng cho cách versioning #4 ở trên, nhưng cũng áp dụng độc lập cho format dữ liệu (JSON/XML/CSV...):

```http
GET /users/5
Accept: application/json      -> trả JSON

GET /users/5
Accept: application/xml       -> trả XML (nếu server hỗ trợ)
```

```java
@GetMapping(value = "/users/{id}", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
public UserResponse getUser(@PathVariable Long id) {
    // Spring tự chọn Message Converter phù hợp dựa theo header Accept của client
    return userService.findById(id);
}
```

> **Thực tế:** Tuyệt đại đa số API nội bộ/microservice hiện nay chỉ hỗ trợ `application/json` duy nhất — Content Negotiation đa định dạng chủ yếu còn gặp ở API public lâu đời (cần tương thích ngược với client dùng XML) hoặc API cho phép export dữ liệu (CSV/Excel).

---

## 8. Error Response chuẩn hóa

### Vấn đề khi KHÔNG chuẩn hóa

```json
// Endpoint A trả lỗi kiểu này
{ "error": "User not found" }

// Endpoint B trả lỗi kiểu khác
{ "message": "Không tìm thấy user", "code": 404 }

// Endpoint C lại khác nữa
{ "errors": ["Email is required", "Password too short"] }
```

→ Frontend phải viết code xử lý riêng cho từng endpoint — cực kỳ khó bảo trì.

### Cấu trúc Error Response chuẩn hóa (khuyến nghị)

```json
{
  "timestamp": "2026-09-07T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Dữ liệu không hợp lệ",
  "path": "/api/v1/users",
  "errors": [
    { "field": "email", "message": "Email không đúng định dạng" },
    { "field": "password", "message": "Password phải có ít nhất 8 ký tự" }
  ]
}
```

### Implement bằng @RestControllerAdvice — Global Exception Handler

```java
@RestControllerAdvice // Bắt exception cho TẤT CẢ Controller trong ứng dụng - tránh lặp try/catch từng nơi
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) // Lỗi từ @Valid trên @RequestBody
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Dữ liệu không hợp lệ")
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class) // "Lưới an toàn" cuối cùng - bắt mọi exception chưa xử lý
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        // ⚠️ KHÔNG trả ex.getMessage() trực tiếp cho lỗi 500 -
        // có thể lộ thông tin nhạy cảm (stack trace, cấu trúc DB...)
        // Log lỗi đầy đủ ở server, chỉ trả message chung chung cho client
        log.error("Lỗi không xác định", ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Đã có lỗi xảy ra, vui lòng thử lại sau")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.internalServerError().body(error);
    }
}
```

⚠️ **Bẫy bảo mật quan trọng:** Không bao giờ trả **stack trace** hoặc **chi tiết lỗi database** (VD: `SQLException` message chứa tên bảng/cột) trực tiếp cho client ở môi trường production — đây là **thông tin rò rỉ (information leakage)** giúp kẻ tấn công hiểu cấu trúc hệ thống.

### RFC 7807 — Problem Details for HTTP APIs (chuẩn hóa ở tầng HTTP)

Cấu trúc Error Response ở trên là "tự chế" theo quy ước riêng — hoàn toàn hợp lý, nhưng có 1 **chuẩn quốc tế** cho việc này: **RFC 7807 (`application/problem+json`)**, được Spring hỗ trợ sẵn từ Spring Boot 3 qua `ProblemDetail`:

```java
@ExceptionHandler(UserNotFoundException.class)
public ProblemDetail handleNotFound(UserNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("User Not Found");
    problem.setType(URI.create("https://api.example.com/errors/user-not-found"));
    problem.setProperty("userId", ex.getUserId()); // Field mở rộng tùy ý
    return problem;
}
```

```json
{
  "type": "https://api.example.com/errors/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "Không tìm thấy user với id=5",
  "instance": "/api/v1/users/5",
  "userId": 5
}
```

> **Chọn cái nào?** Format `ErrorResponse` tự định nghĩa (như phần trên) vẫn hoàn toàn phổ biến và dễ tùy biến theo nhu cầu team. `ProblemDetail`/RFC 7807 phù hợp hơn khi API là **public API** cần tuân thủ chuẩn quốc tế để tương thích với nhiều loại client/công cụ khác nhau. Quan trọng nhất vẫn là: **chọn 1 format và áp dụng nhất quán toàn bộ API**.

---

## 9. HATEOAS

**HATEOAS (Hypermedia As The Engine Of Application State)** — ràng buộc REST "thuần" nhất, ít được áp dụng thực tế nhất (tương ứng **Level 3** trong Richardson Maturity Model ở mục 1): response API không chỉ trả dữ liệu, mà còn trả kèm **các link hành động khả dụng tiếp theo**.

```json
{
  "id": 12,
  "status": "PENDING",
  "totalAmount": 500000,
  "_links": {
    "self": { "href": "/api/v1/orders/12" },
    "cancel": { "href": "/api/v1/orders/12/cancel" },
    "pay": { "href": "/api/v1/orders/12/pay" }
  }
}
```

**Ý tưởng:** Client không cần biết trước "sau khi tạo order thì có thể làm gì" — server tự trả kèm các action khả dụng dựa theo **state hiện tại** của resource (order đã `PAID` thì sẽ không có link `pay` nữa, chỉ có `refund`).

```java
// Spring HATEOAS
@GetMapping("/orders/{id}")
public EntityModel<OrderResponse> getOrder(@PathVariable Long id) {
    Order order = orderService.findById(id);
    OrderResponse response = OrderResponse.from(order);

    EntityModel<OrderResponse> model = EntityModel.of(response);
    model.add(linkTo(methodOn(OrderController.class).getOrder(id)).withSelfRel());

    if (order.getStatus() == OrderStatus.PENDING) {
        model.add(linkTo(methodOn(OrderController.class).payOrder(id)).withRel("pay"));
        model.add(linkTo(methodOn(OrderController.class).cancelOrder(id)).withRel("cancel"));
    }
    return model;
}
```

> **Thực tế:** HATEOAS **hiếm khi được áp dụng đầy đủ** trong các API thực tế (kể cả tại các công ty lớn) vì độ phức tạp implement cao, trong khi lợi ích chỉ rõ ràng khi client là **generic client tự động điều hướng** (hiếm gặp — hầu hết Frontend/Mobile đều hardcode logic gọi API cụ thể theo tài liệu, không "khám phá" API qua link). Nên biết khái niệm này để hiểu REST đầy đủ và trả lời phỏng vấn, nhưng **không cần lo lắng nếu dự án thực tế không dùng**.

---

## 10. Conditional Requests & ETag

Đây là cơ chế HTTP thuần túy (không phải phát minh riêng của REST) nhưng cực kỳ hữu dụng cho 2 mục đích: **giảm băng thông qua caching** và **tránh mất dữ liệu do ghi đè đồng thời (lost update)** — mà không cần thêm field `version` thủ công vào response như optimistic locking ở tầng ứng dụng (Module JPA).

### ETag là gì?

**ETag (Entity Tag)** là 1 chuỗi định danh (thường là hash) đại diện cho **phiên bản hiện tại** của resource. Server trả kèm ETag trong response header; client lưu lại và gửi kèm ở request sau để "hỏi" server: *"resource này có gì thay đổi so với lần trước tôi lấy không?"*

```http
GET /users/5

Response:
HTTP/1.1 200 OK
ETag: "a1b2c3d4"
{ "id": 5, "fullName": "Pho", "email": "pho@example.com" }
```

### Dùng cho Caching — `If-None-Match` (giảm băng thông)

```http
GET /users/5
If-None-Match: "a1b2c3d4"

// Nếu resource CHƯA đổi -> server trả 304, KHÔNG kèm body -> tiết kiệm băng thông
HTTP/1.1 304 Not Modified

// Nếu resource ĐÃ đổi -> server trả 200 kèm data mới + ETag mới
HTTP/1.1 200 OK
ETag: "e5f6g7h8"
{ "id": 5, "fullName": "Pho Updated", ... }
```

### Dùng cho Concurrency Control — `If-Match` (tránh lost update)

Đây là bản chất **optimistic locking ở tầng HTTP** — tương đương `@Version` trong JPA (Module JPA) nhưng thực hiện qua header chuẩn thay vì field tùy biến:

```http
PUT /users/5
If-Match: "a1b2c3d4"
{ "fullName": "Pho Updated", "email": "pho@example.com" }

// Nếu ETag khớp (chưa ai sửa resource kể từ lần client GET) -> update thành công
HTTP/1.1 200 OK

// Nếu ETag KHÔNG khớp (đã có người khác sửa trước đó) -> từ chối update
HTTP/1.1 412 Precondition Failed
```

```java
@GetMapping("/users/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    String etag = "\"" + user.getVersion() + "\""; // Có thể dùng luôn @Version JPA làm ETag
    return ResponseEntity.ok().eTag(etag).body(UserResponse.from(user));
}

@PutMapping("/users/{id}")
public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @RequestHeader("If-Match") String ifMatch,
        @RequestBody UserUpdateRequest request) {

    User user = userService.findById(id);
    String currentEtag = "\"" + user.getVersion() + "\"";
    if (!currentEtag.equals(ifMatch)) {
        // Có người khác đã sửa resource này kể từ lần client GET gần nhất
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    }
    User updated = userService.update(id, request);
    return ResponseEntity.ok(UserResponse.from(updated));
}
```

> **So sánh với `@Version` (Module JPA):** `@Version` xử lý optimistic locking **ở tầng persistence** (JPA tự ném `OptimisticLockException` khi UPDATE không match version) — phù hợp khi conflict xảy ra **giữa các transaction trong cùng service**. ETag/`If-Match` xử lý concurrency **ở tầng HTTP**, phù hợp khi conflict có thể đến từ **client bên ngoài** (2 tab trình duyệt, 2 request API riêng biệt cách nhau nhiều giây) — 2 cơ chế bổ trợ nhau chứ không thay thế nhau: thực tế, giá trị ETag thường được lấy trực tiếp từ field `@Version` của entity.

⚠️ **Bẫy hay gặp:** Nhầm lẫn `If-None-Match` (dùng cho `GET`, mục đích caching, so khớp thất bại → `304`) với `If-Match` (dùng cho `PUT`/`PATCH`/`DELETE`, mục đích concurrency, so khớp thất bại → `412`) — 2 header có **ngữ nghĩa ngược nhau** dù tên gần giống.

---

## 11. Request/Response Body Design

### Luôn dùng DTO, KHÔNG trả Entity trực tiếp (đã đề cập ở Module 11, nhắc lại vì rất quan trọng)

```java
// ❌ SAI - trả Entity trực tiếp
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
    // Lộ toàn bộ field kể cả password (dù đã hash), thông tin nội bộ không nên public,
    // dễ gặp LazyInitializationException khi serialize quan hệ LAZY
}

// ✅ ĐÚNG - dùng DTO/Response object riêng
public record UserResponse(Long id, String fullName, String email, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
    }
}

@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable Long id) {
    User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    return UserResponse.from(user);
}
```

### Request Validation bằng Bean Validation

```java
public record UserCreateRequest(
    @NotBlank(message = "Họ tên không được để trống")
    String fullName,

    @Email(message = "Email không đúng định dạng")
    @NotBlank
    String email,

    @Size(min = 8, message = "Password phải có ít nhất 8 ký tự")
    String password
) {}

@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
    // @Valid kích hoạt validate -> nếu sai, tự động throw MethodArgumentNotValidException
    // -> GlobalExceptionHandler (mục 8) bắt và trả 400 chuẩn hóa
    User user = userService.create(request);
    URI location = URI.create("/api/v1/users/" + user.getId());
    return ResponseEntity.created(location).body(UserResponse.from(user));
    // 201 Created + header Location trỏ tới resource mới -> đúng chuẩn REST
}
```

### Không bao giờ trả field nhạy cảm

```java
// ❌ Tuyệt đối không bao giờ có field password (kể cả đã hash) trong Response DTO
public record UserResponse(Long id, String email, String passwordHash) {} // SAI!

// ✅ Loại bỏ hoàn toàn khỏi Response
public record UserResponse(Long id, String email, String fullName) {}
```

---

## 12. Bulk Operations & Long-running Operations

### Bulk Operations — thao tác hàng loạt

Khi client cần tạo/xóa/cập nhật **nhiều resource cùng lúc**, gọi N request riêng lẻ (N round-trip) vừa chậm vừa khó đảm bảo tính nhất quán. Cách thiết kế phổ biến — dùng **sub-resource số nhiều** với **response trả trạng thái từng phần tử**:

```http
POST /users/bulk
Content-Type: application/json

[
  { "fullName": "A", "email": "a@example.com" },
  { "fullName": "B", "email": "b@example.com" }
]
```

```json
// 207 Multi-Status - vì có thể MỘT SỐ item thành công, MỘT SỐ thất bại
HTTP/1.1 207 Multi-Status
{
  "results": [
    { "status": 201, "data": { "id": 1, "fullName": "A" } },
    { "status": 409, "error": "Email đã tồn tại: b@example.com" }
  ]
}
```

⚠️ **Bẫy hay gặp:** Dùng `200 OK` chung cho cả response bulk dù bên trong có phần tử thất bại — client dễ hiểu nhầm "toàn bộ đã thành công". Nên dùng `207 Multi-Status` (WebDAV, nhưng được dùng rộng rãi ngoài phạm vi WebDAV cho ý nghĩa này), hoặc tối thiểu phải có field rõ ràng đánh dấu từng item thành công/thất bại như ví dụ trên.

### Long-running Operations — xử lý bất đồng bộ

Một số thao tác (export báo cáo lớn, xử lý video, gọi service ngoài chậm) **không thể trả kết quả ngay** trong 1 request-response chu kỳ ngắn. Pattern chuẩn: trả `202 Accepted` ngay lập tức kèm URL để client **poll trạng thái**:

```http
POST /reports/export
{ "type": "SALES", "month": "2026-09" }

Response:
HTTP/1.1 202 Accepted
Location: /reports/export/jobs/abc-123
{ "jobId": "abc-123", "status": "PENDING" }
```

```http
// Client định kỳ gọi lại để kiểm tra tiến độ
GET /reports/export/jobs/abc-123

HTTP/1.1 200 OK
{ "jobId": "abc-123", "status": "PROCESSING", "progress": 45 }

// ... sau 1 lúc ...
GET /reports/export/jobs/abc-123
HTTP/1.1 200 OK
{ "jobId": "abc-123", "status": "COMPLETED", "resultUrl": "/reports/export/jobs/abc-123/download" }
```

```java
@PostMapping("/reports/export")
public ResponseEntity<JobResponse> requestExport(@RequestBody ExportRequest request) {
    String jobId = exportService.startAsync(request); // Đẩy vào hàng đợi/xử lý nền, KHÔNG block request
    JobResponse job = new JobResponse(jobId, JobStatus.PENDING);
    return ResponseEntity.accepted()
            .location(URI.create("/reports/export/jobs/" + jobId))
            .body(job);
}

@GetMapping("/reports/export/jobs/{jobId}")
public JobResponse getJobStatus(@PathVariable String jobId) {
    return exportService.getStatus(jobId);
}
```

> **Liên hệ:** Đây chính là pattern các API cloud lớn dùng cho thao tác chậm (AWS, Stripe export report...). Việc xử lý nền thực tế thường dùng Message Queue (`@Async`, RabbitMQ/Kafka — thuộc phạm vi module riêng), ở đây chỉ tập trung vào **hợp đồng HTTP** (`202` + `Location` + polling endpoint) mà API cần tuân thủ.

---

## 13. Tài liệu hóa API — OpenAPI/Swagger

Một API thiết kế đẹp nhưng **không có tài liệu** vẫn khó dùng — team Frontend/Mobile/service khác cần biết chính xác URI, method, request/response shape mà không phải đọc code Backend.

**OpenAPI Specification** (tên cũ: Swagger) là chuẩn công nghiệp để mô tả API dưới dạng file YAML/JSON máy đọc được, từ đó sinh ra UI tương tác (Swagger UI) để thử API trực tiếp trên trình duyệt.

```xml
<!-- springdoc-openapi - tự động sinh OpenAPI docs từ Controller/DTO có sẵn -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "Quản lý người dùng")
public class UserController {

    @Operation(summary = "Lấy thông tin user theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy user")
    })
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

Thêm dependency trên là đã có sẵn Swagger UI tại `/swagger-ui.html` — tự động liệt kê toàn bộ endpoint, DTO, và cho phép gọi thử trực tiếp. Các annotation (`@Operation`, `@ApiResponses`, `@Schema`...) chỉ để **làm giàu** tài liệu (mô tả rõ hơn), không bắt buộc — springdoc-openapi tự suy ra phần lớn thông tin từ `@RequestMapping`, DTO record, và Bean Validation annotation sẵn có.

> **Thực tế:** OpenAPI docs không chỉ để đọc — file YAML sinh ra còn dùng để **generate client SDK tự động** (TypeScript/Java client), import vào Postman, hoặc làm input cho API Gateway/contract testing. Đầu tư viết tài liệu tốt ngay từ đầu tiết kiệm rất nhiều thời gian trao đổi qua lại giữa các team.

---

## 14. ⚠️ Các bẫy hay gặp

1. **Dùng `GET` để thực hiện thao tác thay đổi dữ liệu** (VD: `GET /users/5/delete`) — vi phạm nghiêm trọng ngữ nghĩa HTTP, nguy hiểm vì trình duyệt/proxy có thể tự động cache/prefetch GET request → xóa dữ liệu ngoài ý muốn.

2. **URI chứa động từ** (`/getUsers`, `/createOrder`) thay vì danh từ (`/users`, `/orders`).

3. **`PUT` nhưng code logic như `PATCH`** (chỉ update field được gửi) — client không set field nào đó tưởng giữ nguyên, thực ra bị mất dữ liệu.

4. **Không phân biệt `401` và `403`** — trả nhầm khiến Frontend không biết nên redirect tới trang login hay trang "không đủ quyền".

5. **Trả `200 OK` cho MỌI trường hợp kể cả lỗi**, nhồi mã lỗi vào body JSON (`{"success": false, "code": "USER_NOT_FOUND"}`) — vi phạm chuẩn HTTP, khiến các công cụ giám sát/cache/proxy hiểu sai bản chất response.

6. **Không có Pagination** cho endpoint danh sách — trả toàn bộ dữ liệu 1 lần, gây crash khi dataset lớn dần theo thời gian.

7. **Không có Idempotency Key cho API tạo giao dịch tài chính** — nguy cơ tạo giao dịch trùng lặp do retry mạng.

8. **Trả Entity trực tiếp thay vì DTO** — lộ dữ liệu nhạy cảm, gặp `LazyInitializationException`, khó version hóa response sau này.

9. **Trả stack trace/chi tiết lỗi DB ở production** — rò rỉ thông tin hệ thống, tạo lỗ hổng bảo mật.

10. **Breaking change không versioning** — sửa cấu trúc response trực tiếp trên API đang chạy, phá vỡ toàn bộ client cũ đang tích hợp.

11. **Nhầm `If-Match` với `If-None-Match`** — dùng sai header khiến cơ chế concurrency control (mục 10) không hoạt động như mong đợi, hoặc vô tình chặn nhầm request hợp lệ.

12. **Xử lý thao tác nặng/chậm (export báo cáo, gọi service ngoài) đồng bộ trong 1 request** — client bị timeout, connection pool bị chiếm giữ lâu; nên áp dụng pattern `202 Accepted` + polling (mục 12).

---

## 15. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| HTTP Methods | GET đọc, POST tạo, PUT thay thế toàn bộ, PATCH cập nhật 1 phần, DELETE xóa |
| URI | Danh từ số nhiều, không chứa động từ, không lồng quá 2-3 cấp |
| Richardson Maturity Model | Level 2 (đúng verb + status code) là mục tiêu thực dụng cho hầu hết API |
| 401 vs 403 | 401 = chưa biết bạn là ai; 403 = biết rồi nhưng không đủ quyền |
| 400 vs 422 | 400 = sai cú pháp/thiếu field; 422 = đúng cú pháp nhưng vi phạm business rule |
| Idempotent | GET/PUT/DELETE idempotent; POST không — cần Idempotency Key cho giao dịch |
| Pagination | Offset-based dễ dùng; Cursor-based tốt hơn cho dataset lớn |
| Versioning | URI Versioning (`/v1/...`) phổ biến nhất trong thực tế dù không "thuần REST" |
| Error Response | Chuẩn hóa 1 format duy nhất cho toàn bộ API (tự định nghĩa hoặc RFC 7807), dùng `@RestControllerAdvice` |
| HATEOAS | Trả kèm link hành động khả dụng — lý thuyết đẹp, thực tế ít dùng |
| ETag/Conditional Request | `If-None-Match` cho caching (→304), `If-Match` cho concurrency (→412) |
| DTO | Luôn dùng DTO cho Request/Response, không bao giờ trả Entity trực tiếp |
| Bulk/Async | Bulk → `207 Multi-Status`; thao tác chậm → `202 Accepted` + polling |
| Tài liệu hóa | springdoc-openapi sinh Swagger UI tự động từ Controller/DTO có sẵn |

---

## 16. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. `PATCH` luôn là idempotent giống `PUT`.
2. Dùng `GET` để xóa dữ liệu là an toàn nếu chỉ có 1 người dùng hệ thống.
3. `401 Unauthorized` nghĩa là user đã đăng nhập nhưng không đủ quyền truy cập resource.
4. URI Versioning (`/api/v1/...`) là cách versioning được dùng phổ biến nhất trong thực tế công nghiệp, dù không hoàn toàn "thuần REST".
5. `POST` không bao giờ là idempotent trong mọi trường hợp.
6. Trả Entity JPA trực tiếp làm Response API có thể gây ra `LazyInitializationException`.
7. Offset-based pagination luôn có hiệu năng tốt hơn Cursor-based pagination với mọi kích thước dataset.
8. HATEOAS là ràng buộc bắt buộc phải có để 1 API được gọi là "RESTful" trong thực tế công nghiệp hiện nay.
9. `If-None-Match` và `If-Match` có cùng ý nghĩa, chỉ khác cách viết.
10. Theo Richardson Maturity Model, 1 API dùng đúng HTTP method + đúng status code cho từng resource riêng biệt (nhưng không có hypermedia link) đã đạt Level 2.

### Phần B — Bài tập viết code (6 bài)

**Bài 1:** Thiết kế đầy đủ URI + HTTP method cho hệ thống quản lý "Thư viện" (Book, Author, Member, Loan) theo đúng chuẩn REST đã học — bao gồm cả action không-CRUD như "gia hạn mượn sách" (renew loan).

**Bài 2:** Viết 1 `GlobalExceptionHandler` (`@RestControllerAdvice`) xử lý 3 loại exception: `ResourceNotFoundException` (404), `DuplicateResourceException` (409 — VD: email đã tồn tại), và validation lỗi từ `@Valid` (400), theo format Error Response chuẩn đã học ở mục 8.

**Bài 3:** Viết 1 endpoint `GET /products` hỗ trợ đầy đủ Pagination (offset-based dùng `Pageable`), Filtering theo `category` và khoảng giá `minPrice`/`maxPrice`, Sorting theo `price` hoặc `name`.

**Bài 4:** Viết 1 endpoint `POST /payments` có áp dụng Idempotency Key — mô tả rõ luồng xử lý khi client gọi lại với cùng key (dùng Map trong bộ nhớ để minh họa, không cần Redis thật).

**Bài 5:** Cho 1 API thiết kế sai: `POST /api/getUserOrders` (nhận `userId` trong body, trả về response luôn kèm mã lỗi trong body dù status 200). Hãy chỉ ra ít nhất 4 lỗi vi phạm nguyên tắc REST và viết lại thiết kế đúng chuẩn.

**Bài 6:** Viết `GET /articles/{id}` và `PUT /articles/{id}` có áp dụng ETag/Conditional Request: `GET` trả kèm header `ETag`; `PUT` yêu cầu header `If-Match`, trả `412 Precondition Failed` nếu ETag không khớp (mô phỏng bằng field `version` đơn giản trên entity).

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** `PATCH` thường KHÔNG idempotent — tùy thuộc vào logic cụ thể (VD: PATCH tăng dần 1 giá trị mỗi lần gọi sẽ cho kết quả khác nhau).
2. **Sai.** Vi phạm ngữ nghĩa HTTP bất kể số lượng người dùng — trình duyệt, proxy, crawler có thể tự động gửi GET request (prefetch, cache warming) mà không biết nó gây xóa dữ liệu.
3. **Sai.** Đó là định nghĩa của `403 Forbidden`. `401 Unauthorized` nghĩa là chưa xác thực được danh tính (chưa đăng nhập/token sai).
4. **Đúng.** Dù về lý thuyết REST thuần không khuyến khích version trong URI, đây vẫn là cách phổ biến nhất trong thực tế vì tính thực dụng, dễ debug, dễ route.
5. **Sai.** Trong hầu hết trường hợp `POST` không idempotent, nhưng nếu áp dụng cơ chế Idempotency Key đúng cách, kết quả cuối cùng có thể được làm cho "idempotent về mặt hiệu ứng" dù bản chất HTTP method vẫn là POST.
6. **Đúng.** Vì Entity có thể chứa quan hệ LAZY, khi serialize (thường ngoài phạm vi transaction) sẽ gặp lỗi này — đây là lý do chính luôn khuyến nghị dùng DTO.
7. **Sai.** Ngược lại — Offset-based chậm dần khi trang càng sâu (dataset lớn), Cursor-based ổn định hơn nhiều trong trường hợp đó.
8. **Sai.** HATEOAS là 1 trong 6 ràng buộc REST lý thuyết, nhưng thực tế hầu hết API công nghiệp ("RESTful-ish"/"Pragmatic REST") không áp dụng đầy đủ HATEOAS mà vẫn được chấp nhận rộng rãi là "REST API".
9. **Sai.** `If-None-Match` dùng cho caching (thường đi với `GET`, không khớp → server trả data mới, khớp → `304`); `If-Match` dùng cho concurrency control (thường đi với `PUT`/`PATCH`/`DELETE`, không khớp → `412 Precondition Failed`) — ngữ nghĩa ngược nhau.
10. **Đúng.** Đúng HTTP verb + đúng status code cho từng resource riêng biệt chính là Level 2 — mức được khuyến nghị đạt tới cho hầu hết API thực tế. Level 3 mới cần thêm hypermedia link (HATEOAS).

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```
# Book
GET    /api/v1/books                    -> Danh sách sách (có pagination/filter)
GET    /api/v1/books/{id}               -> Chi tiết 1 sách
POST   /api/v1/books                    -> Thêm sách mới
PUT    /api/v1/books/{id}               -> Cập nhật toàn bộ thông tin sách
PATCH  /api/v1/books/{id}               -> Cập nhật 1 phần (VD: chỉ đổi số lượng)
DELETE /api/v1/books/{id}               -> Xóa sách

# Author
GET    /api/v1/authors
GET    /api/v1/authors/{id}
POST   /api/v1/authors
GET    /api/v1/authors/{id}/books       -> Danh sách sách của 1 tác giả (nested resource)

# Member
GET    /api/v1/members
GET    /api/v1/members/{id}
POST   /api/v1/members
PUT    /api/v1/members/{id}

# Loan (mượn sách)
GET    /api/v1/loans                    -> Danh sách lượt mượn (filter theo memberId, status)
GET    /api/v1/loans/{id}
POST   /api/v1/loans                    -> Tạo lượt mượn mới (mượn sách)
POST   /api/v1/loans/{id}/return        -> Action không-CRUD: trả sách
POST   /api/v1/loans/{id}/renew         -> Action không-CRUD: gia hạn mượn sách

# Nested resource (tùy chọn, nếu cần xem theo member)
GET    /api/v1/members/{id}/loans       -> Danh sách lượt mượn của 1 member cụ thể
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldErrorDetail> errors
) {
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }
}

public record FieldErrorDetail(String field, String message) {}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = new ErrorResponse(
                Instant.now(), 400, "Bad Request", "Dữ liệu không hợp lệ", req.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}

// Custom exceptions
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
@GetMapping("/products")
public Page<ProductResponse> getProducts(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @PageableDefault(size = 20, sort = "name") Pageable pageable) {

    Specification<Product> spec = Specification.where(null);

    if (category != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
    }
    if (minPrice != null) {
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
    }
    if (maxPrice != null) {
        spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
    }

    return productRepository.findAll(spec, pageable).map(ProductResponse::from);
}

// Gọi thực tế:
// GET /products?category=electronics&minPrice=100000&maxPrice=5000000&page=0&size=10&sort=price,desc
```

*(Lưu ý: `Specification` yêu cầu `ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>`)*

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    // Minh họa đơn giản bằng ConcurrentHashMap - thực tế nên dùng Redis với TTL
    private final Map<String, PaymentResponse> idempotencyStore = new ConcurrentHashMap<>();
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        // Bước 1: Kiểm tra key đã xử lý trước đó chưa
        PaymentResponse existing = idempotencyStore.get(idempotencyKey);
        if (existing != null) {
            // Trả về KẾT QUẢ CŨ (không tạo giao dịch mới) -> đảm bảo idempotent
            return ResponseEntity.ok(existing);
        }

        // Bước 2: Xử lý giao dịch thật (chỉ chạy 1 lần cho mỗi Idempotency-Key)
        PaymentResponse response = paymentService.processPayment(request);

        // Bước 3: Lưu kết quả gắn với key để lần gọi trùng sau đó dùng lại
        idempotencyStore.put(idempotencyKey, response);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

**Luồng xử lý khi client gọi lại (do timeout/retry):**
1. Lần 1: `Idempotency-Key: abc-123` → chưa có trong store → xử lý thanh toán thật, lưu kết quả vào store, trả `201 Created`.
2. Client không nhận được response (timeout mạng) → tự động retry với **cùng key** `abc-123`.
3. Lần 2: `Idempotency-Key: abc-123` → **đã có** trong store → trả thẳng kết quả cũ (`200 OK`), **không xử lý thanh toán lần 2** → tránh trừ tiền trùng lặp.

*(Lưu ý thực tế: cần thêm TTL cho key trong Redis, VD 24h, để tránh store phình to vô hạn.)*

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

**API sai:** `POST /api/getUserOrders` với `{"userId": 5}` trong body, luôn trả `200 OK` kèm `{"success": false, "errorCode": "USER_NOT_FOUND"}` khi lỗi.

**4 lỗi vi phạm REST:**

1. **Sai HTTP method:** Đây là thao tác **đọc dữ liệu** (lấy danh sách order của user) — phải dùng `GET`, không phải `POST`.

2. **URI chứa động từ + tham số nên nằm trên URI:** `getUserOrders` là động từ; `userId` nên là 1 phần của URI (path parameter) chứ không phải nhét trong body của POST.

3. **Không phân biệt resource cha-con:** Đây rõ ràng là quan hệ "orders của 1 user" — nên thiết kế dạng nested resource.

4. **Trả `200 OK` cho cả trường hợp lỗi:** Vi phạm nghiêm trọng chuẩn HTTP — client/monitoring tool/cache không thể phân biệt được request thành công hay thất bại chỉ dựa vào status code, phải tự parse body để biết — đây là anti-pattern phổ biến gọi là **"200 OK Lie"**.

**Thiết kế lại đúng chuẩn:**

```
GET /api/v1/users/{userId}/orders

Thành công:
200 OK
[ { "id": 1, "totalAmount": 500000, ... }, ... ]

User không tồn tại:
404 Not Found
{
  "timestamp": "2026-09-07T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Không tìm thấy user với id=5",
  "path": "/api/v1/users/5/orders"
}
```

</details>

<details>
<summary><b>Đáp án Bài 6</b></summary>

```java
@GetMapping("/articles/{id}")
public ResponseEntity<ArticleResponse> getArticle(@PathVariable Long id) {
    Article article = articleService.findById(id);
    String etag = "\"" + article.getVersion() + "\"";
    return ResponseEntity.ok()
            .eTag(etag)
            .body(ArticleResponse.from(article));
}

@PutMapping("/articles/{id}")
public ResponseEntity<ArticleResponse> updateArticle(
        @PathVariable Long id,
        @RequestHeader(value = "If-Match", required = false) String ifMatch,
        @Valid @RequestBody ArticleUpdateRequest request) {

    Article article = articleService.findById(id);
    String currentEtag = "\"" + article.getVersion() + "\"";

    if (ifMatch == null) {
        // Không gửi If-Match -> yêu cầu bắt buộc để tránh ghi đè "mù"
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build(); // 428
    }
    if (!currentEtag.equals(ifMatch)) {
        // Có người khác đã sửa bài viết này kể từ lần client GET gần nhất
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build(); // 412
    }

    Article updated = articleService.update(id, request); // version tự tăng (VD: qua @Version JPA)
    String newEtag = "\"" + updated.getVersion() + "\"";
    return ResponseEntity.ok().eTag(newEtag).body(ArticleResponse.from(updated));
}
```

**Luồng minh họa conflict:**
1. Client A `GET /articles/9` → nhận `ETag: "1"`.
2. Client B cũng `GET /articles/9` → nhận `ETag: "1"`, rồi `PUT` thành công trước → server tăng version, `ETag` mới là `"2"`.
3. Client A `PUT /articles/9` với `If-Match: "1"` (giá trị cũ) → server so sánh với version hiện tại là `"2"` → không khớp → trả `412 Precondition Failed`, tránh việc Client A ghi đè mất thay đổi của Client B.

</details>

---

*File tiếp theo trong lộ trình: **Module 15 — Spring Data & Persistence nâng cao** (Spring Data JPA Repository chi tiết, Specification/Querydsl, Auditing, Optimistic vs Pessimistic Locking, Database Migration với Flyway/Liquibase).*
