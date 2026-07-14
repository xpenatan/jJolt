import java.io.File
import java.util.Properties

object LibExt {
    const val groupId = "com.github.xpenatan.jJolt"
    const val libName = "jJolt"
    var isRelease = false
    var libVersion: String = ""
        get() {
            return getVersion()
        }

    const val javaMainTarget = "1.8"
    const val javaWebTarget = "17"
    const val javaFFMTarget = "25"
    const val java8Target = javaMainTarget
    const val java11Target = "11"

    // Library dependencies
    const val joltVersion = "5.3.0"
    const val jParserVersion = "1.2.3"
    const val fdxGroup = "io.github.libfdx"
    const val fdxVersion = "-SNAPSHOT"
    const val gdxVersion = "1.14.2"
    const val gdxWebGPUVersion = "0.8"
    const val teaVMVersion = "0.15.0"

    // Example dependencies
    const val gdxTeaVMVersion = "-SNAPSHOT"
    const val gdxImGuiVersion = "1.92.4.0"
    const val jUnitVersion = "4.12"
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
