public class ProducerConsumerExample {
    public static void main(String[] args) {
        Drop drop = new Drop();
        new Thread(new Producer(drop), "producer").start();
        new Thread(new Consumer(drop), "consumer").start();
    }
}
