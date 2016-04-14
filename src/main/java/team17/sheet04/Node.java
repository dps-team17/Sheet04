package team17.sheet04;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Node implements INode {

    private static volatile int NODES_CREATED = 0;
    private static final String separator = ";";

    private int nodeId;
    private NodeInfo[] nodeTable;
    private ServerSocket serverSocket;
    private Random rnd;
    private boolean printSyncMessages;
    private String lastBroadcastMessage;


    private Thread listenerThread;
    private Thread syncThread;
    private boolean printBroadcastMessages;
    private boolean printStartupMessages;

    /**
     * Create a new node with an empty node table (not able to sync)
     */
    public Node() {
        this(null);
    }

    /**
     * Create a new node
     *
     * @param syncPartner the node information of the initial sync partner
     */
    public Node(NodeInfo syncPartner) {
        this.nodeId = NODES_CREATED++;
        this.printSyncMessages = false;
        this.printBroadcastMessages = false;
        this.printStartupMessages = false;

        // Init node table
        nodeTable = new NodeInfo[NODE_TABLE_SIZE];
        nodeTable[0] = syncPartner;
        if (printStartupMessages) log("Node created");
    }

    @Override
    public void Connect() {

        if (printStartupMessages) log("Starting...");

        try {
            createListenerThread();
            listenerThread.start();
            Thread.sleep(100);

            createSyncThread();
            syncThread.start();
            Thread.sleep(100);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("Connected");
    }

    @Override
    public void Disconnect() {

        if (listenerThread == null) throw new IllegalStateException("Node is not connected");

        try {
            syncThread.interrupt();
            syncThread.join();

            serverSocket.close();
            listenerThread.join();

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        log("Disconnected");
    }

    @Override
    public void SendBroadcast(String msg) {
        SendBroadcast(msg, HOP_COUNT, null);
    }

    @Override
    public NodeInfo getNodeInfo() {
        try {
            return new NodeInfo(this.nodeId, InetAddress.getLocalHost().getHostName(), serverSocket.getLocalPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public synchronized NodeInfo[] getKnownNodes() {
        return nodeTable.clone();
    }

    @Override
    public void ShowSyncMessages(boolean value) {
        this.printSyncMessages = value;
    }

    private void createSyncThread() {

        syncThread = new Thread() {

            private final Object lock = new Object();
            private volatile boolean running = true;

            public void run() {

                if (printStartupMessages) log("Start syncing...");

                while (running && !this.isInterrupted()) {
                    try {
                        int timeout = (2 + rnd.nextInt(9)) * 1000;

                        synchronized (lock) {
                            lock.wait(timeout);
                        }

                        sync();

                    } catch (InterruptedException e) {
                        running = false;
                    }
                }

                if (printStartupMessages) log("Syncing stopped");
            }
        };
    }

    private void createListenerThread() {

        listenerThread = new Thread() {

            public void run() {

                try {
                    serverSocket = new ServerSocket(getRandomDynamicPort());

                    if (printStartupMessages) log("Listening on port " + serverSocket.getLocalPort());

                    while (!serverSocket.isClosed()) {

                        try (Socket requester = serverSocket.accept()) {

                            processRequest(requester);

                        } catch (SocketException e) {
                            // socket closed ignore
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (printStartupMessages) log("Stop listening");
            }
        };
    }

    private void processRequest(Socket requester) {

        try (PrintWriter out = new PrintWriter(requester.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(requester.getInputStream()))
        ) {
            // Process request
            String request = in.readLine();

            final String[] parts = request.split(separator);

            switch (parts[0]) {
                case SYNC_MESSAGE:
                    processSyncResponse(request);
                    out.println(getSyncMessage());
                    break;
                case BROADCAST_MESSAGE:
                    processBroadcastMessage(request);
                    break;
            }


            processSyncResponse(request);


        } catch (IOException e) {
            // socket closed ignore
        }
    }

    private void sync() {

        if (!hasKnownNodes()) {
            if (printSyncMessages) log("No sync partner found");
            return;
        }

        NodeInfo syncPartner = getRandomNode();

        if (printSyncMessages) {
            log("Try to sync with " + syncPartner.getHostName() + ":" + syncPartner.getPort());
        }

        try (Socket s = new Socket(syncPartner.getHostName(), syncPartner.getPort());
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            // Send sync request
            String requestString = getSyncMessage();
            if (printSyncMessages) log("Sending request: " + requestString);
            out.println(requestString);

            // Wait for sync response
            s.setSoTimeout(5000);
            final String response = in.readLine();
            if (printSyncMessages) log("Received response: " + response);
            if (response != null) {
                processSyncResponse(response);
            } else {
                if (printSyncMessages) log(syncPartner.getHostName() + " is not reachable any more.");
                removeNode(syncPartner);
            }

        } catch (ConnectException e) {
            if (printSyncMessages) log(syncPartner.getHostName() + " is not reachable any more.");
            removeNode(syncPartner);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the node from the node table
     *
     * @param syncPartner the node to remove
     */
    private synchronized void removeNode(NodeInfo syncPartner) {

        for (int i = 0; i < nodeTable.length; i++) {
            if (nodeTable[i] != null && nodeTable[i].equals(syncPartner)) {
                nodeTable[i] = null;
                break;
            }
        }
    }

    /**
     * Checks if the node has node in the node table
     *
     * @return true if there is a least one node, otherwise false
     */
    private boolean hasKnownNodes() {

        boolean hasNodes = false;

        for (NodeInfo n : nodeTable) {
            if (n != null) {
                hasNodes = true;
                break;
            }
        }

        return hasNodes;
    }

    /**
     * Returns a randomly chosen port number from the range of dynamic ports
     * (49152 - 65534)
     *
     * @return a randomly chosen dynamic port number
     */
    private int getRandomDynamicPort() {
        if (rnd == null) rnd = new Random(Thread.currentThread().getId());
        return rnd.nextInt(16383) + 49152;
    }

    /**
     * Returns a randomly chosen node form the node table
     *
     * @return a randomly chosen node from the node table
     */
    private NodeInfo getRandomNode() {

        int id;
        NodeInfo n = null;

        while (n == null) {
            id = rnd.nextInt(NODE_TABLE_SIZE);
            n = nodeTable[id];
        }

        return n;
    }

    /**
     * Returns a new sync message with the current known nodes
     *
     * @return a new sync message
     */
    private String getSyncMessage() {

        StringBuilder sb = new StringBuilder();

        try {
            // Add header
            sb.append(SYNC_MESSAGE + separator);

            synchronized (this) {
                // Add known nodes
                for (NodeInfo n : nodeTable) {
                    if (n == null) continue;
                    sb.append(String.format("%d:%s:%d", n.getNodeId(), n.getHostName(), n.getPort())).append(separator);
                }
            }

            // Add self
            String hostname = InetAddress.getLocalHost().getHostName();
            sb.append(String.format("%d:%s:%d", this.nodeId, hostname, serverSocket.getLocalPort())).append(separator);

        } catch (UnknownHostException e) {
            System.err.println("The node is not connected to a network");
        }
        return sb.toString();
    }

    private void SendBroadcast(String msg, int hop, NodeInfo sender) {

        String bcmsg = getBroadcastMessage(hop, msg);

        if (msg.equals(lastBroadcastMessage)) return;
        lastBroadcastMessage = msg;

        for (NodeInfo n : nodeTable) {

            if (n == null || n.equals(sender)) continue;

            try (Socket s = new Socket(n.getHostName(), n.getPort());
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

                if (printBroadcastMessages) log(String.format("Broadcast to %d: %s", n.getNodeId(), bcmsg));
                out.println(bcmsg);

            } catch (ConnectException e) {
                log(n.getHostName() + " is not reachable any more.");
                removeNode(n);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getBroadcastMessage(int hop, String message) {

        StringBuilder sb = new StringBuilder();

        // add header
        sb.append(BROADCAST_MESSAGE + separator);

        // add sender
        sb.append(this.getNodeInfo().toString() + separator);

        // add hop count
        sb.append(hop);
        sb.append(separator);

        // add message
        sb.append(message + separator);

        return sb.toString();
    }

    private void processBroadcastMessage(String request) {

        final String[] parts = request.split(separator);

        NodeInfo sender = NodeInfo.Parse(parts[1]);

        int hop = Integer.parseInt(parts[2]);
        hop--;

        String msg = parts[3];

        if (msg.equals(lastBroadcastMessage)) return;

        if (hop >= 0) {
            SendBroadcast(msg, hop, sender);
        }

        log("Broadcast received: " + msg);

    }

    /**
     * Merges the current node table with the nodes form the sync response
     *
     * @param response the sync response to merge
     */
    private void processSyncResponse(String response) {

        String[] parts = response.split(separator);

        if (!parts[0].equals(SYNC_MESSAGE)) return;

        List<NodeInfo> nodes = new ArrayList<>();

        // Parse response
        for (int i = 1; i < parts.length; i++) {
            String[] node = parts[i].split(":");
            int id = Integer.parseInt(node[0]);
            String hostname = node[1];
            int port = Integer.parseInt(node[2]);
            nodes.add(new NodeInfo(id, hostname, port));
        }

        // remove self reference
        if (nodes.contains(this.getNodeInfo())) {
            nodes.remove(this.getNodeInfo());
        }

        // merge node table
        for (NodeInfo n : nodeTable) {
            if (nodes.contains(n)) continue;
            nodes.add(n);
        }

        synchronized (this) {
            // update node table with randomly chosen nodes
            for (int i = 0; i < NODE_TABLE_SIZE; i++) {
                if (nodes.isEmpty()) break;
                int ndx = rnd.nextInt(nodes.size());
                nodeTable[i] = nodes.remove(ndx);
            }
        }
    }

    /**
     * Logs the message to the std out.
     *
     * @param message the message to log.
     */
    private void log(String message) {
        System.out.printf("%d: %s\n", nodeId, message);
    }
}
