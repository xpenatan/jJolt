import java.io.File
import java.util.Properties

object LibExt {
    const val groupId = "com.github.xpenatan.xJolt"
    const val libName = "xJolt"
    var isRelease = false
    var libVersion: String = ""
        get() {
            return getVersion()
        }

    const val javaMainTarget = "1.8"
    const val javaWebTarget = "17"
    const val javaFFMTarget = "25"

    // Library dependencies
    const val jParserVersion = "-SNAPSHOT"
    const val fdxGroup = "io.github.libfdx"
    const val fdxVersion = "-SNAPSHOT"

    // Example dependencies
    const val useRepoLibs = false
    const val exampleVersion = "-SNAPSHOT"
}

private fun getVersion(): String {
    var libVersion = "-SNAPSHOT"
    val file = File("gradle.properties")
    if(file.exists()) {
        val properties = Properties()
        properties.load(file.inputStream())
        val version = properties.getProperty("version")
        if(LibExt.isRelease) {
            libVersion = version
        }
    }
    else {
        if(LibExt.isRelease) {
            throw RuntimeException("properties should exist")
        }
    }
    return libVersion
}
