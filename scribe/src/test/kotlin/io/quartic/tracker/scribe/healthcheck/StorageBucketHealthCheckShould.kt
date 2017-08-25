package io.quartic.tracker.scribe.healthcheck

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.io.IOException

class StorageBucketHealthCheckShould {
    private val storage = mock<Storage>(defaultAnswer = RETURNS_DEEP_STUBS)
    private val healthcheck = StorageBucketHealthCheck(storage, "myBucket")

    @Test
    fun report_healthy_if_bucket_exists_and_accessible() {
        whenever(storage.get("myBucket").exists()).thenReturn(true)

        assertTrue(healthcheck.execute().isHealthy)
    }

    @Test
    fun report_unhealthy_if_bucket_reports_not_existing() {
        whenever(storage.get("myBucket").exists()).thenReturn(false)

        assertFalse(healthcheck.execute().isHealthy)
    }

    @Test
    fun report_unhealthy_if_storage_throws() {
        whenever(storage.get("myBucket")).thenThrow(StorageException(IOException("Bad")))

        assertFalse(healthcheck.execute().isHealthy)
    }
}
