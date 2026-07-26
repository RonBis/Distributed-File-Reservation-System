package comm;

import comm.message.AbstractMessage;
import util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class Server implements Runnable {

    private final ServerSocket serverSocket;
    protected final BlockingQueue<AbstractMessage> msgQ = new LinkedBlockingQueue<>(1000);

    private volatile boolean shuttingDown = false;

    private static final Logger LOG = Log.getLogger(Server.class.getSimpleName());

    public Server(InetAddress bindAddr, int port) throws IOException {
        serverSocket = new ServerSocket(port, 20, bindAddr);
    }

    public void run() {
        LOG.info("Server listening at " + serverSocket.getLocalSocketAddress());
        while (!serverSocket.isClosed()) {
            try {
                final Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket, msgQ).start();
            } catch (SocketException e) {
                if (shuttingDown) {
                    break;
                }
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void close() throws IOException {
        shuttingDown = true;
        serverSocket.close();
    }

    public static final class ClientHandler extends Thread {

        private final Socket socket;
        private ObjectInputStream in;

        private final BlockingQueue<AbstractMessage> msgQ;

        public ClientHandler(Socket socket, BlockingQueue<AbstractMessage> msgQ) {
            this.socket = socket;
            this.msgQ = msgQ;

            try {
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                LOG.severe("Exception in client input stream!\n" + e);
            }
        }

        public void run() {
            AbstractMessage incoming;
            try {
                while ((incoming = (AbstractMessage) in.readObject()) != null) {
                    msgQ.put(incoming);
                }
            } catch (ClassNotFoundException e) {
                LOG.severe("Error deserializing object from client " + socket.getRemoteSocketAddress() + "\n" + e);
            } catch (IOException e) {
                LOG.severe("Error reading input stream from client " + socket.getRemoteSocketAddress() + "\n" + e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.severe("Thread interrupted while waiting for Message Queue to free up space.\n" + e);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                LOG.severe("Connection closed with client " + socket.getRemoteSocketAddress());
            }
        }
    }
}
