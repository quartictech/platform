package io.quartic.howl.storage

import io.quartic.howl.storage.Storage.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Ephemeral local storage.
 */
class LocalStorage(private val config: Config) : Storage {
    data class Config(val dataDir: String = "./data") : StorageConfig

    private val contentTypes = mutableMapOf<StorageCoords, String>()
    private val lock = ReentrantReadWriteLock()

    override fun getData(coords: StorageCoords): StorageResult? {
        try {
            lock.readLock().lock()

            val contentType = contentTypes[coords]
            val file = coords.path.toFile()
            // Testing both is overkill, but whatever
            if ((contentType != null) && file.exists()) {
                return StorageResult(
                    StorageMetadata(
                        Files.getLastModifiedTime(coords.path).toInstant(),
                        contentType,
                        Files.size(coords.path)
                    ),
                    FileInputStream(file)
                )
            }
        } finally {
            lock.readLock().unlock()
        }

        return null
    }

    override fun getMetadata(coords: StorageCoords): StorageMetadata? = getData(coords)?.metadata

    override fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean {
        coords.path.toFile().mkdirs()
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
        return true
    }

    private val StorageCoords.path get() = Paths.get(config.dataDir).resolve(
        when (this) {
            is Managed -> Paths.get(targetNamespace, "managed", identityNamespace, objectKey)
            is Unmanaged -> Paths.get(targetNamespace, "unmanaged", objectKey)
        }
    )

    private fun commitFile(from: Path, coords: StorageCoords, contentType: String?) {
        try {
            lock.writeLock().lock()
            Files.deleteIfExists(coords.path)
            Files.move(from, coords.path)
            contentTypes[coords] = contentType ?: DEFAULT_CONTENT_TYPE
        } finally {
            lock.writeLock().unlock()
        }
    }

    companion object {
        private val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}
