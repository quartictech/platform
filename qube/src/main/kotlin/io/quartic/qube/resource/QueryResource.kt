package io.quartic.qube.resource

import io.quartic.qube.api.QubeQueryService
import io.quartic.qube.api.model.Dag
import io.quartic.common.model.CustomerId

// TODO: Remove this!
class QueryResource(val defaultPipeline: Dag) : QubeQueryService {
    override fun dag(customerId: CustomerId): Dag {
        return defaultPipeline
    }
}

