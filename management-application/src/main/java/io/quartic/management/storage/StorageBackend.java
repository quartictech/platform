package io.quartic.management.storage;

import io.quartic.management.InputStreamWithContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface StorageBackend {
    Optional<InputStreamWithContentType> get(String objectName) throws IOException;
    void put(String contentType, String objectName, InputStream inputStream) throws IOException;
}
