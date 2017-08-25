package io.quartic.common

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT
import com.github.tomakehurst.wiremock.junit.WireMockRule
import feign.Param
import feign.RequestLine
import io.quartic.common.client.ClientBuilder
import io.quartic.common.client.Retrofittable
import io.quartic.common.client.client
import io.quartic.common.test.assertThrows
import org.junit.Rule
import org.junit.Test
import javax.ws.rs.Path

@Path("/ooh")
private interface MyService {
    @RequestLine("GET /ooh/{id}", decodeSlash = false)
    fun getStuff(@Param("id") id: String): String
}

class ClientUtilsShould {
    @JvmField
    @Rule
    var wireMockRule = WireMockRule(DYNAMIC_PORT)

    @Test
    fun encode_slashes_in_path_params() {
        stubFor(get(urlMatching("/ooh/(.*)")).willReturn(aResponse().withBody(""""hello"""")))

        val service = client<MyService>(javaClass, "http://localhost:${wireMockRule.port()}")

        service.getStuff("abc/def")

        verify(getRequestedFor(urlEqualTo("/ooh/abc%2Fdef")))
    }

    interface NoobNonRetrofit

    @Test
    fun prevent_retrofitting_non_retrofit_interface() {
        assertThrows<IllegalArgumentException> {
            ClientBuilder(javaClass).retrofit<NoobNonRetrofit>("http://noob.com")
        }
    }

    @Retrofittable
    interface NoobRetrofit

    @Test
    fun allow_retrofitting_non_retrofit_interface() {
        ClientBuilder(javaClass).retrofit<NoobRetrofit>("http://noob.com")
    }
}
