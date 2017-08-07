package io.quartic.common.secrets

import com.google.common.base.Preconditions.checkArgument
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// See https://www.securecoding.cert.org/confluence/display/java/MSC61-J.+Do+not+use+insecure+or+weak+cryptographic+algorithms etc.
class SecretsCodec(masterKey: ByteArray) {
    constructor(masterKeyBase64: String) : this(masterKeyBase64.decodeAsBase64())

    private val key = SecretKeySpec(masterKey, ALGORITHM)
    private val sr = SecureRandom()

    init {
        checkArgument(masterKey.size == KEY_LENGTH_BITS / 8, "Key is not exactly $KEY_LENGTH_BITS bits long")
    }

    fun encrypt(secret: String): EncryptedSecret {
        val iv = ByteArray(IV_LENGTH_BITS / 8)
        sr.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            GCMParameterSpec(TAG_LENGTH_BITS, iv)
        )

        val secretBytes = secret.toByteArray()
        val payload = cipher.doFinal(secretBytes)

        return EncryptedSecret(
            iv,
            payload.copyOf(secretBytes.size),
            payload.copyOfRange(secretBytes.size, payload.size)
        )
    }

    @Throws(AEADBadTagException::class)
    fun decrypt(encryptedSecret: EncryptedSecret): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(TAG_LENGTH_BITS, encryptedSecret.iv)
        )

        return cipher.doFinal(encryptedSecret.payload + encryptedSecret.tag).encodeAsString()
    }

    companion object {
        val VERSION = 1
        val ALGORITHM = "AES"
        val TRANSFORMATION = "AES/GCM/NoPadding"
        val KEY_LENGTH_BITS = 128
        val IV_LENGTH_BITS = 96
        val TAG_LENGTH_BITS = 128

        fun generateMasterKeyBase64() = sr.nextBytes(KEY_LENGTH_BITS / 8).encodeAsBase64()

        private val sr = SecureRandom()
    }
}
