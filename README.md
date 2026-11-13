# jJolt

![Build](https://github.com/xpenatan/jJolt/actions/workflows/snapshot.yml/badge.svg)

[![Maven Central Version](https://img.shields.io/maven-central/v/com.github.xpenatan.jJolt/core)](https://central.sonatype.com/namespace/com.github.xpenatan.jJolt)
[![Static Badge](https://img.shields.io/badge/snapshot---SNAPSHOT-red)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/xpenatan/jJolt/)

jJolt is a Java binding for the C++ library [Jolt Physics](https://github.com/jrouwe/JoltPhysics), utilizing JNI/FFM for desktop, JNI for Android, and Emscripten for web platforms. It provides a one-to-one correspondence with the C++ code, meaning it mirrors the exact same class and method names.

The binding leverages [jParser](https://github.com/xpenatan/jParser), a custom C/C++ build tool and WebIDL Java code generator, which automatically generates 99% of all classes. Only a small number of essential classes are coded manually, making updates to new Jolt Physics versions quick and efficient.

![image](https://github.com/user-attachments/assets/98ab1f09-6b00-4665-8082-40179f3fbf74)

## Web/TeaVM Examples

* [Jolt-Samples-GL](https://xpenatan.github.io/jJolt/snapshot/gl/samples/)

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

// Desktop module
dependencies {
   implementation("com.github.xpenatan.jJolt:desktop-jni:-SNAPSHOT")
   implementation("com.github.xpenatan.jJolt:desktop-ffm:-SNAPSHOT")
}

// TeaVM module
dependencies {
   implementation("com.github.xpenatan.jJolt:web-wasm:-SNAPSHOT")
}

// Android module
dependencies {
   implementation("com.github.xpenatan.jJolt:android-jni:-SNAPSHOT")
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

The samples use the published jJolt artifacts by default, as configured by
`examplesUseMavenArtifacts = "true"` in `gradle/libs.versions.toml`. To use
the local jJolt projects instead, add the override to the root
`local.properties` file:

```properties
examplesUseMavenArtifacts=false
```

Then run the desired sample. It uses the version configured by `exampleVersion` in `gradle/libs.versions.toml`:

```bash
./gradlew :samples:gdx:gl:platforms:desktop-jni:jolt_samples_run_desktop

# Test a release without editing the version catalog.
./gradlew -PexampleVersion=RELEASE_VERSION :samples:gdx:gl:platforms:desktop-jni:jolt_samples_run_desktop
```

When `local.properties` contains `examplesUseMavenArtifacts`, that value takes precedence. If the property or file is absent, the value from `gradle/libs.versions.toml` is used. Published mode uses Maven artifacts for Jolt core and platform natives.

## How To Run The Samples

### libGDX

- Desktop JNI: `./gradlew :samples:gdx:gl:platforms:desktop-jni:jolt_samples_run_desktop`
- Desktop TeaVM/C: `./gradlew :samples:gdx:gl:platforms:desktop-c:jolt_gdx_desktop_c_run`
- Web: `./gradlew :samples:gdx:gl:platforms:web:jolt_samples_run_teavm`
- Android: `./gradlew :samples:gdx:gl:platforms:android:assembleDebug`

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
