package io.quartic.common.secrets

import com.google.common.base.Preconditions.checkArgument
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Hex.decodeHex
import org.apache.commons.codec.binary.Hex.encodeHexString
import java.util.Arrays.equals

data class EncryptedSecret(
    val iv: ByteArray,
    val payload: ByteArray,
    val tag: ByteArray
) {
    constructor(other: EncryptedSecret) : this(other.iv, other.payload, other.tag)
    constructor(str: String) : this(parse(str))

    init {
        checkArgument(iv.size == SecretsCodec.IV_LENGTH_BITS / 8, "IV is incorrect length")
        checkArgument(tag.size == SecretsCodec.TAG_LENGTH_BITS / 8, "Tag is incorrect length")
    }

    override fun toString() = "{EncryptedSecret}"   // To prevent accidental logging and stuff

    fun toCanonicalRepresentation() = "${SecretsCodec.VERSION}$${encodeHexString(iv)}$${encodeHexString(payload)}$${encodeHexString(tag)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as EncryptedSecret
        return equals(iv, other.iv) && equals(payload, other.payload) && equals(tag, other.tag)
    }

    companion object {
        private fun parse(str: String): EncryptedSecret {
            val parts = str.split("$")
            checkArgument(parts.size == 4, "Representation doesn't contain exactly 4 parts")
            checkArgument(parts[0] == SecretsCodec.VERSION.toString(), "Version mismatch")

            return EncryptedSecret(
                decode(parts[1], "IV"),
                decode(parts[2], "Payload"),
                decode(parts[3], "Tag")
            )
        }

        private fun decode(part: String, name: String) = try {
            decodeHex(part.toCharArray())
        } catch (e: DecoderException) {
            throw IllegalArgumentException("$name component can't be decoded")
        }
    }
}
