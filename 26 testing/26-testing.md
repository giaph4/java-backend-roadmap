# Module 17 — Testing

> **Mức ưu tiên: 🔴 Cao**
> **Vì sao quan trọng:** Code không có test là code không ai dám sửa — mỗi lần thay đổi đều lo sợ "phá vỡ cái gì đó". Testing không chỉ là "viết thêm vài file cho có" — nó là kỹ năng thiết kế code dễ test (liên hệ trực tiếp Constructor Injection ở Module 12), là "lưới an toàn" khi refactor, và là tiêu chuẩn bắt buộc trong quy trình CI/CD chuyên nghiệp. Đây cũng là phần **code review soi kỹ nhất** — 1 PR không có test đi kèm thường bị từ chối ngay ở các công ty có văn hóa engineering tốt.

> **Phạm vi bài này:** Tập trung vào kỹ thuật viết và tổ chức test trong 1 ứng dụng Spring Boot (Unit/Integration/Slice Test, Mockito, Testcontainers). Không đi sâu vào CI/CD pipeline configuration (Jenkins/GitHub Actions cụ thể — thuộc phạm vi DevOps), hay Performance/Load Testing (JMeter/Gatling — công cụ khác, mục tiêu khác với Functional Testing ở đây).

---

## Mục lục

1. [Test Pyramid — chiến lược testing tổng thể](#1-test-pyramid)
2. [JUnit 5 — nền tảng cơ bản](#2-junit-5)
3. [Assertions & AssertJ](#3-assertions--assertj)
4. [Mockito — giả lập dependency](#4-mockito)
5. [Unit Test cho Service Layer](#5-unit-test-cho-service-layer)
6. [@SpringBootTest — Integration Test](#6-springboottest)
7. [MockMvc — test Controller layer](#7-mockmvc)
8. [Testing Spring Security — @WithMockUser](#8-testing-spring-security)
9. [@DataJpaTest — test Repository layer](#9-datajputest)
10. [Testcontainers — test với DB thật trong Docker](#10-testcontainers)
11. [Test Coverage & các chỉ số liên quan](#11-test-coverage)
12. [Mutation Testing — đo lường THẬT chất lượng test](#12-mutation-testing)
13. [⚠️ Các bẫy hay gặp](#13-các-bẫy-hay-gặp)
14. [Tổng kết — Bảng ghi nhớ nhanh](#14-tổng-kết--bảng-ghi-nhớ-nhanh)
15. [Bài tập luyện tập](#15-bài-tập-luyện-tập)

---

## 1. Test Pyramid

**Test Pyramid** là mô hình phân bổ **số lượng và loại test** hợp lý trong 1 dự án — nguyên tắc: **càng lên cao, số lượng càng ít** (vì chi phí viết/chạy/bảo trì càng cao).

```
              ▲
             /E2E\          <- Rất ít (chậm, đắt, dễ vỡ - "flaky")
            /------\           Test toàn bộ luồng qua UI thật
           /Integration\    <- Vừa phải
          /--------------\     Test nhiều tầng cùng lúc (Controller+Service+DB)
         /   Unit Test     \ <- NHIỀU NHẤT (nhanh, rẻ, ổn định)
        /--------------------\  Test 1 class/method độc lập, mock hết dependency
       ──────────────────────
```

| Loại Test | Tốc độ | Số lượng khuyến nghị | Phạm vi kiểm tra |
|---|---|---|---|
| **Unit Test** | Rất nhanh (mili-giây) | **Nhiều nhất** (70%) | 1 class/method, mock mọi dependency ngoài |
| **Integration Test** | Trung bình (giây) | Vừa phải (20%) | Nhiều tầng phối hợp (Service+Repository+DB thật) |
| **E2E Test** | Chậm (nhiều giây - phút) | Ít nhất (10%) | Toàn bộ hệ thống qua giao diện/API thật |

### Vì sao Unit Test nên chiếm đa số?

- **Nhanh:** Chạy hàng nghìn Unit Test trong vài giây, phù hợp chạy liên tục khi code (TDD) hoặc trong CI pipeline
- **Ổn định (không "flaky"):** Không phụ thuộc network, DB thật, timing — kết quả luôn nhất quán
- **Dễ định vị lỗi:** Test fail chỉ ra chính xác class/method nào có vấn đề, không cần đoán mò qua nhiều tầng

⚠️ **Anti-pattern "Ice Cream Cone"** (ngược Test Pyramid — nhiều E2E, ít Unit Test) là dấu hiệu dự án đang gặp vấn đề: test chạy chậm, hay fail ngẫu nhiên (flaky), khó xác định nguyên nhân lỗi, làm chậm cả team.

### TDD (Test-Driven Development) — viết test TRƯỚC khi viết code

Chu trình **Red-Green-Refactor**: viết test trước (fail vì chưa có code — Red), viết code tối thiểu để test pass (Green), rồi dọn dẹp code mà không phá test (Refactor):

```
1. RED     - Viết test cho hành vi CHƯA TỒN TẠI -> chạy test -> FAIL (vì method chưa có/logic sai)
2. GREEN   - Viết code TỐI THIỂU để test pass -> chạy lại -> PASS
3. REFACTOR - Cải thiện code (đặt tên rõ hơn, tách method, xóa trùng lặp)
              trong khi test vẫn PASS -> đảm bảo không phá vỡ hành vi đã đúng
4. Lặp lại cho hành vi tiếp theo
```

> **Thực tế:** TDD không bắt buộc áp dụng cho mọi dòng code, nhưng là kỹ thuật hiệu quả cho logic nghiệp vụ phức tạp (tính toán, validation nhiều rule) — buộc bạn nghĩ rõ **input/output mong đợi** trước khi viết implementation, thường dẫn tới thiết kế code dễ test hơn (ít coupling, single responsibility) một cách tự nhiên.

---

## 2. JUnit 5 — nền tảng cơ bản

### Cấu trúc 1 test class

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    private Calculator calculator;

    @BeforeEach // Chạy TRƯỚC MỖI test method - đảm bảo mỗi test có state sạch, độc lập
    void setUp() {
        calculator = new Calculator();
    }

    @AfterEach // Chạy SAU MỖI test method - dọn dẹp tài nguyên nếu cần
    void tearDown() {
        // VD: đóng connection, xóa file tạm...
    }

    @BeforeAll // Chạy 1 LẦN DUY NHẤT trước TOÀN BỘ test trong class - PHẢI là static
    static void setUpClass() {
        System.out.println("Bắt đầu chạy CalculatorTest");
    }

    @Test
    @DisplayName("Cộng 2 số dương phải trả về tổng đúng")
    void add_TwoPositiveNumbers_ReturnsSum() {
        int result = calculator.add(2, 3);
        assertEquals(5, result);
    }

    @Test
    void divide_ByZero_ThrowsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> calculator.divide(10, 0));
    }

    @Disabled("Chưa implement tính năng này") // Tạm bỏ qua test, KÈM lý do rõ ràng
    @Test
    void multiply_LargeNumbers_HandlesOverflow() { }
}
```

### Quy ước đặt tên test method (khuyến nghị)

```java
// Pattern phổ biến: methodName_condition_expectedResult
void withdraw_InsufficientBalance_ThrowsException() { }
void createUser_DuplicateEmail_ThrowsConflictException() { }
void findById_NonExistentId_ReturnsEmptyOptional() { }
```

### Parameterized Test — chạy cùng 1 logic test với nhiều bộ dữ liệu

```java
@ParameterizedTest
@ValueSource(ints = {2, 4, 6, 8, 10})
void isEven_EvenNumbers_ReturnsTrue(int number) {
    assertTrue(NumberUtils.isEven(number));
}

@ParameterizedTest
@CsvSource({
    "2, 3, 5",
    "10, -5, 5",
    "0, 0, 0"
})
void add_VariousInputs_ReturnsCorrectSum(int a, int b, int expected) {
    assertEquals(expected, calculator.add(a, b));
}
```

**Lợi ích:** Tránh viết N test method gần giống hệt nhau chỉ khác input/expected — 1 method duy nhất chạy được nhiều bộ dữ liệu, dễ maintain, dễ đọc.

### @MethodSource & @EnumSource — khi dữ liệu test phức tạp hơn giá trị đơn giản

`@ValueSource`/`@CsvSource` chỉ phù hợp với kiểu dữ liệu cơ bản (số, chuỗi). Khi cần truyền **object phức tạp** làm tham số test, dùng `@MethodSource` để trỏ tới 1 method cung cấp dữ liệu:

```java
@ParameterizedTest
@MethodSource("provideOrdersForDiscountTest")
void calculateDiscount_VariousOrderTotals_ReturnsCorrectDiscount(Order order, BigDecimal expectedDiscount) {
    BigDecimal discount = discountService.calculate(order);
    assertThat(discount).isEqualByComparingTo(expectedDiscount);
}

// Method PHẢI static, trả về Stream/Collection của Arguments
private static Stream<Arguments> provideOrdersForDiscountTest() {
    return Stream.of(
        Arguments.of(new Order(BigDecimal.valueOf(100_000)), BigDecimal.ZERO),          // Dưới ngưỡng - không giảm giá
        Arguments.of(new Order(BigDecimal.valueOf(500_000)), BigDecimal.valueOf(25_000)), // Đạt ngưỡng 5%
        Arguments.of(new Order(BigDecimal.valueOf(1_000_000)), BigDecimal.valueOf(100_000)) // Đạt ngưỡng 10%
    );
}
```

`@EnumSource` tiện lợi khi cần chạy test cho **tất cả (hoặc 1 phần) giá trị của 1 enum** — hữu ích để đảm bảo logic xử lý đúng cho mọi trạng thái, tránh bỏ sót case khi enum có thêm giá trị mới sau này:

```java
@ParameterizedTest
@EnumSource(OrderStatus.class) // Tự động chạy test cho TẤT CẢ giá trị enum OrderStatus
void getStatusLabel_AllStatuses_ReturnsNonEmptyLabel(OrderStatus status) {
    assertThat(orderService.getStatusLabel(status)).isNotBlank();
    // Nếu sau này thêm 1 giá trị OrderStatus mới mà quên xử lý trong getStatusLabel(),
    // test này TỰ ĐỘNG chạy thêm case đó và có thể phát hiện lỗi ngay
}

@ParameterizedTest
@EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED"}) // Chỉ chạy với 2 giá trị được chọn
void canCancel_PendingOrConfirmedStatus_ReturnsTrue(OrderStatus status) {
    assertThat(orderService.canCancel(status)).isTrue();
}
```

### Test Lifecycle & thứ tự chạy

```
@BeforeAll (1 lần)
   │
   ├─ @BeforeEach -> Test method 1 -> @AfterEach
   ├─ @BeforeEach -> Test method 2 -> @AfterEach
   └─ @BeforeEach -> Test method 3 -> @AfterEach
   │
@AfterAll (1 lần)
```

⚠️ **Mặc định, JUnit 5 tạo 1 instance MỚI cho mỗi test method** (`PER_METHOD` lifecycle) — đây là lý do `@BeforeAll`/`@AfterAll` phải `static` (không có instance cố định để gọi non-static method), và cũng là lý do mỗi test hoàn toàn độc lập, không chia sẻ state qua lại.

---

## 3. Assertions & AssertJ

### JUnit 5 Assertions cơ bản

```java
assertEquals(expected, actual);
assertNotEquals(expected, actual);
assertTrue(condition);
assertFalse(condition);
assertNull(value);
assertNotNull(value);
assertThrows(SomeException.class, () -> someMethod());
assertAll(  // Chạy TẤT CẢ assertion, kể cả cái đầu fail - báo cáo đầy đủ mọi lỗi thay vì dừng ở lỗi đầu tiên
    () -> assertEquals(1, result.getId()),
    () -> assertEquals("Pho", result.getName())
);
```

### AssertJ — thư viện assertion "fluent" (đọc như câu văn tự nhiên, khuyến nghị dùng)

```java
import static org.assertj.core.api.Assertions.assertThat;

@Test
void createUser_ValidInput_ReturnsUserWithCorrectFields() {
    User user = userService.createUser("Pho", "pho@example.com");

    // Chuỗi assertion đọc tự nhiên như tiếng Anh, dễ hiểu ngay cả người không rành code
    assertThat(user.getFullName()).isEqualTo("Pho");
    assertThat(user.getEmail()).isEqualTo("pho@example.com");
    assertThat(user.getId()).isNotNull();
    assertThat(user.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
}

@Test
void getActiveUsers_ReturnsCorrectList() {
    List<User> users = userService.getActiveUsers();

    assertThat(users)
        .hasSize(3)
        .extracting(User::getEmail) // Trích xuất 1 field từ mỗi phần tử để so sánh
        .containsExactlyInAnyOrder("a@example.com", "b@example.com", "c@example.com");
}

@Test
void divide_ByZero_ThrowsException() {
    assertThatThrownBy(() -> calculator.divide(10, 0))
        .isInstanceOf(ArithmeticException.class)
        .hasMessage("/ by zero");
}
```

**Vì sao AssertJ được khuyến nghị hơn Assertions thuần của JUnit:**
- Cú pháp fluent, dễ đọc hơn nhiều: `assertThat(x).isEqualTo(y)` tự nhiên hơn `assertEquals(y, x)` (thứ tự tham số của `assertEquals` cũng hay gây nhầm lẫn — expected trước, actual sau)
- Có sẵn rất nhiều assertion chuyên biệt cho Collection, String, Optional, Exception... giúp code test ngắn gọn hơn
- Thông báo lỗi khi fail rõ ràng, dễ debug hơn

---

## 4. Mockito

**Mockito** là thư viện tạo **Mock Object** (đối tượng giả lập) — thay thế dependency thật bằng 1 "diễn viên đóng thế" có hành vi được lập trình sẵn, giúp Unit Test **cô lập hoàn toàn** class đang test khỏi các dependency bên ngoài (database, API bên thứ 3, service khác...).

### Test Doubles — 5 loại "diễn viên đóng thế", không chỉ có Mock

"Mock" trong lời nói hàng ngày thường dùng để chỉ chung mọi loại đối tượng giả lập, nhưng thuật ngữ kiểm thử chuẩn (theo Martin Fowler) phân biệt **5 loại Test Double** riêng biệt — hiểu đúng giúp chọn công cụ phù hợp và trả lời chính xác trong phỏng vấn:

| Loại | Đặc điểm | Ví dụ trong Mockito |
|---|---|---|
| **Dummy** | Object chỉ để "lấp đầy" tham số bắt buộc, KHÔNG BAO GIỜ thực sự được dùng tới trong test | `new User()` truyền vào tham số không liên quan tới logic đang test |
| **Stub** | Trả về giá trị **cố định, được lập trình sẵn** khi gọi method, không quan tâm có được gọi hay không | `when(repo.findById(1L)).thenReturn(user)` |
| **Fake** | Có **implementation thật, đơn giản hóa** — hoạt động thực sự nhưng không phù hợp production | `InMemoryUserRepository` tự viết dùng `HashMap` thay vì DB thật |
| **Spy** | Bọc quanh object THẬT, cho phép **theo dõi** lời gọi trong khi vẫn chạy logic gốc | `@Spy` của Mockito (mục dưới) |
| **Mock** | Đối tượng giả lập có thể **verify được** lời gọi (số lần, tham số) — kết hợp cả Stub lẫn khả năng verify | `@Mock` của Mockito |

> **Mockito thực chất tạo ra cả Stub lẫn Mock** tùy cách dùng: chỉ `when().thenReturn()` mà không `verify()` → đang dùng nó như Stub; có `verify()` → đang dùng như Mock đúng nghĩa. Phân biệt rạch ròi 5 loại này không quá quan trọng trong code hàng ngày, nhưng là kiến thức nền giúp hiểu sâu tài liệu/bài viết kiểm thử chuyên sâu, và hay được hỏi ở vòng phỏng vấn kỹ thuật.

### Tạo Mock & định nghĩa hành vi

```java
@ExtendWith(MockitoExtension.class) // Kích hoạt Mockito trong JUnit 5
class OrderServiceTest {

    @Mock // Tạo Mock Object - KHÔNG chạy logic thật, chỉ trả về giá trị được lập trình sẵn
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks // Tự động tạo instance OrderService và inject các @Mock ở trên vào (qua Constructor Injection)
    private OrderService orderService;

    @Test
    void placeOrder_ValidOrder_SavesSuccessfully() {
        // Given (Arrange) - thiết lập hành vi giả lập
        Order order = new Order(1L, BigDecimal.valueOf(100));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentGateway.charge(any())).thenReturn(true);

        // When (Act) - thực thi method cần test
        Order result = orderService.placeOrder(order);

        // Then (Assert) - kiểm tra kết quả
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository, times(1)).save(order); // Xác nhận method save() ĐÃ được gọi đúng 1 lần
        verify(paymentGateway).charge(order.getTotalAmount());
    }

    @Test
    void placeOrder_PaymentFails_ThrowsException() {
        when(paymentGateway.charge(any())).thenReturn(false); // Giả lập thanh toán thất bại

        assertThatThrownBy(() -> orderService.placeOrder(new Order(1L, BigDecimal.valueOf(100))))
            .isInstanceOf(PaymentFailedException.class);

        verify(orderRepository, never()).save(any()); // Xác nhận save() KHÔNG BAO GIỜ được gọi khi thanh toán fail
    }
}
```

### Given-When-Then / Arrange-Act-Assert — cấu trúc chuẩn của 1 test

```java
@Test
void methodName_condition_expectedResult() {
    // Given (Arrange) - Chuẩn bị dữ liệu, mock behavior
    // ...

    // When (Act) - Gọi method cần test
    // ...

    // Then (Assert) - Kiểm tra kết quả
    // ...
}
```

### Các kỹ thuật Mockito nâng cao

```java
// Trả về giá trị KHÁC NHAU qua nhiều lần gọi liên tiếp
when(mockService.getNext())
    .thenReturn(1)
    .thenReturn(2)
    .thenThrow(new RuntimeException("Hết dữ liệu"));

// Giả lập throw exception
when(orderRepository.findById(999L)).thenThrow(new EntityNotFoundException());

// ArgumentCaptor - "bắt" tham số thực tế đã được truyền vào mock để kiểm tra sâu hơn
@Captor
private ArgumentCaptor<Order> orderCaptor;

@Test
void placeOrder_CapturesCorrectOrderData() {
    orderService.placeOrder(new OrderRequest(...));

    verify(orderRepository).save(orderCaptor.capture());
    Order capturedOrder = orderCaptor.getValue();
    assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
}

// Spy - GIỐNG Mock nhưng mặc định GỌI METHOD THẬT, trừ khi override cụ thể
@Spy
private List<String> spyList = new ArrayList<>();

@Test
void spyDemo() {
    spyList.add("item"); // Chạy logic THẬT của ArrayList
    verify(spyList).add("item"); // Vẫn verify được như Mock
}
```

⚠️ **Phân biệt Mock vs Spy — bẫy hay gặp:**

| | `@Mock` | `@Spy` |
|---|---|---|
| Hành vi mặc định | KHÔNG chạy logic thật, trả `null`/giá trị mặc định nếu không stub | Chạy logic THẬT của object gốc, trừ khi override |
| Dùng khi nào | Cô lập hoàn toàn dependency (đa số trường hợp) | Cần giữ lại phần lớn hành vi thật, chỉ override 1 vài method cụ thể |

---

## 5. Unit Test cho Service Layer

Đây là loại test **nên viết nhiều nhất** — business logic thường tập trung ở tầng Service.

```java
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private InventoryService inventoryService;

    @Test
    void reduceStock_SufficientStock_ReducesCorrectly() {
        // Given
        Product product = new Product(1L, "Laptop", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When
        inventoryService.reduceStock(1L, 3);

        // Then
        assertThat(product.getStock()).isEqualTo(7);
        verify(productRepository).save(product);
    }

    @Test
    void reduceStock_InsufficientStock_ThrowsException() {
        Product product = new Product(1L, "Laptop", 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> inventoryService.reduceStock(1L, 5))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("không đủ");

        verify(productRepository, never()).save(any()); // Đảm bảo KHÔNG lưu khi có lỗi
    }

    @Test
    void reduceStock_ProductNotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reduceStock(999L, 1))
            .isInstanceOf(ProductNotFoundException.class);
    }
}
```

> **Liên hệ Module 12:** Đây chính là lý do **Constructor Injection được khuyến nghị mạnh mẽ** — nhờ đó, `InventoryService` có thể được `new` trực tiếp (hoặc dùng `@InjectMocks`) với mock dependency, **không cần khởi động toàn bộ Spring Context** — giúp Unit Test chạy trong mili-giây thay vì giây.

---

## 6. @SpringBootTest

**Integration Test** — khởi động **toàn bộ (hoặc 1 phần) Spring Context thật**, test nhiều tầng phối hợp với nhau.

```java
@SpringBootTest // Khởi động TOÀN BỘ ApplicationContext - CHẬM hơn Unit Test đáng kể
@AutoConfigureMockMvc
@Transactional // Tự động ROLLBACK sau mỗi test -> mỗi test có DB state sạch, độc lập với nhau
class OrderIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void placeOrder_EndToEnd_PersistsToDatabase() {
        // Given - dùng REPOSITORY THẬT, KHÔNG mock -> test toàn bộ luồng Service -> Repository -> DB
        User user = userRepository.save(new User("Pho", "pho@example.com"));

        // When
        Order order = orderService.placeOrder(user.getId(), List.of(new OrderItem(1L, 2)));

        // Then
        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getUser().getId()).isEqualTo(user.getId());
    }
}
```

### @SpringBootTest vs Unit Test — khi nào dùng cái nào?

| | Unit Test (Mockito) | @SpringBootTest (Integration) |
|---|---|---|
| Tốc độ | Rất nhanh (mili-giây) | Chậm hơn nhiều (giây - do khởi động Spring Context) |
| Phạm vi | 1 class, mock hết dependency | Nhiều tầng thật (Service+Repository+DB) |
| Khi nào dùng | Test business logic cô lập, đa số trường hợp | Test luồng tích hợp thật, đặc biệt query DB phức tạp |
| Số lượng khuyến nghị | Nhiều (đáy Pyramid) | Vừa phải (giữa Pyramid) |

⚠️ **Bẫy hiệu năng CI/CD:** Nếu **toàn bộ** test suite đều dùng `@SpringBootTest`, thời gian chạy CI có thể tăng từ vài giây lên **hàng chục phút** khi dự án lớn dần — vì mỗi class test (mặc định) sẽ khởi động lại Spring Context riêng (dù Spring có cơ chế cache Context giữa các test class có cùng config, việc lạm dụng vẫn làm chậm đáng kể).

---

## 7. MockMvc — test Controller layer

`@WebMvcTest` chỉ khởi động tầng **Web (Controller)**, mock tầng Service — nhanh hơn `@SpringBootTest` nhiều vì không cần khởi động DB/toàn bộ Bean.

```java
@WebMvcTest(OrderController.class) // CHỈ load Controller layer, không load Service/Repository thật
class OrderControllerTest {

    @Autowired private MockMvc mockMvc; // Giả lập HTTP request mà KHÔNG cần chạy server thật

    @MockBean // Mock Bean và ĐĂNG KÝ vào Spring Context (khác @Mock của Mockito thuần)
    private OrderService orderService;

    @Autowired private ObjectMapper objectMapper; // Convert object <-> JSON

    @Test
    void getOrder_ExistingId_Returns200WithOrderData() throws Exception {
        OrderResponse response = new OrderResponse(1L, BigDecimal.valueOf(500000), OrderStatus.PENDING);
        when(orderService.getOrder(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalAmount").value(500000))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrder_NonExistentId_Returns404() throws Exception {
        when(orderService.getOrder(999L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createOrder_ValidRequest_Returns201() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, List.of(new OrderItemDto(1L, 2)));
        OrderResponse response = new OrderResponse(1L, BigDecimal.valueOf(200000), OrderStatus.PENDING);
        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // Serialize object -> JSON string
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createOrder_InvalidRequest_Returns400() throws Exception {
        // Request thiếu field bắt buộc -> kích hoạt @Valid -> MethodArgumentNotValidException
        String invalidJson = "{}";

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
```

> **Phân biệt `@Mock` (Mockito thuần) vs `@MockBean` (Spring Boot Test):** `@MockBean` không chỉ tạo mock, mà còn **đăng ký mock đó vào Spring ApplicationContext**, thay thế Bean thật cùng type — cần thiết khi Controller được Spring tự động `@Autowired` Service vào, không phải `new` thủ công như Mockito thuần.

---

## 8. Testing Spring Security

Khi Controller có `@PreAuthorize`/được bảo vệ bởi `SecurityFilterChain` (Module 16), test `MockMvc` thông thường (mục 7) sẽ **luôn nhận `401/403`** vì request test không có Authentication nào cả. Module `spring-security-test` cung cấp annotation để giả lập user đã đăng nhập, không cần verify JWT thật.

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### @WithMockUser — cách đơn giản nhất

```java
@WebMvcTest(OrderController.class)
class OrderControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OrderService orderService;

    @Test
    @WithMockUser(username = "pho@example.com", roles = "USER") // Giả lập user đã đăng nhập với role USER
    void getMyOrders_AuthenticatedUser_Returns200() throws Exception {
        when(orderService.findByUserEmail("pho@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isOk()); // Không còn bị 401 vì đã "đăng nhập" giả lập
    }

    @Test
    @WithMockUser(roles = "USER") // Role USER, KHÔNG phải ADMIN
    void deleteAllOrders_NonAdminUser_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders"))
                .andExpect(status().isForbidden()); // @PreAuthorize("hasRole('ADMIN')") từ chối đúng như kỳ vọng
    }

    @Test
    // KHÔNG có @WithMockUser -> mô phỏng request chưa đăng nhập
    void getMyOrders_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isUnauthorized());
    }
}
```

⚠️ **Lưu ý quan trọng:** `roles = "USER"` trong `@WithMockUser` tự động thêm tiền tố `ROLE_` giống hệt cơ chế `hasRole()` đã học ở Module 16 — tương đương `authorities = "ROLE_USER"`. Nếu ứng dụng dùng authority không theo chuẩn `ROLE_*` (permission-based), phải dùng `authorities = "..."` trực tiếp thay vì `roles`.

### @WithUserDetails — khi cần test đúng với UserDetailsService thật

`@WithMockUser` tạo ra 1 `UserDetails` **giả đơn giản**, không đi qua `CustomUserDetailsService` thật của ứng dụng — phù hợp đa số trường hợp chỉ cần test phân quyền theo role. Khi cần principal là chính Entity `User` thật (để test logic dùng `@AuthenticationPrincipal User currentUser`), dùng `@WithUserDetails`:

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithUserDetails(value = "pho@example.com", userDetailsServiceBeanName = "customUserDetailsService")
    // Gọi THẬT CustomUserDetailsService.loadUserByUsername("pho@example.com")
    // -> principal trong SecurityContext là chính Entity User thật, load từ DB test
    void getMyOrders_RealUserDetails_ReturnsCorrectData() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isOk());
    }
}
```

| | `@WithMockUser` | `@WithUserDetails` |
|---|---|---|
| Nguồn `UserDetails` | Giả lập đơn giản, không qua `UserDetailsService` | Gọi THẬT `UserDetailsService`, cần user tồn tại trong DB test |
| Tốc độ setup | Nhanh, không cần chuẩn bị dữ liệu | Cần tạo sẵn user trong DB test trước |
| Phù hợp | Test `@WebMvcTest` (Controller layer cô lập), test phân quyền theo role | Test `@SpringBootTest` cần principal thật (VD: `@AuthenticationPrincipal`) |

---

## 9. @DataJpaTest

Test tầng **Repository** với **database trong bộ nhớ (in-memory)** như H2 — nhanh hơn kết nối DB thật, nhưng vẫn kiểm tra được query JPQL/Specification có đúng không.

```java
@DataJpaTest // CHỈ load tầng JPA (Repository, Entity), tự động rollback sau mỗi test
class OrderRepositoryTest {

    @Autowired private TestEntityManager entityManager; // Helper để chuẩn bị dữ liệu test trực tiếp
    @Autowired private OrderRepository orderRepository;

    @Test
    void findByStatusAndUserEmail_MatchingCriteria_ReturnsCorrectOrders() {
        // Given - chuẩn bị dữ liệu trực tiếp qua EntityManager
        User user = new User("Pho", "pho@example.com");
        entityManager.persist(user);

        Order order1 = new Order(user, OrderStatus.PENDING, BigDecimal.valueOf(100));
        Order order2 = new Order(user, OrderStatus.COMPLETED, BigDecimal.valueOf(200));
        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush(); // Ép chạy SQL ngay để đảm bảo dữ liệu đã "thật sự" trong DB test

        // When
        List<Order> result = orderRepository.findByStatusAndUser_Email(OrderStatus.PENDING, "pho@example.com");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalAmount()).isEqualTo(BigDecimal.valueOf(100));
    }
}
```

⚠️ **Bẫy quan trọng: H2 (in-memory) không phải lúc nào cũng giống hệt MySQL/PostgreSQL thật (production)!** Một số hành vi SQL đặc thù (dialect riêng, function riêng của MySQL/PostgreSQL, cách xử lý Index/JSON column...) có thể **PASS trên H2 nhưng FAIL trên MySQL thật** — đây chính là động lực ra đời của **Testcontainers** (mục 10).

---

## 10. Testcontainers

**Testcontainers** là thư viện cho phép test chạy với **database thật** (MySQL, PostgreSQL, Redis, MongoDB...) trong **Docker container tạm thời** — tự động khởi động trước khi test chạy, tự động dọn dẹp sau khi xong.

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@Testcontainers // Kích hoạt lifecycle quản lý Container
class OrderIntegrationTest {

    @Container // Testcontainers tự động start container này TRƯỚC khi test chạy, tự stop SAU khi xong
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource // Override cấu hình DataSource của Spring Boot để trỏ tới Container vừa tạo
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private OrderRepository orderRepository;

    @Test
    void saveOrder_WithPostgresSpecificFeature_WorksCorrectly() {
        // Test chạy với PostgreSQL THẬT (trong Docker) -
        // phát hiện được các vấn đề đặc thù dialect mà H2 sẽ "bỏ lọt"
        Order order = orderRepository.save(new Order(...));
        assertThat(order.getId()).isNotNull();
    }
}
```

**Vì sao Testcontainers được khuyến nghị hơn H2 cho Integration Test nghiêm túc:**
- Test chạy với **chính xác cùng loại database** dùng ở production → phát hiện được bug liên quan dialect-specific mà H2 bỏ sót
- Không cần cài đặt DB thật trên máy dev/CI server — chỉ cần Docker
- Mỗi lần test chạy đều là **DB "sạch"** hoàn toàn (container mới) — tránh vấn đề "test trước ảnh hưởng test sau" do state cũ còn sót lại

> **Đánh đổi:** Testcontainers chậm hơn H2 (do phải khởi động Docker container thật) — nên cân nhắc dùng cho các Integration Test **quan trọng nhất**, không nhất thiết cho toàn bộ test suite.

---

## 11. Test Coverage

**Test Coverage** đo lường **% code được thực thi** khi chạy toàn bộ test suite — công cụ phổ biến: **JaCoCo** (Java Code Coverage).

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

⚠️ **Hiểu đúng về Coverage — bẫy tư duy phổ biến:** **Coverage cao KHÔNG đồng nghĩa với test tốt.** Coverage chỉ đo "dòng code có được CHẠY QUA hay không", **không đo** "logic có được KIỂM TRA ĐÚNG hay không":

```java
// "Coverage 100%" nhưng test này VÔ NGHĨA - không assert gì cả!
@Test
void testCalculate() {
    calculator.add(2, 3); // Dòng code được "chạy qua" -> tính vào coverage
    // KHÔNG CÓ assertion nào -> dù logic add() có bug (VD: trả về sai kết quả),
    // test này vẫn "pass" (vì không throw exception) và vẫn tính là coverage 100%!
}
```

> **Nguyên tắc thực chiến:** Đặt mục tiêu Coverage (VD: 70-80%) là hợp lý để đảm bảo mức độ test tối thiểu, nhưng **không nên coi Coverage là mục tiêu tối thượng** — ưu tiên viết test có **assertion ý nghĩa**, bao phủ các **edge case** (giá trị biên, null, exception path) hơn là chạy theo con số phần trăm.

---

## 12. Mutation Testing

**Vấn đề:** Ví dụ "Coverage 100% nhưng test vô nghĩa" ở mục 11 cho thấy Coverage không đo được **chất lượng thật** của assertion. **Mutation Testing** giải quyết đúng vấn đề này bằng cách **tự động sửa nhỏ (mutate) code nguồn** rồi kiểm tra xem test suite có **phát hiện được** sự thay đổi đó hay không.

### Nguyên lý hoạt động

Công cụ (phổ biến nhất cho Java: **PIT — pitest**) tự động sinh ra nhiều **"con đột biến" (mutant)** — bản sao code với 1 thay đổi nhỏ có chủ đích:

```java
// Code gốc
public boolean isEligibleForDiscount(int quantity) {
    return quantity > 10;
}

// PIT tự động sinh các "mutant" như:
return quantity >= 10;   // Đổi > thành >=
return quantity < 10;    // Đổi > thành <
return true;             // Loại bỏ hoàn toàn điều kiện
```

Với mỗi mutant, PIT chạy lại **toàn bộ test suite**:
- Nếu **có ít nhất 1 test FAIL** → mutant bị "giết" (killed) → tốt, chứng tỏ test thực sự phát hiện được thay đổi hành vi
- Nếu **toàn bộ test vẫn PASS** → mutant "sống sót" (survived) → **dấu hiệu xấu**: test suite có coverage nhưng không thực sự kiểm tra đúng logic này

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <configuration>
        <targetClasses>
            <param>com.example.service.*</param>
        </targetClasses>
    </configuration>
</plugin>
```

### Liên hệ trực tiếp với bẫy "Coverage giả" ở mục 11

```java
// Test có Coverage 100% cho isEligibleForDiscount(), nhưng KHÔNG assertion đúng
@Test
void testEligibility() {
    inventoryService.isEligibleForDiscount(15); // Chạy qua dòng code -> Coverage tính 100%
    // Không có assertThat(...)... -> Mutation Testing sẽ phát hiện:
    // TẤT CẢ mutant (>=, <, true, false) đều "sống sót" vì test không hề kiểm tra kết quả trả về!
}
```

> **Đánh đổi:** Mutation Testing chạy **rất chậm** (chạy lại toàn bộ test suite cho MỖI mutant sinh ra, có thể hàng trăm/nghìn mutant) — không phù hợp chạy trong mọi lần build CI, thường chỉ chạy định kỳ (VD: nightly build) hoặc khi cần đánh giá sâu chất lượng test của 1 module quan trọng, không phải công cụ dùng hàng ngày như JaCoCo Coverage.

| | Test Coverage (JaCoCo) | Mutation Testing (PIT) |
|---|---|---|
| Đo cái gì | Dòng code có được chạy qua hay không | Test có thực sự PHÁT HIỆN được thay đổi logic hay không |
| Tốc độ | Nhanh (chạy 1 lần) | Rất chậm (chạy lại test suite cho mỗi mutant) |
| Độ tin cậy | Dễ bị "đánh lừa" bởi test không có assertion | Đáng tin cậy hơn nhiều — phản ánh chất lượng thật |
| Tần suất chạy khuyến nghị | Mỗi lần CI build | Định kỳ (nightly/weekly), hoặc trước khi merge code quan trọng |

---

## 13. ⚠️ Các bẫy hay gặp

1. **Viết test không có assertion** (hoặc assertion vô nghĩa như `assertTrue(true)`) — chỉ để "tăng coverage" mà không thực sự kiểm tra logic.

2. **Test phụ thuộc thứ tự chạy** (test A phải chạy trước test B mới đúng) — vi phạm nguyên tắc mỗi test phải **độc lập hoàn toàn**, gây lỗi khó debug khi thứ tự chạy thay đổi (VD: chạy song song).

3. **Dùng `@SpringBootTest` cho MỌI test** thay vì Unit Test thuần với Mockito — làm CI chạy chậm không cần thiết.

4. **Test phụ thuộc vào dữ liệu có sẵn trong DB thật** (không tự tạo dữ liệu test riêng, không rollback) — test có thể pass hôm nay, fail ngày mai khi dữ liệu DB thay đổi.

5. **Quên `@Transactional` trong `@SpringBootTest`** khi thao tác ghi DB → dữ liệu test "rò rỉ" sang các test khác, gây lỗi khó hiểu (đặc biệt khi chạy nhiều test cùng lúc).

6. **Nhầm lẫn `@Mock` và `@MockBean`** — `@Mock` (Mockito thuần) không đăng ký vào Spring Context, dùng sai chỗ sẽ khiến Spring không tìm thấy Bean cần inject.

7. **Test H2 pass nhưng production (MySQL/PostgreSQL) fail** — do khác biệt dialect SQL, không phát hiện được nếu chỉ dùng `@DataJpaTest` với H2 mà không có Integration Test với Testcontainers cho các query phức tạp/đặc thù.

8. **Coi Test Coverage 100% là "an toàn tuyệt đối"** — coverage cao không đảm bảo logic đúng, chỉ đảm bảo dòng code được chạy qua (xem mục 12 — Mutation Testing để đo chất lượng thật).

9. **Mock quá nhiều tới mức test không còn ý nghĩa** — mock cả những class đơn giản (VD: DTO, Value Object) không cần thiết, khiến test trở nên rối rắm và không phản ánh đúng hành vi thật.

10. **Không test edge case/exception path** — chỉ test "happy path" (trường hợp thành công), bỏ qua trường hợp lỗi/giá trị biên (null, empty list, số âm, hết hạn token...) — đây thường là nơi bug thật sự ẩn náu.

11. **Test endpoint có bảo mật mà không set Authentication** — quên `@WithMockUser`/`@WithUserDetails` khiến `MockMvc` test luôn nhận `401` dù logic Controller hoàn toàn đúng, dễ nhầm tưởng code có bug.

12. **Dùng `roles` thay vì `authorities` trong `@WithMockUser` khi hệ thống không theo chuẩn `ROLE_*`** — `roles = "ADMIN"` tự thêm tiền tố `ROLE_`, không khớp nếu ứng dụng lưu authority permission-based không có tiền tố này.

---

## 14. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Ghi nhớ nhanh |
|---|---|
| Test Pyramid | Unit nhiều nhất (nhanh, rẻ) → Integration vừa → E2E ít nhất (chậm, đắt) |
| TDD | Red (viết test fail) → Green (code tối thiểu để pass) → Refactor |
| Given-When-Then | Cấu trúc chuẩn: chuẩn bị → thực thi → kiểm tra |
| Test Doubles | Dummy/Stub/Fake/Spy/Mock — 5 loại "đóng thế" khác nhau, không chỉ có "Mock" |
| Mockito `@Mock`/`@InjectMocks` | Cô lập class đang test khỏi dependency thật, chạy cực nhanh |
| AssertJ | Cú pháp fluent `assertThat(x).isEqualTo(y)` — khuyến nghị hơn Assertions thuần |
| `@MethodSource`/`@EnumSource` | Cung cấp dữ liệu test phức tạp/enum cho `@ParameterizedTest` |
| `@SpringBootTest` | Khởi động toàn bộ Spring Context — chậm, dùng cho Integration Test |
| `@WebMvcTest` + MockMvc | Chỉ load Controller layer, mock Service — test HTTP layer nhanh |
| `@WithMockUser`/`@WithUserDetails` | Giả lập user đăng nhập để test endpoint có bảo mật, không cần JWT thật |
| `@DataJpaTest` | Test Repository với H2 in-memory — nhanh nhưng có thể lệch dialect thật |
| Testcontainers | Test với DB thật (Docker) — chính xác nhất, chậm hơn H2 |
| `@Mock` vs `@MockBean` | `@Mock` (Mockito thuần, không vào Spring Context) vs `@MockBean` (đăng ký vào Spring Context) |
| Test Coverage | Chỉ đo dòng code được chạy qua — không đảm bảo test có ý nghĩa |
| Mutation Testing (PIT) | Đo test có THỰC SỰ phát hiện thay đổi logic hay không — chậm, chạy định kỳ |

---

## 15. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (Đúng/Sai + giải thích)

1. Test Pyramid khuyến nghị nên có nhiều E2E Test nhất vì chúng kiểm tra được toàn bộ hệ thống.
2. `@Mock` của Mockito tự động được đăng ký vào Spring ApplicationContext.
3. Test Coverage 100% đảm bảo code hoàn toàn không có bug logic.
4. `@DataJpaTest` mặc định dùng H2 in-memory database, có thể không phát hiện được vấn đề dialect-specific của MySQL/PostgreSQL thật.
5. Mỗi Unit Test nên độc lập hoàn toàn, không phụ thuộc vào thứ tự chạy hay kết quả của test khác.
6. `@WebMvcTest` khởi động toàn bộ Spring Context giống `@SpringBootTest`.
7. Testcontainers cho phép test chạy với database thật trong Docker container, tự động dọn dẹp sau khi test xong.
8. `verify(mock, never()).someMethod()` dùng để xác nhận 1 method KHÔNG được gọi trong quá trình test.
9. Trong Mutation Testing, 1 mutant "sống sót" (survived) là dấu hiệu TỐT, chứng tỏ code ổn định.
10. `@WithMockUser(roles = "ADMIN")` tương đương với việc set authority là `"ROLE_ADMIN"` trong SecurityContext.

### Phần B — Bài tập viết code (6 bài)

**Bài 1:** Viết Unit Test (dùng Mockito) cho `AuthService.login(email, password)` — 3 trường hợp: đăng nhập thành công, sai password (throw `BadCredentialsException`), user không tồn tại (throw `UserNotFoundException`).

**Bài 2:** Viết `@ParameterizedTest` với `@CsvSource` để test 1 method `validatePassword(String password)` trả về `true`/`false` dựa trên độ dài tối thiểu 8 ký tự, ít nhất 4-5 bộ dữ liệu test khác nhau.

**Bài 3:** Viết `MockMvc` test cho endpoint `POST /api/v1/products` — kiểm tra: (a) tạo thành công trả `201` kèm header `Location`, (b) request thiếu field `name` bắt buộc trả `400`.

**Bài 4:** Viết `@DataJpaTest` cho `ProductRepository` với 1 Specification/Query method tùy chọn (VD: `findByCategoryAndPriceLessThan`), dùng `TestEntityManager` để chuẩn bị dữ liệu.

**Bài 5:** Giải thích (bằng comment code minh họa, không cần chạy thật) tình huống cụ thể mà `@DataJpaTest` (H2) sẽ **PASS** nhưng Testcontainers (PostgreSQL thật) sẽ **FAIL** — gợi ý: liên hệ tới kiểu dữ liệu JSONB hoặc hàm SQL đặc thù đã học ở Module 10 phần RDBMS & NoSQL.

**Bài 6:** Viết `MockMvc` test dùng `@WithMockUser` cho endpoint `DELETE /api/v1/admin/orders/{id}` (được bảo vệ bởi `@PreAuthorize("hasRole('ADMIN')")` ở Module 16) — 3 trường hợp: role ADMIN trả `204`, role USER trả `403`, không đăng nhập trả `401`.

### Phần C — Gợi ý đáp án

<details>
<summary><b>Đáp án Phần A</b></summary>

1. **Sai.** Ngược lại — Test Pyramid khuyến nghị Unit Test nhiều nhất (nhanh, rẻ, ổn định), E2E Test ít nhất (chậm, đắt, dễ "flaky").
2. **Sai.** `@Mock` (Mockito thuần) KHÔNG đăng ký vào Spring Context — muốn vậy phải dùng `@MockBean`.
3. **Sai.** Coverage 100% chỉ đảm bảo dòng code được "chạy qua", không đảm bảo assertion đúng hay logic không có bug.
4. **Đúng.** Đây chính là lý do Testcontainers ra đời — để test với chính DB dùng ở production.
5. **Đúng.** Đây là nguyên tắc cơ bản của test tốt — độc lập, có thể chạy theo bất kỳ thứ tự nào, kể cả song song.
6. **Sai.** `@WebMvcTest` chỉ load tầng Web (Controller), KHÔNG load Service/Repository thật — nhanh hơn `@SpringBootTest` nhiều.
7. **Đúng.** Đây chính là mục đích thiết kế của Testcontainers.
8. **Đúng.** `never()` là 1 dạng `VerificationMode` xác nhận số lần gọi = 0.
9. **Sai.** Mutant "sống sót" là dấu hiệu XẤU — nghĩa là test suite KHÔNG phát hiện được sự thay đổi logic đó, chứng tỏ vùng code này thiếu assertion đủ mạnh, không phải code ổn định.
10. **Đúng.** `@WithMockUser` tự động thêm tiền tố `ROLE_` cho `roles`, giống hệt cơ chế `hasRole()` của Spring Security đã học ở Module 16.

</details>

<details>
<summary><b>Đáp án Bài 1</b></summary>

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    @Test
    void login_ValidCredentials_ReturnsUser() {
        User user = new User("pho@example.com", "hashedPassword123");
        when(userRepository.findByEmail("pho@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "hashedPassword123")).thenReturn(true);

        User result = authService.login("pho@example.com", "rawPassword");

        assertThat(result.getEmail()).isEqualTo("pho@example.com");
    }

    @Test
    void login_WrongPassword_ThrowsBadCredentialsException() {
        User user = new User("pho@example.com", "hashedPassword123");
        when(userRepository.findByEmail("pho@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword123")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("pho@example.com", "wrongPassword"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_UserNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmail("notexist@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("notexist@example.com", "anyPassword"))
            .isInstanceOf(UserNotFoundException.class);

        // Đảm bảo KHÔNG gọi passwordEncoder khi user không tồn tại (fail-fast)
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 2</b></summary>

```java
class PasswordValidatorTest {

    @ParameterizedTest
    @CsvSource({
        "12345678, true",     // Đủ 8 ký tự -> hợp lệ
        "1234567, false",     // Chỉ 7 ký tự -> không hợp lệ
        "abcdefgh, true",     // Đủ 8 ký tự chữ -> hợp lệ
        "'', false",          // Chuỗi rỗng -> không hợp lệ
        "abc, false"          // Quá ngắn -> không hợp lệ
    })
    void validatePassword_VariousInputs_ReturnsExpectedResult(String password, boolean expected) {
        boolean result = PasswordValidator.validatePassword(password);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void validatePassword_NullInput_ReturnsFalse() {
        // Trường hợp null nên tách riêng vì @CsvSource khó biểu diễn null rõ ràng
        assertThat(PasswordValidator.validatePassword(null)).isFalse();
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 3</b></summary>

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProductService productService;

    @Test
    void createProduct_ValidRequest_Returns201WithLocationHeader() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest("Laptop", "electronics", BigDecimal.valueOf(15000000));
        ProductResponse response = new ProductResponse(1L, "Laptop", "electronics", BigDecimal.valueOf(15000000));
        when(productService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/products/1")))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void createProduct_MissingRequiredField_Returns400() throws Exception {
        // "name" là field bắt buộc nhưng bị thiếu trong request
        String invalidRequest = """
            {
                "category": "electronics",
                "price": 15000000
            }
            """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 4</b></summary>

```java
@DataJpaTest
class ProductRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private ProductRepository productRepository;

    @Test
    void findByCategoryAndPriceLessThan_MatchingProducts_ReturnsCorrectResults() {
        // Given
        Product cheapLaptop = new Product("Laptop A", "electronics", BigDecimal.valueOf(10000000));
        Product expensiveLaptop = new Product("Laptop B", "electronics", BigDecimal.valueOf(30000000));
        Product cheapBook = new Product("Sách Java", "books", BigDecimal.valueOf(150000));

        entityManager.persist(cheapLaptop);
        entityManager.persist(expensiveLaptop);
        entityManager.persist(cheapBook);
        entityManager.flush();

        // When - chỉ tìm electronics có giá dưới 20 triệu
        List<Product> result = productRepository.findByCategoryAndPriceLessThan(
                "electronics", BigDecimal.valueOf(20000000));

        // Then
        assertThat(result)
            .hasSize(1)
            .extracting(Product::getName)
            .containsExactly("Laptop A");
    }
}
```

</details>

<details>
<summary><b>Đáp án Bài 5</b></summary>

```java
// Minh họa (pseudo-test, không chạy thật) tình huống H2 PASS nhưng PostgreSQL thật FAIL

@Entity
public class Product {
    @Id @GeneratedValue private Long id;

    @Column(columnDefinition = "jsonb") // Kiểu dữ liệu ĐẶC THÙ của PostgreSQL
    private String metadata; // VD: '{"color": "red", "size": "M"}'
}

// Test dùng @DataJpaTest (H2):
@Test
void saveProductWithJsonbMetadata_H2_MayPassIncorrectly() {
    // H2 KHÔNG có kiểu JSONB thật -> Hibernate có thể fallback về xử lý như VARCHAR/TEXT thông thường
    // -> Test "PASS" nhưng KHÔNG thực sự kiểm tra được hành vi JSONB thật
    //    (VD: query JSON path như "metadata->>'color' = 'red'" sẽ hoạt động khác hoàn toàn
    //     hoặc thậm chí LỖI CÚ PHÁP trên H2 vì H2 không hỗ trợ toán tử ->> của PostgreSQL)
    Product product = new Product();
    product.setMetadata("{\"color\": \"red\"}");
    productRepository.save(product); // Có thể pass "giả" trên H2
}

// Test TƯƠNG TỰ chạy với Testcontainers (PostgreSQL thật):
@Test
void saveProductWithJsonbMetadata_RealPostgres_RevealsRealBehavior() {
    // Với PostgreSQL thật, nếu Entity mapping SAI (VD: thiếu @Type(JsonType.class)
    // hoặc cấu hình Hibernate Types cho JSONB không đúng), test này sẽ THỰC SỰ FAIL
    // -> phát hiện được bug mà H2 "che giấu" do không mô phỏng đúng dialect PostgreSQL
    Product product = new Product();
    product.setMetadata("{\"color\": \"red\"}");
    Product saved = productRepository.save(product);

    // Native query dùng toán tử JSONB đặc thù của PostgreSQL - CHỈ chạy đúng trên PostgreSQL thật
    List<Product> found = productRepository.findByMetadataColor("red"); // dùng ->> operator trong @Query native
    assertThat(found).isNotEmpty(); // Trên H2: có thể lỗi cú pháp hoặc không tìm thấy do xử lý JSON khác
}
```

**Kết luận:** Đây chính là lý do các dự án production nghiêm túc **luôn có ít nhất 1 tầng Integration Test dùng Testcontainers** cho các tính năng liên quan tới đặc thù dialect DB (JSONB, Full-text search, Array type...), thay vì chỉ tin tưởng hoàn toàn vào `@DataJpaTest` với H2.

</details>

<details>
<summary><b>Đáp án Bài 6</b></summary>

```java
@WebMvcTest(AdminOrderController.class)
class AdminOrderControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OrderService orderService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteOrder_AdminRole_Returns204() throws Exception {
        doNothing().when(orderService).deleteById(1L);

        mockMvc.perform(delete("/api/v1/admin/orders/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(orderService).deleteById(1L);
    }

    @Test
    @WithMockUser(roles = "USER") // Đã đăng nhập nhưng KHÔNG đủ quyền
    void deleteOrder_UserRole_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders/1").with(csrf()))
                .andExpect(status().isForbidden());

        verify(orderService, never()).deleteById(any()); // Không được gọi vì bị chặn ở tầng phân quyền
    }

    @Test
    // KHÔNG có @WithMockUser -> mô phỏng request hoàn toàn chưa xác thực
    void deleteOrder_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/orders/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
```

*(Lưu ý: `.with(csrf())` chỉ cần thiết nếu ứng dụng vẫn bật CSRF protection — với JWT thuần Stateless đã tắt CSRF như Module 16, có thể bỏ qua phần này.)*

</details>

---

*File tiếp theo trong lộ trình: **Module 18 — Caching & Messaging** (Spring Cache Abstraction, Redis làm Cache, Cache-Aside/Write-Through Pattern, Message Queue với RabbitMQ/Kafka, Async Processing với @Async).*
