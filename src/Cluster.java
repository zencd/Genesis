import java.awt.*;

public final class Cluster {
    private static long idCounter = 0;
    public final long id = idCounter++;
    public final World world;
    public final Rectangle rect;
    public final boolean leader;

    public Cluster(World world, Rectangle rect, boolean leader) {
        this.world = world;
        this.rect = rect;
        this.leader = leader;
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
