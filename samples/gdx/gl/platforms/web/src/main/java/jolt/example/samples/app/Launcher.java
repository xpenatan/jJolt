package jolt.example.samples.app;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import jolt.example.graphics.GdxGraphicApi;
import jolt.example.graphics.GraphicManagerApi;

public class Launcher {

    public static void main(String[] args) {
        GraphicManagerApi.graphicApi = new GdxGraphicApi();

        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.useDebugGL = false;
        config.width = 0;
        config.height = 0;
        config.useGL30 = true;
        new WebApplication(new TeaVmJoltGame(), config);
    }
}
