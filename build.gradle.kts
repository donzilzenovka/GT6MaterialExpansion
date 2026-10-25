plugins {
    `java-library`
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.9"
}

group = "zenovka"
version = "0.1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

minecraft {
    mcVersion.set("1.7.10")
    username.set("Developer")
}

repositories {
    mavenCentral()
    maven("https://nexus.gtnewhorizons.com/repository/public/")
}

// Supply the exact GT6 6.17.06 jar as libs/gregtech_1.7.10-6.17.06.jar.
// This dependency is compile-only: GT6 must already be installed in the game.
dependencies {
    compileOnly(rfg.deobf(files("libs/gregtech_1.7.10-6.17.06.jar")))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
