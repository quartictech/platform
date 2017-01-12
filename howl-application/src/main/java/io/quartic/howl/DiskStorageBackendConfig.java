package io.quartic.howl;

import io.quartic.howl.storage.DiskStorageBackend;
import io.quartic.howl.storage.StorageBackend;

import java.nio.file.Paths;

public class DiskStorageBackendConfig implements StorageBackendConfig {
    private String dataDir;

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public StorageBackend build() {
        return new DiskStorageBackend(Paths.get(dataDir));
    }
}
