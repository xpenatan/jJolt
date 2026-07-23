plugins {
    id("java-library")
}

dependencies {
    implementation(libs.gdxCore)
    compileOnly(project(":jolt:core"))
    api(libs.imguiGdxShared)
    implementation(project(":extensions:gdx:gl"))
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
}
