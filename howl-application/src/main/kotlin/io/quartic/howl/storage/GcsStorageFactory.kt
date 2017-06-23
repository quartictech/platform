package io.quartic.howl.storage

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.storage.Storage
import com.google.api.services.storage.StorageScopes
import com.google.api.services.storage.model.StorageObject
import io.quartic.howl.storage.Storage.PutResult
import java.io.InputStream

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
        override fun getData(coords: StorageCoords, version: Long?): InputStreamWithContentType? {
            val get = storage.objects().get(config.bucket, coords.path)
            get.generation = version

            try {
                val httpResponse = get.executeMedia()
                val content = httpResponse.content
                if (content != null) {
                    return InputStreamWithContentType(httpResponse.contentType, content)
                }
            } catch (e: GoogleJsonResponseException) {
                if (e.statusCode != 404) {
                    throw e
                }
            }
            return null
        }

        override fun putData(coords: StorageCoords, contentType: String?, inputStream: InputStream) = PutResult(
                storage.objects().insert(
                        config.bucket,
                        StorageObject().setName(coords.path),
                        InputStreamContent(contentType, inputStream)
                ).execute().generation)
    }

    private val StorageCoords.path get() = "$identityNamespace/$objectName"
}


