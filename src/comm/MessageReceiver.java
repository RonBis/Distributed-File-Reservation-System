package comm;

import application.Message;

public interface MessageReceiver {

    void onMessage(Message msg);
}
