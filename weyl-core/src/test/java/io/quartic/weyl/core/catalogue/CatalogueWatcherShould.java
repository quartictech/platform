package io.quartic.weyl.core.catalogue;

import com.fasterxml.jackson.databind.JavaType;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.common.client.WebsocketListener;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static io.quartic.common.test.CollectionUtils.entry;
import static io.quartic.common.test.CollectionUtils.map;
import static io.quartic.common.test.rx.RxUtils.all;
import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.CREATE;
import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.DELETE;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class CatalogueWatcherShould {

    private final WebsocketListener<Map<DatasetId, DatasetConfig>> listener = mock(WebsocketListener.class);
    private final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);

    private final CatalogueWatcher watcher = CatalogueWatcherImpl.of(listenerFactory);

    @Before
    public void before() throws Exception {
        when(listenerFactory.create(any(JavaType.class))).thenReturn((WebsocketListener)listener);
    }

    @Test
    public void emit_creation_event_when_dataset_appears() throws Exception {
        final DatasetId id = mock(DatasetId.class);
        final DatasetConfig config = mock(DatasetConfig.class);

        when(listener.observable()).thenReturn(just(map(entry(id, config))));

        assertThat(all(watcher.events()), contains(CatalogueEventImpl.of(CREATE, id, config)));
    }

    @Test
    public void emit_deletion_event_when_dataset_appears() throws Exception {
        final DatasetId id = mock(DatasetId.class);
        final DatasetConfig config = mock(DatasetConfig.class);

        when(listener.observable()).thenReturn(just(
                map(entry(id, config)),
                map()   // Gone!
        ));

        assertThat(all(watcher.events()), contains(
                CatalogueEventImpl.of(CREATE, id, config),
                CatalogueEventImpl.of(DELETE, id, config)
        ));
    }

    @Test
    public void not_emit_creation_event_when_dataset_persists() throws Exception {
        final DatasetId id = mock(DatasetId.class);
        final DatasetConfig config = mock(DatasetConfig.class);

        when(listener.observable()).thenReturn(just(
                map(entry(id, config)),
                map(entry(id, config))  // Again
        ));

        assertThat(all(watcher.events()), contains(CatalogueEventImpl.of(CREATE, id, config)));
    }

    @Test
    public void emit_events_for_multiple_independent_datasets() throws Exception {
        final DatasetId idA = mock(DatasetId.class);
        final DatasetId idB = mock(DatasetId.class);
        final DatasetConfig configA = mock(DatasetConfig.class);
        final DatasetConfig configB = mock(DatasetConfig.class);

        when(listener.observable()).thenReturn(just(
                map(entry(idA, configA)),
                map(entry(idA, configA), entry(idB, configB)),
                map(entry(idB, configB))
        ));

        assertThat(all(watcher.events()), contains(
                CatalogueEventImpl.of(CREATE, idA, configA),
                CatalogueEventImpl.of(CREATE, idB, configB),
                CatalogueEventImpl.of(DELETE, idA, configA)
        ));
    }
}
