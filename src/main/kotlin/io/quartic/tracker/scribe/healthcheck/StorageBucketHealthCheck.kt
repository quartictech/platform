package io.quartic.tracker.scribe.healthcheck

import com.codahale.metrics.health.HealthCheck
import com.google.cloud.storage.Storage

class StorageBucketHealthCheck(
        private val storage: Storage,
        private val bucketName: String
) : HealthCheck() {
    override fun check(): Result = if (storage.get(bucketName).exists()) {
        Result.healthy()
    } else {
        Result.unhealthy("Bucket '$bucketName' does not exist or is inaccessible")
    }
}