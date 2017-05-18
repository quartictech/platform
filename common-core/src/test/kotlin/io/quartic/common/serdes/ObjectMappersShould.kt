package io.quartic.common.serdes

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.test.assertThrows
import org.junit.Test

class ObjectMappersShould {
    @Test
    fun reject_null_primitives() {
        data class Foo(val x: Double)

        assertThrows<JsonMappingException> {
            OBJECT_MAPPER.readValue<Foo>("""{ "x": null }""")
        }
    }
}