# Module 03.1 — Collections Framework

> **Mức độ ưu tiên: Cao** — Dùng sai cấu trúc dữ liệu (ví dụ dùng `ArrayList` cho thao tác chèn/xóa đầu danh sách liên tục) là nguyên nhân phổ biến nhất gây bug hiệu năng âm thầm trong backend thực tế. Câu hỏi "So sánh ArrayList và LinkedList", "HashMap hoạt động thế nào" gần như chắc chắn xuất hiện ở mọi vòng phỏng vấn kỹ thuật.

---

## Mục lục

1. [Tổng quan Collections Framework](#1-tổng-quan-collections-framework)
2. [List — ArrayList vs LinkedList](#2-list--arraylist-vs-linkedlist)
3. [Set — HashSet, LinkedHashSet, TreeSet](#3-set--hashset-linkedhashset-treeset)
4. [Map — HashMap, LinkedHashMap, TreeMap](#4-map--hashmap-linkedhashmap-treemap)
5. [Cơ chế bên trong HashMap (đào sâu)](#5-cơ-chế-bên-trong-hashmap-đào-sâu)
6. [Bảng độ phức tạp Big-O tổng hợp](#6-bảng-độ-phức-tạp-big-o-tổng-hợp)
7. [Queue & Deque — nhắc nhanh](#7-queue--deque--nhắc-nhanh)
8. [Cây quyết định: chọn cấu trúc dữ liệu nào?](#8-cây-quyết-định-chọn-cấu-trúc-dữ-liệu-nào)
9. [Tổng kết — Bảng ghi nhớ nhanh](#9-tổng-kết--bảng-ghi-nhớ-nhanh)
10. [Bài tập luyện tập](#10-bài-tập-luyện-tập)

---

## 1. Tổng quan Collections Framework

Collections Framework là tập hợp các interface và class chuẩn hóa trong `java.util`, giải quyết đúng hạn chế của mảng (Array) đã nhắc ở Module 01.1: **kích thước cố định**. Collections là các cấu trúc dữ liệu **động (dynamic)** — tự động mở rộng/thu hẹp khi cần.

```
                    Collection (interface)
                    /        |         \
                List        Set        Queue
                 │           │            │
        ┌────────┼────┐  ┌───┼────┐   ┌───┴────┐
    ArrayList LinkedList  HashSet TreeSet   Deque  PriorityQueue
              (cũng là Deque)  │  LinkedHashSet
                          (giữ thứ tự thêm vào)

                       Map (interface — KHÔNG kế thừa Collection, độc lập riêng)
                    /        |          \
               HashMap  LinkedHashMap  TreeMap
```

> **Lưu ý:** `Map` **không** nằm trong hệ thống `Collection` — vì `Map` lưu theo cặp key-value, khác bản chất với các cấu trúc chỉ lưu 1 chuỗi phần tử (`List`, `Set`, `Queue`). Đây là điểm hay bị hỏi nhầm trong phỏng vấn.

---

## 2. List — ArrayList vs LinkedList

`List` là cấu trúc **có thứ tự (ordered)**, **cho phép trùng lặp (duplicate)**, truy cập được qua **index**.

### `ArrayList` — dựa trên mảng động (dynamic array)

```java
List<String> names = new ArrayList<>();
names.add("Pho");
names.add("An");
names.get(0);        // truy cập theo index — RẤT NHANH
names.add(1, "Binh"); // chèn giữa danh sách — CHẬM (phải dịch chuyển phần tử phía sau)
```

**Cơ chế bên trong:** `ArrayList` lưu dữ liệu trong 1 mảng `Object[]` nội bộ. Khi mảng đầy, `ArrayList` tự tạo mảng mới **lớn hơn** (thường tăng ~50%), copy toàn bộ phần tử cũ sang — đây là lý do `add()` ở cuối danh sách **thường** là O(1) nhưng **đôi khi** là O(n) khi cần resize (gọi là "amortized O(1)" — trung bình vẫn coi là O(1)).

### `LinkedList` — dựa trên danh sách liên kết đôi (doubly linked list)

```java
List<String> names = new LinkedList<>();
names.add("Pho");
((LinkedList<String>) names).addFirst("An"); // chèn đầu — RẤT NHANH, không cần dịch chuyển
```

**Cơ chế bên trong:** mỗi phần tử là 1 **Node** chứa dữ liệu + con trỏ đến Node trước/sau. Chèn/xóa ở đầu hoặc giữa **không cần dịch chuyển** các phần tử khác — chỉ cần thay đổi vài con trỏ. Nhưng truy cập theo index (`get(i)`) phải **duyệt tuần tự từ đầu (hoặc cuối, tùy gần đâu hơn)** — chậm với danh sách lớn.

### Bảng so sánh chi tiết

| Thao tác | `ArrayList` | `LinkedList` |
|---|---|---|
| `get(index)` — truy cập ngẫu nhiên | **O(1)** — cực nhanh, tính toán địa chỉ trực tiếp | **O(n)** — phải duyệt từ đầu/cuối |
| `add(element)` — thêm cuối | O(1) amortized (đôi khi O(n) khi resize mảng) | O(1) |
| `add(index, element)` — chèn giữa | O(n) — phải dịch chuyển phần tử phía sau | O(n) để tìm vị trí + O(1) để chèn |
| `addFirst()` / `removeFirst()` | O(n) — phải dịch chuyển toàn bộ | **O(1)** |
| Bộ nhớ | Gọn hơn (chỉ lưu dữ liệu liên tiếp) | Tốn hơn (mỗi Node cần thêm 2 con trỏ prev/next) |
| Cache locality (hiệu năng CPU cache) | **Tốt hơn nhiều** — dữ liệu liền kề trong bộ nhớ | Kém hơn — Node nằm rải rác trên Heap |

> **Khuyến nghị thực tế:** **90%+ trường hợp trong backend nên dùng `ArrayList`** — vì đa số thao tác là đọc/duyệt tuần tự, và `ArrayList` có cache locality tốt hơn hẳn (dữ liệu nằm liền kề giúp CPU truy cập nhanh hơn nhờ cơ chế cache). Chỉ cân nhắc `LinkedList` khi có **rất nhiều thao tác chèn/xóa ở đầu hoặc giữa danh sách**, và gần như không bao giờ truy cập theo index — trường hợp này khá hiếm trong thực tế backend, thường gặp hơn ở cấu trúc Queue (xem mục 7).

---

## 3. Set — HashSet, LinkedHashSet, TreeSet

`Set` là cấu trúc **không cho phép trùng lặp** — dựa trực tiếp vào `equals()`/`hashCode()` (Module 02.4) để xác định 2 phần tử có "trùng nhau" hay không.

### `HashSet` — nhanh nhất, KHÔNG đảm bảo thứ tự

```java
Set<String> tags = new HashSet<>();
tags.add("java");
tags.add("spring");
tags.add("java"); // bị bỏ qua — đã tồn tại
System.out.println(tags.size()); // 2
```
Dựa trên `HashMap` nội bộ (mỗi phần tử là 1 "key", value là hằng số cố định) — thứ tự duyệt **không đảm bảo** và **có thể thay đổi** giữa các lần chạy chương trình khác nhau.

### `LinkedHashSet` — giữ đúng thứ tự thêm vào (insertion order)

```java
Set<String> tags = new LinkedHashSet<>();
tags.add("java");
tags.add("spring");
tags.add("hibernate");
// Duyệt LUÔN theo đúng thứ tự: java, spring, hibernate
```
Vẫn nhanh gần bằng `HashSet` (thêm 1 danh sách liên kết để lưu thứ tự), phù hợp khi cần **cả** tính duy nhất **lẫn** thứ tự ổn định, dễ đoán khi debug/log.

### `TreeSet` — tự động SẮP XẾP theo thứ tự tự nhiên (hoặc Comparator)

```java
Set<Integer> scores = new TreeSet<>();
scores.add(85);
scores.add(60);
scores.add(92);
// Duyệt LUÔN theo thứ tự tăng dần: 60, 85, 92
```
Dựa trên cấu trúc **Red-Black Tree** (cây tìm kiếm nhị phân tự cân bằng) — luôn giữ phần tử theo thứ tự sắp xếp, đổi lại chậm hơn `HashSet` một chút.

### Bảng so sánh 3 loại Set

| Tiêu chí | `HashSet` | `LinkedHashSet` | `TreeSet` |
|---|---|---|---|
| Thứ tự duyệt | Không đảm bảo | Theo thứ tự thêm vào | Theo thứ tự sắp xếp (tự nhiên hoặc Comparator) |
| `add`/`contains`/`remove` | **O(1)** trung bình | O(1) trung bình | O(log n) |
| Cho phép `null` | 1 phần tử `null` | 1 phần tử `null` | **Không** (ném `NullPointerException` khi so sánh) |
| Khi nào dùng | Chỉ cần tính duy nhất, không quan tâm thứ tự — nhanh nhất | Cần duy nhất + thứ tự ổn định dễ đoán | Cần duy nhất + luôn sắp xếp sẵn |

---

## 4. Map — HashMap, LinkedHashMap, TreeMap

`Map` lưu dữ liệu theo cặp **key-value**, key **không trùng lặp** (nếu `put()` key đã tồn tại → ghi đè value cũ).

```java
Map<String, Integer> studentScores = new HashMap<>();
studentScores.put("Pho", 90);
studentScores.put("An", 85);
studentScores.put("Pho", 95); // GHI ĐÈ giá trị cũ (90 → 95), không tạo entry mới

studentScores.get("Pho");              // 95
studentScores.getOrDefault("Binh", 0); // 0 — tránh NullPointerException khi key không tồn tại
studentScores.containsKey("An");       // true

for (Map.Entry<String, Integer> entry : studentScores.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}

// Cách hiện đại, gọn hơn (Java 8+)
studentScores.forEach((name, score) -> System.out.println(name + ": " + score));
```

### Method tiện ích hiện đại đáng chú ý (Java 8+)

```java
// merge() — cực kỳ hữu ích để đếm số lần xuất hiện (word count) mà không cần if/else kiểm tra tồn tại
Map<String, Integer> wordCount = new HashMap<>();
for (String word : List.of("java", "spring", "java", "boot", "java")) {
    wordCount.merge(word, 1, Integer::sum); // nếu key chưa có → gán 1; nếu đã có → cộng dồn
}
// Kết quả: {java=3, spring=1, boot=1}

// computeIfAbsent() — khởi tạo giá trị mặc định nếu key chưa tồn tại (hay dùng khi value là List/Set)
Map<String, List<String>> studentsByClass = new HashMap<>();
studentsByClass.computeIfAbsent("SE101", k -> new ArrayList<>()).add("Pho");
studentsByClass.computeIfAbsent("SE101", k -> new ArrayList<>()).add("An");
// Kết quả: {SE101=[Pho, An]}
```

`HashMap` / `LinkedHashMap` / `TreeMap` có đặc điểm về **thứ tự** và **hiệu năng** hoàn toàn tương tự `HashSet` / `LinkedHashSet` / `TreeSet` đã trình bày ở mục 3 (thực chất `HashSet` được cài đặt **dựa trên** `HashMap` bên trong JDK).

---

## 5. Cơ chế bên trong HashMap (đào sâu)

Đây là phần **hay được hỏi sâu nhất** ở phỏng vấn Middle/Senior — hiểu cơ chế này giúp giải thích được vì sao `HashMap` đạt O(1) trung bình.

### Cấu trúc: mảng các "bucket", mỗi bucket là 1 Linked List (hoặc Red-Black Tree nếu quá dài)

```
HashMap nội bộ = Node<K,V>[] table (mảng các bucket)

table[0] → null
table[1] → Node("An", 85) → Node("Cuong", 70)   ← 2 key khác nhau nhưng CÙNG hashCode % capacity (hash collision)
table[2] → Node("Pho", 95)
table[3] → null
...
```

### Quy trình `put(key, value)`

1. Tính `key.hashCode()`.
2. JVM áp dụng thêm 1 bước "hash spreading" (dịch bit XOR) để phân tán đều hơn, tránh trường hợp nhiều hashCode dồn vào cùng vùng bit thấp.
3. Xác định bucket: `index = hash & (capacity - 1)` (về ý tưởng tương đương `hash % capacity`, nhưng dùng phép AND bitwise nhanh hơn nhiều vì capacity luôn là lũy thừa của 2).
4. Nếu bucket rỗng → tạo `Node` mới.
5. Nếu bucket đã có phần tử (**hash collision**) → duyệt qua từng Node trong bucket, dùng `equals()` so sánh key — nếu trùng thì **ghi đè value**, nếu không trùng thì **thêm Node mới vào cuối danh sách liên kết của bucket đó**.

### Hash Collision — vì sao vẫn đạt hiệu năng O(1) trung bình?

Nếu thiết kế `hashCode()` tốt (phân tán đều), số lượng phần tử rơi vào cùng 1 bucket là **rất ít** (gần như hằng số), nên duyệt trong 1 bucket gần như O(1). Đây chính là lý do **contract equals/hashCode** ở Module 02.4 quan trọng đến vậy — `hashCode()` viết tệ (ví dụ luôn trả về hằng số cố định) sẽ khiến **mọi phần tử** rơi vào **1 bucket duy nhất**, biến `HashMap` thành 1 danh sách liên kết O(n) — mất hoàn toàn lợi thế về hiệu năng.

> **Cải tiến từ Java 8:** nếu 1 bucket có **quá nhiều phần tử** (mặc định ngưỡng 8), JDK tự động chuyển bucket đó từ Linked List sang **Red-Black Tree** — giảm worst-case từ O(n) xuống O(log n) trong trường hợp hash collision nghiêm trọng (dù trường hợp này rất hiếm khi xảy ra với `hashCode()` được viết đúng chuẩn).

### Load Factor & Resizing

`HashMap` có `capacity` (số lượng bucket, mặc định 16) và `loadFactor` (mặc định 0.75). Khi `số phần tử > capacity × loadFactor`, `HashMap` tự động **resize** (thường nhân đôi capacity) và **tính lại hash, phân bổ lại toàn bộ phần tử vào bucket mới** — thao tác này có chi phí O(n), nhưng diễn ra không thường xuyên nên không ảnh hưởng độ phức tạp trung bình (amortized O(1)).

> **Mẹo tối ưu thực tế:** nếu biết trước số lượng phần tử sẽ lưu (ví dụ ~1000), nên khởi tạo `new HashMap<>(1024)` (hoặc số phù hợp) ngay từ đầu để **tránh resize nhiều lần** — tối ưu hiệu năng khi xử lý dữ liệu lớn trong code backend.

---

## 6. Bảng độ phức tạp Big-O tổng hợp

| Cấu trúc | `get`/`contains` | `add`/`put` | `remove` | Ghi chú |
|---|---|---|---|---|
| `ArrayList` | O(1) theo index, O(n) theo giá trị (`contains`) | O(1) amortized (cuối), O(n) (giữa/đầu) | O(n) | Cache locality tốt |
| `LinkedList` | O(n) | O(1) (đầu/cuối), O(n) (giữa, do phải tìm vị trí) | O(1) sau khi có vị trí | Tốn bộ nhớ hơn ArrayList |
| `HashSet` / `HashMap` | **O(1)** trung bình, O(n) worst-case (hash collision cực đoan) | O(1) trung bình | O(1) trung bình | Không đảm bảo thứ tự |
| `LinkedHashSet` / `LinkedHashMap` | O(1) trung bình | O(1) trung bình | O(1) trung bình | Giữ thứ tự thêm vào |
| `TreeSet` / `TreeMap` | O(log n) | O(log n) | O(log n) | Luôn sắp xếp, dựa trên Red-Black Tree |

---

## 7. Queue & Deque — nhắc nhanh

| Interface | Đặc điểm | Implementation phổ biến |
|---|---|---|
| `Queue` | FIFO (First In First Out) — vào trước ra trước | `LinkedList`, `ArrayDeque` |
| `Deque` (Double-Ended Queue) | Thêm/xóa được ở **cả 2 đầu** | `ArrayDeque` (nhanh hơn `LinkedList` trong đa số trường hợp thực tế), `LinkedList` |
| `PriorityQueue` | Phần tử luôn được lấy ra theo thứ tự ưu tiên (dựa trên `Comparable`/`Comparator`), KHÔNG phải FIFO thuần | Dựa trên cấu trúc Heap (Binary Heap) |

```java
Queue<String> queue = new LinkedList<>();
queue.offer("A"); queue.offer("B");
queue.poll(); // lấy ra "A" — FIFO

Deque<Integer> stack = new ArrayDeque<>(); // dùng Deque để mô phỏng Stack (LIFO) — cách hiện đại, khuyến nghị hơn class Stack cũ
stack.push(1); stack.push(2);
stack.pop(); // lấy ra 2 — LIFO (Last In First Out)

PriorityQueue<Integer> minHeap = new PriorityQueue<>(); // mặc định lấy ra phần tử NHỎ NHẤT trước
minHeap.offer(5); minHeap.offer(1); minHeap.offer(3);
minHeap.poll(); // 1
```

> `ArrayDeque` được khuyến nghị thay cho class `Stack` cũ (từ Java 1.0, đã lỗi thời vì kế thừa `Vector` — có overhead đồng bộ hóa không cần thiết trong đa số trường hợp đơn luồng) khi cần mô phỏng ngăn xếp (LIFO).

---

## 8. Cây quyết định: chọn cấu trúc dữ liệu nào?

```
Cần lưu cặp key-value?
├── Có → dùng Map
│         ├── Cần thứ tự sắp xếp theo key? → TreeMap
│         ├── Cần giữ thứ tự thêm vào? → LinkedHashMap
│         └── Không quan tâm thứ tự, cần nhanh nhất? → HashMap (mặc định nên chọn)
│
└── Không → Cần duy nhất (không trùng lặp)?
            ├── Có → dùng Set
            │         ├── Cần sắp xếp? → TreeSet
            │         ├── Cần giữ thứ tự thêm vào? → LinkedHashSet
            │         └── Không quan tâm thứ tự? → HashSet (mặc định nên chọn)
            │
            └── Không (cho phép trùng lặp, có thứ tự) → dùng List
                      ├── Chủ yếu đọc/duyệt, truy cập theo index? → ArrayList (mặc định nên chọn, 90%+ trường hợp)
                      └── Chủ yếu chèn/xóa ở đầu/giữa, hiếm truy cập index? → LinkedList (hoặc cân nhắc ArrayDeque nếu dùng như Queue)
```

---

## 9. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt cần nhớ |
|---|---|
| `Map` vs `Collection` | `Map` KHÔNG kế thừa `Collection` — lưu key-value, khác bản chất |
| `ArrayList` | Mảng động, `get(index)` O(1), chèn/xóa giữa O(n) — mặc định nên dùng |
| `LinkedList` | Danh sách liên kết đôi, `get(index)` O(n), chèn/xóa đầu O(1) |
| `HashSet`/`HashMap` | O(1) trung bình nhờ cơ chế bucket + hashCode, không đảm bảo thứ tự |
| `LinkedHashSet`/`LinkedHashMap` | Như Hash-, cộng thêm giữ thứ tự thêm vào |
| `TreeSet`/`TreeMap` | O(log n), luôn sắp xếp, dựa trên Red-Black Tree |
| Hash Collision | Nhiều key khác nhau cùng rơi vào 1 bucket — Java 8+ dùng Red-Black Tree nếu bucket quá dài (>8 phần tử) |
| Load Factor & Resize | HashMap tự resize khi đầy 75% — nên khởi tạo capacity phù hợp nếu biết trước quy mô dữ liệu |
| `merge()` / `computeIfAbsent()` | Cách viết hiện đại, tránh if/else kiểm tra tồn tại key thủ công |
| `ArrayDeque` | Khuyến nghị thay cho `Stack` cũ khi cần cấu trúc LIFO |

---

## 10. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn code sau chạy chậm bất thường với danh sách 100,000 phần tử — lý do là gì? Cách sửa?
```java
List<Integer> numbers = new ArrayList<>();
for (int i = 0; i < 100000; i++) {
    numbers.add(0, i); // luôn chèn vào ĐẦU danh sách
}
```

**Câu 2.** `HashSet<Student> students` sẽ hoạt động sai nếu class `Student` thiếu điều gì? (Liên hệ lại Module 02.4)

**Câu 3.** Đoạn code sau in ra thứ tự nào? Giải thích vì sao KHÔNG nên giả định `HashMap` giữ nguyên thứ tự `put()`.
```java
Map<String, Integer> map = new HashMap<>();
map.put("banana", 1);
map.put("apple", 2);
map.put("cherry", 3);
for (String key : map.keySet()) {
    System.out.println(key);
}
```

**Câu 4.** So sánh độ phức tạp của `contains()` giữa `ArrayList<String>` và `HashSet<String>` với 1 triệu phần tử — giải thích sự khác biệt bằng cơ chế bên trong.

**Câu 5.** Nếu khởi tạo `new HashMap<>()` không truyền capacity, rồi thêm liên tục 10,000 phần tử, điều gì xảy ra về mặt hiệu năng? Đề xuất cách tối ưu.

---

### Phần B — Bài tập viết code

**Bài 1 — So sánh hiệu năng ArrayList vs LinkedList thực nghiệm.**
Viết chương trình đo thời gian (`System.nanoTime()`) cho 2 thao tác trên cả `ArrayList` và `LinkedList` với 50,000 phần tử: (a) chèn liên tục vào **đầu** danh sách, (b) truy cập ngẫu nhiên bằng `get(index)` 10,000 lần. In bảng kết quả so sánh, giải thích tại sao mỗi cấu trúc thắng/thua ở từng thao tác.

**Bài 2 — Loại bỏ trùng lặp & đếm tần suất từ (Word Frequency Counter).**
Cho một đoạn văn bản (`String` nhiều câu), viết chương trình:
- Tách thành danh sách từ (dùng `split`), chuẩn hóa về chữ thường.
- Dùng `HashSet<String>` để lấy ra tập từ **duy nhất** (không trùng lặp).
- Dùng `HashMap<String, Integer>` + `merge()` để đếm **số lần xuất hiện** của mỗi từ.
- In ra 5 từ xuất hiện nhiều nhất (gợi ý: đưa `entrySet()` vào `List`, dùng `Comparator` sắp theo value giảm dần).

**Bài 3 — Nhóm dữ liệu theo tiêu chí (Group By) dùng computeIfAbsent.**
Cho `List<Student>` với field `name`, `major` (chuyên ngành). Viết method `Map<String, List<Student>> groupByMajor(List<Student> students)` dùng `computeIfAbsent()` để nhóm sinh viên theo từng chuyên ngành, không dùng `if/else` kiểm tra tồn tại key thủ công.

**Bài 4 — Xây dựng LRU Cache đơn giản dùng LinkedHashMap.**
`LinkedHashMap` có 1 constructor đặc biệt hỗ trợ chế độ **access-order** thay vì insertion-order, và có thể override `removeEldestEntry()` để tự động xóa phần tử cũ nhất khi vượt quá kích thước — đây chính là cách cài đặt **LRU Cache (Least Recently Used)** đơn giản nhất trong Java. Viết class `SimpleLRUCache<K, V> extends LinkedHashMap<K, V>` với sức chứa tối đa cố định (ví dụ 3), chứng minh khi thêm phần tử thứ 4, phần tử **ít được truy cập gần đây nhất** sẽ tự động bị loại bỏ.
```java
// Gợi ý cấu trúc khởi đầu:
public class SimpleLRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    public SimpleLRUCache(int capacity) {
        super(16, 0.75f, true); // tham số thứ 3 = true → bật access-order
        this.capacity = capacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity; // trả về true → tự động xóa phần tử cũ nhất
    }
}
```

**Bài 5 — So sánh 3 loại Set bằng ví dụ trực quan.**
Viết chương trình thêm cùng 1 tập dữ liệu (ví dụ tên 5 thành phố Việt Nam) vào cả `HashSet`, `LinkedHashSet`, `TreeSet`, in ra kết quả duyệt của cả 3 để **quan sát trực tiếp** sự khác biệt về thứ tự. Giải thích bằng lời tại sao mỗi loại lại cho thứ tự như vậy.

**Bài 6 (nâng cao — mô phỏng bug thực tế) — Hash Collision cố ý.**
Viết 1 class `BadKey` với `hashCode()` **cố tình luôn trả về hằng số 1** (vi phạm nguyên tắc thiết kế hashCode tốt), nhưng `equals()` vẫn so sánh đúng theo field nội dung. Thêm 10,000 object `BadKey` khác nhau vào `HashMap`, đo thời gian thực hiện so với `HashMap` dùng key là `String` bình thường có cùng số lượng phần tử. Giải thích kết quả đo được bằng cơ chế bucket đã học ở mục 5.

---

### Phần C — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. Vì `add(0, i)` chèn vào **đầu** `ArrayList`, buộc phải dịch chuyển **toàn bộ** phần tử hiện có sang phải mỗi lần gọi — độ phức tạp O(n) mỗi lần chèn, tổng cộng O(n²) cho 100,000 lần. Sửa: dùng `LinkedList` (hoặc `ArrayDeque`) và gọi `addFirst()` — O(1) mỗi lần.
2. Thiếu override `equals()`/`hashCode()` đúng chuẩn (Module 02.4) — nếu dùng bản mặc định của `Object` (dựa vào địa chỉ bộ nhớ), `HashSet` sẽ coi 2 `Student` có nội dung giống hệt nhau là **2 phần tử khác nhau**, dẫn đến trùng lặp dữ liệu mà đáng lẽ phải được loại bỏ.
3. Thứ tự **không đảm bảo** giống thứ tự `put()` — có thể là `apple, banana, cherry` hoặc bất kỳ thứ tự nào khác tùy vào giá trị hash và cách JVM sắp xếp bucket nội bộ. `HashMap` **không** cam kết giữ thứ tự — nếu cần giữ thứ tự thêm vào, phải dùng `LinkedHashMap`.
4. `ArrayList.contains()` là O(n) — phải duyệt tuần tự, so sánh `equals()` với từng phần tử cho đến khi tìm thấy hoặc hết danh sách. `HashSet.contains()` là O(1) trung bình — chỉ cần tính `hashCode()` để nhảy thẳng đến đúng bucket, không cần duyệt toàn bộ tập dữ liệu. Với 1 triệu phần tử, chênh lệch hiệu năng có thể lên tới hàng trăm nghìn lần.
5. `HashMap` sẽ phải **resize nhiều lần** (mỗi lần capacity đầy 75%, nhân đôi kích thước, tính lại hash và phân bổ lại toàn bộ phần tử) — gây lãng phí thời gian và bộ nhớ tạm không cần thiết. Tối ưu: khởi tạo `new HashMap<>(16384)` (hoặc con số phù hợp, thường tính bằng `(int)(số phần tử dự kiến / 0.75) + 1`) ngay từ đầu để tránh resize.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Kết quả dự kiến: `LinkedList.addFirst()` nhanh hơn `ArrayList.add(0, ...)` rất nhiều (có thể hàng trăm/nghìn lần với 50,000 phần tử do độ phức tạp O(n) mỗi lần chèn của ArrayList); ngược lại `ArrayList.get(index)` nhanh hơn đáng kể so với `LinkedList.get(index)` (O(1) so với O(n)).
- **Bài 2:** Cách lấy top 5 gợi ý:
```java
wordCount.entrySet().stream()
    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
    .limit(5)
    .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
```
(Sử dụng Stream API — nếu chưa quen, có thể làm bằng cách đưa `entrySet()` vào `List` rồi `Collections.sort()` với `Comparator` tùy chỉnh, đạt hiệu quả tương tự.)
- **Bài 4:** Đây là ví dụ thực tế rất hay dùng trong hệ thống cache đơn giản (trước khi học đến Redis ở Module 18) — `LinkedHashMap` với `accessOrder=true` sẽ tự động **di chuyển entry vừa được `get()` xuống cuối** danh sách liên kết nội bộ, khiến entry **lâu không được truy cập nhất** luôn nằm ở đầu — `removeEldestEntry()` chỉ cần kiểm tra kích thước để quyết định có xóa phần tử đầu tiên đó hay không.
- **Bài 6:** Kết quả dự kiến: `HashMap<BadKey, ...>` sẽ chậm hơn rất nhiều (có thể hàng nghìn lần) so với `HashMap<String, ...>` bình thường, vì mọi `BadKey` đều rơi vào **cùng 1 bucket duy nhất** (do `hashCode()` luôn là 1) — biến toàn bộ HashMap thành 1 danh sách liên kết (hoặc cây đỏ-đen nếu bucket đủ dài, từ Java 8+) phải duyệt tuần tự O(n) cho mỗi thao tác `put`/`get`, thay vì O(1) như thiết kế `hashCode()` phân tán tốt. Bài tập này minh họa trực quan **tại sao thiết kế `hashCode()` tốt quan trọng đến vậy** đối với hiệu năng thực tế.

</details>

---

*File tiếp theo trong lộ trình: **Module 03.2 — Generics** (generic class/method, bounded types, wildcard `? extends`/`? super`).*
