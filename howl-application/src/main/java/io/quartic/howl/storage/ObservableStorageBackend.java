package io.quartic.howl.storage;

import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class ObservableStorageBackend implements StorageBackend {
    private final StorageBackend delegate;
    private final PublishSubject<StorageBackendChange> changes = PublishSubject.create();

    public ObservableStorageBackend(StorageBackend delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<InputStreamWithContentType> get(String namespace, String objectName, Long version) throws IOException {
        return delegate.get(namespace, objectName, version);
    }

    @Override
    public Long put(String contentType, String namespace, String objectName, InputStream inputStream) throws IOException {
        Long newVersion = delegate.put(contentType, namespace, objectName, inputStream);
        changes.onNext(StorageBackendChangeImpl.of(namespace, objectName, newVersion));
        return newVersion;
    }

    public Observable<StorageBackendChange> changes() {
        return changes;
    }
}
