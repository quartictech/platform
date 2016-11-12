package io.quartic.terminator;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.common.client.WebsocketListener;
import org.junit.After;
import org.junit.Test;

import java.util.Optional;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class CatalogueWatcherShould {
    private final WebsocketListener listener = mock(WebsocketListener.class);
    private final CatalogueWatcher proxy = CatalogueWatcher.builder()
            .catalogueApiRoot("XXX")
            .objectMapper(OBJECT_MAPPER)
            .listener(listener)
            .websocketFactory(mock(WebsocketClientSessionFactory.class))
            .build();

    @After
    public void after() throws Exception {
        proxy.close();
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
        when(listener.observable()).thenReturn(just(OBJECT_MAPPER.writeValueAsString(datasets)));

        proxy.start();

        assertThat(proxy.terminationIds(), contains(terminationId));
    }

    @Test
    public void ignore_incorrect_types() throws Exception {
        final ImmutableMap<DatasetId, DatasetConfig> datasets = ImmutableMap.of(
                DatasetId.of("123"),
                DatasetConfig.of(
                        DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                        mock(DatasetLocator.class)
                ));
        when(listener.observable()).thenReturn(just(OBJECT_MAPPER.writeValueAsString(datasets)));

        proxy.start();

        assertThat(proxy.terminationIds(), empty());
    }
}
