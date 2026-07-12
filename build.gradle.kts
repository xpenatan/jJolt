plugins {
    id("java")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }

    val kotlinVersion = "2.1.10"

    dependencies {
        classpath("com.android.tools.build:gradle:8.12.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects  {

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("http://teavm.org/maven/repository/")
            isAllowInsecureProtocol = true
        }
    }

    configurations.configureEach {
        // Check for updates every sync
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
        val jParserSnapshotModules = setOf(
            "gen-build",
            "gen-build-tool",
            "gen-c",
            "gen-core",
            "gen-jni",
            "gen-ffm",
            "gen-idl",
            "gen-web",
            "api-core",
            "api-web",
            "runtime-base",
            "runtime-core",
            "runtime-c",
            "runtime-web",
            "runtime-jni",
            "runtime-desktop-c",
            "runtime-desktop-c_windows_x64",
            "runtime-desktop-c_linux_x64",
            "runtime-desktop-c_mac_x64",
            "runtime-desktop-c_mac_arm64",
            "runtime-desktop-jni",
            "runtime-desktop-jni_windows_x64",
            "runtime-desktop-jni_linux_x64",
            "runtime-desktop-jni_mac_x64",
            "runtime-desktop-jni_mac_arm64",
            "runtime-desktop-ffm",
            "runtime-desktop-ffm_windows_x64",
            "runtime-desktop-ffm_linux_x64",
            "runtime-desktop-ffm_mac_x64",
            "runtime-desktop-ffm_mac_arm64",
            "runtime-android",
            "loader-c",
            "loader-core",
            "loader-web"
        )
        resolutionStrategy.eachDependency {
            if(requested.group == "com.github.xpenatan.jParser" && requested.name in jParserSnapshotModules) {
                useVersion(LibExt.jParserVersion)
            }
            if(requested.group == "com.github.xpenatan.gdx-teavm") {
                useVersion(LibExt.gdxTeaVMVersion)
            }
        }
    }
}

apply(plugin = "publish")
