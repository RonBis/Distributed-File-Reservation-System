package comm;

import application.Message;
import application.Site;
import application.SiteConfig;
import util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class Transport {

    /// This site's socket address
    private final Site site;
    private final String sockAddr;
    private Server server;

    private final Map<Integer, String> peerIdAddrMap;
    /// [Server]s that this site is connected to. We are using a general connected graph topology.
    private final ConcurrentHashMap<String, Client> peerAddrClientMap = new ConcurrentHashMap<>();

    private final CountDownLatch peersConnected;

    // TODO (Ronil): Disabled for now, enable after implementing path finding algo
    /// (Destination Hop Map) stores the next hop on the shortest path to any destination site (sites with resources).
    // private final Map<Integer, Integer> destHopMap = new HashMap<>();
    private final int[][] destHopMatrix = {
            // 0   1  2  3  4  5
            {0, 0, 0, 0, 0, 0}, // unused (1-based indexing)

            {0, 0, 2, 2, 4, 4}, // from 1
            {0, 1, 0, 3, 1, 3}, // from 2
            {0, 2, 2, 0, 4, 5}, // from 3
            {0, 1, 1, 3, 0, 5}, // from 4
            {0, 4, 3, 3, 4, 0}  // from 5
    };

    private static final Logger LOG = Log.getLogger(Transport.class.getSimpleName());

    public String getSockAddr() {
        return sockAddr;
    }

    public Server getServer() {
        return server;
    }

    public Transport(SiteConfig siteConfig, Site site) {
        this.site = site;
        this.sockAddr = siteConfig.addr();
        this.peerIdAddrMap = siteConfig.peerIdAddrMap();

        this.peersConnected = new CountDownLatch(peerIdAddrMap.size());

        try {
            final String[] socketAddr = sockAddr.split(":");
            final InetAddress bindAddr = InetAddress.ofLiteral(socketAddr[0]);
            final int port = Integer.parseInt(socketAddr[1]);

            server = new Server(bindAddr, port);
        } catch (NumberFormatException e) {
            LOG.severe("Invalid port number!");
        } catch (IOException e) {
            throw new RuntimeException("Could not start server!", e);
        }

        // Start server thread
        final Thread serverThread = new Thread(server, "Site " + site.getId() + " [Server Thread]");
        // Server exception handler
        serverThread.setUncaughtExceptionHandler((_, throwable) -> {
            LOG.severe("Server thread failed: " + throwable.getMessage());
            // TODO (Ronil): Notify parent, restart server, etc.
        });
        serverThread.start();

        // Try to connect other servers in the network
        try {
            tryConnectPeers();  // Connect with direct peers
//            establishPaths();    // Find shortest path to every other site with a resource

            final Thread messageListenerThread = new Thread(
                    this::listenMessages,
                    "Site " + site.getId() + " [Message Listener Thread]");

            // MessageListener exception handler
            messageListenerThread.setUncaughtExceptionHandler((_, throwable) -> {
                LOG.severe("Message listener thread failed: " + throwable.getMessage());
            });
            messageListenerThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void listenMessages() {
        // Message listen loop
        while (true) {
            try {
                final Message msg = server.msgQ.take();
                // If current address doesn't match the receiver, forward message to next hop site
                if (msg.getRecipient() != site.getId()) {
                    send(msg);
                } else {
                    // Otherwise, handle message
                    site.onMessage(msg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void send(Message m) {
        try {
            // Wait for all peers to connect
            peersConnected.await();

            final int nextHopId = destHopMatrix[site.getId()][m.getRecipient()];
            final String nextHopAddr = peerIdAddrMap.get(nextHopId);
            final Client client = peerAddrClientMap.get(nextHopAddr);
            if (client == null) {
                throw new IllegalStateException("No connection to next hop " + nextHopId + "!");
            }

            client.send(m);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void tryConnectPeers() throws IOException {
        // Peer = MapEntry<Id, Addr>
        for (final Map.Entry<Integer, String> peer : peerIdAddrMap.entrySet()) {
            final String[] addr = peer.getValue().split(":");
            final String host = addr[0];
            final int port = Integer.parseInt(addr[1]);

            new Thread(() -> {
                Client client = new Client();
                boolean isConnected = client.connect(host, port);
                if (isConnected) {
                    // Add connected client to `peerAddrClientMap` Map
                    peerAddrClientMap.put(peer.getValue(), client);
                    peersConnected.countDown();
                }
            }, "Site " + site.getId() + " [Client " + peer.getValue() + " Thread]").start();
        }
    }

    /**
     * TODO (Ronil):
     * Each site will try to find the shortest path to any another node in the network (with a resource).
     * As path information, each site will only store the next hop information for a particular destination site.
     * We are using BFS as edges assumed to be unweighted.
     */
    private void establishPaths() {
//        final Queue<Integer> bfsQ = new LinkedList<>(peerIdAddrMap.keySet());
//        destHopMap.put()
    }
}
