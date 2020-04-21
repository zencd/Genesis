import com.sun.deploy.util.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class Tests {
    @Test
    public void test() {
        //World world = new World(preGenRandom(), ()->new Random().nextLong());
        World world = new World(preGenRandom(), ()->200L);
        world.mapGenerationInitiated(1000,1000);
        for (int i = 0; i < 1000; i++) {
            world.iterate2();
        }
        String snapshot = takeSnapshot(world);
        System.out.println("snapshot: " + snapshot.hashCode());
        System.out.println(snapshot);
        assertEquals(934993679, snapshot.hashCode());
    }

    public String takeSnapshot(World world) {
        StringBuilder buf = new StringBuilder();
        for (int x = 0; x < world.width; x++) {
            for (int y = 0; y < world.height; y++) {
                Bot bot = world.matrix[x][y];
                if (bot != null && bot.alive == 3) {
                    buf.append(bot.health);
                    buf.append(",");
                    buf.append(genomeHash(bot));
                    buf.append("|");
                }
            }
        }
        return buf.toString();
    }

    private int genomeHash(Bot bot) {
        int result = 1;
        for (byte gene : bot.mind)
            result = 31 * result + gene;
        return result;
    }

    private double[] preGenRandom() {
        //return Utils.makePreCalcRandom();
        double[] buf = new double[10];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = i;
        }
        return buf;
    }
}
