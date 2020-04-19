import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BotCluster {
    public final Bot[][] matrix;
    public final Rectangle rect;
    //Set<Bot> bots = new HashSet<>();

    public BotCluster(World world, Rectangle rect) {
        this.matrix = world.matrix;
        this.rect = rect;
    }

    @Deprecated
    public void add(Bot bot) {
        //bots.add(bot);
    }

    @Deprecated
    public List<Bot> getBots() {
        List<Bot> bots = new ArrayList<>();
        for (int i = 0; i < rect.width; i++) {
            for (int j = 0; j < rect.height; j++) {
                Bot bot = this.matrix[i][j];
                if (bot != null) {
                    bots.add(bot);
                }
            }
        }
        return bots;
    }
}
