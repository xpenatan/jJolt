plugins {
    alias(libs.plugins.androidLibrary)
}

val moduleName = "android-jni"
val supportedAndroidAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val desktopJParserRuntimeModules = listOf(
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

android {
    namespace = "jolt"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += supportedAndroidAbis
        }
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("$projectDir/../../builder/build/c++/libs/android")
        }
        named("androidTest") {
            java.srcDir(file("../../tests/src/test/java"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    publishing {
        singleVariant("release")
    }
}

dependencies {
    api(project(":jolt:shared:jni")) {
        desktopJParserRuntimeModules.forEach {
            exclude(group = "com.github.xpenatan.jParser", module = it)
        }
    }
    api(libs.jparserRuntimeJni)
    api(libs.jparserRuntimeAndroid)
    api(libs.jparserApiCore)
    api(libs.jparserLoaderCore)

    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestRunner)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = moduleName
        }
    }
}

afterEvaluate {
    publishing {
        publications.named<MavenPublication>("maven") {
            from(components["release"])
        }
    }
}

tasks.named("clean") {
    doFirst {
        val srcPath = "$projectDir/src/main/"
        project.delete(files(srcPath))
    }
}

tasks.matching { it.name.endsWith("JavaWithJavac") || it.name == "preBuild" }.configureEach {
    dependsOn(":jolt:builder:jParser_generate")
}
