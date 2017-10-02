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
import io.quartic.common.logging.logger
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.GcsStorage.Config.Credentials.ApplicationDefault
import io.quartic.howl.storage.GcsStorage.Config.Credentials.ServiceAccountJsonKey
import io.quartic.howl.storage.Storage.StorageResult
import java.io.ByteArrayInputStream
import java.io.InputStream

// See https://cloud.google.com/storage/docs/consistency for the consistency model
class GcsStorage(
    private val bucket: String,
    storageSupplier: () -> Storage
) : io.quartic.howl.storage.Storage {

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
        fun create(config: Config) = GcsStorage(config.bucket) {
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
    }

    private val LOG by logger()
    private val storage by lazy(storageSupplier)

    // TODO - make this atomic wrt data and metadata
    override fun getObject(coords: StorageCoords) = wrapGcsException {
        val get = storage.objects().get(bucket, coords.backendKey)

        val httpResponse = get.executeMedia()
        val content = httpResponse.content
        val metadata = getMetadata(coords)

        if (content != null) {
            StorageResult(metadata!!, content)
        } else {
            null
        }
    }

    override fun getMetadata(coords: StorageCoords): StorageMetadata? = wrapGcsException { getStorageObject(coords).toMetadata() }

    override fun putObject(contentLength: Int?, contentType: String?, inputStream: InputStream, coords: StorageCoords) {
        storage.objects()
            .insert(
                bucket,
                StorageObject().setName(coords.backendKey),
                InputStreamContent(contentType, inputStream)
            )
            .execute()
    }

    // GCS ETags are not a pure function of object content, so the ETag of the destination after copying will not
    // be the same as the ETag of the source (which is what we need).
    // Thus we use object MD5 as the Quartic ETag instead, and rely on a "safe copy" mechanism.
    override fun copyObject(source: StorageCoords, dest: StorageCoords, oldETag: String?) = wrapGcsException {
        var sourceObject = getStorageObject(source)

        if (sourceObject.md5Hash != oldETag) {
            var attempts = 1
            // We definitely need to do a copy, so now we keep attempting to copy until we guarantee we've copied
            // a source object whose metadata we have.
            while (!copyIfSourceETagMatches(source, dest, sourceObject.etag)) {
                // Try again with updated source metadata
                sourceObject = getStorageObject(source)
                LOG.info("Attempt #${++attempts} to copy ${source.backendKey} -> ${dest.backendKey}")
            }
        }

        sourceObject.toMetadata()
    }

    private fun copyIfSourceETagMatches(source: StorageCoords, dest: StorageCoords, eTag: String) =
        with(storage.objects().copy(bucket, source.backendKey, bucket, dest.backendKey, null)) {
            requestHeaders["x-goog-copy-source-if-match"] = eTag
            execute() != null
        }

    private fun getStorageObject(coords: StorageCoords) = storage.objects().get(bucket, coords.backendKey).execute()

    // We need the etag to be a function of content only, hence md5Hash
    private fun StorageObject.toMetadata() = StorageMetadata(contentType ?: DEFAULT_MIME_TYPE, getSize().toLong(), md5Hash)

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


