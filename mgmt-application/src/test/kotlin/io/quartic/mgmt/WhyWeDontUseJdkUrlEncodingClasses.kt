package io.quartic.mgmt

import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URLEncoder
import javax.ws.rs.core.UriBuilder

class WhyWeDontUseJdkUrlEncodingClasses {

    @Test
    fun uri_builder_should_encode_percent_in_query_param_normally() {
        fun String.encodeAsQueryParam() = UriBuilder.fromUri("http://a.b.c")
            .queryParam("foo", this)
            .build()
            .toString().split("=").last()

        assertThat("%XX".encodeAsQueryParam(), equalTo("%25XX"))        // This one is expected
        assertThat("%20".encodeAsQueryParam(), equalTo("%20"))          // This one is WTF WTF WTF
    }

    @Test
    fun url_encoder_should_encode_space_as_percent_twenty() {
        assertThat(URLEncoder.encode(" "), equalTo("+"))                // Should be %20
    }
}
