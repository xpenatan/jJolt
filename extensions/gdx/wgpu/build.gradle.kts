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
    api(libs.gdxWebgpuCore)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java11Target.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.java11Target.get())
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = moduleName
            from(components["java"])
        }
    }
}
