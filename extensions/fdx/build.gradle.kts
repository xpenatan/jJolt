plugins {
    id("java-library")
}

val moduleName = "jolt-fdx"

dependencies {
    compileOnly(project(":jolt:core"))
    api("${LibExt.fdxGroup}:graphics:${LibExt.fdxVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
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
