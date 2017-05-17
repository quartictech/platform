package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId

interface DataProvider {
    val data: Map<ItemId, Map<String, Any>>
}