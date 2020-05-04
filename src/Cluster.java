import java.awt.*;
import java.util.LinkedList;

/**
 * Определяет кластер (кусок) карты с населяющими его ботами.
 * Введено для многопоточной обработки.
 */
public final class Cluster implements Consts {
    private static long idCounter = 0;
    public final long id = idCounter++;
    public final World world;
    public final Rectangle rect;
    public final boolean leader; // true для кластера сшивающего/синхронизирующего мир + он сейчас отвечает за отрисовку
    public final boolean superLeader;
    public volatile boolean ready = false; // true если готов к использованию

    private double[] randMemory; // массив предгенерированных случайных чисел
    private int randIdx = 0;

    private Bot first;

    final LinkedList<Bot> bots = new LinkedList<>();

    public Cluster(World world, Rectangle rect, boolean leader, boolean superLeader) {
        if (superLeader && !leader) {
            throw new IllegalStateException("superLeader assigned incorrectly");
        }
        this.world = world;
        this.rect = rect;
        this.leader = leader;
        this.superLeader = superLeader;
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
        // генерация рандомных чисел долна быть у каждого кластера своя - во избежание необходимости синхронизации и конфликтов
        // но для 32 потоков это долгий процесс, ждать которого не нужно, т.к. между нажатием кнопки сгенерировать карту
        // и нажатием кнопки "старт" проходит достаточно времени чтобы эта операция успела выполниться фоном
        // поэтому используется рабочий поток
        assert this.randMemory == null;
        Utils.startAndJoinThread(() -> {
            this.randMemory = Utils.makePreCalcRandom();
        });
        this.ready = true;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id=" + id +
                ", rect=" + rect +
                ", leader=" + leader +
                '}';
    }

    public void add(Bot bot) {
        if (NUM_WORKERS > 1) {
            assert !bots.contains(bot);
            bots.add(bot);
            bot.cluster = this;
        }
    }

    public void remove(Bot bot) {
        if (NUM_WORKERS > 1) {
            assert bot.cluster == this;
            boolean contains = bots.contains(bot);
            boolean ok = bots.remove(bot);
            if (!ok) {
                System.err.println("bot: " + bot);
                System.err.println("bot: " + bot.hashCode());
                int stop = 0;
            }
            assert ok;
            bot.cluster = null;
        }
    }

    //public void add(Bot bot) {
    //    assert bot != null;
    //    bot.cluster = this;
    //    if (first == null) {
    //        first = bot;
    //        first.nextBot = first;
    //        first.prevBot = first;
    //    } else {
    //        final Bot left = first.prevBot;
    //        final Bot right = first;
    //        if (left == null) {
    //            int stop = 0;
    //        }
    //        left.nextBot = bot;
    //        right.prevBot = bot;
    //        bot.prevBot = left;
    //        bot.nextBot = right;
    //    }
    //    assert bot.prevBot.cluster == bot.cluster;
    //    assert bot.nextBot.cluster == bot.cluster;
    //}
    //
    //public void remove(Bot bot) {
    //    assert bot != null;
    //    final Bot left = bot.prevBot;
    //    final Bot right = bot.nextBot;
    //    //assert left.cluster == bot.cluster;
    //    //assert right.cluster == bot.cluster;
    //    if (left == null) {
    //        int stop = 0;
    //    }
    //    if (right == null) {
    //        int stop = 0;
    //    }
    //    left.nextBot = right;
    //    right.prevBot = left;
    //    bot.nextBot = null;
    //    bot.prevBot = null;
    //    bot.cluster = null;
    //    if (first == bot) {
    //        if (first == right) {
    //            first = null;
    //        } else {
    //            first = right;
    //        }
    //    }
    //}
}
