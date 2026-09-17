package phanB.bai8;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class Main {

    static class Node {
        String name;
        boolean isFile;
        List<Node> children;

        Node(String name, boolean isFile, List<Node> children) {
            this.name = name;
            this.isFile = isFile;
            this.children = children;
        }
    }

    // Bản đệ quy
    private static void printTree(Node node, int depth) {
        System.out.println("  ".repeat(depth) + (node.isFile ? "" : "[D] ") + node.name);
        if (node.children != null) {
            for (Node child : node.children) {
                printTree(child, depth + 1);
            }
        }
    }

    // Bản khử đệ quy bằng ArrayDeque làm stack tường minh
    private static void printTreeIterative(Node root) {
        Deque<Node> stack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        stack.push(root);
        depthStack.push(0);

        while (!stack.isEmpty()) {
            Node node = stack.pop();
            int depth = depthStack.pop();
            System.out.println("  ".repeat(depth) + (node.isFile ? "" : "[D] ") + node.name);

            if (node.children != null) {
                // đẩy ngược để pop ra đúng thứ tự như bản đệ quy
                for (int i = node.children.size() - 1; i >= 0; i--) {
                    stack.push(node.children.get(i));
                    depthStack.push(depth + 1);
                }
            }
        }
    }

    public static void main(String[] args) {
        Node root = new Node("project", false, List.of(
                new Node("src", false, List.of(
                        new Node("Main.java", true, null),
                        new Node("Util.java", true, null)
                )),
                new Node("README.md", true, null),
                new Node("test", false, List.of(
                        new Node("MainTest.java", true, null)
                ))
        ));

        System.out.println("--- Bản đệ quy ---");
        printTree(root, 0);

        System.out.println("--- Bản khử đệ quy (ArrayDeque) ---");
        printTreeIterative(root);
    }
}
