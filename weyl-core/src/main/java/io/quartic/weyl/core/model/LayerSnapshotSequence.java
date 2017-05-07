package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.api.LayerUpdateType;
import org.immutables.value.Value;
import rx.Observable;

import java.util.Collection;

import static java.util.Collections.emptyList;

@SweetStyle
@Value.Immutable
public interface LayerSnapshotSequence {
    @SweetStyle
    @Value.Immutable
    interface Snapshot {
        SnapshotId id();
        Layer absolute();

        /**
         * Note: if you're using diff(), that probably means you'll be sensitive to missed values, etc.  Thus you need
         * to avoid constructs that involve resubscription (with the potential for either duplicates or missed values).
         */
        Diff diff();
    }

    @SweetStyle
    @Value.Immutable
    interface Diff {
        static Diff empty() {
            return DiffImpl.of(LayerUpdateType.APPEND, emptyList());
        }
        LayerUpdateType updateType();
        Collection<Feature> features();
    }

    LayerSpec spec();
    Observable<Snapshot> snapshots();
}
