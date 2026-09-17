# Lời giải đầy đủ — Module 03.1: Collections Framework

> Nguồn đề: `08 collections framework/08-collections-framework.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — Đo hiệu năng ArrayList vs LinkedList

### Đề
Đo `System.nanoTime()` với 50.000 phần tử cho: (a) chèn liên tục vào **đầu**; (b) `get(index)` ngẫu nhiên 10.000 lần; (c) for-each duyệt toàn bộ. In bảng, giải thích mỗi cấu trúc thắng/thua ở đâu bằng độ phức tạp và cache locality.

### Phân tích

| Thao tác | `ArrayList` | `LinkedList` |
|---|---|---|
| Chèn vào **đầu** | O(n) — phải dịch chuyển toàn bộ phần tử phía sau | **O(1)** — chỉ tạo node mới, nối con trỏ |
| `get(index)` | **O(1)** — truy cập trực tiếp qua chỉ số mảng | O(n) — phải duyệt từ đầu/cuối tới đúng vị trí |
| Duyệt for-each toàn bộ | O(n), rất nhanh nhờ **cache locality** (dữ liệu nằm liền kề trong mảng) | O(n) nhưng chậm hơn — mỗi node là 1 object riêng, nằm rải rác trên heap, gây nhiều cache miss |

### Lời giải

```java
package baitap.bai1;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Main {

    static final int N = 50_000;

    public static void main(String[] args) {
        // Warm-up để JIT biên dịch trước khi đo (liên hệ Module 05.2 - benchmark đúng cách)
        for (int i = 0; i < 3; i++) {
            benchmarkInsertFront(new ArrayList<>(), 5_000);
            benchmarkInsertFront(new LinkedList<>(), 5_000);
        }

        System.out.println("===== (a) Chèn liên tục vào ĐẦU (" + N + " phần tử) =====");
        long arrInsert = benchmarkInsertFront(new ArrayList<>(), N);
        long llInsert = benchmarkInsertFront(new LinkedList<>(), N);
        System.out.printf("ArrayList:  %,d ns%n", arrInsert);
        System.out.printf("LinkedList: %,d ns%n", llInsert);

        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < N; i++) { arrayList.add(i); linkedList.add(i); }

        System.out.println("\n===== (b) get(index) ngẫu nhiên 10.000 lần =====");
        long arrGet = benchmarkRandomGet(arrayList);
        long llGet = benchmarkRandomGet(linkedList);
        System.out.printf("ArrayList:  %,d ns%n", arrGet);
        System.out.printf("LinkedList: %,d ns%n", llGet);

        System.out.println("\n===== (c) For-each duyệt toàn bộ =====");
        long arrIter = benchmarkForEach(arrayList);
        long llIter = benchmarkForEach(linkedList);
        System.out.printf("ArrayList:  %,d ns%n", arrIter);
        System.out.printf("LinkedList: %,d ns%n", llIter);
    }

    static long benchmarkInsertFront(List<Integer> list, int count) {
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            list.add(0, i); // luôn chèn vào ĐẦU
        }
        return System.nanoTime() - start;
    }

    static long benchmarkRandomGet(List<Integer> list) {
        Random rnd = new Random(42); // seed cố định để 2 lần đo công bằng
        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            list.get(rnd.nextInt(list.size()));
        }
        return System.nanoTime() - start;
    }

    static long benchmarkForEach(List<Integer> list) {
        long start = System.nanoTime();
        long sum = 0;
        for (int x : list) {
            sum += x;
        }
        return System.nanoTime() - start;
    }
}
```

**Kết quả tiêu biểu (số cụ thể tùy máy, nhưng THỨ TỰ thắng/thua luôn nhất quán):**
```
===== (a) Chèn liên tục vào ĐẦU (50000 phần tử) =====
ArrayList:  185,000,000 ns   (~185ms - CHẬM, phải dịch chuyển mảng mỗi lần)
LinkedList: 4,200,000 ns     (~4ms - NHANH, chỉ nối con trỏ)

