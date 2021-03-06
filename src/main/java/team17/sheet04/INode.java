package team17.sheet04;

public interface INode {

	int NODE_TABLE_SIZE = 3;
	int HOP_COUNT = 5;
	int LOOKUP_TIMEOUT = 5000;

	String SYNC_MESSAGE = "SYNC";
	String BROADCAST_MESSAGE = "BROADCAST";
	String NODE_LOOKUP_REQUEST = "LOOKUP_REQUEST";
	String NODE_LOOKUP_RESPONSE = "LOOKUP_RESPONSE";

	void Connect();

	void Disconnect();

	void SendBroadcast(String msg);

	void SendLookup(int msg);

	NodeInfo getNodeInfo();

	NodeInfo[] getKnownNodes();

	void ShowSyncMessages(boolean value);

	void ShowLookupMessages(boolean value);

}