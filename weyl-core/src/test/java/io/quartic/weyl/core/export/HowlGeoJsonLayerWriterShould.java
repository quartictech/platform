package io.quartic.weyl.core.export;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.howl.api.HowlClient;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.*;
import org.junit.Test;
import org.mockito.internal.verification.Only;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HowlGeoJsonLayerWriterShould {
    private HowlClient howlClient = mock(HowlClient.class);
    private AttributesFactory attributesFactory = new AttributesFactory();
    private HowlGeoJsonLayerWriter layerWriter = new HowlGeoJsonLayerWriter(howlClient,
            new FeatureConverter(attributesFactory));

    @Test
    public void write_to_howl() throws IOException {
        Layer layer = layer("foo", featureCollection(feature("wat")));
        layerWriter.write(layer);
        verify(howlClient, new Only()).uploadFile(any(), any(), any());
    }

    private FeatureCollection featureCollection(Feature... features) {
       return FeatureCollection.EMPTY_COLLECTION.append(ImmutableList.copyOf(features));
    }

    private Feature feature(String id){
        return FeatureImpl.of(EntityId.fromString(id), new GeometryFactory().createPoint(new Coordinate(0, 0)),
                attributesFactory.builder().put("foo", 1).build());
    }


    private Layer layer(String layerId, FeatureCollection features) {
        return LayerImpl.of(
                mock(LayerSpec.class, RETURNS_DEEP_STUBS),
                features,
                mock(DynamicSchema.class),
                mock(SpatialIndex.class),
                ImmutableList.of(),
                mock(LayerStats.class));
    }
}
