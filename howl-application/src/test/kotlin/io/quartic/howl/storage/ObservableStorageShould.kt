package io.quartic.howl.storage

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.howl.api.StorageChange
import org.junit.Test
import rx.observers.TestSubscriber
import java.io.ByteArrayInputStream
import javax.ws.rs.core.MediaType


class ObservableStorageShould {
    private val delegate = mock<Storage>()
    private val storage = ObservableStorage(delegate)

    @Test
    fun notify_on_put() {
        whenever(delegate.putData(any(), any(), any())).thenReturn(Storage.PutResult(1L))

        val subscriber = TestSubscriber.create<StorageChange>()
        storage.changes.subscribe(subscriber)
        putData()

        subscriber.assertValue(StorageChange("foo", "ladispute", 1L))
    }

    @Test
    fun not_notify_if_put_returns_null() {
        whenever(delegate.putData(any(), any(), any())).thenReturn(null)

        val subscriber = TestSubscriber.create<StorageChange>()
        storage.changes.subscribe(subscriber)
        putData()

        subscriber.assertNoValues()
    }

    @Test
    fun not_notify_changes_from_before_subscription() {
        whenever(delegate.putData(any(), any(), any())).thenReturn(Storage.PutResult(1L))

        val subscriber = TestSubscriber.create<StorageChange>()
        putData()
        storage.changes.subscribe(subscriber)

        subscriber.assertNoValues()
    }

    private fun putData() {
        storage.putData(
                StorageCoords("foo", "bar", "ladispute"),
                MediaType.TEXT_PLAIN,
                ByteArrayInputStream("hello".toByteArray())
        )
    }
}
