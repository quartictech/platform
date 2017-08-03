package io.quartic.bild.resource

import io.quartic.bild.api.BildService
import javax.ws.rs.WebApplicationException

class BildResource(val pipelines: Map<String, Any>) : BildService {
    override fun getDag(customerId: String): Any =
        pipelines.getOrElse(customerId, { throw WebApplicationException(404) })
}

