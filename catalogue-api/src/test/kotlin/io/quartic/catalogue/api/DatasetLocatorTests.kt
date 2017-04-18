package io.quartic.catalogue.api

import io.quartic.common.serdes.objectMapper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

abstract class DatasetLocatorTests<out T : DatasetLocator> {
    protected abstract fun locator(): T
    protected abstract fun json(): String

    @Test
    fun deserialize_as_expected() {
        val datasetLocator = objectMapper().readValue(json(), DatasetLocator::class.java as Class<T>)
        assertThat(datasetLocator, equalTo(locator()))
    }
}
