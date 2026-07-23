import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ConcurrentMap / ConcurrentHashMap — atomic putIfAbsent, replace, remove(key,value).
 * Safer than HashMap under concurrent access; no external sync needed for these ops.
 */
public class ConcurrentMapDemo {

    public static void main(String[] args) throws InterruptedException {
        ConcurrentMap<String, Integer> scores = new ConcurrentHashMap<>();

        Runnable bumpAlice = () -> {
            for (int i = 0; i < 10_000; i++) {
                scores.merge("alice", 1, Integer::sum); // atomic update
            }
        };

        Thread t1 = new Thread(bumpAlice);
        Thread t2 = new Thread(bumpAlice);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("alice score=" + scores.get("alice") + " (expect 20000)");

        // Atomic "add only if absent"
        Integer prev = scores.putIfAbsent("bob", 5);
        System.out.println("putIfAbsent bob first=" + prev + " map=" + scores.get("bob"));
        prev = scores.putIfAbsent("bob", 99);
        System.out.println("putIfAbsent bob again prev=" + prev + " (still " + scores.get("bob") + ")");

        // Replace only if current value matches
        boolean replaced = scores.replace("bob", 5, 50);
        System.out.println("replace bob 5→50=" + replaced + " now=" + scores.get("bob"));
    }
}
