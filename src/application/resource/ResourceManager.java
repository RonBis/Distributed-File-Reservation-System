package application.resource;

import application.DesignFile;
import application.Message;
import comm.Transport;
import util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ResourceManager {

    private final int siteId;

    private int publicLabel;
    private int privateLabel;
    private final Map<Integer, Integer> globalResourceTable;
    /// Map: Resource id => Resource object
    private final Map<Integer, Resource> resourceMap = new HashMap<>();

    private final Transport transport;

    private static final Logger LOG = Log.getLogger(ResourceManager.class.getSimpleName());

    public ResourceManager(int siteId, Map<Integer, Integer> globalResourceTable, Transport transport) {
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

    public void requestResource(int resourceId) {
        final int destinationSiteId = globalResourceTable.get(resourceId);
        transport.send(
                new Message.ReqResourceLockMsg(
                        siteId,
                        destinationSiteId,
                        resourceId
                )
        );
    }

    public void handleReqLockResourceMsg(Message.ReqResourceLockMsg msg) {
        // Send ACK with own public label to the resource requester (Message sender)
        boolean shouldGrantReq = resourceMap.get(msg.getResourceId())
                .requestLock(msg.getSender());
        transport.send(
                new Message.ResourceLockAckMsg(
                        siteId,
                        msg.getSender(),
                        msg.getResourceId(),
                        shouldGrantReq,
                        publicLabel
                )
        );
    }

    public void handleReqReleaseResourceMsg(Message.ReqReleaseResourceMsg msg) {

    }

    public void handleResourceLockAckMsg(Message.ResourceLockAckMsg msg) {
        if (msg.isGranted()) {
            LOG.info("Resource " + msg.getResourceId() + " granted.");
        } else {
            LOG.warning("Resource " + msg.getResourceId() + " locked.");
        }
    }
}
