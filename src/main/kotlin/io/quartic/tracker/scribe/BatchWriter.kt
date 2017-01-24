package io.quartic.tracker.scribe

import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import io.quartic.common.logging.logger
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class BatchWriter(
        private val storage: Storage,
        private val bucketName: String,
        private val namespace: String
) {
    private val LOG by logger()

    fun write(messages: List<String>, timestamp: Instant, partNumber: Int): Boolean {
        val objectName = "%s/%s-%04d-%s".format(
                namespace,
                DateTimeFormatter.ISO_INSTANT.format(timestamp),
                partNumber,
                UUID.randomUUID().toString().substring(0, 6)
        )

        val blobInfo = BlobInfo.newBuilder(bucketName, objectName).build()  // TODO: MIME type

        try {
            storage.create(blobInfo, messages.joinToString("\n").toByteArray())
            return true
        } catch (e: StorageException) {
            LOG.error("Error writing to bucket", e)
            return false
        }

        // TODO: verify object is created
    }
}