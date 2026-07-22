import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter
import com.github.xpenatan.jParser.builder.targets.AndroidTarget
import com.github.xpenatan.jParser.gradle.JParserBuildTask
import com.github.xpenatan.jParser.gradle.JParserTargets
import com.github.xpenatan.jParser.idl.IDLClassOrEnum
import com.github.xpenatan.jParser.idl.IDLHelper
import com.github.xpenatan.jParser.idl.IDLRenaming
import com.github.xpenatan.jParser.idl.IDLTypeConverterListener
import java.io.File

plugins {
    id("java-library")
    id("com.github.xpenatan.jparser")
}

fun File.normalizedPath(): String {
    return absolutePath.replace('\\', '/')
}

fun File.writeTextWithRetry(text: String) {
    val normalizedText = text.replace(Regex("[\\t ]+(?=\\r?$)", RegexOption.MULTILINE), "")
    var lastFailure: Exception? = null
    repeat(50) {
        try {
            writeText(normalizedText)
            return
        }
        catch(exception: java.io.IOException) {
            lastFailure = exception
            Thread.sleep(20)
        }
    }
    throw GradleException("Unable to update generated source $this", lastFailure)
}

val downloadBuildDir = file("../download/build")
val joltSourceRoot = File(downloadBuildDir, "jolt-source")
val joltJsSourceRoot = File(downloadBuildDir, "jolt-js-source")
val joltCustomSourceDir = file("src/main/cpp/custom")
val joltCustomHeader = File(joltCustomSourceDir, "JoltCustom.h")
val joltWebIdlExclusions = file("src/main/cpp/jolt_webidl_exclusions.txt")
val joltCmake = File(joltSourceRoot, "Jolt/Jolt.cmake")
val forbiddenJoltBackendPrefixes = listOf(
    "Compute/CPU/",
    "Compute/DX12/",
    "Compute/MTL/",
    "Compute/VK/"
)

fun portableJoltSourcesFromCmake(cmakeFile: File): List<String> {
    if(!cmakeFile.isFile) {
        throw GradleException("Jolt.cmake is unavailable; run :jolt:download:jolt_download_source first: $cmakeFile")
    }
    val cmake = cmakeFile.readText()
    val sourcePath = Regex("""\$\{JOLT_PHYSICS_ROOT}/([^\s)]+\.cpp)""")
    fun sourcesIn(block: String): List<String> =
        sourcePath.findAll(block).map { it.groupValues[1] }.toList()

    val baseBlock = Regex(
        """set\(JOLT_PHYSICS_SRC_FILES\s*(.*?)\r?\n\)""",
        RegexOption.DOT_MATCHES_ALL
    ).find(cmake)?.groupValues?.get(1)
        ?: throw GradleException("Could not find JOLT_PHYSICS_SRC_FILES in $cmakeFile")
    val objectStreamBlock = Regex(
        """if \(ENABLE_OBJECT_STREAM\)(.*?)endif\(\)""",
        RegexOption.DOT_MATCHES_ALL
    ).find(cmake)?.groupValues?.get(1)
        ?: throw GradleException("Could not find ENABLE_OBJECT_STREAM sources in $cmakeFile")

    return sourcesIn(baseBlock) + sourcesIn(objectStreamBlock)
}

val joltSourceFiles = providers.provider {
    portableJoltSourcesFromCmake(joltCmake)
        .map { "${joltSourceRoot.normalizedPath()}/Jolt/$it" }
}
val generatedJavaSourceDirs = listOf(
    file("../core/src/main/java"),
    file("../shared/jni/src/main/java"),
    file("../shared/c/src/main/java"),
    file("../desktop/ffm/src/main/java"),
    file("../web/wasm/src/main/java")
)

val desktopTargets = listOf(
    JParserTargets.WINDOWS64_JNI,
    JParserTargets.LINUX64_JNI,
    JParserTargets.MAC64_JNI,
    JParserTargets.MAC_ARM_JNI,
    JParserTargets.WINDOWS64_FFM,
    JParserTargets.LINUX64_FFM,
    JParserTargets.MAC64_FFM,
    JParserTargets.MAC_ARM_FFM,
    JParserTargets.WINDOWS64_TEAVM_C,
    JParserTargets.LINUX64_TEAVM_C,
    JParserTargets.MAC64_TEAVM_C,
    JParserTargets.MAC_ARM_TEAVM_C
)

val windowsTargets = setOf(
    JParserTargets.WINDOWS64_JNI,
    JParserTargets.WINDOWS64_FFM,
    JParserTargets.WINDOWS64_TEAVM_C
)

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaMainTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaMainTarget)
}

