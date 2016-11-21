package io.quartic.common.pingpong;

public class PingPongResource implements PingPongService {
    public Pong ping() {
        final String version = getClass().getPackage().getImplementationVersion();
        return PongImpl.of(version == null ? "unknown" : version);
    }
}
