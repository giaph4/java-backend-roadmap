# Module 03.1 — Collections Framework

> **Mức độ ưu tiên: Cao** — Chọn sai cấu trúc dữ liệu (dùng `ArrayList` cho thao tác chèn/xóa đầu danh sách liên tục, dùng `LinkedList` rồi `get(i)` trong vòng lặp, quên `hashCode()` khiến `HashMap` tụt xuống O(n)...) là nguyên nhân phổ biến nhất gây bug hiệu năng âm thầm trong backend. "So sánh `ArrayList` và `LinkedList`", "`HashMap` hoạt động thế nào", "`ConcurrentModificationException` xảy ra khi nào" gần như chắc chắn xuất hiện ở mọi vòng phỏng vấn kỹ thuật.

> **Phạm vi bài này:** các interface và class trong `java.util` cho cấu trúc dữ liệu **đơn luồng**: `Collection` (List/Set/Queue/Deque), `Map`, lớp tiện ích `Collections`/`Arrays`, iterator fail-fast, và cách chọn cấu trúc theo độ phức tạp Big-O. **Chỉ nhắc tên, không đi sâu:** collection đồng bộ/đồng thời (`ConcurrentHashMap`, `CopyOnWriteArrayList`, `BlockingQueue` — Module 09 Concurrency), Generics (`<T>`, wildcard — Module 03.2), Stream/Collector (Module 03.3). Những chỗ chạm tới chủ đề khác chỉ nêu đủ để bài này trọn vẹn.

---

## Mục lục

