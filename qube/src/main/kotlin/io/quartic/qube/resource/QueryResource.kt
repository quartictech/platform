package io.quartic.qube.resource

import io.quartic.qube.api.QubeQueryService
import io.quartic.common.model.CustomerId
import io.quartic.qube.api.model.Dag

// TODO: Remove this!
class QueryResource(val defaultPipeline: Dag) : QubeQueryService {
    override fun dag(customerId: CustomerId): Dag {
        return defaultPipeline
    }
}

