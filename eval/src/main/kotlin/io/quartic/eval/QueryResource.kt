package io.quartic.eval

import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryService
import io.quartic.eval.api.model.CytoscapeDag

class QueryResource(val defaultPipeline: CytoscapeDag) : EvalQueryService {
    override fun getDag(customerId: CustomerId): CytoscapeDag {
        return defaultPipeline
    }
}
