import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")

    `maven-publish`
    signing
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

val sourcesJar = tasks.create("sourcesJar", Jar::class.java) {
    archiveClassifier.set("sources")

    from(sourceSets["main"].withConvention(KotlinSourceSet::class) { kotlin.srcDirs })
}

artifacts {
    archives(sourcesJar)
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["kotlin"])

            groupId = project.group.toString()
            artifactId = "library"
            version = Publication.versionName

            artifact(sourcesJar)

            pom {
                name.set("library")
                description.set("Library to control Tuya devices")
                url.set("https://github.com/dector/tuya-lib")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("dector")
                        name.set("dector")
                        email.set("github@dector.space")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/dector/tuya-lib")
                    developerConnection.set("scm:git:ssh://github.com/dector/tuya-lib.git")
                    url.set("https://github.com/dector/tuya-lib")

                    tag.set(Publication.vcsTag)
                }

                // Include transitive dependencies
                /*withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")

                    project.configurations.implementation.get().allDependencies.forEach {
                        val node = dependenciesNode.appendNode("dependency")
                        node.appendNode("groupId", it.group)
                        node.appendNode("artifactId", it.name)
                        node.appendNode("version", it.version)
                    }
                }*/
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

            val secrets = runCatching {
                rootDir
                    .resolve(".secrets/sonatype")
                    .readLines()
                    .filter(String::isNotBlank)
            }.getOrNull()

            if (secrets != null) {
                credentials {
                    username = secrets[0].trim()
                    password = secrets[1].trim()
                }
            } else {
                logger.warn("Sonatype secrets not found")
            }
        }
        maven {
            name = "localRepo"
            url = uri("file://${rootDir}/repo")
        }
    }
}

// Load signing
run {
    val secrets = runCatching {
        rootDir
            .resolve(".secrets/signing")
            .readLines()
            .filter(String::isNotBlank)
    }.getOrNull()

    if (secrets != null) {
        ext["signing.keyId"] = secrets[0].trim()
        ext["signing.password"] = secrets[1].trim()
        ext["signing.secretKeyRingFile"] = File(".").resolve(secrets[2].trim())
    } else {
        logger.warn("Signing secrets not found")
    }
}

signing {
    setRequired({
        gradle.taskGraph.allTasks.any { it.name.startsWith("publish") }
    })

    sign(publishing.publications)
}
