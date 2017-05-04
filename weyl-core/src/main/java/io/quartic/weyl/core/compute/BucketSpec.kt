package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.LayerId

data class BucketSpec (
        val buckets: LayerId,
        val features: LayerId,
        val aggregation: BucketAggregation,
        val normalizeToArea: Boolean
) : ComputationSpec
