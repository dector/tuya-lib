@file:Suppress("EXPERIMENTAL_API_USAGE")

package space.dector.tuya

import com.eclipsesource.json.JsonObject
import java.net.Socket
import java.util.zip.CRC32
import javax.crypto.Cipher


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


class Bulb(
    val ip: IpAddress,
    val deviceId: String,
    val localKey: String,
) {

    fun turnOn() {
        sendControlCommand(JsonObject()
            .set("20", true))
    }

    fun turnOff() {
        sendControlCommand(JsonObject()
            .set("20", false))
    }

    private fun sendControlCommand(dps: JsonObject) {
        val packet = buildPacket(dps)

//        println(packet.asDumpString())

        val socket = Socket(ip, 6668)
        val out = socket.getOutputStream()
        out.write(packet)

/*
        val waitForResponse = false
        if (waitForResponse) run {
            val bytes = ByteArray(1024)
            val input = socket.getInputStream()

            var retries = 3
            var bytesRead = 0

            while (retries > 0 || bytes.size <= 28) {
                runCatching {
                    Thread.sleep(100)
                    bytesRead = input.read(bytes)
                }
                    .onFailure {
                        retries--
                    }
            }

            println("<<")
            println(bytes.take(bytesRead).asDumpString())
            println()
        }
*/

        socket.close()
    }

    private fun buildPacket(
        dps: JsonObject,
    ): ByteArray {
        val encodedCommandData = run {
            val cipher = Cipher.getInstance("AES")
                .apply {
                    val localKey = aesKey(localKey.toByteArray())
                    init(Cipher.ENCRYPT_MODE, localKey)
                }

            val commandData = JsonObject()
                .set("devId", deviceId)
                .set("dps", dps)

            // `uid` is not used in CONTROL command
//            .set("uid", deviceId)

            // `t` might be needed to make payload look different (to avoid replay)
//            .set("t", Instant.now().toEpochMilli().toString())

            cipher.doFinal(commandData.toString().toByteArray())
        }

        val packet = ByteArray(encodedCommandData.size + PAYLOAD_EXTENSION_SIZE + PACKET_FRAME_SIZE)

        // Header
        packet.write(0, "00 00 55 aa")
        // Sequences?
        packet.write(4, "00 00 00 00")
        // Command (CONTROL)
        packet.write(8, "00 00 00 07")

        val payloadSize = encodedCommandData.size + PAYLOAD_EXTENSION_SIZE
        // Length
        packet.write(12, (payloadSize + PACKET_TAIL_SIZE).toString(radix = 16).padStart(8, '0'))

        // Version
        packet.writeString(16, "3.3")

        // Skip 12 bytes

        // AES encoded command data
        packet.write(31, encodedCommandData)

        // CRC
        val crc = run {
            val data = packet.sliceArray(0 until (packet.size - PACKET_TAIL_SIZE))

            CRC32().apply {
                update(data)
            }.value.toUInt().toString(radix = 16).padStart(8, '0')
        }
        packet.write(31 + encodedCommandData.size, crc)

        // Suffix
        packet.write(packet.size - 4, "00 00 aa 55")

        return packet
    }
}
