package io.quartic.weyl.core.feature;

import io.quartic.weyl.core.model.AbstractFeature;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.reverse;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FeatureCollectionShould {
    private final Consumer<Collection<? extends AbstractFeature>> backer = mock(Consumer.class);
    FeatureCollection collection = new FeatureCollection(backer);

    @Test
    public void be_empty_by_default() throws Exception {
        assertThat(collection, empty());
        assertThat(newArrayList(collection), equalTo(newArrayList()));
    }

    @Test
    public void create_new_collection_with_elements_on_append() throws Exception {
        final ArrayList<AbstractFeature> featuresToAppend = newArrayList(feature(), feature());

        FeatureCollection newCollection = collection.append(featuresToAppend);

        assertThat(newCollection, hasSize(2));
        assertThat(newCollection, contains(reverse(featuresToAppend).toArray()));
    }

    @Test
    public void create_new_collection_with_concatenated_elements_on_append() throws Exception {
        final ArrayList<AbstractFeature> featuresToAppend = newArrayList(feature(), feature(), feature(), feature());

        FeatureCollection newCollection = collection
                .append(featuresToAppend.subList(0, 2))
                .append(featuresToAppend.subList(2, 4));

        assertThat(newCollection, hasSize(4));
        assertThat(newCollection, contains(reverse(featuresToAppend).toArray()));
    }

    @Test
    public void not_be_affected_by_append() throws Exception {
        final ArrayList<AbstractFeature> featuresToAppend = newArrayList(feature(), feature());

        collection.append(featuresToAppend);

        assertThat(collection, empty());
    }

    @Test
    public void add_appended_features_to_store() throws Exception {
        final ArrayList<AbstractFeature> featuresToAppend = newArrayList(feature(), feature());

        collection.append(featuresToAppend);

        verify(backer).accept(featuresToAppend);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prohibit_modification() throws Exception {
        collection.add(feature());
    }

    private AbstractFeature feature() {
        return mock(AbstractFeature.class);
    }
}
