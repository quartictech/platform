package io.quartic.weyl.resource;

import io.quartic.common.uid.SequenceUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.GeometryWithAttributes;
import io.quartic.weyl.core.model.ImmutableGeometryWithAttributes;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.geojson.Utils.toJts;
import static io.quartic.weyl.core.model.AbstractAttributes.EMPTY_ATTRIBUTES;

@Path("/geofence")
@Consumes("application/json")
public class GeofenceResource {
    private final GeometryTransformer geometryTransformer;
    private final GeofenceStore geofenceStore;
    private final LayerStore layerStore;
    private final UidGenerator<GeofenceId> gidGenerator = new SequenceUidGenerator<>(GeofenceId::of);

    public GeofenceResource(GeometryTransformer geometryTransformer, GeofenceStore geofenceStore, LayerStore layerStore) {
        this.geometryTransformer = geometryTransformer;
        this.geofenceStore = geofenceStore;
        this.layerStore = layerStore;
    }

    @PUT
    public void update(GeofenceRequest request) {
        request.features().ifPresent(f -> update(request.type(), request.bufferDistance(), geometriesFrom(f)));
        request.layerId().ifPresent(id -> update(request.type(), request.bufferDistance(), geometriesFrom(id)));
    }

    private Stream<GeometryWithAttributes> geometriesFrom(FeatureCollection features) {
        return features.features().stream()
                .filter(f -> f.geometry().isPresent())
                .map(f -> geometryTransformer.transform(toJts(f.geometry().get())))
                .map(g -> ImmutableGeometryWithAttributes.of(g, EMPTY_ATTRIBUTES));
    }

    private Stream<GeometryWithAttributes> geometriesFrom(LayerId layerId) {
        return layerStore.getLayer(layerId).get()
                .features().stream()
                .map(f -> ImmutableGeometryWithAttributes.of(f.geometry(), f.attributes()));
    }

    private void update(GeofenceType type, double bufferDistance, Stream<GeometryWithAttributes> geometries) {
        Collection<Geofence> geofences = geometries
                .map(g -> ImmutableGeometryWithAttributes.copyOf(g).withGeometry(bufferOp(g.geometry(), bufferDistance)))
                .filter(g -> !g.geometry().isEmpty())
                .map(g -> Geofence.of(gidGenerator.get(), type, g.geometry(), g.attributes()))
                .collect(Collectors.toList());
        geofenceStore.setGeofences(geofences);
    }
}
