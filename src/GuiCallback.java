public interface GuiCallback {
    void drawStepChanged(int value);
    void mapGenerationStarted();
    void seaLevelChanged(int value);
    boolean startedOrStopped();
    void viewModeChanged(int viewMode);
}
