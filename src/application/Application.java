package application;

import util.FileReader;
import util.Log;

import java.io.IOException;
import java.util.*;

/// Connectivity: General graph topology<br><br>
/// Site1: {2,4}<br>
/// Site2: {1,3}<br>
/// Site3: {2,4,5}<br>
/// Site4: {1,3,5}<br>
/// Site5: {3,4}
public class Application {

    static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Site argument is not provided!");
            System.exit(1);
        }

        int siteId = -1;
        try {
            siteId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Site argument is invalid! Provide a number.");
            System.exit(1);
        }


        try {
            // Initialize Logger
            Log.initialize(siteId);

            final SiteConfig siteConf = readSiteConf(siteId);
            final Map<Integer, Integer> globalDesignFileTable = readResourceTable();

            new Site(siteConf, globalDesignFileTable);
        } catch (Exception e) {
            System.err.println("Failed to start site! Exiting\n" + e);
            System.exit(2);
        }
    }

    private static SiteConfig readSiteConf(int siteId) throws IOException, NumberFormatException {
        final List<String> lines = FileReader.getInstance().readLines("sites.conf");

        final Map<Integer, String> siteIdAddrMap = new HashMap<>();
        final Map<Integer, int[]> sitePeerIdsMap = new HashMap<>();

        final Map<Integer, String> sitePeerIdAddrMap = new HashMap<>();

        // Skip first line
        for (int i = 1; i < lines.size(); i++) {
            // Format:
            // <site id>    <site address>    <site peer ids>
            //
            final String[] args = lines.get(i).split(" {4}");   // separated by 4 spaces

            final int id = Integer.parseInt(args[0]);
            final String addr = args[1];
            final int[] peers = Arrays.stream(args[2].split(","))
                    .mapToInt(Integer::parseInt).toArray();

            siteIdAddrMap.put(id, addr);
            sitePeerIdsMap.put(id, peers);
        }

        for (int peerId : sitePeerIdsMap.get(siteId)) {
            sitePeerIdAddrMap.put(peerId, siteIdAddrMap.get(peerId));
        }

        // Find initiator
        int initiatorId = findInitiator(sitePeerIdsMap);

        // Find next hops along the shortest path to a destination using BFS
        final Map<Integer, Integer> nextHopMap =
                calculateNextHopsAlongShortestPath(siteId, siteIdAddrMap.keySet(), sitePeerIdsMap);
        return new SiteConfig(siteId, siteIdAddrMap.get(siteId), sitePeerIdAddrMap, nextHopMap, initiatorId);
    }

    private static Map<Integer, Integer> readResourceTable() throws IOException {
        final List<String> lines = FileReader.getInstance().readLines("resources.txt");

        final Map<Integer, Integer> globalResourceTable = new HashMap<>();

        // Skip first line
        for (int i = 1; i < lines.size(); i++) {
            // Format:
            // <resource id>    <site ids>
            //
            final String[] args = lines.get(i).split(" {4}");   // separated by 4 spaces

            final int resourceId = Integer.parseInt(args[0]);
            final int siteId = Integer.parseInt(args[1]);

            globalResourceTable.put(resourceId, siteId);
        }
        return globalResourceTable;
    }

    /// Find next hops along the shortest path to every destination using BFS
    private static Map<Integer, Integer> calculateNextHopsAlongShortestPath(
            int siteId,
            Set<Integer> destinationIds,
            Map<Integer, int[]> sitePeerIdsMap
    ) {
        final Map<Integer, Integer> nextHopMap = new HashMap<>();

        final Queue<Integer> queue = new ArrayDeque<>();
        final Set<Integer> visited = new HashSet<>();
        final Map<Integer, Integer> parent = new HashMap<>();

        queue.offer(siteId);
        visited.add(siteId);
        parent.put(siteId, -1);

        while (!queue.isEmpty()) {
            final int curr = queue.poll();
            for (int neighbor : sitePeerIdsMap.get(curr)) {
                if (visited.add(neighbor)) {
                    parent.put(neighbor, curr);
                    queue.offer(neighbor);
                }
            }
        }

        // Determine the first hop for every destination
        for (int dest : destinationIds) {
            if (dest == siteId) {
                continue;
            }
            int node = dest;
            // Walk up the BFS tree until we reach a direct neighbour of siteId
            while (parent.get(node) != siteId) {
                node = parent.get(node);
            }
            nextHopMap.put(dest, node);
        }
        return nextHopMap;
    }

    /**
     * Find a single initiator for the graph using DFS reachability.
     * For each site ID, run a DFS and check if it can reach all other sites.
     * The first site that can reach everyone is chosen as initiator.
     */
    private static int findInitiator(Map<Integer, int[]> sitePeerIdsMap) {
        final Set<Integer> allSiteIds = sitePeerIdsMap.keySet();

        for (int candidateId : allSiteIds) {
            // DFS from candidateId
            final Set<Integer> visited = new HashSet<>();
            final Deque<Integer> stack = new ArrayDeque<>();
            stack.push(candidateId);

            while (!stack.isEmpty()) {
                final int curr = stack.pop();
                if (!visited.add(curr)) {
                    continue;
                }
                final int[] neighbours = sitePeerIdsMap.get(curr);
                if (neighbours != null) {
                    for (int n : neighbours) {
                        if (!visited.contains(n)) {
                            stack.push(n);
                        }
                    }
                }
            }

            // If DFS reached all nodes, this candidate is a valid initiator
            if (visited.containsAll(allSiteIds)) {
                return candidateId;
            }
        }

        throw new IllegalStateException("No initiator found that can reach all sites!");
    }
}
