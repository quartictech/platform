package io.quartic.howl.api

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface HowlService {
    @Throws(IOException::class)
    fun uploadFile(
        targetNamespace: String,
        key: String,
        contentType: String,
        upload: (OutputStream) -> Unit
    )

    @Throws(IOException::class)
    fun uploadFile(
        targetNamespace: String,
        identityNamespace: String,
        key: String,
        contentType: String,
        upload: (OutputStream) -> Unit
    )

    @Throws(IOException::class)
    fun uploadAnonymousFile(
        targetNamespace: String,
        contentType: String,
        upload: (OutputStream) -> Unit
    ): HowlStorageId

    @Throws(IOException::class)
    fun uploadAnonymousFile(
        targetNamespace: String,
        identityNamespace: String,
        contentType: String,
        upload: (OutputStream) -> Unit
    ): HowlStorageId

    @Throws(IOException::class)
    fun downloadFile(targetNamespace: String, key: String): InputStream?

    @Throws(IOException::class)
    fun downloadFile(targetNamespace: String, identityNamespace: String, key: String): InputStream?
}
