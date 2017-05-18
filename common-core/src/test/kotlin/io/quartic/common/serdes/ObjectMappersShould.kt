package io.quartic.common.serdes

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.test.assertThrows
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test

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
}