package io.quartic.weyl.resource;

import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.*;

public class AggregatesResourceShould {

    private final FeatureStoreQuerier querier = mock(FeatureStoreQuerier.class);
    private final HistogramCalculator calculator = mock(HistogramCalculator.class);
    private final AggregatesResource resource = ImmutableAggregatesResource.builder()
            .querier(querier)
            .calculator(calculator)
            .build();

    @Test
    public void run_calculator_if_all_features_found() throws Exception {
        final FeatureId id = FeatureId.of("123");
        final Feature feature = mock(Feature.class);
        when(querier.retrieveFeaturesOrThrow(newArrayList(id))).thenReturn(newArrayList(feature).stream());

        resource.getHistogram(newArrayList(id));

        verify(calculator).calculate(newArrayList(feature));
    }
}
