package phanB.bai4;
public class Student {

    private String name;
    private int age;
    private String email;

    public Student(String name, int age, String email) {
        // Constructor gọi LẠI setter -> dùng chung logic validate, không lặp code
        setName(name);
        setAge(age);
        setEmail(email);
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên không được để trống");
        }
        this.name = name;
    }

    public void setAge(int age) {
        if (age < 16 || age > 100) {
            throw new IllegalArgumentException("Tuổi phải từ 16 đến 100, nhận được: " + age);
        }
        this.age = age;
    }

    public void setEmail(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        boolean hasDotAfterAt = atIndex >= 0 && email.indexOf('.', atIndex) > atIndex;
        if (atIndex < 0 || !hasDotAfterAt) {
            throw new IllegalArgumentException("Email không hợp lệ: " + email);
        }
        this.email = email;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
}
