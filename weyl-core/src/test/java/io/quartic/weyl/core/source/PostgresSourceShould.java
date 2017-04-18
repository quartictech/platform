package io.quartic.weyl.core.source;

import io.quartic.catalogue.api.PostgresDatasetLocator;
import io.quartic.weyl.core.attributes.AttributesFactory;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ResultIterator;
import rx.observers.Subscribers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostgresSourceShould {
    @Test
    public void close_connection_when_done() throws Exception {
        DBI dbi = mock(DBI.class);
        Handle handle = mock(Handle.class, RETURNS_DEEP_STUBS);
        ResultIterator ri = mock(ResultIterator.class);
        when(dbi.open()).thenReturn(handle);
        when(handle.createQuery(anyString()).iterator()).thenReturn(ri);
        when(ri.hasNext()).thenReturn(false);

        PostgresSource importer = PostgresSource.builder()
                .name("Budgie")
                .locator(new PostgresDatasetLocator("foo", "bar", "baz", "SELECT * FROM foo"))
                .dbi(dbi)
                .attributesFactory(mock(AttributesFactory.class))
                .build();

        importer.observable().subscribe(Subscribers.empty());

        verify(handle).close();
    }
}
