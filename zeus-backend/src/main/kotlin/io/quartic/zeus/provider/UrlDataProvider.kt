package io.quartic.zeus.provider

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.zeus.UrlDataProviderConfiguration
import io.quartic.zeus.model.ItemId

class UrlDataProvider(config: UrlDataProviderConfiguration) : DataProvider {
    override val data by lazy {
        OBJECT_MAPPER.readValue<Map<ItemId, Map<String, Any>>>(config.url)
    }

    override val matcher = SelectiveMatcher(config.indexedAttributes, data)
}