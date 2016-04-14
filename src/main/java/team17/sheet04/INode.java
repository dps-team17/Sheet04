package team17.sheet04;


public interface INode {

    void Connect();

    void Disconnect();

    void Send(String msg, NodeInfo recipient);

    NodeInfo getNodeInfo();

    //NodeInfo[] getKnownNodes();
}