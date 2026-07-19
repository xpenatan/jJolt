import com.github.javaparser.StaticJavaParser
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

val downloadBuildDir = file("../download/build")
val joltSourceRoot = File(downloadBuildDir, "jolt-source")
val joltCustomSourceDir = file("src/main/cpp/custom")
val joltCustomHeader = File(joltCustomSourceDir, "JoltCustom.h")
val joltSourcePattern = "${joltSourceRoot.normalizedPath()}/Jolt/**.cpp"
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
        cppInclude(joltSourcePattern)
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
                if(unit.imports.any { it.nameAsString.endsWith(".NativeObject") }) {
                    return@javaFileLoop
                }

                val packageName = unit.packageDeclaration
                    .map { it.nameAsString }
                    .orElse("")
                val importName = if(packageName.startsWith("gen.web.")) {
                    "gen.web.com.github.xpenatan.jParser.api.NativeObject"
                }
                else {
                    "com.github.xpenatan.jParser.api.NativeObject"
                }

                LexicalPreservingPrinter.setup(unit)
                unit.addImport(importName)
                javaFile.writeText(LexicalPreservingPrinter.print(unit))
            }
        }
    }
}

tasks.matching { it.name.startsWith("jParser_build_") }.configureEach {
    mustRunAfter("jParser_generate")
}
