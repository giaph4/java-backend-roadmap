package phanB.bai2;

public class SlackNotification implements Notifiable {
    @Override
    public void send(String msg) {
        System.out.println("[Slack] " + msg);
    }
}
