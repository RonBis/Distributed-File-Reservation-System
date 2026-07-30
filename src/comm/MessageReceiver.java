package comm;

import comm.message.AbstractMessage;

/**
 * Callback interface implemented by a Site to receive incoming messages.
 *
 * <p>The Transport invokes {@link #onMessage(AbstractMessage)} whenever a
 * message is delivered to the site, regardless of whether it originated
 * from another site over the network or from a local source.</p>
 */
public interface MessageReceiver {

    /**
     * Called by the Transport when a message is delivered to this site.
     *
     * @param msg the received message
     */
    void onMessage(AbstractMessage msg);
}
