package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId

interface DataProvider {
    val data: Map<io.quartic.zeus.model.ItemId, Map<String, Any>>
}