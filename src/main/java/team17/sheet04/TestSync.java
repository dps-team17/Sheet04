package team17.sheet04;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestSync {

	private static List<INode> nodes;
	private static Random rnd;

	public static void main(String[] args) throws InterruptedException {

		int n = INode.NODE_TABLE_SIZE * 3;
		nodes = new ArrayList<>();
		rnd = new Random(Thread.currentThread().getId());

		for (int i = 0; i < n; i++) {
			INode node = new Node(getRandomNodeInfo());
			node.ShowSyncMessages(false);
			node.Connect();
			nodes.add(node);
		}

		VisualizeNodes viz = new VisualizeNodes(nodes, true);
		viz.start();

		Thread.sleep(8000);
		final INode n1 = nodes.get(0);
		n1.Disconnect();

		Thread.sleep(5000);
		INode n3 = new Node(getRandomNodeInfo());
		n3.Connect();
		nodes.add(n3);
		viz.actualizeNodes(nodes);

		Thread.sleep(10000);
		//n1.Connect();

	}

	private static NodeInfo getRandomNodeInfo() {

		if (nodes == null || nodes.isEmpty())
			return null;

		int ndx = rnd.nextInt(nodes.size());

		return nodes.get(ndx).getNodeInfo();
	}
}
