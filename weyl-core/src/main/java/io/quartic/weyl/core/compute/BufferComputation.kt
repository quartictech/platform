package io.quartic.weyl.core.compute

import com.vividsolutions.jts.operation.buffer.BufferOp
import io.quartic.weyl.api.LayerUpdateType.REPLACE
import io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW
import io.quartic.weyl.core.model.*
import rx.Observable
import java.time.Clock

class BufferComputation @JvmOverloads constructor(
        private val layerId: LayerId,
        private val bufferSpec: BufferSpec,
        private val clock: Clock = Clock.systemUTC()
) : LayerPopulator {

    override fun dependencies() = listOf(bufferSpec.layerId)

    override fun spec(dependencies: List<Layer>): LayerSpec {
        val spec = dependencies[0].spec
        return LayerSpec(
                layerId,
                LayerMetadata(
                        "${spec.metadata.name} (buffered)",
                        "${spec.metadata.description} (buffered by ${bufferSpec.bufferDistance}m)",
                        spec.metadata.attribution,
                        clock.instant()
                ),
                IDENTITY_VIEW,
                spec.staticSchema,
                true
        )
    }

    override fun updates(dependencies: List<Layer>): Observable<LayerUpdate> {
        val bufferedFeatures = dependencies[0].features
                .map { (entityId, geometry, attributes) ->
                    NakedFeature(
                            entityId.uid,
                            BufferOp.bufferOp(geometry, bufferSpec.bufferDistance),
                            attributes)
                }
        return Observable.never<LayerUpdate>().startWith(LayerUpdate(REPLACE, bufferedFeatures))
    }
}
