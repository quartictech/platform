package io.quartic.zeus.provider

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.zeus.model.ItemId
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import java.net.URL
import javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import javax.ws.rs.core.MediaType.APPLICATION_JSON

class UrlDataProviderShould {
    @JvmField
    @Rule
    var wireMockRule = WireMockRule(DYNAMIC_PORT)

    @Test
    fun provide_data_from_http_url() {
        val data = mapOf(
                ItemId("123") to mapOf("name" to "alex") as Map<String, Any>,
                ItemId("456") to mapOf("name" to "arlo") as Map<String, Any>,
                ItemId("789") to mapOf("name" to "oliver") as Map<String, Any>
        )

        stubFor(WireMock.get(urlEqualTo("/weird.json"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(OBJECT_MAPPER.writeValueAsString(data))
                )
        )

        assertThat(UrlDataProvider(URL("http://localhost:${wireMockRule.port()}/weird.json"), emptyList()).data, equalTo(data))
    }

    // TODO: test filtering
}