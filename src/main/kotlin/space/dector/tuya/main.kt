@file:OptIn(ExperimentalTime::class)

package space.dector.tuya

import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


fun main() {
    runDiscovery()
}

fun runDiscovery(
    timeout: Duration = 30.seconds,
) = runBlocking {
    val knownDevices = mutableListOf<String>()

    val socket = createListenerSocket()

    withTimeout(timeout) {
        while (isActive) {
            val p = DatagramPacket(ByteArray(2048), 2048)
            runCatching {
                socket.receive(p)
            }.onSuccess {
                val ip = p.address.hostAddress
                if (!knownDevices.contains(ip)) {
                    knownDevices.add(ip)

                    println("IP: $ip")

                    val rawData = p.data.sliceArray(20 until (p.length - 8))
                    val data = decryptPacket(rawData)

                    println("DATA: $data")
                }
            }
        }
    }

    socket.close()

    println("Discovered ${knownDevices.size} devices: $knownDevices")
}

fun createListenerSocket(): DatagramSocket =
    DatagramSocket(6667).apply {
        broadcast = true
        soTimeout = 10_000
    }

