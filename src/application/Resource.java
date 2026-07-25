package application;

abstract public class Resource {

    private final int id;
    private Integer lockedBySiteId = null;

    public Resource(int resourceId) {
        this.id = resourceId;
    }

    public int getId() {
        return id;
    }

    public Integer getLockedBySiteId() {
        return lockedBySiteId;
    }

    public void requestLock(int siteId) {
        if (lockedBySiteId == null) {
            lockedBySiteId = siteId;
        }
    }

    public void releaseLock() {
        lockedBySiteId = null;
    }
}
