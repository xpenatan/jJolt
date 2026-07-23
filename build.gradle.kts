plugins {
    id("java")
    alias(libs.plugins.easyPublishing)
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
        resolutionStrategy.eachDependency {
            val isJParserRuntime = requested.group == "com.github.xpenatan.jParser" && (
                    requested.name.startsWith("api-") ||
                    requested.name.startsWith("loader-") ||
                    requested.name.startsWith("runtime-")
            )
            if(isJParserRuntime) {
                useVersion(libs.versions.jparserVersion.get())
            }
            else if(requested.group == "com.github.xpenatan.gdx-teavm") {
                useVersion(libs.versions.gdxTeavmVersion.get())
            }
        }
    }
}

easyPublishing {
    modules(
        ":jolt:core",
        ":jolt:shared:jni",
        ":jolt:shared:c",
        ":jolt:desktop:jni",
        ":jolt:desktop:ffm",
        ":jolt:desktop:c",
        ":jolt:web:wasm",
        ":jolt:android:jni",
        ":extensions:gdx:gl",
        ":extensions:gdx:wgpu",
        ":extensions:fdx"
    )

    groupId.set(libs.versions.projectGroup.get())
    releaseVersion.set(providers.gradleProperty("version"))
    snapshotVersion.set(libs.versions.snapshotVersion.get())

    snapshotRepositoryUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
    releaseRepositoryUrl.set("https://central.sonatype.com")
    username.set(providers.environmentVariable("CENTRAL_PORTAL_USERNAME"))
    password.set(providers.environmentVariable("CENTRAL_PORTAL_PASSWORD"))
    signingKey.set(providers.environmentVariable("SIGNING_KEY"))
    signingPassword.set(providers.environmentVariable("SIGNING_PASSWORD"))

    pomName.set(libs.versions.projectName.get())
    pomDescription.set("Jolt Physics Java Bindings")
    projectUrl.set("https://github.com/xpenatan/jJolt")

    developerId.set("Xpe")
    developerName.set("Natan")

    scmUrl.set("https://github.com/xpenatan/jJolt")
    scmConnection.set("scm:git:https://github.com/xpenatan/jJolt.git")
    scmDeveloperConnection.set("scm:git:ssh://git@github.com/xpenatan/jJolt.git")
}
