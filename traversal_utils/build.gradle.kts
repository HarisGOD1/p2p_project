plugins {
    kotlin("jvm")
}

group = "su.kamil.dev"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

val nativeOutputDir = layout.buildDirectory.dir("native")

tasks.register<Exec>("build_ip_forger.c") {
    val output = nativeOutputDir.get().file("ip_forger").asFile
    doFirst {
        output.parentFile.mkdirs()
    }
    commandLine(
        "gcc",
        "src/main/native/ip_forger.c",
        "-o",
        output.absolutePath
    )
}
tasks.register<Exec>("build_tcp_forger.c") {
    val output = nativeOutputDir.get().file("tcp_forger").asFile
    doFirst {
        output.parentFile.mkdirs()
    }
    commandLine(
        "gcc",
        "src/main/native/tcp_forger.c",
        "-o",
        output.absolutePath
    )
}

tasks.named("compileKotlin") {
    dependsOn("build_ip_forger.c","build_tcp_forger.c")
}
