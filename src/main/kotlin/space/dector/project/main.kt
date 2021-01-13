package space.dector.project

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

fun main() {
    println("It works!")
    scan()
}

fun scan() {
    val socket = DatagramSocket(6667)
    socket.broadcast = true
    socket.soTimeout = 15_000

    println("Waiting...")

    repeat(1) {
        val p = DatagramPacket(ByteArray(2048), 2048)
        socket.receive(p)

        val ip = p.address.hostAddress
        println("IP: $ip")

        val rawData = p.data.slice(20 until (p.length - 8))
            .toByteArray()

        val packet = decryptPacket(rawData)

        println(packet)
    }
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
