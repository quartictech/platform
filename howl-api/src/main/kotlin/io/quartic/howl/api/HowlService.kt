package io.quartic.howl.api

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface HowlService {
    @Throws(IOException::class)
    fun uploadFile(contentType: String, namespace: String, fileName: String, upload: (OutputStream) -> Unit)

    @Throws(IOException::class)
    fun uploadFile(contentType: String, namespace: String, upload: (OutputStream) -> Unit): HowlStorageId

    @Throws(IOException::class)
    fun downloadFile(namespace: String, fileName: String): InputStream?
}
