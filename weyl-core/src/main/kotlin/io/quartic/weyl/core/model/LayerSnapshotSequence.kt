package io.quartic.weyl.core.model

import io.quartic.weyl.api.LayerUpdateType
import rx.Observable

data class LayerSnapshotSequence(
        val spec: LayerSpec,
        val snapshots: Observable<Snapshot>
) {
    data class Snapshot(
            val id: SnapshotId,
            val absolute: Layer,
            /**
             * Note: if you're using diff(), that probably means you'll be sensitive to missed values, etc.  Thus you need
             * to avoid constructs that involve resubscription (with the potential for either duplicates or missed values).
             */
            val diff: Diff
    )

    data class Diff(
            val updateType: LayerUpdateType,
            val features: Collection<Feature>
    )
}
