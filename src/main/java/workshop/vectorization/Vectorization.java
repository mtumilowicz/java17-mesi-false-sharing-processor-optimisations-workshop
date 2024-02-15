package workshop.vectorization;

public class Vectorization {

    static int SIZE = 1024;
    static int[] ARRAY_A = new int[SIZE];
    static int[] ARRAY_B = new int[SIZE];

    public static void main(String[] args) {
        Object blackhole;
        while(!Thread.interrupted()) {
            int[] localSum = new int[SIZE];
            long startTime = System.currentTimeMillis();
            for (int j = 0; j < SIZE * SIZE; j++) {
                for (int i = 0; i < SIZE; i += 1) {
                    localSum[i] = ARRAY_A[i] + ARRAY_B[i];
                }
            }
            System.out.println(System.currentTimeMillis() - startTime);
            blackhole = localSum;
        }
    }
}
