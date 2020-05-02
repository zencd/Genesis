import java.util.Random;
import java.util.function.Supplier;

/**
 * Main class.
 * Координирует движок симуляции и GUI.
 */
public final class Main implements GuiManager.Callback, Consts {

    private final GuiManager gui;
    private final World world;

    public Main() {
        final Supplier<Long> mapSeeder = () -> new Random(PREDICTABLE_RANDOM ? 111L : new Random().nextLong()).nextLong();
        world = new World(mapSeeder);
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
        gui.paintEverything();
    }

    @Override
    public void seaLevelChanged(int value) {
        final World w = world;
        w.seaLevel = value;
        if (w.map != null) {
            gui.paintEverything();
        }
    }

    @Override
    public boolean startedOrStopped() {
        final World w = world;
        if (w.isStarted()) {
            w.stop();
            gui.stopPaintThread();
            return false;
        } else {
            w.start(gui);
            gui.startPaintThread();
            return true;
        }
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
    }

}
