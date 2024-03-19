package benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LoopOrderBenchmark {
    static final int SIZE = 1024;
    static int[][] ARRAY_A = new int[SIZE][SIZE];
    static int[][] ARRAY_B = new int[SIZE][SIZE];

    @Benchmark
    public void kji() {
        int[][] ret = new int[SIZE][SIZE];

        for (int k = 0; k < SIZE; k++)
            for (int j = 0; j < SIZE; j++)
                for (int i = 0; i < SIZE; i++)
                    ret[i][j] += ARRAY_A[i][k] * ARRAY_B[k][j];
    }

    @Benchmark
    public void jik() {
        int[][] ret = new int[SIZE][SIZE];

        for (int j = 0; j < SIZE; j++)
            for (int i = 0; i < SIZE; i++)
                for (int k = 0; k < SIZE; k++)
                    ret[i][j] += ARRAY_A[i][k] * ARRAY_B[k][j];
    }

    @Benchmark
    public void ikj() {
        int[][] ret = new int[SIZE][SIZE];

        for (int i = 0; i < SIZE; i++)
            for (int k = 0; k < SIZE; k++)
                for (int j = 0; j < SIZE; j++)
                    ret[i][j] += ARRAY_A[i][k] * ARRAY_B[k][j];
    }
}