package space.dector.tuyalib


internal fun log(str: () -> String) {
    if (Configuration.RELEASE) return

    println(str())
}
