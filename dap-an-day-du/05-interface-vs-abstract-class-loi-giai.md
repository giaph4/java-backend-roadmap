# Lời giải đầy đủ — Module 01.5: Interface vs Abstract Class

> Nguồn đề: `05 interface vs abstract class/05-interface-vs-abstract-class.md` (Phần B — Bài tập viết code). Chỉ làm Phần B.

---

## Bài 1 — MediaPlayer (default method)

### Đề
`interface MediaPlayer`: abstract `play(String file)`; default `pause()` → `"Đã tạm dừng"`; default `stop()` → `"Đã dừng"`. `AudioPlayer`, `VideoPlayer` chỉ cài `play()`. `PremiumVideoPlayer` override `stop()` để "lưu vị trí đang xem" rồi gọi `MediaPlayer.super.stop()`.

### Phân tích

`default method` (Java 8+) cho phép interface có **cài đặt sẵn**, khác với method `abstract` truyền thống (bắt buộc lớp implements phải tự viết). Nhờ đó, `AudioPlayer`/`VideoPlayer` chỉ cần lo phần **thực sự khác biệt** giữa chúng (`play()`), còn `pause()`/`stop()` dùng chung mặc định — không phải copy-paste.

`PremiumVideoPlayer` muốn **mở rộng** hành vi `stop()` mặc định (không phải thay thế hoàn toàn) — dùng cú pháp `MediaPlayer.super.stop()` để gọi **đúng bản default gốc** của interface, rồi thêm logic riêng trước/sau đó.

### Lời giải

```java
package baitap.bai1;

public class Main {

    interface MediaPlayer {
        void play(String file); // abstract - BẮT BUỘC lớp implements tự viết

        default String pause() {
            return "Đã tạm dừng";
        }

        default String stop() {
            return "Đã dừng";
        }
    }

    static class AudioPlayer implements MediaPlayer {
        @Override
        public void play(String file) {
            System.out.println("[Audio] Đang phát: " + file);
        }
    }

    static class VideoPlayer implements MediaPlayer {
        @Override
        public void play(String file) {
            System.out.println("[Video] Đang phát: " + file);
        }
    }

    static class PremiumVideoPlayer implements MediaPlayer {
        private int lastPositionSec = 0;

        @Override
        public void play(String file) {
            System.out.println("[Premium Video] Đang phát: " + file);
            lastPositionSec = 42; // giả lập đang xem tới giây 42
        }

        @Override
        public String stop() {
            System.out.println("Lưu vị trí đang xem: " + lastPositionSec + "s");
            return MediaPlayer.super.stop(); // gọi ĐÚNG bản default gốc của interface, không tự lặp lại logic
        }
    }

    public static void main(String[] args) {
        MediaPlayer audio = new AudioPlayer();
        audio.play("song.mp3");
        System.out.println(audio.pause());
        System.out.println(audio.stop());

        System.out.println();

        MediaPlayer premium = new PremiumVideoPlayer();
        premium.play("movie.mp4");
        System.out.println(premium.stop());
    }
}
```

**Kết quả chạy:**
```
[Audio] Đang phát: song.mp3
Đã tạm dừng
Đã dừng

[Premium Video] Đang phát: movie.mp4
Lưu vị trí đang xem: 42s
Đã dừng
```

### Giải thích

- `AudioPlayer`/`VideoPlayer` **không hề** viết `pause()`/`stop()` — chúng "thừa hưởng" thẳng bản default của `MediaPlayer`, y hệt cách class thường kế thừa method từ class cha, dù `MediaPlayer` là `interface`.
- `MediaPlayer.super.stop()` là cú pháp **bắt buộc** khi muốn gọi default method gốc từ 1 override — không thể chỉ viết `super.stop()` như với class thường, vì `super` đơn thuần chỉ dùng cho superclass, còn interface cần chỉ rõ **interface nào** (`MediaPlayer.super`) — quan trọng khi 1 class implement nhiều interface có method trùng tên (xem tiếp Bài 3).

---

## Bài 2 — Static factory qua interface

