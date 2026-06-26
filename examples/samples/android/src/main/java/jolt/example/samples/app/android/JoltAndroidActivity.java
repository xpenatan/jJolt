package jolt.example.samples.app.android;

import android.content.Intent;
import io.github.libfdx.application.ApplicationListener;
import io.github.libfdx.backend.android.AndroidApplicationActivity;
import io.github.libfdx.backend.android.AndroidApplicationConfig;
import io.github.libfdx.backend.android.AndroidGlesProvider;
import io.github.libfdx.backend.android.AndroidVulkanProvider;
import io.github.libfdx.graphics.GraphicsAttachmentProvider;
import io.github.libfdx.graphics.wgpu.WGPUProvider;
import jolt.example.samples.app.JoltSampleApplication;

public class JoltAndroidActivity extends AndroidApplicationActivity {
    @Override
    protected AndroidApplicationConfig createApplicationConfig() {
        return new AndroidApplicationConfig()
                .title("xJolt libfdx - " + graphicsDisplayName())
                .size(960, 540)
                .vSync(true)
                .foregroundFps(60)
                .graphics(graphicsProvider());
    }

    @Override
    protected ApplicationListener createApplicationListener() {
        return new JoltSampleApplication(exitAfterFrames());
    }

    protected String graphicsName() {
        return "wgpu";
    }

    protected String graphicsDisplayName() {
        if ("gles".equalsIgnoreCase(graphicsName())) {
            return "OpenGL ES";
        }
        if ("vulkan".equalsIgnoreCase(graphicsName()) || "vk".equalsIgnoreCase(graphicsName())) {
            return "Vulkan JNI";
        }
        return "WGPU JNI";
    }

    private GraphicsAttachmentProvider graphicsProvider() {
        if ("gles".equalsIgnoreCase(graphicsName())) {
            return new AndroidGlesProvider();
        }
        if ("vulkan".equalsIgnoreCase(graphicsName()) || "vk".equalsIgnoreCase(graphicsName())) {
            return new AndroidVulkanProvider();
        }
        return new WGPUProvider();
    }

    private long exitAfterFrames() {
        Intent intent = getIntent();
        String value = intent != null ? intent.getStringExtra("xjolt.sample.exitAfterFrames") : null;
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        return Long.parseLong(value.trim());
    }
}
