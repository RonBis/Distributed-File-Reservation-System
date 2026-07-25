package application;

import comm.MessageReceiver;
import comm.Transport;
import util.Log;

import java.util.Map;
import java.util.logging.Logger;

public class Site implements Runnable, MessageReceiver {

    private final int id;

    private final Map<Integer, Integer> globalDesignFileTable;
    private final Transport transport;

    private static final Logger LOG = Log.getLogger(Site.class.getSimpleName());

    public Site(
            SiteConfig conf,
            Map<Integer, Integer> globalDesignFileTable
    ) {
        this.id = conf.id();
        this.globalDesignFileTable = globalDesignFileTable;
        this.transport = new Transport(conf, this);

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
                    requestResource(52);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void requestResource(int resourceId) {
        final int destinationSiteId = globalDesignFileTable.get(resourceId);
        transport.send(new Message.ReqLockResourceMsg(this.id, destinationSiteId));
    }

    @Override
    public void onMessage(Message msg) {
        LOG.info(msg.toString());
        switch (msg) {
            case Message.ReqLockResourceMsg m -> handleReqLockResourceMsg(m);
            case Message.ReqReleaseResourceMsg m -> handleReqReleaseResourceMsg(m);
        }
    }

    private void handleReqLockResourceMsg(Message.ReqLockResourceMsg msg) {

    }

    private void handleReqReleaseResourceMsg(Message.ReqReleaseResourceMsg msg) {

    }
}
