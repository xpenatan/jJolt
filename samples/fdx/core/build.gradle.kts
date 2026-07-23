plugins {
    id("java-library")
}

dependencies {
    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        compileOnly(libs.jjoltCore)
        compileOnly(libs.jjoltJoltFdx)
    }
    else {
        compileOnly(project(":jolt:core"))
        compileOnly(project(":extensions:fdx"))
    }

    api(libs.fdxApplication)
    api(libs.fdxDisplay)
    api(libs.fdxGraphics)
    api(libs.fdxG3d)
    api(libs.fdxAssetManager)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaFfmTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaFfmTarget.get())
}
