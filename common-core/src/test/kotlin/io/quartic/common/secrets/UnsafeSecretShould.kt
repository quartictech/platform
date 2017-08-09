package io.quartic.common.secrets

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Assert.assertThat
import org.junit.Test

class UnsafeSecretShould {
    @Test
    fun not_expose_secret_via_to_string() {
        val original = UnsafeSecret("hello")

        assertThat(original.toString(), not(containsString("hello")))
    }
}
