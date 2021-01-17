package space.dector.tuya

import java.security.Key
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


// Thanks tuya-convert! All credits for this file goes to `ct-Open-Source/tuya-convert`!
// https://github.com/ct-Open-Source/tuya-convert/blob/94acc3bb020361266ceb74f082e1d25c92a60345/scripts/tuya-discovery.py#L31

private val key = aesKey(md5("yGAdlopoPVldABfn"))

internal fun decryptPacket(data: ByteArray): String =
    decryptAes(key, data).decodeToString()

@Suppress("SameParameterValue")
private fun md5(str: String): ByteArray = MessageDigest
    .getInstance("MD5")
    .digest(str.toByteArray())

fun aesKey(key: String): Key = aesKey(key.toByteArray())
fun aesKey(key: ByteArray): Key = SecretKeySpec(key, "AES")

fun decryptAes(key: Key, data: ByteArray) = Cipher
    .getInstance("AES/ECB/PKCS5Padding")
    .apply { init(Cipher.DECRYPT_MODE, key) }
    .doFinal(data)
