plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
}

val joltRuntimeName = "c"
val joltRuntimeProject = ":jolt:desktop:c"
val joltSharedCProject = ":jolt:shared:c"
val teaVMBuilderMainClass = "jolt.example.samples.app.desktopc.JoltGdxTeaVMBuilder"
val glfwBuildRoot = layout.buildDirectory.dir("dist/glfw")
val joltDesktopCJar = project(joltRuntimeProject).tasks.named<Jar>("jar").flatMap { it.archiveFile }
val teavmCNativeRuntimeClasspath = files(joltDesktopCJar)

val joltRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val joltDesktopCNativeJarClasspath = files(joltDesktopCJar)

dependencies {
    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    implementation(project(":samples:gdx:gl:core"))
    implementation(project(":samples:shared"))
    implementation(project(joltSharedCProject))
    implementation(project(joltRuntimeProject))
    implementation("com.github.xpenatan.gdx-teavm:backend-glfw:${LibExt.gdxTeaVMVersion}")

    runtimeOnly("com.github.xpenatan.jParser:runtime-desktop-c_windows_x64:${LibExt.jParserVersion}")
    runtimeOnly("com.github.xpenatan.jParser:runtime-desktop-c_linux_x64:${LibExt.jParserVersion}")
    runtimeOnly("com.github.xpenatan.jParser:runtime-desktop-c_mac_x64:${LibExt.jParserVersion}")
    runtimeOnly("com.github.xpenatan.jParser:runtime-desktop-c_mac_arm64:${LibExt.jParserVersion}")
    joltRuntimeClasspath("com.github.xpenatan.jParser:runtime-desktop-c_windows_x64:${LibExt.jParserVersion}")
    joltRuntimeClasspath("com.github.xpenatan.jParser:runtime-desktop-c_linux_x64:${LibExt.jParserVersion}")
    joltRuntimeClasspath("com.github.xpenatan.jParser:runtime-desktop-c_mac_x64:${LibExt.jParserVersion}")
    joltRuntimeClasspath("com.github.xpenatan.jParser:runtime-desktop-c_mac_arm64:${LibExt.jParserVersion}")
    runtimeOnly(joltDesktopCNativeJarClasspath)
    joltRuntimeClasspath(project(joltRuntimeProject))
    joltRuntimeClasspath(joltDesktopCNativeJarClasspath)

    implementation("org.teavm:teavm-tooling:${LibExt.teaVMVersion}")
    implementation("org.teavm:teavm-classlib:${LibExt.teaVMVersion}")
}

fun currentHostJoltCBuildTask(): String? {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    return when {
        osName.contains("windows") -> ":jolt:builder:jParser_build_windows64_teavm_c"
        osName.contains("linux") -> ":jolt:builder:jParser_build_linux64_teavm_c"
        osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64")) ->
            ":jolt:builder:jParser_build_macArm_teavm_c"
        osName.contains("mac") -> ":jolt:builder:jParser_build_mac64_teavm_c"
        else -> null
    }
}

val hostJoltCBuildTask = currentHostJoltCBuildTask()
    ?: throw GradleException("TeaVM C samples are not configured for ${System.getProperty("os.name")}/${System.getProperty("os.arch")}")

project(joltRuntimeProject).tasks.named("jar") {
    mustRunAfter(hostJoltCBuildTask)
}

val prepareGdxTeaVMGlfwBuildRoot = tasks.register("prepareGdxTeaVMGlfwBuildRoot") {
    group = "samples"
    description = "Creates the gdx-teavm GLFW build root before the native build task validates inputs."
    inputs.property("joltCMakeLayoutVersion", 1)
    outputs.dir(glfwBuildRoot)
    outputs.upToDateWhen { false }

    doLast {
        val buildRoot = glfwBuildRoot.get().asFile
        delete(buildRoot.resolve("c"))
        delete(buildRoot.resolve("build/cmake"))
        buildRoot.mkdirs()
    }
}

fun Task.configureGraphicalRuntimeInputs() {
    dependsOn("classes")
    dependsOn(prepareGdxTeaVMGlfwBuildRoot)
    currentHostJoltCBuildTask()?.let { nativeBuildTask ->
        dependsOn(nativeBuildTask)
        project(joltSharedCProject).tasks.named("processResources") {
            mustRunAfter(nativeBuildTask)
        }
        project(joltSharedCProject).tasks.named("jar") {
            mustRunAfter(nativeBuildTask)
        }
        project(joltRuntimeProject).tasks.named("jar") {
            mustRunAfter(nativeBuildTask)
        }
    }
    dependsOn("$joltRuntimeProject:jar")
    inputs.files(joltRuntimeClasspath)
}

fun JavaExec.configureGraphicalTeaVM(
    action: String,
    linkage: String = "STATIC",
    buildType: String = "Debug",
    console: Boolean = false
) {
    mainClass.set(teaVMBuilderMainClass)
    classpath = sourceSets["main"].runtimeClasspath + joltRuntimeClasspath
    workingDir = projectDir
    maxHeapSize = "2048m"
    configureGraphicalRuntimeInputs()
    args(buildType, action, linkage)
    if(console) {
        args("console")
    }
}

tasks.register<JavaExec>("jolt_gdx_desktop_${joltRuntimeName}_generate") {
    group = "jolt_examples_desktop"
    description = "Generates the jJolt libGDX desktop sample through gdx-teavm GLFW C."
    configureGraphicalTeaVM("generate")
}

tasks.register<JavaExec>("jolt_gdx_desktop_${joltRuntimeName}_build") {
    group = "jolt_examples_desktop"
    description = "Builds the jJolt libGDX desktop sample through gdx-teavm GLFW C."
    configureGraphicalTeaVM("build")
}

tasks.register<JavaExec>("jolt_gdx_desktop_${joltRuntimeName}_run") {
    group = "jolt_examples_desktop"
    description = "Runs the jJolt libGDX desktop sample through gdx-teavm GLFW C."
    configureGraphicalTeaVM("run", console = true)
}

tasks.register<JavaExec>("samples_build_app_teavm_c") {
    group = "example-desktop"
    description = "Build and run the jJolt headless sample as a TeaVM C native executable"
    dependsOn(
        hostJoltCBuildTask,
        ":jolt:shared:c:classes",
        ":jolt:shared:c:processResources",
        ":jolt:desktop:c:jar",
        "classes"
    )
    mainClass.set("BuildTeaVMC")
    classpath = sourceSets["main"].runtimeClasspath + teavmCNativeRuntimeClasspath
    doFirst {
        args = listOf(classpath.asPath)
    }
    workingDir = projectDir
}
