package comm;

import comm.message.AbstractMessage;

public interface MessageReceiver {

    void onMessage(AbstractMessage msg);
}