### Đề
`interface Discount { double apply(double price); }` + static `percentage(double pct)`, `fixedAmount(double amt)` (trả lambda), `none()`. Thêm default `andThen(Discount next)` để **ghép** hai giảm giá. `main`: `Discount.percentage(10).andThen(Discount.fixedAmount(5)).apply(200)`.

### Phân tích

`Discount` chỉ có **1 abstract method** (`apply`) → đây là **functional interface**, có thể gán bằng lambda. `static factory method` (Java 8+ cho phép interface có method `static`) là kỹ thuật tạo sẵn các instance `Discount` thông dụng mà không cần class riêng — chỉ cần trả về 1 lambda ngay trong thân static method.

`andThen()` là default method thể hiện **function composition**: áp dụng `this` trước, lấy kết quả đưa tiếp vào `next` — đúng ý tưởng `Function.andThen()` chuẩn của JDK (sẽ gặp lại đầy đủ ở Module 03.3 Stream/Lambda).

### Lời giải

```java
package baitap.bai2;

public class Main {

    interface Discount {
        double apply(double price);

        static Discount percentage(double pct) {
            return price -> price * (1 - pct / 100);
        }

        static Discount fixedAmount(double amt) {
            return price -> Math.max(0, price - amt); // không để giá âm
        }

        static Discount none() {
            return price -> price;
        }

        default Discount andThen(Discount next) {
            return price -> next.apply(this.apply(price)); // áp dụng "this" trước, rồi "next"
        }
    }

    public static void main(String[] args) {
        double result = Discount.percentage(10)
                .andThen(Discount.fixedAmount(5))
                .apply(200);

        System.out.println("percentage(10).andThen(fixedAmount(5)).apply(200) = " + result);

        // Kiểm chứng từng bước
        double afterPercentage = Discount.percentage(10).apply(200);
        double afterFixed = Discount.fixedAmount(5).apply(afterPercentage);
        System.out.println("Bước 1 - sau percentage(10%): " + afterPercentage);
        System.out.println("Bước 2 - sau fixedAmount(5): " + afterFixed);

        System.out.println("none().apply(200) = " + Discount.none().apply(200));
    }
}
```

**Kết quả chạy:**
```
percentage(10).andThen(fixedAmount(5)).apply(200) = 175.0
Bước 1 - sau percentage(10%): 180.0
Bước 2 - sau fixedAmount(5): 175.0
none().apply(200) = 200.0
```

### Giải thích

- `Discount.percentage(10)` gọi qua **tên interface** (`Discount.percentage(...)`), giống hệt cách gọi static method của class — đây là lý do `static factory method` trên interface rất tiện: **gom nhóm các cách tạo instance thông dụng** ngay cạnh định nghĩa hợp đồng, không cần 1 class `DiscountFactory` riêng.
- `andThen()` trả về **1 lambda mới** (không sửa `this`) — mỗi `Discount` là bất biến, ghép nhiều `Discount` không làm thay đổi các `Discount` gốc, có thể tái sử dụng `Discount.percentage(10)` cho nhiều tổ hợp khác nhau.
- Thứ tự **rất quan trọng**: `percentage(10).andThen(fixedAmount(5))` (giảm % trước, trừ tiền cố định sau) cho kết quả khác với `fixedAmount(5).andThen(percentage(10))` (trừ tiền trước, rồi mới giảm %) — thử tính tay sẽ thấy 2 kết quả khác nhau, đúng bản chất phép hợp thành hàm số **không giao hoán**.

---

## Bài 3 — Giải quyết xung đột default method

### Đề
`Walker` và `Swimmer` đều có default `move()`. `Amphibian implements Walker, Swimmer` override `move()` gọi **cả hai** bản gốc (`Walker.super.move()`, `Swimmer.super.move()`) rồi in thêm dòng riêng.

### Phân tích

Khi 1 class implement **2 interface cùng có default method trùng tên, trùng signature**, Java **bắt buộc** class đó phải tự override method đó — nếu không, compiler báo lỗi **"class inherits unrelated defaults"** (không tự chọn hộ vì không biết ý định là gì). Bên trong bản override, có thể gọi lại **CẢ HAI** bản gốc qua cú pháp `TênInterface.super.method()`.

