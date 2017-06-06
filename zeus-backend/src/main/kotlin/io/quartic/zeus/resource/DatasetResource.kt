package io.quartic.zeus.resource

import io.quartic.zeus.csv.CsvGenerator
import io.quartic.zeus.model.Dataset
import io.quartic.zeus.model.DatasetInfo
import io.quartic.zeus.model.DatasetName
import io.quartic.zeus.model.ItemId
import io.quartic.zeus.provider.DataProvider
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/datasets")
class DatasetResource(private val providers: Map<DatasetName, DataProvider>) {

    @get:GET
    @get:Produces(MediaType.APPLICATION_JSON)
    val datasets = providers.mapValues { e -> DatasetInfo(e.value.prettyName) }

    // TODO: merge into /{dataset-name}?type=csv
    @GET
    @Path("/csv/{dataset-name}")
    @Produces("text/csv")
    fun getAllItemsInDatasetAsCsv(@PathParam("dataset-name") name: DatasetName)
            = CsvGenerator().generateFor(getAllItemsInDataset(name).content.values)

    // TODO: should potentially do the filtering via a lazy sequence to avoid memory footprint
    @GET
    @Path("/{dataset-name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllItemsInDataset(
            @PathParam("dataset-name") name: DatasetName,
            @QueryParam("term") terms: Set<String> = emptySet(),
            @QueryParam("limit") limit: Int = 0
    ) = with(getProviderOrThrow(name)) {
        Dataset(
                schema(),
                (if (terms.isEmpty()) data else matcher(terms, limit))
                        .mapValues { it.value.filterKeys { !it.startsWith("_") } }
        )
    }

    private fun DataProvider.schema() = (data.values.firstOrNull() ?: emptyMap())
            .keys
            .filter { !it.startsWith("_") }
            .toList()

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