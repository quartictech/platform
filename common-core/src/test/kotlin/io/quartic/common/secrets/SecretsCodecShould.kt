package io.quartic.common.secrets

import io.quartic.common.secrets.SecretsCodec.EncryptedSecret
import io.quartic.common.test.assertThrows
import org.apache.commons.codec.binary.Hex.encodeHexString
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class SecretsCodecShould {
    private val sr = SecureRandom()

    @Test
    fun parse_stringify_secret_correctly() {
        val original = EncryptedSecret(
            sr.nextBytes(96 / 8),
            sr.nextBytes(32 / 8), // Whatever
            sr.nextBytes(128 / 8)
        )

        val repr = original.toString()

        assertThat(EncryptedSecret.parse(repr), equalTo(original))
    }

    @Test
    fun fail_to_parse_if_parts_are_invalid() {
        // Not enough parts
        assertThrows<IllegalArgumentException> {
            EncryptedSecret.parse("1\$${sr.nextHexString(96 / 4)}\$abcd")
        }

        // Too many parts
        assertThrows<IllegalArgumentException> {
            EncryptedSecret.parse("1\$${sr.nextHexString(96 / 4)}\$abcd\$${sr.nextHexString(128 / 4)}\$abcd")
        }

        // Undecodable
        assertThrows<IllegalArgumentException> {
            EncryptedSecret.parse("1\$${sr.nextHexString(96 / 4)}\$abxy\$${sr.nextHexString(128 / 4)}")
        }

        // IV wrong size
        assertThrows<IllegalArgumentException> {
            EncryptedSecret.parse("1\$${sr.nextHexString(128 / 4)}\$abxy\$${sr.nextHexString(128 / 4)}")
        }

        // Tag wrong size
        assertThrows<IllegalArgumentException> {
            EncryptedSecret.parse("1\$${sr.nextHexString(96 / 4)}\$abxy\$${sr.nextHexString(96 / 4)}")
        }
    }

    @Test
    fun fail_to_parse_if_version_mismatch() {
        assertThrows<IllegalArgumentException> {
            EncryptedSecret.parse("2\$${sr.nextHexString(96 / 4)}\$abxy\$${sr.nextHexString(128 / 4)}")
        }
    }

    @Test
    fun fail_to_create_if_master_key_is_incorrect_length() {
        assertThrows<IllegalArgumentException> {
            SecretsCodec(ByteArray(64 / 8))
        }
    }

    @Test
    fun decrypt_an_encrypted_secret() {
        val key = sr.nextBytes(128 / 8)
        val codec = SecretsCodec(key)

        val original = sr.nextBytes(16)
        val encrypted = codec.encrypt(original)

        assertThat(codec.decrypt(encrypted), equalTo(original))
    }

    @Test
    fun fail_to_decrypt_if_tag_mismatch() {
        val key = sr.nextBytes(128 / 8)
        val codec = SecretsCodec(key)

        val original = sr.nextBytes(16)
        val encrypted = codec.encrypt(original)

        val modified = encrypted.dropLast(32) + sr.nextHexString(32)

        assertThrows<AEADBadTagException> {
            codec.decrypt(modified)
        }
    }

    private fun SecureRandom.nextBytes(num: Int): ByteArray {
        val bytes = ByteArray(num)
        nextBytes(bytes)
        return bytes
    }

    private fun SecureRandom.nextHexString(numChars: Int): String {
        return encodeHexString(nextBytes(numChars / 2))
    }
}
