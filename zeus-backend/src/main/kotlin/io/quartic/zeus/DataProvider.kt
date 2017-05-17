package io.quartic.zeus

import io.quartic.zeus.model.ItemId

interface DataProvider {
    val data: Map<ItemId, Map<String, Any>>
}