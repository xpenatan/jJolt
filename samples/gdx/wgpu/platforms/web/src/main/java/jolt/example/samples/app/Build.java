package jolt.example.samples.app;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.builder.TeaBuilder;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.teavm.vm.TeaVMOptimizationLevel;

public class Build {

    public static void main(String[] args) {
        AssetFileHandle assetsPath = new AssetFileHandle("../../../assets");
        File outputDir = new File("build/dist");
        boolean built = false;
        try {
            new TeaBuilder(new WebBackend()
                    .setStartJettyAfterBuild(false)
                    .setWebAssembly(true))
                    .addAssets(assetsPath)
                    .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
                    .setMainClass(Launcher.class.getName())
                    .setObfuscated(false)
                    .build(outputDir);
            built = true;
        }
        finally {
            setPageTitle(outputDir, "jJolt Samples - TeaVM Wasm WGPU", built);
        }
    }

    private static void setPageTitle(File outputDir, String title, boolean required) {
        File indexFile = new File(outputDir, "webapp/index.html");
        if(!indexFile.isFile()) {
            if(required) {
                throw new IllegalStateException("Could not find generated TeaVM page: " + indexFile.getAbsolutePath());
            }
            return;
        }
        try {
            String source = new String(Files.readAllBytes(indexFile.toPath()), StandardCharsets.UTF_8);
            String rewritten = source.replaceAll("<title>.*</title>", "<title>" + title + "</title>");
            Files.write(indexFile.toPath(), rewritten.getBytes(StandardCharsets.UTF_8));
        }
        catch(IOException e) {
            throw new RuntimeException("Could not update TeaVM page title", e);
        }
    }
}
