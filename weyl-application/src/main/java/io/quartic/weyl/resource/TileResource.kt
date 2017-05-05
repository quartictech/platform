package io.quartic.weyl.resource

import io.dropwizard.jersey.caching.CacheControl
import io.quartic.common.logging.logger
import io.quartic.common.rx.accumulateMap
import io.quartic.common.rx.latest
import io.quartic.common.rx.likeBehavior
import io.quartic.weyl.core.model.LayerId
import io.quartic.weyl.core.model.LayerSnapshotSequence
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot
import io.quartic.weyl.core.model.SnapshotId
import io.quartic.weyl.core.render.VectorTileRenderer
import org.glassfish.jersey.server.ManagedAsync
import rx.Observable
import rx.Observable.error
import rx.schedulers.Schedulers
import javax.ws.rs.*
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.Response

@Path("/")
class TileResource @JvmOverloads constructor(
        snapshotSequences: Observable<LayerSnapshotSequence>,
        private val renderer: VectorTileRenderer = VectorTileRenderer()
) {
    private val LOG by logger()
    private val scheduler = Schedulers.computation()

    // Don't do anything smart with deleted layers
    private val layers = snapshotSequences
            .compose(accumulateMap({ snapshot: LayerSnapshotSequence -> snapshot.spec().id() }) { snapshot -> snapshot })
            .compose(likeBehavior())

    @GET
    @Produces("application/protobuf")
    @Path("/{layerId}/{snapshotId}/{z}/{x}/{y}.pbf")
    @CacheControl(maxAge = 60 * 60)
    @ManagedAsync
    fun render(@PathParam("layerId") layerId: LayerId,
               @PathParam("snapshotId") snapshotId: SnapshotId, // We don't actually use this, it's just there to invalidate the browser cache
               @PathParam("z") z: Int,
               @PathParam("x") x: Int,
               @PathParam("y") y: Int,
               @Suspended asyncResponse: AsyncResponse
    ) {
        layers.subscribeOn(scheduler)
                .map { layers -> renderer.render(latest(getOrError(layers, layerId)).absolute(), z, x, y) }
                .first()
                .subscribe({ data ->
                    LOG.info("returned data: {}", data.size)
                    asyncResponse.resume(if (data.isNotEmpty()) data else Response.noContent().build())
                }, { asyncResponse.resume(it) })
    }

    private fun getOrError(layers: Map<LayerId, LayerSnapshotSequence>, id: LayerId): Observable<Snapshot> {
        val layer = layers[id]
        return if (layer != null) {
            layer.snapshots()
        } else {
            error(NotFoundException("No layer with id " + id))
        }
    }
}
