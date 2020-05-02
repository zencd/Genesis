import java.text.DecimalFormat;

public class Tests {
    public static void main(String[] args) {
        DecimalFormat INT_FORMAT = new DecimalFormat("#,###");
        System.err.println(INT_FORMAT.format(12345));
    }
}
