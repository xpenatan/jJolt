pluginManagement {
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
        id("io.github.libfdx") version "-SNAPSHOT"
    }
}

// Core
include(":jolt:jolt-build")
include(":jolt:jolt-base")
include(":jolt:jolt-core")
include(":jolt:jolt-jni")
include(":jolt:jolt-ffm")
include(":jolt:jolt-web")
include(":jolt:jolt-android")

// Examples
include(":examples:samples:core")
include(":examples:samples:desktop")
include(":examples:samples:web")
include(":examples:samples:android")

//val localLibfdxDir = file("../libfdx")
//if(localLibfdxDir.isDirectory) {
//    includeBuild(localLibfdxDir) {
//        dependencySubstitution {
//            substitute(module("io.github.libfdx:application")).using(project(":libfdx:runtime:application"))
//            substitute(module("io.github.libfdx:display")).using(project(":libfdx:runtime:display"))
//            substitute(module("io.github.libfdx:graphics")).using(project(":libfdx:graphics:api"))
//            substitute(module("io.github.libfdx:g3d")).using(project(":libfdx:graphics:g3d"))
//            substitute(module("io.github.libfdx:asset_manager")).using(project(":libfdx:assets:manager"))
//            substitute(module("io.github.libfdx:backend_desktop")).using(project(":libfdx:backends:desktop"))
//            substitute(module("io.github.libfdx:backend_web")).using(project(":libfdx:backends:web"))
//            substitute(module("io.github.libfdx:backend_android")).using(project(":libfdx:backends:android"))
//            substitute(module("io.github.libfdx:gl_desktop")).using(project(":libfdx:extensions:graphics:gl:platform:desktop"))
//            substitute(module("io.github.libfdx:gl_web")).using(project(":libfdx:extensions:graphics:gl:platform:web"))
//            substitute(module("io.github.libfdx:vulkan_desktop")).using(project(":libfdx:extensions:graphics:vulkan:platform:desktop"))
//            substitute(module("io.github.libfdx:vulkan_android_jni")).using(project(":libfdx:extensions:graphics:vulkan:platform:android_jni"))
//            substitute(module("io.github.libfdx:wgpu_core")).using(project(":libfdx:extensions:graphics:wgpu:core"))
//            substitute(module("io.github.libfdx:wgpu_desktop_jni")).using(project(":libfdx:extensions:graphics:wgpu:platform:desktop_jni"))
//            substitute(module("io.github.libfdx:wgpu_desktop_ffm")).using(project(":libfdx:extensions:graphics:wgpu:platform:desktop_ffm"))
//            substitute(module("io.github.libfdx:wgpu_android_jni")).using(project(":libfdx:extensions:graphics:wgpu:platform:android_jni"))
//            substitute(module("io.github.libfdx:wgpu_web")).using(project(":libfdx:extensions:graphics:wgpu:platform:web"))
//        }
//    }
//}
//includeBuild("E:\\Dev\\Projects\\java\\libfdx\\libfdx\\tools\\gradle-plugin")

//includeBuild("E:\\Dev\\Projects\\java\\jParser") {
//    dependencySubstitution {
//        substitute(module("com.github.xpenatan.jParser:gen-build")).using(project(":jParser:gen:gen-build"))
//        substitute(module("com.github.xpenatan.jParser:gen-build-tool")).using(project(":jParser:gen:gen-build-tool"))
//        substitute(module("com.github.xpenatan.jParser:gen-core")).using(project(":jParser:gen:gen-core"))
//        substitute(module("com.github.xpenatan.jParser:gen-jni")).using(project(":jParser:gen:gen-jni"))
//        substitute(module("com.github.xpenatan.jParser:gen-ffm")).using(project(":jParser:gen:gen-ffm"))
//        substitute(module("com.github.xpenatan.jParser:gen-idl")).using(project(":jParser:gen:gen-idl"))
//        substitute(module("com.github.xpenatan.jParser:gen-web")).using(project(":jParser:gen:gen-web"))
//        substitute(module("com.github.xpenatan.jParser:api-core")).using(project(":jParser:api:api-core"))
//        substitute(module("com.github.xpenatan.jParser:api-web")).using(project(":jParser:api:api-web"))
//        substitute(module("com.github.xpenatan.jParser:runtime-base")).using(project(":jParser:runtime:runtime-base"))
//        substitute(module("com.github.xpenatan.jParser:runtime-core")).using(project(":jParser:runtime:runtime-core"))
//        substitute(module("com.github.xpenatan.jParser:runtime-web")).using(project(":jParser:runtime:runtime-web"))
//        substitute(module("com.github.xpenatan.jParser:runtime-web_wasm")).using(project(":jParser:runtime:runtime-web_wasm"))
//        substitute(module("com.github.xpenatan.jParser:runtime-jni")).using(project(":jParser:runtime:runtime-jni"))
//        substitute(module("com.github.xpenatan.jParser:runtime-ffm")).using(project(":jParser:runtime:runtime-ffm"))
//        substitute(module("com.github.xpenatan.jParser:runtime-android")).using(project(":jParser:runtime:runtime-android"))
//        substitute(module("com.github.xpenatan.jParser:loader-core")).using(project(":jParser:loader:loader-core"))
//        substitute(module("com.github.xpenatan.jParser:loader-web")).using(project(":jParser:loader:loader-web"))
//    }
//}
