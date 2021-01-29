@file:OptIn(ExperimentalTime::class)

package space.dector.tuya

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.security.Key
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


fun main() {
    runDiscovery()
}

@OptIn(ExperimentalTime::class)
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
    }
}

fun createListenerSocket(): DatagramSocket =
    DatagramSocket(6667).apply {
        broadcast = true
        soTimeout = 10_000
    }

data class DetectedDevice(
    val udpIp: IpAddress,
    val rawJsonPayload: JsonObject?,
)

@Suppress("SameParameterValue")
private fun md5(str: String): ByteArray = MessageDigest
    .getInstance("MD5")
    .digest(str.toByteArray())

internal fun aesKey(key: String): Key = aesKey(key.toByteArray())
internal fun aesKey(key: ByteArray): Key = SecretKeySpec(key, "AES")

private val key = aesKey(md5("yGAdlopoPVldABfn"))
internal fun decryptPacket(data: ByteArray): String =
    decryptAes(key, data).decodeToString()

internal fun decryptAes(key: Key, data: ByteArray) = Cipher
    .getInstance("AES/ECB/PKCS5Padding")
    .apply { init(Cipher.DECRYPT_MODE, key) }
    .doFinal(data)
