plugins {
    id("java-library")
}

val moduleName = "web-wasm"

base {
    archivesName.set(moduleName)
}

val emscriptenJS = "$projectDir/../../builder/build/c++/libs/emscripten/jolt.js"
val emscriptenWASM = "$projectDir/../../builder/build/c++/libs/emscripten/jolt.wasm"

tasks.jar {
    from(emscriptenJS, emscriptenWASM)
}

dependencies {
    api(project(":jolt:core"))
    api(libs.jparserLoaderCore)
    api(libs.jparserLoaderWeb)
    api(libs.jparserApiCore)
    api(libs.jparserApiWeb)
    api(libs.jparserRuntimeCore)
    api(libs.jparserRuntimeWeb)
    api(libs.jparserRuntimeWebWasm)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/main/java", "src/main/support/java"))
    }
}

tasks.named("clean") {
    doFirst {
        val srcPath = "$projectDir/src/main/java"
        val jsPath = "$projectDir/src/main/resources/jolt.wasm.js"
        project.delete(files(srcPath, jsPath))
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
