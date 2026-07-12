package jolt.example.samples.app;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import jolt.example.graphics.GdxGraphicApi;
import jolt.example.graphics.GraphicManagerApi;

public class Main {
    public static void main(String[] args) {
        GraphicManagerApi.graphicApi = new GdxGraphicApi();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1444, 800);
        config.setTitle("jJolt Samples - JVM GL");
        config.useVsync(false);
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 0);
        new Lwjgl3Application(new JoltGame(), config);
    }
}
