import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("java")
}

dependencies {
    implementation(project(":samples:gdx:wgpu:core"))
    implementation(project(":samples:shared"))

    if(LibExt.useRepoLibs) {
        implementation("com.github.xpenatan.jJolt:desktop-jni:${LibExt.exampleVersion}")
        implementation("com.github.xpenatan.jJolt:gdx-wgpu:${LibExt.exampleVersion}")
    }
    else {
        implementation(project(":jolt:desktop:jni"))
        implementation(project(":extensions:gdx:wgpu"))
    }

    implementation("io.github.monstroussoftware.gdx-webgpu:backend-desktop:${LibExt.gdxWebGPUVersion}")
    implementation("com.badlogicgames.gdx:gdx-platform:${LibExt.gdxVersion}:natives-desktop")
    implementation("com.github.xpenatan.xImGui:imgui-desktop:${LibExt.gdxImGuiVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.java11Target)
    targetCompatibility = JavaVersion.toVersion(LibExt.java11Target)
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
