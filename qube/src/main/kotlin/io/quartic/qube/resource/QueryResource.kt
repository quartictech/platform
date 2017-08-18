package io.quartic.qube.resource

import io.quartic.qube.api.QubeQueryService
import io.quartic.qube.api.model.Dag
import io.quartic.qube.store.BuildStore
import io.quartic.common.model.CustomerId

class QueryResource(val buildStore: BuildStore, val defaultPipeline: Dag) : QubeQueryService {
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

