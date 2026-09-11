# Module 07 — JVM Internals

> **Mức độ ưu tiên: Trung bình → Bổ sung** — Không cần thuộc lòng chi tiết cài đặt của từng GC algorithm, nhưng hiểu rõ **Runtime Data Areas** (Heap/Stack/Metaspace/Code Cache), cơ chế **Garbage Collection** ở mức khái niệm, **ClassLoader** và **JIT Compiler** là **điểm khác biệt rõ rệt** giữa lập trình viên chỉ biết "viết code chạy được" và người thực sự hiểu **tại sao** code chạy như vậy — cực kỳ hữu ích khi debug production issue (memory leak, ứng dụng chậm dần, latency spike ngẫu nhiên, `OutOfMemoryError` lúc 3 giờ sáng) mà không có kiến thức này gần như không thể chẩn đoán được.

> **Phạm vi bài này:** kiến trúc bên trong của **HotSpot JVM** ở mức cần cho backend engineer — runtime data areas, bố cục object trong bộ nhớ, cấu trúc Heap theo thế hệ, cơ chế & các collector Garbage Collection, reference types, memory leak trong bộ nhớ được quản lý, ClassLoader (phân cấp + parent delegation + loading/linking/initialization), JIT compiler (tiered compilation + tối ưu + deoptimization), các biến thể `OutOfMemoryError`, cờ JVM & công cụ chẩn đoán, container awareness. **Chỉ nhắc tên, không đi sâu:** `<clinit>` & thứ tự khởi tạo class → Module 01.3; `ExceptionInInitializerError` / `NoClassDefFoundError` chi tiết → Module 01.3 & 04; String pool / interning / Compact Strings → Module 01.1; pass-by-value, Stack vs Heap cơ bản → Module 01.1; `ThreadLocal` leak với thread pool → Module 05.2; `WeakHashMap` → Module 03.1; deprecation của `finalize()` → Module 04; carrier thread của virtual thread → Module 06; cách chọn kích thước thread pool → Module 05.2. **Ngoài phạm vi (chỉ cần biết có tồn tại):** AOT compilation / GraalVM native image; `-Xint` (chỉ thông dịch) / `-Xcomp` (biên dịch ngay); bytecode verifier chi tiết; JVM TI / agent instrumentation.

---

## Mục lục

