package io.quartic.howl.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface StorageBackend {
    Optional<InputStreamWithContentType> get(String namespace, String objectName) throws IOException;
    void put(String contentType, String namespace, String objectName, InputStream inputStream) throws IOException;
}
