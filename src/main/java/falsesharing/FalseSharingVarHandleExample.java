package falsesharing;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Random;

public class FalseSharingVarHandleExample {

    private static final VarHandle VH = MethodHandles.arrayElementVarHandle(int[].class);
    private static final int SIZE = 1024 * 1024 * 128;
    private static final int[] ARRAY_A = new int[SIZE];
    private static final int THREADS = 6;
    private static int[] results = new int[THREADS];


    static {
        Random random = new Random();
        for (int i = 0; i < SIZE; i++) {
            ARRAY_A[i] = random.nextInt();
        }
    }
    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        while (!Thread.interrupted()) {
            for (int i =0; i < THREADS; i++) {
                threads[i] = new Thread(create(THREADS, i));
            }
            long start = System.currentTimeMillis();
            for (int i = 0; i < THREADS; i++) {
                threads[i].start();
            }
            for (int i = 0; i < THREADS; i++) {
                threads[i].join();
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    private static Runnable create(int step, int offset) {
        return () -> {
            for (int i = offset; i < ARRAY_A.length; i += step) {
                if (ARRAY_A[i] % 2 == 0) {
                    results[offset]++;
//                    VH.setVolatile(results, offset, results[offset] + 1);
                }
            }
        };
    }
}
