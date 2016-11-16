package io.quartic.weyl.resource;

import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.FeatureId;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeatureStoreQuerierShould {

    private final FeatureStore store = mock(FeatureStore.class);
    private final FeatureStoreQuerier querier = new FeatureStoreQuerier(store);

    @Test(expected = NotFoundException.class)
    public void throw_if_any_feature_not_found() throws Exception {
        final FeatureId existingId = FeatureId.of("123");
        final FeatureId missingId = FeatureId.of("456");
        when(store.get(existingId)).thenReturn(mock(AbstractFeature.class));
        when(store.get(missingId)).thenReturn(null);

        querier.retrieveFeaturesOrThrow(newArrayList(existingId, missingId));
    }

    @Test
    public void retrieve_all_features_if_all_found() throws Exception {
        final FeatureId id = FeatureId.of("123");
        final AbstractFeature feature = mock(AbstractFeature.class);
        when(store.get(id)).thenReturn(feature);

        assertThat(querier.retrieveFeaturesOrThrow(newArrayList(id)).collect(toList()),
                equalTo(newArrayList(feature)));
    }
}
