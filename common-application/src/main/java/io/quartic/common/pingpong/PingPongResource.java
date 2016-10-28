package io.quartic.common.pingpong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/ping")
@Produces(MediaType.APPLICATION_JSON)
public class PingPongResource {
    @GET
    public Pong ping() {
        final String version = getClass().getPackage().getImplementationVersion();
        return Pong.of(version == null ? "unknown" : version);
    }
}
