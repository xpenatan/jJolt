import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("java")
}

dependencies {
    implementation(project(":samples:gdx:wgpu:core"))
    implementation(project(":samples:shared"))

    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        implementation(libs.jjoltDesktopJni)
        implementation(libs.jjoltGdxWgpu)
    }
    else {
        implementation(project(":jolt:desktop:jni"))
        implementation(project(":extensions:gdx:wgpu"))
    }

    implementation(libs.gdxWebgpuBackendDesktop)
    implementation(variantOf(libs.gdxPlatform) { classifier("natives-desktop") })
    implementation(libs.imguiDesktop)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java11Target.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.java11Target.get())
}

val mainClassName = "jolt.example.samples.app.Main"
val assetsDir = rootProject.file("samples/assets")

tasks.register<JavaExec>("jolt_samples_run_desktop_wgpu") {
    group = "jolt_examples_desktop"
    description = "Run WebGPU desktop app"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = assetsDir

    if(DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }
}
