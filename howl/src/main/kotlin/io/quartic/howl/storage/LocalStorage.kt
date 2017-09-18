package io.quartic.howl.storage

import io.quartic.howl.storage.Storage.PutResult
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Long.parseLong
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock

class LocalStorage(private val config: Config) : Storage {
    data class Config(val dataDir: String = "./data") : StorageConfig

    private val lock = ReentrantReadWriteLock()
    private val versionCounter = AtomicLong(System.currentTimeMillis())

    override fun getData(coords: StorageCoords, version: Long?): InputStreamWithContentType? {
        try {
            lock.readLock().lock()
            val path = getVersionPath(coords, version)
            if (path != null) {
                val file = path.toFile()
                if (file.exists()) {
                    return InputStreamWithContentType(
                            Files.probeContentType(path) ?: DEFAULT_CONTENT_TYPE,
                            FileInputStream(file)
                    )
                }
            }
        } finally {
            lock.readLock().unlock()
        }

        return null
    }

    override fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): PutResult? {
        coords.path.toFile().mkdirs()
        var tempFile: File? = null
        val version = versionCounter.incrementAndGet()
        try {
            tempFile = File.createTempFile("howl", "partial")
            FileOutputStream(tempFile!!).use { fileOutputStream -> IOUtils.copy(inputStream, fileOutputStream) }

            renameFile(tempFile.toPath(), getVersionPath(coords, version)!!)
        } finally {
            if (tempFile != null) {
                tempFile.delete()
            }
        }
        return PutResult(version)
    }

    private fun getVersionPath(coords: StorageCoords, version: Long?): Path? {
        val readVersion = version ?: getLatestVersion(coords)

        if (readVersion != null) {
            return coords.path.resolve(readVersion.toString())
        } else {
            return null
        }
    }

    private fun getLatestVersion(coords: StorageCoords): Long? {
        val fileNames = coords.path.toFile().list() ?: return null
        return fileNames.map { it -> parseLong(it) }.max()
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
            Files.move(from, to)
        } finally {
            lock.writeLock().unlock()
        }
    }

    companion object {
        private val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}
