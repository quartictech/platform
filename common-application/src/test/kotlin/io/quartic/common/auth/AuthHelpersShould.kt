package io.quartic.common.auth

import org.junit.Test
import org.junit.Assert.assertThat
import org.hamcrest.Matchers.equalTo

class AuthHelpersShould {
    @Test
    fun parse_subdomain() {
        assertThat(getIssuer("xyz.quartic.io"), equalTo("xyz"))
        assertThat(getIssuer("xyz.quartic.io:1334"), equalTo("xyz"))
    }

    @Test
    fun parse_localhost() {
        assertThat(getIssuer("localhost"), equalTo("localhost"))
        assertThat(getIssuer("localhost:1334"), equalTo("localhost"))
    }
}
