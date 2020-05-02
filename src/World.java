import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Движок симуляции.
 */
public final class World implements Consts {

    private static final int CLUSTER_GRAN = 50;

    int width;
    int height;
    int zoom = 1;
    int seaLevel = SEA_LEVEL_DEFAULT;
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
    private Map<Integer,List<Cluster>> clusterByX; // todo not used yet, but useful
    private List<Cluster> leaders;
    int numThreads = NUM_THREADS;

    private List<Thread> workers = null;
    private CyclicBarrier barrier;
    private boolean started = false; // Флаг работы потока, если false  поток заканчивает работу

    private final Supplier<Long> mapSeeder; // возвращает seed только для построения карты

    public World(Supplier<Long> mapSeeder) {
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
        if (numThreads == 1) {
            initClustersST();
        } else {
            initClustersMT();
        }
        generateMap(mapSeeder.get());
        generateAdam();
    }

    private void initClustersST() {
        final List<Cluster> allClusters = new ArrayList<>();
        Cluster cluster = new Cluster(this, new Rectangle(0, 0, width, height), false);
        allClusters.add(cluster);

        this.clusterByX = makeClusterByX(allClusters);
        this.allClusters = allClusters;
        this.leaders = new ArrayList<>();
    }

    private void initClustersMT() {
        final List<Cluster> allClusters = new ArrayList<>();
        final List<Cluster> leaders = new ArrayList<>();
        final int gap = 5;
        final int baseWidth = (width - (numThreads-1)*gap) / numThreads;

        for (int x = 0, thr = 0; x < width; thr++) {
            if (x > 0) {
                final int aWidth = gap;
                Cluster cluster = new Cluster(this, new Rectangle(x, 0, aWidth, height), true);
                leaders.add(cluster);
                allClusters.add(cluster);
                x += aWidth;
            }
            {
                int aWidth = baseWidth;
                if (thr == numThreads - 1) {
                    aWidth = this.width - x;
                }
                Cluster cluster = new Cluster(this, new Rectangle(x, 0, aWidth, height), false);
                allClusters.add(cluster);
                x += aWidth;
            }
        }

        for (Cluster cluster : allClusters) {
            System.err.println("* " + cluster);
        }
        System.err.println("threads: " + numThreads);
        System.err.println("width: " + width);

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
                if (numThreads == 1) {
                    iterateLinked(gui);
                } else {
                    iterateCluster(cluster, gui);
                }
            }
        }
    }

    private void iterateCluster(Cluster cluster, GuiManager gui) {
        final long now = System.currentTimeMillis();
        //System.err.println("iterate " + cluster + " / " + Thread.currentThread().getId());
        final int maxX = cluster.rect.x + cluster.rect.width;
        for (int x = cluster.rect.x; x < maxX; x++) {
            final int maxY = cluster.rect.y + cluster.rect.height;
            for (int y = cluster.rect.y; y < maxY; y++) {
                final Bot bot = matrix[x][y];
                if (bot != null && bot.alive == Bot.STATE_ALIVE) {
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

    private void iterateLinked(GuiManager gui) {
        //System.err.println("iterateLinked");
        final long now = System.currentTimeMillis();
        while (currentbot != zerobot) {
            if (currentbot.alive == Bot.STATE_ALIVE) currentbot.step();
            currentbot = currentbot.next;
        }
        currentbot = currentbot.next;
        generation++;
        if (generation % gui.drawstep == 0 && (now - lastTimePainted) > 15) {
            gui.paintWorld();
            lastTimePainted = now;
        }
    }

    void start(GuiManager gui) {
        waitForClusters();
        started	= true;
        if (numThreads > 1) {
            barrier = new CyclicBarrier(numThreads, () -> {
                for (Cluster aLeader : leaders) {
                    iterateCluster(aLeader, gui);
                }
            });
        }
        List<Thread> workers = new ArrayList<>();
        for (Cluster cluster : allClusters) {
            if (!cluster.leader || numThreads == 1) {
                Worker thread = new Worker(gui, cluster);
                workers.add(thread);
                thread.start();
            }
        }
        this.workers = workers;
    }

    void stop() {
        started = false;        //Выставляем влаг
        Utils.joinSafe(workers);
        workers = null;
    }

    private void waitForClusters() {
        while (!isClustersReady()) {
            System.err.println("clusters not ready yet - sleeping some");
            Utils.sleep(TimeUnit.MILLISECONDS, 50);
        }
    }

    private boolean isClustersReady() {
        return allClusters.stream().allMatch(cluster -> cluster.ready);
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
        final long start = System.currentTimeMillis();
        final int numThreads = Math.max(NUM_THREADS - 1, 1);
        final int sliceWidth = width / numThreads;
        final List<Thread> threads = new ArrayList<>(0);
        for (int i = 0, xStart = 0; i < numThreads; i++) {
            final int xStartFinal = xStart;
            final int xEnd;
            {
                final int xEndTmp = (i == numThreads - 1) ? width : (xStart + sliceWidth);
                xEnd = Math.min(xEndTmp, width);
            }
            final Runnable runnable = () -> {
                for (int x = xStartFinal; x < xEnd; x++) {
                    for (int y = 0; y < height; y++) {
                        final float value = perlin.getNoise(x / f, y / f, 8, 0.45f);        // вычисляем точку ландшафта
                        final int intValue = (int) (value * 255 + 128) & 255;
                        map[x][y] = intValue;
                        mapInGPU[y * width + x] = map[x][y];
                    }
                }
            };
            final Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
            xStart = xEnd;
        }
        Utils.joinSafe(threads);
        System.err.println("map generated for " + (System.currentTimeMillis() - start) + " ms");
        this.mapInGPU = mapInGPU;
        this.map = map;
    }

    // генерируем первого бота
    public void generateAdam() {
        final int x = width / 2;
        final int y = height / 2;

        final Bot bot = new Bot(this, findCluster(x, y));
        bot.x = x;
        bot.y = y;
        zerobot.prev = bot;
        zerobot.next = bot;
        bot.prev = zerobot;     // ссылка на предыдущего
        bot.next = zerobot;     // ссылка на следующего

        bot.initNewBotStats();

        matrix[bot.x][bot.y] = bot;             // помещаем бота в матрицу
        currentbot = bot;                       // устанавливаем текущим
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
