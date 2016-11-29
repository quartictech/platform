package io.quartic.howl.storage;

import com.google.api.client.util.Maps;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public class InMemoryStorageBackend implements StorageBackend {
    private final Map<String, byte[]> data = Maps.newHashMap();
    private final Map<String, String> contentTypes = Maps.newHashMap();

    @Override
    public synchronized Optional<InputStreamWithContentType> get(String namespace, String objectName) throws IOException {
        return Optional.ofNullable(data.get(namespace + "/" + objectName))
                .map( data -> InputStreamWithContentTypeImpl.of(contentTypes.get(objectName),
                        new ByteArrayInputStream(data)));
    }

    @Override
    public synchronized void put(String contentType, String namespace,
                                 String objectName, InputStream inputStream) throws IOException {
        data.put(namespace + "/" + objectName,  IOUtils.toByteArray(inputStream));
        contentTypes.put(objectName, contentType);
    }
}