import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.diagnostics.ProblemTextConsumer;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.vm.TeaVMOptimizationLevel;

public class BuildTeaVMC {
    private static final String MAIN_CLASS = "jolt.example.samples.app.teavmc.TeaVMCSampleMain";
    private static final String TARGET_NAME = "jjolt_samples_teavmc";
    private static List<File> resolvedClassPath;

    public static void main(String[] args) throws Exception {
        if(args.length > 0 && !args[0].isBlank()) {
            resolvedClassPath = parseClassPath(args[0]);
        }

        File projectDir = new File(".").getCanonicalFile();
        File buildRoot = new File(projectDir, "build/teavm-c");
        File generatedDir = new File(buildRoot, "generated");
        File resourcesDir = new File(buildRoot, "resources");
        File cmakeDir = new File(buildRoot, "cmake");
        File nativeBuildDir = new File(buildRoot, "native-build");

        deleteDirectory(buildRoot);
        generatedDir.mkdirs();
        resourcesDir.mkdirs();
        cmakeDir.mkdirs();

        System.out.println("Generating jJolt TeaVM C sample");
        generateTeaVMC(generatedDir);
        patchGeneratedUCharHeader(generatedDir);
        if(isWindows()) {
            patchGeneratedMSVCTimeFunctions(generatedDir);
        }

        System.out.println("Extracting TeaVM C external_cpp resources");
        extractExternalCppResources(resourcesDir.toPath());
        verifyRequiredResource(resourcesDir, "external_cpp/cmake/post_target/jparser_00_teavmc_loader.cmake");
        verifyRequiredResource(resourcesDir, "external_cpp/cmake/post_target/jparser_runtime_teavm_c.cmake");
        verifyRequiredResource(resourcesDir, "external_cpp/cmake/post_target/jparser_jolt_teavm_c.cmake");
        String hostClassifier = hostClassifier();
        String staticExtension = isWindows()? ".lib" : ".a";
        String libraryPrefix = isWindows()? "" : "lib";
        verifyRequiredResource(resourcesDir, "external_cpp/jparser/runtime/native/" + hostClassifier + "/"
                + libraryPrefix + "runtime64_" + staticExtension);
        verifyRequiredResource(resourcesDir, "external_cpp/jparser/jolt/native/" + hostClassifier + "/"
                + libraryPrefix + "jolt64_" + staticExtension);

        writeCMakeProject(cmakeDir, generatedDir, resourcesDir);
        configureCMake(cmakeDir, nativeBuildDir, new File(buildRoot, "cmake-configure.log"));
        runRequired(Arrays.asList("cmake", "--build", nativeBuildDir.getAbsolutePath(), "--config", "Release", "--parallel"),
                projectDir, new File(buildRoot, "cmake-build.log"));

        File executable = findExecutable(nativeBuildDir.toPath());
        System.out.println("Running " + executable.getAbsolutePath());
        runRequired(Arrays.asList(executable.getAbsolutePath()), executable.getParentFile(), new File(buildRoot, "run-output.txt"));
    }

    private static void generateTeaVMC(File outputDir) throws Exception {
        TeaVMTool tool = new TeaVMTool();
        tool.setTargetType(TeaVMTargetType.C);
        tool.setTargetDirectory(outputDir);
        tool.setTargetFileName("jJoltSamplesTeaVMC.c");
        tool.setMainClass(MAIN_CLASS);
        tool.setEntryPointName("main");
        tool.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
        tool.setObfuscated(false);
        tool.setLog(new ConsoleTeaVMToolLog(false));
        tool.setClassPath(currentClassPath());
        tool.generate();

        ProblemProvider problemProvider = tool.getProblemProvider();
        if(problemProvider != null && !problemProvider.getSevereProblems().isEmpty()) {
            throw new IllegalStateException(renderProblems(problemProvider.getSevereProblems()));
        }

        File mainFile = new File(outputDir, "main.c");
        File allFile = new File(outputDir, "all.c");
        if(!mainFile.exists() || !allFile.exists()) {
            throw new IllegalStateException("TeaVM C output was not generated in: " + outputDir.getAbsolutePath());
        }
    }

