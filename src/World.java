import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;


// Основной класс программы.
public class World implements GuiCallback,Consts {

    int width;
    int height;
    private int zoom;
    int sealevel;
    private int drawstep;
    int[][] map;    //Карта мира
    private int[] mapInGPU;    //Карта для GPU
    private Image mapbuffer = null;
    Bot[][] matrix;    //Матрица мира
    private Bot zerobot = new Bot(this);
    @Deprecated
    private Bot currentbot;
    int generation;
    private int population;
    private int organic;
    private int viewMode = VIEW_MODE_BASE;

    private Image buffer = null;

    private Thread thread = null;
    private List<Thread> threads = null;
    private boolean started = true; // поток работает?

    private final Gui gui;

    public List<BotCluster> clusters;

    private boolean mapPrepared = false;

    static final Font ALERT_FONT = new Font(Font.SANS_SERIF, 0, 24);

    public World() {
        //simulation = this;
        zoom = 1;
        sealevel = SEA_LEVEL_DEFAULT;
        drawstep = DRAW_STEP_DEFAULT;
        gui = new Gui(this);
        gui.init();
    }

    @Override
    public void drawStepChanged(int value) {
        this.drawstep = value;
    }

    @Override
    public void mapGenerationStarted() {
        width = gui.canvas.getWidth() / zoom;    // Ширина доступной части экрана для рисования карты
        height = gui.canvas.getHeight() / zoom;
        generateMap();
        createClusters();
        generateAdam();
        paintMapView();
        paint1();
        mapPrepared = true;
    }

    void createClusters() {
        final int cores = Runtime.getRuntime().availableProcessors();
        int perSide = (int)Math.floor(Math.sqrt(cores));
        //perSide = 1;
        System.out.println("cores: " + cores);
        System.out.println("threads: " + perSide + "x" + perSide);

        List<BotCluster> clusters = new ArrayList<>();

        final int pieceBaseWidth = width / perSide;
        final int pieceBaseHeight = height / perSide;
        for (int i = 0; i < perSide; i++) {
            for (int j = 0; j < perSide; j++) {
                final int x = pieceBaseWidth * i;
                final int y = pieceBaseHeight * j;
                final int piecePreciseWidth = (i == perSide - 1) ? (width - x) : pieceBaseWidth;
                final int piecePreciseHeight = (j == perSide - 1) ? (height - y) : pieceBaseHeight;
                Rectangle rect = new Rectangle(x, y, piecePreciseWidth, piecePreciseHeight);
                System.out.println("thread rect: " + rect);
                clusters.add(new BotCluster(this, rect));
            }
        }

        this.clusters = clusters;
    }

    @Override
    public void seaLevelChanged(int value) {
        sealevel = value;
        if (map != null) {
            paintMapView();
            paint1();
        }
    }

    @Override
    public boolean startedOrStopped() {
        if (threads == null) {
            started = true;
            threads = new ArrayList<>(clusters.size());
            PaintThread paintThread = new PaintThread();
            threads.add(paintThread);
            paintThread.start();
            boolean doIncrement = true;
            for (BotCluster cluster : clusters) {
                thread = new WorkerMT(cluster, doIncrement);
                thread.start();
                threads.add(thread);
                doIncrement = false;
                //System.out.println("started " + thread);
            }
            //thread	= new Worker(); // создаем новый поток
            //thread.start();
            return true;
        } else {
            started = false;        //Выставляем влаг
            Utils.joinSafe(threads);
            threads = null;
            return false;
        }
    }

    @Override
    public void viewModeChanged(int viewMode) {
        this.viewMode = viewMode;
    }

    public void paintMapView() {
        int mapred;
        int mapgreen;
        int mapblue;
        mapbuffer = gui.canvas.createImage(width * zoom, height * zoom); // ширина - высота картинки
        Graphics g = mapbuffer.getGraphics();

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < rgb.length; i++) {
            if (mapInGPU[i] < sealevel) {                     // подводная часть
                mapred = 5;
                mapblue = 140 - (sealevel - mapInGPU[i]) * 3;
                mapgreen = 150 - (sealevel - mapInGPU[i]) * 10;
                if (mapgreen < 10) mapgreen = 10;
                if (mapblue < 20) mapblue = 20;
            } else {                                        // надводная часть
                mapred = (int)(150 + (mapInGPU[i] - sealevel) * 2.5);
                mapgreen = (int)(100 + (mapInGPU[i] - sealevel) * 2.6);
                mapblue = 50 + (mapInGPU[i] - sealevel) * 3;
                if (mapred > 255) mapred = 255;
                if (mapgreen > 255) mapgreen = 255;
                if (mapblue > 255) mapblue = 255;
            }
            rgb[i] = (mapred << 16) | (mapgreen << 8) | mapblue;
        }
        g.drawImage(image, 0, 0, null);


    }


