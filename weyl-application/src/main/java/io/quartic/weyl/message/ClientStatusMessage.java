package io.quartic.weyl.message;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.LayerId;
import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface ClientStatusMessage extends SocketMessage {

    @SweetStyle
    @Value.Immutable
    abstract class SelectionStatus {
        public abstract int seqNum();
        public abstract List<EntityId> entityIds();

        public Pair<Integer, List<EntityId>> toPair() {
            return Pair.of(seqNum(), entityIds());
        }
    }

    List<LayerId> subscribedLiveLayerIds();
    SelectionStatus selection();
}
