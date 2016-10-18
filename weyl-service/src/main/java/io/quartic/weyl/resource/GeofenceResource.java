package io.quartic.weyl.resource;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.stream.Stream;

import static io.quartic.weyl.core.geojson.Utils.toJts;

@Path("/geofence")
@Consumes("application/json")
public class GeofenceResource {
    private final GeofenceStore geofenceStore;
    private final LayerStore layerStore;
    private final UidGenerator<GeofenceId> gidGenerator = new SequenceUidGenerator<>(GeofenceId::of);

    public GeofenceResource(GeofenceStore geofenceStore, LayerStore layerStore) {
        this.geofenceStore = geofenceStore;
        this.layerStore = layerStore;
    }

    @PUT
    public void update(GeofenceRequest request) {
        request.features().ifPresent(f -> updateFromFeatureCollection(request.type(), f));
        request.layerId().ifPresent(id -> updateFromLayerId(request.type(), id));
    }

    private void updateFromFeatureCollection(GeofenceType type, FeatureCollection features) {
        update(type, features.features().stream()
                .filter(f -> f.geometry().isPresent())
                .map(f -> toJts(f.geometry().get())));
    }

    private void updateFromLayerId(GeofenceType type, LayerId layerId) {
        update(type, layerStore.getLayer(layerId).get()
                .features().stream()
                .map(Feature::geometry));
    }

    private void update(GeofenceType type, Stream<Geometry> geometries) {
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(
                geometries
                .filter(g -> g instanceof Polygon)
                .map(g -> (Polygon) g)
                .toArray(Polygon[]::new)
        );
        geofenceStore.setGeofence(Geofence.of(gidGenerator.get(), type, multiPolygon));
    }
}