jParser {
    libName.set("jolt")
    idlName.set("jolt")
    webModuleName.set("jolt")
    modulePrefix.set("")
    modulePath(file(".."))
    moduleBuildSuffix.set("builder")
    moduleBaseSuffix.set("base")
    moduleCoreSuffix.set("core")
    moduleJNISuffix.set("shared/jni")
    moduleFFMSuffix.set("desktop/ffm")
    moduleWebSuffix.set("web/wasm")
    moduleCSuffix.set("shared/c")
    packageName.set("jolt")
    cppSourcePath(joltSourceRoot)
    additionalIDLPath(file("src/main/cpp/jolt_float.idl"))
    jniCppStandard.set("c++17")
    ffmCppStandard.set("c++17")
    webCppStandard.set("c++17")
    teaVMCCppStandard.set("c++17")
    webSideModule.set(1)
    webForcedInclude(joltCustomHeader)
    androidApiLevel.set(AndroidTarget.ApiLevel.Android_10_29)

    native {
        dependsOn(":jolt:download:jolt_download_source")
        headerDir(joltSourceRoot)
        headerDir(joltCustomSourceDir)
        cppIncludes.addAll(joltSourceFiles)
        includeDefaultSources.set(false)
        includeCustomSources.set(false)
        compileFlag("-DJPH_DEBUG_RENDERER")
        compileFlag("-DJPH_DISABLE_CUSTOM_ALLOCATOR")
        compileFlag("-DJPH_ENABLE_ASSERTS")
        compileFlag("-DJPH_CROSS_PLATFORM_DETERMINISTIC")
        compileFlag("-DJPH_OBJECT_LAYER_BITS=32")

        desktopTargets.forEach { targetName ->
            target(targetName) {
                includeDefaultSources.set(false)
                includeCustomSources.set(false)
                if(targetName in windowsTargets) {
                    compileFlag("/MP2")
                    compileFlag("/EHsc")
                }
                if(targetName == JParserTargets.WINDOWS64_TEAVM_C) {
                    compileFlag("/MT")
                }
            }
        }

        target(JParserTargets.WEB_WASM) {
            includeDefaultSources.set(false)
            includeCustomSources.set(false)
            compileFlag("-msimd128")
            compileFlag("-msse4.2")
            linkerFlag("-lc++abi")
            linkerFlag("-lc++")
            linkerFlag("-lc")
        }

        target(JParserTargets.ANDROID_JNI) {
            includeDefaultSources.set(false)
            includeCustomSources.set(false)
            linkerFlag("-Wl,-z,max-page-size=16384")
        }
    }

    idlRenaming(object : IDLRenaming {
        override fun obtainNewPackage(idlClassOrEnum: IDLClassOrEnum, classPackage: String): String {
            return classPackage.replace("jolt", "")
        }
    })
}

