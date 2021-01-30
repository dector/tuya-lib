import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")

    id("com.jfrog.bintray") version "1.8.5"
    `maven-publish`
}

dependencies {
    implementation(KotlinX.coroutines)
    implementation(Deps.minimalJson)

    testImplementation(Deps.kotest.runner)
    testImplementation(Deps.kotest.assertions)
}

group = "space.dector.tuyalib"
version = Publication.versionName

repositories {
    jcenter()
}

kotlin {
    explicitApi()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xopt-in=kotlin.RequiresOptIn"
    )
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}

bintray {
    val secrets = runCatching {
        rootDir.resolve(".secrets/bintray").readLines()
    }.getOrNull()

    if (secrets != null) {
        user = secrets[0]
        key = secrets[1]

        pkg.apply {
            repo = "tuya-lib"
            name = "library"

            setLicenses("Apache-2.0")
            vcsUrl = "https://github.com/dector/tuya-lib"

            version.name = Publication.versionName
            version.vcsTag = Publication.vcsTag
        }

        setPublications("library")
    } else {
        logger.warn("Bintray secrets not found")
    }
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["kotlin"])

            groupId = project.group.toString()
            artifactId = "library"
            version = Publication.versionName
        }
    }

    repositories {
        maven {
            name = "localRepo"
            url = uri("file://${rootDir}/repo")
        }
    }
}
