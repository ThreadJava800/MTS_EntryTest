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
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

application {
    mainClass = "MainKt"
}
