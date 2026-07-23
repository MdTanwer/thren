import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Standard library uses fork/join under the hood (Java 8+):
 * Arrays.parallelSort — good for large arrays on multi-core.
 */
public class ParallelSortDemo {

    public static void main(String[] args) {
        int n = 2_000_000;
        int[] a = ThreadLocalRandom.current().ints(n).toArray();
        int[] b = Arrays.copyOf(a, n);

        long t0 = System.currentTimeMillis();
        Arrays.sort(a);
        long seq = System.currentTimeMillis() - t0;

        t0 = System.currentTimeMillis();
        Arrays.parallelSort(b);
        long par = System.currentTimeMillis() - t0;

        System.out.println("Arrays.sort         ~" + seq + " ms");
        System.out.println("Arrays.parallelSort ~" + par + " ms (fork/join internally)");
        System.out.println("same result=" + Arrays.equals(a, b));
    }
}