tasks.withType(JParserBuildTask::class.java).configureEach {
    doFirst {
        IDLHelper.cppConverter = IDLTypeConverterListener { idlType ->
            if(idlType == "unsigned long long") "uint64" else null
        }
        IDLHelper.javaConverter = IDLTypeConverterListener { idlType ->
            when(idlType) {
                "IDLArray" -> "NativeArray"
                "IDLFloatArray" -> "NativeFloatArray"
                "IDLString" -> "NativeString"
                "BodyID[]", "NativeArrayBodyID" -> "IDLArrayBodyID"
                else -> null
            }
        }
    }

    doLast {
        generatedJavaSourceDirs.forEach { sourceDir ->
            fileTree(sourceDir) {
                include("**/*.java")
            }.forEach javaFileLoop@ { javaFile ->
                val source = javaFile.readText()
                if(!Regex("\\bNativeObject\\b").containsMatchIn(source)) {
                    return@javaFileLoop
                }

                val unit = StaticJavaParser.parse(source)
                LexicalPreservingPrinter.setup(unit)
                var changed = false

                if(unit.imports.none { it.nameAsString.endsWith(".NativeObject") }) {
                    val packageName = unit.packageDeclaration
                        .map { it.nameAsString }
                        .orElse("")
                    val importName = if(packageName.startsWith("gen.web.")) {
                        "gen.web.com.github.xpenatan.jParser.api.NativeObject"
                    }
                    else {
                        "com.github.xpenatan.jParser.api.NativeObject"
                    }
                    unit.addImport(importName)
                    changed = true
                }

                val declaration = unit.types
                    .filter { it.nameAsString == javaFile.nameWithoutExtension }
                    .mapNotNull { it.toClassOrInterfaceDeclaration().orElse(null) }
                    .firstOrNull()

                if(javaFile.normalizedPath().contains("/desktop/ffm/")) {
                    // Panama critical downcalls cannot safely re-enter Java. Many
                    // apparently simple Jolt operations can reach a user filter,
                    // listener, or debug renderer through a native virtual call,
                    // so keep the entire portable FFM surface callback-safe.
                    unit.findAll(MethodCallExpr::class.java)
                        .filter { it.nameAsString == "downcallCritical" }
                        .forEach {
                            it.setName("downcallDefault")
                            changed = true
                        }
                }

                if(declaration != null && declaration.nameAsString == "JoltSettings") {
                    val fields = listOf(
                        "private BroadPhaseLayerInterface internalBroadPhaseLayerInterface;",
                        "private ObjectVsBroadPhaseLayerFilter internalObjectVsBroadPhaseLayerFilter;",
                        "private ObjectLayerPairFilter internalObjectLayerPairFilter;",
                        "private AssertFailedHandler internalAssertFailedHandler;",
                        "private boolean internalTransferred;"
                    )
                    fields.forEach { fieldSource ->
                        val fieldName = fieldSource.substringBefore(';').substringAfterLast(' ')
                        if(declaration.fields.none { it.variables.any { variable -> variable.nameAsString == fieldName } }) {
                            declaration.addMember(StaticJavaParser.parseBodyDeclaration(fieldSource))
                            changed = true
                        }
                    }

                    val retainedSetters = mapOf(
                        "set_mBroadPhaseLayerInterface" to "internalBroadPhaseLayerInterface",
                        "set_mObjectVsBroadPhaseLayerFilter" to "internalObjectVsBroadPhaseLayerFilter",
                        "set_mObjectLayerPairFilter" to "internalObjectLayerPairFilter",
                        "set_mAssertFailedHandler" to "internalAssertFailedHandler"
                    )
                    retainedSetters.forEach { (methodName, fieldName) ->
                        declaration.getMethodsByName(methodName).forEach { method ->
                            val body = method.body.orElse(null) ?: return@forEach
                            val parameterName = method.parameters.first().nameAsString
                            if(body.statements.none { it.toString().contains("internal_requireMutable()") }) {
                                body.addStatement(0, StaticJavaParser.parseStatement("internal_requireMutable();"))
                                body.addStatement(1, StaticJavaParser.parseStatement("this.$fieldName = $parameterName;"))
                                changed = true
                            }
                        }
                    }

                    if(declaration.getMethodsByName("internal_requireMutable").isEmpty()) {
                        declaration.addMember(StaticJavaParser.parseBodyDeclaration("""
                            private void internal_requireMutable() {
                                if (internalTransferred) {
                                    throw new IllegalStateException("JoltSettings has already transferred its collision filters to a JoltInterface");
                                }
                            }
                        """.trimIndent()))
                        declaration.addMember(StaticJavaParser.parseBodyDeclaration("""
                            void internal_validateForConstruction() {
                                internal_requireMutable();
                                if (isDisposed()) {
                                    throw new IllegalStateException("JoltSettings has already been disposed");
                                }
                                if (internalBroadPhaseLayerInterface == null || internalObjectVsBroadPhaseLayerFilter == null || internalObjectLayerPairFilter == null) {
                                    throw new IllegalStateException("JoltSettings requires all three collision-filter interfaces");
                                }
                            }
                        """.trimIndent()))
                        declaration.addMember(StaticJavaParser.parseBodyDeclaration("""
                            Object[] internal_transferOwnership() {
                                internal_validateForConstruction();
                                internalBroadPhaseLayerInterface.native_releaseOwnership();
                                internalObjectVsBroadPhaseLayerFilter.native_releaseOwnership();
                                internalObjectLayerPairFilter.native_releaseOwnership();
                                Object[] retained = new Object[] { internalBroadPhaseLayerInterface, internalObjectVsBroadPhaseLayerFilter, internalObjectLayerPairFilter, internalAssertFailedHandler };
                                internalTransferred = true;
                                internalBroadPhaseLayerInterface = null;
                                internalObjectVsBroadPhaseLayerFilter = null;
                                internalObjectLayerPairFilter = null;
                                internalAssertFailedHandler = null;
                                return retained;
                            }
                        """.trimIndent()))
                        changed = true
                    }
                }

                if(declaration != null && declaration.nameAsString == "JoltInterface") {
                    if(declaration.fields.none { it.variables.any { variable -> variable.nameAsString == "internalRetainedObjects" } }) {
                        declaration.addMember(StaticJavaParser.parseBodyDeclaration("private Object[] internalRetainedObjects;"))
                        changed = true
                    }
                    declaration.constructors
                        .filter { constructor -> constructor.parameters.size == 1 && constructor.parameters[0].typeAsString == "JoltSettings" }
                        .forEach { constructor ->
                            val body = constructor.body
                            if(body.statements.none { it.toString().contains("internal_validateForConstruction") }) {
                                val parameterName = constructor.parameters[0].nameAsString
                                body.addStatement(0, StaticJavaParser.parseStatement("$parameterName.internal_validateForConstruction();"))
                                body.addStatement(StaticJavaParser.parseStatement("internalRetainedObjects = $parameterName.internal_transferOwnership();"))
                                changed = true
                            }
                        }

                    if(declaration.getMethodsByName("internal_releaseTransferredFilters").isEmpty()) {
                        declaration.addMember(StaticJavaParser.parseBodyDeclaration("""
                            private void internal_releaseTransferredFilters() {
                                if (internalRetainedObjects == null) {
                                    return;
                                }
                                for (int i = 0; i < 3; ++i) {
                                    if (internalRetainedObjects[i] instanceof NativeObject) {
                                        ((NativeObject) internalRetainedObjects[i]).native_reset();
                                    }
                                }
                                internalRetainedObjects = null;
                            }
                        """.trimIndent()))
                        changed = true
                    }
                    declaration.getMethodsByName("deleteNative").forEach { method ->
                        val body = method.body.orElse(null) ?: return@forEach
                        if(body.statements.none { it.toString().contains("internal_releaseTransferredFilters") }) {
                            body.addStatement(StaticJavaParser.parseStatement("internal_releaseTransferredFilters();"))
                            changed = true
                        }
                    }
                }

                if(declaration != null && declaration.nameAsString == "CharacterVirtual"
                    && declaration.getMethodsByName("GetListener").isEmpty()) {
                    val packageName = unit.packageDeclaration.map { it.nameAsString }.orElse("")
                    unit.addImport(packageName.substringBefore(".physics") + ".Jolt")
                    declaration.addMember(StaticJavaParser.parseBodyDeclaration("""
                        public CharacterContactListener GetListener() {
                            return Jolt.GetCharacterContactListener(this);
                        }
                    """.trimIndent()))
                    changed = true
                }

                val deprecatedLifecycleMethods = when(declaration?.nameAsString) {
                    "Jolt" -> setOf("Init", "RegisterTypes", "UnregisterTypes")
                    "JoltNew" -> setOf("Factory", "PhysicsSystem", "TempAllocatorImpl", "JobSystemThreadPool")
                    else -> emptySet()
                }
                deprecatedLifecycleMethods.forEach { methodName ->
                    declaration?.getMethodsByName(methodName)?.forEach { method ->
                        if(method.annotations.none { it.nameAsString == "Deprecated" }) {
                            method.addAnnotation("Deprecated")
                            changed = true
                        }
                    }
                }

                if(changed) {
                    javaFile.writeTextWithRetry(LexicalPreservingPrinter.print(unit))
                }
            }
        }
    }
}

