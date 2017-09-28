package io.quartic.howl.storage

import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Ephemeral local storage.
 */
class LocalStorage(private val config: Config) : Storage {
    data class Config(val dataDir: String = "./data") : StorageConfig

    private val contentTypes = mutableMapOf<StorageCoords, String>()
    private val lock = ReentrantReadWriteLock()

    override fun getObject(coords: StorageCoords) = lock.readLock().protect {
        val metadata = getMetadataUnsafe(coords)
        if (metadata != null) {
            StorageResult(
                metadata,
                FileInputStream(coords.path.toFile())
            )
        } else {
            null
        }
    }

    override fun getMetadata(coords: StorageCoords) = lock.readLock().protect { getMetadataUnsafe(coords) }

    override fun putObject(contentLength: Int?, contentType: String?, inputStream: InputStream, coords: StorageCoords) {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("howl", "partial")
            FileOutputStream(tempFile!!).use { fileOutputStream -> IOUtils.copy(inputStream, fileOutputStream) }

            commitFile(tempFile.toPath(), coords, contentType)
        } finally {
            if (tempFile != null) {
                tempFile.delete()
            }
        }
    }

    override fun copyObject(source: StorageCoords, dest: StorageCoords) = lock.writeLock().protect {
        if (Files.exists(source.path)) {
            prepareDestinationUnsafe(dest)
            Files.copy(source.path, dest.path)
            contentTypes[dest] = contentTypes[source]
                ?: Files.probeContentType(source.path)
                ?: DEFAULT_CONTENT_TYPE
            getMetadataUnsafe(dest)
        } else {
            null
        }
    }

    private fun commitFile(from: Path, coords: StorageCoords, contentType: String?) {
        lock.writeLock().protect {
            prepareDestinationUnsafe(coords)
            Files.move(from, coords.path)
            contentTypes[coords] = contentType ?: DEFAULT_CONTENT_TYPE
        }
    }

    private fun getMetadataUnsafe(coords: StorageCoords): StorageMetadata? {
        val file = coords.path.toFile()
        val contentType = contentTypes[coords]
        // Testing both is overkill, but whatever
        return if ((contentType != null) && file.exists()) {
            StorageMetadata(
                Files.getLastModifiedTime(coords.path).toInstant(),
                contentType,
                Files.size(coords.path)
            )
        } else {
            null
        }
    }

    private fun prepareDestinationUnsafe(coords: StorageCoords) {
        coords.path.parent.toFile().mkdirs()
        Files.deleteIfExists(coords.path)
    }

    private fun <R> Lock.protect(block: () -> R): R {
        try {
            lock()
            return block()
        } finally {
            unlock()
        }
    }

    private val StorageCoords.path get() = Paths.get(config.dataDir).resolve(backendKey)

    companion object {
        private val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}
