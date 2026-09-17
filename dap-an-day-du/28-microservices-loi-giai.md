# Lời giải đầy đủ — Module 20: Microservices

> Nguồn đề: `28 microservices/28-microservices.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Sơ đồ Microservices cho "Đặt vé xem phim"

### Đề
`Movie Service`, `Booking Service`, `Payment Service`, `Notification Service` — Database riêng, luồng Sync/Async phù hợp.

### Lời giải

```
┌─────────────────┐         ┌──────────────────┐
│  Movie Service   │         │  Booking Service  │
│  (DB: movie_db)  │◄────────┤  (DB: booking_db) │
└─────────────────┘  SYNC    └──────────────────┘
                      (REST:            │
                    GET /movies/{id}    │ SYNC (REST):
                    kiểm tra phim/suất  │ POST /payments
                    chiếu còn hiệu lực) │ (thu tiền NGAY,
                                        │  cần biết kết quả
                                        │  THÀNH CÔNG/THẤT BẠI
                                        │  để xác nhận vé)
                                        ▼
                              ┌───────────────────┐
                              │  Payment Service   │
                              │  (DB: payment_db)  │
                              └───────────────────┘
                                        │
                                        │ ASYNC (Message Queue:
                                        │ publish "PaymentCompletedEvent")
                                        ▼
                              ┌────────────────────────┐
                              │  Notification Service   │
                              │  (DB: notification_db)  │
                              └────────────────────────┘
                              (gửi email/SMS xác nhận vé -
                               KHÔNG CẦN Booking Service chờ
                               kết quả gửi thành công hay không)
```

### Giải thích

- **Mỗi service SỞ HỮU DATABASE RIÊNG (Database per Service)** — nguyên tắc nền tảng của Microservices: `Booking Service` KHÔNG BAO GIỜ truy vấn trực tiếp vào `payment_db` hay `movie_db` — mọi truy cập dữ liệu XUYÊN SERVICE đều phải đi qua API (REST/message), KHÔNG đi qua JOIN SQL trực tiếp như trong Monolith — đảm bảo mỗi service có thể thay đổi schema nội bộ TỰ DO mà không ảnh hưởng service khác.
- **`Booking Service` → `Payment Service`: SYNCHRONOUS (REST)** — vì luồng nghiệp vụ CẦN BIẾT NGAY kết quả thanh toán THÀNH CÔNG hay THẤT BẠI để quyết định bước tiếp theo (xác nhận vé hay hủy giữ chỗ) — đây là mối quan hệ **PHỤ THUỘC TRỰC TIẾP VÀO KẾT QUẢ**, phù hợp giao tiếp đồng bộ.
- **`Payment Service` → `Notification Service`: ASYNCHRONOUS (Message Queue)** — vì gửi thông báo là tác vụ **PHỤ, KHÔNG QUYẾT ĐỊNH kết quả nghiệp vụ chính** (đặt vé đã hoàn tất, thông báo có gửi chậm/thất bại tạm thời cũng KHÔNG ảnh hưởng tới việc vé đã được xác nhận) — liên hệ trực tiếp nguyên tắc đã phân biệt ở Module 19 (Caching & Messaging, Bài 5): việc PHỤ dùng Async, việc CÓ TÍNH QUYẾT ĐỊNH dùng Sync.
- **`Booking Service` → `Movie Service`: SYNCHRONOUS** — cần xác nhận NGAY suất chiếu còn tồn tại/còn hiệu lực trước khi cho phép đặt vé, đây cũng là dữ liệu bắt buộc phải CÓ trước khi tiếp tục luồng.

---

## Bài 2 — `@FeignClient` cho `InventoryServiceClient`

### Đề
`POST /inventory/reserve` (kiểm tra và trừ tồn kho), dùng trong `OrderService`.

### Lời giải

```java
public record ReserveInventoryRequest(Long productId, int quantity) {}
public record ReserveInventoryResponse(boolean success, String message) {}

@FeignClient(name = "inventory-service", url = "${inventory.service.url}")
public interface InventoryServiceClient {

    @PostMapping("/inventory/reserve")
    ReserveInventoryResponse reserveInventory(@RequestBody ReserveInventoryRequest request);
}
```

```java
@Configuration
@EnableFeignClients   // quét TOÀN BỘ @FeignClient interface trong package, tự động tạo bean implementation
public class FeignConfig {
}
```

```java
@Service
public class OrderService {

