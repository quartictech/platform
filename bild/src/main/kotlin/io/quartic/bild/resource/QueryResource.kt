package io.quartic.bild.resource

import io.quartic.bild.api.BildQueryService
import io.quartic.bild.api.model.Dag
import io.quartic.bild.store.JobStore
import io.quartic.common.model.CustomerId

class QueryResource(val jobResults: JobStore, val defaultPipeline: Dag) : BildQueryService {
    // TODO: remove the fallback pipeline here once builds are working
    override fun dag(customerId: CustomerId): Dag? {
        val buildId = jobResults.getLatest(customerId)

        if (buildId != null) {
            val build = jobResults.getBuild(buildId)
            return build?.dag
        }

        return defaultPipeline
    }
}

