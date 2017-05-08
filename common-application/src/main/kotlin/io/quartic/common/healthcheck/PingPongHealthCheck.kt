package io.quartic.common.healthcheck

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result.healthy
import io.quartic.common.client.client
import io.quartic.common.pingpong.PingPongService

class PingPongHealthCheck(owner: Class<*>, url: String) : HealthCheck() {
    private val pingPong: PingPongService

    init {
        this.pingPong = client(owner, url)
    }

    override fun check(): Result = healthy(pingPong.ping().version)
}
