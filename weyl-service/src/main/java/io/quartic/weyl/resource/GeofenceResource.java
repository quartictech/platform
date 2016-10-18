package io.quartic.weyl.resource;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

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
                .map(f -> (Polygon) Utils.toJts(f.geometry().get()))
                .toArray(Polygon[]::new));
    }

    private void updateFromLayerId(GeofenceType type, LayerId layerId) {
        update(type, layerStore.getLayer(layerId).get()
                .features().stream()
                .map(f -> (Polygon) f.geometry())
                .toArray(Polygon[]::new));
    }

    private void update(GeofenceType type, Polygon[] polygons) {
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons);
        geofenceStore.setGeofence(Geofence.of(gidGenerator.get(), type, multiPolygon));
    }
}
