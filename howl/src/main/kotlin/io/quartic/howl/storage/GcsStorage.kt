package io.quartic.howl.storage

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
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
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.GcsStorage.Config.Credentials.ApplicationDefault
import io.quartic.howl.storage.GcsStorage.Config.Credentials.ServiceAccountJsonKey
import io.quartic.howl.storage.Storage.StorageResult
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant

// See https://cloud.google.com/storage/docs/consistency for the consistency model
class GcsStorage(private val config: Config) : io.quartic.howl.storage.Storage {

    data class Config(val bucket: String, val credentials: Credentials) : StorageConfig {
        @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
        @JsonSubTypes(
            Type(value = ApplicationDefault::class, name = "application_default"),
            Type(value = ServiceAccountJsonKey::class, name = "service_account_json_key")
        )
        sealed class Credentials {
            abstract fun getCredential(transport: HttpTransport, jsonFactory: JsonFactory): GoogleCredential

            class ApplicationDefault : Credentials() {
                override fun getCredential(transport: HttpTransport, jsonFactory: JsonFactory): GoogleCredential =
                    GoogleCredential.getApplicationDefault(transport, jsonFactory)
            }

            // TODO: These should probably be encrypted if we're planning to use them for real
            class ServiceAccountJsonKey(private val jsonKey: String) : Credentials() {
                override fun getCredential(transport: HttpTransport, jsonFactory: JsonFactory): GoogleCredential =
                    GoogleCredential.fromStream(
                        ByteArrayInputStream(jsonKey.toByteArray()),
                        transport,
                        jsonFactory
                    )
            }
        }
    }

    class Factory {
        fun create(config: Config) = GcsStorage(config)
    }

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

    override fun getObject(coords: StorageCoords) = wrapGcsException {
        val get = storage.objects().get(config.bucket, coords.backendKey)

        val httpResponse = get.executeMedia()
        val content = httpResponse.content
        val metadata = getMetadata(coords)

        if (content != null) {
            StorageResult(metadata!!, content)
        } else {
            null
        }
    }

    override fun getMetadata(coords: StorageCoords): StorageMetadata? = wrapGcsException {
        val storageObject: StorageObject = getStorageObject(coords)

        StorageMetadata(
            Instant.ofEpochMilli(storageObject.updated.value),
            storageObject.contentType ?: DEFAULT_MIME_TYPE,
            // Probably OK for now!
            storageObject.getSize().toLong()
        )
    }

    override fun putObject(contentLength: Int?, contentType: String?, inputStream: InputStream, coords: StorageCoords) {
        storage.objects()
            .insert(
                config.bucket,
                StorageObject().setName(coords.backendKey),
                InputStreamContent(contentType, inputStream)
            )
            .execute()
    }

    // According to https://cloud.google.com/storage/docs/consistency, GCS is strongly consistent.  So this
    // read-metadata-after-write pair should be safe (so long as we don't have concurrent writers, which is not
    // something we're considering as it will eventually be mitigated by namespaces/transactions).
    override fun copyObject(source: StorageCoords, dest: StorageCoords): StorageMetadata? = wrapGcsException {
        storage.objects()
            .copy(
                config.bucket,
                source.backendKey,
                config.bucket,
                dest.backendKey,
                null
            )
            .execute()
        getMetadata(dest)
    }

    private fun getStorageObject(coords: StorageCoords) =
        storage.objects().get(config.bucket, coords.backendKey).execute()

    private fun <T> wrapGcsException(block: () -> T): T? = try {
        block()
    } catch (e: GoogleJsonResponseException) {
         if (e.statusCode != 404) {
             throw e
         } else {
             null
         }
    }

    companion object {
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}


