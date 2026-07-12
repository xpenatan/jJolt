package jolt.example.samples.app.desktop;

import io.github.libfdx.backend.desktop.DesktopApplicationBackend;
import io.github.libfdx.backend.desktop.DesktopApplicationConfig;
import io.github.libfdx.backend.desktop.DesktopOpenGLProvider;
import io.github.libfdx.backend.desktop.DesktopVulkanProvider;
import io.github.libfdx.graphics.GraphicsAttachmentProvider;
import io.github.libfdx.graphics.wgpu.WGPUProvider;
import jolt.example.samples.app.JoltSampleApplication;

public final class JoltDesktopLauncher {
    private JoltDesktopLauncher() {
    }

    public static void main(String[] args) {
        String graphics = option(args, "--graphics=", System.getProperty("jjolt.sample.graphics", "wgpu"));
        String label = option(args, "--graphics-label=", System.getProperty("jjolt.sample.graphicsLabel"));
        boolean visible = Boolean.parseBoolean(option(args, "--visible=",
                System.getProperty("jjolt.sample.visible", "true")));
        long exitAfterFrames = Long.parseLong(option(args, "--exit-after-frames=",
                System.getProperty("jjolt.sample.exitAfterFrames", "0")));
        boolean debug = Boolean.parseBoolean(option(args, "--debug=",
                System.getProperty("jjolt.sample.debug", "true")));
        DesktopApplicationConfig config = new DesktopApplicationConfig()
                .title("jJolt libfdx - JVM " + graphicsDisplayName(graphics, label))
                .size(960, 540)
                .visible(visible)
                .vSync(true)
                .foregroundFps(60)
                .graphics(graphicsProvider(graphics));

        new DesktopApplicationBackend().start(config, new JoltSampleApplication(exitAfterFrames, debug));
    }

    private static GraphicsAttachmentProvider graphicsProvider(String graphics) {
        if ("gl".equalsIgnoreCase(graphics) || "opengl".equalsIgnoreCase(graphics)) {
            return new DesktopOpenGLProvider();
        }
        if ("vulkan".equalsIgnoreCase(graphics) || "vk".equalsIgnoreCase(graphics)) {
            return new DesktopVulkanProvider();
        }
        return new WGPUProvider();
    }

    private static String graphicsDisplayName(String graphics, String configured) {
        if (configured != null && configured.trim().length() > 0) {
            return configured.trim();
        }
        if ("gl".equalsIgnoreCase(graphics) || "opengl".equalsIgnoreCase(graphics)) {
            return "OpenGL";
        }
        if ("vulkan".equalsIgnoreCase(graphics) || "vk".equalsIgnoreCase(graphics)) {
            return "Vulkan";
        }
        return "WGPU";
    }

    private static String option(String[] args, String prefix, String fallback) {
        if (args == null) {
            return fallback;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg != null && arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return fallback;
    }
}
