package io.quartic.howl.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.nhaarman.mockito_kotlin.*
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.hamcrest.Matchers.hasItem
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Instant
import java.util.*

class S3StorageShould {
    private val s3 = mock<AmazonS3>()
    private val bucket = "my-bucket"
    private val storage = S3Storage(UnsafeSecret(bucket)) { s3 }

    private val source = Unmanaged("foo")
    private val dest = Unmanaged("bar")

    @Test
    fun attach_correct_etag_constraint_to_copy_request() {
        val metadata = mock<ObjectMetadata> {
            on { lastModified } doReturn Date.from(Instant.now())
            on { contentType } doReturn "noob/hole"
            on { contentLength } doReturn 42
            on { eTag } doReturn "abcdef"
        }
        whenever(s3.getObjectMetadata(bucket, "raw/foo")).thenReturn(metadata)  // Succeed first time

        whenever(s3.copyObject(any())).thenReturn(mock())

        storage.copyObject(source, dest)

        val captor = argumentCaptor<CopyObjectRequest>()
        verify(s3).copyObject(captor.capture())
        assertThat(captor.firstValue.matchingETagConstraints, hasItem("abcdef"))
    }

    @Test
    fun get_etag_again_if_copy_request_failed() {
        val metadata = mock<ObjectMetadata> {
            on { lastModified } doReturn Date.from(Instant.now())
            on { contentType } doReturn "noob/hole"
            on { contentLength } doReturn 42
            on { eTag } doReturn "abcdef" doReturn "ghijkl"
        }
        whenever(s3.getObjectMetadata(bucket, "raw/foo")).thenReturn(metadata)

        whenever(s3.copyObject(any())).thenReturn(null).thenReturn(mock())  // Fail first time, succeed second time

        storage.copyObject(source, dest)

        val captor = argumentCaptor<CopyObjectRequest>()
        verify(s3, times(2)).copyObject(captor.capture())
        assertThat(captor.secondValue.matchingETagConstraints, hasItem("ghijkl"))
    }
}
