package io.quartic.weyl.update

import com.nhaarman.mockito_kotlin.mock
import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.AttributesImpl
import io.quartic.weyl.core.model.Feature
import io.quartic.weyl.core.model.FeatureImpl
import io.quartic.weyl.update.TableUpdateGenerator.Companion.TABULAR_ATTRIBUTE_NAME
import io.quartic.weyl.update.TableUpdateGenerator.Table
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class TableUpdateGeneratorShould {
    private val gen = TableUpdateGenerator()

    @Test
    fun cope_when_no_table_attribute() {
        val result = gen.generate(listOf(
                feature(mapOf(
                        AttributeName.fromString("other") to "oh no"
                ))
        ))

        assertThat(result, equalTo(Table(emptySet(), emptyList())))
    }

    @Test
    fun ignore_non_table_attributes() {
        val result = gen.generate(listOf(
                feature(mapOf(
                        TABULAR_ATTRIBUTE_NAME to listOf(
                                mapOf("name" to "arlo", "age" to 56),
                                mapOf("name" to "alex", "age" to 57)
                        ),
                        AttributeName.fromString("other") to "oh no"
                ))
        ))

        assertThat(result, equalTo(
                Table(setOf("name", "age"), listOf(
                        mapOf("name" to "arlo", "age" to 56),
                        mapOf("name" to "alex", "age" to 57)
                ))
        ))
    }

    @Test
    fun ignore_if_table_attribute_is_not_a_list() {
        val result = gen.generate(listOf(
                feature(mapOf(
                        TABULAR_ATTRIBUTE_NAME to "wtf"
                ))
        ))

        assertThat(result, equalTo(Table(emptySet(), emptyList())))
    }

    @Test
    fun ignore_table_attribute_entries_that_are_not_maps() {
        val result = gen.generate(listOf(
                feature(mapOf(
                        TABULAR_ATTRIBUTE_NAME to listOf(
                                mapOf("name" to "arlo", "age" to 56),
                                mapOf("name" to "alex", "age" to 57),
                                "wtf"
                        )
                ))
        ))

        assertThat(result, equalTo(
                Table(setOf("name", "age"), listOf(
                        mapOf("name" to "arlo", "age" to 56),
                        mapOf("name" to "alex", "age" to 57)
                ))
        ))
    }

    @Test
    fun concatenate_across_multiple_entities() {
        val result = gen.generate(listOf(
                feature(mapOf(
                        TABULAR_ATTRIBUTE_NAME to listOf(
                                mapOf("name" to "arlo", "age" to 56),
                                mapOf("name" to "alex", "age" to 57)
                ))),
                feature(mapOf(
                        TABULAR_ATTRIBUTE_NAME to listOf(
                                mapOf("name" to "oliver", "age" to 58),
                                mapOf("name" to "quarty", "age" to 59)
                )))
        ))

        assertThat(result, equalTo(
                Table(setOf("name", "age"), listOf(
                        mapOf("name" to "arlo", "age" to 56),
                        mapOf("name" to "alex", "age" to 57),
                        mapOf("name" to "oliver", "age" to 58),
                        mapOf("name" to "quarty", "age" to 59)
                ))
        ))
    }

    @Test
    fun union_schema_for_heterogeneous_records() {
        val result = gen.generate(listOf(
                feature(mapOf(
                        TABULAR_ATTRIBUTE_NAME to listOf(
                                mapOf("name" to "arlo", "age" to 56),
                                mapOf("name" to "alex", "num_legs" to 3)
                        )))
        ))

        assertThat(result, equalTo(
                Table(setOf("name", "age", "num_legs"), listOf(
                        mapOf("name" to "arlo", "age" to 56),
                        mapOf("name" to "alex", "num_legs" to 3)
                ))
        ))
    }

    private fun feature(attributes: Map<AttributeName, Any>): Feature {
        return FeatureImpl.of(mock(), mock(), AttributesImpl.of(attributes))
    }
}