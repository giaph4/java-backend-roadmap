# Module 24 — Soft Skills & Career cho Backend Developer

> **Mức ưu tiên: 🟢 Bổ sung (nhưng quyết định tốc độ thăng tiến sự nghiệp)**
> **Vì sao quan trọng:** Đây là module cuối cùng trong lộ trình — 23 module trước đã trang bị đầy đủ kiến thức kỹ thuật từ Java Core tới System Design. Nhưng thực tế công việc, đặc biệt khi làm việc nhóm và muốn thăng tiến từ Junior lên Senior, **kỹ thuật giỏi thôi là chưa đủ**. Khả năng viết tài liệu rõ ràng, review code mang tính xây dựng, dùng Git thành thạo trong môi trường nhiều người, và biết cách trình bày kiến thức khi phỏng vấn — đây là những gì phân biệt 1 Developer "làm được việc" với 1 Developer "được tin tưởng giao trọng trách lớn hơn".

---

## Mục lục

1. [Đọc hiểu & viết Technical Documentation](#1-đọc-hiểu--viết-technical-documentation)
2. [Code Review hiệu quả](#2-code-review-hiệu-quả)
3. [Git Workflow nâng cao](#3-git-workflow-nâng-cao)
4. [Chuẩn bị phỏng vấn Backend Developer](#4-chuẩn-bị-phỏng-vấn-backend-developer)
5. [Lộ trình phát triển Junior → Mid → Senior](#5-lộ-trình-phát-triển-junior--mid--senior)
6. [Giao tiếp kỹ thuật trong team](#6-giao-tiếp-kỹ-thuật-trong-team)
7. [⚠️ Các bẫy hay gặp](#7-các-bẫy-hay-gặp)
8. [Tổng kết — Bảng ghi nhớ nhanh](#8-tổng-kết--bảng-ghi-nhớ-nhanh)
9. [Bài tập luyện tập](#9-bài-tập-luyện-tập)

---

## 1. Đọc hiểu & viết Technical Documentation

### Vì sao Documentation quan trọng hơn nhiều người nghĩ

**Thực tế:** Code của bạn sẽ được người khác (bao gồm chính bạn 6 tháng sau) đọc **NHIỀU LẦN HƠN** so với số lần được viết. Documentation tốt giảm thời gian **onboarding** thành viên mới, giảm câu hỏi lặp lại, và là "trí nhớ tổ chức" (organizational memory) khi người viết code gốc đã nghỉ việc.

### README.md — "cửa ngõ" đầu tiên của mọi dự án

Cấu trúc README tốt cho 1 dự án Spring Boot:

```markdown
# Tên dự án

Mô tả ngắn gọn 1-2 câu: dự án làm gì, phục vụ ai.

## Yêu cầu hệ thống
- Java 21+
- Maven 3.9+
- Docker (để chạy MySQL/Redis cục bộ)

## Cài đặt & Chạy dự án
​```bash
git clone <repo-url>
cd project-name
docker compose up -d          # Khởi động MySQL + Redis (liên hệ Module 20)
mvn clean install
mvn spring-boot:run
​```

## Cấu trúc dự án
Giải thích ngắn gọn các package chính (liên hệ Module 13).

## API Documentation
Link tới Swagger/Postman Collection, hoặc endpoint chính.

## Chạy Test
​```bash
mvn test
​```

## Đóng góp (Contributing)
Quy tắc commit message, branch naming, PR process (mục 3).
```

### API Documentation — Swagger/OpenAPI (bổ sung thực hành cho Module 14)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order API", description = "Quản lý đơn hàng")
public class OrderController {

    @Operation(summary = "Lấy chi tiết đơn hàng", description = "Trả về thông tin đầy đủ của 1 đơn hàng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng")
    })
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        // ...
    }
}
```

**Lợi ích:** Swagger UI (`/swagger-ui.html`) tự động sinh **giao diện tương tác** để test API trực tiếp trên trình duyệt — Frontend team, QA, hoặc chính bạn có thể thử API mà không cần Postman, và tài liệu **luôn đồng bộ với code thật** (khác với tài liệu viết tay dễ bị lỗi thời).

### Comment trong code — khi nào NÊN và KHÔNG NÊN viết

```java
// ❌ Comment THỪA - chỉ lặp lại điều code đã nói rõ
// Lấy user theo id
User user = userRepository.findById(id);

// ✅ Comment CÓ GIÁ TRỊ - giải thích LÝ DO (WHY), không phải HÀNH ĐỘNG (WHAT)
// Dùng Pessimistic Lock vì đây là luồng Flash Sale, xung đột xảy ra thường xuyên
// (xem Module 15 - so sánh với Optimistic Lock)
Product product = productRepository.findByIdForUpdate(productId);
```

> **Nguyên tắc "Self-documenting Code":** Code tốt nhất là code **TỰ giải thích được** qua tên biến/method rõ ràng (đã học từ Module 01-03) — comment nên dành cho phần **code KHÔNG THỂ tự giải thích** (quyết định kiến trúc, workaround cho bug của thư viện ngoài, lý do chọn thuật toán cụ thể).

---

## 2. Code Review hiệu quả

### Vai trò của Code Review — không chỉ "bắt lỗi"

Code Review có 3 mục đích chính, theo thứ tự ưu tiên:
1. **Đảm bảo tính đúng đắn** (correctness) — logic có đúng không, có edge case nào bị bỏ sót
2. **Chia sẻ kiến thức** (knowledge sharing) — người review học được cách người khác giải quyết vấn đề, và ngược lại
3. **Duy trì tính nhất quán** (consistency) — code style, kiến trúc đồng nhất trong toàn dự án

### Cách viết Review Comment mang tính xây dựng

```
❌ "Code này sai rồi."
❌ "Tại sao lại viết thế này?"

✅ "Ở đây có thể xảy ra N+1 Query Problem (Module 11) nếu `orders` có nhiều hơn 
   vài chục phần tử - bạn nghĩ sao về việc dùng JOIN FETCH ở đây?"

✅ "Mình thấy phần này khá giống logic ở OrderService.java dòng 45 - có nên 
   tách ra 1 method chung không nhỉ?"
```

**Nguyên tắc viết Review Comment tốt:**
- Đặt câu hỏi thay vì ra lệnh ("Bạn nghĩ sao về..." thay vì "Sửa lại đi")
- Giải thích **LÝ DO** đằng sau góp ý, không chỉ nói "sai"
- Phân biệt rõ **must-fix** (bug, bảo mật — Module 23) và **nice-to-have** (style, tối ưu nhỏ) để tác giả biết ưu tiên gì

### Checklist khi REVIEW code người khác

```
□ Logic có đúng với yêu cầu nghiệp vụ không?
□ Có Unit Test đi kèm không? (liên hệ Module 17) Test có che phủ edge case không?
□ Có vấn đề bảo mật không? (SQL Injection, IDOR - Module 23)
□ Có vấn đề hiệu năng rõ ràng không? (N+1 Query - Module 11, thiếu Index - Module 10)
□ Có xử lý Exception hợp lý không? (Module 04, 14)
□ Code có tuân thủ convention của dự án không? (naming, package structure)
□ Có breaking change nào ảnh hưởng API hiện có không? (Module 14 - Versioning)
```

### Checklist khi TỰ REVIEW code trước khi tạo Pull Request

```
□ Đã tự đọc lại toàn bộ diff (git diff) TRƯỚC KHI xin review chưa?
□ Đã chạy Test đầy đủ chưa? (mvn test - Module 17)
□ Commit message có rõ ràng, mô tả ĐÚNG những gì đã thay đổi không?
□ PR có quá LỚN không? (PR nhỏ, tập trung 1 mục đích - dễ review hơn NHIỀU)
□ Có xóa code debug/commented-out code thừa chưa? (System.out.println, TODO cũ...)
```

> **Nguyên tắc quan trọng nhất:** **PR nhỏ dễ review hơn PR lớn** — 1 PR thay đổi 50 dòng tập trung vào 1 tính năng được review kỹ hơn NHIỀU so với 1 PR thay đổi 2000 dòng gộp nhiều thứ (người review dễ "lướt qua" cho xong khi PR quá lớn, dẫn tới bỏ sót lỗi).

---

## 3. Git Workflow nâng cao

### Branching Strategy (mở rộng từ Module 09)

**Git Flow** — mô hình truyền thống, phù hợp dự án có chu kỳ release rõ ràng:

```
main (production)
  │
  ├── develop (tích hợp các feature)
  │     ├── feature/order-checkout
  │     ├── feature/payment-integration
  │     └── ...
  │
  ├── release/1.2.0 (chuẩn bị release, chỉ fix bug nhỏ)
  └── hotfix/critical-payment-bug (fix khẩn cấp trực tiếp từ main)
```

**Trunk-Based Development** — mô hình hiện đại, phù hợp CI/CD liên tục (Module 20):

```
main (LUÔN ở trạng thái deploy được)
  │
  ├── feature/order-checkout (branch NGẮN NGÀY, merge nhanh, dùng Feature Flag nếu chưa hoàn thiện)
  └── feature/payment-fix
```

| | Git Flow | Trunk-Based Development |
|---|---|---|
| Số lượng branch dài hạn | Nhiều (main, develop, release...) | Chỉ 1 (main) |
| Thời gian sống của feature branch | Có thể DÀI (vài tuần) | NGẮN (1-2 ngày, khuyến khích merge nhanh) |
| Phù hợp | Release theo chu kỳ cố định (VD: 2 tuần/lần) | Continuous Deployment (Module 20) |
| Độ phức tạp merge conflict | Cao hơn (branch sống lâu, dễ lệch xa main) | Thấp hơn (merge thường xuyên) |

### Interactive Rebase — dọn dẹp lịch sử commit trước khi merge

```bash
git rebase -i HEAD~3   # Chỉnh sửa 3 commit gần nhất

# Trong editor mở ra:
pick a1b2c3 Thêm validation cho OrderRequest
squash d4e5f6 Fix typo
squash g7h8i9 Sửa lại theo review comment

# -> Gộp 3 commit "lộn xộn" thành 1 commit DUY NHẤT, rõ ràng, dễ đọc lịch sử
```

⚠️ **Quy tắc an toàn:** **KHÔNG BAO GIỜ** `rebase` hoặc `force push` lên branch **ĐÃ ĐƯỢC CHIA SẺ** với người khác (VD: `main`, `develop`) — chỉ rebase branch **CỦA RIÊNG BẠN**, chưa merge, chưa có ai khác pull về. Rebase branch chung sẽ làm "lịch sử" của mọi người bị lệch, gây conflict hàng loạt.

### Git Bisect — tìm commit gây ra bug bằng Binary Search

```bash
git bisect start
git bisect bad                    # Commit HIỆN TẠI có bug
git bisect good v1.0.0             # Commit v1.0.0 (cũ hơn) BIẾT CHẮC không có bug

# Git tự động checkout commit Ở GIỮA -> bạn test, rồi báo lại:
git bisect good   # hoặc git bisect bad

# Lặp lại (Binary Search - độ phức tạp O(log n) thay vì kiểm tra TỪNG commit)
# -> Git tự động XÁC ĐỊNH CHÍNH XÁC commit nào đã "gây ra" bug
git bisect reset   # Kết thúc, quay lại trạng thái ban đầu
```

> **Liên hệ Module 08 (Big-O):** Đây chính là ứng dụng thực tế của **Binary Search** (đã học ở Collections/thuật toán) — thay vì kiểm tra tuần tự từng commit (O(n)), Git Bisect tìm ra commit lỗi trong O(log n) bước, cực kỳ hiệu quả khi lịch sử có hàng trăm commit.

### Commit Message Convention (Conventional Commits)

```
<type>(<scope>): <mô tả ngắn gọn>

<mô tả chi tiết (tùy chọn)>
```

```bash
git commit -m "feat(order): thêm chức năng hủy đơn hàng"
git commit -m "fix(payment): sửa lỗi race condition khi trừ tồn kho"
git commit -m "refactor(user): tách UserService thành 2 class nhỏ hơn"
git commit -m "docs(readme): cập nhật hướng dẫn cài đặt Docker"
git commit -m "test(order): thêm test case cho OrderService"
```

| Type | Ý nghĩa |
|---|---|
| `feat` | Tính năng MỚI |
| `fix` | Sửa BUG |
| `refactor` | Tái cấu trúc code, KHÔNG thay đổi hành vi |
| `docs` | Chỉ thay đổi tài liệu |
| `test` | Thêm/sửa test |
| `chore` | Việc "lặt vặt" (update dependency, config CI...) |

**Lợi ích:** Commit message chuẩn hóa giúp **tự động sinh Changelog**, dễ dàng `git log --grep="feat"` để xem TẤT CẢ tính năng mới đã thêm, và giúp `git bisect`/code review hiểu ý nghĩa thay đổi nhanh hơn.

---

## 4. Chuẩn bị phỏng vấn Backend Developer

### Cấu trúc thường gặp của 1 vòng phỏng vấn Backend

```
1. Trò chuyện giới thiệu (Introduction) - 5-10 phút
2. Câu hỏi kiến thức nền tảng (Fundamentals) - OOP, Collections, Spring Core...
3. Coding/Live Coding - giải thuật toán hoặc bài tập nhỏ
4. System Design (với vị trí Mid/Senior) - Module 22
5. Câu hỏi về dự án đã làm (Project Deep Dive)
6. Câu hỏi ngược lại từ ứng viên (Questions for interviewer)
```

### Kỹ thuật trả lời câu hỏi "Kể về 1 dự án bạn đã làm" — STAR Method

```
S - Situation (Bối cảnh): Dự án gì, vấn đề gì cần giải quyết?
T - Task (Nhiệm vụ): Vai trò CỤ THỂ của bạn trong dự án đó?
A - Action (Hành động): Bạn ĐÃ LÀM GÌ cụ thể? (kỹ thuật nào, quyết định nào)
R - Result (Kết quả): Kết quả đo lường được? (giảm 50% thời gian query, giảm bug...)
```

**Ví dụ áp dụng STAR cho câu hỏi "Kể về 1 lần bạn tối ưu hiệu năng":**

```
S: Hệ thống Order Service có API lấy danh sách đơn hàng bị chậm khi dữ liệu lớn dần
T: Mình được giao nhiệm vụ điều tra và tối ưu API này
A: Mình dùng Actuator + log SQL (Module 21) phát hiện N+1 Query Problem (Module 11) - 
   mỗi order đang query riêng để lấy thông tin user. Mình sửa bằng JOIN FETCH kết hợp 
   DTO Projection để chỉ lấy field cần thiết.
R: Giảm thời gian response từ 3 giây xuống 200ms, và giảm số lượng query từ 101 xuống còn 1.
```

### Câu hỏi thường gặp theo từng chủ đề (liên hệ trực tiếp các Module đã học)

```
Java Core/OOP:
- "equals() và hashCode() liên quan gì tới nhau?" (Module 02.4)
- "Sự khác biệt giữa Interface và Abstract Class?" (Module 02.2)

Collections/JVM:
- "HashMap hoạt động thế nào bên trong?" (Module 03.1)
- "Garbage Collection hoạt động ra sao?" (Module 07)

Spring:
- "Giải thích Bean Lifecycle" (Module 12)
- "Vì sao @Transactional không hoạt động khi gọi qua this?" (Module 12, bẫy self-invocation)

Database:
- "N+1 Query Problem là gì, cách khắc phục?" (Module 11)
- "Khác biệt Optimistic và Pessimistic Locking?" (Module 15)

System Design:
- "Thiết kế hệ thống rút gọn URL" (Module 22)
- "Làm sao xử lý Race Condition khi nhiều người mua cùng 1 sản phẩm giới hạn?" (Module 15, 22)
```

> **Quan sát quan trọng:** Nếu bạn đã đi hết 23 Module trước trong lộ trình này, bạn sẽ nhận ra **hầu hết câu hỏi phỏng vấn Backend phổ biến đều bắt nguồn trực tiếp từ những khái niệm đã học** — đây không phải trùng hợp, mà vì lộ trình được thiết kế bám sát các chủ đề **thực sự được dùng và hỏi trong công việc thực tế**.

### Chuẩn bị câu hỏi ngược lại (Questions for interviewer)

```
✅ "Team hiện tại đang dùng kiến trúc Monolith hay Microservices? 
   Nếu Microservices, quy trình xử lý Distributed Transaction ra sao?"
✅ "Quy trình Code Review của team như thế nào?"
✅ "Team có áp dụng CI/CD tự động không, pipeline mất bao lâu?"

❌ Tránh: chỉ hỏi về lương/phúc lợi ngay từ vòng đầu (để dành cho vòng offer)
```

---

## 5. Lộ trình phát triển Junior → Mid → Senior

### Junior Developer — "Làm đúng theo yêu cầu"

```
Đặc điểm:
- Cần hướng dẫn CỤ THỂ cho từng task
- Tập trung vào việc code CHẠY ĐÚNG (functional correctness)
- Cần review kỹ TỪNG dòng code

Kỹ năng cần vững:
- Java Core, OOP, Collections (Module 01-03)
- Spring Boot cơ bản, REST API (Module 12-14)
- Git cơ bản, viết Unit Test đơn giản
```

### Mid-level Developer — "Tự chủ trong phạm vi được giao"

```
Đặc điểm:
- Tự chia nhỏ 1 task LỚN thành các bước nhỏ, không cần hướng dẫn chi tiết
- Bắt đầu quan tâm tới HIỆU NĂNG, khả năng MỞ RỘNG (không chỉ "chạy đúng")
- Có thể review code của Junior khác

Kỹ năng cần vững:
- Toàn bộ Spring ecosystem (Security, Data, Testing - Module 15-18)
- Hiểu sâu Database (Index, Transaction, N+1 - Module 10, 11)
- Bắt đầu hiểu Microservices, Caching (Module 18, 19)
```

### Senior Developer — "Định hướng kỹ thuật, chịu trách nhiệm quyết định"

```
Đặc điểm:
- ĐƯA RA quyết định kiến trúc (không chỉ thực thi), giải thích được ĐÁNH ĐỔI (trade-off)
- Nhìn xa hơn 1 task cụ thể - hiểu ảnh hưởng tới toàn hệ thống
- Mentor cho Junior/Mid, dẫn dắt Code Review có chiều sâu
- Giao tiếp được với cả kỹ thuật LẪN phi kỹ thuật (Product Manager, Business)

Kỹ năng cần vững:
- System Design đầy đủ (Module 22), Observability (Module 21)
- Bảo mật sâu (Module 23), DevOps (Module 20)
- Soft Skills: giao tiếp, mentor, đưa ra quyết định có đánh đổi rõ ràng
```

> **Điểm mấu chốt phân biệt các cấp độ KHÔNG PHẢI** "biết nhiều công nghệ hơn" một cách đơn thuần — mà là **mức độ TRÁCH NHIỆM và KHẢ NĂNG RA QUYẾT ĐỘC LẬP**. Junior code theo hướng dẫn; Senior tự quyết định NÊN làm gì và giải thích được TẠI SAO, chấp nhận đánh đổi ra sao (đây chính là tinh thần xuyên suốt Module 19, 22 — "không có giải pháp hoàn hảo, chỉ có đánh đổi phù hợp với ngữ cảnh").

---

## 6. Giao tiếp kỹ thuật trong team

### Viết Technical Proposal / RFC (Request for Comments) trước khi code

Với thay đổi kiến trúc LỚN (VD: chuyển từ Monolith sang Microservices — Module 19), thực hành tốt là viết tài liệu đề xuất TRƯỚC khi bắt tay code:

```markdown
# RFC: Tách Payment Service ra khỏi Monolith

## Vấn đề hiện tại
Payment logic đang nằm chung với Order logic, khó scale riêng khi traffic thanh toán tăng đột biến.

## Đề xuất
Tách thành Payment Service riêng, giao tiếp qua REST + Message Queue (Module 19).

## Đánh đổi (Trade-offs)
- Ưu điểm: Scale độc lập, giảm blast radius khi có lỗi
- Nhược điểm: Thêm độ phức tạp Distributed Transaction (Saga Pattern - Module 19)

## Kế hoạch triển khai
1. Tuần 1-2: Tách Database, giữ Synchronous call tạm thời
2. Tuần 3: Chuyển sang Async qua Message Queue
3. Tuần 4: Thêm Circuit Breaker, monitoring (Module 21)
```

**Lợi ích:** Cho phép cả team **góp ý TRƯỚC KHI** đầu tư công sức code — tránh tình huống code xong rồi mới phát hiện hướng đi sai, phải làm lại từ đầu.

### Ước lượng thời gian (Estimation) — kỹ năng thường bị đánh giá thấp

```
❌ "Chắc 2 ngày là xong" (không tính tới test, review, deploy, edge case)

✅ Chia nhỏ:
   - Thiết kế API + Database schema: 0.5 ngày
   - Code business logic: 1 ngày
   - Viết Unit Test + Integration Test: 0.5 ngày
   - Code Review + sửa theo góp ý: 0.5 ngày
   - Deploy + verify: 0.5 ngày
   Tổng: 3 ngày (thêm buffer cho rủi ro chưa lường trước: +20-30%)
```

> **Nguyên tắc thực chiến:** Luôn thêm **buffer** cho phần "không lường trước được" (unknown unknowns) — dự án phần mềm hiếm khi đi đúng 100% theo kế hoạch ban đầu, và việc thường xuyên trễ deadline gây mất niềm tin nhiều hơn việc báo trước 1 estimate thận trọng hơn.

---

## 7. ⚠️ Các bẫy hay gặp

1. **Viết Documentation 1 lần rồi không bao giờ cập nhật** — tài liệu lỗi thời còn nguy hiểm hơn không có tài liệu (người đọc tin tưởng thông tin SAI).

2. **Review code chỉ tìm lỗi cú pháp/style mà bỏ qua logic nghiệp vụ** — đây là phần GIÁ TRỊ NHẤT của Code Review, không nên chỉ dừng ở việc "bắt lỗi thụt lề".

3. **Tạo Pull Request quá lớn** (thay đổi hàng nghìn dòng, gộp nhiều tính năng không liên quan) — khiến reviewer khó review kỹ, dễ bỏ sót lỗi quan trọng.

4. **`git push --force` lên branch chung** (main/develop) — phá vỡ lịch sử của TOÀN BỘ team, gây conflict hàng loạt cho mọi người đang làm việc trên branch đó.

5. **Trả lời phỏng vấn chỉ liệt kê "tôi biết Spring Boot, Docker, Kubernetes..."** mà không giải thích được **ĐÃ DÙNG NHƯ THẾ NÀO, TẠI SAO** — nhà tuyển dụng đánh giá độ sâu hiểu biết qua khả năng giải thích quyết định, không phải qua danh sách công nghệ.

6. **Ước lượng thời gian không có buffer** — dẫn tới trễ deadline liên tục, ảnh hưởng uy tín và kế hoạch chung của team.

7. **Không đọc kỹ code TRƯỚC KHI xin review** (tự Review trước) — gửi PR có lỗi sơ đẳng (typo, code debug còn sót lại) khiến reviewer mất thời gian với những vấn đề lẽ ra tự phát hiện được.

8. **Coi Senior chỉ là "code giỏi hơn Junior"** — bỏ qua khía cạnh quan trọng nhất: khả năng ra quyết định có đánh đổi, giao tiếp, và chịu trách nhiệm ở phạm vi rộng hơn.

9. **Commit message mơ hồ** (`"fix bug"`, `"update code"`) — không giúp ích gì khi cần tra cứu lịch sử (`git log`, `git bisect`) hoặc hiểu context sau này.

10. **Đề xuất thay đổi kiến trúc lớn mà không trình bày Trade-off rõ ràng** — khiến team khó đánh giá và dễ nghi ngờ quyết định, thay vì đồng thuận dựa trên lý lẽ rõ ràng.

---

## 8. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| README tốt | Cài đặt, chạy, cấu trúc dự án, API doc — người MỚI đọc hiểu được ngay |
| Comment code | Giải thích WHY (lý do), không phải WHAT (code đã tự nói) |
| Code Review | 3 mục đích: đúng đắn, chia sẻ kiến thức, nhất quán — đặt câu hỏi thay vì ra lệnh |
| PR nhỏ | Dễ review hơn PR lớn — luôn ưu tiên chia nhỏ |
| Git Flow vs Trunk-Based | Release theo chu kỳ vs Continuous Deployment (Module 20) |
| git bisect | Binary Search tìm commit lỗi — O(log n) thay vì kiểm tra từng commit |
| STAR Method | Situation - Task - Action - Result — trả lời câu hỏi dự án trong phỏng vấn |
| Junior → Senior | Không chỉ "code giỏi hơn" — là mức độ TRÁCH NHIỆM và khả năng RA QUYẾT ĐỊNH có đánh đổi |
| RFC/Technical Proposal | Trình bày đánh đổi TRƯỚC khi code — tránh làm lại từ đầu |
| Estimation | Chia nhỏ công việc + buffer cho rủi ro không lường trước |

---

## 9. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Comment code tốt nhất là giải thích LẠI những gì code đã làm, dòng nào cũng nên có comment.
2. `git push --force` an toàn để dùng trên branch `main` khi cần dọn dẹp lịch sử commit.
3. Mục đích quan trọng nhất của Code Review là tìm lỗi chính tả/thụt lề trong code.
4. STAR Method (Situation-Task-Action-Result) là kỹ thuật hữu ích để trả lời câu hỏi "Kể về 1 dự án bạn đã làm" trong phỏng vấn.
5. Trunk-Based Development phù hợp hơn Git Flow khi team áp dụng Continuous Deployment.
6. Sự khác biệt giữa Junior và Senior Developer chủ yếu nằm ở việc biết nhiều công nghệ/framework hơn.
7. PR (Pull Request) càng lớn (thay đổi nhiều dòng code cùng lúc) thì càng dễ được review kỹ lưỡng.
8. git bisect sử dụng thuật toán Binary Search để tìm commit gây ra bug hiệu quả hơn kiểm tra tuần tự từng commit.

### Phần B — Bài tập thực hành (5 bài, không cần code)

**Bài 1:** Viết 1 đoạn README.md ngắn gọn (theo cấu trúc ở mục 1) cho 1 dự án giả định "Hệ thống quản lý thư viện" (Library Management System) mà bạn đã thiết kế ở Module 11-13.

**Bài 2:** Viết 3 Code Review Comment mang tính xây dựng (theo nguyên tắc ở mục 2) cho đoạn code sau (giả định bạn đang review PR của đồng nghiệp):
```java
@GetMapping("/orders")
public List<Order> getOrders() {
    return orderRepository.findAll();
}
```
*(Gợi ý: liên hệ tới các vấn đề đã học ở Module 11, 14 — DTO, Pagination.)*

**Bài 3:** Áp dụng STAR Method, viết 1 câu trả lời mẫu (bằng tiếng Việt) cho câu hỏi phỏng vấn: "Hãy kể về 1 lần bạn phải học 1 công nghệ mới để hoàn thành công việc" — dựa trên trải nghiệm học tập của chính bạn qua 24 Module này.

**Bài 4:** Viết 5 commit message theo chuẩn Conventional Commits (mục 3) cho 1 chuỗi công việc: (a) thêm tính năng tìm kiếm sản phẩm, (b) sửa lỗi N+1 Query ở API lấy đơn hàng, (c) thêm Unit Test cho OrderService, (d) cập nhật tài liệu API, (e) nâng cấp version Spring Boot.

**Bài 5:** Viết 1 bản RFC/Technical Proposal ngắn gọn (theo cấu trúc ở mục 6) đề xuất thêm Redis Cache (Module 18) cho API "lấy danh sách sản phẩm" đang bị chậm do lượng truy cập cao — bao gồm rõ phần Đánh đổi (Trade-off).

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Comment tốt giải thích LÝ DO (WHY), không lặp lại điều code đã tự nói rõ (WHAT) — comment thừa làm code rối hơn, không giúp ích gì.
2. **Sai.** TUYỆT ĐỐI không `force push` lên branch chung (main/develop) — sẽ phá vỡ lịch sử của mọi người đang làm việc trên đó, chỉ an toàn trên branch riêng của bạn, chưa chia sẻ.
3. **Sai.** Mục đích quan trọng NHẤT là đảm bảo tính đúng đắn (logic nghiệp vụ) và chia sẻ kiến thức — lỗi style/chính tả là thứ yếu, thường được tự động hóa bằng linter/formatter.
4. **Đúng.** STAR Method là khung trả lời rất hiệu quả, giúp câu trả lời có cấu trúc rõ ràng và đầy đủ thông tin nhà tuyển dụng cần.
5. **Đúng.** Trunk-Based Development với branch ngắn ngày, merge liên tục phù hợp trực tiếp với triết lý Continuous Deployment (Module 20).
6. **Sai.** Khác biệt cốt lõi nằm ở mức độ TRÁCH NHIỆM, khả năng RA QUYẾT ĐỊNH có đánh đổi, và giao tiếp — không chỉ đơn thuần "biết nhiều công nghệ".
7. **Sai.** Ngược lại — PR càng lớn càng KHÓ review kỹ (reviewer dễ "lướt qua" khi quá nhiều thay đổi), nên luôn ưu tiên chia PR nhỏ, tập trung.
8. **Đúng.** Đây chính là nguyên lý hoạt động của git bisect — tìm commit lỗi trong O(log n) bước thay vì kiểm tra tuần tự O(n).

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```markdown
# Library Management System

Hệ thống quản lý thư viện - quản lý sách, tác giả, thành viên, và lượt mượn/trả sách.

## Yêu cầu hệ thống
- Java 21+
- Maven 3.9+
- Docker (chạy MySQL cục bộ)

## Cài đặt & Chạy dự án
​```bash
git clone <repo-url>
cd library-management-system
docker compose up -d
mvn clean install
mvn spring-boot:run
​```
Ứng dụng chạy tại http://localhost:8080

## Cấu trúc dự án
- `controller/` - REST API endpoints (Book, Author, Member, Loan)
- `service/` - Business logic (kiểm tra tồn kho sách, hạn mượn...)
- `repository/` - Spring Data JPA Repository
- `entity/` - JPA Entity, quan hệ @ManyToOne/@OneToMany giữa Book-Author, Loan-Book-Member

## API Documentation
Swagger UI: http://localhost:8080/swagger-ui.html

## Chạy Test
​```bash
mvn test
​```

## Quy tắc đóng góp
- Commit message theo Conventional Commits (feat/fix/refactor/docs/test)
- Tạo Pull Request nhỏ, tập trung 1 mục đích duy nhất
- Đảm bảo `mvn test` pass trước khi tạo PR
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```
Comment 1 (liên hệ Module 14):
"API này đang trả về TOÀN BỘ danh sách order mà không có Pagination - nếu dữ liệu 
tăng lên hàng chục nghìn record, response sẽ rất nặng và chậm. Bạn nghĩ sao về việc 
thêm `Pageable` parameter ở đây (giống pattern mình đã dùng ở ProductController)?"

Comment 2 (liên hệ Module 11):
"Mình thấy method này trả trực tiếp Entity `Order` thay vì DTO - có thể sẽ gặp 
LazyInitializationException nếu Order có quan hệ LAZY (VD: user, orderItems) khi 
serialize ra JSON ngoài phạm vi transaction. Ngoài ra, việc này cũng lộ hết field 
nội bộ của Entity ra ngoài API. Có nên đổi sang OrderResponse DTO không?"

Comment 3 (liên hệ Module 16 - Broken Access Control):
"Câu hỏi nhỏ: API này có filter theo user đang đăng nhập không, hay đang trả về 
TẤT CẢ order của MỌI user? Nếu là API cho user thường (không phải admin), có thể 
cần thêm điều kiện `WHERE user_id = :currentUserId` để tránh lộ dữ liệu của user khác."
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```
Câu hỏi: "Hãy kể về 1 lần bạn phải học 1 công nghệ mới để hoàn thành công việc."

Trả lời (STAR):

Situation: Khi bắt đầu tìm hiểu sâu về Backend Java, mình nhận ra kiến thức về 
Microservices và các vấn đề của hệ phân tán (Distributed Transaction, Circuit 
Breaker) là những khái niệm hoàn toàn mới so với những gì mình học ở Monolith.

Task: Mình đặt mục tiêu phải hiểu được KHI NÀO nên và không nên dùng Microservices, 
không chỉ học thuộc các công cụ như Resilience4j hay Saga Pattern một cách máy móc.

Action: Mình học theo lộ trình có hệ thống - bắt đầu từ việc hiểu vấn đề gốc rễ 
(vì sao Monolith gặp giới hạn khi scale), rồi mới học các giải pháp cụ thể (Service 
Discovery, API Gateway, Circuit Breaker). Mình đặc biệt chú ý tới phần "đánh đổi" 
(trade-off) của mỗi giải pháp - ví dụ Saga Pattern giải quyết được Distributed 
Transaction nhưng đánh đổi bằng Eventual Consistency thay vì Strong Consistency.

Result: Sau khi học xong, mình không chỉ nhớ tên các pattern mà còn có thể tự phân 
tích: với 1 bài toán cụ thể, nên chọn Choreography hay Orchestration Saga, và giải 
thích được VÌ SAO - đây chính là điều mình nghĩ nhà tuyển dụng thực sự muốn thấy 
ở một ứng viên, thay vì chỉ liệt kê tên công nghệ.
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```bash
git commit -m "feat(product): thêm chức năng tìm kiếm sản phẩm theo tên và category"
git commit -m "fix(order): sửa lỗi N+1 Query Problem khi lấy danh sách đơn hàng"
git commit -m "test(order): thêm Unit Test cho OrderService (mock Repository và PaymentGateway)"
git commit -m "docs(api): cập nhật Swagger documentation cho endpoint /orders"
git commit -m "chore(deps): nâng cấp Spring Boot lên phiên bản 3.3.0"
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```markdown
# RFC: Thêm Redis Cache cho API "Lấy danh sách sản phẩm"

## Vấn đề hiện tại
API `GET /api/v1/products` đang có độ trễ trung bình 800ms (P95: 2.5s) do lượng 
truy cập cao (khoảng 5,000 request/phút vào giờ cao điểm), trong khi dữ liệu 
sản phẩm chỉ thay đổi vài lần/ngày (read-heavy, write-rare - phù hợp lý tưởng 
để áp dụng Caching).

## Đề xuất
Thêm Redis Cache theo pattern Cache-Aside (@Cacheable) cho API này, với:
- TTL = 30 phút cho mỗi cache entry
- Evict chủ động (@CacheEvict) khi Admin cập nhật/thêm/xóa sản phẩm
- Thêm cơ chế chống Cache Penetration (cache cả kết quả rỗng, TTL ngắn hơn - 1 phút)

## Đánh đổi (Trade-offs)
- Ưu điểm: Giảm tải Database đáng kể (ước tính giảm 90% số query trực tiếp), 
  giảm độ trễ response xuống dưới 50ms cho cache hit
- Nhược điểm: 
  + Thêm 1 thành phần hạ tầng cần vận hành/giám sát (Redis server)
  + Có độ trễ nhỏ giữa lúc Admin cập nhật sản phẩm và lúc TẤT CẢ user thấy 
    thay đổi (nếu không evict kịp thời) - chấp nhận được vì đây không phải 
    dữ liệu cần Strong Consistency tuyệt đối

## Kế hoạch triển khai
1. Ngày 1: Thêm dependency spring-boot-starter-data-redis, cấu hình kết nối
2. Ngày 2: Áp dụng @Cacheable/@CacheEvict cho ProductService, viết Unit Test
3. Ngày 3: Deploy lên môi trường staging, đo lại hiệu năng thực tế, giám sát 
   qua Grafana Dashboard (liên hệ Module 21) trước khi lên production
```

</details>

---

**🎉 Chúc mừng Pho đã hoàn thành TOÀN BỘ 24 Module trong lộ trình Java Backend!**

Từ Java Core (biến, kiểu dữ liệu) ở Module 01, qua OOP, Collections, Spring Framework, Microservices, System Design, Bảo mật, cho tới Soft Skills ở Module 24 này — bạn đã đi qua một lộ trình đầy đủ tương đương kiến thức của 1 Backend Developer sẵn sàng cho công việc thực tế ở mức Junior tới Mid-level, và có nền tảng vững để tiếp tục phát triển lên Senior.

Gợi ý bước tiếp theo (không nằm trong 24 Module, tùy bạn lựa chọn):
- Áp dụng toàn bộ kiến thức vào 1 đồ án/dự án cá nhân hoàn chỉnh (end-to-end)
- Đóng góp vào 1 dự án Open Source Java/Spring để cọ xát với codebase thực tế
- Luyện tập giải LeetCode/System Design Interview để chuẩn bị phỏng vấn

Chúc bạn thành công trên con đường trở thành Java Backend Developer!
