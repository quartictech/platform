package io.quartic.common.secrets

import io.quartic.common.test.assertThrows
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import java.security.SecureRandom

class EncryptedSecretShould {
    private val sr = SecureRandom()

    @Test
    fun parse_stringify_secret_correctly() {
        val original = EncryptedSecret(
            sr.nextBytes(96 / 8),
            sr.nextBytes(32 / 8), // Whatever
            sr.nextBytes(128 / 8)
        )

        val repr = original.toCanonicalRepresentation()

        Assert.assertThat(EncryptedSecret(repr), Matchers.equalTo(original))
    }

    @Test
    fun fail_to_parse_if_parts_are_invalid() {
        // Not enough parts
        assertThrows<IllegalArgumentException> {
            EncryptedSecret("1\$${sr.nextHexString(96 / 4)}\$abcd")
        }

        // Too many parts
        assertThrows<IllegalArgumentException> {
            EncryptedSecret("1\$${sr.nextHexString(96 / 4)}\$abcd\$${sr.nextHexString(128 / 4)}\$abcd")
        }

        // Undecodable
        assertThrows<IllegalArgumentException> {
            EncryptedSecret("1\$${sr.nextHexString(96 / 4)}\$abxy\$${sr.nextHexString(128 / 4)}")
        }

        // IV wrong size
        assertThrows<IllegalArgumentException> {
            EncryptedSecret("1\$${sr.nextHexString(128 / 4)}\$abxy\$${sr.nextHexString(128 / 4)}")
        }

        // Tag wrong size
        assertThrows<IllegalArgumentException> {
            EncryptedSecret("1\$${sr.nextHexString(96 / 4)}\$abxy\$${sr.nextHexString(96 / 4)}")
        }
    }

    @Test
    fun fail_to_parse_if_version_mismatch() {
        assertThrows<IllegalArgumentException> {
            EncryptedSecret("2\$${sr.nextHexString(96 / 4)}\$abxy\$${sr.nextHexString(128 / 4)}")
        }
    }
}
