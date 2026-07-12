package jolt.example.samples.app;

import com.github.xpenatan.webgpu.JWebGPUBackend;
import com.monstrous.gdx.webgpu.backends.desktop.WgDesktopApplication;
import com.monstrous.gdx.webgpu.backends.desktop.WgDesktopApplicationConfiguration;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.graphics.WGPUGraphicApi;

public class Main {
    public static void main(String[] args) {
        GraphicManagerApi.graphicApi = new WGPUGraphicApi();

        WgDesktopApplicationConfiguration config = new WgDesktopApplicationConfiguration();
        config.setWindowedMode(1444, 800);
        config.setTitle("jJolt Samples - JVM WGPU");
        config.enableGPUtiming = false;
        config.useVsync(false);
        config.backendWebGPU = JWebGPUBackend.WGPU;
        new WgDesktopApplication(new JoltGame(), config);
    }
}
