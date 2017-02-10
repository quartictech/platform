package io.quartic.howl;

import io.quartic.howl.api.StorageBackendChangeImpl;
import io.quartic.howl.storage.ObservableStorageBackend;
import io.quartic.howl.storage.StorageBackend;
import io.quartic.howl.api.StorageBackendChange;
import org.junit.Test;
import rx.observers.TestSubscriber;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObservableStorageBackendShould {
    private StorageBackend storageBackend = mock(StorageBackend.class);
    private ObservableStorageBackend observableStorageBackend = new ObservableStorageBackend(storageBackend);

    @Test
    public void notify_on_put() throws IOException {
        when(storageBackend.put(any(), any(), any(), any())).thenAnswer(invocation -> 1L);

        TestSubscriber<StorageBackendChange> subscriber = TestSubscriber.create();
        observableStorageBackend.changes().subscribe(subscriber);
        observableStorageBackend.put(MediaType.TEXT_PLAIN, "test", "ladispute",
                new ByteArrayInputStream("hello".getBytes()));

        subscriber.assertValue(StorageBackendChangeImpl.of("test", "ladispute", 1L));
    }

    @Test
    public void not_notify_changes_from_before_subscription() throws IOException {
        TestSubscriber<StorageBackendChange> subscriber = TestSubscriber.create();
        observableStorageBackend.put(MediaType.TEXT_PLAIN, "test", "ladispute",
                new ByteArrayInputStream("hello".getBytes()));
        observableStorageBackend.changes().subscribe(subscriber);
        subscriber.assertNoValues();
    }

}
