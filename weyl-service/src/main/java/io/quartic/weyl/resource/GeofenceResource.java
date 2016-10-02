package io.quartic.weyl.resource;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

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
}
