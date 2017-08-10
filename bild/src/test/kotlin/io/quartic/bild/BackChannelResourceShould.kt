package io.quartic.bild

import com.nhaarman.mockito_kotlin.mock
import io.dropwizard.testing.junit.ResourceTestRule
import io.quartic.bild.resource.BackChannelResource
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

class BackChannelResourceShould {
    @Test
    fun serve_up_runner_more_than_once() {
        for (i in 0..1) {
            val response = resource.jerseyTest.target("/backchannel/runner")
                .request()
                .get()

            assertThat("$i", response.readEntity(String::class.java), equalTo("Hello world"))
        }
    }

    @Rule
    @JvmField
    val resource = ResourceTestRule.builder()
        .addResource(BackChannelResource(mock()))
        .build()
}
