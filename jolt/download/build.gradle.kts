import org.gradle.api.file.RelativePath
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java")
}

val buildDirFile = layout.buildDirectory.get().asFile
val joltSourceRoot = buildDirFile.resolve("jolt-source")
val joltArchiveFile = buildDirFile.resolve("tmp/jolt-source.zip")
val joltJsSourceRoot = buildDirFile.resolve("jolt-js-source")
val joltJsArchiveFile = buildDirFile.resolve("tmp/jolt-js-source.zip")

fun downloadAndExtract(url: String, archiveFile: File, outputDir: File) {
    println("URL: $url")
    delete(outputDir)
    archiveFile.parentFile.mkdirs()
    URL(url).openStream().use { input ->
        Files.copy(input, archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    copy {
        from(zipTree(archiveFile)) {
            eachFile {
                val strippedSegments = relativePath.segments.drop(1)
                if(strippedSegments.isEmpty()) {
                    exclude()
                }
                else {
                    relativePath = RelativePath(!isDirectory, *strippedSegments.toTypedArray())
                }
            }
            includeEmptyDirs = false
        }
        into(outputDir)
    }
    delete(archiveFile)
}

tasks.register("jolt_download_source") {
    group = "jolt"
    description = "Download the pinned Jolt Physics and JoltPhysics.js sources into the build directory."
    inputs.property("joltVersion", LibExt.joltVersion)
    inputs.property("joltCommit", LibExt.joltCommit)
    inputs.property("joltJsVersion", LibExt.joltJsVersion)
    inputs.property("joltJsCommit", LibExt.joltJsCommit)
    outputs.dir(joltSourceRoot)
    outputs.dir(joltJsSourceRoot)

    doLast {
        downloadAndExtract(
            "https://github.com/jrouwe/JoltPhysics/archive/${LibExt.joltCommit}.zip",
            joltArchiveFile,
            joltSourceRoot
        )
        downloadAndExtract(
            "https://github.com/jrouwe/JoltPhysics.js/archive/${LibExt.joltJsCommit}.zip",
            joltJsArchiveFile,
            joltJsSourceRoot
        )
    }
}
