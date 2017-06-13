package io.quartic.howl.storage

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock

class DiskStorageBackend(private val rootPath: Path) : StorageBackend {
    private val lock = ReentrantReadWriteLock()
    private val versionCounter = AtomicLong(System.currentTimeMillis())

    override fun getData(namespace: String, objectName: String, version: Long?): InputStreamWithContentType? {
        try {
            lock.readLock().lock()
            val path = getVersionPath(namespace, objectName, version)
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

    override fun putData(contentType: String?, namespace: String, objectName: String, inputStream: InputStream): Long? {
        getObjectPath(namespace, objectName).toFile().mkdirs()
        var tempFile: File? = null
        val version = versionCounter.incrementAndGet()
        try {
            tempFile = File.createTempFile("howl", "partial")
            FileOutputStream(tempFile!!).use { fileOutputStream -> IOUtils.copy(inputStream, fileOutputStream) }

            renameFile(tempFile.toPath(), getVersionPath(namespace, objectName, version)!!)
        } finally {
            if (tempFile != null) {
                tempFile.delete()
            }
        }
        return version
    }

    private fun getVersionPath(namespace: String, objectName: String, version: Long?): Path? {
        val readVersion = version ?: getLatestVersion(namespace, objectName)

        if (readVersion != null) {
            return getObjectPath(namespace, objectName).resolve(readVersion.toString())
        } else {
            return null
        }
    }

    private fun getObjectPath(namespace: String, objectName: String): Path {
        return rootPath.resolve(Paths.get(namespace, objectName))
    }

    private fun getLatestVersion(namespace: String, objectName: String): Long? {
        val fileNames = rootPath.resolve(Paths.get(namespace, objectName)).toFile().list()

        if (fileNames != null) {
            val latestVersion = Arrays.stream(fileNames)
                    .mapToLong { java.lang.Long.parseLong(it) }
                    .max()

            if (latestVersion.isPresent) {
                return latestVersion.asLong
            }
        }

        return null
    }

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
