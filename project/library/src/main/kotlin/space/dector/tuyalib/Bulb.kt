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

/**
 * Local bulb device class-controller.
 */
public class Bulb private constructor(
    private val config: DeviceConfiguration,
) {

    /** Bulb IP */
    public val ip: IpAddress get() = config.ip

    /**
     * Create new bulb to control.
     *
     * @param ip device's local network IP
     * @param deviceId 20-symbol (HEX) device id
     * @param localKey 16-symbol (HEX) device-specific AES key used to decrypt payload
     */
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

    /**
     * Turn on the bulb.
     *
     * Fail silently if device is not reachable.
     */
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

    /**
     * Turn off the bulb.
     *
     * Fail silently if device is not reachable.
     */
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

    /**
     * Fetch current status of the bulb.
     *
     * Fail silently if device is not reachable.
     */
    public fun status(): DeviceStatus {
        val responseString = send(
            command = Command.DpRequest,
            commandData = JsonObject()
                .set("gwId", config.deviceId)
                .set("devId", config.deviceId),
            expectResponse = true,
        )

        return if (responseString is Result.Ok) {
            val response = Json.parse(responseString.value).asObject()

            response["dps"].asObject()
                .map { member ->
                    Feature.byIdWithValue(
                        id = member.name,
                        value = member.value,
                    )
                }
        } else {
            emptyList()
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
    ): Result<String> {
        val packet = Packet(
            command = command,
            payload = Packet.Payload(commandData),
            localKey = config.localKey,
        )

        // log { packet.toHexString(" ") }

        val result = runCatching {
            var payload = ""

            Socket(config.ip, 6668).use { socket ->
                val out = socket.getOutputStream()
                out.write(packet.toHexData())

                if (expectResponse) runBlocking {
                    delay(50.milliseconds)

                    val input = socket.getInputStream()

                    var bytes = ByteArray(1024)

                    var bytesRead = 0
                    var retries = 3
                    while (retries > 0 && bytesRead <= 28) {
                        runCatching {
                            log { "Reading ($retries)" }
                            bytesRead = input.read(bytes)
                        }.onFailure {
                            retries--
                            delay(100.milliseconds)
                        }
                    }

                    bytes = bytes.sliceArray(0 until bytesRead)

                    log { "<< ${bytes.toHexString()}" }

                    log { "Decoding:" }
                    payload = decodeIncomingData(
                        content = bytes,
                        key = aesKey(config.localKey),
                    )
                    log { "Payload: $payload\n" }
                }
            }

            payload
        }

        return result
            .map { Result.Ok(it) }
            .getOrElse { Result.Fail(it) }
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

/**
 * Device state at a certain point of time.
 */
public interface DeviceStatus {

    /**
     * Returns `true` when device is turned on.
     */
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
