plugins {
    id("java-library")
}

dependencies {
    api(project(":samples:gdx:shared"))

    implementation(libs.gdxCore)
    api(libs.gdxWebgpuCore)
    api(libs.imguiGdxWgpu)

    api(project(":extensions:gdx:wgpu"))
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java11Target.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.java11Target.get())
}
