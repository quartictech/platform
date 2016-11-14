package io.quartic.terminator;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.WebsocketListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class CatalogueWatcherShould {
    private final WebsocketListener<Map<DatasetId, DatasetConfig>> listener = mock(WebsocketListener.class);
    private final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);
    private final CatalogueWatcher proxy = CatalogueWatcher.of(listenerFactory);

    @Before
    public void before() throws Exception {
        when(listenerFactory.create(any(JavaType.class))).thenReturn((WebsocketListener)listener);
    }

    @After
    public void after() throws Exception {
        proxy.close();
    }

    @Test
    public void expose_returned_ids() throws Exception {
        final TerminationId terminationId = TerminationId.of("456");
        final ImmutableMap<DatasetId, DatasetConfig> datasets = datasetsWithLocator(TerminatorDatasetLocator.of(terminationId));
        when(listener.observable()).thenReturn(just(datasets));

        proxy.start();

        assertThat(proxy.terminationIds(), contains(terminationId));
    }

    @Test
    public void ignore_incorrect_types() throws Exception {
        final ImmutableMap<DatasetId, DatasetConfig> datasets = datasetsWithLocator(PostgresDatasetLocator.of("a", "b", "c", "d"));
        when(listener.observable()).thenReturn(just(datasets));

        proxy.start();

        assertThat(proxy.terminationIds(), empty());
    }

    private ImmutableMap<DatasetId, DatasetConfig> datasetsWithLocator(DatasetLocator locator) {
        return ImmutableMap.of(
                DatasetId.of("123"),
                DatasetConfig.of(
                        DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                        locator,
                        emptyMap()
                ));
    }
}
