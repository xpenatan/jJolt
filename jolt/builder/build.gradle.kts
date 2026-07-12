plugins {
    id("java")
}

val mainClassName = "Build"

dependencies {
    implementation(project(":jolt:base"))
    implementation("com.github.xpenatan.jParser:gen-core:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:gen-build:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:gen-build-tool:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:gen-c:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:gen-web:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:gen-jni:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:gen-ffm:${LibExt.jParserVersion}")
    implementation("com.github.xpenatan.jParser:gen-idl:${LibExt.jParserVersion}")
}

tasks.register<JavaExec>("jolt_build_project") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_ffm", "gen_jni", "gen_web", "gen_teavm_c")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_web_wasm") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_web", "web_wasm")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_windows64_jni") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_jni", "windows64_jni")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_windows64_teavm_c") {
    group = "jolt"
    description = "Generate TeaVM C Java bindings and compile native library for Windows"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_teavm_c", "windows64_teavm_c")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_linux64_teavm_c") {
    group = "jolt"
    description = "Generate TeaVM C Java bindings and compile native library for Linux"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_teavm_c", "linux64_teavm_c")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_mac64_teavm_c") {
    group = "jolt"
    description = "Generate TeaVM C Java bindings and compile native library for macOS"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_teavm_c", "mac64_teavm_c")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_macArm_teavm_c") {
    group = "jolt"
    description = "Generate TeaVM C Java bindings and compile native library for macOS ARM"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_teavm_c", "macArm_teavm_c")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_linux64_jni") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_jni", "linux64_jni")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_mac64_jni") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_jni", "mac64_jni")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_macArm_jni") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_jni", "macArm_jni")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_android_jni") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_jni", "android_jni")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_ios_jni") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_jni", "ios_jni")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_windows64_ffm") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_ffm", "windows64_ffm")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_linux64_ffm") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_ffm", "linux64_ffm")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_mac64_ffm") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_ffm", "mac64_ffm")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("jolt_build_project_macArm_ffm") {
    group = "jolt"
    description = "Generate native project"
    mainClass.set(mainClassName)
    args = mutableListOf("gen_ffm", "macArm_ffm")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.withType<JavaExec>().configureEach {
    if(name.startsWith("jolt_build_project")) {
        dependsOn(":jolt:download:jolt_download_source")
    }
}
