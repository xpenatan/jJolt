plugins {
    id("java-library")
}

val moduleName = "desktop-jni"
group = "${LibExt.groupId}.desktop"

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

    implementation("com.github.xpenatan.jParser:runtime-desktop-jni:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-jni_windows_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-jni_linux_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-jni_mac_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-jni_mac_arm64:${LibExt.jParserVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaMainTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaMainTarget)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = moduleName
            groupId = LibExt.groupId
            version = LibExt.libVersion
            from(components["java"])
        }
    }
}
