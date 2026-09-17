# Lời giải đầy đủ — Module 21: DevOps cơ bản

> Nguồn đề: `29 devops co ban/29-devops-co-ban.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Dockerfile Multi-stage Build cho Spring Boot

### Đề
Stage 1 build bằng `maven:3.9-eclipse-temurin-21`, Stage 2 chạy bằng `eclipse-temurin:21-jre-alpine`, user không phải root.

### Lời giải

```dockerfile
# ===== STAGE 1: BUILD ===== #
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy RIÊNG pom.xml TRƯỚC - tận dụng Docker layer cache: nếu chỉ code Java đổi (pom.xml KHÔNG đổi),
# bước "mvn dependency:go-offline" bên dưới sẽ được CACHE, không cần tải lại dependency mỗi lần build
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== STAGE 2: RUNTIME ===== #
FROM eclipse-temurin:21-jre-alpine

# Tạo user/group RIÊNG, KHÔNG dùng root - giảm thiểu rủi ro bảo mật nếu container bị chiếm quyền
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# CHỈ copy file JAR đã build từ Stage 1 - image cuối cùng KHÔNG chứa Maven, source code, hay
# bất kỳ công cụ build nào - giảm kích thước image ĐÁNG KỂ và giảm bề mặt tấn công (attack surface)
COPY --from=build /app/target/*.jar app.jar

# Đổi quyền sở hữu file cho user vừa tạo
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Giải thích

- **Multi-stage build tách biệt HOÀN TOÀN "môi trường BUILD" khỏi "môi trường CHẠY":** Stage 1 (`maven:3.9-eclipse-temurin-21`, nặng — chứa cả JDK đầy đủ, Maven, cache dependency) chỉ dùng để BIÊN DỊCH; Stage 2 (`eclipse-temurin:21-jre-alpine`, NHẸ HƠN NHIỀU — chỉ chứa JRE tối thiểu, không có compiler) là image CUỐI CÙNG thực sự deploy — image cuối giảm từ có thể **~600-800MB** (nếu dùng JDK + Maven) xuống chỉ còn **~150-200MB**, tăng tốc độ pull/deploy, giảm chi phí lưu trữ registry.
- **`COPY --from=build`** là cú pháp CỐT LÕI của multi-stage build — chỉ lấy ĐÚNG file cần thiết (JAR đã build xong) từ Stage trước, KHÔNG mang theo toàn bộ "rác build" (source code, file `.class` trung gian, cache Maven `~/.m2`) sang image cuối.
- **User không phải root (`appuser`) là thực hành bảo mật QUAN TRỌNG:** nếu container chạy bằng `root` (mặc định của nhiều base image nếu không cấu hình) và kẻ tấn công khai thác được 1 lỗ hổng trong ứng dụng (VD RCE — Remote Code Execution), họ sẽ có **TOÀN QUYỀN root TRONG CONTAINER** — dù container có "sandbox" nhất định với host, chạy non-root vẫn là lớp phòng thủ bổ sung quan trọng (defense in depth), giảm thiểu khả năng "container escape" hoặc leo thang đặc quyền.
- **Tách `COPY pom.xml` riêng trước `COPY src`:** tận dụng cơ chế **Docker Layer Caching** — Docker chỉ rebuild lại 1 layer (và MỌI layer SAU nó) khi nội dung layer đó THỰC SỰ thay đổi — vì `pom.xml` (danh sách dependency) thay đổi ÍT HƠN NHIỀU so với code Java, tách riêng giúp layer `mvn dependency:go-offline` (tải dependency — tốn thời gian NHẤT) được CACHE LẠI qua nhiều lần build liên tiếp khi chỉ code thay đổi.

---

## Bài 2 — `docker-compose.yml` cho app + PostgreSQL + Redis

### Đề
App (build từ Dockerfile, giới hạn 1 CPU/512MB), PostgreSQL (Volume + healthcheck), Redis. App chỉ khởi động SAU KHI PostgreSQL sẵn sàng.

### Lời giải

```yaml
version: "3.9"

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/librarydb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}   # đọc từ file .env - KHÔNG hardcode
      SPRING_REDIS_HOST: redis
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: 512M
    depends_on:
      postgres:
        condition: service_healthy   # CHỜ ĐẾN KHI healthcheck của postgres báo "healthy", KHÔNG CHỈ "đã start"
      redis:
        condition: service_started

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: librarydb
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data   # dữ liệu SỐNG SÓT qua việc xóa/tạo lại container
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

### Giải thích

- **`depends_on` với `condition: service_healthy` (KHÔNG PHẢI chỉ `depends_on: [postgres]` đơn thuần):** `depends_on` MẶC ĐỊNH chỉ đảm bảo THỨ TỰ KHỞI ĐỘNG container (postgres container START TRƯỚC), **KHÔNG ĐẢM BẢO** PostgreSQL BÊN TRONG đã THỰC SỰ sẵn sàng nhận kết nối (PostgreSQL cần thời gian khởi tạo database, có thể vài giây SAU KHI container đã "start") — nếu `app` khởi động NGAY khi container `postgres` vừa start (nhưng DB CHƯA sẵn sàng), `app` sẽ gặp lỗi kết nối DB ngay lúc khởi động — `condition: service_healthy` buộc Docker Compose CHỜ tới khi `healthcheck` của `postgres` báo trạng thái "healthy" mới thực sự khởi động `app`.
- **`healthcheck` với `pg_isready`**: lệnh CHUẨN CỦA CHÍNH PostgreSQL để kiểm tra server đã sẵn sàng nhận kết nối chưa — Docker định kỳ (`interval: 5s`) chạy lệnh này BÊN TRONG container `postgres`, coi là "healthy" khi lệnh trả về thành công.
- **`volumes: postgres_data:/var/lib/postgresql/data`**: PostgreSQL lưu TOÀN BỘ dữ liệu vật lý tại đường dẫn này BÊN TRONG container — nếu KHÔNG mount volume, dữ liệu sẽ **MẤT HOÀN TOÀN** mỗi khi container bị xóa/tạo lại (`docker-compose down` rồi `up` lại) — named volume (`postgres_data`) được Docker quản lý RIÊNG, TỒN TẠI ĐỘC LẬP với vòng đời của container.
- **`deploy.resources.limits`**: giới hạn app CHỈ ĐƯỢC dùng tối đa 1 CPU/512MB — ngăn 1 container "tham lam" (do bug leak memory, hoặc traffic đột biến) chiếm dụng TOÀN BỘ tài nguyên host, ảnh hưởng tới các container khác đang chạy cùng máy — thực hành quan trọng khi vận hành nhiều service trên cùng 1 host.

---

## Bài 3 — CI Pipeline `.github/workflows/ci.yml`

### Đề
checkout → setup JDK 21 → `mvn test` → build/push Docker Image (chỉ ở `main`, dùng GitHub Secrets) → cache Docker layer.

### Lời giải

```yaml
name: CI Pipeline

on:
  push:
    branches: ["main", "develop"]
  pull_request:
    branches: ["main"]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven   # tự động cache thư mục ~/.m2 giữa các lần chạy - tăng tốc build

      - name: Run tests
        run: mvn test -B

      - name: Set up Docker Buildx
        if: github.ref == 'refs/heads/main'
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        if: github.ref == 'refs/heads/main'
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build and push Docker image
        if: github.ref == 'refs/heads/main'   # CHỈ build+push khi CHÍNH XÁC là nhánh main
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/library-app:latest
          cache-from: type=gha        # đọc cache layer Docker từ GitHub Actions cache
          cache-to: type=gha,mode=max # ghi LẠI cache layer sau khi build, cho lần chạy SAU tái sử dụng
```

### Giải thích

- **`if: github.ref == 'refs/heads/main'`** trên các step build/push Docker: đảm bảo bước TỐN THỜI GIAN NHẤT (build + push image) CHỈ chạy khi commit/merge THỰC SỰ vào nhánh `main` — Pull Request hay push vào `develop` chỉ chạy `mvn test` (kiểm tra code KHÔNG lỗi) mà KHÔNG lãng phí thời gian/tài nguyên build-push image mỗi lần — tuân thủ nguyên tắc "chỉ publish artifact chính thức từ nhánh chính thức".
- **`secrets.DOCKERHUB_USERNAME`/`secrets.DOCKERHUB_TOKEN`**: đọc từ **GitHub Secrets** (cấu hình riêng trong Settings của repository, KHÔNG hiển thị trong log, KHÔNG commit vào code) — đây chính là ứng dụng thực tế của nguyên tắc "KHÔNG hardcode credential" đã nhấn mạnh xuyên suốt (Module 09 — `.env`; Module 14 — Spring Boot biến môi trường) — CI/CD cũng phải tuân thủ NGHIÊM NGẶT nguyên tắc này.
- **`cache: maven` (trong `setup-java`) + `cache-from`/`cache-to: type=gha` (Docker Buildx)**: 2 TẦNG cache RIÊNG BIỆT — cache Maven dependency (giảm thời gian `mvn test` không cần tải lại thư viện mỗi lần chạy CI) VÀ cache Docker layer (giảm thời gian build image, tận dụng layer đã build TRƯỚC ĐÓ nếu code không đổi nhiều — tương tự cơ chế Layer Caching đã giải thích ở Bài 1, nhưng áp dụng NGAY TRONG môi trường CI).
- **Thứ tự bước hợp lý:** `mvn test` chạy TRƯỚC bất kỳ bước Docker nào — nếu test THẤT BẠI, pipeline **DỪNG NGAY** (GitHub Actions mặc định dừng job khi 1 step lỗi), KHÔNG lãng phí thời gian build/push 1 image chứa code CÓ LỖI.

---

## Bài 4 — 5 lệnh Linux hữu ích

### Đề
(a) log real-time `app.log`, (b) tìm tiến trình Java, (c) kiểm tra port 8080, (d) cấp quyền thực thi `deploy.sh`, (e) dọn dẹp Docker.

### Lời giải

```bash
# (a) Xem log REAL-TIME (theo dõi liên tục các dòng MỚI được ghi thêm vào file)
tail -f app.log

# (b) Tìm tiến trình Java đang chạy - hiển thị PID, thời gian chạy, câu lệnh đầy đủ
ps aux | grep java

# (c) Kiểm tra port 8080 đang bị chiếm bởi process nào
lsof -i :8080
# (hoặc trên hệ thống không có lsof: netstat -tulpn | grep 8080  hoặc  ss -tulpn | grep 8080)

# (d) Cấp quyền THỰC THI cho file deploy.sh
chmod +x deploy.sh

# (e) Dọn dẹp TOÀN BỘ Docker Image/Container KHÔNG DÙNG (dừng, không tham chiếu bởi container nào)
docker system prune -a
```

### Giải thích

- **`tail -f`**: flag `-f` ("follow") giữ lệnh CHẠY LIÊN TỤC, in ra NGAY LẬP TỨC mỗi dòng MỚI được ghi thêm vào file — khác với `tail app.log` (không `-f`) chỉ in ra 10 dòng CUỐI rồi DỪNG — cực kỳ hữu ích khi debug ứng dụng đang chạy thời gian thực (theo dõi log lúc đang test 1 tính năng).
- **`ps aux | grep java`**: `ps aux` liệt kê TOÀN BỘ tiến trình đang chạy trên hệ thống (mọi user — `a`, định dạng đầy đủ — `u`, kể cả tiến trình không gắn với terminal — `x`); `| grep java` LỌC RA chỉ những dòng có chứa "java" — kết hợp 2 lệnh qua PIPE (`|`) là kỹ thuật CƠ BẢN, PHỔ BIẾN NHẤT khi thao tác dòng lệnh Linux.
- **`lsof -i :8080`** ("list open files", với `-i` lọc theo network socket): trả về CHÍNH XÁC process nào (PID, tên lệnh) đang LẮNG NGHE (LISTEN) hoặc kết nối tới port 8080 — cực kỳ hữu ích khi gặp lỗi "port already in use" lúc khởi động ứng dụng, cần biết TIẾN TRÌNH NÀO đang chiếm port để `kill` nó hoặc đổi port.
- **`chmod +x`**: cấp quyền THỰC THI (execute) — trên Linux, 1 file (kể cả script `.sh`) mặc định KHÔNG có quyền chạy trực tiếp (`./deploy.sh`) trừ khi được cấp quyền `x` — đây là bước BẮT BUỘC thường bị quên khi mới copy 1 script từ nơi khác về, gây lỗi "Permission denied".
- **`docker system prune -a`**: dọn dẹp TOÀN DIỆN — xóa TẤT CẢ container đã DỪNG, network không dùng, image "dangling" (không tag) VÀ với `-a`, cả image KHÔNG được bất kỳ container nào đang chạy tham chiếu tới (kể cả có tag) — giải phóng ĐÁNG KỂ dung lượng đĩa sau thời gian dài build/test nhiều image Docker — cần dùng CẨN THẬN (không nên chạy vô tội vạ trên máy có image quan trọng CHƯA chạy container nào nhưng vẫn cần giữ lại).

---

## Bài 5 — Sửa 3 lỗi trong Dockerfile

### Đề
```dockerfile
FROM eclipse-temurin:21-jdk
COPY . .
RUN mvn clean package
ENV DB_PASSWORD=mySecretPassword123
CMD ["java", "-jar", "target/app.jar"]
```
Chỉ ra và sửa 3 lỗi.

### Phân tích 3 lỗi

1. **`FROM eclipse-temurin:21-jdk`** dùng **JDK ĐẦY ĐỦ** cho image CHẠY THẬT (runtime) — chứa cả compiler, debug tools, và các thành phần chỉ cần lúc PHÁT TRIỂN/BIÊN DỊCH, không cần lúc CHẠY — image nặng hơn ĐÁNG KỂ so với dùng JRE, và không tận dụng Multi-stage Build (đã học ở Bài 1) để tách biệt build/runtime.
2. **`ENV DB_PASSWORD=mySecretPassword123`** — HARDCODE mật khẩu THẬT TRỰC TIẾP vào Dockerfile — đây là lỗi bảo mật NGHIÊM TRỌNG: giá trị này sẽ **LƯU VĨNH VIỄN trong LỊCH SỬ LAYER của Docker Image** (ai có quyền `docker history`/`docker inspect` image đều XEM ĐƯỢC, kể cả sau khi đã "sửa lại" Dockerfile ở version sau — layer CŨ với password vẫn còn nếu image cũ chưa bị xóa) — và nếu image được PUSH lên registry công khai (Docker Hub), password bị LỘ HOÀN TOÀN ra công chúng.
3. **`COPY . .` (copy TOÀN BỘ thư mục, không loại trừ gì)** — thiếu file `.dockerignore` — copy CẢ những thứ KHÔNG CẦN THIẾT và có thể NGUY HIỂM vào image: thư mục `.git/` (toàn bộ lịch sử commit, có thể chứa secret cũ), `target/` (build cũ, không cần thiết vì sẽ build lại), `.env` (nếu tồn tại local, chứa credential thật) — làm PHÌNH TO image không cần thiết và tăng rủi ro rò rỉ dữ liệu nhạy cảm.

### Lời giải — sửa lại

```dockerfile
# ===== STAGE 1: BUILD ===== #
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== STAGE 2: RUNTIME (dùng JRE, KHÔNG dùng JDK) ===== #
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# KHÔNG hardcode password - đọc từ biến môi trường TRUYỀN VÀO LÚC CHẠY container
# (docker run -e DB_PASSWORD=... hoặc qua docker-compose environment/.env)
# Dockerfile KHÔNG khai báo ENV DB_PASSWORD gì cả - ứng dụng Spring Boot tự đọc ${DB_PASSWORD} lúc runtime

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

```
# .dockerignore (file MỚI, đặt cùng cấp Dockerfile)
target/
.git/
.env
*.md
.idea/
.vscode/
```

### Giải thích

- **Sửa lỗi 1 (JDK → Multi-stage + JRE):** áp dụng lại đúng kỹ thuật đã học ở Bài 1 — tách biệt build/runtime, giảm kích thước và bề mặt tấn công của image cuối.
- **Sửa lỗi 2 (hardcode password):** KHÔNG khai báo `ENV DB_PASSWORD=...` GÌ CẢ trong Dockerfile — giá trị THẬT được truyền vào **LÚC CHẠY** container (`docker run -e DB_PASSWORD=xxx ...`, hoặc qua `docker-compose.yml` với biến môi trường từ file `.env` — đã làm ở Bài 2) — Dockerfile (và image build ra từ nó) **KHÔNG BAO GIỜ CHỨA** giá trị nhạy cảm nào, an toàn dù image có bị lộ/public.
- **Sửa lỗi 3 (`.dockerignore`):** hoạt động TƯƠNG TỰ `.gitignore` (đã làm ở Module 09) nhưng áp dụng cho **Docker build context** — loại trừ các file/thư mục KHÔNG CẦN THIẾT khỏi quá trình `COPY`, giảm kích thước build context gửi tới Docker daemon (build NHANH HƠN) và tránh vô tình đưa dữ liệu nhạy cảm (`.git/`, `.env`) vào image.
- **Nguyên tắc tổng quát rút ra:** mọi thực hành xấu trong bài này đều VI PHẠM các nguyên tắc ĐÃ được thiết lập xuyên suốt khóa học (Multi-stage build — Bài 1 của module này; không hardcode secret — Module 09, 14; loại trừ file không cần thiết — `.gitignore`/`.dockerignore`) — Dockerfile CŨNG CẦN tuân thủ ĐẦY ĐỦ các nguyên tắc bảo mật/hiệu năng đã học, không phải là "ngoại lệ".

---

## Bài 6 — Kubernetes `deployment.yaml` với `livenessProbe`/`readinessProbe`

### Đề
`livenessProbe`/`readinessProbe` dùng Actuator `/actuator/health/liveness`/`/actuator/health/readiness`. `RollingUpdate` với `maxUnavailable: 0`, `maxSurge: 1`.

### Lời giải

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: library-app
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0   # KHÔNG BAO GIỜ giảm số Pod ĐANG PHỤC VỤ xuống dưới mức hiện tại trong lúc update
      maxSurge: 1          # cho phép TẠO THÊM tối đa 1 Pod MỚI (vượt quá replicas) trong lúc update
  selector:
    matchLabels:
      app: library-app
  template:
    metadata:
      labels:
        app: library-app
    spec:
      containers:
        - name: library-app
          image: mydockerhub/library-app:latest
          ports:
            - containerPort: 8080

          # LIVENESS: "Pod này CÓ CÒN SỐNG KHÔNG?" - nếu FAIL liên tục, Kubernetes TỰ ĐỘNG RESTART container
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30   # chờ 30s SAU KHI container start mới bắt đầu kiểm tra (đủ thời gian Spring Boot khởi động)
            periodSeconds: 10          # kiểm tra mỗi 10 giây
            failureThreshold: 3        # FAIL 3 LẦN LIÊN TIẾP mới coi là "chết", tránh restart nhầm do lag tạm thời

          # READINESS: "Pod này ĐÃ SẴN SÀNG NHẬN TRAFFIC CHƯA?" - nếu FAIL, Kubernetes NGỪNG gửi traffic
          # tới Pod này (loại khỏi Service load balancer) nhưng KHÔNG restart container
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
            failureThreshold: 3

          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
```

### Giải thích

- **`livenessProbe` vs `readinessProbe` — 2 CÂU HỎI HOÀN TOÀN KHÁC NHAU:**
  - **Liveness ("CÒN SỐNG không?"):** phát hiện tình huống ứng dụng bị **TREO/DEADLOCK** (process vẫn chạy về mặt hệ điều hành, nhưng KHÔNG CÒN PHẢN HỒI được request nào — VD deadlock giữa các thread nội bộ, đã học ở Module 05.1) — Kubernetes phản ứng bằng cách **RESTART** container đó, hy vọng khởi động lại sẽ "giải thoát" khỏi trạng thái treo.
  - **Readiness ("SẴN SÀNG PHỤC VỤ chưa?"):** phát hiện tình huống Pod **ĐANG CHẠY BÌNH THƯỜNG NHƯNG CHƯA (hoặc TẠM THỜI KHÔNG) SẴN SÀNG** nhận traffic — VD: vừa khởi động, đang "warm up" cache/kết nối DB; hoặc TẠM THỜI mất kết nối tới 1 dependency quan trọng (DB down tạm thời) — Kubernetes phản ứng bằng cách **NGỪNG ROUTE traffic** tới Pod đó (không đưa vào danh sách Service endpoint), NHƯNG **KHÔNG restart** — khi điều kiện phục hồi, Pod TỰ ĐỘNG được đưa trở lại phục vụ, không cần khởi động lại.
- **`/actuator/health/liveness` và `/actuator/health/readiness` là 2 endpoint TÁCH BIỆT của Spring Boot Actuator** (tính năng "Liveness and Readiness Probes" tích hợp sẵn từ Spring Boot 2.3+) — mặc định `/actuator/health/liveness` chỉ kiểm tra "ứng dụng JVM còn chạy bình thường" (không phụ thuộc DB/Redis); `/actuator/health/readiness` có thể được cấu hình để kiểm tra CẢ kết nối DB/dependency ngoài — đúng đắn phân biệt 2 loại kiểm tra khác mục đích này thay vì dùng CHUNG 1 endpoint `/actuator/health` cho cả 2 probe (nhầm lẫn phổ biến, có thể khiến Kubernetes RESTART container liên tục chỉ vì DB tạm thời chậm, dù bản thân ứng dụng vẫn hoàn toàn khỏe mạnh).
- **`maxUnavailable: 0` + `maxSurge: 1` — đảm bảo Zero-Downtime Deployment:** với `replicas: 3`, khi update, Kubernetes sẽ: (1) TẠO THÊM 1 Pod MỚI (tổng tạm thời 4 Pod, do `maxSurge: 1`) chạy version MỚI, (2) CHỜ Pod mới đó **PASS readinessProbe** (sẵn sàng nhận traffic THẬT), (3) **CHỈ SAU ĐÓ** mới bắt đầu tắt 1 Pod CŨ — lặp lại tuần tự cho tới khi TOÀN BỘ 3 Pod đều là version MỚI — nhờ `maxUnavailable: 0`, **LUÔN LUÔN CÓ ÍT NHẤT 3 Pod ĐANG PHỤC VỤ traffic** tại MỌI THỜI ĐIỂM trong suốt quá trình update, không có "khoảng trống" nào khiến user gặp lỗi 503 — đây chính là cách Kubernetes tận dụng TRỰC TIẾP kết quả của `readinessProbe` để đảm bảo an toàn tuyệt đối cho rolling update.

---

*Đây là lời giải cho toàn bộ Phần B của Module 29. Tiếp theo: Module 22 — Observability.*
