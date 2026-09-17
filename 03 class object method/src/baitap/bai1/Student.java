package baitap.bai1;

public class Student {

    String name;
    int age;
    double gpa;

    public Student() {
        this("Gia Pho", 22);
    }

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public Student(String name, int age, double gpa) {
        this(name, age);
        this.gpa = gpa;
    }

    void printInfo() {
        System.out.println("Name: " + name + ", Age: " + age + ", GPA: " + gpa);
    }
}
