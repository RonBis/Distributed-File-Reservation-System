package application;

import application.resource.ResourceManager;
import comm.MessageReceiver;
import comm.Transport;
import comm.message.AbstractMessage;
import comm.message.LocalMessage;
import comm.message.ResourceMessage;
import comm.message.SnapshotMessage;
import util.Log;

import java.util.Map;
import java.util.logging.Logger;

public class Site implements Runnable, MessageReceiver {

    private final int id;
    private final ResourceManager resourceManager;
    private final Transport transport;
    private final SnapshotRecorder snapshotRecorder;

    private static final Logger LOG = Log.getLogger(Site.class.getSimpleName());

    public Site(
            SiteConfig conf,
            Map<Integer, Integer> globalDesignFileTable
    ) {
        this.id = conf.id();
        LOG.info("Site " + id + " started");

        // Register graceful shutdown hook, ie: close server before System.exit()
        Runtime.getRuntime().addShutdownHook(
                new Thread(this::shutdown, "Site " + id + " [Shutdown hook]"));

        this.transport = new Transport(conf, this);
        this.resourceManager = new ResourceManager(id, globalDesignFileTable, transport);

        // Snapshot recording
        this.snapshotRecorder = new SnapshotRecorder(
                id,
                conf.initiatorId(),
                conf.peerIdAddrMap().keySet(),
                resourceManager,
                transport
        );

        new Thread(this, "Site " + id + " [Main Thread]").start();
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        new ConsoleController(id, transport);
    }

    @Override
    public void onMessage(AbstractMessage msg) {
        if (msg instanceof LocalMessage) {
            LOG.info("LocalCommand: " + msg);
        } else {
            LOG.info("RemoteMessage: " + msg);

            // Record this message as part of channel state
            // if appropriate. (non-marker messages)
            if (!(msg instanceof SnapshotMessage.SnapshotMarkerMsg)) {
                snapshotRecorder.maybeRecordChannelMessage(msg);
            }
        }
        switch (msg) {
            case SnapshotMessage snapshotMessage -> {
                switch (snapshotMessage) {
                    case SnapshotMessage.SnapshotMarkerMsg m -> snapshotRecorder.handleSnapshotMarker(m);
                    case SnapshotMessage.ReqSnapshotMsg m -> {
                        // Only the configured initiator should react by starting a snapshot
                        if (id == m.getRecipient()) {
                            LOG.info("Site " + id + " received ReqSnapshotMsg from site "
                                    + m.getSender() + " and will start a snapshot.");
                            snapshotRecorder.startSnapshot();
                        } else {
                            LOG.info("Site " + id + " received ReqSnapshotMsg not intended for it; ignoring.");
                        }
                    }
                }
            }
            case ResourceMessage resourceMessage -> {
                switch (resourceMessage) {
                    case ResourceMessage.ReqResourceLockMsg m -> resourceManager.handleReqLockResourceMsg(m);
                    case ResourceMessage.ResourceLockAckMsg m -> resourceManager.handleResourceLockAckMsg(m);
                    case ResourceMessage.ReqReleaseResourceMsg m -> resourceManager.handleReqReleaseResourceMsg(m);
                    case ResourceMessage.ReleaseResourceAckMsg m -> resourceManager.handleReleaseResourceAckMsg(m);
                    case ResourceMessage.AddWaitingSiteMsg m -> resourceManager.handleAddWaitingSiteMsg(m);
                    case ResourceMessage.PublicLabelQueryMsg m -> resourceManager.handlePublicLabelQueryMsg(m);
                    case ResourceMessage.PublicLabelQueryReplyMsg m ->
                            resourceManager.handlePublicLabelQueryReplyMsg(m);
                    case ResourceMessage.PublicLabelTransmitMsg m -> resourceManager.handlePublicLabelTransmitMsg(m);
                }
            }
            case LocalMessage localMessage -> {
                switch (localMessage) {
                    case LocalMessage.ReqResourceLockMsg m -> resourceManager.requestResource(m.getResourceId());
                    case LocalMessage.ReqReleaseResourceMsg m -> resourceManager.releaseResource(m.getResourceId());
                    case LocalMessage.PrintStatusMsg _ -> resourceManager.printStatus();
                    case LocalMessage.ReqSnapshotMsg _ -> snapshotRecorder.startSnapshot();
                    case LocalMessage.ExitMsg _ -> System.exit(0);
                }
            }
        }
    }

    private void shutdown() {
        LOG.info("Shutting down...");
        transport.closeAllConnections();
    }
}
