plugins {
    id("java-library")
}

val moduleName = "desktop-ffm"
group = "${LibExt.groupId}.desktop"

base {
    archivesName.set(moduleName)
}

val libDir = "${projectDir}/../../builder/build/c++/libs"
fun nativeFile(primaryPath: String, fallbackPath: String): String {
    return if(file(primaryPath).exists()) primaryPath else fallbackPath
}

val windowsFile = nativeFile("$libDir/windows/vc/ffm/jolt64.dll",
        "$libDir/windows/vc/jolt64.dll")
val linuxFile = nativeFile("$libDir/linux/ffm/libjolt64.so",
        "$libDir/linux/libjolt64.so")
val macArmFile = nativeFile("$libDir/mac/arm/ffm/libjoltarm64.dylib",
        "$libDir/mac/arm/libjoltarm64.dylib")
val macFile = nativeFile("$libDir/mac/ffm/libjolt64.dylib",
        "$libDir/mac/libjolt64.dylib")

tasks.jar {
    from(windowsFile)
    from(linuxFile)
    from(macArmFile)
    from(macFile)
}

dependencies {
    implementation("com.github.xpenatan.jParser:runtime-desktop-ffm:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-ffm_windows_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-ffm_linux_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-ffm_mac_x64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:runtime-desktop-ffm_mac_arm64:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:api-core:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:loader-core:${LibExt.jParserVersion}")
}

val platforms: MutableMap<String, Jar.() -> Unit> = mutableMapOf()
if(file(windowsFile).exists()) {
    platforms["windows_64"] = { from(windowsFile) }
}
if(file(linuxFile).exists()) {
    platforms["linux_x64"] = { from(linuxFile) }
}
if(file(macFile).exists()) {
    platforms["mac_x64"] = { from(macFile) }
}
if(file(macArmFile).exists()) {
    platforms["mac_arm64"] = { from(macArmFile) }
}

val nativeJars = platforms.map { (classifier, config) ->
    tasks.register<Jar>("nativeJar${classifier}") {
        config()
        archiveClassifier.set(classifier)
    }
}

val nativeDesktopJar = tasks.register<Jar>("nativeJarDesktop") {
    archiveClassifier.set("desktop")
    listOf(
        "windows_64" to windowsFile,
        "linux_x64" to linuxFile,
        "mac_x64" to macFile,
        "mac_arm64" to macArmFile,
    ).forEach { (folder, path) ->
        val nativeFile = file(path)
        if(nativeFile.exists()) {
            from(nativeFile) {
                into(folder)
            }
        }
    }
}

val isPublishingTask = gradle.startParameter.taskNames.any { it.contains("publish", ignoreCase = true) }
val nativeFiles = listOf(windowsFile, linuxFile, macFile, macArmFile).map(::file).filter { it.exists() }

tasks.named<Jar>("jar") {
    // For in-repo project dependencies, keep classes and native payload in the same jar.
    // During publishing, keep main desktop-ffm artifact classes-only.
    if(!isPublishingTask) {
        from(nativeFiles)
    }
}

val nativeRuntime by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(nativeRuntime.name, nativeDesktopJar)
    nativeJars.forEach { add(nativeRuntime.name, it) }
}

tasks.named("clean") {
    doFirst {
        val srcPath = "$projectDir/src/main/"
        project.delete(files(srcPath))
    }
}

tasks.named("compileJava") {
    dependsOn(":jolt:builder:jolt_build_project")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
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
            artifact(nativeDesktopJar)
            // publishing will attach the native jars created earlier
            nativeJars.forEach { artifact(it) }
        }
    }
}
