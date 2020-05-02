public interface Consts {
    int VIEW_MODE_BASE = 0;
    int VIEW_MODE_COMBINED = 1;
    int VIEW_MODE_ENERGY = 2;
    int VIEW_MODE_MINERAL = 3;
    int VIEW_MODE_AGE = 4;
    int VIEW_MODE_FAMILY = 5;

    int PERLIN_DEFAULT = 300;
    int SEA_LEVEL_DEFAULT = 145;

    //int NUM_THREADS = 1;
    int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    //boolean PREDICTABLE_RANDOM = true;
    boolean PREDICTABLE_RANDOM = false;
}
