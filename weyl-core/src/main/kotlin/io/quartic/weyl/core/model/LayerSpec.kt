package io.quartic.weyl.core.model

import io.quartic.weyl.core.live.LayerView

data class LayerSpec(
    val id: LayerId,
    val metadata: LayerMetadata,
    val view: LayerView,
    val staticSchema: StaticSchema,
    val indexable: Boolean
)
