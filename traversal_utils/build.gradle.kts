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


    implementation("org.pcap4j:pcap4j-core:1.8.2")
    implementation("org.pcap4j:pcap4j-packetfactory-static:1.8.2")

    compileOnly("org.slf4j:slf4j-api:2.0.17")
    // adds logging impl only in tests: you need to provide your
    //   own implementation if you use this lib, by adding any via 'implementation()'

    testImplementation("ch.qos.logback:logback-classic:1.5.32")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

