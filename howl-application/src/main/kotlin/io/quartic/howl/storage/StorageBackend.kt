package io.quartic.howl.storage

import java.io.IOException
import java.io.InputStream

interface StorageBackend {
    @Throws(IOException::class)
    fun getData(namespace: String, objectName: String, version: Long?): InputStreamWithContentType?

    @Throws(IOException::class)
    fun putData(contentType: String?, namespace: String, objectName: String, inputStream: InputStream): Long?
}
