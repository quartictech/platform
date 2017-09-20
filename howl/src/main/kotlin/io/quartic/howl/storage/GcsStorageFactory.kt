package io.quartic.howl.storage

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.storage.Storage
import com.google.api.services.storage.StorageScopes
import com.google.api.services.storage.model.StorageObject
import io.quartic.howl.storage.Storage.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import java.io.InputStream
import java.time.Instant

class GcsStorageFactory {
    data class Config(val bucket: String) : StorageConfig

    private val storage by lazy {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = JacksonFactory()
        var credential = GoogleCredential.getApplicationDefault(transport, jsonFactory)

        // Depending on the environment that provides the default credentials (for
        // example: Compute Engine, App Engine), the credentials may require us to
        // specify the scopes we need explicitly.  Check for this case, and inject
        // the Cloud Storage scope if required.
        if (credential.createScopedRequired()) {
            credential = credential.createScoped(StorageScopes.all())
        }

        Storage.Builder(transport, jsonFactory, credential)
                .setApplicationName("Quartic platform")
                .build()
    }

    fun create(config: Config) = object : io.quartic.howl.storage.Storage {
        override fun getData(coords: StorageCoords) = wrapGcsException {
            val get = storage.objects().get(config.bucket, coords.bucketKey)

            val httpResponse = get.executeMedia()
            val content = httpResponse.content
            val metadata = getMetadata(coords)

            if (content != null) {
                StorageResult(
                    metadata!!,
                    content
                )
            } else {
                null
            }
        }


        override fun getMetadata(coords: StorageCoords): StorageMetadata? = wrapGcsException {
            val get = storage.objects().get(config.bucket, coords.bucketKey)
            val response = get.execute()

            StorageMetadata(
                Instant.ofEpochMilli(response.updated.value),
                response.contentType,
                response.size.toLong()
            )
        }

        override fun putData(coords: StorageCoords, contentLength: Int?, contentType: String?, inputStream: InputStream): Boolean {
            storage.objects()
                .insert(
                    config.bucket,
                    StorageObject().setName(coords.bucketKey),
                    InputStreamContent(contentType, inputStream)
                )
                .execute()

            return true
        }

        private fun <T> wrapGcsException(block: () -> T): T? = try {
            block()
        } catch (e: GoogleJsonResponseException) {
             if (e.statusCode != 404) {
                 throw e
             } else {
                 null
             }
        }
    }
}


