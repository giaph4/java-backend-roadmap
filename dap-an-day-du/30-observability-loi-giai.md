# Lời giải đầy đủ — Module 22: Observability

> Nguồn đề: `30 observability/30-observability.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — `CorrelationIdFilter` dùng MDC

### Đề
Sinh mới Correlation ID nếu client chưa có, luôn dọn dẹp MDC sau khi xử lý (kể cả khi có exception).

### Lời giải

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // chạy TRƯỚC TIÊN trong filter chain - mọi filter/log sau đó đều có sẵn Correlation ID
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // Lấy Correlation ID từ header nếu client ĐÃ TRUYỀN SẴN (VD từ 1 service khác gọi tới,
        // muốn TRUYỀN TIẾP cùng 1 ID xuyên suốt toàn bộ chuỗi gọi) - nếu KHÔNG có, sinh MỚI
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        try {
            MDC.put(MDC_KEY, correlationId);   // gắn vào MDC - MỌI dòng log SAU ĐÓ trong request này TỰ ĐỘNG có ID này
            response.setHeader(CORRELATION_ID_HEADER, correlationId);   // trả lại cho client - hữu ích khi client cần tra cứu log

            filterChain.doFilter(request, response);   // tiếp tục xử lý request BÌNH THƯỜNG

        } finally {
            // BẮT BUỘC dọn dẹp MDC - KỂ CẢ KHI CÓ EXCEPTION (đảm bảo bằng finally)
            MDC.remove(MDC_KEY);
        }
    }
}
```

### Giải thích

- **`finally` là BẮT BUỘC TUYỆT ĐỐI, không phải "cẩn thận cho chắc":** `MDC` (Mapped Diagnostic Context) lưu trữ dữ liệu **THEO TỪNG THREAD** (dùng `ThreadLocal` bên dưới) — nhưng trong môi trường server dùng **THREAD POOL** (Tomcat tái sử dụng thread cho nhiều request KHÁC NHAU theo thời gian), nếu KHÔNG dọn dẹp (`MDC.remove`) sau khi request A xử lý xong, thread đó khi được TÁI SỬ DỤNG cho request B (khác biệt hoàn toàn) sẽ **VÔ TÌNH MANG THEO Correlation ID CỦA REQUEST A** — gây ra log CỦA REQUEST B bị gắn NHẦM ID của A, cực kỳ khó debug (log sai lệch, dẫn điều tra sai hướng).
- **Đặt `try` BAO QUANH `filterChain.doFilter(...)`** — nếu bất kỳ filter/controller nào phía sau ném exception, `finally` VẪN ĐẢM BẢO chạy `MDC.remove()` — đây là ứng dụng thực tế của kiến thức `try-finally` đảm bảo dọn dẹp tài nguyên đã học ở Module 04 (Exception Handling & I/O).
- **`@Order(Ordered.HIGHEST_PRECEDENCE)`**: đảm bảo Correlation ID được gắn vào MDC **SỚM NHẤT CÓ THỂ** trong chuỗi xử lý request — nếu filter này chạy SAU 1 filter khác (VD `SecurityFilterChain`), các log sinh ra TRONG filter đó sẽ **THIẾU** Correlation ID.
- **Kiểm tra header `X-Correlation-Id` ĐÃ CÓ SẴN (nếu request tới TỪ 1 service khác)**: đây là cơ chế cho phép **1 Correlation ID DUY NHẤT XUYÊN SUỐT TOÀN BỘ CHUỖI GỌI** trong hệ thống Microservices (Service A gọi Service B, B gắn LẠI ĐÚNG ID mà A đã tạo, KHÔNG SINH MỚI) — đây chính là nền tảng của Distributed Tracing (sẽ mở rộng thêm ở Bài 5), cho phép TRUY VẾT 1 request THẬT SỰ đi qua NHIỀU SERVICE khác nhau bằng CÙNG 1 định danh.

---

## Bài 2 — `logback-spring.xml` với `LogstashEncoder` (log JSON)

