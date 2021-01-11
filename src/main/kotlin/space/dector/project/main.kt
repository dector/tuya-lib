package space.dector.project

import java.net.DatagramPacket
import java.net.DatagramSocket

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

        val rawData = p.data.trimEnd().drop(20).dropLast(8).toByteArray()

        println(rawData.infoWithHex())
    }
}

fun ByteArray.trimEnd() = dropLastWhile { it == 0.toByte() }.toByteArray()

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
