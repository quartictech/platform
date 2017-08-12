package io.quartic.zeus.model

data class Dataset(
        val schema: List<String>,   // TODO - this should probably live in DatasetInfo
        val content: Map<ItemId, Map<String, Any>>
)