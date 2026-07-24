plugins {
    alias(libs.plugins.androidApplication)
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
            assets.srcDirs(rootProject.file("samples/assets"))
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
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    }
}
val natives: Configuration by configurations.creating

dependencies {
    coreLibraryDesugaring(libs.androidDesugar)

    if(libs.versions.useRepoLibs.get().toBooleanStrict()) {
        implementation(libs.jjoltAndroidJni)
    }
    else {
        implementation(project(":jolt:android:jni"))
    }

    implementation(libs.gdxCore)
    implementation(libs.gdxBackendAndroid)
    natives(variantOf(libs.gdxPlatform) { classifier("natives-armeabi-v7a") })
    natives(variantOf(libs.gdxPlatform) { classifier("natives-arm64-v8a") })
    natives(variantOf(libs.gdxPlatform) { classifier("natives-x86_64") })
    natives(variantOf(libs.gdxPlatform) { classifier("natives-x86") })

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
