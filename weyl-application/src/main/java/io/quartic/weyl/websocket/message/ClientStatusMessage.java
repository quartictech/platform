package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.LayerId;
import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = ClientStatusMessageImpl.class)
@JsonDeserialize(as = ClientStatusMessageImpl.class)
public interface ClientStatusMessage extends SocketMessage {

    @SweetStyle
    @Value.Immutable
    @JsonSerialize(as = SelectionStatusImpl.class)
    @JsonDeserialize(as = SelectionStatusImpl.class)
    abstract class SelectionStatus {
        public abstract int seqNum();
        public abstract List<EntityId> entityIds();

        public Pair<Integer, List<EntityId>> toPair() {
            return Pair.of(seqNum(), entityIds());
        }
    }

    @SweetStyle
    @Value.Immutable
    @JsonSerialize(as = GeofenceStatusImpl.class)
    @JsonDeserialize(as = GeofenceStatusImpl.class)
    interface GeofenceStatus {
        boolean enabled();
        GeofenceType type();
        Alert.Level defaultLevel();
        Optional<FeatureCollection> features();
        Optional<LayerId> layerId();
        double bufferDistance();    // TODO: what units?
    }

    List<LayerId> openLayerIds();
    SelectionStatus selection();
    GeofenceStatus geofence();
}
