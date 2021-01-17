@file:Suppress("EXPERIMENTAL_API_USAGE")

package space.dector.tuya

import com.eclipsesource.json.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.Socket
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds


data class DeviceConfiguration(
    val ip: IpAddress,
    val deviceId: String,
    val localKey: String,
)

class Bulb(
    val config: DeviceConfiguration,
) {

    constructor(
        ip: IpAddress,
        deviceId: String,
        localKey: String,
    ) : this(DeviceConfiguration(
        ip = ip,
        deviceId = deviceId,
        localKey = localKey,
    ))

    fun turnOn() {
        send(
            command = Command.Control,
            commandData = JsonObject()
                // `uid` is not used in CONTROL command
//            .set("uid", deviceId)

                // `t` might be needed to make payload look different (to avoid replay)
//            .set("t", Instant.now().toEpochMilli().toString())

                .set("devId", config.deviceId)
                .set("dps", JsonObject()
                    .set("20", true)),
        )
    }

    fun turnOff() {
        send(
            command = Command.Control,
            commandData = JsonObject()
                .set("devId", config.deviceId)
                .set("dps", JsonObject()
                    .set("20", false)),
        )
    }

    fun status() {
        send(
            command = Command.DpRequest,
            commandData = JsonObject()
                .set("gwId", config.deviceId)
                .set("devId", config.deviceId),
            readResponse = true,
        )
    }

//    fun state() {
//
//    }

    @OptIn(ExperimentalTime::class)
    private fun send(
        command: Command,
        commandData: JsonObject,
        readResponse: Boolean = false,
    ) {
        val packet = Packet(
            command = command,
            payload = Packet.Payload(commandData),
            localKey = config.localKey,
        )

        // println(packet.toHexString(" "))

        val socket = Socket(config.ip, 6668)
        val out = socket.getOutputStream()
        out.write(packet.toHexData())

        if (readResponse) runBlocking {
            delay(50.milliseconds)

            val input = socket.getInputStream()

            var bytes = ByteArray(1024)

            var bytesRead = 0
            var retries = 3
            while (retries > 0 && bytesRead <= 28) {
                runCatching {
                    println("Reading ($retries)")
                    bytesRead = input.read(bytes)
                }.onFailure {
                    retries--
                    delay(100.milliseconds)
                }
            }

            print("<< ")
            println(bytes.slice(0 until bytesRead).toByteArray().toHexString())
            println()
        }

        socket.close()
    }
}