//    @Override
    public void paint1() {

        Image buf = gui.canvas.createImage(width * zoom, height * zoom); //Создаем временный буфер для рисования
        Graphics g = buf.getGraphics(); //подеменяем графику на временный буфер
        g.drawImage(mapbuffer, 0, 0, null);

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        population = 0;
        organic = 0;
        int mapred, mapgreen, mapblue;

        while (currentbot != null && currentbot != zerobot) {
            if (currentbot.alive == 3) {                      // живой бот
                if (viewMode == VIEW_MODE_BASE) {
                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (currentbot.c_red << 16) | (currentbot.c_green << 8) | currentbot.c_blue;
                } else if (viewMode == VIEW_MODE_ENERGY) {
                    mapgreen = 255 - (int) (currentbot.health * 0.25);
                    if (mapgreen < 0) mapgreen = 0;
                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (255 << 16) | (mapgreen << 8) | 0;
                } else if (viewMode == VIEW_MODE_MINERAL) {
                    mapblue = 255 - (int) (currentbot.mineral * 0.5);
                    if (mapblue < 0) mapblue = 0;
                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (0 << 16) | (255 << 8) | mapblue;
                } else if (viewMode == VIEW_MODE_COMBINED) {
                    mapgreen = (int) (currentbot.c_green * (1 - currentbot.health * 0.0005));
                    if (mapgreen < 0) mapgreen = 0;
                    mapblue = (int) (currentbot.c_blue * (0.8 - currentbot.mineral * 0.0005));
                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (currentbot.c_red << 16) | (mapgreen << 8) | mapblue;
                } else if (viewMode == VIEW_MODE_AGE) {
                    mapred = 255 - (int) (Math.sqrt(currentbot.age) * 4);
                    if (mapred < 0) mapred = 0;
                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (mapred << 16) | (0 << 8) | 255;
                } else if (viewMode == VIEW_MODE_FAMILY) {
                    rgb[currentbot.y * width + currentbot.x] = currentbot.c_family;
                }
                population++;
            } else if (currentbot.alive == 1) {                                            // органика, известняк, коралловые рифы
                if (map[currentbot.x][currentbot.y] < sealevel) {                     // подводная часть
                    mapred = 20;
                    mapblue = 160 - (sealevel - map[currentbot.x][currentbot.y]) * 2;
                    mapgreen = 170 - (sealevel - map[currentbot.x][currentbot.y]) * 4;
                    if (mapblue < 40) mapblue = 40;
                    if (mapgreen < 20) mapgreen = 20;
                } else {                                    // скелетики, трупики на суше
                    mapred = (int) (80 + (map[currentbot.x][currentbot.y] - sealevel) * 2.5);   // надводная часть
                    mapgreen = (int) (60 + (map[currentbot.x][currentbot.y] - sealevel) * 2.6);
                    mapblue = 30 + (map[currentbot.x][currentbot.y] - sealevel) * 3;
                    if (mapred > 255) mapred = 255;
                    if (mapblue > 255) mapblue = 255;
                    if (mapgreen > 255) mapgreen = 255;
                }
                rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (mapred << 16) | (mapgreen << 8) | mapblue;
                organic++;
            }
            currentbot = currentbot.next;
        }
        if (currentbot != null) {
            currentbot = currentbot.next;
        }

        if (population <= 0) {
            started = false;
            g.setFont(ALERT_FONT);
            g.setColor(Color.WHITE);
            g.drawString("None survived", width/2, height/2);
        }

        g.drawImage(image, 0, 0, null);

        gui.generationLabel.setText(" Generation: " + String.valueOf(generation));
        gui.populationLabel.setText(" Population: " + String.valueOf(population));
        gui.organicLabel.setText(" Organic: " + String.valueOf(organic));

        gui.buffer = buf;
        gui.canvas.repaint();
    }

    class Worker extends Thread {
        public void run() {
            started	= true;         // Флаг работы потока, если false  поток заканчивает работу
            while (started) {       // обновляем матрицу
                long time1 = System.currentTimeMillis();
                while (currentbot != zerobot) {
                    if (currentbot.alive == 3) currentbot.step();
                    currentbot = currentbot.next;
                }
                currentbot = currentbot.next;
                generation++;
                long time2 = System.currentTimeMillis();
//                System.out.println("Step execute " + ": " + (time2-time1) + "");
                if (generation % drawstep == 0) {             // отрисовка на экран через каждые ... шагов
                    paint1();                           // отображаем текущее состояние симуляции на экран
                }
                long time3 = System.currentTimeMillis();
//                System.out.println("Paint: " + (time3-time2));
            }
            started = false;        // Закончили работу
        }
    }

    class PaintThread extends Thread {
        public void run() {
            try {
                final long pause = 1000 / 60;
                while (started) {
                    paint1();
                    Thread.sleep(pause);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class WorkerMT extends Thread {
        BotCluster cluster;
        boolean doIncrement;
        WorkerMT(BotCluster cluster, boolean doIncrement) {
            this.cluster = cluster;
            this.doIncrement = doIncrement;
        }
        public void run() {
            Rectangle rect = cluster.rect;
            final int right = rect.x + rect.width;
            final int bottom = rect.y + rect.height;
            while (started) {
                for (int i = rect.x; i < right; i++) {
                    for (int j = rect.y; j < bottom; j++) {
                        Bot bot = cluster.matrix[i][j];
                        if (bot != null && bot.isAlive()) {
                            bot.step();
                        }
                    }
                }
                if (doIncrement) {
                    generation++;
                }
            }
        }
    }

    //@Deprecated // use instance
    //public static World simulation;

    public static void main(String[] args) {
        World world = new World();
        //world.generateMap();
        //simulation = world;
//        simulation.generateAdam();
//        simulation.run();
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

    public void generateMap() {
        generateMap((int) (Math.random() * 10000));
    }

    // генерируем карту
    public void generateMap(int seed) {
        generation = 0;
        this.map = new int[width][height];
        this.matrix = new Bot[width][height];

        Perlin2D perlin = new Perlin2D(seed);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float f = (float) gui.perlinSlider.getValue();
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
        //bot.x = 200;
        //bot.y = 200;
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

        clusters.get(0).add(bot);
        matrix[bot.x][bot.y] = bot;             // помещаем бота в матрицу
        currentbot = bot;                       // устанавливаем текущим
    }


}
