package io.quartic.common.client

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import io.quartic.common.test.assertThrows
import org.junit.Rule
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.CompletableFuture

@Retrofittable
private interface MyService {
    @GET("ooh/{id}")
    fun getStuffAsync(@Path("id") id: String): CompletableFuture<String>
}

class ClientUtilsShould {
    @JvmField
    @Rule
    var wireMockRule = WireMockRule(DYNAMIC_PORT)

    private val clientBuilder = ClientBuilder(javaClass)

    // Feign got this wrong without decodeSlash=false.  Retaining the test just to prove Retrofit is regular
    @Test
    fun encode_slashes_in_path_params() {
        stubFor(get(urlMatching("/ooh/(.*)")).willReturn(aResponse().withBody(""""hello"""")))

        val service = clientBuilder.retrofit<MyService>("http://localhost:${wireMockRule.port()}")

        service.getStuffAsync("abc/def").get()

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
