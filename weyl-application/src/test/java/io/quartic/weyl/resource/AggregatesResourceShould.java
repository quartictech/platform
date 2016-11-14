package io.quartic.weyl.resource;

import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AggregatesResourceShould {

    private final FeatureStore store = mock(FeatureStore.class);
    private final HistogramCalculator calculator = mock(HistogramCalculator.class);
    private final AggregatesResource resource = ImmutableAggregatesResource.builder()
            .featureStore(store)
            .calculator(calculator)
            .build();

    @Test(expected = NotFoundException.class)
    public void respond_with_404_if_any_feature_not_found() throws Exception {
        final FeatureId existingId = FeatureId.of("123");
        final FeatureId missingId = FeatureId.of("456");

        when(store.get(existingId)).thenReturn(mock(Feature.class));
        when(store.get(missingId)).thenReturn(null);

        resource.getHistogram(newArrayList(existingId, missingId));
    }

    @Test
    public void run_calculator_if_all_features_found() throws Exception {
        final FeatureId id = FeatureId.of("123");
        final Feature feature = mock(Feature.class);
        when(store.get(id)).thenReturn(feature);

        resource.getHistogram(newArrayList(id));

        verify(calculator).calculate(newArrayList(feature));
    }
}
