package falsesharing;


import java.util.function.LongFunction;

public class FalseSharingExample {
    public static void main(String[] args) {

        Counter counter1 = new Counter();

        run("false sharing", counter1, counter1); // twice counter1
        run("no false sharing", new Counter(), new Counter()); // different instances passed
    }

    private static void run(String prefix, Counter counter1, Counter counter2) {
        long iterations = 1_000_000_000;
        LongFunction<String> message = time -> prefix + " total time: " + time;

        Thread thread1 = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for(long i=0; i<iterations; i++) {
                counter1.count1++;
            }
            long endTime = System.currentTimeMillis();
            System.out.println(message.apply(endTime - startTime));
        });
        Thread thread2 = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for(long i=0; i<iterations; i++) {
                counter2.count2++;
            }
            long endTime = System.currentTimeMillis();
            System.out.println(message.apply(endTime - startTime));
        });

        thread1.start();
        thread2.start();
    }
}