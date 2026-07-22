plugins {
    id("java-library")
}

val moduleName = "gdx-gl"

base {
    archivesName.set(moduleName)
}

dependencies {
    compileOnly(project(":jolt:core"))
    api("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.java8Target)
    targetCompatibility = JavaVersion.toVersion(LibExt.java8Target)
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
