# Lời giải đầy đủ — Module 09: Build Tools & Quản lý dự án

> Nguồn đề: `17 build tools quan ly du an/17-build-tools-quan-ly-du-an.md` (Phần B — Bài tập thực hành). Chỉ làm Phần B.

---

## Bài 1 — `pom.xml` hoàn chỉnh cho Spring Boot

### Đề
`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, driver PostgreSQL (`runtime`), Lombok (`provided`), `spring-boot-starter-test` (`test`), plugin `spring-boot-maven-plugin`. `<parent>` là `spring-boot-starter-parent`.

### Phân tích

`<parent>spring-boot-starter-parent</parent>` là **BOM (Bill of Materials)** — quản lý **TẬP TRUNG** version của hàng trăm dependency phổ biến trong hệ sinh thái Spring, đảm bảo **tương thích lẫn nhau** (VD: version Spring Data JPA khớp đúng với version Hibernate mà Spring Boot team đã kiểm thử kỹ). Nhờ đó, khai báo dependency **KHÔNG cần ghi `<version>`** — Maven tự lấy đúng version đã được `parent` quản lý.

Mỗi dependency cần đúng `<scope>` theo vai trò: `runtime` (chỉ cần lúc CHẠY, không cần lúc BIÊN DỊCH — vì code không gọi trực tiếp class của driver JDBC, chỉ Spring tự nạp qua JDBC URL); `provided` (cần lúc biên dịch — Lombok sinh code qua annotation processing — nhưng KHÔNG đóng gói vào JAR cuối, vì chỉ là công cụ hỗ trợ biên dịch); `test` (chỉ dùng khi chạy test, không đóng gói vào sản phẩm production).

### Lời giải

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- BOM - quản lý version TẬP TRUNG cho toàn bộ hệ sinh thái Spring Boot -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>demo-app</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Web layer: @RestController, embedded Tomcat... -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA/Hibernate -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Driver PostgreSQL - CHỈ cần lúc RUNTIME, code không import trực tiếp class của nó -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok - CẦN lúc biên dịch (annotation processing) nhưng KHÔNG đóng gói vào JAR cuối -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing: JUnit 5, Mockito, AssertJ... - CHỈ dùng khi chạy test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Đóng gói thành Fat JAR chạy được trực tiếp (java -jar app.jar) -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Giải thích

- **KHÔNG có `<version>` trên bất kỳ dependency Spring nào** — toàn bộ được `spring-boot-starter-parent` (BOM) quản lý sẵn, đảm bảo tương thích lẫn nhau. Chỉ khi cần override version cụ thể (hiếm khi cần) mới ghi `<version>` tường minh, ghi đè lên BOM.
- **`postgresql` scope `runtime`:** code Java **KHÔNG BAO GIỜ** viết `import org.postgresql.Driver` trực tiếp — Spring Boot tự nạp driver phù hợp dựa trên `spring.datasource.url` (dạng `jdbc:postgresql://...`) lúc **CHẠY**, không cần driver có mặt lúc **BIÊN DỊCH**.
- **`lombok` scope `provided`:** annotation processor của Lombok (sinh `getter`/`setter`/`toString`...) chạy **TRONG QUÁ TRÌNH BIÊN DỊCH** — cần có mặt lúc `compile`, nhưng **KHÔNG** cần đóng gói vào JAR cuối cùng (code đã được Lombok "biến đổi" thành bytecode chuẩn ngay lúc compile, JAR chạy production không cần thư viện Lombok nữa).
- Không cần thêm `<version>` cho `spring-boot-maven-plugin` — cũng được quản lý bởi `parent`.

---

## Bài 2 — Quy trình Git Feature Branch hoàn chỉnh

### Đề
(a) tạo `feature/user-registration` từ `develop`; (b) 3 commit riêng biệt (Conventional Commits); (c) `rebase -i` gộp 2 commit "fix typo" vào commit trước; (d) push, mở PR, mô tả PR nên có gì.

### Lời giải

**(a) Tạo branch từ `develop`:**

```bash
git checkout develop
git pull origin develop            # đảm bảo develop LOCAL đang cập nhật mới nhất trước khi tách nhánh
git checkout -b feature/user-registration
```
*Giải thích: luôn `pull` trước khi tách nhánh mới — tránh tình huống branch mới bị "thiếu" các thay đổi gần đây nhất từ `develop`, gây conflict phức tạp hơn khi merge lại sau này.*

**(b) 3 commit riêng biệt theo Conventional Commits:**

