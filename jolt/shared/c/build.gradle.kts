plugins {
    id("java-library")
}

val moduleName = "shared-c"
val generatedTeaVMCResourcesDir = layout.buildDirectory.dir("generated/jparser/resources/main")

base {
    archivesName.set(moduleName)
}

dependencies {
    api(libs.jparserRuntimeCore)
    api(libs.jparserRuntimeC)
    api(libs.jparserApiCore)
    api(libs.jparserLoaderCore)
    api(libs.jparserLoaderC)
    api(libs.teavmCore)
    api(libs.teavmClasslib)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/main/java"))
        java.include("gen/c/**/*.java")
        resources.setSrcDirs(listOf("src/main/resources", generatedTeaVMCResourcesDir))
    }
}

tasks.named("clean") {
    doFirst {
        project.delete(files("$projectDir/src/main/java"))
    }
}

tasks.named("compileJava") {
    dependsOn(":jolt:builder:jParser_generate")
}

tasks.named("processResources") {
    dependsOn(":jolt:builder:jParser_generate")
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaWebTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaWebTarget.get())
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
