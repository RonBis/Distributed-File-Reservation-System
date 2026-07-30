# Distributed File Reservation System

## Requirements

- Java 21 JDK
- Run all commands from the project root directory.

Check Java:

```bash
java -version
javac -version
```

Both should show version 21 or newer.

## Compile

```bash
mkdir -p bin
javac --release 21 -d bin $(find src -name "*.java")
```

## Run

Open 5 terminals in the project root and start one site in each terminal:

```bash
java -cp bin Application 1
java -cp bin Application 2
java -cp bin Application 3
java -cp bin Application 4
java -cp bin Application 5
```

Each site reads:

- `sites.conf` for site IDs, ports, and neighbours.
- `resources.txt` for the home site of each resource.

Start all 5 sites before testing commands. Early `Connection refused` messages are normal while the other sites are still starting.

## Commands

Type these in any site terminal:

```text
lock <resource-id>
release <resource-id>
status
snapshot
help
exit
```

Example:

```text
lock 21
status
release 21
snapshot
```

Available resources are:

```text
11, 12 at site 1
21, 22 at site 2
31, 32 at site 3
41, 42 at site 4
51, 52 at site 5
```

## Logs

Runtime details are written to:

```text
logs/site-<site-id>-<timestamp>.log
```

The terminal mostly accepts commands; the important system messages are in the log files.

## How To Read Messages

- `Server listening` means the site is running on its configured port.
- `Connecting to ...` means a site is trying to connect to a neighbour.
- `Connected to ...` means a neighbour connection is ready.
- `Local Command` means the command came from that site's terminal.
- `Outgoing message` means the site sent a message to another site.
- `Remote Message` means the site received a network message.
- `ReqResourceLockMsg` means a site requested a resource lock.
- `ResourceLockAckMsg ... isGranted: true` means the lock was granted.
- `ResourceLockAckMsg ... isGranted: false` means the resource is busy.
- `AddWaitingSiteMsg` means another site is waiting for a resource currently held here.
- `PublicLabelQueryMsg`, `PublicLabelQueryReplyMsg`, and `PublicLabelTransmitMsg` are Mitchell-Merritt deadlock detection messages.
- `(BLOCK RULE)` means a blocked site updated its public/private labels.
- `(TRANSMIT RULE)` means a label was forwarded to waiting sites.
- `Deadlock detected` means the Mitchell-Merritt algorithm found a waiting cycle.
- `SNAPSHOT` output shows the Chandy-Lamport snapshot: local resource state plus any in-transit channel messages.

## Status Output

`status` writes a `Resource Manager` block to that site's log:

- `Public Label` and `Private Label` are used for deadlock detection.
- `Acquired Resources` are resources currently locked by this site.
- `Waiting Sites` lists sites waiting on resources held by this site.
- `Own Resources` lists resources whose home site is this site, their current holder, and their waiting queue.

## Stop

Type this in each site terminal:

```text
exit
```

