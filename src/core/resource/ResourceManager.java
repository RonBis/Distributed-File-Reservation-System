package core.resource;

import comm.Transport;
import comm.message.ResourceMessage;
import util.Log;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ResourceManager {

    private final int siteId;

    /// Map: Resource id => ID of the site that has the resource
    private final Map<Integer, Integer> globalResourceTable;
    /// Map: Resource id => Resource object
    private final Map<Integer, Resource> resourceMap = new HashMap<>();

    /// Resource ids currently held (locked) by this site, regardless of which site is home for them
    private final Set<Integer> heldResourceIds = new HashSet<>();

    // Public label and private labels to detect deadlock using Mitchell-Merritt algorithm
    private MitchellMerrittLabel publicLabel, privateLabel;
    /// Sites that are blocked on this site
    private final Map<Integer, Set<Integer>> waitingSitesByResource;

    private final Transport transport;
    private static final Logger LOG = Log.getLogger(ResourceManager.class.getSimpleName());

    public ResourceManager(
            int siteId,
            Map<Integer, Integer> globalResourceTable,
            Transport transport
    ) {
        this.siteId = siteId;

        // Ensures distinct public-private label pair for each site
        this.publicLabel = this.privateLabel = new MitchellMerrittLabel(siteId, siteId);
        this.globalResourceTable = globalResourceTable;

        this.waitingSitesByResource = new HashMap<>();
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
        LOG.info(getStatus());
    }

    public String getStatus() {
        return """
                
                ==================== Resource Manager ====================
                Public Label : %s
                Private Label: %s
                
                Acquired Resources: %s
                
                Waiting Sites: %s
                
                Own Resources:
                %s
                ==========================================================
                """.formatted(
                publicLabel, privateLabel,
                formatOwnedResourcesDisplay(),
                waitingSitesByResource.isEmpty() ? "-" : waitingSitesByResource,
                formatResourcesDisplay()
        );
    }

    private String formatOwnedResourcesDisplay() {
        if (heldResourceIds.isEmpty()) return "-";

        return heldResourceIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private String formatResourcesDisplay() {
        if (resourceMap.isEmpty())
            return "  None";

        final StringBuilder sb = new StringBuilder();
        resourceMap.forEach((resourceId, resource) -> sb.append("""
                  Resource %-4d Holder: %-3s Waiting: %s
                """.formatted(
                resourceId,
                resource.getHolder(),
                resource.getReqQ().isEmpty() ? "-" : resource.getReqQ()
        )));
        return sb.toString();
    }

    // Called by the current site
    public void requestResource(int resourceId) {
        final int destinationSiteId = globalResourceTable.get(resourceId);

        LOG.info("Requesting Resource %d from Site %d."
                .formatted(resourceId, destinationSiteId));

        transport.send(new ResourceMessage.ReqResourceLockMsg(
                siteId,
                destinationSiteId,
                resourceId)
        );
    }

    // Called by the current site
    public void releaseResource(int resourceId) {
        final int destinationSiteId = globalResourceTable.get(resourceId);

        LOG.info("Requesting release of Resource %d from Site %d."
                .formatted(resourceId, destinationSiteId));

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
            LOG.info("Resource %d is held by Site %d. Notifying holder that Site %d is waiting."
                    .formatted(resourceId, resource.getHolder(), requestingSiteId));

            transport.send(new ResourceMessage.AddWaitingSiteMsg(
                    siteId,
                    resource.getHolder(),
                    msg.getSender(),
                    resourceId)
            );
        }

        // Send ACK to the resource requester (Message sender)
        LOG.info("Sending resource lock request acknowledgement to Site %d for Resource %d (granted=%s, holder=Site %d)."
                .formatted(requestingSiteId, resourceId, requestGranted, resource.getHolder()));

        transport.send(new ResourceMessage.ResourceLockAckMsg(
                siteId,
                requestingSiteId,
                resourceId,
                requestGranted,
                resource.getHolder(),
                Collections.emptySet())
        );
    }

    public void handleResourceLockAckMsg(ResourceMessage.ResourceLockAckMsg msg) {
        final int resourceId = msg.getResourceId();

        if (msg.isGranted()) {
            LOG.info("Resource " + resourceId + " granted by site " + msg.getSender() + ".");

            heldResourceIds.add(resourceId);

            // Update waiting sites list when a resource is granted later
            waitingSitesByResource.computeIfAbsent(resourceId, _ -> new HashSet<>())
                    .addAll(msg.getWaitersForResource());
            return;
        }

        // Resource is not granted: Ask holder site for its public id and apply block rule after getting reply
        LOG.info("Resource %d is in use by site %d. Querying site %d for public label."
                .formatted(resourceId, msg.getHolderSiteId(), msg.getHolderSiteId()));

        transport.send(new ResourceMessage.PublicLabelQueryMsg(
                siteId,
                msg.getHolderSiteId(),
                resourceId)
        );
    }

    public void handleReqReleaseResourceMsg(ResourceMessage.ReqReleaseResourceMsg msg) {
        final int resourceId = msg.getResourceId();

        Integer next;
        try {
            next = resourceMap.get(resourceId).releaseLock(msg.getSender());

            LOG.info("Site " + msg.getSender() + " released lock on resource " + resourceId + ".");
        } catch (IllegalStateException ignore) {
            return;
        }

        // Send ReleaseResourceACK to sender
        LOG.info("Sending ResourceReleaseAck for resource %d to site %d."
                .formatted(resourceId, msg.getSender()));

        transport.send(new ResourceMessage.ReleaseResourceAckMsg(
                siteId,
                msg.getSender(),
                resourceId)
        );

        if (next != null) {
            // Let the waiting site know that it has been granted the resource and clear local resource request queue.
            // Waiting site list for a resource could be null at any point, so return an Empty Set to prevent
            // NPE on Set.copyOf().
            final Set<Integer> waiters = Set.copyOf(resourceMap.get(resourceId).getReqQ());

            LOG.info("Sending ResourceLockAck for resource %d to waiting site %d with waiting sites: %s."
                    .formatted(resourceId, next, waiters));

            transport.send(new ResourceMessage.ResourceLockAckMsg(
                    siteId,
                    next,
                    resourceId,
                    true,
                    null,
                    waiters)
            );
        }
    }

    public void handleReleaseResourceAckMsg(ResourceMessage.ReleaseResourceAckMsg msg) {
        final int resourceId = msg.getResourceId();

        heldResourceIds.remove(resourceId);

        final Set<Integer> waitingSitesToBeRemoved = waitingSitesByResource.get(resourceId);

        if (waitingSitesToBeRemoved != null && !waitingSitesToBeRemoved.isEmpty()) {
            waitingSitesToBeRemoved.clear();
            LOG.info("Cleared local waiting list for resource: %d.".formatted(resourceId));
        }
    }

    public void handleAddWaitingSiteMsg(ResourceMessage.AddWaitingSiteMsg msg) {
        LOG.info("Site " + msg.getWaitingSiteId() + " is waiting on me.");

        final int resourceId = msg.getResourceId();
        // Add waiting site for the particular resource
        waitingSitesByResource.computeIfAbsent(resourceId, _ -> new HashSet<>())
                .add(msg.getWaitingSiteId());
    }

    public void handlePublicLabelQueryMsg(ResourceMessage.PublicLabelQueryMsg msg) {
        LOG.info("Sending public label %s to asking site: %d.".formatted(publicLabel, msg.getSender()));

        transport.send(new ResourceMessage.PublicLabelQueryReplyMsg(
                siteId,
                msg.getSender(),
                publicLabel,
                msg.getResourceId())
        );
    }

    /// This method is invoked whenever this site was denied a resource and have to wait until it is available.
    public void handlePublicLabelQueryReplyMsg(ResourceMessage.PublicLabelQueryReplyMsg msg) {
        final MitchellMerrittLabel oldPublicLabel = publicLabel, oldPrivateLabel = privateLabel;

        /*
         * Generate new public and private label according to BLOCK RULE:
         * u = v = max(u, blockerU) + 1
         *
         * This site's siteId is retained here in updated public/private label
         * as BLOCK RULE is just concerned with updating the counter, not the label propagation
         */
        publicLabel = privateLabel = new MitchellMerrittLabel(
                Math.max(this.publicLabel.counter(), msg.getPublicLabel().counter()) + 1, siteId);

        LOG.info("(BLOCK RULE) Before: u/v = %s/%s \t After: u/v = %s/%s"
                .formatted(oldPublicLabel, oldPrivateLabel, publicLabel, privateLabel));

        for (Map.Entry<Integer, Set<Integer>> waitersForResource : waitingSitesByResource.entrySet()) {
            // Now TRANSMIT this updated public label to whoever waiting for this site to release resource
            final int heldResourceId = waitersForResource.getKey();
            final Set<Integer> waitingSites = waitersForResource.getValue();

            if (waitingSites.isEmpty()) continue;

            LOG.info("Transmitting updated public label %s to waiting sites: %s for resource: %d."
                    .formatted(publicLabel, waitingSites, heldResourceId));

            for (int waitingSite : waitingSites) {
                transport.send(new ResourceMessage.PublicLabelTransmitMsg(
                        siteId,
                        waitingSite,
                        publicLabel,
                        heldResourceId)
                );
            }
        }
    }

    public void handlePublicLabelTransmitMsg(ResourceMessage.PublicLabelTransmitMsg msg) {
        final MitchellMerrittLabel oldPublicLabel = publicLabel, oldPrivateLabel = privateLabel;
        final MitchellMerrittLabel incomingPublicLabel = msg.getPublicLabel();

        /*
         * Update public label according to TRANSMIT RULE:
         * if (u < blockerU)
         *     u = blockerU
         *     transmit()
         */
        if (this.publicLabel.compareTo(incomingPublicLabel) < 0) {
            publicLabel = incomingPublicLabel;

            LOG.info("(TRANSMIT RULE) Before: u/v = %s/%s \t After: u/v = %s/%s"
                    .formatted(oldPublicLabel, oldPrivateLabel, publicLabel, privateLabel));

            // Propagate TRANSMIT message
            for (Map.Entry<Integer, Set<Integer>> waitersForResource : waitingSitesByResource.entrySet()) {
                // Now TRANSMIT this updated public label to whoever waiting for this site to release resource
                final int heldResourceId = waitersForResource.getKey();
                final Set<Integer> waitingSites = waitersForResource.getValue();

                if (waitingSites.isEmpty()) continue;

                LOG.info("Transmitting updated public label %s to waiting sites: %s for resource: %d."
                        .formatted(publicLabel, waitingSites, heldResourceId));

                for (int waitingSite : waitingSites) {
                    transport.send(new ResourceMessage.PublicLabelTransmitMsg(
                            siteId,
                            waitingSite,
                            publicLabel,
                            heldResourceId)
                    );
                }
            }
        }

        // Detect rule is applied after processing every transmit message:
        // If incoming public label == own public label and own public label == own private label
        if (publicLabel.equals(incomingPublicLabel) && publicLabel.equals(privateLabel)) {
            LOG.severe("Deadlock detected at sites %d --- %d.".formatted(siteId, msg.getSender()));
            System.exit(99);
        }
    }
}
