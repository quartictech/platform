package io.quartic.catalogue

import com.codahale.metrics.health.HealthCheck

class StorageBackendHealthCheck(private val backend: StorageBackend) : HealthCheck() {
    override fun check(): HealthCheck.Result = backend.healthCheck()
}
