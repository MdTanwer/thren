/**
 * Oracle SleepMessages — pause the current thread between prints.
 * sleep(4000) = request ~4 seconds pause (not guaranteed exact).
 */
public class SleepMessages {

    public static void main(String[] args) throws InterruptedException {
        String[] importantInfo = {
            "Mares eat oats",
            "Does eat oats",
            "Little lambs eat ivy",
            "A kid will eat ivy too"
        };

        for (int i = 0; i < importantInfo.length; i++) {
            Thread.sleep(4000); // current thread (main) pauses ~4s
            System.out.println(importantInfo[i]);
        }
    }
}