```bash
# Commit 1: thêm entity + repository
git add src/main/java/.../User.java src/main/java/.../UserRepository.java
git commit -m "feat(user): thêm User entity và UserRepository"

# Commit 2 (sau khi phát hiện lỗi chính tả trong commit trước, sửa NGAY - sẽ gộp lại ở bước c)
git add src/main/java/.../User.java
git commit -m "fix: sửa lỗi chính tả trong comment User entity"

# Commit 3: thêm service + validate
git add src/main/java/.../UserRegistrationService.java
git commit -m "feat(user): thêm UserRegistrationService với validate email/password"

# Commit 4 (lại phát hiện typo khác)
git add src/main/java/.../UserRegistrationService.java
git commit -m "fix: sửa lỗi chính tả trong UserRegistrationService"

# Commit 5: test
git add src/test/java/.../UserRegistrationServiceTest.java
git commit -m "test(user): thêm Unit Test cho UserRegistrationService"
```

**(c) `git rebase -i` gộp 2 commit "fix typo" vào commit TRƯỚC nó:**

```bash
git rebase -i HEAD~5   # xem lại 5 commit gần nhất
```

Trong editor mở ra:
```
pick a1b2c3 feat(user): thêm User entity và UserRepository
fixup d4e5f6 fix: sửa lỗi chính tả trong comment User entity
pick g7h8i9 feat(user): thêm UserRegistrationService với validate email/password
fixup j1k2l3 fix: sửa lỗi chính tả trong UserRegistrationService
pick m4n5o6 test(user): thêm Unit Test cho UserRegistrationService
```

*Giải thích: dùng `fixup` (không phải `squash`) cho 2 commit "fix typo" — `fixup` gộp thay đổi vào commit NGAY TRƯỚC nó **VÀ BỎ LUÔN message của commit fixup** (giữ nguyên message gốc của commit chính) — phù hợp vì "fix typo" không cần xuất hiện riêng trong lịch sử, nó chỉ là phần bổ sung cho ý đồ của commit trước. Kết quả sau rebase: lịch sử chỉ còn 3 commit sạch (`feat entity+repo`, `feat service`, `test`), không còn dấu vết "fix typo" lặt vặt.*

**(d) Push và mở PR:**

```bash
git push origin feature/user-registration
```

Nếu đã từng push trước khi rebase (lịch sử cũ đã lên remote), cần:
```bash
git push --force-with-lease origin feature/user-registration
# --force-with-lease AN TOÀN HƠN --force thường: từ chối push nếu có ai KHÁC đã push lên nhánh này
# sau lần fetch cuối của bạn - tránh vô tình ghi đè công việc của đồng đội
```

**Mô tả PR nên có:**

```markdown
## Mục đích
Thêm chức năng đăng ký user mới (entity, repository, service validate email/password).

## Thay đổi chính
- `User` entity + `UserRepository` (Spring Data JPA)
- `UserRegistrationService` — validate email format, độ dài password, username trùng
- Unit Test đầy đủ cho các trường hợp hợp lệ/không hợp lệ

## Cách test
1. `mvn test` — chạy Unit Test có sẵn
2. Test thủ công: `POST /api/v1/users/register` với body mẫu (đính kèm ví dụ JSON)

## Ghi chú
- Chưa xử lý gửi email xác nhận (sẽ làm ở PR riêng)
- Cần review kỹ phần validate password (Bài liên quan: security)
```

---

## Bài 3 — Giải quyết Merge Conflict hợp lý về nghiệp vụ

### Đề
Conflict giữa `calculateDiscount` bản đơn giản (`* 0.9`) và bản có `tiered discount` (theo ngưỡng giá). Viết bản đã giải quyết, giải thích lý do chọn.

### Phân tích

Đây là conflict **VỀ MẶT NGHIỆP VỤ**, không phải conflict kỹ thuật thuần túy (khác dấu ngoặc/format) — Git **KHÔNG THỂ tự động giải quyết** vì nó không hiểu ý đồ nghiệp vụ, chỉ có **người viết code mới quyết định được** logic nào là đúng/mới hơn.

### Lời giải — bản đã giải quyết

```java
// Giữ lại bản "tiered" (feature/tiered-discount) vì đây RÕ RÀNG là bản MỞ RỘNG,
// bản HEAD (giảm cố định 10%) là logic CŨ đã lỗi thời so với yêu cầu nghiệp vụ mới
public double calculateDiscount(double price) {
    if (price > 1000) return price * 0.8;  // giảm 20% cho đơn hàng LỚN
    return price * 0.95;                    // giảm 5% cho đơn hàng thường
}
```

### Giải thích lý do chọn

