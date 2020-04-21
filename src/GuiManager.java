import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Отвечает за все аспекты визуализации через GUI.
 */
public class GuiManager implements Consts {
    public final GuiFrame frame;
    private final World world;
    int viewMode = VIEW_MODE_BASE;
    int drawstep = 10;
    private Image mapbuffer = null;

    public interface Callback {
        void drawStepChanged(int value);
        void mapGenerationStarted(int canvasWidth, int canvasHeight);
        void seaLevelChanged(int value);
        boolean startedOrStopped();
        void viewModeChanged(int viewMode);
        void perlinChanged(int value);
    }

    public GuiManager(World world, Callback callback) {
        this.world = world;
        this.frame = new GuiFrame(callback);
    }

    public void init() {
        frame.init();
    }

    public void paintMapView() {
        final World w = this.world;
        final int sealevel = w.sealevel;

        int mapred;
        int mapgreen;
        int mapblue;
        mapbuffer = frame.canvas.createImage(w.width * w.zoom, w.height * w.zoom); // ширина - высота картинки
        final Graphics g = mapbuffer.getGraphics();

        final BufferedImage image = new BufferedImage(w.width, w.height, BufferedImage.TYPE_INT_RGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < rgb.length; i++) {
            final int elem = w.mapInGPU[i];
            if (elem < sealevel) {                     // подводная часть
                mapred = 5;
                mapblue = 140 - (sealevel - elem) * 3;
                mapgreen = 150 - (sealevel - elem) * 10;
                if (mapgreen < 10) mapgreen = 10;
                if (mapblue < 20) mapblue = 20;
            } else {                                        // надводная часть
                mapred = (int)(150 + (elem - sealevel) * 2.5);
                mapgreen = (int)(100 + (elem - sealevel) * 2.6);
                mapblue = 50 + (elem - sealevel) * 3;
                if (mapred > 255) mapred = 255;
                if (mapgreen > 255) mapgreen = 255;
                if (mapblue > 255) mapblue = 255;
            }
            rgb[i] = (mapred << 16) | (mapgreen << 8) | mapblue;
        }
        g.drawImage(image, 0, 0, null);
    }

    public void paint1() {
        final World w = this.world;

        final Image buf = frame.canvas.createImage(w.width * w.zoom, w.height * w.zoom); //Создаем временный буфер для рисования
        final Graphics g = buf.getGraphics(); //подеменяем графику на временный буфер
        g.drawImage(mapbuffer, 0, 0, null);

        final BufferedImage image = new BufferedImage(w.width, w.height, BufferedImage.TYPE_INT_ARGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        w.population = 0; // todo incorrect responsibility
        w.organic = 0; // todo incorrect responsibility
        int mapred, mapgreen, mapblue;

        while (w.currentbot != w.zerobot) {
            if (w.currentbot.alive == 3) {                      // живой бот
                if (viewMode == VIEW_MODE_BASE) {
                    rgb[w.currentbot.y * w.width + w.currentbot.x] = (255 << 24) | (w.currentbot.c_red << 16) | (w.currentbot.c_green << 8) | w.currentbot.c_blue;
                } else if (viewMode == VIEW_MODE_ENERGY) {
                    mapgreen = 255 - (int) (w.currentbot.health * 0.25);
                    if (mapgreen < 0) mapgreen = 0;
                    rgb[w.currentbot.y * w.width + w.currentbot.x] = (255 << 24) | (255 << 16) | (mapgreen << 8) | 0;
                } else if (viewMode == VIEW_MODE_MINERAL) {
                    mapblue = 255 - (int) (w.currentbot.mineral * 0.5);
                    if (mapblue < 0) mapblue = 0;
                    rgb[w.currentbot.y * w.width + w.currentbot.x] = (255 << 24) | (0 << 16) | (255 << 8) | mapblue;
                } else if (viewMode == VIEW_MODE_COMBINED) {
                    mapgreen = (int) (w.currentbot.c_green * (1 - w.currentbot.health * 0.0005));
                    if (mapgreen < 0) mapgreen = 0;
                    mapblue = (int) (w.currentbot.c_blue * (0.8 - w.currentbot.mineral * 0.0005));
                    rgb[w.currentbot.y * w.width + w.currentbot.x] = (255 << 24) | (w.currentbot.c_red << 16) | (mapgreen << 8) | mapblue;
                } else if (viewMode == VIEW_MODE_AGE) {
                    mapred = 255 - (int) (Math.sqrt(w.currentbot.age) * 4);
                    if (mapred < 0) mapred = 0;
                    rgb[w.currentbot.y * w.width + w.currentbot.x] = (255 << 24) | (mapred << 16) | (0 << 8) | 255;
                } else if (viewMode == VIEW_MODE_FAMILY) {
                    rgb[w.currentbot.y * w.width + w.currentbot.x] = w.currentbot.c_family;
                }
                w.population++;
            } else if (w.currentbot.alive == 1) {                                            // органика, известняк, коралловые рифы
                if (w.map[w.currentbot.x][w.currentbot.y] < w.sealevel) {                     // подводная часть
                    mapred = 20;
                    mapblue = 160 - (w.sealevel - w.map[w.currentbot.x][w.currentbot.y]) * 2;
                    mapgreen = 170 - (w.sealevel - w.map[w.currentbot.x][w.currentbot.y]) * 4;
                    if (mapblue < 40) mapblue = 40;
                    if (mapgreen < 20) mapgreen = 20;
                } else {                                    // скелетики, трупики на суше
                    mapred = (int) (80 + (w.map[w.currentbot.x][w.currentbot.y] - w.sealevel) * 2.5);   // надводная часть
                    mapgreen = (int) (60 + (w.map[w.currentbot.x][w.currentbot.y] - w.sealevel) * 2.6);
                    mapblue = 30 + (w.map[w.currentbot.x][w.currentbot.y] - w.sealevel) * 3;
                    if (mapred > 255) mapred = 255;
                    if (mapblue > 255) mapblue = 255;
                    if (mapgreen > 255) mapgreen = 255;
                }
                rgb[w.currentbot.y * w.width + w.currentbot.x] = (255 << 24) | (mapred << 16) | (mapgreen << 8) | mapblue;
                w.organic++;
            }
            w.currentbot = w.currentbot.next;
        }
        w.currentbot = w.currentbot.next;

        g.drawImage(image, 0, 0, null);

        frame.generationLabel.setText(" Generation: " + String.valueOf(w.generation));
        frame.populationLabel.setText(" Population: " + String.valueOf(w.population));
        frame.organicLabel.setText(" Organic: " + String.valueOf(w.organic));

        frame.buffer = buf;
        frame.canvas.repaint();
    }
}
