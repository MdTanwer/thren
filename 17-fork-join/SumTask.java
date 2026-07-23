import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * RecursiveTask — returns a result (sum of array range).
 * Shows the classic fork/join pattern with a return value.
 */
public class SumTask extends RecursiveTask<Long> {

    private static final int THRESHOLD = 10_000;

    private final long[] data;
    private final int start;
    private final int end; // exclusive

    public SumTask(long[] data, int start, int end) {
        this.data = data;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        int length = end - start;
        if (length <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += data[i];
            }
            return sum;
        }
        int mid = start + length / 2;
        SumTask left = new SumTask(data, start, mid);
        SumTask right = new SumTask(data, mid, end);
        left.fork();                 // async on another worker (work-stealing)
        long rightResult = right.compute(); // compute this half here
        long leftResult = left.join();      // wait for forked half
        return leftResult + rightResult;
    }

    public static void main(String[] args) {
        int n = 1_000_000;
        long[] data = new long[n];
        for (int i = 0; i < n; i++) {
            data[i] = i + 1;
        }

        long expected = (long) n * (n + 1) / 2;
        ForkJoinPool pool = new ForkJoinPool();
        long actual = pool.invoke(new SumTask(data, 0, n));

        System.out.println("expected=" + expected);
        System.out.println("actual  =" + actual);
        System.out.println("match=" + (expected == actual));
        pool.shutdown();
    }
}
