package io.quartic.common.serdes

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.test.assertThrows
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ObjectMappersShould {
    data class Foo(val x: Double)

    @Test
    fun reject_null_primitives() {
        assertThrows<JsonMappingException> {
            OBJECT_MAPPER.readValue<Foo>("""{ "x": null }""")
        }
    }

    data class Bar(val x: List<Double>)

    // TODO: we don't actually want this behaviour! But it happens anyway, so using this test to document it.
    // See https://github.com/FasterXML/jackson-module-kotlin/issues/27
    @Test
    fun naughtily_allow_null_primitives_inside_collections() {
        assertThat(OBJECT_MAPPER.readValue<Bar>("""{ "x": [null] }""").x[0], nullValue())   // Subverts Kotlin type system!
    }

    @Test
    fun allow_non_numerics() {
        assertThat(OBJECT_MAPPER.readValue<Foo>("""{ "x": Infinity }""").x, equalTo(Double.POSITIVE_INFINITY))
        assertThat(OBJECT_MAPPER.readValue<Foo>("""{ "x": -Infinity }""").x, equalTo(Double.NEGATIVE_INFINITY))
        assertThat(OBJECT_MAPPER.readValue<Foo>("""{ "x": NaN }""").x, equalTo(Double.NaN))
    }

    data class Baz(val t: Instant)

    @Test
    fun expect_unix_timestamps_for_instants() {
        assertThat(OBJECT_MAPPER.readValue<Baz>("""{ "t": 1500561991 }""").t, equalTo(Instant.parse("2017-07-20T14:46:31Z")))
    }

    @Test
    fun write_instant_as_unix_timestamp() {
        assertThat(
            OBJECT_MAPPER.writeValueAsString(Baz(OffsetDateTime.of(2017, 7, 20, 14, 46, 31, 0, ZoneOffset.ofHours(1)).toInstant())),
            equalTo("""{"t":1500558391.000000000}""")
        )
    }
}
