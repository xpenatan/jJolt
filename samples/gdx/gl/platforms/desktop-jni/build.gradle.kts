import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("java")
}

dependencies {
    implementation(project(":samples:gdx:gl:core"))
    implementation(project(":samples:shared"))

    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        implementation(libs.jjoltDesktopJni)
        implementation(libs.jjoltGdxGl)
    }
    else {
        implementation(project(":jolt:desktop:jni"))
        implementation(project(":extensions:gdx:gl"))
    }

    implementation(libs.gdxBackendLwjgl3)
    implementation(variantOf(libs.gdxPlatform) { classifier("natives-desktop") })
    implementation(libs.imguiDesktop)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
}

val mainClassName = "jolt.example.samples.app.Main"
val assetsDir = rootProject.file("samples/assets")

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
