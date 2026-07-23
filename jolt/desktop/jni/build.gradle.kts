plugins {
    id("java-library")
}

val moduleName = "desktop-jni"

base {
    archivesName.set(moduleName)
}

val nativeRoot = file("$projectDir/../../builder/build/c++/libs")
val nativePaths = listOf(
    "$nativeRoot/windows/vc/jni/jolt64.dll",
    "$nativeRoot/linux/jni/libjolt64.so",
    "$nativeRoot/mac/jni/libjolt64.dylib",
    "$nativeRoot/mac/arm/jni/libjoltarm64.dylib",
)

tasks.named<Jar>("jar") {
    from(provider {
        nativePaths.map(::file).filter { it.exists() }
    })
}

dependencies {
    api(project(":jolt:shared:jni"))

    testImplementation(libs.junit)

    implementation(libs.jparserRuntimeDesktopJni)
    implementation(libs.jparserRuntimeDesktopJniWindowsX64)
    implementation(libs.jparserRuntimeDesktopJniLinuxX64)
    implementation(libs.jparserRuntimeDesktopJniMacX64)
    implementation(libs.jparserRuntimeDesktopJniMacArm64)
}

sourceSets {
    test {
        java.srcDir(file("../../tests/src/test/java"))
    }
}

val hostJniBuildTask = when {
    System.getProperty("os.name").lowercase().contains("win") -> ":jolt:builder:jParser_build_windows64_jni"
    System.getProperty("os.name").lowercase().contains("mac") && System.getProperty("os.arch").lowercase().contains("aarch64") -> ":jolt:builder:jParser_build_macArm_jni"
    System.getProperty("os.name").lowercase().contains("mac") -> ":jolt:builder:jParser_build_mac64_jni"
    else -> ":jolt:builder:jParser_build_linux64_jni"
}

tasks.test {
    dependsOn(hostJniBuildTask, tasks.jar)
    classpath = files(tasks.jar) + classpath
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = moduleName
            from(components["java"])
        }
    }
}
