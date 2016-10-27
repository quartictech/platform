package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LiveEventId;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.request.LayerUpdateRequest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotAcceptableException;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayerResourceShould {
    private final LayerStore layerStore = mock(LayerStore.class);
    private final LayerResource resource = new LayerResource(
            layerStore,
            new SequenceUidGenerator<>(FeatureId::of),
            new SequenceUidGenerator<>(LiveEventId::of));

    @Before
    public void setUp() throws Exception {
        when(layerStore.listLayers()).thenReturn(ImmutableList.of(
                Layer.builder()
                        .layerId(LayerId.of("666"))
                        .metadata(LayerMetadata.of("foo", "bar", Optional.empty(), Optional.empty()))
                        .features(mock(io.quartic.weyl.core.feature.FeatureCollection.class))
                        .schema(mock(AttributeSchema.class))
                        .live(true)
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
                LayerViewType.LOCATION_AND_TRACK,
                ImmutableList.of(LiveEvent.of(Instant.now(), Optional.of(collection), Optional.empty())));
    }
}
