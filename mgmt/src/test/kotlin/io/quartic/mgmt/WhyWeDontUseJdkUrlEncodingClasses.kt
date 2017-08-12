package io.quartic.mgmt

import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import javax.ws.rs.core.UriBuilder

// See also http://www.proofbyexample.com/url-and-uri-encoding-in-java.html
class WhyWeDontUseJdkUrlEncodingClasses {

    @Test
    fun jdk_uri_builder_messes_up_encoding_of_percents_in_query_params() {
        fun String.encodeAsQueryParam() = UriBuilder.fromUri("http://a.b.c")
            .queryParam("foo", this)
            .build()
            .toString().split("=").last()

        assertThat("%XX".encodeAsQueryParam(), equalTo("%25XX"))        // This one is expected
        assertThat("%20".encodeAsQueryParam(), equalTo("%20"))          // This one is WTF WTF WTF
    }

    @Test
    fun apache_uri_builder_is_regular() {
        fun String.encodeAsQueryParam() = URIBuilder()
            .setScheme("http")
            .setHost("a.b.c")
            .setParameter("foo", this)
            .build()
            .toString().split("=").last()

        assertThat("%XX".encodeAsQueryParam(), equalTo("%25XX"))
        assertThat("%20".encodeAsQueryParam(), equalTo("%2520"))
        assertThat(" ".encodeAsQueryParam(), equalTo("+"))              // This is acceptable, apparently
    }

    @Test
    fun apache_url_encoded_utils_is_regular() {
        fun String.encodeAsQueryParam() = URLEncodedUtils
            .format(listOf(BasicNameValuePair("foo", this)), Charsets.UTF_8)
            .toString().split("=").last()

        assertThat("%XX".encodeAsQueryParam(), equalTo("%25XX"))
        assertThat("%20".encodeAsQueryParam(), equalTo("%2520"))
        assertThat(" ".encodeAsQueryParam(), equalTo("+"))              // This is acceptable, apparently
    }
}
