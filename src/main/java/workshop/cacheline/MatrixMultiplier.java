package workshop.cacheline;

public class MatrixMultiplier {
    static final int SIZE = 1024;
    static int[][] ARRAY_A = new int[SIZE][SIZE];
    static int[][] ARRAY_B = new int[SIZE][SIZE];

    public static void main(String[] args) {
        Object blackhole;
        while (!Thread.interrupted()) {
            var start = System.currentTimeMillis();
            int[][] ret = new int[SIZE][SIZE];

            // kji (slow), jik (faster), ikj (fastest)

            for (int i = 0; i < SIZE; i++) {
                for (int k = 0; k < SIZE; k++) {

            for (int j = 0; j < SIZE; j++) {


                        ret[i][j] += ARRAY_A[i][k] * ARRAY_B[k][j];
                    }
                }
            }
            System.out.println(System.currentTimeMillis() - start);
            blackhole = ret;
        }
    }

}
