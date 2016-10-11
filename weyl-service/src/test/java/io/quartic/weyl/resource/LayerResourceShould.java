package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LiveLayer;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerViewType;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
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
        when(liveLayerStore.listLayers()).thenReturn(ImmutableList.of(
                LiveLayer.of(LayerId.of("666"), mock(AbstractLayer.class), ImmutableList.of(), (gen, history) -> history.stream())
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
                LayerMetadata.of("foo", "bar", Optional.empty()),
                LiveLayerViewType.LOCATION_AND_TRACK,
                ImmutableList.of(LiveEvent.of(Instant.now(), Optional.of(collection), Optional.empty())));
    }
}
