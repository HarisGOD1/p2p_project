repositories {
    maven("https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/")
    maven("https://jitpack.io")
    maven("https://artifacts.consensys.net/public/maven/maven/")
    mavenCentral()
}
plugins {
    id("buildsrc.convention.kotlin-jvm")

    id("com.google.protobuf").version("0.9.4")
    application
}

dependencies {

    implementation("io.libp2p:jvm-libp2p:1.2.2-RELEASE")
    implementation("io.netty:netty-all:4.2.13.Final")
    implementation("com.google.protobuf:protobuf-java:4.34.1")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.26.0")
    implementation(project(":traversal_utils"))
    testImplementation(kotlin("test"))

    implementation("org.pcap4j:pcap4j-core:1.8.2")
    implementation("org.pcap4j:pcap4j-packetfactory-static:1.8.2")
}

application {
    mainClass = "su.kamil.dev.chat.example.ChatterCliKt"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.34.1"
    }
}