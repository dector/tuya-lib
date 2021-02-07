import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"

    application
}

dependencies {
    implementation("space.dector.tuyalib:library:0.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("com.eclipsesource.minimal-json:minimal-json:0.9.5")
}

repositories {
    jcenter()
    maven("https://dl.bintray.com/dector/tuya-lib")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xopt-in=kotlin.time.ExperimentalTime",
        "-Xopt-in=kotlin.RequiresOptIn"
    )
}

application {
    mainClass.set("MainKt")
}
