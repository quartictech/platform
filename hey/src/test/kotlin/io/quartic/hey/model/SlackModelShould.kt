package io.quartic.hey.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SlackModelShould {

    @Test
    fun generate_timestamps_correctly() {
        val attachment = SlackAttachment(
            title = "Foo",
            text = "Bar",
            timestamp = OffsetDateTime.of(2017, 7, 20, 16, 15, 23, 0, ZoneOffset.ofHours(1))
        )

        val json = OBJECT_MAPPER.writeValueAsString(attachment)
        val parsed = OBJECT_MAPPER.readValue<Map<*, *>>(json)

        assertThat(parsed["ts"] as Int, equalTo(1500563723))
    }
}
