import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Движок симуляции.
 */
public final class World implements Consts {

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

    private List<Cluster> allClusters;
    private List<Cluster> leaders;
    int numThreads = NUM_WORKERS;

    long lastTimePainted = 0;

    private List<Thread> workers = null;
    private CyclicBarrier barrier;
    private boolean started = false; // Флаг работы потока, если false  поток заканчивает работу

    public final Supplier<Long> mapSeeder; // возвращает seed только для построения карты

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
        this.generation = 0;
        this.population = 0;
        this.organic = 0;
        this.matrix = new Bot[width][height];
        MapGenerator.generateMap(this);
        generateAdam();
    }

    private void initClustersST() {
        final List<Cluster> allClusters = new ArrayList<>();
        Cluster cluster = new Cluster(this, new Rectangle(0, 0, width, height), false, false);
        allClusters.add(cluster);

        this.allClusters = allClusters;
        this.leaders = new ArrayList<>();
    }

    private void initClustersMT() {
        final List<Cluster> allClusters = new ArrayList<>();
        final List<Cluster> leaders = new ArrayList<>();
        final int gap = 1; // pixels
        final int baseWidth = (width - (numThreads-1)*gap) / numThreads;

        boolean superLeader = true;
        for (int x = 0, thr = 0; x < width; thr++) {
            if (x > 0) {
                final int aWidth = gap;
                Cluster cluster = new Cluster(this, new Rectangle(x, 0, aWidth, height), true, superLeader);
                leaders.add(cluster);
                allClusters.add(cluster);
                x += aWidth;
                superLeader = false; // only one must rule
            }
            {
                int aWidth = baseWidth;
                if (thr == numThreads - 1) {
                    aWidth = this.width - x;
                }
                Cluster cluster = new Cluster(this, new Rectangle(x, 0, aWidth, height), false, false);
                allClusters.add(cluster);
                x += aWidth;
            }
        }

        //for (Cluster cluster : allClusters) {
        //    System.err.println("* " + cluster);
        //}
        System.err.println("threads: " + numThreads);
        System.err.println("width: " + width);

        this.allClusters = allClusters;
        this.leaders = leaders;
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

    // генерируем первого бота
    public void generateAdam() {
        final Point startPoint = MapGenerator.findStartPoint(this);
        final int x = startPoint.x;
        final int y = startPoint.y;

        final Bot bot = new Bot(this, findCluster(null, x, y));
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

    void start(GuiManager gui) {
        waitForClusters();
        started	= true;
        if (numThreads > 1) {
            barrier = new CyclicBarrier(numThreads, () -> {
                for (Cluster aLeader : leaders) {
                    iterateCluster(aLeader);
                }
            });
        }
        List<Thread> workers = new ArrayList<>();
        for (Cluster cluster : allClusters) {
            if (!cluster.leader || numThreads == 1) {
                Worker thread = new Worker(cluster, gui);
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
        barrier = null;
    }

    private class Worker extends Thread {
        private final Cluster cluster;
        private final GuiManager gui;
        Worker(Cluster cluster, GuiManager gui) {
            this.cluster = cluster;
            this.gui = gui;
            setDaemon(true);
            setName("cluster-" + cluster.id + (cluster.leader ? "-leader" : ""));
        }
        public void run() {
            while (started) {       // обновляем матрицу
                if (numThreads == 1) {
                    iterateLinked(gui);
                } else {
                    iterateCluster(cluster);
                }
            }
        }
    }

    private void iterateCluster(Cluster cluster) {
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
        if (cluster.superLeader) {
            generation++;
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
        if ((now - lastTimePainted) > 15) {
            gui.paintBots();
            lastTimePainted = now;
        }
    }

    Cluster findCluster(Cluster known, int x, int y) {
        if (known != null && known.rect.contains(x, y)) {
            return known;
        }
        for (Cluster cluster : allClusters) {
            if (cluster.rect.contains(x, y)) {
                return cluster;
            }
        }
        throw new RuntimeException("not found cluster for " + x + ", " + y);
    }
}
