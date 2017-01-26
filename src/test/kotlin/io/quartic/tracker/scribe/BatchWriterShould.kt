package io.quartic.tracker.scribe

import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.io.IOException
import java.time.Instant

class BatchWriterShould {
    private val storage = mock<Storage>()
    private val writer = BatchWriter(storage, "myBucket", "test", mock(RETURNS_DEEP_STUBS))

    @Test
    fun write_messages_to_file_with_valid_name_format() {
        writer.write(listOf("foo", "bar"), Instant.EPOCH, 3)

        val captor = argumentCaptor<BlobInfo>()
        verify(storage).create(captor.capture(), eq("foo\nbar".toByteArray()))
        assertEquals(captor.firstValue.bucket, "myBucket")
        assertTrue(captor.firstValue.name.matches("test/1970-01-01T00:00:00Z-0003-.{6}".toRegex())) // Take account of the randomised suffix
    }

    @Test
    fun return_true_on_success() {
        assertTrue(writer.write(listOf("foo", "bar"), Instant.EPOCH, 3))
    }

    @Test
    fun return_false_on_error() {
        whenever(storage.create(any(), any<ByteArray>())).thenThrow(StorageException(IOException("Sad")))

        assertFalse(writer.write(listOf("foo", "bar"), Instant.EPOCH, 3))
    }
}