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

    repeat(15) {
        val p = DatagramPacket(ByteArray(2048), 2048)
        socket.receive(p)

        println(p.data.infoWithHex())
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun ByteArray.infoWithHex() = buildString {
    val bytes: ByteArray = this@infoWithHex

    append(
        "BYTES (",
        bytes.dropLastWhile { it == 0.toByte() }.size,
        " / ",
        bytes.size.toString(),
        "): [",
    )

    bytes.forEachIndexed { i, byte ->
        append("0x", byte.toUByte().toString(radix = 16).padStart(2, '0'))

        if (i != bytes.lastIndex) append(" ")
    }

    append("]")
}
