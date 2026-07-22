package jolt.example.samples.app.web;

import io.github.libfdx.backend.web.WebApplicationBackend;
import io.github.libfdx.backend.web.WebApplicationConfig;
import io.github.libfdx.graphics.gl.web.WebGLProvider;
import jolt.example.samples.app.JoltSampleApplication;

public final class JoltWebWasmLauncher {
    private static final String CANVAS_ID = "libfdx-canvas";

    private JoltWebWasmLauncher() {
    }

    public static void main(String[] args) {
        long exitAfterFrames = Long.parseLong(option(args, "--exit-after-frames=", "0"));
        boolean debug = Boolean.parseBoolean(option(args, "--debug=", "true"));
        WebApplicationConfig config = new WebApplicationConfig()
                .title("jJolt libfdx - Wasm WebGL")
                .size(0, 0)
                .canvasId(CANVAS_ID)
                .graphics(new WebGLProvider());

        new WebApplicationBackend().start(config, new JoltSampleApplication(exitAfterFrames, debug));
    }

    private static String option(String[] args, String prefix, String fallback) {
        if (args == null) {
            return fallback;
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return fallback;
    }
}
