@file:OptIn(ExperimentalTime::class)

package space.dector.tuya

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Socket
import java.time.Instant
import java.util.zip.CRC32
import javax.crypto.Cipher
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


fun main() {
    runDiscovery()
}

fun runDiscovery(
    timeout: Duration = 30.seconds,
) = runBlocking {
    val devices = mutableMapOf<IpAddress, DetectedDevice>()

    val socket = createListenerSocket()

    println("Timeout: $timeout")
    println("Scanning...")
    println()

    withTimeout(timeout) {
        while (isActive) {
            val p = DatagramPacket(ByteArray(2048), 2048)
            runCatching {
                socket.receive(p)
            }.onSuccess {
                val ip = p.address.hostAddress
                if (ip !in devices) {
                    devices[ip] = DetectedDevice(
                        udpIp = ip,
                        rawJsonPayload = null,
                    )
                    println(ip)

                    val rawData = p.data.sliceArray(20 until (p.length - 8))
                    val data = decryptPacket(rawData)

                    devices[ip] = devices[ip]!!.copy(
                        rawJsonPayload = Json.parse(data).asObject(),
                    )
                }
            }
        }
    }

    socket.close()

    println()
    println("Discovered ${devices.size} devices:")
    devices.values.forEach { device ->
        print(device.udpIp)
        print(": ")
        print(device.rawJsonPayload)
        println()
        println()

        sendControl(device, JsonObject()
            .set("20", true))
        Thread.sleep(1000)
        sendControl(device, JsonObject()
            .set("20", false))
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun sendControl(device: DetectedDevice, dps: JsonObject) {
    // CONTROL
    val commandPayload = JsonObject()
        .set("devId", device.rawJsonPayload!!["gwId"])
        .set("uid", device.rawJsonPayload["gwId"])
        .set("t", Instant.now().toEpochMilli().toString())
        .set("dps", dps)

    // TODO detect local key
    val localKey = aesKey(File("localKey").readText().trim().toByteArray())
    val cipher = Cipher
        .getInstance("AES")
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

    val socket = Socket(device.udpIp, 6668)
    val out = socket.getOutputStream()
    out.write(dataToSend.toByteArray())

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

    socket.close()
}

/*

>>> d.turn_on()
DEBUG:tinytuya:json_payload=b'{"devId":"00030842d4a6511ad904","uid":"00030842d4a6511ad904","t":"1610569615","dps":{"20":true}}'
DEBUG:tinytuya:set_status received data=b'\x00\x00U\xaa\x00\x00\x00\x00\x00\x00\x00\x07\x00\x00\x00\x0c\x00\x00\x00\x00x\x93p\x91\x00\x00\xaaU'

>>> d.turn_off()
DEBUG:tinytuya:json_payload=b'{"devId":"00030842d4a6511ad904","uid":"00030842d4a6511ad904","t":"1610569623","dps":{"20":false}}'
DEBUG:tinytuya:set_status received data=b'\x00\x00U\xaa\x00\x00\x00\x00\x00\x00\x00\x07\x00\x00\x00\x0c\x00\x00\x00\x00x\x93p\x91\x00\x00\xaaU'

>>> d.turn_on()
DEBUG:tinytuya:json_payload=b'{"devId":"00030842d4a6511ad904","uid":"00030842d4a6511ad904","t":"1610569624","dps":{"20":true}}'
DEBUG:tinytuya:set_status received data=b'\x00\x00U\xaa\x00\x00\x00\x00\x00\x00\x00\x07\x00\x00\x00\x0c\x00\x00\x00\x00x\x93p\x91\x00\x00\xaaU'

 */

fun createListenerSocket(): DatagramSocket =
    DatagramSocket(6667).apply {
        broadcast = true
        soTimeout = 10_000
    }

typealias IpAddress = String

data class DetectedDevice(
    val udpIp: IpAddress,
    val rawJsonPayload: JsonObject?,
)
