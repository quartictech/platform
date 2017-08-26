package io.quartic.common.secrets

import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom
import java.util.*

fun String.decodeAsBase64(): ByteArray = Base64.getMimeDecoder().decode(this)
fun ByteArray.encodeAsBase64(): String = Base64.getEncoder().encodeToString(this)
fun ByteArray.encodeAsString(): String = String(this)

fun SecureRandom.nextBytes(num: Int): ByteArray {
    val bytes = ByteArray(num)
    nextBytes(bytes)
    return bytes
}

fun SecureRandom.nextHexString(numChars: Int): String {
    return Hex.encodeHexString(nextBytes(numChars / 2))
}
