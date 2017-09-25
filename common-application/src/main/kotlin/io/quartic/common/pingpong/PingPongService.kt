package io.quartic.common.pingpong

import io.quartic.common.client.ClientBuilder.Companion.Jaxable
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import retrofit2.http.GET
import java.util.concurrent.CompletableFuture
import javax.ws.rs.core.MediaType

@Jaxable
@javax.ws.rs.Path("/ping")
@javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
interface PingPongService {
    @javax.ws.rs.GET
    fun ping(): Pong
}

@Retrofittable
interface PingPongClient {
    @GET("ping")
    fun pingAsync(): CompletableFuture<Pong>
}
