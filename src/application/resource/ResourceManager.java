package application.resource;

import comm.Transport;
import comm.message.ResourceMessage;
import util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ResourceManager {

    private final int siteId;

    /// Map: Resource id => ID of the site that has the resource
    private final Map<Integer, Integer> globalResourceTable;
    /// Map: Resource id => Resource object
    private final Map<Integer, Resource> resourceMap = new HashMap<>();

    // Public label and private labels to detect deadlock using Mitchell-Merritt algorithm
    private int publicLabel, privateLabel;
    /// Sites that are blocked on this site
    private final Set<Integer> waitingSites = ConcurrentHashMap.newKeySet();

    private final Transport transport;
    private static final Logger LOG = Log.getLogger(ResourceManager.class.getSimpleName());

    public ResourceManager(
            int siteId,
            Map<Integer, Integer> globalResourceTable,
            Transport transport
    ) {
        this.siteId = siteId;
        this.publicLabel = this.privateLabel = siteId;  // Ensures distinct public-private label pair for each site
        this.globalResourceTable = globalResourceTable;

        this.transport = transport;

        initResources();
    }

    private void initResources() {
        for (final Map.Entry<Integer, Integer> resource : globalResourceTable.entrySet()) {
            if (resource.getValue() == this.siteId) {
                resourceMap.put(resource.getKey(), new DesignFile(resource.getKey()));
            }
        }
    }

    public void printStatus() {
        LOG.info("""
                
                ==================== Resource Manager ====================
                Public Label : %d
                Private Label: %d
                
                Waiting Sites: %s
                
                Resources:
                %s
                ==========================================================
                """.formatted(
                publicLabel,
                privateLabel,
                waitingSites.isEmpty() ? "-" : waitingSites,
                formatResources()
        ));
    }

    private String formatResources() {
        if (resourceMap.isEmpty())
            return "  None";

        StringBuilder sb = new StringBuilder();
        resourceMap.forEach((resourceId, resource) -> sb.append("""
                  Resource %-4d Holder: %-3s Waiting: %s
                """.formatted(
                resourceId,
                resource.getLockedBySiteId(),
                resource.getReqQ().isEmpty() ? "-" : resource.getReqQ()
        )));
        return sb.toString();
    }

    // Called by the current site
    public void requestResource(int resourceId) {
        final int destinationSiteId = globalResourceTable.get(resourceId);

        transport.send(new ResourceMessage.ReqResourceLockMsg(
                siteId,
                destinationSiteId,
                resourceId)
        );
    }

    // Called by the current site
    public void releaseResource(int resourceId) {
        final int destinationSiteId = globalResourceTable.get(resourceId);

        transport.send(new ResourceMessage.ReqReleaseResourceMsg(
                siteId,
                destinationSiteId,
                resourceId)
        );
    }

    public void handleReqLockResourceMsg(ResourceMessage.ReqResourceLockMsg msg) {
        final int resourceId = msg.getResourceId();
        final Resource resource = resourceMap.get(resourceId);
        final int requestingSiteId = msg.getSender();

        final boolean requestGranted = resource.requestLock(requestingSiteId);
        if (!requestGranted) {
            // Send a message AddWaitingSite message to let the resource holder know another site is waiting for it
            transport.send(new ResourceMessage.AddWaitingSiteMsg(
                    siteId,
                    resource.getLockedBySiteId(),
                    msg.getSender())
            );
        }

        // Send ACK to the resource requester (Message sender)
        transport.send(new ResourceMessage.ResourceLockAckMsg(
                siteId,
                requestingSiteId,
                resourceId,
                requestGranted,
                resource.getLockedBySiteId(),
                Collections.emptySet())
        );
    }

    public void handleResourceLockAckMsg(ResourceMessage.ResourceLockAckMsg msg) {
        if (msg.isGranted()) {
            LOG.info("Resource " + msg.getResourceId() + " granted by site " + msg.getSender());
            // Update waiting sites list when a resource is granted later
            waitingSites.addAll(msg.getRemainingWaiters());
            return;
        }

        // Resource is not granted: Ask holder site for its public id and apply block rule after getting reply
        LOG.info("Resource " + msg.getResourceId() + " is held by site " + msg.getHolderSiteId());
        transport.send(new ResourceMessage.PublicLabelQueryMsg(
                siteId,
                msg.getHolderSiteId())
        );
    }

    public void handleReqReleaseResourceMsg(ResourceMessage.ReqReleaseResourceMsg msg) {
        Integer next;
        try {
            next = resourceMap.get(msg.getResourceId()).releaseLock(msg.getSender());
            LOG.info("Site " + msg.getSender() + " released lock on resource " + msg.getResourceId());
        } catch (IllegalStateException ignore) {
            return;
        }

        // Send ReleaseResourceACK to sender
        transport.send(new ResourceMessage.ReleaseResourceAckMsg(
                siteId,
                msg.getSender(),
                next)
        );

        if (next != null) {
            // Let the waiting site know that it has been granted the resource
            transport.send(new ResourceMessage.ResourceLockAckMsg(
                    siteId,
                    next,
                    msg.getResourceId(),
                    true,
                    null,
                    waitingSites)
            );
        }
    }

    public void handleReleaseResourceAckMsg(ResourceMessage.ReleaseResourceAckMsg msg) {
        final Integer waitingSiteToBeRemoved = msg.getWaitingSiteToBeRemoved();
        if (waitingSiteToBeRemoved != null) {
            waitingSites.remove(waitingSiteToBeRemoved);
            LOG.info(String.format(
                    "Removed %d from local waiting site set; current=%s",
                    waitingSiteToBeRemoved, waitingSites)
            );
        }
    }

    public void handleAddWaitingSiteMsg(ResourceMessage.AddWaitingSiteMsg msg) {
        LOG.info("Site " + msg.getWaitingSiteId() + " is waiting on me");
        waitingSites.add(msg.getWaitingSiteId());
    }

    public void handlePublicLabelQueryMsg(ResourceMessage.PublicLabelQueryMsg msg) {
        transport.send(new ResourceMessage.PublicLabelQueryReplyMsg(
                siteId,
                msg.getSender(),
                publicLabel)
        );
    }

    /// This method is invoked whenever this site was denied a resource and have to wait for until it is available.
    public void handlePublicLabelQueryReplyMsg(ResourceMessage.PublicLabelQueryReplyMsg msg) {
        final int oldPublicLabel = publicLabel, oldPrivateLabel = privateLabel;

        /*
         * Generate new public and private label according to BLOCK RULE:
         * u = v = max(u, blockerU) + 1
         */
        publicLabel = privateLabel = Math.max(this.publicLabel, msg.getPublicLabel()) + 1;

        LOG.info(String.format(
                "(BLOCK RULE) Before: u/v = %d/%d \t After: u/v = %d/%d",
                oldPublicLabel, oldPrivateLabel, publicLabel, privateLabel)
        );

        // Now transmit this updated public label to whoever waiting for this site to release resource
        LOG.info(String.format(
                "Transmitting updated public label %d to waiting sites: %s",
                publicLabel, waitingSites)
        );
        for (int waitingSite : waitingSites) {
            transport.send(new ResourceMessage.PublicLabelTransmitMsg(
                    siteId,
                    waitingSite,
                    publicLabel)
            );
        }
    }

    public void handlePublicLabelTransmitMsg(ResourceMessage.PublicLabelTransmitMsg msg) {
        final int oldPublicLabel = publicLabel, oldPrivateLabel = privateLabel;

        /*
         * Update public label according to TRANSMIT RULE:
         * if (u < blockerU)
         *     u = blockerU
         *     transmit()
         */
        if (this.publicLabel < msg.getPublicLabel()) {
            publicLabel = msg.getPublicLabel();

            LOG.info(String.format(
                    "(TRANSMIT RULE) Before: u/v = %d/%d \t After: u/v = %d/%d",
                    oldPublicLabel, oldPrivateLabel, publicLabel, privateLabel)
            );

            // Propagate TRANSMIT message
            for (int waitingSite : waitingSites) {
                transport.send(new ResourceMessage.PublicLabelTransmitMsg(
                        siteId,
                        waitingSite,
                        publicLabel)
                );
            }
        }

        // Detect rule is evaluated after processing every transmit message.
        if (publicLabel == privateLabel) {
            LOG.severe("Deadlock detected at sites " + siteId + " --- " + msg.getSender());
            System.exit(99);
        }
    }
}
