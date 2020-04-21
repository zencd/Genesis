import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Cluster {
    final Set<Bot> bots = new LinkedHashSet<>();
    //private final BotList bots = new BotList();

    void add(Bot bot) {
        bots.add(bot);
    }

    public void remove(Bot bot) {
        boolean ok = bots.remove(bot);
        //if (!ok) System.err.println("bot wasn't found: " + bot);
    }

    public int size() {
        return bots.size();
    }

    public Bot[] getArray() {
        return bots.toArray(new Bot[0]);
        //return bots.getArray();
    }
}
