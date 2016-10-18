package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Polygon;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.request.ImmutableGeofenceRequest;
import org.junit.Test;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.geojson.Utils.toJts;
import static org.mockito.Mockito.*;

public class GeofenceResourceShould {
    private final GeofenceStore geofenceStore = mock(GeofenceStore.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final GeofenceResource resource = new GeofenceResource(geofenceStore, layerStore);
    private final GeometryFactory factory = new GeometryFactory();

    private final Polygon polyA = geojsonPolygon(5.0);
    private final Polygon polyB = geojsonPolygon(6.0);


    @Test
    public void set_geofence_based_on_features() throws Exception {
        final FeatureCollection features = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("123"), Optional.of(polyA), ImmutableMap.of()),
                Feature.of(Optional.of("456"), Optional.of(polyB), ImmutableMap.of())
        ));

        resource.update(ImmutableGeofenceRequest.builder()
                .type(GeofenceType.INCLUDE)
                .features(features)
                .build());

        verifyGeofence();
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
                .build());

        verifyGeofence();
    }

    private void verifyGeofence() {
        verify(geofenceStore).setGeofence(Geofence.of(
                GeofenceId.of("1"),
                GeofenceType.INCLUDE,
                factory.createMultiPolygon(new com.vividsolutions.jts.geom.Polygon[]{toJts(polyA), toJts(polyB)})
        ));
    }

    private io.quartic.weyl.core.model.Feature modelFeatureOf(io.quartic.weyl.core.geojson.Geometry geometry) {
        return ImmutableFeature.of("123", FeatureId.of("abc"), toJts(geometry), ImmutableMap.of());
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
