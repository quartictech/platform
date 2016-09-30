package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.live.LiveLayer;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.request.LayerUpdateRequest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotAcceptableException;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayerResourceShould {

    private final LiveLayerStore liveLayerStore = mock(LiveLayerStore.class);
    private final LayerResource resource = new LayerResource(mock(LayerStore.class), liveLayerStore);

    @Before
    public void setUp() throws Exception {
        when(liveLayerStore.listLayers()).thenReturn(ImmutableList.of(
                LiveLayer.of(LayerId.of("abc"), mock(Layer.class))
        ));
    }

    @Test
    public void acceptValidFeatureCollection() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(id("1234"), point(), propsWithTimestamp()),
                Feature.of(id("5678"), point(), propsWithTimestamp())
        ));

        resource.updateLiveLayer("abc", createRequest(collection));
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfIdsMissing() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(id("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.empty(), point(), propsWithTimestamp())
        ));

        resource.updateLiveLayer("abc", createRequest(collection));
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfTimestampsMissing() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(id("1234"), point(), propsWithTimestamp()),
                Feature.of(id("5678"), point(), propsWithoutTimestamp())
        ));

        resource.updateLiveLayer("abc", createRequest(collection));
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfTimestampsNonNumeric() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(id("1234"), point(), propsWithTimestamp()),
                Feature.of(id("5678"), point(), propsWithInvalidTimestamp())
        ));

        resource.updateLiveLayer("abc", createRequest(collection));
    }

    private Optional<FeatureId> id(String id) {
        return Optional.of(FeatureId.of(id));
    }

    private ImmutableMap<String, Object> propsWithTimestamp() {
        return ImmutableMap.of("timestamp", 12345);
    }

    private ImmutableMap<String, Object> propsWithInvalidTimestamp() {
        return ImmutableMap.of("timestamp", "a_232_.3");
    }

    private ImmutableMap<String, Object> propsWithoutTimestamp() {
        return ImmutableMap.of();
    }

    private Point point() {
        return Point.of(ImmutableList.of(1.0, 2.0));
    }

    private LayerUpdateRequest createRequest(FeatureCollection collection) {
        return LayerUpdateRequest.of("foo", "bar", collection);
    }
}
