package io.quartic.weyl.core.model

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.quartic.weyl.core.live.LayerViewType
import io.quartic.weyl.core.live.LayerViewType.MOST_RECENT

data class MapDatasetExtension @JvmOverloads constructor(
        @JsonUnwrapped val staticSchema: StaticSchema,
        val viewType: LayerViewType = MOST_RECENT
)
