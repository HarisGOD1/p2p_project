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

tasks.register<Exec>("compile_ip_forger.c") {
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
tasks.register<Exec>("compile_tcp_forger.c") {
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
tasks.register<Exec>("compile_udp_forger.c") {
    val output = nativeOutputDir.get().file("udp_forger").asFile
    doFirst {
        output.parentFile.mkdirs()
    }
    commandLine(
        "gcc",
        "src/main/native/udp_forger.c",
        "-o",
        output.absolutePath
    )
}

tasks.named("compileKotlin") {
    dependsOn("compile_ip_forger.c","compile_tcp_forger.c","compile_udp_forger.c")
}