- **Bản `feature/tiered-discount` là bản THAY THẾ có chủ đích, không phải trùng lặp ngẫu nhiên:** tên nhánh `feature/tiered-discount` cho thấy rõ đây là 1 tính năng **được phát triển CÓ CHỦ ĐÍCH** để nâng cấp logic giảm giá từ "1 mức cố định" thành "nhiều bậc theo giá trị đơn hàng" — đây là **cải tiến nghiệp vụ**, không phải lỗi cần revert.
- **Không nên "gộp cả 2"** (VD: cộng thêm điều kiện của cả 2 bản) — vì bản HEAD (`* 0.9` cho MỌI đơn hàng) và bản tiered là **2 CÁCH TIẾP CẬN LOẠI TRỪ NHAU** về cùng 1 nghiệp vụ (tính giảm giá) — không có ý nghĩa "vừa giảm cố định 10%, vừa giảm theo bậc" cùng lúc.
- **Nguyên tắc chung khi giải quyết conflict nghiệp vụ:** LUÔN xác nhận với người viết ra nhánh đối diện (hoặc đọc kỹ commit message/PR description liên quan) trước khi tự ý chọn 1 bên — trong tình huống thực tế, nên **trao đổi với đồng đội** thay vì tự đoán, vì đôi khi cả 2 thay đổi đều cần **GIỮ LẠI Ý ĐỊNH RIÊNG**, chỉ là cách viết code khác nhau, không loại trừ nhau như ví dụ này.

---

## Bài 4 — `.gitignore` cho Spring Boot + IntelliJ + `.env`

### Đề
`.gitignore` đầy đủ, giải thích từng nhóm. `.env.example` cơ chế đồng đội biết cần khai báo biến gì.

### Lời giải

```gitignore
### ===== Maven build output ===== ###
target/
*.jar
*.war
!.mvn/wrapper/maven-wrapper.jar
# -> Sản phẩm BUILD RA từ source code, không phải source - build lại được bất cứ lúc nào,
#    commit vào Git chỉ làm phình repo và gây conflict vô nghĩa (file binary không merge được)

### ===== IntelliJ IDEA ===== ###
.idea/
*.iml
*.ipr
*.iws
out/
# -> Cấu hình RIÊNG của từng máy/từng người (đường dẫn SDK, plugin cá nhân...) -
#    mỗi thành viên team có 1 bộ cấu hình IDE khác nhau, không nên áp đặt chung

### ===== VS Code (nếu team có người dùng) ===== ###
.vscode/
!.vscode/extensions.json
# -> Giữ lại "extensions.json" (gợi ý extension NÊN cài cho dự án) vì đây là thông tin CHUNG hữu ích,
#    còn lại (settings cá nhân) thì loại trừ

### ===== OS-specific ===== ###
.DS_Store
Thumbs.db
# -> File rác tự động sinh bởi macOS/Windows Explorer, không liên quan gì tới dự án

### ===== Biến môi trường / Secret ===== ###
.env
.env.local
application-local.yml
*.pem
*.key
# -> TUYỆT ĐỐI không commit secret thật (password DB, API key...) - liên hệ Module 13/23 bảo mật.
#    File .env chứa GIÁ TRỊ THẬT của từng máy/môi trường, KHÔNG BAO GIỜ nên public lên Git

### ===== Log files ===== ###
*.log
logs/

### ===== Test/Coverage report ===== ###
/coverage/
*.exec
```

### Cơ chế `.env.example` — cho đồng đội biết cần khai báo biến gì

```bash
# .env.example  <- FILE NÀY ĐƯỢC COMMIT (không nằm trong .gitignore)
# Copy file này thành .env rồi điền giá trị THẬT trước khi chạy dự án cục bộ

DB_URL=jdbc:postgresql://localhost:5432/mydb
DB_USERNAME=postgres
DB_PASSWORD=                    # <- để trống, mỗi người tự điền password THẬT của máy mình
JWT_SECRET_KEY=                 # <- để trống, tự sinh secret riêng, KHÔNG dùng chung giữa các môi trường
MAIL_SMTP_HOST=smtp.gmail.com
MAIL_SMTP_PORT=587
```

```bash
# Quy trình dùng thực tế
cp .env.example .env
# Sau đó mở .env, điền giá trị THẬT - file .env đã nằm trong .gitignore, KHÔNG BAO GIỜ bị commit nhầm
```

### Giải thích

