package io.quartic.terminator;

import com.google.common.collect.ImmutableSet;
import io.quartic.catalogue.api.TerminationId;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import org.junit.Test;
import rx.observers.TestSubscriber;

import javax.ws.rs.NotFoundException;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TerminatorResourceShould {
    private static final TerminationId TERMINATION_ID = TerminationId.fromString("abc");
    private final CatalogueWatcher catalogue = mock(CatalogueWatcher.class);
    private final TerminatorResource resource = new TerminatorResource(catalogue);

    @Test
    public void emit_collections_for_things_in_catalogue() throws Exception {
        when(catalogue.terminationIds()).thenReturn(ImmutableSet.of(TERMINATION_ID));

        TestSubscriber<FeatureCollectionWithTerminationId> subscriber = TestSubscriber.create();
        resource.getFeatureCollections().subscribe(subscriber);
        resource.postToDataset(TERMINATION_ID, featureCollection());
        subscriber.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents(),
                contains(new FeatureCollectionWithTerminationId(TERMINATION_ID, featureCollection())));
    }

    @Test(expected = NotFoundException.class)
    public void block_collections_for_things_not_in_catalogue() throws Exception {
        resource.postToDataset(TERMINATION_ID, featureCollection());
    }

    @Test
    public void not_emit_collections_from_before_subscription() throws Exception {
        when(catalogue.terminationIds()).thenReturn(ImmutableSet.of(TERMINATION_ID));

        TestSubscriber<FeatureCollectionWithTerminationId> subscriber = TestSubscriber.create();
        resource.postToDataset(TERMINATION_ID, featureCollection());
        resource.getFeatureCollections().subscribe(subscriber);
        subscriber.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents(), empty());
    }

    private FeatureCollection featureCollection() {
        return FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("456"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())));
    }
}
