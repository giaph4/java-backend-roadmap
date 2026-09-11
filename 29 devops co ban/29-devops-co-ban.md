# Module 20 — DevOps cơ bản cho Backend Developer

> **Mức ưu tiên: 🟡 Trung bình (nhưng gần như bắt buộc trong công việc thực tế)**
> **Vì sao quan trọng:** Bạn có thể viết code Java Backend hoàn hảo, nhưng nếu không biết đóng gói (Docker) và triển khai (CI/CD) nó, sản phẩm không bao giờ tới được tay người dùng. Đây là kỹ năng "cầu nối" giữa Developer và Operations — hầu hết công ty hiện nay yêu cầu Backend Developer hiểu ít nhất ở mức cơ bản Docker/CI-CD, dù không cần thành thạo như 1 DevOps Engineer chuyên trách. Không biết Docker cũng đồng nghĩa không hiểu được câu nói quen thuộc "chạy được trên máy tôi mà" (works on my machine) — vấn đề Docker sinh ra để giải quyết.

> **Phạm vi bài này:** Tập trung vào Docker (đóng gói) và CI/CD (tự động hóa build/test/deploy) ở mức **Backend Developer cần biết để làm việc hiệu quả** với DevOps Engineer, cùng khái niệm cốt lõi của Kubernetes để đọc hiểu file cấu hình. Không đi sâu Observability (structured logging, metrics, distributed tracing chi tiết — thuộc Module 21 tiếp theo) hay vận hành Kubernetes cấp production (Helm, Operator, Service Mesh).

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
9. [Deployment Strategy: Rolling Update, Blue-Green, Canary](#9-deployment-strategy)
10. [⚠️ Các bẫy hay gặp](#10-các-bẫy-hay-gặp)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

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

### Chạy container với user không phải root — thực hành bảo mật quan trọng

Mặc định, nếu Dockerfile không khai báo `USER`, tiến trình bên trong Container chạy với quyền **`root`** — nếu kẻ tấn công khai thác được lỗ hổng trong ứng dụng để "thoát" ra khỏi container (container escape, dù hiếm nhưng có thật), quyền `root` bên trong container có thể bị lợi dụng để leo thang đặc quyền trên Host. Thực hành chuẩn là tạo 1 user riêng, không có quyền quản trị:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo user/group riêng, KHÔNG dùng root để chạy ứng dụng
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar   # Đảm bảo user mới có quyền đọc file JAR

USER appuser   # Từ dòng này trở đi, MỌI lệnh (kể cả ENTRYPOINT) chạy với quyền user thường

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **Lưu ý:** Nhiều base image chính thức (bao gồm 1 số bản `eclipse-temurin`) đã có sẵn user không-root định nghĩa trước — kiểm tra tài liệu base image trước khi tự tạo user riêng để tránh trùng lặp không cần thiết.

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
      - redis-cache                     # chỉ đảm bảo container đã START - xem bẫy ở mục 10)

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

### Giới hạn tài nguyên Container (CPU/Memory)

**Vấn đề:** Mặc định, 1 Container có thể sử dụng **không giới hạn** CPU/RAM của Host — nếu ứng dụng có bug rò rỉ bộ nhớ (memory leak) hoặc vòng lặp vô hạn ngốn CPU, nó có thể "ngốn" hết tài nguyên của toàn bộ máy chủ, ảnh hưởng tới **các container khác** đang chạy cùng.

```yaml
services:
  app:
    build: .
    deploy:
      resources:
        limits:              # Giới hạn TỐI ĐA container được phép dùng
          cpus: '1.0'
          memory: 512M
        reservations:         # Đảm bảo TỐI THIỂU luôn có sẵn cho container (khi tài nguyên khan hiếm)
          cpus: '0.5'
          memory: 256M
```

```bash
# Tương đương khi chạy docker run trực tiếp (không qua Compose)
docker run -d --memory=512m --cpus=1.0 myapp:1.0
```

⚠️ **Lưu ý riêng cho JVM:** Trước Java 10, JVM **không nhận biết được** giới hạn bộ nhớ của Container — nó nhìn thấy RAM của TOÀN BỘ Host và có thể cấp phát Heap vượt quá giới hạn container, dẫn tới bị `OOMKilled` (Container bị hệ điều hành/Docker giết vì vượt hạn mức). Từ Java 10+ (và mặc định từ Java 11), JVM đã hỗ trợ **Container-aware** — tự động đọc đúng giới hạn CPU/Memory của container để tính `-Xmx` phù hợp; vẫn nên set rõ `-XX:MaxRAMPercentage` hoặc `-Xmx` tường minh trong `ENTRYPOINT` để kiểm soát chắc chắn thay vì phó mặc hoàn toàn cho auto-detect.

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

### Cache Docker Layer trong CI — tăng tốc build đáng kể

Mặc định, mỗi lần chạy CI, máy ảo GitHub Actions là **hoàn toàn mới** (không có Docker Layer Cache từ lần build trước) — mỗi lần `docker build` đều build lại từ đầu, kể cả các layer không đổi (VD: tải dependency Maven), rất lãng phí thời gian. `docker/build-push-action` hỗ trợ cache layer giữa các lần chạy CI:

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3

- name: Build and push with layer cache
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: myapp:${{ github.sha }}
    cache-from: type=gha    # Đọc cache từ lần chạy CI trước (GitHub Actions cache)
    cache-to: type=gha,mode=max  # Ghi lại cache cho lần chạy SAU sử dụng
```

> **Hiệu quả thực tế:** Với Dockerfile Multi-stage Build đã tách riêng bước `mvn dependency:go-offline` (mục 3), kết hợp cache layer ở CI, các lần build sau **chỉ cần tải lại dependency khi `pom.xml` thay đổi** — giảm thời gian CI từ vài phút xuống còn vài chục giây cho các lần build không đổi dependency.

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

### livenessProbe vs readinessProbe — 2 loại Probe dễ nhầm lẫn

Ví dụ trên chỉ mới khai báo `livenessProbe` — thực tế, K8s phân biệt rõ **2 loại probe** với mục đích khác nhau, thường phải khai báo **CẢ HAI**:

```yaml
livenessProbe:              # "Pod này CÒN SỐNG không?"
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  # Nếu FAIL nhiều lần liên tiếp -> K8s KILL Pod và TẠO LẠI Pod mới (self-healing)

readinessProbe:              # "Pod này ĐÃ SẴN SÀNG nhận traffic chưa?"
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  # Nếu FAIL -> K8s CHỈ tạm ngừng gửi traffic tới Pod này (không kill, không tạo lại),
  # tự động gửi traffic trở lại NGAY khi probe pass trở lại
```

| | `livenessProbe` | `readinessProbe` |
|---|---|---|
| Câu hỏi trả lời | "Process có còn hoạt động, hay đã bị treo (deadlock/hang)?" | "Đã sẵn sàng phục vụ request thật chưa?" |
| Khi FAIL | K8s **KILL** Pod, tạo Pod mới thay thế | K8s **tạm ngừng route traffic** vào Pod, KHÔNG kill |
| Ví dụ tình huống cần | Ứng dụng bị deadlock, process vẫn "chạy" nhưng không phản hồi được nữa | Ứng dụng vừa khởi động, đang warm-up cache/kết nối DB, CHƯA sẵn sàng xử lý request |

⚠️ **Bẫy hay gặp:** Chỉ khai báo `livenessProbe` mà thiếu `readinessProbe` — trong lúc ứng dụng Spring Boot đang khởi động (context loading, kết nối DB...), K8s có thể đã bắt đầu route traffic vào Pod dù ứng dụng **chưa sẵn sàng thực sự**, gây lỗi request trong vài giây đầu sau mỗi lần deploy/scale.

> **Mức độ ưu tiên học ở giai đoạn này:** Kubernetes là chủ đề **rất rộng và sâu**, thường được học chuyên sâu khi đã vững Docker + có kinh nghiệm vận hành thực tế. Ở giai đoạn hiện tại, **hiểu khái niệm cốt lõi** (Pod, Deployment, Service tương ứng với gì đã học ở Docker/Microservices) là đủ — không cần thành thạo vận hành Kubernetes ngay, việc này thường thuộc phạm vi công việc gần với DevOps Engineer hơn.

---

## 9. Deployment Strategy

Khi CD (mục 6) đã đóng gói xong phiên bản mới, câu hỏi tiếp theo là: **đưa phiên bản mới vào production như thế nào** để giảm thiểu rủi ro gián đoạn dịch vụ? Có 3 chiến lược phổ biến, mỗi chiến lược đánh đổi khác nhau giữa **tốc độ**, **rủi ro**, và **chi phí hạ tầng**.

### 9.1. Rolling Update — mặc định của Kubernetes Deployment

Thay thế Pod cũ bằng Pod mới **từng phần một**, không dừng toàn bộ dịch vụ cùng lúc:

```
Trạng thái ban đầu: [v1] [v1] [v1]  (3 Pod phiên bản cũ)

Bước 1: Tạo 1 Pod v2 mới, chờ readinessProbe pass -> route traffic vào
        [v1] [v1] [v1] [v2]
Bước 2: Sau khi v2 ổn định, xóa 1 Pod v1
        [v1] [v1] [v2]
Bước 3: Lặp lại tuần tự cho tới khi thay thế hết
        [v1] [v2] [v2] -> [v2] [v2] [v2]

-> Tại MỌI thời điểm, luôn có ít nhất vài Pod đang phục vụ traffic - KHÔNG downtime
```

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1   # Tối đa 1 Pod được phép "thiếu" so với replicas mong muốn trong lúc update
      maxSurge: 1         # Tối đa được tạo THÊM 1 Pod vượt số replicas mong muốn trong lúc update
```

**Ưu điểm:** Không cần hạ tầng thêm (mặc định có sẵn trong K8s Deployment), không downtime.
**Nhược điểm:** Trong lúc rolling, **cả v1 và v2 cùng chạy song song** — nếu v2 có breaking change không tương thích ngược (VD: đổi cấu trúc API response), có thể gây lỗi cho client đang được route tới cả 2 phiên bản khác nhau.

### 9.2. Blue-Green Deployment — chuyển đổi tức thì giữa 2 môi trường song song

Chạy **2 môi trường đầy đủ, độc lập** (Blue = phiên bản đang chạy, Green = phiên bản mới) — sau khi Green được kiểm tra kỹ, **chuyển toàn bộ traffic sang Green ngay lập tức** (thường chỉ cần đổi cấu hình Load Balancer/Ingress):

```
Trước khi chuyển:
Traffic 100% ──► [Blue: v1] (đang phục vụ)
                 [Green: v2] (đã deploy, đang test, CHƯA nhận traffic thật)

Sau khi xác nhận Green ổn định -> đổi Load Balancer trỏ sang Green:
Traffic 100% ──► [Green: v2] (đang phục vụ)
                 [Blue: v1] (giữ lại 1 thời gian để ROLLBACK tức thì nếu cần)
```

**Ưu điểm:** Chuyển đổi gần như **tức thì** (chỉ đổi routing, không có giai đoạn "lẫn lộn" 2 phiên bản như Rolling Update), **rollback cực nhanh** (đổi routing về Blue nếu Green có vấn đề).
**Nhược điểm:** Tốn **gấp đôi tài nguyên hạ tầng** trong lúc chuyển đổi (phải chạy đủ cả 2 môi trường Blue và Green cùng lúc).

### 9.3. Canary Deployment — thử nghiệm với 1 phần nhỏ traffic thật trước

Route **1 tỷ lệ NHỎ** traffic thật (VD: 5%) sang phiên bản mới trước, theo dõi metrics/lỗi, rồi **tăng dần** tỷ lệ nếu ổn định:

```
Giai đoạn 1: 95% traffic -> v1,  5% traffic -> v2 (canary)
             Theo dõi error rate/latency của v2 trong khoảng thời gian ngắn

Giai đoạn 2 (nếu v2 ổn định): 70% traffic -> v1,  30% traffic -> v2
Giai đoạn 3: 0% traffic -> v1,  100% traffic -> v2 (hoàn tất chuyển đổi)

Nếu v2 phát hiện lỗi ở BẤT KỲ giai đoạn nào -> route traffic NGAY về 100% v1
-> chỉ ảnh hưởng phần nhỏ user đã "trúng" canary, GIẢM THIỂU tối đa mức độ ảnh hưởng
```

**Ưu điểm:** Rủi ro thấp nhất trong 3 chiến lược — nếu phiên bản mới có bug, chỉ 1 phần nhỏ user bị ảnh hưởng, phát hiện sớm trước khi rollout toàn bộ.
**Nhược điểm:** Phức tạp nhất để triển khai — cần công cụ hỗ trợ định tuyến theo tỷ lệ phần trăm (Ingress nâng cao, Service Mesh như Istio) và hệ thống giám sát (Observability — Module 21) đủ tốt để tự động phát hiện canary có vấn đề hay không.

### So sánh tổng quan

| | Rolling Update | Blue-Green | Canary |
|---|---|---|---|
| Downtime | Không | Không (nếu chuyển đúng cách) | Không |
| Tài nguyên cần thêm | Không (hoặc rất ít, `maxSurge`) | Gấp đôi (tạm thời) | Ít (chỉ % nhỏ Pod mới) |
| Tốc độ rollback | Chậm hơn (phải rolling ngược lại) | **Tức thì** (đổi routing) | Nhanh (route traffic về lại 100% cũ) |
| Rủi ro khi có bug | Ảnh hưởng dần khi rolling | Ảnh hưởng 100% user NGAY khi chuyển (nếu không test kỹ trước) | Ảnh hưởng ít nhất — chỉ % nhỏ user |
| Độ phức tạp triển khai | Thấp (mặc định K8s) | Trung bình (cần 2 môi trường song song) | Cao (cần công cụ định tuyến % + giám sát tốt) |
| Phù hợp | Đa số ứng dụng, thay đổi tương thích ngược | Cần rollback cực nhanh, chấp nhận tốn tài nguyên | Hệ thống lớn, thay đổi rủi ro cao, cần kiểm chứng với traffic thật trước |

---

## 10. ⚠️ Các bẫy hay gặp

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

6. **Chạy container với quyền `root` không cần thiết** — tăng rủi ro bảo mật nếu container bị chiếm quyền kiểm soát (nên tạo user riêng trong Dockerfile với quyền hạn chế, xem mục 3).

7. **Không giới hạn tài nguyên (CPU/Memory) cho Container** — 1 container "ngốn" hết tài nguyên có thể ảnh hưởng các container khác trên cùng máy chủ (xem mục 4).

8. **CI Pipeline không chạy test trước khi deploy** — hoặc bỏ qua bước test để "deploy nhanh hơn" — mất đi toàn bộ ý nghĩa của CI (phát hiện lỗi sớm).

9. **Image tag dùng `latest`** trong production — không kiểm soát được chính xác phiên bản nào đang chạy, khó rollback khi có sự cố (nên dùng tag cụ thể như version number hoặc Git commit SHA).

10. **Không có `livenessProbe`/`readinessProbe`** khi deploy lên Kubernetes — K8s không biết Pod nào thực sự "khỏe mạnh" để định tuyến traffic vào, hoặc không tự động restart Pod bị treo (hang) dù process vẫn "chạy" nhưng không phản hồi được nữa.

11. **Chỉ khai báo `livenessProbe` mà thiếu `readinessProbe`** — Pod nhận traffic ngay cả khi ứng dụng chưa khởi động xong (chưa kết nối DB/cache), gây lỗi request trong vài giây đầu sau mỗi lần deploy.

12. **Chọn sai Deployment Strategy cho mức độ rủi ro của thay đổi** — dùng Rolling Update mặc định cho 1 thay đổi lớn/rủi ro cao (VD: đổi schema DB không tương thích ngược) thay vì Canary/Blue-Green, khiến toàn bộ user bị ảnh hưởng cùng lúc nếu có bug.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Docker Image vs Container | Image = "class" tĩnh; Container = "instance" đang chạy |
| Multi-stage Build | Tách Stage build (Maven+JDK) và Stage runtime (chỉ JRE+JAR) — Image nhẹ hơn nhiều |
| Non-root user | Tạo user riêng trong Dockerfile (`USER`) — giảm rủi ro nếu container bị chiếm quyền |
| Volume | Lưu dữ liệu NGOÀI vòng đời Container — bắt buộc cho Database |
| Resource Limits | Giới hạn CPU/Memory container — tránh 1 container ngốn hết tài nguyên Host |
| Docker Compose | Định nghĩa nhiều container phối hợp trong 1 file YAML, chạy bằng 1 lệnh |
| `depends_on` | Chỉ đảm bảo container ĐÃ START, không đảm bảo service ĐÃ SẴN SÀNG — cần `healthcheck` |
| CI | Tự động build+test mỗi lần push — phát hiện lỗi sớm |
| CD Delivery vs Deployment | Delivery cần người xác nhận deploy; Deployment tự động 100% |
| GitHub Actions/GitLab CI | Định nghĩa pipeline bằng YAML — dùng Secrets, không hardcode credential; cache layer (`type=gha`) tăng tốc build |
| Kubernetes Pod | Đơn vị triển khai nhỏ nhất, chứa 1(vài) container |
| Kubernetes Deployment | Đảm bảo LUÔN đủ số lượng replica Pod mong muốn (self-healing) |
| Kubernetes Service | Điểm truy cập ổn định tới nhóm Pod — tương tự Service Discovery |
| liveness vs readiness | liveness = còn sống hay không (fail → kill Pod); readiness = sẵn sàng nhận traffic hay không (fail → tạm ngừng route) |
| Deployment Strategy | Rolling Update (mặc định, đơn giản) / Blue-Green (rollback tức thì, tốn tài nguyên) / Canary (rủi ro thấp nhất, phức tạp nhất) |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Docker Container có Hệ điều hành (Kernel) riêng biệt hoàn toàn với Host, giống Virtual Machine.
2. Multi-stage Build giúp Image cuối cùng KHÔNG chứa Maven/công cụ build, chỉ chứa JRE và file JAR đã build.
3. Nếu không dùng Volume, dữ liệu trong Container MySQL sẽ mất khi Container đó bị xóa.
4. `depends_on` trong Docker Compose đảm bảo service phụ thuộc đã THỰC SỰ sẵn sàng nhận kết nối, không chỉ là container đã khởi động.
5. Continuous Deployment yêu cầu con người xác nhận thủ công trước khi code lên production.
6. GitHub Secrets nên được dùng để lưu password/API key thay vì hardcode trực tiếp trong file YAML của CI/CD.
7. Kubernetes Pod và Docker Container là 2 khái niệm hoàn toàn giống hệt nhau, không có khác biệt.
8. Dùng tag `latest` cho Docker Image trong production là thực hành tốt vì luôn lấy được phiên bản mới nhất.
9. Khi `readinessProbe` fail, Kubernetes sẽ KILL Pod đó và tạo Pod mới thay thế, giống hệt cơ chế của `livenessProbe`.
10. Trong Canary Deployment, nếu phiên bản mới có bug, chỉ 1 phần nhỏ user (những người được route tới canary) bị ảnh hưởng, thay vì toàn bộ user.

### Phần B — Bài tập viết code (6 bài)

**Bài 1:** Viết 1 Dockerfile dùng Multi-stage Build đầy đủ cho ứng dụng Spring Boot (Maven), Stage 1 build bằng `maven:3.9-eclipse-temurin-21`, Stage 2 chạy bằng `eclipse-temurin:21-jre-alpine`, kèm cấu hình chạy bằng user không phải root.

**Bài 2:** Viết `docker-compose.yml` cho hệ thống gồm: Spring Boot app (build từ Dockerfile hiện tại, có giới hạn tài nguyên 1 CPU/512MB), PostgreSQL (có Volume lưu dữ liệu + healthcheck), Redis. Đảm bảo `app` chỉ khởi động SAU KHI PostgreSQL thực sự sẵn sàng.

**Bài 3:** Viết file `.github/workflows/ci.yml` cho pipeline: checkout code → setup JDK 21 → chạy `mvn test` → CHỈ build và push Docker Image lên Docker Hub khi ở nhánh `main` (dùng GitHub Secrets cho credential), có áp dụng cache Docker layer.

**Bài 4:** Viết 5 lệnh Linux command line hữu ích để: (a) xem log real-time của file `app.log`, (b) tìm tiến trình Java đang chạy, (c) kiểm tra port 8080 đang bị chiếm bởi process nào, (d) cấp quyền thực thi cho file `deploy.sh`, (e) dọn dẹp toàn bộ Docker Image/Container không dùng.

**Bài 5:** Cho đoạn Dockerfile sau có 3 lỗi/thực hành xấu, hãy chỉ ra và sửa lại:
```dockerfile
FROM eclipse-temurin:21-jdk
COPY . .
RUN mvn clean package
ENV DB_PASSWORD=mySecretPassword123
CMD ["java", "-jar", "target/app.jar"]
```

**Bài 6:** Viết file `deployment.yaml` cho Kubernetes với đầy đủ CẢ `livenessProbe` LẪN `readinessProbe` cho 1 ứng dụng Spring Boot (dùng endpoint Actuator `/actuator/health/liveness` và `/actuator/health/readiness`), kèm cấu hình `RollingUpdate` với `maxUnavailable: 0` và `maxSurge: 1` (đảm bảo không giảm số Pod đang phục vụ trong lúc update).

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
9. **Sai.** Khi `readinessProbe` fail, K8s chỉ TẠM NGỪNG route traffic vào Pod (không kill, không tạo lại) — hành vi KILL Pod là của `livenessProbe`.
10. **Đúng.** Đây chính là lợi ích cốt lõi của Canary Deployment — giới hạn mức độ ảnh hưởng khi phiên bản mới có bug.

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

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

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
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M

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

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        run: echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin

      - name: Build and push with layer cache
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: myapp:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
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

<details>
<summary><b>Đáp án Bài 6</b></summary>

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0   # KHÔNG cho phép giảm số Pod đang phục vụ trong lúc update
      maxSurge: 1         # Cho phép tạo thêm 1 Pod mới trước khi xóa Pod cũ
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
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          env:
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
```

**Giải thích `maxUnavailable: 0` + `maxSurge: 1`:** Trong lúc rolling update, K8s LUÔN tạo thêm Pod mới TRƯỚC (surge) rồi mới xóa Pod cũ, thay vì xóa Pod cũ trước — đảm bảo tại MỌI thời điểm số lượng Pod sẵn sàng phục vụ traffic KHÔNG BAO GIỜ giảm xuống dưới `replicas` mong muốn, đánh đổi bằng việc tạm thời có `replicas + maxSurge` Pod chạy song song trong lúc chuyển đổi.

</details>

---

*File tiếp theo trong lộ trình: **Module 21 — Observability** (Structured Logging, Distributed Tracing với Zipkin/Jaeger, Metrics với Micrometer & Prometheus, Grafana Dashboard, ELK Stack cho tập trung log).*
