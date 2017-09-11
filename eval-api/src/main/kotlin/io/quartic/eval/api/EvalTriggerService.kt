package io.quartic.eval.api

import io.quartic.common.client.ClientBuilder.Companion.Jaxable
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import io.quartic.eval.api.model.BuildTrigger
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.CompletableFuture
import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Jaxable
@Path("/trigger")
interface EvalTriggerService {
    @retrofit2.http.POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun trigger(trigger: BuildTrigger)
}

@Retrofittable
interface EvalTriggerServiceClient {
    @POST("trigger")
    fun triggerAsync(@Body trigger: BuildTrigger): CompletableFuture<Void>
}
