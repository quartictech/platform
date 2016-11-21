package io.quartic.weyl.resource;

import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.geojson.Utils.toJts;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static java.util.stream.Collectors.toList;

@Path("/geofence")
@Consumes("application/json")
public class GeofenceResource {
    private final GeometryTransformer geometryTransformer;
    private final GeofenceStore geofenceStore;
    private final LayerStore layerStore;

    public GeofenceResource(GeometryTransformer geometryTransformer, GeofenceStore geofenceStore, LayerStore layerStore) {
        this.geometryTransformer = geometryTransformer;
        this.geofenceStore = geofenceStore;
        this.layerStore = layerStore;
    }

    @PUT
    public void update(GeofenceRequest request) {
        request.features().ifPresent(f -> update(request.type(), request.bufferDistance(), featuresFrom(f)));
        request.layerId().ifPresent(id -> update(request.type(), request.bufferDistance(), featuresFrom(id)));
    }

    private Stream<Feature> featuresFrom(FeatureCollection features) {
        return features.features().stream()
                .filter(f -> f.geometry().isPresent())
                .map(f -> FeatureImpl.of(
                        EntityIdImpl.of("custom"),
                        geometryTransformer.transform(toJts(f.geometry().get())),
                        EMPTY_ATTRIBUTES
                ));
    }

    private Stream<Feature> featuresFrom(LayerId layerId) {
        // TODO: validate that layer exists
        return layerStore.getLayer(layerId).get()
                .features().stream();
    }

    private void update(GeofenceType type, double bufferDistance, Stream<Feature> features) {
        final List<Geofence> geofences = features
                .map(f -> FeatureImpl.copyOf(f)
                        .withGeometry(bufferOp(f.geometry(), bufferDistance))
                        .withEntityId(EntityIdImpl.of("geofence/" + f.entityId().uid()))
                )
                .filter(f -> !f.geometry().isEmpty())
                .map(f -> GeofenceImpl.of(type, f))
                .collect(toList());
        geofenceStore.setGeofences(geofences);
    }
}
