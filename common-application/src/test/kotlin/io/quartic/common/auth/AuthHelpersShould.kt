package io.quartic.common.auth

import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class AuthHelpersShould {
    @Test
    fun parse_subdomain() {
        assertThat(extractSubdomain("xyz.quartic.io"), equalTo("xyz"))
        assertThat(extractSubdomain("xyz.quartic.io:1334"), equalTo("xyz"))
    }

    @Test
    fun parse_localhost() {
        assertThat(extractSubdomain("localhost"), equalTo("localhost"))
        assertThat(extractSubdomain("localhost:1334"), equalTo("localhost"))
    }
}
