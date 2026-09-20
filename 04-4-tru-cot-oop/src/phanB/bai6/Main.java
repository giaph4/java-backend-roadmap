package phanB.bai6;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// ❌ Thiết kế SAI - kế thừa để "gắn thêm" tính năng đếm log
// Lưu ý: dùng HashSet (không phải ArrayList) để tái hiện đúng bug -
// ArrayList tự override addAll() bằng System.arraycopy (KHÔNG gọi add()) nên không có bug này,
// còn HashSet KHÔNG override addAll() -> nó dùng bản mặc định của AbstractCollection.addAll(),
// bản này lặp qua từng phần tử và gọi add() -> đây chính là nguồn gốc bug "đếm gấp đôi".
class BuggyLoggingSet<E> extends HashSet<E> {
    private int addCount = 0;

    @Override
    public boolean add(E e) {
        addCount++;
        return super.add(e);
    }

    @Override
    public boolean addAll(java.util.Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c); // BUG: AbstractCollection.addAll() gọi lại add() cho từng phần tử
                                 // -> addCount bị cộng THÊM một lần nữa trong add() ở trên -> đếm sai gấp đôi
    }

    public int getAddCount() {
        return addCount;
    }
}

// ✅ Thiết kế ĐÚNG - Composition: "có một" List bên trong, không kế thừa để tránh phụ thuộc
// vào chi tiết cài đặt nội bộ (fragile base class problem) của lớp cha.
class LoggingList<E> {
    private final List<E> delegate = new ArrayList<>();
    private int addCount = 0;

    public boolean add(E e) {
        addCount++;
        return delegate.add(e);
    }

    public boolean addAll(java.util.Collection<? extends E> c) {
        addCount += c.size(); // cộng đúng 1 lần cho mỗi phần tử, không đi qua add() nữa
        return delegate.addAll(c);
    }

    public int getAddCount() {
        return addCount;
    }

    public List<E> asList() {
        return new ArrayList<>(delegate); // trả bản sao - giữ đóng gói
    }
}

public class Main {

    public static void main(String[] args) {
        System.out.println("--- BuggyLoggingSet (kế thừa HashSet) ---");
        BuggyLoggingSet<String> buggy = new BuggyLoggingSet<>();
        buggy.add("a");                              // addCount = 1 (đúng)
        buggy.addAll(List.of("b", "c", "d"));         // kỳ vọng +3 = 4
        System.out.println("Số phần tử thực tế: " + buggy.size());                         // 4 - đúng
        System.out.println("addCount báo cáo (SAI - đếm gấp đôi phần addAll): " + buggy.getAddCount()); // 7, kỳ vọng 4

        System.out.println();
        System.out.println("--- LoggingList (composition) ---");
        LoggingList<String> fixed = new LoggingList<>();
        fixed.add("a");
        fixed.addAll(List.of("b", "c", "d"));
        System.out.println("Số phần tử thực tế: " + fixed.asList().size());     // 4
        System.out.println("addCount báo cáo (ĐÚNG): " + fixed.getAddCount());  // 4

        System.out.println();
        System.out.println("Nguyên nhân bug: HashSet KHÔNG override addAll() - nó dùng bản kế thừa từ");
        System.out.println("AbstractCollection.addAll(Collection), bản này lặp qua từng phần tử rồi gọi add(e).");
        System.out.println("Vì add() đã bị BuggyLoggingSet override để +1 addCount, mỗi phần tử trong addAll()");
        System.out.println("bị cộng 2 lần: một lần trong addAll() override, một lần nữa khi add() được gọi lại bên trong.");
        System.out.println("Composition tránh được hoàn toàn vì LoggingList không kế thừa hành vi nội bộ của bất kỳ");
        System.out.println("Collection nào - addAll() tự viết tường minh, không phụ thuộc addAll() có gọi add() hay không.");
    }
}
