package io.quartic.howl.storage

import com.nhaarman.mockito_kotlin.any


class ObservableStorageBackendShould {
    private val storageBackend = com.nhaarman.mockito_kotlin.mock<StorageBackend>()
    private val observableStorageBackend = io.quartic.howl.storage.ObservableStorageBackend(storageBackend)

    @org.junit.Test
    fun notify_on_put() {
        com.nhaarman.mockito_kotlin.whenever(storageBackend.putData(com.nhaarman.mockito_kotlin.any(), any(), any(), any())).thenReturn(1L)

        val subscriber = rx.observers.TestSubscriber.create<io.quartic.howl.api.StorageBackendChange>()
        observableStorageBackend.changes.subscribe(subscriber)
        observableStorageBackend.putData(javax.ws.rs.core.MediaType.TEXT_PLAIN, "test", "ladispute",
                java.io.ByteArrayInputStream("hello".toByteArray()))

        subscriber.assertValue(io.quartic.howl.api.StorageBackendChange("test", "ladispute", 1L))
    }

    @org.junit.Test
    fun not_notify_changes_from_before_subscription() {
        val subscriber = rx.observers.TestSubscriber.create<io.quartic.howl.api.StorageBackendChange>()
        observableStorageBackend.putData(javax.ws.rs.core.MediaType.TEXT_PLAIN, "test", "ladispute",
                java.io.ByteArrayInputStream("hello".toByteArray()))
        observableStorageBackend.changes.subscribe(subscriber)

        subscriber.assertNoValues()
    }

}
