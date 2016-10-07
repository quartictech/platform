package io.quartic.weyl.resource;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.utils.UidGenerator;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("/geofence")
@Consumes("application/json")
public class GeofenceResource {
    private final GeofenceStore geofenceStore;
    private final UidGenerator<GeofenceId> gidGenerator = new UidGenerator<>(GeofenceId::of);

    public GeofenceResource(GeofenceStore geofenceStore) {
        this.geofenceStore = geofenceStore;
    }

    @PUT
    public void update(GeofenceRequest geofenceRequest) {
        Polygon[] polygons = geofenceRequest.features().features().stream()
                .map(f -> (Polygon) Utils.toJts(f.geometry()))
                .toArray(Polygon[]::new);

        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons);
        geofenceStore.setGeofence(Geofence.of(gidGenerator.get(), geofenceRequest.type(),  multiPolygon));
    }
}
