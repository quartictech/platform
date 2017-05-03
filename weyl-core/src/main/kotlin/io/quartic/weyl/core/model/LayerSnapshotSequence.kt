package io.quartic.weyl.core.model

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
            val diff: Collection<Feature>
    )
}
