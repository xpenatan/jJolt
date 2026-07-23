plugins {
    id("java-library")
}

dependencies {
    api(project(":samples:gdx:shared"))

    implementation(libs.gdxCore)
    api(libs.imguiGdxGl)
    api(project(":extensions:gdx:gl"))
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
}
