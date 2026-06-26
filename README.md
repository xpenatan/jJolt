# xJolt

![Build](https://github.com/xpenatan/xJolt/actions/workflows/snapshot.yml/badge.svg)

[![Maven Central Version](https://img.shields.io/maven-central/v/com.github.xpenatan.xJolt/jolt-core)](https://central.sonatype.com/namespace/com.github.xpenatan.xJolt)
[![Static Badge](https://img.shields.io/badge/snapshot---SNAPSHOT-red)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/xpenatan/xJolt/)

xJolt is a Java binding for the C++ library [Jolt Physics](https://github.com/jrouwe/JoltPhysics), utilizing JNI/FFM for desktop, JNI for Android, and Emscripten for web platforms. It provides a one-to-one correspondence with the C++ code, meaning it mirrors the exact same class and method names. Its samples and rendering integrations target libfdx.

The binding leverages [jParser](https://github.com/xpenatan/jParser), a custom C/C++ build tool and WebIDL Java code generator, which automatically generates 99% of all classes. Only a small number of essential classes are coded manually, making updates to new Jolt Physics versions quick and efficient.

![image](https://github.com/user-attachments/assets/98ab1f09-6b00-4665-8082-40179f3fbf74)


## Web/TeaVM Examples:
* [Jolt-Samples-GL](https://xpenatan.github.io/xJolt/snapshot/gl/samples/)
* [Jolt-Samples-WGPU](https://xpenatan.github.io/xJolt/snapshot/wgpu/samples/)


### Platform status:

| Emscripten | Windows | Linux | Mac | Android | iOS |
|:----------:|:-------:|:-----:|:---:|:-------:|:---:|
|     ✅      | ✅ | ✅ |  ✅  | ✅ | ❌ |

* ✅: Have a working build.
* ❌: Build not ready.

## Setup
```groovy
// Use -SNAPSHOT" or any released git-tag version

// Add repository to Root gradle
repositories {
    mavenLocal()
    mavenCentral()
    maven { url "https://central.sonatype.com/repository/maven-snapshots/" }
}

// Core module
implementation("com.github.xpenatan.xJolt:jolt-core:-SNAPSHOT")
implementation("io.github.libfdx:application:-SNAPSHOT")
implementation("io.github.libfdx:g3d:-SNAPSHOT")

// Desktop module
dependencies {
   implementation("com.github.xpenatan.xJolt:jolt-jni:-SNAPSHOT")
   implementation("com.github.xpenatan.xJolt:jolt-ffm:-SNAPSHOT")
   implementation("io.github.libfdx:backend_desktop:-SNAPSHOT")
   implementation("io.github.libfdx:gl_desktop:-SNAPSHOT")
   implementation("io.github.libfdx:vulkan_desktop:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_desktop_jni:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_desktop_ffm:-SNAPSHOT")
}

// TeaVM module
dependencies {
   implementation("com.github.xpenatan.xJolt:jolt-web:-SNAPSHOT")
   implementation("io.github.libfdx:backend_web:-SNAPSHOT")
   implementation("io.github.libfdx:gl_web:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_web:-SNAPSHOT")
}

// Android module
dependencies {
   implementation("com.github.xpenatan.xJolt:jolt-android:-SNAPSHOT")
   implementation("io.github.libfdx:backend_android:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_android_jni:-SNAPSHOT")
   implementation("io.github.libfdx:vulkan_android_jni:-SNAPSHOT")
}
```

## Notes
* In most classes, new instances are created using `JoltNew.[TYPE]` instead of standard Java constructors. This approach is necessary due to limitations in constructor overloading within WebIDL when targeting Emscripten.
* Methods that return an object typically return a temporary object. You should not retain a reference to it, as calling the method again with another instance will overwrite the previously returned object.
* Classes are not disposed automatically; the dispose method must be called when they are no longer in use. However, classes in a WebIDL file marked with "NoDelete" do not require disposal.

## Source Build Prerequisites

- Java 17 or later
- Gradle
- Android NDK ¹
- [Mingw64](https://github.com/niXman/mingw-builds-binaries/releases) or [Visual Studio C++](https://visualstudio.microsoft.com/vs/community/) ¹
- [Emscripten](https://emscripten.org/) ¹

¹: Only need if you want to build from source.

To try the samples with your build, change `LibExt.exampleUseRepoLibs` to false in `Dependencies.kt`. This will allow you to use the local Jolt source code instead of the remote repository.

## How to run the samples
- Clone the repository
- Desktop OpenGL/JNI: `./gradlew :examples:samples:desktop:jolt_sample_desktop_gl_jni_run`
- Desktop WGPU/JNI: `./gradlew :examples:samples:desktop:jolt_sample_desktop_wgpu_jni_run`
- Desktop WGPU/FFM: `./gradlew :examples:samples:desktop:jolt_sample_desktop_wgpu_ffm_run`
- Desktop Vulkan/JNI: `./gradlew :examples:samples:desktop:jolt_sample_desktop_vulkan_jni_run`
- Desktop Vulkan/FFM: `./gradlew :examples:samples:desktop:jolt_sample_desktop_vulkan_ffm_run`
- WebGL JS/Wasm: `./gradlew :examples:samples:web:jolt_sample_webgl_js_build :examples:samples:web:jolt_sample_webgl_wasm_build`
- WebGPU JS/Wasm: `./gradlew :examples:samples:web:jolt_sample_webgpu_js_build :examples:samples:web:jolt_sample_webgpu_wasm_build`
- Android GLES/WGPU/Vulkan: `./gradlew :examples:samples:android:jolt_sample_android_gles_build :examples:samples:android:jolt_sample_android_wgpu_jni_build :examples:samples:android:jolt_sample_android_vulkan_build`


## How to build from source

```
./gradlew :jolt:jolt-build:jolt_download_source

### Build all targets if your machine can handle:
./gradlew :jolt:jolt-build:jolt_build_project_all

### Or a single platform target:
./gradlew :jolt:jolt-build:jolt_build_project_windows64
./gradlew :jolt:jolt-build:jolt_build_project_linux64
./gradlew :jolt:jolt-build:jolt_build_project_mac64 :jolt:jolt-build:build_project_macArm
./gradlew :jolt:jolt-build:jolt_build_project_teavm
```
