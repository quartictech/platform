package io.quartic.terminator;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CatalogueProxyShould {
    private final CatalogueService catalogue = mock(CatalogueService.class);
    private final CatalogueProxy proxy = CatalogueProxy.builder()
            .catalogue(catalogue)
            .pollPeriodMilliseconds(10)
            .build();

    @Test
    public void poll_catalogue_repeatedly() throws Exception {
        proxy.start();
        Thread.sleep(100);

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

        proxy.start();
        Thread.sleep(100);

        verify(catalogue, atMost(5)).getDatasets(); // The naive approach would just slam it with a bunch of backed-up calls
    }

    @Test
    public void handle_catalogue_errors() throws Exception {
        when(catalogue.getDatasets())
                .thenThrow(new RuntimeException("oops"))
                .thenReturn(emptyMap());

        proxy.start();
        Thread.sleep(100);

        verify(catalogue, atLeast(2)).getDatasets();
    }

    @Test
    public void not_slam_the_catalogue_on_error() throws Exception {
        when(catalogue.getDatasets())
                .thenThrow(new RuntimeException("oops"));

        proxy.start();
        Thread.sleep(100);

        verify(catalogue, atMost(11)).getDatasets();
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
        Thread.sleep(100);

        assertThat(proxy.terminationIds(), Matchers.contains(terminationId));
    }
}