### Lời giải

```java
package baitap.bai3;

public class Main {

    interface Walker {
        default void move() {
            System.out.println("Đang đi bộ");
        }
    }

    interface Swimmer {
        default void move() {
            System.out.println("Đang bơi");
        }
    }

    // BẮT BUỘC override move() - nếu bỏ dòng dưới, compiler báo lỗi:
    // "class Amphibian inherits unrelated defaults for move() from types Walker and Swimmer"
    static class Amphibian implements Walker, Swimmer {
        @Override
        public void move() {
            Walker.super.move();   // gọi ĐÚNG bản default của Walker
            Swimmer.super.move();  // gọi ĐÚNG bản default của Swimmer
            System.out.println("-> Amphibian di chuyển được cả 2 cách!");
        }
    }

    public static void main(String[] args) {
        new Amphibian().move();
    }
}
```

**Kết quả chạy:**
```
Đang đi bộ
Đang bơi
-> Amphibian di chuyển được cả 2 cách!
```

### Giải thích

- Nếu chỉ viết `super.move()` (không ghi rõ interface nào) sẽ **lỗi compile** — vì `super` đơn thuần trong ngữ cảnh interface đa kế thừa là **mơ hồ** (compiler không biết chọn `Walker` hay `Swimmer`). Cú pháp `Walker.super.move()` bắt buộc phải **chỉ rõ interface**.
- Đây là lý do đa kế thừa qua `class` bị Java cấm (mơ hồ, không giải quyết được rõ ràng — "Diamond Problem" kinh điển của C++), nhưng đa kế thừa qua `interface` với default method được cho phép **có kiểm soát**: compiler buộc lập trình viên phải **tường minh giải quyết xung đột**, không tự động đoán.

---

## Bài 4 — Interface + Abstract class (hệ hình học)

### Đề
`interface Drawable { void draw(); }`, `interface Resizable { void resize(double f); }`. `abstract class Shape implements Drawable, Resizable` chứa `width`, `height`, cài sẵn `resize()` (nhân `width`/`height` với `f`), để `draw()` abstract. `Square`, `Circle` chỉ cài `draw()`.

### Phân tích

Bài này thể hiện cách **kết hợp cả 2 công cụ đúng vai trò**: `interface` (`Drawable`, `Resizable`) định nghĩa **hợp đồng khả năng** (có thể vẽ, có thể đổi kích thước — nhiều class không liên quan bản chất vẫn có thể cùng implement), còn `abstract class Shape` cung cấp **cài đặt CHUNG dùng lại được** (`resize()` giống nhau cho mọi hình có `width`/`height`) — điều mà `interface` (trước default method, và ngay cả có default method) **không tiện làm** vì interface không có field lưu trạng thái (`width`, `height`).

### Lời giải

```java
package baitap.bai4;

public class Main {

    interface Drawable {
        void draw();
    }

    interface Resizable {
        void resize(double factor);
    }

    static abstract class Shape implements Drawable, Resizable {
        protected double width;
        protected double height;

        protected Shape(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void resize(double factor) {
            // Cài đặt CHUNG - MỌI lớp con dùng lại, không cần viết riêng
            this.width *= factor;
            this.height *= factor;
        }

        // draw() vẫn ABSTRACT - mỗi hình vẽ khác nhau, không thể dùng chung
        @Override
        public abstract void draw();
    }

    static class Square extends Shape {
        public Square(double side) { super(side, side); }

        @Override
        public void draw() {
            System.out.printf("Vẽ hình vuông cạnh %.1f%n", width);
        }
    }

    static class Circle extends Shape {
        public Circle(double diameter) { super(diameter, diameter); } // width = height = đường kính

        @Override
        public void draw() {
            System.out.printf("Vẽ hình tròn đường kính %.1f%n", width);
        }
    }

    public static void main(String[] args) {
        Square square = new Square(4);
        Circle circle = new Circle(6);

        square.draw();
        circle.draw();

        square.resize(2); // dùng CHUNG resize() từ Shape - không viết riêng trong Square
        circle.resize(0.5);

        square.draw();
        circle.draw();
    }
}
```

