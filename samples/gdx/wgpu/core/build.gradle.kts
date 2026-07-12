plugins {
    id("java-library")
}

dependencies {
    api(project(":samples:gdx:shared"))

    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    api("io.github.monstroussoftware.gdx-webgpu:gdx-webgpu:${LibExt.gdxWebGPUVersion}")
    api("com.github.xpenatan.xImGui:gdx-wgpu-impl:${LibExt.gdxImGuiVersion}")

    api(project(":extensions:gdx:wgpu"))
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.java11Target)
    targetCompatibility = JavaVersion.toVersion(LibExt.java11Target)
}
