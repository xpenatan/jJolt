plugins {
    id("java")
}

dependencies {
    implementation(project(":samples:gdx:gl:core"))
    implementation(project(":samples:shared"))

    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}:sources")

    if(LibExt.useRepoLibs) {
        implementation("com.github.xpenatan.jJolt:web-wasm:${LibExt.exampleVersion}")
        implementation("com.github.xpenatan.jJolt:gdx-gl:${LibExt.exampleVersion}")
    }
    else {
        implementation(project(":jolt:web:wasm"))
        implementation(project(":extensions:gdx:gl"))
    }

    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    implementation("com.github.xpenatan.gdx-teavm:backend-web:${LibExt.gdxTeaVMVersion}")
    implementation("com.github.xpenatan.gdx-teavm:backend-web:${LibExt.gdxTeaVMVersion}:sources")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaWebTarget)
}

val mainClassName = "jolt.example.samples.app.Build"

tasks.register<JavaExec>("jolt_samples_run_teavm") {
    group = "jolt_examples_teavm"
    description = "Build SamplesApp example"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
}
