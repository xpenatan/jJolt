import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("java")
}

dependencies {
    implementation(project(":samples:gdx:gl:core"))
    implementation(project(":samples:shared"))

    if(LibExt.useRepoLibs) {
        implementation("com.github.xpenatan.jJolt:desktop-jni:${LibExt.exampleVersion}")
        implementation("com.github.xpenatan.jJolt:gdx-gl:${LibExt.exampleVersion}")
    }
    else {
        implementation(project(":jolt:desktop:jni"))
        implementation(project(":extensions:gdx:gl"))
    }

    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${LibExt.gdxVersion}")
    implementation("com.badlogicgames.gdx:gdx-platform:${LibExt.gdxVersion}:natives-desktop")
    implementation("com.github.xpenatan.xImGui:imgui-desktop:${LibExt.gdxImGuiVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.java8Target)
    targetCompatibility = JavaVersion.toVersion(LibExt.java8Target)
}

val mainClassName = "jolt.example.samples.app.Main"
val assetsDir = file("../../../assets")

tasks.register<JavaExec>("jolt_samples_run_desktop") {
    group = "jolt_examples_desktop"
    description = "Run desktop app"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = assetsDir

    if(DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }
}
