package application;

import comm.AbstractMessage;

public abstract sealed class Message extends AbstractMessage {

    protected Message(int sender, int recipient) {
        super(sender, recipient);
    }

    /// Resource Lock Request message
    public static final class ReqLockResourceMsg extends Message {

        ReqLockResourceMsg(int sender, int recipient) {
            super(sender, recipient);
        }
    }

    /// Resource Release Request message
    public static final class ReqReleaseResourceMsg extends Message {

        ReqReleaseResourceMsg(int sender, int recipient) {
            super(sender, recipient);
        }
    }
}
