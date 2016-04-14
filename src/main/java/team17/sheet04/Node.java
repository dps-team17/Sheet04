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
    private static final int NODE_TABLE_SIZE = 3;
    private static final String separator = ";";

    private int nodeId;
    private NodeInfo[] nodeTable;
    private ServerSocket serverSocket;
    private Random rnd;


    private Thread receiver;
    private Thread syncThread;


    public Node() {
        this(null);
    }

    public Node(NodeInfo syncPartner) {
        this.nodeId = NODES_CREATED++;

        // Init node table
        nodeTable = new NodeInfo[NODE_TABLE_SIZE];
        nodeTable[0] = syncPartner;

        createSyncThread();

        log("Node created");
    }


    @Override
    public void Connect() {

        log("Starting...");

        try {

            receiver = new Thread() {

                public void run() {

                    byte[] packetBuffer;
                    DatagramPacket packet;

                    try {
                        serverSocket = new ServerSocket(getRandomDynamicPort());

                        log("Listening on port " + serverSocket.getLocalPort());

                        while (true) {

                            try (Socket clientSocket = serverSocket.accept();
                                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            ) {
                                String request = in.readLine();
                                log("Request received: " + request);

                                String response = getSyncMessage();
                                log("Sending response: " + response);
                                out.println(getSyncMessage());

                                processSyncResponse(request);

                            } catch (IOException e) {
                                System.out.println("Exception caught when trying to listen on port "
                                        + serverSocket.getLocalPort() + " or listening for a connection");
                                System.out.println(e.getMessage());
                            }
                        }

                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            receiver.start();
            Thread.sleep(500);

            syncThread.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Disconnect() {

        if (receiver == null) throw new InvalidStateException("Node is not connected");

        try {
            serverSocket.close();

            receiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Send(String msg, NodeInfo recipient) {

        log("Sending: " + msg);
        byte[] buf = msg.getBytes();

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buf, buf.length, recipient.getAddress(), recipient.getPort());
            socket.send(packet);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public NodeInfo getNodeInfo() {
        try {
            return new NodeInfo(InetAddress.getLocalHost(), serverSocket.getLocalPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void createSyncThread() {

        syncThread = new Thread() {

            private final Object lock = new Object();
            private volatile boolean running = true;

            public void run() {

                while (!this.isInterrupted()) {
                    try {
                        int timeout = rnd.nextInt(10) * 1000;

                        synchronized (lock) {
                            lock.wait(timeout);
                        }

                        log("Try to sync");
                        if (this.isInterrupted()) break;

                        sync();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                log("Syncing stopped");
            }
        };


    }

    private void sync() {

        if (!hasKnownNodes()) {
            log("No sync partner found");
            return;
        }

        NodeInfo syncPartner = getRandomNode();

        log("Try to sync with " + syncPartner.getAddress().toString() + ":" + syncPartner.getPort());

        try (Socket s = new Socket(syncPartner.getAddress(), syncPartner.getPort());
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            // Send sync request
            String requestString = getSyncMessage();
            log("Sending request: " + requestString);
            out.println(requestString);


            // Wait for sync response
            final String response = in.readLine();
            log(response);
            processSyncResponse(response);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private int getRandomDynamicPort() {
        if (rnd == null) rnd = new Random(Thread.currentThread().getId());
        return rnd.nextInt(16383) + 49152;
    }

    private NodeInfo getRandomNode() {

        int id;
        NodeInfo n = null;

        while (n == null) {
            id = rnd.nextInt(NODE_TABLE_SIZE);
            n = nodeTable[id];
        }

        return n;
    }

    private String getSyncMessage() throws UnknownHostException {


        StringBuilder sb = new StringBuilder();

        // Add header
        sb.append("SYNC" + separator);

        // Add known nodes
        for (int i = 0; i < nodeTable.length; i++) {

            NodeInfo n = nodeTable[i];
            if (n == null) continue;

            sb.append(String.format("%s:%d", n.getAddress().getHostName(), n.getPort()) + separator);
        }

        // Add self
        sb.append(String.format("%s:%d", InetAddress.getLocalHost().getHostName(), serverSocket.getLocalPort()) + separator);

        return sb.toString();
    }

    private void processSyncResponse(String response) throws UnknownHostException {

        String[] parts = response.split(separator);

        if (!parts[0].equals("SYNC")) return;

        List<NodeInfo> nodes = new ArrayList<>();

        // Parse response
        for (int i = 1; i < parts.length; i++) {
            String[] node = parts[i].split(":");
            InetAddress address = InetAddress.getByName(node[0]);
            int port = Integer.parseInt(node[1]);
            nodes.add(new NodeInfo(address, port));
        }

        // remove self reference
        for (NodeInfo n : nodes) {
            if (n.getAddress().equals(serverSocket.getInetAddress()) && n.getPort() == serverSocket.getLocalPort()) {
                nodes.remove(n);
            }
        }

        // merge node table
        for (NodeInfo n:nodeTable) {
            if(nodes.contains(n)) continue;
            nodes.add(n);
        }

        // update node table with randomly chosen nodes
        for (int i = 0; i < NODE_TABLE_SIZE; i++) {
            int ndx = rnd.nextInt(nodes.size());
            nodeTable[i] = nodes.remove(ndx);
        }
    }

    private void log(String message) {
        System.out.printf("%d: %s\n", nodeId, message);
    }
}
