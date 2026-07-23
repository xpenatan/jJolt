plugins {
    id("java")
}

dependencies {
    implementation(project(":samples:gdx:gl:core"))
    implementation(project(":samples:shared"))

    implementation(variantOf(libs.gdxCore) { classifier("sources") })

    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        implementation(libs.jjoltWebWasm)
        implementation(libs.jjoltGdxGl)
    }
    else {
        implementation(project(":jolt:web:wasm"))
        implementation(project(":extensions:gdx:gl"))
    }

    implementation(libs.gdxCore)
    implementation(libs.gdxTeavmBackendWeb)
    implementation(variantOf(libs.gdxTeavmBackendWeb) { classifier("sources") })
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaWebTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaWebTarget.get())
}

val mainClassName = "jolt.example.samples.app.Build"

tasks.register<JavaExec>("jolt_samples_run_teavm") {
    group = "jolt_examples_teavm"
    description = "Build SamplesApp example"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
}
