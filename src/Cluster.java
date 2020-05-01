import java.awt.*;

public final class Cluster {
    public final World world;
    public final Rectangle rect;
    public final boolean leader;

    public Cluster(World world, Rectangle rect, boolean leader) {
        this.world = world;
        this.rect = rect;
        this.leader = leader;
    }
}
