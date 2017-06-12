package io.quartic.howl.storage

import io.quartic.howl.api.StorageBackendChange
import rx.Observable
import rx.subjects.PublishSubject
import java.io.InputStream

class ObservableStorageBackend(private val delegate: StorageBackend) : StorageBackend by delegate {
    private val _changes = PublishSubject.create<StorageBackendChange>()
    val changes: Observable<StorageBackendChange> get() = _changes

    override fun putData(contentType: String?, namespace: String, objectName: String, inputStream: InputStream): Long? {
        val newVersion = delegate.putData(contentType, namespace, objectName, inputStream)
        _changes.onNext(StorageBackendChange(namespace, objectName, newVersion))
        return newVersion
    }
}
