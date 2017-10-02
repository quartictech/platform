package io.quartic.howl.storage

import com.google.api.client.http.HttpHeaders
import com.google.api.services.storage.Storage
import com.google.api.services.storage.Storage.Objects.Copy
import com.google.api.services.storage.model.StorageObject
import com.nhaarman.mockito_kotlin.*
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS

class GcsStorageShould {
    private val gcs = mock<Storage>(defaultAnswer = RETURNS_DEEP_STUBS)
    private val bucket = "my-bucket"
    private val storage = GcsStorage(bucket) { gcs }

    private val source = Unmanaged("foo")
    private val dest = Unmanaged("bar")

    @Test
    fun attach_correct_etag_constraint_to_copy_request() {
        val metadata = mock<StorageObject> {
            on { md5Hash } doReturn "some-noob-hash"
            on { etag } doReturn "abcdef"
        }
        whenever(gcs.objects().get(bucket, "raw/foo").execute()).thenReturn(metadata)

        val headers = mock<HttpHeaders>()
        val copyRequest = mock<Copy> {
            on { requestHeaders } doReturn headers
            on { execute() } doReturn mock<StorageObject>()
        }
        whenever(gcs.objects().copy(any(), any(), any(), any(), anyOrNull())).thenReturn(copyRequest)

        storage.copyObject(source, dest)

        verify(headers)["x-goog-copy-source-if-match"] = "abcdef"
    }

    @Test
    fun get_etag_again_if_copy_request_failed() {
        val metadata = mock<StorageObject> {
            on { md5Hash } doReturn "some-noob-hash"
            on { etag } doReturn "abcdef" doReturn "ghijkl"
        }
        whenever(gcs.objects().get(bucket, "raw/foo").execute()).thenReturn(metadata)

        val headers = mock<HttpHeaders>()
        val copyRequest = mock<Copy> {
            on { requestHeaders } doReturn headers
            on { execute() } doReturn null as StorageObject? doReturn mock<StorageObject>() // Fail first time, succeed second time
        }
        whenever(gcs.objects().copy(any(), any(), any(), any(), anyOrNull())).thenReturn(copyRequest)

        storage.copyObject(source, dest)

        inOrder(headers) {
            verify(headers)["x-goog-copy-source-if-match"] = "abcdef"
            verify(headers)["x-goog-copy-source-if-match"] = "ghijkl"
        }
    }
}
