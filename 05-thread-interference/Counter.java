/**
 * Unsafe counter — Oracle's example.
 * c++ is NOT one atomic step: read → add 1 → write.
 */
class Counter {
    private int c = 0;

    public void increment() {
        c++;
    }

    public void decrement() {
        c--;
    }

    public int value() {
        return c;
    }
}