    private final InventoryServiceClient inventoryServiceClient;

    public OrderService(InventoryServiceClient inventoryServiceClient) {
        this.inventoryServiceClient = inventoryServiceClient;
    }

    public Order placeOrder(OrderRequest request) {
        ReserveInventoryResponse reserveResult = inventoryServiceClient.reserveInventory(
                new ReserveInventoryRequest(request.productId(), request.quantity()));

        if (!reserveResult.success()) {
            throw new InsufficientStockException(reserveResult.message());
        }

        // ...tiếp tục tạo Order...
        return new Order(request);
    }
}
```

### Giải thích

- **`@FeignClient` là kỹ thuật "Declarative REST Client"** — chỉ cần khai báo INTERFACE (không viết implementation) với annotation giống hệt `@RestController` (`@PostMapping`, `@RequestBody`...) — Spring Cloud OpenFeign TỰ ĐỘNG sinh ra implementation THẬT lúc runtime (dynamic proxy), thực hiện gọi HTTP tới service khác — code gọi service khác trông GIỐNG HỆT như gọi 1 method Java bình thường (`inventoryServiceClient.reserveInventory(...)`), che giấu hoàn toàn độ phức tạp của việc tạo `HttpClient`, serialize/deserialize JSON thủ công.
- **`name = "inventory-service"`**: khi tích hợp với Service Discovery (Eureka/Consul), Feign dùng tên này để TỰ ĐỘNG tra cứu địa chỉ IP:PORT thực tế của 1 trong các instance đang chạy của `inventory-service` (kết hợp Load Balancing) — trong ví dụ đơn giản này dùng thêm `url = "${inventory.service.url}"` để trỏ trực tiếp (phù hợp khi CHƯA có Service Discovery, hoặc môi trường test).
- **`OrderService` hoàn toàn KHÔNG BIẾT** `InventoryServiceClient` thực chất đang gọi HTTP tới 1 service RIÊNG BIỆT — nó chỉ thấy 1 interface Java bình thường được inject qua DI — đây là điểm mạnh của Feign: giảm thiểu "boilerplate code" gọi REST, giữ code nghiệp vụ SẠCH, tập trung vào LOGIC thay vì chi tiết kỹ thuật HTTP.

---

## Bài 3 — `@CircuitBreaker` với `fallbackMethod`

### Đề
`processPayment()` — khi Payment Service down, trả "đơn hàng đang chờ xử lý" thay vì lỗi lan ra ngoài.

### Lời giải

```java
@Service
public class PaymentClientService {

    private final PaymentServiceClient paymentServiceClient;

