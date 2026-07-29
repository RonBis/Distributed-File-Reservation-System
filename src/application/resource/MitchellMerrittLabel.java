package application.resource;

import java.io.Serializable;

/// Mitchell-Merritt Public/Private label
public record MitchellMerrittLabel(int counter, int siteId)
        implements Comparable<MitchellMerrittLabel>, Serializable {

    @Override
    public int compareTo(MitchellMerrittLabel other) {
        int cmp = Integer.compare(counter, other.counter);
        return cmp != 0 ? cmp : Integer.compare(siteId, other.siteId);
    }

    @Override
    public String toString() {
        return "<%d, %d>".formatted(counter, siteId);
    }
}
