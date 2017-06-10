package io.quartic.catalogue.api.model

import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

abstract class DatasetLocatorTests<out T : DatasetLocator> {
    protected abstract fun locator(): T
    protected abstract fun json(): String

    @Test
    fun deserialize_as_expected() {
        @Suppress("UNCHECKED_CAST")
        val datasetLocator = OBJECT_MAPPER.readValue(json(), DatasetLocator::class.java as Class<*>) as T
        assertThat(datasetLocator, equalTo(locator()))
    }
}