### Đề
Log dạng JSON, Correlation ID (từ MDC) tự động đính kèm mỗi dòng log.

### Lời giải

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- includeMdcKeyName KHÔNG cần khai báo tường minh nếu muốn TOÀN BỘ MDC được nhúng tự động -
                 LogstashEncoder MẶC ĐỊNH tự động đưa TẤT CẢ key/value hiện có trong MDC vào JSON output -->
            <customFields>{"application":"library-service"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE" />
    </root>

</configuration>
```

**Ví dụ 1 dòng log JSON output tương ứng:**
```json
{"@timestamp":"2026-09-11T10:15:30.123+07:00","level":"INFO","logger_name":"com.example.OrderService","message":"Đơn hàng #123 đã được tạo thành công","application":"library-service","correlationId":"7f9c2a4e-1234-5678-90ab-cdef12345678"}
```

### Giải thích

- **`LogstashEncoder` (thư viện `logstash-logback-encoder`)** tự động chuyển MỖI dòng log thành **1 OBJECT JSON HOÀN CHỈNH** thay vì chuỗi text thông thường (VD `"10:15:30 INFO OrderService - Đơn hàng đã tạo"`) — JSON structured logging cho phép các công cụ tổng hợp log (ELK Stack — Elasticsearch/Logstash/Kibana, Grafana Loki...) **PARSE VÀ LỌC/TÌM KIẾM CHÍNH XÁC theo TỪNG FIELD** (VD tìm TẤT CẢ log có `correlationId = "7f9c..."`, hoặc lọc theo `level = "ERROR"`) — khác hẳn log text thuần túy chỉ tìm được bằng "grep" mờ, dễ nhầm lẫn.
- **Correlation ID TỰ ĐỘNG xuất hiện trong JSON MÀ KHÔNG CẦN CẤU HÌNH GÌ THÊM** — vì `LogstashEncoder` mặc định đưa **TOÀN BỘ nội dung MDC hiện có** vào output — do `CorrelationIdFilter` (Bài 1) đã `MDC.put("correlationId", ...)` TRƯỚC KHI bất kỳ log nào trong request được ghi, MỌI dòng log của request đó TỰ ĐỘNG có field `correlationId` tương ứng, không cần sửa từng câu lệnh log thủ công.
- **`customFields`**: thêm các field TĨNH (cố định cho MỌI dòng log của ứng dụng, VD `application: "library-service"`) — hữu ích khi tổng hợp log TỪ NHIỀU service khác nhau vào CÙNG 1 hệ thống log tập trung (ELK), cho phép LỌC theo tên service dễ dàng.
- **Ứng dụng THỰC TẾ trong Microservices:** khi 1 request đi QUA NHIỀU SERVICE (nhờ Correlation ID được TRUYỀN TIẾP như đã làm ở Bài 1), có thể vào công cụ log tập trung, TÌM KIẾM 1 `correlationId` DUY NHẤT và thấy được **TOÀN BỘ HÀNH TRÌNH** của request đó xuyên qua TẤT CẢ service liên quan — cực kỳ hữu ích khi debug sự cố trong hệ thống phân tán.

---

## Bài 3 — Micrometer: Counter, Timer, Gauge

### Đề
(a) Counter đếm tổng số lần gọi API `checkout`, (b) Timer đo thời gian xử lý, (c) Gauge theo dõi số đơn hàng `PROCESSING`.

### Lời giải

```java
@Service
public class CheckoutService {

    private final Counter checkoutCounter;
    private final Timer checkoutTimer;
    private final AtomicInteger processingOrdersCount = new AtomicInteger(0);

    public CheckoutService(MeterRegistry meterRegistry) {
        // (a) Counter - CHỈ TĂNG, KHÔNG BAO GIỜ GIẢM - đếm TỔNG SỐ LẦN gọi checkout từ lúc ứng dụng khởi động
        this.checkoutCounter = Counter.builder("checkout.requests.total")
                .description("Tổng số lần gọi API checkout")
                .register(meterRegistry);

        // (b) Timer - đo PHÂN BỐ thời gian xử lý (không chỉ trung bình - còn có percentile p50/p95/p99)
        this.checkoutTimer = Timer.builder("checkout.duration")
                .description("Thời gian xử lý checkout")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // (c) Gauge - đo GIÁ TRỊ TẠI THỜI ĐIỂM HIỆN TẠI (có thể TĂNG hoặc GIẢM) - số đơn hàng ĐANG xử lý
        Gauge.builder("orders.processing.count", processingOrdersCount, AtomicInteger::get)
                .description("Số lượng đơn hàng đang ở trạng thái PROCESSING")
                .register(meterRegistry);
    }

    public CheckoutResult checkout(CheckoutRequest request) {
        checkoutCounter.increment();   // TĂNG counter MỖI LẦN method được gọi, KHÔNG PHÂN BIỆT thành công/thất bại

        return checkoutTimer.record(() -> {   // TỰ ĐỘNG đo thời gian THỰC THI của khối lambda bên trong
            processingOrdersCount.incrementAndGet();   // +1 khi BẮT ĐẦU xử lý
            try {
                return processCheckout(request);
            } finally {
                processingOrdersCount.decrementAndGet();   // -1 khi KẾT THÚC xử lý (dù thành công hay lỗi)
            }
        });
    }

    private CheckoutResult processCheckout(CheckoutRequest request) {
        // ...logic nghiệp vụ checkout thật...
        return new CheckoutResult(true, "Đặt hàng thành công");
    }
}
```

### Giải thích

- **Counter vs Gauge — khác biệt CỐT LÕI về Ý NGHĨA dữ liệu:** Counter chỉ **TĂNG DẦN THEO THỜI GIAN** (biểu diễn "tổng số sự kiện đã xảy ra TỪ TRƯỚC ĐẾN GIỜ" — reset về 0 chỉ khi ứng dụng RESTART), phù hợp cho "tổng số request", "tổng số lỗi"; Gauge biểu diễn **GIÁ TRỊ TỨC THỜI, CÓ THỂ LÊN XUỐNG** (VD số đơn hàng ĐANG xử lý — có lúc 5, có lúc 0, có lúc 20 tùy thời điểm quan sát) — dùng SAI loại metric (VD dùng Counter cho "số đơn hàng đang xử lý") sẽ cho biểu đồ VÔ NGHĨA (chỉ tăng mãi, không phản ánh đúng thực tế).
- **`try-finally` quanh `processingOrdersCount.incrementAndGet()`/`decrementAndGet()`**: đảm bảo Gauge LUÔN PHẢN ÁNH ĐÚNG số đơn hàng THỰC SỰ đang xử lý, KỂ CẢ KHI `processCheckout` ném exception — nếu thiếu `finally`, 1 checkout LỖI sẽ khiến counter "processing" bị TĂNG MÃI MÃI KHÔNG BAO GIỜ GIẢM (memory leak về mặt số liệu, dẫn tới Gauge hiển thị SAI LỆCH ngày càng nghiêm trọng theo thời gian).
- **`checkoutTimer.record(() -> {...})`**: cách dùng Timer TIỆN LỢI NHẤT — tự động đo thời gian THỰC THI của toàn bộ khối lambda, không cần tự viết `long start = System.currentTimeMillis()` thủ công — Micrometer tự động tính TOÁN THỐNG KÊ (mean, max, và các percentile đã khai báo).
- **`publishPercentiles(0.5, 0.95, 0.99)`**: cực kỳ quan trọng — CHỈ xem "thời gian TRUNG BÌNH" (mean) có thể GÂY HIỂU LẦM SAI NGHIÊM TRỌNG (VD trung bình 200ms trông "ổn", nhưng nếu p99 = 5000ms nghĩa là **1% người dùng** đang chờ tới **5 GIÂY**, trải nghiệm rất tệ mà con số trung bình che giấu hoàn toàn) — percentile p95/p99 phản ánh TRẢI NGHIỆM CỦA NHÓM NGƯỜI DÙNG CHẬM NHẤT, thường là chỉ số QUAN TRỌNG HƠN NHIỀU so với trung bình khi đánh giá hiệu năng thực tế.

---

## Bài 4 — PromQL query tỷ lệ lỗi 4xx/5xx trong 5 phút

### Đề
Tỷ lệ lỗi (4xx và 5xx) trên tổng request của `payment-service` trong 5 phút gần nhất.

### Lời giải

```promql
sum(rate(http_server_requests_seconds_count{
  application="payment-service",
  status=~"4..|5.."
}[5m]))
/
sum(rate(http_server_requests_seconds_count{
  application="payment-service"
}[5m]))
* 100
```

### Giải thích

- **`rate(...[5m])`**: tính TỐC ĐỘ TĂNG (số request/giây) của 1 Counter TRONG CỬA SỔ THỜI GIAN 5 phút gần nhất — Counter TỰ THÂN chỉ tăng dần (không có ý nghĩa trực tiếp khi đọc giá trị TUYỆT ĐỐI, VD "tổng 45,231,872 request kể từ lúc khởi động" không nói lên ĐANG XẢY RA ĐIỀU GÌ NGAY BÂY GIỜ) — `rate()` chuyển đổi thành "TỐC ĐỘ HIỆN TẠI" (VD "12.5 request/giây trong 5 phút qua"), CÓ Ý NGHĨA để theo dõi xu hướng theo thời gian thực.
- **`status=~"4..|5.."`**: cú pháp regex trong PromQL — `~` nghĩa là "khớp theo regex" (không phải so khớp CHÍNH XÁC `=`); `"4.."` khớp MỌI mã trạng thái BẮT ĐẦU bằng "4" và có ĐÚNG 2 KÝ TỰ THEO SAU (`.` là "bất kỳ ký tự nào" trong regex) — VD khớp `400`, `404`, `429`... ; tương tự `"5.."` khớp `500`, `502`, `503`...; `|` là toán tử "HOẶC" trong regex — gộp lại: khớp BẤT KỲ status code nào thuộc dải 4xx HOẶC 5xx.
- **Chia (tử số = tốc độ lỗi) cho (mẫu số = tốc độ TỔNG request)** rồi nhân `100`: cho ra **TỶ LỆ PHẦN TRĂM lỗi** — đây là metric CHUẨN dùng để định nghĩa/theo dõi **SLI (Service Level Indicator)** cho tỷ lệ thành công của API — trực tiếp liên quan tới khái niệm SLO/Error Budget sẽ tính ở Bài 6.
- **`sum(...)` bao ngoài cả tử số và mẫu số**: cần thiết vì metric `http_server_requests_seconds_count` thường có THÊM các label khác (VD `method`, `uri`, `instance`...) khiến 1 truy vấn PromQL trả về NHIỀU time series (nhiều "dòng" dữ liệu song song, ứng với từng tổ hợp label) — `sum()` GỘP TẤT CẢ các time series đó lại thành **1 CON SỐ DUY NHẤT** (tổng tốc độ lỗi trên TOÀN BỘ endpoint/instance của `payment-service`), phù hợp với yêu cầu "tỷ lệ lỗi CHUNG của service" thay vì tách riêng theo từng endpoint.

---

## Bài 5 — Tình huống chỉ Metrics KHÔNG ĐỦ, cần Tracing + Logs

### Đề
Ví dụ cụ thể — cần kết hợp Distributed Tracing và Logs mới tìm ra nguyên nhân gốc rễ.

### Lời giải — kịch bản cụ thể

```
TÌNH HUỐNG: Dashboard Metrics (Grafana + Prometheus) cho thấy:
  - p99 latency của API "POST /api/v1/orders" (Order Service) TĂNG ĐỘT BIẾN
    từ 200ms lên 4500ms trong khoảng 14:00-14:15
  - Error rate (từ PromQL như Bài 4) VẪN BÌNH THƯỜNG (~0.1%, không có tăng lỗi rõ rệt)
  - CPU/Memory của Order Service instance VẪN BÌNH THƯỜNG (không có dấu hiệu quá tải)

=> CHỈ NHÌN METRICS: biết "CÓ VẤN ĐỀ" (latency cao) NHƯNG HOÀN TOÀN KHÔNG BIẾT "TẠI SAO"
   - Metrics chỉ cho biết SỐ LIỆU TỔNG HỢP (aggregate) - "CÓ BAO NHIÊU request chậm",
     KHÔNG cho biết "CHÍNH XÁC REQUEST NÀO chậm, VÀ CHẬM Ở BƯỚC NÀO bên trong luồng xử lý"

BƯỚC TIẾP THEO - DÙNG DISTRIBUTED TRACING:
   - Vào Jaeger/Zipkin, lọc các TRACE của "POST /api/v1/orders" trong khung 14:00-14:15
     có duration > 4000ms
   - Xem CHI TIẾT 1 trace cụ thể - phát hiện: trace này gồm các SPAN:
       Order Service (tổng 4500ms)
         ├─ [50ms]  validate request
         ├─ [4200ms] gọi HTTP -> Inventory Service (SPAN NÀY CHIẾM GẦN HẾT THỜI GIAN!)
         └─ [250ms] save Order vào DB
   => Đã XÁC ĐỊNH ĐƯỢC "THỦ PHẠM": Inventory Service, KHÔNG PHẢI Order Service tự nó chậm

BƯỚC CUỐI - DÙNG LOGS (đã có Correlation ID xuyên suốt nhờ Bài 1/2):
   - Lấy correlationId của trace vừa tìm được, tra trong hệ thống log tập trung (ELK)
   - Lọc TOÀN BỘ log có correlationId đó, đặc biệt log TỪ Inventory Service
   - Phát hiện dòng log: "WARN: Connection pool exhausted, waiting for available connection..."
     xuất hiện LẶP LẠI NHIỀU LẦN trong khung 14:00-14:15 ở Inventory Service

=> KẾT LUẬN NGUYÊN NHÂN GỐC RỄ: Inventory Service bị CẠN KIỆT CONNECTION POOL tới DB
   (có thể do 1 job batch nội bộ nào đó ĐANG CHIẾM DỤNG quá nhiều connection cùng lúc,
   khiến các request BÌNH THƯỜNG khác phải CHỜ ĐỢI connection rảnh) - hoàn toàn KHÔNG THỂ
   phát hiện được CHỈ TỪ Metrics của Order Service (vì bản thân Order Service không hề lỗi,
   chỉ đang "chờ" 1 service khác) và cũng KHÔNG THỂ xác định CHÍNH XÁC service nào là
   nguyên nhân nếu KHÔNG CÓ Tracing để "nhìn xuyên suốt" toàn bộ chuỗi gọi.
```

### Giải thích

- **3 trụ cột Observability (Metrics, Logs, Traces) bổ trợ CHO NHAU, KHÔNG THAY THẾ NHAU:**
  - **Metrics** trả lời "**CÓ VẤN ĐỀ GÌ ĐANG XẢY RA KHÔNG?**" (phát hiện BẤT THƯỜNG qua con số tổng hợp, dùng để ALERT/cảnh báo tự động) — nhưng CHỈ LÀ SỐ LIỆU TỔNG HỢP, mất đi CHI TIẾT của từng request riêng lẻ.
  - **Distributed Tracing** trả lời "**VẤN ĐỀ ĐÓ NẰM Ở ĐÂU trong CHUỖI GỌI XUYÊN NHIỀU SERVICE?**" — cho thấy BỨC TRANH TOÀN CẢNH của 1 request CỤ THỂ đi qua bao nhiêu service, mỗi bước mất bao lâu — xác định ĐÚNG "thủ phạm" (service nào chậm) trong hệ thống Microservices phức tạp.
  - **Logs** trả lời "**TẠI SAO cụ thể**" — chi tiết THÔNG ĐIỆP/NGỮ CẢNH THỰC SỰ xảy ra BÊN TRONG service đó tại thời điểm cụ thể (exception, warning, business logic detail) mà Metrics/Tracing (chỉ có con số, không có "câu chuyện") không thể cung cấp.
- **Correlation ID (Bài 1/2) chính là "SỢI CHỈ" NỐI LIỀN cả 3 trụ cột này lại với nhau** — nếu KHÔNG có Correlation ID xuyên suốt, việc tìm ĐÚNG log liên quan tới 1 trace cụ thể sẽ CỰC KỲ KHÓ KHĂN (phải dò theo timestamp gần đúng, dễ lẫn với request khác chạy đồng thời) — đây là lý do thiết lập Correlation ID/Tracing NGAY TỪ ĐẦU (không phải khi có sự cố mới thêm vào) là thực hành QUAN TRỌNG của Observability trưởng thành.
- **Bài học kiến trúc rút ra:** đây cũng là minh chứng CỤ THỂ cho vấn đề đã cảnh báo ở Module 20 (Microservices, Bài 5 — Distributed Monolith): 1 service PHỤ THUỘC ĐỒNG BỘ vào service khác (Order → Inventory) có thể khiến SỰ CỐ Ở 1 SERVICE LAN TRUYỀN (cascading) sang service GỌI TỚI NÓ, dù bản thân service gọi hoàn toàn "khỏe mạnh" — càng củng cố lý do cần Circuit Breaker (Module 20, Bài 3) để NGĂN CHẶN sự lan truyền này.

---

## Bài 6 — Tính Error Budget cho SLO 99.95%

### Đề
SLO "99.95% request thành công/tháng". Tính Error Budget (phút downtime/lỗi cho phép) trong 30 ngày. Đề xuất chính sách khi đã tiêu 80% Error Budget trước ngày 20.

### Lời giải — tính toán

```
Tổng thời gian trong 1 tháng 30 ngày:
  30 ngày × 24 giờ × 60 phút = 43,200 phút

SLO = 99.95% thành công  =>  Error Budget = 100% - 99.95% = 0.05% được phép LỖI/DOWNTIME

Error Budget (phút) = 43,200 phút × 0.05% = 43,200 × 0.0005 = 21.6 phút

=> Trong CẢ THÁNG, hệ thống CHỈ ĐƯỢC PHÉP "lỗi/downtime" TỔNG CỘNG TỐI ĐA 21.6 PHÚT
   (tương đương khoảng 21 phút 36 giây) - vượt quá con số này là VI PHẠM SLO đã cam kết.
```

### Lời giải — chính sách khi đã tiêu 80% Error Budget trước ngày 20

```
Tình huống: đã tiêu 80% × 21.6 phút = 17.28 phút Error Budget, NHƯNG MỚI ĐẾN NGÀY 20/30
(tức còn 10 NGÀY NỮA mới hết tháng, mà QUỸ LỖI CHỈ CÒN LẠI 21.6 - 17.28 = 4.32 PHÚT)

=> Tốc độ "đốt" Error Budget ĐANG NHANH HƠN NHIỀU so với tốc độ "an toàn"
   (an toàn: tới ngày 20/30 = 66.7% thời gian tháng thì CHỈ NÊN dùng tối đa ~66.7% budget,
    nhưng THỰC TẾ đã dùng 80% - "cháy" NHANH HƠN kế hoạch ĐÁNG KỂ)

CHÍNH SÁCH HÀNH ĐỘNG ĐỀ XUẤT ("Error Budget Policy" - thực hành chuẩn của SRE):

1. ĐÓNG BĂNG (FREEZE) mọi deploy tính năng MỚI, KHÔNG LIÊN QUAN trực tiếp tới ổn định hệ thống
   - Chỉ cho phép deploy: hotfix khẩn cấp, hoặc thay đổi TRỰC TIẾP nhằm CẢI THIỆN độ ổn định
   - Lý do: mỗi lần deploy đều mang RỦI RO gây thêm lỗi/downtime - khi budget đã cạn,
     KHÔNG THỂ "đánh cược" thêm cho tính năng mới trong khi CHƯA khắc phục nguyên nhân gây lỗi

2. ƯU TIÊN TOÀN BỘ ĐỘI NGŨ ENGINEERING vào việc ĐIỀU TRA VÀ KHẮC PHỤC nguyên nhân gốc rễ
   đã gây ra phần lớn Error Budget bị tiêu hao (dùng Distributed Tracing + Logs như Bài 5
   để xác định CHÍNH XÁC nguyên nhân, không đoán mò)

3. TỔ CHỨC "Postmortem" (họp phân tích sự cố, không đổ lỗi cá nhân - blameless postmortem)
   cho các sự cố ĐÃ GÂY TIÊU HAO Error Budget nhiều nhất trong tháng, rút ra hành động
   cải thiện CỤ THỂ (VD thêm Circuit Breaker ở điểm yếu, tăng cường alerting sớm hơn...)

4. BÁO CÁO tình trạng này LÊN CẤP QUẢN LÝ/STAKEHOLDER liên quan - đây là tín hiệu CẦN
   ĐƯỢC BIẾT SỚM (không đợi tới cuối tháng mới báo cáo "đã vi phạm SLO")

5. NẾU tình trạng "đốt" Error Budget tiếp tục ở tốc độ hiện tại, CẢNH BÁO rằng THÁNG NÀY
   RẤT CÓ THỂ SẼ VI PHẠM SLO đã cam kết - cần chuẩn bị PHƯƠNG ÁN GIẢI TRÌNH với khách hàng/
   đối tác (nếu SLO này được ràng buộc bởi SLA hợp đồng có điều khoản bồi thường)
```

### Giải thích

- **Error Budget là công cụ ĐỊNH LƯỢNG hóa "MỨC ĐỘ RỦI RO CHẤP NHẬN ĐƯỢC"** — thay vì chỉ nói chung chung "hệ thống cần ổn định", SLO 99.95% chuyển hóa thành **1 CON SỐ CỤ THỂ, ĐO ĐƯỢC** (21.6 phút/tháng) — cho phép đội ngũ engineering ra quyết định DỰA TRÊN DỮ LIỆU (data-driven), thay vì cảm tính, về việc "còn được phép mạo hiểm (deploy tính năng mới, thử nghiệm) đến đâu".
- **Ý nghĩa triết lý sâu xa của Error Budget:** KHÔNG PHẢI mục tiêu là "ZERO lỗi tuyệt đối" (điều này thường KHÔNG THỰC TẾ và cực kỳ tốn kém để đạt được, đồng thời làm CHẬM tốc độ phát triển tính năng mới) — mà là chấp nhận 1 MỨC ĐỘ LỖI NHỎ, CÓ KIỂM SOÁT, cho phép team VẪN CÓ THỂ deploy nhanh, thử nghiệm tính năng mới — Error Budget CHƯA CẠN nghĩa là "còn dư địa để mạo hiểm hợp lý"; Error Budget SẮP CẠN là tín hiệu RÕ RÀNG cần **CHUYỂN TRỌNG TÂM từ "tốc độ phát triển tính năng" sang "ổn định hệ thống"** — đây chính là cơ chế CÂN BẰNG tự nhiên giữa 2 mục tiêu thường xung đột (velocity vs. reliability) trong thực hành SRE (Site Reliability Engineering).
- **"Freeze deploy tính năng mới" là chính sách CHUẨN, PHỔ BIẾN NHẤT** khi Error Budget cạn kiệt (Google SRE, nguồn gốc của khái niệm này, cũng áp dụng chính sách tương tự) — logic: MỖI THAY ĐỔI CODE đều có RỦI RO gây lỗi mới (đã học nguyên tắc "thay đổi nhỏ, review kỹ" ở phần Git) — khi budget đã cạn, ưu tiên TUYỆT ĐỐI là NGĂN CHẶN THÊM RỦI RO, không phải tiếp tục "đặt cược" thêm.

---

*Đây là lời giải cho toàn bộ Phần B của Module 30. Tiếp theo: Module 23 — System Design.*
