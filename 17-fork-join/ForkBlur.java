import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Oracle-style ForkBlur (RecursiveAction — no return value).
 * Blurs an int[] of "pixels" by averaging a window; splits work recursively.
 */
public class ForkBlur extends RecursiveAction {

    private final int[] mSource;
    private final int mStart;
    private final int mLength;
    private final int[] mDestination;
    private final int mBlurWidth = 15;

    protected static final int sThreshold = 10_000;

    public ForkBlur(int[] src, int start, int length, int[] dst) {
        mSource = src;
        mStart = start;
        mLength = length;
        mDestination = dst;
    }

    protected void computeDirectly() {
        int sidePixels = (mBlurWidth - 1) / 2;
        for (int index = mStart; index < mStart + mLength; index++) {
            float rt = 0, gt = 0, bt = 0;
            for (int mi = -sidePixels; mi <= sidePixels; mi++) {
                int mindex = Math.min(Math.max(mi + index, 0), mSource.length - 1);
                int pixel = mSource[mindex];
                rt += (float) ((pixel & 0x00ff0000) >> 16) / mBlurWidth;
                gt += (float) ((pixel & 0x0000ff00) >> 8) / mBlurWidth;
                bt += (float) ((pixel & 0x000000ff) >> 0) / mBlurWidth;
            }
            int dpixel = (0xff000000)
                    | (((int) rt) << 16)
                    | (((int) gt) << 8)
                    | (((int) bt) << 0);
            mDestination[index] = dpixel;
        }
    }

    @Override
    protected void compute() {
        if (mLength < sThreshold) {
            computeDirectly();
            return;
        }
        int split = mLength / 2;
        invokeAll(
                new ForkBlur(mSource, mStart, split, mDestination),
                new ForkBlur(mSource, mStart + split, mLength - split, mDestination));
    }

    public static void main(String[] args) {
        int n = 100_000;
        int[] src = new int[n];
        int[] dst = new int[n];
        for (int i = 0; i < n; i++) {
            // simple gradient pixel
            int v = (i * 255 / n) & 0xff;
            src[i] = 0xff000000 | (v << 16) | (v << 8) | v;
        }

        ForkBlur task = new ForkBlur(src, 0, src.length, dst);
        ForkJoinPool pool = new ForkJoinPool(); // uses available processors
        long t0 = System.currentTimeMillis();
        pool.invoke(task);
        long ms = System.currentTimeMillis() - t0;

        System.out.println("blurred " + n + " pixels in ~" + ms + " ms");
        System.out.println("pool parallelism=" + pool.getParallelism());
        System.out.printf("sample dst[0]=0x%08X dst[mid]=0x%08X%n",
                dst[0], dst[n / 2]);
        pool.shutdown();
    }
}
