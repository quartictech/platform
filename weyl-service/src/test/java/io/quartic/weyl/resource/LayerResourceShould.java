package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerViewType;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.request.LayerUpdateRequest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotAcceptableException;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayerResourceShould {
    private final LiveLayerStore liveLayerStore = mock(LiveLayerStore.class);
    private final LayerResource resource = new LayerResource(mock(LayerStore.class), liveLayerStore);

    @Before
    public void setUp() throws Exception {
        Layer layer = Layer.of(
                mock(AttributeSchema.class),
                LayerMetadata.of("foo", "bar", Optional.empty(), Optional.empty()),
                mock(io.quartic.weyl.core.feature.FeatureCollection.class)
        );
        when(liveLayerStore.listLayers()).thenReturn(ImmutableList.of(
                IndexedLayer.builder()
                        .layerId(LayerId.of("666"))
                        .layer(layer)
                        .feedEvents(ImmutableList.of())
                        .view((g, f) -> f.stream())
                        .spatialIndex(mock(SpatialIndex.class))
                        .indexedFeatures(ImmutableList.of())
                        .layerStats(mock(LayerStats.class))
                        .build()
        ));
    }

    @Test
    public void acceptValidFeatureCollection() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.of("5678"), point(), propsWithTimestamp())
        ));

        resource.updateLiveLayer("666", createRequest(collection));
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfIdsMissing() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.empty(), point(), propsWithTimestamp())
        ));

        resource.updateLiveLayer("666", createRequest(collection));
    }

    private ImmutableMap<String, Object> propsWithTimestamp() {
        return ImmutableMap.of("timestamp", 12345);
    }

    private Optional<Point> point() {
        return Optional.of(Point.of(ImmutableList.of(1.0, 2.0)));
    }

    private LayerUpdateRequest createRequest(FeatureCollection collection) {
        return LayerUpdateRequest.of(
                LayerMetadata.of("foo", "bar", Optional.empty(), Optional.empty()),
                LiveLayerViewType.LOCATION_AND_TRACK,
                ImmutableList.of(LiveEvent.of(Instant.now(), Optional.of(collection), Optional.empty())));
    }
}
