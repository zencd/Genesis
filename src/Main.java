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
        world = new World();
        gui = new GuiManager(world, this);
        gui.init();
    }

    @Override
    public void drawStepChanged(int value) {
        this.gui.drawstep = value;
    }

    @Override
    public void mapGenerationStarted(int canvasWidth, int canvasHeight) {
        final World w = world;
        w.width = canvasWidth / w.zoom;    // Ширина доступной части экрана для рисования карты
        w.height = canvasHeight / w.zoom;
        w.generateMap((int) (Math.random() * 10000));
        w.generateAdam();
        gui.paintMapView();
        gui.paint1();
    }

    @Override
    public void seaLevelChanged(int value) {
        final World w = world;
        w.sealevel = value;
        if (w.map != null) {
            gui.paintMapView();
            gui.paint1();
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
                    gui.paint1();
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
