package comm.message;

public abstract sealed class SnapshotMessage extends AbstractMessage {

    private SnapshotMessage(int sender, int recipient) {
        super(sender, recipient);
    }

    /// Snapshot marker message for Chandy-Lamport
    public static final class SnapshotMarkerMsg extends SnapshotMessage {

        private final int snapshotId;

        public SnapshotMarkerMsg(int sender, int recipient, int snapshotId) {
            super(sender, recipient);
            this.snapshotId = snapshotId;
        }

        public int getSnapshotId() {
            return snapshotId;
        }
    }
    /// Request message to ask the designated initiator to start a snapshot
    public static final class ReqSnapshotMsg extends SnapshotMessage {

        public ReqSnapshotMsg(int sender, int recipient) {
            super(sender, recipient);
        }

        @Override
        public String toString() {
            return "ReqSnapshotMsg{sender=" + getSender() + ", recipient=" + getRecipient() + "}";
        }
    }
}
