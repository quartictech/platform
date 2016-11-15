package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Point;
import io.quartic.geojson.Polygon;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.request.ImmutableGeofenceRequest;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.geojson.Utils.fromJts;
import static io.quartic.weyl.core.geojson.Utils.toJts;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatorToWebMercator;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;

public class GeofenceResourceShould {
    private static final ImmutableMap<String, Object> FEATURE_METADATA = ImmutableMap.of("some_prop", 76);
    private final GeofenceStore geofenceStore = mock(GeofenceStore.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final GeofenceResource resource = new GeofenceResource(webMercatorToWebMercator(), geofenceStore, layerStore);
    private int nextGeofenceId = 1;

    private final Polygon polyA = geojsonPolygon(5.0);
    private final Polygon polyB = geojsonPolygon(6.0);

    @Test
    public void set_geofence_based_on_features() throws Exception {
        final FeatureCollection features = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("123"), Optional.of(polyA), emptyMap()),
                Feature.of(Optional.of("456"), Optional.of(polyB), emptyMap())
        ));

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(0.0)
                .build());

        verifyGeofence(emptyMap(), polyA, polyB);
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        final LayerId layerId = LayerId.of("789");
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final AbstractLayer layer = mock(AbstractLayer.class);
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

        verifyGeofence(FEATURE_METADATA, polyA, polyB);
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("123"), Optional.of(polyA), emptyMap()),
                Feature.of(Optional.of("456"), Optional.of(geojsonPoint()), emptyMap())
        ));

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(0.0)
                .build());

        verifyGeofence(emptyMap(), polyA);
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("456"), Optional.of(geojsonPoint()), emptyMap())
        ));

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .bufferDistance(1.0)
                .build());

        verifyGeofence(emptyMap(), (Polygon) fromJts(bufferOp(toJts(geojsonPoint()), 1.0)));
    }

    private void verifyGeofence(Map<String, Object> metadata, Polygon... polygons) {
        verify(geofenceStore).setGeofences(
                stream(polygons)
                        .map(p -> geofenceOf(metadata, p))
                        .collect(toList()));
    }

    private Geofence geofenceOf(Map<String, Object> metadata, Polygon polygon) {
        return Geofence.of(GeofenceId.of(Integer.toString(nextGeofenceId++)), GeofenceType.INCLUDE, toJts(polygon), metadata);
    }

    private io.quartic.weyl.core.model.Feature modelFeatureOf(io.quartic.geojson.Geometry geometry) {
        return ImmutableFeature.of("123", FeatureId.of("abc"), toJts(geometry), FEATURE_METADATA);
    }

    private Point geojsonPoint() {
        return Point.of(newArrayList(1.0, 2.0));
    }

    private Polygon geojsonPolygon(double offset) {
        return Polygon.of(ImmutableList.of(ImmutableList.of(
                newArrayList(1.0 + offset, 2.0 + offset),
                newArrayList(1.0 + offset, 3.0 + offset),
                newArrayList(2.0 + offset, 3.0 + offset),
                newArrayList(2.0 + offset, 2.0 + offset),
                newArrayList(1.0 + offset, 2.0 + offset)
        )));
    }
}
