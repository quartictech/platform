package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import io.quartic.geojson.*;
import io.quartic.geojson.FeatureImpl;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.request.ImmutableGeofenceRequest;
import org.junit.Test;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.geojson.Utils.fromJts;
import static io.quartic.weyl.core.geojson.Utils.toJts;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatorToWebMercator;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;

public class GeofenceResourceShould {
    private static final Attributes FEATURE_ATTRIBUTES = mock(Attributes.class);
    private final GeofenceStore geofenceStore = mock(GeofenceStore.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final GeofenceResource resource = new GeofenceResource(webMercatorToWebMercator(), geofenceStore, layerStore);

    private final Polygon polyA = geojsonPolygon(5.0);
    private final Polygon polyB = geojsonPolygon(6.0);

    private int nextId = 1;

    @Test
    public void set_geofence_based_on_features() throws Exception {
        final FeatureCollection features = FeatureCollectionImpl.of(ImmutableList.of(
                FeatureImpl.of(Optional.of("123"), Optional.of(polyA), emptyMap()),
                FeatureImpl.of(Optional.of("456"), Optional.of(polyB), emptyMap())
        ));

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(0.0)
                .build());

        verifyGeofence("custom", EMPTY_ATTRIBUTES, polyA, polyB);
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        final LayerId layerId = LayerIdImpl.of("789");
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final Layer layer = mock(Layer.class);
        when(layerStore.getLayer(layerId)).thenReturn(Optional.of(layer));
        when(layer.features()).thenReturn(featureCollection);
        when(featureCollection.stream()).thenReturn(
                newArrayList(
                        modelFeatureOf(polyA),
                        modelFeatureOf(polyB)
                ).stream()
        );

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .layerId(layerId)
                .bufferDistance(0.0)
                .build());

        verifyGeofence("xyz", FEATURE_ATTRIBUTES, polyA, polyB);
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = FeatureCollectionImpl.of(ImmutableList.of(
                FeatureImpl.of(Optional.of("123"), Optional.of(polyA), emptyMap()),
                FeatureImpl.of(Optional.of("456"), Optional.of(geojsonPoint()), emptyMap())
        ));

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(0.0)
                .build());

        verifyGeofence("custom", EMPTY_ATTRIBUTES, polyA);
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = FeatureCollectionImpl.of(ImmutableList.of(
                io.quartic.geojson.FeatureImpl.of(Optional.of("456"), Optional.of(geojsonPoint()), emptyMap())
        ));

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(1.0)
                .build());

        verifyGeofence("custom", EMPTY_ATTRIBUTES, (Polygon) fromJts(bufferOp(toJts(geojsonPoint()), 1.0)));
    }

    private void verifyGeofence(String id, Attributes attributes, Polygon... polygons) {
        verify(geofenceStore).setGeofences(
                stream(polygons)
                        .map(p -> geofenceOf(id, attributes, p))
                        .collect(toList()));
    }

    private Geofence geofenceOf(String id, Attributes attributes, Polygon polygon) {
        return GeofenceImpl.of(
                GeofenceType.INCLUDE,
                io.quartic.weyl.core.model.FeatureImpl.of(EntityIdImpl.of("geofence/" + id), toJts(polygon), attributes)
        );
    }

    private Feature modelFeatureOf(io.quartic.geojson.Geometry geometry) {
        return io.quartic.weyl.core.model.FeatureImpl.of(
                EntityIdImpl.of("xyz"),
                toJts(geometry),
                FEATURE_ATTRIBUTES);
    }

    private Point geojsonPoint() {
        return PointImpl.of(newArrayList(1.0, 2.0));
    }

    private Polygon geojsonPolygon(double offset) {
        return PolygonImpl.of(ImmutableList.of(ImmutableList.of(
                newArrayList(1.0 + offset, 2.0 + offset),
                newArrayList(1.0 + offset, 3.0 + offset),
                newArrayList(2.0 + offset, 3.0 + offset),
                newArrayList(2.0 + offset, 2.0 + offset),
                newArrayList(1.0 + offset, 2.0 + offset)
        )));
    }
}