    public PaymentClientService(PaymentServiceClient paymentServiceClient) {
        this.paymentServiceClient = paymentServiceClient;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public PaymentResult processPayment(PaymentRequest request) {
        // Gọi THẬT tới Payment Service qua Feign/RestTemplate
        return paymentServiceClient.charge(request);
    }

    // Method fallback: PHẢI CÙNG kiểu trả về, và tham số = tham số method gốc + thêm 1 Throwable ở CUỐI
    private PaymentResult processPaymentFallback(PaymentRequest request, Throwable throwable) {
        // Payment Service ĐANG DOWN (hoặc quá chậm, circuit đã OPEN) - KHÔNG để lỗi lan ra ngoài,
        // trả về trạng thái "đang chờ xử lý" - đơn hàng vẫn được TẠO, chỉ đánh dấu thanh toán CHƯA XÁC NHẬN
        return new PaymentResult(false, "PENDING",
                "Hệ thống thanh toán đang tạm thời gián đoạn. Đơn hàng của bạn đã được ghi nhận " +
                "và sẽ được xử lý thanh toán ngay khi hệ thống hoạt động trở lại.");
    }
}
```

```yaml
# application.yml (cấu hình Resilience4j)
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10          # xét 10 lần gọi GẦN NHẤT để tính tỷ lệ lỗi
        failure-rate-threshold: 50        # nếu >= 50% trong 10 lần đó LỖI -> circuit chuyển sang OPEN
        wait-duration-in-open-state: 30s  # ở trạng thái OPEN 30 giây, KHÔNG gọi thật, luôn fallback ngay lập tức
        permitted-number-of-calls-in-half-open-state: 3   # sau 30s, cho phép 3 lần gọi THỬ để kiểm tra đã phục hồi chưa
```

### Giải thích

- **3 trạng thái của Circuit Breaker — mô hình "cầu dao điện":**
  - **CLOSED** (bình thường): mọi lời gọi đi qua BÌNH THƯỜNG, chỉ ÂM THẦM đếm tỷ lệ lỗi.
  - **OPEN** (khi tỷ lệ lỗi vượt ngưỡng): **NGẮT MẠCH NGAY LẬP TỨC** — mọi lời gọi tiếp theo **KHÔNG THỰC SỰ GỌI** Payment Service nữa (tránh "dồn thêm tải" vào 1 service ĐÃ ĐANG GẶP SỰ CỐ, đồng thời tránh THREAD của caller bị TREO chờ timeout lâu) — chuyển THẲNG sang `fallbackMethod` NGAY LẬP TỨC.
  - **HALF_OPEN** (sau `wait-duration-in-open-state`): cho phép 1 SỐ ÍT lời gọi THỬ NGHIỆM đi qua thật — nếu thành công, quay lại CLOSED; nếu vẫn lỗi, quay lại OPEN, đợi thêm 1 chu kỳ.
- **`fallbackMethod` PHẢI khớp CHÍNH XÁC signature (cùng kiểu trả về, tham số giống hệt method gốc CỘNG THÊM 1 `Throwable` ở cuối)** — nếu không khớp, Resilience4j sẽ ném lỗi cấu hình lúc khởi động (`NoSuchMethodException`), không phát hiện được cho tới khi chạy thật.
- **Ý nghĩa chiến lược của fallback "đang chờ xử lý" (thay vì trả lỗi 500 trực tiếp):** đây là ứng dụng thực tế của nguyên tắc **Graceful Degradation** (suy giảm dịch vụ có kiểm soát) — thay vì để LỖI TOÀN BỘ (đơn hàng thất bại hoàn toàn) khi CHỈ 1 phần hệ thống (Payment Service) gặp sự cố, hệ thống VẪN GHI NHẬN đơn hàng, chỉ đánh dấu trạng thái đặc biệt — người dùng nhận trải nghiệm TỐT HƠN NHIỀU so với lỗi 500 khó hiểu, và nghiệp vụ có thể tự động RETRY xử lý thanh toán sau khi Payment Service phục hồi (kết hợp Outbox Pattern đã học ở Module 19, Bài 6).
- **Circuit Breaker giải quyết vấn đề gì mà chỉ Timeout đơn thuần KHÔNG giải quyết được:** nếu chỉ set timeout (VD 5 giây) cho MỖI lời gọi, khi Payment Service down, MỖI request VẪN PHẢI CHỜ ĐỦ 5 giây rồi mới timeout — với hàng nghìn request đồng thời, threads của caller (`OrderService`) có thể bị CHIẾM DỤNG HẾT chỉ để... chờ timeout — Circuit Breaker (ở trạng thái OPEN) **BỎ QUA HOÀN TOÀN việc gọi thật và chờ timeout**, trả fallback GẦN NHƯ TỨC THÌ, bảo vệ tài nguyên (thread pool) của chính `OrderService` khỏi bị "lây lan sự cố" từ Payment Service (Cascading Failure).

---

## Bài 4 — Saga Orchestration cho "Đặt vé xem phim"

### Đề
Booking Service giữ chỗ → Payment Service thu tiền → nếu thất bại, Compensating Transaction hủy giữ chỗ. Pseudo-code.

### Lời giải

```java
@Service
public class BookingSagaOrchestrator {

    private final BookingServiceClient bookingServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    public BookingSagaOrchestrator(BookingServiceClient bookingServiceClient, PaymentServiceClient paymentServiceClient) {
        this.bookingServiceClient = bookingServiceClient;
        this.paymentServiceClient = paymentServiceClient;
    }