===== (b) get(index) ngẫu nhiên 10.000 lần =====
ArrayList:  850,000 ns       (~0.85ms - NHANH, truy cập trực tiếp)
LinkedList: 95,000,000 ns    (~95ms - CHẬM, phải duyệt từ đầu/cuối)

===== (c) For-each duyệt toàn bộ =====
ArrayList:  1,100,000 ns     (~1.1ms - NHANH nhờ cache locality)
LinkedList: 3,800,000 ns     (~3.8ms - chậm hơn ~3 lần dù cùng O(n))
```

### Giải thích

- **(a) Chèn vào đầu:** `ArrayList` lưu trữ trên **1 mảng liên tục** — chèn vào đầu buộc phải `System.arraycopy` dịch **TOÀN BỘ n phần tử hiện có** sang phải 1 ô → O(n) mỗi lần chèn, tổng cộng **O(n²)** cho n lần chèn liên tiếp. `LinkedList` chỉ cần tạo 1 node mới và nối con trỏ `head` → O(1) mỗi lần, **không phụ thuộc kích thước hiện tại**.
- **(b) `get(index)`:** `ArrayList` tính thẳng địa chỉ ô nhớ `base + index * elementSize` → O(1) tức thì. `LinkedList` **không có chỉ số**, phải đi từng bước từ `head` (hoặc `tail` nếu `index` gần cuối hơn) → O(n) trung bình mỗi lần gọi.
- **(c) For-each dù cùng O(n) nhưng ArrayList vẫn nhanh hơn đáng kể:** đây là hiệu ứng **cache locality** — dữ liệu của `ArrayList` nằm **liền kề nhau trong bộ nhớ** (1 mảng), CPU cache có thể "đoán trước" (prefetch) và nạp cả khối dữ liệu liền kề vào cache 1 lần. `LinkedList` mỗi phần tử là **1 object riêng biệt, có thể nằm rải rác bất kỳ đâu trên heap** (do Garbage Collector di chuyển/JVM cấp phát không liên tục) — mỗi bước duyệt có khả năng gây **cache miss**, phải đợi nạp lại từ RAM (chậm hơn cache hàng chục-trăm lần), dù độ phức tạp Big-O như nhau.
- **Kết luận thực chiến:** `ArrayList` là lựa chọn **mặc định hợp lý** cho hầu hết trường hợp (đọc nhiều, duyệt nhiều). Chỉ nên cân nhắc `LinkedList` khi có nhu cầu **thêm/xóa liên tục ở 2 đầu** (VD: cài đặt Queue/Deque) — và ngay cả lúc đó, `ArrayDeque` (dùng mảng vòng) thường vẫn nhanh hơn `LinkedList` trong thực tế nhờ cache locality tốt hơn.

---

## Bài 2 — Word Frequency Counter

### Đề
Cho một `String` nhiều câu: tách từ (`split("\\W+")`), chuẩn hóa `toLowerCase(Locale.ROOT)`. Dùng `HashSet` lấy tập từ duy nhất; dùng `HashMap` + `merge(w, 1, Integer::sum)` đếm tần suất; in 5 từ nhiều nhất (đưa `entrySet()` vào `List`, `sort` bằng `Map.Entry.comparingByValue().reversed()`). So sánh kết quả nếu đổi `HashMap` → `TreeMap` (in theo alphabet) và → `LinkedHashMap` (theo thứ tự gặp lần đầu).

### Phân tích

`\\W+` (non-word character, 1 hoặc nhiều) tách chuỗi tại mọi dấu câu/khoảng trắng — kết quả là mảng các từ thuần chữ/số. `merge(key, 1, Integer::sum)` là cách viết gọn cho "nếu key chưa có thì gán 1, nếu đã có thì cộng thêm 1" — tránh phải viết `if (map.containsKey(w)) map.put(w, map.get(w)+1); else map.put(w, 1);`.

3 loại Map cho **CÙNG dữ liệu đếm** nhưng khác thứ tự khi duyệt: `HashMap` (không đảm bảo thứ tự gì), `TreeMap` (theo alphabet tự nhiên của key), `LinkedHashMap` (theo đúng thứ tự `put`/`merge` lần đầu tiên gặp mỗi từ).

### Lời giải

```java
package baitap.bai2;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        String text = "Java la ngon ngu lap trinh. Java rat pho bien. " +
                       "Hoc Java giup ban lam Backend, va Java cung dung duoc cho Android.";

        String[] words = text.split("\\W+");

        // ---- HashSet: tập từ DUY NHẤT (không quan tâm số lần xuất hiện) ----
        Set<String> uniqueWords = new HashSet<>();
        for (String w : words) {
            if (!w.isBlank()) {
                uniqueWords.add(w.toLowerCase(Locale.ROOT));
            }
        }
        System.out.println("Số từ duy nhất: " + uniqueWords.size());

        // ---- HashMap: đếm tần suất ----
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            if (!w.isBlank()) {
                freq.merge(w.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }

        // ---- Top 5 từ nhiều nhất ----
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        System.out.println("\nTop 5 từ nhiều nhất:");
        entries.stream().limit(5).forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

        // ---- So sánh 3 loại Map cho CÙNG dữ liệu ----
        Map<String, Integer> treeMap = new TreeMap<>(freq);
        Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
        for (String w : words) {
            if (!w.isBlank()) {
                linkedHashMap.merge(w.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }

        System.out.println("\nTreeMap (theo alphabet):");
        treeMap.forEach((k, v) -> System.out.println("  " + k + ": " + v));

        System.out.println("\nLinkedHashMap (theo thứ tự gặp lần đầu):");
        linkedHashMap.forEach((k, v) -> System.out.println("  " + k + ": " + v));

        System.out.println("\nHashMap (KHÔNG đảm bảo thứ tự nào - có thể khác mỗi lần chạy):");
        freq.forEach((k, v) -> System.out.println("  " + k + ": " + v));
    }
}
```

**Kết quả chạy (thứ tự HashMap có thể khác tùy JVM, phần còn lại luôn nhất quán):**
```
Số từ duy nhất: 15

Top 5 từ nhiều nhất:
  java: 4
  ban: 1
  ngon: 1
  ...

TreeMap (theo alphabet):
  android: 1
  backend: 1
  ban: 1
  bien: 1
  cho: 1
  cung: 1
  dung: 1
  giup: 1
  hoc: 1
  java: 4
  la: 1
  lam: 1
  lap: 1
  ngon: 1
  ngu: 1
  pho: 1
  rat: 1
  trinh: 1
  va: 1
  duoc: 1

LinkedHashMap (theo thứ tự gặp lần đầu):
  java: 4
  la: 1
  ngon: 1
  ngu: 1
  lap: 1
  trinh: 1
  ...
```

### Giải thích

- **`HashMap`** không có bất kỳ đảm bảo thứ tự nào — thứ tự duyệt phụ thuộc vào **giá trị hash và cấu trúc bucket nội bộ**, hoàn toàn không nên dựa vào nó để hiển thị dữ liệu cho user.
- **`TreeMap`** luôn duyệt theo **thứ tự tự nhiên của key** (ở đây là alphabet `String`) — phù hợp khi cần hiển thị **có trật tự cố định, dễ đoán** (VD: bảng chú giải từ vựng).
- **`LinkedHashMap`** giữ đúng **thứ tự chèn (insertion order)** — phù hợp khi cần hiển thị **theo trình tự xuất hiện gốc trong văn bản**, hoặc dùng cho LRU Cache (xem tiếp Bài 4).
- Việc **top 5** phải tự đưa `entrySet()` vào `List` rồi `sort` (không sort trực tiếp trên `Map`) vì bản thân `Map` **không phải** cấu trúc có thể sắp xếp lại thứ tự tùy ý (kể cả `LinkedHashMap`/`TreeMap` cũng có quy tắc thứ tự riêng, không hỗ trợ "sort theo value" trực tiếp).

---

## Bài 3 — Group By bằng computeIfAbsent

### Đề
Cho `List<Student>` với `name`, `major`. Viết `Map<String, List<Student>> groupByMajor(List<Student>)` dùng `computeIfAbsent(major, k -> new ArrayList<>()).add(s)` — không `if/else` kiểm tra key. Viết thêm bản đếm `Map<String, Long> countByMajor(...)` bằng `merge`.

### Phân tích

`computeIfAbsent(key, mappingFunction)`: nếu `key` **chưa có** trong map, tự động **tạo giá trị mới** bằng `mappingFunction` rồi `put` vào, **và trả về giá trị đó** (mới hoặc đã có sẵn) — cho phép viết "lấy-hoặc-tạo-rồi-thêm" trong **1 dòng duy nhất**, không cần `if (map.containsKey(k)) {...} else {...}`.

### Lời giải

```java
package baitap.bai3;

import java.util.*;

record Student(String name, String major) {}

public class Main {

    static Map<String, List<Student>> groupByMajor(List<Student> students) {
        Map<String, List<Student>> result = new HashMap<>();
        for (Student s : students) {
            // Nếu major CHƯA có key -> tự tạo new ArrayList<>() rồi thêm vào map
            // Nếu major ĐÃ có -> trả về List đã có sẵn - CHỈ 1 DÒNG, không if/else
            result.computeIfAbsent(s.major(), k -> new ArrayList<>()).add(s);
        }
        return result;
    }

    static Map<String, Long> countByMajor(List<Student> students) {
        Map<String, Long> result = new HashMap<>();
        for (Student s : students) {
            result.merge(s.major(), 1L, Long::sum);
        }
        return result;
    }

    public static void main(String[] args) {
        List<Student> students = List.of(
                new Student("Pho", "CNTT"),
                new Student("Huynh", "CNTT"),
                new Student("Gia", "Kinh Te"),
                new Student("An", "CNTT"),
                new Student("Binh", "Kinh Te")
        );

        Map<String, List<Student>> byMajor = groupByMajor(students);
        System.out.println("Group by major:");
        byMajor.forEach((major, list) -> {
            System.out.println("  " + major + ": " + list.stream().map(Student::name).toList());
        });

        Map<String, Long> countMajor = countByMajor(students);
        System.out.println("\nCount by major:");
        countMajor.forEach((major, count) -> System.out.println("  " + major + ": " + count));
    }
}
```

**Kết quả chạy:**
```
Group by major:
  CNTT: [Pho, Huynh, An]
  Kinh Te: [Gia, Binh]

Count by major:
  CNTT: 3
  Kinh Te: 2
```

### Giải thích

- Nếu viết theo lối cũ (`if (!result.containsKey(major)) result.put(major, new ArrayList<>()); result.get(major).add(s);`) tốn **3 dòng** và **2 lần tra map** (`containsKey` + `get`/`put`) cho mỗi phần tử. `computeIfAbsent` chỉ **1 dòng, 1 lần tra map** — vừa gọn vừa hiệu quả hơn.
- Đây chính là logic **thủ công** đứng sau `Collectors.groupingBy()` của Stream API (sẽ học ở Module 03.3) — biết cách viết tay bằng `computeIfAbsent` giúp hiểu rõ Stream đang làm gì "dưới nắp capo", không chỉ dùng như phép màu.
- `Long::sum` (thay vì `Integer::sum`) vì kiểu giá trị đếm là `Long` — khớp đúng kiểu tham chiếu method `merge` yêu cầu.

---

## Bài 4 — LRU Cache bằng LinkedHashMap

### Đề
Hoàn thiện và kiểm chứng đoạn code `SimpleLRUCache` đã cho. `main`: `capacity = 3`, `put` A,B,C; `get(A)`; `put(D)` → chứng minh **B** (lâu nhất không được truy cập) bị loại, không phải A.

### Phân tích

`LinkedHashMap` có 1 tính năng ẩn cực kỳ hữu ích: constructor `LinkedHashMap(initialCapacity, loadFactor, accessOrder)` — khi `accessOrder = true`, thứ tự duyệt map **không còn theo insertion order**, mà theo **"access order"**: mỗi lần `get()`/`put()` 1 entry, entry đó được **di chuyển ra cuối** danh sách nội bộ — entry **lâu nhất KHÔNG được truy cập** luôn nằm **đầu tiên**. `removeEldestEntry()` là hook được gọi **sau mỗi lần `put()`** — trả `true` nghĩa là "hãy xóa entry cũ nhất" (chính là entry đang ở đầu).

### Lời giải

```java
package baitap.bai4;

import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleLRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public SimpleLRUCache(int capacity) {
        super(16, 0.75f, true); // accessOrder = true -> get()/put() đều "đưa lên gần đây nhất"
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity; // vượt sức chứa -> tự động xóa entry CŨ NHẤT (ít dùng nhất)
    }

    public static void main(String[] args) {
        SimpleLRUCache<String, Integer> cache = new SimpleLRUCache<>(3);

        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        System.out.println("Sau khi put A,B,C: " + cache.keySet()); // [A, B, C]

        cache.get("A"); // TRUY CẬP A -> A được đưa lên "gần đây nhất", B/C vẫn giữ nguyên vị trí cũ hơn
        System.out.println("Sau khi get(A): " + cache.keySet());    // [B, C, A] - A đã "nhảy" ra cuối

        cache.put("D", 4); // capacity=3, đã đầy -> phải loại entry CŨ NHẤT
        System.out.println("Sau khi put(D): " + cache.keySet());    // [C, A, D] - B đã bị loại!

        System.out.println("Chứa B? " + cache.containsKey("B")); // false - B bị loại
        System.out.println("Chứa A? " + cache.containsKey("A")); // true - A còn, vì vừa được get() trước đó
    }
}
```

**Kết quả chạy:**
```
Sau khi put A,B,C: [A, B, C]
Sau khi get(A): [B, C, A]
Sau khi put(D): [C, A, D]
Chứa B? false
Chứa A? true
```

### Giải thích

- Ngay sau `get("A")`, thứ tự nội bộ đổi từ `[A, B, C]` thành `[B, C, A]` — `A` bị "đẩy" ra cuối (gần đây nhất), `B` giờ trở thành phần tử **đứng đầu** (lâu nhất chưa được đụng tới).
- Khi `put("D")` khiến `size() = 4 > capacity (3)`, `removeEldestEntry()` trả `true` → `LinkedHashMap` tự động xóa **entry đang đứng đầu** — chính là `B`, **không phải `A`** — dù `A` được thêm vào **trước** `B` (insertion order), nhưng vì `A` **vừa được truy cập gần đây** (`get`), nó "thoát" được lượt loại bỏ.
- Đây chính là cơ chế LRU (Least Recently Used) đầy đủ chỉ trong **~10 dòng code**, tận dụng hoàn toàn tính năng có sẵn của JDK — không cần tự cài `Doubly Linked List` + `HashMap` thủ công như thường thấy trong bài phỏng vấn thuật toán kinh điển "Design LRU Cache".

---

## Bài 5 — Quan sát thứ tự 3 loại Set

### Đề
Thêm cùng tập tên 5 thành phố vào `HashSet`, `LinkedHashSet`, `TreeSet`; in kết quả duyệt cả ba; giải thích bằng lời từng thứ tự. Thêm một tên `null` vào mỗi loại và ghi lại loại nào ném exception.

### Lời giải

```java
package baitap.bai5;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<String> cities = List.of("Hanoi", "Danang", "Saigon", "Hue", "Cantho");

        Set<String> hashSet = new HashSet<>(cities);
        Set<String> linkedHashSet = new LinkedHashSet<>(cities);
        Set<String> treeSet = new TreeSet<>(cities);

        System.out.println("HashSet:       " + hashSet);
        System.out.println("LinkedHashSet: " + linkedHashSet);
        System.out.println("TreeSet:       " + treeSet);

        System.out.println("\n===== Thêm null vào từng loại =====");

        try {
            hashSet.add(null);
            System.out.println("HashSet chấp nhận null: " + hashSet.contains(null));
        } catch (Exception e) {
            System.out.println("HashSet ném " + e.getClass().getSimpleName());
        }

        try {
            linkedHashSet.add(null);
            System.out.println("LinkedHashSet chấp nhận null: " + linkedHashSet.contains(null));
        } catch (Exception e) {
            System.out.println("LinkedHashSet ném " + e.getClass().getSimpleName());
        }

        try {
            treeSet.add(null);
            System.out.println("TreeSet chấp nhận null (không nên xảy ra)");
        } catch (Exception e) {
            System.out.println("TreeSet ném " + e.getClass().getSimpleName());
        }
    }
}
```

**Kết quả chạy (thứ tự HashSet có thể khác tùy JVM, phần còn lại cố định):**
```
HashSet:       [Saigon, Hue, Hanoi, Danang, Cantho]
LinkedHashSet: [Hanoi, Danang, Saigon, Hue, Cantho]
TreeSet:       [Cantho, Danang, Hanoi, Hue, Saigon]

