import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Cluster {
    private final World world;
    private Set<Bot> allBots = new HashSet<>();
    private Set<Bot> newBots = new HashSet<>();
    private Set<Bot> removedBots = new HashSet<>();

    Cluster(World world) {
        this.world = world;
        Bot start = new Bot(world, this);
        add(start);
    }

    void add(Bot bot) {
        newBots.add(bot);
    }

    public void remove(Bot bot) {
        removedBots.add(bot);
    }

    void mergeBots() {
        allBots.removeAll(removedBots);
        newBots.removeAll(removedBots);
        allBots.addAll(newBots);
        newBots.clear();
        removedBots.clear();
    }

    public int size() {
        return allBots.size() + newBots.size();
    }

    Iterable<Bot> bots() {
        return allBots;
    }
}
