package io.quartic.tracker.scribe

import com.codahale.metrics.MetricRegistry
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import io.quartic.common.logging.logger
import io.quartic.common.metrics.meter
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class BatchWriter(
        private val storage: Storage,
        private val bucketName: String,
        private val namespace: String,
        metrics: MetricRegistry
) {
    private val LOG by logger()
    private val fileCreationBytesMeter = meter(metrics, "fileCreation", "bytes")
    private val fileCreationSuccessMeter = meter(metrics, "fileCreation", "success")
    private val fileCreationErrorMeter = meter(metrics, "fileCreation", "failure")

    fun write(messages: List<String>, timestamp: Instant, partNumber: Int): Boolean {
        val objectName = "%s/%s-%04d-%s".format(
                namespace,
                DateTimeFormatter.ISO_INSTANT.format(timestamp),
                partNumber,
                UUID.randomUUID().toString().substring(0, 6)
        )

        val blobInfo = BlobInfo.newBuilder(bucketName, objectName).build()  // TODO: MIME type

        try {
            val bytes = messages.joinToString("\n").toByteArray()
            storage.create(blobInfo, bytes)
            fileCreationBytesMeter.mark(bytes.size.toLong())
            fileCreationSuccessMeter.mark()
            return true
        } catch (e: StorageException) {
            LOG.error("Error writing to bucket", e)
            fileCreationErrorMeter.mark()
            return false
        }

        // TODO: verify object is created
    }
}