/**
 * Fix: always acquire locks in the same global order (e.g. by name).
 * Prevents circular wait — no deadlock.
 */
public class DeadlockFixed {

    static class Friend {
        private final String name;

        public Friend(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /** Bow using consistent lock ordering on both Friend objects. */
        public void bow(Friend bower) {
            Friend first = name.compareTo(bower.name) < 0 ? this : bower;
            Friend second = name.compareTo(bower.name) < 0 ? bower : this;

            synchronized (first) {
                synchronized (second) {
                    System.out.format("%s: %s has bowed to me!%n", name, bower.name);
                    System.out.format("%s: %s has bowed back to me!%n", bower.name, name);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Friend alphonse = new Friend("Alphonse");
        Friend gaston = new Friend("Gaston");

        Thread t1 = new Thread(() -> alphonse.bow(gaston));
        Thread t2 = new Thread(() -> gaston.bow(alphonse));

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Both bows completed — no deadlock");
    }
}
