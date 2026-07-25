package application;

import application.resource.ResourceManager;
import comm.AbstractMessage;
import comm.MessageReceiver;
import comm.Transport;
import util.Log;

import java.util.Map;
import java.util.logging.Logger;

public class Site implements Runnable, MessageReceiver {

    private final int id;
    private final ResourceManager resourceManager;
    private final Transport transport;

    private static final Logger LOG = Log.getLogger(Site.class.getSimpleName());

    public Site(SiteConfig conf, Map<Integer, Integer> globalDesignFileTable) {
        this.id = conf.id();
        this.transport = new Transport(conf, this);
        this.resourceManager = new ResourceManager(id, globalDesignFileTable, transport);

        new Thread(this, "Site " + id + " [Main Thread]").start();
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        LOG.info("Site " + id + " started");
        try {
            while (true) {
                Thread.sleep(4000);
                if (id != 5)
                    resourceManager.requestResource(52);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessage(AbstractMessage msg) {
        LOG.info(msg.toString());
        switch ((Message) msg) {
            case Message.ReqResourceLockMsg m -> resourceManager.handleReqLockResourceMsg(m);
            case Message.ReqReleaseResourceMsg m -> resourceManager.handleReqReleaseResourceMsg(m);
            case Message.ResourceLockAckMsg m -> resourceManager.handleResourceLockAckMsg(m);
        }
    }
}
