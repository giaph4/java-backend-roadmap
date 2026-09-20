package baitap.bai1;

public class Main {

    public static void main(String[] args) {

//        Student st1 = new Student();
//
//        Student st2 = new Student();
//
//        Student st3 = st1;
//
//        System.out.println(st1 == st2); // false
//        System.out.println(st1 == st3); // true
//
//        st2 = null;
//        System.gc();

        Point p = new Point(3, 4);
        p.x();                         // 3
        System.out.println(  new Point(3, 4).equals(p));    // true — so sánh theo giá trị field
    }
}
