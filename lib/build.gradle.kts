plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    // logging
    implementation("org.slf4j:slf4j-simple:2.0.17")
}
