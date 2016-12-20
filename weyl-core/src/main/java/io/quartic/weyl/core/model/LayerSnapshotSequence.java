package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;
import rx.Observable;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface LayerSnapshotSequence {
    @SweetStyle
    @Value.Immutable
    interface Snapshot {
        Layer absolute();

        /**
         * Note: if you're using diff(), that probably means you'll be sensitive to missed values, etc.  Thus you need
         * to avoid constructs that involve resubscription (with the potential for either duplicates or missed values).
         */
        Collection<Feature> diff();
    }

    LayerSpec spec();
    Observable<Snapshot> snapshots();
}
