package io.quartic.zeus.resource

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.quartic.common.test.assertThrows
import io.quartic.zeus.DataProvider
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.model.ItemId
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import javax.ws.rs.NotFoundException

class DatasetResourceShould {

    private val simpleData = mapOf(
            ItemId("123") to mapOf("a" to "b", "c" to "d") as Map<String, Any>,
            ItemId("456") to mapOf("e" to "f", "g" to "h") as Map<String, Any>
    )

    private val nestedData = mapOf(
            ItemId("789") to mapOf("a" to "b", "c" to emptyList<Any>(), "d" to emptyMap<Any, Any>())
    )

    private val resource = DatasetResource(mapOf(
            DatasetName("yeah") to providerOf(simpleData),
            mock<DatasetName>() to mock<DataProvider>()
    ))

    @Test
    fun get_all_items_from_provider() {
        assertThat(resource.getAllItemsInDataset(DatasetName("yeah")), equalTo(simpleData))
    }

    @Test
    fun throw_404_if_dataset_not_found() {
        assertThrows<NotFoundException> {
            resource.getAllItemsInDataset(DatasetName("nah"))
        }
    }

    @Test
    fun get_specified_item_from_provider() {
        assertThat(resource.getItemInDataset(DatasetName("yeah"), ItemId("123")), equalTo(simpleData[ItemId("123")]))
    }

    @Test
    fun throw_404_if_item_not_found() {
        assertThrows<NotFoundException> {
            resource.getItemInDataset(DatasetName("yeah"), ItemId("789"))
        }
    }

    @Test
    fun filter_out_nested_attributes_if_getting_all_items() {
        val resource = DatasetResource(mapOf(DatasetName("yeah") to providerOf(nestedData)))

        assertThat(resource.getAllItemsInDataset(DatasetName("yeah")),
                equalTo(mapOf(ItemId("789") to mapOf("a" to "b") as Map<String, Any>)))
    }

    @Test
    fun not_filter_out_nested_attributes_if_getting_specific_item() {
        val resource = DatasetResource(mapOf(DatasetName("yeah") to providerOf(nestedData)))

        assertThat(resource.getItemInDataset(DatasetName("yeah"), ItemId("789")), equalTo(nestedData[ItemId("789")]))
    }

    private fun providerOf(myData: Map<ItemId, Map<String, Any>>) = mock<DataProvider> {
        on { data } doReturn myData
    }
}