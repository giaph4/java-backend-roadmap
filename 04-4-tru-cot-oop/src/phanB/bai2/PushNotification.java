package phanB.bai2;

public class PushNotification implements Notifiable {
    @Override
    public void send(String msg) {
        System.out.println("[Push] " + msg);
    }
}
