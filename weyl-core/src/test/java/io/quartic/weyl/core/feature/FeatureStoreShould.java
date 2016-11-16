package io.quartic.weyl.core.feature;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import org.junit.Test;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class FeatureStoreShould {
    private final UidGenerator<FeatureId> fidGen = mock(UidGenerator.class);
    private final FeatureStore store = new FeatureStore(fidGen);

    @Test
    public void create_empty_feature_collection() throws Exception {
        assertThat(store.newCollection(), empty());
    }

    @Test
    public void expose_features_appended_to_collection() throws Exception {
        final Feature featureA = feature("42");
        final Feature featureB = feature("43");

        store.newCollection().append(newArrayList(featureA, featureB));

        assertThat(store.get(FeatureId.of("42")), equalTo(featureA));
        assertThat(store.get(FeatureId.of("43")), equalTo(featureB));
    }

    @Test
    public void no_longer_expose_unreferenced_features_via_map() throws Exception {
        store.newCollection().append(newArrayList(feature("42")));
        System.gc();

        // For some reason we can't do something like assertThat(store, empty())
        assertThat(store.get(FeatureId.of("42")), nullValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prohibit_deletion_via_map() throws Exception {
        store.entrySet().remove(feature("42"));
    }

    private Feature feature(String id) {
        // Don't use mocks, because Mockito retains references, which means the weak-reference stuff doesn't happen
        return ImmutableFeature.builder()
                .externalId(id)
                .uid(FeatureId.of(id))
                .geometry(mock(Geometry.class))
                .attributes(mock(Map.class))
                .build();
    }
}
