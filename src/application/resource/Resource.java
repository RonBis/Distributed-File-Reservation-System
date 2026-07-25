package application.resource;

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

    public boolean isLocked() {
        return lockedBySiteId != null;
    }

    /// @return boolean - If request was granted
    public boolean requestLock(int siteId) {
        if (!isLocked()) {
            lockedBySiteId = siteId;
            return true;
        } else {
            return false;
        }
    }

    public void releaseLock() {
        lockedBySiteId = null;
    }
}
