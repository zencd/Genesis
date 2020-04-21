import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Cluster {
    final Set<Bot> bots = new LinkedHashSet<>();

    void add(Bot bot) {
        bots.add(bot);
    }

    public void remove(Bot bot) {
        bots.remove(bot);
    }
}
