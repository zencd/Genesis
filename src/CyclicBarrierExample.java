import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierExample {
    CyclicBarrier cb;
    int count = 0;

    public static void main(String[] args) {
        new CyclicBarrierExample().main();
    }

    private void main() {
        cb = new CyclicBarrier(3, () -> {
            System.out.println("synking " + this);
        });
        for (int i = 0; i < 3; i++) {
            new Thread(new Worker()).start();
        }
    }

    class Worker implements Runnable {
        @Override
        public void run() {
            while (true) {
                doSomeWork();
                Utils.await(cb);
            }
        }

        private void doSomeWork() {
            System.out.println("Doing some work ");
        }
    }
}
