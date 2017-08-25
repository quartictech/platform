package io.quartic.eval.api

import io.quartic.common.client.Jaxable
import io.quartic.common.client.Retrofittable
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.CytoscapeDag
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.CompletableFuture
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Jaxable
@javax.ws.rs.Path("/query")
interface EvalQueryService {
    @javax.ws.rs.GET
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("/dag/cytoscape/{customer_id}")
    fun getDag(
        @PathParam("customer_id") customerId: CustomerId
    ): CytoscapeDag
}

@Retrofittable
interface EvalQueryServiceClient {
    @GET("query/dag/cytoscape/{customer_id}")
    fun getDagAsync(
        @Path("customer_id") customerId: CustomerId
    ): CompletableFuture<CytoscapeDag>
}