- **`.env.example`** đóng vai trò **"tài liệu sống"** — liệt kê ĐÚNG những biến môi trường ứng dụng cần, nhưng **không chứa giá trị nhạy cảm thật** — member mới join dự án chỉ cần `cp .env.example .env` rồi điền giá trị của riêng mình, không cần hỏi han "cần khai báo những biến gì" qua kênh chat/tài liệu rời rạc dễ lỗi thời.
- Đây chính là thực hành chuẩn kết hợp trực tiếp với nguyên tắc **"KHÔNG hardcode secret, đọc qua biến môi trường"** đã học ở Module 13 (Spring Boot `${DB_PASSWORD}`) và nhắc lại nhiều lần ở Module 23 (Bảo mật OWASP).

---

## Bài 5 — Cứu một `reset --hard` lỡ tay

### Đề
Đã `commit` xong 2 tiếng làm việc, lỡ `git reset --hard HEAD~1` xóa mất. Viết lệnh (kể cả `git reflog`) để tìm lại và khôi phục về branch mới.

### Phân tích

**Điểm mấu chốt cần hiểu:** `git reset --hard` **KHÔNG THỰC SỰ XÓA** commit ngay lập tức — nó chỉ di chuyển con trỏ `HEAD`/branch hiện tại **ra khỏi** commit đó. Commit **VẪN CÒN NGUYÊN** trong Git object database, chỉ là **không còn branch nào trỏ tới nó** (trở thành "dangling commit") — và **`git reflog`** ghi lại **LỊCH SỬ MỌI THAY ĐỔI của `HEAD`**, kể cả sau khi `reset`, cho phép tìm lại được.

### Lời giải — quy trình khôi phục

```bash
# BƯỚC 1: Xem reflog - lịch sử MỌI di chuyển của HEAD (kể cả sau reset)
git reflog

# Kết quả tiêu biểu:
# a1b2c3d HEAD@{0}: reset: moving to HEAD~1        <- đây là lệnh reset --hard vừa gây ra sự cố
# d4e5f6g HEAD@{1}: commit: feat(order): thêm chức năng hủy đơn hàng   <- ĐÂY! commit đã "mất"
# h7i8j9k HEAD@{2}: commit: feat(order): thêm OrderService

# BƯỚC 2: Xác nhận đúng commit cần khôi phục (xem lại nội dung)
git show d4e5f6g

# BƯỚC 3: Tạo BRANCH MỚI trỏ thẳng tới commit đó - AN TOÀN, không đụng gì tới branch hiện tại
git branch recovery-branch d4e5f6g

# BƯỚC 4: Chuyển sang branch mới để tiếp tục làm việc / kiểm tra lại toàn bộ thay đổi
git checkout recovery-branch

# BƯỚC 5 (tùy chọn): Nếu muốn "trả lại" đúng branch gốc về trạng thái trước khi lỡ reset
git checkout main          # quay lại branch gốc đã bị reset nhầm
git reset --hard d4e5f6g   # đưa branch gốc VỀ ĐÚNG commit đã tìm lại được
```

### Giải thích

- **`git reflog` chỉ lưu trên MÁY LOCAL, có thời hạn** (mặc định Git tự dọn dẹp các "dangling commit" không còn được trỏ tới sau **~30-90 ngày**, qua cơ chế garbage collection nội bộ `git gc`) — càng phát hiện sớm, khả năng khôi phục càng chắc chắn.
- **Không nên bỏ qua `reflog` chỉ vì "tưởng đã mất vĩnh viễn"** — đây là 1 trong những công cụ "cứu nguy" quan trọng nhất của Git mà nhiều người mới học không biết tới, khiến họ hoảng loạn không cần thiết khi `reset --hard` nhầm.
- **Tạo branch MỚI (thay vì reset lại NGAY branch cũ)** là bước an toàn — cho phép **kiểm tra kỹ nội dung** đã khôi phục trước khi quyết định có "trả lại" branch gốc hay không, tránh gây thêm sai sót chồng lên sai sót ban đầu.
- **Bài học phòng ngừa:** trước khi chạy bất kỳ lệnh Git nào có chữ `--hard`/`--force`, luôn dừng lại suy nghĩ kỹ — đây là các lệnh **CÓ THỂ GÂY MẤT DỮ LIỆU khó phục hồi ngay lập tức nếu không biết `reflog`** — với người mới, nên ưu tiên `git reset --soft` (giữ lại thay đổi ở staging area) thay vì `--hard` khi không chắc chắn.

---

## Bài 6 — Bài toán tổng hợp: Branching Strategy cho capstone 4 người, 8 tuần

### Đề
Đề xuất chiến lược nhánh phù hợp cho team 4 người, dự án 8 tuần — so sánh với Gitflow đầy đủ.

### Phân tích

