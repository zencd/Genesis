import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Cluster {
    //final Set<Bot> bots = new LinkedHashSet<>();
    //private final BotList bots = new BotList();

    private final World world;
    private final Bot start;
    private int size = 0;

    Cluster(World world) {
        this.world = world;
        start = new Bot(world);
        start.nextBot = start;
        start.prevBot = start;
    }

    void add(Bot bot) {
        add(bot, null);
    }

    void add(Bot bot, Bot before) {
        //if (start == null) {
        //    start = bot;
        //    start.nextBot = bot;
        //    start.prevBot = bot;
        //}

        if (before != null) {
            addBefore(bot, before);
        } else {
            addBefore(bot, start.nextBot); // NB "start.nextBot" to prevent infinite processing of a cluster
        }

        size++;
    }

    private static void addAfter(Bot add, Bot after) {
        Bot oldNext = after.nextBot;
        after.nextBot = add;
        add.prevBot = after;
        add.nextBot = oldNext;
        oldNext.prevBot = add;
    }

    private static void addBefore(Bot add, Bot before) {
        Bot oldPrev = before.prevBot;
        before.prevBot = add;
        add.nextBot = before;
        add.prevBot = oldPrev;
        oldPrev.nextBot = add;
    }

    public void remove(Bot bot) {
        Bot left = bot.prevBot;
        Bot right = bot.nextBot;
        left.nextBot = right;
        right.prevBot = left;
        size--;
        //boolean ok = bots.remove(bot);
        //if (!ok) System.err.println("bot wasn't found: " + bot);
    }

    public int size() {
        return size;
    }

    public Bot getStart() {
        assert start != null;
        return start;
    }

    public Bot[] getArray() {
        throw new RuntimeException("deprecated");
    //    return bots.toArray(new Bot[0]);
    //    //return bots.getArray();
    }
}
