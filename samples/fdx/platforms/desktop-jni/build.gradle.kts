import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
}

val glRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val vulkanRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val wgpuJniRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, LibExt.javaFFMTarget.toInt())
    }
}

val wgpuFfmRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, LibExt.javaFFMTarget.toInt())
    }
}

val joltJniRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val joltFfmRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, LibExt.javaFFMTarget.toInt())
    }
}

dependencies {
    implementation(project(":samples:fdx:core"))
    implementation("${LibExt.fdxGroup}:backend_desktop:${LibExt.fdxVersion}")
    implementation("${LibExt.fdxGroup}:wgpu_core:${LibExt.fdxVersion}")

    if(LibExt.useRepoLibs) {
        joltJniRuntimeClasspath("com.github.xpenatan.jJolt:desktop-jni:${LibExt.exampleVersion}")
        joltFfmRuntimeClasspath("com.github.xpenatan.jJolt:desktop-ffm:${LibExt.exampleVersion}")
        implementation("com.github.xpenatan.jJolt:jolt-fdx:${LibExt.exampleVersion}")
    }
    else {
        joltJniRuntimeClasspath(project(":jolt:desktop:jni"))
        joltFfmRuntimeClasspath(project(":jolt:desktop:ffm"))
        implementation(project(":extensions:fdx"))
    }

    glRuntimeClasspath("${LibExt.fdxGroup}:gl_desktop:${LibExt.fdxVersion}")
    vulkanRuntimeClasspath("${LibExt.fdxGroup}:vulkan_desktop:${LibExt.fdxVersion}")
    wgpuJniRuntimeClasspath("${LibExt.fdxGroup}:wgpu_desktop_jni:${LibExt.fdxVersion}")
    wgpuFfmRuntimeClasspath("${LibExt.fdxGroup}:wgpu_desktop_ffm:${LibExt.fdxVersion}")
}

val sampleMainClass = "jolt.example.samples.app.desktop.JoltDesktopLauncher"
val assetsDir = file("../../assets")

fun JavaExec.configureSampleRun(
    descriptionText: String,
    graphics: String,
    graphicsLabel: String,
    providerClasspath: FileCollection,
    joltRuntimeClasspath: FileCollection
) {
    group = "application"
    description = descriptionText
    classpath = joltRuntimeClasspath + sourceSets["main"].runtimeClasspath + providerClasspath
    mainClass.set(sampleMainClass)
    workingDir = assetsDir
    systemProperty("jjolt.sample.graphics", graphics)
    systemProperty("jjolt.sample.graphicsLabel", graphicsLabel)
    System.getProperty("jjolt.sample.exitAfterFrames")?.takeIf { it.isNotBlank() }?.let {
        systemProperty("jjolt.sample.exitAfterFrames", it)
    }
    System.getProperty("jjolt.sample.visible")?.takeIf { it.isNotBlank() }?.let {
        systemProperty("jjolt.sample.visible", it)
    }
    jvmArgs("-Dorg.lwjgl.system.stackSize=1048576")
    if(JavaVersion.current().majorVersion.toInt() >= 22) {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

fun JavaExec.useJava25Launcher() {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(LibExt.javaFFMTarget.toInt()))
    })
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

fun registerDesktopSampleBuild(taskName: String, descriptionText: String, providerClasspath: FileCollection,
        joltRuntimeClasspath: FileCollection) {
    tasks.register(taskName) {
        group = "application"
        description = descriptionText
        dependsOn("classes")
        inputs.files(providerClasspath)
        inputs.files(joltRuntimeClasspath)
    }
}

registerDesktopSampleBuild("jolt_sample_desktop_gl_jni_build",
    "Builds the jJolt libfdx desktop OpenGL sample with Jolt JNI.", glRuntimeClasspath, joltJniRuntimeClasspath)
registerDesktopSampleBuild("jolt_sample_desktop_wgpu_jni_build",
    "Builds the jJolt libfdx desktop WGPU sample with Jolt JNI.", wgpuJniRuntimeClasspath, joltJniRuntimeClasspath)
registerDesktopSampleBuild("jolt_sample_desktop_wgpu_ffm_build",
    "Builds the jJolt libfdx desktop WGPU sample with Jolt FFM.", wgpuFfmRuntimeClasspath, joltFfmRuntimeClasspath)
registerDesktopSampleBuild("jolt_sample_desktop_vulkan_jni_build",
    "Builds the jJolt libfdx desktop Vulkan sample with Jolt JNI.", vulkanRuntimeClasspath, joltJniRuntimeClasspath)
registerDesktopSampleBuild("jolt_sample_desktop_vulkan_ffm_build",
    "Builds the jJolt libfdx desktop Vulkan sample with Jolt FFM.", vulkanRuntimeClasspath, joltFfmRuntimeClasspath)

tasks.register<JavaExec>("jolt_sample_desktop_gl_jni_run") {
    configureSampleRun("Runs the jJolt libfdx desktop OpenGL sample with Jolt JNI.",
        "gl", "OpenGL", glRuntimeClasspath, joltJniRuntimeClasspath)
    dependsOn("jolt_sample_desktop_gl_jni_build")
    useJava25Launcher()
}

tasks.register<JavaExec>("jolt_sample_desktop_wgpu_jni_run") {
    configureSampleRun("Runs the jJolt libfdx desktop WGPU sample with Jolt JNI.",
        "wgpu", "WGPU JNI", wgpuJniRuntimeClasspath, joltJniRuntimeClasspath)
    dependsOn("jolt_sample_desktop_wgpu_jni_build")
    useJava25Launcher()
}

tasks.register<JavaExec>("jolt_sample_desktop_wgpu_ffm_run") {
    configureSampleRun("Runs the jJolt libfdx desktop WGPU sample with Jolt FFM.",
        "wgpu", "WGPU FFM", wgpuFfmRuntimeClasspath, joltFfmRuntimeClasspath)
    dependsOn("jolt_sample_desktop_wgpu_ffm_build")
    useJava25Launcher()
}

tasks.register<JavaExec>("jolt_sample_desktop_vulkan_jni_run") {
    configureSampleRun("Runs the jJolt libfdx desktop Vulkan sample with Jolt JNI.",
        "vulkan", "Vulkan JNI", vulkanRuntimeClasspath, joltJniRuntimeClasspath)
    dependsOn("jolt_sample_desktop_vulkan_jni_build")
    useJava25Launcher()
}

tasks.register<JavaExec>("jolt_sample_desktop_vulkan_ffm_run") {
    configureSampleRun("Runs the jJolt libfdx desktop Vulkan sample with Jolt FFM.",
        "vulkan", "Vulkan FFM", vulkanRuntimeClasspath, joltFfmRuntimeClasspath)
    dependsOn("jolt_sample_desktop_vulkan_ffm_build")
    useJava25Launcher()
}
