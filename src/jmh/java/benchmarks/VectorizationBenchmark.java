package benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class VectorizationBenchmark {

    static int SIZE = 1024;
    static int[] ARRAY_A = new int[SIZE];
    static int[] ARRAY_B = new int[SIZE];

    @Benchmark
    public void step_by_1() {
        int[] localSum = new int[SIZE];
        for (int j = 0; j < SIZE * SIZE; j++) {
            for (int i = 0; i < SIZE; i += 1) {
                localSum[i] = ARRAY_A[i] + ARRAY_B[i];
            }
        }
    }

    @Benchmark
    public void step_by_2() {
        int[] localSum = new int[SIZE];
        for (int j = 0; j < SIZE * SIZE; j++) {
            for (int i = 0; i < SIZE; i += 2) {
                localSum[i] = ARRAY_A[i] + ARRAY_B[i];
            }
        }
    }

}
