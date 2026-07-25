package application;

import comm.AbstractMessage;

public abstract sealed class Message extends AbstractMessage {

    private Message(int sender, int recipient) {
        super(sender, recipient);
    }

    /// Resource Lock Request message
    public static final class ReqResourceLockMsg extends Message {

        private final int resourceId;

        public ReqResourceLockMsg(int sender, int recipient, int resourceId) {
            super(sender, recipient);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    /// Resource Lock ACK message
    public static final class ResourceLockAckMsg extends Message {

        /// If resource lock request was granted
        private final int resourceId;
        private final boolean isGranted;
        private final int publicLabel;

        public ResourceLockAckMsg(int sender, int recipient, int resourceId, boolean isGranted, int publicLabel) {
            super(sender, recipient);
            this.resourceId = resourceId;
            this.isGranted = isGranted;
            this.publicLabel = publicLabel;
        }

        public int getResourceId() {
            return resourceId;
        }

        public int getPublicLabel() {
            return publicLabel;
        }

        public boolean isGranted() {
            return isGranted;
        }
    }

    /// Resource Release Request message
    public static final class ReqReleaseResourceMsg extends Message {

        public ReqReleaseResourceMsg(int sender, int recipient) {
            super(sender, recipient);
        }
    }

//    public static final class MyMessage extends Message {
//
//        private final int myField;
//
//        private MyMessage(int sender, int recipient, int myField) {
//            super(sender, recipient);
//            this.myField = myField;
//        }
//
//        public int getMyField() {
//            return myField;
//        }
//    }
}
