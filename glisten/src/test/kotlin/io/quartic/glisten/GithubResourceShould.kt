package io.quartic.glisten

import io.quartic.common.test.assertThrows
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertThat
import org.junit.Test
import javax.ws.rs.BadRequestException

class GithubResourceShould {
    private val resource = GithubResource()

    @Test
    fun ignore_non_push_event() {
        resource.handleEvent("noob", "abc", emptyMap())
        // Nothing should happen
    }

    @Test
    fun responds_with_bad_request_exception_if_unparseable_body() {
        val e = assertThrows<BadRequestException> {
            resource.handleEvent("push", "abc", mapOf("foo" to "bar"))
        }

        assertThat(e.message, containsString("abc"))
    }
}
