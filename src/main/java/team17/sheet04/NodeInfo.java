package team17.sheet04;

import java.net.InetAddress;

/**
 * This class represents the necessary information to connect to a node
 */
public class NodeInfo {

    private String hostname;
    private int port;
    private int nodeId;

    /**
     * Creates a new instance of an node info
     *
     * @param id the id of the node
     * @param hostname the hostname of the node
     * @param port    the nodes port number
     */
    public NodeInfo(int id, String hostname, int port) {
        this.nodeId = id;
        this.hostname = hostname;
        this.port = port;
    }

    public int getNodeId(){
        return nodeId;
    }


    /**
     * Gets the nodes port number
     *
     * @return the port number on which the node is listening
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the InetAdress of the node
     *
     * @return the InetAdress of the node
     */
    public String getHostName() {

        return hostname;
    }

    @Override
    public int hashCode() {
        return getHostName().hashCode() + getPort();
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof NodeInfo))
            return false;
        if (obj == this)
            return true;

        NodeInfo other = (NodeInfo) obj;

        return this.getHostName().equals(other.getHostName()) && this.getPort() == other.getPort();
    }
}
