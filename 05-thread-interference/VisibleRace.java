/**
 * Slowed-down "c++" so you can SEE the three steps interleave.
 * Teaching model only — real c++ is faster but has the same 3-step race.
 */
public class VisibleRace {

    private int c = 0;

    /** Same three steps as c++, with pauses so another thread can sneak in. */
    public void incrementSlowRacy() {
        int local = c;                 // 1) retrieve
        try { Thread.sleep(50); } catch (InterruptedException e) { return; }
        local = local + 1;             // 2) increment
        try { Thread.sleep(50); } catch (InterruptedException e) { return; }
        c = local;                     // 3) store  ← other thread may overwrite
    }

    public int value() {
        return c;
    }

    public static void main(String[] args) throws InterruptedException {
        VisibleRace shared = new VisibleRace();

        Thread a = new Thread(() -> {
            System.out.println("[A] start increment");
            shared.incrementSlowRacy();
            System.out.println("[A] done, sees c=" + shared.value());
        }, "A");

        Thread b = new Thread(() -> {
            try { Thread.sleep(25); } catch (InterruptedException e) { return; }
            System.out.println("[B] start increment");
            shared.incrementSlowRacy();
            System.out.println("[B] done, sees c=" + shared.value());
        }, "B");

        a.start();
        b.start();
        a.join();
        b.join();

        System.out.println("Expected if both increments applied: 2");
        System.out.println("Actual: " + shared.value() + "  (often 1 — one update lost)");
    }
}
