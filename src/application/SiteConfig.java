package application;

import java.util.Map;

public record SiteConfig(int id, String addr, Map<Integer, String> peerIdAddrMap) {
}
