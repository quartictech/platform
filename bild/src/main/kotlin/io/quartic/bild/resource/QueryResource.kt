package io.quartic.bild.resource

import io.quartic.bild.JobResultStore
import io.quartic.bild.api.BildQueryService
import io.quartic.common.model.CustomerId

class QueryResource(val jobResults: JobResultStore, val defaultPipeline: Any) : BildQueryService {
    // TODO: remove the fallback pipeline here once builds are working
    override fun dag(customerId: CustomerId): Any = jobResults.getLatest(customerId)?.dag ?: defaultPipeline
}

