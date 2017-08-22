package io.quartic.hey.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HeyApiShould {

    @Test
    fun handle_date_formats_correctly() {
        val json = """
            {
                "title": "Foo",
                "text": "Bar",
                "timestamp": "2017-07-20T16:15:23+01:00"
            }
        """

        val attachment = OBJECT_MAPPER.readValue<HeyAttachment>(json)

        println(attachment.timestamp?.toEpochSecond())

        assertThat(attachment.timestamp, equalTo(OffsetDateTime.of(2017, 7, 20, 16, 15, 23, 0, ZoneOffset.ofHours(1))))
    }
}
