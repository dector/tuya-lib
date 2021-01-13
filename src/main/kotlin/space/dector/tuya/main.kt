@file:OptIn(ExperimentalTime::class)

package space.dector.tuya

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
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
    }
}

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
