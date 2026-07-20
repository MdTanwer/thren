/**
 * Oracle addName pattern: sync only the fields that need it,
 * not the whole method (nameList.add stays outside the lock).
 */
public class AddNameExample {

    private String lastName;
    private int nameCount;
    private final java.util.List<String> nameList = new java.util.ArrayList<>();

    public void addName(String name) {
        synchronized (this) {
            lastName = name;
            nameCount++;
        }
        nameList.add(name); // outside lock — other threads can add to list concurrently
    }

    public synchronized String snapshot() {
        return lastName + " count=" + nameCount + " listSize=" + nameList.size();
    }

    public static void main(String[] args) throws InterruptedException {
        AddNameExample book = new AddNameExample();

        Thread t1 = new Thread(() -> book.addName("Alice"), "t1");
        Thread t2 = new Thread(() -> book.addName("Bob"), "t2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println(book.snapshot());
    }
}
