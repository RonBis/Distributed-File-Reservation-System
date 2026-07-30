package core.resource;

import util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

abstract public class Resource {

    private final int id;
    /// Nullable field. If this is null, then this [Resource] is not currently in use by any [core.Site].
    private Integer lockedBySiteId;
    /// Stores incoming resource requests in FIFO order
    private final Queue<Integer> reqQ = new LinkedList<>();

    private static final Logger LOG = Log.getLogger(Resource.class.getSimpleName());

    public Resource(int resourceId) {
        this.id = resourceId;
    }

    public int getId() {
        return id;
    }

    /// Could return null
    public Integer getHolder() {
        return lockedBySiteId;
    }

    public Queue<Integer> getReqQ() {
        return reqQ;
    }

    public boolean isLocked() {
        return lockedBySiteId != null;
    }

    /// @return boolean - If request was granted
    public boolean requestLock(int siteId) {
        if (!isLocked()) {
            lockedBySiteId = siteId;
            return true;
        } else {
            // Resource request not granted, add that site to request queue
            reqQ.add(siteId);
            return false;
        }
    }

    /// @return Integer(nullable) - ID of the site who should get the resource next
    public Integer releaseLock(int siteId) {
        if (lockedBySiteId == null || siteId != lockedBySiteId) {
            LOG.warning("Illegal release lock request by site " + siteId + " for resource " + id);
            throw new IllegalStateException();
        }
        final Integer next = reqQ.poll();
        lockedBySiteId = next;
        return next;
    }
}
