package io.quartic.zeus.model

data class Dataset(
        val schema: List<String>,
        val content: Map<ItemId, Map<String, Any>>
)