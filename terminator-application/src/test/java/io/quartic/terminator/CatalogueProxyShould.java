package io.quartic.terminator;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CatalogueProxyShould {
    private static final int SLEEP_PERIOD_MILLISECONDS = 250;
    private static final long POLL_PERIOD_MILLISECONDS = 10;

    private final CatalogueService catalogue = mock(CatalogueService.class);
    private final CatalogueProxy proxy = CatalogueProxy.builder()
            .catalogue(catalogue)
            .pollPeriodMilliseconds(POLL_PERIOD_MILLISECONDS)
            .build();

    @Test
    public void poll_catalogue_repeatedly() throws Exception {
        proxy.start();
        Thread.sleep(SLEEP_PERIOD_MILLISECONDS);

        verify(catalogue, atLeast(2)).getDatasets();
    }

    @Test
    public void not_slam_the_catalogue_after_it_blocks_for_a_while() throws Exception {
        final AtomicInteger i = new AtomicInteger();
        final MinDiffTracker tracker = new MinDiffTracker();

        when(catalogue.getDatasets()).then(invocation -> {
            tracker.tick();
            if (i.getAndIncrement() == 1) { // Don't delay the very first one, because that delays the TestSubscriber subscription
                Thread.sleep(70);
            }
            return ImmutableMap.of();
        });

        proxy.start();
        Thread.sleep(SLEEP_PERIOD_MILLISECONDS);

        assertThat(tracker.min(), greaterThanOrEqualTo(POLL_PERIOD_MILLISECONDS));
    }

    @Test
    public void handle_catalogue_errors() throws Exception {
        when(catalogue.getDatasets())
                .thenThrow(new RuntimeException("oops"))
                .thenReturn(emptyMap());

        proxy.start();
        Thread.sleep(SLEEP_PERIOD_MILLISECONDS);

        verify(catalogue, atLeast(2)).getDatasets();
    }

    @Test
    public void not_slam_the_catalogue_on_error() throws Exception {
        final MinDiffTracker tracker = new MinDiffTracker();

        when(catalogue.getDatasets()).then(invocation -> {
            tracker.tick();
            throw new RuntimeException("oops");
        });

        proxy.start();
        Thread.sleep(SLEEP_PERIOD_MILLISECONDS);

        assertThat(tracker.min(), greaterThanOrEqualTo(POLL_PERIOD_MILLISECONDS));
    }

    @Test
    public void expose_returned_ids() throws Exception {
        final TerminationId terminationId = TerminationId.of("456");
        final ImmutableMap<DatasetId, DatasetConfig> datasets = ImmutableMap.of(
                DatasetId.of("123"),
                DatasetConfig.of(
                        DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                        TerminatorDatasetLocator.of(terminationId)
                ));
        when(catalogue.getDatasets()).thenReturn(datasets);

        proxy.start();
        Thread.sleep(SLEEP_PERIOD_MILLISECONDS);

        assertThat(proxy.terminationIds(), Matchers.contains(terminationId));
    }

    public static class MinDiffTracker {
        private final AtomicLong prev = new AtomicLong();
        private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

        public void tick() {
            final long now = System.currentTimeMillis();
            final long diff = now - prev.get();
            prev.set(now);
            min.set((diff < min.get()) ? diff : min.get());
        }

        public long min() {
            return min.get();
        }
    }
}
