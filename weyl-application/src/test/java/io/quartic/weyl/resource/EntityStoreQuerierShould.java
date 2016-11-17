package io.quartic.weyl.resource;

import io.quartic.weyl.core.EntityStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityStoreQuerierShould {

    private final FeatureStore featureStore = mock(FeatureStore.class);
    private final EntityStore entityStore = mock(EntityStore.class);
    private final EntityStoreQuerier querier = new EntityStoreQuerier(featureStore, entityStore);

    @Test(expected = NotFoundException.class)
    public void throw_if_any_attributes_not_found() throws Exception {
        final EntityId existingId = EntityId.of("foo");
        final EntityId missingId = EntityId.of("bar");
        when(entityStore.get(existingId)).thenReturn(mock(AbstractFeature.class));
        when(entityStore.get(missingId)).thenReturn(null);

        querier.retrieveEntitiesOrThrow(newArrayList(existingId, missingId));
    }

    @Test
    public void retrieve_all_attributes_if_all_found() throws Exception {
        final EntityId id = EntityId.of("foo");
        final AbstractFeature feature = mock(AbstractFeature.class);
        when(entityStore.get(id)).thenReturn(feature);

        assertThat(querier.retrieveEntitiesOrThrow(newArrayList(id)).collect(toList()),
                equalTo(newArrayList(feature)));
    }
}
