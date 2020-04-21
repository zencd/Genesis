import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BotList {
    private Bot[] array;
    private int nextPos = 0;
    private Set<Integer> emptyIndexes = new HashSet<>();

    public BotList() {
        array = new Bot[1024];
    }

    public void add(Bot item) {
        int pos = getFreePos();
        tryIncrease(pos);
        array[pos] = item;
        item.clusterPos = pos;
    }

    public boolean remove(Bot item) {
        int i = item.clusterPos;
        array[i] = null;
        emptyIndexes.remove(i);
        return true;
    }

    public Bot[] getArray() {
        return array;
    }

    public int size() {
        return nextPos;
    }

    private int getFreePos() {
        final int index;
        Iterator<Integer> it = emptyIndexes.iterator();
        if (it.hasNext()) {
            index = it.next();
            it.remove();
        } else {
            index = nextPos++;
        }
        return index;
    }

    private void tryIncrease(int index) {
        if (index >= array.length) {
            Bot[] array2 = new Bot[array.length * 2];
            System.arraycopy(array, 0, array2, 0, array.length);
            array = array2;
        }
    }
}
