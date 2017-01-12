package io.quartic.howl;

import io.quartic.howl.storage.GcsStorageBackend;
import io.quartic.howl.storage.StorageBackend;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GcsStorageBackendConfig implements StorageBackendConfig {
    private String bucketName;

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public StorageBackend build() {
        try {
            return new GcsStorageBackend(bucketName);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("can't construct google cloud storage backend: " + e);
        }
    }
}
