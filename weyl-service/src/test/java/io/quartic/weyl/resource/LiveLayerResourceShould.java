package io.quartic.weyl.resource;

import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import io.quartic.weyl.core.live.LiveLayerStore;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LiveLayerResourceShould {

    @Test
    public void returnFeaturesForLayer() throws Exception {
        final String layerId = "foo";
        final AbstractFeatureCollection features = mock(AbstractFeatureCollection.class);
        final LiveLayerStore store = mock(LiveLayerStore.class);
        when(store.getFeaturesForLayer(layerId)).thenReturn(Optional.of(features));

        LiveLayerResource resource = new LiveLayerResource(store);

        assertThat(resource.getLayer(layerId), equalTo(features));
    }

    @Test(expected = NotFoundException.class)
    public void throwIfLayerNotFound() throws Exception {
        final String layerId = "foo";
        final LiveLayerStore store = mock(LiveLayerStore.class);
        when(store.getFeaturesForLayer(layerId)).thenReturn(Optional.empty());

        LiveLayerResource resource = new LiveLayerResource(store);

        resource.getLayer(layerId);
    }
}
