# Module 09 — Build Tools & Quản lý dự án

> **Mức độ ưu tiên: Cao (Maven/Git) → Trung bình (Gradle)** — Không dự án backend thực tế nào build tay bằng `javac` từng file. Công cụ build và Git là điều **bắt buộc phải thành thạo ngay ngày đầu đi thực tập/đi làm** — nhà tuyển dụng mặc định ứng viên đã biết, nên thường **không được dạy kỹ ở trường** nhưng lại gây bối rối nhất cho sinh viên mới ra trường.

> **Phạm vi bài này:** cấu trúc & vòng đời Maven, so sánh Gradle, và Git ở mức làm việc thực tế trong team (branching, rebase/merge, PR, conflict, convention). **Chỉ nhắc tên, không đi sâu:** CI/CD pipeline (Module 20), Unit Test/Mockito (Module 17-kế), Spring Boot Starter/Profile thực tế (Module 13). Các chỗ chạm chủ đề khác chỉ nêu đủ để bài trọn vẹn.

---

## Mục lục

1. [Vì sao cần Build Tool](#1-vì-sao-cần-build-tool)
2. [Maven — cấu trúc dự án & `pom.xml`](#2-maven--cấu-trúc-dự-án--pomxml)
3. [Maven — Dependency Management](#3-maven--dependency-management)
4. [Maven — Vòng đời Build (Build Lifecycle)](#4-maven--vòng-đời-build-build-lifecycle)
5. [Maven — Plugin, Profile & Multi-module](#5-maven--plugin-profile--multi-module)
6. [Gradle — tổng quan & so sánh với Maven](#6-gradle--tổng-quan--so-sánh-với-maven)
7. [Git — kiến thức nền tảng & mô hình object](#7-git--kiến-thức-nền-tảng--mô-hình-object)
8. [Git — Undo & xem lại lịch sử](#8-git--undo--xem-lại-lịch-sử)
9. [Git — Branching Model](#9-git--branching-model)
10. [Git — Rebase vs Merge](#10-git--rebase-vs-merge)
11. [Git — Pull Request & Code Review](#11-git--pull-request--code-review)
12. [Git — xử lý Merge Conflict](#12-git--xử-lý-merge-conflict)
13. [`.gitignore` & Commit Convention](#13-gitignore--commit-convention)
14. [Tổng kết — Bảng ghi nhớ nhanh](#14-tổng-kết--bảng-ghi-nhớ-nhanh)
15. [Bài tập luyện tập](#15-bài-tập-luyện-tập)

---

## 1. Vì sao cần Build Tool

Dự án backend thực tế có hàng trăm file `.java`, phụ thuộc hàng chục thư viện ngoài. Build Tool giải quyết:

1. **Quản lý dependency** — tự tải đúng phiên bản thư viện cần dùng, cùng **transitive dependency** (thư viện mà thư viện đó cần), thay vì tự tay tải `.jar` và ghép classpath.
2. **Chuẩn hóa quy trình build** — compile/test/package theo một quy trình, chạy **giống nhau** trên mọi máy trong team và trên CI server (Module 20).
3. **Chuẩn hóa cấu trúc dự án** — ai mở dự án Maven/Gradle mới cũng biết ngay code nằm ở đâu.

---

## 2. Maven — cấu trúc dự án & `pom.xml`

### Cấu trúc thư mục chuẩn (Standard Directory Layout)

```
my-project/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/            ← Maven Wrapper (mục 4) — nên commit cùng dự án
├── src/
│   ├── main/
│   │   ├── java/com/example/app/Application.java
│   │   └── resources/               ← .properties, .yml — KHÔNG phải code
│   └── test/
│       ├── java/
│       └── resources/
└── target/                          ← TỰ SINH, không commit lên Git
```

> Maven mặc định tìm code ở đúng các vị trí này — **"Convention over Configuration"**, nguyên lý cốt lõi sẽ gặp lại xuyên suốt Spring Boot (Module 13).

### Bộ khung `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>        <!-- GAV coordinate — xem dưới -->
    <artifactId>my-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>            <!-- jar | war | pom (mục 5) -->

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>...</dependencies>       <!-- mục 3 -->
    <build><plugins>...</plugins></build>  <!-- mục 5 -->
</project>
```

### GAV Coordinate & Semantic Versioning

`groupId:artifactId:version` (**GAV**) định danh **duy nhất** một thư viện trong Maven Central (hoặc repository nội bộ) — như tọa độ GPS.

`version` nên theo **Semantic Versioning (SemVer)**: `MAJOR.MINOR.PATCH`.

| Thành phần | Tăng khi |
|---|---|
| `MAJOR` | Thay đổi **phá vỡ tương thích** (breaking change) |
| `MINOR` | Thêm tính năng, **vẫn tương thích ngược** |
| `PATCH` | Sửa lỗi, **vẫn tương thích ngược** |

`-SNAPSHOT` (ví dụ `1.0.0-SNAPSHOT`) = phiên bản **đang phát triển** — Maven **luôn kiểm tra lại** bản mới nhất mỗi lần build (không cache cố định như bản release), khác hẳn phiên bản đã release (`1.0.0`) — **bất biến**, một khi đã publish thì nội dung không đổi nữa.

---

## 3. Maven — Dependency Management

### Khai báo & scope

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.2.0</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Maven tự tải `.jar` từ **Maven Central**, cache tại `~/.m2/repository/` — lần sau không tải lại.

| Scope | Ý nghĩa |
|---|---|
| `compile` (mặc định) | Cần mọi giai đoạn, **đóng gói kèm** vào `.jar`/`.war` |
| `test` | Chỉ compile/chạy test — không đóng gói (JUnit, Mockito) |
| `provided` | Cần lúc compile, **không** đóng gói — môi trường triển khai đã có sẵn (Servlet API), hoặc dùng lúc compile để **sinh code** rồi thôi (Lombok) |
| `runtime` | Không cần lúc compile, cần lúc **chạy** (driver JDBC cụ thể — code chỉ gọi qua interface chuẩn) |

### Transitive dependency & xung đột phiên bản

```
Dự án của bạn
    └─► spring-boot-starter-web (bạn khai báo)
            ├─► spring-web, spring-webmvc, tomcat-embed-core, jackson-databind (TỰ ĐỘNG kéo theo)
```

Đây là ý nghĩa của **Spring Boot Starter**: khai báo một dòng, kéo theo cả cây (Module 13).

Khi 2 thư viện cùng phụ thuộc một thư viện thứ ba ở **hai phiên bản khác nhau**, Maven chọn theo **"Nearest Wins"** — khai báo **gần** dự án gốc nhất trong cây thắng; hòa khoảng cách thì **khai báo trước trong `<dependencies>`** thắng.

```bash
mvn dependency:tree              # xem TOÀN BỘ cây — công cụ chẩn đoán ClassNotFoundException/NoSuchMethodError khó hiểu
mvn dependency:analyze           # liệt kê dependency KHAI BÁO nhưng KHÔNG DÙNG, và dùng nhưng CHƯA khai báo trực tiếp
```

### `<exclusions>` — loại một nhánh transitive cụ thể

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>                                        <!-- ví dụ: muốn thay Tomcat bằng Undertow -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### `<dependencyManagement>` / BOM — khai báo version một chỗ, dùng khắp dự án

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>   <!-- BOM: Bill of Materials -->
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>   <!-- KHÔNG cần <version> — lấy từ BOM -->
    </dependency>
</dependencies>
```

> `<dependencyManagement>` chỉ **khai báo sẵn** version/scope — **không** tự thêm dependency vào classpath (khác `<dependencies>`, luôn thêm thật). Đây chính là cơ chế `spring-boot-starter-parent`/BOM giúp mọi `spring-boot-starter-*` trong dự án luôn **cùng một bộ version tương thích**, không cần tự tay ghi version từng cái.

---

## 4. Maven — Vòng đời Build (Build Lifecycle)

Chuỗi **phase** cố định, chạy **tuần tự** — gọi 1 phase tự chạy hết mọi phase trước nó.

```
validate → compile → test → package → verify → install → deploy
```

| Phase | Ý nghĩa |
|---|---|
| `validate` | Kiểm tra `pom.xml` hợp lệ |
| `compile` | Biên dịch `src/main/java` |
| `test` | Chạy Unit Test (`src/test/java`) |
| `package` | Đóng gói `.jar`/`.war` vào `target/` |
| `verify` | Kiểm tra bổ sung (Integration Test, chất lượng code) |
| `install` | Copy vào kho **cục bộ** (`~/.m2/`) — dự án khác trên **cùng máy** dùng được |
| `deploy` | Đẩy lên kho **dùng chung** của team |

```bash
mvn compile
mvn test           # tự chạy compile trước
mvn package         # tự chạy compile + test trước
mvn clean            # XÓA target/
mvn clean install    # rất hay dùng — build sạch, cài vào kho local
```

> `mvn package` tự chạy `test` trước — test **thất bại** thì `package` **dừng lại**, không tạo `.jar`. Đây là cơ chế bảo vệ chất lượng mặc định.

### Phase (lifecycle) vs Goal (plugin) — hai khái niệm dễ nhầm

Một **phase** (`compile`, `test`...) không tự nó làm gì — nó **được gắn** với một hoặc nhiều **goal** của plugin (`compiler:compile`, `surefire:test`...). Gọi trực tiếp một goal (không qua phase) cũng được: `mvn spring-boot:run` chạy đúng **một** goal, không kéo theo cả lifecycle.

### Maven Wrapper — build tái lập trên mọi máy

```bash
./mvnw clean package      # Linux/Mac
mvnw.cmd clean package    # Windows
```

`mvnw`/`.mvn/` **nên commit** cùng dự án — mọi máy (kể cả CI server) dùng **đúng phiên bản Maven** đã khai báo, tránh "trên máy tôi chạy được".

### Cờ hữu ích

```bash
mvn -X package     # log debug chi tiết — chẩn đoán lỗi build khó hiểu
mvn -o package      # offline — chỉ dùng cache ~/.m2, không gọi mạng
mvn -U package       # ép resolve lại bản SNAPSHOT mới nhất (bỏ qua cache cũ)
mvn -P prod package  # kích hoạt profile (mục 5)
```

---

## 5. Maven — Plugin, Profile & Multi-module

### Plugin

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>   <!-- "mvn spring-boot:run" -->
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration><source>17</source><target>17</target></configuration>
        </plugin>
    </plugins>
</build>
```

### Profile — cấu hình theo môi trường

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties><db.url>jdbc:h2:mem:devdb</db.url></properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties><db.url>jdbc:postgresql://prod-server:5432/maindb</db.url></properties>
    </profile>
</profiles>
```

```bash
mvn package -P prod
```

> Nền tảng để hiểu Spring Profile (`application-dev.yml`/`application-prod.yml` — Module 13) — cơ chế khác, ý tưởng "tách cấu hình theo môi trường" giống nhau.

### Multi-module project — nhiều `pom.xml` con dưới một `pom.xml` cha

```xml
<!-- pom.xml gốc -->
<packaging>pom</packaging>          <!-- KHÔNG phải jar/war — POM cha chỉ để điều phối -->
<modules>
    <module>common</module>
    <module>core</module>
    <module>api</module>
</modules>
```

```
project-root/
├── pom.xml               (packaging = pom)
├── common/pom.xml         (parent trỏ về root)
├── core/pom.xml           (phụ thuộc common)
└── api/pom.xml            (phụ thuộc core)
```

`mvn install` ở thư mục gốc build **đúng thứ tự** dựa trên phụ thuộc giữa các module (`common` trước, `api` sau). Dùng khi một hệ thống tách nhiều thư viện/service dùng chung nhưng vẫn build/version cùng nhau.

---

## 6. Gradle — tổng quan & so sánh với Maven

### `build.gradle` (Groovy) / `build.gradle.kts` (Kotlin, có type-safety)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
}
group = 'com.example'
version = '1.0.0-SNAPSHOT'
repositories { mavenCentral() }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}
```

### `implementation` vs `api` — khác biệt lớn nhất so với Maven

```groovy
dependencies {
    api          'com.fasterxml.jackson.core:jackson-databind:2.16.0'  // LỘ RA transitively cho module khác dùng module này
    implementation 'org.apache.commons:commons-lang3:3.14.0'            // CHỈ dùng nội bộ, không lộ ra ngoài
}
```

Maven: mọi dependency `compile` đều lộ ra transitively cho ai dùng module đó. Gradle: `implementation` **đóng gói tốt hơn** — module khác không thấy, không phải recompile khi bạn đổi một `implementation` dependency bên trong; chỉ đổi `api` mới buộc phía dùng phải build lại.

### Vì sao Gradle build nhanh hơn — incremental build & build cache

Mỗi **task** khai báo rõ input/output; Gradle hash input để quyết định **UP-TO-DATE** (bỏ qua, không chạy lại) hay phải chạy. **Build cache** (local/remote) lưu output theo hash input — tái dùng được **giữa các máy/CI**, kể cả build sạch từ đầu.

Build script Gradle có hai giai đoạn: **configuration** (chạy script, dựng đồ thị task) rồi **execution** (chạy đúng task cần theo đồ thị) — lý do Gradle "là code thật" (Groovy/Kotlin) còn Maven chỉ "đọc khai báo" XML.

### `gradlew` — Gradle Wrapper (tương đương `mvnw`)

```bash
./gradlew build
```

### Bảng so sánh

| Tiêu chí | Maven | Gradle |
|---|---|---|
| Cấu hình | XML, khai báo, dài dòng | Groovy/Kotlin DSL, ngắn hơn, lập trình được |
| Tốc độ | Chậm hơn (build lại toàn bộ) | Nhanh hơn — incremental + build cache |
| Đóng gói phụ thuộc | Mọi `compile` scope đều lộ transitively | `implementation` vs `api` — kiểm soát rõ hơn |
| Linh hoạt | Cứng theo lifecycle | Viết task tùy chỉnh dễ |
| Phổ biến | Rất phổ biến, đặc biệt doanh nghiệp lâu năm | Ngày càng phổ biến dự án mới; bắt buộc ở Android |
| Học ban đầu | Dễ đọc hơn dù dài | Cần biết thêm Groovy/Kotlin |

> **Khuyến nghị:** thành thạo Maven trước (phổ biến hơn ở job posting fresher VN, dễ đọc), rồi học Gradle khi đã quen build tool nói chung — hai công cụ chia sẻ cùng tư duy cốt lõi (dependency management, build lifecycle/task graph).

---

## 7. Git — kiến thức nền tảng & mô hình object

**Git** = hệ quản lý phiên bản **phân tán**, chạy cục bộ. **GitHub/GitLab/Bitbucket** = dịch vụ lưu repository từ xa + tính năng cộng tác (PR, Issue, CI/CD) **xây trên nền** Git.

### Bốn vùng & lệnh cơ bản

```
Working Directory  →  Staging Area  →  Local Repository  →  Remote Repository
   (git add)            (git commit)         (git push)
```

```bash
git status
git add file.java   /  git add .
git commit -m "feat(auth): thêm đăng nhập"
git push origin main
git pull origin main    # fetch + merge (mục 10)
git log --oneline --graph
```

### 3 trạng thái file

```
Untracked → Modified → Staged → Committed
```

### Mô hình object bên dưới — vì sao Git nhanh và đáng tin

Mỗi **commit** không phải là "diff" — nó là một **con trỏ tới snapshot toàn bộ thư mục (tree)** tại thời điểm đó, cộng con trỏ tới commit cha, cộng metadata (tác giả, thời gian, message). Nội dung file lưu dưới dạng **blob**, định danh bằng **hash** (SHA-1) của chính nội dung — hai file giống hệt nhau ở hai commit khác nhau **chia sẻ cùng một blob** (dedupe tự nhiên, không lưu trùng).

```
commit ──► tree ──► blob (file A)
  │          └────► blob (file B)
  └──► commit cha ──► tree ──► ...
```

Toàn bộ lịch sử là một **DAG** (đồ thị có hướng không chu trình) các commit. `HEAD` là con trỏ trỏ tới **branch hiện tại** — nhánh chỉ là một con trỏ tới một commit, di chuyển tới khi có commit mới. Checkout thẳng vào một commit (không qua branch) gọi là **detached HEAD**: commit tạo ra ở trạng thái này **không thuộc branch nào**, dễ "mất" nếu không tạo branch trước khi rời đi (cứu bằng `reflog` — mục 8).

---

## 8. Git — Undo & xem lại lịch sử

```bash
git restore file.java                 # bỏ thay đổi CHƯA staged (khôi phục từ commit gần nhất)
git restore --staged file.java        # bỏ khỏi staging, GIỮ thay đổi trong working dir

git reset --soft  HEAD~1              # lùi 1 commit, GIỮ thay đổi ở staging
git reset --mixed HEAD~1              # (mặc định) lùi 1 commit, giữ ở working dir, bỏ staging
git reset --hard  HEAD~1              # ⚠️ lùi 1 commit, XÓA HẲN thay đổi

git revert <hash>                     # tạo commit MỚI "đảo ngược" — AN TOÀN cho lịch sử đã chia sẻ

git stash                             # cất tạm thay đổi chưa commit, working dir sạch
git stash pop                         # lấy lại (và xóa khỏi stash)
git stash list                        # xem các lần đã stash

git cherry-pick <hash>                # áp MỘT commit cụ thể từ nhánh khác vào nhánh hiện tại

git reflog                            # "lưới cứu sinh" — mọi nơi HEAD từng trỏ tới, kể cả sau reset --hard
git tag v1.2.0                        # lightweight tag đánh dấu 1 release
git tag -a v1.2.0 -m "Release 1.2.0"  # annotated tag — có message, tác giả, thời gian (nên dùng cho release)

git blame file.java                   # dòng nào, commit nào, ai sửa cuối
git bisect start                      # tìm kiếm nhị phân commit gây ra bug qua nhiều commit
```

### `reset` vs `revert` — quy tắc vàng

| | `git reset` | `git revert` |
|---|---|---|
| Cách hoạt động | **Di chuyển** con trỏ branch, có thể xóa commit khỏi lịch sử | **Thêm commit mới** đảo ngược thay đổi, giữ nguyên lịch sử cũ |
| An toàn với lịch sử đã push/chia sẻ | ⚠️ Không — đồng nghiệp đã pull sẽ lệch lịch sử | ✅ Có — ai cũng thấy đúng một chuỗi sự kiện |
| Khi dùng | Commit **chưa push**, hoặc nhánh **của riêng bạn** | Commit **đã push**, nhánh **dùng chung** |

> `git reflog` là lưới cứu sinh cuối cùng — kể cả sau `reset --hard`, commit vẫn còn trong Git một thời gian (chưa bị garbage-collect) và tìm lại được qua `reflog`.

---

## 9. Git — Branching Model

```bash
git branch feature/login
git checkout feature/login
git checkout -b feature/login      # tạo + chuyển, 1 lệnh — hay dùng nhất
git switch feature/login            # cú pháp hiện đại hơn (Git 2.23+)
git branch -a                       # liệt kê tất cả (local + remote)
git branch -d feature/login          # xóa (sau khi đã merge)
```

### Gitflow — có cấu trúc, phù hợp release theo chu kỳ

```
main (production, luôn ổn định)
  └── develop (tích hợp)
        ├── feature/login
        ├── feature/payment
        └── release/1.2.0 (chuẩn bị phát hành — chỉ fix nhỏ)
              └── hotfix/critical-bug (fix khẩn trên production)
```

| Nhánh | Mục đích |
|---|---|
| `main` | Production, luôn sẵn sàng deploy |
| `develop` | Tích hợp các feature trước release |
| `feature/*` | 1 tính năng, tách từ `develop`, merge ngược lại |
| `release/*` | Chuẩn bị phát hành — chỉ fix bug nhỏ |
| `hotfix/*` | Fix khẩn trên `main`, merge vào **cả** `main` và `develop` |

### Trunk-Based Development — đơn giản, phù hợp CI/CD liên tục

```
main (nhánh duy nhất, luôn deploy được)
  ├── feature/login (branch NGẮN NGÀY)
  └── feature/payment
```

> Gitflow hợp chu kỳ phát hành **chậm, cố định**. Trunk-Based hợp team **deploy liên tục** (Module 20), ưu tiên merge nhanh, tránh feature branch "sống" lâu (dễ conflict lớn). Xu hướng hiện đại nghiêng về Trunk-Based hoặc Gitflow rút gọn.

---

## 10. Git — Rebase vs Merge

```bash
git checkout feature/login
git rebase main             # "chép lại" từng commit của feature/login lên ĐẦU main mới nhất
git rebase -i HEAD~3         # interactive — squash/reword/drop 3 commit gần nhất TRƯỚC khi merge/PR
```

| | `git merge` | `git rebase` |
|---|---|---|
| Lịch sử | Giữ nguyên, thêm **merge commit** (nhánh rẽ, thấy rõ điểm hợp) | **Viết lại** — commit mới với hash mới, lịch sử **tuyến tính** |
| Nhìn bằng `--graph` | Thấy rõ 2 nhánh chạy song song | Thẳng, sạch, nhưng mất dấu "thực ra 2 việc chạy song song" |
| An toàn | Luôn an toàn | ⚠️ Đổi hash mọi commit bị rebase |

> **Quy tắc vàng của rebase:** **không bao giờ** rebase commit **đã push lên nhánh dùng chung** mà người khác đang dựa vào — hash đổi khiến lịch sử của họ "phân nhánh" khỏi của bạn một cách khó hiểu. Rebase an toàn cho: nhánh feature **của riêng bạn**, **chưa** có ai khác pull. Nếu bắt buộc phải force-push sau rebase, dùng `git push --force-with-lease` (chỉ ghi đè nếu remote **chưa đổi** từ lần bạn fetch gần nhất — an toàn hơn `--force` thô).

### Chiến lược merge PR trên GitHub (liên hệ mục 11)

| Chiến lược | Kết quả trên nhánh đích |
|---|---|
| **Merge commit** | Giữ nguyên toàn bộ commit của PR + 1 merge commit |
| **Squash and merge** | Gộp cả PR thành **1** commit — lịch sử `main` sạch, "1 PR = 1 dòng log" |
| **Rebase and merge** | Chép từng commit của PR lên đầu nhánh đích, không tạo merge commit |

> Khuyến nghị phổ biến: `main` dùng **squash-merge** để lịch sử sạch; trong lúc làm PR thì commit tự do (kể cả "wip", "fix typo") mà không sợ làm bẩn lịch sử chính.

---

## 11. Git — Pull Request & Code Review

Quy trình chuẩn:

```
1. git checkout -b feature/login         (từ develop/main)
2. Code, commit nhiều lần theo tiến độ
3. git push origin feature/login
4. Mở PR: feature/login → develop
5. Đồng nghiệp review — comment, yêu cầu sửa
6. Sửa, commit, push thêm — PR tự cập nhật
7. Được approve → merge (squash/merge/rebase — mục 10)
8. Xóa branch feature/login
```

### Lợi ích Code Review

1. Bắt lỗi **sớm**, rẻ hơn nhiều so với fix trên production.
2. **Chia sẻ kiến thức** — giảm "Bus Factor" (rủi ro chỉ 1 người hiểu 1 đoạn code).
3. Nhất quán coding convention/kiến trúc.
4. CI (Module 20) chạy test/kiểm tra chất lượng **ngay trên PR** trước khi cho merge.

### Kỷ luật PR & etiquette review

- **PR nhỏ, tập trung một việc** — dễ review, dễ revert nếu sai, review nhanh hơn nhiều so với PR 2000 dòng.
- **Draft PR** — mở sớm để trao đổi hướng đi trước khi hoàn thiện, đánh dấu rõ "chưa sẵn sàng merge".
- Phân biệt comment **blocking** ("phải sửa trước khi merge") với **nitpick**/"suy nghĩ thêm" (không chặn merge) — tránh review kéo dài vô ích vì tiểu tiết.
- **Branch protection** (cấu hình trên GitHub/GitLab): bắt buộc ≥1 approval, bắt buộc CI pass, chặn force-push thẳng vào `main` — hàng rào kỹ thuật cho quy trình trên, không phải chỉ dựa vào "mọi người tự giác".

---

## 12. Git — xử lý Merge Conflict

Xảy ra khi Git **không tự động** kết hợp được thay đổi từ 2 nguồn — thường 2 người sửa **cùng dòng**.

```java
<<<<<<< HEAD
    private String status = "PENDING";     // code CỦA BẠN
=======
    private String status = "NEW";         // code từ NHÁNH ĐANG MERGE VÀO
>>>>>>> feature/order-status
```

```bash
git merge feature/order-status   # Git báo conflict, chỉ đúng file/dòng
# 1. Mở file, đọc CẢ HAI phiên bản
# 2. Quyết định giữ bên nào / kết hợp cả hai / viết lại hoàn toàn
# 3. Xóa <<<<<<<, =======, >>>>>>>
git add file.java                # đánh dấu ĐÃ giải quyết
git commit                        # hoàn tất merge
```

> **Nguyên tắc:** không chọn 1 bên rồi xóa bên kia một cách máy móc — đọc hiểu **ý nghĩa nghiệp vụ** của cả hai thay đổi, trao đổi với người viết đoạn kia nếu cần, tránh vô tình xóa mất một fix bug quan trọng.

### Giảm conflict "giả" bằng `.gitattributes`

```gitattributes
* text=auto                    # chuẩn hóa line ending (LF/CRLF) — tránh conflict do khác OS
*.jar binary
CHANGELOG.md merge=union        # gộp cả 2 bên thay vì hỏi xung đột — hợp file chỉ APPEND
```

### `git rerere` — Git tự nhớ cách bạn đã giải quyết

```bash
git config --global rerere.enabled true
```

("**re**use **re**corded **re**solution") — hữu ích khi rebase một nhánh dài, cùng một conflict lặp lại ở nhiều bước rebase; Git tự áp lại cách giải quyết đã ghi nhớ từ lần trước.

---

## 13. `.gitignore` & Commit Convention

### `.gitignore`

```gitignore
target/
build/

.idea/
*.iml
.vscode/

application-local.properties
.env

.DS_Store
Thumbs.db
```

```bash
git config --global core.excludesfile ~/.gitignore_global   # quy tắc CÁ NHÂN (IDE, OS) — không lặp ở từng repo
git rm --cached secrets.properties                            # lỡ track 1 file đáng ra .gitignore — bỏ khỏi Git, GIỮ trên đĩa
```

> ⚠️ **Bảo mật:** lỡ commit mật khẩu/API key — **xóa ở commit sau KHÔNG đủ**, thông tin vẫn còn trong lịch sử cũ, ai cũng `git log`/`git show` xem lại được. Cần công cụ chuyên biệt (`git filter-repo`, BFG Repo-Cleaner) xóa **hoàn toàn khỏi lịch sử**, và **luôn đổi (rotate) key/mật khẩu ngay** — coi như đã lộ.

### Conventional Commits

```
<type>(<phạm vi tùy chọn>): <mô tả ngắn>

[phần thân chi tiết, tùy chọn]
```

| Type | Ý nghĩa |
|---|---|
| `feat` | Tính năng mới |
| `fix` | Sửa lỗi |
| `refactor` | Tái cấu trúc, không đổi hành vi |
| `docs` | Chỉ đổi tài liệu |
| `test` | Thêm/sửa test |
| `chore` | Việc lặt vặt (cập nhật dependency, cấu hình) |

```bash
git commit -m "feat(auth): thêm đăng nhập Google OAuth2"
git commit -m "fix(payment): sửa lỗi tính sai phí ship khi giỏ hàng rỗng"
```

Đánh dấu **breaking change** để công cụ (semantic-release) tự tăng `MAJOR`:

```
feat!: đổi định dạng response API sang JSON:API

BREAKING CHANGE: field `data` giờ là object thay vì array
```

> commit có cấu trúc → tự sinh Changelog, tìm nhanh (`git log --grep="payment"`), cả team hiểu lịch sử mà không cần đọc từng diff.

---

## 14. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Standard Directory Layout | `src/main/java`, `src/main/resources`, `src/test/java` — quy ước Maven |
| GAV / SemVer | `groupId:artifactId:version`; `MAJOR.MINOR.PATCH`; `-SNAPSHOT` luôn resolve lại |
| Dependency Scope | `compile`/`test`/`provided`/`runtime` |
| Transitive & xung đột | "Nearest Wins"; `dependency:tree` chẩn đoán; `<exclusions>` loại nhánh cụ thể |
| BOM / `dependencyManagement` | Khai báo version một chỗ, không tự thêm dependency |
| Build Lifecycle | `validate→compile→test→package→verify→install→deploy`, tuần tự. Phase ≠ goal |
| `mvnw` | Maven Wrapper — build tái lập, không cần cài Maven |
| Multi-module | `packaging=pom` + `<modules>` — build đúng thứ tự phụ thuộc |
| Gradle | Nhanh hơn (incremental + cache); `implementation` vs `api` kiểm soát lộ transitively; `gradlew` |
| Git object model | Commit = snapshot (tree) + cha, không phải diff; blob theo hash; DAG; `HEAD` |
| `reset` vs `revert` | reset sửa lịch sử (chỉ nhánh riêng); revert an toàn cho lịch sử chia sẻ |
| `stash`/`cherry-pick`/`reflog` | Cất tạm; áp 1 commit cụ thể; lưới cứu sinh sau `reset --hard` |
| Gitflow | `main`/`develop`/`feature`/`release`/`hotfix` — release theo chu kỳ |
| Trunk-Based | 1 nhánh chính, feature ngắn ngày — CI/CD liên tục |
| Rebase vs Merge | Rebase = lịch sử thẳng nhưng đổi hash — không rebase nhánh chung; merge = an toàn, giữ merge commit |
| PR merge strategy | Merge commit / Squash / Rebase-merge — squash phổ biến cho `main` |
| Merge Conflict | Hiểu ý nghĩa nghiệp vụ cả 2 bên; `.gitattributes` giảm conflict giả; `rerere` nhớ cách giải |
| `.gitignore` | `target/`, file IDE, **đặc biệt** thông tin nhạy cảm; lộ key → xóa lịch sử + đổi key ngay |
| Conventional Commits | `feat`/`fix`/`refactor`/`docs`/`test`/`chore`; `!`/`BREAKING CHANGE:` cho major |

---

## 15. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** `mvn test` có tự chạy `mvn compile` trước không? Giải thích theo Build Lifecycle.

**Câu 2.** Thư viện A phụ thuộc `library-x:1.0`, thư viện B phụ thuộc `library-x:2.0`, cả A và B khai báo **trực tiếp** trong `pom.xml`. Maven chọn phiên bản nào? Lệnh nào kiểm tra chắc chắn?

**Câu 3.** File `application-local.properties` chứa mật khẩu thật, vô tình commit 3 commit trước, đã xóa ở commit hiện tại. Có còn an toàn không? Cần làm gì?

**Câu 4.** Phân biệt `git fetch` và `git pull`.

**Câu 5.** `hotfix/*` trong Gitflow nên tạo từ nhánh nào? Sau khi xong merge vào đâu? Vì sao?

**Câu 6.** `git reset --hard HEAD~1` và `git revert HEAD` cùng "hủy" commit gần nhất — khác nhau ở điểm nào? Trường hợp nào bắt buộc dùng `revert`?

**Câu 7.** Đồng nghiệp đã `git pull` nhánh `feature/x` của bạn. Bạn `git rebase main` trên nhánh đó rồi `git push --force`. Chuyện gì xảy ra với đồng nghiệp? Nên làm gì thay vì vậy?

**Câu 8.** `<dependencyManagement>` và `<dependencies>` khác nhau ở điểm nào? Vì sao khai báo `spring-boot-starter-web` trong `<dependencies>` mà không cần ghi `<version>` khi dự án dùng BOM của Spring Boot?

---

### Phần B — Bài tập thực hành (mô tả các bước, không cần chạy thật nếu chưa có môi trường)

**Bài 1 — `pom.xml` hoàn chỉnh cho Spring Boot.**
Viết `pom.xml`: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, driver PostgreSQL (`runtime`), Lombok (`provided`), `spring-boot-starter-test` (`test`), plugin `spring-boot-maven-plugin`. Thêm `<parent>` là `spring-boot-starter-parent` (hoặc BOM qua `<dependencyManagement>`) để không phải ghi version từng starter.

**Bài 2 — Quy trình Git Feature Branch hoàn chỉnh.**
Liệt kê lệnh + giải thích ngắn: (a) tạo `feature/user-registration` từ `develop`; (b) 3 commit riêng biệt cho 3 phần việc (Conventional Commits); (c) `git rebase -i` gộp 2 commit "fix typo" vào commit trước nó, trước khi push; (d) push, mở PR, mô tả PR nên có gì (mục đích, cách test).

**Bài 3 — Giải quyết Merge Conflict hợp lý về nghiệp vụ.**
```java
<<<<<<< HEAD
public double calculateDiscount(double price) { return price * 0.9; }
=======
public double calculateDiscount(double price) {
    if (price > 1000) return price * 0.8;
    return price * 0.95;
}
>>>>>>> feature/tiered-discount
```
Viết bản đã giải quyết, giải thích lý do chọn.

**Bài 4 — `.gitignore` cho Spring Boot + IntelliJ + `.env`.**
Viết `.gitignore` đầy đủ; giải thích từng nhóm loại trừ. Thêm cơ chế `.env.example` (commit được, không chứa giá trị thật) để đồng đội biết cần khai báo biến gì.

**Bài 5 — Cứu một `reset --hard` lỡ tay.**
Mô tả tình huống: đã `git commit` xong 2 tiếng làm việc, lỡ `git reset --hard HEAD~1` xóa mất. Viết các lệnh (kể cả `git reflog`) để tìm lại và khôi phục commit đó về một branch mới.

**Bài 6 — Bài toán tổng hợp: Branching Strategy cho capstone 4 người, 8 tuần.**
Đề xuất (kèm sơ đồ text) chiến lược nhánh phù hợp quy mô/thời gian này — so sánh với Gitflow đầy đủ, giải thích vì sao chọn/không chọn.

---

### Phần C — Nâng cao

**Câu 1.** Vì sao commit trong Git là **snapshot** chứ không phải **diff**, và điều đó ảnh hưởng thế nào tới tốc độ của `git checkout`/`git log` so với hệ VCS lưu diff (ví dụ SVN kiểu cũ)? Vì sao hai file giống hệt nhau ở hai thư mục/commit khác nhau không tốn thêm dung lượng?

**Câu 2.** Giải thích "Nearest Wins" của Maven khi cây phụ thuộc **sâu 3-4 tầng** (không phải khai báo trực tiếp). Cho một ví dụ cụ thể hai đường dẫn phụ thuộc cùng dẫn tới `library-x` ở độ sâu khác nhau, và dự đoán Maven chọn bản nào. Vì sao `mvn dependency:tree` là công cụ bắt buộc trong tình huống này thay vì đoán?

**Câu 3.** Gradle `implementation` vs `api`: dựng ví dụ 3 module `common → core → api` để chứng minh khi đổi một dependency `implementation` bên trong `core`, module `api` **không cần build lại**, còn nếu đổi dependency đó thành `api` thì **phải**. Liên hệ nguyên lý đóng gói/coupling đã học (Module 01.6).

**Câu 4.** So sánh chi phí và rủi ro giữa hai chiến lược giữ nhánh feature "đồng bộ" với `main` trong lúc phát triển dài ngày: (a) định kỳ `git merge main` vào feature, (b) định kỳ `git rebase main`. Trường hợp nào an toàn hơn khi **nhiều người cùng làm chung một nhánh feature**? Vì sao?

**Câu 5.** `git revert` một merge commit phức tạp hơn revert một commit thường — vì sao (gợi ý: merge commit có 2 cha, cần chỉ định `-m`)? Giải thích hệ quả nếu sau đó muốn merge lại đúng nhánh đó lần nữa.

**Câu 6.** Trong quy trình PR dùng "squash and merge" cho `main`, `git bisect` để tìm commit gây bug có còn hiệu quả như khi dùng "merge commit" giữ nguyên từng commit nhỏ không? Giải thích đánh đổi giữa lịch sử `main` sạch (1 PR = 1 dòng) và khả năng "bisect" ở độ chi tiết cao.

**Câu 7.** `-SNAPSHOT` trong Maven và tag Git dùng cho release (`v1.2.0`) phục vụ hai mục đích khác nhau nhưng bổ trợ nhau trong CI/CD (Module 20). Giải thích luồng thực tế: từ commit trên `develop` (version `1.3.0-SNAPSHOT`) tới lúc có bản release `1.3.0` gắn tag Git — điều gì cần đổi ở `pom.xml`, và vì sao **không nên** deploy bản `-SNAPSHOT` lên production.

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Có** — lifecycle chạy tuần tự, gọi phase nào tự chạy hết phase trước đó (`validate → compile → test`).
2. Theo "Nearest Wins" — cả hai cùng độ sâu (khai báo trực tiếp) nên Maven chọn theo **thứ tự khai báo trước** trong `<dependencies>`. Kiểm tra chắc chắn: `mvn dependency:tree`.
3. **Không an toàn** — vẫn còn nguyên trong lịch sử commit cũ, `git log`/`git show` xem lại được. Cần: `git filter-repo`/BFG xóa khỏi lịch sử, và **đổi mật khẩu ngay lập tức**.
4. `git fetch` chỉ tải về, **không** merge vào branch hiện tại — an toàn để xem trước. `git pull` = fetch + merge (hoặc rebase tùy cấu hình) — gộp ngay, có thể conflict bất ngờ.
5. Tạo từ `main` (vì fix khẩn trên code đang chạy thật, không phải code dở dang ở `develop`). Merge vào **cả `main` và `develop`** — vào `main` để có hiệu lực ngay; vào `develop` để fix không "biến mất" ở lần release kế tiếp.
6. `reset --hard` **xóa hẳn** commit khỏi nhánh (và cả thay đổi nếu chưa lưu chỗ khác) — sửa lịch sử. `revert` **thêm commit mới** đảo ngược, giữ nguyên lịch sử cũ. Bắt buộc `revert` khi commit **đã push lên nhánh chia sẻ** — `reset` sẽ làm lịch sử của đồng nghiệp đã pull bị lệch khỏi remote.
7. Đồng nghiệp có bản lịch sử **cũ** (hash chưa đổi); sau force-push, remote có hash **mới hoàn toàn** cho cùng nội dung logic → lần `git pull` tiếp theo của họ báo lỗi phân kỳ nghiêm trọng hoặc tạo merge commit rối. Nên: báo trước cho mọi người đang dùng chung nhánh, hoặc tránh rebase nhánh đã có người khác pull — chỉ rebase nhánh **của riêng mình**, dùng `--force-with-lease` thay vì `--force` để ít nhất được cảnh báo nếu remote đã đổi.
8. `<dependencyManagement>` chỉ **khai báo sẵn** version/scope mặc định cho các GAV, **không** tự thêm dependency vào classpath. `<dependencies>` mới thực sự **thêm** dependency. Dự án dùng BOM `spring-boot-dependencies` (qua `import` scope trong `dependencyManagement`, hoặc gián tiếp qua `spring-boot-starter-parent`) thì mọi `spring-boot-starter-*` khai báo trong `<dependencies>` **lấy version từ BOM** — không cần ghi tay, và đảm bảo các starter cùng một bộ version đã được Spring kiểm tra tương thích.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Lombok cần `<scope>provided</scope>` (chỉ dùng lúc compile để sinh code). Driver PostgreSQL cần `<scope>runtime</scope>`. Dùng `<parent>` `spring-boot-starter-parent` → các starter không cần `<version>`.
- **Bài 2:** `(c)` minh họa: `git rebase -i HEAD~3` mở danh sách 3 commit, đổi `pick` thành `fixup` (hoặc `squash`) ở dòng commit "fix typo" để gộp nó vào commit ngay trước, giữ lịch sử PR gọn trước khi push lần đầu.
- **Bài 3:** Giữ logic **phân bậc** (tiered) từ `feature/tiered-discount` vì nó **bao hàm và mở rộng** ý tưởng của HEAD (HEAD chỉ là một trường hợp đặc biệt/đơn giản hóa) — không phải "xóa 1 bên" mà là nhận ra một bên đã được thay thế hoàn toàn.
- **Bài 4:** Thêm `.env` (và `.env.*.local`) vào `.gitignore`; tạo và **commit** `.env.example` (tên biến, giá trị mẫu/rỗng, không có giá trị thật) để onboard thành viên mới.
- **Bài 5:** `git reflog` → tìm dòng `commit: <message>` ứng với commit đã mất (trước lệnh `reset`) → `git branch recovered-work <hash-tìm-được>` → `git checkout recovered-work` để lấy lại toàn bộ thay đổi.
- **Bài 6:** Quy mô nhỏ, ngắn hạn, không CI/CD liên tục → Gitflow đầy đủ là **over-engineering** (liên hệ Module 01.6 §11). Đề xuất: chỉ `main` + `feature/*` (mỗi người 1 nhánh theo module việc), merge qua PR sau review nhanh, bỏ tầng `develop`/`release`/`hotfix`.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. Vì mỗi commit trỏ thẳng tới một `tree` (danh sách blob) mô tả **toàn bộ trạng thái thư mục** tại thời điểm đó — `checkout` một commit chỉ là "trải" các blob theo tree đó ra working directory, không cần **phát lại tuần tự** hàng nghìn diff từ đầu lịch sử như VCS kiểu diff-chain. Blob được định danh bằng hash nội dung: hai file giống hệt (ở cùng commit hay khác commit, khác thư mục) cho **cùng một hash** → Git chỉ lưu **một bản** blob đó, mọi tree tham chiếu lại cùng blob — không tốn thêm dung lượng.
2. Ví dụ: dự án khai báo trực tiếp `A` (kéo theo `library-x:1.0` ở độ sâu 2) và `B` (kéo theo `library-x:2.0` ở độ sâu 3, qua `B → C → library-x:2.0`). "Nearest Wins" đếm khoảng cách từ **gốc dự án**: nhánh qua `A` ngắn hơn (2 bước) nhánh qua `B → C` (3 bước) → Maven chọn `library-x:1.0`, dù đó có thể **không phải** phiên bản bạn muốn (ví dụ bản mới hơn từ `B` sửa lỗi bảo mật). Không đoán được kết quả chỉ bằng đọc `pom.xml` vì độ sâu và tầng phụ thuộc thay đổi liên tục theo version — `dependency:tree` in ra chính xác cây thật đã resolve, kèm dòng ghi chú "(nearest wins used ...)" khi có xung đột.
3. `common` không đổi. `core` khai báo `implementation 'lib:1.0'` — đổi sang `'lib:2.0'` chỉ ảnh hưởng **classpath biên dịch của chính `core`**; vì `lib` không lộ ra ngoài (`implementation`), Gradle biết **`api` bề mặt của `core` không đổi** → module `api` (phụ thuộc `core`) **không cần recompile**. Nếu khai báo `api 'lib:1.0'` trong `core` rồi đổi version — `lib` là một phần **bề mặt public** của `core` (transitively lộ cho `api`) → Gradle phải coi classpath của `api` đã đổi → recompile. Đây chính là "ẩn chi tiết cài đặt, chỉ lộ hợp đồng" (encapsulation/low coupling — Module 01.6) áp dụng ở cấp build graph, không chỉ cấp class.
4. (a) Merge định kỳ: mỗi lần merge tạo một merge commit trên nhánh feature, lịch sử feature "rối" nhưng **mỗi người trong nhóm thấy đúng một chuỗi sự kiện chung, không ai bị đổi hash**. (b) Rebase định kỳ: lịch sử feature sạch/thẳng nhưng **mỗi lần rebase đổi hash toàn bộ commit đã có** trên nhánh đó — nếu nhiều người cùng làm chung nhánh, người khác đã pull các commit cũ sẽ bị phân kỳ, phải tự rebase/force theo, dễ gây mất đồng bộ và xung đột dây chuyền. Với **nhánh dùng chung nhiều người**, `merge` an toàn hơn hẳn; `rebase` chỉ nên dùng khi nhánh đó là **của riêng một người**, đồng bộ với `main` trước khi mở PR.
5. Merge commit có **hai cha** (nhánh đích và nhánh được merge) — `git revert` cần biết "đảo ngược so với cha nào" nên bắt buộc cờ `-m <số thứ tự cha>` (thường `-m 1` = đảo theo nhánh chính). Hệ quả: sau khi revert một merge, Git coi merge đó **"chưa từng xảy ra"** đối với các thay đổi trong nó; nếu sau này muốn merge lại **đúng nhánh đó** (cùng các commit), Git sẽ không thấy gì để merge nữa (nó nghĩ nội dung đã có/đã bị revert) — phải revert **chính commit-revert** trước ("revert the revert") rồi mới merge lại được bình thường.
6. Kém hiệu quả hơn ở độ chi tiết: `bisect` tìm ra **commit squash của cả PR** (chứa hàng chục thay đổi gộp lại), không chỉ ra chính xác dòng/commit nhỏ nào bên trong PR gây lỗi — phải tự đọc diff của PR đó bằng tay để khoanh vùng tiếp. Đánh đổi: `main` sạch, dễ đọc changelog (1 dòng = 1 tính năng) nhưng mất khả năng bisect ở độ hạt mịn; muốn cả hai thì giữ commit chi tiết **trong nhánh feature** (không xóa lịch sử làm việc) trong khi vẫn squash khi merge vào `main` — chấp nhận là hai cấp độ lịch sử phục vụ hai nhu cầu khác nhau.
7. `develop` build ra `myapp-1.3.0-SNAPSHOT.jar`, CI có thể deploy thử lên môi trường dev/staging, **luôn resolve lại bản mới nhất** — không đáng tin làm bản chốt vì thay đổi liên tục và không có gì đảm bảo bản SNAPSHOT ngày hôm nay giống ngày mai. Khi chốt release: đổi `<version>1.3.0-SNAPSHOT</version>` → `<version>1.3.0</version>` (bỏ suffix), commit, gắn `git tag -a v1.3.0 -m "..."` đúng commit đó, build & deploy bản này (bất biến, không đổi nữa) lên production, rồi bump `pom.xml` trên `develop` lên `1.4.0-SNAPSHOT` để tiếp tục phát triển. Không deploy `-SNAPSHOT` lên production vì: (i) không tái lập được — hai lần build "cùng version" có thể ra nội dung khác nhau nếu dependency SNAPSHOT khác cũng đổi; (ii) không truy vết được chính xác "production đang chạy commit nào" nếu không có tag cố định.

</details>

---

*File tiếp theo trong lộ trình: **Module 10 — Database & SQL** (SELECT/JOIN, transaction ACID, index, thiết kế database, EXPLAIN).*
