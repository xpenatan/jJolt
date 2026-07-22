plugins {
    id("java-library")
}

val moduleName = "desktop-c"
val nativeResourceRoot = "external_cpp/jparser/jolt/native"

base {
    archivesName.set(moduleName)
}

val nativeRoot = file("$projectDir/../../builder/build/c++/libs")

data class NativeResource(val sourcePath: String, val platform: String)

val nativeResources = listOf(
    NativeResource("$nativeRoot/mt/windows/vc/teavm_c/jolt64_.lib", "windows_x64/mt/static"),
    NativeResource("$nativeRoot/mt/windows/vc/teavm_c/jolt64.lib", "windows_x64/mt/shared"),
    NativeResource("$nativeRoot/mt/windows/vc/teavm_c/jolt64.dll", "windows_x64/mt/shared"),
    NativeResource("$nativeRoot/md/windows/vc/teavm_c/jolt64_.lib", "windows_x64/md/static"),
    NativeResource("$nativeRoot/md/windows/vc/teavm_c/jolt64.lib", "windows_x64/md/shared"),
    NativeResource("$nativeRoot/md/windows/vc/teavm_c/jolt64.dll", "windows_x64/md/shared"),
    // Preserve the previous layout as an MT compatibility fallback.
    NativeResource("$nativeRoot/mt/windows/vc/teavm_c/jolt64_.lib", "windows_x64"),
    NativeResource("$nativeRoot/mt/windows/vc/teavm_c/jolt64.lib", "windows_x64"),
    NativeResource("$nativeRoot/mt/windows/vc/teavm_c/jolt64.dll", "windows_x64"),
    NativeResource("$nativeRoot/linux/teavm_c/libjolt64_.a", "linux_x64"),
    NativeResource("$nativeRoot/linux/teavm_c/libjolt64.so", "linux_x64"),
    NativeResource("$nativeRoot/mac/teavm_c/libjolt64_.a", "mac_x64"),
    NativeResource("$nativeRoot/mac/teavm_c/libjolt64.dylib", "mac_x64"),
    NativeResource("$nativeRoot/mac/arm/teavm_c/libjolt64_.a", "mac_arm64"),
    NativeResource("$nativeRoot/mac/arm/teavm_c/libjoltarm64.dylib", "mac_arm64")
)

tasks.named<Jar>("jar") {
    outputs.upToDateWhen { false }
    nativeResources.forEach { resource ->
        from(provider { listOf(file(resource.sourcePath)).filter { it.exists() } }) {
            into("$nativeResourceRoot/${resource.platform}")
        }
    }
}

dependencies {
    api(project(":jolt:shared:c"))

    implementation("com.github.xpenatan.jParser:runtime-desktop-c_windows_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-c_linux_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-c_mac_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-c_mac_arm64:${LibExt.jParserVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
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