1. [JVM là gì — bức tranh tổng quan & Runtime Data Areas](#1-jvm-là-gì--bức-tranh-tổng-quan--runtime-data-areas)
2. [Các vùng dữ liệu runtime chi tiết](#2-các-vùng-dữ-liệu-runtime-chi-tiết)
3. [Heap vs Stack — đào sâu](#3-heap-vs-stack--đào-sâu)
4. [Bố cục object trong bộ nhớ (object layout)](#4-bố-cục-object-trong-bộ-nhớ-object-layout)
5. [Cấu trúc Heap theo thế hệ & cơ chế cấp phát](#5-cấu-trúc-heap-theo-thế-hệ--cơ-chế-cấp-phát)
6. [Garbage Collection — khái niệm nền tảng](#6-garbage-collection--khái-niệm-nền-tảng)
7. [Reference types — strong / soft / weak / phantom](#7-reference-types--strong--soft--weak--phantom)
8. [Generational GC — Minor / Major / Full](#8-generational-gc--minor--major--full)
9. [Các GC collector (Serial → G1 → ZGC/Shenandoah)](#9-các-gc-collector-serial--g1--zgcshenandoah)
10. [Memory Leak trong Java & cách chẩn đoán](#10-memory-leak-trong-java--cách-chẩn-đoán)
11. [ClassLoader](#11-classloader)
12. [Quá trình biên dịch & JIT Compiler](#12-quá-trình-biên-dịch--jit-compiler)
13. [OutOfMemoryError, cờ JVM quan trọng & container awareness](#13-outofmemoryerror-cờ-jvm-quan-trọng--container-awareness)
14. [Công cụ chẩn đoán JVM thực tế](#14-công-cụ-chẩn-đoán-jvm-thực-tế)
15. [Tổng kết — Bảng ghi nhớ nhanh](#15-tổng-kết--bảng-ghi-nhớ-nhanh)
16. [Bài tập luyện tập](#16-bài-tập-luyện-tập)

---

## 1. JVM là gì — bức tranh tổng quan & Runtime Data Areas

**JVM (Java Virtual Machine)** là "máy ảo" thực thi bytecode — chính JVM là lý do Java có khẩu hiệu **"Write Once, Run Anywhere"**: code Java biên dịch ra **bytecode** (không phải mã máy trực tiếp), và bytecode chạy được trên **bất kỳ hệ điều hành nào có JVM tương ứng**.

```
   File.java  ──(javac)──►  File.class (bytecode)  ──(JVM)──►  Chạy thực tế trên OS
```

**JVM specification** định nghĩa 3 khối chức năng lớn; **HotSpot** (JVM của Oracle/OpenJDK, phổ biến nhất — tên "HotSpot" đến từ khả năng phát hiện "hot spot" trong code, xem mục 12) là một cài đặt cụ thể của spec đó.

```
┌──────────────────────────────────────────────────────────────────────┐
│                              JVM (HotSpot)                             │
│                                                                        │
│  ┌─────────────────┐   ┌──────────────────────────────────────────┐  │
│  │  ClassLoader     │   │            Runtime Data Areas              │  │
│  │  Subsystem       │   │                                            │  │
│  │  (mục 11)        │   │  DÙNG CHUNG mọi thread:                    │  │
│  │  load → link →   │   │   ┌──────────┐  ┌──────────┐  ┌────────┐ │  │
│  │  initialize      │   │   │   Heap    │  │Metaspace │  │ Code   │ │  │
│  └─────────────────┘   │   │ (mục 3-5) │  │(mục 2)   │  │ Cache  │ │  │
│                          │   └──────────┘  └──────────┘  └────────┘ │  │
│  ┌─────────────────┐   │                                            │  │
│  │ Execution Engine │   │  RIÊNG mỗi thread:                         │  │
│  │  Interpreter +   │   │   ┌──────────┐ ┌────────────┐ ┌─────────┐ │  │
│  │  C1 + C2 (JIT)   │   │   │  Stack    │ │PC Register │ │ Native  │ │  │
│  │  + GC (mục 6-9)  │   │   │ (frames)  │ │            │ │Meth.Stk │ │  │
│  │  (mục 12)        │   │   └──────────┘ └────────────┘ └─────────┘ │  │
│  └─────────────────┘   └──────────────────────────────────────────┘  │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │  JNI  ─── cầu nối tới thư viện native (C/C++), OS syscall           │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

> Điểm mấu chốt cần nhớ ngay: **Heap, Metaspace, Code Cache dùng chung** cho toàn JVM; **Stack, PC Register, Native Method Stack là RIÊNG của từng thread**. Đây là nền tảng để hiểu vì sao object thì cần GC còn local variable thì không, và vì sao tạo nhiều thread lại tốn RAM ngay cả khi chưa làm gì (mỗi thread "thủ sẵn" một Stack).

---

## 2. Các vùng dữ liệu runtime chi tiết

| Vùng | Phạm vi | Chứa gì | Lỗi khi cạn |
|---|---|---|---|
| **Heap** | Dùng chung | Mọi object (`new`), mảng, instance field, và **String pool** (từ Java 7, xem dưới) | `OutOfMemoryError: Java heap space` |
| **Stack** (JVM Stack) | Mỗi thread 1 cái | Chuỗi **stack frame** — mỗi lời gọi method 1 frame: local variable, tham số, toán hạng trung gian, địa chỉ trả về | `StackOverflowError` (đệ quy sâu); `OutOfMemoryError` nếu không cấp nổi Stack cho thread mới |
| **PC Register** | Mỗi thread 1 cái | Địa chỉ bytecode đang thực thi của thread đó (là `undefined` khi đang chạy native method) | — (rất nhỏ, không bao giờ cạn) |
| **Native Method Stack** | Mỗi thread 1 cái | Stack cho code **native** (C/C++) gọi qua JNI | `StackOverflowError` / `OutOfMemoryError` native |
| **Metaspace** | Dùng chung | **Metadata của class**: cấu trúc class, method bytecode, constant pool, annotation, `Klass` object — **KHÔNG phải instance** | `OutOfMemoryError: Metaspace` |
| **Code Cache** | Dùng chung | **Mã máy native** do JIT sinh ra (mục 12) | `CodeCache is full` → JIT tắt, ứng dụng chạy chậm dần |

### Stack — kích thước & `StackOverflowError`

- Mỗi thread có Stack riêng, kích thước mặc định ~512 KB–1 MB (tùy OS/arch), chỉnh bằng **`-Xss`** (ví dụ `-Xss256k`).
- `-Xss` nhỏ → mỗi thread tốn ít RAM hơn (tạo được nhiều thread hơn) nhưng dễ `StackOverflowError` với đệ quy sâu / chuỗi gọi dài.
- `-Xss` lớn → đệ quy sâu hơn được, nhưng **giảm số thread tối đa** (tổng RAM cho Stack = số thread × `-Xss`) → đây là một nguyên nhân của `OutOfMemoryError: unable to create native thread`.

```java
static int depth(int n) { return depth(n + 1); }  // đệ quy vô hạn
// depth(0) → StackOverflowError sau vài nghìn → vài chục nghìn frame (tùy -Xss và kích thước frame)
```

### Metaspace — thay thế PermGen từ Java 8

```
Java 7 trở về trước:  class metadata nằm trong "PermGen" — một vùng CỐ ĐỊNH bên trong Heap
                       → hay gặp "OutOfMemoryError: PermGen space" khi load quá nhiều class
                         (app server redeploy nhiều lần, sinh proxy động, Groovy/Scala...)

Java 8 trở đi:        PermGen bị XÓA. Class metadata chuyển sang "Metaspace" — nằm ở
                       NATIVE MEMORY của OS, MẶC ĐỊNH tự mở rộng theo nhu cầu (không giới hạn cứng)
```

- Chỉnh trần bằng **`-XX:MaxMetaspaceSize=256m`** — nên đặt trong production để một class-loader leak không "ăn" hết RAM máy chủ (nếu không giới hạn, Metaspace phình tới khi OS hết RAM).
- **`OutOfMemoryError: Metaspace`** = đang load/giữ quá nhiều class metadata. Nguyên nhân điển hình: **classloader leak** (mục 10, 11) — redeploy webapp nhiều lần mà classloader cũ không được GC, hoặc sinh vô hạn class động (CGLIB proxy, `Lambda$$`, script engine).

### String pool đã chuyển vào Heap (Java 7)

Trước Java 7, string pool (nơi lưu các `String` literal và kết quả `intern()`) nằm trong PermGen — dễ tràn PermGen nếu `intern()` bừa. **Từ Java 7, string pool nằm trong Heap** → được GC dọn như object thường, và có thể chỉnh số bucket bằng `-XX:StringTableSize`. Chi tiết về interning, Compact Strings (Java 9), `+UseCompactStrings` → **Module 01.1**.

---

## 3. Heap vs Stack — đào sâu

Đã nhắc sơ ở Module 01.1 (pass-by-value) và Module 05.1 (Process vs Thread) — đây là phần trình bày đầy đủ.

| Tiêu chí | **Heap** | **Stack** |
|---|---|---|
| Phạm vi | **1 vùng DUY NHẤT, dùng chung** toàn JVM | **MỖI thread 1 Stack RIÊNG** |
| Lưu gì | Toàn bộ **object** (`new`), instance field, mảng, string pool | **Local variable, tham số, reference** trỏ tới object, toán hạng trung gian |
| Cấp phát | "Tự do" trong vùng cho phép; con trỏ cấp phát nhảy tới (bump-the-pointer) trong Eden (mục 5) | **LIFO** — mỗi lời gọi method đẩy 1 **stack frame**, method kết thúc thì pop frame đó ngay |
| Tốc độ | Chậm hơn (cần đồng bộ đa thread, locality kém hơn) | Rất nhanh (LIFO đơn giản, gần như luôn nằm trong CPU cache) |
| Dọn dẹp bởi | **Garbage Collector** — thời điểm không xác định trước | Tự động khi method kết thúc (pop frame) — **không cần GC** |
| Lỗi | `OutOfMemoryError: Java heap space` | `StackOverflowError` |
| Cờ chỉnh kích thước | `-Xms` (ban đầu) / `-Xmx` (tối đa) | `-Xss` (mỗi thread) |

### Ví dụ — biến nào nằm ở đâu

```java
public class Example {
    private int instanceField = 7;          // nằm TRONG object Example, trên HEAP

    public void method() {
        int x = 10;                          // "x" (primitive) trên STACK của thread hiện tại
        Student s = new Student("Pho");       // "s" (reference) trên STACK; OBJECT Student trên HEAP
        int[] arr = new int[1000];            // "arr" (reference) trên STACK; mảng 1000 int trên HEAP
        method2(x, s);
    }

    public void method2(int x, Student s) {   // tham số trong 1 stack frame MỚI riêng cho method2()
        x = 99;                              // chỉ đổi bản COPY trên frame method2 — "x" của method() không đổi
        s.setName("Long");                    // đổi field BÊN TRONG object trên Heap → method() THẤY thay đổi này
    } // frame method2() bị pop NGAY khi method2() kết thúc
}
```

```
STACK của Thread hiện tại:            HEAP (dùng chung):
┌──────────────────────┐             ┌───────────────────────┐
│ frame: method2        │             │  Student object        │
│  x = 99 (copy)        │             │  { name: "Pho"→"Long" } │◄─┐
│  s = [ref] ───────────┼─────────────┼────────────────────────┘  │
├──────────────────────┤             │  int[1000] { 0,0,... }  │  │
│ frame: method         │             └───────────────────────┘  │
│  x = 10               │                                          │
│  s = [ref] ───────────────────────────────────────────────────────┘
│  arr = [ref] ──────────────► (tới mảng int[1000] ở trên)
└──────────────────────┘
```

> **Pass-by-value của reference:** khi truyền `s` vào `method2`, Java **copy giá trị của reference** (như copy địa chỉ nhà) — không copy object. Nên `s.setName(...)` bên trong `method2` **ảnh hưởng** object gốc (cùng 1 object trên Heap), còn `s = new Student(...)` bên trong `method2` thì **không** (chỉ đổi bản copy reference trên frame method2). Đây là "pass-by-value của reference", không phải "pass-by-reference". Chi tiết → **Module 01.1**.

⚠️ **Escape analysis có thể "phá" mô hình trên:** với JIT bật, một object mà JVM chứng minh được là **không "thoát" khỏi method** có thể **không được cấp phát trên Heap** mà tách thành các biến vô hướng nằm trên Stack/thanh ghi (scalar replacement — mục 12). Mô hình "mọi `new` đều lên Heap" đúng ở mức khái niệm/bytecode, nhưng runtime thực tế tinh vi hơn.

---

## 4. Bố cục object trong bộ nhớ (object layout)

Hiểu một object "nặng" bao nhiêu byte giúp lý giải vì sao `new Integer(1)` tốn 16 byte chứ không phải 4, vì sao Heap 40 GB lại là một "ngưỡng ma thuật", và vì sao thêm 1 field `boolean` đôi khi không làm object to thêm.

```
Một object thường trên HotSpot (64-bit) gồm:

┌─────────────────────────────────────────────────────────────┐
│  OBJECT HEADER                                                │
│  ┌───────────────────────┐  ┌───────────────────────────┐    │
│  │  Mark Word (8 byte)     │  │  Klass Pointer (4 hoặc 8) │    │
│  │  - hashcode (lazy)      │  │  - trỏ tới metadata class │    │
│  │  - GC age bits          │  │    trong Metaspace        │    │
│  │  - lock/biased-lock bits│  │  (4 byte nếu compressed)  │    │
│  └───────────────────────┘  └───────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  INSTANCE DATA — các field (đã sắp xếp lại để giảm padding)   │
├─────────────────────────────────────────────────────────────┤
│  PADDING — chèn cho đủ bội số 8 byte (alignment)              │
└─────────────────────────────────────────────────────────────┘

Mảng có thêm 4 byte lưu LENGTH ngay sau header.
```

- **Object nhỏ nhất** (`new Object()`) = 16 byte: 12 byte header (8 mark + 4 klass nén) + 4 byte padding.
- **`Integer`** = 16 byte: 12 header + 4 (`int value`) → vừa khít, không padding.
- **Alignment 8 byte**: mọi object bắt đầu tại địa chỉ chia hết cho 8 → JVM có thể "nén" con trỏ (xem dưới).

### Compressed Oops (`-XX:+UseCompressedOops`)

"Oop" = *ordinary object pointer* (một reference). Trên máy 64-bit, mỗi reference đáng lẽ 8 byte → tốn RAM và giảm hiệu quả CPU cache.

```
Vì mọi object căn theo 8 byte, 3 bit thấp của địa chỉ LUÔN là 000
→ JVM lưu địa chỉ đã "dịch phải 3 bit" trong 32 bit → biểu diễn được 2^32 × 8 byte = 32 GB Heap
→ reference chỉ tốn 4 byte thay vì 8  ⇒  Heap nhỏ hơn ~20-30%, cache hit tốt hơn
```

- **Bật mặc định** khi `-Xmx ≤ 32 GB` (chính xác hơn: dưới ngưỡng ~32 GB tùy alignment).
- **Vượt 32 GB Heap** → compressed oops **tắt** → mọi reference thành 8 byte → Heap "hiệu dụng" có khi còn **ít hơn** lúc để 31 GB! Đây là lý do lời khuyên kinh điển: hoặc để Heap **dưới 32 GB**, hoặc phải nhảy hẳn lên ~48 GB+ mới bù lại được phần mất.
- Xem layout thật bằng thư viện **JOL** (`org.openjdk.jol`) hoặc `-XX:+PrintGCDetails` cho thông tin vùng nhớ.

> **Field alignment / padding:** JVM (không phải thứ tự khai báo trong source) tự sắp xếp field theo kích thước giảm dần (`long`/`double` → `int`/`float` → `short`/`char` → `byte`/`boolean` → reference) để **giảm padding**. Vì vậy thêm 1 field `boolean` vào "khe trống" padding sẵn có có thể **không làm object to thêm byte nào**.

---

## 5. Cấu trúc Heap theo thế hệ & cơ chế cấp phát

```
┌────────────────────────────────────────────────────────────────────┐
│                               HEAP                                   │
│  ┌───────────────────────────────────┐  ┌───────────────────────┐  │
│  │        Young Generation             │  │   Old Generation       │  │
│  │  ┌──────────┐ ┌──────┐ ┌──────┐   │  │   (Tenured)             │  │
│  │  │   Eden    │ │  S0  │ │  S1  │   │  │                         │  │
│  │  │ (cấp phát │ │(Sur- │ │(Sur- │   │  │  Object sống LÂU, đã     │  │
│  │  │  object   │ │vivor)│ │vivor)│   │  │  qua nhiều Minor GC      │  │
│  │  │  mới ở đây)│ │      │ │      │   │  │  → được "thăng hạng"     │  │
│  │  └──────────┘ └──────┘ └──────┘   │  │                         │  │
│  │   ~8 : 1 : 1 (tỉ lệ mặc định)      │  │  Thường lớn hơn Young    │  │
│  └───────────────────────────────────┘  └───────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
   NGOÀI Heap:  Metaspace (metadata class)  |  Code Cache (mã máy JIT)

   Lưu ý: G1 GC (mục 9) KHÔNG chia Heap thành các dải liên tục như trên
   mà thành hàng nghìn "region" nhỏ, mỗi region đóng vai Eden/Survivor/Old động.
   Mô hình trên là mô hình generational "kinh điển" (Serial/Parallel).
```

| Vùng | Vai trò |
|---|---|
| **Eden** | Nơi **gần như mọi object mới** ra đời (`new`). Đầy Eden → kích hoạt **Minor GC** |
| **Survivor S0 / S1** | Chứa object sống sót qua Minor GC. Luôn có **1 vùng rỗng** — mỗi lần Minor GC copy object sống từ (Eden + survivor đang dùng) sang survivor rỗng, rồi đảo vai |
| **Old / Tenured** | Object đã "sống sót đủ lâu" (đạt **tenuring threshold**, mặc định tối đa 15) hoặc object quá lớn → chuyển thẳng lên đây |
| **Metaspace** | Metadata class (mục 2) — ngoài Heap, native memory |

### Object aging & tenuring threshold

Mỗi object có vài **bit "age"** trong mark word (mục 4). Mỗi lần object sống sót qua 1 Minor GC, age +1. Khi `age ≥ MaxTenuringThreshold` (mặc định 15, chỉnh bằng `-XX:MaxTenuringThreshold`) → object được **promote** lên Old Gen. JVM còn có "dynamic tenuring": nếu Survivor đầy quá nửa, nó hạ ngưỡng để promote sớm hơn.

### Cấp phát nhanh: bump-the-pointer + TLAB

```
Eden là một vùng liền mạch. Cấp phát 1 object =
   1. con trỏ "top" hiện tại chính là địa chỉ object mới
   2. top += kích thước object
   → chỉ 1 phép cộng! Nhanh hơn nhiều so với malloc() của C (phải tìm free list)

Nhưng nếu MỌI thread cùng bump 1 con trỏ "top" → phải CAS/lock → nghẽn.
Giải pháp: TLAB (Thread-Local Allocation Buffer)
   - mỗi thread được "chia" trước một MẢNG NHỎ riêng trong Eden
   - thread bump con trỏ trong TLAB của mình → KHÔNG cần đồng bộ
   - TLAB đầy → xin TLAB mới (lúc này mới cần đồng bộ, hiếm)
```

- Object quá lớn so với TLAB → cấp phát thẳng ngoài TLAB (hoặc thẳng vào Old Gen nếu là "humongous").
- Cờ liên quan: `-XX:+UseTLAB` (mặc định bật), `-XX:+PrintTLAB`.

> **Vì sao cấp phát object trong Java lại "rẻ"?** Nhờ bump-the-pointer + TLAB, `new` một object nhỏ chỉ tốn vài lệnh CPU — rẻ hơn `malloc` của C rất nhiều. Cái đắt là **dọn dẹp về sau** (GC). Đây là lý do "tạo ít rác" (giảm allocation rate) thường hiệu quả hơn "tối ưu từng lần cấp phát".

---

## 6. Garbage Collection — khái niệm nền tảng

**Garbage Collection (GC)** tự động giải phóng object **không còn với tới được (unreachable)** từ chương trình — thay cho `free()` thủ công của C/C++.

### GC roots & reachability analysis

JVM **không** đếm reference (reference counting không xử lý được chu trình A↔B). Thay vào đó: **reachability analysis** — bắt đầu từ tập **GC roots**, đi theo mọi reference; object nào **không** tới được từ bất kỳ root nào → là rác.

**GC roots gồm:**

- **Local variable & tham số** trên Stack của **mọi thread đang sống** (các reference trong stack frame).
- **Static field** của các class đã được load.
- **JNI references** (object được code native giữ).
- **Object đang làm monitor** (bị `synchronized` khóa).
- Các **thread object** đang chạy, một số object hệ thống của JVM.

```java
public void method() {
    Student s = new Student("Pho");  // object reachable qua local var "s" (một GC root path)
    // ... dùng s ...
} // method() kết thúc → "s" biến mất khỏi Stack
  // → không còn root nào tới object Student → UNREACHABLE → có thể bị GC (không nhất thiết NGAY)

Student s1 = new Student("A");
Student s2 = s1;   // s2 cùng trỏ 1 object
s1 = null;         // vẫn reachable qua s2 → CHƯA phải rác
s2 = null;         // giờ mới unreachable
```

### `System.gc()` — chỉ là "gợi ý"

```java
System.gc();  // YÊU CẦU JVM chạy GC — JVM có quyền BỎ QUA (và với -XX:+DisableExplicitGC thì luôn bỏ qua)
```

> ⚠️ **Không gọi `System.gc()` trong code production.** GC là việc JVM tự chọn thời điểm tối ưu; gọi tay thường ép một **Full GC Stop-The-World** không cần thiết, gây latency spike. (Ngoại lệ hiếm: một số framework NIO gọi để dọn `DirectByteBuffer` — xem mục 13.)

### "Stop-The-World" (STW)

Ở nhiều pha, GC phải **tạm dừng TOÀN BỘ thread ứng dụng** tại một **safepoint** để "ảnh chụp" đồ thị object không bị thay đổi giữa chừng. Đây là nguồn gốc các **latency spike ngẫu nhiên** khi Heap đầy dần.

- **Parallel** ≠ **Concurrent**:
  - *Parallel*: nhiều thread GC chạy cùng lúc — **nhưng** ứng dụng vẫn bị dừng (STW).
  - *Concurrent*: thread GC chạy **song song với** ứng dụng (ứng dụng **không** bị dừng, hoặc chỉ dừng rất ngắn).
- Collector hiện đại (G1, ZGC) làm **phần lớn** công việc ở pha concurrent, chỉ để lại vài pha STW cực ngắn.

### Ba họ thuật toán nền

| Thuật toán | Ý tưởng | Ưu / nhược |
|---|---|---|
| **Mark-Sweep** | Đánh dấu object sống → quét xóa object chết | Nhanh, nhưng để lại **phân mảnh** (fragmentation) Heap |
| **Mark-Sweep-Compact** | Như trên + **dồn** object sống về một đầu | Hết phân mảnh, cấp phát lại bằng bump-pointer; nhưng pha compact tốn thời gian |
| **Copying** | Chia đôi vùng; copy object sống sang nửa kia; nửa cũ coi như rỗng hoàn toàn | Rất nhanh khi **tỉ lệ sống thấp** (đúng với Young Gen!), không phân mảnh; nhược: tốn gấp đôi không gian |

Đây chính là lý do thiết kế generational: **Young Gen dùng copying** (tỉ lệ sống thấp → cực nhanh), **Old Gen dùng mark-sweep-compact** (tỉ lệ sống cao → copy sẽ phí).

---

## 7. Reference types — strong / soft / weak / phantom

Mặc định mọi reference bạn viết là **strong**. `java.lang.ref` cho 3 mức "yếu hơn" để hợp tác với GC — dùng cho cache, listener registry, và dọn tài nguyên native.

| Loại | GC dọn object khi... | Dùng để |
|---|---|---|
| **Strong** (`Object o = ...`) | Không bao giờ, chừng nào còn strong ref reachable | Reference thường ngày |
| **`SoftReference`** | Chỉ khi **sắp hết Heap** (GC cố giữ lại càng lâu càng tốt) | **Cache "nhớ được thì tốt"** — memory-sensitive cache |
| **`WeakReference`** | **Ngay lần GC kế tiếp** nếu không còn strong ref | Metadata gắn theo object mà không giữ nó sống: `WeakHashMap` (key yếu — Module 03.1), `ThreadLocal` entry, listener registry |
| **`PhantomReference`** | Object đã finalize xong, sắp bị thu hồi — `get()` **luôn trả `null`** | Biết **chính xác thời điểm** object bị thu hồi để dọn tài nguyên native kèm theo |

```java
// SoftReference — cache co giãn theo áp lực bộ nhớ
SoftReference<byte[]> cached = new SoftReference<>(loadBigBlob());
byte[] blob = cached.get();            // có thể null nếu GC đã dọn khi thiếu RAM
if (blob == null) blob = reload();

// WeakReference + ReferenceQueue — nhận thông báo khi referent bị dọn
ReferenceQueue<Widget> q = new ReferenceQueue<>();
WeakReference<Widget> ref = new WeakReference<>(widget, q);
// ... sau này, ở một thread dọn dẹp:
Reference<?> dead = q.poll();          // != null khi widget đã bị GC → dọn tài nguyên liên quan
```

### `finalize()` đã chết — dùng `Cleaner`

`Object.finalize()` **deprecated for removal** (Module 04): thời điểm chạy không xác định, có thể **không bao giờ chạy**, làm chậm GC, có thể "hồi sinh" object. Thay bằng **`java.lang.ref.Cleaner`** (Java 9) — dựa trên `PhantomReference` + thread dọn riêng:

```java
public class FileHandle implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private final Cleaner.Cleanable cleanable;

    // ⚠️ state phải là class/lambda KHÔNG giữ reference tới FileHandle (nếu không → không bao giờ được dọn)
    private record State(long nativeFd) implements Runnable {
        public void run() { closeNative(nativeFd); }   // chạy khi FileHandle bị GC HOẶC khi close()
    }

    public FileHandle(String path) {
        this.cleanable = CLEANER.register(this, new State(openNative(path)));
    }
    @Override public void close() { cleanable.clean(); }   // dọn NGAY, không chờ GC
}
```

> `Cleaner` là **lưới an toàn (safety net)**, không phải cơ chế chính. Cách đúng vẫn là `try-with-resources` + `close()` tường minh (Module 04). Nhiều class JDK giữ tài nguyên native (`DirectByteBuffer`, `FileInputStream` cũ...) đã chuyển sang `Cleaner` nội bộ.

---

## 8. Generational GC — Minor / Major / Full

JVM dựa trên **"Weak Generational Hypothesis"**: *"đa số object chết rất trẻ"* — biến local, DTO tạm, chuỗi trung gian... chỉ sống trong một method; chỉ số ít (cache, singleton, connection pool) sống lâu.

### Minor GC — dọn Young Generation

```
1. Object mới → Eden (qua TLAB).
2. Eden đầy → Minor GC (STW ngắn):
   - copy object CÒN SỐNG trong (Eden + Survivor-đang-dùng) → Survivor-rỗng, age +1
   - object CHẾT → bị bỏ qua hoàn toàn (không cần "xóa" — vùng cũ coi như trống)
   - đảo vai S0 ↔ S1
   - object đạt tenuring threshold (age 15) hoặc Survivor tràn → PROMOTE lên Old Gen
3. Eden + Survivor-cũ giờ hoàn toàn trống → cấp phát tiếp bằng bump-the-pointer
```

**Đặc điểm:** chạy **thường xuyên** (Eden nhỏ, đầy nhanh) nhưng **rất nhanh** (chỉ copy số ít object sống). STW của Minor GC thường **dưới 1–10 ms** — chấp nhận được với đa số backend.

### Major GC / Full GC — dọn Old Generation

- **Major GC**: dọn Old Gen. Chạy ít hơn (Old Gen lớn, đầy chậm) nhưng **chậm hơn nhiều** — phải quét/compact toàn bộ Old Gen.
- **Full GC**: dọn **cả** Young + Old + (thường) Metaspace, thường **single-threaded fallback** hoặc pha đắt nhất — STW có thể **hàng trăm ms tới vài giây**. Đây là "kẻ thù số 1" của latency.

**Nguyên nhân Full GC thường gặp:**

- Old Gen đầy (promote quá nhanh — "premature promotion" do Survivor nhỏ / allocation rate cao).
- **Promotion failure / concurrent mode failure**: collector concurrent không dọn Old Gen kịp trước khi nó đầy → phải fallback về Full GC STW.
- Gọi `System.gc()`.
- Metaspace đầy.

```
 Tần suất & thời gian điển hình:
  Minor GC:  mỗi vài giây,  STW ~1-10 ms       ← ổn
  Major GC:  mỗi vài phút,  STW ~50-500 ms      ← cần để mắt
  Full GC:   hiếm/không có,  STW 0.5-5 s+        ← thấy thường xuyên = có vấn đề, phải điều tra
```

> **Vì sao 2 tầng tốt hơn quét cả Heap mỗi lần?** Vì rác tập trung ở Young Gen. Minor GC chỉ đụng vùng nhỏ đó, rất thường xuyên nhưng rẻ; hiếm khi phải "động" tới Old Gen. Tổng thời gian GC giảm mạnh so với quét toàn Heap mỗi lần.

---

## 9. Các GC collector (Serial → G1 → ZGC/Shenandoah)

Không cần nhớ chi tiết cài đặt — cần hiểu **triết lý đánh đổi** và **mặc định theo phiên bản Java** để đọc log GC và tham gia thảo luận tuning.

| Collector | Cờ bật | Ý tưởng cốt lõi | Đánh đổi / khi nào dùng |
|---|---|---|---|
| **Serial** | `-XX:+UseSerialGC` | **1 thread** cho toàn bộ GC, STW hoàn toàn | Overhead thấp nhất, đơn giản. Container nhỏ (< 2 CPU, vài trăm MB Heap), CLI ngắn hạn |
| **Parallel** (Throughput) | `-XX:+UseParallelGC` | **Nhiều thread** cho Minor & Major GC, vẫn STW hoàn toàn | Tối đa **throughput** (tổng việc/giây), chấp nhận pause dài hơn. Batch job, ETL, tác vụ nền |
| **CMS** (Concurrent Mark-Sweep) | `-XX:+UseConcMarkSweepGC` | Dọn Old Gen **concurrent** với ứng dụng, giảm pause | **Deprecated Java 9, XÓA HẲN Java 14.** Không phân mảnh → hay "concurrent mode failure" → Full GC. Chỉ gặp ở hệ thống cũ |
| **G1** (Garbage First) | `-XX:+UseG1GC` | Heap chia hàng nghìn **region**; ưu tiên dọn region **nhiều rác nhất trước**; phần lớn concurrent; có compaction từng phần | **MẶC ĐỊNH từ Java 9.** Cân bằng throughput/latency. Đặt mục tiêu pause bằng `-XX:MaxGCPauseMillis=200`. Phù hợp gần như mọi backend Spring Boot |
| **ZGC** | `-XX:+UseZGC` | Concurrent gần như hoàn toàn (colored pointers + load barrier); STW **< 1 ms** bất kể Heap 100 GB hay 16 TB | Ưu tiên tối đa **latency thấp**, đánh đổi ~10-15% throughput & thêm RAM. Sản xuất từ Java 15; "generational ZGC" từ Java 21 |
| **Shenandoah** | `-XX:+UseShenandoahGC` | Tương tự ZGC (concurrent evacuation, Brooks pointer); STW sub-ms | Như ZGC. Có trong OpenJDK build của Red Hat từ lâu, chính thức Java 15 |
| **Epsilon** | `-XX:+UseEpsilonGC` | **Không dọn gì cả** (no-op) | Chỉ để benchmark allocation / test OOM. Không dùng thật |

### G1 — vài điểm đáng biết

```
Heap = 2048+ region bằng nhau (1-32 MB mỗi region tùy Heap size)
Mỗi region tại một thời điểm đóng vai: Eden | Survivor | Old | Humongous (object > 1/2 region)

Chu kỳ: Young GC (STW ngắn) → khi Old chiếm ngưỡng (-XX:InitiatingHeapOccupancyPercent, mặc định 45%)
        → Concurrent Marking (song song ứng dụng) → Mixed GC (dọn Young + vài region Old "nhiều rác nhất")
Nếu G1 không theo kịp → "Full GC" fallback (từ Java 10 là parallel, đỡ hơn trước)
```

- **`-XX:MaxGCPauseMillis`** là *mục tiêu mềm* — G1 tự điều chỉnh kích thước Young Gen để cố đạt, không đảm bảo tuyệt đối. Đặt quá thấp (ví dụ 5 ms) → Young Gen bị teo → GC quá thường xuyên → **giảm throughput**.
- **String deduplication** (`-XX:+UseStringDeduplication`, G1): trong pha GC, phát hiện nhiều `String` khác nhau cùng nội dung → cho chúng **dùng chung 1 mảng `byte[]`** → tiết kiệm Heap cho ứng dụng nhiều chuỗi trùng (log, JSON key).

### GC ergonomics & chọn collector

JVM tự chọn collector + kích thước Heap theo phần cứng ("ergonomics"): mặc định G1 với `-Xmx` = 1/4 RAM khả dụng. Nguyên tắc thực dụng:

- **Mặc định (G1)** cho hầu hết backend — chỉ đổi khi đo được vấn đề cụ thể.
- **Parallel** nếu là batch/throughput thuần, không ai than pause.
- **ZGC/Shenandoah** nếu SLA p99/p999 khắt khe (trading, ad-serving, game backend) và Heap lớn.
- **Serial** trong container bé xíu.

---

## 10. Memory Leak trong Java & cách chẩn đoán

Có GC **không** miễn nhiễm memory leak. Leak trong bộ nhớ được quản lý = object **không còn cần về mặt logic** nhưng **vẫn reachable** từ một GC root → GC không dám dọn. Heap dùng cho vùng đó chỉ tăng → cuối cùng `OutOfMemoryError`.

### Bốn nguyên nhân kinh điển trong backend

```java
// 1. static collection phình mãi, không bao giờ xóa
class CacheManager {
    private static final Map<String, Object> CACHE = new HashMap<>();   // static = GC root, sống suốt đời JVM
    static void put(String k, Object v) { CACHE.put(k, v); }            // chỉ put, không evict → leak
}

// 2. Listener / callback không hủy đăng ký
bus.register(this);      // nếu quên bus.unregister(this) khi component chết
                         // → "listeners" list giữ strong ref → component không bao giờ được GC
                         // Sửa: WeakReference registry, hoặc luôn unregister trong lifecycle hook

// 3. ThreadLocal + thread pool (đọc Module 05.2)
private static final ThreadLocal<HeavyContext> CTX = new ThreadLocal<>();
// worker thread trong pool sống RẤT LÂU; nếu quên CTX.remove() ở finally → HeavyContext
// bám theo thread mãi mãi. Chồng chất qua nhiều request → leak + rò rỉ dữ liệu request cũ.

// 4. Key trong HashMap không có equals()/hashCode() ổn định, hoặc mutable key bị đổi sau khi put
//    → không bao giờ tra ra để remove → entry mắc kẹt.

// 5. classloader leak (mục 11): redeploy webapp, classloader cũ còn 1 static ref từ thư viện
//    (ví dụ ThreadLocal do JDBC driver / logging framework tạo) → toàn bộ class + Metaspace của
//    bản deploy cũ không được GC → "OutOfMemoryError: Metaspace" sau vài lần redeploy.
```

### ⚠️ Cache tự làm không có giới hạn = leak chờ sẵn

`HashMap` làm cache **không có eviction** là nguyên nhân leak phổ biến nhất trong code tự viết. Dùng:

- `LinkedHashMap` với `removeEldestEntry` (LRU thủ công — Module 03.1), hoặc
- Thư viện: **Caffeine** / Guava `Cache` (giới hạn theo size/time/weight), hoặc
- `WeakHashMap` / `SoftReference` value nếu ngữ nghĩa cho phép mất entry.

### Quy trình chẩn đoán leak (thực chiến)

```
1. Xác nhận có leak: theo dõi Old Gen sau mỗi Full GC (jstat -gcutil <pid> 1s).
   Nếu "đáy" (used sau Full GC) TĂNG DẦN theo thời gian → leak thật (khác với chỉ "Heap dao động").

2. Bật tự chụp khi OOM (đặt SẴN trong mọi service production):
   -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/app/

3. Chụp heap dump thủ công lúc nghi ngờ:
   jmap -dump:live,format=b,file=heap.hprof <pid>      (hoặc jcmd <pid> GC.heap_dump heap.hprof)

4. Phân tích bằng Eclipse MAT (Memory Analyzer Tool) — hoặc VisualVM / JProfiler:
   - "Leak Suspects" report
   - "Dominator Tree": object nào GIỮ nhiều Heap nhất (retained heap)
   - "Path to GC Roots" (loại bỏ weak/soft ref): CHÍNH XÁC chuỗi reference nào giữ object sống

5. jmap -histo:live <pid>  → bảng nhanh "class nào nhiều instance / nhiều byte nhất" (không cần dump)
```

> **Đây là kỹ năng Senior điển hình:** dựng lại từ heap dump đúng câu "object X đang bị giữ bởi `static Y.z` → `Map` → ..." rồi vá đúng chỗ giữ reference. Toàn bộ các mục 3–8 ở trên là nền tảng để đọc được report của MAT.

---

## 11. ClassLoader

**ClassLoader** nạp file `.class` (bytecode) vào JVM theo **lazy loading** — chỉ nạp khi class đó **lần đầu được tham chiếu thực sự**.

### Phân cấp (Java 9+)

```
      Bootstrap ClassLoader   (viết bằng C++, "null" trong API Java)
      → nạp class LÕI: java.base module (java.lang.*, java.util.*, ...)
              │  (cha)
              ▼
      Platform ClassLoader    (trước Java 9 gọi là "Extension ClassLoader")
      → nạp các module nền tảng khác của JDK (java.sql, java.xml, crypto provider, ...)
              │  (cha)
              ▼
      Application (System) ClassLoader
      → nạp class trên -classpath / -cp: code của bạn + thư viện bên thứ ba
              │
              ▼
      [Custom ClassLoader]    (tùy chọn — app server, plugin, hot-reload)
```

### Parent Delegation Model

Khi cần nạp class C, một ClassLoader **hỏi cha trước**; chỉ khi **tất cả tổ tiên báo "không có"** thì nó mới tự nạp:

```
loadClass("com.app.Service"):
  App CL  → hỏi Platform CL  → hỏi Bootstrap CL
  Bootstrap: "không phải class lõi" → trả xuống
  Platform:  "không thuộc module JDK" → trả xuống
  App CL:    tìm trên classpath → NẠP
```

> **Vì sao quan trọng — bảo mật & nhất quán:** không ai có thể "tiêm" một `java.lang.String` giả — Bootstrap luôn được hỏi trước và luôn có bản thật. Và mọi ClassLoader con **dùng chung một** `java.lang.Object` do Bootstrap nạp → hệ thống kiểu nhất quán.

### Class identity = (tên đầy đủ + defining ClassLoader)

Hai class **cùng tên đầy đủ** nhưng nạp bởi **hai ClassLoader khác nhau** là **HAI class khác nhau** với JVM:

```java
// object tạo từ classloader A, ép kiểu về class cùng tên nhưng nạp bởi classloader B
Object o = pluginClassLoaderA.loadInstance();
com.api.Plugin p = (com.api.Plugin) o;   // → ClassCastException: com.api.Plugin cannot be cast to com.api.Plugin (!)
```

Đây là gốc của các lỗi "khó tin" trong app server / hệ thống plugin: cùng một tên class, thông báo lỗi trông vô lý, thủ phạm là hai classloader.

### `Class.forName()` vs `ClassLoader.loadClass()`

| | `Class.forName("C")` | `cl.loadClass("C")` |
|---|---|---|
| Có chạy **initialization** (`<clinit>`, gán static, static block)? | **CÓ** (bản 1 tham số) | **KHÔNG** — chỉ load + link, `<clinit>` hoãn tới lần dùng đầu tiên |
| ClassLoader dùng | ClassLoader của class gọi | ClassLoader `cl` cụ thể |
| Điển hình dùng cho | Nạp JDBC driver kiểu cũ (cần chạy static block đăng ký), nạp theo tên cấu hình | Framework tự quản lý vòng đời class |

Dùng `Class.forName("C", false, cl)` để **không** initialize.

### Loading → Linking → Initialization

```
LOADING        đọc bytecode → tạo đối tượng Class trong Metaspace
   │
LINKING
   ├─ Verification   kiểm bytecode hợp lệ & an toàn kiểu (không tràn stack, không ép kiểu bậy)
   ├─ Preparation    cấp phát static field, gán GIÁ TRỊ MẶC ĐỊNH (0/false/null) — CHƯA chạy khởi tạo
   └─ Resolution     (lười) chuyển symbolic reference trong constant pool → reference trực tiếp
   │
INITIALIZATION   chạy <clinit>: gán static field theo code + chạy static {} block
                 → LƯỜI (chỉ khi lần đầu: new, gọi static method/field không hằng, Class.forName 1-arg,
                   khởi tạo subclass)
                 → JVM ĐẢM BẢO chạy ĐÚNG MỘT LẦN, THREAD-SAFE (khóa trên đối tượng Class)
```

Ba tính chất "lazy + once + thread-safe" của `<clinit>` chính là cơ chế đứng sau **Initialization-on-demand Holder idiom** cho singleton (chi tiết → **Module 01.3**). Nếu `<clinit>` ném exception → `ExceptionInInitializerError`, và class bị đánh dấu "erroneous" → mọi lần dùng sau ném `NoClassDefFoundError` (→ Module 01.3 & 04).

### `NoClassDefFoundError` vs `ClassNotFoundException`

| | `ClassNotFoundException` | `NoClassDefFoundError` |
|---|---|---|
| Loại | **checked Exception** | **Error** |
| Khi nào | Nạp class **động** theo tên (`Class.forName`, `loadClass`) mà không tìm thấy | Class **có lúc compile** nhưng **lúc chạy không thấy** định nghĩa; **hoặc** class đó từng khởi tạo lỗi (`<clinit>` ném) trước đó |
| Gợi ý sửa | Sai tên / thiếu jar trong classpath động | Thiếu dependency runtime, xung đột phiên bản, hoặc tìm ExceptionInInitializerError xảy ra **trước** đó trong log |

### Custom ClassLoader — khi nào

- **App server** (Tomcat, JBoss): mỗi webapp một classloader → cách ly, cho phép **undeploy/redeploy** không restart JVM (và là nơi phát sinh **classloader leak** — mục 10).
- **Plugin / OSGi**: nạp/gỡ module lúc chạy.
- **Hot reload** (JRebel, Spring DevTools): vứt classloader cũ, nạp lại class đã đổi.
- Nạp class từ nguồn phi truyền thống: mạng, DB, mã hóa, sinh bytecode lúc chạy (ASM/ByteBuddy).

---

## 12. Quá trình biên dịch & JIT Compiler

### Hai giai đoạn biên dịch

```
1. javac (lúc BUILD)      File.java  ──►  File.class (bytecode) — độc lập nền tảng, CHƯA phải mã máy
2. Execution Engine (lúc CHẠY)
   bytecode ──► Interpreter   : thực thi từng lệnh bytecode — khởi động NGAY, chạy chậm
            ──► JIT Compiler  : dịch method "nóng" → mã máy native, lưu Code Cache — chạy NHANH
```

### Tiered Compilation (mặc định từ Java 8)

HotSpot có **2 JIT compiler**, dùng phối hợp qua 5 "tier":

```
Tier 0: Interpreter — thu thập profiling (số lần gọi, nhánh nào hay đi, kiểu thực tế...)
Tier 1-3: C1 (client compiler) — dịch nhanh, tối ưu nhẹ; tier 2-3 có thêm profiling counter
Tier 4: C2 (server compiler) — dịch chậm, tối ưu SÂU (đây là nơi phần lớn tốc độ đến từ)

Đường điển hình:  Interpreter → C1 (có profiling) → C2
Nếu C2 quá tải:   tạm dừng ở C1 (vẫn nhanh hơn interpreter nhiều)
```

- **Trước Java 8** phải chọn `-client` (chỉ C1) hoặc `-server` (chỉ C2). Nay mặc định dùng cả hai → khởi động nhanh của C1 + đỉnh cao của C2.
- Tắt/ép: `-XX:-TieredCompilation` (chỉ C2), `-XX:TieredStopAtLevel=1` (chỉ C1 — khởi động rất nhanh, hợp CLI / test), `-Xint` (chỉ interpreter), `-Xcomp` (biên dịch mọi thứ ngay — chậm khởi động, hầu như chỉ để test).

### Phát hiện "hot": invocation counter & backedge counter

```
Mỗi method có:  invocation counter  (tăng mỗi lần method được gọi)
                backedge counter    (tăng mỗi lần một vòng lặp "quay lại" đầu vòng)

Vượt ngưỡng (-XX:CompileThreshold, ~10.000 với C2) → xếp hàng cho JIT dịch.

OSR (On-Stack Replacement): nếu MỘT VÒNG LẶP đang chạy nóng lên trong khi method
CHƯA kịp bị gọi đủ nhiều (ví dụ main() có 1 vòng lặp 1 tỉ lần), JIT dịch riêng phần
vòng lặp và "thay thế" ngay khung đang chạy dở → không phải chờ method gọi lại từ đầu.
```

### Các tối ưu JIT quan trọng

| Tối ưu | Làm gì |
|---|---|
| **Inlining** | Chép thân method nhỏ vào chỗ gọi → bỏ chi phí call, và **mở khóa** mọi tối ưu khác. Là tối ưu "mẹ". Method quá lớn (`-XX:MaxInlineSize`, `FreqInlineSize`) sẽ không được inline |
| **Escape Analysis** | Chứng minh object **không thoát** khỏi method/thread → cho phép: **scalar replacement** (không cấp phát object, tách field thành thanh ghi), **stack allocation**, **lock elision** (bỏ `synchronized` trên object cục bộ không ai thấy) |
| **Devirtualization** | Lời gọi ảo (`interface`/`virtual`) mà profiling thấy **thực tế chỉ 1–2 kiểu** → chuyển thành gọi trực tiếp + inline, kèm "guard" kiểm tra kiểu |
| **Loop unrolling** | Nhân bản thân vòng lặp vài lần → giảm chi phí kiểm tra điều kiện, mở đường cho vector hóa (SIMD) |
| **Dead code elimination** | Bỏ code không ảnh hưởng kết quả (kể cả cả một vòng lặp benchmark "rỗng" — xem cảnh báo JMH bên dưới) |
| **Lock coarsening** | Gộp nhiều khối `synchronized` liền nhau trên cùng lock thành một |
| **Null-check / range-check elimination** | Bỏ kiểm tra `null` / chỉ số mảng khi chứng minh được luôn an toàn |

```java
// Escape analysis — ví dụ điển hình
public double distance(double x1, double y1, double x2, double y2) {
    Point a = new Point(x1, y1);      // object "a", "b" KHÔNG thoát khỏi method
    Point b = new Point(x2, y2);
    return Math.hypot(a.x - b.x, a.y - b.y);
}
// Sau JIT + escape analysis: KHÔNG có object Point nào được tạo trên Heap.
// x/y của a,b nằm thẳng trong thanh ghi CPU → zero GC pressure, nhanh như viết bằng biến rời.
```

### Deoptimization — khi JIT "đoán sai"

JIT tối ưu dựa trên **giả định** (assumption) từ profiling: "vòng lặp này luôn nhận `ArrayList`", "class `Payment` chưa có subclass nào" (`-XX:+MonomorphicArrayCheck`, CHA — class hierarchy analysis)... Khi giả định **bị phá vỡ** lúc chạy:

```
- Một class mới được load làm "class Payment giờ có subclass" → mọi code đã inline dựa trên
  "Payment là final trên thực tế" bị VÔ HIỆU
- Một nhánh code trước giờ "chưa từng chạy" (uncommon trap) bỗng chạy
   → JIT DEOPTIMIZE: vứt bản mã máy, quay về interpreter cho method đó, (có thể) profiling lại & dịch lại
```

Deoptimization là **bình thường và cần thiết** (đảm bảo đúng đắn), nhưng **deopt liên tục** (ví dụ code thật sự đa hình cao, hoặc load class động không ngừng) làm ứng dụng "giật" và không bao giờ đạt tốc độ đỉnh. Quan sát bằng `-XX:+PrintCompilation` (thấy dòng có `made not entrant` / `made zombie`).

### JVM Warm-up & vì sao microbenchmark cần JMH

```
Khởi động:      100% interpreter → chậm
Vài giây sau:   C1 vào cuộc → nhanh lên
Vài chục giây / vài nghìn–vạn lần gọi sau:  C2 tối ưu xong hot path → tốc độ ĐỈNH
```

→ Ứng dụng Spring Boot chạy chậm hơn ngay sau khởi động rồi nhanh dần — đó là warm-up, **không** phải bug.

**Đo hiệu năng đoạn code nhỏ mà tự viết vòng lặp + `System.nanoTime()` gần như luôn SAI:**

- Chưa warm-up → đo trúng lúc còn interpreter.
- **Dead code elimination**: JIT thấy kết quả không dùng → xóa luôn vòng lặp → "nhanh vô lý".
- **Constant folding**: input là hằng → JIT tính sẵn kết quả.
- **On-stack replacement** làm số đo lần đầu méo mó.

→ Dùng **JMH (Java Microbenchmark Harness)** — nó tự warm-up, chạy nhiều iteration/fork, dùng `Blackhole` để chặn dead-code elimination, báo cả độ lệch. Bài tập B của module này cố tình cho bạn đo "thủ công" **để thấy sai số**, không phải để tin con số.

> **`final` và JIT:** `final` giúp *người đọc* và giúp compiler `javac` với hằng số biên dịch, nhưng JIT chủ yếu dựa vào **profiling + class hierarchy analysis** chứ không phải từ khóa `final` trên method/field để quyết định inline/devirtualize. Bỏ `final` khỏi method **không** làm nó chậm đi trong thực tế (JIT tự devirtualize nếu chỉ có 1 cài đặt).

---

## 13. OutOfMemoryError, cờ JVM quan trọng & container awareness

### Các biến thể `java.lang.OutOfMemoryError`

| Thông báo | Nghĩa | Hướng điều tra |
|---|---|---|
| `Java heap space` | Heap thật sự đầy — leak, hoặc `-Xmx` quá nhỏ so với tải, hoặc một request nạp dữ liệu khổng lồ | Heap dump (mục 10); kiểm tra query trả quá nhiều dòng, cache vô hạn |
| `GC overhead limit exceeded` | JVM dành **> 98% thời gian** cho GC mà thu lại **< 2%** Heap → bỏ cuộc | Gần như luôn là leak hoặc Heap quá nhỏ; xử như `Java heap space` |
| `Metaspace` | Quá nhiều class metadata | Classloader leak (mục 11), sinh proxy/lambda class vô hạn; đặt `-XX:MaxMetaspaceSize` để "bắt" sớm |
| `unable to create native thread` | OS từ chối tạo thread mới — hết `ulimit -u`, hết RAM cho Stack (số thread × `-Xss`), hoặc chạm `threads-max` | Giảm số thread (dùng pool / virtual thread — Module 05.2, 06), giảm `-Xss`, tăng ulimit |
| `Direct buffer memory` | `ByteBuffer.allocateDirect(...)` (dùng nhiều bởi Netty/NIO) vượt `-XX:MaxDirectMemorySize` | Bộ đệm direct không được giải phóng (chờ GC dọn `DirectByteBuffer` qua `Cleaner`); tăng giới hạn hoặc pool lại buffer |
| `Requested array size exceeds VM limit` | Xin mảng > ~`Integer.MAX_VALUE` phần tử | Bug logic kích thước |

> ⚠️ `OutOfMemoryError` là **`Error`, không phải `Exception`** — đừng `catch (Exception e)` mà mong bắt được, và **đừng** cố "hồi phục" rồi chạy tiếp: JVM lúc đó ở trạng thái bất định. Cách đúng: `-XX:+HeapDumpOnOutOfMemoryError` để lấy bằng chứng, `-XX:+ExitOnOutOfMemoryError` (hoặc `+CrashOnOutOfMemoryError`) để process chết hẳn cho orchestrator restart sạch.

### Bảng cờ JVM nên thuộc

| Cờ | Ý nghĩa |
|---|---|
| `-Xms<size>` / `-Xmx<size>` | Heap ban đầu / tối đa. **Production nên đặt `-Xms` = `-Xmx`** → tránh JVM co giãn Heap (gây pause) và "bất ngờ" hết RAM về sau |
| `-Xss<size>` | Kích thước Stack mỗi thread (mục 2) |
| `-XX:MaxMetaspaceSize=<size>` | Trần Metaspace (mục 2) |
| `-XX:+UseG1GC` / `-XX:+UseZGC` / `-XX:+UseParallelGC` | Chọn collector (mục 9) |
| `-XX:MaxGCPauseMillis=<ms>` | Mục tiêu pause mềm cho G1/ZGC |
| `-XX:+HeapDumpOnOutOfMemoryError` `-XX:HeapDumpPath=<path>` | Tự chụp heap dump khi OOM — **luôn bật trong production** |
| `-XX:+ExitOnOutOfMemoryError` | Thoát process ngay khi OOM (để restart sạch) |
| `-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m` | **Unified logging (Java 9+)** — log GC chi tiết. (Cũ: `-XX:+PrintGCDetails -Xloggc:` đã deprecated) |
| `-XX:+PrintCompilation` | In mỗi lần JIT dịch / deopt một method (mục 12) |
| `-XX:NativeMemoryTracking=summary` | Bật NMT → sau đó `jcmd <pid> VM.native_memory summary` để xem RAM ngoài Heap đi đâu (Metaspace, thread stack, Code Cache, direct buffer, GC struct...) |
| `-XX:+UseStringDeduplication` | Bật string dedup của G1 (mục 9) |
| `-XX:MaxRAMPercentage=<p>` | Đặt `-Xmx` theo **phần trăm RAM khả dụng** — dùng thay `-Xmx` cứng trong container |

### Container awareness (cgroups)

```
Vấn đề (Java 8 đời đầu): JVM đọc /proc/cpuinfo và /proc/meminfo của MÁY CHỦ VẬT LÝ,
không thấy giới hạn cgroup của container → container cấp 512 MB nhưng JVM tưởng có 64 GB
→ đặt -Xmx mặc định = 16 GB → container bị OOM-kill (exit 137) NGAY, khó hiểu.

Sửa: -XX:+UseContainerSupport — MẶC ĐỊNH BẬT từ JDK 10 (và backport JDK 8u191).
JVM đọc đúng cgroup limit (v1 và v2 — cgroup v2 hỗ trợ tốt từ JDK 15+):
   - Bộ nhớ:  dùng -XX:MaxRAMPercentage=75.0 thay cho -Xmx cứng
   - CPU:     Runtime.availableProcessors() trả đúng số CPU cgroup cho phép
              → ảnh hưởng số thread GC, kích thước ForkJoinPool.commonPool, ergonomics
```

- Kiểm tra JVM "nhìn thấy" gì: `java -XX:+PrintFlagsFinal -version | grep -E 'MaxHeapSize|MaxRAMPercentage'` bên trong container.
- Đừng để `-Xmx` = 100% RAM container: chừa chỗ cho Metaspace, thread stack, Code Cache, direct buffer, và bản thân OS. `MaxRAMPercentage=75` là điểm khởi đầu hợp lý cho service nhỏ.

> **Ngoài phạm vi, biết là đủ:** **AOT / GraalVM Native Image** biên dịch sẵn toàn bộ ra file thực thi native → khởi động ~ms, RAM thấp, **không cần warm-up**, nhưng mất JIT đỉnh cao (peak throughput có thể thấp hơn) và hạn chế reflection/dynamic — hợp serverless/CLI. **Project Leyden** đang đưa một phần lợi ích AOT vào HotSpot chuẩn.

---

## 14. Công cụ chẩn đoán JVM thực tế

Không cần thành thạo ngay — **biết tên, biết dùng khi nào**. Tất cả (trừ MAT) đi kèm JDK.

| Công cụ | Dùng để | Ví dụ lệnh |
|---|---|---|
| **`jps`** | Liệt kê tiến trình JVM đang chạy + PID | `jps -lv` |
| **`jstat`** | Thống kê GC theo thời gian thực (đếm & thời gian Minor/Full GC, % từng vùng) | `jstat -gcutil <pid> 1000` (mỗi 1s) |
| **`jmap`** | Chụp heap dump; histogram class | `jmap -histo:live <pid>` · `jmap -dump:live,format=b,file=h.hprof <pid>` |
| **`jstack`** | Thread dump — phát hiện deadlock (Module 05.1), thread bị treo / chờ lock | `jstack <pid>` (chạy 3 lần cách nhau 5s để so) |
| **`jcmd`** | "Dao đa năng" — thay được phần lớn công cụ trên | `jcmd <pid> GC.heap_info` · `GC.run` · `Thread.print` · `VM.native_memory summary` · `GC.heap_dump f.hprof` · `JFR.start` |
| **`jinfo`** | Xem/đổi cờ JVM của tiến trình đang chạy | `jinfo -flags <pid>` |
| **JFR (Java Flight Recorder)** | Profiler overhead cực thấp (< 1–2%) — "ghi hình" allocation, GC, lock contention, method sampling, I/O trong một khoảng thời gian | `jcmd <pid> JFR.start duration=120s filename=rec.jfr` |
| **JMC (JDK Mission Control)** | GUI phân tích file `.jfr` từ JFR | mở `rec.jfr` |
| **VisualVM** | GUI: theo dõi Heap/thread/CPU realtime, chụp & xem heap dump, sampler đơn giản | tải rời (trước đây kèm JDK 8) |
| **`jconsole`** | GUI JMX cơ bản — Heap, thread count, class loaded, MBean | `jconsole` |
| **Eclipse MAT** | Phân tích heap dump chuyên sâu: Dominator Tree, Path to GC Roots, Leak Suspects (mục 10) | GUI, tải rời |

> **Quy trình chẩn đoán chuẩn khi service "chậm dần / đứng hình / RAM tăng":**
> 1. `jstat -gcutil <pid> 1s` — GC có bất thường không? (Full GC liên tục? Old Gen sau Full GC tăng dần?)
> 2. Nếu nghi CPU/lock: `JFR.start duration=120s` rồi mở bằng JMC — xem hot method, lock contention, allocation hotspot.
> 3. Nếu nghi leak: `-XX:+HeapDumpOnOutOfMemoryError` (đã bật sẵn) hoặc `jmap -dump:live` → mở MAT → Leak Suspects → Path to GC Roots.
> 4. Nếu nghi treo/deadlock: `jstack <pid>` ×3 → tìm `BLOCKED` / `Found one Java-level deadlock`.
> 5. RAM tổng cao nhưng Heap ổn: bật `-XX:NativeMemoryTracking` → `jcmd VM.native_memory summary` (thủ phạm hay gặp: quá nhiều thread × stack, Metaspace, direct buffer).

---

## 15. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| Runtime data areas | **Dùng chung:** Heap, Metaspace, Code Cache. **Riêng mỗi thread:** Stack (frames), PC Register, Native Method Stack |
| Stack | `-Xss` mỗi thread; đệ quy sâu → `StackOverflowError`; nhiều thread × `-Xss` lớn → `OOM: unable to create native thread` |
| Metaspace | Thay PermGen (Java 8), ở **native memory**, tự mở rộng — nên đặt `-XX:MaxMetaspaceSize`; đầy = classloader leak / sinh class vô hạn |
| String pool | Nằm trong **Heap từ Java 7** (trước đó ở PermGen) |
| Object layout | Header = mark word (8B) + klass pointer (4B nén / 8B); + instance data + padding (bội số 8). Object nhỏ nhất 16B |
| Compressed oops | Ref 4B thay vì 8B khi Heap ≲ 32 GB. **Vượt 32 GB → tắt → có khi tệ hơn**. Để < 32 GB hoặc ≥ ~48 GB |
| Heap thế hệ | Young (Eden + S0 + S1, tỉ lệ ~8:1:1) + Old. Tenuring threshold mặc định 15 → promote lên Old |
| Cấp phát | Bump-the-pointer trong Eden + **TLAB** (buffer riêng mỗi thread, không cần đồng bộ) → `new` rất rẻ; cái đắt là GC |
| Reachability | Từ **GC roots** (stack locals, static field, JNI ref, thread đang chạy) đi ra; không tới được = rác. Không phải reference counting |
| `System.gc()` | Chỉ là gợi ý; **không gọi trong production** — dễ ép Full GC STW |
| Parallel vs Concurrent | Parallel = nhiều thread GC nhưng **vẫn STW**; Concurrent = GC chạy **cùng lúc với ứng dụng** |
| Minor vs Full GC | Minor: Young, thường xuyên, ~1–10 ms. Full: cả Heap, hiếm, 0.5–5 s+ — thấy nhiều = có vấn đề |
| Reference types | strong / **soft** (cache, dọn khi gần OOM) / **weak** (`WeakHashMap`, `ThreadLocal`, dọn ngay GC kế) / **phantom** (`Cleaner`, `get()`=null) |
| finalize → Cleaner | `finalize()` deprecated for removal; dùng `Cleaner` + `AutoCloseable`; state không được giữ ref tới object gốc |
| GC collectors | **Serial** (nhỏ) · **Parallel** (throughput/batch) · **CMS** (đã xóa Java 14) · **G1** (mặc định Java 9+, region, `MaxGCPauseMillis`) · **ZGC/Shenandoah** (concurrent, STW < 1 ms, Heap khổng lồ) |
| Memory leak (có GC) | Object hết cần nhưng còn reachable: static collection, listener chưa unregister, `ThreadLocal` + pool, cache không eviction, classloader leak |
| Chẩn đoán leak | `jstat` xem đáy Old Gen tăng dần → heap dump (`jmap`/`jcmd`) → Eclipse MAT → Dominator Tree + Path to GC Roots |
| ClassLoader phân cấp | Bootstrap → Platform → Application → (Custom). **Parent delegation**: hỏi cha trước → bảo mật, không trùng class lõi |
| Class identity | (tên đầy đủ **+ defining classloader**) → cùng tên khác loader = khác class → `ClassCastException` "vô lý" |
| `Class.forName` vs `loadClass` | `forName` (1-arg) **chạy `<clinit>`**; `loadClass` thì không |
| load → link → init | link = verify + prepare (static = giá trị mặc định) + resolve. init = `<clinit>` **lazy, once, thread-safe** (→ Module 01.3) |
| `NoClassDefFoundError` vs `ClassNotFoundException` | Error (có lúc compile, thiếu lúc chạy / init lỗi trước đó) vs checked Exception (nạp động không thấy) |
| JIT tiered | Interpreter (profiling) → **C1** (nhanh, tối ưu nhẹ) → **C2** (chậm, tối ưu sâu). Mặc định từ Java 8 |
| Hot detection | invocation counter + backedge counter; **OSR** thay khung vòng lặp đang chạy dở |
| Tối ưu JIT | **inlining** (mẹ của mọi tối ưu) · **escape analysis** → scalar replacement / stack alloc / lock elision · devirtualization · loop unrolling · dead code elimination |
| Deoptimization | JIT tối ưu theo giả định (profiling, class hierarchy); giả định vỡ (load class mới, uncommon trap) → vứt mã máy, về interpreter. Bình thường; deopt liên tục = xấu |
| Warm-up / JMH | Chậm lúc mới khởi động là bình thường. Đo microbenchmark thủ công gần như luôn sai (dead code elim, constant fold) → dùng **JMH** |
| OOM variants | `Java heap space` · `GC overhead limit exceeded` · `Metaspace` · `unable to create native thread` · `Direct buffer memory`. Là **Error** — đừng catch để chạy tiếp |
| Container awareness | `-XX:+UseContainerSupport` (mặc định JDK 10+): đọc đúng cgroup. Dùng `-XX:MaxRAMPercentage=75` thay `-Xmx` cứng; `availableProcessors()` theo cgroup |
| Cờ luôn bật production | `-Xms`=`-Xmx`, `-XX:+HeapDumpOnOutOfMemoryError` + `HeapDumpPath`, `-Xlog:gc*`, `-XX:+ExitOnOutOfMemoryError` |
| Công cụ | `jps` · `jstat -gcutil` · `jmap -histo/-dump` · `jstack` · `jcmd` (đa năng) · JFR + JMC · Eclipse MAT |

---

## 16. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Với đoạn code sau, biến/đối tượng nào nằm trên Stack, cái nào trên Heap? `header` của object `Order` gồm những gì?
```java
public void process() {
    int count = 5;
    String code = "PO-01";
    Order order = new Order(count);
    int[] lines = new int[count];
    validate(order, lines);
}
```

**Câu 2.** Đoạn này có gây memory leak không? Là biến thể `OutOfMemoryError` nào nếu chạy đủ lâu? Vì sao đặt `-XX:MaxMetaspaceSize` lại giúp *phát hiện sớm*?
```java
public class ProxyFactory {
    private static final List<Class<?>> GENERATED = new ArrayList<>();
    public static Object create() {
        Class<?> proxy = new ByteBuddy().subclass(Object.class)
            .make().load(new URLClassLoader(new URL[0])).getLoaded();
        GENERATED.add(proxy);         // giữ mãi
        return proxy.getDeclaredConstructor().newInstance();
    }
}
```

**Câu 3.** Vì sao Minor GC thường nhanh hơn Full GC rất nhiều? Giải thích qua "Weak Generational Hypothesis" **và** thuật toán copying vs mark-sweep-compact.

**Câu 4.** `System.gc()` có đảm bảo GC chạy ngay không? Nêu 2 lý do không nên gọi trong production, và 1 trường hợp framework hạ tầng vẫn gọi nó có chủ đích.

**Câu 5.** Giải thích "JVM warm-up" qua tiered compilation (Interpreter → C1 → C2). Vì sao đo hiệu năng một method nhỏ bằng vòng `for` + `System.nanoTime()` gần như luôn cho số sai? Kể 2 cơ chế JIT làm hỏng phép đo đó.

**Câu 6.** Hai class cùng tên đầy đủ `com.api.Plugin`, một do Application ClassLoader nạp, một do `URLClassLoader` của plugin nạp. Gán đối tượng loại này cho biến kiểu kia → lỗi gì? Thông báo lỗi trông "vô lý" thế nào? Vì sao?

**Câu 7.** `Class.forName("com.mysql.cj.jdbc.Driver")` và `appClassLoader.loadClass("com.mysql.cj.jdbc.Driver")` khác nhau ở điểm nào về mặt *initialization*? Với JDBC driver kiểu cũ, điều đó vì sao quan trọng?

**Câu 8.** Heap của một service để `-Xmx31g` chạy tốt. Đồng nghiệp tăng lên `-Xmx36g` "cho chắc" thì throughput lại *giảm* và RAM dùng *tăng vọt*. Giải thích bằng compressed oops.

---

### Phần B — Bài tập viết code

**Bài 1 — Tái hiện & phân loại các `OutOfMemoryError`.**
Viết chương trình có 3 chế độ (chọn qua `args[0]`):
(a) đệ quy không base case → bắt `StackOverflowError`, in độ sâu đạt được; chạy lại với `-Xss256k` và `-Xss4m`, so sánh.
(b) `List<byte[]>` static, liên tục `add(new byte[1_000_000])` → `OutOfMemoryError: Java heap space`; chạy với `-Xmx64m -XX:+HeapDumpOnOutOfMemoryError` và xác nhận file `.hprof` được tạo.
(c) vòng lặp tạo `URLClassLoader` mới + nạp một class mỗi vòng, giữ vào `List` static → `OutOfMemoryError: Metaspace`; chạy với `-XX:MaxMetaspaceSize=32m`.
Với mỗi chế độ, ghi lại cờ JVM đã dùng và thông báo lỗi chính xác.

**Bài 2 — Quan sát reachability & reference types.**
(a) Viết `Student` có `toString()`. Minh họa bằng `println` từng bước: 2 biến trỏ 1 object → gán `null` 1 biến (còn sống) → gán `null` cả 2 (unreachable).
(b) Tạo `SoftReference<byte[]>` giữ mảng 10 MB. In `ref.get() != null`. Rồi liên tục cấp phát `byte[]` vào một list cho tới khi gần `-Xmx`, in lại `ref.get()` — quan sát nó chuyển sang `null` (GC đã hi sinh soft ref để cứu Heap).
(c) Tạo `WeakReference<Object>` + `ReferenceQueue`; xóa strong ref; gọi `System.gc()`; `poll()` queue trong vòng lặp có timeout → in ra khi weak ref đã bị enqueue.

**Bài 3 — Đo "warm-up" thực nghiệm (và giải thích vì sao số đo không đáng tin).**
Viết method `long sumSquares(int n)`. Gọi trong vòng lặp 20 lần, mỗi lần `n = 20_000_000`, đo `System.nanoTime()` từng lần và in. Chạy 2 lần: bình thường, và với `-XX:+PrintCompilation` (quan sát dòng dịch `sumSquares` và `::sumSquares @ ... (OSR)`). Viết đoạn nhận xét: lần đầu chậm hơn bao nhiêu; từ lần thứ mấy thì ổn định; **2 lý do** số này vẫn không nên đem đi báo cáo (gợi ý: kết quả có được dùng không? input có phải hằng không?).

**Bài 4 — Tái hiện & sửa memory leak bằng cache.**
(a) `LeakyCache`: `static Map<Integer, byte[]>`, `put(i, new byte[100_000])` trong vòng lặp 1..500_000, **không eviction**. Mỗi 20_000 vòng in `used = totalMemory - freeMemory` và số lần Full GC (đọc qua `ManagementFactory.getGarbageCollectorMXBeans()`). Quan sát `used` sau mỗi Full GC vẫn tăng dần.
(b) `BoundedCache`: dùng `LinkedHashMap` override `removeEldestEntry` giới hạn 1000 entry (LRU — Module 03.1). Chạy cùng vòng lặp, in cùng số liệu. Vẽ biểu đồ text (`*` tỉ lệ với `used`) cho cả (a) và (b) cạnh nhau.

**Bài 5 — Chứng minh class identity phụ thuộc ClassLoader.**
Viết `Widget` (class thường). Trong `main`: nạp `Widget` hai lần bằng hai `URLClassLoader` độc lập trỏ cùng thư mục `target/classes`. In `w1.getClass() == w2.getClass()` (false) và `w1.getClass().getClassLoader()` của mỗi bên. Thử `(Widget) instanceFromLoader2` → bắt `ClassCastException`, in `getMessage()` (sẽ thấy "Widget cannot be cast to Widget"). Giải thích trong comment.

**Bài 6 — Escape analysis: đo tác động.**
Viết `hot()` gọi 100 triệu lần một hàm tạo object nhỏ cục bộ không thoát (ví dụ `new int[]{a,b}` hoặc một `record Pair`). Đo thời gian & (nếu bật JFR / `-Xlog:gc`) số lần Minor GC. Chạy lại với `-XX:-DoEscapeAnalysis`. So sánh: bản tắt escape analysis chạy chậm hơn và Minor GC nhiều hơn (do phải cấp phát thật trên Eden). Ghi nhận kết quả.

---

### Phần C — Nâng cao

**Câu 1.** Trình bày đầy đủ các **runtime data areas** theo JVM spec, đánh dấu vùng nào dùng chung / vùng nào riêng mỗi thread, và biến thể `OutOfMemoryError`/`Error` tương ứng khi mỗi vùng cạn. PermGen → Metaspace (Java 8) đã thay đổi **cái gì về vị trí bộ nhớ và hành vi mở rộng**, và vì sao string pool được chuyển ra khỏi PermGen ở Java 7?

**Câu 2.** Giải thích **compressed oops**: cơ chế "dịch 3 bit" hoạt động thế nào nhờ alignment 8 byte, vì sao ngưỡng ~32 GB, và vì sao Heap 33–45 GB có thể cho *ít* bộ nhớ hiệu dụng hơn 31 GB. Object header gồm những gì; `identityHashCode` được lưu ở đâu và "lazy" nghĩa là gì. Vì sao thêm một field `boolean` đôi khi không làm object to thêm?

**Câu 3.** So sánh **copying collector** (Young Gen) với **mark-sweep-compact** (Old Gen): mỗi cái tối ưu cho tỉ lệ sống nào, chi phí không gian & thời gian ra sao. Mô tả một chu kỳ **Minor GC** đầy đủ: vai trò của 2 Survivor, object aging, tenuring threshold (kể cả "dynamic"), và khi nào object đi thẳng lên Old Gen. **TLAB** giải quyết vấn đề gì của bump-the-pointer, và điều gì xảy ra khi TLAB đầy?

**Câu 4.** G1: giải thích mô hình **region**, ý nghĩa "garbage first", các pha (young-only → concurrent marking → mixed), "humongous object", và vì sao `-XX:MaxGCPauseMillis` chỉ là *mục tiêu mềm* — đặt quá thấp gây hại gì? So sánh triết lý G1 với **ZGC/Shenandoah** (colored pointers / Brooks pointer, load/read barrier, STW sub-ms): đánh đổi throughput & footprint để lấy gì? Khi nào bạn thực sự cần chuyển từ G1 sang ZGC?

**Câu 5.** Bốn reference type: với mỗi loại nêu **chính xác** thời điểm GC được phép thu hồi referent, một use-case đúng, và một cách dùng sai. Vì sao `finalize()` bị deprecated for removal (kể 3 vấn đề cụ thể)? Viết khung một class dùng `Cleaner` đúng cách và giải thích vì sao *state object* truyền cho `Cleaner.register` **tuyệt đối không được** giữ reference tới instance đang được đăng ký.

**Câu 6.** **Parent delegation model**: mô tả trình tự khi `loadClass` được gọi, 2 lợi ích (bảo mật + nhất quán kiểu), và một tình huống nó *gây khó* (ví dụ SPI / JDBC driver cần "context classloader" để lách delegation — giải thích tại sao). **Class identity** = (tên + defining loader): dựng một kịch bản app-server redeploy dẫn tới **classloader leak** qua một `ThreadLocal` do thư viện tạo, và vì sao hậu quả là `OutOfMemoryError: Metaspace` chứ không phải `Java heap space`.

**Câu 7.** **Tiered compilation**: vai trò C1 vs C2, ý nghĩa 5 tier, invocation vs backedge counter, và **OSR** — cho một ví dụ code mà không có OSR thì method sẽ mãi chạy chậm. **Deoptimization**: 2 nguyên nhân (class hierarchy invalidation, uncommon trap), điều gì xảy ra với khung đang chạy, và vì sao "deopt là tính năng, không phải lỗi" — nhưng deopt lặp vô hạn thì sao. Vì sao **escape analysis** cho phép *scalar replacement* và *lock elision*; viết một ví dụ mà lock elision loại bỏ hoàn toàn `synchronized`.

**Câu 8.** Bạn nhận một service Spring Boot: RAM container (limit 1 GB) leo dần tới bị OOM-kill (exit 137) sau ~6 giờ, nhưng `-Xmx512m` và Heap (qua `jstat`) trông ổn định, không có `OutOfMemoryError` trong log. Đưa ra giả thuyết (gợi ý: bộ nhớ **ngoài Heap**), và quy trình xác nhận từng bước bằng công cụ JDK (`-XX:NativeMemoryTracking`, `jcmd VM.native_memory`, `jstack` đếm thread, kiểm tra direct buffer / Metaspace / thread stack). Vì sao `-XX:MaxRAMPercentage` + `-XX:+UseContainerSupport` liên quan ở đây?

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. **Stack** (trong frame của `process`): `count` (primitive `int`), `code` (reference tới `String`), `order` (reference), `lines` (reference). **Heap**: object `Order` (và `int count` field bên trong nó), mảng `int[5]`, chuỗi `"PO-01"` (String object; literal này còn được tham chiếu từ string pool nằm trong Heap). Header của `Order` = **mark word** (8 byte: identity hashcode lazy, GC age bits, lock bits) + **klass pointer** (4 byte nếu compressed oops, trỏ tới metadata `Order` trong Metaspace); sau header là instance data rồi padding cho tròn bội số 8.
2. **Có leak.** `GENERATED` là `static` (GC root) giữ strong ref tới mọi `Class` sinh ra; mỗi `Class` lại giữ sống `URLClassLoader` đã định nghĩa nó và toàn bộ metadata class đó trong **Metaspace**. Chạy đủ lâu → `OutOfMemoryError: Metaspace` (không phải `Java heap space` — cái phình là vùng metadata native, không phải Heap object). Đặt `-XX:MaxMetaspaceSize=...` biến "phình vô hạn tới khi hết RAM máy chủ (khó chẩn đoán, OOM-kill)" thành "ném `OOM: Metaspace` sớm + heap dump" → phát hiện & quy trách nhiệm nhanh hơn nhiều.
3. **Weak Generational Hypothesis**: đa số object chết rất trẻ → Young Gen có **tỉ lệ sống rất thấp**. Minor GC dùng **copying**: chỉ chép số ít object *còn sống* sang Survivor rồi coi cả Eden là trống — chi phí tỉ lệ với *object sống*, gần như không phụ thuộc lượng rác → cực nhanh, và không phân mảnh. Full GC xử lý Old Gen có **tỉ lệ sống cao**; copying sẽ phí nên dùng **mark-sweep-compact**: phải quét *toàn bộ* Old Gen để đánh dấu, rồi *dồn* object sống để hết phân mảnh — cả hai pha đắt, trên vùng thường lớn hơn Young nhiều lần → chậm hơn hẳn.
4. **Không đảm bảo** — chỉ là hint, JVM được phép bỏ qua (và `-XX:+DisableExplicitGC` khiến nó luôn bị bỏ qua). Không dùng vì: (i) không có tác dụng chắc chắn; (ii) nếu JVM nghe theo, thường kích hoạt **Full GC Stop-The-World** vào thời điểm không tối ưu → latency spike nhân tạo. Trường hợp hạ tầng gọi có chủ đích: thư viện NIO (ví dụ quản lý `DirectByteBuffer`) đôi khi gọi `System.gc()` để ép dọn các `Cleaner`/`PhantomReference` giải phóng **bộ nhớ direct ngoài Heap** trước khi ném `OutOfMemoryError: Direct buffer memory`.
5. Khởi động: 100% **Interpreter** (chạy từng bytecode, chậm) đồng thời thu thập profiling. Đủ "nóng" → **C1** dịch nhanh ra mã máy tối ưu nhẹ (nhanh hơn nhiều). Nóng hơn nữa → **C2** dịch lại với tối ưu sâu (inlining, escape analysis...) → tốc độ đỉnh. Vì vậy các lần gọi/giây đầu chậm rồi tăng dần. Đo bằng `for` + `nanoTime` sai vì: (i) **dead code elimination** — kết quả không dùng thì JIT xóa luôn cả vòng lặp → "0 ns"; (ii) **constant folding / loop-invariant hoisting** — input là hằng thì JIT tính sẵn; cộng thêm chưa warm-up (đo trúng interpreter) và OSR làm méo lần đo đầu.
6. `ClassCastException`. Thông báo dạng `class com.api.Plugin cannot be cast to class com.api.Plugin` — *trông như* ép một kiểu sang chính nó. Vì **class identity = (tên đầy đủ + defining classloader)**: hai lần nạp bởi hai loader khác nhau tạo ra **hai `Class` khác nhau**, không tương thích gán cho nhau, dù cùng tên và cùng bytecode. (Thông báo hiện đại còn in kèm loader của mỗi bên để bớt "vô lý".)
7. `Class.forName("X")` (dạng 1 tham số) **chạy initialization** — `<clinit>`, gán static, static block. `loadClass("X")` chỉ **load + link**, hoãn `<clinit>` tới lần dùng thật đầu tiên. JDBC driver kiểu cũ (JDBC < 4.0) **đăng ký chính nó với `DriverManager` bên trong một `static {}` block** → phải để block đó chạy, nên bắt buộc `Class.forName` chứ không phải `loadClass`. (JDBC 4.0+ có `ServiceLoader`/`META-INF/services` nên thường không cần `Class.forName` nữa.)
8. Ở `-Xmx31g`, Heap dưới ngưỡng ~32 GB → **compressed oops bật** → mỗi reference chỉ 4 byte → Heap chứa được nhiều object hơn, cache CPU hiệu quả hơn. Ở `-Xmx36g`, vượt ngưỡng → **compressed oops tắt** → mọi reference thành 8 byte → phần Heap "hiệu dụng" cho dữ liệu thật *giảm*, footprint & áp lực cache *tăng* → GC nhiều hơn, throughput giảm. Đó là lý do lời khuyên: giữ Heap **dưới ~32 GB**, hoặc nếu cần hơn thì phải nhảy hẳn lên ~48 GB+ mới bù lại được phần mất.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** (a) độ sâu đạt được tỉ lệ (thô) với `-Xss` / kích thước frame — `-Xss256k` dừng sớm hơn `-Xss4m` nhiều; thông báo `java.lang.StackOverflowError` (không có message). (b) `java.lang.OutOfMemoryError: Java heap space`; với `-XX:+HeapDumpOnOutOfMemoryError` sẽ thấy dòng `Dumping heap to java_pid<pid>.hprof ...` và file được tạo. (c) `java.lang.OutOfMemoryError: Metaspace`. Bài học: **cùng là "hết bộ nhớ" nhưng ba vùng khác nhau, ba thông báo khác nhau, ba hướng xử lý khác nhau** — luôn đọc kỹ dòng sau dấu `:`.
- **Bài 2:** (a) trace "reachability" bằng mắt: object chỉ thành rác khi **không còn root path nào** — gán `null` một biến chưa đủ. (b) `SoftReference` bị dọn **chỉ khi JVM sắp OOM** → chứng minh khác biệt với `WeakReference`; đây là ngữ nghĩa "memory-sensitive cache". (c) `ReferenceQueue.poll()` trả về `Reference` đã enqueue **sau khi** referent được xác định là weakly-reachable và bị thu hồi — nền tảng cách `WeakHashMap`/`Cleaner` "biết" để dọn entry. Lưu ý cần `-Xmx` đủ nhỏ (ví dụ `-Xmx128m`) để (b) tái hiện nhanh.
- **Bài 3:** Điển hình: lần 1 chậm hơn các lần sau vài chục % đến vài lần; ổn định sau ~5–10 lần gọi (khi C2 xong). Với `-XX:+PrintCompilation` thấy dòng compile `...sumSquares` và bản `(OSR)` cho vòng lặp bên trong. Hai lý do không đem số đi báo cáo: (i) nếu kết quả `sumSquares` không được tiêu thụ (in / cộng dồn / `Blackhole`), JIT có thể **loại bỏ dead code** → số vô nghĩa; (ii) `n = 20_000_000` là **hằng** → constant folding / loop-invariant có thể tính sẵn phần lớn. → Đây là lý do tồn tại **JMH** (warm-up, fork, `Blackhole`, đo phân phối).
- **Bài 4:** (a) `used` sau mỗi Full GC (đáy) **tăng đơn điệu** — chữ ký của leak. Số `GarbageCollectorMXBean.getCollectionCount()` cho collector Old tăng dần rồi cuối cùng OOM. (b) `BoundedCache` giữ `used` **dao động quanh một mức phẳng** (≤ ~1000 × 100 KB + overhead) bất kể chạy bao lâu. Biểu đồ `*` cạnh nhau cho thấy rõ "dốc lên mãi" vs "răng cưa quanh trần" — chính là hình ảnh bạn sẽ thấy trên Grafana khi phân biệt leak thật với Heap chỉ dao động.
- **Bài 5:** `w1.getClass() == w2.getClass()` → `false`; hai `getClassLoader()` là hai đối tượng `URLClassLoader` khác nhau. `(Widget) objFromLoader2` ném `ClassCastException` với message kiểu `class Widget cannot be cast to class Widget (Widget is in unnamed module of loader ...; Widget is in unnamed module of loader ...)`. Kết luận trong comment: JVM coi class là **(tên + defining loader)** — muốn hai bên "nói chuyện" được thì kiểu chung (interface `Widget`) phải do **loader cha chung** nạp (đúng parent delegation).
- **Bài 6:** Bản mặc định (escape analysis bật): gần như **0 Minor GC** phát sinh từ `hot()` (object bị scalar-replaced, không lên Eden), thời gian nhỏ. Bản `-XX:-DoEscapeAnalysis`: mỗi vòng cấp phát thật trên Eden → Eden đầy liên tục → **rất nhiều Minor GC**, thời gian lớn hơn rõ. Kết luận: "object nhỏ, cục bộ, không thoát" trong vòng lặp nóng **thường không tốn gì** nhờ JIT — nhưng đừng phụ thuộc mù quáng (escape analysis có giới hạn: object thoát qua field, qua lời gọi không inline được, mảng kích thước động lớn...).

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. **Dùng chung toàn JVM:** Heap (`OutOfMemoryError: Java heap space` / `GC overhead limit exceeded`), Metaspace (`OOM: Metaspace`), Code Cache (`CodeCache is full` → JIT ngừng, không phải OOM). **Riêng mỗi thread:** JVM Stack (`StackOverflowError`; hoặc `OOM: unable to create native thread` khi không cấp nổi stack cho thread mới), PC Register (không cạn), Native Method Stack (`StackOverflowError`/OOM native). Java 8: **PermGen bị xóa**, metadata class chuyển từ **một vùng cố định bên trong Heap** sang **native memory (Metaspace) tự mở rộng theo nhu cầu** (trần = `MaxMetaspaceSize`, mặc định gần như không giới hạn) → hết hẳn lớp lỗi `PermGen space` do đặt `-XX:MaxPermSize` sai. Java 7 chuyển **string pool** ra khỏi PermGen vào Heap vì PermGen nhỏ & cố định khiến `String.intern()` nhiều dễ làm tràn PermGen; ở Heap, các String interned được GC dọn bình thường và bảng hash có thể chỉnh (`-XX:StringTableSize`).
2. Mọi object căn địa chỉ theo bội số 8 → 3 bit thấp luôn `000` → JVM lưu `địa chỉ >> 3` trong 32 bit và khi dùng thì `<< 3` lại → không gian địa chỉ hoá được = `2^32 × 8 = 32 GB`. Vượt ngưỡng đó không "mã hoá" đủ → JVM **tắt** compressed oops, reference trở lại 8 byte. Heap 33–45 GB: phần thêm không bù nổi phần mất do **mọi** reference phình gấp đôi (footprint tăng, cache hit giảm) → bộ nhớ hiệu dụng có thể ít hơn 31 GB; phải lên ~48 GB+ mới thực sự lời. **Object header** = mark word (8B: identity hashcode *lazy*, age bits cho GC, biased/lightweight lock bits) + klass pointer (4B nén / 8B). "Lazy hashcode" = `identityHashCode` **chưa được tính** cho tới lần đầu ai đó gọi `hashCode()`/`System.identityHashCode()`; khi tính xong nó được **ghi vào mark word** (đó cũng là lý do object đã bị lấy identity hashcode thì không thể dùng biased locking nữa). Thêm field `boolean` không làm object to thêm nếu nó lấp vào **padding sẵn có** — JVM sắp field theo kích thước giảm dần để giảm padding, thường có vài byte trống cuối object.
3. **Copying** (Young): chi phí ~ *số object sống*; không phân mảnh; nhược là cần vùng dự trữ (Survivor) — chấp nhận được vì Young nhỏ & tỉ lệ sống thấp. **Mark-sweep-compact** (Old): phải duyệt toàn vùng để mark, rồi compact để chống phân mảnh (Old sống lâu, mảnh tích tụ) — đắt, nhưng tỉ lệ sống cao nên copying sẽ phí hơn. **Chu kỳ Minor GC**: Eden đầy → STW → chép object sống trong (Eden + Survivor-from) sang **Survivor-to** (vùng rỗng), tăng `age`; object nào `age ≥ MaxTenuringThreshold` (mặc định 15) hoặc **Survivor-to tràn** → **promote** lên Old; đảo vai from/to; Eden + Survivor-from giờ trống. "**Dynamic tenuring**": nếu tổng object theo từng độ tuổi vượt `TargetSurvivorRatio` của Survivor, JVM **hạ ngưỡng tuổi** để promote sớm hơn. Object đi **thẳng lên Old**: lớn hơn `PretenureSizeThreshold`, hoặc "humongous" với G1 (> ½ region). **TLAB**: bump-the-pointer trên *một* con trỏ `top` dùng chung mọi thread → phải CAS/lock mỗi lần `new` → nghẽn; TLAB cấp cho mỗi thread một khúc Eden riêng để bump *không đồng bộ*. TLAB đầy → thread xin TLAB mới (chậm, cần đồng bộ, hiếm) hoặc — nếu object quá lớn so với TLAB — cấp phát "slow path" thẳng ngoài TLAB.
4. **G1**: Heap = hàng nghìn **region** bằng nhau (1–32 MB); mỗi region *động* đóng vai Eden/Survivor/Old/Humongous. "Garbage first" = ưu tiên thu các region **nhiều rác nhất** để tối đa bộ nhớ lấy lại trên mỗi đơn vị công. Pha: **young-only** (STW ngắn, chỉ Eden/Survivor) → khi Old chiếm `IHOP` (~45%) bắt đầu **concurrent marking** (song song ứng dụng, đánh dấu liveness Old) → **mixed GC** (thu Young + một *tập* region Old nhiều rác nhất mỗi lần) → nếu không kịp: **Full GC** fallback (parallel từ Java 10). **Humongous** = object > ½ region, chiếm trọn ≥ 1 region, xử lý đặc biệt (dễ gây phí). `MaxGCPauseMillis` là **mục tiêu mềm**: G1 co/giãn Young Gen và số region thu mỗi lần để cố đạt — đặt quá thấp → Young Gen bị ép nhỏ → GC quá thường xuyên → **throughput sụt** và có khi vẫn không đạt pause. **ZGC/Shenandoah**: gần như mọi việc (kể cả **relocation/evacuation**) chạy *concurrent*; dùng **colored pointers + load barrier** (ZGC) / **Brooks forwarding pointer + read barrier** (Shenandoah cũ) để chương trình vẫn chạy trong lúc object đang bị di chuyển; STW chỉ còn vài thao tác gốc → **< 1 ms** bất kể Heap lớn cỡ nào. Đánh đổi: **~10–15% throughput** + **thêm RAM** (barrier, metadata) + (ZGC non-generational cũ) không tận dụng weak generational hypothesis. Chuyển sang ZGC khi: SLA p99/p999 tính bằng *mili giây* và pause G1 (chục–trăm ms) là không chấp nhận được, hoặc Heap quá lớn khiến ngay cả G1 mixed/Full GC pause cũng dài.
5. **Strong**: không bao giờ bị thu chừng nào còn strongly-reachable — dùng cho mọi reference thường; sai khi để một `static`/collection strong ref giữ mãi vật không cần (leak). **Soft**: bị thu **chỉ khi JVM sắp hết Heap** (được phép giữ lâu) — đúng cho *cache tái tạo được, memory-sensitive*; sai khi dùng như "map thường" (hành vi thu không dự đoán được, và trước Java 6 dễ gây OOM). **Weak**: bị thu **ngay chu kỳ GC kế tiếp** nếu không còn strong ref — đúng cho `WeakHashMap` key, canonicalizing map, metadata bên lề; sai khi bạn thật ra cần vật sống (nó biến mất bất ngờ). **Phantom**: `get()` **luôn `null`**; chỉ enqueue vào `ReferenceQueue` **sau khi** object đã finalize và không thể sống lại — đúng để chạy dọn tài nguyên native *sau* khi object chết một cách xác định; sai khi quên drain `ReferenceQueue` (referent không bao giờ được giải phóng). `finalize()` bị bỏ vì: (i) **không đảm bảo chạy** (JVM thoát trước đó) và **không đảm bảo thời điểm** → không dùng để giải phóng tài nguyên hữu hạn; (ii) **làm chậm GC** — object có finalizer cần ≥ 2 chu kỳ GC và một hàng đợi + thread riêng; (iii) **object resurrection** — `finalize()` có thể gán `this` vào một static → object "sống lại", phá vòng đời; thêm nữa exception trong `finalize()` bị nuốt. Khung `Cleaner`:
   ```java
   public class NativeThing implements AutoCloseable {
       private static final Cleaner CLEANER = Cleaner.create();
       private static final class State implements Runnable {   // KHÔNG tham chiếu NativeThing
           private final long handle;
           State(long h) { this.handle = h; }
           public void run() { release0(handle); }             // idempotent
       }
       private final State state;
       private final Cleaner.Cleanable cleanable;
       public NativeThing() { long h = alloc0(); this.state = new State(h); this.cleanable = CLEANER.register(this, state); }
       public void close() { cleanable.clean(); }              // gọi run() đúng một lần, ngay lập tức
   }
   ```
   *State không được giữ ref tới instance* vì `Cleaner` chỉ chạy `run()` khi instance trở nên **phantom-reachable**; nếu `state` (mà `Cleaner` giữ sống) lại trỏ ngược về instance → instance **mãi mãi strongly-reachable** → `Cleaner` **không bao giờ** kích hoạt → vừa leak vừa vô dụng.
6. **`loadClass(name)`**: (1) kiểm tra class đã nạp chưa (`findLoadedClass`); (2) nếu chưa, **uỷ quyền cho parent** `parent.loadClass(name)` (đệ quy tới Bootstrap); (3) chỉ khi mọi tổ tiên `throw ClassNotFoundException` thì mới gọi `findClass` của chính mình. Lợi ích: **bảo mật** (không thay được `java.*` — Bootstrap luôn thắng) và **nhất quán kiểu** (mọi loader chia sẻ một `java.lang.Object` từ Bootstrap). *Gây khó*: SPI như JDBC/JAXP — code **lõi** (do Bootstrap/Platform nạp) cần *khởi tạo cài đặt nằm ở classpath ứng dụng* (do App loader nạp); parent không "nhìn xuống" con được → giải pháp là **Thread Context ClassLoader** (`Thread.currentThread().getContextClassLoader()`) mà `ServiceLoader` dùng để cố ý *lách* delegation, nạp implementation từ loader con. **Classloader leak** → `OOM: Metaspace`: app server redeploy webapp → tạo `WebAppClassLoader` mới, đáng lẽ vứt cái cũ; nhưng một thư viện (ví dụ JDBC driver, logging, `ThreadLocal` trong thread của pool dùng chung ở tầng container) tạo một `ThreadLocal` mà **value của nó là instance của class do `WebAppClassLoader` cũ nạp** → value → class → `WebAppClassLoader` cũ → **toàn bộ class của bản deploy cũ** vẫn strongly-reachable qua thread pool sống lâu của container. Chúng chiếm **Metaspace** (metadata class), không phải Heap → sau vài lần redeploy: `OutOfMemoryError: Metaspace`. (Sửa: container gọi `ThreadLocal.remove`, driver deregister, hoặc dừng/thay pool khi undeploy.)
7. **C1** (client): dịch nhanh, tối ưu vừa phải, chèn counter để tiếp tục profiling → tốt cho *khởi động*. **C2** (server): dịch chậm hơn nhiều, tối ưu sâu (inlining tích cực, escape analysis, loop opts) → tốc độ *đỉnh*. 5 tier: 0 = interpreter; 1 = C1 không profiling (cho method tầm thường); 2/3 = C1 có profiling nhẹ/đầy; 4 = C2. **Invocation counter** đếm số lần *gọi* method; **backedge counter** đếm số lần một vòng lặp *quay lại đầu vòng* — cái thứ hai cần cho method chạy lâu mà ít được gọi lại. **OSR**: `void main(){ for (long i=0;i<5_000_000_000L;i++) heavy(i); }` — `main` chỉ được gọi **một lần**, invocation counter không bao giờ chạm ngưỡng; không có OSR thì vòng lặp chạy mãi trong interpreter. OSR để backedge counter kích hoạt biên dịch *thân vòng lặp* và **thay khung ngăn xếp đang thực thi** bằng bản mã máy — chuyển tiếp giữa chừng. **Deoptimization**: (i) *class hierarchy invalidation* — C2 đã inline dựa trên "method `pay()` chỉ có một cài đặt" (CHA); rồi một class mới có `@Override pay()` được load → giả định vỡ → mã máy bị đánh dấu *not entrant*; (ii) *uncommon trap* — nhánh mà profiling cho là "không bao giờ chạy" bị lược bỏ/hoãn; khi thực tế chạy vào đó → bẫy về interpreter. Khung đang chạy được **chuyển ngược** (mã máy → trạng thái interpreter tương đương) và tiếp tục ở tier 0, sau đó có thể được dịch lại. "Tính năng": nó là cái *cho phép* C2 tối ưu mạnh dựa trên giả định lạc quan mà vẫn đảm bảo đúng đắn. Deopt lặp vô hạn (mã thật sự đa hình cao, hoặc load class động liên tục) → không bao giờ đạt tốc độ đỉnh, "giật" — dấu hiệu trong `-XX:+PrintCompilation` là `made not entrant`/`made zombie` liên tục. **Escape analysis**: nếu object chứng minh được là *không thoát* method → JVM biết không ai khác quan sát nó → có thể (a) **scalar replacement**: không tạo object, đặt các field vào thanh ghi; (b) **lock elision**: bỏ hẳn `synchronized` trên nó. Ví dụ lock elision:
   ```java
   public String join(List<String> xs) {
       StringBuffer sb = new StringBuffer();     // sb KHÔNG thoát khỏi join()
       for (String x : xs) sb.append(x);         // append() là synchronized...
       return sb.toString();                     // ...nhưng JIT xoá bỏ vì sb chỉ thuộc về thread này
   }
   ```
8. Triệu chứng (RAM container leo, Heap phẳng, không `OutOfMemoryError` trong log, bị **OOM-kill exit 137**) ⇒ rò rỉ **bộ nhớ ngoài Heap (native)**. Nghi phạm & cách xác nhận: (1) bật `-XX:NativeMemoryTracking=summary`, khởi động lại, rồi so `jcmd <pid> VM.native_memory summary` theo thời gian — xem hạng mục nào tăng: **Thread** (số thread × stack), **Metaspace/Class**, **Code Cache**, **Internal** (direct buffer), **GC**. (2) `jstack <pid> | grep -c '"'` hoặc `jcmd Thread.print` đếm thread — nếu tăng dần ⇒ leak thread (pool tạo mà không shutdown; mỗi thread ~`-Xss`). (3) Direct buffer: kiểm tra `-XX:MaxDirectMemorySize`, đo qua `BufferPoolMXBean` (`java.nio:type=BufferPool,name=direct`) — Netty/HTTP client giữ buffer không trả → `OOM: Direct buffer memory` *hoặc* chỉ âm thầm ăn RAM. (4) Metaspace: `jcmd GC.heap_info` + theo dõi `Metaspace used` — tăng dần ⇒ classloader leak (câu C6). (5) Vì sao `-XX:+UseContainerSupport` (mặc định JDK 10+) + `-XX:MaxRAMPercentage` liên quan: nếu tắt container support hoặc đặt `-Xmx` xấp xỉ *toàn bộ* RAM container, thì Heap + Metaspace + stacks + Code Cache + direct buffer **cộng lại vượt limit cgroup** → kernel OOM-kill dù Heap chưa đầy. `MaxRAMPercentage=75` (thay `-Xmx` cứng) chừa ~25% cho vùng non-heap + OS, và `availableProcessors()` đọc đúng cgroup giúp JVM không tạo thừa thread GC/ForkJoinPool.

</details>

---

*File tiếp theo trong lộ trình: **Module 08 — Design Patterns** (Creational, Structural, Behavioral Patterns — nền tảng để đọc hiểu source code Spring Framework).*
