package comm.message;

import application.resource.MitchellMerrittLabel;

import java.util.Set;

public abstract sealed class ResourceMessage extends AbstractMessage {

    private ResourceMessage(int sender, int recipient) {
        super(sender, recipient);
    }

    /// Resource Lock Request message.
    public static final class ReqResourceLockMsg extends ResourceMessage {

        private final int resourceId;

        /// Resource Lock Request message
        public ReqResourceLockMsg(int sender, int recipient,
                                  int resourceId
        ) {
            super(sender, recipient);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    /// Resource Lock Acknowledgement message.
    public static final class ResourceLockAckMsg extends ResourceMessage {

        private final int resourceId;
        /// If resource lock request was granted
        private final boolean isGranted;
        /// Site of the site which is using the resource. If [isGranted] is false, this is null.
        private final Integer holderSiteId;
        /// Sites that are currently waiting for this granted resource
        private final Set<Integer> waitersForResource;

        /// Resource Lock Acknowledgement message
        public ResourceLockAckMsg(int sender, int recipient,
                                  int resourceId,
                                  boolean isGranted,
                                  Integer holderSiteId,
                                  Set<Integer> waitersForResource
        ) {
            super(sender, recipient);
            this.resourceId = resourceId;
            this.isGranted = isGranted;
            this.holderSiteId = holderSiteId;
            this.waitersForResource = waitersForResource;
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

        public Set<Integer> getWaitersForResource() {
            return waitersForResource;
        }
    }

    /// Resource Release Request message.
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
    /// remove a previously waiting sites for a resource to be removed from its waitlist.
    public static final class ReleaseResourceAckMsg extends ResourceMessage {

        /// Resource id for which this [ReleaseResourceAckMsg] was triggered.
        private final int resourceId;

        /// Resource Release Acknowledgement message.<br>
        /// This message also instructs the receiver site to
        /// remove a previously waiting site to be removed from its waitlist.
        public ReleaseResourceAckMsg(int sender, int recipient,
                                     int resourceId
        ) {
            super(sender, recipient);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    /// Whenever a resource request is blocked,
    /// this message is sent to the blocker site to let it know who is waiting for it.
    public static final class AddWaitingSiteMsg extends ResourceMessage {

        private final int waitingSiteId;
        /// Which resource the [waitingSiteId] is waiting for.
        private final int resourceId;

        /// Whenever a resource request is blocked,
        /// this message is sent to the blocker site to let it know who is waiting for it.
        public AddWaitingSiteMsg(
                int sender,
                int recipient,
                int waitingSiteId,
                int resourceId
        ) {
            super(sender, recipient);
            this.waitingSiteId = waitingSiteId;
            this.resourceId = resourceId;
        }

        public int getWaitingSiteId() {
            return waitingSiteId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    public static final class PublicLabelQueryMsg extends ResourceMessage {

        /// This is used in the reply of [PublicLabelQueryMsg], aka [PublicLabelQueryReplyMsg].
        private final int resourceId;

        public PublicLabelQueryMsg(int sender, int recipient,
                                   int resourceId
        ) {
            super(sender, recipient);
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    public static final class PublicLabelQueryReplyMsg extends ResourceMessage {

        private final MitchellMerrittLabel publicLabel;
        /// Resource id for which the corresponding [PublicLabelQueryMsg] was triggered.
        private final int resourceId;

        public PublicLabelQueryReplyMsg(int sender, int recipient,
                                        MitchellMerrittLabel publicLabel, int resourceId
        ) {
            super(sender, recipient);
            this.publicLabel = publicLabel;
            this.resourceId = resourceId;
        }

        public MitchellMerrittLabel getPublicLabel() {
            return publicLabel;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    /// Message for propagating(transmit) public label in Mitchell-Merritt algorithm.
    public static final class PublicLabelTransmitMsg extends ResourceMessage {

        private final MitchellMerrittLabel publicLabel;
        /// Resource id for which this [PublicLabelTransmitMsg] was triggered.
        private final int resourceId;

        /// Message for propagating(transmit) public label in Mitchell-Merritt algorithm
        public PublicLabelTransmitMsg(int sender, int recipient,
                                      MitchellMerrittLabel publicLabel, int resourceId
        ) {
            super(sender, recipient);
            this.publicLabel = publicLabel;
            this.resourceId = resourceId;
        }

        public MitchellMerrittLabel getPublicLabel() {
            return publicLabel;
        }

        public int getResourceId() {
            return resourceId;
        }
    }
}
