package io.quartic.howl.storage

import io.quartic.howl.api.StorageChange
import rx.Observable
import rx.subjects.PublishSubject
import java.io.InputStream

class ObservableStorage(private val delegate: Storage) : Storage by delegate {
    private val _changes = PublishSubject.create<StorageChange>()
    val changes: Observable<StorageChange> get() = _changes

    override fun putData(coords: StorageCoords, contentType: String?, inputStream: InputStream): Long? {
        val newVersion = delegate.putData(coords, contentType, inputStream)
        _changes.onNext(StorageChange(coords.targetNamespace, coords.objectName, newVersion))    // TODO: how to handle namespaces for change watch?
        return newVersion
    }
}
