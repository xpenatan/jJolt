plugins {
    id("java-library")
}

val moduleName = "gdx-gl"

base {
    archivesName.set(moduleName)
}

dependencies {
    compileOnly(project(":jolt:core"))
    api(libs.gdxCore)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
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
