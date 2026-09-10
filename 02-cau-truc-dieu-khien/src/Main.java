import org.w3c.dom.Node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Main {

//    enum Status {
//        NEW, IN_PROGRESS, DONE
//    }

    //    Java KHÔNG tối ưu đệ quy đuôi (Tail Call Optimization)
    private static Long factTail(int n, Long acc) {
        if (n <= 1) return acc;

        return factTail(n - 1, acc * n);
    }

    private static Long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("n âm");
        if (n == 0) return 1L;
        return n * factorial(n - 1);
    }

    // Chuyển đệ quy → vòng lặp bằng Deque làm stack tường minh
//    void printTreeIterative(Node root) {
//        Deque<Node> stack = new ArrayDeque<>();
//        stack.push(root);
//        while (!stack.isEmpty()) {
//            Node cur = stack.pop();
//            System.out.println(cur.name);
//            for (int i = cur.children.size() - 1; i >= 0; i--) {
//                stack.push(cur.children.get(i));   // đẩy ngược để giữ thứ tự duyệt
//            }
//        }
//    }

    void subsets(int[] nums, int idx, List<Integer> path, List<List<Integer>> out) {
        if (idx == nums.length) {
            out.add(new ArrayList<>(path));
            return;
        }  // base case
        subsets(nums, idx + 1, path, out);// nhánh: KHÔNG chọn nums[idx]
        path.add(nums[idx]);
        subsets(nums, idx + 1, path, out); // nhánh: CÓ chọn
        path.remove(path.size() - 1);  // undo — trả trạng thái về trước
    }

    public static void main(String[] args) {
//        record Point(int x, int y) {
//        }
//
//        Point shape = new Point(1, 2);
//
//        System.out.println(shape);
//        String r = switch (shape) {
//            case Point(int x, int y) when x == y -> "Trên đường chéo";
//            case Point(int x, int y) -> "(" + x + ", " + y + ")";
//            default -> "?";
//        };
//
//        System.out.println(r);

        System.out.println(factTail(5, 5L));
    }
}