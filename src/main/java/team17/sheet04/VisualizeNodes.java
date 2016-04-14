package team17.sheet04;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizeNodes extends Thread {

	private List<INode> nodes;
	private int amountNodes;
	private boolean matrix[][];
	private Integer[] nodeNames;
	private boolean endless = true;

	private ConcurrentHashMap<Integer, Integer> nodeMap = new ConcurrentHashMap<>();

	public VisualizeNodes(List<INode> nodes, boolean endless) {
		this.nodes = new LinkedList<INode>(nodes);
		this.endless = endless;
		amountNodes = nodes.size();
		matrix = new boolean[amountNodes][amountNodes];
		int i = 0;
		nodeNames = new Integer[amountNodes];
		for (INode n : nodes) {
			nodeNames[i] = n.getNodeInfo().getNodeId();
			nodeMap.put(n.getNodeInfo().getNodeId(), i++);
		}
		for (i = 0; i < amountNodes; i++) {
			for (int j = 0; j < amountNodes; j++) {
				matrix[i][j] = false;
			}
		}
	}

	public void actualizeNodes(List<INode> nodes) {
		synchronized (this.nodes) {
			this.nodes = new LinkedList<INode>(nodes);
			amountNodes = nodes.size();
			matrix = new boolean[amountNodes][amountNodes];
			int i = 0;
			nodeNames = new Integer[amountNodes];
			for (INode n : nodes) {
				nodeNames[i] = n.getNodeInfo().getNodeId();
				nodeMap.put(n.getNodeInfo().getNodeId(), i++);
			}
			for (i = 0; i < amountNodes; i++) {
				for (int j = 0; j < amountNodes; j++) {
					matrix[i][j] = false;
				}
			}
		}
	}

	@Override
	public void run() {
		do {
			synchronized (this.nodes) {
				for (INode n : nodes) {
					int index1 = nodeMap.get(n.getNodeInfo().getNodeId());
					NodeInfo[] nodeInfos = n.getKnownNodes();
					for (NodeInfo info : nodeInfos) {
						if (info != null) {
							int index2 = nodeMap.get(info.getNodeId());
							matrix[index1][index2] = true;
						}
					}
				}

				System.out.println("Connection Table:");
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

			}

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				interrupt();
			}
		} while (endless && !isInterrupted());

	}

}
