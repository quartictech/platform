package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.live.LiveLayerStore;
import org.junit.Test;

import javax.ws.rs.NotAcceptableException;
import java.util.Optional;

import static org.mockito.Mockito.mock;

public class LayerResourceShould {

    private final LayerResource resource = new LayerResource(mock(LayerStore.class), mock(LiveLayerStore.class));

    @Test
    public void acceptValidFeatureCollection() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.of("5678"), point(), propsWithTimestamp())
        ));

        resource.updateLiveLayer("abc", collection);
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfIdsMissing() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.empty(), point(), propsWithTimestamp())
        ));

        resource.updateLiveLayer("abc", collection);
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfTimestampsMissing() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.of("5678"), point(), propsWithoutTimestamp())
        ));

        resource.updateLiveLayer("abc", collection);
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfTimestampsNonNumeric() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.of("5678"), point(), propsWithInvalidTimestamp())
        ));

        resource.updateLiveLayer("abc", collection);
    }

    @Test(expected = NotAcceptableException.class)
    public void throwIfNonUniqueIds() throws Exception {
        FeatureCollection collection = FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp()),
                Feature.of(Optional.of("1234"), point(), propsWithTimestamp())
        ));

        resource.updateLiveLayer("abc", collection);
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
}
