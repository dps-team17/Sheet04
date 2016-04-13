package team17.sheet04;

import java.net.InetAddress;

/**
 * This class represents the necessary information to connect to a node
 */
public class NodeInfo {

    private InetAddress address;
    private int port;

    /**
     * Creates a new instance of an node info
     * @param address the nodes address
     * @param port the nodes port number
     */
    public NodeInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * Gets the nodes port number
     * @return the port number on which the node is listening
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the InetAdress of the node
     * @return the InetAdress of the node
     */
    public InetAddress getAddress() {
        return address;
    }
}