    public BookingResult executeBookingSaga(BookingRequest request) {
        // ===== BƯỚC 1: Booking Service GIỮ CHỖ GHẾ (LOCAL TRANSACTION của Booking Service) =====
        SeatReservationResult reservation;
        try {
            reservation = bookingServiceClient.reserveSeat(request.movieId(), request.seatId());
        } catch (Exception ex) {
            // Bước 1 thất bại ngay từ đầu - KHÔNG CÓ gì cần compensate, dừng saga tại đây
            return BookingResult.failed("Không thể giữ chỗ ghế: " + ex.getMessage());
        }

        // ===== BƯỚC 2: Payment Service THU TIỀN (LOCAL TRANSACTION của Payment Service) =====
        try {
            PaymentResult payment = paymentServiceClient.charge(request.customerId(), request.amount());

            if (!payment.success()) {
                // Bước 2 THẤT BẠI VỀ NGHIỆP VỤ (VD: không đủ tiền) -> COMPENSATING TRANSACTION cho Bước 1
                compensateReservation(reservation.reservationId());
                return BookingResult.failed("Thanh toán thất bại: " + payment.message());
            }

            // ===== CẢ 2 BƯỚC ĐỀU THÀNH CÔNG - xác nhận vé =====
            bookingServiceClient.confirmBooking(reservation.reservationId());
            return BookingResult.success(reservation.reservationId());

        } catch (Exception ex) {
            // Bước 2 THẤT BẠI VỀ MẶT KỸ THUẬT (Payment Service down/timeout) -> CŨNG PHẢI compensate Bước 1
            compensateReservation(reservation.reservationId());
            return BookingResult.failed("Lỗi hệ thống thanh toán: " + ex.getMessage());
        }
    }

