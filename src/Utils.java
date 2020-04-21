import java.util.Random;

public class Utils {
    public static void joinSafe(Thread thread) {
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static double[] makePreCalcRandom() {
        return makePreCalcRandom(new Random());
    }

    public static double[] makePreCalcRandom(Random random) {
        double[] buf = new double[1000000];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = random.nextDouble();
        }
        return buf;
    }
}
