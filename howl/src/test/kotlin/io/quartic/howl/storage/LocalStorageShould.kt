package io.quartic.howl.storage

import io.quartic.howl.storage.LocalStorage.Config
import io.quartic.howl.storage.StorageCoords.Managed
import org.apache.commons.io.IOUtils
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.ws.rs.core.MediaType

class LocalStorageShould {
    @Rule
    @JvmField
    var folder = TemporaryFolder()

    private val storage by lazy { LocalStorage(Config(folder.root.toString())) }

    @Test
    fun store_data_and_return_it() {
        val data = "data".toByteArray()
        storeData("wat", data)
        val readData = getData("wat")

        assertThat(readData, equalTo(data))
    }

    @Test
    fun overwrite_with_new_version() {
        val data = "data".toByteArray()
        storeData("wat", data)
        val data2 = "data2".toByteArray()
        storeData("wat", data2)

        assertThat(getData("wat"), equalTo(data2))
    }

    @Test
    fun writes_to_separate_coords_are_separate() {
        storeData("wat", "data".toByteArray())
        storeData("wat2", "data2".toByteArray())

        assertThat(getData("wat"), equalTo("data".toByteArray()))
        assertThat(getData("wat2"), equalTo("data2".toByteArray()))
    }

    private fun storeData(objectName: String, data: ByteArray)
        = storage.putData(Managed("foo", "bar", objectName), null, MediaType.TEXT_PLAIN, ByteArrayInputStream(data))

    private fun getData(objectName: String): ByteArray? {
        val inputStreamWithContentType = storage.getData(Managed("foo", "bar", objectName))
        return if (inputStreamWithContentType != null) {
            val outputStream = ByteArrayOutputStream()
            IOUtils.copy(inputStreamWithContentType.inputStream, outputStream)
            outputStream.toByteArray()
        } else {
            null
        }
    }
}
