package space.dector.project

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

fun main() {
    runDiscovery()
}

fun runDiscovery() {
    val knownDevices = mutableListOf<String>()

    val t = thread {
        val socket = DatagramSocket(6667).apply {
            broadcast = true
            soTimeout = 15_000
        }

        while (!Thread.interrupted()) {
            val p = DatagramPacket(ByteArray(2048), 2048)
            runCatching { socket.receive(p) }
                .onSuccess {
                    val ip = p.address.hostAddress
                    if (!knownDevices.contains(ip)) {
                        println("IP: $ip")
                        knownDevices.add(ip)

                        val rawData = p.data.slice(20 until (p.length - 8))
                            .toByteArray()

                        val packet = decryptPacket(rawData)

                        println(packet)
                    }
                }
        }
    }

    runCatching {
        t.join(30_000)
        t.interrupt()
    }

    println("Discovered ${knownDevices.size} devices: $knownDevices")
}

// Thanks tuya-convert!
// https://github.com/ct-Open-Source/tuya-convert/blob/94acc3bb020361266ceb74f082e1d25c92a60345/scripts/tuya-discovery.py#L31
private fun decryptPacket(data: ByteArray): String {
    val key = SecretKeySpec(md5("yGAdlopoPVldABfn"), "AES")
    val decoded = Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
        init(Cipher.DECRYPT_MODE, key)
    }.doFinal(data)

    return decoded.decodeToString()
}

private fun md5(str: String): ByteArray {
    return MessageDigest
        .getInstance("MD5")
        .digest(str.toByteArray())
}

@OptIn(ExperimentalUnsignedTypes::class)
fun ByteArray.infoWithHex() = buildString {
    val bytes: ByteArray = this@infoWithHex

    append(
        "BYTES (",
        bytes.size.toString(),
        "): [",
    )

    bytes.forEachIndexed { i, byte ->
        append("0x", byte.toUByte().toString(radix = 16).padStart(2, '0'))

        if (i != bytes.lastIndex) append(" ")
    }

    append("]")
}
