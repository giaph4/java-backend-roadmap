package phanB.bai9;

public class Main {

    static final int READ = 1 << 0;
    static final int WRITE = 1 << 1;
    static final int EXECUTE = 1 << 2;

    static int grant(int perms, int flag) {
        return perms | flag;
    }

    static int revoke(int perms, int flag) {
        return perms & ~flag;
    }

    static boolean has(int perms, int flag) {
        return (perms & flag) != 0;
    }

    static String describe(int perms) {
        return (has(perms, READ) ? "r" : "-")
                + (has(perms, WRITE) ? "w" : "-")
                + (has(perms, EXECUTE) ? "x" : "-");
    }

    public static void main(String[] args) {
        int perms = 0;
        perms = grant(perms, READ);
        perms = grant(perms, WRITE);
        System.out.println("sau grant R,W : " + describe(perms) + "  has(EXECUTE)=" + has(perms, EXECUTE)); // rw-

        perms = grant(perms, EXECUTE);
        System.out.println("sau grant X   : " + describe(perms)); // rwx

        perms = revoke(perms, WRITE);
        System.out.println("sau revoke W  : " + describe(perms)); // r-x
    }
}
