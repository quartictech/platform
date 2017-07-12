package io.quartic.howl.storage

import io.quartic.howl.api.StorageBackendChange
import rx.Observable
import rx.subjects.PublishSubject
import java.io.InputStream

class ObservableStorageBackend(private val delegate: StorageBackend) : StorageBackend by delegate {
    private val _changes = PublishSubject.create<StorageBackendChange>()
    val changes: Observable<StorageBackendChange> get() = _changes

    override fun putData(coords: StorageCoords, contentType: String?, inputStream: InputStream): Long? {
        val newVersion = delegate.putData(coords, contentType, inputStream)
        _changes.onNext(StorageBackendChange(coords.targetNamespace, coords.objectName, newVersion))    // TODO: how to handle namespaces for change watch?
        return newVersion
    }
}
