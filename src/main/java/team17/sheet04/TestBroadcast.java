package team17.sheet04;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by adanek on 14.04.2016.
 */
public class TestBroadcast {

    private static List<INode> nodes;
    private static Random rnd;
    public static void main(String[] args) throws InterruptedException {

        int n = 3; //INode.NODE_TABLE_SIZE * 3;
        nodes = new ArrayList<>();
        rnd =  new Random(Thread.currentThread().getId());

        for (int i = 0; i < n; i++){
            INode node = new Node(getRandomNodeInfo());
            node.Connect();
            nodes.add(node);
        }


        Thread.sleep(5000);

        INode n0 = nodes.get(0);
        n0.SendBroadcast("Important!");
    }

    private static NodeInfo getRandomNodeInfo(){

        if(nodes== null || nodes.isEmpty()) return null;

        int ndx = rnd.nextInt(nodes.size());

        return nodes.get(ndx).getNodeInfo();
    }
}
