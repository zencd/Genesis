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
    Bot zerobot = new Bot(this);
    Bot currentbot;
    int generation;
    int population;
    int organic;
    int perlinValue = PERLIN_DEFAULT;

    private Thread thread = null;
    private boolean started = false; // поток работает?

    private final Supplier<Long> mapSeeder; // возвращает seed только для построения карты
    private final double[] randMemory; // массив предгенерированных случайных чисел
    private int randIdx = 0;

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
        generateMap(mapSeeder.get());
        generateAdam();
    }

    private class Worker extends Thread {
        private final GuiManager gui;
        private long lastTimePainted = 0;
        Worker(GuiManager gui) {
            this.gui = gui;
        }
        public void run() {
            while (started) {       // обновляем матрицу
                long now = System.currentTimeMillis();
                while (currentbot != zerobot) {
                    if (currentbot.alive == 3) currentbot.step();
                    currentbot = currentbot.next;
                }
                currentbot = currentbot.next;
                generation++;
                //long time2 = System.currentTimeMillis();
//                System.out.println("Step execute " + ": " + (time2-time1) + "");
                if (generation % gui.drawstep == 0 && (now-lastTimePainted) > 15) {             // отрисовка на экран через каждые ... шагов
                    gui.paintWorld();                           // отображаем текущее состояние симуляции на экран
                    lastTimePainted = now;
                }
                //long time3 = System.currentTimeMillis();
//                System.out.println("Paint: " + (time3-time2));
            }
        }
    }

    void start(GuiManager gui) {
        thread = new Worker(gui); // создаем новый поток
        started	= true;         // Флаг работы потока, если false  поток заканчивает работу
        thread.start();
    }

    void stop() {
        started = false;        //Выставляем влаг
        Utils.joinSafe(thread);
        thread = null;
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
        zerobot.prev = bot;
        zerobot.next = bot;

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
}
