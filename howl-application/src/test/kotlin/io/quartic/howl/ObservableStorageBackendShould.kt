package io.quartic.howl

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.howl.api.StorageBackendChange
import io.quartic.howl.storage.ObservableStorageBackend
import io.quartic.howl.storage.StorageBackend
import org.junit.Test
import rx.observers.TestSubscriber
import java.io.ByteArrayInputStream
import javax.ws.rs.core.MediaType


class ObservableStorageBackendShould {
    private val storageBackend = mock<StorageBackend>()
    private val observableStorageBackend = ObservableStorageBackend(storageBackend)

    @Test
    fun notify_on_put() {
        whenever(storageBackend.putData(any(), any(), any(), any())).thenReturn(1L)

        val subscriber = TestSubscriber.create<StorageBackendChange>()
        observableStorageBackend.changes.subscribe(subscriber)
        observableStorageBackend.putData(MediaType.TEXT_PLAIN, "test", "ladispute",
                ByteArrayInputStream("hello".toByteArray()))

        subscriber.assertValue(StorageBackendChange("test", "ladispute", 1L))
    }

    @Test
    fun not_notify_changes_from_before_subscription() {
        val subscriber = TestSubscriber.create<StorageBackendChange>()
        observableStorageBackend.putData(MediaType.TEXT_PLAIN, "test", "ladispute",
                ByteArrayInputStream("hello".toByteArray()))
        observableStorageBackend.changes.subscribe(subscriber)

        subscriber.assertNoValues()
    }

}