val normalizeFfmCallbackDowncalls = tasks.register("normalizeFfmCallbackDowncalls") {
    group = "build setup"
    description = "Makes generated Panama downcalls safe when native Jolt code re-enters Java callbacks."

    doLast {
        fileTree(file("../desktop/ffm/src/main/java")) {
            include("**/*.java")
        }.forEach { javaFile ->
            val source = javaFile.readText()
            val normalized = source.replace("FFMDowncallHelper.downcallCritical(", "FFMDowncallHelper.downcallDefault(")
            if(normalized != source) {
                javaFile.writeTextWithRetry(normalized)
            }
        }
    }
}

val normalizeGeneratedNativeObjectImports = tasks.register("normalizeGeneratedNativeObjectImports") {
    group = "build setup"
    description = "Adds the runtime NativeObject import required by opaque WebIDL any values."

    doLast {
        generatedJavaSourceDirs.forEach { sourceDir ->
            fileTree(sourceDir) {
                include("**/*.java")
            }.forEach javaFileLoop@ { javaFile ->
                val source = javaFile.readText()
                if(!Regex("\\bNativeObject\\b").containsMatchIn(source)
                    || source.contains("import com.github.xpenatan.jParser.api.NativeObject;")
                    || source.contains("import gen.web.com.github.xpenatan.jParser.api.NativeObject;")) {
                    return@javaFileLoop
                }

                val unit = StaticJavaParser.parse(source)
                val packageName = unit.packageDeclaration.map { it.nameAsString }.orElse("")
                val importName = if(packageName.startsWith("gen.web.")) {
                    "gen.web.com.github.xpenatan.jParser.api.NativeObject"
                }
                else {
                    "com.github.xpenatan.jParser.api.NativeObject"
                }
                unit.addImport(importName)
                javaFile.writeTextWithRetry(unit.toString())
            }
        }
    }
}

tasks.matching { it.name == "jParser_generate" }.configureEach {
    finalizedBy(normalizeGeneratedNativeObjectImports, normalizeFfmCallbackDowncalls)
}

tasks.matching { it.name.startsWith("jParser_build_") && it.name.endsWith("_ffm") }.configureEach {
    // Native FFM build tasks ask jParser to refresh their Java stubs too.
    // Normalize again after that refresh so callback-capable operations never
    // regress to Panama critical downcalls.
    finalizedBy(normalizeFfmCallbackDowncalls)
}

tasks.matching { it.name.startsWith("jParser_build_") }.configureEach {
    mustRunAfter("jParser_generate")
}

