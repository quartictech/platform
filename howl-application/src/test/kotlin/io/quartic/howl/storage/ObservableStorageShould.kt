package io.quartic.howl.storage

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.howl.api.StorageChange
import rx.observers.TestSubscriber
import java.io.ByteArrayInputStream
import javax.ws.rs.core.MediaType


class ObservableStorageShould {
    private val delegate = mock<Storage>()
    private val storage = ObservableStorage(delegate)

    @org.junit.Test
    fun notify_on_put() {
        whenever(delegate.putData(any(), any(), any())).thenReturn(1L)

        val subscriber = TestSubscriber.create<StorageChange>()
        storage.changes.subscribe(subscriber)
        storage.putData(
                StorageCoords("foo", "bar", "ladispute"),
                MediaType.TEXT_PLAIN,
                ByteArrayInputStream("hello".toByteArray())
        )

        subscriber.assertValue(StorageChange("foo", "ladispute", 1L))
    }

    @org.junit.Test
    fun not_notify_changes_from_before_subscription() {
        val subscriber = TestSubscriber.create<StorageChange>()
        storage.putData(
                StorageCoords("foo", "bar", "ladispute"),
                MediaType.TEXT_PLAIN,
                ByteArrayInputStream("hello".toByteArray())
        )
        storage.changes.subscribe(subscriber)

        subscriber.assertNoValues()
    }

}
