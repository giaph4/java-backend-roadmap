# Module 09 — Build Tools & Quản lý dự án

> **Mức độ ưu tiên: Cao (Maven/Git) → Trung bình (Gradle)** — Không có dự án backend thực tế nào build tay bằng `javac` từng file — công cụ build và Git là điều **bắt buộc phải biết thành thạo ngay từ ngày đầu đi thực tập/đi làm**, quan trọng ngang với biết viết code. Nhà tuyển dụng mặc định ứng viên đã biết Maven/Git, nên đây thường **không được dạy kỹ ở trường** nhưng lại gây bối rối nhiều nhất cho sinh viên mới ra trường trong tuần đầu tiên đi làm.

---

## Mục lục

1. [Vì sao cần Build Tool](#1-vì-sao-cần-build-tool)
2. [Maven — cấu trúc dự án & `pom.xml`](#2-maven--cấu-trúc-dự-án--pomxml)
3. [Maven — Dependency Management](#3-maven--dependency-management)
4. [Maven — Vòng đời Build (Build Lifecycle)](#4-maven--vòng-đời-build-build-lifecycle)
5. [Maven — Plugin & Profile](#5-maven--plugin--profile)
6. [Gradle — tổng quan & so sánh với Maven](#6-gradle--tổng-quan--so-sánh-với-maven)
7. [Git — kiến thức nền tảng](#7-git--kiến-thức-nền-tảng)
8. [Git — Branching Model](#8-git--branching-model)
9. [Git — Pull Request & Code Review](#9-git--pull-request--code-review)
10. [Git — xử lý Merge Conflict](#10-git--xử-lý-merge-conflict)
11. [`.gitignore` & Commit Convention](#11-gitignore--commit-convention)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. Vì sao cần Build Tool

Một dự án backend thực tế có thể có **hàng trăm file `.java`**, phụ thuộc vào **hàng chục thư viện bên ngoài** (Spring, Hibernate, Jackson...). Build Tool giải quyết:

1. **Quản lý dependency (thư viện)** — tự động tải về đúng phiên bản thư viện cần dùng, cùng các thư viện **mà thư viện đó cần** (dependency của dependency — gọi là **transitive dependency**), thay vì tự tay tải file `.jar` và quản lý classpath thủ công.
2. **Chuẩn hóa quy trình build** — compile, test, đóng gói (package) theo đúng 1 quy trình thống nhất, chạy được **giống nhau** trên máy của mọi thành viên trong team và trên server CI/CD (Module 20).
3. **Quản lý cấu trúc dự án chuẩn hóa** — mọi dự án Maven/Gradle đều có cấu trúc thư mục **giống nhau**, giúp bất kỳ ai cũng nhanh chóng hiểu được dự án mới khi mở lên lần đầu.

---

## 2. Maven — cấu trúc dự án & `pom.xml`

### Cấu trúc thư mục chuẩn Maven

```
my-project/
├── pom.xml                          ← "trái tim" của dự án Maven — khai báo mọi thứ
├── src/
│   ├── main/
│   │   ├── java/                    ← source code chính
│   │   │   └── com/example/app/
│   │   │       └── Application.java
│   │   └── resources/               ← file cấu hình, không phải code (.properties, .yml...)
│   └── test/
│       ├── java/                    ← code TEST (liên hệ Module 17)
│       └── resources/               ← file cấu hình dành riêng cho môi trường TEST
└── target/                          ← THƯ MỤC TỰ SINH — chứa kết quả build (.class, .jar...), KHÔNG commit lên Git
```

> **Lưu ý quan trọng:** cấu trúc này gọi là **"Standard Directory Layout"** — Maven **mặc định** tìm code ở đúng những vị trí này, không cần cấu hình gì thêm. Đây chính là triết lý **"Convention over Configuration"** (Quy ước thay vì Cấu hình) — 1 trong những nguyên lý thiết kế cốt lõi sẽ gặp lại xuyên suốt khi học Spring Boot (Module 13): tuân theo đúng quy ước mặc định thì hầu như không cần cấu hình gì cả.

### Cấu trúc cơ bản của `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>

    <!-- GAV coordinate — "địa chỉ định danh" DUY NHẤT cho 1 dự án/thư viện trong hệ sinh thái Maven -->
    <groupId>com.example</groupId>       <!-- thường là tên miền công ty đảo ngược -->
    <artifactId>my-project</artifactId>   <!-- tên dự án -->
    <version>1.0.0-SNAPSHOT</version>     <!-- phiên bản — "SNAPSHOT" nghĩa là ĐANG PHÁT TRIỂN, chưa release chính thức -->
    <packaging>jar</packaging>            <!-- kiểu đóng gói: jar (thư viện/ứng dụng độc lập) hoặc war (ứng dụng web triển khai lên Servlet Container) -->

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- xem mục 3 -->
    </dependencies>

    <build>
        <plugins>
            <!-- xem mục 5 -->
        </plugins>
    </build>
</project>
```

### GAV Coordinate — "địa chỉ" định danh duy nhất

`groupId:artifactId:version` (viết tắt **GAV**) là cách Maven **định danh chính xác** 1 thư viện/dự án cụ thể trong kho lưu trữ (Maven Central Repository, hoặc repository nội bộ công ty) — giống như tọa độ GPS, không thể trùng lặp cho 2 thư viện khác nhau.

---

## 3. Maven — Dependency Management

### Khai báo dependency (thư viện phụ thuộc)

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
        <scope>test</scope> <!-- xem bảng scope bên dưới -->
    </dependency>
</dependencies>
```

Khi build, Maven **tự động tải** file `.jar` tương ứng từ **Maven Central Repository** (kho lưu trữ công khai khổng lồ) về máy, lưu vào **thư mục cache cục bộ** (`~/.m2/repository/`) — lần build sau, nếu đã tải rồi, **không cần tải lại**.

### Dependency Scope — kiểm soát phạm vi sử dụng

| Scope | Ý nghĩa |
|---|---|
| `compile` (mặc định) | Cần cho MỌI giai đoạn: compile, test, chạy runtime, và **đóng gói kèm** vào file `.jar`/`.war` cuối cùng |
| `test` | CHỈ cần khi compile/chạy TEST — không đóng gói vào sản phẩm cuối cùng (ví dụ: JUnit, Mockito — Module 17) |
| `provided` | Cần lúc compile, nhưng **KHÔNG** đóng gói kèm — giả định môi trường triển khai (deployment) đã có sẵn (ví dụ: Servlet API khi triển khai lên Tomcat đã có sẵn) |
| `runtime` | KHÔNG cần lúc compile, nhưng cần lúc CHẠY (ví dụ: driver kết nối database — code chỉ dùng qua interface JDBC chuẩn, driver cụ thể chỉ cần có mặt lúc runtime) |

### Transitive Dependency — "dependency của dependency"

```
Dự án của bạn
    │
    └─► spring-boot-starter-web (bạn khai báo)
            │
            ├─► spring-web (Maven TỰ ĐỘNG tải kèm)
            ├─► spring-webmvc (Maven TỰ ĐỘNG tải kèm)
            ├─► tomcat-embed-core (Maven TỰ ĐỘNG tải kèm)
            └─► jackson-databind (Maven TỰ ĐỘNG tải kèm)
```

> Đây chính là lý do khi làm Spring Boot, chỉ cần khai báo **1 dòng** `spring-boot-starter-web` mà tự động có **hàng chục** thư viện liên quan hoạt động cùng nhau — Maven đã **tự động giải quyết toàn bộ cây phụ thuộc (dependency tree)** phía sau, đây chính là ý nghĩa của khái niệm **"Starter"** trong Spring Boot (sẽ học kỹ ở Module 13).

### Dependency Conflict — vấn đề hay gặp khi dự án lớn

Khi 2 thư viện khác nhau **cùng phụ thuộc** vào 1 thư viện thứ 3, nhưng ở **2 phiên bản khác nhau** — Maven cần chọn **1 phiên bản duy nhất** để dùng, theo nguyên tắc **"Nearest Wins"** (dependency khai báo **gần** với dự án của bạn nhất trong cây phụ thuộc sẽ được ưu tiên).

```bash
mvn dependency:tree # lệnh XEM TOÀN BỘ cây phụ thuộc — công cụ CHẨN ĐOÁN quan trọng khi gặp lỗi xung đột phiên bản (ClassNotFoundException, NoSuchMethodError khó hiểu)
```

---

## 4. Maven — Vòng đời Build (Build Lifecycle)

Maven có 1 chuỗi các **giai đoạn (phase)** cố định, chạy **TUẦN TỰ theo đúng thứ tự** — gọi 1 phase sẽ **tự động chạy TẤT CẢ** các phase đứng trước nó.

```
validate → compile → test → package → verify → install → deploy
```

| Phase | Ý nghĩa |
|---|---|
| `validate` | Kiểm tra `pom.xml` hợp lệ, cấu trúc dự án đúng |
| `compile` | Biên dịch code trong `src/main/java` |
| `test` | Chạy Unit Test (`src/test/java`) — liên hệ Module 17 |
| `package` | Đóng gói thành `.jar`/`.war` trong thư mục `target/` |
| `verify` | Chạy các kiểm tra bổ sung (Integration Test, kiểm tra chất lượng code) |
| `install` | Copy file `.jar`/`.war` vào kho **cục bộ** (`~/.m2/repository/`) — để **dự án KHÁC trên cùng máy** có thể dùng như 1 dependency |
| `deploy` | Đẩy lên kho **dùng chung của team/công ty** (remote repository) — để đồng nghiệp khác cũng dùng được |

```bash
mvn compile   # chỉ compile
mvn test      # compile + chạy test (test CẦN compile trước)
mvn package   # compile + test + đóng gói (package CẦN cả compile lẫn test trước)
mvn clean     # XÓA thư mục target/ — thường kết hợp: "mvn clean package" để build LẠI TỪ ĐẦU, tránh dùng nhầm file .class cũ
mvn clean install # câu lệnh RẤT HAY DÙNG trong thực tế — build sạch từ đầu và cài vào kho local
```

> **Lưu ý quan trọng:** `mvn package` **tự động** chạy `test` trước — nếu có Unit Test **thất bại**, quá trình `package` sẽ **DỪNG LẠI**, không tạo ra file `.jar` — đây là cơ chế **bảo vệ chất lượng** mặc định của Maven, đảm bảo không ai vô tình đóng gói/deploy code đang bị lỗi test.

---

## 5. Maven — Plugin & Profile

### Plugin — mở rộng khả năng của Maven

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId> <!-- cho phép "mvn spring-boot:run" chạy trực tiếp ứng dụng -->
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>17</source>
                <target>17</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Profile — cấu hình khác nhau cho từng môi trường (dev/staging/prod)

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <db.url>jdbc:h2:mem:devdb</db.url> <!-- database TRONG BỘ NHỚ, dùng để dev nhanh -->
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <db.url>jdbc:postgresql://prod-server:5432/maindb</db.url>
        </properties>
    </profile>
</profiles>
```

```bash
mvn package -P prod # kích hoạt CỤ THỂ profile "prod" khi build
```

> Đây là khái niệm nền tảng để hiểu khái niệm **Spring Profile** (`application-dev.yml`, `application-prod.yml`) sẽ học ở Module 13 — dù cơ chế cụ thể khác nhau, ý tưởng **tách cấu hình theo môi trường** là hoàn toàn tương tự.

---

## 6. Gradle — tổng quan & so sánh với Maven

Gradle là công cụ build **thế hệ mới hơn**, ngày càng được nhiều dự án lớn (kể cả chính Spring Framework) sử dụng thay Maven.

### `build.gradle` (Groovy DSL) — tương đương `pom.xml`

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
}

group = 'com.example'
version = '1.0.0-SNAPSHOT'

repositories {
    mavenCentral() // vẫn dùng CHUNG kho Maven Central
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web' // tương đương scope "compile" của Maven
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'        // tương đương scope "test"
}
```

### `build.gradle.kts` (Kotlin DSL) — lựa chọn hiện đại hơn, có type-safety

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
```

### Bảng so sánh Maven vs Gradle

| Tiêu chí | Maven | Gradle |
|---|---|---|
| Ngôn ngữ cấu hình | XML (`pom.xml`) — khai báo (declarative), dài dòng | Groovy/Kotlin DSL — vừa khai báo vừa lập trình được (script), ngắn gọn hơn |
| Tốc độ build | Chậm hơn — build lại TOÀN BỘ mỗi lần (trừ khi cấu hình thêm) | **Nhanh hơn đáng kể** nhờ cơ chế **Incremental Build** (chỉ build lại phần THAY ĐỔI) và **Build Cache** |
| Độ linh hoạt | Cứng nhắc hơn — theo đúng lifecycle cố định | Linh hoạt hơn — có thể viết task tùy chỉnh dễ dàng bằng code thực sự |
| Độ phổ biến hiện tại | Vẫn RẤT phổ biến, đặc biệt ở doanh nghiệp lâu năm, dự án lớn ổn định | Ngày càng phổ biến ở dự án MỚI, đặc biệt phổ biến trong hệ sinh thái Android (bắt buộc) |
| Độ dễ học ban đầu | Dễ đọc hơn (dù dài dòng) vì cấu trúc XML tường minh | Cần hiểu thêm cú pháp Groovy/Kotlin, đường cong học tập ban đầu dốc hơn 1 chút |

> **Khuyến nghị thực tế cho người mới:** nên **thành thạo Maven trước** (phổ biến hơn ở đa số job posting fresher/junior tại Việt Nam, dễ đọc hơn cho người mới), sau đó học thêm Gradle khi đã quen thuộc với khái niệm build tool nói chung — 2 công cụ chia sẻ **CÙNG một tư duy cốt lõi** (dependency management, build lifecycle), chỉ khác cú pháp và 1 số đặc tính kỹ thuật.

---

## 7. Git — kiến thức nền tảng

### Git là gì và khác gì GitHub/GitLab

**Git** là hệ thống **quản lý phiên bản phân tán (Distributed Version Control System)** — chạy **cục bộ** trên máy, theo dõi lịch sử thay đổi của code. **GitHub/GitLab/Bitbucket** là các **dịch vụ lưu trữ repository từ xa (remote)**, xây dựng thêm tính năng cộng tác (Pull Request, Issue Tracking, CI/CD...) **trên nền tảng** Git.

### Khái niệm cốt lõi

```
Working Directory  →  Staging Area  →  Local Repository  →  Remote Repository
   (thư mục code       (git add)         (git commit)          (git push)
    đang sửa)
```

```bash
git status          # xem trạng thái hiện tại: file nào đã sửa, đã staged, chưa track...
git add file.java    # đưa file vào Staging Area (chuẩn bị commit)
git add .            # đưa TẤT CẢ file đã thay đổi vào Staging Area
git commit -m "Thêm tính năng đăng nhập" # LƯU snapshot hiện tại vào Local Repository, kèm message mô tả
git push origin main  # đẩy commit từ Local Repository lên Remote Repository (nhánh "main")
git pull origin main  # kéo thay đổi MỚI NHẤT từ Remote về, TỰ ĐỘNG merge vào nhánh hiện tại
git log              # xem lịch sử commit
git log --oneline --graph # xem lịch sử DẠNG RÚT GỌN, có sơ đồ nhánh trực quan — RẤT hay dùng
```

### 3 vùng "trạng thái" của 1 file trong Git

```
Untracked  →  Modified  →  Staged  →  Committed
(file MỚI,    (đã SỬA      (đã "git    (đã lưu
 Git chưa      nhưng CHƯA   add", chờ   vĩnh viễn
 biết đến)     add)         commit)     vào lịch sử)
```

---

## 8. Git — Branching Model

### Branch (nhánh) — cho phép phát triển tính năng ĐỘC LẬP, không ảnh hưởng code chính

```bash
git branch feature/login          # tạo nhánh MỚI (chưa chuyển sang)
git checkout feature/login          # CHUYỂN sang nhánh đó
git checkout -b feature/login       # tạo VÀ chuyển sang LUÔN, trong 1 lệnh (hay dùng nhất)
git switch feature/login            # cú pháp HIỆN ĐẠI hơn của checkout (Git 2.23+), rõ ràng ý nghĩa hơn

git branch -a                       # liệt kê TẤT CẢ nhánh (local + remote)
git branch -d feature/login          # XÓA nhánh (sau khi đã merge xong, không cần nữa)
```

### Gitflow — mô hình branching truyền thống, có cấu trúc chặt chẽ

```
main (production — LUÔN ổn định, code đang chạy thật)
  │
  └── develop (nhánh TÍCH HỢP — nơi các feature branch hội tụ về)
        │
        ├── feature/login (phát triển tính năng đăng nhập)
        ├── feature/payment (phát triển tính năng thanh toán)
        │
        └── release/1.2.0 (chuẩn bị phát hành — chỉ sửa lỗi nhỏ, không thêm tính năng mới)
              │
              └── hotfix/critical-bug (sửa lỗi KHẨN CẤP trực tiếp trên production, không đợi qua develop)
```

| Loại nhánh | Mục đích |
|---|---|
| `main`/`master` | Code **PRODUCTION**, luôn ổn định, sẵn sàng deploy |
| `develop` | Nhánh **tích hợp** — nơi các feature hoàn thành hội tụ trước khi release |
| `feature/*` | Phát triển **1 tính năng cụ thể**, tách từ `develop`, merge NGƯỢC lại `develop` khi xong |
| `release/*` | Chuẩn bị cho 1 lần **phát hành** — chỉ sửa bug nhỏ, không thêm tính năng mới |
| `hotfix/*` | Sửa lỗi **khẩn cấp** trực tiếp trên `main` (production đang gặp sự cố), sau đó merge cả vào `main` VÀ `develop` |

### Trunk-Based Development — mô hình hiện đại, đơn giản hơn, phổ biến ở nhiều công ty áp dụng CI/CD mạnh

```
main (nhánh DUY NHẤT, LUÔN có thể deploy được)
  │
  ├── feature/login (branch NGẮN NGÀY, merge lại main NHANH CHÓNG — thường trong vài ngày, không kéo dài)
  └── feature/payment
```

> **So sánh triết lý:** Gitflow phù hợp dự án có **chu kỳ phát hành cố định, chậm rãi** (ví dụ phát hành theo quý). Trunk-Based Development phù hợp team áp dụng **CI/CD liên tục** (Module 20), deploy nhiều lần/ngày, ưu tiên merge nhanh, tránh feature branch "sống" quá lâu (dễ gây conflict lớn, khó merge). Nhiều công ty công nghệ hiện đại (đặc biệt startup, scale-up) đang có xu hướng chuyển sang Trunk-Based hoặc các biến thể đơn giản hóa của Gitflow.

---

## 9. Git — Pull Request & Code Review

**Pull Request (PR)** (GitHub) / **Merge Request (MR)** (GitLab) — cơ chế **yêu cầu** merge code từ 1 branch vào branch khác, đi kèm quy trình **Code Review** trước khi được chấp thuận.

### Quy trình chuẩn thực tế trong team

```
1. git checkout -b feature/login          (tạo branch MỚI từ develop)
2. Viết code, "git commit" NHIỀU LẦN theo tiến độ
3. git push origin feature/login           (đẩy branch lên remote)
4. Mở Pull Request trên GitHub/GitLab: feature/login → develop
5. Đồng nghiệp REVIEW code — để lại comment góp ý, yêu cầu sửa
6. Sửa code theo góp ý, commit VÀ push THÊM (PR TỰ ĐỘNG cập nhật)
7. Khi được APPROVE — merge Pull Request vào develop
8. Xóa branch feature/login (đã hoàn thành nhiệm vụ)
```

### Lợi ích của Code Review (không chỉ là "thủ tục")

1. **Bắt lỗi SỚM** trước khi code chạy vào production — rẻ hơn rất nhiều so với fix bug đã lên production.
2. **Chia sẻ kiến thức** trong team — người khác hiểu được code bạn viết, tránh tình trạng "chỉ 1 người biết đoạn code này hoạt động ra sao" (Bus Factor thấp — rủi ro nếu người đó nghỉ việc).
3. **Đảm bảo tính nhất quán** về coding convention, kiến trúc trong toàn bộ codebase.
4. **CI/CD tự động** (Module 20) thường được cấu hình chạy test/kiểm tra chất lượng code **ngay trên Pull Request**, trước khi cho phép merge.

---

## 10. Git — xử lý Merge Conflict

**Merge Conflict** xảy ra khi Git **không thể tự động** kết hợp thay đổi từ 2 nguồn khác nhau — thường do **2 người cùng sửa CHUNG 1 dòng code** ở 2 branch khác nhau.

```java
<<<<<<< HEAD
    private String status = "PENDING"; // code CỦA BẠN (branch hiện tại)
=======
    private String status = "NEW";     // code TỪ NHÁNH ĐANG MERGE VÀO
>>>>>>> feature/order-status
```

### Quy trình giải quyết Merge Conflict

```bash
git merge feature/order-status  # thử merge, Git BÁO có conflict, chỉ ra CHÍNH XÁC file/dòng nào

# 1. MỞ file bị conflict, đọc kỹ CẢ HAI phiên bản
# 2. QUYẾT ĐỊNH giữ lại phần nào (có thể giữ 1 trong 2, hoặc kết hợp CẢ HAI, hoặc viết logic HOÀN TOÀN MỚI)
# 3. XÓA các dấu <<<<<<< , ======= , >>>>>>> sau khi đã chỉnh sửa xong

git add file.java     # đánh dấu ĐàGIẢI QUYẾT xong conflict cho file này
git commit             # HOÀN TẤT merge (Git thường TỰ TẠO sẵn commit message mô tả cuộc merge)
```

> **Nguyên tắc quan trọng khi giải quyết conflict:** **KHÔNG** chỉ đơn giản chọn 1 bên rồi xóa bên còn lại một cách máy móc — luôn **đọc hiểu ý nghĩa nghiệp vụ** của cả 2 thay đổi, đôi khi cần **trao đổi trực tiếp** với đồng nghiệp đã viết đoạn code kia để hiểu rõ ý định trước khi quyết định giữ/sửa như thế nào, tránh vô tình **xóa mất 1 fix bug quan trọng** của người khác.

---

## 11. `.gitignore` & Commit Convention

### `.gitignore` — loại trừ file KHÔNG nên commit lên Git

```gitignore
# Build output — Maven/Gradle TỰ SINH LẠI được, không cần lưu trữ lịch sử
target/
build/

# IDE-specific files — mỗi người dùng IDE khác nhau, không nên áp đặt lên cả team
.idea/
*.iml
.vscode/

# File cấu hình chứa THÔNG TIN NHẠY CẢM — TUYỆT ĐỐI không commit (mật khẩu database, API key...)
application-local.properties
.env

# File hệ điều hành tự sinh
.DS_Store       # macOS
Thumbs.db       # Windows
```

> ⚠️ **Lưu ý bảo mật cực kỳ quan trọng:** nếu **lỡ** commit 1 file chứa mật khẩu/API key thật lên Git, **chỉ xóa file đó ở commit sau KHÔNG ĐỦ** — thông tin đó **vẫn còn nguyên** trong **lịch sử commit cũ**, ai cũng có thể xem lại được. Cần dùng công cụ chuyên biệt (`git filter-repo`, BFG Repo-Cleaner) để **xóa hoàn toàn khỏi lịch sử**, và **luôn nên đổi lại (rotate) mật khẩu/key đó ngay lập tức** vì coi như đã bị lộ.

### Commit Message Convention — quy ước viết commit rõ ràng, có cấu trúc

**Conventional Commits** — chuẩn phổ biến nhất hiện nay:

```
<type>(<phạm vi tùy chọn>): <mô tả ngắn gọn>

[phần thân chi tiết hơn, tùy chọn]
```

| Type | Ý nghĩa |
|---|---|
| `feat` | Thêm tính năng MỚI |
| `fix` | Sửa lỗi (bug fix) |
| `refactor` | Tái cấu trúc code, KHÔNG thay đổi hành vi bên ngoài |
| `docs` | Chỉ thay đổi tài liệu (README, comment...) |
| `test` | Thêm/sửa test, không đổi logic nghiệp vụ |
| `chore` | Việc lặt vặt (cập nhật dependency, cấu hình build...) |

```bash
git commit -m "feat(auth): thêm chức năng đăng nhập bằng Google OAuth2"
git commit -m "fix(payment): sửa lỗi tính sai phí ship khi giỏ hàng rỗng"
git commit -m "refactor(order): tách logic tính tổng tiền ra OrderCalculator riêng"
```

> **Lợi ích thực tế:** commit message có cấu trúc rõ ràng giúp **tự động sinh Changelog**, dễ dàng **tìm lại** commit liên quan đến 1 tính năng/bug cụ thể (`git log --grep="payment"`), và giúp cả team **hiểu nhanh** lịch sử thay đổi mà không cần đọc chi tiết từng diff.

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| Standard Directory Layout | `src/main/java`, `src/main/resources`, `src/test/java` — quy ước Maven mặc định |
| GAV Coordinate | `groupId:artifactId:version` — định danh duy nhất 1 thư viện |
| Dependency Scope | `compile`/`test`/`provided`/`runtime` — kiểm soát khi nào thư viện thực sự cần |
| Transitive Dependency | Dependency của dependency được tự động tải kèm — nền tảng của Spring Boot Starter |
| Maven Build Lifecycle | `validate → compile → test → package → verify → install → deploy` — tuần tự |
| `mvn clean package` | Câu lệnh RẤT hay dùng — build sạch từ đầu, tự chạy test trước khi đóng gói |
| Gradle vs Maven | Gradle nhanh hơn (incremental build), linh hoạt hơn; Maven dễ đọc hơn cho người mới |
| Gitflow | `main`/`develop`/`feature`/`release`/`hotfix` — phù hợp chu kỳ release chậm |
| Trunk-Based Development | 1 nhánh chính, feature branch ngắn ngày — phù hợp CI/CD liên tục |
| Merge Conflict | Xảy ra khi 2 nguồn cùng sửa 1 dòng — luôn hiểu ý nghĩa nghiệp vụ trước khi quyết định giữ bên nào |
| `.gitignore` | Loại trừ `target/`, file IDE, và ĐẶC BIỆT là file chứa thông tin nhạy cảm |
| Conventional Commits | `feat`/`fix`/`refactor`/`docs`/`test`/`chore` — commit message có cấu trúc |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Chạy `mvn test` có tự động chạy `mvn compile` trước không? Giải thích theo Build Lifecycle.

**Câu 2.** Thư viện A phụ thuộc `library-x:1.0`, thư viện B phụ thuộc `library-x:2.0`, cả A và B đều được khai báo TRỰC TIẾP trong `pom.xml` của bạn. Maven sẽ chọn phiên bản nào? Dùng lệnh gì để kiểm tra chắc chắn?

**Câu 3.** File `application-local.properties` chứa mật khẩu database thật đã VÔ TÌNH bị commit lên Git 3 commit trước, sau đó đã bị xóa ở commit hiện tại. Thông tin mật khẩu đó có còn an toàn không? Cần làm gì?

**Câu 4.** Phân biệt `git fetch` và `git pull` — điểm khác biệt cốt lõi là gì?

**Câu 5.** Trong Gitflow, `hotfix/*` branch nên được tạo từ nhánh nào, và sau khi hoàn thành cần merge vào những nhánh nào? Giải thích tại sao.

---

### Phần B — Bài tập thực hành (mô tả các bước, không cần chạy thật nếu chưa có môi trường)

**Bài 1 — Viết `pom.xml` hoàn chỉnh cho dự án Spring Boot cơ bản.**
Viết `pom.xml` cho 1 dự án Spring Boot đơn giản gồm: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, driver PostgreSQL (scope `runtime`), Lombok (dùng để giảm code boilerplate — sẽ gặp lại nhiều ở Module tiếp theo), và `spring-boot-starter-test` (scope `test`). Thêm plugin `spring-boot-maven-plugin` để có thể chạy `mvn spring-boot:run`.

**Bài 2 — Mô phỏng quy trình Git Feature Branch hoàn chỉnh.**
Viết ra (dạng danh sách lệnh, kèm giải thích ngắn từng bước) quy trình đầy đủ để: (a) tạo branch mới `feature/user-registration` từ `develop`, (b) thực hiện 3 commit riêng biệt cho 3 phần việc nhỏ (dùng Conventional Commits), (c) đẩy lên remote, (d) mô tả bước mở Pull Request và những gì nên có trong PR description (mục đích thay đổi, cách test, ảnh chụp màn hình nếu có UI...).

**Bài 3 — Giải quyết Merge Conflict giả định.**
Cho 2 đoạn code xung đột sau (2 người cùng sửa method `calculateDiscount` ở 2 branch khác nhau):
```java
<<<<<<< HEAD
public double calculateDiscount(double price) {
    return price * 0.9; // giảm 10%
}
=======
public double calculateDiscount(double price) {
    if (price > 1000) return price * 0.8; // giảm 20% cho đơn hàng lớn
    return price * 0.95; // giảm 5% cho đơn hàng thường
}
>>>>>>> feature/tiered-discount
```
Viết ra phiên bản đã giải quyết conflict một cách **hợp lý về mặt nghiệp vụ** (không đơn giản chọn 1 trong 2 mà bỏ qua ý tưởng còn lại), giải thích ngắn gọn lý do lựa chọn.

**Bài 4 — Thiết kế `.gitignore` cho dự án Spring Boot + IntelliJ.**
Viết file `.gitignore` đầy đủ cho 1 dự án Spring Boot dùng Maven, phát triển bằng IntelliJ IDEA, có dùng file `.env` để lưu biến môi trường local. Liệt kê VÀ giải thích ngắn gọn lý do loại trừ từng nhóm file/thư mục.

**Bài 5 — Bài toán tổng hợp: thiết kế Branching Strategy cho dự án capstone nhóm.**
Giả sử bạn đang làm capstone theo nhóm 4 người, dự kiến demo/nộp bài sau 8 tuần, KHÔNG có yêu cầu deploy liên tục nhiều lần/ngày. Đề xuất (bằng lời, có thể vẽ sơ đồ text đơn giản) 1 branching strategy phù hợp — chọn Gitflow đầy đủ, Trunk-Based đơn giản, hay 1 biến thể rút gọn ở giữa — giải thích lý do lựa chọn dựa trên quy mô team, thời gian dự án, và tần suất release thực tế của 1 đồ án capstone (khác với dự án doanh nghiệp thực tế có CI/CD liên tục).

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Có** — Maven Build Lifecycle chạy TUẦN TỰ, gọi bất kỳ phase nào sẽ tự động chạy **TẤT CẢ** phase đứng trước nó theo đúng thứ tự (`validate → compile → test`), nên `mvn test` chắc chắn kích hoạt `compile` trước đó.
2. Theo nguyên tắc **"Nearest Wins"** — vì CẢ HAI đều được khai báo **trực tiếp** trong `pom.xml` (cùng "khoảng cách" tới dự án gốc), Maven sẽ chọn theo thứ tự **khai báo TRƯỚC trong file `pom.xml`** (dependency nào xuất hiện TRƯỚC trong danh sách `<dependencies>` sẽ được ưu tiên trong trường hợp "hòa" về độ gần). Dùng `mvn dependency:tree` để xem chính xác phiên bản nào được chọn thực tế và toàn bộ lý do.
3. **KHÔNG an toàn** — dù đã xóa ở commit hiện tại, thông tin mật khẩu **vẫn còn nguyên vẹn** trong lịch sử Git (ở commit cũ 3 bước trước), bất kỳ ai có quyền truy cập repository (kể cả khi repository là private, nhưng nhiều thành viên team/tổ chức có quyền xem) đều có thể `git log`/`git show` để xem lại nội dung file ở commit cũ đó. Cần: (a) dùng công cụ như BFG Repo-Cleaner/`git filter-repo` để xóa HOÀN TOÀN khỏi lịch sử, (b) **NGAY LẬP TỨC đổi mật khẩu database đó** — coi như đã bị lộ và không thể tin tưởng được nữa dù có xóa khỏi lịch sử Git.
4. `git fetch` chỉ **TẢI VỀ** thông tin/commit mới nhất từ remote, **KHÔNG tự động merge** vào branch hiện tại đang làm việc — an toàn để "xem trước" có gì mới mà không ảnh hưởng code đang sửa dở. `git pull` = `git fetch` + `git merge` (hoặc `git rebase` tùy cấu hình) **GỘP LUÔN** — tự động merge thay đổi mới vào branch hiện tại ngay lập tức, có thể gây conflict bất ngờ nếu đang có thay đổi chưa commit.
5. `hotfix/*` nên tạo từ nhánh **`main`** (vì đây là lỗi khẩn cấp đang ảnh hưởng PRODUCTION đang chạy thật, cần fix dựa trên đúng code đang chạy production, không phải code đang phát triển dở dang ở `develop`). Sau khi hoàn thành, cần merge vào **CẢ `main` VÀ `develop`** — vào `main` để fix có hiệu lực ngay trên production; vào `develop` để đảm bảo fix này **không bị mất** khi lần release tiếp theo từ `develop` được thực hiện (nếu không merge vào `develop`, bug đã fix có thể **tái xuất hiện** ở lần release kế tiếp).

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Lombok cần thêm `<scope>provided</scope>` (chỉ dùng lúc compile để sinh code, không cần đóng gói vào sản phẩm cuối). Driver PostgreSQL dùng `<scope>runtime</scope>` vì code chỉ thao tác qua interface JDBC chuẩn, driver cụ thể chỉ cần có mặt lúc chạy thực tế.
- **Bài 3:** Phiên bản hợp lý: giữ lại **logic phân bậc** (tiered discount) từ nhánh `feature/tiered-discount` vì nó **bao hàm và mở rộng** ý tưởng giảm giá của phiên bản kia (nhánh HEAD chỉ là 1 trường hợp đặc biệt/đơn giản hóa của logic phân bậc), không đơn giản là "xóa 1 bên" mà là **nhận ra 1 bên đã lỗi thời/được thay thế hoàn toàn** bởi bên còn lại — đây chính là kỹ năng "đọc hiểu ý nghĩa nghiệp vụ" đã nhấn mạnh ở mục 10.
- **Bài 4:** Ngoài các mục đã liệt kê ở mục 11, dự án dùng `.env` cần thêm dòng `.env` (và có thể `.env.local`, `.env.*.local`) vào `.gitignore`, đồng thời **nên tạo thêm** file `.env.example` (KHÔNG chứa giá trị thật, chỉ có tên biến với giá trị mẫu/rỗng) và **CÓ commit** file `.env.example` này — giúp thành viên mới trong team biết cần khai báo những biến môi trường nào mà không lộ giá trị thật.
- **Bài 5:** Với quy mô nhỏ (4 người), thời gian ngắn (8 tuần), không có áp lực CI/CD liên tục — **Gitflow đầy đủ thường là OVER-ENGINEERING** (quá phức tạp so với nhu cầu thực tế, liên hệ lại khái niệm này ở Module 02.3 — SOLID). Đề xuất hợp lý: **biến thể rút gọn** — chỉ cần `main` (code ổn định, sẵn sàng demo) + `feature/*` (mỗi thành viên làm 1 nhánh riêng cho phần việc của mình, đặt tên rõ ràng theo module: `feature/auth`, `feature/booking-api`...) — merge trực tiếp vào `main` qua Pull Request sau khi các thành viên khác review nhanh, KHÔNG cần thêm tầng `develop`/`release`/`hotfix` phức tạp vì không có nhu cầu quản lý nhiều phiên bản release song song như dự án doanh nghiệp thực tế.

</details>

---

*File tiếp theo trong lộ trình: **Module 10 — Database & SQL** (SELECT/JOIN, transaction ACID, index, thiết kế database, EXPLAIN).*
