@file:Suppress("EXPERIMENTAL_API_USAGE")

package space.dector.tuya

import com.eclipsesource.json.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.Socket
import java.util.zip.CRC32
import javax.crypto.Cipher
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds


private const val PACKET_HEAD_SIZE = (0
    + 4 // Prefix
    + 4 // Sequences?
    + 4 // Command
    + 4 // Length
    )
private const val PACKET_TAIL_SIZE = (0
    + 4 // CRC
    + 4 // Suffix
    )
private const val PACKET_FRAME_SIZE = PACKET_HEAD_SIZE + PACKET_TAIL_SIZE
private const val PAYLOAD_EXTENSION_SIZE = 0 +
    3 + // Protocol version
    12  // Just 12 bytes of zeros?


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
            command = "07",
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
            command = "07",
            commandData = JsonObject()
                .set("devId", config.deviceId)
                .set("dps", JsonObject()
                    .set("20", false)),
        )
    }

    fun status() {
        send(
            command = "0a",
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
        command: String,
        commandData: JsonObject,
        readResponse: Boolean = false,
    ) {
        val commandHex = command.padStart(8, '0')

        val packet = buildPacket(
            commandHex = commandHex,
            commandData = commandData.toString().toByteArray(),
        )

        println(packet.asDumpString())

        val socket = Socket(config.ip, 6668)
        val out = socket.getOutputStream()
        out.write(packet)

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
            println(bytes.slice(0 until bytesRead).asDumpString())
            println()
        }

        socket.close()
    }

    private fun buildPacket(
        commandHex: String,
        commandData: ByteArray,
    ): ByteArray {
        val encodedCommandData = run {
            val cipher = Cipher.getInstance("AES")
                .apply {
                    val localKey = aesKey(config.localKey.toByteArray())
                    init(Cipher.ENCRYPT_MODE, localKey)
                }

            cipher.doFinal(commandData)
        }

        val payloadExtensionSize = if (commandHex != "0000000a") PAYLOAD_EXTENSION_SIZE else 0

        val packet = ByteArray(encodedCommandData.size + payloadExtensionSize + PACKET_FRAME_SIZE)

        var pos = 0

        // Header
        packet.write(pos, "00 00 55 aa")
        pos += 4

        // Sequences?
        packet.write(pos, "00 00 00 00")
        pos += 4

        // Command (CONTROL)
        packet.write(pos, commandHex)
        pos += 4

        val payloadSize = encodedCommandData.size + payloadExtensionSize
        // Length
        packet.write(pos, (payloadSize + PACKET_TAIL_SIZE).toString(radix = 16).padStart(8, '0'))
        pos += 4

        if (payloadExtensionSize != 0) {
            // Version
            packet.writeString(pos, "3.3")
            pos += 3

            // Skip 12 bytes
            pos += 12
        }

        // AES encoded command data
        packet.write(pos, encodedCommandData)
        pos += encodedCommandData.size

        // CRC
        val crc = run {
            val data = packet.sliceArray(0 until (packet.size - PACKET_TAIL_SIZE))

            CRC32().apply {
                update(data)
            }.value.toUInt().toString(radix = 16).padStart(8, '0')
        }
        packet.write(pos, crc)
        pos += 4

        // Suffix
        packet.write(pos, "00 00 aa 55")
        pos += 4

        return packet
    }
}
