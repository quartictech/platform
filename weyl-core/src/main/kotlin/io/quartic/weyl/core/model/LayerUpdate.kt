package io.quartic.weyl.core.model

import io.quartic.weyl.api.LayerUpdateType

data class LayerUpdate(
        val type: LayerUpdateType,
        val features: Collection<NakedFeature>  // TODO: not Collection
)
