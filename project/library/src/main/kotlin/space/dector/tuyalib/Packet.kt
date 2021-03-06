package space.dector.tuyalib

import com.eclipsesource.json.JsonObject
import java.util.zip.CRC32
import javax.crypto.Cipher


private const val PACKET_HEAD_SIZE = (0
        + 4 // Prefix
        + 4 // Sequences?
        + 4 // Command
        + 4 // Length
        )
private const val PACKET_TAIL_SIZE = (0
        + 4 // CRC
        + 4 // Suffix
        )

@ExperimentalUnsignedTypes
internal data class Packet(
    val command: Command,
    val payload: Payload,
    val localKey: String,
    val version: Version = Version.V3_3,
) {

    fun toHexData(): ByteArray {
        val hexPayload = prepareHexPayload(
            localKey = localKey,
            payload = payload.json.toString(),
            extension = Extension.of(
                command = command,
                version = version,
            ),
        )

        return createFramedPayload(
            command = command,
            payload = hexPayload,
        )
    }

    data class Payload(
        val json: JsonObject,
    )
}

@ExperimentalUnsignedTypes
private fun createFramedPayload(
    command: Command,
    payload: ByteArray,
): ByteArray {
    val buf = ByteArray(
        PACKET_HEAD_SIZE +
                payload.size +
                PACKET_TAIL_SIZE
    )
    var pos = 0

    // Header
    buf.write(pos, "00 00 55 aa")
    pos += 4

    // Sequences?
    buf.write(pos, "00 00 00 00")
    pos += 4

    // Command (CONTROL)
    buf.write(pos, command.hex4Bytes())
    pos += 4

    // Length
    buf.write(pos, (payload.size + PACKET_TAIL_SIZE).toHexString(bytes = 4))
    pos += 4

    // Payload
    buf.write(pos, payload)
    pos += payload.size

    // CRC
    val crc = crc32(buf, 0 until (buf.size - PACKET_TAIL_SIZE))
        .toHexString(bytes = 4)
    log { crc }
    buf.write(pos, crc)
    pos += 4

    // Suffix
    buf.write(pos, "00 00 aa 55")
    pos += 4

    return buf
}

@ExperimentalUnsignedTypes
private fun crc32(buffer: ByteArray, indicies: IntRange): UInt {
    val data = buffer.sliceArray(indicies)

    return CRC32()
        .apply { update(data) }
        .value
        .toUInt()
}

private fun aesEncode(
    key: String,
    data: String,
): ByteArray = aesEncode(
    key = key.toByteArray(),
    data = data.toByteArray(),
)

private fun aesEncode(
    key: ByteArray,
    data: ByteArray,
): ByteArray {
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, aesKey(key))

    return cipher.doFinal(data)
}

@ExperimentalUnsignedTypes
private fun prepareHexPayload(
    localKey: String,
    payload: String,
    extension: Extension = Extension.None,
): ByteArray {
    val encodedPayload = aesEncode(
        key = localKey,
        data = payload,
    )

    val newPayload = when (extension) {
        Extension.None ->
            encodedPayload
        is Extension.WithVersion -> {
            val extendedPayload = ByteArray(
                encodedPayload.size +
                        3 + // Protocol version
                        12  // Just 12 bytes of zeros?
            )

            var pos = 0

            // Version
            val version = extension.version.id.toByteArray()
            check(version.size == 3)

            extendedPayload.write(pos, version)
            pos += 3

            // Skip 12 bytes
            pos += 12

            // Payload
            extendedPayload.write(pos, encodedPayload)

            extendedPayload
        }
    }

    return newPayload
}

internal enum class Version(val id: String) {
    V3_3("3.3"),
}

internal enum class Command(val id: String) {
    Control("07"),
    DpRequest("0a"),
}

internal sealed class Extension {
    object None : Extension()
    data class WithVersion(val version: Version) : Extension()

    companion object
}

internal fun Extension.Companion.of(
    command: Command,
    version: Version,
): Extension {
    return when (version) {
        Version.V3_3 -> when (command) {
            Command.DpRequest -> Extension.None
            else -> Extension.WithVersion(version)
        }
    }
}

internal fun Command.hex4Bytes(): String = id.padStart(8, '0')

@ExperimentalUnsignedTypes
internal fun UInt.toHexString(bytes: Int): String = toInt()
    .toHexString(bytes = bytes)

@ExperimentalUnsignedTypes
internal fun Int.toHexString(bytes: Int): String {
    val hexString = toUInt().toString(radix = 16)
    val expectedLength = bytes * 2

    require(hexString.length <= expectedLength) {
        "Value '$this' will not fit into $bytes-byte HEX string"
    }

    return hexString.padStart(expectedLength, '0')
}
