package io.quartic.common.healthcheck

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result.healthy
import io.quartic.common.client.ClientBuilder
import io.quartic.common.pingpong.PingPongClient
import java.net.URI

class PingPongHealthCheck(clientBuilder: ClientBuilder, url: URI) : HealthCheck() {
    private val pingPong: PingPongClient = clientBuilder.retrofit(url)

    override fun check(): Result = healthy(pingPong.pingAsync().get().version)
}
