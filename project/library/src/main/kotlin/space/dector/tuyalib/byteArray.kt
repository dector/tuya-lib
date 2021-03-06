package space.dector.tuyalib


@ExperimentalUnsignedTypes
internal fun Packet.toHexString(
    separator: String = "",
) = toHexData().toHexString(separator)

@ExperimentalUnsignedTypes
internal fun ByteArray.toHexString(
    separator: String = "",
) = joinToString(separator) {
    it.toUByte()
        .toString(16)
        .padStart(2, '0')
}

@ExperimentalUnsignedTypes
internal fun ByteArray.write(position: Int, data: String) {
    val filteredString = data.mapNotNull {
        it.toLowerCase().takeIf { c -> c in HEX_ALPHABET }
    }

    require(filteredString.size % 2 == 0) { "Expected even amount of bytes, got '${filteredString.size}'" }

    val bytes = filteredString.chunked(2).map {
        it.joinToString("")
            .toUByte(radix = 16)
            .toByte()
    }.toByteArray()

    write(position, bytes)
}

internal fun ByteArray.write(position: Int, data: ByteArray) {
    require(position + data.size <= this.size) {
        "Position ($position) + Data (${data.size}) should be less than buffer size (${this.size})"
    }

    data.forEachIndexed { i, byte ->
        this[position + i] = byte
    }
}

internal fun ByteArray.writeString(position: Int, str: String) {
    val bytes = str.encodeToByteArray()

    write(position, bytes)
}

private const val HEX_ALPHABET = "1234567890abcdef"
