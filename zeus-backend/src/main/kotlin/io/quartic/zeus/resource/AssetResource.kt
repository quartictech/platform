package io.quartic.zeus.resource

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.zeus.model.Asset
import io.quartic.zeus.model.AssetId
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/assets")
class AssetResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getAssets(): Map<AssetId, Asset> {
        javaClass.getResourceAsStream("/assets.json").use { stream ->
            return OBJECT_MAPPER.readValue(stream)
        }
    }
}