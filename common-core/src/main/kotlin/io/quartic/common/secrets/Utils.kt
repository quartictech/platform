package io.quartic.common.secrets

import java.util.*

fun String.decodeAsBase64(): ByteArray = Base64.getDecoder().decode(this)
fun ByteArray.encodeAsBase64(): String = Base64.getEncoder().encodeToString(this)
fun ByteArray.encodeAsString(): String = String(this)
