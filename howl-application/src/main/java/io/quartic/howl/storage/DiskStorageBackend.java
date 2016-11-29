package io.quartic.howl.storage;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DiskStorageBackend implements StorageBackend {
    private final Path rootPath;

    public DiskStorageBackend(Path rootPath) {
       this.rootPath = rootPath;
    }

    @Override
    public Optional<InputStreamWithContentType> get(String namespace, String objectName) throws IOException {
        Path path = rootPath.resolve(Paths.get(namespace, objectName));
        String contentType = Files.probeContentType(path);
        if (path.toFile().exists()) {
            return Optional.of(InputStreamWithContentTypeImpl.of(contentType, new FileInputStream(path.toFile())));
        }

        return Optional.empty();
    }

    @Override
    public void put(String contentType, String namespace, String objectName, InputStream inputStream) throws IOException {
        Path dirPath = rootPath.resolve(namespace);
        dirPath.toFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(dirPath.resolve(objectName).toFile());
        IOUtils.copy(inputStream, fileOutputStream);
    }
}