**Kết quả chạy:**
```
Vẽ hình vuông cạnh 4.0
Vẽ hình tròn đường kính 6.0
Vẽ hình vuông cạnh 8.0
Vẽ hình tròn đường kính 3.0
```

### Giải thích

- `Square`/`Circle` **không hề viết `resize()`** — kế thừa thẳng từ `Shape`. Nếu `resize()` được đặt trong `interface Resizable` (dù là default method), interface đó **không có field `width`/`height`** để thao tác — buộc phải đặt logic có trạng thái này ở `abstract class`.
- Đây là quy tắc thực chiến: **dùng `interface` cho "khả năng/hợp đồng"** (không trạng thái), **dùng `abstract class` cho "cài đặt chung có trạng thái"** — 2 công cụ bổ trợ nhau, không thay thế nhau.

---

## Bài 5 — Refactor abstract class → interface

### Đề
Cho `abstract class SoundMaker` mà `Dog` và `Robot` (không liên quan bản chất) đang buộc phải kế thừa. Đổi `SoundMaker` thành `interface`; giải thích 2–3 câu vì sao hợp lý hơn (is-a vs can-do).

### Phân tích

`Dog` và `Robot` **không có quan hệ "is-a" tự nhiên** — không có khái niệm "Robot LÀ MỘT loại SoundMaker" theo nghĩa phân loại sinh học/vật lý như "Dog là một Animal". Cái chúng có chung chỉ là **khả năng** — "có thể phát ra âm thanh" (can-do). Bản chất "can-do" (khả năng) là đúng ngữ nghĩa của `interface`, không phải `extends` (kế thừa is-a).

### Trước khi refactor (thiết kế sai)

```java
package baitap.bai5;

abstract class SoundMakerBad {
    abstract void makeSound();
}

class DogBad extends SoundMakerBad {
    @Override void makeSound() { System.out.println("Gâu gâu!"); }
    // Dog CHỈ kế thừa được SoundMakerBad - không thể "extends" thêm gì khác
    // (VD: nếu muốn Dog cũng extends 1 class Animal chung -> KHÔNG THỂ, Java chỉ đơn kế thừa)
}

class RobotBad extends SoundMakerBad {
    @Override void makeSound() { System.out.println("Bíp bíp!"); }
}
```

### Lời giải — refactor thành interface

```java
package baitap.bai5;

public class Main {

    interface SoundMaker {
        void makeSound();
    }

    static class Animal {
        protected String name;
        Animal(String name) { this.name = name; }
    }

    // Dog GIỜ ĐÂY vẫn có thể "extends Animal" (is-a Animal, quan hệ phân loại thật)
    // ĐỒNG THỜI "implements SoundMaker" (can-do - có khả năng phát âm thanh)
    static class Dog extends Animal implements SoundMaker {
        Dog(String name) { super(name); }
        @Override public void makeSound() { System.out.println(name + " sủa: Gâu gâu!"); }
    }

    // Robot KHÔNG PHẢI Animal, nhưng vẫn "can-do" phát âm thanh - chỉ cần implements
    static class Robot implements SoundMaker {
        @Override public void makeSound() { System.out.println("Robot kêu: Bíp bíp!"); }
    }

    public static void main(String[] args) {
        SoundMaker[] makers = { new Dog("Lu"), new Robot() };
        for (SoundMaker m : makers) {
            m.makeSound();
        }
    }
}
```

**Kết quả chạy:**
```
Lu sủa: Gâu gâu!
Robot kêu: Bíp bíp!
```

### Giải thích vì sao interface hợp lý hơn (is-a vs can-do)

