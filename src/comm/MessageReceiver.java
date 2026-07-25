package comm;

public interface MessageReceiver {

    void onMessage(AbstractMessage msg);
}
