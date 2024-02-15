package workshop.falsesharing;


public class FalseSharingExample {
    public static void main(String[] args) throws InterruptedException {

        Counter counter1 = new Counter();

        Test.run("false sharing", counter1, counter1); // twice counter1
        Test.run("no false sharing", new Counter(), new Counter()); // different instances passed
    }
}