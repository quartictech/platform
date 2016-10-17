package io.quartic.weyl.message;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractClientStatusMessage extends SocketMessage {
    List<LayerId> subscribedLiveLayerIds();
}
