plugins {
    id("java-library")
}

dependencies {
    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    compileOnly(project(":jolt:core"))
    api("com.github.xpenatan.xImGui:gdx-shared-impl:${LibExt.gdxImGuiVersion}")
    implementation(project(":extensions:gdx:gl"))
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.java8Target)
    targetCompatibility = JavaVersion.toVersion(LibExt.java8Target)
}
