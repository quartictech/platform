package io.quartic.common.application

import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.MASTER_KEY
import io.quartic.common.TOKEN_KEY_BASE64
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.auth.User
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.encodeAsBase64
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.time.Duration
import javax.ws.rs.core.HttpHeaders

class ApplicationBaseTokenAuthShould {

    @Test
    fun respond_with_401_if_no_token_supplied() {
        val response = target()
            .request()
            .get()

        assertThat(response.status, equalTo(401))
        assertThat(response.headers[HttpHeaders.WWW_AUTHENTICATE]!!.last() as String, startsWith("Cookie"))
    }

    @Test
    fun respond_with_200_if_valid_token_supplied() {
        val tokenGenerator = TokenGenerator(TOKEN_KEY_BASE64, Duration.ofMinutes(10))

        val tokens = tokenGenerator.generate(User(666, 777), "localhost")

        val response = target()
            .request()
            .cookie(TOKEN_COOKIE, tokens.jwt)
            .header(XSRF_TOKEN_HEADER, tokens.xsrf)
            .get()

        assertThat(response.status, equalTo(200))
        assertThat(response.readEntity(String::class.java), equalTo("Hello 666"))
    }

    private fun target() = JerseyClientBuilder().build().target("http://localhost:${RULE.localPort}/api/test")

    companion object {
        private val CODEC = SecretsCodec(MASTER_KEY)

        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<TestApplication.TestConfiguration>(
            TestApplication::class.java,
            resourceFilePath("test.yml"),
            config("base64EncodedMasterKey", MASTER_KEY.encodeAsBase64()),
            config("auth.type", "token"),
            config("auth.encryptedKey", CODEC.encrypt(TOKEN_KEY_BASE64).toString())
        )
    }
}
