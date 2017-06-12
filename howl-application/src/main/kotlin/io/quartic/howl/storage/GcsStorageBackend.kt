package io.quartic.howl.storage

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.storage.Storage
import com.google.api.services.storage.StorageScopes
import com.google.api.services.storage.model.StorageObject
import java.io.InputStream

class GcsStorageBackend(private val bucketSuffix: String) : StorageBackend {
    private val storage = buildService()

    private fun buildService(): Storage {
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

        return Storage.Builder(transport, jsonFactory, credential)
                .setApplicationName("Quartic platform")
                .build()
    }

    override fun getData(namespace: String, objectName: String, version: Long?): InputStreamWithContentType? {
        val get = storage.objects().get("$namespace.$bucketSuffix", getObjectName(namespace, objectName))
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

    override fun putData(contentType: String?, namespace: String, objectName: String, inputStream: InputStream): Long? {
        return storage.objects().insert(
                "$namespace.$bucketSuffix",
                StorageObject().setName(getObjectName(namespace, objectName)),
                InputStreamContent(contentType, inputStream)
        ).execute().generation
    }

    private fun getObjectName(namespace: String, objectName: String) = "$namespace/$objectName"
}
