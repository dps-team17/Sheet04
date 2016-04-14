package team17.sheet04;


import sun.plugin.dom.exception.InvalidStateException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Node implements INode {

    private static int NODES_CREATED = 0;
    private static final String separator = ";";

    private int nodeId;
    private NodeInfo[] nodeTable;
    private ServerSocket serverSocket;
    private Random rnd;


    private Thread listenerThread;
    private Thread syncThread;

    /**
     * Create a new node with an empty node table (not able to sync)
     */
    public Node() {
        this(null);
    }

    /**
     * Create a new node
     * @param syncPartner the node information of the initial sync partner
     */
    public Node(NodeInfo syncPartner) {
        this.nodeId = NODES_CREATED++;

        // Init node table
        nodeTable = new NodeInfo[NODE_TABLE_SIZE];
        nodeTable[0] = syncPartner;
        log("Node created");
    }

    @Override
    public void Connect() {

        log("Starting...");

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

        if (listenerThread == null) throw new InvalidStateException("Node is not connected");

        try {
            syncThread.interrupt();
            syncThread.join();

            serverSocket.close();
            listenerThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        log("Disconnected");
    }

    @Override
    public void Send(String msg, NodeInfo recipient) {

    }

    @Override
    public NodeInfo getNodeInfo() {
        try {
            return new NodeInfo(InetAddress.getLocalHost().getHostName(), serverSocket.getLocalPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public synchronized NodeInfo[] getKnownNodes() {
        return nodeTable.clone();
    }

    private void createSyncThread() {

        syncThread = new Thread() {

            private final Object lock = new Object();
            private volatile boolean running = true;

            public void run() {

                log("Start syncing...");

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

                log("Syncing stopped");
            }
        };
    }

    private void createListenerThread(){

        listenerThread = new Thread() {

            public void run() {

                byte[] packetBuffer;
                DatagramPacket packet;

                try {
                    serverSocket = new ServerSocket(getRandomDynamicPort());

                    log("Listening on port " + serverSocket.getLocalPort());

                    while (!serverSocket.isClosed()) {

                        try (Socket requester = serverSocket.accept()) {

                            processReqeust(requester);

                        } catch (SocketException e) {
                            // socket closed ignore
                        }
                    }

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                log("Stop listening");
            }
        };
    }

    private void processReqeust(Socket requester){

        try (PrintWriter out = new PrintWriter(requester.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(requester.getInputStream()));
        ) {
            // Process request
            String request = in.readLine();
            processSyncResponse(request);

            // Send response
            String response = getSyncMessage();
            out.println(getSyncMessage());

        } catch (IOException e) {
            // socket closed ignore
        }
    }

    private void sync() {

        if (!hasKnownNodes()) {
            log("No sync partner found");
            return;
        }

        NodeInfo syncPartner = getRandomNode();

        log("Try to sync with " + syncPartner.getHostName() + ":" + syncPartner.getPort());

        try (Socket s = new Socket(syncPartner.getHostName(), syncPartner.getPort());
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            // Send sync request
            String requestString = getSyncMessage();
            log("Sending request: " + requestString);
            out.println(requestString);

            // Wait for sync response
            s.setSoTimeout(5000);
            final String response = in.readLine();
            log("Received response: "+response);
            if(response != null){
                processSyncResponse(response);
            } else {
                log(syncPartner.getHostName() + " is not reachable any more.");
                removeNode(syncPartner);
            }

        } catch (ConnectException e){
            log(syncPartner.getHostName() + " is not reachable any more.");
            removeNode(syncPartner);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the node from the node table
     * @param syncPartner the node to remove
     */
    private synchronized void removeNode(NodeInfo syncPartner) {

        for(int i = 0; i< nodeTable.length; i++){
            if(nodeTable[i] != null && nodeTable[i].equals(syncPartner)){
                nodeTable[i] = null;
                break;
            }
        }
    }

    /**
     * Checks if the node has node in the node table
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
     * @return a randomly chosen dynamic port number
     */
    private int getRandomDynamicPort() {
        if (rnd == null) rnd = new Random(Thread.currentThread().getId());
        return rnd.nextInt(16383) + 49152;
    }

    /**
     * Returns a randomly chosen node form the node table
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
     * @return a new sync message
     */
    private String getSyncMessage()  {

        StringBuilder sb = new StringBuilder();

        try {
            // Add header
            sb.append("SYNC" + separator);

            synchronized (this) {
                // Add known nodes
                for (int i = 0; i < nodeTable.length; i++) {

                    NodeInfo n = nodeTable[i];
                    if (n == null) continue;

                    sb.append(String.format("%s:%d", n.getHostName(), n.getPort()) + separator);
                }
            }

            // Add self
            String hostname = InetAddress.getLocalHost().getHostName();
            sb.append(String.format("%s:%d", hostname, serverSocket.getLocalPort()) + separator);

        } catch (UnknownHostException e) {
            System.err.println("The node is not connected to a network");
        }
        return sb.toString();
    }

    /**
     * Merges the current node table with the nodes form the sync response
     * @param response the sync response to merge
    */
    private void processSyncResponse(String response){

        String[] parts = response.split(separator);

        if (!parts[0].equals("SYNC")) return;

        List<NodeInfo> nodes = new ArrayList<>();

        // Parse response
        for (int i = 1; i < parts.length; i++) {
            String[] node = parts[i].split(":");
            String hostname = node[0];
            int port = Integer.parseInt(node[1]);
            nodes.add(new NodeInfo(hostname, port));
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
     * @param message the message to log.
     */
    private void log(String message) {
        System.out.printf("%d: %s\n", nodeId, message);
    }
}
