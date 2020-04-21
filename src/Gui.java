import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Gui extends JFrame implements Consts {

    Image buffer = null;

    final JPanel canvas = new JPanel() {
        public void paint(Graphics g) {
            g.drawImage(buffer, 0, 0, null);
        }
    };

    final JPanel paintPanel = new JPanel(new FlowLayout());
    final JLabel generationLabel = new JLabel("Generation: 0");
    final JLabel populationLabel = new JLabel("Population: 0");
    final JLabel organicLabel = new JLabel("Organic: 0");

    public static final Map<String,Integer> VIEW_MODE_MAP = new HashMap<>();
    static {
        VIEW_MODE_MAP.put("Base", VIEW_MODE_BASE);
        VIEW_MODE_MAP.put("Combined", VIEW_MODE_COMBINED);
        VIEW_MODE_MAP.put("Energy", VIEW_MODE_ENERGY);
        VIEW_MODE_MAP.put("Minerals", VIEW_MODE_MINERAL);
        VIEW_MODE_MAP.put("Age", VIEW_MODE_AGE);
        VIEW_MODE_MAP.put("Family", VIEW_MODE_FAMILY);
    }

    public static final String BUTTON_START = "Start";
    public static final String BUTTON_STOP = "Stop";


    private final JRadioButton baseButton = new JRadioButton("Base", true);
    private final JRadioButton combinedButton = new JRadioButton("Combined", false);
    private final JRadioButton energyButton = new JRadioButton("Energy", false);
    private final JRadioButton mineralButton = new JRadioButton("Minerals", false);
    private final JRadioButton ageButton = new JRadioButton("Age", false);
    private final JRadioButton familyButton = new JRadioButton("Family", false);

    final JSlider perlinSlider = new JSlider (JSlider.HORIZONTAL, 0, 480, 300);
    private final JButton mapButton = new JButton("Create Map");
    private final JSlider sealevelSlider = new JSlider (JSlider.HORIZONTAL, 0, 256, SEA_LEVEL_DEFAULT);
    private final JButton startButton = new JButton(BUTTON_START);
    private final JSlider drawstepSlider = new JSlider (JSlider.HORIZONTAL, 0, 40, DRAW_STEP_DEFAULT);

    private final GuiCallback guiCallback;

    public Gui(GuiCallback guiCallback) {
        this.guiCallback = guiCallback;
    }

    public void init() {
        setTitle("Genesis 1.2.0");
        setSize(new Dimension(1800, 900));
        final Dimension sSize = Toolkit.getDefaultToolkit().getScreenSize(), fSize = getSize();
        if (fSize.height > sSize.height) fSize.height = sSize.height;
        if (fSize.width  > sSize.width) fSize.width = sSize.width;
        //setLocation((sSize.width - fSize.width)/2, (sSize.height - fSize.height)/2);
        setSize(new Dimension(sSize.width, sSize.height));

        setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);
        final Container container = getContentPane();

        paintPanel.setLayout(new BorderLayout());// у этого лейаута приятная особенность - центральная часть растягивается автоматически
        paintPanel.add(canvas, BorderLayout.CENTER);// добавляем нашу карту в центр
        container.add(paintPanel);

        final JPanel statusPanel = new JPanel(new FlowLayout());
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        //statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        container.add(statusPanel, BorderLayout.SOUTH);

        final Border statusCellBorder = new CompoundBorder(BorderFactory.createLoweredBevelBorder(), new EmptyBorder(0,4,0,4));

        generationLabel.setPreferredSize(new Dimension(200, 18));
        generationLabel.setBorder(statusCellBorder);
        statusPanel.add(generationLabel);
        populationLabel.setPreferredSize(new Dimension(200, 18));
        populationLabel.setBorder(statusCellBorder);
        statusPanel.add(populationLabel);
        organicLabel.setPreferredSize(new Dimension(140, 18));
        organicLabel.setBorder(statusCellBorder);
        statusPanel.add(organicLabel);

        final JToolBar toolbar = new JToolBar();
        toolbar.setOrientation(SwingConstants.VERTICAL);
//        toolbar.setBorderPainted(true);
//        toolbar.setBorder(BorderFactory.createLoweredBevelBorder());
        container.add(toolbar, BorderLayout.WEST);

        final JLabel slider1Label = new JLabel("Map scale");
        toolbar.add(slider1Label);

        perlinSlider.setMajorTickSpacing(160);
        perlinSlider.setMinorTickSpacing(80);
        perlinSlider.setPaintTicks(true);
        perlinSlider.setPaintLabels(true);
        perlinSlider.setPreferredSize(new Dimension(100, perlinSlider.getPreferredSize().height));
        perlinSlider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        toolbar.add(perlinSlider);

        //mapButton.addActionListener(new World.mapButtonAction());
        toolbar.add(mapButton);

        final JLabel slider2Label = new JLabel("Sea level");
        toolbar.add(slider2Label);

        //sealevelSlider.addChangeListener(new World.sealevelSliderChange());
        sealevelSlider.setMajorTickSpacing(128);
        sealevelSlider.setMinorTickSpacing(64);
        sealevelSlider.setPaintTicks(true);
        sealevelSlider.setPaintLabels(true);
        sealevelSlider.setPreferredSize(new Dimension(100, sealevelSlider.getPreferredSize().height));
        sealevelSlider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        toolbar.add(sealevelSlider);

        //startButton.addActionListener(new World.startButtonAction());
        toolbar.add(startButton);

        final JLabel slider3Label = new JLabel("Draw step");
        toolbar.add(slider3Label);

        //drawstepSlider.addChangeListener(new World.drawstepSliderChange());
        drawstepSlider.setMajorTickSpacing(10);
//        drawstepSlider.setMinimum(1);
//        drawstepSlider.setMinorTickSpacing(64);
        drawstepSlider.setPaintTicks(true);
        drawstepSlider.setPaintLabels(true);
        drawstepSlider.setPreferredSize(new Dimension(100, sealevelSlider.getPreferredSize().height));
        drawstepSlider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        toolbar.add(drawstepSlider);

        final ButtonGroup group = new ButtonGroup();
        List<AbstractButton> radioButtons = Arrays.asList(baseButton, combinedButton, energyButton, mineralButton, ageButton, familyButton);
        for (AbstractButton radioButton : radioButtons) {
            group.add(radioButton);
            toolbar.add(radioButton);
        }

        drawstepSlider.addChangeListener(e -> {
            int ds = drawstepSlider.getValue();
            if (ds == 0) ds = 1;
            guiCallback.drawStepChanged(ds);
        });

        mapButton.addActionListener(e -> guiCallback.mapGenerationStarted());

        sealevelSlider.addChangeListener(event -> guiCallback.seaLevelChanged(sealevelSlider.getValue()));

        startButton.addActionListener(e -> {
            boolean started = guiCallback.startedOrStopped();
            perlinSlider.setEnabled(!started);
            mapButton.setEnabled(!started);
            startButton.setText(started ? BUTTON_STOP : BUTTON_START);
        });

        final ActionListener radioListener = e -> {
            String action = e.getActionCommand();
            Integer mode = VIEW_MODE_MAP.get(action);
            if (mode != null) {
                guiCallback.viewModeChanged(mode);
            }
        };

        for (AbstractButton radioButton : radioButtons) {
            radioButton.addActionListener(radioListener);
        }

        this.pack();
        //this.setSize(1600, 700);
        setExtendedState(MAXIMIZED_BOTH);
        this.setVisible(true);
    }
}
