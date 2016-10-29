package io.quartic.weyl.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.feature.FeatureStore;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ResultIterator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PostgresImporterShould {
    @Test
    public void close_connection_when_done() throws Exception {
        DBI dbi = mock(DBI.class);
        Handle handle = mock(Handle.class, RETURNS_DEEP_STUBS);
        ResultIterator ri = mock(ResultIterator.class);
        when(dbi.open()).thenReturn(handle);
        when(handle.createQuery(anyString()).iterator()).thenReturn(ri);
        when(ri.hasNext()).thenReturn(false);

        FeatureStore featureStore = mock(FeatureStore.class);
        ObjectMapper mapper = new ObjectMapper();

        PostgresImporter importer = new PostgresImporter(dbi, "SELECT * FROM foo", featureStore, mapper);

        importer.get();

        verify(handle).close();
    }
}
