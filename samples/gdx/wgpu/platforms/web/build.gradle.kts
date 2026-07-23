plugins {
    id("java")
}

dependencies {
    implementation(project(":samples:gdx:wgpu:core"))
    implementation(project(":samples:shared"))

    implementation(variantOf(libs.gdxCore) { classifier("sources") })

    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        implementation(libs.jjoltWebWasm)
        implementation(libs.jjoltGdxWgpu)
    }
    else {
        implementation(project(":jolt:web:wasm"))
        implementation(project(":extensions:gdx:wgpu"))
    }

    implementation(libs.gdxCore)
    implementation(libs.gdxTeavmBackendWeb)
    implementation(variantOf(libs.gdxTeavmBackendWeb) { classifier("sources") })
    implementation(libs.gdxWebgpuBackendTeavm)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaWebTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaWebTarget.get())
}

val mainClassName = "jolt.example.samples.app.Build"

tasks.register<JavaExec>("jolt_samples_run_teavm_wgpu") {
    group = "jolt_examples_teavm"
    description = "Build SamplesApp WGPU example"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
}
