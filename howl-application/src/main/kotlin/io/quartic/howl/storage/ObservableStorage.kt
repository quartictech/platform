package io.quartic.howl.storage

import io.quartic.howl.api.StorageChange
import io.quartic.howl.storage.Storage.PutResult
import rx.Observable
import rx.subjects.PublishSubject
import java.io.InputStream

class ObservableStorage(private val delegate: Storage) : Storage by delegate {
    private val _changes = PublishSubject.create<StorageChange>()
    val changes: Observable<StorageChange> get() = _changes

    override fun putData(coords: StorageCoords, contentType: String?, inputStream: InputStream): PutResult? {
        val result = delegate.putData(coords, contentType, inputStream)
        if (result != null) {
            _changes.onNext(StorageChange(coords.targetNamespace, coords.objectName, result.version))    // TODO: how to handle namespaces for change watch?
        }
        return result
    }
}
