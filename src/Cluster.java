import java.awt.*;

/**
 * Определяет кластер (кусок) карты с населяющими его ботами.
 * Введено для многопоточной обработки.
 */
public final class Cluster {
    private static long idCounter = 0;
    public final long id = idCounter++;
    public final World world;
    public final Rectangle rect;
    public final boolean leader; // true для кластера сшивающего/синхронизирующего мир + он сейчас отвечает за отрисовку
    public final boolean superLeader;
    public volatile boolean ready = false; // true если готов к использованию

    private double[] randMemory; // массив предгенерированных случайных чисел
    private int randIdx = 0;

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
}
