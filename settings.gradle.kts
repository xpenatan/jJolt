pluginManagement {
    val jParserPluginVersion = "-SNAPSHOT"

    resolutionStrategy {
        eachPlugin {
            if(requested.id.id == "com.github.xpenatan.jparser") {
                useModule("com.github.xpenatan.jParser:jparser-gradle-plugin:$jParserPluginVersion")
            }
        }
    }

    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
        gradlePluginPortal()
        maven {
            url = uri("http://teavm.org/maven/repository/")
            isAllowInsecureProtocol = true
        }
    }

    plugins {
        id("com.github.xpenatan.jparser") version jParserPluginVersion
        id("io.github.libfdx") version "-SNAPSHOT"
    }
}

rootProject.name = "jJolt"

// Core
include(":jolt:builder")
include(":jolt:download")
include(":jolt:base")
include(":jolt:core")
include(":jolt:shared:jni")
include(":jolt:shared:c")
include(":jolt:desktop:jni")
include(":jolt:desktop:ffm")
include(":jolt:desktop:c")
include(":jolt:web:wasm")
include(":jolt:android:jni")

// Extensions
include(":extensions:gdx:gl")
include(":extensions:gdx:wgpu")
include(":extensions:fdx")

// Examples
include(":samples:gdx:shared")
include(":samples:gdx:gl:core")
include(":samples:gdx:wgpu:core")

include(":samples:shared")
include(":samples:gdx:gl:platforms:desktop-jni")
include(":samples:gdx:wgpu:platforms:desktop-jni")
include(":samples:gdx:gl:platforms:desktop-c")
include(":samples:gdx:gl:platforms:web")
include(":samples:gdx:wgpu:platforms:web")
include(":samples:gdx:gl:platforms:android")

include(":samples:fdx:core")
include(":samples:fdx:platforms:desktop-jni")
include(":samples:fdx:platforms:web")
include(":samples:fdx:platforms:android")
