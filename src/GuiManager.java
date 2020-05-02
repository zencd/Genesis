import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Отвечает за все аспекты визуализации через GUI.
 */
public final class GuiManager implements Consts {
    public final GuiFrame frame;
    private final World world;
    int viewMode = VIEW_MODE_BASE;
    int drawstep = 10;
    private Image mapBuffer = null;

    public interface Callback {
        void drawStepChanged(int value);
        void mapGenerationInitiated(int canvasWidth, int canvasHeight);
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

    public void paintMap() {
        final World w = this.world;
        final int sealevel = w.seaLevel;

        final Image mapBuffer = frame.canvas.createImage(w.width * w.zoom, w.height * w.zoom); // ширина - высота картинки
        final Graphics g = mapBuffer.getGraphics();

        final BufferedImage image = new BufferedImage(w.width, w.height, BufferedImage.TYPE_INT_RGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < rgb.length; i++) {
            final int elem = w.mapInGPU[i];
            int red;
            int green;
            int blue;
            if (elem < sealevel) {                     // подводная часть
                red = 5;
                blue = 140 - (sealevel - elem) * 3;
                green = 150 - (sealevel - elem) * 10;
                if (green < 10) green = 10;
                if (blue < 20) blue = 20;
            } else {                                        // надводная часть
                red = (int)(150 + (elem - sealevel) * 2.5);
                green = (int)(100 + (elem - sealevel) * 2.6);
                blue = 50 + (elem - sealevel) * 3;
                if (red > 255) red = 255;
                if (green > 255) green = 255;
                if (blue > 255) blue = 255;
            }
            rgb[i] = (red << 16) | (green << 8) | blue;
        }
        g.drawImage(image, 0, 0, null);
        this.mapBuffer = mapBuffer;
    }

    public void paintWorld() {
        final World w = this.world;
        final int width = w.width;
        final int height = w.height;

        final Image buf = frame.canvas.createImage(width * w.zoom, height * w.zoom); //Создаем временный буфер для рисования
        final Graphics g = buf.getGraphics(); //подеменяем графику на временный буфер
        g.drawImage(mapBuffer, 0, 0, null);

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        w.population = 0; // todo incorrect responsibility
        w.organic = 0; // todo incorrect responsibility

        if (w.numThreads == 1) {
            drawLinked(w, rgb);
        } else {
            drawArray(w, rgb);
        }

        g.drawImage(image, 0, 0, null);

        frame.generationLabel.setText(" Generation: " + w.generation);
        frame.populationLabel.setText(" Population: " + w.population);
        frame.organicLabel.setText(" Organic: " + w.organic);

        frame.buffer = buf;
        frame.canvas.repaint();
    }

