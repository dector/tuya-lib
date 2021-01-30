@file:Suppress("EXPERIMENTAL_API_USAGE")

package space.dector.tuyalib

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.Socket
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds


private data class DeviceConfiguration(
    val ip: IpAddress,
    val deviceId: String,
    val localKey: String,
)

public class Bulb private constructor(
    private val config: DeviceConfiguration,
) {

    public constructor(
        ip: IpAddress,
        deviceId: String,
        localKey: String,
    ) : this(
        DeviceConfiguration(
            ip = ip,
            deviceId = deviceId,
            localKey = localKey,
        )
    )

    public fun turnOn() {
        send(
            command = Command.Control,
            commandData = JsonObject()
                // `uid` is not used in CONTROL command
//            .set("uid", deviceId)

                // `t` might be needed to make payload look different (to avoid replay)
//            .set("t", Instant.now().toEpochMilli().toString())

                .set("devId", config.deviceId)
                .set(
                    "dps", JsonObject()
                        .set("20", true)
                ),
        )
    }

    public fun turnOff() {
        send(
            command = Command.Control,
            commandData = JsonObject()
                .set("devId", config.deviceId)
                .set(
                    "dps", JsonObject()
                        .set("20", false)
                ),
        )
    }

    public fun status(): DeviceStatus {
        val responseString = send(
            command = Command.DpRequest,
            commandData = JsonObject()
                .set("gwId", config.deviceId)
                .set("devId", config.deviceId),
            expectResponse = true,
        )

        val response = Json.parse(responseString).asObject()

        return response["dps"].asObject()
            .map { member ->
                Feature.byIdWithValue(
                    id = member.name,
                    value = member.value,
                )
            }.toDeviceStatus()
    }

//    fun state() {
//
//    }

    @OptIn(ExperimentalTime::class)
    private fun send(
        command: Command,
        commandData: JsonObject,
        expectResponse: Boolean = false,
    ): String {
        val packet = Packet(
            command = command,
            payload = Packet.Payload(commandData),
            localKey = config.localKey,
        )

        //println(packet.toHexString(" "))

        val socket = Socket(config.ip, 6668)
        val out = socket.getOutputStream()
        out.write(packet.toHexData())

        var payload = ""
        if (expectResponse) runBlocking {
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

            bytes = bytes.sliceArray(0 until bytesRead)

            print("<< ")
            println(bytes.toHexString())

            println("Decoding:")
            payload = decodeIncomingData(
                content = bytes,
                key = aesKey(config.localKey),
            )
            println("Payload: $payload")
            println()
        }

        socket.close()

        return payload
    }
}

private sealed class Feature {
    data class Unknown(val id: String, val value: String) : Feature()
    data class OnOff(val value: Boolean) : Feature()

    companion object
}

private fun Feature.Companion.byIdWithValue(id: String, value: JsonValue): Feature {
    return when (id) {
        "20" ->
            Feature.OnOff(value.asBoolean())
        else ->
            Feature.Unknown(
                id = id,
                value = value.toString(),
            )
    }
}

private fun List<Feature>.toDeviceStatus(): DeviceStatus = RealDeviceStatus(this)

public interface DeviceStatus {

    public fun isOn(): Boolean
}

private data class RealDeviceStatus(
    val features: List<Feature>,
) : DeviceStatus {

    override fun isOn(): Boolean {
        return features
            .first { it is Feature.OnOff }
            .let { it as Feature.OnOff }
            .value
    }
}
