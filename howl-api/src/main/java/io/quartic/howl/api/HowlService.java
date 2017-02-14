package io.quartic.howl.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

public interface HowlService {
    void uploadFile(String contentType, String namespace, String fileName, Consumer<OutputStream> upload) throws IOException;
    HowlStorageId uploadFile(String contentType, String namespace, Consumer<OutputStream> upload) throws IOException;
    Optional<InputStream> downloadFile(String namespace, String fileName) throws IOException;
}