    private void drawArray(World w, int[] rgb) {
        final int width = w.width;
        final int height = w.height;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Bot bot = w.matrix[x][y];
                if (bot == null) {
                    continue;
                }
                int red, green, blue;
                if (bot.alive == Bot.STATE_ALIVE) {                      // живой бот
                    final int viewMode = this.viewMode;
                    if (viewMode == VIEW_MODE_BASE) {
                        rgb[bot.y * width + bot.x] = (255 << 24) | (bot.c_red << 16) | (bot.c_green << 8) | bot.c_blue;
                    } else if (viewMode == VIEW_MODE_ENERGY) {
                        green = 255 - (int) (bot.health * 0.25);
                        if (green < 0) green = 0;
                        rgb[bot.y * width + bot.x] = (255 << 24) | (255 << 16) | (green << 8) | 0;
                    } else if (viewMode == VIEW_MODE_MINERAL) {
                        blue = 255 - (int) (bot.mineral * 0.5);
                        if (blue < 0) blue = 0;
                        rgb[bot.y * width + bot.x] = (255 << 24) | (0 << 16) | (255 << 8) | blue;
                    } else if (viewMode == VIEW_MODE_COMBINED) {
                        green = (int) (bot.c_green * (1 - bot.health * 0.0005));
                        if (green < 0) green = 0;
                        blue = (int) (bot.c_blue * (0.8 - bot.mineral * 0.0005));
                        rgb[bot.y * width + bot.x] = (255 << 24) | (bot.c_red << 16) | (green << 8) | blue;
                    } else if (viewMode == VIEW_MODE_AGE) {
                        red = 255 - (int) (Math.sqrt(bot.age) * 4);
                        if (red < 0) red = 0;
                        rgb[bot.y * width + bot.x] = (255 << 24) | (red << 16) | (0 << 8) | 255;
                    } else if (viewMode == VIEW_MODE_FAMILY) {
                        rgb[bot.y * width + bot.x] = bot.c_family;
                    }
                    w.population++;
                } else if (bot.alive == Bot.STATE_ORGANIC) {                                            // органика, известняк, коралловые рифы
                    if (w.map[bot.x][bot.y] < w.seaLevel) {                     // подводная часть
                        red = 20;
                        blue = 160 - (w.seaLevel - w.map[bot.x][bot.y]) * 2;
                        green = 170 - (w.seaLevel - w.map[bot.x][bot.y]) * 4;
                        if (blue < 40) blue = 40;
                        if (green < 20) green = 20;
                    } else {                                    // скелетики, трупики на суше
                        red = (int) (80 + (w.map[bot.x][bot.y] - w.seaLevel) * 2.5);   // надводная часть
                        green = (int) (60 + (w.map[bot.x][bot.y] - w.seaLevel) * 2.6);
                        blue = 30 + (w.map[bot.x][bot.y] - w.seaLevel) * 3;
                        if (red > 255) red = 255;
                        if (blue > 255) blue = 255;
                        if (green > 255) green = 255;
                    }
                    rgb[bot.y * width + bot.x] = (255 << 24) | (red << 16) | (green << 8) | blue;
                    w.organic++;
                }
            }
        }
    }

    private void drawLinked(World w, int[] rgb) {
        final int width = w.width;
        while (w.currentbot != w.zerobot) {
            int red, green, blue;
            if (w.currentbot.alive == Bot.STATE_ALIVE) {                      // живой бот
                final int viewMode = this.viewMode;
                if (viewMode == VIEW_MODE_BASE) {
                    rgb[w.currentbot.y * width + w.currentbot.x] = (255 << 24) | (w.currentbot.c_red << 16) | (w.currentbot.c_green << 8) | w.currentbot.c_blue;
                } else if (viewMode == VIEW_MODE_ENERGY) {
                    green = 255 - (int) (w.currentbot.health * 0.25);
                    if (green < 0) green = 0;
                    rgb[w.currentbot.y * width + w.currentbot.x] = (255 << 24) | (255 << 16) | (green << 8) | 0;
                } else if (viewMode == VIEW_MODE_MINERAL) {
                    blue = 255 - (int) (w.currentbot.mineral * 0.5);
                    if (blue < 0) blue = 0;
                    rgb[w.currentbot.y * width + w.currentbot.x] = (255 << 24) | (0 << 16) | (255 << 8) | blue;
                } else if (viewMode == VIEW_MODE_COMBINED) {
                    green = (int) (w.currentbot.c_green * (1 - w.currentbot.health * 0.0005));
                    if (green < 0) green = 0;
                    blue = (int) (w.currentbot.c_blue * (0.8 - w.currentbot.mineral * 0.0005));
                    rgb[w.currentbot.y * width + w.currentbot.x] = (255 << 24) | (w.currentbot.c_red << 16) | (green << 8) | blue;
                } else if (viewMode == VIEW_MODE_AGE) {
                    red = 255 - (int) (Math.sqrt(w.currentbot.age) * 4);
                    if (red < 0) red = 0;
                    rgb[w.currentbot.y * width + w.currentbot.x] = (255 << 24) | (red << 16) | (0 << 8) | 255;
                } else if (viewMode == VIEW_MODE_FAMILY) {
                    rgb[w.currentbot.y * width + w.currentbot.x] = w.currentbot.c_family;
                }
                w.population++;
            } else if (w.currentbot.alive == Bot.STATE_ORGANIC) {                                            // органика, известняк, коралловые рифы
                if (w.map[w.currentbot.x][w.currentbot.y] < w.seaLevel) {                     // подводная часть
                    red = 20;
                    blue = 160 - (w.seaLevel - w.map[w.currentbot.x][w.currentbot.y]) * 2;
                    green = 170 - (w.seaLevel - w.map[w.currentbot.x][w.currentbot.y]) * 4;
                    if (blue < 40) blue = 40;
                    if (green < 20) green = 20;
                } else {                                    // скелетики, трупики на суше
                    red = (int) (80 + (w.map[w.currentbot.x][w.currentbot.y] - w.seaLevel) * 2.5);   // надводная часть
                    green = (int) (60 + (w.map[w.currentbot.x][w.currentbot.y] - w.seaLevel) * 2.6);
                    blue = 30 + (w.map[w.currentbot.x][w.currentbot.y] - w.seaLevel) * 3;
                    if (red > 255) red = 255;
                    if (blue > 255) blue = 255;
                    if (green > 255) green = 255;
                }
                rgb[w.currentbot.y * width + w.currentbot.x] = (255 << 24) | (red << 16) | (green << 8) | blue;
                w.organic++;
            }
            w.currentbot = w.currentbot.next;
        }
        w.currentbot = w.currentbot.next;
    }
}
