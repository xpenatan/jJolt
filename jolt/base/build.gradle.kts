plugins {
    id("java")
}

dependencies {
    implementation(libs.jparserLoaderCore)
    implementation(libs.jparserRuntimeBase)
    implementation(libs.jparserRuntimeCore)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaMainTarget.get())
}