tasks.register("verifyJoltSourceSelection") {
    group = "verification"
    description = "Verifies the portable Jolt source set selected directly from Jolt 5.6's Jolt.cmake."
    dependsOn(":jolt:download:jolt_download_source")

    inputs.file(joltCmake)

    doLast {
        val sources = portableJoltSourcesFromCmake(joltCmake)
        val forbidden = sources.filter { source ->
            forbiddenJoltBackendPrefixes.any(source::startsWith)
        }
        val missingFiles = sources.filterNot { source ->
            File(joltSourceRoot, "Jolt/$source").isFile
        }
        val duplicates = sources.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        if(forbidden.isNotEmpty() || missingFiles.isNotEmpty() || duplicates.isNotEmpty()) {
            val details = buildList {
                if(forbidden.isNotEmpty()) add("Disabled backend sources selected:\n - " + forbidden.joinToString("\n - "))
                if(missingFiles.isNotEmpty()) add("Manifest files not found:\n - " + missingFiles.joinToString("\n - "))
                if(duplicates.isNotEmpty()) add("Duplicate Jolt.cmake sources:\n - " + duplicates.joinToString("\n - "))
            }
            throw GradleException("Jolt source selection verification failed:\n" + details.joinToString("\n"))
        }
        logger.lifecycle("Verified " + sources.size + " portable Jolt sources selected directly from Jolt 5.6 Jolt.cmake.")
    }
}

data class WebIdlMember(
    val kind: String,
    val name: String,
    val returnOrPropertyType: String,
    val parameterTypes: List<String> = emptyList()
) {
    fun display(container: String): String = when(kind) {
        "constructor" -> "$container(${parameterTypes.joinToString(", ")})"
        "property" -> "$container.$name: $returnOrPropertyType"
        else -> "$container.$name(${parameterTypes.joinToString(", ")}): $returnOrPropertyType"
    }
}

data class WebIdlSurface(
    val interfaces: Map<String, Set<WebIdlMember>>,
    val enums: Map<String, Set<String>>
)

// WebIDL extended attributes can contain quoted brackets (for example
// [Operator="[]"]), while array types use a bare [] suffix. Match only the
// former so the parity verifier does not erase array-valued declarations.
val webIdlAnnotationRegex = Regex("""\[(?:"[^"]*"|[^]])+]""")

