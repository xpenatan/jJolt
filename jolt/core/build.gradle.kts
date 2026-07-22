plugins {
    id("java-library")
}

val moduleName = "core"

dependencies {
    api("com.github.xpenatan.jParser:loader-core:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:api-core:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:runtime-core:${LibExt.jParserVersion}")
}

tasks.named("clean") {
    doFirst {
        val srcPath = "$projectDir/src/main/java"
        project.delete(files(srcPath))
    }
}

tasks.named("compileJava") {
    dependsOn(":jolt:builder:jParser_generate")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaMainTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaMainTarget)
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
