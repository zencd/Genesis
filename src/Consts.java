import java.text.DecimalFormat;

public interface Consts {

    String VERSION = "1.3.0";
    String FRAME_TITLE = "Genesis " + VERSION;

    DecimalFormat INT_FORMAT = new DecimalFormat("#,###");

    int VIEW_MODE_BASE = 0;
    int VIEW_MODE_COMBINED = 1;
    int VIEW_MODE_ENERGY = 2;
    int VIEW_MODE_MINERAL = 3;
    int VIEW_MODE_AGE = 4;
    int VIEW_MODE_FAMILY = 5;

    int PERLIN_DEFAULT = 300;
    int SEA_LEVEL_DEFAULT = 145;

    int SPEED_GENS = 20_000;
    int SPEED_TIME = 60_000;

    int NUM_WORKERS = 1;
    //int NUM_WORKERS = 2;
    //int NUM_WORKERS = 7;
    //int NUM_WORKERS = Runtime.getRuntime().availableProcessors();
    //int NUM_WORKERS = Runtime.getRuntime().availableProcessors() * 2;
    //int NUM_WORKERS = 24;

    int NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();

    boolean PREDICTABLE_RANDOM = true;
    //boolean PREDICTABLE_RANDOM = false;
}
