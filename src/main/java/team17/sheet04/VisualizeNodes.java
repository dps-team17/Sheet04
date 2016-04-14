package team17.sheet04;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizeNodes extends Thread {

	private List<INode> nodes;
	private int amountNodes;
	private boolean matrix[][];
	private String[] nodeNames;

	private ConcurrentHashMap<String, Integer> nodeMap = new ConcurrentHashMap<>();

	public VisualizeNodes(List<INode> nodes) {
		this.nodes = new LinkedList<INode>(nodes);
		amountNodes = nodes.size();
		matrix = new boolean[amountNodes][amountNodes];
		int i = 0;
		nodeNames = new String[amountNodes];
		for (INode n : nodes) {
			nodeNames[i] = n.getNodeInfo().getHostName();
			nodeMap.put(nodeNames[i] + n.getNodeInfo().getPort(), i++);
		}
		for (i = 0; i < amountNodes; i++) {
			for (int j = 0; j < amountNodes; j++) {
				matrix[i][j] = false;
			}
		}
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			for (INode n : nodes) {
				int index1 = nodeMap.get(n.getNodeInfo().getHostName() + n.getNodeInfo().getPort());
				NodeInfo[] nodeInfos = n.getKnownNodes();
				for (NodeInfo info : nodeInfos) {
					if (info != null) {
						int index2 = nodeMap.get(info.getHostName() + info.getPort());
						matrix[index1][index2] = true;
					}
				}
			}
			System.out.print("\t");
			for (int i = 0; i < amountNodes; i++) {
				System.out.print(nodeNames[i] + "\t");
			}
			System.out.println();
			for (int i = 0; i < amountNodes; i++) {
				System.out.print(nodeNames[i] + "\t");
				for (int j = 0; j < amountNodes; j++) {
					System.out.print((matrix[i][j] ? "X" : " ") + "\t");
					matrix[i][j] = false;
				}
				System.out.println();
			}

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				interrupt();
			}
		}

	}

}
