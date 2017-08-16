package io.quartic.bild.resource

import io.quartic.bild.api.BildQueryService
import io.quartic.bild.api.model.Dag
import io.quartic.bild.store.BuildStore
import io.quartic.common.model.CustomerId

class QueryResource(val buildStore: BuildStore, val defaultPipeline: Dag) : BildQueryService {
    // TODO: remove the fallback pipeline here once builds are working
    override fun dag(customerId: CustomerId): Dag {
        val buildId = buildStore.getLatest(customerId)

        if (buildId != null) {
            val build = buildStore.getBuild(buildId)
            if (build?.dag != null) {
                return build.dag
            }
        }

        return defaultPipeline
    }
}

