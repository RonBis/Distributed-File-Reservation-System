package comm.message;

import java.util.Set;

public abstract sealed class ResourceMessage extends AbstractMessage {

    private ResourceMessage(int sender, int recipient) {
        super(sender, recipient);
    }

    /// Resource Lock Request message
    public static final class ReqResourceLockMsg extends ResourceMessage {

        private final int resourceId;

        /// Resource Lock Request message
        public ReqResourceLockMsg(
                int sender,
                int recipient,
                int resourceId
        ) {
            super(sender, recipient);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    /// Resource Lock Acknowledgement message
    public static final class ResourceLockAckMsg extends ResourceMessage {

        private final int resourceId;
        /// If resource lock request was granted
        private final boolean isGranted;
        /// Site of the site which is using the resource. If [isGranted] is false, this is null.
        private final Integer holderSiteId;
        /// Sites that are currently waiting for this granted resource
        private final Set<Integer> remainingWaiters;

        /// Resource Lock Acknowledgement message
        public ResourceLockAckMsg(
                int sender,
                int recipient,
                int resourceId,
                boolean isGranted,
                Integer holderSiteId,
                Set<Integer> remainingWaiters
        ) {
            super(sender, recipient);
            this.resourceId = resourceId;
            this.isGranted = isGranted;
            this.holderSiteId = holderSiteId;
            this.remainingWaiters = remainingWaiters;
        }

        public int getResourceId() {
            return resourceId;
        }

        public boolean isGranted() {
            return isGranted;
        }

        public Integer getHolderSiteId() {
            return holderSiteId;
        }

        public Set<Integer> getRemainingWaiters() {
            return remainingWaiters;
        }
    }

    /// Resource Release Request message
    public static final class ReqReleaseResourceMsg extends ResourceMessage {

        private final int resourceId;

        /// Resource Release Request message
        public ReqReleaseResourceMsg(
                int sender,
                int recipient,
                int resourceId
        ) {
            super(sender, recipient);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    /// Resource Release Acknowledgement message.<br>
    /// This message also instructs the receiver site to
    /// remove a previously waiting site to be removed from its waitlist.
    public static final class ReleaseResourceAckMsg extends ResourceMessage {

        private final Integer waitingSiteToBeRemoved;

        /// Resource Release Acknowledgement message.<br>
        /// This message also instructs the receiver site to
        /// remove a previously waiting site to be removed from its waitlist.
        public ReleaseResourceAckMsg(
                int sender,
                int recipient,
                Integer waitingSiteToBeRemoved  // Nullable
        ) {
            super(sender, recipient);
            this.waitingSiteToBeRemoved = waitingSiteToBeRemoved;
        }

        public Integer getWaitingSiteToBeRemoved() {
            return waitingSiteToBeRemoved;
        }
    }

    /// Whenever a resource request is blocked,
    /// this message is sent to the blocker site to let it know who is waiting for it.
    public static final class AddWaitingSiteMsg extends ResourceMessage {

        private final int waitingSiteId;

        /// Whenever a resource request is blocked,
        /// this message is sent to the blocker site to let it know who is waiting for it.
        public AddWaitingSiteMsg(
                int sender,
                int recipient,
                int waitingSiteId
        ) {
            super(sender, recipient);
            this.waitingSiteId = waitingSiteId;
        }

        public int getWaitingSiteId() {
            return waitingSiteId;
        }
    }

    public static final class PublicLabelQueryMsg extends ResourceMessage {

        public PublicLabelQueryMsg(int sender, int recipient) {
            super(sender, recipient);
        }
    }

    public static final class PublicLabelQueryReplyMsg extends ResourceMessage {

        private final int publicLabel;

        public PublicLabelQueryReplyMsg(int sender, int recipient, int publicLabel) {
            super(sender, recipient);
            this.publicLabel = publicLabel;
        }

        public int getPublicLabel() {
            return publicLabel;
        }
    }

    /// Message for propagating(transmit) public label in Mitchell-Merritt algorithm
    public static final class PublicLabelTransmitMsg extends ResourceMessage {

        private final int publicLabel;

        /// Message for propagating(transmit) public label in Mitchell-Merritt algorithm
        public PublicLabelTransmitMsg(int sender, int recipient, int publicLabel) {
            super(sender, recipient);
            this.publicLabel = publicLabel;
        }

        public int getPublicLabel() {
            return publicLabel;
        }
    }
}
