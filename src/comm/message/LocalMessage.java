package comm.message;

public abstract sealed class LocalMessage extends AbstractMessage
        permits LocalMessage.ReqResourceLockMsg,
        LocalMessage.ReqReleaseResourceMsg,
        LocalMessage.PrintStatusMsg,
        LocalMessage.ExitMsg {

    protected LocalMessage(int siteId) {
        super(siteId, siteId);
    }

    public static final class ReqResourceLockMsg extends LocalMessage {
        private final int resourceId;

        public ReqResourceLockMsg(int siteId, int resourceId) {
            super(siteId);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    public static final class ReqReleaseResourceMsg extends LocalMessage {
        private final int resourceId;

        public ReqReleaseResourceMsg(int siteId, int resourceId) {
            super(siteId);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    public static final class PrintStatusMsg extends LocalMessage {
        public PrintStatusMsg(int siteId) {
            super(siteId);
        }
    }

    public static final class ExitMsg extends LocalMessage {
        public ExitMsg(int siteId) {
            super(siteId);
        }
    }
}
