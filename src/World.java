/**
 * Движок симуляции.
 */
public class World implements Consts {

    int width;
    int height;
    int zoom;
    int sealevel;
    int[][] map;    //Карта мира
    int[] mapInGPU;    //Карта для GPU
    Bot[][] matrix;    //Матрица мира
    Bot zerobot = new Bot(this);
    Bot currentbot;
    int generation;
    int population;
    int organic;
    int perlinValue = PERLIN_DEFAULT;

    Thread thread = null;
    boolean started = true; // поток работает?

    public World() {
        zoom = 1;
        sealevel = SEA_LEVEL_DEFAULT;
    }

    class Worker extends Thread {
        Gui gui;
        long lastTimePainted = 0;
        Worker(Gui gui) {
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
                    gui.paint1();                           // отображаем текущее состояние симуляции на экран
                    lastTimePainted = now;
                }
                //long time3 = System.currentTimeMillis();
//                System.out.println("Paint: " + (time3-time2));
            }
        }
    }

    void start(Gui gui) {
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
    public void generateMap(int seed) {
        generation = 0;
        this.map = new int[width][height];
        this.matrix = new Bot[width][height];

        final float f = (float) perlinValue;
        Perlin2D perlin = new Perlin2D(seed);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float value = perlin.getNoise(x/f,y/f,8,0.45f);        // вычисляем точку ландшафта
                map[x][y] = (int)(value * 255 + 128) & 255;
            }
        }
        mapInGPU = new int[width * height];
        for (int i=0; i<width; i++) {
            for(int j=0; j<height; j++) {
                mapInGPU[j*width+i] = map[i][j];
            }
        }
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

}
