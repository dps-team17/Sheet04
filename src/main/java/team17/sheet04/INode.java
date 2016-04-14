package team17.sheet04;


public interface INode {

    int NODE_TABLE_SIZE = 3;
    int HOP_COUNT = 5;

    String SYNC_MESSAGE = "SYNC";
    String BROADCAST_MESSAGE = "BROADCAST";

    void Connect();

    void Disconnect();

    void SendBroadcast(String msg);

    NodeInfo getNodeInfo();

    NodeInfo[] getKnownNodes();

    void ShowSyncMessages(boolean value);

}