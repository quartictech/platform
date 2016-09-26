package io.quartic.weyl.resource;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceState;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.List;
import java.util.stream.Collectors;

@Path("/geofence")
public class GeofenceResource {
    private final GeofenceStore geofenceStore;

    public GeofenceResource(GeofenceStore geofenceStore) {
        this.geofenceStore = geofenceStore;
    }

    @PUT
    public void update(GeofenceRequest geofenceRequest) {
        List<Polygon> polygonList = geofenceRequest.features().features().stream()
                .map(f -> (Polygon) Utils.toJts(f.geometry()))
                .collect(Collectors.toList());
        Polygon[] polygons = new Polygon[polygonList.size()];

        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons);
        geofenceStore.setGeofence(Geofence.of(geofenceRequest.type(),  multiPolygon));
    }

    @GET
    public GeofenceState getState() {
        return geofenceStore.getGlobalState();
    }
}
