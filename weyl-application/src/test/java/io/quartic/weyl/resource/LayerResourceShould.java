package io.quartic.weyl.resource;

import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.live.LayerView;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerImpl;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerStats;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayerResourceShould {
    @Test
    public void not_list_empty_layers() throws Exception {
        final Layer populatedLayer = layer("foo", mock(Feature.class));
        final Layer emptyLayer = layer("goo");
        final LayerStore store = mock(LayerStore.class);
        when(store.listLayers()).thenReturn(newArrayList(populatedLayer, emptyLayer));

        final LayerResource resource = new LayerResource(store);

        assertThat(resource.listLayers("oo"), hasSize(1));
    }

    private Layer layer(String name, Feature... features) {
        final LayerMetadata metadata = mock(LayerMetadata.class);
        when(metadata.name()).thenReturn(name);
        return LayerImpl.of(
                LayerSpecImpl.of(
                        mock(LayerId.class),
                        metadata,
                        mock(LayerView.class),
                        mock(AttributeSchema.class),
                        false
                ),
                EMPTY_COLLECTION.append(asList(features)),
                mock(SpatialIndex.class),
                emptyList(),
                mock(LayerStats.class)
        );
    }
}