1. [Tổng quan Collections Framework](#1-tổng-quan-collections-framework)
2. [List — ArrayList vs LinkedList](#2-list--arraylist-vs-linkedlist)
3. [Set — HashSet, LinkedHashSet, TreeSet, EnumSet](#3-set--hashset-linkedhashset-treeset-enumset)
4. [Map — HashMap, LinkedHashMap, TreeMap và họ hàng](#4-map--hashmap-linkedhashmap-treemap-và-họ-hàng)
5. [Cơ chế bên trong HashMap (đào sâu)](#5-cơ-chế-bên-trong-hashmap-đào-sâu)
6. [Iterator fail-fast & ConcurrentModificationException](#6-iterator-fail-fast--concurrentmodificationexception)
7. [Bảng độ phức tạp Big-O tổng hợp](#7-bảng-độ-phức-tạp-big-o-tổng-hợp)
8. [Queue & Deque — hai họ method, ArrayDeque, PriorityQueue](#8-queue--deque--hai-họ-method-arraydeque-priorityqueue)
9. [An toàn luồng & bất biến](#9-an-toàn-luồng--bất-biến)
10. [Bẫy thường gặp](#10-bẫy-thường-gặp)
11. [Cây quyết định: chọn cấu trúc dữ liệu nào?](#11-cây-quyết-định-chọn-cấu-trúc-dữ-liệu-nào)
12. [Tổng kết — Bảng ghi nhớ nhanh](#12-tổng-kết--bảng-ghi-nhớ-nhanh)
13. [Bài tập luyện tập](#13-bài-tập-luyện-tập)

---

## 1. Tổng quan Collections Framework

Collections Framework (`java.util`) chuẩn hóa các cấu trúc dữ liệu **động** — tự mở rộng/thu hẹp — giải quyết hạn chế "kích thước cố định" của mảng (Module 01.1).

### Cây kế thừa interface

```
                      Iterable<T>                        (chỉ có iterator() — cho phép dùng for-each)
                          │
                     Collection<E>                       add, remove, contains, size, isEmpty, stream...
        ┌─────────────────┼──────────────────┐
      List<E>           Set<E>            Queue<E>
        │                 │                  │
        │            SortedSet<E>         Deque<E>        (hàng đợi 2 đầu; cũng là Queue)
        │                 │
        │           NavigableSet<E>                       ceiling/floor/higher/lower/pollFirst...
        │
   RandomAccess (marker — báo "get(i) là O(1)")

                       Map<K,V>                           ĐỘC LẬP — KHÔNG kế thừa Collection
                          │
                    SortedMap<K,V>
                          │
                   NavigableMap<K,V>                      firstEntry/floorKey/ceilingEntry/headMap...
```

| Interface | Ý nghĩa |
|---|---|
| `Iterable<E>` | Gốc của mọi thứ duyệt được bằng `for (E e : c)`. Chỉ yêu cầu `iterator()`. |
| `Collection<E>` | "Một nhóm phần tử". Cha chung của `List`, `Set`, `Queue`. |
| `List<E>` | Có thứ tự theo **vị trí (index)**, cho **trùng lặp**, truy cập qua `get(i)`. |
| `Set<E>` | **Không trùng lặp** (theo `equals`/`hashCode` — Module 02.4). |
| `SortedSet` / `NavigableSet` | `Set` luôn sắp xếp + truy vấn "phần tử gần nhất" (`ceiling`, `floor`...). |
| `Queue<E>` | Thường FIFO; có cặp method "ném exception" và "trả giá trị đặc biệt" (mục 8). |
| `Deque<E>` | Thêm/xóa **cả hai đầu**; dùng làm Queue **hoặc** Stack. |
| `Map<K,V>` | Cặp **key → value**, key không trùng. **Không** phải `Collection`. |
| `SortedMap` / `NavigableMap` | `Map` sắp xếp theo key + truy vấn khoảng. |

> **`Map` không kế thừa `Collection`** vì nó lưu *cặp* key-value, không phải một chuỗi phần tử đơn. Nhưng ba "view" của nó — `keySet()`, `values()`, `entrySet()` — thì đúng là `Collection`.

### Lớp skeletal (bộ khung) và lớp cũ (legacy)

| Nhóm | Thành viên | Ghi chú |
|---|---|---|
| Skeletal — cài sẵn phần lặp lại, kế thừa để tự viết collection | `AbstractList`, `AbstractSet`, `AbstractMap`, `AbstractQueue` | Liên hệ "skeletal implementation" ở Module 01.5. |
| Legacy — có từ Java 1.0/1.1, **đồng bộ hóa cứng**, nên tránh | `Vector`, `Stack` (kế thừa `Vector`), `Hashtable`, `Enumeration`, `Properties` | Thay bằng `ArrayList`, `ArrayDeque`, `HashMap`. `Properties` vẫn dùng cho file `.properties`. |

### Lớp tiện ích tĩnh

```java
// java.util.Collections — thao tác trên collection đã có
Collections.sort(list);                 Collections.sort(list, comparator);
Collections.reverse(list);              Collections.shuffle(list);
Collections.max(coll);                  Collections.min(coll, cmp);
Collections.frequency(coll, obj);       Collections.binarySearch(sortedList, key);
Collections.emptyList();                Collections.singletonList(x);   // bất biến, 0 hoặc 1 phần tử
Collections.unmodifiableList(list);     // VIEW chỉ-đọc bọc quanh list gốc (mục 9)
Collections.synchronizedMap(map);       // bọc đồng bộ (mục 9)

// java.util.Arrays — cầu nối mảng ↔ collection
List<String> a = Arrays.asList("x", "y");   // List cố định kích thước, ghi xuyên xuống mảng (mục 10)
int[] arr = {3, 1, 2};  Arrays.sort(arr);   Arrays.stream(arr);

// Factory bất biến (Java 9+) — ngắn, null-hostile (ném NPE nếu có phần tử null)
List<Integer> l = List.of(1, 2, 3);
Set<String>  s = Set.of("a", "b");
Map<String,Integer> m = Map.of("a", 1, "b", 2);
Map<String,Integer> big = Map.ofEntries(Map.entry("a", 1), Map.entry("b", 2));
List<Integer> copy = List.copyOf(l);        // bản sao phòng thủ, bất biến
```

---

## 2. List — ArrayList vs LinkedList

`List` **có thứ tự theo vị trí**, cho **trùng lặp**, truy cập qua **index**.

### `ArrayList` — mảng động

```java
List<String> names = new ArrayList<>();      // hoặc new ArrayList<>(1000) nếu biết trước quy mô
names.add("Pho");                            // thêm cuối — O(1) amortized
names.get(0);                                // O(1) — tính địa chỉ trực tiếp
names.add(1, "Binh");                        // chèn giữa — O(n), dịch phần tử phía sau
```

**Bên trong:** một mảng `Object[] elementData`. Khi đầy, tạo mảng mới lớn hơn **1.5 lần** (`newCap = oldCap + (oldCap >> 1)`), `System.arraycopy` toàn bộ sang. Vì vậy `add()` cuối **thường** O(1), **thỉnh thoảng** O(n) khi resize → gọi là *amortized O(1)*.

- `new ArrayList<>()` khởi tạo mảng **rỗng dùng chung**; mảng thật cấp phát ở lần `add()` đầu (mặc định 10 phần tử).
- Biết trước số lượng → `new ArrayList<>(n)` hoặc `list.ensureCapacity(n)` để tránh resize nhiều lần. `list.trimToSize()` để trả lại phần thừa.
- `ArrayList` cài `RandomAccess` → thuật toán trong JDK (như `Collections.binarySearch`) tự chọn nhánh duyệt theo index.

### `LinkedList` — danh sách liên kết đôi (và **cũng là `Deque`**)

```java
LinkedList<String> dq = new LinkedList<>();
dq.addFirst("An");   dq.addLast("Pho");      // O(1) ở cả hai đầu
dq.get(500);                                 // O(n) — duyệt từ đầu hoặc cuối, tùy gần hơn
```

**Bên trong:** mỗi phần tử là một `Node` chứa `item` + con trỏ `prev`/`next`. Chèn/xóa ở đầu/cuối chỉ đổi vài con trỏ. Nhưng **không có** truy cập ngẫu nhiên — `get(i)` phải đi bộ.

### Bảng so sánh

| Thao tác | `ArrayList` | `LinkedList` |
|---|---|---|
| `get(i)` / `set(i, x)` | **O(1)** | **O(n)** |
| `add(x)` cuối | O(1) amortized | O(1) |
| `addFirst` / `removeFirst` | O(n) (dịch toàn bộ) | **O(1)** |
| `add(i, x)` / `remove(i)` giữa | O(n) (dịch phần đuôi) | O(n) tìm vị trí + O(1) nối lại |
| `contains(x)` / `indexOf(x)` | O(n) | O(n) |
| Bộ nhớ / phần tử | Gọn (chỉ dữ liệu) | Tốn (thêm 2 con trỏ + header Node) |
| Cache locality (tốc độ CPU) | **Tốt** — dữ liệu liền kề | Kém — Node rải rác trên Heap |

> ⚠️ **Bẫy `LinkedList` + vòng lặp theo index:**
> ```java
> for (int i = 0; i < list.size(); i++) list.get(i);   // O(n²) trên LinkedList!
> for (String x : list) { ... }                        // O(n) — for-each dùng iterator, đi bộ 1 lần
> ```

> **Khuyến nghị:** **~95% trường hợp backend dùng `ArrayList`** — đa số thao tác là đọc/duyệt, và cache locality thắng lớn. Chỉ cân nhắc `LinkedList` khi chèn/xóa liên tục ở **hai đầu** và gần như không `get(i)` — mà khi đó `ArrayDeque` thường còn nhanh hơn (mục 8).

### Hai "view" của List cần biết

```java
// subList(from, to) — CỬA SỔ trên list gốc, không phải bản sao
List<Integer> window = big.subList(2, 5);
window.clear();                 // idiom xóa nhanh đoạn [2,5) khỏi big
// ⚠️ sau khi structural-modify `big` trực tiếp, mọi thao tác trên `window` ném ConcurrentModificationException

// Arrays.asList(...) — kích thước CỐ ĐỊNH, ghi xuyên xuống mảng nguồn
List<Integer> fixed = Arrays.asList(1, 2, 3);
fixed.set(0, 9);               // OK — mảng nguồn cũng đổi
fixed.add(4);                  // ✗ UnsupportedOperationException
```

---

## 3. Set — HashSet, LinkedHashSet, TreeSet, EnumSet

`Set` **không cho trùng lặp** — dựa trực tiếp vào `equals()`/`hashCode()` (`HashSet`, `LinkedHashSet`) hoặc `compareTo`/`Comparator` (`TreeSet`) — xem Module 02.4.

### `HashSet` — nhanh nhất, KHÔNG thứ tự

```java
Set<String> tags = new HashSet<>();
tags.add("java"); tags.add("spring"); tags.add("java");   // lần 3 bị bỏ qua
tags.size();  // 2
```

Cài đặt = một `HashMap` nội bộ (phần tử là key, value là một `Object` hằng). Thứ tự duyệt **không xác định** và **có thể đổi sau khi resize**.

### `LinkedHashSet` — giữ thứ tự thêm vào

```java
Set<String> tags = new LinkedHashSet<>();   // duyệt: đúng thứ tự add()
```

`HashMap` + một danh sách liên kết đôi xâu các entry theo thứ tự chèn. Chậm hơn `HashSet` không đáng kể; là lựa chọn tốt khi cần **kết quả ổn định để test/log/debug**.

### `TreeSet` — luôn sắp xếp (Red-Black Tree)

```java
NavigableSet<Integer> s = new TreeSet<>(List.of(85, 60, 92, 60));  // {60, 85, 92}
s.first(); s.last();                       // 60 / 92
s.ceiling(70);  // 85  — phần tử nhỏ nhất ≥ 70
s.floor(70);    // 60  — phần tử lớn nhất ≤ 70
s.higher(85);   // 92  — chặt lớn hơn
s.headSet(85);  // [60]           (< 85)
s.tailSet(85);  // [85, 92]       (≥ 85)
s.pollFirst();  // 60 và xóa khỏi set
s.descendingSet(); // view duyệt ngược
```

Cây tìm kiếm nhị phân tự cân bằng — mọi thao tác O(log n).

> ⚠️ `TreeSet`/`TreeMap` định nghĩa "trùng nhau" **hoàn toàn bằng `compareTo`/`Comparator`**, phớt lờ `equals()`. Comparator trả `0` cho hai phần tử khác nhau → một trong hai **bị mất** khi thêm vào `TreeSet` (Module 02.4, mục 11).

### `EnumSet` — Set của hằng enum, cực nhanh

```java
enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }
EnumSet<Day> workdays = EnumSet.range(Day.MON, Day.FRI);
EnumSet<Day> weekend  = EnumSet.complementOf(workdays);   // {SAT, SUN}
```

Cài đặt bằng **một `long` bit-vector** (khi ≤ 64 hằng) → nhanh hơn `HashSet` nhiều lần, không rác. Luôn ưu tiên khi phần tử là enum.

### Bảng so sánh

| Tiêu chí | `HashSet` | `LinkedHashSet` | `TreeSet` | `EnumSet` |
|---|---|---|---|---|
| Thứ tự duyệt | Không xác định | Thứ tự thêm vào | Thứ tự sắp xếp | Thứ tự khai báo enum |
| `add`/`contains`/`remove` | O(1) TB | O(1) TB | O(log n) | O(1), hằng số rất nhỏ |
| Cho `null` | 1 phần tử `null` | 1 phần tử `null` | **Không** (NPE khi so sánh) | **Không** |
| Nền tảng | `HashMap` | `HashMap` + linked list | Red-Black Tree | bit-vector `long` |

---

## 4. Map — HashMap, LinkedHashMap, TreeMap và họ hàng

`Map` lưu cặp **key → value**; `put()` key đã tồn tại → **ghi đè** value cũ và trả về value cũ.

```java
Map<String, Integer> score = new HashMap<>();
score.put("Pho", 90);
score.put("Pho", 95);                 // ghi đè; trả về 90
score.get("Pho");                     // 95
score.get("Binh");                    // null
score.getOrDefault("Binh", 0);        // 0
score.containsKey("An");              // false

for (Map.Entry<String, Integer> e : score.entrySet()) { e.getKey(); e.getValue(); }
score.forEach((k, v) -> System.out.println(k + "=" + v));
```

### Ba "view" — đều là cửa sổ sống, xóa xuyên xuống Map

```java
Set<String>            keys   = score.keySet();
Collection<Integer>    vals   = score.values();
Set<Map.Entry<String,Integer>> entries = score.entrySet();

keys.remove("Pho");                   // XÓA luôn entry "Pho" khỏi map
score.entrySet().removeIf(e -> e.getValue() < 60);   // cách xóa theo điều kiện AN TOÀN khi đang duyệt
for (Map.Entry<String,Integer> e : entries) e.setValue(e.getValue() + 1);  // sửa value tại chỗ hợp lệ
```

### Họ method hiện đại (Java 8+) — tránh `if (map.containsKey...) else`

```java
score.putIfAbsent("An", 0);                       // chỉ đặt nếu chưa có
score.merge("java", 1, Integer::sum);             // đếm tần suất: chưa có → 1; có → cộng dồn
score.computeIfAbsent("SE101", k -> new ArrayList<>()).add("Pho");  // multimap
score.computeIfPresent("Pho", (k, v) -> v - 5);   // chỉ tính nếu key đang có
score.compute("An", (k, v) -> (v == null ? 0 : v) + 1);
score.replaceAll((k, v) -> Math.min(v, 100));
```

> ⚠️ `merge`/`compute`/`computeIfPresent`: nếu hàm remap trả về `null` thì entry **bị xóa** khỏi map. Đây là hành vi có chủ đích (dọn key rỗng) nhưng dễ bất ngờ.

### `HashMap` / `LinkedHashMap` / `TreeMap` — thứ tự & hiệu năng

Song song hoàn toàn với `HashSet`/`LinkedHashSet`/`TreeSet` ở mục 3 (thực chất `HashSet` *được cài bằng* `HashMap`). `TreeMap` là `NavigableMap`: `firstEntry()`, `floorKey(k)`, `ceilingEntry(k)`, `headMap(k)`, `subMap(lo, hi)`, `descendingMap()`.

### Các `Map` chuyên dụng

| Lớp | Khi nào dùng |
|---|---|
| `EnumMap<K extends Enum, V>` | Key là enum — cài bằng mảng theo `ordinal`, nhanh hơn `HashMap` nhiều, thứ tự = thứ tự khai báo. |
| `IdentityHashMap` | So key bằng `==` thay vì `equals()` — dùng cho bài toán "định danh object" (serializer, phát hiện chu trình). |
| `WeakHashMap` | Key được giữ bằng *weak reference* — entry tự biến mất khi key không còn ai tham chiếu. Dùng làm cache phụ trợ / metadata. |
| `Properties` | Cấu hình `key=value` dạng `String`, đọc/ghi file `.properties`. |
| `LinkedHashMap` (access-order) | `new LinkedHashMap<>(16, .75f, true)` + override `removeEldestEntry` → **LRU cache** (bài tập B4). |

---

## 5. Cơ chế bên trong HashMap (đào sâu)

Phần **hay hỏi sâu nhất** ở phỏng vấn Middle/Senior.

### Cấu trúc

```
HashMap = Node<K,V>[] table       // mảng bucket; kích thước LUÔN là lũy thừa của 2

table == null cho tới lần put() đầu tiên  (lazy init, mặc định 16 bucket)

table[ 0] → null
table[ 5] → Node(hash, "An", 85, next) → Node(hash, "Cuong", 70, null)   // va chạm: 2 key, 1 bucket
table[11] → (TreeNode ... )            // bucket đã "tree hóa" thành cây đỏ-đen
```

### `put(key, value)` — từng bước

1. `h = key.hashCode()`.
2. **Hash spreading:** `hash = h ^ (h >>> 16)` — trộn 16 bit cao xuống 16 bit thấp, để những `hashCode` chỉ khác nhau ở bit cao không cùng đổ vào một bucket.
3. **Chỉ số bucket:** `index = (table.length - 1) & hash` — tương đương `hash % length` nhưng nhanh hơn *vì* `length` là lũy thừa của 2.
4. Bucket rỗng → đặt `Node` mới.
5. Bucket có phần tử (va chạm) → duyệt: `key` trùng (so bằng `hash` rồi `equals()`) → **ghi đè value**; không trùng → **nối `Node` mới vào cuối**.
6. Nếu `++size > threshold` (`= capacity * loadFactor`) → **resize** (mục dưới).

`get(key)` đi đúng ba bước 1-3 để ra bucket, rồi `equals()` trong bucket.

### Va chạm (collision) — vì sao vẫn O(1) trung bình?

`hashCode()` phân tán tốt ⇒ số phần tử mỗi bucket ≈ hằng số ⇒ duyệt trong bucket ≈ O(1). Ngược lại, `hashCode()` tồi (ví dụ luôn trả hằng số) ⇒ **mọi** phần tử vào **một** bucket ⇒ `HashMap` thoái hóa thành danh sách liên kết O(n). Đây chính là lý do **contract `equals`/`hashCode`** (Module 02.4) quan trọng đến vậy.

### Tree hóa bucket (Java 8+)

| Ngưỡng | Giá trị | Ý nghĩa |
|---|---|---|
| `TREEIFY_THRESHOLD` | 8 | Bucket có ≥ 8 Node → chuyển từ linked list sang **cây đỏ-đen** (O(n) → O(log n) trong bucket). |
| `MIN_TREEIFY_CAPACITY` | 64 | Nhưng nếu `table.length < 64` thì **resize thay vì tree hóa** (bảng còn nhỏ, giãn bảng rẻ hơn). |
| `UNTREEIFY_THRESHOLD` | 6 | Cây co lại còn ≤ 6 Node (do `remove`/resize) → quay về linked list. |

Cây so sánh Node bằng `hashCode`, rồi bằng `Comparable` nếu key có cài, cuối cùng dùng `System.identityHashCode` làm "trọng tài".

### Load factor & resize

`capacity` mặc định 16, `loadFactor` mặc định 0.75. Vượt `capacity * 0.75` → **nhân đôi** capacity, cấp `table` mới, **phân bổ lại** mọi Node. Chi phí một lần resize là O(n) nhưng hiếm nên không đổi độ phức tạp trung bình (*amortized O(1)*).

> Mẹo Java 8: khi resize gấp đôi, mỗi Node hoặc **ở nguyên chỉ số cũ** hoặc **dời đúng `oldCapacity`** — quyết định chỉ bằng **một bit** của hash (`hash & oldCap`), không cần tính lại `%`. Vì vậy thứ tự duyệt `HashMap` *đổi* sau resize.

### Tính capacity ban đầu cho đúng

Cần chứa `n` mapping **không resize lần nào** → truyền:

```java
new HashMap<>((int) (n / 0.75f) + 1);     // HashMap sẽ tự làm tròn LÊN lũy thừa của 2 gần nhất
// Java 19+:
Map<K,V> m = HashMap.newHashMap(n);        // làm sẵn phép tính trên
```

### `null` và thread-safety

- `HashMap`: cho **1 key `null`** (luôn ở bucket 0) và **nhiều value `null`**.
- `Hashtable`, `ConcurrentHashMap`, `TreeMap` (key): **không** cho `null`.
- `HashMap` **không an toàn đa luồng** — hai thread `put` song song có thể mất update, hoặc (JDK 7) tạo vòng lặp vô hạn trong bucket khi resize. Đa luồng → `ConcurrentHashMap` (mục 9).

---

## 6. Iterator fail-fast & ConcurrentModificationException

### Chuyện gì xảy ra

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));
for (String s : list) {
    if (s.equals("b")) list.remove(s);      // ✗ ConcurrentModificationException
}
```

`ArrayList`, `HashMap`, `HashSet`... giữ một bộ đếm `modCount` tăng lên mỗi lần **thay đổi cấu trúc** (thêm/xóa phần tử, không tính `set()`). Iterator ghi nhớ `expectedModCount` lúc tạo; mỗi `next()` kiểm tra `modCount == expectedModCount`, lệch thì ném `ConcurrentModificationException` (CME).

> **fail-fast là nỗ lực tốt nhất, không phải bảo đảm.** Đừng bắt CME để "xử lý" — nó là dấu hiệu **bug logic**. Ngược lại, việc *không* ném CME cũng không chứng minh code đúng (ví dụ sửa phần tử áp chót của `ArrayList` rồi thoát vòng lặp có thể lọt).

### Bốn cách sửa đúng

```java
// 1. Iterator.remove() — cách kinh điển khi cần vừa duyệt vừa xóa
Iterator<String> it = list.iterator();
while (it.hasNext()) { if (cond(it.next())) it.remove(); }

// 2. removeIf(predicate) — ngắn nhất, ưu tiên dùng (Java 8+)
list.removeIf(s -> s.equals("b"));
map.entrySet().removeIf(e -> e.getValue() < 60);

// 3. Duyệt trên bản sao, sửa bản gốc
for (String s : List.copyOf(list)) if (cond(s)) list.remove(s);

// 4. ListIterator — khi cần add/set trong lúc duyệt List
ListIterator<String> lit = list.listIterator();
while (lit.hasNext()) { if (cond(lit.next())) lit.set("X"); }
```

### Iterator "fail-safe" (không ném CME)

`CopyOnWriteArrayList`, `ConcurrentHashMap` (mục 9) duyệt trên **ảnh chụp** hoặc chấp nhận thay đổi song song — không bao giờ ném CME, đổi lại iterator có thể **không thấy** phần tử vừa thêm. Dùng cho ngữ cảnh đa luồng, không phải để "né" CME đơn luồng.

---

## 7. Bảng độ phức tạp Big-O tổng hợp

| Cấu trúc | `get` / truy cập | `contains` / tìm | `add` / `put` | `remove` | Thứ tự | Nền tảng |
|---|---|---|---|---|---|---|
| `ArrayList` | **O(1)** theo index | O(n) | O(1)* cuối · O(n) giữa | O(n) | Vị trí chèn | Mảng động |
| `LinkedList` | O(n) | O(n) | **O(1)** hai đầu · O(n) giữa | O(1) sau khi có Node | Vị trí chèn | Linked list đôi |
| `ArrayDeque` | O(1) hai đầu | O(n) | **O(1)*** hai đầu | O(1) hai đầu · O(n) giữa | FIFO/LIFO | Mảng vòng |
| `HashSet` / `HashMap` | — | **O(1)** TB · O(log n) xấu | O(1) TB | O(1) TB | Không xác định | Bucket + hash |
| `LinkedHashSet` / `LinkedHashMap` | — | O(1) TB | O(1) TB | O(1) TB | Chèn (hoặc access) | Hash + linked list |
| `TreeSet` / `TreeMap` | — | O(log n) | O(log n) | O(log n) | Sắp xếp | Red-Black Tree |
| `EnumSet` / `EnumMap` | O(1) | O(1) | O(1) | O(1) | Thứ tự enum | bit-vector / mảng |
| `PriorityQueue` | `peek` O(1) | O(n) | `offer` O(log n) | `poll` O(log n) · `remove(x)` O(n) | Chỉ đỉnh heap | Binary heap (mảng) |

`*` = amortized (thỉnh thoảng O(n) khi resize).

---

## 8. Queue & Deque — hai họ method, ArrayDeque, PriorityQueue

### Hai họ method song song

| Mục đích | Ném exception khi lỗi | Trả giá trị đặc biệt khi lỗi |
|---|---|---|
| Thêm | `add(e)` → `IllegalStateException` nếu đầy | `offer(e)` → `false` |
| Lấy & xóa đầu | `remove()` → `NoSuchElementException` nếu rỗng | `poll()` → `null` |
| Xem đầu (không xóa) | `element()` → `NoSuchElementException` | `peek()` → `null` |

Với hàng đợi có giới hạn dung lượng (`BlockingQueue`) hoặc code cần xử lý "rỗng/đầy" như luồng bình thường → dùng họ `offer`/`poll`/`peek`.

### `ArrayDeque` — Queue **và** Stack, thay cho `LinkedList` lẫn `Stack`

```java
Deque<Integer> q = new ArrayDeque<>();
q.offer(1); q.offer(2); q.poll();       // 1  — dùng như FIFO Queue

Deque<Integer> stack = new ArrayDeque<>();
stack.push(1); stack.push(2); stack.pop();  // 2  — dùng như LIFO Stack
```

- Mảng vòng (circular buffer), tự giãn — **không giới hạn dung lượng**, **không cho `null`** (vì `null` là tín hiệu "rỗng" của `poll`).
- Nhanh hơn `LinkedList` khi làm Queue (cache locality) và nhanh hơn `Stack` khi làm Stack (`Stack` kế thừa `Vector` — `synchronized` thừa).

### `PriorityQueue` — lấy ra theo ưu tiên, KHÔNG phải FIFO

```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();          // đỉnh = nhỏ nhất
minHeap.offer(5); minHeap.offer(1); minHeap.offer(3);
minHeap.poll();   // 1

PriorityQueue<Task> maxByPriority =
    new PriorityQueue<>(Comparator.comparingInt(Task::priority).reversed());
```

- Binary heap trên mảng: `offer`/`poll` O(log n), `peek` O(1), nhưng `contains(x)` và `remove(x)` là **O(n)**.
- ⚠️ **Duyệt (`for`, `iterator`, `toString`) KHÔNG theo thứ tự sắp xếp** — chỉ `poll()` liên tiếp mới ra thứ tự đúng.
- Không cho `null`; comparator/`Comparable` phải nhất quán, nếu không thứ tự `poll` sai.

### Thuật toán sắp xếp phía sau (nhắc từ Module 02.4)

`Collections.sort` / `List.sort` → **TimSort**, ổn định, ~O(n) với dữ liệu gần sắp sẵn. `Arrays.sort(primitive[])` → dual-pivot quicksort, không ổn định.

---

## 9. An toàn luồng & bất biến

### Ba mức "an toàn luồng"

| Nhóm | Ví dụ | Đặc điểm |
|---|---|---|
| **Không đồng bộ** (mặc định) | `ArrayList`, `HashMap`, `HashSet`, `ArrayDeque` | Nhanh nhất. Chỉ dùng trong 1 thread, hoặc có khóa ngoài. |
| **Bọc đồng bộ** | `Collections.synchronizedList/Map/Set(...)` | Mỗi method `synchronized`. **Vẫn phải tự `synchronized (coll)` khi *duyệt*** — nếu không, CME/đọc bẩn. |
| **Đồng thời (concurrent)** | `ConcurrentHashMap`, `CopyOnWriteArrayList`, `ConcurrentLinkedQueue`, `BlockingQueue` | Thiết kế cho nhiều thread: khóa mịn hoặc lock-free, iterator fail-safe. Chi tiết ở **Module 09**. |

> `Vector`/`Hashtable` cũng "đồng bộ" nhưng là legacy — khóa toàn cục thô, đừng dùng cho code mới.

### Ba kiểu "chỉ đọc" — khác nhau về bản chất

```java
List<Integer> src = new ArrayList<>(List.of(1, 2, 3));

// (a) VIEW không sửa được — nhưng list gốc đổi thì view "thấy" theo
List<Integer> ro = Collections.unmodifiableList(src);
ro.add(4);        // ✗ UnsupportedOperationException
src.add(4);       // ro giờ là [1,2,3,4]  ← rò rỉ thay đổi!

// (b) BẢN SAO bất biến — tách hẳn khỏi nguồn
List<Integer> immut = List.copyOf(src);
src.add(5);       // immut không đổi

// (c) Factory bất biến — bất biến + null-hostile ngay từ đầu
List<Integer> lit = List.of(1, 2, 3);
```

Trả collection ra ngoài class (getter) → dùng `List.copyOf(...)` hoặc `Collections.unmodifiableList(new ArrayList<>(...))` để giữ **đóng gói** (Module 02.1 — defensive copy).

---

## 10. Bẫy thường gặp

### 10.1. `remove(int)` vs `remove(Object)` trên `List<Integer>`

```java
List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
list.remove(1);              // xóa PHẦN TỬ Ở INDEX 1 → xóa 20  (khớp remove(int))
list.remove(Integer.valueOf(10));   // xóa GIÁ TRỊ 10           (ép sang remove(Object))
```

Với `List<Integer>`, `list.remove(1)` **không** xóa số 1. Muốn xóa theo giá trị phải bọc `Integer.valueOf(...)`.

### 10.2. `Arrays.asList` với mảng primitive

```java
int[] a = {1, 2, 3};
List<int[]> wrong = Arrays.asList(a);     // 1 phần tử: chính mảng a  (int[] không phải Object[])
List<Integer> right = Arrays.stream(a).boxed().toList();
```

### 10.3. `Arrays.asList` kích thước cố định

```java
List<String> l = Arrays.asList("a", "b");
l.add("c");        // ✗ UnsupportedOperationException
l.set(0, "z");     // OK (ghi xuyên xuống mảng)
List<String> mut = new ArrayList<>(Arrays.asList("a", "b"));   // cần bọc nếu muốn sửa cấu trúc
```

### 10.4. `capacity` ≠ `size`

`new ArrayList<>(100)` có `size() == 0` — 100 chỉ là sức chứa nội bộ. Không thể `list.get(50)` cho tới khi thực sự `add` đủ phần tử.

### 10.5. Autoboxing ngầm trong collection

`List<Long> ids` rồi `ids.contains(4)` → `4` là `int`, autobox thành `Integer`, không bao giờ `equals` `Long` → luôn `false`. Phải `ids.contains(4L)`.

### 10.6. Key mutable trong `HashMap` / phần tử mutable trong `HashSet`

Đổi field tham gia `hashCode()` sau khi đã bỏ vào map/set → phần tử "mất tích" (Module 02.4, mục 8.3). Key nên **bất biến** (`String`, `Integer`, `record`, enum).

### 10.7. `Comparator` không nhất quán với `equals` trong `TreeSet`/`TreeMap`

Comparator chỉ so một field → hai object khác nhau nhưng "ngang hàng" → `TreeSet` chỉ giữ **một**. Muốn giữ cả hai, thêm `thenComparing` cho tới khi phân biệt được (Module 02.4, mục 11).

### 10.8. Stream `.toList()` (Java 16+) trả list **bất biến**

`stream().toList()` ⇒ không `add`/`sort` được. Cần sửa → `stream().collect(Collectors.toCollection(ArrayList::new))`.

---

## 11. Cây quyết định: chọn cấu trúc dữ liệu nào?

```
Lưu cặp key → value?
├── CÓ → Map
│        ├── Key là enum?                         → EnumMap
│        ├── Cần sắp xếp / truy vấn khoảng theo key? → TreeMap
│        ├── Cần giữ thứ tự thêm vào (hoặc LRU)?  → LinkedHashMap
│        ├── Nhiều thread ghi song song?          → ConcurrentHashMap        (Module 09)
│        └── Còn lại — nhanh nhất                  → HashMap  ← mặc định
│
└── KHÔNG → Cần loại trùng lặp?
     ├── CÓ → Set
     │        ├── Phần tử là enum?                → EnumSet
     │        ├── Cần sắp xếp / "phần tử gần nhất"? → TreeSet
     │        ├── Cần thứ tự thêm vào ổn định?    → LinkedHashSet
     │        └── Còn lại                          → HashSet  ← mặc định
     │
     └── KHÔNG → Cần xử lý ở ĐẦU/CUỐI (hàng đợi, ngăn xếp, BFS/DFS)?
          ├── CÓ → lấy ra theo ưu tiên?           → PriorityQueue
          │        còn lại (FIFO/LIFO)             → ArrayDeque
          └── KHÔNG → List có thứ tự, cho trùng
                   ├── Chủ yếu đọc/duyệt/`get(i)`  → ArrayList  ← mặc định (~95%)
                   └── Chèn/xóa liên tục hai đầu, hiếm `get(i)` → ArrayDeque (hoặc LinkedList)
```

---

## 12. Tổng kết — Bảng ghi nhớ nhanh

| Khái niệm | Điểm mấu chốt |
|---|---|
| `Iterable` → `Collection` → List/Set/Queue | `Map` **độc lập**, không phải `Collection`; nhưng `keySet`/`values`/`entrySet` thì là. |
| `ArrayList` | Mảng động, `get(i)` O(1), chèn/xóa giữa O(n), grow ×1.5. Mặc định cho List. |
| `LinkedList` | Linked list đôi + là `Deque`. `get(i)` O(n) — **không** lặp theo index. |
| `HashSet`/`HashMap` | O(1) TB nhờ bucket + `hashCode()`. Không thứ tự, đổi sau resize. |
| `LinkedHashSet`/`LinkedHashMap` | Như Hash- + giữ thứ tự thêm vào; access-order → LRU. |
| `TreeSet`/`TreeMap` | O(log n), luôn sắp xếp, Red-Black Tree, `NavigableSet/Map` (`ceiling`/`floor`/`headMap`...). "Trùng nhau" theo `compareTo`, **bỏ qua `equals`**. |
| `EnumSet`/`EnumMap` | Key/phần tử là enum → bit-vector/mảng, nhanh nhất. Luôn ưu tiên. |
| HashMap nội bộ | `hash = h ^ (h>>>16)` → `(n-1) & hash`. Tree hóa bucket ở 8 Node **và** table ≥ 64; untreeify ở 6. |
| Load factor 0.75 & resize ×2 | Amortized O(1). Biết trước `n` → `new HashMap<>((int)(n/0.75)+1)` hoặc `HashMap.newHashMap(n)`. |
| `null` | `HashMap`: 1 key null OK. `Hashtable`/`ConcurrentHashMap`/`TreeMap`-key/`ArrayDeque`: **không**. |
| `merge`/`computeIfAbsent`/`putIfAbsent` | Word-count & multimap không cần `if containsKey`. Hàm remap trả `null` → xóa entry. |
| fail-fast / CME | `modCount` lệch → `ConcurrentModificationException`. Sửa: `Iterator.remove()`, `removeIf`, `ListIterator`, hoặc duyệt bản sao. |
| `ArrayDeque` | Thay cho `Stack` (LIFO) và `LinkedList`-as-Queue (FIFO). Không `null`, không giới hạn. |
| `PriorityQueue` | `offer`/`poll` O(log n); **duyệt KHÔNG theo thứ tự** — chỉ `poll` liên tiếp. |
| An toàn luồng | Mặc định: không. `Collections.synchronizedXxx` (tự khóa khi duyệt). Đa luồng thật → concurrent collections (Module 09). |
| Chỉ đọc | `unmodifiableList` = view (nguồn đổi thì rò rỉ); `List.copyOf` / `List.of` = bất biến thật. |
| Bẫy | `list.remove(int)` vs `remove(Object)`; `Arrays.asList` cố định kích thước; `capacity ≠ size`; autobox trong `contains`; `stream().toList()` bất biến. |

---

## 13. Bài tập luyện tập

### Phần A — Trắc nghiệm nhận định (giải thích lý do)

**Câu 1.** Đoạn sau chạy chậm bất thường với 100.000 phần tử — vì sao, sửa thế nào?
```java
List<Integer> nums = new ArrayList<>();
for (int i = 0; i < 100_000; i++) nums.add(0, i);   // luôn chèn ĐẦU
```

**Câu 2.** `HashSet<Student>` hoạt động sai nếu `Student` thiếu điều gì? (liên hệ Module 02.4). Nếu `Student` có `hashCode()` đúng nhưng là field **mutable** và bị đổi sau khi `add`, chuyện gì xảy ra?

**Câu 3.** Đoạn này in thứ tự nào? Vì sao **không** được giả định `HashMap` giữ thứ tự `put`? Đổi sang loại `Map` nào để giữ thứ tự chèn? Loại nào để in ra `apple, banana, cherry`?
```java
Map<String,Integer> m = new HashMap<>();
m.put("banana",1); m.put("apple",2); m.put("cherry",3);
m.keySet().forEach(System.out::println);
```

**Câu 4.** So sánh `contains()` giữa `ArrayList<String>` và `HashSet<String>` với 1 triệu phần tử — giải thích bằng cơ chế bên trong. `TreeSet` thì sao?

**Câu 5.** `new HashMap<>()` (không truyền capacity) rồi thêm 10.000 phần tử — điều gì xảy ra về hiệu năng? Công thức capacity ban đầu nên truyền là gì?

**Câu 6.** Với `List<Integer> list = new ArrayList<>(List.of(10, 20, 30))`, `list.remove(1)` xóa phần tử nào? Làm sao xóa **giá trị** `10`?

**Câu 7.** Đoạn này ném exception gì, ở đâu, tên gọi cơ chế? Viết **hai** cách sửa.
```java
List<String> xs = new ArrayList<>(List.of("a","b","c"));
for (String s : xs) if (s.equals("b")) xs.remove(s);
```

**Câu 8.** `PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(5,1,3));` — `System.out.println(pq)` in ra gì? `pq.poll(); pq.poll();` ra gì? Vì sao hai kết quả "mâu thuẫn"?

**Câu 9.** Phân biệt `Collections.unmodifiableList(src)` và `List.copyOf(src)` khi sau đó `src.add(x)`.

---

### Phần B — Bài tập viết code

**Bài 1 — Đo hiệu năng ArrayList vs LinkedList.**
Đo `System.nanoTime()` với 50.000 phần tử cho: (a) chèn liên tục vào **đầu**; (b) `get(index)` ngẫu nhiên 10.000 lần; (c) for-each duyệt toàn bộ. In bảng, giải thích mỗi cấu trúc thắng/thua ở đâu bằng độ phức tạp và cache locality.

**Bài 2 — Word Frequency Counter.**
Cho một `String` nhiều câu: tách từ (`split("\\W+")`), chuẩn hóa `toLowerCase(Locale.ROOT)`. Dùng `HashSet` lấy tập từ duy nhất; dùng `HashMap` + `merge(w, 1, Integer::sum)` đếm tần suất; in 5 từ nhiều nhất (đưa `entrySet()` vào `List`, `sort` bằng `Map.Entry.comparingByValue().reversed()`). So sánh kết quả nếu đổi `HashMap` → `TreeMap` (in theo alphabet) và → `LinkedHashMap` (theo thứ tự gặp lần đầu).

**Bài 3 — Group By bằng computeIfAbsent.**
Cho `List<Student>` với `name`, `major`. Viết `Map<String, List<Student>> groupByMajor(List<Student>)` dùng `computeIfAbsent(major, k -> new ArrayList<>()).add(s)` — không `if/else` kiểm tra key. Viết thêm bản đếm `Map<String, Long> countByMajor(...)` bằng `merge`.

**Bài 4 — LRU Cache bằng LinkedHashMap.**
Hoàn thiện và kiểm chứng:
```java
public class SimpleLRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    public SimpleLRUCache(int capacity) {
        super(16, 0.75f, true);                 // access-order
        this.capacity = capacity;
    }
    @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```
`main`: `capacity = 3`, `put` A,B,C; `get(A)`; `put(D)` → chứng minh **B** (lâu nhất không được truy cập) bị loại, không phải A.

**Bài 5 — Quan sát thứ tự 3 loại Set.**
Thêm cùng tập tên 5 thành phố vào `HashSet`, `LinkedHashSet`, `TreeSet`; in kết quả duyệt cả ba; giải thích bằng lời từng thứ tự. Thêm một tên `null` vào mỗi loại và ghi lại loại nào ném exception.

**Bài 6 — Hash Collision cố ý.**
`class BadKey` với `hashCode()` luôn `return 1;` nhưng `equals()` so đúng nội dung. Thêm 50.000 `BadKey` khác nhau vào `HashMap`, đo thời gian so với key `String` cùng số lượng. Giải thích bằng cơ chế bucket + tree hóa (mục 5) — vì sao vẫn không "cứu" nổi về O(1).

**Bài 7 — NavigableMap cho bài toán tra khoảng.**
Cho bảng điểm chữ theo mốc: `0→F, 50→D, 65→C, 75→B, 85→A`. Dùng `TreeMap<Integer,String>` + `floorEntry(score)` viết `String grade(int score)`. Test với `49, 50, 64, 90`. Giải thích vì sao `floorEntry` gọn hơn hẳn chuỗi `if/else`.

---

### Phần C — Nâng cao

**Câu 1.** Giải thích chính xác vì sao `HashMap` yêu cầu capacity là **lũy thừa của 2**, và bước `hash = h ^ (h >>> 16)` giải quyết vấn đề gì nếu bỏ đi. Cho ví dụ hai `hashCode` chỉ khác nhau ở bit cao.

**Câu 2.** `hashCode()` trả hằng số vẫn **đúng contract** nhưng biến `HashMap` 1 triệu key thành gần O(n) mỗi thao tác. Java 8+ tree hóa bucket — vì sao điều đó **vẫn không** đưa lại O(1)? Hai ngưỡng `TREEIFY_THRESHOLD` (8) và `MIN_TREEIFY_CAPACITY` (64) phối hợp thế nào?

**Câu 3.** Trình bày vì sao `for (int i = 0; i < linkedList.size(); i++) linkedList.get(i)` là O(n²) còn for-each là O(n). `LinkedList` có tối ưu gì cho `get(i)` khi `i` gần cuối?

**Câu 4.** `ConcurrentModificationException` là "best-effort". Cho một ví dụ code **sửa cấu trúc list trong lúc duyệt mà KHÔNG ném CME** (gợi ý: xóa phần tử áp chót của `ArrayList`). Giải thích bằng chỉ số `cursor`/`size` trong `ArrayList.Itr`.

**Câu 5.** So sánh chi phí bộ nhớ & thời gian giữa `EnumMap<Day,X>` và `HashMap<Day,X>` cho key là enum 7 hằng. `EnumMap` cài đặt bằng gì? Khi nào `HashMap` vẫn hợp lý hơn?

**Câu 6.** `Collections.synchronizedMap(map)` khiến từng method `synchronized`, nhưng đoạn `for (K k : m.keySet()) ...` vẫn có thể ném CME khi thread khác `put`. Vì sao? Viết cách duyệt đúng. So sánh với `ConcurrentHashMap` ở khía cạnh này.

**Câu 7.** `subList(from, to)` trả về *view*. Cho một đoạn code dùng `list.subList(a, b).clear()` để xóa một dải, rồi giải thích vì sao thao tác **structural** trực tiếp lên `list` gốc sau đó khiến `view` hỏng (ném CME khi đụng tới).

---

### Phần D — Gợi ý đáp án (tự chấm)

<details>
<summary>Bấm để xem gợi ý đáp án Phần A</summary>

1. `add(0, i)` chèn đầu `ArrayList` → `System.arraycopy` dịch **toàn bộ** phần tử sang phải mỗi lần → O(n) mỗi lần, tổng O(n²). Sửa: `ArrayDeque`/`LinkedList` + `addFirst()` (O(1)); hoặc nếu cuối cùng cần thứ tự đó thì `add()` vào cuối rồi `Collections.reverse()` một lần.
2. Thiếu override `equals()`/`hashCode()` đúng chuẩn → dùng identity của `Object` → hai `Student` cùng nội dung bị coi là hai phần tử khác nhau → trùng lặp không bị loại. Nếu `hashCode()` dựa field **mutable** và field bị đổi sau `add`: phần tử vẫn nằm ở bucket cũ nhưng `hashCode()` mới trỏ bucket khác → `contains`/`remove` trả sai, phần tử thành "bóng ma".
3. Không xác định — có thể `apple, banana, cherry` hoặc bất kỳ thứ tự nào, phụ thuộc hash và bố cục bucket, và **đổi sau resize**. Giữ thứ tự chèn → `LinkedHashMap`. In alphabet `apple, banana, cherry` → `TreeMap`.
4. `ArrayList.contains()` O(n) — duyệt tuần tự `equals()` từng phần tử. `HashSet.contains()` O(1) TB — `hashCode()` → bucket → `equals()` vài phần tử. `TreeSet.contains()` O(log n) — đi xuống cây so sánh ~log₂(10⁶) ≈ 20 lần. Với 1 triệu phần tử, HashSet nhanh hơn ArrayList hàng trăm nghìn lần.
5. Resize nhiều lần: 16 → 32 → ... vượt mỗi mốc 75% thì nhân đôi, cấp `table` mới, phân bổ lại toàn bộ — tốn thời gian & rác GC. Truyền `new HashMap<>((int)(10_000 / 0.75f) + 1)` (≈ 13_334, HashMap làm tròn lên 16_384), hoặc `HashMap.newHashMap(10_000)` (Java 19+).
6. `list.remove(1)` khớp `remove(int index)` → xóa phần tử ở **index 1** = `20`. Xóa giá trị `10`: `list.remove(Integer.valueOf(10))`.
7. `ConcurrentModificationException` tại `iterator.next()` của vòng for-each, ở lần lặp sau khi `remove`. Cơ chế: **fail-fast** qua `modCount`/`expectedModCount`. Sửa: `xs.removeIf(s -> s.equals("b"))`; hoặc `Iterator<String> it = xs.iterator(); while (it.hasNext()) if (it.next().equals("b")) it.remove();`.
8. `println(pq)` in `[1, 5, 3]` (bố cục nội bộ của heap, **không** sắp xếp). `poll()` hai lần → `1` rồi `3`. Không mâu thuẫn: `PriorityQueue` chỉ đảm bảo **đỉnh heap** (`peek`/`poll`) là nhỏ nhất; iterator/`toString` duyệt mảng nền theo thứ tự lưu trữ.
9. `unmodifiableList(src)` là **view**: không sửa qua `ro` được, nhưng `src.add(x)` khiến `ro` "thấy" `x` (rò rỉ). `List.copyOf(src)` là **bản sao bất biến**: `src.add(x)` không ảnh hưởng gì.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần B</summary>

- **Bài 1:** Dự kiến: `LinkedList.addFirst` nhanh hơn `ArrayList.add(0,..)` hàng trăm–nghìn lần (O(1) vs O(n)); `ArrayList.get(i)` nhanh hơn `LinkedList.get(i)` rõ rệt (O(1) vs O(n)); for-each xấp xỉ nhau về Big-O nhưng `ArrayList` vẫn nhỉnh nhờ cache locality (Node của `LinkedList` rải rác trên Heap).
- **Bài 2:** Top-5: `wordCount.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(5).forEach(...)`, hoặc `List<Map.Entry<...>> es = new ArrayList<>(wordCount.entrySet()); es.sort(Map.Entry.comparingByValue().reversed());`. `TreeMap` → duyệt ra alphabet; `LinkedHashMap` → thứ tự gặp lần đầu (vì `merge` không đổi thứ tự chèn của key đã có).
- **Bài 3:** `students.stream()` không bắt buộc; bản vòng lặp: `for (Student s : list) result.computeIfAbsent(s.major(), k -> new ArrayList<>()).add(s);`. Đếm: `for (...) count.merge(s.major(), 1L, Long::sum);`.
- **Bài 4:** `super(16, 0.75f, true)` bật access-order → mỗi `get`/`put` đẩy entry xuống cuối danh sách liên kết nội bộ; entry ở **đầu** là "eldest" (lâu nhất không đụng tới). Sau `put` A,B,C rồi `get(A)`: thứ tự nội bộ B, C, A → `put(D)` khiến size = 4 > 3 → `removeEldestEntry` trả `true` → xóa **B**.
- **Bài 5:** `HashSet` — thứ tự "ngẫu nhiên" theo hash; `LinkedHashSet` — đúng thứ tự add; `TreeSet` — alphabet (theo `String.compareTo`). `TreeSet` ném `NullPointerException` khi add `null` (so sánh với `null`); `HashSet`/`LinkedHashSet` nhận 1 `null`.
- **Bài 6:** `HashMap<BadKey,..>` chậm hơn hàng nghìn lần: mọi key vào **một** bucket. Bucket đó tree hóa thành cây đỏ-đen ⇒ O(log n) thay vì O(n) — **đỡ hơn nhưng vẫn không phải O(1)**, và cây so sánh phải dựa `hashCode` (bằng nhau hết) rồi `Comparable`/identity hash nên hằng số lớn. Bài học: tree hóa là "lưới an toàn", không thay được `hashCode()` phân tán tốt.
- **Bài 7:** `TreeMap<Integer,String> g = new TreeMap<>(Map.of(0,"F",50,"D",65,"C",75,"B",85,"A")); String grade(int s){ return g.floorEntry(s).getValue(); }`. `49→F, 50→D, 64→D, 90→A`. `floorEntry(s)` trả entry có key lớn nhất ≤ `s` — đúng ngữ nghĩa "mốc" — thay cho 5 nhánh `if` và tự nhiên đúng thứ tự.

</details>

<details>
<summary>Bấm để xem gợi ý đáp án Phần C</summary>

1. Lũy thừa của 2 ⇒ `hash % length` viết được thành `hash & (length - 1)` (một phép AND, nhanh hơn chia). Nhưng phép AND đó **chỉ giữ các bit thấp** của hash → nếu nhiều `hashCode` giống nhau ở bit thấp mà khác bit cao, chúng dồn hết vào một bucket. `h ^ (h >>> 16)` trộn 16 bit cao xuống, để bit cao cũng tham gia chọn bucket. Ví dụ `0x0000ABCD` và `0x1234ABCD` cùng `& 15` ra `0xD` → không spreading thì cùng bucket; sau `^ (h>>>16)` chúng khác nhau.
2. Vì ngay cả cây đỏ-đen vẫn là **O(log n)** cho mỗi `get`/`put` khi n key nằm chung một bucket — chậm hơn O(1) và hằng số lớn (so `hashCode` → `Comparable` → identity hash). `TREEIFY_THRESHOLD = 8`: chỉ tree hóa bucket khi nó đủ dài. `MIN_TREEIFY_CAPACITY = 64`: nếu `table` còn nhỏ (< 64) thì **resize** trước — vì bucket dài lúc bảng nhỏ thường do bảng chật, không phải do `hashCode` tồi; giãn bảng phân tán lại rẻ hơn và giữ được O(1).
3. `get(i)` trên `LinkedList` là O(i) (đi bộ từ đầu/cuối). Vòng `for` theo index gọi `get(0)+get(1)+...+get(n-1)` = O(0+1+...+(n-1)) = O(n²). for-each dùng `iterator()` giữ con trỏ `Node` hiện tại, `next()` là O(1) → tổng O(n). Tối ưu duy nhất của `LinkedList.get(i)`: nếu `i > size/2` thì duyệt **ngược từ cuối** → giảm nửa số bước, vẫn O(n).
4. `ArrayList` với `[a, b, c, d]`, xóa phần tử ở index 2 (`c`) khi `cursor == 3`: `remove` làm `size` giảm còn 3, `modCount++`. Vòng for-each gọi `hasNext()` → `cursor (3) != size (3)` là `false` → **thoát vòng lặp trước khi gọi `next()`** → không có lần `next()` nào để phát hiện `modCount` lệch → không CME. Đây là lý do "best-effort": xóa phần tử áp chót lọt lưới.
5. `EnumMap` cài bằng **một mảng `Object[]`** đánh chỉ số theo `ordinal()` của enum — `get`/`put` là truy cập mảng trực tiếp, không băm, không autobox, không Node → nhanh hơn và tốn ít bộ nhớ hơn `HashMap` (không bucket, không `Map.Entry`). Thứ tự duyệt = thứ tự khai báo hằng. `HashMap` chỉ hợp lý hơn khi key trộn nhiều kiểu, hoặc cần `null` key, hoặc code generic không biết trước kiểu enum.
6. `synchronizedMap` chỉ khóa **từng lời gọi method**; vòng lặp `for` gồm **nhiều** lời gọi `next()` — giữa hai lần đó, thread khác chen vào `put` làm `modCount` đổi → `next()` kế tiếp ném CME. Duyệt đúng: `synchronized (m) { for (K k : m.keySet()) ... }` — khóa toàn bộ vòng lặp. `ConcurrentHashMap` không cần: iterator của nó **fail-safe** (phản ánh trạng thái tại/sau thời điểm tạo, không ném CME), đổi lại có thể bỏ sót phần tử vừa thêm.
7. `subList` trả về `SubList` giữ tham chiếu tới list gốc và ghi nhớ `modCount` của gốc lúc tạo. `subList(a,b).clear()` gọi `removeRange` **qua view** nên đồng bộ được `modCount`. Nhưng nếu sau đó `list.add(x)`/`list.remove(i)` **trực tiếp trên gốc**, `modCount` gốc tăng mà view không biết → lần tiếp theo đụng tới view (`view.get`, `view.size`, duyệt) so `modCount` lệch → ném `ConcurrentModificationException`. Quy tắc: khi đã tạo `subList`, hãy thao tác **qua view** cho tới khi dùng xong, đừng sửa cấu trúc list gốc song song.

</details>

---

*File tiếp theo trong lộ trình: **Module 03.2 — Generics** (generic class/method, bounded type, wildcard `? extends`/`? super`, type erasure).*
