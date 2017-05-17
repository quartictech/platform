package io.quartic.zeus.resource

import io.quartic.zeus.DataProvider
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.model.ItemId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/stuff")
class StuffResource(private val providers: Map<DatasetName, DataProvider>) {

    @GET
    @Path("/{dataset-name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllItemsInDataset(
            @PathParam("dataset-name") name: DatasetName
    ) = providers[name]?.data ?: throw NotFoundException("No dataset with name '$name'")
    // TODO: filter out keys with "complex" values

    @GET
    @Path("/{dataset-name}/{item-id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItemInDataset(
            @PathParam("dataset-name") name: DatasetName,
            @PathParam("item-id") id: ItemId
    ) = getAllItemsInDataset(name)[id] ?: throw NotFoundException("No item with id '$id'")

    // TODO: add search endpoint
}