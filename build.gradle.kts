import java.util.Properties

plugins {
    id("java")
    alias(libs.plugins.easyPublishing)
}

val examplesUseMavenArtifacts = Properties()
    .apply { rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load) }
    .getProperty("examplesUseMavenArtifacts", libs.versions.examplesUseMavenArtifacts.get())
    .toBooleanStrict()
val samplesRepoVersion = providers.gradleProperty("exampleVersion")
    .orElse(libs.versions.exampleVersion)
    .get()
val jJoltGroup = libs.versions.projectGroup.get()

extra["examplesUseMavenArtifacts"] = examplesUseMavenArtifacts

allprojects  {
    val isSampleProject = path.startsWith(":samples:")

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
        ":extensions:gdx:gl"
    )

    groupId.set(libs.versions.projectGroup)
    releaseVersion.set(libs.versions.jJoltRelease)
    snapshotVersion.set(libs.versions.jJoltSnapshot)

    snapshotRepositoryUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
    releaseRepositoryUrl.set("https://central.sonatype.com")
    username.set(providers.environmentVariable("CENTRAL_PORTAL_USERNAME"))
    password.set(providers.environmentVariable("CENTRAL_PORTAL_PASSWORD"))
    signingKey.set(providers.environmentVariable("SIGNING_KEY"))
    signingPassword.set(providers.environmentVariable("SIGNING_PASSWORD"))

    pomName.set(libs.versions.projectName)
    pomDescription.set("Jolt Physics Java Bindings")
    projectUrl.set("https://github.com/xpenatan/jJolt")

    developerId.set("Xpe")
    developerName.set("Natan")

    scmUrl.set("https://github.com/xpenatan/jJolt")
    scmConnection.set("scm:git:https://github.com/xpenatan/jJolt.git")
    scmDeveloperConnection.set("scm:git:ssh://git@github.com/xpenatan/jJolt.git")
}
