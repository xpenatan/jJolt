package jolt.example.samples.app.desktopc;

import com.github.xpenatan.gdx.teavm.backends.glfw.GLFWApplication;
import com.github.xpenatan.gdx.teavm.backends.glfw.GLFWApplicationConfiguration;
import jolt.example.graphics.GdxGraphicApi;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.samples.app.TeaVmJoltGame;

public final class JoltGdxDesktopLauncher {
    private JoltGdxDesktopLauncher() {
    }

    public static void main(String[] args) {
        GraphicManagerApi.graphicApi = new GdxGraphicApi();

        GLFWApplicationConfiguration config = new GLFWApplicationConfiguration();
        config.setTitle("jJolt Samples - TeaVM C GL");
        config.setWindowedMode(960, 540);
        config.useVsync(false);
        config.setForegroundFPS(0);
        System.setProperty("os.name", "Windows");

        new GLFWApplication(new TeaVmJoltGame(false), config);
    }
}
