package io.quartic.common.secrets

import io.quartic.common.test.assertThrows
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class SecretsCodecShould {
    private val sr = SecureRandom()

    @Test
    fun fail_to_create_if_master_key_is_incorrect_length() {
        assertThrows<IllegalArgumentException> {
            SecretsCodec(ByteArray(64 / 8).toUnsafeSecret())
        }
    }

    @Test
    fun decrypt_an_encrypted_secret() {
        val key = sr.nextBytes(256 / 8).toUnsafeSecret()
        val codec = SecretsCodec(key)

        val encrypted = codec.encrypt(CONTROVERSIAL_SECRET)

        assertThat(codec.decrypt(encrypted), equalTo(CONTROVERSIAL_SECRET))
    }

    @Test
    fun fail_to_decrypt_if_tag_mismatch() {
        val key = sr.nextBytes(256 / 8).toUnsafeSecret()
        val codec = SecretsCodec(key)

        val encrypted = codec.encrypt(CONTROVERSIAL_SECRET)
        val modified = encrypted.copy(tag = sr.nextBytes(128 / 8))

        assertThrows<AEADBadTagException> {
            codec.decrypt(modified)
        }
    }

    private fun ByteArray.toUnsafeSecret() = UnsafeSecret(this.encodeAsBase64())

    companion object {
        private val CONTROVERSIAL_SECRET = UnsafeSecret("Arlo likes La Dispute")
    }
}
