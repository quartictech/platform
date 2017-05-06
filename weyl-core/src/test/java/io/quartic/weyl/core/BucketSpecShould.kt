package io.quartic.weyl.core

import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.weyl.core.compute.BucketSum
import io.quartic.weyl.core.model.AttributeName
import org.junit.Test

data class Noob(val attribute: AttributeName)

class BucketSpecShould {
    @Test
    fun name() {
        val json = """
            {
                "type": "sum",
                "attribute": "balls"
            }
        """.trim()

        val readValue = OBJECT_MAPPER.readValue(json, BucketSum::class.java)
        println(readValue)
    }
}