plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":lib"))
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

application {
    mainClass = "MainKt"
}
