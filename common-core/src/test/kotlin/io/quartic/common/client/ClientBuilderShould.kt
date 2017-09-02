package io.quartic.common.client

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT
import com.github.tomakehurst.wiremock.junit.WireMockRule
import feign.Param
import feign.RequestLine
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
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

    private val clientBuilder = ClientBuilder(javaClass)

    @Test
    fun encode_slashes_in_path_params() {
        stubFor(get(urlMatching("/ooh/(.*)")).willReturn(aResponse().withBody(""""hello"""")))

        val service = clientBuilder.feign<MyService>("http://localhost:${wireMockRule.port()}")

        service.getStuff("abc/def")

        verify(getRequestedFor(urlEqualTo("/ooh/abc%2Fdef")))
    }

    interface NoobNonRetrofit

    @Test
    fun prevent_retrofitting_non_retrofit_interface() {
        assertThrows<IllegalArgumentException> {
            clientBuilder.retrofit<NoobNonRetrofit>("http://noob.com")
        }
    }

    @Retrofittable
    interface NoobRetrofit

    @Test
    fun allow_retrofitting_non_retrofit_interface() {
        clientBuilder.retrofit<NoobRetrofit>("http://noob.com")
    }
}
