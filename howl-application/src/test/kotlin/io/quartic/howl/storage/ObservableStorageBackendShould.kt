package io.quartic.howl.storage

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.howl.api.StorageBackendChange
import rx.observers.TestSubscriber
import java.io.ByteArrayInputStream
import javax.ws.rs.core.MediaType


class ObservableStorageBackendShould {
    private val storageBackend = mock<StorageBackend>()
    private val observableStorageBackend = ObservableStorageBackend(storageBackend)

    @org.junit.Test
    fun notify_on_put() {
        whenever(storageBackend.putData(any(), any(), any())).thenReturn(1L)

        val subscriber = TestSubscriber.create<StorageBackendChange>()
        observableStorageBackend.changes.subscribe(subscriber)
        observableStorageBackend.putData(
                StorageCoords("foo", "bar", "ladispute"),
                MediaType.TEXT_PLAIN,
                ByteArrayInputStream("hello".toByteArray())
        )

        subscriber.assertValue(StorageBackendChange("foo", "ladispute", 1L))
    }

    @org.junit.Test
    fun not_notify_changes_from_before_subscription() {
        val subscriber = TestSubscriber.create<StorageBackendChange>()
        observableStorageBackend.putData(
                StorageCoords("foo", "bar", "ladispute"),
                MediaType.TEXT_PLAIN,
                ByteArrayInputStream("hello".toByteArray())
        )
        observableStorageBackend.changes.subscribe(subscriber)

        subscriber.assertNoValues()
    }

}
