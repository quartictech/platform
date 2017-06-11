package io.quartic.howl.storage

import org.apache.commons.io.IOUtils
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.ws.rs.core.MediaType

class DiskStorageBackendShould {
    @Rule
    @JvmField
    var folder = TemporaryFolder()

    private val backend by lazy { DiskStorageBackend(folder.root.toPath()) }

    @Test
    fun store_data_and_return_it() {
        val data = "data".toByteArray()
        storeData("wat", data)
        val readData = getData("wat", null)

        assertThat(readData, equalTo(data))
    }

    @Test
    fun overwrite_with_new_version() {
        val data = "data".toByteArray()
        storeData("wat", data)
        val data2 = "data2".toByteArray()
        storeData("wat", data2)

        assertThat(getData("wat", null), equalTo(data2))
    }

    @Test
    fun writes_to_separate_objects_are_separate() {
        storeData("wat", "data".toByteArray())
        storeData("wat2", "data2".toByteArray())

        assertThat(getData("wat", null), equalTo("data".toByteArray()))
        assertThat(getData("wat2", null), equalTo("data2".toByteArray()))
    }

    @Test
    fun overwrites_create_new_versions() {
        val version = storeData("leet", "data".toByteArray())
        val version2 = storeData("leet", "data2".toByteArray())

        assertThat(version != version2, equalTo(true))
    }

    private fun storeData(objectName: String, data: ByteArray)
            = backend.putData(MediaType.TEXT_PLAIN, "test", objectName, ByteArrayInputStream(data))

    private fun getData(objectName: String, version: Long?): ByteArray? {
        val inputStreamWithContentType = backend.getData("test", objectName, version)
        return if (inputStreamWithContentType != null) {
            val outputStream = ByteArrayOutputStream()
            IOUtils.copy(inputStreamWithContentType.inputStream, outputStream)
            outputStream.toByteArray()
        } else {
            null
        }
    }
}
