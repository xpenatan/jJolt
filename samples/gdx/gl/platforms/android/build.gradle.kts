plugins {
    id("com.android.application")
    id("kotlin-android")
}

group = "jolt.example.samples.app.android"

android {
    namespace = "jolt.example.samples.app.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "jolt.example.samples.app.android"
        minSdk = 24
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        named("main") {
            assets.srcDirs(project.file("../../../assets"))
            jniLibs.srcDirs("libs")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(LibExt.java8Target)
        targetCompatibility = JavaVersion.toVersion(LibExt.java8Target)
    }
    kotlinOptions {
        jvmTarget = LibExt.java8Target
    }
}
val natives: Configuration by configurations.creating

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    if(LibExt.useRepoLibs) {
        implementation("com.github.xpenatan.jJolt:android-jni:${LibExt.exampleVersion}")
    }
    else {
        implementation(project(":jolt:android:jni"))
    }

    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    implementation("com.badlogicgames.gdx:gdx-backend-android:${LibExt.gdxVersion}")
    natives("com.badlogicgames.gdx:gdx-platform:${LibExt.gdxVersion}:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:${LibExt.gdxVersion}:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:${LibExt.gdxVersion}:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-platform:${LibExt.gdxVersion}:natives-x86")

    implementation(project(":samples:shared"))
}

val androidExcludedJParserModules = listOf(
    "runtime-core",
    "runtime-desktop-jni",
    "runtime-desktop-jni_windows_x64",
    "runtime-desktop-jni_linux_x64",
    "runtime-desktop-jni_mac_x64",
    "runtime-desktop-jni_mac_arm64",
    "runtime-jni_windows_x64",
    "runtime-jni_linux_x64",
    "runtime-jni_mac_x64",
    "runtime-jni_mac_arm64"
)

configurations.configureEach {
    androidExcludedJParserModules.forEach {
        exclude(group = "com.github.xpenatan.jParser", module = it)
    }
}


tasks.register("copyAndroidNatives") {
    group = "basic-android"
    doFirst {
        natives.files.forEach { jar ->
            val outputDir = file("libs/" + jar.nameWithoutExtension.substringAfterLast("natives-"))
            outputDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outputDir)
                include("*.so")
            }
        }
    }
}

tasks.whenTaskAdded {
    if ("package" in name) {
        dependsOn("copyAndroidNatives")
    }
}