    // COMPENSATING TRANSACTION: "hành động NGƯỢC" để hủy tác dụng của Bước 1 đã thành công trước đó
    private void compensateReservation(String reservationId) {
        try {
            bookingServiceClient.cancelReservation(reservationId);   // GIẢI PHÓNG lại ghế đã giữ
        } catch (Exception ex) {
            // Compensate THẤT BẠI là tình huống NGHIÊM TRỌNG - cần cơ chế retry riêng/cảnh báo vận hành (không đi sâu ở đây)
            log.error("KHÔNG THỂ compensate reservation {} - cần can thiệp thủ công!", reservationId, ex);
        }
    }
}
```

### Giải thích

- **Orchestration (khác Choreography):** có 1 "nhạc trưởng" (`BookingSagaOrchestrator`) **BIẾT VÀ ĐIỀU KHIỂN TRỰC TIẾP TOÀN BỘ TRÌNH TỰ CÁC BƯỚC** của saga (gọi Booking Service, gọi Payment Service, quyết định compensate hay không) — khác với **Choreography** (không có nhạc trưởng, mỗi service TỰ LẮNG NGHE event và TỰ QUYẾT ĐỊNH hành động tiếp theo, như luồng Event-driven đã làm ở Module 13, Bài 6) — Orchestration phù hợp khi luồng nghiệp vụ có **NHIỀU BƯỚC, LOGIC ĐIỀU KIỆN PHỨC TẠP**, dễ THEO DÕI và DEBUG hơn (nhìn vào 1 class là biết toàn bộ luồng), nhưng đánh đổi bằng việc `BookingSagaOrchestrator` biết QUÁ NHIỀU về các service khác (coupling cao hơn Choreography).
- **Compensating Transaction KHÔNG PHẢI "ROLLBACK" theo nghĩa DB transaction thông thường** — vì Bước 1 (giữ chỗ) đã **COMMIT THẬT** ở Booking Service (transaction cục bộ của riêng nó đã hoàn tất) — "hủy" ở đây là 1 **HÀNH ĐỘNG NGHIỆP VỤ MỚI, NGƯỢC LẠI VỀ MẶT Ý NGHĩA** (`cancelReservation`), không phải cơ chế `ROLLBACK` tự động của DB — đây chính là lý do các bước trong Saga được gọi là "Local Transaction" (transaction CỤC BỘ, độc lập ở TỪNG service) thay vì 1 Distributed Transaction bao trùm toàn bộ (mà Microservices vốn KHÔNG hỗ trợ hiệu quả, khác Monolith).
- **Không có khái niệm "ACID xuyên suốt toàn bộ Saga"** — chỉ có tính chất **"Eventual Consistency"** (nhất quán CUỐI CÙNG, sau khi mọi bước — kể cả compensate — đã hoàn tất) — trong khoảng thời gian NGẮN giữa Bước 1 và việc compensate (nếu Bước 2 thất bại), hệ thống ở trạng thái "TẠM THỜI BẤT NHẤT" (ghế đang bị giữ dù cuối cùng sẽ được giải phóng) — đây là đánh đổi CỐ HỮU của kiến trúc Microservices, khác biệt căn bản so với ACID transaction đơn giản trong Monolith.
- **Compensate CÓ THỂ THẤT BẠI** (ghi log ở ví dụ trên) — đây là vấn đề THỰC TẾ QUAN TRỌNG cần lưu ý: hệ thống production-grade cần thêm cơ chế **RETRY tự động** cho compensating transaction (tương tự `OutboxPoller` đã học ở Module 19, Bài 6) hoặc **DLQ (Dead Letter Queue)** + cảnh báo vận hành để đảm bảo không có "ghế bị giữ MÃI MÃI" do compensate thất bại mà không ai biết.

---

## Bài 5 — Anti-pattern "Distributed Monolith"

### Đề
Ví dụ cụ thể hệ thống rơi vào Distributed Monolith. Đề xuất tái cấu trúc.

### Lời giải — ví dụ cụ thể

```
Tình huống: hệ thống "Đặt vé xem phim" (Bài 1) NHƯNG được implement SAI như sau:

Booking Service.confirmBooking():
    1. gọi ĐỒNG BỘ (SYNC) -> Movie Service (kiểm tra phim)
    2. gọi ĐỒNG BỘ (SYNC) -> Payment Service (thu tiền)
    3. gọi ĐỒNG BỘ (SYNC) -> Notification Service (gửi email) và CHỜ NÓ TRẢ VỀ THÀNH CÔNG
       (dù bản chất gửi email KHÔNG QUYẾT ĐỊNH kết quả đặt vé - đã phân tích SAI ở Bài 1)
    4. gọi ĐỒNG BỘ (SYNC) -> User Service (lấy thông tin user để cá nhân hóa email)
    5. Notification Service, để gửi email, LẠI GỌI TIẾP ĐỒNG BỘ -> Movie Service
       (để lấy tên phim hiển thị trong email, thay vì NHẬN SẴN trong event payload)

=> Kết quả: 1 request đặt vé của user giờ đây kích hoạt CHUỖI GỌI ĐỒNG BỘ DÀI:
   Booking -> Movie -> Payment -> Notification -> User -> Notification -> Movie (LẶP LẠI!)

=> Đặc trưng của Distributed Monolith:
   - MỌI service đều phải "SỐNG" (available) THÌ 1 request MỚI hoàn thành được -
     giống HỆT Monolith (1 lỗi nhỏ ở bất kỳ đâu = TOÀN BỘ luồng thất bại),
     nhưng giờ đây CÒN TỆ HƠN vì có thêm ĐỘ TRỄ MẠNG (network latency) ở MỖI lần gọi
   - Độ trễ (latency) CỘNG DỒN qua TỪNG service trong chuỗi (tổng thời gian phản hồi = tổng của TẤT CẢ)
   - KHÔNG THỂ deploy/scale ĐỘC LẬP từng service - vì service nào cũng "kéo theo" nhiều service khác
```

### Lời giải — đề xuất tái cấu trúc

```
1. CHUYỂN các lời gọi KHÔNG QUYẾT ĐỊNH kết quả chính (Notification) sang ASYNC (message queue)
   -> Booking Service publish "BookingConfirmedEvent" (chứa SẴN đủ dữ liệu cần thiết:
      tên phim, tên user, suất chiếu...) - Notification Service TỰ LẤY dữ liệu từ EVENT PAYLOAD,
      KHÔNG CẦN gọi ngược lại Movie Service/User Service nữa

2. GIẢM SỐ LẦN GỌI ĐỒNG BỘ CẦN THIẾT bằng cách NHÚNG SẴN dữ liệu cần thiết vào event/response
   (denormalization có chủ đích - tương tự nguyên tắc đã bàn ở Module 11, Bài 2 - MongoDB embed)
   thay vì để mỗi service TỰ GỌI NGƯỢC LẠI để lấy thêm thông tin

3. CHỈ GIỮ LẠI đồng bộ cho các bước THỰC SỰ CẦN BIẾT KẾT QUẢ NGAY để quyết định luồng tiếp theo
   (Booking -> Payment: BẮT BUỘC sync vì cần biết thanh toán có thành công không)

Kết quả sau tái cấu trúc:
   Booking Service:
     1. SYNC -> Movie Service (BẮT BUỘC - cần biết phim/suất còn hiệu lực TRƯỚC khi giữ chỗ)
     2. SYNC -> Payment Service (BẮT BUỘC - cần biết kết quả thanh toán)
     3. ASYNC -> publish "BookingConfirmedEvent" (chứa đủ dữ liệu) -> Notification Service TỰ xử lý,
        KHÔNG có gọi ngược lại nào nữa
```

### Giải thích

- **Bản chất Distributed Monolith:** hệ thống ĐÃ được TÁCH thành nhiều service riêng biệt (về mặt CODEBASE, DEPLOYMENT), nhưng vẫn **GIAO TIẾP VỚI NHAU THEO KIỂU ĐỒNG BỘ, CHẶT CHẼ Y HỆT MONOLITH** — dẫn tới việc **MẤT ĐI TOÀN BỘ LỢI ÍCH của Microservices** (deploy độc lập, scale độc lập, fault isolation) trong khi vẫn phải **GÁNH CHỊU TOÀN BỘ NHƯỢC ĐIỂM** (độ trễ mạng, độ phức tạp vận hành, cần quản lý nhiều service riêng) — đây là kết quả TỆ HẠI NHẤT có thể có khi tách Microservices SAI CÁCH.
- **Nguyên tắc cốt lõi để tránh:** thiết kế giao tiếp giữa các service theo **"chỉ đồng bộ khi THỰC SỰ CẦN BIẾT KẾT QUẢ NGAY để quyết định bước tiếp theo"** — mọi trường hợp khác nên ưu tiên **ASYNC (event-driven)** kết hợp **NHÚNG ĐỦ DỮ LIỆU CẦN THIẾT** vào payload của event (chấp nhận 1 chút "denormalization"/trùng lặp dữ liệu để đổi lấy việc GIẢM SỐ LẦN GỌI XUYÊN SERVICE) — đây chính là nguyên tắc thiết kế API/event đã áp dụng nhất quán xuyên suốt Module 19 (Caching & Messaging).

---

## Bài 6 — Strangler Fig Pattern tách Monolith Thương mại điện tử

### Đề
Domain: `User`, `Product`, `Order`, `Review`, `Notification`. Thứ tự tách + lý do. Vai trò Gateway/Proxy.

### Lời giải — thứ tự đề xuất tách

```
THỨ TỰ 1: Notification Service (TÁCH ĐẦU TIÊN)
  Lý do: ÍT PHỤ THUỘC NHẤT về dữ liệu (chỉ cần nhận thông tin QUA EVENT/tham số, không cần
  truy vấn ngược lại nhiều bảng khác của Monolith) - RỦI RO THẤP NHẤT nếu tách sai,
  vì thất bại của Notification KHÔNG ảnh hưởng nghiêm trọng tới nghiệp vụ CHÍNH (mua bán) -
  PHÙ HỢP làm "bài học kinh nghiệm đầu tiên" cho team về vận hành Microservices thực tế.

THỨ TỰ 2: Review Service
  Lý do: Domain TƯƠNG ĐỐI ĐỘC LẬP về nghiệp vụ (đánh giá sản phẩm) - CHỦ YẾU là ĐỌC/GHI dữ liệu
  RIÊNG của nó (review, rating), chỉ cần THAM CHIẾU (không JOIN sâu) tới Product/User qua ID -
  rủi ro thấp, và có thể coi là "bước đệm" tiếp theo sau khi đã có kinh nghiệm với Notification.

THỨ TỰ 3: Product Service
  Lý do: Phức tạp hơn (nhiều nghiệp vụ liên quan: catalog, tồn kho, giá) nhưng vẫn tương đối
  TÁCH BIỆT khỏi User/Order về mặt dữ liệu cốt lõi - tách TRƯỚC Order vì Order sẽ CẦN Product Service
  đã sẵn sàng để gọi tới (Order phụ thuộc VÀO Product, không phải ngược lại).

THỨ TỰ 4: User Service
  Lý do: NHIỀU domain khác (Order, Review) đều cần tham chiếu tới User (authorId, customerId...) -
  tách User TRƯỚC Order để đảm bảo khi tách Order, đã có sẵn 1 nguồn THAM CHIẾU NHẤT QUÁN
  cho danh tính người dùng (authentication/authorization tập trung).

THỨ TỰ 5: Order Service (TÁCH CUỐI CÙNG, PHỨC TẠP NHẤT)
  Lý do: PHỤ THUỘC VÀO CẢ User, Product (và gián tiếp cả Payment nếu có) - đây là domain
  CỐT LÕI NHẤT, THAY ĐỔI THƯỜNG XUYÊN NHẤT, và có NHIỀU LUỒNG NGHIỆP VỤ PHỨC TẠP (Saga,
  Outbox Pattern...) - nên tách SAU CÙNG khi team ĐÃ CÓ ĐỦ KINH NGHIỆM vận hành Microservices
  từ 4 domain trước đó, giảm thiểu rủi ro cho phần quan trọng nhất của hệ thống.
```

### Vai trò của Gateway/Proxy trong suốt quá trình

```
                         ┌─────────────────────┐
   Client (Frontend) ───►│   API Gateway/Proxy   │
                         └─────────────────────┘
                              │           │
                    (routing dựa trên     │
                     path/domain)         │
                              │           │
                    ┌─────────┘           └──────────┐
                    ▼                                 ▼
         ┌─────────────────────┐          ┌───────────────────────┐
         │  Notification Service │          │   MONOLITH (còn lại:   │
         │  (ĐÃ TÁCH)            │          │   User, Product, Order,│
         └─────────────────────┘          │   Review - CHƯA TÁCH)   │
                                            └───────────────────────┘

Theo thời gian, Gateway DẦN DẦN chuyển hướng (route) NHIỀU HƠN traffic sang các
Microservices MỚI TÁCH, phần MONOLITH "co lại" (strangled) dần - ĐÚNG NGHĨA ĐEN
của tên gọi "Strangler Fig" (cây đa bóp nghẹt - mọc bao quanh và dần thay thế cây chủ).
```

### Giải thích

- **Gateway/Proxy đóng vai trò "TẤM CHẮN" (facade) DUY NHẤT MÀ CLIENT NHÌN THẤY** — client (frontend/mobile app) LUÔN gọi tới CÙNG 1 địa chỉ Gateway, KHÔNG BAO GIỜ biết (và không cần biết) request của nó thực sự đang được xử lý bởi Monolith CŨ hay Microservice MỚI đã tách — đây là điều kiện TIÊN QUYẾT để có thể tách dần dần **MÀ KHÔNG CẦN "BIG BANG" MIGRATION** (ngừng toàn bộ hệ thống, viết lại 1 lần, chuyển đổi đột ngột — rủi ro cực cao cho hệ thống ĐANG CHẠY PRODUCTION).
- **Cơ chế Routing của Gateway thay đổi DẦN DẦN theo tiến độ:** ban đầu, MỌI request (kể cả `/api/notifications/**`) đều route tới Monolith; SAU KHI tách xong `Notification Service`, Gateway CHỈ CẦN đổi 1 rule routing (`/api/notifications/** -> Notification Service` thay vì `-> Monolith`) — Monolith VẪN TIẾP TỤC PHỤC VỤ các domain CHƯA TÁCH BÌNH THƯỜNG, KHÔNG CẦN "dừng" hay "viết lại toàn bộ" ngay lập tức.
- **Nguyên tắc chọn thứ tự tách: "từ ít rủi ro/ít phụ thuộc tới nhiều rủi ro/nhiều phụ thuộc"** — không có 1 công thức "đúng tuyệt đối" cho MỌI hệ thống, nhưng nguyên tắc chung được khuyến nghị rộng rãi: tách trước các domain **NGOẠI VI (ít bị domain khác phụ thuộc VÀO nó)**, để lại domain **TRUNG TÂM, PHỨC TẠP NHẤT** (ở đây là `Order`, vì phụ thuộc VÀO gần như mọi domain khác) tách SAU CÙNG, khi đã tích lũy đủ kinh nghiệm và có sẵn hạ tầng vận hành Microservices ỔN ĐỊNH (monitoring, distributed tracing — Module 21).
- **Lợi ích cốt lõi của Strangler Fig so với "Big Bang Rewrite":** hệ thống VẪN CHẠY ĐƯỢC và PHỤC VỤ NGƯỜI DÙNG THẬT trong SUỐT quá trình chuyển đổi — mỗi lần tách 1 domain là 1 THAY ĐỔI NHỎ, CÓ THỂ ĐO LƯỜNG VÀ ROLLBACK RIÊNG (chỉ cần đổi routing rule ở Gateway quay lại Monolith nếu Service mới có vấn đề) — giảm thiểu RỦI RO NGHIÊM TRỌNG so với việc viết lại TOÀN BỘ hệ thống rồi "cutover" một lần duy nhất.

---

*Đây là lời giải cho toàn bộ Phần B của Module 28. Tiếp theo: Module 21 — DevOps cơ bản.*