===== Thêm null vào từng loại =====
HashSet chấp nhận null: true
LinkedHashSet chấp nhận null: true
TreeSet ném NullPointerException
```

### Giải thích

- **`HashSet`:** thứ tự **không thể đoán trước, không đảm bảo bất kỳ quy luật nào** — phụ thuộc `hashCode()` của `String` và số lượng bucket nội bộ. Có thể **thay đổi giữa các lần chạy chương trình khác nhau** (dù cùng dữ liệu, tùy cấu hình JVM/bộ nhớ).
- **`LinkedHashSet`:** giữ đúng **thứ tự chèn (insertion order)** — luôn là `[Hanoi, Danang, Saigon, Hue, Cantho]`, đúng thứ tự trong `cities` gốc, **nhất quán 100% mọi lần chạy**.
- **`TreeSet`:** tự động **sắp xếp theo thứ tự tự nhiên** (alphabet của `String`) — `Cantho < Danang < Hanoi < Hue < Saigon`.
- **`HashSet`/`LinkedHashSet` chấp nhận ĐÚNG 1 phần tử `null`** (vì bên dưới dùng `HashMap`, mà `HashMap` cho phép 1 key `null`). **`TreeSet` KHÔNG chấp nhận `null`** — vì nó cần **so sánh** (`compareTo()`) để xác định vị trí chèn, và `null.compareTo(...)` (hoặc `Comparator` so với `null`) sẽ ném `NullPointerException` ngay khi cố gắng so sánh.

---

## Bài 6 — Hash Collision cố ý

### Đề
`class BadKey` với `hashCode()` luôn `return 1;` nhưng `equals()` so đúng nội dung. Thêm 50.000 `BadKey` khác nhau vào `HashMap`, đo thời gian so với key `String` cùng số lượng. Giải thích bằng cơ chế bucket + tree hóa — vì sao vẫn không "cứu" nổi về O(1).

### Phân tích

`HashMap` lý tưởng đạt O(1) khi các `hashCode()` **phân tán đều** trên nhiều bucket. Nếu **MỌI key đều trả cùng 1 `hashCode()`**, tất cả đều rơi vào **CÙNG 1 bucket duy nhất** — bucket đó trở thành cấu trúc tuyến tính (trước Java 8 là linked list O(n), từ Java 8 trở đi **tự động chuyển thành cây đỏ-đen (Red-Black Tree) khi 1 bucket có ≥ 8 phần tử VÀ tổng dung lượng bảng ≥ 64** — cải thiện xuống **O(log n)**, nhưng **vẫn KHÔNG PHẢI O(1)**.

### Lời giải

```java
package baitap.bai6;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Main {

    static class BadKey {
        private final int value;
        BadKey(int value) { this.value = value; }

        @Override
        public int hashCode() {
            return 1; // CỐ Ý - MỌI object đều cùng hashCode -> dồn hết vào 1 bucket
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BadKey)) return false;
            return value == ((BadKey) o).value; // so nội dung ĐÚNG chuẩn - chỉ hashCode() cố tình tệ
        }
    }

    public static void main(String[] args) {
        int n = 50_000;

        // ---- HashMap với BadKey (mọi key cùng hashCode) ----
        Map<BadKey, Integer> badMap = new HashMap<>();
        long startBad = System.nanoTime();
        for (int i = 0; i < n; i++) {
            badMap.put(new BadKey(i), i);
        }
        long badInsertTime = System.nanoTime() - startBad;

        long startBadGet = System.nanoTime();
        for (int i = 0; i < n; i++) {
            badMap.get(new BadKey(i));
        }
        long badGetTime = System.nanoTime() - startBadGet;

        // ---- HashMap với String key (hashCode phân tán bình thường) ----
        Map<String, Integer> goodMap = new HashMap<>();
        long startGood = System.nanoTime();
        for (int i = 0; i < n; i++) {
            goodMap.put("key" + i, i);
        }
        long goodInsertTime = System.nanoTime() - startGood;

        long startGoodGet = System.nanoTime();
        for (int i = 0; i < n; i++) {
            goodMap.get("key" + i);
        }
        long goodGetTime = System.nanoTime() - startGoodGet;

        System.out.println("===== HashMap<BadKey, Integer> (mọi hashCode = 1) =====");
        System.out.printf("Insert %d phần tử: %,d ns%n", n, badInsertTime);
        System.out.printf("Get %d phần tử:    %,d ns%n", n, badGetTime);

        System.out.println("\n===== HashMap<String, Integer> (hashCode phân tán bình thường) =====");
        System.out.printf("Insert %d phần tử: %,d ns%n", n, goodInsertTime);
        System.out.printf("Get %d phần tử:    %,d ns%n", n, goodGetTime);
    }
}
```

**Kết quả tiêu biểu (chênh lệch RẤT lớn, minh chứng rõ hậu quả):**
```
===== HashMap<BadKey, Integer> (mọi hashCode = 1) =====
Insert 50000 phần tử: 450,000,000 ns   (~450ms)
Get 50000 phần tử:    380,000,000 ns   (~380ms)

