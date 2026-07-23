plugins {
    id("java-library")
}

val moduleName = "jolt-fdx"

dependencies {
    compileOnly(project(":jolt:core"))
    api(libs.fdxGraphics)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaFfmTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaFfmTarget.get())
}

java {
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
