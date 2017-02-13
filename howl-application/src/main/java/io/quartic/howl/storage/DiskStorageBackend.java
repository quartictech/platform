package io.quartic.howl.storage;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DiskStorageBackend implements StorageBackend {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private final Path rootPath;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public DiskStorageBackend(Path rootPath) {
       this.rootPath = rootPath;
    }

    private Optional<Path> getFilePath(String namespace, String objectName, Long version) {
        Optional<Long> readVersion = version == null ?
                getLatestVersion(namespace, objectName) : Optional.of(version);

        return readVersion.map(v -> rootPath.resolve(Paths.get(namespace, objectName, v.toString())));
    }

    private Optional<Long> getLatestVersion(String namespace, String objectName) {
        String[] fileNames = rootPath.resolve(Paths.get(namespace, objectName)).toFile().list();

        if (fileNames != null) {
            OptionalLong latestVersion =  Arrays.stream(fileNames)
                    .mapToLong(Long::parseLong)
                    .max();

            if (latestVersion.isPresent()) {
                return Optional.of(latestVersion.getAsLong());
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputStreamWithContentType> get(String namespace, String objectName, Long version) throws IOException {
        try {
            lock.readLock().lock();
            Optional<Path> path = getFilePath(namespace, objectName, version);

            if (path.isPresent()) {
                String contentType = Optional.ofNullable(Files.probeContentType(path.get())).orElse(DEFAULT_CONTENT_TYPE);
                File file = path.get().toFile();

                if (file.exists()) {
                    return Optional.of(InputStreamWithContentTypeImpl.of(contentType, new FileInputStream(file)));
                }
            }
        }
        finally {
            lock.readLock().unlock();
        }

        return Optional.empty();
    }

    @Override
    public Long put(String contentType, String namespace, String objectName, InputStream inputStream) throws IOException {
        Long version = System.currentTimeMillis();
        Path path = getFilePath(namespace, objectName, version).get();
        path.getParent().toFile().mkdirs();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("howl", "partial");
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                IOUtils.copy(inputStream, fileOutputStream);
            }

            lock.writeLock().lock();
            Files.move(tempFile.toPath(), path);
        }
        finally {
            lock.writeLock().unlock();
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        return version;
    }
}
