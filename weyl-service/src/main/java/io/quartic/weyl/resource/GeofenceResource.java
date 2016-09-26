package io.quartic.weyl.resource;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceState;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.*;

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
        geofenceStore.setGeofence(Geofence.of(geofenceRequest.type(),  multiPolygon));
    }

    @GET
    @Produces("application/json")
    public GeofenceState getState() {
        return geofenceStore.getGlobalState();
    }
}