fun normalizeWebIdlType(rawType: String): String {
    var type = rawType
        .replace(webIdlAnnotationRegex, " ")
        .replace(Regex("""\b(optional|static|readonly)\b"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    type = when(type) {
        "DOMString" -> "NativeString"
        "JPHString" -> "NativeString"
        "VehicleTrack[]" -> "ArrayVehicleTrack"
        else -> type
    }
    return type.replace(" ", "_")
}

fun splitWebIdlParameters(rawParameters: String): List<String> {
    if(rawParameters.isBlank()) return emptyList()
    return rawParameters.split(',').map { rawParameter ->
        val withoutDefault = rawParameter.substringBefore('=').trim()
        val withoutAnnotations = withoutDefault.replace(webIdlAnnotationRegex, " ")
        val tokens = withoutAnnotations
            .replace(Regex("""\boptional\b"""), " ")
            .trim()
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
        normalizeWebIdlType(tokens.dropLast(1).joinToString(" "))
    }
}

fun portableWebIdlTypeMatches(upstreamType: String, bindingType: String): Boolean {
    if(upstreamType == bindingType) return true
    // The default jJolt build uses Jolt's float-precision aliases, where
    // RVec3/RMat44 are represented by Vec3/Mat44. Keep this asymmetric:
    // a genuinely upstream Vec3 declaration must not be widened to RVec3.
    return upstreamType == "RVec3" && bindingType == "Vec3"
        || upstreamType == "RMat44" && bindingType == "Mat44"
}

fun portableWebIdlMemberMatches(upstream: WebIdlMember, binding: WebIdlMember): Boolean {
    return upstream.kind == binding.kind
        && (upstream.kind == "constructor" || upstream.name == binding.name)
        && portableWebIdlTypeMatches(upstream.returnOrPropertyType, binding.returnOrPropertyType)
        && upstream.parameterTypes.size == binding.parameterTypes.size
        && upstream.parameterTypes.zip(binding.parameterTypes).all { (upstreamType, bindingType) ->
            portableWebIdlTypeMatches(upstreamType, bindingType)
        }
}

fun parseWebIdl(files: List<File>): WebIdlSurface {
    val text = files.joinToString("\n") { it.readText() }
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\r\n]*"""), "")

    val interfaces = linkedMapOf<String, MutableSet<WebIdlMember>>()
    Regex("""interface\s+(\w+)(?:\s*:\s*\w+)?\s*\{(.*?)}\s*;""", RegexOption.DOT_MATCHES_ALL)
        .findAll(text)
        .forEach { match ->
            val interfaceName = match.groupValues[1]
            val members = interfaces.getOrPut(interfaceName) { linkedSetOf() }
            match.groupValues[2].split(';').forEach memberLoop@ { rawMember ->
                val compact = rawMember.replace(Regex("""\s+"""), " ").trim()
                if(compact.isBlank()) return@memberLoop

                val bindTo = Regex("BindTo\\s*=\\s*\"([^\"]+)\"").find(compact)?.groupValues?.get(1)
                val withoutAnnotations = compact.replace(webIdlAnnotationRegex, " ")
                    .replace(Regex("""\s+"""), " ")
                    .trim()

                if(withoutAnnotations.contains('(')) {
                    val beforeParameters = withoutAnnotations.substringBefore('(').trim()
                    val rawName = beforeParameters.substringAfterLast(' ')
                    var name = bindTo ?: rawName.replace(Regex("""__\d+$"""), "")
                    name = name.removeSuffix("_custom")
                    val parameters = splitWebIdlParameters(withoutAnnotations.substringAfter('(').substringBeforeLast(')'))
                    val constructor = rawName == interfaceName || rawName.endsWith("Impl") && rawName == interfaceName
                    if(constructor) {
                        members += WebIdlMember("constructor", "<constructor>", "void", parameters)
                    }
                    else {
                        val returnType = normalizeWebIdlType(beforeParameters.removeSuffix(rawName).trim())
                        members += WebIdlMember("method", name, returnType, parameters)
                    }
                }
                else if(Regex("""\battribute\b""").containsMatchIn(withoutAnnotations)) {
                    val name = withoutAnnotations.substringAfterLast(' ')
                    val typeText = withoutAnnotations.substringAfter("attribute ").removeSuffix(name).trim()
                    members += WebIdlMember("property", name, normalizeWebIdlType(typeText))
                }
            }
        }

    val enums = linkedMapOf<String, Set<String>>()
    Regex("""enum\s+(\w+)\s*\{(.*?)}\s*;""", RegexOption.DOT_MATCHES_ALL)
        .findAll(text)
        .forEach { match ->
            enums[match.groupValues[1]] = Regex(""""([^"]+)"""")
                .findAll(match.groupValues[2])
                .map { it.groupValues[1] }
                .toSet()
        }
    return WebIdlSurface(interfaces, enums)
}

tasks.register("verifyJoltWebIdlParity") {
    group = "verification"
    description = "Fails when a portable JoltPhysics.js 1.1.0 WebIDL declaration is not represented by jJolt."
    dependsOn(":jolt:download:jolt_download_source")

    val upstreamFiles = listOf(
        File(joltJsSourceRoot, "JoltJS.idl"),
        File(joltJsSourceRoot, "JoltJS-DebugRenderer.idl")
    )
    val bindingFiles = listOf(
        file("src/main/cpp/jolt.idl"),
        file("src/main/cpp/jolt_float.idl")
    )
    inputs.files(upstreamFiles + bindingFiles + joltWebIdlExclusions)
    inputs.property("joltCommit", LibExt.joltCommit)
    inputs.property("joltJsCommit", LibExt.joltJsCommit)

    doLast {
        val exclusions = joltWebIdlExclusions.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith('#') }
            .map { line ->
                val fields = line.split('|', limit = 3)
                if(fields.size != 3 || fields[2].isBlank()) {
                    throw GradleException("Invalid WebIDL exclusion (expected kind|symbol|platform reason): $line")
                }
                Triple(fields[0], fields[1], fields[2])
            }

        val allowedExclusions = setOf(
            "category:Jolt/Compute/**",
            "category:Jolt/Physics/Hair/**",
            "member:JoltInterface.sGetTotalMemory",
            "member:JoltInterface.sGetFreeMemory"
        )
        val actualExclusions = exclusions.map { "${it.first}:${it.second}" }.toSet()
        if(actualExclusions != allowedExclusions) {
            throw GradleException("The reviewed WebIDL exclusion manifest changed unexpectedly. Allowed entries: ${allowedExclusions.sorted()}; actual: ${actualExclusions.sorted()}")
        }

        val upstream = parseWebIdl(upstreamFiles)
        val binding = parseWebIdl(bindingFiles)
        val interfaceAliases = mapOf(
            "AssertFailedHandlerJS" to "AssertFailedHandlerImpl",
            "BodyFilterJS" to "BodyFilterImpl",
            "CharacterContactListenerEm" to "CharacterContactListener",
            "CharacterContactListenerJS" to "CharacterContactListenerImpl",
            "DebugRendererJS" to "DebugRendererImplCustom",
            "JPHString" to "NativeString",
            "ShapeFilterJS" to "ShapeFilterCallbackImpl",
            "ShapeFilterJS2" to "ShapeFilterCallbackImpl"
        )

        val missing = mutableListOf<String>()
        var matchedDeclarations = 0
        upstream.interfaces.forEach { (upstreamName, upstreamMembers) ->
            if(upstreamName == "JPHString") {
                // NativeString is supplied by jParser on every backend and is the reviewed Java equivalent.
                matchedDeclarations += 1 + upstreamMembers.size
                return@forEach
            }

            val bindingName = interfaceAliases[upstreamName] ?: upstreamName
            val bindingMembers = binding.interfaces[bindingName]
            if(bindingMembers == null) {
                missing += "interface $upstreamName (expected binding $bindingName)"
                return@forEach
            }
            matchedDeclarations++

            upstreamMembers.forEach memberLoop@ { upstreamMember ->
                val exclusionKey = "member:$upstreamName.${upstreamMember.name}"
                if(exclusionKey in actualExclusions) return@memberLoop

                var expected = upstreamMember
                if(expected.kind == "constructor") {
                    expected = expected.copy(name = "<constructor>")
                }
                if(upstreamName == "ShapeFilterJS" && expected.name == "ShouldCollide") {
                    expected = expected.copy(name = "ShouldCollide_Any")
                }
                if(upstreamName == "ShapeFilterJS2" && expected.name == "ShouldCollide") {
                    expected = expected.copy(name = "ShouldCollide_Shape")
                }
                if(upstreamName == "CharacterVirtual" && expected.name == "GetListener") {
                    // The native getter returns Jolt's base callback pointer. jJolt
                    // binds it as an opaque native operation and generates a typed
                    // Java convenience through Jolt.GetCharacterContactListener,
                    // avoiding an unsafe implicit base-to-derived conversion.
                    // BindTo keeps the upstream native name in the parity model;
                    // GetListenerNative is the generated Java-side spelling.
                    expected = expected.copy(name = "GetListener", returnOrPropertyType = "any")
                }
                if(upstreamName == "StateRecorderJS"
                    && expected.name in setOf("ReadBytes", "WriteBytes")
                    && expected.parameterTypes == listOf("any", "unsigned_long")) {
                    // JoltPhysics.js is wasm32, so its size_t byte count is an unsigned long.
                    // jJolt widens only this portable callback boundary to uint64/Java long;
                    // the native StateRecorder override still receives the platform size_t.
                    // BindTo gives the widened native bridge a distinct name so it cannot
                    // collide with the size_t override on a 32-bit target.
                    expected = expected.copy(
                        name = "${expected.name}Java",
                        parameterTypes = listOf("any", "unsigned_long_long")
                    )
                }
                if(upstreamName == "RVec3" || upstreamName == "RMat44") {
                    // The upstream IDL fixes RVec storage to double. jJolt emits
                    // precision-specific IDLs, so compare its float build to the
                    // same logical real-valued API rather than the scalar spelling.
                    expected = expected.copy(
                        returnOrPropertyType = expected.returnOrPropertyType.replace("double", "float"),
                        parameterTypes = expected.parameterTypes.map { it.replace("double", "float") }
                    )
                }

                val matches = bindingMembers.any { candidate ->
                    portableWebIdlMemberMatches(expected, candidate)
                }
                if(matches) matchedDeclarations++ else missing += expected.display(upstreamName)
            }
        }

        upstream.enums.forEach { (enumName, upstreamValues) ->
            val bindingValues = binding.enums[enumName]
            if(bindingValues == null) {
                missing += "enum $enumName"
            }
            else {
                matchedDeclarations++
                (upstreamValues - bindingValues).forEach { missing += "$enumName.$it" }
                matchedDeclarations += (upstreamValues intersect bindingValues).size
            }
        }

        if(missing.isNotEmpty()) {
            throw GradleException("JoltPhysics.js WebIDL parity failed with ${missing.size} unmatched portable declaration(s):\n - ${missing.sorted().joinToString("\n - ")}")
        }
        logger.lifecycle("Verified $matchedDeclarations portable WebIDL declarations against JoltPhysics.js ${LibExt.joltJsVersion} (${LibExt.joltJsCommit}).")
    }
}

