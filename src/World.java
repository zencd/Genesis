import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

/**
 * Движок симуляции.
 */
public final class World implements Consts {

    private static final int CLUSTER_GRAN = 50;

    int width;
    int height;
    int zoom = 1;
    int sealevel = SEA_LEVEL_DEFAULT;
    int[][] map;    //Карта мира
    int[] mapInGPU;    //Карта для GPU
    Bot[][] matrix;    //Матрица мира
    Bot zerobot = makeZeroBot(this);
    Bot currentbot;
    int generation;
    int population;
    int organic;
    int perlinValue = PERLIN_DEFAULT;
    private long lastTimePainted = 0;

    private List<Cluster> allClusters;
    private Map<Integer,List<Cluster>> clusterByX;
    private List<Cluster> leaders;

    private List<Thread> workers = null;
    private CyclicBarrier barrier;
    private boolean started = false; // Флаг работы потока, если false  поток заканчивает работу

    private final Supplier<Long> mapSeeder; // возвращает seed только для построения карты
    private final double[] randMemory; // массив предгенерированных случайных чисел
    private int randIdx = 0;

    public World(double[] randMemory, Supplier<Long> mapSeeder) {
        this.randMemory = randMemory;
        this.mapSeeder = mapSeeder;
    }

    @Deprecated
    static Bot makeZeroBot(World world) {
        // todo the zero bot should be eliminated
        return new Bot(world, null);
    }

    public boolean isStarted() {
        return started;
    }

    public void mapGenerationInitiated(int canvasWidth, int canvasHeight) {
        width = canvasWidth / zoom;    // Ширина доступной части экрана для рисования карты
        height = canvasHeight / zoom;
        initClusters();
        generateMap(mapSeeder.get());
        generateAdam();
    }

    private void initClusters() {
        final List<Cluster> allClusters = new ArrayList<>();
        final List<Cluster> leaders = new ArrayList<>();
        final int thr = 2;
        final int gap = 5;
        final int width1 = width / 2;
        final int width2 = width - width1;
        Cluster cluster1 = new Cluster(this, new Rectangle(0, 0, width1, height), false);
        Cluster cluster2 = new Cluster(this, new Rectangle(width1, 0, width2, height), true);
        allClusters.add(cluster1);
        allClusters.add(cluster2);

        leaders.add(cluster2);

        this.clusterByX = makeClusterByX(allClusters);
        this.allClusters = allClusters;
        this.leaders = leaders;
    }

    private Map<Integer,List<Cluster>> makeClusterByX(List<Cluster> allClusters) {
        Map<Integer,List<Cluster>> clusterByX = new HashMap<>();
        for (int i = 0; i <= width; i++) {
            for (Cluster aCluster : allClusters) {
                final int x = i * CLUSTER_GRAN;
                if (aCluster.rect.contains(x, 0)) {
                    List<Cluster> cc = clusterByX.computeIfAbsent(i, k -> new ArrayList<>(1));
                    cc.add(aCluster);
                }
            }
        }
        return clusterByX;
    }

    private class Worker extends Thread {
        private final GuiManager gui;
        private final Cluster cluster;
        Worker(GuiManager gui, Cluster cluster) {
            this.gui = gui;
            this.cluster = cluster;
            setName("cluster-" + cluster.id + (cluster.leader ? "-leader" : ""));
        }
        public void run() {
            while (started) {       // обновляем матрицу
                if (TRAVERSE_MODE == 0) {
                    iterateLinked();
                } else {
                    iterateCluster(cluster, gui);
                }
            }
        }
    }

    private void iterateCluster(Cluster cluster, GuiManager gui) {
        final long now = System.currentTimeMillis();
        System.err.println("iterate " + cluster + " / " + Thread.currentThread().getId());
        final int maxX = cluster.rect.x + cluster.rect.width;
        for (int x = cluster.rect.x; x < maxX; x++) {
            final int maxY = cluster.rect.y + cluster.rect.height;
            for (int y = cluster.rect.y; y < maxY; y++) {
                final Bot bot = matrix[x][y];
                if (bot != null && bot.alive == 3) {
                    bot.step();
                }
            }
        }
        if (!cluster.leader) {
            Utils.await(barrier);
        }
        if (cluster.leader) {
            generation++;
            if (generation % gui.drawstep == 0 && (now - lastTimePainted) > 15) {
                gui.paintWorld();
                lastTimePainted = now;
            }
        }
    }

    private void iterateLinked() {
        while (currentbot != zerobot) {
            if (currentbot.alive == 3) currentbot.step();
            currentbot = currentbot.next;
        }
        currentbot = currentbot.next;
    }

    void start(GuiManager gui) {
        started	= true;
        barrier = new CyclicBarrier(allClusters.size() - 1, () -> {
            for (Cluster aLeader : leaders) {
                iterateCluster(aLeader, gui);
            }
        });
        List<Thread> workers = new ArrayList<>();
        for (Cluster cluster : allClusters) {
            if (!cluster.leader) {
                Worker thread = new Worker(gui, cluster);
                workers.add(thread);
                thread.start();
            }
        }
        this.workers = workers;
    }

    void stop() {
        started = false;        //Выставляем влаг
        for (Thread worker : workers) {
            Utils.joinSafe(worker);
        }
        workers = null;
    }

    // делаем паузу
    // не используется
    /*public void sleep() {
        try {
            int delay = 20;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
        }
    }*/

    // генерируем карту
    public void generateMap(long seed) {
        generation = 0; // todo responsibility
        int[][] map = new int[width][height];
        this.matrix = new Bot[width][height]; // todo responsibility
        final int[] mapInGPU = new int[width * height];

        final float f = (float) perlinValue;
        final Perlin2D perlin = new Perlin2D(seed);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final float value = perlin.getNoise(x/f,y/f,8,0.45f);        // вычисляем точку ландшафта
                final int intValue = (int) (value * 255 + 128) & 255;
                map[x][y] = intValue;
                mapInGPU[y * width + x] = map[x][y];
            }
        }
        this.mapInGPU = mapInGPU;
        this.map = map;
    }

    // генерируем первого бота
    public void generateAdam() {
        final int x = width / 2;
        final int y = height / 2;

        final Bot bot = new Bot(this, findCluster(x, y));
        zerobot.prev = bot;
        zerobot.next = bot;

        bot.adr = 0;            // начальный адрес генома
        bot.x = x;      // координаты бота
        bot.y = y;
        bot.health = 990;       // энергия
        bot.mineral = 0;        // минералы
        bot.alive = 3;          // бот живой
        bot.age = 0;            // возраст
        bot.c_red = 170;        // задаем цвет бота
        bot.c_blue = 170;
        bot.c_green = 170;
        bot.direction = 5;      // направление
        bot.prev = zerobot;     // ссылка на предыдущего
        bot.next = zerobot;     // ссылка на следующего
        for (int i = 0; i < 64; i++) {          // заполняем геном командой 32 - фотосинтез
            bot.mind[i] = 32;
        }

        matrix[bot.x][bot.y] = bot;             // помещаем бота в матрицу
        currentbot = bot;                       // устанавливаем текущим
    }

    public double rand() {
        // todo maybe not MT ready
        int i = this.randIdx + 1;
        if (i >= randMemory.length) {
            i = 0;
        }
        this.randIdx = i;
        return randMemory[i];
    }

    Cluster findCluster(int x, int y) {
        for (Cluster cluster : allClusters) {
            if (cluster.rect.contains(x, y)) {
                return cluster;
            }
        }
        throw new RuntimeException("not found cluster for " + x + ", " + y);
    }
}
