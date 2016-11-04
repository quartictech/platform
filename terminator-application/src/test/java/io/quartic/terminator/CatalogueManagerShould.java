package io.quartic.terminator;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static io.quartic.terminator.AbstractDatasetAction.ActionType.ADDED;
import static io.quartic.terminator.AbstractDatasetAction.ActionType.REMOVED;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CatalogueManagerShould {
    private final CatalogueService catalogue = mock(CatalogueService.class);
    private final CatalogueManager manager = CatalogueManager.builder()
            .catalogue(catalogue)
            .pollPeriodMilliseconds(10)
            .build();
    public static final DatasetConfig CONFIG = DatasetConfig.of(
            DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
            mock(DatasetLocator.class)
    );

    @Test
    public void poll_catalogue_repeatedly() throws Exception {
        observe(manager.datasetActions());

        verify(catalogue, atLeast(2)).getDatasets();
    }

    @Test
    public void not_slam_the_catalogue_after_it_blocks_for_a_while() throws Exception {
        final AtomicInteger i = new AtomicInteger();
        when(catalogue.getDatasets()).then(invocation -> {
            if (i.getAndIncrement() == 1) { // Don't delay the very first one, because that delays the TestSubscriber subscription
                Thread.sleep(70);
            }
            return ImmutableMap.of();
        });

        observe(manager.datasetActions());

        verify(catalogue, atMost(5)).getDatasets(); // The naive approach would just slam it with a bunch of backed-up calls
    }

    @Test
    public void handle_catalogue_errors() throws Exception {
        when(catalogue.getDatasets())
                .thenThrow(new RuntimeException("oops"))
                .thenReturn(emptyMap());

        observe(manager.datasetActions());

        verify(catalogue, atLeast(2)).getDatasets();
    }

    @Test
    public void generate_added_actions_for_added_elements() throws Exception {
        when(catalogue.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), CONFIG));

        assertThat(observe(manager.datasetActions(), 1),
                contains(DatasetAction.of(ADDED, DatasetId.of("123"), CONFIG)));
    }

    @Test
    public void generate_removed_actions_for_removed_elements() throws Exception {
        when(catalogue.getDatasets())
                .thenReturn(ImmutableMap.of(DatasetId.of("123"), CONFIG))
                .thenReturn(ImmutableMap.of());

        assertThat(observe(manager.datasetActions(), 2),
                hasItem(DatasetAction.of(REMOVED, DatasetId.of("123"), CONFIG)));
    }

    private <T> List<T> observe(Observable<T> observable, int n) {
        final TestSubscriber<T> subscriber = TestSubscriber.create();
        observable.subscribe(subscriber);
        subscriber.awaitValueCount(n, 100, MILLISECONDS);
        return subscriber.getOnNextEvents();
    }

    private <T> List<T> observe(Observable<T> observable) {
        final TestSubscriber<T> subscriber = TestSubscriber.create();
        observable.subscribe(subscriber);
        subscriber.awaitTerminalEventAndUnsubscribeOnTimeout(100, MILLISECONDS);
        return subscriber.getOnNextEvents();
    }
}
