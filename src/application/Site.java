package application;

import application.resource.ResourceManager;
import comm.MessageReceiver;
import comm.Transport;
import comm.message.AbstractMessage;
import comm.message.LocalMessage;
import comm.message.ResourceMessage;
import util.Log;

import java.util.Map;
import java.util.logging.Logger;

public class Site implements Runnable, MessageReceiver {

    private final int id;
    private final ResourceManager resourceManager;
    private final Transport transport;

    private static final Logger LOG = Log.getLogger(Site.class.getSimpleName());

    public Site(SiteConfig conf, Map<Integer, Integer> globalDesignFileTable) {
        // Register graceful shutdown hook, ie: close server before System.exit()
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        this.id = conf.id();
        LOG.info("Site " + id + " started");
        this.transport = new Transport(conf, this);
        this.resourceManager = new ResourceManager(id, globalDesignFileTable, transport);


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
        switch (msg) {
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
