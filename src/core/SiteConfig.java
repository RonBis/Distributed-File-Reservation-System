package core;

import java.util.Map;

/**
 * @param id            Site id
 * @param addr          Socket address the site server will bind to
 * @param peerIdAddrMap Map: peer site id => peer site socket address
 * @param nextHopMap    Map: next hop site id to send a Message to a destination site via the shortest path
 */
public record SiteConfig(
        int id,
        String addr,
        Map<Integer, String> peerIdAddrMap,
        Map<Integer, Integer> nextHopMap,
        int initiatorId
) {
}
