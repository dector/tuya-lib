import com.eclipsesource.json.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import space.dector.tuyalib.Bulb
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.seconds


fun main() {
    val config = readDeviceConfiguration()

    val bulb = Bulb(
        ip = config.ip,
        deviceId = config.deviceId,
        localKey = config.localKey,
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

private fun readDeviceConfiguration(): Config {
    val file = File("device.json")
    if (!file.exists()) {
        File("device.json.template").copyTo(file)

        println(
            "We created 'device.json' file.\n" +
                "Please edit it and provide configuration for your device."
        )
        exitProcess(1)
    }

    val json = Json.parse(file.readText())
        .asObject()

    val config = Config(
        ip = json["ip"].asString(),
        deviceId = json["devId"].asString(),
        localKey = json["localKey"].asString(),
    )

    if (listOf(config.ip, config.deviceId, config.localKey).any { it.startsWith("<") }) {
        System.err.println(
            "ERROR. Incorrect device configuration.\n" +
                "Please set 'device.json' file with your values."
        )
        exitProcess(1)
    }

    return config
}

data class Config(
    val ip: String,
    val deviceId: String,
    val localKey: String,
)
