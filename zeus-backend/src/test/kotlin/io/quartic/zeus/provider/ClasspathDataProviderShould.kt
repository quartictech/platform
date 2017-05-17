package io.quartic.zeus.provider

import io.quartic.zeus.ClasspathDataProviderConfiguration
import io.quartic.zeus.model.ItemId
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class ClasspathDataProviderShould {
    @Test
    fun provide_data_from_classpath_resource() {
        val provider = ClasspathDataProvider(ClasspathDataProviderConfiguration("/noob.json", emptySet()))
        assertThat(provider.data, equalTo(mapOf(
                ItemId("123") to mapOf("name" to "alex") as Map<String, Any>,
                ItemId("456") to mapOf("name" to "arlo") as Map<String, Any>,
                ItemId("789") to mapOf("name" to "oliver") as Map<String, Any>
        )))
    }

    // TODO: test filtering
}