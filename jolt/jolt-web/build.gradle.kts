plugins {
    id("java-library")
}

val moduleName = "jolt-web"

val emscriptenJS = "$projectDir/../jolt-build/build/c++/libs/emscripten/jolt.js"
val emscriptenWASM = "$projectDir/../jolt-build/build/c++/libs/emscripten/jolt.wasm"

tasks.jar {
    from(emscriptenJS, emscriptenWASM)
}

dependencies {
    api(project(":jolt:jolt-core"))
    api("com.github.xpenatan.jParser:loader-core:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:loader-web:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:api-core:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:api-web:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:runtime-core:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:runtime-web:${LibExt.jParserVersion}")
    api("com.github.xpenatan.jParser:runtime-web_wasm:${LibExt.jParserVersion}")
}

sourceSets {
    main {
        java.srcDir("src/main/support/java")
    }
}

tasks.named("clean") {
    doFirst {
        val srcPath = "$projectDir/src/main/java"
        val jsPath = "$projectDir/src/main/resources/jolt.wasm.js"
        project.delete(files(srcPath, jsPath))
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = moduleName
            group = LibExt.groupId
            version = LibExt.libVersion
            from(components["java"])
        }
    }
}
