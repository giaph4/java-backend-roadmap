class Base {
    Base(int x) {

    }
}

class Sub extends Base {
    Sub(int x) {
        super(x);
    }
}

public class Car extends Vehicle {

    private int doors;

    public Car(String brand, int doors) {
        super(brand);
        this.doors = doors;
    }

    @Override
    public void move() {
        super.move();
    }


    public void openTrunk() {
        System.out.println("The trunk of the car is opened.");
    }
}
