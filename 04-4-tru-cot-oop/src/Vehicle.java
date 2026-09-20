public class Vehicle {

    protected String brand;
    protected int speed;

    public Vehicle(String brand) {
        this.brand = brand;
    }

    public void move() {
        System.out.printf("The %s is moving at a speed of %d km/h.%n", brand, speed);
    }
}
