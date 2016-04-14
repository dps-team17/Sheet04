package team17.sheet04;


public class TestClient {


    public static void main(String[] args) throws InterruptedException {

        Node n1 = new Node();
        n1.Connect();


        Thread.sleep(500);

        INode n2 = new Node(n1.getNodeInfo());
        n2.Connect();
    }
}