1. **Đúng bản chất ngữ nghĩa:** "Dog LÀ MỘT Animal" là quan hệ phân loại thật (is-a) — nên dùng `extends`. "Dog/Robot CÓ THỂ phát âm thanh" chỉ là **khả năng dùng chung**, không phải bản chất phân loại — nên dùng `implements` (can-do).
2. **Giải phóng "suất" đơn kế thừa:** Java chỉ cho `extends` **1 class duy nhất**. Nếu `SoundMaker` là `abstract class`, `Dog` **mất luôn quyền** `extends Animal` (hoặc bất kỳ class nào khác biểu diễn đúng bản chất của nó) — buộc phải hy sinh 1 trong 2 mối quan hệ quan trọng hơn. Đổi `SoundMaker` sang `interface`, `Dog` **giữ được cả 2**: vừa `extends Animal` (is-a), vừa `implements SoundMaker` (can-do) — vì 1 class có thể `implements` **nhiều** interface cùng lúc.
3. **Robot không hề "là một SoundMaker" theo nghĩa phân loại** — ép nó `extends SoundMakerBad` chỉ để tận dụng chung `makeSound()` là dùng kế thừa **sai mục đích** (kế thừa vì tiện lợi code, không vì quan hệ bản chất) — 1 anti-pattern phổ biến khi mới học OOP.

---

## Bài 6 — Skeletal implementation

### Đề
`interface Stack<E>` với `push`, `pop`, `peek`, `size`, `isEmpty`. Viết `abstract class AbstractStack<E> implements Stack<E>` cài sẵn `isEmpty()` (dựa `size()`) và `peek()` throw nếu rỗng. Viết `ArrayStack<E> extends AbstractStack<E>` chỉ cài `push`/`pop`/`size`.

### Phân tích

**Skeletal implementation** (mẫu hình rất phổ biến trong JDK thật, VD `AbstractList`, `AbstractMap`) là 1 `abstract class` **implements** interface, tự cài sẵn những method **có thể suy ra được từ các method khác** — giảm gánh nặng cho lớp con cụ thể, chỉ còn phải viết những method **thực sự cốt lõi, không thể suy ra**.

`isEmpty()` luôn có thể tính từ `size() == 0` — không cần lớp con tự viết lại logic này mỗi lần. `peek()` (xem phần tử đỉnh mà không lấy ra) **cần truy cập dữ liệu thật** nên vẫn phải để `abstract` (hoặc cài đặt dựa trên method trừu tượng khác nếu thiết kế cho phép) — ở đây chọn cách chung nhất: `AbstractStack` cài `peek()` với logic kiểm tra rỗng dùng chung, nhưng việc "lấy phần tử đỉnh thật sự" giao lại cho lớp con qua 1 abstract method riêng.

### Lời giải

```java
package baitap.bai6;

import java.util.EmptyStackException;

public class Main {

    interface Stack<E> {
        void push(E item);
        E pop();
        E peek();
        int size();
        boolean isEmpty();
    }

    static abstract class AbstractStack<E> implements Stack<E> {

        // Cài sẵn DỰA TRÊN size() - lớp con không cần viết lại
        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        // Cài sẵn LOGIC KIỂM TRA chung (throw nếu rỗng),
        // nhưng việc "lấy phần tử đỉnh thật" giao cho lớp con qua peekInternal()
        @Override
        public E peek() {
            if (isEmpty()) {
                throw new EmptyStackException();
            }
            return peekInternal();
        }

        protected abstract E peekInternal(); // lớp con tự biết cấu trúc dữ liệu thật để lấy đỉnh
    }

    static class ArrayStack<E> extends AbstractStack<E> {
        private final java.util.List<E> data = new java.util.ArrayList<>();

        @Override
        public void push(E item) {
            data.add(item);
        }

        @Override
        public E pop() {
            if (isEmpty()) throw new EmptyStackException();
            return data.remove(data.size() - 1);
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        protected E peekInternal() {
            return data.get(data.size() - 1);
        }
    }

    public static void main(String[] args) {
        ArrayStack<String> stack = new ArrayStack<>();
        System.out.println("isEmpty() ban đầu: " + stack.isEmpty()); // dùng chung từ AbstractStack

        stack.push("a");
        stack.push("b");
        stack.push("c");

        System.out.println("size() = " + stack.size());
        System.out.println("peek() = " + stack.peek()); // dùng chung, kiểm tra rỗng tự động
        System.out.println("pop() = " + stack.pop());
        System.out.println("size() sau pop = " + stack.size());

        stack.pop();
        stack.pop();
        try {
            stack.peek();
        } catch (EmptyStackException e) {
            System.out.println("peek() khi rỗng -> EmptyStackException (đúng như kỳ vọng)");
        }
    }
}
```

