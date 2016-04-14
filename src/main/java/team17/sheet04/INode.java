package team17.sheet04;


public interface INode {

    int NODE_TABLE_SIZE = 3;

    void Connect();

    void Disconnect();

    void Send(String msg, NodeInfo recipient);

    NodeInfo getNodeInfo();

    NodeInfo[] getKnownNodes();
}