Gitflow (đầy đủ với `main`/`develop`/`release/*`/`hotfix/*`) được thiết kế cho **sản phẩm có chu kỳ RELEASE CHÍNH THỨC LẶP LẠI** (VD: phần mềm đóng gói, release theo quý) — với **team nhỏ (4 người), thời gian ngắn (8 tuần), không có khái niệm "release production" lặp lại nhiều lần**, Gitflow đầy đủ là **THỪA THÃI VÀ PHỨC TẠP KHÔNG CẦN THIẾT**.

### Lời giải — Đề xuất: GitHub Flow đơn giản hóa (biến thể Trunk-Based)

```
Sơ đồ nhánh đề xuất:

main (LUÔN ở trạng thái chạy được - demo được bất cứ lúc nào cho giảng viên/người hướng dẫn)
  │
  ├── feature/user-auth         (thành viên A, 2-3 ngày, PR review rồi merge)
  ├── feature/order-management  (thành viên B, 2-3 ngày)
  ├── feature/payment-integration (thành viên C, 2-3 ngày)
  ├── feature/admin-dashboard   (thành viên D, 2-3 ngày)
  └── fix/order-status-bug      (bất kỳ ai, sửa nhanh, merge ngay khi xong)

  - KHÔNG có branch "develop" riêng - feature branch merge THẲNG vào main sau khi review
  - KHÔNG có branch "release/*" - vì không có khái niệm release đóng gói theo chu kỳ
  - Mỗi feature branch SỐNG NGẮN (2-3 ngày, tối đa 1 tuần) - merge sớm, tránh conflict lớn
```

**Quy trình làm việc:**
1. Mỗi thành viên tạo `feature/<tên-việc>` từ `main` mới nhất.
2. Làm việc, commit thường xuyên (Conventional Commits).
3. Tạo Pull Request khi xong (hoặc gần xong, xin review sớm) — **ít nhất 1 thành viên khác review** trước khi merge.
4. Merge vào `main` (dùng **Squash and Merge** trên GitHub — gộp toàn bộ commit của PR thành 1 commit gọn trên `main`, giữ lịch sử `main` sạch sẽ).
5. Xóa feature branch ngay sau khi merge.

### So sánh với Gitflow đầy đủ

| | Gitflow đầy đủ | GitHub Flow đơn giản (đề xuất) |
|---|---|---|
| Số loại branch dài hạn | 2 (`main` + `develop`) + branch tạm (`release/*`, `hotfix/*`) | Chỉ 1 (`main`) |
| Độ phức tạp học/áp dụng | Cao — 4 người mới học Git dễ nhầm lẫn quy tắc | Thấp — dễ nhớ, dễ áp dụng ngay |
| Phù hợp chu kỳ release | Có chu kỳ release CHÍNH THỨC lặp lại (VD: 2 tuần/lần) | KHÔNG cần khái niệm "release" tách biệt — code trên `main` LUÔN sẵn sàng demo |
| Overhead quản lý | Cao (nhiều bước merge qua lại giữa develop/release/main) | Thấp (1 bước merge PR → main) |
| Rủi ro conflict | Feature branch có thể sống lâu (theo release cycle), dễ lệch xa `develop` | Feature branch sống NGẮN (2-3 ngày) → ít conflict |

### Giải thích vì sao chọn

- **Team 4 người, 8 tuần KHÔNG có nhu cầu "release song song nhiều phiên bản"** (đặc trưng Gitflow được thiết kế để giải quyết — VD: vừa fix hotfix cho bản đang chạy production, vừa phát triển tính năng mới cho bản tiếp theo) — dự án capstone chỉ có **1 dòng phát triển duy nhất**, tiến thẳng tới demo cuối kỳ.
- **`main` luôn ở trạng thái chạy được** là tiêu chí quan trọng nhất cho dự án ngắn hạn — bất cứ lúc nào giảng viên/người hướng dẫn muốn xem demo, `main` đã sẵn sàng, không cần "release" riêng qua nhiều bước.
- **Giảm overhead học tập:** với 4 người có thể chưa thành thạo Git, quy trình càng đơn giản càng giảm rủi ro thao tác sai (nhầm branch, quên merge `develop` vào `release`...) — phù hợp ưu tiên **dành thời gian cho code thay vì quản lý quy trình Git phức tạp**.
- Đây cũng chính xác là mô hình được khuyến nghị ở phần lý thuyết Module 24 (Soft Skills) — **Trunk-Based Development** phù hợp với nhịp độ phát triển nhanh, liên tục — đúng đặc điểm của 1 dự án capstone 8 tuần.

---

*Đây là lời giải cho toàn bộ Phần B của Module 17. Tiếp theo: Module 18 — Database & SQL.*