fun normalizeGeneratedJavaType(rawType: String): String = rawType
    .replace(Regex("""\b(?:gen\.web\.|gen\.c\.)?jolt\."""), "")
    .replace(Regex("""\s+"""), "")

fun generatedJavaApi(root: File): Map<String, Set<String>> {
    if(!root.isDirectory) {
        throw GradleException("Generated API root does not exist: $root")
    }

    return root.walkTopDown()
        .filter { it.isFile && it.extension == "java" }
        .associate { javaFile ->
            val relativePath = javaFile.relativeTo(root).invariantSeparatorsPath
            val unit = StaticJavaParser.parse(javaFile)
            val declaration = unit.types
                .filter { it.nameAsString == javaFile.nameWithoutExtension }
                .mapNotNull { it.toClassOrInterfaceDeclaration().orElse(null) }
                .firstOrNull()
            val members = linkedSetOf<String>()

            declaration?.methods?.forEach { method ->
                if((method.isPublic || method.isProtected)
                    && !method.nameAsString.startsWith("internal_")
                    && method.nameAsString != "native_new"
                    && method.nameAsString != "deleteNative") {
                    val parameters = method.parameters.joinToString(",") { parameter ->
                        normalizeGeneratedJavaType(parameter.typeAsString) + if(parameter.isVarArgs) "..." else ""
                    }
                    val staticMarker = if(method.isStatic) "static " else ""
                    members += "$staticMarker${method.nameAsString}($parameters):${normalizeGeneratedJavaType(method.typeAsString)}"
                }
            }

            declaration?.constructors?.forEach { constructor ->
                val parameters = constructor.parameters.map { normalizeGeneratedJavaType(it.typeAsString) }
                val dummyConstructor = parameters == listOf("byte", "char")
                if((constructor.isPublic || constructor.isProtected) && !dummyConstructor) {
                    members += "<constructor>(${parameters.joinToString(",")})"
                }
            }

            relativePath to members
        }
}

