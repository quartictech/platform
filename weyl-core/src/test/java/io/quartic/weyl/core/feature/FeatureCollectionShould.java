package io.quartic.weyl.core.feature;

import io.quartic.weyl.core.feature.FeatureCollection.Store;
import io.quartic.weyl.core.model.Feature;
import org.junit.Test;

import java.util.ArrayList;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FeatureCollectionShould {
    private final Store store = mock(Store.class);
    FeatureCollection collection = new FeatureCollection(store);

    @Test
    public void be_empty_by_default() throws Exception {
        assertThat(collection, empty());
    }

    @Test
    public void create_new_collection_with_elements_on_append() throws Exception {
        final ArrayList<Feature> featuresToAppend = newArrayList(feature(), feature());

        FeatureCollection newCollection = collection.append(featuresToAppend);

        assertThat(newCollection, hasSize(2));
        assertThat(newCollection, containsInAnyOrder(featuresToAppend.toArray()));
    }

    @Test
    public void create_new_collection_with_concatenated_elements_on_append() throws Exception {
        final ArrayList<Feature> featuresToAppend = newArrayList(feature(), feature(), feature(), feature());

        FeatureCollection newCollection = collection
                .append(featuresToAppend.subList(0, 2))
                .append(featuresToAppend.subList(2, 4));

        assertThat(newCollection, hasSize(4));
        assertThat(newCollection, containsInAnyOrder(featuresToAppend.toArray()));
    }

    @Test
    public void not_be_affected_by_append() throws Exception {
        final ArrayList<Feature> featuresToAppend = newArrayList(feature(), feature());

        collection.append(featuresToAppend);

        assertThat(collection, empty());
    }

    @Test
    public void add_appended_features_to_store() throws Exception {
        final ArrayList<Feature> featuresToAppend = newArrayList(feature(), feature());

        collection.append(featuresToAppend);

        verify(store).addAll(featuresToAppend);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prohibit_modification() throws Exception {
        collection.add(feature());
    }

    private Feature feature() {
        return mock(Feature.class);
    }
}
