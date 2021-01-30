rootProject.name = "tuya-lib"

fun includePart(name: String) {
    include(":$name")
    project(":$name").apply {
        projectDir = File("project/$name")
    }
}

includePart("library")

