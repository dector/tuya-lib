package space.dector.tuya


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
