package space.dector.tuya

import com.eclipsesource.json.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


@OptIn(ExperimentalTime::class)
fun main() {
    val device = Json.parse(File("data/devices/1.json").readText())
        .asObject()

    val bulb = Bulb(
        ip = device["ip"].asString(),
        deviceId = device["devId"].asString(),
        localKey = device["localKey"].asString(),
    )

    runBlocking {
        println("Turning ON")
        bulb.turnOn()

        delay(1.seconds)

        println("Turning OFF")
        bulb.turnOff()
    }
}
