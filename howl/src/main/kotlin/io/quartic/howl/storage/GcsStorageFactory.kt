package io.quartic.howl.storage

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.storage.Storage
import com.google.api.services.storage.StorageScopes
import com.google.api.services.storage.model.StorageObject
import io.quartic.howl.storage.Storage.StorageMetadata
import io.quartic.howl.storage.Storage.StorageResult
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant

class GcsStorageFactory {
    data class Config(val bucket: String, val credentials: Credentials) : StorageConfig

    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(
        Type(value = Credentials.ApplicationDefault::class, name = "application_default"),
        Type(value = Credentials.ServiceAccountJsonKey::class, name = "service_account_json_key")
    )
    sealed class Credentials {
        abstract fun getCredential(transport: HttpTransport, jsonFactory: JsonFactory): GoogleCredential

        class ApplicationDefault : Credentials() {
            override fun getCredential(transport: HttpTransport, jsonFactory: JsonFactory): GoogleCredential =
                GoogleCredential.getApplicationDefault(transport, jsonFactory)
        }

        // TODO: These should probably be encrypted if we're planning to use them for real
        class ServiceAccountJsonKey(private val jsonKey: String): Credentials() {
            override fun getCredential(transport: HttpTransport, jsonFactory: JsonFactory): GoogleCredential =
                GoogleCredential.fromStream(
                    ByteArrayInputStream(jsonKey.toByteArray()),
                    transport,
                    jsonFactory
                )
        }
    }

    fun create(config: Config) = object : io.quartic.howl.storage.Storage {
        private val storage by lazy {
            val transport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = JacksonFactory()
            var credential = config.credentials.getCredential(transport, jsonFactory)

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
                response.contentType ?: DEFAULT_MIME_TYPE,
                // Probably OK for now!
                response.getSize().toLong()
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

    companion object {
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}


