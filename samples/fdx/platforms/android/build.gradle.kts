import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
}

group = "jolt.example.samples.app.android"

android {
    namespace = "jolt.example.samples.app.android"
    compileSdk = 36
    enableKotlin = false

    defaultConfig {
        applicationId = "jolt.example.samples.app.android"
        minSdk = 29
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        named("main") {
            assets.srcDirs(rootProject.file("samples/assets"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaFfmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaFfmTarget.get())
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.androidDesugar)

    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        implementation(libs.jjoltAndroidJni)
        implementation(libs.jjoltJoltFdx)
    }
    else {
        implementation(project(":jolt:android:jni"))
        implementation(project(":extensions:fdx"))
    }

    implementation(project(":samples:fdx:core"))
    implementation(libs.fdxBackendAndroid)
    implementation(libs.fdxWgpuAndroidJni)
    implementation(libs.fdxVulkanAndroidJni)
}

val androidExcludedJParserModules = listOf(
    "runtime-core",
    "runtime-desktop-jni",
    "runtime-desktop-jni_windows_x64",
    "runtime-desktop-jni_linux_x64",
    "runtime-desktop-jni_mac_x64",
    "runtime-desktop-jni_mac_arm64",
    "runtime-jni_windows_x64",
    "runtime-jni_linux_x64",
    "runtime-jni_mac_x64",
    "runtime-jni_mac_arm64"
)

configurations.configureEach {
    androidExcludedJParserModules.forEach {
        exclude(group = "com.github.xpenatan.jParser", module = it)
    }
}

fun adbExecutable(): String {
    val executable = if(System.getProperty("os.name").lowercase().contains("win")) "adb.exe" else "adb"
    val sdkRoots = mutableListOf<String>()
    val localPropertiesFile = rootProject.file("local.properties")
    if(localPropertiesFile.isFile) {
        val localProperties = Properties()
        localPropertiesFile.inputStream().use { localProperties.load(it) }
        localProperties.getProperty("sdk.dir")?.let { sdkRoots += it }
    }
    System.getenv("ANDROID_HOME")?.let { sdkRoots += it }
    System.getenv("ANDROID_SDK_ROOT")?.let { sdkRoots += it }
    sdkRoots.asSequence()
            .map { file("$it/platform-tools/$executable") }
            .firstOrNull { it.isFile }
            ?.let { return it.absolutePath }

    System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .asSequence()
            .map { File(it, executable) }
            .firstOrNull { it.isFile }
            ?.let { return it.absolutePath }

    throw GradleException("Could not find $executable. Set sdk.dir in local.properties, set ANDROID_HOME or ANDROID_SDK_ROOT, or add adb to PATH.")
}

fun registerAndroidBuildTask(name: String, descriptionText: String) {
    tasks.register(name) {
        group = "application"
        description = descriptionText
        dependsOn("assembleDebug")
    }
}

fun registerAndroidRunTask(name: String, activityName: String) {
    tasks.register<Exec>(name) {
        group = "application"
        description = "Installs and launches the jJolt libfdx Android sample."
        dependsOn("installDebug")
        val command = mutableListOf(adbExecutable(), "shell", "am", "start", "-n",
                "jolt.example.samples.app.android/$activityName")
        System.getProperties().stringPropertyNames()
                .filter { it.startsWith("jjolt.sample.") }
                .sorted()
                .forEach { key ->
                    val value = System.getProperty(key)
                    if(!value.isNullOrBlank()) {
                        command.addAll(listOf("--es", key, value))
                    }
                }
        commandLine(command)
    }
}

registerAndroidBuildTask("jolt_sample_android_gles_build", "Builds the jJolt libfdx Android OpenGL ES sample.")
registerAndroidBuildTask("jolt_sample_android_wgpu_jni_build", "Builds the jJolt libfdx Android WGPU JNI sample.")
registerAndroidBuildTask("jolt_sample_android_vulkan_build", "Builds the jJolt libfdx Android Vulkan JNI sample.")

registerAndroidRunTask("jolt_sample_android_gles_run",
        "jolt.example.samples.app.android.JoltAndroidGlesActivity")
registerAndroidRunTask("jolt_sample_android_wgpu_jni_run",
        "jolt.example.samples.app.android.JoltAndroidWgpuActivity")
registerAndroidRunTask("jolt_sample_android_vulkan_run",
        "jolt.example.samples.app.android.JoltAndroidVulkanActivity")
