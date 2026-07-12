plugins {
    id("java-library")
}

dependencies {
    implementation("com.github.mgsx-dev.gdx-gltf:core:2.2.1")

    if(LibExt.useRepoLibs) {
        compileOnly("com.github.xpenatan.jJolt:core:${LibExt.exampleVersion}")
        implementation("com.github.xpenatan.jJolt:gdx-gl:${LibExt.exampleVersion}")
    }
    else {
        compileOnly(project(":jolt:core"))
        implementation(project(":extensions:gdx:gl"))
    }

    implementation(project(":samples:gdx:shared"))
    api("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    implementation("com.github.xpenatan.xImGui:imgui-core:${LibExt.gdxImGuiVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.java8Target)
    targetCompatibility = JavaVersion.toVersion(LibExt.java8Target)
}
