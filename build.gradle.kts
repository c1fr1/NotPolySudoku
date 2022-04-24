import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 Should be one of the following depending on your operating system or the platform you are targeting

 x64 Windows - "windows"
 x86 Windows - "windows-x86"
 arm64 Windows - "windows-arm64"

 x64 Linux - "linux"
 arm 64 Linux - "linux-arm64"
 arm 32 Linux - "linux-arm32"
 */
val osString = "windows"
val lwjglNatives = "natives-${osString}"

//this can be any valid tag on this repo, or a short commit id if you are using Jitpack.
//More info at https://jitpack.io/.
val enignetsVersion = "1.0.1a_5"

val lwjglVersion = "3.2.3"
val jomlVersion = "1.10.1"

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "c1fr1"
version = "0.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")

    maven {
        url = uri("https://maven.pkg.github.com/c1fr1/Enignets")
        credentials {
            username = project.property("githubUsername").toString()
            password = project.property("githubPAT").toString()
        }
    }
}



dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")
    implementation("org.joml", "joml", jomlVersion)

    implementation("edu.cmu.sphinx", "sphinx4-core", "5prealpha-SNAPSHOT")
    implementation("edu.cmu.sphinx", "sphinx4-data", "5prealpha-SNAPSHOT")

    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)

    implementation("c1fr1:enignets:${enignetsVersion}")//using specific version on github packages
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}