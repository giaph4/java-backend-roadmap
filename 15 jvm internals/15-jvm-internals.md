# Module 07 — JVM Internals

> **Mức độ ưu tiên: Trung bình → Bổ sung** — Không cần thuộc lòng chi tiết cài đặt của từng GC algorithm, nhưng hiểu rõ Heap/Stack, cơ chế Garbage Collection ở mức khái niệm, và ClassLoader/JIT là **điểm khác biệt rõ rệt** giữa lập trình viên chỉ biết "viết code chạy được" và người thực sự hiểu **tại sao** code chạy như vậy — cực kỳ hữu ích khi debug production issue (memory leak, ứng dụng chậm dần theo thời gian) mà không có kiến thức này gần như không thể chẩn đoán được.

---

## Mục lục

1. [JVM là gì — bức tranh tổng quan](#1-jvm-là-gì--bức-tranh-tổng-quan)
2. [Heap vs Stack — đào sâu](#2-heap-vs-stack--đào-sâu)
3. [Cấu trúc chi tiết của Heap](#3-cấu-trúc-chi-tiết-của-heap)
4. [Garbage Collection — khái niệm nền tảng](#4-garbage-collection--khái-niệm-nền-tảng)
5. [Generational Garbage Collection — cách JVM tối ưu GC](#5-generational-garbage-collection--cách-jvm-tối-ưu-gc)
6. [Các thuật toán GC phổ biến (mức khái niệm)](#6-các-thuật-toán-gc-phổ-biến-mức-khái-niệm)
7. [Memory Leak trong Java — vẫn có thể xảy ra dù có GC](#7-memory-leak-trong-java--vẫn-có-thể-xảy-ra-dù-có-gc)
8. [ClassLoader](#8-classloader)
9. [Quá trình biên dịch & JIT Compiler](#9-quá-trình-biên-dịch--jit-compiler)
10. [Công cụ chẩn đoán JVM thực tế](#10-công-cụ-chẩn-đoán-jvm-thực-tế)
11. [Tổng kết — Bảng ghi nhớ nhanh](#11-tổng-kết--bảng-ghi-nhớ-nhanh)
12. [Bài tập luyện tập](#12-bài-tập-luyện-tập)

---

## 1. JVM là gì — bức tranh tổng quan

**JVM (Java Virtual Machine)** là "máy ảo" thực thi bytecode Java — chính JVM là lý do Java có khẩu hiệu nổi tiếng **"Write Once, Run Anywhere"**: code Java biên dịch ra **bytecode** (không phải mã máy trực tiếp), và bytecode này chạy được trên **bất kỳ hệ điều hành nào có JVM tương ứng** (Windows, Linux, macOS).

```
   File.java  ──(javac compiler)──►  File.class (bytecode)  ──(JVM)──►  Chạy thực tế trên OS
```

### Các thành phần chính của JVM

```
┌─────────────────────────────────────────────────────┐
│                        JVM                            │
│  ┌───────────────┐  ┌──────────────────────────────┐ │
│  │  ClassLoader   │  │       Runtime Data Area       │ │
│  │  Subsystem     │  │  ┌────────┐  ┌──────────────┐ │ │
│  │  (mục 8)       │  │  │ Heap    │  │Method Area/   │ │ │
│  └───────────────┘  │  │(mục 2-3)│  │Metaspace       │ │ │
│                       │  └────────┘  └──────────────┘ │ │
│  ┌───────────────┐  │  ┌────────┐  ┌──────────────┐ │ │
│  │ Execution      │  │  │Stack    │  │  PC Register │ │ │
│  │ Engine         │  │  │(mỗi     │  │  (mỗi thread) │ │ │
│  │ (Interpreter + │  │  │thread)  │  └──────────────┘ │ │
│  │ JIT — mục 9)   │  │  └────────┘                     │ │
│  └───────────────┘  └──────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## 2. Heap vs Stack — đào sâu

Đã nhắc sơ ở Module 05.1 (Process vs Thread) — đây là phần trình bày đầy đủ hơn về 2 vùng nhớ quan trọng nhất.

| Tiêu chí | **Heap** | **Stack** |
|---|---|---|
| Phạm vi | **1 vùng DUY NHẤT, dùng chung** cho toàn bộ JVM/tất cả thread | **MỖI thread có 1 Stack RIÊNG** |
| Lưu gì | Toàn bộ **object** (tạo bằng `new`), instance field | **Local variable, tham số method, reference** trỏ đến object trên Heap |
| Cơ chế cấp phát | Tương đối "tự do", không theo thứ tự cố định | Theo cơ chế **LIFO** (Last In First Out) — mỗi lời gọi method tạo 1 **Stack Frame** mới, method kết thúc thì Stack Frame đó bị hủy ngay |
| Tốc độ truy cập | Chậm hơn Stack | Nhanh hơn nhiều (do cơ chế LIFO đơn giản, thường được tối ưu ở tầng CPU cache) |
| Được dọn dẹp bởi | **Garbage Collector** (mục 4) — không xác định chính xác thời điểm | Tự động khi method kết thúc — **không cần** Garbage Collector |
| Lỗi liên quan | `OutOfMemoryError: Java heap space` | `StackOverflowError` (đã gặp ở Module 01.2 — đệ quy không có base case) |

### Ví dụ minh họa cụ thể — biến nào nằm ở đâu

```java
public class Example {
    public void method() {
        int x = 10;                       // "x" (giá trị primitive) nằm trên STACK của thread hiện tại
        Student s = new Student("Pho");    // "s" (reference) nằm trên STACK; OBJECT Student thực sự nằm trên HEAP
        method2(x, s);
    }

    public void method2(int x, Student s) { // tham số cũng nằm trên STACK, trong 1 Stack Frame MỚI riêng cho method2()
        // ...
    } // Stack Frame của method2() bị HỦY NGAY khi method2() kết thúc — "x", "s" (tham số) biến mất khỏi Stack
}
```

```
STACK của Thread hiện tại:          HEAP (dùng chung mọi thread):
┌─────────────────────┐             ┌──────────────────────┐
│ Stack Frame: method2 │             │  Student object       │
│  x = 10 (copy)       │             │  { name: "Pho" }      │◄─┐
│  s = [reference] ────┼─────────────┼───────────────────────┘  │
├─────────────────────┤             └──────────────────────┘  │
│ Stack Frame: method  │                                        │
│  x = 10              │                                        │
│  s = [reference] ─────────────────────────────────────────────┘
└─────────────────────┘
```

> **Lưu ý quan trọng:** khi truyền `s` (reference) vào `method2(x, s)`, Java **copy giá trị của reference** (giống như copy 1 địa chỉ nhà) sang Stack Frame mới — **không** copy toàn bộ object trên Heap. Đây là lý do sửa đổi field bên trong object thông qua tham số method **VẪN ảnh hưởng** đến object gốc (vì cả 2 reference cùng trỏ đến 1 object duy nhất trên Heap) — kiến thức nền tảng để hiểu rõ **Java truyền tham số theo "pass-by-value của reference"**, không phải "pass-by-reference" thực sự như một số ngôn ngữ khác.

---

## 3. Cấu trúc chi tiết của Heap

```
┌──────────────────────────────────────────────────────────┐
│                          HEAP                              │
│  ┌────────────────────────────┐  ┌───────────────────┐   │
│  │      Young Generation        │  │  Old Generation     │   │
│  │  ┌────────┐ ┌─────┐ ┌─────┐ │  │  (Tenured Space)     │   │
│  │  │  Eden   │ │ S0  │ │ S1  │ │  │                      │   │
│  │  │  Space  │ │(Sur-│ │(Sur-│ │  │  Chứa object SỐNG    │   │
│  │  │         │ │vivor)│ │vivor)│ │  │  LÂU, đã "sống sót"  │   │
│  │  └────────┘ └─────┘ └─────┘ │  │  qua nhiều lần Minor GC│   │
│  └────────────────────────────┘  └───────────────────┘   │
└──────────────────────────────────────────────────────────┘

  Ngoài Heap: Metaspace (từ Java 8+, thay thế "PermGen" cũ) — lưu METADATA của class
  (thông tin class, method, không phải instance) — nằm NGOÀI Heap, dùng vùng nhớ native của OS
```

| Vùng | Chứa gì |
|---|---|
| **Eden Space** | Nơi **MỌI object mới** được tạo ra đầu tiên (`new`) |
| **Survivor Space (S0, S1)** | Object đã "sống sót" qua ít nhất 1 lần dọn dẹp ở Eden, được chuyển sang đây |
| **Old Generation (Tenured)** | Object đã tồn tại **đủ lâu** (sống sót qua nhiều ngưỡng dọn dẹp) được "thăng hạng" lên đây |
| **Metaspace** | Metadata của class (KHÔNG phải instance) — nằm ngoài Heap, dùng bộ nhớ native, tự động mở rộng theo nhu cầu |

> Chi tiết cơ chế các vùng này hoạt động cùng nhau được giải thích ở mục 5 (Generational GC).

---

## 4. Garbage Collection — khái niệm nền tảng

**Garbage Collection (GC)** là cơ chế **tự động** dọn dẹp các object **không còn được tham chiếu (reference) từ bất kỳ đâu** trong chương trình — giải phóng bộ nhớ Heap mà không cần lập trình viên tự `free()` thủ công như C/C++.

### Khi nào 1 object được coi là "rác" (garbage) — có thể bị GC dọn dẹp?

```java
public void method() {
    Student s = new Student("Pho"); // object Student được tạo, có 1 reference "s" trỏ đến
    // ... sử dụng s ...
} // method() kết thúc — "s" (reference trên Stack) bị hủy
  // → KHÔNG còn bất kỳ reference nào trỏ đến object Student này nữa
  // → object trở thành "rác" (unreachable), CÓ THỂ bị GC dọn dẹp (không nhất thiết dọn NGAY LẬP TỨC)
```

```java
Student s1 = new Student("Pho");
Student s2 = s1; // s2 TRỎ ĐẾN CÙNG object với s1
s1 = null;        // s1 không còn trỏ đến object nữa, NHƯNG s2 VẪN trỏ đến — object VẪN "sống" (reachable), CHƯA phải rác
```

### `System.gc()` — chỉ là "gợi ý", không đảm bảo chạy ngay

```java
System.gc(); // yêu cầu JVM CHẠY Garbage Collection — nhưng JVM có quyền BỎ QUA gợi ý này!
```
> **Không nên gọi `System.gc()` trong code production** — GC là công việc **JVM tự quyết định thời điểm tối ưu**, gọi thủ công thường **phản tác dụng** (có thể gây "Stop-The-World" không cần thiết — xem bên dưới).

### "Stop-The-World" — chi phí thực sự của GC

Khi GC chạy (đặc biệt là Major/Full GC — dọn dẹp Old Generation), JVM thường phải **tạm dừng TOÀN BỘ các thread ứng dụng** (gọi là **"Stop-The-World" pause**) để đảm bảo không có object nào bị thay đổi reference **trong lúc** đang quét dọn — đây chính là nguyên nhân gây ra hiện tượng **"đứng hình" (latency spike)** ngẫu nhiên mà nhiều backend service gặp phải khi Heap đầy dần theo thời gian.

> **Liên hệ thực tế:** đây là lý do tại sao việc chọn đúng thuật toán GC (mục 6) và tối ưu kích thước Heap là 1 phần quan trọng của **performance tuning** cho backend production — 1 service Spring Boot xử lý hàng nghìn request/giây mà gặp Stop-The-World pause vài giây có thể gây timeout hàng loạt request đang chờ xử lý.

---

## 5. Generational Garbage Collection — cách JVM tối ưu GC

JVM dựa trên 1 quan sát thực nghiệm quan trọng, gọi là **"Weak Generational Hypothesis"**: **"Đa số object CHẾT RẤT TRẺ"** — object được tạo ra thường chỉ tồn tại trong thời gian ngắn (biến local trong 1 method, DTO tạm thời...), chỉ 1 số ít object thực sự tồn tại lâu dài (cache, singleton, connection pool...).

Dựa trên quan sát này, JVM chia Heap thành nhiều "thế hệ" (generation) và áp dụng chiến lược dọn dẹp **khác nhau** cho từng vùng:

### Minor GC — dọn dẹp Young Generation (Eden + Survivor)

```
1. Object mới được tạo → vào Eden Space
2. Khi Eden ĐẦY → Minor GC được kích hoạt:
   - Object CÒN SỐNG (còn reference) trong Eden → chuyển sang Survivor Space (S0)
   - Object CHẾT (không còn reference) trong Eden → bị dọn dẹp ngay, giải phóng bộ nhớ
3. Lần Minor GC TIẾP THEO: object sống sót ở S0 → chuyển sang S1 (2 vùng Survivor thay phiên nhau)
4. Object "sống sót" qua ĐỦ SỐ LẦN Minor GC (ngưỡng mặc định thường là 15 lần, có thể cấu hình)
   → được "THĂNG HẠNG" (promotion) sang Old Generation
```

**Đặc điểm Minor GC:** chạy **thường xuyên** (vì Eden Space nhỏ, đầy nhanh), nhưng **rất nhanh** (vì đa số object trong Eden đã "chết trẻ" theo đúng giả thuyết trên, số lượng object thực sự cần copy sang Survivor thường ít) — thời gian Stop-The-World của Minor GC thường **không đáng kể**.

### Major GC / Full GC — dọn dẹp Old Generation

Chạy **ít thường xuyên hơn nhiều** (vì Old Generation lớn hơn, đầy chậm hơn), nhưng khi chạy thường **chậm hơn đáng kể** vì phải quét qua **toàn bộ** Old Generation (thường lớn hơn Young Generation rất nhiều) — đây là nguồn gốc chính của các **Stop-The-World pause đáng kể** trong thực tế.

> **Vì sao thiết kế 2 tầng như vậy tối ưu hơn quét toàn bộ Heap mỗi lần?** Vì đa số object chết ngay ở Young Generation — Minor GC chỉ cần quét vùng **NHỎ** này thường xuyên, hiếm khi cần "động" đến Old Generation (nơi chứa object thực sự sống lâu, hiếm khi trở thành rác) — giảm đáng kể tổng thời gian GC so với việc quét toàn bộ Heap mỗi lần dọn dẹp.

---

## 6. Các thuật toán GC phổ biến (mức khái niệm)

Không cần nhớ chi tiết cài đặt — chỉ cần hiểu **ý tưởng cốt lõi** và **đánh đổi (trade-off)** để có thể tham gia thảo luận kỹ thuật hoặc đọc log GC cơ bản.

| Thuật toán | Ý tưởng cốt lõi | Đánh đổi |
|---|---|---|
| **Serial GC** | Dùng **1 thread duy nhất** để thực hiện GC | Đơn giản, nhưng Stop-The-World lâu — chỉ phù hợp ứng dụng nhỏ, single-core |
| **Parallel GC** | Dùng **nhiều thread song song** để tăng tốc GC | Vẫn Stop-The-World, nhưng nhanh hơn Serial nhờ tận dụng đa nhân CPU — tối ưu **throughput** (tổng lượng công việc xử lý được) |
| **G1 GC (Garbage First)** | Chia Heap thành nhiều **vùng nhỏ (region)**, ưu tiên dọn dẹp vùng có **nhiều rác nhất trước** ("garbage first") | Cân bằng giữa throughput và độ trễ (latency) — là **GC mặc định** từ Java 9 trở đi, phù hợp đa số ứng dụng backend hiện đại |
| **ZGC / Shenandoah** | Thiết kế để đạt **Stop-The-World cực ngắn** (dưới 10ms) dù Heap RẤT LỚN (hàng chục/trăm GB) | Ưu tiên tối đa **độ trễ thấp**, đánh đổi 1 phần throughput — phù hợp hệ thống cực nhạy cảm về latency (trading, real-time system) |

> **Không cần học thuộc chi tiết cài đặt từng thuật toán** — điều quan trọng để phỏng vấn và làm việc thực tế là hiểu được **triết lý đánh đổi**: throughput (xử lý được nhiều việc nhất trong 1 khoảng thời gian) **>< **latency (mỗi request phản hồi nhanh nhất, ít bị "giật" bởi GC pause) — và biết rằng **G1 GC** là lựa chọn mặc định hợp lý cho hầu hết ứng dụng Spring Boot thông thường.

---

## 7. Memory Leak trong Java — vẫn có thể xảy ra dù có GC

Nhiều người mới lầm tưởng Java có GC thì **không thể** có memory leak — **sai**. Memory leak trong Java xảy ra khi object **không còn được sử dụng về mặt logic**, nhưng **vẫn còn ít nhất 1 reference** trỏ đến nó ở đâu đó — khiến GC **không dám** dọn dẹp (vì về mặt kỹ thuật, nó vẫn "reachable").

### Nguyên nhân phổ biến trong backend thực tế

```java
// 1. Static Collection ngày càng phình to, không bao giờ xóa
public class CacheManager {
    private static List<Object> cache = new ArrayList<>(); // static — sống suốt vòng đời ứng dụng
    public static void addToCache(Object obj) {
        cache.add(obj); // liên tục thêm, KHÔNG BAO GIỜ remove() → memory leak dần theo thời gian
    }
}

// 2. Listener/Callback không được hủy đăng ký (unregister)
public class EventBus {
    private List<EventListener> listeners = new ArrayList<>();
    public void register(EventListener l) { listeners.add(l); }
    // Nếu KHÔNG có unregister() được gọi đúng lúc, listener "chết" (object gốc không dùng nữa)
    // vẫn bị GIỮ LẠI trong "listeners" list, không thể bị GC dọn dẹp
}

// 3. Vòng lặp mở resource không đóng (liên hệ Module 04 — try-with-resources)
for (int i = 0; i < 1_000_000; i++) {
    FileInputStream fis = new FileInputStream("file.txt"); // KHÔNG close() → rò rỉ file handle, không phải Heap leak thuần túy nhưng cùng bản chất "quên giải phóng tài nguyên"
}
```

> **Liên hệ thực tế cực kỳ quan trọng — ThreadLocal Leak:** đây là loại memory leak **nổi tiếng khó phát hiện nhất** trong các ứng dụng Spring chạy trên Application Server có Thread Pool cố định (thread được **tái sử dụng** cho nhiều request khác nhau). Nếu dùng `ThreadLocal` để lưu dữ liệu tạm cho 1 request (ví dụ thông tin user đang đăng nhập) mà **quên gọi `threadLocal.remove()`** khi request kết thúc, dữ liệu đó **vẫn còn gắn với thread** đó — khi thread được tái sử dụng cho request **khác**, dữ liệu cũ vẫn còn đó (rủi ro bảo mật — lộ dữ liệu user A cho user B) VÀ Heap dần phình to vì dữ liệu cũ không bao giờ được GC dọn dẹp. Đây là kiến thức nâng cao sẽ gặp lại khi học Spring Security (Module 15) — `SecurityContextHolder` sử dụng `ThreadLocal` nội bộ.

---

## 8. ClassLoader

**ClassLoader** chịu trách nhiệm **nạp (load)** file `.class` (bytecode) vào JVM khi cần dùng đến — theo cơ chế **lazy loading** (chỉ nạp khi lần đầu tiên class đó thực sự được tham chiếu, không nạp hết mọi class ngay khi khởi động).

### 3 loại ClassLoader theo mô hình phân cấp (Delegation Hierarchy)

```
      Bootstrap ClassLoader (nạp các class LÕI của JDK: java.lang.*, java.util.*...)
              │
              ▼
      Platform/Extension ClassLoader (nạp các class MỞ RỘNG của JDK)
              │
              ▼
      Application ClassLoader (nạp class do LẬP TRÌNH VIÊN viết, và các thư viện bên thứ 3 trong classpath)
```

### Nguyên tắc "Parent Delegation Model"

Khi cần nạp 1 class, ClassLoader **luôn ủy quyền (delegate) lên ClassLoader cha trước** — chỉ khi ClassLoader cha **không tìm thấy** class đó, ClassLoader con mới tự nạp:

```
Cần nạp class "com.example.MyClass"
   │
   ▼
Application ClassLoader hỏi Platform ClassLoader trước
   │
   ▼
Platform ClassLoader hỏi Bootstrap ClassLoader trước
   │
   ▼
Bootstrap KHÔNG tìm thấy (vì đây không phải class lõi JDK) → trả lại cho Platform
   │
   ▼
Platform KHÔNG tìm thấy → trả lại cho Application
   │
   ▼
Application ClassLoader TỰ NẠP class này (vì tìm trong classpath của ứng dụng)
```

> **Vì sao thiết kế này quan trọng — Bảo mật:** đảm bảo lập trình viên **không thể** tự viết 1 class giả mạo tên `java.lang.String` để "đánh lừa" hệ thống — vì `Bootstrap ClassLoader` luôn được hỏi TRƯỚC và luôn tìm thấy `java.lang.String` **thật** từ JDK trước khi bất kỳ ClassLoader con nào kịp nạp phiên bản giả mạo.

---

## 9. Quá trình biên dịch & JIT Compiler

### 2 giai đoạn biên dịch của Java

```
1. javac (Ahead-Of-Time, lúc BUILD)
   File.java  ──────►  File.class (bytecode) — CHƯA phải mã máy trực tiếp

2. JVM Execution Engine (lúc CHẠY)
   File.class (bytecode)  ──►  Interpreter (thông dịch từng dòng bytecode)  ──►  Chạy chậm hơn
                          ──►  JIT Compiler (biên dịch "nóng" thành mã máy)  ──►  Chạy NHANH như mã máy gốc
```

### JIT (Just-In-Time) Compiler — tối ưu hiệu năng thông minh

Ban đầu, JVM **thông dịch (interpret)** bytecode từng dòng — chậm nhưng khởi động nhanh. JIT Compiler **theo dõi** những đoạn code được gọi **RẤT NHIỀU LẦN** ("hot code" / "hot spot" — đây cũng chính là nguồn gốc tên gọi **HotSpot JVM**, JVM chuẩn phổ biến nhất của Oracle/OpenJDK), rồi **biên dịch trực tiếp thành mã máy native** ngay tại thời điểm chạy (Just-In-Time), giúp các lần gọi tiếp theo chạy **nhanh gần bằng mã máy thuần** thay vì phải thông dịch lại từ đầu mỗi lần.

```
Lần gọi 1-1000: Interpreter thông dịch bytecode → chậm
Method này được gọi RẤT NHIỀU LẦN → JIT nhận diện là "hot method"
JIT biên dịch method này thành MÃ MÁY NATIVE, lưu cache lại
Lần gọi 1001 trở đi: chạy TRỰC TIẾP mã máy đã biên dịch → NHANH HƠN ĐÁNG KỂ
```

> **Hệ quả thực tế quan trọng — "JVM Warm-up":** đây là lý do các ứng dụng Java/Spring Boot thường **chạy chậm hơn 1 chút ngay sau khi khởi động** (vì JIT chưa kịp "làm nóng", còn đang thông dịch), rồi **dần dần nhanh lên** sau vài phút/vài nghìn request khi JIT đã tối ưu xong các đoạn code "nóng" nhất. Đây cũng là lý do benchmark hiệu năng Java **cần "warm-up" trước** (chạy thử vài nghìn lần bỏ qua kết quả đo, rồi mới bắt đầu đo thật) để có con số phản ánh đúng hiệu năng thực tế lúc production, tránh đo nhầm vào giai đoạn "còn nguội".

---

## 10. Công cụ chẩn đoán JVM thực tế

Không cần thành thạo ngay, nhưng nên **biết tên và mục đích** — sẽ hữu ích khi cần debug production issue thực tế trong công việc sau này:

| Công cụ | Mục đích |
|---|---|
| **JVisualVM** | GUI trực quan — theo dõi Heap, Thread, CPU usage theo thời gian thực |
| **JConsole** | Tương tự JVisualVM, đi kèm sẵn trong JDK |
| **jstat** | Command-line — xem thống kê GC theo thời gian thực (số lần Minor/Major GC, thời gian mỗi lần...) |
| **jmap** | Chụp "ảnh chụp" (heap dump) trạng thái Heap tại 1 thời điểm — dùng để phân tích memory leak |
| **jstack** | Chụp "ảnh chụp" trạng thái của TẤT CẢ thread — dùng để phát hiện Deadlock (liên hệ Module 05.1), thread bị treo |
| **JFR (Java Flight Recorder)** | Công cụ profiling hiệu năng thấp overhead, tích hợp sẵn trong JDK hiện đại — có thể "ghi lại" toàn bộ hoạt động JVM trong 1 khoảng thời gian để phân tích sau |

> **Liên hệ thực tế:** khi 1 service Spring Boot trong production bỗng dưng "đứng hình" định kỳ, hoặc RAM tăng dần không giảm theo thời gian (dấu hiệu memory leak điển hình), quy trình chẩn đoán chuẩn thường là: dùng `jstat` xem tần suất/thời gian GC có bất thường không → dùng `jmap` chụp heap dump → phân tích heap dump (bằng Eclipse MAT hoặc JVisualVM) để tìm ra chính xác **object nào đang chiếm dụng bộ nhớ nhiều nhất và bị giữ lại bởi reference nào** — đây chính là kỹ năng thực chiến bậc Senior, xây dựng trực tiếp trên nền tảng kiến thức của module này.

---

## 11. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| Heap | Dùng chung mọi thread, chứa object; được GC dọn dẹp; lỗi `OutOfMemoryError` |
| Stack | Riêng mỗi thread, chứa local variable/tham số/reference; tự dọn khi method kết thúc; lỗi `StackOverflowError` |
| Truyền tham số Java | "Pass-by-value của reference" — copy giá trị reference, không copy object |
| Garbage Collection | Tự động dọn object "unreachable" (không còn reference nào trỏ tới) |
| Stop-The-World | GC (đặc biệt Major/Full GC) tạm dừng toàn bộ thread ứng dụng — nguồn gốc latency spike |
| Generational GC | Young Gen (Eden+Survivor) dọn thường xuyên+nhanh; Old Gen dọn hiếm+chậm hơn |
| G1 GC | Mặc định từ Java 9, cân bằng throughput/latency, phù hợp đa số ứng dụng backend |
| Memory Leak trong Java | Vẫn xảy ra dù có GC — do reference "quên" không giải phóng (static collection, listener chưa unregister, ThreadLocal chưa remove) |
| ClassLoader | 3 tầng (Bootstrap → Platform → Application), Parent Delegation Model đảm bảo bảo mật |
| JIT Compiler | Biên dịch "hot code" thành mã máy native lúc runtime — lý do có hiện tượng "JVM warm-up" |

---

## 12. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau, biến nào nằm trên Stack, biến/object nào nằm trên Heap?
```java
public void process() {
    int count = 5;
    String name = "Pho";
    List<String> items = new ArrayList<>();
    items.add(name);
}
```

**Câu 2.** Đoạn code sau có gây memory leak không? Giải thích.
```java
public class Registry {
    private static final Map<String, Object> data = new HashMap<>();
    public static void store(String key, Object value) {
        data.put(key, value);
    }
    // KHÔNG có method remove() hay clear() nào được gọi trong toàn bộ vòng đời ứng dụng
}
```

**Câu 3.** Vì sao Minor GC thường nhanh hơn Major/Full GC rất nhiều? Giải thích bằng "Weak Generational Hypothesis".

**Câu 4.** Gọi `System.gc()` trong code có đảm bảo Garbage Collection chạy ngay lập tức không? Vì sao không nên lạm dụng?

**Câu 5.** Giải thích hiện tượng "JVM Warm-up" — vì sao 1 ứng dụng Spring Boot thường chạy chậm hơn ngay sau khi khởi động so với sau khi đã chạy ổn định 1 thời gian?

---

### Phần B — Bài tập viết code (mang tính khảo sát/quan sát, không chỉ viết logic)

**Bài 1 — Tái hiện `StackOverflowError` và `OutOfMemoryError`.**
Viết 1 method đệ quy KHÔNG có base case (liên hệ Module 01.2) để chủ động tái hiện `StackOverflowError`, bắt exception này và in ra thông báo. Sau đó, viết 1 vòng lặp liên tục tạo object lớn (ví dụ mảng `byte[1_000_000]`) và **thêm vào 1 `List` static** (không cho phép GC dọn dẹp) để tái hiện `OutOfMemoryError: Java heap space` (⚠️ lưu ý: nên giới hạn Heap size khi chạy thử bằng cờ `-Xmx64m` để tái hiện nhanh, tránh treo máy quá lâu).

**Bài 2 — Quan sát hành vi Reference bằng code.**
Viết class `Student` có `toString()`. Viết đoạn code minh họa rõ ràng (kèm comment giải thích từng bước) tình huống: 2 biến cùng trỏ đến 1 object, sau đó gán `null` cho 1 biến — object **vẫn còn sống** vì biến còn lại vẫn tham chiếu. Minh họa thêm trường hợp gán `null` cho **cả 2** biến — lúc này object mới thực sự "unreachable", có thể bị GC dọn dẹp (dùng `System.out.println` để trace từng bước, không cần công cụ đo GC thực sự).

**Bài 3 — Đo thời gian JIT Warm-up thực nghiệm.**
Viết 1 method tính toán đơn giản nhưng lặp lại nhiều (ví dụ tính tổng bình phương từ 1 đến n). Gọi method này 3 lần trong vòng `for`, mỗi lần với 10 triệu vòng lặp, đo thời gian bằng `System.nanoTime()` cho từng lần gọi riêng biệt. Quan sát và ghi nhận: lần gọi **đầu tiên** có xu hướng chậm hơn đáng kể so với các lần **sau** (do JIT chưa kịp tối ưu hóa "hot method" này) — đây là bằng chứng thực nghiệm trực tiếp cho khái niệm JVM Warm-up.

**Bài 4 — Tái hiện Memory Leak bằng static Collection.**
Viết class `LeakyCache` với `static List<byte[]> cache`, có method `addEntry()` liên tục thêm mảng `byte[100_000]` vào mà **không bao giờ xóa**. Viết `main` gọi `addEntry()` trong vòng lặp lớn (100,000 lần), theo dõi bộ nhớ sử dụng bằng `Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()` in ra sau mỗi 10,000 lần lặp — quan sát bộ nhớ sử dụng **tăng dần liên tục, không bao giờ giảm** dù GC có chạy — đây chính là dấu hiệu kinh điển của memory leak.

**Bài 5 — Bài toán tổng hợp: So sánh có/không có Memory Leak (bài thực nghiệm quan trọng nhất).**
Viết lại Bài 4 thành 2 phiên bản: (a) phiên bản LEAK như Bài 4, (b) phiên bản đã SỬA — thêm logic giới hạn kích thước cache (ví dụ nếu vượt quá 1000 phần tử thì xóa bớt phần tử cũ nhất, dùng lại kiến thức `LinkedHashMap`/LRU đã học ở Module 03.1 Bài 4, chuyển đổi phù hợp sang `List`). Chạy cả 2 phiên bản, in ra biểu đồ text đơn giản (dùng dấu `*` tỉ lệ với mức bộ nhớ sử dụng) sau mỗi 10,000 lần lặp để **so sánh trực quan** xu hướng bộ nhớ giữa 2 phiên bản — bài tập này giúp thấy rõ **hậu quả cụ thể** và **cách khắc phục thực tế** của memory leak, thay vì chỉ hiểu lý thuyết suông.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. Trên **Stack**: `count` (giá trị primitive), `name` (reference — bản thân biến trỏ tới String), `items` (reference — bản thân biến trỏ tới ArrayList). Trên **Heap**: chuỗi `"Pho"` (String object thực sự, vì `String` là reference type dù có tối ưu qua String Pool — Module 01.1), object `ArrayList` thực sự.
2. **Có, gây memory leak** — `data` là `static Map` (sống suốt vòng đời ứng dụng), liên tục `put()` thêm phần tử mà không bao giờ `remove()`/`clear()` — mọi entry một khi đã thêm vào sẽ **mãi mãi được giữ lại** (vẫn "reachable" qua `data`), Heap sử dụng cho vùng này chỉ tăng, không bao giờ giảm theo thời gian — đúng định nghĩa memory leak trong Java dù có GC.
3. "Weak Generational Hypothesis" — **đa số object chết rất trẻ** (chỉ tồn tại trong 1 method rồi hết dùng). Vì vậy Minor GC chỉ cần quét vùng **Young Generation nhỏ** (Eden+Survivor), và vì đa số object ở đây đã "chết", số lượng object CÒN SỐNG cần copy sang vùng khác rất ít — quá trình quét & copy diễn ra rất nhanh. Ngược lại, Old Generation chứa các object đã "chứng minh" sống lâu, hiếm khi chết — nhưng khi Major GC chạy, nó vẫn phải quét **toàn bộ** vùng này (thường lớn hơn Young Gen nhiều lần) để tìm ra số ít object đã chết, tốn thời gian hơn nhiều.
4. **Không đảm bảo** — `System.gc()` chỉ là 1 "gợi ý" (hint) gửi đến JVM, JVM có toàn quyền **bỏ qua** nếu thấy chưa cần thiết. Không nên lạm dụng vì: (a) không có tác dụng đảm bảo như mong đợi, (b) nếu JVM thực sự tuân theo gợi ý này, có thể vô tình kích hoạt 1 Stop-The-World pause **không cần thiết** vào thời điểm không tối ưu, gây gián đoạn hiệu năng ứng dụng đang chạy bình thường.
5. Ngay sau khi khởi động, JVM **chưa có đủ dữ liệu thống kê** về đoạn code nào được gọi nhiều ("hot code") — Execution Engine phải dùng **Interpreter** (thông dịch từng dòng, chậm hơn) cho hầu hết mọi lời gọi. Sau khi ứng dụng đã xử lý đủ nhiều request/vòng lặp, JIT Compiler dần **nhận diện** được các method/đoạn code "nóng" (được gọi lặp lại rất nhiều), biên dịch chúng thành **mã máy native**, khiến các lần gọi tiếp theo nhanh hơn đáng kể — đây là quá trình "làm nóng" (warm-up) tự nhiên của JVM.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Với `OutOfMemoryError`, nên thêm cờ JVM `-Xmx64m` (giới hạn Heap tối đa 64MB) khi chạy để tái hiện lỗi **nhanh chóng** thay vì phải chờ rất lâu với Heap mặc định (có thể lên đến vài GB tùy máy) — đây cũng là kỹ năng thực tế: biết cách **chủ động giới hạn tài nguyên** để kiểm thử hành vi ứng dụng trong điều kiện khắc nghiệt.
- **Bài 2:** Bài tập này giúp **tự tay quan sát** khái niệm "reachability" (khả năng được tham chiếu tới) một cách trực quan, thay vì chỉ đọc lý thuyết — dù không thể "nhìn thấy" GC dọn dẹp trực tiếp bằng `println` (GC tự quyết định thời điểm), việc hiểu rõ **khi nào 1 object CÓ THỂ bị coi là rác** đã là bước quan trọng nhất.
- **Bài 3:** Kết quả điển hình mong đợi: lần gọi 1 có thể chậm hơn lần gọi 2, 3 vài chục % đến vài lần (tùy độ phức tạp phép tính và cấu hình JVM) — nếu kết quả không rõ rệt trên máy cụ thể, đó cũng là kết quả hợp lệ cần ghi nhận và suy luận (ví dụ: phép tính quá đơn giản, JIT tối ưu quá nhanh ngay từ đầu, hoặc số vòng lặp chưa đủ lớn để tạo khác biệt rõ rệt) — quan trọng là hiểu được **PHƯƠNG PHÁP đo đạc đúng** hơn là chỉ chăm chăm vào 1 con số cụ thể.
- **Bài 4 & 5:** Đây là cặp bài tập quan trọng nhất của module — biến kiến thức lý thuyết trừu tượng về Memory Leak thành **trải nghiệm quan sát được bằng mắt** thông qua số liệu bộ nhớ tăng dần không kiểm soát (Bài 4) và cách khắc phục bằng giới hạn kích thước cache có kiểm soát (Bài 5) — đây chính là kỹ năng chẩn đoán và xử lý sự cố (troubleshooting) cấp độ thực chiến, không chỉ dừng lại ở "biết định nghĩa memory leak là gì".

</details>

---

*File tiếp theo trong lộ trình: **Module 08 — Design Patterns** (Creational, Structural, Behavioral Patterns — nền tảng để đọc hiểu source code Spring Framework).*
