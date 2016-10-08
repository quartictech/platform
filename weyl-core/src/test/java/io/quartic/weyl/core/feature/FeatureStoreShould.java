package io.quartic.weyl.core.feature;

import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeatureStoreShould {
    private final FeatureStore store = new FeatureStore();

    @Test
    public void expose_features_through_immutable_collection() throws Exception {
        assertThatFeaturesAreExposed(store::createImmutableCollection);
    }

    @Test
    public void expose_features_through_mutable_collection() throws Exception {
        assertThatFeaturesAreExposed(store::createMutableCollection);
    }

    private void assertThatFeaturesAreExposed(Function<List<Feature>, Collection<Feature>> create) {
        final List<Feature> features = newArrayList(feature("42"), feature("43"));

        Collection<Feature> collection = create.apply(features);

        assertThat(collection, containsInAnyOrder(features.toArray()));
    }

    @Test
    public void take_copy_of_the_initialiser_for_immutable_collection() throws Exception {
        assertThatCopyIsCreated(store::createImmutableCollection);
    }

    @Test
    public void take_copy_of_the_initialiser_for_mutable_collection() throws Exception {
        assertThatCopyIsCreated(store::createMutableCollection);
    }

    private void assertThatCopyIsCreated(Function<List<Feature>, Collection<Feature>> create) {
        final List<Feature> originals = newArrayList(feature("42"), feature("43"));
        final List<Feature> features = newArrayList(originals);

        Collection<Feature> collection = create.apply(features);
        features.add(feature("44"));

        assertThat(collection, containsInAnyOrder(originals.toArray()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void not_allow_changes_to_immutable_collection() throws Exception {
        Collection<Feature> collection = store.createImmutableCollection(newArrayList());
        collection.add(feature("42"));
    }

    @Test
    public void allow_changes_to_mutable_collection() throws Exception {
        Collection<Feature> collection = store.createMutableCollection(newArrayList());
        collection.add(feature("42"));
    }

    @Test
    public void not_expose_features_of_one_collection_via_another_collection() throws Exception {
        final List<Feature> featuresA = newArrayList(feature("42"), feature("43"));
        final List<Feature> featuresB = newArrayList(feature("44"), feature("45"));

        Collection<Feature> collectionA = store.createImmutableCollection(featuresA);
        Collection<Feature> collectionB = store.createImmutableCollection(featuresB);

        assertThat(collectionA, containsInAnyOrder(featuresA.toArray()));
        assertThat(collectionB, containsInAnyOrder(featuresB.toArray()));
    }

    @Test
    public void expose_features_from_all_collections_as_map() throws Exception {
        final Feature featureA = feature("42");
        final Feature featureB = feature("43");
        final Feature featureC = feature("44");
        final Feature featureD = feature("45");

        store.createImmutableCollection(newArrayList(featureA, featureB));
        store.createImmutableCollection(newArrayList(featureC, featureD));

        assertThat(store.get(FeatureId.of("42")), equalTo(featureA));
        assertThat(store.get(FeatureId.of("43")), equalTo(featureB));
        assertThat(store.get(FeatureId.of("44")), equalTo(featureC));
        assertThat(store.get(FeatureId.of("45")), equalTo(featureD));
    }

    @Test
    public void expose_changes_to_mutable_collection_via_map() throws Exception {
        final Feature featureA = feature("42");
        final Feature featureB = feature("43");
        final Feature featureC = feature("44");

        final Collection<Feature> collection = store.createMutableCollection(newArrayList(featureA, featureB));
        collection.add(featureC);

        assertThat(store.values(), containsInAnyOrder(featureA, featureB, featureC));
    }

    private Feature feature(String id) {
        Feature feature = mock(Feature.class);
        when(feature.uid()).thenReturn(FeatureId.of(id));
        return feature;
    }
}