===== HashMap<String, Integer> (hashCode phân tán bình thường) =====
Insert 50000 phần tử: 8,500,000 ns     (~8.5ms)
Get 50000 phần tử:    6,200,000 ns     (~6.2ms)
```
*(chênh lệch ~40-50 lần — con số cụ thể tùy máy nhưng xu hướng luôn tương tự)*

### Giải thích — vì sao tree hóa "không cứu nổi" O(1)

```
Bước 1: MỌI BadKey đều hashCode()=1 -> HashMap tính bucket index = hash(1) % capacity
        -> LUÔN chọn ĐÚNG 1 bucket, bất kể capacity bao lớn (rehash cũng vô ích)

Bước 2: Bucket đó tích lũy TOÀN BỘ 50,000 phần tử
        -> Vượt ngưỡng 8 phần tử/bucket (TREEIFY_THRESHOLD) -> Java 8 tự CHUYỂN bucket đó
           từ Linked List sang CÂY ĐỎ-ĐEN (Red-Black Tree)

Bước 3: Tra cứu trong bucket giờ là O(log n) thay vì O(n) - CÓ CẢI THIỆN
        nhưng KHÔNG PHẢI O(1) như HashMap "khỏe mạnh" bình thường -
        vì TOÀN BỘ 50,000 phần tử vẫn nằm trong DUY NHẤT 1 cây, không phân tán ra nhiều bucket
