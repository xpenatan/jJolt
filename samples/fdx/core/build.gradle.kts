plugins {
    id("java-library")
}

dependencies {
    if(LibExt.useRepoLibs) {
        compileOnly("com.github.xpenatan.jJolt:core:${LibExt.exampleVersion}")
        compileOnly("com.github.xpenatan.jJolt:jolt-fdx:${LibExt.exampleVersion}")
    }
    else {
        compileOnly(project(":jolt:core"))
        compileOnly(project(":extensions:fdx"))
    }

    api("${LibExt.fdxGroup}:application:${LibExt.fdxVersion}")
    api("${LibExt.fdxGroup}:display:${LibExt.fdxVersion}")
    api("${LibExt.fdxGroup}:graphics:${LibExt.fdxVersion}")
    api("${LibExt.fdxGroup}:g3d:${LibExt.fdxVersion}")
    api("${LibExt.fdxGroup}:asset_manager:${LibExt.fdxVersion}")
}

java {
    sourceCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
    targetCompatibility = JavaVersion.toVersion(LibExt.javaFFMTarget)
}
