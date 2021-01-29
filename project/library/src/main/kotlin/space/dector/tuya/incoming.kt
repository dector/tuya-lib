package space.dector.tuya

import java.security.Key


@ExperimentalUnsignedTypes
internal fun decodeIncomingData(
    content: ByteArray,
    key: Key,
): String {
    val minimalContentSize = 0 +
        4 * 4 + // Head
        1 +     // Exit code
        2 * 4   // Tail

    require(content.size >= minimalContentSize) {
        "Incoming data seems corrupted or format is unknown. " +
            "Expected at least '$minimalContentSize' bytes but ${content.size} received."
    }

    val indexOfDataLength = 3 * 4   // Prefix, Sequence, Command
    val payloadLength = content.slice(indexOfDataLength until (indexOfDataLength + 4))
        .joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        .also { println("Parsed length: $it") }
        .toUInt(16).toInt() -
        4 -     // Exit code
        2 * 4   // Tail

    val indexOfPayload = indexOfDataLength +
        2 * 4   // Length, Exit code
    val encodedPayload = content.sliceArray(indexOfPayload until (indexOfPayload + payloadLength))

    val payload = decryptAes(key, encodedPayload)

    println(payload.toHexString(" "))

    return payload.decodeToString()
}
