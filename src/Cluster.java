import java.awt.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Cluster {
    private static final long idCounter = 0;
    public final long id = idCounter + 1;
    private final World world;
    public long botsProcessed = 0;
    public long generation = 0;
    private Set<Bot> allBots = new HashSet<>();
    private Set<Bot> newBots = new HashSet<>();
    private Set<Bot> removedBots = new HashSet<>();
    private int size = 0;
    public final Rectangle rect;
    public final boolean leader;
    final String name;

    Cluster(World world, Rectangle rect, String name, boolean leader) {
        this.world = world;
        this.rect = rect;
        this.name = name;
        this.leader = leader;
        //Bot start = new Bot(world, this);
        //add(start);
    }

    void add(Bot bot) {
        //newBots.add(bot);
        //world.matrix[bot.x][bot.y] = bot;
        size++;
    }

    public void remove(Bot bot) {
        //removedBots.add(bot);
        world.matrix[bot.x][bot.y] = null;
        size--;
    }

    void mergeBots() {
        //allBots.removeAll(removedBots);
        //newBots.removeAll(removedBots);
        //allBots.addAll(newBots);
        //newBots.clear();
        //removedBots.clear();
    }

    public int size() {
        //return allBots.size() + newBots.size();
        return size;
    }

    @Deprecated
    Iterable<Bot> bots() {
        //return allBots;
        return new BotIterable();
    }

    private class BotIterable implements Iterable<Bot> {
        @Override
        public Iterator<Bot> iterator() {
            //List<Bot> bots = new ArrayList<>();
            //final int endX = rect.x + rect.width;
            //for (int x = rect.x; x < endX; x++) {
            //    final int endY = rect.y + rect.height;
            //    for (int y = rect.y; y < endY; y++) {
            //        Bot bot = world.matrix[x][y];
            //        if (bot != null) {
            //            bots.add(bot);
            //        }
            //    }
            //}
            //return bots.iterator();
            return new BotIterator();
        }
    }

    private class BotIterator implements Iterator<Bot> {
        private int nextX = rect.x;
        private int nextY = rect.y;

        @Override
        public boolean hasNext() {
            final int endX = rect.x + rect.width;
            final int endY = rect.y + rect.height;
            int x = nextX;
            int y = nextX;
            while (x < endX && y < endY) {

            }
            return false;
        }

        @Override
        public Bot next() {
            return null;
        }
    }
}
