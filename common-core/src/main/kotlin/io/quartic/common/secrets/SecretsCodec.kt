package io.quartic.common.secrets

import com.google.common.base.Preconditions.checkArgument
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Hex.decodeHex
import org.apache.commons.codec.binary.Hex.encodeHexString
import java.security.SecureRandom
import java.util.Arrays.equals
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// See https://www.securecoding.cert.org/confluence/display/java/MSC61-J.+Do+not+use+insecure+or+weak+cryptographic+algorithms etc.
class SecretsCodec(masterKey: ByteArray) {
    constructor(base64EncodedMasterKey: String) : this(base64EncodedMasterKey.decodeAsBase64())

    private val key = SecretKeySpec(masterKey, ALGORITHM)
    private val sr = SecureRandom()

    init {
        checkArgument(masterKey.size == KEY_LENGTH_BITS / 8, "Key is not exactly $KEY_LENGTH_BITS bits long")
    }

    fun encrypt(base64EncodedSecret: String) = encrypt(base64EncodedSecret.decodeAsBase64())

    fun encrypt(secret: ByteArray): EncryptedSecret {
        val iv = ByteArray(IV_LENGTH_BITS / 8)
        sr.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            GCMParameterSpec(TAG_LENGTH_BITS, iv)
        )

        val payload = cipher.doFinal(secret)

        return EncryptedSecret(
            iv,
            payload.copyOf(secret.size),
            payload.copyOfRange(secret.size, payload.size)
        )
    }

    @Throws(AEADBadTagException::class)
    fun decrypt(encryptedSecret: EncryptedSecret): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(TAG_LENGTH_BITS, encryptedSecret.iv)
        )

        return cipher.doFinal(encryptedSecret.payload + encryptedSecret.tag)
    }

    data class EncryptedSecret(
        val iv: ByteArray,
        val payload: ByteArray,
        val tag: ByteArray
    ) {
        constructor(other: EncryptedSecret) : this(other.iv, other.payload, other.tag)
        constructor(str: String) : this(parse(str))

        init {
            checkArgument(iv.size == IV_LENGTH_BITS / 8, "IV is incorrect length")
            checkArgument(tag.size == TAG_LENGTH_BITS / 8, "Tag is incorrect length")
        }

        override fun toString() = "${VERSION}$${encodeHexString(iv)}$${encodeHexString(payload)}$${encodeHexString(tag)}"

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
                checkArgument(parts[0] == VERSION.toString(), "Version mismatch")

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

    companion object {
        val VERSION = 1
        val ALGORITHM = "AES"
        val TRANSFORMATION = "AES/GCM/NoPadding"
        val KEY_LENGTH_BITS = 128
        val IV_LENGTH_BITS = 96
        val TAG_LENGTH_BITS = 128

        fun generateMasterKey(): ByteArray {
            val bytes = ByteArray(KEY_LENGTH_BITS / 8)
            sr.nextBytes(bytes)
            return bytes
        }

        private val sr = SecureRandom()
    }
}
