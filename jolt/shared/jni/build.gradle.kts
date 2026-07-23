plugins {
    id("java-library")
}

val moduleName = "shared-jni"

base {
    archivesName.set(moduleName)
}

dependencies {
    api(libs.jparserRuntimeJni)
    api(libs.jparserApiCore)
    api(libs.jparserLoaderCore)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/main/java"))
    }
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
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
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
