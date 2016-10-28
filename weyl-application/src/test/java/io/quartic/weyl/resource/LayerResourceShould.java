package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.service.WebsocketImporterService;
import io.quartic.weyl.request.LayerUpdateRequest;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LayerResourceShould {
    private final LayerStore layerStore = mock(LayerStore.class);
    private final WebsocketImporterService websocketImporterService = mock(WebsocketImporterService.class);
    private final LayerResource resource = new LayerResource(layerStore, websocketImporterService);

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
    public void create_websocket_importer() throws URISyntaxException {
        resource.updateLiveLayer("666", createRequest());
        verify(websocketImporterService).start(new URI("ws://nowhere"), LayerId.of("666"));
    }

    private ImmutableMap<String, Object> propsWithTimestamp() {
        return ImmutableMap.of("timestamp", 12345);
    }

    private Optional<Point> point() {
        return Optional.of(Point.of(ImmutableList.of(1.0, 2.0)));
    }

    private LayerUpdateRequest createRequest() {
        return LayerUpdateRequest.of(
                LayerMetadata.of("foo", "bar", Optional.empty(), Optional.empty()),
                LayerViewType.LOCATION_AND_TRACK,
                "ws://nowhere");
    }
}