**Kết quả chạy:**
```
isEmpty() ban đầu: true
size() = 3
peek() = c
pop() = c
size() sau pop = 2
peek() khi rỗng -> EmptyStackException (đúng như kỳ vọng)
```

### Giải thích

- `ArrayStack` chỉ phải viết **4 method** (`push`, `pop`, `size`, `peekInternal`) — `isEmpty()` và `peek()` (với đầy đủ logic throw khi rỗng) được **thừa hưởng miễn phí** từ `AbstractStack`. Nếu có thêm `LinkedStack` (dùng `LinkedList` thay vì `ArrayList`), nó cũng chỉ cần viết đúng 4 method đó — không lặp lại logic `isEmpty()`/`peek()`.
- Đây chính xác là cách JDK thật thiết kế `AbstractList`: lớp con (`ArrayList`, `LinkedList`) chỉ cần cài vài method cốt lõi, còn hàng chục method tiện ích khác (`indexOf`, `contains`, `isEmpty`...) được `AbstractList`/`AbstractCollection` cung cấp sẵn dựa trên các method cốt lõi đó.

---

## Bài 7 — Re-abstraction

### Đề
`interface JsonSerializable` có default `toJson()` trả `"{}"`. `interface StrictJsonSerializable extends JsonSerializable` re-abstract `toJson()`. Chứng minh: `class A implements JsonSerializable {}` compile được, còn `class B implements StrictJsonSerializable {}` **không** compile cho tới khi cài `toJson()`.

### Phân tích

**Re-abstraction** là kỹ thuật khai báo lại **CÙNG method** (cùng tên, cùng signature) nhưng **bỏ từ khóa `default`**, biến 1 method **đã có cài đặt sẵn** ở interface cha thành **BẮT BUỘC phải cài đặt lại** ở interface con (hoặc bất kỳ class nào implements interface con đó). Dùng khi muốn **thắt chặt hợp đồng**: interface cha cho phép "dùng tạm mặc định", nhưng interface con (chuyên biệt hơn) đòi hỏi **bắt buộc phải có cài đặt thật sự**, không được dùng giá trị "cho có" như `"{}"`.

### Lời giải

```java
package baitap.bai7;

public class Main {

    interface JsonSerializable {
        default String toJson() {
            return "{}"; // mặc định "cho có" - không bắt buộc mọi class phải tự viết
        }
    }

    interface StrictJsonSerializable extends JsonSerializable {
        @Override
        String toJson(); // RE-ABSTRACT - bỏ "default", bắt buộc phải cài lại
    }

    // Compile ĐƯỢC - A dùng default toJson() = "{}" của JsonSerializable, không cần tự viết
    static class A implements JsonSerializable {
    }

    // Nếu bỏ comment dòng dưới, sẽ LỖI COMPILE:
    // "class B is not abstract and does not override abstract method toJson() in StrictJsonSerializable"
    //
    // static class B implements StrictJsonSerializable {
    // }

    // Phải cài đặt tường minh mới compile được:
    static class B implements StrictJsonSerializable {
        @Override
        public String toJson() {
            return "{\"type\":\"B\"}";
        }
    }

    public static void main(String[] args) {
        A a = new A();
        System.out.println("A.toJson() = " + a.toJson()); // dùng default "{}"

        B b = new B();
        System.out.println("B.toJson() = " + b.toJson()); // dùng bản đã cài đặt tường minh
    }
}
```

**Kết quả chạy:**
```
A.toJson() = {}
B.toJson() = {"type":"B"}
```

### Giải thích

