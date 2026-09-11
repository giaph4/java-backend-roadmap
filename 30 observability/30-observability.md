# Module 21 — Observability

> **Mức ưu tiên: 🟡 Trung bình (nhưng thiết yếu khi hệ thống chạy Microservices thật)**
> **Vì sao quan trọng:** Khi hệ thống chỉ là 1 Monolith chạy trên 1 server, `System.out.println` và đọc log thủ công còn khả thi. Nhưng khi đã học tới Microservices (Module 19) — 1 request đi qua 5-10 service khác nhau — câu hỏi "request này bị lỗi/chậm ở đâu?" **không thể trả lời được** nếu không có Observability. Đây là "con mắt" của hệ thống production: không có nó, mọi sự cố đều là "mò kim đáy bể".

> **Phạm vi bài này:** Tập trung vào 3 trụ cột Observability (Logs, Metrics, Traces) và công cụ triển khai ở tầng ứng dụng Spring Boot. Không đi sâu vận hành hạ tầng Elasticsearch/Prometheus Cluster ở quy mô lớn, hay thiết kế hệ thống chịu tải (Load Balancing, Scaling, CDN — thuộc Module 22 System Design tiếp theo).

---

## Mục lục

1. [3 trụ cột của Observability: Logs, Metrics, Traces](#1-3-trụ-cột-của-observability)
2. [Structured Logging](#2-structured-logging)
3. [Correlation ID — theo dõi 1 request xuyên suốt hệ thống](#3-correlation-id)
4. [Distributed Tracing với Zipkin/Jaeger](#4-distributed-tracing)
5. [Metrics với Micrometer & Prometheus](#5-metrics-với-micrometer--prometheus)
6. [Grafana Dashboard](#6-grafana-dashboard)
7. [ELK Stack — tập trung Log](#7-elk-stack)
8. [Alerting — cảnh báo chủ động](#8-alerting)
9. [SLI/SLO/SLA & Error Budget](#9-slislosla--error-budget)
10. [⚠️ Các bẫy hay gặp](#10-các-bẫy-hay-gặp)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. 3 trụ cột của Observability

**Observability (Khả năng quan sát)** là khả năng **hiểu được TRẠNG THÁI BÊN TRONG** của hệ thống chỉ dựa vào dữ liệu nó "phát ra" từ bên ngoài (log, metric, trace) — **KHÔNG cần** phải sửa code/debug trực tiếp trên production.

```
┌─────────────────────────────────────────────────┐
│                  OBSERVABILITY                       │
│                                                       │
│  ┌───────────┐   ┌───────────┐   ┌───────────┐      │
│  │   LOGS      │   │  METRICS   │   │  TRACES    │      │
│  │"Chuyện gì đã │   │"Hệ thống    │   │"Request đi   │      │
│  │  xảy ra?"   │   │ đang khỏe   │   │ qua đâu, mất  │      │
│  │             │   │ mạnh không?" │   │ bao lâu?"    │      │
│  └───────────┘   └───────────┘   └───────────┘      │
└─────────────────────────────────────────────────┘
```

| Trụ cột | Câu hỏi trả lời | Công cụ phổ biến |
|---|---|---|
| **Logs** | "Điều gì đã xảy ra tại thời điểm cụ thể này?" | Logback/Log4j2 + ELK Stack |
| **Metrics** | "Hệ thống đang hoạt động ở mức độ nào? (CPU, số request/giây, tỷ lệ lỗi...)" | Micrometer + Prometheus + Grafana |
| **Traces** | "1 request cụ thể đã đi qua những service nào, mất bao lâu ở mỗi bước?" | Micrometer Tracing + Zipkin/Jaeger |

**Phân biệt Monitoring vs Observability:**

| | Monitoring | Observability |
|---|---|---|
| Cách tiếp cận | Theo dõi các chỉ số **ĐÃ BIẾT TRƯỚC** cần quan tâm (CPU, RAM, uptime) | Thu thập đủ dữ liệu để trả lời **CÂU HỎI CHƯA BIẾT TRƯỚC** khi sự cố xảy ra |
| Ví dụ | Dashboard hiển thị CPU usage | Truy vấn "tại sao request của user X lúc 14:32 bị lỗi 500?" dựa trên trace/log chi tiết |
| Quan hệ | Là 1 PHẦN của Observability | Bao hàm cả Monitoring lẫn khả năng "đào sâu" (drill-down) khi có vấn đề mới phát sinh |

### RED Method — khung chọn Metric nên theo dõi cho mỗi Service

Với hàng trăm metric có thể thu thập, câu hỏi thực tế là: **nên bắt đầu theo dõi cái gì trước?** **RED Method** (phổ biến trong giới SRE — Site Reliability Engineering) đề xuất 3 chỉ số tối thiểu cho **mọi service hướng request** (API, microservice):

| Chữ cái | Đo gì | Ví dụ Metric |
|---|---|---|
| **R**ate | Số request/giây service đang xử lý | `rate(http_server_requests_seconds_count[1m])` |
| **E**rrors | Số/tỷ lệ request bị lỗi trên tổng số request | `rate(http_server_requests_seconds_count{status=~"5.."}[1m])` |
| **D**uration | Thời gian xử lý mỗi request (nên xem theo percentile, không chỉ average) | `histogram_quantile(0.95, ...)` — đã học ở mục 6 |

> **Liên hệ:** 3 chỉ số RED chính là bộ dashboard **tối thiểu** nên có cho mọi Microservice trước khi nghĩ tới việc thêm metric nghiệp vụ tùy chỉnh (Counter/Gauge riêng ở mục 5) — trả lời ngay câu hỏi "service này đang khỏe không" chỉ trong vài giây nhìn Dashboard, mà không cần biết trước sự cố cụ thể là gì. (Framework song song **USE Method** — Utilization/Saturation/Errors — dùng để theo dõi *tài nguyên hạ tầng* như CPU/Disk/Network thay vì service, không đi sâu ở đây vì thuộc phạm vi hạ tầng hơn là code ứng dụng.)

---

## 2. Structured Logging

### Vấn đề với Logging truyền thống (Unstructured — dạng text tự do)

```java
log.info("User " + userId + " đã đặt hàng " + orderId + " với số tiền " + amount);
// Output: "User 123 đã đặt hàng 456 với số tiền 500000"
```

**Vấn đề:** Log dạng **text tự do** rất khó **tìm kiếm/lọc/phân tích tự động** — muốn tìm "tất cả log liên quan tới `userId=123`" phải dùng regex phức tạp trên chuỗi text, dễ sai sót, không hiệu quả khi có hàng triệu dòng log.

### Structured Logging — log dưới dạng JSON (có cấu trúc)

```java
// Dùng MDC (Mapped Diagnostic Context) hoặc structured logging library (VD: logstash-logback-encoder)
log.info("Đơn hàng đã được tạo",
    kv("userId", userId),
    kv("orderId", orderId),
    kv("amount", amount),
    kv("event", "ORDER_CREATED"));
```

```json
// Output dạng JSON - MÁY TÍNH đọc và xử lý dễ dàng
{
  "timestamp": "2026-09-07T10:30:00Z",
  "level": "INFO",
  "message": "Đơn hàng đã được tạo",
  "userId": 123,
  "orderId": 456,
  "amount": 500000,
  "event": "ORDER_CREATED",
  "service": "order-service",
  "traceId": "abc123def456"
}
```

**Lợi ích của Structured Logging:**
- Dễ dàng **query chính xác** trong hệ thống tập trung log (VD: Elasticsearch): `userId: 123 AND event: ORDER_CREATED`
- Dễ dàng tạo **Dashboard/Alert** dựa trên field cụ thể (VD: đếm số lượng log có `level: ERROR` trong 5 phút)
- Tích hợp tốt với công cụ phân tích tự động (không cần regex phức tạp)

### Cấu hình Logback cho Structured Logging (`logback-spring.xml`)

```xml
<configuration>
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Tự động format log thành JSON, kèm các field mặc định (timestamp, level, thread...) -->
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE" />
    </root>
</configuration>
```

### Log Level — dùng đúng mức độ (nhắc lại nguyên tắc quan trọng)

| Level | Khi nào dùng |
|---|---|
| `ERROR` | Lỗi NGHIÊM TRỌNG, cần xử lý/chú ý NGAY (exception không mong muốn, hệ thống không hoạt động đúng) |
| `WARN` | Tình huống bất thường nhưng hệ thống VẪN hoạt động được (VD: fallback được kích hoạt, retry lần 2) |
| `INFO` | Sự kiện nghiệp vụ quan trọng (đơn hàng được tạo, user đăng nhập) — mức mặc định ở production |
| `DEBUG` | Chi tiết kỹ thuật hữu ích khi DEBUG (giá trị biến, luồng xử lý) — thường TẮT ở production (quá nhiều log) |
| `TRACE` | Chi tiết cực kỳ sâu (từng bước nhỏ nhất) — hiếm khi bật, kể cả lúc debug |

⚠️ **Bẫy quan trọng:** KHÔNG BAO GIỜ log thông tin nhạy cảm (password, token, số thẻ tín dụng, dữ liệu cá nhân) — đã cảnh báo ở Module 16, nhắc lại vì đây là lỗi rất dễ mắc khi debug rồi quên xóa log.

---

## 3. Correlation ID

**Vấn đề trong Microservices:** 1 request từ Client đi qua `API Gateway → Order Service → Inventory Service → Payment Service` — mỗi service ghi log **riêng biệt**. Làm sao biết **các dòng log ở các service KHÁC NHAU** đều thuộc về **CÙNG 1 request** ban đầu?

### Giải pháp: Correlation ID (hay Trace ID)

```
1. Request đến API Gateway -> Gateway SINH RA 1 ID DUY NHẤT (VD: UUID)
   -> gắn vào HTTP Header: X-Correlation-Id: abc-123-def

2. Gateway gọi Order Service -> TRUYỀN KÈM header X-Correlation-Id: abc-123-def

3. Order Service gọi Inventory Service -> TIẾP TỤC truyền kèm CÙNG ID này

4. Mọi service đều LOG kèm correlation ID này trong mỗi dòng log
```

```json
// Order Service log:
{"timestamp": "...", "message": "Tạo đơn hàng", "correlationId": "abc-123-def", "service": "order-service"}

// Inventory Service log (CÙNG request, SERVICE KHÁC):
{"timestamp": "...", "message": "Trừ tồn kho", "correlationId": "abc-123-def", "service": "inventory-service"}

// Payment Service log:
{"timestamp": "...", "message": "Xử lý thanh toán thất bại", "correlationId": "abc-123-def", "service": "payment-service", "level": "ERROR"}
```

→ Khi có lỗi, chỉ cần **query theo `correlationId: abc-123-def`** trong hệ thống log tập trung (ELK — mục 7) là thấy được **TOÀN BỘ hành trình** của request đó qua mọi service, theo đúng thứ tự thời gian.

### Triển khai trong Spring Boot bằng Filter + MDC

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString(); // Sinh mới nếu request từ bên ngoài chưa có
        }

        MDC.put("correlationId", correlationId); // MDC (Mapped Diagnostic Context) - Logback tự động
                                                     // đính kèm giá trị này vào MỌI dòng log trong request này
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear(); // QUAN TRỌNG: dọn dẹp MDC sau khi xong, tránh "rò rỉ" sang request khác
                          // (đặc biệt quan trọng khi dùng Thread Pool tái sử dụng thread)
        }
    }
}
```

⚠️ **Bẫy khi dùng MDC với `@Async`/Thread Pool:** MDC dựa trên `ThreadLocal` — khi 1 method chạy trong **thread KHÁC** (VD: `@Async`, hoặc code trong Message Queue Consumer), MDC context **KHÔNG tự động được truyền sang** thread mới. Cần chủ động truyền lại (dùng `TaskDecorator` cho `ThreadPoolTaskExecutor`, hoặc set MDC thủ công ở đầu Consumer method).

---

## 4. Distributed Tracing

**Distributed Tracing** đi xa hơn Correlation ID — không chỉ biết "log nào cùng thuộc 1 request", mà còn biết **CHÍNH XÁC** mỗi bước (Span) mất **bao lâu**, tạo thành 1 biểu đồ trực quan gọi là **Trace**.

### Khái niệm Span & Trace

```
Trace (toàn bộ hành trình của 1 request):
├── Span 1: API Gateway nhận request                [0ms   -> 250ms]  (tổng 250ms)
│   ├── Span 2: Gọi Order Service                    [10ms  -> 200ms]  (190ms)
│   │   ├── Span 3: Order Service query DB            [15ms  -> 45ms]   (30ms)
│   │   └── Span 4: Order Service gọi Inventory Service [50ms  -> 190ms] (140ms) <- CHẬM NHẤT!
│   │       └── Span 5: Inventory Service query DB      [60ms  -> 180ms] (120ms) <- TÌM RA NGUỒN GỐC CHẬM
└── (Response trả về client)
```

→ Nhìn vào Trace này, phát hiện ngay: **Inventory Service query DB đang mất 120ms** — đây chính là "thủ phạm" khiến toàn bộ request chậm, mà nếu chỉ nhìn log text thông thường sẽ rất khó phát hiện.

### Micrometer Tracing (thay thế Spring Cloud Sleuth đã ngừng phát triển)

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # Ghi lại 100% request (production thường giảm xuống 0.1 = 10% để tiết kiệm tài nguyên)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

**Điểm mạnh:** Micrometer Tracing **tự động** tạo Span cho các thao tác phổ biến (HTTP request, JDBC query, RestTemplate/WebClient call...) mà **không cần code thủ công** — chỉ cần thêm dependency + cấu hình.

```java
// Có thể tạo Span thủ công cho logic nghiệp vụ cụ thể cần theo dõi riêng
@Service
public class OrderService {

    private final Tracer tracer;

    public Order processOrder(OrderRequest request) {
        Span span = tracer.nextSpan().name("validate-order-business-rules").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // Logic validate phức tạp cần đo thời gian riêng
            validateBusinessRules(request);
        } finally {
            span.end();
        }
        // ...
    }
}
```

### Zipkin vs Jaeger — công cụ hiển thị Trace

| | Zipkin | Jaeger |
|---|---|---|
| Nguồn gốc | Twitter | Uber |
| Tích hợp Spring | Rất phổ biến, đơn giản (Micrometer Tracing hỗ trợ sẵn) | Cũng phổ biến, thường dùng trong hệ sinh thái Kubernetes/Cloud Native |
| Giao diện | Đơn giản, dễ dùng | Trực quan hơn 1 chút, nhiều tính năng phân tích sâu |
| Độ phổ biến thực tế | Cao trong hệ sinh thái Spring | Cao trong hệ sinh thái CNCF (Cloud Native Computing Foundation) |

> **Cả 2 công cụ hoạt động theo cùng nguyên lý** (thu thập Span, hiển thị Trace) — khác biệt chủ yếu ở giao diện và hệ sinh thái tích hợp. Chọn 1 trong 2 tùy theo công nghệ hạ tầng công ty đang dùng.

### OpenTelemetry — chuẩn hóa Observability, không phụ thuộc 1 vendor cụ thể

Micrometer Tracing (Brave) và Zipkin là 1 cặp công cụ cụ thể — nhưng ngành công nghiệp đang hội tụ về **OpenTelemetry (OTel)**, 1 dự án của **CNCF** định nghĩa **chuẩn chung** (API, SDK, giao thức) để thu thập cả 3 trụ cột Logs/Metrics/Traces, **không ràng buộc vào 1 backend cụ thể** (có thể xuất dữ liệu tới Zipkin, Jaeger, Prometheus, hay các nền tảng thương mại như Datadog/New Relic — chỉ cần đổi cấu hình exporter, không đổi code):

```xml
<!-- Micrometer Tracing cũng hỗ trợ OpenTelemetry làm bridge thay thế Brave -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

```
Ứng dụng Spring Boot -> instrument bằng OpenTelemetry API (chuẩn, vendor-neutral)
     │
     ▼
OpenTelemetry Collector (tùy chọn - gom dữ liệu, xử lý trước khi gửi đi)
     │
     ├──► Exporter tới Zipkin/Jaeger (traces)
     ├──► Exporter tới Prometheus (metrics)
     └──► Exporter tới hệ thống log tập trung (logs)
```

> **Vì sao quan trọng:** Trước đây mỗi loại dữ liệu (log/metric/trace) thường cần thư viện/giao thức riêng biệt của từng vendor, gây khóa chặt (vendor lock-in) và khó đổi công cụ giám sát sau này. OpenTelemetry cho phép **instrument code 1 lần** theo chuẩn chung, rồi tùy ý đổi backend hiển thị (Zipkin → Jaeger → nền tảng thương mại...) chỉ bằng cách đổi cấu hình exporter, không phải sửa lại code ứng dụng. Đây là xu hướng công nghiệp hiện tại — nên biết khái niệm và vì sao nó ra đời, dù ở mức học tập, dùng trực tiếp Zipkin qua Micrometer Tracing (Brave) như mục trên vẫn hoàn toàn đủ dùng và đơn giản hơn để bắt đầu.

---

## 5. Metrics với Micrometer & Prometheus

### Micrometer — lớp trừu tượng đo lường (giống Spring Cache Abstraction ở Module 18, nhưng cho Metrics)

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Micrometer tự động thu thập nhiều metric có sẵn** (nhờ tích hợp với Spring Boot Actuator đã học ở Module 13):
- JVM Metrics: heap memory, GC pause time, thread count (liên hệ Module 07 — JVM Internals)
- HTTP Metrics: số request/giây, response time, tỷ lệ status code (200/400/500)
- DataSource Metrics: connection pool usage (liên hệ HikariCP)

### 4 loại Metric cơ bản

```java
@Service
public class OrderService {

    private final MeterRegistry meterRegistry;

    public Order createOrder(OrderRequest request) {
        // Counter - đếm số LẦN xảy ra sự kiện, chỉ TĂNG, không giảm
        meterRegistry.counter("orders.created.total").increment();

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Order order = doCreateOrder(request);
            return order;
        } finally {
            // Timer - đo THỜI GIAN thực thi 1 thao tác
            sample.stop(meterRegistry.timer("orders.creation.duration"));
        }
    }

    @PostConstruct
    public void initGauge() {
        // Gauge - đo GIÁ TRỊ HIỆN TẠI có thể TĂNG/GIẢM (VD: số lượng đơn hàng đang xử lý)
        meterRegistry.gauge("orders.pending.count", pendingOrders, List::size);
    }

    // Distribution Summary - giống Timer nhưng đo GIÁ TRỊ bất kỳ (không chỉ thời gian, VD: kích thước file upload)
    public void recordOrderAmount(BigDecimal amount) {
        meterRegistry.summary("orders.amount.distribution").record(amount.doubleValue());
    }
}
```

| Loại Metric | Đo gì | Ví dụ |
|---|---|---|
| **Counter** | Số lần xảy ra (chỉ tăng) | Tổng số đơn hàng đã tạo, tổng số lỗi 500 |
| **Gauge** | Giá trị hiện tại (tăng/giảm) | Số connection đang mở, số item trong queue |
| **Timer** | Thời gian thực thi | Thời gian xử lý 1 API request |
| **Distribution Summary** | Phân phối giá trị bất kỳ | Kích thước file upload, giá trị đơn hàng |

### Prometheus — thu thập & lưu trữ Metrics theo mô hình "Pull"

```
Prometheus Server ĐỊNH KỲ (VD: mỗi 15 giây) TỰ ĐỘNG GỌI (pull/scrape)
tới endpoint /actuator/prometheus của MỖI instance ứng dụng
     │
     ▼
Lưu trữ dữ liệu dạng Time-Series Database (dữ liệu gắn với mốc thời gian)
     │
     ▼
Cho phép query bằng ngôn ngữ riêng PromQL
```

```yaml
# application.yml - Bật endpoint /actuator/prometheus (liên hệ Module 13 - Actuator)
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
  metrics:
    tags:
      application: order-service # Gắn nhãn để phân biệt metric của service nào khi có nhiều instance
```

```yaml
# prometheus.yml - Cấu hình Prometheus Server biết SCRAPE (lấy dữ liệu) từ đâu
scrape_configs:
  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:8080']
    scrape_interval: 15s
```

⚠️ **Phân biệt Pull (Prometheus) vs Push model:** Prometheus dùng mô hình **Pull** (Server tự động "kéo" dữ liệu định kỳ) — khác với hệ thống Logging (thường theo mô hình **Push**, ứng dụng tự động "đẩy" log ra ngoài). Mô hình Pull giúp Prometheus dễ dàng kiểm soát tần suất/khối lượng dữ liệu thu thập, tránh ứng dụng bị quá tải bởi việc gửi metric liên tục.

---

## 6. Grafana Dashboard

**Grafana** là công cụ **trực quan hóa (visualization)** — kết nối tới Prometheus (hoặc nhiều nguồn dữ liệu khác) để vẽ **Dashboard** (biểu đồ, đồng hồ đo, bảng số liệu) hiển thị Metrics theo thời gian thực.

```
┌─────────────────────────────────────────────┐
│              Grafana Dashboard                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │Request/sec │  │Error Rate  │  │P95 Latency │    │
│  │   1,250    │  │   0.3%     │  │   145ms    │    │
│  └──────────┘  └──────────┘  └──────────┘    │
│  ┌─────────────────────────────────────┐    │
│  │  Biểu đồ HTTP Request theo thời gian    │    │
│  │  📈 (đường biểu diễn tăng/giảm)         │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
              ▲ query dữ liệu bằng PromQL
              │
      ┌───────────────┐
      │  Prometheus     │
      └───────────────┘
```

### PromQL — ví dụ query cơ bản

```promql
# Tổng số request/giây cho order-service
rate(http_server_requests_seconds_count{application="order-service"}[1m])

# Tỷ lệ lỗi (status 5xx) trong 5 phút gần nhất
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))

# P95 Latency (95% request nhanh hơn giá trị này) - chỉ số QUAN TRỌNG hơn trung bình (average)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

⚠️ **Vì sao P95/P99 quan trọng hơn "thời gian trung bình" (average)?** Thời gian trung bình có thể **che giấu** vấn đề — VD: 99 request mất 50ms, 1 request mất 5000ms → trung bình chỉ ~99ms (trông "ổn"), nhưng **P99 = 5000ms** cho thấy rõ **1% user đang có trải nghiệm rất tệ**. Trong thực tế production, luôn theo dõi P95/P99 thay vì chỉ nhìn average.

---

## 7. ELK Stack

**ELK Stack** (Elasticsearch + Logstash + Kibana) là bộ công cụ phổ biến để **tập trung hóa (centralize)** log từ nhiều service/instance vào **1 nơi duy nhất**, dễ tìm kiếm/phân tích.

```
┌───────────┐   ┌───────────┐   ┌───────────┐
│Order Service│   │User Service │   │Payment Service│
│  (log JSON) │   │  (log JSON) │   │  (log JSON) │
└──────┬──────┘   └──────┬──────┘   └──────┬──────┘
       │                  │                  │
       └──────────────────┴──────────────────┘
                           │ (gửi log qua Filebeat/Logstash)
                           ▼
                  ┌───────────────┐
                  │  Elasticsearch   │  <- Lưu trữ, đánh index log để tìm kiếm nhanh
                  └───────────────┘
                           ▲
                           │ query
                  ┌───────────────┐
                  │    Kibana        │  <- Giao diện tìm kiếm/trực quan hóa log
                  └───────────────┘
```

| Thành phần | Vai trò |
|---|---|
| **Elasticsearch** | Database chuyên biệt cho **tìm kiếm full-text** cực nhanh (đã giới thiệu khái niệm ở Module 10) — lưu trữ và đánh index log |
| **Logstash** (hoặc **Filebeat** nhẹ hơn) | Thu thập log từ nhiều nguồn, xử lý/chuẩn hóa (parse), rồi đẩy vào Elasticsearch |
| **Kibana** | Giao diện web để tìm kiếm, lọc, tạo Dashboard trực quan từ dữ liệu trong Elasticsearch |

### Luồng hoạt động thực tế

```
1. Ứng dụng (Order Service) ghi log dạng JSON ra file/console (đã học Structured Logging - mục 2)
2. Filebeat (agent nhẹ, chạy cùng mỗi container/server) đọc file log, gửi sang Logstash/Elasticsearch
3. Logstash (tùy chọn) xử lý thêm (parse, enrich, filter) trước khi lưu
4. Elasticsearch lưu trữ, đánh index theo từng field (userId, correlationId, level...)
5. Developer/DevOps vào Kibana, tìm kiếm: "correlationId: abc-123-def" 
   -> Thấy NGAY toàn bộ log liên quan tới request đó, xuyên suốt MỌI service
```

**Ví dụ tìm kiếm trong Kibana:**

```
level: ERROR AND service: payment-service AND @timestamp: [now-1h TO now]
-> Tìm tất cả log ERROR của payment-service trong 1 giờ gần nhất
```

> **Liên hệ trực tiếp Correlation ID (mục 3):** Đây chính là lý do Correlation ID quan trọng — không có nó, dù đã tập trung log vào Elasticsearch, vẫn **không thể** nối các dòng log từ nhiều service thành 1 "câu chuyện" hoàn chỉnh của 1 request cụ thể.

---

## 8. Alerting

**Alerting** là bước cuối cùng khép kín vòng Observability — thay vì phải **chủ động mở Dashboard để xem**, hệ thống **TỰ ĐỘNG cảnh báo** (Slack, Email, PagerDuty...) khi phát hiện bất thường.

```yaml
# Alertmanager rule (Prometheus) - VÍ DỤ
groups:
  - name: order-service-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5..", application="order-service"}[5m]))
          /
          sum(rate(http_server_requests_seconds_count{application="order-service"}[5m])) > 0.05
        for: 2m   # Điều kiện phải ĐÚNG LIÊN TỤC 2 phút mới báo (tránh cảnh báo do dao động ngắn hạn)
        labels:
          severity: critical
        annotations:
          summary: "Tỷ lệ lỗi 5xx của order-service vượt quá 5% trong 5 phút gần nhất"
```

**Nguyên tắc thiết kế Alert tốt:**
- **Actionable** — mỗi cảnh báo phải rõ ràng "cần làm gì tiếp theo", tránh cảnh báo "cho có" mà không ai xử lý
- **Tránh Alert Fatigue** (quá nhiều cảnh báo khiến người nhận "chai lì", bỏ qua cả cảnh báo quan trọng) — chỉ cảnh báo những vấn đề THỰC SỰ cần con người can thiệp ngay
- Dùng `for: Xm` (khoảng thời gian) để tránh cảnh báo do **dao động tạm thời** (spike ngắn hạn không phải vấn đề thật)

---

## 9. SLI/SLO/SLA & Error Budget

Alerting (mục 8) trả lời "khi nào báo động" — nhưng **ngưỡng nào là hợp lý để báo động?** SLI/SLO/SLA là khung khái niệm chuẩn (từ Google SRE) để trả lời câu hỏi đó **dựa trên mục tiêu độ tin cậy đã thống nhất trước**, thay vì chọn ngưỡng tùy hứng.

### 3 khái niệm, dễ nhầm lẫn tên gọi

| Khái niệm | Ý nghĩa | Ví dụ |
|---|---|---|
| **SLI** (Service Level *Indicator*) | Chỉ số ĐO ĐƯỢC THỰC TẾ, phản ánh chất lượng dịch vụ | "Tỷ lệ request thành công trong 5 phút qua là 99.95%" |
| **SLO** (Service Level *Objective*) | MỤC TIÊU nội bộ team đặt ra cho SLI | "Tỷ lệ request thành công phải ≥ 99.9% mỗi tháng" |
| **SLA** (Service Level *Agreement*) | CAM KẾT chính thức với khách hàng/bên ngoài, thường có ràng buộc pháp lý/bồi thường nếu không đạt | "Cam kết uptime 99.5%, nếu không đạt sẽ hoàn phí theo hợp đồng" |

```
SLI (đo được) --------> SLO (mục tiêu nội bộ, thường KHẮT KHE HƠN SLA) --------> SLA (cam kết ra bên ngoài)

VD cụ thể:
SLI: 99.95% request trả về < 200ms trong tháng này (con số đo thực tế)
SLO: Team đặt mục tiêu SLI phải ≥ 99.9% (mục tiêu nội bộ, có margin an toàn)
SLA: Cam kết với khách hàng SLI ≥ 99.5% (thường lỏng hơn SLO để có "đệm" an toàn)
```

> **Nguyên tắc:** SLO luôn nên **khắt khe hơn** SLA đã cam kết ra bên ngoài — để team có "khoảng đệm" phát hiện và xử lý vấn đề TRƯỚC KHI vi phạm SLA thực sự (gây hậu quả hợp đồng/tài chính).

### Error Budget — "ngân sách lỗi" được phép tiêu

Nếu SLO là 99.9% (cho phép tối đa 0.1% request lỗi/downtime), phần **0.1% còn lại** chính là **Error Budget** — "ngân sách" lỗi được phép "tiêu" trong 1 chu kỳ (thường 1 tháng) mà KHÔNG bị coi là vi phạm mục tiêu:

```
SLO 99.9% uptime/tháng -> Error Budget = 0.1% × 30 ngày ≈ 43 phút downtime được PHÉP mỗi tháng

Nếu team đã "tiêu" hết 43 phút Error Budget trong 10 ngày đầu tháng:
-> Chính sách phổ biến: TẠM DỪNG release tính năng mới, ưu tiên TUYỆT ĐỐI cho việc ổn định hệ thống
   cho tới khi qua chu kỳ mới hoặc Error Budget được "làm mới"

Nếu Error Budget còn dư nhiều:
-> Team có thể tự tin release nhanh hơn, chấp nhận rủi ro thử nghiệm tính năng mới
```

> **Giá trị thực tế của Error Budget:** Nó biến cuộc tranh luận trừu tượng "nên ưu tiên tốc độ ra tính năng mới hay ổn định hệ thống?" thành **1 con số cụ thể, đo được** — dùng chung làm cơ sở ra quyết định giữa Product/Engineering, thay vì tranh cãi cảm tính. Đây cũng là câu hỏi khá thường gặp ở vòng phỏng vấn về tư duy vận hành hệ thống (SRE mindset) cho vị trí Backend/Senior.

---

## 10. ⚠️ Các bẫy hay gặp

1. **Log dạng text tự do (unstructured)** ở hệ thống production quy mô lớn — không thể tìm kiếm/phân tích hiệu quả khi có hàng triệu dòng log.

2. **Không có Correlation ID** trong hệ thống Microservices — không thể ghép nối log của cùng 1 request qua nhiều service, debug cực kỳ khó khăn.

3. **MDC không được dọn dẹp (`MDC.clear()`)** sau mỗi request — trong môi trường Thread Pool tái sử dụng, correlation ID của request TRƯỚC có thể "rò rỉ" sang request SAU, gây log sai lệch nghiêm trọng.

4. **MDC không tự động truyền sang thread mới** khi dùng `@Async`/Message Queue Consumer — mất Correlation ID giữa các bước bất đồng bộ.

5. **Chỉ theo dõi "thời gian trung bình" (average) thay vì P95/P99** — che giấu vấn đề thực sự ảnh hưởng một bộ phận user.

6. **Bật `sampling.probability: 1.0` (ghi 100% trace) ở production tải cao** mà không cân nhắc — tốn tài nguyên lưu trữ/xử lý đáng kể, nên giảm xuống (VD: 0.1 = 10%) khi hệ thống đã ổn định.

7. **Log thông tin nhạy cảm** (password, token, dữ liệu cá nhân) — dù vô tình khi debug, vẫn là lỗ hổng bảo mật nghiêm trọng nếu log được tập trung vào hệ thống có nhiều người truy cập.

8. **Cấu hình Alert quá nhạy (không có `for` duration)** — cảnh báo liên tục do dao động ngắn hạn bình thường, gây "Alert Fatigue", khiến người nhận bỏ qua cả cảnh báo thật sự quan trọng.

9. **Không giới hạn TTL (retention) cho log/metrics/trace** — dữ liệu tích lũy vô hạn, tốn chi phí lưu trữ không cần thiết (thường chỉ cần giữ log chi tiết vài tuần, metrics tổng hợp có thể giữ lâu hơn).

10. **Chỉ có Metrics mà không có Logs/Traces (hoặc ngược lại)** — 3 trụ cột bổ trợ lẫn nhau: Metrics cho biết "CÓ vấn đề", Traces cho biết "vấn đề Ở ĐÂU", Logs cho biết "CHI TIẾT điều gì đã xảy ra" — thiếu 1 trong 3 khiến quá trình debug sự cố production chậm và khó khăn hơn nhiều.

11. **Đặt SLO cao hơn cả những gì hệ thống THỰC SỰ cần** (VD: SLO 99.99% cho 1 API nội bộ ít quan trọng) — buộc team tốn công sức/chi phí vận hành không tương xứng với giá trị nghiệp vụ thực tế; SLO nên phản ánh đúng mức độ quan trọng của service, không phải "càng cao càng tốt".

12. **Không có quy trình rõ ràng khi Error Budget cạn kiệt** — đặt ra SLO/Error Budget nhưng không có chính sách hành động cụ thể (VD: tạm dừng release) khi vi phạm, khiến khái niệm này chỉ tồn tại trên giấy mà không ảnh hưởng thực tế tới cách team ra quyết định.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| 3 trụ cột Observability | Logs (chuyện gì xảy ra) + Metrics (hệ thống khỏe không) + Traces (đi qua đâu, mất bao lâu) |
| RED Method | Rate, Errors, Duration — bộ metric tối thiểu cho mọi service hướng request |
| Structured Logging | Log dạng JSON — dễ tìm kiếm/phân tích tự động hơn text tự do |
| Correlation ID | ID duy nhất theo 1 request xuyên suốt mọi service — ghép nối log lại thành 1 câu chuyện |
| MDC | Cơ chế Logback gắn Correlation ID vào mọi log — cẩn thận với Thread Pool/@Async |
| Distributed Tracing | Span + Trace — biết chính xác bước nào chậm trong chuỗi gọi Microservices |
| Zipkin/Jaeger | Công cụ hiển thị Trace trực quan |
| OpenTelemetry | Chuẩn vendor-neutral thu thập Logs/Metrics/Traces, không khóa chặt vào 1 backend |
| Micrometer | Lớp trừu tượng đo Metrics — Counter/Gauge/Timer/Distribution Summary |
| Prometheus | Thu thập Metrics theo mô hình Pull, lưu Time-Series, query bằng PromQL |
| Grafana | Trực quan hóa Metrics thành Dashboard |
| P95/P99 | Quan trọng hơn Average — phản ánh trải nghiệm của nhóm user chịu độ trễ cao nhất |
| ELK Stack | Elasticsearch (lưu trữ+tìm kiếm) + Logstash/Filebeat (thu thập) + Kibana (giao diện) |
| Alerting | Chủ động cảnh báo — cần `for` duration để tránh Alert Fatigue |
| SLI/SLO/SLA | Chỉ số đo được → mục tiêu nội bộ (khắt khe hơn) → cam kết bên ngoài |
| Error Budget | "Ngân sách" lỗi được phép tiêu — cạn thì ưu tiên ổn định thay vì release mới |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Structured Logging (dạng JSON) dễ tìm kiếm/phân tích tự động hơn logging dạng text tự do.
2. Correlation ID chỉ cần thiết trong kiến trúc Monolith, không cần trong Microservices.
3. MDC dựa trên ThreadLocal, nên cần chủ động xử lý khi dùng `@Async` hoặc Thread Pool để tránh mất/rò rỉ Correlation ID.
4. P95 Latency luôn có giá trị THẤP HƠN Average Latency.
5. Prometheus thu thập Metrics theo mô hình "Push" — ứng dụng chủ động gửi dữ liệu tới Prometheus Server.
6. Distributed Tracing giúp xác định CHÍNH XÁC bước nào trong chuỗi gọi Microservices đang gây ra độ trễ cao.
7. Alert không có điều kiện "for duration" có nguy cơ gây Alert Fatigue do cảnh báo dao động ngắn hạn không thực sự nghiêm trọng.
8. Kibana là công cụ LƯU TRỮ log, còn Elasticsearch là công cụ TÌM KIẾM/TRỰC QUAN HÓA.
9. SLO (Service Level Objective) thường nên khắt khe hơn SLA đã cam kết với khách hàng, để có khoảng đệm an toàn.
10. Khi Error Budget đã cạn kiệt trong chu kỳ hiện tại, thực hành phổ biến là team tiếp tục release tính năng mới bình thường vì Error Budget không ảnh hưởng tới quyết định kỹ thuật.

### Phần B — Bài tập viết code (6 bài)

**Bài 1:** Viết 1 `CorrelationIdFilter` đầy đủ (dùng MDC) cho ứng dụng Spring Boot, đảm bảo sinh mới Correlation ID nếu request từ client chưa có, và luôn dọn dẹp MDC sau khi xử lý xong (kể cả khi có exception).

**Bài 2:** Viết cấu hình `logback-spring.xml` dùng `LogstashEncoder` để log dạng JSON, đảm bảo Correlation ID (từ MDC) được tự động đính kèm vào mỗi dòng log.

**Bài 3:** Viết 1 Service method dùng Micrometer để đo: (a) Counter đếm tổng số lần gọi API `checkout`, (b) Timer đo thời gian xử lý, (c) Gauge theo dõi số lượng đơn hàng đang ở trạng thái `PROCESSING`.

**Bài 4:** Viết 1 PromQL query tính tỷ lệ lỗi (status 4xx và 5xx) trên tổng số request của service `payment-service` trong 5 phút gần nhất.

**Bài 5:** Giải thích bằng ví dụ cụ thể (không cần code) tình huống mà chỉ có Metrics KHÔNG ĐỦ để debug sự cố — cần kết hợp thêm Distributed Tracing và Logs mới tìm ra nguyên nhân gốc rễ.

**Bài 6:** Cho 1 API thanh toán có SLO "99.95% request thành công mỗi tháng". Tính Error Budget (số phút downtime/lỗi được phép) trong 1 tháng 30 ngày, và đề xuất 1 chính sách hành động cụ thể khi Error Budget đã tiêu hết 80% trước ngày 20 của tháng.

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Đúng.** Đây chính là lợi ích cốt lõi của Structured Logging (JSON) — máy tính parse và query theo field cụ thể dễ dàng hơn nhiều so với regex trên text tự do.
2. **Sai.** Ngược lại — Correlation ID gần như KHÔNG cần thiết trong Monolith (mọi thứ xử lý trong 1 process, dễ theo dõi), nhưng CỰC KỲ quan trọng trong Microservices để ghép nối log qua nhiều service.
3. **Đúng.** MDC dựa trên ThreadLocal — khi code chạy trên thread khác (`@Async`, Message Consumer), context không tự động truyền theo, cần xử lý thủ công (TaskDecorator, set MDC lại ở đầu method).
4. **Sai.** P95 luôn CAO HƠN HOẶC BẰNG Average trong phân phối thông thường (vì P95 phản ánh nhóm request CHẬM NHẤT trong 95% dữ liệu, trong khi Average bị "kéo xuống" bởi phần lớn request nhanh).
5. **Sai.** Prometheus dùng mô hình "Pull" — Prometheus Server CHỦ ĐỘNG gọi (scrape) tới endpoint `/actuator/prometheus` của ứng dụng theo định kỳ, không phải ứng dụng tự đẩy dữ liệu đi.
6. **Đúng.** Đây chính là mục đích thiết kế của Distributed Tracing — nhìn thấy rõ Span nào (bước nào) mất nhiều thời gian nhất trong toàn bộ Trace.
7. **Đúng.** Không có `for` duration, alert có thể kích hoạt do 1 spike ngắn hạn bình thường, gây báo động giả liên tục, khiến người nhận dần bỏ qua cả cảnh báo thật.
8. **Sai.** Ngược lại — Elasticsearch là nơi LƯU TRỮ + ĐÁNH INDEX (tìm kiếm), còn Kibana là GIAO DIỆN để tìm kiếm/trực quan hóa dữ liệu đó.
9. **Đúng.** SLO khắt khe hơn SLA giúp team có "khoảng đệm" phát hiện và xử lý vấn đề trước khi thực sự vi phạm cam kết với khách hàng.
10. **Sai.** Ngược lại — thực hành phổ biến khi Error Budget cạn kiệt là TẠM DỪNG release tính năng mới, ưu tiên ổn định hệ thống cho tới khi Error Budget được làm mới ở chu kỳ tiếp theo.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Luôn dọn dẹp, kể cả khi có exception xảy ra trong quá trình xử lý request
            MDC.remove(MDC_KEY);
        }
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```xml
<configuration>
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>correlationId</includeMdcKeyName>
            <!-- LogstashEncoder mặc định đã TỰ ĐỘNG bao gồm toàn bộ MDC context vào output JSON,
                 dòng includeMdcKeyName ở trên là tùy chọn để CHỈ ĐỊNH RÕ (nếu muốn giới hạn field) -->
            <customFields>{"service":"order-service"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE" />
    </root>
</configuration>
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
@Service
public class CheckoutService {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger processingOrdersCount = new AtomicInteger(0);

    public CheckoutService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Gauge - theo dõi giá trị HIỆN TẠI, tự động cập nhật mỗi khi Prometheus scrape
        meterRegistry.gauge("orders.processing.count", processingOrdersCount);
    }

    public CheckoutResult checkout(CheckoutRequest request) {
        meterRegistry.counter("checkout.requests.total").increment(); // (a) Counter

        Timer.Sample sample = Timer.start(meterRegistry); // (b) Timer bắt đầu đo
        processingOrdersCount.incrementAndGet(); // (c) Gauge tăng khi bắt đầu xử lý

        try {
            CheckoutResult result = doCheckout(request);
            meterRegistry.counter("checkout.success.total").increment();
            return result;
        } catch (Exception e) {
            meterRegistry.counter("checkout.failure.total").increment();
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("checkout.duration")); // (b) Timer kết thúc đo
            processingOrdersCount.decrementAndGet(); // (c) Gauge giảm khi xử lý xong
        }
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```promql
sum(rate(http_server_requests_seconds_count{application="payment-service", status=~"4..|5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count{application="payment-service"}[5m]))
```

*(Giải thích: `status=~"4..|5.."` là regex khớp mọi status code bắt đầu bằng 4 hoặc 5 — tức nhóm 4xx và 5xx. `rate(...[5m])` tính tốc độ tăng trung bình trong cửa sổ 5 phút gần nhất, chia tử số cho mẫu số ra tỷ lệ phần trăm lỗi.)*

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

**Tình huống cụ thể:** Dashboard Grafana (dựa trên Metrics) cho thấy **P99 Latency của API `POST /orders` tăng đột biến** từ 200ms lên 3000ms trong 10 phút gần đây. Metrics này TRẢ LỜI ĐƯỢC câu hỏi **"CÓ vấn đề gì đó xảy ra"** và **"mức độ nghiêm trọng"** (P99 tăng gấp 15 lần) — nhưng **KHÔNG cho biết** *tại sao* lại chậm, hay *bước nào* trong toàn bộ chuỗi xử lý là nguyên nhân.

**Bước tiếp theo cần Distributed Tracing:** Xem 1 vài Trace cụ thể của các request `POST /orders` bị chậm trong khoảng thời gian đó — phát hiện: Span "Order Service gọi Inventory Service" đang chiếm **2800ms/3000ms tổng thời gian** (gần như toàn bộ độ trễ nằm ở bước gọi sang Inventory Service), trong khi các Span khác (validate, lưu Order DB) vẫn bình thường (~50-100ms).

**Bước cuối cùng cần Logs:** Bây giờ đã biết "thủ phạm" là Inventory Service, vào xem **log chi tiết** của chính Inventory Service tại đúng khung thời gian đó (lọc theo `correlationId` của 1 trace cụ thể vừa tìm được) — phát hiện dòng log: `"Đang chờ Connection Pool - pool đã đạt maximum 10 connections"` — từ đó xác định NGUYÊN NHÂN GỐC RỄ: **HikariCP Connection Pool của Inventory Service bị cạn kiệt** (có thể do 1 query nào đó đang giữ connection quá lâu, hoặc traffic tăng đột biến vượt quá pool size đã cấu hình — liên hệ Module 10/15).

**Kết luận:** Chỉ với Metrics, ta biết "có vấn đề và mức độ nghiêm trọng". Chỉ với Tracing, ta thu hẹp được "vấn đề nằm ở service/bước nào". Chỉ với Logs chi tiết của đúng service/thời điểm đó (nhờ Correlation ID liên kết từ Trace), ta mới tìm ra được **nguyên nhân kỹ thuật cụ thể** để khắc phục (tăng pool size, tối ưu query đang giữ connection lâu...). Đây chính là lý do 3 trụ cột Observability phải đi CÙNG NHAU, không thể chỉ dựa vào 1 trụ cột duy nhất khi debug sự cố production phức tạp.

</details>

<details>
<summary><b>Đáp án Bài 6</b></summary>

**Tính Error Budget:**

```
SLO: 99.95% request thành công/tháng
-> Tỷ lệ lỗi cho phép: 100% - 99.95% = 0.05%

1 tháng 30 ngày = 30 × 24 × 60 = 43,200 phút

Error Budget = 0.05% × 43,200 phút = 21.6 phút downtime/lỗi được PHÉP trong tháng
```

**Chính sách hành động khi đã tiêu 80% Error Budget trước ngày 20/30:**

```
Đã dùng: 80% × 21.6 phút ≈ 17.3 phút (trong 20/30 ngày, tức 2/3 chu kỳ)
Còn lại: ~4.3 phút cho 10 ngày còn lại của tháng -> RẤT MỎNG, rủi ro cao

Đề xuất chính sách cụ thể:
1. TẠM DỪNG mọi release tính năng mới không khẩn cấp cho tới hết tháng
   (giảm thiểu rủi ro gây thêm downtime/lỗi từ thay đổi mới)
2. Ưu tiên TUYỆT ĐỐI cho việc điều tra nguyên nhân đã gây tiêu tốn 80% Error Budget
   (dùng Distributed Tracing + Logs như đã học ở Bài 5 để xác định nguyên nhân gốc rễ)
3. Chỉ cho phép deploy các bản vá lỗi (hotfix) liên quan trực tiếp tới việc cải thiện độ ổn định
4. Thông báo cho các bên liên quan (Product Owner, khách hàng nếu cần theo SLA)
   về tình trạng Error Budget để họ hiểu vì sao tốc độ ra tính năng mới bị chậm lại tạm thời
5. Sau khi qua chu kỳ mới (đầu tháng sau), Error Budget được "làm mới" (reset) hoàn toàn,
   quay lại nhịp độ phát triển bình thường
```

**Ý nghĩa:** Đây là cách Error Budget biến 1 quyết định thường mang tính cảm tính ("có nên release tính năng mới lúc này không?") thành quyết định dựa trên **dữ liệu cụ thể, đã thống nhất từ trước** — giảm tranh cãi, tăng tính minh bạch giữa Engineering và Product.

</details>

---

*File tiếp theo trong lộ trình: **Module 22 — System Design cơ bản cho Backend** (Load Balancing, Horizontal vs Vertical Scaling, Database Replication & Sharding, CDN, Rate Limiting, thiết kế hệ thống quy mô lớn: URL Shortener/News Feed — bài toán phỏng vấn kinh điển).*
