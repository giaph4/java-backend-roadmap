# Module 20 — DevOps cơ bản cho Backend Developer

> **Mức ưu tiên: 🟡 Trung bình (nhưng gần như bắt buộc trong công việc thực tế)**
> **Vì sao quan trọng:** Bạn có thể viết code Java Backend hoàn hảo, nhưng nếu không biết đóng gói (Docker) và triển khai (CI/CD) nó, sản phẩm không bao giờ tới được tay người dùng. Đây là kỹ năng "cầu nối" giữa Developer và Operations — hầu hết công ty hiện nay yêu cầu Backend Developer hiểu ít nhất ở mức cơ bản Docker/CI-CD, dù không cần thành thạo như 1 DevOps Engineer chuyên trách. Không biết Docker cũng đồng nghĩa không hiểu được câu nói quen thuộc "chạy được trên máy tôi mà" (works on my machine) — vấn đề Docker sinh ra để giải quyết.

---

## Mục lục

1. [Vấn đề "Works on my machine" — vì sao cần Docker](#1-vấn-đề-works-on-my-machine)
2. [Docker Image vs Container](#2-docker-image-vs-container)
3. [Dockerfile — đóng gói ứng dụng Spring Boot](#3-dockerfile)
4. [Docker Compose — chạy nhiều container cùng lúc](#4-docker-compose)
5. [Linux Command Line cần thiết](#5-linux-command-line-cần-thiết)
6. [CI/CD — khái niệm & luồng hoạt động](#6-cicd--khái-niệm--luồng-hoạt-động)
7. [GitHub Actions — CI/CD thực hành](#7-github-actions)
8. [Giới thiệu Kubernetes](#8-giới-thiệu-kubernetes)
9. [⚠️ Các bẫy hay gặp](#9-các-bẫy-hay-gặp)
10. [Tổng kết — Bảng ghi nhớ nhanh](#10-tổng-kết--bảng-ghi-nhớ-nhanh)
11. [Bài tập luyện tập](#11-bài-tập-luyện-tập)

---

## 1. Vấn đề "Works on my machine"

**Tình huống kinh điển:** Code chạy hoàn hảo trên máy Developer (Java 21, MySQL 8, cấu hình riêng), nhưng khi deploy lên server (Java 17, MySQL 5.7, thiếu 1 biến môi trường) → **lỗi tùm lum** hoặc thậm chí không chạy được.

**Nguyên nhân gốc rễ:** Sự khác biệt về **môi trường** (OS, version ngôn ngữ, dependency, biến môi trường, cấu hình...) giữa máy dev, máy test, và server production.

### Virtual Machine (VM) — giải pháp cũ, nặng nề

```
┌─────────────────────────────┐
│   VM 1        │   VM 2        │
│ ┌───────────┐ │ ┌───────────┐ │
│ │Guest OS    │ │ │Guest OS    │ │  <- Mỗi VM có HỆ ĐIỀU HÀNH RIÊNG (nặng, chậm khởi động)
│ │ (đầy đủ)   │ │ │ (đầy đủ)   │ │
│ ├───────────┤ │ ├───────────┤ │
│ │ App        │ │ │ App        │ │
│ └───────────┘ │ └───────────┘ │
├─────────────────────────────┤
│         Hypervisor            │
├─────────────────────────────┤
│         Host OS               │
└─────────────────────────────┘
```

### Docker (Container) — giải pháp nhẹ, nhanh, phổ biến hiện nay

```
┌───────────┐ ┌───────────┐ ┌───────────┐
│Container 1 │ │Container 2 │ │Container 3 │  <- KHÔNG có OS riêng, dùng CHUNG Kernel của Host
│  App +      │ │  App +      │ │  App +      │     Chỉ đóng gói riêng: code, thư viện, config
│  Dependency │ │  Dependency │ │  Dependency │
└───────────┘ └───────────┘ └───────────┘
├─────────────────────────────────────┤
│           Docker Engine                │
├─────────────────────────────────────┤
│              Host OS                   │
└─────────────────────────────────────┘
```

| | Virtual Machine | Docker Container |
|---|---|---|
| Kernel | Mỗi VM có OS + Kernel riêng | Dùng CHUNG Kernel của Host OS |
| Kích thước | Nặng (hàng GB) | Nhẹ (hàng chục-trăm MB) |
| Thời gian khởi động | Chậm (phút) | Nhanh (giây, thậm chí mili-giây) |
| Cô lập (Isolation) | Cô lập hoàn toàn (mạnh hơn) | Cô lập ở mức process (nhẹ hơn, đủ dùng cho đa số trường hợp) |
| Mật độ trên 1 máy | Ít VM/máy vật lý | Nhiều Container/máy vật lý |

**Lợi ích cốt lõi của Docker:** Đóng gói ứng dụng + TOÀN BỘ môi trường cần thiết (Java version, biến môi trường, thư viện hệ thống) thành **1 đơn vị duy nhất (Image)** — chạy ở BẤT KỲ đâu có Docker Engine đều cho **cùng 1 kết quả**, xóa bỏ hoàn toàn vấn đề "works on my machine".

---

## 2. Docker Image vs Container

Đây là 2 khái niệm **bị nhầm lẫn nhiều nhất** với người mới học Docker.

```
Image  = "Bản thiết kế" / "Class" (trong lập trình OOP)
Container = "Instance" được tạo ra TỪ Image đó (giống new Object() từ Class)
```

| | Docker Image | Docker Container |
|---|---|---|
| Bản chất | File tĩnh (read-only), chứa code + dependency + config | Instance ĐANG CHẠY được tạo từ Image |
| Số lượng | 1 Image | Có thể tạo NHIỀU Container từ CÙNG 1 Image |
| Trạng thái | Bất biến (immutable) | Có thể start/stop/xóa độc lập |
| Ví dụ | `myapp:1.0` (file .tar chứa mọi thứ) | `myapp-instance-1`, `myapp-instance-2` (đang chạy) |

```bash
# Image - "bản thiết kế" tĩnh
docker images                          # Liệt kê các Image đã tải/build

# Container - "instance" đang chạy được TẠO RA từ Image
docker run -d -p 8080:8080 myapp:1.0   # Tạo và chạy 1 Container TỪ Image "myapp:1.0"
docker run -d -p 8081:8080 myapp:1.0   # Tạo THÊM 1 Container KHÁC, CÙNG Image, port khác
docker ps                              # Liệt kê các Container ĐANG CHẠY
```

### Docker Layer — cách Image được xây dựng

Mỗi Image được xây dựng từ **nhiều Layer (lớp) xếp chồng lên nhau** — mỗi lệnh trong Dockerfile (mục 3) thường tạo ra 1 Layer mới.

```
Layer 4: COPY app.jar /app/app.jar        <- Layer TRÊN CÙNG (thay đổi thường xuyên nhất)
Layer 3: RUN apt-get install curl
Layer 2: FROM eclipse-temurin:21-jre       <- Base Image
Layer 1: (Kernel Layer - dùng chung với Host)
```

**Lợi ích của cơ chế Layer:** Docker **cache lại từng Layer** — nếu Layer nào không đổi (VD: base image, dependency), lần build sau **không cần build lại**, chỉ build lại Layer bị thay đổi → tăng tốc quá trình build đáng kể.

---

## 3. Dockerfile

**Dockerfile** là file văn bản chứa **các bước tuần tự** để Docker xây dựng (build) 1 Image.

### Dockerfile đơn giản (chưa tối ưu) cho ứng dụng Spring Boot

```dockerfile
FROM eclipse-temurin:21-jre           # Base Image - chỉ có Java Runtime (JRE), không cần full JDK để CHẠY app
WORKDIR /app                          # Đặt thư mục làm việc bên trong container
COPY target/myapp-1.0.0.jar app.jar   # Copy file JAR đã build sẵn vào Image
EXPOSE 8080                           # Khai báo port ứng dụng sẽ lắng nghe (chỉ là "tài liệu", không tự mở port)
ENTRYPOINT ["java", "-jar", "app.jar"] # Lệnh chạy khi Container khởi động
```

```bash
# Build Image từ Dockerfile
docker build -t myapp:1.0 .

# Chạy Container từ Image vừa build
docker run -d -p 8080:8080 --name myapp-container myapp:1.0
```

### Multi-stage Build — Dockerfile tối ưu hơn (khuyến nghị dùng trong thực tế)

**Vấn đề với Dockerfile đơn giản ở trên:** Bạn phải **tự build file JAR TRƯỚC** (`mvn clean package`) rồi mới `docker build` — không tự động hóa hoàn toàn, và Image cuối cùng có thể lẫn cả công cụ build (Maven, source code) không cần thiết khi CHẠY ứng dụng.

```dockerfile
# ===== STAGE 1: Build - dùng Image có đầy đủ Maven + JDK để BUILD =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline          # Tải dependency TRƯỚC (tận dụng Docker Layer Cache -
                                          # nếu pom.xml không đổi, bước này không cần chạy lại)
COPY src ./src
RUN mvn clean package -DskipTests      # Build ra file JAR

# ===== STAGE 2: Runtime - CHỈ lấy file JAR đã build, dùng Image NHẸ để CHẠY =====
FROM eclipse-temurin:21-jre-alpine     # "alpine" - bản Linux siêu nhẹ, giảm kích thước Image đáng kể
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar  # Copy CHỈ file JAR từ Stage 1, KHÔNG mang theo Maven/source code
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Lợi ích Multi-stage Build:**
- Image cuối cùng **NHỎ HƠN NHIỀU** — không chứa Maven, source code, chỉ có JRE + file JAR đã build
- **Tự động hóa hoàn toàn** — chỉ cần `docker build`, không cần `mvn package` thủ công trước
- Bảo mật tốt hơn — giảm số lượng công cụ/thư viện không cần thiết trong Image production (giảm bề mặt tấn công)

### .dockerignore — tránh copy file không cần thiết vào Image (giống .gitignore)

```
# .dockerignore
target/
.git/
.idea/
*.md
.env
```

⚠️ **Bẫy hay gặp — quên .dockerignore:** Nếu không có, `COPY . .` sẽ copy cả thư mục `.git/` (có thể rất lớn), file `.env` (chứa secret!) vào Image — vừa làm Image phình to không cần thiết, vừa **rò rỉ secret** vào Image (ai kéo Image về đều xem được).

---

## 4. Docker Compose

**Vấn đề:** Ứng dụng thực tế thường cần **nhiều container phối hợp** (Spring Boot app + MySQL + Redis + RabbitMQ...) — chạy `docker run` thủ công cho từng container, phải tự cấu hình network giữa chúng, rất bất tiện.

**Docker Compose** cho phép định nghĩa **toàn bộ hệ thống nhiều container** trong 1 file YAML, chạy bằng 1 lệnh duy nhất.

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .                          # Build Image từ Dockerfile trong thư mục hiện tại
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/mydb
      # "mysql-db" - Docker Compose tự động tạo DNS nội bộ, các service gọi nhau qua TÊN SERVICE
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=secret
      - SPRING_REDIS_HOST=redis-cache
    depends_on:
      - mysql-db                       # Đảm bảo mysql-db khởi động TRƯỚC app (không đảm bảo mysql "sẵn sàng" 100%,
      - redis-cache                     # chỉ đảm bảo container đã START - xem bẫy ở mục 9)

  mysql-db:
    image: mysql:8.0                   # Dùng Image có sẵn từ Docker Hub, KHÔNG cần tự build
    environment:
      - MYSQL_ROOT_PASSWORD=secret
      - MYSQL_DATABASE=mydb
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql       # Persist dữ liệu - KHÔNG mất khi container bị xóa (xem bên dưới)

  redis-cache:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  mysql-data:                          # Khai báo Named Volume để lưu trữ dữ liệu MySQL lâu dài
```

```bash
docker compose up -d          # Khởi động TOÀN BỘ hệ thống (app + mysql + redis) chỉ với 1 lệnh
docker compose down            # Dừng và xóa TOÀN BỘ container (nhưng volume vẫn giữ nguyên nếu không thêm -v)
docker compose logs -f app     # Xem log của riêng service "app", real-time (-f = follow)
docker compose ps              # Xem trạng thái các service
```

### Volume — vì sao cần thiết cho Database

⚠️ **Bẫy nghiêm trọng nếu không hiểu Volume:** Container về bản chất là **ephemeral (tạm thời)** — khi Container bị xóa (`docker rm`), **TOÀN BỘ dữ liệu bên trong cũng mất theo** (kể cả dữ liệu MySQL!). **Volume** là cơ chế "gắn" 1 thư mục **bên ngoài Container** (nằm trên Host hoặc Docker quản lý riêng) vào Container — dữ liệu vẫn tồn tại dù Container bị xóa và tạo lại.

```
Không có Volume:
docker rm mysql-container -> TOÀN BỘ dữ liệu database MẤT VĨNH VIỄN!

Có Volume:
docker rm mysql-container -> Dữ liệu VẪN CÒN trong Volume
docker run ... (tạo container MỚI, gắn LẠI cùng Volume) -> Dữ liệu cũ vẫn còn nguyên
```

---

## 5. Linux Command Line cần thiết

Vì Docker Container và server production hầu hết chạy Linux, các lệnh sau là **tối thiểu cần biết**:

### Điều hướng & thao tác file

```bash
pwd                     # In thư mục hiện tại (Print Working Directory)
ls -la                  # Liệt kê file/thư mục (bao gồm file ẩn, chi tiết)
cd /path/to/dir         # Di chuyển thư mục
mkdir -p a/b/c          # Tạo thư mục (kể cả thư mục cha nếu chưa có, nhờ -p)
cp file1.txt file2.txt  # Copy file
mv old.txt new.txt      # Đổi tên/di chuyển file
rm -rf thu-muc/         # Xóa thư mục (⚠️ cẩn thận - -r đệ quy, -f không hỏi xác nhận)
cat application.yml     # In nội dung file ra màn hình
tail -f app.log         # Xem log REAL-TIME (thường dùng để theo dõi ứng dụng đang chạy)
grep "ERROR" app.log    # Tìm dòng chứa "ERROR" trong file log
find . -name "*.jar"    # Tìm file theo tên/pattern
```

### Quản lý tiến trình (Process)

```bash
ps aux                  # Liệt kê TẤT CẢ tiến trình đang chạy
ps aux | grep java      # Lọc tiến trình có chữ "java" (VD: tìm ứng dụng Spring Boot đang chạy)
kill -9 <PID>           # Buộc dừng tiến trình theo Process ID (-9 = SIGKILL, dừng ngay lập tức)
top / htop              # Xem tài nguyên hệ thống (CPU, RAM) theo thời gian thực
```

### Quyền truy cập file (Permissions)

```bash
chmod +x deploy.sh      # Cấp quyền THỰC THI (execute) cho file script
chmod 755 script.sh     # Set quyền chi tiết: owner=rwx(7), group=rx(5), other=rx(5)
chown user:group file   # Đổi chủ sở hữu file
```

### Biến môi trường (liên hệ trực tiếp application.yml — Module 13)

```bash
export DB_PASSWORD=secret123    # Set biến môi trường (chỉ tồn tại trong phiên terminal hiện tại)
echo $DB_PASSWORD                # In giá trị biến môi trường
env                              # Liệt kê TẤT CẢ biến môi trường hiện có

# Đây chính là cách production đọc secret AN TOÀN thay vì hardcode trong application.yml:
# spring.datasource.password: ${DB_PASSWORD}
```

### Network cơ bản

```bash
curl http://localhost:8080/actuator/health     # Gọi HTTP request từ command line (test API nhanh)
netstat -tulpn | grep 8080                      # Kiểm tra port 8080 đang được process nào sử dụng
ping google.com                                  # Kiểm tra kết nối mạng
```

### Docker command line thường dùng

```bash
docker ps -a                    # Liệt kê MỌI container (kể cả đã dừng, nhờ -a)
docker logs -f myapp-container  # Xem log container real-time
docker exec -it myapp-container /bin/bash  # "SSH vào" bên trong container đang chạy để debug
docker stop myapp-container     # Dừng container (vẫn còn tồn tại, có thể start lại)
docker rm myapp-container       # XÓA HẲN container
docker rmi myapp:1.0            # Xóa Image
docker system prune -a          # Dọn dẹp TOÀN BỘ Image/Container/Network không dùng (giải phóng dung lượng)
```

---

## 6. CI/CD — khái niệm & luồng hoạt động

### CI (Continuous Integration) — Tích hợp liên tục

**Ý tưởng:** Mỗi khi Developer push code lên repository, **tự động** chạy 1 chuỗi kiểm tra (build, chạy test, kiểm tra code style) để phát hiện lỗi **CÀNG SỚM CÀNG TỐT** — thay vì đợi tới lúc merge/deploy mới phát hiện.

```
Developer push code
     │
     ▼
CI Pipeline TỰ ĐỘNG chạy:
  1. Checkout code
  2. Build (mvn compile)
  3. Chạy Unit Test + Integration Test (Module 17)
  4. Kiểm tra Code Coverage
  5. Kiểm tra Code Style (Checkstyle/SonarQube)
     │
     ▼
✅ Pass -> Cho phép merge/tiếp tục CD
❌ Fail -> BÁO NGAY cho Developer, CHẶN merge (không cho code lỗi lọt vào nhánh chính)
```

### CD (Continuous Delivery/Deployment) — Chuyển giao/Triển khai liên tục

| | Continuous **Delivery** | Continuous **Deployment** |
|---|---|---|
| Sau khi CI pass | Tự động build ra bản deploy được (Artifact/Docker Image), sẵn sàng deploy | Tự động deploy THẲNG lên production, KHÔNG cần con người bấm nút |
| Bước cuối | Cần **con người** xác nhận (bấm nút "Deploy") | **Hoàn toàn tự động**, không cần con người can thiệp |
| Mức độ tự động hóa | Cao (nhưng chưa 100%) | Tối đa (100% tự động) |

```
CI (build + test) -> CD (Delivery: đóng gói sẵn sàng, CHỜ người bấm Deploy)
CI (build + test) -> CD (Deployment: tự động deploy THẲNG lên production)
```

> **Thực tế:** Nhiều công ty chọn **Continuous Delivery** (có bước xác nhận thủ công trước khi lên production) cho các hệ thống quan trọng, và chỉ dùng **Continuous Deployment** hoàn toàn tự động cho môi trường ít rủi ro hơn (dev/staging) hoặc khi độ tin cậy của test suite đã rất cao.

---

## 7. GitHub Actions

**GitHub Actions** là công cụ CI/CD tích hợp sẵn trong GitHub — định nghĩa pipeline bằng file YAML trong thư mục `.github/workflows/`.

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest   # Chạy trên máy ảo Ubuntu do GitHub cung cấp (miễn phí trong giới hạn)

    steps:
      - name: Checkout code
        uses: actions/checkout@v4   # Tải code về máy ảo CI

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven               # Cache dependency Maven -> build lần sau nhanh hơn

      - name: Build with Maven
        run: mvn clean compile

      - name: Run tests
        run: mvn test

      - name: Generate test coverage report
        run: mvn jacoco:report      # Liên hệ Module 17 - JaCoCo Coverage

      - name: Build Docker Image
        if: github.ref == 'refs/heads/main'  # CHỈ build Image khi push vào nhánh main
        run: docker build -t myapp:${{ github.sha }} .

      - name: Push to Docker Registry
        if: github.ref == 'refs/heads/main'
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
          docker push myapp:${{ github.sha }}
        # secrets.* -> GitHub Secrets, KHÔNG BAO GIỜ hardcode password trực tiếp trong file YAML
```

⚠️ **Bẫy bảo mật quan trọng:** TUYỆT ĐỐI không hardcode password/API key trực tiếp trong file YAML (dù file này có commit riêng tư) — luôn dùng **GitHub Secrets** (Settings → Secrets and variables → Actions), tương tự nguyên tắc "không hardcode secret trong `application.yml`" đã học ở Module 13.

### GitLab CI — tương tự nhưng cú pháp khác (`.gitlab-ci.yml`)

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy

build-job:
  stage: build
  image: eclipse-temurin:21-jdk
  script:
    - mvn clean compile

test-job:
  stage: test
  image: eclipse-temurin:21-jdk
  script:
    - mvn test

deploy-job:
  stage: deploy
  script:
    - echo "Deploy lên server..."
  only:
    - main   # CHỈ chạy job này khi ở nhánh main
```

> **Về bản chất, GitHub Actions và GitLab CI giải quyết CÙNG 1 vấn đề** (tự động hóa build/test/deploy) — chỉ khác nền tảng và cú pháp YAML. Hiểu rõ 1 trong 2 sẽ giúp học cái còn lại rất nhanh.

---

## 8. Giới thiệu Kubernetes

**Vấn đề khi chạy nhiều Container thủ công (Docker Compose) ở quy mô lớn:** Docker Compose phù hợp cho 1 máy chủ đơn lẻ — nhưng khi cần chạy **hàng trăm Container** trên **nhiều máy chủ**, tự động **scale lên/xuống** theo tải, tự động **khởi động lại** Container bị crash, tự **cân bằng tải**... Docker Compose không đủ khả năng — đây là lúc cần **Kubernetes (K8s)**.

### Kubernetes là gì?

**Kubernetes** là hệ thống **điều phối Container (Container Orchestration)** — tự động hóa việc triển khai, scale, và quản lý vòng đời của Container trên 1 **cụm (cluster)** nhiều máy chủ.

```
┌───────────────────────────────────────────────┐
│                Kubernetes Cluster                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  Node 1   │  │  Node 2   │  │  Node 3   │  <- Mỗi Node là 1 máy chủ (vật lý/ảo)
│  │ ┌──────┐ │  │ ┌──────┐ │  │ ┌──────┐ │        │
│  │ │Pod A  │ │  │ │Pod B  │ │  │ │Pod A  │ │  <- Pod chứa 1 (hoặc vài) Container
│  │ └──────┘ │  │ └──────┘ │  │ └──────┘ │        │
│  └──────────┘  └──────────┘  └──────────┘        │
└───────────────────────────────────────────────┘
```

### Các khái niệm cốt lõi

| Khái niệm | Ý nghĩa |
|---|---|
| **Pod** | Đơn vị triển khai nhỏ nhất trong K8s — chứa 1 (hoặc vài) container liên quan chặt chẽ |
| **Deployment** | Định nghĩa "muốn chạy BAO NHIÊU replica (bản sao) của Pod này" — K8s tự động đảm bảo LUÔN đủ số lượng |
| **Service** | Điểm truy cập ổn định (địa chỉ cố định) tới 1 nhóm Pod — liên hệ trực tiếp **Service Discovery** đã học ở Module 19 |
| **ConfigMap/Secret** | Lưu cấu hình/secret riêng biệt khỏi Image — liên hệ `application.yml`/biến môi trường đã học |
| **Ingress** | Định tuyến traffic từ bên ngoài vào cluster — liên hệ **API Gateway** đã học ở Module 19 |
| **HPA (Horizontal Pod Autoscaler)** | Tự động tăng/giảm số lượng Pod dựa trên tải (CPU/Memory/custom metrics) |

### Ví dụ file cấu hình Deployment cơ bản

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3                     # Muốn LUÔN có 3 Pod chạy đồng thời
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: myregistry/order-service:1.0
          ports:
            - containerPort: 8080
          livenessProbe:            # Liên hệ Module 13 - Actuator /actuator/health
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
          env:
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:        # Đọc từ Kubernetes Secret, KHÔNG hardcode
                  name: db-secret
                  key: password
---
apiVersion: v1
kind: Service
metadata:
  name: order-service-svc
spec:
  selector:
    app: order-service
  ports:
    - port: 80
      targetPort: 8080
```

**Cơ chế Self-healing (tự phục hồi) — 1 trong những lợi ích lớn nhất của K8s:**

```
Deployment yêu cầu 3 replica -> 1 Pod bất ngờ CRASH (còn lại 2 Pod)
     │
     ▼
Kubernetes TỰ ĐỘNG phát hiện (qua livenessProbe) -> tự động TẠO LẠI 1 Pod mới
     │
     ▼
Quay lại đủ 3 Pod như yêu cầu ban đầu - KHÔNG CẦN con người can thiệp
```

> **Mức độ ưu tiên học ở giai đoạn này:** Kubernetes là chủ đề **rất rộng và sâu**, thường được học chuyên sâu khi đã vững Docker + có kinh nghiệm vận hành thực tế. Ở giai đoạn hiện tại, **hiểu khái niệm cốt lõi** (Pod, Deployment, Service tương ứng với gì đã học ở Docker/Microservices) là đủ — không cần thành thạo vận hành Kubernetes ngay, việc này thường thuộc phạm vi công việc gần với DevOps Engineer hơn.

---

## 9. ⚠️ Các bẫy hay gặp

1. **Không dùng Multi-stage Build** — Image production chứa cả Maven, source code, công cụ build không cần thiết, kích thước phình to, tăng bề mặt tấn công bảo mật.

2. **Quên `.dockerignore`** — copy nhầm file `.git/`, `.env` (chứa secret!), `target/` cũ vào Image.

3. **Hardcode secret trong Dockerfile/docker-compose.yml/CI YAML** — thay vì dùng biến môi trường/Secret Manager (GitHub Secrets, Kubernetes Secret...).

4. **Không dùng Volume cho Database container** — mất toàn bộ dữ liệu khi container bị xóa/restart ngoài ý muốn.

5. **Hiểu sai `depends_on` trong Docker Compose** — `depends_on` chỉ đảm bảo container **ĐÃ START**, KHÔNG đảm bảo service bên trong (VD: MySQL) đã **sẵn sàng nhận kết nối** — ứng dụng Spring Boot có thể khởi động và cố kết nối DB TRƯỚC KHI MySQL thực sự sẵn sàng, gây lỗi kết nối ngẫu nhiên. Giải pháp: dùng `healthcheck` kết hợp `condition: service_healthy`, hoặc cấu hình retry kết nối DB ở tầng ứng dụng.

```yaml
mysql-db:
  image: mysql:8.0
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    interval: 5s
    timeout: 5s
    retries: 10

app:
  depends_on:
    mysql-db:
      condition: service_healthy   # Chờ MySQL THỰC SỰ sẵn sàng, không chỉ "đã start"
```

6. **Chạy container với quyền `root` không cần thiết** — tăng rủi ro bảo mật nếu container bị chiếm quyền kiểm soát (nên tạo user riêng trong Dockerfile với quyền hạn chế).

7. **Không giới hạn tài nguyên (CPU/Memory) cho Container** — 1 container "ngốn" hết tài nguyên có thể ảnh hưởng các container khác trên cùng máy chủ.

8. **CI Pipeline không chạy test trước khi deploy** — hoặc bỏ qua bước test để "deploy nhanh hơn" — mất đi toàn bộ ý nghĩa của CI (phát hiện lỗi sớm).

9. **Image tag dùng `latest`** trong production — không kiểm soát được chính xác phiên bản nào đang chạy, khó rollback khi có sự cố (nên dùng tag cụ thể như version number hoặc Git commit SHA).

10. **Không có `livenessProbe`/`readinessProbe`** khi deploy lên Kubernetes — K8s không biết Pod nào thực sự "khỏe mạnh" để định tuyến traffic vào, hoặc không tự động restart Pod bị treo (hang) dù process vẫn "chạy" nhưng không phản hồi được nữa.

---

## 10. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Docker Image vs Container | Image = "class" tĩnh; Container = "instance" đang chạy |
| Multi-stage Build | Tách Stage build (Maven+JDK) và Stage runtime (chỉ JRE+JAR) — Image nhẹ hơn nhiều |
| Volume | Lưu dữ liệu NGOÀI vòng đời Container — bắt buộc cho Database |
| Docker Compose | Định nghĩa nhiều container phối hợp trong 1 file YAML, chạy bằng 1 lệnh |
| `depends_on` | Chỉ đảm bảo container ĐÃ START, không đảm bảo service ĐÃ SẴN SÀNG — cần `healthcheck` |
| CI | Tự động build+test mỗi lần push — phát hiện lỗi sớm |
| CD Delivery vs Deployment | Delivery cần người xác nhận deploy; Deployment tự động 100% |
| GitHub Actions/GitLab CI | Định nghĩa pipeline bằng YAML — dùng Secrets, không hardcode credential |
| Kubernetes Pod | Đơn vị triển khai nhỏ nhất, chứa 1(vài) container |
| Kubernetes Deployment | Đảm bảo LUÔN đủ số lượng replica Pod mong muốn (self-healing) |
| Kubernetes Service | Điểm truy cập ổn định tới nhóm Pod — tương tự Service Discovery |

---

## 11. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Docker Container có Hệ điều hành (Kernel) riêng biệt hoàn toàn với Host, giống Virtual Machine.
2. Multi-stage Build giúp Image cuối cùng KHÔNG chứa Maven/công cụ build, chỉ chứa JRE và file JAR đã build.
3. Nếu không dùng Volume, dữ liệu trong Container MySQL sẽ mất khi Container đó bị xóa.
4. `depends_on` trong Docker Compose đảm bảo service phụ thuộc đã THỰC SỰ sẵn sàng nhận kết nối, không chỉ là container đã khởi động.
5. Continuous Deployment yêu cầu con người xác nhận thủ công trước khi code lên production.
6. GitHub Secrets nên được dùng để lưu password/API key thay vì hardcode trực tiếp trong file YAML của CI/CD.
7. Kubernetes Pod và Docker Container là 2 khái niệm hoàn toàn giống hệt nhau, không có khác biệt.
8. Dùng tag `latest` cho Docker Image trong production là thực hành tốt vì luôn lấy được phiên bản mới nhất.

### Phần B — Bài tập viết code (5 bài)

**Bài 1:** Viết 1 Dockerfile dùng Multi-stage Build đầy đủ cho ứng dụng Spring Boot (Maven), Stage 1 build bằng `maven:3.9-eclipse-temurin-21`, Stage 2 chạy bằng `eclipse-temurin:21-jre-alpine`.

**Bài 2:** Viết `docker-compose.yml` cho hệ thống gồm: Spring Boot app (build từ Dockerfile hiện tại), PostgreSQL (có Volume lưu dữ liệu + healthcheck), Redis. Đảm bảo `app` chỉ khởi động SAU KHI PostgreSQL thực sự sẵn sàng.

**Bài 3:** Viết file `.github/workflows/ci.yml` cho pipeline: checkout code → setup JDK 21 → chạy `mvn test` → CHỈ build và push Docker Image lên Docker Hub khi ở nhánh `main` (dùng GitHub Secrets cho credential).

**Bài 4:** Viết 5 lệnh Linux command line hữu ích để: (a) xem log real-time của file `app.log`, (b) tìm tiến trình Java đang chạy, (c) kiểm tra port 8080 đang bị chiếm bởi process nào, (d) cấp quyền thực thi cho file `deploy.sh`, (e) dọn dẹp toàn bộ Docker Image/Container không dùng.

**Bài 5:** Cho đoạn Dockerfile sau có 3 lỗi/thực hành xấu, hãy chỉ ra và sửa lại:
```dockerfile
FROM eclipse-temurin:21-jdk
COPY . .
RUN mvn clean package
ENV DB_PASSWORD=mySecretPassword123
CMD ["java", "-jar", "target/app.jar"]
```

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Container dùng CHUNG Kernel với Host OS, KHÔNG có Kernel riêng — đây chính là khác biệt cốt lõi so với Virtual Machine.
2. **Đúng.** Đây chính là mục đích thiết kế của Multi-stage Build — Stage cuối cùng CHỈ copy file JAR đã build từ Stage trước, không mang theo công cụ build.
3. **Đúng.** Container là ephemeral — dữ liệu bên trong mất khi Container bị xóa, trừ khi dùng Volume để lưu trữ NGOÀI vòng đời Container.
4. **Sai.** `depends_on` CHỈ đảm bảo container đã START (tiến trình bắt đầu chạy), KHÔNG đảm bảo service bên trong đã sẵn sàng nhận kết nối — cần kết hợp `healthcheck` + `condition: service_healthy`.
5. **Sai.** Đó là định nghĩa của Continuous DELIVERY — Continuous DEPLOYMENT thì hoàn toàn tự động, KHÔNG cần xác nhận thủ công.
6. **Đúng.** Đây là thực hành bảo mật cơ bản, tránh rò rỉ credential trong source code/file cấu hình.
7. **Sai.** Pod là khái niệm của Kubernetes (có thể chứa NHIỀU container liên quan), Container là khái niệm của Docker — Pod là lớp trừu tượng cao hơn, không đồng nhất hoàn toàn.
8. **Sai.** Ngược lại — dùng tag `latest` trong production là thực hành XẤU vì không kiểm soát được chính xác phiên bản đang chạy, khó rollback khi có sự cố. Nên dùng tag cụ thể (version/commit SHA).

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```dockerfile
# ===== STAGE 1: Build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/mydb
      - SPRING_DATASOURCE_USERNAME=appuser
      - SPRING_DATASOURCE_PASSWORD=secret
      - SPRING_REDIS_HOST=redis-cache
    depends_on:
      postgres-db:
        condition: service_healthy
      redis-cache:
        condition: service_started

  postgres-db:
    image: postgres:16
    environment:
      - POSTGRES_USER=appuser
      - POSTGRES_PASSWORD=secret
      - POSTGRES_DB=mydb
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d mydb"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis-cache:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres-data:
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: mvn test

  build-and-push:
    needs: test              # CHỈ chạy job này nếu job "test" đã PASS
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build Docker Image
        run: docker build -t myapp:${{ github.sha }} -t myapp:latest .

      - name: Login to Docker Hub
        run: echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin

      - name: Push Docker Image
        run: |
          docker push myapp:${{ github.sha }}
          docker push myapp:latest
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```bash
# (a) Xem log real-time
tail -f app.log

# (b) Tìm tiến trình Java đang chạy
ps aux | grep java

# (c) Kiểm tra port 8080 đang bị chiếm bởi process nào
netstat -tulpn | grep 8080
# hoặc: lsof -i :8080

# (d) Cấp quyền thực thi cho file deploy.sh
chmod +x deploy.sh

# (e) Dọn dẹp toàn bộ Docker Image/Container không dùng
docker system prune -a
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

**3 lỗi/thực hành xấu:**

1. **Không dùng Multi-stage Build** — `FROM eclipse-temurin:21-jdk` chứa cả JDK đầy đủ + Maven phải cài thêm để build, khiến Image cuối cùng (dùng để CHẠY app) nặng nề không cần thiết, mang theo cả source code và công cụ build.

2. **`COPY . .` không có `.dockerignore`** — có nguy cơ copy nhầm `.git/`, file `.env`, `target/` cũ vào Image, làm phình to Image và có thể rò rỉ thông tin nhạy cảm.

3. **Hardcode secret trực tiếp trong Dockerfile** (`ENV DB_PASSWORD=mySecretPassword123`) — bất kỳ ai có quyền truy cập Image (hoặc chỉ cần chạy `docker history`) đều có thể xem được password này — vi phạm nghiêm trọng nguyên tắc bảo mật đã học ở Module 13/16.

**Sửa lại:**

```dockerfile
# ===== STAGE 1: Build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
# KHÔNG hardcode secret - password sẽ được truyền vào lúc CHẠY container
# (qua docker run -e DB_PASSWORD=... hoặc docker-compose environment/secrets)
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```
# .dockerignore (file bổ sung, cần thiết để tránh lỗi #2)
target/
.git/
.env
*.md
```

</details>

---

*File tiếp theo trong lộ trình: **Module 21 — Observability** (Structured Logging, Distributed Tracing với Zipkin/Jaeger, Metrics với Micrometer & Prometheus, Grafana Dashboard, ELK Stack cho tập trung log).*
