object Deps {
    val minimalJson = "com.eclipsesource.minimal-json:minimal-json:0.9.5"

    object kotest {
        const val runner = "io.kotest:kotest-runner-junit5:${Versions.kotest}"
        const val assertions = "io.kotest:kotest-assertions-core:${Versions.kotest}"
    }
}

object KotlinX {
    private const val version = "1.4.2"

    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
}

object Versions {
    const val kotlin = "1.4.30"

    const val kotest = "4.2.3"
}
