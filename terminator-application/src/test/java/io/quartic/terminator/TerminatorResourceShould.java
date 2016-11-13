package io.quartic.terminator;

import com.google.common.collect.ImmutableSet;
import io.quartic.catalogue.api.TerminationId;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Point;
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
    private final CatalogueWatcher catalogue = mock(CatalogueWatcher.class);
    private final TerminatorResource resource = new TerminatorResource(catalogue);

    @Test
    public void emit_collections_for_things_in_catalogue() throws Exception {
        when(catalogue.terminationIds()).thenReturn(ImmutableSet.of(TerminationId.of("abc")));

        TestSubscriber<FeatureCollectionWithTerminationId> subscriber = TestSubscriber.create();
        resource.featureCollections().subscribe(subscriber);
        resource.postToDataset("abc", featureCollection());
        subscriber.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents(),
                contains(FeatureCollectionWithTerminationId.of(TerminationId.of("abc"), featureCollection())));
    }

    @Test(expected = NotFoundException.class)
    public void block_collections_for_things_not_in_catalogue() throws Exception {
        resource.postToDataset("abc", featureCollection());
    }

    @Test
    public void not_emit_collections_from_before_subscription() throws Exception {
        when(catalogue.terminationIds()).thenReturn(ImmutableSet.of(TerminationId.of("abc")));

        TestSubscriber<FeatureCollectionWithTerminationId> subscriber = TestSubscriber.create();
        resource.postToDataset("abc", featureCollection());
        resource.featureCollections().subscribe(subscriber);
        subscriber.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents(), empty());
    }

    private FeatureCollection featureCollection() {
        return FeatureCollection.of(newArrayList(
                Feature.of(Optional.of("456"), Optional.of(Point.of(newArrayList(1.0, 2.0))), emptyMap())));
    }
}
