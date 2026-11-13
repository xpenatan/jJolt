pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        google()
        gradlePluginPortal()
        mavenCentral()
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

// Examples
include(":samples:gdx:shared")
include(":samples:gdx:gl:core")

include(":samples:shared")
include(":samples:gdx:gl:platforms:desktop-jni")
include(":samples:gdx:gl:platforms:desktop-c")
include(":samples:gdx:gl:platforms:web")
include(":samples:gdx:gl:platforms:android")
