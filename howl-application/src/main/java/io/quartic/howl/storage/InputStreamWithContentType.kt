package io.quartic.howl.storage

import java.io.InputStream

class InputStreamWithContentType(
        val contentType: String,
        val inputStream: InputStream
) : AutoCloseable {
    override fun close() = inputStream.close()
}
