package comm;

import comm.message.AbstractMessage;
import core.Site;
import core.SiteConfig;
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
    private final Map<Integer, String> peerIdAddrMap;
    /// [Server]s that this site is connected to. We are using a general connected graph topology.
    private final ConcurrentHashMap<String, Client> peerAddrClientMap = new ConcurrentHashMap<>();
    /// (Destination Hop Map) stores the next hop on the shortest path to any destination site (sites with resources).
    private final Map<Integer, Integer> nextHopMap;

    private final CountDownLatch peersConnected;
    private Server server;
    private final Thread messageListenerThread;

    private static final Logger LOG = Log.getLogger(Transport.class.getSimpleName());

    public Transport(SiteConfig siteConfig, Site site) {
        this.site = site;
        this.peerIdAddrMap = siteConfig.peerIdAddrMap();
        this.nextHopMap = siteConfig.nextHopMap();

        this.peersConnected = new CountDownLatch(peerIdAddrMap.size());

        final String sockAddr = siteConfig.addr();
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
        final Thread serverThread = new Thread(server, "[Server Thread]");
        // Server exception handler
        serverThread.setUncaughtExceptionHandler((_, throwable) -> {
            LOG.severe("Server thread failed: " + throwable.getMessage());
            // TODO (Ronil): Notify parent, restart server, etc.
        });
        serverThread.start();

        // Try to connect other servers in the network
        try {
            // Connect with direct peers
            tryConnectPeers();

            messageListenerThread = new Thread(
                    this::listenMessages,
                    "[Message Listener Thread]");

            // MessageListener exception handler
            messageListenerThread.setUncaughtExceptionHandler(
                    (_, throwable) ->
                            LOG.severe("Message listener thread failed: " + throwable.getMessage())
            );
            messageListenerThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void listenMessages() {
        // Message listen loop
        while (true) {
            try {
                final AbstractMessage msg = server.msgQ.take();
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

    public void postLocalMessage(AbstractMessage m) {
        try {
            server.msgQ.put(m);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while posting local message.", e);
        }
    }

    public void send(AbstractMessage m) {
        try {
            if (m.getRecipient() == m.getSender()) {
                postLocalMessage(m);
                return;
            }

            // Wait for all peers to connect
            peersConnected.await();

            final int nextHopId = nextHopMap.get(m.getRecipient());

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

    public void closeAllConnections() {
        try {
            server.close(); // Stop server socket
            // Close all client sockets
            for (Client c : peerAddrClientMap.values()) {
                c.close();
            }

            messageListenerThread.interrupt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
