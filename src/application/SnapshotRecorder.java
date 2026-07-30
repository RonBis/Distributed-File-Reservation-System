package application;

import application.resource.ResourceManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import comm.Transport;
import comm.message.AbstractMessage;
import comm.message.SnapshotMessage;
import util.Log;

public class SnapshotRecorder {

    private final int siteId;
    private final int initiator;
    private final Set<Integer> neighbourIds;

    // Last recorded snapshot id for this site
    private int snapshotId = -1;
    // Channel states: snapshotId -> (fromSiteId -> list of messages)
    private final Map<Integer, Map<Integer, List<AbstractMessage>>> channelStatesBySnapshot
            = new HashMap<>();
    // Open channels: snapshotId -> set of senderIds from which we are still recording
    private final Map<Integer, Set<Integer>> openChannelsBySnapshot = new HashMap<>();

    private final ResourceManager resourceManager;
    private final Transport transport;

    private static final Logger LOG = Log.getLogger(SnapshotRecorder.class.getSimpleName());

    public SnapshotRecorder(
            int siteId,
            int initiator,
            Set<Integer> neighbourIds,
            ResourceManager resourceManager,
            Transport transport
    ) {
        this.siteId = siteId;
        this.initiator = initiator;
        this.neighbourIds = neighbourIds;
        this.resourceManager = resourceManager;
        this.transport = transport;
    }

    /**
     * Record local state of this site for a given snapshotId.
     * This should be called when a site first participates in a snapshot run.
     */
    public synchronized void recordLocalState(int snapshotId) {
        // If this site is not the initiator and we have already recorded this snapshot, skip.
        if (siteId != initiator && this.snapshotId == snapshotId) {
            return;
        }

        this.snapshotId = snapshotId;

        // Initialize open channels: record from all neighbours until marker arrives from them
        Set<Integer> openChannels = new HashSet<>();
        for (int neighbourId : neighbourIds) {
            if (neighbourId == siteId) {
                continue;
            }
            openChannels.add(neighbourId);
        }
        openChannelsBySnapshot.put(snapshotId, openChannels);

        // Initialize empty channel state map for this snapshot
        channelStatesBySnapshot.put(snapshotId, new HashMap<>());

            }

    /**
     * Log the complete snapshot (local + channel state) for a given snapshotId at this site.
     * Assumes local state has already been recorded.
     */
    private synchronized void logSnapshot(int snapshotId) {
        Map<Integer, List<AbstractMessage>> channelStates = channelStatesBySnapshot.get(snapshotId);
        if (channelStates == null) {
            channelStates = Map.of();
        }

        StringBuilder channelsSb = new StringBuilder();
        channelsSb.append("Channel states:\n");

        if (channelStates.isEmpty()) {
            channelsSb.append("  (no in-transit messages recorded)\n");
        } else {
            for (Map.Entry<Integer, List<AbstractMessage>> entry : channelStates.entrySet()) {
                int fromSite = entry.getKey();
                List<AbstractMessage> msgs = entry.getValue();
                channelsSb.append("  From site ").append(fromSite).append(":\n");
                if (msgs.isEmpty()) {
                    channelsSb.append("    (no messages)\n");
                } else {
                    for (AbstractMessage m : msgs) {
                        channelsSb.append("    ").append(m).append("\n");
                    }
                }
            }
        }

        LOG.info(("""
                
                ======================= SNAPSHOT (#%d) AT SITE %d =====================
                Timestamp: %s
                
                Local state:
                %s
                
                %s
                ========================================================================
                """)
                .formatted(
                        snapshotId,
                        siteId,
                        Instant.now(),
                        resourceManager.getStatus(),
                        channelsSb.toString()
                ));
    }

    /**
     * Initiates a snapshot:
     * 1) Records local state at this site.
     * 2) Sends snapshot markers to all other sites.
     */
    public void startSnapshot() {
        // Case 1: this site IS the designated initiator => actually start the snapshot
        if (siteId == initiator) {
            snapshotId += 1;    // Increment snapshot id with each snapshot initiate request

            // Step 1: record local state at this site
            recordLocalState(snapshotId);

            // Step 2: send markers to all neighbours
            for (int destId : neighbourIds) {
                if (destId == this.siteId) {
                    continue;
                }
                transport.send(new SnapshotMessage.SnapshotMarkerMsg(siteId, destId, snapshotId));
            }

            LOG.info("Site " + siteId + " started snapshot #" + snapshotId + " as initiator.");
        }
        // Case 2: this site is NOT the initiator => send a ReqSnapshotMsg to initiator
        else {
            transport.send(
                    new SnapshotMessage.ReqSnapshotMsg(
                            siteId,
                            initiator
                    )
            );
            LOG.info("Site " + siteId + " requested snapshot from initiator " + initiator + ".");
        }
    }

    /**
     * Called for every incoming message (except markers) so we can record
     * messages in transit as channel state for active snapshots.
     */
    public synchronized void maybeRecordChannelMessage(AbstractMessage msg) {
        int senderId = msg.getSender();

        // For the current snapshotId (lastSnapshotId), check if this sender's channel is still open
        int snapshotId = this.snapshotId;
        if (snapshotId < 0) {
            // No snapshot recorded yet at this site
            return;
        }

        Set<Integer> openChannels = openChannelsBySnapshot.get(snapshotId);
        if (openChannels == null || !openChannels.contains(senderId)) {
            // We are not recording this channel (either marker already arrived or not a neighbour)
            return;
        }

        // Record this message as part of the channel state
        Map<Integer, List<AbstractMessage>> channelStates = channelStatesBySnapshot.get(snapshotId);
        if (channelStates == null) {
            return;
        }

        channelStates.computeIfAbsent(senderId, k -> new ArrayList<>()).add(msg);

        LOG.info("Site " + siteId + " recorded in-transit message on channel from "
                + senderId + " for snapshot " + snapshotId + ": " + msg);
    }

    /**
     * Handle a snapshot marker message:
     * - If this is the first time we see this snapshotId, record local state.
     * - Forward marker to all other sites via the given transport.
     */
    public void handleSnapshotMarker(SnapshotMessage.SnapshotMarkerMsg msg) {
        int markerSnapshotId = msg.getSnapshotId();

        // If this is the first time we see this snapshotId at this site,
        // record local state and initialize openChannels/channelStates.
        boolean firstTimeForThisSnapshot = (this.snapshotId != markerSnapshotId);
        if (firstTimeForThisSnapshot) {
            recordLocalState(markerSnapshotId);
        } else {
            LOG.info("Site " + siteId + " received marker for already-recorded snapshot "
                    + markerSnapshotId + " - local state already recorded, closing channel only.");
        }

        // Close this incoming channel (from msg.getSender()) for this snapshot
        Set<Integer> openChannels = openChannelsBySnapshot.get(markerSnapshotId);
        if (openChannels != null) {
            openChannels.remove(msg.getSender());
        }

        // If all incoming channels are now closed for this snapshot, log complete snapshot state.
        if (openChannels != null && openChannels.isEmpty()) {
            logSnapshot(markerSnapshotId);
        }

        // Forward marker to neighbours ONLY the first time we participate in this snapshotId.
        if (firstTimeForThisSnapshot) {
            for (int destId : neighbourIds) {
                if (destId == siteId) {
                    continue;
                }
                transport.send(
                        new SnapshotMessage.SnapshotMarkerMsg(
                                siteId,
                                destId,
                                markerSnapshotId
                        )
                );
                LOG.info("Site " + siteId + " forwarded SnapshotMarkerMsg for snapshot "
                        + markerSnapshotId + " to neighbour " + destId);
            }
        }
    }}
