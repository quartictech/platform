package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.LayerId

data class BufferSpec(
        val layerId: LayerId,
        val bufferDistance: Double
) : ComputationSpec
