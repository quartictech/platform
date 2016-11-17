package io.quartic.weyl.resource;

import io.quartic.weyl.core.AttributesStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.AbstractAttributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.LayerId;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.model.AbstractAttributes.EMPTY_ATTRIBUTES;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AttributesStoreQuerierShould {

    private final FeatureStore featureStore = mock(FeatureStore.class);
    private final AttributesStore attributesStore = mock(AttributesStore.class);
    private final AttributesStoreQuerier querier = new AttributesStoreQuerier(featureStore, attributesStore);

    @Test(expected = NotFoundException.class)
    public void throw_if_any_attributes_not_found() throws Exception {
        final EntityId existingId = EntityId.of(LayerId.of("foo"), "123");
        final EntityId missingId = EntityId.of(LayerId.of("bar"), "456");
        when(attributesStore.get(existingId)).thenReturn(EMPTY_ATTRIBUTES);
        when(attributesStore.get(missingId)).thenReturn(null);

        querier.retrieveAttributesOrThrow(newArrayList(existingId, missingId));
    }

    @Test
    public void retrieve_all_attributes_if_all_found() throws Exception {
        final EntityId id = EntityId.of(LayerId.of("foo"), "123");
        final AbstractAttributes attributes = mock(AbstractAttributes.class);
        when(attributesStore.get(id)).thenReturn(attributes);

        assertThat(querier.retrieveAttributesOrThrow(newArrayList(id)).collect(toList()),
                equalTo(newArrayList(attributes)));
    }
}
