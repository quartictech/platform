package io.quartic.catalogue;

import com.codahale.metrics.health.HealthCheck;

public class StorageBackendHealthCheck extends HealthCheck{
    private final StorageBackend backend;

    public StorageBackendHealthCheck(StorageBackend backend) {
        this.backend = backend;
    }

    @Override
    protected Result check() throws Exception {
        return backend.healthCheck();
    }
}
