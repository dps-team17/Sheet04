package team17.sheet04;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class TestBroadcast {

    private static List<INode> nodes;
    private static Random rnd;
    public static void main(String[] args) throws InterruptedException {

        int n = INode.NODE_TABLE_SIZE * 3;
        nodes = new ArrayList<>();
        rnd =  new Random(System.currentTimeMillis());

        for (int i = 0; i < n; i++){
            INode node = new Node(getRandomNodeInfo());
            node.Connect();
            nodes.add(node);
        }


        Thread.sleep(15000);

        INode n0 = nodes.get(0);

        VisualizeNodes viz = new VisualizeNodes(nodes, false);
        viz.start();

        Thread.sleep(100);

        n0.SendBroadcast("Important!");

    }

    private static NodeInfo getRandomNodeInfo(){

        if(nodes== null || nodes.isEmpty()) return null;

        int ndx = rnd.nextInt(nodes.size());

        return nodes.get(ndx).getNodeInfo();
    }
}
