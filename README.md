# jJolt

![Build](https://github.com/xpenatan/jJolt/actions/workflows/snapshot.yml/badge.svg)

[![Maven Central Version](https://img.shields.io/maven-central/v/com.github.xpenatan.jJolt/core)](https://central.sonatype.com/namespace/com.github.xpenatan.jJolt)
[![Static Badge](https://img.shields.io/badge/snapshot---SNAPSHOT-red)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/xpenatan/jJolt/)

jJolt is a Java binding for the C++ library [Jolt Physics](https://github.com/jrouwe/JoltPhysics), utilizing JNI/FFM for desktop, JNI for Android, and Emscripten for web platforms. It provides a one-to-one correspondence with the C++ code, meaning it mirrors the exact same class and method names. Its samples and rendering integrations target libfdx.

The binding leverages [jParser](https://github.com/xpenatan/jParser), a custom C/C++ build tool and WebIDL Java code generator, which automatically generates 99% of all classes. Only a small number of essential classes are coded manually, making updates to new Jolt Physics versions quick and efficient.

![image](https://github.com/user-attachments/assets/98ab1f09-6b00-4665-8082-40179f3fbf74)

## Web/TeaVM Examples

* [Jolt-Samples-GL](https://xpenatan.github.io/jJolt/snapshot/gl/samples/)
* [Jolt-Samples-WGPU](https://xpenatan.github.io/jJolt/snapshot/wgpu/samples/)

## Platform Status

| Emscripten | Windows | Linux | Mac | Android | iOS |
|:----------:|:-------:|:-----:|:---:|:-------:|:---:|
| Yes | Yes | Yes | Yes | Yes | No |

* Yes: Have a working build.
* No: Build not ready.

## Setup

```groovy
// Use -SNAPSHOT or any released git-tag version.

// Add repository to root Gradle.
repositories {
    mavenLocal()
    mavenCentral()
    maven { url "https://central.sonatype.com/repository/maven-snapshots/" }
}

// Core module
implementation("com.github.xpenatan.jJolt:core:-SNAPSHOT")
implementation("io.github.libfdx:application:-SNAPSHOT")
implementation("io.github.libfdx:g3d:-SNAPSHOT")

// Desktop module
dependencies {
   implementation("com.github.xpenatan.jJolt:desktop-jni:-SNAPSHOT")
   implementation("com.github.xpenatan.jJolt:desktop-ffm:-SNAPSHOT")
   implementation("io.github.libfdx:backend_desktop:-SNAPSHOT")
   implementation("io.github.libfdx:gl_desktop:-SNAPSHOT")
   implementation("io.github.libfdx:vulkan_desktop:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_desktop_jni:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_desktop_ffm:-SNAPSHOT")
}

// TeaVM module
dependencies {
   implementation("com.github.xpenatan.jJolt:web-wasm:-SNAPSHOT")
   implementation("io.github.libfdx:backend_web:-SNAPSHOT")
   implementation("io.github.libfdx:gl_web:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_web:-SNAPSHOT")
}

// Android module
dependencies {
   implementation("com.github.xpenatan.jJolt:android-jni:-SNAPSHOT")
   implementation("io.github.libfdx:backend_android:-SNAPSHOT")
   implementation("io.github.libfdx:wgpu_android_jni:-SNAPSHOT")
   implementation("io.github.libfdx:vulkan_android_jni:-SNAPSHOT")
}
```

## Notes

* Use `JoltSettings` and `JoltInterface` for lifecycle management. `JoltInterface` initializes Jolt, owns the physics system, allocator, job system, and the three collision-filter objects supplied through settings, and shuts them down in the correct order. Objects returned by its getters are borrowed.
* In most classes, new instances are created using `JoltNew.[TYPE]` instead of standard Java constructors. This approach is necessary due to limitations in constructor overloading within WebIDL when targeting Emscripten.
* Methods that return an object typically return a temporary object. You should not retain a reference to it, as calling the method again with another instance will overwrite the previously returned object.
* Classes are not disposed automatically; the dispose method must be called when they are no longer in use. However, classes in a WebIDL file marked with `NoDelete` do not require disposal.

## Source Build Prerequisites

- Java 17 or later
- Gradle
- Android NDK (only needed for Android native artifacts)
- [Mingw64](https://github.com/niXman/mingw-builds-binaries/releases) or [Visual Studio C++](https://visualstudio.microsoft.com/vs/community/) (only needed for desktop native artifacts)
- [Emscripten](https://emscripten.org/) (only needed for WebAssembly native artifacts)

To try the samples with your build, set `useRepoLibs = "false"` in `gradle/libs.versions.toml`. This will use the local jJolt source code instead of the remote repository.

## How To Run The Samples

- Desktop OpenGL/JNI: `./gradlew :samples:gdx:gl:platforms:desktop-jni:jolt_sample_desktop_gl_jni_run`
- Desktop WGPU/JNI: `./gradlew :samples:gdx:gl:platforms:desktop-jni:jolt_sample_desktop_wgpu_jni_run`
- Desktop WGPU/FFM: `./gradlew :samples:gdx:gl:platforms:desktop-jni:jolt_sample_desktop_wgpu_ffm_run`
- Desktop Vulkan/JNI: `./gradlew :samples:gdx:gl:platforms:desktop-jni:jolt_sample_desktop_vulkan_jni_run`
- Desktop Vulkan/FFM: `./gradlew :samples:gdx:gl:platforms:desktop-jni:jolt_sample_desktop_vulkan_ffm_run`
- WebGL JS/Wasm: `./gradlew :samples:fdx:platforms:web:jolt_sample_webgl_js_build :samples:fdx:platforms:web:jolt_sample_webgl_wasm_build`
- WebGPU JS: `./gradlew :samples:fdx:platforms:web:jolt_sample_webgpu_js_build`
- Android GLES/WGPU/Vulkan: `./gradlew :samples:gdx:gl:platforms:android:jolt_sample_android_gles_build :samples:gdx:gl:platforms:android:jolt_sample_android_wgpu_jni_build :samples:gdx:gl:platforms:android:jolt_sample_android_vulkan_build`

## How To Build From Source

```bash
./gradlew :jolt:download:jolt_download_source

# Generate binding sources for JNI, FFM, and web.
./gradlew :jolt:builder:jParser_generate

# Generate and build native platform targets.
./gradlew :jolt:builder:jParser_build_windows64_jni :jolt:builder:jParser_build_windows64_ffm :jolt:builder:jParser_build_windows64_teavm_c
./gradlew :jolt:builder:jParser_build_linux64_jni :jolt:builder:jParser_build_linux64_ffm :jolt:builder:jParser_build_linux64_teavm_c
./gradlew :jolt:builder:jParser_build_mac64_jni :jolt:builder:jParser_build_mac64_ffm :jolt:builder:jParser_build_mac64_teavm_c
./gradlew :jolt:builder:jParser_build_macArm_jni :jolt:builder:jParser_build_macArm_ffm :jolt:builder:jParser_build_macArm_teavm_c
./gradlew :jolt:builder:jParser_build_android_jni
./gradlew :jolt:builder:jParser_build_web_wasm
```
