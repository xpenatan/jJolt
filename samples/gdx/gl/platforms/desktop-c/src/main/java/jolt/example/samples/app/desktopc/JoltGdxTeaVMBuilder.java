package jolt.example.samples.app.desktopc;

import com.github.xpenatan.gdx.teavm.backends.glfw.config.backend.TeaGLFWBackend;
import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.ClasspathResourceWalker;
import com.github.xpenatan.gdx.teavm.backends.shared.config.builder.TeaBuilder;
import com.github.xpenatan.gdx.teavm.backends.shared.config.builder.TeaBuilderData;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import org.teavm.vm.TeaVMOptimizationLevel;

public final class JoltGdxTeaVMBuilder {
    private static final String JPARSER_TEAVMC_LINKAGE = "JPARSER_TEAVMC_LINKAGE";
    private static final String JPARSER_JOLT_TEAVMC_LIBRARY = "JPARSER_JOLT_TEAVMC_LIBRARY";
    private static final int NATIVE_MIN_HEAP_SIZE = 64 * 1024 * 1024;
    private static final int NATIVE_MAX_HEAP_SIZE = 1024 * 1024 * 1024;
    private static final int NATIVE_MIN_DIRECT_BUFFER_SIZE = 128 * 1024 * 1024;

    private JoltGdxTeaVMBuilder() {
    }

    public static void main(String[] args) throws IOException {
        TeaGLFWBackend.NativeBuildType buildType = parseBuildType(args);
        String action = args.length > 1 ? normalize(args[1]) : "generate";
        TeaVMCLinkage linkage = parseLinkage(args);
        boolean buildExecutable = action.equals("build") || action.equals("run");
        boolean runExecutable = action.equals("run");
        if(!action.equals("generate") && !buildExecutable) {
            throw new IllegalArgumentException("Expected action argument: generate, build, or run");
        }
        boolean consoleLog = runExecutable && (buildType == TeaGLFWBackend.NativeBuildType.DEBUG || hasConsoleArg(args));

        TeaGLFWBackend backend = new JoltTeaGLFWBackend()
                .setBuildType(buildType)
                .setBuildExecutableAfterBuild(buildExecutable)
                .setRunExecutableAfterBuild(runExecutable)
                .setRunExecutableWithConsoleLog(consoleLog);
        backend.cmakeDefinition(JPARSER_TEAVMC_LINKAGE, linkage.name());
        findGeneratedJoltStaticLibrary().ifPresent(library ->
                backend.cmakeDefinition(JPARSER_JOLT_TEAVMC_LIBRARY, cmakePath(library)));

        new TeaBuilder(backend)
                .addAssets(new AssetFileHandle("../../../assets"))
                .setOutputName("jjolt-gdx")
                .setObfuscated(false)
                .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
                .setMinHeapSize(NATIVE_MIN_HEAP_SIZE)
                .setMaxHeapSize(NATIVE_MAX_HEAP_SIZE)
                .setMinDirectBuffersSize(NATIVE_MIN_DIRECT_BUFFER_SIZE)
                .setMainClass(JoltGdxDesktopLauncher.class.getName())
                .addReflectionClass("com.badlogic.gdx.math.Vector3")
                .build(new File("build/dist/glfw"));
    }

    private static TeaGLFWBackend.NativeBuildType parseBuildType(String[] args) {
        if(args.length == 0) {
            return TeaGLFWBackend.NativeBuildType.DEBUG;
        }
        return TeaGLFWBackend.NativeBuildType.fromString(args[0]);
    }

    private static boolean hasConsoleArg(String[] args) {
        for(int i = 3; i < args.length; i++) {
            String value = normalize(args[i]);
            if(value.equals("console")) {
                return true;
            }
            throw new IllegalArgumentException("Unsupported gdx-teavm GLFW option: " + args[i]);
        }
        return false;
    }

    private static TeaVMCLinkage parseLinkage(String[] args) {
        if(args.length < 3) {
            return TeaVMCLinkage.STATIC;
        }
        return TeaVMCLinkage.valueOf(args[2].trim().toUpperCase(Locale.ROOT));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace("--", "");
    }

    private static java.util.Optional<File> findGeneratedJoltStaticLibrary() throws IOException {
        File repoRoot = findRepoRoot(new File(".").getCanonicalFile());
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        String relativePath;
        if(osName.contains("windows")) {
            relativePath = "jolt/builder/build/c++/libs/windows/vc/teavm_c/jolt64_.lib";
        }
        else if(osName.contains("linux")) {
            relativePath = "jolt/builder/build/c++/libs/linux/teavm_c/libjolt64_.a";
        }
        else if(osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64"))) {
            relativePath = "jolt/builder/build/c++/libs/mac/arm/teavm_c/libjolt64_.a";
        }
        else if(osName.contains("mac")) {
            relativePath = "jolt/builder/build/c++/libs/mac/teavm_c/libjolt64_.a";
        }
        else {
            return java.util.Optional.empty();
        }
        File library = new File(repoRoot, relativePath).getCanonicalFile();
        return library.isFile() ? java.util.Optional.of(library) : java.util.Optional.empty();
    }

    private static File findRepoRoot(File start) {
        File current = start;
        while(current != null) {
            if(new File(current, "settings.gradle.kts").isFile()) {
                return current;
            }
            current = current.getParentFile();
        }
        throw new IllegalStateException("Unable to find repository root from: " + start.getAbsolutePath());
    }

    private static String cmakePath(File file) {
        return file.getAbsolutePath().replace('\\', '/');
    }

    private static boolean isNativeHeaderResource(String lookup) {
        return lookup.endsWith(".inl") || lookup.endsWith(".hpp") || lookup.endsWith(".hh");
    }

    private static final class JoltTeaGLFWBackend extends TeaGLFWBackend {
        @Override
        protected void copyAssets(TeaBuilderData data) {
            super.copyAssets(data);
            try {
                copyJoltNativeHeaderResources();
            }
            catch(IOException e) {
                throw new RuntimeException("Failed to copy jJolt TeaVM C inline headers", e);
            }
        }

        private void copyJoltNativeHeaderResources() throws IOException {
            List<String> resources = ClasspathResourceWalker.listResources(
                    classLoader,
                    "external_cpp/jparser/jolt/source",
                    ClasspathResourceWalker.DEFAULT_FILTER);
            Path outputRoot = new File(buildRootPath, "c").toPath();
            for(String resource : resources) {
                String lookup = resource.startsWith("/") ? resource.substring(1) : resource;
                if(!isNativeHeaderResource(lookup)) {
                    continue;
                }
                try(InputStream input = classLoader.getResourceAsStream(lookup)) {
                    if(input == null) {
                        continue;
                    }
                    Path target = outputRoot.resolve(lookup);
                    Files.createDirectories(target.getParent());
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private enum TeaVMCLinkage {
        STATIC,
        SHARED_LINKED,
        RUNTIME_LOADED
    }
}
