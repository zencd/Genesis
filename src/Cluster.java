import java.awt.*;

public final class Cluster {
    private static long idCounter = 0;
    public final long id = idCounter++;
    public final World world;
    public final Rectangle rect;
    public final boolean leader;
    public volatile boolean ready = false;

    private double[] randMemory; // массив предгенерированных случайных чисел
    private int randIdx = 0;

    public Cluster(World world, Rectangle rect, boolean leader) {
        this.world = world;
        this.rect = rect;
        this.leader = leader;
        initRandAsync();
    }

    public double rand() {
        int i = this.randIdx + 1;
        if (i >= randMemory.length) {
            i = 0;
        }
        this.randIdx = i;
        return randMemory[i];
    }

    private void initRandAsync() {
        assert this.randMemory == null;
        new Thread(() -> {
            this.randMemory = Utils.makePreCalcRandom();
            this.ready = true;
        }).start();
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id=" + id +
                ", rect=" + rect +
                ", leader=" + leader +
                '}';
    }
}
