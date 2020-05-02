import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MapGenerator implements Consts {

    static void generateMap(World world) {
        final int[][] map = new int[world.width][world.height];
        final int[] mapInGPU = new int[world.width * world.height];

        final float f = (float) world.perlinValue;
        final Perlin2D perlin = new Perlin2D(world.mapSeeder.get());
        final long start = System.currentTimeMillis();
        final int numThreads = Math.max(NUM_PROCESSORS - 1, 1);
        final int sliceWidth = world.width / numThreads;
        final List<Thread> threads = new ArrayList<>(0);
        for (int i = 0, xStart = 0; i < numThreads; i++) {
            final int xStartFinal = xStart;
            final int xEnd;
            {
                final int xEndTmp = (i == numThreads - 1) ? world.width : (xStart + sliceWidth);
                xEnd = Math.min(xEndTmp, world.width);
            }
            final Runnable runnable = () -> {
                for (int x = xStartFinal; x < xEnd; x++) {
                    for (int y = 0; y < world.height; y++) {
                        final float value = perlin.getNoise(x / f, y / f, 8, 0.45f);        // вычисляем точку ландшафта
                        final int intValue = (int) (value * 255 + 128) & 255;
                        map[x][y] = intValue;
                        mapInGPU[y * world.width + x] = map[x][y];
                    }
                }
            };
            final Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
            xStart = xEnd;
        }
        Utils.joinSafe(threads);
        System.err.println("map generated for " + (System.currentTimeMillis() - start) + " ms");
        world.mapInGPU = mapInGPU;
        world.map = map;
    }

    static Point findStartPoint(World world) {
        int x = world.width / 2;
        int y = world.height / 2;
        int[][] vectors = {
                {0, +1},
                {-1, 0},
                {0, -1},
                {+1, 0},
        };
        int stepsPerVector = 2;
        final int space = 5;
        while (true) {
            for (int[] vector : vectors) {
                for (int i = 0; i < stepsPerVector; i++) {
                    x += vector[0] * space;
                    y += vector[1] * space;
                    //System.err.println("x: " + x + ", y: " + y);
                    final int level = world.map[x][y];
                    if (level > world.seaLevel) {
                        return new Point(x, y);
                    }
                }
            }
            stepsPerVector += 2 * space;
            x += space;
            y -= space;
        }
    }
}
