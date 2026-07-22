plugins {
    id("java")
    id("io.github.libfdx")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
}

dependencies {
    implementation(project(":samples:fdx:core"))

    if(LibExt.useRepoLibs) {
        implementation("com.github.xpenatan.jJolt:web-wasm:${LibExt.exampleVersion}")
        implementation("com.github.xpenatan.jJolt:jolt-fdx:${LibExt.exampleVersion}")
    }
    else {
        implementation(project(":jolt:web:wasm"))
        implementation(project(":extensions:fdx"))
    }

    implementation("${LibExt.fdxGroup}:backend_web:${LibExt.fdxVersion}")
    implementation("${LibExt.fdxGroup}:gl_web:${LibExt.fdxVersion}")
    implementation("${LibExt.fdxGroup}:wgpu_web:${LibExt.fdxVersion}")
}

libfdx {
    assets(rootProject.file("samples/assets"))
    js {
        mainClass.set("jolt.example.samples.app.web.JoltWebJsLauncher")
        htmlTitle.set("jJolt libfdx - WebGL JS")
        canvasId.set("libfdx-canvas")
        htmlWidth.set(0)
        htmlHeight.set(0)
    }
    wasm {
        mainClass.set("jolt.example.samples.app.web.JoltWebWasmLauncher")
        htmlTitle.set("jJolt libfdx - WebGL Wasm")
        canvasId.set("libfdx-canvas")
        htmlWidth.set(0)
        htmlHeight.set(0)
    }
}

val jsWebappDir = layout.buildDirectory.dir("dist/web-js/webapp")
val wasmWebappDir = layout.buildDirectory.dir("dist/web-wasm/webapp")

fun registerJoltRuntimeScriptCopy(
    taskName: String,
    prepareTaskName: String,
    webappDir: Provider<Directory>
): TaskProvider<Task> {
    val runtimeClasspath = configurations.named("runtimeClasspath")
    return tasks.register(taskName) {
        dependsOn(prepareTaskName)
        inputs.files(runtimeClasspath)
        outputs.file(webappDir.map { it.file("scripts/jolt.js") })
        outputs.file(webappDir.map { it.file("scripts/jolt.wasm") })
        doLast {
            val scriptsDir = webappDir.get().dir("scripts").asFile
            val scriptNames = setOf("jolt.js", "jolt.wasm")
            project.delete(scriptNames.map { File(scriptsDir, it) })
            project.copy {
                runtimeClasspath.get().files.forEach { entry ->
                    when {
                        entry.isDirectory -> from(entry) {
                            include("jolt.js", "jolt.wasm")
                        }
                        entry.isFile && entry.extension == "jar" -> from(zipTree(entry)) {
                            include("**/jolt.js", "**/jolt.wasm")
                            eachFile {
                                relativePath = RelativePath(true, name)
                            }
                            includeEmptyDirs = false
                        }
                    }
                }
                into(scriptsDir)
            }
            val missing = scriptNames.filterNot { File(scriptsDir, it).isFile }
            if(missing.isNotEmpty()) {
                throw GradleException("Missing jJolt web runtime scripts: ${missing.joinToString()}")
            }
        }
    }
}

val copyJoltJsRuntimeScripts = registerJoltRuntimeScriptCopy(
    "copyJoltJsRuntimeScripts",
    "libfdx_web_js_prepare",
    jsWebappDir
)
val copyJoltWasmRuntimeScripts = registerJoltRuntimeScriptCopy(
    "copyJoltWasmRuntimeScripts",
    "libfdx_web_wasm_prepare",
    wasmWebappDir
)

tasks.register("jolt_sample_webgl_js_build") {
    group = "application"
    description = "Builds the jJolt libfdx WebGL JavaScript sample."
    dependsOn("libfdx_web_js_build", copyJoltJsRuntimeScripts)
}

tasks.register("jolt_sample_webgl_wasm_build") {
    group = "application"
    description = "Builds the jJolt libfdx WebGL Wasm sample."
    dependsOn("libfdx_web_wasm_build", copyJoltWasmRuntimeScripts)
}

tasks.register("jolt_sample_webgpu_js_build") {
    group = "application"
    description = "Builds the jJolt libfdx WebGPU JavaScript sample."
    dependsOn("libfdx_web_js_build", copyJoltJsRuntimeScripts)
    configureWebGpuPage("dist/web-js/webapp", "jJolt libfdx - JS WebGPU")
}

tasks.register<io.github.libfdx.gradle.LibfdxRunWebTask>("jolt_sample_webgl_js_run") {
    group = "application"
    description = "Builds and serves the jJolt libfdx WebGL JavaScript sample."
    dependsOn("jolt_sample_webgl_js_build")
    webappDir.set(jsWebappDir)
    port.set(libfdx.js.serverPort)
    defaultPath.set("/")
}

tasks.register<io.github.libfdx.gradle.LibfdxRunWebTask>("jolt_sample_webgl_wasm_run") {
    group = "application"
    description = "Builds and serves the jJolt libfdx WebGL Wasm sample."
    dependsOn("jolt_sample_webgl_wasm_build")
    webappDir.set(wasmWebappDir)
    port.set(libfdx.wasm.serverPort)
    defaultPath.set("/")
}

tasks.register<io.github.libfdx.gradle.LibfdxRunWebTask>("jolt_sample_webgpu_js_run") {
    group = "application"
    description = "Builds and serves the jJolt libfdx WebGPU JavaScript sample."
    dependsOn("jolt_sample_webgpu_js_build")
    webappDir.set(jsWebappDir)
    port.set(libfdx.js.serverPort)
    defaultPath.set("/webgpu.html")
}

fun Task.configureWebGpuPage(webappPath: String, title: String) {
    val webappDir = layout.buildDirectory.dir(webappPath)
    val indexFile = webappDir.map { it.file("index.html") }
    val loaderFile = webappDir.map { it.file("scripts/fdx-loader.js") }
    val outputFile = webappDir.map { it.file("webgpu.html") }
    val outputLoaderFile = webappDir.map { it.file("scripts/fdx-loader-webgpu.js") }
    inputs.files(indexFile, loaderFile)
    outputs.files(outputFile, outputLoaderFile)
    doLast {
        writeWebGpuPage(
            indexFile.get().asFile,
            loaderFile.get().asFile,
            outputFile.get().asFile,
            outputLoaderFile.get().asFile,
            title
        )
    }
}

fun writeWebGpuPage(indexFile: File, loaderFile: File, outputFile: File, outputLoaderFile: File, title: String) {
    val source = indexFile.readText()
    val withTitle = source.replace(Regex("<title>.*</title>"), "<title>$title</title>")
    val rewrittenPage = withTitle.replace("scripts/fdx-loader.js", "scripts/fdx-loader-webgpu.js")
    if(rewrittenPage == withTitle) {
        throw GradleException("Could not create WebGPU launch page from ${indexFile.absolutePath}")
    }
    outputFile.writeText(rewrittenPage)

    val loaderSource = loaderFile.readText()
    val rewrittenLoader = loaderSource.replace("mainClassArgs: []", "mainClassArgs: [\"--graphics=webgpu\"]")
    if(rewrittenLoader == loaderSource) {
        throw GradleException("Could not configure WebGPU arguments in ${loaderFile.absolutePath}")
    }
    outputLoaderFile.writeText(rewrittenLoader)
}