tasks.register("verifyGeneratedBackendParity") {
    group = "verification"
    description = "Verifies that every generated portable Java operation exists on JNI/Android, FFM, TeaVM C and Wasm."
    dependsOn("jParser_generate")

    val apiRoots = linkedMapOf(
        "core" to file("../core/src/main/java/jolt"),
        "JNI and Android JNI" to file("../shared/jni/src/main/java/jolt"),
        "FFM" to file("../desktop/ffm/src/main/java/jolt"),
        "TeaVM C" to file("../shared/c/src/main/java/gen/c/jolt"),
        "Wasm" to file("../web/wasm/src/main/java/gen/web/jolt")
    )
    inputs.files(apiRoots.values)
    inputs.file(file("../android/jni/build.gradle.kts"))

    doLast {
        val coreApi = generatedJavaApi(apiRoots.getValue("core"))
        val missing = mutableListOf<String>()
        var verifiedOperations = 0

        apiRoots.filterKeys { it != "core" }.forEach { (backend, root) ->
            val backendApi = generatedJavaApi(root)
            coreApi.forEach { (relativePath, coreMembers) ->
                val backendMembers = backendApi[relativePath]
                if(backendMembers == null) {
                    missing += "$backend: missing generated type $relativePath"
                }
                else {
                    (coreMembers - backendMembers).forEach { member ->
                        missing += "$backend: $relativePath -> $member"
                    }
                    verifiedOperations += coreMembers.count { it in backendMembers }
                }
            }
        }

        val androidBuild = file("../android/jni/build.gradle.kts").readText()
        if(!androidBuild.contains("api(project(\":jolt:shared:jni\"))")) {
            missing += "Android JNI does not consume the verified shared JNI binding surface"
        }
        val criticalFfmDowncalls = fileTree(apiRoots.getValue("FFM")) {
            include("**/*.java")
        }.filter { javaFile -> javaFile.readText().contains("FFMDowncallHelper.downcallCritical(") }
        criticalFfmDowncalls.forEach { javaFile ->
            missing += "FFM callback safety: ${javaFile.relativeTo(apiRoots.getValue("FFM")).invariantSeparatorsPath} still uses a critical downcall"
        }

        if(missing.isNotEmpty()) {
            throw GradleException("Generated backend parity failed with ${missing.size} omission(s):\n - ${missing.sorted().joinToString("\n - ")}")
        }
        logger.lifecycle("Verified $verifiedOperations generated portable operation implementations across JNI/Android, FFM, TeaVM C and Wasm.")
    }
}

tasks.register("verifyGeneratedSourcesClean") {
    group = "verification"
    description = "Regenerates all bindings and fails when the checked-in generated sources change."
    dependsOn("jParser_generate")

    val generatedPaths = listOf(
        "jolt/core/src/main/java",
        "jolt/shared/jni/src/main/java",
        "jolt/desktop/ffm/src/main/java",
        "jolt/shared/c/src/main/java",
        "jolt/web/wasm/src/main/java"
    )
    doLast {
        val process = ProcessBuilder(
            listOf("git", "status", "--porcelain=v1", "--untracked-files=all", "--") + generatedPaths
        )
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
        val status = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if(exitCode != 0) {
            throw GradleException("Unable to inspect generated-source status (git exit $exitCode):\n$status")
        }
        if(status.isNotEmpty()) {
            throw GradleException("Binding regeneration changed checked-in sources. Run :jolt:builder:jParser_generate and commit the result:\n$status")
        }
        logger.lifecycle("Verified that binding regeneration leaves the checked-in sources unchanged.")
    }
}
