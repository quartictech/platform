package io.quartic.weyl.resource;

import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.*;

public class AggregatesResourceShould {

    private final EntityStoreQuerier querier = mock(EntityStoreQuerier.class);
    private final HistogramCalculator calculator = mock(HistogramCalculator.class);
    private final AggregatesResource resource = ImmutableAggregatesResource.builder()
            .querier(querier)
            .calculator(calculator)
            .build();

    @Test
    public void run_calculator_if_all_features_found() throws Exception {
        final EntityId id = EntityId.of("abc");
        final AbstractFeature feature = mock(AbstractFeature.class);
        when(querier.retrieveEntitiesOrThrow(newArrayList(id))).thenReturn(newArrayList(feature).stream());

        resource.getHistogram(newArrayList(id));

        verify(calculator).calculate(newArrayList(feature));
    }
}