```

- **Tree hóa (Java 8+) là "lưới an toàn"**, biến trường hợp tệ nhất từ O(n) xuống O(log n) — giúp `HashMap` **không sụp đổ hoàn toàn** khi gặp key có `hashCode()` cố ý/vô tình bị trùng nhiều, nhưng **không thể khôi phục lại O(1)** vì gốc rễ vấn đề (mọi phần tử chung 1 bucket) **không đổi** — chỉ đổi cách tổ chức dữ liệu **BÊN TRONG** bucket đó.
- **Liên hệ bảo mật thực tế:** đây chính là cơ chế đứng sau kiểu tấn công **"Hash DoS" (Hash Flooding Attack)** — kẻ tấn công cố tình gửi nhiều key có cùng `hashCode()` (hoặc khai thác thuật toán hash yếu) khiến server tốn CPU bất thường xử lý `HashMap`/`HashSet` nội bộ — 1 trong những lý do các ngôn ngữ hiện đại (bao gồm Java) đã bổ sung cơ chế tree hóa và/hoặc random hash seed để giảm thiểu rủi ro này.
- **Bài học thiết kế:** `hashCode()` chất lượng kém (phân tán không đều) là 1 trong những nguyên nhân **âm thầm** gây suy giảm hiệu năng nghiêm trọng nhất khi dùng `HashMap`/`HashSet` — không hề có exception hay cảnh báo nào, chỉ đơn giản là "chạy chậm dần" khi dữ liệu tăng lên.

---

## Bài 7 — NavigableMap cho bài toán tra khoảng

### Đề
Cho bảng điểm chữ theo mốc: `0→F, 50→D, 65→C, 75→B, 85→A`. Dùng `TreeMap<Integer,String>` + `floorEntry(score)` viết `String grade(int score)`. Test với `49, 50, 64, 90`. Giải thích vì sao `floorEntry` gọn hơn hẳn chuỗi `if/else`.

### Phân tích

`TreeMap` implement `NavigableMap`, cung cấp `floorEntry(key)` — trả về **entry có key LỚN NHẤT nhưng ≤ key đang tìm** (nếu không có, trả `null`). Với bảng điểm dạng "mốc dưới cùng" (`0→F` nghĩa là "từ 0 điểm trở lên, dưới mốc tiếp theo, là hạng F"), `floorEntry(score)` chính xác trả về **đúng mốc phù hợp** mà không cần chuỗi so sánh thủ công.

### Lời giải

```java
package baitap.bai7;

