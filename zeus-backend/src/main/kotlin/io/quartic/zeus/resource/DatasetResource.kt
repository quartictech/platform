package io.quartic.zeus.resource

import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.model.ItemId
import io.quartic.zeus.provider.DataProvider
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/datasets")
class DatasetResource(private val providers: Map<DatasetName, DataProvider>) {

    // TODO: should potentially do the filtering via a lazy sequence to avoid memory footprint
    @GET
    @Path("/{dataset-name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllItemsInDataset(
            @PathParam("dataset-name") name: DatasetName,
            @QueryParam("term") terms: Set<String> = emptySet()
    ): Map<ItemId, Map<String, Any>> {
        return with(getProviderOrThrow(name)) {
            if (terms.isEmpty()) {
                data
            } else {
                matcher(terms)
            }
        }.mapValues { it.value.filterKeys { !it.startsWith("_") } }
    }
    
    @GET
    @Path("/{dataset-name}/{item-id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItemInDataset(
            @PathParam("dataset-name") name: DatasetName,
            @PathParam("item-id") id: ItemId
    ) = getProviderOrThrow(name).data[id] ?: throw NotFoundException("No item with id '$id'")

    private fun getProviderOrThrow(name: DatasetName) =
            providers[name] ?: throw NotFoundException("No dataset with name '$name'")

}