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
    alias(libs.plugins.jparser)
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
val joltCustomSourceDir = file("src/main/cpp/custom")
val joltCustomHeader = File(joltCustomSourceDir, "JoltCustom.h")
val joltCmake = File(joltSourceRoot, "Jolt/Jolt.cmake")

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
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
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
            }
        }

        targetVariant(JParserTargets.WINDOWS64_TEAVM_C, "mt") {
            includeBaseTargetHooks.set(true)
            compileFlag("/MT")
        }
        targetVariant(JParserTargets.WINDOWS64_TEAVM_C, "md") {
            includeBaseTargetHooks.set(true)
            compileFlag("/MD")
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

tasks.register("jParser_build_windows64_teavm_c") {
    group = "jParser"
    description = "Build both MT and MD Windows x64 TeaVM C native libraries."
    dependsOn(
        "jParser_build_windows64_teavm_c_mt",
        "jParser_build_windows64_teavm_c_md"
    )
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
                val usesNativeObject = Regex("\\bNativeObject\\b").containsMatchIn(source)
                val needsTrackedVehicleAdapter = javaFile.nameWithoutExtension == "TrackedVehicleController"
                if(!usesNativeObject && !needsTrackedVehicleAdapter) {
                    return@javaFileLoop
                }

                val unit = StaticJavaParser.parse(source)
                LexicalPreservingPrinter.setup(unit)
                var changed = false

                if(usesNativeObject && unit.imports.none { it.nameAsString.endsWith(".NativeObject") }) {
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

                if(declaration != null && declaration.nameAsString == "TrackedVehicleController"
                    && declaration.getMethodsByName("GetTracks").isEmpty()) {
                    val packageName = unit.packageDeclaration.map { it.nameAsString }.orElse("")
                    unit.addImport(packageName.substringBefore(".physics") + ".Jolt")
                    declaration.addMember(StaticJavaParser.parseBodyDeclaration("""
                        public ArrayVehicleTrack GetTracks() {
                            return Jolt.GetTrackedVehicleTracks(this);
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
