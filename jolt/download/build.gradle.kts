import org.gradle.api.file.RelativePath
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java")
}

val buildDirFile = layout.buildDirectory.get().asFile
val joltSourceRoot = buildDirFile.resolve("jolt-source")
val joltIncludeDir = joltSourceRoot.resolve("Jolt")
val joltArchiveFile = buildDirFile.resolve("tmp/jolt-source.zip")

tasks.register("jolt_download_source") {
    group = "jolt"
    description = "Download Jolt Physics ${LibExt.joltVersion} source into the build directory."
    inputs.property("joltVersion", LibExt.joltVersion)
    outputs.dir(joltSourceRoot)
    onlyIf {
        !joltIncludeDir.isDirectory
    }

    doLast {
        val url = "https://github.com/jrouwe/JoltPhysics/archive/refs/tags/v${LibExt.joltVersion}.zip"
        println("URL: $url")
        delete(joltSourceRoot)
        joltArchiveFile.parentFile.mkdirs()
        URL(url).openStream().use { input ->
            Files.copy(input, joltArchiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        copy {
            from(zipTree(joltArchiveFile)) {
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
            into(joltSourceRoot)
        }
        delete(joltArchiveFile)
    }
}
