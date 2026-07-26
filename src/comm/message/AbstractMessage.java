package comm.message;

import java.io.Serial;
import java.io.Serializable;

public abstract sealed class AbstractMessage implements Serializable permits LocalMessage, ResourceMessage {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int sender, recipient;

    public int getSender() {
        return sender;
    }

    public int getRecipient() {
        return recipient;
    }

    protected AbstractMessage(int sender, int recipient) {
        this.sender = sender;
        this.recipient = recipient;
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() +
                ", sender: " + sender +
                ", recipient: " + recipient +
                ">";
    }
}
