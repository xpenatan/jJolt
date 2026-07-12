package jolt.example.samples.app;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.monstrous.gdx.webgpu.backends.teavm.WgTeaApplication;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.graphics.WGPUGraphicApi;

public class Launcher {

    public static void main(String[] args) {
        GraphicManagerApi.graphicApi = new WGPUGraphicApi();

        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width = 0;
        config.height = 0;
        new WgTeaApplication(new TeaVmJoltGame(), config);
    }
}
