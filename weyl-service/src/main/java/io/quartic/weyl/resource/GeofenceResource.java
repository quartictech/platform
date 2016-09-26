package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import io.quartic.weyl.core.geofence.*;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.*;
import java.util.Map;

import static io.quartic.weyl.core.utils.Utils.uuid;

@Path("/geofence")
@Consumes("application/json")
public class GeofenceResource {
    private final GeofenceStore geofenceStore;

    public GeofenceResource(GeofenceStore geofenceStore) {
        this.geofenceStore = geofenceStore;
    }

    @PUT
    public void update(GeofenceRequest geofenceRequest) {
        Polygon[] polygons = geofenceRequest.features().features().stream()
                .map(f -> (Polygon) Utils.toJts(f.geometry()))
                .toArray(Polygon[]::new);

        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons);
        geofenceStore.setGeofence(Geofence.of(uuid(GeofenceId::of), geofenceRequest.type(),  multiPolygon));
    }

    @GET
    @Produces("application/json")
    public GeofenceState getState() {
        return geofenceStore.getGlobalState();
    }

    @GET
    @Path("/violations")
    @Produces("application/json")
    public Map<ViolationId, Violation> getViolations() {
        return ImmutableMap.of(
                id("abcd"), Violation.of(id("abcd"), "Something bad happened!"),
                id("efgh"), Violation.of(id("efgh"), "Something even worse happened!")
        );
    }

    private ViolationId id(String id) {
        return ViolationId.of(id);
    }
}
