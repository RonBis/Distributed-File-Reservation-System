package application;

import util.FileReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Connectivity: General graph topology<br><br>
/// Site1: {2,4}<br>
/// Site2: {1,3}<br>
/// Site3: {2,4,5}<br>
/// Site4: {1,3,5}<br>
/// Site5: {3,4}
public class Application {

    public static void main(String[] args) {
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
            final SiteConfig siteConf = readSiteConf(siteId);
            final Map<Integer, Integer> globalDesignFileTable = readResourceTable();

            new Site(siteConf, globalDesignFileTable);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to start site! Exiting");
            System.exit(2);
        }
    }

    static SiteConfig readSiteConf(int siteId) throws IOException, NumberFormatException {
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
                    .mapToInt(Integer::parseInt)
                    .toArray();

            siteIdAddrMap.put(id, addr);
            sitePeerIdsMap.put(id, peers);
        }

        for (int peerId : sitePeerIdsMap.get(siteId)) {
            sitePeerIdAddrMap.put(peerId, siteIdAddrMap.get(peerId));
        }

        return new SiteConfig(siteId, siteIdAddrMap.get(siteId), sitePeerIdAddrMap);
    }

    static Map<Integer, Integer> readResourceTable() throws IOException {
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
}
