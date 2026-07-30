package core.resource;

import java.io.Serializable;

/**
 * Mitchell-Merritt public/private label, represented as a {@code (counter, siteId)} pair
 * instead of a bare integer.
 * <p>
 * The BLOCK rule computes new labels as {@code max(u, blockerU) + 1}. With a plain int,
 * two unrelated sites can land on the same counter purely by coincidence, and since
 * deadlock is detected on label equality, that coincidence looks exactly like a real cycle.
 * <p>
 * Example: Site A blocks on Site B and computes counter 6. Separately, Site C blocks on
 * Site D (an unrelated chain) and also computes counter 6. If Site C's label reaches
 * Site A, a bare-int comparison would see {@code 6 == 6} and wrongly report a deadlock
 * between A and C, even though they were never waiting on each other.
 * <p>
 * Tagging each label with its originating site's ID fixes this: {@code (6, siteC)} and
 * {@code (6, siteA)} are different values, so no false match occurs. Two labels can only
 * be equal if the same site generated both — which only happens when a site's own label has
 * actually travelled all the way around a real cycle and returned. The counter is compared
 * first (it drives the "larger label wins" ordering the BLOCK/TRANSMIT rules rely on);
 * siteId only breaks ties, never overrides a counter difference.
 */
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
