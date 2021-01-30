package space.dector.tuyalib

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
        val wasOn = run {
            println("Checking pre-test status")
            bulb.status().isOn()
        }

        delay(1.seconds)

        run {
            println("Turning ON")
            bulb.turnOn()

            delay(0.5.seconds)

            println("Is ON: ${bulb.status().isOn()}")
        }

        delay(1.seconds)

        run {
            println("Turning OFF")
            bulb.turnOff()

            delay(0.5.seconds)

            println("Is ON: ${bulb.status().isOn()}")
        }

        delay(1.seconds)

        if (wasOn) {
            bulb.turnOn()
        } else {
            bulb.turnOff()
        }
    }
}
