package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.AttributeName

data class Histogram(
        val attribute: AttributeName,
        val buckets: Collection<Bucket>
) {
    data class Bucket(val value: Any, val count: Long)
}
