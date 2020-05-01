import java.util.Random;

/**
 * Main class.
 * Координирует движок симуляции и GUI.
 */
public class Main implements GuiManager.Callback, Consts {

    private final GuiManager gui;
    private final World world;
    private Thread paintThread;
    private boolean paintThreadActive = false;

    public Main() {
        double[] randArray = Utils.makePreCalcRandom(new Random(1000L));
        world = new World(randArray, ()->new Random(111L).nextLong());
        gui = new GuiManager(world, this);
        gui.init();
    }

    @Override
    public void drawStepChanged(int value) {
        this.gui.drawstep = value;
    }

    @Override
    public void mapGenerationInitiated(int canvasWidth, int canvasHeight) {
        final World w = world;
        w.mapGenerationInitiated(canvasWidth, canvasHeight);
        gui.paintMap();
        gui.paintWorld();
    }

    @Override
    public void seaLevelChanged(int value) {
        final World w = world;
        w.sealevel = value;
        if (w.map != null) {
            gui.paintMap();
            gui.paintWorld();
        }
    }

    @Override
    public boolean startedOrStopped() {
        final World w = world;
        if (w.isStarted()) {
            w.stop();
            stopPaintThread();
            return false;
        } else {
            w.start(gui);
            startPaintThread();
            return true;
        }
    }

    class Painter extends Thread {
        public void run() {
            try {
                while (world.currentbot == null) {
                    Thread.sleep(5);
                }
                while (paintThreadActive) {
                    gui.paintWorld();
                    Thread.sleep(15);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void startPaintThread() {
        paintThread = new Painter();
        paintThreadActive = true;
        //paintThread.start();
    }

    void stopPaintThread() {
        paintThread = null;
        Utils.joinSafe(paintThread);
    }

    @Override
    public void viewModeChanged(int viewMode) {
        this.gui.viewMode = viewMode;
    }

    @Override
    public void perlinChanged(int value) {
        world.perlinValue = value;
    }

    public static void main(String[] args) {
        new Main();
        //simulation.generateMap();
        //simulation.generateAdam();
        //simulation.run();
    }

}
