package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
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
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;
import io.quartic.weyl.request.GeofenceRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.geojson.Utils.toJts;

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

    private Stream<Geometry> geometriesFrom(FeatureCollection features) {
        return features.features().stream()
                .filter(f -> f.geometry().isPresent())
                .map(f -> geometryTransformer.transform(toJts(f.geometry().get())));
    }

    private Stream<Geometry> geometriesFrom(LayerId layerId) {
        return layerStore.getLayer(layerId).get()
                .features().stream()
                .map(Feature::geometry);
    }

    private void update(GeofenceType type, double bufferDistance, Stream<Geometry> geometries) {
        Collection<Geofence> geofences = geometries
                .map(g -> bufferOp(g, bufferDistance))
                .map(g -> Geofence.of(gidGenerator.get(), type, g))
                .collect(Collectors.toList());
        geofenceStore.setGeofences(geofences);
    }
}
