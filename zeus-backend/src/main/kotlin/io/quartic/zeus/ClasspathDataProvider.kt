package io.quartic.zeus

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.zeus.model.ItemId

class ClasspathDataProvider(private val resourceName: String) : DataProvider {
    override val data by lazy {
        OBJECT_MAPPER.readValue<Map<ItemId, Map<String, Any>>>(javaClass.getResourceAsStream(resourceName))
    }
}