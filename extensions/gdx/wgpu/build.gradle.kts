plugins {
    id("java-library")
}

val moduleName = "gdx-wgpu"

base {
    archivesName.set(moduleName)
}

dependencies {
    compileOnly(project(":jolt:core"))
    api(project(":extensions:gdx:gl"))
    api("io.github.monstroussoftware.gdx-webgpu:gdx-webgpu:${LibExt.gdxWebGPUVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.java11Target)
    targetCompatibility = JavaVersion.toVersion(LibExt.java11Target)
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
