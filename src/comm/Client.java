package comm;

import comm.message.AbstractMessage;
import util.Log;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class Client {

    private Socket clientSocket;
    private ObjectOutputStream out;

    private static final Logger LOG = Log.getLogger(Client.class.getSimpleName());

    public boolean connect(String host, int port) {
        final int MAX_RETRIES = 60;
        final int CONNECTION_TIMEOUT = 5000;  // ms
        int RETRY_DELAY = 5000;  // ms

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                LOG.info("Connecting to " + host + ":" + port + "  ...");

                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);

                out = new ObjectOutputStream(clientSocket.getOutputStream());

                LOG.info("Connected to " + host + ":" + port);
                return true;
            } catch (SocketTimeoutException e) {
                LOG.warning(String.format("Connection timed out for " + host + ":" + port + " (attempt %d/%d)", attempt, MAX_RETRIES));
            } catch (IOException e) {
                LOG.warning(String.format("Connection failed for " + host + ":" + port + " (attempt %d/%d): %s", attempt, MAX_RETRIES, e.getMessage()));
            }

            try {
                Thread.sleep(RETRY_DELAY);
//                RETRY_DELAY *= 2;    // exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.severe("Connection retries interrupted.\n" + e);
            }
        }

        return false;
    }

    public synchronized void send(AbstractMessage m) throws IOException {
        out.writeObject(m);
        out.flush();
    }

    public void close() throws IOException {
        clientSocket.close();
    }
}
