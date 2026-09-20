package phanB.bai2;

public class EmailNotification implements Notifiable{
    @Override
    public void send(String msg) {
        System.out.println("[Email] " + msg);
    }
}
