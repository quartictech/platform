package io.quartic.weyl.core.live;

import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.Geometry;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.SourceUpdateImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class LiveEventConverterShould {
    private final FeatureConverter featureConverter = mock(FeatureConverter.class);
    private final LiveEventConverter converter = new LiveEventConverter(featureConverter);

    @Test
    public void convert_features() throws Exception {
        final Feature featureA = mock(Feature.class);
        final Feature featureB = mock(Feature.class);
        final NakedFeature modelFeatureA = mock(NakedFeature.class);
        final NakedFeature modelFeatureB = mock(NakedFeature.class);
        Mockito.<Optional<? extends Geometry>>when(featureA.geometry()).thenReturn(Optional.of(mock(Geometry.class)));
        Mockito.<Optional<? extends Geometry>>when(featureB.geometry()).thenReturn(Optional.of(mock(Geometry.class)));
        when(featureConverter.toModel(featureA)).thenReturn(modelFeatureA);
        when(featureConverter.toModel(featureB)).thenReturn(modelFeatureB);

        final SourceUpdate update = converter.updateFrom(featureCollectionOf(featureA, featureB));

        verify(featureConverter).toModel(featureA);
        verify(featureConverter).toModel(featureB);
        assertThat(update, equalTo(SourceUpdateImpl.of(newArrayList(modelFeatureA, modelFeatureB))));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        final Feature feature = mock(Feature.class);
        when(feature.geometry()).thenReturn(Optional.empty());

        final SourceUpdate update = converter.updateFrom(featureCollectionOf(feature));

        verifyZeroInteractions(featureConverter);
        assertThat(update, equalTo(SourceUpdateImpl.of(newArrayList())));
    }

    private FeatureCollection featureCollectionOf(Feature... features) {
        return FeatureCollectionImpl.of(newArrayList(features));
    }
}
