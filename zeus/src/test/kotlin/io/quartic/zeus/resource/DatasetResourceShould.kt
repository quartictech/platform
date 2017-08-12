package io.quartic.zeus.resource

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.common.test.assertThrows
import io.quartic.zeus.model.DatasetInfo
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.model.ItemId
import io.quartic.zeus.provider.DataProvider
import io.quartic.zeus.provider.DataProvider.TermMatcher
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import javax.ws.rs.NotFoundException



class DatasetResourceShould {

    private val simpleData = mapOf(
            ItemId("123") to mapOf("a" to "b", "c" to "d") as Map<String, Any>,
            ItemId("456") to mapOf("e" to "f", "g" to "h") as Map<String, Any>
    )

    private val filterableData = mapOf(
            ItemId("789") to mapOf("a" to "b", "_c" to "d", "e_" to "f") as Map<String, Any>
    )

    private val resource = DatasetResource(mapOf(
            DatasetName("yeah") to providerOf("Positive Polly", simpleData),
            DatasetName("oh no") to providerOf("Negative Nancy", mock())
    ))

    @Test
    fun get_dataset_names() {
        assertThat(resource.datasets, equalTo(mapOf(
                DatasetName("yeah") to DatasetInfo("Positive Polly"),
                DatasetName("oh no") to DatasetInfo("Negative Nancy")
        )))
    }

    @Test
    fun get_all_items_from_provider() {
        assertThat(resource.getAllItemsInDataset(DatasetName("yeah")).content, equalTo(simpleData))
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
    fun filter_out_keys_beginning_with_underscore_if_getting_all_items() {
        val resource = DatasetResource(mapOf(DatasetName("yeah") to providerOf("", filterableData)))

        assertThat(resource.getAllItemsInDataset(DatasetName("yeah")).content,
                equalTo(mapOf(ItemId("789") to mapOf("a" to "b", "e_" to "f") as Map<String, Any>)))
    }

    @Test
    fun not_filter_out_keys_if_getting_specific_item() {
        val resource = DatasetResource(mapOf(DatasetName("yeah") to providerOf("", filterableData)))

        assertThat(resource.getItemInDataset(DatasetName("yeah"), ItemId("789")), equalTo(filterableData[ItemId("789")]))
    }

    @Test
    fun pass_terms_to_matcher_and_return_only_its_results() {
        val myMatcher = mock<TermMatcher> {
            on { invoke(any(), any()) } doReturn simpleData
        }
        val provider = mock<DataProvider> {
            on { prettyName } doReturn ""
            on { matcher } doReturn myMatcher
        }
        val resource = DatasetResource(mapOf(DatasetName("yeah") to provider))

        val results = resource.getAllItemsInDataset(DatasetName("yeah"), setOf("hello", "goodbye"), 5)

        verify(myMatcher).invoke(setOf("hello", "goodbye"), 5)
        assertThat(results.content, equalTo(simpleData))
    }

    @Test
    fun extract_schema_from_dataset() {
        assertThat(resource.getAllItemsInDataset(DatasetName("yeah")).schema, equalTo(listOf("a", "c")))
    }

    @Test
    fun extract_empty_schema_if_dataset_is_empty() {
        val resource = DatasetResource(mapOf(DatasetName("yeah") to providerOf("", emptyMap())))

        assertThat(resource.getAllItemsInDataset(DatasetName("yeah")).schema, equalTo(emptyList()))
    }

    @Test
    fun get_all_items_as_csv() {
        assertThat(resource.getAllItemsInDatasetAsCsv(DatasetName("yeah")), equalTo("a,c\nb,d\n"))
    }

    private fun providerOf(myPrettyName: String, myData: Map<ItemId, Map<String, Any>>) = mock<DataProvider> {
        on { prettyName } doReturn myPrettyName
        on { data } doReturn myData
    }
}