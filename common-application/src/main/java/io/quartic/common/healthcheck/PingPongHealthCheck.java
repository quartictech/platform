package io.quartic.common.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import io.quartic.common.client.ClientBuilder;
import io.quartic.common.pingpong.PingPongService;

public class PingPongHealthCheck extends HealthCheck {
    private final PingPongService pingPong;

    public PingPongHealthCheck(Class<?> owner, String url) {
        this.pingPong = ClientBuilder.build(PingPongService.class, owner, url);
    }

    @Override
    protected Result check() throws Exception {
        return Result.healthy(pingPong.ping().version());
    }
}
