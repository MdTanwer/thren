package ch1;

public class Practice {
    public static void main(String[] args) {
       

       

        // thread sleep 
 Runnable runnable2 = new Runnable() {
    @Override
    public void run(){
        String threadName = Thread.currentThread().getName();
        int count =0;
        while(!Thread.currentThread().isInterrupted()){
            System.out.println(threadName + ": " + count++);
        }
    }
 };

 Thread thread2 = new Thread(runnable2);
 Thread thread3 = new Thread(runnable2);
  try {
    Thread.sleep(1000);
   
  } catch (InterruptedException e) {
    e.printStackTrace();
  }



  thread2.interrupt();
  thread3.interrupt();
    }
}