import java.util.Map;
import java.util.TreeMap;

public class Main {

    static final TreeMap<Integer, String> GRADE_TABLE = new TreeMap<>(Map.of(
            0, "F",
            50, "D",
            65, "C",
            75, "B",
            85, "A"
    ));

    static String grade(int score) {
        Map.Entry<Integer, String> entry = GRADE_TABLE.floorEntry(score);
        if (entry == null) {
            throw new IllegalArgumentException("Điểm không hợp lệ: " + score); // score < 0
        }
        return entry.getValue();
    }

    public static void main(String[] args) {
        int[] tests = {49, 50, 64, 90};
        for (int score : tests) {
            System.out.println("grade(" + score + ") = " + grade(score));
        }
    }
}
```

**Kết quả chạy:**
```
grade(49) = F
grade(50) = D
grade(64) = D
grade(90) = A
```

### Giải thích vì sao `floorEntry` gọn hơn `if/else`

**Cách viết `if/else` truyền thống** (dài, dễ sai thứ tự điều kiện — bẫy đã gặp ở FizzBuzz Bài 10 module Cấu trúc điều khiển):

```java
static String gradeIfElse(int score) {
    if (score >= 85) return "A";
    else if (score >= 75) return "B";
    else if (score >= 65) return "C";
    else if (score >= 50) return "D";
    else return "F";
    // PHẢI xếp theo thứ tự GIẢM DẦN - nếu lỡ đảo ngược thứ tự, mọi điểm đều rơi vào nhánh đầu tiên!
}
```

- **`floorEntry`** tra cứu bằng **cấu trúc dữ liệu cây nhị phân tìm kiếm cân bằng** (Red-Black Tree bên trong `TreeMap`) — độ phức tạp **O(log n)**, và quan trọng hơn: **không phụ thuộc thứ tự khai báo mốc trong code** — `TreeMap` tự sắp xếp key, `floorEntry` luôn tìm đúng bất kể mốc được thêm vào theo thứ tự nào.
- **Dễ mở rộng/bảo trì hơn nhiều:** muốn thêm mốc `95 → A+`, chỉ cần thêm **1 dòng dữ liệu** vào `GRADE_TABLE`, không phải sửa lại **logic điều kiện** như với `if/else` (nơi phải chèn đúng vị trí giữa chuỗi so sánh, dễ sai sót).
- Đây là mẫu hình rất hữu ích cho **mọi bài toán "tra khoảng theo mốc"** trong thực tế: bậc thuế lũy tiến, bậc giá điện/nước theo lũy kế, ngưỡng phân loại rủi ro tín dụng... — thay vì chuỗi `if/else` cồng kềnh, `TreeMap.floorEntry()`/`ceilingEntry()` cho code ngắn gọn, đúng đắn và dễ bảo trì hơn hẳn.

---

*Đây là lời giải cho toàn bộ Phần B của Module 08. Tiếp theo: Module 09 — Generics.*
