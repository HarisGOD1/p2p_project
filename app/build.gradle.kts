plugins {
    id("buildsrc.convention.kotlin-jvm")

    application
}

dependencies {

    implementation("io.libp2p:jvm-libp2p:1.2.2-RELEASE")
}

application {
    mainClass = "su.kamil.dev.app.AppKt"
}
