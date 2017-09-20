package io.quartic.howl.storage

import io.quartic.howl.storage.Storage.*
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

class LocalStorage(private val config: Config) : Storage {
    data class Config(val dataDir: String = "./data") : StorageConfig

    private val lock = ReentrantReadWriteLock()

    override fun getData(coords: StorageCoords): StorageResult? {
        try {
            lock.readLock().lock()
            val file = coords.path.toFile()
            if (file.exists()) {
                return StorageResult(
                    StorageMetadata(
                        Files.getLastModifiedTime(coords.path).toInstant(),
                        Files.probeContentType(coords.path) ?: DEFAULT_CONTENT_TYPE,
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

            renameFile(tempFile.toPath(), coords.path)
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

    private fun renameFile(from: Path, to: Path) {
        try {
            lock.writeLock().lock()
            Files.deleteIfExists(to)
            Files.move(from, to)
        } finally {
            lock.writeLock().unlock()
        }
    }

    companion object {
        private val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}
