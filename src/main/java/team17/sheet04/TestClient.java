package team17.sheet04;

/**
 * Created by adanek on 12.04.2016.
 */
public class TestClient {


    public static void main(String[] args){

        Node n1 = new Node();
        Node n2 = new Node(new NodeInfo(n1.getIpAddress(), n1.getPortNumber()));

        n1.activate();
        n2.activate();
    }
}