    private static void patchGeneratedUCharHeader(File outputDir) throws IOException {
        File header = new File(outputDir, "uchar.h");
        String text = Files.readString(header.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        String original = "#else\n\n#include <uchar.h>\n\n#endif\n";
        String replacement = "#else\n\n"
                + "#if defined(_MSC_VER) || defined(__APPLE__)\n"
                + "#include <stdint.h>\n"
                + "typedef uint16_t char16_t;\n"
                + "typedef int char32_t;\n"
                + "#else\n"
                + "#include <uchar.h>\n"
                + "#endif\n\n"
                + "#endif\n";
        if(!text.contains(original)) {
            throw new IllegalStateException("Unable to patch generated TeaVM uchar.h: " + header.getAbsolutePath());
        }
        Files.writeString(header.toPath(), text.replace(original, replacement), StandardCharsets.UTF_8);
    }

    private static void patchGeneratedMSVCTimeFunctions(File outputDir) throws IOException {
        patchText(new File(outputDir, "date.c"), "localtime_s(b, a)", "_localtime64_s(b, a)");
        patchText(new File(outputDir, "time.c"), "gmtime_s(b, a)", "_gmtime64_s(b, a)");
        patchText(new File(outputDir, "time.c"), "localtime_s(b, a)", "_localtime64_s(b, a)");
    }

    private static void patchText(File file, String original, String replacement) throws IOException {
        String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if(!text.contains(original)) {
            throw new IllegalStateException("Unable to patch generated TeaVM C file: " + file.getAbsolutePath());
        }
        Files.writeString(file.toPath(), text.replace(original, replacement), StandardCharsets.UTF_8);
    }

    private static void extractExternalCppResources(Path outputRoot) throws IOException {
        for(File classPathEntry : currentClassPath()) {
            if(classPathEntry.isDirectory()) {
                copyExternalCppDirectory(classPathEntry.toPath(), outputRoot);
            }
            else if(classPathEntry.isFile() && classPathEntry.getName().endsWith(".jar")) {
                copyExternalCppJar(classPathEntry, outputRoot);
            }
        }
    }

    private static void copyExternalCppDirectory(Path classPathRoot, Path outputRoot) throws IOException {
        Path externalCpp = classPathRoot.resolve("external_cpp");
        if(!Files.isDirectory(externalCpp)) {
            return;
        }
        try(var paths = Files.walk(externalCpp)) {
            paths.filter(Files::isRegularFile).forEach(source -> {
                Path relative = classPathRoot.relativize(source);
                copyFile(source, outputRoot.resolve(relative));
            });
        }
    }

    private static void copyExternalCppJar(File jar, Path outputRoot) throws IOException {
        try(JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while(entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if(entry.isDirectory() || !entry.getName().startsWith("external_cpp/")) {
                    continue;
                }
                Path destination = outputRoot.resolve(entry.getName());
                Files.createDirectories(destination.getParent());
                Files.copy(jarFile.getInputStream(entry), destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void copyFile(Path source, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void verifyRequiredResource(File root, String relativePath) {
        File file = new File(root, relativePath);
        if(!file.isFile()) {
            throw new IllegalStateException("Missing required TeaVM C resource: " + file.getAbsolutePath());
        }
    }

    private static void writeCMakeProject(File cmakeDir, File generatedDir, File resourcesDir) throws IOException {
        String generated = toCMakePath(generatedDir);
        String resources = toCMakePath(resourcesDir);
        String text = ""
                + "cmake_minimum_required(VERSION 3.20)\n"
                + "project(jjolt_samples_teavmc C CXX)\n"
                + "set(JPARSER_TEAVMC_APP_TARGET " + TARGET_NAME + ")\n"
                + "set(JPARSER_TEAVMC_GENERATED_SOURCE_ROOT \"" + generated + "\")\n"
                + "if(MSVC)\n"
                + "  set(CMAKE_MSVC_RUNTIME_LIBRARY \"MultiThreaded$<$<CONFIG:Debug>:Debug>DLL\")\n"
                + "endif()\n"
                + "add_executable(" + TARGET_NAME + " \"${JPARSER_TEAVMC_GENERATED_SOURCE_ROOT}/all.c\")\n"
                + "set_target_properties(" + TARGET_NAME + " PROPERTIES C_STANDARD 17 C_STANDARD_REQUIRED YES)\n"
                + "if(MSVC)\n"
                + "  target_compile_options(" + TARGET_NAME + " PRIVATE \"/bigobj\" \"/utf-8\" \"$<$<COMPILE_LANGUAGE:C>:/std:c17>\" \"$<$<COMPILE_LANGUAGE:C>:/TC>\" \"$<$<COMPILE_LANGUAGE:CXX>:/EHsc>\")\n"
                + "  target_compile_definitions(" + TARGET_NAME + " PRIVATE \"_CRT_SECURE_NO_WARNINGS\" \"NOMINMAX\" \"WIN32_LEAN_AND_MEAN\")\n"
                + "endif()\n"
                + "include(\"" + resources + "/external_cpp/cmake/post_target/jparser_00_teavmc_loader.cmake\")\n"
                + "include(\"" + resources + "/external_cpp/cmake/post_target/jparser_runtime_teavm_c.cmake\")\n"
                + "include(\"" + resources + "/external_cpp/cmake/post_target/jparser_jolt_teavm_c.cmake\")\n";
        Files.writeString(new File(cmakeDir, "CMakeLists.txt").toPath(), text, StandardCharsets.UTF_8);
    }

    private static void configureCMake(File cmakeDir, File nativeBuildDir, File logFile) throws IOException, InterruptedException {
        List<List<String>> attempts = new ArrayList<>();
        if(isWindows()) {
            attempts.add(Arrays.asList("cmake", "-S", cmakeDir.getAbsolutePath(), "-B", nativeBuildDir.getAbsolutePath(), "-A", "x64", "-DCMAKE_BUILD_TYPE=Release"));
        }
        attempts.add(Arrays.asList("cmake", "-S", cmakeDir.getAbsolutePath(), "-B", nativeBuildDir.getAbsolutePath(), "-DCMAKE_BUILD_TYPE=Release"));

        IOException lastIo = null;
        InterruptedException lastInterrupted = null;
        int lastExitCode = 0;
        for(int i = 0; i < attempts.size(); i++) {
            deleteDirectory(nativeBuildDir);
            try {
                lastExitCode = runProcess(attempts.get(i), cmakeDir, logFile);
                if(lastExitCode == 0) {
                    return;
                }
            }
            catch(IOException e) {
                lastIo = e;
            }
            catch(InterruptedException e) {
                lastInterrupted = e;
                Thread.currentThread().interrupt();
                break;
            }
        }

        if(lastInterrupted != null) {
            throw lastInterrupted;
        }
        if(lastIo != null) {
            throw lastIo;
        }
        throw new IllegalStateException("CMake configure failed with exit code " + lastExitCode + ". See " + logFile.getAbsolutePath());
    }

    private static File findExecutable(Path nativeBuildDir) throws IOException {
        String executableName = isWindows() ? TARGET_NAME + ".exe" : TARGET_NAME;
        try(var paths = Files.walk(nativeBuildDir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(executableName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to find built executable: " + executableName))
                    .toFile();
        }
    }

    private static void runRequired(List<String> command, File workingDir, File logFile) throws IOException, InterruptedException {
        int exitCode = runProcess(command, workingDir, logFile);
        if(exitCode != 0) {
            throw new IllegalStateException("Command failed with exit code " + exitCode + ": " + String.join(" ", command)
                    + ". See " + logFile.getAbsolutePath());
        }
    }

    private static int runProcess(List<String> command, File workingDir, File logFile) throws IOException, InterruptedException {
        Files.createDirectories(logFile.toPath().getParent());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir);
        builder.redirectErrorStream(true);

        Process process = builder.start();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while((line = reader.readLine()) != null) {
                System.out.println(line);
                writer.write(line);
                writer.newLine();
            }
        }
        return process.waitFor();
    }

    private static void deleteDirectory(File directory) throws IOException {
        if(!directory.exists()) {
            return;
        }
        try(var paths = Files.walk(directory.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        }
                        catch(IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private static List<File> currentClassPath() {
        if(resolvedClassPath != null) {
            return resolvedClassPath;
        }
        return parseClassPath(System.getProperty("java.class.path"));
    }

    private static List<File> parseClassPath(String classPath) {
        String[] entries = classPath.split(File.pathSeparator);
        ArrayList<File> files = new ArrayList<>();
        for(String entry : entries) {
            if(!entry.isEmpty()) {
                files.add(new File(entry));
            }
        }
        return files;
    }

    private static String toCMakePath(File file) throws IOException {
        return file.getCanonicalPath().replace('\\', '/');
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String hostClassifier() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        if(osName.contains("win")) {
            return "windows_x64";
        }
        if(osName.contains("linux")) {
            return "linux_x64";
        }
        if(osName.contains("mac")) {
            return osArch.contains("aarch64") || osArch.contains("arm64")? "mac_arm64" : "mac_x64";
        }
        throw new IllegalStateException("Unsupported TeaVM C host: " + osName + "/" + osArch);
    }

    private static String renderProblems(List<Problem> problems) {
        StringBuilder builder = new StringBuilder("TeaVM C build failed:");
        for(Problem problem : problems) {
            builder.append(System.lineSeparator()).append("- ");
            problem.render(new StringProblemConsumer(builder));
            if(problem.getLocation() != null) {
                builder.append(" at ").append(problem.getLocation());
            }
        }
        return builder.toString();
    }

    private static class StringProblemConsumer implements ProblemTextConsumer {
        private final StringBuilder builder;

        private StringProblemConsumer(StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void append(String text) {
            builder.append(text);
        }

        @Override
        public void appendClass(String className) {
            builder.append(className);
        }

        @Override
        public void appendType(ValueType type) {
            builder.append(type);
        }

        @Override
        public void appendMethod(MethodReference method) {
            builder.append(method);
        }

        @Override
        public void appendField(FieldReference field) {
            builder.append(field);
        }

        @Override
        public void appendLocation(TextLocation location) {
            builder.append(location);
        }
    }
}
