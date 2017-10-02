package io.quartic.howl.storage

import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import org.apache.commons.codec.digest.DigestUtils.md5Hex
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
        withTempFile { tmp ->
            FileOutputStream(tmp).use { ostream -> IOUtils.copy(inputStream, ostream) }
            commitFile(tmp.toPath(), coords, contentType)
        }
    }

    override fun copyObject(source: StorageCoords, dest: StorageCoords, oldETag: String?) = lock.writeLock().protect {
        if (Files.exists(source.path)) {
            val sourceETag = getETagUnsafe(source)
            if (sourceETag != oldETag) {
                prepareDestinationUnsafe(dest)
                Files.copy(source.path, dest.path)
                contentTypes[dest] = contentTypes[source]
                    ?: Files.probeContentType(source.path)
                    ?: DEFAULT_CONTENT_TYPE
            }
            getMetadataUnsafe(dest)
        } else {
            null
        }
    }

    private fun <R> withTempFile(block: (File) -> R): R {
        var file: File? = null
        return try {
            file = File.createTempFile("howl", "partial")
            block(file)
        } finally {
            if (file != null) {
                file.delete()
            }
        }
    }

    private fun commitFile(from: Path, coords: StorageCoords, contentType: String?) = lock.writeLock().protect {
        prepareDestinationUnsafe(coords)
        Files.move(from, coords.path)
        contentTypes[coords] = contentType ?: DEFAULT_CONTENT_TYPE
        getMetadataUnsafe(coords)!!
    }

    private fun getMetadataUnsafe(coords: StorageCoords): StorageMetadata? {
        val file = coords.path.toFile()
        val contentType = contentTypes[coords]
        // Testing both is overkill, but whatever
        return if ((contentType != null) && file.exists()) {
            StorageMetadata(contentType, Files.size(coords.path), getETagUnsafe(coords))
        } else {
            null
        }
    }

    private fun getETagUnsafe(coords: StorageCoords) = FileInputStream(coords.path.toFile()).use(::md5Hex)

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
