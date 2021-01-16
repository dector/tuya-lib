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
