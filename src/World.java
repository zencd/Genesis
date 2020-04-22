import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Движок симуляции.
 */
public class World implements Consts {

    int width;
    int height;
    int zoom = 1;
    int sealevel = SEA_LEVEL_DEFAULT;
    int[][] map;    //Карта мира
    int[] mapInGPU;    //Карта для GPU
    Bot[][] matrix;    //Матрица мира
    @Deprecated
    Bot zerobot = new Bot(this);
    @Deprecated
    Bot currentbot;
    int generation;
    int population;
    int organic;
    int perlinValue = PERLIN_DEFAULT;

    private Thread thread = null;
    private Thread paintThread = null;
    private boolean started = false; // поток работает?

    private final Supplier<Long> mapSeeder; // возвращает seed только для построения карты
    private final double[] randMemory; // массив предгенерированных случайных чисел
    private int randIdx = 0;

    List<Cluster> clusters;

    private long timeStarted;
    private long botsProcessed;

    public World(double[] randMemory, Supplier<Long> mapSeeder) {
        this.randMemory = randMemory;
        this.mapSeeder = mapSeeder;
    }

    public boolean isStarted() {
        return started;
    }

    public void mapGenerationInitiated(int canvasWidth, int canvasHeight) {
        width = canvasWidth / zoom;    // Ширина доступной части экрана для рисования карты
        height = canvasHeight / zoom;
        clusters = new ArrayList<>();
        clusters.add(new Cluster(this));
        generateMap(mapSeeder.get());
        generateAdam();
    }

    public final void moveTo(Bot bot, int newX, int newY) {
        final Cluster prevCluster = findCluster(bot);
        final int prevX = bot.x;
        final int prevY = bot.y;
        matrix[newX][newY] = bot;
        matrix[prevX][prevY] = null;
        bot.x = newX;
        bot.y = newY;
        final Cluster newCluster = findCluster(bot);
        if (prevCluster != newCluster) {
            prevCluster.remove(bot);
            newCluster.add(bot, null);
        }
    }

    public void addToWorld(Bot newBot, Bot before) {
        // add to matrix
        matrix[newBot.x][newBot.y] = newBot;

        // add to cluster
        findCluster(newBot).add(newBot, before);

        // вставляем нового бота между ботом-предком и предыдущим ботом
        // в цепочке ссылок, которая объединяет всех ботов
    }

    final Cluster findCluster(Bot bot) {
        return clusters.get(0); // todo a single cluster yet
    }

    final void iterate1() {
        int proc1 = 0;
        int proc2 = 0;
        while (currentbot != zerobot) {
            proc1++;
            if (currentbot.alive == 3) {
                currentbot.step();
                proc2++;
            }
            currentbot = currentbot.next;
        }
        currentbot = currentbot.next;
        generation++;

        final long tookSinceStart = System.currentTimeMillis() - timeStarted;
        this.botsProcessed += proc2;
        double sec = tookSinceStart / 1000d;
        long speed = (long) (this.botsProcessed / sec / 1000);
        System.out.println("gen: " + generation + ", proc1: " + proc1 + ", proc2: " + proc2 + ", speed: " + speed + " KB/sec");
    }

    final void iterate2() {
        assert clusters.size() == 1;
        for (Cluster cluster : clusters) {
            final int size = cluster.size();
            Bot first = cluster.getStart();
            Bot bot = first;
            int proc = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; bot != first || i == 0; i++) {
                //if (i % 100 == 0) {
                //    System.out.println("bot: " + bot + ", cluster: " + cluster.size());
                //}
                if (bot.alive == 3) {
                    bot.step();
                    proc++;
                }
                if (bot == bot.nextBot) {
                    break;
                }
                bot = bot.nextBot;
            }
            final long tookSinceStart = System.currentTimeMillis() - timeStarted;
            this.botsProcessed += proc;
            double sec = tookSinceStart / 1000d;
            long speed = (long) (this.botsProcessed / sec / 1000);
            long took = System.currentTimeMillis() - start;
            //System.out.println("gen: " + generation + ", bots: " + size + ", speed: " + speed + " KB/sec");
        }
        //currentbot = clusters.get(0).bots.iterator().next();
        generation++;
    }

    private class Worker extends Thread {
        private final GuiManager gui;
        //private long lastTimePainted = 0;
        Worker(GuiManager gui) {
            this.gui = gui;
        }
        public void run() {
            while (started) {       // обновляем матрицу
                long now = System.currentTimeMillis();
                if (ALGO == 1) {
                    iterate1();
                } else {
                    iterate2();
                }
                //long time2 = System.currentTimeMillis();
//                System.out.println("Step execute " + ": " + (time2-time1) + "");
//                if (generation % gui.drawstep == 0 && (now-lastTimePainted) > 15) {             // отрисовка на экран через каждые ... шагов
                    //gui.paintWorld();                           // отображаем текущее состояние симуляции на экран
                    //lastTimePainted = now;
                //}
            }
            //System.out.println("worker thread finished");
        }
    }

    private class PaintWorker extends Thread {
        private final GuiManager gui;
        private long lastTimePainted = 0;
        PaintWorker(GuiManager gui) {
            this.gui = gui;
        }
        public void run() {
            try {
                while (started) {
                    Thread.sleep(15);
                    gui.paintWorld();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void start(GuiManager gui) {
        timeStarted = System.currentTimeMillis();
        botsProcessed = 0;
        started	= true;         // Флаг работы потока, если false  поток заканчивает работу

        thread = new Worker(gui); // создаем новый поток
        thread.start();

        paintThread = new PaintWorker(gui);
        paintThread.start();
    }

    void stop() {
        started = false;        //Выставляем влаг
        Utils.joinSafe(thread);
        Utils.joinSafe(paintThread);
        thread = null;
        paintThread = null;
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
        Bot bot = new Bot(this);

        bot.adr = 0;            // начальный адрес генома
        bot.x = width / 2;      // координаты бота
        bot.y = height / 2;
        bot.health = 990;       // энергия
        bot.mineral = 0;        // минералы
        bot.alive = 3;          // бот живой
        bot.age = 0;            // возраст
        bot.c_red = 170;        // задаем цвет бота
        bot.c_blue = 170;
        bot.c_green = 170;
        bot.direction = 5;      // направление
        for (int i = 0; i < 64; i++) {          // заполняем геном командой 32 - фотосинтез
            bot.mind[i] = 32;
        }

        zerobot.prev = bot;
        zerobot.next = bot;
        bot.prev = zerobot;     // ссылка на предыдущего
        bot.next = zerobot;     // ссылка на следующего

        clusters.get(0).add(bot);
        //zerobot.prevBot = bot;
        //zerobot.nextBot = bot;
        //bot.prevBot = zerobot;
        //bot.nextBot = zerobot;

        matrix[bot.x][bot.y] = bot;             // помещаем бота в матрицу

        currentbot = bot;                       // устанавливаем текущим
        addToWorld(bot, null);
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
}
