package io.quartic.zeus.resource

import io.quartic.zeus.DataProvider
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.model.ItemId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/datasets")
class DatasetResource(private val providers: Map<DatasetName, DataProvider>) {

    @GET
    @Path("/{dataset-name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllItemsInDataset(
            @PathParam("dataset-name") name: DatasetName
    ) = getDatasetOrThrow(name)
            .mapValues { removeNestedAttributes(it.value) }

    private fun removeNestedAttributes(item: Map<String, Any>) = item.filterValues { it !is Collection<*> && it !is Map<*,*> }

    @GET
    @Path("/{dataset-name}/{item-id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItemInDataset(
            @PathParam("dataset-name") name: DatasetName,
            @PathParam("item-id") id: ItemId
    ) = getDatasetOrThrow(name)[id] ?: throw NotFoundException("No item with id '$id'")

    // TODO: add search endpoint

    private fun getDatasetOrThrow(name: DatasetName) =
            providers[name]?.data ?: throw NotFoundException("No dataset with name '$name'")

}