plugins {
    id("java-library")
}

dependencies {
    implementation(libs.gdxGltfCore)

    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        compileOnly(libs.jjoltCore)
        implementation(libs.jjoltGdxGl)
    }
    else {
        compileOnly(project(":jolt:core"))
        implementation(project(":extensions:gdx:gl"))
    }

    implementation(project(":samples:gdx:shared"))
    api(libs.gdxCore)
    implementation(libs.imguiCore)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
}
