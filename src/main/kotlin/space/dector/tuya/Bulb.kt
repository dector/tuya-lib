package space.dector.tuya

import com.eclipsesource.json.JsonObject
import java.net.Socket
import java.time.Instant
import java.util.zip.CRC32
import javax.crypto.Cipher

class Bulb(
    val ip: IpAddress,
    val deviceId: String,
    val localKey: String,
) {

    fun turnOn() {
        sendControl(JsonObject()
            .set("20", true))
    }

    fun turnOff() {
        sendControl(JsonObject()
            .set("20", false))
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sendControl(dps: JsonObject) {
        // CONTROL
        val commandPayload = JsonObject()
            .set("devId", deviceId)
            .set("uid", deviceId)
            .set("t", Instant.now().toEpochMilli().toString())
            .set("dps", dps)

        val localKey = aesKey(localKey.toByteArray())
        val cipher = Cipher.getInstance("AES")
            .apply { init(Cipher.ENCRYPT_MODE, localKey) }

        val payload: List<Byte> = cipher.doFinal(commandPayload.toString().toByteArray())
            .toMutableList()
            .also { it.addAll(0, (1..12).map { 0.toByte() }) }
            .also { it.addAll(0, "3.3".toByteArray().toList()) }
            .also {
                it.addAll("00 00 00 00 00 00 aa 55"
                    .split(" ")
                    .map { it.toUByte(16).toByte() })
            }

        check(payload.size <= 0xff)

        val buff = buildList<Byte> {
            // Prefix
            "00 00 55 aa 00 00 00 00 00 00 00"
                .split(" ")
                .map { it.toUByte(16).toByte() }
                .let(this::addAll)

            // Command
            add("07".toByte(16))
            "00 00 00"
                .split(" ")
                .map { it.toByte(16) }
                .let(this::addAll)
            add(payload.size.toByte())

            // Payload
            addAll(payload)
        }

        val dataToSend = buildList<Byte> {
            addAll(buff.dropLast(8))

            val crc = CRC32().apply {
                update(buff.dropLast(8).toByteArray())
            }.value.toString(16)
                .chunked(2)
                .takeLast(4)
                .map { it.toUByte(16).toByte() }
            addAll(crc)

            addAll(buff.takeLast(4))
        }

        println(">>>")
        println(dataToSend.joinToString(" ") { it.toUByte().toString(16).padStart(2, '0') })

        val socket = Socket(ip, 6668)
        val out = socket.getOutputStream()
        out.write(dataToSend.toByteArray())

        val waitForResponse = false
        if (waitForResponse) run {
            println("<<")
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

            println(bytes.take(bytesRead).joinToString(" ") { it.toUByte().toString(16).padStart(2, '0') })
            println()
        }

        socket.close()
    }
}