- `class A implements JsonSerializable {}` — thân class **rỗng hoàn toàn** vẫn compile được, vì `toJson()` đã có `default`, `A` **được phép** không cài đặt gì thêm.
- `class B implements StrictJsonSerializable {}` (rỗng) **không compile** — vì `StrictJsonSerializable` đã "xóa" default, biến `toJson()` trở lại thành abstract. Lỗi cụ thể: `"class B is not abstract and does not override abstract method toJson() in StrictJsonSerializable"`.
- **Ứng dụng thực tế:** đây là kỹ thuật dùng khi thiết kế hệ interface phân tầng — VD `Repository` (interface gốc, có default method tiện ích cho phần lớn trường hợp) và `StrictRepository extends Repository` (dùng cho các nghiệp vụ đặc biệt **bắt buộc** phải override kỹ, không được dùng hành vi mặc định "an toàn nhưng sơ sài").

---

## Bài 8 — "Class thắng interface" (dự đoán rồi kiểm chứng)

### Đề
```java
class LegacyPrinter { public void print() { System.out.println("Legacy"); } }
interface ModernPrinter { default void print() { System.out.println("Modern"); } }
class Report extends LegacyPrinter implements ModernPrinter { }
// new Report().print();  -> dự đoán? Muốn gọi bản "Modern" thì viết thế nào trong Report?
```

### Phân tích

Java có quy tắc rõ ràng khi 1 method có thể tới từ **cả class cha lẫn interface (default)**: **"class luôn thắng interface"** (class wins) — bất kể interface có `default` hay không, method kế thừa từ **superclass** luôn được ưu tiên. Quy tắc này tồn tại để **đảm bảo tương thích ngược**: default method là tính năng thêm vào Java 8 **sau khi** hàng triệu dòng code cũ đã tồn tại — nếu để default method "thắng" bất ngờ đè lên hành vi class cha có sẵn, code cũ có thể đổi hành vi ngoài ý muốn chỉ vì 1 interface mới thêm default method trùng tên.

### Lời giải

```java
package baitap.bai8;

public class Main {

    static class LegacyPrinter {
        public void print() {
            System.out.println("Legacy");
        }
    }

    interface ModernPrinter {
        default void print() {
            System.out.println("Modern");
        }
    }

    static class Report extends LegacyPrinter implements ModernPrinter {
        // KHÔNG override gì cả - để xem quy tắc "class thắng interface" áp dụng ra sao
    }

    // Muốn Report thực sự chạy bản "Modern": phải TỰ override + gọi tường minh
    static class ModernReport extends LegacyPrinter implements ModernPrinter {
        @Override
        public void print() {
            ModernPrinter.super.print(); // ép gọi đúng bản default của interface
        }
    }

    public static void main(String[] args) {
        new Report().print();       // dự đoán: "Legacy" - class luôn thắng interface
        new ModernReport().print(); // "Modern" - vì override tường minh, tự chọn gọi bản nào
    }
}
```

**Kết quả chạy:**
```
Legacy
Modern
```

### Giải thích

- `new Report().print()` in `"Legacy"` — dù `ModernPrinter.print()` là method **mới hơn, "gần" `Report` hơn về mặt khai báo** (`implements` ngay tại `Report`), quy tắc **"class luôn thắng interface"** vẫn áp dụng tuyệt đối, không xét khoảng cách kế thừa.
- **Muốn gọi bản "Modern"**, `Report` **bắt buộc phải tự override `print()`**, và bên trong gọi tường minh `ModernPrinter.super.print()` — không có cách nào khác để "chọn" interface thắng class một cách tự động, đây là quyết định **phải làm tường minh, không có default ngầm**.
- **Liên hệ thực tế:** đây chính xác là tình huống hay gặp khi nâng cấp code cũ (`LegacyPrinter` có sẵn từ lâu) để "cắm thêm" 1 interface mới có default method (`ModernPrinter`, VD để đồng bộ với hệ thống mới) — hiểu đúng quy tắc "class thắng interface" giúp tránh bug im lặng do tưởng nhầm hành vi mới sẽ tự động được áp dụng.

---

*Đây là lời giải cho toàn bộ Phần B của Module 05. Tiếp theo: Module 06 — SOLID Principles.*
